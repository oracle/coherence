/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.common.schema.util;

/**
 * @author as  2014.10.15
 */
public class Logger
    {
    private static final java.util.logging.Logger LOG = java.util.logging.Logger.getLogger(Logger.class.getName());

    public static void error(String sMessage)
        {
        LOG.severe(sMessage);
        }

    public static void warn(String sMessage)
        {
        LOG.warning(sMessage);
        }

    public static void info(String sMessage)
        {
        LOG.info(sMessage);
        }

    public static void debug(String sMessage)
        {
        LOG.finer(sMessage);
        }
    }
