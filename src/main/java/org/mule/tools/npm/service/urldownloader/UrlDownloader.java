/**
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tools.npm.service.urldownloader;

import org.apache.commons.io.IOUtils;
import org.apache.maven.settings.Proxy;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public abstract class UrlDownloader {
    public abstract InputStream getInputStreamFromUrl(final URL url) throws IOException;

    /* static factory method */
    public static UrlDownloader forProxy(Proxy proxy) {
        if (proxy == null) {
            return new NonProxyUrlDownloader();
        } else {
            return new MavenProxyUrlDownloader(proxy);
        }
    }

    public String loadTextFromUrl(final URL url) {
        try {
            return IOUtils.toString(getInputStreamFromUrl(url));
        } catch (IOException e) {
            /* if we fail the first time, try once more after 200 millis */
            try {
                Thread.sleep(200);
                return IOUtils.toString(getInputStreamFromUrl(url));
            } catch (InterruptedException e1) {
                throw new RuntimeException(e1);
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }
        }
    }

}
