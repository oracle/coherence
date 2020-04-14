/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util.fsm;

/**
 * A {@link StateEntryAction} encapsulates the actions to be performed by
 * a {@link FiniteStateMachine} when a particular state is entered.
 * <p>
 * A {@link StateEntryAction} for a particular state will be executed
 * when the {@link FiniteStateMachine} has been set (to the said state) or a
 * {@link Transition} (to the said state) has successfully completed.
 *
 * @param <S>  the type of the state
 *
 * @author Brian Oliver
 * @since Coherence 12.2.1
 */
public interface StateEntryAction<S extends Enum<S>>
    {
    /**
     * Performs the necessary actions when a {@link FiniteStateMachine}
     * enters a particular state.  Once finished, the returned
     * {@link Instruction} is performed.
     *
     * @param exitingState   the previous state of the {@link FiniteStateMachine}
     *                       prior to the state change (may be <code>null</code>
     *                       if there was no previous state)
     * @param enteringState  the new state of the {@link FiniteStateMachine}
     * @param event          the {@link Event} that triggered the action
     * @param context        the {@link ExecutionContext} for the action
     *
     * @return  the {@link Instruction} to be performed by the {@link FiniteStateMachine}
     */
    public Instruction onEnterState(S exitingState,
                                    S enteringState,
                                    Event<S> event,
                                    ExecutionContext context);
    }
