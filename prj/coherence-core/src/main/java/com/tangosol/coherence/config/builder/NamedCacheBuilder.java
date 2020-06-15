/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.builder;

import com.tangosol.config.expression.ParameterResolver;

import com.tangosol.net.NamedCache;

import java.util.Map;

/**
 * A {@link NamedCacheBuilder} realizes {@link NamedCache}s.
 *
 * @author pfm  2011.12.27
 * @since Coherence 12.1.2
 */
public interface NamedCacheBuilder
    {
    /**
     * Realizes a {@link NamedCache} (possibly "ensuring it") based on the state
     * of the builder, the provided {@link ParameterResolver} and {@link MapBuilder}
     * dependencies.
     * <p>
     * The {@link MapBuilder} dependencies are required to satisfy the requirement
     * when realizing a {@link NamedCache} additionally involves realizing one
     * or more internal {@link Map}s.
     *
     * @param resolver      the ParameterResolver
     * @param dependencies  the {@link MapBuilder} dependencies
     *
     * @return a {@link NamedCache}
     */
    public NamedCache realizeCache(ParameterResolver resolver, MapBuilder.Dependencies dependencies);
    }
