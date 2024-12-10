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
 * A {@link SwitchEvent} describes the events that may occur on a light switch
 *
 * @author Brian Oliver
 */
public enum SwitchEvent implements Event<Light>
    {
    /**
     * This event will the transition the light to {@link Light#ON}.
     */
    SWITCH_ON(Light.ON),

    /**
     * This event will the transition the light to {@link Light#OFF}.
     */
    SWITCH_OFF(Light.OFF),

    /**
     * This event will the transition the light to  {@link Light#BROKEN}.
     */
    TOGGLE_TOO_FAST(Light.BROKEN);

    /**
     * Constructs a {@link SwitchEvent}
     *
     * @param desiredState the desired {@link Light} state after the event
     */
    private SwitchEvent(Light desiredState)
        {
        m_desiredState = desiredState;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public Light getDesiredState(Light currentState,
            ExecutionContext context)
        {
        return m_desiredState;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The desired {@link Light} state after the {@link Event}.
     */
    private Light m_desiredState;
    }

