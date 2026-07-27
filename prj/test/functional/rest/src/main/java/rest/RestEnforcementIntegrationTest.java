/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package rest;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.oracle.bedrock.runtime.LocalPlatform;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.concurrent.RemoteCallable;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.coherence.testing.AbstractFunctionalTest;
import com.oracle.coherence.testing.RuntimeHalt;

import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ProxyService;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.HttpAcceptor;
import com.tangosol.coherence.component.util.safeService.SafeProxyService;

import com.tangosol.coherence.rest.providers.JacksonMapperProvider;
import com.tangosol.internal.util.CoherenceMode;
import com.tangosol.io.SerializationRole;
import com.tangosol.io.internal.SerializationTelemetry;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.Cluster;
import com.tangosol.net.NamedCache;

import com.tangosol.util.InvocableMap;
import com.tangosol.util.OperationReason;
import com.tangosol.util.ValueExtractor;
import com.tangosol.util.aggregator.LongSum;
import com.tangosol.util.function.Remote;
import com.tangosol.util.processor.NumberMultiplier;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;

import org.glassfish.jersey.jackson.JacksonFeature;

import org.glassfish.jersey.logging.LoggingFeature;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import data.pof.PortablePerson;
import data.pof.VersionablePortablePerson;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.Serializable;

import java.net.InetSocketAddress;
import java.net.Socket;

import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import rest.data.Persona;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.hamcrest.CoreMatchers.is;

/**
 * REST cache data-plane executable policy integration coverage.
 *
 * @author Aleks Seovic  2026.05.12
 * @since 26.04
 */
