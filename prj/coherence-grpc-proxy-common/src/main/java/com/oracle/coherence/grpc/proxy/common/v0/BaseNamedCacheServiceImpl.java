/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.proxy.common.v0;

import com.google.protobuf.BoolValue;
import com.google.protobuf.ByteString;
import com.google.protobuf.BytesValue;

import com.google.protobuf.Empty;

import com.oracle.coherence.common.base.Logger;

import com.oracle.coherence.grpc.BinaryHelper;
import com.oracle.coherence.grpc.proxy.common.BaseGrpcServiceImpl;
import com.oracle.coherence.grpc.v0.CacheRequestHolder;
import com.oracle.coherence.grpc.ErrorsHelper;
import com.oracle.coherence.grpc.SafeStreamObserver;

import com.oracle.coherence.grpc.messages.cache.v0.AddIndexRequest;
import com.oracle.coherence.grpc.messages.cache.v0.AggregateRequest;
import com.oracle.coherence.grpc.messages.cache.v0.ClearRequest;
import com.oracle.coherence.grpc.messages.cache.v0.ContainsEntryRequest;
import com.oracle.coherence.grpc.messages.cache.v0.DestroyRequest;
import com.oracle.coherence.grpc.messages.cache.v0.GetAllRequest;
import com.oracle.coherence.grpc.messages.cache.v0.MapListenerRequest;
import com.oracle.coherence.grpc.messages.cache.v0.MapListenerResponse;
import com.oracle.coherence.grpc.messages.cache.v0.PutAllRequest;
import com.oracle.coherence.grpc.messages.cache.v0.RemoveIndexRequest;

import com.tangosol.internal.util.processor.BinaryProcessors;

import com.tangosol.io.Serializer;

import com.tangosol.net.AsyncNamedCache;
import com.tangosol.net.CacheService;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.Member;
import com.tangosol.net.NamedCache;
import com.tangosol.net.PartitionedService;

import com.tangosol.util.Binary;
import com.tangosol.util.Filter;
import com.tangosol.util.Filters;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.InvocableMap.EntryProcessor;
import com.tangosol.util.ValueExtractor;
import com.tangosol.util.filter.AlwaysFilter;

import io.grpc.Status;

import io.grpc.stub.StreamObserver;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;

import java.util.stream.Collectors;

import static com.oracle.coherence.grpc.proxy.common.v0.ResponseHandlers.handleUnary;


/**
 * A base class for gRPC {@link NamedCacheService} implementations.
 *
 * @author Jonathan Knight  2024.02.08
 */
