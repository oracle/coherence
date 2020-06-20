/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.cdi.server;

import com.oracle.coherence.common.base.Blocking;

import com.tangosol.net.DefaultCacheServer;

import javax.enterprise.inject.se.SeContainerInitializer;

/**
 * This class bootstraps the CDI container, which will in turn start Coherence
 * server via CDI extension.
 * <p/>
 * This class should only be used when Coherence CDI is used in a standalone
 * mode. When used with Helidon 2.0, Helidon's {@code io.helidon.microprofile.cdi.Main}
 * should be used instead and will ensure that both Helidon and Coherence
 * services are started correctly.
 *
 * @author Aleks Seovic  2020.03.24
 * @since 20.06
 */
public class Server
    {
    /**
     * Main method that creates and starts {@code CdiCacheServer}.
     *
     * @param args program arguments
     */
    public static void main(String[] args)
        {
        new Server().start();
        }

    /**
     * Starts CDI, which will indirectly start {@link DefaultCacheServer}.
     */
    public void start()
        {
        // configure logging
        LogConfig.configureLogging();

        // bootstrap Weld
        SeContainerInitializer initializer = SeContainerInitializer.newInstance();
        initializer.initialize();

        monitorServices(5000L);
        }

    /**
     * Blocks in a loop for {@code cWaitMillis} until the Coherence monitor is
     * stopped.
     *
     * @param cWaitMillis the number of milliseconds to block during each
     *                    iteration
     */
    private void monitorServices(long cWaitMillis)
        {
        DefaultCacheServer dcs = DefaultCacheServer.getInstance();
        do
            {
            try
                {
                Blocking.sleep(cWaitMillis);
                }
            catch (InterruptedException ignore)
                {
                }
            }
        while (!dcs.isMonitorStopped());

        dcs.shutdownServer();
        }
    }
