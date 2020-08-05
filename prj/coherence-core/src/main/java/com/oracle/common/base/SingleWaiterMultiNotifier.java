/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.common.base;

/**
 * A Condition-like object, used by a single thread to block for a
 * notification, and optimized for many concurrent notifications by other
 * threads. Basically, this is a blocking queue without any state to
 * actually enqueue: the {@link SingleWaiterMultiNotifier#await()} method
 * is analogous to an imaginary "take all" variant of the
 * {@link java.util.concurrent.BlockingQueue#take() BlockingQueue.take()}
 * method, and the
 * {@link SingleWaiterMultiNotifier#signal()} method is analogous to
 * {@link java.util.concurrent.BlockingQueue#put(Object)
 * BlockingQueue.put()}.
 * <p>
 * Note that no synchronization is needed to use this class; i.e. clients
 * must not synchronize on this class prior to calling <code>await()</code> or
 * <code>signal()</code>, nor should the use any of the primitive <code>wait()</code>
 * or <code>notify()</code> methods.
 * <p>
 * Since SingleWaiterMultiNotifier is only usable by a single waiting thread it is
 * does not require an external readiness check, as signaling can record that state.
 *
 * @author cp/mf  2010-06-15
 * @deprecated use {@link com.oracle.coherence.common.base.SingleWaiterMultiNotifier} instead
 */
@Deprecated
public class SingleWaiterMultiNotifier
        extends com.oracle.coherence.common.base.SingleWaiterMultiNotifier
        implements Notifier
    {
    }
