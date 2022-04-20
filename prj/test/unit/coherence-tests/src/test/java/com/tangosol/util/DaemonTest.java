/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


import com.oracle.coherence.common.base.Blocking;

/**
* Various tests of Daemon and TaskDaemon.
*
* @author cp  2006.02.21
*/
public class DaemonTest
        extends Base
    {
    public static void main(String[] asArg)
            throws Exception
        {
        testDaemon();

        testTaskDaemon(false);
        testTaskDaemon(true);

        testFinishing(false);
        testFinishing(true);

        testPeriodic();

        // for (int i = 0; i < 10000; ++i)
        //    new DaemonTest().testInnerClassProblem();
        }

    public static void testDaemon()
            throws Exception
        {
        Daemon daemon = new Daemon()
            {
            public void run()
                {
                out("daemon=" + this + ", thread=" + getThread()
                    + " (internal hash=" + System.identityHashCode(getThread())
                    + "), n=" + (++n));
                }
            int n;
            };

        out("running=" + daemon.isRunning() + ", stopping=" + daemon.isStopping());
        daemon.start();
        sleep(1);

        out("running=" + daemon.isRunning() + ", stopping=" + daemon.isStopping());
        daemon.start();
        sleep(1);

        out("running=" + daemon.isRunning() + ", stopping=" + daemon.isStopping());
        daemon.start();
        sleep(1);

        out("running=" + daemon.isRunning() + ", stopping=" + daemon.isStopping());
        daemon.start();
        sleep(1000);
        daemon.stop();
        }

    public static void testTaskDaemon(boolean fTimeout)
            throws Exception
        {
        out();
        out("*** testTaskDaemon(" + fTimeout + ")");

        TaskDaemon daemon = new TaskDaemon("test");
        if (fTimeout)
            {
            out();
            out("setting timeout to 2 seconds");
            daemon.setIdleTimeout(2000);
            }

        Runnable task = new Runnable()
            {
            public void run()
                {
                out("running: " + this);
                }
            public String toString()
                {
                return "test task (current thread: " + Thread.currentThread()
                    + ", internal hash=" + System.identityHashCode(
                        Thread.currentThread()) + ")";
                }
            };

        out();
        out("queueing task: " + task);
        daemon.executeTask(task);

        out();
        out("queuing task to run in 5 seconds");
        daemon.scheduleTask(task, getSafeTimeMillis() + 5000);
        sleep(10000);

        out();
        out("queuing task to run in 1..10 seconds");
        for (int i = 1; i <= 10; ++i)
            {
            daemon.scheduleTask(task, getSafeTimeMillis() + (i * 1000));
            }
        sleep(15000);

        out();
        out("queueing task");
        daemon.executeTask(task);

        out();
        out("stopping daemon");
        daemon.stop();
        sleep(1000);

        out();
        out("queueing task");
        daemon.executeTask(task);

        out();
        out("stopping daemon");
        daemon.stop();
        sleep(1000);
        }

    public static void testFinishing(boolean fFinish)
            throws Exception
        {
        out();
        out("*** testFinishing(" + fFinish + ")");

        TaskDaemon daemon = new TaskDaemon("test");
        if (fFinish)
            {
            out();
            out("setting finish");
            daemon.setFinishing(true);
            }

        Runnable task = new Runnable()
            {
            int n;
            public void run()
                {
                out("running #" + (++n));
                sleep(1);
                }
            };

        out();
        out("queueing 50 tasks");
        for (int i = 0; i < 50; ++i)
            {
            daemon.executeTask(task);
            }

        out();
        sleep(10);
        out("stopping daemon");
        daemon.stop();
        sleep(1000);
        }

    public static void testPeriodic()
            throws Exception
        {
        out();
        out("*** testPeriodic()");

        TaskDaemon daemon = new TaskDaemon("test");
        Runnable task = new Runnable()
            {
            int n;
            public void run()
                {
                out("running #" + (++n));
                sleep(1);
                }
            };

        out();
        out("queueing periodic");
        daemon.executePeriodicTask(task, 100);
        sleep(1000);

        out();
        out("stopping daemon");
        daemon.stop();
        sleep(1000);
        }

    /**
    * This test breaks on JDK 1.4 and works on JDK 1.5. The difference is in
    * the JAVAC compiler itself, as JDK 1.4 compiled code will still break
    * when run on JDK 1.5.
    */
    public void testInnerClassProblem()
        {
        new Daemon("test", Thread.NORM_PRIORITY, true)
            {
            public void run()
                {
                try
                    {
                    azzert(this != null);
                    azzert(DaemonTest.this != null);
                    azzert(DaemonTest.this.m_sTest != null);
                    }
                catch (Exception e)
                    {
                    System.out.println(e);
                    }
                }
            };
        }
    public String m_sTest = "this is a test";

    public static void sleep(int cMillis)
        {
        try
            {
            Blocking.sleep(cMillis);
            }
        catch (Exception e) {}
        }
    }
