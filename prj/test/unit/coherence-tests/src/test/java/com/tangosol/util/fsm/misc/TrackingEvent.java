/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util.fsm.misc;

import com.tangosol.util.fsm.Event;
import com.tangosol.util.fsm.ExecutionContext;
import com.tangosol.util.fsm.LifecycleAwareEvent;

/**
 * A {@link TrackingEvent} is a {@link LifecycleAwareEvent} that tracks
 * the processing of another {@link Event}.
 *
 * @param <S>
 *
 * @author Brian Oliver
 */
public class TrackingEvent<S extends Enum<S>> implements LifecycleAwareEvent<S>
    {
    /**
     * Constructs a {@link TrackingEvent}.
     *
     * @param event  the {@link Event} to be tracked
     */
    public TrackingEvent(Event<S> event)
        {
        m_event        = event;
        m_wasAccepted  = false;
        m_wasEvaluated = false;
        m_wasProcessed = false;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public S getDesiredState(S                currentState,
            ExecutionContext context)
        {
        m_wasEvaluated = true;

        return m_event.getDesiredState(currentState, context);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onAccept(ExecutionContext context)
        {
        if (m_event instanceof LifecycleAwareEvent)
            {
            m_wasAccepted = ((LifecycleAwareEvent<S>) m_event).onAccept(context);
            }
        else
            {
            // non-lifecycle aware events are always accepted
            m_wasAccepted = true;
            }

        return m_wasAccepted;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onProcessed(ExecutionContext context)
        {
        m_wasProcessed = true;

        if (m_event instanceof LifecycleAwareEvent)
            {
            ((LifecycleAwareEvent<S>) m_event).onProcessed(context);
            }
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onProcessing(ExecutionContext context)
        {
        if (m_event instanceof LifecycleAwareEvent)
            {
            ((LifecycleAwareEvent<S>) m_event).onProcessing(context);
            }
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
        {
        return String.format("TrackingEvent{%s}", m_event);
        }

    /**
     * Determines if the tracked {@link Event} was accepted for processing.
     *
     * @return if the {@link Event} was accepted for processing
     */
    public boolean accepted()
        {
        return m_wasAccepted;
        }

    /**
     * Determines if the {@link #getDesiredState(Enum, ExecutionContext)} was called
     * for the tracked {@link Event}.
     *
     * @return if {@link #getDesiredState(Enum, ExecutionContext)} was called for the
     *         tracked {@link Event}
     */
    public boolean evaluated()
        {
        return m_wasEvaluated;
        }


    /**
     * Determines if the tracked {@link Event} was processed.
     *
     * @return if the tracked {@link Event} was processed
     */
    public boolean processed()
        {
        return m_wasProcessed;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The {@link Event} that will be tracked.
     */
    private Event<S> m_event;

    /**
     * A flag to indicate if the {@link Event} was accepted.
     */
    private boolean m_wasAccepted;

    /**
     * A flag to indicate if the {@link Event} was evaluated (with
     * {@link #getDesiredState(Enum, ExecutionContext)}.
     */
    private boolean m_wasEvaluated;

    /**
     * A flag to indicate if the {@link Event} was processed
     * (with {@link #getDesiredState(Enum, ExecutionContext)}).
     */
    private boolean m_wasProcessed;
    }
