/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package grpc.proxy;

import com.google.protobuf.Message;
import com.oracle.coherence.common.base.Logger;
import io.grpc.stub.StreamObserver;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observer;

import io.reactivex.rxjava3.disposables.Disposable;

import io.reactivex.rxjava3.observers.TestObserver;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * A test {@link StreamObserver} that is based on the RxJava {@link TestObserver}
 * that allows tests to easily assert state of the observer.
 *
 * @author Jonathan Knight  2019.11.18
 * @since 20.06
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
        if (m_fLog)
            {
            Logger.info("TestStreamObserver.onCompleted() called");
            }
        onComplete();
        }

    @Override
    public void onNext(@NonNull T value)
        {
        if (m_fLog)
            {
            Logger.info("TestStreamObserver.onCompleted() called value=" + value);
            }
        super.onNext(value);
        }

    @Override
    public void onError(@NonNull Throwable t)
        {
        if (m_fLog)
            {
            Logger.info("TestStreamObserver.onCompleted() called t=" + t);
            }
        super.onError(t);
        }

    // ----- public methods -------------------------------------------------

    public void enableLog(boolean enable)
        {
        m_fLog = enable;
        }

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

    /**
     * Return the number of messages received by this observer.
     *
     * @return the number of messages received by this observer
     */
    public int valueCount()
        {
        return values().size();
        }

    /**
     * Wait until the {@code TestObserver} receives the given
     * number of items.
     *
     * @param atLeast  the number of items expected at least
     * @param timeout  the timeout value
     * @param unit     the timeout units
     *
     * @return this
     */
    public TestStreamObserver<T> awaitCount(int atLeast, long timeout, TimeUnit unit) throws InterruptedException
        {
        long start         = System.currentTimeMillis();
        long timeoutMillis = unit.toMillis(timeout);

        for (; ; )
            {
            if (System.currentTimeMillis() - start >= timeoutMillis)
                {
                this.timeout = true;
                break;
                }
            if (done.getCount() == 0L)
                {
                break;
                }
            if (values.size() >= atLeast)
                {
                break;
                }

            try
                {
                Thread.sleep(10);
                }
            catch (InterruptedException ex)
                {
                throw new RuntimeException(ex);
                }
            }
        return this;
        }

    /**
     * Return a {@link Stream} of received values of the specified type
     *
     * @param converter  a function to convert the messages received by this observer into the required type
     * @param type       the type of message
     * @param <M>        the type of message
     *
     * @return a {@link Stream} of received values of the specified type
     */
    public <M extends Message> Stream<M> streamOf(Function<T, M> converter, Class<M> type)
        {
        return values().stream()
                .map(converter)
                .filter(Objects::nonNull)
                .filter(type::isInstance)
                .map(type::cast);
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

    // ----- data members ---------------------------------------------------

    private boolean m_fLog;
    }
