/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net;

import com.oracle.coherence.common.base.Classes;
import com.oracle.coherence.common.base.Logger;
import com.oracle.coherence.common.base.Timeout;

import com.oracle.coherence.common.collections.ConcurrentHashMap;

import com.oracle.coherence.common.util.Duration;

import com.tangosol.application.ContainerContext;
import com.tangosol.application.Context;

import com.tangosol.coherence.config.Config;
import com.tangosol.coherence.config.scheme.ServiceScheme;

import com.tangosol.internal.health.HealthCheckWrapper;

import com.tangosol.internal.net.ConfigurableCacheFactorySession;
import com.tangosol.internal.net.SystemSessionConfiguration;

import com.tangosol.internal.net.metrics.MetricsHttpHelper;
import com.tangosol.net.events.CoherenceDispatcher;
import com.tangosol.net.events.CoherenceLifecycleEvent;
import com.tangosol.net.events.EventDispatcher;
import com.tangosol.net.events.EventDispatcherAwareInterceptor;
import com.tangosol.net.events.EventDispatcherRegistry;
import com.tangosol.net.events.EventInterceptor;
import com.tangosol.net.events.InterceptorRegistry;

import com.tangosol.net.events.internal.CoherenceEventDispatcher;
import com.tangosol.net.events.internal.Registry;

import com.tangosol.run.xml.XmlHelper;
import com.tangosol.util.Base;
import com.tangosol.util.CopyOnWriteMap;
import com.tangosol.util.HealthCheck;
import com.tangosol.util.RegistrationBehavior;
import com.tangosol.util.ResourceRegistry;
import com.tangosol.util.SimpleResourceRegistry;

import java.io.File;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;

