/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.bedrock.runtime.coherence;

import com.oracle.bedrock.OptionsByType;
import com.oracle.bedrock.runtime.Platform;
import com.oracle.bedrock.runtime.coherence.callables.FindExtendProxyPort;
import com.oracle.bedrock.runtime.coherence.callables.FindGrpcProxyPort;
import com.oracle.bedrock.runtime.coherence.callables.HasSession;
import com.oracle.bedrock.runtime.coherence.callables.HasSessionWithScope;
import com.oracle.bedrock.runtime.coherence.options.LocalHost;
import com.oracle.bedrock.runtime.coherence.options.MachineName;
import com.oracle.bedrock.runtime.concurrent.RemoteCallable;
import com.oracle.bedrock.runtime.concurrent.callable.RemoteCallableStaticMethod;
import com.oracle.bedrock.runtime.java.ContainerBasedJavaApplicationLauncher;
import com.oracle.bedrock.runtime.java.JavaApplication;
import com.oracle.bedrock.runtime.java.container.ContainerClassLoader;
import com.oracle.bedrock.runtime.java.options.ClassName;
import com.oracle.bedrock.runtime.java.options.Headless;
import com.oracle.bedrock.runtime.java.options.IPv4Preferred;
import com.oracle.bedrock.runtime.java.options.JavaModules;
import com.oracle.bedrock.runtime.java.options.SystemProperties;
import com.oracle.bedrock.runtime.java.options.SystemProperty;
import com.oracle.bedrock.runtime.remote.RemotePlatform;
import com.oracle.bedrock.util.Trilean;
import com.tangosol.net.Coherence;
import com.tangosol.net.NamedCache;
import com.tangosol.net.Session;
import com.tangosol.util.UID;
import com.tangosol.util.UUID;

