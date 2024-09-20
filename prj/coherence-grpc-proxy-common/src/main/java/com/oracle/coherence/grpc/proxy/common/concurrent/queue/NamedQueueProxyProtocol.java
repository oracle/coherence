/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.proxy.common.concurrent.queue;

import com.google.protobuf.Any;
import com.google.protobuf.BytesValue;
import com.oracle.coherence.concurrent.Queues;
import com.oracle.coherence.grpc.BinaryHelper;
import com.oracle.coherence.grpc.GrpcService;
import com.oracle.coherence.grpc.NamedQueueProtocol;

import com.oracle.coherence.grpc.messages.common.v1.OptionalValue;
import com.oracle.coherence.grpc.messages.concurrent.queue.v1.EnsureQueueRequest;
import com.oracle.coherence.grpc.messages.concurrent.queue.v1.NamedQueueRequest;
import com.oracle.coherence.grpc.messages.concurrent.queue.v1.NamedQueueRequestType;
import com.oracle.coherence.grpc.messages.concurrent.queue.v1.NamedQueueResponse;
import com.oracle.coherence.grpc.messages.concurrent.queue.v1.NamedQueueResponseType;

import com.oracle.coherence.grpc.messages.concurrent.queue.v1.NamedQueueType;
import com.oracle.coherence.grpc.messages.concurrent.queue.v1.QueueOfferResult;
import com.oracle.coherence.grpc.messages.proxy.v1.InitRequest;
import com.oracle.coherence.grpc.proxy.common.BaseProxyProtocol;

import com.tangosol.internal.net.ConfigurableCacheFactorySession;
import com.tangosol.internal.net.NamedCacheDeactivationListener;
import com.tangosol.internal.net.queue.BinaryNamedMapDeque;
import com.tangosol.internal.net.queue.BinaryNamedMapQueue;
import com.tangosol.internal.net.queue.ConverterNamedMapDeque;
import com.tangosol.internal.net.queue.ConverterNamedMapQueue;
import com.tangosol.internal.net.queue.NamedMapDeque;
import com.tangosol.internal.net.queue.NamedMapQueue;
import com.tangosol.internal.net.queue.paged.BinaryPagedNamedQueue;
import com.tangosol.io.Serializer;
import com.tangosol.net.Coherence;
import com.tangosol.net.ExtensibleConfigurableCacheFactory;
import com.tangosol.net.NamedDeque;
import com.tangosol.net.NamedMap;
import com.tangosol.net.NamedQueue;
import com.tangosol.net.Session;
import com.tangosol.util.Base;
import com.tangosol.util.Binary;
import com.tangosol.util.Converter;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.LongArray;
import com.tangosol.util.MapEvent;
import com.tangosol.util.SparseArray;
import com.tangosol.util.UUID;

import io.grpc.stub.StreamObserver;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The server size {@link NamedQueueProtocol named queue protocol}.
 */
