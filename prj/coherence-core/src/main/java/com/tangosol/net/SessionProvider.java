/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net;

import com.oracle.coherence.common.util.Options;

import com.tangosol.internal.net.DefaultSessionProvider;

import com.tangosol.net.events.EventInterceptor;
import com.tangosol.net.options.WithClassLoader;
import com.tangosol.net.options.WithConfiguration;
import com.tangosol.net.options.WithName;
import com.tangosol.net.options.WithScopeName;

import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
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
        extends Comparable<SessionProvider>
    {
    // ----- SessionProvider methods ----------------------------------------

    /**
     * Create a {@link Session} from the specified configuration.
     *
     * @param configuration  the configuration to use to create the session
     * @param mode           the current {@link com.tangosol.net.Coherence.Mode}
     *
     * @return an {@link Optional} containing a {@link Session} or an empty
     *         {@link Optional} if this provider cannot supply a {@link Session}
     *         from the specified configuration
     */
    default Optional<Session> createSession(SessionConfiguration configuration, Coherence.Mode mode)
        {
        return createSession(configuration, mode, Collections.emptyList());
        }

    /**
     * Create a {@link Session} from the specified configuration.
     *
     * @param configuration  the configuration to use to create the session
     * @param defaultMode    the {@link com.tangosol.net.Coherence.Mode} the session should use
     *                       if not specified in the {@link SessionConfiguration}
     * @param interceptors   optional {@link EventInterceptor interceptors} to add to
     *                       the session in addition to any in the configuration
     *
     * @return an {@link Optional} containing a {@link Session} or an empty
     *         {@link Optional} if this provider cannot supply a {@link Session}
     *         from the specified configuration
     */
    default Optional<Session> createSession(SessionConfiguration                    configuration,
                                            Coherence.Mode                          defaultMode,
                                            Iterable<? extends EventInterceptor<?>> interceptors)
        {
        return createSession(configuration, defaultMode, null, interceptors);
        }

    /**
     * Create a {@link Session} from the specified configuration.
     *
     * @param configuration  the configuration to use to create the session
     * @param defaultMode    the {@link com.tangosol.net.Coherence.Mode} the session should use
     *                       if not specified in the {@link SessionConfiguration}
     * @param sScopePrefix   the prefix to prepend to the session scope
     * @param interceptors   optional {@link EventInterceptor interceptors} to add to
     *                       the session in addition to any in the configuration
     *
     * @return an {@link Optional} containing a {@link Session} or an empty
     *         {@link Optional} if this provider cannot supply a {@link Session}
     *         from the specified configuration
     */
    default Optional<Session> createSession(SessionConfiguration                    configuration,
                                            Coherence.Mode                          defaultMode,
                                            String                                  sScopePrefix,
                                            Iterable<? extends EventInterceptor<?>> interceptors)
        {
        Coherence.Mode mode    = configuration.getMode().orElse(defaultMode);
        Context        context = new DefaultContext(mode, DefaultSessionProvider.getBaseProvider(), interceptors, sScopePrefix);
        Context        result  = createSession(configuration, context);
        return result == null ? Optional.empty() : Optional.ofNullable(result.getSession());
        }

    /**
     * Create a {@link Session} from the specified configuration.
     * <p>
     * The return value is a {@link Context} that may be the same context passed
     * in. If the provider could create a {@link Session} the context will be
     * completed and contain a session. If the provider cannot create a session
     * the context will not be completed. If the provider could not create a
     * session and no other providers should be tried then the result context
     * will be completed without a session.
     *
     * @param configuration  the configuration to use to create the session
     * @param context        the {@link Context} to use when creating the session
     *
     * @return the resulting {@link Context} either not completed, completed with a
     *         {@link Session} or completed empty.
     */
    Context createSession(SessionConfiguration configuration, Context context);

    /**
     * Create a {@link Session} using the specified {@link Option}s.
     *
     * @param options  the {@link Session.Option}s for creating the {@link Session}
     *
     * @return a new {@link Session} or {@code null} if this provider cannot
     *         supply a {@link Session} from the specified options
     *
     * @throws IllegalArgumentException
     *              when a {@link Session} can't be creating using the
     *              specified {@link Option}.
     *
     * @deprecated  since 20.12 - use {@link #createSession(SessionConfiguration, Coherence.Mode, Iterable)}
     */
    @Deprecated
    default Session createSession(Session.Option... options)
        {
        Options<Session.Option>      opts    = Options.from(Session.Option.class, options);
        SessionConfiguration.Builder builder = SessionConfiguration.builder();

        opts.ifPresent(WithName.class, option -> builder.named(option.getName()));
        opts.ifPresent(WithConfiguration.class, option -> builder.withConfigUri(option.getLocation()));
        opts.ifPresent(WithClassLoader.class, option -> builder.withClassLoader(option.getClassLoader()));
        opts.ifPresent(WithScopeName.class, option -> builder.withScopeName(option.getScopeName()));

        return createSession(builder.build(), Coherence.Mode.ClusterMember, Collections.emptyList())
                .orElseThrow(() -> new IllegalArgumentException("Cannot create a session from the specified options"));
        }

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
        return PRIORITY;
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

    @Override
    default int compareTo(SessionProvider other)
        {
        return Integer.compare(getPriority(), other.getPriority());
        }

    /**
     * Release the {@link Session}.
     *
     * @param session  the {@link Session} to release
     */
    default void releaseSession(Session session)
        {
        }

    // ----- Option interface -----------------------------------------------

    /**
     * An immutable option for creating and configuring {@link SessionProvider}s.
     *
     * @deprecated since 20.12 use {@link SessionConfiguration} and
     * {@link SessionProvider#createSession(SessionConfiguration, Coherence.Mode, Iterable)}
     */
    @Deprecated
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
     *
     * @deprecated since 20.12 {@link Option} is deprecated use {@link #get()}
     */
    @Deprecated
    @SuppressWarnings("unused")
    static SessionProvider get(Option... options)
        {
        return get();
        }

    /**
     * Acquire the {@link SessionProvider} to use to create sessions.
     *
     * @return a {@link SessionProvider}
     */
    static SessionProvider get()
        {
        return DefaultSessionProvider.INSTANCE;
        }

    // ----- inner class: Providers -----------------------------------------

    /**
     * A {@link SessionProvider} that builds {@link Session} instances using
     * {@link SessionProvider}s loaded via the {@link ServiceLoader} falling back
     * to the default {@link CacheFactoryBuilder} provider if no other providers
     * can build a {@link Session} from the specified options.
     *
     * @deprecated since 20.12 there is no replacement for this class
     */
    @Deprecated
    class Providers
            extends DefaultSessionProvider
        {
        }

    // ----- inner interface Context ----------------------------------------

    /**
     * A context used by providers.
     */
    interface Context
        {
        /**
         * Returns the mode that {@link Coherence} is running in.
         *
         * @return the mode that {@link Coherence} is running in
         */
        Coherence.Mode getMode();

        /**
         * Complete this context.
         *
         * @param session  the {@link Session} created by the provider.
         *
         * @return a completed {@link Context} containing the {@link Session}
         */
        Context complete(Session session);

        /**
         * Complete this context without a session.
         * <p>
         * This will cause the root provider to return a {@code null}
         * session without trying any further providers in its chain.
         *
         * @return a completed empty {@link Context}
         */
        Context complete();

        /**
         * Return {@code true} if this context has been completed.
         *
         * @return {@code true} if this context has been completed
         */
        boolean isComplete();

        /**
         * Return {@code true} if the context contains a non-null
         * {@link Session} instance.
         *
         * @return {@code true} if the context contains a non-null
         *         {@link Session} instance
         */
        boolean hasSession();

        /**
         * Return the {@link Session} created by the provider,
         * or {@code null} if no session could be created.
         *
         * @return the {@link Session} created by the provider or
         *         {@code null} if no session could be created
         */
        Session getSession();

        /**
         * Returns the default session provider that may be used to delegate
         * to for session creation.
         *
         * @return the default session provider
         */
        SessionProvider defaultProvider();

        /**
         * Returns zero or more {@link EventInterceptor} instances to add to the session.
         *
         * @return zero or more {@link EventInterceptor} instances to add to the session
         */
        Iterable<? extends EventInterceptor<?>> getInterceptors();

        /**
         * Returns the {@link Context} from calling the default {@link #defaultProvider()}
         * {@link SessionProvider#createSession(SessionConfiguration, Coherence.Mode, Iterable)}
         * method to create a session.
         *
         * @param configuration  the session configuration to use
         *
         * @return the {@link Context} returned by the default provider
         */
        default Context createSession(SessionConfiguration configuration)
            {
            return defaultProvider().createSession(configuration, this);
            }

        /**
         * Return any prefix to prepend to the scope name of the session.
         *
         * @return any prefix to prepend to the scope name of the session
         */
        default String getScopePrefix()
            {
            return Coherence.DEFAULT_SCOPE;
            }
        }

    // ----- inner class DefaultContext -------------------------------------

    class DefaultContext
            implements Context
        {
        /**
         * Create a new default context.
         *
         * @param mode          the mode to create the session
         * @param provider      the default {@link SessionProvider}
         * @param sScopePrefix  the prefix to prepend to the session scope
         * @param interceptors  the interceptors to add to the session
         *
         * @throws NullPointerException if either parameter is {@code null}
         */
        public DefaultContext(Coherence.Mode                          mode,
                              SessionProvider                         provider,
                              Iterable<? extends EventInterceptor<?>> interceptors,
                              String                                  sScopePrefix)
            {
            f_mode            = Objects.requireNonNull(mode);
            f_sessionProvider = Objects.requireNonNull(provider);
            f_interceptors    = interceptors;
            f_sScopePrefix    = sScopePrefix == null ? Coherence.DEFAULT_SCOPE : sScopePrefix.trim();
            }

        @Override
        public Coherence.Mode getMode()
            {
            return f_mode;
            }

        @Override
        public Context complete(Session session)
            {
            m_session   = session;
            m_fComplete = true;
            return this;
            }

        @Override
        public Context complete()
            {
            m_fComplete = true;
            return this;
            }

        @Override
        public boolean isComplete()
            {
            return m_fComplete;
            }

        @Override
        public boolean hasSession()
            {
            return m_session != null;
            }

        @Override
        public Session getSession()
            {
            return m_session;
            }

        @Override
        public SessionProvider defaultProvider()
            {
            return f_sessionProvider;
            }

        @Override
        public Iterable<? extends EventInterceptor<?>> getInterceptors()
            {
            return f_interceptors;
            }

        @Override
        public String getScopePrefix()
            {
            return f_sScopePrefix == null ? Context.super.getScopePrefix() : f_sScopePrefix;
            }

        // ----- data members -----------------------------------------------

        /**
         * The mode to create the session.
         */
        private final Coherence.Mode f_mode;

        /**
         * The default session provider.
         */
        private final SessionProvider f_sessionProvider;

        /**
         * The interceptors to add to the session.
         */
        private final Iterable<? extends EventInterceptor<?>> f_interceptors;

        /**
         * {@code true} if the context is complete.
         */
        private boolean m_fComplete;

        /**
         * The optional {@link Session} created by a provider.
         */
        private Session m_session;

        private final String f_sScopePrefix;
        }

    // ----- inner interface Supplier ---------------------------------------

    /**
     * Implemented by suppliers of a {@link SessionProvider}.
     */
    interface Provider
        {
        /**
         * Provide an optional {@link SessionProvider}.
         *
         * @return an optional {@link SessionProvider}
         */
        Optional<SessionProvider> getSessionProvider();
        }

    // ----- constants ------------------------------------------------------

    /**
     * The default priority for providers.
     */
    public static final int PRIORITY = 0;
    }
