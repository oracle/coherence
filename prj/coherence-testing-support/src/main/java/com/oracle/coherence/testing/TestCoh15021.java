/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.testing;

import com.oracle.coherence.common.base.Timeout;

import com.tangosol.net.NamedCache;

import com.tangosol.net.cache.LocalCache;
import com.tangosol.net.cache.NearCache;

import com.tangosol.util.Base;
import com.tangosol.util.WrapperException;
import com.tangosol.util.processor.PreloadRequest;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertTrue;

/**
 * Common test code for COH-15021
 *
 * @author phf 2017.03.28
 */
public class TestCoh15021 extends Base
    {

    /**
     * Regression test for COH-15021 (courtesy of Tim Middleton).
     *
     * @param cacheBack  back cache
     */
    public static void testCoh15021(NamedCache cacheBack)
        {
        NearCache     cache1         = new NearCache(new LocalCache(1000), cacheBack);
        NearCache     cache2         = new NearCache(new LocalCache(1000), cacheBack);
        AtomicBoolean atomicContinue = new AtomicBoolean(true);

        Thread thread1 = new Thread(new LoadTest(cache1), "LoadThread1");
        Thread thread2 = new Thread(new LoadTest(cache2), "LoadThread2");
        Thread thread3 = new Thread(() ->
        {
        // the effect if COH-15021 is that the service thread gets blocked
        // for 5 seconds (see CachingMap#validate);
        // let's check that we are never paused for much more than that duration
        try
            {
            while (atomicContinue.get())
                {
                try (Timeout t = Timeout.after(10, TimeUnit.SECONDS))
                    {
                    cacheBack.invoke("test", PreloadRequest.INSTANCE);
                    }
                catch (Exception e)
                    {
                    atomicContinue.set(false);

                    // stop the service and the load threads
                    cacheBack.getCacheService().stop();

                    thread1.interrupt();
                    thread2.interrupt();
                    break;
                    }
                sleep(100);
                System.out.print("."); System.out.flush();
                }
            }
        catch (WrapperException e)
            {
            if (e.getOriginalException() instanceof InterruptedException)
                {
                // expected
                }
            else
                {
                throw e;
                }
            }
        }, "CheckThread");

        thread1.start();
        thread2.start();
        thread3.start();

        try
            {
            thread1.join();
            thread2.join();

            assertTrue("Test failed", atomicContinue.get());

            atomicContinue.set(false);
            thread3.join();
            }
        catch (InterruptedException e) {}
        }

    private static class LoadTest
        implements Runnable
        {
        public LoadTest(NamedCache cache)
            {
            f_cache = cache;
            }

        public void run()
            {
            int cIters = 1000;
            int cBatch = 500;
            int nMax = 250000;

            for (int i = 1; i <= cIters; i++)
                {
                Map mapEntries = generateRandomEntries(cBatch, nMax);
                f_cache.putAll(mapEntries);

                // random gets
                Set setKeys = generateRandomKeys(cBatch, nMax);
                f_cache.getAll(setKeys);
                }
            }

        private static Map generateRandomEntries(int cItems, int nMax)
            {
            Map map = new HashMap<>();
            Random rand = getRandom();

            for (int i = 1; i <= cItems; i++)
                {
                map.put("key" + rand.nextInt(nMax) + 1, "data");
                }

            return map;
            }

        private static Set generateRandomKeys(int cItems, int nMax)
            {
            Set set = new HashSet<>();
            Random rand = getRandom();

            for (int i = 1; i <= cItems; i++)
                {
                set.add("key" + rand.nextInt(nMax) + 1);
                }

            return set;
            }

        final private NamedCache f_cache;
        }
    }
