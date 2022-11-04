/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic.impl.paged;

import com.oracle.coherence.common.base.Exceptions;
import com.oracle.coherence.common.base.Logger;

import com.oracle.coherence.common.base.TimeHelper;
import com.oracle.coherence.common.util.Options;
import com.oracle.coherence.common.util.Sentry;

import com.tangosol.coherence.config.Config;

import com.tangosol.internal.net.DebouncedFlowControl;

import com.tangosol.internal.net.metrics.Meter;
import com.tangosol.internal.net.topic.impl.paged.agent.CloseSubscriptionProcessor;
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
import com.tangosol.internal.net.topic.impl.paged.model.SubscriberGroupId;
import com.tangosol.internal.net.topic.impl.paged.model.SubscriberId;
import com.tangosol.internal.net.topic.impl.paged.model.SubscriberInfo;
import com.tangosol.internal.net.topic.impl.paged.model.Subscription;

import com.tangosol.io.Serializer;

import com.tangosol.net.CacheService;
import com.tangosol.net.Cluster;
import com.tangosol.net.DistributedCacheService;
import com.tangosol.net.FlowControl;
import com.tangosol.net.Member;
import com.tangosol.net.NamedCache;
import com.tangosol.net.PartitionedService;

import com.tangosol.net.events.EventDispatcher;
import com.tangosol.net.events.EventDispatcherAwareInterceptor;

import com.tangosol.net.events.partition.cache.EntryEvent;
import com.tangosol.net.events.partition.cache.PartitionedCacheDispatcher;

import com.tangosol.net.management.MBeanHelper;

import com.tangosol.net.topic.NamedTopic;
import com.tangosol.net.topic.Position;
import com.tangosol.net.topic.Subscriber;
import com.tangosol.net.topic.TopicException;

import com.tangosol.util.AbstractMapListener;
import com.tangosol.util.Base;
import com.tangosol.util.Binary;
import com.tangosol.util.CircularArrayList;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.Filter;
import com.tangosol.util.Filters;
import com.tangosol.util.Gate;
import com.tangosol.util.InvocableMapHelper;
import com.tangosol.util.LongArray;
import com.tangosol.util.MapEvent;
import com.tangosol.util.MapListener;
import com.tangosol.util.MultiplexingMapListener;
import com.tangosol.util.SparseArray;
import com.tangosol.util.TaskDaemon;
import com.tangosol.util.ThreadGateLite;
import com.tangosol.util.ValueExtractor;

import com.tangosol.util.aggregator.ComparableMin;
import com.tangosol.util.aggregator.GroupAggregator;
import com.tangosol.util.aggregator.LongMin;

import com.tangosol.util.extractor.ReflectionExtractor;

import com.tangosol.util.filter.InKeySetFilter;

import com.tangosol.util.listener.SimpleMapListener;

import java.time.Instant;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;

import java.util.stream.Collectors;

/**
 * A subscriber of values from a paged topic.
 *
 * @author jk/mf 2015.06.15
 * @since Coherence 14.1.1
 */
