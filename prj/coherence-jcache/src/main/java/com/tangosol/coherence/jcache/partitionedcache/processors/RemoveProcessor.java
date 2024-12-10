/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.jcache.partitionedcache.processors;

import com.tangosol.coherence.jcache.common.Helper;
import com.tangosol.coherence.jcache.common.JCacheIdentifier;

import com.tangosol.util.BinaryEntry;
import com.tangosol.util.InvocableMap;

/**
 * JCache RemoveProcessor Entry Processor - implements JCache write-through and cache statistics.
 * @param <K>  key type
 * @param <V>  value type
 *
 * @author jf  2013.12.18
 * @since Coherence 12.1.3
 */
public class RemoveProcessor<K, V>
        extends AbstractRemoveProcessor<K, V>
    {
    // ----- Constructor ----------------------------------------------------

    /**
     * Constructs RemoveProcessor for ExternalizableLite and POF.
     *
     */
    public RemoveProcessor()
        {
        super(null);
        }

    /**
     * Constructs JCache Remove Entry Processor
     *
     * @param id  unique JCache cache identifier
     */
    public RemoveProcessor(JCacheIdentifier id)
        {
        super(id);
        }

    // ----- InvocableMap.EntryProcessor interface --------------------------

    @Override
    public Object process(InvocableMap.Entry entry)
        {
        long        ldtStart = Helper.getCurrentTimeMillis();
        BinaryEntry binEntry = (BinaryEntry) entry;
        Boolean     fResult  = Boolean.FALSE;

        if (binEntry.isPresent())
            {
            // do not want a CoherenceRemoveListener fired if entry is expired.
            boolean syntheticRemove = BinaryEntryHelper.isExpired(binEntry, ldtStart);

            if (syntheticRemove)
                {
                BinaryEntryHelper.expireEntry(binEntry);
                deleteCacheEntry(binEntry);
                }
            else
                {
                entry.remove(false);
                BinaryEntryHelper.getContext(m_cacheId, binEntry).getStatistics().registerRemoves(1, ldtStart);
                fResult = Boolean.TRUE;
                }
            }
        else
            {
            deleteCacheEntry(binEntry);
            }

        return fResult;
        }
    }
