/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.proxy.common.v0;

import com.google.protobuf.BoolValue;
import com.google.protobuf.BytesValue;
import com.google.protobuf.Empty;
import com.google.protobuf.Int32Value;

import com.oracle.coherence.grpc.SafeStreamObserver;

import com.oracle.coherence.grpc.messages.cache.v0.AddIndexRequest;
import com.oracle.coherence.grpc.messages.cache.v0.AggregateRequest;
import com.oracle.coherence.grpc.messages.cache.v0.ClearRequest;
import com.oracle.coherence.grpc.messages.cache.v0.ContainsEntryRequest;
import com.oracle.coherence.grpc.messages.cache.v0.ContainsKeyRequest;
import com.oracle.coherence.grpc.messages.cache.v0.ContainsValueRequest;
import com.oracle.coherence.grpc.messages.cache.v0.DestroyRequest;
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
import com.oracle.coherence.grpc.messages.cache.v0.MapListenerRequest;
import com.oracle.coherence.grpc.messages.cache.v0.MapListenerResponse;
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

import com.oracle.coherence.grpc.proxy.common.BindableGrpcProxyService;
import com.oracle.coherence.grpc.proxy.common.GrpcProxyMetrics;
import com.oracle.coherence.grpc.services.cache.v0.NamedCacheServiceGrpc;

import io.grpc.stub.StreamObserver;

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
        m_service.addIndex(request, SafeStreamObserver.ensureSafeObserver(observer));
        }

    @Override
    public void aggregate(AggregateRequest request, StreamObserver<BytesValue> observer)
        {
        m_service.aggregate(request, SafeStreamObserver.ensureSafeObserver(observer));
        }

    @Override
    public void clear(ClearRequest request, StreamObserver<Empty> observer)
        {
        m_service.clear(request, SafeStreamObserver.ensureSafeObserver(observer));
        }

    @Override
    public void containsEntry(ContainsEntryRequest request, StreamObserver<BoolValue> observer)
        {
        m_service.containsEntry(request, SafeStreamObserver.ensureSafeObserver(observer));
        }

    @Override
    public void containsKey(ContainsKeyRequest request, StreamObserver<BoolValue> observer)
        {
        m_service.containsKey(request, SafeStreamObserver.ensureSafeObserver(observer));
        }

    @Override
    public void containsValue(ContainsValueRequest request, StreamObserver<BoolValue> observer)
        {
        m_service.containsValue(request, SafeStreamObserver.ensureSafeObserver(observer));
        }

    @Override
    public void destroy(DestroyRequest request, StreamObserver<Empty> observer)
        {
        m_service.destroy(request, SafeStreamObserver.ensureSafeObserver(observer));
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
        m_service.get(request, SafeStreamObserver.ensureSafeObserver(observer));
        }

    @Override
    public void getAll(GetAllRequest request, StreamObserver<Entry> observer)
        {
        m_service.getAll(request, SafeStreamObserver.ensureSafeObserver(observer));
        }

    @Override
    public void invoke(InvokeRequest request, StreamObserver<BytesValue> observer)
        {
        m_service.invoke(request, SafeStreamObserver.ensureSafeObserver(observer));
        }

    @Override
    public void invokeAll(InvokeAllRequest request, StreamObserver<Entry> observer)
        {
        m_service.invokeAll(request, SafeStreamObserver.ensureSafeObserver(observer));
        }

    @Override
    public void isEmpty(IsEmptyRequest request, StreamObserver<BoolValue> observer)
        {
        m_service.isEmpty(request, SafeStreamObserver.ensureSafeObserver(observer));
        }

    @Override
    public void isReady(IsReadyRequest request, StreamObserver<BoolValue> observer)
        {
        m_service.isReady(request, SafeStreamObserver.ensureSafeObserver(observer));
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
        m_service.put(request, SafeStreamObserver.ensureSafeObserver(observer));
        }

    @Override
    public void putAll(PutAllRequest request, StreamObserver<Empty> observer)
        {
        m_service.putAll(request, SafeStreamObserver.ensureSafeObserver(observer));
        }

    @Override
    public void putIfAbsent(PutIfAbsentRequest request, StreamObserver<BytesValue> observer)
        {
        m_service.putIfAbsent(request, SafeStreamObserver.ensureSafeObserver(observer));
        }

    @Override
    public void remove(RemoveRequest request, StreamObserver<BytesValue> observer)
        {
        m_service.remove(request, SafeStreamObserver.ensureSafeObserver(observer));
        }

    @Override
    public void removeIndex(RemoveIndexRequest request, StreamObserver<Empty> observer)
        {
        m_service.removeIndex(request, SafeStreamObserver.ensureSafeObserver(observer));
        }

    @Override
    public void removeMapping(RemoveMappingRequest request, StreamObserver<BoolValue> observer)
        {
        m_service.removeMapping(request, SafeStreamObserver.ensureSafeObserver(observer));
        }

    @Override
    public void replace(ReplaceRequest request, StreamObserver<BytesValue> observer)
        {
        m_service.replace(request, SafeStreamObserver.ensureSafeObserver(observer));
        }

    @Override
    public void replaceMapping(ReplaceMappingRequest request, StreamObserver<BoolValue> observer)
        {
        m_service.replaceMapping(request, SafeStreamObserver.ensureSafeObserver(observer));
        }

    @Override
    public void size(SizeRequest request, StreamObserver<Int32Value> observer)
        {
        m_service.size(request, SafeStreamObserver.ensureSafeObserver(observer));
        }

    @Override
    public void truncate(TruncateRequest request, StreamObserver<Empty> observer)
        {
        m_service.truncate(request, SafeStreamObserver.ensureSafeObserver(observer));
        }

    @Override
    public void values(ValuesRequest request, StreamObserver<BytesValue> observer)
        {
        m_service.values(request, SafeStreamObserver.ensureSafeObserver(observer));
        }

    // ----- data members ---------------------------------------------------

    /**
     * The {@link NamedCacheService} to call.
     */
    private final NamedCacheService m_service;
    }
