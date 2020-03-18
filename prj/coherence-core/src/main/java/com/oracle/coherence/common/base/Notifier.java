/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.common.base;


/**
 * A Condition-like object, used to block thread(s) for a notification.
 * <p>
 * Unlike {@link java.util.concurrent.locks.Condition Condition} no external locking or synchronization
 * is needed with Notifiers; i.e. clients need not synchronize on this class prior to
 * calling <tt>await()</tt> or <tt>signal()</tt>, nor should they use any of the
 * primitive <tt>wait()</tt> or <tt>notify()</tt> methods.
 *
 * Note: the Notifiers are expected to be {@link Timeout} compatible.*
 *
 * @author cp/mf  2010-06-15
 */
public interface Notifier
    {
    /**
    * Wait for a notification. Note that spurious wake-ups are possible.
    *
    * @throws InterruptedException  if the calling thread is interrupted
    *         while it is waiting
    */
    public default void await()
        throws InterruptedException
        {
        await(0);
        }

    /**
     * Wait for a notification. Note that spurious wake-ups are possible.
     *
     * @param cMillis  the maximum wait time in milliseconds, or zero for indefinite
     *
     * @throws InterruptedException  if the calling thread is interrupted
     *         while it is waiting
     */
    public void await(long cMillis)
            throws InterruptedException;

    /**
    * Notifies the waiting thread(s), waking them up if awaiting.
    */
    public void signal();
    }
