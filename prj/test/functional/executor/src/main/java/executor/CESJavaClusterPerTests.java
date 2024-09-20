/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package executor;

import com.oracle.bedrock.junit.CoherenceClusterResource;

import com.oracle.bedrock.runtime.LocalPlatform;

import com.oracle.bedrock.runtime.coherence.CoherenceCluster;

import com.oracle.bedrock.runtime.coherence.options.CacheConfig;
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

import com.oracle.bedrock.testsupport.junit.TestLogs;

import com.oracle.coherence.concurrent.config.ConcurrentServicesSessionConfiguration;

import com.tangosol.net.DefaultCacheServer;

import executor.common.NewClusterPerTest;

import java.io.File;

import org.junit.Rule;

import org.junit.experimental.categories.Category;

import org.junit.rules.TestName;

/**
 * Tests will spin up a new cluster for each test using java as the serialization
 * format.
 *
 * @author rl 7.29.2009
 * @since 21.12
 */
@Category(NewClusterPerTest.class)
public class CESJavaClusterPerTests
        extends AbstractCESClusterPerTests
    {
    // ----- constructors ---------------------------------------------------

    public CESJavaClusterPerTests()
        {
        super(EXTEND_CONFIG);
        }

    // ----- AbstractClusteredExecutorServiceTests --------------------------

    public CoherenceClusterResource getCoherence()
        {
        return m_coherence;
        }

    public String getLabel()
        {
        return this.getClass().getSimpleName() + File.separatorChar + COUNTER;
        }

    // ----- constants ------------------------------------------------------

    protected static final String CACHE_CONFIG = ConcurrentServicesSessionConfiguration.CONFIG_URI;

    protected static final String EXTEND_CONFIG = "coherence-concurrent-client-config.xml";

    // ----- data members ---------------------------------------------------

    @Rule
    public TestName m_testName = new TestName();

    @Rule
    public TestLogs m_testLogs = new TestLogs();

    /**
     * The {@link CoherenceClusterResource} to establish a {@link CoherenceCluster} for testing.
     */
    @Rule
    public CoherenceClusterResource m_coherence =
            new CoherenceClusterResource()
                    .with(SystemProperty.of("coherence.serializer", "java"),
                          ClassName.of(DefaultCacheServer.class),
                          Multicast.ttl(0),
                          LocalHost.only(),
                          Logging.at(9),
                          Pof.disabled(),
                          CacheConfig.of(CACHE_CONFIG),
                          ClusterPort.of(7574),
                          ClusterName.of(CESJavaSingleClusterTests.class.getSimpleName()), // default name is too long
                          SystemProperty.of("coherence.concurrent.scope", "$SYS"),
                          SystemProperty.of(EXTEND_ADDRESS_PROPERTY, LocalPlatform.get().getLoopbackAddress().getHostAddress()),
                          SystemProperty.of(EXTEND_PORT_PROPERTY, "9099"),
                          JmxFeature.enabled(),
                          m_testLogs)
                    .include(STORAGE_ENABLED_MEMBER_COUNT,
                             DisplayName.of("CacheServer"),
                             RoleName.of(STORAGE_ENABLED_MEMBER_ROLE),
                             LocalStorage.enabled(),
                             SystemProperty.of(EXTEND_ENABLED_PROPERTY, false),
                             SystemProperty.of(EXECUTOR_LOGGING_PROPERTY, true))
                    .include(STORAGE_DISABLED_MEMBER_COUNT,
                             DisplayName.of("ComputeServer"),
                             RoleName.of(STORAGE_DISABLED_MEMBER_ROLE),
                             LocalStorage.disabled(),
                             SystemProperty.of(EXTEND_ENABLED_PROPERTY, false),
                             SystemProperty.of(EXECUTOR_LOGGING_PROPERTY, true))
                    .include(PROXY_MEMBER_COUNT,
                             DisplayName.of("ProxyServer"),
                             RoleName.of(PROXY_MEMBER_ROLE),
                             LocalStorage.disabled(),
                             SystemProperty.of(EXTEND_ENABLED_PROPERTY, true));
    }
