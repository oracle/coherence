/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config;

/**
 * A {@link SchemeMappingRegistry} provides a mechanism manage a collection
 * of {@link ResourceMapping}s, together with the ability to search the registry for
 * said {@link ResourceMapping}s, possibly using wild-cards.
 * <p>
 * {@link ResourceMappingRegistry}s are {@link Iterable}, the order of iteration
 * being that in which the {@link ResourceMapping}s where added to the said
 * {@link ResourceMappingRegistry}.
 *
 * @author jk 2015.06.01
 * @since Coherence 14.1.1
 */
public interface ResourceMappingRegistry
        extends Iterable<ResourceMapping>
    {
    // ----- ResourceMappingRegistry methods ----------------------------------------

    /**
     * Registers a {@link ResourceMapping} with the {@link ResourceMappingRegistry}.
     *
     * @param mapping  the {@link ResourceMapping} to register
     *
     * @throws IllegalArgumentException if a {@link ResourceMapping} with the same
     *                                  pattern has already been registered
     */
    public void register(ResourceMapping mapping)
            throws IllegalArgumentException;

    /**
     * Attempts to find the {@link CacheMapping} that matches the specified
     * name and type.
     * <p>
     * The matching algorithm first attempts to find an exact match of a
     * {@link CacheMapping} with the provided name.  Should that fail,
     * all of the currently registered wild-carded {@link CacheMapping}s are
     * searched to find a match (in the order in which they were registered),
     * with the most specific (longest match) being returned if there are
     * multiple matches.
     *
     * @param sName the name
     *
     * @return <code>null</code> if a mapping could not be located
     *         for the specified name and type
     */
    public default CacheMapping findCacheMapping(String sName)
        {
        return findMapping(sName, CacheMapping.class);
        }

    /**
     * Attempts to find the {@link ResourceMapping} that matches the specified
     * name and type.
     * <p>
     * The matching algorithm first attempts to find an exact match of a
     * {@link ResourceMapping} with the provided name.  Should that fail,
     * all of the currently registered wild-carded {@link ResourceMapping}s are
     * searched to find a match (in the order in which they were registered),
     * with the most specific (longest match) being returned if there are
     * multiple matches.
     *
     * @param sName  the name
     * @param type   the type of the mapping to locate
     *
     * @return <code>null</code> if a mapping could not be located
     *         for the specified name and type
     */
    public <M extends ResourceMapping> M findMapping(String sName, Class<M> type);

    /**
     * Determines the number of {@link ResourceMapping}s in this {@link ResourceMappingRegistry}.
     *
     * @return the number of {@link ResourceMapping}s
     */
    public int size();
    }
