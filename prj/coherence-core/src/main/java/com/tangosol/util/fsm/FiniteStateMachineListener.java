/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util.fsm;

/**
 * A FiniteStateMachineListener listens for state related events on a {@link FiniteStateMachine}.
 *
 * @author pfm  2013.04.17
 * @since Coherence 12.2.1
 */
public interface FiniteStateMachineListener<S extends Enum<S>>
    {
    /**
     * Called during a state transition immediately before a {@link FiniteStateMachine}
     * enters a particular state.
     *
     * @param stateFrom  the state that the FiniteStateMachine is leaving
     * @param stateTo    the state that the FiniteStateMachine is entering
     */
    public void onTransition(S stateFrom, S stateTo);
    }
