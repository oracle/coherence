/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.testing.bedrock;


import com.oracle.bedrock.Option;
import com.oracle.bedrock.OptionsByType;
import com.oracle.bedrock.runtime.Application;
import com.oracle.bedrock.runtime.MetaClass;
import com.oracle.bedrock.runtime.Platform;
import com.oracle.bedrock.runtime.Profile;
import com.oracle.bedrock.runtime.java.options.SystemProperty;


/**
 * Enable configuring Coherence production mode for all Java applications launched by Bedrock.
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
        optionsByType.add(SystemProperty.of("coherence.mode", "prod"));
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
