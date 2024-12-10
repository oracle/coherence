/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.cache;


import com.oracle.coherence.common.base.Blocking;

import com.tangosol.io.BinaryStore;
import com.tangosol.io.BinaryStoreManager;

import com.tangosol.run.xml.XmlElement;

import com.tangosol.util.Base;
import com.tangosol.util.Binary;
import com.tangosol.util.ConcurrentMap;
import com.tangosol.util.Daemon;
import com.tangosol.util.ImmutableArrayList;
import com.tangosol.util.IteratorEnumerator;
import com.tangosol.util.MapEvent;
import com.tangosol.util.SafeHashMap;
import com.tangosol.util.SafeLinkedList;
import com.tangosol.util.SegmentedConcurrentMap;
import com.tangosol.util.SimpleEnumerator;

import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
* A version of SerializationMap that implements an LRU policy using
* time-based paging of the cache. This implementation uses a
* {@link BinaryStoreManager} to create a <i>current</i> BinaryStore which is
* used to store all objects being placed into the cache. Once the specified
* "current" time period has elapsed, a new current BinaryStore is created,
* and the previously current page is closed, which means that it will no
* longer be used to store objects that are being placed into the cache.
* This continues until the total number of pages (the one current plus all
* of the previously current pages) exceeds the maximum number of pages
* defined for the cache. When that happens, the oldest page is evicted,
* triggering the related events, and the BinaryStore for that page is
* destroyed. Note that cache items can be accessed out of and removed from
* closed pages, but cache items are never written to closed pages.
* <p>
* To avoid a massive number of simultaneous events, the eviction of a
* closed page can be performed asynchronously on a daemon thread.
*
* @since Coherence 2.4
* @author cp  2004.05.05
*/
public class SerializationPagedCache
        extends AbstractSerializationCache
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a SerializationPagedCache on top of a BinaryStoreManager.
    *
    * @param storemgr   the BinaryStoreManager that provides BinaryStore
    *                   objects that the serialized objects are written to
    * @param cPages     the maximum number of pages to have active at a time
    * @param cPageSecs  the length of time, in seconds, that a 'page' is
    *                   current
    */
    public SerializationPagedCache(BinaryStoreManager storemgr, int cPages,
            int cPageSecs)
        {
        super(null);
        init(storemgr, cPages, cPageSecs);
        }

    /**
    * Construct a SerializationPagedCache on top of a BinaryStoreManager.
    *
    * @param storemgr   the BinaryStoreManager that provides BinaryStore
    *                   objects that the serialized objects are written to
    * @param cPages     the maximum number of pages to have active at a time
    * @param cPageSecs  the length of time, in seconds, that a 'page' is
    *                   current
    * @param loader     the ClassLoader to use for deserialization
    */
    public SerializationPagedCache(BinaryStoreManager storemgr, int cPages,
            int cPageSecs, ClassLoader loader)
        {
        super(null, loader);
        init(storemgr, cPages, cPageSecs);
        }

    /**
    * Construct a SerializationPagedCache on top of a BinaryStoreManager.
    *
    * @param storemgr    the BinaryStoreManager that provides BinaryStore
    *                    objects that the serialized objects are written to
    * @param cPages      the maximum number of pages to have active at a time
    * @param cPageSecs   the length of time, in seconds, that a 'page' is
    *                    current
    * @param fBinaryMap  true indicates that this map will only manage
    *                    binary keys and values
    * @param fPassive    true indicates that this map is a passive cache,
    *                    which means that it is just a backup of the cache
    *                    and does not actively expire data
    */
    public SerializationPagedCache(BinaryStoreManager storemgr, int cPages,
            int cPageSecs, boolean fBinaryMap, boolean fPassive)
        {
        super(null, fBinaryMap);
        setPassivePagedBackup(fPassive);
        init(storemgr, cPages, cPageSecs);
        }

    /**
    * Construct a SerializationPagedCache on top of a BinaryStoreManager.
    *
    * @param storemgr   the BinaryStoreManager that provides BinaryStore
    *                   objects that the serialized objects are written to
    * @param cPages     the maximum number of pages to have active at a time
    * @param cPageSecs  the length of time, in seconds, that a 'page' is
    *                   current
    */
    private void init(BinaryStoreManager storemgr, int cPages, int cPageSecs)
        {
        if (cPages < 2)
            {
            throw new IllegalArgumentException(
                    "Minimum page count is 2; specified value is " + cPages);
            }
        if (cPages > 3600)
            {
            throw new IllegalArgumentException(
                    "Maximum page count is 3600; specified value is " + cPages);
            }

        if (cPageSecs < 5)
            {
            throw new IllegalArgumentException(
                    "Minimum page duration is 5 seconds; specified value is "
                    + cPageSecs + " seconds.");
            }
        if (cPageSecs > 604800)
            {
            throw new IllegalArgumentException("Maximum page duration is "
                    + "604800 seconds (one week); specified value is "
                    + cPageSecs + " seconds.");
            }

        m_cPageMillis = cPageSecs * 1000L;
        m_storemgr    = storemgr;

        if (storemgr == null)
            {
            setBinaryStore(null);
            }
        else
            {
            setBinaryStore(instantiatePagedStore(cPages));
            }
        }


    // ----- XmlConfigurable interface --------------------------------------

    /**
    * Determine the current configuration of the object.
    *
    * @return the XML configuration or null
    */
    public XmlElement getConfig()
        {
        return m_xmlConfig;
        }

    /**
    * Specify the configuration for the object.
    *
    * @param xml  the XML configuration for the object
    *
    * @exception IllegalStateException  if the object is not in a state that
    *            allows the configuration to be set; for example, if the
    *            object has already been configured and cannot be reconfigured
    */
    public void setConfig(XmlElement xml)
        {
        azzert(getConfig() == null, "The SerializationPagedCache has already"
                + " been configured.");
        azzert(isEmpty(), "The SerializationPagedCache cannot be configured"
                + " once it contains data.");

        m_xmlConfig = xml;
        if (xml != null)
            {
            setPageDuration(xml.getSafeElement("page-duration-seconds")
                    .getLong(getPageDuration() / 1000L) * 1000L);

            setLockDelaySeconds(xml.getSafeElement("lock-delay-seconds")
                    .getInt(getLockDelaySeconds()));

            setVirtualErase(xml.getSafeElement("virtual-erase")
                    .getBoolean(isVirtualErase()));

            setAsynchronousPageDeactivation(xml.getSafeElement("async-page-deactivation")
                    .getBoolean(isAsynchronousPageDeactivation()));

            setPassivePagedBackup(xml.getSafeElement("passive-paged-backup")
                    .getBoolean(isPassivePagedBackup()));

            if (isDebug())
                {
                log("SerializationPagedCache after setConfig():\n"
                        + toString());
                }
            }
        }


    // ----- Map interface --------------------------------------------------

    /**
    * Clear all key/value mappings.
    */
    public void clear()
        {
        lockInternal(ConcurrentMap.LOCK_ALL);
        try
            {
            super.clear();
            }
        finally
            {
            unlockInternal(ConcurrentMap.LOCK_ALL);
            }
        }

    /**
    * Returns the value to which this map maps the specified key.
    *
    * @param oKey  the key object
    *
    * @return the value to which this map maps the specified key,
    *         or null if the map contains no mapping for this key
    */
    public Object get(Object oKey)
        {
        lockInternal(oKey);
        try
            {
            return super.get(oKey);
            }
        finally
            {
            unlockInternal(oKey);
            }
        }

    /**
    * Associates the specified value with the specified key in this map.
    *
    * @param oKey    key with which the specified value is to be associated
    * @param oValue  value to be associated with the specified key
    *
    * @return previous value associated with specified key, or <tt>null</tt>
    *         if there was no mapping for key
    */
    public Object put(Object oKey, Object oValue)
        {
        checkPage();

        lockInternal(oKey);
        try
            {
            return super.put(oKey, oValue);
            }
        finally
            {
            unlockInternal(oKey);
            }
        }

    /**
    * Copies all of the mappings from the specified map to this map.
    * These mappings will replace any mappings that this map had for
    * any of the keys currently in the specified map.
    *
    * @param map  map of entries to be stored in this map
    */
    public void putAll(Map map)
        {
        checkPage();

        // un-roll to satisfy the locking requirements without introducing
        // the possibility of deadlock (e.g. trying to require that all keys
        // be locked to proceed)
        boolean fSinglePut = (map.size() == 1);
        for (Iterator iter = map.entrySet().iterator(); iter.hasNext(); )
            {
            Map.Entry entry = (Map.Entry) iter.next();
            Object    oKey  = entry.getKey();
            lockInternal(oKey);
            try
                {
                super.putAll(fSinglePut
                        ? map
                        : Collections.singletonMap(oKey, entry.getValue()));
                }
            finally
                {
                unlockInternal(oKey);
                }
            }
        }

    /**
    * Removes the mapping for this key from this map if present.
    * Expensive: updates both the underlying cache and the local cache.
    *
    * @param oKey key whose mapping is to be removed from the map
    *
    * @return previous value associated with specified key, or <tt>null</tt>
    *           if there was no mapping for key.  A <tt>null</tt> return can
    *           also indicate that the map previously associated <tt>null</tt>
    *           with the specified key, if the implementation supports
    *           <tt>null</tt> values.
    */
    public Object remove(Object oKey)
        {
        lockInternal(oKey);
        try
            {
            return super.remove(oKey);
            }
        finally
            {
            unlockInternal(oKey);
            }
        }


    // ----- AbstractKeySetBasedMap methods ---------------------------------

    /**
    * {@inheritDoc}
    */
    protected boolean removeBlind(Object oKey)
        {
        lockInternal(oKey);
        try
            {
            return super.removeBlind(oKey);
            }
        finally
            {
            unlockInternal(oKey);
            }
        }


    // ----- SerializationMap methods ---------------------------------------

    /**
    * {@inheritDoc}
    */
    protected void eraseStore()
        {
        // destroy all but the current wrapped page in the paged
        // binary store
        PagedBinaryStore     storePaged    = getPagedBinaryStore();
        WrapperBinaryStore   storeCurrent  = storePaged.getCurrentPage();
        WrapperBinaryStore[] astoreWrapped = storePaged.getActivePageArray();
        for (int i = 0, c = astoreWrapped.length; i < c; ++i)
            {
            WrapperBinaryStore storeWrapped = astoreWrapped[i];
            if (storeWrapped != null && storeWrapped != storeCurrent)
                {
                storeWrapped.destroy();
                }
            }

        // destroy all remaining real (underlying) BinaryStore
        // objects (except for the current one)
        BinaryStore storeRealCurrent = storeCurrent.getBinaryStore();
        for (Iterator iter = iterateBinaryStores(); iter.hasNext(); )
            {
            BinaryStore store = (BinaryStore) iter.next();
            if (store != storeRealCurrent)
                {
                destroyBinaryStore(store);
                }
            }

        // clear the current page
        storeCurrent.eraseAll();
        }


    // ----- SerializationPagedCache methods --------------------------------

    /**
    * Flush items that have expired. This may occur asynchronously, depending
    * on the {@link #isAsynchronousPageDeactivation()
    * AsynchronousPageDeactivation} property.
    *
    * @since Coherence 3.2
    */
    public void evict()
        {
        checkPage();
        }


    // ----- Object methods -------------------------------------------------

    /**
    * {@inheritDoc}
    */
    public String toString()
        {
        return "SerializationPagedCache {" + getDescription() + "}";
        }


    // ----- accessors ------------------------------------------------------

    /**
    * {@inheritDoc}
    */
    protected String getDescription()
        {
        return super.getDescription()
               + ", PageDurationMillis=" + getPageDuration()
               + ", MaximumPages=" + getMaximumPages()
               + ", LockDelaySeconds=" + getLockDelaySeconds()
               + ", VirtualErase=" + isVirtualErase()
               + ", AsynchronousPageDeactivation=" + isAsynchronousPageDeactivation()
               + ", PassivePagedBackup=" + isPassivePagedBackup()
               + ", Debug=" + isDebug();
        }

    /**
    * Determine if BinaryStore erase commands will be done only in memory
    * (to the cached list of keys) and not passsed to the underlying
    * BinaryStore, thus improving performance and cutting I/O, but
    * potentially wasting disk space (or whatever resource the paged data are
    * stored on.)
    *
    * @return true if the erase should not go to the underlying store, but
    *         simply remove the key from the cached list of keys managed by
    *         that store
    */
    public boolean isVirtualErase()
        {
        return m_fVirtualErase;
        }

    /**
    * Specify whether BinaryStore erase commands will be done only in memory
    * to the cached list of keys.
    *
    * @param fVirtualErase  true if the erase should not go to the underlying
    *                       store, but simply remove the key from the cached
    *                       list of keys managed by that store; false to pass
    *                       all erase requests down to the underlying store
    */
    protected void setVirtualErase(boolean fVirtualErase)
        {
        m_fVirtualErase = fVirtualErase;
        }

    /**
    * Determine the number of seconds to wait for a lock in debug mode. This
    * is only applicable to debug mode; in normal mode, a lock is queued for
    * indefinitely.
    *
    * @return the number of seconds to wait for a lock
    */
    public int getLockDelaySeconds()
        {
        return m_cSecondsLockDelay;
        }

    /**
    * Specify the number of seconds to wait for a lock in debug mode. This
    * is only applicable to debug mode; in normal mode, a lock is queued for
    * indefinitely.
    * <p>
    * This will not take effect until the next lock is requested <b>and</b>
    * unless debug mode is turned on.
    *
    * @param cSecondsLockDelay  the number of seconds to wait for a lock
    */
    public void setLockDelaySeconds(int cSecondsLockDelay)
        {
        m_cSecondsLockDelay = cSecondsLockDelay;
        }

    /**
    * Obtain the map used for managing key- and map-level locks to ensure
    * data consistency.
    *
    * @return the ConcurrentMap used to manage thread access to keys and to
    *         the entire map
    */
    protected ConcurrentMap getLockMap()
        {
        return m_mapLocks;
        }

    /**
    * Returns the BinaryStoreManager that provides BinaryStore objects
    * this cache uses for its storage.
    * <p>
    * This object is intended for use only by the createBinaryStore,
    * and destroyBinaryStore methods.
    *
    * @return the BinaryStoreManager for this cache
    */
    protected BinaryStoreManager getBinaryStoreManager()
        {
        return m_storemgr;
        }

    /**
    * Configures the BinaryStore that this map will use for its storage.
    *
    * @param store  the BinaryStore to use
    */
    protected void setBinaryStore(BinaryStore store)
        {
        PagedBinaryStore storeOld = getPagedBinaryStore();
        PagedBinaryStore storeNew = (PagedBinaryStore) store;

        // clean up
        if (storeOld != null)
            {
            storeOld.close();
            }

        super.setBinaryStore(storeNew);

        if (storeNew != null)
            {
            // prime the paged store
            advancePage();
            }
        }

    /**
    * Returns the BinaryStore that this map uses for its storage.
    * <p>
    * Note: This implementation assumes that the BinaryStore is only being
    * modified by this Map instance. If you modify the BinaryStore contents,
    * the behavior of this Map is undefined.
    *
    * @return the BinaryStore
    */
    protected PagedBinaryStore getPagedBinaryStore()
        {
        return (PagedBinaryStore) getBinaryStore();
        }

    /**
    * Get the list of registered BinaryStore objects. This list is intended
    * for use only by the createBinaryStore, destroyBinaryStore and
    * iterateBinaryStores methods.
    *
    * @return the list of BinaryStore objects returned previously from
    *         createBinaryStore that have not yet been destroyed by
    *         destroyBinaryStore
    */
    protected List getBinaryStoreList()
        {
        return m_listStores;
        }

    /**
    * Obtain an iterator of all registered (not destroyed) BinaryStore
    * objects being used by this cache.
    *
    * @return a read-only iterator of BinaryStore objects used by this cache
    */
    protected Iterator iterateBinaryStores()
        {
        List list = getBinaryStoreList();
        synchronized (list)
            {
            return new SimpleEnumerator(list.toArray());
            }
        }

    /**
    * Determine the maximum number of pages that the cache will manage,
    * beyond which the oldest pages are destroyed.
    *
    * @return the maximum number of pages that the cache will manage
    */
    public int getMaximumPages()
        {
        return getPagedBinaryStore().getMaximumPages();
        }

    /**
    * Determine the length of time that the most recently created page is
    * kept as the current page.
    *
    * @return the time in milliseconds that a page remains current
    */
    public long getPageDuration()
        {
        return m_cPageMillis;
        }

    /**
    * Specify the length of time that the most recently created page is
    * kept as the current page.
    *
    * @param cPageMillis  the time in milliseconds that a page remains
    *                     current
    */
    protected void setPageDuration(long cPageMillis)
        {
        m_cPageMillis = cPageMillis;
        }

    /**
    * Determine the time that the current page was created.
    *
    * @return the time that the current page was created or 0L if no page has
    *         been created yet
    */
    protected long getCurrentPageTime()
        {
        return m_ldtCurrentPage;
        }

    /**
    * Determine the time that the next page should be created. If the time
    * returned from this method is in the past, then the implication is that
    * a new page should be created to replace the current page.
    *
    * @return the time that the next page should be created or 0L if no page
    *         has been created yet
    */
    protected long getPageAdvanceTime()
        {
        long lPrev = getCurrentPageTime();
        return lPrev == 0L ? 0L : lPrev + getPageDuration();
        }

    /**
    * Determine if this is just a passive backup for a paged cache.
    *
    * @return true if this cache has been configured as a backup for a paged
    *         cache
    */
    public boolean isPassivePagedBackup()
        {
        return m_fPassiveBackup;
        }

    /**
    * Specify whether this is just a passive backup for a paged cache. A
    * backup will not deactivate pages on its own, but simply waits for pages
    * to empty and then destroys them as soon as they empty.
    *
    * @param fPassiveBackup  true if this cache is just a backup for a paged
    *                        cache
    */
    protected void setPassivePagedBackup(boolean fPassiveBackup)
        {
        m_fPassiveBackup = fPassiveBackup;
        }

    /**
    * Determine if a daemon should evict the items from a deactivated page to
    * avoid blocking other work from being done.
    *
    * @return true if a daemon should be used for page deactivation
    */
    public boolean isAsynchronousPageDeactivation()
        {
        return m_fAsyncDeactivate;
        }

    /**
    * Specify whether a daemon should evict the items from a deactivated page
    * to avoid blocking other work from being done. The deactivation of a
    * page involves evicting every entry from that page, so with a large page
    * of data, it is possible that the cache would be "frozen" for a period
    * of time while the entries are evicted (events being generated, etc.)
    * <p>
    * This will not take effect until the next page is deactivated.
    *
    * @param fAsync  pass true to specify that a daemon should be used for
    *                page deactivation, or false to block all other threads
    *                while a page is fully deactivated
    */
    public void setAsynchronousPageDeactivation(boolean fAsync)
        {
        m_fAsyncDeactivate = fAsync;
        }

    /**
    * Determine the status of the internal debug flag.
    *
    * @return true if the cache is in debug mode, false otherwise
    */
    public static boolean isDebug()
        {
        return s_fDebug;
        }

    /**
    * Set the status of the internal debug flag.
    *
    * @param fDebug  true to set the cache into debug mode, false to set it
    *                into normal runtime mode
    */
    public static void setDebug(boolean fDebug)
        {
        s_fDebug = fDebug;
        }


    // ----- locking support ------------------------------------------------

    /**
    * Obtain a lock for the specified key.
    *
    * @param oKey  the key to lock
    */
    protected void lockInternal(Object oKey)
        {
        ConcurrentMap mapLocks = getLockMap();
        if (isDebug())
            {
            if (mapLocks.lock(oKey))
                {
                return;
                }

            log("Queuing for lock on \"" + oKey + "\"");
            long ldtStart = getSafeTimeMillis();
            if (mapLocks.lock(oKey, getLockDelaySeconds() * 1000L))
                {
                long ldtStop = getSafeTimeMillis();
                log("Lock  on \"" + oKey + "\" obtained after "
                        + (ldtStop - ldtStart) + "ms.");
                return;
                }

            String sMsg = "Unable to obtain lock on \"" + oKey
                    + "\" within the interval of " + getLockDelaySeconds()
                    + " seconds; aborting lock due to debug mode.";
            log(sMsg);
            throw new RuntimeException(sMsg);
            }
        else
            {
            while (!mapLocks.lock(oKey, -1L))
                {
                Thread.yield();
                }
            }
        }

    /**
    * Obtain a lock for the specified key, but only if no other thread has
    * the lock at the point in time that this method is invoked.
    *
    * @param oKey  the key to lock
    *
    * @return true if the lock was available and the key is now locked by
    *         this thread; false otherwise
    */
    protected boolean lockInternalNoWait(Object oKey)
        {
        return getLockMap().lock(oKey);
        }

    /**
    * Release the lock on the specified key.
    *
    * @param oKey  the key to unlock
    */
    protected void unlockInternal(Object oKey)
        {
        getLockMap().unlock(oKey);
        }


    // ----- paging support -------------------------------------------------

    /**
    * Create and register a new BinaryStore object, using this cache's
    * BinaryStoreManager.
    *
    * @return a new BinaryStore
    */
    protected BinaryStore createBinaryStore()
        {
        BinaryStoreManager mgr = getBinaryStoreManager();
        azzert(mgr != null, "BinaryStoreManager is required by createBinaryStore().");

        BinaryStore store = mgr.createBinaryStore();
        azzert(store != null);

        List list = getBinaryStoreList();
        synchronized (list)
            {
            azzert(!list.contains(store));
            list.add(store);
            }

        return store;
        }

    /**
    * Destroy and unregister a BinaryStore object that was previously created
    * using this cache's BinaryStoreManager by the createBinaryStore method.
    *
    * @param store  a BinaryStore returned previously from createBinaryStore
    */
    protected void destroyBinaryStore(BinaryStore store)
        {
        azzert(store != null);

        // ignore the fake stores
        if (store instanceof FakeBinaryStore)
            {
            return;
            }

        if (isDebug())
            {
            log("destroyBinaryStore: " + store);
            }

        List list = getBinaryStoreList();
        synchronized (list)
            {
            if (list.contains(store))
                {
                getBinaryStoreManager().destroyBinaryStore(store);
                list.remove(store);
                }
            else
                {
                if (isDebug())
                    {
                    log("destroyBinaryStore: The specified BinaryStore is"
                            + " not registered; skipping.");
                    }
                }
            }
        }

    /**
    * Determine if the time has come to set up a new page for storing current
    * cache data. This potentially destroys the oldest page, if the maximum
    * number of active pages has been reached.
    */
    protected synchronized void checkPage()
        {
        if (getSafeTimeMillis() > getPageAdvanceTime())
            {
            advancePage();
            }
        }

    /**
    * Advance to a new current page, and deactivate the oldest page is the
    * maximum number of active pages has been reached.
    */
    protected synchronized void advancePage()
        {
        // advance the page
        final WrapperBinaryStore storeNew = instantiateWrapperStore(createBinaryStore());
        final WrapperBinaryStore storeOld = getPagedBinaryStore().advanceCurrentPage(storeNew);

        if (isDebug())
            {
            log("SerializationPagedCache: Processing advancePage()"
                + ", Current-System-Time=" + formatDateTime(getSafeTimeMillis())
                + ", Current-Page-Time=" + formatDateTime(getCurrentPageTime())
                + ", Page-Duration=" + getPageDuration() + "ms"
                + ", Page-Advance-Time=" + formatDateTime(getPageAdvanceTime())
                + ", Passive-Paged-Backup=" + isPassivePagedBackup()
                + ", Store-New=" + storeNew
                + ", Store-Old=" + storeOld);
            }

        // update time when the current page became current
        m_ldtCurrentPage = getSafeTimeMillis();

        // evict up any deactivated page if necessary
        if (storeOld != null && !isPassivePagedBackup())
            {
            if (isAsynchronousPageDeactivation())
                {
                runTask(new Runnable()
                    {
                    public void run()
                        {
                        deactivatePage(storeOld);
                        }
                    });
                }
            else
                {
                deactivatePage(storeOld);
                }
            }
        }

    /**
    * Run the passed task on a separate thread.
    *
    * @param task  the Runnable object to run on a separate thread
    */
    protected void runTask(final Runnable task)
        {
        Daemon daemon = new Daemon("SerializationPagedCache-"
                                   + (++m_cWorkerThreads), Thread.NORM_PRIORITY, false)
            {
            public void run()
                {
                task.run();
                }
            };

        daemon.start();
        }

    /**
    * Deactivate a page that is no longer active. This means expiring any
    * remaining entries on that page. By splitting this method out, it makes
    * it easier for sub-classes to provide this functionality in an
    * asynchronous manner.
    *
    * @param store  the "page" to deactivate
    */
    protected void deactivatePage(final WrapperBinaryStore store)
        {
        try
            {
            // build a list of keys to evict; this allows us to have our
            // own list of keys to avoid erasing individual entries from
            // the actual binary store
            Object[] aoKey = null;
            while (aoKey == null)
                {
                try
                    {
                    aoKey = SimpleEnumerator.toArray(store.keys());
                    }
                catch (ConcurrentModificationException e) {}
                    {
                    }
                }

            // transform it to a "set" of remaining keys to evict
            Set setEvict = new HashSet();
            setEvict.addAll(new ImmutableArrayList(aoKey));

            // the master copy of information on what Binary keys are owned
            // by what BinaryStore objects; the key of this map is the Binary
            // key and the value is the owning BinaryStore object (i.e. the
            // "page" that the key is on)
            Map mapBinKeys = getPagedBinaryStore().getPagedKeyMap();

            // iterate over them until they are all gone
            int cIters = 0;
            while (!setEvict.isEmpty())
                {
                if (cIters++ > 0)
                    {
                    if (isDebug())
                        {
                        log("Iteration #" + cIters + " in deactivatePage().");
                        }

                    try
                        {
                        Blocking.sleep(cIters * 10);
                        }
                    catch (Exception e) {}
                    }

                for (Iterator iter = setEvict.iterator(); iter.hasNext(); )
                    {
                    final Binary binKey = (Binary) iter.next();
                    final Object oKey   = fromBinary(binKey);

                    // lock the key
                    if (lockInternalNoWait(oKey))
                        {
                        try
                            {
                            // find out if that key is still owned by
                            // this store
                            if (mapBinKeys.get(binKey) == store)
                                {
                                if (hasListeners())
                                    {
                                    dispatchPendingEvent(oKey,
                                        MapEvent.ENTRY_DELETED, null, true);
                                    }

                                unregisterKey(oKey);
                                }
                            else
                                {
                                // this key has been updated after this
                                // eviction process began, probably by a
                                // put() call with a newer value (in
                                // other words, the key is now owned by a
                                // "newer" page); as a result, it is no
                                // longer in the list of keys to evict
                                // from the page being deactivated
                                }

                            iter.remove();
                            }
                        finally
                            {
                            unlockInternal(oKey);
                            }
                        }
                    }
                }
            }
        catch (Throwable e)
            {
            err("An exception has been thrown during deactivatePage() processing:");
            err(e);
            err("(Exception has been logged; deactivatePage() will"
                    + " simply discard the page.)");
            }
        finally
            {
            BinaryStore storeActual = store.getBinaryStore();
            if (storeActual != null)
                {
                destroyBinaryStore(storeActual);
                }
            }
        }


    // ----- inner class:  PagedBinaryStore ---------------------------------

    /**
    * Factory method: Instantiate a PagedBinaryStore.
    *
    * @param cPages    the maximum number of pages to have active at a time
    *
    * @return a new PagedBinaryStore object
    */
    protected PagedBinaryStore instantiatePagedStore(int cPages)
        {
        return new PagedBinaryStore(cPages);
        }

    /**
    * A virtual BinaryStore implementation that aggregates a sequence (newest
    * to oldest) of periodic BinaryStore objects.
    */
    public class PagedBinaryStore
            extends Base
            implements BinaryStore
        {
        // ----- constructors -------------------------------------------

        /**
        * Construct a PagedBinaryStore.
        *
        * @param cPages    the maximum number of pages to have active at a
        *                  time
        */
        public PagedBinaryStore(int cPages)
            {
            m_cMaxPages = cPages;
            m_astore    = new WrapperBinaryStore[cPages];
            }

        // ----- BinaryStore interface ----------------------------------

        /**
        * {@inheritDoc}
        */
        public Binary load(Binary binKey)
            {
            Binary binValue = null;

            WrapperBinaryStore store = (WrapperBinaryStore) getPagedKeyMap().get(binKey);
            if (store != null)
                {
                binValue = store.load(binKey);
                }

            return binValue;
            }

        /**
        * {@inheritDoc}
        */
        public void store(Binary binKey, Binary binValue)
            {
            Map                mapKeys = getPagedKeyMap();
            WrapperBinaryStore storeNew;

            storeNew = getCurrentPage();
            if (storeNew == null)
                {
                throw new IllegalStateException("PagedBinaryStore.store("
                                                + binKey + ", " + binValue + "): No current page available.");
                }
            storeNew.store(binKey, binValue);

            try
                {
                WrapperBinaryStore storeOld = (WrapperBinaryStore) mapKeys.get(binKey);
                if (storeOld != null && storeOld != storeNew)
                    {
                    try
                        {
                        storeOld.erase(binKey);
                        }
                    catch (Exception e)
                        {
                        logException(e, "store");
                        }
                    }
                }
            finally
                {
                mapKeys.put(binKey, storeNew);
                }
            }

        /**
        * {@inheritDoc}
        */
        public void erase(Binary binKey)
            {
            WrapperBinaryStore storeOld = (WrapperBinaryStore) getPagedKeyMap().remove(binKey);
            if (storeOld != null)
                {
                try
                    {
                    storeOld.erase(binKey);
                    }
                catch (Exception e)
                    {
                    logException(e, "erase");
                    }
                }
            }

        /**
        * {@inheritDoc}
        */
        public void eraseAll()
            {
            try
                {
                WrapperBinaryStore[] astore = getActivePageArray();
                for (int i = 0, c = astore.length; i < c; ++i)
                    {
                    WrapperBinaryStore store = astore[i];
                    if (store != null)
                        {
                        try
                            {
                            store.eraseAll();
                            }
                        catch (Exception e)
                            {
                            logException(e, "eraseAll");
                            }
                        }
                    }
                }
            finally
                {
                getPagedKeyMap().clear();
                }
            }

        /**
        * {@inheritDoc}
        */
        public Iterator keys()
            {
            Map      map   = getPagedKeyMap();
            Object[] aoKey = map.keySet().toArray();
            return new SimpleEnumerator(aoKey);
            }

        // ----- life-cycle support ---------------------------------------------

        /**
        * Release underlying resources.
        */
        public void close()
            {
            if (isDebug())
                {
                log("SerializationPagedCache: Performing close().");
                }

            // destroy all wrapped pages in the paged binary store
            WrapperBinaryStore[] astoreWrapped = getActivePageArray();
            for (int i = 0, c = astoreWrapped.length; i < c; ++i)
                {
                WrapperBinaryStore storeWrapped = astoreWrapped[i];
                if (storeWrapped != null)
                    {
                    storeWrapped.destroy();
                    }
                }

            // destroy all remaining real (underlying) BinaryStore
            // objects (except for the current one)
            for (Iterator iter = iterateBinaryStores(); iter.hasNext(); )
                {
                BinaryStore store = (BinaryStore) iter.next();
                destroyBinaryStore(store);
                }

            if (isDebug())
                {
                log("SerializationPagedCache: Completed close().");
                }
            }

        // ----- accessors ----------------------------------------------

        /**
        * Determine the maximum number of pages that the cache will manage,
        * beyond which the oldest pages are destroyed.
        *
        * @return the maximum number of pages that the cache will manage
        */
        public int getMaximumPages()
            {
            return m_cMaxPages;
            }

        /**
        * Determine the number of pages that the PagedBinaryStore is
        * currently managing. This is the "active" page count.
        *
        * @return the number of pages that the PagedBinaryStore is
        *         currently managing
        */
        public int getActivePageCount()
            {
            int cPages = getTotalPageCount();
            int cMax   = getMaximumPages();
            return Math.min(cPages, cMax);
            }

        /**
        * Determine the number of pages that the PagedBinaryStore has
        * managed in total, including those it is currently managing.
        *
        * @return the total number of pages ever managed by the
        *         PagedBinaryStore
        */
        public int getTotalPageCount()
            {
            return m_cPages;
            }

        /**
        * Determine the number of pages that the PagedBinaryStore has
        * managed in total, including those it is currently managing.
        *
        * @return the total number of pages ever managed by the
        *         PagedBinaryStore
        */
        public int getCurrentPageNumber()
            {
            return getTotalPageCount() - 1;
            }

        /**
        * Determine if the specified page number is active. A page is
        * active if it is either the current page, or a page that has
        * not aged to the point that it has been discarded.
        *
        * @param nPage  the page number to test
        *
        * @return if the specified page is active
        */
        public boolean isPageActive(int nPage)
            {
            // first check if the page number is out of range (high)
            // by comparing it to the "total page counter"
            int cPages = getTotalPageCount();
            if (nPage >= cPages)
                {
                return false;
                }

            // next check if the page number is out of range (low) by
            // comparing it to the bottom end of the page range that is
            // active
            int cActive = getActivePageCount();
            if (nPage < (cPages - cActive))
                {
                return false;
                }

            return true;
            }

        /**
        * Get the array of BinaryStore objects, one for each active page. The
        * array is indexed by "page indexes" and <b>not</b> page numbers.
        * Page numbers are sequential page ids; they can be translated to
        * page indices by using the toPageIndex method.
        *
        * @return the array of active BinaryStore objects
        */
        public WrapperBinaryStore[] getActivePageArray()
            {
            return m_astore;
            }

        /**
        * Get the BinaryStore for the page specified by the passed page
        * number. The page number is basically the infinitely increasing
        * counter of pages; the first page is 0, the second is 1, and so
        * on.
        *
        * @param nPage  the page number
        *
        * @return the correspodning BinaryStore, or null if the specified
        *         page number is not active
        */
        public WrapperBinaryStore getPage(int nPage)
            {
            int iPage = toPageIndex(nPage);
            if (iPage != -1)
                {
                try
                    {
                    return m_astore[iPage];
                    }
                catch (ArrayIndexOutOfBoundsException e)
                    {
                    }
                }
            return null;
            }

        /**
        * Obtain the page to which current updates are being performed.
        * This page is referred to as the "current" page.
        *
        * @return the BinaryStore that holds the current page's data
        */
        protected WrapperBinaryStore getCurrentPage()
            {
            return getPage(getCurrentPageNumber());
            }

        /**
        * Obtain the oldest active page.
        *
        * @return the BinaryStore that holds the oldest active page's data
        */
        protected WrapperBinaryStore getOldestActivePage()
            {
            int cActive = getActivePageCount();
            return cActive <= 0 ? null : getPage(getCurrentPageNumber() - cActive + 1);
            }

        /**
        * Translate a page number to an index into the PageArray.
        *
        * @param nPage  the page number
        *
        * @return an index into the PageArray, or -1 if the page number is
        *         no longer valid
        */
        protected int toPageIndex(int nPage)
            {
            int cPages = getTotalPageCount();
            if (nPage >= cPages)
                {
                return -1;
                }

            int cMax = getMaximumPages();
            if (nPage < (cPages - cMax))
                {
                return -1;
                }

            return nPage % cMax;
            }

        /**
        * Obtain the map that manages the mapping from Binary keys to
        * BinaryStore objects. The BinaryStore objects are actually
        * wrappers around the real BinaryStore objects that each manage
        * one "page" of data.
        *
        * @return the map of Binary key to BinaryStore
        */
        protected Map getPagedKeyMap()
            {
            return m_mapKeys;
            }

        // ----- page management ----------------------------------------

        /**
        * Advance the current page, using the passed BinaryStore as the store
        * for the new current page, and returning the oldest active page, if
        * the maximum number of active pages is exceeded by the advancing of
        * the current page.
        *
        * @param store  the BinaryStore to use for the new current page
        *
        * @return the oldest active page, if keeping it would exceed the
        *         maximum number of pages; otherwise null
        */
        protected WrapperBinaryStore advanceCurrentPage(WrapperBinaryStore store)
            {
            WrapperBinaryStore[] astore = getActivePageArray();

            // page that was the current up until now
            WrapperBinaryStore storePrevCurrent = getCurrentPage();

            // advance page
            int nCurrent = m_cPages++;
            int iCurrent = toPageIndex(nCurrent);

            // avoid rollover
            if (nCurrent == Integer.MAX_VALUE)
                {
                throw new IllegalStateException("SerializationPagedCache: Maximum total pages exceeded ("
                                                + Integer.MAX_VALUE + ").");
                }

            // extract the page that is being replaced
            WrapperBinaryStore storeOld = astore[iCurrent];

            // store the new page
            astore[iCurrent] = store;

            // tell the previously current page that it is no longer current
            if (storePrevCurrent != null)
                {
                storePrevCurrent.close();
                }

            return storeOld;
            }

        // ----- internal -----------------------------------------------

        /**
        * Helper to log ignored exceptions.
        *
        * @param e        the throwable object
        * @param sMethod  the calling method name
        */
        protected void logException(Throwable e, String sMethod)
            {
            err("An exception has been thrown by the underlying BinaryStore"
                    + " during processing:");
            err(e);
            err("(Exception has been logged; \" + sMethod + \"() will continue.)");
            }

        // ----- data fields --------------------------------------------

        /**
        * A map from Binary key to BinaryStore object that holds the key.
        */
        private Map m_mapKeys = new SafeHashMap();

        /**
        * The counter of pages.
        */
        private int m_cPages;

        /**
        * The maximum number of pages to have active at a time.
        */
        private int m_cMaxPages;

        /**
        * These are the "real" and active BinaryStore objects; each one
        * represents one page of cached data. They are indexed by the
        * modulo of the page counter and the maximum number of pages.
        */
        private WrapperBinaryStore[] m_astore;
        }


    // ----- inner class:  WrapperBinaryStore -------------------------------

    /**
    * Factory method: Instantiate a WrapperBinaryStore.
    *
    * @param store  the BinaryStore to wrap
    *
    * @return a new WrapperBinaryStore object
    */
    protected WrapperBinaryStore instantiateWrapperStore(BinaryStore store)
        {
        return new WrapperBinaryStore(store);
        }

    /**
    * A wrapper BinaryStore implementation that keeps track of its size.
    */
    public class WrapperBinaryStore
            implements BinaryStore
        {
        // ----- constructors -------------------------------------------

        /**
        * Construct a WrapperBinaryStore.
        *
        * @param store  the BinaryStore to delegate to
        */
        public WrapperBinaryStore(BinaryStore store)
            {
            setBinaryStore(store);
            }

        // ----- BinaryStore interface ----------------------------------

        /**
        * {@inheritDoc}
        */
        public Binary load(Binary binKey)
            {
            if (getBinaryStoreKeyMap().containsKey(binKey))
                {
                return getBinaryStore().load(binKey);
                }
            else
                {
                return null;
                }
            }

        /**
        * {@inheritDoc}
        */
        public void store(Binary binKey, Binary binValue)
            {
            getBinaryStore().store(binKey, binValue);
            Map mapKeys = getBinaryStoreKeyMap();
            if (!mapKeys.containsKey(binKey))
                {
                mapKeys.put(binKey, null);
                }
            }

        /**
        * {@inheritDoc}
        */
        public void erase(Binary binKey)
            {
            Map mapKeys = getBinaryStoreKeyMap();
            if (mapKeys.containsKey(binKey))
                {
                if (!isVirtualErase())
                    {
                    getBinaryStore().erase(binKey);
                    }
                mapKeys.remove(binKey);
                }

            checkDestroy();
            }

        /**
        * {@inheritDoc}
        */
        public void eraseAll()
            {
            Map mapKeys = getBinaryStoreKeyMap();
            if (!mapKeys.isEmpty())
                {
                if (!isVirtualErase())
                    {
                    getBinaryStore().eraseAll();
                    }
                mapKeys.clear();
                }

            checkDestroy();
            }

        /**
        * {@inheritDoc}
        */
        public Iterator keys()
            {
            return new SimpleEnumerator(getBinaryStoreKeyMap().keySet().toArray());
            }

        // ----- Object methods -----------------------------------------

        /**
        * Returns a string representation of the object.
        *
        * @return a string representation of the object
        */
        public String toString()
            {
            return "WrapperBinaryStore"
                    + " {BinaryStore=" + getBinaryStore()
                    + ", Current=" + isCurrent()
                    + ", Size=" + getSize()
                    + '}';
            }

        // ----- accessors ----------------------------------------------

        /**
        * @return  the wrapped BinaryStore; null after it is destroyed
        */
        public synchronized BinaryStore getBinaryStore()
            {
            return m_store;
            }

        /**
        * Specify the store to wrap. The store is set to null if/when this
        * wrapper destroys it.
        *
        * @param store  the wrapped BinaryStore
        */
        protected void setBinaryStore(BinaryStore store)
            {
            m_store = store;
            }

        /**
        * @return  the number of keys in the wrapped BinaryStore
        */
        public int getSize()
            {
            return getBinaryStoreKeyMap().size();
            }

        /**
        * @return  the map of keys stored by the wrapped BinaryStore
        */
        protected Map getBinaryStoreKeyMap()
            {
            return m_mapKeys;
            }

        /**
        * @return  true if the page is still current
        */
        public boolean isCurrent()
            {
            return m_fCurrent;
            }

        // ----- internal -----------------------------------------------

        /**
        * Used to specify that the page is no longer current.
        */
        protected void close()
            {
            if (isCurrent())
                {
                m_fCurrent = false;
                checkDestroy();
                }
            }

        /**
        * Test if the underlying store can be destroyed, and if so, destroy
        * it.
        */
        protected void checkDestroy()
            {
            if (!isCurrent() && getSize() == 0)
                {
                synchronized (this)
                    {
                    BinaryStore store = getBinaryStore();
                    if (store != null && !(store instanceof FakeBinaryStore) && getSize() == 0)
                        {
                        destroy();
                        }
                    }
                }
            }

        /**
        * Destroy the underlying BinaryStore.
        */
        protected synchronized void destroy()
            {
            BinaryStore store = getBinaryStore();
            if (store != null && !(store instanceof FakeBinaryStore))
                {
                setBinaryStore(instantiateFakeBinaryStore());
                destroyBinaryStore(store);
                }
            }

        // ----- data fields --------------------------------------------

        /**
        * The wrapped BinaryStore.
        */
        private BinaryStore m_store;

        /**
        * The keys known to exist in the wrapped store.
        */
        private Map m_mapKeys = new SafeHashMap();

        /**
        * True as long as the page is current.
        */
        private boolean m_fCurrent = true;
        }


    // ----- inner class:  WrapperBinaryStore -------------------------------

    /**
    * Factory method: Instantiate a FakeBinaryStore.
    *
    * @return a new FakeBinaryStore object
    */
    protected FakeBinaryStore instantiateFakeBinaryStore()
        {
        return new FakeBinaryStore();
        }

    /**
    * A lite BinaryStore implementation used when the real underlying
    * BinaryStore gets destroyed.
    */
    public static class FakeBinaryStore
            implements BinaryStore
        {
        // ----- BinaryStore interface ----------------------------------

        /**
        * {@inheritDoc}
        */
        public Binary load(Binary binKey)
            {
            return (Binary) getBinaryMap().get(binKey);
            }

        /**
        * {@inheritDoc}
        */
        public void store(Binary binKey, Binary binValue)
            {
            getBinaryMap().put(binKey, binValue);
            }

        /**
        * {@inheritDoc}
        */
        public void erase(Binary binKey)
            {
            getBinaryMap().remove(binKey);
            }

        /**
        * {@inheritDoc}
        */
        public void eraseAll()
            {
            getBinaryMap().clear();
            }

        /**
        * {@inheritDoc}
        */
        public Iterator keys()
            {
            return new IteratorEnumerator(getBinaryMap().keySet().iterator());
            }

        // ----- Object methods -----------------------------------------

        /**
        * Returns a string representation of the object.
        *
        * @return a string representation of the object
        */
        public String toString()
            {
            return "FakeBinaryStore"
                    + " {BinaryMap.size=" + getBinaryMap().size() + "}";
            }

        // ----- accessors ----------------------------------------------

        /**
        * Obtain the map that stores the binary values held by this
        * BinaryStore.
        *
        * @return a map, keyed by Binary key with a value of Binary.
        */
        protected Map getBinaryMap()
            {
            return m_mapBinaries;
            }

        // ----- data members -------------------------------------------

        /**
        * The map that stores the binary values held by this BinaryStore.
        */
        private Map m_mapBinaries = new SafeHashMap();
        }


    // ----- data fields ----------------------------------------------------

    /**
    * XML configuration for the filter.
    */
    private XmlElement m_xmlConfig;

    /**
    * The BinaryStoreManager provides the paged BinaryStore
    * implementation with new pages.
    */
    private BinaryStoreManager m_storemgr;

    /**
    * The list of all BinaryStore objects that are not destroyed.
    */
    private List m_listStores = new SafeLinkedList();

    /**
    * Specifies whether this is just a passive backup for a paged cache. A
    * backup will not deactivate pages on its own, but simply waits for pages
    * to empty and then destroys them as soon as they empty.
    */
    private boolean m_fPassiveBackup;

    /**
    * True means that erases will not actually be done on disk, saving I/O
    * but potentially wasting disk space temporarily.
    * <p>
    * The default is true.
    */
    private boolean m_fVirtualErase = true;

    /**
    * The length of time, in milliseconds, that a 'page' is current.
    * <p>
    * The default is one hour.
    */
    private long m_cPageMillis = 60L * 60L * 1000L;

    /**
    * The length of time, in milliseconds, that a 'page' is current.
    * <p>
    * The default is true.
    */
    private boolean m_fAsyncDeactivate = true;

    /**
    * The time, in milliseconds, that the current page was created, or 0L if
    * no page has been created yet.
    */
    private long m_ldtCurrentPage;

    /**
    * This map is used for managing key- and map-level locks to ensure
    * data consistency, particularly in light of support for asynchronous
    * expiry.
    */
    private ConcurrentMap m_mapLocks = new SegmentedConcurrentMap();

    /**
    * In debug mode, an attempt to lock times out after this many seconds.
    * <p>
    * The default is one minute (60 seconds.)
    */
    private int m_cSecondsLockDelay = 60;

    /**
    * Internal counter of threads created.
    */
    private int m_cWorkerThreads;

    /**
    * Internal debug flag.
    * <p>
    * The default is false.
    */
    private static boolean s_fDebug = false;
    }