/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.coherence.config.Config;
import com.tangosol.internal.health.HealthCheckWrapper;

import com.tangosol.internal.net.ConfigurableCacheFactorySession;
import com.tangosol.internal.net.ScopedUriScopeResolver;
import com.tangosol.internal.net.SystemSessionConfiguration;

import com.tangosol.net.events.CoherenceDispatcher;
import com.tangosol.net.events.CoherenceLifecycleEvent;
import com.tangosol.net.events.EventDispatcher;
import com.tangosol.net.events.EventDispatcherAwareInterceptor;
import com.tangosol.net.events.EventDispatcherRegistry;
import com.tangosol.net.events.EventInterceptor;
import com.tangosol.net.events.InterceptorRegistry;

import com.tangosol.net.events.internal.CoherenceEventDispatcher;
import com.tangosol.net.events.internal.Registry;

import com.tangosol.util.Base;
import com.tangosol.util.CopyOnWriteMap;
import com.tangosol.util.HealthCheck;
import com.tangosol.util.RegistrationBehavior;
import com.tangosol.util.ResourceRegistry;
import com.tangosol.util.SimpleResourceRegistry;

import java.util.ArrayList;
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
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

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
     * be created using the factory methods.
     *
     * @param config  the {@link CoherenceConfiguration} to configure this instance
     * @param mode    the mode that this instance will run in
     *
     * @throws NullPointerException if the config parameter is {@code null}
     */
    private Coherence(CoherenceConfiguration config, Mode mode)
        {
        f_config     = Objects.requireNonNull(config);
        f_mode       = mode;
        f_sName      = config.getName();
        f_registry   = new SimpleResourceRegistry();
        f_dispatcher = new CoherenceEventDispatcher(this);
        f_health     = new HealthCheckWrapper(new CoherenceHealth(), HealthCheckWrapper.SUBTYPE_COHERENCE);

        Registry eventRegistry = new Registry();
        f_registry.registerResource(InterceptorRegistry.class, eventRegistry);
        f_registry.registerResource(EventDispatcherRegistry.class, eventRegistry);
        eventRegistry.registerEventDispatcher(f_dispatcher);
        for (EventInterceptor<?> interceptor : f_config.getInterceptors())
            {
            eventRegistry.registerEventInterceptor(interceptor);
            }

        for (LifecycleListener listener : ServiceLoader.load(LifecycleListener.class))
            {
            eventRegistry.registerEventInterceptor(listener);
            }
        }

    // ----- factory methods ------------------------------------------------

    /**
     * Create a default {@link Coherence} cluster member instance.
     * <p>
     * The {@link Coherence} instance built by the {@code Builder} will be a
     * cluster member.
     *
     * @return a default {@link Coherence} instance
     */
    public static Coherence clusterMember()
        {
        return clusterMemberBuilder(CoherenceConfiguration.create()).build();
        }

    /**
     * Create a {@link Coherence} instance from the specified {@link CoherenceConfiguration}.
     * <p>
     * The {@link Coherence} instance built by the {@code Builder} will be a
     * cluster member.
     *
     * @param config  the configuration to use to create the
     *                {@link Coherence} instance
     *
     * @return a {@link Coherence} instance from the specified {@link CoherenceConfiguration}
     */
    public static Coherence clusterMember(CoherenceConfiguration config)
        {
        return clusterMemberBuilder(config).build();
        }

    /**
     * Create a default {@link Coherence} client instance.
     * <p>
     * If using the default Coherence cache configuration file, this will configure Coherence
     * to be an Extend client.
     * <p>
     * If using the default Coherence Concurrent extensions, this will configure Coherence
     * Concurrent to be an Extend client.
     *
     * @return a default {@link Coherence} instance
     */
    public static Coherence client()
        {
        return clientBuilder(CoherenceConfiguration.create()).build();
        }

    /**
     * Create a default {@link Coherence} client instance.
     * <p>
     * If using the default Coherence cache configuration file, this will configure Coherence
     * to be an Extend client.
     * <p>
     * If using the default Coherence cache configuration file, this will configure Coherence
     * to be an Extend client using a fixed address configured with the parameters
     * {@code coherence.extend.address} and {@code coherence.extend.port}.
     *
     * @return a default {@link Coherence} instance
     */
    public static Coherence fixedClient()
        {
        return fixedClientBuilder(CoherenceConfiguration.create()).build();
        }

    /**
     * Create a client {@link Coherence} instance from the specified {@link CoherenceConfiguration}.
     * <p>
     * If using the default Coherence cache configuration file, this will configure Coherence
     * to be an Extend client.
     * <p>
     * If using the default Coherence Concurrent extensions, this will configure Coherence
     * Concurrent to be an Extend client.
     *
     * @param config  the configuration to use to create the
     *                {@link Coherence} instance
     *
     * @return a {@link Coherence} instance from the specified {@link CoherenceConfiguration}
     */
    public static Coherence client(CoherenceConfiguration config)
        {
        return clientBuilder(config).build();
        }

    /**
     * Create a client {@link Coherence} instance from the specified {@link CoherenceConfiguration}.
     * <p>
     * If using the default Coherence cache configuration file, this will configure Coherence
     * to be an Extend client.
     * <p>
     * If using the default Coherence cache configuration file, this will configure Coherence
     * to be an Extend client using a fixed address configured with the parameters
     * {@code coherence.extend.address} and {@code coherence.extend.port}.
     *
     * @param config  the configuration to use to create the
     *                {@link Coherence} instance
     *
     * @return a {@link Coherence} instance from the specified {@link CoherenceConfiguration}
     */
    public static Coherence fixedClient(CoherenceConfiguration config)
        {
        return fixedClientBuilder(config).build();
        }

    /**
     * Returns a {@link Builder} instance that can build a {@link Coherence}
     * instance using the specified {@link CoherenceConfiguration}.
     * <p>
     * The {@link Coherence} instance built by the {@code Builder} will be a
     * cluster member. Coherence auto-start services will be managed by a
     * {@link DefaultCacheServer} instance for each configured session.
     *
     * @return a {@link Builder} instance that can build a {@link Coherence}
     *         instance using the specified {@link CoherenceConfiguration}
     */
    public static Builder clusterMemberBuilder(CoherenceConfiguration config)
        {
        return new Builder(config, Mode.ClusterMember);
        }

    /**
     * Returns a {@link Builder} instance that can build a {@link Coherence}
     * instance using the specified {@link CoherenceConfiguration}.
     * <p>
     * The {@link Coherence} instance built by the {@code Builder} will be a
     * client, it will not start or join a Coherence cluster.
     * <p>
     * If using the default Coherence cache configuration file, this will configure Coherence
     * to be an Extend client.
     * <p>
     * If using the default Coherence Concurrent extensions, this will configure Coherence
     * Concurrent to be an Extend client.
     *
     * @return a {@link Builder} instance that can build a {@link Coherence}
     *         instance using the specified {@link CoherenceConfiguration}
     */
    public static Builder clientBuilder(CoherenceConfiguration config)
        {
        return new Builder(config, Mode.Client);
        }

    /**
     * Returns a {@link Builder} instance that can build a {@link Coherence}
     * instance using the specified {@link CoherenceConfiguration}.
     * <p>
     * The {@link Coherence} instance built by the {@code Builder} will be a client
     * using a fixed Extend address, it will not start or join a Coherence cluster.
     * <p>
     * If using the default Coherence cache configuration file, this will configure Coherence
     * to be an Extend client using a fixed address configured with the parameters
     * {@code coherence.extend.address} and {@code coherence.extend.port}.
     * <p>
     * If using the default Coherence Concurrent extensions, this will configure Coherence
     * Concurrent to be an Extend client.
     *
     * @return a {@link Builder} instance that can build a {@link Coherence}
     *         instance using the specified {@link CoherenceConfiguration}
     */
    public static Builder fixedClientBuilder(CoherenceConfiguration config)
        {
        return new Builder(config, Mode.ClientFixed);
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
     * Close all {@link Coherence} instances.
     * <b>
     * If starting {@link Coherence} instances ensured the Coherence {@link Cluster}
     * instance then calling this method will also fully shutdown Coherence.
     */
    @SuppressWarnings("OptionalAssignedToNull")
    public static void closeAll()
        {
        Logger.info("Stopping all Coherence instances");
        s_mapInstance.values().forEach(Coherence::close);
        if (s_serverSystem != null)
            {
            s_serverSystem.stop();
            }
        Logger.info("All Coherence instances stopped");
        s_serverSystem  = null;
        s_sessionSystem = null;
        s_fInitialized  = false;
        if (s_fEnsuredCluster != null && s_fEnsuredCluster)
            {
            CacheFactory.shutdown();
            }
        s_fEnsuredCluster = null;
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
     * Return the {@link Mode} that this instance is running as.
     *
     * @return the {@link Mode} that this instance is running as
     */
    public Mode getMode()
        {
        return f_mode;
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
        if (SYSTEM_SESSION.equals(sName))
            {
            return s_sessionSystem != null && s_sessionSystem.isPresent();
            }
        SessionConfiguration configuration = f_config.getSessionConfigurations().get(sName);
        return configuration != null && configuration.isEnabled();
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
            return initializeSystemSession(Collections.emptyList(), f_mode);
            }

        String sSessionName = sName == null ? Coherence.DEFAULT_NAME : sName;

        SessionConfiguration configuration = f_config.getSessionConfigurations().get(sSessionName);
        if (configuration == null || !configuration.isEnabled())
            {
            throw new IllegalArgumentException("No Session has been configured with the name " + sSessionName);
            }

        return f_mapSession.get(sSessionName);
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
    public CompletableFuture<Coherence> whenStarted()
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
     * Asynchronously start this {@link Coherence} instance.
     * <p>
     * If this instance already been started and has not
     * been closed this method call is a no-op.
     *
     * @return a {@link CompletableFuture} that will be completed when
     *         this {@link Coherence} instance has started
     */
    public CompletableFuture<Coherence> start()
        {
        assertNotClosed();
        if (m_fStarted)
            {
            return f_futureStarted;
            }

        synchronized (this)
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
                Runnable runnable = () ->
                    {
                    try
                        {
                        if (f_mapServer.isEmpty())
                            {
                            startInternal();
                            }


                        f_mapServer.values().forEach(holder -> holder.getServer().waitForServiceStart());
                        f_futureStarted.complete(this);
                        f_dispatcher.dispatchStarted();
                        }
                    catch (Throwable thrown)
                        {
                        Logger.err(thrown);
                        f_futureStarted.completeExceptionally(thrown);
                        }
                    };

                Thread t = Base.makeThread(null , runnable,
                                           isDefaultInstance() ? "Coherence" : "Coherence:" + f_sName);
                t.setDaemon(true);
                t.start();
                }
            catch (Throwable thrown)
                {
                f_futureStarted.completeExceptionally(thrown);
                }
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

                // close sessions in reverse order
                f_config.getSessionConfigurations().values()
                        .stream()
                        .sorted(Comparator.reverseOrder())
                        .map(cfg -> f_mapSession.get(cfg.getName()))
                        .filter(Objects::nonNull)
                        .forEach(session -> {
                            try
                                {
                                session.close();
                                }
                            catch(Throwable t)
                                {
                                Logger.err("Error closing session " + session.getName(), t);
                                }
                        });

                // Stop servers in reverse order
                f_mapServer.values()
                        .stream()
                        .sorted(Comparator.reverseOrder())
                        .map(PriorityHolder::getServer)
                        .forEach(this::stopServer);

                f_mapServer.clear();
                }

            getCluster().getManagement().unregister(f_health);

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
     * Returns the current management registry.
     *
     * @return the current management registry or {@code null}
     *         if the management is disabled on this node
     *
     * @since 22.06
     */
    public com.tangosol.net.management.Registry getManagement()
        {
        return getCluster().getManagement();
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
        if (args.length > 0 && args[0].equals("--version"))
            {
            System.out.println(CacheFactory.VERSION);
            if (args.length == 1)
                {
                System.exit(0);
                }
            }

        String sClient = Config.getProperty("coherence.client");
        Coherence coherence;
        if (sClient == null || !sClient.contains("remote"))
            {
            coherence = Coherence.clusterMember();
            }
        else
            {
            coherence = Coherence.client();
            }
        coherence.start();
        // block forever (or until the Coherence instance is shutdown)
        coherence.whenClosed().join();
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Return {@code true} if this is the default instance.
     *
     * @return {@code true} if this is the default instance
     */
    private boolean isDefaultInstance()
        {
        return DEFAULT_NAME.equals(f_sName);
        }

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

        Cluster cluster = CacheFactory.getCluster();
        if (s_fEnsuredCluster == null)
            {
            s_fEnsuredCluster = !cluster.isRunning();
            }

        // ensure the System Session before doing anything else
        // even though there might not actually be a System session
        initializeSystemSession(globalInterceptors, f_mode);

        Logger.info(() -> isDefaultInstance()
                          ? "Starting default Coherence instance"
                          : "Starting Coherence instance " + f_sName);

        cluster.getManagement().register(f_health);

        try
            {
            // Create the sessions in priority order of their configurations
            f_config.getSessionConfigurations()
                    .values()
                    .stream()
                    .sorted()
                    .forEach(configuration -> {
                        if (m_fClosed)
                            {
                            // closed during start-up
                            return;
                            }
                        if (configuration.isEnabled())
                            {
                            String sName = configuration.getName();

                            Iterable<? extends EventInterceptor<?>> interceptors
                                    = join(globalInterceptors, configuration.getInterceptors());

                            Optional<Session> optional = ensureSessionInternal(configuration, f_mode, interceptors);
                            if (optional.isPresent())
                                {
                                Session session = optional.get();
                                // ensure that the session is activated
                                session.activate();
                                f_mapSession.put(sName, session);

                                if (session instanceof ConfigurableCacheFactorySession)
                                    {
                                    ConfigurableCacheFactorySession supplier = (ConfigurableCacheFactorySession) session;
                                    ConfigurableCacheFactory        ccf      = supplier.getConfigurableCacheFactory();

                                    if (f_mode == Mode.ClusterMember)
                                        {
                                        // This is a server and the session is not a client session so wrap in a DCS
                                        // to manage the auto-start services.
                                        DefaultCacheServer dcs = startCCF(sName, ccf);
                                        f_mapServer.put(sName, new PriorityHolder(configuration.getPriority(), dcs));
                                        }
                                    }
                                }
                            else
                                {
                                Logger.warn("Skipping Session " + configuration.getName()
                                        + " Session provider returned null");
                                }
                            }
                    });
            }
        catch (Throwable t)
            {
            Logger.err("Failed to start Coherence instance " + f_sName, t);
            close();
            }

        if (f_mode == Mode.ClusterMember)
            {
            Logger.info(() -> "Started Coherence server " + f_sName
                              + CacheFactory.getCluster().getServiceBanner());
            }
        else
            {
            Logger.info(() -> "Started Coherence client " + f_sName);
            }
        }

    /**
     * Combine two Iterables of interceptors into a single iterable.
     *
     * @param one  the first iterable of interceptors
     * @param two  the first iterable of interceptors
     *
     * @return  the combined iterable of interceptors
     */
    private static Iterable<? extends EventInterceptor<?>> join (Iterable<? extends EventInterceptor<?>> one,
                                                                 Iterable<? extends EventInterceptor<?>> two)
        {
        if (one == null && two == null)
            {
            return Collections.emptyList();
            }
        if (one == null)
            {
            return two;
            }
        if (two == null)
            {
            return one;
            }

        Stream<? extends EventInterceptor<?>>  s1 = StreamSupport.stream(one.spliterator(), false);
        Stream<? extends EventInterceptor<?>>  s2 = StreamSupport.stream(two.spliterator(), false);

        return Stream.concat(s1, s2).collect(Collectors.toCollection(ArrayList::new));
        }

    /**
     * Ensure the specified {@link Session} exists.
     *
     * @param configuration  the {@link SessionConfiguration configuration} for the {@link Session}
     * @param mode           the {@link Mode} the requesting {@link Coherence} instance is running in
     * @param interceptors   optional {@link EventInterceptor interceptors} to add to
     *                       the session in addition to any in the configuration
     *
     * @return the configured and started {@link Session}
     */
    private static Optional<Session> ensureSessionInternal(SessionConfiguration configuration,
            Mode mode, Iterable<? extends EventInterceptor<?>> interceptors)
        {
        return SessionProvider.get().createSession(configuration, mode, interceptors);
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
    private DefaultCacheServer startCCF(String sName, ConfigurableCacheFactory ccf)
        {
        String  sScopeName = ccf.getScopeName();
        boolean fHasScope  = sScopeName != null && !sScopeName.isEmpty();

        Logger.info(() -> (sName == null || sName.isEmpty()
                           ? "Starting default session"
                           : "Starting session " + sName)
                          + (fHasScope ? " with scope name " + sScopeName : ""));
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
     * <p>
     * The System session only applies to certain environments so we may not
     * actually start anything.
     *
     * @param interceptors  any interceptors to add to the system session
     * @param mode          the mode that the Coherence instance is running in
     *
     * @return the singleton system {@link Session}
     */
    @SuppressWarnings("OptionalAssignedToNull")
    private synchronized Session initializeSystemSession(Iterable<? extends EventInterceptor<?>> interceptors, Mode mode)
        {
        if (s_sessionSystem == null)
            {
            // Ensure we are properly initialised
            Coherence.init();

            SessionConfiguration configuration = SystemSessionConfiguration.INSTANCE;
            Iterable<? extends EventInterceptor<?>> allInterceptors
                    = join(interceptors, configuration.getInterceptors());

            Optional<Session> optional = ensureSessionInternal(configuration, mode, allInterceptors);
            if (optional.isPresent())
                {
                Session session = optional.get();
                if (session instanceof ConfigurableCacheFactorySession)
                    {
                    ConfigurableCacheFactory ccfSystem = ((ConfigurableCacheFactorySession) session)
                            .getConfigurableCacheFactory();
                    s_serverSystem = startCCF(SYSTEM_SCOPE, ccfSystem);
                    }
                }

            registerHealthChecks();

            s_sessionSystem = optional;
            }
        else
            {
            s_sessionSystem.ifPresent(session -> registerInterceptors(session, interceptors));
            }
        return s_sessionSystem.orElse(null);
        }

    /**
     * Visible for testing only, set the System {@link Session}.
     *
     * @param optional an optional containing the System {@link Session}
     */
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    static void setSystemSession(Optional<Session> optional)
        {
        s_sessionSystem = Objects.requireNonNull(optional);
        }

    /**
     * Discover and register any {@link HealthCheck} instances using the {@link ServiceLoader}.
     */
    private void registerHealthChecks()
        {
        ServiceLoader<HealthCheck>           loader    = ServiceLoader.load(HealthCheck.class);
        com.tangosol.net.management.Registry registry = getCluster().getManagement();
        for (HealthCheck healthCheck : loader)
            {
            Logger.info("Registering discovered HealthCheck: " + healthCheck.getName());
            registry.register(healthCheck);
            }
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
        private Builder(CoherenceConfiguration config, Mode mode)
            {
            f_config = config;
            f_mode = mode;
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

            Coherence coherence = new Coherence(f_config, f_mode);
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

        /**
         * The mode that the {@link Coherence} instance should run as.
         */
        private final Mode f_mode;
        }

    // ----- inner enum Type ------------------------------------------------

    /**
     * An enum representing the different modes that a {@link Coherence}
     * instance can run in.
     */
    public enum Mode
        {
        /**
         * The {@link Coherence} instance should run as a non-cluster member client.
         * Typically, this would be something like an Extend or gRPC client.
         * For an Extend client, the proxy will be discovered using the name service.
         */
        Client("remote"),
        /**
         * The {@link Coherence} instance should run as a non-cluster member client.
         * Typically, this would be something like an Extend or gRPC client.
         * For an Extend client, the proxy be configured with a fixed address and port.
         */
        ClientFixed("remote-fixed"),
        /**
         * The {@link Coherence} instance should run as a cluster member client.
         */
        ClusterMember("direct");

        Mode(String sClient)
            {
            f_sClient = sClient;
            }

        /**
         * Returns the default {@code coherence.client} property.
         *
         * @return the default {@code coherence.client} property
         */
        public String getClient()
            {
            return f_sClient;
            }

        /**
         * The default {@code coherence.client} property for this mode.
         */
        private String f_sClient;
        }

    // ----- inner interface LifecycleListener ------------------------------

    /**
     * An interface implemented by listeners of {@link CoherenceLifecycleEvent CoherenceLifecycleEvents}.
     * <p>
     * Implementations of this interface properly registered as services will be discovered
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

    // ----- Health ---------------------------------------------------------

    /**
     * The Coherence instance's health check.
     */
    private class CoherenceHealth
            implements HealthCheck
        {
        @Override
        public String getName()
            {
            if (Coherence.this.isDefaultInstance())
                {
                return "Default";
                }
            return Coherence.this.getName();
            }

        @Override
        public boolean isMemberHealthCheck()
            {
            return true;
            }

        @Override
        public boolean isReady()
            {
            return Coherence.this.isStarted();
            }

        @Override
        public boolean isLive()
            {
            return Coherence.this.isStarted();
            }

        @Override
        public boolean isStarted()
            {
            return Coherence.this.isStarted();
            }

        @Override
        public boolean isSafe()
            {
            return Coherence.this.isStarted();
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
    public static final String DEFAULT_NAME = "";

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
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private static Optional<Session> s_sessionSystem;

    /**
     * The {@link DefaultCacheServer} wrapping the System session.
     */
    private static DefaultCacheServer s_serverSystem;

    /**
     * A flag indicating whether the bootstrap API ensured the Coherence cluster service.
     */
    private static Boolean s_fEnsuredCluster = null;

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
     * This {@link Coherence} instance {@link CoherenceHealth health check MBean}.
     */
    private final HealthCheckWrapper f_health;

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
    private final CompletableFuture<Coherence> f_futureStarted = new CompletableFuture<>();

    /**
     * A {@link CompletableFuture} that will be completed when this {@link Coherence}
     * instance has stopped.
     */
    private final CompletableFuture<Void> f_futureClosed = new CompletableFuture<>();

    /**
     * The {@link Mode} that the {@link Coherence} instance will run in.
     */
    private final Mode f_mode;
    }
