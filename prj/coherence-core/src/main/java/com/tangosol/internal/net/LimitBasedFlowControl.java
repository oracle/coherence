/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A limit based FlowControl implementation.
 *
 * @author mf  2016.02.03
 */
public class LimitBasedFlowControl
    extends AbstractFlowControl
    {
    // ----- BasicFlowControl interface -------------------------------------

    /**
     * Construct a flow control object with the specified limits.
     *
     * @param lLimitNormal    the limit below which a backlog is considered to be normal
     * @param lLimitExcessive the limit above which a backlog is considered to be excessive
     */
    public LimitBasedFlowControl(long lLimitNormal, long lLimitExcessive)
        {
        f_lLimitNormal    = lLimitNormal;
        f_lLimitExcessive = lLimitExcessive;
        }

    /**
     * Evaluate the current backlog.
     *
     * @param lBacklog the backlog
     *
     * @return this object
     */
    public LimitBasedFlowControl evaluateBacklog(long lBacklog)
        {
        if (lBacklog > f_lLimitExcessive && !m_fBacklogged)
            {
            m_fBacklogged = true;
            }
        else if (lBacklog <= f_lLimitNormal && m_fBacklogged)
            {
            m_fBacklogged = false;
            notifyWaiters();
            }
        // else; unchanged (common case)

        return this;
        }

    @Override
    public boolean isBacklogged()
        {
        return m_fBacklogged;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The limit at which a backlog condition ends.
     */
    protected final long f_lLimitNormal;

    /**
     * The limit at which a backlog condition starts.
     */
    protected final long f_lLimitExcessive;

    /**
     * True if the backlog is excessive
     */
    protected volatile boolean m_fBacklogged;
    }
