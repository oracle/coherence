/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


import java.util.Map;

import com.oracle.coherence.common.base.Blocking;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Test;


/**
* Unit test of {@link WrapperConcurrentMap}.
*
* @author jh  2007.04.27
*/
public class WrapperConcurrentMapTest
        extends Base
    {
   // ----- unit tests -----------------------------------------------------

    private static TestCase[] setupTests()
        {
        return new TestCase[]
            {
            setupConcurrentLockUnlockSame(),
            setupConcurrentLockUnlockDifferent(),
            setupConcurrentAllSame(),
            setupConcurrentAllDifferent(),
            };
        }

    @After
    public void cleanup()
        {
        for (int i = 0; i < m_aTestCases.length; i++)
            {
            TestCase testCase = m_aTestCases[i];
            testCase.shutdown();
            }

        sleep(100);
        }

    /**
    * Test SegmentedConcurrentMap implementation.
    */
    @Test
    public void testSegmentedConcurrentMap()
        {
        m_aTestCases = setupTests();
        testSegmentedConcurrentMap(m_aTestCases);
        }

    /**
    * Test SegmentedConcurrentMap implementation against the specified tests.
    */
    public static void testSegmentedConcurrentMap(TestCase[] aTest)
        {
        try
            {
            for (int i = 0; i < aTest.length; i++)
                {
                aTest[i].run(new SegmentedConcurrentMap());
                }
            }
        catch (Throwable t)
            {
            throw ensureRuntimeException(t);
            }
        }

    /**
    * Test WrapperConcurrentMap implementation.
    */
    @Test
    public void testWrapperConcurrentMap()
        {
        m_aTestCases = setupTests();
        testWrapperConcurrentMap(m_aTestCases);
        }

    /**
    * Test WrapperConcurrentMap implementation against the specified tests.
    */
    public static void testWrapperConcurrentMap(TestCase[] aTest)
        {
        try
            {
            for (int i = 0; i < aTest.length; i++)
                {
                aTest[i].run(new WrapperConcurrentMap(new SafeHashMap()));
                }
            }
        catch (Throwable t)
            {
            throw ensureRuntimeException(t);
            }
        }

    /**
    * Test concurrent lock() and unlock() operations against the same key.
    */
    public static TestCase setupConcurrentLockUnlockSame()
        {
        Runner[] aThread = new Runner[NUM_THREADS];
        for (int i = 0; i < NUM_THREADS; ++i)
            {
            aThread[i] = new LockUnlockRunner("key", NUM_ITERATIONS);
            aThread[i].setName("LockUnlockRunnerSame(" + i + ")");
            }

        return new TestCase("testConcurrentLockUnlockSame", aThread);
        }

    /**
    * Test concurrent lock() and unlock() operations against different keys.
    */
    public static TestCase setupConcurrentLockUnlockDifferent()
        {
        Runner[] aThread = new Runner[NUM_THREADS];
        for (int i = 0; i < NUM_THREADS; ++i)
            {
            aThread[i] = new LockUnlockRunner("key"+(i%10), NUM_ITERATIONS);
            aThread[i].setName("LockUnlockRunnerDifferent(" + i + ")");
            }

        return new TestCase("testConcurrentLockUnlockDifferent", aThread);
        }

    /**
    * Test concurrent lock() unlock(), put() and remove() operations
    * against the same key.
    */
    public static TestCase setupConcurrentAllSame()
        {
        Runner[] aThread = new Runner[NUM_THREADS];
        for (int i = 0; i < NUM_THREADS; ++i)
            {
            aThread[i] = new AllRunner("key", NUM_ITERATIONS);
            aThread[i].setName("AllRunnerSame(" + i + ")");
            }

        return new TestCase("testConcurrentAllSame", aThread);
        }

    /**
    * Test concurrent lock(), unlock(), put() and remove() operations
    * against different keys.
    */
    public static TestCase setupConcurrentAllDifferent()
        {
        Runner[] aThread = new Runner[NUM_THREADS];
        for (int i = 0; i < NUM_THREADS; ++i)
            {
            aThread[i] = new AllRunner("key"+(i%10), NUM_ITERATIONS);
            aThread[i].setName("AllRunnerDifferent(" + i + ")");
            }

        return new TestCase("testConcurrentAllDifferent", aThread);
        }

    /**
    * Return the name of the specified map.
    */
    private static String mapTestName(Map map)
        {
        if (map instanceof WrapperConcurrentMap)
            {
            WrapperConcurrentMap mapWrapper = (WrapperConcurrentMap) map;
            return "WrapperConcurrentMap(" +
                mapWrapper.getMap().getClass().getName() + ")";
            }
        return map.getClass().getName();
        }


    // ----- Runner inner class ---------------------------------------------

    /**
    * Base TestCase runner thread.
    */
    public static abstract class Runner
            extends Thread
        {
        // ----- constructors ---------------------------------------------

        /**
        * Create a new Runner thread that will perform the specified
        * number of test iterations.
        *
        * @param cIteration  the number of test iterations to perform
        */
        public Runner(int cIteration)
            {
            m_cIteration = cIteration;
            }

        /**
        * Set the ConcurrentMap to be tested.
        *
        * @param map  the ConcurrentMap to be tested
        */
        public void setMap(ConcurrentMap map)
            {
            m_map = map;
            }

        /**
        * Set the TestCase instance to run on this Thread.
        *
        * @param test  the TestCase instance to run on this Thread
        */
        public void setTestCase(TestCase test)
            {
            m_test = test;
            }

        /**
        * Artificially spin some CPU time.  This is here to provide a
        * (more?) realisitic approximation of actual lock()/unlock()
        * client workload.
        */
        public int spinTime(int i)
            {
            if (i == 0 || i == 1)
                {
                return 1;
                }

            return spinTime(i - 1) + spinTime(i - 2);
            }

        /**
        * Perform a single test iteration.
        *
        * @param nIter  the iteration number
        */
        protected abstract void doIteration(int nIter);


        // ----- Thread methods -------------------------------------------

        /**
        * {@inheritDoc}
        */
        @Override
        public void run()
            {
            boolean fFirst = true;
            while (true)
                {
                synchronized (m_test)
                    {
                    if (fFirst)
                        {
                        // signal that this test thread is "ready" to be notified
                        m_test.m_atomicThreads.incrementAndGet();
                        fFirst = false;
                        }
                    try
                        {
                        /* wait for all the other threads to start */
                        Blocking.wait(m_test);
                        }
                    catch (InterruptedException ie)
                        {
                        Thread.currentThread().interrupt();
                        break;
                        }
                    }
                try
                    {
                    for (int i = 0, c = m_cIteration; i < c; ++i)
                        {
                        doIteration(i);
                        }
                    }
                catch(OutOfMemoryError oom)
                    {
                    oom.printStackTrace();
                    while (true)
                        {
                        try
                            {
                            synchronized (this)
                                {
                                Blocking.wait(this);
                                }
                            }
                        catch(InterruptedException ie)
                            {
                            }
                        }
                    }

                m_test.testThreadFinished();
                }
            }


        // ----- data members -----------------------------------------------

        /**
        * The TestCase being run by this thread.
        */
        protected TestCase m_test;

        /**
        * The target ConcurrentMap.
        */
        protected ConcurrentMap m_map;

        /**
        * The number of iterations.
        */
        protected int m_cIteration;
        }


    // ----- LockUnlockRunner inner class -----------------------------------

    /**
    * Thread that performs successive lock() and unlock() operations against
    * a specified ConcurrentMap.
    */
    public static class LockUnlockRunner
            extends Runner
        {

        /**
        * Create a new LockUnlockRunner thread that will perform successive
        * lock() and unlock() operations on the specified key against the
        * ConcurrentMap being tested.
        *
        * @param oKey        the key to peform the operations against
        * @param cIteration  the number of operations to test
        */
        public LockUnlockRunner(Object oKey, int cIteration)
            {
            super(cIteration);
            m_oKey = oKey;
            }

        /**
        * {@inheritDoc}
        */
        @Override
        protected void doIteration(int nIter)
            {
            ConcurrentMap map  = m_map;
            Object        oKey = m_oKey;

            map.lock(oKey, -1);
            spinTime(15);
            map.unlock(oKey);
            }

        // ----- data members -----------------------------------------------

        /**
        * The key to lock() and unlock().
        */
        private Object m_oKey;
        }


    // ----- AllRunner inner class ------------------------------------------

    /**
    * Thread that performs successive put(), remove(), lock(), and unlock()
    * operations against a specified ConcurrentMap.
    */
    public static class AllRunner
            extends Runner
        {

        /**
        * Create a new AllRunner thread that will perform successive put(),
        * remove(), lock(), and unlock() operations on the specified key
        * against the ConcurrentMap being tested.
        *
        * @param oKey        the key to peform the operations against
        * @param cIteration  the number of operations to test
        */
        public AllRunner(Object oKey, int cIteration)
            {
            super(cIteration);
            m_oKey = oKey;
            }

        /**
        * {@inheritDoc}
        */
        @Override
        protected void doIteration(int nIter)
            {
            ConcurrentMap map  = m_map;
            Object        oKey = m_oKey;

            switch (nIter % 4)
                {
                case 0:
                case 5:
                    {
                    map.lock(oKey, -1);
                    }
                    break;
                case 1:
                case 4:
                    {
                    map.put(oKey, "goat");
                    }
                    break;
                case 2:
                case 7:
                    {
                    map.remove(oKey);
                    }
                    break;
                case 3:
                case 6:
                    {
                    map.unlock(oKey);
                    }
                    break;
                }
            }

        // ----- data members -----------------------------------------------

        /**
        * The key to lock(), unlock(), and put().
        */
        private Object m_oKey;
        }


    // ----- TestCase inner class -------------------------------------------

    /**
    * The TestCase class.
    */
    public static class TestCase
        {
        /**
        * Construct a new TestCase with the specified name to be run by the
        * specified array of Runner threads.
        *
        * @param sTestName  the name of the TestCase
        * @param aThread    the array of Runner threads
        */
        public TestCase(String sTestName, Runner[] aThread)
            {
            m_aThread   = aThread;
            m_sTestName = sTestName;

            for (int i = 0; i < aThread.length; i++)
                {
                aThread[i].setTestCase(this);
                aThread[i].start();
                }
            }

        /**
        * Run the TestCase on the specified ConcurrentMap
        *
        * @param map  the ConcurrentMap to test
        */
        public void run(ConcurrentMap map)
            {
            Runner[] aThread = m_aThread;
            /* set the map being tested */
            for (int i = 0; i < aThread.length; i++)
                {
                Runner runner = aThread[i];
                runner.setMap(map);
                }

            out("Running " + m_sTestName + " on " + mapTestName(map));
            long ldtStart = Base.getSafeTimeMillis();

            while (m_atomicThreads.get() < aThread.length)
                {
                // COH-9751: wait for all runner threads to start so we don't miss
                //           a notification
                Base.sleep(100L);
                }

            // ready... set...  go!
            synchronized (this)
                {
                notifyAll();

                try
                    {
                    /* wait for all the test threads to finish */
                    Blocking.wait(this);
                    }
                catch (InterruptedException ie)
                    {
                    Thread.currentThread().interrupt();
                    throw Base.ensureRuntimeException(ie);
                    }
                }
            long ldtEnd = Base.getSafeTimeMillis();
            out("Elapsed time: " + (ldtEnd - ldtStart) + " ms");
            }

        /**
        * Shutdown the test, and stop all Runner threads.
        */
        public void shutdown()
            {
            Thread[] aThread = m_aThread;
            for (int i = 0; i < aThread.length; i++)
                {
                aThread[i].interrupt();
                }
            }

        /**
        * Used by the Runner thread to notify the TestCase that it has
        * finished a TestCase execution.
        */
        private void testThreadFinished()
            {
            if (m_atomicThreads.decrementAndGet() == 0)
                {
                /* last thread is finishing */
                synchronized (this)
                    {
                    notify();
                    }
                }
            }

        // ----- data members -----------------------------------------------

        /**
        * The counter of active test threads.
        */
        private AtomicInteger m_atomicThreads = new AtomicInteger(0);

        /**
        * The array of test threads.
        */
        private Runner[]      m_aThread;

        /**
        * The name of this test.
        */
        private String        m_sTestName;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The number of iterations to run each test.
    */
    public static final int NUM_ITERATIONS = 10000;

    /**
    * The number of concurrent test threads.
    */
    public static final int NUM_THREADS    = 50;

    /**
     * Array of test cases.
     */
    private TestCase[] m_aTestCases;
    }