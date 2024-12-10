/*
 * Copyright (c) 2016, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.executor;

import com.tangosol.io.ExternalizableLite;

import com.tangosol.util.function.Remote.Predicate;

import java.io.Serializable;

import java.time.Duration;

import java.util.Objects;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A task which may take a long time to execute, may be executed by multiple
 * {@link Executor}s, may generate intermediate results, and may yield for
 * later execution.
 * <p>
 * Implementations define a single method called {@link #execute(Context)}
 * that performs the task, possibly yielding execution to some later point.
 * Once the method has completed execution, by returning a result or throwing
 * an exception (but not a Yield exception), the task is considered completed
 * for the assigned {@link Executor}.
 * </p>
 * <p>
 * {@link Task}s are like {@link Callable} and {@link Runnable} classes in
 * that they are designed to be potentially executed by one or more
 * {@link Thread}s, typically {@link Executor}s. Unlike {@link Callable} and
 * {@link Runnable} classes, the execution may occur in Java Virtual Machines,
 * fail and/or recover between different Java Virtual Machine processes.
 * As such, it is recommended to account for fail-over if a task is recovered.
 * Important execution state should be saved in the in the {@link Context}
 * {@link Context#getProperties() properties}.  Tasks should check
 * {@link Context#isResuming()} to determine if the task has failed over to
 * another executor and if execution state should be recovered from the
 * {@link Context} {@link Context#getProperties() properties}.
 * </p>
 *
 * @param <T>  the type of result produced by the {@link Task}
 *
 * @author bo
 * @since 21.12
 */
