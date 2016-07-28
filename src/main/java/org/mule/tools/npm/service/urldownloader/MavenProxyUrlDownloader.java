/**
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tools.npm.service.urldownloader;

import org.apache.maven.settings.Proxy;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLConnection;

final class MavenProxyUrlDownloader extends UrlDownloader {
    private final Proxy proxy;

    MavenProxyUrlDownloader(Proxy proxy) {
        if (proxy == null) {
            throw new IllegalArgumentException("Proxy Url Downloader requires a proxy");
        }
        this.proxy = proxy;
    }

    public InputStream getInputStreamFromUrl(final URL url) throws IOException {
        final String proxyUser = proxy.getUsername();
        final String proxyPassword = proxy.getPassword();
        final String proxyAddress = proxy.getHost();
        final int proxyPort = proxy.getPort();

        java.net.Proxy.Type proxyProtocol = java.net.Proxy.Type.DIRECT;
        if (proxy.getProtocol() != null && proxy.getProtocol().equalsIgnoreCase("HTTP")) {
            proxyProtocol = java.net.Proxy.Type.HTTP;
        } else if (proxy.getProtocol() != null && proxy.getProtocol().equalsIgnoreCase("SOCKS")) {
            proxyProtocol = java.net.Proxy.Type.SOCKS;
        }

        final InetSocketAddress sa = new InetSocketAddress(proxyAddress, proxyPort);
        final java.net.Proxy jproxy = new java.net.Proxy(proxyProtocol, sa);

        URLConnection conn = url.openConnection(jproxy);

        if (proxyUser != null && proxyUser != "") {
            @SuppressWarnings("restriction")
            final sun.misc.BASE64Encoder encoder = new sun.misc.BASE64Encoder();
            @SuppressWarnings("restriction")
            final String encodedUserPwd = encoder.encode((proxyUser + ":" + proxyPassword).getBytes());
            conn.setRequestProperty("Proxy-Authorization", "Basic " + encodedUserPwd);
        }
        return conn.getInputStream();
    }

}
