/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config;

import java.util.Iterator;

import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * A {@link CacheMappingRegistry} provides a mechanism manage a collection
 * of {@link CacheMapping}s, together with the ability to search the registry for
 * said {@link CacheMapping}s, possibly using wild-cards.
 * <p>
 * {@link CacheMappingRegistry}s are {@link Iterable}, the order of iteration
 * being that in which the {@link CacheMapping}s where added to the said
 * {@link CacheMappingRegistry}.
 *
 * @author bo  2011.06.29
 * @since Coherence 12.1.2
 *
 * @deprecated  As Coherence 14.1.1, use {@link ResourceMappingRegistry}.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class CacheMappingRegistry
        implements Iterable<CacheMapping>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a {@link CacheMappingRegistry}.
     */
    public CacheMappingRegistry()
        {
        f_registry = new SchemeMappingRegistry();
        }

    /**
     * CacheMappingRegistry delegates to {@link ResourceMappingRegistry}.
     *
     * @param registry delegate resource registry containing CacheMapping
     *
     * @since Coherence 14.1.1
     */
    public CacheMappingRegistry(ResourceMappingRegistry registry)
        {
        f_registry = registry;
        }

    // ----- Iterable interface ---------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<CacheMapping> iterator()
        {
        Stream stream = StreamSupport.stream(f_registry.spliterator(), false).filter(e -> e instanceof CacheMapping);
        return stream.iterator();
        }

    // ----- CacheMappingRegistry methods -----------------------------------

    /**
     * Registers a {@link CacheMapping} with the {@link CacheMappingRegistry}.
     *
     * @param cacheMapping  the {@link CacheMapping} to register
     *
     * @throws IllegalArgumentException if a {@link CacheMapping} with the same
     *                                  pattern has already been registered
     */
    public void register(CacheMapping cacheMapping)
            throws IllegalArgumentException
        {
        if (findCacheMapping(cacheMapping.getNamePattern()) == null)
            {
            f_registry.register(cacheMapping);
            }
        else
            {
            throw new IllegalArgumentException(String.format(
                "Attempted to redefined an existing cache mapping for the <cache-name>%s</cache-name>",
                cacheMapping.getNamePattern()));
            }
        }

    /**
     * Attempts to find the {@link CacheMapping} that matches the specified
     * cache name.
     * <p>
     * The matching algorithm first attempts to find an exact match of a
     * {@link CacheMapping} with the provided cache name.  Should that fail,
     * all of the currently registered wild-carded {@link CacheMapping}s are
     * searched to find a match (in the order in which they were registered),
     * with the most specific (longest match) being returned if there are
     * multiple matches.
     *
     * @param sCacheName  the cache name
     *
     * @return <code>null</code> if a {@link CacheMapping} could not be located
     *         for the specified cache name
     */
    public CacheMapping findCacheMapping(String sCacheName)
        {
        return f_registry.findCacheMapping(sCacheName);
        }

    /**
     * Determines the number of {@link CacheMapping}s in the {@link CacheMappingRegistry}.
     *
     * @return the number of {@link CacheMapping}s
     */
    public int size()
        {
        return (int) StreamSupport.stream(f_registry.spliterator(), false).filter(e -> e instanceof CacheMapping).count();
        }

    /**
     * Get the underlying {@link ResourceMappingRegistry}.
     *
     * @return underlying registry
     */
    public ResourceMappingRegistry getMappingRegistry()
        {
        return f_registry;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The resource mapping registry containing {@link CacheMapping}s and other {@link ResourceMapping}s.
     */
    private final ResourceMappingRegistry f_registry;
    }
