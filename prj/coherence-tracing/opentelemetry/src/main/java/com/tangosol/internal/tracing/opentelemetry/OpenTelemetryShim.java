/*
 * Copyright (c) 2023, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.tracing.opentelemetry;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.internal.tracing.PropertySource;
import com.tangosol.internal.tracing.Span;
import com.tangosol.internal.tracing.Tracer;
import com.tangosol.internal.tracing.TracingShim;

import io.opentelemetry.api.trace.TracerProvider;

import io.opentelemetry.sdk.OpenTelemetrySdk;

import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;

import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;

import java.util.stream.Collectors;

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

        if (checkTracingEnabled(dependencies.getSamplingRatio()))
            {
            Map<String, String> props = configureTracing(dependencies);

            // initialize the tracing SDK
            OpenTelemetrySdk sdk = AutoConfiguredOpenTelemetrySdk.builder()
                    .addPropertiesSupplier(() -> props)
                    .build()
                    .getOpenTelemetrySdk();

            m_tracer = new OpenTelemetryTracer(sdk);
            }
        else
            {
            // tracer will be present, but will be noop
            m_tracer = new OpenTelemetryTracer(null);
            }

        Logger.finest(() -> "Initialized TracingShim: " + this);

        return new TracingShim.Control()
            {
            public synchronized void close()
                {
                if (!m_fClosed)
                    {
                    m_fClosed = true;
                    m_tracer.dispose();
                    m_tracer = null;
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

    @Override
    public boolean isEnabled()
        {
        Tracer tracer = getTracer();
        return tracer != null && !tracer.isNoop();
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

        if (!(o instanceof OpenTelemetryShim that))
            {
            return false;
            }

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
     * Return the {@link PropertySource} used to initialize
     * {@code OpenTelemetry}.
     *
     * @return the {@link PropertySource} used to initialize
     *         {@code OpenTelemetry}
     *
     * @since 25.03.1
     */
    private static PropertySource getPropertySource()
        {
        try
            {
            PropertySource propertySource = ServiceLoader.load(PropertySource.class).iterator().next();

            Logger.finest(() -> "Loaded OpenTelemetry PropertySource: " + propertySource.getClass().getName());

            return propertySource;
            }
        catch (Exception e)
            {
            Logger.finest("No PropertySource found, falling back to the default PropertySource");
            return new DefaultPropertySource();
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
     * Obtain the configuration properties that will be passed to the
     * {@code OpenTelemetry} runtime.
     *
     * @param dependencies tracing dependencies
     *
     * @since 25.03.1
     */
    private static Map<String, String> configureTracing(Dependencies dependencies)
        {
        Map<String, String> props = getPropertySource().getProperties();

        // by default, unless already set, we set these properties to none;
        // the user will be required to configure explicitly

        if (!props.containsKey(OTEL_LOGS_EXPORTER_PROPERTY))
            {
            props.put(OTEL_LOGS_EXPORTER_PROPERTY, "none");
            }

        if (!props.containsKey(OTEL_METRICS_EXPORTER_PROPERTY))
            {
            props.put(OTEL_METRICS_EXPORTER_PROPERTY, "none");
            }

        if (!props.containsKey(OTEL_TRACES_EXPORTER_PROPERTY))
            {
            Logger.warn(("OpenTelemetry enabled, but %s property was not defined."
                         + "  No traces will be captured.").formatted(OTEL_TRACES_EXPORTER_PROPERTY));

            props.put(OTEL_TRACES_EXPORTER_PROPERTY, "none");
            }

        if (!props.containsKey(OTEL_SERVICE_NAME_PROPERTY))
            {
            String sIdentity = dependencies.getIdentity();

            if (sIdentity != null)
                {
                props.put(OTEL_SERVICE_NAME_PROPERTY, sIdentity);
                }
            else
                {
                Logger.warn(("OpenTelemetry enabled, but no %s property was defined and no identity was configured.  "
                             + "Defaulting the service name to 'Coherence'").formatted(OTEL_SERVICE_NAME_PROPERTY));

                props.put(OTEL_SERVICE_NAME_PROPERTY, "Coherence");
                }
            }

        // if the user has already provided these, honor their configuration,
        // otherwise, expose our configuration
        if (!props.containsKey(OTEL_SAMPLER_TYPE_PROPERTY))
            {
            float flSamplingRatio = dependencies.getSamplingRatio();

            props.put(OTEL_SAMPLER_TYPE_PROPERTY, OTEL_SAMPLER_PROPERTY_VALUE);
            props.put(OTEL_TRACES_SAMPLER_ARG_PROPERTY,
                      Float.compare(0.0f, flSamplingRatio) == 0
                          ? "1.0"
                          : String.valueOf(flSamplingRatio));
            }

        return props;
        }

    // ----- inner class: DefaultPropertySource -----------------------------

    /**
     * The default {@link PropertySource} if none are found by the
     * {@link ServiceLoader}.  This implementation uses system properties
     * for underlying source.
     */
    protected static class DefaultPropertySource
            implements PropertySource
        {
        // ----- PropertySource interface -----------------------------------

        @Override
        public Map<String, String> getProperties()
            {
            return System.getProperties().entrySet().stream()
                    .filter(e -> e.getKey().toString().startsWith("otel."))
                    .collect(Collectors.toMap(
                            e -> e.getKey().toString(),
                            e -> e.getValue().toString()));
            }
        }

    // ----- constants ------------------------------------------------------

    /**
     * A static, no-op, {@link OpenTelemetrySpan}.
     */
    protected static final OpenTelemetrySpan NOOP_SPAN =
            new OpenTelemetrySpan(io.opentelemetry.api.trace.Span.getInvalid());

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
    protected static final String OTEL_SAMPLER_PROPERTY_VALUE = "parentbased_traceidratio";

    /**
     * System property recognized by {@code OpenTelemetry} to pass arguments to
     * configure the defined sampler.
     */
    protected static final String OTEL_TRACES_SAMPLER_ARG_PROPERTY = "otel.traces.sampler.arg";

    /**
     * System property that configures the traces exporter for {@code OpenTelemetry}.
     *
     * @since 25.03.1
     */
    protected static final String OTEL_TRACES_EXPORTER_PROPERTY = "otel.traces.exporter";

    /**
     * System property that configures the logs exporter for {@code OpenTelemetry}.
     *
     * @since 25.03.1
     */
    protected static final String OTEL_LOGS_EXPORTER_PROPERTY = "otel.logs.exporter";

    /**
     * System property that configures the traces exporter for {@code OpenTelemetry}.
     *
     * @since 25.03.1
     */
    protected static final String OTEL_METRICS_EXPORTER_PROPERTY = "otel.metrics.exporter";

    /**
     * System property that configures the {@code OpenTelemetry} service name.
     *
     * @since 25.03.1
     */
    protected static final String OTEL_SERVICE_NAME_PROPERTY = "otel.service.name";

    /**
     * A no-op Tracer which will be used when tracing is on the classpath,
     * but Coherence tracing itself is disabled.
     *
     * @since 25.03.1
     */
    protected static final io.opentelemetry.api.trace.Tracer NOOP_TRACER =
            TracerProvider.noop().get(OpenTelemetryTracer.SCOPE_NAME);

    /**
     * A static, no-op, {@link OpenTelemetryTracer.SpanBuilder}.
     */
    protected static final OpenTelemetryTracer.SpanBuilder NOOP_BUILDER =
            new OpenTelemetryTracer.SpanBuilder(NOOP_TRACER.spanBuilder("noop"))
                {
                public Span startSpan()
            {
            return NOOP_SPAN;
            }
                };

    // ----- data members ---------------------------------------------------

    /**
     * The tracing configuration {@link TracingShim.Dependencies dependences}.
     */
    protected TracingShim.Dependencies m_dependencies;

    /**
     * The {@link Tracer} that Coherence will be using.
     */
    protected OpenTelemetryTracer m_tracer;
    }
