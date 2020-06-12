/*
 * Copyright (c) 2019, 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.mp.config;

import com.tangosol.net.DefaultCacheServer;

import org.eclipse.microprofile.config.Config;

import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.eclipse.microprofile.config.spi.ConfigSource;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

/**
 * Unit tests for {@link CoherenceConfigSource}.
 *
 * @author Aleks Seovic  2019.10.12
 */
class CoherenceConfigSourceTest
    {
    @BeforeAll
    static void setup()
        {
        DefaultCacheServer.startServerDaemon().waitForServiceStart();

        System.setProperty("config.value", "sysprop");
        }

    @AfterAll
    static void stop()
        {
        DefaultCacheServer.shutdown();
        }

    private static Config getConfig()
        {
        ConfigProviderResolver resolver = ConfigProviderResolver.instance();
        ConfigBuilder builder = resolver.getBuilder();
        return builder
                .addDefaultSources()
                .addDiscoveredSources()
                .build();
        }

    private static CoherenceConfigSource getConfigSource(Config config)
        {
        for (ConfigSource source : config.getConfigSources())
            {
            if (source instanceof CoherenceConfigSource)
                {
                CoherenceConfigSource ccs = (CoherenceConfigSource) source;
                ccs.activate();
                return ccs;
                }
            }
        throw new IllegalStateException("CoherenceConfigSource is not in a list of sources");
        }

    @BeforeEach
    void clearSystemProperties()
        {
        System.clearProperty("coherence.mp.configsource.enabled");
        System.clearProperty("coherence.mp.configsource.ordinal");
        }

    @Test
    void testDefaults()
        {
        Config config = getConfig();
        CoherenceConfigSource source = getConfigSource(config);
        source.getConfigCache().put("config.value", "cache");

        assertThat(source.getProperties().entrySet(), hasSize(1));
        assertThat(source.getProperties(), hasKey("config.value"));
        assertThat(source.getPropertyNames(), hasSize(1));
        assertThat(source.getPropertyNames(), hasItem("config.value"));
        assertThat(source.getValue("config.value"), is("cache"));
        assertThat(source.getOrdinal(), is(500));
        assertThat(source.getName(), is("coherence"));
        }

    @Test
    void testDiscovery()
        {
        Config config = getConfig();
        getConfigSource(config).getConfigCache().put("config.value", "cache");

        assertThat(config.getValue("config.value", String.class), is("cache"));
        }

    @Test
    void testPriority()
        {
        System.setProperty("coherence.mp.configsource.ordinal", "100");

        Config config = getConfig();
        assertThat(config.getValue("config.value", String.class), is("sysprop"));
        }
    }
