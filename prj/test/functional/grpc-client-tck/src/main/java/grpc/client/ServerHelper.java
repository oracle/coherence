/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package grpc.client;

import com.oracle.bedrock.runtime.coherence.callables.FindGrpcProxyPort;

import com.oracle.coherence.common.base.Exceptions;

import com.oracle.coherence.grpc.NamedCacheProtocol;

import com.oracle.coherence.grpc.client.common.AsyncNamedCacheClient;
import com.oracle.coherence.grpc.client.common.GrpcConnection;
import com.oracle.coherence.grpc.client.common.GrpcRemoteService;
import com.oracle.coherence.grpc.client.common.NamedCacheClient;
import com.oracle.coherence.grpc.client.common.NamedCacheClientChannel;

import com.oracle.coherence.grpc.client.common.v0.GrpcConnectionV0;
import com.oracle.coherence.grpc.messages.cache.v1.NamedCacheResponse;
import com.tangosol.internal.net.grpc.DefaultRemoteGrpcCacheServiceDependencies;
import com.tangosol.internal.net.grpc.RemoteGrpcServiceDependencies;

import com.tangosol.io.Serializer;

import com.tangosol.net.Coherence;
import com.tangosol.net.CoherenceConfiguration;
import com.tangosol.net.Session;
import com.tangosol.net.SessionConfiguration;
import com.tangosol.net.grpc.GrpcDependencies;

import io.grpc.Channel;
import io.grpc.ManagedChannel;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import java.lang.reflect.Modifier;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * A utility class to deploy the Coherence gRPC proxy service
 * into a gRPC server.
 *
 * @author Jonathan Knight  2019.11.29
 * @since 20.06
 */
