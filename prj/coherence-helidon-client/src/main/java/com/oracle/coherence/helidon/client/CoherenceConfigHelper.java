/*
 * Copyright (c) 2020, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.helidon.client;

import com.tangosol.net.Coherence;

import io.helidon.config.Config;

import jakarta.enterprise.inject.spi.BeanManager;

import java.util.Collections;
import java.util.Map;

import java.util.stream.Collectors;

/**
 * A helper class for reading Coherence config from Helidon Microprofile {@link io.helidon.config.Config}.
 *
 * @author Jonathan Knight  2020.12.18
 * @since 20.12
 */
public class CoherenceConfigHelper
    {
    public CoherenceConfigHelper(BeanManager beanManager)
        {
        f_config = beanManager.createInstance()
                              .select(Config.class)
                              .stream()
                              .findFirst()
                              .orElseGet(Config::empty);
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Obtain the top level Coherence {@link Config}.
     *
     * @return the top level Coherence {@link Config}
     */
    public Config getCoherenceConfig()
        {
        return f_config.get(CONFIG_KEY_COHERENCE);
        }

    /**
     * Obtain the session configurations.
     *
     * @return the session configurations
     */
    public Map<String, Config> getSessions()
        {
        return getCoherenceConfig().get(CONFIG_KEY_SESSIONS)
                .asNodeList()
                .orElse(Collections.emptyList())
                .stream()
                .collect(Collectors.toMap(this::getKey, config -> config));
        }

    private String getKey(Config config)
        {
        String key = config.name();
        return config.get("name").asString().orElse(key);
        }

    // ----- constants ------------------------------------------------------

    public static final String CONFIG_KEY_COHERENCE = "coherence";

    public static final String CONFIG_KEY_SESSIONS = "sessions";

    // ----- data members ---------------------------------------------------

    private final Config f_config;
    }
