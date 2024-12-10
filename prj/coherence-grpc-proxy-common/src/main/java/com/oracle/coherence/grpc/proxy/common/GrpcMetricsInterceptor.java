/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.proxy.common;

import io.grpc.ForwardingServerCall;
import io.grpc.ForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;

import java.util.Objects;

/**
 * A {@link ServerInterceptor} that enables capturing of gRPC metrics.
 *
 * @author Jonathan Knight  2020.10.14
 */
public class GrpcMetricsInterceptor
        implements ServerInterceptor
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a {@link GrpcMetricsInterceptor}.
     *
     * @param metrics the {@link GrpcProxyServiceMetrics} that tracks requests
     *
     * @throws NullPointerException if the metrics parameter is null
     */
    public GrpcMetricsInterceptor(GrpcProxyServiceMetrics metrics)
        {
        this(metrics, GrpcConnectionMetrics.getInstance());
        }

    /**
     * Create a {@link GrpcMetricsInterceptor}.
     *
     * @param metrics            the {@link GrpcProxyServiceMetrics} that tracks requests
     * @param connectionMetrics  the gRPC connection metrics.
     *
     * @throws NullPointerException if the either parameter is null
     */
    GrpcMetricsInterceptor(GrpcProxyServiceMetrics metrics, GrpcConnectionMetrics connectionMetrics)
        {
        f_metrics           = Objects.requireNonNull(metrics);
        f_connectionMetrics = Objects.requireNonNull(connectionMetrics);
        }

    // ----- ServerInterceptor methods --------------------------------------

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next)
        {
        f_connectionMetrics.register(call);

        ServerCall<ReqT, RespT> counting = new ResponseCountingServerCall<>(f_metrics, call);
        switch (call.getMethodDescriptor().getType())
            {
            case UNARY:
                return new RequestTimingCallListener<>(next.startCall(counting, headers), f_metrics);
            case CLIENT_STREAMING:
            case SERVER_STREAMING:
                ServerCall.Listener<ReqT> serverCall = next.startCall(counting, headers);
                return new StreamingTimingCallListener<>(serverCall, f_metrics);
            case BIDI_STREAMING:
                long                      nStart       = System.nanoTime();
                ServerCall.Listener<ReqT> bidiCall     = next.startCall(counting, headers);
                ServerCall.Listener<ReqT> bidiListener = new MessageTimingCallListener<>(bidiCall, f_metrics);
                long                      time         = System.nanoTime() - nStart;
                f_metrics.addRequestDuration(time);
                return bidiListener;
            case UNKNOWN:
            default:
                // unknown request type - no metrics.
                return next.startCall(call, headers);
            }
        }

    // ----- Object methods -------------------------------------------------

    @Override
    public boolean equals(Object o)
        {
        if (this == o)
            {
            return true;
            }
        return o != null && getClass() == o.getClass();
        }

    @Override
    public int hashCode()
        {
        return getClass().hashCode();
        }

    // ----- inner class: CountingServerCall -----------------------------------

    /**
     * A {@link SimpleForwardingServerCall} that counts
     * responses sent to the client.
     *
     * @param <ReqT>  the call request type
     * @param <RespT> the call response type
     */
    static class ResponseCountingServerCall<ReqT, RespT>
            extends ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT>
        {
        // ----- constructors -----------------------------------------------

        /**
         * Create a {@link ResponseCountingServerCall}.
         *
         * @param delegate the call to time
         * @param metrics  the gRPC metrics to update
         */
        ResponseCountingServerCall(GrpcProxyServiceMetrics metrics, ServerCall<ReqT, RespT> delegate)
            {
            super(delegate);
            f_metrics = metrics;
            }

        // ----- SimpleForwardingServerCall methods -------------------------

        @Override
        public void sendMessage(RespT message)
            {
            super.sendMessage(message);
            f_metrics.markSent();
            }

        @Override
        public void close(Status status, Metadata responseHeaders)
            {
            super.close(status, responseHeaders);
            if (status.getCode() == Status.Code.OK)
                {
                f_metrics.markSuccess();
                }
            else
                {
                f_metrics.markError();
                }
            }

        // ----- data members -----------------------------------------------

        /**
         * The {@link GrpcProxyServiceMetrics} to update with the call times.
         */
        protected final GrpcProxyServiceMetrics f_metrics;
        }

    // ----- inner class: MessageTimingCallListener -------------------------

    /**
     * A {@link SimpleForwardingServerCallListener} that counts and times messages
     * received from the client.
     *
     * @param <ReqT>  the request type
     */
    private static class MessageTimingCallListener<ReqT>
            extends ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>
        {
        public MessageTimingCallListener(ServerCall.Listener<ReqT> delegate, GrpcProxyServiceMetrics metrics)
            {
            super(delegate);
            f_metrics = metrics;
            }

        // ----- SimpleForwardingServerCallListener methods -----------------

        @Override
        public void onMessage(ReqT message)
            {
            long nStartNanos = System.nanoTime();
            super.onMessage(message);
            long nDuration = System.nanoTime() - nStartNanos;
            f_metrics.markReceived();
            f_metrics.addMessageDuration(nDuration);
            }

        // ----- data members -----------------------------------------------

        /**
         * The {@link GrpcProxyServiceMetrics} to update with the call times.
         */
        protected final GrpcProxyServiceMetrics f_metrics;
        }

    // ----- inner class: RequestTimingCallListener -------------------------

    /**
     * A {@link SimpleForwardingServerCallListener} that times requests and
     * counts and times messages received.
     *
     * @param <ReqT>  the request type
     */
    private static class RequestTimingCallListener<ReqT>
            extends MessageTimingCallListener<ReqT>
        {
        public RequestTimingCallListener(ServerCall.Listener<ReqT> delegate, GrpcProxyServiceMetrics metrics)
            {
            super(delegate, metrics);
            m_nStartNanos = System.nanoTime();
            }

        // ----- SimpleForwardingServerCallListener methods -----------------

        @Override
        public void onHalfClose()
            {
            m_nStartNanos = System.nanoTime();
            super.onHalfClose();
            }

        @Override
        public void onCancel()
            {
            super.onCancel();
            long time = System.nanoTime() - m_nStartNanos;
            f_metrics.addRequestDuration(time);
            }

        @Override
        public void onComplete()
            {
            super.onComplete();
            long time = System.nanoTime() - m_nStartNanos;
            f_metrics.addRequestDuration(time);
            }

        // ----- data members -----------------------------------------------

        /**
         * The method start time.
         */
        private long m_nStartNanos;
        }

    // ----- inner class: StreamingTimingCallListener -----------------------

    /**
     * A {@link SimpleForwardingServerCallListener} that counts and times server
     * streaming requests.
     *
     * @param <ReqT>  the request type
     */
    private static class StreamingTimingCallListener<ReqT>
            extends MessageTimingCallListener<ReqT>
        {
        public StreamingTimingCallListener(ServerCall.Listener<ReqT> delegate, GrpcProxyServiceMetrics metrics)
            {
            super(delegate, metrics);
            }

        // ----- SimpleForwardingServerCallListener methods -----------------

        @Override
        public void onHalfClose()
            {
            long nStartNanos = System.nanoTime();
            super.onHalfClose();
            long nDuration = System.nanoTime() - nStartNanos;
            f_metrics.addRequestDuration(nDuration);
            }
        }

    // ----- data members ---------------------------------------------------

    /**
     * The {@link GrpcProxyServiceMetrics} to update with request metrics.
     */
    private final GrpcProxyServiceMetrics f_metrics;

    /**
     * The gRPC connection metrics.
     */
    private final GrpcConnectionMetrics f_connectionMetrics;
    }
