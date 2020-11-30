/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.proxy;

import com.google.protobuf.BoolValue;
import com.google.protobuf.BytesValue;
import com.google.protobuf.Empty;
import com.google.protobuf.Int32Value;

import com.oracle.coherence.grpc.AddIndexRequest;
import com.oracle.coherence.grpc.AggregateRequest;
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
import com.oracle.coherence.grpc.NamedCacheServiceGrpc;
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

import io.grpc.stub.StreamObserver;

import java.util.concurrent.CompletionStage;

/**
 * A plain gRPC implementation of NamedCache service.
 *
 * @author Jonathan Knight  2020.09.21
 */
public class NamedCacheServiceGrpcImpl
        extends NamedCacheServiceGrpc.NamedCacheServiceImplBase
        implements BindableGrpcProxyService
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a {@link NamedCacheServiceGrpcImpl} that wraps a default
     * implementation of {@link NamedCacheService}.
     */
    public NamedCacheServiceGrpcImpl()
        {
        this(NamedCacheServiceImpl.newInstance());
        }

    /**
     * Create a {@link NamedCacheServiceGrpcImpl} with default configuration.
     *
     * @param service the {@link NamedCacheService} to use
     */
    public NamedCacheServiceGrpcImpl(NamedCacheService service)
        {
        m_service = service;
        }

    // ----- BindableGrpcProxyService methods -------------------------------

    @Override
    public GrpcProxyMetrics getMetrics()
        {
        return m_service.getMetrics();
        }

    // ----- NamedCacheServiceGrpc.NamedCacheServiceImplBase methods --------

    @Override
    public void addIndex(AddIndexRequest request, StreamObserver<Empty> observer)
        {
        m_service.addIndex(request)
                .handleAsync((result, err) -> handleUnary(result, err, SafeStreamObserver.ensureSafeObserver(observer)));
        }

    @Override
    public void aggregate(AggregateRequest request, StreamObserver<BytesValue> observer)
        {
        m_service.aggregate(request)
                .handleAsync((result, err) -> handleUnary(result, err, SafeStreamObserver.ensureSafeObserver(observer)));
        }

    @Override
    public void clear(ClearRequest request, StreamObserver<Empty> observer)
        {
        CompletionStage<Empty> stage = m_service.clear(request);
        stage
                .handle((result, err) -> handleUnary(result, err, SafeStreamObserver.ensureSafeObserver(observer)));
        }

    @Override
    public void containsEntry(ContainsEntryRequest request, StreamObserver<BoolValue> observer)
        {
        m_service.containsEntry(request)
                .handleAsync((result, err) -> handleUnary(result, err, SafeStreamObserver.ensureSafeObserver(observer)));
        }

    @Override
    public void containsKey(ContainsKeyRequest request, StreamObserver<BoolValue> observer)
        {
        m_service.containsKey(request)
                .handleAsync((result, err) -> handleUnary(result, err, SafeStreamObserver.ensureSafeObserver(observer)));
        }

    @Override
    public void containsValue(ContainsValueRequest request, StreamObserver<BoolValue> observer)
        {
        m_service.containsValue(request)
                .handleAsync((result, err) -> handleUnary(result, err, SafeStreamObserver.ensureSafeObserver(observer)));
        }

    @Override
    public void destroy(DestroyRequest request, StreamObserver<Empty> observer)
        {
        m_service.destroy(request)
                .handleAsync((result, err) -> handleUnary(result, err, SafeStreamObserver.ensureSafeObserver(observer)));
        }

    @Override
    public void entrySet(EntrySetRequest request, StreamObserver<Entry> observer)
        {
        m_service.entrySet(request, SafeStreamObserver.ensureSafeObserver(observer));
        }

    @Override
    public StreamObserver<MapListenerRequest> events(StreamObserver<MapListenerResponse> observer)
        {
        return m_service.events(SafeStreamObserver.ensureSafeObserver(observer));
        }

    @Override
    public void get(GetRequest request, StreamObserver<OptionalValue> observer)
        {
        m_service.get(request)
                .handleAsync((result, err) -> handleUnary(result, err, SafeStreamObserver.ensureSafeObserver(observer)));
        }

    @Override
    public void getAll(GetAllRequest request, StreamObserver<Entry> observer)
        {
        m_service.getAll(request, SafeStreamObserver.ensureSafeObserver(observer));
        }

    @Override
    public void invoke(InvokeRequest request, StreamObserver<BytesValue> observer)
        {
        m_service.invoke(request)
                .handleAsync((result, err) -> handleUnary(result, err, SafeStreamObserver.ensureSafeObserver(observer)));
        }

    @Override
    public void invokeAll(InvokeAllRequest request, StreamObserver<Entry> observer)
        {
        m_service.invokeAll(request, SafeStreamObserver.ensureSafeObserver(observer));
        }

    @Override
    public void isEmpty(IsEmptyRequest request, StreamObserver<BoolValue> observer)
        {
        m_service.isEmpty(request)
                .handleAsync((result, err) -> handleUnary(result, err, SafeStreamObserver.ensureSafeObserver(observer)));
        }

    @Override
    public void keySet(KeySetRequest request, StreamObserver<BytesValue> observer)
        {
        m_service.keySet(request, SafeStreamObserver.ensureSafeObserver(observer));
        }

    @Override
    public void nextEntrySetPage(PageRequest request, StreamObserver<EntryResult> observer)
        {
        m_service.nextEntrySetPage(request, SafeStreamObserver.ensureSafeObserver(observer));
        }

    @Override
    public void nextKeySetPage(PageRequest request, StreamObserver<BytesValue> observer)
        {
        m_service.nextKeySetPage(request, SafeStreamObserver.ensureSafeObserver(observer));
        }

    @Override
    public void put(PutRequest request, StreamObserver<BytesValue> observer)
        {
        m_service.put(request)
                .handleAsync((result, err) -> handleUnary(result, err, SafeStreamObserver.ensureSafeObserver(observer)));
        }

    @Override
    public void putAll(PutAllRequest request, StreamObserver<Empty> observer)
        {
        m_service.putAll(request)
                .handleAsync((result, err) -> handleUnary(result, err, SafeStreamObserver.ensureSafeObserver(observer)));
        }

    @Override
    public void putIfAbsent(PutIfAbsentRequest request, StreamObserver<BytesValue> observer)
        {
        m_service.putIfAbsent(request)
                .handleAsync((result, err) -> handleUnary(result, err, SafeStreamObserver.ensureSafeObserver(observer)));
        }

    @Override
    public void remove(RemoveRequest request, StreamObserver<BytesValue> observer)
        {
        m_service.remove(request)
                .handleAsync((result, err) -> handleUnary(result, err, SafeStreamObserver.ensureSafeObserver(observer)));
        }

    @Override
    public void removeIndex(RemoveIndexRequest request, StreamObserver<Empty> observer)
        {
        m_service.removeIndex(request)
                .handleAsync((result, err) -> handleUnary(result, err, SafeStreamObserver.ensureSafeObserver(observer)));
        }

    @Override
    public void removeMapping(RemoveMappingRequest request, StreamObserver<BoolValue> observer)
        {
        m_service.removeMapping(request)
                .handleAsync((result, err) -> handleUnary(result, err, SafeStreamObserver.ensureSafeObserver(observer)));
        }

    @Override
    public void replace(ReplaceRequest request, StreamObserver<BytesValue> observer)
        {
        m_service.replace(request)
                .handleAsync((result, err) -> handleUnary(result, err, SafeStreamObserver.ensureSafeObserver(observer)));
        }

    @Override
    public void replaceMapping(ReplaceMappingRequest request, StreamObserver<BoolValue> observer)
        {
        m_service.replaceMapping(request)
                .handleAsync((result, err) -> handleUnary(result, err, SafeStreamObserver.ensureSafeObserver(observer)));
        }

    @Override
    public void size(SizeRequest request, StreamObserver<Int32Value> observer)
        {
        m_service.size(request)
                .handleAsync((result, err) -> handleUnary(result, err, SafeStreamObserver.ensureSafeObserver(observer)));
        }

    @Override
    public void truncate(TruncateRequest request, StreamObserver<Empty> observer)
        {
        m_service.truncate(request)
                .handleAsync((result, err) -> handleUnary(result, err, SafeStreamObserver.ensureSafeObserver(observer)));
        }

    @Override
    public void values(ValuesRequest request, StreamObserver<BytesValue> observer)
        {
        m_service.values(request, SafeStreamObserver.ensureSafeObserver(observer));
        }

    // ----- helper methods -------------------------------------------------

    <R> Void handleUnary(R result, Throwable err, StreamObserver<R> observer)
        {
        if (err != null)
            {
            observer.onError(err);
            }
        else 
            {
            observer.onNext(result);
            observer.onCompleted();
            }
        return null;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The {@link NamedCacheService} to call.
     */
    private final NamedCacheService m_service;
    }
