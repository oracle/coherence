/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.persistence.journal;

import com.oracle.coherence.persistence.PersistentStore;

import com.tangosol.internal.util.DaemonPool;
import com.tangosol.internal.util.Daemons;
import com.tangosol.internal.util.DefaultDaemonPoolDependencies;
import com.tangosol.internal.util.PartitionedCacheComponent;

import com.tangosol.io.FileHelper;
import com.tangosol.io.ReadBuffer;

import com.tangosol.persistence.CachePersistenceHelper;
import com.tangosol.persistence.AbstractPersistenceManager.AbstractPersistentStore;

import com.tangosol.util.Binary;
import com.tangosol.util.LongArray;

import java.io.File;
import java.io.IOException;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link JournalBackingMap}.
 *
 * @author Aleks Seovic  2026.04.02
 * @since 26.04
 */
public class JournalBackingMapTest
    {
    @Before
    public void setup()
            throws IOException
        {
        m_file = FileHelper.createTempDir();

        DefaultDaemonPoolDependencies deps = new DefaultDaemonPoolDependencies();
        deps.setThreadCount(1);

        m_pool = Daemons.newDaemonPool(deps);
        m_pool.start();

        m_manager = new JournalPersistenceManager(m_file, null, "test-journal-backing-map");
        m_manager.setDaemonPool(m_pool);
        m_store = (AbstractPersistentStore) m_manager.open("partition-0", null);
        m_store.ensureExtent(1L);
        }

    @After
    public void cleanup()
            throws IOException
        {
        if (m_manager != null)
            {
            m_manager.release();
            }
        if (m_pool != null)
            {
            m_pool.stop();
            }
        if (m_file != null)
            {
            FileHelper.deleteDir(m_file);
            }
        }

    @Test
    public void testGetWithNoStore()
        {
        JournalBackingMap map = new JournalBackingMap();

        assertNull(map.get(new Binary(new byte[] {1, 1})));
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
        }

    @Test
    public void testGetDelegatesToStore()
        {
        Binary binKey   = new Binary(new byte[] {1, 1});
        Binary binValue = new Binary(new byte[] {11});

        m_store.store(1L, binKey, binValue, null);

        JournalBackingMap map = new JournalBackingMap();
        map.setStore(m_store);
        map.setExtentId(1L);

        assertEquals(binValue, map.get(binKey));
        }

    @Test
    public void testMissingExtentBehavesAsEmpty()
        {
        JournalBackingMap map = new JournalBackingMap();
        map.setStore(m_store);
        map.setExtentId(2L);

        assertNull(map.get(new Binary(new byte[] {2, 1})));
        assertFalse(map.containsKey(new Binary(new byte[] {2, 2})));
        assertTrue(map.isEmpty());
        }

    @Test
    public void testPutReturnsOldValue()
        {
        Binary binKey      = new Binary(new byte[] {1, 2});
        Binary binValueOld = new Binary(new byte[] {22});
        Binary binValueNew = new Binary(new byte[] {23});

        m_store.store(1L, binKey, binValueOld, null);

        JournalBackingMap map = new JournalBackingMap();
        map.setStore(m_store);
        map.setExtentId(1L);

        assertEquals(binValueOld, map.put(binKey, binValueNew));
        assertEquals(binValueNew, map.get(binKey));

        Binary binKeyNew = new Binary(new byte[] {1, 9});
        assertNull(map.put(binKeyNew, binValueNew));
        assertEquals(binValueNew, map.get(binKeyNew));
        }

    @Test
    public void testPutStoresCacheMetadata()
        {
        JournalBackingMap map = new JournalBackingMap();
        map.setStore(m_store);
        map.setExtentId(1L);
        map.setCacheName("journal-backing-map");

        map.put(new Binary(new byte[] {1, 7}), new Binary(new byte[] {77}));

        LongArray<String> laCaches = CachePersistenceHelper.getCacheNamesForActiveRecovery(m_store);

        assertEquals("journal-backing-map", laCaches.get(1L));
        }

    @Test
    public void testRemoveReturnsOldValue()
        {
        Binary binKey   = new Binary(new byte[] {1, 3});
        Binary binValue = new Binary(new byte[] {33});

        m_store.store(1L, binKey, binValue, null);

        JournalBackingMap map = new JournalBackingMap();
        map.setStore(m_store);
        map.setExtentId(1L);

        assertEquals(binValue, map.remove(binKey));
        assertNull(map.get(binKey));
        assertNull(map.remove(new Binary(new byte[] {1, 8})));
        }

    @Test
    public void testContainsKey()
        {
        Binary binKey   = new Binary(new byte[] {1, 4});
        Binary binValue = new Binary(new byte[] {44});

        m_store.store(1L, binKey, binValue, null);

        JournalBackingMap map = new JournalBackingMap();
        map.setStore(m_store);
        map.setExtentId(1L);

        assertTrue(map.containsKey(binKey));
        assertFalse(map.containsKey(new Binary(new byte[] {1, 5})));
        }

    @Test
    public void testSameKeyAcrossExtents()
        {
        Binary binKey  = new Binary(new byte[] {7, 7});
        Binary binVal1 = new Binary(new byte[] {71});
        Binary binVal2 = new Binary(new byte[] {72});

        m_store.ensureExtent(2L);
        m_store.store(1L, binKey, binVal1, null);
        m_store.store(2L, binKey, binVal2, null);

        JournalBackingMap mapOne = new JournalBackingMap();
        mapOne.setStore(m_store);
        mapOne.setExtentId(1L);

        JournalBackingMap mapTwo = new JournalBackingMap();
        mapTwo.setStore(m_store);
        mapTwo.setExtentId(2L);

        assertEquals(binVal1, mapOne.get(binKey));
        assertEquals(binVal2, mapTwo.get(binKey));
        }

    @Test
    public void testSizeAndIsEmptyReflectStoredEntries()
        {
        Binary binKeyOne = new Binary(new byte[] {9, 1});
        Binary binKeyTwo = new Binary(new byte[] {9, 2});
        Binary binValue  = new Binary(new byte[] {91});

        JournalBackingMap map = new JournalBackingMap();
        map.setStore(m_store);
        map.setExtentId(1L);

        assertTrue(map.isEmpty());
        assertEquals(0, map.size());

        map.put(binKeyOne, binValue);
        map.put(binKeyTwo, binValue);

        assertFalse(map.isEmpty());
        assertEquals(2, map.size());

        map.remove(binKeyOne);
        map.remove(binKeyTwo);

        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
        }

    @Test
    public void testClear()
        {
        JournalBackingMap map = new JournalBackingMap();
        map.setStore(m_store);
        map.setExtentId(1L);

        map.put(new Binary(new byte[] {1}), new Binary(new byte[] {11}));
        map.put(new Binary(new byte[] {2}), new Binary(new byte[] {22}));

        map.clear();

        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
        assertNull(map.get(new Binary(new byte[] {1})));
        }

    @Test
    public void testPutAll()
        {
        JournalBackingMap map = new JournalBackingMap();
        map.setStore(m_store);
        map.setExtentId(1L);

        Map<Binary, Binary> mapEntries = new LinkedHashMap<>();
        mapEntries.put(new Binary(new byte[] {1, 1}), new Binary(new byte[] {11}));
        mapEntries.put(new Binary(new byte[] {1, 2}), new Binary(new byte[] {22}));
        mapEntries.put(new Binary(new byte[] {1, 3}), new Binary(new byte[] {33}));

        map.putAll(mapEntries);

        assertEquals(3, map.size());
        for (Map.Entry<Binary, Binary> entry : mapEntries.entrySet())
            {
            assertEquals(entry.getValue(), map.get(entry.getKey()));
            }
        }

    @Test
    public void testCollectionViews()
        {
        JournalBackingMap map = new JournalBackingMap();
        map.setStore(m_store);
        map.setExtentId(1L);

        Binary binKey1 = new Binary(new byte[] {4, 1});
        Binary binKey2 = new Binary(new byte[] {4, 2});
        Binary binVal1 = new Binary(new byte[] {41});
        Binary binVal2 = new Binary(new byte[] {42});

        map.put(binKey1, binVal1);
        map.put(binKey2, binVal2);

        Set<Binary>             setKeys    = map.keySet();
        Collection<Binary>      colValues  = map.values();
        Set<Map.Entry<Binary, Binary>> setEntries = map.entrySet();

        assertThat(setKeys, containsInAnyOrder(binKey1, binKey2));
        assertThat(colValues, containsInAnyOrder(binVal1, binVal2));
        assertTrue(setEntries.containsAll(Arrays.asList(
                new java.util.AbstractMap.SimpleImmutableEntry<>(binKey1, binVal1),
                new java.util.AbstractMap.SimpleImmutableEntry<>(binKey2, binVal2))));
        }

    @Test
    @SuppressWarnings("unchecked")
    public void testResolveStoreUsesPrimaryAccessorsByDefault()
        {
        PersistentStore<ReadBuffer>  store   = mock(PersistentStore.class);
        PartitionedCacheComponent    service = mock(PartitionedCacheComponent.class);
        Binary                       key     = new Binary(new byte[] {5, 1});
        Binary                       value   = new Binary(new byte[] {51});

        when(service.getPersistentStore(7)).thenReturn(store);
        when(service.ensureOpenPersistentStore(7)).thenReturn(store);
        when(service.getPartitionCount()).thenReturn(257);
        when(store.isOpen()).thenReturn(true);
        when(store.containsExtent(1L)).thenReturn(false);

        JournalBackingMap map = new JournalBackingMap();
        map.setPartitionedCacheService(service);
        map.setPartition(7);
        map.setExtentId(1L);

        assertNull(map.get(key));
        assertNull(map.put(key, value));

        verify(service).getPersistentStore(7);
        verify(service).ensureOpenPersistentStore(7);
        verify(service, never()).getBackupPersistentStore(7);
        verify(service, never()).ensureOpenBackupPersistentStore(7);
        }

    @Test
    @SuppressWarnings("unchecked")
    public void testResolveStoreUsesBackupAccessorsWhenConfigured()
        {
        PersistentStore<ReadBuffer>  store   = mock(PersistentStore.class);
        PartitionedCacheComponent    service = mock(PartitionedCacheComponent.class);
        Binary                       key     = new Binary(new byte[] {6, 1});
        Binary                       value   = new Binary(new byte[] {61});

        when(service.getBackupPersistentStore(9)).thenReturn(store);
        when(service.ensureOpenBackupPersistentStore(9)).thenReturn(store);
        when(service.getPartitionCount()).thenReturn(257);
        when(store.isOpen()).thenReturn(true);
        when(store.containsExtent(1L)).thenReturn(false);

        JournalBackingMap map = new JournalBackingMap();
        map.setPartitionedCacheService(service);
        map.setPartition(9);
        map.setExtentId(1L);
        map.setBackup(true);

        assertNull(map.get(key));
        assertNull(map.put(key, value));

        verify(service).getBackupPersistentStore(9);
        verify(service).ensureOpenBackupPersistentStore(9);
        verify(service, never()).getPersistentStore(9);
        verify(service, never()).ensureOpenPersistentStore(9);
        }

    @Test
    public void testSetBackupFailsAfterMapAccess()
        {
        JournalBackingMap map = new JournalBackingMap();

        assertNull(map.get(new Binary(new byte[] {7, 1})));

        try
            {
            map.setBackup(true);
            fail("expected IllegalStateException");
            }
        catch (IllegalStateException e)
            {
            assertTrue(e.getMessage().contains("setBackup"));
            }
        }

    private File m_file;
    private DaemonPool m_pool;
    private JournalPersistenceManager m_manager;
    private AbstractPersistentStore m_store;
    }
