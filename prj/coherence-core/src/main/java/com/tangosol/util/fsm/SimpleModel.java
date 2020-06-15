/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util.fsm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * A {@link SimpleModel} is a basic implementation of a {@link Model} for a {@link FiniteStateMachine}.
 *
 * @param <S>  the type of state
 *
 * @author Brian Oliver
 */
public class SimpleModel<S extends Enum<S>>
        implements Model<S>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a {@link SimpleModel} given an Enum of the possible states
     * of a {@link FiniteStateMachine}.
     *
     * @param stateClass  the Enum class containing the possible states
     */
    public SimpleModel(Class<S> stateClass)
        {
        m_stateClass           = stateClass;
        m_states               = stateClass.getEnumConstants();
        m_transitions          = new ArrayList<Transition<S>>();
        m_mapStateEntryActions = new HashMap<S, StateEntryAction<S>>();
        m_mapStateExitActions  = new HashMap<S, StateExitAction<S>>();
        }

    // ----- Model interface ------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<S> getStateClass()
        {
        return m_stateClass;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public S[] getStates()
        {
        return m_states;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<S, StateEntryAction<S>> getStateEntryActions()
        {
        return m_mapStateEntryActions;
        }

    /**
     * Adds/overrides the {@link StateEntryAction} for the specified state in
     * the {@link Model}.
     *
     * @param state             the state
     * @param stateEntryAction  the {@link StateEntryAction} for the state
     */
    public void addStateEntryAction(S state, StateEntryAction<S> stateEntryAction)
        {
        m_mapStateEntryActions.put(state, stateEntryAction);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<S, StateExitAction<S>> getStateExitActions()
        {
        return m_mapStateExitActions;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterable<Transition<S>> getTransitions()
        {
        return m_transitions;
        }

    // ----- SimpleModel methods --------------------------------------------

    /**
     * Obtains the state with the specified name from the {@link SimpleModel}.
     *
     * @param sName  the name of the state to obtain
     *
     * @return the state with the specified name or <code>null</code> if no
     *         such state is known by the {@link SimpleModel}
     */
    public S getState(String sName)
        {
        sName = sName.trim().toUpperCase();

        for (S state : m_states)
            {
            if (state.name().toUpperCase().equals(sName))
                {
                return state;
                }
            }

        return null;
        }

    /**
     * Adds/overrides the {@link StateExitAction} for the specified state in the {@link Model}.
     *
     * @param state            the state
     * @param stateExitAction  the {@link StateExitAction} for the state
     */
    public void addStateExitAction(S state, StateExitAction<S> stateExitAction)
        {
        m_mapStateExitActions.put(state, stateExitAction);
        }

    /**
     * Adds the specified {@link Transition} to the {@link Model}.
     *
     * @param transition the {@link Transition} to add
     */
    public void addTransition(Transition<S> transition)
        {
        m_transitions.add(transition);
        }

    // ----- data members ---------------------------------------------------

    /**
     * The {@link Class} of the state of the {@link Model}.
     */
    private Class<S> m_stateClass;

    /**
     * The array of possible states of a {@link FiniteStateMachine}.
     */
    private S[] m_states;

    /**
     * A list of possible transitions for the {@link FiniteStateMachine}.
     */
    private ArrayList<Transition<S>> m_transitions;

    /**
     * A map of states to their {@link StateEntryAction}.
     */
    private HashMap<S, StateEntryAction<S>> m_mapStateEntryActions;

    /**
     * A map of states to their {@link StateExitAction}.
     */
    private HashMap<S, StateExitAction<S>> m_mapStateExitActions;
    }
