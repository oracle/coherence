/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util.fsm;

import java.util.EnumSet;

/**
 * A {@link Transition} represents a transition in a {@link FiniteStateMachine} from one
 * or more possible states to a desired state.
 *
 * @param <S>  the type of the state of the {@link FiniteStateMachine}
 *
 * @author Brian Oliver
 * @since Coherence 12.2.1
 */
public class Transition<S extends Enum<S>>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a {@link Transition} (without an {@link TransitionAction}).
     *
     * @param sName       the name of the {@link Transition} (used for display
     *                    and logging purposes)
     * @param statesFrom  the set of states from which this {@link Transition} may occur
     * @param stateTo     the state once the {@link TransitionAction} has been
     *                    successfully performed
     */
    public Transition(String sName, EnumSet<S> statesFrom, S stateTo)
        {
        this(sName, statesFrom, stateTo, /* no action */ null);
        }

    /**
     * Constructs a {@link Transition} (without an {@link TransitionAction}).
     *
     * @param sName      the name of the {@link Transition} (used for display
     *                   and logging purposes)
     * @param stateFrom  the state from which this {@link Transition} may occur
     * @param stateTo    the state once the {@link TransitionAction} has been
     *                   successfully performed
     */
    public Transition(String sName, S stateFrom, S stateTo)
        {
        this(sName, stateFrom, stateTo, /* no action */ null);
        }

    /**
     * Constructs a {@link Transition}.
     *
     * @param sName       the name of the {@link Transition} (used for display
     *                    and logging purposes)
     * @param statesFrom  the set of states from which this {@link Transition} may occur
     * @param stateTo     the state once the {@link TransitionAction} has been
     *                    successfully performed
     * @param action      the {@link TransitionAction} to be perform for the
     *                    {@link Transition}
     */
    public Transition(String sName, EnumSet<S> statesFrom, S stateTo, TransitionAction<S> action)
        {
        m_sName      = sName;
        m_statesFrom = statesFrom;
        m_stateTo    = stateTo;
        m_action     = action;
        }

    /**
     * Constructs a {@link Transition}.
     *
     * @param sName      the name of the {@link Transition} (used for display and logging purposes)
     * @param stateFrom  the state from which this {@link Transition} may occur
     * @param stateTo    the state once the {@link TransitionAction} has been successfully performed
     * @param action     the {@link TransitionAction} to be perform for the {@link Transition}
     */
    public Transition(String sName, S stateFrom, S stateTo, TransitionAction<S> action)
        {
        m_sName      = sName;
        m_statesFrom = EnumSet.of(stateFrom);
        m_stateTo    = stateTo;
        m_action     = action;
        }

    // ----- Transition methods ---------------------------------------------

    /**
     * Obtains the name of the {@link Transition}.
     *
     * @return the name of the {@link Transition}
     */
    public String getName()
        {
        return m_sName;
        }

    /**
     * Determines if the specified state is one of the possible starting
     * states for the {@link Transition}.
     *
     * @param state  the state to check
     *
     * @return <code>true</code> if the specified state is one of the
     *         starting states for the {@link Transition}
     */
    public boolean isStartingState(S state)
        {
        return m_statesFrom.contains(state);
        }

    /**
     * Obtains the {@link TransitionAction} to be performed for the {@link Transition}.
     *
     * @return the {@link TransitionAction} for the transition
     */
    public TransitionAction<S> getAction()
        {
        return m_action;
        }

    /**
     * Obtains the state to which the {@link FiniteStateMachine} will be in
     * once this {@link Transition} has occurred.
     *
     * @return the state of the {@link FiniteStateMachine} after this {@link
     *         Transition} has occurred
     */
    public S getEndingState()
        {
        return m_stateTo;
        }

    // ----- Object methods -------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
        {
        return String.format("Transition{name=%s, from=%s, to=%s, action=%s}",
                    m_sName, m_statesFrom, m_stateTo, m_action);
        }

    // ----- data members ---------------------------------------------------

    /**
     * The name of the {@link Transition}.
     * <p>
     * This is used for display and logging purposes.
     */
    private final String m_sName;

    /**
     * The set of states from which this {@link Transition} may occur.
     */
    private final EnumSet<S> m_statesFrom;

    /**
     * The state of the {@link FiniteStateMachine} once the {@link TransitionAction}
     * has been successfully performed.
     */
    private final S m_stateTo;

    /**
     * The {@link TransitionAction} to be performed for the {@link Transition}.
     * <p>
     * This may be <code>null</code> if no action is to be performed.
     */
    private final TransitionAction<S> m_action;
    }
