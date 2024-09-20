/*
 * Copyright (c) 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.tracing.opentelemetry;

import com.tangosol.internal.tracing.Scope;
import com.tangosol.internal.tracing.Span;
import com.tangosol.internal.tracing.SpanContext;
import com.tangosol.internal.tracing.Tracer;

import com.tangosol.util.LiteMap;

import io.opentelemetry.api.GlobalOpenTelemetry;

import io.opentelemetry.api.trace.SpanKind;

import io.opentelemetry.context.Context;

import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.context.propagation.TextMapSetter;

import java.time.Instant;

import java.util.Map;
import java.util.Objects;

/**
 * Implementation of {@link com.tangosol.internal.tracing.Tracer} for {@code OpenTelemetry}.
 *
 * @author rl 8.25.2023
 * @since  24.03
 */
public class OpenTelemetryTracer
        implements Tracer
    {
    // ----- Tracer interface -----------------------------------------------

    @Override
    public Span getCurrentSpan()
        {
        io.opentelemetry.api.trace.Span activeSpan = io.opentelemetry.api.trace.Span.current();
        return activeSpan == null || !activeSpan.getSpanContext().isValid() ? null : new OpenTelemetrySpan(activeSpan);
        }

    @Override
    public Scope withSpan(Span span)
        {
        return new OpenTelemetryScope(((io.opentelemetry.api.trace.Span) span.underlying()).makeCurrent());
        }

    @Override
    public Span.Builder spanBuilder(String spanName)
        {
        return new SpanBuilder(getTracer().spanBuilder(spanName));
        }

    @Override
    public Map<String, String> inject(SpanContext spanContext)
        {
        LiteMap<String, String> injectTarget = new LiteMap<>();
        TextMapPropagator       propagator   = GlobalOpenTelemetry.getPropagators().getTextMapPropagator();

        propagator.inject(Context.current(), injectTarget, new MapSetter());

        return injectTarget;
        }

    @Override
    public SpanContext extract(Map<String, String> carrier)
        {
        LiteMap<String, String> extractTarget = new LiteMap<>();
        TextMapPropagator       propagator    = GlobalOpenTelemetry.getPropagators().getTextMapPropagator();

        return new OpenTelemetrySpanContext(io.opentelemetry.api.trace.Span.fromContext(
                propagator.extract(Context.current(), extractTarget, new MapGetter()))
                                                    .getSpanContext());
        }

    // ----- NoopAware interface --------------------------------------------

    @Override
    public boolean isNoop()
        {
        return OpenTelemetryShimLoader.Noops.isNoop(getTracer());
        }

    // ----- Wrapper interface ----------------------------------------------

    @SuppressWarnings("unchecked")
    @Override
    public <T> T underlying()
        {
        return (T) getTracer();
        }

    // ----- Object methods -------------------------------------------------

    @Override
    public String toString()
        {
        return "OpenTelemetryTracer()";
        }

    // ----- helper methods -------------------------------------------------

    private io.opentelemetry.api.trace.Tracer getTracer()
        {
        return GlobalOpenTelemetry.getTracer(SCOPE_NAME);
        }

    // ----- inner class SpanBuilder ----------------------------------------

    protected static class SpanBuilder
            implements Span.Builder
        {
        // ----- constructors -----------------------------------------------

        public SpanBuilder(io.opentelemetry.api.trace.SpanBuilder spanBuilder)
            {
            Objects.requireNonNull(spanBuilder, "Parameter spanBuilder cannot be null");
            f_spanBuilder = spanBuilder;
            }


        // ----- Span.Builder interface -------------------------------------

        @Override
        public Span.Builder setParent(Span parent)
            {
            if (parent != null)
                {
                f_spanBuilder.setParent(Context.current().with(parent.underlying()));
                }

            return this;
            }

        @Override
        public Span.Builder setParent(SpanContext remoteParent)
            {
            if (remoteParent != null)
                {
                f_spanBuilder.setParent(Context.current().with(io.opentelemetry.api.trace.Span.wrap(remoteParent.underlying())));
                }

            return this;
            }

        @Override
        public Span.Builder setNoParent()
            {
            f_spanBuilder.setNoParent();

            return this;
            }

        @Override
        public Span.Builder withMetadata(String sKey, String sValue)
            {
            if (sKey != null && !sKey.isEmpty())
                {
                if (SPAN_KIND.equals(sKey))
                    {
                    f_spanBuilder.setSpanKind(SpanKind.valueOf(sValue.toUpperCase()));
                    }
                else
                    {
                    f_spanBuilder.setAttribute(sKey, sValue);
                    }
                }

            return this;
            }

        @Override
        public Span.Builder withMetadata(String sKey, boolean fValue)
            {
            if (sKey != null && !sKey.isEmpty())
                {
                f_spanBuilder.setAttribute(sKey, fValue);
                }

            return this;
            }

        @Override
        public Span.Builder withMetadata(String sKey, long lValue)
            {
            if (sKey != null && !sKey.isEmpty())
                {
                f_spanBuilder.setAttribute(sKey, lValue);
                }

            return this;
            }

        @Override
        public Span.Builder withMetadata(String sKey, double dValue)
            {
            if (sKey != null && !sKey.isEmpty())
                {
                f_spanBuilder.setAttribute(sKey, dValue);
                }

            return this;
            }

        @Override
        public Span.Builder withAssociation(String sLabel, SpanContext associatedContext)
            {
            f_spanBuilder.addLink(associatedContext.underlying());

            return this;
            }

        @Override
        public Span.Builder setStartTimestamp(long ldtStartTime)
            {
            f_spanBuilder.setStartTimestamp(Instant.ofEpochMilli(ldtStartTime));

            return this;
            }

        @Override
        public Span startSpan()
            {
            return new OpenTelemetrySpan(f_spanBuilder.startSpan());
            }

        // ----- constants --------------------------------------------------

        /**
         * Special key to translate span.kind metadata to otel SpanKind.
         */
        private static final String SPAN_KIND = "span.kind";

        // ----- data members -----------------------------------------------

        /**
         * The internal {@link io.opentelemetry.api.trace.SpanBuilder}.
         */
        private final io.opentelemetry.api.trace.SpanBuilder f_spanBuilder;
        }

    // ----- inner class: MapGetter -----------------------------------------

    /**
     * Simple {@link TextMapGetter} implementation for context propagation.
     */
    private static final class MapGetter
            implements TextMapGetter<Map<String, String>>
        {
        // ----- TextMapGetter interface ------------------------------------

        @Override
        public Iterable<String> keys(Map<String, String> carrier)
            {
            return carrier.keySet();
            }

        @Override
        public String get(Map<String, String> carrier, String sKey)
            {
            return carrier == null ? null : carrier.get(sKey);
            }
        }

    // ----- inner class: MapGetter -----------------------------------------

    /**
     * Simple {@link TextMapSetter} implementation for context propagation.
     */
    private static final class MapSetter
            implements TextMapSetter<Map<String, String>>
        {
        // ----- TextMapSetter interface ------------------------------------

        @Override
        public void set(Map<String, String> carrier, String sKey, String sValue)
            {
            if (carrier != null)
                {
                carrier.put(sKey, sValue);
                }
            }
        }

    // ----- constants ------------------------------------------------------

    /**
     * The scope name that the {@code Coherence} {@code OpenTelemetry} tracer
     * will use.
     */
    public static final String SCOPE_NAME = "oracle.coherence";
    }
