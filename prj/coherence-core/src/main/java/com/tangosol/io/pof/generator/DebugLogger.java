/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io.pof.generator;

import java.util.logging.Logger;

/**
 * Used by generated POF code to write log messages when
 * debug code generation is enabled.
 *
 * @author jk  2018.04.13
 */
public class DebugLogger
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Private constructor so that instances cannot be created.
     */
    private DebugLogger()
        {
        }

    // ----- DebugLogger methods --------------------------------------------

    /**
     * Log the specified message at {@link java.util.logging.Level#FINEST}.
     *
     * @param msg  the log message.
     */
    public static void log(String msg)
        {
        LOGGER.finest(msg);
        }

    // ----- constants ------------------------------------------------------

    /**
     * The {@link Logger} to use.
     */
    private static Logger LOGGER = Logger.getLogger(DebugLogger.class.getName());
    }
