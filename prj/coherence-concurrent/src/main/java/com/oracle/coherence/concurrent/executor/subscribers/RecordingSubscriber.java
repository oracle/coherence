/*
 * Copyright (c) 2016, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.concurrent.executor.subscribers;

import com.oracle.coherence.concurrent.executor.Task;

import com.oracle.coherence.concurrent.executor.internal.ExecutorTrace;

import java.util.concurrent.CopyOnWriteArrayList;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A {@link Task.Subscriber} that records iteration with a {@link Task.Coordinator}.
 *
 * @param <T>  the type of result received
 *
 * @author bo
 */
public class RecordingSubscriber<T>
        implements Task.Subscriber<T>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a {@link RecordingSubscriber}.
     */
    public RecordingSubscriber()
        {
        f_fCompleted   = new AtomicBoolean(false);
        f_fErrored     = new AtomicBoolean(false);
        f_throwable    = new AtomicReference<>();
        f_listItems    = new CopyOnWriteArrayList<>();
        f_subscription = new AtomicReference<>();
        f_fUsed        = new AtomicBoolean(false);
        }

    // ----- Task.Subscriber interface --------------------------------------

    @Override
    public void onComplete()
        {
        ExecutorTrace.entering(RecordingSubscriber.class, "onComplete");

        f_fCompleted.compareAndSet(false, true);
        f_subscription.set(null);

        ExecutorTrace.exiting(RecordingSubscriber.class, "onComplete");
        }

    @Override
    public void onError(Throwable throwable)
        {
        ExecutorTrace.entering(RecordingSubscriber.class, "onError", throwable);

        f_fErrored.compareAndSet(false, true);
        f_subscription.set(null);
        f_throwable.set(throwable);

        ExecutorTrace.exiting(RecordingSubscriber.class, "onError");
        }

    @Override
    public void onNext(T item)
        {
        ExecutorTrace.entering(RecordingSubscriber.class, "onNext", item);

        f_listItems.add(item);

        ExecutorTrace.exiting(RecordingSubscriber.class, "onNext");
        }

    @Override
    public void onSubscribe(Task.Subscription<? extends T> subscription)
        {
        ExecutorTrace.entering(RecordingSubscriber.class, "onSubscribe", subscription);

        try
            {
            if (f_fUsed.compareAndSet(false, true))
                {
                f_subscription.set(subscription);
                }
            else
                {
                throw new UnsupportedOperationException("RecordingSubscriber reuse is not supported.");
                }
            }
            finally
                {
                ExecutorTrace.exiting(RecordingSubscriber.class, "onSubscribe");
                }
        }

    // ----- accessors ------------------------------------------------------

    /**
     * Determines if the {@link RecordingSubscriber} was published an error by a {@link Task.Coordinator}.
     *
     * @return <code>true</code> if {@link RecordingSubscriber#onError(Throwable)} was invoked
     * <code>false</code> otherwise
     */
    public boolean isError()
        {
        return f_fErrored.get();
        }

    /**
     * Determines if the {@link RecordingSubscriber} was completed by a {@link Task.Coordinator}.
     *
     * @return <code>true</code> if {@link RecordingSubscriber#onComplete()} was invoked,
     * <code>false</code> otherwise
     */
    public boolean isCompleted()
        {
        return f_fCompleted.get();
        }

    /**
     * Determines if the {@link RecordingSubscriber} has been subscribed to a {@link Task.Coordinator}.
     *
     * @return <code>true</code> if the {@link RecordingSubscriber} has been subscribed,
     * <code>false</code> otherwise
     */
    public boolean isSubscribed()
        {
        return f_subscription.get() != null;
        }

    /**
     * Determines if the specified item was received.
     *
     * @param item the item
     *
     * @return <code>true</code> if the item has been received,
     * <code>false</code> otherwise
     */
    public boolean received(T item)
        {
        return f_listItems.contains(item);
        }

    /**
     * Returns the first item that was received the by the {@link Task.Subscriber}.
     *
     * @return the first item or <code>null</code> if no items have been received
     */
    public T getFirst()
        {
        return f_listItems.get(0);
        }

    /**
     * Returns the last item that was received the by the {@link Task.Subscriber}.
     *
     * @return the last item or <code>null</code> if no items have been received
     */
    public T getLast()
        {
        int size = f_listItems.size();

        return size == 0 ? null : f_listItems.get(size - 1);
        }

    /**
     * Determines the number of items the {@link RecordingSubscriber} has received.
     *
     * @return the number of items
     */
    public int size()
        {
        return f_listItems.size();
        }

    /**
     * Returns the {@link Throwable} that completed the {@link Task} or <code>null</code> if the {@link Task} is still
     * active or was not completed with an exception.
     *
     * @return the {@link Throwable} that completed the {@link Task} or <code>null</code>
     */
    public Throwable getThrowable()
        {
        return f_throwable.get();
        }

    // ----- data members ---------------------------------------------------

    /**
     * Tracks whether this {@link RecordingSubscriber} has ever been subscribed to a {@link Task.Coordinator}. {@link
     * RecordingSubscriber}s are not reusable.
     */
    protected final AtomicBoolean f_fUsed;

    /**
     * Completed flag.
     */
    protected final AtomicBoolean f_fCompleted;

    /**
     * Error flag.
     */
    protected final AtomicBoolean f_fErrored;

    /**
     * Failure cause.
     */
    protected final AtomicReference<Throwable> f_throwable;

    /**
     * Items received by this subscriber.
     */
    protected final CopyOnWriteArrayList<T> f_listItems;

    /**
     * The {@link Task.Subscription}.
     */
    protected final AtomicReference<Task.Subscription<? extends T>> f_subscription;
    }
