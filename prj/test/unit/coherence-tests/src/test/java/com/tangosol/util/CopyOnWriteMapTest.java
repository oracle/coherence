/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util;


import com.oracle.coherence.common.base.Blocking;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;


/**
 * @author pp  2011.09.17
 */
public class CopyOnWriteMapTest
    {
    /**
     * Assert basic functionality (put/get/size).
     */
    @Test
    public void basic()
        {
        Map<Integer, String> map = new CopyOnWriteMap<Integer, String>(WeakHashMap.class);

        map.put(1, "one");
        map.put(2, "two");
        map.put(3, "three");

        assertEquals(3, map.size());
        assertEquals("one", map.get(1));
        }

    /**
     * Assert that modification of an iterator fails.
     */
    @Test
    public void iteratorModification()
        {
        Map<Integer, String> map = new CopyOnWriteMap<Integer, String>(WeakHashMap.class);

        map.put(1, "one");
        map.put(2, "two");
        map.put(3, "three");
        map.put(4, "four");

        Iterator iter = map.keySet().iterator();
        while (iter.hasNext())
            {
            if (((Integer) iter.next()).intValue() == 1)
                {
                iter.remove();
                }
            }

        assertEquals(3, map.size());

        Set set = new HashSet();
        set.add(2);
        set.add(3);

        map.keySet().removeAll(set);
        assertEquals(1, map.size());
        }

    /**
     * Simple multi-threaded access test.
     */
    @Test
    public void multithreadedWeakHashMapAccess()
            throws InterruptedException, ExecutionException
        {
        final Map<Object, Integer> map = new CopyOnWriteMap<Object, Integer>(WeakHashMap.class);

        // array of keys used for testing
        final Object[] oKeys = new Object[100];
        for (int i = 0; i < oKeys.length; i++)
            {
            oKeys[i] = new Object();
            }

        ExecutorService executor = Executors.newFixedThreadPool(20);

        // this callable will mutate the map and return an exception if one is caught
        Callable<RuntimeException> mapModifyCaller = new Callable<RuntimeException>()
            {
            @Override
            public RuntimeException call()
                    throws Exception
                {
                for (int i = 0; i < oKeys.length; i++)
                    {
                    try
                        {
                        Object oKey = oKeys[i];
                        if (oKey != null)
                            {
                            map.put(oKey, i);
                            assertEquals((Integer) i, map.get(oKey));
                            map.size();
                            }
                        Blocking.sleep(Base.getRandom().nextInt(10));
                        }
                    catch (RuntimeException e)
                        {
                        return e;
                        }
                    }
                return null;
                }
            };

        // this callable will remove some of the keys and force a GC
        Callable<RuntimeException> keyRemovalCaller = new Callable<RuntimeException>()
            {
            @Override
            public RuntimeException call()
                    throws Exception
                {
                for (int i = 0; i < 10; i++)
                    {
                    oKeys[i * 10] = null;
                    }
                Runtime.getRuntime().gc();
                return null;
                }
            };

        Collection<Callable<RuntimeException>> list = new ArrayList<Callable<RuntimeException>>();

        for (int i = 0; i < 50; i++)
            {
            list.add(mapModifyCaller);
		    }

        list.add(keyRemovalCaller);

        for (int i = 0; i < 50; i++)
            {
            list.add(mapModifyCaller);
		    }

        // rethrow any exceptions that were caught
        List<Future<RuntimeException>> results = executor.invokeAll(list);
        for (Future<RuntimeException> future : results)
            {
            RuntimeException e = future.get();
            if (e != null)
                {
                throw e;
                }
            }
        executor.shutdown();

        Runtime.getRuntime().gc();

        // the map will usually contain 90 entries but sometimes a few
        // entries will not have been collected; make sure the map
        // is never larger than expected (this could result from concurrent
        // modification of the internal map data structure)
        assertTrue(map.size() >= 90 && map.size() < 100);
        }
    }
