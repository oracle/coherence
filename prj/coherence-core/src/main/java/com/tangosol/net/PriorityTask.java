/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net;

/**
* The PriorityTask interface allows to control the ordering in which a
* service schedules tasks for execution using a thread pool and limit their
* execution times to a specified duration. Instances of PriorityTask typically
* also implement either {@link Invocable} or Runnable interface.
* <p>
* Depending on the value returned by the {@link #getSchedulingPriority()}
* method, the scheduling order will be one of the following:
* <ul>
* <li> {@link #SCHEDULE_STANDARD} - a task will be scheduled for execution in
*      a natural (based on the request arrival time) order;
* <li> {@link #SCHEDULE_FIRST} - a task will be scheduled in front of any
*      equal or lower scheduling priority tasks and executed as soon as any of
*      worker threads become available;
* <li> {@link #SCHEDULE_IMMEDIATE} - a task will be immediately executed by
*      any idle worker thread; if all of them are active, a new thread will be
*      created to execute this task.
* </ul>
*
* A best effort will be made to limit the task execution time according to the
* value returned by the {@link #getExecutionTimeoutMillis()} method. However,
* it should be noted that:
* <ul>
* <li> for tasks with the scheduling priority of SCHEDULE_IMMEDIATE, factors
*      that could make the execution time longer than the timeout value are
*      long GC pauses and high network latency;
* <li> if the service has a task backlog (when there are more tasks scheduled
*      for execution than the number of available worker threads), the
*      request execution time (measured from the client's perspective) for
*      tasks with the scheduling priorities of SCHEDULE_STANDARD or
*      SCHEDULE_FIRST could be longer and include the time those tasks were
*      kept in a queue before invocation;
* <li> the corresponding service is free to cancel the task execution before
*      the task is started and call the {@link #runCanceled} method if it's
*      known that the client is no longer interested in the results of the
*      task execution.
* </ul>
*
* In addition to allowing control of the task execution (as scheduled and
* measured on the server side), the PriorityTask interface could also be used to
* control the request time from the calling thread perspective (measured on the
* client). A best effort will be made to limit the request time (the time period
* that the calling thread is blocked waiting for a response from the
* corresponding service) to the value returned by the
* {@link #getRequestTimeoutMillis()} method.
* <p>
* It should be noted that the request timeout value (RT) could be grater than,
* equal to or less than the task execution timeout value (ET). The value of RT
* which is less than ET indicates that even though the task execution is
* allowed to take longer period of time, the client thread will not wait for a
* result of the execution and will be able to handle a timeout exception if it
* arises. Since the time spent by the task waiting in the service backlog queue
* does not count toward the task execution time, a value of RT that is equal or
* slightly greater than ET still leaves a possibility that the client thread
* will throw a TimeoutException before the task completes its execution normally
* on a server.
*
* @author gg 2006.11.02
* @since Coherence 3.3
*/

public interface PriorityTask
    {
    /**
    * Obtain this task's scheduling priority. Valid values are one of the
    * SCHEDULE_* constants.
    *
    * @return  this task's scheduling priority
    */
    public int getSchedulingPriority();

    /**
    * Obtain the maximum amount of time this task is allowed to run before
    * the corresponding service will attempt to stop it.
    * <p>
    * The value of {@link #TIMEOUT_DEFAULT TIMEOUT_DEFAULT} indicates a default
    * timeout value configured for the corresponding service; the value of
    * {@link #TIMEOUT_NONE TIMEOUT_NONE} indicates that this task can execute
    * indefinitely.
    * <p>
    * If, by the time the specified amount of time passed, the task has not
    * finished, the service will attempt to stop the execution by using the
    * {@link Thread#interrupt()} method. In the case that interrupting the
    * thread does not result in the task's termination, the
    * {@link #runCanceled} method will be called.
    *
    * @return the execution timeout value in milliseconds or one of the
    *          special TIMEOUT_* values
    */
    public long getExecutionTimeoutMillis();

    /**
    * Obtain the maximum amount of time a calling thread is willing to wait for
    * a result of the request execution. The request time is measured on the
    * client side as the time elapsed from the moment a request is sent for
    * execution to the corresponding server node(s) and includes:
    * <ul>
    * <li> the time it takes to deliver the request to the executing node(s);
    * <li> the interval between the time the task is received and placed
    *      into a service queue until the execution starts;
    * <li> the task execution time;
    * <li> the time it takes to deliver a result back to the client.
    * </ul>
    * <p>
    * The value of {@link #TIMEOUT_DEFAULT TIMEOUT_DEFAULT} indicates a default
    * timeout value configured for the corresponding service; the value of
    * {@link #TIMEOUT_NONE TIMEOUT_NONE} indicates that the client thread is
    * willing to wait indefinitely until the task execution completes or is
    * canceled by the service due to a task execution timeout specified by the
    * {@link #getExecutionTimeoutMillis()} value.
    * <p>
    * If the specified amount of time elapsed and the client has not received
    * any response from the server, an {@link RequestTimeoutException} will
    * be thrown to the caller.
    *
    * @return the request timeout value in milliseconds or one of the
    *          special TIMEOUT_* values
    */
    public long getRequestTimeoutMillis();

    /**
    * This method will be called if and only if all attempts to interrupt this
    * task were unsuccessful in stopping the execution or if the execution was
    * canceled <b>before</b> it had a chance to run at all.
    * <p>
    * Since this method is usually called on a service thread, implementors
    * must exercise extreme caution since any delay introduced by the
    * implementation will cause a delay of the corresponding service.
    *
    * @param fAbandoned true if the task has timed-out, but all attempts to
    *        interrupt it were unsuccessful in stopping the execution;
    *        otherwise the task was never started
    */
    public void runCanceled(boolean fAbandoned);


    // ----- constants ------------------------------------------------------

    /**
    * Scheduling value indicating that this task is to be queued and executed
    * in a natural (based on the request arrival time) order.
    */
    public final static int SCHEDULE_STANDARD = 0;

    /**
    * Scheduling value indicating that this task is to be queued in front of
    * any equal or lower scheduling priority tasks and executed as soon as any
    * of the worker threads become available.
    */
    public final static int SCHEDULE_FIRST = 1;

    /**
    * Scheduling value indicating that this task is to be immediately executed
    * by any idle worker thread; if all of them are active, a new thread will
    * be created to execute this task.
    */
    public final static int SCHEDULE_IMMEDIATE = 2;

    /**
    * A special timeout value to indicate that the corresponding service's
    * default timeout value should be used.
    */
    public static final long TIMEOUT_DEFAULT = 0L;

    /**
    * A special timeout value to indicate that this task or request can run
    * indefinitely.
    */
    public static final long TIMEOUT_NONE   = -1L;
    }