/*
 * Copyright (c) 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package concurrent.executor;

import com.oracle.bedrock.junit.CoherenceClusterExtension;

import com.oracle.bedrock.runtime.LocalPlatform;

import com.oracle.bedrock.runtime.coherence.CoherenceCluster;
import com.oracle.bedrock.runtime.coherence.options.ClusterPort;
import com.oracle.bedrock.runtime.coherence.options.LocalHost;
import com.oracle.bedrock.runtime.coherence.options.LocalStorage;
import com.oracle.bedrock.runtime.coherence.options.Logging;
import com.oracle.bedrock.runtime.coherence.options.Multicast;
import com.oracle.bedrock.runtime.coherence.options.RoleName;

import com.oracle.bedrock.runtime.java.options.ClassName;
import com.oracle.bedrock.runtime.java.options.IPv4Preferred;

import com.oracle.bedrock.runtime.java.options.SystemProperty;

import com.oracle.bedrock.runtime.options.DisplayName;
import com.oracle.bedrock.runtime.options.StabilityPredicate;

import com.tangosol.net.Coherence;

import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Tests to ensure RemoteExecutor can be on another cluster member using POF.
 *
 * @author rl  2.28.2024
 * @since 15.1.1.0.0
 */
public class RemoteExecutorPofIT
        extends AbstractRemoteExecutorTest
    {
    static
        {
        System.setProperty("coherence.concurrent.serializer", "pof");
        }

    // ----- AbstractRemoteExecutorTest methods -----------------------------

    @Override
    protected Coherence getClient()
        {
        return Coherence.clusterMember();
        }

    // ----- data members ---------------------------------------------------

    /**
     * A Bedrock JUnit5 extension that starts a Coherence cluster of a single
     * storage-enabled member.
     */
    @RegisterExtension
    static CoherenceClusterExtension coherenceResource =
            new CoherenceClusterExtension()
                    .using(LocalPlatform.get())
                    .with(ClassName.of(Coherence.class),
                          Logging.at(9),
                          LocalHost.only(),
                          Multicast.ttl(0),
                          IPv4Preferred.yes(),
                          SystemProperty.of("coherence.concurrent.serializer", "pof"),
                          ClusterPort.automatic(),
                          SystemProperty.of("coherence.lambdas", "dynamic"),
                          StabilityPredicate.of(CoherenceCluster.Predicates.isCoherenceRunning()))
                    .include(1,
                             DisplayName.of("storage"),
                             RoleName.of("storage"),
                             LocalStorage.enabled(),
                             Logging.at(9));
    }