@SuppressWarnings("rawtypes")
public class PagedTopicSubscriber<V>
    implements Subscriber<V>, AutoCloseable
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a {@link PagedTopicSubscriber}.
     *
     * @param topic    the underlying {@link PagedTopic} that this subscriber is subscribed to
     * @param caches   the {@link PagedTopicCaches} managing the underlying topic data
     * @param options  the {@link Option}s controlling this {@link PagedTopicSubscriber}
     *
     * @throws NullPointerException if the {@code topic} or {@code caches} parameters are {@code null}
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public <T> PagedTopicSubscriber(NamedTopic<?> topic, PagedTopicCaches caches, Option<? super T, V>... options)
        {
        f_topic  = Objects.requireNonNull(topic);
        m_caches = Objects.requireNonNull(caches, "The TopicCaches parameter cannot be null");

        Options<Subscriber.Option> optionsMap = Options.from(Subscriber.Option.class, options);
        Name                       nameOption = optionsMap.get(Name.class, null);
        String                     sName      = nameOption == null ? null : nameOption.getName();

        f_fAnonymous                 = sName == null;
        m_listenerGroupDeactivation  = new GroupDeactivationListener();
        m_listenerChannelAllocation  = new ChannelListener();
        f_serializer                 = m_caches.getSerializer();
        f_listenerNotification       = new SimpleMapListener<>().addDeleteHandler(this::onChannelPopulatedNotification);
        f_gate                       = new ThreadGateLite<>();

        ChannelOwnershipListeners<T> listeners = optionsMap.get(ChannelOwnershipListeners.class, ChannelOwnershipListeners.none());
        m_aChannelOwnershipListener = listeners.getListeners().toArray(new ChannelOwnershipListener[0]);

        CacheService cacheService = m_caches.getService();
        Cluster      cluster      = cacheService.getCluster();
        Member       member       = cluster.getLocalMember();

        f_fCompleteOnEmpty   = optionsMap.contains(CompleteOnEmpty.class);
        f_nNotificationId    = System.identityHashCode(this); // used even if we don't wait to avoid endless channel scanning
        f_filterNotification = new InKeySetFilter<>(/*filter*/ null, m_caches.getPartitionNotifierSet(f_nNotificationId));
        f_id                 = f_fAnonymous
                                    ? new SubscriberId(0, member.getId(), member.getUuid())
                                    : new SubscriberId(f_nNotificationId, member.getId(), member.getUuid());

        f_subscriberGroupId  = f_fAnonymous ? SubscriberGroupId.anonymous() : SubscriberGroupId.withName(sName);
        f_key                = new SubscriberInfo.Key(f_subscriberGroupId, f_id.getId());

        Filtered filtered = optionsMap.get(Filtered.class);
        f_filter = filtered == null ? null : filtered.getFilter();

        Convert convert = optionsMap.get(Convert.class);
        f_fnConverter = convert == null ? null : convert.getFunction();

        f_taskReconnect = new ReconnectTask(this);

        f_daemon = new TaskDaemon("PagedTopic:Subscriber:" + m_caches.getTopicName() + ":" + f_id.getId());
        f_daemon.start();
        f_daemonChannels = new TaskDaemon("PagedTopic:Subscriber:" + m_caches.getTopicName() + ":Channels:" + f_id.getId());
        f_daemonChannels.start();

        long cBacklog = cluster.getDependencies().getPublisherCloggedCount();
        f_backlog            = new DebouncedFlowControl((cBacklog * 2) / 3, cBacklog);
        f_queueReceiveOrders = new BatchingOperationsQueue<>(this::trigger, 1,
                                        f_backlog, v -> 1, BatchingOperationsQueue.Executor.fromTaskDaemon(f_daemon));

        int cChannel = m_caches.getChannelCount();

        f_setPolledChannels = new BitSet(cChannel);
        f_setHitChannels    = new BitSet(cChannel);
        m_aChannel          = initializeChannels(m_caches, cChannel, f_subscriberGroupId);

        registerChannelAllocationListener();
        registerDeactivationListener();
        registerMBean();

        ensureConnected();

        // Note: post construction this implementation must be fully async
        }

    // ----- accessors ------------------------------------------------------

    /**
     * Returns the subscriber's unique identifier.
     *
     * @return the subscriber's unique identifier
     */
    public long getId()
        {
        return f_id.getId();
        }

    /**
     * Returns the subscriber's notification identifier.
     *
     * @return the subscriber's notification identifier
     */
    public int getNotificationId()
        {
        return f_nNotificationId;
        }

    /**
     * Returns this subscriber's key.
     *
     * @return  this subscriber's key
     */
    public SubscriberInfo.Key getKey()
        {
        return f_key;
        }

    /**
     * Returns {@code true} if this is an anonymous subscriber,
     * or {@code false} if this subscriber is in a group.
     *
     * @return {@code true} if this is an anonymous subscriber,
     *         or {@code false} if this subscriber is in a group
     */
    public boolean isAnonymous()
        {
        return f_fAnonymous;
        }

    public long getBacklog()
        {
        return f_backlog.getBacklog();
        }

    public long getMaxBacklog()
        {
        return f_backlog.getExcessiveLimit();
        }

    public Filter<V> getFilter()
        {
        return f_filter;
        }

    public Function<V, ?> getConverter()
        {
        return f_fnConverter;
        }

    public Serializer getSerializer()
        {
        return f_serializer;
        }

    public boolean isCompleteOnEmpty()
        {
        return f_fCompleteOnEmpty;
        }

    // ----- Subscriber methods ---------------------------------------------

    @Override
    @SuppressWarnings("unchecked")
    public <T> NamedTopic<T> getNamedTopic()
        {
        return (NamedTopic<T>) f_topic;
        }

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<Element<V>> receive()
        {
        ensureActive();
        return (CompletableFuture<Element<V>>) f_queueReceiveOrders.add(ReceiveRequest.SINGLE);
        }

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<List<Element<V>>> receive(int cBatch)
        {
        ensureActive();
        return (CompletableFuture<List<Element<V>>>) f_queueReceiveOrders.add(new ReceiveRequest(true, cBatch));
        }

    public Optional<Element<V>> peek(int nChannel)
        {
        ensureActive();
        Map<Integer, Position> map = getHeads();
        Position position = map.get(nChannel);
        if (position != null)
            {
            Optional<CommittableElement> optional = m_queueValuesPrefetched.stream()
                    .filter(e -> e.getPosition().equals(position))
                    .findFirst();

            if (optional.isPresent())
                {
                return Optional.of(optional.get().getElement());
                }

            PagedPosition pagedPosition = (PagedPosition) position;
            ContentKey    key           = new ContentKey(nChannel, pagedPosition.getPage(), pagedPosition.getOffset());
            Binary        binary        = m_caches.Data.get(key.toBinary(m_caches.getPartitionCount()));
            return binary == null
                    ? Optional.empty()
                    : Optional.of(PageElement.fromBinary(binary, m_caches.getSerializer()));
            }
        return Optional.empty();
        }

    @Override
    public CompletableFuture<CommitResult> commitAsync(int nChannel, Position position)
        {
        ensureActive();
        try
            {
            if (position instanceof PagedPosition)
                {
                return commitInternal(nChannel, (PagedPosition) position, null);
                }
            else
                {
                throw new IllegalArgumentException("Invalid position type");
                }
            }
        catch (Throwable t)
            {
            CompletableFuture<CommitResult> future = new CompletableFuture<>();
            future.completeExceptionally(t);
            return future;
            }
        }

    @Override
    @SuppressWarnings({"unchecked"})
    public CompletableFuture<Map<Integer, CommitResult>> commitAsync(Map<Integer, Position> mapPositions)
        {
        ensureActive();

        Map<Integer, CommitResult>  mapResult = new HashMap<>();
        Map<Integer, PagedPosition> mapCommit = new HashMap<>();

        for (Map.Entry<Integer, Position> entry : mapPositions.entrySet())
            {
            Integer  nChannel = entry.getKey();
            Position position = entry.getValue();
            if (position instanceof PagedPosition)
                {
                mapCommit.put(nChannel, (PagedPosition) position);
                }
            else
                {
                mapResult.put(nChannel, new CommitResult(nChannel, position, CommitResultStatus.Rejected));
                }
            }

        CompletableFuture<CommitResult>[] aFuture = mapCommit.entrySet()
                .stream()
                .map(e -> commitInternal(e.getKey(), e.getValue(), mapResult))
                .toArray(CompletableFuture[]::new);

        return CompletableFuture.allOf(aFuture).handle((_void, err) -> mapResult);
        }

    @Override
    public int[] getChannels()
        {
        return getChannelSet().stream()
                .mapToInt(i -> i)
                .toArray();
        }


    /**
     * Returns the current set of channels that this {@link Subscriber} owns.
     * <p>
     * Subscribers that are part of a subscriber group own a sub-set of the available channels.
     * A subscriber in a group should normally be assigned ownership of at least one channel. In the case where there
     * are more subscribers in a group that the number of channels configured for a topic, then some
     * subscribers will obviously own zero channels.
     * Anonymous subscribers that are not part of a group are always owners all the available channels.
     *
     * @return the current set of channels that this {@link Subscriber} is the owner of, or an
     *         empty array if this subscriber has not been assigned ownership any channels
     */
    public Set<Integer> getChannelSet()
        {
        // Only have channels when connected
        if (m_nState == STATE_CONNECTED)
            {
            Gate<?> gate = f_gate;
            gate.enter(-1);
            try
                {
                return Arrays.stream(m_aChannel)
                        .filter(c -> c.m_fOwned)
                        .map(c -> c.subscriberPartitionSync.getChannelId())
                        .collect(Collectors.toSet());
                }
            finally
                {
                gate.exit();
                }
            }
        else
            {
            return Collections.emptySet();
            }
        }

    @Override
    public boolean isOwner(int nChannel)
        {
        if (m_nState == STATE_CONNECTED)
            {
            return nChannel >= 0 && m_aChannel[nChannel].m_fOwned;
            }
        return false;
        }

    @Override
    public int getChannelCount()
        {
        return  m_aChannel == null ? m_caches.getChannelCount() : m_aChannel.length;
        }

    @Override
    public FlowControl getFlowControl()
        {
        return f_backlog;
        }

    @Override
    public void onClose(Runnable action)
        {
        f_listOnCloseActions.add(action);
        }

    @Override
    public boolean isActive()
        {
        return m_nState != STATE_CLOSED && m_nState != STATE_CLOSING;
        }

    @Override
    @SuppressWarnings("unchecked")
    public Map<Integer, Position> getLastCommitted()
        {
        ensureActive();
        CompletableFuture<Map<Integer, Position>> future = (CompletableFuture<Map<Integer, Position>>)
                f_queueReceiveOrders.addFirst(new GetPositionRequest(PositionType.Committed));

        return future.join();
        }

    @Override
    @SuppressWarnings("unchecked")
    public Map<Integer, Position> getHeads()
        {
        ensureActive();
        CompletableFuture<Map<Integer, Position>> future = (CompletableFuture<Map<Integer, Position>>)
                f_queueReceiveOrders.addFirst(new GetPositionRequest(PositionType.Head));

        return future.join();
        }

    @Override
    @SuppressWarnings("unchecked")
    public Map<Integer, Position> getTails()
        {
        ensureActive();
        CompletableFuture<Map<Integer, Position>> future = (CompletableFuture<Map<Integer, Position>>)
                f_queueReceiveOrders.addFirst(new GetPositionRequest(PositionType.Tail));

        return future.join();
        }

    @Override
    @SuppressWarnings("unchecked")
    public Position seek(int nChannel, Position position)
        {
        ensureActive();
        CompletableFuture<Map<Integer, Position>> future = (CompletableFuture<Map<Integer, Position>>)
                f_queueReceiveOrders.addFirst(new SeekRequest(Collections.singletonMap(nChannel, position)));

        Map<Integer, Position> map = future.join();
        return map.get(nChannel);
        }

    @Override
    @SuppressWarnings("unchecked")
    public Map<Integer, Position> seek(Map<Integer, Position> mapPosition)
        {
        ensureActive();
        CompletableFuture<Map<Integer, Position>> future = (CompletableFuture<Map<Integer, Position>>)
                f_queueReceiveOrders.addFirst(new SeekRequest(mapPosition));

        return future.join();
        }

    @Override
    @SuppressWarnings("unchecked")
    public Position seek(int nChannel, Instant timestamp)
        {
        ensureActive();
        CompletableFuture<Map<Integer, Position>> future = (CompletableFuture<Map<Integer, Position>>)
                f_queueReceiveOrders.addFirst(new SeekRequest(timestamp, nChannel));

        return future.join().get(nChannel);
        }

    @Override
    @SuppressWarnings("unchecked")
    public Map<Integer, Position> seekToHead(int... anChannel)
        {
        ensureActiveAnOwnedChannels(anChannel);
        return ((CompletableFuture<Map<Integer, Position>>)
                f_queueReceiveOrders.addFirst(new SeekRequest(SeekType.Head, anChannel))).join();
        }

    @Override
    @SuppressWarnings("unchecked")
    public Map<Integer, Position> seekToTail(int... anChannel)
        {
        ensureActiveAnOwnedChannels(anChannel);
        return ((CompletableFuture<Map<Integer, Position>>)
                f_queueReceiveOrders.addFirst(new SeekRequest(SeekType.Tail, anChannel))).join();
        }

    @Override
    public int getRemainingMessages()
        {
        int[] anChannel = getChannels();
        return m_caches.getRemainingMessages(f_subscriberGroupId, anChannel);
        }

    @Override
    public int getRemainingMessages(int nChannel)
        {
        if (isOwner(nChannel))
            {
            return m_caches.getRemainingMessages(f_subscriberGroupId, nChannel);
            }
        return 0;
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
        if (m_nState == STATE_CLOSED)
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
        String sChannelsPolled = f_setPolledChannels.toString();
        int    cChannelsHit    = f_setHitChannels.cardinality();
        String sChannelsHit    = f_setHitChannels.toString();
        f_setPolledChannels.clear();
        f_setHitChannels.clear();

        String sState;
        switch (m_nState)
            {
            case STATE_INITIAL:
                sState = "Initial";
                break;
            case STATE_CONNECTED:
                sState = "Connected";
                break;
            case STATE_DISCONNECTED:
                sState = "Disconnected";
                break;
            case STATE_CLOSED:
                sState = "Closed";
                break;
            default:
                sState = "Unknown(" + m_nState + ")";
            }

        return getClass().getSimpleName() + "(" + "topic=" + m_caches.getTopicName() +
            ", id=" + f_id +
            ", group=" + f_subscriberGroupId +
            ", durable=" + !f_fAnonymous +
            ", state=" + sState +
            ", backlog=" + f_backlog +
            ", channelAllocation=" + (f_fAnonymous ? "[ALL]" : Arrays.toString(m_aChannelOwned)) +
            ", channelsPolled=" + sChannelsPolled + cChannelsPolled +
            ", channelsHit=" + sChannelsHit + cChannelsHit + "/" + cChannelsPolled  +
            ", batchSize=" + (cValues / (Math.max(1, cPoll - cMisses))) +
            ", hitRate=" + ((cPoll - cMisses) * 100 / Math.max(1, cPoll)) + "%" +
            ", colRate=" + (cColl * 100 / Math.max(1, cPoll)) + "%" +
            ", waitNotifyRate=" + (cWait * 100 / Math.max(1, cPoll)) + "/" + (cNotify * 100 / Math.max(1, cPoll)) + "%" +
            ')';
        }

    // ----- helper methods -------------------------------------------------

    private PagedTopicChannel[] initializeChannels(PagedTopicCaches caches, int cChannel,
            SubscriberGroupId subscriberGroupId)
        {
        return initializeChannels(caches, cChannel, subscriberGroupId, null);
        }

    private PagedTopicChannel[] initializeChannels(PagedTopicCaches caches, int cChannel,
            SubscriberGroupId subscriberGroupId, PagedTopicChannel[] existing)
        {
        if (existing != null && existing.length >= cChannel)
            {
            return existing;
            }

        // must alter channels under a lock
        try (Sentry<?> ignored = f_gate.close())
            {
            PagedTopicChannel[] aChannel = new PagedTopicChannel[cChannel];
            int                 cPart    = caches.getPartitionCount();
            for (int nChannel = 0; nChannel < cChannel; nChannel++)
                {
                if (existing != null && nChannel < existing.length)
                    {
                    aChannel[nChannel] = existing[nChannel];
                    }
                else
                    {
                    aChannel[nChannel] = new PagedTopicChannel();
                    aChannel[nChannel].subscriberPartitionSync = Subscription.createSyncKey(subscriberGroupId, nChannel, cPart);
                    }
                }

            return aChannel;
            }
        }

    /**
     * Returns the specified channel.
     *
     * @param nChannel  the channel to return
     *
     * @return the specified channel
     */
    public Channel getChannel(int nChannel)
        {
        return nChannel < m_aChannel.length ? m_aChannel[nChannel] : new Channel.EmptyChannel(nChannel);
        }

    /**
     * Return the number of completed receive requests.
     *
     * @return the number of completed receive requests
     */
    public long getReceived()
        {
        return m_cReceived.getCount();
        }

    /**
     * Return the number of receive requests completed empty.
     * <p>
     * This wil only apply to subscribers using the {@link com.tangosol.net.topic.Subscriber.CompleteOnEmpty}
     * option.
     *
     * @return the number of receive requests completed empty
     */
    public long getReceivedEmpty()
        {
        return m_cReceivedEmpty.getCount();
        }

    /**
     * Return the number of exceptionally completed receive requests.
     *
     * @return the number of exceptionally completed receive requests
     */
    public long getReceivedError()
        {
        return m_cReceivedError.getCount();
        }

    /**
     * Return the number of disconnections.
     *
     * @return the number of disconnections
     */
    public long getDisconnectCount()
        {
        return m_cDisconnect.getCount();
        }

    /**
     * Returns the auto-reconnect task execution count.
     *
     * @return the auto-reconnect task execution count
     */
    public int getAutoReconnectTaskCount()
        {
        return f_taskReconnect.getExecutionCount();
        }

    /**
     * Returns the number of polls of the topic for messages.
     * <p>
     * This is typically larger than the number of messages received due to polling empty pages,
     * empty topics, etc.
     *
     * @return the number of polls of the topic for messages
     */
    public long getPolls()
        {
        return m_cPolls;
        }

    /**
     * Returns the number of message elements received.
     *
     * @return the number of message elements received
     */
    public long getElementsPolled()
        {
        return m_cValues;
        }

    /**
     * Returns the number of times the subscriber has waited on empty channels.
     *
     * @return the number of times the subscriber has waited on empty channels
     */
    public long getWaitCount()
        {
        return m_cWait;
        }

    /**
     * Returns the number of times an empty channel has been polled.
     *
     * @return the number of times an empty channel has been polled
     */
    public long getMisses()
        {
        return m_cMisses;
        }

    /**
     * Returns the number of times an empty channel has been polled
     * due to a previous subscriber changing polling the channel and
     * hence this subscribers head being out of date.
     *
     * @return the number of times an empty channel has been polled
     *         due to a previous subscriber changing polling the
     *         channel and hence this subscribers head being out of
     *         date.
     */
    public long getMissCollisions()
        {
        return m_cMissCollisions;
        }

    /**
     * Returns the number of notification received that a channel has been populated.
     *
     * @return the number of notification received that a channel has been populated
     */
    public long getNotify()
        {
        return m_cNotify;
        }

    /**
     * Returns {@code true} if the specified {@link Position} has been committed in the specified
     * channel.
     * <p>
     * If the channel parameter is not a channel owned by this subscriber then even if the result
     * returned is {@code false}, the position could since have been committed by the owning subscriber.
     *
     * @param nChannel  the channel
     * @param position  the position within the channel to check
     *
     * @return {@code true} if the specified {@link Position} has been committed in the
     *         specified channel, or {@code false} if the position is not committed or
     *         this subscriber
     */
    public boolean isCommitted(int nChannel, Position position)
        {
        ensureActive();
        return m_caches.isCommitted(f_subscriberGroupId, nChannel, position);
        }

    /**
     * Initialise the subscriber.
     *
     * @throws InterruptedException if the wait for channel allocation is interrupted
     * @throws ExecutionException if the wait for channel allocation fails
     */
    protected synchronized void initialise() throws InterruptedException, ExecutionException, TimeoutException
        {
        ensureActive();
        if (m_nState == STATE_CONNECTED)
            {
            return;
            }

        // We must do initialisation under the gate lock
        try (Sentry<?> ignored = f_gate.close())
            {
            boolean fReconnect = m_nState == STATE_DISCONNECTED;

            if (fReconnect)
                {
                Logger.finest("Reconnecting subscriber " + this);
                }

            m_caches.ensureConnected();

            boolean fDisconnected = m_nState == STATE_DISCONNECTED;
            long[]  alHead        = m_caches.initializeSubscription(f_subscriberGroupId, f_id, f_filter, f_fnConverter,
                                                                    fReconnect, false, fDisconnected);
            int     cChannel      = alHead.length;

            if (cChannel > m_aChannel.length)
                {
                // this subscriber has fewer channels than the server so needs to be resized
                m_aChannel = initializeChannels(m_caches, cChannel, f_subscriberGroupId, m_aChannel);
                }

            for (int nChannel = 0; nChannel < cChannel; ++nChannel)
                {
                PagedTopicChannel channel = m_aChannel[nChannel];
                channel.m_lHead  = alHead[nChannel];
                channel.m_nNext  = -1; // unknown page position to start
                channel.m_fEmpty = false; // even if we could infer emptiness here it is unsafe unless we've registered for events
                }

            if (f_fAnonymous)
                {
                // anonymous so we own all channels
                List<Integer> listChannel = new ArrayList<>(cChannel);
                for (int i = 0; i < cChannel; i++)
                    {
                    listChannel.add(i);
                    }
                updateChannelOwnership(listChannel, false);
                }
            else
                {
                CompletableFuture<Subscription> future = m_caches.Subscriptions.async().get(m_aChannel[0].subscriberPartitionSync);
                Subscription subscription = null;
                try
                    {
                    // we use a timout here because a never ending get can cause a deadlock during fail-over scenarios
                    subscription = future.get(INIT_TIMEOUT_SECS, TimeUnit.SECONDS);
                    }
                catch (TimeoutException e)
                    {
                    future.cancel(true);
                    throw e;
                    }
                List<Integer> list = Arrays.stream(subscription.getChannels(f_id, cChannel)).boxed().collect(Collectors.toList());
                updateChannelOwnership(list, false);
                }

            switchChannel();
            heartbeat();
            registerNotificationListener();

            setState(STATE_CONNECTED);
            }
        }

    /**
     * Trigger a receive loop.
     *
     * @param cBatch  the size of the batch of requests to process.
     */
    private void trigger(int cBatch)
        {
        receiveInternal(f_queueReceiveOrders, cBatch);
        }

    /**
     * Schedule another receive.
     *
     * @param queueRequest  the batching queue handling the requests
     * @param cBatch        the number of receives to schedule in this batch
     */
    private void receiveInternal(BatchingOperationsQueue<Request, ?> queueRequest, Integer cBatch)
        {
        if (isActive())
            {
            ensureConnected();
            }

        if (!queueRequest.isBatchComplete() || queueRequest.fillCurrentBatch(cBatch))
            {
            if (queueRequest.isBatchComplete())
                {
                return;
                }

            heartbeat();
            complete(queueRequest);

            int nChannel = ensureOwnedChannel();
            if (!queueRequest.isBatchComplete() && nChannel >= 0)
                {
                // we have emptied the pre-fetch queue but the batch has more in it, so fetch more
                PagedTopicChannel channel  = m_aChannel[nChannel];
                long              lHead    = channel.m_lHead;
                long              lVersion = channel.m_lVersion;

                int nPart = ((PartitionedService) m_caches.Subscriptions.getCacheService())
                                    .getKeyPartitioningStrategy()
                                    .getKeyPartition(new Page.Key(nChannel, lHead));

                InvocableMapHelper.invokeAsync(m_caches.Subscriptions,
                                               new Subscription.Key(nPart, nChannel, f_subscriberGroupId), m_caches.getUnitOfOrder(nPart),
                                               new PollProcessor(lHead, Integer.MAX_VALUE, f_nNotificationId, f_id),
                                               (result, e) -> onReceiveResult(channel, lVersion, lHead, result, e))
                                  .handleAsync((r, e) ->
                                      {
                                      if (e != null)
                                          {
                                          Logger.err(e);
                                          return null;
                                          }
                                      if (!m_queueValuesPrefetched.isEmpty())
                                          {
                                          complete(queueRequest);
                                          }
                                      trigger(cBatch);
                                      return null;
                                      }, f_daemon::executeTask);
                }
            else
                {
                if (m_queueValuesPrefetched.isEmpty() && nChannel < 0)
                    {
                    // we have emptied the pre-fetch queue or the topic is empty
                    // we need to switch channel under the gate lock as we might have a concurrent notification
                    // that a channel is no longer empty (which also happens under the lock)
                    Gate<?> gate = f_gate;
                    // Wait to enter the gate
                    gate.enter(-1);
                    try
                        {
                        // now we are in the gate lock, re-try switching channel as we might have had a notification
                        if (switchChannel())
                            {
                            // we have a non-empty channel so go be round and process the "receive" requests again
                            receiveInternal(queueRequest, cBatch);
                            }
                        else
                            {
                            // the topic is empty
                            if (f_fCompleteOnEmpty)
                                {
                                // the complete-on-empty flag is set, so complete any outstanding requests
                                m_queueValuesPrefetched.add(getEmptyElement());
                                complete(queueRequest);
                                }
                            queueRequest.resetTrigger();
                            }
                        }
                    finally
                        {
                        // and finally exit from the gate
                        gate.exit();
                        }
                    }
                else
                    {
                    // go around again, more requests have come in
                    receiveInternal(queueRequest, cBatch);
                    }
                }
            }
        }

    /**
     * Complete as many outstanding requests as possible from the contents of the pre-fetch queue.
     *
     * @param queueRequest  the queue of requests to complete
     */
    @SuppressWarnings("unchecked")
    private void complete(BatchingOperationsQueue<Request, ?> queueRequest)
        {
        LinkedList<Request> queueBatch = queueRequest.getCurrentBatchValues();

        Queue<CommittableElement> queueValuesPrefetched = m_queueValuesPrefetched;

        Request firstRequest = queueBatch.peek();
        while (firstRequest instanceof FunctionalRequest)
            {
            ((FunctionalRequest) queueBatch.poll()).execute(this, queueRequest);
            firstRequest = queueBatch.peek();
            }

        int cValues  = 0;
        int cRequest = queueBatch.size();

        if (isActive() && !queueValuesPrefetched.isEmpty())
            {
            LongArray          aValues = new SparseArray<>();
            CommittableElement element = queueValuesPrefetched.peek();

            if (element != null && element.isEmpty())
                {
                // we're empty, remove the empty/null element from the pre-fetch queue
                queueValuesPrefetched.poll();
                while (cValues < cRequest)
                    {
                    Request request = queueBatch.get(cValues);
                    if (request instanceof ReceiveRequest)
                        {
                        ReceiveRequest receiveRequest = (ReceiveRequest) request;
                        if (!receiveRequest.isBatch())
                            {
                            aValues.set(cValues, null);
                            }
                        else
                            {
                            aValues.set(cValues, Collections.emptyList());
                            }
                        cValues++;
                        }
                    }
                }
            else
                {
                while (m_nState == STATE_CONNECTED && cValues < cRequest && !queueValuesPrefetched.isEmpty())
                    {
                    Request request = queueBatch.get(cValues);
                    if (request instanceof ReceiveRequest)
                        {
                        ReceiveRequest receiveRequest = (ReceiveRequest) request;
                        if (receiveRequest.isBatch())
                            {
                            int cElement = receiveRequest.getElementCount();
                            List<CommittableElement> list = new ArrayList<>();
                            for (int i = 0; i < cElement && !queueValuesPrefetched.isEmpty(); i++)
                                {
                                element = queueValuesPrefetched.poll();
                                // ensure we still own the channel
                                if (element != null && !element.isEmpty() && isOwner(element.getChannel()))
                                    {
                                    list.add(element);
                                    }
                                }
                            aValues.set(cValues++, list);
                            }
                        else
                            {
                            element = queueValuesPrefetched.poll();
                            // ensure we still own the channel
                            if (element != null && !element.isEmpty() && isOwner(element.getChannel()))
                                {
                                aValues.set(cValues++, element);
                                }
                            }
                        }
                    }
                }
            queueRequest.completeElements(cValues, aValues, this::onReceiveError, this::onReceiveComplete);
            }
        }

    private Throwable onReceiveError(Throwable err, Object o)
        {
        m_cReceived.mark();
        m_cReceivedError.mark();
        return new TopicException(err);
        }

    @SuppressWarnings("unchecked")
    private void onReceiveComplete(Object o)
        {
        if (o == null)
            {
            m_cReceived.mark();
            m_cReceivedEmpty.mark();
            return;
            }

        if (o instanceof Element)
            {
            m_cReceived.mark();
            onReceiveComplete((Element<?>) o);
            }
        else if (o instanceof Collection)
            {
            m_cReceived.mark();
            for (Element<?> e : ((Collection<Element<?>>) o))
                {
                onReceiveComplete(e);
                }
            }
        }

    private void onReceiveComplete(Element<?> element)
        {
        int c = element.getChannel();
        if (m_aChannel[c].m_fOwned)
            {
            m_aChannel[c].m_lastReceived = (PagedPosition) element.getPosition();
            }
        }

    /**
     * Asynchronously commit the specified channel and position.
     *
     * @param nChannel   the channel to commit
     * @param position   the position within the channel to commit
     * @param mapResult  the {@link Map} to add the commit result to
     *
     * @return a {@link CompletableFuture} that completes when the commit request has completed
     */
    private CompletableFuture<CommitResult> commitInternal(int nChannel, PagedPosition position, Map<Integer, CommitResult>  mapResult)
        {
        try
            {
            long lPage = position.getPage();
            int  cPart = m_caches.getPartitionCount();
            int  nPart = ((PartitionedService) m_caches.Subscriptions.getCacheService())
                                .getKeyPartitioningStrategy().getKeyPartition(new Page.Key(nChannel, lPage));

            scheduleHeadIncrement(m_aChannel[nChannel], lPage - 1).join();

            Set<Subscription.Key> setKeys = m_aChannel[nChannel].ensureSubscriptionKeys(cPart, f_subscriberGroupId);

            // We must execute against all Subscription keys for the channel and subscriber group
            CompletableFuture<Map<Subscription.Key, CommitResult>> future
                    = InvocableMapHelper.invokeAllAsync(m_caches.Subscriptions,
                                                        setKeys, m_caches.getUnitOfOrder(nPart),
                                                        new CommitProcessor(position, f_id));

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
                                result = new CommitResult(nChannel, position, CommitResultStatus.Rejected, err);
                                }

                            if (mapResult != null)
                                {
                                mapResult.put(nChannel, result);
                                }

                            m_aChannel[nChannel].m_lastCommit = position;
                            return result;
                            });
            }
        catch (Throwable thrown)
            {
            CompletableFuture<CommitResult> future = new CompletableFuture<>();
            future.completeExceptionally(thrown);
            return future;
            }
        }

    private Position seekChannel(int nChannel, PagedPosition pagedPosition)
        {
        SeekProcessor.Result result       = seekInternal(nChannel, pagedPosition);
        Position             seekPosition = updateSeekedChannel(nChannel, result);
        return seekPosition == null ? PagedPosition.NULL_POSITION : seekPosition;
        }

    private Position updateSeekedChannel(int nChannel, SeekProcessor.Result result)
        {
        PagedPosition positionHead = result.getHead();
        PagedPosition seekPosition = result.getSeekPosition();

        if (positionHead != null)
            {
            m_aChannel[nChannel].m_lHead = positionHead.getPage();
            m_aChannel[nChannel].m_nNext = positionHead.getOffset();
            }

        m_queueValuesPrefetched.removeIf(e -> e.getChannel() == nChannel);
        return seekPosition;
        }

    /**
     * Move the head of the specified channel to a new position.
     *
     * @param nChannel   the channel to commit
     * @param position   the position within the channel to commit
     */
    private SeekProcessor.Result seekInternal(int nChannel, PagedPosition position)
        {
        // We must execute against all Subscription keys for the same channel and subscriber group
        Set<Subscription.Key> setKeys = m_aChannel[nChannel]
                .ensureSubscriptionKeys(m_caches.getPartitionCount(), f_subscriberGroupId);

        Map<Subscription.Key, SeekProcessor.Result> mapResult
                = m_caches.Subscriptions.invokeAll(setKeys, new SeekProcessor(position, f_id));

        // the new head is the lowest non-null returned position
        return mapResult.values()
                .stream()
                .filter(Objects::nonNull)
                .sorted()
                .findFirst()
                .orElse(null);
        }

    private Map<Integer, Position> seekInternal(SeekRequest request)
        {
        SeekType type      = request.getType();
        int[]    anChannel = request.getChannels();

        if (anChannel == null || anChannel.length == 0)
            {
            return new HashMap<>();
            }

        ensureActiveAnOwnedChannels(anChannel);

        switch (type)
            {
            case Head:
                ValueExtractor<Page, Integer> extractorChannel = new ReflectionExtractor<>("getChannelId", new Object[0], ReflectionExtractor.KEY);
                ValueExtractor<Page, Long>    extractorPage    = new ReflectionExtractor<>("getPageId", new Object[0], ReflectionExtractor.KEY);
                Map<Integer, Long>            mapHeads         = m_caches.Pages.aggregate(GroupAggregator.createInstance(extractorChannel, new LongMin<>(extractorPage)));

                Map<Integer, Position> mapSeek = new HashMap<>();
                for (int nChannel : anChannel)
                    {
                    mapSeek.put(nChannel, new PagedPosition(mapHeads.get(nChannel), -1));
                    }

                return seekInternal(mapSeek);
            case Tail:
                Map<Integer, Position> mapTails = m_caches.getTails();
                return seekInternal(filterForChannel(mapTails, anChannel));
            case Position:
                return seekInternal(request.getPositions());
            case Instant:
                Map<Integer, Position> mapPosition = new HashMap<>();
                Instant                instant     = request.getInstant();

                for (int nChannel : request.getChannels())
                    {
                    Position position = seekInternal(nChannel, instant);
                    mapPosition.put(nChannel, position);
                    }
                return mapPosition;
            default:
                throw new IllegalArgumentException("Invalid SeekType " + type);
            }
        }

    private Map<Integer, Position> seekInternal(Map<Integer, Position> mapPosition)
        {
        ensureActive();

        List<Integer> listUnallocated = mapPosition.keySet().stream()
                .filter(c -> !isOwner(c))
                .collect(Collectors.toList());

        if (listUnallocated.size() > 0)
            {
            throw new IllegalStateException("Subscriber is not allocated channels " + listUnallocated);
            }

        try
            {
            // pause receives while we seek
            f_queueReceiveOrders.pause();

            Map<Integer, PagedPosition> mapSeek = new HashMap<>();
            for (Map.Entry<Integer, Position> entry : mapPosition.entrySet())
                {
                Integer  nChannel = entry.getKey();
                Position position = entry.getValue();
                if (position instanceof PagedPosition)
                    {
                    mapSeek.put(nChannel, (PagedPosition) position);
                    }
                else
                    {
                    throw new IllegalArgumentException("Invalid position type for channel " + nChannel);
                    }
                }

            Map<Integer, Position> mapResult    = new HashMap<>();
            for (Map.Entry<Integer, PagedPosition> entry : mapSeek.entrySet())
                {
                int                  nChannel     = entry.getKey();
                SeekProcessor.Result result       = seekInternal(nChannel, entry.getValue());
                Position             seekPosition = updateSeekedChannel(nChannel, result);
                mapResult.put(nChannel, seekPosition);
                }
            return mapResult;
            }
        finally
            {
            // resume receives from the new positions
            f_queueReceiveOrders.resetTrigger();
            }
        }

    private Position seekInternal(int nChannel, Instant timestamp)
        {
        if (!isOwner(nChannel))
            {
            throw new IllegalStateException("Subscriber is not allocated channel " + nChannel);
            }

        Objects.requireNonNull(timestamp);

        try
            {
            // pause receives while we seek
            f_queueReceiveOrders.pause();

            ValueExtractor<Object, Integer>  extractorChannel   = Page.ElementExtractor.chained(Element::getChannel);
            ValueExtractor<Object, Instant>  extractorTimestamp = Page.ElementExtractor.chained(Element::getTimestamp);
            ValueExtractor<Object, Position> extractorPosition  = Page.ElementExtractor.chained(Element::getPosition);

            Binary bin = m_caches.Data.aggregate(
                    Filters.equal(extractorChannel, nChannel).and(Filters.greater(extractorTimestamp, timestamp)),
                    new ComparableMin<>(extractorPosition));

            @SuppressWarnings("unchecked")
            PagedPosition position = (PagedPosition) m_caches.getService().getBackingMapManager()
                    .getContext().getValueFromInternalConverter().convert(bin);

            if (position == null)
                {
                // nothing found greater than the timestamp so either the topic is empty
                // or all elements are earlier, in either case seek to the tail
                return seekToTail(nChannel).get(nChannel);
                }

            PagedPosition positionSeek;
            int           nOffset = position.getOffset();

            if (nOffset == 0)
                {
                // The position found is the head of a page, so we actually want to seek to the previous element
                // We don;t know the tail of that page, so we can use Integer.MAX_VALUE
                positionSeek = new PagedPosition(position.getPage() - 1, Integer.MAX_VALUE);
                }
            else
                {
                // we are not at the head of a page so seek to the page and previous offset
                positionSeek = new PagedPosition(position.getPage(), nOffset - 1);
                }
            return seekChannel(nChannel, positionSeek);
            }
        finally
            {
            // resume receives from the new position
            f_queueReceiveOrders.resetTrigger();
            }
        }

    private void ensureActiveAnOwnedChannels(int... anChannel)
        {
        ensureActive();

        if (anChannel != null && anChannel.length > 0)
            {
            List<Integer> listUnallocated = Arrays.stream(anChannel)
                    .filter(c -> !isOwner(c))
                    .boxed()
                    .collect(Collectors.toList());

            if (listUnallocated.size() != 0)
                {
                throw new IllegalArgumentException("One or more channels are not allocated to this subscriber " + listUnallocated);
                }
            }
        }

    private Map<Integer, Position> filterForChannel(Map<Integer, Position> mapPosition, int... anChannel)
        {
        Map<Integer, Position> mapChannel = new HashMap<>();
        for (int nChannel : anChannel)
            {
            Position position = mapPosition.get(nChannel);
            if (position != null)
                {
                mapChannel.put(nChannel, position);
                }
            }
        return mapChannel;
        }

    private Map<Integer, Position> getHeadsInternal()
        {
        ensureActive();

        Map<Integer, Position> mapHeads  = new HashMap<>();
        int[]                  anChannel = getChannels();

        for (int nChannel : anChannel)
            {
            mapHeads.put(nChannel, m_aChannel[nChannel].getHead());
            }

        for (CommittableElement element : m_queueValuesPrefetched)
            {
            int nChannel = element.getChannel();
            if (mapHeads.containsKey(nChannel))
                {
                Position      positionCurrent = mapHeads.get(nChannel);
                PagedPosition position        = (PagedPosition) element.getPosition();

                if (nChannel != CommittableElement.EMPTY && position != null
                        && position.getPage() != Page.EMPTY
                        && (positionCurrent == null || positionCurrent.compareTo(position) > 0))
                    {
                    mapHeads.put(nChannel, position);
                    }
                }
            }

        return mapHeads;
        }

    private Map<Integer, Position> getTailsInternal()
        {
        Map<Integer, Position> map       = m_caches.getTails();
        Map<Integer, Position> mapTails  = new HashMap<>();
        int[]                  anChannel = getChannels();
        for (int nChannel : anChannel)
            {
            mapTails.put(nChannel, map.getOrDefault(nChannel, m_aChannel[nChannel].getHead()));
            }
        return mapTails;
        }

    private Map<Integer, Position> getLastCommittedInternal()
        {
        ensureActive();
        Map<Integer, Position> mapCommit  = m_caches.getLastCommitted(f_subscriberGroupId);
        int[]                  anChannels = m_aChannelOwned;
        Map<Integer, Position> mapResult  = new HashMap<>();
        for (int nChannel : anChannels)
            {
            mapResult.put(nChannel, mapCommit.getOrDefault(nChannel, PagedPosition.NULL_POSITION));
            }
        return mapResult;
        }

    /**
     * Ensure that the subscriber is active.
     *
     * @throws IllegalStateException if not active
     */
    protected void ensureActive()
        {
        if (!isActive())
            {
            throw new IllegalStateException("The subscriber is not active");
            }
        }

    /**
     * Returns the state of the subscriber.
     *
     * @return the state of the subscriber
     */
    public int getState()
        {
        return m_nState;
        }

    /**
     * Returns the state of the subscriber.
     *
     * @return the state of the subscriber
     */
    public String getStateName()
        {
        return STATES[m_nState];
        }

    /**
     * Set the state of the subscriber.
     *
     * @param nState  the state of the subscriber
     */
    protected void setState(int nState)
        {
        m_nState = nState;
        }

    /**
     * Ensure that the subscriber is connected.
     */
    public void connect()
        {
        ensureActive();
        ensureConnected();;
        }

    /**
     * Ensure that the subscriber is connected.
     */
    protected void ensureConnected()
        {
        if (isActive() && m_nState != STATE_CONNECTED)
            {
            synchronized (this)
                {
                ensureActive();
                PagedTopicDependencies dependencies = m_caches.getDependencies();
                long                    retry        = dependencies.getReconnectRetryMillis();
                long                    now          = System.currentTimeMillis();
                long                    timeout      = now + dependencies.getReconnectTimeoutMillis();
                Throwable               error        = null;

                if (m_nState != STATE_CONNECTED)
                    {
                    while (now < timeout)
                        {
                        if (!isActive())
                            {
                            break;
                            }
                        try
                            {
                            if (m_caches.getService().isSuspended())
                                {
                                break;
                                }
                            m_caches.ensureConnected();
                            initialise();
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
                            Logger.info("Failed to reconnect subscriber, will retry in "
                                    + retry + " millis " + this + " due to " + error.getMessage());
                            Logger.finest(error);
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
                    }

                if (error != null)
                    {
                    throw Exceptions.ensureRuntimeException(error);
                    }
                }
            }
        }

    /**
     * Returns {@code true} if this subscriber is disconnected from the topic.
     *
     * @return {@code true} if this subscriber is disconnected from the topic
     */
    public boolean isDisconnected()
        {
        return m_nState == STATE_DISCONNECTED;
        }

    /**
     * Returns {@code true} if this subscriber is initialising.
     *
     * @return {@code true} if this subscriber is initialising
     */
    public boolean isInitialising()
        {
        return m_nState == STATE_INITIAL;
        }

    /**
     * Disconnect this subscriber.
     * <p>
     * This will cause the subscriber to re-initialize itself on re-connection.
     */
    public void disconnect()
        {
        if (isActive() && m_nState != STATE_DISCONNECTED)
            {
            setState(STATE_DISCONNECTED);
            m_cDisconnect.mark();
            if (!f_fAnonymous)
                {
                // reset the channel allocation for non-anonymous subscribers, channels
                // will be reallocated when (or if) reconnection occurs
                m_listenerChannelAllocation.reset();
                }
            // clear out the pre-fetch queue because we have no idea what we'll get on reconnection
            m_queueValuesPrefetched.clear();

            PagedTopicDependencies  dependencies = m_caches.getDependencies();
            long                    cWaitMillis  = dependencies.getReconnectWaitMillis();
            Logger.finest("Disconnected Subscriber " + this);
            f_daemon.scheduleTask(f_taskReconnect, TimeHelper.getSafeTimeMillis() + cWaitMillis);
            }
        }

    /**
     * Returns this subscriber's group identifier.
     *
     * @return this subscriber's group identifier
     */
    public SubscriberGroupId getSubscriberGroupId()
        {
        return f_subscriberGroupId;
        }

    /**
     * Notification that one or more channels that were empty now have content.
     *
     * @param nChannel  the non-empty channels
     */
    public void notifyChannel(int nChannel)
        {
        onChannelPopulatedNotification(new int[]{nChannel});
        }

    /**
     * Asynchronously handle a channel populated notification event.
     *
     * @param evt  the channel populated notification event
     */
    private void onChannelPopulatedNotification(MapEvent<?, ?> evt)
        {
        f_daemon.executeTask(() -> onChannelPopulatedNotification((int[]) evt.getOldValue()));
        }

    /**
     * Notification that one or more channels that were empty now have content.
     *
     * @param anChannel  the non-empty channels
     */
    protected void onChannelPopulatedNotification(int[] anChannel)
        {
        if (anChannel == null || anChannel.length == 0)
            {
            // we have no channel allocation, so we're still effectively empty
            return;
            }

        boolean fWasEmpty;

        // Channel operations are done under a lock
        try (Sentry<?> ignored = f_gate.close())
            {
            if (m_aChannel == null || !isActive())
                {
                // not initialised yet or no longer active
                return;
                }

            ++m_cNotify;

            int nChannelCurrent  = m_nChannel;
            fWasEmpty = nChannelCurrent < 0 || m_aChannel[nChannelCurrent].m_fEmpty;
            for (int nChannel : anChannel)
                {
                m_aChannel[nChannel].onChannelPopulatedNotification();
                }
            }

        if (fWasEmpty)
            {
            // we were on the empty channel so switch and trigger a request loop
            switchChannel();
            f_queueReceiveOrders.triggerOperations();
            }
        // else; we weren't waiting so things are already scheduled
        }

    /**
     * Set the specified channel as empty.
     *
     * @param nChannel  the channel to mark as empty
     * @param lVersion  the version to use as the CAS to mark the channel
     */
    private void onChannelEmpty(int nChannel, long lVersion)
        {
        if (isDisconnected())
            {
            // we're disconnected, nothing to do.
            return;
            }

        // Channel operations are done under a lock
        Gate<?> gate = f_gate;
        // Wait to enter the gate
        gate.enter(-1);
        try
            {
            if (m_aChannel == null || !isActive() || isDisconnected())
                {
                // not initialised yet or no longer active
                return;
                }

            m_aChannel[nChannel].setEmpty(lVersion);
            }
        finally
            {
            // and finally exit from the gate
            gate.exit();
            }
        }

    /**
     * Compare-and-increment the remote head pointer.
     *
     * @param lHeadAssumed  the assumed old value, increment will only occur if the actual head matches this value
     *
     * @return a {@link CompletableFuture} that completes with the new head
     */
    protected CompletableFuture<Long> scheduleHeadIncrement(PagedTopicChannel channel, long lHeadAssumed)
        {
        if (isActive())
            {
            // update the globally visible head page
            return InvocableMapHelper.invokeAsync(m_caches.Subscriptions, channel.subscriberPartitionSync,
                                                  m_caches.getUnitOfOrder(channel.subscriberPartitionSync.getPartitionId()),
                                                  new HeadAdvancer(lHeadAssumed + 1),
                                                  (lPriorHeadRemote, e2) ->
                {
                if (lPriorHeadRemote < lHeadAssumed + 1)
                    {
                    // our CAS succeeded, we'd already updated our local head before attempting it,
                    // but we do get to clear any contention since the former winner's CAS will fail
                    channel.m_fContended = false;
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
                        if (!channel.m_fContended)
                            {
                            channel.m_fContended = true;
                            f_listChannelsContended.add(channel);
                            }

                        m_cHitsSinceLastCollision = 0;
                        }
                    // else; we knew we were contended, don't doubly backoff

                    if (lPriorHeadRemote > channel.m_lHead)
                        {
                        // only update if we haven't locally moved ahead; yes it is possible that we lost the
                        // CAS but have already advanced our head simply through brute force polling
                        channel.m_lHead = lPriorHeadRemote;
                        channel.m_nNext = -1; // unknown page position
                        }
                    }
                });
            }
        return CompletableFuture.completedFuture(-1L);
        }

    /**
     * If this is not an anonymous subscriber send a heartbeat to the server.
     */
    public void heartbeat()
        {
        if (!f_fAnonymous)
            {
            // we're not anonymous so send a poll heartbeat
            m_caches.Subscribers.async().invoke(f_key, new SubscriberHeartbeatProcessor());
            }
        }

    private void updateChannelOwnership(List<Integer> listChannels, boolean fLost)
        {
        if (!isActive())
            {
            return;
            }

        Collections.sort(listChannels);
        int[] aChannel    = listChannels.stream().mapToInt(i -> i).toArray();
        int   nMaxChannel = listChannels.stream().mapToInt(i -> i).max().orElse(getChannelCount() - 1);

        // channel ownership change must be done under a lock
        try (Sentry<?> ignored = f_gate.close())
            {
            if (!isActive())
                {
                return;
                }

            if (nMaxChannel >= m_aChannel.length)
                {
                int cChannel = nMaxChannel + 1;
                // this subscriber has fewer channels than the server so needs to be resized
                m_aChannel = initializeChannels(m_caches, cChannel, f_subscriberGroupId, m_aChannel);
                }

            if (!Arrays.equals(m_aChannelOwned, aChannel))
                {
                Set<Integer> setNew     = new HashSet<>(listChannels);
                Set<Integer> setRevoked = new HashSet<>();
                if (m_aChannelOwned != null && m_aChannelOwned.length > 0)
                    {
                    for (int nChannel : m_aChannelOwned)
                        {
                        setNew.remove(nChannel);
                        setRevoked.add(nChannel);
                        }
                    listChannels.forEach(setRevoked::remove);
                    }
                setRevoked = Collections.unmodifiableSet(setRevoked);

                Set<Integer> setAdded = new HashSet<>(listChannels);
                setAdded = Collections.unmodifiableSet(setAdded);

                Logger.finest(String.format("Subscriber %d channel allocation changed, assigned=%s revoked=%s",
                                          f_id.getId(), setAdded, setRevoked));

                m_aChannelOwned = aChannel;

                if (!f_fAnonymous)
                    {
                    // reset revoked channel heads - we'll re-sync if they are reallocated
                    setRevoked.forEach(c ->
                        {
                        PagedTopicChannel channel = m_aChannel[c];
                        channel.m_fContended = false;
                        channel.m_fOwned     = false;
                        channel.m_lastCommit = null;
                        channel.setPopulated();
                        });

                    // if we're initializing and not anonymous, we do not own any channels,
                    // we'll update with the allocated ownership
                    if (m_nState == STATE_INITIAL)
                        {
                        for (PagedTopicChannel channel : m_aChannel)
                            {
                            channel.m_fOwned = false;
                            channel.m_lastCommit = null;
                            }
                        }

                    // re-sync added channel heads and reset empty flag
                    setNew.forEach(c ->
                        {
                        PagedTopicChannel channel = m_aChannel[c];
                        channel.m_fContended = false;
                        channel.m_fOwned     = true;
                        channel.setPopulated();
                        });
                    }

                if (m_aChannelOwnershipListener.length > 0)
                    {
                    for (ChannelOwnershipListener listener : m_aChannelOwnershipListener)
                        {
                        if (!setRevoked.isEmpty())
                            {
                            try
                                {
                                if (fLost)
                                    {
                                    listener.onChannelsLost(setRevoked);
                                    }
                                else
                                    {
                                    listener.onChannelsRevoked(setRevoked);
                                    }
                                }
                            catch (Throwable t)
                                {
                                Logger.err(t);
                                }
                            }
                        if (!setAdded.isEmpty())
                            {
                            try
                                {
                                listener.onChannelsAssigned(setAdded);
                                }
                            catch (Throwable t)
                                {
                                Logger.err(t);
                                }
                            }
                        }

                    }

                onChannelPopulatedNotification(m_aChannelOwned);
                }
            }
        }

    /**
     * Return the current channel to poll, ensuring this subscriber owns the channel.
     *
     * @return the current channel to poll
     */
    protected int ensureOwnedChannel()
        {
        if (m_nChannel >= 0 && m_aChannel[m_nChannel].m_fOwned)
            {
            return m_nChannel;
            }
        switchChannel();
        return m_nChannel;
        }

    /**
     * Switch to the next available channel.
     *
     * @return {@code true} if a potentially non-empty channel has been found
     *         or {@code false} iff all channels are known to be empty
     */
    protected boolean switchChannel()
        {
        if (m_aChannel == null || !isActive() || isDisconnected())
            {
            // disconnected or no longer active
            return false;
            }

        // channel access must be done under a lock to ensure channel
        // state does not change while switching
        Gate<?> gate = f_gate;
        // Wait to enter the gate
        gate.enter(-1);
        try
            {
if (Config.getBoolean("coherence.subscriber.debug"))
    {
    Logger.info("Subscriber " + f_id + " switching channel current=" + m_nChannel + " owned=" + Arrays.toString(m_aChannelOwned));
    }

            if (m_aChannelOwned.length == 0)
                {
                m_nChannel = -1;
                return false;
                }

            if (m_aChannelOwned.length == 1)
                {
                int nChannel = m_aChannelOwned[0];
                // only one allocated channel
                if (m_aChannel[nChannel].m_fEmpty)
                    {
//if (Config.getBoolean("coherence.subscriber.debug"))
//    {
//    Logger.info("Subscriber " + f_id + " switching channel - single owned channel is empty");
//    }
                    // our single channel is empty
                    m_nChannel = -1;
                    return false;
                    }
                else
                    {
                    // our single channel is not empty, switch to it
//if (Config.getBoolean("coherence.subscriber.debug"))
//    {
//    Logger.info("Subscriber " + f_id + " switching channel - single owned channel is not empty");
//    }
                    m_nChannel = nChannel;
                    return true;
                    }
                }

            int nChannelStart = m_nChannel;
            int nChannel      = nChannelStart;
            int i        = 0;
            for (; i < m_aChannelOwned.length; i++)
                {
                if (m_aChannelOwned[i] > nChannel)
                    {
                    break;
                    }
                if (m_aChannelOwned[i] == nChannel)
                    {
                    i++;
                    break;
                    }
                }

            if (i >= m_aChannelOwned.length)
                {
                i = 0;
                }

            // "i" is now the next channel index
            nChannel = m_aChannelOwned[i];

            // now ensure the channel is not empty
            int cTried = 0;
            while (nChannel != nChannelStart && cTried < m_aChannel.length && (!m_aChannel[nChannel].m_fOwned || m_aChannel[nChannel].m_fEmpty))
                {
                cTried++;
                nChannel++;
                if (nChannel == m_aChannel.length)
                    {
                    nChannel = 0;
                    }
                }

            if (m_aChannel[nChannel].m_fOwned && !m_aChannel[nChannel].m_fEmpty)
                {
if (Config.getBoolean("coherence.subscriber.debug"))
    {
    Logger.info("Subscriber " + f_id + " switching channel - switched to " + nChannel);
    }
                m_nChannel = nChannel;
                return true;
                }
if (Config.getBoolean("coherence.subscriber.debug"))
    {
    Logger.info("Subscriber " + f_id + " switching channel - All Channels Empty!");
    }
            m_nChannel = -1;
            return false;
            }
        finally
            {
            // and finally exit from the gate
            gate.exit();
            }
        }

    /**
     * Handle the result of an async receive.
     *
     * @param channel  the associated channel
     * @param lPageId  the page the poll targeted
     * @param result   the result
     * @param e        and exception
     */
    protected void onReceiveResult(PagedTopicChannel channel, long lVersion, long lPageId, PollProcessor.Result result, Throwable e)
        {
        int nChannel = channel.subscriberPartitionSync.getChannelId();
if (Config.getBoolean("coherence.subscriber.debug"))
    {
    Logger.info("Subscriber " + f_id + " onReceiveResult channel=" + channel + " page=" + lPageId + " result=" + result + " error=" + e);
    }

        // check that there is no error, and we still own the channel
        if (e == null )
            {
            Queue<Binary> queueValues = result.getElements();
            int           cReceived   = queueValues.size();
            int           cRemaining  = result.getRemainingElementCount();
            int           nNext       = result.getNextIndex();

            f_setPolledChannels.set(nChannel);
            ++m_cPolls;

            if (cReceived == 0)
                {
                ++m_cMisses;

                if (channel.m_nNext != nNext && channel.m_nNext != -1) // collision
                    {
                    ++m_cMissCollisions;
                    m_cHitsSinceLastCollision = 0;
                    // don't backoff here, as it is possible all subscribers could end up backing off and
                    // the channel would be temporarily abandoned.  We only backoff as part of trying to increment the
                    // page as that is a CAS and for someone to fail, someone else must have succeeded.
                    }
                // else; spurious notify
                }
            else if (!queueValues.isEmpty())
                {
                f_setHitChannels.set(nChannel);
                ++m_cHitsSinceLastCollision;
                m_cValues += cReceived;

                // add the received elements to the pre-fetch queue
                queueValues.stream()
                        .map(bin -> new CommittableElement(bin, nChannel))
                        .forEach(m_queueValuesPrefetched::add);
                }

            channel.m_nNext = nNext;

            if (cRemaining == PollProcessor.Result.EXHAUSTED)
                {
                // we know the page is exhausted, so the new head is at least one higher
                if (lPageId >= channel.m_lHead && lPageId != Page.NULL_PAGE)
                    {
                    channel.m_lHead = lPageId + 1;
                    channel.m_nNext = 0;
                    }

                // we're actually on the EMPTY_PAGE, so we'll concurrently increment the durable
                // head pointer and then update our pointer accordingly
                if (lPageId == Page.NULL_PAGE)
                    {
                    scheduleHeadIncrement(channel, lPageId);
                    }

                // switch to a new channel since we've exhausted this page
                switchChannel();
                }
            else if (cRemaining == 0 || cRemaining == PollProcessor.Result.NOT_ALLOCATED_CHANNEL)
                {
                // we received nothing or polled a channel we do not own
                if (cRemaining == 0)
                    {
                    // we received nothing, mark the channel as empty
                    onChannelEmpty(nChannel, lVersion);
                    }

                // attempt to switch to a non-empty channel
                if (!switchChannel())
                    {
                    // we've run out of channels to poll from
                    if (f_fCompleteOnEmpty)
                        {
                        // add an empty element, which signals to the completion method that we're done
                        m_queueValuesPrefetched.add(getEmptyElement());
                        }
                    else
                        {
                        // wait for non-empty;
                        // Note: automatically registered for notification as part of returning an empty result set
                        ++m_cWait;
                        }
                    }
                }
            else if (cRemaining == PollProcessor.Result.UNKNOWN_SUBSCRIBER)
                {
                // The subscriber was unknown, possibly due to a persistence snapshot recovery or the topic being
                // destroyed whilst the poll was in progress.
                // Disconnect and let reconnection sort us out
                disconnect();
                }
            }
        else // remove failed; this is fairly catastrophic
            {
            // TODO: figure out error handling
            // fail all currently (and even concurrently) scheduled removes
            f_queueReceiveOrders.handleError((err, bin) -> e, BatchingOperationsQueue.OnErrorAction.CompleteWithException);
            }
        }

    /**
     * Destroy subscriber group.
     *
     * @param pagedTopicCaches   the associated caches
     * @param subscriberGroupId  the group to destroy
     */
    public static void destroy(PagedTopicCaches pagedTopicCaches, SubscriberGroupId subscriberGroupId)
        {
        if (pagedTopicCaches.isActive() && pagedTopicCaches.Subscriptions.isActive())
            {
            int                   cParts      = ((PartitionedService) pagedTopicCaches.Subscriptions.getCacheService()).getPartitionCount();
            Set<Subscription.Key> setSubParts = new HashSet<>(cParts);
            for (int i = 0; i < cParts; ++i)
                {
                // channel 0 will propagate the operation to all other channels
                setSubParts.add(new Subscription.Key(i, /*nChannel*/ 0, subscriberGroupId));
                }

            // see note in TopicSubscriber constructor regarding the need for locking
            boolean fNamed = subscriberGroupId.getMemberTimestamp() == 0;
            if (fNamed)
                {
                pagedTopicCaches.Subscriptions.lock(subscriberGroupId, -1);
                }

            try
                {
                InvocableMapHelper.invokeAllAsync(pagedTopicCaches.Subscriptions, setSubParts,
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
            if (m_nState != STATE_CLOSED)
                {
                setState(STATE_CLOSING); // accept no new requests, and cause all pending ops to complete ASAP (see onReceiveResult)

                try
                    {
                    unregisterMBean();

                    f_queueReceiveOrders.close();
                    f_queueReceiveOrders.cancelAllAndClose("Subscriber has been closed", null);

                    // flush this subscriber to wait for all the outstanding
                    // operations to complete (or to be cancelled if we're destroying)
                    try
                        {
                        flushInternal(fDestroyed ? FlushMode.FLUSH_DESTROY : FlushMode.FLUSH).get(CLOSE_TIMEOUT_SECS, TimeUnit.SECONDS);
                        }
                    catch (TimeoutException e)
                        {
                        // too long to wait for completion; force all outstanding futures to complete exceptionally
                        flushInternal(FlushMode.FLUSH_CLOSE_EXCEPTIONALLY).join();
                        Logger.warn("Subscriber.close: timeout after waiting " + CLOSE_TIMEOUT_SECS
                                + " seconds for completion with flush.join(), forcing complete exceptionally");
                        }
                    catch (ExecutionException | InterruptedException e)
                        {
                        // ignore
                        }

                    if (!fDestroyed)
                        {
                        // caches have not been destroyed, so we're just closing this subscriber
                        unregisterDeactivationListener();
                        unregisterChannelAllocationListener();
                        unregisterNotificationListener();
                        notifyClosed(m_caches.Subscriptions, f_subscriberGroupId, f_id);
                        removeSubscriberEntry();
                        }

                    if (!fDestroyed && f_subscriberGroupId.getMemberTimestamp() != 0)
                        {
                        // this subscriber is anonymous and thus non-durable and must be destroyed upon close
                        // Note: if close isn't the cluster will eventually destroy this subscriber once it
                        // identifies the associated member has left the cluster.
                        // If an application creates a lot of subscribers and does not close them when finished
                        // then this will cause heap consumption to rise.
                        // There used to be a To-Do comment here about cleaning up in a finalizer, but as
                        // finalizers in the JVM are not reliable that is probably not such a good idea.
                        destroy(m_caches, f_subscriberGroupId);
                        }
                    }
                finally
                    {
                    setState(STATE_CLOSED);

                    f_listOnCloseActions.forEach(action ->
                    {
                    try
                        {
                        action.run();
                        }
                    catch (Throwable t)
                        {
                        Logger.finest(this.getClass().getName() + ".close(): handled onClose exception: " +
                            t.getClass().getCanonicalName() + ": " + t.getMessage());
                        }
                    });
                    f_daemon.stop(true);
                    f_daemonChannels.stop(false);
                    }
                }
            }
        }

    /**
     * Obtain a {@link CompletableFuture} that will be complete when
     * all the currently outstanding add operations complete.
     * <p>
     * If this method is called in response to a topic destroy then the
     * outstanding operations will be completed with an exception as the underlying
     * topic caches have been destroyed, so they can never complete normally.
     * <p>
     * if this method is called in response to a timeout waiting for flush to complete normally,
     * indicated by {@link FlushMode#FLUSH_CLOSE_EXCEPTIONALLY}, complete exceptionally all outstanding
     * asynchronous operations so close finishes.
     * <p>
     * The returned {@link CompletableFuture} will always complete
     * normally, even if the outstanding operations complete exceptionally.
     *
     * @param mode  {@link FlushMode} flush mode to use
     *
     * @return a {@link CompletableFuture} that will be completed when
     *         all the currently outstanding add operations are complete
     */
    private CompletableFuture<Void> flushInternal(FlushMode mode)
        {
        String sTopicName   = m_caches.getTopicName();
        String sDescription = null;
        switch (mode)
            {
            case FLUSH_DESTROY:
                sDescription = "Topic " + sTopicName + " was destroyed";

            case FLUSH_CLOSE_EXCEPTIONALLY:
                String sReason = sDescription != null
                        ? sDescription
                        : "Force Close of Subscriber " + f_id + " for topic " + sTopicName;

                BiFunction<Throwable, Request, Throwable> fn  = (err, bin) -> new TopicException(sReason, err);
                Arrays.stream(m_aChannel)
                    .forEach(channel -> f_queueReceiveOrders.handleError(fn,
                        BatchingOperationsQueue.OnErrorAction.CompleteWithException));

                return CompletableFuture.completedFuture(null);

            case FLUSH:
            default:
                return f_queueReceiveOrders.flush();
            }
        }

    /**
     * Called to notify the topic that a subscriber has closed or timed-out.
     *
     * @param cache              the subscription cache
     * @param subscriberGroupId  the subscriber group identifier
     * @param subscriberId       the subscriber identifier
     */
    static void notifyClosed(NamedCache<Subscription.Key, Subscription> cache,
            SubscriberGroupId subscriberGroupId, SubscriberId subscriberId)
        {
        if (!cache.isActive())
            {
            // cache is already inactive, so we do not need to do anything
            return;
            }

        try
            {
            DistributedCacheService service      = (DistributedCacheService) cache.getCacheService();
            int                     cParts       = service.getPartitionCount();
            List<Subscription.Key>  listSubParts = new ArrayList<>(cParts);
            for (int i = 0; i < cParts; ++i)
                {
                // Note: we unsubscribe against channel 0 in each partition, and it will in turn update all channels
                listSubParts.add(new Subscription.Key(i, /*nChannel*/ 0, subscriberGroupId));
                }

            cache.invokeAll(listSubParts, new CloseSubscriptionProcessor(subscriberId));
            }
        catch (Throwable t)
            {
            // this could have been caused by the cache becoming inactive during clean-up, if so ignore the error
            if (cache.isActive())
                {
                // cache is still active, so log the error
                String sId = SubscriberId.NullSubscriber.equals(subscriberId) ? "<ALL>" : idToString(subscriberId.getId());
                Logger.fine("Caught exception closing subscription for subscriber "
                    + sId + " in group " + subscriberGroupId.getGroupName(), t);
                }
            }
        }
    /**
     * Called to remove the entry for this subscriber from the subscriber info cache.
     *
     */
    protected void removeSubscriberEntry()
        {
        NamedCache<SubscriberInfo.Key, SubscriberInfo> cache = m_caches.Subscribers;
        if (!cache.isActive())
            {
            // cache is already inactive so we cannot do anything
            return;
            }

        try
            {
            cache.invoke(f_key, EvictSubscriber.INSTANCE);
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
    protected void registerChannelAllocationListener()
        {
        try
            {
            ChannelListener listenerChannel = m_listenerChannelAllocation;

            if (listenerChannel != null)
                {
                m_caches.Subscriptions.addMapListener(listenerChannel, m_aChannel[0].subscriberPartitionSync, false);
                }
            }
        catch (RuntimeException e)
            {
            Logger.err(e);
            }
        }

    /**
     * Unregister the channel allocation listener.
     */
    protected void unregisterChannelAllocationListener()
        {
        try
            {
            ChannelListener listener = m_listenerChannelAllocation;

            if (listener != null)
                {
                m_caches.Subscriptions.removeMapListener(listener, m_aChannel[0].subscriberPartitionSync);
                }
            }
        catch (RuntimeException e)
            {
            Logger.err(e);
            }
        }

    @SuppressWarnings("unchecked")
    protected void registerNotificationListener()
        {
        // register a subscriber listener in each partition, we must be completely setup before doing this
        // as the callbacks assume we're fully initialized
        if (m_caches.Notifications.isActive())
            {
            m_caches.Notifications.addMapListener(f_listenerNotification, f_filterNotification, /*fLite*/ false);
            }
        }

    @SuppressWarnings("unchecked")
    protected void unregisterNotificationListener()
        {
        // un-register the subscriber listener in each partition
        if (m_caches.Notifications.isActive())
            {
            m_caches.Notifications.removeMapListener(f_listenerNotification, f_filterNotification);
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
            if (!f_fAnonymous)
                {
                GroupDeactivationListener listenerGroup = m_listenerGroupDeactivation;

                // only need to register this listener for non-anonymous subscribers
                if (listenerGroup != null)
                    {
                    m_caches.Subscriptions.addMapListener(listenerGroup, m_aChannel[0].subscriberPartitionSync, true);
                    }
                }

            m_caches.addListener(f_listenerDeactivation);
            }
        catch (RuntimeException e)
            {
            // intentionally empty
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
            GroupDeactivationListener listenerGroup = m_listenerGroupDeactivation;

            if (listenerGroup != null && m_caches.Subscriptions.isActive())
                {
                m_caches.Subscriptions.removeMapListener(listenerGroup, m_aChannel[0].subscriberPartitionSync);
                }

            m_caches.removeListener(f_listenerDeactivation);
            }
        catch (RuntimeException e)
            {
            // intentionally empty
            }
        }

    /**
     * Register the subscriber MBean.
     */
    protected synchronized void registerMBean()
        {
        CacheService service = m_caches.getService();
        MBeanHelper.registerSubscriberMBean(service, f_topic.getName(), this);
        }

    /**
     * Register the subscriber MBean.
     */
    protected synchronized void unregisterMBean()
        {
        CacheService service = m_caches.getService();
        MBeanHelper.unregisterSubscriberMBean(getId(), f_topic.getName(), service.getInfo().getServiceName());
        }

    /**
     * Returns an empty {@link CommittableElement}.
     *
     * @return an empty {@link CommittableElement}
     */
    CommittableElement getEmptyElement()
        {
        if (m_elementEmpty == null)
            {
            Binary binValue   = ExternalizableHelper.toBinary(null, f_serializer);
            Binary binElement = PageElement.toBinary(-1, 0L, 0, 0L, binValue);
            m_elementEmpty = new CommittableElement(binElement, CommittableElement.EMPTY);
            }
        return m_elementEmpty;
        }

    /**
     * Create a subscriber identifier.
     *
     * @param nNotificationId  the notification identifier
     * @param nMemberId        the cluster member id
     *
     * @return a subscriber identifier
     */
    public static long createId(long nNotificationId, long nMemberId)
        {
        return (nMemberId << 32) | (nNotificationId & 0xFFFFFFFFL);
        }

    /**
     * Parse a cluster member id from a subscriber identifier.
     *
     * @param nId  the subscriber identifier
     *
     * @return the cluster member id from the subscriber id
     */
    public static int memberIdFromId(long nId)
        {
        return (int) (nId >> 32);
        }

    /**
     * Return a string representation of a subscriber identifier.
     *
     * @param nId  the subscriber identifier
     *
     * @return a string representation of the subscriber identifier
     */
    public static String idToString(long nId)
        {
        return nId + "/" + memberIdFromId(nId);
        }

    /**
     * Return a string representation of a subscriber identifier.
     *
     * @param id  the subscriber identifier
     *
     * @return a string representation of the subscriber identifier
     */
    public static String idToString(SubscriberId id)
        {
        return id.getId() + "/" + id.getMemberId();
        }

    /**
     * Return a string representation of a collection of subscriber identifiers.
     *
     * @param setId  the collection of subscriber identifiers
     *
     * @return a string representation of the collection of subscriber identifiers
     */
    public static String idToString(Collection<Long> setId)
        {
        return setId.stream()
                .map(PagedTopicSubscriber::idToString)
                .collect(Collectors.joining(","));
        }

    /**
     * Return a string representation of a collection of subscriber identifiers.
     *
     * @param setId  the collection of subscriber identifiers
     *
     * @return a string representation of the collection of subscriber identifiers
     */
    public static String subscriberIdToString(Collection<SubscriberId> setId)
        {
        return setId.stream()
                .map(PagedTopicSubscriber::idToString)
                .collect(Collectors.joining(","));
        }

    /**
     * Parse a subscriber notification identifier from a subscriber identifier.
     *
     * @param nId  the subscriber identifier
     *
     * @return te notification identifier parsed from the subscriber identifier
     */
    public static int notificationIdFromId(long nId)
        {
        return (int) (nId & 0xFFFFFFFFL);
        }

    // ----- inner class: CommittableElement --------------------------------

    /**
     * CommittableElement is a wrapper around a {@link PageElement}
     * that makes it committable.
     */
    private class CommittableElement
        implements Element<V>
        {
        // ----- constructors -----------------------------------------------

        /**
         * Create an element
         *
         * @param binValue  the binary element value
         */
        CommittableElement(Binary binValue, int nChannel)
            {
            m_element  = PageElement.fromBinary(binValue, f_serializer);
            f_nChannel = nChannel;
            }

        // ----- accessors --------------------------------------------------

        /**
         * Returns the wrapped element.
         *
         * @return the wrapped {@link PageElement}
         */
        PageElement<V> getElement()
            {
            return m_element;
            }

        // ----- Element methods --------------------------------------------

        @Override
        public V getValue()
            {
            return m_element.getValue();
            }

        @Override
        public Binary getBinaryValue()
            {
            return m_element.getBinaryValue();
            }

        @Override
        public int getChannel()
            {
            return f_nChannel;
            }

        @Override
        public Position getPosition()
            {
            return m_element.getPosition();
            }

        @Override
        public Instant getTimestamp()
            {
            return m_element.getTimestamp();
            }

        @Override
        public CompletableFuture<CommitResult> commitAsync()
            {
            try
                {
                return PagedTopicSubscriber.this.commitAsync(getChannel(), getPosition());
                }
            catch (Throwable e)
                {
                CommitResult result = new CommitResult(0, null, e);
                return CompletableFuture.completedFuture(result);
                }
            }

        public boolean isEmpty()
            {
            return getChannel() == EMPTY;
            }

        // ----- Object methods ---------------------------------------------

        @Override
        public String toString()
            {
            return "Element(" +
                    "channel=" + f_nChannel +
                    ", position=" + getPosition() +
                    ", timestamp=" + getTimestamp() +
                    ", value=" + getValue() +
                    ')';
            }

        // ----- constructors -----------------------------------------------

        /**
         * The value used for a page id in an empty element.
         */
        public static final int EMPTY = -1;

        // ----- data members -----------------------------------------------

        /**
         * The wrapped element.
         */
        private final PageElement<V> m_element;

        /**
         * The channel for this element.
         */
        private final int f_nChannel;
        }

    // ----- inner class: Channel -------------------------------------------

    /**
     * Channel is a data structure which represents the state of a channel as known
     * by this subscriber.
     */
    public static class PagedTopicChannel
            implements Channel
        {
        @Override
        public int getId()
            {
            return subscriberPartitionSync.getChannelId();
            }

        @Override
        public PagedPosition getLastCommit()
            {
            return m_lastCommit;
            }

        @Override
        public PagedPosition getLastReceived()
            {
            return m_lastReceived;
            }

        @Override
        public boolean isEmpty()
            {
            return m_fEmpty;
            }

        @Override
        public boolean isOwned()
            {
            return m_fOwned;
            }

        @Override
        public PagedPosition getHead()
            {
            if (m_lHead == Page.EMPTY)
                {
                return PagedPosition.NULL_POSITION;
                }
            return new PagedPosition(m_lHead, m_nNext);
            }

        /**
         * Set this channel as empty only if the channel version matches the specified version.
         *
         * @param lVersion  the channel version to use as a CAS
         *
         * @return {@code true} if the version matched and the channel was marked as empty
         */
        protected synchronized boolean setEmpty(long lVersion)
            {
            if (m_lVersion == lVersion)
                {
                m_fEmpty = true;
                return true;
                }
            return false;
            }

        /**
         * Called to notify the channel that a populated notification was received.
         */
        protected synchronized void onChannelPopulatedNotification()
            {
            m_cNotify++;
            setPopulated();
            }

        /**
         * Set this channel as populated and bump the version up by one.
         */
        protected synchronized void setPopulated()
            {
            m_lVersion++;
            m_fEmpty = false;
            }

        /**
         * Return number of channel populated notifications received.
         *
         * @return number of channel populated notifications received
         */
        public long getNotify()
            {
            return m_cNotify;
            }

        /**
         * Return the {@link Subscription.Key} to use to execute cluster wide subscription operations
         * for this channel.
         *
         * @param nPart              the number of partitions
         * @param subscriberGroupId  the subscriber group identifier
         *
         * @return the {@link Subscription.Key} to use to execute cluster wide subscription operations
         *         for this channel
         */
        protected Set<Subscription.Key> ensureSubscriptionKeys(int nPart, SubscriberGroupId subscriberGroupId)
            {
            if (m_setSubscriptionKeys == null)
                {
                int                   nChannel = subscriberPartitionSync.getChannelId();
                Set<Subscription.Key> setKeys  = new HashSet<>();
                for (int p = 0; p < nPart; p++)
                    {
                    setKeys.add(new Subscription.Key(p, nChannel, subscriberGroupId));
                    }
                m_setSubscriptionKeys = setKeys;
                }
            return m_setSubscriptionKeys;
            }

        // ----- Object methods ---------------------------------------------

        public String toString()
            {
            return "Channel=" + subscriberPartitionSync.getChannelId() +
                    ", owned=" + m_fOwned +
                    ", empty=" + m_fEmpty +
                    ", head=" + m_lHead +
                    ", next=" + m_nNext +
                    ", contended=" + m_fContended;
            }

        // ----- data members -----------------------------------------------

        /**
         * The current head page for this subscriber, this value may safely be behind (but not ahead) of the actual head.
         * <p>
         * This field is volatile as it is possible it gets concurrently updated by multiple threads if the futures get
         * completed on IO threads. We don't go with a full blow AtomicLong as either value is suitable and worst case
         * we update to an older value and this would be harmless and just get corrected on the next attempt.
         */
        volatile long m_lHead;

        /**
         * The current version of this channel.
         * <p>
         * This is used for CAS operations on the empty flag.
         */
        volatile long m_lVersion;

        /**
         * The number of channel populated notifications received.
         */
        volatile long m_cNotify;

        /**
         * The index of the next item in the page, or -1 for unknown
         */
        int m_nNext = -1;

        /**
         * True if the channel has been found to be empty.  Once identified as empty we don't need to poll form it again
         * until we receive an event indicating that it has seen a new insertion.
         */
        volatile boolean m_fEmpty;

        /**
         * The key which holds the channels head for this group.
         */
        Subscription.Key subscriberPartitionSync;

        /**
         * True if contention has been detected on this channel.
         */
        boolean m_fContended;

        /**
         * True if this subscriber owns this channel.
         */
        volatile boolean m_fOwned = true;

        /**
         * The set of subscription keys.
         */
        Set<Subscription.Key> m_setSubscriptionKeys;

        /**
         * The last position received by this subscriber
         */
        PagedPosition m_lastReceived;

        /**
         * The last position successfully committed by this subscriber
         */
        PagedPosition m_lastCommit;
        }

    // ----- inner class: FlushMode ----------------------------------------

    enum FlushMode
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

    // ----- inner interface: Request ---------------------------------------

    /**
     * The base interface for subscriber requests.
     */
    protected interface Request
        {
        }

    // ----- inner interface: Request ---------------------------------------

    /**
     * The base interface for subscriber requests
     * that can execute themselves.
     */
    protected static abstract class FunctionalRequest
            implements Request
        {
        protected abstract void execute(PagedTopicSubscriber<?> subscriber, BatchingOperationsQueue<Request, ?> queue);

        protected void onRequestComplete(Object o)
            {
            }

        protected Throwable onRequestError(Throwable err, Object o)
            {
            return new TopicException(err);
            }
        }

    // ----- inner class: ReceiveRequest ------------------------------------

    /**
     * A receive request.
     */
    protected static class ReceiveRequest
            implements Request
        {
        /**
         * Create a receive request.
         *
         * @param fBatch    {@code true} if this is a batch receive
         * @param cElement  the number of elements to receive
         */
        protected ReceiveRequest(boolean fBatch, int cElement)
            {
            f_fBatch   = fBatch;
            f_cElement = cElement;
            }

        // ----- accessors --------------------------------------------------

        /**
         * Returns {@code true} if this is a batch request.
         *
         * @return {@code true} if this is a batch request
         */
        public boolean isBatch()
            {
            return f_fBatch;
            }

        /**
         * Returns the number of elements to receive.
         *
         * @return the number of elements to receive
         */
        public int getElementCount()
            {
            return f_cElement;
            }

        // ----- constructors -----------------------------------------------

        /**
         * A singleton, non-batch, receive request.
         */
        public static final ReceiveRequest SINGLE = new ReceiveRequest(false, 1);

        // ----- data members -----------------------------------------------

        /**
         * A flag indicating whether this is a batch request.
         */
        private final boolean f_fBatch;

        /**
         * The number of elements to receive if this is a batch request.
         */
        private final int f_cElement;
        }

    // ----- inner class: SeekRequest ---------------------------------------

    /**
     * A request to move the subscriber to a new position.
     */
    protected static class SeekRequest
            extends FunctionalRequest
        {
        /**
         * Create a {@link SeekRequest}.
         *
         * @param type       the type of the request
         * @param anChannel  the channels to reposition
         */
        public SeekRequest(SeekType type, int... anChannel)
            {
            this(type, null, null, anChannel);
            }

        /**
         * Create a {@link SeekRequest}.
         *
         * @param mapPosition  a map of {@link Position} keyed by channel to move to
         */
        public SeekRequest(Map<Integer, Position> mapPosition)
            {
            this(SeekType.Position, mapPosition, null);
            }

        /**
         * Create a {@link SeekRequest}.
         *
         * @param instant    the {@link Instant} to use to reposition the subscriber
         * @param anChannel  the channels to reposition
         */
        public SeekRequest(Instant instant, int... anChannel)
            {
            this(SeekType.Instant, null, instant, anChannel);
            }

        /**
         * Create a {@link SeekRequest}.
         *
         * @param type         the type of the request
         * @param mapPosition  a map of {@link Position} keyed by channel to move to
         * @param instant      the {@link Instant} to use to reposition the subscriber
         * @param anChannel    the channels to reposition
         */
        private SeekRequest(SeekType type, Map<Integer, Position> mapPosition, Instant instant, int... anChannel)
            {
            switch (type)
                {
                case Position:
                    if (mapPosition == null)
                        {
                        throw new IllegalArgumentException("Seek request of type " + type + " require a position");
                        }
                    anChannel = mapPosition.keySet().stream().mapToInt(Integer::intValue).toArray();
                    break;
                case Instant:
                    if (instant == null)
                        {
                        throw new IllegalArgumentException("Seek request of type " + type + " require an instant");
                        }
                    break;
                }

            m_type        = type;
            m_mapPosition = mapPosition;
            m_instant     = instant;
            m_anChannel   = anChannel;
            }

        @Override
        @SuppressWarnings("unchecked")
        protected void execute(PagedTopicSubscriber<?> subscriber, BatchingOperationsQueue<Request, ?> queueBatch)
            {
            Map<Integer, Position> map = subscriber.seekInternal(this);
            queueBatch.completeElement(map, this::onRequestError, this::onRequestComplete);
            }

        /**
         * Return the type of seek request.
         *
         * @return the type of seek request
         */
        public SeekType getType()
            {
            return m_type;
            }

        /**
         * Return a map of {@link Position} keyed by channel to move to.
         *
         * @return a map of {@link Position} keyed by channel to move to
         */
        public Map<Integer, Position> getPositions()
            {
            return m_mapPosition;
            }

        /**
         * Return the {@link Instant} to use to reposition the subscriber.
         *
         * @return the {@link Instant} to use to reposition the subscriber
         */
        public Instant getInstant()
            {
            return m_instant;
            }

        /**
         * Return the channels to reposition.
         *
         * @return the channels to reposition
         */
        public int[] getChannels()
            {
            return m_anChannel;
            }

        // ----- data members ---------------------------------------------------

        /**
         * The type of seek request.
         */
        protected final SeekType m_type;

        /**
         * A map of {@link Position} keyed by channel to move to.
         */
        protected final Map<Integer, Position> m_mapPosition;

        /**
         * The {@link Instant} to use to reposition the subscriber.
         */
        protected final Instant m_instant;

        /**
         * The channels to reposition.
         */
        protected final int[] m_anChannel;
        }

    // ----- inner enum: SeekType -------------------------------------------

    /**
     * An enum representing type of {@link SeekRequest}
     */
    protected enum SeekType
        {
        /**
         * Seek to the head of the channel.
         */
        Head,
        /**
         * Seek to the tail of the channel.
         */
        Tail,
        /**
         * Seek to a specific position in a channel.
         */
        Position,
        /**
         * Seek to a specific timestamp in a channel.
         */
        Instant
        }

    // ----- inner class: GetPositionRequest --------------------------------

    /**
     * A request to move the subscriber to a new position.
     */
    protected static class GetPositionRequest
            extends FunctionalRequest
        {
        /**
         * Create a {@link GetPositionRequest}.
         *
         * @param type  the request type
         */
        public GetPositionRequest(PositionType type)
            {
            f_type = type;
            }

        @Override
        protected void execute(PagedTopicSubscriber<?> subscriber, BatchingOperationsQueue<Request, ?> queue)
            {
            Map<Integer, Position> map;
            switch (f_type)
                {
                case Head:
                    map = subscriber.getHeadsInternal();
                    break;
                case Tail:
                    map = subscriber.getTailsInternal();
                    break;
                case Committed:
                    map = subscriber.getLastCommittedInternal();
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + f_type);
                }
            queue.completeElement(map, this::onRequestError, this::onRequestComplete);
            }

        // ----- data members ---------------------------------------------------

        /**
         * A flag indicating whether to obtain the head ({@code true}) or tail ({@code false}) positions.
         */
        private final PositionType f_type;
        }

    // ----- inner enum: SeekType -------------------------------------------

    /**
     * An enum representing type of {@link GetPositionRequest}
     */
    protected enum PositionType
        {
        /**
         * Obtain the head position.
         */
        Head,
        /**
         * Obtain the tail position.
         */
        Tail,
        /**
         * Obtain the committed position.
         */
        Committed,
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
            }

        @Override
        public void onDisconnect()
            {
            disconnect();
            }

        @Override
        public void onDestroy()
            {
            Logger.finest("Detected release of topic "
                                + m_caches.getTopicName() + ", closing subscriber "
                                + PagedTopicSubscriber.this);
            closeInternal(false);
            }

        @Override
        public void onRelease()
            {
            Logger.finest("Detected destroy of topic "
                                + m_caches.getTopicName() + ", closing subscriber "
                                + PagedTopicSubscriber.this);
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
        @SuppressWarnings("rawtypes")
        public void entryDeleted(MapEvent evt)
            {
            if (isActive())
                {
                // destroy subscriber group
                Logger.finest("Detected removal of subscriber group "
                                    + f_subscriberGroupId.getGroupName() + ", closing subscriber "
                                    + PagedTopicSubscriber.this);
                closeInternal(true);
                }
            }
        }

    // ----- inner class: ChannelListener -----------------------------------

    /**
     * A {@link MapListener} that tracks changes to the channels owned by this subscriber.
     */
    protected class ChannelListener
            extends MultiplexingMapListener<Subscription.Key, Subscription>
        {
        public ChannelListener()
            {
            m_latch = new CountDownLatch(1);
            }

        // ----- MultiplexingMapListener methods ----------------------------

        @Override
        protected void onMapEvent(MapEvent<Subscription.Key, Subscription> evt)
            {
            f_daemonChannels.executeTask(() -> onChannelAllocation(evt));
            //onChannelAllocation(evt);
//            CompletableFuture.runAsync(() -> onChannelAllocation(evt))
//                .handle((ignored, err) ->
//                    {
//                    if (err != null)
//                        {
//                        Logger.finer("Error on channel allocation in subscriber " + getId(), err);
//                        }
//                    return null;
//                    });
            }

        // ----- Object methods ---------------------------------------------

        @Override
        @SuppressWarnings("unchecked")
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
            ChannelListener that = (ChannelListener) o;
            return Objects.equals(getId(), that.getId());
            }

        @Override
        public int hashCode()
            {
            return Objects.hash(getId());
            }

        // ----- helper methods ---------------------------------------------

        /**
         * Reset this listener.
         */
        public void reset()
            {
            if (m_latch.getCount() == 0)
                {
                // effectively revokes all channels
                updateChannelOwnership(Collections.emptyList(), true);
                m_latch = new CountDownLatch(1);
                }
            }

        private void onChannelAllocation(MapEvent<Subscription.Key, Subscription> evt)
            {
            if (!isActive())
                {
                return;
                }

            if (evt.isDelete())
                {
                updateChannelOwnership(Collections.emptyList(), true);
                }
            else
                {
                Subscription subscription = evt.getNewValue();
                if (subscription.hasSubscriber(f_id))
                    {
                    List<Integer> list = Arrays.stream(subscription.getChannels(f_id, m_caches.getChannelCount()))
                            .boxed()
                            .collect(Collectors.toList());

                    updateChannelOwnership(list, false);
                    m_latch.countDown();
                    }
                else if (isActive() && !f_fAnonymous && !isDisconnected() && !isInitialising())
                    {
                    Logger.finest("Disconnecting Subscriber " + PagedTopicSubscriber.this);
                    updateChannelOwnership(Collections.emptyList(), true);
                    disconnect();
                    }
                }
            }

        private long getId()
            {
            return PagedTopicSubscriber.this.f_id.getId();
            }

        // ----- data members -----------------------------------------------

        /**
         * A latch that is triggered when channel ownership is initialized.
         */
        private CountDownLatch m_latch;
        }

    // ----- inner class: TimeoutInterceptor --------------------------------

    /**
     * A server side interceptor used to detect removal of {@link SubscriberInfo} entries
     * from the subscriber {@link PagedTopicCaches#Subscribers} when a subscriber is closed
     * or is evicted due to timeout.
     */
    public static class TimeoutInterceptor
            implements EventDispatcherAwareInterceptor<EntryEvent<SubscriberInfo.Key, SubscriberInfo>>
        {
        public TimeoutInterceptor()
            {
            f_executor = Executors.newSingleThreadScheduledExecutor(runnable ->
                {
                String sName = "PagedTopic:SubscriberTimeoutInterceptor:" + f_instance.incrementAndGet();
                return Base.makeThread(null, runnable, sName);
                });
            }

        @Override
        public void introduceEventDispatcher(String sIdentifier, EventDispatcher dispatcher)
            {
            if (dispatcher instanceof PartitionedCacheDispatcher)
                {
                String sCacheName = ((PartitionedCacheDispatcher) dispatcher).getCacheName();
                if (PagedTopicCaches.Names.SUBSCRIBERS.equals(PagedTopicCaches.Names.fromCacheName(sCacheName)))
                    {
                    dispatcher.addEventInterceptor(sIdentifier, this, Collections.singleton(EntryEvent.Type.REMOVED), true);
                    }
                }
            }

        @Override
        public void onEvent(EntryEvent<SubscriberInfo.Key, SubscriberInfo> event)
            {
            if (event.getType() == EntryEvent.Type.REMOVED)
                {
                SubscriberInfo.Key key            = event.getKey();
                long               nId            = key.getSubscriberId();
                SubscriberGroupId  groupId        = key.getGroupId();

                Logger.finest(String.format(
                        "Cleaning up subscriber %d in group '%s' owned by member %d",
                        key.getSubscriberId(), groupId.getGroupName(), memberIdFromId(nId)));

                // we MUST process the event on another thread so as not to block the event dispatcher thread.
                f_executor.execute(() -> processSubscriberRemoval(event));
                }
            }

        @SuppressWarnings({"unchecked"})
        private void processSubscriberRemoval(EntryEvent<SubscriberInfo.Key, SubscriberInfo> event)
            {
            SubscriberInfo.Key key            = event.getKey();
            SubscriberInfo     info           = event.getOriginalValue();
            long               nId            = key.getSubscriberId();
            SubscriberGroupId  groupId        = key.getGroupId();
            String             sTopicName     = PagedTopicCaches.Names.getTopicName(event.getCacheName());
            String             sSubscriptions = PagedTopicCaches.Names.SUBSCRIPTIONS.cacheNameForTopicName(sTopicName);
            CacheService       cacheService   = event.getService();
            int                nMember        = memberIdFromId(nId);

            if (event.getEntry().isSynthetic())
                {
                Logger.finest(String.format(
                        "Subscriber expired after %d ms - groupId='%s', memberId=%d, notificationId=%d, last heartbeat at %s",
                        info.getTimeoutMillis(), groupId.getGroupName(), nMember, notificationIdFromId(nId), info.getLastHeartbeat()));
                }
            else
                {
                boolean fManual = ((Set<Member>) cacheService.getInfo().getServiceMembers()).stream().anyMatch(m -> m.getId() == nMember);
                String  sReason = fManual ? "manual removal of subscriber(s)" : "departure of member " + nMember;
                Logger.finest(String.format(
                        "Subscriber %d in group '%s' removed due to %s",
                        nId, groupId.getGroupName(), sReason));
                }

            SubscriberId subscriberId = new SubscriberId(nId, info.getOwningUid());
            notifyClosed(cacheService.ensureCache(sSubscriptions, null), groupId, subscriberId);
            }

        // ----- constants --------------------------------------------------

        private static final AtomicInteger f_instance = new AtomicInteger();

        // ----- data members -----------------------------------------------

        private final Executor f_executor;
        }

    // ----- inner class: ReconnectTask -------------------------------------

    /**
     * A daemon task to reconnect the subscriber iff the subscriber
     * has outstanding receive requests.
     */
    protected static class ReconnectTask
            implements Runnable
        {
        /**
         * Create a reconnection task.
         *
         * @param subscriber  the subscriber to reconnect
         */
        protected ReconnectTask(PagedTopicSubscriber subscriber)
            {
            m_subscriber = subscriber;
            }

        @Override
        public void run()
            {
            if (m_subscriber.getState() == PagedTopicSubscriber.STATE_CONNECTED || !m_subscriber.isActive())
                {
                // nothing to do, either the subscriber is connected, or it is closed
                return;
                }
            try
                {
                if (m_subscriber.m_caches.getService().isSuspended())
                    {
                    Logger.finest("Skipping reconnect task, service is suspended for subscriber " + m_subscriber);
                    }
                else
                    {
                    if (m_subscriber.f_queueReceiveOrders.size() > 0)
                        {
                        Logger.finest("Running reconnect task, reconnecting " + m_subscriber);
                        m_subscriber.ensureConnected();
                        }
                    else
                        {
                        Logger.finest("Skipping reconnect task, no pending receives for subscriber " + m_subscriber);
                        }
                    }
                }
            catch (Throwable t)
                {
                Logger.finest("Failed to reconnect subscriber " + m_subscriber, t);
                }
            f_cExecution.incrementAndGet();
            }

        /**
         * Returns the task execution count.
         *
         * @return the task execution count
         */
        public int getExecutionCount()
            {
            return f_cExecution.get();
            }

        // ----- data members -----------------------------------------------

        /**
         * The subscriber to reconnect.
         */
        private final PagedTopicSubscriber m_subscriber;

        /**
         * The number of time the task has executed.
         */
        private final AtomicInteger f_cExecution = new AtomicInteger();
        }

    // ----- constants ------------------------------------------------------

    /**
     * Value of the initial subscriber state.
     */
    public static final int STATE_INITIAL = 0;

    /**
     * Value of the subscriber state when connected.
     */
    public static final int STATE_CONNECTED = 1;

    /**
     * Value of the subscriber state when disconnected.
     */
    public static final int STATE_DISCONNECTED = 2;

    /**
     * Value of the subscriber state when closing.
     */
    public static final int STATE_CLOSING = 3;

    /**
     * Value of the subscriber state when closed.
     */
    public static final int STATE_CLOSED = 4;

    /**
     * An array of state names. The indexes match the values of the state constants.
     */
    public static final String[] STATES = {"Initial", "Connected", "Disconnected", "CLosing", "Closed"};

    /**
     * Subscriber close timeout on first flush attempt. After this time is exceeded, all outstanding asynchronous operations will be completed exceptionally.
     */
    public static final long CLOSE_TIMEOUT_SECS = TimeUnit.MILLISECONDS.toSeconds(Base.parseTime(Config.getProperty("coherence.topic.subscriber.close.timeout", "30s"), Base.UNIT_S));

    /**
     * Subscriber initialise timeout.
     */
    public static final long INIT_TIMEOUT_SECS = TimeUnit.MILLISECONDS.toSeconds(Base.parseTime(Config.getProperty("coherence.topic.subscriber.init.timeout", "30s"), Base.UNIT_S));

    // ----- data members ---------------------------------------------------

    /**
     * The underlying {@link NamedTopic} being subscribed to.
     */
    private final NamedTopic<?> f_topic;

    /**
     * The {@link PagedTopicCaches} instance managing the caches for the topic
     * being consumed.
     */
    protected PagedTopicCaches m_caches;

    /**
     * Flag indicating whether this subscriber is part of a group or is anonymous.
     */
    protected final boolean f_fAnonymous;

    /**
     * This subscribers cluster wide unique identifier.
     */
    protected final SubscriberId f_id;

    /**
     * The {@link SubscriberInfo.Key} to use to send heartbeats.
     */
    protected SubscriberInfo.Key f_key;

    /**
     * The optional {@link Filter} to use to filter messages.
     */
    protected final Filter<V>   f_filter;

    /**
     * The optional function to use to transform the payload of the message on the server.
     */
    protected final Function<V, ?> f_fnConverter;

    /**
     * The cache's serializer.
     */
    protected final Serializer f_serializer;

    /**
     * The identifier for this {@link PagedTopicSubscriber}.
     */
    protected SubscriberGroupId f_subscriberGroupId;

    /**
     * This subscriber's notification id.
     */
    protected final int f_nNotificationId;

    /**
     * The filter used to register the notification listener.
     */
    protected final Filter<Object> f_filterNotification;

    /**
     * True if configured to complete when empty
     */
    protected final boolean f_fCompleteOnEmpty;

    /**
     * The {@link Gate} controlling access to the channel operations.
     */
    private final Gate<?> f_gate;

    /**
     * The state of the subscriber.
     */
    private volatile int m_nState = STATE_INITIAL;

    /**
     * Optional queue of prefetched values which can be used to fulfil future receive requests.
     */
    protected Queue<CommittableElement> m_queueValuesPrefetched = new ConcurrentLinkedDeque<>();

    /**
     * Queue of pending receive awaiting values.
     */
    protected final BatchingOperationsQueue<Request, ?> f_queueReceiveOrders;

    /**
     * Subscriber flow control object.
     */
    protected final DebouncedFlowControl f_backlog;

    /**
     * The state for the channels.
     */
    protected volatile PagedTopicChannel[] m_aChannel;

    /**
     * The owned channels.
     */
    protected volatile int[] m_aChannelOwned;

    /**
     * The current channel.
     */
    protected volatile int m_nChannel;

    /**
     * The daemon used to complete subscriber futures so that they are not on the service thread.
     */
    protected final TaskDaemon f_daemon;

    /**
     * The daemon used to execute subscriber channel allocation changes.
     */
    protected final TaskDaemon f_daemonChannels;

    /**
     * The listener that receives notifications for non-empty channels.
     */
    @SuppressWarnings("rawtypes")
    private final SimpleMapListener f_listenerNotification;

    /**
     * The listener that will update channel allocations.
     */
    protected ChannelListener m_listenerChannelAllocation;

    /**
     * The array of {@link ChannelOwnershipListener listeners} to be notified when channel allocations change.
     */
    protected final ChannelOwnershipListener[] m_aChannelOwnershipListener;

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
    @SuppressWarnings("unchecked")
    protected final List<PagedTopicChannel> f_listChannelsContended = new CircularArrayList();

    /**
     * BitSet of polled channels since last toString call.
     */
    protected final BitSet f_setPolledChannels;

    /**
     * BitSet of channels which hit since last toString call.
     */
    protected final BitSet f_setHitChannels;

    /**
     * The deactivation listener.
     */
    protected final DeactivationListener f_listenerDeactivation = new DeactivationListener();

    /**
     * The NamedCache deactivation listener.
     */
    protected GroupDeactivationListener m_listenerGroupDeactivation;

    /**
     * A {@link List} of actions to run when this publisher closes.
     */
    private final List<Runnable> f_listOnCloseActions = new ArrayList<>();

    /**
     * An empty committable element.
     */
    private CommittableElement m_elementEmpty;

    /**
     * The number of completed receive requests.
     */
    private final Meter m_cReceived = new Meter();

    /**
     * The number of completed receive requests.
     */
    private final Meter m_cReceivedEmpty = new Meter();

    /**
     * The number of exceptionally completed receive requests.
     */
    private final Meter m_cReceivedError = new Meter();

    /**
     * The number of disconnections.
     */
    private final Meter m_cDisconnect = new Meter();

    /**
     * The {@link ReconnectTask} to use to reconnect this subscriber.
     */
    private final ReconnectTask f_taskReconnect;
    }
