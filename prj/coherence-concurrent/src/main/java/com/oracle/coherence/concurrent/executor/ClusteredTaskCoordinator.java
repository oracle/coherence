/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.executor;

import com.oracle.coherence.common.base.Logger;

import com.oracle.coherence.concurrent.executor.internal.ExecutorTrace;

import com.tangosol.net.CacheService;
import com.tangosol.net.MemberEvent;
import com.tangosol.net.MemberListener;
import com.tangosol.net.NamedCache;

import com.tangosol.util.MapEvent;
import com.tangosol.util.MapListener;

import com.tangosol.util.filter.NotFilter;
import com.tangosol.util.filter.PresentFilter;

import com.tangosol.util.processor.ConditionalPut;

import java.util.Iterator;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

/**
 * An implementation of a {@link Task.Coordinator} for Coherence-based implementation.
 *
 * @param <T>  the type of the {@link Task} result
 *
 * @version bo
 * @since 21.12
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class ClusteredTaskCoordinator<T>
        extends AbstractTaskCoordinator<T>
        implements MapListener
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a {@link ClusteredTaskCoordinator}.
     *
     * @param service          the {@link CacheService} containing the orchestration
     *                         metadata caches
     * @param manager          the {@link ClusteredTaskManager}
     * @param executorService  the {@link ExecutorService} from the
     *                         {@link TaskExecutorService} for asynchronously publishing
     */
    public ClusteredTaskCoordinator(CacheService service, ClusteredTaskManager manager, ExecutorService executorService)
        {
        super(manager.getTaskId(), executorService, manager.getRetainDuration() != null);

        m_taskManagers = service.ensureCache(ClusteredTaskManager.CACHE_NAME, null);

        // TODO - only add map listener if there is at least one subscriber
        m_taskManagers.addMapListener(this, getTaskId(), false);
        f_memberListener = new ClusteredMemberListener(this, service);
        addMemberListener(f_memberListener);
        }

    /**
     * Constructs a {@link ClusteredTaskCoordinator}.
     *
     * @param service          the {@link CacheService} containing the orchestration metadata caches
     * @param manager          the {@link ClusteredTaskManager}
     * @param executorService  the {@link ExecutorService} from the {@link TaskExecutorService}
     *                         for asynchronously publishing
     * @param properties       the {@link Task.Properties} of the task
     * @param subscribers      an optional set of subscribers to pre-register
     */
    public ClusteredTaskCoordinator(CacheService service, ClusteredTaskManager manager, ExecutorService executorService,
            Task.Properties properties, Iterator<Task.Subscriber<? super T>> subscribers)
        {
        super(manager.getTaskId(), executorService, manager.getRetainDuration() != null);

        m_taskManagers = service.ensureCache(ClusteredTaskManager.CACHE_NAME, null);
        m_properties   = properties;

        if (subscribers != null)
            {
            while (subscribers.hasNext())
                {
                Task.Subscriber<? super T> subscriber = subscribers.next();

                subscribe(subscriber);
                }
            }

        // TODO - only add map listener if there is at least one subscriber
        m_taskManagers.addMapListener(this, getTaskId(), false);
        f_memberListener = new ClusteredMemberListener(this, service);
        addMemberListener(f_memberListener);

        // attempt to add the task to the cluster
        ClusteredTaskManager existing =
                (ClusteredTaskManager) m_taskManagers.invoke(f_sTaskId,
                        new ConditionalPut(new NotFilter(new PresentFilter()), manager, true));

        if (existing != null)
            {
            // the task already exists, so clean up
            m_taskManagers.removeMapListener(this, getTaskId());

            Logger.warn(() -> String.format("Task with the identity [%s] already exists.  Task will not be created.",
                                            f_sTaskId));

            throw new IllegalArgumentException("Task with identity [" + f_sTaskId + "] already exists");
            }
        }

    // ----- AbstractTaskCoordinator methods --------------------------------

    @Override
    public boolean cancel(boolean fMayInterruptIfRunning)
        {
        return (Boolean) m_taskManagers.invoke(getTaskId(), new ClusteredTaskManager.TerminateProcessor(true));
        }

    @Override
    public Task.Properties getProperties()
        {
        synchronized (this)
            {
            if (m_properties == null)
                {
                m_properties = new ClusteredProperties(f_sTaskId, m_taskManagers.getCacheService());
                }
            }

        return m_properties;
        }

    @Override
    protected void subscribeRetainedTask(Task.Subscriber subscriber)
        {
        ClusteredTaskManager taskManager = (ClusteredTaskManager) m_taskManagers.get(f_sTaskId);
        if (taskManager != null)
            {
            try
                {
                Object result = taskManager.getLastResult().get();
                subscriber.onNext(result);
                }
            catch (Throwable t)
                {
                subscriber.onError(t);
                }

            subscriber.onComplete();
            }
        else
            {
            throw new IllegalStateException("Task : [" + f_sTaskId + "] not found.");
            }
        }

    /**
     * Closes this {@link ClusteredTaskCoordinator} and notifies the
     * {@link Task.Subscriber}s that there will no longer be any further items by calling
     * {@link Task.Subscriber#onComplete()}.
     */
    @Override
    public void close()
        {
        super.close();
        removeMemberListener(f_memberListener);
        }

    // ----- MapListener interface -------------------------------------------

    @Override
    public void entryInserted(MapEvent mapEvent)
        {
        // no-op
        }

    @Override
    public void entryUpdated(MapEvent mapEvent)
        {
        ClusteredTaskManager<?, ?, T> oldManager = (ClusteredTaskManager) mapEvent.getOldValue();
        ClusteredTaskManager<?, ?, T> manager    = (ClusteredTaskManager) mapEvent.getNewValue();

        int latestResultVersion = manager.getResultVersion();
        if (oldManager.getResultVersion() != latestResultVersion)
            {
            // remember this option as the last, so we can publish it to new subscribers
            Result<T> lastResult = m_lastValue = manager.getLastResult();

            ExecutorTrace.log(() -> String.format("Task [%s] has a new result[%s]: [%s]",
                                            getTaskId(), latestResultVersion, lastResult));

            offer(lastResult);
            }

        if (manager.isCancelled() || manager.isCompleted())
            {
            if (manager.isCancelled())
                {
                ExecutorTrace.log(() -> String.format("Task [%s] has been cancelled.", getTaskId()));

                super.cancel(true);
                }
            else
                {
                ExecutorTrace.log(() -> String.format("Task [%s] has been completed.", getTaskId()));

                close();
                }

            if (manager.getRunCompletionRunnable())
                {
                try
                    {
                    manager.getCompletionRunnable().accept(manager.getLastResult().get());
                    }
                catch (Throwable ignored)
                    {
                    // ignore - do not run completion on error
                    }
                manager.setRunCompletionRunnable(false);
                }

            // we no longer require the map listener
            removeMapListener();
            }
        }

    @Override
    public void entryDeleted(MapEvent mapEvent)
        {
        ExecutorTrace.log(() -> String.format("Task [%s] has been removed.", getTaskId()));

        // we no longer require the map listener
        removeMapListener();
        }

    // ----- public methods -------------------------------------------------

    /**
     * Adds the specified {@link MemberListener}.
     *
     * @param listener  the {@link MemberListener} to add
     */
    public void addMemberListener(MemberListener listener)
        {
        if (m_taskManagers != null)
            {
            m_taskManagers.getCacheService().addMemberListener(listener);
            }
        }

    /**
     * Removes the specified {@link MemberListener}.
     *
     * @param listener  the {@link MemberListener} to remove
     */
    public void removeMemberListener(MemberListener listener)
        {
        if (m_taskManagers != null)
            {
            m_taskManagers.getCacheService().removeMemberListener(listener);
            }
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Removes the {@link MapListener} previously installed by this
     * {@code ClusteredTaskCoordinator}.
     */
    protected void removeMapListener()
        {
        // remove the map listener
        final ClusteredTaskCoordinator coordinator = this;

        // create a Runnable to remove the MapListener
        Runnable removeMapListenerRunnable = () -> m_taskManagers.removeMapListener(coordinator, getTaskId());

        try
            {
            // attempt to perform the remove asynchronously
            f_executorService.submit(removeMapListenerRunnable);
            }
        catch (RejectedExecutionException e)
            {
            Logger.fine(() -> String.format("MapListener for Task [%s] could not be removed asynchronously.",
                                            getTaskId()));
            }
        }

    // ----- inner class: ClusteredMemberListener ---------------------------

    /**
     * A {@link MemberListener} to monitor the member this {@link ClusteredTaskCoordinator}
     * is part of / connected to in case of an *Extend client.
     */
    public class ClusteredMemberListener
            implements MemberListener
        {
        // ------ constructors ----------------------------------------------

        /**
         * Constructs a new {@code ClusteredMemberListener}.
         *
         * @param coordinator  the {@link ClusteredTaskCoordinator} to monitor
         * @param service      the associated executor {@link CacheService}
         */
        ClusteredMemberListener(ClusteredTaskCoordinator coordinator, CacheService service)
            {
            f_coordinator = coordinator;
            f_service     = service;
            }

        // ----- MemberListener interface -----------------------------------

        @Override
        public void memberLeaving(MemberEvent event)
            {
            // no-op
            }

        @Override
        public void memberLeft(MemberEvent event)
            {
            // no-op
            }

        @Override
        public void memberJoined(MemberEvent event)
            {
            // create a Runnable to add the MapListener
            Runnable addMapListenerRunnable = () ->
                {
                m_taskManagers = f_service.ensureCache(ClusteredTaskManager.CACHE_NAME, null);

                m_taskManagers.get(getTaskId());
                m_taskManagers.addMapListener(f_coordinator, getTaskId(), false);
                };

            try
                {
                // attempt to perform the add asynchronously
                f_executorService.submit(addMapListenerRunnable);
                }
            catch (RejectedExecutionException e)
                {
                Logger.fine(() -> String.format("MapListener for Task [%s] could not be added asynchronously.",
                                                getTaskId()));
                }
            }

        // ----- data members -----------------------------------------------

        /**
         * The {@link ClusteredTaskCoordinator}.
         */
        protected final ClusteredTaskCoordinator f_coordinator;

        /**
         * The {@link CacheService}.
         */
        protected final CacheService f_service;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The {@link ClusteredTaskManager}s cache.
     */
    @SuppressWarnings("rawtypes")
    protected NamedCache m_taskManagers;

    /**
     * The sharable {@link Task.Properties} for this task.
     */
    protected Task.Properties m_properties;

    /**
     * The member listener for cluster member.
     */
    protected final ClusteredMemberListener f_memberListener;
    }
