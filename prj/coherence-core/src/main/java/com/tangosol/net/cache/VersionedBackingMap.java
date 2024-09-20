/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.cache;


import com.tangosol.net.BackingMapManagerContext;
import com.tangosol.net.NamedCache;

import com.tangosol.util.Base;
import com.tangosol.util.MapListener;
import com.tangosol.util.ObservableMap;

import java.util.Map;


/**
* Backing Map implementation that provides a size-limited cache of a
* persistent store and supports configurable write-behind caching.
* Additionally, this implementation supports a VersionedNearCache in
* front of the distributed cache, and manages version data caches to
* optimize the near cache without sacrificing data integrity.
* <p>
* This implementation does not support null keys or null values. The
* values stored in this Map must implement the Versionable interface.
*
* @author cp 2002.10.20
* @deprecated as of Coherence 3.2 all the functionality of this class has been
*              superceded by the ReadWriteBackingMap implementation
*/
public class VersionedBackingMap
        extends ReadWriteBackingMap
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a VersionedBackingMap using a CacheLoader object.
    *
    * @param ctxService           the context provided by the CacheService
    *                             which is using this backing map
    * @param mapInternal          the ObservableMap used to store the data
    *                             internally in this backing map
    * @param mapMisses            the Map used to cache CacheLoader misses
    *                             (optional)
    * @param loader               the CacheLoader responsible for the
    *                             persistence of the cached data (optional)
    * @param mapVersionTransient  (optional) a replicated cache of versions
    *                             of the cached data as it is in memory
    * @param mapVersionPersist    (optional) a replicated cache of versions
    *                             of the cached data as it was written to the
    *                             CacheStore
    * @param fManageTransient     if true, the backing map is responsible for
    *                             keeping the transient version cache up to
    *                             date; if false (as is the case when using
    *                             the VersionedNearCache implementation), the
    *                             backing map manages the transient version
    *                             cache only for operations for which no
    *                             other party is aware (such as entry expiry)
    */
    public VersionedBackingMap(BackingMapManagerContext ctxService, ObservableMap mapInternal,
            Map mapMisses, CacheLoader loader, NamedCache mapVersionTransient,
            NamedCache mapVersionPersist, boolean fManageTransient)
        {
        super(ctxService, mapInternal, mapMisses, loader, true, 0, 0.0, RWBM_WB_REMOVE_DEFAULT);
        init(mapVersionTransient, mapVersionPersist, fManageTransient);
        }

    /**
    * Construct a VersionedBackingMap using a CacheStore object.
    *
    * @param ctxService            the context provided by the CacheService
    *                              which is using this backing map
    * @param mapInternal           the ObservableMap used to store the data
    *                              internally in this backing map
    * @param mapMisses             the Map used to cache CacheStore misses
    *                              (optional)
    * @param store                 the object responsible for the persistence
    *                              of the cached data (optional)
    * @param fReadOnly             pass true to use the CacheStore only for
    *                              read operations
    * @param cWriteBehindSeconds   number of seconds to write if there is a
    *                              CacheStore; zero disables write-behind
    *                              caching (optional)
    * @param dflRefreshAheadFactor the interval before an entry expiration time
    *                              (expressed as a percentage of the internal
    *                              cache expiration interval) during which an
    *                              asynchronous load request for the
    *                              entry will be scheduled; zero disables
    *                              refresh-ahead; only applicable when
    *                              the <tt>mapInternal</tt> parameter is an
    *                              instance of {@link LocalCache} (optional)
    * @param mapVersionTransient   (optional) a replicated cache of versions
    *                              of the cached data as it is in memory
    * @param mapVersionPersist     (optional) a replicated cache of versions
    *                              of the cached data as it was written to the
    *                              CacheStore
    * @param fManageTransient      if true, the backing map is responsible for
    *                              keeping the transient version cache up to
    *                              date; if false (as is the case when using
    *                              the VersionedNearCache implementation), the
    *                              backing map manages the transient version
    *                              cache only for operations for which no
    *                              other party is aware (such as entry expiry)
    */
    public VersionedBackingMap(BackingMapManagerContext ctxService, ObservableMap mapInternal,
            Map mapMisses, CacheStore store, boolean fReadOnly, int cWriteBehindSeconds,
            double dflRefreshAheadFactor,NamedCache mapVersionTransient, NamedCache mapVersionPersist,
            boolean fManageTransient)
        {
        this(ctxService, mapInternal, mapMisses, store, fReadOnly, cWriteBehindSeconds,
                dflRefreshAheadFactor, mapVersionTransient, mapVersionPersist, fManageTransient, RWBM_WB_REMOVE_DEFAULT);
        }

    /**
    * Construct a VersionedBackingMap using a CacheStore object.
    *
    * @param ctxService            the context provided by the CacheService
    *                              which is using this backing map
    * @param mapInternal           the ObservableMap used to store the data
    *                              internally in this backing map
    * @param mapMisses             the Map used to cache CacheStore misses
    *                              (optional)
    * @param store                 the object responsible for the persistence
    *                              of the cached data (optional)
    * @param fReadOnly             pass true to use the CacheStore only for
    *                              read operations
    * @param cWriteBehindSeconds   number of seconds to write if there is a
    *                              CacheStore; zero disables write-behind
    *                              caching (optional)
    * @param dflRefreshAheadFactor the interval before an entry expiration time
    *                              (expressed as a percentage of the internal
    *                              cache expiration interval) during which an
    *                              asynchronous load request for the
    *                              entry will be scheduled; zero disables
    *                              refresh-ahead; only applicable when
    *                              the <tt>mapInternal</tt> parameter is an
    *                              instance of {@link LocalCache} (optional)
    * @param mapVersionTransient   (optional) a replicated cache of versions
    *                              of the cached data as it is in memory
    * @param mapVersionPersist     (optional) a replicated cache of versions
    *                              of the cached data as it was written to the
    *                              CacheStore
    * @param fManageTransient      if true, the backing map is responsible for
    *                              keeping the transient version cache up to
    *                              date; if false (as is the case when using
    *                              the VersionedNearCache implementation), the
    *                              backing map manages the transient version
    *                              cache only for operations for which no
    *                              other party is aware (such as entry expiry)
    * @param fWriteBehindRemove    pass true if the specified loader is in fact
    *                              a CacheStore that needs to apply write-behind to remove
    *
    * @since 12.1.1.4.18
    */
    public VersionedBackingMap(BackingMapManagerContext ctxService, ObservableMap mapInternal,
                               Map mapMisses, CacheStore store, boolean fReadOnly, int cWriteBehindSeconds,
                               double dflRefreshAheadFactor,NamedCache mapVersionTransient, NamedCache mapVersionPersist,
                               boolean fManageTransient, boolean fWriteBehindRemove)
        {
        super(ctxService, mapInternal, mapMisses, store, fReadOnly, cWriteBehindSeconds, dflRefreshAheadFactor, fWriteBehindRemove);
        init(mapVersionTransient, mapVersionPersist, fManageTransient);
        }

    /**
    * Initialize the VersionedBackingMap.
    *
    * @param mapVersionTransient  (optional) a replicated cache of versions
    *                             of the cached data as it is in memory
    * @param mapVersionPersist    (optional) a replicated cache of versions
    *                             of the cached data as it was written to the
    *                             CacheStore
    * @param fManageTransient     if true, the backing map is responsible for
    *                             keeping the transient version cache up to
    *                             date; if false (as is the case when using
    *                             the VersionedNearCache implementation), the
    *                             backing map manages the transient version
    *                             cache only for operations for which no
    *                             other party is aware (such as entry expiry)
    */
    private void init(NamedCache mapVersionTransient, NamedCache mapVersionPersist, boolean fManageTransient)
        {
        m_mapVersionTransient = mapVersionTransient;
        m_mapVersionPersist   = mapVersionPersist;
        m_fManageTransient    = fManageTransient;
        }


    // ----- internal cache -------------------------------------------------

    /**
    * Factory pattern: Create a MapListener that listens to the internal
    * cache and routes those events to anyone listening to this
    * VersionedBackingMap.
    *
    * @return a new routing MapListener
    */
    protected MapListener instantiateInternalListener()
        {
        return new InternalMapListener();
        }

    /**
    * A MapListener implementation that listens to the internal cache and
    * routes those events to anyone listening to this VersionedBackingMap.
    *
    * @author cp 2002.10.22
    */
    protected class InternalMapListener
            extends ReadWriteBackingMap.InternalMapListener
        {
        }


    // ----- CacheStoreWrapper ----------------------------------------------

    /**
    * Factory pattern: Instantiate a CacheStore wrapper around the passed
    * CacheStore. (Supports CacheStore extension by delegation pattern.)
    *
    * @param store  the CacheStore to wrap
    *
    * @return the CacheStoreWrapper that can supplement and override the
    *         operations of the supplied CacheStore
    */
    protected ReadWriteBackingMap.CacheStoreWrapper instantiateCacheStoreWrapper(CacheStore store)
        {
        return store == null ? null : new CacheStoreWrapper(store);
        }

    /**
    * A wrapper around the original CacheStore to allow operations to be
    * overridden and extended.
    *
    * @author cp 2002.10.22
    */
    public class CacheStoreWrapper
            extends ReadWriteBackingMap.CacheStoreWrapper
        {
        // ----- constructors -------------------------------------------

        /**
        * Construct a wrapped CacheStore.
        *
        * @param store  the CacheStore to wrap
        */
        public CacheStoreWrapper(CacheStore store)
            {
            super(store);
            }
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Get the cache of version info for the data stored in the persistent
    * store behind this backing map (and other related backing maps if this
    * is backing a distributed cache).
    *
    * @return the NamedCache object that has a "last written" version entry
    *         for each key maintained in memory by this cache, or null if
    *         the version cache is not used
    */
    public NamedCache getPersistentVersionCache()
        {
        return m_mapVersionPersist;
        }

    /**
    * Update the persistent version of the specified key.
    *
    * @param oKey  the key in its external ("real") format
    * @param ver   the new version to store; null implies remove
    */
    protected void updatePersistentVersion(Object oKey, Comparable ver)
        {
        NamedCache mapVerPersistent = getPersistentVersionCache();
        if (mapVerPersistent != null)
            {
            try
                {
                if (ver == null)
                    {
                    mapVerPersistent.remove(oKey);
                    }
                else
                    {
                    mapVerPersistent.put(oKey, ver);
                    }
                }
            catch (Exception e)
                {
                Base.err("An exception occurred updating the persistent version cache:");
                Base.err(e);
                Base.err("(The exception will be ignored. The VersionedBackingMap will continue.)");
                }
            }
        }

    /**
    * Get the cache of version info for the data maintained in this
    * backing map (and other related backing maps if this is backing a
    * distributed cache).
    *
    * @return the NamedCache object that has a version entry for each key
    *         maintained in memory by this cache, or null if the version
    *         cache is not used
    */
    public NamedCache getTransientVersionCache()
        {
        return m_mapVersionTransient;
        }

    /**
    * Update the transient version of the specified key.
    *
    * @param oKey  the key in its external ("real") format
    * @param ver   the new version to store; null implies remove
    */
    protected void updateTransientVersion(Object oKey, Comparable ver)
        {
        NamedCache mapVerTransient = getTransientVersionCache();
        if (mapVerTransient != null)
            {
            if (mapVerTransient.lock(oKey, MAX_LOCK_WAIT))
                {
                try
                    {
                    if (ver == null)
                        {
                        mapVerTransient.remove(oKey);
                        }
                    else
                        {
                        mapVerTransient.put(oKey, ver);
                        }
                    }
                catch (Exception e)
                    {
                    Base.err("An exception occurred updating the " +
                            "transient version cache:");
                    Base.err(e);
                    Base.err("(The exception will be ignored. The " +
                            "VersionedBackingMap will continue.)");
                    }
                finally
                    {
                    mapVerTransient.unlock(oKey);
                    }
                }
            }
        }

    /**
    * Determine if this backing map is responsible for keeping the transient
    * version cache in sync.
    *
    * @return true if the backing map is the object that is maintaining the
    *         transient version cache; false if it is not (e.g. if the
    *         VersionedNearCache is being used)
    */
    public boolean isManagingTransientVersion()
        {
        return m_fManageTransient;
        }


    // ----- data fields ----------------------------------------------------

    /**
    * The length of time (in millis) to wait on a lock; keep this value low
    * to avoid causing timing problems since some calls will be on the
    * distributed cache thread.
    */
    public static final long MAX_LOCK_WAIT = 0L;

    /**
    * The optional cache of version info for the data maintained in this
    * backing map (and other related backing maps if this is backing a
    * distributed cache).
    */
    private NamedCache m_mapVersionTransient;

    /**
    * The optional cache of version info for the data stored in the
    * persistent store behind this backing map (and other related backing
    * maps if this is backing a distributed cache).
    */
    private NamedCache m_mapVersionPersist;

    /**
    * If true, the backing map is responsible for keeping the transient
    * version cache up to date; if false (as is the case when using the
    * VersionedNearCache implementation), the backing map manages the
    * transient version cache only for operations for which no other party
    * is aware (such as entry expiry).
    */
    private boolean    m_fManageTransient;
    }
