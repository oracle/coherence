/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.ai.grpc;

import com.google.protobuf.Empty;

import com.oracle.coherence.grpc.proxy.common.BaseGrpcServiceImpl;

import com.oracle.coherence.grpc.proxy.common.GrpcProxyService;
import com.oracle.coherence.grpc.proxy.common.GrpcServiceDependencies;
import com.tangosol.net.grpc.GrpcDependencies;

import io.grpc.stub.StreamObserver;

/**
 * A {@link GrpcProxyService} that represents a gRPC vector store service.
 */
public interface VectorStoreService
        extends GrpcProxyService
    {
    /**
     * Clear a store.
     *
     * @param request   the {@link ClearRequest} to identify the store to clear
     * @param observer  the {@link StreamObserver} to receive responses
     */
    void clear(ClearRequest request, StreamObserver<Empty> observer);

    /**
     * Destroy a store.
     *
     * @param request   the {@link ClearRequest} to identify the store to clear
     * @param observer  the {@link StreamObserver} to receive responses
     */
    void destroy(DestroyRequest request, StreamObserver<Empty> observer);

    /**
     * Add vectors to a store.
     *
     * @param request   the {@link ClearRequest} to identify the store to clear
     * @param observer  the {@link StreamObserver} to receive responses
     */
    void add(AddRequest request, StreamObserver<Empty> observer);

    /**
     * Stream vectors to a store.
     * <p/>
     * The first {@link UploadRequest} must be a {@link UploadStart} message,
     * which is then followed by {@link Vectors} messages.
     *
     * @param observer  the {@link StreamObserver} to receive responses
     *
     * @return a {@link StreamObserver} to send {@link UploadRequest} messages to
     */
    StreamObserver<UploadRequest> upload(StreamObserver<Empty> observer);

    /**
     * Get a vector from the store.
     *
     * @param request   the {@link GetVectorRequest} to identify the store and vector key
     * @param observer  the {@link StreamObserver} to receive the response
     */
    void get(GetVectorRequest request, StreamObserver<OptionalVector> observer);

    /**
     * Perform a query on the store.
     *
     * @param request   the {@link SimilarityQuery} to execute
     * @param observer  the {@link StreamObserver} to receive the query results
     */
    void query(SimilarityQuery request, StreamObserver<QueryResult> observer);

    // ----- inner interface: Dependencies ----------------------------------

    /**
     * The dependencies to configure a {@link VectorStoreService}.
     */
    interface Dependencies
            extends BaseGrpcServiceImpl.Dependencies
        {
        }

    // ----- inner class: DefaultDependencies -------------------------------

    /**
     * The default {@link Dependencies} implementation.
     */
    class DefaultDependencies
            extends BaseGrpcServiceImpl.DefaultDependencies
            implements Dependencies
        {
        /**
         * Create a {@link DefaultDependencies}.
         *
         * @param serverType  the type of the gRPC server
         */
        public DefaultDependencies(GrpcDependencies.ServerType serverType)
            {
            super(serverType);
            }

        /**
         * Create a {@link DefaultDependencies}.
         *
         * @param deps  the {@link GrpcServiceDependencies} to use
         */
        public DefaultDependencies(GrpcServiceDependencies deps)
            {
            super(deps);
            }

        /**
         * Create a {@link DefaultDependencies}.
         *
         * @param deps  the {@link VectorStoreService.Dependencies} to copy
         */
        public DefaultDependencies(VectorStoreService.Dependencies deps)
            {
            super(deps);
            }
        }
    }
