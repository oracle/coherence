/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic;

import com.oracle.coherence.common.base.Exceptions;
import com.oracle.coherence.common.base.Logger;
import com.oracle.coherence.common.base.TimeHelper;

import com.oracle.coherence.common.util.Options;
import com.oracle.coherence.common.util.Sentry;

import com.tangosol.coherence.config.Config;

import com.tangosol.internal.net.DebouncedFlowControl;

import com.tangosol.internal.net.metrics.Meter;

import com.tangosol.internal.net.topic.impl.paged.BatchingOperationsQueue;
import com.tangosol.internal.net.topic.impl.paged.model.PageElement;
import com.tangosol.internal.net.topic.impl.paged.model.PagedTopicSubscription;
import com.tangosol.internal.net.topic.impl.paged.model.SubscriberGroupId;
import com.tangosol.internal.net.topic.impl.paged.model.SubscriberId;
import com.tangosol.internal.net.topic.impl.paged.model.SubscriberInfo;

import com.tangosol.io.Serializer;

import com.tangosol.net.Cluster;
import com.tangosol.net.FlowControl;
import com.tangosol.net.Service;

import com.tangosol.net.management.MBeanHelper;

import com.tangosol.net.topic.NamedTopic;
import com.tangosol.net.topic.Position;
import com.tangosol.net.topic.Subscriber;
import com.tangosol.net.topic.TopicDependencies;
import com.tangosol.net.topic.TopicException;

import com.tangosol.util.Base;
import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.Filter;
import com.tangosol.util.Gate;
import com.tangosol.util.Listeners;
import com.tangosol.util.LongArray;
import com.tangosol.util.ServiceEvent;
import com.tangosol.util.ServiceListener;
import com.tangosol.util.SparseArray;
import com.tangosol.util.TaskDaemon;
import com.tangosol.util.ThreadGateLite;
import com.tangosol.util.ValueExtractor;

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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * A subscriber of values from a paged topic.
 * <p>
 * This class provides the functionality of a {@link Subscriber} and
 * uses a {@link SubscriberConnector} to provides a connection to
 * clustered topic resources.
 *
 * @author Jonathan Knight  2024.11.26
 */
