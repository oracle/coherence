/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.proxy.common.v0;

import com.google.protobuf.BoolValue;
import com.google.protobuf.BytesValue;
import com.google.protobuf.Empty;
import com.google.protobuf.Int32Value;

import com.oracle.coherence.grpc.GrpcService;
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
        this(service, null);
        }

    /**
     * Create a {@link NamedCacheServiceGrpcImpl}.
     *
     * @param service       the {@link NamedCacheService} to use
     * @param dependencies  the optional service dependencies
     */
    public NamedCacheServiceGrpcImpl(NamedCacheService service, GrpcService.Dependencies dependencies)
        {
        m_service = service;
        m_sErrorDisclosure = dependencies == null ? null : dependencies.getErrorDisclosure();
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
        m_service.addIndex(request, safe(observer));
        }

    @Override
    public void aggregate(AggregateRequest request, StreamObserver<BytesValue> observer)
        {
        m_service.aggregate(request, safe(observer));
        }

    @Override
    public void clear(ClearRequest request, StreamObserver<Empty> observer)
        {
        m_service.clear(request, safe(observer));
        }

    @Override
    public void containsEntry(ContainsEntryRequest request, StreamObserver<BoolValue> observer)
        {
        m_service.containsEntry(request, safe(observer));
        }

    @Override
    public void containsKey(ContainsKeyRequest request, StreamObserver<BoolValue> observer)
        {
        m_service.containsKey(request, safe(observer));
        }

    @Override
    public void containsValue(ContainsValueRequest request, StreamObserver<BoolValue> observer)
        {
        m_service.containsValue(request, safe(observer));
        }

    @Override
    public void destroy(DestroyRequest request, StreamObserver<Empty> observer)
        {
        m_service.destroy(request, safe(observer));
        }

    @Override
    public void entrySet(EntrySetRequest request, StreamObserver<Entry> observer)
        {
        m_service.entrySet(request, safe(observer));
        }

    @Override
    public StreamObserver<MapListenerRequest> events(StreamObserver<MapListenerResponse> observer)
        {
        return m_service.events(safe(observer));
        }

    @Override
    public void get(GetRequest request, StreamObserver<OptionalValue> observer)
        {
        m_service.get(request, safe(observer));
        }

    @Override
    public void getAll(GetAllRequest request, StreamObserver<Entry> observer)
        {
        m_service.getAll(request, safe(observer));
        }

    @Override
    public void invoke(InvokeRequest request, StreamObserver<BytesValue> observer)
        {
        m_service.invoke(request, safe(observer));
        }

    @Override
    public void invokeAll(InvokeAllRequest request, StreamObserver<Entry> observer)
        {
        m_service.invokeAll(request, safe(observer));
        }

    @Override
    public void isEmpty(IsEmptyRequest request, StreamObserver<BoolValue> observer)
        {
        m_service.isEmpty(request, safe(observer));
        }

    @Override
    public void isReady(IsReadyRequest request, StreamObserver<BoolValue> observer)
        {
        m_service.isReady(request, safe(observer));
        }

        @Override
    public void keySet(KeySetRequest request, StreamObserver<BytesValue> observer)
        {
        m_service.keySet(request, safe(observer));
        }

    @Override
    public void nextEntrySetPage(PageRequest request, StreamObserver<EntryResult> observer)
        {
        m_service.nextEntrySetPage(request, safe(observer));
        }

    @Override
    public void nextKeySetPage(PageRequest request, StreamObserver<BytesValue> observer)
        {
        m_service.nextKeySetPage(request, safe(observer));
        }

    @Override
    public void put(PutRequest request, StreamObserver<BytesValue> observer)
        {
        m_service.put(request, safe(observer));
        }

    @Override
    public void putAll(PutAllRequest request, StreamObserver<Empty> observer)
        {
        m_service.putAll(request, safe(observer));
        }

    @Override
    public void putIfAbsent(PutIfAbsentRequest request, StreamObserver<BytesValue> observer)
        {
        m_service.putIfAbsent(request, safe(observer));
        }

    @Override
    public void remove(RemoveRequest request, StreamObserver<BytesValue> observer)
        {
        m_service.remove(request, safe(observer));
        }

    @Override
    public void removeIndex(RemoveIndexRequest request, StreamObserver<Empty> observer)
        {
        m_service.removeIndex(request, safe(observer));
        }

    @Override
    public void removeMapping(RemoveMappingRequest request, StreamObserver<BoolValue> observer)
        {
        m_service.removeMapping(request, safe(observer));
        }

    @Override
    public void replace(ReplaceRequest request, StreamObserver<BytesValue> observer)
        {
        m_service.replace(request, safe(observer));
        }

    @Override
    public void replaceMapping(ReplaceMappingRequest request, StreamObserver<BoolValue> observer)
        {
        m_service.replaceMapping(request, safe(observer));
        }

    @Override
    public void size(SizeRequest request, StreamObserver<Int32Value> observer)
        {
        m_service.size(request, safe(observer));
        }

    @Override
    public void truncate(TruncateRequest request, StreamObserver<Empty> observer)
        {
        m_service.truncate(request, safe(observer));
        }

    @Override
    public void values(ValuesRequest request, StreamObserver<BytesValue> observer)
        {
        m_service.values(request, safe(observer));
        }

    @Override
    public void close()
        {
        m_service.close();
        }

    // ----- helper methods -------------------------------------------------

    private <T> StreamObserver<T> safe(StreamObserver<T> observer)
        {
        return m_sErrorDisclosure == null
                ? SafeStreamObserver.ensureSafeObserver(observer)
                : SafeStreamObserver.ensureSafeObserver(observer, m_sErrorDisclosure);
        }

    // ----- data members ---------------------------------------------------

    /**
     * The {@link NamedCacheService} to call.
     */
    private final NamedCacheService m_service;

    /**
     * The gRPC error-disclosure policy, or {@code null} for the default.
     */
    private final String m_sErrorDisclosure;
    }
