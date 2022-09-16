/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package net;

import common.AbstractFunctionalTest;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.DefaultCacheServer;

import com.tangosol.util.Base;
import com.tangosol.util.Resources;

import java.io.File;
import java.io.IOException;

import java.net.URISyntaxException;
import java.net.URL;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.is;

import static org.junit.Assert.assertTrue;

import static test.matcher.CoherenceMatchers.hasThreadGroupSize;

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
        try
            {
            URL url   = Resources.findFileOrResource("coh_new_examples.gar", getContextClassLoader());
            File file = new File(url.toURI());
            GAR_FILE_NAME = file.getAbsolutePath();
            }
        catch (URISyntaxException e)
            {
            throw Base.ensureRuntimeException(e);
            }

        System.setProperty("tangosol.coherence.distributed.localstorage", "true");

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

        server.startDaemon(1000);

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

    /**
     * The GAR file to be exploded by the tests
     */
    public static String GAR_FILE_NAME;

    /**
     * Scheduler service commonly used to start or shutdown DCS in a
     * background thread.
     */
    private static ScheduledExecutorService s_scheduler;
    }
