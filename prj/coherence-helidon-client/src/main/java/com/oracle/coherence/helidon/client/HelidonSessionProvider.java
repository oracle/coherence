/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.helidon.client;


import com.oracle.coherence.client.GrpcSessionConfiguration;
import com.oracle.coherence.client.GrpcSessionProvider;

import com.oracle.coherence.cdi.Name;

import com.tangosol.io.Serializer;

import com.tangosol.net.Coherence;
import com.tangosol.net.SessionConfiguration;

import io.grpc.Channel;

import io.helidon.config.Config;

import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;

import java.util.Map;
import java.util.Optional;

/**
 * An implementation of a {@link GrpcSessionProvider} that uses Helidon
 * Microprofile configuration and gRPC integration to configure and
 * provider a {@link GrpcSessionConfiguration} for a given session.
 *
 * @author Jonathan Knight  2020.12.17
 * @since 20.12
 */
public class HelidonSessionProvider
        implements GrpcSessionProvider
    {
    /**
     * Must have a public default constructor because this class will be discovered
     * by the {@link com.oracle.coherence.client.GrpcSessions} factory class
     * using the {@link java.util.ServiceLoader}.
     */
    public HelidonSessionProvider()
        {
        }

    // ----- GrpcSessionProvider methods ------------------------------------

    @Override
    public int getPriority()
        {
        return GrpcSessionProvider.PRIORITY + 1;
        }

    @Override
    public Context createSession(SessionConfiguration configuration, Context context)
        {
        return getBeanManager()
                .map(cdi -> createSession(cdi, configuration, context))
                .orElse(context);
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Create a {@link com.tangosol.net.Session} by looking up the configuration
     * of the session in the Helidon Microprofile {@link Config}.
     *
     * @param beanManager    the CDI {@link BeanManager}
     * @param configuration  the requested session configuration
     * @param context        the session provider context
     *
     * @return the completed {@link Context} containing the session or an incomplete
     *         {@link Context} if this provider could not create the session.
     */
    Context createSession(BeanManager beanManager, SessionConfiguration configuration, Context context)
        {
        CoherenceConfigHelper configHelper     = new CoherenceConfigHelper(beanManager);
        Map<String, Config>   mapSessionConfig = configHelper.getSessions();
        String                sName            = configuration.getName();
        Config                sessionConfig    = mapSessionConfig.get(sName);

        if (sessionConfig == null && Coherence.DEFAULT_NAME.equals(sName))
            {
            sessionConfig = mapSessionConfig.get("default");
            }
        else if (sessionConfig == null && Coherence.SYSTEM_SESSION.equals(sName))
            {
            sessionConfig = mapSessionConfig.get("system");
            }

        if (sessionConfig != null)
            {
            String sType = sessionConfig.get("type").asString().orElse("grpc");
            if ("grpc".equals(sType))
                {
                String sScope = sessionConfig.get("scope").asString().orElse(configuration.getScopeName());
                String sChannelName = sessionConfig.get("channel").asString().orElse("default");

                GrpcSessionConfiguration.Builder builder = beanManager.createInstance()
                        .select(Channel.class, GrpcChannelLiteral.of(sChannelName))
                        .stream()
                        .findFirst()
                        .map(GrpcSessionConfiguration::builder)
                        .orElse(GrpcSessionConfiguration.builder(sChannelName));

                builder.named(sName)
                       .withScopeName(sScope);

                if (configuration instanceof GrpcSessionConfiguration)
                    {
                    GrpcSessionConfiguration grpcConfig = (GrpcSessionConfiguration) configuration;
                    builder.withSerializer(grpcConfig.getSerializer().orElse(null),
                                           grpcConfig.getFormat().orElse(null))
                            .withTracing(grpcConfig.enableTracing())
                            .withPriority(grpcConfig.getPriority());
                    }

                sessionConfig.get("serializer").asString()
                        .ifPresent(sFormat -> {
                        Serializer serializer = beanManager.createInstance()
                                .select(Serializer.class, Name.Literal.of(sFormat))
                                .stream()
                                .findFirst()
                                .orElse(null);
                        builder.withSerializer(serializer, sFormat);
                        });

                GrpcSessionConfiguration grpcSessionConfig = builder.build();
                return context.createSession(grpcSessionConfig);
                }
            }
        return context;
        }

    /**
     * Returns the {@link BeanManager} from the current CDI environment.
     *
     * @return the {@link BeanManager} from the current CDI environment
     *         or an empty {@link Optional} if not running in CDI.
     */
    Optional<BeanManager> getBeanManager()
        {
        try
            {
            return Optional.of(CDI.current().getBeanManager());
            }
        catch (IllegalStateException e)
            {
            return Optional.empty();
            }
        }
    }
