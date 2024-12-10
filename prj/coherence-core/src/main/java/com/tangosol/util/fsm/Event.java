/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util.fsm;

/**
 * An {@link Event} captures the information that may trigger a {@link Transition}
 * in a {@link FiniteStateMachine} from one state to another.
 *
 * @param <S>  the type of the state of the {@link FiniteStateMachine}
 *
 * @author Brian Oliver
 * @since Coherence 12.2.1
 */
public interface Event<S extends Enum<S>>
    {
    /**
     * Determines the desired state of the {@link FiniteStateMachine} for the
     * {@link Event} given the current state of the {@link FiniteStateMachine}.
     *
     * @param state    the current state of the {@link FiniteStateMachine}
     * @param context  the {@link ExecutionContext} for the {@link Event}
     *
     * @return  the desired state of the {@link FiniteStateMachine} or
     *          <code>null</code> if no transition is required
     */
    public S getDesiredState(S state, ExecutionContext context);
    }
