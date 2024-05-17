/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.proxy.common.cache;

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
import com.oracle.coherence.grpc.NamedCacheProtocol;

import com.oracle.coherence.grpc.messages.cache.v1.EnsureCacheRequest;
import com.oracle.coherence.grpc.messages.cache.v1.ExecuteRequest;
import com.oracle.coherence.grpc.messages.cache.v1.IndexRequest;
import com.oracle.coherence.grpc.messages.cache.v1.KeyOrFilter;
import com.oracle.coherence.grpc.messages.cache.v1.KeysOrFilter;
import com.oracle.coherence.grpc.messages.cache.v1.MapEventMessage;
import com.oracle.coherence.grpc.messages.cache.v1.MapListenerRequest;
import com.oracle.coherence.grpc.messages.cache.v1.NamedCacheRequest;
import com.oracle.coherence.grpc.messages.cache.v1.NamedCacheRequestType;
import com.oracle.coherence.grpc.messages.cache.v1.NamedCacheResponse;
import com.oracle.coherence.grpc.messages.cache.v1.PutAllRequest;
import com.oracle.coherence.grpc.messages.cache.v1.PutRequest;
import com.oracle.coherence.grpc.messages.cache.v1.QueryRequest;
import com.oracle.coherence.grpc.messages.cache.v1.ReplaceMappingRequest;
import com.oracle.coherence.grpc.messages.cache.v1.ResponseType;
import com.oracle.coherence.grpc.messages.common.v1.BinaryKeyAndValue;
import com.oracle.coherence.grpc.messages.common.v1.CollectionOfBytesValues;
import com.oracle.coherence.grpc.messages.common.v1.OptionalValue;
import com.oracle.coherence.grpc.messages.proxy.v1.InitRequest;

import com.tangosol.application.ContainerContext;
import com.tangosol.application.Context;
import com.tangosol.coherence.component.net.extend.Channel;
import com.tangosol.coherence.component.net.extend.Connection;

import com.tangosol.coherence.component.net.extend.message.Request;
import com.tangosol.coherence.component.net.extend.messageFactory.NamedCacheFactory;
import com.tangosol.coherence.component.net.extend.proxy.NamedCacheProxy;
import com.tangosol.coherence.component.net.extend.proxy.serviceProxy.CacheServiceProxy;

import com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.Acceptor;
import com.tangosol.internal.net.NamedCacheDeactivationListener;
import com.tangosol.internal.util.collection.ConvertingNamedCache;
import com.tangosol.internal.util.processor.BinaryProcessors;
import com.tangosol.internal.util.processor.CacheProcessors;

import com.tangosol.io.Serializer;

import com.tangosol.net.AsyncNamedCache;
import com.tangosol.net.ExtensibleConfigurableCacheFactory;
import com.tangosol.net.NamedCache;

import com.tangosol.net.cache.CacheMap;

import com.tangosol.net.messaging.ConnectionManager;
import com.tangosol.net.messaging.Response;
import com.tangosol.util.Base;
import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.Filter;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.LongArray;
import com.tangosol.util.MapEvent;
import com.tangosol.util.MapTrigger;
import com.tangosol.util.SimpleMapEntry;
import com.tangosol.util.SparseArray;
import com.tangosol.util.UUID;

import com.tangosol.util.ValueExtractor;
import com.tangosol.util.filter.AlwaysFilter;

import com.tangosol.util.filter.InKeySetFilter;
import io.grpc.stub.StreamObserver;

import java.util.BitSet;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.LongStream;

/**
 * The server side {@link NamedCacheProtocol} implementation.
 */
