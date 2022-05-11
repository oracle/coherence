/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package jaeger;

import io.opentracing.Span;

import io.opentracing.noop.NoopSpan;

import org.junit.Test;

/**
 * Tests to validate Coherence can properly bootstrap with Jaeger+TracerResolver.
 *
 * @author rl 2019
 * @since 14.1.1.0
 */
public class JaegerTracingTest
        extends AbstractJaegerTracingTest
    {
    // ----- test methods ---------------------------------------------------

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
     * instance of {@code JaegerSpan}.
     */
    @Override
    @Test
    public void testExpectedSpanTypeWhenTraceIsZeroWithOuterSpan()
        {
        super.testExpectedSpanTypeWhenTraceIsZeroWithOuterSpan();
        }

    // ----- static members -------------------------------------------------

    /**
     * Added in order to ensure that opentracing-noop is added to the module
     * path.
     */
    @SuppressWarnings("unused")
    private static final NoopSpan NOOP = NoopSpan.INSTANCE;
    }
