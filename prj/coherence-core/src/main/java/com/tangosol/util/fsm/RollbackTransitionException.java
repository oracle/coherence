/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util.fsm;

/**
 * A {@link RollbackTransitionException} may be thrown during an {@link TransitionAction}
 * for a {@link Transition} if the said {@link Transition} should be aborted.  ie: no state
 * change should occur.
 *
 * @author Brian Oliver
 * @since Coherence 12.2.1
 */
@SuppressWarnings("serial")
public class RollbackTransitionException extends Exception
    {
    /**
     * Constructs a {@link RollbackTransitionException} with the specified from and to states.
     *
     * @param stateFrom  the state from which the {@link Transition} was being made
     * @param stateTo    the state to which the {@link Transition} was being made
     * @param sReason    the reason for the rollback
     */
    public RollbackTransitionException(Enum<?> stateFrom, Enum<?> stateTo, String sReason)
        {
        super(String.format("Rollback of transition from %s to %s occurred. %s", stateFrom, stateTo, sReason));

        m_stateFrom = stateFrom;
        m_stateTo   = stateTo;
        }

    /**
     * Constructs a {@link RollbackTransitionException} with the specified from and to states.
     *
     * @param stateFrom  the state from which the {@link Transition} was being made
     * @param stateTo    the state to which the {@link Transition} was being made
     * @param cause      the underlying exception that caused the rollback
     */
    public RollbackTransitionException(Enum<?> stateFrom, Enum<?> stateTo, Throwable cause)
        {
        super(String.format("Rollback of transition from %s to %s occured due to an exception.",
                stateFrom, stateTo), cause);

        m_stateFrom = stateFrom;
        m_stateTo   = stateTo;
        }

    /**
     * Obtain the state from which the {@link Transition} was rolled back.
     *
     * @return the state from which the {@link Transition} was rolled back
     */
    public Enum<?> getStateFrom()
        {
        return m_stateFrom;
        }

    /**
     * Obtain the state to which the {@link Transition} did not occur.
     *
     * @return the state to which the {@link Transition} did not occur
     */
    public Enum<?> getStateTo()
        {
        return m_stateTo;
        }

    /**
     * The state from which the {@link Transition} was being made.
     */
    private Enum<?> m_stateFrom;

    /**
     * The start to which the {@link Transition} was being made.
     */
    private Enum<?> m_stateTo;
    }
