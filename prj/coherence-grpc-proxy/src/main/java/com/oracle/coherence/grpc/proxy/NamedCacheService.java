/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.proxy;

import com.google.protobuf.BoolValue;
import com.google.protobuf.ByteString;
import com.google.protobuf.BytesValue;
import com.google.protobuf.Empty;
import com.google.protobuf.Int32Value;

import com.oracle.coherence.cdi.SerializerProducer;

import com.oracle.coherence.grpc.AddIndexRequest;
import com.oracle.coherence.grpc.AggregateRequest;
import com.oracle.coherence.grpc.BinaryHelper;
import com.oracle.coherence.grpc.ClearRequest;
import com.oracle.coherence.grpc.ContainsEntryRequest;
import com.oracle.coherence.grpc.ContainsKeyRequest;
import com.oracle.coherence.grpc.ContainsValueRequest;
import com.oracle.coherence.grpc.DestroyRequest;
import com.oracle.coherence.grpc.Entry;
import com.oracle.coherence.grpc.EntryResult;
import com.oracle.coherence.grpc.EntrySetRequest;
import com.oracle.coherence.grpc.GetAllRequest;
import com.oracle.coherence.grpc.GetRequest;
import com.oracle.coherence.grpc.InvokeAllRequest;
import com.oracle.coherence.grpc.InvokeRequest;
import com.oracle.coherence.grpc.IsEmptyRequest;
import com.oracle.coherence.grpc.KeySetRequest;
import com.oracle.coherence.grpc.MapListenerRequest;
import com.oracle.coherence.grpc.MapListenerResponse;
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
import com.oracle.coherence.grpc.SizeRequest;
import com.oracle.coherence.grpc.TruncateRequest;
import com.oracle.coherence.grpc.ValuesRequest;

import com.tangosol.internal.util.collection.ConvertingNamedCache;

import com.tangosol.io.DefaultSerializer;
import com.tangosol.io.Serializer;

import com.tangosol.io.pof.ConfigurablePofContext;

import com.tangosol.net.AsyncNamedCache;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.CacheService;
import com.tangosol.net.Cluster;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.DistributedCacheService;
import com.tangosol.net.Member;
import com.tangosol.net.NamedCache;
import com.tangosol.net.PartitionedService;

import com.tangosol.net.cache.NearCache;

import com.tangosol.util.Aggregators;
import com.tangosol.util.Base;
import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.Filter;
import com.tangosol.util.Filters;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.InvocableMap.EntryProcessor;
import com.tangosol.util.NullImplementation;
import com.tangosol.util.ValueExtractor;

import com.tangosol.util.extractor.IdentityExtractor;

import com.tangosol.util.filter.AlwaysFilter;

import io.grpc.Status;

import io.grpc.stub.StreamObserver;

import io.helidon.config.Config;

import io.helidon.grpc.core.ResponseHelper;

import io.helidon.microprofile.grpc.core.Bidirectional;
import io.helidon.microprofile.grpc.core.Grpc;
import io.helidon.microprofile.grpc.core.ServerStreaming;
import io.helidon.microprofile.grpc.core.Unary;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.enterprise.context.ApplicationScoped;

import javax.inject.Inject;

import org.eclipse.microprofile.metrics.annotation.Metered;

import static com.oracle.coherence.grpc.proxy.Processors.ContainsValueProcessor;
import static com.oracle.coherence.grpc.proxy.Processors.RemoveBlindProcessor;
import static com.oracle.coherence.grpc.proxy.Processors.RemoveProcessor;
import static com.oracle.coherence.grpc.proxy.Processors.ReplaceMappingProcessor;
import static com.oracle.coherence.grpc.proxy.Processors.ReplaceProcessor;

/**
 * A gRPC NamedCache service.
 * <p>
 * This class uses {@link com.tangosol.net.AsyncNamedCache} and asynchronous {@link CompletionStage}
 * wherever possible. This makes the code more complex but the advantages of not blocking the gRPC
 * request thread or the Coherence service thread will outweigh the downside of complexity.
 * <p>
 * The asynchronous processing of {@link CompletionStage}s is done using an {@link DaemonPoolExecutor}
 * so as not to consume or block threads in the Fork Join Pool. The {@link DaemonPoolExecutor} is
 * configurable so that its thread counts can be controlled.
 * <p>
 * This class is an {@link ApplicationScoped} CDI bean and also a {@link Grpc} service. When used in a
 * Helidon Microprofile gRPC server this service will be automatically discovered and deployed.
 * Alternatively it is possible to use the {@link NamedCacheService.Builder} to create an instance of
 * this service and manually deploy it.
 *
 * @author Mahesh Kannan    2019.11.01
 * @author Jonathan Knight  2019.11.07
 * @since 14.1.2
 */
