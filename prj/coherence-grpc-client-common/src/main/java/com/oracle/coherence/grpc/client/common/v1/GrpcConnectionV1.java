/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.client.common.v1;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.Message;

import com.oracle.coherence.common.base.Logger;
import com.oracle.coherence.common.base.Predicate;

import com.oracle.coherence.common.collections.ConcurrentHashMap;

import com.oracle.coherence.grpc.BinaryHelper;
import com.oracle.coherence.grpc.ErrorsHelper;
import com.oracle.coherence.grpc.LockingStreamObserver;
import com.oracle.coherence.grpc.SafeStreamObserver;

import com.oracle.coherence.grpc.client.common.GrpcConnection;

import com.oracle.coherence.grpc.messages.common.v1.ErrorMessage;
import com.oracle.coherence.grpc.messages.common.v1.HeartbeatMessage;

import com.oracle.coherence.grpc.messages.proxy.v1.InitRequest;
import com.oracle.coherence.grpc.messages.proxy.v1.InitResponse;
import com.oracle.coherence.grpc.messages.proxy.v1.ProxyRequest;
import com.oracle.coherence.grpc.messages.proxy.v1.ProxyResponse;
import com.oracle.coherence.grpc.services.proxy.v1.ProxyServiceGrpc;

import com.tangosol.internal.net.grpc.RemoteGrpcServiceDependencies;

import com.tangosol.internal.util.Daemons;
import com.tangosol.io.Serializer;

import com.tangosol.net.Coherence;
import com.tangosol.net.PriorityTask;
import com.tangosol.net.RequestIncompleteException;

import com.tangosol.net.grpc.GrpcDependencies;
import com.tangosol.util.SafeClock;
import com.tangosol.util.UUID;

import io.grpc.Channel;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import java.util.concurrent.atomic.AtomicLong;

import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * The version 1 {@link GrpcConnection} implementation.
 */
