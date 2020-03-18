/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package tracing;

import com.tangosol.net.CacheFactory;
import io.jaegertracing.internal.reporters.InMemoryReporter;

/**
 * Jaeger testing utils.
 *
 * @author rl 2019
 * @since 14.1.1.0
 */
public final class TestingUtils
    {
    // ----- public methods -------------------------------------------------

    /**
     * Validate the current {@link InMemoryReporter} using the provided {@link ReporterValidator validator}.
     *
     * @param validator  the {@link ReporterValidator} to use to validate the current {@link InMemoryReporter}
     */
    public static void validateReporter(ReporterValidator validator)
        {
        InMemoryReporter reporter = AdaptiveTracerFactory.getReporter();
        if (reporter == null)
            {
            return;
            }

        try
            {
            CacheFactory.log("Captured spans: " + AdaptiveTracerFactory.getReporter().getSpans());
            validator.validate(reporter);
            }
        finally
            {
            AdaptiveTracerFactory.resetReporter();
            }
        }

    // ----- inner class: InMemoryReporter ----------------------------------

    /**
     * Interface for {@link InMemoryReporter} validation.
     */
    @FunctionalInterface
    protected interface ReporterValidator
        {
        void validate(InMemoryReporter reporter);
        }
    }
