/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.tracing.opentracing;

import com.tangosol.internal.tracing.Span;
import com.tangosol.internal.tracing.SpanContext;

import com.tangosol.util.Base;

import io.opentracing.noop.NoopSpan;

import java.io.Serializable;

import java.util.Map;
import java.util.Objects;

/**
 * {@link Span} adapter for {@code OpenTracing}.
 *
 * @since  14.1.1.0
 * @author rl 11.5.2019
 */
@Deprecated
public class OpenTracingSpan
        implements Span
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a new {@code OpenTracingSpan}.
     *
     * @param span  the {@link io.opentracing.Span} delegate
     *
     * @throws NullPointerException if {@code openTracingSpan} is {@code null}
     */
    public OpenTracingSpan(io.opentracing.Span span)
        {
        Objects.requireNonNull(span, "Parameter span cannot be null");

        f_openTracingSpan = span;
        f_spanContext     = new OpenTracingSpanContext(span.context());
        f_fNoop           = span instanceof NoopSpan;
        }


    // ----- Span interface (internal) --------------------------------------

    @Override
    public Span setMetadata(String sKey, String sValue)
        {
        if (sKey != null)
            {
            f_openTracingSpan.setTag(sKey, sValue);
            }
        return this;
        }

    @Override
    public Span setMetadata(String sKey, long lValue)
        {
        if (sKey != null)
            {
            f_openTracingSpan.setTag(sKey, lValue);
            }
        return this;
        }

    @Override
    public Span setMetadata(String sKey, double dValue)
        {
        if (sKey != null)
            {
            f_openTracingSpan.setTag(sKey, dValue);
            }
        return this;
        }

    @Override
    public Span setMetadata(String sKey, boolean fValue)
        {
        if (sKey != null)
            {
            f_openTracingSpan.setTag(sKey, fValue);
            }
        return this;
        }

    @Override
    public Span log(String sEvent)
        {
        if (sEvent != null && !sEvent.isEmpty())
            {
            f_openTracingSpan.log(sEvent);
            }
        return this;
        }

    @Override
    public Span log(Map<String, ? super Serializable> mapFields)
        {
        if (mapFields != null && !mapFields.isEmpty())
            {
            f_openTracingSpan.log(mapFields);
            }
        return null;
        }

    @Override
    public Span updateName(String sName)
        {
        if (sName != null)
            {
            f_openTracingSpan.setOperationName(sName);
            }
        return this;
        }

    @Override
    public void end()
        {
        f_openTracingSpan.finish();
        }

    @Override
    public SpanContext getContext()
        {
        return f_spanContext;
        }

    @Override
    public boolean isNoop()
        {
        return f_fNoop;
        }

    // ----- Wrapper interface ------------------------------------------

    /**
     * Returns the underlying {@link io.opentracing.Span}.
     *
     * @return the underlying {@link io.opentracing.Span}
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> T underlying()
        {
        return (T) f_openTracingSpan;
        }

    // ----- object methods -------------------------------------------------

    @Override
    public boolean equals(Object o)
        {
        if (this == o)
            {
            return true;
            }
        if (!(o instanceof OpenTracingSpan))
            {
            return false;
            }
        OpenTracingSpan that = (OpenTracingSpan) o;
        return Base.equals(f_openTracingSpan, that.f_openTracingSpan);
        }

    @Override
    public int hashCode()
        {
        return Objects.hash(f_openTracingSpan);
        }

    @Override
    public String toString()
        {
        return "OpenTracingSpan("
               + "Span=" + f_openTracingSpan
               + ')';
        }

    // ----- data members ---------------------------------------------------

    /**
     * The underlying {@code OpenTracing} {@link  io.opentracing.Span span}.
     */
    protected final io.opentracing.Span f_openTracingSpan;

    /**
     * The {@link SpanContext} associated with this {@link Span}.
     */
    protected final OpenTracingSpanContext f_spanContext;

    /**
     * Flag indicating this {@code Span} can be considered a no-op.
     */
    protected final boolean f_fNoop;
    }
