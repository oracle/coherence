/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.proxy;

import io.grpc.stub.StreamObserver;

import io.reactivex.rxjava3.core.Observer;

import io.reactivex.rxjava3.disposables.Disposable;

import io.reactivex.rxjava3.observers.TestObserver;

/**
 * A test {@link StreamObserver} that is based on the RxJava {@link TestObserver}
 * that allows tests to easily assert state of the observer.
 *
 * @author Jonathan Knight  2019.11.18
 * @since 14.1.2
 */
public class TestStreamObserver<T>
        extends TestObserver<T>
        implements StreamObserver<T>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a non-forwarding TestStreamObserver.
     */
    public TestStreamObserver()
        {
        onSubscribe(new TestObserver<>());
        }

    /**
     * Constructs a forwarding TestStreamObserver.
     *
     * @param actual the {@link StreamObserver} to forward events to
     */
    public TestStreamObserver(StreamObserver<? super T> actual)
        {
        super(new WrapperStreamObserver<>(actual));
        onSubscribe(new TestObserver<>());
        }

    // ----- StreamObserver interface ---------------------------------------

    @Override
    public void onCompleted()
        {
        onComplete();
        }

    // ----- public methods -------------------------------------------------

    public Throwable getError()
        {
        return errors != null && errors.size() > 0 ? errors.get(0) : null;
        }

    /**
     * Obtain the specified value.
     *
     * @param i the index of the value to obtain
     *
     * @return the value at the specified index
     *
     * @throws IndexOutOfBoundsException – if the index is out of range
     */
    public T valueAt(int i)
        {
        return values().get(i);
        }

    // ----- inner class: WrapperStreamObserver -----------------------------

    private static class WrapperStreamObserver<T>
            implements Observer<T>, StreamObserver<T>
        {

        private final StreamObserver<T> wrapped;

        WrapperStreamObserver(StreamObserver<T> wrapped)
            {
            this.wrapped = wrapped;
            }

        @Override
        public void onCompleted()
            {
            wrapped.onCompleted();
            }

        @Override
        public void onNext(T t)
            {
            wrapped.onNext(t);
            }

        @Override
        public void onError(Throwable e)
            {
            wrapped.onError(e);
            }

        @Override
        public void onComplete()
            {
            wrapped.onCompleted();
            }

        @Override
        public void onSubscribe(Disposable d)
            {
            }
        }
    }
