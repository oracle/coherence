/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net.topic.impl.paged;

import com.oracle.coherence.common.base.Blocking;
import com.oracle.coherence.common.base.Exceptions;
import com.oracle.coherence.common.base.Logger;

import com.oracle.coherence.common.util.SafeClock;
import com.tangosol.internal.net.topic.NamedTopicSubscriber;
import com.tangosol.internal.net.topic.NamedTopicSubscriber.TopicChannel;
import com.tangosol.internal.net.topic.ReceiveResult;
import com.tangosol.internal.net.topic.SeekResult;
import com.tangosol.internal.net.topic.SubscriberConnector;
import com.tangosol.internal.net.topic.TopicSubscription;

import com.tangosol.internal.net.topic.impl.paged.agent.CommitProcessor;
import com.tangosol.internal.net.topic.impl.paged.agent.DestroySubscriptionProcessor;
import com.tangosol.internal.net.topic.impl.paged.agent.EvictSubscriber;
import com.tangosol.internal.net.topic.impl.paged.agent.HeadAdvancer;
import com.tangosol.internal.net.topic.impl.paged.agent.PollProcessor;
import com.tangosol.internal.net.topic.impl.paged.agent.SeekProcessor;
import com.tangosol.internal.net.topic.impl.paged.agent.SubscriberHeartbeatProcessor;

import com.tangosol.internal.net.topic.impl.paged.model.ContentKey;

import com.tangosol.internal.net.topic.impl.paged.model.Page;
import com.tangosol.internal.net.topic.impl.paged.model.PageElement;
import com.tangosol.internal.net.topic.impl.paged.model.PagedPosition;
import com.tangosol.internal.net.topic.impl.paged.model.PagedTopicSubscription;
import com.tangosol.internal.net.topic.impl.paged.model.SubscriberGroupId;
import com.tangosol.internal.net.topic.impl.paged.model.SubscriberId;
import com.tangosol.internal.net.topic.impl.paged.model.SubscriberInfo;
import com.tangosol.internal.net.topic.impl.paged.model.Subscription;

import com.tangosol.internal.util.Daemons;

import com.tangosol.net.Member;
import com.tangosol.net.NamedCache;
import com.tangosol.net.PagedTopicService;
import com.tangosol.net.PartitionedService;
import com.tangosol.net.partition.KeyPartitioningStrategy;

import com.tangosol.net.topic.Position;
import com.tangosol.net.topic.Subscriber;
import com.tangosol.net.topic.Subscriber.CommitResult;
import com.tangosol.net.topic.Subscriber.Element;
import com.tangosol.net.topic.TopicDependencies;

import com.tangosol.util.AbstractMapListener;
import com.tangosol.util.Binary;
import com.tangosol.util.Filter;
import com.tangosol.util.Filters;
import com.tangosol.util.InvocableMapHelper;
import com.tangosol.util.Listeners;
import com.tangosol.util.LongArray;
import com.tangosol.util.MapEvent;
import com.tangosol.util.SimpleLongArray;
import com.tangosol.util.UUID;
import com.tangosol.util.ValueExtractor;

import com.tangosol.util.aggregator.ComparableMin;
import com.tangosol.util.aggregator.GroupAggregator;
import com.tangosol.util.aggregator.LongMin;

import com.tangosol.util.extractor.ReflectionExtractor;

import com.tangosol.util.filter.InKeySetFilter;

import com.tangosol.util.listener.SimpleMapListener;

import java.time.Instant;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A {@link SubscriberConnector} that connects a {@link SubscriberConnector.ConnectedSubscriber}
 * to an underlying {@link PagedTopic}.
 *
 * @param <V>  the type of element contained in the topic
 *
 * @author Jonathan Knight  2024.11.26
 */
