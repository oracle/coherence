/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.proxy;

import com.google.protobuf.BoolValue;
import com.google.protobuf.ByteString;
import com.google.protobuf.BytesValue;
import com.google.protobuf.Empty;
import com.google.protobuf.Int32Value;
import com.oracle.coherence.grpc.BinaryHelper;
import com.oracle.coherence.grpc.v0.CacheRequestHolder;
import com.oracle.coherence.grpc.ErrorsHelper;
import com.oracle.coherence.grpc.SafeStreamObserver;

import com.oracle.coherence.grpc.messages.cache.v0.AddIndexRequest;
import com.oracle.coherence.grpc.messages.cache.v0.AggregateRequest;
import com.oracle.coherence.grpc.messages.cache.v0.ContainsEntryRequest;
import com.oracle.coherence.grpc.messages.cache.v0.ContainsKeyRequest;
import com.oracle.coherence.grpc.messages.cache.v0.ContainsValueRequest;
import com.oracle.coherence.grpc.messages.cache.v0.Entry;
import com.oracle.coherence.grpc.messages.cache.v0.EntryResult;
import com.oracle.coherence.grpc.messages.cache.v0.EntrySetRequest;
import com.oracle.coherence.grpc.messages.cache.v0.GetAllRequest;
import com.oracle.coherence.grpc.messages.cache.v0.GetRequest;
import com.oracle.coherence.grpc.messages.cache.v0.InvokeAllRequest;
import com.oracle.coherence.grpc.messages.cache.v0.InvokeRequest;
import com.oracle.coherence.grpc.messages.cache.v0.IsEmptyRequest;
import com.oracle.coherence.grpc.messages.cache.v0.IsReadyRequest;
import com.oracle.coherence.grpc.messages.cache.v0.KeySetRequest;
import com.oracle.coherence.grpc.messages.cache.v0.OptionalValue;
import com.oracle.coherence.grpc.messages.cache.v0.PageRequest;
import com.oracle.coherence.grpc.messages.cache.v0.PutAllRequest;
import com.oracle.coherence.grpc.messages.cache.v0.PutIfAbsentRequest;
import com.oracle.coherence.grpc.messages.cache.v0.PutRequest;
import com.oracle.coherence.grpc.messages.cache.v0.RemoveIndexRequest;
import com.oracle.coherence.grpc.messages.cache.v0.RemoveMappingRequest;
import com.oracle.coherence.grpc.messages.cache.v0.RemoveRequest;
import com.oracle.coherence.grpc.messages.cache.v0.ReplaceMappingRequest;
import com.oracle.coherence.grpc.messages.cache.v0.ReplaceRequest;
import com.oracle.coherence.grpc.messages.cache.v0.SizeRequest;
import com.oracle.coherence.grpc.messages.cache.v0.TruncateRequest;
import com.oracle.coherence.grpc.messages.cache.v0.ValuesRequest;

import com.oracle.coherence.grpc.proxy.common.v0.BaseNamedCacheServiceImpl;
import com.oracle.coherence.grpc.proxy.common.v0.NamedCacheService;
import com.oracle.coherence.grpc.proxy.common.v0.PagedQueryHelper;

import com.tangosol.internal.util.processor.BinaryProcessors;
import com.tangosol.io.Serializer;
import com.tangosol.net.AsyncNamedCache;
import com.tangosol.net.PartitionedService;
import com.tangosol.net.grpc.GrpcDependencies;
import com.tangosol.util.Aggregators;
import com.tangosol.util.Binary;
import com.tangosol.util.Filter;
import com.tangosol.util.Filters;
import com.tangosol.util.InvocableMap.EntryProcessor;
import com.tangosol.util.extractor.IdentityExtractor;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.oracle.coherence.grpc.proxy.common.v0.ResponseHandlers.handleError;
import static com.oracle.coherence.grpc.proxy.common.v0.ResponseHandlers.handleErrorOrComplete;
import static com.oracle.coherence.grpc.proxy.common.v0.ResponseHandlers.handleSetOfEntries;
import static com.oracle.coherence.grpc.proxy.common.v0.ResponseHandlers.handleStream;
import static com.oracle.coherence.grpc.proxy.common.v0.ResponseHandlers.handleUnary;

import static com.tangosol.internal.util.processor.BinaryProcessors.BinaryContainsValueProcessor;
import static com.tangosol.internal.util.processor.BinaryProcessors.BinaryRemoveProcessor;
import static com.tangosol.internal.util.processor.BinaryProcessors.BinaryReplaceMappingProcessor;
import static com.tangosol.internal.util.processor.BinaryProcessors.BinaryReplaceProcessor;

/**
 * An async gRPC {@link NamedCacheService}.
 * <p>
 * This class uses {@link AsyncNamedCache} and asynchronous {@link CompletionStage}
 * wherever possible. This makes the code more complex but the advantages of not blocking the gRPC
 * request thread or the Coherence service thread will outweigh the downside of complexity.
 * <p>
 * The asynchronous processing of {@link CompletionStage}s is done using an
 * {@link com.oracle.coherence.grpc.proxy.common.DaemonPoolExecutor}
 * so as not to consume or block threads in the Fork Join Pool.
 * The {@link com.oracle.coherence.grpc.proxy.common.DaemonPoolExecutor} is
 * configurable so that its thread counts can be controlled.
 *
 * @author Jonathan Knight  2020.09.22
 * @since 20.06
 */
