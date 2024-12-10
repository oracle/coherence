/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package queues;

import com.oracle.bedrock.junit.CoherenceClusterExtension;

import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.coherence.options.CacheConfig;
import com.oracle.bedrock.runtime.coherence.options.ClusterName;
import com.oracle.bedrock.runtime.coherence.options.LocalHost;
import com.oracle.bedrock.runtime.coherence.options.LocalStorage;
import com.oracle.bedrock.runtime.coherence.options.RoleName;
import com.oracle.bedrock.runtime.coherence.options.WellKnownAddress;

import com.oracle.bedrock.runtime.options.DisplayName;

import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.testsupport.junit.TestLogsExtension;


import com.oracle.coherence.grpc.client.common.GrpcRemoteCacheService;
import com.tangosol.coherence.component.util.safeService.SafeCacheService;

import com.tangosol.net.CacheService;
import com.tangosol.net.Coherence;
import com.tangosol.net.NamedDeque;
import com.tangosol.net.NamedMap;
import com.tangosol.net.Session;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;


@SuppressWarnings({"rawtypes", "resource"})
public class GrpcClientDequeTests<QueueType extends NamedDeque>
        extends AbstractDequeTests<QueueType>
    {
    @BeforeAll
    static void setup() throws Exception
        {
        System.setProperty("coherence.cluster",     CLUSTER_NAME);
        System.setProperty("coherence.ttl",         "0");
        System.setProperty("coherence.wka",         "127.0.0.1");
        System.setProperty("coherence.localhost",   "127.0.0.1");
        System.setProperty("coherence.profile",     "thin");
        System.setProperty("coherence.client",      "grpc");
        System.setProperty("coherence.cacheconfig", "queue-cache-config.xml");

        for (CoherenceClusterMember member : m_cluster.getCluster())
            {
            Eventually.assertDeferred(member::isCoherenceRunning, is(true));
            }

        m_coherence = Coherence.client().start().get(5, TimeUnit.MINUTES);
        }

    @Override
    public Session getSession()
        {
        return m_coherence.getSession();
        }

    @Test
    public void shouldBeRemoteClient()
        {
        NamedMap     test    = getCollectionCache("test");
        CacheService service = test.getService();
        if (service instanceof SafeCacheService)
            {
            service = ((SafeCacheService) service).getRunningCacheService();
            }
        assertThat(service, is(instanceOf(GrpcRemoteCacheService.class)));
        }

    // ----- data members ---------------------------------------------------

    public static final String CLUSTER_NAME = "GrpcClientDequeTests";

    protected static Coherence m_coherence;

    @RegisterExtension
    static final TestLogsExtension m_logs = new TestLogsExtension(GrpcClientDequeTests.class);

    @RegisterExtension
    static final CoherenceClusterExtension m_cluster = new CoherenceClusterExtension()
            .with(WellKnownAddress.loopback(),
                  CacheConfig.of("queue-cache-config.xml"),
                  LocalHost.only(),
                  ClusterName.of(CLUSTER_NAME))
            .include(3, CoherenceClusterMember.class,
                    LocalStorage.enabled(),
                    RoleName.of("storage"),
                    DisplayName.of("storage"),
                    m_logs);
    }
