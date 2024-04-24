/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package grpc.client;

import com.oracle.bedrock.junit.CoherenceClusterExtension;
import com.oracle.bedrock.runtime.LocalPlatform;
import com.oracle.bedrock.runtime.coherence.CoherenceCluster;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.coherence.options.CacheConfig;
import com.oracle.bedrock.runtime.coherence.options.ClusterName;
import com.oracle.bedrock.runtime.coherence.options.LocalHost;
import com.oracle.bedrock.runtime.coherence.options.OperationalOverride;
import com.oracle.bedrock.runtime.coherence.options.Pof;
import com.oracle.bedrock.runtime.coherence.options.RoleName;
import com.oracle.bedrock.runtime.coherence.options.WellKnownAddress;
import com.oracle.bedrock.runtime.java.options.IPv4Preferred;
import com.oracle.bedrock.runtime.java.options.SystemProperty;
import com.oracle.bedrock.runtime.network.AvailablePortIterator;
import com.oracle.bedrock.runtime.options.DisplayName;
import com.oracle.bedrock.runtime.options.Ports;
import com.oracle.bedrock.runtime.options.StabilityPredicate;
import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.testsupport.junit.TestLogsExtension;
import com.oracle.coherence.grpc.client.common.GrpcRemoteCacheService;
import com.tangosol.coherence.component.util.safeService.SafeCacheService;
import com.tangosol.net.CacheService;
import com.tangosol.net.Coherence;
import com.tangosol.net.CoherenceConfiguration;
import com.tangosol.net.NamedCache;
import com.tangosol.net.Session;
import com.tangosol.net.SessionConfiguration;
import com.tangosol.net.grpc.GrpcDependencies;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class GrpcNameServiceLookupIT
    {
    @BeforeAll
    static void setup() throws Exception
        {
        CoherenceClusterMember clusterMember = CLUSTER_EXTENSION.getCluster().getAny();

        Eventually.assertDeferred(() -> clusterMember.isServiceRunning(GrpcDependencies.SCOPED_PROXY_SERVICE_NAME), is(true));

        int grpcPort = clusterMember.getGrpcProxyPort();

        System.setProperty("coherence.wka", "127.0.0.1");
        System.setProperty("coherence.override", "test-coherence-override.xml");
        System.setProperty("coherence.cluster", CLUSTER_NAME);
        System.setProperty("coherence.serializer", "pof");
        System.setProperty("coherence.pof.config", "test-pof-config.xml");
        System.setProperty("coherence.grpc.channels.default.host", "127.0.0.1");
        System.setProperty("coherence.grpc.channels.default.port", "7574");
        System.setProperty("coherence.grpc.ns.host", "127.0.0.1");
        System.setProperty("coherence.grpc.ns.port", "7574");
        System.setProperty("coherence.grpc.address", "127.0.0.1");
        System.setProperty("coherence.grpc.port", String.valueOf(grpcPort));

        CoherenceConfiguration cfg = CoherenceConfiguration.builder()
                .withSession(SessionConfiguration.create("grpc-ns-lookup-cache-config.xml"))
                .build();

        Coherence coherence = Coherence.client(cfg).start().get(5, TimeUnit.MINUTES);
        m_session = coherence.getSession();
        assertThat(m_session, is(notNullValue()));
        }

    @AfterAll
    static void cleanup()
        {
        Coherence.closeAll();
        }

    @Test
    public void shouldUseFixedAddressChannel()
        {
        NamedCache<String, String> cache   = m_session.getCache("fixed");
        CacheService               service = cache.getCacheService();
        if (service instanceof SafeCacheService)
            {
            service = ((SafeCacheService) service).getRunningCacheService();
            }
        assertThat(service, is(instanceOf(GrpcRemoteCacheService.class)));
        cache.put("key-1", "value-1");
        assertThat(cache.get("key-1"), is("value-1"));
        }

    @Test
    public void shouldUseDefaultLookupChannel()
        {
        NamedCache<String, String> cache   = m_session.getCache("default");
        CacheService               service = cache.getCacheService();
        if (service instanceof SafeCacheService)
            {
            service = ((SafeCacheService) service).getRunningCacheService();
            }
        assertThat(service, is(instanceOf(GrpcRemoteCacheService.class)));
        cache.put("key-1", "value-1");
        assertThat(cache.get("key-1"), is("value-1"));
        }

    @Test
    public void shouldUseEmptyLookupChannel()
        {
        NamedCache<String, String> cache   = m_session.getCache("empty");
        CacheService               service = cache.getCacheService();
        if (service instanceof SafeCacheService)
            {
            service = ((SafeCacheService) service).getRunningCacheService();
            }
        assertThat(service, is(instanceOf(GrpcRemoteCacheService.class)));
        cache.put("key-1", "value-1");
        assertThat(cache.get("key-1"), is("value-1"));
        }


    @Test
    public void shouldUseInlineLookupChannel()
        {
        NamedCache<String, String> cache   = m_session.getCache("inline");
        CacheService               service = cache.getCacheService();
        if (service instanceof SafeCacheService)
            {
            service = ((SafeCacheService) service).getRunningCacheService();
            }
        assertThat(service, is(instanceOf(GrpcRemoteCacheService.class)));
        cache.put("key-1", "value-1");
        assertThat(cache.get("key-1"), is("value-1"));
        }

    // ----- data members ---------------------------------------------------


    static final String CLUSTER_NAME = "GrpcNameServiceLookupIT";

    static final LocalPlatform PLATFORM = LocalPlatform.get();

    static final AvailablePortIterator PORTS = PLATFORM.getAvailablePorts();

    static final int CLUSTER_SIZE = 3;

    @RegisterExtension
    static final TestLogsExtension TEST_LOGS = new TestLogsExtension(DefaultCacheConfigGrpcIT.class);

    @RegisterExtension
    static final CoherenceClusterExtension CLUSTER_EXTENSION = new CoherenceClusterExtension()
            .with(CacheConfig.of("coherence-cache-config.xml"),
                  OperationalOverride.of("test-coherence-override.xml"),
                  Pof.config("test-pof-config.xml"),
                  SystemProperty.of("coherence.serializer", "pof"),
                  SystemProperty.of("coherence.extend.port", PORTS, Ports.capture()),
                  WellKnownAddress.loopback(),
                  ClusterName.of(CLUSTER_NAME),
                  DisplayName.of("storage"),
                  RoleName.of("storage"),
                  LocalHost.only(),
                  IPv4Preferred.yes(),
                  StabilityPredicate.of(CoherenceCluster.Predicates.isCoherenceRunning()),
                  TEST_LOGS)
            .include(CLUSTER_SIZE, CoherenceClusterMember.class);

    private static Session m_session;
    }
