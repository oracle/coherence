/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.testing.bedrock;


import com.oracle.bedrock.Option;
import com.oracle.bedrock.OptionsByType;
import com.oracle.bedrock.runtime.Application;
import com.oracle.bedrock.runtime.MetaClass;
import com.oracle.bedrock.runtime.Platform;
import com.oracle.bedrock.runtime.Profile;
import com.oracle.bedrock.runtime.java.options.SystemProperties;
import com.oracle.bedrock.runtime.java.options.SystemProperty;


/**
 * Default Java applications launched by Bedrock to Coherence production mode,
 * preserving an explicitly configured member mode.
 *
 * @author jf  2021.03.26
 */
public class ProductionmodeProfile
        implements Profile, Option
    {
    // ----- constructors ------------------------------------------------------

    /**
     * Constructs a {@link ProductionmodeProfile}.
     */
    @OptionsByType.Default
    public ProductionmodeProfile(String sParameters)
        {
        }

    // ----- Profile methods ---------------------------------------------------

    @Override
    public void onLaunching(
            Platform platform,
            MetaClass metaClass,
            OptionsByType optionsByType)
        {
        SystemProperties properties = optionsByType.get(SystemProperties.class);
        if (properties == null || properties.get("coherence.mode") == null)
            {
            optionsByType.add(SystemProperty.of("coherence.mode", "prod"));
            }
        }

    @Override
    public void onLaunched(
            Platform platform,
            Application application,
            OptionsByType optionsByType)
        {
        // there's nothing to do after an application has been realized
        }

    @Override
    public void onClosing(
            Platform platform,
            Application application,
            OptionsByType optionsByType)
        {
        // there's nothing to do after an application has been closed
        }
    }
