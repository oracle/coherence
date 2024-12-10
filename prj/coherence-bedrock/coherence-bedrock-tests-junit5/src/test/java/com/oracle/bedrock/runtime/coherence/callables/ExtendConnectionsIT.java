/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.bedrock.runtime.coherence.callables;

import com.oracle.bedrock.runtime.LocalPlatform;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.coherence.options.ClusterName;
import com.oracle.bedrock.runtime.coherence.options.LocalHost;
import com.oracle.bedrock.runtime.coherence.options.RoleName;
import com.oracle.bedrock.runtime.concurrent.RemoteCallable;
import com.oracle.bedrock.runtime.java.options.ClassName;
import com.oracle.bedrock.runtime.java.options.IPv4Preferred;
import com.oracle.bedrock.runtime.java.options.SystemProperty;
import com.oracle.bedrock.runtime.options.DisplayName;
import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.testsupport.junit.TestLogsExtension;
import com.tangosol.net.Coherence;
import com.tangosol.net.Member;
import com.tangosol.net.NamedCache;
import com.tangosol.net.Session;
import com.tangosol.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ExtendConnectionsIT
    {
    @Test
    public void shouldCountConnections()
        {
        LocalPlatform platform     = LocalPlatform.get();
        String        sCluster     = getClass().getSimpleName();
        String        sServiceName = "Proxy";

        try (CoherenceClusterMember server = platform.launch(CoherenceClusterMember.class,
                                                             ClassName.of(Coherence.class),
                                                             LocalHost.only(),
                                                             IPv4Preferred.yes(),
                                                             ClusterName.of(sCluster),
                                                             RoleName.of("server"),
                                                             m_logs,
                                                             DisplayName.of("server")))
            {
            Eventually.assertDeferred(server::isCoherenceRunning, is(true));
            Eventually.assertDeferred(() -> server.isServiceRunning(sServiceName), is(true));

            GetExtendConnectionCount getConnectionCount = new GetExtendConnectionCount(sServiceName);

            assertThat(server.getExtendConnectionCount(sServiceName), is(0));

            UUID uuid1;
            UUID uuid2;

            try (CoherenceClusterMember client1 = platform.launch(CoherenceClusterMember.class,
                                                                  ClassName.of(Coherence.class),
                                                                  LocalHost.only(),
                                                                  IPv4Preferred.yes(),
                                                                  SystemProperty.of("coherence.client", "remote"),
                                                                  ClusterName.of(sCluster),
                                                                  RoleName.of("client"),
                                                                  m_logs,
                                                                  DisplayName.of("client-1")))
                {
                Eventually.assertDeferred(client1::isCoherenceRunning, is(true));

                uuid1 = client1.invoke(new Connect());

                assertThat(server.getExtendConnectionCount(sServiceName), is(1));
                assertThat(server.hasExtendConnection(sServiceName, uuid1), is(true));

                try (CoherenceClusterMember client2 = platform.launch(CoherenceClusterMember.class,
                                                                      ClassName.of(Coherence.class),
                                                                      LocalHost.only(),
                                                                      IPv4Preferred.yes(),
                                                                      SystemProperty.of("coherence.client", "remote"),
                                                                      ClusterName.of(sCluster),
                                                                      RoleName.of("client"),
                                                                      m_logs,
                                                                      DisplayName.of("client-2")))
                    {
                    Eventually.assertDeferred(client2::isCoherenceRunning, is(true));

                    uuid2 = client2.invoke(new Connect());

                    assertThat(server.getExtendConnectionCount(sServiceName), is(2));
                    assertThat(server.hasExtendConnection(sServiceName, uuid1), is(true));
                    assertThat(server.hasExtendConnection(sServiceName, uuid2), is(true));
                    }
                // client-2 closed on exit of try block

                Eventually.assertDeferred(() -> server.getExtendConnectionCount(sServiceName), is(1));
                assertThat(server.hasExtendConnection(sServiceName, uuid1), is(true));
                assertThat(server.hasExtendConnection(sServiceName, uuid2), is(false));
                }
            // client-1 closed on exit of try block

            Eventually.assertDeferred(() -> server.getExtendConnectionCount(sServiceName), is(0));
            assertThat(server.hasExtendConnection(sServiceName, uuid1), is(false));
            assertThat(server.hasExtendConnection(sServiceName, uuid2), is(false));
            }
        }

    public static class Connect
            implements RemoteCallable<UUID>
        {
        @Override
        public UUID call()
            {
            Session                    session = Coherence.getInstance().getSession();
            NamedCache<Object, Object> cache   = session.getCache("test");
            Member                     member  = cache.getCacheService().getInfo().getServiceMember(0);
            return member.getUuid();
            }
        }

    @RegisterExtension
    public static final TestLogsExtension m_logs = new TestLogsExtension(ExtendConnectionsIT.class);
    }
