/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package rest;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.tangosol.coherence.rest.providers.JacksonMapperProvider;
import com.tangosol.io.SerializationRole;
import com.tangosol.io.internal.SerializationTelemetry;

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

import java.io.Serializable;

import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.hamcrest.CoreMatchers.is;

import common.AbstractFunctionalTest;

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
        s_sModeOld = System.getProperty(PROP_COHERENCE_MODE);
        System.setProperty(PROP_COHERENCE_MODE, "prod");
        System.setProperty("test.extend.port", "0");
        doStartCacheServer(SERVER_NAME, FILE_SERVER_CFG_CACHE);
        }

    @AfterClass
    public static void shutdown()
        {
        stopCacheServer(SERVER_NAME);
        restoreProperty(PROP_COHERENCE_MODE, s_sModeOld);
        }

    @Before
    public void resetTelemetry()
        {
        setupCache();
        getNamedCache("dist-test1").invoke(1, new ResetTelemetryProcessor());
        }

    private void setupCache()
        {
        NamedCache cache = getNamedCache("dist-test1");
        cache.clear();
        cache.put(1, PortablePerson.create());
        cache.put(2, VersionablePortablePerson.create());

        cache = getNamedCache("dist-test-proc");
        cache.clear();
        cache.put(1, new Persona("Peter", 25));
        cache.put(2, new Persona("Mary", 23));

        cache = getNamedCache("dist-test-named-query");
        cache.clear();
        cache.put(1, new Persona("Ivan", 33));
        cache.put(2, new Persona("Aleks", 37));
        cache.put(3, new Persona("Vaso", 37));
        }

    @Test
    public void registryAggregatorAllowsXmlExecutableClass()
        {
        Response response = getWebTarget("dist-test1/custom-long-sum(age)")
                .request(MediaType.APPLICATION_JSON)
                .get();

        assertEquals(200, response.getStatus());
        assertEquals("72", response.readEntity(String.class));
        assertCounter(OperationReason.AGGREGATE, "allowed", SerializationTelemetry.SUB_REASON_POLICY, 2L);
        }

    @Test
    public void registryAggregatorRejectsPlainClass()
        {
        Response response = getWebTarget("dist-test1/plain-long-sum(age)")
                .request(MediaType.APPLICATION_JSON)
                .get();

        assertTrue(response.getStatus() >= 400);
        assertCounter(OperationReason.AGGREGATE, "rejected", SerializationTelemetry.SUB_REASON_POLICY, 1L);
        }

    @Test
    public void registryProcessorAllowsXmlExecutableClass()
        {
        Response response = getWebTarget("dist-test-proc/(1,2)/custom-number-doubler(age)")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.text(""));

        assertEquals(200, response.getStatus());
        assertCounter(OperationReason.PROCESS_ENTRY, "allowed", SerializationTelemetry.SUB_REASON_POLICY, 4L);
        }

    @Test
    public void registryProcessorRejectsPlainClass()
        {
        Response response = getWebTarget("dist-test-proc/(1,2)/plain-number-doubler(age)")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.text(""));

        assertTrue(response.getStatus() >= 400);
        assertCounter(OperationReason.PROCESS_ENTRY, "rejected", SerializationTelemetry.SUB_REASON_POLICY, 1L);
        }

    @Test
    public void defaultProcessorFactoryRejectsBeforeConstructorSideEffect()
        {
        Response response = getWebTarget("dist-test-proc/(1,2)/constructor-side-effect-processor(age)")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.text(""));

        assertTrue(response.getStatus() >= 400);
        assertCounter(OperationReason.PROCESS_ENTRY, "rejected", SerializationTelemetry.SUB_REASON_POLICY, 1L);
        assertCounter(OperationReason.PROCESS_ENTRY, "constructor", "constructor", 0L);
        }

    @Test
    public void defaultProcessorFactoryAllowsConstructorAfterPolicyCheck()
        {
        Response response = getWebTarget("dist-test-proc/(1,2)/allowed-constructor-side-effect-processor(age)")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.text(""));

        assertEquals(200, response.getStatus());
        assertCounter(OperationReason.PROCESS_ENTRY, "allowed", SerializationTelemetry.SUB_REASON_POLICY, 4L);
        assertCounter(OperationReason.PROCESS_ENTRY, "constructor", "constructor", 2L);
        }

    @Test
    public void defaultAggregatorFactoryGatesSynthesizedExtractor()
        {
        Response response = getWebTarget("dist-test1/long-sum(age)")
                .request(MediaType.APPLICATION_JSON)
                .get();

        assertEquals(200, response.getStatus());
        assertEquals("72", response.readEntity(String.class));
        assertCounter(OperationReason.EXTRACT, "allowed", SerializationTelemetry.SUB_REASON_POLICY, 2L);
        assertCounter(OperationReason.AGGREGATE, "allowed", SerializationTelemetry.SUB_REASON_POLICY, 2L);
        }

    @Test
    public void defaultAggregatorFactoryRejectsBeforeConstructorSideEffect()
        {
        Response response = getWebTarget("dist-test1/constructor-side-effect-aggregator(age)")
                .request(MediaType.APPLICATION_JSON)
                .get();

        assertTrue(response.getStatus() >= 400);
        assertCounter(OperationReason.AGGREGATE, "rejected", SerializationTelemetry.SUB_REASON_POLICY, 1L);
        assertCounter(OperationReason.AGGREGATE, "constructor", "constructor", 0L);
        }

    @Test
    public void defaultAggregatorFactoryAllowsConstructorAfterPolicyCheck()
        {
        Response response = getWebTarget("dist-test1/allowed-constructor-side-effect-aggregator(age)")
                .request(MediaType.APPLICATION_JSON)
                .get();

        assertEquals(200, response.getStatus());
        assertEquals("72", response.readEntity(String.class));
        assertCounter(OperationReason.AGGREGATE, "allowed", SerializationTelemetry.SUB_REASON_POLICY, 2L);
        assertCounter(OperationReason.AGGREGATE, "constructor", "constructor", 3L);
        }

    @Test
    public void cohqlQueryGatesFilterAndComparator()
        {
        // keeps PROD REST URL expressions alias-only so this route reaches the comparator gate
        Response response = getWebTarget("dist-test-named-query;start=0;sort=by-age:asc")
                .queryParam("q", "age < 100")
                .request(MediaType.APPLICATION_JSON)
                .get();

        assertEquals(200, response.getStatus());
        assertCounter(OperationReason.EVALUATE_FILTER, "allowed", SerializationTelemetry.SUB_REASON_POLICY, 1L);
        assertCounter(OperationReason.EXTRACT, "allowed", SerializationTelemetry.SUB_REASON_POLICY, 2L);
        assertCounter(OperationReason.COMPARE, "allowed", SerializationTelemetry.SUB_REASON_POLICY, 3L);
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

    private Map<String, Long> telemetry()
        {
        return (Map<String, Long>) getNamedCache("dist-test1").invoke(1, new GetTelemetryProcessor());
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
        NamedCache cache = getNamedCache("dist-test1");
        return (Integer) cache.invoke(1, new AbstractRestTests.GetPortProcessor());
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
        properties.put("com.tangosol.coherence.rest.server.DefaultResourceConfig.logging.enabled", "true");
        properties.put("java.util.logging.config.file", System.getProperty("java.util.logging.config.file", ""));

        CoherenceClusterMember clusterMember = startCacheServer(sName, "rest", sCacheConfig, properties);
        Eventually.assertDeferred(() -> clusterMember.isServiceRunning("ExtendHttpProxyService"), is(true));
        }

    private static void restoreProperty(String sName, String sValue)
        {
        if (sValue == null)
            {
            System.clearProperty(sName);
            }
        else
            {
            System.setProperty(sName, sValue);
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

    // ----- telemetry processors -----------------------------------------

    @Remote.Executable
    public static class ResetTelemetryProcessor
            implements InvocableMap.EntryProcessor<Object, Object, Void>, Serializable
        {
        @Override
        public Void process(InvocableMap.Entry<Object, Object> entry)
            {
            SerializationTelemetry.resetForTesting();
            return null;
            }
        }

    @Remote.Executable
    public static class GetTelemetryProcessor
            implements InvocableMap.EntryProcessor<Object, Object, Map<String, Long>>, Serializable
        {
        @Override
        public Map<String, Long> process(InvocableMap.Entry<Object, Object> entry)
            {
            return SerializationTelemetry.snapshot();
            }
        }

    public static final String FILE_SERVER_CFG_CACHE = "server-cache-config-default.xml";

    private static final String SERVER_NAME = "RestEnforcementIntegrationTest-" + System.nanoTime();

    private static final String PROP_COHERENCE_MODE = "coherence.mode";

    private static String s_sModeOld;

    private Client m_client;
    }
