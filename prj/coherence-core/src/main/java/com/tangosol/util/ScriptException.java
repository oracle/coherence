/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util;

/**
 * Represents an exception of some sort has occurred while loading or
 * executing scripts.
 *
 * @author mk 2019.07.26
 * @since 14.1.1.0
 */
public class ScriptException
        extends RuntimeException
    {
    // ------ constructors ----------------------------------------------------

    /**
     * Create an instance of {@code ScriptException} with the specified
     * message.
     *
     * @param sMessage  the detail message (which is saved for later retrieval
     *                  by the {@link #getMessage()} method)
     */
    public ScriptException(String sMessage)
        {
        super(sMessage);
        }

    /**
     * Create an instance of {@code ScriptException} with the specified
     * message and cause.
     *
     * @param  sMessage  the detail message (which is saved for later retrieval
     *                   by the {@link #getMessage()} method)
     * @param  tCause    the cause (which is saved for later retrieval by the
     *                   {@link #getCause()} method).  (A {@code null} value is
     *                   permitted, and indicates that the cause is nonexistent or
     *                   unknown)
     */
    public ScriptException(String sMessage, Throwable tCause)
        {
        super(sMessage, tCause);
        }
    }
