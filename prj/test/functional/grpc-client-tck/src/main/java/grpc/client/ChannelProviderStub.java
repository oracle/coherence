/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package grpc.client;

import com.oracle.coherence.grpc.client.common.ChannelProvider;
import io.grpc.Channel;

import java.util.Optional;

public class ChannelProviderStub
        implements ChannelProvider
    {
    @Override
    public Optional<Channel> getChannel(String sName)
        {
        return Optional.ofNullable(s_channel);
        }

    public static void setChannel(Channel channel)
        {
        s_channel = channel;
        }

    // ----- data members ---------------------------------------------------

    private static Channel s_channel;
    }
