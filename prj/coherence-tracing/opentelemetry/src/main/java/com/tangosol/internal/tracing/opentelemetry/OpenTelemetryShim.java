/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.tracing.opentelemetry;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.internal.tracing.Span;
import com.tangosol.internal.tracing.Tracer;
import com.tangosol.internal.tracing.TracingShim;

import com.tangosol.util.Base;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;

import io.opentelemetry.api.events.GlobalEventEmitterProvider;

import io.opentelemetry.api.trace.TracerProvider;

import io.opentelemetry.context.propagation.ContextPropagators;

import java.io.Closeable;
import java.io.IOException;

import java.lang.reflect.Field;

import java.util.Objects;

/**
 * {@link TracingShim} implementation for {@code OpenTelemetry}.
 *
 * @author rl 8.25.2023
 * @since  24.03
 */
public class OpenTelemetryShim
        implements TracingShim
    {
    // ----- TracingShim interface ------------------------------------------

    @Override
    public Span activateSpan(Span span)
        {
        if (span != null)
            {
            ((io.opentelemetry.api.trace.Span) span.underlying()).makeCurrent();
            }

        return span;
        }

    @Override
    public boolean isNoop()
        {
        return false;
        }

    @Override
    public Control initialize(Dependencies dependencies)
        {
        final TracingShim.Dependencies depsFin = m_dependencies = new TracingShim.DefaultDependencies(dependencies);

        m_tracer = new OpenTelemetryTracer();

        if (isEnabled())
            {
            return null;
            }
        else
            {
            float flSamplingRatio = dependencies.getSamplingRatio();

            if (checkTracingEnabled(flSamplingRatio))
                {
                configureTracingSampling(flSamplingRatio);

                OpenTelemetry underlying = getUnderlying();
                if (underlying != null && !INTERNAL_NOOP.equals(underlying))
                    {
                    return null; // Tracer wasn't registered by Coherence,
                                 // which implies pre-existing Tracer; no control
                    }

                if (INTERNAL_NOOP.equals(underlying))
                    {
                    GlobalOpenTelemetry.resetForTest();
                    GlobalEventEmitterProvider.resetForTest();
                    }

                GlobalOpenTelemetry.get(); // initialize OT
                }

            Logger.finest(() -> "Initialized TracingShim: " + this);

            return new TracingShim.Control()
                {
                public synchronized void close()
                    {
                    if (!m_fClosed)
                        {
                        m_fClosed = true;
                        try
                            {
                            OpenTelemetry ot = (OpenTelemetry) GLOBAL_TELEMETRY_REGISTERED_FIELD.get(null);
                            if (ot != null)
                                {
                                Object oDelegate = OBFUSCATED_TELEMETRY_DELEGATE_FIELD.get(ot);
                                if (oDelegate instanceof Closeable)
                                    {
                                    ((Closeable) oDelegate).close();
                                    }
                                }
                            }
                        catch (IllegalAccessException | IOException e)
                            {
                            throw Base.ensureRuntimeException(e);
                            }

                        GlobalOpenTelemetry.resetForTest();
                        GlobalEventEmitterProvider.resetForTest();
                        GlobalOpenTelemetry.set(INTERNAL_NOOP);
                        }
                    }

                public TracingShim.Dependencies getDependencies()
                    {
                    return depsFin;
                    }

                // ----- data members -----------------------------------

                /**
                 * True once close has been called.
                 */
                private boolean m_fClosed;
                };
            }
        }

    @Override
    public boolean isEnabled()
        {
        return isTelemetryRegistered();
        }

    @Override
    public Tracer getTracer()
        {
        return m_tracer;
        }

    @Override
    public Span getNoopSpan()
        {
        return NOOP_SPAN;
        }

    @Override
    public Span.Builder getNoopSpanBuilder()
        {
        return NOOP_BUILDER;
        }

    @Override
    public Dependencies getDependencies()
        {
        return m_dependencies;
        }

    // ----- Object methods -------------------------------------------------

    @Override
    public boolean equals(Object o)
        {
        if (this == o)
            {
            return true;
            }
        if (!(o instanceof OpenTelemetryShim))
            {
            return false;
            }
        OpenTelemetryShim that = (OpenTelemetryShim) o;
        return Objects.equals(getDependencies(), that.getDependencies())
               && Objects.equals(getTracer(), that.getTracer());
        }

    @Override
    public int hashCode()
        {
        return Objects.hash(getDependencies(), getTracer());
        }

    @Override
    public String toString()
        {
        return "OpenTelemetryShim("
               + "Dependencies=" + getDependencies()
               + ", Tracer=" + getTracer()
               + ')';
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Returns {@code true} if a non-noop {@link OpenTelemetry} instance has been
     * registered with the {@link GlobalOpenTelemetry}.
     *
     * @return {@code true} if a non-noop {@link OpenTelemetry} instance has been
     *         registered with the {@link GlobalOpenTelemetry}
     *
     * @throws RuntimeException if any of the reflection calls fail
     */
    private static boolean isTelemetryRegistered()
        {
        OpenTelemetry otelActual = getUnderlying();

        if (otelActual == null || INTERNAL_NOOP.equals(otelActual))
            {
            return false;
            }

        return !OpenTelemetry.noop().equals(otelActual);
        }

    /**
     * Return the actual {@link OpenTelemetry} instance registered
     * with {@link GlobalOpenTelemetry}.
     *
     * @return the registered {@link OpenTelemetry} instance or {@code null}
     *
     * @throws RuntimeException if any of the reflection calls fail
     */
    private static OpenTelemetry getUnderlying()
        {
        try
            {
            OpenTelemetry otel = (OpenTelemetry) GLOBAL_TELEMETRY_REGISTERED_FIELD.get(null);

            if (otel == null)
                {
                return null;
                }

            return (OpenTelemetry) OBFUSCATED_TELEMETRY_DELEGATE_FIELD.get(otel);
            }
        catch (IllegalAccessException iae)
            {
            throw Base.ensureRuntimeException(iae);
            }
        }

    /**
     * Returns {@code true} if sampling is between {@code 0} and {@code 1}, otherwise {@code false}
     *
     * @param flSamplingRatio  the configured sampling ratio
     *
     * @return {@code true} if sampling is between {@code 0} and {@code 1}, otherwise {@code false}
     */
    private static boolean checkTracingEnabled(float flSamplingRatio)
        {
        return flSamplingRatio >= TRACING_LOWER_SAMPLE_RANGE
               && flSamplingRatio <= TRACING_UPPER_SAMPLE_RANGE;
        }

    /**
     * Determine if the specified system property or environment variable is present.
     *
     * @param sPropOrEnvName  the system property or environment variable name
     *
     * @return {@code true} if the name is already present as a system property or
     * environment variable
     */
    @SuppressWarnings("SameParameterValue")
    private static boolean isSet(String sPropOrEnvName)
        {
        return System.getProperty(sPropOrEnvName) != null
               || System.getenv(sPropOrEnvName) != null;
        }

    /**
     * Configure the sampling for known {@link Tracer tracers}.
     *
     * @param flSamplingRatio  the sampling ratio
     */
    private static void configureTracingSampling(float flSamplingRatio)
        {
        // if the user has already provided these, honor their configuration, otherwise expose our configuration
        if (!isSet(OTEL_SAMPLER_TYPE_PROPERTY))
            {
            System.setProperty(OTEL_SAMPLER_TYPE_PROPERTY, OTEL_SAMPLER_VALUE_PROPERTY);
            System.setProperty(OTEL_TRACES_SAMPLER_ARG_PROPERTY,
                               Float.compare(0.0f, flSamplingRatio) == 0
                               ? "1.0"
                               : String.valueOf(flSamplingRatio));
            }
        }

    // ----- inner class: InternalNoopTelemetry -----------------------------

    /**
     * A no-op {@link OpenTelemetry} implementation.
     */
    public static class InternalNoopTelemetry
            implements OpenTelemetry
        {
        public TracerProvider getTracerProvider()
            {
            return TracerProvider.noop();
            }

        public ContextPropagators getPropagators()
            {
            return ContextPropagators.noop();
            }
        }

    // ----- constants ------------------------------------------------------

    /**
     * A static, no-op, {@link OpenTelemetrySpan}.
     */
    protected static final OpenTelemetrySpan NOOP_SPAN =
            new OpenTelemetrySpan(io.opentelemetry.api.trace.Span.getInvalid());

    /**
     * A static, no-op, {@link OpenTelemetryTracer.SpanBuilder}.
     */
    protected static final OpenTelemetryTracer.SpanBuilder NOOP_BUILDER;

    /**
     * A private static field within {@link GlobalOpenTelemetry} which must be manipulated
     * via reflection to properly support enabling/disabling tracing dynamically.
     */
    protected static final Field GLOBAL_TELEMETRY_REGISTERED_FIELD;

    /**
     * A field within a static inner class within {@link GlobalOpenTelemetry}
     * which contains the actual registered {@link OpenTelemetry} instance.
     */
    protected static final Field OBFUSCATED_TELEMETRY_DELEGATE_FIELD;

    /**
     * Valid lower-range value for probabilistic sampling.
     */
    protected static final float TRACING_LOWER_SAMPLE_RANGE = 0.0f;

    /**
     * Valid upper-range value for probabilistic sampling.
     */
    protected static final float TRACING_UPPER_SAMPLE_RANGE = 1.0f;

    /**
     * System property recognized by {@code OpenTelemetry} to configure the tracing
     * sampler.
     */
    protected static final String OTEL_SAMPLER_TYPE_PROPERTY = "otel.traces.sampler";

    /**
     * The default tracing sampler that Coherence will use when it's configuring
     * the OpenTelemetry runtime.
     */
    protected static final String OTEL_SAMPLER_VALUE_PROPERTY = "parentbased_traceidratio";

    /**
     * System property recognized by {@code OpenTelemetry} to pass arguments to
     * configure the defined sampler.
     */
    protected static final String OTEL_TRACES_SAMPLER_ARG_PROPERTY = "otel.traces.sampler.arg";

    /**
     * Internal no-op {@link OpenTelemetry} instance.  This will be used when
     * Coherence manages the lifecycle of {@link OpenTelemetry} to be able to
     * discern from a user-installed no-op {@link OpenTelemetry} instance.
     */
    protected static final OpenTelemetry INTERNAL_NOOP = new InternalNoopTelemetry();

    // ----- data members ---------------------------------------------------

    /**
     * The tracing configuration {@link TracingShim.Dependencies dependences}.
     */
    protected TracingShim.Dependencies m_dependencies;

    /**
     * The {@link Tracer} that Coherence will be using.
     */
    protected com.tangosol.internal.tracing.Tracer m_tracer;

    // ----- static initializer ---------------------------------------------

    static
        {
        // initialize the static, no-op SpanBuilder
        TracerProvider                    p = TracerProvider.noop();
        io.opentelemetry.api.trace.Tracer t = p.get("oracle.coherence");

        NOOP_BUILDER = new OpenTelemetryTracer.SpanBuilder(t.spanBuilder("noop"))
            {
            public Span startSpan()
                {
                return NOOP_SPAN;
                }
            };

        // Get our hook into the internals of GlobalOpenTelemetry to
        // allow dynamic enable/disable of tracing.
        Field fieldRegistered = null;
        try
            {
            fieldRegistered = GlobalOpenTelemetry.class.getDeclaredField("globalOpenTelemetry");
            fieldRegistered.setAccessible(true);
            }
        catch (NoSuchFieldException e)
            {
            Logger.err("An incompatible version of the OpenTelemetry API has been detected "
                       + "on the classpath. Tracing will be disabled.");
            }
        GLOBAL_TELEMETRY_REGISTERED_FIELD = fieldRegistered;

        Field fieldDelegate;
        try
            {
            fieldDelegate = Class.forName(GlobalOpenTelemetry.class.getName() + "$ObfuscatedOpenTelemetry")
                    .getDeclaredField("delegate");
            fieldDelegate.setAccessible(true);
            OBFUSCATED_TELEMETRY_DELEGATE_FIELD = fieldDelegate;
            }
        catch (ClassNotFoundException | NoSuchFieldException e)
            {
            throw new RuntimeException(e);
            }
        }
    }
