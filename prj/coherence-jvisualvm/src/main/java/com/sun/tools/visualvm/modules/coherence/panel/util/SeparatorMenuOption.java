/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.sun.tools.visualvm.modules.coherence.panel.util;

import com.sun.tools.visualvm.modules.coherence.VisualVMModel;

import com.sun.tools.visualvm.modules.coherence.helper.RequestSender;
import java.awt.event.ActionEvent;

import javax.management.MBeanServerConnection;

/**
 * A Placeholder for adding a separator in the list of menu options.
 */
public class SeparatorMenuOption
        extends AbstractMenuOption
    {
    /**
     * {@inheritDoc}
     */
    public SeparatorMenuOption(VisualVMModel model, RequestSender requestSender, ExportableJTable jtable)
        {
        super(model, requestSender, jtable);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMenuItem()
        {
        return null;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(ActionEvent actionEvent)
        {
        // NOOP
        }
    }
