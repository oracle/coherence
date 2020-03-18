/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.tracing.opentracing33;

import com.tangosol.internal.tracing.TracingShim;
import com.tangosol.internal.tracing.TracingShimLoader;

import com.tangosol.net.CacheFactory;

import com.tangosol.util.Base;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A {@link TracingShimLoader} to load/enable the {@code OpenTracing}
 * runtime for use with Coherence.
 *
 * @since  14.1.1.0
 * @author rl 11.5.2019
 */
public class OpenTracingShimLoader
        implements TracingShimLoader
    {
    // ----- TracingShimLoader interface ------------------------------------

    @Override
    public TracingShim loadTracingShim()
        {
        return ensureDependenciesPresent() ? new OpenTracingShim() : TracingShim.Noop.INSTANCE;
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
        ClassLoader  loader       = Base.getContextClassLoader();
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
            if (CacheFactory.isLogEnabled(CacheFactory.LOG_MAX))
                {
                CacheFactory.log("Tracing support will not be enabled.  The following dependencies appear " +
                                 "to be missing: " + listMissingDeps.toString(), CacheFactory.LOG_MAX);
                }
            return false;
            }

        try
            {
            // all deps are available, make sure it's a version we can interoperate with
            // registerIfAbsent added in 0.32.0
            Class<?> clsGlobalTracer = Class.forName("io.opentracing.util.GlobalTracer");
            clsGlobalTracer.getMethod("registerIfAbsent", Class.forName("io.opentracing.Tracer", false, loader));
            }
        catch (NoSuchMethodException e)
            {
            if (CacheFactory.isLogEnabled(CacheFactory.LOG_MAX))
                {
                CacheFactory.log("Detected incompatible OpenTracing artifacts on the classpath.  Coherence supports"
                                 + " OpenTracing 0.32.0 or later.", CacheFactory.LOG_MAX);
                }
            return false;
            }
        catch (ClassNotFoundException ignored)
            {
            }
        return true;
        }

    /**
     * Verify the specified class is on the classpath.
     *
     * @param sClassName       the class name to verify
     * @param classLoader      the {@link ClassLoader} to use
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
    protected static final Map<String, String> EXPECTED_CLASSES = Collections.unmodifiableMap(
            new HashMap<String, String>()
                {
                    {
                    put("io.opentracing.Span",                                  "opentracing-api");
                    put("io.opentracing.noop.NoopSpan",                         "opentracing-noop");
                    put("io.opentracing.util.GlobalTracer",                     "opentracing-util");
                    put("io.opentracing.contrib.tracerresolver.TracerResolver", "opentracing-tracerresolver");
                    }
                });
    }
