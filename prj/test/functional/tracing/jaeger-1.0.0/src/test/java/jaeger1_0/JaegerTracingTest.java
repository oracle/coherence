/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package jaeger1_0;

import com.tangosol.internal.tracing.TracingHelper;

import com.tangosol.internal.tracing.opentracing.AbstractOpenTracingTracer;

import com.tangosol.internal.tracing.opentracing33.OpenTracingTracer;

import io.jaegertracing.internal.JaegerSpan;

import io.opentracing.Span;

import io.opentracing.contrib.tracerresolver.TracerFactory;

import io.opentracing.noop.NoopSpan;

import org.junit.Test;

import tracing.AbstractJaegerTracingTest;

/**
 * Tests to validate Coherence can properly bootstrap with Jaeger+TracerResolver.
 *
 * @author rl 2019
 * @since 14.1.1.0
 */
public class JaegerTracingTest
        extends AbstractJaegerTracingTest
    {
    // ----- AbstractJaegerTracingTest methods ------------------------------

    @Override
    protected Class<? extends AbstractOpenTracingTracer> getExcpectedTracerType()
        {
        return OpenTracingTracer.class;
        }

    // ----- test methods ---------------------------------------------------

    /**
     * When coherence tracing is enabled and a usable {@link TracerFactory} is present, tracing should be enabled.
     */
    @Override
    @Test
    public void testIsEnabled()
        {
        super.testIsEnabled();
        }

    /**
     * Verify {@link TracingHelper#getTracer()} returns an instance of {@link OpenTracingTracer}.
     */
    @Override
    @Test
    public void testGetTracer()
        {
        super.testGetTracer();
        }

    /**
     * Verify the act of starting a cluster when tracing is enabled will result in {@link Span spans} being captured.
     */
    @Override
    @Test
    public void testTraceCaptured()
        {
        super.testTraceCaptured();
        }

    /**
     * Verify the act of starting a cluster when tracing is set to {@code 0} will result in no spans being captured.
     */
    @Override
    @Test
    public void testTraceNotCapturedWhenTraceIsZero()
        {
        super.testTraceNotCapturedWhenTraceIsZero();
        }

    /**
     * Verify the act of starting a cluster when tracing is set to {@code 0} will result in no spans being captured.
     */
    @Override
    @Test
    public void testTraceCapturedWhenExplicitAndTraceIsZero()
        {
        super.testTraceCapturedWhenExplicitAndTraceIsZero();
        }

    /**
     * Verify the act of starting a cluster when tracing is set to {@code 0} and an outer span is present,
     * allows tracing spans to be collected.
     */
    @Override
    @Test
    public void testNoTraceCapturedWhenExplicitAndNoOuterSpanAndTraceIsZero()
        {
        super.testNoTraceCapturedWhenExplicitAndNoOuterSpanAndTraceIsZero();
        }

    /**
     * Validate the {@link Span} produced when tracing is zero and there is no outer span is an
     * instance of {@link NoopSpan}.
     */
    @Override
    @Test
    public void testExpectedSpanTypeWhenTraceIsZeroNoOuterSpan()
        {
        super.testExpectedSpanTypeWhenTraceIsZeroNoOuterSpan();
        }

    /**
     * Validate the {@link Span} produced when tracing is zero and there is an outer span is an
     * instance of {@link JaegerSpan}.
     */
    @Override
    @Test
    public void testExpectedSpanTypeWhenTraceIsZeroWithOuterSpan()
        {
        super.testExpectedSpanTypeWhenTraceIsZeroWithOuterSpan();
        }
    }
