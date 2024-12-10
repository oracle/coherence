/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package grpc.proxy;

import com.oracle.bedrock.junit.CoherenceClusterExtension;
import com.oracle.bedrock.runtime.LocalPlatform;
import com.oracle.bedrock.runtime.coherence.CoherenceCluster;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.coherence.options.CacheConfig;
import com.oracle.bedrock.runtime.coherence.options.ClusterName;
import com.oracle.bedrock.runtime.coherence.options.LocalHost;
import com.oracle.bedrock.runtime.coherence.options.OperationalOverride;
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
import com.tangosol.discovery.NSLookup;
import com.tangosol.net.grpc.GrpcDependencies;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;

public class GrpcProxyNameServiceLookupIT
    {
    @Test
    public void shouldLookupGrpcProxies() throws Exception
        {
        CoherenceCluster cluster = CLUSTER_EXTENSION.getCluster();

        List<InetSocketAddress> listSocket = new ArrayList<>();
        for (CoherenceClusterMember member : cluster)
            {
            Eventually.assertDeferred(() -> member.invoke(IsGrpcProxyRunning.INSTANCE), is(true));
            Ports ports = member.getOptions().get(Ports.class);
            int nPort = ports.getPort(GrpcDependencies.PROP_PORT).getActualPort();
            listSocket.add(new InetSocketAddress("127.0.0.1", nPort));
            }


        InetSocketAddress         address = new InetSocketAddress("127.0.0.1", 7574);
        Collection<SocketAddress> colAddr = NSLookup.lookupGrpcProxy(CLUSTER_NAME, address);
        assertThat(colAddr, is(notNullValue()));
        assertThat(colAddr.size(), is(cluster.getClusterSize()));
        }

    // ----- data members ---------------------------------------------------

    static final String CLUSTER_NAME = "GrpcNSLookupIT";

    static final LocalPlatform PLATFORM = LocalPlatform.get();

    static final AvailablePortIterator GRPC_PORTS = PLATFORM.getAvailablePorts();

    static final int CLUSTER_SIZE = 3;

    @RegisterExtension
    static final TestLogsExtension m_testLogs = new TestLogsExtension(GrpcProxyNameServiceLookupIT.class);

    @RegisterExtension
    static final CoherenceClusterExtension CLUSTER_EXTENSION = new CoherenceClusterExtension()
            .with(CacheConfig.of("coherence-config.xml"),
                    OperationalOverride.of("test-coherence-override.xml"),
                    SystemProperty.of(GrpcDependencies.PROP_PORT, GRPC_PORTS, Ports.capture()),
                    WellKnownAddress.loopback(),
                    ClusterName.of(CLUSTER_NAME),
                    DisplayName.of("storage"),
                    RoleName.of("storage"),
                    LocalHost.only(),
                    IPv4Preferred.yes(),
                    StabilityPredicate.of(CoherenceCluster.Predicates.isCoherenceRunning()),
                    m_testLogs)
            .include(CLUSTER_SIZE, CoherenceClusterMember.class);
    }
