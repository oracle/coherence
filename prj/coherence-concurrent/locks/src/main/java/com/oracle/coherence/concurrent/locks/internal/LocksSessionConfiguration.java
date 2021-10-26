/*
 * Copyright (c) 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.locks.internal;

import com.oracle.coherence.concurrent.locks.Locks;

import com.tangosol.net.Coherence;
import com.tangosol.net.SessionConfiguration;
import com.tangosol.net.SessionProvider;

import java.util.Optional;

/**
 * A {@link SessionConfiguration} for the server side Locks session.
 *
 * @author Aleks Seovic  2021.10.20
 * @since 21.12
 */
public class LocksSessionConfiguration
        implements SessionConfiguration
    {
    // ----- AtomicsSessionConfiguration methods ----------------------------

    @Override
    public String getName()
        {
        return Locks.SESSION_NAME;
        }

    @Override
    public String getScopeName()
        {
        return Coherence.SYSTEM_SCOPE;
        }

    @Override
    public Optional<String> getConfigUri()
        {
        return Optional.of(Locks.CONFIG_URI);
        }

    // ----- inner class: AtomicsSessionProvider ----------------------------

    /**
     * The custom Locks session provider.
     */
    public static class LocksSessionProvider
            implements SessionProvider
        {
        // ----- SessionProvider interface ----------------------------------

        @Override
        public int getPriority()
            {
            return PROVIDER_PRIORITY;
            }

        @Override
        public Context createSession(SessionConfiguration configuration, Context context)
            {
            if (Locks.SESSION_NAME.equals(configuration.getName()))
                {
                if (context.getMode() == Coherence.Mode.ClusterMember)
                    {
                    // we only add this Atomics session on a cluster member
                    return context.createSession(new LocksSessionConfiguration());
                    }
                else
                    {
                    // there is no Atomics session on an Extend client.
                    return context.complete();
                    }
                }
            // the request was not for the Atomics session
            return context;
            }
        }

    // ----- constants ------------------------------------------------------

    /**
     * The session provider priority.
     */
    public static final int PROVIDER_PRIORITY = 0;
    }
