/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.tracing.opentracing;

import com.tangosol.internal.tracing.Scope;
import com.tangosol.internal.tracing.Span;
import com.tangosol.internal.tracing.SpanContext;

import com.tangosol.util.LiteMap;

import io.opentracing.Tracer;

import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMapAdapter;

import io.opentracing.util.GlobalTracer;

import java.util.Map;

/**
 * Implementation of {@link com.tangosol.internal.tracing.Tracer} for {@code OpenTracing 0.33.0}.
 *
 * @since  14.1.1.0
 * @author rl 12.5.2019
 */
@Deprecated
public class OpenTracingTracer
        extends AbstractOpenTracingTracer
    {
    // ----- methods from AbstractOpenTracingTracer -------------------------

    @Override
    public Scope withSpan(Span span)
        {
        Tracer tracer = underlying();
        return new OpenTracingScope(tracer.scopeManager().activate(span.underlying()));
        }

    @Override
    public Span.Builder spanBuilder(String spanName)
        {
        Tracer tracer = underlying();
        return new SpanBuilder(tracer.buildSpan(spanName));
        }

    @Override
    public Map<String, String> inject(SpanContext spanContext)
        {
        LiteMap<String, String> injectTarget = new LiteMap<>();

        GlobalTracer.get().inject(spanContext.underlying(),
                                  Format.Builtin.TEXT_MAP, new TextMapAdapter(injectTarget));

        return injectTarget;
        }

    @Override
    public SpanContext extract(Map<String, String> extractTarget)
        {
        return new OpenTracingSpanContext(GlobalTracer.get().extract(
                Format.Builtin.TEXT_MAP, new TextMapAdapter(extractTarget)));
        }

    // ----- inner class: SpanBuilder ---------------------------------------

    /**
     * {@code OpenTracing 0.33.0} {@link Span.Builder}.
     */
    protected static class SpanBuilder
            extends AbstractOpenTracingSpanBuilder
        {
        // ----- constructors -----------------------------------------------

        /**
         * Constructs the {@link Span.Builder}.
         *
         * @param spanBuilder  the delegate builder.
         */
        public SpanBuilder(Tracer.SpanBuilder spanBuilder)
            {
            super(spanBuilder);
            }

        // ----- methods from AbstractOpenTracingSpanBuilder ----------------

        @Override
        public Span startSpan()
            {
            return new OpenTracingSpan(f_openTracingSpanBuilder.start());
            }
        }
    }
