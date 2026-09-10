/*
 * Copyright (c) 2016, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.executor;

import com.oracle.coherence.common.base.Logger;

import com.oracle.coherence.concurrent.executor.internal.ExecutorTrace;

import com.oracle.coherence.concurrent.executor.util.Caches;

import com.tangosol.internal.tracing.Scope;
import com.tangosol.internal.tracing.Span;
import com.tangosol.internal.tracing.SpanContext;
import com.tangosol.internal.tracing.TracingHelper;

import com.tangosol.net.CacheService;
import com.tangosol.net.MemberEvent;
import com.tangosol.net.MemberListener;
import com.tangosol.net.NamedCache;
import com.tangosol.net.ServiceInfo;

import com.tangosol.util.MapEvent;
import com.tangosol.util.MapListener;

import com.tangosol.util.filter.NotFilter;
import com.tangosol.util.filter.PresentFilter;

import com.tangosol.util.processor.ConditionalPut;

import java.util.Iterator;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

import javax.security.auth.Subject;

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
     * This constructor should only be used to obtain a coordinator for a previously
     * submitted task.
     *
     * @param service          the {@link CacheService} containing the orchestration
     *                         metadata caches
     * @param manager          the {@link ClusteredTaskManager}
     * @param executorService  the {@link ExecutorService} from the
     *                         {@link TaskExecutorService} for asynchronous publishing
     */
    public ClusteredTaskCoordinator(CacheService service, ClusteredTaskManager manager, ExecutorService executorService)
        {
        super(manager.getTaskId(), executorService, manager.getRetainDuration() != null);

        f_cacheService = service;
        f_subject      = manager.getSubject();
        m_nResultVersion = manager.getResultVersion();

        // TODO - only add map listener if there is at least one subscriber
        Caches.tasks(service).addMapListener(this, getTaskId(), false);
        f_memberListener = new ClusteredMemberListener(service);
        addMemberListener(f_memberListener);
        }

    /**
     * Constructs a {@link ClusteredTaskCoordinator}.
     *
     * @param service          the {@link CacheService} containing the orchestration metadata caches
     * @param manager          the {@link ClusteredTaskManager}
     * @param executorService  the {@link ExecutorService} from the {@link TaskExecutorService}
     *                         for asynchronous publishing
     * @param properties       the {@link Task.Properties} of the task
     * @param subscribers      an optional set of subscribers to pre-register
     */
    public ClusteredTaskCoordinator(CacheService service, ClusteredTaskManager manager, ExecutorService executorService,
            Task.Properties properties, Iterator<Task.Subscriber<? super T>> subscribers)
        {
        super(manager.getTaskId(), executorService, manager.getRetainDuration() != null);

        f_cacheService = service;
        m_properties   = properties;
        f_subject      = manager.getSubject();
        m_nResultVersion = manager.getResultVersion();

        if (subscribers != null)
            {
            while (subscribers.hasNext())
                {
                Task.Subscriber<? super T> subscriber = subscribers.next();

                subscribe(subscriber);
                }
            }

        // TODO - only add map listener if there is at least one subscriber
        Caches.tasks(getCacheService()).addMapListener(this, getTaskId(), false);
        f_memberListener = new ClusteredMemberListener(service);
        addMemberListener(f_memberListener);

        // attempt to add the task to the cluster
        ClusteredTaskManager existing =
                (ClusteredTaskManager) Caches.tasks(getCacheService()).invoke(f_sTaskId,
                        new ConditionalPut(new NotFilter(new PresentFilter()), manager, true));

        if (existing != null)
            {
            // the task already exists, so clean up
            Caches.tasks(getCacheService()).removeMapListener(this, getTaskId());

            Logger.warn(() -> String.format("Task with the identity [%s] already exists.  Task will not be created.",
                                            f_sTaskId));

            throw new IllegalArgumentException("Task with identity [" + f_sTaskId + "] already exists");
            }
        }

    // ----- AbstractTaskCoordinator methods --------------------------------

    @Override
    public void subscribe(Task.Subscriber<? super T> subscriber)
        {
        ConcurrentTaskInstallGate.enforceSubscriber(subscriber, f_subject);
        super.subscribe(subscriber);
        }

    @Override
    public boolean cancel(boolean fMayInterruptIfRunning)
        {
        String               sTaskId     = getTaskId();
        CacheService         service     = getCacheService();
        ClusteredTaskManager taskManager = (ClusteredTaskManager) Caches.tasks(service).get(sTaskId);

        if (taskManager != null)
            {
            SpanContext  parentSpanContext = taskManager.getParentSpanContext();
            Span.Builder builder           = TracingHelper.newSpan("Task.Cancel")
                                                 .withAssociation(Span.Association.FOLLOWS_FROM.key(), parentSpanContext)
                                                 .withMetadata(Span.Type.COMPONENT.key(), "ExecutorService");
            Span         executionSpan     = builder.startSpan();

            try (Scope ignored = TracingHelper.getTracer().withSpan(executionSpan))
                {
                boolean fResult = (boolean) Caches.tasks(service).invoke(sTaskId, new ClusteredTaskManager.CancellationProcessor());

                if (fMayInterruptIfRunning)
                    {
                    // Notify the clustered registrations running assignments
                    // to interrupt the execution thread
                    ClusteredAssignment.cancelAssignments(sTaskId, service);
                    }

                return fResult;
                }
            finally
                {
                executionSpan.end();
                }
            }

        return false;
        }

    @Override
    public Task.Properties getProperties()
        {
        synchronized (this)
            {
            if (m_properties == null)
                {
                m_properties = new ClusteredProperties(f_sTaskId, getCacheService());
                }
            }

        return m_properties;
        }

    @Override
    protected void subscribeRetainedTask(Task.Subscriber subscriber)
        {
        ClusteredTaskManager taskManager = (ClusteredTaskManager) Caches.tasks(getCacheService()).get(f_sTaskId);
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
        ExecutorTrace.entering(this.getClass(), "entryUpdated",() -> mapEvent);

        processTaskUpdate((ClusteredTaskManager<?, ?, T>) mapEvent.getNewValue());

        ExecutorTrace.exiting(this.getClass(), "entryUpdated");
        }

    @Override
    public void entryDeleted(MapEvent mapEvent)
        {
        ExecutorTrace.log(() -> String.format("Task [%s] has been removed.", getTaskId()));

        if (!isDone())
            {
            closeExceptionally(new IllegalStateException("Task [" + getTaskId()
                    + "] was removed before its completion could be observed."));
            }
        else
            {
            removeMapListener();
            }
        }

    // ----- public methods -------------------------------------------------

    /**
     * Adds the specified {@link MemberListener}.
     *
     * @param listener  the {@link MemberListener} to add
     */
    public void addMemberListener(MemberListener listener)
        {
        getCacheService().addMemberListener(listener);
        }

    /**
     * Removes the specified {@link MemberListener}.
     *
     * @param listener  the {@link MemberListener} to remove
     */
    public void removeMemberListener(MemberListener listener)
        {
        getCacheService().removeMemberListener(listener);
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Reconcile the latest task state, including results that may have been
     * produced while an Extend connection was unavailable.
     *
     * @param manager  the latest task state
     */
    protected synchronized void processTaskUpdate(ClusteredTaskManager<?, ?, T> manager)
        {
        if (isDone())
            {
            return;
            }

        int latestResultVersion = manager.getResultVersion();
        if (latestResultVersion > m_nResultVersion)
            {
            m_nResultVersion = latestResultVersion;

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

    /**
     * Close this coordinator when a task can no longer be recovered.
     *
     * @param throwable  the recovery failure
     */
    protected synchronized void closeExceptionally(Throwable throwable)
        {
        super.close(throwable);
        removeMemberListener(f_memberListener);
        removeMapListener();
        }

    /**
     * Reinstall the task listener after a remote-service topology change and
     * reconcile the state that may have changed while the listener was absent.
     *
     */
    protected void recover()
        {
        if (isDone())
            {
            return;
            }

        try
            {
            NamedCache tasks = Caches.tasks(getCacheService());
            tasks.addMapListener(this, getTaskId(), false);

            ClusteredTaskManager<?, ?, T> manager =
                    (ClusteredTaskManager<?, ?, T>) tasks.get(getTaskId());
            if (manager == null)
                {
                closeExceptionally(new IllegalStateException("Task [" + getTaskId()
                        + "] is no longer available after reconnecting."));
                }
            else
                {
                processTaskUpdate(manager);
                }
            }
        catch (RuntimeException e)
            {
            Logger.fine(() -> String.format("MapListener for Task [%s] could not be recovered: %s",
                                            getTaskId(), e));
            }
        }

    /**
     * Schedule recovery after a remote-service topology change.
     *
     */
    protected void scheduleRecovery()
        {
        try
            {
            f_executorService.submit(this::recover);
            }
        catch (RejectedExecutionException e)
            {
            Logger.fine(() -> String.format("MapListener recovery for Task [%s] could not be scheduled.",
                                            getTaskId()));
            }
        }

    /**
     * Return the underlying {@link CacheService} for the executor service.
     *
     * @return the underlying {@link CacheService}
     */
    protected CacheService getCacheService()
        {
        return f_cacheService;
        }

    /**
     * Removes the {@link MapListener} previously installed by this
     * {@code ClusteredTaskCoordinator}.
     */
    protected void removeMapListener()
        {
        // remove the map listener
        final ClusteredTaskCoordinator coordinator = this;

        // create a Runnable to remove the MapListener
        Runnable removeMapListenerRunnable = () -> Caches.tasks(getCacheService()).removeMapListener(coordinator, getTaskId());

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
         * @param service  the associated executor {@link CacheService}
         */
        ClusteredMemberListener(CacheService service)
            {
            f_service = service;
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
            ServiceInfo info  = f_service.getInfo();
            String      sType = info == null ? null : info.getServiceType();
            if (CacheService.TYPE_REMOTE.equals(sType) || CacheService.TYPE_REMOTE_GRPC.equals(sType))
                {
                scheduleRecovery();
                }
            }

        @Override
        public void memberJoined(MemberEvent event)
            {
            scheduleRecovery();
            }

        // ----- data members -----------------------------------------------

        /**
         * The {@link CacheService}.
         */
        protected final CacheService f_service;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The sharable {@link Task.Properties} for this task.
     */
    protected Task.Properties m_properties;

    /**
     * The latest task result version delivered to subscribers.
     */
    protected int m_nResultVersion;

    /**
     * The member listener for cluster member.
     */
    protected final ClusteredMemberListener f_memberListener;

    /**
     * The {@link CacheService} used by the executor service.
     */
    protected final CacheService f_cacheService;

    /**
     * Advisory submitter subject for install-gate telemetry.
     */
    protected final Subject f_subject;
    }
