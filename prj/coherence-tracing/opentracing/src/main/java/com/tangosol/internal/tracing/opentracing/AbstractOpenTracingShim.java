/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.tracing.opentracing;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.internal.tracing.Span;
import com.tangosol.internal.tracing.TracingShim;

import com.tangosol.util.Base;

import io.opentracing.Tracer;

import io.opentracing.contrib.tracerresolver.TracerResolver;

import io.opentracing.noop.NoopSpan;
import io.opentracing.noop.NoopTracerFactory;

import io.opentracing.util.GlobalTracer;

import java.io.Closeable;
import java.io.IOException;

import java.util.Objects;

import java.util.function.Supplier;

/**
 * Base class for {@code OpenTracing} {@link TracingShim shims}.
 *
 * @since  14.1.1.0
 * @author rl 12.5.2019
 */
@Deprecated
public abstract class AbstractOpenTracingShim
        implements TracingShim
    {
    // ----- AbstractOpenTracingShim methods --------------------------------

    /**
     * {@code true} if the resolved tracer is successfully registered otherwise returns {@code false}.
     *
     * @param tracer  the {@link io.opentracing.Tracer} to register
     *
     * @return {@code true} if the resolved tracer is successfully registered otherwise returns {@code false}
     *
     * @throws Exception if an error occurs during registration
     */
    protected abstract boolean registerTracer(io.opentracing.Tracer tracer) throws Exception;

    /**
     * Callback which will be invoked when the {@link Control} is closed.
     */
    protected abstract void onControlClose();

    /**
     * Return the API version this shim requires.
     *
     * @return the API version this shim requires
     */
    protected abstract String getApiVersion();

    /**
     * Return a new {@link com.tangosol.internal.tracing.Tracer} instance.
     *
     * @return a new {@link com.tangosol.internal.tracing.Tracer} instance
     */
    protected abstract com.tangosol.internal.tracing.Tracer createTracer();

    // ----- TracingShim interface ------------------------------------------

    @Override
    public Span activateSpan(Span span)
        {
        if (span != null)
            {
            ((io.opentracing.Tracer) m_tracer.underlying()).scopeManager().activate(span.underlying());
            }

        return span;
        }

    /**
     * Bootstraps the {@code OpenTracing} runtime.
     *
     * @param dependencies  {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public TracingShim.Control initialize(TracingShim.Dependencies dependencies)
        {
        final TracingShim.Dependencies depsFin = m_dependencies = new TracingShim.DefaultDependencies(dependencies);

        m_tracer = createTracer();

        if (isEnabled())
            {
            return null;
            }
        else
            {
            float flSamplingRatio = dependencies.getSamplingRatio();

            // if possible, communicate the sampling ratio to the known tracers
            if (checkTracingEnabled(flSamplingRatio))
                {
                configureTracingSampling(flSamplingRatio);

                Tracer tracer = TracerResolver.resolveTracer(Base.getContextClassLoader());
                if (tracer != null)
                    {
                    try
                        {
                        if (!registerTracer(tracer))
                            {
                            return null; // Tracer wasn't registered which implies pre-existing Tracer; no control
                            }
                        }
                    catch (Exception e)
                        {
                        Logger.finest(() -> "Unexpected exception during Tracer registration:", e);
                        return null;
                        }
                    }
                }

            final Tracer tracerFin = GlobalTracer.get();

            Logger.finest(() -> "Initialized TracingShim: " + this);

            return new TracingShim.Control()
                {
                public synchronized void close()
                    {
                    if (!m_fClosed)
                        {
                        m_fClosed = true;
                        //noinspection ConstantConditions
                        if (tracerFin instanceof Closeable)
                            {
                            try
                                {
                                ((Closeable) tracerFin).close();
                                }
                            catch (IOException e)
                                {
                                Supplier<String> supplierMsg = () ->
                                        String.format("Exception raised closing tracer [%s]:", tracerFin.getClass().getName());
                                Logger.finest(supplierMsg, e);
                                }
                            }

                        onControlClose();
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
                protected boolean m_fClosed;
                };
            }
        }

    @Override
    public boolean isEnabled()
        {
        return GlobalTracer.isRegistered();
        }

    @Override
    public com.tangosol.internal.tracing.Tracer getTracer()
        {
        return m_tracer;
        }

    @Override
    public com.tangosol.internal.tracing.Span getNoopSpan()
        {
        return NOOP_SPAN;
        }

    @Override
    public com.tangosol.internal.tracing.Span.Builder getNoopSpanBuilder()
        {
        return NOOP_BUILDER;
        }

    @Override
    public TracingShim.Dependencies getDependencies()
        {
        return m_dependencies;
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

    /**
     * Determines if tracing should be enabled.
     *
     * @param flSamplingRatio  the configured sampling ratio
     *
     * @return {@code true} if sampling is between {@code 0} and {@code 1}, otherwise {@code false}
     */
    private static boolean checkTracingEnabled(float flSamplingRatio)
        {
        return flSamplingRatio >= TRACING_LOWER_SAMPLE_RANGE && flSamplingRatio <= TRACING_UPPER_SAMPLE_RANGE;
        }

    /**
     * Configure the sampling for known {@link Tracer tracers}.
     *
     * @param flSamplingRatio  the sampling ratio
     */
    private static void configureTracingSampling(float flSamplingRatio)
        {
        // if the user has already provided these, honor their configuration, otherwise expose our configuration
        if (!isSet(JAEGER_SAMPLER_TYPE_PROPERTY))
            {
            System.setProperty(JAEGER_SAMPLER_TYPE_PROPERTY, JAEGER_DEFAULT_SAMPLER_TYPE);
            System.setProperty(JAEGER_SAMPLER_VALUE_PROPERTY,
                               Float.compare(0.0f, flSamplingRatio) == 0
                               ? "1.0"
                               : String.valueOf(flSamplingRatio));
            }
        // TODO(rlubke): Zipkin support
        }

    /**
     * Determine if the specified system property or environment variable is present.
     *
     * @param sPropOrEnvName  the system property or environment variable name
     *
     * @return {@code true} if the name is already present as a system property or
     * environment variable
     */
    @SuppressWarnings("SameParameterValue") // TODO(rlubke): this can be removed once zipkin support is added
    private static boolean isSet(String sPropOrEnvName)
        {
        return System.getProperty(sPropOrEnvName) != null
               || System.getenv(sPropOrEnvName) != null;
        }

    // ----- object methods -------------------------------------------------

    @Override
    public boolean equals(Object o)
        {
        if (this == o)
            {
            return true;
            }
        if (!(o instanceof AbstractOpenTracingShim))
            {
            return false;
            }
        AbstractOpenTracingShim that = (AbstractOpenTracingShim) o;
        return getDependencies().equals(that.getDependencies())
               && getTracer().equals(that.getTracer());
        }

    @Override
    public int hashCode()
        {
        return Objects.hash(getDependencies(), getTracer());
        }

    @Override
    public String toString()
        {
        return "OpenTracingShim("
               + "API-Version=" + getApiVersion()
               + ", Dependencies=" + getDependencies()
               + ", Tracer=" + getTracer()
               + ')';
        }

    // ----- data members ---------------------------------------------------

    /**
     * The dependencies.
     */
    protected TracingShim.Dependencies m_dependencies;

    /**
     * Internal {@link com.tangosol.internal.tracing.Tracer} reference.
     */
    protected com.tangosol.internal.tracing.Tracer m_tracer;

    // ----- constants ------------------------------------------------------

    /**
     * The system property/environment variable name for Jaeger's sampler type configuration.
     */
    protected static final String JAEGER_SAMPLER_TYPE_PROPERTY = "JAEGER_SAMPLER_TYPE";

    /**
     * The system property/environment variable name for Jaeger's sampler value configuration (numeric).
     */
    protected static final String JAEGER_SAMPLER_VALUE_PROPERTY = "JAEGER_SAMPLER_PARAM";

    /**
     * The value for the {@link #JAEGER_SAMPLER_TYPE_PROPERTY} system property/environment variable name.
     */
    protected static final String JAEGER_DEFAULT_SAMPLER_TYPE = "probabilistic";

    /**
     * Valid lower-range value for probabilistic sampling.
     */
    protected static final float TRACING_LOWER_SAMPLE_RANGE = 0.0f;

    /**
     * Valid upper-range value for probabilistic sampling.
     */
    protected static final float TRACING_UPPER_SAMPLE_RANGE = 1.0f;

    /**
     * {@link OpenTracingSpan} wrapping {@link NoopSpan#INSTANCE}.
     */
    protected static final OpenTracingSpan NOOP_SPAN = new OpenTracingSpan(NoopSpan.INSTANCE);

    /**
     * {@link OpenTracingSpan} wrapping a no-op {@link Tracer.SpanBuilder}.
     */
    protected static final AbstractOpenTracingSpanBuilder NOOP_BUILDER =
            new AbstractOpenTracingSpanBuilder(NoopTracerFactory.create().buildSpan("no-op"))
                {
                @Override
                public Span startSpan()
                    {
                    return NOOP_SPAN;
                    }
                };
    }
