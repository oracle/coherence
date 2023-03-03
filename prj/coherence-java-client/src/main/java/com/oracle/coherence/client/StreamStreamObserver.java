/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.client;

import io.grpc.stub.StreamObserver;

import java.util.ArrayList;
import java.util.List;

import java.util.concurrent.CompletableFuture;

/**
 * A {@link StreamObserver} that collects all of its responses into a {@link List} and then completes a {@link
 * CompletableFuture} when the observer completes.
 *
 * @param <T> the response type
 *
 * @author Jonathan Knight  2020.09.21
 * @since 20.06
 */
public class StreamStreamObserver<T>
        implements StreamObserver<T>
    {

    // ----- accessors ------------------------------------------------------

    public CompletableFuture<List<T>> future()
        {
        return f_future;
        }

    // ----- StreamStreamObserver methods -----------------------------------

    @Override
    public void onNext(T value)
        {
        list.add(value);
        }

    @Override
    public void onError(Throwable t)
        {
        f_future.completeExceptionally(t);
        }

    @Override
    public void onCompleted()
        {
        f_future.complete(list);
        }

    // ----- data members ---------------------------------------------------

    private final CompletableFuture<List<T>> f_future = new CompletableFuture<>();

    private final List<T> list = new ArrayList<>();
    }
