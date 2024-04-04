/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.ai.config;

import com.oracle.coherence.common.base.Classes;

import com.tangosol.net.Coherence;
import com.tangosol.net.SessionConfiguration;
import com.tangosol.net.SessionProvider;

import com.tangosol.util.Resources;

import java.net.URL;

import java.util.Optional;

/**
 * The Coherence AI Module session configuration.
 */
public class VectorStoreSessionConfig
        implements SessionConfiguration
    {
    @Override
    public String getName()
        {
        return SCOPE_NAME;
        }

    @Override
    public String getScopeName()
        {
        return SCOPE_NAME;
        }

    @Override
    public Optional<String> getConfigUri()
        {
        //if a custom config is on the class path use that, otherwise use the default.
        URL url = Resources.findFileOrResource(CONFIG_URI, Classes.getContextClassLoader());
        return Optional.of(url != null ? CONFIG_URI : DEFAULT_CONFIG_URI);
        }

    // ----- inner class: ConcurrentServicesSessionProvider -----------------

    /**
     * The Coherence AI module session provider.
     */
    public static class VectorSessionProvider
            implements SessionProvider
        {
        // ----- SessionProvider interface ----------------------------------

        @Override
        public Context createSession(SessionConfiguration configuration, Context context)
            {
            if (SCOPE_NAME.equals(configuration.getName()))
                {
                Coherence.Mode mode = context.getMode();
                if (mode.isClusterMember())
                    {
                    return context.createSession(configuration);
                    }
                else
                    {
                    return context.createSession(new VectorStoreSessionConfig());
                    }
                }
            return context;
            }
        }

    // ----- constants ------------------------------------------------------

    public static final String SCOPE_NAME = "$CoherenceAI";

    public static final String CONFIG_URI = "coherence-ai-cache-config.xml";

    public static final String DEFAULT_CONFIG_URI = "/com/oracle/coherence/ai/" + CONFIG_URI;
    }