@SuppressWarnings({"ConstantValue"})
public abstract class BaseNamedCacheServiceImpl
        extends BaseGrpcServiceImpl
        implements NamedCacheService
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a {@link BaseNamedCacheServiceImpl}.
     *
     * @param dependencies the {@link NamedCacheService.Dependencies} to use to configure the service
     */
    public BaseNamedCacheServiceImpl(NamedCacheService.Dependencies dependencies)
        {
        super(dependencies, MBEAN_NAME, "GrpcNamedCacheProxy");
        m_nEventsHeartbeat = dependencies.getEventsHeartbeat();
        }

    // ----- BaseGrpcServiceImpl implementation -----------------------------

    // ----- addIndex -------------------------------------------------------

    /**
     * Execute the {@link AddIndexRequest} request.
     *
     * @param holder  the {@link CacheRequestHolder} containing the {@link AddIndexRequest} request
     *
     * @return {@link BinaryHelper#EMPTY}
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    protected Empty addIndex(CacheRequestHolder<AddIndexRequest, Void> holder)
        {
        AddIndexRequest request    = holder.getRequest();
        NamedCache      cache      = holder.getCache();
        Serializer      serializer = holder.getSerializer();
        ValueExtractor  extractor  = ensureValueExtractor(request.getExtractor(), serializer);
        Comparator<?>   comparator = BinaryHelper.fromByteString(request.getComparator(), serializer);

        cache.addIndex(extractor, request.getSorted(), comparator);
        return BinaryHelper.EMPTY;
        }

    // ----- aggregate ------------------------------------------------------

    /**
     * Execute the filtered {@link AggregateRequest} request.
     *
     * @param request  the {@link AggregateRequest}
     *
     * @return a {@link CompletionStage} that completes with a {@link CacheRequestHolder} containing
     *         the serialized result of executing request
     */
    protected CompletionStage<BytesValue> aggregateWithFilter(AggregateRequest request, Executor executor)
        {
        return createHolderAsync(request, request.getScope(), request.getCache(), request.getFormat())
                .thenComposeAsync(h -> aggregateWithFilter(h, executor), executor)
                .handleAsync(ResponseHandlers::handleError, executor);
        }

    /**
     * Execute the filtered {@link AggregateRequest} request.
     *
     * @param holder  the {@link CacheRequestHolder} containing the {@link ContainsEntryRequest} request
     *
     * @return a {@link CompletionStage} that completes with a {@link CacheRequestHolder} containing
     *         the serialized result of executing request
     */
    protected CompletionStage<BytesValue> aggregateWithFilter(CacheRequestHolder<AggregateRequest, Void> holder,
                                                              Executor executor)
        {
        AggregateRequest request     = holder.getRequest();
        ByteString       filterBytes = request.getFilter();

        // if no filter is present in the request use an AlwaysFilter
        Filter<Binary> filter = filterBytes.isEmpty()
                                ? Filters.always()
                                : BinaryHelper.fromByteString(filterBytes, holder.getSerializer());

        ByteString processorBytes = request.getAggregator();
        InvocableMap.EntryAggregator<Binary, Binary, Binary> aggregator
                = BinaryHelper.fromByteString(processorBytes, holder.getSerializer());

        return holder.runAsync(holder.getAsyncCache().aggregate(filter, aggregator))
                .thenApplyAsync(h -> BinaryHelper.toBytesValue(h.getResult(), h.getSerializer()), executor);
        }

    /**
     * Execute the key-based {@link AggregateRequest} request.
     *
     * @param request  the {@link AggregateRequest}
     *
     * @return a {@link CompletionStage} that completes with a {@link CacheRequestHolder} containing
     *         the serialized result of executing request
     */
    protected CompletionStage<BytesValue> aggregateWithKeys(AggregateRequest request, Executor executor)
        {
        return createHolderAsync(request, request.getScope(), request.getCache(), request.getFormat())
                .thenComposeAsync(h -> aggregateWithKeys(h, executor), executor)
                .handleAsync(ResponseHandlers::handleError, executor);
        }

    /**
     * Execute the filtered {@link AggregateRequest} request.
     *
     * @param holder  the {@link CacheRequestHolder} containing the {@link ContainsEntryRequest} request
     *
     * @return a {@link CompletionStage} that completes with a {@link CacheRequestHolder} containing
     *         the serialized result of executing request
     */
    protected CompletionStage<BytesValue> aggregateWithKeys(CacheRequestHolder<AggregateRequest, Void> holder,
                                                            Executor executor)
        {
        AggregateRequest request = holder.getRequest();
        List<Binary>     keys    = request.getKeysList()
                .stream()
                .map(holder::convertKeyDown)
                .collect(Collectors.toList());

        InvocableMap.EntryAggregator<Binary, Binary, Binary> aggregator
                = BinaryHelper.fromByteString(request.getAggregator(), holder.getSerializer());

        return holder.runAsync(holder.getAsyncCache().aggregate(keys, aggregator))
                .thenApplyAsync(h -> BinaryHelper.toBytesValue(h.getResult(), h.getSerializer()), executor);
        }

    // ----- clear ----------------------------------------------------------

    @Override
    public void clear(ClearRequest request, StreamObserver<Empty> observer)
        {
        StreamObserver<Empty> safeObserver = SafeStreamObserver.ensureSafeObserver(observer);
        try
            {
            NamedCache<Binary, Binary> cache = getPassThroughCache(request.getScope(), request.getCache());
            cache.clear();
            handleUnary(Empty.getDefaultInstance(), null, safeObserver);
            }
        catch (Throwable t)
            {
            handleUnary(Empty.getDefaultInstance(), t, safeObserver);
            }
        }

    // ----- destroy --------------------------------------------------------

    @Override
    public void destroy(DestroyRequest request, StreamObserver<Empty> observer)
        {
        StreamObserver<Empty> safeObserver = SafeStreamObserver.ensureSafeObserver(observer);
        String sCacheName = request.getCache();
        try
            {
            if (sCacheName == null || sCacheName.trim().isEmpty())
                {
                throw Status.INVALID_ARGUMENT
                        .withDescription(INVALID_CACHE_NAME_MESSAGE)
                        .asRuntimeException();
                }

            Logger.finer("Destroying cache " + sCacheName);
            ConfigurableCacheFactory   ccf   = getCCF(request.getScope());
            NamedCache<Binary, Binary> cache = ccf.ensureCache(sCacheName, null);
            ccf.destroyCache(cache);
            handleUnary(Empty.getDefaultInstance(), null, safeObserver);
            Logger.info("Destroyed cache " + sCacheName);
            }
        catch (Throwable t)
            {
            Logger.err("Caught exception destroying cache \"" + sCacheName + "\"", t);
            handleUnary(Empty.getDefaultInstance(), t, safeObserver);
            }
        }

    // ----- events ---------------------------------------------------------

    @Override
    public StreamObserver<MapListenerRequest> events(StreamObserver<MapListenerResponse> observer)
        {
        return new MapListenerProxy(this, SafeStreamObserver.ensureSafeObserver(observer), m_nEventsHeartbeat);
        }

    // ----- getAll ---------------------------------------------------------

    /**
     * Convert the keys for a {@link GetAllRequest} from the request's serialization format
     * to the cache's serialization format.
     *
     * @param holder the {@link CacheRequestHolder} containing the {@link GetAllRequest}
     *               containing the keys to convert
     * @return A {@link CompletionStage} that completes with the converted keys
     */
    protected List<Binary> convertKeysToBinary(CacheRequestHolder<GetAllRequest, Void> holder)
        {
        GetAllRequest request = holder.getRequest();
        return request.getKeyList()
                .stream()
                .map(holder::convertKeyDown)
                .collect(Collectors.toList());
        }

    /**
     * Perform a {@code putAll} operation on a partitioned cache.
     * <p>
     * This method will split the map of entries into a map per storage member
     * and execute the putAll invocation for each member separately. This is
     * more efficient than sending the map of entries to all members.
     *
     * @param holder  the {@link CacheRequestHolder} containing the {@link PutAllRequest} request
     * @param map     the map of {@link Binary} keys and values to put into the cache
     *
     * @return a {@link CompletionStage} that completes when the putAll operation completes
     */
    protected CompletionStage<Empty> partitionedPutAll(CacheRequestHolder<PutAllRequest, Void> holder,
                                                       Map<Binary, Binary> map)
        {
        try
            {
            Map<Member, Map<Binary, Binary>> mapByOwner = new HashMap<>();

            PartitionedService service = (PartitionedService) holder.getCache().getCacheService();

            for (Map.Entry<Binary, Binary> entry : map.entrySet())
                {
                Binary key    = entry.getKey();
                Member member = service.getKeyOwner(key);

                // member could be null here, indicating that the owning partition is orphaned
                Map<Binary, Binary> mapForMember = mapByOwner.computeIfAbsent(member, m -> new HashMap<>());
                mapForMember.put(key, entry.getValue());
                }


            AsyncNamedCache<Binary, Binary> cache   = holder.getAsyncCache();
            long                            cMillis = holder.getRequest().getTtl();
            CompletableFuture<?>[]          futures = mapByOwner.values()
                    .stream()
                    .map(mapForMember -> plainPutAll(cache, mapForMember, cMillis))
                    .map(CompletionStage::toCompletableFuture)
                    .toArray(CompletableFuture[]::new);

            return CompletableFuture.allOf(futures)
                    .thenApply(v -> BinaryHelper.EMPTY);
            }
        catch (Throwable t)
            {
            CompletableFuture<Empty> future = new CompletableFuture<>();
            future.completeExceptionally(t);
            return future;
            }
        }

    /**
     * Perform a {@code putAll} operation on a partitioned cache.
     *
     * @param cache    the {@link AsyncNamedCache} to update
     * @param map      the map of {@link Binary} keys and values to put into the cache
     * @param cMillis  the expiry delay to set on the entries
     *
     * @return a {@link CompletionStage} that completes when the {@code putAll} operation completes
     */
    protected CompletionStage<Empty> plainPutAll(AsyncNamedCache<Binary, Binary> cache,
            Map<Binary, Binary> map, long cMillis)
        {
        return cache.invokeAll(map.keySet(), BinaryProcessors.putAll(map, cMillis))
                .thenApplyAsync(v -> BinaryHelper.EMPTY, f_executor);
        }

    // ----- removeIndex ----------------------------------------------------

    /**
     * Execute the {@link RemoveIndexRequest} request.
     *
     * @param holder  the {@link CacheRequestHolder} containing the {@link RemoveIndexRequest} request
     *
     * @return {@link BinaryHelper#EMPTY}
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    protected Empty removeIndex(CacheRequestHolder<RemoveIndexRequest, Void> holder)
        {
        RemoveIndexRequest         request   = holder.getRequest();
        NamedCache<Binary, Binary> cache     = holder.getCache();
        ValueExtractor             extractor = ensureValueExtractor(request.getExtractor(), holder.getSerializer());

        cache.removeIndex(extractor);
        return BinaryHelper.EMPTY;
        }

    // ----- helper methods -------------------------------------------------

    /**
     * A helper method that always returns {@link Empty}.
     * <p>
     * This method is to make {@link CompletionStage} handler code
     * a little more elegant as it can use this method as a method
     * reference.
     *
     * @param ignored  the value
     * @param <V>      the type of the value
     *
     * @return an {@link Empty} instance.
     */
    protected <V> Empty empty(V ignored)
        {
        return BinaryHelper.EMPTY;
        }

    /**
     * Execute the {@link Runnable} and return an {@link Empty} instance.
     *
     * @param task  the runnable to execute
     *
     * @return always returns an {@link Empty} instance
     */
    protected Empty execute(Runnable task)
        {
        task.run();
        return BinaryHelper.EMPTY;
        }

    /**
     * Execute the {@link Callable} and return the result.
     *
     * @param task  the runnable to execute
     * @param <T>   the result type
     *
     * @return the result of executing the {@link Callable}
     */
    protected <T> T execute(Callable<T> task)
        {
        try
            {
            return task.call();
            }
        catch (Throwable t)
            {
            throw ErrorsHelper.ensureStatusRuntimeException(t);
            }
        }

    /**
     * Deserialize a {@link Binary} to a boolean value.
     *
     * @param binary      the {@link Binary} to deserialize
     * @param serializer  the {@link Serializer} to use
     *
     * @return the deserialized boolean value
     */
    protected BoolValue toBoolValue(Binary binary, Serializer serializer)
        {
        return BoolValue.of(BinaryHelper.fromBinary(binary, serializer));
        }

    /**
     * Obtain a {@link ValueExtractor} from the serialized data in a {@link ByteString}.
     *
     * @param bytes       the {@link ByteString} containing the serialized {@link ValueExtractor}
     * @param serializer  the serializer to use
     *
     * @return a deserialized {@link ValueExtractor}
     *
     * @throws io.grpc.StatusRuntimeException if the {@link ByteString} is null or empty
     */
    @SuppressWarnings("ConstantConditions")
    public ValueExtractor<?, ?> ensureValueExtractor(ByteString bytes, Serializer serializer)
        {
        if (bytes == null || bytes.isEmpty())
            {
            throw Status.INVALID_ARGUMENT
                    .withDescription(MISSING_EXTRACTOR_MESSAGE)
                    .asRuntimeException();
            }

        return BinaryHelper.fromByteString(bytes, serializer);
        }

    /**
     * Obtain a {@link Filter} from the serialized data in a {@link ByteString}.
     * <p>
     * If the {@link ByteString} is {@code null} or {@link ByteString#EMPTY} then
     * an {@link AlwaysFilter} is returned.
     *
     * @param bytes       the {@link ByteString} containing the serialized {@link Filter}
     * @param serializer  the serializer to use
     * @param <T>         the {@link Filter} type
     *
     * @return a deserialized {@link Filter}
     */
    @Override
    public <T> Filter<T> ensureFilter(ByteString bytes, Serializer serializer)
        {
        if (bytes == null || bytes.isEmpty())
            {
            return Filters.always();
            }
        return BinaryHelper.fromByteString(bytes, serializer);
        }

    /**
     * Obtain a {@link Filter} from the serialized data in a {@link ByteString}.
     *
     * @param bytes       the {@link ByteString} containing the serialized {@link Filter}
     * @param serializer  the serializer to use
     * @param <T>         the {@link Filter} type
     *
     * @return a deserialized {@link Filter} or {@code null} if no filter is set
     */
    @Override
    public <T> Filter<T> getFilter(ByteString bytes, Serializer serializer)
        {
        if (bytes == null || bytes.isEmpty())
            {
            return null;
            }
        return BinaryHelper.fromByteString(bytes, serializer);
        }

    /**
     * Obtain a {@link Comparator} from the serialized data in a {@link ByteString}.
     *
     * @param bytes       the {@link ByteString} containing the serialized {@link Comparator}
     * @param serializer  the serializer to use
     * @param <T>         the {@link Comparator} type
     *
     * @return a deserialized {@link Comparator} or {@code null} if the {@link ByteString}
     *         is {@code null} or {@link ByteString#EMPTY}
     */
    public <T> Comparator<T> deserializeComparator(ByteString bytes, Serializer serializer)
        {
        if (bytes == null || bytes.isEmpty())
            {
            return null;
            }
        return BinaryHelper.fromByteString(bytes, serializer);
        }

    /**
     * Obtain an {@link AsyncNamedCache}.
     *
     * @param scope      the scope name to use to obtain the CCF to get the cache from
     * @param cacheName  the name of the cache
     *
     * @return the {@link AsyncNamedCache} with the specified name
     */
    protected CompletionStage<AsyncNamedCache<Binary, Binary>> getAsyncCache(String scope, String cacheName)
        {
        return CompletableFuture.supplyAsync(() -> getPassThroughCache(scope, cacheName).async(), f_executor);
        }

    /**
     * Cast an {@link EntryProcessor} to an
     * {@link EntryProcessor} that returns a
     * {@link Binary} result.
     *
     * @param ep  the {@link EntryProcessor} to cast
     *
     * @return a {@link EntryProcessor} that returns
     *         a {@link Binary} result
     */
    @SuppressWarnings("unchecked")
    protected EntryProcessor<Binary, Binary, Binary> castProcessor(EntryProcessor<Binary, Binary, ?> ep)
        {
        return (EntryProcessor<Binary, Binary, Binary>) ep;
        }

    /**
     * Asynchronously create a {@link CacheRequestHolder} for a given request.
     *
     * @param request     the request object to add to the holder
     * @param sScope      the scope name to use to identify the CCF to obtain the cache from
     * @param sCacheName  the name of the cache that the request executes against
     * @param format      the optional serialization format used by requests that contain a payload
     * @param <Req>       the type of the request
     *
     * @return a {@link CompletionStage} that completes when the {@link CacheRequestHolder} has
     *         been created
     */
    public <Req> CompletionStage<CacheRequestHolder<Req, Void>> createHolderAsync(Req request,
                                                                           String sScope,
                                                                           String sCacheName,
                                                                           String format)
        {
        return CompletableFuture.supplyAsync(() -> createRequestHolder(request, sScope, sCacheName, format), f_executor);
        }

    /**
     * Create a {@link CacheRequestHolder} for a given request.
     *
     * @param <Req>       the type of the request
     * @param request     the request object to add to the holder
     * @param sScope      the scope name to use to identify the CCF to obtain the cache from
     * @param sCacheName  the name of the cache that the request executes against
     * @param format      the optional serialization format used by requests that contain a payload
     *
     * @return the {@link CacheRequestHolder} holding the request
     */
    public <Req> CacheRequestHolder<Req, Void> createRequestHolder(Req    request,
                                                                   String sScope,
                                                                   String sCacheName,
                                                                   String format)
        {
        if (request == null)
            {
            throw Status.INVALID_ARGUMENT
                    .withDescription(INVALID_REQUEST_MESSAGE)
                    .asRuntimeException();
            }

        if (sCacheName == null || sCacheName.isEmpty())
            {
            throw Status.INVALID_ARGUMENT
                    .withDescription(INVALID_CACHE_NAME_MESSAGE)
                    .asRuntimeException();
            }

        NamedCache<Binary, Binary>      c              = getCache(sScope, sCacheName, true);
        NamedCache<Binary, Binary>      nonPassThrough = getCache(sScope, sCacheName, false);
        AsyncNamedCache<Binary, Binary> cache          = c.async();
        CacheService                    cacheService   = cache.getNamedCache().getCacheService();
        String                          cacheFormat    = CacheRequestHolder.getCacheFormat(cacheService);
        Serializer                      serializer     = getSerializer(format, cacheFormat, cacheService::getSerializer, cacheService::getContextClassLoader);

        return new CacheRequestHolder<>(request, cache, () -> nonPassThrough, format, serializer, f_executor);
        }

    // ----- constants ------------------------------------------------------

    /**
     * The name to use for the management MBean.
     */
    public static final String MBEAN_NAME = "type=GrpcNamedCacheProxy";

    public static final String INVALID_REQUEST_MESSAGE = "invalid request, the request cannot be null";

    public static final String MISSING_PROCESSOR_MESSAGE = "the request does not contain a serialized entry processor";

    public static final String MISSING_EXTRACTOR_MESSAGE = "the request does not contain a serialized ValueExtractor";

    public static final String MISSING_AGGREGATOR_MESSAGE = "the request does not contain a serialized ValueExtractor";

    // ----- data members ---------------------------------------------------

    private final long m_nEventsHeartbeat;
    }
