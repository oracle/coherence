/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.tracing.opentracing;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.internal.tracing.TracingShim;

import io.opentracing.Tracer;

import io.opentracing.noop.NoopTracerFactory;

import io.opentracing.util.GlobalTracer;

import java.lang.reflect.Field;

/**
 * {@link TracingShim} implementation for {@code OpenTracing 0.33.0}.
 *
 * @since  14.1.1.0
 * @author rl 11.5.2019
 */
@Deprecated
public class OpenTracingShim
        extends AbstractOpenTracingShim
    {
    // ----- AbstractOpenTracingShimMethods ---------------------------------

    @Override
    protected boolean registerTracer(Tracer tracer)
        {
        if (GLOBAL_TRACER_REGISTERED_FIELD == null) // loader should shield ensure not null
            {
            return false;
            }

        return GlobalTracer.registerIfAbsent(tracer);
        }

    @Override
    protected void onControlClose()
        {
        try
            {
            // set field to false to allow disabling tracing
            GLOBAL_TRACER_REGISTERED_FIELD.set(null, false);

            // Register a no-op tracer and set the GLOBAL_TRACER_REGISTERED_FIELD to false again;
            // this ensure GlobalTracer.isRegistered will once again return false, and allow new
            // Tracers to be registered, even those which aren't created by the Tracing helper.
            // This is based on internal awareness of how GlobalTracer register/isRegistered works.
            GlobalTracer.registerIfAbsent(NoopTracerFactory.create());
            GLOBAL_TRACER_REGISTERED_FIELD.set(null, false);
            }
        catch (IllegalStateException | IllegalAccessException e)
            {
            Logger.finest("Unexpected exception resetting GlobalTracer: ", e);
            }
        }

    @Override
    protected String getApiVersion()
        {
        return "0.33.0";
        }

    @Override
    protected com.tangosol.internal.tracing.Tracer createTracer()
        {
        return new OpenTracingTracer();
        }

    // ----- constants ------------------------------------------------------

    /**
     * A private static field within {@link GlobalTracer} which must be manipulated
     * via reflection to properly support enabling/disabling tracing dynamically.
     */
    protected static final Field GLOBAL_TRACER_REGISTERED_FIELD;

    // ----- static initializer -----------------------------------------

    static
        {
        Field fieldRegistered = null;
        try
            {
            fieldRegistered = GlobalTracer.class.getDeclaredField("isRegistered");
            fieldRegistered.setAccessible(true);
            }
        catch (NoSuchFieldException e)
            {
            // this shouldn't happen as the loader would qualify the environment before this code is executed
            Logger.err("An incompatible version of the OpenTracing API has been detected "
                       + "on the classpath. Tracing will be disabled.");
            }
        GLOBAL_TRACER_REGISTERED_FIELD = fieldRegistered;
        }
    }
