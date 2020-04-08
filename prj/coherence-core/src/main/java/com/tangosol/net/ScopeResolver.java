/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net;

/**
 * This interface is used to derive a scope name used to create an instance
 * of {@link ConfigurableCacheFactory}. This scope name is used as a prefix
 * to service names created by the {@link ConfigurableCacheFactory} which
 * enables consumers of the factory to isolate their services and caches
 * from other applications running in the same cluster.
 *
 * @author pp  2010.01.20
 *
 * @since Coherence 3.7
 */
public interface ScopeResolver
    {
    /**
     * Implementations of this method must decide whether to
     * <ul>
     * <li>return the scope name provided by the cache configuration</li>
     * <li>return a modified scope name or return a different scope name based on
     * external configuration</li>
     * <li>throw an exception indicating rejection of the requested scope
     * </ul>
     *
     * @param sConfigURI  the configuration URI
     * @param loader      class loader for which the configuration should be used
     * @param sScopeName  the scope name provided in the cache configuration; may be null
     *
     * @return scope name for the {@link ConfigurableCacheFactory} instance
     *         that will be created with the configuration specified by
     *         sConfigURI
     *
     * @throws IllegalArgumentException  if the requested scope name is rejected
     *                                   (for example if a duplicate scope name is detected)
     */
    public String resolveScopeName(String sConfigURI, ClassLoader loader, String sScopeName);
    }
