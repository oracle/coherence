/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net;

import com.oracle.coherence.common.base.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;

/**
 * Creates {@link Session}s for use by applications requiring Coherence-based
 * resources, including {@link NamedCache}s, often for specific
 * {@link ClassLoader}s, for deployed modules.
 *
 * @see Session
 *
 * @author bo 2015.07.27
 */
public interface SessionProvider
    {
    // ----- SessionProvider methods ----------------------------------------

    /**
     * Create a {@link Session} using the specified {@link Option}s.
     *
     * @param options  the {@link Session.Option}s for creating the {@link Session}
     *
     * @return a new {@link Session}
     *
     * @throws IllegalArgumentException
     *              when a {@link Session} can't be creating using the
     *              specified {@link Option}.
     */
    Session createSession(Session.Option... options);

    /**
     * Obtain the priority that this {@link SessionProvider}
     * should have over other {@link SessionProvider}s when multiple
     * providers may be able to provide a {@link Session}.
     * <p>
     * Higher values are higher precedence.
     *
     * @return this {@link SessionProvider}'s priority.
     */
    default int getPriority()
        {
        return 0;
        }

    /**
     * Optionally close all of the {@link Session} instances provided by
     * this {@link SessionProvider}.
     * <p>
     * This allows providers where sessions consume external resources, such
     * as remote connections, to clean up.
     */
    default void close()
        {
        }

    // ----- Option interface -----------------------------------------------

    /**
     * An immutable option for creating and configuring {@link SessionProvider}s.
     */
    interface Option
        {
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Acquire the {@link SessionProvider} based on the current calling context
     * and the provided {@link Option}s.
     *
     * @param options  the {@link Option}s for acquiring the {@link SessionProvider}
     *
     * @return a {@link SessionProvider}
     *
     * @throws IllegalArgumentException
     *              when a {@link SessionProvider} can't be acquired using the
     *              specified {@link Option}s
     *
     * @throws IllegalStateException
     *              when a {@link SessionProvider} can't be auto-detected
     */
    static SessionProvider get(Option... options)
        {
        return Providers.INSTANCE;
        }

    // ----- inner class: Providers -----------------------------------------

    /**
     * A {@link SessionProvider} that builds {@link Session} instances using
     * {@link SessionProvider}s loaded via the {@link ServiceLoader} falling back
     * to the default {@link CacheFactoryBuilder} provider if no other providers
     * can build a {@link Session} from the specified options.
     */
    class Providers
            implements SessionProvider
        {
        // ----- constructors -----------------------------------------------

        /**
         * Create a {@link Providers} instance.
         */
        Providers()
            {
            m_defaultProvider = CacheFactory.getCacheFactoryBuilder();
            List<SessionProvider>          list   = new ArrayList<>();
            ServiceLoader<SessionProvider> loader = ServiceLoader.load(SessionProvider.class);
            for (SessionProvider provider : loader)
                {
                list.add(provider);
                }
            list.sort(Comparator.comparingInt(SessionProvider::getPriority));
            m_listProvider = list;
            }

        // ----- SessionProvider methods ------------------------------------

        @Override
        public Session createSession(Session.Option... options)
            {
            for (SessionProvider provider : m_listProvider)
                {
                Session session = provider.createSession(options);
                if (session != null)
                    {
                    return session;
                    }
                }
            return m_defaultProvider.createSession(options);
            }

        @Override
        public void close()
            {
            for (SessionProvider provider : m_listProvider)
                {
                try
                    {
                    provider.close();
                    }
                catch (Throwable t)
                    {
                    Logger.err(t);
                    }
                }
            }

        // ----- constructors -----------------------------------------------

        /**
         * The singleton {@link Providers} instance.
         */
        private static final Providers INSTANCE = new Providers();

        // ----- data members -----------------------------------------------

        private final SessionProvider m_defaultProvider;

        private final List<SessionProvider> m_listProvider;
        }
    }
