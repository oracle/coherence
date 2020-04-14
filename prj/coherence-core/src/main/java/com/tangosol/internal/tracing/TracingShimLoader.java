/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.tracing;

import java.util.ServiceLoader;

/**
 * Implementations of this class are responsible for loading {@link TracingShim} implementations from the classpath per
 * the {@link ServiceLoader} contract.
 * <p>
 * Service definitions must be placed in <em>META-INF/services</em> using the <em>provider-configuration file</em>
 * named <em>{@value #SERVICE_NAME}</em>.
 *
 * @see ServiceLoader
 *
 * @since  14.1.1.0
 * @author rl 11.5.2019
 */
public interface TracingShimLoader
    {
    /**
     * Load the implementation-specific {@link TracingShim} from the classpath via the {@link ServiceLoader}
     * contract.  Service definitions must be placed in <em>META-INF/services</em> using
     * the <em>provider-configuration file</em> named <em>{@value #SERVICE_NAME}</em>.
     * <p>
     * No explicit ordering guarantees are made, thus if there are multiple {@link TracingShim shims} on the
     * classpath, the first {@link TracingShim shim} loaded will be what is used for lifetime of the JVM.
     * <p>
     * If a {@link TracingShimLoader loader} determines it is unable to successfully provide a working shim, say
     * classes are missing, then the loader must return {@link TracingShim.Noop#INSTANCE}.
     *
     * @return the {@link TracingShim} per the {@link ServiceLoader} contract or {@link TracingShim.Noop#INSTANCE}
     *         if it's not possible to load the {@link TracingShim shim}
     */
    public TracingShim loadTracingShim();

    /**
     * The name of the {@link TracingShim} <em>provider-configuration file</em> in the resource directory
     * <em>META-INF/services</em>.
     */
    public String SERVICE_NAME = "com.tangosol.internal.tracing.TracingShimLoader";
    }
