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
 * A provider of named gRPC {@link Channel} instances.
 * <p>
 * Instances of this interface are loaded by the {@link ServiceLoader}
 * when building {@link GrpcSessionConfiguration} instances to provide
 * a {@link Channel} for the session. This allows application code to
 * provide channels specifically configured for a gRPC sessions.
 * For example, if a gRPC connection requires TLS configuration, this
 * can be configured in a custom {@link ChannelProvider}.
 *
 * @author Jonathan Knight  2020.12.15
 * @since 20.12
 */
public interface ChannelProvider
        extends Comparable<ChannelProvider>
    {
    /**
     * Return the {@link Channel}.
     *
     * @param sName  the name of the channel to get
     *
     * @return the {@link Channel}
     */
    Optional<Channel> getChannel(String sName);

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
    default int compareTo(ChannelProvider other)
        {
        return Integer.compare(getPriority(), other.getPriority());
        }

    // ----- constants --------------------------------------------------

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
