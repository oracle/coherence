/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net;

import java.util.Locale;

/**
 * Service worker-pool implementation type.
 *
 * @author Aleks Seovic  2026.04.26
 * @since 26.04
 */
public enum DaemonPoolType
    {
    /**
     * The legacy platform-thread daemon pool.
     */
    PLATFORM,

    /**
     * The virtual-thread daemon pool.
     */
    VIRTUAL;

    /**
     * Parse a daemon-pool type from configuration text.
     *
     * @param sValue  the configured value
     *
     * @return the parsed daemon-pool type
     *
     * @throws IllegalArgumentException if the value is unknown
     */
    public static DaemonPoolType fromString(String sValue)
        {
        String sName = sValue == null ? "" : sValue.trim();
        for (DaemonPoolType type : values())
            {
            if (type.name().equalsIgnoreCase(sName))
                {
                return type;
                }
            }

        throw new IllegalArgumentException("Unknown <daemon-pool> value \"" + sName
                + "\". Valid values are \"platform\" and \"virtual\".");
        }

    /**
     * Return the lowercase configuration value.
     *
     * @return the lowercase configuration value
     */
    public String getConfigValue()
        {
        return name().toLowerCase(Locale.ROOT);
        }
    }
