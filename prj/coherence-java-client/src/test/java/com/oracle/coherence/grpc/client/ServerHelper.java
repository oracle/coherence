/*
 * Copyright (c) 2020 Oracle and/or its affiliates. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.oracle.coherence.grpc.client;

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
 * @since 14.1.2
 */
public final class ServerHelper
    {
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

        System.setProperty("coherence.ttl",         "0");
        System.setProperty("coherence.clustername", "NamedCacheServiceIT");
        System.setProperty("coherence.override",    "coherence-json-override.xml");
        System.setProperty("coherence.pof.config",  "test-pof-config.xml");

        DefaultCacheServer.startServerDaemon().waitForServiceStart();

        m_ccf = CacheFactory.getCacheFactoryBuilder()
                .getConfigurableCacheFactory("coherence-cache-config.xml", null);

        m_channel = GrpcChannelsProvider.create(Config.empty()).channel("default");

        // Deploy the server side gRPC service into a plain Helidon gRPC server
        com.oracle.coherence.grpc.proxy.NamedCacheService service = com.oracle.coherence.grpc.proxy.NamedCacheService.create();

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
        m_server.shutdown();
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

    protected ConfigurableCacheFactory m_ccf;

    protected Channel m_channel;

    protected GrpcServer m_server;
    }
