/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.testing.cache;

import com.tangosol.net.NamedCache;
import com.oracle.coherence.testing.util.BaseMapTest;
import com.tangosol.net.cache.ContinuousQueryCache;
import com.tangosol.net.cache.WrapperNamedCache;
import com.tangosol.util.Filter;
import com.tangosol.util.SafeHashMap;
import com.tangosol.util.filter.AlwaysFilter;

/**
 * A base class for ContinuousQueryCache tests.
 *
 * @author jk 2015.04.09
 */
public class BaseContinuousQueryCacheTest
    {
    /**
    * Get the specified cache, always creating a new copy.
    *
    * @param sName  the cache name to create
    *
    * @return a new cache instance
    */
    public static NamedCache getNewCache(String sName)
        {
        return new WrapperNamedCache(new SafeHashMap(), sName);
        }

    /**
    * Simple Functor class for use in tests.
    */
    public static abstract class Functor
        {
        abstract public void execute(Object obj);
        }

    /**
    * Run the functor test
    */
    public static void testFunctorAlwaysCache(Functor functor, boolean cacheValues)
        {
        NamedCache cacheBase = getNewCache("cqc-test");
        Filter filter    = new AlwaysFilter();
        NamedCache cacheCQC  = new ContinuousQueryCache(cacheBase,
                                                        filter, cacheValues /*cache values*/);
        BaseMapTest.assertIdenticalMaps(cacheBase, cacheCQC);

        functor.execute(cacheCQC);

        BaseMapTest.assertIdenticalMaps(cacheBase, cacheCQC);
        }

    }
