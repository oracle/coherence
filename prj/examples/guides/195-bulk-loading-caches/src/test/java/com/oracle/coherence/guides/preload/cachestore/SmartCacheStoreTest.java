/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.guides.preload.cachestore;

import com.tangosol.io.DefaultSerializer;
import com.tangosol.io.Serializer;
import com.tangosol.net.cache.CacheStore;
import com.tangosol.util.Binary;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.ExternalizableHelper;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
public class SmartCacheStoreTest
    {
    @Test
    public void shouldCallLoad()
        {
        CacheStore<String, String> delegate = mock(CacheStore.class);
        BinaryEntry<String, String> binaryEntry = mock(BinaryEntry.class);

        when(delegate.load("foo")).thenReturn("bar");
        when(binaryEntry.getKey()).thenReturn("foo");

        SmartCacheStore<String, String> cacheStore = new SmartCacheStore<>(delegate);
        cacheStore.load(binaryEntry);

        verify(delegate).load("foo");
        verify(binaryEntry).setValue("bar");
        }

    @Test
    public void shouldCallLoadAll()
        {
        CacheStore<String, String> delegate = mock(CacheStore.class);
        BinaryEntry<String, String> binaryEntry1 = mock(BinaryEntry.class, "1");
        BinaryEntry<String, String> binaryEntry2 = mock(BinaryEntry.class, "2");
        BinaryEntry<String, String> binaryEntry3 = mock(BinaryEntry.class, "3");

        Map<String, String> loaded = new HashMap<>();
        loaded.put("One", "Value-One");
        loaded.put("Three", "Value-Three");

        when(delegate.loadAll(anyCollection())).thenReturn(loaded);
        when(binaryEntry1.getKey()).thenReturn("One");
        when(binaryEntry2.getKey()).thenReturn("Two");
        when(binaryEntry3.getKey()).thenReturn("Three");

        SmartCacheStore<String, String> cacheStore = new SmartCacheStore<>(delegate);
        cacheStore.loadAll(Set.of(binaryEntry1, binaryEntry2, binaryEntry3));

        verify(delegate).loadAll(Set.of("One", "Two", "Three"));
        verify(binaryEntry1).setValue("Value-One");
        verify(binaryEntry2, never()).setValue(any());
        verify(binaryEntry3).setValue("Value-Three");
        }

    @Test
    public void shouldCallErase()
        {
        CacheStore<String, String> delegate = mock(CacheStore.class);
        BinaryEntry<String, String> binaryEntry = mock(BinaryEntry.class);

        when(binaryEntry.getKey()).thenReturn("foo");

        SmartCacheStore<String, String> cacheStore = new SmartCacheStore<>(delegate);
        cacheStore.erase(binaryEntry);

        verify(delegate).erase("foo");
        }

    @Test
    public void shouldCallEraseAll()
        {
        CacheStore<String, String> delegate = mock(CacheStore.class);
        BinaryEntry<String, String> binaryEntry1 = mock(BinaryEntry.class, "1");
        BinaryEntry<String, String> binaryEntry2 = mock(BinaryEntry.class, "2");
        BinaryEntry<String, String> binaryEntry3 = mock(BinaryEntry.class, "3");

        when(binaryEntry1.getKey()).thenReturn("One");
        when(binaryEntry2.getKey()).thenReturn("Two");
        when(binaryEntry3.getKey()).thenReturn("Three");

        SmartCacheStore<String, String> cacheStore = new SmartCacheStore<>(delegate);
        cacheStore.eraseAll(Set.of(binaryEntry1, binaryEntry2, binaryEntry3));

        verify(delegate).eraseAll(Set.of("One", "Two", "Three"));
        }

    @Test
    public void shouldCallStoreWithUndecoratedBinary()
        {
        CacheStore<String, String> delegate = mock(CacheStore.class);
        BinaryEntry<String, String> binaryEntry = mock(BinaryEntry.class);
        Binary binaryValue = toBinary("bar");

        when(binaryEntry.getKey()).thenReturn("foo");
        when(binaryEntry.getBinaryValue()).thenReturn(binaryValue);
        when(binaryEntry.getValue()).thenReturn("bar");

        SmartCacheStore<String, String> cacheStore = new SmartCacheStore<>(delegate);
        cacheStore.store(binaryEntry);

        verify(delegate).store("foo", "bar");
        }

    @Test
    public void shouldNotCallStoreWithDecoratedBinary()
        {
        CacheStore<String, String> delegate = mock(CacheStore.class);
        BinaryEntry<String, String> binaryEntry = mock(BinaryEntry.class);
        Binary binaryValue = toDecoratedBinary("bar");

        when(binaryEntry.getKey()).thenReturn("foo");
        when(binaryEntry.getBinaryValue()).thenReturn(binaryValue);
        when(binaryEntry.getValue()).thenReturn("bar");

        SmartCacheStore<String, String> cacheStore = new SmartCacheStore<>(delegate);
        cacheStore.store(binaryEntry);

        verifyNoInteractions(delegate);
        }

    @Test
    public void shouldOnlyCallStoreAllWithUndecoratedBinaries()
        {
        CacheStore<String, String> delegate = mock(CacheStore.class);
        BinaryEntry<String, String> binaryEntry1 = mock(BinaryEntry.class, "1");
        Binary binaryValue1 = toBinary("One");
        BinaryEntry<String, String> binaryEntry2 = mock(BinaryEntry.class, "2");
        Binary binaryValue2 = toDecoratedBinary("Two");
        BinaryEntry<String, String> binaryEntry3 = mock(BinaryEntry.class, "3");
        Binary binaryValue3 = toBinary("Three");
        BinaryEntry<String, String> binaryEntry4 = mock(BinaryEntry.class, "4");
        Binary binaryValue4 = toDecoratedBinary("Four");

        when(binaryEntry1.getKey()).thenReturn("One");
        when(binaryEntry1.getBinaryValue()).thenReturn(binaryValue1);
        when(binaryEntry1.getValue()).thenReturn("Value-One");
        when(binaryEntry2.getKey()).thenReturn("Two");
        when(binaryEntry2.getBinaryValue()).thenReturn(binaryValue2);
        when(binaryEntry2.getValue()).thenReturn("Value-Two");
        when(binaryEntry3.getKey()).thenReturn("Three");
        when(binaryEntry3.getBinaryValue()).thenReturn(binaryValue3);
        when(binaryEntry3.getValue()).thenReturn("Value-Three");
        when(binaryEntry4.getKey()).thenReturn("Four");
        when(binaryEntry4.getBinaryValue()).thenReturn(binaryValue4);
        when(binaryEntry4.getValue()).thenReturn("Value-Four");

        SmartCacheStore<String, String> cacheStore = new SmartCacheStore<>(delegate);
        cacheStore.storeAll(Set.of(binaryEntry1, binaryEntry2, binaryEntry3, binaryEntry4));

        verify(delegate).storeAll(Map.of("One", "Value-One", "Three", "Value-Three"));
        }

    // ----- helper methods -------------------------------------------------

    Binary toBinary(Object o)
        {
        return ExternalizableHelper.toBinary(o, serializer);
        }

    Binary toDecoratedBinary(Object o)
        {
        Binary binary = toBinary(o);
        return ExternalizableHelper.decorate(binary, SmartCacheStore.DEFAULT_DECORATION_ID, DEFAULT_DECORATION);
        }

    // ----- data members ---------------------------------------------------

    public static final Serializer serializer = new DefaultSerializer();

    public static final Binary DEFAULT_DECORATION = Binary.NO_BINARY;
    }
