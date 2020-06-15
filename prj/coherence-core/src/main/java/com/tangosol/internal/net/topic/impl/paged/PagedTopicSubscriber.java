/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic.impl.paged;

import com.oracle.coherence.common.util.Options;

import com.tangosol.coherence.config.Config;

import com.tangosol.internal.net.DebouncedFlowControl;
import com.tangosol.internal.net.NamedCacheDeactivationListener;

import com.tangosol.internal.net.topic.impl.paged.agent.DestroySubscriptionProcessor;
import com.tangosol.internal.net.topic.impl.paged.agent.EnsureSubscriptionProcessor;
import com.tangosol.internal.net.topic.impl.paged.agent.HeadAdvancer;
import com.tangosol.internal.net.topic.impl.paged.agent.PollProcessor;
import com.tangosol.internal.net.topic.impl.paged.model.NotificationKey;
import com.tangosol.internal.net.topic.impl.paged.model.Page;
import com.tangosol.internal.net.topic.impl.paged.model.SubscriberGroupId;
import com.tangosol.internal.net.topic.impl.paged.model.Subscription;

import com.tangosol.internal.util.Primes;

import com.tangosol.io.Serializer;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.FlowControl;
import com.tangosol.net.PartitionedService;
import com.tangosol.net.topic.Subscriber;

import com.tangosol.util.AbstractMapListener;
import com.tangosol.util.Base;
import com.tangosol.util.Binary;
import com.tangosol.util.CircularArrayList;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.Filter;
import com.tangosol.util.HashHelper;
import com.tangosol.util.InvocableMapHelper;
import com.tangosol.util.MapEvent;
import com.tangosol.util.MapListenerSupport;
import com.tangosol.util.filter.InKeySetFilter;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Queue;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * A subscriber of values from a paged topic.
 *
 * @author jk/mf 2015.06.15
 * @since Coherence 14.1.1
 */
