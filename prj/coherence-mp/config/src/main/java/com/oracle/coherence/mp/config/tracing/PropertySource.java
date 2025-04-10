/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.mp.config.tracing;

import java.util.Map;

import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

/**
 * A {@link PropertySource} implementation to expose
 * {@code Helidon MP Configuration} properties to code that
 * configures {@code OpenTelemetry} for Coherence.
 *
 * @author rl  4.7.2025
 * @since 25.03.1
 */
public class PropertySource
        implements com.tangosol.internal.tracing.PropertySource
    {
    // ----- PropertySource interface ---------------------------------------

    @Override
    public Map<String, String> getProperties()
        {
        Config config = ConfigProvider.getConfig();

        return StreamSupport.stream(config.getPropertyNames().spliterator(), false)
                .filter(key -> key.startsWith("otel."))
                .collect(Collectors.toMap(
                        key -> key,
                        key -> config.getValue(key, String.class)
                ));
        }
    }
