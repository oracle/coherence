/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.cache;


import com.oracle.coherence.testing.cache.BaseContinuousQueryCacheTest;
import com.tangosol.util.MapTest;

import org.junit.Test;

import java.util.Map;


/**
* Extra unit tests for the ContinuousQueryCache.
*
* @author cp  Jan 24, 2006
*/
public class ContinuousQueryCacheExtraTest
    {
    /**
    * Run MapTest.testMultithreadedMap against non caching CQC
    */
    @Test
    public void testMultithreadedMap()
        {
        BaseContinuousQueryCacheTest.testFunctorAlwaysCache(
            new BaseContinuousQueryCacheTest.Functor()
                {
                public void execute(Object map)
                    {
                    MapTest.testMultithreadedMap((Map) map);
                    }
                }, false);
        }

    /**
    * Run MapTest.testMultithreadedMap against non caching CQC
    */
    @Test
    public void testMultithreadedMap_caching()
        {
        BaseContinuousQueryCacheTest.testFunctorAlwaysCache(
            new BaseContinuousQueryCacheTest.Functor()
                {
                public void execute(Object map)
                    {
                    MapTest.testMultithreadedMap((Map) map);
                    }
                }, true);
        }
    }
