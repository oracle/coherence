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
import com.oracle.bedrock.runtime.coherence.options.Logging;
import com.oracle.bedrock.runtime.coherence.options.OperationalOverride;
import com.oracle.bedrock.runtime.coherence.options.Pof;
import com.oracle.bedrock.runtime.coherence.options.RoleName;
import com.oracle.bedrock.runtime.coherence.options.WellKnownAddress;
import com.oracle.bedrock.runtime.java.options.IPv4Preferred;
import com.oracle.bedrock.runtime.java.options.SystemProperty;
import com.oracle.bedrock.runtime.options.DisplayName;
import com.oracle.bedrock.runtime.options.Ports;
import com.oracle.bedrock.runtime.options.StabilityPredicate;
import com.oracle.bedrock.testsupport.junit.TestLogsExtension;
import com.oracle.coherence.grpc.client.common.GrpcRemoteCacheService;
import com.tangosol.coherence.component.util.safeService.SafeCacheService;
import com.tangosol.internal.net.grpc.RemoteGrpcCacheServiceDependencies;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.CacheService;
import com.tangosol.net.Coherence;
import com.tangosol.net.Service;
import com.tangosol.net.Session;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@SuppressWarnings("resource")
public class GrpcRemoteCacheServiceConfigIT
    {
    @BeforeAll
    static void setup()
        {
        System.setProperty("coherence.cluster", CLUSTER_NAME);
        System.setProperty("coherence.ttl", "0");
        System.setProperty("coherence.wka", "127.0.0.1");
        System.setProperty("coherence.localhost", "127.0.0.1");
        System.setProperty("coherence.override", "test-coherence-override.xml");
        }

    @BeforeEach
    void setupEach()
        {
        Coherence.closeAll();
        System.clearProperty("coherence.grpc.request.timeout");
        System.clearProperty("coherence.grpc.heartbeat.interval");
        }

    @Test
    public void shouldHaveDefaults() throws Exception
        {
        System.clearProperty("coherence.grpc.request.timeout");
        System.clearProperty("coherence.grpc.heartbeat.interval");
        Coherence coherence = Coherence.client(Coherence.Mode.Grpc).start().get(5, TimeUnit.MINUTES);
        Session   session   = coherence.getSession();

        CacheService service = (CacheService) session.getService("RemoteGrpcCache");
        if (service instanceof SafeCacheService)
            {
            service = ((SafeCacheService) service).getRunningCacheService();
            }
        assertThat(service, is(notNullValue()));
        assertThat(service, is(instanceOf(GrpcRemoteCacheService.class)));

        GrpcRemoteCacheService             grpcService  = (GrpcRemoteCacheService) service;
        RemoteGrpcCacheServiceDependencies dependencies = grpcService.getDependencies();
        // assert default values
        assertThat(dependencies.getRequestTimeoutMillis(), is(0L));
        assertThat(dependencies.getDeadline(), is(0L));
        assertThat(dependencies.getHeartbeatInterval(), is(0L));
        }

    @Test
    public void shouldHaveDeadline() throws Exception
        {
        long nTimeout = 10000L;
        System.setProperty("coherence.grpc.request.timeout", String.valueOf(nTimeout));

        Coherence coherence = Coherence.client(Coherence.Mode.Grpc).start().get(5, TimeUnit.MINUTES);
        Session   session   = coherence.getSession();

        CacheService service = (CacheService) session.getService("RemoteGrpcCache");
        if (service instanceof SafeCacheService)
            {
            service = ((SafeCacheService) service).getRunningCacheService();
            }
        assertThat(service, is(notNullValue()));
        assertThat(service, is(instanceOf(GrpcRemoteCacheService.class)));

        GrpcRemoteCacheService             grpcService  = (GrpcRemoteCacheService) service;
        RemoteGrpcCacheServiceDependencies dependencies = grpcService.getDependencies();
        assertThat(dependencies.getRequestTimeoutMillis(), is(nTimeout));
        assertThat(dependencies.getDeadline(), is(nTimeout));
        }

    @Test
    public void shouldHaveHeartbeatInterval() throws Exception
        {
        long nInterval = 5000L;
        System.setProperty("coherence.grpc.heartbeat.interval", String.valueOf(nInterval));

        Coherence coherence = Coherence.client(Coherence.Mode.Grpc).start().get(5, TimeUnit.MINUTES);
        Session   session   = coherence.getSession();

        CacheService service = (CacheService) session.getService("RemoteGrpcCache");
        if (service instanceof SafeCacheService)
            {
            service = ((SafeCacheService) service).getRunningCacheService();
            }
        assertThat(service, is(notNullValue()));
        assertThat(service, is(instanceOf(GrpcRemoteCacheService.class)));

        GrpcRemoteCacheService             grpcService  = (GrpcRemoteCacheService) service;
        RemoteGrpcCacheServiceDependencies dependencies = grpcService.getDependencies();
        assertThat(dependencies.getHeartbeatInterval(), is(nInterval));
        }

    // ----- data members ---------------------------------------------------

    static final String CLUSTER_NAME = "DefaultCacheConfigGrpcIT";

    static final int CLUSTER_SIZE = 1;

    @RegisterExtension
    static final TestLogsExtension TEST_LOGS = new TestLogsExtension(DefaultCacheConfigGrpcIT.class);

    @RegisterExtension
    static final CoherenceClusterExtension CLUSTER_EXTENSION = new CoherenceClusterExtension()
            .with(CacheConfig.of("coherence-config.xml"),
                  OperationalOverride.of("test-coherence-override.xml"),
                  WellKnownAddress.loopback(),
                  ClusterName.of(CLUSTER_NAME),
                  DisplayName.of("storage"),
                  RoleName.of("storage"),
                  LocalHost.only(),
                  IPv4Preferred.yes(),
                  StabilityPredicate.of(CoherenceCluster.Predicates.isCoherenceRunning()),
                  TEST_LOGS)
            .include(CLUSTER_SIZE, CoherenceClusterMember.class);
    }
