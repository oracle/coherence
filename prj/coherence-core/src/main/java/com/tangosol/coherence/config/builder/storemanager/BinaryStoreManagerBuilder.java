/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.builder.storemanager;

import com.tangosol.config.expression.ParameterResolver;
import com.tangosol.io.BinaryStoreManager;


/**
 * A {@link BinaryStoreManagerBuilder} is responsible for realizing {@link BinaryStoreManager}s.
 *
 * @author pfm  2011.11.30
 * @since Coherence 12.1.2
 */
public interface BinaryStoreManagerBuilder
    {
    /**
     * Realize a {@link BinaryStoreManager} given the provided parameters.
     *
     * @param resolver  the {@link ParameterResolver} for resolving expressions and runtime parameters
     * @param loader    the {@link ClassLoader} for loading classes (if necessary)
     * @param fPaged    the flag indicating whether the map is paged
     *
     * @return a {@link BinaryStoreManager}
     */
    public BinaryStoreManager realize(ParameterResolver resolver, ClassLoader loader, boolean fPaged);
    }