public class PagedTopicSubscriber<V>
    implements Subscriber<V>, AutoCloseable,
    MapListenerSupport.SynchronousListener<NotificationKey, int[]>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a {@link PagedTopicSubscriber}.
     *
     * @param pagedTopicCaches  the {@link PagedTopicCaches} managing the underlying topic data
     * @param options           the {@link Option}s controlling this {@link PagedTopicSubscriber}
     */
    protected <T> PagedTopicSubscriber(PagedTopicCaches pagedTopicCaches, Option<? super T, V>... options)
        {
        Options<Subscriber.Option> optionsMap = Options.from(Subscriber.Option.class, options);
        Name                       nameOption = optionsMap.get(Name.class, null);
        String                     sName      = nameOption == null ? null : nameOption.getName();

        m_listenerDeactivation      = new DeactivationListener();
        m_listenerGroupDeactivation = new GroupDeactivationListener();
        f_caches                    = Objects.requireNonNull(pagedTopicCaches, "The TopicCaches parameter cannot be null");
        f_serializer                = f_caches.getSerializer();
        f_subscriberGroupId         = sName == null
            ? SubscriberGroupId.anonymous()
            : SubscriberGroupId.withName(sName);

        registerDeactivationListener();

        // TODO: error out on unprocessed (therefor unsupported) Options
        // TODO: should there be an option to control how we behave with unsupported/unrecognized options, should this
        // be an an option by option basis?

        f_fCompleteOnEmpty = optionsMap.contains(CompleteOnEmpty.class);
        f_nNotificationId  = f_caches.newNotifierId(); // used even if we don't wait to avoid endless channel scanning

        Filtered filtered = optionsMap.get(Filtered.class);
        Filter   filter   = filtered == null ? null : filtered.getFilter();
        Convert  convert  = optionsMap.get(Convert.class);
        Function function = convert == null ? null : convert.getFunction();

        // TODO: it would be good to limit backlog to a page size, but we don't know how many values this will be
        // we could average out received value sizes over time and build this up, but making the DebouncedFlowControl
        // thread-safe may be more of a cost then its' worth
        long cBacklog = f_caches.getCacheService().getCluster().getDependencies().getPublisherCloggedCount();
        f_backlog = new DebouncedFlowControl((cBacklog * 2) / 3, cBacklog);

        try
            {
            int cParts   = f_caches.getPartitionCount();
            int cChannel = f_caches.getChannelCount();

            f_setPolledChannels = new BitSet(cChannel);
            f_setHitChannels    = new BitSet(cChannel);

            List<Subscription.Key> listSubParts = new ArrayList<>(cParts);
            for (int i = 0; i < cParts; ++i)
                {
                // Note: we ensure against channel 0 in each partition, and it will in turn initialize all channels
                listSubParts.add(new Subscription.Key(i, /*nChannel*/ 0, f_subscriberGroupId));
                }

            // outside of any lock discover if pages are already pinned.  Note that since we don't
            // hold a lock, this is only useful if the group was already fully initialized (under lock) earlier.
            // Otherwise there is no guarantee that there isn't gaps in our pinned pages.
            // check results to verify if initialization has already completed
            Collection<long[]> colPages = sName == null
                ? null
                : InvocableMapHelper.invokeAllAsync(
                f_caches.Subscriptions, listSubParts, key -> pagedTopicCaches.getUnitOfOrder(key.getPartitionId()),
                new EnsureSubscriptionProcessor(EnsureSubscriptionProcessor.PHASE_INQUIRE, null, filter, function))
                .get().values();

            long   lPageBase = pagedTopicCaches.getBasePage();
            long[] alHead    = new long[cChannel];

            if (colPages == null || colPages.contains(null))
                {
                // The subscription doesn't exist in at least some partitions, create it under lock. A lock is used only
                // to protect against concurrent create/destroy/create resulting an gaps in the pinned pages.  Specifically
                // it would be safe for multiple subscribers to concurrently "create" the subscription, it is only unsafe
                // if there is also a concurrent destroy as this could result in gaps in the pinned pages.
                if (sName != null)
                    {
                    f_caches.Subscriptions.lock(f_subscriberGroupId, -1);
                    }

                try
                    {
                    colPages = InvocableMapHelper.invokeAllAsync(f_caches.Subscriptions, listSubParts,
                        key -> pagedTopicCaches.getUnitOfOrder(key.getPartitionId()),
                        new EnsureSubscriptionProcessor(EnsureSubscriptionProcessor.PHASE_PIN, null, filter, function))
                        .get().values();

                    Configuration  configuration = f_caches.getConfiguration();

                    // mapPages now reflects pinned pages
                    for (int nChannel = 0; nChannel < cChannel; ++nChannel)
                        {
                        final int finChan = nChannel;

                        if (configuration.isRetainConsumed())
                            {
                            // select lowest page in each channel as our channel heads
                            alHead[nChannel] = colPages.stream()
                                .mapToLong((alPage) -> Math.max(alPage[finChan], lPageBase))
                                .min()
                                .getAsLong();
                            }
                        else
                            {
                            // select highest page in each channel as our channel heads
                            alHead[nChannel] = colPages.stream()
                                .mapToLong((alPage) -> Math.max(alPage[finChan], lPageBase))
                                .max()
                                .getAsLong();
                            }
                        }

                    // finish the initialization by having subscription in all partitions advance to our selected heads
                    InvocableMapHelper.invokeAllAsync(f_caches.Subscriptions, listSubParts,
                        key -> pagedTopicCaches.getUnitOfOrder(key.getPartitionId()),
                        new EnsureSubscriptionProcessor(EnsureSubscriptionProcessor.PHASE_ADVANCE, alHead, filter, function))
                        .join();
                    }
                finally
                    {
                    if (sName != null)
                        {
                        f_caches.Subscriptions.unlock(f_subscriberGroupId);
                        }
                    }
                }
            else
                {
                // all partitions were already initialized, min is our head
                for (int nChannel = 0; nChannel < cChannel; ++nChannel)
                    {
                    final int finChan = nChannel;
                    alHead[nChannel] = colPages.stream().mapToLong((alResult) -> alResult[finChan])
                        .min().orElse(Page.NULL_PAGE);
                    }
                }

            Channel[] aChannel = f_aChannel = new Channel[cChannel];
            for (int nChannel = 0; nChannel < cChannel; ++nChannel)
                {
                Channel channel = aChannel[nChannel] = new Channel();
                channel.lHead   = alHead[nChannel];
                channel.nNext   = -1; // unknown page position to start
                channel.fEmpty  = false; // even if we could infer emptiness here it is unsafe unless we've registered for events

                // we don't just use (0,chan) as that would concentrate extra load on a single partitions when there are many groups
                int nPart = Math.abs((HashHelper.hash(f_subscriberGroupId.hashCode(), nChannel) % cParts));
                channel.subscriberPartitionSync = new Subscription.Key(nPart, nChannel, f_subscriberGroupId);
                }

            // select a random prime step which is larger then the channel count.  This will ensure that this subscriber
            // visits all channels before revisiting any while also "randomizing" the the visitation order w.r.t other
            // members of the same subscription to minimize the chances of contention.
            f_nChannelStep = Primes.random(cChannel);

            m_nChannel = Base.mod(f_nChannelStep, cChannel);

            // register a subscriber listener in each partition, we must be completely setup before doing this
            // as the callbacks assume we're fully initialized
            f_caches.Notifications.addMapListener(this, new InKeySetFilter<>(/*filter*/ null,
                f_caches.getPartitionNotifierSet(f_nNotificationId)), /*fLite*/ false);
            }
        catch (Exception e)
            {
            throw Base.ensureRuntimeException(e);
            }

        // Note: post construction this implementation must be fully async
        }

    // ----- Subscriber methods ---------------------------------------------

    @Override
    public CompletableFuture<Element<V>> receive()
        {
        CompletableFuture<Element<V>> future = new CompletableFuture<>();

        f_queueReceiveOrders.add(future);

        if (m_fClosed) // testing after adding to above queue ensures that concurrent close won't miss canceling a future
            {
            future.cancel(true); // only in case it made it into the set returned from flush
            f_queueReceiveOrders.remove(future); // avoid memory build up in case of repeated post-close calls
            ensureActive(); // throw
            }
        else
            {
            // only after ensuring state do we increment the count, thus we ensure that a value will not be requested
            // on our behalf if we've cancelled above
            f_backlog.incrementBacklog();

            scheduleReceives();
            }

        return future;
        }

    @Override
    public FlowControl getFlowControl()
        {
        return f_backlog;
        }

    // ----- MapListener methods --------------------------------------------

    @Override
    public void entryInserted(MapEvent<NotificationKey, int[]> evt)
        {
        // TODO: filter out this event type
        }

    @Override
    public void entryUpdated(MapEvent<NotificationKey, int[]> evt)
        {
        // TODO: filter out this event type
        }

    @Override
    public void entryDeleted(MapEvent<NotificationKey, int[]> evt)
        {
        ++m_cNotify;

        for (int nChannel : evt.getOldValue())
            {
            f_aChannel[nChannel].fEmpty = false;
            }

        if (f_lockRemoveSubmit.compareAndSet(LOCK_WAIT, LOCK_OPEN))
            {
            switchChannel();
            scheduleReceives();
            }
        // else; we weren't waiting so things are already scheduled
        }

    @Override
    public void onClose(Runnable action)
        {
        f_listOnCloseActions.add(action);
        }

    @Override
    public boolean isActive()
        {
        return !m_fClosed;
        }

    // ----- Closeable methods ----------------------------------------------

    @Override
    public void close()
        {
        closeInternal(false);
        }

    // ----- Object methods -------------------------------------------------

    @Override
    public String toString()
        {
        if (m_fClosed)
            {
            return getClass().getSimpleName() + "(inactive)";
            }

        long cPollsNow  = m_cPolls;
        long cValuesNow = m_cValues;
        long cMissesNow = m_cMisses;
        long cCollNow   = m_cMissCollisions;
        long cWaitNow   = m_cWait;
        long cNotifyNow = m_cNotify;

        long cPoll   = cPollsNow  - m_cPollsLast;
        long cValues = cValuesNow - m_cValuesLast;
        long cMisses = cMissesNow - m_cMissesLast;
        long cColl   = cCollNow   - m_cMissCollisionsLast;
        long cWait   = cWaitNow   - m_cWaitsLast;
        long cNotify = cNotifyNow - m_cNotifyLast;

        m_cPollsLast          = cPollsNow;
        m_cValuesLast         = cValuesNow;
        m_cMissesLast         = cMissesNow;
        m_cMissCollisionsLast = cCollNow;
        m_cWaitsLast          = cWaitNow;
        m_cNotifyLast         = cNotifyNow;

        int    cChannelsPolled = f_setPolledChannels.cardinality();
        int    cChannelsHit    = f_setHitChannels.cardinality();
        String sChannlesHit    = f_setHitChannels.toString();
        f_setPolledChannels.clear();
        f_setHitChannels.clear();

        return getClass().getSimpleName() + "(" + "topic=" + f_caches.getTopicName() +
            ", group=" + f_subscriberGroupId +
            ", closed=" + m_fClosed +
            ", backlog=" + f_backlog +
            ", channels=" + sChannlesHit + cChannelsHit + "/" + cChannelsPolled  +
            ", batchSize=" + (cValues / (Math.max(1, cPoll - cMisses))) +
            ", hitRate=" + ((cPoll - cMisses) * 100 / Math.max(1, cPoll)) + "%" +
            ", colRate=" + (cColl * 100 / Math.max(1, cPoll)) + "%" +
            ", waitNotifyRate=" + (cWait * 100 / Math.max(1, cPoll)) + "/" + (cNotify * 100 / Math.max(1, cPoll)) + "%" +
            ')';
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Ensure that the subscriber is active.
     *
     * @throws IllegalStateException if not active
     */
    private void ensureActive()
        {
        if (!isActive())
            {
            throw new IllegalStateException("The subscriber is not active");
            }
        }

    /**
     * Compare-and-increment the remote head pointer.
     *
     * @param lHeadAssumed  the assumed old value, increment will only occur if the actual head matches this value
     */
    protected void scheduleHeadIncrement(Channel channel, long lHeadAssumed)
        {
        if (!m_fClosed)
            {
            // update the globally visible head page
            InvocableMapHelper.invokeAsync(f_caches.Subscriptions, channel.subscriberPartitionSync,
                f_caches.getUnitOfOrder(channel.subscriberPartitionSync.getPartitionId()),
                new HeadAdvancer(lHeadAssumed + 1),
                (lPriorHeadRemote, e2) ->
                {
                if (lPriorHeadRemote < lHeadAssumed + 1)
                    {
                    // our CAS succeeded, we'd already updated our local head before attempting it
                    // but we do get to clear any contention since the former winner's CAS will fail
                    channel.fContended = false;
                    // we'll allow the channel to be removed from the contended channel list naturally during
                    // the next nextChannel call
                    }
                else
                    {
                    // our CAS failed; i.e. the remote head was already at or beyond where we tried to set it.
                    // comparing against the prior value allows us to know if we won or lost the CAS which
                    // we can use to coordinate contention such that only the losers backoff

                    if (lHeadAssumed != Page.NULL_PAGE)
                        {
                        // we thought we knew what page we were on, but we were wrong, thus someone
                        // else had incremented it, this is a collision.  Backoff and allow them
                        // temporary exclusive access, they'll do the same for the channels we
                        // increment
                        if (!channel.fContended)
                            {
                            channel.fContended = true;
                            f_listChannelsContended.add(channel);
                            }

                        m_cHitsSinceLastCollision = 0;
                        }
                    // else; we knew we were contended, don't doubly backoff

                    if (lPriorHeadRemote > channel.lHead)
                        {
                        // only update if we haven't locally moved ahead; yes it is possible that we lost the
                        // CAS but have already advanced our head simply through brute force polling
                        channel.lHead = lPriorHeadRemote;
                        channel.nNext = -1; // unknown page position
                        }
                    }
                });
            }
        }

    /**
     * Attempt to fulfill any queue'd orders.
     */
    protected void scheduleReceives()
        {
        for (long cOrders = f_backlog.getBacklog();
             cOrders > 0 && !m_fClosed &&
                 f_lockRemoveSubmit.get() == LOCK_OPEN && f_lockRemoveSubmit.compareAndSet(LOCK_OPEN, LOCK_POLL); )
            {
            Collection<Binary> colPrefetched = m_listValuesPrefetched;
            if (colPrefetched != null) // uncommon
                {
                // we already have values we can hand out immediately
                for (Iterator<Binary> iter = colPrefetched.iterator();
                     iter.hasNext() && consumeValue(iter.next());
                     iter.remove())
                    {}

                if (colPrefetched.isEmpty())
                    {
                    m_listValuesPrefetched = null;
                    }
                }

            // update the value now that we hold the lock, it could have increased by other threads, and more
            // importantly it could have decreased as we serviced pre-fetched
            cOrders = f_backlog.getBacklog();
            if (cOrders == 0)
                {
                // no need to request zero items
                f_lockRemoveSubmit.set(LOCK_OPEN);

                // now that we've unlocked we need to ensure that there wasn't a concurrent add which
                // we would be responsible for fetching; re-lock and schedule if necessary
                }
            else // common
                {
                int     nChannel = m_nChannel;
                Channel channel  = f_aChannel[nChannel];
                long    lHead    = channel.lHead;
                int     nPart    = ((PartitionedService) f_caches.Subscriptions.getCacheService())
                    .getKeyPartitioningStrategy().getKeyPartition(new Page.Key(nChannel, lHead));

                InvocableMapHelper.invokeAsync(f_caches.Subscriptions,
                    new Subscription.Key(nPart, nChannel, f_subscriberGroupId), f_caches.getUnitOfOrder(nPart),
                    new PollProcessor(lHead, (int) cOrders, f_nNotificationId),
                    (result, e) -> onReceiveResult(channel, lHead, result, e));

                // at this point we technically don't hold the lock anymore it is held by the EP which oddly may have
                // completed by now.  Most likely we won't execute the loop an additional time as it would be quite
                // unusual (though possible) for the EP to have completed by now.  It would be allowable to just return
                // here but that is just so ugly
                }
            }
        // else; already scheduled or nothing to schedule
        }

    /**
     * Use the specified value to complete one of this subscriber's outstanding futures.
     *
     * @param binValue  the value to consume
     *
     * @return true iff the value was consumed
     */
    protected boolean consumeValue(Binary binValue)
        {
        CompletableFuture<Element<V>> futureNext;
        while ((futureNext = f_queueReceiveOrders.poll()) != null)
            {
            f_backlog.decrementBacklog();
            if (futureNext.complete(new RemovedElement(binValue)))
                {
                return true;
                }
            }

        return false;
        }

    /**
     * Find the next non-empty channel.
     *
     * @param nChannel the current channel
     *
     * @return the next non-empty channel, or nChannel if all are empty
     */
    protected int nextChannel(int nChannel)
        {
        int     cChannels     = f_aChannel.length;
        int     nChannelStart = nChannel;
        boolean fContention   = !f_listChannelsContended.isEmpty();

        while (fContention)
            {
            // it's possible that a channel was marked as empty after already having been identified
            // as contended. i.e. we issue a poll on that channel while we are concurrently querying
            // the head.  The increment fails and the channel is marked as contended, and then we get
            // back an empty result set and
            Channel chan = f_listChannelsContended.get(0);
            if (chan.fEmpty || !chan.fContended)
                {
                f_listChannelsContended.remove(0);
                chan.fContended = false;
                fContention = !f_listChannelsContended.isEmpty();
                }
            else
                {
                break;
                }
            }

        Channel chanContended = m_cHitsSinceLastCollision > COLLISION_BACKOFF_COUNT && fContention
            ? f_listChannelsContended.get(0) : null;

        // if chanContended is non-null then we'll return it unless there are uncontended channels earlier in the
        // search order in which case the contendeds have to wait their turn.  This ensures we check each channel
        // at most once per pass over all other channels unless the channel is contended, in which case we don't
        // select it until we've had a sufficient number of hits

        do
            {
            // long mod is used as nChannel + step may cause overflow and while an int mod would
            // yield a safe array index it wouldn't ensure that we'd eventually visit all channels
            nChannel = (int) Base.mod(nChannel + (long) f_nChannelStep, cChannels);

            Channel channel = f_aChannel[nChannel];

            if (!channel.fEmpty && (!channel.fContended || channel == chanContended))
                {
                return nChannel;
                }
            }
        while (nChannel != nChannelStart);

        // we didn't find any non-empty uncontended channels and ended up back at our start channel.
        // the start channel wasn't selected in the loop thus it is either empty or contended.
        // we know that the first element if any in f_listChannelsContended is non-empty and thus
        // if if exists it is our first choice.
        return fContention
            ? f_listChannelsContended.get(0).subscriberPartitionSync.getChannelId()
            : nChannelStart;
        }

    /**
     * Switch to the next available channel.
     *
     * @return true if a potentially non-empty channel has been found false iff all channels are known to be empty
     */
    protected boolean switchChannel()
        {
        int     nChannelStart = m_nChannel;
        int     nChannel      = m_nChannel = nextChannel(nChannelStart);
        Channel channel       = f_aChannel[nChannel];

        if (channel.fEmpty)
            {
            return false;
            }
        else if (channel.fContended)
            {
            channel.fContended = false; // clear the contention now that we've selected it
            f_listChannelsContended.remove(0);
            }

        int nChannelNext = nextChannel(nChannel);
        if (nChannelNext != nChannel && nChannelNext != nChannelStart)
            {
            Channel channelNext = f_aChannel[nChannelNext];
            if (channelNext.fContended)
                {
                // do read-ahead of the next channel head so that when we finish
                // with the new one we are likely at the proper position for the next
                scheduleHeadIncrement(channelNext, Page.NULL_PAGE);
                }
            }

        return true;
        }

    /**
     * Handle the result of an async receive.
     *
     * @param channel  the associated channel
     * @param lPageId  lTail the page the receive targeted
     * @param result   the result
     * @param e        and exception
     */
    protected void onReceiveResult(Channel channel, long lPageId, PollProcessor.Result result, Throwable e)
        {
        if (e == null)
            {
            int          nChannel   = channel.subscriberPartitionSync.getChannelId();
            List<Binary> listValues = result.getElements();
            int          cReceived  = listValues.size();
            int          cRemaining = result.getRemainingElementCount();
            int          nNext      = result.getNextIndex();

            f_setPolledChannels.set(nChannel);
            ++m_cPolls;

            if (cReceived == 0)
                {
                ++m_cMisses;

                if (channel.nNext != nNext && channel.nNext != -1) // collision
                    {
                    ++m_cMissCollisions;
                    m_cHitsSinceLastCollision = 0;
                    // don't backoff here, as it is possible all subscribers could end up backing off and
                    // the channel would be temporarily abandoned.  We only backoff as part of trying to increment the
                    // page as that is a CAS and for someone to fail, someone else must have succeeded.
                    }
                // else; spurious notify
                }
            else
                {
                f_setHitChannels.set(nChannel);
                ++m_cHitsSinceLastCollision;
                m_cValues += cReceived;

                // fulfill requests
                for (Iterator<Binary> iter = listValues.iterator();
                     iter.hasNext() && consumeValue(iter.next());
                     iter.remove())
                    {}

                if (!listValues.isEmpty())
                    {
                    // hold onto remaining values; these values will be drained before any further values are fetched
                    // see scheduleReceives
                    m_listValuesPrefetched = listValues;
                    }
                }

            channel.nNext = nNext;

            if (cRemaining == PollProcessor.Result.EXHAUSTED)
                {
                // we know the page is exhausted, so the new head is at least one higher
                if (lPageId >= channel.lHead && lPageId != Page.NULL_PAGE)
                    {
                    channel.lHead = lPageId + 1;
                    channel.nNext = 0;
                    }

                // we'll concurrently increment the durable head pointer and then update our pointer accordingly
                scheduleHeadIncrement(channel, lPageId);

                // switch to a new channel since we've exhausted this page
                switchChannel();
                }
            else if (cRemaining == 0)
                {
                channel.fEmpty = true;

                if (!switchChannel())
                    {
                    // we've run out of channels to poll from
                    if (f_fCompleteOnEmpty)
                        {
                        // complete everything with null, we know all channels are currently empty
                        CompletableFuture<Element<V>> next;
                        while ((next = f_queueReceiveOrders.poll()) != null)
                            {
                            f_backlog.decrementBacklog();
                            next.complete(null);
                            }
                        }
                    else
                        {
                        // wait for non-empty;
                        // Note: automatically registered for notification as part of returning an empty result set
                        ++m_cWait;
                        f_lockRemoveSubmit.set(LOCK_WAIT);
                        }
                    }
                }
            else if (cRemaining == PollProcessor.Result.UNKNOWN_SUBSCRIBER)
                {
                // The subscriber was unknown, probably due to being destroyed whilst
                // the poll was in progress.
                closeInternal(true);
                }

            if (m_fClosed)
                {
                // cancelling here ensures we can't loose data as there are no requests on the wire
                f_queueReceiveOrders.forEach(future -> future.cancel(true));

                f_lockRemoveSubmit.set(LOCK_CLOSED);
                }
            else
                {
                // we'll concurrently attempt to remove more
                // Note: the unlock must be done last, specifically we don't want more removes scheduled
                // until after we've updated m_alHead (if it will be done)
                if (f_lockRemoveSubmit.compareAndSet(LOCK_POLL, LOCK_OPEN))
                    {
                    scheduleReceives();
                    }
                // else; we're in LOCK_WAIT state
                }
            }
        else // remove failed; this is fairly catastrophic
            {
            // TODO: figure out error handling
            // fail all currently (and even concurrently) scheduled removes

            CompletableFuture<Element<V>> next;
            while ((next = f_queueReceiveOrders.poll()) != null)
                {
                f_backlog.decrementBacklog();
                next.completeExceptionally(e);
                }

            f_lockRemoveSubmit.set(LOCK_OPEN);
            scheduleReceives();
            }
        }


    /**
     * Destroy subscriber group.
     *
     * @param pagedTopicCaches   the associated caches
     * @param subscriberGroupId  the group to destroy
     */
    static void destroy(PagedTopicCaches pagedTopicCaches, SubscriberGroupId subscriberGroupId)
        {
        int                    cParts       = ((PartitionedService) pagedTopicCaches.Subscriptions.getCacheService()).getPartitionCount();
        List<Subscription.Key> listSubParts = new ArrayList<>(cParts);
        for (int i = 0; i < cParts; ++i)
            {
            // channel 0 will propagate the operation to all other channels
            listSubParts.add(new Subscription.Key(i, /*nChannel*/ 0, subscriberGroupId));
            }

        // see note in TopicSubscriber constructor regarding the need for locking
        boolean fNamed = subscriberGroupId.getMemberTimestamp() == 0;
        if (fNamed)
            {
            pagedTopicCaches.Subscriptions.lock(subscriberGroupId, -1);
            }

        try
            {
            InvocableMapHelper.invokeAllAsync(pagedTopicCaches.Subscriptions, listSubParts,
                (key) -> pagedTopicCaches.getUnitOfOrder(key.getPartitionId()),
                DestroySubscriptionProcessor.INSTANCE)
                .join();
            }
        finally
            {
            if (fNamed)
                {
                pagedTopicCaches.Subscriptions.unlock(subscriberGroupId);
                }
            }
        }

    /**
     * Close and clean-up this subscriber.
     *
     * @param fDestroyed  {@code true} if this call is in response to the caches
     *                    being destroyed/released and hence just clean up local
     *                    state
     */
    private void closeInternal(boolean fDestroyed)
        {
        synchronized (this)
            {
            if (!m_fClosed)
                {
                m_fClosed = true; // accept no new requests, and cause all pending ops to complete ASAP (see onReceiveResult)

                try
                    {
                    if (!fDestroyed)
                        {
                        // caches have not been destroyed so we're just closing this subscriber
                        unregisterDeactivationListener();

                        // un-register the subscriber listener in each partition
                        f_caches.Notifications.removeMapListener(this, new InKeySetFilter<>(/*filter*/ null, f_caches.getPartitionNotifierSet(f_nNotificationId)));
                        }

                    if (fDestroyed || f_lockRemoveSubmit.compareAndSet(LOCK_OPEN, LOCK_CLOSED) ||
                        f_lockRemoveSubmit.compareAndSet(LOCK_WAIT, LOCK_CLOSED))
                        {
                        // we're being destroyed or no EPs outstanding; just cancel everything which is queued
                        f_queueReceiveOrders.forEach(future -> future.cancel(true));
                        }
                    else
                        {
                        // wait for EP result which will fulfill what it can and cancel the rest
                        CompletableFuture.allOf(f_queueReceiveOrders.toArray(new CompletableFuture[0])).handle((v, t) -> null).join();
                        }

                    if (!fDestroyed && f_subscriberGroupId.getMemberTimestamp() != 0)
                        {
                        // this subscriber is anonymous and thus non-durable and must be destroyed upon close
                        // Note: if close isn't the cluster will eventually destroy this subscriber once it
                        // identifies the associated member has left the cluster.
                        // TODO: should we also do it via a finalizer or similar to avoid leaks if app code forgets
                        // to call close?
                        destroy(f_caches, f_subscriberGroupId);
                        }
                    }
                finally
                    {
                    f_listOnCloseActions.forEach(action ->
                    {
                    try
                        {
                        action.run();
                        }
                    catch (Throwable t)
                        {
                        CacheFactory.log(this.getClass().getName() + ".close(): handled onClose exception: " +
                            t.getClass().getCanonicalName() + ": " + t.getMessage(), Base.LOG_QUIET);
                        }
                    });
                    }
                }
            }
        }

    /**
     * Instantiate and register a DeactivationListener with the topic subscriptions cache.
     */
    @SuppressWarnings("unchecked")
    protected void registerDeactivationListener()
        {
        try
            {
            GroupDeactivationListener listenerGroup = m_listenerGroupDeactivation;

            if (listenerGroup != null)
                {
                f_caches.Subscriptions.addMapListener(listenerGroup, new Subscription.Key(0, 0, f_subscriberGroupId), true);
                }

            NamedCacheDeactivationListener listener = m_listenerDeactivation;
            if (listener != null)
                {
                f_caches.Subscriptions.addMapListener(listener);
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
            GroupDeactivationListener listenerGroup = m_listenerGroupDeactivation;

            if (listenerGroup != null)
                {
                f_caches.Subscriptions.removeMapListener(listenerGroup, new Subscription.Key(0, 0, f_subscriberGroupId));
                }

            NamedCacheDeactivationListener listener = m_listenerDeactivation;
            if (listener != null)
                {
                f_caches.Subscriptions.removeMapListener(listener);
                }
            }
        catch (RuntimeException e) {}
        }

    // ----- inner class: RemovedElement ------------------------------------

    /**
     * RemovedElement holds an value removed from the topic.
     */
    private class RemovedElement
        implements Element<V>
        {
        RemovedElement(Binary binValue)
            {
            m_binValue = binValue;
            }

        @Override
        public V getValue()
            {
            Binary binValue = m_binValue;
            V      value    = m_value;
            if (binValue != null)
                {
                synchronized (this)
                    {
                    binValue = m_binValue;
                    if (binValue == null)
                        {
                        value = m_value;
                        }
                    else
                        {
                        m_value    = value = ExternalizableHelper.fromBinary(binValue, f_serializer);
                        m_binValue = null;
                        }
                    }
                }

            return value;
            }

        // ----- data members -----------------------------------------------

        /**
         * The serialized value, null once deserialized.
         */
        private Binary m_binValue;

        /**
         * The removed value, null until deseralized.
         */
        private volatile V m_value;
        }

    /**
     * Channel is a data structure which represents the state of a channel as known
     * by this subscriber.
     */
    protected static class Channel
        {
        /**
         * The current head page for this subscriber, this value may safely be behind (but not ahead) of the actual head.
         *
         * volatile as it is possible it gets concurrently updated by multiple threads if the futures get completed
         * on IO threads.  We don't both going with a full blow AtmoicLong as either value is suitable and worst case
         * we update to an older value and this would be harmless and just get corrected on the next attempt.
         */
        volatile long lHead;

        /**
         * The index of the next item in the page, or -1 for unknown
         */
        int nNext = -1;

        /**
         * True if the channel has been found to be empty.  Once identified as empty we don't need to poll form it again
         * until we receive an event indicating that it has seen a new insertion.
         */
        boolean fEmpty;

        /**
         * The key which holds the channels head for this group.
         */
        Subscription.Key subscriberPartitionSync;

        /**
         * True if contention has been detected on this channel.
         */
        boolean fContended;

        public String toString()
            {
            return "Channel=" + subscriberPartitionSync.getChannelId() +
                ", empty=" + fEmpty +
                ", head=" + lHead +
                ", next=" + nNext +
                ", contended=" + fContended;
            }
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
            CacheFactory.log("Detected destroy of topic "
                + f_caches.getTopicName() + ", closing subscriber "
                + PagedTopicSubscriber.this, CacheFactory.LOG_QUIET);
            closeInternal(true);
            }
        }

    // ----- inner class: GroupDeactivationListener -------------------------

    /**
     * A {@link AbstractMapListener} to detect the removal of the subscriber group
     * that the subscriber is subscribed to.
     */
    protected class GroupDeactivationListener
        extends AbstractMapListener
        {
        @Override
        public void entryDeleted(MapEvent evt)
            {
            // destroy subscriber group
            CacheFactory.log("Detected removal of subscriber group "
                + f_subscriberGroupId.getGroupName() + ", closing subscriber "
                + PagedTopicSubscriber.this, CacheFactory.LOG_QUIET);
            closeInternal(true);
            }
        }

    // ----- constants ------------------------------------------------------

    /**
     * Value of an available lock.
     */
    protected static final int LOCK_OPEN = 0;

    /**
     * Value of a lock which is polling the topic.
     */
    protected static final int LOCK_POLL = 1;

    /**
     * Value of a lock which is waiting on the topic.
     */
    protected static final int LOCK_WAIT = 2;

    /**
     * Value of a lock when subscriber is closing/closed.
     */
    protected static final int LOCK_CLOSED = 3;

    /**
     * The number of hits before we'll retry a previously contended channel.
     */
    protected static final int COLLISION_BACKOFF_COUNT = Integer.parseInt(Config.getProperty(
        "coherence.pagedTopic.collisionBackoff", "100"));

    // ----- data members ---------------------------------------------------

    /**
     * The {@link PagedTopicCaches} instance managing the caches for the topic
     * being consumed.
     */
    protected final PagedTopicCaches f_caches;

    /**
     * The cache's serializer.
     */
    protected final Serializer f_serializer;

    /**
     * The identifier for this {@link PagedTopicSubscriber}.
     */
    protected final SubscriberGroupId f_subscriberGroupId;

    /**
     * This subscriber's notification id.
     */
    protected final int f_nNotificationId;

    /**
     * True if configured to complete when empty
     */
    protected final boolean f_fCompleteOnEmpty;

    /**
     * True iff the subscriber has been closed.
     */
    protected volatile boolean m_fClosed;

    /**
     * Optional list of prefetched values which can be used to fulfil future receive requests.
     */
    protected Collection<Binary> m_listValuesPrefetched;

    /**
     * Queue of pending receive awaiting values.
     */
    protected final Queue<CompletableFuture<Element<V>>> f_queueReceiveOrders = new ConcurrentLinkedQueue<>();

    /**
     * Subscriber flow control object.
     */
    protected final DebouncedFlowControl f_backlog;

    /**
     * The submit lock for remove operations, legal values are from LOCK_* above.
     */
    protected final AtomicInteger f_lockRemoveSubmit = new AtomicInteger();

    /**
     * The state for the channels.
     */
    protected final Channel[] f_aChannel;

    /**
     * The current channel.
     */
    protected int m_nChannel;

    /**
     * The amount to step between channel selection.
     */
    protected final int f_nChannelStep;

    /**
     * The number of poll requests.
     */
    protected long m_cPolls;

    /**
     * The last value of m_cPolls used within {@link #toString} stats.
     */
    protected long m_cPollsLast;

    /**
     * The number of values received.
     */
    protected long m_cValues;

    /**
     * The last value of m_cValues used within {@link #toString} stats.
     */
    protected long m_cValuesLast;

    /**
     * The number of times this subscriber has waited.
     */
    protected long m_cWait;

    /**
     * The last value of m_cWait used within {@link #toString} stats.
     */
    protected long m_cWaitsLast;

    /**
     * The number of misses;
     */
    protected long m_cMisses;

    /**
     * The last value of m_cMisses used within {@link #toString} stats.
     */
    protected long m_cMissesLast;

    /**
     * The number of times a miss was attributable to a collision
     */
    protected long m_cMissCollisions;

    /**
     * The last value of m_cMissCollisions used within {@link #toString} stats.
     */
    protected long m_cMissCollisionsLast;

    /**
     * The number of times this subscriber has been notified.
     */
    protected long m_cNotify;

    /**
     * The last value of m_cNotify used within {@link #toString} stats.
     */
    protected long m_cNotifyLast;

    /**
     * The number of hits since our last miss.
     */
    protected int m_cHitsSinceLastCollision;

    /**
     * List of contended channels, ordered such that those checked longest ago are at the front of the list
     */
    protected final List<Channel> f_listChannelsContended = new CircularArrayList();

    /**
     * BitSet of polled channels since last toString call.
     */
    protected final BitSet f_setPolledChannels;

    /**
     * BitSet of channels which hit since last toString call.
     */
    protected final BitSet f_setHitChannels;

    /**
     * The NamedCache deactivation listener.
     */
    protected NamedCacheDeactivationListener m_listenerDeactivation;

    /**
     * The NamedCache deactivation listener.
     */
    protected GroupDeactivationListener m_listenerGroupDeactivation;

    /**
     * A {@link List} of actions to run when this publisher closes.
     */
    private final List<Runnable> f_listOnCloseActions = new ArrayList<>();
    }