/*
 * Copyright (c) 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.tracing.opentelemetry;

import com.tangosol.internal.tracing.Scope;

import java.util.Objects;

/**
 * {@link Scope} adapter for {@code OpenTelemetry}.
 *
 * @author rl 8.25.2023
 * @since  24.03
 */
public class OpenTelemetryScope
        implements Scope
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a new {@code OpenTelemetryScope}.
     *
     * @param scope  the {@link io.opentelemetry.context.Scope} delegate
     *
     * @throws NullPointerException if {@code scope} is {@code null}
     */
    public OpenTelemetryScope(io.opentelemetry.context.Scope scope)
        {
        Objects.requireNonNull(scope, "Parameter scope cannot be null");

        f_scope = scope;
        f_fNoop = OpenTelemetryShimLoader.Noops.isNoop(scope);
        }

    // ----- Scope interface ------------------------------------------------

    @Override
    public void close()
        {
        f_scope.close();
        }

    // ----- NoopAware interface --------------------------------------------

    public boolean isNoop()
        {
        return f_fNoop;
        }

    // ----- Wrapper interface ----------------------------------------------

    @Override
    public <T> T underlying()
        {
        return null;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The underlying {@code OpenTracing} {@link io.opentelemetry.context.Scope}.
     */
    protected final io.opentelemetry.context.Scope f_scope;

    /**
     * Flag indicating this {@code Scope} can be considered a no-op.
     */
    protected final boolean f_fNoop;
    }
