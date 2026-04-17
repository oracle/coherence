/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
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
 * A Bedrock {@link Profile} to set the Coherence reliable transport property.
 */
public class TransportProfile
        implements Profile, Option
    {
    /**
     * Create a {@link TransportProfile}.
     *
     * @param sParam  the Coherence reliable transport value to set
     */
    @OptionsByType.Default
    public TransportProfile(String sParam)
        {
        if ("datagram".equals(sParam) || "tmb".equals(sParam) || "tmbs".equals(sParam))
            {
            m_sValue = sParam;
            }
        else
            {
            throw new IllegalStateException("Invalid transport parameter, must be \"datagram\" or \"tmb\"");
            }
        }

    @Override
    public void onLaunching(Platform platform, MetaClass metaClass, OptionsByType optionsByType)
        {
        SystemProperties properties = optionsByType.get(SystemProperties.class);
        SystemProperty   property   = properties.get(PROP_TRANSPORT);
        if (property == null)
            {
            optionsByType.add(SystemProperty.of(PROP_TRANSPORT, m_sValue));
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
     * The Coherence reliable transport property.
     */
    public static final String PROP_TRANSPORT = "coherence.transport.reliable";

    /**
     * The Coherence reliable transport value to set.
     */
    private final String m_sValue;
    }
