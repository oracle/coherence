/*
 * Copyright (c) 2021, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.executor;

import com.oracle.coherence.concurrent.executor.function.Predicates;
import com.oracle.coherence.concurrent.executor.options.Name;

import com.tangosol.config.xml.NamespaceHandler;

import com.tangosol.util.ThreadFactory;

import com.tangosol.util.function.Remote;

import java.util.Collection;
import java.util.List;

import java.util.Objects;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A RemoteExecutor allows submitting and/or scheduling
 * {@link Remote.Runnable runnables}, {@link Remote.Callable callables},
 * and {@link Task tasks} for execution within a Coherence cluster.
 *
 * <h2>Using a RemoteExecutor</h2>
 * A RemoteExecutor may be obtained by a known name:
 *   {@link #get(String) RemoteExecutor.get(“executorName”}
 * <p>
 * Once a reference to a {@code RemoteExecutor} has been obtained,
 * similar to an {@link ExecutorService}, tasks may be submitted:
 *
 * <pre>
 * {@code
 *     // obtain the named RemoteExecutor (defined in xml configuration; see below)
 *     RemoteExecutor executor = RemoteExecutor.get("MyExecutor");
 *
 *     // submit a simple runnable to the cluster but only to the executors
 *     // named "MyExecutor"
 *     Future future = executor.submit(() -> System.out.println("EXECUTED));
 *
 *     // block until completed
 *     future.get();
 * }
 * </pre>
 *
 * A {@code RemoteExecutor} allows scheduling of tasks independent of the
 * underlying thread pool (more about that below); See:
 * <ul>
 *   <li>{@link #schedule(Remote.Runnable, long, TimeUnit)}</li>
 *   <li>{@link #schedule(Remote.Callable, long, TimeUnit)}</li>
 *   <li>{@link #scheduleAtFixedRate(Remote.Runnable, long, long, TimeUnit)}</li>
 *   <li>{@link #scheduleWithFixedDelay(Remote.Runnable, long, long, TimeUnit)}</li>
 * </ul>
 * <p>
 * In order to use an executor, it must first be configured within
 * the application's cache configuration.  To begin configuring
 * executors, developers <em>must</em> include a reference
 * to the {@code coherence-concurrent} module's {@link NamespaceHandler}:
 *
 * <h2>Configuring RemoteExecutors</h2>
 *
 * The configuration supports multiple executor types and their related configuration.
 * In order to support executor definitions within the cache-configuration resource,
 * the document namespaces should be updated to add a reference to the executor
 * namespace handler:
 *
 * <pre>
 * {@code
 * <cache-config xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
 *                xmlns="http://xmlns.oracle.com/coherence/coherence-cache-config"
 *                xmlns:c="class://com.oracle.coherence.concurrent.config.NamespaceHandler"
 *                xsi:schemaLocation="http://xmlns.oracle.com/coherence/coherence-cache-config coherence-cache-config.xsd
 *                                   class://com.oracle.coherence.concurrent.config.NamespaceHandler concurrent.xsd">
 *   ...
 * <cache-config>
 * }
 * </pre>
 *
 * In this case, the arbitrary namespace of {@code c} was chosen and will be used
 * for the examples below.
 * <p>
 * It should be noted, that it will be normal to have the same executor configured
 * on multiple Coherence cluster members.  When dispatching a task, it will be
 * sent to one of the executors matching the configured name for processing.  Thus,
 * if a member fails, the tasks will fail over to the remaining named executors
 * still present in the cluster.
 * <p>
 * The lifecycle of registered executors is tied to that of the owning
 * Coherence cluster member, thus if a cluster member is brought down
 * gracefully, the remaining tasks running on the executor local to that member
 * will continue to completion.
 *
 * <h2>Example configurations</h2>
 *
 * <pre>
 * {@code
 * <!-- creates a single-threaded executor named <em>Single</em> -->
 * <c:single>
 *   <c:name>Single</c:nam>
 * </c:single>
 *
 * <!-- creates a fixed thread pool executor named <em>Fixed5</em> with five threads -->
 * <c:fixed>
 *   <c:name>Fixed5</c:name>
 *   <c:thread-count>5</c:thread-count>
 * </c:fixed>
 *
 * <!-- creates a cached thread pool executor named <em>Cached</em> -->
 * <c:cached>
 *   <c:name>Cached</c:name>
 * </c:cached>
 *
 * <!-- creates a work-stealing thread pool executor named <em>Stealing</em> with a parallelism of five-->
 * <c:work-stealing>
 *   <c:name>Stealing</c:name>
 *   <c:parallelism>5</c:parallelism>
 * </c:work-stealing>
 * }
 * </pre>
 *
 * An example defining a {@link ThreadFactory}:
 *
 * <pre>
 * {@code
 * <!-- creates a fixed thread pool executor named <em>Fixed5</em> with five threads and a custom thread factory -->
 * <c:fixed>
 *   <c:name>Fixed5</c:name>
 *   <c:thread-count>5</c:thread-count>
 *   <c:thread-factory>
 *     <instance>
 *       <class-name>my.custom.ThreadFactory</class-name>
 *     </instance>
 *   </c:thread-factory>
 * </c:fixed>
 * }
 * </pre>
 *
 * If not {@link ThreadFactory} is defined, a default factory will be used.
 * The threads will be named {@code CES:[executor-name]}-[incrementing-counter].
 * For example, if the executor is named {@code Fixed5}, the threads name
 * would be {@code CES:Fixed5-1, CES:Fixed5-2}, etc.
 *
 * <h2>Task Orchestration</h2>
 *
 * In addition to the {@link java.util.concurrent.ExecutorService}-like
 * functionality offered by this class, it also provides the ability to
 * {@link Task.Orchestration orchestrate } tasks concurrently or sequentially
 * across multiple Coherence cluster members and collect the produced results
 * (if any).
 * <p>
 * There are several concepts that should be understood when using orchestrations:
 * <ul>
 *     <li>The {@link Task} interface; similar in concept to a Callable</li>
 *     <li>The {@link Task.Orchestration} interface; controls how and where
 *         a {@link Task} will be run</li>
 *     <li>The {@link Task.Coordinator} interface; handles the publishing or
 *         collected results and notifying any subscribers</li>
 *     <li>The {@link Task.Subscriber} interface; a receiver of {@link Task}
 *         results</li>
 *     <li>The {@link Task.Properties} interface; properties available to
 *         any task (of the same orchestration) no matter where it is executing.
 *         Useful for storing intermediate task execution state in case of
 *         cluster fail-over</li>
 *     <li>The {@link Task.Collector} interface; defines logic for collection
 *         and yielding task results</li>
 * </ul>
 *
 * <h2>Orchestration Examples</h2>
 *
 * This simplest example is orchestrating a {@link Task} across all members
 * where the named executor is defined:
 *
 * <pre>
 * {@code
 * RemoteExecutor executor = RemoteExecutor.getDefault();
 *
 * // WaitingSubscriber is an implementation of the
 * // com.oracle.coherence.concurrent.executor.Task.Subscriber interface
 * // that has a get() method that blocks until Subscriber.onComplete() is
 * // called and will return the results received by onNext()
 * WaitingSubscriber subscriber = new WaitingSubscriber();
 *
 *
 * // ValueTask is an implementation of the
 * // com.oracle.coherence.concurrent.executor.Task interface
 * Task.Coordinator<String> coordinator = executor.submit(new ValueTask("Hello World"));
 * coordinator.subscribe(subscriber);
 *
 * // wait for the task to complete
 * // if this was run on four cluster members, the returned
 * // Collection will have four results
 * Collection<String> results = subscriber.get();
 * }
 * </pre>
 *
 * If running the tasks on all similarly named executors is not desirable,
 * it is possible to limit where the tasks are run in a couple of ways.
 * First is by setting a {@link Task.Orchestration#limit(int) limit} on
 * the orchestration:
 *
 * <pre>
 * {@code
 * // The task will be executed by a single executor on one of the owning
 * // cluster members
 * Task.Orchestration<String> orchestration =
 *     executor.orchestrate(new ValueTask("Hello World"))
 *             .limit(1)
 *             .subscribe(subscriber)
 *             .submit();
 * }
 * </pre>
 *
 * or by {@link Task.Orchestration#filter(Remote.Predicate) filtering}
 * which executor(s) will run on:
 *
 * <pre>
 * {@code
 * // The task will be executed on all cluster members matching the role
 * // of 'storage'
 * Task.Orchestration<String> orchestration =
 *     executor.orchestrate(new ValueTask("Hello World"))
 *             .filter(Predicates.role("storage"))
 *             .subscribe(subscriber)
 *             .submit();
 * }
 * </pre>
 *
 * There are several {@link Predicates predicates} available for use, however,
 * in the case none apply to the target use case, simply implement the
 * {@link Remote.Predicate} interface.  Both limits and filters can be applied
 * simultaneously.
 * <p>
 * Collection of results and how they are presented to the subscriber
 * can be customized by using {@code collect} and {@code until}:
 *
 * <pre>
 * {@code
 * // orchestrate the task, collecting the first non-null result,
 * // subscribe, and submit
 * Task.Orchestration<String> orchestration =
 *     executor.orchestrate(new MayReturnNullTask())
 *             .collect(TaskCollectors.firstOf())
 *             .until(Predicates.nonNullValue())
 *             .subscribe(subscriber)
 *             .submit();
 *
 * // wait for the task to complete
 * // the first non-result returned will be the one provided to the
 * // subscriber
 * Collection<String> results = subscriber.get();
 * }
 * </pre>
 *
 * Several {@link TaskCollectors collectors} are provided, however, in the
 * case none apply to the target use case, implement the
 * {@link Task.Collector} interface.
 * <p>
 * </pre>
 *
 * @author rlubke 11.15.2021
 * @since 21.12
 */
