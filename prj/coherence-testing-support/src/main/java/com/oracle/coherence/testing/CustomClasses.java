/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.testing;

import com.oracle.coherence.caffeine.CaffeineCache;
import com.tangosol.io.AsyncBinaryStoreManager;
import com.tangosol.io.BinaryStore;
import com.tangosol.io.BinaryStoreManager;
import com.tangosol.io.bdb.BerkeleyDBBinaryStoreManager;

import com.tangosol.net.BackingMapManagerContext;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.CacheService;
import com.tangosol.net.NamedCache;
import com.tangosol.net.cache.AbstractCacheStore;
import com.tangosol.net.cache.BinaryEntryStore;
import com.tangosol.net.cache.CacheLoader;
import com.tangosol.net.cache.LocalCache;
import com.tangosol.net.cache.NearCache;
import com.tangosol.net.cache.OldCache;
import com.tangosol.net.cache.OverflowMap;
import com.tangosol.net.cache.ReadWriteBackingMap;
import com.tangosol.net.cache.SerializationCache;
import com.tangosol.net.cache.SerializationMap;
import com.tangosol.net.cache.SerializationPagedCache;
import com.tangosol.net.cache.SimpleSerializationMap;
import com.tangosol.net.cache.WrapperNamedCache;

import com.tangosol.util.MapEvent;
import com.tangosol.util.MapListener;
import com.tangosol.util.ObservableMap;
import com.tangosol.util.SafeHashMap;

import java.io.File;

import java.util.HashMap;
import java.util.Map;


/**
 * The CustomClass class has a set of inner classes used to test configuration
 * custom classes.
 *
 * @author pfm 2012.04.27
 */
