/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net;

import com.oracle.coherence.common.base.Logger;

import com.oracle.coherence.common.util.MutableOptions;

import com.tangosol.internal.net.ConfigurableCacheFactorySession;

import com.tangosol.internal.net.ScopedUriScopeResolver;
import com.tangosol.net.events.CoherenceDispatcher;
import com.tangosol.net.events.CoherenceLifecycleEvent;
import com.tangosol.net.events.EventDispatcher;
import com.tangosol.net.events.EventDispatcherAwareInterceptor;
import com.tangosol.net.events.EventDispatcherRegistry;
import com.tangosol.net.events.EventInterceptor;
import com.tangosol.net.events.InterceptorRegistry;
import com.tangosol.net.events.internal.CoherenceEventDispatcher;
import com.tangosol.net.events.internal.Registry;

import com.tangosol.net.options.WithName;

import com.tangosol.util.CopyOnWriteMap;
import com.tangosol.util.RegistrationBehavior;
import com.tangosol.util.ResourceRegistry;
import com.tangosol.util.SimpleResourceRegistry;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;

import java.util.concurrent.CompletableFuture;

import java.util.stream.Collectors;

/**
 * A {@link Coherence} instance encapsulates and controls one or more
 * {@link Session} instances.
 * <p>
 * A {@link Coherence} instance is typically created from a {@link CoherenceConfiguration}
 * which contains one or more {@link SessionConfiguration} instances.
 * The {@link Coherence} instance is then {@link #start() started} which in turn creates
 * and starts the configured {@link Session Sessions}.
 * When the {@link Coherence} instance is {@link #close() closed} all of the {@link Session}
 * instances being managed are also {@link Session#close() closed}.
 * <p>
 * An example of creating and starting a {@link Coherence} instance is shown below:
 * <pre><code>
 *    SessionConfiguration session = SessionConfiguration.builder()
 *            .named("Prod")
 *            .withConfigUri("cache-config.xml")
 *            .build();
 *
 *    CoherenceConfiguration cfg = CoherenceConfiguration.builder()
 *            .withSession(SessionConfiguration.defaultSession())
 *            .withSession(session)
 *            .build();
 *
 *    Coherence coherence = Coherence.create(cfg);
 *
 *    coherence.start();
 * </code></pre>
 *
 * @author Jonathan Knight  2020.10.26
 * @since 20.12
 *
 * @see CoherenceConfiguration
 * @see SessionConfiguration
 */
