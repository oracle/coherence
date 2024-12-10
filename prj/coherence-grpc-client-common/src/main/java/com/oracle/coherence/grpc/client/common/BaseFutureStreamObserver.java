/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.client.common;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;

import io.grpc.stub.StreamObserver;

import java.util.concurrent.CompletableFuture;

/**
 * An abstract {@link StreamObserver} that, on error, will redefine a
 * {@link StatusRuntimeException} with a status of {@link Status#UNIMPLEMENTED}
 * as an {@code UnsupportedOperationException}.
 *
 * @param <T> the response type
 *
 * @since 14.1.1.2206.5
 */
public abstract class BaseFutureStreamObserver<T>
        implements StreamObserver<T>
    {
    // ----- ErrorHandlingObserver methods ----------------------------------

        /**
         * Return the {@link CompletableFuture} associated with this
         * {@code StreamObserver}.
         *
         * @return the {@link CompletableFuture} associated with this
         *         {@code StreamObserver}
         */
    @SuppressWarnings("rawtypes")
    public abstract CompletableFuture future();

    // ----- interface: StreamObserver --------------------------------------

    /**
     * If {@code throwable} is a {@link StatusRuntimeException} with a
     * status of {@link Status#UNIMPLEMENTED}, complete the future
     * exceptionally with an {@link UnsupportedOperationException}.
     *
     * @param throwable the stream error
     */
    @SuppressWarnings("rawtypes")
    @Override
    public void onError(Throwable throwable)
        {
        CompletableFuture f = future();
        if (!f.isDone())
            {
            if (throwable instanceof StatusRuntimeException)
                {
                StatusRuntimeException sre = (StatusRuntimeException) throwable;
                if (sre.getStatus().getCode() == Status.Code.UNIMPLEMENTED.toStatus().getCode())
                    {
                    f.completeExceptionally(
                            new UnsupportedOperationException("This operation"
                                    + " is not supported by the current gRPC proxy. "
                                    + "Either upgrade the version of Coherence on the "
                                    + "gRPC proxy or connect to a gRPC proxy "
                                    + "that supports the operation."));
                    return;
                    }
                }
            f.completeExceptionally(throwable);
            }
        }
    }
