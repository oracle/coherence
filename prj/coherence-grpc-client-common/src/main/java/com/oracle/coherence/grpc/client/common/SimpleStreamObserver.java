/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.client.common;

import com.oracle.coherence.common.base.Logger;

import io.grpc.stub.StreamObserver;

import java.util.Objects;

import java.util.function.Consumer;

/**
 * A simple {@link StreamObserver} that uses a {@link Consumer}
 * to handle the calls to {@link #onNext(Object)}
 *
 * @author Jonathan Knight  2025.01.25
 */
public class SimpleStreamObserver<Resp>
        implements StreamObserver<Resp>
    {
    /**
     * Create a {@link SimpleStreamObserver}.
     *
     * @param handler  the {@link Consumer} to handle calls to {@link #onNext(Object)}
     */
    public SimpleStreamObserver(Consumer<Resp> handler)
        {
        f_handler = Objects.requireNonNull(handler);
        }

    @Override
    public void onNext(Resp resp)
        {
        try
            {
            f_handler.accept(resp);
            }
        catch (Exception e)
            {
            Logger.err(e);
            }
        }

    @Override
    public void onError(Throwable throwable)
        {
        Logger.err(throwable);
        }

    @Override
    public void onCompleted()
        {
        }

    // ----- data members ---------------------------------------------------

    /**
     * The {@link Consumer} to handle calls to {@link #onNext(Object)}.
     */
    private final Consumer<Resp> f_handler;
    }
