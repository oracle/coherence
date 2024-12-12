/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net.topic.impl.paged;

import com.oracle.coherence.common.base.Associated;
import com.oracle.coherence.common.base.Blocking;
import com.oracle.coherence.common.base.Exceptions;
import com.oracle.coherence.common.base.Logger;

import com.tangosol.internal.net.topic.PublishResult;
import com.tangosol.internal.net.topic.PublisherChannelConnector;

import com.tangosol.internal.net.topic.impl.paged.agent.OfferProcessor;
import com.tangosol.internal.net.topic.impl.paged.agent.TailAdvancer;
import com.tangosol.internal.net.topic.impl.paged.agent.TopicInitialiseProcessor;

import com.tangosol.internal.net.topic.impl.paged.model.Page;
import com.tangosol.internal.net.topic.impl.paged.model.Usage;

import com.tangosol.internal.util.DaemonPool;

import com.tangosol.net.PagedTopicService;

import com.tangosol.net.partition.KeyPartitioningStrategy;

import com.tangosol.net.topic.Publisher;
import com.tangosol.net.topic.TopicDependencies;
import com.tangosol.net.topic.TopicException;

import com.tangosol.util.Binary;
import com.tangosol.util.InvocableMapHelper;

import java.util.List;
import java.util.Objects;

import java.util.concurrent.CompletableFuture;

import java.util.function.BiConsumer;

/**
 * A {@link PublisherChannelConnector} to connect a channel specific publisher
 * to an underlying paged topic.
 *
 * @param <V>  the type of element published to the topic
 *
 * @author Jonathan Knight  2024.11.26
 */
