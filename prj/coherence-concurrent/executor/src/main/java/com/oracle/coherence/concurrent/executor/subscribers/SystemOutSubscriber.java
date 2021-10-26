/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.concurrent.executor.subscribers;

import com.oracle.coherence.concurrent.executor.Task;

/**
 * A {@link Task.Subscriber} that logs interactions to the {@link System#out}.
 *
 * @param <T>  the type of result received
 *
 * @author bo
 * @since 21.12
 */
public class SystemOutSubscriber<T>
        implements Task.Subscriber<T>
    {

    // ----- Task.Subscriber interface --------------------------------------

    @Override
    public void onComplete()
        {
        System.out.println("Subscription [" + m_subscription + "] Completed");
        }

    @Override
    public void onError(Throwable throwable)
        {
        System.out.println("Subscription [" + m_subscription + "] Encountered: " + throwable);

        throwable.printStackTrace();
        }

    @Override
    public void onNext(T item)
        {
        System.out.println("Subscription [" + m_subscription + "] Received: " + item);
        }

    @Override
    public void onSubscribe(Task.Subscription subscription)
        {
        System.out.println("Subscription [" + subscription + "] Commenced");

        m_subscription = subscription;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The {@link Task.Subscription} for the {@link Task.Subscriber}.
     */
    protected Task.Subscription m_subscription;
    }
