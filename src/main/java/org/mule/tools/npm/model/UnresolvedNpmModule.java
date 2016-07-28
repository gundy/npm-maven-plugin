/**
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tools.npm.model;

public final class UnresolvedNpmModule {
    private final String name;
    private final String versionSpecification;

    private UnresolvedNpmModule(Builder builder) {
        if (builder.name == null) {
            throw new IllegalArgumentException("Name must be specified");
        }
        this.name = builder.name;
        this.versionSpecification = "latest".equalsIgnoreCase(builder.versionSpecification) ? null : builder.versionSpecification;

    }

    public String getName() {
        return name;
    }

    public String getVersionSpecification() {
        return versionSpecification;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UnresolvedNpmModule npmModule = (UnresolvedNpmModule) o;

        if (name != null ? !name.equals(npmModule.name) : npmModule.name != null) return false;
        return versionSpecification != null ? versionSpecification.equals(npmModule.versionSpecification) : npmModule.versionSpecification == null;

    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (versionSpecification != null ? versionSpecification.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return name+":"+ ((versionSpecification==null) ?"latest":versionSpecification);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String name;
        private String versionSpecification;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder versionSpecification(String versionSpecification) {
            this.versionSpecification = versionSpecification;
            return this;
        }

        public Builder fromConfigurationString(String pkg) {
            if (pkg == null) {
                throw new RuntimeException("cannot construct module from null");
            }
            if (!pkg.contains(":")) {
                this.name=pkg;
            } else {
                String[] nameVersion = pkg.split(":");
                this.name = nameVersion[0];
                if (!"latest".equalsIgnoreCase(nameVersion[1])) {
                    this.versionSpecification = nameVersion[1];
                }
            }
            return this;
        }


        public UnresolvedNpmModule build() {
            return new UnresolvedNpmModule(this);
        }
    }
}
