/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.tracing.opentelemetry;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.coherence.config.Config;
import com.tangosol.internal.tracing.TracingShim;
import com.tangosol.internal.tracing.TracingShimLoader;

import com.tangosol.util.Base;

import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.TracerProvider;

import io.opentelemetry.context.Scope;

import java.text.MessageFormat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A {@link TracingShimLoader} to load/enable the {@code OpenTelemetry}
 * runtime for use with Coherence.
 *
 * @author rl 8.25.2023
 * @since  24.03
 */
public class OpenTelemetryShimLoader
        implements TracingShimLoader
    {
    // ----- TracingShimLoader interface ------------------------------------

    @Override
    public TracingShim loadTracingShim()
        {
        if (ENABLED)
            {
            return ensureDependenciesPresent()
                   ? new OpenTelemetryShim()
                   : TracingShim.Noop.INSTANCE;
            }

        return null;
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Ensures the expected OpenTracing classes are available and are compatible with
     * Coherence's tracing implementation.
     *
     * @return {@code true} if the classpath looks good, otherwise {@code false}
     */
    protected boolean ensureDependenciesPresent()
        {
        ClassLoader  loader          = Base.getContextClassLoader();
        List<String> listMissingDeps = null;

        // check all dependencies are available on the classpath
        for (Map.Entry<String, String> entry : EXPECTED_CLASSES.entrySet())
            {
            String sClassName = entry.getKey();

            if (!ensureClassPresent(sClassName, loader))
                {
                if (listMissingDeps == null)
                    {
                    listMissingDeps = new ArrayList<>(EXPECTED_CLASSES.size());
                    }
                listMissingDeps.add(entry.getValue());
                }
            }

        if (listMissingDeps != null)
            {
            Logger.finest(MessageFormat.format("OpenTelemetry support will not be enabled.  "
                    + "The following dependencies appear to be missing: {0}", listMissingDeps));
            return false;
            }

        return true;
        }

    /**
     * Verify the specified class is on the classpath.
     *
     * @param sClassName   the class name to verify
     * @param classLoader  the {@link ClassLoader} to use
     *
     * @return {@code true} if the class is available, otherwise {@code false}.
     */
    protected boolean ensureClassPresent(String sClassName, ClassLoader classLoader)
        {
        try
            {
            Class.forName(sClassName, false, classLoader);
            }
        catch (Exception e)
            {
            return false;
            }
        return true;
        }

    // ----- constants ------------------------------------------------------

    /**
     * {@link Map} of class names and their associated maven dependencies that should be on the classpath
     * in order to enable tracing.
     */
    protected static final Map<String, String> EXPECTED_CLASSES = Map.of(
            "io.opentelemetry.api.OpenTelemetry",    "opentelemetry-api",
            "io.opentelemetry.context.Context",      "opentelemetry-context",
            "io.opentelemetry.sdk.OpenTelemetrySdk", "opentelemetry-sdk");

    /**
     * A flag that allows the explicit disabling of OpenTelemetry.
     */
    protected static final boolean ENABLED =
            Config.getBoolean("com.oracle.coherence.opentelemetry.enabled", true);

    // ----- inner class: Noops ---------------------------------------------
    /**
     * Utility class for determining what OpenTelemetry types are Noop types.
     * Stored in static class to defer loading of OpenTelemetry types.
     */
    static final class Noops
        {
        // ---- api ---------------------------------------------------------

        /**
         * Return {@code true} if the provided {@link Tracer} is
         * a noop implementation.
         *
         * @param tracer  the {@link Tracer} to test
         *
         * @return {@code true} the provided {@link Tracer} is
         *         a noop implementation
         */
        static boolean isNoop(Tracer tracer)
            {
            return f_clzNoopTracer.isInstance(tracer);
            }

        /**
         * Return {@code true} if the provided {@link SpanContext} is
         * a noop implementation.
         *
         * @param spanContext  the {@link SpanContext} to test
         *
         * @return {@code true} the provided {@link SpanContext} is
         *         a noop implementation
         */
        static boolean isNoop(SpanContext spanContext)
            {
            return !spanContext.isValid();
            }

        /**
         * Return {@code true} if the provided {@link Scope} is
         * a noop implementation.
         *
         * @param scope  the {@link Scope} to test
         *
         * @return {@code true} the provided {@link Scope} is
         *         a noop implementation
         */
        static boolean isNoop(Scope scope)
            {
            return f_clzNoopScope.isInstance(scope);
            }

        // ----- constants --------------------------------------------------

        /**
         * Expected type for a noop {@link Tracer}.
         */
        static final Class<? extends Tracer> f_clzNoopTracer;

        /**
         * Expected type for a noop {@link Scope}.
         */
        static final Class<? extends Scope> f_clzNoopScope;


        // ----- initializer ------------------------------------------------

        static
            {
            TracerProvider p = TracerProvider.noop();
            Tracer         t = p.get("oracle.coherence");

            f_clzNoopTracer         = t.getClass();
            f_clzNoopScope          = Scope.noop().getClass();
            }
        }
    }