public class NamedCacheProxyProtocol
        implements NamedCacheProtocol<NamedCacheRequest, NamedCacheResponse>
    {
    @Override
    public Class<NamedCacheRequest> getRequestType()
        {
        return NamedCacheRequest.class;
        }

    @Override
    public Class<NamedCacheResponse> getResponseType()
        {
        return NamedCacheResponse.class;
        }

    @Override
    public Serializer getSerializer()
        {
        return m_serializer;
        }

    @Override
    public void init(GrpcService service, InitRequest request, int nVersion, UUID clientUUID, StreamObserver<NamedCacheResponse> observer)
        {
        f_lock.lock();
        try
            {
            String sScope  = request.getScope();
            String sFormat = request.getFormat();

            m_service       = service;
            m_context       = service.getDependencies().getContext().orElse(null);
            m_ccf           = (ExtensibleConfigurableCacheFactory) service.getCCF(sScope);
            m_serializer    = service.getSerializer(sFormat, m_ccf.getConfigClassLoader());
            m_proxy         = new CacheServiceProxy();
            m_connection    = new Connection();
            m_eventObserver = observer;

            m_connection.setId(clientUUID);

            m_proxy.setCacheFactory(m_ccf);
            m_proxy.setSerializer(m_serializer);
            }
        finally
            {
            f_lock.unlock();
            }
        }

    @Override
    public void onRequest(NamedCacheRequest request, StreamObserver<NamedCacheResponse> observer)
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

    private void onRequestInternal(NamedCacheRequest request, StreamObserver<NamedCacheResponse> observer)
        {
        NamedCacheRequestType requestType = request.getType();
        int                   cacheId     = request.getCacheId();

        if (requestType == NamedCacheRequestType.EnsureCache)
            {
            onEnsureCache(unpack(request, EnsureCacheRequest.class), observer);
            }
        else
            {
            if (cacheId == 0)
                {
                throw new IllegalArgumentException("Missing channel id in request, has an EnsureCache request been sent" + requestType);
                }

            if (m_destroyedIds.get(cacheId))
                {
                throw new IllegalStateException("The cache with id " + cacheId + " has been explicitly destroyed");
                }

            NamedCacheProxy proxy = m_aProxy.get(cacheId);
            if (proxy == null)
                {
                throw new IllegalStateException("No cache proxy exist for id " + cacheId);
                }

            switch (requestType)
                {
                case Aggregate:
                    onAggregate(proxy, request, observer);
                    break;
                case Clear:
                    onClear(proxy, observer);
                    break;
                case ContainsEntry:
                    onContainsEntry(proxy, request, observer);
                    break;
                case ContainsKey:
                    onContainsKey(proxy, request, observer);
                    break;
                case ContainsValue:
                    onContainsValue(proxy, request, observer);
                    break;
                case Destroy:
                    onDestroyCache(cacheId, observer);
                    break;
                case Get:
                    onGet(proxy, request, observer);
                    break;
                case GetAll:
                    onGetAll(proxy, request, observer);
                    break;
                case Index:
                    onIndex(proxy, request, observer);
                    break;
                case Invoke:
                    onInvoke(proxy, request, observer);
                    break;
                case IsEmpty:
                    onIsEmpty(proxy, observer);
                    break;
                case IsReady:
                    onIsReady(proxy, observer);
                    break;
                case MapListener:
                    onMapListener(proxy, request, observer);
                    break;
                case PageOfEntries:
                    onPageOfEntries(proxy, request, observer);
                    break;
                case PageOfKeys:
                    onPageOfKeys(proxy, request, observer);
                    break;
                case Put:
                    onPut(proxy, request, observer);
                    break;
                case PutAll:
                    onPutAll(proxy, request, observer);
                    break;
                case PutIfAbsent:
                    onPutIfAbsent(proxy, request, observer);
                    break;
                case QueryEntries:
                    onQueryEntrySet(proxy, request, observer);
                    break;
                case QueryKeys:
                    onQueryKeySet(proxy, request, observer);
                    break;
                case QueryValues:
                    onQueryValues(proxy, request, observer);
                    break;
                case Remove:
                    onRemove(proxy, request, observer);
                    break;
                case RemoveMapping:
                    onRemoveMapping(proxy, request, observer);
                    break;
                case Replace:
                    onReplace(proxy, request, observer);
                    break;
                case ReplaceMapping:
                    onReplaceMapping(proxy, request, observer);
                    break;
                case Size:
                    onSize(proxy, observer);
                    break;
                case Truncate:
                    onTruncate(proxy, observer);
                    break;
                case UNRECOGNIZED:
                case Unknown:
                default:
                    throw new IllegalArgumentException("Unrecognized request: " + requestType);
                }
            }
        }

    @Override
    public void close()
        {

        }

    // ----- helper methods -------------------------------------------------

    @SuppressWarnings({"rawtypes", "unchecked"})
    protected void onAggregate(NamedCacheProxy proxy, NamedCacheRequest request, StreamObserver<NamedCacheResponse> observer)
        {
        ExecuteRequest               execute    = unpack(request, ExecuteRequest.class);
        InvocableMap.EntryAggregator aggregator = fromByteString(execute.getAgent());

        if (execute.hasKeys())
            {
            KeysOrFilter keysOrFilter = execute.getKeys();
            if (keysOrFilter.hasKey())
                {
                Binary binKey    = BinaryHelper.toBinary(keysOrFilter.getKey());
                Binary binResult = (Binary) proxy.aggregate(List.of(binKey), aggregator);
                completeKeyValue(binKey, binResult, proxy, observer);
                }
            else if (keysOrFilter.hasKeys())
                {
                CollectionOfBytesValues keys     = keysOrFilter.getKeys();
                List<Binary>            listKeys = keys.getValuesList().stream()
                                                        .map(BinaryHelper::toBinary)
                                                        .toList();

                Binary binResult = (Binary) proxy.aggregate(listKeys, aggregator);
                complete(binResult, proxy, observer);
                }
            else if (keysOrFilter.hasFilter())
                {
                Filter<?> filter    = fromByteString(keysOrFilter.getFilter());
                Binary    binResult = (Binary) proxy.aggregate(Objects.requireNonNullElse(filter, AlwaysFilter.INSTANCE()), aggregator);
                complete(binResult, proxy, observer);
                }
            else
                {
                Binary    binResult = (Binary) proxy.aggregate(aggregator);
                complete(binResult, proxy, observer);
                }
            }
        else
            {
            Binary    binResult = (Binary) proxy.aggregate(aggregator);
            complete(binResult, proxy, observer);
            }
        }

    protected void onClear(NamedCacheProxy proxy, StreamObserver<NamedCacheResponse> observer)
        {
        proxy.clear();
        observer.onCompleted();
        }

    protected void onContainsEntry(NamedCacheProxy proxy, NamedCacheRequest request, StreamObserver<NamedCacheResponse> observer)
        {
        BinaryKeyAndValue keyAndValue = unpack(request, BinaryKeyAndValue.class);
        Binary            binKey      = BinaryHelper.toBinary(keyAndValue.getKey());
        Binary            binValue    = BinaryHelper.toBinary(keyAndValue.getValue());
        boolean           fContains   = proxy.entrySet().contains(new SimpleMapEntry<>(binKey, binValue));
        complete(fContains, proxy, observer);
        }

    protected void onContainsKey(NamedCacheProxy proxy, NamedCacheRequest request, StreamObserver<NamedCacheResponse> observer)
        {
        Binary binKey = unpackBinary(request);
        complete(proxy.containsKey(binKey), proxy, observer);
        }

    protected void onContainsValue(NamedCacheProxy proxy, NamedCacheRequest request, StreamObserver<NamedCacheResponse> observer)
        {
        Binary binValue = unpackBinary(request);
        complete(proxy.containsValue(binValue), proxy, observer);
        }

    @SuppressWarnings("resource")
    protected void onDestroyCache(int nId, StreamObserver<NamedCacheResponse> observer)
        {
        f_lock.lock();
        try
            {
            NamedCacheProxy proxy = m_aProxy.remove(nId);
            if (proxy != null)
                {
                proxy.getNamedCache().destroy();
                }
            m_destroyedIds.set(nId);
            }
        finally
            {
            f_lock.unlock();
            }
        observer.onCompleted();
        }

    protected void onEnsureCache(EnsureCacheRequest request, StreamObserver<NamedCacheResponse> observer)
        {
        f_lock.lock();
        try
            {
            int cacheId;
            do
                {
                cacheId = Base.getRandom().nextInt(Integer.MAX_VALUE);
                }
            while (cacheId == 0 || m_aProxy.get(cacheId) != null || m_destroyedIds.get(cacheId));

            NamedCache<?, ?> cache      = m_proxy.ensureCache(request.getCache(), null);
            NamedCacheProxy  cacheProxy = new NamedCacheProxy();
            cacheProxy.setNamedCache(cache);
            cacheProxy.setCacheId(cacheId);
            cacheProxy.addMapListener(new CacheListener(cacheId));

            Serializer serializerThis = m_proxy.getSerializer();
            Serializer serializerThat = CacheServiceProxy.getSerializer(cache);
            boolean    fCompatible    = ExternalizableHelper.isSerializerCompatible(serializerThis, serializerThat);

            ChannelStub channelStub = new ChannelStub(cacheId, fCompatible);
            cacheProxy.registerChannel(channelStub);

            m_aProxy.set(cacheId, cacheProxy);

            observer.onNext(response(cacheId).build());
            observer.onCompleted();
            }
        finally
            {
            f_lock.unlock();
            }
        }

    @SuppressWarnings("unchecked")
    protected void onGet(NamedCacheProxy proxy, NamedCacheRequest request, StreamObserver<NamedCacheResponse> observer)
        {
        Binary                binKey    = unpackBinary(request);
        OptionalValue.Builder builder   = OptionalValue.newBuilder();
        ChannelStub           channel   = (ChannelStub) proxy.getChannel();
        Binary                binResult = null;

        if (channel.isSerializerCompatible())
            {
            binResult = (Binary) proxy.get(binKey);
            }
        else
            {
            ConvertingNamedCache cache = (ConvertingNamedCache) proxy.getNamedCache();
            Binary binKeyConv = (Binary) cache.getConverterKeyUp().convert(cache.getConverterKeyDown().convert(binKey));
            Binary binary = (Binary) cache.invokePassThru(binKeyConv, BinaryProcessors.get());
            if (binary != null)
                {
                Object oResult = ExternalizableHelper.fromBinary(binary, cache.getService().getSerializer());
                binResult = (Binary) cache.getConverterValueUp().convert(oResult);
                }
            }

        if (binResult == null || Binary.NO_BINARY.equals(binKey))
            {
            builder.setPresent(false);
            }
        else
            {
            builder.setPresent(true);
            builder.setValue(BinaryHelper.toByteString(binResult));
            }

        observer.onNext(response(proxy.getCacheId())
                .setType(ResponseType.Message)
                .setMessage(Any.pack(builder.build()))
                .build());
        observer.onCompleted();
        }

    @SuppressWarnings("unchecked")
    protected void onGetAll(NamedCacheProxy proxy, NamedCacheRequest request, StreamObserver<NamedCacheResponse> observer)
        {
        int                     cacheId  = proxy.getCacheId();
        CollectionOfBytesValues colKeys  = unpack(request, CollectionOfBytesValues.class);
        List<Binary>            listKeys = colKeys.getValuesList().stream()
                .map(BinaryHelper::toBinary)
                .toList();

        BiConsumer<? super Binary, ? super Binary> callback = (k, v) ->
            {
            BinaryKeyAndValue keyAndValue = BinaryKeyAndValue.newBuilder()
                    .setKey(BinaryHelper.toByteString(k))
                    .setValue(BinaryHelper.toByteString(v))
                    .build();

            observer.onNext(NamedCacheResponse.newBuilder()
                    .setCacheId(cacheId)
                    .setType(ResponseType.Message)
                    .setMessage(Any.pack(keyAndValue))
                    .build());
            };

        async(proxy).ifPresentOrElse(
            asyncCache -> asyncCache.invokeAll(listKeys, BinaryProcessors.get(), callback).join(),
            () -> proxy.getAll(listKeys).forEach(callback));
        observer.onCompleted();
        }

    protected void onIndex(NamedCacheProxy proxy, NamedCacheRequest request, StreamObserver<NamedCacheResponse> observer)
        {
        IndexRequest         indexRequest = unpack(request, IndexRequest.class);
        ValueExtractor<?, ?> extractor    = fromByteString(indexRequest.getExtractor());

        if (indexRequest.getAdd())
            {
            boolean       fSorted    = indexRequest.getSorted();
            Comparator<?> comparator = fromByteString(indexRequest.getComparator());

            proxy.addIndex(extractor, fSorted, comparator);
            }
        else
            {
            proxy.removeIndex(extractor);
            }
        observer.onCompleted();
        }

    @SuppressWarnings({"rawtypes", "unchecked"})
    protected void onInvoke(NamedCacheProxy proxy, NamedCacheRequest request, StreamObserver<NamedCacheResponse> observer)
        {
        ExecuteRequest               execute      = unpack(request, ExecuteRequest.class);
        InvocableMap.EntryProcessor  processor    = fromByteString(execute.getAgent());
        KeysOrFilter                 keysOrFilter = execute.getKeys();
        KeysOrFilter.KeyOrFilterCase type         = keysOrFilter.getKeyOrFilterCase();

        if (type == KeysOrFilter.KeyOrFilterCase.KEY)
            {
            Binary binKey   = BinaryHelper.toBinary(keysOrFilter.getKey());
            Binary binValue = (Binary) proxy.invoke(binKey, processor);
            completeKeyValue(binKey, binValue, proxy, observer);
            }
        else if (type == KeysOrFilter.KeyOrFilterCase.KEYS)
            {
            CollectionOfBytesValues keys     = keysOrFilter.getKeys();
            List<Binary>            listKeys = keys.getValuesList().stream()
                                                    .map(BinaryHelper::toBinary)
                                                    .toList();
            Map<Binary, Binary> map = proxy.invokeAll(listKeys, processor);
            completeMapStream(map, proxy, observer);
            }
        else // either a Filter or nothing specified (i.e. AlwaysFilter)
            {
            Filter<?> filter = keysOrFilter.hasFilter()
                    ? BinaryHelper.fromByteString(keysOrFilter.getFilter(), m_serializer)
                    : AlwaysFilter.INSTANCE();

            Map<Binary, Binary> map = proxy.invokeAll(filter, processor);
            completeMapStream(map, proxy, observer);
            }
        }

    protected void onIsEmpty(NamedCacheProxy proxy, StreamObserver<NamedCacheResponse> observer)
        {
        complete(proxy.isEmpty(), proxy, observer);
        }

    protected void onIsReady(NamedCacheProxy proxy, StreamObserver<NamedCacheResponse> observer)
        {
        complete(proxy.isReady(), proxy, observer);
        }

    protected void onMapListener(NamedCacheProxy proxy, NamedCacheRequest request, StreamObserver<NamedCacheResponse> observer)
        {
        MapListenerRequest listenerRequest = unpack(request, MapListenerRequest.class);
        boolean            fAdd            = listenerRequest.getSubscribe();
        KeyOrFilter        keyOrFilter     = listenerRequest.getKeyOrFilter();

        if (keyOrFilter.getKeyOrFilterCase() == KeyOrFilter.KeyOrFilterCase.KEY)
            {
            NamedCacheFactory.ListenerKeyRequest listenerKeyRequest = createListenerKeyRequest(proxy, listenerRequest);
            listenerKeyRequest.setAdd(fAdd);
            listenerKeyRequest.run();
            complete(listenerKeyRequest, observer);
            }
        else
            {
            NamedCacheFactory.ListenerFilterRequest listenerFilterRequest = createListenerFilterRequest(proxy, listenerRequest);
            listenerFilterRequest.setAdd(fAdd);
            listenerFilterRequest.run();
            complete(listenerFilterRequest, observer);
            }
        }


    protected NamedCacheFactory.ListenerKeyRequest createListenerKeyRequest(NamedCacheProxy proxy, MapListenerRequest request)
        {
        NamedCacheFactory.ListenerKeyRequest listenerKeyRequest = new NamedCacheFactory.ListenerKeyRequest();
        listenerKeyRequest.setNamedCache(proxy);
        listenerKeyRequest.setChannel(proxy.getChannel());

        Binary binKey = BinaryHelper.toBinary(request.getKeyOrFilter().getKey());
        listenerKeyRequest.setKey(binKey);
        listenerKeyRequest.setLite(request.getLite());
        listenerKeyRequest.setPriming(request.getPriming());
        listenerKeyRequest.setNamedCache(proxy);
        return listenerKeyRequest;
        }

    protected NamedCacheFactory.ListenerFilterRequest createListenerFilterRequest(NamedCacheProxy proxy, MapListenerRequest request)
        {
        MapTrigger<?, ?> trigger     = fromByteString(request.getTrigger());
        Filter<?>        dflt        = trigger == null ? AlwaysFilter.INSTANCE : null;
        KeyOrFilter      keyOrFilter = request.getKeyOrFilter();
        Filter<?>        filter      = keyOrFilter.getKeyOrFilterCase() == KeyOrFilter.KeyOrFilterCase.FILTER
                                                ? fromByteString(keyOrFilter.getFilter(), dflt) : null;

        if (filter instanceof InKeySetFilter<?>)
            {
            InKeySetFilter<?> filterKeys = new InKeySetFilter<>(null, ((InKeySetFilter<?>) filter).getKeys());
            filterKeys.ensureConverted(k -> ExternalizableHelper.toBinary(k, m_serializer));
            filter = filterKeys;
            }

        NamedCacheFactory.ListenerFilterRequest listenerFilterRequest = new NamedCacheFactory.ListenerFilterRequest();
        listenerFilterRequest.setNamedCache(proxy);
        listenerFilterRequest.setChannel(proxy.getChannel());
        listenerFilterRequest.setFilter(filter);
        listenerFilterRequest.setFilterId(request.getFilterId());
        listenerFilterRequest.setLite(request.getLite());
        listenerFilterRequest.setPriming(request.getPriming());
        listenerFilterRequest.setNamedCache(proxy);
        listenerFilterRequest.setTrigger(trigger);
        return listenerFilterRequest;
        }

    @SuppressWarnings("unchecked")
    protected void onPageOfEntries(NamedCacheProxy proxy, NamedCacheRequest request, StreamObserver<NamedCacheResponse> observer)
        {
        BytesValue cookieBytes = request.hasMessage() ? unpack(request, BytesValue.class) : null;
        Binary     binCookie   = cookieBytes == null || cookieBytes.getValue().isEmpty()
                                    ? null : BinaryHelper.toBinary(cookieBytes);

        NamedCacheFactory.QueryRequest qr = new NamedCacheFactory.QueryRequest();
        qr.setCookie(binCookie);
        qr.setKeysOnly(false);
        qr.setNamedCache(proxy);
        qr.setChannel(proxy.getChannel());

        Binary              cookie;
        Map<Binary, Binary> map;
        while (true)
            {
            qr.run();
            if (qr.ensureResponse().isFailure())
                {
                complete(qr, observer);
                return;
                }

            NamedCacheFactory.PartialResponse response = (NamedCacheFactory.PartialResponse) qr.ensureResponse();
            map    = (Map<Binary, Binary>) response.getResult();
            cookie = response.getCookie();
            if (!map.isEmpty() || cookie == null)
                {
                break;
                }
            else
                {
                qr.setCookie(response.getCookie());
                }
            }


        observer.onNext(response(proxy)
                .setMessage(Any.pack(BinaryHelper.toBytesValue(cookie)))
                .build());

        completeMapStream(map, proxy, observer);
        }

    @SuppressWarnings("unchecked")
    protected void onPageOfKeys(NamedCacheProxy proxy, NamedCacheRequest request, StreamObserver<NamedCacheResponse> observer)
        {
        BytesValue cookieBytes = request.hasMessage() ? unpack(request, BytesValue.class) : null;
        Binary     binCookie   = cookieBytes == null || cookieBytes.getValue().isEmpty()
                                    ? null : BinaryHelper.toBinary(cookieBytes);

        NamedCacheFactory.QueryRequest qr = new NamedCacheFactory.QueryRequest();
        qr.setCookie(binCookie);
        qr.setKeysOnly(true);
        qr.setNamedCache(proxy);
        qr.setChannel(proxy.getChannel());

        Binary      cookie;
        Set<Binary> set;
        while (true)
            {
            qr.run();
            if (qr.ensureResponse().isFailure())
                {
                complete(qr, observer);
                return;
                }

            NamedCacheFactory.PartialResponse response = (NamedCacheFactory.PartialResponse) qr.ensureResponse();
            set    = (Set<Binary>) response.getResult();
            cookie = response.getCookie();
            if (!set.isEmpty() || cookie == null)
                {
                break;
                }
            else
                {
                qr.setCookie(response.getCookie());
                }
            }

        observer.onNext(response(proxy)
                .setMessage(Any.pack(BinaryHelper.toBytesValue(cookie)))
                .build());

        completeSetStream(set, proxy, observer);
        }

    protected void onPut(NamedCacheProxy proxy, NamedCacheRequest request, StreamObserver<NamedCacheResponse> observer)
        {
        PutRequest putRequest = unpack(request, PutRequest.class);
        long       ttl        = putRequest.hasTtl() ? putRequest.getTtl() : CacheMap.EXPIRY_DEFAULT;
        Binary     binKey     = BinaryHelper.toBinary(putRequest.getKey());
        Binary     binValue   = BinaryHelper.toBinary(putRequest.getValue());
        Binary     binResult  = (Binary) proxy.put(binKey, binValue, ttl);
        complete(binResult, proxy, observer);
        }

    @SuppressWarnings("unchecked")
    protected void onPutAll(NamedCacheProxy proxy, NamedCacheRequest request, StreamObserver<NamedCacheResponse> observer)
        {
        PutAllRequest           putRequest = unpack(request, PutAllRequest.class);
        List<BinaryKeyAndValue> entries    = putRequest.getEntriesList();
        if (!entries.isEmpty())
            {
            Map<Binary, Binary> map = new HashMap<>();
            for (BinaryKeyAndValue keyAndValue : entries)
                {
                map.put(BinaryHelper.toBinary(keyAndValue.getKey()), BinaryHelper.toBinary(keyAndValue.getValue()));
                }
            long ttl = putRequest.getTtl();
            if (ttl == NamedCache.EXPIRY_DEFAULT)
                {
                proxy.putAll(map);
                }
            else
                {
                proxy.async().putAll(map, ttl).join();
                }
            }
        observer.onCompleted();
        }

    @SuppressWarnings("unchecked")
    protected void onPutIfAbsent(NamedCacheProxy proxy, NamedCacheRequest request, StreamObserver<NamedCacheResponse> observer)
        {
        PutRequest putRequest = unpack(request, PutRequest.class);
        Binary     binKey     = BinaryHelper.toBinary(putRequest.getKey());
        Binary     binValue   = BinaryHelper.toBinary(putRequest.getValue());
        Binary     binResult;

        if (((ChannelStub) proxy.getChannel()).isSerializerCompatible())
            {
            binResult = (Binary) proxy.invoke(binKey, BinaryProcessors.putIfAbsent(binValue, NamedCache.EXPIRY_DEFAULT));
            binResult = ExternalizableHelper.fromBinary(binResult, proxy.getCacheService().getSerializer());
            }
        else
            {
            binResult = (Binary) proxy.putIfAbsent(binKey, fromBinary(binValue));
            }
        complete(binResult, proxy, observer);
        }

    @SuppressWarnings("unchecked")
    protected void onQueryEntrySet(NamedCacheProxy proxy, NamedCacheRequest request, StreamObserver<NamedCacheResponse> observer)
        {
        QueryRequest  query      = unpack(request, QueryRequest.class);
        Filter<?>     filter     = query.hasFilter() ? fromByteString(query.getFilter()) : AlwaysFilter.INSTANCE();
        Comparator<?> comparator = query.hasComparator() ? fromByteString(query.getComparator()) : null;
        int           cacheId    = proxy.getCacheId();

        Consumer<Map.Entry<? extends Binary, ? extends Binary>> callback = entry ->
            {
            BinaryKeyAndValue keyAndValue = BinaryKeyAndValue.newBuilder()
                    .setKey(BinaryHelper.toByteString(entry.getKey()))
                    .setValue(BinaryHelper.toByteString(entry.getValue()))
                    .build();

            observer.onNext(NamedCacheResponse.newBuilder()
                    .setCacheId(cacheId)
                    .setType(ResponseType.Message)
                    .setMessage(Any.pack(keyAndValue))
                    .build());
            };

        if (comparator == null)
            {
            async(proxy).ifPresentOrElse(
                    asyncCache -> asyncCache.entrySet(filter, callback).join(),
                    () -> proxy.entrySet(filter).forEach(callback));
            }
        else
            {
            Set<Map.Entry<Binary, Binary>> set = proxy.entrySet(filter, comparator);
            for (Map.Entry<Binary, Binary> entry : set)
                {
                callback.accept(entry);
                }
            }
        observer.onCompleted();
        }

    @SuppressWarnings("unchecked")
    protected void onQueryKeySet(NamedCacheProxy proxy, NamedCacheRequest request, StreamObserver<NamedCacheResponse> observer)
        {
        QueryRequest query   = unpack(request, QueryRequest.class);
        Filter<?>    filter  = query.hasFilter() ? fromByteString(query.getFilter()) : AlwaysFilter.INSTANCE();
        int          cacheId = proxy.getCacheId();

        Consumer<Binary> callback = binary ->
            {
            observer.onNext(NamedCacheResponse.newBuilder()
                    .setCacheId(cacheId)
                    .setType(ResponseType.Message)
                    .setMessage(Any.pack(BinaryHelper.toBytesValue(binary)))
                    .build());
            };

        async(proxy).ifPresentOrElse(
            asyncCache -> asyncCache.keySet(filter, callback).join(),
            () -> proxy.keySet(filter).forEach(callback));
        observer.onCompleted();
        }

    @SuppressWarnings("unchecked")
    protected void onQueryValues(NamedCacheProxy proxy, NamedCacheRequest request, StreamObserver<NamedCacheResponse> observer)
        {
        QueryRequest query   = unpack(request, QueryRequest.class);
        Filter<?>    filter  = query.hasFilter() ? fromByteString(query.getFilter()) : AlwaysFilter.INSTANCE();
        int          cacheId = proxy.getCacheId();

        Consumer<Binary> callback = binary ->
            {
            observer.onNext(NamedCacheResponse.newBuilder()
                    .setCacheId(cacheId)
                    .setType(ResponseType.Message)
                    .setMessage(Any.pack(BinaryHelper.toBytesValue(binary)))
                    .build());
            };

        async(proxy).ifPresentOrElse(
            asyncCache -> asyncCache.values(filter, callback).join(),
            () -> proxy.values(filter).forEach(callback));
        observer.onCompleted();
        }

    protected void onRemove(NamedCacheProxy proxy, NamedCacheRequest request, StreamObserver<NamedCacheResponse> observer)
        {
        Binary binKey    = unpackBinary(request);
        Binary binResult = (Binary) proxy.remove(binKey);
        complete(binResult, proxy, observer);
        }

    protected void onRemoveMapping(NamedCacheProxy proxy, NamedCacheRequest request, StreamObserver<NamedCacheResponse> observer)
        {
        BinaryKeyAndValue keyAndValue = unpack(request, BinaryKeyAndValue.class);
        Binary            binKey      = BinaryHelper.toBinary(keyAndValue.getKey());
        Object            oValue      = fromByteString(keyAndValue.getValue());
        Binary            binResult   = (Binary) proxy.invoke(binKey, CacheProcessors.remove(oValue));
        Boolean           fRemoved    = fromBinary(binResult);
        complete(fRemoved, proxy, observer);
        }

    @SuppressWarnings("unchecked")
    protected void onReplace(NamedCacheProxy proxy, NamedCacheRequest request, StreamObserver<NamedCacheResponse> observer)
        {
        BinaryKeyAndValue keyAndValue = unpack(request, BinaryKeyAndValue.class);
        Binary            binKey      = BinaryHelper.toBinary(keyAndValue.getKey());
        Binary            binValue    = BinaryHelper.toBinary(keyAndValue.getValue());
        Object            oValue      = fromBinary(binValue);
        Binary            binResult   = (Binary) proxy.replace(binKey, oValue);
        complete(binResult, proxy, observer);
        }

    protected void onReplaceMapping(NamedCacheProxy proxy, NamedCacheRequest request, StreamObserver<NamedCacheResponse> observer)
        {
        ReplaceMappingRequest mappingRequest = unpack(request, ReplaceMappingRequest.class);
        Binary                binKey         = BinaryHelper.toBinary(mappingRequest.getKey());
        Binary                binValuePrev   = BinaryHelper.toBinary(mappingRequest.getPreviousValue());
        Object                oValuePrev     = fromBinary(binValuePrev);
        Binary                binValueNew    = BinaryHelper.toBinary(mappingRequest.getNewValue());
        Object                oValueNew      = fromBinary(binValueNew);

        Binary  binResult = (Binary) proxy.invoke(binKey, CacheProcessors.replace(oValuePrev, oValueNew));
        boolean fReplaced = fromBinary(binResult);
        complete(fReplaced, proxy, observer);
        }

    protected void onSize(NamedCacheProxy proxy, StreamObserver<NamedCacheResponse> observer)
        {
        complete(proxy.size(), proxy, observer);
        }

    protected void onTruncate(NamedCacheProxy proxy, StreamObserver<NamedCacheResponse> observer)
        {
        proxy.truncate();
        observer.onCompleted();
        }

    /**
     * Create a {@link NamedCacheResponse.Builder} with the cache
     * identifier set to the value of the cache identifier of
     * a specified {@link NamedCacheProxy}.
     *
     * @param proxy  the {@link NamedCacheProxy} to use to set the
     *               cache identifier
     *
     * @return a {@link NamedCacheResponse.Builder} with the cache
     *         identifier set to the {@link NamedCacheProxy} cache
     *         identifier
     */
    protected NamedCacheResponse.Builder response(NamedCacheProxy proxy)
        {
        return response(proxy.getCacheId());
        }

    /**
     * Create a {@link NamedCacheResponse.Builder} with the cache
     * identifier set to the specified value.
     *
     * @param cacheId  the cache identifier
     *
     * @return a {@link NamedCacheResponse.Builder} with the cache
     *         identifier set to the specified value
     */
    protected NamedCacheResponse.Builder response(int cacheId)
        {
        return NamedCacheResponse.newBuilder().setCacheId(cacheId);
        }

    /**
     * Send a {@link NamedCacheResponse} containing a {@link BoolValue} to
     * a {@link StreamObserver} and then complete the observer.
     *
     * @param f         the boolean value to use to send a response before completing
     * @param proxy     the {@link NamedCacheProxy} to use to obtain a cache identifier
     * @param observer  the {@link StreamObserver} to complete
     */
    protected void complete(boolean f, NamedCacheProxy proxy, StreamObserver<NamedCacheResponse> observer)
        {
        complete(BoolValue.of(f), proxy, observer);
        }

    /**
     * Send a {@link NamedCacheResponse} containing an {@link Int32Value} to
     * a {@link StreamObserver} and then complete the observer.
     *
     * @param n         the int value to use to send a response before completing
     * @param proxy     the {@link NamedCacheProxy} to use to obtain a cache identifier
     * @param observer  the {@link StreamObserver} to complete
     */
    protected void complete(int n, NamedCacheProxy proxy, StreamObserver<NamedCacheResponse> observer)
        {
        complete(Int32Value.of(n), proxy, observer);
        }

    /**
     * Send a {@link NamedCacheResponse} containing a {@link BytesValue} to
     * a {@link StreamObserver} and then complete the observer.
     *
     * @param binary   the binary value to use to send a response before completing
     * @param proxy     the {@link NamedCacheProxy} to use to obtain a cache identifier
     * @param observer  the {@link StreamObserver} to complete
     */
    protected void complete(Binary binary, NamedCacheProxy proxy, StreamObserver<NamedCacheResponse> observer)
        {
        complete(BinaryHelper.toBytesValue(binary), proxy, observer);
        }

    /**
     * Send a {@link NamedCacheResponse} containing a {@link BinaryKeyAndValue} to
     * a {@link StreamObserver} and then complete the observer.
     *
     * @param request   the {@link Request} containing the result
     * @param observer  the {@link StreamObserver} to complete
     */
    protected void complete(Request request, StreamObserver<NamedCacheResponse> observer)
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
     * Send a {@link NamedCacheResponse} containing a {@link BinaryKeyAndValue} to
     * a {@link StreamObserver} and then complete the observer.
     *
     * @param binKey    the binary key to use to send a response before completing
     * @param request   the {@link Request} containing the result
     * @param proxy     the {@link NamedCacheProxy} to use to obtain a cache identifier
     * @param observer  the {@link StreamObserver} to complete
     */
    protected void complete(Binary binKey, Request request, NamedCacheProxy proxy, StreamObserver<NamedCacheResponse> observer)
        {
        Response response = request.ensureResponse();
        if (response.isFailure())
            {
            observer.onError((Throwable) response.getResult());
            }
        else
            {
            Binary binValue = (Binary) response.getResult();
            completeKeyValue(binKey, binValue, proxy, observer);
            }
        }

    /**
     * Send a {@link NamedCacheResponse} containing a {@link BinaryKeyAndValue} to
     * a {@link StreamObserver} and then complete the observer.
     *
     * @param binKey    the binary key to use to send a response before completing
     * @param binValue  the {@link Binary} value
     * @param proxy     the {@link NamedCacheProxy} to use to obtain a cache identifier
     * @param observer  the {@link StreamObserver} to complete
     */
    protected void completeKeyValue(Binary binKey, Binary binValue, NamedCacheProxy proxy, StreamObserver<NamedCacheResponse> observer)
        {
        BinaryKeyAndValue keyAndValue = BinaryKeyAndValue.newBuilder()
                .setKey(BinaryHelper.toByteString(binKey))
                .setValue(BinaryHelper.toByteString(binValue))
                .build();
        complete(keyAndValue, proxy, observer);
        }

    /**
     * Send a {@link NamedCacheResponse} containing a {@link BytesValue} to
     * a {@link StreamObserver} and then complete the observer.
     *
     * @param message   the {@link Message} value to use to send a response before completing
     * @param proxy     the {@link NamedCacheProxy} to use to obtain a cache identifier
     * @param observer  the {@link StreamObserver} to complete
     */
    protected void complete(Message message, NamedCacheProxy proxy, StreamObserver<NamedCacheResponse> observer)
        {
        observer.onNext(response(proxy.getCacheId())
                .setType(ResponseType.Message)
                .setMessage(Any.pack(message))
                .build());
        observer.onCompleted();
        }

    /**
     * Send a {@link NamedCacheResponse} containing a stream of
     * {@link BinaryKeyAndValue} to a {@link StreamObserver}
     * and then complete the observer.
     *
     * @param request   the {@link Request} containing the result
     * @param proxy     the {@link NamedCacheProxy} to use to obtain a cache identifier
     * @param observer  the {@link StreamObserver} to complete
     */
    @SuppressWarnings("unchecked")
    protected void completeMapStream(Request request, NamedCacheProxy proxy, StreamObserver<NamedCacheResponse> observer)
        {
        Response response = request.ensureResponse();
        if (response.isFailure())
            {
            observer.onError((Throwable) response.getResult());
            }
        else
            {
            Map<Binary, Binary> map = (Map<Binary, Binary>) response.getResult();
            completeMapStream(map, proxy, observer);
            }
        }

    /**
     * Send a {@link NamedCacheResponse} containing a stream of
     * {@link BinaryKeyAndValue} to a {@link StreamObserver}
     * and then complete the observer.
     *
     * @param map       the map of binary keys and values to stream to the observer
     * @param proxy     the {@link NamedCacheProxy} to use to obtain a cache identifier
     * @param observer  the {@link StreamObserver} to complete
     */
    protected void completeMapStream(Map<Binary, Binary> map, NamedCacheProxy proxy, StreamObserver<NamedCacheResponse> observer)
        {
        int cacheId = proxy.getCacheId();
        for (Map.Entry<Binary, Binary> entry : map.entrySet())
            {
            BinaryKeyAndValue keyAndValue = BinaryKeyAndValue.newBuilder()
                    .setKey(BinaryHelper.toByteString(entry.getKey()))
                    .setValue(BinaryHelper.toByteString(entry.getValue()))
                    .build();

            observer.onNext(response(cacheId)
                    .setType(ResponseType.Message)
                    .setMessage(Any.pack(keyAndValue))
                    .build());

            }
        observer.onCompleted();
        }

    /**
     * Send a {@link NamedCacheResponse} containing a stream of
     * {@link BinaryKeyAndValue} to a {@link StreamObserver}
     * and then complete the observer.
     *
     * @param request   the {@link Request} containing the result
     * @param proxy     the {@link NamedCacheProxy} to use to obtain a cache identifier
     * @param observer  the {@link StreamObserver} to complete
     */
    @SuppressWarnings("unchecked")
    protected void completeSetStream(Request request, NamedCacheProxy proxy, StreamObserver<NamedCacheResponse> observer)
        {
        Response response = request.ensureResponse();
        if (response.isFailure())
            {
            observer.onError((Throwable) response.getResult());
            }
        else
            {
            completeSetStream((Set<Binary>) response.getResult(), proxy, observer);
            }
        }

    /**
     * Send a {@link NamedCacheResponse} containing a stream of
     * {@link BinaryKeyAndValue} to a {@link StreamObserver}
     * and then complete the observer.
     *
     * @param set       the set of binary instances to stream to the observer
     * @param proxy     the {@link NamedCacheProxy} to use to obtain a cache identifier
     * @param observer  the {@link StreamObserver} to complete
     */
    protected void completeSetStream(Set<Binary> set, NamedCacheProxy proxy, StreamObserver<NamedCacheResponse> observer)
        {
        int cacheId = proxy.getCacheId();
        for (Binary binary : set)
            {
            observer.onNext(response(cacheId)
                    .setType(ResponseType.Message)
                    .setMessage(Any.pack(BinaryHelper.toBytesValue(binary)))
                    .build());

            }
        observer.onCompleted();
        }

    /**
     * Obtain an async version of a {@link NamedCacheProxy}.
     *
     * @param proxy  the {@link NamedCacheProxy} to get the async cache for
     *
     * @return an {@link Optional} containing the {@link AsyncNamedCache} or
     *         an empty {@link Optional} if the underlying cache does not
     *         support async behaviour.
     */
    @SuppressWarnings("unchecked")
    protected Optional<AsyncNamedCache<Binary, Binary>> async(NamedCacheProxy proxy)
        {
        try
            {
            return Optional.of(proxy.async());
            }
        catch (UnsupportedOperationException e)
            {
            return Optional.empty();
            }
        }

    /**
     * Return a {@link BytesValue} from a {@link NamedCacheRequest}
     * converted to a {@link Binary}.
     *
     * @param request  the {@link NamedCacheRequest} to get the {@link BytesValue} from
     * @return the {@link BytesValue} from a {@link NamedCacheRequest}
     *         converted to a {@link Binary}
     */
    protected Binary unpackBinary(NamedCacheRequest request)
        {
        BytesValue binaryValue = unpack(request, BytesValue.class);
        return BinaryHelper.toBinary(binaryValue.getValue());
        }

    /**
     * Unpack the message field from a {@link NamedCacheRequest}.
     *
     * @param request  the {@link NamedCacheRequest} to get the message from
     * @param type     the expected type of the message
     * @param <T>      the expected type of the message
     *
     * @return the unpacked message
     */
    protected <T extends Message> T unpack(NamedCacheRequest request, Class<T> type)
        {
        try
            {
            Any any = request.getMessage();
            return any.unpack(type);
            }
        catch (InvalidProtocolBufferException e)
            {
            throw Exceptions.ensureRuntimeException(e, "Could not unpack message field of type " + type.getName());
            }
        }

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

    // ----- inner class: ChannelStub ---------------------------------------

    /**
     * A stub of an Extend {@link Channel} used to handle event messages.
     */
    protected class ChannelStub
            extends Channel
        {
        /**
         * Create the {@link ChannelStub}.
         *
         * @param cacheId      the cache identifier
         * @param fCompatible  {@code true} if the client's serializer is compatible with the
         *                     underlying cache service serializer
         */
        public ChannelStub(int cacheId, boolean fCompatible)
            {
            com.tangosol.coherence.component.net.extend.protocol.NamedCacheProtocol protocol
                    = new com.tangosol.coherence.component.net.extend.protocol.NamedCacheProtocol();

            NamedCacheFactory factory = new NamedCacheFactory();
            factory.setProtocol(protocol);
            factory.setVersion(protocol.getCurrentVersion());
            setMessageFactory(factory);
            m_cacheId     = cacheId;
            m_fCompatible = fCompatible;
            }

        @Override
        public void send(com.tangosol.net.messaging.Message message)
            {
            if (message instanceof NamedCacheFactory.MapEvent event)
                {
                int     nId        = event.getId();
                boolean fSynthetic = event.isSynthetic();
                boolean fPriming   = event.isPriming();
                boolean fUpdate    = nId == MapEvent.ENTRY_UPDATED;
                Object  oKey       = event.getKey();
                Object  oValueOld  = event.getValueOld();
                Object  oValueNew  = event.getValueNew();

                boolean fVersionUpdate = fSynthetic && fPriming && fUpdate &&
                                oKey == null && oValueNew == null && oValueOld == null;

                MapEventMessage.Builder builder = MapEventMessage.newBuilder()
                        .setId(nId)
                        .setKey(BinaryHelper.toByteString((Binary) event.getKey()))
                        .setPriming(fPriming)
                        .setSynthetic(fSynthetic)
                        .setExpired(event.isExpired())
                        .setVersionUpdate(fVersionUpdate)
                        .setTransformationState(MapEventMessage.TransformationState.forNumber(event.getTransformationState()));

                Binary binOld = (Binary) event.getValueOld();
                if (binOld != null)
                    {
                    builder.setOldValue(BinaryHelper.toByteString(binOld));
                    }

                Binary binNew = (Binary) event.getValueNew();
                if (binNew != null)
                    {
                    builder.setNewValue(BinaryHelper.toByteString(binNew));
                    }

                long[] filterIds = event.getFilterIds();
                if (filterIds != null && filterIds.length > 0)
                    {
                    builder.addAllFilterIds(LongStream.of(filterIds).boxed().toList());
                    }

                NamedCacheResponse response = NamedCacheResponse.newBuilder()
                        .setCacheId(m_cacheId)
                        .setType(ResponseType.MapEvent)
                        .setMessage(Any.pack(builder.build()))
                        .build();

                m_eventObserver.onNext(response);
                }
            }

        @Override
        public com.tangosol.net.messaging.Connection getConnection()
            {
            return new ConnectionStub();
            }

        @Override
        public Peer getConnectionManager()
            {
            return new ConnectionManagerStub();
            }

        /**
         * Return {@code true} if the client's serializer is compatible with the
         * underlying cache service serializer.
         *
         * @return {@code true} if the client's serializer is compatible with the
         *         underlying cache service serializer
         */
        public boolean isSerializerCompatible()
            {
            return m_fCompatible;
            }

        // ----- data members -----------------------------------------------

        /**
         * The cache identifier.
         */
        protected final int m_cacheId;

        /**
         * {@code true} if the client's serializer is compatible with the underlying cache service serializer.
         */
        protected final boolean m_fCompatible;
        }

    // ----- inner class: ConnectionStub ------------------------------------

    /**
     * A stub for a {@link Connection}.
     */
    protected static class ConnectionStub
            extends Connection
        {
        @Override
        public String toString()
            {
            return "GrpcConnection";
            }

        @Override
        public ConnectionManager getConnectionManager()
            {
            return new ConnectionManagerStub();
            }
        }

    // ----- inner class: ConnectionManagerStub -----------------------------

    /**
     * A stub for a {@link ConnectionManager}.
     */
    protected static class ConnectionManagerStub
            extends Acceptor
        {
        public ConnectionManagerStub()
            {
            super("GrpcAcceptor", null, false);
            }

        @Override
        public String toString()
            {
            return "GrpcAcceptor";
            }
        }

    // ----- inner class: CacheListener -------------------------------------

    /**
     * A {@link NamedCacheDeactivationListener} to receive truncate and destroy
     * events for the cache.
     */
    @SuppressWarnings("rawtypes")
    protected class CacheListener
            implements NamedCacheDeactivationListener
        {
        public CacheListener(int cacheId)
            {
            m_cacheId = cacheId;
            }

        @Override
        public void entryInserted(MapEvent evt)
            {
            // unused
            }

        @Override
        public void entryUpdated(MapEvent evt)
            {
            // Cache truncated
            send(ResponseType.Truncated);
            }

        @Override
        public void entryDeleted(MapEvent evt)
            {
            // Cache destroyed
            send(ResponseType.Destroyed);
            }

        private void send(ResponseType type)
            {
            NamedCacheResponse event = NamedCacheResponse.newBuilder()
                    .setCacheId(m_cacheId)
                    .setType(type)
                    .build();
            m_eventObserver.onNext(event);
            }

        /**
         * The cache identifier.
         */
        private final int m_cacheId;
        }


    // ----- data members ---------------------------------------------------

    /**
     * The lock to control access to state.
     */
    private static final Lock f_lock = new ReentrantLock();

    /**
     * The parent {@link GrpcService}.
     */
    private GrpcService m_service;

    /**
     * The {@link ExtensibleConfigurableCacheFactory} owning the cache.
     */
    private ExtensibleConfigurableCacheFactory m_ccf;

    /**
     * The client's serializer.
     */
    private Serializer m_serializer;

    /**
     * The {@link Connection} to use to send responses.
     */
    private Connection m_connection;

    /**
     * The {@link CacheServiceProxy}.
     */
    private CacheServiceProxy m_proxy;

    /**
     * An array of {@link NamedCacheProxy} instances indexed by the cache identifier.
     */
    private final LongArray<NamedCacheProxy> m_aProxy = new SparseArray<>();

    /**
     * A bit-set containing destroyed cache identifiers.
     */
    private final BitSet m_destroyedIds = new BitSet();

    /**
     * A {@link StreamObserver} for processing event messages.
     */
    private StreamObserver<NamedCacheResponse> m_eventObserver;

    /**
     * The optional container {@link Context} for this protocol.
     */
    private Context m_context;
    }
