/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.executor.config;

import com.oracle.coherence.concurrent.executor.Executors;

import com.tangosol.coherence.config.Config;
import com.tangosol.net.Coherence;
import com.tangosol.net.SessionConfiguration;

import java.util.Optional;

/**
 * {@link SessionConfiguration} for the Coherence Executor service.
 *
 * @author rl  8.10.2021
 * @since 21.12
 */
public class ExecutorSessionConfiguration
        implements SessionConfiguration
    {
    // ----- SessionConfiguration interface ---------------------------------

    @Override
    public String getName()
        {
        return Executors.SESSION_NAME;
        }

    @Override
    public String getScopeName()
        {
        return Coherence.SYSTEM_SCOPE;
        }

    @Override
    public Optional<String> getConfigUri()
        {
        return Optional.of(Config.getProperty(Executors.EXECUTOR_CONFIG_OVERRIDE, Executors.EXECUTOR_CONFIG_URI));
        }
    }
