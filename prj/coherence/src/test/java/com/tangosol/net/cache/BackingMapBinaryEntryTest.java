/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.cache;

import com.tangosol.util.Base;
import com.tangosol.util.Binary;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
* Unit tests for the BackingMapBinaryEntry.
*
* @author tb  2011.01.10
*/
public class BackingMapBinaryEntryTest
    {

    /**
    * Test expire on BackingMapBinaryEntry.
    */
    @Test
    public void testExpire()
        {
        Binary binKey     = new Binary("key".getBytes());
        Binary binVal     = new Binary("value".getBytes());
        Binary binVal2    = new Binary("value2".getBytes());
        Binary binValOrig = new Binary("valueOriginal".getBytes());


        BackingMapBinaryEntry entry =
                new BackingMapBinaryEntry(binKey, binVal, binValOrig, null);

        long ldt1 = Base.getSafeTimeMillis() + 100L;
        entry.expire(100L);
        Base.sleep(10L);
        long ldt2   = Base.getSafeTimeMillis() + entry.getExpiry() ;
        long ldtOff = Math.abs(ldt1 - ldt2);
        assertTrue("Expiry is off by " + ldtOff,  ldtOff <= 1L);

        entry.expire(-19L);
        assertEquals(CacheMap.EXPIRY_NEVER, entry.getExpiry());

        entry.expire(0);
        assertEquals(CacheMap.EXPIRY_DEFAULT, entry.getExpiry());
        }
    }
