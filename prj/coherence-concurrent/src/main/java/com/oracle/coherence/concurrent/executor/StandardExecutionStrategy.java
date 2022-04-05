/*
 * Copyright (c) 2016, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.executor;

import com.oracle.coherence.concurrent.executor.internal.ExecutorTrace;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.ExternalizableHelper;

import com.tangosol.util.function.Remote.Predicate;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadLocalRandom;

/**
 * An {@link ExecutionStrategy} that creates {@link ExecutionPlan}s for executing a
 * {@link Task} on a number of available {@link Executor}s that satisfy a specified
 * {@link Predicate}.
 *
 * @author bo
 * @since 21.12
 */
public class StandardExecutionStrategy
        implements ExecutionStrategy, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a {@link StandardExecutionStrategy} (required for serialization).
     */
    @SuppressWarnings("unused")
    public StandardExecutionStrategy()
        {
        }

    /**
     * Constructs a {@link StandardExecutionStrategy}.
     *
     * @param cDesiredExecutors     the number of {@link Executor}s to execute the
     *                              {@link Task} (-1 means all available)
     * @param predicate             the {@link Predicate} to determine if an
     *                              {@link Executor} is a candidate for executing a
     *                              {@link Task} based on the
     *                              {@link TaskExecutorService.ExecutorInfo}
     * @param fConcurrentExecution  should the produced {@link ExecutionPlan} assign the
     *                              {@link Task} to {@link Executor}s concurrently?
     */
    public StandardExecutionStrategy(final int cDesiredExecutors,
            final Predicate<? super TaskExecutorService.ExecutorInfo> predicate, final boolean fConcurrentExecution)
        {
        m_cDesiredExecutors    = cDesiredExecutors;
        m_predicate            = predicate;
        m_fPerformConcurrently = fConcurrentExecution;
        }

    // ----- ExecutionStrategy ----------------------------------------------

    @Override
    public ExecutionPlan analyze(ExecutionPlan currentPlan,
            Map<String, ? extends TaskExecutorService.ExecutorInfo> mapExecutorInfo,
            EnumSet<EvaluationRationale> rationales)
        {
        // we'll be randomly choosing execution services
        ThreadLocalRandom random = ThreadLocalRandom.current();

        // establish a map of candidate executors based on ExecutorInfo
        HashMap<String, TaskExecutorService.ExecutorInfo> mapCandidates = new HashMap<>();

        for (TaskExecutorService.ExecutorInfo info : mapExecutorInfo.values())
            {
            // candidates must satisfy the predicate
            if (m_predicate.test(info))
                {
                mapCandidates.put(info.getId(), info);
                }
            }

        // remember the number of candidates
        int cCandidateCount = mapCandidates.size();

        ExecutorTrace.log(() -> String.format("Executor candidates [%s]; current desired count [%s]",
                                              mapCandidates, m_cDesiredExecutors));

        // the new plan will be based on the current plan
        MutableExecutionPlan newPlan = new MutableExecutionPlan(currentPlan);

        // determine the current recovery count
        int cPendingRecoveries = newPlan.getPendingRecoveryCount();

        ExecutorTrace.log(() -> String.format("Recovery count [%s]", newPlan.getPendingRecoveryCount()));

        // remove executors from the candidate map that are already ASSIGNed in the plan,
        // remove executors from the candidate map that are REASSIGNed and
        // remove executors from the plan if they are no longer candidates,
        for (Iterator<String> iterator = newPlan.getIds(); iterator.hasNext(); )
            {
            String sExecutorId = iterator.next();

            if (newPlan.getAction(sExecutorId).isEffectivelyAssigned() && mapCandidates.containsKey(sExecutorId))
                {
                // the executor is already in the plan, so it can't be a
                // candidate for adding to the plan again!
                mapCandidates.remove(sExecutorId);
                }
            else if (newPlan.contains(sExecutorId) && !mapCandidates.containsKey(sExecutorId)
                     || newPlan.getAction(sExecutorId) == ExecutionPlan.Action.REASSIGN)
                {
                // the executor is in the plan, but it's no longer a candidate
                // or the executor is REASSIGNed
                // let's release it
                newPlan.release(sExecutorId);

                // At this point we know that the task was assigned and is about to be
                // marked for release from an executor, but it may still need to be assigned to a
                // different executor ie: an assignment from here on (for this one) should be considered "recovered".
                cPendingRecoveries++;

                // if the executor is a candidate, remove it.
                mapCandidates.remove(sExecutorId);
                }
            }

        // determine how many executors we currently have effectively assigned
        int cEffectivelyAssigned = newPlan.count(ExecutionPlan.Action::isEffectivelyAssigned);

        ExecutorTrace.log(() -> String.format("Current effective assignment count [%s]",
                                              newPlan.count(ExecutionPlan.Action::isEffectivelyAssigned)));

        // determine how many executors are desired
        // (which is either all candidates or the specified number)
        int cDesired = m_cDesiredExecutors < 0 ? cCandidateCount : m_cDesiredExecutors;

        ExecutorTrace.log(() -> String.format("Desired executor count (calculated) [%s]", cDesired));

        // determine how many additional executors we need (based on the concurrency)
        // (assume none)
        int cExtra = 0;

        if (m_fPerformConcurrently)
            {
            // for concurrent execution we need as many we're missing
            // (which is the number desired minus the number we've already got assigned)
            cExtra = Math.max(0, cDesired - cEffectivelyAssigned);
            }
        else
            {
            // for sequential execution we will attempt to include a new candidate when;
            // we don't have enough already and either;
            // a). the task is first created, or
            // b). when a result has been provided
            if (cEffectivelyAssigned < cDesired
                && (rationales.contains(EvaluationRationale.TASK_CREATED)
                    || rationales.contains(EvaluationRationale.TASK_RESULT_PROVIDED)))
                {
                // we choose 1 when we currently don't have enough
                cExtra = 1;
                }
            }

        int cExtraFinal = cExtra;
        ExecutorTrace.log(() -> String.format("Additional executor required count [%s]", cExtraFinal));

        // determine how many remaining candidates there are to choose from
        int cRemaining = mapCandidates.size();

        ExecutorTrace.log(() -> String.format("Remaining executor candidates [%s]", mapCandidates));

        // randomly choose the required number from the candidates from those remaining
        while (cRemaining > 0 && cExtra > 0)
            {
            for (Iterator<String> iterator = mapCandidates.keySet().iterator();
                 iterator.hasNext() && cRemaining > 0 && cExtra > 0; )
                {
                String sExecutorId = iterator.next();

                if (cRemaining == cExtra || random.nextBoolean())
                    {
                    if (cPendingRecoveries > 0)
                        {
                        newPlan.recover(sExecutorId);
                        cPendingRecoveries--;
                        }
                    else
                        {
                        newPlan.assign(sExecutorId);
                        }

                    cExtra--;
                    cRemaining--;
                    }
                }
            }

        // re-determine the number of effectively assigned executors
        cEffectivelyAssigned = newPlan.count(ExecutionPlan.Action::isEffectivelyAssigned);

        ExecutorTrace.log(() -> String.format("Effective candidate count [%s]", newPlan.count(ExecutionPlan.Action::isEffectivelyAssigned)));

        // the plan is only satisfied when the number of effectively assigned executors reaches the required number
        newPlan.setSatisfied(cEffectivelyAssigned == cDesired && cEffectivelyAssigned > 0);

        // remember the remaining number of pending recoveries
        newPlan.setPendingRecoveryCount(Math.max(cPendingRecoveries, 0));

        return newPlan;
        }

    // ----- ExternalizableLite interface -------------------------------

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        m_cDesiredExecutors    = ExternalizableHelper.readInt(in);
        m_predicate            = ExternalizableHelper.readObject(in);
        m_fPerformConcurrently = in.readBoolean();
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        ExternalizableHelper.writeInt(out, m_cDesiredExecutors);
        ExternalizableHelper.writeObject(out, m_predicate);
        out.writeBoolean(m_fPerformConcurrently);
        }

    // ----- PortableObject interface ---------------------------------------

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        m_cDesiredExecutors    = in.readInt(0);
        m_predicate            = in.readObject(1);
        m_fPerformConcurrently = in.readBoolean(2);
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeInt(0,     m_cDesiredExecutors);
        out.writeObject(1,  m_predicate);
        out.writeBoolean(2, m_fPerformConcurrently);
        }

    // ----- data members ---------------------------------------------------

    /**
     * The number of {@link Executor}s to choose to execute a {@link Task}.
     */
    protected int m_cDesiredExecutors;

    /**
     * The {@link Predicate} that must be satisfied by {@link Executor} to be a candidate
     * for executing a {@link Task}.
     */
    protected Predicate<? super TaskExecutorService.ExecutorInfo> m_predicate;

    /**
     * Should the {@link ExecutionPlan} schedule the {@link Task} for execution
     * concurrently or sequentially?  The default is concurrently.
     */
    protected boolean m_fPerformConcurrently;
    }
