/*
 * Copyright (c) 2020, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.client;

import io.grpc.stub.StreamObserver;

import java.util.concurrent.CompletableFuture;

import java.util.function.BiFunction;

/**
 * A {@link StreamObserver} that completes a {@link CompletableFuture}.
 *
 * @param <T>  the type of element observed
 * @param <R>  the type of the {@link CompletableFuture}
 *
 * @author Jonathan Knight  2020.09.22
 * @since 20.06
 */
class FutureStreamObserver<T, R>
        extends BaseFutureStreamObserver<T>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a new {@code FutureStreamObserver}.
     *
     * @param future         the {@link CompletableFuture} to notify
     * @param initialResult  the initial result
     * @param function       the function to call for each entry within the stream
     */
    FutureStreamObserver(CompletableFuture<R> future, R initialResult, BiFunction<T, R, R> function)
        {
        f_future   = future;
        m_result   = initialResult;
        f_function = function;
        }

    // ----- accessors ------------------------------------------------------

    @Override
    public CompletableFuture<R> future()
        {
        return f_future;
        }

    // ----- StreamObserver interface ---------------------------------------

    @Override
    public void onNext(T t)
        {
        if (!f_future.isDone())
            {
            m_result = f_function.apply(t, m_result);
            }
        }

    @Override
    public void onCompleted()
        {
        if (!f_future.isDone())
            {
            f_future.complete(m_result);
            }
        }

    // ----- data members ---------------------------------------------------

    /**
     * The {@link CompletableFuture} to notify.
     */
    protected final CompletableFuture<R> f_future;

    /**
     * Callback function for entry processing.
     */
    protected final BiFunction<T, R, R> f_function;

    /**
     * The result after the callback is applied.
     */
    protected R m_result;
    }
