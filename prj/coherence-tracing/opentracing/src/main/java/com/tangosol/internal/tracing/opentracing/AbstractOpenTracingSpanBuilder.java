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

import java.util.Objects;

/**
 * {@link Span.Builder} adapter for {@code OpenTracing}.
 *
 * @since  14.1.1.0
 * @author rl 11.5.2019
 */
@Deprecated
public abstract class AbstractOpenTracingSpanBuilder
        implements Span.Builder
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a new {@code OpenTracingSpanBuilder}.
     *
     * @param spanBuilder  the {@link io.opentracing.Tracer.SpanBuilder} delegate
     *
     * @throws NullPointerException if {@code openTracingSpanBuilder} is {@code null}
     */
    public AbstractOpenTracingSpanBuilder(io.opentracing.Tracer.SpanBuilder spanBuilder)
        {
        Objects.requireNonNull(spanBuilder, "Parameter spanBuilder cannot be null");

        this.f_openTracingSpanBuilder = spanBuilder;
        }

    // ----- Tracer.SpanBuilder interface -----------------------------------

    @Override
    public Span.Builder setParent(Span parent)
        {
        if (parent != null)
            {
            f_openTracingSpanBuilder.asChildOf((io.opentracing.Span) parent.underlying());
            }
        return this;
        }

    @Override
    public Span.Builder setParent(SpanContext remoteParent)
        {
        if (remoteParent != null)
            {
            f_openTracingSpanBuilder.asChildOf((io.opentracing.SpanContext) remoteParent.underlying());
            }
        return this;
        }

    @Override
    public Span.Builder setNoParent()
        {
        f_openTracingSpanBuilder.ignoreActiveSpan();
        return this;
        }

    @Override
    public Span.Builder withMetadata(String sKey, String sValue)
        {
        f_openTracingSpanBuilder.withTag(sKey, sValue);
        return this;
        }

    @Override
    public Span.Builder withMetadata(String sKey, boolean fValue)
        {
        f_openTracingSpanBuilder.withTag(sKey, fValue);
        return this;
        }

    @Override
    public Span.Builder withMetadata(String sKey, long lValue)
        {
        f_openTracingSpanBuilder.withTag(sKey, lValue);
        return this;
        }

    @Override
    public Span.Builder withMetadata(String sKey, double dValue)
        {
        f_openTracingSpanBuilder.withTag(sKey, dValue);
        return this;
        }

    @Override
    public Span.Builder withAssociation(String sLabel, SpanContext associatedContext)
        {
        f_openTracingSpanBuilder.addReference(sLabel, associatedContext.underlying());
        return this;
        }

    @Override
    public Span.Builder setStartTimestamp(long ldtStartTime)
        {
        f_openTracingSpanBuilder.withStartTimestamp(ldtStartTime);
        return this;
        }

    // ----- object methods -------------------------------------------------

    @Override
    public boolean equals(Object o)
        {
        if (this == o)
            {
            return true;
            }
        if (!(o instanceof AbstractOpenTracingSpanBuilder))
            {
            return false;
            }
        AbstractOpenTracingSpanBuilder that = (AbstractOpenTracingSpanBuilder) o;
        return Base.equals(f_openTracingSpanBuilder, that.f_openTracingSpanBuilder);
        }

    @Override
    public int hashCode()
        {
        return Objects.hash(f_openTracingSpanBuilder);
        }

    @Override
    public String toString()
        {
        return "OpenTracingSpanBuilder("
               + "SpanBuilder=" + f_openTracingSpanBuilder
               + ')';
        }

    // ----- data members ---------------------------------------------------

    /**
     * The underlying {@code OpenTracing} {@link io.opentracing.Tracer.SpanBuilder}.
     */
    protected final io.opentracing.Tracer.SpanBuilder f_openTracingSpanBuilder;
    }
