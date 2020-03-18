/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.sun.tools.visualvm.modules.coherence.panel.util;

import java.awt.event.ActionListener;

import javax.management.MBeanServerConnection;

/**
 * Defines an individual right-click menu option which can be applied to an
 * {@link ExportableJTable}.
 *
 * @author tam  2014.02.17
 * @since  12.1.3
 */
public interface MenuOption
        extends ActionListener
    {
    /**
     * Return the name of the menu option.
     *
     * @return the name of the menu option
     */
    public String getMenuItem();

    /**
     * Set the menu label to override the default of "Show details
     *
     * @param sMenuLabel  the label to use
     */
    public void setMenuLabel(String sMenuLabel);
    }