public class Coherence
        implements AutoCloseable
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a {@link Coherence} instance using the state from the
     * specified {@link CoherenceConfiguration}.
     * <p>
     * This constructor is private, instances of {@link Coherence} should
     * be created using the factory {@link #create(CoherenceConfiguration)}
     * and {@link #builder(CoherenceConfiguration)} methods.
     *
     * @param config  the {@link CoherenceConfiguration} to configure this instance
     *
     * @throws NullPointerException if the config parameter is {@code null}
     */
    private Coherence(CoherenceConfiguration config)
        {
        f_config     = Objects.requireNonNull(config);
        f_sName      = config.getName();
        f_registry   = new SimpleResourceRegistry();
        f_dispatcher = new CoherenceEventDispatcher(this);

        Registry eventRegistry = new Registry();
        f_registry.registerResource(InterceptorRegistry.class, eventRegistry);
        f_registry.registerResource(EventDispatcherRegistry.class, eventRegistry);
        eventRegistry.registerEventDispatcher(f_dispatcher);
        for (EventInterceptor<?> interceptor : f_config.getInterceptors())
            {
            eventRegistry.registerEventInterceptor(interceptor);
            }
        }

    // ----- factory methods ------------------------------------------------

    /**
     * Create a default {@link Coherence} instance.
     *
     * @return a default {@link Coherence} instance
     */
    public static Coherence create()
        {
        return builder(CoherenceConfiguration.create()).build();
        }

    /**
     * Create a {@link Coherence} instance from the specified {@link CoherenceConfiguration}.
     *
     * @param config  the configuration to use to create the
     *                {@link Coherence} instance
     *
     * @return a {@link Coherence} instance from the specified {@link CoherenceConfiguration}
     */
    public static Coherence create(CoherenceConfiguration config)
        {
        return builder(config).build();
        }

    /**
     * Returns a {@link Builder} instance that can build a {@link Coherence}
     * instance using the specified {@link CoherenceConfiguration}.
     *
     * @return a {@link Builder} instance that can build a {@link Coherence}
     *         instance using the specified {@link CoherenceConfiguration}
     */
    public static Builder builder(CoherenceConfiguration config)
        {
        return new Builder(config);
        }

    /**
     * Returns all of the {@link Coherence} instances.
     *
     * @return all of the {@link Coherence} instances
     */
    public static Collection<Coherence> getInstances()
        {
        return Collections.unmodifiableCollection(s_mapInstance.values());
        }

    /**
     * Returns the named {@link Coherence} instance or {@code null}
     * if the name is {@code null} or no {@link Coherence} instance exists
     * with the specified name.
     *
     * @param sName  the name of the {@link Coherence} instance to return
     *
     * @return the named {@link Coherence} instance or {@code null} if no
     *         {@link Coherence} instance exists with the specified name
     */
    public static Coherence getInstance(String sName)
        {
        return sName == null ? null : s_mapInstance.get(sName);
        }

    /**
     * Returns a {@link Coherence} instance created or {@code null}
     * if no {@link Coherence} instance exists.
     * <p>
     * This method is useful if only a single {@link Coherence} instance
     * exists in an application. If multiple instances exists the actual
     * instance returned is undetermined.
     *
     * @return a {@link Coherence} instance created
     */
    public static Coherence getInstance()
        {
        Coherence coherence = s_mapInstance.get(Coherence.DEFAULT_NAME);
        if (coherence != null)
            {
            return coherence;
            }
        return s_mapInstance.entrySet()
                .stream()
                .findFirst()
                .map(Map.Entry::getValue)
                .orElse(null);
        }

    /**
     * Find the {@link Session} instance with the given name across
     * all {@link Coherence} instances.
     *
     * @param sName  the {@link Session} name to find
     *
     * @return the an {@link Optional} containing the {@link Session} with
     *         the specified name, or an empty optional if the session does
     *         not exist
     */
    public static Optional<Session> findSession(String sName)
        {
        for (Coherence coherence : s_mapInstance.values())
            {
            Session session = coherence.getSession(sName);
            if (session != null)
                {
                return Optional.of(session);
                }
            }
        return Optional.empty();
        }

    /**
     * Find the {@link Session} instances with the given scope name across
     * all {@link Coherence} instances.
     *
     * @param sScope  the scope name of the {@link Session Sessions} to find
     *
     * @return the an {@link Optional} containing the {@link Session} with
     *         the specified name, or an empty optional if the session does
     *         not exist
     */
    public static Collection<Session> findSessionsByScope(String sScope)
        {
        return s_mapInstance.values()
                            .stream()
                            .flatMap(coh -> coh.getSessionsWithScope(sScope).stream())
                            .collect(Collectors.toList());
        }

    /**
     * Close all {@link Coherence} instance.
     */
    public static synchronized void closeAll()
        {
        Logger.info("Stopping all Coherence instances");
        s_mapInstance.values().forEach(Coherence::close);
        if (s_serverSystem != null)
            {
            s_serverSystem.stop();
            }
        Logger.info("All Coherence instances stopped");
        s_serverSystem = null;
        s_sessionSystem = null;
        }

    // ----- Coherence API --------------------------------------------------

    /**
     * Returns the name of this {@link Coherence} instance.
     *
     * @return the name of this {@link Coherence} instance
     */
    public String getName()
        {
        return f_sName;
        }

    /**
     * Returns the configuration used by this {@link Coherence} instance.
     *
     * @return the configuration used by this {@link Coherence} instance
     */
    public CoherenceConfiguration getConfiguration()
        {
        return f_config;
        }

    /**
     * Returns {@code true} if this {@link Coherence} instance can
     * provide a {@link Session} with the specified name.
     *
     * @param sName  the name of the {@link Session}
     *
     * @return {@code true} if this {@link Coherence} instance can
     *         provide a {@link Session} with the specified name
     */
    public boolean hasSession(String sName)
        {
        return f_config.getSessionConfigurations().containsKey(sName);
        }

    /**
     * Obtain the default {@link Session} from the {@link Coherence} instance.
     *
     * @return the default {@link Session}
     *
     * @throws IllegalStateException  if this instance has been closed
     */
    public Session getSession()
        {
        return getSession(DEFAULT_NAME);
        }

    /**
     * Obtain the {@link Session} from the {@link Coherence} instance that was
     * configured with the specified configuration name.
     *
     * @param sName  the name of the {@link Session} to return
     *
     * @return the named {@link Session}
     *
     * @throws IllegalStateException  if this instance has been closed
     */
    public Session getSession(String sName)
        {
        assertNotClosed();

        if (Coherence.SYSTEM_SESSION.equals(sName))
            {
            return Coherence.getSystemSession();
            }

        String sSessionName = sName == null ? Coherence.DEFAULT_NAME : sName;

        SessionConfiguration configuration = f_config.getSessionConfigurations().get(sSessionName);
        if (configuration == null || !configuration.isEnabled())
            {
            throw new IllegalArgumentException("No Session has been configured with the name " + sSessionName);
            }

        return f_mapSession.compute(sSessionName, (k, session) ->
            {
            if (session == null || !session.isActive())
                {
                session = ensureSessionInternal(sSessionName, configuration);
                }
            return session;
            });
        }

    /**
     * Obtain all the {@link Session Sessions} from the {@link Coherence} instance
     * that are configured with the specified scope name.
     *
     * @param sScope  the scope name of the {@link Session Sessions} to return
     *
     * @return the {@link Session} instances with the specified scope or an
     *         an empty {@link Collection} if no {@link Session Sessions}
     *         have the required scope name
     *
     * @throws IllegalStateException  if this instance has been closed
     */
    public Collection<Session> getSessionsWithScope(String sScope)
        {
        assertNotClosed();

        String sScopeName = sScope == null ? Coherence.DEFAULT_SCOPE : sScope;

        return f_config.getSessionConfigurations()
                       .values()
                       .stream()
                       .filter(cfg -> sScopeName.equals(cfg.getScopeName()))
                       .map(SessionConfiguration::getName)
                       .map(this::getSession)
                       .collect(Collectors.toList());
        }

    /**
     * Obtain a {@link CompletableFuture} that will be completed when
     * this {@link Coherence} instance has started.
     *
     * @return a {@link CompletableFuture} that will be completed when
     *         this {@link Coherence} instance has started
     */
    public CompletableFuture<Void> whenStarted()
        {
        return f_futureStarted;
        }

    /**
     * Obtain a {@link CompletableFuture} that will be completed when
     * this {@link Coherence} instance has closed.
     *
     * @return a {@link CompletableFuture} that will be completed when
     *         this {@link Coherence} instance has closed
     */
    public CompletableFuture<Void> whenClosed()
        {
        return f_futureClosed;
        }

    /**
     * Return {@code true} if this {@link Coherence} instance has been started.
     *
     * @return {@code true} if this {@link Coherence} instance has been started
     */
    public boolean isStarted()
        {
        return m_fStarted && !m_fClosed;
        }

    /**
     * Return {@code true} if this {@link Coherence} instance has been closed.
     *
     * @return {@code true} if this {@link Coherence} instance has been closed
     */
    public boolean isClosed()
        {
        return m_fClosed;
        }

    /**
     * Start this {@link Coherence} instance.
     * <p>
     * If this instance already been started and has not
     * been closed this method call is a no-op.
     *
     * @return a {@link CompletableFuture} that will be completed when
     *         this {@link Coherence} instance has started
     */
    public synchronized CompletableFuture<Void> start()
        {
        assertNotClosed();
        if (m_fStarted)
            {
            return f_futureStarted;
            }

        f_dispatcher.dispatchStarting();
        m_fStarted = true;

        try
            {
            if (f_mapServer.isEmpty())
                {
                startInternal();
                }

            CompletableFuture.runAsync(() ->
                {
                try
                    {
                    f_mapServer.values().forEach(holder -> holder.getServer().waitForServiceStart());
                    f_futureStarted.complete(null);
                    }
                catch (Throwable thrown)
                    {
                    Logger.err(thrown);
                    f_futureStarted.completeExceptionally(thrown);
                    }
                });
            f_dispatcher.dispatchStarted();
            }
        catch (Throwable thrown)
            {
            f_futureStarted.completeExceptionally(thrown);
            }
        return f_futureStarted;
        }

    /**
     * Close this {@link Coherence} instance.
     */
    @Override
    public synchronized void close()
        {
        if (m_fClosed)
            {
            return;
            }

        f_dispatcher.dispatchStopping();
        m_fClosed = true;
        try
            {
            s_mapInstance.remove(f_sName);
            if (m_fStarted)
                {
                m_fStarted = false;

                // Stop servers in revers order
                f_mapServer.values()
                        .stream()
                        .sorted(Comparator.reverseOrder())
                        .map(PriorityHolder::getServer)
                        .forEach(this::stopServer);

                f_mapServer.clear();
                }
            f_futureClosed.complete(null);
            }
        catch (Throwable thrown)
            {
            Logger.err(thrown);
            f_futureClosed.completeExceptionally(thrown);
            }

        f_dispatcher.dispatchStopped();
        f_registry.dispose();
        }

    /**
     * Returns the System {@link Session}.
     *
     * @return the System {@link Session}
     */
    public static Session getSystemSession()
        {
        return ensureSystemSession(Collections.emptyList());
        }

    /**
     * Return the {@link ResourceRegistry} for this {@link Coherence} instance.
     *
     * @return the ResourceRegistry for this {@link Coherence} instance
     */
    public ResourceRegistry getResourceRegistry()
        {
        return f_registry;
        }

    /**
     * Return the {@link InterceptorRegistry} for this {@link Coherence} instance.
     *
     * @return the {@link InterceptorRegistry} for this {@link Coherence} instance
     */
    public InterceptorRegistry getInterceptorRegistry()
        {
        return f_registry.getResource(InterceptorRegistry.class);
        }

    /**
     * Return a {@link Cluster} object for Coherence services.
     *
     * @return a {@link Cluster} object, which may or may not be running
     */
    public Cluster getCluster()
        {
        return CacheFactory.getCluster();
        }

    /**
     * Start a Coherence server.
     * <p>
     * This method will start Coherence configured with a single session
     * that uses the default cache configuration file. This is effectively
     * the same as running {@link DefaultCacheServer#main(String[])} without
     * any arguments but will additionally bootstrap a {@link Coherence}
     * instance with the default name {@link Coherence#DEFAULT_NAME}
     * and an unscoped {@link Session} with the same default name.
     *
     * @param args  the program arguments.
     */
    public static void main(String[] args)
        {
        Coherence coherence = Coherence.create();
        coherence.start();
        // block forever (or until the Coherence instance is shutdown)
        coherence.whenClosed().join();
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Assert that this {@link Coherence} instance is not closed.
     */
    private void assertNotClosed()
        {
        if (m_fClosed)
            {
            throw new IllegalStateException("This " + getClass().getSimpleName() + " instance has been closed");
            }
        }

    /**
     * Start this {@link Coherence} instance.
     */
    private synchronized void startInternal()
        {
        Iterable<EventInterceptor<?>> globalInterceptors = f_config.getInterceptors();

        // ensure there is a System Session and register the interceptors with it before doing anything else
        ensureSystemSession(globalInterceptors);

        Logger.info(() -> "Starting Coherence instance " + f_sName);

        try
            {
            // Creat the sessions in priority order of their configurations
            f_config.getSessionConfigurations()
                    .values()
                    .stream()
                    .sorted()
                    .forEach(configuration -> {
                        if (configuration.isEnabled())
                            {
                            String sName = configuration.getName();

                            Logger.finer(() -> "Starting Coherence Session " + sName);

                            Session                       session      = ensureSessionInternal(sName, configuration);
                            Iterable<EventInterceptor<?>> interceptors = configuration.getInterceptors();
                            registerInterceptors(session, globalInterceptors);
                            registerInterceptors(session, interceptors);

                            if (session instanceof ConfigurableCacheFactorySession)
                                {
                                ConfigurableCacheFactorySession supplier = (ConfigurableCacheFactorySession) session;
                                ConfigurableCacheFactory        ccf      = supplier.getConfigurableCacheFactory();
                                DefaultCacheServer              dcs      = startCCF(sName, ccf);
                                f_mapServer.put(sName, new PriorityHolder(configuration.getPriority(), dcs));
                                }
                            }
                    });
            }
        catch (Throwable t)
            {
            Logger.err("Failed to start Coherence instance", t);
            close();
            }

        Logger.info(() -> "Started Coherence instance " + f_sName);
        }

    /**
     * Ensure the specified {@link Session} exists.
     *
     * @param sName          the name of the {@link Session}
     * @param configuration  the {@link SessionConfiguration configuration} for the {@link Session}
     *
     * @return the configured and started {@link Session}
     */
    private static Session ensureSessionInternal(String sName, SessionConfiguration configuration)
        {
        MutableOptions<Session.Option> options  = new MutableOptions<>(Session.Option.class);
        SessionProvider                provider = configuration.getSessionProvider()
                                                               .orElse(SessionProvider.get());

        options.addAll(configuration.getOptions());
        options.add(WithName.of(sName));
        Session.Option[] opts = options.asArray();

        return provider.createSession(opts);
        }

    /**
     * Register the specified {@link EventInterceptor interceptors} with the {@link Session}.
     *
     * @param session       the {@link Session} to register the interceptors with
     * @param interceptors  the {@link EventInterceptor interceptors} to register
     */
    private static void registerInterceptors(Session session, Iterable<? extends EventInterceptor<?>> interceptors)
        {
        InterceptorRegistry registry = session.getInterceptorRegistry();
        for (EventInterceptor<?> interceptor : interceptors)
            {
            registry.registerEventInterceptor(interceptor, RegistrationBehavior.FAIL);
            }
        }

    /**
     * Activate and start the autostart services in the specified {@link ConfigurableCacheFactory}.
     * <p>
     * The {@link ConfigurableCacheFactory} will be wrapped in a {@link DefaultCacheServer} that
     * will monitor and manage the {@link ConfigurableCacheFactory} services.
     *
     * @param sName  the name of the {@link ConfigurableCacheFactory}
     * @param ccf    the {@link ConfigurableCacheFactory} to start
     *
     * @return the {@link DefaultCacheServer} that is managing and monitoring
     *         the {@link ConfigurableCacheFactory} services
     */
    private static DefaultCacheServer startCCF(String sName, ConfigurableCacheFactory ccf)
        {
        Logger.info(() -> "Starting Coherence Session " + sName + " with scope name " + ccf.getScopeName());
        DefaultCacheServer dcs = new DefaultCacheServer(ccf);
        dcs.startDaemon(DefaultCacheServer.DEFAULT_WAIT_MILLIS);
        return dcs;
        }

    /**
     * Stop a {@link DefaultCacheServer} instance.
     *
     * @param dcs  the {@link DefaultCacheServer} to stop
     */
    private void stopServer(DefaultCacheServer dcs)
        {
        try
            {
            dcs.stop();
            }
        catch (Throwable thrown)
            {
            Logger.err(thrown);
            }
        }

    /**
     * Perform the static initialization.
     * <p>
     * This will override the configured {@link CacheFactoryBuilder}
     * to use a {@link ScopedCacheFactoryBuilder} with a custom
     * scope resolver {@link ScopedUriScopeResolver}.
     */
    private static synchronized void init()
        {
        if (s_fInitialized)
            {
            return;
            }
        s_fInitialized = true;
        CacheFactory.setCacheFactoryBuilder(new ScopedCacheFactoryBuilder(new ScopedUriScopeResolver(false)));
        }

    /**
     * Ensure that the singleton system {@link Session} exists and is started.
     *
     * @param interceptors  any interceptors to add to the system session
     *
     * @return the singleton system {@link Session}
     */
    private static synchronized Session ensureSystemSession(Iterable<? extends EventInterceptor<?>> interceptors)
        {
        if (s_sessionSystem == null)
            {
            // Ensure we are properly initialised
            Coherence.init();

            SessionConfiguration            configuration = ensureSystemSessionConfiguration();
            ConfigurableCacheFactorySession sessionSystem = (ConfigurableCacheFactorySession)
                        ensureSessionInternal(configuration.getName(), configuration);

            registerInterceptors(sessionSystem, configuration.getInterceptors());
            registerInterceptors(sessionSystem, interceptors);

            ConfigurableCacheFactory ccfSystem = sessionSystem.getConfigurableCacheFactory();

            s_serverSystem  = startCCF(SYSTEM_SCOPE, ccfSystem);
            s_sessionSystem = sessionSystem;
            }
        else
            {
            registerInterceptors(s_sessionSystem, interceptors);
            }

        return s_sessionSystem;
        }

    /**
     * Visible for testing only, set the System {@link Session}.
     *
     * @param session the System {@link Session}
     */
    static void setSystemSession(ConfigurableCacheFactorySession session)
        {
        s_sessionSystem = session;
        }

    /**
     * Visible for testing only, set the System {@link SessionConfiguration}.
     *
     * @param cfg the System {@link SessionConfiguration}
     */
    static void setSystemSessionConfiguration(SessionConfiguration cfg)
        {
        s_cfgSystem = cfg;
        }

    /**
     * Return the {@link SessionConfiguration} to use to create the
     * System session.
     *
     * @return the {@link SessionConfiguration} to use to create the
     *         System session
     */
    private static SessionConfiguration ensureSystemSessionConfiguration()
        {
        if (s_cfgSystem == null)
            {
            return SessionConfiguration.builder()
                                       .named(SYSTEM_SCOPE)
                                       .withConfigUri(SYS_CCF_URI)
                                       .withScopeName(SYSTEM_SCOPE)
                                       .build();
            }
        return s_cfgSystem;
        }


    /**
     * Validate the {@link CoherenceConfiguration}.
     *
     * @param configuration  the {@link CoherenceConfiguration} to validate
     */
    private static void validate(CoherenceConfiguration configuration)
        {
        for (Map.Entry<String, SessionConfiguration> entry : configuration.getSessionConfigurations().entrySet())
            {
            String sName = entry.getKey();
            for (Coherence coherence : s_mapInstance.values())
                {
                if (coherence.hasSession(sName))
                    {
                    throw new IllegalStateException("A Session with the name '" + sName
                            + "' already exists in Coherence instance '" + coherence.getName() + "'");
                    }
                }
            }
        }

    // ----- inner class: PriorityHolder ------------------------------------

    /**
     * A holder of a priority and a {@link DefaultCacheServer}.
     */
    private static class PriorityHolder
            implements Comparable<PriorityHolder>
        {
        public PriorityHolder(int nPriority, DefaultCacheServer server)
            {
            f_nPriority = nPriority;
            f_server    = server;
            }

        public int getPriority()
            {
            return f_nPriority;
            }

        public DefaultCacheServer getServer()
            {
            return f_server;
            }

        @Override
        public int compareTo(PriorityHolder o)
            {
            return Integer.compare(f_nPriority, o.f_nPriority);
            }

        // ----- data members -----------------------------------------------

        /**
         * The priority of this holder.
         */
        private final int f_nPriority;

        /**
         * The {@link DefaultCacheServer} instance.
         */
        private final DefaultCacheServer f_server;
        }

    // ----- inner class: Builder -------------------------------------------

    /**
     * A builder to build {@link Coherence} instances.
     */
    public static class Builder
        {
        private Builder(CoherenceConfiguration config)
            {
            f_config = config;
            }

        /**
         * Build a {@link Coherence} instance.
         * 
         * @return a {@link Coherence} instance
         */
        public Coherence build()
            {
            // Ensure we are properly initialised
            Coherence.init();

            // validate the configuration
            Coherence.validate(f_config);

            Coherence coherence = new Coherence(f_config);
            Coherence prev      = s_mapInstance.putIfAbsent(f_config.getName(), coherence);
            if (prev != null)
                {
                throw new IllegalStateException("A Coherence instance with the name "
                                                + f_config.getName() + " already exists");
                }

            // discover any lifecycle listeners via the ServiceLoader
            InterceptorRegistry registry = coherence.getInterceptorRegistry();
            for (LifecycleListener listener : ServiceLoader.load(LifecycleListener.class))
                {
                registry.registerEventInterceptor(listener);
                }

            return coherence;
            }

        // ----- data members -----------------------------------------------

        /**
         * The configuration to use to build a {@link Coherence} instance.
         */
        private final CoherenceConfiguration f_config;
        }

    // ----- inner interface LifecycleListener ------------------------------

    /**
     * An interface implemented by listeners of {@link CoherenceLifecycleEvent CoherenceLifecycleEvents}.
     * <p>
     * Implementations of this interface properly registred as services will be discovered
     * using the {@link ServiceLoader} and automatically registered as interceptors with
     * each {@link Coherence} instance.
     */
    public interface LifecycleListener
            extends EventDispatcherAwareInterceptor<CoherenceLifecycleEvent>
        {
        @Override
        void onEvent(CoherenceLifecycleEvent event);

        @Override
        default void introduceEventDispatcher(String sIdentifier, EventDispatcher dispatcher)
            {
            if (dispatcher instanceof CoherenceDispatcher)
                {
                dispatcher.addEventInterceptor(sIdentifier, this);
                }
            }
        }

    // ----- constants ------------------------------------------------------

    /**
     * The name of the System configuration uri.
     */
    public static final String SYS_CCF_URI = "coherence-system-config.xml";

    /**
     * The System scope name.
     */
    public static final String SYSTEM_SCOPE = "$SYS";

    /**
     * The System session name.
     */
    public static final String SYSTEM_SESSION = SYSTEM_SCOPE;

    /**
     * The default scope name.
     */
    public static final String DEFAULT_SCOPE = "";

    /**
     * The default session name.
     */
    public static final String DEFAULT_NAME = CacheFactoryBuilder.URI_DEFAULT;

    // ----- data members ---------------------------------------------------

    /**
     * A flag indicating whether the static initialisation is complete.
     */
    private static volatile boolean s_fInitialized = false;

    /**
     * The map of all named {@link Coherence} instances.
     */
    private static final CopyOnWriteMap<String, Coherence> s_mapInstance = new CopyOnWriteMap<>(LinkedHashMap.class);

    /**
     * The System session.
     */
    private static ConfigurableCacheFactorySession s_sessionSystem;

    /**
     * The {@link DefaultCacheServer} wrapping the System session.
     */
    private static DefaultCacheServer s_serverSystem;

    /**
     * The {@link SessionConfiguration} for the System session.
     */
    private static SessionConfiguration s_cfgSystem;

    /**
     * The name of this {@link Coherence} instance.
     */
    private final String f_sName;

    /**
     * This session's {@link ResourceRegistry}.
     */
    private final ResourceRegistry f_registry;

    /**
     * The event dispatcher.
     */
    private final CoherenceEventDispatcher f_dispatcher;

    /**
     * The configuration for this {@link Coherence} instance.
     */
    private final CoherenceConfiguration f_config;

    /**
     * The map of named {@link PriorityHolder} instances containing a {@link DefaultCacheServer}
     * instance wrapping a session.
     */
    private final Map<String, PriorityHolder> f_mapServer = new HashMap<>();

    /**
     * The map of named {@link Session} instances.
     */
    private final Map<String, Session> f_mapSession = new CopyOnWriteMap<>(new HashMap<>());

    /**
     * A flag indicating whether this {@link Coherence} instance is started.
     */
    private volatile boolean m_fStarted = false;

    /**
     * A flag indicating whether this {@link Coherence} instance is stopped.
     */
    private volatile boolean m_fClosed = false;

    /**
     * A {@link CompletableFuture} that will be completed when this {@link Coherence}
     * instance has started.
     */
    private final CompletableFuture<Void> f_futureStarted = new CompletableFuture<>();

    /**
     * A {@link CompletableFuture} that will be completed when this {@link Coherence}
     * instance has stopped.
     */
    private final CompletableFuture<Void> f_futureClosed = new CompletableFuture<>();
    }
