/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.jcache.partitionedcache;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.coherence.jcache.common.CoherenceCacheEntryEvent;
import com.tangosol.coherence.jcache.common.CoherenceCacheEventEventDispatcher;
import com.tangosol.coherence.jcache.partitionedcache.processors.BinaryEntryHelper;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.net.BackingMapManagerContext;
import com.tangosol.net.cache.CacheEvent;

import com.tangosol.util.AbstractMapListener;
import com.tangosol.util.Binary;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.ConverterCollections;
import com.tangosol.util.MapEvent;
import com.tangosol.util.filter.EntryFilter;

import java.io.IOException;
import java.io.Serializable;

import java.util.Map;

import javax.cache.event.CacheEntryExpiredListener;
import javax.cache.event.EventType;

/**
 * MapListener for coherence cache to generate JCache ExpiryEvents.
 * The filter is a server-side one that requires to be able to rely on isSynthetic() having proper value.
 *
 * ExpiryEvents are only delivered asynchronously.
 *
 * @param <K> key type
 * @param <V> value type
 *
 * @author jf  2013.12.18
 * @version Coherence 12.1.3
 */
public class PartitionedCacheSyntheticDeleteMapListener<K, V>
        extends AbstractMapListener
    {
    // ------ constructors --------------------------------------------------

    /**
     * Constructs {@link PartitionedCacheSyntheticDeleteMapListener} for {@link PartitionedCache}
     *
     *
     * @param sDescription user description of handler
     * @param cache        source JCache
     */
    PartitionedCacheSyntheticDeleteMapListener(String sDescription, PartitionedCache cache)
        {
        f_description = sDescription;
        m_cache       = cache;
        }

    // ------ AbstractMapListener methods -----------------------------------

    // next line is commented out since the original implementation was relying on client-side isSynthetic to work
    // and it did not.  So that was replaced with JCache Synthetic decoration that allowed the JCache adapter
    // to decorate what type of synthetic update was performed on the entry.  This allowed for special case
    // handling of CLEAR, EXPIRY, LOADED, REUSE_OF_EXPIRED_ENTRY_BY_NEW_PUT.  So now that isSynthetic was
    // fixed in Coherence to propagate to client side listener, it might not be enough to replace the
    // current functionaliy being used in JCACHE_SYNTHETIC decoration.

    /*
     * @Override
     * public void entryDeleted(MapEvent mapEvent)
     *   {
     *   CoherenceCacheEventEventDispatcher<K, V> dispatcher = new CoherenceCacheEventEventDispatcher<K, V>();
     *   CoherenceCacheEntryEvent<K, V> entryExpired = new CoherenceCacheEntryEvent<K, V>(m_cacheSource, EventType.EXPIRED,
     *                                                     (K) mapEvent.getKey(), (V) mapEvent.getOldValue());
     *
     *   dispatcher.addEvent(CacheEntryExpiredListener.class, entryExpired);
     *   dispatcher.dispatch(m_cacheSource.getRegisteredEventListeners());
     *   }
     */

    /**
     * These updated mapEvents were decorated with JACHE_SYNTHETIC_EXPIRY or JCACHE_SYNTHETIC_EXPIRY_EVENT_FOR_ORIGINAL_VALUES.
     * Filter removed all non JCACHE_SYNTHETIC_EXPIRY or JCACHE_SYNTHETIC_EXPIRY_EVENT_FOR_ORIGINAL_VALUES.
     */
    @Override
    public void entryUpdated(MapEvent evt)
        {
        CoherenceCacheEventEventDispatcher<K, V> dispatcher = new CoherenceCacheEventEventDispatcher<K, V>();

        CoherenceCacheEntryEvent<K, V> expiredEntry = new CoherenceCacheEntryEvent<K, V>(m_cache, EventType.EXPIRED,
                                                          (K) evt.getKey(), null, (V) evt.getOldValue());

        dispatcher.addEvent(CacheEntryExpiredListener.class, expiredEntry);

        // ExpiryEvents are only delivered asynchronously, so deliver to all listeners.
        dispatcher.dispatch(m_cache.getRegisteredSynchronousEventListeners());
        dispatcher.dispatch(m_cache.getRegisteredAsynchronousEventListeners());

        }

    // ----- Object methods ------------------------------------------------

    @Override
    public String toString()
        {
        return this.getClass().getSimpleName() + " cacheName=" + (m_cache == null ? "" : m_cache.getName())
               + " description=" + f_description;
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Get JCACHE_SYNTHETHIC kind for this entry
     *
     * @param binEntry binary entry
     *
     * @return null if no JCACHE_SYNTHETIC decoration
     */
    private static Byte getJCacheSynthetic(BinaryEntry binEntry)
        {
        assert(binEntry != null);

        BackingMapManagerContext ctx      = binEntry.getContext();
        Binary                   binValue = binEntry.getBinaryValue();

        assert(ctx != null);

        if (binValue == null)
            {
            return null;
            }

        return BinaryEntryHelper.getJCacheSyntheticKind(binValue, ctx);
        }

    /**
     * Server side filter for JCache ExpiryCacheEvents.
     */
    public static class JCacheExpiryEntryFilter<T>
            implements EntryFilter<Object, T>, Serializable, PortableObject
        {
        @Override
        public boolean evaluate(T o)
            {
            boolean fResult = false;

//          Leave this in just in case when Coherence expiry with 1 millisecond is treated immediately, perhaps
//          this mode can be considered again.

//          UPDATE: expiry(1L) being considered expired immediately was not implemented.
//                  only Coherence bug fix was that isSynthetic is not only server-side and it can be inspected
//                  on client-side CacheEventEntry handlers.  Still leaving this original code here in case
//                  we consider using Coherence isSynthetic in future.
//                   if (o instanceof CacheEvent && ((CacheEvent) o).isSynthetic())
//                       {
//                         // synthetic delete is actual expiration.
//                         // in order to avoid double counting both the update with JCacheSyntheticExpiry and the
//                         // actual Coherence synthetic delete, never count synthetic deletes at this time.
//                       fResult = false;
//                       }

            if (o instanceof ConverterCollections.ConverterMapEvent)
                {
                ConverterCollections.ConverterMapEvent cme = (ConverterCollections.ConverterMapEvent) o;

                if (cme.getId() == CacheEvent.ENTRY_UPDATED)
                    {
                    BinaryEntry binEntry            = (BinaryEntry) cme.getNewEntry();
                    Byte        jcacheSyntheticKind = getJCacheSynthetic(binEntry);

                    fResult = jcacheSyntheticKind == null
                              ? false
                              : BinaryEntryHelper.JCACHE_SYNTHETIC_EXPIRY.equals(jcacheSyntheticKind)
                                || BinaryEntryHelper.JCACHE_SYNTHETIC_EXPIRY_EVENT_FOR_ORIGINAL_VALUES.equals(
                                    jcacheSyntheticKind);

                    if (fResult)
                        {
                        Logger.fine(() -> "jacheSyntheticKind=" + jcacheSyntheticKind + " event=" + o);
                        }
                    }
                }

            return fResult;
            }

        @Override
        public boolean evaluateEntry(Map.Entry entry)
            {
            return true;
            }

        @Override
        public void readExternal(PofReader pofReader)
                throws IOException
            {
            }

        @Override
        public void writeExternal(PofWriter pofWriter)
                throws IOException
            {
            }
        }

    // ------ data members --------------------------------------------------
    private final String           f_description;
    private final PartitionedCache m_cache;
    }
