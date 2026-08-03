/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package federation.compatibility;

import com.oracle.bedrock.OptionsByType;
import com.oracle.bedrock.options.Timeout;
import com.oracle.bedrock.runtime.LocalPlatform;
import com.oracle.bedrock.runtime.coherence.CoherenceCacheServer;
import com.oracle.bedrock.runtime.coherence.CoherenceCluster;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterBuilder;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.coherence.JMXManagementMode;
import com.oracle.bedrock.runtime.coherence.options.CacheConfig;
import com.oracle.bedrock.runtime.coherence.options.ClusterName;
import com.oracle.bedrock.runtime.coherence.options.ClusterPort;
import com.oracle.bedrock.runtime.coherence.options.LocalHost;
import com.oracle.bedrock.runtime.coherence.options.LocalStorage;
import com.oracle.bedrock.runtime.coherence.options.Logging;
import com.oracle.bedrock.runtime.coherence.options.Pof;
import com.oracle.bedrock.runtime.coherence.options.RoleName;
import com.oracle.bedrock.runtime.coherence.options.SiteName;
import com.oracle.bedrock.runtime.coherence.options.WellKnownAddress;
import com.oracle.bedrock.runtime.java.options.ClassName;
import com.oracle.bedrock.runtime.java.options.HeapSize;
import com.oracle.bedrock.runtime.java.options.IPv4Preferred;
import com.oracle.bedrock.runtime.java.options.JvmOptions;
import com.oracle.bedrock.runtime.java.options.SystemProperty;
import com.oracle.bedrock.runtime.java.profiles.JmxProfile;
import com.oracle.bedrock.runtime.options.DisplayName;
import com.oracle.bedrock.testsupport.MavenProjectFileUtils;
import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.testsupport.junit.TestLogsExtension;

import com.tangosol.net.DefaultCacheServer;
import com.tangosol.net.NamedCache;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.File;

import java.util.concurrent.TimeUnit;

import static com.oracle.bedrock.deferred.DeferredHelper.within;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Federation mixed-version compatibility tests.
 *
 * @author Aleks Seovic  2026.05.17
 * @since 15.1.2.0
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class FederationCompatibilityIT
    {
    @Test
    public void shouldReplicateFromPreviousOriginToCurrentDestination()
        {
        assertReplicates(Version.Previous, Version.Current);
        }

    @Test
    public void shouldReplicateFromCurrentOriginToPreviousDestination()
        {
        assertReplicates(Version.Current, Version.Previous);
        }

    /**
     * Assert that direct-connect federation replicates from origin to destination.
     *
     * @param originVersion       the origin member version
     * @param destinationVersion  the destination member version
     */
    private static void assertReplicates(Version originVersion, Version destinationVersion)
        {
        int    nBostonPort  = LocalPlatform.get().getAvailablePorts().next();
        int    nNewYorkPort = LocalPlatform.get().getAvailablePorts().next();
        String sSuffix      = originVersion.name().toLowerCase() + "-to-" + destinationVersion.name().toLowerCase()
                + '-' + System.nanoTime();

        try (CoherenceCluster destination = startCluster(NEWYORK, destinationVersion, nNewYorkPort, nBostonPort, sSuffix);
             CoherenceCluster origin      = startCluster(BOSTON, originVersion, nBostonPort, nNewYorkPort, sSuffix))
            {
            CoherenceClusterMember memberOrigin      = origin.getAny();
            CoherenceClusterMember memberDestination = destination.getAny();
            NamedCache             cacheOrigin       = memberOrigin.getCache(CACHE_NAME);
            NamedCache             cacheDestination  = memberDestination.getCache(CACHE_NAME);

            for (int i = 0; i < ENTRY_COUNT; i++)
                {
                cacheOrigin.put(i, "value-" + i);
                }

            Eventually.assertDeferred(cacheDestination::size, is(ENTRY_COUNT), within(5, TimeUnit.MINUTES));
            for (int i = 0; i < ENTRY_COUNT; i++)
                {
                assertThat(cacheDestination.get(i), is("value-" + i));
                }
            }
        }

    /**
     * Start a one-member federation participant cluster.
     *
     * @param sParticipant  the participant and cluster name
     * @param version       the Coherence version to run
     * @param nLocalPort    the local cluster port
     * @param nRemotePort   the remote participant cluster port
     * @param sSuffix       the test suffix used in display names
     *
     * @return the running cluster
     */
    private static CoherenceCluster startCluster(String sParticipant, Version version, int nLocalPort, int nRemotePort,
            String sSuffix)
        {
        File          fileOutput = MavenProjectFileUtils.ensureTestOutputBaseFolder(FederationCompatibilityIT.class);
        OptionsByType options    = OptionsByType.of(
                version.getClassPath(),
                ClusterName.of(sParticipant),
                ClusterPort.of(nLocalPort),
                CacheConfig.of(CACHE_CONFIG),
                ClassName.of(DefaultCacheServer.class),
                DisplayName.of(sParticipant + '-' + version.name() + '-' + sSuffix),
                HeapSize.of(64, HeapSize.Units.MB, 1, HeapSize.Units.GB, true),
                IPv4Preferred.yes(),
                JmxProfile.enabled(),
                JMXManagementMode.ALL,
                JvmOptions.include("-XX:+ExitOnOutOfMemoryError", "-XX:HeapDumpPath=" + fileOutput.getAbsolutePath()),
                LocalHost.only(),
                LocalStorage.enabled(),
                Logging.at(7),
                Pof.config(POF_CONFIG),
                Pof.enabled(),
                RoleName.of("storage"),
                SiteName.of(sParticipant),
                SystemProperty.of("coherence.distribution.2server", false),
                SystemProperty.of("coherence.federation.trace.logging", true),
                SystemProperty.of("coherence.management.http", "none"),
                SystemProperty.of("coherence.override", OPERATIONAL_OVERRIDE),
                SystemProperty.of("coherence.partitions", "17"),
                SystemProperty.of("coherence.reflect.filter", "*"),
                SystemProperty.of("test.federation.port.boston", BOSTON.equals(sParticipant) ? nLocalPort : nRemotePort),
                SystemProperty.of("test.federation.port.newyork", NEWYORK.equals(sParticipant) ? nLocalPort : nRemotePort),
                SystemProperty.of("test.federation.sendtimeout", "60s"),
                SystemProperty.of("test.multicast.port", nLocalPort),
                Timeout.after(5, TimeUnit.MINUTES),
                WellKnownAddress.loopback(),
                m_testLogs);

        CoherenceClusterBuilder builder = new CoherenceClusterBuilder();
        builder.include(1, CoherenceCacheServer.class, options.asArray());
        CoherenceCluster cluster = builder.build();
        Eventually.assertDeferred(cluster::getClusterSize, is(1), within(5, TimeUnit.MINUTES));
        return cluster;
        }

    // ----- constants ------------------------------------------------------

    private static final String BOSTON = "BOSTON";

    private static final String NEWYORK = "NEWYORK";

    private static final String CACHE_NAME = "fed-compat";

    private static final String CACHE_CONFIG = "federation-compatibility-cache-config.xml";

    private static final String OPERATIONAL_OVERRIDE = "federation-compatibility-override.xml";

    private static final String POF_CONFIG = "federation-compatibility-pof-config.xml";

    private static final int ENTRY_COUNT = 32;

    /**
     * A JUnit extension to capture Bedrock process logs under the target folder.
     */
    @RegisterExtension
    @Order(1)
    static final TestLogsExtension m_testLogs = new TestLogsExtension(FederationCompatibilityIT.class);
    }
