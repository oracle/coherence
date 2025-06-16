/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.bedrock.runtime.coherence.profiles;

import com.oracle.bedrock.Option;
import com.oracle.bedrock.OptionsByType;

import com.oracle.bedrock.runtime.Application;
import com.oracle.bedrock.runtime.MetaClass;
import com.oracle.bedrock.runtime.Platform;
import com.oracle.bedrock.runtime.Profile;

import com.oracle.bedrock.runtime.java.options.SystemProperties;
import com.oracle.bedrock.runtime.java.options.SystemProperty;


/**
 * A Bedrock {@link Profile} to set the Coherence lambda mode.
 */
public class LambdaModeProfile
        implements Profile, Option
    {
    /**
     * Create a {@link LambdaModeProfile}.
     *
     * @param sParam  the Coherence lambda mode to set
     */
    @OptionsByType.Default
    public LambdaModeProfile(String sParam)
        {
        if ("static".equals(sParam) || "dynamic".equals(sParam))
            {
            m_sValue = sParam;
            }
        else
            {
            throw new IllegalStateException("Invalid lambda mode parameter, must be \"static\" or \"dynamic\"");
            }
        }

    @Override
    public void onLaunching(Platform platform, MetaClass metaClass, OptionsByType optionsByType)
        {
        SystemProperties properties = optionsByType.get(SystemProperties.class);
        SystemProperty   property   = properties.get(PROP_LAMBDAS);
        if (property == null)
            {
            optionsByType.add(SystemProperty.of(PROP_LAMBDAS, m_sValue));
            }
        }

    @Override
    public void onLaunched(Platform platform, Application application, OptionsByType optionsByType)
        {
        }

    @Override
    public void onClosing(Platform platform, Application application, OptionsByType optionsByType)
        {
        }

    // ----- data members ---------------------------------------------------

    /**
     * The Coherence lambda mode property.
     */
    public static final String PROP_LAMBDAS = "coherence.lambdas";

    /**
     * The Coherence lambda mode value to set.
     */
    private final String m_sValue;
    }
