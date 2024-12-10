/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.jcache.partitionedcache;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.coherence.jcache.common.Helper;
import com.tangosol.coherence.jcache.common.JCacheContext;
import com.tangosol.coherence.jcache.common.JCacheEntryMetaInf;
import com.tangosol.coherence.jcache.common.JCacheIdentifier;
import com.tangosol.coherence.jcache.partitionedcache.processors.BinaryEntryHelper;

import com.tangosol.net.BackingMapManagerContext;

import com.tangosol.net.cache.BinaryEntryStore;

import com.tangosol.util.Binary;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.Converter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.util.concurrent.atomic.AtomicReference;

import javax.cache.Cache;

import javax.cache.expiry.ExpiryPolicy;

import javax.cache.integration.CacheLoader;
import javax.cache.integration.CacheLoaderException;
import javax.cache.integration.CacheWriter;
import javax.cache.integration.CacheWriterException;

/**
 * Generic Coherence BinaryEntryStore for Coherence JCache Adapter.
 *
 * @param <K> key type
 * @param <V> value type
 *
 * @version Coherence 12.1.3
 * @author jf  2013.07.08
 */
public class PartitionedCacheBinaryEntryStore<K, V>
        implements BinaryEntryStore
    {
    // ----- Constructors ---------------------------------------------------

    /**
     * Construct a native Coherence CacheStore which implements JCache read-through and write-through semantics.
     *
     * @param sName internal Coherence NamedCache cache name. encodes the JCache CacheManager context.
     * @param mgrCtx Coherence context for NamedCache
     * @param classLoader classLoader used by the Coherence NamedCache
     *
     * Only gets called if coherence configuration file defines &lt;cache-scheme&gt; element referring to this class.
     *
     * Here is the configuration that is now injected into coherence configuration files using the JCache namespace.
     *
     * <pre>{@code
     *   &lt;cachestore-scheme&gt;
     *      &lt;class-scheme&gt;
     *         &lt;class-name&gt;com.tangosol.coherence.jcache.partitionedcache.PartitionedCacheBinaryEntryStore&lt;/class-name&gt;
     *            &lt;init-params&gt;
     *                &lt;init-param&gt;
     *                    &lt;param-type>java.lang.String&lt;/param-type&gt;
     *                    &lt;param-value>{cache-name}&lt;/param-value&gt;
     *                &lt;/init-param&gt;
     *                &lt;init-param&gt;
     *                    &lt;param-type&gt;com.tangosol.net.BackingMapManagerContext&lt;/param-type&gt;
     *                    &lt;param-value&gt;{manager-context}&lt;/param-value&gt;
     *                &lt;/init-param&gt;
     *                &lt;init-param&gt;
     *                    &lt;param-type&gt;java.lang.ClassLoader&lt;/param-type&gt;
     *                    &lt;param-value&gt;{class-loader}&lt;/param-value&gt;
     *               &lt;/init-param&gt;
     *          &lt;/init-params&gt;
     *        &lt;/class-scheme&gt;
     *   &lt;/cachestore-scheme&gt;
     * }</pre>
     */
    public PartitionedCacheBinaryEntryStore(String sName, BackingMapManagerContext mgrCtx, ClassLoader classLoader)
        {
        m_cacheId = new JCacheIdentifier(sName);

        Logger.finest(() -> "Created PartitionedCacheBinaryEntryStore for [name=" + sName
                            + " JCacheId=" + m_cacheId.getCanonicalCacheName() + "]");
        }

    // ----- BinaryEntryStore methods ---------------------------------------

    @Override
    public void load(BinaryEntry binaryEntry)
        {
        long        ldtStart = Helper.getCurrentTimeMillis();

        CacheLoader loader   = getReadThroughCacheLoader(binaryEntry);

        if (loader != null)
            {
            /* Too verbose. But keep in case need to debug native loading in future.
            if (Logger.isEnabled(Logger.FINEST))
                {
                Logger.finest("PartitionedCacheBinaryEntryStore.load called for binaryEntry key="
                                 + binaryEntry.getKey()
                                 + Base.printStackTrace(new Exception("stacktrace")));
                }
            */
            Object oValue = loader.load(binaryEntry.getKey());

            if (oValue != null)
                {
                Binary binValue = (Binary) binaryEntry.getContext().getValueToInternalConverter().convert(oValue);
                JCacheEntryMetaInf metaInf = new JCacheEntryMetaInf(ldtStart, getExpiryPolicy(binaryEntry));

                binValue = BinaryEntryHelper.decorateBinValueWithJCacheMetaInf(binValue, metaInf,
                    binaryEntry.getContext());

                // next line ensures that a just loaded entry is not written back with write-through.
                binValue = BinaryEntryHelper.decorateUpdateJCacheSynthetic(binValue, binaryEntry.getContext(),
                    BinaryEntryHelper.JCACHE_SYNTHETIC_LOADED);

                binaryEntry.updateBinaryValue(binValue);

                Logger.finest(() -> "PartitionedCacheBinaryEntryStore.load loaded key=" + binaryEntry.getKey()
                                 + " value=" + oValue);
                }
            }
        }

    @Override
    public void loadAll(Set set)
        {
        long             ldtStart   = Helper.getCurrentTimeMillis();

        Set<BinaryEntry> binEntries = (Set<BinaryEntry>) set;

        if (!binEntries.isEmpty())
            {
            BinaryEntry aBinEntry = binEntries.iterator().next();
            CacheLoader loader   = getReadThroughCacheLoader(aBinEntry);

            if (loader != null)
                {
                try
                    {
                    JCacheEntryMetaInf metaInf             = new JCacheEntryMetaInf(ldtStart,
                                                                 getExpiryPolicy(aBinEntry));
                    Converter          toInternalConverter = aBinEntry.getContext().getValueToInternalConverter();
                    Map                loadedMap           = loader.loadAll(new KeyIterable(binEntries));

                    for (BinaryEntry binEntry : binEntries)
                        {
                        Object oValue = loadedMap.get(binEntry.getKey());

                        if (!binEntry.isPresent() && oValue != null)
                            {
                            Binary binValue = (Binary) toInternalConverter.convert(oValue);

                            binValue = BinaryEntryHelper.decorateBinValueWithJCacheMetaInf(binValue, metaInf,
                                binEntry.getContext());

                            // next line ensures that a just loaded entry is not written back with write-through.
                            binValue = BinaryEntryHelper.decorateUpdateJCacheSynthetic(binValue, binEntry.getContext(),
                                BinaryEntryHelper.JCACHE_SYNTHETIC_LOADED);
                            binEntry.updateBinaryValue(binValue);
                            }
                        }
                    }
                catch (Throwable e)
                    {
                    throw new CacheLoaderException(e);
                    }
                }
            }
        }

    @Override
    public void store(BinaryEntry binaryEntry)
        {
        CacheWriter writer = getCacheWriter(binaryEntry);

        if (writer != null)
            {
            boolean fJCacheSynthetic = BinaryEntryHelper.isJCacheSyntheticOrLoaded(binaryEntry);

            if (!fJCacheSynthetic)
                {
                Cache.Entry entry = new CacheEntry(binaryEntry);
                writer.write(entry);
                }
            }
        }

    @Override
    public void storeAll(Set set)
        {
        if (set.isEmpty())
            {
            return;
            }

        Set<BinaryEntry> binEntries = (Set<BinaryEntry>) set;

        CacheWriter      writer     = getCacheWriter(binEntries.iterator().next());

        if (writer != null)
            {
            Iterator<BinaryEntry> iter = binEntries.iterator();

            // remove jcache synthetic updates and readThrough loaded entries
            while (iter.hasNext())
                {
                if (BinaryEntryHelper.isJCacheSyntheticOrLoaded(iter.next()))
                    {
                    iter.remove();
                    }
                }

            // writeAll remainder
            if (set.size() != 0)
                {
                List<CacheEntry> jcacheEntries = new ArrayList<CacheEntry>(set.size());
                for (BinaryEntry binEntry : binEntries )
                    {
                    jcacheEntries.add(new CacheEntry(binEntry));
                    }
                try
                    {
                    writer.writeAll(jcacheEntries);

                    // the parameter set must have all entries removed if
                    // they were all successfully written.
                    set.clear();
                    }
                catch (RuntimeException e)
                    {

                    // handle partial writeAll interrupted by an exception.
                    // all entries remaining in jcacheEntries were not written.
                    // ensure that parameter set has same members in it that
                    // jcacheEntries has. this represents the entries
                    // that were not written due to exception.
                    set.clear();
                    for (Cache.Entry entry : jcacheEntries)
                        {
                        set.add(entry.unwrap(BinaryEntry.class));
                        }
                    throw e;
                    }
                }
            }
        }

    @Override
    public void erase(BinaryEntry binaryEntry)
        {
        CacheWriter writer = getCacheWriter(binaryEntry);

        if (writer != null)
            {
            Logger.finest(() -> "PartitionedCacheBinaryEntryStore.erase calling CacheWriter.delete on key="
                             + binaryEntry.getKey() + "CacheWriter class="
                             + writer.getClass().getCanonicalName());

            try
                {
                writer.delete(binaryEntry.getKey());
                }
            catch (UnsupportedOperationException e)
                {
                // Have to wrapper UnsupportedOperationException since Coherence impl detects and
                // disables write-through erase impacting the entry.remove().
                // For JCache implementation, desire that Coherence reverts the entry.remove() so nest the
                // UnsupportedOperationException in general JCache CacheWriterException so Coherence works as it needs
                // to for JCache.  This effectively causes the Cache.remove(K) to not occur due to write-through
                // failure. Just as specified in CacheWriter.delete(K).
                throw new CacheWriterException("CacheWriter implementation " + writer.getClass().getCanonicalName()
                                               + ".delete threw an exception", e);
                }
            }
        }

    @Override
    public void eraseAll(Set setBinEntries)
        {
        if (setBinEntries.isEmpty())
            {
            return;
            }

        CacheWriter writer = getCacheWriter((BinaryEntry) setBinEntries.iterator().next());

        if (writer != null)
            {
            Set<Object> setKeysFailed = new HashSet<Object>();

            try
                {
                KeyIterator iter          = new KeyIterator((Set<BinaryEntry>) setBinEntries);

                while (iter.hasNext())
                    {
                    setKeysFailed.add(iter.next());
                    }

                writer.deleteAll(setKeysFailed);
                setBinEntries.clear();
                }
            catch (RuntimeException re)
                {
                // remove all entries from parameter setBinEntries that were deleted by deleteAll
                for (Iterator<? extends BinaryEntry> iter = setBinEntries.iterator(); iter.hasNext();)
                    {
                    if (!setKeysFailed.contains(iter.next().getKey()))
                        {
                        iter.remove();
                        }
                    }

                throw new CacheWriterException("CacheWriter implementation " + writer.getClass().getCanonicalName()
                                               + ".deleteAll(Set) threw an exception", re);
                }
            }
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Get JCache context from resource registry
     *
     * @param binEntry provide context to get the JCache context from the resource registry
     *
     * @return JCache context
     */
    private JCacheContext getContext(BinaryEntry binEntry)
        {
        if (m_refCtx.get() == null)
            {
            m_refCtx.compareAndSet(null, BinaryEntryHelper.getContext(m_cacheId, binEntry));
            }

        return m_refCtx.get();
        }

    /**
     * Get expiryPolicy for current JCache context for cache that this store is associated with.
     *
     * @param binEntry  context to lookup expiry policy
     *
     * @return the expiry policy for JCache context that this entity is associated with.
     */
    private ExpiryPolicy getExpiryPolicy(BinaryEntry binEntry)
        {
        return getContext(binEntry).getExpiryPolicy();
        }

    /**
     * Get cacheLoader for current JCache context for cache that this store is associated with.
     *
     * @param binEntry  context to lookup expiry policy
     *
     * @return the cache laoder for JCache context that this entity is associated with.
     */
    private CacheLoader getCacheLoader(BinaryEntry binEntry)
        {
        return getContext(binEntry).getCacheLoader();
        }

    /**
     * Get read-through cacheLoader for current JCache context for cache that this store is associated with.
     *
     * @param binEntry  context to lookup expiry policy
     *
     * @return the cache loader for JCache context that this entity is associated with if read-through is enabled.
     */
    private CacheLoader getReadThroughCacheLoader(BinaryEntry binEntry)
        {
        return getContext(binEntry).isReadThrough() ? getCacheLoader(binEntry) : null;
        }

    /**
     * Get cacheWriter for current JCache context for cache that this store is associated with.
     *
     * @param binEntry  context to lookup expiry policy
     *
     * @return the cache writer for JCache context that this entity is associated with.
     */
    private CacheWriter getCacheWriter(BinaryEntry binEntry)
        {
        return getContext(binEntry).getCacheWriter();
        }

    // ----- inner classes --------------------------------------------------

    static private class CacheEntry<K, V>
            implements Cache.Entry<K, V>
        {
        // ----- constructors -----------------------------------------------

        /**
         * Constructs a CacheEntry
         *
         * @param binEntry binaryEntry with its original value
         */
        public CacheEntry(BinaryEntry binEntry)
            {
            f_binEntry = binEntry;
            }

        // ----- Cache.Entry interface --------------------------------------

        @Override
        public K getKey()
            {
            return (K) f_binEntry.getKey();
            }

        @Override
        public V getValue()
            {
            return (V) f_binEntry.getValue();
            }

        @Override
        public <T> T unwrap(Class<T> clazz)
            {
            if (clazz != null && clazz.isInstance(this))
                {
                return (T) this;
                }
            else if (clazz != null && clazz.isAssignableFrom(BinaryEntry.class))
                {
                return (T)f_binEntry;
                }
            else
                {
                throw new IllegalArgumentException("The class " + clazz + " is unknown to this implementation");
                }
            }

        // ----- data members -----------------------------------------------
        final private BinaryEntry f_binEntry;
        }

    static private class KeyIterable
            implements Iterable<Object>
        {
        /**
         * Constructs {@link KeyIterable}
         *
         * @param set  set of BinaryEntries
         */
        public KeyIterable(Set<BinaryEntry> set)
            {
            f_iter = new KeyIterator(set);
            }

        @Override
        public Iterator<Object> iterator()
            {
            return f_iter;
            }

        /**
         * Field description
         */
        public final KeyIterator f_iter;
        }

    static private class KeyIterator
            implements Iterator<Object>
        {
        // ----- constructors -----------------------------------------------

        /**
         * Constructs a iterator over the keys of <code>binEntries</code>
         *
         * @param binEntries entries to iterate over
         */
        public KeyIterator(Set<BinaryEntry> binEntries)
            {
            f_iter = binEntries.iterator();
            }

        // ----- Iterator methods -------------------------------------------

        @Override
        public boolean hasNext()
            {
            return f_iter.hasNext();
            }

        @Override
        public Object next()
            {
            BinaryEntry binEntry = f_iter.next();

            return binEntry == null ? null : binEntry.getKey();
            }

        @Override
        public void remove()
            {
            f_iter.remove();
            }

        // ----- data members -----------------------------------------------
        private final Iterator<BinaryEntry> f_iter;
        }

    // ----- data members ---------------------------------------------------
    transient private AtomicReference<JCacheContext> m_refCtx = new AtomicReference<JCacheContext>();
    private final JCacheIdentifier                   m_cacheId;
    }
