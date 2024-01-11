/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
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
import com.tangosol.coherence.config.Config;

public class LoomProfile
        implements Profile, Option
    {
    /**
     * Create a {@link LoomProfile}.
     */
    @OptionsByType.Default
    public LoomProfile()
        {
        this(Config.getBoolean(PROPERTY_ENABLED));
        }

    public LoomProfile(boolean fEnabled)
        {
        m_fEnabled = fEnabled;
        }

    @Override
    public void onLaunching(Platform platform, MetaClass metaClass, OptionsByType optionsByType)
        {
        SystemProperties properties = optionsByType.get(SystemProperties.class);
        SystemProperty   property   = properties.get(PROPERTY_ENABLED);
        if (property == null)
            {
            optionsByType.add(SystemProperty.of(PROPERTY_ENABLED, m_fEnabled));
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
     * The property to enable virtual threads.
     */
    static final String PROPERTY_ENABLED = "coherence.virtualthreads.enabled";

    private final boolean m_fEnabled;
    }
