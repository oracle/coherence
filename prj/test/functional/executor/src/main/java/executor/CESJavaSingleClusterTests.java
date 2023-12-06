/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package executor;

import com.oracle.bedrock.runtime.coherence.CoherenceCluster;

import com.oracle.bedrock.runtime.coherence.options.ClusterName;
import com.oracle.bedrock.runtime.coherence.options.ClusterPort;
import com.oracle.bedrock.runtime.coherence.options.LocalHost;
import com.oracle.bedrock.runtime.coherence.options.LocalStorage;
import com.oracle.bedrock.runtime.coherence.options.Logging;
import com.oracle.bedrock.runtime.coherence.options.Multicast;
import com.oracle.bedrock.runtime.coherence.options.Pof;
import com.oracle.bedrock.runtime.coherence.options.RoleName;

import com.oracle.bedrock.runtime.java.features.JmxFeature;

import com.oracle.bedrock.runtime.java.options.ClassName;
import com.oracle.bedrock.runtime.java.options.SystemProperty;

import com.oracle.bedrock.runtime.options.DisplayName;

import com.oracle.bedrock.runtime.options.StabilityPredicate;
import com.tangosol.net.Coherence;

import executor.common.CoherenceClusterResource;
import executor.common.LogOutput;
import executor.common.SingleClusterForAllTests;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;

import org.junit.experimental.categories.Category;

/**
 * Tests will spin up a cluster shared by each test using java as the serialization
 * format.
 *
 * @author rl 7.29.2009
 * @since 21.12
 */
@Category(SingleClusterForAllTests.class)
public class CESJavaSingleClusterTests
        extends AbstractCESSingleClusterTests
    {
    // ----- constructors ---------------------------------------------------

    public CESJavaSingleClusterTests()
        {
        super(EXTEND_CONFIG);
        }

    // ----- test lifecycle -------------------------------------------------

    @BeforeClass
    public static void setupClass()
        {
        System.setProperty("test.heap.max", "512");
        // ensure the proxy service is running (before we connect)
        AbstractClusteredExecutorServiceTests.ensureConcurrentServiceRunning(s_coherence.getCluster());
        ensureExecutorProxyAvailable(s_coherence.getCluster());
        }

    // ----- AbstractClusteredExecutorServiceTests --------------------------

    public CoherenceClusterResource getCoherence()
        {
        return s_coherence;
        }

    public String getLabel()
        {
        return CESJavaSingleClusterTests.class.getSimpleName();
        }

    // ----- constants ------------------------------------------------------

    protected static final String EXTEND_CONFIG = "coherence-concurrent-client-config.xml";

    // ----- data members ---------------------------------------------------

    /**
     * The {@link CoherenceClusterResource} to establish a {@link CoherenceCluster} for testing.
     */
    @ClassRule
    public static CoherenceClusterResource s_coherence =
            (CoherenceClusterResource) new CoherenceClusterResource()
                    .with(ClassName.of(Coherence.class),
                          Multicast.ttl(0),
                          LocalHost.only(),
                          Logging.at(9),
                          Pof.disabled(),
                          ClusterPort.of(7574),
                          ClusterName.of(CESJavaSingleClusterTests.class.getSimpleName()), // default name is too long
                          SystemProperty.of(EXTEND_ADDRESS_PROPERTY, EXTEND_HOST),
                          SystemProperty.of(EXTEND_PORT_PROPERTY, EXTEND_PORT),
                          JmxFeature.enabled(),
                          StabilityPredicate.of(CoherenceCluster.Predicates.isCoherenceRunning()))
                    .include(STORAGE_ENABLED_MEMBER_COUNT,
                             DisplayName.of("CacheServer"),
                             LogOutput.to(CESJavaSingleClusterTests.class.getSimpleName(), "CacheServer"),
                             RoleName.of(STORAGE_ENABLED_MEMBER_ROLE),
                             LocalStorage.enabled(),
                             SystemProperty.of(EXTEND_ENABLED_PROPERTY, false),
                             SystemProperty.of(EXECUTOR_LOGGING_PROPERTY, true))
                    .include(STORAGE_DISABLED_MEMBER_COUNT,
                             DisplayName.of("ComputeServer"),
                             LogOutput.to(CESJavaSingleClusterTests.class.getSimpleName(), "ComputeServer"),
                             RoleName.of(STORAGE_DISABLED_MEMBER_ROLE),
                             LocalStorage.disabled(),
                             SystemProperty.of(EXTEND_ENABLED_PROPERTY, false),
                             SystemProperty.of(EXECUTOR_LOGGING_PROPERTY, true))
                    .include(PROXY_MEMBER_COUNT,
                             DisplayName.of("ProxyServer"),
                             LogOutput.to(CESJavaSingleClusterTests.class.getSimpleName(), "ProxyServer"),
                             RoleName.of(PROXY_MEMBER_ROLE),
                             LocalStorage.disabled(),
                             SystemProperty.of(EXTEND_ENABLED_PROPERTY, true),
                             SystemProperty.of(EXECUTOR_LOGGING_PROPERTY, true));
    }

