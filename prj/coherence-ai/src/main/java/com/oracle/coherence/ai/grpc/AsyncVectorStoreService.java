/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.ai.grpc;

import com.google.protobuf.Empty;

import com.oracle.coherence.grpc.ErrorsHelper;
import com.oracle.coherence.grpc.SafeStreamObserver;

import com.oracle.coherence.grpc.proxy.common.v0.ResponseHandlers;

import io.grpc.stub.StreamObserver;

import java.util.concurrent.CompletableFuture;

/**
 * An async vector store service that uses a daemon pool to hand off work
 * so as not to block the gRPC request thread.
 */
public class AsyncVectorStoreService
        extends SyncVectorStoreService
    {
    /**
     * Create an {@link AsyncVectorStoreService}.
     *
     * @param deps  the {@link VectorStoreService.Dependencies dependencies} for the service
     */
    public AsyncVectorStoreService(VectorStoreService.Dependencies deps)
        {
        super(deps);
        }

    @Override
    public void clear(ClearRequest request, StreamObserver<Empty> observer)
        {
        StreamObserver<Empty> safeObserver = SafeStreamObserver.ensureSafeObserver(observer);
        try
            {
            CompletableFuture.runAsync(() -> super.clear(request, safeObserver), f_executor)
                    .handle((_void, error) -> ResponseHandlers.handleError(error, safeObserver));
            }
        catch (Throwable error)
            {
            safeObserver.onError(ErrorsHelper.ensureStatusRuntimeException(error));
            }
        }

    @Override
    public void destroy(DestroyRequest request, StreamObserver<Empty> observer)
        {
        StreamObserver<Empty> safeObserver = SafeStreamObserver.ensureSafeObserver(observer);
        try
            {
            CompletableFuture.runAsync(() -> super.destroy(request, safeObserver), f_executor)
                    .handle((_void, error) -> ResponseHandlers.handleError(error, safeObserver));
            }
        catch (Throwable error)
            {
            safeObserver.onError(ErrorsHelper.ensureStatusRuntimeException(error));
            }
        }

    @Override
    public void add(AddRequest request, StreamObserver<Empty> observer)
        {
        StreamObserver<Empty> safeObserver = SafeStreamObserver.ensureSafeObserver(observer);
        try
            {
            CompletableFuture.runAsync(() -> super.add(request, safeObserver), f_executor)
                    .handle((_void, error) -> ResponseHandlers.handleError(error, safeObserver));
            }
        catch (Throwable error)
            {
            safeObserver.onError(ErrorsHelper.ensureStatusRuntimeException(error));
            }
        }

    @Override
    public void get(GetVectorRequest request, StreamObserver<OptionalVector> observer)
        {
        StreamObserver<OptionalVector> safeObserver = SafeStreamObserver.ensureSafeObserver(observer);
        try
            {
            CompletableFuture.runAsync(() -> super.get(request, safeObserver), f_executor)
                    .handle((_void, error) -> ResponseHandlers.handleError(error, safeObserver));
            }
        catch (Throwable error)
            {
            safeObserver.onError(ErrorsHelper.ensureStatusRuntimeException(error));
            }
        }

    @Override
    public void query(SimilarityQuery request, StreamObserver<QueryResult> observer)
        {
        StreamObserver<QueryResult> safeObserver = SafeStreamObserver.ensureSafeObserver(observer);
        try
            {
            CompletableFuture.runAsync(() -> super.query(request, safeObserver), f_executor)
                    .handle((_void, error) -> ResponseHandlers.handleError(error, safeObserver));
            }
        catch (Throwable error)
            {
            safeObserver.onError(ErrorsHelper.ensureStatusRuntimeException(error));
            }
        }
    }
