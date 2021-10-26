/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.atomic;

import com.tangosol.net.Coherence;
import com.tangosol.net.SessionConfiguration;
import com.tangosol.net.SessionProvider;

import java.util.Optional;

/**
 * A {@link SessionConfiguration} for the server side atomics session.
 *
 * @author Aleks Seovic  2021.01.08
 * @since 21.12
 */
public class AtomicsSessionConfiguration
        implements SessionConfiguration
    {
    // ----- AtomicsSessionConfiguration methods ----------------------------

    @Override
    public String getName()
        {
        return Atomics.SESSION_NAME;
        }

    @Override
    public String getScopeName()
        {
        return Coherence.SYSTEM_SCOPE;
        }

    @Override
    public Optional<String> getConfigUri()
        {
        return Optional.of(Atomics.CONFIG_URI);
        }

    // ----- inner class: AtomicsSessionProvider ----------------------------

    /**
     * The custom Atomics session provider.
     */
    public static class AtomicsSessionProvider
            implements SessionProvider
        {
        // ----- SessionProvider interface ----------------------------------

        @Override
        public Context createSession(SessionConfiguration configuration, Context context)
            {
            if (Atomics.SESSION_NAME.equals(configuration.getName()))
                {
                if (context.getMode() == Coherence.Mode.ClusterMember)
                    {
                    // we only add this Atomics session on a cluster member
                    return context.createSession(new AtomicsSessionConfiguration());
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
    }
