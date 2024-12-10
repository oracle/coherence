/*
 * Copyright (c) 2020, 2022 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.cdi.server;

import com.tangosol.net.Coherence;
import com.tangosol.net.DefaultCacheServer;

import javax.enterprise.inject.se.SeContainerInitializer;

/**
 * This class bootstraps the CDI container, which will in turn start Coherence
 * server via CDI extension.
 * <p>
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

        // wait until Coherence is shut down
        Coherence.getInstance().whenClosed().join();
        }
    }
