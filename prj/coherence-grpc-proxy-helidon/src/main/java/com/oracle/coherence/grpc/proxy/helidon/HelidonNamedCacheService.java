/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.proxy.helidon;

import com.google.protobuf.BoolValue;
import com.google.protobuf.ByteString;
import com.google.protobuf.BytesValue;
import com.google.protobuf.Empty;
import com.google.protobuf.Int32Value;
import com.oracle.coherence.grpc.AddIndexRequest;
import com.oracle.coherence.grpc.AggregateRequest;
import com.oracle.coherence.grpc.BinaryHelper;
import com.oracle.coherence.grpc.CacheRequestHolder;
import com.oracle.coherence.grpc.ContainsEntryRequest;
import com.oracle.coherence.grpc.ContainsKeyRequest;
import com.oracle.coherence.grpc.ContainsValueRequest;
import com.oracle.coherence.grpc.Entry;
import com.oracle.coherence.grpc.EntryResult;
import com.oracle.coherence.grpc.EntrySetRequest;
import com.oracle.coherence.grpc.ErrorsHelper;
import com.oracle.coherence.grpc.GetAllRequest;
import com.oracle.coherence.grpc.GetRequest;
import com.oracle.coherence.grpc.InvokeAllRequest;
import com.oracle.coherence.grpc.InvokeRequest;
import com.oracle.coherence.grpc.IsEmptyRequest;
import com.oracle.coherence.grpc.IsReadyRequest;
import com.oracle.coherence.grpc.KeySetRequest;
import com.oracle.coherence.grpc.OptionalValue;
import com.oracle.coherence.grpc.PageRequest;
import com.oracle.coherence.grpc.PutAllRequest;
import com.oracle.coherence.grpc.PutIfAbsentRequest;
import com.oracle.coherence.grpc.PutRequest;
import com.oracle.coherence.grpc.RemoveIndexRequest;
import com.oracle.coherence.grpc.RemoveMappingRequest;
import com.oracle.coherence.grpc.RemoveRequest;
import com.oracle.coherence.grpc.ReplaceMappingRequest;
import com.oracle.coherence.grpc.ReplaceRequest;
import com.oracle.coherence.grpc.SafeStreamObserver;
import com.oracle.coherence.grpc.SizeRequest;
import com.oracle.coherence.grpc.TruncateRequest;
import com.oracle.coherence.grpc.ValuesRequest;
import com.oracle.coherence.grpc.proxy.common.BaseNamedCacheServiceImpl;
import com.oracle.coherence.grpc.proxy.common.NamedCacheService;
import com.oracle.coherence.grpc.proxy.common.PagedQueryHelper;
import com.tangosol.internal.util.processor.BinaryProcessors;
import com.tangosol.internal.util.processor.CacheProcessors;
import com.tangosol.io.Serializer;
import com.tangosol.net.AsyncNamedCache;
import com.tangosol.net.NamedCache;
import com.tangosol.net.PartitionedService;
import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.Filter;
import com.tangosol.util.Filters;
import com.tangosol.util.InvocableMap.EntryProcessor;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.oracle.coherence.grpc.proxy.common.ResponseHandlers.handleErrorOrComplete;
import static com.oracle.coherence.grpc.proxy.common.ResponseHandlers.handleStream;
import static com.oracle.coherence.grpc.proxy.common.ResponseHandlers.handleStreamOfEntries;
import static com.oracle.coherence.grpc.proxy.common.ResponseHandlers.handleUnary;
import static com.tangosol.internal.util.processor.BinaryProcessors.BinaryContainsValueProcessor;
import static com.tangosol.internal.util.processor.BinaryProcessors.BinaryReplaceMappingProcessor;
import static com.tangosol.internal.util.processor.BinaryProcessors.BinaryReplaceProcessor;

/**
 * A gRPC {@link NamedCacheService}.
 *
 * @author Jonathan Knight  2024.02.08
 */