@SuppressWarnings("TypeParameterExplicitlyExtendsObject")
public class PagedTopicChannelPublisherConnector<V>
        implements PublisherChannelConnector<V>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a {@link PagedTopicChannelPublisherConnector}.
     *
     * @param lPublisherId     the identifier for the parent publisher
     * @param nChannel         the channel to publish to
     * @param caches           the paged topic caches
     * @param nNotifyPostFull  the post full notification identifier
     */
    public PagedTopicChannelPublisherConnector(long                           lPublisherId,
                                               int                            nChannel,
                                               int                            nChannelCount,
                                               PagedTopicCaches               caches,
                                               int                            nNotifyPostFull)
        {
        f_lPublisherId            = lPublisherId;
        f_nChannel                = nChannel;
        f_nChannelCount           = nChannelCount;
        f_sTopicName              = caches.getTopicName();
        m_caches                  = caches;
        f_nNotifyPostFull         = nNotifyPostFull;
        f_keyUsageSync            = caches.getUsageSyncKey(nChannel);
        f_nUsageSyncUnitOfOrder   = caches.getUnitOfOrder(f_keyUsageSync.getPartitionId());
        f_keyPartitioningStrategy = caches.getService().getKeyPartitioningStrategy();

        m_state = State.Active;
        }

    @Override
    public int getChannel()
        {
        return f_nChannel;
        }

    @Override
    public String getTopicName()
        {
        return f_sTopicName;
        }

    @Override
    public void ensureConnected()
        {
        PagedTopicCaches  caches = m_caches;
        if (m_state != State.Active || caches == null)
            {
            // we're closed
            return;
            }

        TopicDependencies dependencies = caches.getDependencies();
        long              retry        = dependencies.getReconnectRetryMillis();
        long              now          = System.currentTimeMillis();
        long              timeout      = now + dependencies.getReconnectTimeoutMillis();
        Throwable         error        = null;

        while (now < timeout)
            {
            caches = m_caches;
            if (m_state != State.Active || caches == null)
                {
                // we're closed
                return;
                }

            try
                {
                retry   = dependencies.getReconnectRetryMillis();
                timeout = now + dependencies.getReconnectTimeoutMillis();

                caches.ensureConnected();

                // we must ensure the topic has the required number of channels
                PagedTopicService service = caches.getService();
                if (service.isSuspended())
                    {
                    Blocking.sleep(100);
                    break;
                    }

                int cActual = service.ensureChannelCount(f_sTopicName, f_nChannel + 1, f_nChannelCount);
                if (f_nChannel >= cActual)
                    {
                    Logger.warn(() -> String.format("This publisher is publishing to channel %d, but the topic is configured with %d channels", f_nChannel, cActual));
                    }

                error = null;
                break;
                }
            catch (Throwable thrown)
                {
                error = thrown;
                if (error instanceof TopicException)
                    {
                    break;
                    }
                }
            now = System.currentTimeMillis();
            if (now < timeout)
                {
                Logger.finer("Failed to reconnect publisher, will retry in "
                        + retry + " millis " + this + " due to " + error.getMessage());
                try
                    {
                    Thread.sleep(retry);
                    }
                catch (InterruptedException e)
                    {
                    // ignored
                    }
                }
            }

        if (error != null)
            {
            throw Exceptions.ensureRuntimeException(error);
            }
        }

    @Override
    public CompletableFuture<? extends Object> initialize()
        {
        return ensurePageId();
        }

    @Override
    public void offer(Object oCookie, List<Binary> listBinary, int nNotifyPostFull, BiConsumer<PublishResult, Throwable> handler)
        {
        PagedTopicCaches caches  = m_caches;
        Long             lPageId = (Long) oCookie;

        Page.Key keyPage = new Page.Key(f_keyUsageSync.getChannelId(), lPageId);
        int      nPart   = f_keyPartitioningStrategy.getKeyPartition(keyPage);

        OfferProcessor processor  = new OfferProcessor(listBinary, f_nNotifyPostFull, false);
        InvocableMapHelper.invokeAsync(caches.Pages, keyPage, caches.getUnitOfOrder(nPart), processor,
                (result, e) ->
                    {
                    PublishResult publishResult = null;
                    if (e == null)
                        {
                        publishResult = result.toPublishResult(f_nChannel, lPageId);
                        }
                    handler.accept(publishResult, e);
                    }
                );
        }

    @Override
    public CompletableFuture<? extends Object> prepareOfferRetry(Object oCookie)
        {
        Long lPageId = (Long) oCookie;
        return moveToNextPage(lPageId);
        }

    @Override
    public TopicDependencies getTopicDependencies()
        {
        return m_caches.getDependencies();
        }

    /**
     * Returns {@code true} if this publisher is active, otherwise returns {@code false}.
     *
     * @return {@code true} if this publisher is active, otherwise returns {@code false}
     */
    public boolean isActive()
        {
        return m_state == State.Active;
        }

    /**
     * Stop this publisher from accepting any further messages to publish
     */
    public synchronized void stop()
        {
        if (m_state == State.Active)
            {
            m_state = State.Closing;
            }
        }

    /**
     * Close this publisher.
     */
    public synchronized void close()
        {
        if (m_state == State.Closing)
            {
            m_state  = State.Closed;
            m_caches = null;
            }
        }

    /**
     * Obtain the page id for this channel creating and initialising the page id if required.
     *
     * @return the future page id
     */
    protected CompletableFuture<Long> ensurePageId()
        {
        if (m_futurePageId == null)
            {
            return initializePageId();
            }
        return m_futurePageId;
        }

    /**
     * Initialise the page id instance if it does not already exist.
     *
     * @return the future page id
     */
    private synchronized CompletableFuture<Long> initializePageId()
        {
        if (m_futurePageId == null)
            {
            ensureConnected();

            m_futurePageId = InvocableMapHelper.invokeAsync(m_caches.Usages, f_keyUsageSync,
                    m_caches.getUnitOfOrder(f_keyUsageSync.getPartitionId()),
                    new TopicInitialiseProcessor());
            }
        return m_futurePageId;
        }

    /**
     * Asynchronously obtain the next page ID to offer to after the
     * specified page ID.
     *
     * @param lPage  the current page ID
     *
     * @return  A {@link CompletableFuture} that will, on completion, contain the
     *          next page ID, or if the topic is full the current page ID
     */
    protected CompletableFuture<Long> moveToNextPage(long lPage)
        {
        CompletableFuture<Long> futureResult;
        long                    lPageCurrent = m_lTail;

        if (lPageCurrent > lPage)
            {
            // The ID has already been moved on from the specified value
            // (presumably by another thread) so we just return the current value
            return CompletableFuture.completedFuture(lPageCurrent);
            }

        synchronized (this)
            {
            // Get the current value in case it has changed
            lPageCurrent = m_lTail;

            // Verify that we have not already moved the page between
            // the last check and the synchronize
            if (lPageCurrent > lPage)
                {
                return CompletableFuture.completedFuture(lPageCurrent);
                }

            // Check that there is not already a move in progress
            futureResult = m_futureMovePage;
            if (futureResult == null)
                {
                m_futureMovePage = futureResult = InvocableMapHelper.invokeAsync(
                        m_caches.Usages, f_keyUsageSync, f_nUsageSyncUnitOfOrder,
                        new TailAdvancer(lPage + 1), (result, e) ->
                              {
                              if (e == null)
                                  {
                                  updatePageId(result);
                                  }
                              else
                                  {
                                  throw Exceptions.ensureRuntimeException(e);
                                  }
                              });
                }

            return futureResult;
            }
        }

    /**
     * Handle completion of an async entry processor that updates the page id.
     *
     * @param lPageNew the updated page id value returned from the processor
     */
    protected void updatePageId(long lPageNew)
        {
        if (m_lTail < lPageNew)
            {
            synchronized (this)
                {
                if (m_lTail < lPageNew)
                    {
                    m_lTail = lPageNew;
                    }
                }
            }
        m_futurePageId   = m_futureMovePage;
        m_futureMovePage = null;
        }

    // ----- Object methods -------------------------------------------------


    @Override
    public boolean equals(Object o)
        {
        if (this == o)
            {
            return true;
            }
        if (o == null || getClass() != o.getClass())
            {
            return false;
            }
        PagedTopicChannelPublisherConnector<?> that = (PagedTopicChannelPublisherConnector<?>) o;

        return f_lPublisherId == that.f_lPublisherId
                && f_nChannel == that.f_nChannel
                && Objects.equals(m_caches, that.m_caches);
        }

    @Override
    public int hashCode()
        {
        return Objects.hash(f_lPublisherId, f_nChannel, m_caches);
        }

    @Override
    public String toString()
        {
        return getClass().getSimpleName() +
                "(topic=" + f_sTopicName +
                ", channel=" + f_nChannel +
                ", state=" + m_state +
                ", publisher=" + f_lPublisherId +
                ")";
        }

    // ----- inner enum: State ----------------------------------------------

    /**
     * An enum representing the {@link Publisher} state.
     */
    public enum State
        {
        /**
         * The publisher is active.
         */
        Active,
        /**
         * The publisher is closing.
         */
        Closing,
        /**
         * The publisher is closed.
         */
        Closed,
        }

    // ----- inner class: AssociatedExecutor --------------------------------

    /**
     * A {@link BatchingOperationsQueue.Executor} that expects the {@link DaemonPool}
     * to honour the {@link Associated} tasks submitted to it.
     */
    protected class AssociatedExecutor
            implements BatchingOperationsQueue.Executor
        {
        public AssociatedExecutor(DaemonPool pool)
            {
            f_pool = pool;
            }

        @Override
        public void execute(Runnable runnable)
            {
            f_pool.add(new AssociatedTask(runnable));
            }

        // ----- data members -----------------------------------------------

        /**
         * The {@link DaemonPool} used to complete published message futures
         * so that they are not on the service thread.
         */
        private final DaemonPool f_pool;
        }

    // ----- inner class: AssociatedTask ------------------------------------

    protected class AssociatedTask
            implements Runnable, Associated<Integer>
        {
        public AssociatedTask(Runnable task)
            {
            f_task = task;
            }

        @Override
        public Integer getAssociatedKey()
            {
            return f_nChannel;
            }

        @Override
        public void run()
            {
            f_task.run();
            }

        // ----- data members -----------------------------------------------

        private final Runnable f_task;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The identifier of the parent publisher.
     */
    private final long f_lPublisherId;

    /**
     * The channel to publish to.
     */
    private final int f_nChannel;

    /**
     * The total number of channels.
     */
    private final int f_nChannelCount;

    /**
     * The name of the topic being published to
     */
    private final String f_sTopicName;

    /**
     * The topic's underlying caches.
     */
    private PagedTopicCaches m_caches;

    /**
     * The partitioning strategy used by the topic's caches.
     */
    private final KeyPartitioningStrategy f_keyPartitioningStrategy;

    /**
     * The post full notifier.
     */
    private final int f_nNotifyPostFull;

    /**
     * The key for the Usage object which maintains the channel's tail.
     */
    private final Usage.Key f_keyUsageSync;

    /**
     * The unit of order for the Usage object which maintains the channel's tail.
     */
    private final int f_nUsageSyncUnitOfOrder;

    /**
     * The current state of the publisher.
     */
    private volatile State m_state;

    /**
     * The tail for this channel
     */
    private volatile long m_lTail = -1;

    /**
     * The {@link CompletableFuture} that will be complete when the
     * page ID is updated.
     */
    private volatile CompletableFuture<Long> m_futurePageId;

    /**
     * The {@link CompletableFuture} that will complete when the current page increment
     * operation completes. This value will be null if no page increment operation is in
     * progress.
     */
    private CompletableFuture<Long> m_futureMovePage;
    }
