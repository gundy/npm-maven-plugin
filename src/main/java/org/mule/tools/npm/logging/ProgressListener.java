/**
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tools.npm.logging;

import org.apache.maven.plugin.logging.Log;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ProgressListener implements ActionListener {

    private Log log;

    public ProgressListener(Log log) {
        this.log = log;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        this.log.debug("Downloaded bytes : " + ((DownloadCountingOutputStream) e.getSource()).getByteCount());
    }
}
