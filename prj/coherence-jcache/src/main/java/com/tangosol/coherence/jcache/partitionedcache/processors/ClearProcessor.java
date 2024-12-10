/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.jcache.partitionedcache.processors;

import com.tangosol.util.BinaryEntry;
import com.tangosol.util.InvocableMap;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * JCache Clear EntryProcessor
 *
 * There must not be a CacheEntryEvent generated for a cleared entry and no CacheWriter delete should be called.
 *
 * @author rhl 2013.05.15
 * @since Coherence 12.1.3
 */
public class ClearProcessor
        extends AbstractEntryProcessor
    {
    // ----- InvocableMap.EntryProcessor interface --------------------------

    /**
     * evict an entry from the cache without causing a coherence removed event
     *
     * @param entry expiring entry
     *
     * @return nothing
     */
    public Object process(InvocableMap.Entry entry)
        {
        clearEntry((BinaryEntry) entry);

        return null;
        }

    /**
     * evict all entries from the cache without causing a coherence removed event
     *
     * @param setEntries to evict
     *
     * @return nothing
     */
    public Map processAll(Set setEntries)
        {
        Set<BinaryEntry> setBinEntry = (Set<BinaryEntry>)setEntries;
        for (BinaryEntry binEntry : setBinEntry )
            {
            clearEntry(binEntry);
            }

        return Collections.EMPTY_MAP;
        }

    // ----- constants ------------------------------------------------------

    /** 
     * singleton
     */
    static final public ClearProcessor INSTANCE = new ClearProcessor();
    }
