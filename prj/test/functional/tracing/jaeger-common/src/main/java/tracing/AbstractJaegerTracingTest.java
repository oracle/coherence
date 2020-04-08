/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package tracing;

import com.oracle.coherence.common.base.Blocking;

import com.tangosol.internal.tracing.TracingHelper;

import com.tangosol.internal.tracing.TracingShim;

import com.tangosol.internal.tracing.opentracing.AbstractOpenTracingTracer;

import com.tangosol.net.NamedCache;

import com.tangosol.net.cache.TypeAssertion;

import io.jaegertracing.Configuration;

import io.jaegertracing.internal.JaegerSpan;
import io.jaegertracing.internal.JaegerTracer;

import io.jaegertracing.internal.reporters.InMemoryReporter;

import io.jaegertracing.internal.samplers.ConstSampler;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;

import io.opentracing.contrib.tracerresolver.TracerFactory;

import io.opentracing.noop.NoopSpan;

import io.opentracing.util.GlobalTracer;

import java.lang.reflect.Field;

import java.util.Properties;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;

import static org.junit.Assert.assertThat;

import static tracing.TestingUtils.validateReporter;

/**
 * Tests to validate Coherence can properly bootstrap with Jaeger+TracerResolver.
 *
 * @author rl 2019
 * @since 14.1.1.0
 */
