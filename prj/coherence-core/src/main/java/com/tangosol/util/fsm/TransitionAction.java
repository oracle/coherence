/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util.fsm;

/**
 * A {@link TransitionAction} encapsulates the actions to be performed as part
 * of the {@link Transition} from one state to another.
 *
 * @see Transition
 *
 * @author Brian Oliver
 * @since Coherence 12.2.1
 */
public interface TransitionAction<S extends Enum<S>>
    {
    /**
     * Performs the necessary actions as part of a {@link Transition} from one
     * state to another, triggered by a specified {@link Event}.
     * <p>
     * Note: After executing this method the {@link FiniteStateMachine} will
     * automatically move to the state specified by the {@link Transition}.
     *
     * @param sName      the name of the transition
     * @param stateFrom  the state from which the transition is occurring
     * @param stateTo    the state to which the transition is occurring
     * @param event      the {@link Event} that triggered the {@link Transition}
     * @param context    the {@link ExecutionContext} for the action
     *
     * @throws RollbackTransitionException  if the {@link Transition} should be
     *                                      aborted and the {@link FiniteStateMachine}
     *                                      should be left in it's current state
     */
    public void onTransition(String           sName,
                             S                stateFrom,
                             S                stateTo,
                             Event<S>         event,
                             ExecutionContext context) throws RollbackTransitionException;
    }
