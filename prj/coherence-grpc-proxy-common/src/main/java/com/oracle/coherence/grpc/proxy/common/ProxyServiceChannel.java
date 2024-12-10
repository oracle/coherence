/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.proxy.common;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.Message;

import com.oracle.coherence.common.base.Logger;
import com.oracle.coherence.grpc.BinaryHelper;
import com.oracle.coherence.grpc.ErrorsHelper;
import com.oracle.coherence.grpc.GrpcService;
import com.oracle.coherence.grpc.GrpcServiceProtocol;
import com.oracle.coherence.grpc.LockingStreamObserver;
import com.oracle.coherence.grpc.SafeStreamObserver;

import com.oracle.coherence.grpc.messages.common.v1.Complete;
import com.oracle.coherence.grpc.messages.common.v1.ErrorMessage;
import com.oracle.coherence.grpc.messages.common.v1.HeartbeatMessage;
import com.oracle.coherence.grpc.messages.proxy.v1.InitRequest;
import com.oracle.coherence.grpc.messages.proxy.v1.InitResponse;
import com.oracle.coherence.grpc.messages.proxy.v1.ProxyRequest;
import com.oracle.coherence.grpc.messages.proxy.v1.ProxyResponse;

import com.tangosol.io.Serializer;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.Member;

import com.tangosol.util.SafeClock;
import com.tangosol.util.UUID;

import io.grpc.Status;

import io.grpc.StatusException;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * A bidirectional gRPC channel that handles requests for a gRPC proxy
 * and sends responses back down the channel.
 */
