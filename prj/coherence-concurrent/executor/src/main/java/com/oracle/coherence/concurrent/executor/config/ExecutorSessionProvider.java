/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.concurrent.executor.config;

import com.oracle.coherence.concurrent.executor.Executors;

import com.tangosol.net.Coherence;
import com.tangosol.net.SessionConfiguration;
import com.tangosol.net.SessionProvider;

/**
 * {@link SessionProvider} for the Coherence Executor service.
 *
 * @author rl  8.10.2021
 * @since 21.12
 */
public class ExecutorSessionProvider
        implements SessionProvider
    {
    // ----- SessionProvider interface --------------------------------------

    @Override
    public Context createSession(SessionConfiguration configuration, Context context)
        {
        if (Executors.SESSION_NAME.equals(configuration.getName()))
            {
            if (context.getMode() == Coherence.Mode.ClusterMember)
                {
                // we only add this Executors session on a cluster member
                return context.createSession(new ExecutorSessionConfiguration());
                }
            else
                {
                // there is no Executors session on an Extend client.
                return context.complete();
                }
            }
        // the request was not for the Executors session
        return context;
        }
    }
