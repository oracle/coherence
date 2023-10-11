/*
 * Copyright (c) 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package opentelemetry.core;

import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;

import com.oracle.coherence.common.base.Blocking;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.Cluster;
import com.tangosol.net.NamedCache;

import io.opentelemetry.api.GlobalOpenTelemetry;

import io.opentelemetry.api.trace.Tracer;

import io.opentelemetry.context.Scope;

import io.opentelemetry.proto.common.v1.AnyValue;

import io.opentelemetry.proto.trace.v1.Span;
import io.opentelemetry.proto.trace.v1.Span.SpanKind;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.mockserver.integration.ClientAndServer;

import tracing.AbstractCacheOperationsTracingIT;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Validate {@code PartitionedCache} produces the expected tracing
 * {@link io.opentelemetry.api.trace.Span spans} when performing cache
 * operations.
 *
 * @author rl 9.22.2023
 * @since 24.03
 */
public class CacheOperationsTracingIT
        extends AbstractCacheOperationsTracingIT
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Run the test using the specified cache configuration.
     */
    public CacheOperationsTracingIT()
        {
        f_sSerializerName = "java";
        f_sCacheConfig    = "cache-config.xml";
        }

    // ---- test lifecycle --------------------------------------------------

    @Before
    public void _before()
        {
        m_collectorServer = TestingUtils.createServer(4318);
        }

    @After
    public void _after()
        {
        m_collectorServer.reset();
        m_collectorServer.stop();
        m_collectorServer = null;
        GlobalOpenTelemetry.resetForTest();
        }

    // ----- AbstractTracingTest methods ------------------------------------

    @Override
    protected Properties getDefaultProperties()
        {
        Properties props = super.getDefaultProperties();
        props.setProperty("otel.service.name",           getClass().getName());
        props.setProperty("otel.exporter.otlp.protocol", "http/protobuf");
        props.setProperty("otel.metrics.exporter",       "none");
        props.setProperty("otel.logs.exporter",          "none");

        props.setProperty("otel.java.global-autoconfigure.enabled", "true");

        props.setProperty("coherence.distributed.localstorage",  "false");

        return props;
        }

    @Override
    public void _startCluster(Properties props, String sOverrideXml)
        {
        super._startCluster(props, f_sCacheConfig, sOverrideXml);
        }

    @Override
    protected void onClusterStart()
        {
        Cluster cluster = CacheFactory.ensureCluster();

        assertThat(cluster.isRunning(),      is(true));
        assertThat("cluster already exists", cluster.getMemberSet().size(), is(1));

        Properties propsMain = new Properties();
        propsMain.putAll(getDefaultProperties());
        propsMain.setProperty("coherence.role",                     "storage");
        propsMain.setProperty("coherence.distributed.localstorage", "true");
        propsMain.setProperty("coherence.cacheconfig",              f_sCacheConfig);

        m_memberStorageNode = startMember(2, propsMain);
        waitForServer(m_memberStorageNode);
        waitForBalanced(getNamedCache().getCacheService());
        }

    // ----- AbstractCacheOperationsTracingIT methods -----------------------

    @SuppressWarnings("rawtypes")
    @Override
    protected NamedCache getNamedCache()
        {
        return getNamedCache("dist");
        }

    @Override
    public String getCacheConfigPath()
        {
        return f_sCacheConfig;
        }

    @Override
    protected void runCacheOperation(TestBody cacheOperation, String sOpName, Validator validator)
            throws Exception
        {
        Tracer tracer = GlobalOpenTelemetry.getTracer("oracle.coherence.test");

        io.opentelemetry.api.trace.Span span   = tracer.spanBuilder(sOpName).startSpan();
        span.makeCurrent();

        try (Scope ignored = span.makeCurrent())
            {
            cacheOperation.run();
            Blocking.sleep(250);
            }
        finally
            {
            span.end();
            }

        validator.validate();
        }

    // ----- test methods ---------------------------------------------------

    @Test
    public void testGetTracing()
        {
        super.testGetTracing(() ->
                {
                List<Span> listAllSpans = TestingUtils.waitForAllSpans(m_collectorServer);
                System.out.println(listAllSpans);

                // validate client spans
                List<Span> listExpected = TestingUtils.getSpans(listAllSpans, new String[] { "Get.request" }, 1);
                assertThat(listExpected.size(), is(1));
                listExpected.forEach(span -> validateTagsForSpan(span, false, TestingUtils.MutationType.none));

                // validate server spans
                listExpected = TestingUtils.getSpans(listAllSpans, new String[] {"Get.dispatch", "Get.process"}, 2);
                assertThat(listExpected.size(), is(2));
                listExpected.forEach(span -> validateTagsForSpan(span, false, TestingUtils.MutationType.none));
                });
        }

    @Test
    public void testPutTracing()
        {
        super.testPutTracing(() ->
                {
                List<Span> listAllSpans = TestingUtils.waitForAllSpans(m_collectorServer);

                // validate client spans
                List<Span> listExpected = TestingUtils.getSpans(listAllSpans,
                        new String[] { "Put.request", "FilterEvent.process" }, 1);
                assertThat(listExpected.size(), is(2));
                listExpected.forEach(span -> validateTagsForSpan(span, false, TestingUtils.MutationType.inserted));

                // validate server spans
                listExpected = TestingUtils.getSpans(listAllSpans, new String[] {"Put.dispatch", "Put.process"}, 2);
                assertThat(listExpected.size(), is(2));
                listExpected.forEach(span -> validateTagsForSpan(span, false, TestingUtils.MutationType.inserted));
                });
        }

    @Test
    public void testRemoveTracing()
        {
        super.testRemoveTracing(() ->
                {
                List<Span> listAllSpans = TestingUtils.waitForAllSpans(m_collectorServer);

                // validate client spans
                List<Span> listExpected = TestingUtils.getSpans(listAllSpans,
                                                   new String[] { "Remove.request", "FilterEvent.process" }, 1);
                assertThat(listExpected.size(), is(2));
                listExpected.forEach(span -> validateTagsForSpan(span, false, TestingUtils.MutationType.deleted));

                // validate server spans
                listExpected = TestingUtils.getSpans(listAllSpans, new String[] {"Remove.dispatch", "Remove.process"}, 2);
                assertThat(listExpected.size(), is(2));
                listExpected.forEach(span -> validateTagsForSpan(span, false, TestingUtils.MutationType.deleted));
                });
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Verify the tags contained within the provided {@link Span span} contains the expected keys and or keys/values.
     *
     * @param span              the {@link Span span}
     * @param fInternalMessage  flag indicating if the {@link Span span} should be marked internal or not
     * @param mutationType      the mutation type of the map event that is expected to be produced by the operation
     */
    @SuppressWarnings("SameParameterValue")
    protected static void validateTagsForSpan(Span span, boolean fInternalMessage, TestingUtils.MutationType mutationType)
        {
        assertThat(span, is(notNullValue()));
        Map<String, AnyValue> metadata = TestingUtils.getMetadata(span);
        String             sOpName  = span.getName();

        CacheFactory.log("Validating span: " + span + ", tags: " + metadata);
        assertThat(sOpName + ": incorrect component tag",
                   metadata.get("component").getStringValue(), is("DistributedCache"));

        if (!sOpName.startsWith("FilterEvent"))
            {
            assertThat(sOpName + ": incorrect internal.message tag",
                       metadata.get("internal.message").getBoolValue(),
                       is(fInternalMessage));
            }

        assertThat(sOpName + ": missing thread tag",          metadata.containsKey("thread"),          is(true));
        assertThat(sOpName + ": missing operation.class tag", metadata.containsKey("operation.class"), is(true));

        if (sOpName.endsWith("request"))
            {
            assertThat(sOpName + ": incorrect member tag",
                       metadata.get("member").getIntValue(),
                       is(1L));
            assertThat(sOpName + ": member.source tag should not be present",
                       metadata.containsKey("member.source"),
                       is(false));
            assertThat(sOpName + ": incorrect span type",
                       span.getKind(),
                       is(SpanKind.SPAN_KIND_CLIENT));
            }
        else if (sOpName.endsWith("dispatch"))
            {
            assertThat(sOpName + ": incorrect member tag",        metadata.get("member").getIntValue(),        is(2L));
            assertThat(sOpName + ": incorrect member.source tag", metadata.get("member.source").getIntValue(), is(1L));
            assertThat(sOpName + ": incorrect span type",
                       span.getKind(),
                       is(SpanKind.SPAN_KIND_SERVER));
            }
        else if (sOpName.endsWith("process"))
            {
            assertThat(sOpName + ": incorrect cache tag", metadata.get("cache").getStringValue(), is("dist"));
            if (sOpName.startsWith("FilterEvent"))
                {
                assertThat(sOpName + ": incorrect member tag", metadata.get("member").getIntValue(), is(1L));
                assertThat(sOpName + ": incorrect event.action tag",
                           metadata.get("event.action").getStringValue(),
                           is(mutationType.name()));
                }
            else
                {
                assertThat(sOpName + ": incorrect member tag",
                           metadata.get("member").getIntValue(),
                           is(2L));
                assertThat(sOpName + ": member.source tag should not be present",
                           metadata.containsKey("member.source"),
                           is(false));
                assertThat(sOpName + ": incorrect span type",
                           span.getKind(),
                           is(SpanKind.SPAN_KIND_SERVER));
                }
            }
        }

    // ----- data members ---------------------------------------------------

    /**
     * Name of the cache config to be used by this test.
     */
    protected final String f_sCacheConfig;

    /**
     * Name of the serializer used by this test.
     */
    protected final String f_sSerializerName;

    /**
     * {@link CoherenceClusterMember} representing the storage node in this cluster.
     */
    protected CoherenceClusterMember m_memberStorageNode;

    /**
     * The mock server receiving tracing
     * {@link io.opentelemetry.api.trace.Span spans} generated by these
     * tests.
     */
    protected ClientAndServer m_collectorServer;
    }
