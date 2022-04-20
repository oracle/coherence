/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util;

import com.oracle.coherence.common.base.Blocking;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Unit test of {@link UUID}.
 *
 * @author par  2014.07.30
 */
public class UUIDTest
        extends Base
    {
    // ----- unit tests -----------------------------------------------------
    /**
     * COH-11868 - Test UUID construction by multiple threads.  Creates multiple
     * threads which contend to initialize a single UUID instance.
     *
     * The unfixed issue (COH-11868) can be reproduced with this unit test
     * in about 30% of executions of this unit test when 30,000 threads
     * are created, on a Windows machine.  (Linux could only create about
     * 750 threads, so can't be used to reproduce this issue).
     */
    @Test
    public void testUUIDConflict()
        {
        Runner[] aThread = new Runner[NUM_THREADS];
        UUID     u       = new UUID();
        for (int i = 0; i < NUM_THREADS; ++i)
            {
            aThread[i] = new UUIDRunner(u);
            aThread[i].setName("UUIDRunner(" + i + ")");
            }

        try (TestCase test = new TestCase("testUUIDConflict", aThread))
            {
            test.run();

            assertTrue(stringResult(aThread));
            }
        }

    // ----- helper methods -----------------------------------------------------
    /**
     * Verify that the thread didn't collide while
     * initializing UUID.
     */
    private boolean stringResult(Runner[] aThread)
        {
        String[] sResults = new String[NUM_THREADS];
        for (int i = 0; i < aThread.length; i++)
            {
            sResults[i] = ((UUIDRunner) aThread[i]).getString();
            }

        for (int i = 0; i < aThread.length - 1; ++i)
            {
            if (!sResults[i].equals(sResults[i+1]))
                {
                log("UUIDTest; comparison failed: " + i + " vs " + (i + 1));
                return false;
                }
            }
        return true;
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
         * Create a new Runner thread that will perform the test.
         */
        public Runner()
            {
            }

        /**
         * Set the test case.
         *
         * @param test  the test case
         */
        public void setTestCase(TestCase test)
            {
            m_test = test;
            }

        /**
         * Perform the test.
         *
         */
        protected abstract void doTest();

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
                        // wait for all the other threads to start
                        Blocking.wait(m_test);
                        }
                    catch (InterruptedException ie)
                        {
                        Thread.currentThread().interrupt();
                        break;
                        }
                    }

                doTest();
                m_test.testThreadFinished();
                }
            }


        // ----- data members -----------------------------------------------

        /**
         * Test case to be run.
         */
        protected TestCase m_test;
        }


    // ----- UUIDRunner inner class -----------------------------------

    /**
     * Thread that performs toString operation against
     * a UUID object.
     *
     * A Runner thread will perform toString() on
     * the UUID object. Tests whether one thread will overwrite
     * the initialization already performed by a different
     * thread.
     */
    public static class UUIDRunner
            extends Runner
        {

        // ----- Constructors -------------------------------------
        /**
         * Construct UUIDRunner with UUID to act upon.
         *
         * @param id  the UUID on which to act
         */
        public UUIDRunner(UUID id)
            {
            m_id = id;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void doTest()
            {
            UUID id = m_id;
            m_sUUID = id.toString();
            }

        /**
         * Accessor to get the string initialized by the test.
         */
        public String getString()
            {
            return m_sUUID;
            }

        // ----- data members -----------------------------------------------

        /**
         * The UUID to initialize.
         */
        private UUID m_id;

        /**
         * The UUID as string.
         */
        private String m_sUUID;
        }

    // ----- TestCase inner class -------------------------------------------

    /**
     * The TestCase class.
     */
    public static class TestCase
            implements AutoCloseable
        {

        // ----- Constructors -----------------------------------------------

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
         * Run the TestCase on the specified UUID
         */
        public void run()
            {
            Runner[] aThread = m_aThread;

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

        // ----- AutoCloseable interface ------------------------------------

        /**
         * Shutdown the test, and stop all Runner threads.
         */
        @Override
        public void close()
            {
            Thread[] aThread = m_aThread;
            for (int i = 0; i < aThread.length; i++)
                {
                aThread[i].interrupt();
                }
            for (int i = 0; i < aThread.length; i++)
                {
                try
                    {
                    aThread[i].join();
                    }
                catch (InterruptedException e)
                    {
                    // do nothing
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
     * The number of concurrent test threads.
     */
    public static final int NUM_THREADS = 5;
    }