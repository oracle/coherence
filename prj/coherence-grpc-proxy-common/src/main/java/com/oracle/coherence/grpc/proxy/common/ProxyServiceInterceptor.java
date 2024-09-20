/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.proxy.common;

import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Grpc;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;

import java.net.SocketAddress;

/**
 * An interceptor for the proxy service.
 */
public class ProxyServiceInterceptor
        implements ServerInterceptor
    {
    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next)
        {
        SocketAddress address = call.getAttributes().get(Grpc.TRANSPORT_ATTR_REMOTE_ADDR);
        Context       context = Context.current().withValue(KEY_REMOTE_ADDRESS, address);
        return Contexts.interceptCall(context, call, headers, next);
        }

    /**
     * Return the remote address for the current call.
     *
     * @return the remote address for the current call
     */
    public static SocketAddress getRemoteAddress()
        {
        return KEY_REMOTE_ADDRESS.get();
        }

    // ----- constants ------------------------------------------------------

    /**
     * The context key name for the client remote address.
     */
    private static final String KEY_NAME_REMOTE_ADDRESS = "com.oracle.coherence.remoteAddress";

    /**
     * The context key for the client remote address.
     */
    public static final Context.Key<SocketAddress> KEY_REMOTE_ADDRESS = Context.key(KEY_NAME_REMOTE_ADDRESS);
    }
