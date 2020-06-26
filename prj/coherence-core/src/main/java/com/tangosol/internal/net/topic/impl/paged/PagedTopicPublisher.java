/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic.impl.paged;

import com.oracle.coherence.common.base.Converter;
import com.oracle.coherence.common.base.Logger;

import com.oracle.coherence.common.util.MemorySize;
import com.oracle.coherence.common.util.Options;

import com.tangosol.coherence.config.Config;

import com.tangosol.internal.net.DebouncedFlowControl;
import com.tangosol.internal.net.NamedCacheDeactivationListener;
import com.tangosol.internal.net.topic.impl.paged.agent.OfferProcessor;
import com.tangosol.internal.net.topic.impl.paged.agent.TailAdvancer;
import com.tangosol.internal.net.topic.impl.paged.agent.TopicInitialiseProcessor;
import com.tangosol.internal.net.topic.impl.paged.model.NotificationKey;
import com.tangosol.internal.net.topic.impl.paged.model.Page;
import com.tangosol.internal.net.topic.impl.paged.model.Usage;
import com.tangosol.internal.net.topic.impl.paged.model.Usage.Key;

import com.tangosol.net.FlowControl;
import com.tangosol.net.NamedCache;
import com.tangosol.net.PartitionedService;
import com.tangosol.net.RequestIncompleteException;
import com.tangosol.net.topic.NamedTopic;
import com.tangosol.net.topic.Publisher;

import com.tangosol.io.Serializer;

import com.tangosol.util.AbstractMapListener;
import com.tangosol.util.Base;
import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.HashHelper;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.InvocableMapHelper;
import com.tangosol.util.LongArray;
import com.tangosol.util.MapEvent;
import com.tangosol.util.MapListenerSupport;
import com.tangosol.util.SparseArray;
import com.tangosol.util.filter.InKeySetFilter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.tangosol.internal.net.topic.impl.paged.PagedTopicPublisher.FlushMode.*;

/**
 * A {@link PagedTopicPublisher} is a Topic implementation which publishes topic values
 * into pages stored within ordered channels on top of a partitioned cache.
 * <p>
 * This implementation uses various underlying {@link NamedCache} instances to
 * hold the data for the topic.
 * <p>
 * All interactions with the topic are via {@link InvocableMap.EntryProcessor}s
 * that run against keys in the page cache. This ensures that a page will be
 * locked for operations that add or remove from the topic and that the operations
 * happen against the correct head and tail pages.
 *
 * @author jk/mf 2015.05.15
 * @since Coherence 14.1.1
 */
