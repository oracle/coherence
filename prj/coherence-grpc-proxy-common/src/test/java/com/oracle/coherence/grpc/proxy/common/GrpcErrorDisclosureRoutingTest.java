/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.proxy.common;

import com.google.protobuf.Message;
import com.oracle.coherence.grpc.ErrorsHelper;
import com.oracle.coherence.grpc.GrpcService;
import com.oracle.coherence.grpc.GrpcServiceProtocol;
import com.oracle.coherence.grpc.SafeStreamObserver;
import com.oracle.coherence.grpc.messages.cache.v0.MapListenerErrorResponse;
import com.oracle.coherence.grpc.messages.cache.v0.MapListenerResponse;
import com.oracle.coherence.grpc.messages.proxy.v1.InitRequest;
import com.oracle.coherence.grpc.messages.proxy.v1.ProxyResponse;
import com.oracle.coherence.grpc.proxy.common.v0.MapListenerProxy;
import com.oracle.coherence.grpc.proxy.common.v0.NamedCacheService;
import com.oracle.coherence.grpc.proxy.common.v0.ResponseHandlers;

import com.tangosol.application.Context;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.GrpcAcceptor;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.grpcAcceptor.GrpcConnection;
import com.tangosol.internal.util.DaemonPool;
import com.tangosol.io.DefaultSerializer;
import com.tangosol.io.Serializer;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.grpc.GrpcDependencies;
import com.tangosol.net.grpc.GrpcDiagnosticsPolicy;
import com.tangosol.net.messaging.Protocol;
import com.tangosol.util.UUID;

import io.grpc.BindableService;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.protobuf.services.HealthStatusManager;
import io.grpc.stub.StreamObserver;

import java.io.Closeable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionException;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for configured gRPC diagnostics routing.
 *
 * @author Aleks Seovic  2026.05.20
 * @since 26.07
 */
