/*
 * Copyright (c) 2016, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.executor;

import com.oracle.coherence.common.base.Logger;

import com.oracle.coherence.concurrent.executor.internal.ExecutorTrace;

import java.util.Set;

import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A base implementation of a {@link Task.Coordinator}.
 *
 * @param <T>  the type of the {@link Task}
 *
 * @author bo
 * @since 21.12
 */
public abstract class AbstractTaskCoordinator<T>
        implements Task.Coordinator<T>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct an {@link AbstractTaskCoordinator}.
     *
     * @param taskId           {@link Task} ID
     * @param executorService  {@link ExecutorService} to use for async notifications to {@link Task.Coordinator}s
     * @param fRetainTask      whether to retain the {@link Task} after completion
     */
    public AbstractTaskCoordinator(String          taskId,
                                   ExecutorService executorService,
                                   boolean         fRetainTask)
        {
        f_sTaskId         = taskId;
        f_fRetainTask     = fRetainTask;
        m_setSubscribers  = new CopyOnWriteArraySet<>();
        m_lastValue       = Result.none();
        f_cancelled       = new AtomicBoolean(false);
        f_closed          = new AtomicBoolean(false);
        f_executorService = executorService;
        }

    // ----- Task.Coordinator methods ---------------------------------------

    @Override
    public String getTaskId()
        {
        return f_sTaskId;
        }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning)
        {
        if (!f_closed.get())
            {
            if (f_cancelled.compareAndSet(false, true))
                {
                close();

                return true;
                }
            }

        return false;
        }

    @Override
    public boolean isCancelled()
        {
        return f_cancelled.get();
        }

    @Override
    public boolean isDone()
        {
        return f_closed.get();
        }

    @Override
    public void subscribe(final Task.Subscriber<? super T> subscriber)
        {
        subscriber.onSubscribe(new Task.Subscription<T>()
            {
            // ----- Task.Subscription interface ----------------------------

            @Override
            public void cancel()
                {
                m_setSubscribers.remove(subscriber);
                }

            @Override
            public Task.Coordinator<T> getCoordinator()
                {
                return AbstractTaskCoordinator.this;
                }
            });

        // assume we need to add the subscriber
        boolean addSubscriber = true;

        if (f_closed.get())
            {
            addSubscriber = false;

            if (f_fRetainTask)
                {
                subscribeRetainedTask(subscriber);
                }

            // create a Runnable to close the subscriber
            Runnable closeSubscriberRunnable = () ->
            {
            if (m_lastValue != null)
                {
                Throwable throwable = null;
                T         value     = null;

                try
                    {
                    value = m_lastValue.get();
                    }
                catch (Throwable t)
                    {
                    throwable = t;
                    }

                try
                    {
                    if (throwable == null)
                        {
                        subscriber.onNext(value);
                        }
                    else
                        {
                        subscriber.onError(throwable);
                        }
                    }
                catch (Throwable ignored)
                    {
                    // ignore exceptions thrown by subscriber
                    }
                }
            closeSubscriber(subscriber, false);
            };

            try
                {
                // attempt to close the subscriber asynchronously
                f_executorService.submit(closeSubscriberRunnable);
                }
            catch (RejectedExecutionException e)
                {
                // the executor can't close the subscriber, so let's close it ourselves
                closeSubscriberRunnable.run();
                }
            }

        if (addSubscriber)
            {
            m_setSubscribers.add(subscriber);
            }
        }

    // ----- AbstractTaskCoordinator methods ---------------------------------

    /**
     * A helper method to close a {@link Task.Subscriber} using the calling {@link Thread}.
     * <p>
     * Closing a {@link Task.Subscriber} involves calling either {@link Task.Subscriber#onComplete()}
     * or {@link Task.Subscriber#onError(Throwable)} when an error occurred.
     *
     * @param subscriber  the {@link Task.Subscriber}
     * @param fRemove     {@code true} if the {@link Task.Subscriber} should additionally
     *                    be removed from the {@link Task.Coordinator},{@code false} otherwise
     */
    protected void closeSubscriber(Task.Subscriber<? super T> subscriber,
                                   boolean fRemove)
        {
        ExecutorTrace.entering(AbstractTaskCoordinator.class, "closeSubscriber", subscriber, getTaskId(), m_lastValue);

        if (fRemove)
            {
            ExecutorTrace.log(() -> String.format("Removing Subscriber %s", subscriber));

            m_setSubscribers.remove(subscriber);

            ExecutorTrace.log(() -> String.format("Removed Subscriber %s", subscriber));
            }

        // notify the subscriber that it's closed.
        try
            {
            if (f_cancelled.get())
                {
                ExecutorTrace.log(() -> String.format("Notifying Subscriber %s of cancellation", subscriber));
                subscriber.onError(new InterruptedException("Task " + getTaskId() + " has been cancelled."));
                }
            else if (m_lastValue != null && m_lastValue.isValue())
                {
                // only call onComplete() if the task returned a value
                subscriber.onComplete();
                }
            else
                {
                ExecutorTrace.log(() -> String.format("Subscriber %s of closed, but no results received.", subscriber));
                }
            }
        catch (Throwable throwable)
            {
            Logger.warn(() -> String.format("Failed to close subscriber %s", subscriber));
            ExecutorTrace.throwing(AbstractTaskCoordinator.class, "closeSubscriber", throwable);

            // we always remove when an error occurs
            m_setSubscribers.remove(subscriber);

            try
                {
                subscriber.onError(throwable);
                }
            catch (Throwable ignored)
                {
                // nothing can be done when the subscriber can't handle an error
                }
            }

        ExecutorTrace.exiting(AbstractTaskCoordinator.class, "closeSubscriber");
        }

    /**
     * Closes the {@link AbstractTaskCoordinator} and notifies the {@link Task.Subscriber}s that there will no longer be
     * any further items by calling {@link Task.Subscriber#onComplete()}.
     */
    public void close()
        {
        if (f_closed.compareAndSet(false, true))
            {
            ExecutorTrace.log("Scheduling the closing of subscribers");

            // create a Runnable to close the subscribers
            Runnable closeSubscribersRunnable = () ->
            {
            for (Task.Subscriber<? super T> subscriber : m_setSubscribers)
                {
                closeSubscriber(subscriber, true);
                }
            };

            try
                {
                // attempt to close the subscribers asynchronously
                // (we do this as some subscribers may still be receiving data, which must occur before closing)
                f_executorService.submit(closeSubscribersRunnable);
                }
            catch (RejectedExecutionException e)
                {
                // the executor rejected the request, so attempt to close the subscribers ourselves
                closeSubscribersRunnable.run();
                }
            }
        else
            {
            ExecutorTrace.log("Skipped closing subscribers as the coordinator is already closed");
            }
        }

    /**
     * Determine if the {@link AbstractTaskCoordinator} currently has any {@link Task.Subscriber}s.
     *
     * @return <code>true</code> if the {@link AbstractTaskCoordinator} has {@link Task.Subscriber}s,
     * <code>false</code> otherwise
     */
    public boolean hasSubscribers()
        {
        return !m_setSubscribers.isEmpty();
        }

    /**
     * Offers an item to be asynchronously published to current {@link Task.Subscriber}s.
     *
     * @param item the item
     */
    public void offer(final Result<T> item)
        {
        ExecutorTrace.entering(AbstractTaskCoordinator.class, "offer", item);

        if (f_closed.get())
            {
            // ignore the offer when the publisher is closed
            }
        else
            {
            if (hasSubscribers())
                {
                // create a Runnable that will offer the result to the subscribers
                Runnable offerRunnable = () ->
                    {
                    T         result = null;
                    Throwable resultError = null;
                    try
                        {
                        result = item.get();
                        }
                    catch (Throwable t)
                        {
                        resultError = t;
                        }

                    for (Task.Subscriber<? super T> subscriber : m_setSubscribers)
                        {
                        try
                            {
                            if (resultError == null)
                                {
                                subscriber.onNext(result);
                                }
                            else
                                {
                                subscriber.onError(resultError);
                                }
                            }
                        catch (Exception e)
                            {
                            if (Logger.isEnabled(Logger.WARNING))
                                {
                                String sMsg = "Task [%s]: removing subscriber [%s] as it threw an"
                                              + " exception processing result: [%s]";
                                Logger.warn(String.format(sMsg, getTaskId(), subscriber, item), e);
                                }

                            // when the subscriber throws an exception consuming the item
                            // we remove it as it's probably faulty
                            m_setSubscribers.remove(subscriber);
                            }
                        }
                    };

                try
                    {
                    // attempt to perform the offer asynchronously
                    f_executorService.submit(offerRunnable);
                    }
                catch (RejectedExecutionException e)
                    {
                    // can't perform the offer asynchronously, so try on the calling thread
                    offerRunnable.run();
                    }
                }
            }

        ExecutorTrace.exiting(AbstractTaskCoordinator.class, "offer");
        }

    // ----- abstract methods -----------------------------------------------

    /**
     * Subscribes to a retained task (a task that has completed execution, but
     * it still held in memory).
     *
     * @param subscriber  the subscriber
     *
     * @throws IllegalStateException if there is no task to subscribe to
     */
    protected abstract void subscribeRetainedTask(Task.Subscriber<?> subscriber);

    // ----- data members ---------------------------------------------------

    /**
     * The {@link ExecutorService} from the {@link TaskExecutorService} for asynchronously publishing items.
     */
    protected final ExecutorService f_executorService;

    /**
     * The unique identity of the {@link Task}.
     */
    protected final String f_sTaskId;

    /**
     * The flag to indicate whether the {@link Task} is retained.
     */
    protected final boolean f_fRetainTask;

    /**
     * Is the {@link Task.Coordinator} closed to accept more subscribers or offers of items.
     */
    protected final AtomicBoolean f_closed;

    /**
     * Has the {@link Task} been cancelled.
     */
    protected final AtomicBoolean f_cancelled;

    /**
     * The {@link Task.Subscriber}s to which items will be published.
     */
    protected Set<Task.Subscriber<? super T>> m_setSubscribers;

    /**
     * The last value offered (that we'll offer to new {@link Task.Subscriber}s).
     */
    protected volatile Result<T> m_lastValue;
    }
