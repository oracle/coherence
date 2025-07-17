/*
 * Copyright (c) 2023, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package opentelemetry.core;

import com.github.tomakehurst.wiremock.WireMockServer;

import com.oracle.bedrock.deferred.Deferred;

import com.oracle.bedrock.options.Timeout;

import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.testsupport.deferred.Repetitively;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.internal.tracing.TracingHelper;

import com.tangosol.net.NamedCache;

import com.tangosol.net.cache.TypeAssertion;

import io.opentelemetry.api.GlobalOpenTelemetry;

import io.opentelemetry.api.trace.Tracer;

import io.opentelemetry.context.Scope;

import io.opentelemetry.proto.trace.v1.Span;

import java.util.List;
import java.util.Properties;

import java.util.concurrent.TimeUnit;

import org.hamcrest.Matcher;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import tracing.AbstractTracingIT;

import static org.hamcrest.CoreMatchers.is;

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
        props.setProperty("otel.traces.exporter",        "otlp");
        props.setProperty("otel.exporter.otlp.protocol", "http/protobuf");

        // for testing we install a Global tracing instance
        props.setProperty("otel.java.global-autoconfigure.enabled", "true");

        return props;
        }

    // ---- test lifecycle --------------------------------------------------

    @Before
    public void _before()
        {
        m_collectorServer = TestingUtils.createMockServer(4318);
        }

    @After
    public void _after()
        {
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
        WireMockServer server = m_collectorServer;
        runTest(() ->
                Eventually.assertDeferred("No spans recorded",
                    () -> !TestingUtils.extractSpans(server).isEmpty(),
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

                io.opentelemetry.api.trace.Span span = tracer.spanBuilder("test").startSpan();

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

                WireMockServer server = m_collectorServer;
                Eventually.assertDeferred("No spans recorded",
                        () ->
                            {
                            List<Span> spans =
                                    TestingUtils.extractSpans(server);

                            if (spans.isEmpty())
                                {
                                return false;
                                }

                            Logger.finest("Captured spans: " + spans);

                            return spans.stream().anyMatch(span1 -> span1.getName().equals("test"));
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
     */
    @Test
    public void testTelemetryLifecycleManagedByCoherence()
        {
        // start from a clean slate - no OpenTelemetry registered
        assertThat(TracingHelper.isEnabled(), is(false));

        // Run coherence and assert tracing is enabled.
        runTest(() -> assertThat(TracingHelper.isEnabled(), is(true)), "tracing-enabled-with-zero.xml");

        assertThat(TracingHelper.isEnabled(), is(false));
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Over a 15-second time period, assert no spans are captured.
     */
    protected void assertNoSpans()
        {
        WireMockServer server = m_collectorServer;

        Repetitively.assertThat("Spans incorrectly recorded",
                (Deferred<Boolean>) () ->
                    {
                    List<Span> spans = TestingUtils.extractSpans(server);
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
    protected WireMockServer m_collectorServer;
    }
