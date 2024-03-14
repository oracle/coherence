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

import com.tangosol.net.Coherence;
import com.tangosol.net.NamedDeque;
import com.tangosol.net.Session;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;


@SuppressWarnings({"rawtypes"})
public class ClusteredDequeTests<QueueType extends NamedDeque>
        extends AbstractDequeTests<QueueType>
    {
    @BeforeAll
    static void setup()  throws Exception
        {
        System.setProperty("coherence.cluster",     CLUSTER_NAME);
        System.setProperty(LocalStorage.PROPERTY,   "false");
        System.setProperty("coherence.ttl",         "0");
        System.setProperty("coherence.wka",         "127.0.0.1");
        System.setProperty("coherence.localhost",   "127.0.0.1");
        System.setProperty("coherence.cacheconfig", "queue-cache-config.xml");

        for (CoherenceClusterMember member : m_cluster.getCluster())
            {
            Eventually.assertDeferred(member::isCoherenceRunning, is(true));
            }

        Coherence coherence = Coherence.clusterMember().start().get(5, TimeUnit.MINUTES);
        m_session = coherence.getSession();
        }

    @Override
    public Session getSession()
        {
        return m_session;
        }

    // ----- data members ---------------------------------------------------

    public static final String CLUSTER_NAME = "ClusteredDequeTests";

    private static Session m_session;

    @RegisterExtension
    static final TestLogsExtension m_logs = new TestLogsExtension(ClusteredDequeTests.class);

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
