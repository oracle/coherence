/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.tracing.opentracing;

import com.tangosol.internal.tracing.SpanContext;

import com.tangosol.util.Base;

import io.opentracing.noop.NoopSpanContext;

import java.util.Objects;

/**
 * {@link SpanContext} adapter for {@code OpenTracing}.
 *
 * @since  14.1.1.0
 * @author rl 11.5.2019
 */
@Deprecated
public class OpenTracingSpanContext
        implements SpanContext
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a new {@code OpenTracingSpanContext}.
     *
     * @param spanContext  the {@link io.opentracing.SpanContext} delegate
     *
     * @throws NullPointerException if {@code spanContext} is {@code null}
     */
    public OpenTracingSpanContext(io.opentracing.SpanContext spanContext)
        {
        Objects.requireNonNull(spanContext, "Parameter openTracingSpanContext cannot be null");

        f_openTracingSpanContext = spanContext;
        f_fNoop                  = spanContext instanceof NoopSpanContext;
        }

    // ----- SpanContext interface ------------------------------------------

    @Override
    public String getTraceId()
        {
        return f_openTracingSpanContext.toTraceId();
        }

    @Override
    public String getSpanId()
        {
        return f_openTracingSpanContext.toSpanId();
        }

    @Override
    public boolean isNoop()
        {
        return f_fNoop;
        }

    // ----- Wrapper interface ----------------------------------------------

    /**
     * Returns the underlying {@link io.opentracing.SpanContext}.
     *
     * @return the underlying {@link io.opentracing.SpanContext}
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> T underlying()
        {
        return (T) f_openTracingSpanContext;
        }

    // ----- object methods -------------------------------------------------

    @Override
    public boolean equals(Object o)
        {
        if (this == o)
            {
            return true;
            }
        if (!(o instanceof OpenTracingSpanContext))
            {
            return false;
            }
        OpenTracingSpanContext that = (OpenTracingSpanContext) o;
        return Base.equals(f_openTracingSpanContext, that.f_openTracingSpanContext);
        }

    @Override
    public int hashCode()
        {
        return Objects.hash(f_openTracingSpanContext);
        }

    @Override
    public String toString()
        {
        return "OpenTracingSpanContext("
               + "SpanContext=" + f_openTracingSpanContext
               + ')';
        }

    // ----- data members ---------------------------------------------------

    /**
     * The underlying {@code OpenTracing} {@link io.opentracing.SpanContext}.
     */
    protected final io.opentracing.SpanContext f_openTracingSpanContext;

    /**
     * Flag indicating this {@code SpanContext} can be considered a no-op.
     */
    protected final boolean f_fNoop;
    }
