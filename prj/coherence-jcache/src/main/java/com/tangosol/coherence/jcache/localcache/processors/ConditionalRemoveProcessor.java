/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.jcache.localcache.processors;

import com.tangosol.coherence.jcache.common.Helper;
import com.tangosol.coherence.jcache.common.JCacheStatistics;
import com.tangosol.coherence.jcache.localcache.LocalCache;
import com.tangosol.coherence.jcache.localcache.LocalCacheValue;

import com.tangosol.util.InvocableMap;

/**
 * An {@link com.tangosol.util.InvocableMap.EntryProcessor} to
 * remove an entry if it has a specified value.
 *
 * @author jf  2013.11.12
 * @since Coherence 12.1.3
 */
public class ConditionalRemoveProcessor<K, V>
        extends AbstractEntryProcessor
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a {@link com.tangosol.coherence.jcache.localcache.processors.ConditionalRemoveProcessor}.
     *
     * @param expectedValue internal form of expected value
     */
    public ConditionalRemoveProcessor(LocalCache cache, Object expectedValue)
        {
        super(cache);

        if (expectedValue == null)
            {
            throw new NullPointerException("expectedValue can't be null");
            }

        m_internalExpectedValue = expectedValue;
        }

    // ----- EntryProcessor interface ---------------------------------------

    @Override
    public Object process(InvocableMap.Entry entry)
        {
        boolean fResult            = false;
        boolean fStatisticsEnabled = isStatisticsEnabled();
        long    ldtNow             = Helper.getCurrentTimeMillis();
        long    ldtStart           = fStatisticsEnabled ? Helper.getCurrentTimeMillis() : 0L;
        int     hitCount           = 0;

        try
            {
            if (entry.isPresent())
                {
                LocalCacheValue cachedValue = (LocalCacheValue) entry.getValue();
                boolean         fIsExpired  = cachedValue != null && cachedValue.isExpiredAt(ldtNow);

                if (cachedValue == null || fIsExpired)
                    {
                    // entry expired from JCache expiry.

                    fResult = false;

                    if (fIsExpired)
                        {
                        processExpiries(entry.getKey());
                        }

                    }
                else if (m_internalExpectedValue.equals(cachedValue.get()))
                    {
                    hitCount++;
                    deleteCacheEntry(fromInternalKey(entry.getKey()));
                    fResult = true;
                    entry.remove(false);
                    }
                else
                    {
                    accessLocalCacheValue(cachedValue, ldtNow);
                    entry.setValue(cachedValue);
                    hitCount++;
                    }
                }
            }
        finally
            {
            if (fStatisticsEnabled)
                {
                JCacheStatistics stats = getJCacheStatistics();

                if (fResult)
                    {
                    stats.registerRemoves(1, ldtStart);
                    }

                if (hitCount == 1)
                    {
                    stats.registerHits(1, ldtStart);

                    }
                else
                    {
                    stats.registerMisses(1, ldtStart);
                    }
                }
            }

        return fResult;
        }

    // ------ data members --------------------------------------------------

    private Object m_internalExpectedValue;
    }
