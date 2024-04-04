/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.ai.grpc;

import com.google.protobuf.Empty;

import com.oracle.coherence.grpc.proxy.common.BindableGrpcProxyService;
import com.oracle.coherence.grpc.proxy.common.GrpcProxyMetrics;
import com.oracle.coherence.grpc.proxy.common.GrpcProxyServiceMetrics;

import io.grpc.stub.StreamObserver;

/**
 * The gRPC vector store service implementation.
 */
public class VectorStoreServiceGrpcImpl
        extends VectorStoreServiceGrpc.VectorStoreServiceImplBase
        implements BindableGrpcProxyService
    {
    /**
     * Create a gRPC vector service.
     *
     * @param service  the {@link VectorStoreService} to delegate to.
     */
    public VectorStoreServiceGrpcImpl(VectorStoreService service)
        {
        m_service = service;
        }

    @Override
    public GrpcProxyMetrics getMetrics()
        {
        return m_service.getMetrics();
        }

    @Override
    public void clear(ClearRequest request, StreamObserver<Empty> observer)
        {
        m_service.clear(request, observer);
        }

    @Override
    public void destroy(DestroyRequest request, StreamObserver<Empty> observer)
        {
        m_service.destroy(request, observer);
        }

    @Override
    public void add(AddRequest request, StreamObserver<Empty> observer)
        {
        m_service.add(request, observer);
        }

    @Override
    public StreamObserver<UploadRequest> upload(StreamObserver<Empty> observer)
        {
        return m_service.upload(observer);
        }

    @Override
    public void query(SimilarityQuery request, StreamObserver<QueryResult> observer)
        {
        m_service.query(request, observer);
        }

    // ----- data members ---------------------------------------------------

    /**
     * The {@link VectorStoreService} to delegate to.
     */
    private final VectorStoreService m_service;
    }
