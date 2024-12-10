/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net;

import com.oracle.coherence.common.base.Converter;
import com.oracle.coherence.common.base.NonBlocking;

import java.util.concurrent.atomic.AtomicLong;

/**
 * A FlowControl implementation which supports debouncing based on user specified thresholds.
 *
 * @author mf  2016.02.03
 */
public class DebouncedFlowControl
    extends AbstractFlowControl
    {
    // ----- DebouncedFlowControl interface ---------------------------------

    /**
     * Construct a flow control object with the specified limits.
     *
     * @param lLimitNormal    the limit below which a backlog is considered to be normal
     * @param lLimitExcessive the limit above which a backlog is considered to be excessive
     */
    public DebouncedFlowControl(long lLimitNormal, long lLimitExcessive)
        {
        this (lLimitNormal, lLimitExcessive, null);
        }

    /**
     * Construct a flow control object with the specified limits.
     *
     * @param lLimitNormal    the limit below which a backlog is considered to be normal
     * @param lLimitExcessive the limit above which a backlog is considered to be excessive
     * @param unitFormatter   converter used for format the backlog units
     */
    public DebouncedFlowControl(long lLimitNormal, long lLimitExcessive, Converter<Long, String> unitFormatter)
        {
        f_lLimitNormal    = lLimitNormal;
        f_lLimitExcessive = lLimitExcessive;
        f_unitFormatter   = unitFormatter == null ? Object::toString : unitFormatter;
        }


    // ----- DebouncedFlowControl interface ---------------------------------

    /**
     * Return the current backlog.
     *
     * @return the backlog
     */
    public long getBacklog()
        {
        return f_lBacklog.get() >> 1;
        }

    /**
     * Adjust the backlog by the specified amount and evaluate the backlog.
     *
     * @param c  the amount to adjust by
     *
     * @return this object
     */
    public DebouncedFlowControl adjustBacklog(long c)
        {
        boolean fExitBacklog;
        long    lBacklogOld;
        long    lBacklogNew;
        long    cBacklogNew;
        long    fBacklogged;

        do
            {
            lBacklogOld  = f_lBacklog.get();
            cBacklogNew  = (lBacklogOld >> 1) + c;
            fBacklogged  = lBacklogOld & 0x1;
            fExitBacklog = false;

            if (cBacklogNew >= Long.MAX_VALUE >> 1 || cBacklogNew <= Long.MIN_VALUE >> 1)
                {
                throw new IllegalArgumentException("adjustment would overflow");
                }
            else if (fBacklogged == 0 && cBacklogNew >= f_lLimitExcessive)
                {
                fBacklogged = 1;
                }
            else if (fBacklogged == 1 && cBacklogNew <= f_lLimitNormal)
                {
                fBacklogged  = 0;
                fExitBacklog = true;
                }

            lBacklogNew = (cBacklogNew << 1) | fBacklogged;
            }
        while (!f_lBacklog.compareAndSet(lBacklogOld, lBacklogNew));

        if (fExitBacklog)
            {
            // we transitioned from backlogged to normal above
            // we may have reentered by now, but it is still safe to trigger the continuations
            processContinuations();
            }
        else if (fBacklogged == 1 && !NonBlocking.isNonBlockingCaller())
            {
            drainBacklog(0);
            }

        return this;
        }

    /**
     * Increment and evaluate the backlog.
     *
     * @return this object
     */
    public DebouncedFlowControl incrementBacklog()
        {
        return adjustBacklog(1);
        }

    /**
     * Decrement and evaluate the backlog.
     *
     * @return this object
     */
    public DebouncedFlowControl decrementBacklog()
        {
        return adjustBacklog(-1);
        }

    /**
     * Returns the limit below which the backlog is considered normal.
     *
     * @return the limit below which the backlog is considered normal
     */
    public long getNormalLimit()
        {
        return f_lLimitNormal;
        }

    /**
     * Returns the limit above which the flow control is considered backlogged.
     *
     * @return the limit above which the flow control is considered backlogged
     */
    public long getExcessiveLimit()
        {
        return f_lLimitExcessive;
        }

    // ----- AbstractFlowControl interface ----------------------------------

    @Override
    public boolean isBacklogged()
        {
        return (f_lBacklog.get() & 0x1) != 0;
        }

    // ---- Object interface ------------------------------------------------

    @Override
    public String toString()
        {
        long lBacklog = f_lBacklog.get();
        long cBacklog = lBacklog >> 1;
        long fBacklog = lBacklog & 0x1;
        return f_unitFormatter.convert(cBacklog) + "/" +
            f_unitFormatter.convert(f_lLimitExcessive) + (" " + cBacklog * 100 / f_lLimitExcessive + "%") +
            (fBacklog == 0 ? " normal" : " excessive");
        }

    // ----- data members ---------------------------------------------------

    /**
     * The limit at which a backlog condition ends.
     */
    private final long f_lLimitNormal;

    /**
     * The limit at which a backlog condition starts.
     */
    private final long f_lLimitExcessive;

    /**
     * The bit 0 is backlog state, remaining 63 bits are the backlog counter.
     */
    private final AtomicLong f_lBacklog = new AtomicLong();

    /**
     * The unit formatter
     */
    private final Converter<Long, String> f_unitFormatter;
    }
