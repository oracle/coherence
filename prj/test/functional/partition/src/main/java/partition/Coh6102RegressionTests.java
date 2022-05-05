/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package partition;


import static org.junit.Assert.assertFalse;
import com.tangosol.net.Cluster;
import com.tangosol.net.GuardSupport;
import com.tangosol.net.Guardable;
import com.tangosol.net.NamedCache;
import com.tangosol.net.Service;
import com.tangosol.net.ServiceFailurePolicy;

import com.tangosol.util.Base;
import com.tangosol.util.MapEvent;
import com.tangosol.util.MultiplexingMapListener;
import com.tangosol.util.extractor.IdentityExtractor;
import com.tangosol.util.filter.AlwaysFilter;
import com.tangosol.util.filter.GreaterFilter;
import com.tangosol.util.processor.ConditionalRemove;

import com.oracle.coherence.common.internal.util.HeapDump;
import com.oracle.coherence.testing.AbstractFunctionalTest;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.junit.BeforeClass;
import org.junit.Test;


public class Coh6102RegressionTests
        extends AbstractFunctionalTest
    {
    public Coh6102RegressionTests()
        {
        super("regression-cache-config.xml");
        }


    // ----- test lifecycle -------------------------------------------------

    /**
    * Initialize the test class.
    */
    @BeforeClass
    public static void _startup()
        {
        // this test requires local storage to be enabled
        System.setProperty("coherence.distributed.localstorage", "true");
        System.setProperty("coherence.distributed.threads.min", "4");
        System.setProperty("coherence.log.level", "2");

        AbstractFunctionalTest._startup();
        }


    // ----- test methods ---------------------------------------------------

    @Test
    public void test()
            throws InterruptedException
        {
        m_fRunning = true;
        m_fRecoverd = false;
        m_cache = getFactory().ensureCache("COH6102", null);
        m_cache.addIndex(IdentityExtractor.INSTANCE, true, null);

        Runnable indexRunner = new Runnable()
        {
            public void run()
                {
                NamedCache cache = m_cache;
                Random random = new Random();
                while (m_fRunning)
                    {
                    cache.addIndex(IdentityExtractor.INSTANCE, true, null);
                    long lVal = random.nextInt(100);
                    cache.put(lVal, lVal, 22l);
                    // As of COH-14657, addIndex just schedule UpdateIndexRequest
                    // for each partition and return.  All cache operation need to wait
                    // for the associated partition addIndex task to finish.  Since this
                    // test has multiple indexRunner threads that constantly addIndex/removeIndex,
                    // updateRunner thread's put operation hardly get a chance to run.
                    // Add 1s sleep to let test finish normally.
                    Base.sleep(1000);
                    cache.removeIndex(IdentityExtractor.INSTANCE);
                    }
                }
        };

        Runnable updateRunner = new Runnable()
        {
            public void run()
                {
                NamedCache cache = m_cache;
                Random random = new Random();
                for (long i = 0; i < 500000 && m_fRunning; i++)
                    {
                    long lVal = random.nextInt(100);
                    cache.put(lVal, lVal, 22l);
                    if (i % 1000 == 0)
                        {
                        System.out.print('.');
                        System.out.flush();
                        }
                    }
                // flag the other threads to terminate
                m_fRunning = false;
                }
        };

        Thread updateThread1 = new Thread(updateRunner);
        Thread updateThread2 = new Thread(indexRunner);
        Thread updateThread3 = new Thread(indexRunner);
        Thread updateThread4 = new Thread(indexRunner);

        updateThread1.start();
        updateThread2.start();
        updateThread3.start();
        updateThread4.start();

        updateThread1.join();
        updateThread2.join();
        updateThread3.join();
        updateThread4.join();

        assertFalse("one or more threads was determined to have been stuck", m_fRecoverd);
        }


    public static class Listener
            extends MultiplexingMapListener
        {
        protected void onMapEvent(MapEvent evt)
            {
            s_executor.execute(new Runnable()
            {
                public void run()
                    {
                    if (m_fRunning)
                        {
                    m_cache.invokeAll(new GreaterFilter(IdentityExtractor.INSTANCE, 22l), new ConditionalRemove(
                            AlwaysFilter.INSTANCE));
                        }
                    }
            });
            }
        }

    static ExecutorService s_executor = Executors.newSingleThreadExecutor(new ThreadFactory()
    {
        public Thread newThread(Runnable r)
            {
            Thread thread = new Thread(r);
            thread.setDaemon(true);
            return thread;
            }
    });

    public static class COH6102RecoverPolicy
            implements ServiceFailurePolicy
        {
        @Override
        public void onGuardableRecovery(Guardable guardable, Service service)
            {
            System.out.println("onGuardableRecovery");
            m_fRunning = false;
            m_fRecoverd = true;

            System.out.println(HeapDump.dumpHeap());

            Base.log(GuardSupport.getThreadDump());
            guardable.recover();
            }

        @Override
        public void onGuardableTerminate(Guardable guardable, Service service)
            {
            System.out.println(HeapDump.dumpHeap());
            
            System.out.println("onGuardableTerminate");
            Base.log(GuardSupport.getThreadDump());
            }

        @Override
        public void onServiceFailed(Cluster cluster)
            {
            System.out.println("onServiceFailed");
            Base.log(GuardSupport.getThreadDump());
            }

        }

    static NamedCache m_cache;
    static volatile boolean m_fRunning;
    static volatile boolean m_fRecoverd;
    }
