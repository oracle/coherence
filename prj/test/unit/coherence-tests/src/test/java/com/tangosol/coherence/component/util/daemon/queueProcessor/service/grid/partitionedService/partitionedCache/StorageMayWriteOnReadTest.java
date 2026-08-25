/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.partitionedCache;

import com.tangosol.net.cache.CacheLoader;
import com.tangosol.net.cache.ReadWriteBackingMap;

import com.tangosol.util.ObservableHashMap;
import com.tangosol.util.ObservableMap;
import com.tangosol.util.SafeHashMap;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Unit tests for {@link Storage#mayWriteOnRead()}.
 *
 * @author Aleks Seovic  2026.04.27
 * @since 26.07
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class StorageMayWriteOnReadTest
    {
    @Test
    public void shouldReturnFalseForPlainBackingMap()
        {
        assertThat(storage(plainBackingMap(), false).mayWriteOnRead(), is(false));
        }

    @Test
    public void shouldReturnTrueForReadWriteBackingMapWithReadThrough()
        {
        assertThat(storage(readWriteBackingMapWithReadThrough(), false).mayWriteOnRead(), is(true));
        }

    @Test
    public void shouldReturnTrueForReadWriteBackingMapWithoutReadThrough()
        {
        assertThat(storage(readWriteBackingMapWithoutReadThrough(), false).mayWriteOnRead(), is(true));
        }

    @Test
    public void shouldReturnTrueForSlidingExpiryOnly()
        {
        assertThat(storage(plainBackingMap(), true).mayWriteOnRead(), is(true));
        }

    @Test
    public void shouldReturnTrueForReadWriteBackingMapWithSlidingExpiry()
        {
        assertThat(storage(readWriteBackingMapWithReadThrough(), true).mayWriteOnRead(), is(true));
        }

    @Test
    public void shouldCacheDecisionOnceComputed()
        {
        Storage storage = storage(plainBackingMap(), false);

        assertThat(storage.mayWriteOnRead(), is(false));

        storage.setBackingMapInternal(readWriteBackingMapWithReadThrough());
        storage.setExpirySliding(true);

        assertThat(storage.mayWriteOnRead(), is(false));
        }

    private static Storage storage(ObservableMap mapBacking, boolean fExpirySliding)
        {
        Storage storage = new TestStorage();
        storage.setBackingMapInternal(mapBacking);
        storage.setExpirySliding(fExpirySliding);
        return storage;
        }

    private static ObservableMap plainBackingMap()
        {
        return new ObservableHashMap();
        }

    private static ReadWriteBackingMap readWriteBackingMapWithReadThrough()
        {
        return new ReadWriteBackingMap(null, new ObservableHashMap(), new SafeHashMap(),
                (CacheLoader<Object, Object>) key -> null);
        }

    private static ReadWriteBackingMap readWriteBackingMapWithoutReadThrough()
        {
        return new ReadWriteBackingMap(null, new ObservableHashMap(), new SafeHashMap(),
                (CacheLoader<Object, Object>) null);
        }

    /**
     * Test fixture that avoids full component service initialization.
     */
    public static class TestStorage
            extends Storage
        {
        @Override
        public void onInit()
            {
            }
        }
    }
