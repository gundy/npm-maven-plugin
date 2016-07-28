/**
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tools.npm.service.registry;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.codehaus.plexus.util.StringUtils;
import org.mule.tools.npm.model.NpmModuleRootMetadata;
import org.mule.tools.npm.model.NpmModuleSpecificVersionMetadata;
import com.github.gundy.semver4j.model.Version;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.archiver.tar.TarGZipUnArchiver;
import org.mule.tools.npm.logging.DownloadCountingOutputStream;
import org.mule.tools.npm.logging.LoggerAdapter;
import org.mule.tools.npm.logging.ProgressListener;
import org.mule.tools.npm.model.UnresolvedNpmModule;
import org.mule.tools.npm.service.urldownloader.UrlDownloader;

import java.io.*;
import java.net.URL;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class NpmRegistry {
    private static final String NPM_URL_EXTENSION="%s";

    private final String npmRegistryBaseUrl;
    private final String npmRegistryUrl;
    private final UrlDownloader urlDownloader;
    private final Log log;

    private final ConcurrentMap<String, NpmModuleRootMetadata> rootMetaCache;

    public NpmRegistry(String npmRegistryBaseUrl, UrlDownloader urlDownloader, Log log) {
        if (npmRegistryBaseUrl == null) {
            throw new IllegalArgumentException("NPM registry base URL must be specified");
        }
        this.npmRegistryBaseUrl =
                npmRegistryBaseUrl.endsWith("/") ?
                        npmRegistryBaseUrl
                        : npmRegistryBaseUrl + "/";
        this.npmRegistryUrl = this.npmRegistryBaseUrl + NPM_URL_EXTENSION;
        this.urlDownloader = urlDownloader;
        this.log = log;
        this.rootMetaCache = new ConcurrentHashMap<String, NpmModuleRootMetadata>();
    }

    /**
     * Download root metadata (ie. list of available versions of a given module)
     * from npm registry..
     *
     * @param moduleName name of module to lookup
     * @return NpmModuleRootMetadata with set of available versions of this module.
     */
    public synchronized NpmModuleRootMetadata fetchRootMetadataForModule(String moduleName) {
        if (rootMetaCache.containsKey(moduleName)) {
            return rootMetaCache.get(moduleName);
        } else {
            try {
                // Don't use URLEncoder.encode() utility because we want to keep the @ on scoped packages
                String urlEncodedName = StringUtils.replace(moduleName, "/", "%2F");

                URL dl = new URL(String.format(npmRegistryUrl,urlEncodedName));
                ObjectMapper objectMapper = new ObjectMapper();
                Map allVersionsMetadata = objectMapper.readValue(urlDownloader.loadTextFromUrl(dl),Map.class);

                Map allVersions = ((Map) allVersionsMetadata.get("versions"));
                NpmModuleRootMetadata.Builder builder = NpmModuleRootMetadata.builder(allVersions);
                NpmModuleRootMetadata meta = builder.build();
                rootMetaCache.putIfAbsent(moduleName, meta);
                return meta;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Get metadata specific to a particular version of a module - eg.
     *  tarball URL, set of resolved dependencies etc.
     * @param module the name of the module to look up
     * @return A NpmModuleSpecificVersionMetadata instance with the relevant data.
     */
    public NpmModuleSpecificVersionMetadata fetchMetadataForLatestModuleMatchingVersionSpecification(NpmModuleRootMetadata rootMetadata, UnresolvedNpmModule module) {

        log.debug("Fetching metadata for "+module);

        try {
            Version version = resolveVersion(module);
            log.debug(" - resolved version to "+version.toString());
            Map versionSpecificData = (Map)rootMetadata.getVersionDataMap().get(version.toString());

            NpmModuleSpecificVersionMetadata.Builder builder = NpmModuleSpecificVersionMetadata.builder();
            builder.resolvedVersion(version);

            Map distMap = (Map) versionSpecificData.get("dist");
            builder.tarballUrl(new URL((String) distMap.get("tarball")));

            Map dependenciesMap = (Map) versionSpecificData.get("dependencies");

            if (dependenciesMap != null) {
                for (Object dependencyAsObject : dependenciesMap.entrySet()){
                    Map.Entry dependency = (Map.Entry) dependencyAsObject;
                    String dependencyName = (String) dependency.getKey();

                    String dependencyVersionSpecification = ((String) dependency.getValue());
                    builder.addDependency(UnresolvedNpmModule.builder().name(dependencyName).versionSpecification(dependencyVersionSpecification).build());
                }
            }
            return builder.build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    /*
     * resolve the latest version that matches the search criteria
     */
    private Version resolveVersion(UnresolvedNpmModule module) {
        try {
            NpmModuleRootMetadata rootMeta = fetchRootMetadataForModule(module.getName());
            if ("latest".equalsIgnoreCase(module.getVersionSpecification()) || module.getVersionSpecification() == null) {
                return Version.maxVersionSatisfying(rootMeta.getAvailableVersions(), "*");
            } else {
                return Version.maxVersionSatisfying(rootMeta.getAvailableVersions(), module.getVersionSpecification());
            }
        } catch (Exception e) {
            throw new RuntimeException("Unable to resolve canonical version for module "+module,e);
        }
    }

    private void saveModuleAndDependenciesInternal(Set<String> savedAlready, UnresolvedNpmModule module, File baseFolder) {
        /* there is no point downloading and saving a module again if we've already retrieved it before */
        if (savedAlready.contains(module.getName())) {
            return;
        }

        NpmModuleRootMetadata rootMeta = fetchRootMetadataForModule(module.getName());
        NpmModuleSpecificVersionMetadata metadata = fetchMetadataForLatestModuleMatchingVersionSpecification(rootMeta, module);

        /* download module */
        saveModule(module.getName(), metadata.getResolvedVersion().toString(), metadata, baseFolder);
        savedAlready.add(module.getName());

        /* download dependencies recursively */
        Set<UnresolvedNpmModule> dependencies = metadata.getDependencies();
        for (UnresolvedNpmModule dependency : dependencies) {
            saveModuleAndDependenciesInternal(savedAlready, dependency, baseFolder);
        }
    }

    /*
     * 1. create temp folder in baseFolder called <moduleName>_tmp
     * 2. write module tarball to tempFolder/<moduleName>-<moduleVersion>.tgz
     * 3. cd tempFolder ; tar xfvz <tarball>
     * 4. delete tempFolder/<tarball> file
     * 5. mv tempFolder/package (a directory) into baseFolder
     * 6. rmdir tempFolder
     */
    private void saveModule(String moduleName, String version, NpmModuleSpecificVersionMetadata metadata, File baseFolder) {
        URL dl;
        OutputStream os = null;
        InputStream is = null;

        String name = moduleName;

        File outputFolderFileTmp = new File(baseFolder, name.replace("/", "_") + "_tmp");
        File outputFolderFile = new File(baseFolder, name);

        /* secondary validation; verify that we haven't already downloaded this file. */
        if (outputFolderFile.exists()) {
            return;  // Already downloaded nothing to do
        }

        if (!outputFolderFileTmp.mkdirs()) {
            throw new RuntimeException("Unable to create output temp folder: "+outputFolderFileTmp);
        }

        File tarFile = new File(outputFolderFileTmp, name.replace("/", "_") + "-" + version + ".tgz");
        ProgressListener progressListener = new ProgressListener(log);
        log.debug("Downloading " + moduleName+"@"+version.toString());

        try {
            os = new FileOutputStream(tarFile);
            is = urlDownloader.getInputStreamFromUrl(metadata.getTarballUrl());

            DownloadCountingOutputStream dcount = new DownloadCountingOutputStream(os);
            dcount.setListener(progressListener);

            IOUtils.copy(is, dcount);

        } catch (FileNotFoundException e) {
            throw new RuntimeException(String.format("Error downloading module %s", moduleName),e);
        } catch (IOException e) {
            throw new RuntimeException(String.format("Error downloading module %s", moduleName),e);
        } finally {
            IOUtils.closeQuietly(os);
            IOUtils.closeQuietly(is);
        }

        final TarGZipUnArchiver ua = new TarGZipUnArchiver();
        ua.enableLogging(new LoggerAdapter(log));
        ua.setSourceFile(tarFile);
        ua.setDestDirectory(outputFolderFileTmp);
        ua.extract();

        FileUtils.deleteQuietly(tarFile);


        File fileToMove;

        if (!outputFolderFile.mkdirs()) {
            throw new RuntimeException("Unable to create output folder: "+outputFolderFileTmp);
        }

        File[] files = outputFolderFileTmp.listFiles();
        if (files != null && files.length == 1) {
            fileToMove = files[0];
        } else {
            File aPackage = new File(outputFolderFileTmp, "package");
            if (aPackage.exists() && aPackage.isDirectory()) {
                fileToMove = aPackage;
            } else {
                throw new RuntimeException(String.format(
                        "Only one file should be present at the folder when " +
                                "unpacking module %s: ", moduleName));
            }
        }

        try {
            FileUtils.copyDirectory(fileToMove, outputFolderFile);
        } catch (IOException e) {
            throw new RuntimeException(String.format("Error moving to the final folder when " +
                    "unpacking module %s: ", moduleName),e);
        }

        try {
            FileUtils.deleteDirectory(outputFolderFileTmp);
        } catch (IOException e) {
            log.info("Error while deleting temporary folder: " + outputFolderFileTmp, e);
        }
    }

    /**
     * Save a module and it's dependencies to a folder on disk.
     *
     * @param module The module to save.
     * @param baseFolder The location on disk to save the module.
     */
    public void saveModuleAndDependencies(UnresolvedNpmModule module, File baseFolder) {
        if (!baseFolder.exists()) {
            boolean success = baseFolder.mkdirs();
            if (!success) {
                throw new RuntimeException("Unable to create directory: "+baseFolder);
            }
        }
        saveModuleAndDependenciesInternal(new TreeSet<String>(), module, baseFolder);
    }

}
