/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.jcache.localcache.processors;

import com.tangosol.coherence.jcache.common.CoherenceCacheEntry;
import com.tangosol.coherence.jcache.common.Helper;
import com.tangosol.coherence.jcache.common.JCacheStatistics;
import com.tangosol.coherence.jcache.localcache.LocalCache;
import com.tangosol.coherence.jcache.localcache.LocalCacheValue;

import com.tangosol.util.InvocableMap;

/**
 * An {@link com.tangosol.util.InvocableMap.EntryProcessor} to
 * replace a specific existing value with another value, returning
 * the existing value.
 *
 * @author jf  2013.11.11
 * @since Coherence 12.1.3
 */
public class GetAndReplaceProcessor<K, V>
        extends AbstractEntryProcessor<K, V, V>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a {@link com.tangosol.coherence.jcache.localcache.processors.GetAndReplaceProcessor}.
     *
     * @param cache target JCache
     * @param internalNewValue replacement value if entry exists
     */
    public GetAndReplaceProcessor(LocalCache cache, Object internalNewValue)
        {
        super(cache);
        m_internalNewValue = internalNewValue;
        }

    // ----- EntryProcessor interface ---------------------------------------

    @Override
    public Object process(InvocableMap.Entry entry)
        {
        Object  exisitingValue     = null;
        boolean fStatisticsEnabled = isStatisticsEnabled();
        long    ldtNow             = Helper.getCurrentTimeMillis();
        long    ldtStart           = fStatisticsEnabled ? ldtNow : 0L;

        try
            {
            if (entry.isPresent())
                {
                LocalCacheValue cachedValue = (LocalCacheValue) entry.getValue();
                boolean         fIsExpired  = cachedValue != null && cachedValue.isExpiredAt(ldtNow);

                if (cachedValue == null || fIsExpired)
                    {
                    // entry expired from JCache expiry.

                    exisitingValue = null;

                    if (fIsExpired)
                        {
                        processExpiries(entry.getKey());
                        }
                    }
                else
                    {
                    exisitingValue = cachedValue.getInternalValue(ldtNow);

                    CoherenceCacheEntry<K, V> entryExternal = new CoherenceCacheEntry<K,
                                                                  V>(fromInternalKey(entry.getKey()),
                                                                     fromInternalValue(m_internalNewValue));

                    writeCacheEntry(entryExternal);
                    entry.setValue(updateLocalCacheValue(cachedValue, m_internalNewValue, ldtNow));
                    }
                }
            }
        finally
            {
            if (fStatisticsEnabled)
                {
                JCacheStatistics stats = getJCacheStatistics();

                if (exisitingValue == null)
                    {
                    stats.registerMisses(1, ldtStart);
                    }
                else
                    {
                    stats.registerPuts(1, ldtStart);
                    stats.registerHits(1, ldtStart);
                    }
                }
            }

        return exisitingValue;
        }

    // ------ data members --------------------------------------------------

    /**
     * The replacement value for entry.
     */
    private Object m_internalNewValue;
    }
