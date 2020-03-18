/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.partition;


import com.oracle.coherence.common.base.Blocking;

import com.tangosol.net.Action;
import com.tangosol.net.ActionPolicy;
import com.tangosol.net.CacheService;
import com.tangosol.net.MemberEvent;
import com.tangosol.net.MemberListener;
import com.tangosol.net.PartitionedService;
import com.tangosol.net.Service;

import com.tangosol.util.Base;


/**
 * FailoverAccessPolicy is used to moderate the client request load during a
 * failover event in order to allow cache servers adequate opportunity to
 * re-establish partition backups.
 * <p>
 * While typically not necessary to ensure the timely recovery to a "safe"
 * state, this access policy could be used in situations where a heavy load of
 * of high-latency requests may prevent or significantly delay cache servers
 * from successfully acquiring exclusive access to partitions needing to be
 * transferred or backed up.
 *
 * @author rhl 09.12.2011
 * @since  Coherence 12.1.2
 */
public class FailoverAccessPolicy
        implements ActionPolicy
    {
    // ----- Constructors ----------------------------------------------------

    /**
     * Default constructor.
     */
    public FailoverAccessPolicy()
        {
        this(5000L, 60000L, 5000L);
        }

    /**
     * Construct a FailoverAccessPolicy according to the specified parameters.
     *
     * @param cThresholdMillis  the delay before the policy should start holding
     *                          requests (after becoming endangered)
     * @param cLimitMillis      the delay before the policy makes a maximal effort
     *                          to hold requests (after becoming endangered)
     * @param cMaxDelayMillis   the maximum amount of time to hold a request
     */
    public FailoverAccessPolicy(long cThresholdMillis, long cLimitMillis, long cMaxDelayMillis)
        {
        if (cThresholdMillis > cLimitMillis)
            {
            throw new IllegalArgumentException(
                    "The endangered threshold value must be less than or equal to the endangered limit");
            }
        if (cMaxDelayMillis <= 0)
            {
            throw new IllegalArgumentException("The max-delay value must be positive");
            }

        m_cThresholdMillis = cThresholdMillis;
        m_cLimitMillis     = cLimitMillis;
        m_cMaxDelayMillis  = cMaxDelayMillis;
        }


    // ----- accessors -------------------------------------------------------

    /**
     * Return the current endangered state (one of the STATE_* constants).
     *
     * @return the current endangered state
     */
    public int getState()
        {
        // Note: this could be a "stale" read (see #m_nState)
        return m_nState;
        }

    /**
     * Set the endangered state (one of the STATE_* constants).
     *
     * @param nState  the new endangered state
     */
    protected synchronized void setState(int nState)
        {
        m_nState = nState;
        }


    // ----- ActionPolicy methods --------------------------------------------

    /**
     * {@inheritDoc}
     */
    public void init(Service service)
        {
        Base.azzert(service instanceof PartitionedService,
                    "This ActionPolicy may only be used with PartitionedService");

        service.addMemberListener(new MembershipListener());
        m_service = (PartitionedService) service;
        }

    /**
     * {@inheritDoc}
     */
    public boolean isAllowed(Service service, Action action)
        {
        if ((action != CacheService.CacheAction.READ &&
             action != CacheService.CacheAction.WRITE) ||
            getState() == STATE_SAFE)
            {
            return true;
            }

        // the service could be endangered; check if the request should be held
        synchronized (this)
            {
            if (checkEndangered())
                {
                try
                    {
                    long ldtNow            = Base.getSafeTimeMillis();
                    long cMillisEndangered = ldtNow - m_ldtEndangered;
                    if (cMillisEndangered > m_cThresholdMillis)
                        {
                        // the service has been endangered longer than the
                        // threshold; calculate the amount of time to hold the
                        // client request for
                        long cWaitMillis = calculateWaitTime(cMillisEndangered);
                        if (cWaitMillis > 0L)
                            {
                            Blocking.wait(this, cWaitMillis);
                            }
                        }
                    }
                catch (InterruptedException e)
                    {
                    // restore the interrupt flag
                    Thread.currentThread().interrupt();
                    }

                if (getState() == STATE_ENDANGERED)
                    {
                    // reset the endangered state so it will checked again
                    // by the next set of requests
                    setState(STATE_UNKNOWN);

                    // first thread that wakes up should notify all others
                    // so the client load will go out at the same time,
                    // preventing "death by a thousand cuts"
                    notifyAll();
                    }
                }
            }

        return true;
        }


    // ----- helper methods --------------------------------------------------

    /**
     * Return the amount of time that a request should be delayed before being
     * allowed to proceed.
     *
     * @param cMillisEndangered  the amount of time that the service has been endangered
     *
     * @return the amount of time (in ms) to delay the client request
     */
    protected long calculateWaitTime(long cMillisEndangered)
        {
        // "dampened" logarithmic curve that approaches m_cMaxDelayMillis
        int  cScale = 1000;
        long cWait  = (long) (m_cMaxDelayMillis *
            Math.min(Math.log((float) cMillisEndangered / cScale) /
                     Math.log(m_cLimitMillis / cScale), 1));

        return cWait;
        }

    /**
     * Check to see if the associated service is endangered.
     * <p>
     * Note: the caller must hold synchronization on this quorum policy.
     *
     * @return true iff the service is endangered
     */
    protected boolean checkEndangered()
        {
        int nState = getState();
        switch (nState)
            {
            case STATE_SAFE:
                return false;

            case STATE_ENDANGERED:
                return true;

            case STATE_UNKNOWN:
            default:
            }

        PartitionedService service     = m_service;
        int                cPartitions = service.getPartitionCount();
        int                cBackups    = service.getBackupCount();

        for (int iPart = 0; iPart < cPartitions; iPart++)
            {
            for (int iStore = 1; iStore <= cBackups; iStore++)
                {
                if (service.getBackupOwner(iPart, iStore) == null)
                    {
                    if (m_ldtEndangered == 0L)
                        {
                        // entering the endangered state; set the timestamp
                        m_ldtEndangered = Base.getSafeTimeMillis();
                        }

                    setState(STATE_ENDANGERED);
                    return true;
                    }
                }
            }

        m_ldtEndangered = 0L;
        setState(STATE_SAFE);
        return false;
        }


    // ----- inner class: MembershipListener ---------------------------------

    /**
     * The MemberListener is used to listen to service membership events to
     * monitor the endangered status of the service.
     */
    protected class MembershipListener
            implements MemberListener
        {
        /**
         * {@inheritDoc}
         */
        public void memberJoined(MemberEvent evt)
            {
            }

        /**
         * {@inheritDoc}
         */
        public void memberLeaving(MemberEvent evt)
            {
            }

        /**
         * {@inheritDoc}
         */
        public void memberLeft(MemberEvent evt)
            {
            setState(STATE_UNKNOWN);
            }
        }


    // ----- constants and data members --------------------------------------

    /**
     * Constant used to indicate that the service is "safe" (non-endangered).
     */
    public static final int STATE_SAFE       = 0;

    /**
     * Constant used to indicate that the service is in an unknown (possibly
     * endangered) state.
     */
    public static final int STATE_UNKNOWN    = 1;

    /**
     * Constant used to indicate that the service is in an endangered state.
     */
    public static final int STATE_ENDANGERED = 2;

    /**
     * The amount of time after being in an endangered state before the quorum
     * policy should start to hold client requests.
     */
    public long m_cThresholdMillis;

    /**
     * The amount of time after being in an endangered state after which the
     * quorum policy makes a maximal effort to hold client requests.
     */
    public long m_cLimitMillis;

    /**
     * The maximum amount of time (in ms) that a request could be delayed by
     * this quorum policy.
     */
    public long m_cMaxDelayMillis;

    /**
     * One of the STATE_* constants representing the current quorum state.
     * <p>
     * Note: this field is intentionally left non-volatile.  The consequence of
     *       a "stale" read is that the first request per thread following a
     *       transition to endangered could be missed by this quorum (allowed to
     *       proceed).  As this has no impact, we would rather avoid the
     *       additional volatile read on each "normal" operation.
     */
    public int m_nState = STATE_SAFE;

    /**
     * The time at which the service most recently became endangered (or 0 if
     * the service is in the "safe" state).
     */
    public long m_ldtEndangered = 0;

    /**
     * The PartitionedService that this quorum policy is bound to.
     */
    protected PartitionedService m_service;
    }