public interface RemoteExecutor
    {
    /**
     * Submits a one-shot task that becomes enabled after the given delay.
     *
     * @param command  the task to execute
     * @param lcDelay  the time from now to delay execution
     * @param unit     the time unit of the delay parameter
     *
     * @return a {@link ScheduledFuture} representing the pending completion
     *         of the task and whose {@code get()} method will return
     *         {@code null} upon completion
     *
     * @throws RejectedExecutionException if the task cannot be
     *                                    scheduled for execution
     * @throws NullPointerException       if {@code callable} or {@code unit}
     *                                    is {@code null}
     */
    ScheduledFuture<?> schedule(Remote.Runnable command, long lcDelay, TimeUnit unit);

    /**
     * Submits a value-returning one-shot task that becomes enabled
     * after the given delay.
     *
     * @param callable  the function to execute
     * @param lcDelay   the time from now to delay execution
     * @param unit      the time unit of the delay parameter
     * @param <V>       the type of the callable's result
     *
     * @return a ScheduledFuture that can be used to extract result or cancel
     *
     * @throws RejectedExecutionException if the task cannot be
     *                                    scheduled for execution
     * @throws NullPointerException       if {@code callable} or {@code unit}
     *                                    is {@code null}
     */
    <V> ScheduledFuture<V> schedule(Remote.Callable<V> callable, long lcDelay, TimeUnit unit);

    /**
     * Submits a periodic action that becomes enabled first after the
     * given initial delay, and subsequently with the given period;
     * that is, executions will commence after
     * {@code initialDelay}, then {@code initialDelay + period}, then
     * {@code initialDelay + 2 * period}, and so on.
     *
     * <p>The sequence of task executions continues indefinitely until
     * one of the following exceptional completions occur:
     * <ul>
     *   <li>The task is {@linkplain Future#cancel explicitly cancelled}
     *       via the returned future.</li>
     *   <li>The executor terminates, also resulting in task cancellation.</li>
     *   <li>An execution of the task throws an exception.  In this case
     *       calling {@link Future#get() get} on the returned future will throw
     *       {@link ExecutionException}, holding the exception as its cause.</li>
     * </ul>
     * Subsequent executions are suppressed.  Subsequent calls to
     * {@link Future#isDone isDone()} on the returned future will
     * return {@code true}.
     *
     * <p>If any execution of this task takes longer than its period, then
     * subsequent executions may start late, but will not concurrently
     * execute.
     *
     * @param command         the task to execute
     * @param lcInitialDelay  the time to delay first execution
     * @param lcPeriod        the period between successive executions
     * @param unit            the time unit of the initialDelay and period parameters
     *
     * @return a {@link ScheduledFuture} representing pending completion of
     *         the series of repeated tasks.  The future's
     *         {@link Future#get() get()} method will never return normally,
     *         and will throw an exception upon task cancellation or
     *         abnormal termination of a task execution.
     *
     * @throws RejectedExecutionException if the {@code task} cannot be
     *                                    scheduled for execution
     * @throws NullPointerException       if {@code callable} or {@code unit}
     *                                    is {@code null}
     * @throws IllegalArgumentException   if {@code lcPeriod} less than or equal to zero
     */
    ScheduledFuture<?> scheduleAtFixedRate(Remote.Runnable command, long lcInitialDelay, long lcPeriod, TimeUnit unit);

    /**
     * Submits a periodic action that becomes enabled first after the
     * given initial delay, and subsequently with the given delay
     * between the termination of one execution and the commencement of
     * the next.
     *
     * <p>The sequence of task executions continues indefinitely until
     * one of the following exceptional completions occur:
     * <ul>
     *   <li>The task is {@linkplain Future#cancel explicitly cancelled}
     *       via the returned future.</li>
     *   <li>The executor terminates, also resulting in task cancellation.</li>
     *   <li>An execution of the task throws an exception.  In this case
     *       calling {@link Future#get() get} on the returned future will throw
     *       {@link ExecutionException}, holding the exception as its cause.</li>
     * </ul>
     * Subsequent executions are suppressed.  Subsequent calls to
     * {@link Future#isDone isDone()} on the returned future will
     * return {@code true}.
     *
     * @param command         the task to execute
     * @param lcInitialDelay  the time to delay first execution
     * @param lcDelay         the delay between the termination of one
     *                        execution and the commencement of the next
     * @param unit            the time unit of the initialDelay and delay
     *                        parameters
     *
     * @return a {@link ScheduledFuture} representing pending completion of
     *         the series of repeated tasks.  The future's {@link
     *         Future#get() get()} method will never return normally,
     *         and will throw an exception upon task cancellation or
     *         abnormal termination of a task execution.
     *
     * @throws RejectedExecutionException if the task cannot be
     *                                    scheduled for execution
     * @throws NullPointerException       if {@code callable} or {@code unit}
     *                                    is {@code null}
     * @throws IllegalArgumentException   if {@code lcDelay} less than or
     *                                    equal to zero
     */
    ScheduledFuture<?> scheduleWithFixedDelay(Remote.Runnable command, long lcInitialDelay,
                                              long lcDelay, TimeUnit unit);

    /**
     * Executes the given tasks, returning a list of Futures holding
     * their status and results when all complete.
     * {@link Future#isDone} is {@code true} for each
     * element of the returned list.
     * Note that a <em>completed</em> task could have
     * terminated either normally or by throwing an exception.
     * The results of this method are undefined if the given
     * collection is modified while this operation is in progress.
     *
     * @param colTasks  the collection of tasks
     * @param <T>       the type of the values returned from the tasks
     *
     * @return a list of Futures representing the tasks, in the same
     *         sequential order as produced by the iterator for the
     *         given task list, each of which has completed
     *
     * @throws InterruptedException       if interrupted while waiting, in
     *                                    which case unfinished tasks are cancelled
     * @throws NullPointerException       if tasks or any of its elements are {@code null}
     * @throws RejectedExecutionException if any task cannot be
     *                                    scheduled for execution
     */
    <T> List<Future<T>> invokeAll(Collection<? extends Remote.Callable<T>> colTasks)
            throws InterruptedException;

    /**
     * Executes the given tasks, returning a list of Futures holding
     * their status and results
     * when all complete or the timeout expires, whichever happens first.
     * {@link Future#isDone} is {@code true} for each
     * element of the returned list.
     * Upon return, tasks that have not completed are cancelled.
     * Note that a <em>completed</em> task could have
     * terminated either normally or by throwing an exception.
     * The results of this method are undefined if the given
     * collection is modified while this operation is in progress.
     *
     * @param colTasks   the collection of tasks
     * @param lcTimeout  the maximum time to wait
     * @param unit       the time unit of the timeout argument
     * @param <T>        the type of the values returned from the tasks
     *
     * @return a list of Futures representing the tasks, in the same
     *         sequential order as produced by the iterator for the
     *         given task list. If the operation did not time out,
     *         each task will have completed. If it did time out, some
     *         of these tasks will not have completed.
     *
     * @throws InterruptedException       if interrupted while waiting, in
     *                                    which case unfinished tasks are cancelled
     * @throws NullPointerException       if tasks, any of its elements, or
     *                                    unit are {@code null}
     * @throws RejectedExecutionException if any task cannot be scheduled
     *                                    for execution
     */
    <T> List<Future<T>> invokeAll(Collection<? extends Remote.Callable<T>> colTasks, long lcTimeout, TimeUnit unit)
            throws InterruptedException;

    /**
     * Executes the given tasks, returning the result
     * of one that has completed successfully (i.e., without throwing
     * an exception), if any do. Upon normal or exceptional return,
     * tasks that have not completed are cancelled.
     * The results of this method are undefined if the given
     * collection is modified while this operation is in progress.
     *
     * @param colTasks  the collection of tasks
     * @param <T>       the type of the values returned from the tasks
     *
     * @return the result returned by one of the tasks
     *
     * @throws InterruptedException       if interrupted while waiting
     * @throws NullPointerException       if tasks or any element task
     *                                    subject to execution is {@code null}
     * @throws ExecutionException         if no task successfully completes
     * @throws RejectedExecutionException if tasks cannot be scheduled
     *                                    for execution
     */
    <T> T invokeAny(Collection<? extends Remote.Callable<T>> colTasks)
            throws InterruptedException, ExecutionException;

    /**
     * Executes the given tasks, returning the result
     * of one that has completed successfully (i.e., without throwing
     * an exception), if any do before the given timeout elapses.
     * Upon normal or exceptional return, tasks that have not
     * completed are cancelled.
     * The results of this method are undefined if the given
     * collection is modified while this operation is in progress.
     *
     * @param colTasks   the collection of tasks
     * @param lcTimeout  the maximum time to wait
     * @param unit       the time unit of the timeout argument
     * @param <T>        the type of the values returned from the tasks
     *
     * @return the result returned by one of the tasks
     *
     * @throws InterruptedException       if interrupted while waiting
     * @throws NullPointerException       if tasks, or unit, or any element
     *                                    task subject to execution is {@code null}
     * @throws TimeoutException           if the given timeout elapses before
     *                                    any task successfully completes
     * @throws ExecutionException         if no task successfully completes
     * @throws RejectedExecutionException if tasks cannot be scheduled
     *                                    for execution
     */
    <T> T invokeAny(Collection<? extends Remote.Callable<T>> colTasks, long lcTimeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException;

    /**
     * Submits a value-returning task for execution and returns a
     * Future representing the pending results of the task. The
     * Future's {@code get} method will return the task's result upon
     * successful completion.
     *
     * <p>If you would like to immediately block waiting
     * for a task, you can use constructions of the form
     * {@code result = exec.submit(aCallable).get();}
     *
     * <p>Note: The {@link Executors} class includes a set of methods
     * that can convert some other common closure-like objects,
     * for example, {@link java.security.PrivilegedAction} to
     * {@link Callable} form so they can be submitted.
     *
     * @param task  the task to submit
     * @param <T>   the type of the task's result
     *
     * @return a {@link Future} representing pending completion of the task
     *
     * @throws RejectedExecutionException if the task cannot be
     *                                    scheduled for execution
     * @throws NullPointerException       if the task is {@code null}
     */
    <T> Future<T> submit(Remote.Callable<T> task);

    /**
     * Submits a Runnable task for execution and returns a Future
     * representing that task. The Future's {@code get} method will
     * return the given result upon successful completion.
     *
     * @param task    the task to submit
     * @param result  the result to return
     * @param <T>     the type of the result
     *
     * @return a {@link Future} representing pending completion of the task
     *
     * @throws RejectedExecutionException if the task cannot be
     *                                    scheduled for execution
     * @throws NullPointerException       if the task is {@code null}
     */
    <T> Future<T> submit(Remote.Runnable task, T result);

    /**
     * Submits a Runnable task for execution and returns a Future
     * representing that task. The Future's {@code get} method will
     * return {@code null} upon <em>successful</em> completion.
     *
     * @param task  the task to submit
     *
     * @return a {@link Future} representing pending completion of the task
     *
     * @throws RejectedExecutionException if the task cannot be
     *                                    scheduled for execution
     * @throws NullPointerException       if the task is {@code null}
     */
    Future<?> submit(Remote.Runnable task);

    /**
     * Submits the {@link Task} for execution by the {@link RemoteExecutor}.
     * The submitted task will be invoked, in parallel, across all cluster
     * members where this executor is registered.
     * <p>
     * The default implementation is {@code orchestrate(task).submit()}
     * </p>
     *
     * @param task  the {@link Task}
     * @param <T>   the type result produced by the {@link Task}
     *
     * @return a {@link Task.Coordinator} for the {@link Task}
     *
     * @throws RejectedExecutionException if this task cannot be accepted
     *                                    for execution
     * @throws NullPointerException       if task is {@code null}
     *
     * @see Task.Orchestration#submit()
     * @see Task.Collectable#submit()
     *
     * @since 14.1.2.0.0
     */
    default <T> Task.Coordinator<T> submit(Task<T> task)
        {
        return orchestrate(task).submit();
        }

    /**
     * Executes the given command at some time in the future.  The command
     * may execute in a new thread, in a pooled thread, or in the calling
     * thread, at the discretion of the {@code Executor} implementation.
     *
     * @param command  the runnable task
     *
     * @throws RejectedExecutionException if this task cannot be accepted
     *                                    for execution
     * @throws NullPointerException       if command is {@code null}
     */
    void execute(Remote.Runnable command);

    /**
     * Creates a pending {@link Task.Orchestration} for a {@link Task}.
     *
     * @param task  the {@link Task}
     * @param <T>   the type result produced by the {@link Task}
     *
     * @return an {@link Task.Orchestration}
     *
     * @throws NullPointerException if task is {@code null}
     *
     * @since 14.1.2.0.0
     */
    <T> Task.Orchestration<T> orchestrate(Task<T> task);

    /**
     * Attempts to acquire the {@link Task.Coordinator} for a previously submitted
     * {@link Task}.
     *
     * @param taskId  the unique identity originally allocated to the {@link Task}
     *                (available by calling {@link Task.Coordinator#getTaskId()})
     * @param <T>     the type result produced by the {@link Task}
     *
     * @return the {@link Task.Coordinator} for the specified {@link Task} or
     *         {@code null} if the {@link Task} is unknown
     *
     * @throws NullPointerException if {@code sTaskId} is {@code null}
     *
     * @since 14.1.2.0.0
     */
    <T> Task.Coordinator<T> acquire(String taskId);

    /**
     * Returns {@code true} if this executor has been shut down.
     *
     * @return {@code true} if this executor has been shut down
     */
    boolean isShutdown();

    /**
     * Returns {@code true} if all tasks have completed following shut down.
     * Note that {@code isTerminated} is never {@code true} unless
     * either {@code shutdown} or {@code shutdownNow} was called first.
     *
     * @return {@code true} if all tasks have completed following shut down
     */
    boolean isTerminated();

    /**
     * Blocks until all tasks have completed execution after a shutdown
     * request, or the timeout occurs, or the current thread is
     * interrupted, whichever happens first.
     *
     * @param lcTimeout  the maximum time to wait
     * @param unit       the time unit of the timeout argument
     *
     * @return {@code true} if this executor terminated and
     *         {@code false} if the timeout elapsed before termination
     *
     * @throws InterruptedException if interrupted while waiting
     */
    boolean awaitTermination(long lcTimeout, TimeUnit unit)
            throws InterruptedException;

    /**
     * Initiates an orderly shutdown in which previously submitted
     * tasks are executed, but no new tasks will be accepted.
     * Invocation has no additional effect if already shut down.
     *
     * <p>This method does not wait for previously submitted tasks to
     * complete execution.  Use {@link #awaitTermination awaitTermination}
     * to do that.
     */
    void shutdown();

    /**
     * Attempts to stop all actively executing tasks, halts the
     * processing of waiting tasks, and returns a list of the tasks
     * that were awaiting execution.
     *
     * <p>This method does not wait for actively executing tasks to
     * terminate.  Use {@link #awaitTermination awaitTermination} to
     * do that.
     *
     * <p>There are no guarantees beyond best-effort attempts to stop
     * processing actively executing tasks.  For example, typical
     * implementations will cancel via {@link Thread#interrupt}, so any
     * task that fails to respond to interrupts may never terminate.
     *
     * @return list of tasks that never commenced execution
     */
    List<Runnable> shutdownNow();

    /**
     * Return the {@code RemoteExecutor} for the given name.  Will
     * return {@code null} if no {@link RemoteExecutor} is available
     * by the given name.
     *
     * @param sName  the {@code RemoteExecutor} name
     *
     * @return the {@code RemoteExecutor} for the given name.
     *
     * @throws NullPointerException     if {@code sName} is {@code null}
     * @throws IllegalArgumentException if {@code sName} is zero-length
     */
    static RemoteExecutor get(String sName)
        {
        Objects.requireNonNull(sName, "sName argument must not be null");

        if (sName.isEmpty())
            {
            throw new IllegalArgumentException("sName cannot be zero-length");
            }

        return new NamedClusteredExecutorService(Name.of(sName));
        }

    /**
     * Return the default executor.  This is a single-threaded executor
     * service that is registered at service start.
     *
     * @return the default executor; a single-threaded executor service
     *         that is registered at service start
     */
    static RemoteExecutor getDefault()
        {
        return get(DEFAULT_EXECUTOR_NAME);
        }

    // ----- constants ------------------------------------------------------

    /**
     * The name of the {@code default} executor; a single-threaded executor
     * on each member running the {@code coherence-concurrent} module.
     */
    String DEFAULT_EXECUTOR_NAME = "coherence-concurrent-default-executor";
    }
