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
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;

import java.util.concurrent.ConcurrentHashMap;

/**
 * A named provider of gRPC {@link io.grpc.Channel} instances.
 * <p>
 * Instances of this interface are loaded by the {@link ServiceLoader}
 * when building {@link GrpcSessionConfiguration} instances to provide
 * a {@link Channel} for the session. This allows application code to
 * provide channels specifically configured for a gRPC sessions.
 * For example, if a gRPC connection requires TLS configuration, this
 * can be configured in a custom {@link NamedChannelProvider}.
 *
 * @author Jonathan Knight  2020.12.15
 * @since 20.12
 */
public interface NamedChannelProvider
        extends Comparable<NamedChannelProvider>
    {
    /**
     * Return the name of this provider.
     *
     * @return the name of this provider
     */
    String getName();

    /**
     * Return the {@link Channel}.
     * @return the {@link Channel}
     */
    Channel getChannel();

    /**
     * Returns the priority for this provider.
     * <p>
     * Sessions will be created in priority order, highest
     * priority first.
     * <p>
     * The default priority is zero (see {@link #DEFAULT_PRIORITY}.
     *
     * @return  the priority for this provider
     */
    default int getPriority()
        {
        return DEFAULT_PRIORITY;
        }

    @Override
    default int compareTo(NamedChannelProvider other)
        {
        return Integer.compare(getPriority(), other.getPriority());
        }

    // ----- inner class: Registry ------------------------------------------

    /**
     * A registry of discovered {@link NamedChannelProvider} instances.
     */
    class Registry
        {
        /**
         * Find a {@link Channel} from a specific {@link NamedChannelProvider}.
         *
         * @param sName  the name of the provider
         *
         * @return the {@link Channel} instance or an empty {@link Optional} if
         *         no provider exists with the specified name
         */
        public Optional<Channel> findChannel(String sName)
            {
            List<NamedChannelProvider> list = ensureChannelProviders().get(sName);
            if (list == null)
                {
                return Optional.empty();
                }
            for (NamedChannelProvider provider : list)
                {
                Channel channel = provider.getChannel();
                if (channel != null)
                    {
                    return Optional.of(channel);
                    }
                }
            return Optional.empty();
            }

        /**
         * Add a {@link NamedChannelProvider} to the registry.
         *
         * @param provider  the provider to add
         *
         * @return this {@link Registry}
         */
        public Registry add(NamedChannelProvider provider)
            {
            if (provider != null)
                {
                String sName = provider.getName();
                if (sName != null)
                    {
                    Map<String, List<NamedChannelProvider>> map = ensureChannelProviders();
                    List<NamedChannelProvider> list = map.computeIfAbsent(sName, k -> new ArrayList<>());
                    list.add(provider);
                    list.sort(Comparator.reverseOrder());
                    }
                }

            return this;
            }

        /**
         * Remove all providers for the specified name.
         *
         * @param sName  the name of the providers to remove
         *
         * @return this {@link Registry}
         */
        public Registry removeAll(String sName)
            {
            if (sName != null && m_providers != null)
                {
                m_providers.remove(sName);
                }
            return this;
            }

        /**
         * Ensure that the {@link #m_providers} map is populated with discovered
         * {@link NamedChannelProvider} instances.
         *
         * @return the discovered {@link NamedChannelProvider} instances.
         */
        private synchronized Map<String, List<NamedChannelProvider>> ensureChannelProviders()
            {
            if (m_providers == null)
                {
                Map<String, List<NamedChannelProvider>> map = new ConcurrentHashMap<>();
                ServiceLoader<NamedChannelProvider> loader = ServiceLoader.load(NamedChannelProvider.class);
                for (NamedChannelProvider provider : loader)
                    {
                    String sName = provider.getName();
                    if (sName != null)
                        {
                        List<NamedChannelProvider> list = map.computeIfAbsent(sName, k -> new ArrayList<>());
                        list.add(provider);
                        list.sort(Comparator.reverseOrder());
                        }
                    }
                m_providers = map;
                }
            return m_providers;
            }

        private Map<String, List<NamedChannelProvider>> m_providers;
        }

    // ----- constants --------------------------------------------------

    Registry REGISTRY = new Registry();

    /**
     * The default channel name.
     */
    String DEFAULT_CHANNEL_NAME = "default";

    /**
     * The System channel name.
     */
    String SYSTEM_CHANNEL_NAME = "system";

    /**
     * The default priority for a configuration.
     * @see #getPriority()
     */
    int DEFAULT_PRIORITY = 0;
    }
