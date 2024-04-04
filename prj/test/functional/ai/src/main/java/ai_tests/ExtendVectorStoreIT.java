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
import com.oracle.bedrock.runtime.coherence.options.LocalStorage;
import com.oracle.bedrock.runtime.coherence.options.Logging;
import com.oracle.bedrock.runtime.coherence.options.RoleName;
import com.oracle.bedrock.runtime.coherence.options.WellKnownAddress;

import com.oracle.bedrock.runtime.java.options.IPv4Preferred;

import com.oracle.bedrock.runtime.options.DisplayName;
import com.oracle.bedrock.runtime.options.StabilityPredicate;

import com.oracle.bedrock.testsupport.junit.TestLogsExtension;

import com.oracle.coherence.ai.VectorStore;

import com.tangosol.coherence.component.net.extend.RemoteService;

import com.tangosol.coherence.component.util.SafeService;

import com.tangosol.net.Coherence;
import com.tangosol.net.Service;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@SuppressWarnings("resource")
public class ExtendVectorStoreIT
        extends BaseVectorStoreIT
    {
    @BeforeAll
    static void startCoherence() throws Exception
        {
        System.setProperty("coherence.ttl",     "0");
        System.setProperty("coherence.wka",     "127.0.0.1");
        System.setProperty("coherence.cluster", CLUSTER_NAME);
        System.setProperty(LocalStorage.PROPERTY, "false");

        Coherence.client(Coherence.Mode.Client).start().get(5, TimeUnit.MINUTES);
        }

    @Test
    public void shouldBeRemoteStore()
        {
        VectorStore<float[], ?, ?> store   = VectorStore.ofFloats("test");
        Service                    service = store.getService();
        if (service instanceof SafeService)
            {
            service = ((SafeService) service).getService();
            }
        assertThat(service, is(instanceOf(RemoteService.class)));
        }


    public static final String CLUSTER_NAME = "ExtendVectorStoreIT";

    static final int CLUSTER_SIZE = 3;

    @RegisterExtension
    static final TestLogsExtension TEST_LOGS = new TestLogsExtension(ExtendVectorStoreIT.class);

    @RegisterExtension
    static final CoherenceClusterExtension CLUSTER_EXTENSION = new CoherenceClusterExtension()
            .with(ClusterName.of(CLUSTER_NAME),
                  DisplayName.of("storage"),
                  RoleName.of("storage"),
                  Logging.atMax(),
                  WellKnownAddress.loopback(),
                  LocalHost.only(),
                  IPv4Preferred.yes(),
                  StabilityPredicate.of(CoherenceCluster.Predicates.isCoherenceRunning()),
                  TEST_LOGS)
            .include(CLUSTER_SIZE, CoherenceClusterMember.class);
    }
