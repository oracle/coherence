/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.client;

import io.grpc.Channel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;

/**
 * A provider of gRPC channels.
 * <p>
 * Named {@link Channel} instances are provided by instances of
 * {@link ChannelProvider} discovered by the {@link ServiceLoader}
 * or manually added to this provider.
 * <p>
 * When providing a {@link Channel} the registered or discovered providers
 * will be tried in {@link ChannelProvider#getPriority() priority} order.
 * <p>
 * The providers are lazilly loaded the first time a channel is required.
 *
 * @author Jonathan Knight  2020.12.17
 * @since 20.12
 */
public class ChannelProviders
    {
    /**
     * Protected constructor as this is a typically a
     * singleton accessed via {@link #INSTANCE}.
     */
    protected ChannelProviders()
        {
        }

    /**
     * Find a {@link Channel} from one of the channel providers.
     *
     * @param sName the name of the channel
     *
     * @return the {@link Channel} instance or an empty {@link Optional}
     *         if no provider exists with the specified name
     */
    public Optional<Channel> findChannel(String sName)
        {
        for (ChannelProvider provider : ensureChannelProviders())
            {
            Optional<Channel> channel = provider.getChannel(sName);
            if (channel.isPresent())
                {
                return channel;
                }
            }
        return Optional.empty();
        }

    /**
     * Add a {@link ChannelProvider} to the registry.
     *
     * @param provider the provider to add
     *
     * @return this {@link ChannelProvider}
     */
    public ChannelProviders add(ChannelProvider provider)
        {
        if (provider != null)
            {
            List<ChannelProvider> list = ensureChannelProviders();
            list.add(provider);
            list.sort(Comparator.reverseOrder());
            }
        return this;
        }

    /**
     * Ensure that the {@link #m_providers} list is populated with discovered {@link ChannelProvider} instances.
     *
     * @return the discovered {@link ChannelProvider} instances.
     */
    private synchronized List<ChannelProvider> ensureChannelProviders()
        {
        if (m_providers == null)
            {
            List<ChannelProvider> list = new ArrayList<>();
            ServiceLoader<ChannelProvider> loader = ServiceLoader.load(ChannelProvider.class);
            for (ChannelProvider provider : loader)
                {
                list.add(provider);
                }
            list.sort(Comparator.reverseOrder());
            m_providers = list;
            }
        return m_providers;
        }

    // ----- constants ------------------------------------------------------

    /**
     * The singleton instance of {@link ChannelProviders}.
     */
    public static final ChannelProviders INSTANCE = new ChannelProviders();

    // ----- data members ---------------------------------------------------

    /**
     * The list of ordered {@link ChannelProvider} instances.
     */
    private List<ChannelProvider> m_providers;
    }