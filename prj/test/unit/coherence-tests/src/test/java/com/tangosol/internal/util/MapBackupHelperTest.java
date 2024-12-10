/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util;

import com.tangosol.net.DistributedCacheService;
import com.tangosol.net.cache.WrapperNamedCache;

import com.tangosol.util.Base;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import java.util.HashMap;
import java.util.Map;

import static junit.framework.Assert.assertEquals;

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
    }
