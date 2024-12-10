/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util.fsm;

/**
 * A marker interface for {@link Instruction}s to {@link FiniteStateMachine}s.
 *
 * @author Brian Oliver
 * @since Coherence 12.2.1
 */
public interface Instruction
    {
    /**
     * The {@link Instruction} for a {@link FiniteStateMachine} to do nothing.
     */
    public final static Instruction NOTHING = new Instruction()
        {
        };

    /**
     * The {@link Instruction} for a {@link FiniteStateMachine} to stop.
     */
    public final static Instruction STOP = new Instruction()
        {
        };

    // ----- inner class ProcessEvent ---------------------------------------

    /**
     * An {@link Instruction} for a {@link FiniteStateMachine} to process an {@link Event}.
     * <p>
     * (immediately on the thread that created the {@link Instruction}).
     */
    public final class ProcessEvent<S extends Enum<S>>
            implements Instruction
        {
        /**
         * Constructs a {@link ProcessEvent} {@link Instruction}.
         *
         * @param event  the {@link Event} to process
         */
        public ProcessEvent(Event<S> event)
            {
            m_event = event;
            }

        /**
         * Obtain the {@link Event} to process.
         *
         * @return the {@link Event} to process
         */
        public Event<S> getEvent()
            {
            return m_event;
            }

        /**
         * The {@link Event} to process.
         */
        private Event<S> m_event;
        }

    // ----- inner class TransitionTo ---------------------------------------

    /**
     * An {@link Instruction} for a {@link FiniteStateMachine} to {@link
     * TransitionTo} another state. (immediately on the thread that created
     * the {@link Instruction}).
     */
    public final class TransitionTo<S extends Enum<S>>
            implements Instruction, Event<S>
        {
        /**
         * Constructs a {@link TransitionTo}.
         *
         * @param desiredState  the desired state to which to transition
         */
        public TransitionTo(S desiredState)
            {
            f_desiredState = desiredState;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public S getDesiredState(S currentState, ExecutionContext context)
            {
            return f_desiredState;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString()
            {
            return "TransitionTo{" + f_desiredState + '}';
            }

        /**
         * The desired state.
         */
        private final S f_desiredState;
        }
    }