public class PagedTopicSubscriberConnector<V>
        implements SubscriberConnector<V>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a {@link PagedTopicSubscriberConnector}.
     *
     * @param caches   the underlying {@link PagedTopicCaches} containing the topic data
     * @param options  the options to use to configure the subscriber
     * @param <T>      the type of element received by the subscriber
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public <T> PagedTopicSubscriberConnector(PagedTopicCaches caches, Subscriber.Option<? super T, V>[] options)
        {
        NamedTopicSubscriber.OptionSet<T, V> optionSet = NamedTopicSubscriber.optionsFrom(options);

        f_subscriberGroupId   = optionSet.getSubscriberGroupId();
        f_caches              = Objects.requireNonNull(caches);
        f_heartbeatProcessor  = new SubscriberHeartbeatProcessor();
        f_headSubscriptionKey = Subscription.createSyncKey(f_subscriberGroupId, 0, f_caches.getPartitionCount());

        NamedTopicSubscriber.WithNotificationId withNotificationId = optionSet.get(NamedTopicSubscriber.WithNotificationId.class);
        int                                     nNotificationId;
        if (withNotificationId == null)
            {
            nNotificationId = System.identityHashCode(this);
            }
        else
            {
            nNotificationId = withNotificationId.getId();
            }

        NamedTopicSubscriber.WithSubscriberId withSubscriberId = optionSet.get(NamedTopicSubscriber.WithSubscriberId.class);
        SubscriberId                          subscriberId     = withSubscriberId == null ? null : withSubscriberId.getId(nNotificationId);
        if (subscriberId == null)
            {
            Member member = caches.getService().getCluster().getLocalMember();
            subscriberId = new SubscriberId(nNotificationId, member.getId(), member.getUuid());
            }
        f_subscriberId = subscriberId;
        }

    // ----- BaseTopicSubscriber.Connector methods --------------------------

    @Override
    public boolean isActive()
        {
        return f_caches.isActive()
                && f_caches.getService().isRunning();
        }

    @Override
    public boolean isGroupDestroyed()
        {
        return m_fGroupDestroyed;
        }

    @Override
    public boolean isDestroyed()
        {
        return f_caches.isDestroyed();
        }

    @Override
    public boolean isReleased()
        {
        return f_caches.isReleased();
        }

    @Override
    public String getTypeName()
        {
        return "PagedTopicSubscriber";
        }

    @Override
    public void postConstruct(ConnectedSubscriber<V> subscriber)
        {
        registerChannelAllocationListener(subscriber);
        registerDeactivationListener(subscriber);
        }

    @Override
    public void onInitialized(ConnectedSubscriber<V> subscriber)
        {
        registerNotificationListener(subscriber);
        }

    @Override
    public void addListener(SubscriberListener listener)
        {
        f_listeners.add(listener);
        }

    @Override
    public void removeListener(SubscriberListener listener)
        {
        f_listeners.remove(listener);
        }

    @Override
    public TopicDependencies getTopicDependencies()
        {
        return f_caches.getDependencies();
        }

    @Override
    public long getConnectionTimestamp()
        {
        return m_connectionTimestamp;
        }

    @Override
    public long getSubscriptionId()
        {
        return m_subscriptionId;
        }

    @Override
    public SubscriberId getSubscriberId()
        {
        return f_subscriberId;
        }

    @Override
    public SubscriberGroupId getSubscriberGroupId()
        {
        return f_subscriberGroupId;
        }

    @Override
    public void close()
        {
        }

    @Override
    public void closeSubscription(ConnectedSubscriber<V> subscriber, boolean fDestroyed)
        {
        State state = m_state;
        if (m_state != State.Closing && state != State.Closed)
            {
            f_lockState.lock();
            try
                {
                if (m_state != State.Closed)
                    {
                    m_state = State.Closing;
                    }
                }
            finally
                {
                f_lockState.unlock();
                }
            }

        if (m_state == State.Closing)
            {
            Logger.finest("Closing subscription for topic subscriber: fDestroyed=" + fDestroyed + " subscriber=" + subscriber);
            if (!fDestroyed)
                {
                // caches have not been destroyed, so we're just closing this subscriber
                PagedTopicService service = f_caches.getService();
                boolean           fActive = f_caches.isActive()
                                                && service.isRunning()
                                                && !service.getOwnershipEnabledMembers().isEmpty();
                if (fActive)
                    {
                    unregisterDeactivationListener();
                    unregisterChannelAllocationListener();
                    unregisterNotificationListener();
                    }
                PagedTopicSubscription.notifyClosed(f_caches.Subscriptions, f_subscriberGroupId, m_subscriptionId, f_subscriberId);
                if (fActive)
                    {
                    removeSubscriberEntry(subscriber.getKey());
                    }
                }
            else
                {
                PagedTopicSubscription.notifyClosed(f_caches.Subscriptions, f_subscriberGroupId, m_subscriptionId, f_subscriberId);
                }

            if (!fDestroyed && f_subscriberGroupId.isAnonymous())
                {
                // this subscriber is anonymous and thus non-durable and must be destroyed upon close
                // Note: if close isn't the cluster will eventually destroy this subscriber once it
                // identifies the associated member has left the cluster.
                // If an application creates a lot of subscribers and does not close them when finished
                // then this will cause heap consumption to rise.
                // There used to be a To-Do comment here about cleaning up in a finalizer, but as
                // finalizers in the JVM are not reliable that is probably not such a good idea.
                try
                    {
                    destroy(f_caches, f_subscriberGroupId, m_subscriptionId);
                    }
                catch (Exception e)
                    {
                    Logger.err(e);
                    }
                }

            // We need to ensure that the subscription has really gone.
            // During a fail-over situation the subscriber may still exist in the configmap
            // so we  need to repeat the closure notification
            String            sTopic        = f_caches.getTopicName();
            PagedTopicService service       = f_caches.getService();
            Set<SubscriberId> setSubscriber = service.getSubscribers(sTopic, f_subscriberGroupId);
            while (setSubscriber.contains(f_subscriberId))
                {
                Logger.fine("Repeating subscriber closed notification for topic subscriber: " + subscriber);
                try
                    {
                    Blocking.sleep(100);
                    }
                catch (InterruptedException e)
                    {
                    break;
                    }
                PagedTopicSubscription.notifyClosed(f_caches.Subscriptions, f_subscriberGroupId, m_subscriptionId, f_subscriberId);
                setSubscriber = service.getSubscribers(sTopic, f_subscriberGroupId);
                }
            m_state = State.Closed;
            }
        }

    @Override
    public Subscriber.Element<V> peek(int nChannel, Position position)
        {
        PagedPosition pagedPosition = (PagedPosition) position;
        ContentKey    key           = new ContentKey(nChannel, pagedPosition.getPage(), pagedPosition.getOffset());
        Binary        binary        = f_caches.Data.get(key.toBinary(f_caches.getPartitionCount()));

        return binary == null ? null : PageElement.fromBinary(binary, f_caches.getSerializer());
        }

    @Override
    public int getRemainingMessages(SubscriberGroupId groupId, int... anChannel)
        {
        return f_caches.getRemainingMessages(groupId, anChannel);
        }

    @Override
    public TopicChannel createChannel(ConnectedSubscriber<V> subscriber, int nChannel)
        {
        f_lockState.lock();
        try
            {
            int               cPart = f_caches.getPartitionCount();
            Subscription.Key  key   = Subscription.createSyncKey(f_subscriberGroupId, nChannel, cPart);
            f_aSubscriberPartitionSync.set(nChannel, key);
            return new PagedTopicChannel(nChannel);
            }
        finally
            {
            f_lockState.unlock();
            }
        }

    @Override
    public boolean isCommitted(SubscriberGroupId groupId, int nChannel, Position position)
        {
        return f_caches.isCommitted(groupId, nChannel, position);
        }

    @Override
    public void ensureConnected()
        {
        f_caches.ensureConnected();
        if (f_caches.isActive() && (m_state == State.Initial || m_state == State.Disconnected))
            {
            m_state = State.Connected;
            }
        }

    @Override
    public Position[] initialize(ConnectedSubscriber<V> subscriber, boolean fForceReconnect, boolean fReconnect, boolean fDisconnected)
        {
        ensureConnected();
        if (f_subscriberGroupId.isDurable())
            {
            initializeSubscription(subscriber, fForceReconnect);
            // heartbeat immediately to update the subscriber's timestamp in the Subscriber cache
            heartbeat(subscriber, false);
            }
        return initializeSubscriptionHeads(subscriber, fReconnect, fDisconnected);
        }

    protected void initializeSubscription(ConnectedSubscriber<V> subscriber, boolean fForceReconnect)
        {
        if (m_subscriptionId == 0)
            {
            // this is the first time, so we get the unique id of the subscriber group
            PagedTopicService service = f_caches.getService();
            m_subscriptionId = service.ensureSubscription(f_caches.getTopicName(), f_subscriberGroupId,
                    f_subscriberId, subscriber.getFilter(), subscriber.getConverter());
            }
        else
            {
            // this is a reconnect request, ensure the group still exists
            // ensure this subscriber is subscribed
            if (!ensureSubscription(subscriber, m_subscriptionId, fForceReconnect))
                {
                throw new IllegalStateException("The subscriber group \""
                        + f_subscriberId + "\" (id=" + m_subscriptionId
                        + ") this subscriber was previously subscribed to has been destroyed");
                }
            }
        TopicSubscription subscription = getSubscription(subscriber, m_subscriptionId);
        if (subscription != null)
            {
            m_connectionTimestamp = subscription.getSubscriberTimestamp(f_subscriberId);
            }
        else
            {
            // the subscription may be null during rolling upgrade where the senior is an older version
            m_connectionTimestamp = SafeClock.INSTANCE.getSafeTimeMillis();
            }

        }

    protected Position[] initializeSubscriptionHeads(ConnectedSubscriber<V> subscriber, boolean fReconnect, boolean fDisconnected)
        {
        Filter<?>            filter    = subscriber.getFilter();
        ValueExtractor<?, ?> extractor = subscriber.getConverter();

        long[] alHead = f_caches.initializeSubscription(f_subscriberGroupId, f_subscriberId, m_subscriptionId,
                filter, extractor, fReconnect, false, fDisconnected);

        Position[] positions = new Position[alHead.length];
        for (int nChannel = 0; nChannel < alHead.length; ++nChannel)
            {
            positions[nChannel] = new PagedPosition(alHead[nChannel], PagedPosition.NULL_OFFSET);
            }
        return positions;
        }

    @Override
    public boolean ensureSubscription(ConnectedSubscriber<V> subscriber, long subscriptionId, boolean fForceReconnect)
        {
        PagedTopicService service      = f_caches.getService();
        String            sTopic       = f_caches.getTopicName();
        service.ensureSubscription(sTopic, subscriptionId, f_subscriberId, fForceReconnect);
        return !service.isSubscriptionDestroyed(subscriptionId);
        }

    @Override
    public TopicSubscription getSubscription(ConnectedSubscriber<V> subscriber, long id)
        {
        PagedTopicService service = f_caches.getService();
        return service.getSubscription(id);
        }

    @Override
    public SortedSet<Integer> getOwnedChannels(ConnectedSubscriber<V> subscriber)
        {
        try
            {
            SortedSet<Integer> setChannel;
            TopicSubscription  pagedTopicSubscription = getSubscription(subscriber, m_subscriptionId);
            if (pagedTopicSubscription != null)
                {
                // we have a PagedTopicSubscription so get the channels from it
                setChannel = pagedTopicSubscription.getOwnedChannels(f_subscriberId);
                }
            else
                {
                setChannel = Collections.emptySortedSet();
                }
            return setChannel;
            }
        catch (Exception e)
            {
            throw Exceptions.ensureRuntimeException(e);
            }
        }

    @Override
    public CompletableFuture<CommitResult> commit(ConnectedSubscriber<V> subscriber, int nChannel, Position position)
        {
        PagedPosition      pagedPosition = (PagedPosition) position;
        long               lPage         = pagedPosition.getPage();
        int                cPart         = f_caches.getPartitionCount();
        PartitionedService service       = (PartitionedService) f_caches.Subscriptions.getCacheService();
        Page.Key           pageKey       = new Page.Key(nChannel, lPage);
        int                nPart         = service.getKeyPartitioningStrategy().getKeyPartition(pageKey);

        scheduleHeadIncrement(subscriber, nChannel, lPage - 1).join();

        Set<Subscription.Key> setKeys = ensureSubscriptionKeys(nChannel, cPart, f_subscriberGroupId);

        // We must execute against all Subscription keys for the channel and subscriber group
        CompletableFuture<Map<Subscription.Key, CommitResult>> future
                = CompletableFuture.supplyAsync(() -> f_caches.Subscriptions
                        .invokeAll(setKeys, new CommitProcessor(pagedPosition, f_subscriberId)), Daemons.commonPool());

        return future.handle((map, err) ->
                        {
                        CommitResult result;
                        if (err == null)
                            {
                            // we are only interested in the result for the actual committed position
                            Subscription.Key key = new Subscription.Key(nPart, nChannel, f_subscriberGroupId);
                            result = map.get(key);
                            }
                        else
                            {
                            Logger.err("Commit failure", err);
                            result = new CommitResult(nChannel, position, Subscriber.CommitResultStatus.Rejected, err);
                            }
                        return result;
                        });
        }

        private Set<Subscription.Key> ensureSubscriptionKeys(int nChannel, int nPart, SubscriberGroupId groupId)
            {
            f_lockState.lock();
            try
                {
                Set<Subscription.Key> set = f_aSetSubscriptionKeys.get(nChannel);
                if (set == null)
                    {
                    Set<Subscription.Key> setKeys  = new HashSet<>();
                    for (int p = 0; p < nPart; p++)
                        {
                        setKeys.add(new Subscription.Key(p, nChannel, groupId));
                        }
                    f_aSetSubscriptionKeys.set(nChannel, setKeys);
                    set = setKeys;
                    }
                return set;
                }
            finally
                {
                f_lockState.unlock();
                }
            }

    @Override
    public Map<Integer, Position> getLastCommittedInGroup(SubscriberGroupId groupId)
        {
        return f_caches.getLastCommitted(groupId);
        }

    @Override
    public CompletableFuture<ReceiveResult> receive(ConnectedSubscriber<V> subscriber, int nChannel,
            Position headPosition, long lVersion, ReceiveHandler handler)
        {
        int                     nNotificationId = f_subscriberId.getNotificationId();
        long                    pageHead        = ((PagedPosition) headPosition).getPage();
        long                    lHead           = pageHead == TopicChannel.HEAD_UNKNOWN ? getSubscriptionHead(subscriber, nChannel) : pageHead;
        PartitionedService      service         = (PartitionedService) f_caches.Subscriptions.getCacheService();
        KeyPartitioningStrategy strategy        = service.getKeyPartitioningStrategy();
        int                     nPart           = strategy.getKeyPartition(new Page.Key(nChannel, lHead));
        Executor                executor        = subscriber.getExecutor();
        Subscription.Key        key             = new Subscription.Key(nPart, nChannel, f_subscriberGroupId);
        int                     unitOfOrder     = f_caches.getUnitOfOrder(nPart);
        PollProcessor           processor       = new PollProcessor(lHead, Integer.MAX_VALUE, nNotificationId, f_subscriberId);

        return InvocableMapHelper.invokeAsync(f_caches.Subscriptions, key, unitOfOrder, processor, executor,
                (result, error) -> onReceive(subscriber, nChannel, lVersion, lHead, result, error, handler));
        }

    private void onReceive(ConnectedSubscriber<V> subscriber, int nChannel, long lVersion, long lPageId,
            ReceiveResult result, Throwable error, ReceiveHandler handler)
        {
        handler.onReceive(lVersion, result, error, () ->
            {
            PollProcessor.Result pollResult = (PollProcessor.Result) result;
            int                  cRemaining = result.getRemainingElementCount();
            int                  nNext      = pollResult.getNextIndex();

            subscriber.updateChannel(nChannel, channel ->
                {
                PagedTopicChannel pagedChannel = (PagedTopicChannel) channel;
                PagedPosition     position     = pagedChannel.getHead();
                long              lHead        = position.getPage();

                channel.setHead(new PagedPosition(position.getPage(), nNext));

                if (cRemaining == PollProcessor.Result.EXHAUSTED)
                    {
                    // we know the page is exhausted, so the new head is at least one higher
                    if (lPageId >= lHead && lPageId != Page.NULL_PAGE)
                        {
                        channel.setHead(new PagedPosition(lPageId + 1, 0));
                        }

                    // we're actually on the EMPTY_PAGE, so we'll concurrently increment the durable
                    // head pointer and then update our pointer accordingly
                    if (lPageId == Page.NULL_PAGE)
                        {
                        scheduleHeadIncrement(subscriber, nChannel, lPageId);
                        }
                    }
                });
            });
        }


    @Override
    public Map<Integer, Position> getTopicHeads(int[] anChannel)
        {
        ValueExtractor<Page, Integer> extractorChannel = new ReflectionExtractor<>("getChannelId", new Object[0], ReflectionExtractor.KEY);
        ValueExtractor<Page, Long>    extractorPage    = new ReflectionExtractor<>("getPageId", new Object[0], ReflectionExtractor.KEY);
        Map<Integer, Long>            mapHeads         = f_caches.Pages.aggregate(GroupAggregator.createInstance(extractorChannel, new LongMin<>(extractorPage)));

        Map<Integer, Position> mapSeek = new HashMap<>();
        for (int nChannel : anChannel)
            {
            mapSeek.put(nChannel, new PagedPosition(mapHeads.get(nChannel), -1));
            }
        return mapSeek;
        }

    @Override
    public Map<Integer, Position> getTopicTails()
        {
        return f_caches.getTails();
        }


