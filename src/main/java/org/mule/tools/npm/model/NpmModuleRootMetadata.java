/**
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tools.npm.model;

import com.github.gundy.semver4j.model.Version;

import java.util.*;

public final class NpmModuleRootMetadata {
    private final SortedSet<Version> availableVersions;
    private final Map versionDataMap;

    public NpmModuleRootMetadata(Builder builder) {
        this.availableVersions = builder.availableVersions;
        this.versionDataMap = builder.versionDataMap;
    }

    public Map getVersionDataMap() {
        return versionDataMap;
    }

    public SortedSet<Version> getAvailableVersions() {
        return new TreeSet<Version>(availableVersions);
    }

    public Version getLatestVersion() {
        if (availableVersions.size() == 0) {
            return null;
        } else {
            return availableVersions.last();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NpmModuleRootMetadata that = (NpmModuleRootMetadata) o;

        return availableVersions != null ? availableVersions.equals(that.availableVersions) : that.availableVersions == null;

    }

    @Override
    public int hashCode() {
        return availableVersions != null ? availableVersions.hashCode() : 0;
    }

    public static Builder builder(Map versionDataMap) {
        return new Builder(versionDataMap);
    }

    public static final class Builder {
        private final TreeSet<Version> availableVersions;
        private final Map versionDataMap;

        public Builder(Map versionDataMap) {
            this.versionDataMap = versionDataMap;
            this.availableVersions = new TreeSet<Version>();
            for (Object o : versionDataMap.keySet()) {
                availableVersions.add(Version.fromString(o.toString()));
            }

        }

        public NpmModuleRootMetadata build() {
            return new NpmModuleRootMetadata(this);
        }
    }
}
