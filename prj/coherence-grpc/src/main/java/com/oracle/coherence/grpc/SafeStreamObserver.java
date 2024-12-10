/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc;

import io.grpc.Status;

import io.grpc.stub.StreamObserver;

import java.net.SocketException;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A {@link io.grpc.stub.StreamObserver} that handles exceptions correctly.
 *
 * @param <T> the type of response expected
 * @author Jonathan Knight  2020.09.22
 */
public class SafeStreamObserver<T>
        implements StreamObserver<T>
    {
    /**
     * Create a {@link SafeStreamObserver} that wraps another {@link io.grpc.stub.StreamObserver}.
     *
     * @param streamObserver the {@link io.grpc.stub.StreamObserver} to wrap
     */
    private SafeStreamObserver(StreamObserver<? super T> streamObserver)
        {
        delegate = streamObserver;
        }

    @Override
    public void onNext(T t)
        {
        if (done)
            {
            return;
            }

        if (t == null)
            {
            onError(Status.INVALID_ARGUMENT
                    .withDescription("onNext called with null. Null values are generally not allowed.")
                    .asRuntimeException());
            }
        else
            {
            try
                {
                delegate.onNext(t);
                }
            catch (Throwable thrown)
                {
                throwIfFatal(thrown);
                onError(thrown);
                }
            }
        }

    @Override
    public void onError(Throwable thrown)
        {
        try
            {
            if (done)
                {
                LOGGER.log(Level.SEVERE, checkNotNull(thrown), () -> "OnError called after StreamObserver was closed");
                }
            else
                {
                setDone(thrown);
                delegate.onError(ErrorsHelper.ensureStatusRuntimeException(checkNotNull(thrown)));
                }
            }
        catch (Throwable t)
            {
            throwIfFatal(t);
            if (!isSocketClosedError(thrown))
                {
                LOGGER.log(Level.SEVERE, t, () -> "Caught exception handling onError");
                }
            }
        }

    @Override
    public void onCompleted()
        {
        if (done)
            {
            LOGGER.log(Level.WARNING, "onComplete called after StreamObserver was closed");
            }
        else
            {
            try
                {
                setDone(null);
                delegate.onCompleted();
                }
            catch (Throwable thrown)
                {
                throwIfFatal(thrown);
                if (!isSocketClosedError(thrown))
                    {
                    LOGGER.log(Level.SEVERE, thrown, () -> "Caught exception handling onComplete");
                    }
                }
            }
        }

    /**
     * Obtain the wrapped {@link StreamObserver}.
     *
     * @return the wrapped {@link StreamObserver}
     */
    public StreamObserver<? super T> delegate()
        {
        return delegate;
        }

    /**
     * Returns {@code true} if this observer is complete.
     *
     * @return {@code true} if this observer is complete
     */
    public boolean isDone()
        {
        return done;
        }


    public CompletableFuture<Void> whenDone()
        {
        CompletableFuture<Void> future = new CompletableFuture<>();
        doneFuture.whenComplete((v, err) ->
            {
            if (err == null)
                {
                future.complete(null);
                }
            else
                {
                future.completeExceptionally(err);
                }
            });
        return future;
        }

    private void setDone(Throwable t)
        {
        done = true;
        if (t == null)
            {
            doneFuture.complete(null);
            }
        else
            {
            doneFuture.completeExceptionally(t);
            }
        }

    private Throwable checkNotNull(Throwable thrown)
        {
        if (thrown == null)
            {
            thrown = Status.INVALID_ARGUMENT
                    .withDescription("onError called with null Throwable. Null exceptions are generally not allowed.")
                    .asRuntimeException();
            }

        return thrown;
        }

    /**
     * Throws a particular {@code Throwable} only if it belongs to a set of "fatal" error
     * varieties. These varieties are as follows:
     * <ul>
     * <li>{@code VirtualMachineError}</li>
     * <li>{@code LinkageError}</li>
     * </ul>
     *
     * @param thrown the {@code Throwable} to test and perhaps throw
     */
    private static void throwIfFatal(Throwable thrown)
        {
        if (thrown instanceof VirtualMachineError)
            {
            throw (VirtualMachineError) thrown;
            }
        else if (thrown instanceof LinkageError)
            {
            throw (LinkageError) thrown;
            }
        }

    /**
     * Returns {@code true} if the {@link Throwable} or its root cause
     * is due to a socket being closed.
     *
     * @param throwable  the {@link Throwable} to test
     *
     * @return {@code true} if the {@link Throwable} or its root cause
     *         is due to a socket being closed
     */
    private boolean isSocketClosedError(Throwable throwable)
        {
        Throwable cause = throwable;
        while (cause != null)
            {
            if (cause instanceof SocketException && "Socket closed".equals(cause.getMessage()))
                {
                return true;
                }
            cause = cause.getCause();
            }
        return false;
        }

    /**
     * Ensure that the specified {@link StreamObserver} is a safe observer.
     * <p>
     * If the specified observer is not an instance of {@link SafeStreamObserver} then wrap it in a
     * {@link SafeStreamObserver}.
     *
     * @param observer the {@link StreamObserver} to test
     * @param <T>      the response type expected by the observer
     * @return a safe {@link StreamObserver}
     */
    public static <T> StreamObserver<T> ensureSafeObserver(StreamObserver<T> observer)
        {
        if (observer instanceof SafeStreamObserver)
            {
            return observer;
            }

        return new SafeStreamObserver<>(observer);
        }

    // ----- constants ------------------------------------------------------
    /**
     * The actual StreamObserver.
     */
    private final StreamObserver<? super T> delegate;

    // ----- data members ---------------------------------------------------

    /**
     * Indicates a terminal state.
     */
    private volatile boolean done;

    private final CompletableFuture<Void> doneFuture = new CompletableFuture<>();

    /**
     * The {2link Logger} to use.
     */
    private static final Logger LOGGER = Logger.getLogger(SafeStreamObserver.class.getName());

    }
