/*
 * Copyright (c) 2016, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.executor;

import com.oracle.coherence.concurrent.executor.atomic.AtomicEnum;

import com.oracle.coherence.concurrent.executor.function.Predicates;

import com.oracle.coherence.concurrent.executor.options.Member;
import com.oracle.coherence.concurrent.executor.options.Role;

import com.oracle.coherence.concurrent.executor.options.Storage;
import com.oracle.coherence.concurrent.executor.subscribers.internal.AnyFutureSubscriber;
import com.oracle.coherence.concurrent.executor.subscribers.internal.FutureSubscriber;

import com.oracle.coherence.concurrent.executor.tasks.internal.CallableTask;
import com.oracle.coherence.concurrent.executor.tasks.internal.RunnableTask;
import com.oracle.coherence.concurrent.executor.tasks.internal.RunnableWithResultTask;
import com.oracle.coherence.concurrent.executor.tasks.internal.ScheduledCallableTask;
import com.oracle.coherence.concurrent.executor.tasks.internal.ScheduledRunnableTask;

import com.oracle.coherence.concurrent.executor.util.Caches;
import com.oracle.coherence.concurrent.executor.util.OptionsByType;

import com.tangosol.internal.tracing.Scope;
import com.tangosol.internal.tracing.Span;
import com.tangosol.internal.tracing.TracingHelper;

import com.tangosol.net.CacheService;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.DistributedCacheService;
import com.tangosol.net.Session;

import com.tangosol.util.Base;
import com.tangosol.util.DaemonThreadFactory;

import com.tangosol.util.extractor.ReflectionExtractor;

import com.tangosol.util.function.Remote;

import java.time.Duration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.RunnableScheduledFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * An Oracle Coherence based {@link TaskExecutorService}.
 *
 * @author bo, lh
 * @since 21.12
 */