public class NettyNamedCacheService
        extends BaseNamedCacheServiceImpl
        implements NamedCacheService
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a {@link NettyNamedCacheService}.
     *
     * @param dependencies the {@link NamedCacheService.Dependencies} to use to configure the service
     */
    public NettyNamedCacheService(NamedCacheService.Dependencies dependencies)
        {
        super(dependencies);
        }

    // ----- factory methods ------------------------------------------------

    /**
     * Create an instance of {@link NettyNamedCacheService}
     * using the default dependencies configuration.
     *
     * @param deps  the {@link NamedCacheService.Dependencies} to use to create the service
     *
     * @return  an instance of {@link NettyNamedCacheService}
     */
    public static NettyNamedCacheService newInstance(NamedCacheService.Dependencies deps)
        {
        return new NettyNamedCacheService(deps);
        }

    /**
     * Create an instance of {@link NettyNamedCacheService}
     * using the default dependencies configuration.
     *
     * @return  an instance of {@link NettyNamedCacheService}
     */
    public static NettyNamedCacheService newInstance()
        {
        return newInstance(new NamedCacheService.DefaultDependencies(GrpcDependencies.ServerType.Asynchronous));
        }

    // ----- NamedCacheClient implementation --------------------------------

    // ----- addIndex -------------------------------------------------------

    @Override
    public void addIndex(AddIndexRequest request, StreamObserver<Empty> observer)
        {
        createHolderAsync(request, request.getScope(), request.getCache(), request.getFormat())
                .thenApplyAsync(this::addIndex, f_executor)
                .handleAsync((result, err) -> handleUnary(result, err, SafeStreamObserver.ensureSafeObserver(observer)));
        }

    // ----- aggregate ------------------------------------------------------

    @Override
    public void aggregate(AggregateRequest request, StreamObserver<BytesValue> observer)
        {
        StreamObserver<BytesValue> safeObserver   = SafeStreamObserver.ensureSafeObserver(observer);
        ByteString                 processorBytes = request.getAggregator();

        if (processorBytes.isEmpty())
            {
            safeObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("the request does not contain a serialized entry aggregator")
                    .asRuntimeException());
            }
        else
            {
            try
                {
                if (request.getKeysCount() != 0)
                    {
                    // aggregate with keys
                    aggregateWithKeys(request, f_executor)
                        .handleAsync((result, err) -> handleUnary(result, err, safeObserver), f_executor);                    }
                else
                    {
                    // aggregate with filter
                    aggregateWithFilter(request, f_executor)
                        .handleAsync((result, err) -> handleUnary(result, err, safeObserver), f_executor);                    }
                }
            catch (Throwable t)
                {
                safeObserver.onError(ErrorsHelper.ensureStatusRuntimeException(t));
                }
            }
        }

    // ----- containsEntry --------------------------------------------------

    @Override
    public void containsEntry(ContainsEntryRequest request, StreamObserver<BoolValue> observer)
        {
        createHolderAsync(request, request.getScope(), request.getCache(), request.getFormat())
                .thenComposeAsync(this::containsEntry, f_executor)
                .thenApplyAsync(h -> toBoolValue(h.getResult(), h.getCacheSerializer()), f_executor)
                .handleAsync((result, err) -> handleUnary(result, err, SafeStreamObserver.ensureSafeObserver(observer)), f_executor);
        }

    /**
     * Execute the {@link ContainsEntryRequest} request and return a {@link CompletionStage} that will complete when
     * the {@link AsyncNamedCache} request completes and will contain a {@link CacheRequestHolder}
     * holding the result of the contains entry request as a serialized Boolean.
     *
     * @param holder  the {@link CacheRequestHolder} containing the {@link ContainsEntryRequest} request
     *
     * @return a {@link CompletionStage} that completes with a {@link CacheRequestHolder} containing
     *         the serialized Boolean result of executing the {@link ContainsEntryRequest} request
     */
    protected CompletionStage<CacheRequestHolder<ContainsEntryRequest, Binary>>
    containsEntry(CacheRequestHolder<ContainsEntryRequest, Void> holder)
        {
        ContainsEntryRequest request = holder.getRequest();
        Binary               key     = holder.convertKeyDown(request.getKey());
        Binary               value   = holder.convertDown(request.getValue());

        EntryProcessor<Binary, Binary, Binary> processor = castProcessor(new BinaryContainsValueProcessor(value));
        return holder.runAsync(holder.getAsyncCache().invoke(key, processor));
        }

    // ----- containsEntry --------------------------------------------------

    @Override
    public void containsKey(ContainsKeyRequest request, StreamObserver<BoolValue> observer)
        {
        createHolderAsync(request, request.getScope(), request.getCache(), request.getFormat())
                .thenComposeAsync(this::containsKey, f_executor)
                .thenApplyAsync(h -> BoolValue.of(h.getDeserializedResult()), f_executor)
                .handleAsync((result, err) -> handleUnary(result, err, SafeStreamObserver.ensureSafeObserver(observer)), f_executor);
        }

    /**
     * Execute the {@link ContainsKeyRequest} request and return a {@link CompletionStage} that will complete when
     * the {@link AsyncNamedCache} request completes and will contain a {@link CacheRequestHolder}
     * holding the result of the contains key request.
     *
     * @param holder  the {@link CacheRequestHolder} containing the {@link ContainsKeyRequest} request
     *
     * @return a {@link CompletionStage} that completes with a {@link CacheRequestHolder} containing
     *         the Boolean result of executing the {@link ContainsKeyRequest} request
     */
    protected CompletionStage<CacheRequestHolder<ContainsKeyRequest, Boolean>>
    containsKey(CacheRequestHolder<ContainsKeyRequest, Void> holder)
        {
        ContainsKeyRequest request = holder.getRequest();
        Binary             key     = holder.convertKeyDown(request.getKey());

        return holder.runAsync(holder.getAsyncCache().containsKey(key));
        }

    // ----- containsValue --------------------------------------------------

    @Override
    public void containsValue(ContainsValueRequest request, StreamObserver<BoolValue> observer)
        {
        createHolderAsync(request, request.getScope(), request.getCache(), request.getFormat())
                .thenComposeAsync(this::containsValue, f_executor)
                .thenApplyAsync(h -> BoolValue.of(h.getResult() > 0), f_executor)
                .handleAsync((result, err) -> handleUnary(result, err, SafeStreamObserver.ensureSafeObserver(observer)), f_executor);
        }

    /**
     * Execute the {@link ContainsValueRequest} request and return a {@link CompletionStage} that will complete when
     * the {@link AsyncNamedCache} request completes and will contain a {@link CacheRequestHolder}
     * holding the result of the contains value request as a serialized Boolean.
     *
     * @param holder  the {@link CacheRequestHolder} containing the {@link ContainsValueRequest} request
     *
     * @return a {@link CompletionStage} that completes with a {@link CacheRequestHolder} containing
     *         the serialized Boolean result of executing the {@link ContainsValueRequest} request
     */
    protected CompletionStage<CacheRequestHolder<ContainsValueRequest, Integer>>
    containsValue(CacheRequestHolder<ContainsValueRequest, Void> holder)
        {
        ContainsValueRequest request = holder.getRequest();
        Object               value   = BinaryHelper.fromByteString(request.getValue(), holder.getSerializer());
        Filter<Binary>       filter  = Filters.equal(IdentityExtractor.INSTANCE(), value);

        return holder.runAsync(holder.getAsyncCache().aggregate(filter, Aggregators.count()));
        }

    // ----- entrySet -------------------------------------------------------

    @Override
    public void entrySet(EntrySetRequest request, StreamObserver<Entry> observer)
        {
        createHolderAsync(request, request.getScope(), request.getCache(), request.getFormat())
                .thenApplyAsync(h -> this.entrySet(h, observer), f_executor)
                .handleAsync((v, err) -> handleError(err, observer), f_executor);
        }

    /**
     * Execute the {@link EntrySetRequest} request and send the results to the {@link StreamObserver}.
     *
     * @param holder    the {@link CacheRequestHolder} containing the {@link EntrySetRequest} request
     * @param observer  the {@link StreamObserver} which will receive results
     *
     * @return always return {@link Void}
     */
    protected Void entrySet(CacheRequestHolder<EntrySetRequest, Void> holder, StreamObserver<Entry> observer)
        {
        try
            {
            EntrySetRequest request    = holder.getRequest();
            Serializer      serializer = holder.getSerializer();
            Filter<Binary>  filter     = ensureFilter(request.getFilter(), serializer);

            Comparator<Map.Entry<Binary, Binary>> comparator =
                    deserializeComparator(request.getComparator(), serializer);

            if (comparator == null)
                {
                holder.runAsync(holder.getAsyncCache().entrySet(filter, holder.entryConsumer(observer)))
                        .handleAsync((v, err) -> handleErrorOrComplete(err, observer), f_executor);
                }
            else
                {
                holder.runAsync(holder.getAsyncCache().entrySet(filter, comparator))
                        .handleAsync((h, err) -> handleSetOfEntries(h, err, observer), f_executor);
                }
            }
        catch (Throwable t)
            {
            observer.onError(ErrorsHelper.ensureStatusRuntimeException(t));
            }
        return VOID;
        }

    // ----- get ------------------------------------------------------------

    @Override
    public void get(GetRequest request, StreamObserver<OptionalValue> observer)
        {
        createHolderAsync(request, request.getScope(), request.getCache(), request.getFormat())
                .thenComposeAsync(this::get, f_executor)
                .thenApplyAsync(h -> h.toOptionalValue(h.getDeserializedResult()), f_executor)
                .handleAsync((result, err) -> handleUnary(result, err, SafeStreamObserver.ensureSafeObserver(observer)), f_executor);
        }

    /**
     * Execute the {@link GetRequest} request and return a {@link CompletionStage} that will complete when
     * the {@link AsyncNamedCache} request completes and will contain a {@link CacheRequestHolder}
     * holding the result of the {@link GetRequest} request as a serialized Boolean.
     *
     * @param holder  the {@link CacheRequestHolder} containing the {@link GetRequest} request
     *
     * @return a {@link CompletionStage} that completes with a {@link CacheRequestHolder} containing
     *         the serialized Binary result of executing the {@link GetRequest} request
     */
    protected CompletionStage<CacheRequestHolder<GetRequest, Binary>> get(CacheRequestHolder<GetRequest, Void> holder)
        {
        Binary key = holder.convertKeyDown(holder.getRequest().getKey());
        EntryProcessor<Binary, Binary, Binary> processor = BinaryProcessors.get();
        return holder.runAsync(holder.getAsyncCache().invoke(key, processor));
        }

    // ----- getAll ---------------------------------------------------------

    @Override
    public void getAll(GetAllRequest request, StreamObserver<Entry> observer)
        {
        if (request.getKeyList().isEmpty())
            {
            // no keys have been requested so just complete the observer
            observer.onCompleted();
            }
        else
            {
            createHolderAsync(request, request.getScope(), request.getCache(), request.getFormat())
                    .thenApplyAsync(h -> this.getAll(h, observer), f_executor)
                    .handleAsync((v, err) -> handleError(err, observer), f_executor);
            }
        }

    /**
     * Execute the {@link GetAllRequest} request and send the results to the {@link StreamObserver}.
     *
     * @param holder    the {@link CacheRequestHolder} containing the {@link GetAllRequest} request
     * @param observer  the {@link StreamObserver} which will receive results
     *
     * @return always return {@link Void}
     */
    protected Void getAll(CacheRequestHolder<GetAllRequest, Void> holder, StreamObserver<Entry> observer)
        {
        Consumer<? super Map.Entry<? extends Binary, ? extends Binary>> callback = holder.entryConsumer(observer);
        holder.runAsync(convertKeys(holder))
                .thenComposeAsync(h -> h.runAsync(h.getAsyncCache().invokeAll(h.getResult(), BinaryProcessors.get(), callback)), f_executor)
                .handleAsync((v, err) -> handleErrorOrComplete(err, observer), f_executor);
        return VOID;
        }

    // ----- invoke ---------------------------------------------------------

    @Override
    public void invoke(InvokeRequest request, StreamObserver<BytesValue> observer)
        {
        ByteString processorBytes = request.getProcessor();
        if (!processorBytes.isEmpty())
            {
            createHolderAsync(request, request.getScope(), request.getCache(), request.getFormat())
                    .thenComposeAsync(this::invoke, f_executor)
                    .thenApplyAsync(h -> BinaryHelper.toBytesValue(h.convertUp(h.getResult())), f_executor)
                    .handleAsync((result, err) ->
                        {
                        handleUnary(result, err, SafeStreamObserver.ensureSafeObserver(observer));
                        return null;
                        }, f_executor);
            }
        else
            {
            observer.onError(Status.INVALID_ARGUMENT
                    .withDescription(MISSING_PROCESSOR_MESSAGE)
                    .asRuntimeException());
            }
        }

    /**
     * Execute the {@link InvokeRequest} request and return a {@link CompletionStage} that will complete when
     * the {@link AsyncNamedCache} request completes and will contain a {@link CacheRequestHolder}
     * holding the result of the {@link InvokeRequest} request as a serialized Boolean.
     *
     * @param holder  the {@link CacheRequestHolder} containing the {@link InvokeRequest} request
     *
     * @return a {@link CompletionStage} that completes with a {@link CacheRequestHolder} containing
     *         the serialized Binary result of executing the {@link InvokeRequest} request
     */
    protected CompletionStage<CacheRequestHolder<InvokeRequest, Binary>>
            invoke(CacheRequestHolder<InvokeRequest, Void> holder)
        {
        InvokeRequest request = holder.getRequest();
        Binary key = holder.convertKeyDown(request.getKey());
        EntryProcessor<Binary, Binary, Binary> processor
                = BinaryHelper.fromByteString(request.getProcessor(), holder.getSerializer());

        return holder.runAsync(holder.getAsyncCache().invoke(key, processor));
        }

    // ----- invokeAll ------------------------------------------------------

    @Override
    public void invokeAll(InvokeAllRequest request, StreamObserver<Entry> observer)
        {
        ByteString processorBytes = request.getProcessor();
        if (processorBytes.isEmpty())
            {
            observer.onError(Status.INVALID_ARGUMENT
                                     .withDescription(MISSING_PROCESSOR_MESSAGE)
                                     .asRuntimeException());
            }
        else
            {
            CompletionStage<Void> future;
            try
                {
                if (request.getKeysCount() != 0)
                    {
                    // invokeAll with keys
                    future = invokeAllWithKeys(request, observer);
                    }
                else
                    {
                    // invokeAll with filter
                    future = invokeAllWithFilter(request, observer);
                    }

                future.handleAsync((v, err) -> handleError(err, observer), f_executor);
                }
            catch (Throwable t)
                {
                observer.onError(ErrorsHelper.ensureStatusRuntimeException(t));
                }
            }
        }

    /**
     * Execute the filtered {@link InvokeAllRequest} request passing the results to the provided
     * {@link StreamObserver}.
     *
     * @param request   the {@link InvokeAllRequest}
     * @param observer  the {@link StreamObserver} which will receive the results
     *
     * @return always returns a {@link CompletionStage} returning {@link Void}
     */
    protected CompletionStage<Void> invokeAllWithFilter(InvokeAllRequest request, StreamObserver<Entry> observer)
        {
        return createHolderAsync(request, request.getScope(), request.getCache(), request.getFormat())
                .thenComposeAsync(h -> invokeAllWithFilter(h, observer), f_executor);
        }

    /**
     * Execute the filtered {@link InvokeAllRequest} request passing the results to the provided
     * {@link StreamObserver}.
     *
     * @param holder    the {@link CacheRequestHolder} containing the {@link InvokeAllRequest}
     * @param observer  the {@link StreamObserver} which will receive the results
     *
     * @return always returns a {@link CompletionStage} returning {@link Void}
     */
    protected CompletionStage<Void> invokeAllWithFilter(CacheRequestHolder<InvokeAllRequest, Void> holder,
                                                      StreamObserver<Entry> observer)
        {
        InvokeAllRequest request     = holder.getRequest();
        ByteString       filterBytes = request.getFilter();

        // if no filter is present in the request use an AlwaysFilter
        Filter<Binary> filter = filterBytes.isEmpty()
                                ? Filters.always()
                                : BinaryHelper.fromByteString(filterBytes, holder.getSerializer());

        ByteString                             processorBytes = request.getProcessor();
        EntryProcessor<Binary, Binary, Binary> processor       = BinaryHelper.fromByteString(processorBytes,
                                                                                             holder.getSerializer());

        Consumer<Map.Entry<? extends Binary, ? extends Binary>> callback = holder.entryConsumer(observer);

        return holder.runAsync(holder.getAsyncCache().invokeAll(filter, processor, callback))
                .handleAsync((v, err) -> handleErrorOrComplete(err, observer), f_executor);
        }

    /**
     * Execute the key-based {@link InvokeAllRequest} request passing the results to the provided
     * {@link StreamObserver}.
     *
     * @param request   the {@link InvokeAllRequest}
     * @param observer  the {@link StreamObserver} which will receive the results
     *
     * @return always returns a {@link CompletionStage} returning {@link Void}
     */
    protected CompletionStage<Void> invokeAllWithKeys(InvokeAllRequest request, StreamObserver<Entry> observer)
        {
        return createHolderAsync(request, request.getScope(), request.getCache(), request.getFormat())
                .thenComposeAsync(h -> invokeAllWithKeys(h, observer), f_executor);
        }

    /**
     * Execute the key-based {@link InvokeAllRequest} request passing the results to the provided
     * {@link StreamObserver}.
     *
     * @param holder    the {@link CacheRequestHolder} containing the {@link InvokeAllRequest}
     * @param observer  the {@link StreamObserver} which will receive the results
     *
     * @return always returns a {@link CompletionStage} returning {@link Void}
     */
    protected CompletionStage<Void> invokeAllWithKeys(CacheRequestHolder<InvokeAllRequest, Void> holder,
                                                    StreamObserver<Entry> observer)
        {
        InvokeAllRequest request = holder.getRequest();
        List<Binary>     keys    = request.getKeysList()
                .stream()
                .map(holder::convertKeyDown)
                .collect(Collectors.toList());

        EntryProcessor<Binary, Binary, Binary> processor
                = BinaryHelper.fromByteString(request.getProcessor(), holder.getSerializer());

        Consumer<Map.Entry<? extends Binary, ? extends Binary>> callback = holder.entryConsumer(observer);

        return holder.runAsync(holder.getAsyncCache().invokeAll(keys, processor, callback))
                .handleAsync((v, err) -> handleErrorOrComplete(err, observer), f_executor);
        }

    // ----- isEmpty --------------------------------------------------------

    @Override
    public void isEmpty(IsEmptyRequest request, StreamObserver<BoolValue> observer)
        {
        getAsyncCache(request.getScope(), request.getCache())
                .thenCompose(AsyncNamedCache::isEmpty)
                .thenApplyAsync(BoolValue::of, f_executor)
                .handleAsync((result, err) -> handleUnary(result, err, SafeStreamObserver.ensureSafeObserver(observer)), f_executor);
        }

    // ----- isReady --------------------------------------------------------

    @Override
    public void isReady(IsReadyRequest request, StreamObserver<BoolValue> observer)
        {
        getAsyncCache(request.getScope(), request.getCache())
                .thenComposeAsync(c -> CompletableFuture.supplyAsync(() -> c.getNamedMap().isReady()), f_executor)
                .thenApplyAsync(BoolValue::of, f_executor)
                .handleAsync((result, err) -> handleUnary(result, err, SafeStreamObserver.ensureSafeObserver(observer)), f_executor);
        }

    // ----- keySet ---------------------------------------------------------

    @Override
    public void keySet(KeySetRequest request, StreamObserver<BytesValue> observer)
        {
        createHolderAsync(request, request.getScope(), request.getCache(), request.getFormat())
                .thenApplyAsync(h -> this.keySet(h, observer), f_executor)
                .handleAsync((v, err) -> handleError(err, observer), f_executor);
        }

    /**
     * Execute the key-based {@link KeySetRequest} request passing the results to the provided
     * {@link StreamObserver}.
     *
     * @param holder    the {@link CacheRequestHolder} containing the {@link KeySetRequest}
     * @param observer  the {@link StreamObserver} which will receive the results
     *
     * @return always returns {@link Void}
     */
    protected Void keySet(CacheRequestHolder<KeySetRequest, Void> holder, StreamObserver<BytesValue> observer)
        {
        try
            {
            KeySetRequest request = holder.getRequest();
            Serializer serializer = holder.getSerializer();
            Filter<Binary> filter = ensureFilter(request.getFilter(), serializer);

            Consumer<Binary> callback = holder.binaryConsumer(observer);

            holder.runAsync(holder.getAsyncCache().keySet(filter, callback))
                    .handleAsync((v, err) -> handleErrorOrComplete(err, observer), f_executor);
            }
        catch (Throwable t)
            {
            observer.onError(ErrorsHelper.ensureStatusRuntimeException(t));
            }
        return VOID;
        }

    // ----- Paged Queries keySet() entrySet() values() ---------------------

    @Override
    public void nextKeySetPage(PageRequest request, StreamObserver<BytesValue> observer)
        {
        createHolderAsync(request, request.getScope(), request.getCache(), request.getFormat())
                .thenApplyAsync(h -> PagedQueryHelper.keysPagedQuery(h, getTransferThreshold()), f_executor)
                .handleAsync((stream, err) -> handleStream(stream, err, observer), f_executor)
                .handleAsync((v, err) -> handleError(err, observer), f_executor);
        }

    @Override
    public void nextEntrySetPage(PageRequest request, StreamObserver<EntryResult> observer)
        {
        createHolderAsync(request, request.getScope(), request.getCache(), request.getFormat())
                .thenApplyAsync(h -> PagedQueryHelper.entryPagedQuery(h, getTransferThreshold()), f_executor)
                .handleAsync((stream, err) -> handleStream(stream, err, observer), f_executor)
                .handleAsync((v, err) -> handleError(err, observer), f_executor);
        }

    // ----- put ------------------------------------------------------------

    @Override
    public void put(PutRequest request, StreamObserver<BytesValue> observer)
        {
        createHolderAsync(request, request.getScope(), request.getCache(), request.getFormat())
                .thenComposeAsync(this::put, f_executor)
                .handleAsync((result, err) -> handleUnary(result, err, SafeStreamObserver.ensureSafeObserver(observer)), f_executor);
        }

    /**
     * Execute a put request.
     *
     * @param holder  the {@link CacheRequestHolder} containing the {@link PutRequest} request
     *
     * @return a {@link CompletionStage} that completes with a {@link BytesValue} containing
     *         the serialized result of executing the {@link PutRequest} request
     */
    protected CompletionStage<BytesValue> put(CacheRequestHolder<PutRequest, Void> holder)
        {
        PutRequest request = holder.getRequest();
        Binary     key     = holder.convertKeyDown(request.getKey());
        Binary     value   = holder.convertDown(request.getValue());

        return holder.getAsyncCache().invoke(key, BinaryProcessors.put(value, request.getTtl()))
                .thenApplyAsync(holder::deserializeToBytesValue, f_executor);
        }

    // ----- putAll ---------------------------------------------------------

    @Override
    public void putAll(PutAllRequest request, StreamObserver<Empty> observer)
        {
        createHolderAsync(request, request.getScope(), request.getCache(), request.getFormat())
                .thenComposeAsync(this::putAll, f_executor)
                .handleAsync((result, err) -> handleUnary(result, err, SafeStreamObserver.ensureSafeObserver(observer)), f_executor);
        }

    /**
     * Execute a putAll request.
     *
     * @param holder  the {@link CacheRequestHolder} containing the {@link PutAllRequest} request
     *
     * @return a {@link CompletionStage} that completes after executing the {@link PutAllRequest} request
     */
    protected CompletionStage<Empty> putAll(CacheRequestHolder<PutAllRequest, Void> holder)
        {
        PutAllRequest request = holder.getRequest();
        if (request.getEntryCount() == 0)
            {
            return CompletableFuture.completedFuture(BinaryHelper.EMPTY);
            }

        Map<Binary, Binary> map = new HashMap<>();
        for (Entry entry : request.getEntryList())
            {
            Binary key   = holder.convertKeyDown(entry.getKey());
            Binary value = holder.convertDown(entry.getValue());
            map.put(key, value);
            }

        if (holder.getCache().getCacheService() instanceof PartitionedService)
            {
            return partitionedPutAll(holder, map);
            }
        else
            {
            return plainPutAll(holder.getAsyncCache(), map, request.getTtl());
            }
        }

    // ----- putIfAbsent ----------------------------------------------------

    @Override
    public void putIfAbsent(PutIfAbsentRequest request, StreamObserver<BytesValue> observer)
        {
        createHolderAsync(request, request.getScope(), request.getCache(), request.getFormat())
                .thenComposeAsync(this::putIfAbsent, f_executor)
                .handleAsync((result, err) -> handleUnary(result, err, SafeStreamObserver.ensureSafeObserver(observer)), f_executor);
        }

    /**
     * Execute a {@link PutIfAbsentRequest} request.
     *
     * @param holder  the {@link CacheRequestHolder} containing the {@link PutIfAbsentRequest} request
     *
     * @return a {@link CompletionStage} that completes with a {@link BytesValue} containing
     * the serialized result of executing the {@link PutIfAbsentRequest} request
     */
    protected CompletableFuture<BytesValue> putIfAbsent(CacheRequestHolder<PutIfAbsentRequest, Void> holder)
        {
        PutIfAbsentRequest request = holder.getRequest();
        Binary             key     = holder.convertKeyDown(request.getKey());
        Binary             value   = holder.convertDown(request::getValue);

        return holder.getAsyncCache().invoke(key, BinaryProcessors.putIfAbsent(value, request.getTtl()))
                .thenApplyAsync(holder::deserializeToBytesValue, f_executor);
        }

    // ----- remove ---------------------------------------------------------

    @Override
    public void remove(RemoveRequest request, StreamObserver<BytesValue> observer)
        {
        createHolderAsync(request, request.getScope(), request.getCache(), request.getFormat())
                .thenComposeAsync(h -> h.runAsync(remove(h)), f_executor)
                .thenApplyAsync(h -> h.toBytesValue(h.getResult()), f_executor)
                .handleAsync((result, err) -> handleUnary(result, err, SafeStreamObserver.ensureSafeObserver(observer)), f_executor);
        }

    /**
     * Execute a {@link RemoveRequest} request.
     *
     * @param holder  the {@link CacheRequestHolder} containing the {@link RemoveRequest} request
     *
     * @return a {@link CompletionStage} that completes with a {@link Binary} containing
     * the serialized result of executing the {@link RemoveRequest} request
     */
    protected CompletableFuture<Binary> remove(CacheRequestHolder<RemoveRequest, Void> holder)
        {
        RemoveRequest request = holder.getRequest();
        Binary        key     = holder.convertKeyDown(request.getKey());

        return holder.getAsyncCache().invoke(key, BinaryRemoveProcessor.INSTANCE)
                .thenApplyAsync(holder::fromBinary, f_executor);
        }

    // ----- removeIndex ----------------------------------------------------

    @Override
    public void removeIndex(RemoveIndexRequest request, StreamObserver<Empty> observer)
        {
        createHolderAsync(request, request.getScope(), request.getCache(), request.getFormat())
                .thenApplyAsync(this::removeIndex, f_executor)
                .handleAsync((result, err) -> handleUnary(result, err, SafeStreamObserver.ensureSafeObserver(observer)), f_executor);
        }

    // ----- remove mapping -------------------------------------------------

    @Override
    public void removeMapping(RemoveMappingRequest request, StreamObserver<BoolValue> observer)
        {
        createHolderAsync(request, request.getScope(), request.getCache(), request.getFormat())
                .thenComposeAsync(this::removeMapping, f_executor)
                .thenApplyAsync(h -> BoolValue.of(h.getDeserializedResult()), f_executor)
                .handleAsync((result, err) -> handleUnary(result, err, SafeStreamObserver.ensureSafeObserver(observer)), f_executor);
        }

    /**
     * Execute the {@link RemoveMappingRequest} request and return a {@link CompletionStage} that will complete when
     * the {@link AsyncNamedCache} request completes and will contain a {@link CacheRequestHolder}
     * holding the result of the {@link RemoveMappingRequest} request as a serialized Boolean.
     *
     * @param holder  the {@link CacheRequestHolder} containing the {@link RemoveMappingRequest} request
     *
     * @return a {@link CompletionStage} that completes with a {@link CacheRequestHolder} containing
     *         the serialized Binary result of executing the {@link RemoveMappingRequest} request
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    protected CompletionStage<CacheRequestHolder<RemoveMappingRequest, Boolean>>
    removeMapping(CacheRequestHolder<RemoveMappingRequest, Void> holder)
        {
        RemoveMappingRequest request = holder.getRequest();
        Binary               key     = holder.convertKeyDown(request.getKey());
        Object               value   = BinaryHelper.fromByteString(request.getValue(), holder.getSerializer());
        AsyncNamedCache      cache   = holder.getAsyncCache();

        return holder.runAsync(cache.remove(key, value));
        }

    // ----- replace --------------------------------------------------------

    @Override
    public void replace(ReplaceRequest request, StreamObserver<BytesValue> observer)
        {
        createHolderAsync(request, request.getScope(), request.getCache(), request.getFormat())
                .thenComposeAsync(h -> h.runAsync(replace(h)), f_executor)
                .thenApplyAsync(h -> h.toBytesValue(h.getResult()), f_executor)
                .handleAsync((result, err) -> handleUnary(result, err, SafeStreamObserver.ensureSafeObserver(observer)), f_executor);
        }

    /**
     * Execute a {@link ReplaceRequest} request.
     *
     * @param holder  the {@link CacheRequestHolder} containing the {@link ReplaceRequest} request
     *
     * @return a {@link CompletionStage} that completes with a {@link Binary} containing
     *         the serialized result of executing the {@link ReplaceRequest} request
     */
    protected CompletableFuture<Binary> replace(CacheRequestHolder<ReplaceRequest, Void> holder)
        {
        ReplaceRequest request = holder.getRequest();
        Binary         key     = holder.convertKeyDown(request.getKey());
        Binary         value   = holder.convertDown(request.getValue());

        return holder.getAsyncCache().invoke(key, castProcessor(new BinaryReplaceProcessor(value)))
                .thenApplyAsync(holder::fromBinary, f_executor);
        }

    // ----- replace mapping ------------------------------------------------

    @Override
    public void replaceMapping(ReplaceMappingRequest request, StreamObserver<BoolValue> observer)
        {
        createHolderAsync(request, request.getScope(), request.getCache(), request.getFormat())
                .thenComposeAsync(h -> h.runAsync(replaceMapping(h)), f_executor)
                .thenApplyAsync(h -> toBoolValue(h.getResult(), h.getCacheSerializer()), f_executor)
                .handleAsync((result, err) -> handleUnary(result, err, SafeStreamObserver.ensureSafeObserver(observer)), f_executor);
        }

    /**
     * Execute a {@link ReplaceMappingRequest} request.
     *
     * @param holder  the {@link CacheRequestHolder} containing the {@link ReplaceMappingRequest} request
     *
     * @return a {@link CompletionStage} that completes with a {@link Binary} containing
     *         the serialized result of executing the {@link ReplaceMappingRequest} request
     */
    protected CompletableFuture<Binary> replaceMapping(CacheRequestHolder<ReplaceMappingRequest, Void> holder)
        {
        ReplaceMappingRequest request   = holder.getRequest();
        Binary                key       = holder.convertKeyDown(request.getKey());
        Binary                prevValue = holder.convertDown(request.getPreviousValue());
        Binary                newValue  = holder.convertDown(request.getNewValue());

        return holder.getAsyncCache().invoke(key, castProcessor(new BinaryReplaceMappingProcessor(prevValue, newValue)));
        }

    // ----- size -----------------------------------------------------------

    @Override
    public void size(SizeRequest request, StreamObserver<Int32Value> observer)
        {
        getAsyncCache(request.getScope(), request.getCache())
                .thenComposeAsync(AsyncNamedCache::size, f_executor)
                .thenApplyAsync(Int32Value::of, f_executor)
                .handleAsync((result, err) -> handleUnary(result, err, SafeStreamObserver.ensureSafeObserver(observer)), f_executor);
        }

    // ----- truncate -------------------------------------------------------

    @Override
    public void truncate(TruncateRequest request, StreamObserver<Empty> observer)
        {
        getAsyncCache(request.getScope(), request.getCache())
                .thenApplyAsync(cache -> this.execute(() -> cache.getNamedCache().truncate()), f_executor)
                .handleAsync((result, err) -> handleUnary(result, err, SafeStreamObserver.ensureSafeObserver(observer)), f_executor);
        }

    // ----- values ---------------------------------------------------------

    /**
     * Execute the {@link ValuesRequest} request passing the results to the provided
     * {@link StreamObserver}.
     *
     * @param request   the {@link ValuesRequest}
     * @param observer  the {@link StreamObserver} which will receive the results
     */
    @Override
    public void values(ValuesRequest request, StreamObserver<BytesValue> observer)
        {
        createHolderAsync(request, request.getScope(), request.getCache(), request.getFormat())
                .thenApplyAsync(h -> this.values(h, observer), f_executor)
                .handleAsync((v, err) -> handleError(err, observer), f_executor);
        }

    /**
     * Execute the {@link ValuesRequest} request passing the results to the provided
     * {@link StreamObserver}.
     *
     * @param holder    the {@link CacheRequestHolder} containing the {@link ValuesRequest}
     * @param observer  the {@link StreamObserver} which will receive the results
     *
     * @return always returns {@link Void}
     */
    protected Void values(CacheRequestHolder<ValuesRequest, Void> holder, StreamObserver<BytesValue> observer)
        {
        try
            {
            ValuesRequest      request    = holder.getRequest();
            Serializer         serializer = holder.getSerializer();
            Filter<Binary>     filter     = ensureFilter(request.getFilter(), serializer);
            Comparator<Binary> comparator = deserializeComparator(request.getComparator(), serializer);

            AsyncNamedCache<Binary, Binary> cache = holder.getAsyncCache();
            if (comparator == null)
                {
                Consumer<Binary> callback = holder.binaryConsumer(observer);
                cache.values(filter, callback)
                        .handleAsync((v, err) -> handleErrorOrComplete(err, observer), f_executor);
                }
            else
                {
                holder.runAsync(holder.getAsyncCache().values(filter, comparator))
                        .handleAsync((h, err) -> handleStream(h, err, observer), f_executor)
                        .handleAsync((v, err) -> handleError(err, observer), f_executor);
                }
            }
        catch (Throwable t)
            {
            observer.onError(ErrorsHelper.ensureStatusRuntimeException(t));
            }
        return VOID;
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Convert the keys for a {@link GetAllRequest} from the request's serialization format
     * to the cache's serialization format.
     *
     * @param holder  the {@link CacheRequestHolder} containing the {@link GetAllRequest}
     *                containing the keys to convert
     *
     * @return A {@link CompletionStage} that completes with the converted keys
     */
    protected CompletionStage<List<Binary>> convertKeys(CacheRequestHolder<GetAllRequest, Void> holder)
        {
        return CompletableFuture.supplyAsync(() -> convertKeysToBinary(holder), f_executor);
        }
    }
