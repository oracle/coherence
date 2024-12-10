/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package opentracing;

import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;

import com.oracle.bedrock.runtime.concurrent.RemoteCallable;

import com.oracle.coherence.common.base.Blocking;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.Cluster;
import com.tangosol.net.NamedCache;

import com.tangosol.util.Base;

import io.jaegertracing.internal.JaegerSpan;

import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;

import io.opentracing.util.GlobalTracer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.Test;

import org.junit.runner.RunWith;

import org.junit.runners.Parameterized;

import tracing.AbstractCacheOperationsTracingIT;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

import static org.hamcrest.MatcherAssert.assertThat;

import static opentracing.TestingUtils.validateReporter;

/**
 * Validate {@code PartitionedCache} produces tracing {@link Span spans} when performing cache operations.
 *
 * @author rl 2019
 * @since 14.1.1.0
 */
@RunWith(Parameterized.class)
public class CacheOperationsTracingIT
        extends AbstractCacheOperationsTracingIT
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Run the test using the specified cache configuration.
     *
     * @param sSerializerName  the serializer name
     * @param sCacheConfig     cache configuration
     */
    public CacheOperationsTracingIT(String sSerializerName, String sCacheConfig)
        {
        f_sSerializerName = sSerializerName;
        f_sCacheConfig    = sCacheConfig;
        }

    // ----- AbstractTracingTest methods ------------------------------------

    @Override
    protected Properties getDefaultProperties()
        {
        Properties props = super.getDefaultProperties();
        props.setProperty(JaegerConfigProperties.SERVICE_NAME.toString(), getClass().getName());
        props.setProperty("coherence.distributed.localstorage",  "false");
        props.setProperty("java.security.debug",                          "access,domain,failure");
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

        assertThat(cluster.isRunning(), is(true));
        assertThat("cluster already exists", cluster.getMemberSet().size(), is(1));

        Properties propsMain = new Properties();
        propsMain.putAll(getDefaultProperties());
        propsMain.setProperty("coherence.role",                     "storage");
        propsMain.setProperty("coherence.distributed.localstorage", "true");
        propsMain.setProperty("java.security.debug",                         "access,domain,failure");
        propsMain.setProperty("coherence.cacheconfig",              f_sCacheConfig);

        m_memberStorageNode = startMember(2, propsMain);
        waitForServer(m_memberStorageNode);
        waitForBalanced(getNamedCache().getCacheService());
        }

    // ----- methods from AbstractCacheOperationsTracingTest ----------------

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

    protected void runCacheOperation(TestBody cacheOperation, String sOpName, Validator validator)
            throws Exception
        {
        Scope scopeTest = startTestScope(sOpName);
        Span  span      = GlobalTracer.get().activeSpan();
        try
            {
            cacheOperation.run();
            Blocking.sleep(250);
            }
        finally
            {
            scopeTest.close();
            span.finish();
            }

        validator.validate();
        }

    // ----- test methods ---------------------------------------------------

    /**
     * Validate generated spans for a {@code GET} operation.
     */
    @Test
    public void testGetTracing()
        {
        super.testGetTracing(() ->
                validateReporter((reporter) ->
                    {
                    JaegerSpan[] localSpans = validateOpsPresent(
                            new String[] {"Get.request"},
                            AdaptiveTracerFactory.getReporter().getSpans());
                    Arrays.stream(localSpans).forEach(
                            span -> validateTagsForSpan(span, false, MutationType.none));

                    m_memberStorageNode.invoke(
                            new RemoteSpanValidator(new String[] {"Get.dispatch", "Get.process"},
                                                    false, MutationType.none));
                    }));
        }

    /**
     * Validate generated spans for a {@code PUT} operation.
     */
    @Test
    public void testPutTracing()
        {
        super.testPutTracing(() ->
                validateReporter((reporter) ->
                    {
                    JaegerSpan[] localSpans = validateOpsPresent(
                            new String[] {"Put.request", "FilterEvent.process"},
                            AdaptiveTracerFactory.getReporter().getSpans());
                    Arrays.stream(localSpans).forEach(
                            span -> validateTagsForSpan(span, false, MutationType.inserted));

                    m_memberStorageNode.invoke(
                            new RemoteSpanValidator(new String[] {"Put.dispatch", "Put.process"},
                                                    false, MutationType.none));
                    }));
        }

    /**
     * Validate generated spans for a {@code REMOVE} operation.
     */
    @Test
    public void testRemoveTracing()
        {
        super.testRemoveTracing(() ->
                validateReporter((reporter) ->
                    {
                    JaegerSpan[] localSpans = validateOpsPresent(
                            new String[] {"Remove.request", "FilterEvent.process"},
                            AdaptiveTracerFactory.getReporter().getSpans());
                    Arrays.stream(localSpans).forEach(
                            span -> validateTagsForSpan(span, false, MutationType.deleted));

                    m_memberStorageNode.invoke(
                            new RemoteSpanValidator(new String[] {"Remove.dispatch", "Remove.process"},
                                                    false, MutationType.none));
                    }));
        }

    // ----- junit configuration --------------------------------------------

    /**
     * Test parameters.
     * @return test parameters
     */
    @Parameterized.Parameters(name = "{0}")
    public static Object[][] data()
        {
        return PARAMS.clone();
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Starts the test scope.
     *
     * @param sOpName  the opName
     *
     * @return the scope
     */
    protected Scope startTestScope(String sOpName)
        {
        Tracer tracer  = GlobalTracer.get();
        return tracer.activateSpan(tracer.buildSpan(sOpName).start());
        }

    /**
     * Verify the tags contained within the provided {@link JaegerSpan} contains the expected keys and or keys/values.
     *
     * @param span              the span
     * @param fInternalMessage  flag indicating if the span should be marked internal or not
     * @param mutationType      the mutation type of the map event that is expected to be produced by the operation
     */
    @SuppressWarnings("SameParameterValue")
    protected static void validateTagsForSpan(JaegerSpan span, boolean fInternalMessage, MutationType mutationType)
        {
        assertThat(span, is(notNullValue()));
        Map<String, Object> metadata = span.getTags();
        String             sOpName  = span.getOperationName();

        CacheFactory.log("Validating span: " + span + ", tags: " + span.getTags());

        assertThat(sOpName + ": incorrect component tag", metadata.get("component"), is("DistributedCache"));

        if (!sOpName.startsWith("FilterEvent"))
            {
            assertThat(sOpName + ": incorrect internal.message tag",
                       metadata.get("internal.message"),
                       is(fInternalMessage));
            }

        assertThat(sOpName + ": missing thread tag",          metadata.containsKey("thread"),          is(true));
        assertThat(sOpName + ": missing operation.class tag", metadata.containsKey("operation.class"), is(true));

        if (sOpName.endsWith("request"))
            {
            assertThat(sOpName + ": incorrect member tag",
                       metadata.get("member"),
                       is(1L));
            assertThat(sOpName + ": member.source tag should not be present",
                       metadata.containsKey("member.source"),
                       is(false));
            }
        else if (sOpName.endsWith("dispatch"))
            {
            assertThat(sOpName + ": incorrect member tag",        metadata.get("member"),        is(2L));
            assertThat(sOpName + ": incorrect member.source tag", metadata.get("member.source"), is(1L));
            }
        else if (sOpName.endsWith("process"))
            {
            assertThat(sOpName + ": incorrect cache tag", metadata.get("cache"), is("dist"));
            if (sOpName.startsWith("FilterEvent"))
                {
                assertThat(sOpName + ": incorrect member tag", metadata.get("member"), is(1L));
                assertThat(sOpName + ": incorrect event.action tag",
                           metadata.get("event.action"),
                           is(mutationType.name()));
                }
            else
                {
                assertThat(sOpName + ": incorrect member tag",
                           metadata.get("member"),
                           is(2L));
                assertThat(sOpName + ": member.source tag should not be present",
                           metadata.containsKey("member.source"),
                           is(false));
                }
            }
        }

    /**
     * Validate the provided operations are present in the {@link List list} of captured {@link Span spans}.
     *
     * @param sOpNames  array of operation names to search for
     * @param spans     list of {@link Span spans}
     *
     * @return the {@link JaegerSpan}s associated with the provided operation names
     */
    protected static JaegerSpan[] validateOpsPresent(String[] sOpNames, List<JaegerSpan> spans)
        {
        assertThat(sOpNames, is(notNullValue()));
        assertThat("Spans not recorded.", spans.isEmpty(), is(false));

        JaegerSpan[] spansFound = new JaegerSpan[sOpNames.length];
        for (int i = 0, len = sOpNames.length; i < len; i++)
            {
            if (spansFound[i] == null)
                {
                final String searchFor = sOpNames[i];

                spansFound[i] = spans.stream().filter(
                        span -> Base.equals(searchFor, span.getOperationName())).findFirst().orElse(null);
                }
            }

        for (int i = 0, len = sOpNames.length; i < len; i++)
            {
            assertThat("Unable to find operation " + sOpNames[i] + " in spans on the member.",
                       spansFound[i],
                       is(notNullValue()));
            }

        return spansFound;
        }

    // ----- inner class: RemoteSpanValidator -------------------------------

    /**
     * {@link RemoteCallable} that allows validation of Spans on remote members.
     */
    protected static class RemoteSpanValidator
            implements RemoteCallable<Void>
        {
        // ----- constructors -----------------------------------------------

        /**
         * Constructs the RemoteSpanValidator.
         *
         * @param sOpsNames         an array of operation names
         * @param fInternalMessage  flag indicating if the span should be marked internal or not
         * @param mutationType      mutation type.
         */
        public RemoteSpanValidator(String[] sOpsNames, boolean fInternalMessage, MutationType mutationType)
            {
            f_sOpNames         = sOpsNames;
            f_fInternalMessage = fInternalMessage;
            f_sMutationType    = mutationType;
            }

        // ----- RemoteCallable interface -----------------------------------

        @SuppressWarnings("RedundantThrows")
        @Override
        public Void call() throws Exception
            {

            List<JaegerSpan> spans           = new ArrayList<>(AdaptiveTracerFactory.getReporter().getSpans());
            JaegerSpan[]     spansOfInterest = validateOpsPresent(f_sOpNames, spans);

            Arrays.stream(spansOfInterest).forEach(
                    span -> validateTagsForSpan(span, false, f_sMutationType));
            return null;
            }

        // ----- data members -----------------------------------------------

        /**
         * Array of operation names.
         */
        protected final String[] f_sOpNames;

        /**
         * If the message span should be considered internal or not.
         */
        protected final boolean f_fInternalMessage;

        /**
         * The mutation type of the cache operation.
         */
        protected final MutationType f_sMutationType;
        }

    // ----- data members ---------------------------------------------------

    /**
     * Enums for expected cache event mutation types.
     */
    @SuppressWarnings("unused")
    protected enum MutationType
        {
        /**
         * MapEvent insert.
         */
        inserted,

        /**
         * MapEvent update.
         */
        updated,

        /**
         * MapEvent deleted.
         */
        deleted,

        /**
         * No event.
         */
        none,
        }

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

    // ----- constants ------------------------------------------------------

    /**
     * Test constructor parameters.
     */
    protected static final Object[][] PARAMS =
            new Object[][]
                {
                    {"java", "cache-config.xml"},
                    {"pof",  "cache-config-pof.xml"},
                    {"json", "cache-config-json.xml"}
                };
    }
