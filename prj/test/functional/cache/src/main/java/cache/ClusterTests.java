/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package cache;

import org.junit.Test;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.coherence.common.base.Disposable;
import com.oracle.coherence.testing.AbstractFunctionalTest;

import com.tangosol.coherence.component.application.console.Coherence;
import com.tangosol.coherence.component.util.SafeCluster;
import com.tangosol.coherence.component.util.SafeService;
import com.tangosol.coherence.component.util.daemon.queueProcessor.Service;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.Cluster;

import com.tangosol.util.ServiceEvent;
import com.tangosol.util.ServiceListener;
import com.tangosol.util.SynchronousListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.oracle.bedrock.deferred.DeferredHelper.within;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
* Test harness for Cluster.
*
* @author cp 2010-06-xx
*/
public class ClusterTests
        extends AbstractFunctionalTest
    {
    /**
    * Default constructor.
    */
    public ClusterTests()
        {
        }

    /**
     * Verify that discovery shutdown cannot reload configuration after it is cleared.
     */
    @Test
    public void testShutdownStopsNameService()
        {
        SafeCluster cluster = (SafeCluster) CacheFactory.ensureCluster();
        Thread      thread  = cluster.getCluster().getNameService().getAcceptor().getThread();

        assertNotNull("the local name service must be running", thread);
        assertTrue(thread.isAlive());

        CacheFactory.shutdown();

        assertFalse("the name service must exit before shutdown returns", thread.isAlive());
        assertFalse("shutdown must leave operational configuration cleared", Coherence.isConfigurationLoaded());
        }

    /**
     * Verify that interrupting an application-service wait also skips discovery waits.
     */
    @Test
    public void testShutdownPreservesServiceWaitInterrupt()
            throws Exception
        {
        CacheFactory.shutdown();

        String               sTimeout        = System.getProperty("coherence.shutdown.timeout");
        BlockingStopListener listenerService = new BlockingStopListener();
        BlockingStopListener listenerName    = new BlockingStopListener();
        List<Thread>         listThreads     = new ArrayList<>();

        try
            {
            System.setProperty("coherence.shutdown.timeout", "30s");

            SafeService serviceSafe = (SafeService) CacheFactory.getCache("dist-shutdown-interrupt").getCacheService();
            Service     service     = (Service) serviceSafe.getRunningService();
            com.tangosol.coherence.component.net.Cluster cluster =
                    ((SafeCluster) CacheFactory.ensureCluster()).getCluster();
            Service serviceName = cluster.getNameService().getAcceptor();

            service.addServiceListener(listenerService);
            serviceName.addServiceListener(listenerName);
            listThreads.add(service.getThread());
            listThreads.add(serviceName.getThread());

            AtomicBoolean fInterrupted = new AtomicBoolean();
            Thread threadStop = new Thread(() ->
                {
                cluster.stop();
                fInterrupted.set(Thread.currentThread().isInterrupted());
                }, "cluster-stop-interrupt-test");
            listThreads.add(threadStop);
            threadStop.start();

            assertTrue("application service must enter its exit callback",
                    listenerService.f_entered.await(10, TimeUnit.SECONDS));
            // the blocked application service prevents reaching the ClusterService join
            Eventually.assertDeferred(() -> Arrays.stream(threadStop.getStackTrace()).anyMatch(frame ->
                    frame.getClassName().equals("java.lang.Thread") && frame.getMethodName().equals("join")),
                    is(true), within(10, TimeUnit.SECONDS));
            threadStop.interrupt();

            assertTrue("NameService must still receive its stop request",
                    listenerName.f_entered.await(10, TimeUnit.SECONDS));
            threadStop.join(5000);

            assertFalse("interrupted shutdown must not wait for the blocked NameService", threadStop.isAlive());
            assertTrue("shutdown must restore the caller's interrupt status", fInterrupted.get());
            }
        finally
            {
            listenerService.f_release.countDown();
            listenerName.f_release.countDown();
            try
                {
                for (Thread thread : listThreads)
                    {
                    thread.join(10000);
                    }
                CacheFactory.shutdown();
                }
            finally
                {
                if (sTimeout == null)
                    {
                    System.clearProperty("coherence.shutdown.timeout");
                    }
                else
                    {
                    System.setProperty("coherence.shutdown.timeout", sTimeout);
                    }
                }
            }
        }

    /**
    * Test the ability to manage resources (COH-4268) in the cluster.
    *
    * @throws Exception
    */
    @Test
    public void testResourceManagement()
            throws Exception
        {
        Cluster cluster = CacheFactory.ensureCluster();

        // First testing that you can't register a null resource
        try
            {
            cluster.registerResource("test", null);
            fail("It shouldn't be possible to register a null resource");
            }
        catch (NullPointerException e)
            {
            // expected
            }
        catch (Throwable t)
            {
            fail("Only NullPointerException allowed (" + t + ")");
            }

        // A disposable to use in the tests
        Disposable disp = new Disposable()
            {
            @Override
            public void dispose()
                {
                }
            };

        // Testing basic retrieval and storage of resource
        cluster.registerResource("test", disp);
        assertTrue(cluster.getResource("test") == disp);

        // Testing storing of the same resource again, which is allowed
        cluster.registerResource("test", disp);

        // Now testing registering another resource with the same name, not allowed
        Disposable anotherDisp = new Disposable()
            {
            @Override
            public void dispose()
                {
                }
            };
        try
            {
            cluster.registerResource("test", anotherDisp);
            fail("It shouldn't be possible to register another resource with the same name");
            }
        catch (IllegalArgumentException e)
            {
            // expected
            }
        catch (Throwable t)
            {
            fail("Only IllegalArgumentException allowed (" + t + ")");
            }

        // but if we unregister the resource first it is possible to register
        // a different resource
        assertTrue(cluster.unregisterResource("test") == disp);
        cluster.registerResource("test", anotherDisp);
        out("JournalTests.testResourceManagement finished");
        }

    /**
     * Hold a service thread in its exit callback until the test releases it.
     *
     * @since 26.10
     */
    private static class BlockingStopListener
            implements ServiceListener, SynchronousListener
        {
        @Override
        public void serviceStarting(ServiceEvent evt)
            {
            }

        @Override
        public void serviceStarted(ServiceEvent evt)
            {
            }

        @Override
        public void serviceStopping(ServiceEvent evt)
            {
            }

        @Override
        public void serviceStopped(ServiceEvent evt)
            {
            f_entered.countDown();
            boolean fInterrupted = false;
            try
                {
                while (true)
                    {
                    try
                        {
                        f_release.await();
                        return;
                        }
                    catch (InterruptedException e)
                        {
                        fInterrupted = true;
                        }
                    }
                }
            finally
                {
                if (fInterrupted)
                    {
                    Thread.currentThread().interrupt();
                    }
                }
            }

        private final CountDownLatch f_entered = new CountDownLatch(1);
        private final CountDownLatch f_release = new CountDownLatch(1);
        }
    }
