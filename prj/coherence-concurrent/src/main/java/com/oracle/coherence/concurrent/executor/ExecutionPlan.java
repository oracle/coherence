/*
 * Copyright (c) 2016, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.executor;

import com.tangosol.io.ExternalizableLite;

import java.util.Iterator;

import java.util.concurrent.Executor;

import java.util.function.Predicate;

/**
 * Defines proposed {@link ExecutionPlan.Action}s for assigning a {@link Task} to
 * zero or more {@link Executor}s for execution.
 *
 * @author bo
 * @since 21.12
 */
public interface ExecutionPlan
        extends ExternalizableLite
    {
    /**
     * Obtains the proposed {@link ExecutionPlan.Action} for the specified
     * {@link Executor}.
     *
     * @param sId  the {@link Executor} id
     *
     * @return an {@link ExecutionPlan.Action} or <code>null</code> if no
     *         {@link ExecutionPlan.Action} has been defined
     */
    ExecutionPlan.Action getAction(String sId);

    /**
     * Sets the proposed {@link ExecutionPlan.Action} for the specified
     * {@link Executor}.
     *
     * @param sId     the {@link Executor} id
     * @param action  the {@link ExecutionPlan.Action} to set to
     *
     * @return a boolean to indicate if the {@link ExecutionPlan} was changed
     *         as a result of setting an action
     */
    boolean setAction(String sId, ExecutionPlan.Action action);

    /**
     * Obtains the number of {@link ExecutionPlan.Action}s for the
     * {@link ExecutionPlan}.
     *
     * @return the number of {@link ExecutionPlan.Action}s
     */
    int size();

    /**
     * Obtains the unique identities of {@link Executor}s with
     * {@link ExecutionPlan.Action}s.
     *
     * @return the {@link Executor} identities.
     */
    Iterator<String> getIds();

    /**
     * Determines the number of a {@link Executor}s whose
     * {@link ExecutionPlan.Action}s satisfy the specified {@link Predicate} in
     * the {@link ExecutionPlan}.
     *
     * @param predicate  the {@link Predicate} to evaluate the
     *                   {@link ExecutionPlan.Action}
     *
     * @return the number of {@link Executor}s that satisfy the specified
     * {@link Predicate}
     */
    int count(Predicate<? super ExecutionPlan.Action> predicate);

    /**
     * Determines if the {@link ExecutionPlan} will satisfy the {@link ExecutionStrategy}
     * conditions, including assigning the required number of {@link Executor}s to a
     * {@link Task} for execution.
     *
     * @return <code>true</code> when the {@link ExecutionPlan} satisfies the
     *         {@link ExecutionStrategy} that defined it <code>false</code> otherwise
     */
    boolean isSatisfied();

    /**
     * Determines the current number of effective assignments of a {@link Task} to
     * {@link Executor}s that failed (for some reason), which could be re-assigned
     * as a {@link Action#RECOVER} action.
     *
     * @return the pending recovery count
     */
    int getPendingRecoveryCount();

    /**
     * Optimizes the {@link ExecutionPlan}, returning if there where any changes caused
     * by the optimization.
     *
     * @return <code>true</code>  if the {@link ExecutionPlan} was changed during
     *         optimization, <code>false</code> otherwise
     */
    boolean optimize();

    // ----- enum: Action ---------------------------------------------------

    /**
     * A proposed {@link ExecutionPlan.Action} for an {@link Executor}.
     */
    enum Action
        {
        /**
         * Indicates an {@link Executor} should be assigned a {@link Task} to execute.
         */
        ASSIGN,

        /**
         * Indicates an {@link Executor} should be assigned a {@link Task} to execute,
         * that was previously assigned to another {@link Executor}.
         */
        RECOVER,

        /**
         * Indicates an {@link Executor} has completed its own execution of the
         * {@link Task}.
         */
        COMPLETED,

        /**
         * Indicates a different {@link Executor} should be assigned to execute
         * the {@link Task}.
         */
        REASSIGN,

        /**
         * Indicates an {@link Executor} should be released from executing a {@link Task},
         * especially if it was previously assigned the {@link Task}.
         */
        RELEASE;

        /**
         * Determine if the {@link Action} effectively assigns (or recovers) a
         * {@link Task} to an {@link Executor}.
         *
         * @return <code>true</code> if the {@link Action} is {@link #ASSIGN} or
         *         {@link #RECOVER} or {@link #COMPLETED} <code>false</code> otherwise
         */
        public boolean isEffectivelyAssigned()
            {
            return this == ASSIGN || this == RECOVER || this == COMPLETED;
            }
        }
    }