import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.CompletableFuture;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.regex.Pattern;
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
     * Return the default {@link Coherence} instance, creating it if it does not already exist.
     *
     * @return a default {@link Coherence} instance
     */
    public static Coherence create()
        {
        return builder(CoherenceConfiguration.create(), Mode.ClusterMember)
                .build(true);
        }

    /**
     * Create a default {@link Coherence} instance.
     *
     * @param config  the configuration to use to create the
     *                {@link Coherence} instance
     *
     * @return a default {@link Coherence} instance
     */
    public static Coherence create(CoherenceConfiguration config)
        {
        return create(config, Mode.ClusterMember);
        }

    /**
     * Create a default {@link Coherence} instance.
     *
     * @param config  the configuration to use to create the
     *                {@link Coherence} instance
     * @param mode    the {@link Mode} the {@link Coherence} instance will run in
     *
     * @return a default {@link Coherence} instance
     */
    public static Coherence create(CoherenceConfiguration config, Mode mode)
        {
        return create(config, mode, null);
        }

    /**
     * Create a default {@link Coherence} instance.
     *
     * @param config  the configuration to use to create the
     *                {@link Coherence} instance
     * @param mode    the {@link Mode} the {@link Coherence} instance will run in
     * @param loader  the {@link ClassLoader} associated with the {@link Coherence} instance
     *
     * @return a default {@link Coherence} instance
     */
    public static Coherence create(CoherenceConfiguration config, Mode mode, ClassLoader loader)
        {
        return builder(config, mode).build(loader);
        }

    /**
     * Returns a {@link Builder} instance that can build a {@link Coherence}
     * instance using the specified {@link CoherenceConfiguration}.
     * <p>
     * The {@link Coherence} instance built by the {@code Builder} will be a
     * cluster member. Coherence auto-start services will be managed by a
     * {@link DefaultCacheServer} instance for each configured session.
     *
     * @param config the {@link CoherenceConfiguration} to use to build the {@link Coherence} instance
     *
     * @return a {@link Builder} instance that can build a {@link Coherence}
     *         instance using the specified {@link CoherenceConfiguration}
     */
    public static Builder builder(CoherenceConfiguration config)
        {
        return clusterMemberBuilder(config);
        }

    /**
     * Returns a {@link Builder} instance that can build a {@link Coherence}
     * instance using the specified {@link CoherenceConfiguration}.
     * <p>
     * The {@link Coherence} instance built by the {@code Builder} will be a
     * cluster member. Coherence auto-start services will be managed by a
     * {@link DefaultCacheServer} instance for each configured session.
     *
     * @param config the {@link CoherenceConfiguration} to use to build the {@link Coherence} instance
     * @param mode   the {@link Mode} the {@link Coherence} instance will run in
     *
     * @return a {@link Builder} instance that can build a {@link Coherence}
     *         instance using the specified {@link CoherenceConfiguration}
     */
    public static Builder builder(CoherenceConfiguration config, Mode mode)
        {
        return new Builder(config, mode);
        }

    /**
     * Return the default {@link Coherence} cluster member instance, creating it if it does not already exist.
     * <p>
     * The {@link Coherence} instance built by the {@code Builder} will be a
     * cluster member.
     *
     * @return a default {@link Coherence} instance
     */
    public static Coherence clusterMember()
        {
        return clusterMemberBuilder(CoherenceConfiguration.create()).build(true);
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
     * Return the default {@link Coherence} client instance, creating it if it does not already exist.
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
        return clientBuilder(CoherenceConfiguration.create()).build(true);
        }

    /**
     * Return the default {@link Coherence} client instance, creating it if it does not already exist.
     * <p>
     * If using the default Coherence cache configuration file, this will configure Coherence
     * to be an Extend client.
     * <p>
     * If using the default Coherence Concurrent extensions, this will configure Coherence
     * Concurrent to be an Extend client.
     *
     * @param mode  the default mode to run the Coherence instance
     *
     * @return a default {@link Coherence} instance
     */
    public static Coherence client(Mode mode)
        {
        return clientBuilder(CoherenceConfiguration.create(), mode).build(true);
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
     * Create a default {@link Coherence} client instance, creating it if it does not already exist.
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
        return fixedClientBuilder(CoherenceConfiguration.create()).build(true);
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
     * @param config  the {@link CoherenceConfiguration} to configure the {@link Coherence} instance
     *
     * @return a {@link Builder} instance that can build a {@link Coherence}
     *         instance using the specified {@link CoherenceConfiguration}
     */
    public static Builder clientBuilder(CoherenceConfiguration config)
        {
        Mode   mode    = Mode.Client;
        String sClient = Config.getProperty("coherence.client");
        try
            {
            mode = Mode.fromClientName(sClient);
            }
        catch (Exception e)
            {
            // ignored, just use Mode.Client
            }
        return clientBuilder(config, mode);
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
     * @param config  the {@link CoherenceConfiguration} to configure the {@link Coherence} instance
     * @param mode    the default mode to run the Coherence instance
     *
     * @return a {@link Builder} instance that can build a {@link Coherence}
     *         instance using the specified {@link CoherenceConfiguration}
     */
    public static Builder clientBuilder(CoherenceConfiguration config, Mode mode)
        {
        return new Builder(config, mode);
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
        return getInstances(Classes.ensureClassLoader(null));
        }

    /**
     * Returns all of the {@link Coherence} instances.
     *
     * @param loader  the {@link ClassLoader} to obtain instances
     *
     * @return all of the {@link Coherence} instances
     */
    public static Collection<Coherence> getInstances(ClassLoader loader)
        {
        Map<String, Coherence> map = getInstanceMap(loader);
        return map == null ? Collections.emptyList()
                : Collections.unmodifiableCollection(new ArrayList<>(map.values()));
        }

    private static Map<String, Coherence> getInstanceMap()
        {
        return getInstanceMap(null);
        }

    private static Map<String, Coherence> getInstanceMap(ClassLoader loader)
        {
        ClassLoader loaderSearch = Classes.ensureClassLoader(loader);

        // most likely code path: retrieve existing factory from provided
        // ClassLoader or its parents; create if it doesn't exist

        // Note: Returning a map associated with the parent's class loader
        // may disallow loading classes bound to the given class loader;
        // however this constraint is introduced to accommodate for the EAR /
        // WAR / GAR use case
        Map<ClassLoader, Map<String, Coherence>> map = s_mapByLoader;
        Map<String, Coherence> mapInstances;
        do
            {
            mapInstances = map.get(loaderSearch);
            }
        while (mapInstances == null && (loaderSearch = loaderSearch.getParent()) != null);

        if (mapInstances == null)
            {
            // last resort - try finding the map using the ClassLoader of the Coherence class
            mapInstances = map.get(Coherence.class.getClassLoader());
            }

        return mapInstances;
        }

    private static Map<String, Coherence> ensureInstanceMap(ClassLoader loader)
        {
        loader = Classes.ensureClassLoader(loader);
        Map<String, Coherence> mapInstance = getInstanceMap(loader);
        if (mapInstance == null)
            {
            s_instanceLock.lock();
            try
                {
                mapInstance = getInstanceMap(loader);
                if (mapInstance == null)
                    {
                    mapInstance = s_mapByLoader.computeIfAbsent(loader, l -> new LinkedHashMap<>());
                    }
                }
            finally
                {
                s_instanceLock.unlock();
                }
            }
        return mapInstance;
        }

    protected static void removeInstance(String sName)
        {
        removeInstance(sName, null);
        }

    protected static void removeInstance(String sName, ClassLoader loader)
        {
        s_instanceLock.lock();
        try
            {
            ClassLoader loaderSearch = Classes.ensureClassLoader(loader);

            // most likely code path: retrieve existing factory from provided
            // ClassLoader or its parents; create if it doesn't exist

            // Note: Returning a map associated with the parent's class loader
            // may disallow loading classes bound to the given class loader;
            // however this constraint is introduced to accommodate for the EAR /
            // WAR / GAR use case
            Map<ClassLoader, Map<String, Coherence>> map = s_mapByLoader;
            Map<String, Coherence> mapInstance;
            do
                {
                mapInstance = map.get(loaderSearch);
                }
            while (mapInstance == null && (loaderSearch = loaderSearch.getParent()) != null);

            if (mapInstance != null)
                {
                mapInstance.remove(sName);
                if (mapInstance.isEmpty())
                    {
                    map.remove(loaderSearch);
                    }
                }
            }
        finally
            {
            s_instanceLock.unlock();
            }
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
        return getInstance(sName, null);
        }

    /**
     * Returns the named {@link Coherence} instance or {@code null}
     * if the name is {@code null} or no {@link Coherence} instance exists
     * with the specified name.
     *
     * @param sName   the name of the {@link Coherence} instance to return
     * @param loader  the {@link ClassLoader} associated with the {@link Coherence} instance
     *
     * @return the named {@link Coherence} instance or {@code null} if no
     *         {@link Coherence} instance exists with the specified name
     */
    public static Coherence getInstance(String sName, ClassLoader loader)
        {
        Map<String, Coherence> map = getInstanceMap(loader);
        return sName == null || map == null ? null : map.get(sName);
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
        return getInstance((ClassLoader) null);
        }

    /**
     * Returns a {@link Coherence} instance created or {@code null}
     * if no {@link Coherence} instance exists.
     * <p>
     * This method is useful if only a single {@link Coherence} instance
     * exists in an application. If multiple instances exists the actual
     * instance returned is undetermined.
     *
     * @param loader  the {@link ClassLoader} associated with the {@link Coherence} instance
     *
     * @return a {@link Coherence} instance created
     */
    public static Coherence getInstance(ClassLoader loader)
        {
        Map<String, Coherence> map = getInstanceMap(loader);
        if (map == null)
            {
            return null;
            }

        Coherence coherence = map.get(Coherence.DEFAULT_NAME);
        if (coherence != null)
            {
            return coherence;
            }
        return map.entrySet()
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
        Map<String, Coherence> map = getInstanceMap();
        if (map != null)
            {
            for (Coherence coherence : map.values())
                {
                if (coherence.hasSession(sName))
                    {
                    Session session = coherence.getSession(sName);
                    if (session != null)
                        {
                        return Optional.of(session);
                        }
                    }
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
        Collection<Session> col = null;
        if (Coherence.SYSTEM_SCOPE.equals(sScope))
            {
            col = s_systemSession.map(Session.class::cast)
                    .map(Collections::singleton).orElse(null);
            }

        if (col == null)
            {
            Map<String, Coherence> map = getInstanceMap();
            if (map != null)
                {
                col = map.values()
                        .stream()
                        .flatMap(coh -> coh.getSessionsWithScope(sScope).stream())
                        .collect(Collectors.toList());
                }
            else
                {
                col = Collections.emptyList();
                }
            }
        return col;
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
        Map<String, Coherence> map = getInstanceMap();
        if (map != null)
            {
            List<Coherence> list = new ArrayList<>(map.values());
            list.forEach(Coherence::close);
            list.clear();
            }

        if (s_systemServiceMonitor != null)
            {
            s_systemServiceMonitor.close();
            }
        if (s_systemSession != null && s_systemSession.isPresent())
            {
            ConfigurableCacheFactorySession session = s_systemSession.get();
            CacheFactoryBuilder             builder = CacheFactory.getCacheFactoryBuilder();
            ConfigurableCacheFactory        ccf     = session.getConfigurableCacheFactory();

            try
                {
                session.close();
                }
            catch (Exception e)
                {
                Logger.err(e);
                }
            finally
                {
                builder.release(ccf);
                ccf.dispose();
                }
            }
        Logger.info("All Coherence instances stopped");
        s_systemServiceMonitor = null;
        s_systemSession        = null;
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
            return m_localSystemSession != null && m_localSystemSession.isPresent();
            }
        SessionConfiguration configuration = getSessionConfiguration(sName);
        return configuration != null && configuration.isEnabled();
        }

    /**
     * Return a set of session names that this {@code Coherence}
     * instance has.
     *
     * @return a set of session names that this {@code Coherence}
     *         instance has
     */
    public Set<String> getSessionNames()
        {
        return Collections.unmodifiableSet(f_mapSession.keySet());
        }

    /**
     * Return a set of session scope names that this {@code Coherence}
     * instance has.
     *
     * @return a set of session scope names that this {@code Coherence}
     *         instance has
     */
    public Set<String> getSessionScopeNames()
        {
        return f_mapSession.values()
                .stream()
                .map(Session::getScopeName)
                .collect(Collectors.toSet());
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
     * @return an {@link Optional} containing the named {@link Session} or an
     *         empty {@link Optional} if this {@link Coherence} instance does
     *         not contain the named session
     *
     * @throws IllegalStateException  if this instance has been closed
     */
    public Optional<Session> getSessionIfPresent(String sName)
        {
        assertNotClosed();
        return getSessionInternal(sName);
        }

    /**
     * Obtain the {@link Session} from the {@link Coherence} instance that was
     * configured with the specified configuration name.
     *
     * @param sName  the name of the {@link Session} to return
     *
     * @return the named {@link Session}
     *
     * @throws IllegalArgumentException if no session exists for the specified name
     * @throws IllegalStateException    if this instance has been closed
     */
    public Session getSession(String sName)
        {
        assertNotClosed();

        String sSessionName = sName == null ? Coherence.DEFAULT_NAME : sName;

        return getSessionInternal(sName)
                .orElseThrow(() -> new IllegalArgumentException("No Session has been configured with the name " + sSessionName));
        }

    /**
     * Add a {@link SessionConfiguration session} to this {@link Coherence} instance.
     * <p>
     * The {@link SessionConfiguration#getName() session name} must be globally unique across
     * all {@link Coherence} instances.
     * <p>
     * If this {@link Coherence} instance is already running, then the session will be started
     * immediately.
     *
     * @param config  the {@link SessionConfiguration configuration} of the session to add
     *
     * @return this {@link Coherence} instance
     *
     * @throws IllegalArgumentException  if the configuration does not have a name
     * @throws IllegalStateException     if this {@link Coherence} instance is closed
     */
    public synchronized Coherence addSession(SessionConfiguration config)
        {
        assertNotClosed();

        String sName = config.getName();
        if (hasSession(sName))
            {
            throw new IllegalStateException("A Session with the name '" + sName
                    + "' already exists in this Coherence instance '" + getName() + "'");
            }

        return addSessionInternal(config);
        }

    /**
     * Add a {@link SessionConfiguration session} to this {@link Coherence} instance, iff a
     * session configuration is not already present in this {@link Coherence} instance with
     * the specified name.
     * <p>
     * The {@link SessionConfiguration#getName() session name} must be globally unique across
     * all {@link Coherence} instances.
     * <p>
     * If this {@link Coherence} instance is already running, then the session will be started
     * immediately.
     *
     * @param config  the {@link SessionConfiguration configuration} of the session to add
     *
     * @return this {@link Coherence} instance
     *
     * @throws IllegalArgumentException  if the configuration does not have a name
     * @throws IllegalStateException     if this {@link Coherence} instance is closed
     */
    public Coherence addSessionIfAbsent(SessionConfiguration config)
        {
        return addSessionIfAbsent(config.getName(), () -> config);
        }

    /**
     * Add a {@link SessionConfiguration session} to this {@link Coherence} instance, iff a
     * session configuration is not already present in this {@link Coherence} instance with
     * the specified name.
     * <p>
     * The {@link SessionConfiguration#getName() session name} must be globally unique across
     * all {@link Coherence} instances.
     * <p>
     * If this {@link Coherence} instance is already running, then the session will be started
     * immediately.
     *
     * @param sName     the name of the session to add
     * @param supplier  a {@link Supplier} used to provide the {@link SessionConfiguration configuration}
     *                  of the session to add, if the session is not present
     *
     * @return this {@link Coherence} instance
     *
     * @throws IllegalArgumentException  if the configuration name does not match the name argument
     * @throws IllegalArgumentException  if the configuration does not have a name
     * @throws IllegalStateException     if this {@link Coherence} instance is closed
     */
    public synchronized Coherence addSessionIfAbsent(String sName, Supplier<SessionConfiguration> supplier)
        {
        assertNotClosed();

        if (hasSession(sName))
            {
            return this;
            }
        SessionConfiguration config = supplier.get();
        if (!Objects.equals(sName, config.getName()))
            {
            throw new IllegalArgumentException("The configuration name '" + config.getName()
                + "' does not match the name argument '" + sName + "'");
            }
        return addSessionInternal(config);
        }

    private Coherence addSessionInternal(SessionConfiguration config)
        {
        validate(config);
        f_mapAdditionalSessionConfig.put(config.getName(), config);
        if (isStarted())
            {
            // This Coherence instance is already started so start the session
            Iterable<EventInterceptor<?>> globalInterceptors = f_config.getInterceptors();
            startSession(config, globalInterceptors);
            }

        return this;
        }

    /**
     * Obtain all the {@link Session Sessions} from the {@link Coherence} instance
     * that are configured with the specified scope name.
     *
     * @param sScope  the scope name of the {@link Session Sessions} to return
     *
     * @return the {@link Session} instances with the specified scope or
     *         an empty {@link Collection} if no {@link Session Sessions}
     *         have the required scope name
     *
     * @throws IllegalStateException  if this instance has been closed
     */
    public Collection<Session> getSessionsWithScope(String sScope)
        {
        assertNotClosed();

        String sScopeName;
        if (sScope == null || Coherence.DEFAULT_SCOPE.equals(sScope))
            {
            sScopeName = f_config.getApplicationContext()
                    .map(Context::getDefaultScope)
                    .orElse(Coherence.DEFAULT_SCOPE);
            }
        else
            {
            sScopeName = sScope;
            }

        if (m_localSystemSession.isPresent())
            {
            Session session = m_localSystemSession.get();
            if (Coherence.SYSTEM_SCOPE.equals(sScope) || session.getScopeName().equals(sScope))
                {
                return Collections.singletonList(session);
                }
            }

        return getSessionConfigurations()
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
     * Start this {@link Coherence} instance and block until Coherence has started.
     * <p>
     * This method will wait for the specified timeout.
     * <p>
     * If this instance already been started and has not
     * been closed this method call is a no-op.
     *
     * @return the running {@link Coherence} instance
     *
     * @throws InterruptedException if Coherence does not start within the timeout
     */
    @SuppressWarnings("unused")
    public Coherence startAndWait() throws InterruptedException
        {
        return startAndWait(DEFAULT_START_TIMEOUT);
        }

    /**
     * Start this {@link Coherence} instance and block until Coherence has started.
     * <p>
     * This method will wait for the specified timeout.
     * <p>
     * If this instance already been started and has not
     * been closed this method call is a no-op.
     *
     * @param timeout  the timeout duration to wait for Coherence to start
     *
     * @return the running {@link Coherence} instance
     *
     * @throws InterruptedException if Coherence does not start within the timeout
     */
    public Coherence startAndWait(Duration timeout) throws InterruptedException
        {
        long cMillis = timeout == null
                ? DEFAULT_START_TIMEOUT.as(Duration.Magnitude.MILLI)
                : Math.max(1, timeout.as(Duration.Magnitude.MILLI));
        try (Timeout ignored = Timeout.after(cMillis))
            {
            start(true);
            }
        return this;
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
        return start(false);
        }

    /**
     * Start this {@link Coherence} instance.
     * <p>
     * If this instance already been started and has not
     * been closed this method call is a no-op.
     */
    public void startOnCallingThread()
        {
        start(true);
        }

    /**
     * Start this {@link Coherence} instance.
     * <p>
     * If this instance already been started and has not
     * been closed this method call is a no-op.
     *
     * @param fRunOnCallingThread  {@code true} to start the Coherence instance synchronously
     *                             on the calling thread, or {@code false} to start the
     *                             Coherence instance asynchronously
     *
     * @return a {@link CompletableFuture} that will be completed when
     *         this {@link Coherence} instance has started
     */
    private CompletableFuture<Coherence> start(boolean fRunOnCallingThread)
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

            try
                {
                Runnable runnable = () ->
                    {
                    ContainerContext context = f_config.getApplicationContext()
                            .map(Context::getContainerContext)
                            .orElse(null);

                    if (context != null)
                        {
                        ContainerContext contextCurrent = context.getCurrentThreadContext();
                        if (contextCurrent == null ||
                           !contextCurrent.getDomainPartition().equals(context.getDomainPartition()))
                            {
                            throw new IllegalStateException("start() called out of context");
                            }

                        if (!context.isGlobalDomainPartition())
                            {
                            context = context.getGlobalContext();
                            context.setCurrentThreadContext();
                            }
                        else
                            {
                            // null context indicates we don't need to reset the thread context
                            context = null;
                            }
                        }

                    try
                        {
                        if (f_mapServer.isEmpty())
                            {
                            startInternal();
                            }

                        f_mapServer.values().forEach(holder -> holder.getServer().waitForServiceStart());
                        m_fStarted = true;
                        f_futureStarted.complete(this);
                        f_dispatcher.dispatchStarted();
                        }
                    catch (Throwable thrown)
                        {
                        Logger.err(thrown);
                        f_futureStarted.completeExceptionally(thrown);
                        }
                    finally
                        {
                        if (context != null)
                            {
                            context.resetCurrentThreadContext();
                            }
                        }
                    };

                if (fRunOnCallingThread)
                    {
                    runnable.run();
                    }
                else
                    {
                    Thread t = Base.makeThread(null , runnable,
                            isDefaultInstance() ? "Coherence" : "Coherence:" + f_sName);
                    t.setDaemon(true);
                    t.start();
                    }
                }
            catch (Throwable thrown)
                {
                f_futureStarted.completeExceptionally(thrown);
                }
            }

        return f_futureStarted;
        }

    public boolean isActive()
        {
        return m_fStarted && !m_fClosed;
        }

    /**
     * Close this {@link Coherence} instance.
     */
    @SuppressWarnings({"OptionalAssignedToNull"})
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
            removeInstance(f_sName);
            if (m_fStarted)
                {
                m_fStarted = false;

                // Stop servers in reverse order
                f_mapServer.values()
                        .stream()
                        .sorted(Comparator.reverseOrder())
                        .map(PriorityHolder::getServer)
                        .forEach(this::stopServer);

                f_mapServer.clear();

                // close sessions in reverse order
                getSessionConfigurations()
                        .sorted(Comparator.reverseOrder())
                        .forEach(cfg ->
                            {
                            Session session = f_mapSession.get(cfg.getName());
                            if (session != null)
                                {
                                try
                                    {
                                    session.close();
                                    if (isNotGarSession(cfg)) // we do not close the default GAR session
                                        {
                                        cfg.sessionProvider().ifPresent(p -> p.releaseSession(session));
                                        if (session instanceof ConfigurableCacheFactorySession)
                                            {
                                            ConfigurableCacheFactory ccf = ((ConfigurableCacheFactorySession) session)
                                                    .getConfigurableCacheFactory();
                                            if (ccf.isActive())
                                                {
                                                ccf.dispose();
                                                }
                                            }
                                        }
                                    }
                                catch(Throwable t)
                                    {
                                    Logger.err("Error closing session " + session.getName(), t);
                                    }
                                }
                        });

                if (f_mode == Mode.Gar)
                    {
                    m_localSystemServiceMonitor.close();
                    if (m_localSystemSession.isPresent())
                        {
                        try
                            {
                            ConfigurableCacheFactorySession session = m_localSystemSession.get();
                            session.getConfigurableCacheFactory().dispose();
                            session.close();
                            SessionProvider provider = m_localSystemSessionConfig.sessionProvider()
                                    .orElseGet(SessionProvider::get);
                            provider.releaseSession(session);
                            }
                        catch (Exception e)
                            {
                            Logger.err(e);
                            }
                        }
                    }
                m_localSystemSession        = null;
                m_localSystemServiceMonitor = null;
                }

            getCluster().getManagement().unregister(f_health);

            f_futureClosed.complete(null);
            }
        catch (Throwable thrown)
            {
            Logger.err(thrown);
            f_futureClosed.completeExceptionally(thrown);
            }

        stopMetrics();

        f_dispatcher.dispatchStopped();
        f_registry.dispose();
        }

    private boolean isNotGarSession(SessionConfiguration configuration)
        {
        return f_mode != Mode.Gar || !configuration.getName().equals(f_sName);
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
        boolean fShowVersion = false;
        File    fileGar      = null;
        String  sAppName     = null;
        long    cWaitMillis  = DefaultCacheServer.DEFAULT_WAIT_MILLIS;

        for (String sArg : args)
            {
            if ("--version".equals(sArg))
                {
                fShowVersion = true;
                }
            else if (sArg.endsWith(".xml"))
                {
                CacheFactory.getCacheFactoryBuilder().setCacheConfiguration(null,
                    XmlHelper.loadFileOrResource(sArg, "cache configuration", null));
                }
            else if (sArg.endsWith(".gar") || sArg.contains(File.separator) || ".".equals(sArg))
                {
                fileGar = new File(sArg);
                }
            else if (Pattern.matches("[0-9]*", sArg))
                {
                // numeric arguments are ignored, this is here to be compatible with DCS.main()
                }
            else
                {
                sAppName = sArg;
                }
            }

        if (fShowVersion)
            {
            System.out.println(CacheFactory.VERSION);
            if (args.length == 1)
                {
                System.exit(0);
                }
            }

        String sClient = Config.getProperty("coherence.client");
        Mode   mode    = null;

        try
            {
            mode = Mode.fromClientName(sClient);
            }
        catch (IllegalArgumentException e)
            {
            // ignored
            }

        Coherence coherence;

        if (mode == null)
            {
            coherence = Coherence.clusterMember();
            }
        else
            {
            coherence = Coherence.builder(CoherenceConfiguration.create(), mode).build();
            }

        coherence.start();

        // block forever (or until the Coherence instance is shutdown)
        coherence.whenClosed().join();
        }

    // ----- Object methods -------------------------------------------------

    @Override
    public String toString()
        {
        return "Coherence{" +
                "name='" + f_sName + '\'' +
                ", mode='" + f_mode + '\'' +
                ", started='" + m_fStarted + '\'' +
                ", closed='" + m_fClosed + '\'' +
                ", sessions=[" + getSessionConfigurations()
                    .map(s -> "{name='" + s.getName() + "', scope='" + s.getScopeName() + "'}")
                    .collect(Collectors.joining(",")) + "]" +
                '}';
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
     * Obtain the {@link Session} from the {@link Coherence} instance that was
     * configured with the specified configuration name.
     *
     * @param sName  the name of the {@link Session} to return
     *
     * @return an {@link Optional} containing the named {@link Session} or an
     *         empty {@link Optional} if this {@link Coherence} instance does
     *         not contain the named session
     */
    private Optional<Session> getSessionInternal(String sName)
        {
        if (Coherence.SYSTEM_SESSION.equals(sName))
            {
            return Optional.ofNullable(initializeSystemSession(Collections.emptyList()));
            }

        String sSessionName = sName == null ? Coherence.DEFAULT_NAME : sName;
        if (Coherence.DEFAULT_NAME.equals(sSessionName))
            {
            sSessionName = f_config.getDefaultSessionName();
            }

        SessionConfiguration configuration = getSessionConfiguration(sSessionName);
        if (configuration == null || !configuration.isEnabled())
            {
            return Optional.empty();
            }

        return Optional.ofNullable(f_mapSession.get(sSessionName));
        }

    /**
     * Start this {@link Coherence} instance.
     */
    @SuppressWarnings("resource")
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
        initializeSystemSession(globalInterceptors);

        Logger.info(() -> isDefaultInstance()
                          ? "Starting default Coherence instance" + " mode=" + f_mode
                          : "Starting Coherence instance " + f_sName + " mode=" + f_mode);

        cluster.getManagement().register(f_health);

        try
            {
            // Create the sessions in priority order of their configurations
            getSessionConfigurations()
                    .sorted(Comparator.reverseOrder())
                    .forEach(configuration -> startSession(configuration, globalInterceptors));
            }
        catch (Throwable t)
            {
            Logger.err("Failed to start Coherence instance " + f_sName + " mode=" + f_mode, t);
            close();
            }

        if (f_mode.isClusterMember())
            {
            Logger.info(() -> "Started Coherence server " + f_sName  + " mode=" + f_mode
                              + CacheFactory.getCluster().getServiceBanner());
            }
        else
            {
            ensureMetrics();
            Logger.info(() -> "Started Coherence client " + f_sName + " mode=" + f_mode);
            }
        }

    /**
     * Start the specified {@link SessionConfiguration session}.
     *
     * @param configuration       the {@link SessionConfiguration configuration} of the session to start
     * @param globalInterceptors  the {@link EventInterceptor interceptors} to add to the session
     */
    private void startSession(SessionConfiguration configuration, Iterable<EventInterceptor<?>> globalInterceptors)
        {
        if (m_fClosed)
            {
            // closed during start-up
            return;
            }

        if (configuration.isEnabled())
            {
            String sName = configuration.getName();
            Mode   mode  = configuration.getMode().orElse(f_mode);

            if (Coherence.DEFAULT_NAME.equals(sName))
                {
                Logger.info("Starting default Session mode=" + mode);
                }
            else
                {
                Logger.info("Starting Session \"" + sName + "\" mode=" + mode);
                }

            Iterable<? extends EventInterceptor<?>> interceptors
                    = join(globalInterceptors, configuration.getInterceptors());

            Optional<Session> optional = ensureSessionInternal(configuration, f_mode, getScopePrefix(), interceptors);
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

                    if (mode.isClusterMember())
                        {
                        if (ccf.isActive())
                            {
                            // The CCF is already active (e.g. we're the default CCF for a GAR)
                            // so we just report the services, we do not need to start or monitor them
                            DefaultCacheServer dcs = new DefaultCacheServer(ccf);
                            dcs.reportStarted(((ExtensibleConfigurableCacheFactory) ccf).getServiceMap().keySet());
                            }
                        else
                            {
                            // This is a server and the session is not a client session so wrap in a DCS
                            // to manage the auto-start services.
                            ccf.activate();
                            DefaultCacheServer dcs = startCCF(sName, ccf);
                            f_mapServer.put(sName, new PriorityHolder(configuration.getPriority(), dcs));
                            }
                        }
                    }
                }
            else
                {
                Logger.warn("Skipping Session " + configuration.getName()
                        + " Session provider returned null");
                }
            }
        }

    /**
     * Returns the {@link SessionConfiguration} for the specified session.
     *
     * @param sName  the name of the session
     *
     * @return the {@link SessionConfiguration} for the specified session,
     *         or {@code null} if this {@link Coherence} instance does not
     *         contain a {@link SessionConfiguration} with the specified
     *         name
     */
    private SessionConfiguration getSessionConfiguration(String sName)
        {
        SessionConfiguration configuration = f_config.getSessionConfigurations().get(sName);
        if (configuration == null)
            {
            configuration = f_mapAdditionalSessionConfig.get(sName);
            }
        return configuration;
        }

    /**
     * Returns a {@link Stream} of all the {@link SessionConfiguration session configurations}
     * this {@link Coherence} instance has.
     *
     * @return a {@link Stream} of all the {@link SessionConfiguration session configurations}
     *         this {@link Coherence} instance has
     */
    private Stream<SessionConfiguration> getSessionConfigurations()
        {
        return Stream.concat(f_config.getSessionConfigurations().values().stream(),
                             f_mapAdditionalSessionConfig.values().stream());
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
     * @param sScopePrefix   the prefix to prepend to the session scope
     * @param interceptors   optional {@link EventInterceptor interceptors} to add to
     *                       the session in addition to any in the configuration
     *
     * @return the configured and started {@link Session}
     */
    private static Optional<Session> ensureSessionInternal(SessionConfiguration configuration,
            Mode mode, String sScopePrefix, Iterable<? extends EventInterceptor<?>> interceptors)
        {
        SessionProvider   provider = configuration.sessionProvider().orElseGet(SessionProvider::get);
        Optional<Session> optional = provider.createSession(configuration, mode, sScopePrefix, interceptors);
        if (optional.isPresent())
            {
            String sName   = configuration.getName();
            if (sName == null || DEFAULT_NAME.equals(sName))
                {
                sName = "$Default$";
                }
            Logger.info("Created Session " + sName + " mode="
                                + configuration.getMode().orElse(mode));
            }
        return optional;
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
        displayStartBanner(sName, ccf);
        DefaultCacheServer dcs = new DefaultCacheServer(ccf);
        dcs.startDaemon(DefaultCacheServer.DEFAULT_WAIT_MILLIS);
        return dcs;
        }

    /**
     * Activate and start the autostart services in the specified system {@link ConfigurableCacheFactory}.
     * <p>
     * The {@link ConfigurableCacheFactory} will be wrapped in a {@link ServiceMonitor} that
     * will monitor and manage the {@link ConfigurableCacheFactory} services.
     *
     * @param sName  the name of the {@link ConfigurableCacheFactory}
     * @param ccf    the {@link ConfigurableCacheFactory} to start
     *
     * @return the {@link ServiceMonitor} that is managing and monitoring
     *         the {@link ConfigurableCacheFactory} services
     */
    private ServiceMonitor startSystemCCF(String sName, ExtensibleConfigurableCacheFactory ccf)
        {
        displayStartBanner(sName, ccf);
        ServiceMonitor monitor = new SimpleServiceMonitor(DefaultCacheServer.DEFAULT_WAIT_MILLIS);
        monitor.setConfigurableCacheFactory(ccf);
        ccf.activate();
        monitor.registerServices(ccf.getServiceMap());
        monitor.getThread().setName(sName + ':' + monitor.getThread().getName());
        return monitor;
        }

    private void displayStartBanner(String sName, ConfigurableCacheFactory ccf)
        {
        String  sScopeName = ccf.getScopeName();
        boolean fHasScope  = sScopeName != null && !sScopeName.isEmpty();

        Logger.info(() -> (sName == null || sName.isEmpty()
                           ? "Starting default session"
                           : "Starting session " + sName)
                          + (fHasScope ? " with scope name " + sScopeName : ""));

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
     * Ensure that the singleton system {@link Session} exists and is started.
     * <p>
     * The System session only applies to certain environments, so we may not
     * actually start anything.
     *
     * @param interceptors  any interceptors to add to the system session
     *
     * @return the singleton system {@link Session}
     */
    @SuppressWarnings("OptionalAssignedToNull")
    private Session initializeSystemSession(Iterable<? extends EventInterceptor<?>> interceptors)
        {
        boolean fCreated = false;
        if (f_mode == Mode.Gar)
            {
            if (m_localSystemSession == null)
                {
                m_localSystemSessionLock.lock();
                try
                    {
                    if (m_localSystemSession == null)
                        {
                        createSystemSession(interceptors);
                        fCreated = true;
                        }
                    }
                finally
                    {
                    m_localSystemSessionLock.unlock();
                    }
                }
            if (!fCreated)
                {
                m_localSystemSession.ifPresent(session -> registerInterceptors(session, interceptors));
                }
            }
        else
            {
            if (s_systemSession == null)
                {
                s_globalSystemSessionLock.lock();
                try
                    {
                    if (s_systemSession == null)
                        {
                        s_systemSession = createSystemSession(interceptors);
                        s_systemServiceMonitor = m_localSystemServiceMonitor;
                        fCreated        = true;
                        }
                    }
                finally
                    {
                    s_globalSystemSessionLock.unlock();
                    }
                }
            else
                {
                m_localSystemSession = s_systemSession;
                }
            if (!fCreated)
                {
                s_systemSession.ifPresent(session -> registerInterceptors(session, interceptors));
                }
            }

        return m_localSystemSession.orElse(null);
        }

    private Optional<ConfigurableCacheFactorySession> createSystemSession(Iterable<? extends EventInterceptor<?>> interceptors)
        {
        SessionConfiguration configuration = new SystemSessionConfiguration(f_mode);
        String               sScopePrefix  = getScopePrefix();

        Iterable<? extends EventInterceptor<?>> allInterceptors
                = join(interceptors, configuration.getInterceptors());

        Optional<ConfigurableCacheFactorySession> optional =
                ensureSessionInternal(configuration, f_mode, sScopePrefix, allInterceptors)
                        .map(ConfigurableCacheFactorySession.class::cast);

        if (optional.isPresent() && (Mode.ClusterMember == f_mode || Mode.Gar == f_mode))
            {
            ConfigurableCacheFactorySession    session   = optional.get();
            ExtensibleConfigurableCacheFactory ccfSystem = (ExtensibleConfigurableCacheFactory)
                    session.getConfigurableCacheFactory();
            m_localSystemServiceMonitor = startSystemCCF(configuration.getScopeName(), ccfSystem);
            session.activate();
            }

        registerHealthChecks();

        m_localSystemSessionConfig = configuration;
        m_localSystemSession       = optional;
        return optional;
        }

    /**
     * Visible for testing only, set the System {@link Session}.
     *
     * @param optional an optional containing the System {@link Session}
     */
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    static void setSystemSession(Optional<ConfigurableCacheFactorySession> optional)
        {
        s_systemSession = Objects.requireNonNull(optional);
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

    private void ensureMetrics()
        {
        if (f_mode.isClusterMember())
            {
            // metrics will have been started by a DCS instance
            return;
            }
        if (m_metricsMonitor == null)
            {
            f_lockMetrics.lock();
            try
                {
                if (m_metricsMonitor == null)
                    {
                    Map<Service, String> mapService = new HashMap<>();
                    MetricsHttpHelper.ensureMetricsService(mapService);
                    MetricsServiceMonitor monitor = new MetricsServiceMonitor();
                    monitor.registerServices(mapService);
                    monitor.start();
                    m_metricsMonitor = monitor;
                    }
                }
            finally
                {
                f_lockMetrics.unlock();
                }
            }
        }

    private void stopMetrics()
        {
        if (f_mode.isClusterMember())
            {
            // metrics is managed by a DCS instance
            return;
            }

        if (m_metricsMonitor != null)
            {
            f_lockMetrics.lock();
            try
                {
                if (m_metricsMonitor != null)
                    {
                    m_metricsMonitor.unregisterAll();
                    m_metricsMonitor = null;
                    }
                }
            finally
                {
                f_lockMetrics.unlock();
                }

            }
        }

    /**
     * Validate the {@link CoherenceConfiguration}.
     *
     * @param configuration  the {@link CoherenceConfiguration} to validate
     */
    private static void validate(CoherenceConfiguration configuration)
        {
        Collection<SessionConfiguration> sessions = configuration.getSessionConfigurations().values();
        Set<String>                      setName  = new HashSet<>();
        for (SessionConfiguration sessionConfiguration : sessions)
            {
            validate(sessionConfiguration);
            String sName = sessionConfiguration.getName();
            if (!setName.add(sName))
                {
                throw new IllegalStateException("A Session with the name '" + sName
                        + "' already exists in this Coherence configuration");
                }
            }
        }

    /**
     * Validate the {@link SessionConfiguration}.
     *
     * @param configuration  the {@link SessionConfiguration} to validate
     */
    private static void validate(SessionConfiguration configuration)
        {
        String sName = configuration.getName();
        if (sName == null)
            {
            throw new IllegalArgumentException("A session configuration must provide a non-null name");
            }
        }

    /**
     * Obtain the prefix to prepend to session scopes.
     *
     * @return the prefix to prepend to session scopes
     */
    private String getScopePrefix()
        {
        if (f_mode == Mode.Gar)
            {
            return f_config.getApplicationContext()
                    .map(a ->
                        {
                        String sScope = a.getDefaultScope();
                        return ServiceScheme.getScopePrefix(sScope, a.getContainerContext());
                        })
                    .orElse(getName());
            }
        return Coherence.DEFAULT_SCOPE;
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

        @SuppressWarnings("unused")
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
            f_mode   = mode == null ? Mode.ClusterMember : mode;
            }

        /**
         * Build a {@link Coherence} instance.
         * 
         * @return a {@link Coherence} instance
         */
        public Coherence build()
            {
            return build(null);
            }

        /**
         * Build a {@link Coherence} instance.
         *
         * @param loader  the {@link ClassLoader} associated with the {@link Coherence} instance
         *
         * @return a {@link Coherence} instance
         */
        public Coherence build(ClassLoader loader)
            {
            // validate the configuration
            Coherence.validate(f_config);

            return build(loader, false);
            }

        // ----- helper methods ---------------------------------------------

        protected Coherence build(boolean fAllowDuplicate)
            {
            return build(null, fAllowDuplicate);
            }

        protected Coherence build(ClassLoader loader, boolean fAllowDuplicate)
            {
            Coherence              coherence = new Coherence(f_config, f_mode);
            Map<String, Coherence> map       = ensureInstanceMap(loader);
            Coherence              prev      = map.putIfAbsent(f_config.getName(), coherence);
            if (prev != null && f_mode != Mode.Gar)
                {
                if (!fAllowDuplicate)
                    {
                    throw new IllegalStateException("A Coherence instance with the name "
                            + f_config.getName() + " already exists");
                    }
                return prev;
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
         * The {@link Coherence} instance should run as a non-cluster member Extend client.
         * The proxy will be discovered using the name service.
         */
        Client("remote", false),
        /**
         * The {@link Coherence} instance should run as a non-cluster member Extend client,
         * configured with a fixed address and port.
         */
        ClientFixed("remote-fixed", false),
        /**
         * The {@link Coherence} instance should run as a cluster member client.
         */
        ClusterMember("direct", true),
        /**
         * The {@link Coherence} instance should run as a non-cluster member gRPC client.
         * The proxy will be discovered using the name service.
         */
        Grpc("grpc", false),
        /**
         * The {@link Coherence} instance should run as a non-cluster member gRPC client,
         * configured with a fixed address and port.
         */
        GrpcFixed("grpc-fixed", false),
        /**
         * The {@link Coherence} instance has been created from a gar.
         */
        Gar("direct", true);

        Mode(String sClient, boolean fClusterMember)
            {
            f_sClient        = sClient;
            f_fClusterMember = fClusterMember;
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
         * Return {@code true} if this mode is a cluster member.
         *
         * @return {@code true} if this mode is a cluster member
         */
        public boolean isClusterMember()
            {
            return f_fClusterMember;
            }

        /**
         * Return the {@link Mode} for the given client name.
         *
         * @param sClient  the client name
         *
         * @return the {@link Mode} for the given client name
         *
         * @throws IllegalArgumentException – if specified client name does not match any {@link Mode}
         */
        public static Mode fromClientName(String sClient)
            {
            for (Mode mode : Mode.values())
                {
                if (mode.getClient().equals(sClient))
                    {
                    return mode;
                    }
                }
            throw new IllegalArgumentException("No Mode exists with a client name \"" + sClient + "\"");
            }

        // ----- data members -----------------------------------------------

        /**
         * The default {@code coherence.client} property for this mode.
         */
        private final String f_sClient;

        /**
         * A flag indicating whether this mode is a cluster member.
         */
        private final boolean f_fClusterMember;
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
     * A custom {@link SimpleServiceMonitor} that allows
     * an easy way to unregister all services.
     */
    private static class MetricsServiceMonitor
            extends SimpleServiceMonitor
        {
        public void unregisterAll()
            {
            Set<Service> set = new HashSet<>(m_mapServices.keySet());
            unregisterServices(set);
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

    /**
     * The system property to use to set the default timeout to wait for Coherence instances to start.
     */
    public static final String PROP_START_TIMEOUT = "coherence.startup.timeout";

    /**
     * The default start up timeout.
     */
    public static final Duration DEFAULT_START_TIMEOUT = new Duration(5, Duration.Magnitude.MINUTE);

    // ----- data members ---------------------------------------------------

    /**
     * The map of all named {@link Coherence} instances by class loader.
     */
    private static final Map<ClassLoader, Map<String, Coherence>> s_mapByLoader = new CopyOnWriteMap<>(WeakHashMap.class);

    /**
     * The lock controlling mutation of {@link #s_mapByLoader}.
     */
    private static final Lock s_instanceLock = new ReentrantLock();

    /**
     * The lock to manage the global system session state.
     */
    private static final Lock s_globalSystemSessionLock = new ReentrantLock();

    /**
     * The global System session.
     */
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private static Optional<ConfigurableCacheFactorySession> s_systemSession;

    /**
     * The {@link ServiceMonitor} wrapping the System session.
     */
    private static ServiceMonitor s_systemServiceMonitor;

    /**
     * The lock to manage this instance's system session state.
     */
    private final Lock m_localSystemSessionLock = new ReentrantLock();

    /**
     * This instance's System session configuration.
     */
    private SessionConfiguration m_localSystemSessionConfig;

    /**
     * This instance's System session.
     */
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private Optional<ConfigurableCacheFactorySession> m_localSystemSession;

    /**
     * The {@link DefaultCacheServer} wrapping the local System session.
     */
    private ServiceMonitor m_localSystemServiceMonitor;

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
     * A {@link Map} of additional {@link SessionConfiguration session configurations}.
     */
    private final Map<String, SessionConfiguration> f_mapAdditionalSessionConfig = new ConcurrentHashMap<>();

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

    /**
     * The lock to control starting and stopping metrics when running as a client.
     */
    private final ReentrantLock f_lockMetrics = new ReentrantLock();

    /**
     * The {@link ServiceMonitor} used to monitor metrics when running as a client.
     */
    private MetricsServiceMonitor m_metricsMonitor;
    }
