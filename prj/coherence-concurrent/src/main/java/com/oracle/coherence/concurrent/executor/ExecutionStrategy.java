/*
 * Copyright (c) 2016, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.executor;

import com.tangosol.io.ExternalizableLite;

import java.util.EnumSet;
import java.util.Map;

import java.util.concurrent.Executor;

/**
 * Analyzes current runtime information to define an {@link ExecutionPlan} for executing a {@link Task} with available
 * {@link Executor}s.
 *
 * @author bo
 * @since 21.12
 */
public interface ExecutionStrategy
        extends ExternalizableLite
    {
    /**
     * Analyzes the current {@link ExecutionPlan} for a {@link Task}, together with the
     * {@link TaskExecutorService.ExecutorInfo} to produce a new {@link ExecutionPlan}.
     *
     * @param currentPlan      the current {@link ExecutionPlan} (<code>null</code>
     *                         when undefined)
     * @param mapExecutorInfo  a read-only {@link Map} of the current
     *                         {@link TaskExecutorService.ExecutorInfo}, one for each
     *                         registered {@link Executor}, keyed by the {@link Executor}
     *                         id
     * @param rationales       the {@link ExecutionStrategy.EvaluationRationale}s for
     *                         evaluating the {@link ExecutionStrategy}
     *
     * @return new {@link ExecutionPlan}
     */
    ExecutionPlan analyze(ExecutionPlan currentPlan,
            Map<String, ? extends TaskExecutorService.ExecutorInfo> mapExecutorInfo,
            EnumSet<ExecutionStrategy.EvaluationRationale> rationales);

    // ----- enum: EvaluationRationale --------------------------------------

    /**
     * A rationale for evaluating an {@link ExecutionStrategy}.
     */
    enum EvaluationRationale
        {
        /**
         * A {@link Task} was created and submitted for orchestration.
         */
        TASK_CREATED,

        /**
         * A {@link Task} was recovered.
         */
        TASK_RECOVERED,

        /**
         * A {@link Result} was provided or updated.
         */
        TASK_RESULT_PROVIDED,

        /**
         * The available {@link Executor}s have changed.
         */
        EXECUTOR_SERVICES_CHANGED
        }
}