@SuppressWarnings("rawtypes")
public class ProxyServiceChannel
        implements StreamObserver<ProxyRequest>
    {
    /**
     * Create a {@link ProxyServiceChannel}.
     *
     * @param service   the parent {@link GrpcService}
     * @param observer  the {@link StreamObserver} to send responses to
     */
    public ProxyServiceChannel(GrpcService service, StreamObserver<ProxyResponse> observer)
        {
        this(service, observer, null);
        }

    /**
     * Create a {@link ProxyServiceChannel}.
     *
     * @param service         the parent {@link GrpcService}
     * @param observer        the {@link StreamObserver} to send responses to
     * @param memberSupplier  a {@link Supplier} to supply the local {@link Member}
     */
    protected ProxyServiceChannel(GrpcService service, StreamObserver<ProxyResponse> observer, Supplier<Member> memberSupplier)
        {
        f_service        = service;
        f_observer       = SafeStreamObserver.ensureSafeObserver(LockingStreamObserver.ensureLockingObserver(observer));
        f_memberSupplier = Objects.requireNonNullElse(memberSupplier, () -> CacheFactory.getCluster().getLocalMember());
        }

    // ----- StreamObserver methods -----------------------------------------

    @Override
    public void onNext(ProxyRequest request)
        {
        try
            {
            long                     nId         = request.getId();
            ProxyRequest.RequestCase requestCase = request.getRequestCase();

            switch (requestCase)
                {
                case INIT:
                    init(nId, request.getInit());
                    break;
                case HEARTBEAT:
                    if (request.getHeartbeat().getAck())
                        {
                        f_observer.onNext(ProxyResponse.newBuilder()
                                .setId(nId)
                                .setHeartbeat(HeartbeatMessage.getDefaultInstance())
                                .build());
                        }
                    break;
                case MESSAGE:
                    assertInit();
                    try
                        {
                        Message                 message  = request.getMessage().unpack(m_clzRequest);
                        StreamObserver<Message> observer = new ForwardingStreamObserver<>(nId);
                        m_protocol.onRequest(message, SafeStreamObserver.ensureSafeObserver(observer));
                        }
                    catch (Throwable e)
                        {
                        sendError(nId, e);
                        }
                    break;
                case REQUEST_NOT_SET:
                default:
                    throw new UnsupportedOperationException("Unsupported request type: " + requestCase);
                }
            }
        catch (Throwable t)
            {
            f_observer.onError(Status.INTERNAL
                    .withCause(t)
                    .asRuntimeException());
            if (m_protocol != null)
                {
                m_protocol.onError(t);
                }
            }
        }

    @Override
    public void onError(Throwable t)
        {
        f_observer.onError(t);
        if (m_protocol != null)
            {
            m_protocol.onError(t);
            }
        else
            {
            ErrorsHelper.logIfNotCancelled(t);
            }
        }

    @Override
    public void onCompleted()
        {
        if (m_protocol != null)
            {
            m_protocol.close();
            }
        }

    /**
     * Return the serializer being used by the protocol.
     *
     * @return the serializer being used by the protocol
     */
    public Serializer getSerializer()
        {
        return m_protocol.getSerializer();
        }

    // ----- helper methods ---------------------------------------------

    /**
     * Return an async wrapper around this channel.
     *
     * @param executor  the {@link Executor} to use to execute tasks
     *
     * @return an async wrapper around this channel
     */
    protected StreamObserver<ProxyRequest> async(Executor executor)
        {
        return new AsyncWrapper(executor, this);
        }

    /**
     * Initialise this channel.
     *
     * @param nId      the message id of the init request
     * @param request  the {@link InitRequest} to use for initialisation
     */
    @SuppressWarnings("unchecked")
    protected void init(long nId, InitRequest request)
        {
        f_lock.lock();
        try
            {
            if (m_clientUUID != null)
                {
                throw new IllegalStateException("The client connection is already initialized");
                }

            String sProtocol = request.getProtocol();
            m_protocol = loadProtocol(sProtocol)
                    .orElseThrow(() -> Status.FAILED_PRECONDITION
                            .withDescription("Failed to load proxy protocol " + sProtocol)
                            .asRuntimeException());

            int nVersion = getSupportedVersion(request);
            m_clzRequest = m_protocol.getRequestType();
            m_clientUUID = createClientUUID();

            m_protocol.init(f_service, request, nVersion, m_clientUUID, new ForwardingStreamObserver<>(0));

            Member       member   = f_memberSupplier.get();
            InitResponse response = InitResponse.newBuilder()
                    .setVersion(CacheFactory.VERSION)
                    .setEncodedVersion(CacheFactory.VERSION_ENCODED)
                    .setProtocolVersion(nVersion)
                    .setUuid(ByteString.copyFrom(m_clientUUID.toByteArray()))
                    .setProxyMemberId(member.getId())
                    .setProxyMemberUuid(ByteString.copyFrom(member.getUuid().toByteArray()))
                    .build();

            f_observer.onNext(ProxyResponse.newBuilder()
                    .setId(nId)
                    .setInit(response)
                    .build());
            }
        finally
            {
            f_lock.unlock();
            }
        }

    /**
     * Create a client {@link UUID}.
     *
     * @return the client {@link UUID}
     */
    private UUID createClientUUID()
        {
        SocketAddress address = ProxyServiceInterceptor.getRemoteAddress();
        if (address instanceof InetSocketAddress inetSocketAddress)
            {
            return new UUID(
                    SafeClock.INSTANCE.getSafeTimeMillis(),
                    inetSocketAddress.getAddress(),
                    inetSocketAddress.getPort(),
                    f_cClient.incrementAndGet());
            }
        return new UUID();
        }

    /**
     * Return the supported version of the protocol to use
     *
     * @param request  the {@link InitRequest}
     *
     * @return the supported version of the protocol
     */
    private int getSupportedVersion(InitRequest request)
        {
        String sProtocol         = request.getProtocol();
        int    nVersionClient    = request.getProtocolVersion();
        int    nMinVersionClient = request.getSupportedProtocolVersion();
        int    nVersionThis      = m_protocol.getVersion();
        int    nMinVersionThis   = m_protocol.getSupportedVersion();
        int    nVersion;

        if (nVersionThis == nVersionClient)
            {
            // both versions equal
            nVersion = nVersionThis;
            }
        else if (nVersionThis > nVersionClient && nVersionClient >= nMinVersionThis)
            {
            // server is a higher version but can support the client
            nVersion = nVersionClient;
            }
        else if (nVersionClient > nVersionThis && nVersionThis >= nMinVersionClient)
            {
            // client is a higher version but can support the server version
            nVersion = nVersionThis;
            }
        else
            {
            throw Status.FAILED_PRECONDITION
                    .withDescription("Cannot support protocol version for " + sProtocol
                            + " requested=" + nMinVersionClient + ".." + nVersionClient
                            + " supported=" + nMinVersionThis + ".." + nVersionThis)
                    .asRuntimeException();
            }
        return nVersion;
        }

    /**
     * Assert that this channel has received an {@link InitRequest}.
     *
     * @throws IllegalStateException if this channel has not been initialized
     */
    protected void assertInit()
        {
        if (m_clientUUID == null)
            {
            throw new IllegalStateException("The client connection has not been initialized");
            }
        }

    /**
     * Send an error message down the channel.
     *
     * @param nId     the identifier of the request that the error corresponds to
     * @param thrown  the error to send
     */
    protected void sendError(long nId, Throwable thrown)
        {
        String sMsg = thrown.getMessage();
        if (sMsg == null || sMsg.isEmpty())
            {
            sMsg = thrown.getClass().getSimpleName();
            }

        ErrorMessage.Builder builder = ErrorMessage.newBuilder()
                .setMessage(sMsg);

        Serializer serializer = m_protocol.getSerializer();
        if (serializer != null)
            {
            try
                {
                if (thrown instanceof StatusRuntimeException)
                    {
                    Throwable cause = thrown.getCause();
                    if (cause != null)
                        {
                        thrown = cause;
                        }
                    }
                if (thrown instanceof StatusException)
                    {
                    Throwable cause = thrown.getCause();
                    if (cause != null)
                        {
                        thrown = cause;
                        }
                    }
                builder.setError(BinaryHelper.toByteString(thrown, serializer));
                }
            catch (Throwable t)
                {
                Logger.err(t);
                }
            }

        f_observer.onNext(ProxyResponse.newBuilder()
                .setId(nId)
                .setError(builder.build())
                .build());
        }

    /**
     * Load the requested {@link GrpcServiceProtocol}.
     *
     * @param sProtocol  the name of the protocol to load
     *
     * @return  the requested protocol, or an empty optional if the protocol cannot be loaded
     */
    protected static Optional<GrpcServiceProtocol> loadProtocol(String sProtocol)
        {
        return ServiceLoader.load(GrpcServiceProtocol.class)
                .stream()
                .map(ServiceLoader.Provider::get)
                .filter(s -> sProtocol.equalsIgnoreCase(s.getProtocol()))
                .max(Comparator.comparingInt(GrpcServiceProtocol::getPriority))
                .stream()
                .findFirst();
        }

    // ----- inner class: AsyncWrapper --------------------------------------

    /**
     * An async wrapper around a {@link StreamObserver}.
     */
    public static class AsyncWrapper
            implements StreamObserver<ProxyRequest>
        {
        /**
         * Create an async wrapper.
         *
         * @param executor      the {@link Executor} to use to submit requests
         * @param channel  the wrapped {@link StreamObserver}
         */
        public AsyncWrapper(Executor executor, ProxyServiceChannel channel)
            {
            f_executor = executor;
            f_wrapped  = channel;
            }

        /**
         * Return the wrapped {@link ProxyServiceChannel}.
         *
         * @return the wrapped {@link ProxyServiceChannel}
         */
        public ProxyServiceChannel getWrapped()
            {
            return f_wrapped;
            }

        @Override
        public void onNext(ProxyRequest request)
            {
            f_executor.execute(() -> f_wrapped.onNext(request));
            }

        @Override
        public void onError(Throwable t)
            {
            f_wrapped.onError(t);
            }

        @Override
        public void onCompleted()
            {
            f_wrapped.onCompleted();
            }

        // ----- data members ---------------------------------------------------

        /**
         * The {@link Executor} to use to submit requests.
         */
        private final Executor f_executor;

        /**
         * The wrapped {@link ProxyServiceChannel}.
         */
        private final ProxyServiceChannel f_wrapped;
        }

    // ----- inner class: ForwardingStreamObserver --------------------------

    /**
     * A {@link StreamObserver} used to handle responses from the
     * {@link GrpcServiceProtocol} and convert them to a
     * {@link ProxyResponse} to send down the channel.
     *
     * @param <Resp> the type of the protocol responses
     */
    protected class ForwardingStreamObserver<Resp extends Message>
            implements StreamObserver<Resp>
        {
        /**
         * Create a {@link ForwardingStreamObserver}.
         *
         * @param nId  the identifier of the request message
         */
        public ForwardingStreamObserver(long nId)
            {
            m_nId = nId;
            }

        @Override
        public void onNext(Resp response)
            {
            f_observer.onNext(ProxyResponse.newBuilder()
                    .setId(m_nId)
                    .setMessage(Any.pack(response))
                    .build());
            }

        @Override
        public void onError(Throwable t)
            {
            sendError(m_nId, t);
            }

        @Override
        public void onCompleted()
            {
            f_observer.onNext(ProxyResponse.newBuilder()
                    .setId(m_nId)
                    .setComplete(Complete.getDefaultInstance())
                    .build());
            }

        // ----- data members -----------------------------------------------

        /**
         * The identifier of the request message.
         */
        private final long m_nId;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The lock to control thread safe access.
     */
    private static final Lock f_lock = new ReentrantLock();

    /**
     * The parent {@link GrpcService}.
     */
    private final GrpcService f_service;

    /**
     * The {@link StreamObserver} to send responses to.
     */
    private final StreamObserver<ProxyResponse> f_observer;

    /**
     * The {@link Supplier} tp use to obtain the local {@link Member}.
     */
    private final Supplier<Member> f_memberSupplier;

    /**
     * The {@link UUID} of the client.
     */
    private UUID m_clientUUID;

    /**
     * An identifier for the client.
     */
    private static final AtomicInteger f_cClient = new AtomicInteger();

    /**
     * The message protocol to use.
     */
    private GrpcServiceProtocol<Message, Message> m_protocol;

    /**
     * The type of the requests.
     */
    private Class<? extends Message> m_clzRequest;
    }
