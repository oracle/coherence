/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package ai_tests;

import com.oracle.bedrock.junit.CoherenceClusterExtension;

import com.oracle.bedrock.runtime.coherence.CoherenceCluster;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;

import com.oracle.bedrock.runtime.coherence.options.ClusterName;
import com.oracle.bedrock.runtime.coherence.options.LocalHost;
import com.oracle.bedrock.runtime.coherence.options.RoleName;
import com.oracle.bedrock.runtime.coherence.options.WellKnownAddress;

import com.oracle.bedrock.runtime.java.options.IPv4Preferred;

import com.oracle.bedrock.runtime.options.DisplayName;
import com.oracle.bedrock.runtime.options.StabilityPredicate;

import com.oracle.bedrock.testsupport.junit.TestLogsExtension;

import com.tangosol.net.Coherence;
import com.tangosol.net.Session;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.concurrent.TimeUnit;

public class ClusteredVectorDemoTests
        extends VectorDemoTests
    {
    @BeforeAll
    static void setupCoherence() throws Exception
        {
        System.setProperty("coherence.cluster", CLUSTER_NAME);
        System.setProperty("coherence.wka", "127.0.0.1");
        System.setProperty("coherence.localhost", "127.0.0.1");
        System.setProperty("coherence.ttl", "0");
        System.setProperty("coherence.profile", "thin");
        System.setProperty("coherence.client", "remote");
        System.setProperty("coherence.distributed.localstorage", "false");

        Coherence coherence = Coherence.client(Coherence.Mode.Client).start().get(5, TimeUnit.MINUTES);
        m_session = coherence.getSession();
        }

    @AfterAll
    static void cleanupCoherence()
        {
        Coherence.closeAll();
        }

    @Override
    Session getSession()
        {
        return m_session;
        }


    protected static Session m_session;

    @RegisterExtension
    static final TestLogsExtension m_testLogs = new TestLogsExtension(VectorDemoTests.class);

    @RegisterExtension
    static final CoherenceClusterExtension m_clusterExtension = new CoherenceClusterExtension()
            .with(WellKnownAddress.loopback(),
                  ClusterName.of(CLUSTER_NAME),
                  DisplayName.of("storage"),
                  RoleName.of("storage"),
                  LocalHost.only()
                  )
            .include(3, CoherenceClusterMember.class,
                    StabilityPredicate.of(CoherenceCluster.Predicates.isCoherenceRunning()),
                    IPv4Preferred.autoDetect(),
                    m_testLogs);
    }