public class GrpcConnectionV1
        implements GrpcConnection, StreamObserver<ProxyResponse>
    {
    /**
     * Create a {@link GrpcConnectionV1}.
     *
     * @param dependencies  the dependencies to use
     * @param type          the expected response message type
     *
     * @throws NullPointerException if the expected response type is {@code null}
     */
    public GrpcConnectionV1(Dependencies dependencies, Class<? extends Message> type)
        {
        RemoteGrpcServiceDependencies serviceDependencies = dependencies.getServiceDependencies();

        f_responseType      = Objects.requireNonNull(type);
        m_serializer        = dependencies.getSerializer();
        m_dependencies      = dependencies;
        f_sScope            = Objects.requireNonNullElse(serviceDependencies.getRemoteScopeName(), Coherence.DEFAULT_SCOPE);
        m_channel           = dependencies.getChannel();
        m_sProtocol         = dependencies.getProtocolName();
        f_requestTimeout     = serviceDependencies.getRequestTimeoutMillis();
        f_nHeartbeatInterval = serviceDependencies.getHeartbeatInterval();
        f_fHeartbeatAck      = serviceDependencies.isRequireHeartbeatAck();
        }

    @Override
    public void connect()
        {
        LockingStreamObserver<ProxyRequest> observer = m_observer;
        if (observer != null && !observer.isDone())
            {
            throw new IllegalStateException("Already initialized");
            }
        ensureConnected();
        }

    @Override
    public boolean isConnected()
        {
        return m_observer != null && !m_observer.isDone();
        }

    @Override
    public void close()
        {
        closeInternal(null);
        }

    public void closeInternal(Throwable closeWithError)
        {
        Throwable error = closeWithError == null
                ? new RequestIncompleteException("Channel was closed")
                : closeWithError;
        m_mapFuture.values().forEach(f -> f.onError(error));
        if (!m_closed)
            {
            f_lock.lock();
            try
                {
                if (!m_closed)
                    {
                    m_closed = true;
                    m_mapFuture.values().forEach(f -> f.onError(error));
                    m_mapFuture.clear();
    
                    if (m_observer != null && !m_observer.isDone())
                        {
                        m_observer.onCompleted();
                        }

                    if (!m_mapFuture.isEmpty())
                        {
                        m_mapFuture.values().forEach(f -> f.onError(new RequestIncompleteException("channel closed")));
                        m_mapFuture.clear();
                        }
                    m_listeners.clear();
                    m_observer     = null;
                    m_initResponse = null;
                    m_uuid         = null;
                    }
                }
            finally
                {
                f_lock.unlock();
                }
            }
        }

    @Override
    public Channel getChannel()
        {
        return m_channel;
        }

    @Override
    public UUID getUUID()
        {
        assertInit();
        return m_uuid;
        }

    @Override
    public String getProxyVersion()
        {
        assertInit();
        return m_initResponse.getVersion();
        }

    @Override
    public int getProxyVersionEncoded()
        {
        assertInit();
        return m_initResponse.getEncodedVersion();
        }

    @Override
    public int getProtocolVersion()
        {
        assertInit();
        return m_initResponse.getProtocolVersion();
        }

    @Override
    public <T extends Message> T send(Message message)
        {
        return send(message, ensureConnected());
        }

    @Override
    public <T extends Message> CompletableFuture<T> poll(Message message)
        {
        return poll(message, ensureConnected());
        }

    @Override
    public <T extends Message> void poll(Message message, StreamObserver<T> observer)
        {
        poll(message, observer, ensureConnected());
        }

    @Override
    @SuppressWarnings("unchecked")
    public void onNext(ProxyResponse response)
        {
        f_observerLock.lock();
        try
            {
            ProxyResponse.ResponseCase responseCase = response.getResponseCase();
            if (responseCase == ProxyResponse.ResponseCase.HEARTBEAT)
                {
                // nothing to do
                f_cHeartbeatAck.increment();
                return;
                }

            long id = response.getId();
            if (id == 0)
                {
                // this is a non-request related message, send it to any listeners
                try
                    {
                    Message message = response.getMessage().unpack(f_responseType);
                    m_listeners.forEach(listener ->
                        {
                        Predicate<Message> predicate = (Predicate<Message>) listener.predicate();
                        if (predicate.evaluate(message))
                            {
                            ((StreamObserver<Message>) listener.observer()).onNext(message);
                            }
                        });
                    }
                catch (Exception e)
                    {
                    Logger.err(e);
                    }
                }
            else
                {
                StreamObserver<Message> handler = (StreamObserver<Message>) m_mapFuture.get(id);
                if (handler != null)
                    {
                    try
                        {
                        if (responseCase == ProxyResponse.ResponseCase.MESSAGE)
                            {
                            Message message = response.getMessage().unpack(f_responseType);
                            handler.onNext(message);
                            // we do not remove the handler from the map yet, as there may be
                            // more responses for the same request
                            }
                        else
                            {
                            // The response is a terminal type, so we remove the handler from the map
                            m_mapFuture.remove(id);
                            switch (responseCase)
                                {
                                case INIT:
                                    m_initResponse = response.getInit();
                                    m_uuid         = new UUID(m_initResponse.getUuid().toByteArray());
                                    handler.onNext(m_initResponse);
                                    handler.onCompleted();
                                    break;
                                case ERROR:
                                    ErrorMessage error = response.getError();
                                    Throwable cause = null;
                                    if (error.hasError())
                                        {
                                        cause = BinaryHelper.fromByteString(error.getError(), m_serializer);
                                        }
                                    handler.onError(new RequestIncompleteException(error.getMessage(), cause));
                                    break;
                                case COMPLETE:
                                    handler.onCompleted();
                                    break;
                                case RESPONSE_NOT_SET:
                                default:
                                    handler.onError(new RequestIncompleteException("Unexpected response case: " + responseCase));
                                }
                            }
                        }
                    catch (Exception e)
                        {
                        handler.onError(e);
                        }
                    }
                else
                    {
                    Logger.err("Failed to find handler for response: " + id);
                    }
                }
            }
        finally
            {
            f_observerLock.unlock();
            }
        }

    @Override
    public void onError(Throwable t)
        {
        f_observerLock.lock();
        try
            {
            if (m_connectFuture != null)
                {
                m_connectFuture.completeExceptionally(t);
                }

            if (!m_closed)
                {
                ErrorsHelper.logIfNotCancelled(t);
                closeInternal(t);
                }
            else
                {
                Logger.err("onError called after close() has been called", t);
                }
            }
        finally
            {
            f_observerLock.unlock();
            }
        }

    @Override
    public void onCompleted()
        {
        f_observerLock.lock();
        try
            {
            closeInternal(null);
            }
        finally
            {
            f_observerLock.unlock();
            }
        }

    @Override
    public <T extends Message> void addResponseObserver(Listener<T> listener)
        {
        m_listeners.add(listener);
        }

    @Override
    public <T extends Message> void removeResponseObserver(Listener<T> listener)
        {
        m_listeners.remove(listener);
        }

    @Override
    public long getHeartbeatsSent()
        {
        return f_cHeartbeat.sum();
        }

    @Override
    public long getLastHeartbeatTime()
        {
        return m_nLastHeartbeatTime;
        }

    @Override
    public long getHeartbeatsAcked()
        {
        return f_cHeartbeatAck.sum();
        }

    // ----- Object methods -------------------------------------------------

    @Override
    public String toString()
        {
        if (m_closed)
            {
            return "GrpcConnectionV1(Closed)";
            }
        if (m_initResponse == null)
            {
            return "GrpcConnectionV1(Not Initialized)";
            }
        return "GrpcConnectionV1(" +
               "scope=\"" + f_sScope + "\" " +
               ", protocol=\"" + m_sProtocol + "\" " +
               ", version=" + m_initResponse.getProtocolVersion() +
                ")";
        }


    // ----- helper methods -------------------------------------------------

    private  <T extends Message> T send(Message message, LockingStreamObserver<ProxyRequest> observer)
        {
        CompletableFuture<T> future  = poll(message, observer);
        return awaitFuture(future, f_requestTimeout);
        }

    private <T extends Message> T awaitFuture(CompletableFuture<T> future, long nMillis)
        {
        try
            {
            if (nMillis > 0L)
                {
                return future.get(nMillis, TimeUnit.MILLISECONDS);
                }
            return future.get();
            }
        catch (ExecutionException e)
            {
            Throwable cause = e.getCause();
            if (!(cause instanceof StatusRuntimeException))
                {
                Logger.err(cause);
                }
            throw new RequestIncompleteException(e.getMessage(), e);
            }
        catch (InterruptedException | TimeoutException e)
            {
            throw new RequestIncompleteException(e.getMessage(), e);
            }
        }

    private  <T extends Message> CompletableFuture<T> poll(Message message, LockingStreamObserver<ProxyRequest> sender)
        {
        ResponseHandler<T> observer = new ResponseHandler<>();
        poll(message, observer, sender);
        return observer.getFuture();
        }

    private <T extends Message> void poll(Message message, StreamObserver<T> observer, LockingStreamObserver<ProxyRequest> sender)
        {
        try
            {
            long nId = m_nMessageId.incrementAndGet();

            ProxyRequest.Builder builder = ProxyRequest.newBuilder().setId(nId);
            if (message instanceof InitRequest)
                {
                builder.setInit((InitRequest) message);
                }
            else if (message instanceof HeartbeatMessage)
                {
                builder.setHeartbeat((HeartbeatMessage) message);
                }
            else
                {
                builder.setMessage(Any.pack(message));
                }

            m_mapFuture.put(nId, observer);
            sender.onNext(builder.build());
            }
        catch (Exception e)
            {
            observer.onError(e);
            }
        }

    protected void assertActive()
        {
        if (m_closed)
            {
            throw new IllegalStateException("This connection has been closed");
            }
        }

    protected LockingStreamObserver<ProxyRequest> ensureConnected()
        {
        assertActive();
        LockingStreamObserver<ProxyRequest> observer = m_observer;
        if (observer == null)
            {
            f_lock.lock();
            observer = m_observer;
            if (observer == null)
                {
                InitRequest request;
                try
                    {
                    ProxyServiceGrpc.ProxyServiceStub stub = createStub(m_channel);
                    observer = LockingStreamObserver.ensureLockingObserver(
                            SafeStreamObserver.ensureSafeObserver(stub.subChannel(this)));

                    request = InitRequest.newBuilder()
                            .setScope(f_sScope)
                            .setFormat(m_serializer.getName())
                            .setProtocol(m_sProtocol)
                            .setProtocolVersion(m_dependencies.getVersion())
                            .setSupportedProtocolVersion(m_dependencies.getSupportedVersion())
                            .build();

                    ResponseHandler<ProxyResponse> handler = new ResponseHandler<>();
                    m_connectFuture = handler.getFuture();
                    poll(request, handler, observer);
                    long nMillis = f_requestTimeout <= 0 ? GrpcDependencies.DEFAULT_DEADLINE_MILLIS : f_requestTimeout;
                    awaitFuture(m_connectFuture, nMillis);
                    m_connectFuture = null;
                    m_observer = observer;

                    if (f_nHeartbeatInterval > 0L)
                        {
                        Daemons.commonPool().schedule(new HeartbeatTask(f_fHeartbeatAck), f_nHeartbeatInterval);
                        }
                    }
                finally
                    {
                    f_lock.unlock();
                    }
                }
            }
        return observer;
        }

    protected void assertInit()
        {
        assertActive();
        if (m_initResponse == null)
            {
            throw new IllegalStateException("Connection has not been intialized");
            }
        }

    /**
     * Create the default {@link ProxyServiceGrpc.ProxyServiceStub}
     * to use for requests.
     *
     * @return the default {@link ProxyServiceGrpc.ProxyServiceStub}
     */
    private ProxyServiceGrpc.ProxyServiceStub createStub(Channel channel)
        {
        return createStubWithoutDeadline(channel);
        }

    /**
     * Create the default {@link ProxyServiceGrpc.ProxyServiceStub}
     * to use for requests.
     * <p/>
     * The value of {@link PriorityTask#TIMEOUT_DEFAULT TIMEOUT_DEFAULT} indicates
     * a default timeout value configured for the corresponding service; the value of
     * {@link PriorityTask#TIMEOUT_NONE TIMEOUT_NONE} indicates that the client thread
     * is willing to wait indefinitely until the task execution completes or is
     *
     * @param nDeadline the deadline to set on the stub
     * @return the default {@link ProxyServiceGrpc.ProxyServiceStub}
     */
    private ProxyServiceGrpc.ProxyServiceStub createStubWithDeadline(Channel channel, long nDeadline)
        {
        if (nDeadline == PriorityTask.TIMEOUT_NONE)
            {
            return createStubWithoutDeadline(channel);
            }
        else if (nDeadline <= PriorityTask.TIMEOUT_DEFAULT)
            {
            return createStub(channel);
            }
        return createStubWithoutDeadline(channel).withDeadlineAfter(nDeadline, TimeUnit.MILLISECONDS);
        }

    /**
     * Create the default {@link ProxyServiceGrpc.ProxyServiceStub}
     * to use for requests.
     *
     * @return the default {@link ProxyServiceGrpc.ProxyServiceStub}
     */
    private ProxyServiceGrpc.ProxyServiceStub createStubWithoutDeadline(Channel channel)
        {
        return ProxyServiceGrpc.newStub(channel);
        }

    // ----- inner class: ResponseHandler -----------------------------------

    @SuppressWarnings("unchecked")
    private static class ResponseHandler<T extends Message>
            implements StreamObserver<Message>
        {
        @Override
        public void onNext(Message value)
            {
            m_value = (T) value;
            }

        @Override
        public void onError(Throwable t)
            {
            f_future.completeExceptionally(t);
            }

        @Override
        public void onCompleted()
            {
            f_future.complete(m_value);
            }

        /**
         * Return the {@link CompletableFuture} that will be completed
         * with the value received by the call to {@link #onNext(Message)}.
         *
         * @return the {@link CompletableFuture} that will be completed
         *         with the value received by the call to {@link #onNext(Message)}
         */
        private CompletableFuture<T> getFuture()
            {
            return f_future;
            }

        // ----- data members -----------------------------------------------

        /**
         * The {@link CompletableFuture} that will be completed with the
         * value received by the call to {@link #onNext(Message)}.
         */
        private final CompletableFuture<T> f_future = new CompletableFuture<>();

        /**
         * The value received by the call to {@link #onNext(Message)}.
         */
        private T m_value;
        }

    // ----- inner class: HeartbeatTask -------------------------------------

    protected class HeartbeatTask
            implements Runnable
        {
        public HeartbeatTask(boolean fAck)
            {
            ByteString bytes = m_uuid == null
                       ? ByteString.EMPTY
                       : ByteString.copyFrom(m_uuid.toByteArray());

            f_message = HeartbeatMessage.newBuilder()
                    .setUuid(bytes)
                    .setAck(fAck)
                    .build();
            }

        @Override
        public void run()
            {
            if (isConnected())
                {
                m_nLastHeartbeatTime = SafeClock.INSTANCE.getSafeTimeMillis();
                f_cHeartbeat.increment();
                poll(f_message);
                Daemons.commonPool().schedule(this, f_nHeartbeatInterval);
                }
            }

        /**
         * The {@link HeartbeatMessage} to send.
         */
        private final HeartbeatMessage f_message;
        }

    // ----- data members ---------------------------------------------------

    /**
     * A lock to control thread safety.
     */
    private final Lock f_lock = new ReentrantLock();


    /**
     * A lock to control thread safety for received messages.
     */
    private final Lock f_observerLock = new ReentrantLock();

    private final Class<? extends Message> f_responseType;

    /**
     * The list of response listeners.
     */
    private final List<Listener<?>> m_listeners = new CopyOnWriteArrayList<>();

    /**
     * The service dependencies.
     */
    private final GrpcConnection.Dependencies m_dependencies;

    /**
     * The scope to use on the proxy.
     */
    private final String f_sScope;

    /**
     * The client's serializer.
     */
    private final Serializer m_serializer;

    /**
     * A map of {@link CompletableFuture} instances for requests waiting to be completed.
     */
    private final Map<Long, StreamObserver<? extends Message>> m_mapFuture = new ConcurrentHashMap<>();

    /**
     * The unique request identifier.
     */
    private final AtomicLong m_nMessageId = new AtomicLong(1L);

    /**
     * The {@link StreamObserver} to send responses to.
     * <p/>
     * This must be thread safe as calls must be synchronized.
     */
    private LockingStreamObserver<ProxyRequest> m_observer;

    private CompletableFuture<ProxyResponse> m_connectFuture;

    /**
     * The name of the protocol this connection is using.
     */
    private final String m_sProtocol;

    /**
     * The response from the {@link InitRequest}.
     */
    private InitResponse m_initResponse;

    /**
     * This client's {@link UUID}.
     */
    private UUID m_uuid;

    /**
     * A flag to indicate that this connection is closed.
     */
    private volatile boolean m_closed = false;

    /**
     * The underlying gRPC {@link Channel}
     */
    private Channel m_channel;

    /**
     * The request timeout to apply.
     */
    private final long f_requestTimeout;

    /**
     * The frequency to use to send heart-beats.
     */
    private final long f_nHeartbeatInterval;

    /**
     * A flag to indicate whether the server should send ack responses for heart beat messages.
     */
    private boolean f_fHeartbeatAck;

    /**
     * The count of heartbeat acks received.
     */
    private final LongAdder f_cHeartbeatAck = new LongAdder();

    /**
     * The count of heart beat messages sent.
     */
    private final LongAdder f_cHeartbeat = new LongAdder();

    /**
     * The timestamp of the last heart beat message.
     */
    private long m_nLastHeartbeatTime;
    }
