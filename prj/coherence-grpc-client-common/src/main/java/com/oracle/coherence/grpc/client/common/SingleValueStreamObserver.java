/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.client.common;

import io.grpc.stub.StreamObserver;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * A simple {@link io.grpc.stub.StreamObserver} adapter class that completes a {@link CompletableFuture} when the
 * observer is completed.
 * <p>
 * This observer uses the value passed to its {@link #onNext(Object)} method to complete the {@link CompletableFuture}.
 * <p>
 * This observer should only be used in cases where a single result is expected. If more that one call is made to {@link
 * #onNext(Object)} then future will be completed with an exception.
 *
 * @param <T> The type of objects received in this stream.
 *
 * @author Jonathan Knight  2020.09.21
 * @since 20.06
 */
public class SingleValueStreamObserver<T>
        implements StreamObserver<T>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a SingleValueStreamObserver.
     */
    public SingleValueStreamObserver()
        {
        }

    // ----- StreamObserver methods -----------------------------------------

    @Override
    public void onNext(T value)
        {
        if (m_cNext++ == 0)
            {
            m_oResult = value;
            }
        else
            {
            f_future.completeExceptionally(new IllegalStateException("More than one result received."));
            }
        }

    @Override
    public void onError(Throwable t)
        {
        f_future.completeExceptionally(t);
        }

    @Override
    public void onCompleted()
        {
        f_future.complete(m_oResult);
        }

    // ----- accessors ------------------------------------------------------

    /**
     * Obtain the {@link CompletableFuture} that will be completed when
     * the {@link io.grpc.stub.StreamObserver} completes.
     *
     * @return The CompletableFuture
     */
    public CompletionStage<T> completionStage()
        {
        return f_future;
        }

    // ----- data members ---------------------------------------------------

    private int m_cNext;

    private T m_oResult;

    private final CompletableFuture<T> f_future = new CompletableFuture<>();
    }
