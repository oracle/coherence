/*
 * Copyright (c) 2021 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.config;

import java.util.List;

/**
 * A simple holder for the parsing result of an {@code coherence-concurrent} elements.
 * This holder will be scoped to the owning cache factory.
 *
 * @author rl  11.20.21
 * @since 21.12
 */
public class ConcurrentConfiguration
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Set the {@link List list} of parsed {@link NamedExecutorService}s.
     *
     * @param listNamedExecutorServices list of parsed {@link NamedExecutorService}
     */
    public ConcurrentConfiguration(List<NamedExecutorService> listNamedExecutorServices)
        {
        this.f_listNamedExecutorServices = listNamedExecutorServices;
        }

    // ----- accessors ------------------------------------------------------

    /**
     * Return the {@link List list} of parsed {@link NamedExecutorService}s.
     *
     * @return the {@link List list} of parsed {@link NamedExecutorService}s
     */
    public List<NamedExecutorService> getNamedExecutorServices()
        {
        return f_listNamedExecutorServices;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The {@link List list} of parsed {@link NamedExecutorService}s.
     */
    protected List<NamedExecutorService> f_listNamedExecutorServices;
    }
