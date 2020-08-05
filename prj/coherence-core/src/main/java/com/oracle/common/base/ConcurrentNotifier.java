/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.common.base;


import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import java.util.concurrent.locks.LockSupport;


/**
 * A Condition-like object, usable by multiple threads to both wait and signal.
 * <p>
 * Note that no synchronization is needed to use this class; i.e. clients
 * must not synchronize on this class prior to calling <code>await()</code> or
 * <code>signal()</code>, nor should the use any of the primitive <code>wait()</code>
 * or <code>notify()</code> methods.
 * <p>
 * Unlike the {@link SingleWaiterMultiNotifier}, this notifier implementation requires
 * the notion of a {@link #isReady ready} check.  When the notifier is ready then a call
 * to await because a no-op.  An example ready check for a notifier based queue removal
 * would be <code>!queue.isEmpty();</code>
 *
 * @author mf  2018.04.13
 * @deprecated use {@link com.oracle.coherence.common.base.ConcurrentNotifier} instead
 */
@Deprecated
public abstract class ConcurrentNotifier
        extends com.oracle.coherence.common.base.ConcurrentNotifier
        implements Notifier
    {
    }
