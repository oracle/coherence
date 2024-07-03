/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic.impl.paged;

import com.oracle.coherence.common.base.Exceptions;
import com.oracle.coherence.common.base.Logger;

import com.oracle.coherence.common.base.TimeHelper;
import com.oracle.coherence.common.util.Options;
import com.oracle.coherence.common.util.SafeClock;
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
import com.tangosol.internal.net.topic.impl.paged.model.PagedTopicSubscription;
import com.tangosol.internal.net.topic.impl.paged.model.SubscriberGroupId;
import com.tangosol.internal.net.topic.impl.paged.model.SubscriberId;
import com.tangosol.internal.net.topic.impl.paged.model.SubscriberInfo;
import com.tangosol.internal.net.topic.impl.paged.model.Subscription;

import com.tangosol.internal.util.Daemons;
import com.tangosol.io.Serializer;

import com.tangosol.net.CacheService;
import com.tangosol.net.Cluster;
import com.tangosol.net.FlowControl;
import com.tangosol.net.Member;
import com.tangosol.net.NamedCache;
import com.tangosol.net.PagedTopicService;
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
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.Filter;
import com.tangosol.util.Filters;
import com.tangosol.util.Gate;
import com.tangosol.util.InvocableMapHelper;
import com.tangosol.util.Listeners;
import com.tangosol.util.LongArray;
import com.tangosol.util.MapEvent;
import com.tangosol.util.MapListener;
import com.tangosol.util.SparseArray;
import com.tangosol.util.TaskDaemon;
import com.tangosol.util.ThreadGateLite;
import com.tangosol.util.UUID;
import com.tangosol.util.ValueExtractor;

import com.tangosol.util.aggregator.ComparableMin;
import com.tangosol.util.aggregator.GroupAggregator;
import com.tangosol.util.aggregator.LongMin;

import com.tangosol.util.extractor.ReflectionExtractor;

import com.tangosol.util.filter.InKeySetFilter;

import com.tangosol.util.listener.SimpleMapListener;

import java.io.PrintStream;

import java.time.Instant;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EventListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;

import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;

import java.util.stream.Collectors;

/**
 * A subscriber of values from a paged topic.
 *
 * @author jk/mf 2015.06.15
 * @since Coherence 14.1.1
 */
