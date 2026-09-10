/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.persistence.journal;

import com.oracle.coherence.persistence.PersistentStore;
import com.oracle.coherence.persistence.PersistentStore.Visitor;

import com.tangosol.internal.util.PartitionedCacheComponent;

import com.tangosol.io.ReadBuffer;

import com.tangosol.net.PartitionedService;

import com.tangosol.persistence.CachePersistenceHelper;

import com.tangosol.util.Binary;
import com.tangosol.util.LongArray;
import com.tangosol.util.SparseArray;

import java.util.AbstractMap;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * A {@link Map} facade over a journal persistent store.
 * <p>
 * This map is used for {@code <journal-scheme>} primary storage, so the map
 * itself is the durable representation of the cache data. Service-level
 * persistence must therefore bypass the legacy side-channel for these
 * mutations; each logical update is durably recorded exactly once in the
 * journal store rather than once in the map and again in a separate
 * persistence path.
 * <p>
 * Recovery may re-apply journal-backed {@code put}/{@code remove} operations
 * while rebuilding service state. Those operations must remain idempotent
 * with respect to already-persisted journal contents so replay, restart, or
 * ownership transitions converge on the same final data set without double
 * writing.
 *
 * @author Aleks Seovic  2026.04.02
 * @since 26.04
 */
