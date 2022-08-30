/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package grpc.client;

import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.coherence.client.AsyncNamedCacheClient;
import com.oracle.coherence.client.NamedCacheClient;
import com.oracle.coherence.common.base.Exceptions;
import com.oracle.coherence.grpc.CredentialsHelper;

import com.tangosol.io.Serializer;

import com.tangosol.net.Coherence;
import com.tangosol.net.CoherenceConfiguration;

import com.tangosol.net.Session;
import com.tangosol.net.SessionConfiguration;
import com.tangosol.net.grpc.GrpcChannelDependencies;
import com.tangosol.net.grpc.GrpcDependencies;
import io.grpc.ChannelCredentials;
import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
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

import static org.hamcrest.CoreMatchers.is;

/**
 * A utility class to deploy the Coherence gRPC proxy service
 * into a gRPC server.
 *
 * @author Jonathan Knight  2019.11.29
 * @since 20.06
 */
public final class ServerHelper
        implements BeforeAllCallback, AfterAllCallback
    {
    /**
     * Create a {@link ServerHelper}.
     */
    public ServerHelper()
        {
        m_properties = new Properties();
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

        return new AsyncNamedCacheClient<>(deps);
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
            ChannelCredentials credentials = InsecureChannelCredentials.create();
            m_channel = Grpc.newChannelBuilderForAddress("127.0.0.1", nPort, credentials).build();

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
            ManagedChannel managedChannel = m_channel;
            managedChannel.shutdown();
            try
                {
                if (!managedChannel.awaitTermination(20, TimeUnit.SECONDS))
                    {
                    managedChannel.shutdownNow();
                    }
                }
            catch (InterruptedException ignored)
                {
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

    private Session m_session;

    private ManagedChannel m_channel;

    private final Properties m_properties;

    private final Map<String, Map<String, AsyncNamedCacheClient<?, ?>>> clients = new ConcurrentHashMap<>();

    private volatile boolean m_fRunning;
    }
