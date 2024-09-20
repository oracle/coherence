/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net;

import com.tangosol.net.Coherence;
import com.tangosol.net.SessionConfiguration;
import com.tangosol.net.SessionProvider;

import java.util.Optional;

/**
 * A {@link SessionConfiguration} for the server side system session.
 *
 * @author Jonathan Knight  2020.12.16
 * @since 20.12
 */
public class SystemSessionConfiguration
        implements SessionConfiguration
    {
    /**
     * Create a {@link SystemSessionConfiguration}.
     *
     * @param mode the mode the session will run in
     */
    public SystemSessionConfiguration(Coherence.Mode mode)
        {
        f_mode   = mode == null ? Coherence.Mode.ClusterMember : mode;
        f_sScope = Coherence.SYSTEM_SCOPE;
        }

    // ----- SystemSessionConfiguration methods -----------------------------

    @Override
    public String getName()
        {
        return Coherence.SYSTEM_SESSION;
        }

    @Override
    public String getScopeName()
        {
        return f_sScope;
        }

    @Override
    public Optional<String> getConfigUri()
        {
        return Optional.of(Coherence.SYS_CCF_URI);
        }

    @Override
    public Optional<Coherence.Mode> getMode()
        {
        return Optional.of(f_mode);
        }

    // ----- inner class: SystemSessionProvider -----------------------------

    /**
     * The custom System session provider.
     */
    public static class SystemSessionProvider
            implements SessionProvider
        {
        @Override
        public int getPriority()
            {
            return PROVIDER_PRIORITY;
            }

        @Override
        public Context createSession(SessionConfiguration configuration, Context context)
            {
            if (configuration instanceof SystemSessionConfiguration)
                {
                return context.createSession(configuration);
                }
            if (Coherence.SYSTEM_SESSION.equals(configuration.getName()))
                {
                return context.createSession(new SystemSessionConfiguration(context.getMode()));
                }
            // the request was not for the system session
            return context;
            }

        /**
         * The singleton {@link SystemSessionProvider} instance.
         */
        static final SystemSessionProvider INSTANCE = new SystemSessionProvider();
        }

    // ----- constants ------------------------------------------------------

    /**
     * The session provider priority.
     */
    public static final int PROVIDER_PRIORITY = 0;

    // ----- data members ---------------------------------------------------

    private final Coherence.Mode f_mode;

    private final String f_sScope;
    }
