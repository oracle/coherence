/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package opentracing;

import com.tangosol.net.CacheFactory;

import io.jaegertracing.Configuration;

import io.jaegertracing.internal.JaegerSpan;
import io.jaegertracing.internal.JaegerTracer;

import io.jaegertracing.internal.reporters.InMemoryReporter;

import io.opentracing.Tracer;

import io.opentracing.contrib.tracerresolver.TracerFactory;

import java.util.ArrayList;
import java.util.List;

import java.util.concurrent.atomic.AtomicReference;

/**
 * {@link TracerFactory} to produce an in-memory {@link Tracer} for testing purposes.  However,
 * if {@link JaegerConfigProperties#ENDPOINT} is defined, then the default
 * behavior, as described <a href="https://github.com/jaegertracing/jaeger-client-java/tree/master/jaeger-tracerresolver">...here</a>,
 * will be executed to create a new {@link Tracer} instance.  This allows running a test against a live Jaeger
 * instance.
 *
 * @author rl 2019
 * @since 14.1.1.0
 */
public class AdaptiveTracerFactory
        implements TracerFactory
    {
    // ----- TracerFactory interface ----------------------------------------

    @Override
    public synchronized Tracer getTracer()
        {
        JaegerTracer.Builder builder = Configuration.fromEnv().getTracerBuilder();

        String endpoint = JaegerConfigProperties.ENDPOINT.toString();

        if (System.getenv(endpoint) == null && System.getProperty(endpoint) == null)
            {
            InMemoryReporter reporter = REPORTER.get();
            if (reporter == null)
                {
                reporter = new SafeInMemoryReporter();
                REPORTER.compareAndSet(null, reporter);
                }
            builder.withReporter(reporter);
            }

        builder.withTag("test.name", System.getProperty("test.name"));

        Tracer tracer = builder.build();
        CacheFactory.log("Initialized " + tracer.toString());
        return tracer;
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Return the {@link InMemoryReporter}, if any.
     *
     * @return the {@link InMemoryReporter}, if any
     */
    public static InMemoryReporter getReporter()
        {
        return REPORTER.get();
        }

    /**
     * Clear the cached {@link InMemoryReporter reporter}.
     */
    public static void resetReporter()
        {
        REPORTER.set(null);
        }

    // ----- inner class: SafeInMemoryReporter ------------------------------

    /**
     * Extension of {@link InMemoryReporter} that allows concurrent iteration/modification
     * over the capture spans.
     */
    protected static class SafeInMemoryReporter
            extends InMemoryReporter
        {
        @Override
        public synchronized List<JaegerSpan> getSpans()
            {
            List<JaegerSpan> spans = super.getSpans();
            int              size  = spans.size();
            List<JaegerSpan> copy  = new ArrayList<>(size);
            for (int i = 0, len = spans.size(); i < len; i++)
                {
                copy.add(spans.get(i));
                }
            return copy;
            }
        }

    // ----- data members ---------------------------------------------------

    /**
     * Reference to store the current {@link InMemoryReporter}.
     */
    private static final AtomicReference<InMemoryReporter> REPORTER = new AtomicReference<>();
    }
