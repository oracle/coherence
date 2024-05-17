/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc;

import io.grpc.stub.StreamObserver;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A {@link StreamObserver} that delegates calls to another
 * {@link StreamObserver} under a {@link Lock}.
 *
 * @param <V>  the type of the observer
 */
public class LockingStreamObserver<V>
        implements StreamObserver<V>
    {
    /**
     * Create a {@link LockingStreamObserver}.
     *
     * @param delegate  the {@link StreamObserver} to delegate to
     */
    public LockingStreamObserver(StreamObserver<? super V> delegate)
        {
        f_delegate = (SafeStreamObserver<? super V>) SafeStreamObserver.ensureSafeObserver(delegate);
        }

    @Override
    public void onNext(V value)
        {
        f_lock.lock();
        try
            {
            f_delegate.onNext(value);
            }
        finally
            {
            f_lock.unlock();
            }
        }

    @Override
    public void onError(Throwable t)
        {
        f_lock.lock();
        try
            {
            f_delegate.onError(t);
            }
        finally
            {
            f_lock.unlock();
            }
        }

    @Override
    public void onCompleted()
        {
        f_lock.lock();
        try
            {
            f_delegate.onCompleted();
            }
        finally
            {
            f_lock.unlock();
            }
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Returns {@code true} if this observer is complete.
     *
     * @return {@code true} if this observer is complete
     */
    public boolean isDone()
        {
        return f_delegate.isDone();
        }

    /**
     * Ensure that the specified {@link StreamObserver} is a safe observer.
     * <p>
     * If the specified observer is not an instance of {@link LockingStreamObserver} then wrap it in a
     * {@link LockingStreamObserver}.
     *
     * @param observer the {@link StreamObserver} to test
     * @param <T>      the response type expected by the observer
     * @return a safe {@link StreamObserver}
     */
    public static <T> LockingStreamObserver<T> ensureLockingObserver(StreamObserver<T> observer)
        {
        if (observer instanceof LockingStreamObserver)
            {
            return (LockingStreamObserver<T>) observer;
            }
        return new LockingStreamObserver<>(observer);
        }

    // ----- data members ---------------------------------------------------

    /**
     * The lock to synchronize writing to the observer.
     */
    private final Lock f_lock = new ReentrantLock();

    /**
     * The {@link StreamObserver} to delegate to.
     */
    private final SafeStreamObserver<? super V> f_delegate;
    }