public class PagedTopicPublisher<V>
        implements Publisher<V>,
            MapListenerSupport.SynchronousListener<NotificationKey, int[]>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a {@link PagedTopicPublisher}.
     *
     * @param pagedTopicCaches  the {@link PagedTopicCaches} managing this topic's caches
     * @param options           the {@link Option}s controlling this {@link PagedTopicPublisher}
     */
    public PagedTopicPublisher(PagedTopicCaches pagedTopicCaches, Option<? super V>... options)
        {
        this(pagedTopicCaches, null, options);
        }

    /**
     * Create a {@link PagedTopicPublisher}.
     *
     * @param pagedTopicCaches  the {@link PagedTopicCaches} managing this topic's caches
     * @param elementQueue      the batching queue (non-null for testing purposes)
     * @param options           the {@link Option}s controlling this {@link PagedTopicPublisher}
     */
    @SuppressWarnings("unchecked")
    protected PagedTopicPublisher(PagedTopicCaches pagedTopicCaches,
            BatchingOperationsQueue<Void> elementQueue,
            Option<? super V>... options)
        {
        m_caches = Objects.requireNonNull(pagedTopicCaches,
                "The PagedTopicCaches parameter cannot be null");

        registerDeactivationListener();

        Serializer serializer = pagedTopicCaches.getSerializer();

        f_convValueToBinary   = (value) -> ExternalizableHelper.toBinary(value, serializer);
        f_sTopicName          = pagedTopicCaches.getTopicName();
        f_options             = Options.from(Option.class, options);
        f_nNotifyPostFull     = f_options.contains(FailOnFull.class) ? 0 : pagedTopicCaches.newNotifierId();
        f_funcOrder           = computeOrderByOption(f_options);

        int  cParts   = ((PartitionedService) m_caches.getCacheService()).getPartitionCount();
        long cbBatch  = m_caches.getConfiguration().getMaxBatchSizeBytes();
        int  cChannel = pagedTopicCaches.getChannelCount();

        f_aChannel          = new Channel[cChannel];
        f_setOfferedChannel = new BitSet(cChannel);

        DebouncedFlowControl backlog = new DebouncedFlowControl(
                /*normal*/ cbBatch * 2, /*excessive*/ cbBatch * 3,
                (l) -> new MemorySize(Math.abs(l)).toString()); // attempt to always have at least one batch worth
        f_flowcontrol = backlog;

        for (int nChannel = 0; nChannel < cChannel; ++nChannel)
            {
            Channel channel = f_aChannel[nChannel] = new Channel();
            channel.batchingQueue = elementQueue == null
                    ? new BatchingOperationsQueue<>((c) -> addQueuedElements(channel, c), 1, backlog)
                    : elementQueue;
            // we don't just use (0,chan) as that would concentrate extra load on a single partitions when there are many channels
            int nPart = Math.abs((HashHelper.hash(f_sTopicName.hashCode(), nChannel) % cParts));
            channel.keyUsageSync = new Key(nPart, nChannel);
            }

        if (f_nNotifyPostFull != 0)
            {
            // register a publisher listener in each partition, we do this even if the config isn't declared
            // with high-units as the server may have an alternate config
            pagedTopicCaches.Notifications.addMapListener(this, new InKeySetFilter<>(/*filter*/ null,
                    pagedTopicCaches.getPartitionNotifierSet(f_nNotifyPostFull)), /*fLite*/ false);
            }
        }

    // ----- TopicPublisher methods ------------------------------------

    /**
     * Specifies whether or not the publisher is active.
     *
     * @return true if the publisher is active; false otherwise
     */
    public boolean isActive()
        {
        return f_aChannel[0].batchingQueue.isActive();
        }

    // ----- NamedTopic.Publisher methods ------------------------------

    @Override
    public CompletableFuture<Void> send(V value)
        {
        ensureActive();

        Channel channel = f_aChannel[Base.mod(f_funcOrder.getOrderId(value), f_aChannel.length)];

        return channel.batchingQueue.add(f_convValueToBinary.convert(value));
        }

    @Override
    public FlowControl getFlowControl()
        {
        return f_flowcontrol;
        }

    @Override
    public CompletableFuture<Void> flush()
        {
        return flushInternal(FLUSH);
        }

    @Override
    public synchronized void close()
        {
        if (isActive())
            {
            closeInternal(false);
            }
        }

    @Override
    public void onClose(Runnable action)
        {
        f_listOnCloseActions.add(action);
        }

    // ----- accessor methods -----------------------------------------------

    /**
     * Obtain the name of the {@link NamedTopic} that this publisher offers elements to.
     *
     * @return the name of the {@link NamedTopic} that this publisher offers elements to
     */
    public String getName()
        {
        return f_sTopicName;
        }

    /**
     * Obtain the current {@link OrderBy} option for this {@link PagedTopicPublisher}.
     *
     * @return the OrderByOption.
     */
    OrderBy getOrderByOption()
        {
        return f_funcOrder;
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

        PagedTopicPublisher<?> that = (PagedTopicPublisher<?>) o;

        return m_caches.equals(that.m_caches);

        }

    @Override
    public int hashCode()
        {
        return m_caches.hashCode();
        }

    @Override
    public String toString()
        {
        PagedTopicCaches caches = m_caches;
        if (caches == null)
            {
            return getClass().getSimpleName() + "(inactive)";
            }

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

        int cChannels = f_setOfferedChannel.cardinality();
        String sChannels = f_setOfferedChannel.toString();
        f_setOfferedChannel.clear();

        return getClass().getSimpleName() + "(topic=" + caches.getTopicName() +
                ", orderBy=" + f_funcOrder +
                ", backlog=" + f_flowcontrol +
                ", channels=" + sChannels + cChannels +
                ", batchSize=" + (cAccepted / Math.max(1, cOffers - cMisses)) +
                ", hitRate=" + ((cOffers - cMisses) * 100 / Math.max(1, cOffers)) + "%" +
                ", waitNotifyRate=" + (cWait * 100 / Math.max(1, cOffers)) + "/" + (cNotify * 100 / Math.max(1, cOffers)) + "%" +
                ")";
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Compute the OrderBy option for sent messages from this publisher.
     * Defaults to {@link OrderByThread} when no {@link OrderBy} option specified.
     *
     * @param options  All Options for this Publisher.
     *
     * @return {@link OrderBy} option for this publisher
     */
    private OrderBy computeOrderByOption(Options options)
        {
        Iterator<OrderBy> iter = options.getInstancesOf(OrderBy.class).iterator();

        return iter.hasNext() ? iter.next() : OrderBy.thread();
        }

    /**
     * Ensure that this publisher is active.
     *
     * @throws IllegalStateException if not active
     */
    private void ensureActive()
        {
        if (!isActive())
            {
            throw new IllegalStateException("This publisher is no longer active");
            }
        }

    /**
     * Close this {@link PagedTopicPublisher}.
     * <p>
     * The {@link Option}s passed to this method will override any options
     * that were set when this publisher was created.
     *
     * @param fDestroyed  {@code true} if this method is being called in response
     *                    to the topic being destroyed, in which case there is no
     *                    need to clean up cached data
     */

    protected synchronized void closeInternal(boolean fDestroyed)
        {
        if (m_caches == null)
            {
            // already closed
            return;
            }

        try
            {
            if (!fDestroyed && f_nNotifyPostFull != 0)
                {
                unregisterDeactivationListener();

                // unregister the publisher listener in each partition
                PagedTopicCaches caches = m_caches;

                caches.Notifications.removeMapListener(this, new InKeySetFilter<>(/*filter*/ null,
                        caches.getPartitionNotifierSet(f_nNotifyPostFull)));
                }

            // close the queues
            for (Channel channel : f_aChannel)
                {
                channel.batchingQueue.close();
                }

            // flush this publisher to wait for all of the outstanding
            // add operations to complete (or to be cancelled if we're destroying)
            try
                {
                flushInternal(fDestroyed ? FLUSH_DESTROY : FLUSH).get(CLOSE_TIMEOUT_SECS, TimeUnit.SECONDS);
                }
            catch (TimeoutException e)
                {
                // too long to wait for completion; force all outstanding futures to complete exceptionally
                flushInternal(FLUSH_CLOSE_EXCEPTIONALLY).join();
                Logger.warn("Publisher.close: timeout after waiting " + CLOSE_TIMEOUT_SECS + " seconds for completion with flush.join(), forcing complete exceptionally");
                }
            catch (ExecutionException | InterruptedException e)
                {
                // ignore
                }
            }
        finally
            {
            // clean up
            m_caches = null;

            f_listOnCloseActions.forEach(action ->
                {
                try
                    {
                    action.run();
                    }
                catch (Throwable t)
                    {
                    Logger.fine(this.getClass().getName() + ".close(): handled onClose exception: " +
                        t.getClass().getCanonicalName() + ": " + t.getMessage());
                    }
                });
            }
        }

    /**
     * Obtain a {@link CompletableFuture} that will be complete when
     * all of the currently outstanding add operations complete.
     * <p>
     * If this method is called in response to a topic destroy then the
     * outstanding operations will be completed with an exception as the underlying
     * topic caches have been destroyed so they can never complete normally.
     * <p>
     * if this method is called in response to a timeout waiting for flush to complete normally,
     * indicated by {@link FlushMode#FLUSH_CLOSE_EXCEPTIONALLY}, complete exceptionally all outstanding
     * asynchronous operations so close finishes.
     *
     * The returned {@link CompletableFuture} will always complete
     * normally, even if the outstanding operations complete exceptionally.
     *
     * @param mode  {@link FlushMode} flush mode to use
     *
     * @return a {@link CompletableFuture} that will be completed when
     *         all of the currently outstanding add operations are complete
     */
    private CompletableFuture<Void> flushInternal(FlushMode mode)
        {
        String sDescription = null;
        switch (mode)
            {
            case FLUSH_DESTROY:
                sDescription = "Topic " + f_sTopicName + " was destroyed";

            case FLUSH_CLOSE_EXCEPTIONALLY:
                if (sDescription == null)
                    {
                    sDescription = "Force Close of Publisher " + hashCode() + " for topic " + f_sTopicName;
                    }

                Throwable error = new RequestIncompleteException(sDescription);
                Arrays.stream(f_aChannel)
                    .forEach(channel -> channel.batchingQueue.handleError(error,
                        BatchingOperationsQueue.OnErrorAction.CompleteWithException));

                return CompletableFuture.completedFuture(null);

            case FLUSH:
            default:
                CompletableFuture[] aFuture = new CompletableFuture[f_aChannel.length];

                for (int i = 0; i < aFuture.length; ++i)
                    {
                    aFuture[i] = f_aChannel[i].batchingQueue.flush();
                    }
                return CompletableFuture.allOf(aFuture);
            }
        }

    /**
     * Offer the batched elements from the channel to the topic.
     *
     * @param channel        the channel
     * @param cbMaxElements  the maximum number of bytes to offer
     */
    protected void addQueuedElements(Channel channel, int cbMaxElements)
        {
        // Fill the current batch with the specified number of elements
        if (channel.batchingQueue.fillCurrentBatch(cbMaxElements))
            {
            // There are elements in the queue so process them by
            // first ensuring the page id is set
            ensurePageId(channel)
                    .thenAccept((_void) -> addInternal(channel, channel.lTail))
                    .handle(this::handleError);
            }
        }

    /**
     * Asynchronously add elements to the specified page.
     *
     * @param channel  the channel
     * @param lPageId  the id of the page to offer the elements to
     */
    protected void addInternal(Channel channel, long lPageId)
        {
        List<Binary> listBinary = channel.batchingQueue.getCurrentBatchValues();

        // If the list is empty (which would probably be due to the
        // application code calling cancel on the futures) the we
        // do not need to do anything else
        if (listBinary.isEmpty())
            {
            return;
            }

        Page.Key keyPage = new Page.Key(channel.keyUsageSync.getChannelId(), lPageId);
        int      nPart   = ((PartitionedService) m_caches.getCacheService())
                .getKeyPartitioningStrategy().getKeyPartition(keyPage);

        PagedTopicCaches caches = m_caches;
        InvocableMapHelper.invokeAsync(caches.Pages, keyPage, caches.getUnitOfOrder(nPart),
                new OfferProcessor(listBinary, f_nNotifyPostFull, false),
                (result, e) ->
                    {
                    if (e == null)
                        {
                        handleOfferCompletion(result, channel, lPageId);
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
     * @param channel  the channel
     * @param lPageId  the id of the page offered to
     */
    protected void handleOfferCompletion(OfferProcessor.Result result, Channel channel, long lPageId)
        {
        // Complete the offered elements
        LongArray<Throwable> aErrors   = result.getErrors();
        int                  cAccepted = result.getAcceptedCount();
        int                  nChannel  = channel.keyUsageSync.getChannelId();

        ++m_cOffers;
        m_cAccepted += cAccepted;

        if (cAccepted == 0)
            {
            ++m_cMisses;
            }

        f_setOfferedChannel.set(nChannel);

        if (f_nNotifyPostFull == 0 && result.getStatus() == OfferProcessor.Result.Status.TopicFull)
            {
            int       ceBatch = channel.batchingQueue.getCurrentBatch().size();
            Throwable e       = new IllegalStateException("the topic is at capacity"); // java.util.Queue.add throws ISE so we do to

            if (aErrors == null)
                {
                aErrors = new SparseArray<>();
                }

            while (cAccepted < ceBatch)
                {
                ++cAccepted;
                aErrors.add(e);
                }
            }

        channel.batchingQueue.completeElements(cAccepted, aErrors);

        // If there are any errors
        handleIndividualErrors(aErrors);

        if (isActive())
            {
            switch (result.getStatus())
                {
                case PageSealed:
                    moveToNextPage(channel, lPageId)
                            .thenRun(() -> addQueuedElements(channel, result.getPageCapacity()))
                            .handle(PagedTopicPublisher.this::handleError);
                    break;

                case TopicFull:
                    if (f_nNotifyPostFull != 0)
                        {
                        channel.batchingQueue.pause();
                        break;
                        }
                    // else; fall through

                default:
                    addQueuedElements(channel, result.getPageCapacity());
                    break;
                }
            }
        // else; if the error handler closed the publisher there is nothing else to do
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

        OnFailure onFailure = f_options.get(OnFailure.class);

        switch (onFailure)
            {
            case Stop:
                // Stop the publisher.
                // The handleError method will do the appropriate thing.
                Throwable throwable = aErrors.get(aErrors.getFirstIndex());
                handleError(VOID, throwable);
                break;
            case Continue:
                // Do nothing as the individual errors will
                // already have been handled
                break;
            }
        }

    /**
     * Obtain the page id for a given channel creating and initialising a the page id if required.
     *
     * @param channel  the channel
     *
     * @return the future page id
     */
    protected CompletableFuture<Long> ensurePageId(Channel channel)
        {
        if (channel.futurePageId == null)
            {
            return initializePageId(channel);
            }

        return channel.futurePageId;
        }

    /**
     * Initialise the page id instance if it does not already exist.
     *
     * @param channel  the channel
     *
     * @return the future page id
     */
    private CompletableFuture<Long> initializePageId(Channel channel)
        {
        synchronized (channel)
            {
            if (channel.futurePageId == null)
                {
                channel.futurePageId = InvocableMapHelper.invokeAsync(m_caches.Usages, channel.keyUsageSync,
                        m_caches.getUnitOfOrder(channel.keyUsageSync.getPartitionId()),
                        new TopicInitialiseProcessor());
                }

            return channel.futurePageId;
            }
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
    protected CompletableFuture<Long> moveToNextPage(Channel channel, long lPage)
        {
        CompletableFuture<Long> futureResult;
        long                    lPageCurrent = channel.lTail;

        if (lPageCurrent > lPage)
            {
            // The ID has already been moved on from the specified value
            // (presumably by another thread) so we just return the current value
            return CompletableFuture.completedFuture(lPageCurrent);
            }

        synchronized (channel)
            {
            // Get the current value in case it has changed
            lPageCurrent = channel.lTail;

            // Verify that we have not already moved the page between
            // the last check and the synchronize
            if (lPageCurrent > lPage)
                {
                return CompletableFuture.completedFuture(lPageCurrent);
                }

            // Check that there is not already a move in progress
            futureResult = channel.futureMovePage;
            if (futureResult == null)
                {
                channel.futureMovePage = futureResult = InvocableMapHelper.invokeAsync(
                        m_caches.Usages, channel.keyUsageSync, m_caches.getUnitOfOrder(channel.keyUsageSync.getPartitionId()),
                        new TailAdvancer(lPage + 1), (result, e) ->
                              {
                              if (e == null)
                                  {
                                  updatePageId(channel, result);
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
    protected void updatePageId(Channel channel, long lPageNew)
        {
        if (channel.lTail < lPageNew)
            {
            synchronized (channel)
                {
                if (channel.lTail < lPageNew)
                    {
                    channel.lTail = lPageNew;
                    }
                }
            }

        channel.futureMovePage = null;
        }

    /**
     * Handle the specified error.
     *
     * @param throwable  the error to handle
     */
    protected <R> R handleError(R result, Throwable throwable)
        {
        if (throwable != null)
            {
            // call the BatchingOperationsQueue's error handler to cancel all
            // outstanding elements.
            for (Channel channel : f_aChannel)
                {
                channel.batchingQueue.handleError(throwable, BatchingOperationsQueue.OnErrorAction.Cancel);
                }

            closeInternal(false);
            }

        return result;
        }

    @Override
    public void entryInserted(MapEvent<NotificationKey, int[]> evt)
        {
        }

    @Override
    public void entryUpdated(MapEvent<NotificationKey, int[]> evt)
        {
        }

    @Override
    public void entryDeleted(MapEvent<NotificationKey, int[]> evt)
        {
        ++m_cNotify;

        for (int nChannel : evt.getOldValue())
            {
            Channel channel = f_aChannel[nChannel];
            if (channel.batchingQueue.resume())
                {
                ++m_cWait;
                addQueuedElements(f_aChannel[nChannel], 1);
                }
            }
        }

    /**
     * Instantiate and register a DeactivationListener with the topic data cache.
     */
    @SuppressWarnings("unchecked")
    protected void registerDeactivationListener()
        {
        try
            {
            NamedCacheDeactivationListener listener = f_listenerDeactivation;

            if (listener != null)
                {
                m_caches.Data.addMapListener(listener);
                }
            }
        catch (RuntimeException e) {}
        }

    /**
     * Unregister cache deactivation listener.
     */
    @SuppressWarnings("unchecked")
    protected void unregisterDeactivationListener()
        {
        try
            {
            NamedCacheDeactivationListener listener = f_listenerDeactivation;
            if (listener != null)
                {
                m_caches.Data.removeMapListener(listener);
                }
            }
        catch (RuntimeException e) {}
        }

    // ----- inner class: Channel -------------------------------------------

    /**
     * Channel is a record which represents the state of a channel as known
     * by this publisher.
     */
    protected static class Channel
        {
        /**
         * The key for the Usage object which maintains the channel's tail.
         */
        Usage.Key keyUsageSync;

        /**
         * The {@link BatchingOperationsQueue} controlling the batches of add operations.
         */
        BatchingOperationsQueue<Void> batchingQueue;

        /**
         * The tail for this channel
         */
        volatile long lTail = -1;

        /**
         * The {@link CompletableFuture} that will be complete when the
         * page ID is updated.
         */
        volatile CompletableFuture<Long> futurePageId;

        /**
         * The {@link CompletableFuture} that will complete when the current page increment
         * operation completes. This value will be null if no page increment operation is in
         * progress.
         */
        CompletableFuture<Long> futureMovePage;
        }

    // ----- inner class: DeactivationListener ------------------------------

    /**
     * A {@link NamedCacheDeactivationListener} to detect the subscribed topic
     * being destroyed.
     */
    protected class DeactivationListener
            extends AbstractMapListener
            implements NamedCacheDeactivationListener
        {
        @Override
        public void entryDeleted(MapEvent evt)
            {
            // destroy/disconnect event
            Logger.fine("Detected destroy of topic "
                             + f_sTopicName + ", closing publisher "
                             + PagedTopicPublisher.this);
            closeInternal(true);
            }
        }

    // ----- inner class: FlushMode ----------------------------------------

    static enum FlushMode
        {
        /**
         *  Wait for all outstanding asynchronous operations to complete.
         */
        FLUSH,

        /**
         * Cancel all outstanding asynchronous operations due to topic being destroyed.
         */
        FLUSH_DESTROY,

        /**
         * Complete exceptionally all outstanding asynchronous operations due to timeout during initial {@link #FLUSH} during close.
         */
        FLUSH_CLOSE_EXCEPTIONALLY
        }

    // ----- constants ------------------------------------------------------

    /**
     * A constant to use to make it obvious when methods are returning Void.
     */
    private static final Void VOID = null;

    /**
     * Publisher close timeout on first flush attempt. After this time is exceeded, all outstanding asynchronous operations will be completed exceptionally.
     */
    public static final long CLOSE_TIMEOUT_SECS = TimeUnit.MILLISECONDS.toSeconds(Base.parseTime(Config.getProperty("coherence.topic.publisher.close.timeout", "30s"), Base.UNIT_S));

    // ----- data members ---------------------------------------------------

    /**
     * The {@link PagedTopicCaches} instance managing the caches backing this topic.
     */
    private PagedTopicCaches m_caches;

    /**
     * The name of the topic.
     */
    private final String f_sTopicName;

    /**
     * The {@link Options} controlling this {@link PagedTopicPublisher}'s operation.
     */
    private final Options<Publisher.Option> f_options;

    /**
     * The converter that will convert values being offered to {@link Binary} instances.
     */
    private final Converter<V, Binary> f_convValueToBinary;

    /**
     * The post full notifier.
     */
    private final int f_nNotifyPostFull;

    /**
     * The ordering function.
     */
    private final OrderBy<V> f_funcOrder;

    /**
     * The publisher flow control.
     */
    private final FlowControl f_flowcontrol;

    /**
     * Channel array.
     */
    protected final Channel[] f_aChannel;

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

    /**
     * The set of channels which elements have been offered to since the last @{link #toString} call.
     */
    private final BitSet f_setOfferedChannel;

    /**
     * The NamedCache deactivation listener.
     */
    private final NamedCacheDeactivationListener f_listenerDeactivation = new DeactivationListener();

    /**
     * A {@link List} of actions to run when this publisher closes.
     */
    private final List<Runnable> f_listOnCloseActions = new ArrayList<>();
    }