@SuppressWarnings({"rawtypes", "PatternVariableCanBeUsed", "SameParameterValue", "SimplifyStreamApiCallChains"})
public class NamedTopicSubscriber<V>
    implements Subscriber<V>, SubscriberConnector.ConnectedSubscriber<V>, SubscriberStatistics, AutoCloseable
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a {@link NamedTopicSubscriber}.
     *
     * @param topic      the underlying {@link NamedTopic} that this subscriber is subscribed to
     * @param connector  the connector to connect to server side resources
     * @param options    the {@link Option}s controlling this {@link NamedTopicSubscriber}
     *
     * @throws NullPointerException if the {@code topic} or {@code caches} parameters are {@code null}
     */
    @SuppressWarnings("unchecked")
    public <T> NamedTopicSubscriber(NamedTopic<?> topic, SubscriberConnector<V> connector, Option<? super T, V>[] options)
        {
        OptionSet<T, V> optionSet = NamedTopicSubscriber.optionsFrom(options);

        f_topic           = Objects.requireNonNull(topic);
        f_connector       = connector;
        f_sTopicName      = topic.getName();
        f_gate            = new ThreadGateLite<>();
        f_gateState       = new ThreadGateLite<>();
        f_converter       = new SerializerValueConverter<>(topic.getService().getSerializer());
        f_subscriberId    = connector.getSubscriberId();
        f_nNotificationId = f_subscriberId.getNotificationId();

        long nId = f_subscriberId.getId();

        f_subscriberGroupId         = connector.getSubscriberGroupId();
        f_fAnonymous                = f_subscriberGroupId.isAnonymous();
        f_fCompleteOnEmpty          = optionSet.isCompleteOnEmpty();
        f_key                       = new SubscriberInfo.Key(f_subscriberGroupId, nId);
        f_filter                    = optionSet.getFilter().orElse(null);
        f_extractor                 = optionSet.getExtractor().orElse(null);
        m_aChannelOwnershipListener = optionSet.getChannelListeners();
        f_anManualChannel           = optionSet.getSubscribeTo();

        WithIdentifyingName withIdentifyingName = optionSet.get(WithIdentifyingName.class);
        f_sIdentifyingName = withIdentifyingName == null ? null : withIdentifyingName.getName();

        f_taskReconnect    = new ReconnectTask(this);
        f_daemon           = new TaskDaemon("Topic:Subscriber:" + f_sTopicName + ":" + nId);
        f_executor         = new TaskDaemon("Topic:Subscriber:" + f_sTopicName + ":Receive:" + nId);
        f_daemonChannels   = new TaskDaemon("Topic:Subscriber:" + f_sTopicName + ":Channels:" + nId);
        f_executorChannels = f_daemonChannels::executeTask;

        f_daemon.start();
        f_daemonChannels.start();

        Service service  = topic.getService();
        Cluster cluster  = service.getCluster();
        long    cBacklog = cluster.getDependencies().getPublisherCloggedCount();
        f_backlog            = new DebouncedFlowControl((cBacklog * 2) / 3, cBacklog);
        f_queueReceiveOrders = new BatchingOperationsQueue<>(this::trigger, 1,
                                        f_backlog, v -> 1, BatchingOperationsQueue.Executor.fromTaskDaemon(f_daemon));

        topic.getTopicService().addServiceListener(f_serviceStartListener);
        f_connector.addListener(new Listener());
        f_connector.postConstruct(this);

        int cChannel = f_topic.getChannelCount();
        initializeChannels(cChannel);
        registerMBean();
        ensureConnected();

        // Note: post construction this implementation must be fully async
        }

    // ----- PagedTopicSubscriber methods -----------------------------------

    @Override
    public String getTypeName()
        {
        return f_connector.getTypeName();
        }

    /**
     * Returns the subscriber's identifier.
     *
     * @return the subscriber's identifier
     */
    @Override
    public long getId()
        {
        return getSubscriberId().getId();
        }

    /**
     * Returns the subscriber's unique identifier.
     *
     * @return the subscriber's unique identifier
     */
    @Override
    public SubscriberId getSubscriberId()
        {
        return f_subscriberId;
        }

    /**
     * Returns the unique identifier of the subscribe group,
     * or zero if the subscriber is anonymous.
     *
     * @return the unique identifier of the subscribe group,
     *         or zero if the subscriber is anonymous
     */
    @Override
    public long getSubscriptionId()
        {
        return f_connector.getSubscriptionId();
        }

    /**
     * Return the timestamp of the last time the subscriber connected.
     *
     * @return the timestamp of the last time the subscriber connected
     */
    public long getConnectionTimestamp()
        {
        return f_connector.getConnectionTimestamp();
        }

    /**
     * Returns the subscriber's optional identifying name.
     *
     * @return the subscriber's unique identifying name
     */
    @Override
    public String getIdentifyingName()
        {
        return f_sIdentifyingName;
        }

    @Override
    public int getNotificationId()
        {
        return f_nNotificationId;
        }

    @Override
    public SubscriberInfo.Key getKey()
        {
        return f_key;
        }

    @Override
    public boolean isAnonymous()
        {
        return f_fAnonymous;
        }

    @Override
    public long getBacklog()
        {
        return f_backlog.getBacklog();
        }

    @Override
    public long getMaxBacklog()
        {
        return f_backlog.getExcessiveLimit();
        }

    @Override
    public Filter<V> getFilter()
        {
        return f_filter;
        }

    @Override
    public ValueExtractor<V, ?> getConverter()
        {
        return f_extractor;
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
        CompletableFuture<List<Element<V>>> future = (CompletableFuture<List<Element<V>>>) 
                f_queueReceiveOrders.add(new ReceiveRequest(true, cBatch));

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

            return optional.map(committableElement -> (Element<V>) committableElement.getElement())
                    .or(() -> Optional.ofNullable(f_connector.peek(nChannel, position)));
            }
        return Optional.empty();
        }

    @Override
    public CompletableFuture<CommitResult> commitAsync(int nChannel, Position position)
        {
        ensureActive();
        try
            {
            return commitInternal(nChannel, position, null);
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
        Map<Integer, Position>      mapCommit = new HashMap<>();

        for (Map.Entry<Integer, Position> entry : mapPositions.entrySet())
            {
            Integer  nChannel = entry.getKey();
            Position position = entry.getValue();
            mapCommit.put(nChannel, position);
            }

        CompletableFuture<CommitResult>[] aFuture = mapCommit.entrySet()
                .stream()
                .map(e -> commitInternal(e.getKey(), e.getValue(), mapResult))
                .toArray(CompletableFuture[]::new);

        return CompletableFuture.allOf(aFuture).thenApply(_void -> mapResult);
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
        return f_connector.isCommitted(f_subscriberGroupId, nChannel, position);
        }

    @Override
    public int[] getChannels()
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
                            .filter(TopicChannel::isOwned)
                            .mapToInt(Channel::getId)
                            .toArray();
                    }
                }
            finally
                {
                gate.exit();
                }
            }
        return NO_CHANNELS;
        }

    /**
     * Update the specified channel under the channel lock.
     *
     * @param nChannel  the channel to update
     * @param fn        the function to apply to update the channel
     */
    @Override
    public void updateChannel(int nChannel, Consumer<TopicChannel> fn)
        {
        applyToChannel(nChannel, channel ->
            {
            fn.accept(channel);
            return null;
            });
        }

    /**
     * Update the specified channel under the channel lock.
     *
     * @param nChannel  the channel to update
     * @param fn        the function to apply to update the channel
     * @param <R>       the type of the result
     *
     * @return the result of invoking the function
     */
    @SuppressWarnings("UnusedReturnValue")
    public <R> R applyToChannel(int nChannel, Function<TopicChannel, R> fn)
        {
        return fn.apply(m_aChannel[nChannel]);
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
                            .filter(TopicChannel::isOwned)
                            .map(Channel::getId)
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
            if (m_aChannel == null)
                {
                return false;
                }
            return nChannel >= 0 && nChannel < m_aChannel.length && m_aChannel[nChannel].isOwned();
            }
        return false;
        }

    @Override
    public int getChannelCount()
        {
        if (m_nState != STATE_CONNECTED)
            {
            return f_topic.getChannelCount();
            }
        TopicChannel[] aChannel = m_aChannel;
        return  aChannel == null ? f_topic.getChannelCount() : aChannel.length;
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
                f_queueReceiveOrders.addFirst(SeekRequest.position(Collections.singletonMap(nChannel, position)));

        Map<Integer, Position> map = future.join();
        return map.get(nChannel);
        }

    @Override
    @SuppressWarnings("unchecked")
    public Map<Integer, Position> seek(Map<Integer, Position> mapPosition)
        {
        ensureActive();
        CompletableFuture<Map<Integer, Position>> future = (CompletableFuture<Map<Integer, Position>>)
                f_queueReceiveOrders.addFirst(SeekRequest.position(mapPosition));

        return future.join();
        }

    @Override
    @SuppressWarnings("unchecked")
    public Position seek(int nChannel, Instant timestamp)
        {
        ensureActive();
        CompletableFuture<Map<Integer, Position>> future = (CompletableFuture<Map<Integer, Position>>)
                f_queueReceiveOrders.addFirst(SeekRequest.instant(Map.of(nChannel, timestamp)));

        return future.join().get(nChannel);
        }

    @SuppressWarnings("unchecked")
    public Map<Integer, Position> seekToTimestamps(Map<Integer, Instant> map)
        {
        ensureActive();
        CompletableFuture<Map<Integer, Position>> future = (CompletableFuture<Map<Integer, Position>>)
                f_queueReceiveOrders.addFirst(SeekRequest.instant(map));

        return future.join();
        }

    @Override
    @SuppressWarnings("unchecked")
    public Map<Integer, Position> seekToHead(int... anChannel)
        {
        ensureActiveAnOwnedChannels(anChannel);
        return ((CompletableFuture<Map<Integer, Position>>)
                f_queueReceiveOrders.addFirst(SeekRequest.head(anChannel))).join();
        }

    @Override
    @SuppressWarnings("unchecked")
    public Map<Integer, Position> seekToTail(int... anChannel)
        {
        ensureActiveAnOwnedChannels(anChannel);
        return ((CompletableFuture<Map<Integer, Position>>)
                f_queueReceiveOrders.addFirst(SeekRequest.tail(anChannel))).join();
        }

    @Override
    public int getRemainingMessages()
        {
        int[] anChannel = getChannels();
        return f_connector.getRemainingMessages(f_subscriberGroupId, anChannel);
        }

    @Override
    public int getRemainingMessages(int nChannel)
        {
        if (isOwner(nChannel))
            {
            return f_connector.getRemainingMessages(f_subscriberGroupId, new int[]{nChannel});
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

        TopicChannel[] aChannel        = m_aChannel;
        long           cChannelsPolled = Arrays.stream(aChannel).filter(TopicChannel::isPolled).count();
        String         sChannelsPolled = Arrays.toString(Arrays.stream(aChannel).filter(TopicChannel::isPolled).mapToInt(TopicChannel::getId).toArray());
        long           cChannelsHit    = Arrays.stream(aChannel).filter(TopicChannel::isHit).count();
        String         sChannelsHit    = Arrays.toString(Arrays.stream(aChannel).filter(TopicChannel::isHit).mapToInt(TopicChannel::getId).toArray());

        String  sState     = getStateName();
        String  sName      = f_sIdentifyingName == null ? "" : ", name=" + f_sIdentifyingName;

        return getClass().getSimpleName() + "(" + "topic=" + f_sTopicName + sName +
            ", id=" + f_subscriberId +
            ", group=" + f_subscriberGroupId +
            ", subscriptionId=" + f_connector.getSubscriptionId() +
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

    protected TopicChannel[] initializeChannels(int cChannel)
        {
        // must alter channels under a lock
        try (Sentry<?> ignored = f_gate.close())
            {
            TopicChannel[] aExisting = m_aChannel;
            if (aExisting != null && aExisting.length >= cChannel)
                {
                return aExisting;
                }
    
            TopicChannel[] aChannel = new TopicChannel[cChannel];
            for (int nChannel = 0; nChannel < cChannel; nChannel++)
                {
                if (aExisting != null && nChannel < aExisting.length)
                    {
                    aChannel[nChannel] = aExisting[nChannel];
                    }
                else
                    {
                    aChannel[nChannel] = f_connector.createChannel(this, nChannel);
                    }
                }

            if (f_anManualChannel != null && f_anManualChannel.length > 0)
                {
                for (TopicChannel channel : aChannel)
                    {
                    channel.setUnowned();
                    }
                for (int c : f_anManualChannel)
                    {
                    aChannel[c].setOwned();
                    }
                }

            m_aChannel = aChannel;
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
    @Override
    public Channel getChannel(int nChannel)
        {
        TopicChannel[] aChannel = m_aChannel;
        return nChannel >= 0 && nChannel < aChannel.length ? aChannel[nChannel] : new Channel.EmptyChannel(nChannel);
        }

    /**
     * Return the number of cancelled receive requests.
     *
     * @return the number of cancelled receive requests
     */
    @Override
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
    @Override
    public long getReceived()
        {
        return m_cReceived.getCount();
        }

    /**
     * Return the mean rate of completed receive requests.
     *
     * @return the mean rate of completed receive requests
     */
    @Override
    public double getReceivedMeanRate()
        {
        return m_cReceived.getMeanRate();
        }

    /**
     * Return the one-minute rate of completed receive requests.
     *
     * @return the one-minute rate of completed receive requests
     */
    @Override
    public double getReceivedOneMinuteRate()
        {
        return m_cReceived.getOneMinuteRate();
        }

    /**
     * Return the five-minute rate of completed receive requests.
     *
     * @return the five-minute rate of completed receive requests
     */
    @Override
    public double getReceivedFiveMinuteRate()
        {
        return m_cReceived.getFiveMinuteRate();
        }

    /**
     * Return the fifteen-minute rate of completed receive requests.
     *
     * @return the fifteen-minute rate of completed receive requests
     */
    @Override
    public double getReceivedFifteenMinuteRate()
        {
        return m_cReceived.getFifteenMinuteRate();
        }

    /**
     * Return the number of receive requests completed empty.
     * <p>
     * This wil only apply to subscribers using the {@link CompleteOnEmpty}
     * option.
     *
     * @return the number of receive requests completed empty
     */
    @Override
    public long getReceivedEmpty()
        {
        return m_cReceivedEmpty.getCount();
        }

    /**
     * Return the number of exceptionally completed receive requests.
     *
     * @return the number of exceptionally completed receive requests
     */
    @Override
    public long getReceivedError()
        {
        return m_cReceivedError.getCount();
        }

    /**
     * Return the number of disconnections.
     *
     * @return the number of disconnections
     */
    @Override
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
    @Override
    public long getPolls()
        {
        return m_cPolls;
        }

    /**
     * Returns the number of message elements received.
     *
     * @return the number of message elements received
     */
    @Override
    public long getElementsPolled()
        {
        return m_cValues;
        }

    /**
     * Returns the number of times the subscriber has waited on empty channels.
     *
     * @return the number of times the subscriber has waited on empty channels
     */
    @Override
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
    @Override
    public long getNotify()
        {
        return m_cNotify;
        }

    /**
     * Initialise the subscriber.
     */
    private void initialise()
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

                int     nPrevState    = setState(STATE_CONNECTING);
                boolean fDisconnected = nPrevState == STATE_DISCONNECTED;

                if (fReconnect)
                    {
                    Logger.finest("Reconnecting subscriber " + this);
                    }

                try
                    {
                    Position[]     alHead   = f_connector.initialize(this, m_fForceReconnect, fReconnect, fDisconnected);
                    TopicChannel[] aChannel = m_aChannel;

                    int cChannel = alHead.length;
                    if (cChannel > aChannel.length)
                        {
                        // this subscriber has fewer channels than the server so needs to be resized
                        aChannel = initializeChannels(cChannel);
                        }

                    for (int nChannel = 0; nChannel < cChannel; ++nChannel)
                        {
                        TopicChannel channel = aChannel[nChannel];
                        channel.setHead(alHead[nChannel]);
                        channel.setPopulated(); // even if we could infer emptiness here it is unsafe unless we've registered for events
                        }

                    cChannel = aChannel.length;

                    if (f_fAnonymous)
                        {
                        // anonymous so we own all channels
                        SortedSet<Integer> setChannel;
                        if (f_anManualChannel == null || f_anManualChannel.length == 0)
                            {
                            setChannel = new TreeSet<>();
                            for (int i = 0; i < cChannel; i++)
                                {
                                setChannel.add(i);
                                }
                            }
                        else
                            {
                            setChannel = IntStream.of(f_anManualChannel)
                                    .boxed()
                                    .collect(Collectors.toCollection(TreeSet::new));
                            }
                        updateChannelOwnership(setChannel, false);
                        }
                    else
                        {
                        SortedSet<Integer> setChannel = f_connector.getOwnedChannels(this);
                        updateChannelOwnership(setChannel, false);
                        }

                    heartbeat();
                    f_connector.onInitialized(this);
                    if (casState(STATE_CONNECTING, STATE_CONNECTED))
                        {
                        switchChannel();
                        }
                    }
                catch (Throwable t)
                    {
                    // something failed, so assume we are now disconnected
                    setState(STATE_DISCONNECTED);
                    throw Exceptions.ensureRuntimeException(t);
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
        if (isConnected())
            {
            receiveInternal(f_queueReceiveOrders, cBatch);
            }
        else
            {
            f_queueReceiveOrders.resetTrigger();
            }
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
                TopicChannel channel  = m_aChannel[nChannel];
                long         lVersion = channel.getVersion();

                f_connector.receive(this, nChannel, channel.getHead(), lVersion, Integer.MAX_VALUE, (lVersion1, result, e1, continuation) ->
                        onReceiveResult(channel, lVersion1, result, e1, continuation))
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
     * Asynchronously receive a message from the topic.
     *
     * @param nChannel      the channel to receive from
     * @param cMaxElements  the maximum number of elements to receive
     * @param handler       the response handler
     *
     * @return a {@link CompletableFuture} that will complete with the completion of the receive operation
     */
    @Override
    public CompletableFuture<ReceiveResult> receive(int nChannel, int cMaxElements, SubscriberConnector.ReceiveHandler handler)
        {
        ensureConnected();
        TopicChannel channel  = m_aChannel[nChannel];
        Position     head     = channel.getHead();
        long         lVersion = channel.getVersion();
        return f_connector.receive(this, nChannel, head, lVersion, cMaxElements, handler);
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
    protected void complete(BatchingOperationsQueue<Request, ?> queueRequest, LinkedList<Request> queueBatch)
        {
        ConcurrentLinkedDeque<CommittableElement> queuePrefetched = m_queueValuesPrefetched;

        Gate<?> gate         = f_gate;
        Request firstRequest = queueBatch.peek();
        if (firstRequest instanceof FunctionalRequest)
            {
            queueRequest.pause();
            try (Sentry<?> ignored = gate.close())
                {
                while (firstRequest instanceof FunctionalRequest)
                    {
                    Request request = queueBatch.poll();
                    if (request instanceof FunctionalRequest)
                        {
                        ((FunctionalRequest) request).execute(this, queueRequest);
                        }
                    firstRequest = queueBatch.peek();
                    }
                }
            finally
                {
                queueRequest.resume();
                }
            }

        int cValues  = 0;
        int cRequest = queueBatch.size();

        if (isActive() && !queuePrefetched.isEmpty())
            {
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
    @Override
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
        TopicChannel channel = m_aChannel[c];
        if (channel != null && channel.isOwned())
            {
            channel.m_lastReceived = element.getPosition();
            channel.m_cReceived.mark();
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
    private CompletableFuture<CommitResult> commitInternal(int nChannel, Position position, Map<Integer, CommitResult> mapResult)
        {
        try
            {
            TopicChannel channel = m_aChannel[nChannel];
            return f_connector.commit(this, nChannel, position)
                    .thenApply(result ->
                        {
                        if (mapResult != null)
                            {
                            mapResult.put(nChannel, result);
                            }
                        channel.committed(position);
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

    @Override
    public Position updateSeekedChannel(int nChannel, SeekResult result)
        {
        Position positionHead = result.getHead();
        Position seekPosition = result.getSeekPosition();
        if (positionHead != null)
            {
            try (Sentry<?> ignored = f_gate.close())
                {
                TopicChannel channel = m_aChannel[nChannel];
                channel.setHead(positionHead);
                }
            }
        m_queueValuesPrefetched.removeIf(e -> e.getChannel() == nChannel);
        return seekPosition;
        }

    private Map<Integer, Position> seekInternal(SeekRequest request)
        {
        SeekType type      = request.getType();
        int[]    anChannel = request.getChannels();

        ensureActiveAnOwnedChannels(anChannel);

        //noinspection EnhancedSwitchMigration
        switch (type)
            {
            case Head:
                if (anChannel == null || anChannel.length == 0)
                    {
                    return new HashMap<>();
                    }
                Map<Integer, Position> mapSeek = f_connector.getTopicHeads(anChannel);
                return seekInternal(mapSeek);
            case Tail:
                if (anChannel == null || anChannel.length == 0)
                    {
                    return new HashMap<>();
                    }
                Map<Integer, Position> mapTails = f_connector.getTopicTails();
                return seekInternal(filterForChannel(mapTails, anChannel));
            case Position:
                Map<Integer, Position> positions = request.getPositions();
                return positions.isEmpty() ? Collections.emptyMap() : seekInternal(positions);
            case Instant:
                Map<Integer, Instant> instants = request.getInstants();
                return instants.isEmpty() ? Collections.emptyMap() : seekInternalToTimestamps(instants);
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

            Map<Integer, Position> mapSeek = new HashMap<>();
            for (Map.Entry<Integer, Position> entry : mapPosition.entrySet())
                {
                Integer  nChannel = entry.getKey();
                Position position = entry.getValue();
                mapSeek.put(nChannel, position);
                }

            Map<Integer, SeekResult> mapResult = f_connector.seekToPosition(this, mapSeek);
            Map<Integer, Position>   mapSeeked = new HashMap<>();
            for (Map.Entry<Integer, SeekResult> entry : mapResult.entrySet())
                {
                int        nChannel     = entry.getKey();
                Position   seekPosition = updateSeekedChannel(nChannel, entry.getValue());
                mapSeeked.put(nChannel, seekPosition);
                }
            return mapSeeked;
            }
        finally
            {
            // resume receives from the new positions
            f_queueReceiveOrders.resetTrigger();
            }
        }

    private Map<Integer, Position> seekInternalToTimestamps(Map<Integer, Instant> mapInstant)
        {
        ensureActive();

        List<Integer> listUnallocated = mapInstant.keySet().stream()
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
            Map<Integer, SeekResult> mapResult = f_connector.seekToTimestamp(this, mapInstant);
            Map<Integer, Position>   mapSeeked = new HashMap<>();
            for (Map.Entry<Integer, SeekResult> entry : mapResult.entrySet())
                {
                int        nChannel     = entry.getKey();
                Position   seekPosition = updateSeekedChannel(nChannel, entry.getValue());
                mapSeeked.put(nChannel, seekPosition);
                }
            return mapSeeked;
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
            TopicChannel channel = m_aChannel[nChannel];
            mapHeads.put(nChannel, channel.getHead());
            }

        for (CommittableElement element : m_queueValuesPrefetched)
            {
            int nChannel = element.getChannel();
            if (mapHeads.containsKey(nChannel))
                {
                Position positionCurrent = mapHeads.get(nChannel);
                Position position        = element.getPosition();

                if (nChannel != CommittableElement.EMPTY && position != null
                        && !position.isEmpty()
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
        Map<Integer, Position> map       = f_connector.getTopicTails();
        Map<Integer, Position> mapTails  = new HashMap<>();
        int[]                  anChannel = getChannels();
        for (int nChannel : anChannel)
            {
            TopicChannel channel = m_aChannel[nChannel];
            mapTails.put(nChannel, map.getOrDefault(nChannel, channel.getHead()));
            }
        return mapTails;
        }

    private Map<Integer, Position> getLastCommittedInternal()
        {
        ensureActive();
        Map<Integer, Position> mapCommit  = f_connector.getLastCommittedInGroup(f_subscriberGroupId);
        int[]                  anChannels = m_aChannelOwned;
        Map<Integer, Position> mapResult  = new HashMap<>();
        for (int nChannel : anChannels)
            {
            mapResult.put(nChannel, mapCommit.getOrDefault(nChannel, Position.EMPTY_POSITION));
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
    @Override
    public int getState()
        {
        return m_nState;
        }

    /**
     * Returns the state of the subscriber.
     *
     * @return the state of the subscriber
     */
    @Override
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
    @Override
    public void connect()
        {
        ensureActive();
        ensureConnected();
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
            if (f_topic.getService().isSuspended())
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
                TopicDependencies dependencies = f_connector.getTopicDependencies();
                long              retry        = dependencies.getReconnectRetryMillis();
                long              now          = System.currentTimeMillis();
                long              timeout      = now + dependencies.getReconnectTimeoutMillis();
                Throwable         error        = null;

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
                            if (f_topic.getService().isSuspended())
                                {
                                Logger.finer("Skipping ensureConnected, service is suspended " + this);
                                break;
                                }
                            f_connector.ensureConnected();
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
    @Override
    public boolean isConnected()
        {
        return m_nState == STATE_CONNECTED;
        }

    /**
     * Disconnect this subscriber.
     * <p>
     * This will cause the subscriber to re-initialize itself on re-connection.
     */
    @Override
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
    public void disconnectInternal(boolean fForceReconnect)
        {
        long nTimestamp = f_connector.getConnectionTimestamp();
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
                        if (f_connector.getConnectionTimestamp() != nTimestamp)
                            {
                            // reconnected since this disconnect was originally called, so just return
                            return;
                            }

                        if (isActive() && casState(nState, STATE_DISCONNECTED))
                            {
                            m_fForceReconnect = fForceReconnect;
                            m_cDisconnect.mark();
                            // clear out the pre-fetch queue because we have no idea what we'll get on reconnection
                            m_queueValuesPrefetched.clear();

                            TopicDependencies  dependencies = f_connector.getTopicDependencies();
                            long               cWaitMillis  = dependencies.getReconnectWaitMillis();
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
    @Override
    public SubscriberGroupId getSubscriberGroupId()
        {
        return f_subscriberGroupId;
        }

    /**
     * Notification that one or more channels that were empty now have content.
     *
     * @param nChannel the non-empty channels
     */
    @Override
    public void notifyChannel(int nChannel)
        {
        onChannelPopulatedNotification(new int[]{nChannel});
        }

    /**
     * Notification that one or more channels that were empty now have content.
     *
     * @param anChannel  the non-empty channels
     */
    public void onChannelPopulatedNotification(int[] anChannel)
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
            TopicChannel[] aChannel = m_aChannel;
            if (aChannel == null || !isActive())
                {
                // not initialised yet or no longer active
                return;
                }

            ++m_cNotify;

            int nChannelCurrent  = m_nChannel;
            fWasEmpty = nChannelCurrent < 0 || aChannel[nChannelCurrent].isEmpty();
            for (int nChannel : anChannel)
                {
                aChannel[nChannel].onChannelPopulatedNotification();
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
                TopicChannel[] aChannel = m_aChannel;
                if (aChannel == null || !isActive() || !isConnected())
                    {
                    // not initialised yet or no longer active
                    return;
                    }

                aChannel[nChannel].setEmpty(lVersion);
                }
            finally
                {
                // and finally exit from the gate
                gate.exit();
                }
            }
        }

    /**
     * If this is not an anonymous subscriber send a heartbeat to the server.
     */
    public void heartbeat()
        {
        f_connector.heartbeat(this, true);
        }

    private void updateChannelOwnership(SortedSet<Integer> setChannel, boolean fLost)
        {
        if (!isActive())
            {
            return;
            }

        if (setChannel == null)
            {
            setChannel = Collections.emptySortedSet();
            }

        if (f_anManualChannel != null && f_anManualChannel.length > 0)
            {
            SortedSet<Integer> setManual = IntStream.of(f_anManualChannel)
                    .boxed()
                    .collect(Collectors.toCollection(TreeSet::new));
            setChannel = new TreeSet<>(setChannel);
            setChannel.retainAll(setManual);
            }

        int[] anOwned     = setChannel.stream().mapToInt(i -> i).toArray();
        int   nMaxChannel = setChannel.stream().mapToInt(i -> i).max().orElse(getChannelCount() - 1);

        // channel ownership change must be done under a lock
        try (Sentry<?> ignored = f_gate.close())
            {
            if (!isActive())
                {
                return;
                }

            TopicChannel[] aExistingChannel = m_aChannel;
            if (nMaxChannel >= aExistingChannel.length)
                {
                // This subscriber has fewer channels than the server so needs to be resized
                // We disconnect as the subscription may not be properly initialized for
                // the new channel count if this has happened due to a rolling upgrade
                // from an earlier buggy topics version
                Logger.finer(() -> String.format("Disconnecting subscriber %d on topic %s due to increase in channel count from %d to %d",
                        f_subscriberId.getId(), f_topic.getName(), aExistingChannel.length, nMaxChannel));
                disconnectInternal(true);
                return;
                }

            if (!Arrays.equals(m_aChannelOwned, anOwned))
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
                        f_subscriberId.getId(), f_sIdentifyingName, setAssigned, setAdded, setRevoked));

                m_aChannelOwned = anOwned;

                if (!f_fAnonymous)
                    {
                    // reset revoked channel heads - we'll re-sync if they are reallocated
                    TopicChannel[] aChannel = m_aChannel;

                    // if we're initializing and not anonymous, we do not own any channels,
                    // we'll update with the allocated ownership
                    if (m_nState == STATE_INITIAL)
                        {
                        for (TopicChannel channel : aChannel)
                            {
                            channel.setUnowned();
                            channel.setPopulated();
                            }
                        }

                    // clear all channel flags
                    for (TopicChannel channel : aChannel)
                        {
                        channel.m_fContended = false;
                        channel.setUnowned();
                        channel.setPopulated();
                        }
                    // reset channel flags for channels we now own
                    for (int c : m_aChannelOwned)
                        {
                        TopicChannel channel = aChannel[c];
                        channel.m_fContended = false;
                        channel.setOwned();
                        channel.setPopulated();
                        }
                    for (int c : setAdded)
                        {
                        TopicChannel channel = aChannel[c];
                        channel.clearPolled();
                        channel.clearHit();
                        }
                    for (int c : setRevoked)
                        {
                        TopicChannel channel = aChannel[c];
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
        if (m_nChannel >= 0 && isChannelOwned(m_nChannel))
            {
            return m_nChannel;
            }
        switchChannel();
        return m_nChannel;
        }

    public boolean isChannelOwned(int nChannel)
        {
        if (m_aChannel == null)
            {
            return false;
            }
        return nChannel >= 0 && nChannel < m_aChannel.length && m_aChannel[nChannel].isOwned();
        }

    /**
     * Switch to the next available channel.
     *
     * @return {@code true} if a potentially non-empty channel has been found
     *         or {@code false} iff all channels are known to be empty
     */
    protected boolean switchChannel()
        {
        TopicChannel[] aChannel = m_aChannel;
        if (aChannel == null || !isActive() || !isConnected())
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
            aChannel = m_aChannel;
            if (aChannel == null || !isActive() || !isConnected())
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
                if (aChannel[nChannel].isEmpty())
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
            while (nChannel != nChannelStart && cTried < aChannel.length && (!aChannel[nChannel].isOwned() || aChannel[nChannel].isEmpty()))
                {
                cTried++;
                nChannel++;
                if (nChannel == aChannel.length)
                    {
                    nChannel = 0;
                    }
                }

            if (aChannel[nChannel].isOwned() && !aChannel[nChannel].isEmpty())
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
     * @param channel   the associated channel
     * @param lVersion  the version of the channel prior to the call to receive
     * @param result    the receive result
     * @param e         and exception
     */
    protected void onReceiveResult(TopicChannel channel, long lVersion, ReceiveResult result, Throwable e, SubscriberConnector.Continuation continuation)
        {
        int nChannel = channel.getId();

        f_receiveLock.lock();
        try
            {
            // check that there is no error, and we still own the channel
            if (e == null )
                {
                int                  cRemaining = 0;
                ReceiveResult.Status status     = null;
                try
                    {
                    Queue<Binary>        queueValues = result.getElements();
                    int                  cReceived   = queueValues.size();
                    cRemaining = result.getRemainingElementCount();
                    status     = result.getStatus();

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
                            channel.setFirstPolled(m_queueValuesPrefetched.getFirst().getPosition(), nTime);
                            channel.setLastPolled(m_queueValuesPrefetched.getLast().getPosition(), nTime);
                            }
                        }

                    if (continuation != null)
                        {
                        continuation.onContinue();
                        }

                    if (status == ReceiveResult.Status.Exhausted)
                        {
                        // switch to a new channel since we've exhausted this one
                        switchChannel();
                        }
                    else if (cRemaining == 0 || status == ReceiveResult.Status.NotAllocatedChannel)
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
                    else if (status == ReceiveResult.Status.UnknownSubscriber)
                        {
                        // The subscriber was unknown, possibly due to a persistence snapshot recovery or the topic being
                        // destroyed whilst the poll was in progress.
                        // Disconnect and let reconnection sort us out
                        disconnectInternal(true);
                        }
                    }
                catch (Exception ex)
                    {
                    f_queueReceiveOrders.handleError((err, bin) -> ex, BatchingOperationsQueue.OnErrorAction.CompleteWithException);
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
     * Close and clean-up this subscriber.
     *
     * @param fDestroyed  {@code true} if this call is in response to the caches
     *                    being destroyed/released and hence just clean up local
     *                    state
     */
    public void closeInternal(boolean fDestroyed)
        {
        if (m_nState != STATE_CLOSED && m_nState != STATE_CLOSING)
            {
            try (Sentry<?> ignored = f_gate.close())
                {
                if (m_nState == STATE_CLOSED || m_nState == STATE_CLOSING)
                    {
                    // some other thread got in here first
                    return;
                    }
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
                f_connector.closeSubscription(this, fDestroyed);
                f_topic.getService().removeServiceListener(f_serviceStartListener);
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
                f_executor.stop(true);
                f_connector.close();
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
        String sDescription = null;
        switch (mode)
            {
            case FLUSH_DESTROY:
                sDescription = "Topic " + f_sTopicName + " was destroyed";

            case FLUSH_CLOSE_EXCEPTIONALLY:
                String sReason = sDescription != null
                        ? sDescription
                        : "Force Close of Subscriber " + f_subscriberId + " for topic " + f_sTopicName;

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
            Binary binValue   = f_converter.toBinary(null);
            Binary binElement = PageElement.toBinary(-1, 0L, 0, 0L, binValue);
            m_elementEmpty = new CommittableElement(binElement, CommittableElement.EMPTY);
            }
        return m_elementEmpty;
        }

    public void onChannelAllocation(SortedSet<Integer> setChannel, boolean fLost)
        {
        f_daemonChannels.executeTask(() -> updateChannelOwnership(setChannel, fLost));
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
                .map(NamedTopicSubscriber::idToString)
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
                .map(NamedTopicSubscriber::idToString)
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

    @Override
    public Executor getExecutor()
        {
        return f_executor;
        }

    @Override
    public SubscriberConnector<V> getConnector()
        {
        return f_connector;
        }

    public Position getChannelHead(int nChannel)
        {
        if (nChannel >= 0 && nChannel < m_aChannel.length)
            {
            TopicChannel channel = m_aChannel[nChannel];
            return channel.getHead();
            }
        return Position.EMPTY_POSITION;
        }

    @Override
    public void setChannelHeadIfHigher(int nChannel, Position head)
        {
        TopicChannel channel = m_aChannel[nChannel];
        channel.setHeadIfHigher(head);
        }

    @Override
    public Element<V> createElement(Binary binary, int nChannel)
        {
        return new CommittableElement(binary, nChannel);
        }

// ----- inner class: WithIdentifier ------------------------------------

    /**
     * An {@link Option} that provides a human-readable name to a {@link NamedTopicSubscriber}.
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
     * An {@link Option} that provides a notification id to a {@link NamedTopicSubscriber}.
     * <p/>
     * This can be useful in testing to make it easier to control subscribes and channel allocations,
     * it should not be used in production systems.
     */
    public interface WithNotificationId<V, U>
            extends Option<V, U>
        {
        int getId();
        }

    // ----- inner class: WithIdentifier ------------------------------------

    /**
     * An {@link Option} that provides a {@link SubscriberId} to a {@link NamedTopicSubscriber}.
     */
    public interface WithSubscriberId<V, U>
            extends Option<V, U>
        {
        /**
         * Return the {@link SubscriberId} to use
         *
         * @param nNotificationId  the notification id
         *
         * @return the {@link SubscriberId} to use
         */
        SubscriberId getId(int nNotificationId);
        }

    // ----- inner class OptionSet ------------------------------------------

    /**
     * A holder of subscriber options.
     *
     * @param <V>  the type of value in the underlying topic
     * @param <U>  the type of value the subscriber receives
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static class OptionSet<V, U>
            extends Options<Option<V, U>>
        {
        /**
         * Create an option set.
         *
         * @param clsType   the type of the options
         * @param aOptions  the array of options
         */
        private OptionSet(Class<Option<V, U>> clsType, Option<V, U>[] aOptions)
            {
            super(clsType, aOptions);
            }

        /**
         * Return the subscriber group name.
         *
         * @return the subscriber group name
         */
        public Optional<String> getSubscriberGroupName()
            {
            Subscriber.Name nameOption = get(Subscriber.Name.class, null);
            if (nameOption == null)
                {
                return Optional.empty();
                }
            String sName = nameOption.getName();
            return sName == null || sName.isBlank() ? Optional.empty() : Optional.of(sName);
            }

        /**
         * Return the subscriber group identifier.
         *
         * @return the subscriber group identifier
         */
        public SubscriberGroupId getSubscriberGroupId()
            {
            return getSubscriberGroupName().map(SubscriberGroupId::withName)
                    .orElse(SubscriberGroupId.anonymous());
            }

        /**
         * Return the optional {@link Filter} for the subscriber group
         *
         * @return the optional {@link Filter} for the subscriber group
         */
        public Optional<Filter<U>> getFilter()
            {
            Subscriber.Filtered filtered = get(Subscriber.Filtered.class);
            return filtered == null ? Optional.empty() : Optional.ofNullable(filtered.getFilter());
            }

        /**
         * Return the optional {@link ValueExtractor} for the subscriber group
         *
         * @return the optional {@link ValueExtractor} for the subscriber group
         */
        public Optional<ValueExtractor<U, ?>> getExtractor()
            {
            Subscriber.Convert convert = get(Subscriber.Convert.class);
            return convert == null ? Optional.empty() : Optional.ofNullable(convert.getExtractor());
            }

        /**
         * Return {@code true} if the subscriber should complete receive requests when empty.
         *
         * @return {@code true} if the subscriber should complete receive requests when empty
         */
        public boolean isCompleteOnEmpty()
            {
            return contains(Subscriber.CompleteOnEmpty.class);
            }

        /**
         * Return an array of {@link ChannelOwnershipListener} instances.
         *
         * @return an array of {@link ChannelOwnershipListener} instances
         */
        public ChannelOwnershipListener[] getChannelListeners()
            {
            ChannelOwnershipListeners<V> listeners = get(ChannelOwnershipListeners.class);
            if (listeners == null)
                {
                return new ChannelOwnershipListener[0];
                }
            List<ChannelOwnershipListener> list = listeners.getListeners();
            return list.toArray(ChannelOwnershipListener[]::new);
            }

        /**
         * Return the requested channels to be subscribed to.
         *
         * @return  the requested channels to be subscribed to
         */
        public int[] getSubscribeTo()
            {
            SubscribeTo subscribeTo = get(SubscribeTo.class);
            return subscribeTo == null ? SubscribeTo.AUTO.getChannels() : subscribeTo.getChannels();
            }
        }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <V, U> OptionSet<V, U> optionsFrom(Option<? super V, U>[] options)
        {
        Class<?> clsType = Option.class;
        return new OptionSet(clsType, options);
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
            m_element  = PageElement.fromBinary(binValue, f_converter::fromBinary);
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
                return NamedTopicSubscriber.this.commitAsync(getChannel(), getPosition());
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
    public static abstract class TopicChannel
            implements Channel
        {
        @Override
        public Position getLastCommit()
            {
            return m_lastCommit;
            }

        @Override
        public long getCommitCount()
            {
            return m_cCommited;
            }

        public void committed(Position position)
            {
            m_lastCommit = position;
            m_cCommited++;
            }

        @Override
        public Position getLastReceived()
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
        public void received(Position position)
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

        @Override
        public Position getHead()
            {
            return m_head;
            }

        /**
         * Set the first polled position.
         *
         * @param position  the first polled position
         */
        public void setFirstPolled(Position position, long nTimestamp)
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
        public void setLastPolled(Position position, long nTimestamp)
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

        public long getVersion()
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

        public void setHead(Position position)
            {
            m_head = position;
            int nChannel = getId();
            }

        /**
         * Update the head position if the specified head is
         * higher than the current head.
         *
         * @param position  the head position to set.
         */
        public void setHeadIfHigher(Position position)
            {
            m_lock.lock();
            try
                {
                if (m_head == null || m_head.compareTo(position) < 0)
                    {
                    m_head = position;
                    int nChannel = getId();
                    }
                }
            finally
                {
                m_lock.unlock();
                }
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
        public void setPopulated()
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

        // ----- constants --------------------------------------------------

        /**
         * A page id value to indicate that the head page is unknown.
         */
        public static final int HEAD_UNKNOWN = -1;

        // ----- data members -----------------------------------------------

        /**
         * The current version of this channel.
         * <p>
         * This is used for CAS operations on the empty flag.
         */
        protected AtomicLong m_lVersion = new AtomicLong();

        /**
         * The number of channel populated notifications received.
         */
        protected AtomicLong m_cNotify = new AtomicLong();

        /**
         * True if the channel has been found to be empty.  Once identified as empty we don't need to poll form it again
         * until we receive an event indicating that it has seen a new insertion.
         */
        protected volatile boolean m_fEmpty;

        /**
         * True if contention has been detected on this channel.
         */
        protected boolean m_fContended;

        /**
         * True if this subscriber owns this channel.
         */
        protected volatile boolean m_fOwned = true;

        /**
         * The head position.
         */
        protected volatile Position m_head;

        /**
         * The position of the last element used to complete a "receive"
         * request {@link CompletableFuture}.
         */
        protected Position m_lastReceived;

        /**
         * The number of elements polled by this subscriber.
         */
        protected long m_cPolls;

        /**
         * The first position received by this subscriber.
         */
        protected Position m_firstPolled;

        /**
         * The timestamp when the first element was received by this subscriber.
         */
        protected long m_firstPolledTimestamp;

        /**
         * The last position received by this subscriber.
         */
        protected Position m_lastPolled;

        /**
         * The timestamp when the last element was received by this subscriber.
         */
        protected long m_lastPolledTimestamp;

        /**
         * The last position successfully committed by this subscriber.
         */
        protected Position m_lastCommit;

        /**
         * The number of completed commit requests.
         */
        protected long m_cCommited;

        /**
         * The counter of completed receives for the channel.
         */
        protected Meter m_cReceived = new Meter();

        /**
         * A flag indicating this channel has been polled since ownership was last assigned.
         */
        protected boolean m_fPolled;

        /**
         * A flag indicating a message has been received from this channel since ownership was last assigned.
         */
        protected boolean m_fHit;

        /**
         * The lock to control state updates.
         */
        protected final Lock m_lock = new ReentrantLock();
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
        protected abstract void execute(NamedTopicSubscriber<?> subscriber, BatchingOperationsQueue<Request, ?> queue);

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
    public static class SeekRequest
            extends FunctionalRequest
        {
        /**
         * Create a {@link SeekRequest} to seek to the head.
         *
         * @param anChannel  the channels to reposition
         */
        public static SeekRequest head(int... anChannel)
            {
            return new SeekRequest(SeekType.Head, null, null, anChannel);
            }

        /**
         * Create a {@link SeekRequest} to seek to the tail.
         *
         * @param anChannel  the channels to reposition
         */
        public static SeekRequest tail(int... anChannel)
            {
            return new SeekRequest(SeekType.Tail, null, null, anChannel);
            }

        /**
         * Create a {@link SeekRequest}.
         *
         * @param map  a map of {@link Position} keyed by channel to move to
         */
        public static SeekRequest position(Map<Integer, Position> map)
            {
            return new SeekRequest(SeekType.Position, map, null);
            }

        /**
         * Create a {@link SeekRequest}.
         *
         * @param map  the map of channel to {@link Instant} to use to reposition the subscriber
         */
        public static SeekRequest instant(Map<Integer, Instant> map)
            {
            return new SeekRequest(SeekType.Instant, null, map);
            }

        /**
         * Create a {@link SeekRequest}.
         *
         * @param type         the type of the request
         * @param mapPosition  a map of {@link Position} keyed by channel to move to
         * @param anChannel    the channels to reposition
         */
        private SeekRequest(SeekType type, Map<Integer, Position> mapPosition, Map<Integer, Instant> mapInstant, int... anChannel)
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
                    if (mapInstant == null)
                        {
                        throw new IllegalArgumentException("Seek request of type " + type + " require an instant");
                        }
                    break;
                }

            m_type        = type;
            m_mapPosition = mapPosition;
            m_mapInstant  = mapInstant;
            m_anChannel   = anChannel;
            }

        @Override
        protected void execute(NamedTopicSubscriber<?> subscriber, BatchingOperationsQueue<Request, ?> queueBatch)
            {
            Map<Integer, Position> map = subscriber.seekInternal(this);
            queueBatch.completeElement(this, map, this::onRequestComplete);
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
         * Return the map of channel to {@link Instant} to use to reposition the subscriber.
         *
         * @return the map of channel to {@link Instant} to use to reposition the subscriber
         */
        public Map<Integer, Instant> getInstants()
            {
            return m_mapInstant;
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
         * The map of channels to {@link Instant} to use to reposition the subscriber.
         */
        protected final Map<Integer, Instant> m_mapInstant;

        /**
         * The channels to reposition.
         */
        protected final int[] m_anChannel;
        }

    // ----- inner enum: SeekType -------------------------------------------

    /**
     * An enum representing type of {@link SeekRequest}
     */
    public enum SeekType
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
        protected void execute(NamedTopicSubscriber<?> subscriber, BatchingOperationsQueue<Request, ?> queue)
            {
            Map<Integer, Position> map;
            //noinspection EnhancedSwitchMigration
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

    // ----- inner class: Listener ------------------------------------------

    private class Listener
            implements SubscriberConnector.SubscriberListener
        {
        @Override
        public void onEvent(SubscriberConnector.SubscriberEvent evt)
            {
            if (isActive())
                {
                switch (evt.getType())
                    {
                    case GroupDestroyed:
                        if (isActive())
                            {
                            Logger.finest("Detected removal of subscriber group "
                                    + f_subscriberGroupId.getGroupName()
                                    + ", closing subscriber "
                                    + this);
                            CompletableFuture.runAsync(() -> closeInternal(true), f_executor);
                            }
                        break;
                    case ChannelAllocation:
                        onChannelAllocation(evt.getAllocatedChannels(), false);
                        break;
                    case ChannelsLost:
                        onChannelAllocation(PagedTopicSubscription.NO_CHANNELS, true);
                        break;
                    case Unsubscribed:
                        onChannelAllocation(PagedTopicSubscription.NO_CHANNELS, true);
                        disconnectInternal(false);
                        break;
                    case ChannelPopulated:
                        // must use the channel executor
                        CompletableFuture.runAsync(() -> onChannelPopulatedNotification(evt.getPopulatedChannels()), f_executorChannels);
                        break;
                    case Destroyed:
                        Logger.finest("Detected destroy of topic "
                                + f_sTopicName + ", closing subscriber "
                                + this);
                        CompletableFuture.runAsync(() -> closeInternal(true), f_executor);
                        break;
                    case Released:
                        Logger.finest("Detected release of topic "
                                + f_sTopicName + ", closing subscriber "
                                + this);
                        CompletableFuture.runAsync(() -> closeInternal(true), f_executor);
                        break;
                    case Disconnected:
                        disconnectInternal(false);
                        break;
                    default:
                        throw new IllegalStateException("Unexpected event type: " + evt.getType());
                    }
                }
            }
        }

    // ----- inner class: ServiceListener -----------------------------------

    private class ServiceStartListener
            implements ServiceListener
        {
        @Override
        public void serviceStarted(ServiceEvent evt)
            {
            f_executor.execute(() ->
                {
                if (isActive())
                    {
                    ensureConnected();
                    f_queueReceiveOrders.triggerOperations();
                    }
                });
            }

        @Override
        public void serviceResumed(ServiceEvent evt)
            {
            f_executor.execute(() ->
                {
                if (isActive())
                    {
                    ensureConnected();
                    f_queueReceiveOrders.triggerOperations();
                    }
                });
            }

        @Override
        public void serviceStarting(ServiceEvent evt)
            {
            }

        @Override
        public void serviceStopping(ServiceEvent evt)
            {
            }

        @Override
        public void serviceStopped(ServiceEvent evt)
            {
            }
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
        protected ReconnectTask(NamedTopicSubscriber<?> subscriber)
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
        private final NamedTopicSubscriber<?> m_subscriber;

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
         * @param subscriber  the {@link NamedTopicSubscriber}
         * @param nNewState   the new state of the subscriber
         * @param nPrevState  the previous state of the subscriber
         */
        void onStateChange(NamedTopicSubscriber<?> subscriber, int nNewState, int nPrevState);
        }

    // ----- inner class ValueConverter -------------------------------------

    /**
     * A converter to serialize and deserialize values.
     *
     * @param <V>  the type of values to convert
     */
    public interface ValueConverter<V>
        {
        Binary toBinary(V value);

        V fromBinary(Binary binary);
        }

    // ----- inner class SerializerValueConverter ---------------------------

    /**
     * A converter to serialize and deserialize values.
     *
     * @param <V>  the type of values to convert
     */
    public static class SerializerValueConverter<V>
            implements ValueConverter<V>
        {
        public SerializerValueConverter(Serializer serializer)
            {
            f_serializer = serializer;
            }

        @Override
        public Binary toBinary(V value)
            {
            return ExternalizableHelper.toBinary(value, f_serializer);
            }

        @Override
        public V fromBinary(Binary binary)
            {
            return ExternalizableHelper.fromBinary(binary, f_serializer);
            }

        // ----- data members -----------------------------------------------

        private final Serializer f_serializer;
        }

    // ----- inner class WithValueConverter ---------------------------------

    public static class WithValueConverter<V, U>
            implements Option<V, U>
        {
        public WithValueConverter(ValueConverter<U> converter)
            {
            f_converter = Objects.requireNonNull(converter);
            }

        public ValueConverter<U> getConverter()
            {
            return f_converter;
            }

        private final ValueConverter<U> f_converter;
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

    /**
     * A constant empty int array.
     */
    private static final int[] NO_CHANNELS = new int[0];

    // ----- data members ---------------------------------------------------

    /**
     * The connector to connect to server side topic resources.
     */
    private final SubscriberConnector<V> f_connector;

    /**
     * The underlying {@link NamedTopic} being subscribed to.
     */
    protected final NamedTopic<?> f_topic;

    /**
     * The name of the topic.
     */
    protected final String f_sTopicName;

    /**
     * The converter to use to convert binary values.
     */
    protected final ValueConverter<V> f_converter;

//    /**
//     * The unique identifier for this {@link NamedTopicSubscriber}'s subscriber group.
//     */
//    protected long m_subscriptionId;

//    /**
//     * The subscriber's connection timestamp.
//     */
//    protected volatile long m_connectionTimestamp;

    /**
     * The {@link Gate} controlling access to the channel operations.
     */
    protected final Gate<?> f_gate;

    /**
     * The {@link Gate} controlling access to the subscriber state.
     */
    protected final Gate<?> f_gateState;

    /**
     * The lock to control receive processing.
     */
    protected final Lock f_receiveLock = new ReentrantLock();

    /**
     * The state of the subscriber.
     */
    protected volatile int m_nState = STATE_INITIAL;

    /**
     * A flag to indicate that the reconnect logic should force a reconnect
     * request even if the subscriber is in the config map.
     */
    protected volatile boolean m_fForceReconnect;

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
     * The {@link TaskDaemon} to execute async operations (this will wrap {@link #f_daemon}).
     */
    protected final TaskDaemon f_executor;

    /**
     * The daemon used to execute subscriber channel allocation changes.
     */
    protected final TaskDaemon f_daemonChannels;

    /**
     * The {@link Executor} to execute channel operations (this will wrap {@link #f_daemonChannels}).
     */
    protected final Executor f_executorChannels;

    /**
     * The array of {@link ChannelOwnershipListener listeners} to be notified when channel allocations change.
     */
    protected final ChannelOwnershipListener[] m_aChannelOwnershipListener;

    /**
     * The manually assigned channels.
     */
    protected final int[] f_anManualChannel;

    /**
     * The number of poll requests.
     */
    protected       long            m_cPolls;

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

    /**
     * The identifier for the subscriber.
     */
    protected final SubscriberId f_subscriberId;

    /**
     * This subscriber's notification id.
     */
    protected final int f_nNotificationId;
    
    /**
     * Flag indicating whether this subscriber is part of a group or is anonymous.
     */
    protected final boolean f_fAnonymous;

    /**
     * The identifier for this {@link NamedTopicSubscriber}.
     */
    protected final SubscriberGroupId f_subscriberGroupId;

    /**
     * The {@link SubscriberInfo.Key} to use to send heartbeats.
     */
    protected final SubscriberInfo.Key f_key;

    /**
     * The optional {@link Filter} to use to filter messages.
     */
    protected final Filter<V> f_filter;

    /**
     * The optional function to use to transform the payload of the message on the server.
     */
    protected final ValueExtractor<V, ?> f_extractor;

    /**
     * True if configured to complete when empty
     */
    protected final boolean f_fCompleteOnEmpty;

    /**
     * The array of channels.
     */
    protected TopicChannel[] m_aChannel;

    /**
     * The service start listener.
     */
    private final ServiceStartListener f_serviceStartListener = new ServiceStartListener();
    }
