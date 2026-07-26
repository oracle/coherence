/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package net;

import common.AbstractFunctionalTest;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.DefaultCacheServer;

import java.io.IOException;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.is;

import static test.matcher.CoherenceMatchers.hasThreadGroupSize;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Test the functionality of DefaultCacheServer.
 *
 * @author hr 2012.08.03
 */
public class DefaultCacheServerTests
        extends AbstractFunctionalTest
    {
    // ----- test lifecycle -------------------------------------------------

    /**
     * Initialize the test class.
     */
    @BeforeClass
    public static void _startup()
        {
        System.setProperty("tangosol.coherence.distributed.localstorage", "true");
        System.setProperty("coherence.wka", "127.0.0.1");
        System.setProperty("coherence.localhost", "127.0.0.1");

        s_scheduler = Executors.newScheduledThreadPool(1);

        AbstractFunctionalTest._startup();
        }

    @After
    public void _postTestCleanup()
        {
        // if a previous test shuts down the cluster on a different thread,
        // we need to ensure the next test doesn't pick up a "disposed" cluster
        CacheFactory.shutdown();
        }

    // ----- DCS tests ------------------------------------------------------

    /**
     * Test of startAndMonitor.
     */
    @Test
    public void startAndMonitorTest()
            throws IOException
        {
        DefaultCacheServer server = new DefaultCacheServer(
               CacheFactory.getConfigurableCacheFactory(getContextClassLoader()));

        s_scheduler.schedule(new ServerShutdownTask(server), 1, SECONDS);
        server.startAndMonitor(1000);

        // as startAndMonitor block the current thread if control returns to
        // this frame then ServerShutdownTask did its job
        }

    /**
     * Test the instance level start daemon.
     */
    @Test
    public void startDaemonTest()
            throws IOException, InterruptedException, ExecutionException
        {
        DefaultCacheServer server = new DefaultCacheServer(
               CacheFactory.getConfigurableCacheFactory(getContextClassLoader()));

        startDaemon(server);

        Eventually.assertThat(invoking(server).isMonitorStopped(), is(false));

        server.shutdownServer();

        Eventually.assertThat(invoking(server).isMonitorStopped(), is(true));
        }

    /**
     * Test that static start daemon.
     */
    @Test
    public void staticStartDaemonTest()
            throws IOException
        {
        DefaultCacheServer server = DefaultCacheServer.startServerDaemon();

        Eventually.assertThat(invoking(server).isMonitorStopped(), is(false), Eventually.delayedBy(3, TimeUnit.SECONDS));

        DefaultCacheServer.shutdown();

        Eventually.assertThat(invoking(server).isMonitorStopped(), is(true));
        }

    /**
     * Test the entry point to DCS, main, functions as expected.
     */
    @Test
    public void mainTest()
            throws IOException
        {
        s_scheduler.submit(
            () -> DefaultCacheServer.main(new String[] {"override-cache-config.xml", "1"}));

        Eventually.assertThat(invoking(hasThreadGroupSize(greaterThan(0))).
            matches("OverriddenExamplesPartitionedPofCache"), is(true));

        DefaultCacheServer.shutdown();

        Eventually.assertThat(invoking(hasThreadGroupSize(anyOf(nullValue(), is(0)))).
            matches("OverriddenExamplesPartitionedPofCache"), is(true));
        }

    /**
     * Test start and shutdown DCS with no service monitoring.
     */
    @Test
    public void startAndShutdownDCSWithNoMonitoring()
        {
        DefaultCacheServer.start();

        // shutdown should not throw a NullPointerException
        DefaultCacheServer.shutdown();
        }

    // ----- inner class: ServerShutdownTask --------------------------------

    /**
     * Invoke {@link DefaultCacheServer#shutdownServer()}.
     */
    public final class ServerShutdownTask
            implements Runnable
        {
        public ServerShutdownTask(DefaultCacheServer server)
            {
            m_server = server;
            }

        @Override
        public void run()
            {
            m_server.waitForServiceStart();
            m_server.shutdownServer();
            }

        private final DefaultCacheServer m_server;
        }

    // ----- helpers -----------------------------------------------------------

    /**
     * Start a DefaultCacheServer daemon.
     * <p>
     * This timeboxes DCS.startDaemon() and throws on timeout so an
     * indefinitely retrying startup path cannot hang the test fork.
     *
     * @param server  the server to start
     */
    protected static void startDaemon(DefaultCacheServer server)
        {
        ExecutorService executor = Executors.newSingleThreadExecutor(r ->
            {
            Thread thread = new Thread(r, "DefaultCacheServerTests.startDaemon");
            thread.setDaemon(true);
            return thread;
            });
        Future<?>       future   = executor.submit(() -> server.startDaemon(1000));

        try
            {
            future.get(5, TimeUnit.MINUTES);
            }
        catch (TimeoutException e)
            {
            future.cancel(true);
            server.shutdownServer();
            throw new AssertionError("DefaultCacheServer.startDaemon did not complete within 5 minutes", e);
            }
        catch (InterruptedException e)
            {
            future.cancel(true);
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted while waiting for DefaultCacheServer.startDaemon", e);
            }
        catch (ExecutionException e)
            {
            throw new AssertionError("DefaultCacheServer.startDaemon failed", e.getCause());
            }
        finally
            {
            executor.shutdownNow();
            }
        }

    /**
     * Scheduler service commonly used to start or shutdown DCS in a
     * background thread.
     */
    private static ScheduledExecutorService s_scheduler;
    }
