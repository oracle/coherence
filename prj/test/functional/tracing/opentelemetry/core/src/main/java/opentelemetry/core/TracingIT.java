/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package opentelemetry.core;

import com.oracle.bedrock.deferred.Deferred;

import com.oracle.bedrock.options.Timeout;

import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.testsupport.deferred.Repetitively;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.internal.tracing.TracingHelper;

import com.tangosol.internal.tracing.opentelemetry.OpenTelemetryShim;
import com.tangosol.net.NamedCache;

import com.tangosol.net.cache.TypeAssertion;

import io.opentelemetry.api.GlobalOpenTelemetry;

import io.opentelemetry.api.OpenTelemetry;

import io.opentelemetry.api.events.GlobalEventEmitterProvider;

import io.opentelemetry.api.trace.Tracer;

import io.opentelemetry.context.Scope;

import io.opentelemetry.proto.trace.v1.Span;

import java.lang.reflect.Field;

import java.util.List;
import java.util.Properties;

import java.util.concurrent.TimeUnit;

import org.hamcrest.Matcher;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import org.mockserver.integration.ClientAndServer;

import tracing.AbstractTracingIT;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests to validate various tracing behaviors within Coherence.
 *
 * @author rl 9.22.2023
 * @since  24.03
 */
public class TracingIT
        extends AbstractTracingIT
    {
    // ----- methods from AbstractTracingTest -------------------------------

    @Override
    protected Properties getDefaultProperties()
        {
        Properties props = super.getDefaultProperties();
        props.setProperty("otel.service.name",           getClass().getName());
        props.setProperty("otel.exporter.otlp.protocol", "http/protobuf");
        props.setProperty("otel.metrics.exporter",       "none");
        props.setProperty("otel.logs.exporter",          "none");

        props.setProperty("otel.java.global-autoconfigure.enabled", "true");

        return props;
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

    // ----- test methods ---------------------------------------------------

    @Test
    @Ignore
    @Override
    public void shouldBeDisabledByDefault()
        {
        super.shouldBeDisabledByDefault();
        }

    /**
     * Verify the act of starting a cluster when tracing is enabled will
     * result in {@link Span spans} being captured.
     */
    @Test
    public void testTraceCaptured()
        {
        runTest(() ->
                Eventually.assertDeferred("No spans recorded",
                    () -> !TestingUtils.extractSpans(m_collectorServer).isEmpty(),
                    is(true)));
        }

    /**
     * Verify the act of starting a cluster when tracing is set to {@code 0}
     * will result in no spans being captured.
     */
    @Test
    public void testTraceNotCapturedWhenTraceIsZero()
        {
        runTest(this::assertNoSpans, "tracing-enabled-with-zero.xml");
        }

    @Test
    public void testTraceCapturedWhenExplicitAndTraceIsZero()
        {
        runTest(() ->
                {
                Tracer tracer = GlobalOpenTelemetry.getTracer("oracle.coherence.test");

                io.opentelemetry.api.trace.Span span   = tracer.spanBuilder("test").startSpan();
                span.makeCurrent();

                NamedCache<String, String> cache =
                        getNamedCache("dist1", TypeAssertion.withTypes(String.class, String.class));

                try (Scope ignored = span.makeCurrent())
                    {
                    cache.put("a", "b");
                    cache.get("a");
                    }
                finally
                    {
                    span.end();
                    }

                Eventually.assertDeferred("No spans recorded",
                        () ->
                            {
                            List<Span> spans =
                                    TestingUtils.extractSpans(m_collectorServer);

                            if (spans.isEmpty())
                                {
                                return false;
                                }

                            Logger.finest("Captured spans: " + spans);
                            return true;
                            },
                        is(true));
                }, "tracing-enabled-with-zero.xml");
        }

    /**
     * Verify the act of starting a cluster when tracing is set to {@code 0}
     * and an outer span is present, allows tracing spans to be collected.
     */
    @Test
    public void testNoTraceCapturedWhenExplicitAndNoOuterSpanAndTraceIsZero()
        {
        runTest(() ->
                {
                NamedCache<String, String> cache =
                        getNamedCache("dist2", TypeAssertion.withTypes(String.class, String.class));

                cache.put("a", "b");
                cache.get("a");

                assertNoSpans();
                }, "tracing-enabled-with-zero.xml");
        }

    /**
     * Assume the required dependencies are on the class path, verify
     * Coherence properly controls the lifecycle of the managed
     * OpenTelemetry runtime.
     *
     * @throws Exception if an error occurs processing the test
     */
    @Test
    public void testTelemetryLifecycleManagedByCoherence()
            throws Exception
        {
        // start from a clean slate - no OpenTelemetry registered
        assertThat(getOpenTelemetryWithNoInit(), nullValue());
        assertThat(TracingHelper.isEnabled(), is(false));

        // Run coherence and assert tracing is enabled.
        runTest(() -> assertThat(TracingHelper.isEnabled(), is(true)), "tracing-enabled-with-zero.xml");

        assertThat(TracingHelper.isEnabled(), is(false));
        assertThat(getOpenTelemetryWithNoInit(), notNullValue());
        assertThat(getOpenTelemetryDelegateNoInit().getClass(),
                   is(OpenTelemetryShim.InternalNoopTelemetry.class));
        }

    /**
     * Validate Coherence will respect any pre-existing OpenTelemetry registrations
     * when initializing the cluster.  Additionally, ensure that Coherence doesn't
     * clobber the pre-existing OpenTelemetry instance when the cluster is stopped.
     */
    @Test
    public void testTelemetryLifecycleNotManagedByCoherence()
        {
        Properties propsTest = getDefaultProperties();
        propsTest.forEach((key, value) -> System.setProperty(key.toString(), value.toString()));

        try
            {
            GlobalOpenTelemetry.get(); // trigger initialization of otel runtime

            // call GlobalTelemetry.get() again as two different
            // ObfuscatedGlobalOpenTelemetry instances will be created
            // during initialization.  We want the second one.
            OpenTelemetry otel = GlobalOpenTelemetry.get();

            // start coherence and validate TracingHelper is active
            // and that the GlobalOpenTelemetry instance is the same
            // meaning Coherence hasn't made any changes
            runTest(() ->
                    {
                    assertThat(TracingHelper.isEnabled(), is(true));
                    assertThat(GlobalOpenTelemetry.get(), is(otel));
                    }, "tracing-enabled-with-zero.xml");

            // Coherence is now shutdown, assert Coherence didn't clobber
            // the existing registered OpenTelemetry
            assertThat(GlobalOpenTelemetry.get(), is(otel));
            }
        finally
            {
            propsTest.forEach((key, value) -> System.clearProperty(key.toString()));
            GlobalOpenTelemetry.resetForTest();
            GlobalEventEmitterProvider.resetForTest();
            }
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Obtain the {@link OpenTelemetry} instance registered with the
     * {@link GlobalOpenTelemetry} via reflection to avoid initializing the
     * runtime.
     *
     * @return the registered {@link OpenTelemetry}, or {@code null} if there
     *         is no such registration
     *
     * @throws Exception if a reflection error occurs
     */
    protected OpenTelemetry getOpenTelemetryWithNoInit()
            throws Exception
        {
        Field otel = GlobalOpenTelemetry.class.getDeclaredField("globalOpenTelemetry");
        otel.setAccessible(true);

        return (OpenTelemetry) otel.get(null);
        }

    /**
     * Obtain the {@link OpenTelemetry} instance delegate registered with the
     * {@link GlobalOpenTelemetry} via reflection to avoid initializing the
     * runtime.
     *
     * @return the registered {@link OpenTelemetry}, or {@code null} if there
     *         is no such registration
     *
     * @throws Exception if a reflection error occurs
     */
    protected OpenTelemetry getOpenTelemetryDelegateNoInit()
        throws Exception
        {
        OpenTelemetry otel = getOpenTelemetryWithNoInit();
        if (otel != null)
            {
            Field fieldDelegate = Class.forName(
                            GlobalOpenTelemetry.class.getName() +
                            "$ObfuscatedOpenTelemetry")
                    .getDeclaredField("delegate");
            fieldDelegate.setAccessible(true);
            otel = (OpenTelemetry) fieldDelegate.get(otel);
            }

        return otel;
        }

    /**
     * Over a 15-second time period, assert no spans are captured.
     */
    protected void assertNoSpans()
        {
        Repetitively.assertThat("Spans incorrectly recorded",
                (Deferred<Boolean>) () ->
                    {
                    List<Span> spans = TestingUtils.extractSpans(m_collectorServer);
                    if (!spans.isEmpty())
                        {
                        Logger.err("Unexpected spans! " + spans);
                        return false;
                        }
                    return true;
                    },
                (Matcher<Boolean>) is(true),
                Timeout.of(15, TimeUnit.SECONDS));
        }

    // ----- data members ---------------------------------------------------

    /**
     * The mock server receiving tracing
     * {@link io.opentelemetry.api.trace.Span spans} generated by these
     * tests.
     */
    protected ClientAndServer m_collectorServer;
    }
