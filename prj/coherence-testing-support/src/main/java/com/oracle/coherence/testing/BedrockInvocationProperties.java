/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.testing;

import com.oracle.bedrock.OptionsByType;
import com.oracle.bedrock.runtime.java.options.SystemProperties;
import com.oracle.bedrock.runtime.java.options.SystemProperty;

/**
 * Test helper for propagating invocation-level Coherence properties to Bedrock members.
 *
 * @author as  2026.05.02
 * @since 26.04
 */
public final class BedrockInvocationProperties
    {
    /**
     * Inherit command-line Coherence properties unless the member options already define them.
     *
     * @param options  the member options to update
     *
     * @return the updated options
     */
    public static OptionsByType inherit(OptionsByType options)
        {
        inherit(options, "coherence.mode");
        inherit(options, "coherence.cluster");
        inherit(options, "coherence.wka");
        inherit(options, "coherence.localhost");
        inherit(options, "coherence.internal.invoke.trace.shim");
        return options;
        }

    /**
     * Inherit a command-line property unless the member options already define it.
     *
     * @param options  the member options to update
     * @param sName    the system property name
     */
    private static void inherit(OptionsByType options, String sName)
        {
        String sValue = System.getProperty(sName);
        if (sValue == null)
            {
            return;
            }

        SystemProperties properties = options.get(SystemProperties.class);
        if (properties == null || properties.get(sName) == null)
            {
            options.add(SystemProperty.of(sName, sValue));
            }
        }

    /**
     * No instances.
     */
    private BedrockInvocationProperties()
        {
        }
    }