public abstract class AbstractJaegerTracingTest
        extends AbstractTracingTest
    {
    // ----- AbstractTracingTest methods ------------------------------------

    @Override
    protected Properties getDefaultProperties()
        {
        Properties props = super.getDefaultProperties();
        props.setProperty(JaegerConfigProperties.SERVICE_NAME.toString(), getClass().getName());
        return props;
        }

    /**
     * Starts the test scope.
     *
     * @param sOpName the operation name
     *
     * @return the {@link Scope}
     */
    @SuppressWarnings("SameParameterValue")
    protected Scope startTestScope(String sOpName)
        {
        Tracer tracer  = GlobalTracer.get();
        return tracer.activateSpan(tracer.buildSpan(sOpName).start());
        }

    // ----- test lifecycle -------------------------------------------------

    /**
     * Reset the {@link InMemoryReporter} before each test execution.
     */
    @SuppressWarnings("CheckStyle")
    @Before
    public void _before()
        {
        AdaptiveTracerFactory.resetReporter();
        }

    // ----- test methods ---------------------------------------------------

    @Test
    @Ignore
    @Override
    public void shouldBeDisabledByDefault()
        {
        super.shouldBeDisabledByDefault();
        }

    /**
     * When coherence tracing is enabled and a usable {@link TracerFactory} is present, tracing should be enabled.
     */
    @Test
    public void testIsEnabled()
        {
        runTest(() -> assertThat("Tracing should be enabled.", TracingHelper.isEnabled(), is(true)));
        }

    /**
     * Verify {@link TracingHelper#getTracer()} returns an instance of {@link AbstractOpenTracingTracer}.
     */
    @Test
    public void testGetTracer()
        {
        runTest(() ->
                {
                com.tangosol.internal.tracing.Tracer tracer = TracingHelper.getTracer();
                assertThat("Expected tracer of type "
                                   + getExcpectedTracerType()
                                   + ", received: "
                                   + tracer.getClass().getName(),
                           getExcpectedTracerType().isInstance(tracer),
                           is(true));
                });
        }

    /**
     * Verify the act of starting a cluster when tracing is enabled will result in {@link Span spans} being captured.
     */
    @Test
    public void testTraceCaptured()
        {
        runTest(() ->
                        validateReporter((reporter) ->
                                                 assertThat("No spans recorded.",
                                                            reporter.getSpans().isEmpty(),
                                                            is(false))));
        }

    /**
     * Verify the act of starting a cluster when tracing is set to {@code 0} will result in no spans being captured.
     */
    @Test
    public void testTraceNotCapturedWhenTraceIsZero()
        {
        runTest(() ->
                {
                assertThat("Tracing should be enabled.", TracingHelper.isEnabled(), is(true));
                validateReporter((reporter) ->
                                         assertThat("Spans incorrectly recorded.",
                                                    reporter.getSpans().isEmpty(),
                                                    is(true)));
                }, "tracing-enabled-with-zero.xml");
        }

    /**
     * Verify the act of starting a cluster when tracing is set to {@code 0} will result in no spans being captured.
     */
    @Test
    public void testTraceCapturedWhenExplicitAndTraceIsZero()
        {
        runTest(() ->
                {
                assertThat("Tracing should be enabled.", TracingHelper.isEnabled(), is(true));

                try (@SuppressWarnings("unused") Scope scopeTest = startTestScope("test"))
                    {
                    NamedCache<String, String> cache =
                            getNamedCache("dist", TypeAssertion.withTypes(String.class, String.class));
                    assertThat(cache, is(notNullValue()));
                    cache.put("a", "b");
                    cache.get("a");
                    }

                Blocking.sleep(250);
                validateReporter((reporter) ->
                                         assertThat("No spans recorded.",
                                                    reporter.getSpans().isEmpty(),
                                                    is(false)));
                }, "tracing-enabled-with-zero.xml");
        }

    /**
     * Verify the act of starting a cluster when tracing is set to {@code 0} and an outer span is present,
     * allows tracing spans to be collected.
     */
    @Test
    public void testNoTraceCapturedWhenExplicitAndNoOuterSpanAndTraceIsZero()
        {
        runTest(() ->
                {
                assertThat("Tracing should be enabled.", TracingHelper.isEnabled(), is(true));

                // span created via Tracing will be no-op if there is no outer span and thus won't be recorded.
                NamedCache<String, String> cache =
                        getNamedCache("dist", TypeAssertion.withTypes(String.class, String.class));
                assertThat(cache, is(notNullValue()));
                cache.put("a", "b");
                cache.get("a");

                Blocking.sleep(250);
                validateReporter((reporter) ->
                                         assertThat("Spans incorrectly recorded.",
                                                    reporter.getSpans().isEmpty(),
                                                    is(true)));
                }, "tracing-enabled-with-zero.xml");
        }

    /**
     * Validate the {@link Span} produced when tracing is zero and there is no outer span is an
     * instance of {@link NoopSpan}.
     */
    @Test
    public void testExpectedSpanTypeWhenTraceIsZeroNoOuterSpan()
        {
        runTest(() ->
                {
                assertThat("Tracing should be enabled.", TracingHelper.isEnabled(), is(true));

                com.tangosol.internal.tracing.Span tracingSpan = TracingHelper.getActiveSpan();
                assertThat("Expected active span to null, but was "
                                   + (tracingSpan == null ? "null" : tracingSpan.getClass().getName()),
                           tracingSpan,
                           is(nullValue()));

                }, "tracing-enabled-with-zero.xml");
        }

    /**
     * Validate the {@link Span} produced when tracing is zero and there is an outer span is an
     * instance of {@link JaegerSpan}.
     */
    @Test
    public void testExpectedSpanTypeWhenTraceIsZeroWithOuterSpan()
        {
        runTest(() ->
                {
                assertThat("Tracing should be enabled.", TracingHelper.isEnabled(), is(true));

                try (@SuppressWarnings("unused") Scope scopeTest = startTestScope("test"))
                    {
                    com.tangosol.internal.tracing.Span tracingSpan = TracingHelper.getActiveSpan();
                    assertThat("Expected active span to be JaegerSpan, but was "
                                       + (tracingSpan == null ? "null" : tracingSpan.getClass().getName()),
                               tracingSpan != null && tracingSpan.underlying() instanceof JaegerSpan,
                               is(true));
                    }
                }, "tracing-enabled-with-zero.xml");
        }

    /**
     * Validate Coherence considers tracing enabled if a Tracer has been globally registered and will not terminate
     * the tracer when Coherence is shut down under the assumption the tracer is external to its scope.
     *
     * @throws Exception if an error occurs processing the test
     */
    @Test
    public void testExternalTracerPresence() throws Exception
        {
        try
            {
            // required to create the tracer
            System.setProperty(JaegerConfigProperties.SERVICE_NAME.toString(), getClass().getName());

            // Build up a custom Jaeger Tracer using in-memory reporting
            final AtomicBoolean    closed        = new AtomicBoolean();
            JaegerTracer.Builder   tracerBuilder = Configuration.fromEnv().getTracerBuilder();
            // Use in-memory reporter to validate we can capture spans and ensure we can capture any incorrect
            // closure of the tracer
            final InMemoryReporter reporter      = new InMemoryReporter()
                {
                @Override
                public void close()
                    {
                    super.close();
                    closed.compareAndSet(false, true);
                    }
                };

            // register the tracer and validate TracingHelper takes no initialization action
            final Tracer tracer = tracerBuilder.withReporter(reporter).withSampler(new ConstSampler(true)).build();
            assertThat("Tracer already registered!  Test in invalid state.",
                       GlobalTracer.registerIfAbsent(tracer),
                       is(true));
            assertThat("Expected TracingHelper.initialize() to return null",
                       TracingHelper.initialize(new TracingShim.DefaultDependencies()),
                       is(nullValue()));

            // Start a cluster and run sanity checks
            runTest(() ->
                    {
                    assertThat("Expected TracingHelper.isEnabled() to return true",
                               TracingHelper.isEnabled(),
                               is(true));

                    // TracerResolver and therefor by extension TracingHelper will return
                    // any registered global Tracer before invoking service loaders
                    Tracer underlying = TracingHelper.getTracer().underlying();
                    Field field = GlobalTracer.class.getDeclaredField("tracer");
                    field.setAccessible(true);
                    assertThat("Unable to find tracer field within GlobalTracer - API INCOMPATIBILITY!",
                               field,
                               is(notNullValue()));
                    assertThat("Coherence incorrectly initialized a new Tracer",
                               field.get(underlying) == tracer,
                               is(true));
                    });
            Blocking.sleep(250);

            // verify we have spans
            assertThat("No spans recorded", reporter.getSpans().isEmpty(), is(false));

            // verify the Tracer wasn't closed by Coherence
            assertThat("Tracer was incorrectly closed by Coherence", closed.get(), is(false));
            }
        finally
            {
            // reset the tracer to avoid the possibility of impacting other tests
            Field fieldRegistered = GlobalTracer.class.getDeclaredField("isRegistered");
            fieldRegistered.setAccessible(true);
            fieldRegistered.set(null, false);
            }
        }

    // ----- abstract helper methods ----------------------------------------

    /**
     * Get the expected tracer shim type.
     *
     * @return the expected tracer shim type
     */
    protected abstract Class<? extends AbstractOpenTracingTracer> getExcpectedTracerType();

    }
