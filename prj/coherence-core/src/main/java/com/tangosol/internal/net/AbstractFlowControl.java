/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net;

import com.oracle.coherence.common.base.Blocking;
import com.oracle.coherence.common.base.Continuation;
import com.oracle.coherence.common.collections.ConcurrentLinkedQueue;

import com.tangosol.net.FlowControl;

import com.tangosol.util.Base;

import java.util.Queue;

/**
 * Abstract implementation of the FlowControl interface.
 *
 * @author mf  2016.02.03
 */
public abstract class AbstractFlowControl
        implements FlowControl
    {
    // -----  AbstractFlowControl interface ---------------------------------

    /**
     * Return true iff a backlog is present
     *
     * @return true iff a backlog is present
     */
    public abstract boolean isBacklogged();

    /**
     * Invoke any pending Continuations from {@link #drainBacklog(long)}
     */
    protected void notifyWaiters()
        {
        Continuation<Void> continuation;
        while ((continuation = f_queueWaiting.poll()) != null)
            {
            continuation.proceed(null);
            }

        synchronized (this)
            {
            notifyAll();
            }
        }

    /**
     * Invoke any pending Continuations from {@link #drainBacklog(long)}
     */
    protected void processContinuations()
        {
        Continuation<Void> continuation;
        while ((continuation = f_queueWaiting.poll()) != null)
            {
            continuation.proceed(null);
            }

        synchronized (this)
            {
            notifyAll();
            }
        }

    // ----- FlowControl interface ------------------------------------------

    @Override
    public void flush()
        {
        // no-op at this level
        }

    @Override
    public long drainBacklog(long cMillis)
        {
        if (isBacklogged())
            {
            long ldtTimeout = cMillis == 0 ? Long.MAX_VALUE : Base.getSafeTimeMillis() + cMillis;

            synchronized (this)
                {
                try
                    {
                    for (cMillis  = Base.computeSafeWaitTime(ldtTimeout);
                         cMillis >= 0 && isBacklogged();
                         cMillis  = Base.computeSafeWaitTime(ldtTimeout))
                        {
                        Blocking.wait(this, cMillis);
                        }

                    return Base.computeSafeWaitTime(ldtTimeout);
                    }
                catch (InterruptedException e)
                    {
                    Thread.currentThread().interrupt();
                    return -1;
                    }
                }
            }

        return cMillis;
        }

    @Override
    public boolean checkBacklog(Continuation<Void> continueNormal)
        {
        if (isBacklogged())
            {
            if (continueNormal != null)
                {
                f_queueWaiting.add(continueNormal);
                if (!isBacklogged())
                    {
                    notifyWaiters();
                    }
                }
            return true;
            }
        return false;
        }


    // ----- data members ---------------------------------------------------

    /**
     * The continuations awaiting notification.
     */
    private final Queue<Continuation<Void>> f_queueWaiting = new ConcurrentLinkedQueue<>();
    }
