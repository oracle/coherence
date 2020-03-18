/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.common.base;

/**
 * SingleWaiterCooperativeNotifier is an extension of the SingleWaiterMultiNotifier which attempts to offload
 * potentially expensive "notification" work from signaling threads onto the waiting threads. This notifier
 * is beneficial when there are few signaling threads, but potentially many waiting threads, each waiting on
 * their own notifier.
 * <p>
 * Unlike the standard Notifier usage, a signaling thread must at some point invoke the static {@link #flush}
 * method, or {@link #await} on any SingleWaiterCooperativeNotifier to ensure that all deferred signals
 * are processed.
 *
 * @author mf  2014.03.12
 * @deprecated use {@link com.oracle.coherence.common.base.SingleWaiterCooperativeNotifier} instead
 */
@Deprecated
public class SingleWaiterCooperativeNotifier
        extends com.oracle.coherence.common.base.SingleWaiterCooperativeNotifier
        implements Notifier
    {
    /**
     * Ensure that any deferred signals will be processed.
     * <p>
     * Note it is more advantageous if the calling thread's natural idle point is
     * to sit in {@link #await}, in which case calling this method is not required.
     * </p>
     *
     * @deprecated use {@link com.oracle.coherence.common.base.SingleWaiterCooperativeNotifier}
     *             instead
     */
     public static void flush()
        {
        com.oracle.coherence.common.base.SingleWaiterCooperativeNotifier.flush();
        }
    }
