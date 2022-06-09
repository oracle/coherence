/*
 * Copyright (c) 2020, 2022 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.client;

import com.tangosol.internal.net.SystemSessionConfiguration;

import com.tangosol.net.Coherence;
import com.tangosol.net.SessionConfiguration;

/**
 * A {@link GrpcSessionProvider} for the gRPC client side system session.
 * <p>
 * This provider will create a gRPC client session as the system session
 * if the Coherence mode is {@link Coherence.Mode#Client}.
 *
 * @author Jonathan Knight  2020.12.16
 * @since 20.12
 */
public class ClientSystemSessionProvider
        implements GrpcSessionProvider
    {
    @Override
    public int getPriority()
        {
        // Must have a higher priority than the default provider
        return SystemSessionConfiguration.PROVIDER_PRIORITY + 1;
        }

    @Override
    public Context createSession(SessionConfiguration configuration, Context context)
        {
        // we only add this System session on a gRPC client
        if (context.getMode() != Coherence.Mode.ClusterMember
                && Coherence.SYSTEM_SESSION.equals(configuration.getName()))
            {
            GrpcSessionConfiguration cfg = GrpcSessionConfiguration
                    .builder(ChannelProvider.SYSTEM_CHANNEL_NAME, ChannelProvider.DEFAULT_CHANNEL_NAME)
                    .named(Coherence.SYSTEM_SESSION)
                    .withScopeName(Coherence.SYSTEM_SCOPE)
                    .build();

            return context.createSession(cfg);
            }
        return context;
        }
    }