@Grpc(name = NamedCacheService.SERVICE_NAME)
@ApplicationScoped
public class NamedCacheService
        implements NamedCacheClient
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a {@link NamedCacheService}.
     *
     * @param cluster     the Coherence {@link Cluster}
     * @param ccf         the {@link ConfigurableCacheFactory} that will be used by this
     *                    service to access Coherence resources
     * @param serializer  the producer to use to lookup named {@link Serializer} instances
     * @param config      the {@link Config} to use to configure the service
     */
    @Inject
    public NamedCacheService(Cluster cluster, ConfigurableCacheFactory ccf,
                             SerializerProducer serializer, Config config)
        {
        this.cacheFactory       = ccf;
        this.serializerProducer = serializer;

        Config serviceConfig = config.get(CONFIG_PREFIX);

        serviceConfig.get(CONFIG_TRANSFER_THRESHOLD).asLong().ifPresent(this::setTransferThreshold);

        if (serviceConfig.get(CONFIG_USE_DAEMON_POOL).asBoolean().orElse(true))
            {
            DaemonPoolExecutor pool = DaemonPoolExecutor.builder(serviceConfig)
                    .name(SERVICE_NAME)
                    .registry(cluster::getManagement)
                    .build();
            pool.start();
            this.executor = pool;
            }
        else
            {
            this.executor = ForkJoinPool.commonPool();
            }
        }

    // ----- builder methods ------------------------------------------------

    /**
     * Create a {@link NamedCacheService} that will use the default {@link ConfigurableCacheFactory}.
     *
     * @return an instance of {@link com.oracle.coherence.grpc.proxy.NamedCacheService}
     */
    public static NamedCacheService create()
        {
        return builder().build();
        }

    /**
     * Obtain a {@link Builder} to build {@link NamedCacheService} instances.
     *
     * @return a {@link Builder} to build {@link NamedCacheService} instances
     */
    public static Builder builder()
        {
        return builder(Config.empty());
        }

    /**
     * Obtain a {@link Builder} to build {@link NamedCacheService} instances.
     *
     * @param config  the {@link Config} to use to configure the {@link NamedCacheService}
     *
     * @return a {@link Builder} to build {@link NamedCacheService} instances
     */
    public static Builder builder(Config config)
        {
        return new Builder(config);
        }

    // ----- accessors ------------------------------------------------------

    /**
     * Return the transfer threshold.
     *
     * @return the {@link #transferThreshold}
     */
    long getTransferThreshold()
        {
        return transferThreshold;
        }

    /**
     * Set the transfer threshold.
     *
     * @param lSize  the new transfer threshold
     */
    void setTransferThreshold(long lSize)
        {
        transferThreshold = lSize;
        }

    // ----- NamedCacheClient implementation --------------------------------

    // ----- addIndex -------------------------------------------------------

    @Unary
    @Metered
    @Override
    public CompletionStage<Empty> addIndex(AddIndexRequest request)
        {
        return createHolder(request, request.getCache(), request.getFormat())
                .thenApplyAsync(this::addIndex, executor);
        }

    /**
     * Execute the {@link AddIndexRequest} request.
     *
     * @param holder  the {@link CacheRequestHolder} containing the {@link AddIndexRequest} request
     *
     * @return {@link BinaryHelper#EMPTY}
     */
    @SuppressWarnings({"rawtypes"})
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

    @Unary
    @Metered
    @Override
    public CompletionStage<BytesValue> aggregate(AggregateRequest request)
        {
        ByteString processorBytes = request.getAggregator();
        if (processorBytes.isEmpty())
            {
            CompletableFuture<BytesValue> future = new CompletableFuture<>();
            future.completeExceptionally(
                    Status.INVALID_ARGUMENT
                        .withDescription("the request does not contain a serialized entry aggregator")
                        .asRuntimeException());
            return future;
            }
        else
            {
            try
                {
                if (request.getKeysCount() != 0)
                    {
                    // aggregate with keys
                    return aggregateWithKeys(request);
                    }
                else
                    {
                    // aggregate with filter
                    return aggregateWithFilter(request);
                    }
                }
            catch (Throwable t)
                {
                throw ErrorsHelper.ensureStatusRuntimeException(t);
                }
            }
        }

    /**
     * Execute the filtered {@link AggregateRequest} request.
     *
     * @param request  the {@link AggregateRequest}
     *
     * @return a {@link CompletionStage} that completes with a {@link CacheRequestHolder} containing
     *         the serialized result of executing request
     */
    protected CompletionStage<BytesValue> aggregateWithFilter(AggregateRequest request)
        {
        return createHolder(request, request.getCache(), request.getFormat())
                .thenComposeAsync(this::aggregateWithFilter, executor)
                .handleAsync(this::handleError, executor);
        }

    /**
     * Execute the filtered {@link AggregateRequest} request.
     *
     * @param holder  the {@link CacheRequestHolder} containing the {@link ContainsEntryRequest} request
     *
     * @return a {@link CompletionStage} that completes with a {@link CacheRequestHolder} containing
     *         the serialized result of executing request
     */
    protected CompletionStage<BytesValue> aggregateWithFilter(CacheRequestHolder<AggregateRequest, Void> holder)
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
    protected CompletionStage<BytesValue> aggregateWithKeys(AggregateRequest request)
        {
        return createHolder(request, request.getCache(), request.getFormat())
                .thenComposeAsync(this::aggregateWithKeys, executor)
                .handleAsync(this::handleError, executor);
        }

    /**
     * Execute the filtered {@link AggregateRequest} request.
     *
     * @param holder  the {@link CacheRequestHolder} containing the {@link ContainsEntryRequest} request
     *
     * @return a {@link CompletionStage} that completes with a {@link CacheRequestHolder} containing
     *         the serialized result of executing request
     */
    protected CompletionStage<BytesValue> aggregateWithKeys(CacheRequestHolder<AggregateRequest, Void> holder)
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

    @Unary
    @Metered
    @Override
    public CompletionStage<Empty> clear(ClearRequest request)
        {
        return getAsyncCache(request.getCache())
                .thenComposeAsync(cache -> cache.invokeAll(AlwaysFilter.INSTANCE(),
                                                           RemoveBlindProcessor.INSTANCE),
                                  executor)
                .thenApplyAsync(this::empty, executor);
        }

    // ----- containsEntry --------------------------------------------------

    @Unary
    @Metered
    @Override
    public CompletionStage<BoolValue> containsEntry(ContainsEntryRequest request)
        {
        return createHolder(request, request.getCache(), request.getFormat())
                .thenComposeAsync(this::containsEntry, executor)
                .thenApplyAsync(h -> toBoolValue(h.getResult(), h.getCacheSerializer()), executor);
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

        EntryProcessor<Binary, Binary, Binary> processor = castProcessor(new ContainsValueProcessor(value));
        return holder.runAsync(holder.getAsyncCache().invoke(key, processor));
        }

    // ----- containsEntry --------------------------------------------------

    @Unary
    @Metered
    @Override
    public CompletionStage<BoolValue> containsKey(ContainsKeyRequest request)
        {
        return createHolder(request, request.getCache(), request.getFormat())
                .thenComposeAsync(this::containsKey, executor)
                .thenApplyAsync(h -> BoolValue.of(h.getDeserializedResult()), executor);
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

    @Unary
    @Metered
    @Override
    public CompletionStage<BoolValue> containsValue(ContainsValueRequest request)
        {
        return createHolder(request, request.getCache(), request.getFormat())
                .thenComposeAsync(this::containsValue, executor)
                .thenApplyAsync(h -> BoolValue.of(h.getResult() > 0), executor);
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

    // ----- destroy --------------------------------------------------------

    @Unary
    @Metered
    @Override
    public CompletionStage<Empty> destroy(DestroyRequest request)
        {
        return getAsyncCache(request.getCache())
                .thenApplyAsync(cache -> this.execute(() -> cache.getNamedCache().destroy()), executor);
        }

    // ----- entrySet -------------------------------------------------------

    @ServerStreaming
    @Metered
    @Override
    public void entrySet(EntrySetRequest request, StreamObserver<Entry> observer)
        {
        createHolder(request, request.getCache(), request.getFormat())
                .thenApplyAsync(h -> this.entrySet(h, observer), executor)
                .handleAsync((v, err) -> this.handleError(err, observer), executor);
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

            holder.runAsync(holder.getAsyncCache().entrySet(filter, comparator))
                    .handleAsync((h, err) -> this.handleSetOfEntries(h, err, observer, false), executor);
            }
        catch (Throwable t)
            {
            observer.onError(ErrorsHelper.ensureStatusRuntimeException(t));
            }
        return VOID;
        }

    // ----- events ---------------------------------------------------------

    @Bidirectional
    @Metered
    @Override
    public StreamObserver<MapListenerRequest> events(StreamObserver<MapListenerResponse> observer)
        {
        return new MapListenerProxy(this, observer);
        }

    // ----- get ------------------------------------------------------------

    @Unary
    @Metered
    @Override
    public CompletionStage<OptionalValue> get(GetRequest request)
        {
        return createHolder(request, request.getCache(), request.getFormat())
                .thenComposeAsync(this::get, executor)
                .thenApplyAsync(h -> h.toOptionalValue(h.getDeserializedResult()), executor);
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

        EntryProcessor<Binary, Binary, Binary> processor = Processors.get();

        return holder.runAsync(holder.getAsyncCache().invoke(key, processor));
        }

    // ----- getAll ---------------------------------------------------------

    @ServerStreaming
    @Metered
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
            createHolder(request, request.getCache(), request.getFormat())
                    .thenApplyAsync(h -> this.getAll(h, observer), executor)
                    .handleAsync((v, err) -> this.handleError(err, observer), executor);
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
        holder.runAsync(convertKeys(holder))
                .thenComposeAsync(h -> h.runAsync(h.getAsyncCache().invokeAll(
                        h.getResult(), Processors.get())), executor)
                .handleAsync((h, err) -> handleMapOfEntries(h, err, observer, true), executor);
        return VOID;
        }

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
        return CompletableFuture.supplyAsync(() ->
            {
            GetAllRequest request = holder.getRequest();
            return request.getKeyList()
                    .stream()
                    .map(holder::convertKeyDown)
                    .collect(Collectors.toList());
            }, executor);
        }

    // ----- invoke ---------------------------------------------------------

    @Unary
    @Metered
    @Override
    public CompletionStage<BytesValue> invoke(InvokeRequest request)
        {
        ByteString processorBytes = request.getProcessor();
        if (processorBytes.isEmpty())
            {
            CompletableFuture<BytesValue> future = new CompletableFuture<>();
            future.completeExceptionally(Status.INVALID_ARGUMENT
                                                 .withDescription("the request does not contain a serialized"
                                                                  + " entry processor")
                                                 .asRuntimeException());
            return future;
            }

        return createHolder(request, request.getCache(), request.getFormat())
                .thenComposeAsync(this::invoke, executor)
                .thenApplyAsync(h -> BinaryHelper.toBytesValue(h.convertUp(h.getResult())), executor);
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

    @ServerStreaming
    @Metered
    @Override
    public void invokeAll(InvokeAllRequest request, StreamObserver<Entry> observer)
        {
        ByteString processorBytes = request.getProcessor();
        if (processorBytes.isEmpty())
            {
            observer.onError(Status.INVALID_ARGUMENT
                                     .withDescription("the request does not contain a serialized entry processor")
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

                future.handleAsync((v, err) -> this.handleError(err, observer), executor);
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
        return createHolder(request, request.getCache(), request.getFormat())
                .thenComposeAsync(h -> invokeAllWithFilter(h, observer), executor);
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

        return holder.runAsync(holder.getAsyncCache().invokeAll(filter, processor))
                .handleAsync((h, err) -> handleMapOfEntries(h, err, observer, false), executor);
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
        return createHolder(request, request.getCache(), request.getFormat())
                .thenComposeAsync(h -> invokeAllWithKeys(h, observer), executor);
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

        return holder.runAsync(holder.getAsyncCache().invokeAll(keys, processor))
                .handleAsync((h, err) -> handleMapOfEntries(h, err, observer, false), executor);
        }

    // ----- isEmpty --------------------------------------------------------

    @Unary
    @Metered
    @Override
    public CompletionStage<BoolValue> isEmpty(IsEmptyRequest request)
        {
        return getAsyncCache(request.getCache())
                .thenComposeAsync(AsyncNamedCache::isEmpty, executor)
                .thenApplyAsync(BoolValue::of, executor);
        }

    // ----- keySet ---------------------------------------------------------

    @ServerStreaming
    @Metered
    @Override
    public void keySet(KeySetRequest request, StreamObserver<BytesValue> observer)
        {
        createHolder(request, request.getCache(), request.getFormat())
                .thenApplyAsync(h -> this.keySet(h, observer), executor)
                .handleAsync((v, err) -> this.handleError(err, observer), executor);
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

            holder.runAsync(holder.getAsyncCache().keySet(filter))
                    .handleAsync((h, err) -> this.handleStream(h, err, observer), executor)
                    .handleAsync((v, err) -> this.handleError(err, observer), executor);
            }
        catch (Throwable t)
            {
            observer.onError(ErrorsHelper.ensureStatusRuntimeException(t));
            }
        return VOID;
        }

    // ----- Paged Queries keySet() entrySet() values() ---------------------

    @ServerStreaming
    @Metered
    @Override
    public void nextKeySetPage(PageRequest request, StreamObserver<BytesValue> observer)
        {
        createHolder(request, request.getCache(), request.getFormat())
                .thenApplyAsync(h -> PagedQueryHelper.keysPagedQuery(h, getTransferThreshold()), executor)
                .handleAsync((stream, err) -> handleStream(stream, err, observer), executor)
                .handleAsync((v, err) -> this.handleError(err, observer));
        }

    @ServerStreaming
    @Metered
    @Override
    public void nextEntrySetPage(PageRequest request, StreamObserver<EntryResult> observer)
        {
        createHolder(request, request.getCache(), request.getFormat())
                .thenApplyAsync(h -> PagedQueryHelper.entryPagedQuery(h, getTransferThreshold()), executor)
                .handleAsync((stream, err) -> handleStream(stream, err, observer), executor)
                .handleAsync((v, err) -> this.handleError(err, observer));
        }

    // ----- put ------------------------------------------------------------

    @Unary
    @Metered
    @Override
    public CompletionStage<BytesValue> put(PutRequest request)
        {
        return createHolder(request, request.getCache(), request.getFormat())
                .thenComposeAsync(this::put, executor);
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

        return holder.getAsyncCache().invoke(key, Processors.put(value, request.getTtl()))
                .thenApplyAsync(holder::deserializeToBytesValue, executor);
        }

    // ----- putAll ---------------------------------------------------------

    @Unary
    @Metered
    @Override
    public CompletionStage<Empty> putAll(PutAllRequest request)
        {
        return createHolder(request, request.getCache(), request.getFormat())
                .thenComposeAsync(this::putAll, executor);
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
            return plainPutAll(holder.getAsyncCache(), map);
            }
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
            CompletableFuture<?>[]          futures = mapByOwner.values()
                    .stream()
                    .map(mapForMember -> plainPutAll(cache, mapForMember))
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
     * @param cache  the {@link com.tangosol.net.AsyncNamedCache} to update
     * @param map    the map of {@link Binary} keys and values to put into the cache
     *
     * @return a {@link CompletionStage} that completes when the {@code putAll} operation completes
     */
    protected CompletionStage<Empty> plainPutAll(AsyncNamedCache<Binary, Binary> cache, Map<Binary, Binary> map)
        {
        return cache.invokeAll(map.keySet(), Processors.putAll(map))
                .thenApplyAsync(v -> BinaryHelper.EMPTY, executor);
        }

    // ----- putIfAbsent ----------------------------------------------------

    @Unary
    @Metered
    @Override
    public CompletionStage<BytesValue> putIfAbsent(PutIfAbsentRequest request)
        {
        return createHolder(request, request.getCache(), request.getFormat())
                .thenComposeAsync(this::putIfAbsent, executor);
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

        return holder.getAsyncCache().invoke(key, Processors.putIfAbsent(value, request.getTtl()))
                .thenApplyAsync(holder::deserializeToBytesValue, executor);
        }

    // ----- remove ---------------------------------------------------------

    @Unary
    @Metered
    @Override
    public CompletionStage<BytesValue> remove(RemoveRequest request)
        {
        return createHolder(request, request.getCache(), request.getFormat())
                .thenComposeAsync(h -> h.runAsync(remove(h)), executor)
                .thenApplyAsync(h -> h.toBytesValue(h.getResult()), executor);
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

        return holder.getAsyncCache().invoke(key, RemoveProcessor.INSTANCE)
                .thenApplyAsync(holder::fromCacheBinary, executor);
        }

    // ----- removeIndex ----------------------------------------------------

    @Unary
    @Metered
    @Override
    public CompletionStage<Empty> removeIndex(RemoveIndexRequest request)
        {
        return createHolder(request, request.getCache(), request.getFormat())
                .thenApplyAsync(this::removeIndex, executor);
        }

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

    // ----- remove mapping -------------------------------------------------

    @Unary
    @Metered
    @Override
    public CompletionStage<BoolValue> removeMapping(RemoveMappingRequest request)
        {
        return createHolder(request, request.getCache(), request.getFormat())
                .thenComposeAsync(this::removeMapping, executor)
                .thenApplyAsync(h -> BoolValue.of(h.getDeserializedResult()), executor);
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

    @Unary
    @Metered
    @Override
    public CompletionStage<BytesValue> replace(ReplaceRequest request)
        {
        return createHolder(request, request.getCache(), request.getFormat())
                .thenComposeAsync(h -> h.runAsync(replace(h)), executor)
                .thenApplyAsync(h -> h.toBytesValue(h.getResult()), executor);
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

        return holder.getAsyncCache().invoke(key, castProcessor(new ReplaceProcessor(value)))
                .thenApplyAsync(holder::fromCacheBinary, executor);
        }

    // ----- replace mapping ------------------------------------------------

    @Unary
    @Metered
    @Override
    public CompletionStage<BoolValue> replaceMapping(ReplaceMappingRequest request)
        {
        return createHolder(request, request.getCache(), request.getFormat())
                .thenComposeAsync(h -> h.runAsync(replaceMapping(h)), executor)
                .thenApplyAsync(h -> toBoolValue(h.getResult(), h.getCacheSerializer()), executor);
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

        return holder.getAsyncCache().invoke(key, castProcessor(new ReplaceMappingProcessor(prevValue, newValue)));
        }

    // ----- size -----------------------------------------------------------

    @Unary
    @Metered
    @Override
    public CompletionStage<Int32Value> size(SizeRequest request)
        {
        CompletionStage<Int32Value> s = getAsyncCache(request.getCache())
                .thenComposeAsync(AsyncNamedCache::size, executor)
                .thenApplyAsync(Int32Value::of, executor);
        s.handle((sz, err) -> null);
        return s;
        }

    // ----- truncate -------------------------------------------------------

    @Unary
    @Metered
    @Override
    public CompletionStage<Empty> truncate(TruncateRequest request)
        {
        return getAsyncCache(request.getCache())
                .thenApplyAsync(cache -> this.execute(() -> cache.getNamedCache().truncate()), executor);
        }

    // ----- values ---------------------------------------------------------

    /**
     * Execute the {@link ValuesRequest} request passing the results to the provided
     * {@link StreamObserver}.
     *
     * @param request   the {@link ValuesRequest}
     * @param observer  the {@link StreamObserver} which will receive the results
     */
    @ServerStreaming
    @Metered
    @Override
    public void values(ValuesRequest request, StreamObserver<BytesValue> observer)
        {
        createHolder(request, request.getCache(), request.getFormat())
                .thenApplyAsync(h -> this.values(h, observer), executor)
                .handleAsync((v, err) -> this.handleError(err, observer), executor);
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

            holder.runAsync(holder.getAsyncCache().values(filter, comparator))
                    .handleAsync((h, err) -> this.handleStream(h, err, observer), executor)
                    .handleAsync((v, err) -> this.handleError(err, observer), executor);
            }
        catch (Throwable t)
            {
            observer.onError(ErrorsHelper.ensureStatusRuntimeException(t));
            }
        return VOID;
        }

    // ----- helper methods -------------------------------------------------

    /**
     * A helper method that always returns {@link Empty}.
     * <p>
     * This method is to make {@link CompletionStage} handler code
     * a little more elegant as it can use this method as a method
     * reference.
     *
     * @param value  the value
     * @param <V>    the type of the value
     *
     * @return an {@link Empty} instance.
     */
    protected <V> Empty empty(V value)
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
     * Handle the result of the asynchronous invoke all request sending the results, or any errors
     * to the {@link StreamObserver}.
     *
     * @param holder        the {@link CacheRequestHolder} containing the request
     * @param err          any error that occurred during execution of the get all request
     * @param observer      the {@link StreamObserver} to receive the results
     * @param fDeserialize  a flag indicating whether the {@link Binary} values should be deserialized
     *
     * @return always return {@link Void}
     */
    protected Void handleMapOfEntries(CacheRequestHolder<?, Map<Binary, Binary>> holder,
                                      Throwable err,
                                      StreamObserver<Entry> observer,
                                      boolean fDeserialize)
        {
        if (err == null)
            {
            handleStreamOfEntries(holder, holder.getResult().entrySet().stream(), observer, fDeserialize);
            }
        else
            {
            observer.onError(ErrorsHelper.ensureStatusRuntimeException(err));
            }
        return VOID;
        }

    /**
     * Handle the result of the asynchronous entry set request sending the results, or any errors
     * to the {@link StreamObserver}.
     *
     * @param holder        the {@link CacheRequestHolder} containing the request
     * @param err           any error that occurred during execution of the get all request
     * @param observer      the {@link StreamObserver} to receive the results
     * @param fDeserialize  a flag indicating whether the {@link Binary} values should be deserialized
     *
     * @return always return {@link Void}
     */
    protected Void handleSetOfEntries(CacheRequestHolder<?, Set<Map.Entry<Binary, Binary>>> holder,
                                    Throwable err,
                                    StreamObserver<Entry> observer,
                                    boolean fDeserialize)
        {
        if (err == null)
            {
            handleStreamOfEntries(holder, holder.getResult().stream(), observer, fDeserialize);
            }
        else
            {
            observer.onError(ErrorsHelper.ensureStatusRuntimeException(err));
            }
        return VOID;
        }

    /**
     * Handle the result of the asynchronous invoke all request sending the results, or any errors
     * to the {@link StreamObserver}.
     *
     * @param holder        the {@link CacheRequestHolder} containing the request
     * @param entries       a {@link Stream<Entry>} of entries
     * @param observer      the {@link StreamObserver} to receive the results
     * @param fDeserialize  a flag indicating whether the {@link Binary} values should be deserialized
     *
     * @return always return {@link Void}
     */
    protected Void handleStreamOfEntries(CacheRequestHolder<?, ?> holder,
                                         Stream<Map.Entry<Binary, Binary>> entries,
                                         StreamObserver<Entry> observer,
                                         boolean fDeserialize)
        {
        try
            {
            entries.forEach(entry ->
                {
                Binary binValue = entry.getValue();
                if (fDeserialize)
                    {
                    // The Binary value returned by the GetProcessor is actually a serialized Binary
                    // so we need to fDeserialize it first
                    binValue = holder.fromCacheBinary(entry.getValue());
                    }
                observer.onNext(holder.toEntry(entry.getKey(), binValue));
                });

            observer.onCompleted();
            }
        catch (Throwable thrown)
            {
            observer.onError(ErrorsHelper.ensureStatusRuntimeException(thrown));
            }
        return VOID;
        }

    /**
     * Send an {@link java.lang.Iterable} of {@link Binary} instances to a {@link StreamObserver},
     * converting the {@link Binary} instances to a {@link BytesValue}.
     *
     * @param holder    the {@link CacheRequestHolder} containing the request and
     *                  {@link Iterable} or {@link Binary} instances to stream
     * @param err       the error the pass to {@link StreamObserver#onError(Throwable)}
     * @param observer  the {@link StreamObserver} to receive the results
     *
     * @return always return {@link Void}
     */
    protected Void handleStream(CacheRequestHolder<?, ? extends Iterable<Binary>> holder,
                                Throwable err,
                                StreamObserver<BytesValue> observer)
        {
        if (err == null)
            {
            try
                {
                Iterable<Binary>   iterable = holder.getResult();
                Stream<BytesValue> stream   = StreamSupport.stream(iterable.spliterator(), false)
                        .map(bin -> BinaryHelper.toBytesValue(holder.convertUp(bin)));
                ResponseHelper.stream(observer, stream);
                }
            catch (Throwable t)
                {
                observer.onError(ErrorsHelper.ensureStatusRuntimeException(t));
                }
            }
        else
            {
            observer.onError(ErrorsHelper.ensureStatusRuntimeException(err));
            }
        return VOID;
        }

    /**
     * A handler method that streams results to a {@link StreamObserver}
     * and completes the {@link StreamObserver} or if an error is
     * provided calls {@link StreamObserver#onError(Throwable)}.
     * <p>
     * Note: this method will complete by calling either {@link StreamObserver#onCompleted()}
     * or {@link StreamObserver#onError(Throwable)}.
     *
     * @param stream    the elements to stream to the {@link StreamObserver}
     * @param err       the error the pass to {@link StreamObserver#onError(Throwable)}
     * @param observer  the {@link StreamObserver}
     * @param <Resp>    the type of the element to stream to the {@link StreamObserver}
     *
     * @return always returns {@link Void}
     */
    protected <Resp> Void handleStream(Stream<Resp> stream, Throwable err, StreamObserver<Resp> observer)
        {
        if (err == null)
            {
            try
                {
                ResponseHelper.stream(observer, stream);
                }
            catch (Throwable t)
                {
                observer.onError(ErrorsHelper.ensureStatusRuntimeException(t));
                }
            }
        else
            {
            observer.onError(ErrorsHelper.ensureStatusRuntimeException(err));
            }
        return VOID;
        }

    /**
     * A handler method that will call {@link StreamObserver#onError(Throwable)} if the
     * error parameter is not {@code null}.
     * <p>
     * NOTE: this method will not complete the {@link StreamObserver} if there is no error.
     *
     * @param err       the error the pass to {@link StreamObserver#onError(Throwable)}
     * @param observer  the {@link StreamObserver}
     * @param <Resp>    the type of the element to stream to the {@link StreamObserver}
     *
     * @return always returns {@link Void}
     */
    protected <Resp> Void handleError(Throwable err, StreamObserver<Resp> observer)
        {
        if (err != null)
            {
            observer.onError(ErrorsHelper.ensureStatusRuntimeException(err));
            }
        return VOID;
        }

    /**
     * A handler method that will return the response if there is no error or if there
     * is an error then ensure that it is a {@link io.grpc.StatusRuntimeException}.
     *
     * @param response  the response to return if there is no error
     * @param err       the error to check
     * @param <Resp>    the type of the response
     *
     * @return always returns the passed in response
     */
    protected <Resp> Resp handleError(Resp response, Throwable err)
        {
        if (err != null)
            {
            throw ErrorsHelper.ensureStatusRuntimeException(err);
            }
        return response;
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
    ValueExtractor<?, ?> ensureValueExtractor(ByteString bytes, Serializer serializer)
        {
        if (bytes == null || bytes.isEmpty())
            {
            throw Status.INVALID_ARGUMENT
                    .withDescription("the request does not contain a serialized ValueExtractor")
                    .asRuntimeException();
            }

        return BinaryHelper.fromByteString(bytes, serializer);
        }

    /**
     * Obtain a {@link Filter} from the serialized data in a {@link ByteString}.
     * <p>
     * If the {@link ByteString} is {@code null} or {@link ByteString#EMPTY} then
     * an {@link com.tangosol.util.filter.AlwaysFilter} is returned.
     *
     * @param bytes       the {@link ByteString} containing the serialized {@link Filter}
     * @param serializer  the serializer to use
     * @param <T>         the {@link Filter} type
     *
     * @return a deserialized {@link Filter}
     */
    <T> Filter<T> ensureFilter(ByteString bytes, Serializer serializer)
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
     *  @param <T>        the {@link Filter} type
     *
     * @return a deserialized {@link Filter} or {@code null} if no filter is set
     */
    <T> Filter<T> getFilter(ByteString bytes, Serializer serializer)
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
     *  @param <T>        the {@link Comparator} type
     *
     * @return a deserialized {@link Comparator} or {@code null} if the {@link ByteString}
     *         is {@code null} or {@link ByteString#EMPTY}
     */
    <T> Comparator<T> deserializeComparator(ByteString bytes, Serializer serializer)
        {
        if (bytes == null || bytes.isEmpty())
            {
            return null;
            }
        return BinaryHelper.fromByteString(bytes, serializer);
        }

    /**
     * Obtain an {@link com.tangosol.net.AsyncNamedCache}.
     *
     * @param cacheName  the name of the cache
     *
     * @return the {@link com.tangosol.net.AsyncNamedCache} with the specified name
     */
    protected CompletionStage<AsyncNamedCache<Binary, Binary>> getAsyncCache(String cacheName)
        {
        return CompletableFuture.supplyAsync(() -> getPassThroughCache(cacheName).async(), executor);
        }

    /**
     * Obtain an {@link com.tangosol.net.NamedCache}.
     *
     * @param cacheName  the name of the cache
     *
     * @return the {@link com.tangosol.net.NamedCache} with the specified name
     */
    protected NamedCache<Binary, Binary> getPassThroughCache(String cacheName)
        {
        return getCache(cacheName, true);
        }

    /**
     * Obtain an {@link com.tangosol.net.NamedCache}.
     *
     * @param cacheName  the name of the cache
     * @param passThru   {@code true} to use a binary pass-thru cache
     *
     * @return the {@link com.tangosol.net.NamedCache} with the specified name
     */
    @SuppressWarnings("unchecked")
    protected NamedCache<Binary, Binary> getCache(String cacheName, boolean passThru)
        {
        if (cacheName == null || cacheName.trim().length() == 0)
            {
            throw Status.INVALID_ARGUMENT
                    .withDescription("invalid request, cache name cannot be null or empty")
                    .asRuntimeException();
            }

        ClassLoader loader = passThru ? NullImplementation.getClassLoader() : Base.getContextClassLoader();

        NamedCache<Binary, Binary> cache  = cacheFactory.ensureCache(cacheName, loader);

        // optimize front-cache out of storage enabled proxies
        boolean near = cache instanceof NearCache;
        if (near)
            {
            CacheService service = cache.getCacheService();
            if (service instanceof DistributedCacheService
                && ((DistributedCacheService) service).isLocalStorageEnabled())
                {
                cache = ((NearCache<Binary, Binary>) cache).getBackCache();
                }
            }

        if (near)
            {
            return new ConvertingNamedCache(cache,
                                            NullImplementation.getConverter(),
                                            ExternalizableHelper.CONVERTER_STRIP_INTDECO,
                                            NullImplementation.getConverter(),
                                            NullImplementation.getConverter());
            }
        else
            {
            return cache;
            }
        }

    /**
     * Cast an {@link com.tangosol.util.InvocableMap.EntryProcessor} to an
     * {@link com.tangosol.util.InvocableMap.EntryProcessor} that returns a
     * {@link Binary} result.
     *
     * @param ep  the {@link com.tangosol.util.InvocableMap.EntryProcessor} to cast
     *
     * @return a {@link com.tangosol.util.InvocableMap.EntryProcessor} that returns
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
     * @param sCacheName  the name of the cache that the request executes against
     * @param format      the optional serialization format used by requests that contain a payload
     * @param <Req>       the type of the request
     *
     * @return a {@link CompletionStage} that completes when the {@link CacheRequestHolder} has
     *         been created
     */
    <Req> CompletionStage<CacheRequestHolder<Req, Void>> createHolder(Req request, String sCacheName, String format)
        {
        return CompletableFuture.supplyAsync(() -> supplyHolderInternal(request, sCacheName, format), executor);
        }

    /**
     * Create a {@link CacheRequestHolder} for a given request.
     *
     * @param <Req>       the type of the request
     * @param request     the request object to add to the holder
     * @param sCacheName  the name of the cache that the request executes against
     * @param format      the optional serialization format used by requests that contain a payload
     *
     * @return the {@link CacheRequestHolder} holding the request
     */
    <Req> CacheRequestHolder<Req, Void> supplyHolderInternal(Req request, String sCacheName, String format)
        {
        if (request == null)
            {
            throw Status.INVALID_ARGUMENT
                    .withDescription("invalid request, the request cannot be null")
                    .asRuntimeException();
            }

        if (sCacheName == null || sCacheName.isEmpty())
            {
            throw Status.INVALID_ARGUMENT
                    .withDescription("invalid request, cache name cannot be null or empty")
                    .asRuntimeException();
            }

        NamedCache<Binary, Binary>      c               = getCache(sCacheName, true);
        NamedCache<Binary, Binary>      nonPassThrough  = getCache(sCacheName, false);
        AsyncNamedCache<Binary, Binary> cache           = c.async();
        CacheService                    cacheService    = cache.getNamedCache().getCacheService();
        Serializer                      serializerCache = cacheService.getSerializer();
        String                          cacheFormat     = serializerCache.getName();

        if ((cacheFormat == null || cacheFormat.isEmpty()) && serializerCache instanceof DefaultSerializer)
            {
            cacheFormat = "java";
            }
        else if ((cacheFormat == null || cacheFormat.isEmpty()) && serializerCache instanceof ConfigurablePofContext)
            {
            cacheFormat = "pof";
            }

        Serializer serializerRequest;
        if (format == null || format.trim().isEmpty() || format.equals(cacheFormat))
            {
            serializerRequest = serializerCache;
            }
        else
            {
            ClassLoader loader = cacheService.getContextClassLoader();
            serializerRequest  = serializerProducer.getNamedSerializer(format, loader);
            }

        if (serializerRequest == null)
            {
            throw Status.INVALID_ARGUMENT
                    .withDescription("invalid request format, cannot find serializer with name '" + format + "'")
                    .asRuntimeException();
            }

        return new CacheRequestHolder<>(request,
                                        cache,
                                        () -> nonPassThrough,
                                        format,
                                        serializerRequest, executor);
        }

    // ----- inner class: Builder -------------------------------------------

    /**
     * A builder that builds instances of {@link NamedCacheService}.
     */
    public static class Builder
            implements io.helidon.common.Builder<NamedCacheService>
        {
        // ----- constructors -----------------------------------------------

        /**
         * The initial configuration for this builder.
         *
         * @param config initial configuration
         */
        protected Builder(Config config)
            {
            this.m_config = config == null ? Config.empty() : config;
            }

        // ----- Builder interface ------------------------------------------

        @Override
        public NamedCacheService build()
            {
            return new NamedCacheService(ensureCluster(),
                                         ensureConfigurableCacheFactory(),
                                         ensureSerializerProducer(),
                                         m_config);
            }

        // ----- public methods ---------------------------------------------

        /**
         * Set the {@link ConfigurableCacheFactory} used to obtain Coherence resources.
         *
         * @param ccf  the {@link ConfigurableCacheFactory} to use
         *
         * @return this {@link Builder}
         */
        public Builder configurableCacheFactory(ConfigurableCacheFactory ccf)
            {
            this.m_ccf = ccf;
            return this;
            }

        /**
         * Set the {@link SerializerProducer} used to obtain {@link Serializer} instances.
         *
         * @param producer  the {@link SerializerProducer} to use
         *
         * @return this {@link Builder}
         */
        public Builder serializerProducer(SerializerProducer producer)
            {
            this.m_serializerProducer = producer;
            return this;
            }

        // ----- helper methods ---------------------------------------------

        /**
         * Returns a new or previously cached {@link ConfigurableCacheFactory}.
         *
         * @return a new or previously cached {@link ConfigurableCacheFactory}
         */
        protected ConfigurableCacheFactory ensureConfigurableCacheFactory()
            {
            if (m_ccf == null)
                {
                return CacheFactory.getCacheFactoryBuilder().getConfigurableCacheFactory(Base.getContextClassLoader());
                }
            return m_ccf;
            }

        /**
         * Returns a new or previously cached {@link SerializerProducer}.
         *
         * @return a new or previously cached {@link SerializerProducer}
         */
        protected SerializerProducer ensureSerializerProducer()
            {
            if (m_serializerProducer == null)
                {
                return SerializerProducer.create();
                }
            return m_serializerProducer;
            }

        /**
         * Returns a new or previously cached {@link Cluster}.
         *
         * @return a new or previously cached {@link Cluster}
         */
        protected Cluster ensureCluster()
            {
            if (m_cluster == null)
                {
                m_cluster = CacheFactory.ensureCluster();
                }
            return m_cluster;
            }

        // ----- data members -----------------------------------------------

        /**
         * The {@link ConfigurableCacheFactory}.
         */
        protected ConfigurableCacheFactory m_ccf;

        /**
         * The Coherence {@link Cluster}.
         */
        protected Cluster m_cluster;

        /**
         * The {@link SerializerProducer}.
         */
        protected SerializerProducer m_serializerProducer;

        /**
         * The {@link Config}.
         */
        protected Config m_config;
        }

    // ----- constants --------------------------------------------------

    /**
     * The prefix used by configuration keys.
     */
    protected static final String CONFIG_PREFIX = "coherence.named_cache_service";

    /**
     * The configuration key to determine whether to use a daemon pool
     * for async operations ({@code true}) or the ForkJoin pool {@code false}.
     */
    protected static final String CONFIG_USE_DAEMON_POOL = "use_daemon_pool";

    /**
     * The transfer threshold to use to determine page sizes for paged queries.
     */
    protected static final String CONFIG_TRANSFER_THRESHOLD = "transfer_threshold";

    /**
     * A {@link Void} value to make it obvious the return value in Void methods.
     */
    protected static final Void VOID = null;

    /**
     * The name of the gRPC service.
     */
    public static final String SERVICE_NAME = "coherence.NamedCacheService";

    // ----- data members -----------------------------------------------

    /**
     * The {@link ConfigurableCacheFactory} used to obtain caches.
     */
    protected final ConfigurableCacheFactory cacheFactory;

    /**
     * The producer to use to lookup named {@link Serializer} instances.
     */
    protected final SerializerProducer serializerProducer;

    /**
     * The {@link Executor} to use to hand off asynchronous tasks.
     */
    protected final Executor executor;

    /**
     * The transfer threshold used for paged requests.
     */
    protected long transferThreshold = 524288;
    }
