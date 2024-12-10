/*
 * Copyright (c) 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.tracing.opentelemetry;

import com.tangosol.internal.tracing.Span;
import com.tangosol.internal.tracing.SpanContext;

import java.io.Serializable;

import java.util.Map;
import java.util.Objects;

/**
 * {@link Span} adapter for {@code OpenTelemetry}.
 *
 * @author rl 8.25.2023
 * @since  24.03
 */
public class OpenTelemetrySpan
        implements Span
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a new {@code OpenTelemetrySpan}.
     *
     * @param span  the {@link io.opentelemetry.api.trace.Span} delegate
     *
     * @throws NullPointerException if {@code openTracingSpan} is {@code null}
     */
    public OpenTelemetrySpan(io.opentelemetry.api.trace.Span span)
        {
        Objects.requireNonNull(span, "Parameter span cannot be null");

        f_span        = span;
        f_spanContext = new OpenTelemetrySpanContext(span.getSpanContext());
        f_fNoop       = !span.isRecording();
        }
    // ----- Span interface -------------------------------------------------

    @Override
    public Span setMetadata(String sKey, String sValue)
        {
        if (sKey != null)
            {
            f_span.setAttribute(sKey, sValue);
            }

        return this;
        }

    @Override
    public Span setMetadata(String sKey, long lValue)
        {
        if (sKey != null)
            {
            f_span.setAttribute(sKey, lValue);
            }

        return this;
        }

    @Override
    public Span setMetadata(String sKey, double dValue)
        {
        if (sKey != null)
            {
            f_span.setAttribute(sKey, dValue);
            }

        return this;
        }

    @Override
    public Span setMetadata(String sKey, boolean fValue)
        {
        if (sKey != null)
            {
            f_span.setAttribute(sKey, fValue);
            }

        return this;
        }

    @Override
    public Span log(String sEvent)
        {
        if (sEvent != null && !sEvent.isEmpty())
            {
            f_span.addEvent(sEvent);
            }

        return this;
        }

    @Override
    public Span log(Map<String, ? super Serializable> mapFields)
        {
        if (mapFields != null && !mapFields.isEmpty())
            {
            StringBuilder sb     = new StringBuilder();
            int           nCount = mapFields.size();

            for (Map.Entry<String, ? super Serializable> e : mapFields.entrySet())
                {
                sb.append(e.getKey()).append(": ").append(e.getValue().toString());
                if (--nCount != 0)
                    {
                    sb.append(", ");
                    }
                }

            f_span.addEvent(sb.toString());
            }

        return this;
        }

    @Override
    public Span updateName(String sName)
        {
        if (sName != null)
            {
            f_span.updateName(sName);
            }

        return this;
        }

    @Override
    public void end()
        {
        f_span.end();
        }

    public SpanContext getContext()
        {
        return f_spanContext;
        }

    // ----- NoopAware interface --------------------------------------------

    public boolean isNoop()
        {
        return f_fNoop;
        }

    // ----- Wrapper interface ----------------------------------------------

    @SuppressWarnings("unchecked")
    public <T> T underlying()
        {
        return (T) f_span;
        }

    // ----- Object methods -------------------------------------------------

    @Override
    public boolean equals(Object o)
        {
        if (this == o)
            {
            return true;
            }
        if (!(o instanceof OpenTelemetrySpan))
            {
            return false;
            }

        OpenTelemetrySpan that = (OpenTelemetrySpan) o;

        return f_fNoop == that.f_fNoop &&
               Objects.equals(f_span, that.f_span) &&
               Objects.equals(f_spanContext, that.f_spanContext);
        }

    @Override
    public int hashCode()
        {
        return Objects.hash(f_span, f_spanContext, f_fNoop);
        }

    @Override
    public String toString()
        {
        return "OpenTelemetrySpan{" +
               "Span=" + f_span +
               ", SpanContext=" + f_spanContext +
               ", Noop=" + f_fNoop +
               '}';
        }

    // ----- data members ---------------------------------------------------

    /**
     * The underlying {@code OpenTelemetry} {@link  io.opentelemetry.api.trace.Span span}.
     */
    protected final io.opentelemetry.api.trace.Span f_span;

    /**
     * The {@link SpanContext} associated with this {@link Span}.
     */
    protected final SpanContext f_spanContext;

    /**
     * Flag indicating this {@code Span} can be considered a no-op.
     */
    protected final boolean f_fNoop;
    }
