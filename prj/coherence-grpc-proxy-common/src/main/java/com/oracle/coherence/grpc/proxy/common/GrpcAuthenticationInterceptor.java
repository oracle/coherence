/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.grpc.proxy.common;

import com.oracle.coherence.grpc.GrpcAuthentication;
import com.oracle.coherence.grpc.GrpcSecurityContext;

import com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.GrpcAcceptor;
import com.tangosol.internal.net.service.peer.acceptor.GrpcAcceptorDependencies;
import com.tangosol.net.security.UsernameAndPassword;

import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.ForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;

import javax.security.auth.Subject;

/**
 * A gRPC interceptor that authenticates configured Coherence gRPC services.
 *
 * @author Aleks Seovic  2026.05.20
 * @since 26.07
 */
public class GrpcAuthenticationInterceptor
        implements ServerInterceptor
    {
    /**
     * Create an auth interceptor.
     *
     * @param deps      the gRPC acceptor dependencies
     * @param acceptor  the gRPC acceptor
     */
    public GrpcAuthenticationInterceptor(GrpcAcceptorDependencies deps, GrpcAcceptor acceptor)
        {
        f_sAuthMethod = deps.getAuthMethod();
        f_acceptor    = acceptor;
        }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call,
            Metadata headers, ServerCallHandler<ReqT, RespT> next)
        {
        Subject subject = null;
        if (GrpcAuthentication.isAuthRequired(f_sAuthMethod))
            {
            UsernameAndPassword token = GrpcAuthentication.parseBasicCredentials(headers);
            GrpcAuthentication.validateBasicTransport(call.getAttributes());
            subject = GrpcAuthentication.assertSubject(token, f_acceptor::assertIdentityToken);
            }

        Context context = GrpcSecurityContext.withSubject(Context.current(), subject);
        ServerCall.Listener<ReqT> listener = Contexts.interceptCall(context, call, headers, next);
        return new SubjectAwareListener<>(listener, subject);
        }

    // ----- inner class: SubjectAwareListener -----------------------------

    private static class SubjectAwareListener<ReqT>
            extends ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>
        {
        protected SubjectAwareListener(ServerCall.Listener<ReqT> listener, Subject subject)
            {
            super(listener);
            f_subject = subject;
            }

        @Override
        public void onMessage(ReqT message)
            {
            GrpcSecurityContext.runAs(f_subject, () -> super.onMessage(message));
            }

        @Override
        public void onHalfClose()
            {
            GrpcSecurityContext.runAs(f_subject, super::onHalfClose);
            }

        @Override
        public void onCancel()
            {
            GrpcSecurityContext.runAs(f_subject, super::onCancel);
            }

        @Override
        public void onComplete()
            {
            GrpcSecurityContext.runAs(f_subject, super::onComplete);
            }

        @Override
        public void onReady()
            {
            GrpcSecurityContext.runAs(f_subject, super::onReady);
            }

        private final Subject f_subject;
        }

    // ----- data members --------------------------------------------------

    private final String f_sAuthMethod;

    private final GrpcAcceptor f_acceptor;
    }
