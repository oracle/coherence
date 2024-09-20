/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic.impl.paged;

import com.oracle.coherence.common.base.Associated;

import com.oracle.coherence.common.base.Blocking;
import com.oracle.coherence.common.base.Exceptions;
import com.oracle.coherence.common.base.Logger;
import com.oracle.coherence.common.util.Duration;
import com.tangosol.internal.net.DebouncedFlowControl;

import com.tangosol.internal.net.topic.impl.paged.agent.OfferProcessor;
import com.tangosol.internal.net.topic.impl.paged.agent.TailAdvancer;
import com.tangosol.internal.net.topic.impl.paged.agent.TopicInitialiseProcessor;

import com.tangosol.internal.net.topic.impl.paged.model.Page;
import com.tangosol.internal.net.topic.impl.paged.model.Usage;

import com.tangosol.internal.util.DaemonPool;

import com.tangosol.io.Serializer;

import com.tangosol.net.PagedTopicService;
import com.tangosol.net.partition.KeyPartitioningStrategy;

import com.tangosol.net.topic.NamedTopic;
import com.tangosol.net.topic.Publisher;
import com.tangosol.net.topic.TopicException;
import com.tangosol.net.topic.TopicPublisherException;

import com.tangosol.util.Binary;
import com.tangosol.util.InvocableMapHelper;
import com.tangosol.util.LongArray;
import com.tangosol.util.SparseArray;

import java.util.List;
import java.util.Objects;

import java.util.concurrent.CompletableFuture;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;

/**
 * A publisher that publishes to a single channel.
 * <p>
 * Any errors will cause this publisher to close and complete all outstanding
 * requests with exceptions.
 *
 * @author Jonathan Knight 2021.06.03
 * @since 21.06
 */
