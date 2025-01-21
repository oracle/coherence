/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.internal.extend.proxy.serviceProxy;

import com.oracle.coherence.grpc.messages.cache.v1.NamedCacheResponse;

import com.tangosol.coherence.component.net.extend.proxy.GrpcExtendProxy;

import com.tangosol.coherence.component.net.extend.proxy.serviceProxy.CacheServiceProxy;

import com.tangosol.net.messaging.Channel;

public class GrpcCacheServiceProxy
        extends CacheServiceProxy
        implements GrpcExtendProxy<NamedCacheResponse>
    {

    @Override
    public Channel getChannel()
        {
        return m_channel;
        }

    public void setChannel(Channel channel)
        {
        m_channel = channel;
        }

    @Override
    public void registerChannel(Channel channel)
        {
        super.registerChannel(channel);
        m_channel = channel;
        }

    @Override
    public void unregisterChannel(Channel channel)
        {
        super.unregisterChannel(channel);
        m_channel = null;
        }

    // ----- data members ---------------------------------------------------

    private Channel m_channel;
    }
