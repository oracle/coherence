/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.guides.federation;

import com.oracle.bedrock.OptionsByType;

import com.oracle.bedrock.runtime.LocalPlatform;
import com.oracle.bedrock.runtime.coherence.CoherenceCacheServer;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.coherence.JMXManagementMode;
import com.oracle.bedrock.runtime.coherence.options.CacheConfig;
import com.oracle.bedrock.runtime.coherence.options.ClusterName;
import com.oracle.bedrock.runtime.coherence.options.ClusterPort;
import com.oracle.bedrock.runtime.coherence.options.LocalStorage;
import com.oracle.bedrock.runtime.coherence.options.LocalHost;
import com.oracle.bedrock.runtime.coherence.options.Logging;
import com.oracle.bedrock.runtime.coherence.options.Multicast;
import com.oracle.bedrock.runtime.coherence.options.WellKnownAddress;
import com.oracle.bedrock.runtime.java.options.SystemProperty;
import com.oracle.bedrock.runtime.java.profiles.JmxProfile;
import com.oracle.bedrock.runtime.network.AvailablePortIterator;
import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class FederationExampleTest {
    private static final String CLUSTER_SUFFIX = java.util.UUID.randomUUID().toString();
    private static final String PRIMARY_CLUSTER = "ClusterA-" + CLUSTER_SUFFIX;
    private static final String SECONDARY_CLUSTER = "ClusterB-" + CLUSTER_SUFFIX;

    protected static AvailablePortIterator availablePortIteratorWKA;
    protected static CoherenceCacheServer  primaryMember   = null;
    protected static CoherenceCacheServer  secondaryMember = null;
    private static final String CACHE_CONFIG = "federation-cache-config.xml";
    private static final String CACHE = "test-cache";
    private static final String edition = CacheFactory.getEdition();

    @BeforeAll
    public static void _startup() {
        // ignore test if we are running under community edition
        Assumptions.assumeFalse("CE".equals(edition));

        LocalPlatform platform = LocalPlatform.get();
        availablePortIteratorWKA = platform.getAvailablePorts();
        availablePortIteratorWKA.next();

        int primaryClusterPort   = availablePortIteratorWKA.next();
        int secondaryClusterPort = availablePortIteratorWKA.next();

        int primaryFederationPort   = availablePortIteratorWKA.next();
        int secondaryFederationPort = availablePortIteratorWKA.next();

        OptionsByType primaryClusterOptions = createCacheServerOptions(PRIMARY_CLUSTER, primaryClusterPort,
                primaryFederationPort, primaryFederationPort, secondaryFederationPort);
        OptionsByType secondaryClusterOptions = createCacheServerOptions(SECONDARY_CLUSTER, secondaryClusterPort,
                secondaryFederationPort, primaryFederationPort, secondaryFederationPort);

        primaryMember = platform.launch(CoherenceCacheServer.class, primaryClusterOptions.asArray());
        secondaryMember = platform.launch(CoherenceCacheServer.class, secondaryClusterOptions.asArray());


        Eventually.assertDeferred(() -> primaryMember.getClusterSize(), is(1));
        Eventually.assertDeferred(() -> secondaryMember.getClusterSize(), is(1));
    }

    @AfterAll
    public static void _shutdown() {
        // ignore test if we are running under community edition
        Assumptions.assumeFalse("CE".equals(edition));
        CacheFactory.shutdown();
        destroyMember(primaryMember);
        destroyMember(secondaryMember);
    }

    @Test
    public void runTest() {
        // ignore test if we are running under community edition
        Assumptions.assumeFalse("CE".equals(edition));
        final int COUNT = 1000;

        NamedCache<Integer, String> ncPrimary = primaryMember.getCache(CACHE);
        NamedCache<Integer, String> ncSecondary = secondaryMember.getCache(CACHE);

        ncPrimary.clear();
        ncSecondary.clear();

        assertEquals(0, ncPrimary.size());
        assertEquals(0, ncSecondary.size());

        Map<Integer, String> buffer = new HashMap<>();
        for (int i = 0; i < COUNT; i++) {
            buffer.put(i, "Value-" + i);
        }

        // Add data to primary cluster
        ncPrimary.putAll(buffer);
        assertEquals(COUNT, ncPrimary.size());

        // wait for data to reach secondary
        Eventually.assertDeferred(ncSecondary::size, is(COUNT));

        // clear the data in secondary and wait for primary to be 0
        ncSecondary.clear();

        Eventually.assertDeferred(ncPrimary::size, is(0));
    }

    protected static OptionsByType createCacheServerOptions(String clusterName, int clusterPort, int federationPortLocal,
                                                            int federationPortPrimary, int federationPortSecondary) {
        String        hostName      = "127.0.0.1";
        OptionsByType optionsByType = OptionsByType.empty();
        String        mode          = System.getProperty("coherence.mode");

        optionsByType.addAll(JMXManagementMode.ALL,
                JmxProfile.enabled(),
                LocalStorage.enabled(),
                LocalHost.only(),
                WellKnownAddress.of(hostName),
                Multicast.ttl(0),
                CacheConfig.of(CACHE_CONFIG),
                Logging.at(3),
                ClusterName.of(clusterName),
                SystemProperty.of("test.primary.cluster.name", PRIMARY_CLUSTER),
                SystemProperty.of("test.secondary.cluster.name", SECONDARY_CLUSTER),
                ClusterPort.of(clusterPort),
                SystemProperty.of("federation.local.port", federationPortLocal),
                SystemProperty.of("federation.primary.port", federationPortPrimary),
                SystemProperty.of("federation.secondary.port", federationPortSecondary));

        if (mode != null && !mode.isBlank()) {
            optionsByType.add(SystemProperty.of("coherence.mode", mode));
        }

        return optionsByType;
    }

    private static void destroyMember(CoherenceClusterMember member) {
        try {
            if (member != null) {
                member.close();
            }
        }
        catch (Throwable thrown) {
            // ignored
        }
    }
}
