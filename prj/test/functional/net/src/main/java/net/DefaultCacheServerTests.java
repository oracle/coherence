/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package net;

import com.oracle.bedrock.testsupport.MavenProjectFileUtils;
import com.oracle.coherence.common.base.Timeout;
import com.oracle.coherence.testing.AbstractFunctionalTest;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.DefaultCacheServer;

import java.io.File;
import java.io.IOException;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.number.OrderingComparison.greaterThanOrEqualTo;

import static org.junit.Assert.assertTrue;

import static com.oracle.coherence.testing.matcher.CoherenceMatchers.hasThreadGroupSize;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Test the functionality of DefaultCacheServer in both normal and gar modes.
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
        File buildFolder   = MavenProjectFileUtils.locateBuildFolder(DefaultCacheServerTests.class);
        File classesFolder = new File(buildFolder, "classes");

        System.setProperty("coherence.distributed.localstorage", "true");

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
    public void startAndMonitorTest() throws Exception
        {
        DefaultCacheServer server = new DefaultCacheServer(
               CacheFactory.getConfigurableCacheFactory(getContextClassLoader()));

        s_scheduler.schedule(new ServerShutdownTask(server), 5, SECONDS);
        try (Timeout ignored = Timeout.after(30, SECONDS))
            {
            server.startAndMonitor(1000);
            // as startAndMonitor block the current thread if control returns to
            // this frame then ServerShutdownTask did its job
            }
        }

    /**
     * Test the instance level start daemon.
     */
    @Test
    public void startDaemonTest()
        {
        DefaultCacheServer server = new DefaultCacheServer(
               CacheFactory.getConfigurableCacheFactory(getContextClassLoader()));

        server.startDaemon(1000);

        Eventually.assertDeferred(server::isMonitorStopped, is(false));

        server.shutdownServer();

        Eventually.assertDeferred(server::isMonitorStopped, is(true));
        }

    /**
     * Test that static start daemon.
     */
    @Test
    public void staticStartDaemonTest()
        {
        DefaultCacheServer server = DefaultCacheServer.startServerDaemon();

        Eventually.assertDeferred(server::isMonitorStopped, is(false), Eventually.delayedBy(3, TimeUnit.SECONDS));

        DefaultCacheServer.shutdown();

        Eventually.assertDeferred(server::isMonitorStopped, is(true));
        }

    /**
     * Test the entry point to DCS, main, functions as expected.
     */
    @Test
    public void mainTest()
        {
        s_scheduler.submit(
            () -> DefaultCacheServer.main(new String[] {"override-cache-config.xml", "1"}));

        Eventually.assertDeferred(() -> hasThreadGroupSize(greaterThan(0)).
            matches("OverriddenExamplesPartitionedPofCache"), is(true));

        DefaultCacheServer.shutdown();

        Eventually.assertDeferred(() -> hasThreadGroupSize(anyOf(nullValue(), is(0))).
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
    public final static class ServerShutdownTask
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

    /**
     * Scheduler service commonly used to start or shutdown DCS in a
     * background thread.
     */
    private static ScheduledExecutorService s_scheduler;
    }
