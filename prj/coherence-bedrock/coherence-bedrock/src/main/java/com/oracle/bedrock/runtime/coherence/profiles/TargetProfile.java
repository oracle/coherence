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

public class TargetProfile
        implements Profile, Option
    {
    /**
     * Create a {@link TargetProfile}.
     */
    @OptionsByType.Default
    public TargetProfile(String ignored)
        {
        }

    @Override
    public void onLaunching(Platform platform, MetaClass metaClass, OptionsByType optionsByType)
        {
        SystemProperties properties = optionsByType.get(SystemProperties.class);
        SystemProperty   property   = properties.get(PROP);
        String           sValue     = System.getProperty(PROP);

        if (property == null && sValue != null && !sValue.isEmpty())
            {
            optionsByType.add(SystemProperty.of(PROP, sValue));
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

    public static final String PROP = "project.build.directory";
    }
