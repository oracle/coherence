/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util.fsm;

/**
 * Provides contextual information about a {@link FiniteStateMachine},
 * typically to aid in runtime decision making for actions
 * (eg: {@link TransitionAction}s, {@link StateEntryAction}s and/or
 * {@link StateExitAction}s) and {@link Event}s.
 *
 * @author Brian Oliver
 * @since Coherence 12.2.1
 */
public interface ExecutionContext
    {
    /**
     * Obtains the name of the {@link FiniteStateMachine} that produced the
     * {@link ExecutionContext}.
     *
     * @return  the name of the {@link FiniteStateMachine}
     */
    public String getName();


    /**
     * Obtains the number of successful transitions that have occurred on the
     * {@link FiniteStateMachine} thus far.
     *
     * @return  the number of transitions that have occurred on the
     *          {@link FiniteStateMachine}
     */
    public long getTransitionCount();
    }
