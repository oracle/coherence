/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package topics;


import com.oracle.bedrock.junit.CoherenceClusterResource;
import com.oracle.bedrock.junit.SessionBuilders;

import com.oracle.bedrock.runtime.LocalPlatform;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.coherence.options.CacheConfig;
import com.oracle.bedrock.runtime.coherence.options.ClusterName;
import com.oracle.bedrock.runtime.coherence.options.LocalHost;
import com.oracle.bedrock.runtime.coherence.options.LocalStorage;
import com.oracle.bedrock.runtime.coherence.options.Logging;
import com.oracle.bedrock.runtime.coherence.options.Pof;
import com.oracle.bedrock.runtime.coherence.options.RoleName;
import com.oracle.bedrock.runtime.coherence.options.WellKnownAddress;
import com.oracle.bedrock.runtime.concurrent.RemoteRunnable;
import com.oracle.bedrock.runtime.java.options.SystemProperty;

import com.oracle.bedrock.runtime.network.AvailablePortIterator;
import com.oracle.bedrock.runtime.options.DisplayName;
import com.oracle.bedrock.runtime.options.Ports;
import com.oracle.bedrock.testsupport.junit.TestLogs;
import com.tangosol.coherence.config.Config;

import com.tangosol.internal.net.ConfigurableCacheFactorySession;
import com.tangosol.internal.util.invoke.Lambdas;

import com.tangosol.net.ExtensibleConfigurableCacheFactory;
import com.tangosol.net.Session;

import com.tangosol.util.Base;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Validate topics with Java payload using default coherence-cache-config.
 */
public class DefaultConfigJavaSerializerTopicTests
        extends AbstractNamedTopicTests
    {
    // ----- constructors ---------------------------------------------------

    public DefaultConfigJavaSerializerTopicTests()
        {
        super("java");
        }

    // ----- test lifecycle methods -----------------------------------------

    @BeforeClass
    public static void setup() throws Exception
        {
        String sHost = LocalPlatform.get().getLoopbackAddress().getHostAddress();
        System.setProperty("coherence.localhost", sHost);
        System.setProperty(LocalStorage.PROPERTY, "false");
        }

    @Before
    public void logStart()
        {
        String sMsg = ">>>>> Starting test: " + m_testWatcher.getMethodName();
        for (CoherenceClusterMember member : cluster.getCluster())
            {
            member.submit(() -> System.err.println(sMsg)).join();
            }
        }

    @After
    public void logEnd()
        {
        String sMsg = ">>>>> Finished test: " + m_testWatcher.getMethodName();
        for (CoherenceClusterMember member : cluster.getCluster())
            {
            member.submit(() -> System.err.println(sMsg)).join();
            }
        }

    // ----- helpers --------------------------------------------------------

    protected ExtensibleConfigurableCacheFactory getECCF()
        {
        return (ExtensibleConfigurableCacheFactory) cluster
            .createSession(SessionBuilders.storageDisabledMember());
        }

    @Override
    protected Session getSession()
        {
        return new ConfigurableCacheFactorySession(getECCF(), Base.getContextClassLoader());
        }

    @Override
    protected void runInCluster(RemoteRunnable runnable)
        {
        cluster.getCluster().forEach((member) -> member.submit(runnable));
        }

    @Override
    protected int getStorageMemberCount()
        {
        return STORAGE_MEMBER_COUNT;
        }

    @Override
    protected String getCoherenceCacheConfig()
        {
        return CACHE_CONFIG_FILE;
        }

    @Override
    protected int[] getMetricsPorts()
        {
        int[] anPort = new int[STORAGE_MEMBER_COUNT];
        int   i      = 0;
        for (CoherenceClusterMember member : cluster.getCluster())
            {
            Ports ports = member.getOptions().get(Ports.class);
            assertThat(ports, is(notNullValue()));
            Ports.Port port = member.getOptions().get(Ports.class).getPort("coherence.metrics.http.port");
            assertThat(port, is(notNullValue()));
            anPort[i++] = port.getActualPort();
            }
        return anPort;
        }

    // ----- constants ------------------------------------------------------

    static public int STORAGE_MEMBER_COUNT = 2;

    public static final String CACHE_CONFIG_FILE = DEFAULT_COHERENCE_CACHE_CONFIG;

    @ClassRule
    public static TestLogs s_testLogs = new TestLogs(DefaultConfigPofSerializerTopicTests.class);

    public static AvailablePortIterator s_ports = LocalPlatform.get().getAvailablePorts();

    @ClassRule
    public static CoherenceClusterResource cluster =
        new CoherenceClusterResource()
            .with(ClusterName.of(DefaultConfigJavaSerializerTopicTests.class.getSimpleName() + "Cluster"),
                  CacheConfig.of(CACHE_CONFIG_FILE),
                  Logging.atMax(),
                  Pof.disabled(),
                  SystemProperty.of("coherence.management", "all"),
                  SystemProperty.of("coherence.management.remote", "true"),
                  SystemProperty.of("coherence.management.refresh.expiry", "1ms"),
                  SystemProperty.of("coherence.metrics.http.enabled", true),
                  SystemProperty.of("coherence.metrics.http.address", "127.0.0.1"),
                  SystemProperty.of("coherence.metrics.http.port", s_ports, Ports.capture()),
                  LocalHost.only(),
                  WellKnownAddress.loopback(),
                  SystemProperty.of(Lambdas.LAMBDAS_SERIALIZATION_MODE_PROPERTY, Config.getProperty(Lambdas.LAMBDAS_SERIALIZATION_MODE_PROPERTY)))
            .include(STORAGE_MEMBER_COUNT, CoherenceClusterMember.class,
                     RoleName.of("DefaultConfigJavaSerializerTopicTestsStorage"),
                     DisplayName.of("storage"),
                     s_testLogs.builder(),
                     LocalStorage.enabled());
    }