@SuppressWarnings({"rawtypes"})
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
        f_serializer                 = m_caches.getSerializer();
        f_listenerNotification       = new SimpleMapListener<>().addDeleteHandler(this::onChannelPopulatedNotification);
        f_gate                       = new ThreadGateLite<>();
        f_gateState                  = new ThreadGateLite<>();

        ChannelOwnershipListeners<T> listeners = optionsMap.get(ChannelOwnershipListeners.class, ChannelOwnershipListeners.none());
        m_aChannelOwnershipListener = listeners.getListeners().toArray(new ChannelOwnershipListener[0]);

        CacheService cacheService = m_caches.getService();
        Cluster      cluster      = cacheService.getCluster();
        Member       member       = cluster.getLocalMember();

        boolean            fWarn  = false;
        WithNotificationId withId = optionsMap.get(WithNotificationId.class);
        if (withId == null)
            {
            f_nNotificationId = System.identityHashCode(this);
            }
        else
            {
            f_nNotificationId = withId.getId();
            fWarn = true;
            }

        f_fCompleteOnEmpty          = optionsMap.contains(CompleteOnEmpty.class);
        f_filterNotification        = new InKeySetFilter<>(/*filter*/ null, m_caches.getPartitionNotifierSet(f_nNotificationId));
        f_id                        = new SubscriberId(f_nNotificationId, member.getId(), member.getUuid());
        f_subscriberGroupId         = f_fAnonymous ? SubscriberGroupId.anonymous() : SubscriberGroupId.withName(sName);
        f_key                       = new SubscriberInfo.Key(f_subscriberGroupId, f_id.getId());
        f_heartbeatProcessor        = new SubscriberHeartbeatProcessor();
        m_listenerChannelAllocation = new ChannelListener(f_id, new PagedTopicSubscription.Key(f_topic.getName(), f_subscriberGroupId));

        Filtered filtered = optionsMap.get(Filtered.class);
        f_filter = filtered == null ? null : filtered.getFilter();

        Convert convert = optionsMap.get(Convert.class);
        f_extractor = convert == null ? null : convert.getExtractor();

        f_taskReconnect = new ReconnectTask(this);

        f_daemon         = new TaskDaemon("PagedTopic:Subscriber:" + m_caches.getTopicName() + ":" + f_id.getId());
        f_executor       = new TaskDaemon("PagedTopic:Subscriber:" + m_caches.getTopicName() + ":Receive:" + f_id.getId());
        f_daemonChannels = new TaskDaemon("PagedTopic:Subscriber:" + m_caches.getTopicName() + ":Channels:" + f_id.getId());
        f_executorChannels = f_daemonChannels::executeTask;
        f_daemon.start();
        f_daemonChannels.start();

        long cBacklog = cluster.getDependencies().getPublisherCloggedCount();
        f_backlog            = new DebouncedFlowControl((cBacklog * 2) / 3, cBacklog);
        f_queueReceiveOrders = new BatchingOperationsQueue<>(this::trigger, 1,
                                        f_backlog, v -> 1, BatchingOperationsQueue.Executor.fromTaskDaemon(f_daemon));

        int cChannel = m_caches.getChannelCount();

        m_aChannel = initializeChannels(m_caches, cChannel, f_subscriberGroupId);

        WithIdentifyingName withIdentifyingName = optionsMap.get(WithIdentifyingName.class);
        f_sIdentifyingName = withIdentifyingName == null ? null : withIdentifyingName.getName();

        registerChannelAllocationListener();
        registerDeactivationListener();
        registerMBean();

        if (fWarn)
            {
            Logger.warn("Subscriber " + f_id + " is being created with a custom notification id " + f_nNotificationId);
            }

        ensureConnected();

        // Note: post construction this implementation must be fully async
        }

    // ----- PagedTopicSubscriber methods -----------------------------------

    /**
     * Returns the subscriber's identifier.
     *
     * @return the subscriber's identifier
     */
    public long getId()
        {
        return f_id.getId();
        }

    /**
     * Returns the subscriber's unique identifier.
     *
     * @return the subscriber's unique identifier
     */
    public SubscriberId getSubscriberId()
        {
        return f_id;
        }

    /**
     * Returns the unique identifier of the subscribe group,
     * or zero if the subscriber is anonymous.
     *
     * @return the unique identifier of the subscribe group,
     *         or zero if the subscriber is anonymous
     */
    public long getSubscriptionId()
        {
        return m_subscriptionId;
        }

    /**
     * Returns the subscriber's optional identifying name.
     *
     * @return the subscriber's unique identifying name
     */
    public String getIdentifyingName()
        {
        return f_sIdentifyingName;
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

    public ValueExtractor<V, ?> getConverter()
        {
        return f_extractor;
        }

    public Serializer getSerializer()
        {
        return f_serializer;
        }

    public boolean isCompleteOnEmpty()
        {
        return f_fCompleteOnEmpty;
        }

    /**
     * Print the state of this subscriber's channels.
     *
     * @param out  the {@link PrintStream} to print to
     */
    public void printChannels(PrintStream out)
        {
        Gate<?> gate = f_gate;
        // Wait to enter the gate
        gate.enter(-1);
        try
            {
            out.println("Owned: " + Arrays.toString(m_aChannelOwned));
            for (int c = 0; c < m_aChannel.length; c++)
                {
                out.printf("%d: %s current=%b\n", c, m_aChannel[c], (c == m_nChannel));
                }
            }
        finally
            {
            // and finally exit from the gate
            gate.exit();
            }
        }

    /**
     * Print the state of this subscriber's channels.
     *
     * @param out  the {@link PrintStream} to print to
     */
    public void printPreFetchCache(PrintStream out)
        {
        Gate<?> gate = f_gate;
        // Wait to enter the gate
        gate.enter(-1);
        try
            {
            out.println("Pre-Fetch Cache: ");
            m_queueValuesPrefetched.forEach(out::println);
            }
        finally
            {
            // and finally exit from the gate
            gate.exit();
            }
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
        CompletableFuture<Element<V>> future = (CompletableFuture<Element<V>>) f_queueReceiveOrders.add(ReceiveRequest.SINGLE);
        f_cReceiveRequests.add(1L);
        future.handle((e, error) ->
            {
            if (error instanceof CancellationException)
                {
                if (!(error instanceof BatchingOperationsQueue.OperationCancelledException))
                    {
                    Logger.err("Receive cancelled", error);
                    }
                f_cCancelled.add(1L);
                }
            return null;
            });
        return future;
        }

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<List<Element<V>>> receive(int cBatch)
        {
        ensureActive();
        CompletableFuture<List<Element<V>>> future = (CompletableFuture<List<Element<V>>>) f_queueReceiveOrders.add(new ReceiveRequest(true, cBatch));
        f_cReceiveRequests.add(1L);
        future.handle((e, error) ->
            {
            if (error instanceof CancellationException)
                {
                if (!(error instanceof BatchingOperationsQueue.OperationCancelledException))
                    {
                    Logger.err("Receive cancelled", error);
                    }
                f_cCancelled.add(1L);
                }
            return null;
            });
        return future;
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
                if (m_nState == STATE_CONNECTED)
                    {
                    return Arrays.stream(m_aChannel)
                            .filter(PagedTopicChannel::isOwned)
                            .map(c -> c.subscriberPartitionSync.getChannelId())
                            .collect(Collectors.toSet());
                    }
                }
            finally
                {
                gate.exit();
                }
            }
        return Collections.emptySet();
        }

    @Override
    public boolean isOwner(int nChannel)
        {
        if (m_nState == STATE_CONNECTED)
            {
            return nChannel >= 0 && m_aChannel[nChannel].isOwned();
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
        long cWaitNow   = m_cWait;
        long cNotifyNow = m_cNotify;

        long cPoll   = cPollsNow  - m_cPollsLast;
        long cValues = cValuesNow - m_cValuesLast;
        long cMisses = cMissesNow - m_cMissesLast;
        long cWait   = cWaitNow   - m_cWaitsLast;
        long cNotify = cNotifyNow - m_cNotifyLast;

        m_cPollsLast          = cPollsNow;
        m_cValuesLast         = cValuesNow;
        m_cMissesLast         = cMissesNow;
        m_cWaitsLast          = cWaitNow;
        m_cNotifyLast         = cNotifyNow;

        PagedTopicChannel[] aChannel = m_aChannel;
        long   cChannelsPolled = Arrays.stream(aChannel).filter(PagedTopicChannel::isPolled).count();
        String sChannelsPolled = Arrays.toString(Arrays.stream(aChannel).filter(PagedTopicChannel::isPolled).mapToInt(PagedTopicChannel::getId).toArray());
        long   cChannelsHit    = Arrays.stream(aChannel).filter(PagedTopicChannel::isHit).count();
        String sChannelsHit    = Arrays.toString(Arrays.stream(aChannel).filter(PagedTopicChannel::isHit).mapToInt(PagedTopicChannel::getId).toArray());

        String sState = getStateName();
        String sName  = f_sIdentifyingName == null ? "" : ", name=" + f_sIdentifyingName;

        return getClass().getSimpleName() + "(" + "topic=" + m_caches.getTopicName() + sName +
            ", id=" + f_id +
            ", group=" + f_subscriberGroupId +
            ", subscriptionId=" + m_subscriptionId +
            ", durable=" + !f_fAnonymous +
            ", state=" + sState +
            ", prefetched=" + m_queueValuesPrefetched.size() +
            ", backlog=" + f_backlog +
            ", subscriptions=" + m_cSubscribe.getCount() +
            ", disconnections=" + m_cDisconnect.getCount() +
            ", received=" + m_cReceived.getCount() +
            ", receivedEmpty=" + m_cReceivedEmpty.getCount() +
            ", receivedError=" + m_cReceivedError.getCount() +
            ", channelAllocation=" + (f_fAnonymous ? "[ALL]" : Arrays.toString(m_aChannelOwned)) +
            ", totalChannelsPolled=" + cPollsNow +
            ", channelsPolledSinceReallocation=" + sChannelsPolled + cChannelsPolled +
            ", channelsHit=" + sChannelsHit + cChannelsHit + "/" + cChannelsPolled  +
            ", batchSize=" + (cValues / (Math.max(1, cPoll - cMisses))) +
            ", values=" + cValuesNow +
            ", notifications=" + cNotifyNow +
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
     * Return the number of cancelled receive requests.
     *
     * @return the number of cancelled receive requests
     */
    public long getCancelled()
        {
        return f_cCancelled.longValue();
        }

    /**
     * Return the count of calls to one of the receive methods.
     *
     * @return the count of calls to one of the receive methods
     */
    public long getReceiveRequests()
        {
        return f_cReceiveRequests.longValue();
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
     * Return the mean rate of completed receive requests.
     *
     * @return the mean rate of completed receive requests
     */
    public double getReceivedMeanRate()
        {
        return m_cReceived.getMeanRate();
        }

    /**
     * Return the one-minute rate of completed receive requests.
     *
     * @return the one-minute rate of completed receive requests
     */
    public double getReceivedOneMinuteRate()
        {
        return m_cReceived.getOneMinuteRate();
        }

    /**
     * Return the five-minute rate of completed receive requests.
     *
     * @return the five-minute rate of completed receive requests
     */
    public double getReceivedFiveMinuteRate()
        {
        return m_cReceived.getFiveMinuteRate();
        }

    /**
     * Return the fifteen-minute rate of completed receive requests.
     *
     * @return the fifteen-minute rate of completed receive requests
     */
    public double getReceivedFifteenMinuteRate()
        {
        return m_cReceived.getFifteenMinuteRate();
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
    protected void initialise() throws InterruptedException, ExecutionException, TimeoutException
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

            while(m_nState != STATE_CONNECTED)
                {
                if (!isActive())
                    {
                    // the subscriber has been closed
                    break;
                    }

                int nPrevState = setState(STATE_CONNECTING);

                if (fReconnect)
                    {
                    Logger.finest("Reconnecting subscriber " + this);
                    }

                m_caches.ensureConnected();

                if (!f_fAnonymous)
                    {
                    PagedTopicService service = m_caches.getService();
                    if (m_subscriptionId == 0)
                        {
                        // this is the first time, so we get the unique id of the subscriber group
                        m_subscriptionId = service.ensureSubscription(f_topic.getName(), f_subscriberGroupId, f_id,
                                                                      f_filter, f_extractor);
                        }
                    else
                        {
                        // this is a reconnect request, ensure the group still exists
                        // ensure this subscriber is subscribed
                        service.ensureSubscription(f_topic.getName(), m_subscriptionId, f_id, m_fForceReconnect);
                        if (service.isSubscriptionDestroyed(m_subscriptionId))
                            {
                            close();
                            throw new IllegalStateException("The subscriber group \"" + f_subscriberGroupId + "\" (id="
                                                                    + m_subscriptionId + ") this subscriber was previously subscribed to has been destroyed");
                            }
                        }
                    PagedTopicSubscription subscription = service.getSubscription(m_subscriptionId);
                    if (subscription != null)
                        {
                        m_connectionTimestamp = subscription.getSubscriberTimestamp(f_id);
                        }
                    else
                        {
                        // the subscription may be null during rolling upgrade where the senior is an older version
                        m_connectionTimestamp = SafeClock.INSTANCE.getSafeTimeMillis();
                        }

                    // heartbeat immediately to update the subscriber's timestamp in the Subscriber cache
                    heartbeat(false);
                    }

                boolean fDisconnected = nPrevState == STATE_DISCONNECTED;
                long[]  alHead        = m_caches.initializeSubscription(f_subscriberGroupId, f_id, m_subscriptionId,
                                                                        f_filter, f_extractor, fReconnect, false, fDisconnected);
                int     cChannel      = alHead.length;

                if (cChannel > m_aChannel.length)
                    {
                    // this subscriber has fewer channels than the server so needs to be resized
                    PagedTopicChannel[] aChannel = m_aChannel;
                    m_aChannel = initializeChannels(m_caches, cChannel, f_subscriberGroupId, aChannel);
                    }

                for (int nChannel = 0; nChannel < cChannel; ++nChannel)
                    {
                    PagedTopicChannel channel = m_aChannel[nChannel];
                    channel.m_lHead  = alHead[nChannel];
                    channel.m_nNext  = PagedPosition.NULL_OFFSET; // unknown page position to start
                    channel.setPopulated(); // even if we could infer emptiness here it is unsafe unless we've registered for events
                    }

                if (f_fAnonymous)
                    {
                    // anonymous so we own all channels
                    SortedSet<Integer> listChannel = new TreeSet<>();
                    for (int i = 0; i < cChannel; i++)
                        {
                        listChannel.add(i);
                        }
                    updateChannelOwnership(listChannel, false);
                    }
                else
                    {
                    PagedTopicSubscription pagedTopicSubscription = m_caches.getService().getSubscription(m_subscriptionId);
                    SortedSet<Integer>     setChannel;
                    if (pagedTopicSubscription != null)
                        {
                        // we have a PagedTopicSubscription so get the channels from it
                        setChannel = pagedTopicSubscription.getOwnedChannels(f_id);
                        }
                    else
                        {
                        CompletableFuture<Subscription> future = m_caches.Subscriptions.async().get(m_aChannel[0].subscriberPartitionSync);
                        try
                            {
                            // we use a timout here because a never ending get can cause a deadlock during fail-over scenarios
                            Subscription subscription = future.get(INIT_TIMEOUT_SECS, TimeUnit.SECONDS);
                            setChannel = Arrays.stream(subscription.getChannels(f_id, cChannel)).boxed().collect(Collectors.toCollection(TreeSet::new));
                            }
                        catch (TimeoutException e)
                            {
                            future.cancel(true);
                            if (future.isDone() && !future.isCompletedExceptionally())
                                {
                                // the future must have completed between the get timeout and the cancel call
                                Subscription subscription = future.get(INIT_TIMEOUT_SECS, TimeUnit.SECONDS);
                                setChannel = Arrays.stream(subscription.getChannels(f_id, cChannel)).boxed().collect(Collectors.toCollection(TreeSet::new));
                                }
                            else
                                {
                                throw e;
                                }
                            }
                        }
                    updateChannelOwnership(setChannel, false);
                    }

                heartbeat();
                registerNotificationListener();
                if (casState(STATE_CONNECTING, STATE_CONNECTED))
                    {
                    switchChannel();
                    }
                }

            m_cSubscribe.mark();
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
            heartbeat();
            complete(queueRequest);

            int nChannel = ensureOwnedChannel();
            if (!queueRequest.isBatchComplete() && nChannel >= 0)
                {
                // we have emptied the pre-fetch queue but the batch has more in it, so fetch more
                PagedTopicChannel channel  = m_aChannel[nChannel];
                long              lVersion = channel.getVersion();
                long              lHead    = channel.m_lHead == PagedTopicChannel.HEAD_UNKNOWN
                                                    ? getSubscriptionHead(channel) : channel.m_lHead;

                int nPart = ((PartitionedService) m_caches.Subscriptions.getCacheService())
                                    .getKeyPartitioningStrategy()
                                    .getKeyPartition(new Page.Key(nChannel, lHead));

                InvocableMapHelper.invokeAsync(m_caches.Subscriptions,
                                               new Subscription.Key(nPart, nChannel, f_subscriberGroupId), m_caches.getUnitOfOrder(nPart),
                                               new PollProcessor(lHead, Integer.MAX_VALUE, f_nNotificationId, f_id),
                                               f_executor,
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
     * Returns the initial head page.
     *
     * @return the initial head page
     */
    private long getSubscriptionHead(PagedTopicChannel channel)
        {
        Subscription subscription = m_caches.Subscriptions.get(channel.subscriberPartitionSync);
        if (subscription == null)
            {
            try
                {
                initialise();
                }
            catch (Throwable e)
                {
                throw Exceptions.ensureRuntimeException(e);
                }
            return m_aChannel[channel.getId()].m_lHead;
            }
        return subscription.getSubscriptionHead();
        }

    /**
     * Complete as many outstanding requests as possible from the contents of the pre-fetch queue.
     *
     * @param queueRequest  the queue of requests to complete
     */
    protected void complete(BatchingOperationsQueue<Request, ?> queueRequest)
        {
        LinkedList<Request> queueBatch = queueRequest.getCurrentBatchValues();
        complete(queueRequest, queueBatch);
        }

    /**
     * Complete as many outstanding requests as possible from the contents of the pre-fetch queue.
     *
     * @param queueRequest  the queue of requests to complete
     */
    @SuppressWarnings("unchecked")
    protected void complete(BatchingOperationsQueue<Request, ?> queueRequest,
                            LinkedList<Request> queueBatch)
        {
        ConcurrentLinkedDeque<CommittableElement> queuePrefetched = m_queueValuesPrefetched;

        Request firstRequest = queueBatch.peek();
        while (firstRequest instanceof FunctionalRequest)
            {
            ((FunctionalRequest) queueBatch.poll()).execute(this, queueRequest);
            firstRequest = queueBatch.peek();
            }

        int cValues  = 0;
        int cRequest = queueBatch.size();

        if (isActive() && !queuePrefetched.isEmpty())
            {
            Gate<?> gate = f_gate;
            // Wait to enter the gate as we need to check channel ownership under a lock
            gate.enter(-1);
            try
                {
                LongArray          aValues = new SparseArray<>();
                CommittableElement element = queuePrefetched.peek();

                if (element != null && element.isEmpty())
                    {
                    // we're empty, remove the empty/null element from the pre-fetch queue
                    queuePrefetched.poll();
                    while (cValues < cRequest)
                        {
                        Request request = queueBatch.get(cValues);
                        if (request instanceof ReceiveRequest)
                            {
                            ReceiveRequest receiveRequest = (ReceiveRequest) request;
                            if (!receiveRequest.isBatch())
                                {
                                // this is a single request, i.e subscriber.receive();
                                aValues.set(cValues, null);
                                }
                            else
                                {
                                // this is a batch request, i.e subscriber.receive(100);
                                aValues.set(cValues, Collections.emptyList());
                                }
                            cValues++;
                            }
                        }
                    // complete all the requests as "empty"
                    queueRequest.completeElements(cValues, aValues, this::onReceiveError, this::onReceiveComplete);
                    }
                else
                    {
                    while (m_nState == STATE_CONNECTED && cValues < cRequest && !queuePrefetched.isEmpty())
                        {
                        Request request = queueBatch.get(cValues);
                        if (request instanceof ReceiveRequest)
                            {
                            ReceiveRequest receiveRequest = (ReceiveRequest) request;
                            if (receiveRequest.isBatch())
                                {
                                // this is a batch request, i.e subscriber.receive(100);
                                int cElement = receiveRequest.getElementCount();
                                LinkedList<CommittableElement> list = new LinkedList<>();
                                for (int i = 0; i < cElement && !queuePrefetched.isEmpty(); i++)
                                    {
                                    element = queuePrefetched.poll();
                                    // ensure we still own the channel
                                    if (element != null && !element.isEmpty() && isOwner(element.getChannel()))
                                        {
                                        list.add(element);
                                        }
                                    }
                                cValues++;
                                boolean fCompleted = queueRequest.completeElement(list, this::onReceiveComplete);
                                if (!fCompleted)
                                    {
                                    // failed to complete the future, it could have been cancelled
                                    // push the all the elements back on the queue
                                    CommittableElement e;
                                    while ((e = list.pollLast()) != null)
                                        {
                                        queuePrefetched.offerFirst(e);
                                        }
                                    }
                                }
                            else
                                {
                                // this is a single request, i.e subscriber.receive();
                                element = queuePrefetched.poll();
                                // ensure we still own the channel
                                if (element != null && !element.isEmpty() && isOwner(element.getChannel()))
                                    {
                                    cValues++;
                                    boolean fCompleted = queueRequest.completeElement(element, this::onReceiveComplete);
                                    if (!fCompleted)
                                        {
                                        // failed to complete the future, it could have been cancelled
                                        // push the element back on the queue
                                        queuePrefetched.offerFirst(element);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            finally
                {
                // and finally exit from the gate
                gate.exit();
                }
            }
        }

    /**
     * Return the size of the prefetch queue.
     *
     * @return the size of the prefetch queue
     */
    public int getReceiveQueueSize()
        {
        return f_queueReceiveOrders.size();
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
        if (m_aChannel[c].isOwned())
            {
            m_aChannel[c].m_lastReceived = (PagedPosition) element.getPosition();
            m_aChannel[c].m_cReceived.mark();
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
                    = CompletableFuture.supplyAsync(() -> m_caches.Subscriptions.invokeAll(setKeys, new CommitProcessor(position, f_id)), Daemons.commonPool());

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

                            m_aChannel[nChannel].committed(position);
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

        if (!listUnallocated.isEmpty())
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

            if (!listUnallocated.isEmpty())
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
        return getStateName(m_nState);
        }

    /**
     * Returns the state of the subscriber.
     *
     * @return the state of the subscriber
     */
    public static String getStateName(int nState)
        {
        if (nState >= 0 && nState < STATES.length)
            {
            return STATES[nState];
            }
        return "Unknown (" + nState + ")";
        }

    /**
     * Set the state of the subscriber.
     *
     * @param nState  the state of the subscriber
     *
     * @return the previous state
     */
    protected int setState(int nState)
        {
        try (Sentry<?> ignored = f_gateState.close())
            {
            int nPrevState = m_nState;
            m_nState = nState;
            if (nPrevState != nState)
                {
                notifyStateChange(nState, nPrevState);
                }
            return nPrevState;
            }
        }

    /**
     * CAS the state of the subscriber.
     *
     * @param nPrevState  the expected previous state of the subscriber
     * @param nState      the state of the subscriber
     *
     * @return {@code true} if the state was changed
     */
    @SuppressWarnings("SameParameterValue")
    protected boolean casState(int nPrevState, int nState)
        {
        if (m_nState == nPrevState)
            {
            try (Sentry<?> ignored = f_gateState.close())
                {
                if (m_nState == nPrevState)
                    {
                    m_nState = nState;
                    notifyStateChange(nState, nPrevState);
                    return true;
                    }
                }
            }
        return false;
        }

    private void notifyStateChange(int nState, int nPrevState)
        {
        for (EventListener listener : f_stateListeners.listeners())
            {
            try
                {
                ((StateListener) listener).onStateChange(this, nState, nPrevState);
                }
            catch (Throwable t)
                {
                Logger.err(t);
                }
            }
        }

    /**
     * Add a {@link StateListener state change listener}.
     *
     * @param listener  the {@link StateListener listener} to add
     */
    public void addStateListener(StateListener listener)
        {
        f_stateListeners.add(listener);
        }

    /**
     * Remove a previously added {@link StateListener state change listener}.
     *
     * @param listener  the {@link StateListener listener} to remove
     */
    @SuppressWarnings("unused")
    public void removeStateListener(StateListener listener)
        {
        f_stateListeners.remove(listener);
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
     * Reconnect this subscriber if not connected and there are pending receive requests.
     */
    protected void reconnectInternal()
        {
        if (getState() == STATE_CONNECTED || !isActive())
            {
            // nothing to do, either the subscriber is connected, or it is closed
            return;
            }

        try {
            if (m_caches.getService().isSuspended())
                {
                Logger.finest("Skipping reconnect task, service is suspended for subscriber " + this);
                }
            else
                {
                if (f_queueReceiveOrders.size() > 0)
                    {
                    Logger.finest("Running reconnect task, reconnecting " + this);
                    ensureConnected();
                    f_queueReceiveOrders.triggerOperations();
                    }
                else
                    {
                    Logger.finest("Skipping reconnect task, no pending receives for subscriber " + this);
                    }
                }
            }
        catch (Throwable t)
            {
            Logger.finest("Failed to reconnect subscriber " + this, t);
            }
        }

    /**
     * Ensure that the subscriber is connected.
     */
    protected void ensureConnected()
        {
        if (isActive() && m_nState != STATE_CONNECTED)
            {
            try (Sentry<?> ignored = f_gate.close())
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
                                Logger.finer("Skipping ensureConnected, service is suspended " + this);
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
                    if (error == null)
                        {
                        f_queueReceiveOrders.triggerOperations();
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
     * Returns {@code true} if this subscriber is connected to the topic.
     *
     * @return {@code true} if this subscriber is connected to the topic
     */
    public boolean isConnected()
        {
        return m_nState == STATE_CONNECTED;
        }

    /**
     * Disconnect this subscriber.
     * <p>
     * This will cause the subscriber to re-initialize itself on re-connection.
     */
    public void disconnect()
        {
        disconnectInternal(false);
        }

    /**
     * Disconnect this subscriber.
     * <p>
     * This will cause the subscriber to re-initialize itself on re-connection.
     *
     * @param fForceReconnect  force the subscriber to reconnect
     */
    private void disconnectInternal(boolean fForceReconnect)
        {
        long nTimestamp = m_connectionTimestamp;
        if (isActive())
            {
            // We will loop around until we successfully set the state to be disconnected
            // This is because other threads may change the state
            while (true)
                {
                int nState = m_nState;
                if (nState == STATE_CONNECTED)
                    {
                    // we disconnect under a lock
                    try (Sentry<?> ignored = f_gate.close())
                        {
                        if (m_connectionTimestamp != nTimestamp)
                            {
                            // reconnected since this disconnect was originally called, so just return
                            return;
                            }

                        if (isActive() && casState(nState, STATE_DISCONNECTED))
                            {
                            m_fForceReconnect = fForceReconnect;
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
                    }
                else
                    {
                    if (nState == STATE_DISCONNECTED || casState(nState, STATE_DISCONNECTED))
                        {
                        break;
                        }
                    }
                }
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
        CompletableFuture.runAsync(() -> onChannelPopulatedNotification((int[]) evt.getOldValue()), f_executorChannels);
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
            fWasEmpty = nChannelCurrent < 0 || m_aChannel[nChannelCurrent].isEmpty();
            for (int nChannel : anChannel)
                {
                m_aChannel[nChannel].onChannelPopulatedNotification();
                }
            }

        // If the pre-fetch queue has the empty element at its head, we need to remove it
        CommittableElement element = m_queueValuesPrefetched.peek();
        if (element != null && element.isEmpty())
            {
            m_queueValuesPrefetched.poll();
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
        // ensure we're connected, otherwise nothing to do.
        if (isConnected())
            {
            // Channel operations are done under a lock
            Gate<?> gate = f_gate;
            // Wait to enter the gate
            gate.enter(-1);
            try
                {
                if (m_aChannel == null || !isActive() || !isConnected())
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
                                                  new HeadAdvancer(lHeadAssumed + 1), f_executor,
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
                            }
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
        heartbeat(true);
        }

    /**
     * If this is not an anonymous subscriber send a heartbeat to the server.
     *
     * @param fAsync  {@code true} to invoke the heartbeat processor asynchronously
     */
    private void heartbeat(boolean fAsync)
        {
        if (!f_fAnonymous)
            {
            // we're not anonymous so send a poll heartbeat
            UUID uuid = m_caches.getService().getCluster().getLocalMember().getUuid();
            f_heartbeatProcessor.setUuid(uuid);
            f_heartbeatProcessor.setSubscription(m_subscriptionId);
            f_heartbeatProcessor.setlConnectionTimestamp(m_connectionTimestamp);
            if (fAsync)
                {
                m_caches.Subscribers.async().invoke(f_key, f_heartbeatProcessor);
                }
            else
                {
                m_caches.Subscribers.invoke(f_key, f_heartbeatProcessor);
                }
            }
        }

    private void updateChannelOwnership(SortedSet<Integer> setChannel, boolean fLost)
        {
        if (!isActive())
            {
            return;
            }

        int[] aChannel    = setChannel.stream().mapToInt(i -> i).toArray();
        int   nMaxChannel = setChannel.stream().mapToInt(i -> i).max().orElse(getChannelCount() - 1);

        // channel ownership change must be done under a lock
        try (Sentry<?> ignored = f_gate.close())
            {
            if (!isActive())
                {
                return;
                }

            PagedTopicChannel[] aExistingChannel = m_aChannel;
            if (nMaxChannel >= aExistingChannel.length)
                {
                // This subscriber has fewer channels than the server so needs to be resized
                // We disconnect as the subscription may not be properly initialized for
                // the new channel count if this has happened due to a rolling upgrade
                // from an earlier buggy topics version
                Logger.finer(() -> String.format("Disconnecting subscriber %d on topic %s due to increase in channel count from %d to %d",
                        f_id.getId(), f_topic.getName(), aExistingChannel.length, nMaxChannel));
                disconnectInternal(true);
                return;
                }

            if (!Arrays.equals(m_aChannelOwned, aChannel))
                {
                Set<Integer> setRevoked = new HashSet<>();
                Set<Integer> setAdded   = new HashSet<>(setChannel);
                if (m_aChannelOwned != null && m_aChannelOwned.length > 0)
                    {
                    for (int nChannel : m_aChannelOwned)
                        {
                        setRevoked.add(nChannel);
                        setAdded.remove(nChannel);
                        }
                    setChannel.forEach(setRevoked::remove);
                    }
                setRevoked = Collections.unmodifiableSet(setRevoked);

                Set<Integer> setAssigned = Set.copyOf(setChannel);

                Logger.finest(String.format("Subscriber %d (name=%s) channel allocation changed, assigned=%s added=%s revoked=%s",
                                            f_id.getId(), f_sIdentifyingName, setAssigned, setAdded, setRevoked));

                m_aChannelOwned = aChannel;

                if (!f_fAnonymous)
                    {
                    // reset revoked channel heads - we'll re-sync if they are reallocated

                    // if we're initializing and not anonymous, we do not own any channels,
                    // we'll update with the allocated ownership
                    if (m_nState == STATE_INITIAL)
                        {
                        for (PagedTopicChannel channel : m_aChannel)
                            {
                            channel.setUnowned();
                            channel.setPopulated();
                            }
                        }

                    // clear all channel flags
                    for (PagedTopicChannel channel : m_aChannel)
                        {
                        channel.m_fContended = false;
                        channel.setUnowned();
                        channel.setPopulated();
                        }
                    // reset channel flags for channels we now own
                    for (int c : m_aChannelOwned)
                        {
                        PagedTopicChannel channel = m_aChannel[c];
                        channel.m_fContended = false;
                        channel.setOwned();
                        channel.setPopulated();
                        }
                    for (int c : setAdded)
                        {
                        PagedTopicChannel channel = m_aChannel[c];
                        channel.clearPolled();
                        channel.clearHit();
                        }
                    for (int c : setRevoked)
                        {
                        PagedTopicChannel channel = m_aChannel[c];
                        channel.clearPolled();
                        channel.clearHit();
                        }
                    }

                // if the pre-fetch queue contains the empty marker, we need to remove it
                CommittableElement element = m_queueValuesPrefetched.peek();
                if (element != null && element.isEmpty())
                    {
                    m_queueValuesPrefetched.poll();
                    }

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
                    if (!setAssigned.isEmpty())
                        {
                        try
                            {
                            listener.onChannelsAssigned(setAssigned);
                            }
                        catch (Throwable t)
                            {
                            Logger.err(t);
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
        if (m_nChannel >= 0 && m_aChannel[m_nChannel].isOwned())
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
        if (m_aChannel == null || !isActive() || !isConnected())
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
            if (m_aChannel == null || !isActive() || !isConnected())
                {
                // disconnected or no longer active
                return false;
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
                if (m_aChannel[nChannel].isEmpty())
                    {
                    // our single channel is empty
                    m_nChannel = -1;
                    return false;
                    }
                else
                    {
                    // our single channel is not empty, switch to it
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
            while (nChannel != nChannelStart && cTried < m_aChannel.length && (!m_aChannel[nChannel].isOwned() || m_aChannel[nChannel].isEmpty()))
                {
                cTried++;
                nChannel++;
                if (nChannel == m_aChannel.length)
                    {
                    nChannel = 0;
                    }
                }

            if (m_aChannel[nChannel].isOwned() && !m_aChannel[nChannel].isEmpty())
                {
                m_nChannel = nChannel;
                return true;
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

        f_receiveLock.lock();
        try
            {
            // check that there is no error, and we still own the channel
            if (e == null )
                {
                Queue<Binary> queueValues = result.getElements();
                int           cReceived   = queueValues.size();
                int           cRemaining  = result.getRemainingElementCount();
                int           nNext       = result.getNextIndex();

                channel.setPolled();
                ++m_cPolls;

                if (cReceived == 0)
                    {
                    ++m_cMisses;
                    }
                else if (!queueValues.isEmpty())
                    {
                    channel.setHit();
                    m_cValues += cReceived;
                    channel.adjustPolls(cReceived);

                    // add the received elements to the pre-fetch queue
                    queueValues.stream()
                            .map(bin -> new CommittableElement(bin, nChannel))
                            .forEach(m_queueValuesPrefetched::add);

                    if (!m_queueValuesPrefetched.isEmpty())
                        {
                        long nTime = System.currentTimeMillis();
                        channel.setFirstPolled((PagedPosition) m_queueValuesPrefetched.getFirst().getPosition(), nTime);
                        channel.setLastPolled((PagedPosition) m_queueValuesPrefetched.getLast().getPosition(), nTime);
                        }
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
                    disconnectInternal(true);
                    }
                }
            else // remove failed; this is fairly catastrophic
                {
                // TODO: figure out error handling
                // fail all currently (and even concurrently) scheduled removes
                f_queueReceiveOrders.handleError((err, bin) -> e, BatchingOperationsQueue.OnErrorAction.CompleteWithException);
                }
            }
        finally
            {
            f_receiveLock.unlock();
            }
        }

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
     * Close and clean-up this subscriber.
     *
     * @param fDestroyed  {@code true} if this call is in response to the caches
     *                    being destroyed/released and hence just clean up local
     *                    state
     */
    private void closeInternal(boolean fDestroyed)
        {
        if (m_nState != STATE_CLOSED)
            {
            try (Sentry<?> ignored = f_gate.close())
                {
                setState(STATE_CLOSING); // accept no new requests, and cause all pending ops to complete ASAP (see onReceiveResult)
                }

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
                    notifyClosed(m_caches.Subscriptions, f_subscriberGroupId, m_subscriptionId, f_id);
                    removeSubscriberEntry();
                    }
                else
                    {
                    notifyClosed(m_caches.Subscriptions, f_subscriberGroupId, m_subscriptionId, f_id);
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
                    destroy(m_caches, f_subscriberGroupId, m_subscriptionId);
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
     * @param lSubscriptionId    the unique identifier of the subscription
     */
    static void notifyClosed(NamedCache<Subscription.Key, Subscription> cache,
            SubscriberGroupId subscriberGroupId, long lSubscriptionId, SubscriberId subscriberId)
        {
        PagedTopicService service = (PagedTopicService) cache.getCacheService();

        if (lSubscriptionId == 0)
            {
            String sTopicName = PagedTopicCaches.Names.getTopicName(cache.getCacheName());
            lSubscriptionId = service.getSubscriptionId(sTopicName, subscriberGroupId);
            }

        PagedTopicSubscription subscription = service.getSubscription(lSubscriptionId);
        if (subscription != null && subscription.hasSubscriber(subscriberId))
            {
            service.destroySubscription(lSubscriptionId, subscriberId);
            }

        if (!cache.isActive())
            {
            // cache is already inactive, so we do not need to do anything
            return;
            }

        try
            {
            int                    cParts       = service.getPartitionCount();
            List<Subscription.Key> listSubParts = new ArrayList<>(cParts);

            for (int i = 0; i < cParts; ++i)
                {
                // Note: we unsubscribe against channel 0 in each partition, and it will in turn update all channels
                listSubParts.add(new Subscription.Key(i, /*nChannel*/ 0, subscriberGroupId));
                }

            if (cache.isActive())
                {
                try
                    {
                    cache.invokeAll(listSubParts, new CloseSubscriptionProcessor(subscriberId));
                    }
                catch (Exception e)
                    {
                    // ignored -
                    }
                }
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
            m_caches.f_topicService.addSubscriptionListener(m_listenerChannelAllocation);
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
            m_caches.f_topicService.removeSubscriptionListener(m_listenerChannelAllocation);
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
    protected void registerMBean()
        {
        MBeanHelper.registerSubscriberMBean(this);
        }

    /**
     * Register the subscriber MBean.
     */
    protected void unregisterMBean()
        {
        MBeanHelper.unregisterSubscriberMBean(this);
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

    /**
     * Return an option to set the identifying name.
     *
     * @param sName  the name to use for the identifier
     *
     * @return an option to set the identifying name
     */
    public static Option withIdentifyingName(String sName)
        {
        return new WithIdentifyingName(sName);
        }

    /**
     * Return an option to set the Subscriber's id.
     * <p/>
     * This option should only be used in testing.
     *
     * @param nId  the Subscriber's id
     *
     * @return an option to set the Subscriber's id
     */
    public static Option withNotificationId(int nId)
        {
        return (WithNotificationId<Object, Object>) () -> nId;
        }

    // ----- inner class: WithIdentifier ------------------------------------

    /**
     * An {@link Option} that provides a human-readable name to a {@link PagedTopicSubscriber}.
     * <p/>
     * This can be useful in testing to make it easier to identify specific subscribers
     * in a log message.
     */
    public static class WithIdentifyingName
            implements Option
        {
        public WithIdentifyingName(String sName)
            {
            f_sName = sName;
            }

        public String getName()
            {
            return f_sName;
            }

        private final String f_sName;
        }

    // ----- inner class: WithIdentifier ------------------------------------

    /**
     * An {@link Option} that provides a notification id to a {@link PagedTopicSubscriber}.
     * <p/>
     * This can be useful in testing to make it easier to control subscribes and channel allocations,
     * it should not be used in production systems.
     */
    public interface WithNotificationId<V, U>
            extends Option<V, U>
        {
        int getId();
        }

    // ----- inner class: CommittableElement --------------------------------

    /**
     * CommittableElement is a wrapper around a {@link PageElement}
     * that makes it committable.
     */
    protected class CommittableElement
        implements Element<V>
        {
        // ----- constructors -----------------------------------------------

        /**
         * Create an element
         *
         * @param binValue  the binary element value
         */
        protected CommittableElement(Binary binValue, int nChannel)
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
        public long getCommitCount()
            {
            return m_cCommited;
            }

        public void committed(PagedPosition position)
            {
            m_lastCommit = position;
            m_cCommited++;
            }

        @Override
        public PagedPosition getLastReceived()
            {
            return m_lastReceived;
            }

        @Override
        public long getReceiveCount()
            {
            return m_cReceived.getCount();
            }

        /**
         * Update the last received position.
         *
         * @param position  the last received position
         */
        public void received(PagedPosition position)
            {
            m_lastReceived = position;
            m_cReceived.mark();
            }

        @Override
        public long getPolls()
            {
            return m_cPolls;
            }

        /**
         * Adjust the polls count.
         *
         * @param c the amount to adjust by
         */
        public void adjustPolls(long c)
            {
            m_cPolls += c;
            }

        @Override
        public Position getFirstPolled()
            {
            return m_firstPolled;
            }

        /**
         * Set the first polled position.
         *
         * @param position  the first polled position
         */
        public void setFirstPolled(PagedPosition position, long nTimestamp)
            {
            if (m_firstPolled == null)
                {
                m_firstPolled          = position;
                m_firstPolledTimestamp = nTimestamp;
                }
            }

        @Override
        public long getFirstPolledTimestamp()
            {
            return m_firstPolledTimestamp;
            }

        @Override
        public Position getLastPolled()
            {
            return m_lastPolled;
            }

        /**
         * Set the last polled position.
         *
         * @param position  the last polled position
         */
        public void setLastPolled(PagedPosition position, long nTimestamp)
            {
            m_lastPolled          = position;
            m_lastPolledTimestamp = nTimestamp;
            }

        @Override
        public long getLastPolledTimestamp()
            {
            return m_lastPolledTimestamp;
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
        public int getOwnedCode()
            {
            return isOwned() ? 1 : 0;
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

        @Override
        public long getReceived()
            {
            return m_cReceived.getCount();
            }

        /**
         * Return the mean rate of receive requests completed.
         *
         * @return the mean rate of receive requests completed
         */
        public double getReceivedMeanRate()
            {
            return m_cReceived.getMeanRate();
            }

        @Override
        public double getReceivedOneMinuteRate()
            {
            return m_cReceived.getOneMinuteRate();
            }

        @Override
        public double getReceivedFiveMinuteRate()
            {
            return m_cReceived.getFiveMinuteRate();
            }

        @Override
        public double getReceivedFifteenMinuteRate()
            {
            return m_cReceived.getFifteenMinuteRate();
            }

        // ----- helper methods ---------------------------------------------

        /**
         * Set this channel as empty only if the channel version matches the specified version.
         *
         * @param lVersion  the channel version to use as a CAS
         */
        protected void setEmpty(long lVersion)
            {
            m_lock.lock();
            try
                {
                if (m_lVersion.get() == lVersion)
                    {
                    m_fEmpty = true;
                    }
                }
            finally
                {
                m_lock.unlock();
                }
            }

        protected long getVersion()
            {
            m_lock.lock();
            try
                {
                return m_lVersion.get();
                }
            finally
                {
                m_lock.unlock();
                }
            }

        protected void setOwned()
            {
            m_fOwned = true;
            }

        protected void setUnowned()
            {
            m_fOwned = false;
            }

        /**
         * Called to notify the channel that a populated notification was received.
         */
        protected void onChannelPopulatedNotification()
            {
            m_cNotify.incrementAndGet();
            setPopulated();
            }

        /**
         * Set this channel as populated and bump the version up by one.
         */
        protected void setPopulated()
            {
            m_lock.lock();
            try
                {
                m_lVersion.incrementAndGet();
                m_fEmpty = false;
                }
            finally
                {
                m_lock.unlock();
                }
            }

        /**
         * Return number of channel populated notifications received.
         *
         * @return number of channel populated notifications received
         */
        public long getNotify()
            {
            return m_cNotify.get();
            }

        /**
         * Set the flag indicating this channel has been polled since ownership was last assigned.
         */
        public void setPolled()
            {
            m_fPolled = true;
            }

        /**
         * Clear the flag indicating this channel has been polled since ownership was last assigned.
         */
        public void clearPolled()
            {
            m_fPolled = false;
            }

        /**
         * @return the flag indicating this channel has been polled since ownership was last assigned.
         */
        public boolean isPolled()
            {
            return m_fPolled;
            }

        /**
         * Set the flag indicating a message has been received from this channel since ownership was last assigned.
         */
        public void setHit()
            {
            m_fHit = true;
            }

        /**
         * Clear the flag indicating a message has been received from this channel since ownership was last assigned.
         */
        public void clearHit()
            {
            m_fHit = false;
            }

        /**
         * @return the flag indicating a message has been received from this channel since ownership was last assigned
         */
        public boolean isHit()
            {
            return m_fHit;
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
                    ", version=" + m_lVersion.get() +
                    ", head=" + m_lHead +
                    ", next=" + m_nNext +
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
         * The current head page for this subscriber, this value may safely be behind (but not ahead) of the actual head.
         * <p>
         * This field is volatile as it is possible it gets concurrently updated by multiple threads if the futures get
         * completed on IO threads. We don't go with a full blow AtomicLong as either value is suitable and worst case
         * we update to an older value and this would be harmless and just get corrected on the next attempt.
         */
        volatile long m_lHead = HEAD_UNKNOWN;

        /**
         * The current version of this channel.
         * <p>
         * This is used for CAS operations on the empty flag.
         */
        AtomicLong m_lVersion = new AtomicLong();

        /**
         * The number of channel populated notifications received.
         */
        AtomicLong m_cNotify = new AtomicLong();

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
         * The position of the last element used to complete a "receive"
         * request {@link CompletableFuture}.
         */
        PagedPosition m_lastReceived;

        /**
         * The number of elements polled by this subscriber.
         */
        long m_cPolls;

        /**
         * The first position received by this subscriber.
         */
        PagedPosition m_firstPolled;

        /**
         * The timestamp when the first element was received by this subscriber.
         */
        long m_firstPolledTimestamp;

        /**
         * The last position received by this subscriber.
         */
        PagedPosition m_lastPolled;

        /**
         * The timestamp when the last element was received by this subscriber.
         */
        long m_lastPolledTimestamp;

        /**
         * The last position successfully committed by this subscriber.
         */
        PagedPosition m_lastCommit;
        /*
         * The number of completed commit requests.
         */
        long m_cCommited;

        /**
         * The counter of completed receives for the channel.
         */
        Meter m_cReceived = new Meter();

        /**
         * A flag indicating this channel has been polled since ownership was last assigned.
         */
        boolean m_fPolled;

        /**
         * A flag indicating a message has been received from this channel since ownership was last assigned.
         */
        boolean m_fHit;

        /**
         * The lock to control state updates.
         */
        private final Lock m_lock = new ReentrantLock();
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
        protected void execute(PagedTopicSubscriber<?> subscriber, BatchingOperationsQueue<Request, ?> queueBatch)
            {
            Map<Integer, Position> map = subscriber.seekInternal(this);
            queueBatch.completeElement(map, this::onRequestComplete);
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
            queue.completeElement(map, this::onRequestComplete);
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
            disconnectInternal(false);
            }

        @Override
        public void onDestroy()
            {
            Logger.finest("Detected destroy of topic "
                                + m_caches.getTopicName() + ", closing subscriber "
                                + PagedTopicSubscriber.this);
            CompletableFuture.runAsync(() -> closeInternal(true), f_executor);
            }

        @Override
        public void onRelease()
            {
            Logger.finest("Detected release of topic "
                                + m_caches.getTopicName() + ", closing subscriber "
                                + PagedTopicSubscriber.this);
            CompletableFuture.runAsync(() -> closeInternal(true), f_executor);
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
                CompletableFuture.runAsync(() -> closeInternal(true), f_executor);
                }
            }
        }

    // ----- inner class: ChannelListener -----------------------------------

    /**
     * A {@link MapListener} that tracks changes to the channels owned by this subscriber.
     */
    protected class ChannelListener
            implements PagedTopicSubscription.Listener
        {
        public ChannelListener(SubscriberId id, PagedTopicSubscription.Key key)
            {
            f_id    = id;
            f_key   = key;
            m_latch = new CountDownLatch(1);
            }

        // ----- PagedTopicSubscription.Listener methods --------------------

        @Override
        public void onUpdate(PagedTopicSubscription subscription)
            {
            if (Objects.equals(subscription.getKey(), f_key))
                {
                f_daemonChannels.executeTask(() -> onChannelAllocation(subscription));
                }
            }

        @Override
        public void onDelete(PagedTopicSubscription subscription)
            {
            if (Objects.equals(subscription.getKey(), f_key))
                {
                f_daemonChannels.executeTask(() -> updateChannelOwnership(PagedTopicSubscription.NO_CHANNELS, true));
                }
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
            return Objects.equals(f_id, that.f_id) && Objects.equals(f_key, that.f_key);
            }

        @Override
        public int hashCode()
            {
            return Objects.hash(f_id, f_key);
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
                updateChannelOwnership(PagedTopicSubscription.NO_CHANNELS, true);
                m_latch = new CountDownLatch(1);
                }
            }

        private void onChannelAllocation(PagedTopicSubscription subscription)
            {
            if (!isActive())
                {
                return;
                }

            SortedSet<Integer> setChannel = null;
            if (subscription.hasSubscriber(f_id))
                {
                setChannel = subscription.getOwnedChannels(f_id);
                }

            if (setChannel != null && setChannel.isEmpty())
                {
                updateChannelOwnership(PagedTopicSubscription.NO_CHANNELS, true);
                }
            else
                {
                if (setChannel != null)
                    {
                    updateChannelOwnership(setChannel, false);
                    m_latch.countDown();
                    }
                else if (isActive() && !f_fAnonymous && isConnected())
                    {
                    Logger.finest("Disconnecting Subscriber (null channel set) " + PagedTopicSubscriber.this);
                    updateChannelOwnership(PagedTopicSubscription.NO_CHANNELS, true);
                    disconnectInternal(false);
                    }
                }
            }

        // ----- data members -----------------------------------------------

        /**
         * A latch that is triggered when channel ownership is initialized.
         */
        private CountDownLatch m_latch;

        /**
         * The subscriber identifier.
         */
        private final SubscriberId f_id;

        /**
         * The subscription key to listen for changes to.
         */
        private final PagedTopicSubscription.Key f_key;
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
            PagedTopicService  topicService   = (PagedTopicService) event.getService();
            int                nMember        = memberIdFromId(nId);
            long               lTimestamp     = info.getConnectionTimestamp();

            if (event.getEntry().isSynthetic())
                {
                Logger.finest(String.format(
                        "Subscriber expired after %d ms - groupId='%s', memberId=%d, notificationId=%d, last heartbeat at %s",
                        info.getTimeoutMillis(), groupId.getGroupName(), nMember, notificationIdFromId(nId), info.getLastHeartbeat()));
                }
            else
                {
                boolean fManual = ((Set<Member>) topicService.getInfo().getServiceMembers()).stream().anyMatch(m -> m.getId() == nMember);
                String  sReason = fManual ? "manual removal of subscriber(s)" : "departure of member " + nMember;
                Logger.finest(String.format(
                        "Subscriber %d in group '%s' removed due to %s",
                        nId, groupId.getGroupName(), sReason));
                }

            SubscriberId           subscriberId    = new SubscriberId(nId, info.getOwningUid());
            long                   lSubscriptionId = topicService.getSubscriptionId(sTopicName, groupId);
            PagedTopicSubscription subscription    = topicService.getSubscription(lSubscriptionId);

            // This is an async event, the subscriber may have already reconnected with a newer timestamp
            if (subscription == null || subscription.getSubscriberTimestamp(subscriberId) <= lTimestamp)
                {
                notifyClosed(topicService.ensureCache(sSubscriptions, null), groupId, lSubscriptionId, subscriberId);
                }
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
        protected ReconnectTask(PagedTopicSubscriber<?> subscriber)
            {
            m_subscriber = subscriber;
            }

        @Override
        public void run()
            {
            m_subscriber.reconnectInternal();
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
        private final PagedTopicSubscriber<?> m_subscriber;

        /**
         * The number of time the task has executed.
         */
        private final AtomicInteger f_cExecution = new AtomicInteger();
        }

    // ----- inner interface StateListener ----------------------------------

    /**
     * Implemented by classes that need to be informed of state changes
     * in this subscriber.
     */
    public interface StateListener
            extends EventListener
        {
        /**
         * The state of the specified subscriber changed.
         *
         * @param subscriber  the {@link PagedTopicSubscriber}
         * @param nNewState   the new state of the subscriber
         * @param nPrevState  the previous state of the subscriber
         */
        void onStateChange(PagedTopicSubscriber<?> subscriber, int nNewState, int nPrevState);
        }

    // ----- constants ------------------------------------------------------

    /**
     * Value of the initial subscriber state.
     */
    public static final int STATE_INITIAL = 0;

    /**
     * Value of the subscriber state when connecting.
     */
    public static final int STATE_CONNECTING = 1;

    /**
     * Value of the subscriber state when connected.
     */
    public static final int STATE_CONNECTED = 2;

    /**
     * Value of the subscriber state when disconnected.
     */
    public static final int STATE_DISCONNECTED = 3;

    /**
     * Value of the subscriber state when closing.
     */
    public static final int STATE_CLOSING = 4;

    /**
     * Value of the subscriber state when closed.
     */
    public static final int STATE_CLOSED = 5;

    /**
     * An array of state names. The indexes match the values of the state constants.
     */
    public static final String[] STATES = {"Initial", "Connecting", "Connected", "Disconnected", "CLosing", "Closed"};

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
    protected final PagedTopicCaches m_caches;

    /**
     * Flag indicating whether this subscriber is part of a group or is anonymous.
     */
    protected final boolean f_fAnonymous;

    /**
     * This subscriber's identifier.
     */
    protected final SubscriberId f_id;

    /**
     * The {@link SubscriberInfo.Key} to use to send heartbeats.
     */
    protected final SubscriberInfo.Key f_key;

    /**
     * The entry processor to use to heartbeat the server.
     */
    protected final SubscriberHeartbeatProcessor f_heartbeatProcessor;

    /**
     * The optional {@link Filter} to use to filter messages.
     */
    protected final Filter<V>   f_filter;

    /**
     * The optional function to use to transform the payload of the message on the server.
     */
    protected final ValueExtractor<V, ?> f_extractor;

    /**
     * The cache's serializer.
     */
    protected final Serializer f_serializer;

    /**
     * The identifier for this {@link PagedTopicSubscriber}.
     */
    protected final SubscriberGroupId f_subscriberGroupId;

    /**
     * The unique identifier for this {@link PagedTopicSubscriber}'s subscriber group.
     */
    protected long m_subscriptionId;

    /**
     * The subscriber's connection timestamp.
     */
    protected volatile long m_connectionTimestamp;

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
     * The {@link Gate} controlling access to the subscriber state.
     */
    private final Gate<?> f_gateState;

    /**
     * The lock to control receive processing.
     */
    private final Lock f_receiveLock = new ReentrantLock();

    /**
     * The state of the subscriber.
     */
    private volatile int m_nState = STATE_INITIAL;

    /**
     * A flag to indicate that the reconnect logic should force a reconnect
     * request even if the subscriber is in the config map.
     */
    private volatile boolean m_fForceReconnect;

    /**
     * Optional queue of prefetched values which can be used to fulfil future receive requests.
     */
    protected final ConcurrentLinkedDeque<CommittableElement> m_queueValuesPrefetched = new ConcurrentLinkedDeque<>();

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
     * The {@link Executor} to execute async operations (this will wrap {@link #f_daemon}).
     */
    private final Executor f_executor;

    /**
     * The daemon used to execute subscriber channel allocation changes.
     */
    protected final TaskDaemon f_daemonChannels;

    /**
     * The {@link Executor} to execute channel operations (this will wrap {@link #f_daemonChannels}).
     */
    private final Executor f_executorChannels;

    /**
     * The listener that receives notifications for non-empty channels.
     */
    @SuppressWarnings("rawtypes")
    private final SimpleMapListener f_listenerNotification;

    /**
     * The listener that will update channel allocations.
     */
    protected final ChannelListener m_listenerChannelAllocation;

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
     * The number of times this subscriber has been notified.
     */
    protected long m_cNotify;

    /**
     * The last value of m_cNotify used within {@link #toString} stats.
     */
    protected long m_cNotifyLast;

    /**
     * The deactivation listener.
     */
    protected final DeactivationListener f_listenerDeactivation = new DeactivationListener();

    /**
     * The NamedCache deactivation listener.
     */
    protected final GroupDeactivationListener m_listenerGroupDeactivation;

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
     * The number of subscribe attempts.
     */
    private final Meter m_cSubscribe = new Meter();

    /**
     * The number of disconnections.
     */
    private final Meter m_cDisconnect = new Meter();

    /**
     * The {@link ReconnectTask} to use to reconnect this subscriber.
     */
    private final ReconnectTask f_taskReconnect;

    /**
     * A human-readable name for this subscriber.
     */
    private final String f_sIdentifyingName;

    /**
     * Listeners for subscriber state changes.
     */
    private final Listeners f_stateListeners = new Listeners();

    /**
     * The count of calls to the {@link #receive()} or {@link #receive(int)} methods.
     */
    private final LongAdder f_cReceiveRequests = new LongAdder();

    /**
     * The count of receive futures that have been cancelled.
     */
    private final LongAdder f_cCancelled = new LongAdder();
    }
