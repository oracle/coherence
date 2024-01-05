/*
 * Copyright (c) 2016, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.executor;

import com.oracle.coherence.common.base.Logger;

import com.oracle.coherence.concurrent.executor.TaskExecutorService.ExecutorInfo;

import com.oracle.coherence.concurrent.executor.internal.Cause;
import com.oracle.coherence.concurrent.executor.internal.ExecutorTrace;
import com.oracle.coherence.concurrent.executor.internal.LiveObject;

import com.oracle.coherence.concurrent.executor.options.Debugging;

import com.oracle.coherence.concurrent.executor.processors.LocalOnlyProcessor;

import com.oracle.coherence.concurrent.executor.util.Caches;
import com.oracle.coherence.concurrent.executor.util.OptionsByType;

import com.tangosol.internal.tracing.Span;
import com.tangosol.internal.tracing.SpanContext;
import com.tangosol.internal.tracing.TracingHelper;

import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.net.CacheService;
import com.tangosol.net.NamedCache;

import com.tangosol.util.Base;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.Extractors;
import com.tangosol.util.Filter;
import com.tangosol.util.Filters;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.InvocableMap.Entry;

import com.tangosol.util.filter.EqualsFilter;
import com.tangosol.util.filter.KeyAssociatedFilter;
import com.tangosol.util.filter.PresentFilter;

import com.tangosol.util.function.Remote.Predicate;

import com.tangosol.util.processor.ConditionalRemove;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.time.Duration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import java.util.concurrent.Executor;

import java.util.function.BiConsumer;

/**
 * Manages the definition and current state of an individual {@link Task} being
 * orchestrated by an {@link TaskExecutorService}.
 *
 * @param <T>  the {@link Task} type
 * @param <A>  the mutable accumulation type of the reduction operation by
 *             the {@link Task.Collector}
 * @param <R>  the result type of the reduction operation performed by
 *             the {@link Task.Collector}
 *
 * @author bo
 * @since 21.12
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class ClusteredTaskManager<T, A, R>
        implements ExternalizableLite, LiveObject, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs an {@link ClusteredTaskManager} (required for serialization).
     */
    @SuppressWarnings("unused")
    public ClusteredTaskManager()
        {
        }

    /**
     * Constructs a {@link ClusteredTaskManager} for the specified {@link Task}.
     *
     * @param sTaskId              the unique identity of the {@link Task} being managed
     * @param task                 the {@link Task} to manage
     * @param executionStrategy    the {@link ExecutionStrategy}
     * @param collector            the {@link Task.Collector} for results
     * @param completionPredicate  the {@link Predicate} to determine if a {@link Task} is complete
     * @param completionRunnable   the {@link Task.CompletionRunnable} to call when a {@link Task} is complete
     * @param retainDuration       the {@link Duration} to retain a {@link Task} after it is complete
     * @param optionsByType        the {@link OptionsByType} to be used
     */
    @SuppressWarnings("unchecked")
    public ClusteredTaskManager(String sTaskId, Task<T> task, ExecutionStrategy executionStrategy,
                                Task.Collector<? super T, A, R> collector, Predicate<? super R> completionPredicate,
                                Task.CompletionRunnable<? super R> completionRunnable, Duration retainDuration,
                                OptionsByType<Task.Option> optionsByType)
        {
        m_sTaskId                = sTaskId;
        m_task                   = task;
        m_executionStrategy      = executionStrategy;
        m_collector              = collector == null ? null : (Task.Collector<T, A, R>) collector;
        m_completionPredicate    = completionPredicate;
        m_completionRunnable     = completionRunnable;
        m_fRunCompletionRunnable = completionRunnable != null;
        m_retainDuration         = retainDuration;

        // a value less than zero means that debug logging, unless overriden by the user,
        // will only be available if the system property coherence.executor.trace.logging
        // is true and coherence.log.level is set to seven or higher
        m_debugging = optionsByType.get(Debugging.class, Debugging.of(Integer.MIN_VALUE));

        m_lastResult     = Result.none();
        m_nResultVersion = 0;
        m_executionPlan  = null;

        // NOTE: initialized to one to ensure the ExecutionStrategy is initially evaluated
        m_cPendingExecutionStrategyUpdateCount   = 1;
        m_cPendingExecutionPlanOptimizationCount = 0;

        if (collector != null)
            {
            // we only keep a list if there is a collector
            m_listResults = new ArrayList<>();
            }

        m_lCurrentResultGeneration   = 0;
        m_lProcessedResultGeneration = 0;

        m_fCancelled = false;
        m_fCompleted = false;
        m_state      = State.ORCHESTRATED;

        Span parentSpan = TracingHelper.getActiveSpan();
        if (parentSpan != null)
            {
            m_parentSpanContext = parentSpan.getContext();
            }
        }

    // ----- public methods -------------------------------------------------

    /**
     * Returns a {@link ComposableContinuation} to invoke based on the current state.
     * This may return {@code null} if no continuation is needed.
     *
     * @param service  the associated {@link CacheService}
     * @param entry    the cache {@link Entry} for the task being processed
     * @param cause    the {@link Cause} triggered an update
     *
     * @return a {@link ComposableContinuation} to invoke based on the current state or
     *         {@code null} if no continuation is needed
     */
    public ComposableContinuation onProcess(final CacheService service,
                                            final Entry        entry,
                                            final Cause        cause)
        {
        String sTaskId = getTaskId();

        ExecutorTrace.entering(ClusteredTaskManager.class, "onProcess", service, sTaskId, entry, cause, m_state);

        ComposableContinuation continuation = null;

        switch (m_state)
            {
            case ORCHESTRATED:
                continuation = new AsyncProcessChangesContinuation(service, (String) entry.getKey(), cause);

                break;

            case TERMINATING:

                continuation = new CleanupContinuation(service, (String) entry.getKey());

                break;
            }

        ExecutorTrace.exiting(ClusteredTaskManager.class, "onProcess", sTaskId, continuation);

        return continuation;
        }

    /**
     * Determines if an {@link Executor} has been allocated the {@link Task}.
     *
     * @param executorId  the {@link Executor} ID
     *
     * @return <code>true</code> if the {@link Executor} with the specified identifier has been allocated
     *         the {@link Task}, <code>false</code> otherwise.
     */
    public boolean isOwner(String executorId)
        {
        ExecutionPlan.Action action = m_executionPlan.getAction(executorId);

        return action != null && action.isEffectivelyAssigned();
        }

    // ----- accessors ------------------------------------------------------

    /**
     * Obtains the unique identity of the {@link Task}.
     *
     * @return the unique identity of the {@link Task}
     */
    public String getTaskId()
        {
        return m_sTaskId;
        }

    /**
     * Obtains the state of the {@link Task}.
     *
     * @return the state of the {@link Task}
     */
    public State getState()
        {
        return m_state;
        }

    /**
     * Sets the state of the {@link Task}.
     *
     * @param state  the state to set to
     */
    public void setState(State state)
        {
        m_state = state;
        }

    /**
     * Obtains the flag to indicate if we need to run the {@link Task.CompletionRunnable}.
     *
     * @return the flag to indicate if we need to run the {@link Task.CompletionRunnable}.
     */
    public boolean getRunCompletionRunnable()
        {
        return m_fRunCompletionRunnable;
        }

    /**
     * Sets the flag to indicate if we need to run the {@link Task.CompletionRunnable}.
     *
     * @param value  the flag to indicate if we need to run the {@link Task.CompletionRunnable}
     */
    public void setRunCompletionRunnable(boolean value)
        {
        m_fRunCompletionRunnable = value;
        }

    /**
     * Obtains the {@link Task.CompletionRunnable}.
     *
     * @return the {@link Task.CompletionRunnable}
     */
    public Task.CompletionRunnable<? super R> getCompletionRunnable()
        {
        return m_completionRunnable;
        }

    /**
     * Obtains the {@link Duration} to retain the {@link Task} after it is complete.
     *
     * @return the {@link Duration}
     */
    public Duration getRetainDuration()
        {
        return m_retainDuration;
        }

    /**
     * Obtains the {@link Debugging} option of the {@link Task}.
     *
     * @return the {@link Debugging} option
     */
    public Debugging getDebugging()
        {
        return m_debugging;
        }

    /**
     * Asynchronously processes the changes that have recently occurred on the {@link ClusteredTaskManager}.
     *
     * @param service  the {@link CacheService} that owns the {@link ClusteredTaskManager} state
     * @param key      the {@link NamedCache} key for the {@link ClusteredTaskManager}
     * @param cause    the underlying event {@link Cause} that triggered the processing
     */
    public void asyncProcessChanges(CacheService service,
                                    String       key,
                                    Cause        cause)
        {
        ExecutorTrace.entering(ClusteredTaskManager.class, "asyncProcessChanges", service, key, this, cause);

        long          nNewResultCount = m_lCurrentResultGeneration - m_lProcessedResultGeneration;
        Debugging     debug           = m_debugging;
        boolean       fWillLog        = Logger.isEnabled(debug.getLogLevel());
        StringBuilder bldrDebug       = fWillLog ? new StringBuilder(256) : null;

        if (fWillLog)
            {
            bldrDebug.append("\n------------------------------------\n");
            bldrDebug.append("Task                               : ").append(key).append(EOL);
            bldrDebug.append("State                              : ").append(m_state).append(EOL);
            bldrDebug.append("Completed ?                        : ").append(m_fCompleted).append(EOL);
            bldrDebug.append("Cancelled ?                        : ").append(m_fCancelled).append(EOL);
            bldrDebug.append("Last Result                        : ").append(m_lastResult).append(EOL);
            bldrDebug.append("Result Version                     : ").append(m_nResultVersion).append(EOL);
            bldrDebug.append("Pending Results from Executors     : ").append(nNewResultCount).append(EOL);
            bldrDebug.append("Last Processed Generation          : ").append(m_lProcessedResultGeneration).append(EOL);
            bldrDebug.append("Total Results from Executors       : ").append(m_lCurrentResultGeneration).append(EOL);
            bldrDebug.append("Pending Execution Strategy Updates : ").append(m_cPendingExecutionStrategyUpdateCount).append(EOL);
            bldrDebug.append("Pending Execution Plan Updates     : ").append(m_cPendingExecutionPlanOptimizationCount).append(EOL);
            bldrDebug.append("Execution Plan                     : ").append(m_executionPlan).append(EOL);
            }

        // assume the task result won't be changed
        boolean fResultChanged = false;

        // remember the original result
        Result<R> originalResult = m_lastResult;

        // assume the task execution plan won't be changed
        boolean fExecutionPlanChanged = false;

        // remember the original pending update counts
        int originalPendingExecutionStrategyUpdateCount = m_cPendingExecutionStrategyUpdateCount;

        // determine the rationales for evaluating the ExecutionStrategy
        EnumSet<ExecutionStrategy.EvaluationRationale> rationales =
            EnumSet.noneOf(ExecutionStrategy.EvaluationRationale.class);

        if (m_executionPlan == null)
            {
            rationales.add(ExecutionStrategy.EvaluationRationale.TASK_CREATED);
            }

        if (m_cPendingExecutionStrategyUpdateCount > 0)
            {
            rationales.add(ExecutionStrategy.EvaluationRationale.EXECUTOR_SERVICES_CHANGED);
            }

        if (nNewResultCount > 0)
            {
            rationales.add(ExecutionStrategy.EvaluationRationale.TASK_RESULT_PROVIDED);
            }

        if (cause == Cause.PARTITIONING)
            {
            rationales.add(ExecutionStrategy.EvaluationRationale.TASK_RECOVERED);
            }

        if (fWillLog)
            {
            bldrDebug.append("Evaluation Rationales              : ").append(rationales).append(EOL);
            }

        // only process changes if the task is running, not completed, not cancelled, and there are updates to process
        boolean fEvaluatedResult   = false;
        boolean fEvaluatedStrategy = false;

        if (m_state == State.ORCHESTRATED
            && !m_fCompleted
            && !m_fCancelled
            && (nNewResultCount > 0 || m_cPendingExecutionStrategyUpdateCount > 0))
            {

            // only evaluate the result if there have been updates to the resultMap
            if (nNewResultCount > 0)
                {
                fEvaluatedResult = true;
                fResultChanged    = asyncEvaluateResult(originalResult);
                }

            // only evaluate the execution strategy if we're not completed and there have been updates
            if (!m_fCompleted
                && !m_fCancelled
                && (nNewResultCount > 0 || originalPendingExecutionStrategyUpdateCount > 0))
                {
                fEvaluatedStrategy   = true;
                fExecutionPlanChanged = asyncEvaluateExecutionStrategy(service, rationales);
                }
            }

        if (fWillLog)
            {
            bldrDebug.append("(re) Evaluated Task Result?        : ").append(fEvaluatedResult).append(EOL);
            bldrDebug.append("Result Changed?                    : ").append(fResultChanged).append(EOL);
            bldrDebug.append("(re) Evaluated Execution Strategy? : ").append(fEvaluatedStrategy).append(EOL);
            bldrDebug.append("Execution Plan Changed?            : ").append(fExecutionPlanChanged).append(EOL);
            }

        // prepare the update to the task
        ChainedProcessor chain = ChainedProcessor.empty();

        if (fResultChanged || nNewResultCount > 0)
            {
            // update the collected result when the result was changed or pending count changed
            chain.andThen(new UpdateCollectedResultProcessor<>(m_lastResult,
                                                               m_lCurrentResultGeneration,
                                                               m_fCompleted));
            }

        if (fExecutionPlanChanged)
            {
            if (fWillLog)
                {
                bldrDebug.append("Updated Execution Plan             : ").append(m_executionPlan)
                        .append(EOL).append(EOL);
                }

            chain.andThen(new UpdateExecutionPlanProcessor(
                    m_executionPlan, originalPendingExecutionStrategyUpdateCount));
            }

        // assume the task being updated is local
        boolean isLocal = true;

        // only update if we have processors to execute
        if (!chain.isEmpty())
            {
            if (fWillLog)
                {
                bldrDebug.append("\nUpdating Task ...").append(EOL);
                }

            Result result = (Result) Caches.tasks(service).invoke(m_sTaskId, LocalOnlyProcessor.of(chain));

            // is the task local?
            isLocal = result.isPresent();
            }

        // we'll only perform updates if the task is still local, or we're recovering (which means we're local)
        if (isLocal || cause == Cause.PARTITIONING)
            {
            // ensure the executors are updated with the current execution plan (when we haven't yet completed)
            if (!m_fCompleted && !m_fCancelled && m_cPendingExecutionPlanOptimizationCount > 0)
                {
                if (fWillLog)
                    {
                    bldrDebug.append("Commenced Assigning and Optimizing Execution Plan using [")
                            .append(m_executionPlan).append(']').append(EOL);
                    }

                // register the task assignments with the executors based on the execution plan
                ClusteredAssignment.registerAssignments(m_sTaskId, m_executionPlan, service);

                if (fWillLog)
                    {
                    bldrDebug.append("Optimizing Execution Plan ...").append(EOL);
                    }

                // optimize the execution plan (to remove RELEASE actions).
                fExecutionPlanChanged = m_executionPlan.optimize();

                if (fWillLog)
                    {
                    bldrDebug.append("Execution Plan ")
                            .append(fExecutionPlanChanged
                                        ? "was optimized"
                                        : "did not require optimization").append(EOL);
                    }

                // use an EntryProcessor update the execution plan and count (that it's now completed)
               Caches.tasks(service).invoke(m_sTaskId, LocalOnlyProcessor.of(
                       new OptimizeExecutionPlanProcessor(m_executionPlan, m_cPendingExecutionPlanOptimizationCount)));

                if (fWillLog)
                    {
                    bldrDebug.append("Completed Assigning and Optimizing Execution Plan").append(EOL);
                    }
                }
            else
                {
                if (fWillLog)
                    {
                    bldrDebug.append("Skipping Optimization of Execution Plan as it is completed,")
                            .append(" cancelled or has no pending optimizations to perform")
                            .append(EOL);
                    }
                }
            }
        else
            {
            if (fWillLog)
                {
                bldrDebug.append("Abandoned Performing Updates as the task ")
                        .append("is no longer local").append(EOL);
                }
            }

        if (fWillLog)
            {
            bldrDebug.append("------------------------------------");
            ExecutorTrace.log(bldrDebug.toString(), debug);
            }

        ExecutorTrace.exiting(ClusteredTaskManager.class, "asyncProcessChanges", null, this);
        }

    // ----- accessors ------------------------------------------------------

    /**
     * Obtains the {@link Task} to be executed.
     *
     * @return the {@link Task}
     */
    public Task<T> getTask()
        {
        return m_task;
        }

    /**
     * Determines if the {@link Task} has completed execution and can now be removed.
     *
     * @return <code>true</code> if the {@link Task} has completed execution,
     *         <code>false</code> otherwise
     */
    public boolean isCompleted()
        {
        return m_fCompleted;
        }

    /**
     * Determines if the {@link Task} has been cancelled.
     *
     * @return <code>true</code> if the {@link Task} has been cancelled,
     *         <code>false</code> otherwise
     */
    public boolean isCancelled()
        {
        return m_fCancelled;
        }

    /**
     * Returns true if the {@link Task} completed. Completion may be due to normal
     * termination, an exception, or cancellation -- in all of these cases, this method
     * will return true.
     *
     * @return <code>true</code> the associated {@link Task} has is done
     *         <code>false</code> otherwise
     */
    public boolean isDone()
        {
        return m_fCompleted || m_fCancelled || m_state == State.TERMINATING;
        }

    /**
     * Get the last collected {@link Result}.
     *
     * @return the last collected {@link Result}
     */
    public Result<R> getLastResult()
        {
        return m_lastResult;
        }

    /**
     * Get the version number of the latest result.
     *
     * @return the version number of the latest result
     */
    public int getResultVersion()
        {
        return m_nResultVersion;
        }

    /**
     * Updates the {@link Result} for the specified {@link Executor}.
     *
     * @param result  the {@link Result} for the {@link Executor}
     */
    public void setResult(Result<T> result)
        {
        if (m_collector == null)
            {
            m_nResultVersion++;
            m_lastResult = (Result<R>) result; // R and T are the same when no collector
            }
        else
            {
            m_listResults.add(result);
            }

        // increase the result update count since our last result evaluation
        m_lCurrentResultGeneration++;
        }

    /**
     * Gets the partition ID.
     *
     * @return the partition ID.
     */
    public int getPartitionId()
        {
        return m_nPartitionId;
        }

    /**
     * Sets the {@link Task} partition ID.
     *
     * @param nPartitionId  the {@link Task} partition ID to set to
     */
    public void setPartitionId(int nPartitionId)
        {
        m_nPartitionId = nPartitionId;
        }

    /**
     * Gets the partition based {@link Task} sequence number.
     *
     * @return the partition based {@link Task} sequence number
     */
    public long getTaskSequence()
        {
        return m_lTaskSequence;
        }

    /**
     * Sets the partition based {@link Task} sequence number.
     *
     * @param sequence  the {@link Task} sequence number to set to
     */
    public void setTaskSequence(long sequence)
        {
        m_lTaskSequence = sequence;
        }

    // ----- ExternalizableLite interface -------------------------------

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        m_sTaskId                = ExternalizableHelper.readUTF(in);
        m_task                   = ExternalizableHelper.readObject(in);
        m_executionStrategy      = ExternalizableHelper.readObject(in);
        m_collector              = ExternalizableHelper.readObject(in);
        m_completionPredicate    = ExternalizableHelper.readObject(in);
        m_completionRunnable     = ExternalizableHelper.readObject(in);
        m_fRunCompletionRunnable = m_completionRunnable != null;
        long retainSeconds       = ExternalizableHelper.readLong(in);

        if (retainSeconds == -1L)
            {
            m_retainDuration = null;
            }
        else
            {
            m_retainDuration = Duration.ofSeconds(retainSeconds);
            }

        m_debugging      = ExternalizableHelper.readObject(in);
        m_lastResult     = ExternalizableHelper.readObject(in);
        m_nResultVersion = ExternalizableHelper.readInt(in);
        m_executionPlan  = ExternalizableHelper.readObject(in);

        m_cPendingExecutionStrategyUpdateCount   = ExternalizableHelper.readInt(in);
        m_cPendingExecutionPlanOptimizationCount = ExternalizableHelper.readInt(in);

        if (m_collector != null)
            {
            // we only keep a list if there is a collector
            m_listResults = new ArrayList<>();
            ExternalizableHelper.readCollection(in, m_listResults, null);
            }

        m_lCurrentResultGeneration   = ExternalizableHelper.readLong(in);
        m_lProcessedResultGeneration = ExternalizableHelper.readLong(in);

        m_fCancelled = in.readBoolean();
        m_fCompleted = in.readBoolean();
        m_state      = ExternalizableHelper.readObject(in);

        Map<String, String> wireTracingContext = new LinkedHashMap();
        ExternalizableHelper.readMap(in, wireTracingContext, Base.getContextClassLoader());
        m_parentSpanContext = wireTracingContext.isEmpty() ? SpanContext.Noop.INSTANCE : TracingHelper.getTracer().extract(wireTracingContext);
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        ExternalizableHelper.writeUTF(out, m_sTaskId);
        ExternalizableHelper.writeObject(out, m_task);
        ExternalizableHelper.writeObject(out, m_executionStrategy);
        ExternalizableHelper.writeObject(out, m_collector);
        ExternalizableHelper.writeObject(out, m_completionPredicate);
        ExternalizableHelper.writeObject(out, m_completionRunnable);
        ExternalizableHelper.writeLong(out, m_retainDuration == null ? -1L : m_retainDuration.getSeconds());
        ExternalizableHelper.writeObject(out, m_debugging);
        ExternalizableHelper.writeObject(out, m_lastResult);
        ExternalizableHelper.writeInt(out, m_nResultVersion);
        ExternalizableHelper.writeObject(out, m_executionPlan);
        ExternalizableHelper.writeInt(out, m_cPendingExecutionStrategyUpdateCount);
        ExternalizableHelper.writeInt(out, m_cPendingExecutionPlanOptimizationCount);

        if (m_collector != null)
            {
            ExternalizableHelper.writeCollection(out, m_listResults);
            }

        ExternalizableHelper.writeLong(out, m_lCurrentResultGeneration);
        ExternalizableHelper.writeLong(out, m_lProcessedResultGeneration);
        out.writeBoolean(m_fCancelled);
        out.writeBoolean(m_fCompleted);
        ExternalizableHelper.writeObject(out, m_state);

        Map<String, String> injectMap = TracingHelper.getTracer().inject(m_parentSpanContext);
        ExternalizableHelper.writeMap(out, injectMap == null ? Collections.emptyMap() : injectMap);
        }

    // ----- PortableObject interface ---------------------------------------

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        m_sTaskId                = in.readString(0);
        m_task                   = in.readObject(1);
        m_executionStrategy      = in.readObject(2);
        m_collector              = in.readObject(3);
        m_completionPredicate    = in.readObject(4);
        m_completionRunnable     = in.readObject(5);
        m_fRunCompletionRunnable = m_completionRunnable != null;
        long retainSeconds       = in.readLong(6);

        if (retainSeconds == -1L)
            {
            m_retainDuration = null;
            }
        else
            {
            m_retainDuration = Duration.ofSeconds(retainSeconds);
            }

        m_debugging      = in.readObject(7);
        m_lastResult     = in.readObject(8);
        m_nResultVersion = in.readInt(9);
        m_executionPlan  = in.readObject(10);

        m_cPendingExecutionStrategyUpdateCount   = in.readInt(11);
        m_cPendingExecutionPlanOptimizationCount = in.readInt(12);

        if (m_collector != null)
            {
            // we only keep a list if there is a collector
            m_listResults = new ArrayList<>();
            m_listResults = in.readCollection(13, m_listResults);
            }

        m_lCurrentResultGeneration   = in.readLong(14);
        m_lProcessedResultGeneration = in.readLong(15);

        m_fCancelled = in.readBoolean(16);
        m_fCompleted = in.readBoolean(17);
        m_state      = in.readObject(18);

        Map<String, String> wireTracingContext = new LinkedHashMap();
        in.readMap(19, wireTracingContext);
        m_parentSpanContext = TracingHelper.getTracer().extract(wireTracingContext);
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeString(0, m_sTaskId);
        out.writeObject(1, m_task);
        out.writeObject(2, m_executionStrategy);
        out.writeObject(3, m_collector);
        out.writeObject(4, m_completionPredicate);
        out.writeObject(5, m_completionRunnable);
        out.writeLong(6,   m_retainDuration == null ? -1L : m_retainDuration.getSeconds());
        out.writeObject(7, m_debugging);
        out.writeObject(8, m_lastResult);
        out.writeInt(9,    m_nResultVersion);

        out.writeObject(10, m_executionPlan);
        out.writeInt(11,    m_cPendingExecutionStrategyUpdateCount);
        out.writeInt(12,    m_cPendingExecutionPlanOptimizationCount);

        out.writeCollection(13, m_listResults);
        out.writeLong(14,       m_lCurrentResultGeneration);
        out.writeLong(15,       m_lProcessedResultGeneration);

        out.writeBoolean(16, m_fCancelled);
        out.writeBoolean(17, m_fCompleted);
        out.writeObject(18,  m_state);
        out.writeObject(19,  TracingHelper.getTracer().inject(m_parentSpanContext));
        }

    // ----- Object methods -------------------------------------------------

    @Override
    public String toString()
        {
        return "ClusteredTaskManager{" +
               "taskId='" + m_sTaskId + '\'' +
               ", resultVersion=" + m_nResultVersion +
               ", currentResultGeneration=" + m_lCurrentResultGeneration +
               ", processedResultGeneration=" + m_lProcessedResultGeneration +
               ", completed=" + m_fCompleted +
               ", cancelled=" + m_fCancelled +
               ", state=" + m_state +
               '}';
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Return the {@link SpanContext} of the span that enqueued this task.
     *
     * @return the {@link SpanContext} of the span that enqueued this task
     *
     * @since 22.06
     */
    protected SpanContext getParentSpanContext()
        {
        return m_parentSpanContext;
        }

    /**
     * Evaluate the result of the {@link Task} given the current {@link Result}s made by each of the assigned {@link
     * Executor}(s), returning if a result change occurred.
     *
     * @param originalResult  the original {@link Result} to evaluate
     *
     * @return <code>true</code> if a new result is available,
     *         <code>false</code> otherwise
     */
    protected boolean asyncEvaluateResult(Result<R> originalResult)
        {
        String sTaskId = getTaskId();

        ExecutorTrace.entering(ClusteredTaskManager.class, "asyncEvaluateResult", sTaskId, originalResult);

        Debugging debug = m_debugging;

        // assume there is no change in the result
        boolean resultChanged = false;

        if (m_fCompleted)
            {
            ExecutorTrace.log(() -> String.format("Skipping result collection for Task [%s] as it is completed", m_sTaskId),
                       debug);
            }
        else if (m_collector == null)
            {
            if (m_lastResult.isThrowable())
                {
                m_fCompleted = true;
                }
            else
                {
                resultChanged = true;
                }

            if (!m_fCompleted)
                {
                // check if execution is completed on all Executors
                if (m_executionPlan.isSatisfied())
                    {
                    boolean fCompleted = true;

                    for (Iterator<String> iter = m_executionPlan.getIds(); iter.hasNext() && fCompleted; )
                        {
                        String executorId = iter.next();

                        if (m_executionPlan.getAction(executorId) != ExecutionPlan.Action.COMPLETED)
                            {
                            fCompleted = false;
                            }
                        }
                    m_fCompleted = fCompleted;
                    if (m_fCompleted)
                        {
                        ExecutorTrace.log(() -> String.format("Task [%s] has completed on all assigned Executors", m_sTaskId),
                                   debug);
                        }
                    }
                }
            }
        else
            {
            ExecutorTrace.log(() -> String.format("Collecting result for Task [%s] using collector [%s]",
                                           m_sTaskId, m_collector), debug);

            // create a container for the result accumulation
            A                container   = m_collector.supplier().get();
            Predicate<A>     finishable  = m_collector.finishable();
            BiConsumer<A, T> accumulator = m_collector.accumulator();

            try
                {
                for (Iterator<Result<T>> iterator = m_listResults.iterator(); iterator.hasNext() && !finishable.test(container); )
                    {
                    Result<T> result = iterator.next();

                    if (result.isPresent())
                        {
                        // will throw here if the result is a Throwable
                        accumulator.accept(container, result.get());
                        }
                    }

                // use the finisher to produce the result
                Result<R> result = Result.of(m_collector.finisher().apply(container));

                ExecutorTrace.log(() -> String.format("Collected result [%s] for Task [%s]", result, m_sTaskId), debug);

                // a task can only be completed if the execution plan and the completion predicate
                // have been satisfied
                if (m_executionPlan.isSatisfied() && m_completionPredicate.test(result.get()) && !m_fCompleted)
                    {
                    m_fCompleted  = true;
                    m_lastResult  = result;
                    resultChanged = true;
                    }

                if (originalResult == null || !originalResult.equals(result))
                    {
                    m_lastResult = result;
                    resultChanged = true;
                    }
                else
                    {
                    ExecutorTrace.log(() -> String.format(
                            "Collected result [%s] for Task [%s] hasn't changed (no changes to publish)",
                            result, m_sTaskId),
                               debug);
                    }
                }
            catch (Throwable t)
                {
                // either Task execution threw, or the Collector did

                m_fCompleted = true;
                m_lastResult = Result.throwable(t);

                ExecutorTrace.log(() -> String.format("Task [%s] failed due to [%s]", m_sTaskId, t), debug);
                }
            }

        ExecutorTrace.exiting(ClusteredTaskManager.class, "asyncEvaluateResult", sTaskId, resultChanged);

        return resultChanged;
        }

    /**
     * Asynchronously evaluate the {@link ExecutionStrategy} for the {@link Task} and if necessary, update the {@link
     * ExecutionPlan} and {@link Result} map.
     *
     * @param service     the {@link CacheService} to use for accessing {@link ClusteredExecutorService} caches
     * @param rationales  the {@link ExecutionStrategy.EvaluationRationale}
     *
     * @return <code>true</code> if the {@link ExecutionPlan} was changed as part of evaluating,
     * <code>false</code> otherwise
     */
    protected boolean asyncEvaluateExecutionStrategy(CacheService service,
            EnumSet<ExecutionStrategy.EvaluationRationale> rationales)
        {
        Debugging debug = m_debugging;

        // assume there is no change in the ExecutionPlan
        boolean fExecutionPlanUpdated;

        ExecutorTrace.entering(ClusteredTaskManager.class, "asyncEvaluateExecutionStrategy", service, m_sTaskId, rationales);

        ExecutorTrace.log(() -> String.format("Evaluating the Execution Plan for Task [%s] due to [%s]",
                                       m_sTaskId, rationales), debug);

        try
            {
            // obtain the cache containing ExecutionInfo
            NamedCache<String, ExecutorInfo> executorInfoCache = Caches.executors(service);

            // filter-in only running executors
            Map<String, ExecutorInfo> executors =
                    executorInfoCache.invokeAll(RUNNING_EXECUTOR_FILTER, Entry::getValue);

            // determine the new assignments based on the current assignments and execution service information
            ExecutionPlan executionPlan =
                    m_executionStrategy.analyze(m_executionPlan, executors, rationales);

            // has the execution plan changed?
            fExecutionPlanUpdated = (executionPlan == null && m_executionPlan != null) ||
                                    (executionPlan != null && m_executionPlan == null) ||
                                    (executionPlan != null && !executionPlan.equals(m_executionPlan));

            if (ExecutorTrace.isEnabled())
                {
                ExecutorTrace.log(String.format("Current execution plan [%s]",    m_executionPlan));
                ExecutorTrace.log(String.format("Updated(?) execution plan [%s]", executionPlan));
                }

            if (fExecutionPlanUpdated)
                {
                // remember the updated plan
                m_executionPlan = executionPlan;

                ExecutorTrace.log(() -> String.format("Execution Plan for Task [%s] has changed.  Will be updated.",
                                               m_sTaskId), debug);

                // we can now optimize the execution plan
                m_cPendingExecutionPlanOptimizationCount++;
                }
            else
                {
                ExecutorTrace.log(() -> String.format("Execution Plan for Task [%s] was not changed.  Will not be updated.",
                                               m_sTaskId), debug);
                }
            }
        catch (Exception e)
            {
            ExecutorTrace.throwing(ClusteredTaskManager.class, "asyncEvaluateExecutionStrategy", e);

            throw Base.ensureRuntimeException(e);
            }

        // reset the pending execution strategy update as we've now evaluated the execution strategy
        m_cPendingExecutionStrategyUpdateCount = 0;

        ExecutorTrace.exiting(ClusteredTaskManager.class, "asyncEvaluateExecutionStrategy", m_sTaskId, fExecutionPlanUpdated);

        return fExecutionPlanUpdated;
        }

    /**
     * Cleanup metadata associated with the given task key.
     *
     * @param service  the associated {@link CacheService}
     * @param sKey     the task key
     */
    protected void cleanup(CacheService service, String sKey)
        {
        String sTaskId  = getTaskId();
        Duration retain = m_retainDuration;

        ExecutorTrace.entering(ClusteredTaskManager.class, "cleanup", service, sKey, sTaskId, retain);

        ClusteredAssignment.removeAssignments(sTaskId, service);

        if (retain == null)
            {
            Caches.tasks(service).remove(sKey);
            }
        else if (!Duration.ZERO.equals(retain))
            {
            Caches.tasks(service).invoke(sKey, new RetainProcessor(m_retainDuration.toMillis()));
            }

        // clean up the task properties
        cleanProperties(service);

        ExecutorTrace.exiting(ClusteredTaskManager.class, "cleanup", sTaskId);
        }

    /**
     * Clean up the task properties.
     *
     * @param service  the {@link CacheService}
     */
    protected void cleanProperties(CacheService service)
        {
        String sTaskId  = getTaskId();
        Filter filterAsc = new KeyAssociatedFilter(new EqualsFilter("getTaskId", sTaskId), sTaskId);

        ExecutorTrace.entering(ClusteredTaskManager.class, "cleanProperties", sTaskId);

        Caches.properties(service).invokeAll(filterAsc, new ConditionalRemove(PresentFilter.INSTANCE, false));

        ExecutorTrace.exiting(ClusteredTaskManager.class, "cleanProperties", sTaskId);
        }

    // ----- LiveObject interface --------------------------------------------

    /**
     * See {@link #onProcess(CacheService, Entry, Cause)}.
     *
     * @param service  {@inheritDoc}
     * @param entry    {@inheritDoc}
     * @param cause    {@inheritDoc}
     *
     * @return {@inheritDoc}
     *
     * @see #onProcess(CacheService, Entry, Cause)
     */
    @Override
    public ComposableContinuation onInserted(final CacheService service,
                                             final Entry        entry,
                                             final Cause        cause)
        {
        return onProcess(service, entry, cause);
        }

    /**
     * See {@link #onProcess(CacheService, Entry, Cause)}.
     *
     * @param service  {@inheritDoc}
     * @param entry    {@inheritDoc}
     * @param cause    {@inheritDoc}
     *
     * @return {@inheritDoc}
     *
     * @see #onProcess(CacheService, Entry, Cause)
     */
    @Override
    public ComposableContinuation onUpdated(final CacheService service,
                                            final Entry        entry,
                                            final Cause        cause)
        {
        return onProcess(service, entry, cause);
        }

    /**
     * Effectively, a no-op.
     *
     * @param service  {@inheritDoc}
     * @param entry    {@inheritDoc}
     * @param cause    {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public ComposableContinuation onDeleted(final CacheService       service,
                                            final InvocableMap.Entry entry,
                                            final Cause              cause)
        {
        return null;
        }

    // ----- inner class: AsyncProcessChangesContinuation -------------------

    /**
     * The {@link ComposableContinuation} to asynchronously process the changes to the
     * {@link ClusteredTaskManager}.
     */
    public class AsyncProcessChangesContinuation
            implements ComposableContinuation
        {
        // ----- constructors -----------------------------------------------

        /**
         * Constructs an {@link AsyncProcessChangesContinuation}.
         *
         * @param cacheService  the {@link CacheService}
         * @param sTaskId       the {@link Task} identity
         * @param cause         the {@link Cause} of the request to process changes
         */
        public AsyncProcessChangesContinuation(CacheService cacheService, String sTaskId, Cause cause)
            {
            f_cacheService = cacheService;
            f_sTaskId      = sTaskId;
            f_cause        = cause;
            f_sId          = Integer.toHexString(hashCode());
            }

        // ----- ComposableContinuation methods -----------------------------

        @Override
        public void proceed(Object o)
            {
            asyncProcessChanges(f_cacheService, f_sTaskId, f_cause);
            }

        @Override
        public ComposableContinuation compose(ComposableContinuation continuation)
            {
            // the offered continuation takes precedence over this continuation
            // (so we return that)
            return continuation;
            }

        // ----- Object methods ---------------------------------------------

        @Override
        public String toString()
            {
            return "AsyncProcessChangesContinuation{id=" + f_sId + ", taskId=" + f_sTaskId +  ", cause=" + f_cause + '}';
            }

        // ----- data members -----------------------------------------------

        /**
         * The {@link CacheService}.
         */
        protected final CacheService f_cacheService;

        /**
         * The task ID.
         */
        protected final String f_sTaskId;

        /**
         * The {@link ComposableContinuation} failure cause.
         */
        protected final Cause f_cause;

        /**
         * An internal identifier for debugging to allow picking continuations
         * different continuation instances.
         */
        protected final String f_sId;
        }

    // ----- inner class: ChainedProcessor ----------------------------------

    /**
     * A {@link ChainedProcessor} executes zero or more contained
     * {@link InvocableMap.EntryProcessor}s in sequence against a single
     * {@link InvocableMap.Entry} as a single transaction.
     */
    public static class ChainedProcessor
            extends PortableAbstractProcessor<String, ClusteredTaskManager, Void>
        {
        // ----- constructors -----------------------------------------------

        /**
         * Constructs an empty {@link ChainedProcessor}
         */
        public ChainedProcessor()
            {
            m_listProcessors = new ArrayList<>();
            }

        // ----- public methods ---------------------------------------------

        /**
         * Adds another {@link InvocableMap.EntryProcessor} to the {@link ChainedProcessor}.
         *
         * @param processor  the {@link InvocableMap.EntryProcessor} to add
         *
         * @return this {@link ChainedProcessor} to permit fluent-style method calls
         */
        public ChainedProcessor andThen(InvocableMap.EntryProcessor<String, ClusteredTaskManager, ?> processor)
            {
            m_listProcessors.add(processor);

            return this;
            }

        /**
         * Obtains an initially empty {@link ChainedProcessor}.
         *
         * @return an empty {@link ChainedProcessor}
         */
        public static ChainedProcessor empty()
            {
            return new ChainedProcessor();
            }

        /**
         * Determines if the {@link ChainedProcessor} is empty i.e., contains no {@link InvocableMap.EntryProcessor}s.
         *
         * @return <code>true</code> if the {@link ChainedProcessor} is empty,
         *         <code>false</code> otherwise
         */
        public boolean isEmpty()
            {
            return m_listProcessors.isEmpty();
            }

        // ----- EntryProcessor interface -----------------------------------

        @Override
        public Void process(InvocableMap.Entry<String, ClusteredTaskManager> entry)
            {
            for (InvocableMap.EntryProcessor processor : m_listProcessors)
                {
                processor.process(entry);
                }

            return null;
            }

        // ----- PortableObject interface -----------------------------------

        @Override
        public void readExternal(PofReader in) throws IOException
            {
            m_listProcessors = in.readCollection(0, m_listProcessors);
            }

        @Override
        public void writeExternal(PofWriter out) throws IOException
            {
            out.writeCollection(0, m_listProcessors);
            }

        // ----- data members -----------------------------------------------

        /**
         * {@link InvocableMap.EntryProcessor}s to be invoked in list-order.
         */
        protected ArrayList<InvocableMap.EntryProcessor> m_listProcessors;
        }

    // ----- inner class: RetainProcessor -----------------------------------

    /**
     * Handles updating the {@link ClusteredTaskManager} appropriately if
     * the task is being retained after execution.
     *
     * @since 22.06.7
     */
    public static class RetainProcessor
        extends PortableAbstractProcessor
        implements ExternalizableLite
        {
        // ----- constructors -----------------------------------------------

        /**
         * For serialization.
         */
        @SuppressWarnings("unused")
        public RetainProcessor()
            {
            }

        /**
         * Constructs a new {@code RetainProcessor} that will apply
         * the given expiry value to the processed entry.
         *
         * @param cMillis  the {@code time-to-live} for the entry.
         */
        public RetainProcessor(long cMillis)
            {
            m_cMillis = cMillis;
            }

        // ----- ExternalizableList interface -------------------------------

        @Override
        public void readExternal(DataInput in) throws IOException
            {
            m_cMillis = ExternalizableHelper.readLong(in);
            }

        @Override
        public void writeExternal(DataOutput out) throws IOException
            {
            ExternalizableHelper.writeLong(out, m_cMillis);
            }

        // ----- PortableAbstractProcessor methods --------------------------

        @Override
        public Object process(Entry entry)
            {
            if (entry.isPresent())
                {
                ClusteredTaskManager manager = (ClusteredTaskManager) entry.getValue();
                manager.m_retainDuration = Duration.ZERO;
                entry.setValue(manager, true);
                ((BinaryEntry) entry).expire(m_cMillis);
                }

            return null;
            }

        @Override
        public void readExternal(PofReader in) throws IOException
            {
            m_cMillis = in.readLong(0);
            }

        @Override
        public void writeExternal(PofWriter out) throws IOException
            {
            out.writeLong(0, m_cMillis);
            }

        // ----- data members -----------------------------------------------

        /**
         * The {@code ttl} to apply to the processed entry.
         */
        protected long m_cMillis;
        }

    // ----- inner class: CleanupContinuation -------------------------------

    /**
     * A {@link ComposableContinuation} to clean up the {@link ClusteredTaskManager}.
     */
    public class CleanupContinuation
            implements ComposableContinuation
        {
        // ----- constructors -----------------------------------------------

        /**
         * Constructs a {@link CleanupContinuation}.
         *
         * @param cacheService  the {@link CacheService}
         * @param sTaskId        the {@link Task} identity
         */
        public CleanupContinuation(CacheService cacheService, String sTaskId)
            {
            f_cacheService = cacheService;
            f_sTaskId      = sTaskId;
            }

        // ----- continuation interface -------------------------------------

        @Override
        public void proceed(Object o)
            {
            cleanup(f_cacheService, f_sTaskId);
            }

        @Override
        public ComposableContinuation compose(ComposableContinuation continuation)
            {
            // this continuation always takes precedence over any offered continuation
            return this;
            }

        // ----- Object methods ---------------------------------------------

        @Override
        public String toString()
            {
            return "CleanupContinuation{" + "taskId='" + f_sTaskId + '\'' + '}';
            }

        // ----- data members -----------------------------------------------

        /**
         * The {@link CacheService}.
         */
        protected final CacheService f_cacheService;

        /**
         * The task id.
         */
        protected final String f_sTaskId;
        }

    // ----- inner class: NotifyExecutionStrategyProcessor ------------------

    /**
     * An {@link InvocableMap.EntryProcessor} to notify the {@link Task} that an event
     * has occurred which requires the {@link ExecutionStrategy} to be re-evaluated.
     */
    public static class NotifyExecutionStrategyProcessor
            extends PortableAbstractProcessor<String, ClusteredTaskManager, Boolean>
        {
        // ----- constructors -----------------------------------------------

        /**
         * Constructs {@link NotifyExecutionStrategyProcessor}.
         */
        public NotifyExecutionStrategyProcessor()
            {
            }

        // ----- EntryProcessor interface -----------------------------------

        @Override
        public Boolean process(InvocableMap.Entry<String, ClusteredTaskManager> entry)
            {
            if (entry.isPresent())
                {
                ClusteredTaskManager manager = entry.getValue();

                if (!manager.isCompleted())
                    {
                    manager.m_cPendingExecutionStrategyUpdateCount++;
                    entry.setValue(manager);

                    return true;
                    }

                return false;
                }

            return false;
            }
        }

    // ----- inner class: OptimizeExecutionPlanProcessor --------------------

    /**
     * An {@link InvocableMap.EntryProcessor} to optimize the {@link ExecutionPlan} for a {@link Task}.
     */
    public static class OptimizeExecutionPlanProcessor
            extends PortableAbstractProcessor<String, ClusteredTaskManager, Void>
        {
        // ----- constructors -----------------------------------------------

        /**
         * Constructs an {@link OptimizeExecutionPlanProcessor} (required for serialization).
         */
        @SuppressWarnings("unused")
        public OptimizeExecutionPlanProcessor()
            {
            }

        /**
         * Constructs {@link OptimizeExecutionPlanProcessor}.
         *
         * @param executionPlan                           the {@link ExecutionPlan}
         * @param cPendingExecutionPlanOptimizationCount  the pending {@link ExecutionPlan} optimization count
         */
        public OptimizeExecutionPlanProcessor(ExecutionPlan executionPlan, int cPendingExecutionPlanOptimizationCount)
            {
            m_executionPlan                          = executionPlan;
            m_cPendingExecutionPlanOptimizationCount = cPendingExecutionPlanOptimizationCount;
            }

        // ----- PortableAbstractProcessor interface ------------------------

        @Override
        public Void process(InvocableMap.Entry<String, ClusteredTaskManager> entry)
            {
            ExecutorTrace.entering(OptimizeExecutionPlanProcessor.class,
                                   "process",
                                   () -> String.format("key=%s, value=%s", entry.getKey(), entry.getValue()));

            if (entry.isPresent())
                {
                ClusteredTaskManager manager = entry.getValue();
                Debugging            debug   = manager.m_debugging;

                if (manager.isCompleted())
                    {
                    ExecutorTrace.log(() -> String.format(
                            "Skipping Execution Plan Optimization for Task [%s] as the Task is completed",
                            manager.getTaskId()), debug);
                    }
                else
                    {
                    ExecutionPlan currentPlan = manager.m_executionPlan;

                    if (currentPlan != null)
                        {
                        // don't overwrite "COMPLETED" assignments
                        for (Iterator<String> iter = m_executionPlan.getIds(); iter.hasNext(); )
                            {
                            String executorId = iter.next();

                            ExecutionPlan.Action currentAction = currentPlan.getAction(executorId);

                            if (currentAction == ExecutionPlan.Action.COMPLETED)
                                {
                                m_executionPlan.setAction(executorId, ExecutionPlan.Action.COMPLETED);
                                }
                            }
                        }

                    ExecutorTrace.log(() -> String.format("Optimized Execution Plan for Task [%s].  Now [%s]",
                                                   manager.m_sTaskId, m_executionPlan), debug);

                    manager.m_executionPlan = m_executionPlan;
                    manager.m_cPendingExecutionPlanOptimizationCount =
                        Math.max(0, manager.m_cPendingExecutionPlanOptimizationCount
                                    - m_cPendingExecutionPlanOptimizationCount);

                    entry.setValue(manager);
                    }
                }

            ExecutorTrace.exiting(OptimizeExecutionPlanProcessor.class, "process");

            return null;
            }

        // ----- PortableObject interface -----------------------------------

        @Override
        public void readExternal(PofReader in) throws IOException
            {
            m_executionPlan                          = in.readObject(0);
            m_cPendingExecutionPlanOptimizationCount = in.readInt(1);
            }

        @Override
        public void writeExternal(PofWriter out) throws IOException
            {
            out.writeObject(0, m_executionPlan);
            out.writeInt(1,    m_cPendingExecutionPlanOptimizationCount);
            }

        // ----- data members -----------------------------------------------

        /**
         * The {@link ExecutionPlan}.
         */
        protected ExecutionPlan m_executionPlan;

        /**
         * Pending execution plan optimization count.
         */
        protected int m_cPendingExecutionPlanOptimizationCount;
        }

    // ----- inner class SetActionProcessor ---------------------------------

    /**
     * An {@link InvocableMap.EntryProcessor} to compare and set the action of a
     * {@link ExecutionPlan} for a given {@link Executor}. Return true if the action
     * is set, false otherwise.
     */
    public static class SetActionProcessor
            extends PortableAbstractProcessor<String, ClusteredTaskManager, Boolean>
        {
        // ----- constructors -----------------------------------------------

        /**
         * Constructs an {@link SetActionProcessor} (required for serialization).
         */
        @SuppressWarnings("unused")
        public SetActionProcessor()
            {
            }

        /**
         * Constructs a {@link ClusteredExecutorInfo.SetStateProcessor} that ignores
         * the current state.
         *
         * @param sExecutorId  the {@link Executor} id
         * @param desired      the desired state
         */
        public SetActionProcessor(String sExecutorId, ExecutionPlan.Action desired)
            {
            m_sExecutorId = sExecutorId;
            m_previous    = null;
            m_desired     = desired;
            }

        /**
         * Constructs a {@link ClusteredExecutorInfo.SetStateProcessor}.
         *
         * @param sExecutorId  the {@link Executor} id
         * @param previous     the previous states (<code>null</code> if okay to replace any state)
         * @param desired      the desired state
         */
        public SetActionProcessor(String sExecutorId, EnumSet<ExecutionPlan.Action> previous,
                ExecutionPlan.Action desired)
            {
            m_sExecutorId = sExecutorId;
            m_previous    = previous;
            m_desired     = desired;
            }

        /**
         * Constructs a {@link ClusteredExecutorInfo.SetStateProcessor}.
         *
         * @param sExecutorId  the {@link Executor} id
         * @param previous    the previous state (<code>null</code> if okay to replace any state)
         * @param desired     the desired state
         */
        @SuppressWarnings("unused")
        public SetActionProcessor(String sExecutorId, ExecutionPlan.Action previous, ExecutionPlan.Action desired)
            {
            m_sExecutorId = sExecutorId;
            m_previous    = EnumSet.of(previous);
            m_desired     = desired;
            }

        // ----- EntryProcessor interface -----------------------------------

        @Override
        public Boolean process(InvocableMap.Entry<String, ClusteredTaskManager> entry)
            {
            // assume the action was not set
            boolean result = false;

            ExecutorTrace.entering(SetActionProcessor.class,
                                   "process",
                                   () -> String.format("key=%s, value=%s", entry.getKey(), entry.getValue()));

            if (entry.isPresent())
                {
                ClusteredTaskManager manager = entry.getValue();

                ExecutionPlan.Action existing = manager.m_executionPlan.getAction(m_sExecutorId);

                if (existing != null && m_previous != null && m_previous.contains(existing)
                    || m_previous == null
                    || m_previous.isEmpty())
                    {
                    Debugging debug = manager.m_debugging;
                    ExecutorTrace.log(() -> String.format("Changing Executor [%s] action from [%s] to [%s]",
                                                   m_sExecutorId, existing, m_desired), debug);

                    boolean isStateSet = manager.m_executionPlan.setAction(m_sExecutorId, m_desired);

                    if (isStateSet)
                        {
                        entry.setValue(manager);
                        }

                    result = isStateSet;
                    }
                }

            ExecutorTrace.exiting(SetActionProcessor.class, "process", result);

            return result;
            }

        // ----- PortableObject interface -----------------------------------

        @Override
        public void readExternal(PofReader in) throws IOException
            {
            m_sExecutorId = in.readString(0);

            ExecutionPlan.Action action = in.readObject(1);

            if (action != null)
                {
                m_previous = EnumSet.of(action);
                }

            m_desired = in.readObject(2);
            }

        @Override
        public void writeExternal(PofWriter out) throws IOException
            {
            out.writeString(0, m_sExecutorId);
            out.writeObject(1, m_previous == null ? null : m_previous.iterator().next());
            out.writeObject(2, m_desired);
            }

        // ----- data members -----------------------------------------------

        /**
         * The {@link Executor} id.
         */
        protected String m_sExecutorId;

        /**
         * The previous allowed {@link ExecutorInfo.State}s, or <code>null</code> if not
         * applicable.
         */
        protected EnumSet<ExecutionPlan.Action> m_previous;

        /**
         * The desired {@link ExecutorInfo.State}.
         */
        protected ExecutionPlan.Action m_desired;
        }

    // ----- inner class: CancellationProcessor --------------------------------

    /**
     * An {@link InvocableMap.EntryProcessor} to terminate a running {@link Task}.
     */
    public static class CancellationProcessor
            extends PortableAbstractProcessor<String, ClusteredTaskManager, Boolean>
        {
        // ----- EntryProcessor interface -----------------------------------

        @Override
        public Boolean process(InvocableMap.Entry<String, ClusteredTaskManager> entry)
            {
            if (entry.isPresent())
                {
                ClusteredTaskManager manager = entry.getValue();

                if (manager.m_state == State.ORCHESTRATED)
                    {
                    manager.m_fCancelled = true;
                    manager.m_state = State.TERMINATING;

                    entry.setValue(manager);

                    return true;
                    }
                }

            return false;
            }
        }

    // ----- inner class UpdateCollectedResultProcessor ---------------------

    /**
     * An {@link PortableAbstractProcessor} to update the collected result for a {@link Task}.
     *
     * @param <T> the result type
     */
    public static class UpdateCollectedResultProcessor<T>
            extends PortableAbstractProcessor<String, ClusteredTaskManager, Void>
        {
        // ----- constructors -----------------------------------------------

        /**
         * Constructs an {@link UpdateCollectedResultProcessor} (required for serialization).
         */
        @SuppressWarnings("unused")
        public UpdateCollectedResultProcessor()
            {
            }

        /**
         * Constructs an {@link UpdateCollectedResultProcessor}.
         *
         * @param newResult                      the new result
         * @param lProcessedResultMapGeneration  the processed generation count of the result map
         * @param fCompleted                     a boolean to indicate if the task is completed
         */
        public UpdateCollectedResultProcessor(Result<T> newResult, long lProcessedResultMapGeneration,
                boolean fCompleted)
            {
            m_lProcessedResultMapGeneration = lProcessedResultMapGeneration;
            m_newResult                     = newResult;
            m_fCompleted                    = fCompleted;
            }

        // ----- EntryProcessor interface -----------------------------------

        @Override
        public Void process(InvocableMap.Entry<String, ClusteredTaskManager> entry)
            {
            ExecutorTrace.entering(UpdateCollectedResultProcessor.class,
                                   "process",
                                   () -> String.format("key=%s, value=%s", entry.getKey(), entry.getValue()));

            if (entry.isPresent())
                {
                ClusteredTaskManager manager = entry.getValue();
                Debugging            debug   = manager.m_debugging;

                if (manager.m_collector != null)
                    {
                    if (manager.m_lastResult == null)
                        {
                        ExecutorTrace.log(() -> String.format("Task [%s] has a newly collected result [%s]",
                                                       manager.getTaskId(), m_newResult), debug);
                        }
                    else
                        {
                        ExecutorTrace.log(() -> String.format("Task [%s] collected result will be updated from [%s] to [%s]",
                                                       manager.getTaskId(), manager.getLastResult(), m_newResult),
                                   debug);
                        }

                    if (manager.m_lastResult == null &&
                            m_newResult != null ||
                            !Objects.equals(manager.m_lastResult, m_newResult))
                        {
                        manager.m_lastResult = m_newResult;
                        manager.m_nResultVersion++;

                        // move forward the generation of processed results
                        manager.m_lProcessedResultGeneration = m_lProcessedResultMapGeneration;
                        }
                    else
                        {
                        ExecutorTrace.log(() -> String.format(
                                "Task [%s] result [%s] has not changed.  No update will be performed",
                                manager.getTaskId(), manager.getLastResult()), debug);
                        }
                    }

                // update if the result completed the task (iff completed)
                if (!manager.m_fCompleted && m_fCompleted)
                    {
                    manager.m_fCompleted = true;
                    manager.m_state      = State.TERMINATING;
                    }

                ExecutorTrace.log(() -> String.format(
                        "Task [%s] ([%s])",
                        manager.getTaskId(), manager), debug);

                entry.setValue(manager);
                }
            else
                {
                Logger.fine(() -> String.format("Ignoring request to update Task [%s] as it is no longer present",
                                                entry.getKey()));
                }

            ExecutorTrace.exiting(UpdateCollectedResultProcessor.class, "process");

            return null;
            }

        // ----- PortableObject interface -----------------------------------

        @Override
        public void readExternal(PofReader in) throws IOException
            {
            m_newResult                     = in.readObject(0);
            m_lProcessedResultMapGeneration = in.readLong(1);
            m_fCompleted                    = in.readBoolean(2);
            }

        @Override
        public void writeExternal(PofWriter out) throws IOException
            {
            out.writeObject(0,  m_newResult);
            out.writeLong(1,    m_lProcessedResultMapGeneration);
            out.writeBoolean(2, m_fCompleted);
            }

        // ----- data members -----------------------------------------------

        /**
         * The new {@link Result}.
         */
        protected Result<T> m_newResult;

        /**
         * The processed generation count of the result map.
         */
        protected long m_lProcessedResultMapGeneration;

        /**
         * Flag indicating completion status.
         */
        protected boolean m_fCompleted;
        }

    // ----- inner class: UpdateContributedResultProcessor ------------------

    /**
     * An {@link InvocableMap.EntryProcessor} to update a contributed {@link Result}
     * for a specific {@link Executor}, returning <code>true</code> if the update
     * was successful, <code>false</code> otherwise.
     */
    public static class UpdateContributedResultProcessor
            extends PortableAbstractProcessor<String, ClusteredTaskManager, Boolean>
        {
        // ----- constructors -----------------------------------------------

        /**
         * Constructs an {@link UpdateContributedResultProcessor} (required for serialization).
         */
        @SuppressWarnings("unused")
        public UpdateContributedResultProcessor()
            {
            }

        /**
         * Constructs an {@link UpdateContributedResultProcessor}.
         *
         * @param sExecutorId the identity of the {@link Executor}
         * @param result      the {@link Result} for the {@link Executor}
         */
        public UpdateContributedResultProcessor(String sExecutorId, Result result)
            {
            m_sExecutorId = sExecutorId;
            m_result      = result;
            }

        // ----- EntryProcessor interface -----------------------------------

        @Override
        public Boolean process(InvocableMap.Entry<String, ClusteredTaskManager> entry)
            {
            ExecutorTrace.entering(UpdateContributedResultProcessor.class,
                                   "process",
                                   () -> String.format("key=%s, value=%s", entry.getKey(), entry.getValue()));

            try
                {
                if (entry.isPresent())
                    {
                    ClusteredTaskManager taskManager = entry.getValue();
                    Debugging            debug       = taskManager.m_debugging;

                    if (taskManager.isOwner(m_sExecutorId))
                        {
                        taskManager.setResult(m_result);

                        entry.setValue(taskManager);

                        ExecutorTrace.log(() -> String.format("Result[%s] contributed for Task [%s] by Executor [%s]: %s",
                                                       taskManager.m_lCurrentResultGeneration, taskManager.getTaskId(),
                                                       m_sExecutorId, m_result), debug);

                        return true;
                        }
                    else
                        {
                        // there's no existing Task Result, so we return false (indicating an error)
                        if (Logger.isEnabled(debug.getLogLevel()))
                            {
                            String sMsg = "Ignoring result contributed for Task [%s] as the Task is no longer"
                                          + " assigned to Executor [%s]: %s";
                            ExecutorTrace.log(String.format(sMsg, taskManager.getTaskId(), m_sExecutorId, m_result),
                                       debug);
                            }

                        return false;
                        }
                    }
                else
                    {
                    if (Logger.isEnabled(Logger.FINEST))
                        {
                        Logger.finest(String.format("Ignoring result contributed for Task [%s] as the Task is no longer"
                                + " present. Executor [%s]: %s", entry.getKey(), m_sExecutorId, m_result));
                        }

                    return false;
                    }
                }
            finally
                {
                ExecutorTrace.exiting(UpdateContributedResultProcessor.class, "process");
                }
            }

        // ----- PortableObject interface -----------------------------------

        @Override
        public void readExternal(PofReader in) throws IOException
            {
            m_sExecutorId = in.readString(0);
            m_result      = in.readObject(1);
            }

        @Override
        public void writeExternal(PofWriter out) throws IOException
            {
            out.writeString(0, m_sExecutorId);
            out.writeObject(1, m_result);
            }

        // ----- data members -----------------------------------------------

        /**
         * The identity of the {@link Executor}.
         */
        protected String m_sExecutorId;

        /**
         * The {@link Result} for the {@link Executor}.
         */
        protected Result m_result;
        }

    /**
     * An {@link InvocableMap.EntryProcessor} to update the {@link ExecutionPlan} for a {@link Task} after the {@link
     * ExecutionStrategy} has been evaluated.
     */
    public static class UpdateExecutionPlanProcessor
            extends PortableAbstractProcessor<String, ClusteredTaskManager, Void>
        {
        // ----- constructors -----------------------------------------------

        /**
         * Constructs an {@link UpdateExecutionPlanProcessor} (required for serialization).
         */
        @SuppressWarnings("unused")
        public UpdateExecutionPlanProcessor()
            {
            }

        /**
         * Constructs {@link UpdateExecutionPlanProcessor}.
         *
         * @param executionPlan                        the {@link ExecutionPlan}
         * @param cPendingExecutionStrategyUpdateCount the pending {@link ExecutionStrategy} update count
         */
        public UpdateExecutionPlanProcessor(ExecutionPlan executionPlan, int cPendingExecutionStrategyUpdateCount)
            {
            m_executionPlan                        = executionPlan;
            m_cPendingExecutionStrategyUpdateCount = cPendingExecutionStrategyUpdateCount;
            }

        // ----- EntryProcessor interface -----------------------------------

        @Override
        public Void process(InvocableMap.Entry<String, ClusteredTaskManager> entry)
            {
            ExecutorTrace.entering(UpdateExecutionPlanProcessor.class,
                                   "process",
                                   () -> String.format("key=%s, value=%s", entry.getKey(), entry.getValue()));

            if (entry.isPresent())
                {
                ClusteredTaskManager manager = entry.getValue();
                Debugging            debug   = manager.m_debugging;

                if (manager.isCompleted())
                    {
                    ExecutorTrace.log(() -> String.format("Skipping Execution Plan Update for Task [%s] as it is completed",
                                                    manager.getTaskId()), debug);
                    }
                else
                    {
                    ExecutorTrace.log(() -> String.format("Updating Execution Plan for Task [%s]",
                                                   manager.getTaskId()), debug);

                    // store the new execution plan
                    manager.m_executionPlan = m_executionPlan;

                    // reset the pending execution strategy update count (now that we've processed)
                    manager.m_cPendingExecutionStrategyUpdateCount =
                        Math.max(0, manager.m_cPendingExecutionStrategyUpdateCount - m_cPendingExecutionStrategyUpdateCount);

                    // increment the pending optimizations as we've yet to optimize this execution plan
                    manager.m_cPendingExecutionPlanOptimizationCount++;

                    entry.setValue(manager);
                    }
                }

            ExecutorTrace.exiting(UpdateExecutionPlanProcessor.class, "process");

            return null;
            }

        // ----- PortableObject interface -----------------------------------

        @Override
        public void readExternal(PofReader in) throws IOException
            {
            m_executionPlan                        = in.readObject(0);
            m_cPendingExecutionStrategyUpdateCount = in.readInt(1);
            }

        @Override
        public void writeExternal(PofWriter out) throws IOException
            {
            out.writeObject(0, m_executionPlan);
            out.writeInt(1,    m_cPendingExecutionStrategyUpdateCount);
            }

        // ----- data members -----------------------------------------------

        /**
         * The {@link ExecutionPlan}.
         */
        protected ExecutionPlan m_executionPlan;

        /**
         * The pending {@link ExecutionStrategy} update count.
         */
        protected int m_cPendingExecutionStrategyUpdateCount;
        }

    // ----- enum: State ----------------------------------------------------

    /**
     * The state of the {@link ClusteredTaskManager}.
     */
    public enum State
        {
            /**
             * The {@code ClusteredTaskManager} is in pending state.
             */
        PENDING,

        /**
         * The {@code ClusteredTaskManager} has been orchestrated.
         */
        ORCHESTRATED,

        /**
         * The {@code ClusteredTaskManager} has been terminated.
         */
        TERMINATING
        }

    // ----- constants ------------------------------------------------------

    /**
     * {@link Filter} for obtaining only JOINING and RUNNING executors.
     *
     * @since 22.06.7
     */
    protected static final Filter<ExecutorInfo> RUNNING_EXECUTOR_FILTER =
            Filters.equal(Extractors.extract("state"), ExecutorInfo.State.JOINING)
                    .or(Filters.equal(Extractors.extract("state"), ExecutorInfo.State.RUNNING));

    // ----- data members ---------------------------------------------------

    /**
     * The unique identity of the {@link Task}.
     */
    protected String m_sTaskId;

    /**
     * The partition based sequence number of the {@link Task}.
     */
    protected volatile long m_lTaskSequence;

    /**
     * The partition ID of the {@link Task}.
     */
    protected int m_nPartitionId;

    /**
     * The {@link Task}.
     */
    protected Task<T> m_task;

    /**
     * The {@link ExecutionStrategy} for executing the {@link Task} with {@link Executor}s.
     */
    protected ExecutionStrategy m_executionStrategy;

    /**
     * The {@link Task.Collector} for the {@link Task} result(s).
     */
    protected Task.Collector<T, A, R> m_collector;

    /**
     * The {@link Predicate} to determine if the {@link Task} is complete, based on a collected result.
     */
    protected Predicate<? super R> m_completionPredicate;

    /**
     * The runnable to call when the {@link Task} is complete.
     */
    protected Task.CompletionRunnable<? super R> m_completionRunnable;

    /**
     * A flag to indicate whether to run the completionRunnable or not.
     */
    protected boolean m_fRunCompletionRunnable;

    /**
     * The {@link Duration} to retain the {@link Task} after it is complete.
     */
    protected Duration m_retainDuration;

    /**
     * The {@link Debugging} option for the {@link Task}.
     */
    protected Debugging m_debugging;

    /**
     * The current {@link ExecutionPlan}.  Will be <code>null</code> when is still to be created using the {@link
     * ExecutionStrategy}.
     */
    protected ExecutionPlan m_executionPlan;

    /**
     * The number of events/updates/changes that have occurred since the last time the {@link ExecutionStrategy} was
     * evaluated, which may impact the {@link ExecutionPlan}.
     */
    protected int m_cPendingExecutionStrategyUpdateCount;

    /**
     * The number of potential optimizations that can occurred to the {@link ExecutionPlan} since it was last
     * optimized.
     */
    protected int m_cPendingExecutionPlanOptimizationCount;

    /**
     * The last collected result for the {@link Task}.
     */
    protected volatile Result<R> m_lastResult;

    /**
     * The lastResult version represented by a monotonically increasing integer value.
     */
    protected int m_nResultVersion;

    /**
     * The list of {@link Result}s returned by the {@link Executor}s that are pending processing by the {@link
     * Task.Collector}.
     */
    protected List<Result<T>> m_listResults;

    /**
     * Monotonically increasing generation counter indicating how many times a result was provided.
     */
    protected long m_lCurrentResultGeneration;

    /**
     * The generation counter of the last time the results were evaluated.
     */
    protected long m_lProcessedResultGeneration;

    /**
     * A flag to indicate if the {@link Task} is now completed, in which case no further results will be accepted and no
     * future updates will be published.
     */
    protected volatile boolean m_fCompleted;

    /**
     * A flag to indicate if the {@link Task} is now cancelled, in which case no further results will be accepted and no
     * future updates will be published.
     */
    protected volatile boolean m_fCancelled;

    /**
     * The current state of the {@link ClusteredTaskManager}.
     */
    protected volatile State m_state;

    /**
     * The {@link SpanContext} of the span enqueuing this task.
     */
    private SpanContext m_parentSpanContext = SpanContext.Noop.INSTANCE;

    private static final String EOL = System.lineSeparator();
    }
