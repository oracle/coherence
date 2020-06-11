/*
 * Copyright (c) 2019, 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.mp.config;

import com.tangosol.coherence.config.EnvironmentVariableResolver;
import com.tangosol.coherence.config.SystemPropertyResolver;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;

/**
 * An implementation of Coherence property resolvers that reads system
 * properties and environment variables from MP Config.
 *
 * @author Aleks Seovic  2019.10.11
 * @since Coherence 14.1.2
 *
 * @see com.tangosol.coherence.config.SystemPropertyResolver
 * @see com.tangosol.coherence.config.EnvironmentVariableResolver
 */
public class ConfigPropertyResolver
        implements SystemPropertyResolver, EnvironmentVariableResolver
    {
    // ---- constructor -----------------------------------------------------

    /**
     * Construct {@code ConfigPropertyResolver} instance.
     */
    public ConfigPropertyResolver()
        {
        ConfigProviderResolver resolver = ConfigProviderResolver.instance();
        ConfigBuilder builder = resolver.getBuilder();
        m_config = builder
                .addDefaultSources()
                .withSources(new CoherenceDefaultsConfigSource())
                .build();
        }

    // ---- EnvironmentVariableResolver interface ---------------------------

    @Override
    public String getEnv(String name)
        {
        return m_config.getOptionalValue(name, String.class).orElse(null);
        }

    // ---- SystemPropertyResolver interface --------------------------------

    @Override
    public String getProperty(String name)
        {
        return m_config.getOptionalValue(name, String.class).orElse(null);
        }

    // ---- data members ----------------------------------------------------

    /**
     * Configuration instance.
      */
    private final Config m_config;
    }
