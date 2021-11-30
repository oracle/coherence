/*
 * Copyright (c) 2021 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.config;

import java.util.concurrent.ExecutorService;

import java.util.function.Supplier;

/**
 * A holder for a named {@link ExecutorService}.
 *
 * @author rl  11.20.21
 * @since 21.12
 */
public class NamedExecutorService
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a new {@link NamedExecutorService}.  This is a simple wrapper
     * around an executor's logical name and a supplier that produces the
     * {@link ExecutorService} itself.
     *
     * @param sName         the logical {@link ExecutorService} name
     * @param sDescription  the description of the {@link ExecutorService}
     * @param supplier      the {@link Supplier} that will produce the
     *                      {@link ExecutorService}
     */
    public NamedExecutorService(String sName, String sDescription, Supplier<ExecutorService> supplier)
        {
        f_sName        = sName;
        f_sDescription = sDescription;
        f_supplier     = supplier;
        }

    // ----- accessors ------------------------------------------------------

    /**
     * Returns the {@link ExecutorService} name.
     *
     * @return the {@link ExecutorService} name
     */
    public String getName()
        {
        return f_sName;
        }

    /**
     * Returns the description of this {@link ExecutorService}.
     *
     * @return the description of this {@link ExecutorService}
     */
    public String getDescription()
        {
        return f_sDescription;
        }

    /**
     * The {@link ExecutorService} associated with {@link #getName()}.
     *
     * @return {@link ExecutorService} associated with {@link #getName()}
     */
    public ExecutorService getExecutorService()
        {
        return f_supplier.get();
        }

    // ----- data members ---------------------------------------------------

    /**
     * The {@link ExecutorService} name.
     */
    protected final String f_sName;

    /**
     * The description of this {@link ExecutorService}.
     */
    protected final String f_sDescription;

    /**
     * The {@link Supplier} to create the {@link ExecutorService}.
     */
    protected final Supplier<ExecutorService> f_supplier;
    }
