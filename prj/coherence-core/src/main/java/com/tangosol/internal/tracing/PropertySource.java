/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.tracing;

import java.util.Map;

/**
 * Custom source of tracing configuration properties.
 * In a stand-alone Coherence configuration, this really has no use
 * as system properties are sufficient.  However, in environments
 * using the Coherence Helidon, Micronaut, or Spring integrations,
 * this may be useful to expose the application-specific configuration
 * properties to the {@link TracingShim}s
 *
 * @author rl  4.4.2025
 * @since 25.03.1
 */
public interface PropertySource
    {
    /**
     * Return a {@link Map} of properties for configuring a Tracer
     * implementation.
     *
     * @return a {@link Map} of properties for configuring a Tracer
     * implementation.
     */
    Map<String, String> getProperties();

    /**
     * The name of the {@link PropertySource} <em>provider-configuration
     * file</em> in the resource directory <em>META-INF/services</em>.
     */
    String SERVICE_NAME = "com.tangosol.internal.tracing.PropertySource";
    }
