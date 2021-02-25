/*
 * Copyright (c) 2000-2021 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.guides.cachestores;

import com.tangosol.net.Coherence;
import com.tangosol.net.CoherenceConfiguration;
import com.tangosol.net.Session;
import com.tangosol.net.SessionConfiguration;

import org.junit.jupiter.api.AfterAll;

/**
 * Base functionality for cache stores tests.
 *
 * @author Tim Middleton 2020.02.17
 */
public abstract class AbstractCacheStoreTest {

    /**
     * Coherence the {@link Session}.
     */
    private static Session session;

    /**
     * Returns the {@link Session}.
     * @return the {@link Session}
     */
    protected static Session getSession() {
        return session;
    }

    /**
     * Startup Coherence with a given cache config.
     *
     * @param cacheConfig cache config to use
     */
    protected static void startupCoherence(String cacheConfig) {
        System.setProperty("coherence.log.level", "3");

        SessionConfiguration sessionConfig = SessionConfiguration.builder()
                                                                 .withConfigUri(cacheConfig)
                                                                 .build();
        CoherenceConfiguration cfg = CoherenceConfiguration.builder()
                                                           .withSession(sessionConfig)
                                                           .build();
        Coherence coherence = Coherence.clusterMember(cfg);
        coherence.start().join();

        session = coherence.getSession();
    }

    @AfterAll
    static void shutdownCoherence() {
        Coherence coherence = Coherence.getInstance(); //<1>
        coherence.close();
    }

    /**
     * Close a resource silently.
     *
     * @param closeable the {@link AutoCloseable} to close
     */
    protected static void closeQuiet(AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            }
            catch (Exception ignore) {
            }
        }
    }
}
