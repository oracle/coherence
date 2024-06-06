/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package bootstrap;

import com.oracle.bedrock.runtime.LocalPlatform;

import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.coherence.ServiceStatus;

import com.oracle.bedrock.runtime.coherence.options.LocalHost;
import com.oracle.bedrock.runtime.coherence.options.Logging;

import com.oracle.bedrock.runtime.java.options.IPv4Preferred;
import com.oracle.bedrock.runtime.java.options.SystemProperty;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.bedrock.testsupport.junit.TestLogsExtension;

import com.oracle.bedrock.util.Capture;

import com.tangosol.coherence.component.net.extend.remoteService.RemoteCacheService;

import com.tangosol.coherence.component.util.SafeService;

import com.tangosol.net.Coherence;
import com.tangosol.net.NamedCache;
import com.tangosol.net.Service;
import com.tangosol.net.Session;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.RegisterExtension;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * This class tests that the Coherence is started as an Extend client
 * when the bootstrap API starts Coherence as a client.
 */
public class ExtendClientTests
    {
    @Test
    public void shouldUseNameService()
        {
        LocalPlatform platform = LocalPlatform.get();

        try (CoherenceClusterMember member = platform.launch(CoherenceClusterMember.class,
                                                             IPv4Preferred.yes(),
                                                             LocalHost.only(),
                                                             Logging.atMax(),
                                                             m_testLogs))
            {
            Eventually.assertDeferred(member::isReady, is(true));
            Eventually.assertDeferred(() -> member.getServiceStatus("Proxy"), is(ServiceStatus.RUNNING));

            try (CoherenceClusterMember client = platform.launch(CoherenceClusterMember.class,
                                                                 SystemProperty.of("coherence.client", "remote"),
                                                                 IPv4Preferred.yes(),
                                                                 LocalHost.only(),
                                                                 Logging.atMax(),
                                                                 m_testLogs))
                {
                assertRemoteClient(client);
                }
            }
        }

    @Test
    public void shouldUseFixedAddresses()
        {
        LocalPlatform platform = LocalPlatform.get();
        Capture<Integer> nPort = new Capture<>(platform.getAvailablePorts());

        try (CoherenceClusterMember member = platform.launch(CoherenceClusterMember.class,
                                                             SystemProperty.of("coherence.extend.port", nPort),
                                                             IPv4Preferred.yes(),
                                                             LocalHost.only(),
                                                             m_testLogs))
            {
            Eventually.assertDeferred(member::isReady, is(true));
            Eventually.assertDeferred(() -> member.getServiceStatus("Proxy"), is(ServiceStatus.RUNNING));

            try (CoherenceClusterMember client = platform.launch(CoherenceClusterMember.class,
                                                                 SystemProperty.of("coherence.client", "remote-fixed"),
                                                                 SystemProperty.of("coherence.extend.address", "127.0.0.1"),
                                                                 SystemProperty.of("coherence.extend.port", nPort),
                                                                 IPv4Preferred.yes(),
                                                                 LocalHost.only(),
                                                                 m_testLogs))
                {
                assertRemoteClient(client);
                }
            }
        }

    private void assertRemoteClient(CoherenceClusterMember client)
        {
        Eventually.assertDeferred(client::isCoherenceRunning, is(true));

        client.invoke(() ->
            {
            Session session = Coherence.getInstance().getSession();
            NamedCache<?, ?> cache = session.getCache("test");
            Service cacheService = cache.getService();
            if (cacheService instanceof SafeService)
                {
                cacheService = ((SafeService) cacheService).getService();
                }
            assertThat(cacheService, is(instanceOf(RemoteCacheService.class)));
            return null;
            });
        }

    @RegisterExtension
    public static final TestLogsExtension m_testLogs = new TestLogsExtension(ExtendClientTests.class);
    }