public class JournalBackingMap
        extends AbstractMap<Binary, Binary>
        implements PersistentBackingMap
    {
    @Override
    public Binary get(Object oKey)
        {
        if (!(oKey instanceof Binary))
            {
            return null;
            }

        PersistentStore<ReadBuffer> store = resolveStoreForRead();
        if (!isReadableStore(store))
            {
            return null;
            }

        return (Binary) store.load(m_lExtentId, (Binary) oKey);
        }

    @Override
    public Binary put(Binary key, Binary value)
        {
        PersistentStore<ReadBuffer> store = resolveStore(true);
        if (store == null)
            {
            return null;
            }

        long   lExtentId = m_lExtentId;
        Binary binPrev   = store.containsExtent(lExtentId)
                ? (Binary) store.load(lExtentId, key)
                : null;

        store.ensureExtent(lExtentId);
        ensureCacheMetadata(store);
        store.store(lExtentId, key, value, null);
        sealStore(store);

        return binPrev;
        }

    @Override
    public Binary remove(Object key)
        {
        PersistentStore<ReadBuffer> store = resolveStore(true);
        if (store == null || !(key instanceof Binary) || !store.containsExtent(m_lExtentId))
            {
            return null;
            }

        Binary binKey  = (Binary) key;
        Binary binPrev = (Binary) store.load(m_lExtentId, binKey);
        if (binPrev != null)
            {
            store.erase(m_lExtentId, binKey, null);
            sealStore(store);
            }

        return binPrev;
        }

    @Override
    public boolean containsKey(Object key)
        {
        return get(key) != null;
        }

    @Override
    public int size()
        {
        PersistentStore<ReadBuffer> store = resolveStoreForRead();
        if (!isReadableStore(store))
            {
            return 0;
            }

        int[] cEntries = new int[1];
        iterateExtent(store, m_lExtentId, (lExtentId, key, value) ->
            {
            cEntries[0]++;
            return true;
            });
        return cEntries[0];
        }

    @Override
    public boolean isEmpty()
        {
        PersistentStore<ReadBuffer> store = resolveStoreForRead();
        if (!isReadableStore(store))
            {
            return true;
            }

        boolean[] fEmpty = new boolean[] {true};
        iterateExtent(store, m_lExtentId, (lExtentId, key, value) ->
            {
            fEmpty[0] = false;
            return false;
            });
        return fEmpty[0];
        }

    @Override
    public Set<Entry<Binary, Binary>> entrySet()
        {
        PersistentStore<ReadBuffer> store = resolveStoreForRead();
        if (!isReadableStore(store))
            {
            return Collections.emptySet();
            }

        Set<Entry<Binary, Binary>> setEntries = new LinkedHashSet<>();
        iterateExtent(store, m_lExtentId, (lExtentId, key, value) ->
            {
            setEntries.add(new SimpleImmutableEntry<>(key.toBinary(), value.toBinary()));
            return true;
            });
        return setEntries;
        }

    @Override
    public void clear()
        {
        PersistentStore<ReadBuffer> store = resolveStore(true);
        if (store != null && store.containsExtent(m_lExtentId))
            {
            store.truncateExtent(m_lExtentId);
            sealStore(store);
            }
        }

    public void setStore(PersistentStore<ReadBuffer> store)
        {
        m_store = store;
        m_fMetadataEnsured = false;
        m_fStoreSealed = false;
        }

    public PersistentStore<ReadBuffer> getStore()
        {
        return m_store;
        }

    public void setPartitionedCacheService(PartitionedCacheComponent service)
        {
        m_service = service;
        }

    public void setPartition(int nPartition)
        {
        m_nPartition = nPartition;
        }

    /**
     * Configure this map to resolve the backup store rather than the active store.
     * This selector must only be set before the map resolves a store from the service.
     *
     * @param fBackup  {@code true} to resolve backup stores; {@code false} for active stores
     */
    public void setBackup(boolean fBackup)
        {
        if (m_fStoreResolved)
            {
            throw new IllegalStateException("setBackup must be called before the backing map resolves a persistent store");
            }

        m_fBackup = fBackup;
        }

    public void setExtentId(long lExtentId)
        {
        m_lExtentId = lExtentId;
        m_fMetadataEnsured = false;
        }

    public long getExtentId()
        {
        return m_lExtentId;
        }

    public void setCacheName(String sCacheName)
        {
        m_sCacheName = sCacheName;
        m_fMetadataEnsured = false;
        }

    public String getCacheName()
        {
        return m_sCacheName;
        }

    /**
     * Ensure the store metadata tracks this cache/extent mapping.
     *
     * @param store  the backing persistent store
     */
    private void ensureCacheMetadata(PersistentStore<ReadBuffer> store)
        {
        if (m_fMetadataEnsured)
            {
            return;
            }

        String sCacheName = m_sCacheName;
        long   lExtentId  = m_lExtentId;
        if (store == null || sCacheName == null || lExtentId <= 0L)
            {
            return;
            }

        LongArray<String> laCaches = store.containsExtent(CachePersistenceHelper.META_EXTENT)
                ? CachePersistenceHelper.getCacheNamesForActiveRecovery(store)
                : new SparseArray<>();

        if (!sCacheName.equals(laCaches.get(lExtentId)))
            {
            laCaches.set(lExtentId, sCacheName);
            CachePersistenceHelper.storeCacheNames(store, laCaches);
            }

        m_fMetadataEnsured = true;
        }

    /**
     * Resolve the current store for this partition.
     *
     * @return the current persistent store
     */
    private PersistentStore<ReadBuffer> resolveStore(boolean fEnsureOpen)
        {
        m_fStoreResolved = true;

        PartitionedCacheComponent service = m_service;
        if (service != null && m_nPartition >= 0)
            {
            PersistentStore<ReadBuffer> store = m_fBackup
                    ? (fEnsureOpen
                        ? service.ensureOpenBackupPersistentStore(m_nPartition)
                        : service.getBackupPersistentStore(m_nPartition))
                    : (fEnsureOpen
                        ? service.ensureOpenPersistentStore(m_nPartition)
                        : service.getPersistentStore(m_nPartition));
            if (store != null)
                {
                if (store != m_store)
                    {
                    m_fMetadataEnsured = false;
                    m_fStoreSealed = false;
                    }
                m_store = store;
                }
        }

        return m_store;
        }

    /**
     * Resolve the current store for read access, reopening a known closed store
     * without creating a store when the partition has not yet realized one.
     *
     * @return the current persistent store
     */
    private PersistentStore<ReadBuffer> resolveStoreForRead()
        {
        PersistentStore<ReadBuffer> store = resolveStore(false);
        return store == null || store.isOpen() ? store : resolveStore(true);
        }

    /**
     * Return {@code true} iff the store can be read for this map extent.
     *
     * @param store  the store to inspect
     *
     * @return {@code true} iff this map extent is readable
     */
    private boolean isReadableStore(PersistentStore<ReadBuffer> store)
        {
        return store != null && store.isOpen() && store.containsExtent(m_lExtentId);
        }

    /**
     * Ensure the store is marked recoverable after a direct persistent-store mutation.
     *
     * @param store  the backing persistent store
     */
    private void sealStore(PersistentStore<ReadBuffer> store)
        {
        PartitionedCacheComponent service = m_service;
        if (store != null && service instanceof PartitionedService && !m_fStoreSealed)
            {
            synchronized (this)
                {
                if (!m_fStoreSealed)
                    {
                    CachePersistenceHelper.seal(store, (PartitionedService) service, null);
                    m_fStoreSealed = true;
                    }
                }
            }
        }

    private PersistentStore<ReadBuffer> m_store;

    private PartitionedCacheComponent m_service;

    private int m_nPartition = -1;

    /**
     * {@code true} iff this map should resolve backup persistent stores.
     * Threading: configured during partition setup before concurrent store resolution.
     */
    private boolean m_fBackup;

    /**
     * {@code true} once this map has attempted to resolve its store from the service.
     * Threading: same single-threaded setup/first-resolution guard as the store fields above.
     */
    private boolean m_fStoreResolved;

    private long m_lExtentId;

    private String m_sCacheName;

    /**
     * Visit all entries belonging to the supplied extent.
     *
     * @param store      the backing store
     * @param lExtentId  the extent id
     * @param visitor    the visitor to apply
     */
    private void iterateExtent(PersistentStore<ReadBuffer> store, long lExtentId, Visitor<ReadBuffer> visitor)
        {
        store.iterate(new Visitor<>()
            {
            @Override
            public boolean visit(long lExtentIdVisited, ReadBuffer key, ReadBuffer value)
                {
                return lExtentIdVisited != lExtentId || visitor.visit(lExtentIdVisited, key, value);
                }

            @Override
            public boolean visitExtent(long lExtentIdVisited)
                {
                return lExtentIdVisited == lExtentId && visitor.visitExtent(lExtentIdVisited);
                }
            });
        }

    /**
     * {@code true} once cache metadata has been written to the current store.
     */
    private volatile boolean m_fMetadataEnsured;

    /**
     * {@code true} once the current store has been sealed by this map.
     * A seal marks the initialized store, not an individual mutation, so it
     * remains valid across subsequent journal commits. The flag is reset
     * whenever store resolution selects a different store.
     */
    private volatile boolean m_fStoreSealed;

    }
