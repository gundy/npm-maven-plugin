/**
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tools.npm.logging;

import org.apache.commons.io.output.CountingOutputStream;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.OutputStream;

public class DownloadCountingOutputStream extends CountingOutputStream {

    private ActionListener listener = null;

    public DownloadCountingOutputStream(OutputStream out) {
        super(out);
    }

    public void setListener(ActionListener listener) {
        this.listener = listener;
    }

    @Override
    protected void afterWrite(int n) throws IOException {
        super.afterWrite(n);
        if (listener != null) {
            listener.actionPerformed(new ActionEvent(this, 0, null));
        }
    }

}
