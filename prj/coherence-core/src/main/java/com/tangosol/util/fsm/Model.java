/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util.fsm;

import java.util.Map;

/**
 * A {@link Model} represents the definition of a {@link FiniteStateMachine},
 * the set of known states, {@link Transition}s between said states and
 * {@link StateEntryAction}s / {@link StateExitAction}s to be performed when
 * said states are changed.
 *
 * @param <S>  the type of state of the {@link FiniteStateMachine}
 *
 * @author Brian Oliver
 * @since Coherence 12.2.1
 */
public interface Model<S extends Enum<S>>
    {
    /**
     * Obtains the {@link Transition}s defined by the {@link Model}.
     *
     * @return the {@link Transition}s defined by the {@link Model}
     */
    public Iterable<Transition<S>> getTransitions();

    /**
     * Obtains the {@link Class} of the state of the {@link Model}.
     *
     * @return the {@link Class} of the state
     */
    public Class<S> getStateClass();

    /**
     * Obtains the valid states defined by the {@link Model}.
     *
     * @return the valid states of the {@link Model}
     */
    public S[] getStates();

    /**
     * Obtains the {@link StateEntryAction}s defined for the states in the {@link Model}.
     *
     * @return the defined {@link StateEntryAction}s defined for the states in the {@link Model}
     */
    public Map<S, StateEntryAction<S>> getStateEntryActions();

    /**
     * Obtains the {@link StateExitAction}s defined for the states in the {@link Model}.
     *
     * @return the defined {@link StateExitAction}s defined for the states in the {@link Model}
     */
    public Map<S, StateExitAction<S>> getStateExitActions();
    }