public class HelidonNamedCacheService
        extends BaseNamedCacheServiceImpl
        implements NamedCacheService
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a {@link HelidonNamedCacheService}.
     *
     * @param dependencies the {@link NamedCacheService.Dependencies} to use to configure the service
     */
    public HelidonNamedCacheService(NamedCacheService.Dependencies dependencies)
        {
        super(dependencies);
        }

    // ----- factory methods ------------------------------------------------

    /**
     * Create an instance of {@link HelidonNamedCacheService}
     * using the default dependencies configuration.
     *
     * @param deps  the {@link NamedCacheService.Dependencies} to use to create the service
     *
     * @return  an instance of {@link HelidonNamedCacheService}
     */
    public static HelidonNamedCacheService newInstance(NamedCacheService.Dependencies deps)
        {
        return new HelidonNamedCacheService(deps);
        }

    // ----- NamedCacheClient implementation --------------------------------

    // ----- addIndex -------------------------------------------------------

    @Override
    public void addIndex(AddIndexRequest request, StreamObserver<Empty> observer)
        {
        Throwable err = null;
        try
            {
            CacheRequestHolder<AddIndexRequest, Void> holder
                    = createRequestHolder(request, request.getScope(), request.getCache(), request.getFormat());
            addIndex(holder);
            }
        catch (Throwable e)
            {
            err = e;
            }
        handleUnary(Empty.getDefaultInstance(), err, SafeStreamObserver.ensureSafeObserver(observer));
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
                    .withDescription(MISSING_AGGREGATOR_MESSAGE)
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
                        .handleAsync((result, err) -> handleUnary(result, err, safeObserver), f_executor)
                        .toCompletableFuture().join();
                    }
                else
                    {
                    // aggregate with filter
                    aggregateWithFilter(request, f_executor)
                        .handle((result, err) -> handleUnary(result, err, safeObserver))
                        .toCompletableFuture().join();
                    }
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
        BoolValue result = null;
        Throwable error  = null;
        try
            {
            CacheRequestHolder<ContainsEntryRequest, Void> holder
                    = createRequestHolder(request, request.getScope(), request.getCache(), request.getFormat());

            Binary                                 key       = holder.convertKeyDown(request.getKey());
            Binary                                 value     = holder.convertDown(request.getValue());
            EntryProcessor<Binary, Binary, Binary> processor = castProcessor(new BinaryContainsValueProcessor(value));
            Binary                                 binary    = holder.getCache().invoke(key, processor);
            result = toBoolValue(binary, holder.getCacheSerializer());
            }
        catch (Throwable e)
            {
            error = e;
            }
        handleUnary(result, error, SafeStreamObserver.ensureSafeObserver(observer));
        }

    // ----- containsEntry --------------------------------------------------

    @Override
    public void containsKey(ContainsKeyRequest request, StreamObserver<BoolValue> observer)
        {
        BoolValue result = null;
        Throwable error  = null;
        try
            {
            CacheRequestHolder<ContainsKeyRequest, Void> holder
                    = createRequestHolder(request, request.getScope(), request.getCache(), request.getFormat());

            Binary  key     = holder.convertKeyDown(request.getKey());
            boolean fResult = holder.getCache().containsKey(key);
            result = BoolValue.of(fResult);
            }
        catch (Throwable e)
            {
            error = e;
            }
        handleUnary(result, error, SafeStreamObserver.ensureSafeObserver(observer));
        }

    // ----- containsValue --------------------------------------------------

    @Override
    public void containsValue(ContainsValueRequest request, StreamObserver<BoolValue> observer)
        {
        BoolValue result = null;
        Throwable error  = null;
        try
            {
            CacheRequestHolder<ContainsValueRequest, Void> holder
                    = createRequestHolder(request, request.getScope(), request.getCache(), request.getFormat());

            Binary  value    = holder.convertDown(request.getValue());
            boolean fPresent = holder.getCache().containsValue(value);
            result = BoolValue.of(fPresent);
            }
        catch (Throwable e)
            {
            error = e;
            }
        handleUnary(result, error, SafeStreamObserver.ensureSafeObserver(observer));
        }

    // ----- entrySet -------------------------------------------------------

    @Override
    public void entrySet(EntrySetRequest request, StreamObserver<Entry> observer)
        {
        StreamObserver<Entry> safeObserver = SafeStreamObserver.ensureSafeObserver(observer);
        try
            {
            CacheRequestHolder<EntrySetRequest, Void> holder =
                    createRequestHolder(request, request.getScope(), request.getCache(), request.getFormat());

            Serializer     serializer = holder.getSerializer();
            Filter<Binary> filter     = ensureFilter(request.getFilter(), serializer);

            Comparator<Map.Entry<Binary, Binary>> comparator =
                    deserializeComparator(request.getComparator(), serializer);

            if (comparator == null)
                {
                holder.getAsyncCache().entrySet(filter, holder.entryConsumer(safeObserver)).join();
                safeObserver.onCompleted();
                }
            else
                {
                Set<Map.Entry<Binary, Binary>> entries = holder.getCache().entrySet(filter, comparator);
                handleStreamOfEntries(holder, entries.stream(), safeObserver);
                }
            }
        catch (Throwable t)
            {
            safeObserver.onError(ErrorsHelper.ensureStatusRuntimeException(t));
            }
        }

    // ----- get ------------------------------------------------------------

    @Override
    public void get(GetRequest request, StreamObserver<OptionalValue> observer)
        {
        OptionalValue result = null;
        Throwable     error  = null;
        try
            {
            CacheRequestHolder<GetRequest, Void> holder
                    = createRequestHolder(request, request.getScope(), request.getCache(), request.getFormat());

            Binary key    = holder.convertKeyDown(holder.getRequest().getKey());
            Binary binary = holder.getCache().get(key);
            result = holder.toOptionalValue(binary);
            }
        catch (Throwable e)
            {
            error = e;
            }
        handleUnary(result, error, SafeStreamObserver.ensureSafeObserver(observer));
        }

    // ----- getAll ---------------------------------------------------------

    @Override
    public void getAll(GetAllRequest request, StreamObserver<Entry> observer)
        {
        StreamObserver<Entry> safeObserver = SafeStreamObserver.ensureSafeObserver(observer);
        try
            {
            List<ByteString> listKey = request.getKeyList();
            if (!listKey.isEmpty())
                {
                CacheRequestHolder<GetAllRequest, Void> holder =
                        createRequestHolder(request, request.getScope(), request.getCache(), request.getFormat());

                Consumer<? super Map.Entry<? extends Binary, ? extends Binary>> callback = holder.entryConsumer(observer);
                List<Binary> listKeys = listKey.stream().map(holder::convertKeyDown).toList();
                holder.getAsyncCache().invokeAll(listKeys, BinaryProcessors.get(), callback).join();
                }
            safeObserver.onCompleted();
            }
        catch (Throwable e)
            {
            safeObserver.onError(e);
            }
        }

    // ----- invoke ---------------------------------------------------------

    @Override
    public void invoke(InvokeRequest request, StreamObserver<BytesValue> observer)
        {
        BytesValue result = null;
        Throwable  error  = null;
        try
            {
            ByteString processorBytes = request.getProcessor();
            if (!processorBytes.isEmpty())
                {
                CacheRequestHolder<InvokeRequest, Void> holder =
                        createRequestHolder(request, request.getScope(), request.getCache(), request.getFormat());

                Binary key = holder.convertKeyDown(request.getKey());
                EntryProcessor<Binary, Binary, Binary> processor
                        = BinaryHelper.fromByteString(request.getProcessor(), holder.getSerializer());

                Binary binary = holder.getCache().invoke(key, processor);
                result = holder.toBytesValue(binary);
                }
            else
                {
                error = Status.INVALID_ARGUMENT
                            .withDescription(MISSING_PROCESSOR_MESSAGE)
                            .asRuntimeException();
                }
            }
        catch (Throwable e)
            {
            error = e;
            }
        handleUnary(result, error, SafeStreamObserver.ensureSafeObserver(observer));
        }

    // ----- invokeAll ------------------------------------------------------

    @Override
    public void invokeAll(InvokeAllRequest request, StreamObserver<Entry> observer)
        {
        StreamObserver<Entry> safeObserver = SafeStreamObserver.ensureSafeObserver(observer);
        try
            {
            ByteString processorBytes = request.getProcessor();
            if (processorBytes.isEmpty())
                {
                Throwable error = Status.INVALID_ARGUMENT
                        .withDescription(MISSING_PROCESSOR_MESSAGE)
                        .asRuntimeException();
                safeObserver.onError(error);
                }
            else
                {
                CacheRequestHolder<InvokeAllRequest, Void> holder =
                        createRequestHolder(request, request.getScope(), request.getCache(), request.getFormat());

                if (request.getKeysCount() != 0)
                    {
                    // invokeAll with keys
                    invokeAllWithKeys(holder, safeObserver);
                    }
                else
                    {
                    // invokeAll with filter
                    invokeAllWithFilter(holder, safeObserver);
                    }
                }
            }
        catch (Throwable e)
            {
            safeObserver.onError(e);
            }
        }

    /**
     * Execute the key-based {@link InvokeAllRequest} request passing the results to the provided
     * {@link StreamObserver}.
     *
     * @param holder    the {@link CacheRequestHolder} containing the {@link InvokeAllRequest}
     * @param observer  the {@link StreamObserver} which will receive the results
     */
    protected void invokeAllWithKeys(CacheRequestHolder<InvokeAllRequest, Void> holder,
                                     StreamObserver<Entry> observer)
        {
        Throwable error = null;
        try
            {
            InvokeAllRequest request = holder.getRequest();
            List<Binary>     keys    = request.getKeysList()
                    .stream()
                    .map(holder::convertKeyDown)
                    .collect(Collectors.toList());

            EntryProcessor<Binary, Binary, Binary> processor
                    = BinaryHelper.fromByteString(request.getProcessor(), holder.getSerializer());

            Consumer<Map.Entry<? extends Binary, ? extends Binary>> callback = holder.entryConsumer(observer);

            holder.getAsyncCache().invokeAll(keys, processor, callback).join();
            }
        catch (Throwable e)
            {
            error = e;
            }
        handleErrorOrComplete(error, observer);
        }

    /**
     * Execute the filtered {@link InvokeAllRequest} request passing the results to the provided
     * {@link StreamObserver}.
     *
     * @param holder    the {@link CacheRequestHolder} containing the {@link InvokeAllRequest}
     * @param observer  the {@link StreamObserver} which will receive the results
     */
    protected void invokeAllWithFilter(CacheRequestHolder<InvokeAllRequest, Void> holder,
                                       StreamObserver<Entry> observer)
        {
        Throwable error = null;
        try
            {
            InvokeAllRequest request     = holder.getRequest();
            ByteString       filterBytes = request.getFilter();

            // if no filter is present in the request use an AlwaysFilter
            Filter<Binary> filter = filterBytes.isEmpty()
                    ? Filters.always()
                    : BinaryHelper.fromByteString(filterBytes, holder.getSerializer());

            ByteString processorBytes = request.getProcessor();
            EntryProcessor<Binary, Binary, Binary> processor = BinaryHelper.fromByteString(processorBytes,
                    holder.getSerializer());

            Consumer<Map.Entry<? extends Binary, ? extends Binary>> callback = holder.entryConsumer(observer);
            holder.getAsyncCache().invokeAll(filter, processor, callback).join();
            }
        catch (Throwable e)
            {
            error = e;
            }
        handleErrorOrComplete(error, observer);
        }

    // ----- isEmpty --------------------------------------------------------

    @Override
    public void isEmpty(IsEmptyRequest request, StreamObserver<BoolValue> observer)
        {
        BoolValue result = null;
        Throwable error  = null;
        try
            {
            NamedCache<Binary, Binary> cache = getCache(request.getScope(), request.getCache(), true);
            result = BoolValue.of(cache.isEmpty());
            }
        catch (Throwable e)
            {
            error = e;
            }
        handleUnary(result, error, SafeStreamObserver.ensureSafeObserver(observer));
        }

    // ----- isReady --------------------------------------------------------

    @Override
    public void isReady(IsReadyRequest request, StreamObserver<BoolValue> observer)
        {
        BoolValue result = null;
        Throwable error  = null;
        try
            {
            NamedCache<Binary, Binary> cache = getCache(request.getScope(), request.getCache(), true);
            result = BoolValue.of(cache.isReady());
            }
        catch (Throwable e)
            {
            error = e;
            }
        handleUnary(result, error, SafeStreamObserver.ensureSafeObserver(observer));
        }

    // ----- keySet ---------------------------------------------------------

    @Override
    public void keySet(KeySetRequest request, StreamObserver<BytesValue> observer)
        {
        StreamObserver<BytesValue> safeObserver = SafeStreamObserver.ensureSafeObserver(observer);
        Throwable                  error        = null;
        try
            {
            CacheRequestHolder<KeySetRequest, Void> holder = 
                    createRequestHolder(request, request.getScope(), request.getCache(), request.getFormat());
        
            Serializer       serializer = holder.getSerializer();
            Filter<Binary>   filter     = ensureFilter(request.getFilter(), serializer);
            Consumer<Binary> callback   = holder.binaryConsumer(safeObserver);

            holder.getAsyncCache().keySet(filter, callback).join();
            }
        catch (Throwable t)
            {
            error = t;
            }
        handleErrorOrComplete(error, safeObserver);
        }

    // ----- Paged Queries keySet() entrySet() values() ---------------------

    @Override
    public void nextKeySetPage(PageRequest request, StreamObserver<BytesValue> observer)
        {
        Stream<BytesValue> stream = null;
        Throwable          error  = null;
        try
            {
            CacheRequestHolder<PageRequest, Void> holder = 
                    createRequestHolder(request, request.getScope(), request.getCache(), request.getFormat());

            stream = PagedQueryHelper.keysPagedQuery(holder, getTransferThreshold());
            }
        catch (Throwable e)
            {
            error = e;
            }
        handleStream(stream, error, SafeStreamObserver.ensureSafeObserver(observer));
        }

    @Override
    public void nextEntrySetPage(PageRequest request, StreamObserver<EntryResult> observer)
        {
        Stream<EntryResult> stream = null;
        Throwable           error  = null;
        try
            {
            CacheRequestHolder<PageRequest, Void> holder = 
                    createRequestHolder(request, request.getScope(), request.getCache(), request.getFormat());

            stream = PagedQueryHelper.entryPagedQuery(holder, getTransferThreshold());
            }
        catch (Throwable e)
            {
            error = e;
            }
        handleStream(stream, error, SafeStreamObserver.ensureSafeObserver(observer));
        }

    // ----- put ------------------------------------------------------------

    @Override
    public void put(PutRequest request, StreamObserver<BytesValue> observer)
        {
        BytesValue result = null;
        Throwable  error  = null;
        try
            {
            CacheRequestHolder<PutRequest, Void> holder = 
                    createRequestHolder(request, request.getScope(), request.getCache(), request.getFormat());

            Binary key    = holder.convertKeyDown(request.getKey());
            Binary value  = holder.convertDown(request.getValue());
            Binary binary = holder.getCache().put(key, value,request.getTtl());
            result = holder.toBytesValue(binary);
            }
        catch (Throwable e)
            {
            error = e;
            }
        handleUnary(result, error, SafeStreamObserver.ensureSafeObserver(observer));
        }

    // ----- putAll ---------------------------------------------------------

    @Override
    public void putAll(PutAllRequest request, StreamObserver<Empty> observer)
        {
        Throwable error  = null;
        try
            {
            CacheRequestHolder<PutAllRequest, Void> holder = 
                    createRequestHolder(request, request.getScope(), request.getCache(), request.getFormat());

            if (request.getEntryCount() > 0)
                {
                Map<Binary, Binary> map = new HashMap<>();
                for (Entry entry : request.getEntryList())
                    {
                    Binary key   = holder.convertKeyDown(entry.getKey());
                    Binary value = holder.convertDown(entry.getValue());
                    map.put(key, value);
                    }

                NamedCache<Binary, Binary> cache = holder.getCache();
                if (cache.getCacheService() instanceof PartitionedService)
                    {
                    partitionedPutAll(holder, map)
                            .toCompletableFuture().join();
                    }
                else
                    {
                    cache.invokeAll(map.keySet(), BinaryProcessors.putAll(map, request.getTtl()));
                    }
                }
            }
        catch (Throwable e)
            {
            error = e;
            }
        handleUnary(Empty.getDefaultInstance(), error, SafeStreamObserver.ensureSafeObserver(observer));
        }

    // ----- putIfAbsent ----------------------------------------------------

    @Override
    public void putIfAbsent(PutIfAbsentRequest request, StreamObserver<BytesValue> observer)
        {
        BytesValue result = null;
        Throwable  error  = null;
        try
            {
            CacheRequestHolder<PutIfAbsentRequest, Void> holder =
                    createRequestHolder(request, request.getScope(), request.getCache(), request.getFormat());

            Binary key   = holder.convertKeyDown(request.getKey());
            Binary value = holder.convertDown(request::getValue);
            Binary binary = holder.getCache().invoke(key, BinaryProcessors.putIfAbsent(value, request.getTtl()));
            binary = holder.fromBinary(binary);
            result = holder.toBytesValue(binary);
            }
        catch (Throwable e)
            {
            error = e;
            }
        handleUnary(result, error, SafeStreamObserver.ensureSafeObserver(observer));
        }

    // ----- remove ---------------------------------------------------------

    @Override
    public void remove(RemoveRequest request, StreamObserver<BytesValue> observer)
        {
        BytesValue result = null;
        Throwable  error  = null;
        try
            {
            CacheRequestHolder<RemoveRequest, Void> holder =
                    createRequestHolder(request, request.getScope(), request.getCache(), request.getFormat());

            Binary key    = holder.convertKeyDown(request.getKey());
            Binary binary = holder.getCache().remove(key);
            result = holder.toBytesValue(binary);
            }
        catch (Throwable e)
            {
            error = e;
            }
        handleUnary(result, error, SafeStreamObserver.ensureSafeObserver(observer));
        }

    // ----- removeIndex ----------------------------------------------------

    @Override
    public void removeIndex(RemoveIndexRequest request, StreamObserver<Empty> observer)
        {
        Empty     result = null;
        Throwable error  = null;
        try
            {
            CacheRequestHolder<RemoveIndexRequest, Void> holder =
                    createRequestHolder(request, request.getScope(), request.getCache(), request.getFormat());
            result = removeIndex(holder);
            }
        catch (Throwable e)
            {
            error = e;
            }
        handleUnary(result, error, SafeStreamObserver.ensureSafeObserver(observer));
        }

    // ----- remove mapping -------------------------------------------------

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void removeMapping(RemoveMappingRequest request, StreamObserver<BoolValue> observer)
        {
        BoolValue result = null;
        Throwable error  = null;
        try
            {
            CacheRequestHolder<RemoveMappingRequest, Void> holder =
                    createRequestHolder(request, request.getScope(), request.getCache(), request.getFormat());

            Binary     key    = holder.convertKeyDown(request.getKey());
            Object     value  = BinaryHelper.fromByteString(request.getValue(), holder.getSerializer());
            NamedCache cache  = holder.getCache();
            Binary     binary = (Binary) cache.invoke(key, CacheProcessors.remove(value));
            result = toBoolValue(binary, cache.getCacheService().getSerializer());
            }
        catch (Throwable e)
            {
            error = e;
            }
        handleUnary(result, error, SafeStreamObserver.ensureSafeObserver(observer));
        }

    // ----- replace --------------------------------------------------------

    @Override
    public void replace(ReplaceRequest request, StreamObserver<BytesValue> observer)
        {
        BytesValue result = null;
        Throwable  error  = null;
        try
            {
            CacheRequestHolder<ReplaceRequest, Void> holder =
                    createRequestHolder(request, request.getScope(), request.getCache(), request.getFormat());

            NamedCache<Binary, Binary> cache  = holder.getCache();
            Binary                     key    = holder.convertKeyDown(request.getKey());
            Binary                     value  = holder.convertDown(request.getValue());
            Binary                     binary = cache.invoke(key, castProcessor(new BinaryReplaceProcessor(value)));
            binary = ExternalizableHelper.fromBinary(binary, cache.getCacheService().getSerializer());
            result = holder.toBytesValue(binary);
            }
        catch (Throwable e)
            {
            error = e;
            }
        handleUnary(result, error, SafeStreamObserver.ensureSafeObserver(observer));
        }

    // ----- replace mapping ------------------------------------------------

    @Override
    public void replaceMapping(ReplaceMappingRequest request, StreamObserver<BoolValue> observer)
        {
        BoolValue result = null;
        Throwable error  = null;
        try
            {
            CacheRequestHolder<ReplaceMappingRequest, Void> holder =
                    createRequestHolder(request, request.getScope(), request.getCache(), request.getFormat());

            Binary                        key       = holder.convertKeyDown(request.getKey());
            Binary                        prevValue = holder.convertDown(request.getPreviousValue());
            Binary                        newValue  = holder.convertDown(request.getNewValue());
            BinaryReplaceMappingProcessor processor = new BinaryReplaceMappingProcessor(prevValue, newValue);
            Binary                        binary    = holder.getCache().invoke(key, castProcessor(processor));
            result = toBoolValue(binary, holder.getCacheSerializer());
            }
        catch (Throwable e)
            {
            error = e;
            }
        handleUnary(result, error, SafeStreamObserver.ensureSafeObserver(observer));
        }

    // ----- size -----------------------------------------------------------

    @Override
    public void size(SizeRequest request, StreamObserver<Int32Value> observer)
        {
        Int32Value result = null;
        Throwable  error  = null;
        try
            {
            int size = getCache(request.getScope(), request.getCache(), true).size();
            result = Int32Value.of(size);
            }
        catch (Throwable e)
            {
            error = e;
            }
        handleUnary(result, error, SafeStreamObserver.ensureSafeObserver(observer));
        }

    // ----- truncate -------------------------------------------------------

    @Override
    public void truncate(TruncateRequest request, StreamObserver<Empty> observer)
        {
        Throwable error = null;
        try
            {
            getCache(request.getScope(), request.getCache(), true).truncate();
            }
        catch (Throwable e)
            {
            error = e;
            }
        handleUnary(Empty.getDefaultInstance(), error, SafeStreamObserver.ensureSafeObserver(observer));
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
        StreamObserver<BytesValue> safeObserver = SafeStreamObserver.ensureSafeObserver(observer);
        Throwable                  error        = null;
        try
            {
            CacheRequestHolder<ValuesRequest, Void> holder =
                    createRequestHolder(request, request.getScope(), request.getCache(), request.getFormat());

            Serializer         serializer = holder.getSerializer();
            Filter<Binary>     filter     = ensureFilter(request.getFilter(), serializer);
            Comparator<Binary> comparator = deserializeComparator(request.getComparator(), serializer);

            AsyncNamedCache<Binary, Binary> cache = holder.getAsyncCache();
            if (comparator == null)
                {
                Consumer<Binary> callback = holder.binaryConsumer(safeObserver);
                cache.values(filter, callback).join();
                }
            else
                {
                Collection<Binary> col = holder.getCache().values(filter, comparator);
                col.stream().map(holder::toBytesValue).forEach(safeObserver::onNext);
                }
            }
        catch (Throwable e)
            {
            error = e;
            }
        handleErrorOrComplete(error, safeObserver);
        }
    }