class GrpcErrorDisclosureRoutingTest
    {
    @Test
    void shouldApplySafeDisclosureToV0ResponseHandlerErrors()
        {
        CapturingObserver<String> observer = new CapturingObserver<>();
        StreamObserver<String>    safe     = SafeStreamObserver.ensureSafeObserver(observer,
                GrpcDiagnosticsPolicy.ERROR_DISCLOSURE_SAFE);

        ResponseHandlers.handleUnary(null, new IllegalStateException("secret token"), safe);

        StatusRuntimeException error = (StatusRuntimeException) observer.getError();
        assertEquals(Status.INTERNAL.getCode(), error.getStatus().getCode());
        assertEquals(ErrorsHelper.SAFE_INTERNAL_ERROR_MESSAGE, error.getStatus().getDescription());
        assertTrue(ErrorsHelper.getRemoteStack(error).isEmpty());
        }

    @Test
    void shouldApplySafeDisclosureToV1ProxyChannelErrors()
            throws Exception
        {
        CapturingObserver<ProxyResponse> observer = new CapturingObserver<>();
        TestProxyServiceChannel          channel  = new TestProxyServiceChannel(
                new TestGrpcService(GrpcDiagnosticsPolicy.ERROR_DISCLOSURE_SAFE), observer);

        setProtocol(channel, new TestProtocol());
        channel.send(7, new IllegalStateException("secret token"));

        ProxyResponse response = observer.getValues().get(0);
        assertEquals(7, response.getId());
        assertEquals(ErrorsHelper.SAFE_INTERNAL_ERROR_MESSAGE, response.getError().getMessage());
        assertFalse(response.getError().hasError());
        }

    @Test
    void shouldApplySafeDisclosureToV0MapListenerPayloadErrors()
        {
        TestMapListenerProxy       proxy = new TestMapListenerProxy(GrpcDiagnosticsPolicy.ERROR_DISCLOSURE_SAFE);
        MapListenerErrorResponse  error = proxy.error("listener-one", new IllegalStateException("secret token"));

        assertEquals("listener-one", error.getUid());
        assertEquals(Status.Code.FAILED_PRECONDITION.value(), error.getCode());
        assertEquals(ErrorsHelper.SAFE_INTERNAL_ERROR_MESSAGE, error.getMessage());
        assertEquals(0, error.getStackCount());
        }

    @Test
    void shouldMapDirectSecurityFailureInV0MapListenerPayload()
        {
        TestMapListenerProxy      proxy = new TestMapListenerProxy(GrpcDiagnosticsPolicy.ERROR_DISCLOSURE_DIAGNOSTIC);
        MapListenerErrorResponse error = proxy.error("listener-security-direct", new SecurityException("denied"));

        assertEquals("listener-security-direct", error.getUid());
        assertEquals(Status.Code.PERMISSION_DENIED.value(), error.getCode());
        assertEquals("denied", error.getMessage());
        assertTrue(error.getStackCount() > 0);
        }

    @Test
    void shouldMapWrappedSecurityFailureInSafeV0MapListenerPayload()
        {
        TestMapListenerProxy proxy = new TestMapListenerProxy(GrpcDiagnosticsPolicy.ERROR_DISCLOSURE_SAFE);
        MapListenerErrorResponse error = proxy.error("listener-security-wrapped",
                new CompletionException(new SecurityException("secret denial detail")));

        assertEquals("listener-security-wrapped", error.getUid());
        assertEquals(Status.Code.PERMISSION_DENIED.value(), error.getCode());
        assertEquals(ErrorsHelper.SAFE_INTERNAL_ERROR_MESSAGE, error.getMessage());
        assertEquals(0, error.getStackCount());
        }

    @Test
    void shouldPreserveInvalidArgumentV0MapListenerPayloadErrors()
        {
        TestMapListenerProxy      proxy = new TestMapListenerProxy(GrpcDiagnosticsPolicy.ERROR_DISCLOSURE_DIAGNOSTIC);
        MapListenerErrorResponse error = proxy.error("listener-invalid", new IllegalArgumentException("bad request"));

        assertEquals("listener-invalid", error.getUid());
        assertEquals(Status.Code.INVALID_ARGUMENT.value(), error.getCode());
        assertEquals("bad request", error.getMessage());
        assertTrue(error.getStackCount() > 0);
        }

    @Test
    void shouldPreserveBoundedStatusDescriptionForSafeV0MapListenerPayloadErrors()
        {
        TestMapListenerProxy       proxy = new TestMapListenerProxy(GrpcDiagnosticsPolicy.ERROR_DISCLOSURE_SAFE);
        MapListenerErrorResponse  error = proxy.error("listener-two",
                Status.INVALID_ARGUMENT.withDescription("bad listener request").asRuntimeException());

        assertEquals("listener-two", error.getUid());
        assertEquals(Status.Code.INVALID_ARGUMENT.value(), error.getCode());
        assertEquals("bad listener request", error.getMessage());
        assertEquals(0, error.getStackCount());
        }

    @Test
    void shouldPreserveDiagnosticV0MapListenerPayloadErrors()
        {
        TestMapListenerProxy      proxy = new TestMapListenerProxy(GrpcDiagnosticsPolicy.ERROR_DISCLOSURE_DIAGNOSTIC);
        MapListenerErrorResponse error = proxy.error("listener-three", new IllegalStateException("secret token"));

        assertEquals("listener-three", error.getUid());
        assertEquals(Status.Code.FAILED_PRECONDITION.value(), error.getCode());
        assertEquals("secret token", error.getMessage());
        assertTrue(error.getStackCount() > 0);
        }

    @Test
    void shouldGateChannelzButKeepHealth()
        {
        TestGrpcAcceptorController controller = new TestGrpcAcceptorController();
        HealthStatusManager        health     = new HealthStatusManager();

        com.tangosol.internal.net.service.peer.acceptor.DefaultGrpcAcceptorDependencies deps =
                new com.tangosol.internal.net.service.peer.acceptor.DefaultGrpcAcceptorDependencies();
        deps.setChannelz(GrpcDiagnosticsPolicy.CHANNELZ_DISABLED);

        List<BindableService> list = controller.bindables(deps, health);
        assertEquals(1, list.size());
        assertEquals("grpc.health.v1.Health", list.get(0).bindService().getServiceDescriptor().getName());

        deps.setChannelz(GrpcDiagnosticsPolicy.CHANNELZ_ENABLED);
        list = controller.bindables(deps, health);
        assertEquals(2, list.size());
        assertEquals("grpc.channelz.v1.Channelz", list.get(0).bindService().getServiceDescriptor().getName());
        assertEquals("grpc.health.v1.Health", list.get(1).bindService().getServiceDescriptor().getName());
        }

    private static void setProtocol(ProxyServiceChannel channel, GrpcServiceProtocol<?, ?> protocol)
            throws Exception
        {
        Field field = ProxyServiceChannel.class.getDeclaredField("m_protocol");
        field.setAccessible(true);
        field.set(channel, protocol);
        }

    private static class TestProxyServiceChannel
            extends ProxyServiceChannel
        {
        TestProxyServiceChannel(GrpcService service, StreamObserver<ProxyResponse> observer)
            {
            super(service, observer, () -> null);
            }

        void send(long nId, Throwable thrown)
            {
            sendError(nId, thrown);
            }
        }

    private static class TestGrpcService
            implements GrpcService
        {
        TestGrpcService(String sErrorDisclosure)
            {
            f_sErrorDisclosure = sErrorDisclosure;
            }

        @Override
        public ConfigurableCacheFactory getCCF(String sScope)
            {
            throw new UnsupportedOperationException();
            }

        @Override
        public Serializer getSerializer(String sFormat, ClassLoader loader)
            {
            throw new UnsupportedOperationException();
            }

        @Override
        public Dependencies getDependencies()
            {
            return new Dependencies()
                {
                @Override
                public Optional<Context> getContext()
                    {
                    return Optional.empty();
                    }

                @Override
                public String getErrorDisclosure()
                    {
                    return f_sErrorDisclosure;
                    }
                };
            }

        @Override
        public void addCloseable(Closeable closeable)
            {
            }

        @Override
        public void removeCloseable(Closeable closeable)
            {
            }

        @Override
        public GrpcAcceptor getGrpcAcceptor()
            {
            throw new UnsupportedOperationException();
            }

        private final String f_sErrorDisclosure;
        }

    private static class TestMapListenerProxy
            extends MapListenerProxy
        {
        TestMapListenerProxy(String sErrorDisclosure)
            {
            super((NamedCacheService) null, new CapturingObserver<>(), 0, sErrorDisclosure);
            }

        @Override
        public MapListenerErrorResponse error(String uid, Throwable t)
            {
            return super.error(uid, t);
            }
        }

    private static class TestProtocol
            implements GrpcServiceProtocol<Message, Message>
        {
        @Override
        public String getProtocol()
            {
            return "test";
            }

        @Override
        public int getVersion()
            {
            return 1;
            }

        @Override
        public int getSupportedVersion()
            {
            return 1;
            }

        @Override
        public Class<Message> getRequestType()
            {
            return Message.class;
            }

        @Override
        public Class<Message> getResponseType()
            {
            return Message.class;
            }

        @Override
        public Serializer getSerializer()
            {
            return new DefaultSerializer();
            }

        @Override
        public void init(GrpcService service, InitRequest request, int nVersion, UUID clientUUID,
                StreamObserver<Message> observer, GrpcConnection<Message> connection)
            {
            }

        @Override
        public void onRequest(Message request, StreamObserver<Message> observer)
            {
            }

        @Override
        public void close()
            {
            }

        @Override
        public Protocol[] getExtendProtocols()
            {
            return new Protocol[0];
            }
        }

    private static class TestGrpcAcceptorController
            extends BaseGrpcAcceptorController
        {
        List<BindableService> bindables(
                com.tangosol.internal.net.service.peer.acceptor.GrpcAcceptorDependencies dependencies,
                HealthStatusManager health)
            {
            return createBindableServices(dependencies, health);
            }

        @Override
        protected GrpcServiceDependencies createServiceDeps(GrpcServiceDependencies defaultDeps)
            {
            return defaultDeps;
            }

        @Override
        protected void startInternal(List<io.grpc.ServerServiceDefinition> listService, List<BindableService> listBindable)
            {
            }

        @Override
        protected void stopInternal()
            {
            }

        @Override
        public GrpcDependencies.ServerType getServerType()
            {
            return GrpcDependencies.ServerType.Asynchronous;
            }

        @Override
        public String getInProcessName()
            {
            return null;
            }

        @Override
        public int getLocalPort()
            {
            return -1;
            }

        @Override
        public void setDaemonPool(DaemonPool pool)
            {
            }
        }

    private static class CapturingObserver<T>
            implements StreamObserver<T>
        {
        @Override
        public void onNext(T value)
            {
            f_listValue.add(value);
            }

        @Override
        public void onError(Throwable t)
            {
            f_error = t;
            }

        @Override
        public void onCompleted()
            {
            f_fCompleted = true;
            }

        List<T> getValues()
            {
            return f_listValue;
            }

        Throwable getError()
            {
            return f_error;
            }

        @SuppressWarnings("unused")
        boolean isCompleted()
            {
            return f_fCompleted;
            }

        private final List<T> f_listValue = new ArrayList<>();

        private Throwable f_error;

        private boolean f_fCompleted;
        }
    }