public class ClusteredExecutorService
        implements TaskExecutorService
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a {@link ClusteredExecutorService} based on the specified {@link CacheService}.
     *
     * @param cacheService  the {@link CacheService} to use for orchestration metadata
     */
    public ClusteredExecutorService(CacheService cacheService)
        {
        init(cacheService);
        }

    /**
     * Constructs a {@link ClusteredExecutorService} based on the specified {@link ConfigurableCacheFactory}.
     *
     * @param cacheFactory  the {@link ConfigurableCacheFactory}
     */
    public ClusteredExecutorService(ConfigurableCacheFactory cacheFactory)
        {
        init(Caches.assignments(cacheFactory).getCacheService());
        }

    /**
     * Constructs a {@link ClusteredExecutorService} based on the specified {@link Session}.
     *
     * @param session  the {@link Session}
     */
    public ClusteredExecutorService(Session session)
        {
        init(Caches.assignments(session).getCacheService());
        }


    // ----- accessors ------------------------------------------------------

    /**
     * Obtains the internal {@link ScheduledExecutorService} that can be used for scheduling asynchronous local
     * activities for the {@link TaskExecutorService}.
     *
     * @return a {@link ScheduledExecutorService}
     */
    public ScheduledExecutorService getScheduledExecutorService()
        {
        return f_scheduledExecutorService;
        }

    /**
     * Obtains the {@link CacheService} being used by the {@link TaskExecutorService}.
     *
     * @return the {@link CacheService}
     */
    public CacheService getCacheService()
        {
        return m_cacheService;
        }

    // ----- TaskExecutorService interface ----------------------------------

    @Override
    public Registration register(ExecutorService executor, Registration.Option... options)
        {
        if (isShutdown())
            {
            throw new IllegalStateException("ClusteredExecutorService [" + this + "] is "
                                            + (isTerminated() ? "terminated" : "shutting down") + '.');
            }

        ClusteredRegistration registration = f_mapLocalRegistrations.get(executor);

        if (registration == null)
            {
            // automatically include the Member (when it's not defined)
            OptionsByType<Registration.Option> optionsByType = OptionsByType.from(Registration.Option.class, options);

            if (optionsByType.get(Member.class, null) == null)
                {
                optionsByType.add(Member.autoDetect());
                }

            // automatically include the Member Role (when Role is not defined)
            if (optionsByType.get(Role.class, null) == null)
                {
                Member member = optionsByType.get(Member.class);

                optionsByType.add(Role.of(member.get().getRoleName()));
                }

            // automatically include the Storage type (when Storage is not defined)
            if (optionsByType.get(Storage.class, null) == null)
                {
                CacheService service = getCacheService();
                Storage      storage =
                        Storage.enabled(getCacheService() instanceof DistributedCacheService
                                        && ((DistributedCacheService) service).isLocalStorageEnabled());

                optionsByType.add(storage);
                }

            // establish a unique identity for the registration
            String sIdentity = UUID.randomUUID().toString();

            // create a local registration for the executor
            registration = new ClusteredRegistration(this, sIdentity, executor, optionsByType);

            ClusteredRegistration existingRegistration = f_mapLocalRegistrations.putIfAbsent(executor, registration);

            // ensure we use the existing registration if there was one
            if (existingRegistration != null)
                {
                registration = existingRegistration;
                }

            // start the registration
            registration.start();
            }

        return registration;
        }

    @Override
    public Registration deregister(ExecutorService executor)
        {
        ClusteredRegistration registration = f_mapLocalRegistrations.remove(executor);

        if (registration != null)
            {
            // close the registration
            registration.close();
            }

        if (isShutdown() && f_mapLocalRegistrations.isEmpty())
            {
            f_scheduledExecutorService.shutdown();
            }

        return registration;
        }

    @Override
    public <T> Task.Orchestration<T> orchestrate(Task<T> task)
        {
        return new ClusteredOrchestration<>(this, Objects.requireNonNull(task));
        }

    @SuppressWarnings("rawtypes")
    @Override
    public <R> Task.Coordinator<R> acquire(String sTaskId)
        {
        Objects.requireNonNull(sTaskId);
        CacheService service = getCacheService();

        if (Caches.tasks(service).containsKey(sTaskId))
            {
            ClusteredTaskManager manager = (ClusteredTaskManager) Caches.tasks(service).get(sTaskId);

            ClusteredTaskCoordinator<R> coordinator =
                    new ClusteredTaskCoordinator<>(service, manager, getScheduledExecutorService());

            if (manager.isCompleted() && manager.getRetainDuration() != null)
                {
                coordinator.close();
                }
            return coordinator;
            }

        return null;
        }

    // ----- RemoteExecutor interface ---------------------------------------

    @Override
    public boolean isShutdown()
        {
        return f_state.get().compareTo(State.STOPPING_GRACEFULLY) >= 0;
        }

    @Override
    public boolean isTerminated()
        {
        if (f_state.get().equals(State.TERMINATED))
            {
            return true;
            }
        else if (f_scheduledExecutorService.isTerminated())
            {
            // f_scheduledExecutorService has finished shutting down
            setState(State.ANY, State.TERMINATED);

            return true;
            }
        else
            {
            return false;
            }
        }

    @Override
    public boolean awaitTermination(long lcTimeout, TimeUnit unit)
            throws InterruptedException
        {
        if (isTerminated())
            {
            return true;
            }
        else
            {
            // wait for f_scheduledExecutorService to terminate
            return f_scheduledExecutorService.awaitTermination(lcTimeout, unit);
            }
        }

    @Override
    public void shutdown()
        {
        if (shutdownInternal())
            {
            if (f_mapLocalRegistrations.isEmpty())
                {
                f_scheduledExecutorService.shutdown();
                }
            else
                {
                // set local executors to stop accepting tasks, and close() gracefully
                for (ClusteredRegistration registration : f_mapLocalRegistrations.values())
                    {
                    registration.shutdown();
                    }
                }
            }
        }

    @Override
    public List<Runnable> shutdownNow()
        {
        shutdownNowInternal();

        return Collections.emptyList();
        }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public void execute(Remote.Runnable command)
        {
        Objects.requireNonNull(command);

        if (command instanceof CESRunnableFuture) // constructed from newTaskFor()
            {
            CESRunnableFuture<?> futureSubscriber = (CESRunnableFuture) command;

            Task.Coordinator coordinator =
                    orchestrate(futureSubscriber.isRunnable()
                            ? new RunnableWithResultTask(futureSubscriber.getRunnable(),
                                                         futureSubscriber.getRunnableValue())
                            : new CallableTask(futureSubscriber.getCallable()))
                    .limit(1)
                    .subscribe(futureSubscriber)
                    .submit();

            futureSubscriber.setCoordinator(coordinator);
            }
        else
            {
            orchestrate(new RunnableTask(command))
                    .limit(1)
                    .submit();
            }
        }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public <T> ScheduledFuture<T> schedule(Remote.Callable<T> callable, long lcDelay, TimeUnit unit)
        {
        Objects.requireNonNull(callable);
        Objects.requireNonNull(unit);

        ScheduledCallableTask<T> callableTask =
                new ScheduledCallableTask(callable,
                        lcDelay == 0
                        ? null
                        : Duration.ofNanos(unit.toNanos(lcDelay)));

        CESRunnableFuture futureSubscriber = new CESRunnableFuture<>(callableTask);
        Task.Coordinator  coordinator      = orchestrate(callableTask).limit(1)
                .subscribe(futureSubscriber)
                .submit();

        futureSubscriber.setCoordinator(coordinator);

        return futureSubscriber;
        }

    @Override
    public ScheduledFuture<?> schedule(Remote.Runnable command, long lcDelay, TimeUnit unit)
        {
        return scheduleRunnable(command, lcDelay, 0, 0, unit);
        }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Remote.Runnable command, long lcInitialDelay, long lcPeriod, TimeUnit unit)
        {
        if (lcPeriod <= 0)
            {
            throw new IllegalArgumentException("Period must be greater than zero");
            }

        return scheduleRunnable(command, lcInitialDelay, lcPeriod, 0, unit);
        }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Remote.Runnable command, long lcInitialDelay, long lcDelay, TimeUnit unit)
        {
        if (lcDelay <= 0)
            {
            throw new IllegalArgumentException("Delay must be greater than zero");
            }

        return scheduleRunnable(command, lcInitialDelay, 0, lcDelay, unit);
        }

    @Override
    public <T> T invokeAny(Collection<? extends Remote.Callable<T>> colTasks)
        throws InterruptedException, ExecutionException
        {
        try
            {
            return doInvokeAny(colTasks, false, 0);
            }
        catch (TimeoutException cannotHappen)
            {
            assert false;
            return null;
            }
        }

    /**
     * Override implementation is required as {@link AbstractExecutorService}'s implementation creates its own {@link
     * RunnableFuture}s that are passed to {@link #execute(Remote.Runnable)}.
     */
    @Override
    public <T> T invokeAny(Collection<? extends Remote.Callable<T>> colTasks, long lcTimeout, TimeUnit unit)
        throws InterruptedException, ExecutionException, TimeoutException
        {
        return doInvokeAny(colTasks, true, unit.toNanos(lcTimeout));
        }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Remote.Callable<T>> colTasks)
            throws InterruptedException
        {
        Objects.requireNonNull(colTasks);

        ArrayList<Future<T>> listFutures = new ArrayList<>(colTasks.size());
        try
            {
            for (Callable<T> t : colTasks)
                {
                Objects.requireNonNull(t);

                CESRunnableFuture<T> f = new CESRunnableFuture<>(t);
                listFutures.add(f);
                execute(f);
                }
            for (int i = 0, size = listFutures.size(); i < size; i++)
                {
                Future<T> f = listFutures.get(i);
                if (!f.isDone())
                    {
                    try
                        {
                        f.get();
                        }
                    catch (CancellationException | ExecutionException ignore)
                        {
                        }
                    }
                }
            return listFutures;
            }
        catch (Throwable t)
            {
            cancelAll(listFutures);
            throw t;
            }
        }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Remote.Callable<T>> tasks, long lcTimeout, TimeUnit unit)
            throws InterruptedException
        {
        Objects.requireNonNull(tasks);
        Objects.requireNonNull(unit);

        final long           lcNanos     = unit.toNanos(lcTimeout);
        final long           lcDeadline  = System.nanoTime() + lcNanos;
        ArrayList<Future<T>> listFutures = new ArrayList<>(tasks.size());
        int                  cJ          = 0;

        timedOut:
        try
            {
            for (Callable<T> t : tasks)
                {
                Objects.requireNonNull(t);

                listFutures.add(new CESRunnableFuture<>(t));
                }

            final int size = listFutures.size();

            // Interleave time checks and calls to execute in case
            // executor doesn't have any/much parallelism.
            for (int i = 0; i < size; i++)
                {
                if (((i == 0) ? lcNanos : lcDeadline - System.nanoTime()) <= 0L)
                    {
                    break timedOut;
                    }
                execute((Remote.Runnable) listFutures.get(i));
                }

            for (; cJ < size; cJ++)
                {
                Future<T> f = listFutures.get(cJ);
                if (!f.isDone())
                    {
                    try
                        {
                        f.get(lcDeadline - System.nanoTime(), TimeUnit.NANOSECONDS);
                        }
                    catch (CancellationException | ExecutionException ignore)
                        {
                        }
                    catch (TimeoutException timedOut)
                        {
                        break timedOut;
                        }
                    }
                }
            return listFutures;
            }
        catch (Throwable t)
            {
            cancelAll(listFutures);
            throw t;
            }
        // Timed out before all the tasks could be completed; cancel remaining
        cancelAll(listFutures, cJ);
        return listFutures;
        }

    @Override
    public <T> Future<T> submit(Remote.Callable<T> task)
        {
        Objects.requireNonNull(task);

        CESRunnableFuture<T> futureTask = new CESRunnableFuture<>(task);
        execute(futureTask);

        return futureTask;
        }

    @Override
    public <T> Future<T> submit(Remote.Runnable task, T result)
        {
        Objects.requireNonNull(task);

        CESRunnableFuture<T> futureTask = new CESRunnableFuture<>(task, result);
        execute(futureTask);

        return futureTask;
        }

    @Override
    public Future<?> submit(Remote.Runnable task)
        {
        Objects.requireNonNull(task);

        CESRunnableFuture<Void> futureTask = new CESRunnableFuture<>(task, null);
        execute(futureTask);

        return futureTask;
        }

    // ----- helper methods -------------------------------------------------

    /**
     * The main mechanics of invokeAny. See AbstractExecutorService#doInvokeAny(Collection, boolean, long) on
     * which this implementation is based.
     *
     * @param colTasks  {@link Collection} of {@link Remote.Callable}s
     * @param fTimed    whether the invokeAny is timed
     * @param lcNanos   nanos to wait if timed
     * @param <T>       return value type
     *
     * @return see {@link #invokeAny(Collection)}
     *
     * @throws InterruptedException see {@link #invokeAny(Collection)}
     * @throws ExecutionException   see {@link #invokeAny(Collection)}
     * @throws TimeoutException     see {@link #invokeAny(Collection)}
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    protected <T> T doInvokeAny(Collection<? extends Remote.Callable<T>> colTasks, boolean fTimed, long lcNanos)
            throws InterruptedException, ExecutionException, TimeoutException
        {
        Objects.requireNonNull(colTasks);

        int cTasks = colTasks.size();
        if (cTasks == 0)
            {
            throw new IllegalArgumentException();
            }

        // TODO review mapCoordinators - collection not queried
        //noinspection MismatchedQueryAndUpdateOfCollection
        final ConcurrentHashMap<Callable<T>, Task.Coordinator<T>> mapCoordinators = new ConcurrentHashMap<>(cTasks);

        List<FutureSubscriber<T>> listSubscribers    = new ArrayList<>(cTasks);
        Object                    oCompletionMonitor = new Object();

        try
            {
            long nStartTime = fTimed ? System.nanoTime() : 0;

            for (Callable<T> task : colTasks)
                {
                AnyFutureSubscriber<T> subscriber = new AnyFutureSubscriber<>(oCompletionMonitor);

                listSubscribers.add(subscriber);
                mapCoordinators.put(task, orchestrate(new CallableTask(task))
                        .limit(1)
                        .subscribe(subscriber)
                        .submit());
                }

            ExecutionException ee = null;
            for (;; )
                {
                synchronized (oCompletionMonitor)
                    {
                    // check all tasks
                    boolean fTaskStillRunning = false;
                    for (Iterator<FutureSubscriber<T>> iter = listSubscribers.iterator(); iter.hasNext(); )
                        {
                        FutureSubscriber<T> futureSubscriber = iter.next();
                        if (futureSubscriber.isDone())
                            {
                            if (futureSubscriber.getCompleted())
                                {
                                return futureSubscriber.get();
                                }
                            if (ee == null)
                                {
                                // collect an Exception to throw if all tasks fail
                                try
                                    {
                                    futureSubscriber.get();
                                    }
                                catch (ExecutionException exception)
                                    {
                                    ee = exception;
                                    }
                                }
                            iter.remove(); // don't bother checking this entry again
                            }
                        else
                            {
                            fTaskStillRunning = true;
                            }
                        }

                    if (fTaskStillRunning)
                        {
                        if (fTimed)
                            {
                            long nRemainingNanos = lcNanos - (System.nanoTime() - nStartTime);

                            if (nRemainingNanos > 0)
                                {
                                oCompletionMonitor.wait(nRemainingNanos / 1000000, (int) (nRemainingNanos % 1000000));
                                }
                            else
                                {
                                throw new TimeoutException();
                                }
                            }
                        else
                            {
                            oCompletionMonitor.wait();
                            }
                        }
                    else
                        {
                        // all tasks failed, ee should never be null
                        throw ee == null ? new ExecutionException(new IllegalStateException()) : ee;
                        }
                    }
                }
            }
        finally
            {
            // cancel any still running tasks
            for (Future future : listSubscribers)
                {
                future.cancel(true);
                }
            }
        }

    /**
     * Cancels all provided {@link Future futures}.
     *
     * @param futures  list of {@link Future futures}
     * @param <T>      the future result type
     */
    protected static <T> void cancelAll(List<Future<T>> futures) {
    cancelAll(futures, 0);
    }

    /**
     * Cancels provided {@link Future futures} beginning with the specified
     * index.
     *
     * @param futures    list of {@link Future futures}
     * @param nStartIdx  the start index within the list
     * @param <T>        the future result type
     */
    protected static <T> void cancelAll(List<Future<T>> futures, int nStartIdx) {
    for (int nSize = futures.size(), nIdx = nStartIdx; nIdx < nSize; nIdx++)
        futures.get(nIdx).cancel(true);
    }

    /**
     * Submit a {@link Task} for execution with the given taskId, {@link ExecutionStrategy}, {@link OptionsByType},
     * result collector, completion {@link Remote.Predicate}, and {@link Task.Subscriber}(s).
     *
     * @param task                 the {@link Task} to submit
     * @param sTaskId              the task ID
     * @param strategy             the {@link ExecutionStrategy} for this task
     * @param optionsByType        the {@link OptionsByType} to be used
     * @param properties           the {@link Task.Properties} for this task
     * @param collector            the {@link Task.Collector} to be used to collect the result
     * @param completionPredicate  the completion {@link Remote.Predicate}
     * @param completionRunnable   the {@link Task.CompletionRunnable} to call when the task is complete
     * @param retainDuration       the {@link Duration} to retain a {@link Task} after it is complete
     * @param subscribers          a list of subscribers
     * @param <T>                  the type of the task
     * @param <A>                  the accumulation type of the collectable
     * @param <R>                  the type of the collected result
     *
     * @return a {@link Task.Coordinator}
     */
    protected <T, A, R> Task.Coordinator<R> submit(Task<T> task, String sTaskId, ExecutionStrategy strategy,
            OptionsByType<Task.Option> optionsByType, Task.Properties properties,
            Task.Collector<? super T, A, R> collector, Remote.Predicate<? super R> completionPredicate,
            Task.CompletionRunnable<? super R> completionRunnable, Duration retainDuration,
            Iterator<Task.Subscriber<? super R>> subscribers)
        {
        if (isShutdown())
            {
            throw new RejectedExecutionException("ClusteredExecutorService [" + this + "] is "
                                                 + (isTerminated() ? "terminated" : "shutting down") + '.');
            }

        // ensure there's a task identity
        sTaskId = sTaskId == null || sTaskId.isEmpty() ? "task:" + UUID.randomUUID() : sTaskId;

        // ensure there's an assignment strategy
        if (strategy == null)
            {
            strategy = new ExecutionStrategyBuilder().build();
            }

        // ensure there's a completion predicate
        if (completionPredicate == null)
            {
            completionPredicate = Predicates.never();
            }

        Span.Builder builder       = TracingHelper.newSpan("Task.Submit")
                                         .withMetadata(Span.Type.COMPONENT.key(), "ExecutorService");
        Span         executionSpan = builder.startSpan();

        try (Scope ignored = TracingHelper.getTracer().withSpan(executionSpan))
            {
            // create a task manager for the task
            ClusteredTaskManager<T, A, R> manager = new ClusteredTaskManager<>(sTaskId, task, strategy, collector,
                                                                               completionPredicate, completionRunnable,
                                                                               retainDuration, optionsByType);

            ClusteredProperties clusteredProperties = null;
            if (properties != null)
                {
                clusteredProperties = new ClusteredProperties(sTaskId, m_cacheService, (TaskProperties) properties);
                }

            return new ClusteredTaskCoordinator<>(m_cacheService, manager, getScheduledExecutorService(),
                                                  clusteredProperties, subscribers);
            }
        catch (Exception e)
            {
            TracingHelper.augmentSpanWithErrorDetails(executionSpan, true, e);
            throw Base.ensureRuntimeException(e);
            }
        finally
            {
            executionSpan.end();
            }
        }

    /**
     * Responsible for adding indexes for the {@code ClusteredAssignment} cache.
     *
     * @param cacheService  the cache service providing the caches used by the
     *                      executor service
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    protected void init(CacheService cacheService)
        {
        m_cacheService = cacheService;

        if (m_cacheService instanceof DistributedCacheService)
            {
            if ((((DistributedCacheService) m_cacheService).isLocalStorageEnabled()))
                {
                // establish indexes for Executor assignments
                Caches.assignments(cacheService).addIndex(new ReflectionExtractor("getExecutorId"), true, null);
                }
            }
        }

    /**
     * Schedules the specified {@link Runnable}.
     *
     * @param command        the {@link Runnable} to schedule
     * @param cInitialDelay  the initial delay before scheduling occurs
     * @param cPeriod        the period between execution
     * @param cDelay         the delay to start the next execution after the completion
     *                       of the current execution
     * @param unit           the {@link TimeUnit} to apply
     *
     * @return a {@link ScheduledFuture} representing the result from processing the command
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    protected ScheduledFuture<?> scheduleRunnable(Runnable command, long cInitialDelay, long cPeriod,
                                                  long cDelay, TimeUnit unit)
        {
        Objects.requireNonNull(command);

        Duration initialDelayDur = cInitialDelay == 0
                ? null
                : Duration.ofNanos(unit.toNanos(cInitialDelay));

        Duration periodDur = cPeriod == 0
                ? null
                : Duration.ofNanos(unit.toNanos(cPeriod));

        Duration delayDur = cDelay == 0
                ? null
                : Duration.ofNanos(unit.toNanos(cDelay));

        ScheduledRunnableTask runnableTask = new ScheduledRunnableTask(command, initialDelayDur, periodDur, delayDur);

        CESRunnableFuture futureSubscriber = new CESRunnableFuture(runnableTask, null);
        Task.Coordinator  coordinator      = orchestrate(runnableTask)
                .limit(1)
                .subscribe(futureSubscriber)
                .submit();

        futureSubscriber.setCoordinator(coordinator);
        return futureSubscriber;
        }

    /**
     * State transition helper.
     *
     * @param from  the {@link State} transitioning from
     * @param to    the {@link State} transitioning to
     *
     * @return {@code true} if the state transition was successful
     */
    protected boolean setState(State from, State to)
        {
        boolean result;

        if (from == State.ANY)
            {
            f_state.set(to);
            result = true;
            }
        else
            {
            result = f_state.compareAndSet(from, to);
            }

        return result;
        }

    /**
     * Initiate graceful shutdown.
     *
     * @return true if shutdown has been initiated; false if the
     * {@link ClusteredExecutorService} is already shutting down or terminated.
     */
    protected boolean shutdownInternal()
        {
        return setState(State.READY, State.STOPPING_GRACEFULLY);
        }

    /**
     * Initiate immediate shutdown.
     *
     * @return true if shutdown has been initiated; false if the
     * {@link ClusteredExecutorService} is already shutting down or terminated.
     */
    @SuppressWarnings("UnusedReturnValue")
    protected boolean shutdownNowInternal()
        {
        if (setState(State.READY, State.STOPPING_IMMEDIATELY)
            || setState(State.STOPPING_GRACEFULLY, State.STOPPING_IMMEDIATELY))
            {
            for (ExecutorService executor : f_mapLocalRegistrations.keySet())
                {
                deregister(executor);
                }

            f_scheduledExecutorService.shutdown();

            return true;
            }
        else
            {
            return false;
            }
        }

    // ----- inner class: CESRunnableFuture ---------------------------------

    /**
     * {@code CESRunnableFuture} is a holder for the Callable or Runnable which will be executed by {@link
     * #execute(Remote.Runnable)}. Calling {@link #run()} is not supported.
     *
     * @param <V>  value type for encapsulated task
     */
    protected static class CESRunnableFuture<V>
            extends FutureSubscriber<V>
            implements RunnableScheduledFuture<V>, Remote.Runnable
        {
        // ----- constructors -----------------------------------------------

        /**
         * Construct a {@link CESRunnableFuture} which encapsulates the passed in {@link Callable}.
         *
         * @param callable  the {@link Callable} to encapsulate
         */
        public CESRunnableFuture(Callable<V> callable)
            {
            m_callable  = callable;
            f_fRunnable = false;
            }

        /**
         * Construct a {@link CESRunnableFuture} which encapsulates the passed in {@link Runnable}.
         *
         * @param runnable  the {@link Runnable} to encapsulate
         * @param value     the value to return on completion of the {@link Runnable}; may be null
         */
        public CESRunnableFuture(Runnable runnable, V value)
            {
            m_runnable      = runnable;
            m_runnableValue = value;
            f_fRunnable     = true;
            }

        // ----- accessors --------------------------------------------------

        /**
         * Return whether a {@link Runnable} is encapsulated.
         *
         * @return whether a {@link Runnable} is encapsulated
         */
        public boolean isRunnable()
            {
            return f_fRunnable;
            }

        /**
         * Return whether a {@link Callable} is encapsulated.
         *
         * @return whether a {@link Callable} is encapsulated
         */
        public boolean isCallable()
            {
            return !f_fRunnable;
            }

        /**
         * Get the encapsulated {@link Callable}.
         *
         * @return the encapsulated {@link Callable}
         */
        public Callable<V> getCallable()
            {
            return m_callable;
            }

        /**
         * Get the encapsulated {@link Runnable}.
         *
         * @return the encapsulated {@link Runnable}
         */
        public Runnable getRunnable()
            {
            return m_runnable;
            }

        /**
         * Get the value to be returned on completion of the encapsulated
         * {@link Runnable}.  May be {@code null}.
         *
         * @return the value to be returned on completion of the
         *         encapsulated {@link Runnable}
         */
        public V getRunnableValue()
            {
            return m_runnableValue;
            }

        @Override
        public void run()
            {
            throw new UnsupportedOperationException();
            }

        // ----- RunnableScheduledFuture interface --------------------------

        @Override
        public boolean isPeriodic()
            {
            if (isRunnable())
                {
                Runnable runnable = getRunnable();
                return runnable instanceof ScheduledRunnableTask
                       && ((ScheduledRunnableTask) getRunnable()).getPeriod() != null;
                }
            else
                {
                return false;
                }
            }

        @SuppressWarnings("rawtypes")
        @Override
        public long getDelay(TimeUnit unit)
            {
            if (isCallable())
                {
                Callable callable = getCallable();
                return callable instanceof ScheduledCallableTask
                       ? unit.convert(((ScheduledCallableTask) callable).getInitialDelay().toNanos(),
                                      TimeUnit.NANOSECONDS)
                       : 0;
                }
            else
                {
                Runnable runnable = getRunnable();
                return runnable instanceof ScheduledRunnableTask
                       ? unit.convert(((ScheduledRunnableTask) runnable).getInitialDelay().toNanos(),
                                      TimeUnit.NANOSECONDS)
                       : 0;
                }
            }

        @Override
        public int compareTo(Delayed o)
            {
            if (o == this) // compare zero if same object
                {
                return 0;
                }
            long diff = getDelay(TimeUnit.NANOSECONDS) - o.getDelay(TimeUnit.NANOSECONDS);
            return (diff < 0) ? -1 : (diff > 0) ? 1 : 0;
            }

        // ----- data members -----------------------------------------------

        /**
         * The encapsulated {@link Callable}. May be {@code null}.
         */
        protected Callable<V> m_callable;

        /**
         * The encapsulated {@link Runnable}. May be {@code null}.
         */
        protected Runnable m_runnable;

        /**
         * The value to return on completion of the encapsulated {@link Runnable}.
         */
        protected V m_runnableValue;

        /**
         * Whether a {@link Callable} or a {@link Runnable} is encapsulated.
         */
        protected final boolean f_fRunnable;
        }

    // ----- enum: State ----------------------------------------------------

    /**
     * Enumeration representing possible state transitions of the
     * {@code ClusteredExecutorService}.
     */
    protected enum State
        {
        /**
         * {@code ANY} is used for state transitions.
         */
        ANY,

        /**
         * The executor service is ready to process tasks.
         */
        READY,

        /**
         * The executor service is shutting down gracefully.
         */
        STOPPING_GRACEFULLY,

        /**
         * The executor service is stopping immediately.
         */
        STOPPING_IMMEDIATELY,

        /**
         * The executor service has terminated.
         */
        TERMINATED
        }

    // ----- data members ---------------------------------------------------

    /**
     * The {@link CacheService} containing the orchestration metadata caches.
     */
    protected CacheService m_cacheService;

    /**
     * The locally registered {@link Executor} {@link Registration}s, by {@link Executor}s.
     */
    protected final ConcurrentHashMap<ExecutorService, ClusteredRegistration> f_mapLocalRegistrations
            = new ConcurrentHashMap<>();

    /**
     * The current state of the {@link TaskExecutorService}.
     */
    protected final AtomicEnum<State> f_state = AtomicEnum.of(State.READY);

    /**
     * A {@link ScheduledExecutorService} for performing local asynchronous operations.
     */
    protected final ScheduledExecutorService f_scheduledExecutorService =
            Executors.newSingleThreadScheduledExecutor(new DaemonThreadFactory("ClusteredExecutorService-"));
    }
