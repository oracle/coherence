/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package topics.remote;


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
import com.oracle.bedrock.runtime.options.DisplayName;
import com.oracle.bedrock.testsupport.junit.TestLogs;
import com.tangosol.coherence.config.Config;
import com.tangosol.internal.net.ConfigurableCacheFactorySession;
import com.tangosol.internal.util.invoke.Lambdas;
import com.tangosol.net.ExtensibleConfigurableCacheFactory;
import com.tangosol.net.Invocable;
import com.tangosol.net.InvocationService;
import com.tangosol.net.Member;
import com.tangosol.net.Session;
import com.tangosol.util.Base;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import topics.AbstractNamedTopicTests;

import java.util.Map;
import java.util.Objects;

/**
 * Validate topics over Extend with the server using Java client with POF server.
 */
public class RemoteDefaultConfigJavaClientPofClusterTests
        extends AbstractNamedTopicTests
    {
    // ----- constructors ---------------------------------------------------

    public RemoteDefaultConfigJavaClientPofClusterTests()
        {
        super("java", true);
        }

    // ----- test lifecycle methods -----------------------------------------

    @BeforeClass
    public static void setup() throws Exception
        {
        String sHost = LocalPlatform.get().getLoopbackAddress().getHostAddress();
        System.setProperty("coherence.localhost", sHost);
        System.setProperty("coherence.serializer", "java");
        System.setProperty("coherence.cacheconfig.override", "coherence-cache-config-override.xml");
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

    @Test
    @Ignore("Test skipped as the server is POF")
    public void shouldPublishAndPollUsingJavaSerializationOnly() throws Exception
        {
        }

    // ----- helpers --------------------------------------------------------

    protected ExtensibleConfigurableCacheFactory getECCF()
        {
        return (ExtensibleConfigurableCacheFactory) cluster
            .createSession(SessionBuilders.extendClient("coherence-cache-config.xml",
                    SystemProperty.of("coherence.client", "remote"),
                    SystemProperty.of("coherence.serializer", m_sSerializer),
                    SystemProperty.of("coherence.cacheconfig.override", "coherence-cache-config-override.xml")));
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
    @SuppressWarnings("unchecked")
    protected <R> R runOnServer(Invocable invocable)
        {
        InvocationService service = (InvocationService) getECCF().ensureService("RemoteInvocation");
        Map<Member, R>    map     = service.query(invocable, null);
        return map.isEmpty() ? null : map.values()
                .stream()
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
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

    // ----- constants ------------------------------------------------------

    static public int STORAGE_MEMBER_COUNT = 2;

    public static final String CACHE_CONFIG_FILE = DEFAULT_COHERENCE_CACHE_CONFIG;

    @ClassRule
    public static TestLogs s_testLogs = new TestLogs(RemoteDefaultConfigJavaClientPofClusterTests.class);

    @ClassRule
    public static CoherenceClusterResource cluster =
        new CoherenceClusterResource()
            .with(ClusterName.of(RemoteDefaultConfigJavaClientPofClusterTests.class.getSimpleName() + "Cluster"),
                  CacheConfig.of(CACHE_CONFIG_FILE),
                  Logging.atMax(),
                  Pof.config("pof-config.xml"),
                  SystemProperty.of("coherence.serializer", "pof"),
                  SystemProperty.of("coherence.extend.serializer", "java"),
                  SystemProperty.of("coherence.management", "all"),
                  SystemProperty.of("coherence.management.remote", "true"),
                  SystemProperty.of("coherence.management.refresh.expiry", "1ms"),
                  LocalHost.only(),
                  WellKnownAddress.of("127.0.0.1"),
                  SystemProperty.of(Lambdas.LAMBDAS_SERIALIZATION_MODE_PROPERTY, Config.getProperty(Lambdas.LAMBDAS_SERIALIZATION_MODE_PROPERTY)))
            .include(STORAGE_MEMBER_COUNT, CoherenceClusterMember.class,
                     RoleName.of("storage"),
                     DisplayName.of("storage"),
                     s_testLogs.builder(),
                     LocalStorage.enabled());
    }
