/**
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tools.npm.service.urldownloader;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

final class NonProxyUrlDownloader extends UrlDownloader {
    NonProxyUrlDownloader() {
    }

    public InputStream getInputStreamFromUrl(final URL url) throws IOException {
        URLConnection conn = url.openConnection();
        return conn.getInputStream();
    }
}