public class PagedTopicChannelPublisher
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a {@link PagedTopicChannelPublisher}.
     *
     * @param lPublisherId     the identifier for the parent {@link PagedTopicPublisher}
     * @param nChannel         the channel to publish to
     * @param caches           the paged topic caches
     * @param nNotifyPostFull  the post full notification identifier
     * @param flowControl      the {@link DebouncedFlowControl} control to use
     * @param pool             the {@link DaemonPool} to execute publish completions
     */
    public PagedTopicChannelPublisher(long                           lPublisherId,
                                      int                            nChannel,
                                      int                            nChannelCount,
                                      PagedTopicCaches               caches,
                                      int                            nNotifyPostFull,
                                      DebouncedFlowControl           flowControl,
                                      DaemonPool                     pool,
                                      BiConsumer<Throwable, Integer> onErrorHandler)
        {
        f_lPublisherId            = lPublisherId;
        f_nChannel                = nChannel;
        f_nChannelCount           = nChannelCount;
        f_sTopicName              = caches.getTopicName();
        f_onErrorHandler          = onErrorHandler;
        m_caches                  = caches;
        f_nNotifyPostFull         = nNotifyPostFull;
        f_keyUsageSync            = caches.getUsageSyncKey(nChannel);
        f_nUsageSyncUnitOfOrder   = caches.getUnitOfOrder(f_keyUsageSync.getPartitionId());
        f_serializer              = caches.getSerializer();
        f_keyPartitioningStrategy = caches.getService().getKeyPartitioningStrategy();

        BatchingOperationsQueue.Executor executor   = new AssociatedExecutor(pool);
        NamedTopic.ElementCalculator     calculator = caches.getElementCalculator();

        f_batchingQueue = new BatchingOperationsQueue<>(this::addQueuedElements, 1, flowControl,
                calculator::calculateUnits, executor);

        m_state = State.Active;
        }

    /**
     * Publish the specified messages.
     *
     * @param binValue  the messages to publish as a serialized {@link Binary} value
     *
     * @return  a {@link CompletableFuture} that will complete when the messages has been published
     *          with the status of the publish request
     */
    public CompletableFuture<Publisher.Status> publish(Binary binValue)
        {
        ensureConnected();
        try
            {
            return f_batchingQueue.add(binValue);
            }
        catch (IllegalStateException e)
            {
            // The batching queue throws an IllegalStateException if closed,
            // so we throw another with a more meaningful message
            throw new IllegalStateException("This publisher is no longer active", e);
            }
        }

    private void ensureConnected()
        {
        long      now     = System.currentTimeMillis();
        long      retry   = PagedTopic.DEFAULT_RECONNECT_TIMEOUT_SECONDS.as(Duration.Magnitude.MILLI);
        long      timeout = now *2;
        Throwable error   = null;

        while (now < timeout)
            {
            PagedTopicCaches caches = m_caches;
            if (m_state != State.Active || caches == null)
                {
                // we're closed
                return;
                }

            try
                {
                PagedTopicDependencies dependencies = caches.getDependencies();
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
            f_batchingQueue.close();
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
            // belt and braces cancellation of remaining publish requests
            f_batchingQueue.cancelAllAndClose("Publisher has been closed", null);
            m_caches = null;
            }
        }

    /**
     * Obtain a {@link CompletableFuture} that will be complete when
     * all of the currently outstanding publish operations complete.
     *
     * @param mode  {@link PagedTopicPublisher.FlushMode} flush mode to use
     *
     * @return a {@link CompletableFuture} that will be completed when
     *         all of the currently outstanding publish operations are complete
     */
    public CompletableFuture<Void> flush(PagedTopicPublisher.FlushMode mode)
        {
        String sDescription = null;

        switch (mode)
            {
            case FLUSH_DESTROY:
                sDescription = "Topic " + f_sTopicName + " was destroyed";

            case FLUSH_CLOSE_EXCEPTIONALLY:
                String sReason = sDescription != null
                        ? sDescription
                        : "Force Close of Publisher " + f_lPublisherId + " channel "
                                + f_nChannel + " for topic " + f_sTopicName;

                BiFunction<Throwable, Binary, Throwable> fn  = TopicPublisherException.createFactory(f_serializer, sReason);
                f_batchingQueue.handleError(fn, BatchingOperationsQueue.OnErrorAction.CompleteWithException);
                return f_batchingQueue.flush();

            case FLUSH:
            default:
                return f_batchingQueue.flush();
            }
        }

    /**
     * Returns the channel this publisher publishes to.
     *
     * @return  the channel this publisher publishes to
     */
    public int getChannel()
        {
        return f_nChannel;
        }

    /**
     * Method called to notify this publisher that space is now available in a previously full topic.
     */
    protected void onNotification()
        {
        ++m_cNotify;
        if (f_batchingQueue.resume())
            {
            ++m_cWait;
            addQueuedElements(1);
            }
        }

    /**
     * Offer the batched elements from this channel.
     *
     * @param cbMaxElements  the maximum number of bytes to offer
     */
    protected void addQueuedElements(int cbMaxElements)
        {
        // Fill the current batch with the specified number of elements
        if (f_batchingQueue.fillCurrentBatch(cbMaxElements))
            {
            // There are elements in the queue so process them by
            // first ensuring the page id is set
            ensurePageId()
                    .thenAccept((_void) -> addInternal(m_lTail))
                    .handle(this::handleError);
            }
        }

    /**
     * Asynchronously add elements to the specified page.
     *
     * @param lPageId  the id of the page to offer the elements to
     */
    protected void addInternal(long lPageId)
        {
        List<Binary> listBinary = f_batchingQueue.getCurrentBatchValues();

        // If the list is empty (which would probably be due to the
        // application code calling cancel on the futures) the we
        // do not need to do anything else
        if (listBinary.isEmpty())
            {
            return;
            }

        PagedTopicCaches caches  = m_caches;

        Page.Key keyPage = new Page.Key(f_keyUsageSync.getChannelId(), lPageId);
        int      nPart   = f_keyPartitioningStrategy.getKeyPartition(keyPage);

        InvocableMapHelper.invokeAsync(caches.Pages, keyPage, caches.getUnitOfOrder(nPart),
                new OfferProcessor(listBinary, f_nNotifyPostFull, false),
                (result, e) ->
                    {
                    if (e == null)
                        {
                        handleOfferCompletion(result, lPageId);
                        }
                    else
                        {
                        handleError(null, e);
                        }
                    }
                );
        }

    /**
     * Handle completion of a {@link CompletableFuture} linked to an async execution of
     * a {@link OfferProcessor}.
     *
     * @param result   the result returned from the {@link OfferProcessor}
     * @param lPageId  the id of the page offered to
     */
    protected void handleOfferCompletion(OfferProcessor.Result result, long lPageId)
        {
        // Complete the offered elements
        LongArray<Throwable>        aErrors   = result.getErrors();
        LongArray<Publisher.Status> aMetadata = new SparseArray<>();
        int                         cAccepted = result.getAcceptedCount();
        int                         nChannel  = f_keyUsageSync.getChannelId();

        ++m_cOffers;
        m_cAccepted += cAccepted;

        if (cAccepted == 0)
            {
            ++m_cMisses;
            }

        if (f_nNotifyPostFull == 0 && result.getStatus() == OfferProcessor.Result.Status.TopicFull)
            {
            int       ceBatch = f_batchingQueue.getCurrentBatch().size();
            Throwable error   = new IllegalStateException("the topic is at capacity"); // java.util.Queue.add throws ISE so we do to

            if (aErrors == null)
                {
                aErrors = new SparseArray<>();
                }

            while (cAccepted < ceBatch)
                {
                ++cAccepted;
                aErrors.add(error);
                }
            }
        else
            {
            int nOffset = result.getOffset();
            for (long i = 0; i < cAccepted; i++)
                {
                if (aErrors == null || aErrors.get(i) == null)
                    {
                    aMetadata.set(i, new PagedTopicPublisher.PublishedStatus(nChannel, lPageId, nOffset++));
                    }
                }
            }

        f_batchingQueue.completeElements(cAccepted, aErrors, aMetadata, TopicPublisherException.createFactory(f_serializer), null);

        // If there are any errors
        handleIndividualErrors(aErrors);

        // we need to handle offer completions until actually closed to
        // allow for flushing during close
        if (m_state != State.Closed)
            {
            switch (result.getStatus())
                {
                case PageSealed:
                    moveToNextPage(lPageId)
                            .thenRun(() -> addQueuedElements(result.getPageCapacity()))
                            .handle(this::handleError);
                    break;

                case TopicFull:
                    if (f_nNotifyPostFull != 0)
                        {
                        f_batchingQueue.pause();
                        break;
                        }
                    // else; fall through

                default:
                    addQueuedElements(result.getPageCapacity());
                    break;
                }
            }
        // else; if the error handler closed the publisher there is nothing else to do
        }

    /**
     * Handle the specified error.
     *
     * @param ignored    the ignored value (this parameter allows this method to be used as an async handler
     *                   but is never used by the method)
     * @param throwable  the error to handle
     */
    protected Void handleError(Object ignored, Throwable throwable)
        {
        if (throwable != null)
            {
            // call the BatchingOperationsQueue's error handler to cancel all outstanding elements.
            synchronized (this)
                {
                stop();
                // Inform the error handler of the error, this should be the parent publisher
                // so that it can close itself if required.
                // We do this here so that the publisher closes before this channel publisher to catch
                // any race where a new message is in the middle of being added
                if (f_onErrorHandler != null)
                    {
                    try
                        {
                        f_onErrorHandler.accept(throwable, f_nChannel);
                        }
                    catch (Throwable t)
                        {
                        // shouldn't happen but if we do get an exception we can ignore it
                        // and carry on with our clean-up
                        Logger.err(t);
                        }
                    }
                f_batchingQueue.handleError(TopicPublisherException.createFactory(f_serializer, throwable.getMessage()),
                                            BatchingOperationsQueue.OnErrorAction.CancelAndClose);
                close();
                }
            }
        return VOID;
        }

    /**
     * Process the array of exceptions associated with an offer.
     *
     * @param aErrors the error array
     */
    protected void handleIndividualErrors(LongArray<Throwable> aErrors)
        {
        if (aErrors == null || aErrors.isEmpty())
            {
            return;
            }

        // Stop this publisher.
        Throwable throwable = aErrors.get(aErrors.getFirstIndex());
        handleError(VOID, throwable);
        }

    /**
     * Obtain the page id for this channel creating and initialising a the page id if required.
     *
     * @return the future page id
     */
    protected CompletableFuture<Long> ensurePageId()
        {
        if (futurePageId == null)
            {
            return initializePageId();
            }
        return futurePageId;
        }

    /**
     * Initialise the page id instance if it does not already exist.
     *
     * @return the future page id
     */
    private synchronized CompletableFuture<Long> initializePageId()
        {
        if (futurePageId == null)
            {
            futurePageId = InvocableMapHelper.invokeAsync(m_caches.Usages, f_keyUsageSync,
                    m_caches.getUnitOfOrder(f_keyUsageSync.getPartitionId()),
                    new TopicInitialiseProcessor());
            }
        return futurePageId;
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
                                  handleError(result, e);
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
        PagedTopicChannelPublisher that = (PagedTopicChannelPublisher) o;

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
        long cOffersNow   = m_cOffers;
        long cAcceptedNow = m_cAccepted;
        long cMissesNow   = m_cMisses;
        long cWaitNow     = m_cWait;
        long cNotifyNow   = m_cNotify;

        long cOffers   = cOffersNow   - m_cOffersLast;
        long cAccepted = cAcceptedNow - m_cAcceptedLast;
        long cMisses   = cMissesNow   - m_cMissesLast;
        long cWait     = cWaitNow     - m_cWaitsLast;
        long cNotify   = cNotifyNow   - m_cNotifyLast;

        m_cOffersLast   = cOffersNow;
        m_cAcceptedLast = cAcceptedNow;
        m_cMissesLast   = cMissesNow;
        m_cWaitsLast    = cWaitNow;
        m_cNotifyLast   = cNotifyNow;

        return getClass().getSimpleName() +
                "(topic=" + f_sTopicName +
                ", channel=" + f_nChannel +
                ", state=" + m_state +
                ", publisher=" + f_lPublisherId +
                ", batchSize=" + (cAccepted / Math.max(1, cOffers - cMisses)) +
                ", hitRate=" + ((cOffers - cMisses) * 100 / Math.max(1, cOffers)) + "%" +
                ", waitNotifyRate=" + (cWait * 100 / Math.max(1, cOffers)) + "/" + (cNotify * 100 / Math.max(1, cOffers)) + "%" +
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

    // ----- constants ------------------------------------------------------

    /**
     * A singleton Void value;
     */
    private static final Void VOID = null;

    // ----- data members ---------------------------------------------------

    /**
     * The identifier of the parent {@link PagedTopicPublisher}.
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
     * A consumer to be notified on publishing errors.
     */
    private final BiConsumer<Throwable, Integer> f_onErrorHandler;

    /**
     * The topic's underlying caches.
     */
    private PagedTopicCaches m_caches;

    /**
     * The serializer to use to serialize message payloads.
     */
    private final Serializer f_serializer;

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
     * The {@link BatchingOperationsQueue} controlling the batches of add operations.
     */
    private final BatchingOperationsQueue<Binary, Publisher.Status> f_batchingQueue;

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
    private volatile CompletableFuture<Long> futurePageId;

    /**
     * The {@link CompletableFuture} that will complete when the current page increment
     * operation completes. This value will be null if no page increment operation is in
     * progress.
     */
    private CompletableFuture<Long> m_futureMovePage;

    /**
     * The number of times an offer was made
     */
    private long m_cOffers;

    /**
     * The last value of m_cOffers used within {@link #toString} stats.
     */
    private long m_cOffersLast;

    /**
     * The number of accepted items.
     */
    private long m_cAccepted;

    /**
     * The last value of m_cAccepted used within {@link #toString} stats.
     */
    private long m_cAcceptedLast;

    /**
     * The number of times no elements were accepted from an offer
     */
    private long m_cMisses;

    /**
     * The last value of m_cMisses used within {@link #toString} stats.
     */
    private long m_cMissesLast;

    /**
     * The number of this this publisher has waited.
     */
    private long m_cWait;

    /**
     * The last value of m_cWait used within {@link #toString} stats.
     */
    private long m_cWaitsLast;

    /**
     * The number of times this publisher was notified.
     */
    private long m_cNotify;

    /**
     * The last value of m_cNotify used within {@link #toString} stats.
     */
    private long m_cNotifyLast;
    }