@SuppressWarnings({"resource", "EnhancedSwitchMigration"})
public class NamedQueueProxyProtocol
        extends BaseProxyProtocol<NamedQueueRequest, NamedQueueResponse>
        implements NamedQueueProtocol<NamedQueueRequest, NamedQueueResponse>
    {
    @Override
    public Class<NamedQueueRequest> getRequestType()
        {
        return NamedQueueRequest.class;
        }

    @Override
    public Class<NamedQueueResponse> getResponseType()
        {
        return NamedQueueResponse.class;
        }

    @Override
    protected void initInternal(GrpcService service, InitRequest request, int nVersion, UUID clientUUID)
        {
        String sScope = request.getScope();
        m_fConcurrentSession = Coherence.SYSTEM_SCOPE.equals(sScope);
        if (m_fConcurrentSession)
            {
            m_session = Coherence.findSession(Queues.SESSION_NAME).orElseThrow(() -> new IllegalStateException("Coherence Concurrent session not found"));
            }
        else
            {
            ExtensibleConfigurableCacheFactory eccf = (ExtensibleConfigurableCacheFactory) service.getCCF(sScope);
            m_session = new ConfigurableCacheFactorySession(eccf, eccf.getConfigClassLoader(), sScope);
            }
        }

    @Override
    public void close()
        {
        m_aQueue.clear();
        super.close();
        }
    
    @Override
    protected void onRequestInternal(NamedQueueRequest request, StreamObserver<NamedQueueResponse> observer)
        {
        NamedQueueRequestType requestType = request.getType();

        if (requestType == NamedQueueRequestType.EnsureQueue)
            {
            onEnsureQueue(unpack(request, EnsureQueueRequest.class), observer);
            }
        else
            {
            int queueId = request.getQueueId();
            if (queueId == 0)
                {
                throw new IllegalArgumentException("Missing queue id in request, has an EnsureQueue request been sent" + requestType);
                }

            switch (requestType)
                {
                case Clear:
                    onClear(queueId, observer);
                    break;
                case Destroy:
                    onDestroyQueue(queueId, observer);
                    break;
                case IsEmpty:
                    onIsEmpty(queueId, observer);
                    break;
                case IsReady:
                    onIsReady(queueId, observer);
                    break;
                case OfferTail:
                    onOfferTail(queueId, request, observer);
                    break;
                case OfferHead:
                    onOfferHead(queueId, request, observer);
                    break;
                case PollHead:
                    onPollHead(queueId, observer);
                    break;
                case PeekHead:
                    onPeekHead(queueId, observer);
                    break;
                case PollTail:
                    onPollTail(queueId, observer);
                    break;
                case PeekTail:
                    onPeekTail(queueId, observer);
                    break;
                case Size:
                    onSize(queueId, observer);
                    break;
                case UNRECOGNIZED:
                case Unknown:
                default:
                    throw new IllegalArgumentException("Unrecognized request: " + requestType);
                }
            }
        }

    @Override
    protected NamedQueueResponse response(int queueId, Any any)
        {
        return NamedQueueResponse.newBuilder()
                .setQueueId(queueId)
                .setType(NamedQueueResponseType.Message)
                .setMessage(any)
                .build();
        }

    @Override
    protected Any getMessage(NamedQueueRequest request)
        {
        return request.getMessage();
        }

    // ----- helper methods -------------------------------------------------

    @SuppressWarnings({"unchecked", "rawtypes"})
    protected void onEnsureQueue(EnsureQueueRequest request, StreamObserver<NamedQueueResponse> observer)
        {
        f_lock.lock();
        try
            {
            int queueId;

            String         sName = request.getQueue();
            NamedQueueType type  = request.getType();

            if (m_mapQueue.containsKey(sName))
                {
                // we have already ensured the same queue name
                queueId = m_mapQueue.get(sName);
                NamedQueue<Binary> queue = m_aQueue.get(queueId);
                if (queue instanceof ConverterNamedMapQueue)
                    {
                    queue = (NamedQueue<Binary>) ((ConverterNamedMapQueue) queue).getCollection();
                    }

                switch (type)
                    {
                    case Queue:
                        // everything is a queue so do not check anything
                        break;
                    case Deque:
                        // make sure the previously ensured queue is a Deque
                        if (!(queue instanceof BinaryNamedMapDeque))
                            {
                            throw new IllegalArgumentException("Ensure queue is being called for a previously ensured queue of a different type. name=\""
                                    + sName + "\" requested type \"" + type + "\" actual type \"" + queue.getClass() + "\"");
                            }
                        break;
                    case PagedQueue:
                        // make sure the previously ensured queue is a paged queue
                        if (!(queue instanceof BinaryPagedNamedQueue))
                            {
                            throw new IllegalArgumentException("Ensure queue is being called for a previously ensured queue of a different type. name=\""
                                    + sName + "\" requested type \"" + type + "\" actual type \"" + queue.getClass() + "\"");
                            }
                        break;
                    case UNRECOGNIZED:
                        throw new IllegalArgumentException("Unrecognized queue type " + type);
                    }
                }
            else
                {
                do
                    {
                    queueId = Base.getRandom().nextInt(Integer.MAX_VALUE);
                    }
                while (queueId == 0 || m_aQueue.get(queueId) != null || m_destroyedIds.contains(queueId));

                NamedMapQueue<Binary, Binary> queue;

                switch (type)
                    {
                    case Queue:
                        if (m_fConcurrentSession)
                            {
                            sName = Queues.cacheNameForQueue(sName);
                            }
                        queue = new BinaryNamedMapQueue(sName, m_session);
                        break;
                    case Deque:
                        if (m_fConcurrentSession)
                            {
                            sName = Queues.cacheNameForDeque(sName);
                            }
                        queue = new BinaryNamedMapDeque(sName, m_session);
                        break;
                    case PagedQueue:
                        if (m_fConcurrentSession)
                            {
                            sName = Queues.cacheNameForPagedQueue(sName);
                            }
                        queue = new BinaryPagedNamedQueue(sName, m_session);
                        break;
                    case UNRECOGNIZED:
                    default:
                        throw new IllegalArgumentException("Unrecognized queue type " + type);
                    }

                NamedMap<Binary, Binary> namedMap = queue.getNamedMap();
                namedMap.addMapListener(new QueueCacheListener(queueId));

                Serializer serializerThis = m_proxy.getSerializer();
                Serializer serializerThat = queue.getNamedMap().getService().getSerializer();
                boolean    fCompatible    = ExternalizableHelper.isSerializerCompatible(serializerThis, serializerThat);

                if (!fCompatible)
                    {
                    Converter<Binary, Object> cFromThat = bin -> ExternalizableHelper.fromBinary(bin, serializerThat);
                    Converter<Object, Binary> cToThat   = obj -> ExternalizableHelper.toBinary(obj, serializerThat);
                    Converter<Binary, Object> cFromThis = bin -> ExternalizableHelper.fromBinary(bin, serializerThis);
                    Converter<Object, Binary> cToThis   = obj -> ExternalizableHelper.toBinary(obj, serializerThis);
                    Converter<Binary, Binary> convUp    = bin -> bin == null ? null : cToThis.convert(cFromThat.convert(bin));
                    Converter<Binary, Binary> convDown  = bin -> bin == null ? null : cToThat.convert(cFromThis.convert(bin));

                    if (type == NamedQueueType.Deque)
                        {
                        queue = new ConverterNamedMapDeque<>((NamedMapDeque<Binary, Binary>) queue,
                                convUp, convUp, convDown, convDown);
                        }
                    else
                        {
                        queue = new ConverterNamedMapQueue<>(queue, convUp, convUp, convDown, convDown);
                        }
                    }

                m_mapQueue.put(request.getQueue(), queueId);
                m_aQueue.set(queueId, queue);
                }


            observer.onNext(response(queueId).build());
            observer.onCompleted();
            }
        finally
            {
            f_lock.unlock();
            }
        }

    protected void onClear(int queueId, StreamObserver<NamedQueueResponse> observer)
        {
        NamedQueue<Binary> queue = assertQueue(queueId);
        queue.clear();
        observer.onCompleted();
        }

    @SuppressWarnings("resource")
    protected void onDestroyQueue(int nId, StreamObserver<NamedQueueResponse> observer)
        {
        f_lock.lock();
        try
            {
            NamedQueue<?> queue = m_aQueue.remove(nId);
            if (queue != null)
                {
                String sName = queue.getName();
                queue.destroy();
                m_mapQueue.remove(sName);
                }
            m_destroyedIds.add(nId);
            }
        finally
            {
            f_lock.unlock();
            }
        observer.onCompleted();
        }

    protected void onIsEmpty(int queueId, StreamObserver<NamedQueueResponse> observer)
        {
        NamedQueue<Binary> queue = assertQueue(queueId);
        complete(queue.isEmpty(), queueId, observer);
        }

    protected void onIsReady(int queueId, StreamObserver<NamedQueueResponse> observer)
        {
        NamedQueue<Binary> queue = assertQueue(queueId);
        complete(queue.isReady(), queueId, observer);
        }

    protected void onOfferTail(int queueId, NamedQueueRequest request, StreamObserver<NamedQueueResponse> observer)
        {
        NamedQueue<Binary> queue  = assertQueue(queueId);
        Binary             binary = unpackBinary(request);

        long    id       = queue.append(binary);
        boolean fSuccess = id != Long.MIN_VALUE;

        QueueOfferResult result = QueueOfferResult.newBuilder()
                .setIndex(id)
                .setSucceeded(fSuccess)
                .build();
        complete(result, queueId, observer);
        }

    protected void onOfferHead(int queueId, NamedQueueRequest request, StreamObserver<NamedQueueResponse> observer)
        {
        NamedDeque<Binary> queue  = assertDeque(queueId);
        Binary             binary = unpackBinary(request);

        long    id       = queue.prepend(binary);
        boolean fSuccess = id != Long.MIN_VALUE;

        QueueOfferResult result = QueueOfferResult.newBuilder()
                .setIndex(id)
                .setSucceeded(fSuccess)
                .build();
        complete(result, queueId, observer);
        }

    protected void onPollHead(int queueId, StreamObserver<NamedQueueResponse> observer)
        {
        NamedQueue<Binary> queue = assertQueue(queueId);
        completePeekOrPoll(queue.poll(), queueId, observer);
        }

    protected void onPeekHead(int queueId, StreamObserver<NamedQueueResponse> observer)
        {
        NamedQueue<Binary> queue = assertQueue(queueId);
        completePeekOrPoll(queue.peek(), queueId, observer);
        }

    protected void onPollTail(int queueId, StreamObserver<NamedQueueResponse> observer)
        {
        NamedDeque<Binary> deque = assertDeque(queueId);
        completePeekOrPoll(deque.pollLast(), queueId, observer);
        }

    protected void onPeekTail(int queueId, StreamObserver<NamedQueueResponse> observer)
        {
        NamedDeque<Binary> deque = assertDeque(queueId);
        completePeekOrPoll(deque.peekLast(), queueId, observer);
        }

    protected void onSize(int queueId, StreamObserver<NamedQueueResponse> observer)
        {
        NamedQueue<Binary> queue = assertQueue(queueId);
        complete(queue.size(), queueId, observer);
        }

    protected void completePeekOrPoll(Binary binResult, int queueId, StreamObserver<NamedQueueResponse> observer)
        {
        OptionalValue.Builder builder   = OptionalValue.newBuilder();

        if (binResult == null)
            {
            builder.setPresent(false);
            }
        else
            {
            builder.setPresent(true);
            builder.setValue(BinaryHelper.toByteString(binResult));
            }

        observer.onNext(response(queueId)
                .setType(NamedQueueResponseType.Message)
                .setMessage(Any.pack(builder.build()))
                .build());
        observer.onCompleted();
        }

    protected NamedQueue<Binary> assertQueue(int queueId)
        {
        if (m_destroyedIds.contains(queueId))
            {
            throw new IllegalStateException("The queue with id " + queueId + " has been explicitly destroyed");
            }

        NamedQueue<Binary> queue = m_aQueue.get(queueId);
        if (queue == null)
            {
            throw new IllegalStateException("No queue exist for id " + queueId);
            }
        return queue;
        }

    protected NamedDeque<Binary> assertDeque(int queueId)
        {
        NamedQueue<Binary> queue = assertQueue(queueId);
        if (queue instanceof NamedDeque)
            {
            return (NamedDeque<Binary>) queue;
            }
        throw new IllegalStateException("The queue with id " + queueId + " is not a NamedDeque");
        }

    /**
     * Create a {@link NamedQueueResponse.Builder} with the queue
     * identifier set to the specified value.
     *
     * @param queueId  the queue identifier
     *
     * @return a {@link NamedQueueResponse.Builder} with the queue
     *         identifier set to the specified value
     */
    protected NamedQueueResponse.Builder response(int queueId)
        {
        return NamedQueueResponse.newBuilder().setQueueId(queueId);
        }

    /**
     * Return a {@link BytesValue} from a {@link NamedQueueRequest}
     * converted to a {@link Binary}.
     *
     * @param request  the {@link NamedQueueRequest} to get the {@link BytesValue} from
     *
     * @return the {@link BytesValue} from a {@link NamedQueueRequest}
     *         converted to a {@link Binary}
     */
    protected Binary unpackBinary(NamedQueueRequest request)
        {
        BytesValue binaryValue = unpack(request, BytesValue.class);
        return BinaryHelper.toBinary(binaryValue.getValue());
        }

    // ----- inner class: QueueListener -------------------------------------

    /**
     * A {@link NamedCacheDeactivationListener} to receive truncate and destroy
     * events for the queue.
     */
    @SuppressWarnings("rawtypes")
    protected class QueueCacheListener
            implements NamedCacheDeactivationListener
        {
        public QueueCacheListener(int queueId)
            {
            m_queueId = queueId;
            }

        @Override
        public void entryInserted(MapEvent evt)
            {
            // unused
            }

        @Override
        public void entryUpdated(MapEvent evt)
            {
            // Queue truncated
            send(NamedQueueResponseType.Truncated);
            }

        @Override
        public void entryDeleted(MapEvent evt)
            {
            // Queue destroyed
            send(NamedQueueResponseType.Destroyed);
            }

        private void send(NamedQueueResponseType type)
            {
            NamedQueueResponse event = NamedQueueResponse.newBuilder()
                    .setQueueId(m_queueId)
                    .setType(type)
                    .build();
            m_eventObserver.onNext(event);
            }

        /**
         * The queue identifier.
         */
        private final int m_queueId;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The session for this proxy.
     */
    private Session m_session;

    /**
     * {@code true} if the {@link Session} is the Coherence Concurrent session
     */
    private boolean m_fConcurrentSession;

    /**
     * An array of {@link NamedQueue} instances indexed by the queue identifier.
     */
    protected final LongArray<NamedQueue<Binary>> m_aQueue = new SparseArray<>();

    /**
     * The map of queue names to queue identifiers.
     */
    protected final Map<String, Integer> m_mapQueue = new ConcurrentHashMap<>();
    }