//    @Override
    public SeekResult seekToPosition(ConnectedSubscriber<V> subscriber, int nChannel, Position position)
        {
        // We must execute against all Subscription keys for the same channel and subscriber group
        Set<Subscription.Key> setKeys      = ensureSubscriptionKeys(nChannel, f_caches.getPartitionCount(), f_subscriberGroupId);
        SeekProcessor         processor    = new SeekProcessor((PagedPosition) position, f_subscriberId);

        Map<Subscription.Key, SeekResult> mapResult = f_caches.Subscriptions.invokeAll(setKeys, processor);

        // the new head is the lowest non-null returned position
        return mapResult.values()
                .stream()
                .filter(Objects::nonNull)
                .sorted()
                .findFirst()
                .orElse(null);
        }

    @Override
    public Map<Integer, SeekResult> seekToPosition(ConnectedSubscriber<V> subscriber, Map<Integer, Position> map)
        {
        Map<Integer, SeekResult> mapResult = new HashMap<>();
        for (Map.Entry<Integer, Position> entry : map.entrySet())
            {
            int        nChannel = entry.getKey();
            SeekResult result   = seekToPosition(subscriber, nChannel, entry.getValue());
            mapResult.put(nChannel, result);
            subscriber.updateSeekedChannel(nChannel, result);
            }
        return mapResult;
        }

    @Override
    public Map<Integer, SeekResult> seekToTimestamp(ConnectedSubscriber<V> subscriber, Map<Integer, Instant> map)
        {
        Map<Integer, SeekResult> mapResult = new HashMap<>();
        for (Map.Entry<Integer, Instant> entry : map.entrySet())
            {
            int        nChannel = entry.getKey();
            Position   position = getPositionAfterTimestamp(nChannel, entry.getValue());
            SeekResult result   = seekToPosition(subscriber, nChannel, position);
            mapResult.put(nChannel, result);
            subscriber.updateSeekedChannel(nChannel, result);
            }
        return mapResult;
        }

    @SuppressWarnings("unchecked")
    public Position getPositionAfterTimestamp(int nChannel, Instant timestamp)
        {
        ValueExtractor<Object, Integer>  extractorChannel   = Page.ElementExtractor.chained(Element::getChannel);
        ValueExtractor<Object, Instant>  extractorTimestamp = Page.ElementExtractor.chained(Element::getTimestamp);
        ValueExtractor<Object, Position> extractorPosition  = Page.ElementExtractor.chained(Element::getPosition);

        Binary bin = f_caches.Data.aggregate(
                Filters.equal(extractorChannel, nChannel).and(Filters.greater(extractorTimestamp, timestamp)),
                new ComparableMin<>(extractorPosition));

        PagedPosition position = (PagedPosition) f_caches.getService().getBackingMapManager()
                .getContext().getValueFromInternalConverter().convert(bin);

        if (position == null)
            {
            return null;
            }

        int nOffset = position.getOffset();
        if (nOffset == 0)
            {
            // The position found is the head of a page, so we actually want to seek to the previous element
            // We don;t know the tail of that page, so we can use Integer.MAX_VALUE
            return new PagedPosition(position.getPage() - 1, Integer.MAX_VALUE);
            }
        // else we are not at the head of a page so seek to the page and previous offset
        return new PagedPosition(position.getPage(), nOffset - 1);
        }

    @Override
    public void heartbeat(ConnectedSubscriber<V> subscriber, boolean fAsync)
        {
        if (f_subscriberGroupId.isDurable())
            {
            UUID               uuid = f_subscriberId.getUID();
            SubscriberInfo.Key key  = subscriber.getKey();
            // we're not anonymous so send a poll heartbeat
            f_heartbeatProcessor.setUuid(uuid);
            f_heartbeatProcessor.setSubscription(subscriber.getSubscriptionId());
            f_heartbeatProcessor.setlConnectionTimestamp(subscriber.getConnectionTimestamp());
            if (fAsync)
                {
                f_caches.Subscribers.async().invoke(key, f_heartbeatProcessor);
                }
            else
                {
                f_caches.Subscribers.invoke(key, f_heartbeatProcessor);
                }
            }

        }

    // ----- helper methods -------------------------------------------------

    /**
     * Destroy a subscriber group.
     *
     * @param caches   the associated caches
     * @param groupId  the group to destroy
     */
    public static void destroy(PagedTopicCaches caches, SubscriberGroupId groupId, long lSubscriptionId)
        {
        PagedTopicService service = caches.getService();
        if (lSubscriptionId == 0 && !groupId.isAnonymous())
            {
            lSubscriptionId = service.getSubscriptionId(caches.getTopicName(), groupId);
            }

        service.destroySubscription(lSubscriptionId);

        if (caches.isActive() && caches.Subscriptions.isActive())
            {
            int                   cParts      = service.getPartitionCount();
            Set<Subscription.Key> setSubParts = new HashSet<>(cParts);

            for (int i = 0; i < cParts; ++i)
                {
                // channel 0 will propagate the operation to all other channels
                setSubParts.add(new Subscription.Key(i, /*nChannel*/ 0, groupId));
                }


            DestroySubscriptionProcessor processor = new DestroySubscriptionProcessor(lSubscriptionId);
            InvocableMapHelper.invokeAllAsync(caches.Subscriptions, setSubParts,
                            (key) -> caches.getUnitOfOrder(key.getPartitionId()),
                            processor)
                    .join();
            }
        }

    /**
     * Compare-and-increment the remote head pointer.
     *
     * @param lHeadAssumed  the assumed old value, increment will only occur if the actual head matches this value
     *
     * @return a {@link CompletableFuture} that completes with the new head
     */
    protected CompletableFuture<Long> scheduleHeadIncrement(ConnectedSubscriber<V> subscriber, int nChannel, long lHeadAssumed)
        {
        if (isActive())
            {
            Subscription.Key syncKey   = f_aSubscriberPartitionSync.get(nChannel);
            int              nPart     = syncKey.getPartitionId();
            Executor         executor  = subscriber.getExecutor();
            HeadAdvancer     processor = new HeadAdvancer(lHeadAssumed + 1);
            int              nOrder    = f_caches.getUnitOfOrder(nPart);

            // update the globally visible head page
            return InvocableMapHelper.invokeAsync(f_caches.Subscriptions, syncKey, nOrder, processor, executor,
                    (lPriorHeadRemote, e2) ->
                            subscriber.updateChannel(nChannel, channel ->
                                {
                                PagedTopicChannel pagedChannel = (PagedTopicChannel) channel;
                                if (lPriorHeadRemote >= lHeadAssumed + 1)
                                    {
                                    // our CAS failed; i.e. the remote head was already at or beyond where we tried to set it.
                                    // comparing against the prior value allows us to know if we won or lost the CAS which
                                    // we can use to coordinate contention such that only the losers backoff
                                    PagedPosition     position = pagedChannel.getHead();
                                    long              lHead    = position.getPage();

                                    if (lPriorHeadRemote > lHead)
                                        {
                                        // only update if we haven't locally moved ahead; yes it is possible that we lost the
                                        // CAS but have already advanced our head simply through brute force polling
                                        channel.setHead(new PagedPosition(lPriorHeadRemote, PagedPosition.NULL_OFFSET));
                                        }
                                    }
                                }));
            }
        return CompletableFuture.completedFuture(-1L);
        }

    /**
     * Returns the initial head page.
     *
     * @return the initial head page
     */
    private long getSubscriptionHead(ConnectedSubscriber<?> subscriber, int nChannel)
        {
        Subscription.Key  syncKey      = f_aSubscriberPartitionSync.get(nChannel);
        Subscription      subscription = f_caches.Subscriptions.get(syncKey);
        return subscription.getSubscriptionHead();
        }

    /**
     * Called to remove the entry for this subscriber from the subscriber info cache.
     */
    protected void removeSubscriberEntry(SubscriberInfo.Key key)
        {
        NamedCache<SubscriberInfo.Key, SubscriberInfo> cache = f_caches.Subscribers;
        if (!cache.isActive())
            {
            // cache is already inactive so we cannot do anything
            return;
            }

        try
            {
            cache.invoke(key, EvictSubscriber.INSTANCE);
            }
        catch (Throwable t)
            {
            Logger.err(t);
            }
        }

    /**
     * Instantiate and register a MapListener with the topic subscriptions cache that
     * will listen for changes in channel allocations.
     */
    protected void registerChannelAllocationListener(ConnectedSubscriber<V> subscriber)
        {
        f_lockState.lock();
        try
            {
            m_subscriptionListener = new SubscriptionListener(subscriber);
            m_subscriptionKey = new PagedTopicSubscription.Key(f_caches.getTopicName(), f_subscriberGroupId);
            f_caches.f_topicService.addSubscriptionListener(m_subscriptionListener);
            }
        catch (RuntimeException e)
            {
            Logger.err(e);
            }
        finally
            {
            f_lockState.unlock();
            }
        }

    /**
     * Unregister the channel allocation listener.
     */
    protected void unregisterChannelAllocationListener()
        {
        try
            {
            PagedTopicSubscription.Listener listener = m_subscriptionListener;
            if (listener != null)
                {
                f_caches.f_topicService.removeSubscriptionListener(listener);
                }
            }
        catch (RuntimeException e)
            {
            Logger.err(e);
            }
        }

    /**
     * Instantiate and register a DeactivationListener with the topic subscriptions cache.
     */
    @SuppressWarnings("unchecked")
    protected void registerDeactivationListener(ConnectedSubscriber<V> subscriber)
        {
        f_lockState.lock();
        try
            {
            if (f_subscriberGroupId.isDurable())
                {
                m_listenerGroupDeactivation = new GroupDeactivationListener();
                f_caches.Subscriptions.addMapListener(m_listenerGroupDeactivation, f_headSubscriptionKey, true);
                }
            m_listenerDeactivation = new DeactivationListener();
            f_caches.addListener(m_listenerDeactivation);
            }
        catch (RuntimeException e)
            {
            // intentionally empty
            }
        finally
            {
            f_lockState.unlock();
            }
        }

    /**
     * Unregister cache deactivation listener.
     */
    @SuppressWarnings("unchecked")
    protected void unregisterDeactivationListener()
        {
        try
            {
            if (f_caches.isActive())
                {
                if (f_caches.Subscriptions.isActive() && m_listenerGroupDeactivation != null)
                    {
                    f_caches.Subscriptions.removeMapListener(m_listenerGroupDeactivation, f_headSubscriptionKey);
                    }

                if (m_listenerDeactivation != null)
                    {
                    f_caches.removeListener(m_listenerDeactivation);
                    }
                }
            }
        catch (Exception e)
            {
            // intentionally empty
            }
        }

    private void onChannelPopulatedNotification(MapEvent<?, ?> evt)
        {
        int[] anChannel = (int[]) evt.getOldValue();
        SubscriberEvent event = new SubscriberEvent(PagedTopicSubscriberConnector.this, SubscriberEvent.Type.ChannelPopulated, anChannel);
        event.dispatch(f_listeners);
        }

    @SuppressWarnings("unchecked")
    protected void registerNotificationListener(ConnectedSubscriber<V> subscriber)
        {
        // register a subscriber listener in each partition, we must be completely setup before doing this
        // as the callbacks assume we're fully initialized
        if (f_caches.Notifications.isActive())
            {
            int nNotificationId = subscriber.getNotificationId();
            m_listenerNotification = new SimpleMapListener<>().addDeleteHandler(this::onChannelPopulatedNotification);
            m_filterNotification   = new InKeySetFilter<>(/*filter*/ null, f_caches.getPartitionNotifierSet(nNotificationId));
            f_caches.Notifications.addMapListener(m_listenerNotification, m_filterNotification, /*fLite*/ false);
            }
        }

    @SuppressWarnings({"unchecked", "rawtypes"})
    protected void unregisterNotificationListener()
        {
        try
            {
            // un-register the subscriber listener in each partition
            SimpleMapListener listener = m_listenerNotification;
            Filter<?>         filter   = m_filterNotification;
            if (f_caches.Notifications.isActive() && listener != null)
                {
                f_caches.Notifications.removeMapListener(listener, filter);
                }
            }
        catch (Exception e)
            {
            // intentionally empty
            }
        }

    // ----- inner enum: State ----------------------------------------------

    public enum State
        {
        Initial,
        Connected,
        Disconnected,
        Closing,
        Closed,
        }

    // ----- inner class: SubscriptionListener ------------------------------

    protected class SubscriptionListener
            implements PagedTopicSubscription.Listener
        {
        public SubscriptionListener(ConnectedSubscriber<?> subscriber)
            {
            f_subscriberId = subscriber.getSubscriberId();
            f_fDurable     = subscriber.getSubscriberGroupId().isDurable();
            }

        @Override
        public void onUpdate(PagedTopicSubscription subscription)
            {
            if (Objects.equals(subscription.getKey(), m_subscriptionKey))
                {
                if (!isActive())
                    {
                    return;
                    }

                SortedSet<Integer> setChannel   = null;
                if (subscription.hasSubscriber(f_subscriberId))
                    {
                    setChannel = subscription.getOwnedChannels(f_subscriberId);
                    }

                if (setChannel != null && setChannel.isEmpty())
                    {
                    SubscriberEvent event = new SubscriberEvent(PagedTopicSubscriberConnector.this, SubscriberEvent.Type.ChannelsLost, PagedTopicSubscription.NO_CHANNELS);
                    event.dispatch(f_listeners);
                    }
                else
                    {
                    if (setChannel != null)
                        {
                        SubscriberEvent event = new SubscriberEvent(PagedTopicSubscriberConnector.this, SubscriberEvent.Type.ChannelAllocation, setChannel);
                        event.dispatch(f_listeners);
                        }
                    else if (isActive() && f_fDurable)
                        {
                        Logger.finest("Disconnecting Subscriber (null channel set) " + PagedTopicSubscriberConnector.this);
                        SubscriberEvent event = new SubscriberEvent(PagedTopicSubscriberConnector.this, SubscriberEvent.Type.Unsubscribed, PagedTopicSubscription.NO_CHANNELS);
                        event.dispatch(f_listeners);
                        }
                    }
                }
            }

        @Override
        public void onDelete(PagedTopicSubscription subscription)
            {
            if (Objects.equals(subscription.getKey(), m_subscriptionKey))
                {
                SubscriberEvent event = new SubscriberEvent(PagedTopicSubscriberConnector.this, SubscriberEvent.Type.ChannelsLost);
                event.dispatch(f_listeners);
                }
            }

        // ----- data members -----------------------------------------------

        private final SubscriberId f_subscriberId;

        private final boolean f_fDurable;
        }

    // ----- inner class: DeactivationListener ------------------------------

    /**
     * A {@link PagedTopicCaches.Listener} to detect the subscribed topic deactivation.
     */
    protected class DeactivationListener
        implements PagedTopicCaches.Listener
        {
        @Override
        public void onConnect()
            {
            f_lockState.lock();
            try
                {
                if (m_state == State.Disconnected || m_state == State.Initial)
                    {
                    m_state = State.Connected;
                    }
                }
            finally
                {
                f_lockState.unlock();
                }
            }

        @Override
        public void onDisconnect()
            {
            f_lockState.lock();
            try
                {
                if (m_state == State.Connected || m_state == State.Initial)
                    {
                    m_state = State.Disconnected;
                    SubscriberEvent event = new SubscriberEvent(PagedTopicSubscriberConnector.this, SubscriberEvent.Type.Disconnected);
                    event.dispatch(f_listeners);
                    }
                }
            finally
                {
                f_lockState.unlock();
                }
            }

        @Override
        public void onDestroy()
            {
            f_lockState.lock();
            try
                {
                if (m_state != State.Closed && m_state != State.Closing)
                    {
                    m_state = State.Closed;
                    SubscriberEvent event = new SubscriberEvent(PagedTopicSubscriberConnector.this, SubscriberEvent.Type.Destroyed);
                    event.dispatch(f_listeners);
                    }
                }
            finally
                {
                f_lockState.unlock();
                }
            }

        @Override
        public void onRelease()
            {
            f_lockState.lock();
            try
                {
                if (m_state != State.Closed && m_state != State.Closing)
                    {
                    m_state = State.Closed;
                    SubscriberEvent event = new SubscriberEvent(PagedTopicSubscriberConnector.this, SubscriberEvent.Type.Released);
                    event.dispatch(f_listeners);
                    }
                }
            finally
                {
                f_lockState.unlock();
                }
            }
        }

    // ----- inner class: GroupDeactivationListener ---------------------

    /**
     * A {@link AbstractMapListener} to detect the removal of the subscriber group
     * that the subscriber is subscribed to.
     */
    protected class GroupDeactivationListener
        extends AbstractMapListener
        {
        @Override
        @SuppressWarnings("rawtypes")
        public void entryDeleted(MapEvent evt)
            {
            m_fGroupDestroyed = true;
            SubscriberEvent event = new SubscriberEvent(PagedTopicSubscriberConnector.this, SubscriberEvent.Type.GroupDestroyed);
            event.dispatch(f_listeners);
            }
        }

    // ----- inner class: Channel -------------------------------------------
    
    /**
     * Channel is a data structure which represents the state of a channel as known
     * by this subscriber.
     */
    public static class PagedTopicChannel
            extends TopicChannel
            implements Subscriber.Channel
        {
        public PagedTopicChannel(int nChannel)
            {
            m_nChannel = nChannel;
            m_head     = new PagedPosition(HEAD_UNKNOWN, -1);
            }
    
        @Override
        public int getId()
            {
            return m_nChannel;
            }
    
        @Override
        public PagedPosition getHead()
            {
            if (((PagedPosition) m_head).getPage() == Page.EMPTY)
                {
                return PagedPosition.NULL_POSITION;
                }
            return (PagedPosition) m_head;
            }
    
        // ----- Object methods ---------------------------------------------
    
        public String toString()
            {
            return "Channel=" + getId() +
                    ", owned=" + m_fOwned +
                    ", empty=" + m_fEmpty +
                    ", version=" + m_lVersion.get() +
                    ", head=" + m_head +
                    ", polls=" + m_cPolls +
                    ", received=" + m_cReceived.getCount() +
                    ", committed=" + m_cCommited +
                    ", first=" + m_firstPolled +
                    ", firstTimestamp=" + m_firstPolledTimestamp +
                    ", last=" + m_lastPolled +
                    ", lastTimestamp=" + m_lastPolledTimestamp +
                    ", contended=" + m_fContended;
            }
    
        // ----- constants --------------------------------------------------
    
        /**
         * A page id value to indicate that the head page is unknown.
         */
        public static final int HEAD_UNKNOWN = -1;
    
        // ----- data members -----------------------------------------------

        /**
         * The channel identifier.
         */
        private final int m_nChannel;
        }

    // ----- data members ---------------------------------------------------

    private State m_state = State.Initial;

    /**
     * The {@link PagedTopicCaches} to use to invoke cache operations.
     */
    private final PagedTopicCaches f_caches;

    /**
     * The unique subscriber identifier.
     */
    private final SubscriberId f_subscriberId;

    /**
     * The subscriber group identifier.
     */
    private final SubscriberGroupId f_subscriberGroupId;

    /**
     * The key for the subscriber.
     */
    private PagedTopicSubscription.Key m_subscriptionKey;

    /**
     * The deactivation listener.
     */
    private DeactivationListener m_listenerDeactivation;

    /**
     * The NamedCache deactivation listener.
     */
    private GroupDeactivationListener m_listenerGroupDeactivation;

    /**
     * The filter used to register the notification listener.
     */
    private Filter<?> m_filterNotification;

    /**
     * The listener that receives notifications for non-empty channels.
     */
    @SuppressWarnings("rawtypes")
    private SimpleMapListener m_listenerNotification;

    /**
     * The entry processor to use to heartbeat the server.
     */
    protected final SubscriberHeartbeatProcessor f_heartbeatProcessor;

    /**
     * The head subscription key.
     */
    private final Subscription.Key f_headSubscriptionKey;

    /**
     * The key which holds the channels head for this group.
     */
    @SuppressWarnings("unchecked")
    private final LongArray<Subscription.Key> f_aSubscriberPartitionSync = new SimpleLongArray();

    /**
     * The set of subscription keys.
     */
    @SuppressWarnings("unchecked")
    private final LongArray<Set<Subscription.Key>> f_aSetSubscriptionKeys = new SimpleLongArray();

    /**
     * The lock controlling updates to state.
     */
    private final Lock f_lockState = new ReentrantLock();

    /**
     * The paged topic subscription listener.
     */
    private SubscriptionListener m_subscriptionListener;

    /**
     * The registered {@link SubscriberListener} instances.
     */
    private final Listeners f_listeners = new Listeners();

    /**
     * A flag to indicate that this subscriber's group was destroyed.
     */
    private boolean m_fGroupDestroyed = false;

    /**
     * The unique identifier for the subscriber group.
     */
    protected long m_subscriptionId;

    /**
     * The subscriber's connection timestamp.
     */
    protected volatile long m_connectionTimestamp;
    }
