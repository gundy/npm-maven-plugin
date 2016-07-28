/**
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tools.npm.model;

import com.github.gundy.semver4j.model.Version;

import java.net.URL;
import java.util.*;

public final class NpmModuleSpecificVersionMetadata {
    private final URL tarballUrl;
    private final Set<UnresolvedNpmModule> dependencies;
    private final Version resolvedVersion;

    public NpmModuleSpecificVersionMetadata(Builder builder) {
        if (builder.tarballUrl == null) {
            throw new IllegalArgumentException("Tarball URL must be specified");
        }
        this.tarballUrl = builder.tarballUrl;
        if (builder.resolvedVersion == null) {
            throw new IllegalArgumentException("Resolved version must be specified");
        }
        this.resolvedVersion = builder.resolvedVersion;
        this.dependencies = Collections.unmodifiableSet(builder.dependencies);
    }

    public URL getTarballUrl() {
        return tarballUrl;
    }

    public Version getResolvedVersion() {
        return resolvedVersion;
    }

    public Set<UnresolvedNpmModule> getDependencies() {
        return dependencies;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private URL tarballUrl;
        private HashSet<UnresolvedNpmModule> dependencies;
        private Version resolvedVersion;

        public Builder() {
            this.dependencies = new HashSet<UnresolvedNpmModule>();
        }

        public Builder tarballUrl(URL tarballUrl) {
            this.tarballUrl = tarballUrl;
            return this;
        }

        public Builder addDependency(UnresolvedNpmModule dependency) {
            this.dependencies.add(dependency);
            return this;
        }

        public Builder resolvedVersion(Version resolvedVersion) {
            this.resolvedVersion = resolvedVersion;
            return this;
        }

        public Builder addDependencies(Collection<UnresolvedNpmModule> dependencies) {
            this.dependencies.addAll(dependencies);
            return this;
        }

        public NpmModuleSpecificVersionMetadata build() {
            return new NpmModuleSpecificVersionMetadata(this);
        }
    }
}
