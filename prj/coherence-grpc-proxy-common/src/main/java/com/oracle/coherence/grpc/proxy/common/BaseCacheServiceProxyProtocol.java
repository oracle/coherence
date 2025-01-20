/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.proxy.common;

import com.google.protobuf.Message;
import com.oracle.coherence.grpc.GrpcService;
import com.oracle.coherence.grpc.internal.extend.proxy.serviceProxy.GrpcCacheServiceProxy;
import com.oracle.coherence.grpc.messages.proxy.v1.InitRequest;
import com.tangosol.coherence.component.net.extend.protocol.CacheServiceProtocol;
import com.tangosol.coherence.component.net.extend.proxy.GrpcExtendProxy;
import com.tangosol.coherence.component.net.extend.proxy.serviceProxy.CacheServiceProxy;
import com.tangosol.io.Serializer;
import com.tangosol.net.ExtensibleConfigurableCacheFactory;
import com.tangosol.net.messaging.Protocol;
import com.tangosol.util.UUID;

/**
 * A base class for cache based {@link BaseProxyProtocol} implementations.
 *
 * @param <Req>   the request type
 * @param <Resp>  the response type
 *
 * @author Jonathan Knight  2025.01.25
 */
public abstract class BaseCacheServiceProxyProtocol<Req extends Message, Resp extends Message>
        extends BaseProxyProtocol<Req, Resp>
    {
    @Override
    protected GrpcExtendProxy<Resp> initInternal(GrpcService service, InitRequest request, int nVersion,
            UUID clientUUID)
        {
        String                             sScope     = request.getScope();
        String                             sFormat    = request.getFormat();
        ExtensibleConfigurableCacheFactory eccf       = (ExtensibleConfigurableCacheFactory) service.getCCF(sScope);
        Serializer                         serializer = service.getSerializer(sFormat, eccf.getConfigClassLoader());
        m_proxy = new GrpcCacheServiceProxy();
        m_proxy.setCacheFactory(eccf);
        m_proxy.setSerializer(serializer);
        return null;
        }

    @Override
    public Protocol[] getExtendProtocols()
        {
        return new Protocol[]{CacheServiceProtocol.getInstance()};
        }

    @Override
    public long getObserverId(long nId, Resp request)
        {
        return 0L;
        }

    // ----- data members ---------------------------------------------------
    
    /**
     * The {@link CacheServiceProxy}.
     */
    protected GrpcCacheServiceProxy m_proxy;
    }
