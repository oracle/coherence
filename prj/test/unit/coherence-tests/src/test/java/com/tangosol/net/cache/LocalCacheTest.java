/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.cache;


import com.tangosol.util.Base;
import com.tangosol.util.ClassHelper;
import com.tangosol.util.Daemon;

import com.oracle.coherence.common.base.Blocking;
import org.junit.Ignore;
import org.junit.Test;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;

import java.util.Map;


/**
* A test showing differences between various options between 3.4 (baseline)
* and 3.5 implementations.
*
* @author cp  2009.01.15
*/
public class LocalCacheTest
        extends Base
    {
    static int ITERS   = 5;
    static int MAX     = 100000;
    static int THREADS = 2;

    static String[] POLICIES = new String[] {"Hybrid", "LRU", "LFU"};
    static int   [] EXPIRIES = new int[] {0, 1000, 60000};
    static int   [] PRUNELOW = new int[] {80};

    /**
    * Default constructor.
    */
    public LocalCacheTest()
        {
        super();
        }

    /**
    * Command line execution.
    *
    * @param asArg an array of command line arguments
    */
    public static void main(String[] asArg)
            throws Throwable
        {
        LocalCacheTest test = new LocalCacheTest();
        // test.simpleTest();

        test.ensureKeys(MAX * 2);

        for (int iIter = 0; iIter < ITERS; ++iIter)
            {
            out("iteration: " + iIter);
            test.test();
            }
        }

    @Ignore
    @Test
    public void simpleTest()
            throws Throwable
        {
        LocalCache cache = new LocalCache(10, 0);
        for (int iIter = 0; iIter < 20; ++iIter)
            {
            for (int i = 0; i < 15; ++i)
                {
                cache.put(""+getRandom().nextInt(20), null);
                sleep(1);
                }
            out(cache.getCacheStatistics());
            cache.getCacheStatistics().resetHitStatistics();
            }
        }

    @Ignore
    @Test
    public void test()
            throws Throwable
        {
        for (int iPolicy = 0; iPolicy <= 2; ++iPolicy)
            {
            String sPolicy = POLICIES [iPolicy];

            for (int iExpiry = 0; iExpiry < EXPIRIES.length; ++iExpiry)
                {
                int cMillis = EXPIRIES[iExpiry];

                for (int iPrune = 0; iPrune < PRUNELOW.length; ++iPrune)
                    {
                    boolean fOverrideLowUnits = PRUNELOW[iPrune] != 0;
                    double  dflPruneFactor    = ((double) PRUNELOW[iPrune]) / 100;

                    // old cache, new cache, old local, new local
                    for (int iClass = 0; iClass <= 3; ++iClass)
                        {
                        Class  clz;
                        String sClass;
                        switch (iClass)
                            {
                            case 0:
                                clz    = OldOldCache.class;
                                sClass = "OldCache v1";
                                break;
                            case 1:
                                clz    = OldCache.class;
                                sClass = "OldCache v2";
                                break;
                            case 2:
                                clz    = OldLocalCache.class;
                                sClass = "LocalCache v1";
                                break;
                            case 3:
                                clz    = LocalCache.class;
                                sClass = "LocalCache v2";
                                break;
                            default:
                                throw new IllegalStateException();
                            }

                        for (int iIncr = 0; iIncr <= (iClass & 1); ++iIncr)
                            {
                            boolean fIncr = (iClass & 1) != 0;
                            String sIncr = fIncr ? ", incremental evict=" + new String[] {"off", "on"} [iIncr] : "";

                            String sDesc = sClass + ", " + sPolicy + ", Expiry=" + cMillis + "ms, low=" + (fOverrideLowUnits ? ("" + PRUNELOW[iPrune] + "%") : "default") + sIncr + ", threads=" + THREADS;

                            Map cache = (Map) clz.newInstance();

                            ClassHelper.invoke(cache, "setExpiryDelay" , new Object[] {cMillis});
                            ClassHelper.invoke(cache, "setHighUnits"   , new Object[] {MAX});
                            ClassHelper.invoke(cache, "setEvictionType", new Object[] {iPolicy});
                            if (fOverrideLowUnits)
                                {
                                ClassHelper.invoke(cache, "setLowUnits", new Object[] {(int) (dflPruneFactor * MAX)});
                                }
                            if (fIncr)
                                {
                                ClassHelper.invoke(cache, "setIncrementalEviction", new Object[] {iIncr == 1});
                                }

                            TestPlan planPut = new TestPlan(cache, THREADS, 5, MAX * 2, 0);
                            planPut.execute();

                            TestPlan planGet = new TestPlan(cache, THREADS, 25, MAX * 2, 1);
                            planGet.execute();

                            out("  plan: " + sDesc);
                            out("    " + planPut);
                            out("    " + planGet);
                            }
                        }
                    }
                }
            }
        }

    public static class TestPlan
            extends Base
            implements Runnable
        {
        public TestPlan(Map cache, int cThreads, int cIters, int cKeys, int nTest)
            {
            m_cache    = cache;
            m_cThreads = cThreads;
            m_cIters   = cIters;
            m_cKeys    = cKeys;
            m_nTest    = nTest;
            m_asKeys   = (String[]) randomize((String[]) KEYS.clone());
            }

        public synchronized void execute()
                throws Throwable
            {
            if (m_cThreads == 1)
                {
                m_statsStart = new MemoryStats();
                m_dtStart = System.currentTimeMillis();
                run();
                m_statsStop = new MemoryStats();
                m_dtStop    = System.currentTimeMillis();
                }
            else
                {
                // create the threads
                int      cThreads = m_cThreads;
                Daemon[] athread  = new Daemon[cThreads];
                for (int i = 0; i < cThreads; ++i)
                    {
                    athread[i] = new Daemon("test-" + i)
                        {
                        public void run()
                            {
                            TestPlan.this.run();
                            }
                        };
                    athread[i].start();
                    }

                // wait for them to complete
                m_statsStart = new MemoryStats();
                m_dtStart = System.currentTimeMillis();
                while (m_cStopped < m_cThreads)
                    {
                    try
                        {
                        Blocking.wait(this);
                        }
                    catch (Throwable e)
                        {
                        throw ensureRuntimeException(e);
                        }
                    }
                m_statsStop = new MemoryStats();
                m_dtStop = System.currentTimeMillis();
                }

            CacheStatistics stats = (CacheStatistics)
                    ClassHelper.invoke(m_cache, "getCacheStatistics", new Object[0]);
            m_sStats = stats.toString();
            stats.resetHitStatistics();
            }

        public void run()
            {
            try
                {
                synchronized (this)
                    {
                    // really this sync block is just to wait for the monitor so
                    // that all the threads start about the same time
                    m_cStarted += 1;
                    }

                switch (m_nTest)
                    {
                    case 0:
                        testPut(m_cache, m_cIters, m_cKeys, m_asKeys);
                        break;

                    case 1:
                        testGet(m_cache, m_cIters, m_cKeys, m_asKeys);
                        break;

                    default:
                        throw new IllegalStateException();
                    }

                synchronized (this)
                    {
                    m_cStopped += 1;
                    notifyAll();
                    }
                }
            catch (RuntimeException e)
                {
                out("caught exception in test on thread " + Thread.currentThread().getName() + ":");
                e.printStackTrace();
                }
            }

        public String toString()
            {
            long cMillisTest = m_dtStop - m_dtStart;
            long cMillisGC   = m_statsStop.getCollectionTime() - m_statsStart.getCollectionTime();
            return TESTS[m_nTest] + ": " + (cMillisTest - cMillisGC) + "ms (plus " + cMillisGC +"ms GC), stats=" + m_sStats;
            }

        int m_nTest;

        int m_cThreads;
        int m_cStarted;
        int m_cStopped;

        MemoryStats m_statsStart;
        MemoryStats m_statsStop;

        Map m_cache;
        int m_cIters;
        int m_cKeys;
        String[] m_asKeys;

        long m_dtStart;
        long m_dtStop;
        String m_sStats;

        static String[] TESTS = {"Puts", "Gets"};
        }

    static void testGet(Map cache, int cIters, int cKeys, String[] asKey)
        {
        for (int iIter = 0; iIter < cIters; ++iIter)
            {
            for (int i = 0; i < cKeys; ++i)
                {
                Object o = asKey[i];
                cache.get(o);
                if (o != null && o != o)
                    {
                    throw new IllegalStateException();
                    }
                }
            }
        }

    static void testPut(Map cache, int cIters, int cKeys, String[] asKey)
        {
        for (int iIter = 0; iIter < cIters; ++iIter)
            {
            for (int i = 0; i < cKeys; ++i)
                {
                Object o = asKey[i];
                cache.put(o, o);
                }
            }
        }

    static void ensureKeys(int c)
        {
        String[] asKey = KEYS;
        if (asKey == null || asKey.length < c)
            {
            KEYS = asKey = new String[c];
            for (int i = 0; i < c; ++i)
                {
                asKey[i] = String.valueOf(i);
                }
            }
        }

    static void sleep(int cSecs)
        {
        try
            {
            Blocking.sleep(cSecs * 1000);
            }
        catch (Throwable e) {}
        }

    static String[] KEYS;

    static class MemoryStats
        {
        public MemoryStats()
            {
            long cGC        = 0L;
            long cMillisGC  = 0L;

            for (GarbageCollectorMXBean mb : ManagementFactory.getGarbageCollectorMXBeans())
                {
                cGC       += mb.getCollectionCount();
                cMillisGC += mb.getCollectionTime();
                }

            m_cGC       = cGC;
            m_cMillisGC = cMillisGC;
            }

        public long getCollectionCount()
            {
            return m_cGC;
            }

        public long getCollectionTime()
            {
            return m_cMillisGC;
            }

        private long m_cGC;
        private long m_cMillisGC;
        }
    }
