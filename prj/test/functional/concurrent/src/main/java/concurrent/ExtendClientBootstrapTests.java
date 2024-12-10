/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package concurrent;

import com.oracle.bedrock.junit.CoherenceClusterExtension;

import com.oracle.bedrock.runtime.LocalPlatform;

import com.oracle.bedrock.runtime.coherence.CoherenceCluster;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;

import com.oracle.bedrock.runtime.coherence.options.ClusterName;
import com.oracle.bedrock.runtime.coherence.options.LocalHost;
import com.oracle.bedrock.runtime.coherence.options.Logging;
import com.oracle.bedrock.runtime.coherence.options.RoleName;
import com.oracle.bedrock.runtime.coherence.options.WellKnownAddress;

import com.oracle.bedrock.runtime.java.options.IPv4Preferred;
import com.oracle.bedrock.runtime.java.options.SystemProperty;

import com.oracle.bedrock.runtime.options.DisplayName;
import com.oracle.bedrock.runtime.options.Ports;
import com.oracle.bedrock.runtime.options.StabilityPredicate;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.bedrock.testsupport.junit.TestLogsExtension;

import com.oracle.coherence.concurrent.config.ConcurrentServicesSessionConfiguration;

import com.tangosol.coherence.component.net.extend.remoteService.RemoteCacheService;

import com.tangosol.coherence.component.util.SafeService;

import com.tangosol.net.Coherence;
import com.tangosol.net.NamedCache;
import com.tangosol.net.Service;
import com.tangosol.net.Session;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Iterator;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * This class tests that the Concurrent caches are started as Extend clients
 * when the bootstrap API starts Coherence as a client.
 */
public class ExtendClientBootstrapTests
    {
    @Test
    public void shouldUseNameService()
        {
        LocalPlatform platform = LocalPlatform.get();

        try (CoherenceClusterMember client = platform.launch(CoherenceClusterMember.class,
                                                             ClusterName.of(CLUSTER_NAME),
                                                             SystemProperty.of("coherence.client", "remote"),
                                                             IPv4Preferred.yes(),
                                                             LocalHost.only(),
                                                             WellKnownAddress.loopback(),
                                                             Logging.atMax(),
                                                             m_testLogs))
            {
            assertRemoteClient(client);
            }
        }


    @Test
    public void shouldUseFixedAddresses()
        {
        LocalPlatform          platform        = LocalPlatform.get();
        CoherenceClusterMember member          = m_clusterExtension.getCluster().getAny();
        int                    nPort           = member.getExtendProxyPort();
        int                    nConcurrentPort = Integer.valueOf(member.getSystemProperty("coherence.concurrent.extend.port"));

        try (CoherenceClusterMember client = platform.launch(CoherenceClusterMember.class,
                                                             SystemProperty.of("coherence.client", "remote-fixed"),
                                                             SystemProperty.of("coherence.extend.address", "127.0.0.1"),
                                                             SystemProperty.of("coherence.extend.port", nPort),
                                                             SystemProperty.of("coherence.concurrent.extend.address", "127.0.0.1"),
                                                             SystemProperty.of("coherence.concurrent.extend.port", nConcurrentPort),
                                                             IPv4Preferred.yes(),
                                                             LocalHost.only(),
                                                             m_testLogs))
            {
            assertRemoteClient(client);
            }
        }

    private void assertRemoteClient(CoherenceClusterMember client)
        {
        Eventually.assertDeferred(client::isCoherenceRunning, is(true));

        client.invoke(() ->
            {
            Session session = Coherence.getInstance().getSession(ConcurrentServicesSessionConfiguration.SESSION_NAME);
            NamedCache<?, ?> cache = session.getCache("atomic-test");
            Service cacheService = cache.getService();
            if (cacheService instanceof SafeService)
                {
                cacheService = ((SafeService) cacheService).getService();
                }
            assertThat(cacheService, is(instanceOf(RemoteCacheService.class)));
            return null;
            });
        }

    public static final String CLUSTER_NAME = "ExtendClientBootstrapTestsCluster";

    static final LocalPlatform PLATFORM = LocalPlatform.get();

    static final Iterator<Integer> PORTS = PLATFORM.getAvailablePorts();

    @RegisterExtension
    public static final TestLogsExtension m_testLogs = new TestLogsExtension(ExtendClientBootstrapTests.class);

    @RegisterExtension
    public static final CoherenceClusterExtension m_clusterExtension = new CoherenceClusterExtension()
            .with(
                    SystemProperty.of("coherence.extend.port", PORTS, Ports.capture()),
                    SystemProperty.of("coherence.concurrent.extend.port", PORTS, Ports.capture()),
                    WellKnownAddress.loopback(),
                    ClusterName.of(CLUSTER_NAME),
                    DisplayName.of("storage"),
                    RoleName.of("storage"),
                    Logging.atMax(),
                    LocalHost.only(),
                    IPv4Preferred.yes(),
                    StabilityPredicate.of(CoherenceCluster.Predicates.isCoherenceRunning()),
                    m_testLogs)
            .include(1, CoherenceClusterMember.class);

    }
