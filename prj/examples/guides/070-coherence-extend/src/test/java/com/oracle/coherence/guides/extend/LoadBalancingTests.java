/*
 * Copyright (c) 2022 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.guides.extend;

import com.oracle.bedrock.runtime.LocalPlatform;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.coherence.callables.IsServiceRunning;
import com.oracle.bedrock.runtime.coherence.options.CacheConfig;
import com.oracle.bedrock.runtime.coherence.options.ClusterName;
import com.oracle.bedrock.runtime.coherence.options.LocalHost;
import com.oracle.bedrock.runtime.coherence.options.Logging;
import com.oracle.bedrock.runtime.coherence.options.RoleName;
import com.oracle.bedrock.runtime.concurrent.RemoteCallable;
import com.oracle.bedrock.runtime.java.options.ClassName;
import com.oracle.bedrock.runtime.java.options.IPv4Preferred;
import com.oracle.bedrock.runtime.java.options.SystemProperty;
import com.oracle.bedrock.runtime.options.DisplayName;
import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.tangosol.net.Coherence;
import com.tangosol.net.Member;
import com.tangosol.net.NamedCache;
import com.tangosol.net.Session;
import com.tangosol.util.UUID;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * A test class showing the usage of Coherence*Extend for the load-balancing use-case.
 *
 * @author Gunnar Hillert  2022.09.22
 */
class LoadBalancingTests {
    // # tag::testLoadBalancingUseCase[]
    @Test
    void testCoherenceExtendConnection() throws InterruptedException {

        LocalPlatform platform = LocalPlatform.get();

        int numberOfServers = 4; // <1>
        int numberOfClients = 3; // <2>

        List<CoherenceClusterMember> servers = new ArrayList<>(numberOfServers);
        List<CoherenceClusterMember> clients = new ArrayList<>(numberOfClients);

        try {
            for (int i = 1; i <= numberOfServers; i++) {
                CoherenceClusterMember server = platform.launch(CoherenceClusterMember.class,
                        CacheConfig.of("load-balancing/server-coherence-cache-config.xml"), // <3>
                        ClassName.of(Coherence.class),
                        LocalHost.only(),
                        Logging.atInfo(),
                        IPv4Preferred.yes(),
                        ClusterName.of("myCluster"),
                        RoleName.of("server"),
                        SystemProperty.of("coherence.log.level", "5"),
                        DisplayName.of("server-" + i));
                servers.add(server);
            }

            for (CoherenceClusterMember server : servers) {
                Eventually.assertDeferred(() -> server.invoke(
                        new IsServiceRunning("MyCountryExtendService")), is(true)); // <4>
                assertThat(server.getExtendConnectionCount("MyCountryExtendService"), is(0)); // <5>
            }

            for (int i = 1; i <= numberOfClients; i++) {
                CoherenceClusterMember client = platform.launch(CoherenceClusterMember.class,
                        CacheConfig.of("load-balancing/client-coherence-cache-config.xml"),  // <6>
                        ClassName.of(Coherence.class),
                        LocalHost.only(),
                        Logging.atInfo(),
                        IPv4Preferred.yes(),
                        SystemProperty.of("coherence.client", "remote"),
                        SystemProperty.of("coherence.tcmp.enabled", "false"),
                        SystemProperty.of("coherence.log.level", "5"),
                        ClusterName.of("myCluster"),
                        RoleName.of("client"),
                        DisplayName.of("client-" + i));
                clients.add(client);
            }

            for (CoherenceClusterMember client : clients) {
                Eventually.assertDeferred(client::isCoherenceRunning, is(true)); // <7>
                client.invoke(new Connect()); // <8>
            }

            int clientCount = servers.stream()
                    .map(server -> server.getExtendConnectionCount("MyCountryExtendService"))
                    .reduce(0, Integer::sum);  // <9>

            assertThat(clientCount, is(3));  // <10>

            TimeUnit.MILLISECONDS.sleep(20000);  // <11>
        } finally {
            for (CoherenceClusterMember client : clients) {
                client.close();
            }
            for (CoherenceClusterMember server : servers) {
                server.close();
            }
        }
    }
    // # end::testLoadBalancingUseCase[]

    // # tag::testLoadBalancingUseCaseConnect[]
    public static class Connect implements RemoteCallable<UUID> { // <1>
        @Override
        public UUID call() {
            Session session = Coherence.getInstance().getSession(); // <2>
            NamedCache<Object, Object> cache = session.getCache("countries"); // <3>
            Member member = cache.getCacheService().getInfo().getServiceMember(0);
            return member.getUuid();
        }
    }
    // # end::testLoadBalancingUseCaseConnect[]
}