import java.io.NotSerializableException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.InetAddress;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public interface CoherenceClusterMember
        extends JavaApplication
    {
    /**
     * Obtains the number of members in the cluster in which the member represented
     * by this {@link CoherenceClusterMember} belongs.
     *
     * @return the number of members in the cluster
     */
    int getClusterSize();


    /**
     * Obtains the cluster port for the {@link CoherenceClusterMember}.
     *
     * @return the cluster port
     */
    int getLocalMemberClusterPort();


    /**
     * Obtains the local member id for the {@link CoherenceClusterMember}.
     *
     * @return the local member id
     */
    int getLocalMemberId();


    /**
     * Obtains the local member {@link UID} for the {@link CoherenceClusterMember}.
     *
     * @return the local member {@link UID}
     */
    UID getLocalMemberUID();


    /**
     * Obtains the local member {@link UUID} for the {@link CoherenceClusterMember}.
     *
     * @return the local member {@link UUID}
     */
    UUID getLocalMemberUUID();


    /**
     * Obtains the member {@link UID}s for the {@link CoherenceCluster} in which the
     * {@link CoherenceClusterMember} is operating.
     *
     * @return a {@link Set} of {@link UID}, one for each {@link CoherenceClusterMember}
     */
    Set<UID> getClusterMemberUIDs();


    /**
     * Obtains the machine name for the local member.
     *
     * @return the machine name
     */
    String getMachineName();


    /**
     * Obtains the name for the local member.
     *
     * @return the member name
     */
    String getMemberName();


    /**
     * Obtains the role name for the local member.
     *
     * @return the role name
     */
    String getRoleName();


    /**
     * Obtains the role name for the local member.
     *
     * @return the role name
     */
    String getRackName();


    /**
     * Obtains the site name for the local member.
     *
     * @return the site name
     */
    String getSiteName();


    /**
     * Obtains the cluster name for the local member.
     *
     * @return the site name
     */
    String getClusterName();


    /**
     * Obtains a proxy of the specified {@link NamedCache} available in the
     * {@link CoherenceClusterMember}.
     * <p>
     * WARNING: Some methods on the returned {@link NamedCache} proxy are not
     * available for remote execution and are thus not supported.  Attempts to
     * use such methods will thrown {@link UnsupportedOperationException}.
     * <p>
     * The unsupported methods are: getCacheService, addMapListener,
     * removeMapListener, entrySet, values, addIndex and removeIndex.
     * <p>
     * Additionally note that method invocations taking unserializable parameters
     * or returning unserializable values will throw {@link NotSerializableException}s.
     *
     * @param cacheName the name of the {@link NamedCache}
     * @return a proxy to the {@link NamedCache}
     */
    <K, V> NamedCache<K, V> getCache(String cacheName);


    /**
     * Obtains a proxy of a strongly-typed {@link NamedCache} available in the
     * {@link CoherenceClusterMember}.
     * <p>
     * WARNING: Some methods on the returned {@link NamedCache} proxy are not
     * available for remote execution and are thus not supported.  Attempts to
     * use such methods will thrown {@link UnsupportedOperationException}.
     * <p>
     * The unsupported methods are: getCacheService, addMapListener,
     * removeMapListener, entrySet, values, addIndex and removeIndex.
     * <p>
     * Additionally note that method invocations taking unserializable parameters
     * or returning unserializable values will throw {@link NotSerializableException}s.
     *
     * @param cacheName  the name of the {@link NamedCache}
     * @param keyClass   the type of the keys for the {@link NamedCache}
     * @param valueClass the type of the values for the {@link NamedCache}
     * @param <K>        the type of the key class
     * @param <V>        the type of the value class
     * @return a proxy to the {@link NamedCache}
     */
    <K, V> NamedCache<K, V> getCache(String cacheName, Class<K> keyClass, Class<V> valueClass);


    /**
     * Obtains a proxy of a {@link NamedCache} from the named {@link Session} available in the
     * {@link CoherenceClusterMember}.
     * <p>
     * WARNING: Some methods on the returned {@link NamedCache} proxy are not
     * available for remote execution and are thus not supported.  Attempts to
     * use such methods will thrown {@link UnsupportedOperationException}.
     * <p>
     * The unsupported methods are: getCacheService, addMapListener,
     * removeMapListener, entrySet, values, addIndex and removeIndex.
     * <p>
     * Additionally note that method invocations taking unserializable parameters
     * or returning unserializable values will throw {@link NotSerializableException}s.
     *
     * @param sessionName the name of the {@link Session} that owns the cache
     * @param cacheName   the name of the {@link NamedCache}
     * @param <K>         the type of the key class
     * @param <V>         the type of the value class
     * @return a proxy to the {@link NamedCache}
     */
    <K, V> NamedCache<K, V> getCache(String sessionName, String cacheName);


    /**
     * Obtains a proxy of a strongly typed {@link NamedCache} from the named {@link Session}
     * available in the {@link CoherenceClusterMember}.
     * <p>
     * WARNING: Some methods on the returned {@link NamedCache} proxy are not
     * available for remote execution and are thus not supported.  Attempts to
     * use such methods will thrown {@link UnsupportedOperationException}.
     * <p>
     * The unsupported methods are: getCacheService, addMapListener,
     * removeMapListener, entrySet, values, addIndex and removeIndex.
     * <p>
     * Additionally note that method invocations taking unserializable parameters
     * or returning unserializable values will throw {@link NotSerializableException}s.
     *
     * @param sessionName the name of the {@link Session} that owns the cache
     * @param cacheName   the name of the {@link NamedCache}
     * @param keyClass    the type of the keys for the {@link NamedCache}
     * @param valueClass  the type of the values for the {@link NamedCache}
     * @param <K>         the type of the key class
     * @param <V>         the type of the value class
     * @return a proxy to the {@link NamedCache}
     */
    <K, V> NamedCache<K, V> getCache(String sessionName, String cacheName, Class<K> keyClass, Class<V> valueClass);


    /**
     * Obtains a proxy of a {@link NamedCache} from the named {@link Session}
     * in the named {@link com.tangosol.net.Coherence} instance, available in the
     * {@link CoherenceClusterMember}.
     * <p>
     * WARNING: Some methods on the returned {@link NamedCache} proxy are not
     * available for remote execution and are thus not supported.  Attempts to
     * use such methods will thrown {@link UnsupportedOperationException}.
     * <p>
     * The unsupported methods are: getCacheService, addMapListener,
     * removeMapListener, entrySet, values, addIndex and removeIndex.
     * <p>
     * Additionally note that method invocations taking unserializable parameters
     * or returning unserializable values will throw {@link NotSerializableException}s.
     *
     * @param coherenceName the name of the {@link com.tangosol.net.Coherence} instance that owns the {@link Session}
     * @param sessionName   the name of the {@link Session} that owns the cache
     * @param cacheName     the name of the {@link NamedCache}
     * @param <K>           the type of the key class
     * @param <V>           the type of the value class
     * @return a proxy to the {@link NamedCache}
     */
    <K, V> NamedCache<K, V> getCache(String coherenceName, String sessionName, String cacheName);


    /**
     * Obtains a proxy of a strongly typed {@link NamedCache} from the named {@link Session}
     * in the named {@link com.tangosol.net.Coherence} instance, available in the
     * {@link CoherenceClusterMember}.
     * <p>
     * WARNING: Some methods on the returned {@link NamedCache} proxy are not
     * available for remote execution and are thus not supported.  Attempts to
     * use such methods will thrown {@link UnsupportedOperationException}.
     * <p>
     * The unsupported methods are: getCacheService, addMapListener,
     * removeMapListener, entrySet, values, addIndex and removeIndex.
     * <p>
     * Additionally note that method invocations taking unserializable parameters
     * or returning unserializable values will throw {@link NotSerializableException}s.
     *
     * @param coherenceName the name of the {@link com.tangosol.net.Coherence} instance that owns the {@link Session}
     * @param sessionName   the name of the {@link Session} that owns the cache
     * @param cacheName     the name of the {@link NamedCache}
     * @param keyClass      the type of the keys for the {@link NamedCache}
     * @param valueClass    the type of the values for the {@link NamedCache}
     * @param <K>           the type of the key class
     * @param <V>           the type of the value class
     * @return a proxy to the {@link NamedCache}
     */
    <K, V> NamedCache<K, V> getCache( String coherenceName, String sessionName, String cacheName,
            Class<K> keyClass, Class<V> valueClass);


    /**
     * Obtains the default {@link Session} from the default {@link com.tangosol.net.Coherence}
     * instance available in the {@link CoherenceClusterMember}.
     * <p>
     * WARNING: Some methods on the returned {@link Session} proxy are not
     * available for remote execution and are thus not supported.  Attempts to
     * use such methods will thrown {@link UnsupportedOperationException}.
     * <p>
     * Additionally note that method invocations taking unserializable parameters
     * or returning unserializable values will throw {@link NotSerializableException}s.
     *
     * @return a proxy to the remote {@link Session}
     */
    Session getSession();


    /**
     * Obtains the named {@link Session} from the default {@link com.tangosol.net.Coherence}
     * instance available in the {@link CoherenceClusterMember}.
     * <p>
     * WARNING: Some methods on the returned {@link Session} proxy are not
     * available for remote execution and are thus not supported.  Attempts to
     * use such methods will thrown {@link UnsupportedOperationException}.
     * <p>
     * Additionally note that method invocations taking unserializable parameters
     * or returning unserializable values will throw {@link NotSerializableException}s.
     *
     * @param sessionName the name of the {@link Session}
     * @return a proxy to the remote {@link Session}
     */
    Session getSession(String sessionName);


    /**
     * Obtains the named {@link Session} from the named {@link com.tangosol.net.Coherence}
     * instance available in the {@link CoherenceClusterMember}.
     * <p>
     * WARNING: Some methods on the returned {@link Session} proxy are not
     * available for remote execution and are thus not supported.  Attempts to
     * use such methods will thrown {@link UnsupportedOperationException}.
     * <p>
     * Additionally note that method invocations taking unserializable parameters
     * or returning unserializable values will throw {@link NotSerializableException}s.
     *
     * @param coherenceName the name of the {@link com.tangosol.net.Coherence} instance
     * @param sessionName   the name of the {@link Session}
     * @return a proxy to the remote {@link Session}
     */
    Session getSession(String coherenceName, String sessionName);

    /**
     * Determines if a specified service is being run by the {@link CoherenceClusterMember}.
     *
     * @param serviceName the name of the service
     * @return <code>true</code> if the service is running, <code>false</code> otherwise
     */
    boolean isServiceRunning(String serviceName);


    /**
     * Determines if a specified service is being run by the {@link CoherenceClusterMember}.
     *
     * @param scopeName   the scope name of the service
     * @param serviceName the name of the service
     * @return <code>true</code> if the service is running, <code>false</code> otherwise
     */
    default boolean isServiceRunning(String scopeName, String serviceName)
        {
        return isServiceRunning(scopeName + ":" + serviceName);
        }


    /**
     * Returns {@code true} if the default {@link Coherence} instance is running.
     *
     * @return {@code true} if the default {@link Coherence} instance is running
     */
    boolean isCoherenceRunning();

    /**
     * Returns {@code true} if the named {@link Coherence} instance is running.
     *
     * @return {@code true} if the named {@link Coherence} instance is running
     */
    boolean isCoherenceRunning(String sName);

    /**
     * Returns {@code true} if the member "safe" health check returns {@code true}.
     *
     * @return {@code true} if the member "safe" health check returns {@code true}
     */
    boolean isSafe();

    /**
     * Returns {@code true} if the member "ready" health check returns {@code true}.
     *
     * @return {@code true} if the member "ready" health check returns {@code true}
     */
    boolean isReady();

    /**
     * Determines if a specified service is storage enabled.
     *
     * @param serviceName the name of the service
     * @return a {@link Trilean} indicating if the service is storage enabled {@link Trilean#TRUE},
     * disabled {@link Trilean#FALSE} or unknown / undefined / not applicable {@link Trilean#UNKNOWN}
     */
    Trilean isStorageEnabled(String serviceName);


    /**
     * Determines if a specified service is storage enabled.
     *
     * @param scopeName   the scope name of the service
     * @param serviceName the name of the service
     * @return a {@link Trilean} indicating if the service is storage enabled {@link Trilean#TRUE},
     * disabled {@link Trilean#FALSE} or unknown / undefined / not applicable {@link Trilean#UNKNOWN}
     */
    default Trilean isStorageEnabled(String scopeName, String serviceName)
        {
        return isStorageEnabled(scopeName + ":" + serviceName);
        }


    /**
     * Returns the service names known to the Coherence cluster.
     *
     * @return the service names known to the Coherence cluster
     */
    Set<String> getServiceNames();

    /**
     * Determines the status of a service being run by the {@link CoherenceClusterMember}.
     *
     * @param serviceName the name of the service
     * @return the {@link ServiceStatus}
     */
    ServiceStatus getServiceStatus(String serviceName);


    /**
     * Returns the number of Extend clients currently connected
     * to a specific proxy service.
     *
     * @param sProxyName  the name of the Extend proxy service
     *
     * @return  the number of clients connected to the proxy service
     */
    int getExtendConnectionCount(String sProxyName);

    /**
     * Returns {@code true} if the specific proxy service has a
     * connected client with the specified {@link UUID}.
     * .
     *
     * @param sProxyName  the name of the Extend proxy service
     * @param uuid        the client {@link UUID}
     *
     * @return  the number of clients connected to the proxy service
     */
    boolean hasExtendConnection(String sProxyName, UUID uuid);

    /**
     * Determines the status of a service being run by the {@link CoherenceClusterMember}.
     *
     * @param scopeName   the scope name of the service
     * @param serviceName the name of the service
     * @return the {@link ServiceStatus}
     */
    default ServiceStatus getServiceStatus(String scopeName, String serviceName)
        {
        return getServiceStatus(scopeName + ":" + serviceName);
        }

    void threadDump();

    /**
     * Return the port the default Extend proxy is listening on.
     *
     * @return  the port the default Extend proxy is listening on,
     *          or {@code -1} if the proxy is not running
     */
    default int getExtendProxyPort()
        {
        return invoke(FindExtendProxyPort.INSTANCE);
        }

    /**
     * Return the port a specific Extend proxy is listening on.
     *
     * @param sServiceName  the proxy service name
     *
     * @return  the port the Extend proxy is listening on,
     *          or {@code -1} if the proxy is not running
     */
    default int getExtendProxyPort(String sServiceName)
        {
        return invoke(new FindExtendProxyPort(sServiceName));
        }

    /**
     * Return the port the default gRPC proxy is listening on.
     *
     * @return  the port the default gRPC proxy is listening on,
     *          or {@code -1} if the proxy is not running
     */
    default int getGrpcProxyPort()
        {
        return invoke(FindGrpcProxyPort.INSTANCE);
        }

    /**
     * Return the port a scoped gRPC proxy is listening on.
     *
     * @param sScope  the scope prefix to use to locate the gRPC proxy
     *
     * @return  the port the default gRPC proxy is listening on,
     *          or {@code -1} if the proxy is not running
     */
    default int getGrpcProxyPort(String sScope)
        {
        return invoke(new FindGrpcProxyPort(sScope));
        }

    /**
     * Return {@code true} if the cluster member has a named {@link Session} started by
     * the Coherence bootstrap API.
     *
     * @param sName  the name of the session
     *
     * @return {@code true} if the cluster member has a named {@link Session} started by
     *         the Coherence bootstrap API
     */
    default boolean hasSession(String sName)
        {
        return invoke(new HasSession(sName));
        }

    /**
     * Return {@code true} if the cluster member has a named {@link Session} started by
     * the Coherence bootstrap API.
     *
     * @param sSessionName    the name of the session
     * @param sCoherenceName  an optional {@link Coherence} instance name
     *
     * @return {@code true} if the cluster member has a named {@link Session} started by
     *         the Coherence bootstrap API
     */
    default boolean hasSession(String sSessionName, String sCoherenceName)
        {
        return invoke(new HasSession(sSessionName, sCoherenceName));
        }

    /**
     * Return {@code true} if the cluster member has a {@link Session} with a specific
     * scope started by the Coherence bootstrap API.
     *
     * @param sName  the session scope name
     *
     * @return {@code true} if the cluster member has a {@link Session} with a specific
     *         scope started by the Coherence bootstrap API
     */
    default boolean hasSessionWithScope(String sName)
        {
        return invoke(new HasSessionWithScope(sName));
        }

    /**
     * Return {@code true} if the cluster member has a {@link Session} with a specific
     * scope started by the Coherence bootstrap API.
     *
     * @param sScopeName      the session scope
     * @param sCoherenceName  an optional {@link Coherence} instance name
     *
     * @return {@code true} if the cluster member has a {@link Session} with a specific
     *         scope started by the Coherence bootstrap API.
     */
    default boolean hasSessionWithScope(String sScopeName, String sCoherenceName)
        {
        return invoke(new HasSessionWithScope(sScopeName, sCoherenceName));
        }

    /**
     * The {@link com.oracle.bedrock.runtime.MetaClass} for {@link CoherenceClusterMember}s.
     */
    class MetaClass
            implements com.oracle.bedrock.runtime.MetaClass<CoherenceClusterMember>,
                       ContainerBasedJavaApplicationLauncher.ApplicationController
        {
        /**
         * The com.tangosol.net.DefaultCacheServer classname.
         */
        public static final String DEFAULT_CACHE_SERVER_CLASSNAME = "com.tangosol.net.DefaultCacheServer";

        /**
         * The default main classname.
         */
        public static final String MAIN_CLASSNAME = "com.tangosol.net.Coherence";

        /**
         * The com.tangosol.net.CacheFactory classname.
         */
        public static final String CACHE_FACTORY_CLASSNAME = "com.tangosol.net.CacheFactory";


        /**
         * Constructs a {@link MetaClass} for a {@link CoherenceClusterMember}.
         */
        @OptionsByType.Default
        public MetaClass()
            {
            }


        @Override
        public Class<? extends CoherenceClusterMember> getImplementationClass(Platform platform,
                OptionsByType optionsByType)
            {
            return CoherenceCacheServer.class;
            }


        @Override
        public void onLaunching(Platform platform, OptionsByType optionsByType)
            {
            // automatically define the default cache server as the default class
            optionsByType.addIfAbsent(ClassName.of(MAIN_CLASSNAME));

            // automatically define IPv4
            optionsByType.addIfAbsent(IPv4Preferred.yes());

            // cache servers are always headless
            optionsByType.add(Headless.enabled());

            SystemProperties properties = optionsByType.get(SystemProperties.class)
                    .addIfAbsent(SystemProperty.of(LocalHost.PROPERTY,GetLocalHostProperty.INSTANCE))
                    .addIfAbsent(SystemProperty.of(MachineName.PROPERTY, GetMemberProperty.INSTANCE));

            // update the system properties as it may have been modified
            optionsByType.add(properties);

            JavaModules javaModules = optionsByType.get(JavaModules.class);

            String sModule = getClass().getModule().getName();
            optionsByType.add(javaModules.adding(sModule, "com.oracle.coherence"));
            }


        @Override
        public void onLaunch(Platform platform, OptionsByType optionsByType)
            {
            // there's nothing to do before launching the application
            }


        @Override
        public void onLaunched(Platform platform, CoherenceClusterMember member, OptionsByType optionsByType)
            {
            // nothing to do after launch
            }


        @Override
        public CompletableFuture<Void> start(ContainerBasedJavaApplicationLauncher.ControllableApplication application)
            {
            RemoteCallable<Void> callable = new StartCoherence();

            return application.submit(callable);
            }


        @Override
        public CompletableFuture<Void> destroy(ContainerBasedJavaApplicationLauncher.ControllableApplication application)
            {
            RemoteCallable<Void> callable = new RemoteCallableStaticMethod<>(MAIN_CLASSNAME, "closeAll");

            return application.submit(callable);
            }


        @Override
        public void configure(ContainerClassLoader containerClassLoader, PipedOutputStream pipedOutputStream,
                PipedInputStream pipedInputStream, OptionsByType optionsByType)
            {
            ClassName className = optionsByType.getOrSetDefault(ClassName.class, ClassName.of(MAIN_CLASSNAME));

            ContainerBasedJavaApplicationLauncher.configureRemoteChannel(containerClassLoader, pipedOutputStream,
                    pipedInputStream, className.getName());
            }
        }


    // ----- inner class: GetMemberProperty ---------------------------------

    /**
     * A {@link SystemProperty.ContextSensitiveValue} to set the Coherence member name.
     */
    class GetMemberProperty
            implements SystemProperty.ContextSensitiveValue
        {
        @Override
        public Object resolve(String name, Platform platform, OptionsByType optionsByType)
            {
            try
                {
                if (platform instanceof RemotePlatform)
                    {
                    return platform.getName();
                    }
                }
            catch (NoClassDefFoundError e)
                {
                // ignored
                }
            return null;
            }

        private static GetMemberProperty INSTANCE = new GetMemberProperty();
        }

    // ----- inner class: GetLocalHostProperty ------------------------------

    /**
     * A {@link SystemProperty.ContextSensitiveValue} to set the Coherence member name.
     */
    class GetLocalHostProperty
            implements SystemProperty.ContextSensitiveValue
        {
        @Override
        public Object resolve(String name, Platform platform, OptionsByType optionsByType)
            {
            try
                {
                if (platform instanceof RemotePlatform)
                    {
                    InetAddress inetAddress = platform.getAddress();

                    if (inetAddress == null)
                        {
                        return null;    // property doesn't exist
                        }
                    else
                        {
                        return inetAddress.getHostAddress();
                        }
                    }
                }
            catch (NoClassDefFoundError e)
                {
                // ignored
                }
            return null;    // property doesn't exist
            }

        private static GetLocalHostProperty INSTANCE = new GetLocalHostProperty();
        }

    // ----- inner class: StartCoherence ------------------------------------

    /**
     * A {@link RemoteCallable} that will start a {@link Coherence} cluster member
     * using the default configuration.
     */
    class StartCoherence
            implements RemoteCallable<Void>
        {
        @Override
        public Void call() throws Exception
            {
            Coherence.clusterMember().start();
            return null;
            }
        }
    }
