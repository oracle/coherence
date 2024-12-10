/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest.query;

import com.tangosol.net.NamedCache;

import com.tangosol.util.ValueExtractor;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Defines an interface that Query implementations must support.
 *
 * @author as  2012.01.19
 */
public interface Query
    {
    /**
     * Return the values that satisfy this query.
     *
     * @param <E>       the element type
     * @param cache     the cache to be queried (filtered)
     * @param extractor the extractor to apply to each entry in the result set
     * @param sOrder    ordering expression (see
     *                  {@link com.tangosol.coherence.rest.util.ComparatorHelper}
     *                  for details)
     * @param nStart    the start index
     * @param cResults  the size of the result set to be returned
     *
     * @return the values that satisfy query criteria
     *
     * @throws QueryException if any error occurs during query execution
     */
    public <E> Collection<E> execute(NamedCache cache, ValueExtractor<Map.Entry, ? extends E> extractor, String sOrder, int nStart, int cResults);

    /**
     * Return the values that satisfy this query.
     *
     * @param cache     cache to be queried (filtered)
     * @param sOrder    ordering expression (see
     *                  {@link com.tangosol.coherence.rest.util.ComparatorHelper}
     *                  for details)
     * @param nStart    start index
     * @param cResults  size of the result set to be returned
     *
     * @return the values that satisfy query criteria
     *
     * @throws QueryException if any error occurs during query execution
     *
     * @deprecated As of Coherence 12.2.1. Use {@link #execute(NamedCache, ValueExtractor, String, int, int)} instead.
     */
    @Deprecated
    public default Collection values(NamedCache cache, String sOrder, int nStart, int cResults)
        {
        return execute(cache, Map.Entry::getValue, sOrder, nStart, cResults);
        }

    /**
     * Return the keys that satisfy this query.
     *
     * @param cache  cache to be queried (filtered)
     *
     * @return the keys that satisfy query criteria
     *
     * @throws QueryException if any error occurs during query execution
     */
    public Set keySet(NamedCache cache);
    }
