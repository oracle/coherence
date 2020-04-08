/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util.fsm;

/**
 * A {@link FiniteStateMachine} implements a general purpose finite-state-machine.
 *
 * @param <S>  the type of state of the FiniteStateMachine
 *
 * @see Model
 * @see Transition
 * @see TransitionAction
 * @see StateEntryAction
 *
 * @author Brian Oliver
 * @since Coherence 12.2.1
 */
public interface FiniteStateMachine<S extends Enum<S>>
    {
    /**
     * Obtains the name of the {@link FiniteStateMachine}.  This is primarily used for
     * display/logging/monitoring purposes.
     *
     * @return the name of the {@link FiniteStateMachine}
     */
    public String getName();

    /**
     * Request the {@link FiniteStateMachine} to process the specified {@link Event}.
     * <p>
     * Note: There's no guarantee that the {@link Event} will be processed because:
     * <ol>
     *    <li>the {@link Transition} to be performed for the {@link Event}
     *        is invalid as the {@link FiniteStateMachine} is not in the required
     *        starting state.
     *    <li>the {@link FiniteStateMachine} may have been stopped.
     * </ol>
     *
     * @param event  the {@link Event} for the {@link FiniteStateMachine} to
     *               process
     */
    public void process(Event<S> event);

    /**
     * Obtains the current state of the {@link FiniteStateMachine}.
     * <p>
     * Note: After returning the current state there's no guarantee that the state will be the same
     * because a {@link Transition} may be executing asynchronously in the background on another thread.
     *
     * @return  the current state of the {@link FiniteStateMachine}
     */
    public S getState();

    /**
     * Obtains the number of transitions that have occurred in the {@link FiniteStateMachine}.
     * <p>
     * Note: After returning the count there's no guarantee that the count will be the same on the next request
     * because a {@link Transition} may be executing asynchronously in the background on another thread.
     *
     * @return  the number of transitions that have occurred in the {@link FiniteStateMachine}
     */
    public long getTransitionCount();

    /**
     * Start the {@link FiniteStateMachine} and enter the initial state.
     * <p>
     * Note: Once stopped a {@link FiniteStateMachine} can't be restarted; instead a
     * new {@link FiniteStateMachine} should be created.
     *
     * @return <code>true</code> if the start was successful or the FiniteStateMachine is already stated,
     * <code>false</code> if it has been stopped
     *
     * @throws IllegalStateException  if the FiniteStateMachine was already stopped
     */
    public boolean start();

    /**
     * Stops the {@link FiniteStateMachine} as soon as possible.
     * <p>
     * Note: Once stopped a {@link FiniteStateMachine} can't be restarted; instead a new
     * {@link FiniteStateMachine} should be created.
     *
     * @return <code>true</code> if the stop was successful, <code>false</code> if it's already stopped
     *
     * @throws IllegalStateException  if the FiniteStateMachine was never started
     */
    public boolean stop();

    /**
     * Add a {@link FiniteStateMachineListener} to the {@link FiniteStateMachine}.
     * <p>
     * Note that unique instances of FiniteStateMachineListener are identified
     * via their {@code equals} and {@code hashCode} implementations.
     *
     * @param listener  the listener to be added
     */
    public void addListener(FiniteStateMachineListener<S> listener);

    /**
     * Remove a {@link FiniteStateMachineListener} from the {@link FiniteStateMachine}.
     * <p>
     * Note that unique instances of FiniteStateMachineListener are identified
     * via their {@code equals} and {@code hashCode} implementations.
     *
     * @param listener  the listener to be removed
     */
    public void removeListener(FiniteStateMachineListener<S> listener);
    }
