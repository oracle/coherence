/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util;

import com.tangosol.io.ByteArrayReadBuffer;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.ReadBuffer.BufferInput;
import com.tangosol.io.SerializationRole;
import com.tangosol.io.internal.SerializationAllowlist;

import com.tangosol.net.DistributedCacheService;
import com.tangosol.net.cache.WrapperNamedCache;

import com.tangosol.util.Base;
import com.tangosol.util.ExternalizableHelper;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author jk 2014.05.13
 */
public class MapBackupHelperTest
    {
    @Test
    public void shouldBackupMap()
            throws IOException
        {
        assertBackupMap(new HashMap());
        }

    @Test
    public void shouldBackupNamedCache()
            throws IOException
        {
        assertBackupMap(new WrapperNamedCache(new HashMap(), "test"));
        }

    @Test
    public void shouldBackupDistributedCache()
            throws IOException
        {
        DistributedCacheService service = mock(DistributedCacheService.class);
        when(service.getPartitionCount()).thenReturn(257);

        assertBackupMap(new WrapperNamedCache(new HashMap(), "test", service));

        verify(service, times(1)).getPartitionCount();
        }

    @Test
    public void shouldReadMapUnderToolingRole()
            throws IOException
        {
        assertReadMapRole(0);
        }

    @Test
    public void shouldReadMapBlocksUnderToolingRole()
            throws IOException
        {
        assertReadMapRole(1);
        }

    protected void assertBackupMap(Map mapSrc) throws IOException
        {
        for (int i = 0; i < 100; i++)
            {
            mapSrc.put(i, Base.getRandom().nextInt());
            }

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        DataOutputStream out    = new DataOutputStream(stream);

        MapBackupHelper.writeMap(out, mapSrc);

        Map mapTarget = new HashMap();

        DataInputStream in = new DataInputStream(new ByteArrayInputStream(stream.toByteArray()));

        MapBackupHelper.readMap(in, mapTarget, 0, null);

        assertEquals(mapSrc, mapTarget);
        }

    protected void assertReadMapRole(int cBlock)
            throws IOException
        {
        Map mapSrc = new HashMap();
        mapSrc.put(new RoleAwareValue("key-1"), new RoleAwareValue("value-1"));
        mapSrc.put(new RoleAwareValue("key-2"), new RoleAwareValue("value-2"));

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        DataOutputStream      out    = new DataOutputStream(stream);

        MapBackupHelper.writeMap(out, mapSrc);

        Map         mapTarget = new HashMap();
        BufferInput in        = new ByteArrayReadBuffer(stream.toByteArray()).getBufferInput();

        RoleAwareValue.reset();
        String sAllowedOld = System.getProperty(SerializationAllowlist.PROP_SERIALIZATION_ALLOWED);
        try
            {
            System.setProperty(SerializationAllowlist.PROP_SERIALIZATION_ALLOWED, RoleAwareValue.class.getName());
            MapBackupHelper.readMap(in, mapTarget, cBlock, RoleAwareValue.class.getClassLoader());
            }
        finally
            {
            if (sAllowedOld == null)
                {
                System.clearProperty(SerializationAllowlist.PROP_SERIALIZATION_ALLOWED);
                }
            else
                {
                System.setProperty(SerializationAllowlist.PROP_SERIALIZATION_ALLOWED, sAllowedOld);
                }
            }

        assertEquals(mapSrc, mapTarget);
        assertFalse(RoleAwareValue.roles().isEmpty());
        assertTrue(RoleAwareValue.roles().stream().allMatch(role -> role == SerializationRole.TOOLING));
        assertEquals(SerializationRole.UNCLASSIFIED, SerializationRole.current());
        }

    // ----- inner class: RoleAwareValue -----------------------------------

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
    }
