/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.proxy.common;

import com.google.protobuf.Any;
import com.google.protobuf.BoolValue;
import com.google.protobuf.ByteString;
import com.google.protobuf.BytesValue;
import com.google.protobuf.Int32Value;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.oracle.coherence.common.base.Exceptions;
import com.oracle.coherence.grpc.BinaryHelper;
import com.oracle.coherence.grpc.GrpcService;

import com.oracle.coherence.grpc.GrpcServiceProtocol;
import com.oracle.coherence.grpc.messages.common.v1.BinaryKeyAndValue;
import com.oracle.coherence.grpc.messages.proxy.v1.InitRequest;
import com.tangosol.application.ContainerContext;
import com.tangosol.application.Context;
import com.tangosol.coherence.component.net.extend.Connection;
import com.tangosol.coherence.component.net.extend.message.Request;
import com.tangosol.coherence.component.net.extend.proxy.serviceProxy.CacheServiceProxy;
import com.tangosol.io.Serializer;
import com.tangosol.net.ExtensibleConfigurableCacheFactory;
import com.tangosol.net.messaging.Response;
import com.tangosol.util.Binary;
import com.tangosol.util.UUID;
import io.grpc.stub.StreamObserver;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A base class for server side gRPC protocol implementations.
 */
public abstract class BaseProxyProtocol<Req extends Message, Resp extends Message>
        implements GrpcServiceProtocol<Req, Resp>
    {
    @Override
    public Serializer getSerializer()
        {
        return m_serializer;
        }

    public void init(GrpcService service, InitRequest request, int nVersion, UUID clientUUID, StreamObserver<Resp> observer)
        {
        f_lock.lock();
        try
            {
            String sScope  = request.getScope();
            String sFormat = request.getFormat();

            ExtensibleConfigurableCacheFactory eccf = (ExtensibleConfigurableCacheFactory) service.getCCF(sScope);

            m_service       = service;
            m_context       = service.getDependencies().getContext().orElse(null);
            m_ccf           = eccf;
            m_serializer    = service.getSerializer(sFormat, m_ccf.getConfigClassLoader());
            m_connection    = new Connection();
            m_eventObserver = observer;

            m_connection.setId(clientUUID);

            m_proxy = new CacheServiceProxy();
            m_proxy.setCacheFactory(m_ccf);
            m_proxy.setSerializer(m_serializer);

            initInternal(service, request, nVersion, clientUUID);
            }
        finally
            {
            f_lock.unlock();
            }
        }

    protected abstract void initInternal(GrpcService service, InitRequest request, int nVersion, UUID clientUUID);
    
    @Override
    public void onRequest(Req request, StreamObserver<Resp> observer)
        {
        // If we are inside a container (i.e. WLS Managed Coherence) then we must run
        // inside the correct container context
        ContainerContext containerContext = m_context == null ? null : m_context.getContainerContext();
        if (containerContext != null)
            {
            containerContext.runInDomainPartitionContext(() -> onRequestInternal(request, observer));
            }
        else
            {
            onRequestInternal(request, observer);
            }
        }

    protected abstract void onRequestInternal(Req request, StreamObserver<Resp> observer);

    @Override
    public void close()
        {
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Send a response containing a {@link BoolValue} to
     * a {@link StreamObserver} and then complete the observer.
     *
     * @param f         the boolean value to use to send a response before completing
     * @param id        the resource identifier
     * @param observer  the {@link StreamObserver} to complete
     */
    protected void complete(boolean f, int id, StreamObserver<Resp> observer)
        {
        complete(BoolValue.of(f), id, observer);
        }

    /**
     * Send a response containing an {@link Int32Value} to
     * a {@link StreamObserver} and then complete the observer.
     *
     * @param n         the int value to use to send a response before completing
     * @param id        the resource identifier
     * @param observer  the {@link StreamObserver} to complete
     */
    protected void complete(int n, int id, StreamObserver<Resp> observer)
        {
        complete(Int32Value.of(n), id, observer);
        }

    /**
     * Send a response containing a {@link BytesValue} to
     * a {@link StreamObserver} and then complete the observer.
     *
     * @param binary   the binary value to use to send a response before completing
     * @param id        the resource identifier
     * @param observer  the {@link StreamObserver} to complete
     */
    protected void complete(Binary binary, int id, StreamObserver<Resp> observer)
        {
        complete(BinaryHelper.toBytesValue(binary), id, observer);
        }

    /**
     * Send a response containing a {@link BinaryKeyAndValue} to
     * a {@link StreamObserver} and then complete the observer.
     *
     * @param request   the {@link Request} containing the result
     * @param observer  the {@link StreamObserver} to complete
     */
    protected void complete(Request request, StreamObserver<Resp> observer)
        {
        Response response = request.ensureResponse();
        if (response.isFailure())
            {
            observer.onError((Throwable) response.getResult());
            }
        else
            {
            observer.onCompleted();
            }
        }

    /**
     * Send a response containing a {@link BinaryKeyAndValue} to
     * a {@link StreamObserver} and then complete the observer.
     *
     * @param binKey    the binary key to use to send a response before completing
     * @param request   the {@link Request} containing the result
     * @param id        the resource identifier
     * @param observer  the {@link StreamObserver} to complete
     */
    protected void complete(Binary binKey, Request request, int id, StreamObserver<Resp> observer)
        {
        Response response = request.ensureResponse();
        if (response.isFailure())
            {
            observer.onError((Throwable) response.getResult());
            }
        else
            {
            Binary binValue = (Binary) response.getResult();
            completeKeyValue(binKey, binValue, id, observer);
            }
        }

    /**
     * Send a response containing a {@link BinaryKeyAndValue} to
     * a {@link StreamObserver} and then complete the observer.
     *
     * @param binKey    the binary key to use to send a response before completing
     * @param binValue  the {@link Binary} value
     * @param id        the resource identifier
     * @param observer  the {@link StreamObserver} to complete
     */
    protected void completeKeyValue(Binary binKey, Binary binValue, int id, StreamObserver<Resp> observer)
        {
        BinaryKeyAndValue keyAndValue = BinaryKeyAndValue.newBuilder()
                .setKey(BinaryHelper.toByteString(binKey))
                .setValue(BinaryHelper.toByteString(binValue))
                .build();
        complete(keyAndValue, id, observer);
        }

    /**
     * Send a response containing a {@link BytesValue} to
     * a {@link StreamObserver} and then complete the observer.
     *
     * @param message   the {@link Message} value to use to send a response before completing
     * @param id        the resource identifier
     * @param observer  the {@link StreamObserver} to complete
     */
    protected void complete(Message message, int id, StreamObserver<Resp> observer)
        {
        observer.onNext(response(id, Any.pack(message)));
        observer.onCompleted();
        }

    /**
     * Send a response containing a stream of
     * {@link BinaryKeyAndValue} to a {@link StreamObserver}
     * and then complete the observer.
     *
     * @param request   the {@link Request} containing the result
     * @param id        the resource identifier
     * @param observer  the {@link StreamObserver} to complete
     */
    @SuppressWarnings("unchecked")
    protected void completeMapStream(Request request, int id, StreamObserver<Resp> observer)
        {
        Response response = request.ensureResponse();
        if (response.isFailure())
            {
            observer.onError((Throwable) response.getResult());
            }
        else
            {
            Map<Binary, Binary> map = (Map<Binary, Binary>) response.getResult();
            completeMapStream(map, id, observer);
            }
        }

    /**
     * Send a response containing a stream of
     * {@link BinaryKeyAndValue} to a {@link StreamObserver}
     * and then complete the observer.
     *
     * @param map       the map of binary keys and values to stream to the observer
     * @param id        the resource identifier
     * @param observer  the {@link StreamObserver} to complete
     */
    protected void completeMapStream(Map<Binary, Binary> map, int id, StreamObserver<Resp> observer)
        {
        for (Map.Entry<Binary, Binary> entry : map.entrySet())
            {
            BinaryKeyAndValue keyAndValue = BinaryKeyAndValue.newBuilder()
                    .setKey(BinaryHelper.toByteString(entry.getKey()))
                    .setValue(BinaryHelper.toByteString(entry.getValue()))
                    .build();

            observer.onNext(response(id, Any.pack(keyAndValue)));
            }
        observer.onCompleted();
        }

    /**
     * Send a response containing a stream of
     * {@link BinaryKeyAndValue} to a {@link StreamObserver}
     * and then complete the observer.
     *
     * @param request   the {@link Request} containing the result
     * @param id        the resource identifier
     * @param observer  the {@link StreamObserver} to complete
     */
    @SuppressWarnings("unchecked")
    protected void completeSetStream(Request request, int id, StreamObserver<Resp> observer)
        {
        Response response = request.ensureResponse();
        if (response.isFailure())
            {
            observer.onError((Throwable) response.getResult());
            }
        else
            {
            completeSetStream((Set<Binary>) response.getResult(), id, observer);
            }
        }

    /**
     * Send a response containing a stream of
     * {@link BinaryKeyAndValue} to a {@link StreamObserver}
     * and then complete the observer.
     *
     * @param set       the set of binary instances to stream to the observer
     * @param id        the resource identifier
     * @param observer  the {@link StreamObserver} to complete
     */
    protected void completeSetStream(Set<Binary> set, int id, StreamObserver<Resp> observer)
        {
        for (Binary binary : set)
            {
            observer.onNext(response(id, Any.pack(BinaryHelper.toBytesValue(binary))));
            }
        observer.onCompleted();
        }

    protected abstract Resp response(int id, Any any);

    /**
     * Unpack the message field from a request.
     *
     * @param request  the request to get the message from
     * @param type     the expected type of the message
     * @param <T>      the expected type of the message
     *
     * @return the unpacked message
     */
    protected <T extends Message> T unpack(Req request, Class<T> type)
        {
        try
            {
            Any any = getMessage(request);
            return any.unpack(type);
            }
        catch (InvalidProtocolBufferException e)
            {
            throw Exceptions.ensureRuntimeException(e, "Could not unpack message field of type " + type.getName());
            }
        }

    protected abstract Any getMessage(Req request);
    
    /**
     * Deserialize a {@link Binary} value using this proxy's serializer.
     *
     * @param binary  the {@link Binary} to deserialize
     * @param <T>     the expected deserialized type
     *
     * @return the deserialized value or {@code null} if the binary value is {@code null}
     */
    protected <T> T fromBinary(Binary binary)
        {
        if (binary == null)
            {
            return null;
            }
        return BinaryHelper.fromBinary(binary, m_serializer);
        }

    /**
     * Deserialize a {@link ByteString} value using this proxy's serializer.
     *
     * @param bytes  the {@link ByteString} to deserialize
     * @param <T>    the expected deserialized type
     *
     * @return the deserialized value or {@code null} if the {@link ByteString} value is {@code null}
     */
    protected <T> T fromByteString(ByteString bytes)
        {
        if (bytes.isEmpty())
            {
            return null;
            }
        return BinaryHelper.fromByteString(bytes, m_serializer);
        }

    /**
     * Deserialize a {@link ByteString} value using this proxy's serializer,
     * or a default value if the deserialized result is {@code null}.
     *
     * @param bytes         the {@link ByteString} to deserialize
     * @param defaultValue  the default value to return if the deserialized value is null
     * @param <T>    the expected deserialized type
     *
     * @return the deserialized value or the default value if the
     *         deserialized value is {@code null}
     */
    protected <T> T fromByteString(ByteString bytes, T defaultValue)
        {
        if (bytes.isEmpty())
            {
            return defaultValue;
            }
        T oResult = BinaryHelper.fromByteString(bytes, m_serializer);
        return  oResult == null ? defaultValue : oResult;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The lock to control access to state.
     */
    protected static final Lock f_lock = new ReentrantLock();

    /**
     * The parent {@link GrpcService}.
     */
    protected GrpcService m_service;

    /**
     * The {@link ExtensibleConfigurableCacheFactory} owning the cache.
     */
    protected ExtensibleConfigurableCacheFactory m_ccf;

    /**
     * The {@link CacheServiceProxy}.
     */
    protected CacheServiceProxy m_proxy;

    /**
     * The client's serializer.
     */
    protected Serializer m_serializer;

    /**
     * The {@link Connection} to use to send responses.
     */
    protected Connection m_connection;

    /**
     * A bit-set containing destroyed cache identifiers.
     */
    protected final Set<Integer> m_destroyedIds = new HashSet<>();

    /**
     * A {@link StreamObserver} for processing event messages.
     */
    protected StreamObserver<Resp> m_eventObserver;

    /**
     * The optional container {@link Context} for this protocol.
     */
    private Context m_context;
    }
