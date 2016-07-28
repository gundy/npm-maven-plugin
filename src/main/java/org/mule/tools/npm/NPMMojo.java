/**
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tools.npm;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.settings.Settings;
import org.mule.tools.npm.model.UnresolvedNpmModule;
import org.mule.tools.npm.service.registry.NpmRegistry;
import org.mule.tools.npm.service.urldownloader.UrlDownloader;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Goal that offers Recess support in Maven builds.
 *
 * @goal fetch-modules
 * @phase generate-sources
 */
public class NPMMojo extends AbstractMojo {

    /**
     * Where the resulting files will be downloaded.
     *
     * @parameter expression="${recess.outputDirectory}" default-value="${basedir}/src/main/resources/META-INF"
     */
    private File outputDirectory;

    /**
     * The identifiers of the packages to download. Use the following syntax: package:version
     *
     * @parameter expression="${recess.packages}
     */
    private String [] packages;

    /**
     * Location of package.json file to scan for dependencies (either this, or packages must be specified)
     *
     * @parameter
     */
    private File packageJson;

    /**
     * The Maven Settings.
     * 
     * @parameter default-value="${settings}"
     * @required
     * @readonly
     */
    private Settings settings;

    /**
     * Base URL for NPM repository - update this if you're using a cache/proxy in a corporate environment.
     *
     * @parameter expression="${recess.npmRepositoryBase}" default-value="http://registry.npmjs.org"
     */
    private String npmRepositoryBase;


    public void execute() throws MojoExecutionException {
        if (packages != null) {
            download(parsePackages());
        }

        if (packageJson != null) {
            download(parsePackageJson());
        }
    }

    private Collection<UnresolvedNpmModule> parsePackageJson() throws MojoExecutionException {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            Map packageJsonData = objectMapper.readValue(packageJson, Map.class);
            Map dependenciesMap = (Map)packageJsonData.get("dependencies");

            List<UnresolvedNpmModule> packageJsonModules = new ArrayList<UnresolvedNpmModule>();

            if (dependenciesMap != null) {
                for (Object dependencyAsObject : dependenciesMap.entrySet()){
                    Map.Entry dependency = (Map.Entry) dependencyAsObject;
                    String dependencyName = (String) dependency.getKey();
                    String dependencyVersionSpecification = ((String) dependency.getValue());
                    packageJsonModules.add(UnresolvedNpmModule.builder().name(dependencyName).versionSpecification(dependencyVersionSpecification).build());
                }
            }

            return packageJsonModules;

        } catch (Exception e) {
            throw new MojoExecutionException("Unable to read package.json file", e);
        }
    }

    public void download(Collection<UnresolvedNpmModule> npmModules) throws MojoExecutionException {
        Log log = getLog();
        UrlDownloader urlDownloader = UrlDownloader.forProxy(settings.getActiveProxy());
        NpmRegistry registry = new NpmRegistry(npmRepositoryBase, urlDownloader, log);

        for (UnresolvedNpmModule module : npmModules) {
            try {
                registry.saveModuleAndDependencies(module, outputDirectory);
            } catch (Throwable t) {
                throw new MojoExecutionException("Unable to download package ["+module.toString()+"]", t);
            }
        }
    }

    public Collection<UnresolvedNpmModule> parsePackages() {
        List<UnresolvedNpmModule> modules = new ArrayList<UnresolvedNpmModule>();
        for (String aPackage : packages) {
            UnresolvedNpmModule module = UnresolvedNpmModule.builder().fromConfigurationString(aPackage).build();
            modules.add(module);
        }
        return modules;
    }
}
