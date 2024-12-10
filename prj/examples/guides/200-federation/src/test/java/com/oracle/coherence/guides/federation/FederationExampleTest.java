/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
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

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class FederationExampleTest {

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

        OptionsByType primaryClusterOptions = createCacheServerOptions("PrimaryCluster", primaryClusterPort, primaryClusterPort,
                secondaryClusterPort);
        OptionsByType secondaryClusterOptions = createCacheServerOptions("SecondaryCluster", secondaryClusterPort,
                primaryClusterPort,
                secondaryClusterPort);

        primaryMember = platform.launch(CoherenceCacheServer.class, primaryClusterOptions.asArray());
        secondaryMember = platform.launch(CoherenceCacheServer.class, secondaryClusterOptions.asArray());


        Eventually.assertThat(invoking(primaryMember).getClusterSize(), is(1));
        Eventually.assertThat(invoking(secondaryMember).getClusterSize(), is(1));
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

        assertEquals(ncPrimary.size(), 0);
        assertEquals(ncSecondary.size(), 0);

        Map<Integer, String> buffer = new HashMap<>();
        for (int i = 0; i < COUNT; i++) {
            buffer.put(i, "Value-" + i);
        }

        // Add data to primary cluster
        ncPrimary.putAll(buffer);
        assertEquals(ncPrimary.size(), COUNT);

        // wait for data to reach secondary
        Eventually.assertDeferred(ncSecondary::size, is(COUNT));

        // clear the data in secondary and wait for primary to be 0
        ncSecondary.clear();

        Eventually.assertDeferred(ncPrimary::size, is(0));
    }

    protected static OptionsByType createCacheServerOptions(String clusterName, int clusterPort,
                                                            int federationPortPrimary, int federationPortSecondary) {
        String        hostName      = LocalPlatform.get().getLoopbackAddress().getHostAddress();
        OptionsByType optionsByType = OptionsByType.empty();

        optionsByType.addAll(JMXManagementMode.ALL,
                JmxProfile.enabled(),
                LocalStorage.enabled(),
                WellKnownAddress.of(hostName),
                Multicast.ttl(0),
                CacheConfig.of(CACHE_CONFIG),
                Logging.at(3),
                ClusterName.of(clusterName),
                ClusterPort.of(clusterPort),
                SystemProperty.of("primary.cluster.port", Integer.toString(federationPortPrimary)),
                SystemProperty.of("secondary.cluster.port", Integer.toString(federationPortSecondary)),
                SystemProperty.of("primary.cluster.host", hostName),
                SystemProperty.of("secondary.cluster.host", hostName));

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
