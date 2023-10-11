/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.tracing.opentracing;

import com.tangosol.internal.tracing.Scope;

import com.tangosol.util.Base;

import io.opentracing.noop.NoopScopeManager;

import java.util.Objects;

/**
 * {@link Scope} adapter for {@code OpenTracing}.
 *
 * @since  14.1.1.0
 * @author rl 11.5.2019
 */
@Deprecated
public class OpenTracingScope
        implements Scope
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a new {@code OpenTracingScope}.
     *
     * @param scope  the {@link io.opentracing.Scope} delegate
     *
     * @throws NullPointerException if {@code openTracingScope} is {@code null}
     */
    public OpenTracingScope(io.opentracing.Scope scope)
        {
        Objects.requireNonNull(scope, "Parameter scope cannot be null");

        f_openTracingScope = scope;
        f_fNoop            = scope instanceof NoopScopeManager.NoopScope;
        }

    // ----- Closeable interface --------------------------------------------

    @Override
    public void close()
        {
        f_openTracingScope.close();
        }

    // ----- NoopAware interface --------------------------------------------

    @Override
    public boolean isNoop()
        {
        return f_fNoop;
        }

    // ----- Wrapper interface ------------------------------------------

    /**
     * Returns the underlying {@link io.opentracing.Scope}.
     *
     * @return the underlying {@link io.opentracing.Scope}
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> T underlying()
        {
        return (T) f_openTracingScope;
        }

    // ----- object methods -------------------------------------------------

    @Override
    public boolean equals(Object o)
        {
        if (this == o)
            {
            return true;
            }
        if (!(o instanceof OpenTracingScope))
            {
            return false;
            }
        OpenTracingScope that = (OpenTracingScope) o;
        return Base.equals(f_openTracingScope, that.f_openTracingScope);
        }

    @Override
    public int hashCode()
        {
        return Objects.hash(f_openTracingScope);
        }

    @Override
    public String toString()
        {
        return "OpenTracingScope("
               + "Scope=" + f_openTracingScope
               + ')';
        }

    // ----- data members ---------------------------------------------------

    /**
     * The underlying {@code OpenTracing} {@link io.opentracing.Scope}.
     */
    protected final io.opentracing.Scope f_openTracingScope;

    /**
     * Flag indicating this {@code Scope} can be considered a no-op.
     */
    protected final boolean f_fNoop;
    }
