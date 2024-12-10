/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util.fsm.misc;

import com.tangosol.util.fsm.Event;
import com.tangosol.util.fsm.ExecutionContext;

/**
 * The {@link Event}s that can occur on a {@link Motor}.
 *
 * @author Brian Oliver
 */
public enum MotorEvent
        implements Event<Motor>
    {
    /**
     * This event will the transition the motor to {@link Motor#RUNNING}.
     */
    TURN_ON(Motor.RUNNING),

    /**
     * This event will the transition the motor to {@link Motor#RUNNING}.
     */
    TURN_OFF(Motor.STOPPED);

    /**
     * Constructs a {@link MotorEvent}.
     *
     * @param desiredState
     */
    private MotorEvent(Motor desiredState)
        {
        m_desiredState = desiredState;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public Motor getDesiredState(Motor currentState,
            ExecutionContext context)
        {
        return m_desiredState;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The desired state after the event is processed
     */
    private Motor m_desiredState;
    }
