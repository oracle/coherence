/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.client;

import io.grpc.ManagedChannel;

import io.helidon.config.Config;

import io.helidon.grpc.client.GrpcChannelDescriptor;
import io.helidon.grpc.client.GrpcChannelsProvider;
import io.helidon.grpc.core.GrpcTlsDescriptor;

/**
 * A builder of gRPC {@link ManagedChannel}s.
 * <p>
 * This class is a temporary work around for the fact that the
 * CDI Channel producer in Helidon 2.0.0 was broken.
 *
 * @author Jonathan Knight  2020.06.23
 * @since 20.06
 */
public class GrpcChannelBuilder
    {
    // ----- constructors ---------------------------------------------------

    private GrpcChannelBuilder(GrpcChannelsProvider provider)
        {
        m_provider = provider;
        }

    // ----- ChannelBuilder methods -----------------------------------------

    public static GrpcChannelBuilder create()
        {
        return builder().build();
        }

    public static GrpcChannelBuilder create(Config config)
        {
        return builder(config).build();
        }

    public static Builder builder()
        {
        return builder(Config.empty());
        }

    public static Builder builder(Config config)
        {
        return new Builder(config);
        }

    public ManagedChannel channel(String sName)
        {
        return m_provider.channel(sName);
        }

    // ----- helper methods -------------------------------------------------

    GrpcChannelsProvider getProvider()
        {
        return m_provider;
        }


    // ----- inner class: Builder -------------------------------------------

    /**
     * A builder to build a {@link GrpcChannelBuilder}.
     */
    public static class Builder
        {
        Builder(Config config)
            {
            m_config = config;
            }

        public GrpcChannelBuilder build()
            {
            GrpcChannelsProvider.Builder builder = GrpcChannelsProvider.builder()
                    .channel(GrpcChannelsProvider.DEFAULT_CHANNEL_NAME, GrpcChannelDescriptor.builder().build());

            Config cfgGrpc     = m_config.get("grpc");
            Config cfgChannels = cfgGrpc.get(GrpcChannelsProvider.CFG_KEY_CHANNELS);

            if (cfgChannels.exists())
                {
                for (Config cfg : cfgChannels.asNodeList().get())
                    {
                    Config cfgName = cfg.get("name");
                    String key = cfgName.exists() ? cfgName.asString().get() : cfg.key().name();
                    GrpcChannelDescriptor.Builder descriptorBuilder = GrpcChannelDescriptor.builder()
                            .host(cfg.get("host").asString().orElse(GrpcChannelsProvider.DEFAULT_HOST))
                            .port(cfg.get("port").asInt().orElse(GrpcChannelsProvider.DEFAULT_PORT));

                    cfg.get("target").asString().ifPresent(descriptorBuilder::target);
                    cfg.get("loadBalancerPolicy").asString().ifPresent(descriptorBuilder::loadBalancerPolicy);

                    if (cfg.get("inProcess").asBoolean().orElse(false))
                        {
                        descriptorBuilder.inProcess();
                        }

                    Config tlsConfig = cfg.get("tls");
                    if (tlsConfig.exists())
                        {
                        descriptorBuilder.sslDescriptor(GrpcTlsDescriptor.builder(tlsConfig).build());
                        }

                    builder.channel(key, descriptorBuilder.build());
                    }
                }

            return new GrpcChannelBuilder(builder.build());
            }

        private final Config m_config;
        }

    // ----- data members ---------------------------------------------------

    private final GrpcChannelsProvider m_provider;
    }
