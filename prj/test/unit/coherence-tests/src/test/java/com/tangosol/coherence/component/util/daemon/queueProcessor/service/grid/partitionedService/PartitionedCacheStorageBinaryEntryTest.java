/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService;

import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.partitionedCache.Storage;

import com.tangosol.net.NamedCache;

import com.tangosol.net.cache.CacheMap;
import com.tangosol.net.cache.WrapperNamedCache;

import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;

import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

/**
* Unit tests for the PartitionedCache.Storage.BinaryEntry.
*
* @author tb  2011.01.10
*/
public class PartitionedCacheStorageBinaryEntryTest
    {
    /**
    * Test expire on PartitionedCache.Storage.BinaryEntry.
    */
    @Test
    public void testExpire()
        {
        Binary binVal  = new Binary("value".getBytes());
        Binary binVal2 = new Binary("value2".getBytes());

        // create mocks
        PartitionedCache mockService = mock(PartitionedCache.class);

        NamedCache  backingMap = new WrapperNamedCache(new HashMap(), "test");
        TestStorage storage    = new TestStorage(backingMap, mockService);

        // set expectations
        when(mockService.getClusterTime()).thenReturn(9999L);

        Storage.BinaryEntry entry = new Storage.BinaryEntry(null, storage, true);

        entry.updateBinaryValue(binVal);

        // set expiry
        entry.expire(1000L);

        // assert that the expiry value has been set
        assertEquals(1000L, entry.getExpiry());

        // update the binary value
        entry.updateBinaryValue(binVal2);

        // assert that the expiry is still set
        assertEquals(1000L, entry.getExpiry());

        // set an expiry value of 0
        entry.expire(0);

        // assert that the default expiry is set
        assertEquals(CacheMap.EXPIRY_DEFAULT, entry.getExpiry());

        // update the binary value
        entry.updateBinaryValue(binVal);

        // assert that the default expiry is still set
        assertEquals(CacheMap.EXPIRY_DEFAULT, entry.getExpiry());

        // set an negative expiry value
        entry.expire(-10);

        // assert that never expire is set
        assertEquals(CacheMap.EXPIRY_NEVER, entry.getExpiry());

        // update the binary value
        entry.updateBinaryValue(binVal2);

        // assert that never expire is still set
        assertEquals(CacheMap.EXPIRY_NEVER, entry.getExpiry());
        }


    // ----- helper methods -------------------------------------------------

    /**
    * Get the expiry value from the given binary value.
    *
    * @param binValue  the binary value
    *
    * @return the decoded expiry
    */
    private static long decodeExpiry(Binary binValue)
        {
        Binary binExpiry = ExternalizableHelper.getDecoration(binValue, ExternalizableHelper.DECO_EXPIRY);
        long   cMillis   = CacheMap.EXPIRY_DEFAULT;

        if (binExpiry != null)
            {
            try
                {
                long ldtTime   = 9999L;
                long ldtExpiry = binExpiry.getBufferInput().readLong();
                cMillis = ldtExpiry == CacheMap.EXPIRY_NEVER
                        ? ldtExpiry : Math.max(ldtExpiry - ldtTime, 1L);
                }
            catch (Exception e) {/*Do nothing*/}
            }

        return cMillis;
        }


    // ----- inner classes --------------------------------------------------

    /**
    * Testable PartitionedCache.Storage
    */
    class TestStorage extends Storage
        {
        /**
        * Construct a TestStorage
        *
        * @param backingMap  the backing map
        * @param service     the parent service
        */
        TestStorage(NamedCache backingMap, PartitionedCache service)
            {
            super(null, null, true);

            m_service = service;
            setBackingMapInternal(backingMap);
            }

        // ----- PartitionedCache.Storage overrides ---------------------
        /**
        * Initialize.
        */
        public void onInit()
            {
            }

        /**
        * Get the parent service.
        *
        * @return the service
        */
        public PartitionedCache getService()
            {
            return m_service;
            }

        // ----- data members -------------------------------------------

        /**
        * The parent service.
        */
        private PartitionedCache m_service;
        }
    }
