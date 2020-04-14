/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.partition;


import com.tangosol.net.NamedCache;

import com.tangosol.util.Filter;

import com.tangosol.util.filter.PartitionedFilter;

import java.util.Map;


/**
* An Iterator that iterates over keys in a partition-by-partition or
* member-by-member manner.
*
* @author cp  2009.04.07
* @since Coherence 3.5
*/
public class PartitionedIterator<T>
        extends AbstractPartitionedIterator<T>
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct PartitionedIterator that will provide iteration of the
    * keys of the specified cache using the specified filter, but will
    * only query one partition or one member at a time.
    *
    * @param cache     the cache to query
    * @param filter    the query expressed as a Filter
    * @param setPids   the partitions to execute the query against
    * @param nOptions  pass a bit-or'd combination of any of the
    *                  <tt>OPT_*</tt> constants
    */
    public PartitionedIterator(NamedCache cache, Filter filter,
            PartitionSet setPids, int nOptions)
        {
        super(filter, cache, setPids, (nOptions & OPT_BY_MEMBER) != 0, (nOptions & OPT_RANDOMIZED) != 0);
        m_fKeysOnly = (nOptions & OPT_ENTRIES) == 0;
        }

    // ---- AbstractPartitionedIterator methods -----------------------------

    @Override
    protected Iterable<T> nextIterable(PartitionedFilter filter)
        {
        return m_fKeysOnly ? m_cache.keySet(filter) : m_cache.entrySet(filter);
        }

    // ----- AbstractStableIterator methods ---------------------------------

    @Override
    protected void remove(Object oPrev)
        {
        m_cache.keySet().remove(m_fKeysOnly ? oPrev : ((Map.Entry) oPrev).getKey());
        }

    // ----- constants ------------------------------------------------------

    /**
    * An option to iterate the Map keys. This is the default.
    */
    public static final int OPT_KEYS            = 0;

    /**
    * An option to iterate Map Entry objects.
    */
    public static final int OPT_ENTRIES         = 1;

    // ---- data members ----------------------------------------------------

    /**
    * Differentiates between a key iterator (true) and an entry iterator
    * (false).
    */
    protected boolean m_fKeysOnly;
    }
