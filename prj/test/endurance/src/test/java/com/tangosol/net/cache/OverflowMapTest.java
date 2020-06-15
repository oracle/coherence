/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.cache;


import com.tangosol.util.MapTest;
import com.tangosol.util.ObservableHashMap;
import com.tangosol.util.ObservableMap;
import com.tangosol.util.SafeHashMap;

import org.junit.Test;

import java.util.Map;

import static com.tangosol.util.MapTest.*;

import static org.junit.Assert.*;


/**
* Test for the OverflowMap.
*
* @author cp  2005.11.17
* @since Coherence 3.1
*/
public class OverflowMapTest
    {
    // ----- automated tests ------------------------------------------------

    /**
    * Executes a variety of tests against the OverflowMap and
    * SimpleOverflowMap implementations.
    */
    @Test
    public void testOverflow()
        {
        for (int nOverflowType = 0; nOverflowType < 3; ++nOverflowType)
            {
            for (int nFrontType = 0; nFrontType < 6; ++nFrontType)
                {
                for (int nBackType = 0; nBackType < 4; ++nBackType)
                    {
                    testOverflow(nOverflowType, nFrontType, nBackType);
                    }
                }
            }
        }

    /**
    * Executes a specific tests against an OverflowMap or SimpleOverflowMap
    * implementation.
    *
    * @param nOverflowType
    * @param nFrontType
    * @param nBackType
    */
    public static void testOverflow(int nOverflowType, int nFrontType, int nBackType)
        {
        ObservableMap mapFront;
        Map           mapBack;
        Map           mapOverflow;
        String        sFront;
        String        sBack;
        String        sOverflow;

        switch (nFrontType)
            {
            case 0:
                mapFront = new LocalCache(10);
                sFront   = "LocalCache(10)";
                break;
            case 1:
                mapFront = new LocalCache(100);
                sFront   = "LocalCache(100)";
                break;
            case 2:
                mapFront = new LocalCache(10000);
                sFront   = "LocalCache(10000)";
                break;
            case 3:
                mapFront = new LocalCache(10, 1);
                sFront   = "LocalCache(10, 1ms)";
                break;
            case 4:
                mapFront = new LocalCache(100, 1);
                sFront   = "LocalCache(100, 1ms)";
                break;
            case 5:
                mapFront = new LocalCache(10000, 1);
                sFront   = "LocalCache(10000, 1ms)";
                break;
            default:
                fail("illegal type: " + nFrontType);
                return;
            }

        switch (nBackType)
            {
            case 0:
                mapBack = new SafeHashMap();
                sBack   = "SafeHashMap()";
                break;
            case 1:
                mapBack = new ObservableHashMap();
                sBack   = "ObservableHashMap()";
                break;
            case 2:
                mapBack = new SerializationMap(instantiateTestBinaryStore());
                sBack   = "SerializationMap(BinaryStore)";
                break;
            case 3:
                mapBack = new SerializationCache(instantiateTestBinaryStore(), 10000);
                sBack   = "SerializationCache(BinaryStore, 10000)";
                break;
            default:
                fail("illegal type: " + nBackType);
                return;
            }

        switch (nOverflowType)
            {
            case 0:
                mapOverflow = new OverflowMap(mapFront, mapBack);
                sOverflow   = "OverflowMap";
                break;
            case 1:
                mapOverflow = new SimpleOverflowMap(mapFront, mapBack);
                sOverflow   = "SimpleOverflowMap";
                break;
            case 2:
                mapOverflow = new SimpleOverflowMap(mapFront, mapBack, new LocalCache(5));
                sOverflow   = "SimpleOverflowMap with miss cache";
                break;
            default:
                fail("illegal type: " + nOverflowType);
                return;
            }

        System.out.println("Testing " + sOverflow + ", front=" + sFront + ", back=" + sBack);
        if (mapOverflow instanceof ObservableMap)
            {
            testObservableMap((ObservableMap) mapOverflow);
            }
        else
            {
            testMap(mapOverflow);
            }
        mapOverflow.clear();
        testMultithreadedMap(mapOverflow);
        }

    /**
    * This is a brutal multi-threaded test with expiry.
    */
    public static void testExpiry()
        {
        for (int i = 0; i < 10; ++i)
            {
            OverflowMap mapTest = new OverflowMap(new LocalCache(10, 1),
                                          new LocalCache(100, 5));
            mapTest.setExpiryDelay(3);

            int cKeys    = 200;
            int cThreads = 8;
            int cIters   = 10000;

            System.out.println("- Running multi-threaded test of " + cIters + " iterations against "
                + mapTest.getClass().getName() + " with " + cKeys
                + " keys on " + cThreads + " threads");
            long lStart = System.currentTimeMillis();

            new MapTest().startTestDaemons(new Map[] {mapTest}, cKeys, cThreads, cIters, false);

            long lStop = System.currentTimeMillis();
            System.out.println("Test completed in " + (lStop - lStart) + "ms");
            }
        }
    }
