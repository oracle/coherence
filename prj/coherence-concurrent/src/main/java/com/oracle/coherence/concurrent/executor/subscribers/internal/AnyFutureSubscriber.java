/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.concurrent.executor.subscribers.internal;

/**
 * A {@link FutureSubscriber} which notifies a monitor object when the task completes or fails.
 *
 * @param <T>  the non-null type of result to be subscribed by the subscriber.
 *
 * @author bo, lh
 * @since 21.12
 */
public class AnyFutureSubscriber<T>
        extends FutureSubscriber<T>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a {@link AnyFutureSubscriber}.
     *
     * @param oCompletionMonitor  object monitor to track whether the task is done either via completion or exception
     */
    public AnyFutureSubscriber(Object oCompletionMonitor)
        {
        f_oMonitor = oCompletionMonitor;
        }

    // ----- FutureSubscriber methods ---------------------------------------

    @Override
    public void onComplete()
        {
        super.onComplete();
        synchronized (f_oMonitor)
            {
            f_oMonitor.notifyAll();
            }
        }

    @Override
    public void onError(Throwable throwable)
        {
        super.onError(throwable);
        synchronized (f_oMonitor)
            {
            f_oMonitor.notifyAll();
            }
        }

    // ----- data members ---------------------------------------------------

    /**
     * Object monitor to track whether the task is done either via completion or exception.
     */
    protected final Object f_oMonitor;
    }
