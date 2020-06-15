/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util.fsm;

/**
 * A {@link LifecycleAwareEvent} is a specialized {@link Event} that
 * receives callbacks during the processing of the {@link Event} by
 * a {@link FiniteStateMachine}.
 *
 * @param <S>  the type of state of a {@link FiniteStateMachine}
 *
 * @author Brian Oliver
 * @since Coherence 12.2.1
 */
public interface LifecycleAwareEvent<S extends Enum<S>> extends Event<S>
    {
    /**
     * Called by a {@link FiniteStateMachine} when the {@link LifecycleAwareEvent}
     * is initially about to be accepted for processing.
     *
     * @param context  the {@link ExecutionContext} for the {@link Event}
     *
     * @return <code>true</code> if the {@link Event} should be accepted, or
     *         <code>false</code> if the {@link FiniteStateMachine} should
     *         ignore the {@link LifecycleAwareEvent}
     */
    public boolean onAccept(ExecutionContext context);

    /**
     * Called by a {@link FiniteStateMachine} when the {@link LifecycleAwareEvent}
     * is about to be processed.
     *
     * @param context  the {@link ExecutionContext} for the {@link Event}
     */
    public void onProcessing(ExecutionContext context);

    /**
     * Called by a {@link FiniteStateMachine} when the {@link LifecycleAwareEvent}
     * has been processed.
     *
     * @param context  the {@link ExecutionContext} for the {@link Event}
     */
    public void onProcessed(ExecutionContext context);
    }
