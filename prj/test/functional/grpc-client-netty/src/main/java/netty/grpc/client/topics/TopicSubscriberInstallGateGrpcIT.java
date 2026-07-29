/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package netty.grpc.client.topics;

import com.oracle.bedrock.junit.CoherenceClusterResource;
import com.oracle.bedrock.junit.SessionBuilders;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.coherence.JMXManagementMode;
import com.oracle.bedrock.runtime.coherence.options.CacheConfig;
import com.oracle.bedrock.runtime.coherence.options.ClusterName;
import com.oracle.bedrock.runtime.coherence.options.LocalHost;
import com.oracle.bedrock.runtime.coherence.options.Logging;
import com.oracle.bedrock.runtime.coherence.options.RoleName;
import com.oracle.bedrock.runtime.coherence.options.WellKnownAddress;
import com.oracle.bedrock.runtime.concurrent.RemoteCallable;
import com.oracle.bedrock.runtime.java.options.IPv4Preferred;
import com.oracle.bedrock.runtime.java.options.SystemProperty;
import com.oracle.bedrock.runtime.options.DisplayName;
import com.oracle.bedrock.testsupport.junit.TestLogs;

import com.oracle.coherence.common.base.Classes;
import com.oracle.coherence.grpc.GrpcSerializerPolicy;
import com.oracle.coherence.grpc.GrpcService;

import com.tangosol.coherence.config.Config;
import com.tangosol.internal.net.ConfigurableCacheFactorySession;
import com.tangosol.internal.util.CoherenceMode;
import com.tangosol.internal.util.invoke.Lambdas;

import com.tangosol.io.SerializationRole;
import com.tangosol.io.internal.SerializationTelemetry;

import com.tangosol.net.ExtensibleConfigurableCacheFactory;
import com.tangosol.net.Session;
import com.tangosol.net.grpc.GrpcDiagnosticsPolicy;
import com.tangosol.net.topic.NamedTopic;
import com.tangosol.net.topic.Subscriber;

import com.tangosol.util.Filter;
import com.tangosol.util.OperationReason;
import com.tangosol.util.ValueExtractor;
import com.tangosol.util.function.Remote;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import topics.NamedTopicTests;

import java.io.Serializable;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * gRPC wire coverage for topic subscriber filter and extractor install gates.
 *
 * @author Aleks Seovic  2026.05.11
 * @since 26.04
 */