public class RestEnforcementIntegrationTest
        extends AbstractFunctionalTest
    {
    public RestEnforcementIntegrationTest()
        {
        super(FILE_SERVER_CFG_CACHE);
        }

    @BeforeClass
    public static void startup()
        {
        doStartCacheServer(SERVER_NAME, FILE_SERVER_CFG_CACHE);
        }

    @AfterClass
    public static void shutdown()
        {
        stopMember(s_member);
        s_member = null;
        }

    @Before
    public void resetTelemetry()
        {
        s_member.invoke(SetupCache.INSTANCE);
        s_member.invoke(ResetTelemetry.INSTANCE);
        }

    @Test
    public void registryAggregatorAllowsXmlExecutableClass()
        {
        Response response = getWebTarget("dist-test1/custom-long-sum(age)")
                .request(MediaType.APPLICATION_JSON)
                .get();

        assertEquals(200, response.getStatus());
        assertEquals("72", response.readEntity(String.class));
        }

    @Test
    public void registryAggregatorShadowsPlainClass()
        {
        Response response = getWebTarget("dist-test1/plain-long-sum(age)")
                .request(MediaType.APPLICATION_JSON)
                .get();

        assertEquals(200, response.getStatus());
        assertEquals("72", response.readEntity(String.class));
        assertWouldRejectCounter(PlainLongSum.class, OperationReason.AGGREGATE);
        }

    @Test
    public void registryProcessorAllowsXmlExecutableClass()
        {
        Response response = getWebTarget("dist-test-proc/(1,2)/custom-number-doubler(Age)")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.text(""));

        assertEquals(200, response.getStatus());
        }

    @Test
    public void registryProcessorShadowsPlainClass()
        {
        Response response = getWebTarget("dist-test-proc/(1,2)/plain-number-doubler(Age)")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.text(""));

        assertEquals(200, response.getStatus());
        assertWouldRejectCounter(PlainNumberDoubler.class, OperationReason.PROCESS_ENTRY);
        }

    @Test
    public void defaultProcessorFactoryShadowsConstructorSideEffectProcessor()
        {
        Response response = getWebTarget("dist-test-proc/(1,2)/constructor-side-effect-processor(Age)")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.text(""));

        assertEquals(200, response.getStatus());
        assertWouldRejectCounter(ConstructorSideEffectProcessor.class, OperationReason.PROCESS_ENTRY);
        }

    @Test
    public void defaultProcessorFactoryAllowsConstructorAfterPolicyCheck()
        {
        Response response = getWebTarget("dist-test-proc/(1,2)/allowed-constructor-side-effect-processor(Age)")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.text(""));

        assertEquals(200, response.getStatus());
        assertCounter(OperationReason.PROCESS_ENTRY, "constructor", "constructor", 3L);
        }

    @Test
    public void defaultAggregatorFactoryGatesSynthesizedExtractor()
        {
        Response response = getWebTarget("dist-test1/long-sum(age)")
                .request(MediaType.APPLICATION_JSON)
                .get();

        assertEquals(200, response.getStatus());
        assertEquals("72", response.readEntity(String.class));
        }

    @Test
    public void defaultAggregatorFactoryShadowsConstructorSideEffectAggregator()
        {
        Response response = getWebTarget("dist-test1/constructor-side-effect-aggregator(age)")
                .request(MediaType.APPLICATION_JSON)
                .get();

        assertEquals(200, response.getStatus());
        assertEquals("72", response.readEntity(String.class));
        assertWouldRejectCounter(ConstructorSideEffectAggregator.class, OperationReason.AGGREGATE);
        }

    @Test
    public void defaultAggregatorFactoryAllowsConstructorAfterPolicyCheck()
        {
        Response response = getWebTarget("dist-test1/allowed-constructor-side-effect-aggregator(age)")
                .request(MediaType.APPLICATION_JSON)
                .get();

        assertEquals(200, response.getStatus());
        assertEquals("72", response.readEntity(String.class));
        assertCounter(OperationReason.AGGREGATE, "constructor", "constructor", 3L);
        }

    @Test
    public void cohqlQueryGatesFilterAndComparator()
        {
        Response response = getWebTarget("dist-test-named-query;start=0;sort=age:asc")
                .queryParam("q", "age < 100")
                .request(MediaType.APPLICATION_JSON)
                .get();

        assertEquals(200, response.getStatus());
        }

    private void assertCounter(OperationReason reason, String sResult, String sSubReason, long cExpected)
        {
        String sKey = "coh.executable.policy_check{reason=" + reason.name()
                + ",role=" + SerializationRole.REST.name()
                + ",result=" + sResult
                + ",mode=prod"
                + ",sub_reason=" + sSubReason + "}";
        Map<String, Long> map = telemetry();
        assertEquals("counter " + sKey + " in " + map, Long.valueOf(cExpected),
                Long.valueOf(map.getOrDefault(sKey, 0L)));
        }

    private void assertWouldRejectCounter(Class<?> clz, OperationReason reason)
        {
        String sKey = "coh.executable.policy_check{result=would_reject,class=" + clz.getName()
                + ",reason=" + reason.name()
                + ",role=" + SerializationRole.REST.name() + "}";
        Map<String, Long> map = telemetry();
        assertTrue("counter " + sKey + " in " + map, map.getOrDefault(sKey, 0L) > 0L);
        }

    private Map<String, Long> telemetry()
        {
        return s_member.invoke(GetTelemetry.INSTANCE);
        }

    private WebTarget getWebTarget(String sUrl)
        {
        return getClient().target(getResourceUrl(sUrl));
        }

    private String getResourceUrl(String sResource)
        {
        return "http://127.0.0.1:" + getPort() + "/api/" + sResource;
        }

    private int getPort()
        {
        return s_nPort;
        }

    private Client getClient()
        {
        if (m_client == null)
            {
            ClientBuilder builder = ClientBuilder.newBuilder()
                    .withConfig(new ClientConfig())
                    .property(ClientProperties.CONNECT_TIMEOUT, 120000)
                    .property(ClientProperties.READ_TIMEOUT, 120000)
                    .register(JacksonMapperProvider.class)
                    .register(JacksonFeature.class);
            ((ClientConfig) builder.getConfiguration())
                    .register(new LoggingFeature(Logger.getLogger("coherence.rest.diagnostic"),
                            Level.INFO,
                            LoggingFeature.Verbosity.PAYLOAD_TEXT,
                            4096));
            m_client = builder.build();
            }
        return m_client;
        }

    @SuppressWarnings("resource")
    private static void doStartCacheServer(String sName, String sCacheConfig)
        {
        Properties properties = new Properties();
        properties.put(PROP_COHERENCE_CLUSTER, SERVER_NAME);
        properties.put(PROP_COHERENCE_MODE, "prod");
        properties.put(CoherenceMode.PROP_SECURITY_MODE, CoherenceMode.SECURITY_MODE_COMPATIBILITY);
        properties.put("com.tangosol.coherence.rest.server.DefaultResourceConfig.logging.enabled", "true");
        properties.put("java.util.logging.config.file", System.getProperty("java.util.logging.config.file", ""));
        properties.put("coherence.override", "rest-tests-coherence-override.xml");
        properties.put("coherence.wka", "127.0.0.1");
        properties.put("test.extend.port", "0");
        properties.put("test.unicast.port", "0");
        properties.put("test.multicast.address", generateUniqueAddress(true));
        properties.put("test.multicast.port", String.valueOf(LocalPlatform.get().getAvailablePorts().next()));

        s_member = startCacheServer(sName, "rest", sCacheConfig, properties);
        Eventually.assertDeferred(() -> s_member.isServiceRunning("ExtendHttpProxyService"), is(true));

        s_nPort = s_member.invoke(GetRestPort.INSTANCE);
        assertTrue("REST port should be assigned", s_nPort > 0);
        Eventually.assertDeferred(() -> isPortOpen(s_nPort), is(true));
        }

    private static boolean isPortOpen(int nPort)
        {
        try (Socket socket = new Socket())
            {
            socket.connect(new InetSocketAddress("127.0.0.1", nPort), 1000);
            return true;
            }
        catch (IOException e)
            {
            return false;
            }
        }

    private static void stopMember(CoherenceClusterMember member)
        {
        if (member == null)
            {
            return;
            }

        try
            {
            member.submit(new RuntimeHalt());
            member.waitFor();
            }
        catch (Throwable ignored)
            {
            // the member may already have exited
            }
        finally
            {
            member.close();
            }
        }

    // ----- REST registry fixtures ---------------------------------------

    @Remote.Executable
    public static class AnnotatedLongSum
            extends LongSum
        {
        public AnnotatedLongSum()
            {
            }

        public AnnotatedLongSum(String sName)
            {
            super(sName);
            }

        public AnnotatedLongSum(ValueExtractor extractor)
            {
            super(extractor);
            }

        @Override
        public InvocableMap.StreamingAggregator<Object, Object, Object, Long> supply()
            {
            return new AnnotatedLongSum(getValueExtractor());
            }
        }

    public static class PlainLongSum
            extends LongSum
        {
        public PlainLongSum()
            {
            }

        public PlainLongSum(String sName)
            {
            super(sName);
            }

        public PlainLongSum(ValueExtractor extractor)
            {
            super(extractor);
            }

        @Override
        public InvocableMap.StreamingAggregator<Object, Object, Object, Long> supply()
            {
            return new PlainLongSum(getValueExtractor());
            }
        }

    @Remote.Executable
    public static class AnnotatedNumberDoubler
            extends NumberMultiplier
        {
        public AnnotatedNumberDoubler()
            {
            }

        public AnnotatedNumberDoubler(String sName)
            {
            super(sName, Integer.valueOf(2), false);
            }
        }

    public static class PlainNumberDoubler
            extends NumberMultiplier
        {
        public PlainNumberDoubler()
            {
            }

        public PlainNumberDoubler(String sName)
            {
            super(sName, Integer.valueOf(2), false);
            }
        }

    public static class ConstructorSideEffectProcessor
            extends NumberMultiplier
        {
        public ConstructorSideEffectProcessor()
            {
            recordConstructorSideEffect(OperationReason.PROCESS_ENTRY);
            }

        public ConstructorSideEffectProcessor(String sName)
            {
            super(sName, Integer.valueOf(2), false);
            recordConstructorSideEffect(OperationReason.PROCESS_ENTRY);
            }
        }

    @Remote.Executable
    public static class AllowedConstructorSideEffectProcessor
            extends ConstructorSideEffectProcessor
        {
        public AllowedConstructorSideEffectProcessor()
            {
            }

        public AllowedConstructorSideEffectProcessor(String sName)
            {
            super(sName);
            }
        }

    public static class ConstructorSideEffectAggregator
            extends LongSum
        {
        public ConstructorSideEffectAggregator()
            {
            recordConstructorSideEffect(OperationReason.AGGREGATE);
            }

        public ConstructorSideEffectAggregator(ValueExtractor extractor)
            {
            super(extractor);
            recordConstructorSideEffect(OperationReason.AGGREGATE);
            }

        @Override
        public InvocableMap.StreamingAggregator<Object, Object, Object, Long> supply()
            {
            return new ConstructorSideEffectAggregator(getValueExtractor());
            }
        }

    @Remote.Executable
    public static class AllowedConstructorSideEffectAggregator
            extends ConstructorSideEffectAggregator
        {
        public AllowedConstructorSideEffectAggregator()
            {
            }

        public AllowedConstructorSideEffectAggregator(ValueExtractor extractor)
            {
            super(extractor);
            }

        @Override
        public InvocableMap.StreamingAggregator<Object, Object, Object, Long> supply()
            {
            return new AllowedConstructorSideEffectAggregator(getValueExtractor());
            }
        }

    private static void recordConstructorSideEffect(OperationReason reason)
        {
        SerializationTelemetry.recordExecutablePolicyCheck("constructor",
                RestEnforcementIntegrationTest.class, reason, SerializationRole.REST, null, "constructor");
        }

    // ----- member callables ---------------------------------------------

    public enum SetupCache
            implements RemoteCallable<Void>
        {
        INSTANCE;

        @Override
        public Void call()
            {
            NamedCache cache = CacheFactory.getCache("dist-test1");
            cache.clear();
            cache.put(1, PortablePerson.create());
            cache.put(2, VersionablePortablePerson.create());

            cache = CacheFactory.getCache("dist-test-proc");
            cache.clear();
            cache.put(1, new Persona("Peter", 25));
            cache.put(2, new Persona("Mary", 23));

            cache = CacheFactory.getCache("dist-test-named-query");
            cache.clear();
            cache.put(1, new Persona("Ivan", 33));
            cache.put(2, new Persona("Aleks", 37));
            cache.put(3, new Persona("Vaso", 37));
            return null;
            }
        }

    public enum ResetTelemetry
            implements RemoteCallable<Void>
        {
        INSTANCE;

        @Override
        public Void call()
            {
            SerializationTelemetry.resetForTesting();
            return null;
            }
        }

    public enum GetTelemetry
            implements RemoteCallable<Map<String, Long>>
        {
        INSTANCE;

        @Override
        public Map<String, Long> call()
            {
            return SerializationTelemetry.snapshot();
            }
        }

    public enum GetRestPort
            implements RemoteCallable<Integer>
        {
        INSTANCE;

        @Override
        public Integer call()
            {
            Cluster cluster = CacheFactory.getCluster();
            Object  service = cluster.getService("ExtendHttpProxyService");
            if (service instanceof SafeProxyService)
                {
                service = ((SafeProxyService) service).getRunningService();
                }

            ProxyService proxyService = (ProxyService) service;
            HttpAcceptor acceptor     = (HttpAcceptor) proxyService.getAcceptor();
            return acceptor == null ? 0 : acceptor.getListenPort();
            }
        }

    public static final String FILE_SERVER_CFG_CACHE = "server-cache-config-default.xml";

    private static final String SERVER_NAME = "RestEnforcementIntegrationTest-" + System.nanoTime();

    private static final String PROP_COHERENCE_CLUSTER = "coherence.cluster";

    private static final String PROP_COHERENCE_MODE = "coherence.mode";

    private static CoherenceClusterMember s_member;

    private static int s_nPort;

    private Client m_client;
    }
