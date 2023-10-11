/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package tracing;

import com.tangosol.internal.tracing.Tracer;
import com.tangosol.internal.tracing.TracingHelper;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test to ensure Coherence behaves as expected when OpenTracing isn't on the classpath.
 *
 * @author rl 2019
 * @since 14.1.1.0
 */
public class CleanTracingTest
        extends AbstractTracingIT
    {
    // ----- test methods ---------------------------------------------------

    /**
     * Even if the override attempts to enable tracing, if there is no usable {@code TracerFactory}, tracing will
     * remain disabled.
     */
    @Test
    public void ensureDisabledIfNoAppropriateTracer()
        {
        runTest(() -> assertFalse("Expected tracing to NOT be enabled.", TracingHelper.isEnabled()));
        }

    /**
     * Calling {@link TracingHelper#getTracer()} when tracing is disabled results in a no-op tracer being returned.
     */
    @Test
    public void testGetTracer()
        {
        runTest(() ->
                {
                Tracer tracer = TracingHelper.getTracer();
                assertTrue("Expected isNoop() to return true when invoked on tracer." + tracer.getClass().getName(),
                           TracingHelper.isNoop(tracer));
                });
        }
    }
