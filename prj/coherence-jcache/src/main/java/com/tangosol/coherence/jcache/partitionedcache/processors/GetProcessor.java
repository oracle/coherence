/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.jcache.partitionedcache.processors;

import com.tangosol.coherence.jcache.common.Helper;
import com.tangosol.coherence.jcache.common.JCacheContext;
import com.tangosol.coherence.jcache.common.JCacheEntryMetaInf;
import com.tangosol.coherence.jcache.common.JCacheIdentifier;
import com.tangosol.coherence.jcache.common.JCacheStatistics;

import com.tangosol.net.BackingMapManagerContext;
import com.tangosol.net.GuardSupport;

import com.tangosol.util.Binary;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.LiteMap;

import java.util.Map;
import java.util.Set;

/**
 * JCache Get Entry Processor
 *
 * Implements cache statistics, Accessed CacheEntryEvent and read-through.
 *
 * @param <K>  key type
 *
 * @author jf 2013.18.12
 * @since Coherence 12.1.3
 *
 */
public class GetProcessor<K>
        extends AbstractEntryProcessor
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs {@link GetProcessor}
     *
     */
    public GetProcessor()
        {
        super();
        }

    /**
     * Constructs a Get EntryProcessor
     *
     *
     * @param id  JCache unique cache identifier
     */
    public GetProcessor(JCacheIdentifier id)
        {
        super(id);
        }

    // ----- InvocableMap.EntryProcessor interface --------------------------

    @Override
    public Object process(InvocableMap.Entry entry)
        {
        long             ldtStart             = Helper.getCurrentTimeMillis();
        BinaryEntry      binEntry             = entry instanceof BinaryEntry ? (BinaryEntry) entry : null;
        boolean          fBinEntryOrigPresent = binEntry != null && binEntry.isPresent();
        JCacheContext    jcacheCtx            = BinaryEntryHelper.getContext(m_cacheId, binEntry);
        JCacheStatistics stats                = jcacheCtx.getStatistics();

        // Note if read-through is enabled, this call could cause loading.
        Binary binValue = binEntry.getBinaryValue();

        if (binEntry.isPresent())
            {
            BackingMapManagerContext ctx          = binEntry.getContext();
            JCacheEntryMetaInf       valueMetaInf = BinaryEntryHelper.getValueMetaInf(binEntry);

            if (BinaryEntryHelper.isExpired(binEntry, valueMetaInf, ldtStart))
                {
                BinaryEntryHelper.expireEntry(binEntry);

                if (BinaryEntryHelper.getContext(m_cacheId, binEntry).isReadThrough())
                    {
                    Object oValue = BinaryEntryHelper.getContext(m_cacheId,
                                        binEntry).getCacheLoader().load(binEntry.getKey());

                    if (oValue != null)
                        {
                        binValue = (Binary) binEntry.getContext().getValueToInternalConverter().convert(oValue);
                        }

                    valueMetaInf = new JCacheEntryMetaInf(ldtStart, jcacheCtx.getExpiryPolicy());

                    if (binValue == null || BinaryEntryHelper.isExpired(ctx, binValue, valueMetaInf, ldtStart))
                        {
                        stats.registerMisses(1, ldtStart);

                        return null;
                        }
                    }
                else
                    {
                    stats.registerMisses(1, ldtStart);

                    return null;
                    }
                }

            assert(valueMetaInf != null);

            valueMetaInf.accessed(ldtStart, jcacheCtx.getExpiryPolicy());

            binValue = BinaryEntryHelper.decorateBinValueWithJCacheMetaInf(binValue, valueMetaInf, ctx);
            binValue = BinaryEntryHelper.jcacheSyntheticUpdateEntry(binValue, ctx);

            // must update value due to decorate update of JCACHE metaInfo for value.  Should be synthetic update in
            // get. (not working the Coherence Update listener does get fired even though no change in user visible
            // value, only meta info change to reflect this access via get.
            // used to be updateBinaryValue(binValue, true (isSynthetic)) but 3.7.1 does not have this method.
            // adding isSynthetic by implementing in the adapter now since isSynthetic in 12.2.1 and up was not
            // sufficient for our use case.
            binEntry.updateBinaryValue(binValue);

            if (fBinEntryOrigPresent)
                {
                stats.registerHits(1, ldtStart);
                }
            else
                {
                // read-through when getting entry value caused entry to exist, so register this as a miss.
                stats.registerMisses(1, ldtStart);
                }
            }
        else
            {
            stats.registerMisses(1, ldtStart);
            }

        return binValue;
        }

    @Override
    public Map processAll(Set setEntries)
        {
        // adapted from AbstractProcessor
        Map<Object, Binary> mapResults = new LiteMap();

        for (Object entry : setEntries)
            {
            GuardSupport.heartbeat();

            BinaryEntry bEntry   = (BinaryEntry) entry;
            Binary      binValue = (Binary) process(bEntry);

            if (binValue != null)
                {
                // TODO: the following never gets to the other side...
                // TODO: The problem is if the key class is not on the server...
                // TODO: also seems to force us to deserialize the key
                // mapResults.put(bEntry.getBinaryKey(), binValue);
                mapResults.put(bEntry.getKey(), binValue);
                }
            }

        return mapResults;
        }
    }