public interface Task<T>
        extends ExternalizableLite
    {
    /**
     * Executes the {@link Task}.
     *
     * @param context  the {@link Context}
     *
     * @return the result of executing the {@link Task}
     *
     * @throws Yield which indicates the {@link Task} execution should yield execution
     *         until some later point in time
     */
    T execute(Context<T> context) throws Exception;

    // ----- inner interface: Collectable -----------------------------------

    /**
     * Provides the mechanism to submit, orchestrate and collect results from a
     * {@link Task} defined as part of an {@link Orchestration}, including the creation
     * of a {@link Task.Coordinator} to subscribe to results, manage and interact with
     * an orchestrated {@link Task}.
     * <p>
     * A {@link Collectable} represents the last part of an {@link Orchestration}
     * definition pipeline.
     *
     * @param <T>  the type of the {@link Task}
     * @param <R>  the type of the result
     *
     * @see Orchestration
     */
    interface Collectable<T, R>
        {
        /**
         * Sets the {@link Predicate} to determine when the collection of results will be
         * considered completed, based on the collected result, after which no further
         * results will be published to {@link Subscriber}s.
         * <p>
         * When collection activity is completed, {@link Subscriber}s will then be notified by
         * calling {@link Subscriber#onComplete()}.
         *
         * @param predicate  the {@link Predicate} to determine when the {@link Task}
         *                   has completed (based on the collected results) or
         *                   <code>null</code> to indicate continuous collection and
         *                   publishing of results
         *
         * @return a {@link Collectable} to permit fluent-style method chaining
         */
        Completable<T, R> until(Predicate<? super R> predicate);

        /**
         * Provide a runnable to call when the task has completed.
         *
         * @param completionRunnable  the {@link Task.CompletionRunnable} to call when the
         *                            {@link Task} has completed
         *
         * @return a {@link Collectable} to permit fluent-style method chaining
         */
        Completable<T, R> andThen(Task.CompletionRunnable<? super R> completionRunnable);

        /**
         * Registers the specified {@link Task.Subscriber} as part of the orchestration
         * of the {@link Task}.
         * <p>
         * This method may be used multiple times to register multiple
         * {@link Subscriber}s.
         *
         * @param subscriber  the {@link Subscriber}
         *
         * @return the {@link Collectable} to permit fluent-style method chaining
         */
        Collectable<T, R> subscribe(Subscriber<? super R> subscriber);

        /**
         * Submits the {@link Task.Collectable} for orchestration by a
         * {@link TaskExecutorService}.
         *
         * @return a {@link Task.Coordinator} for managing an orchestrated {@link Task}
         *         and publishing of collected results to {@link Task.Subscriber}s
         */
        Coordinator<R> submit();
        }

    // ----- inner interface: Collector -------------------------------------

    /**
     * A mutable reduction operation that accumulates results into a mutable result
     * container, optionally transforming the accumulated result into a final
     * representation after all results have been processed.
     *
     * @param <T>  the type of input elements to the reduction operation
     * @param <A>  the mutable accumulation type of the reduction operation
     *             (often hidden as an implementation detail)
     * @param <R>  the result type of the reduction operation
     *
     * @see java.util.stream.Collector
     */
    interface Collector<T, A, R>
            extends ExternalizableLite
        {
        /**
         * A function that folds {@link Task} results into a mutable result container.
         *
         * @return a function that folds {@link Task} results into a mutable
         *         result container
         */
        BiConsumer<A, T> accumulator();

        /**
         * Perform the final transformation from the intermediate accumulation type
         * A to the final result type R.
         *
         * @return a function which transforms the intermediate result to the
         *         final result
         */
        Function<A, R> finisher();

        /**
         * A {@link Predicate} to determine if a result container can be finished early
         * avoiding further accumulation of results using the container.
         * <p>
         * Should there be no further results to accumulate, finishing the result
         * container with the {@link #finisher()} will occur regardless of the result
         * returned by the {@link Predicate}.
         *
         * @return a {@link Predicate}
         */
        Predicate<A> finishable();

        /**
         * A function that creates and returns a new mutable result container.
         *
         * @return a function which returns a new mutable result container
         */
        Supplier<A> supplier();
        }

    // ----- inner interface: Completable -----------------------------------

    /**
     * A {@link Collectable} that supports performing operations when a {@link Task}
     * orchestration is complete.
     *
     * @param <T>  the type of the {@link Task}
     * @param <R>  the type of the result
     */
    interface Completable<T, R>
            extends Collectable<T, R>
        {
        }

    // ----- inner interface: CompletionRunnable ----------------------------

    /**
     * A runnable to be called upon task completion.
     *
     * @param <T>  the type of result produced by the task.
     */
    interface CompletionRunnable<T>
            extends Consumer<T>, ExternalizableLite
        {
        }

    // ----- inner interface: Context ---------------------------------------

    /**
     * Provides contextual information for a {@link Task} as it is executed, including the ability to access and update
     * intermediate results for the {@link Executor} executing the said {@link Task}.
     *
     * @param <T>  the type of value produced by a {@link Task}
     */
    interface Context<T>
        {
        /**
         * Sets the intermediate result a {@link Task} has produced while being executed
         * by an {@link Executor} as part of an {@link Orchestration}.
         * <p>
         * The provided result may be later collected by an {@link TaskExecutorService}
         * according to the configured {@link Task.Collector} and then published to
         * registered {@link Task.Subscriber}s.
         * <p>
         * Multiple calls to this method are permitted during the execution of a
         * {@link Task}, thus permitting a stream of results to be collected and
         * thus published.
         *
         * @param result  the result
         */
        void setResult(T result);

        /**
         * Determines if a {@link Task} completed according to the
         * {@link TaskExecutorService}.
         * <p>
         * Completion may be due to normal termination, an exception or cancellation.
         * In all of these cases, this method will return <code>true</code>.
         *
         * @return <code>true</code> if the {@link Task} is considered completed
         *         <code>false</code> otherwise
         */
        boolean isDone();

        /**
         * Determines if a {@link Task} was cancelled.
         *
         * @return {@code true} if the task was cancelled
         *
         * @since 22.06
         */
        boolean isCancelled();

        /**
         * Determines if a {@link Task} execution by an {@link Executor} resuming
         * after being recovered or due to resumption after {@link Yield}ing.
         *
         * @return <code>true</code> if the {@link Task} is
         */
        boolean isResuming();

        /**
         * Obtain the properties of the {@link Task}.
         *
         * @return the task properties
         */
        Properties getProperties();

        /**
         * Obtains the unique identity of this {@link Task}.
         *
         * @return the task identity
         */
        String getTaskId();

        /**
         * Obtain the unique identity of the executing {@link Executor}.
         *
         * @return the executing {@link Executor}'s identity
         */
        String getExecutorId();
        }

    // ----- inner interface: Coordinator -----------------------------------

    /**
     * A publisher of collected {@link Task} results that additionally permits
     * coordination of the submitted {@link Task}.
     * <p>
     * Results collected by the execution of a {@link Task} using one or more
     * {@link Executor}s are sent to registered {@link Subscriber}s. Each registered
     * {@link Subscriber} receives the same results (via method
     * {@link Subscriber#onNext(Object)}) in the same order, unless drops or
     * errors are encountered.
     * <p>
     * If a {@link Coordinator} encounters an error that does not allow results to be
     * issued to a {@link Subscriber}, the {@link Subscriber#onError(Throwable)} is
     * called, and then receives no further results. Otherwise, when it is known that
     * no further results will be issued to it, the {@link Subscriber#onComplete()}
     * is called.
     * <p>
     * {@link Coordinator}s ensure that {@link Subscriber} method invocations for
     * each subscription are strictly ordered in happens-before order.
     *
     * @param <T>  the type of the collected results
     */
    interface Coordinator<T>
        {
        /**
         * Subscribes the specified {@link Subscriber} to the {@link Coordinator} of
         * a {@link Task} to receive collected results.
         * <p>
         * If already subscribed, or the attempt to subscribe fails, the
         * {@link Subscriber#onError(Throwable)} method is invoked with an
         * {@link IllegalStateException}.
         *
         * @param subscriber  the {@link Subscriber}
         */
        void subscribe(Subscriber<? super T> subscriber);

        /**
         * Attempts to cancel execution of the {@link Task}. This attempt will fail if
         * the task has already completed, has already been cancelled, or could not be
         * cancelled for some other reason. If successful, and this task has not started
         * when cancel is called, this task should never run. If the task has already
         * started, then the mayInterruptIfRunning parameter determines whether the
         * thread executing this task should be interrupted in an attempt to stop
         * the task.
         * <p>
         * After this method returns, subsequent calls to isDone() will always
         * return true. Subsequent calls to isDone() will always return true if this
         * method returned true.
         * </p>
         *
         * @param mayInterruptIfRunning  <code>true</code> if the thread executing this
         *                               task should be interrupted; otherwise,
         *                               in-progress tasks are allowed to complete
         *
         * @return <code>false</code> if the task could not be cancelled, typically
         *         because it has already completed normally; <code>true</code> otherwise
         *
         * @see java.util.concurrent.Future#cancel(boolean)
         */
        boolean cancel(boolean mayInterruptIfRunning);

        /**
         * Returns <code>true</code> if the {@link Task} was cancelled before
         * it completed normally.
         *
         * @return <code>true</code> if this task was cancelled before it completed
         *
         * @see java.util.concurrent.Future#isCancelled()
         */
        boolean isCancelled();

        /**
         * Returns <code>true</code> if the {@link Task} completed. Completion may be due
         * to normal termination, an exception, or cancellation -- in all of these cases,
         * this method will return <code>true</code>.
         *
         * @return <code>true</code> if this task completed
         *
         * @see java.util.concurrent.Future#isDone()
         */
        boolean isDone();

        /**
         * Obtains the unique identity of the {@link Task} being coordinated.
         *
         * @return the task identity
         */
        String getTaskId();

        /**
         * Obtain the properties of the {@link Task} being coordinated.
         *
         * @return the task properties
         */
        Properties getProperties();
        }

    // ----- inner interface: Option ----------------------------------------

    /**
     * An option for configuring {@link Task} orchestration.
     */
    interface Option
            extends ExternalizableLite
        {
        }

    // ----- inner interface: SubscribedOrchestration -----------------------

    /**
     * Defines the subset of {@link Orchestration} methods which are permitted after
     * {@link #subscribe(Subscriber)} is called.  Once {@link #subscribe(Subscriber)}
     * is called, only two methods are permitted: more {@link #subscribe(Subscriber)}
     * calls, or {@link #submit()}.
     *
     * @param <T>  the type of results produced by a {@link Task}
     *
     * @see Orchestration
     */
    interface SubscribedOrchestration<T>
        {
        /**
         * Registers the specified {@link Task.Subscriber} as part of the orchestration
         * of the {@link Task}.
         * <p>
         * This method may be used multiple times to register multiple {@link Subscriber}s.
         * <p>
         * This method is mutually exclusive to {@link Orchestration#collect(Collector)}.
         * To subscribe when using a {@link Collector}, use
         * {@link Collectable#subscribe(Subscriber)} on the {@link Collectable} returned
         * by {@link Orchestration#collect(Collector)}.
         *
         * @param subscriber  the {@link Subscriber}
         *
         * @return the {@link Orchestration} to permit fluent-style method chaining
         *
         * @see Collectable#subscribe(Subscriber)
         */
        SubscribedOrchestration<T> subscribe(Subscriber<? super T> subscriber);

        /**
         * Submits the {@link Task} for orchestration by the {@link TaskExecutorService}.
         *
         * @return a {@link Task.Coordinator} for managing an orchestrated {@link Task}
         *         and publishing of collected results to {@link Task.Subscriber}s
         *
         * @see Collectable#subscribe(Subscriber)
         */
        Coordinator<T> submit();
        }

    // ----- inner interface: Orchestration ---------------------------------

    /**
     * Defines information concerning the orchestration of a {@link Task} across a set
     * of {@link Executor}s for a given {@link TaskExecutorService}.   This information
     * represents the first part of an orchestration pipeline definition, which is
     * agnostic to the final type of result.  The optional second and last part of an
     * orchestration pipeline is defined by the {@link Collectable}, produced when
     * {@link #collect(Collector)} is called.  {@link #subscribe(Subscriber)} (optional)
     * and {@link #submit()} may be called directly on the {@link Orchestration} if using
     * a {@link Collector} is not desired.
     *
     * @param <T>  the type of results produced by a {@link Task}
     *
     * @see Collectable
     */
    interface Orchestration<T>
        extends SubscribedOrchestration<T>
        {
        /**
         * Specify that the {@link Task} should be executed concurrently on its
         * assigned {@link Executor}s.
         * <p>
         * This is the default setting for execution.
         *
         * @return the {@link Orchestration} to permit fluent-style method chaining
         */
        Orchestration<T> concurrently();

        /**
         * Specify that the {@link Task} should be executed sequentially on its assigned
         * {@link Executor}s.
         *
         * @return the {@link Orchestration} to permit fluent-style method chaining
         */
        Orchestration<T> sequentially();

        /**
         * Limit the {@link Executor}s to only those that satisfy the specified
         * {@link Predicate}.
         *
         * @param predicate  the {@link TaskExecutorService.ExecutorInfo} predicate
         *
         * @return the {@link Orchestration} to permit fluent-style method chaining
         */
        Orchestration<T> filter(Predicate<? super TaskExecutorService.ExecutorInfo> predicate);

        /**
         * Limit the number of {@link Executor}s to use when executing the {@link Task}.
         *
         * @param cLimit  the number of {@link Executor}s to use
         *
         * @return the {@link Orchestration} to permit fluent-style method chaining
         */
        Orchestration<T> limit(int cLimit);

        /**
         * Sets the unique identity for the {@link Task} when it is orchestrated.
         *
         * @param sIdentity  the unique identity for the {@link Task}
         *
         * @return the {@link Orchestration} to permit fluent-style method chaining
         */
        Orchestration<T> as(String sIdentity);

        /**
         * Sets the {@link Option}s for orchestration.
         *
         * @param options  the {@link Option}
         *
         * @return the {@link Orchestration} to permit fluent-style method chaining
         */
        Orchestration<T> with(Option... options);

        /**
         * Defines the {@link Properties} for the {@link Task}.
         *
         * @param sName  the name of the property
         * @param value  the value of the property
         * @param <V>    the value type of the property
         *
         * @return the {@link Orchestration} to permit fluent-style method chaining
         */
        <V extends Serializable> Orchestration<T> define(String sName, V value);

        /**
         * Sets the {@link Duration} to retain the task after it is completed.
         *
         * @param duration  the duration to retain the task after it is completed.
         *
         * @return the {@link Orchestration} to permit fluent-style method chaining
         */
        Orchestration<T> retain(Duration duration);

        /**
         * Creates a {@link Collectable} for the {@link Task} using the specified
         * {@link Collector}, that will collect individual results from {@link Task}s
         * when they are executed with orchestrated {@link Executor}s.
         *
         * @param collector  the {@link Collector}
         * @param <R>        the collected result type
         *
         * @return the {@link Collectable} for the {@link Task}
         */
        <R> Collectable<T, R> collect(Collector<? super T, ?, R> collector);
        }

    // ----- inner interface: Properties ------------------------------------

    /**
     * Define an interface to allow states sharing between the task executors.
     */
    interface Properties
        {
        /**
         * Get the property of a given key.
         *
         * @param sKey  the key of the property
         * @param <V>   the value type of the property
         *
         * @return the value of the property
         */
        <V extends Serializable> V get(String sKey);

        /**
         * Put a property with the given name and value.
         *
         * @param sKey   the key of the property
         * @param value  the value of the property
         * @param <V>    the value type of the property
         *
         * @return the previous value of the property or null if it's a new property
         */
        <V extends Serializable> V put(String sKey, V value);
        }

    // ----- inner interface: Subscriber ------------------------------------

    /**
     * A receiver of items produced by a {@link Coordinator}.
     *
     * @param <T>  the type of result received
     */
    interface Subscriber<T>
        {
        /**
         * Invoked by a {@link Coordinator} when it is known that no additional
         * {@link Subscriber} method invocations will occur or has already been
         * terminated by an error.
         * <p>
         * After this method is invoked no other {@link Subscriber} methods will be called.
         * <p>
         * If this method throws an exception, the {@link Subscriber} will be closed.
         */
        void onComplete();

        /**
         * Invoked by a {@link Coordinator} when an unrecoverable error was encountered,
         * after which no other {@link Subscriber} methods are invoked.
         * <p>
         * If this method throws an exception, the {@link Subscriber} will be closed.
         *
         * @param throwable  the error
         */
        void onError(Throwable throwable);

        /**
         * Invoked when a {@link Coordinator} has produced an item for consumption.
         * <p>
         * If this method throws an exception, the {@link Subscriber} will be closed.
         *
         * @param item  the item (possibly <code>null</code>)
         */
        void onNext(T item);

        /**
         * Invoked prior to the {@link Subscriber} methods {@link #onComplete()},
         * {@link #onError(Throwable)} and {@link #onNext(Object)} being invoked for
         * a {@link Subscription} to a {@link Coordinator}.
         *
         * @param subscription  the {@link Subscription}
         */
        void onSubscribe(Subscription<? extends T> subscription);
        }

    // ----- inner interface: Subscription ----------------------------------

    /**
     * Represents a subscription a {@link Subscriber} has made to a {@link Coordinator},
     * allowing control and termination of the {@link Subscription}.
     *
     * @param <T>  the type of result received by the {@link Subscriber}
     */
    interface Subscription<T>
        {
        /**
         * Causes the {@link Subscriber} to (eventually) stop receiving items from
         * a {@link Coordinator}.
         */
        @SuppressWarnings("unused")
        void cancel();

        /**
         * Obtains the {@link Coordinator} to which the {@link Subscriber} is subscribed.
         *
         * @return the {@link Coordinator}
         */
        Coordinator<T> getCoordinator();
        }

    // ----- inner class: Yield ---------------------------------------------

    /**
     * An exception signalling the execution of a {@link Task} by an {@link Executor}
     * is to be suspended and resumed at some later point in time, typically by
     * the same {@link Executor}.
     */
    class Yield
            extends Exception
        {
        // ----- constructors -----------------------------------------------

        /**
         * Constructs a {@link Yield}.
         *
         * @param duration  the {@link Duration} to yield
         */
        private Yield(Duration duration)
            {
            m_duration = duration;
            }

        // ----- public methods ---------------------------------------------

        /**
         * Obtains the {@link Duration} for the {@link Yield}.
         *
         * @return the {@link Duration}
         */
        public Duration getDuration()
            {
            return m_duration;
            }

        /**
         * Constructs a {@link Yield} for the specified duration.
         *
         * @param duration  the {@link Duration}
         *
         * @return a {@link Yield}
         */
        public static Yield atLeast(Duration duration)
            {
            return new Yield(duration);
            }

        // ----- Object methods ---------------------------------------------

        @Override
        public boolean equals(Object object)
            {
            if (this == object)
                {
                return true;
                }

            if (object == null || getClass() != object.getClass())
                {
                return false;
                }

            Yield yield = (Yield) object;

            return Objects.equals(m_duration, yield.m_duration);
            }

        @Override
        public int hashCode()
            {
            return m_duration != null ? m_duration.hashCode() : 0;
            }

        // ----- data members -----------------------------------------------

        /**
         * The minimum duration to yield.
         */
        protected final Duration m_duration;
        }
    }
