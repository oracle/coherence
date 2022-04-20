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
* Unit test of thread {@link Gate} implementations.
*
* @author op  2013.04.20
*/
public class GateTest
        extends Base
    {
    // ----- unit tests -----------------------------------------------------

    private static TestCase[] setupTests()
        {
        return new TestCase[]
            {
            setupEnterExitTest(),
            //setupBarCloseTest()
            };
        }

    /**
    * Test legacy ThreadGate implementation.
    */
    @Test
    public void testThreadGate()
            throws Throwable
        {
        testGate(1);
        }

    /**
    * Test ThreadGateLite implementation.
    */
    @Test
    public void testThreadGateLite()
            throws Throwable
        {
        testGate(2);
    }

    /**
    * Test WrapperReentrantGate implementation.
    */
    @Test
    public void testWrapperReentrantGate()
            throws Throwable
        {
        testGate(3);
        }

    protected void testGate(int iGate)
            throws Throwable
        {
        // lock downgrade is allowed
        singleThreadTest(makeGate(iGate), new GATE_OPS[]
                         {GATE_OPS.CLOSE, GATE_OPS.ENTER},
                         new boolean[]{true, true});
        // lock upgrade: legacy ThreadGate allows it, but TGL and WrapperReentrantGate - do not
        singleThreadTest(makeGate(iGate), new GATE_OPS[]
                         {GATE_OPS.ENTER, GATE_OPS.CLOSE},
                         new boolean[]{true, iGate == 1 ? true : false });
        singleThreadTest(makeGate(iGate), new GATE_OPS[]
                         {GATE_OPS.CLOSE, GATE_OPS.BAR, GATE_OPS.OPEN, GATE_OPS.CLOSED_CUR_THR},
                         new boolean[]{true, true, true, true});
        singleThreadTest(makeGate(iGate), new GATE_OPS[]
                         {GATE_OPS.BAR, GATE_OPS.CLOSE, GATE_OPS.OPEN, GATE_OPS.CLOSED_CUR_THR,
                          GATE_OPS.BAR, GATE_OPS.CLOSE, GATE_OPS.BAR, GATE_OPS.CLOSE,
                          GATE_OPS.OPEN, GATE_OPS.OPEN, GATE_OPS.OPEN, GATE_OPS.OPEN, GATE_OPS.OPEN,
                          GATE_OPS.CLOSED_CUR_THR},
                         new boolean[]{true, true, true, true, true, true, true, true,
                                       true, true, true, true, true, false});
        singleThreadTest(makeGate(iGate), new GATE_OPS[]
                         {GATE_OPS.ENTER, GATE_OPS.BAR, GATE_OPS.ENTER, GATE_OPS.EXIT, GATE_OPS.OPEN,
                          GATE_OPS.CLOSED_CUR_THR},
                         new boolean[]{true, true, true, true, true, false});
        singleThreadTest(makeGate(iGate), new GATE_OPS[]
                         {GATE_OPS.ENTER, GATE_OPS.CLOSE},
                         // TGL does not allow lock promotion, WRG by default is based on TGL
                         new boolean[]{true, iGate == 1 ? true : false});
        singleThreadTest(makeGate(iGate), new GATE_OPS[]
                         {GATE_OPS.ENTER, GATE_OPS.ENTER, GATE_OPS.ENTER, GATE_OPS.EXIT, GATE_OPS.EXIT,
                          GATE_OPS.CLOSE, GATE_OPS.EXIT, GATE_OPS.CLOSE},
                         new boolean[]{true, true, true, true, true, iGate == 1 ? true : false, true, true});
//todo 1. can proceed after barEntry by another thread if already in; 2. dead thread detected
        fourThreadTest(makeGate(iGate), new GATE_OPS[]
                       {GATE_OPS.CLOSE, GATE_OPS.BAR, GATE_OPS.ENTER, GATE_OPS.OPEN},
                       new Boolean[]{true, false, false, null});
        fourThreadTest(makeGate(iGate), new GATE_OPS[]
                       {GATE_OPS.ENTER, GATE_OPS.CLOSE, GATE_OPS.BAR, GATE_OPS.OPEN},
                       new Boolean[]{true, false, true, null});
        fourThreadTest(makeGate(iGate), new GATE_OPS[]
                       {GATE_OPS.BAR, GATE_OPS.CLOSE, GATE_OPS.ENTER, GATE_OPS.EXIT},
                       new Boolean[]{true, false, false, null});
        fourThreadTest(makeGate(iGate), new GATE_OPS[]
                       {GATE_OPS.ENTER, GATE_OPS.BAR, GATE_OPS.ENTER, GATE_OPS.CLOSED},
                       new Boolean[]{true, true, false, false});

        TestCase[] aTestCases = setupTests();
        runTesCase(aTestCases, iGate);

        // clean up the threads
        for (int i = 0; i < aTestCases.length; i++)
            {
            aTestCases[i].cleanup();
            }
        }

    // ---- Helper methods ---------------------------------

    protected static Gate makeGate(int  iGate)
    {
    switch (iGate)
        {
        case 1:
            return new ThreadGate();
        case 2:
            return new ThreadGateLite();
        case 3:
            return new WrapperReentrantGate();
        }
    return null;
    }

    /**
    * Test ThreadGateLite implementation with the specified tests.
    */
    public static void runTesCase(TestCase[] aTest, int iGate)
            throws Throwable
        {
        for (int i = 0; i < aTest.length; i++)
            {
            aTest[i].execute(makeGate(iGate));
            }
        }

    private static TestCase setupEnterExitTest()
        {
        Runner[] aThread = new Runner[NUM_THREADS];
        for (int i = 0; i < NUM_THREADS; ++i)
            {
            aThread[i] = new EnterExitRunner(NUM_ITERATIONS);
            aThread[i].setName("EnterExitRunner(" + i + ")");
            }

        return new TestCase("testEnterExit", aThread);
        }

    private static TestCase setupBarCloseTest()
        {
        Runner[] aThread = new Runner[NUM_THREADS + 2];
        // add special threads
        for (int i = 0; i < 2; ++i)
            {
            aThread[i] = new BarCloseRunner(NUM_ITERATIONS);
            aThread[i].setName(Integer.toString(i));
            }

        // add enter/exit threads to the test
        for (int i = 2; i < NUM_THREADS + 2; ++i)
            {
            aThread[i] = new ReEnterRunner(NUM_ITERATIONS);
            aThread[i].setName("ReEnterRunner(" + i + ")");
            }

        return new TestCase("testBarClose", aThread);
        }

    private void singleThreadTest(Gate gate, GATE_OPS[] aOps, boolean[] afResults)
                throws Throwable
        {
        System.out.println("* * * Single-thread test for " + gate.getClass() + " * * * ");
        int nOpsLen = aOps.length;
        for (int i = 0; i < nOpsLen; i++)
            {
            doOperation(gate, aOps[i], afResults[i], false);
            }
        }

    private void fourThreadTest(Gate gate, GATE_OPS[] aOps, Boolean[] afResults)
            throws Throwable
        {
        assertEquals(4, aOps.length);

        System.out.println("* * * Four-thread test for " + gate.getClass()
               + " * * * \n* * * afResults=" + afResults[0]+", "+afResults[1]+", "
               +afResults[2]+", " + ""+afResults[3]);
        final Object oMon = new Object();

        OrderedRunner first = new OrderedRunner(aOps[0], 1, afResults[0] == null ? false : afResults[0],
                                            oMon, afResults[0] == null ? true : false);
        first.setName("First Runner-" + aOps[0]);
        first.setMaxRun(3000);
        first.setStart(System.currentTimeMillis());

        OrderedRunner second = new OrderedRunner(aOps[1], 2, afResults[1] == null ? false : afResults[1],
                                               oMon, afResults[1] == null ? true : false);
        second.setName("Second Runner-" + aOps[1]);
        second.setMaxRun(4000);
        second.setStart(System.currentTimeMillis());

        OrderedRunner third = new OrderedRunner(aOps[2], 3,  afResults[2] == null ? false : afResults[2],
                                oMon, afResults[2] == null ? true : false);
        third.setName("Third Runner-" + aOps[2]);
        third.setMaxRun(5000);
        third.setStart(System.currentTimeMillis());

        OrderedRunner fourth = new OrderedRunner(aOps[3], 4,  afResults[3] == null ? false : afResults[3],
                                        oMon, afResults[3] == null ? true : false);
        fourth.setName("Fourth Runner-" + aOps[3]);
        fourth.setMaxRun(6000);
        fourth.setStart(System.currentTimeMillis());

        Runner[] aThread = new Runner[]{first, second, third, fourth};
        TestCase test = new TestCase("Four-thread Test: " + aOps[0] + "-" + aOps[1]
                                             + "-" + aOps[2] + "-" + aOps[3], aThread);
        first.setTestCase(test);
        second.setTestCase(test);
        third.setTestCase(test);
        fourth.setTestCase(test);
        test.execute(gate);
        }

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
        * Run the TestCase on the specified thread gate
        *
        * @param gate  the Gate to test
        */
        public void execute(Gate gate)
            throws Throwable
            {
            Runner[] aThread = m_aThread;

            // init all threads with the gate
            for (int i = 0; i < aThread.length; i++)
                {
                aThread[i].setGate(gate);
                }

            System.out.println("==== Running " + m_sTestName + " for " + gate.getClass().getName()
                      + " in " + m_aThread.length + " threads with "
                      + aThread[m_aThread.length - 1].getIterations() + " " + "iterations ====");

            while(m_atomicThreads.get() < m_aThread.length)
                {
                sleep(100);
                }

            synchronized (this)
                {
                // wake up test threads and run the test
                notifyAll();

                try
                    {
                    // wait for all the test threads to finish
                    Blocking.wait(this);
                    }
                catch (InterruptedException ie)
                    {
                    System.out.println(m_sTestName + ".execute: InterruptedException: "
                    + "currentThread().interrupt(); rethrow ex");
                    Thread.currentThread().interrupt();
                    throw ensureRuntimeException(ie);
                    }
                }
            // propagate exception from runner thread to main
            if (m_ex != null)
                {
                throw new RuntimeException(m_ex);
                }
            }

        /**
        * Used by the Runner thread to notify the TestCase that it has
        * finished a TestCase execution.
        *
        * @param t      a possible exception thrown by the Runner
        */
        private void threadFinished(Throwable t)
                throws InterruptedException
            {
            synchronized (this)
                {
                if (t != null)
                    {
                    // if one of the threads produced an exception, terminate test case
                    m_ex = t;
                    notifyAll();
                    }
                else if (m_atomicThreads.decrementAndGet() == 0)
                    {
                    // last thread - signal end of test case
                    notifyAll();
                    }
                else
                    {
                    // wait for TestCase to end all threads, see #execute
                    Blocking.wait(this);
                    }
                }
            }

        /**
         * Clean up threads used by TestCase.
         */
        public void cleanup()
            {
            for (int i = 0; i < m_aThread.length; i++)
                {
                Runner runner = m_aThread[i];
                if (runner.isAlive())
                    {
                    runner.interrupt();
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
        private Runner[] m_aThread;

        /**
        * The name of this test.
        */
        private String m_sTestName;

        /**
        * The counter of threads currently in the gate (entered and not exited)
        */
        private AtomicInteger m_atomicEntered = new AtomicInteger(0);

        /**
        * The number of threads inside the gate when entry was barred
        */
        private volatile boolean m_fBarred;

        /**
        * Current status of the gate being tested
        */
        private volatile boolean m_fClosed;

        /**
        * Used to delay the start of the first BarCloseRunner
        * until ReEnterThreads have progressed enough
        */
        private volatile boolean[] m_afCloseWait = new boolean[] {true};

        /**
        * An exception or error thrown by one of the threads,
        * which needs to be saved and re-thrown by junit thread
        */
        protected Throwable m_ex;

        }

    // ----- Runner inner class ---------------------------------------------

    /**
    * Basic TestCase runner thread.
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
        * Get the number of test iterations this thread will perform
        */
        public int getIterations()
            {
            return m_cIteration;
            }

        /**
        * Set the thread gate to be tested.
        *
        * @param g  the Gate to be tested
        */
        public void setGate(Gate g)
            {
            m_gate = g;
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
        * Get time in milliseconds when the thread was started
        */
        public long getStart()
            {
            return m_ldtStart;
            }

        public void setStart(long start)
            {
            m_ldtStart = start;
            }

        /**
        * Get maximum time in milliseconds the thread is allowed to run
        */
        public long getMaxRun()
            {
            return m_ldtMaxRun;
            }

        /**
        * Set maximum time in milliseconds the thread is allowed to run
        */
        public void setMaxRun(long maxRun)
            {
            m_ldtMaxRun = maxRun;
            }

        /**
        * Artificially spin some CPU time to provide an approximation
        * of the actual thread gate workload.
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
        protected abstract void doIteration(int nIter) throws Throwable;

        // ----- Thread methods -------------------------------------------

        /**
        * {@inheritDoc}
        */
        public void run()
            {
            synchronized (m_test)
                {
                try
                    {
                    // wait for all the other threads to start
                    m_test.m_atomicThreads.incrementAndGet();
                    Blocking.wait(m_test);
                    }
                catch (InterruptedException ie)
                    {
                    Thread.currentThread().interrupt();
                    return;
                    }
                }
            System.out.println("   started " + getName());

            try
                {
                try
                    {
                    for (int i = 0, c = m_cIteration; i < c; ++i)
                        {
                        doIteration(i);
                        }
                    }
                catch (Throwable t)
                    {
                    System.out.println("==> finished " + getName() + " with EXCEPTION!");
                    m_test.threadFinished(t);
                    return;
                    }

                System.out.println("   successfully finished " + getName());
                m_test.threadFinished(null);

                }
            catch (InterruptedException ie)
                {
                Thread.currentThread().interrupt();
                return;
                }
            }

        // ----- data members -----------------------------------------------

        /**
        * The TestCase being run by this thread.
        */
        protected TestCase m_test;

        /**
        * The target Gate.
        */
        protected Gate m_gate;

        /**
        * The number of iterations.
        */
        protected int m_cIteration;

        /**
        * Time in milliseconds when the thread was started
        */
        protected long m_ldtStart;

        /**
        * Maximum time in milliseconds the thread is allowed to run
        */
        protected long m_ldtMaxRun;

        }
    /**
    * Thread that performs successive enter() and exit() operations against
    * a specified Gate.
    */
    public static class EnterExitRunner
            extends Runner
        {

        /**
        * Construct a new EnterExitRunner thread.
        *
        * @param cIteration the number of times to run enter/exit step
        */
        public EnterExitRunner(int cIteration)
            {
            super(cIteration);
            }

        /**
        * {@inheritDoc}
        */
        protected void doIteration(int nIter)
            {
            Gate gate = m_gate;

            gate.enter(-1);
            assertTrue(gate.isEnteredByCurrentThread());
            spinTime(15*(nIter%2 + 1));
            assertTrue(gate.isEnteredByCurrentThread());
            gate.exit();
            assertFalse(gate.isEnteredByCurrentThread());
            }
        }

    /**
    * Thread that performs a sequence of enter() operations
    * followed by a sequence of exit() operations against a specified Gate.
    */
    public static class ReEnterRunner
            extends Runner
        {

        /**
        * Construct a new EnterExitRunner thread.
        *
        * @param cIteration the number of times to run enter/exit step
        */
        public ReEnterRunner(int cIteration)
            {
            super(cIteration);
            }

        /**
        * {@inheritDoc}
        */
        protected void doIteration(int nIter)
            {
            Gate    gate    = m_gate;
            int     cBefore = tloCntEntered.get();
            boolean fEnter;

            // when enough threads have entered, release BarCloseRunner threads from waiting
            if (m_test.m_afCloseWait[0] && m_test.m_atomicEntered.get() > NUM_THREADS / 2)
                {
                synchronized (m_test.m_afCloseWait)
                    {
                    if (m_test.m_afCloseWait[0])
                        {
                        m_test.m_afCloseWait[0] = false;
                        m_test.m_afCloseWait.notifyAll();
                        }
                    }
                }

            // split iterations equally between enter and exit attempts
            if (nIter < NUM_ITERATIONS / 2)
                {
                fEnter = gate.enter(0);
                if (fEnter)
                    {
                    // count only the first enter by the thread
                    if (cBefore == 0)
                        {
                        m_test.m_atomicEntered.getAndIncrement();
                        }
                    tloCntEntered.set(tloCntEntered.get() + 1);
                    spinTime(15*(nIter%2 + 1));
                    }
                }
            else
                {
                spinTime(15 * (nIter % 2 + 1));

                int cThisEntered = tloCntEntered.get();
                if (cThisEntered > 1)
                    {
                    gate.exit();
                    assertTrue("#### cThisEntered="+cThisEntered, gate.isEnteredByCurrentThread());
                    tloCntEntered.set(cThisEntered - 1);
                    }
                else if (cThisEntered == 1)
                    {
                    gate.exit();
                    m_test.m_atomicEntered.getAndDecrement();
                    assertFalse(gate.isEnteredByCurrentThread());
                    tloCntEntered.set(0);
                    }
                else
                    {
                    assertFalse(gate.isEnteredByCurrentThread());
                    }
                }
            }

        // ---- data members ------------------------------------

        private ThreadLocal<Integer> tloCntEntered = new ThreadLocal<Integer>()
                {
                protected Integer initialValue()
                    {
                    return 0;
                    }
                };
        }

    /**
    * Thread that performs successive close() and open() operations against
    * a specified Gate.
    */
    public static class BarCloseRunner
            extends Runner
        {

        /**
        * Construct a new BarCloseRunner thread.
        *
        * @param cIteration the number of iterations
        */
        public BarCloseRunner(int cIteration)
            {
            super(cIteration);
            }

        /**
        * {@inheritDoc}
        */
        protected void doIteration(int nIter)
            {
            if (tloFinished.get())
                {
                return;
                }

            if (m_test.m_afCloseWait[0])
                {
                synchronized (m_test.m_afCloseWait)
                    {
                    if (m_test.m_afCloseWait[0])
                        {
                        try
                            {
                            Blocking.wait(m_test.m_afCloseWait);
                            }
                        catch (InterruptedException e)
                            {
                            }
                        }
                    }
                }

            int nThread = Integer.parseInt(getName());
            Gate gate = m_gate;
            spinTime(15*(nIter%2 + 1));
            assertFalse(gate.isEnteredByCurrentThread());

            switch (nThread)
                {
                // thread "0" bars entry and locks successfully:
                // it waits for a decent number of threads to enter
                // the gate, then bars the entry, only once,
                // then tries to close
                case 0:
                    if (m_test.m_fClosed)
                        {
                        // this thread has closed the gate,
                        // it is still allowed enter and barEntry
                        assertTrue(gate.enter(0));
                        gate.exit();
                        assertTrue(gate.isClosedByCurrentThread());
                        assertTrue(gate.barEntry(0));
                        // this open matches barEntry above
                        gate.open();
                        assertTrue(gate.isClosedByCurrentThread());
                         // let the gate stay closed a while
                        if (nIter > NUM_ITERATIONS*2/3)
                            {
                            // this open matches close done by this thread
                            // in previous iteration
                            gate.open();
                            System.out.print("   Thread 0 opened gate, ");
                            // but predicate remains true, b/c all barEntry
                            // calls have not been matched with open calls yet
                            assertTrue(gate.isClosedByCurrentThread());
                            System.out.println("but isClosedByCurrentThread() is true.");

                            spinTime(15);
                            // this open matches the first barEntry, gate is finally open.
                            // Even if thread "1" does barEntry(), isClosed() should be false
                            gate.open();
                            System.out.print("   Thread 0 opened gate, ");
                            assertFalse(gate.isClosed());
                            System.out.println("now isClosedByCurrentThread() is false.");

                            // gate may be already barred by thread "1",
                            // but not by this thread
                            assertFalse(gate.isClosedByCurrentThread());
                            tloFinished.set(true);
                           }
                        }
                    else if (m_test.m_fBarred)
                        {
                        // check that the thread which barred entry is still allowed to enter
                        assertTrue(gate.enter(0));
                        System.out.println("   Thread 0 can still enter gate.");
                        gate.exit();

                        // keep trying to close
                        int nInsideBeforeClose = m_test.m_atomicEntered.get();
                        m_test.m_fClosed = gate.close(0);
                        int nInsideAfterClose = m_test.m_atomicEntered.get();

                        // do the checks if we were lucky
                        if (nInsideBeforeClose == nInsideAfterClose)
                            {
                            assertTrue(m_test.m_fClosed && nInsideBeforeClose == 0
                                           || !m_test.m_fClosed && nInsideBeforeClose > 0);
                            System.out.println("   Thread "+getName()+": checked correctness of Gate.close().");
                            }

                        // at the end of the run wait up to 3 min to succeed
                        if (!m_test.m_fClosed && nIter == NUM_ITERATIONS - 5)
                            {
                            for (int i = 0; i < 180; i++)
                                {
                                if (m_test.m_fClosed = gate.close(1000))
                                    {
                                    break;
                                    }
                                }
                            assertTrue(m_test.m_fClosed);
                            System.out.println("   Thread 0 closed gate.");
                            }
                        }
                    else if (m_test.m_atomicEntered.get() >= NUM_THREADS/3)
                        {
                        // operation should succeed immediately,
                        // because no other thread is doing barEntry or close
                        m_test.m_fBarred = gate.barEntry(0);

                        assertTrue(m_test.m_fBarred);
                        System.out.print("   Thread 0 barred gate, ");

                        // barEntry != close, gate is not "closed" yet
                        assertFalse(gate.isClosedByCurrentThread());
                        assertFalse(gate.isClosed());
                        System.out.println("   but not closed it.");
                        }
                    break;
                // thread "1" tries to bar entry, but fails until the very end.
                case 1:
                    spinTime(15);

                    // on the last iteration wait more than 3 min to succeed
                    if (nIter == NUM_ITERATIONS - 1)
                        {
                        System.out.println("   Thread 1: last iteration.");
                        // this makes sure that thread "0" goes first
                        while (!m_test.m_fBarred)
                            {
                            Base.sleep(100);
                            }

                        boolean fBarred = false;
                        for (int i = 0; i < 200; i++)
                            {
                            if (fBarred = gate.barEntry(1000))
                                {
                                System.out.println("   Thread 1: barred gate.");
                                break;
                                }
                            }
                        assertTrue(fBarred);
                        gate.open();
                        System.out.println("   Thread 1: opened gate.");
                        tloFinished.set(true);
                        }
                    break;

                default:
                    throw ensureRuntimeException(
                        new Exception("BarCloseThread with number " + nThread + "cannot run."));

                }
            }

        // ---- data members ------------------------------------

        private ThreadLocal<Boolean> tloFinished = new ThreadLocal<Boolean>()
                {
                protected Boolean initialValue()
                    {
                    return Boolean.FALSE;
                    }
                };
        }

        protected static void doOperation(Gate gate, GATE_OPS eOperation,
                                          boolean fExpect, boolean fException)
                throws Throwable
            {
            boolean fResult = false;
            boolean fAssert = false;
            System.out.print(Thread.currentThread());
            try
                {
                switch (eOperation)
                    {
                    case CLOSE:
                        fResult = gate.close(100);
                        fAssert = true;
                        System.out.println(" CLOSE: expect="+fExpect+", actual="+fResult);
                        break;
                    case BAR:
                        fResult = gate.barEntry(100);
                        fAssert = true;
                        System.out.println(" BAR: expect="+fExpect+", actual="+fResult);
                        break;
                    case OPEN:
                        System.out.println(" before OPEN, fException=" + fException);
                        gate.open();
                        System.out.println(" after OPEN");
                        break;
                    case ENTER:
                        fResult = gate.enter(100);
                        fAssert = true;
                        System.out.println(" ENTER: expect="+fExpect+", actual="+fResult);
                        break;
                    case EXIT:
                        System.out.println(" before EXIT");
                        gate.exit();
                        System.out.println(" after EXIT");
                        break;
                    case CLOSED:
                        fResult = gate.isClosed();
                        fAssert = true;
                        System.out.println(" CLOSED: expect="+fExpect+", actual="+fResult);
                        break;
                    case CLOSED_CUR_THR:
                        fResult = gate.isClosedByCurrentThread();
                        fAssert = true;
                        System.out.println(" CLOSE_CUR_THR: expect="+fExpect+", actual="+fResult);
                        break;
                    default:
                        throw new RuntimeException("Runner initialized with unknown gate operation");
                    }
                }
            catch (Throwable t)
                {
                if (fException)
                    {
                    System.out.println("caught expected exception "+t);
                    return;
                    }
                else
                    {
                    System.out.println("==> Unexpected exception! "+t);
                    throw t;
                    }
                }

            if (fException)
                {
                fail("Exception was expected.");
                }

            if (fAssert && fExpect)
                {
                assertTrue(fResult);
                }
            else if (fAssert && !fExpect)
                {
                assertFalse(fResult);
                }
            }

    /**
     * This thread goes second, after FirstRunner, and does one gate operation.
     */
    public static class OrderedRunner extends Runner
        {
        public OrderedRunner(GATE_OPS op, int nOrder, boolean fExpect,
                             Object oMon, boolean fException)
            {
            super(nOrder);
            m_eOperation = op;
            m_nOrder     = nOrder;
            m_oMon       = oMon;
            m_fExpect    = fExpect;
            m_fException = fException;
            }

        public void doIteration(int nIter)
                throws Throwable
            {
            assertTrue(nIter < m_nOrder);
            if (nIter <  m_nOrder - 1)
                {
                synchronized (m_oMon)
                    {
                    try
                        {
                        Blocking.wait(m_oMon, getMaxRun());
                        }
                    catch (InterruptedException e)
                        {
                        if (System.currentTimeMillis() - getStart() < getMaxRun())
                            {
                            doIteration(0);
                            }
                        else
                            {
                            fail(getName() + " could not proceed in time allowed.");
                            }
                        }
                    }
                }
            else
                {
                try
                    {
                    doOperation(m_gate, m_eOperation, m_fExpect, m_fException);
                    }
                finally
                    {
                    // signal completion of your step and release waiting Runners
                    synchronized (m_oMon)
                        {
                        m_oMon.notifyAll();
                        }
                    }
                }
            }

        // ---- data members ------------------------------------

        /**
        *  The order in a multi-threaded scenario when this thread
        *  should execute its operation
        */
        private int m_nOrder;
        /**
        * The Gate operation to be executed by this thread.
        */
        private GATE_OPS m_eOperation;

        /**
        *  The external object to synchronize on (cannot use Runner.m_test)
        */
        private Object m_oMon;

        /**
        * The expected result of a boolean gate operation, to be asserted
        * by the thread, ignored for operations returning void.
        */
        private boolean m_fExpect;

        /**
        * True iff operation should throw an exception,
        * usually IllegalMonitorStateException
        */
        private boolean m_fException;
        }

    // ------ Data members ---------------------------------------
    /**
    * Enumeration of gate operations
    */
    public enum GATE_OPS {CLOSE, BAR, OPEN, ENTER, EXIT, CLOSED, CLOSED_CUR_THR};

    /**
    * The number of iterations to run each test.
    * Should be an even number.
    */
    public static final int NUM_ITERATIONS = 10;

    /**
    * The number of concurrent test threads.
    */
    public static final int NUM_THREADS    = 20;
    }
