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

    /**
     * Resolve the URI that identifies the cache configuration.  The URI provided
     * may be a normal URL or Resource, or it may be a "special" default URI that
     * is used when a specific cache configuration file is not indicated (for
     * example, if the user requests a factory via {@link CacheFactory#getConfigurableCacheFactory()}.
     * If the "default" URI is requested, the URI is resolved to the default
     * cache configuration name indicated in the operational configuration file;
     * otherwise the provided URI is returned.
     *
     * @param sConfigURI  the passed in URI
     *
     * @return the resolved URI
     *
     * @see CacheFactoryBuilder#URI_DEFAULT
     */
    default String resolveURI(String sConfigURI)
        {
        return sConfigURI;
        }

    /**
     * Returns {@code true} if any scope set in the defaults section
     * of the XML configuration file should take precedence over
     * any scope decoded from the URI.
     *
     * @return {@code true} to use any scope defined in the XML configuration file
     */
    default boolean useScopeInConfig()
        {
        return true;
        }

    /**
     * A default implementation of a {@link ScopeResolver}
     * that always returns passed in scope name.
     */
    ScopeResolver INSTANCE = (sConfigURI, loader, sScopeName) -> sScopeName;
    }
