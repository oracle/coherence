/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net;

import com.tangosol.util.Filter;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.InvocableMapHelper;
import com.tangosol.util.MapTrigger;

import com.tangosol.util.filter.AlwaysFilter;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Mutable lifecycle and aggregation state owned by one continuous aggregation
 * registration in one primary partition.
 *
 * @author Aleks Seovic  2026.09.06
 * @since 26.10
 */
public class ContinuousAggregationState
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a state that requires an initial partition build.
     */
    public ContinuousAggregationState()
        {
        }

    // ----- lifecycle ------------------------------------------------------

    /**
     * Attempt to reserve a rebuild for this state.
     *
     * @return {@code true} if the caller should schedule the rebuild
     */
    public boolean scheduleBuild()
        {
        Status status = m_status;
        return (status == Status.BUILDING || status == Status.DIRTY)
                && m_fBuildScheduled.compareAndSet(false, true);
        }

    /**
     * Mark this state for a full rebuild. This is used when a stale bound was
     * required to answer a query and the partition had to be scanned.
     */
    public synchronized void requireRebuild()
        {
        if (m_status == Status.STALE_BOUND)
            {
            m_status = Status.DIRTY;
            }
        }

    /**
     * Install a successfully built aggregator instance.
     *
     * @param aggregator  the built partition aggregator
     */
    public synchronized void completeBuild(InvocableMap.StreamingAggregator aggregator)
        {
        m_aggregator = aggregator;
        m_failure    = null;
        m_status     = Status.READY;
        m_fBuildScheduled.set(false);
        }

    /**
     * Record a failed build.
     *
     * @param failure  the build failure, or {@code null} if ownership changed
     */
    public synchronized void failBuild(Throwable failure)
        {
        m_failure = failure;
        m_status  = Status.DIRTY;
        m_fBuildScheduled.set(false);
        }

    /**
     * Return whether this state currently has an exact result.
     *
     * @return {@code true} if the result is ready
     */
    public boolean isReady()
        {
        return m_status == Status.READY;
        }

    /**
     * Return this state's status.
     *
     * @return the current status
     */
    public Status getStatus()
        {
        return m_status;
        }

    /**
     * Return the last maintenance or build failure.
     *
     * @return the last failure, or {@code null}
     */
    public synchronized Throwable getFailure()
        {
        return m_failure;
        }

    // ----- maintenance ----------------------------------------------------

    /**
     * Apply a backing-map transition to this state.
     * <p>
     * The entry is expected to implement {@link MapTrigger.Entry} and expose
     * both its original and current values. Failures are contained by marking
     * this state dirty; they are never propagated to the cache mutation.
     *
     * @param definition  the continuous aggregation definition
     * @param entry       the changed entry
     *
     * @return the resulting state status
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public synchronized Status apply(ContinuousAggregationDefinition definition,
                                     InvocableMap.Entry entry)
        {
        if (m_status != Status.READY && m_status != Status.STALE_BOUND)
            {
            return m_status;
            }

        try
            {
            Filter           filter       = definition.getEvaluationFilter();
            MapTrigger.Entry triggerEntry = (MapTrigger.Entry) entry;
            boolean          fAlways      = filter instanceof AlwaysFilter;
            boolean          fOldMatches  = triggerEntry.isOriginalPresent()
                    && (fAlways || InvocableMapHelper.evaluateOriginalEntry(filter, triggerEntry));
            boolean          fNewMatches  = entry.isPresent()
                    && (fAlways || InvocableMapHelper.evaluateEntry(filter, entry));

            if (!fOldMatches && !fNewMatches)
                {
                return m_status;
                }

            InvocableMap.StreamingAggregator aggregator = m_aggregator;
            InvocableMap.StreamingAggregator.RetractionResult result;
            if (fOldMatches && fNewMatches)
                {
                result = aggregator.update(entry);
                }
            else
                {
                result = InvocableMap.StreamingAggregator.RetractionResult.UPDATED;
                if (fOldMatches)
                    {
                    result = aggregator.retract(entry);
                    }
                if (result == InvocableMap.StreamingAggregator.RetractionResult.REBUILD_REQUIRED)
                    {
                    m_status = Status.DIRTY;
                    return m_status;
                    }

                if (fNewMatches)
                    {
                    aggregator.accumulate(entry);
                    }

                result = aggregator.getMaintenanceStatus();
                }

            m_status = result == InvocableMap.StreamingAggregator.RetractionResult.UPDATED
                    ? Status.READY
                    : result == InvocableMap.StreamingAggregator.RetractionResult.STALE_BOUND
                      ? Status.STALE_BOUND : Status.DIRTY;
            return m_status;
            }
        catch (Throwable t)
            {
            m_failure = t;
            m_status  = Status.DIRTY;
            return m_status;
            }
        }

    /**
     * Return the exact partial result.
     *
     * @return the exact partial result
     *
     * @throws IllegalStateException if this state is not ready
     */
    public synchronized Object getPartialResult()
        {
        if (m_status != Status.READY || m_aggregator == null)
            {
            throw new IllegalStateException("continuous aggregation state is " + m_status);
            }
        return m_aggregator.getPartialResult();
        }

    /**
     * Return this state's certified stale bound.
     *
     * @return the stale bound in partial-result format
     *
     * @throws IllegalStateException if this state does not contain a bound
     */
    public synchronized Object getBound()
        {
        if (m_status != Status.STALE_BOUND
                || !(m_aggregator instanceof ContinuousAggregationBound))
            {
            throw new IllegalStateException("continuous aggregation state does not contain a bound");
            }
        return ((ContinuousAggregationBound) m_aggregator).getContinuousAggregationBound();
        }

    // ----- checkpoint support -------------------------------------------

    /**
     * Return a detached snapshot of this exact aggregation state.
     *
     * @return the snapshot, or {@code null} if this state cannot be
     *         checkpointed exactly
     */
    public synchronized Object snapshotState()
        {
        InvocableMap.StreamingAggregator aggregator = m_aggregator;
        return m_status == Status.READY && aggregator != null
                && aggregator.isStateCheckpointable()
               ? aggregator.snapshotState()
               : null;
        }

    /**
     * Restore an exact aggregation state from a checkpoint.
     *
     * @param definition  the continuous aggregation definition
     * @param oState      the checkpointed aggregator state
     *
     * @return {@code true} if the checkpoint was restored; {@code false} if
     *         it was rejected and a partition rebuild is required
     */
    @SuppressWarnings("rawtypes")
    public synchronized boolean restoreState(ContinuousAggregationDefinition definition,
                                             Object oState)
        {
        try
            {
            InvocableMap.StreamingAggregator aggregator = definition.getAggregator().supply();
            if (!aggregator.isStateCheckpointable())
                {
                return false;
                }

            aggregator.restoreState(oState);
            if (aggregator.getMaintenanceStatus()
                    != InvocableMap.StreamingAggregator.RetractionResult.UPDATED)
                {
                return false;
                }

            m_aggregator = aggregator;
            m_failure    = null;
            m_status     = Status.READY;
            m_fBuildScheduled.set(false);
            return true;
            }
        catch (Throwable t)
            {
            m_aggregator = null;
            m_failure    = t;
            m_status     = Status.BUILDING;
            m_fBuildScheduled.set(false);
            return false;
            }
        }

    // ----- inner class: Status -------------------------------------------

    /**
     * The lifecycle status of a partition aggregation state.
     */
    public enum Status
        {
        /** The initial or replacement state is being built. */
        BUILDING,

        /** The state contains an exact partition result. */
        READY,

        /** The state contains a certified bound but is not exact. */
        STALE_BOUND,

        /** The state cannot be queried until it is rebuilt. */
        DIRTY
        }

    // ----- data members ---------------------------------------------------

    /**
     * The current long-lived aggregation instance.
     */
    private InvocableMap.StreamingAggregator m_aggregator;

    /**
     * The current lifecycle status.
     */
    private volatile Status m_status = Status.BUILDING;

    /**
     * Whether a build has already been scheduled.
     */
    private final AtomicBoolean m_fBuildScheduled = new AtomicBoolean();

    /**
     * The most recent maintenance failure.
     */
    private transient Throwable m_failure;
    }
