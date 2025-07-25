/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package netty.grpc.client.topics;

import com.oracle.bedrock.junit.CoherenceClusterResource;
import com.oracle.bedrock.junit.SessionBuilders;
import com.oracle.bedrock.runtime.LocalPlatform;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.coherence.JMXManagementMode;
import com.oracle.bedrock.runtime.coherence.options.CacheConfig;
import com.oracle.bedrock.runtime.coherence.options.ClusterName;
import com.oracle.bedrock.runtime.coherence.options.LocalHost;
import com.oracle.bedrock.runtime.coherence.options.Logging;
import com.oracle.bedrock.runtime.coherence.options.RoleName;
import com.oracle.bedrock.runtime.coherence.options.WellKnownAddress;
import com.oracle.bedrock.runtime.java.options.IPv4Preferred;
import com.oracle.bedrock.runtime.java.options.SystemProperty;
import com.oracle.bedrock.runtime.options.DisplayName;
import com.oracle.bedrock.testsupport.junit.TestLogs;
import com.oracle.coherence.common.base.Classes;
import com.oracle.coherence.grpc.GrpcService;
import com.tangosol.coherence.config.Config;
import com.tangosol.internal.net.ConfigurableCacheFactorySession;
import com.tangosol.internal.util.invoke.Lambdas;
import com.tangosol.net.ExtensibleConfigurableCacheFactory;
import com.tangosol.net.Session;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import topics.NamedTopicTests;

import java.util.Arrays;
import java.util.Collection;

@RunWith(Parameterized.class)
public class SimpleSubscriberTests
        extends AbstractSimpleSubscriberTests
    {
    // ----- constructors ---------------------------------------------------

    public SimpleSubscriberTests(String sSerializer)
        {
        super(sSerializer);
        }

    // ----- test lifecycle methods -----------------------------------------

    @Parameterized.Parameters(name = "serializer={0}")
    public static Collection<Object[]> parameters()
        {
        return Arrays.asList(new Object[][]
                {
                {"pof"}, {"java"}
                });
        }

    @BeforeClass
    public static void setup()
        {
        System.setProperty("coherence.topic.publisher.close.timeout", "2s");
        System.setProperty("com.oracle.coherence.common.internal.util.HeapDump.Bug-27585336-tmb-migration", "true");

        String sHost = LocalPlatform.get().getLoopbackAddress().getHostAddress();
        System.setProperty("coherence.localhost", sHost);
        System.setProperty("coherence.cluster", CLUSTER_NAME);
        System.setProperty("coherence.cacheconfig", "grpc-topics-client-cache-config.xml");
        }

    @Override
    protected Session getSession()
        {
        return new ConfigurableCacheFactorySession(getECCF(), Classes.getContextClassLoader());
        }

    protected ExtensibleConfigurableCacheFactory getECCF()
        {
        return (ExtensibleConfigurableCacheFactory) cluster
                .createSession(SessionBuilders.extendClient("grpc-topics-client-cache-config.xml"));
        }


    // ----- data members ---------------------------------------------------

    public static final int STORAGE_MEMBER_COUNT = 2;

    public static final String CLUSTER_NAME = "RemoteTopicTests";

    public static final String CACHE_CONFIG_FILE = "topic-cache-config.xml";

    @ClassRule
    public static TestLogs s_testLogs = new TestLogs(NamedTopicTests.class);

    @ClassRule
    public static CoherenceClusterResource cluster =
            new CoherenceClusterResource()
                    .with(ClusterName.of(CLUSTER_NAME),
                            Logging.at(9),
                            CacheConfig.of(CACHE_CONFIG_FILE),
                            LocalHost.only(),
                            WellKnownAddress.of("127.0.0.1"),
                            JMXManagementMode.ALL,
                            IPv4Preferred.yes(),
                            SystemProperty.of(GrpcService.PROP_LOG_MESSAGES, System.getProperty(GrpcService.PROP_LOG_MESSAGES)),
                            SystemProperty.of("coherence.topic.publisher.close.timeout", "2s"),
                            SystemProperty.of("coherence.management.remote", "true"),
                            SystemProperty.of("coherence.management.refresh.expiry", "1ms"),
                            SystemProperty.of(Lambdas.LAMBDAS_SERIALIZATION_MODE_PROPERTY,
                                    Config.getProperty(Lambdas.LAMBDAS_SERIALIZATION_MODE_PROPERTY)))
                    .include(STORAGE_MEMBER_COUNT,
                            CoherenceClusterMember.class,
                            DisplayName.of("storage"),
                            RoleName.of("storage"),
                            s_testLogs.builder());
    }
