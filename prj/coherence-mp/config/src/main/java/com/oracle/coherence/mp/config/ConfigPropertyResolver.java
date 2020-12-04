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
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.eclipse.microprofile.config.spi.ConfigSource;

import java.util.Arrays;
import java.util.stream.StreamSupport;

/**
 * An implementation of Coherence property resolvers that reads system
 * properties and environment variables from MP Config.
 *
 * @author Aleks Seovic  2019.10.11
 * @since 20.06
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
     * <p>
     * This class is loaded by the {@link java.util.ServiceLoader} so
     * must have a default constructor.
     */
    public ConfigPropertyResolver()
        {
        ConfigProviderResolver resolver = ConfigProviderResolver.instance();
        ConfigSource[] aSource = StreamSupport.stream(resolver.getConfig().getConfigSources().spliterator(), false)
                .filter(p -> !CoherenceConfigSource.class.isAssignableFrom(p.getClass()))
                .toArray(ConfigSource[]::new);

        m_config = resolver.getBuilder()
                .withSources(aSource)
                .withSources(new CoherenceDefaultsConfigSource())
                .build();
        }

    // ---- EnvironmentVariableResolver interface ---------------------------

    @Override
    public String getEnv(String name)
        {
        return getProperty(name);
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