public class CustomClasses
    {
    // ----- CustomMap inner class ------------------------------------------

    /**
     * Custom SerializationMap extension.
     */
    @SuppressWarnings("rawtypes")
    public static class CustomCacheStore
            extends AbstractCacheStore
        {
        // ----- constructors ----------------------------------------------

        public CustomCacheStore()
            {
            super();
            }

        public CustomCacheStore(String sCacheName)
            {
            super();
            }

        public CustomCacheStore(String sCacheName, String sParam)
            {
            super();
            }

        public CustomCacheStore(String sCacheName, int nParam)
            {
            super();
            }

        @Override
        public void erase(Object oKey)
            {
            map.remove(oKey);
            }

        @Override
        public Object load(Object oKey)
            {
            m_fLoaded = true;
            return map.get(oKey);
            }

        @Override
        public void store(Object oKey, Object oVal)
            {
            map.put(oKey, oVal);
            }

        /**
         * The map to hold CacheStore values
         */
        private final HashMap<Object, Object> map = new HashMap<Object, Object>();

        // ----- helper methods ---------------------------------------------

        /**
         * Return the flag indicating if loadAll was called.
         *
         * @return true if loadAll was called
         */
        public boolean isLoaded()
            {
            return m_fLoaded;
            }

        // ----- data members -----------------------------------------------

        private boolean m_fLoaded;
        }

    /**
     * Custom AsyncBinaryStoreManager.
     */
    public static class CustomAsyncBinaryStoreManager
            extends AsyncBinaryStoreManager
        {
        // ----- constructors ----------------------------------------------

        public CustomAsyncBinaryStoreManager(BinaryStoreManager manager)
            {
            super(manager);
            }

        public CustomAsyncBinaryStoreManager(BinaryStoreManager manager, int cbMax)
            {
            super(manager, cbMax);
            }
        }

    /**
     * Custom BinaryStoreManager.
     */
    public static class CustomBinaryStoreManager
            extends BerkeleyDBBinaryStoreManager
        {
        // ----- constructors ---------------------------------------------

        public CustomBinaryStoreManager()
            {
            super();
            }

        public CustomBinaryStoreManager(File dirParent, String sDbName)
            {
            super(dirParent, sDbName);
            }
        }

     /**
      * CustomListener
      */
     public static class CustomListener
            implements MapListener
        {
        // ----- Listener interface ------------------------------------------

        /**
         * {@inheritDoc}
         */
        @Override
        public void entryInserted(MapEvent evt)
            {
            m_fCalled = true;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public void entryUpdated(MapEvent evt)
            {
            m_fCalled = true;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public void entryDeleted(MapEvent evt)
            {
            m_fCalled = true;
            }

        // ----- CustomListener methods -----------------------------------------

        /**
         * Return the flag indicating if the listener was called.
         *
         * @return true if the listener was called
         */
        public static boolean isCalled()
            {
            return m_fCalled;
            }

        // ----- data members --------------------------------------------------

        private static boolean m_fCalled;
        }

    /**
     * Custom LocalCache.
     */
    public static class CustomLocalCache
            extends LocalCache
        {
        // ----- constructors --------------------------------------------

        public CustomLocalCache()
            {
            super();
            }

        public CustomLocalCache(int cHighUnits, int cDelayMills)
            {
            super(cHighUnits, cDelayMills);
            }

        // ----- CustomCacheLoader methods -------------------------------

        @Override
        public void loadAll()
            {
            m_fLoaded = true;
            super.loadAll();
            }

        /**
         * Return the flag indicating if loadAll was called.
         *
         * @return true if loadAll was called
         */
        public boolean isLoaded()
            {
            return m_fLoaded;
            }

        // ----- data members --------------------------------------------

        private boolean m_fLoaded;
        }

    /**
     * Custom CaffeineCache.
     */
    public static class CustomCaffeineCache
            extends CaffeineCache
        {
        }

    /**
     * Custom NamedCache.
     */
    public static class CustomNamedCache
            extends WrapperNamedCache
        {
        // ----- constructors -------------------------------------------

        public CustomNamedCache(String name)
            {
            this(new SafeHashMap(), name);
            }

        public CustomNamedCache(Map map, String sName)
            {
            super(map, sName, (CacheService) CacheFactory.getCluster().ensureService("LocalCache", "LocalCache"));
            }
        }

    /**
     * Custom NearCache.
     */
    public static class CustomNearCache
            extends NearCache
        {
        // ----- constructors --------------------------------------------

        public CustomNearCache(Map mapFront, NamedCache mapBack)
            {
            super(mapFront, mapBack);
            }

        public CustomNearCache(Map mapFront, NamedCache mapBack, int nStrategy)
            {
            super(mapFront, mapBack, nStrategy);
            }
        }

    /**
     * Custom overflow map.
     */
    public static class CustomOverflowMap
            extends OverflowMap
        {
        // ----- constructors -------------------------------------------

        public CustomOverflowMap(ObservableMap mapFront, Map mapBack)
            {
            super(mapFront, mapBack);
            }
        }

    /**
     * Custom ReadWriteBackingMap.
     */
    public static class CustomReadWriteBackingMap
            extends ReadWriteBackingMap
        {
        // ----- constructors ------------------------------------------

        public CustomReadWriteBackingMap(BackingMapManagerContext ctxService, ObservableMap mapInternal, Map mapMisses,
                CacheLoader loader)
            {
            super(ctxService, mapInternal, mapMisses, loader);
            }

        public CustomReadWriteBackingMap(BackingMapManagerContext ctxService, ObservableMap mapInternal, Map mapMisses,
                CacheLoader loader, boolean fReadOnly, int cWriteBehindSeconds, double dflRefreshAheadFactor)
            {
            super(ctxService, mapInternal, mapMisses, loader, fReadOnly, cWriteBehindSeconds, dflRefreshAheadFactor);
            }


        public CustomReadWriteBackingMap(BackingMapManagerContext ctxService, ObservableMap mapInternal, Map mapMisses,
                BinaryEntryStore storeBinary, boolean fReadOnly, int cWriteBehindSeconds, double dflRefreshAheadFactor)
            {
            super(ctxService, mapInternal, mapMisses, storeBinary, fReadOnly, cWriteBehindSeconds,
                    dflRefreshAheadFactor);
            }
        }

    /**
     * Custom SerializationCache used for external scheme.
     */
    public static class CustomSerializationCache
            extends SerializationCache
        {
        // ----- constructors --------------------------------------------

        public CustomSerializationCache(BinaryStore store, int cMax)
            {
            super(store, cMax);
            }

        public CustomSerializationCache(BinaryStore store, int cMax, ClassLoader loader)
            {
            super(store, cMax, loader);
            }

        public CustomSerializationCache(BinaryStore store, int cMax, boolean fBinaryMap)
            {
            super(store, cMax, fBinaryMap);
            }
        }

    /**
     * Custom SerializationMap map.
     */
    public static class CustomSerializationMap
            extends SerializationMap
        {
        // ----- constructors -------------------------------------------

        public CustomSerializationMap(BinaryStore store)
            {
            super(store);
            }

        public CustomSerializationMap(BinaryStore store, ClassLoader loader)
            {
            super(store, loader);
            }

        public CustomSerializationMap(BinaryStore store, boolean fBinaryMap)
            {
            super(store, fBinaryMap);
            }
        }

    /**
     * Custom SerializationPagedCache used for paged external scheme.
     */
    public static class CustomSerializationPagedCache
            extends SerializationPagedCache
        {
        // ----- constructors -------------------------------------------

        public CustomSerializationPagedCache(BinaryStoreManager storemgr, int cPages, int cPageSecs)
            {
            super(storemgr, cPages, cPageSecs);
            }

        public CustomSerializationPagedCache(BinaryStoreManager storemgr, int cPages, int cPageSecs, ClassLoader loader)
            {
            super(storemgr, cPages, cPageSecs, loader);
            }

        public CustomSerializationPagedCache(BinaryStoreManager storemgr, int cPages, int cPageSecs,
                boolean fBinaryMap, boolean fPassive)
            {
            super(storemgr, cPages, cPageSecs, fBinaryMap, fPassive);
            }
        }

    /**
     * The Invalid class tests a custom cache that doesn't extend a required
     * class or implement a required interface.
     */
    public static class Invalid
        {
        /**
         * Construct the object.
         */
        public Invalid()
            {}

        /**
         * Construct the object.
         *
         * @param cUnits the number of units that the cache manager will cache
         *            before pruning the cache
         * @param cExpiryMillis the number of milliseconds that each cache entry
         *            lives before being automatically expired
         */
        public Invalid(int cUnits, int cExpiryMillis)
            {}
        }
    }
