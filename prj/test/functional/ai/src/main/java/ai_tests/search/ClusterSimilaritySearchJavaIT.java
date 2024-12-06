/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package ai_tests.search;

import com.oracle.bedrock.junit.CoherenceClusterExtension;
import com.oracle.bedrock.junit.SessionBuilders;
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
import com.oracle.bedrock.runtime.options.StabilityPredicate;
import com.oracle.bedrock.testsupport.junit.TestLogsExtension;
import com.oracle.coherence.ai.DocumentChunk;
import com.tangosol.net.Coherence;
import com.tangosol.net.NamedMap;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.RegisterExtension;

public class ClusterSimilaritySearchJavaIT
        extends BaseSimilaritySearchIT
    {
    @BeforeAll
    static void setup() throws Exception
        {
        String sAddress = "127.0.0.1";
        System.setProperty("coherence.wka", sAddress);
        System.setProperty("coherence.localhost", sAddress);
        System.setProperty("test.unicast.address", sAddress);
        System.setProperty("test.unicast.port", "0");
        System.setProperty("coherence.ttl", "0");

        System.setProperty("coherence.distributed.partitioncount", "13");

        m_session = CLUSTER_EXTENSION.buildSession(SessionBuilders.storageDisabledMember());

        NamedMap<Integer, DocumentChunk> vectors = m_session.getMap("vectors");
        m_valueZero = populateVectors(vectors);
        }

    @AfterAll
    static void cleanup() throws Exception
        {
        Coherence.closeAll();
        CLUSTER_EXTENSION.afterAll(null);
        }


    static final String CLUSTER_NAME = "ClusterSimilaritySearchJavaIT";

    static final int CLUSTER_SIZE = 3;

    @RegisterExtension
    static final TestLogsExtension TEST_LOGS = new TestLogsExtension(ClusterSimilaritySearchJavaIT.class);

    @RegisterExtension
    static final CoherenceClusterExtension CLUSTER_EXTENSION = new CoherenceClusterExtension()
            .with(SystemProperty.of("coherence.serializer", "java"),
                  WellKnownAddress.loopback(),
                  ClusterName.of(CLUSTER_NAME),
                  DisplayName.of("storage"),
                  RoleName.of("storage"),
                  Logging.atMax(),
                  LocalHost.only(),
                  IPv4Preferred.autoDetect(),
                  StabilityPredicate.of(CoherenceCluster.Predicates.isCoherenceRunning()),
                  TEST_LOGS)
            .include(CLUSTER_SIZE, CoherenceClusterMember.class);
    }
