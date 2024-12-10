/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.proxy.common;

import com.oracle.coherence.common.base.Logger;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;

import static io.grpc.Metadata.ASCII_STRING_MARSHALLER;

/**
 * A gRPC server interceptor that will log messages on the server.
 * <p/>
 * This is useful for debugging but should not be used in production as it will log
 * the contents of messages.
 */
public class ServerLoggingInterceptor
        implements ServerInterceptor
    {
    public static final Metadata.Key<String> TRACE_ID_KEY = Metadata.Key.of("traceId", ASCII_STRING_MARSHALLER);

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next)
        {
        String                        sId        = headers.get(TRACE_ID_KEY);
        MethodDescriptor<ReqT, RespT> descriptor = call.getMethodDescriptor();
        Logger.info(() -> String.format("In interceptCall loggingId=%s method=%s", sId, descriptor.getFullMethodName()));

        GrpcServerCall<ReqT, RespT> grpcServerCall = new GrpcServerCall<>(call, sId);

        ServerCall.Listener<ReqT> listener = next.startCall(grpcServerCall, headers);

        return new GrpcForwardingServerCallListener<>(descriptor, listener)
            {

            @Override
            public void onMessage(ReqT message)
                {
                Logger.info(() -> String.format("In onMessage: loggingId=%s method=%s, message=%s", sId, m_sMethodName, message));
                super.onMessage(message);
                }

            @Override
            public void onHalfClose()
                {
                Logger.info(() -> String.format("In onHalfClose: loggingId=%s method=%s", sId, m_sMethodName));
                super.onHalfClose();
                }

            @Override
            public void onCancel()
                {
                Logger.info(() -> String.format("In onCancel: loggingId=%s method=%s", sId, m_sMethodName));
                super.onCancel();
                }

            @Override
            public void onComplete()
                {
                Logger.info(() -> String.format("In onComplete: loggingId=%s method=%s", sId, m_sMethodName));
                super.onComplete();
                }

            @Override
            public void onReady()
                {
                Logger.info(() -> String.format("In onReady: loggingId=%s method=%s", sId, m_sMethodName));
                super.onReady();
                }
            };
        }

    private static class GrpcServerCall<ReqT, RespT> extends ServerCall<ReqT, RespT>
        {

        private  final ServerCall<ReqT, RespT> m_serverCall;

        private final String m_sId;

        protected GrpcServerCall(ServerCall<ReqT, RespT> serverCall, String sId)
            {
            m_serverCall = serverCall;
            m_sId             = sId;
            }

        @Override
        public void request(int numMessages)
            {
            m_serverCall.request(numMessages);
            }

        @Override
        public void sendHeaders(Metadata headers)
            {
            m_serverCall.sendHeaders(headers);
            }

        @Override
        public void sendMessage(RespT message)
            {
            Logger.info(() -> String.format("In sendMessage: id=%s method=%s, response=%s", m_sId, m_serverCall.getMethodDescriptor().getFullMethodName(), message));
            m_serverCall.sendMessage(message);
            }

        @Override
        public void close(Status status, Metadata trailers)
            {
            m_serverCall.close(status, trailers);
            }

        @Override
        public boolean isCancelled()
            {
            return m_serverCall.isCancelled();
            }

        @Override
        public MethodDescriptor<ReqT, RespT> getMethodDescriptor()
            {
            return m_serverCall.getMethodDescriptor();
            }
        }

    private static class GrpcForwardingServerCallListener<ReqT> extends io.grpc.ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>
        {
        protected GrpcForwardingServerCallListener(MethodDescriptor<ReqT, ?> method, ServerCall.Listener<ReqT> listener)
            {
            super(listener);
            m_sMethodName = method.getFullMethodName();
            }

        // ----- data members ---------------------------------------------------

        protected final String m_sMethodName;
        }
    }
