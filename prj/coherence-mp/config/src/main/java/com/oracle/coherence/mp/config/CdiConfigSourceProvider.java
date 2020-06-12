/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.mp.config;

import com.oracle.coherence.common.base.Logger;

import java.util.Collections;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.CDI;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;

/**
 * Bridges the gap between {@link ServiceLoader} and CDI, by registering itself
 * as a service and using CDI to discover available CDI-managed {@link ConfigSource}s.
 *
 * @author Aleks Seovic  2020.06.11
 * @since 20.06
 */
public class CdiConfigSourceProvider
        implements ConfigSourceProvider
    {
    public Iterable<ConfigSource> getConfigSources(ClassLoader forClassLoader)
        {
        try
            {
            Instance<ConfigSource> sources = CDI.current().select(ConfigSource.class);
            if (sources.isUnsatisfied())
                {
                Logger.config("No CDI-managed configuration sources were discovered");
                }
            else
                {
                String sSourceNames = sources.stream()
                        .map(ConfigSource::getName)
                        .collect(Collectors.joining(", "));
                Logger.config("Registering CDI-managed configuration sources: " + sSourceNames);
                }

            return sources;
            }
        catch (IllegalStateException cdiNotAvailable)
            {
            Logger.info("CDI is not available. No CDI-managed configuration sources will be used.");
            return Collections.emptySet();
            }
        }
    }
