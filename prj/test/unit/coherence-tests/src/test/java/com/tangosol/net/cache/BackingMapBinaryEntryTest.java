/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
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
        // bump the jitter time to account for observed jump causing derived
        // remaining expiry time to be off by 32ms
        System.setProperty("coherence.safeclock.jitter", "32");

        Binary binKey     = new Binary("key".getBytes());
        Binary binVal     = new Binary("value".getBytes());
        Binary binValOrig = new Binary("valueOriginal".getBytes());

        BackingMapBinaryEntry entry =
                new BackingMapBinaryEntry(binKey, binVal, binValOrig, null);

        long ldt1 = Base.getSafeTimeMillis() + 1000L;

        entry.expire(1000L);
        Base.sleep(100L);

        long ldt2   = Base.getSafeTimeMillis() + entry.getExpiry();
        long ldtOff = Math.abs(ldt1 - ldt2);

        assertTrue("Expiry is off by " + ldtOff,  ldtOff <= 5L);

        entry.expire(-19L);
        assertEquals(CacheMap.EXPIRY_NEVER, entry.getExpiry());

        entry.expire(0);
        assertEquals(CacheMap.EXPIRY_DEFAULT, entry.getExpiry());
        }
    }
