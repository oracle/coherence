/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.jcache.partitionedcache.processors;

import com.tangosol.coherence.jcache.common.Helper;
import com.tangosol.coherence.jcache.common.JCacheEntryMetaInf;
import com.tangosol.coherence.jcache.common.JCacheIdentifier;
import com.tangosol.coherence.jcache.common.JCacheStatistics;

import com.tangosol.util.Binary;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.InvocableMap;

/**
 * JCache GetAndRemove Entry Processor
 *
 * @param <K> key type
 * @param <V> value type
 *
 * @author jf  2013.12.18
 * @since Coherence 12.1.3
 */
public class GetAndRemoveProcessor<K, V>
        extends AbstractRemoveProcessor<K, V>
    {
    // ----- Constructors ---------------------------------------------------

    /**
     * Constructs ...
     *
     */
    public GetAndRemoveProcessor()
        {
        super(null);
        }

    /**
     * Constructs a JCache GetAndRemove Entry Processor
     *
     *
     * @param id  a JCache unique identifier
     */
    public GetAndRemoveProcessor(JCacheIdentifier id)
        {
        super(id);
        }

    // ----- InvocableMap.EntryProcessor interface --------------------------

    @Override
    public Object process(InvocableMap.Entry entry)
        {
        long        ldtStart = Helper.getCurrentTimeMillis();
        BinaryEntry binEntry = (BinaryEntry) entry;
        Binary      binValue = binEntry.isPresent() ? binEntry.getBinaryValue() : null;

        if (binEntry.isPresent())
            {
            JCacheEntryMetaInf valueMetaInf = BinaryEntryHelper.getValueMetaInf(binEntry);

            if (BinaryEntryHelper.isExpired(binEntry, valueMetaInf, ldtStart))
                {
                binValue = null;
                BinaryEntryHelper.expireEntry(binEntry);
                deleteCacheEntry(binEntry);
                }
            else
                {
                binEntry.remove(false);
                }
            }
        else
            {
            deleteCacheEntry(binEntry);
            }

        JCacheStatistics stats = BinaryEntryHelper.getContext(m_cacheId, binEntry).getStatistics();

        if (binValue == null)
            {
            stats.registerMisses(1, ldtStart);
            }
        else
            {
            stats.registerHits(1, ldtStart);
            stats.registerRemoves(1, ldtStart);
            }

        return binValue;
        }
    }
