/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.client.common;

import com.oracle.coherence.common.base.Logger;
import io.grpc.Attributes;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Status;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static io.grpc.Metadata.ASCII_STRING_MARSHALLER;

/**
 * A gRPC client interceptor that will log messages on the client.
 * <p/>
 * This is useful for debugging but should not be used in production as it will log
 * the contents of messages.
 */
public class ClientLoggingInterceptor
        implements ClientInterceptor
    {
    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method,
            CallOptions options, Channel next)
        {
        return new ClientLoggingForwardingClientCall<>(method,
                next.newCall(method, options.withDeadlineAfter(10000, TimeUnit.MILLISECONDS)))
            {

            @Override
            public void sendMessage(ReqT message)
                {
                Logger.info(() -> String.format("In sendMessage method=%s, message=%s", method, message));
                super.sendMessage(message);
                }

            @Override
            public void start(Listener<RespT> responseListener, Metadata headers)
                {
                headers.put(TRACE_ID_KEY, String.valueOf(TRACE_ID.getAndIncrement()));

                Logger.info(() -> String.format("In start method=%s", method));

                ClientLoggingListener<RespT> backendListener = new ClientLoggingListener<>(methodName, responseListener);
                super.start(backendListener, headers);
                }

            @Override
            public void request(int numMessages)
                {
                Logger.info(() -> String.format("In request method=%s", method));
                super.request(numMessages);
                }

            @Override
            public void cancel(String message, Throwable cause)
                {
                Logger.info(() -> String.format("In cancel method=%s", method));
                super.cancel(message, cause);
                }

            @Override
            public void halfClose()
                {
                Logger.info(() -> String.format("In halfClose method=%s", method));
                super.halfClose();
                }

            @Override
            public void setMessageCompression(boolean enabled)
                {
                Logger.info(() -> String.format("In setMessageCompression method=%s", method));
                super.setMessageCompression(enabled);
                }

            @Override
            public boolean isReady()
                {
                Logger.info(() -> String.format("In isReady method=%s", method));
                return super.isReady();
                }

            @Override
            public Attributes getAttributes()
                {
                Logger.info(() -> String.format("In getAttributes method=%s", method));
                return super.getAttributes();
                }
            };
        }


    private static class ClientLoggingListener<RespT> extends ClientCall.Listener<RespT>
        {

        String                     methodName;
        ClientCall.Listener<RespT> responseListener;

        protected ClientLoggingListener(String methodName, ClientCall.Listener<RespT> responseListener)
            {
            super();
            this.methodName       = methodName;
            this.responseListener = responseListener;
            }

        @Override
        public void onMessage(RespT message)
            {
            Logger.info(() -> String.format("In onMessage method=%s, message=%s", methodName, message));
            responseListener.onMessage(message);
            }

        @Override
        public void onHeaders(Metadata headers)
            {
            responseListener.onHeaders(headers);
            }

        @Override
        public void onClose(Status status, Metadata trailers)
            {
            responseListener.onClose(status, trailers);
            }

        @Override
        public void onReady()
            {
            responseListener.onReady();
            }
        }

    private static class ClientLoggingForwardingClientCall<ReqT, RespT> extends io.grpc.ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>
        {

        String methodName;

        protected ClientLoggingForwardingClientCall(MethodDescriptor<ReqT, RespT> method, ClientCall<ReqT, RespT> delegate)
            {
            super(delegate);
            methodName = method.getFullMethodName();
            }
        }

    // ----- constants ------------------------------------------------------

    public static final Metadata.Key<String> TRACE_ID_KEY = Metadata.Key.of("loggingId", ASCII_STRING_MARSHALLER);

    private static final AtomicLong TRACE_ID = new AtomicLong();
    }
