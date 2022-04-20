/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.bedrock.runtime.coherence;

import com.oracle.bedrock.Option;
import com.oracle.bedrock.OptionsByType;
import com.oracle.bedrock.runtime.Application;
import com.oracle.bedrock.runtime.MetaClass;
import com.oracle.bedrock.runtime.Platform;
import com.oracle.bedrock.runtime.Profile;
import com.oracle.bedrock.runtime.java.options.SystemProperties;
import com.oracle.bedrock.runtime.java.options.SystemProperty;

public enum JMXManagementMode
        implements Option, Profile
    {
        ALL,
        NONE,
        REMOTE_ONLY,
        LOCAL_ONLY;

    /**
     * The tangosol.coherence.management property.
     */
    public static final String PROPERTY = "coherence.management";


    /**
     * Determines the system property representation of the {@link JMXManagementMode}
     *
     * @return A {@link String}
     */
    public String toSystemProperty()
        {
        // default to all
        String result = "all";

        if (this == NONE)
            {
            result = "none";
            }
        else if (this == REMOTE_ONLY)
            {
            result = "remote-only";
            }
        else if (this == LOCAL_ONLY)
            {
            result = "local-only";
            }

        return result;
        }


    /**
     * Obtains a {@link JMXManagementMode} based on system-property value.
     *
     * @param systemProperty the system-property value
     * @return a {@link JMXManagementMode} or null if unknown
     */
    public static JMXManagementMode fromSystemProperty(String systemProperty)
        {
        systemProperty = systemProperty == null ? "" : systemProperty.trim().toLowerCase();

        if (systemProperty == null)
            {
            return null;
            }
        else if (systemProperty.equals("none"))
            {
            return NONE;
            }
        else if (systemProperty.equals("remote-only"))
            {
            return REMOTE_ONLY;
            }
        else if (systemProperty.equals("local-only"))
            {
            return LOCAL_ONLY;
            }
        else if (systemProperty.equals("all"))
            {
            return ALL;
            }
        else
            {
            return null;
            }
        }


    @Override
    public void onLaunching(
            Platform platform,
            MetaClass metaClass,
            OptionsByType optionsByType)
        {
        SystemProperties systemProperties = optionsByType.get(SystemProperties.class);

        if (systemProperties != null)
            {
            optionsByType.add(SystemProperty.of(PROPERTY, toSystemProperty()));
            }
        }


    @Override
    public void onLaunched(
            Platform platform,
            Application application,
            OptionsByType optionsByType)
        {
        }


    @Override
    public void onClosing(
            Platform platform,
            Application application,
            OptionsByType optionsByType)
        {
        }
    }
