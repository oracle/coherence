/*
 * Copyright (c) 2022, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.guides.extend.utils;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.Coherence;
import com.tangosol.net.CoherenceConfiguration;
import com.tangosol.net.NamedCache;
import com.tangosol.net.Session;
import com.tangosol.net.SessionConfiguration;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Helper class to start Coherence and to get caches.
 *
 * @author Gunnar Hillert  2022.09.26
 */
public abstract class CoherenceHelper {

    public static final String FIREWALL_INSTANCE_NAME = "firewall";
    public static final String LOAD_BALANCING_INSTANCE_NAME = "loadBalancing";
    public static final String NAME_SERVICE_INSTANCE_NAME = "nameService";

    // # tag::bootstrap[]
    public static void startCoherenceClient(String instanceName, String cacheConfig) {
        SessionConfiguration sessionConfiguration = SessionConfiguration.create(cacheConfig);

        SessionConfiguration sessionCfg = SessionConfiguration.builder()
                .named(SessionConfiguration.defaultSession().getName())
                .withConfigUri(cacheConfig)
                .build();

        CoherenceConfiguration cfg = CoherenceConfiguration.builder()
                .named(instanceName)
                .withSession(SessionConfiguration.defaultSession())
                .withSession(sessionCfg)
                .build();

        // Create the Coherence instance from the configuration
        Coherence coherence = Coherence.client(cfg);

        // Start Coherence
        try {
            coherence.start().get(60, TimeUnit.SECONDS);
        } catch (TimeoutException | InterruptedException | ExecutionException e) {
            throw new RuntimeException("Coherence did not start up within 60s.", e);
        }
    }
    // # end::bootstrap[]

    public static <K, V> NamedCache<K, V> getMap(String instanceName, String name) {
        Coherence coherence = Coherence.getInstance(instanceName); // <1>
        Session session = coherence.getSession(); // <2>
        return session.getCache(name); // <3>
    }

    public static void cleanup() {
        Coherence.closeAll();
        CacheFactory.shutdown();
    }
}
