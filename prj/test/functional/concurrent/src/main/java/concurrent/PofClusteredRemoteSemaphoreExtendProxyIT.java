/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package concurrent;

import com.oracle.bedrock.junit.CoherenceClusterExtension;

import com.oracle.bedrock.runtime.LocalPlatform;

import com.oracle.bedrock.runtime.coherence.CoherenceCluster;

import com.oracle.bedrock.runtime.coherence.options.ClusterPort;
import com.oracle.bedrock.runtime.coherence.options.LocalHost;
import com.oracle.bedrock.runtime.coherence.options.LocalStorage;
import com.oracle.bedrock.runtime.coherence.options.Logging;
import com.oracle.bedrock.runtime.coherence.options.Multicast;
import com.oracle.bedrock.runtime.coherence.options.Pof;
import com.oracle.bedrock.runtime.coherence.options.RoleName;

import com.oracle.bedrock.runtime.java.options.ClassName;
import com.oracle.bedrock.runtime.java.options.IPv4Preferred;
import com.oracle.bedrock.runtime.java.options.SystemProperty;

import com.oracle.bedrock.runtime.options.DisplayName;
import com.oracle.bedrock.runtime.options.StabilityPredicate;

import com.tangosol.net.Coherence;

import org.junit.jupiter.api.extension.RegisterExtension;

public class PofClusteredRemoteSemaphoreExtendProxyIT
        extends AbstractClusteredRemoteSemaphoreExtendProxyIT
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor.
     */
    public PofClusteredRemoteSemaphoreExtendProxyIT()
        {
        super(f_coherenceResource);
        }

    // ----- data members ---------------------------------------------------

    /**
     * A Bedrock utility to capture logs of spawned processes into files
     * under target/test-output. This is added as an option to the cluster
     * and client processes.
     */
    static TestLogs logs = new TestLogs(PofClusteredRemoteSemaphoreExtendProxyIT.class);

    /**
     * A Bedrock JUnit5 extension that starts a Coherence cluster made up of
     * three storage enabled members that act as Extend proxies, and three Extend clients.
     */
    @RegisterExtension
    static CoherenceClusterExtension f_coherenceResource =
            new CoherenceClusterExtension()
                    .using(LocalPlatform.get())
                    .with(ClassName.of(Coherence.class),
                          Logging.at(9),
                          LocalHost.only(),
                          Multicast.ttl(0),
                          IPv4Preferred.yes(),
                          logs,
                          ClusterPort.automatic(),
                          Pof.enabled(),
                          Pof.config("coherence-concurrent-pof-config.xml"),
                          StabilityPredicate.of(CoherenceCluster.Predicates.isCoherenceRunning()))
                    .include(3,
                             DisplayName.of("storage"),
                             RoleName.of("storage"),
                             SystemProperty.of(EXTEND_ENABLED_PROPERTY, false),
                             SystemProperty.of("coherence.proxy.enabled", false),
                             LocalStorage.enabled())
                    .include(3,
                             DisplayName.of("proxy"),
                             RoleName.of("proxy"),
                             SystemProperty.of(EXTEND_ENABLED_PROPERTY, true),
                             SystemProperty.of("coherence.proxy.enabled", true),
                             LocalStorage.enabled())
                    .include(3,
                             DisplayName.of("client"),
                             RoleName.of("client"),
                             LocalStorage.disabled(),
                             SystemProperty.of("coherence.client", "remote"),
                             SystemProperty.of("tangosol.coherence.tcmp.enabled", "false"),
                             StabilityPredicate.of(CoherenceCluster.Predicates.isCoherenceRunning())
                    );
    }
