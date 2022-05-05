/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package net;

import com.oracle.coherence.common.base.Timeout;
import com.oracle.coherence.testing.AbstractFunctionalTest;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.DefaultCacheServer;
import com.tangosol.net.RequestTimeoutException;

import com.tangosol.util.Base;
import com.tangosol.util.WrapperException;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test the functionality of the newly implemented interruptible locks
 * in the safe tier (COH-23345).
 *
 * @author bbc 2021.12.09
 */
public class StartClusterWithTimeoutTests
    extends AbstractFunctionalTest
    {
    // ----- test lifecycle -------------------------------------------------

    /**
     * Initialize the test class.
     */
    @BeforeClass
    public static void _startup()
        {
        }

    @After
    public void shutdown()
        {
        CacheFactory.shutdown();
        }


    // ----- test methods ---------------------------------------------------

    /**
     * Test that cluster starting process should be interrupted if it take longer than
     * the given timeout.
     */
    @Test
    public void testShouldBeInterrupted()
        {
        // test start via CF.ensureCluster
        try (Timeout t = Timeout.after(2, TimeUnit.SECONDS))
            {
            CacheFactory.ensureCluster();
            fail("CacheFactory.ensureCluster should be interrupted!");
            }
        catch (Exception e)
            {
            assertTrue(((WrapperException) e).getOriginalException() instanceof InterruptedException);
            }

        // test start via getCache
        try (Timeout t = Timeout.after(2, TimeUnit.SECONDS))
            {
            CacheFactory.getCache("dist");
            fail("CacheFactory.getCache should be interrupted!");
            }
        catch (Exception e)
            {
            assertTrue(((WrapperException) e).getOriginalException() instanceof InterruptedException);
            }

        // test start with DefaultCacheServer
        try (Timeout t = Timeout.after(2, TimeUnit.SECONDS))
            {
            DefaultCacheServer server = new DefaultCacheServer(
                    CacheFactory.getConfigurableCacheFactory(getContextClassLoader()));

            server.startDaemon(1000);

            //no exception but server should not started
            assertTrue(server.isMonitorStopped());
            }
        catch (Exception e)
            {
            // should not have InterruptedException as startDaemon catch and swallow the InterruptedException
            // see DefaultCacheServer.startDaemon
            }
        }

    /**
     * Test that thread local Timeout should take precedence over request timeout.
     */
    @Test
    public void testTimeoutShouldTakePrecedence()
        {
        try (Timeout t = Timeout.after(2, TimeUnit.MINUTES))
            {
            System.setProperty("coherence.distributed.request.timeout", "2s");
            CacheFactory.ensureCluster();
            }
        catch (Exception e)
            {
            fail("Thread local Timeout (2m) should take Precedence over request timeout (2s)");
            }
        finally
            {
            System.clearProperty("coherence.distributed.request.timeout");
            }
        }

    /**
     * Test concurrent starting cluster behavior. If a winner thread took the lock and starting the cluster, other
     * threads should only block on lock for the specified timeout.
     */
    @Test
    public void testShouldTimeoutWithEnsureCluster() throws Exception
        {
        final CountDownLatch latch1 = new CountDownLatch(1);
        final CountDownLatch latch2 = new CountDownLatch(1);

        ExecutorService executor = null;
        try
            {
            executor = Executors.newFixedThreadPool(3);
            Future<Exception> future1 = executor.submit(() ->
                {
                latch1.await();

                Exception exception = null;
                try (Timeout t = Timeout.after(2, TimeUnit.MINUTES))
                    {
                    CacheFactory.ensureCluster();
                    }
                catch (Exception e)
                    {
                    exception = e;
                    }
                return exception;
                });

            Future<Exception> future2 = executor.submit(() ->
                {
                latch2.await();

                Exception exception = null;
                try (Timeout t = Timeout.after(1, TimeUnit.SECONDS))
                    {
                    CacheFactory.ensureCluster();
                    }
                catch (Exception e)
                    {
                    exception = e;
                    }

                return exception;
                });

            // ensure task1 is the lock winner
            latch1.countDown();
            Base.sleep(1000);
            latch2.countDown();

            Exception exception = future1.get();
            if (exception != null)
                {
                fail("CacheFactory.ensureCluster() failed to start the cluster! \n " + Base.getStackTrace(exception));
                }

            exception = future2.get();
            if (exception == null)
                {
                fail("CacheFactory.ensureCluster should failed with RequestTimeout!");
                }
            else
                {
                assertTrue(exception instanceof RequestTimeoutException);
                }
            }
        finally
            {
            executor.shutdown();
            }
        }

    /**
     * Test concurrent starting cluster behavior. If a winner thread took the lock and starting the cluster, other
     * threads should only block on lock for the specified timeout.
     */
    @Test
    public void testShouldTimeoutWithGetCache() throws Exception
        {
        final CountDownLatch latch1 = new CountDownLatch(1);
        final CountDownLatch latch2 = new CountDownLatch(1);

        ExecutorService executor = null;
        try
            {
            executor = Executors.newFixedThreadPool(3);
            Future<Exception> future1 = executor.submit(() ->
                {
                latch1.await();

                Exception exception = null;
                try (Timeout t = Timeout.after(2, TimeUnit.MINUTES))
                    {
                    CacheFactory.getCache("dist");
                    }
                catch (Exception e)
                    {
                    exception = e;
                    }
                return exception;
                });

            Future<Exception> future2 = executor.submit(() ->
                {
                latch2.await();

                Exception exception = null;
                try (Timeout t = Timeout.after(1, TimeUnit.SECONDS))
                    {
                    CacheFactory.getCache("dist");
                    }
                catch (Exception e)
                    {
                    exception = e;
                    }

                return exception;
                });

            // ensure task1 is the lock winner
            latch1.countDown();
            Base.sleep(1000);
            latch2.countDown();

            Exception exception = future1.get();
            if (exception != null)
                {
                fail("CacheFactory.getCache() failed to start the cluster! \n " + Base.getStackTrace(exception));
                }

            exception = future2.get();
            if (exception == null)
                {
                fail("CacheFactory.getCache() should failed with RequestTimeoutException!");
                }
            else
                {
                assertTrue(exception instanceof RequestTimeoutException);
                }
            }
        finally
            {
            executor.shutdown();
            }
        }
    }
