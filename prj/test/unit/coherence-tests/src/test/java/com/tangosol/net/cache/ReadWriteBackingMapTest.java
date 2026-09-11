/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.cache;

import com.tangosol.io.pof.ConfigurablePofContext;

import com.tangosol.net.BackingMapManagerContext;
import com.tangosol.net.cache.ConfigurableCacheMap.EvictionApprover;

import com.tangosol.util.Binary;
import com.tangosol.util.ConcurrentMap;
import com.tangosol.util.Converter;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.ObservableHashMap;
import com.tangosol.util.ObservableMap;
import com.tangosol.util.SegmentedConcurrentMap;
import com.tangosol.util.processor.AbstractProcessor;

import com.oracle.coherence.testing.TestBinaryCacheStore;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.number.IsCloseTo.closeTo;

import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertThrows;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author jk  2013.11.26
 */
public class ReadWriteBackingMapTest
    {
    @BeforeClass
    public static void setup()
        {
        m_mapInternal    = new LocalCache();
        m_mapPutInternal = new LocalCache();

        ((LocalCache) m_mapInternal).setExpiryDelay(500);

        m_converterToInternal = new Converter()
        {
        @Override
        public Object convert(Object value)
            {
            return ExternalizableHelper.toBinary(value, ctxPof);
            }
        };

        m_converterFromInternal = new Converter()
        {
        @Override
        public Object convert(Object value)
            {
            return ExternalizableHelper.fromBinary((Binary) value, ctxPof);
            }
        };

        m_key1   = toBinary("Key-1");
        m_value1 = toBinary("Value-1");
        m_key2   = toBinary("Key-2");
        m_value2 = toBinary("Value-2");
        m_key3   = toBinary("Key-3");
        m_value3 = toBinary("Value-3");
        m_key4   = toBinary("Key-4");
        m_value4 = toBinary("Value-4");
        }

    @Before
    public void resetMocks()
        {
        m_mapInternal.clear();
        m_mapPutInternal.clear();

        m_mapMisses      = mock(Map.class);
        m_ctxService     = mock(BackingMapManagerContext.class);
        m_mapControl     = mock(SegmentedConcurrentMap.class);
        m_readQueue      = mock(ReadWriteBackingMap.ReadQueue.class);
        m_storeBinary    = mock(BinaryEntryStore.class);

        when(m_ctxService.getKeyToInternalConverter()).thenReturn(m_converterToInternal);
        when(m_ctxService.getValueToInternalConverter()).thenReturn(m_converterToInternal);
        when(m_ctxService.getKeyFromInternalConverter()).thenReturn(m_converterFromInternal);
        when(m_ctxService.getValueFromInternalConverter()).thenReturn(m_converterFromInternal);
        when(m_ctxService.isKeyOwned(m_key1)).thenReturn(true);
        when(m_ctxService.isKeyOwned(m_key2)).thenReturn(false);
        when(m_ctxService.isKeyOwned(m_key3)).thenReturn(true);
        when(m_ctxService.isKeyOwned(m_key4)).thenReturn(false);

        m_data = new LinkedHashMap<Binary,Binary>();
        m_data.put(m_key1, m_value1);
        m_data.put(m_key2, m_value2);
        m_data.put(m_key3, m_value3);
        }

    @After
    public void releaseBackingMap()
        {
        if (m_readWriteBackingMap != null)
            {
            m_readWriteBackingMap.release();
            m_readWriteBackingMap = null;
            }
        }
    
    @Test
    public void shouldCallPutIfPutAllMapHasSizeOfOne() throws Exception
        {
        Map map = Collections.singletonMap(m_key1, m_value1);

        m_readWriteBackingMap = createReadWriteBackingMap(false, 0, 0.5d);
        m_readWriteBackingMap.putAll(map);

        assertThat(m_mapPutInternal.size(), is(1));
        assertThat((Binary) m_mapPutInternal.get(m_key1), is(m_value1));
        }

    @Test
    public void shouldCallPutIfStoreAllIsNotSupported() throws Exception
        {
        m_readWriteBackingMap = createReadWriteBackingMap(false, 0, 0.5d);

        m_readWriteBackingMap.getCacheStore().setStoreAllSupported(false);
        m_readWriteBackingMap.putAll(m_data);
        assertThat(m_mapPutInternal.size(), is(3));
        assertThat((Binary) m_mapPutInternal.get(m_key1), is(m_value1));
        assertThat((Binary) m_mapPutInternal.get(m_key2), is(m_value2));
        assertThat((Binary) m_mapPutInternal.get(m_key3), is(m_value3));
        }

    @Test
    public void shouldCallPutIfWriteBehindConfigured() throws Exception
        {
        m_readWriteBackingMap = createReadWriteBackingMap(false, 500, 0.5d);
        try
            {
            m_readWriteBackingMap.putAll(m_data);
            assertThat(m_mapPutInternal.size(), is(3));
            assertThat((Binary) m_mapPutInternal.get(m_key1), is(m_value1));
            assertThat((Binary) m_mapPutInternal.get(m_key2), is(m_value2));
            assertThat((Binary) m_mapPutInternal.get(m_key3), is(m_value3));
            }
        finally
            {
            // as we have shared state across test instances (mapInternal), any
            // changed state by this test must be reset
            ((ConfigurableCacheMap) m_mapInternal).setEvictionApprover(null);
            }
        }

    @Test
    public void shouldReportWriteBehindRemoveSizeConsistently()
        {
        m_readWriteBackingMap = createReadWriteBackingMap(false, 500, 0.5d, m_storeBinary, true);

        m_mapInternal.put(m_key1, ReadWriteBackingMap.BIN_ERASE_PENDING);
        m_mapInternal.put(m_key2, m_value2);

        assertSize(m_readWriteBackingMap, 1);
        assertThat(m_readWriteBackingMap.getPendingRemoves(), is(Collections.singleton(m_key1)));
        }

    @Test
    public void shouldNotReportNegativeWriteBehindRemoveSize()
        {
        ObservableMap mapInternalPrev = m_mapInternal;
        try
            {
            m_mapInternal = new ObservableHashMap()
                {
                @Override
                public int size()
                    {
                    return 0;
                    }
                };
            m_readWriteBackingMap = createReadWriteBackingMap(false, 500, 0.5d, m_storeBinary, true);
            m_mapInternal.put(m_key1, ReadWriteBackingMap.BIN_ERASE_PENDING);

            assertSize(m_readWriteBackingMap, 0);
            }
        finally
            {
            m_mapInternal = mapInternalPrev;
            }
        }

    @Test
    public void shouldTrackPendingRemovesFromTombstoneTransitions()
        {
        m_readWriteBackingMap = createReadWriteBackingMap(false, 500, 0.5d, m_storeBinary, true);

        m_mapInternal.put(m_key1, ReadWriteBackingMap.BIN_ERASE_PENDING);
        assertThat(m_readWriteBackingMap.getPendingRemoves(), is(Collections.singleton(m_key1)));

        m_mapInternal.put(m_key1, m_value1);
        assertThat(m_readWriteBackingMap.getPendingRemoves(), is(Collections.emptySet()));

        m_mapInternal.put(m_key1, ReadWriteBackingMap.BIN_ERASE_PENDING);
        m_mapInternal.remove(m_key1);
        assertThat(m_readWriteBackingMap.getPendingRemoves(), is(Collections.emptySet()));
        }

    @Test
    public void shouldInitializePendingRemovesFromInternalTombstones()
        {
        m_mapInternal.put(m_key1, ReadWriteBackingMap.BIN_ERASE_PENDING);
        m_mapInternal.put(m_key2, m_value2);

        m_readWriteBackingMap = createReadWriteBackingMap(false, 500, 0.5d, m_storeBinary, true);

        assertSize(m_readWriteBackingMap, 1);
        assertThat(m_readWriteBackingMap.getPendingRemoves(), is(Collections.singleton(m_key1)));
        }

    @Test
    public void shouldSuppressReadThroughForInternalTombstones()
        {
        m_readWriteBackingMap = createReadWriteBackingMap(false, 500, 0.5d, m_storeBinary, true);
        m_mapInternal.put(m_key1, ReadWriteBackingMap.BIN_ERASE_PENDING);

        assertThat(m_readWriteBackingMap.containsKey(m_key1), is(false));
        assertThat(m_readWriteBackingMap.get(m_key1), is((Object) null));
        assertThat(m_readWriteBackingMap.getAll(Collections.singleton(m_key1)), is(Collections.emptyMap()));
        verify(m_storeBinary, never()).load(any());
        verify(m_storeBinary, never()).loadAll(anySet());
        }

    @Test
    public void shouldDistinguishNullValueFromAbsentKeyForContainsKey()
        {
        ObservableMap mapInternalPrev = m_mapInternal;
        try
            {
            m_mapInternal = new ObservableHashMap();
            m_readWriteBackingMap = createReadWriteBackingMap(false, 0, 0.0d);
            m_mapInternal.put(m_key1, null);

            assertThat(m_readWriteBackingMap.containsKey(m_key1), is(true));
            assertThat(m_readWriteBackingMap.containsKey(m_key2), is(false));
            }
        finally
            {
            m_mapInternal = mapInternalPrev;
            }
        }

    @Test
    public void shouldReturnImmutablePendingRemoves()
        {
        m_readWriteBackingMap = createReadWriteBackingMap(false, 500, 0.5d, m_storeBinary, true);
        m_mapInternal.put(m_key1, ReadWriteBackingMap.BIN_ERASE_PENDING);

        Set setPendingRemoves = m_readWriteBackingMap.getPendingRemoves();

        assertThrows(UnsupportedOperationException.class, () -> setPendingRemoves.add(m_key2));
        assertThrows(UnsupportedOperationException.class, () -> setPendingRemoves.remove(m_key1));
        assertThrows(UnsupportedOperationException.class, setPendingRemoves::clear);
        assertThat(m_mapInternal.get(m_key1), is(ReadWriteBackingMap.BIN_ERASE_PENDING));
        }

    @Test
    public void shouldLockAndUnlockEntries() throws Exception
        {
        m_readWriteBackingMap = createReadWriteBackingMap(false, 0, 0.5d);
        m_readWriteBackingMap.putAll(m_data);

        InOrder inOrder = inOrder(m_mapControl);
        inOrder.verify(m_mapControl).lock(m_key1, -1);
        inOrder.verify(m_mapControl).lock(m_key2, -1);
        inOrder.verify(m_mapControl).lock(m_key3, -1);
        inOrder.verify(m_mapControl).unlock(m_key1);
        inOrder.verify(m_mapControl).unlock(m_key2);
        inOrder.verify(m_mapControl).unlock(m_key3);
        }

    @Test
    public void shouldRemoveEntriesFromMissesMap() throws Exception
        {
        m_readWriteBackingMap = createReadWriteBackingMap(false, 0, 0.5d);
        m_readWriteBackingMap.putAll(m_data);

        verify(m_mapMisses).remove(m_key1);
        verify(m_mapMisses).remove(m_key2);
        verify(m_mapMisses).remove(m_key3);
        }

    @Test
    public void shouldCancelAnyPendingReads() throws Exception
        {
        ReadWriteBackingMap.ReadLatch latch1 = mock(ReadWriteBackingMap.ReadLatch.class, "Latch1");
        ReadWriteBackingMap.ReadLatch latch2 = mock(ReadWriteBackingMap.ReadLatch.class, "Latch2");

        when(m_mapControl.get(m_key1)).thenReturn(latch1);
        when(m_mapControl.get(m_key2)).thenReturn(latch2);

        m_readWriteBackingMap = createReadWriteBackingMap(false, 0, 0.5d);
        m_readWriteBackingMap.putAll(m_data);

        verify(m_readQueue).remove(m_key1);
        verify(m_readQueue).remove(m_key2);
        verify(m_readQueue).remove(m_key3);
        verify(latch1).cancel();
        verify(latch2).cancel();
        }

    @Test
    public void shouldCallStoreAllWithOnlyOwnedEntries() throws Exception
        {
        m_readWriteBackingMap = createReadWriteBackingMap(false, 0, 0.5d);
        m_readWriteBackingMap.putAll(m_data);

        ArgumentCaptor<Set> entryCaptor = ArgumentCaptor.forClass(Set.class);
        verify(m_storeBinary).storeAll(entryCaptor.capture());
        Set<Map.Entry> entries = entryCaptor.getValue();
        assertThat(entries.size(), is(2));
        assertThat(entries.contains(new BackingMapBinaryEntry(m_key1, toBinary("Value-1"), null, null)), is(true));
        assertThat(entries.contains(new BackingMapBinaryEntry(m_key3, toBinary("Value-3"), null, null)), is(true));
        }

    @Test
    public void shouldNotCallStoreAllWithEmptySetOfEntries() throws Exception
        {
        m_data.clear();
        m_data.put(m_key2, m_value2);
        m_data.put(m_key4, m_value4);

        m_readWriteBackingMap = createReadWriteBackingMap(false, 0, 0.5d);
        m_readWriteBackingMap.putAll(m_data);

        verify(m_storeBinary, never()).storeAll(anySet());
        }

    @Test
    public void shouldApplyExpiryTimeSetByStore() throws Exception
        {
        m_readWriteBackingMap = createReadWriteBackingMap(false, 0, 0.5d, new TestBinaryCacheStore(5000L));

        m_readWriteBackingMap.putAll(m_data);

        assertThat(getEntryExpiry(m_key1), is(not(0.0d)));
        assertThat(getEntryExpiry(m_key3), is(not(0.0d)));
        }

    @Test
    public void shouldNotApplyExpiryTimeIfNotSetByStore() throws Exception
        {
        m_readWriteBackingMap = createReadWriteBackingMap(false, 0, 0.5d, new TestBinaryCacheStore(Integer.MIN_VALUE));
        m_readWriteBackingMap.putAll(m_data);

        assertThat(getEntryExpiry(m_key1), is(0.0));
        assertThat(getEntryExpiry(m_key3), is(0.0));
        }

    @Test(expected = UnsupportedOperationException.class)
    public void shouldThrowExceptionIfExpirySetByStoreAndInternalMapNotCacheMap() throws Exception
        {
        ObservableMap mapInternalPrev = m_mapInternal;
        try
            {
            m_mapInternal = new ObservableHashMap();
            m_readWriteBackingMap = createReadWriteBackingMap(false, 0, 0.5d, new TestBinaryCacheStore(5000L));
            m_readWriteBackingMap.putAll(m_data);
            }
        finally
            {
            // shared state reset
            m_mapInternal = mapInternalPrev;
            }
        }

    @Test
    public void shouldSetExpiryToOneForRemovedEntries() throws Exception
        {
        TestBinaryCacheStore store = new TestBinaryCacheStore(Integer.MIN_VALUE);
        store.setProcessor(new AbstractProcessor()
            {
            @Override
            public Object process(InvocableMap.Entry entry)
                {
                entry.remove(false);
                return null;
                }
            });

        m_readWriteBackingMap = createReadWriteBackingMap(false, 0, 0.5d, store);

        ConfigurableCacheMap mapInternal  = (ConfigurableCacheMap) m_mapInternal;
        EvictionApprover     approverPrev = mapInternal.getEvictionApprover();
        try
            {
            mapInternal.setEvictionApprover(EvictionApprover.DISAPPROVER);
            m_readWriteBackingMap.putAll(m_data);

            double expectedTime = System.currentTimeMillis() + 1L;
            assertThat(getEntryExpiry(m_key1), is(closeTo(expectedTime, 500.0d)));
            }
        finally
            {
            mapInternal.setEvictionApprover(approverPrev);
            }
        }

    @Test
    public void shouldUseValueMutatedByStore() throws Exception
        {
        TestBinaryCacheStore store = new TestBinaryCacheStore(Integer.MIN_VALUE);
        store.setProcessor(new AbstractProcessor()
            {
            @Override
            public Object process(InvocableMap.Entry entry)
                {
                String value = (String) entry.getValue();
                entry.setValue("Mutated-" + value);
                return null;
                }
            });

        m_readWriteBackingMap = createReadWriteBackingMap(false, 0, 0.5d, store);

        m_readWriteBackingMap.putAll(m_data);
        assertThat((Binary) m_mapInternal.get(m_key1), is(toBinary("Mutated-Value-1")));
        assertThat((Binary) m_mapInternal.get(m_key3), is(toBinary("Mutated-Value-3")));
        }

    protected static Binary toBinary(Object o)
        {
        return ExternalizableHelper.toBinary(o, ctxPof);
        }

    protected static Object fromBinary(Object o)
        {
        return ExternalizableHelper.fromBinary((Binary) o, ctxPof);
        }

    protected double getEntryExpiry(Binary key)
        {
        LocalCache.Entry entry = (LocalCache.Entry) ((LocalCache) m_mapInternal).getEntry(key);
        return entry.getExpiryMillis();
        }

    protected void assertSize(ReadWriteBackingMap map, int cExpected)
        {
        assertThat(map.size(), is(cExpected));
        assertThat(map.keySet().size(), is(cExpected));
        assertThat(map.entrySet().size(), is(cExpected));
        assertThat(map.values().size(), is(cExpected));
        }

    protected ReadWriteBackingMap createReadWriteBackingMap(boolean fReadOnly, int cWriteBehindSeconds,
                double dflRefreshAheadFactor)
        {
        return createReadWriteBackingMap(fReadOnly, cWriteBehindSeconds, dflRefreshAheadFactor, m_storeBinary);
        }

    protected ReadWriteBackingMap createReadWriteBackingMap(boolean fReadOnly, int cWriteBehindSeconds,
                double dflRefreshAheadFactor, BinaryEntryStore store)
        {
        return createReadWriteBackingMap(fReadOnly, cWriteBehindSeconds, dflRefreshAheadFactor, store,
                ReadWriteBackingMap.RWBM_WB_REMOVE_DEFAULT);
        }

    protected ReadWriteBackingMap createReadWriteBackingMap(boolean fReadOnly, int cWriteBehindSeconds,
                double dflRefreshAheadFactor, BinaryEntryStore store, boolean fWriteBehindRemove)
        {
        return new ReadWriteBackingMap(m_ctxService, m_mapInternal, m_mapMisses, store,
                                       fReadOnly, cWriteBehindSeconds, dflRefreshAheadFactor, fWriteBehindRemove)
            {
            @Override
            protected ConcurrentMap instantiateControlMap()
                {
                return m_mapControl;
                }

            @Override
            protected ReadQueue instantiateReadQueue()
                {
                return m_readQueue;
                }

            @Override
            protected Object putInternal(Object oKey, Object oValue, long cMillis)
                {
                return m_mapPutInternal.put(oKey, oValue, cMillis);
                }
            };
        }

    protected ReadWriteBackingMap m_readWriteBackingMap;
    
    protected static ConfigurablePofContext ctxPof = new ConfigurablePofContext("coherence-pof-config.xml");

    protected static Map                           m_mapMisses;
    protected static ObservableMap                 m_mapInternal;
    protected static LocalCache                    m_mapPutInternal;
    protected static BinaryEntryStore              m_storeBinary;
    protected static BackingMapManagerContext      m_ctxService;
    protected static SegmentedConcurrentMap        m_mapControl;
    protected static ReadWriteBackingMap.ReadQueue m_readQueue;
    protected static Converter                     m_converterToInternal;
    protected static Converter                     m_converterFromInternal;
    protected static LinkedHashMap<Binary,Binary>  m_data;
    protected static Binary                        m_key1;
    protected static Binary                        m_key2;
    protected static Binary                        m_key3;
    protected static Binary                        m_key4;
    protected static Binary                        m_value1;
    protected static Binary                        m_value2;
    protected static Binary                        m_value3;
    protected static Binary                        m_value4;
    }
