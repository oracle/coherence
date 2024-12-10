/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package executor.common;

import com.oracle.coherence.concurrent.executor.Task;

import java.util.concurrent.CopyOnWriteArrayList;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A {@link Task.Subscriber} that logs interactions to the {@link System#out}.
 *
 * @author lh
 * @since 21.12
 */
public class TestSubscriber<T>
        implements Task.Subscriber<T>
    {
    // ----- constructors ---------------------------------------------------

    public TestSubscriber()
        {
        this(WhereToThrow.NONE);
        }

    public TestSubscriber(WhereToThrow eventName)
        {
        f_fCompleted   = new AtomicBoolean(false);
        f_subscription = new AtomicReference<>();
        f_items        = new CopyOnWriteArrayList<>();
        f_eventName    = eventName;
        }

    // ----- Task.Subscriber interface --------------------------------------

    @Override
    public void onComplete()
        {
        System.out.println("Subscription [" + f_subscription + "] Completed");
        if (f_eventName.equals(WhereToThrow.ON_COMPLETE))
            {
            throw new RuntimeException("Test exception handling...");
            }
        f_fCompleted.compareAndSet(false, true);
        }

    @Override
    public void onError(Throwable throwable)
        {
        System.out.println("Subscription [" + f_subscription + "] Encountered: " + throwable);
        if (f_eventName.equals(WhereToThrow.ON_ERROR))
            {
            throw new RuntimeException("Test exception handling...");
            }
        throwable.printStackTrace();
        }

    @Override
    public void onNext(T item)
        {
        System.out.println("Subscription [" + f_subscription + "] Received: " + item);
        System.out.println("Now throw an exception to test exception handling...");
        if (f_eventName.equals(WhereToThrow.ON_NEXT))
            {
            throw new RuntimeException("Test exception handling...");
            }
        f_items.add(item);
        }

    @SuppressWarnings("rawtypes")
    @Override
    public void onSubscribe(Task.Subscription subscription)
        {
        System.out.println("Subscription [" + f_subscription + "] Commenced");
        if (f_eventName.equals(WhereToThrow.ON_SUBSCRIBE))
            {
            throw new RuntimeException("Test exception handling...");
            }
        f_subscription.set(subscription);
        }

    // ----- public methods -------------------------------------------------

    /**
     * Determines if the {@link TestSubscriber} has been completed by a {@link Task.Coordinator}.
     *
     * @return <code>true</code> if the {@link TestSubscriber} has been completed,
     *         <code>false</code> otherwise
     */
    public boolean isCompleted()
        {
        return f_fCompleted.get();
        }

    /**
     * Determines if the {@link TestSubscriber} has been subscribed to a {@link Task.Coordinator}.
     *
     * @return <code>true</code> if the {@link TestSubscriber} has been subscribed,
     *         <code>false</code> otherwise
     */
    public boolean isSubscribed()
        {
        return f_subscription.get() != null;
        }

    /**
     * Determines if the specified item was received.
     *
     * @param item  the item
     *
     * @return <code>true</code> if the item has been received,
     *         <code>false</code> otherwise
     */
    public boolean received(T item)
        {
        return f_items.contains(item);
        }

    // ----- enum: WhereToThrow ---------------------------------------------

    /**
     * Where to throw exception.
     */
    public enum WhereToThrow
        {
            NONE,
            ON_COMPLETE,
            ON_SUBSCRIBE,
            ON_NEXT,
            ON_ERROR,
        }

    // ----- data members ---------------------------------------------------

    /**
     * The {@link Task.Subscription} for the {@link Task.Subscriber}.
     */
    @SuppressWarnings("rawtypes")
    protected final AtomicReference<Task.Subscription> f_subscription;

    protected final AtomicBoolean f_fCompleted;

    protected final WhereToThrow f_eventName;

    protected final CopyOnWriteArrayList<T> f_items;
    }
