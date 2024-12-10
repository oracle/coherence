/*
 * Copyright (c) 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.tracing.opentelemetry;

import com.tangosol.internal.tracing.SpanContext;

import java.util.Objects;

/**
 * {@link SpanContext} adapter for {@code OpenTracing}.
 *
 * @author rl 8.25.2023
 * @since  24.03
 */
public class OpenTelemetrySpanContext
        implements SpanContext
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a new {@code OpenTracingSpanContext}.
     *
     * @param spanContext  the {@link io.opentelemetry.api.trace.SpanContext} delegate
     *
     * @throws NullPointerException if {@code spanContext} is {@code null}
     */
    public OpenTelemetrySpanContext(io.opentelemetry.api.trace.SpanContext spanContext)
        {
        Objects.requireNonNull(spanContext, "Parameter openTracingSpanContext cannot be null");

        f_spanContext = spanContext;
        f_fNoop       = OpenTelemetryShimLoader.Noops.isNoop(spanContext);
        }

    // ----- SpamContext interface ------------------------------------------

    @Override
    public String getTraceId()
        {
        return f_spanContext.getTraceId();
        }

    @Override
    public String getSpanId()
        {
        return f_spanContext.getSpanId();
        }

    // ----- NoopAware interface --------------------------------------------

    @Override
    public boolean isNoop()
        {
        return f_fNoop;
        }

    // ----- Wrapper interface ----------------------------------------------

    @Override
    @SuppressWarnings("unchecked")
    public <T> T underlying()
        {
        return (T) f_spanContext;
        }

    // ----- Object methods -------------------------------------------------

    @Override
    public boolean equals(Object o)
        {
        if (this == o)
            {
            return true;
            }
        if (!(o instanceof OpenTelemetrySpanContext))
            {
            return false;
            }

        OpenTelemetrySpanContext that = (OpenTelemetrySpanContext) o;

        return f_fNoop == that.f_fNoop && Objects.equals(f_spanContext, that.f_spanContext);
        }

    @Override
    public int hashCode()
        {
        return Objects.hash(f_spanContext, f_fNoop);
        }

    @Override
    public String toString()
        {
        return "OpenTelemetrySpanContext{" +
               "SpanContext=" + f_spanContext +
               ", Noop=" + f_fNoop +
               '}';
        }

    // ----- data members ---------------------------------------------------

    /**
     * The underlying {@code OpenTracing} {@link io.opentelemetry.api.trace.SpanContext}.
     */
    protected final io.opentelemetry.api.trace.SpanContext f_spanContext;

    /**
     * Flag indicating this {@code SpanContext} can be considered a no-op.
     */
    protected final boolean f_fNoop;
    }
