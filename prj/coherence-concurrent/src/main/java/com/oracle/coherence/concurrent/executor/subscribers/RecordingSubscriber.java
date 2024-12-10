/*
 * Copyright (c) 2016, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.concurrent.executor.subscribers;

import com.oracle.coherence.concurrent.executor.Task;

import com.oracle.coherence.concurrent.executor.internal.ExecutorTrace;

import java.util.concurrent.CopyOnWriteArrayList;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

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
        f_listItems = new CopyOnWriteArrayList<>();
        f_rwLock    = new ReentrantReadWriteLock();
        }

    // ----- Task.Subscriber interface --------------------------------------

    @Override
    public void onComplete()
        {
        ExecutorTrace.entering(RecordingSubscriber.class, "onComplete");

        Lock wLock = f_rwLock.writeLock();
        try
            {
            wLock.lock();
            m_fCompleted   = true;
            m_subscription = null;
            }
        finally
            {
            wLock.unlock();
            }

        ExecutorTrace.exiting(RecordingSubscriber.class, "onComplete");
        }

    @Override
    public void onError(Throwable throwable)
        {
        ExecutorTrace.entering(RecordingSubscriber.class, "onError", throwable);

        Lock wLock = f_rwLock.writeLock();
        try
            {
            wLock.lock();
            m_fErrored     = true;
            m_subscription = null;
            m_throwable    = throwable;
            }
        finally
            {
            wLock.unlock();
            }

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

        Lock wLock = f_rwLock.writeLock();
        try
            {
            wLock.lock();
            if (!m_fUsed)
                {
                m_fUsed        = true;
                m_subscription = subscription;
                }
            else
                {
                throw new UnsupportedOperationException("RecordingSubscriber reuse is not supported.");
                }
            }
            finally
                {
                wLock.unlock();
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
        Lock rLock = f_rwLock.readLock();
        try
            {
            rLock.lock();
            return m_fErrored;
            }
        finally
            {
            rLock.unlock();
            }
        }

    /**
     * Determines if the {@link RecordingSubscriber} was completed by a {@link Task.Coordinator}.
     *
     * @return <code>true</code> if {@link RecordingSubscriber#onComplete()} was invoked,
     * <code>false</code> otherwise
     */
    public boolean isCompleted()
        {
        Lock rLock = f_rwLock.readLock();
        try
            {
            rLock.lock();
            return m_fCompleted;
            }
        finally
            {
            rLock.unlock();
            }
        }

    /**
     * Determines if the {@link RecordingSubscriber} has been subscribed to a {@link Task.Coordinator}.
     *
     * @return <code>true</code> if the {@link RecordingSubscriber} has been subscribed,
     * <code>false</code> otherwise
     */
    public boolean isSubscribed()
        {
        Lock rLock = f_rwLock.readLock();
        try
            {
            rLock.lock();
            return m_subscription != null;
            }
        finally
            {
            rLock.unlock();
            }
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
        Lock rLock = f_rwLock.readLock();
        try
            {
            rLock.lock();
            return m_throwable;
            }
        finally
            {
            rLock.unlock();
            }
        }

    // ----- data members ---------------------------------------------------

    /**
     * Tracks whether this {@link RecordingSubscriber} has ever been subscribed to a {@link Task.Coordinator}. {@link
     * RecordingSubscriber}s are not reusable.
     */
    protected boolean m_fUsed;

    /**
     * Completed flag.
     */
    protected boolean m_fCompleted;

    /**
     * Error flag.
     */
    protected boolean m_fErrored;

    /**
     * Failure cause.
     */
    protected Throwable m_throwable;

    /**
     * Items received by this subscriber.
     */
    protected CopyOnWriteArrayList<T> f_listItems;

    /**
     * The {@link Task.Subscription}.
     */
    protected Task.Subscription<? extends T> m_subscription;

    /**
     * {@link ReadWriteLock} to guard mutations.
     */
    protected final ReadWriteLock f_rwLock;
    }
