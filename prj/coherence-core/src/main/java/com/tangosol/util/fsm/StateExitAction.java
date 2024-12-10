/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util.fsm;

/**
 * A {@link StateExitAction} encapsulates the actions to be performed by
 * a {@link FiniteStateMachine} when leaving a known state.
 * <p>
 * A {@link StateExitAction} for a particular state will be executed
 * prior to a {@link FiniteStateMachine} entering a new state.
 *
 * @param <S>  the type of the state
 *
 * @author Brian Oliver
 * @since Coherence 12.2.1
 */
public interface StateExitAction<S extends Enum<S>>
    {
    /**
     * Performs the necessary actions when a {@link FiniteStateMachine}
     * exits a particular state.
     *
     * @param state    the state of the {@link FiniteStateMachine} prior to
     *                 changing state
     * @param event    the {@link Event} that triggered the action
     * @param context  the {@link ExecutionContext} for the action
     */
    public void onExitState(S state,
                            Event<S> event,
                            ExecutionContext context);
    }
