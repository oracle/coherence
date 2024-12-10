/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.tracing.opentracing;

import com.tangosol.internal.tracing.Span;
import com.tangosol.internal.tracing.Tracer;

import com.tangosol.util.Base;

import io.opentracing.util.GlobalTracer;

import java.util.Objects;

/**
 * Base {@link Tracer} implementation for {@code OpenTracing}.
 *
 * @since  14.1.1.0
 * @author rl 11.5.2019
 */
@Deprecated
public abstract class AbstractOpenTracingTracer
        implements Tracer
    {
    // ----- Tracer interface -----------------------------------------------

    @Override
    public Span getCurrentSpan()
        {
        io.opentracing.Span activeSpan = GlobalTracer.get().activeSpan();
        return activeSpan == null ? null : new OpenTracingSpan(activeSpan);
        }

    // ----- NoopAware interface --------------------------------------------

    /**
     * Always returns {@code false}.
     *
     * @return {@code false}
     */
    @Override
    public boolean isNoop()
        {
        return false;
        }

    // ----- Wrapper interface ------------------------------------------

    /**
     * Returns the underlying {@link io.opentracing.Tracer}.
     *
     * @return the underlying {@link io.opentracing.Tracer}
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> T underlying()
        {
        return (T) GlobalTracer.get();
        }

    // ----- object methods -------------------------------------------------

    @Override
    public boolean equals(Object o)
        {
        if (this == o)
            {
            return true; // common case
            }
        if (!(o instanceof AbstractOpenTracingTracer))
            {
            return false;
            }
        AbstractOpenTracingTracer that = (AbstractOpenTracingTracer) o;
        return Base.equals(underlying(), that.underlying());
        }

    @Override
    public int hashCode()
        {
        return Objects.hash(underlying());
        }

    @Override
    public String toString()
        {
        return "OpenTracingTracer("
               + "Tracer=" + GlobalTracer.get()
               + ')';
        }
    }