public class TopicSubscriberInstallGateGrpcIT
    {
    @BeforeClass
    public static void setup()
        {
        System.setProperty("coherence.topic.publisher.close.timeout", "2s");
        System.setProperty("coherence.cluster", CLUSTER_NAME);
        System.setProperty("coherence.cacheconfig", CLIENT_CACHE_CONFIG);
        }

    @Before
    public void resetTelemetry()
        {
        cluster.getCluster().forEach(member -> member.submit(new ResetTelemetry()).join());
        }

    @Test
    public void annotatedFilterInstallsInProdOverGrpc()
        {
        NamedTopic<String> topic = ensureTopic();
        topic.ensureSubscriberGroup(groupName(), new AnnotatedFilter(), null);

        assertCounter(OperationReason.EVALUATE_FILTER, "allowed",
                SerializationTelemetry.SUB_REASON_POLICY, 1L);
        }

    @Test
    public void unannotatedFilterRejectedInProdOverGrpc()
        {
        NamedTopic<String> topic = ensureTopic();

        try
            {
            topic.ensureSubscriberGroup(groupName(), new PlainFilter(), null);
            fail("Expected gRPC filter install to fail");
            }
        catch (RuntimeException e)
            {
            assertTrue(String.valueOf(e), containsMessage(e, "Remote execution denied"));
            }

        assertCounter(OperationReason.EVALUATE_FILTER, "rejected",
                SerializationTelemetry.SUB_REASON_POLICY, 1L);
        }

    @Test
    public void unannotatedExtractorRejectedInProdOverGrpc()
        {
        NamedTopic<String> topic = ensureTopic();

        try
            {
            topic.createSubscriber(Subscriber.inGroup(groupName()),
                    Subscriber.withConverter(new PlainExtractor())).close();
            fail("Expected gRPC extractor install to fail");
            }
        catch (RuntimeException e)
            {
            assertTrue(String.valueOf(e), containsMessage(e, "Remote execution denied"));
            }

        assertCounter(OperationReason.EXTRACT, "rejected",
                SerializationTelemetry.SUB_REASON_POLICY, 1L);
        }

    private NamedTopic<String> ensureTopic()
        {
        return getSession().getTopic("java-grpc-topic-subscriber-install-" + COUNTER.incrementAndGet());
        }

    private Session getSession()
        {
        ExtensibleConfigurableCacheFactory eccf = (ExtensibleConfigurableCacheFactory)
                cluster.createSession(SessionBuilders.extendClient(CLIENT_CACHE_CONFIG));
        return new ConfigurableCacheFactorySession(eccf, Classes.getContextClassLoader());
        }

    private String groupName()
        {
        return "group-" + COUNTER.incrementAndGet();
        }

    private void assertCounter(OperationReason reason, String sResult, String sSubReason, long cExpected)
        {
        String sKey = "coh.executable.policy_check{reason=" + reason.name()
                + ",role=" + SerializationRole.TOPICS.name()
                + ",result=" + sResult
                + ",mode=prod"
                + ",sub_reason=" + sSubReason + "}";
        Map<String, Long> map = telemetry();
        assertEquals("counter " + sKey + " in " + map, Long.valueOf(cExpected), map.get(sKey));
        }

    private Map<String, Long> telemetry()
        {
        return cluster.getCluster().iterator().next().submit(new GetTelemetrySnapshot()).join();
        }

    private static boolean containsMessage(Throwable t, String sMessage)
        {
        while (t != null)
            {
            if (String.valueOf(t).contains(sMessage)
                    || String.valueOf(t.getMessage()).contains(sMessage))
                {
                return true;
                }
            t = t.getCause();
            }
        return false;
        }

    // ----- filter and extractor fixtures ---------------------------------

    @Remote.Executable
    public static class AnnotatedFilter
            implements Filter<String>, Serializable
        {
        @Override
        public boolean evaluate(String value)
            {
            return true;
            }
        }

    public static class PlainFilter
            implements Filter<String>, Serializable
        {
        @Override
        public boolean evaluate(String value)
            {
            return true;
            }
        }

    public static class PlainExtractor
            implements ValueExtractor<String, String>, Serializable
        {
        @Override
        public String extract(String target)
            {
            return target;
            }
        }

    // ----- inner class: ResetTelemetry -----------------------------------

    public static class ResetTelemetry
            implements RemoteCallable<Void>
        {
        @Override
        public Void call()
            {
            SerializationTelemetry.resetForTesting();
            return null;
            }
        }

    // ----- inner class: GetTelemetrySnapshot -----------------------------

    public static class GetTelemetrySnapshot
            implements RemoteCallable<Map<String, Long>>
        {
        @Override
        public Map<String, Long> call()
            {
            return SerializationTelemetry.snapshot();
            }
        }

    private static final String CLUSTER_NAME = "TopicSubscriberInstallGateGrpcIT-" + System.nanoTime();

    private static final String CACHE_CONFIG_FILE = "topic-cache-config.xml";

    private static final String CLIENT_CACHE_CONFIG = "grpc-topics-client-cache-config.xml";

    private static final AtomicInteger COUNTER = new AtomicInteger();

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
                            SystemProperty.of("coherence.mode", "prod"),
                            SystemProperty.of(CoherenceMode.PROP_SECURITY_MODE, CoherenceMode.SECURITY_MODE_HARDENED),
                            SystemProperty.of("coherence.proxy.enabled", "true"),
                            SystemProperty.of(GrpcDiagnosticsPolicy.PROP_ERROR_DISCLOSURE,
                                    GrpcDiagnosticsPolicy.ERROR_DISCLOSURE_DIAGNOSTIC),
                            SystemProperty.of(GrpcSerializerPolicy.PROP_ALLOWED_SERIALIZERS, "java"),
                            SystemProperty.of(GrpcService.PROP_LOG_MESSAGES,
                                    System.getProperty(GrpcService.PROP_LOG_MESSAGES)),
                            SystemProperty.of("coherence.topic.publisher.close.timeout", "2s"),
                            SystemProperty.of("coherence.management.remote", "true"),
                            SystemProperty.of("coherence.management.refresh.expiry", "1ms"),
                            SystemProperty.of(Lambdas.LAMBDAS_SERIALIZATION_MODE_PROPERTY,
                                    Config.getProperty(Lambdas.LAMBDAS_SERIALIZATION_MODE_PROPERTY)))
                    .include(1,
                            CoherenceClusterMember.class,
                            DisplayName.of("storage"),
                            RoleName.of("storage"),
                            s_testLogs.builder());
    }