@SuppressWarnings({"CallToPrintStackTrace"})
public final class ServerHelper
        implements BeforeAllCallback, AfterAllCallback
    {
    /**
     * Create a {@link ServerHelper}.
     */
    public ServerHelper()
        {
        m_properties = new Properties();
        int    nProtocol = NamedCacheProtocol.VERSION;
        String sProp     = System.getProperty("test.grpc.cache.protocol");
        if (sProp != null)
            {
            nProtocol = Integer.parseInt(sProp);
            }
        m_nProtocolVersion = nProtocol;
        }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception
        {
        Optional<Class<?>> testClass = context.getTestClass();
        testClass.ifPresent(this::setupServer);
        start();
        }

    @Override
    public void afterAll(ExtensionContext context)
        {
        shutdown();
        }

    public ServerHelper setProperty(String sKey, String sValue)
        {
        if (isRunning())
            {
            throw new IllegalStateException("The server has already started");
            }
        m_properties.setProperty(sKey, sValue);
        return this;
        }

    public ServerHelper setScope(String sScope)
        {
        if (isRunning())
            {
            throw new IllegalStateException("The server has already started");
            }
        m_sScope = sScope == null ? GrpcDependencies.DEFAULT_SCOPE : sScope;
        return this;
        }

    public ServerHelper setProtocolVersion(int nVersion)
        {
        if (isRunning())
            {
            throw new IllegalStateException("The server has already started");
            }
        m_nProtocolVersion = nVersion;
        return this;
        }

    @SuppressWarnings("unchecked")
    public  <K, V> NamedCacheClient<K, V> createClient(String sScope,
                                                       String sCacheName,
                                                       String sSerializerName,
                                                       Serializer serializer)
        {
        Map<String, AsyncNamedCacheClient<?, ?>> map = clients.computeIfAbsent(sCacheName, k -> new HashMap<>());
        AsyncNamedCacheClient<K, V> async = (AsyncNamedCacheClient<K, V>)
                map.computeIfAbsent(sSerializerName, k -> newClient(sScope, sCacheName, sSerializerName, serializer));

        if (!async.isActiveInternal())
            {
            map.remove(sSerializerName);
            return createClient(sScope, sCacheName, sSerializerName, serializer);
            }
        return (NamedCacheClient<K, V>) async.getNamedCache();
        }

    public AsyncNamedCacheClient<?, ?> newClient(String sScope, String sCacheName, String sFormat, Serializer serializer)
        {
        AsyncNamedCacheClient.DefaultDependencies deps
                = new  AsyncNamedCacheClient.DefaultDependencies(sCacheName, m_channel, null);

        deps.setScope(sScope);
        deps.setSerializer(serializer, sFormat);

        RemoteGrpcServiceDependencies serviceDeps = new DefaultRemoteGrpcCacheServiceDependencies();

        GrpcConnection connection;
        if (m_nProtocolVersion == 0)
            {
            connection = new GrpcConnectionV0(m_channel);
            }
        else
            {
            GrpcConnection.Dependencies connectionDeps
                    = new GrpcConnection.DefaultDependencies(NamedCacheProtocol.PROTOCOL_NAME, serviceDeps,
                    m_channel, m_nProtocolVersion, m_nProtocolVersion, serializer);

            connection = GrpcRemoteService.connect(connectionDeps, NamedCacheResponse.class);
            }

        NamedCacheClientChannel protocol = NamedCacheClientChannel.createProtocol(deps, connection);
        return new AsyncNamedCacheClient<>(deps, protocol);
        }

    // ----- helper methods -------------------------------------------------

    void setupServer(Class<?> cls)
        {
        Arrays.stream(cls.getMethods())
                .filter(m -> Modifier.isStatic(m.getModifiers()))
                .filter(m -> m.isAnnotationPresent(ServerHelper.Configuration.class))
                .filter(m -> m.getParameterCount() == 1)
                .filter(m -> ServerHelper.class.equals(m.getParameterTypes()[0]))
                .forEach(m ->
                         {
                         try
                             {
                             m.invoke(null, this);
                             }
                         catch (Throwable e)
                             {
                             e.printStackTrace();
                             }
                         });

        }

    /**
     * Start the server.
     */
    void start()
        {
        if (isRunning())
            {
            return;
            }

        for (String sName : m_properties.stringPropertyNames())
            {
            System.setProperty(sName, m_properties.getProperty(sName));
            }

        try
            {
            System.setProperty(GrpcDependencies.PROP_ENABLED, "true");

            SessionConfiguration sessionConfiguration = SessionConfiguration.builder()
                    .withScopeName(m_sScope)
                    .named(m_sScope)
                    .build();

            CoherenceConfiguration configuration = CoherenceConfiguration.builder()
                    .withSession(sessionConfiguration)
                    .build();

            Coherence coherence = Coherence.clusterMember(configuration).start()
                    .get(5, TimeUnit.MINUTES);

            m_session = coherence.getSession(m_sScope);

            int nPort = FindGrpcProxyPort.local();
            m_channel = ServerHelperChannelProvider.getChannel("127.0.0.1", nPort);

            m_fRunning = true;
            }
        catch (Exception e)
            {
            e.printStackTrace();
            throw Exceptions.ensureRuntimeException(e);
            }
        }

    /**
     * Stop the server.
     */
    void shutdown()
        {
        clients.values()
                .stream()
                .flatMap(m -> m.values().stream())
                .forEach(client ->
                    {
                    try
                        {
                        if (client.isActiveInternal())
                            {
                            client.release().join();
                            }
                        }
                    catch (Exception e)
                        {
                        // ignored - we're done anyway.
                        }
                    });

        if (m_channel != null)
            {
            Channel channel = m_channel;
            if (channel instanceof ManagedChannel managedChannel)
                {
                managedChannel.shutdownNow();
                }
            }
        Coherence.closeAll();
        m_fRunning = false;
        }

    private boolean isRunning()
        {
        return m_fRunning && IsGrpcProxyRunning.locally();
        }

    // ----- inner class Configuration --------------------------------------

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Configuration {
    }

    // ----- accessors ------------------------------------------------------

    Session getSession()
        {
        return m_session;
        }

    // ----- data members ---------------------------------------------------

    String m_sScope = GrpcDependencies.DEFAULT_SCOPE;

    private int m_nProtocolVersion;

    private Session m_session;

    private Channel m_channel;

    private final Properties m_properties;

    private final Map<String, Map<String, AsyncNamedCacheClient<?, ?>>> clients = new ConcurrentHashMap<>();

    private volatile boolean m_fRunning;
    }
