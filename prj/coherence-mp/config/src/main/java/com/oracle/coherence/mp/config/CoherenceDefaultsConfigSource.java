/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.mp.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.microprofile.config.spi.ConfigSource;

/**
 * A static {@link ConfigSource} implementation that overrides default
 * Coherence configuration to:
 * <ul>
 *     <li>Use {@code java.util.logging} as a logging destination</li>
 *     <li>Use {@code com.oracle.coherence} as a logger name</li>
 *     <li>Changes default message format to {@code (thread={thread}, member={member},
 *         up={uptime}): {text}}, in order to allow {@code java.util.logging}
 *         to control overall message formatting
 *     </li>
 * </ul>
 *
 * This {@link ConfigSource} has an ordinal of 0, so the default configuration values
 * above will only be used if none of the higher priority configuration sources
 * provides an override for a configuration property.
 *
 * @author Aleks Seovic  2020.05.16
 * @since 14.1.2
 */
public class CoherenceDefaultsConfigSource
        implements ConfigSource
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Construct {@code CoherenceDefaultsConfigSource} instance.
     */
    public CoherenceDefaultsConfigSource()
        {
        Map<String, String> map = new HashMap<>();
        map.put(DESTINATION, "jdk");
        map.put(LOGGER_NAME, "com.oracle.coherence");
        map.put(MESSAGE_FORMAT, "(thread={thread}, member={member}, up={uptime}): {text}");

        f_mapProperties = Collections.unmodifiableMap(map);
        }


    // ---- ConfigSource interface ------------------------------------------

    @Override
    public Map<String, String> getProperties()
        {
        return f_mapProperties;
        }

    @Override
    public int getOrdinal()
        {
        return 0;
        }

    @Override
    public String getValue(String propertyName)
        {
        return f_mapProperties.get(propertyName);
        }

    @Override
    public String getName()
        {
        return "CoherenceDefaultsConfigSource";
        }

    // ---- constants -------------------------------------------------------

    /**
     * The name of the config property for logging destination.
     */
    private static final String DESTINATION = "coherence.log";

    /**
     * The name of the config property for logger name.
     */
    private static final String LOGGER_NAME = "coherence.log.logger";

    /**
     * The name of the config property for message format.
     */
    private static final String MESSAGE_FORMAT = "coherence.log.format";

    // ---- data members ----------------------------------------------------

    /**
     * A map holding default logging configuration.
     */
    private final Map<String, String> f_mapProperties;
    }
