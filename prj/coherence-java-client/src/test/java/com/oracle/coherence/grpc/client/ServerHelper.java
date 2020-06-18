/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.client;

import com.oracle.coherence.cdi.Scope;
import com.oracle.coherence.grpc.proxy.NamedCacheService;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.DefaultCacheServer;
import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.helidon.config.Config;
import io.helidon.grpc.client.GrpcChannelsProvider;
import io.helidon.grpc.server.GrpcRouting;
import io.helidon.grpc.server.GrpcServer;
import io.helidon.grpc.server.ServiceDescriptor;
import io.helidon.microprofile.grpc.server.GrpcServiceBuilder;
import java.util.concurrent.TimeUnit;

/**
 * A utility class to deploy the Coherence gRPC proxy service into
 * a plain Helidon gRPC server without the use of CDI.
 *
 * @author Jonathan Knight  2019.11.29
 * @since 20.06
 */
public final class ServerHelper
    {
    /**
     * Create a {@link ServerHelper}.
     */
    public ServerHelper()
        {
        this(Scope.DEFAULT);
        }

    /**
     * Create a {@link ServerHelper} that uses the specified scope
     * name for the CCF that it creates.
     *
     * @param sScope  the scope name to use
     */
    public ServerHelper(String sScope)
        {
        f_sScope = sScope;
        }

    /**
     * Start the server.
     *
     * @throws Exception if an error occurs
     */
    protected void start() throws Exception
        {
        if (m_server != null && m_server.isRunning())
            {
            return;
            }

        System.setProperty("test.scope",            f_sScope);
        System.setProperty("coherence.ttl",         "0");
        System.setProperty("coherence.clustername", "NamedCacheServiceIT");
        System.setProperty("coherence.override",    "coherence-json-override.xml");
        System.setProperty("coherence.pof.config",  "test-pof-config.xml");

        DefaultCacheServer.startServerDaemon().waitForServiceStart();

        m_ccf = CacheFactory.getCacheFactoryBuilder()
                .getConfigurableCacheFactory("coherence-config.xml", null);

        m_channel = GrpcChannelsProvider.create(Config.empty()).channel("default");

        NamedCacheService.FixedCacheFactorySupplier ccfSupplier = new NamedCacheService.FixedCacheFactorySupplier(m_ccf);

        // Deploy the server side gRPC service into a plain Helidon gRPC server
        com.oracle.coherence.grpc.proxy.NamedCacheService service
                = com.oracle.coherence.grpc.proxy.NamedCacheService.builder()
                    .configurableCacheFactorySupplier(ccfSupplier)
                    .build();

        ServiceDescriptor descriptor = GrpcServiceBuilder.create(NamedCacheService.class, () -> service, null).build();
        GrpcRouting       routing    = GrpcRouting.builder().register(descriptor).build();
        m_server = GrpcServer.builder(routing)
                .build()
                .start()
                .toCompletableFuture()
                .get();
        }

    /**
     * Stop the server.
     */
    protected void shutdown()
        {
        if (m_channel != null)
            {
            ManagedChannel managedChannel = (ManagedChannel) m_channel;
            managedChannel.shutdownNow();
            try
                {
                managedChannel.awaitTermination(6, TimeUnit.SECONDS);
                }
            catch (InterruptedException ignored)
                {
                }
            }
        m_server.shutdown().toCompletableFuture().join();
        DefaultCacheServer.shutdown();
        }

    // ----- accessors ------------------------------------------------------

    protected ConfigurableCacheFactory getCCF()
        {
        return m_ccf;
        }

    protected Channel getChannel()
        {
        return m_channel;
        }

    // ----- data members ---------------------------------------------------

    protected final String f_sScope;

    protected ConfigurableCacheFactory m_ccf;

    protected Channel m_channel;

    protected GrpcServer m_server;
    }
