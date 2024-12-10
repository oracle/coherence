/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.cache;


import com.tangosol.util.Base;
import com.tangosol.util.ImmutableArrayList;
import com.tangosol.util.MapEvent;
import com.tangosol.util.MapListener;
import com.tangosol.util.SafeHashMap;
import com.tangosol.util.SimpleEnumerator;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


/**
* A LocalCache implementation that supports the JCache API, CacheLoader and
* CacheStore objects.
*
* @since Coherence 2.2
*
* @author cp  2003.05.30
*/
public class LocalCache
        extends OldCache
        implements CacheMap
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct the cache manager.
    */
    public LocalCache()
        {
        this(DEFAULT_UNITS);
        }

    /**
    * Construct the cache manager.
    *
    * @param cUnits         the number of units that the cache manager will
    *                       cache before pruning the cache
    */
    public LocalCache(int cUnits)
        {
        this(cUnits, DEFAULT_EXPIRE);
        }

    /**
    * Construct the cache manager.
    *
    * @param cUnits         the number of units that the cache manager will
    *                       cache before pruning the cache
    * @param cExpiryMillis  the number of milliseconds that each cache entry
    *                       lives before being automatically expired
    */
    public LocalCache(int cUnits, int cExpiryMillis)
        {
        super(cUnits, cExpiryMillis);
        }

    /**
    * Construct the cache manager.
    *
    * @param cUnits         the number of units that the cache manager will
    *                       cache before pruning the cache
    * @param cExpiryMillis  the number of milliseconds that each cache entry
    *                       lives before being automatically expired
    * @param loader         the CacheLoader or CacheStore to use
    */
    public LocalCache(int cUnits, int cExpiryMillis, CacheLoader loader)
        {
        this(cUnits, cExpiryMillis);

        setCacheLoader(loader);
        }


    // ----- Map interface --------------------------------------------------

    /**
    * Removes all mappings from this map.
    */
    public synchronized void clear()
        {
        // this method is only called as a result of a call from the cache
        // consumer, not from any internal eviction etc.

        // if there is a CacheStore, tell it that all entries are being erased
        CacheStore store = getCacheStore();
        if (store != null)
            {
            store.eraseAll(Collections.unmodifiableCollection(keySet()));
            }

        super.clear();
        }

    /**
    * Removes the mapping for this key from this map if present.
    *
    * @param oKey  key whose mapping is to be removed from the map
    *
    * @return previous value associated with specified key, or <tt>null</tt>
    *         if there was no mapping for key.  A <tt>null</tt> return can
    *         also indicate that the map previously associated <tt>null</tt>
    *         with the specified key, if the implementation supports
    *         <tt>null</tt> values
    */
    public synchronized Object remove(Object oKey)
        {
        // this method is only called as a result of a call from the cache
        // consumer, not from any internal eviction etc.

        // check for the specified entry; getEntryInternal() will only return an
        // entry if the entry exists and has not expired
        OldCache.Entry entry = (OldCache.Entry) getEntryInternal(oKey);
        if (entry == null)
            {
            return null;
            }
        else
            {
            // if there is a CacheStore, tell it that the entry is being
            // erased
            CacheStore store = getCacheStore();
            if (store != null)
                {
                store.erase(oKey);
                }

            return super.remove(oKey);
            }
        }


    // ----- JCache interface -----------------------------------------------

    /**
    * Determine the loader used by this LocalCache, if any.
    *
    * @return the loader used by this LocalCache, or null if none
    */
    public CacheLoader getCacheLoader()
        {
        return m_loader;
        }

    /**
    * Specify the loader used by this LocalCache.
    *
    * @param loader  loader to use, or null
    */
    public synchronized void setCacheLoader(CacheLoader loader)
        {
        if (loader != m_loader)
            {
            // unconfigure the old loader
            m_loader    = null;
            m_store     = null;

            MapListener listener = m_listener;
            if (listener != null)
                {
                removeMapListener(listener);
                m_listener = null;
                }

            // configure with the new loader
            m_loader = loader;
            if (loader instanceof CacheStore)
                {
                m_store    = (CacheStore) loader;
                m_listener = listener = instantiateInternalListener();
                this.addMapListener(listener);
                }
            }
        }

    /**
    * If the specified item is in the cache, return it. Otherwise,
    * load the value for the specified key and return it.
    *
    * @param oKey  the key to the desired cached item
    *
    * @return the value corresponding to the specified key, otherwise null
    */
    public Object get(Object oKey)
        {
        return super.get(oKey);
        }

    /**
    * Locate an Entry in the hash map based on its key. If the Entry is not in
    * the cache, load the Entry for the specified key and return it.
    *
    * @param oKey  the key to the desired cached Entry
    *
    * @return the Entry corresponding to the specified key, otherwise null
    */
    public SafeHashMap.Entry getEntry(Object oKey)
        {
        SafeHashMap.Entry entry = super.getEntry(oKey);

        // Try to load and register Misses only if Cache Loader is configured.
        if (entry == null && getCacheLoader() != null)
            {
            long ldtStart = getCurrentTimeMillis();

            load(oKey);

            // use getEntryInternal() instead of get() to avoid screwing
            // up stats
            entry = getEntryInternal(oKey);
            m_stats.registerMisses(0, ldtStart);
            }

        return entry;
        }

    /**
    * Get all the specified keys, if they are in the cache. For each key
    * that is in the cache, that key and its corresponding value will be
    * placed in the map that is returned by this method. The absence of
    * a key in the returned map indicates that it was not in the cache,
    * which may imply (for caches that can load behind the scenes) that
    * the requested data could not be loaded.
    * <p>
    * The result of this method is defined to be semantically the same as
    * the following implementation, without regards to threading issues:
    *
    * <pre><tt>
    * Map map = new AnyMap(); // could be hash map or ...
    * for (Iterator iter = col.iterator(); iter.hasNext(); )
    *     {
    *     Object oKey = iter.next();
    *     Object oVal = get(oKey);
    *     if (oVal != null || containsKey(oKey))
    *         {
    *         map.put(oKey, oVal);
    *         }
    *     }
    * return map;
    * </tt></pre>
    *
    * @param colKeys  a collection of keys that may be in the named cache
    *
    * @return a Map of keys to values for the specified keys passed in
    *         <tt>col</tt>
    */
    public Map getAll(Collection colKeys)
        {
        long ldtStart = 0;

        // first, get all of the requested keys that are already loaded
        // into the map
        Map map    = peekAll(colKeys);
        int cTotal = colKeys.size();
        int cHits  = map.size();
        if (cHits < cTotal)
            {
            // load the remaining keys
            CacheLoader loader = getCacheLoader();
            if (loader != null)
                {
                ldtStart = getCurrentTimeMillis();

                // build a list of the missing keys to load
                Set setRequest = new HashSet(colKeys);
                setRequest.removeAll(map.keySet());

                // load the missing keys
                loadAll(setRequest);

                // whichever ones are now loaded, add their values to the
                // result
                map.putAll(peekAll(setRequest));
                }
            }

        // update stats
        m_stats.registerHits(cHits, ldtStart);
        m_stats.registerMisses(cTotal - cHits, ldtStart);

        return map;
        }

    /**
    * Indicates to the cache that the specified key should be loaded into the
    * cache, if it is not already in the cache. This provides a means to
    * "pre-load" a single entry into the cache using the cache's loader.
    * <p>
    * If a valid entry with the specified key already exists in the cache,
    * or if the cache does not have a loader, then this method has no effect.
    * <p>
    * An implementation may perform the load operation asynchronously.
    *
    * @param oKey  the key to request to be loaded
    */
    public void load(final Object oKey)
        {
        CacheLoader loader = getCacheLoader();
        if (loader != null && getEntryInternal(oKey) == null)
            {
            Object oValue = loader.load(oKey);
            if (oValue != null)
                {
                KeyMask mask = new KeyMask()
                    {
                    public boolean isIgnored(Object oCheckKey)
                        {
                        return equals(oKey, oCheckKey);
                        }
                    };

                setKeyMask(mask);
                try
                    {
                    super.put(oKey, oValue);
                    }
                finally
                    {
                    setKeyMask(null);
                    }
                }
            }
        }

    /**
    * Indicates to the cache that it should load data from its loader to
    * fill the cache; this is sometimes referred to as "pre-loading" or
    * "warming" a cache.
    * <p>
    * The specific set of data that will be loaded is unspecified. The
    * implementation may choose to load all data, some specific subset
    * of the data, or no data. An implementation may require that the
    * loader implement the IterableCacheLoader interface in order for
    * this method to load any data.
    * <p>
    * An implementation may perform the load operation asynchronously.
    */
    public void loadAll()
        {
        CacheLoader loader = getCacheLoader();
        if (loader instanceof IterableCacheLoader)
            {
            Iterator iter = ((IterableCacheLoader) loader).keys();

            int cMaxUnits = getHighUnits();
            if (cMaxUnits > 0 && cMaxUnits < Integer.MAX_VALUE)
                {
                int cTarget  = Math.max(getLowUnits(), (int) (0.9 * cMaxUnits));
                int cCurrent = getUnits();
                while (iter.hasNext() && cCurrent < cTarget)
                    {
                    load(iter.next());

                    int cUnits = getUnits();
                    if (cUnits < cCurrent)
                        {
                        // cache is already starting to prune itself for
                        // some reason; assume that eviction occurred
                        // which is an indication that we've warmed the
                        // cache suitably
                        break;
                        }

                    cCurrent = cUnits;
                    }
                }
            else
                {
                loadAll(new ImmutableArrayList(SimpleEnumerator.toArray(iter)));
                }
            }
        }

    /**
    * Indicates to the cache that the specified keys should be loaded into
    * the cache, if they are not already in the cache. This provides a means
    * to "pre-load" entries into the cache using the cache's loader.
    * <p>
    * The result of this method is defined to be semantically the same as
    * the following implementation:
    *
    * <pre><tt>
    * CacheLoader loader = getCacheLoader();
    * if (loader != null &amp;&amp; !colKeys.isEmpty())
    *     {
    *     Set setRequest = new HashSet(colKeys);
    *     setRequest.removeAll(peekAll(colKeys).keySet());
    *     if (!setRequest.isEmpty())
    *         {
    *         Map map = loader.loadAll(colKeys);
    *         if (!map.isEmpty())
    *             {
    *             putAll(map);
    *             }
    *         }
    *     }
    * </tt></pre>
    *
    * @param colKeys  a collection of keys to request to be loaded
    */
    public void loadAll(Collection colKeys)
        {
        CacheLoader loader = getCacheLoader();
        if (loader != null && !colKeys.isEmpty())
            {
            Set setRequest = new HashSet(colKeys);
            setRequest.removeAll(peekAll(colKeys).keySet());
            if (!setRequest.isEmpty())
                {
                Map map = loader.loadAll(setRequest);
                if (!map.isEmpty())
                    {
                    final Set setKeys = map.keySet();
                    KeyMask mask = new KeyMask()
                        {
                        public boolean isIgnored(Object oCheckKey)
                            {
                            return setKeys.contains(oCheckKey);
                            }
                        };

                    setKeyMask(mask);
                    try
                        {
                        super.putAll(map);
                        }
                    finally
                        {
                        setKeyMask(null);
                        }
                    }
                }
            }
        }

    /**
    * Checks for a valid entry corresponding to the specified key in the
    * cache, and returns the corresponding value if it is. If it is not in
    * the cache, returns null, and does not attempt to load the value using
    * its cache loader.
    *
    * @param oKey  the key to "peek" into the cache for
    *
    * @return the value corresponding to the specified key
    */
    public Object peek(Object oKey)
        {
        // avoid super.get() because it affects statistics
        OldCache.Entry entry = (OldCache.Entry) getEntryInternal(oKey);
        return entry == null ? null : entry.getValue();
        }

    /**
    * Checks for a valid entry corresponding to each specified key in the
    * cache, and places the corresponding value in the returned map if it is.
    * For each key that is not in the cache, no entry is placed into the
    * returned map. The cache does not attempt to load any values using
    * its cache loader.
    * <p>
    * The result of this method is defined to be semantically the same as
    * the following implementation, without regards to threading issues:
    *
    * <pre><tt>
    * Map map = new HashMap();
    * for (Iterator iter = colKeys.iterator(); iter.hasNext(); )
    *     {
    *     Object oKey   = iter.next();
    *     Object oValue = peek(oKey);
    *     if (oValue != null || containsKey(oKey))
    *         {
    *         map.put(oKey, oValue);
    *         }
    *     }
    * return map;
    * </tt></pre>
    *
    * @param colKeys  a collection of keys to "peek" into the cache for
    *
    * @return a Map of keys that were found in the cache and their values
    */
    public Map peekAll(Collection colKeys)
        {
        Map map = new HashMap(colKeys.size());
        for (Iterator iter = colKeys.iterator(); iter.hasNext(); )
            {
            Object      oKey  = iter.next();
            OldCache.Entry entry = (OldCache.Entry) getEntryInternal(oKey);
            if (entry != null)
                {
                map.put(oKey, entry.getValue());
                }
            }
        return map;
        }


    // ----- internal -------------------------------------------------------

    /**
    * Determine the store used by this LocalCache, if any.
    *
    * @return the CacheStore used by this LocalCache, or null if none
    */
    protected CacheStore getCacheStore()
        {
        return m_store;
        }

    /**
    * ThreadLocal: Get the current key mask for the current thread.
    *
    * @return the current key mask
    */
    protected KeyMask getKeyMask()
        {
        KeyMask mask = (KeyMask) m_tloIgnore.get();
        return mask == null ? DEFAULT_KEY_MASK : mask;
        }

    /**
    * ThreadLocal: Set the key mask for the current thread.
    *
    * @param mask  the new key mask, or null to clear the mask
    */
    protected void setKeyMask(KeyMask mask)
        {
        m_tloIgnore.set(mask);
        }

    /**
    * {@inheritDoc}
    */
    protected synchronized boolean removeEvicted(OldCache.Entry entry)
        {
        long    dtExpiry     = entry.getExpiryMillis();
        boolean fExpired     = dtExpiry != 0 && (dtExpiry & ~0xFFL) < getCurrentTimeMillis();
        KeyMask mask         = getKeyMask();
        boolean fPrev        = mask.ensureSynthetic();
        boolean fPrevExpired = fExpired ? mask.ensureExpired() : false;
        try
            {
            return super.removeEvicted(entry);
            }
        finally
            {
            mask.setSynthetic(fPrev);
            mask.setExpired(fPrevExpired);
            }
        }

    /**
    * Factory pattern: instantiate a new CacheEvent corresponding
    * to the specified parameters.
    *
    * @return a new instance of the CacheEvent class (or a subclass thereof)
    */
    protected MapEvent instantiateMapEvent(int nId, Object oKey, Object oValueOld, Object oValueNew)
        {
        return new CacheEvent(this,
                              nId,
                              oKey,
                              oValueOld,
                              oValueNew,
                              getKeyMask().isSynthetic(),
                              CacheEvent.TransformationState.TRANSFORMABLE,
                              false,
                              getKeyMask().isExpired());
        }


    // ----- inner class: InternalListener ----------------------------------

    /**
    * Factory pattern: Instantiate an internal MapListener to listen to this
    * cache and report changes to the CacheStore.
    *
    * @return  a new MapListener instance
    */
    protected MapListener instantiateInternalListener()
        {
        return new InternalListener();
        }

    /**
    * An internal MapListener that listens to this cache and reports
    * changes to the CacheStore.
    */
    protected class InternalListener
            extends Base
            implements MapListener
        {
        /**
        * Invoked when a map entry has been inserted.
        *
        * @param evt  the MapEvent carrying the insert information
        */
        public void entryInserted(MapEvent evt)
            {
            onModify(evt);
            }

        /**
        * Invoked when a map entry has been updated.
        *
        * @param evt  the MapEvent carrying the update information
        */
        public void entryUpdated(MapEvent evt)
            {
            onModify(evt);
            }

        /**
        * Invoked when a map entry has been removed.
        *
        * @param evt  the MapEvent carrying the delete information
        */
        public void entryDeleted(MapEvent evt)
            {
            // deletions are handled by the clear() and remove(Object)
            // methods, and are ignored by the listener, because they
            // include evictions, which may be impossible to differentiate
            // from client-invoked removes and clears
            }

        /**
        * A value modification event (insert or update) has occurred.
        *
        * @param evt  the MapEvent object
        */
        protected void onModify(MapEvent evt)
            {
            if (!getKeyMask().isIgnored(evt.getKey()))
                {
                CacheStore store = getCacheStore();
                if (store != null)
                    {
                    store.store(evt.getKey(), evt.getNewValue());
                    }
                }
            }
        }


    // ----- inner class: KeyMask -------------------------------------------

    /**
    * A class that masks certain changes so that they are not reported back
    * to the CacheStore.
    */
    protected class KeyMask
            extends Base
        {
        /**
        * Check if a key should be ignored.
        *
        * @param oKey  the key that a change event has occurred for
        *
        * @return true if change events for the key should be ignored
        */
        public boolean isIgnored(Object oKey)
            {
            return false;
            }

        /**
        * Check whether or not the currently performed operation is
        * internally initiated.
        *
        * @return true iff the current operation is internal
        */
        public boolean isSynthetic()
            {
            return true;
            }

        /**
        * Specify whether or not the currently performed operation is internally
        * initiated.
        *
        * @param fSynthetic  true iff the current operation is internal
        */
        public void setSynthetic(boolean fSynthetic)
            {
            }

        /**
        * Check whether or not the currently performed operation has been initiated
        * because the entry expired.
        *
        * @return true iff the entry has expired
        *
        * @since 22.06
        */
        public boolean isExpired()
            {
            return true;
            }

        /**
        * Specify whether or not the currently performed operation concerns an
        * expired entry
        *
        * @param fExpired  true iff the current operation is an expiration one
        *
        * @since 22.06
        */
        public void setExpired(boolean fExpired)
            {
            }

        /**
        * Ensure that the synthetic operation flag is set.
        *
        * @return the previous value of the flag
        */
        public boolean ensureSynthetic()
            {
            boolean f = isSynthetic();
            if (!f)
                {
                setSynthetic(true);
                }
            return f;
            }

        /**
        * Ensure that the expired operation flag is set.
        *
        * @return the previous value of the flag
        *
        * @since 22.06
        */
        public boolean ensureExpired()
            {
            boolean f = isExpired();
            if (!f)
                {
                setExpired(true);
                }
            return f;
            }
        }


    // ----- inner class: Entry ---------------------------------------------

    /**
    * {@inheritDoc}
    */
    protected SafeHashMap.Entry instantiateEntry()
        {
        return new Entry();
        }

    /**
    * A holder for a cached value.
    */
    public class Entry
            extends OldCache.Entry
        {
        }


    // ----- constants ------------------------------------------------------

    /**
    * By default, the cache size (in units) is infinite.
    */
    public static final int DEFAULT_UNITS    = Integer.MAX_VALUE;

    /**
    * By default, the cache entries never expire.
    */
    public static final int DEFAULT_EXPIRE   = 0;

    /**
    * The default key mask that ignores nothing.
    */
    protected final KeyMask DEFAULT_KEY_MASK = new KeyMask()
        {
        public boolean isSynthetic()
            {
            return m_fSynthetic;
            }
        public void setSynthetic(boolean fSynthetic)
            {
            m_fSynthetic = fSynthetic;
            }
        public boolean isExpired()
            {
            return m_fExpired;
            }
        public void setExpired(boolean fExpired)
            {
            m_fExpired = fExpired;
            }
        private boolean m_fSynthetic = false;
        private boolean m_fExpired   = false;
        };


    // ----- data members ---------------------------------------------------

    /**
    * The loader used by this cache for misses.
    */
    private CacheLoader m_loader;

    /**
    * The store used by this cache for modifications. If this value is
    * non-null, then it is the same reference as the loader.
    */
    private CacheStore m_store;

    /**
    * The map listener used by this cache to listen to itself in order to
    * pass events to the CacheStore. Only used when there is a CacheStore.
    */
    private MapListener m_listener;

    /**
    * The thread-local object to check for keys that the current thread
    * is supposed to ignore if those keys change. Contains KeyMask objects.
    */
    private ThreadLocal m_tloIgnore = new ThreadLocal();
    }
