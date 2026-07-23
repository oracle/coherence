/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.cache;

import com.tangosol.io.BinaryStore;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.SerializationRole;

import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link BinaryStoreCacheStore}.
 *
 * @author Aleks Seovic  2026.05.15
 */
public class BinaryStoreCacheStoreTest
    {
    @Test
    public void shouldMaterializeValuesUnderCacheStoreRole()
        {
        MapBinaryStore binaryStore = new MapBinaryStore();
        BinaryStoreCacheStore<RoleAwareValue, RoleAwareValue> cacheStore =
                new BinaryStoreCacheStore<>(binaryStore);

        RoleAwareValue.reset();
        RoleAwareValue key   = new RoleAwareValue("key");
        RoleAwareValue value = new RoleAwareValue("value");
        binaryStore.store(ExternalizableHelper.toBinary(key), ExternalizableHelper.toBinary(value));

        assertEquals(value, cacheStore.load(key));
        assertFalse(RoleAwareValue.roles().isEmpty());
        assertTrue(RoleAwareValue.roles().stream().allMatch(role -> role == SerializationRole.CACHE_STORE));
        assertEquals(SerializationRole.UNCLASSIFIED, SerializationRole.current());
        }

    @Test
    public void shouldMaterializeKeysUnderCacheStoreRole()
        {
        MapBinaryStore binaryStore = new MapBinaryStore();
        BinaryStoreCacheStore<RoleAwareValue, RoleAwareValue> cacheStore =
                new BinaryStoreCacheStore<>(binaryStore);

        RoleAwareValue.reset();
        RoleAwareValue key = new RoleAwareValue("key");
        binaryStore.store(ExternalizableHelper.toBinary(key), ExternalizableHelper.toBinary("value"));

        Iterator<RoleAwareValue> iter = cacheStore.keys();
        assertEquals(key, iter.next());
        assertFalse(iter.hasNext());
        assertFalse(RoleAwareValue.roles().isEmpty());
        assertTrue(RoleAwareValue.roles().stream().allMatch(role -> role == SerializationRole.CACHE_STORE));
        assertEquals(SerializationRole.UNCLASSIFIED, SerializationRole.current());
        }

    @Test
    public void shouldRestoreRoleAfterKeyMaterializationFailure()
        {
        MapBinaryStore binaryStore = new MapBinaryStore();
        BinaryStoreCacheStore<RejectingRoleAwareValue, Object> cacheStore =
                new BinaryStoreCacheStore<>(binaryStore);

        RejectingRoleAwareValue.reset();
        binaryStore.store(ExternalizableHelper.toBinary(new RejectingRoleAwareValue()),
                ExternalizableHelper.toBinary("value"));

        assertThrows(RuntimeException.class, () -> cacheStore.keys().next());
        assertEquals(Collections.singletonList(SerializationRole.CACHE_STORE), RejectingRoleAwareValue.roles());
        assertEquals(SerializationRole.UNCLASSIFIED, SerializationRole.current());
        }

    // ----- inner class: MapBinaryStore -----------------------------------

    /**
     * Simple binary store for tests.
     */
    private static class MapBinaryStore
            implements BinaryStore
        {
        @Override
        public Binary load(Binary binKey)
            {
            return m_map.get(binKey);
            }

        @Override
        public void store(Binary binKey, Binary binValue)
            {
            m_map.put(binKey, binValue);
            }

        @Override
        public void erase(Binary binKey)
            {
            m_map.remove(binKey);
            }

        @Override
        public void eraseAll()
            {
            m_map.clear();
            }

        @Override
        public Iterator<Binary> keys()
            {
            return m_map.keySet().iterator();
            }

        private final Map<Binary, Binary> m_map = new LinkedHashMap<>();
        }

    // ----- inner class: RoleAwareValue -----------------------------------

    /**
     * Value that records the active serialization role during materialization.
     */
    public static class RoleAwareValue
            implements ExternalizableLite
        {
        public RoleAwareValue()
            {
            }

        RoleAwareValue(String sValue)
            {
            m_sValue = sValue;
            }

        @Override
        public void readExternal(DataInput in)
                throws IOException
            {
            s_listRoles.add(SerializationRole.current());
            m_sValue = ExternalizableHelper.readSafeUTF(in);
            }

        @Override
        public void writeExternal(DataOutput out)
                throws IOException
            {
            ExternalizableHelper.writeSafeUTF(out, m_sValue);
            }

        @Override
        public boolean equals(Object o)
            {
            return o instanceof RoleAwareValue
                    && java.util.Objects.equals(m_sValue, ((RoleAwareValue) o).m_sValue);
            }

        @Override
        public int hashCode()
            {
            return java.util.Objects.hashCode(m_sValue);
            }

        static void reset()
            {
            s_listRoles.clear();
            }

        static List<SerializationRole> roles()
            {
            return s_listRoles;
            }

        private String m_sValue;

        private static final List<SerializationRole> s_listRoles = new ArrayList<>();
        }

    // ----- inner class: RejectingRoleAwareValue ---------------------------

    /**
     * Value that records the active serialization role and then rejects.
     */
    public static class RejectingRoleAwareValue
            implements ExternalizableLite
        {
        @Override
        public void readExternal(DataInput in)
                throws IOException
            {
            s_listRoles.add(SerializationRole.current());
            throw new IOException("rejecting role-aware value");
            }

        @Override
        public void writeExternal(DataOutput out)
            {
            }

        static void reset()
            {
            s_listRoles.clear();
            }

        static List<SerializationRole> roles()
            {
            return s_listRoles;
            }

        private static final List<SerializationRole> s_listRoles = new ArrayList<>();
        }
    }
