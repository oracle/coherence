/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic.impl.paged;

import com.oracle.coherence.common.base.Exceptions;
import com.oracle.coherence.common.base.Logger;

import com.oracle.coherence.common.collections.ConcurrentHashMap;

import com.tangosol.coherence.config.Config;
import com.tangosol.internal.net.NamedCacheDeactivationListener;

import com.tangosol.internal.net.topic.impl.paged.agent.EnsureSubscriptionProcessor;

import com.tangosol.internal.net.topic.impl.paged.agent.EvictSubscriber;
import com.tangosol.internal.net.topic.impl.paged.filter.UnreadTopicContentFilter;
import com.tangosol.internal.net.topic.impl.paged.model.NotificationKey;
import com.tangosol.internal.net.topic.impl.paged.model.Page;
import com.tangosol.internal.net.topic.impl.paged.model.ContentKey;
import com.tangosol.internal.net.topic.impl.paged.model.PagedPosition;
import com.tangosol.internal.net.topic.impl.paged.model.SubscriberGroupId;
import com.tangosol.internal.net.topic.impl.paged.model.SubscriberId;
import com.tangosol.internal.net.topic.impl.paged.model.SubscriberInfo;
import com.tangosol.internal.net.topic.impl.paged.model.Subscription;
import com.tangosol.internal.net.topic.impl.paged.model.Usage;

import com.tangosol.io.ClassLoaderAware;
import com.tangosol.io.Serializer;

import com.tangosol.net.CacheService;
import com.tangosol.net.DistributedCacheService;
import com.tangosol.net.Member;
import com.tangosol.net.MemberEvent;
import com.tangosol.net.MemberListener;
import com.tangosol.net.NamedCache;

import com.tangosol.net.PagedTopicService;
import com.tangosol.net.cache.TypeAssertion;

import com.tangosol.net.topic.NamedTopic;
import com.tangosol.net.topic.Position;

import com.tangosol.net.topic.Publisher;
import com.tangosol.net.topic.Subscriber;
import com.tangosol.util.AbstractMapListener;
import com.tangosol.util.Aggregators;
import com.tangosol.util.Binary;
import com.tangosol.util.Filter;
import com.tangosol.util.Filters;
import com.tangosol.util.HashHelper;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.InvocableMapHelper;
import com.tangosol.util.MapEvent;
import com.tangosol.util.NullImplementation;
import com.tangosol.util.ValueExtractor;

import com.tangosol.util.aggregator.Count;
import com.tangosol.util.aggregator.GroupAggregator;

import com.tangosol.util.extractor.EntryExtractor;
import com.tangosol.util.extractor.ReflectionExtractor;

import java.io.PrintStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.tangosol.net.cache.TypeAssertion.withTypes;

/**
 * This class encapsulates operations on the set of {@link NamedCache}s
 * that are used to hold the underlying data for a topic.
 *
 * @author jk 2015.06.19
 * @since Coherence 14.1.1
 */
@SuppressWarnings("rawtypes")
public class PagedTopicCaches
    implements ClassLoaderAware, AutoCloseable
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a {@link PagedTopicCaches}.
     *
     * @param sName         the name of the topic
     * @param cacheService  the {@link CacheService} owning the underlying caches
     */
    public PagedTopicCaches(String sName, PagedTopicService cacheService)
        {
        this(sName, cacheService, true);
        }

    /**
     * Create a {@link PagedTopicCaches}.
     *
     * @param sName              the name of the topic
     * @param cacheService       the {@link CacheService} owning the underlying caches
     * @param registerListeners  {@code true} to register listeners
     */
    public PagedTopicCaches(String sName, PagedTopicService cacheService, boolean registerListeners)
        {
        this(sName, cacheService, null, registerListeners);
        }

    /**
     * Create a {@link PagedTopicCaches}.
     *
     * @param sName              the name of the topic
     * @param cacheService       the {@link CacheService} owning the underlying caches
     * @param functionCache      the function to invoke to obtain each underlying cache
     * @param registerListeners  {@code true} to register listeners
     */
    PagedTopicCaches(String sName, PagedTopicService cacheService,
            BiFunction<String, ClassLoader, NamedCache> functionCache,
                     boolean registerListeners)
        {
        if (sName == null || sName.isEmpty())
            {
            throw new IllegalArgumentException("The name argument cannot be null or empty String");
            }

        if (cacheService == null)
            {
            throw new IllegalArgumentException("The cacheService argument cannot be null");
            }

        if (functionCache == null)
            {
            functionCache = cacheService::ensureCache;
            }

        f_sTopicName        = sName;
        f_topicService      = cacheService;
        f_sCacheServiceName = cacheService.getInfo().getServiceName();
        f_cPartition        = cacheService.getPartitionCount();
        f_functionCache     = functionCache;
        f_dependencies      = cacheService.getTopicBackingMapManager().getTopicDependencies(sName);

        initializeCaches(registerListeners);

        m_state = State.Active;
        }

    // ----- TopicCaches methods --------------------------------------

    /**
     * Return the serializer.
     *
     * @return the serializer
     */
    public Serializer getSerializer()
        {
        return f_topicService.getSerializer();
        }

    @Override
    public void close()
        {
        release();
        }

    /**
     * Destroy the PagedTopicCaches.
     */
    public void release()
        {
        releaseOrDestroy(/* destroy */ false);
        }

    /**
     * Destroy the PagedTopicCaches.
     */
    public void destroy()
        {
        releaseOrDestroy(/* destroy */ true);
        }

    /**
     * Determines {@code true} if the caches are active.
     *
     * @return true if the caches are active; false otherwise
     */
    public boolean isActive()
        {
        State state = m_state;
        return state == State.Active || state == State.Disconnected;
        }

    /**
     * Returns {@code true} if the caches are destroyed,
     * specifically the page cache for the topic.
     *
     * @return true if the caches are destroyed; false otherwise
     */
    public boolean isDestroyed()
        {
        return Pages.isDestroyed();
        }

    /**
     * Returns whether the caches are released,
     * specifically the page cache for the topic.
     *
     * @return true if the caches are released; false otherwise
     */
    public boolean isReleased()
        {
        return Pages.isReleased();
        }

    public void addListener(Listener listener)
        {
        m_mapListener.put(listener, Boolean.TRUE);
        }

    public void removeListener(Listener listener)
        {
        m_mapListener.remove(listener);
        }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void ensureConnected()
        {
        if (m_state == State.Disconnected)
            {
            synchronized (this)
                {
                if (m_state == State.Disconnected)
                    {
                    m_state = State.Active;
                    f_setCaches.forEach(NamedCache::size);
                    Set<Listener> setListener = m_mapListener.keySet();
                    for (Listener listener : setListener)
                        {
                        try
                            {
                            listener.onConnect();
                            }
                        catch (Throwable t)
                            {
                            Logger.err(t);
                            }
                        }
                    }
                }
            }
        }

    // ----- ClassLoaderAware methods ---------------------------------------

    @Override
    public ClassLoader getContextClassLoader()
        {
        return f_topicService.getContextClassLoader();
        }

    @Override
    public void setContextClassLoader(ClassLoader classLoader)
        {
        throw new UnsupportedOperationException();
        }

    // ----- accessor methods -----------------------------------------------

    /**
     * Return the topic name.
     *
     * @return the topic name
     */
    public String getTopicName()
        {
        return f_sTopicName;
        }

    /**
     * Get the start page for this topic upon creation.
     *
     * @return the start page
     */
    public int getBasePage()
        {
        return Math.abs(f_sTopicName.hashCode() % f_cPartition);
        }

    /**
     * Return the partition count for this topic.
     *
     * @return the partition count for this topic
     */
    public int getPartitionCount()
        {
        return f_cPartition;
        }

    /**
     * Return the channel count for this topic.
     *
     * @return the channel count for this topic
     */
    public int getChannelCount()
        {
        return f_topicService.getChannelCount(f_sTopicName);
        }

    /**
     * Return the set of NotificationKeys covering all partitions for the given notifier
     *
     * @param nNotifier  the notifier id
     *
     * @return the NotificationKeys
     */
    public Set<NotificationKey> getPartitionNotifierSet(int nNotifier)
        {
        Set<NotificationKey> setKey = new HashSet<>();
        for (int i = 0; i < f_cPartition; ++i)
            {
            setKey.add(new NotificationKey(i, nNotifier));
            }

        return setKey;
        }

    /**
     * Return the unit of order for a topic partition.
     *
     * @param nPartition  the partition
     *
     * @return the unit of order
     */
    public int getUnitOfOrder(int nPartition)
        {
        return f_sTopicName.hashCode() + nPartition;
        }

    /**
     * Return the associated {@link PagedTopicService}.
     *
     * @return the {@link PagedTopicService}
     */
    public PagedTopicService getService()
        {
        return f_topicService;
        }

    /**
     * Return the {@link PagedTopicDependencies}.
     *
     * @return the {@link PagedTopicDependencies}
     */
    public PagedTopicDependencies getDependencies()
        {
        return f_dependencies;
        }

    /**
     * Returns the {@link Usage.Key} for the {@link Usage} entry that tracks the topic's tail for
     * a given channel.
     * <p>
     * We don't just use partition zero as that would concentrate extra load on a single partitions
     * when there are many channels.
     *
     * @param nChannel  the channel number
     *
     * @return the {@link Usage.Key} for the {@link Usage} entry that tracks the topic's head and tail
     */
    public Usage.Key getUsageSyncKey(int nChannel)
        {
        int nPart = Math.abs((HashHelper.hash(f_sTopicName.hashCode(), nChannel) % f_cPartition));
        return new Usage.Key(nPart, nChannel);
        }

    /**
     * Returns {@code true} if a subscriber in the specified group has committed the specified {@link Position}
     * in the specified channel.
     *
     * @param groupId    the subscriber group identifier
     * @param nChannel  the channel identifier
     * @param position  the {@link Position} to check
     *
     * @return {@code true} if a subscriber in the specified group has committed the specified {@link Position}
     *         in the specified channel; or {@code false} if the position is not committed or the subscriber
     *         group does not exist
     */
    public boolean isCommitted(SubscriberGroupId groupId, int nChannel, Position position)
        {
        if (position instanceof PagedPosition && nChannel >= 0 && nChannel < getChannelCount())
            {
            Map<Integer, Position> map          = getLastCommitted(groupId);
            Position               posCommitted = map.get(nChannel);
            return posCommitted != null && posCommitted.compareTo(position) >= 0;
            }
        return false;
        }

    /**
     * Returns a {@link Map} of channel numbers to the latest {@link Position} committed
     * for that channel. If a channel has had zero commits it will be missing
     * from the map's keySet.
     *
     * @param subscriberGroupId  the {@link SubscriberGroupId identifier} of the subscriber
     *                           group to obtain the commits for
     *
     * @return {@code true} if the specified {@link Position} has been committed in the
     *         specified channel
     */
    public Map<Integer, Position> getLastCommitted(SubscriberGroupId subscriberGroupId)
        {
        InvocableMap.EntryAggregator<Subscription.Key, Subscription, Position> aggregatorPos
                = Aggregators.comparableMax(Subscription::getCommittedPosition);

        // Aggregate the subscription commits and remove any null values from the returned map
        return getPositions(subscriberGroupId, aggregatorPos);
        }

    /**
     * Returns a {@link Map} of the {@link Position} of the head for each channel for a subscriber group.
     *
     * @param subscriberGroupId  the {@link SubscriberGroupId identifier} of the subscriber
     *                           group to obtain the heads for
     *
     * @return a {@link Map} of the {@link Position} of the head for each channel for a subscriber group
     */
    @SuppressWarnings("unused")
    public Map<Integer, Position> getHeads(SubscriberGroupId subscriberGroupId, long nSubscriberId)
        {
        InvocableMap.EntryAggregator<Subscription.Key, Subscription, Position> aggregatorPos
                = Aggregators.comparableMin(new Subscription.HeadExtractor(nSubscriberId));

        // Aggregate the subscription commits and remove any null values from the returned map
        return getPositions(subscriberGroupId, aggregatorPos);
        }

    private Map<Integer, Position> getPositions(SubscriberGroupId subscriberGroupId, InvocableMap.EntryAggregator<Subscription.Key, Subscription, Position> aggregator)
        {
        ValueExtractor<Subscription.Key, Integer>           extractorChannel = new ReflectionExtractor<>("getChannelId", new Object[0], EntryExtractor.KEY);
        ValueExtractor<Subscription.Key, SubscriberGroupId> extractorGroup   = new ReflectionExtractor<>("getGroupId", new Object[0], EntryExtractor.KEY);
        Filter<Subscription.Key>                            filter           = Filters.equal(extractorGroup, subscriberGroupId);
        Filter<PagedPosition>                               filterPosition   = Filters.not(Filters.equal(PagedPosition::getPage, Page.NULL_PAGE));

        // Aggregate the subscription commits and remove any null values from the returned map
        return Subscriptions.aggregate(filter, GroupAggregator.createInstance(extractorChannel, aggregator, filterPosition))
                .entrySet()
                .stream()
                .filter(e -> e.getKey() != Page.EMPTY && e.getValue() != null)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }

    /**
     * Returns a {@link Map} of the {@link Position} of the head for each channel.
     *
     * @return a {@link Map} of the {@link Position} of the head for each channel
     */
    public Map<Integer, Position> getHeads()
        {
        ValueExtractor<Page.Key, Integer> extractorChannel
                = new ReflectionExtractor<>("getChannelId", new Object[0], EntryExtractor.KEY);

        InvocableMap.EntryAggregator<Page.Key, Page, Position> aggregatorTail
                = Aggregators.comparableMin(Page.HeadExtractor.INSTANCE);

        return Pages.aggregate(GroupAggregator.createInstance(extractorChannel, aggregatorTail));
        }

    /**
     * Returns a {@link Map} of the {@link Position} of the tail for each channel.
     *
     * @return a {@link Map} of the {@link Position} of the tail for each channel
     */
    public Map<Integer, Position> getTails()
        {
        ValueExtractor<Page.Key, Integer> extractorChannel
                = new ReflectionExtractor<>("getChannelId", new Object[0], EntryExtractor.KEY);

        InvocableMap.EntryAggregator<Page.Key, Page, Position> aggregatorTail
                = Aggregators.comparableMax(Page.TailExtractor.INSTANCE);

        return Pages.aggregate(GroupAggregator.createInstance(extractorChannel, aggregatorTail));
        }

    /**
     * Returns the {@link NamedTopic.ElementCalculator} to use to calculate message sizes.
     *
     * @return the {@link NamedTopic.ElementCalculator} to use to calculate message sizes
     */
    public NamedTopic.ElementCalculator getElementCalculator()
        {
        return getDependencies().getElementCalculator();
        }

    /**
     * Returns the identifiers for all the subscribers belonging to a subscriber group.
     * <p>
     * There is no guarantee that all the subscribers are actually still active. If a subscriber
     * process exits without closing the subscriber, the identifier remains in the cache until it
     * is timed-out.
     *
     * @param sGroupName  the subscriber group name to get subscribers for
     *
     * @return the identifiers for all the subscribers belonging to a subscriber group
     */
    public Set<SubscriberId> getSubscribers(String sGroupName)
        {
        return f_topicService.getSubscribers(f_sTopicName, SubscriberGroupId.withName(sGroupName));
        }

    /**
     * Return the set of {@link Subscriber.Name named} subscriber group(s) and statically configured subscriber-group(s).
     *
     * @return the set of named subscriber groups
     */
    public Set<String> getSubscriberGroups()
        {
        return getSubscriberGroupsIds(false)
                .stream()
                .map(SubscriberGroupId::getGroupName)
                .collect(Collectors.toSet());
        }

    /**
     * Return the set of subscriber group(s) for the topic, optionally including pseudo-groups
     * created for anonymous subscribers.
     *
     * @param fAnonymous  {@code true} to include anonymous subscriber groups
     *
     * @return the set of named subscriber groups
     */
    public Set<SubscriberGroupId> getSubscriberGroupsIds(boolean fAnonymous)
        {
        Stream<SubscriberGroupId> stream = fAnonymous
                ? f_topicService.getSubscriberGroups(f_sTopicName).stream()
                : f_topicService.getSubscriberGroups(f_sTopicName).stream().filter(SubscriberGroupId::isDurable);

        return stream.collect(Collectors.toSet());
        }

    /**
     * Returns an immutable map of subscriber id to channels allocated to that subscriber.
     *
     * @param sGroup  the subscriber group to obtain allocations for
     *
     * @return an immutable map of subscriber id to channels allocated to that subscriber
     */
    public Map<Long, Set<Integer>> getChannelAllocations(String sGroup)
        {
        Subscription.Key key          = new Subscription.Key(0, 0, SubscriberGroupId.withName(sGroup));
        Subscription     subscription = Subscriptions.get(key);
        if (subscription != null)
            {
            return Collections.unmodifiableMap(subscription.getAllocationMap());
            }
        return Collections.emptyMap();
        }

    /**
     * Print the channel allocations for the specified subscriber group.
     *
     * @param sGroup  the name of the subscriber group
     * @param out     the {@link PrintStream} to print to
     */
    public void printChannelAllocations(String sGroup, PrintStream out)
        {
        Map<Integer, String> mapMember = f_topicService.getCluster()
                .getMemberSet()
                .stream()
                .collect(Collectors.toMap(Member::getId, Member::toString));

        out.println("Subscriber channel allocations for topic \"" + f_sTopicName + "\" subscriber group \"" + sGroup + "\":");
        for (Map.Entry<Long, Set<Integer>> entry : getChannelAllocations(sGroup).entrySet())
            {
            long nId     = entry.getKey();
            int  nMember = PagedTopicSubscriber.memberIdFromId(nId);
            out.println("SubscriberId=" + nId + " channels=" + entry.getValue() + " " + mapMember.get(nMember));
            }
        }

    /**
     * Disconnect a specific subscriber from a topic.
     * <p>
     * Disconnecting a subscriber will cause channels to be reallocated and
     * positions to be rolled back to the last commit for the channels
     * owned by the disconnected subscriber.
     *
     * @param groupId  the name of the subscriber group
     * @param id       the subscriber id
     */
    public void disconnectSubscriber(SubscriberGroupId groupId, SubscriberId id)
        {
        Subscribers.invoke(new SubscriberInfo.Key(groupId, id.getId()), EvictSubscriber.INSTANCE);
        }

    /**
     * Disconnect all group subscribers from a topic.
     * <p>
     * Disconnecting subscribers will cause channels to be reallocated and
     * positions to be rolled back to the last commit for the channels
     * owned by the disconnected subscriber.
     *
     * @param sGroup   the name of the subscriber group
     * @param nMember  the cluster member to disconnect subscribers for
     *
     * @return the identifiers of the disconnected subscribers
     */
    public long[] disconnectAllSubscribers(String sGroup, int nMember)
        {
        return disconnectAllSubscribers(SubscriberGroupId.withName(sGroup), nMember);
        }

    /**
     * Disconnect all group subscribers from a topic.
     * <p>
     * Disconnecting subscribers will cause channels to be reallocated and
     * positions to be rolled back to the last commit for the channels
     * owned by the disconnected subscriber.
     *
     * @param id       the {@link SubscriberGroupId id} of the subscriber group
     * @param nMember  the cluster member to disconnect subscribers for
     *
     * @return the identifiers of the disconnected subscribers
     */
    public long[] disconnectAllSubscribers(SubscriberGroupId id, int nMember)
        {
        Filter filter = Filters.equal(SubscriberInfo.GroupIdExtractor.INSTANCE, id)
                .and(Filters.equal(SubscriberInfo.MemberIdExtractor.INSTANCE, nMember));

        Map<SubscriberInfo.Key, Boolean> map = Subscribers.invokeAll(filter, EvictSubscriber.INSTANCE);
        return map.keySet().stream().mapToLong(SubscriberInfo.Key::getSubscriberId).toArray();
        }

    /**
     * Disconnect all subscribers in a group.
     * <p>
     * Disconnecting subscribers will cause channels to be reallocated and
     * positions to be rolled back to the last commit for the channels
     * owned by the disconnected subscriber.
     *
     * @param sGroup  the name of the subscriber group
     *
     * @return the identifiers of the disconnected subscribers
     */
    public long[] disconnectAllSubscribers(String sGroup)
        {
        return disconnectAllSubscribers(SubscriberGroupId.withName(sGroup));
        }

    /**
     * Disconnect all subscribers in a group.
     * <p>
     * Disconnecting subscribers will cause channels to be reallocated and
     * positions to be rolled back to the last commit for the channels
     * owned by the disconnected subscriber.
     *
     * @param id  the {@link SubscriberGroupId id} of the subscriber group
     *
     * @return the identifiers of the disconnected subscribers
     */
    public long[] disconnectAllSubscribers(SubscriberGroupId id)
        {
        Filter<Map.Entry<SubscriberInfo.Key, SubscriberInfo>> filter = Filters.equal(SubscriberInfo.GroupIdExtractor.INSTANCE, id);
        Map<SubscriberInfo.Key, Boolean>                      map    = Subscribers.invokeAll(filter, EvictSubscriber.INSTANCE);
        return map.keySet().stream().mapToLong(SubscriberInfo.Key::getSubscriberId).toArray();
        }

    /**
     * Disconnect all group subscribers from <b>all</b> groups.
     * <p>
     * Disconnecting subscribers will cause channels to be reallocated and
     * positions to be rolled back to the last commit for the channels
     * owned by the disconnected subscriber.
     */
    public void disconnectAllSubscribers()
        {
        for (SubscriberGroupId id : getSubscriberGroupsIds(false))
            {
            long lSubscription = f_topicService.getSubscriptionId(f_sTopicName, id);
            PagedTopicSubscriber.notifyClosed(Subscriptions, id, lSubscription, SubscriberId.NullSubscriber);
            }

        Subscribers.clear();
        }

    /**
     * Ensure the specified subscriber group exists.
     *
     * @param sName      the name of the group
     * @param filter     the filter to use to filter messages received by the group
     * @param extractor  the {@link ValueExtractor} to convert the messages received by the group
     */
    public void ensureSubscriberGroup(String sName, Filter<?> filter, ValueExtractor<?, ?> extractor)
        {
        SubscriberGroupId subscriberGroupId = SubscriberGroupId.withName(sName);
        initializeSubscription(subscriberGroupId, SubscriberId.NullSubscriber, 0L, filter, extractor, false, true, false);
        }

    /**
     * Initialise a subscription.
     *
     * @param subscriberGroupId  the subscriber group identifier
     * @param subscriberId       the subscriber identifier
     * @param filter             the filter to use to filter messages received by the subscription
     * @param extractor          the {@link ValueExtractor} function to convert the messages received by the subscription
     * @param fReconnect         {@code true} if this is a reconnection
     * @param fCreateGroupOnly   {@code true} if this is to only create a subscriber group
     * @param fDisconnected      {@code true} if this is an existing, disconnected subscription
     *
     * @return the pages that are the heads of the channels
     */
    protected long[] initializeSubscription(SubscriberGroupId    subscriberGroupId,
                                            SubscriberId         subscriberId,
                                            long                 lSubscription,
                                            Filter<?>            filter,
                                            ValueExtractor<?, ?> extractor,
                                            boolean              fReconnect,
                                            boolean              fCreateGroupOnly,
                                            boolean              fDisconnected)
        {
        try
            {
            String                sName         = subscriberGroupId.getGroupName();
            Set<Subscription.Key> setSubKeys    = new HashSet<>(f_cPartition);

            if (lSubscription == 0)
                {
                lSubscription = getService().ensureSubscription(f_sTopicName, subscriberGroupId, subscriberId, filter, extractor);
                }

            for (int i = 0; i < f_cPartition; ++i)
                {
                // Note: we ensure against channel 0 in each partition, and it will in turn initialize all channels
                setSubKeys.add(new Subscription.Key(i, /*nChannel*/ 0, subscriberGroupId));
                }

            // outside any lock discover if pages are already pinned.  Note that since we don't
            // hold a lock, this is only useful if the group was already fully initialized (under lock) earlier.
            // Otherwise, there is no guarantee that there isn't gaps in our pinned pages.
            // check results to verify if initialization has already completed
            EnsureSubscriptionProcessor processor = new EnsureSubscriptionProcessor(EnsureSubscriptionProcessor.PHASE_INQUIRE,
                    null, filter,  extractor, subscriberId, fReconnect, fCreateGroupOnly, lSubscription);
            Collection<EnsureSubscriptionProcessor.Result> results;
            if (sName == null)
                {
                results = null;
                }
            else
                {
                CompletableFuture<Map<Subscription.Key, EnsureSubscriptionProcessor.Result>> future
                        = InvocableMapHelper.invokeAllAsync(Subscriptions,
                                                            setSubKeys,
                                                            key -> getUnitOfOrder(key.getPartitionId()), processor);

                try
                    {
                    long cMillis = getDependencies().getSubscriberTimeoutMillis();
                    results = future.get(cMillis, TimeUnit.MILLISECONDS).values();
                    }
                catch (TimeoutException e)
                    {
                    try
                        {
                        future.cancel(true);
                        }
                    catch (Throwable ignored)
                        {
                        // ignored
                        }
                    throw Exceptions.ensureRuntimeException(e);
                    }
                }

            Collection<long[]> colPages = EnsureSubscriptionProcessor.Result.assertPages(results);

            long[] alHead;
            if (colPages == null || colPages.contains(null) || fDisconnected)
                {
                alHead = initialiseSubscriptionPages(subscriberId, lSubscription, filter, extractor,
                                                     fReconnect, fCreateGroupOnly, setSubKeys);
                }
            else
                {
                // all partitions were already initialized, min is our head
                int cChannel = colPages.stream().mapToInt(an -> an.length).max().orElse(getChannelCount());
                alHead = new long[cChannel];
                for (int nChannel = 0; nChannel < cChannel; ++nChannel)
                    {
                    final int finChan = nChannel;
                    alHead[nChannel] = colPages.stream()
                            .mapToLong((alResult) -> alResult[finChan])
                            .min().orElse(Page.NULL_PAGE);
                    }
                }

            return alHead;
            }
        catch (InterruptedException | ExecutionException e)
            {
            if (isActive())
                {
                throw Exceptions.ensureRuntimeException(e);
                }
            return new long[0];
            }
        }

    /**
     * Initialise the head pages for the subscriber.
     *
     * @param subscriberId       the subscriber identifier
     * @param filter             the filter to use to filter messages received by the subscription
     * @param extractor        the converter function to convert the messages received by the subscription
     * @param fReconnect         {@code true} if this is a reconnection
     * @param fCreateGroupOnly   {@code true} if this is to only create a subscriber group
     * @param setSubKeys         the set of {@link Subscription.Key keys} to use to initialise the subscription pages
     *
     * @return an array of head pages
     *
     * @throws InterruptedException if any asynchronous operations are interrupted
     * @throws ExecutionException if any asynchronous operations fail
     */
    @SuppressWarnings("OptionalGetWithoutIsPresent")
    protected long[] initialiseSubscriptionPages(SubscriberId          subscriberId,
                                                 long                  lSubscription,
                                                 Filter<?>             filter,
                                                 ValueExtractor<?, ?>  extractor,
                                                 boolean               fReconnect,
                                                 boolean               fCreateGroupOnly,
                                                 Set<Subscription.Key> setSubKeys)
            throws InterruptedException, ExecutionException
        {
        EnsureSubscriptionProcessor processor
                = new EnsureSubscriptionProcessor(EnsureSubscriptionProcessor.PHASE_PIN, null,
                        filter, extractor, subscriberId, fReconnect, fCreateGroupOnly, lSubscription);

        CompletableFuture<Map<Subscription.Key, EnsureSubscriptionProcessor.Result>> future
                = InvocableMapHelper.invokeAllAsync(Subscriptions,
                                                    setSubKeys,
                                                    key -> getUnitOfOrder(key.getPartitionId()),
                                                    processor);

        Collection<EnsureSubscriptionProcessor.Result> results;
        try
            {
            long cMillis = getDependencies().getSubscriberTimeoutMillis();
            results = future.get(cMillis, TimeUnit.MILLISECONDS).values();
            }
        catch (TimeoutException e)
            {
            try
                {
                future.cancel(true);
                }
            catch (Throwable ignored)
                {
                // ignored
                }
            throw Exceptions.ensureRuntimeException(e);
            }

        Collection<long[]> colPages = EnsureSubscriptionProcessor.Result.assertPages(results);
        int                cChannel = colPages.stream().mapToInt(an -> an.length).max().orElse(getChannelCount());

        PagedTopicDependencies dependencies = getDependencies();
        long                   lPageBase     = getBasePage();
        long[]                 alHead        = new long[cChannel];

        // mapPages now reflects pinned pages
        for (int nChannel = 0; nChannel < cChannel; ++nChannel)
            {
            final int finChan = nChannel;

            if (fReconnect || dependencies.isRetainConsumed())
                {
                // select lowest page in each channel as our channel heads
                alHead[nChannel] = colPages.stream()
                    .mapToLong((alPage) -> alPage.length > finChan ? Math.max(alPage[finChan], lPageBase) : lPageBase)
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
        processor = new EnsureSubscriptionProcessor(EnsureSubscriptionProcessor.PHASE_ADVANCE, alHead, filter,
                extractor, subscriberId, fReconnect, fCreateGroupOnly, lSubscription);

        CompletableFuture<?> futureSub = InvocableMapHelper.invokeAllAsync(Subscriptions, setSubKeys,
                key -> getUnitOfOrder(key.getPartitionId()), processor);

        try
            {
            futureSub.get(30, TimeUnit.SECONDS);
            }
        catch (TimeoutException e)
            {
            throw Exceptions.ensureRuntimeException(e, "Timed out waiting for subscriptions");
            }

        return alHead;
        }

    /**
     * Return an immutable set of all the caches for the topic.
     *
     * @return an immutable set of all the caches for the topic
     */
    public Set<NamedCache> getCaches()
        {
        return Collections.unmodifiableSet(f_setCaches);
        }

    /**
     * Returns the number of remaining messages to be read from the topic for the specific {@link SubscriberGroupId}.
     * <p>
     * This method is a sum of the remaining messages for each channel from the last committed message (exclusive)
     * to the current tail.This result returned by this method is somewhat transient in situations where there are
     * active Subscribers with in-flight commit requests, so the count may change just after the method returns.
     * Message expiry may also affect the returned value, if messages expire after the count is returned.
     *
     * @param id  the {@link SubscriberGroupId}
     *
     * @return  the number of remaining messages for the {@link SubscriberGroupId}
     */
    @SuppressWarnings("unchecked")
    public int getRemainingMessages(SubscriberGroupId id, int... anChannel)
        {
        if (Subscriptions.containsKey(new Subscription.Key(0, 0, id)))
            {
            Map<Integer, Position> mapHeads = getLastCommitted(id);
            Map<Integer, Position> mapTails = getTails();

            for (int i = 0; i < getChannelCount(); i++)
                {
                mapHeads.putIfAbsent(i, new PagedPosition(-1L, -1));
                }

            if (anChannel.length > 0)
                {
                List<Integer> listChannel = IntStream.of(anChannel).boxed().collect(Collectors.toList());
                mapHeads.keySet().retainAll(listChannel);
                mapTails.keySet().retainAll(listChannel);
                }

            InvocableMap.EntryAggregator counter = new Count();
            Binary bin = (Binary) Data.aggregate(new UnreadTopicContentFilter(mapHeads, mapTails), counter);
            return ((Number) f_topicService.getBackingMapManager().getContext()
                    .getValueFromInternalConverter().convert(bin)).intValue();
            }
        // subscriber group does not exist, return the total number of messages
        return Data.size();
        }

    // ----- object methods -------------------------------------------------

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

        PagedTopicCaches that = (PagedTopicCaches) o;

        return f_sTopicName.equals(that.f_sTopicName) && f_topicService.equals(that.f_topicService);
        }

    @Override
    public int hashCode()
        {
        return HashHelper.hash(f_sTopicName, 31);
        }

    @Override
    public String toString()
        {
        return "TopicCaches(name='" + f_sTopicName
                + ", service=" + f_sCacheServiceName
                + ", state=" + m_state
                + ")";
        }

    // ----- helper methods -------------------------------------------------

    @SuppressWarnings("unchecked")
    private void initializeCaches(boolean registerListeners)
        {
        f_topicService.start();

        ClassLoader loader = f_topicService.getContextClassLoader();

        Pages         = f_functionCache.apply(Names.PAGES.cacheNameForTopicName(f_sTopicName), loader);
        Subscribers   = f_functionCache.apply(Names.SUBSCRIBERS.cacheNameForTopicName(f_sTopicName), loader);
        Notifications = f_functionCache.apply(Names.NOTIFICATIONS.cacheNameForTopicName(f_sTopicName), loader);
        Usages        = f_functionCache.apply(Names.USAGE.cacheNameForTopicName(f_sTopicName), loader);
        Subscriptions = f_functionCache.apply(Names.SUBSCRIPTIONS.cacheNameForTopicName(f_sTopicName), loader);
        Data          = f_functionCache.apply(Names.CONTENT.cacheNameForTopicName(f_sTopicName), NullImplementation.getClassLoader());

        Set<NamedCache> setCaches = f_setCaches = new HashSet<>();

        setCaches.add(Pages);
        setCaches.add(Data);
        setCaches.add(Subscriptions);
        setCaches.add(Subscribers);
        setCaches.add(Notifications);
        setCaches.add(Usages);

        if (registerListeners)
            {
            ensureListeners();
            }
        }

    @SuppressWarnings("unchecked")
    private void ensureListeners()
        {
        DeactivationListener listener = m_deactivationListener;
        Pages.addMapListener(listener);
        f_topicService.addMemberListener(listener);
        }

    @SuppressWarnings("unchecked")
    private void removeListeners()
        {
        DeactivationListener listener = m_deactivationListener;
        if (Pages.isActive())
            {
            Pages.removeMapListener(listener);
            }
        f_topicService.removeMemberListener(listener);
        }

    /**
     * Release or destroy the PagedTopicCaches.
     *
     * @param fDestroy  true to destroy, false to release
     */
    private void releaseOrDestroy(boolean fDestroy)
        {
        if (isActive())
            {
            m_state = fDestroy ? State.Destroyed : State.Released;

            Set<Listener> setListener = m_mapListener.keySet();
            for (Listener listener : setListener)
                {
                try
                    {
                    if (fDestroy)
                        {
                        listener.onDestroy();
                        }
                    else
                        {
                        listener.onRelease();
                        }
                    }
                catch (Throwable t)
                    {
                    Logger.err(t);
                    }
                }

            removeListeners();

            if (f_setCaches != null)
                {
                Consumer<NamedCache> function = fDestroy ? this::destroyCache : this::releaseCache;
                if (f_setCaches != null)
                    {
                    f_setCaches.forEach(c ->
                        {
                        if (c.isActive())
                            {
                            function.accept(c);
                            }
                        });
                    f_setCaches = null;
                    }
                }
            }
        }

    private void releaseCache(NamedCache<?, ?> cache)
        {
        if (cache.isActive() && !cache.isReleased())
            {
            f_topicService.releaseCache(cache);
            }
        }

    private void destroyCache(NamedCache<?, ?> cache)
        {
        if (cache.isActive() && !cache.isDestroyed())
            {
            f_topicService.destroyCache(cache);
            }
        }

    void disconnected()
        {
        if (m_state == State.Active)
            {
            synchronized (this)
                {
                if (m_state == State.Active)
                    {
                    m_state = State.Disconnected;
                    Set<Listener> setListener = m_mapListener.keySet();
                    for (Listener listener : setListener)
                        {
                        try
                            {
                            listener.onDisconnect();
                            }
                        catch (Throwable t)
                            {
                            Logger.err(t);
                            }
                        }
                    }
                }
            }
        }

    // ----- inner class: Names ---------------------------------------------

    /**
     * A pseudo enumeration representing the different caches used
     * by topic and topic implementations.
     *
     * @author jk 2015.06.08
     * @since Coherence 14.1.1
     */
    public static class Names<K,V>
        {
        // ----- constructors ---------------------------------------------------

        /**
         * Create a TopicCacheNames with the given cache name prefix.
         *
         * @param sName       the name of this TopicCacheNames.
         * @param sPrefix     the prefix to use to obtain the cache name from the topic name
         * @param classKey    the type of the cache keys
         * @param classValue  the type of the cache values
         */
        private Names(String sName, String sPrefix, Class<K> classKey, Class<V> classValue, Storage storage)
            {
            f_sName         = sName;
            f_sPrefix       = sPrefix;
            f_classKey      = classKey;
            f_classValue    = classValue;
            f_typeAssertion = withTypes(f_classKey, f_classValue);
            f_storage       = storage;

            s_setValues.add(this);
            }

        // ----- TopicCacheNames methods ----------------------------------------

        /**
         * Return the cache name from the specified topic name.
         *
         * @param sTopicName  the topic name
         *
         * @return the cache name
         */
        public String cacheNameForTopicName(String sTopicName)
            {
            return f_sPrefix + sTopicName;
            }

        /**
         * Return the {@link Names} that matches the specified cache name.
         *
         * @param sCacheName  the cache name;
         *
         * @return the {@link Names} that matches the specified cache name
         */
        public static Names fromCacheName(String sCacheName)
            {
            for (Names pagedTopicCacheNames : values())
                {
                if (sCacheName.startsWith(pagedTopicCacheNames.f_sPrefix))
                    {
                    return pagedTopicCacheNames;
                    }
                }

            throw new IllegalArgumentException("Cache name " + sCacheName + " is not a valid TopicCacheName");
            }

        /**
         * For a given cache name return the topic name.
         *
         * @param sCacheName  the cache name
         *
         * @return the topic name
         */
        public static String getTopicName(String sCacheName)
            {
            for (Names pagedTopicCacheNames : values())
                {
                if (sCacheName.startsWith(pagedTopicCacheNames.f_sPrefix))
                    {
                    return sCacheName.substring(pagedTopicCacheNames.f_sPrefix.length());
                    }
                }

            return sCacheName;
            }

        /**
         * Obtain the set of all possible {@link Names}.
         *
         * @return the set of all possible {@link Names}
         */
        public static Set<Names> values()
            {
            return Collections.unmodifiableSet(s_setValues);
            }

        // ----- accessor methods -----------------------------------------------

        /**
         * Obtain the {@link TypeAssertion} to use when obtaining a type safe
         * version of the cache this {@link Names} represents.
         *
         * @return the {@link TypeAssertion} to use when obtaining a type safe
         *         version of the cache this {@link Names} represents
         */
        public TypeAssertion<K,V> getTypeAssertion()
            {
            return f_typeAssertion;
            }

        /**
         * Obtain the prefix used to get the cache name from the
         * topic name.
         *
         * @return the prefix used to get the cache name from the
         *         topic name
         */
        public String getPrefix()
            {
            return f_sPrefix;
            }

        /**
         * Obtain the Class of the cache keys.
         *
         * @return the Class of the cache keys
         */
        public Class<K> getKeyClass()
            {
            return f_classKey;
            }

        /**
         * Obtain the Class of the cache values.
         *
         * @return the Class of the cache values
         */
        public Class<V> getValueClass()
            {
            return f_classValue;
            }

        /**
         * Obtain the storage location for the cache of this type.
         *
         * @return the storage location for the cache of this type
         */
        public Storage getStorage()
            {
            return f_storage;
            }

        /**
         * Return true if corresponding cache should be considered an internal sub-mapping.
         *
         * @return true if corresponding sub cache mapping should be considered internal.
         */
        public boolean isInternal()
            {
            return Storage.MetaData.equals(getStorage());
            }

        /**
         * Return {@code true} if the specified cache name matches this {@link Names}.
         *
         * @param sCacheName  the cache name to test
         *
         * @return {@code true} if the specified cache name matches this {@link Names}
         */
        public boolean isA(String sCacheName)
            {
            return sCacheName != null && sCacheName.startsWith(f_sPrefix);
            }

        // ----- object methods -------------------------------------------------

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

            Names<?, ?> names = (Names<?, ?>) o;

            return f_sName.equals(names.f_sName);

            }

        @Override
        public int hashCode()
            {
            return f_sName.hashCode();
            }

        @Override
        public String toString()
            {
            return f_sName;
            }


        // ----- constants ------------------------------------------------------

        /**
         * The {@link Set} of all legal {@link Names}.
         */
        private static final Set<Names> s_setValues = new HashSet<>();

        /**
         * The name prefix for all meta-data {@link Names} used to implement a {@link NamedTopic}.
         */
        public static final String METACACHE_PREFIX="$meta$topic";

        /**
         * The name prefix for all topic content {@link Names} used to implement a {@link NamedTopic}.
         */
        public static final String CONTENT_PREFIX="$topic";

        /**
         * The cache that holds the topic content.
         * <p>
         * Use of no prefix rather than METACACHE_PREFIX since it is being used to filter out internal topic meta caches.
         */
        public static final Names<ContentKey,Object> CONTENT =
            new Names<>("content", CONTENT_PREFIX + "$", ContentKey.class, Object.class, Names.Storage.Data);

        /**
         * The cache that holds the topic pages.
         */
        public static final Names<Page.Key,Page> PAGES =
            new Names<>("pages", METACACHE_PREFIX +"$pages$", Page.Key.class,
                Page.class, Names.Storage.MetaData);

        /**
         * The cache that holds the topic subscriber partitions.
         */
        public static final Names<Subscription.Key,Subscription> SUBSCRIPTIONS =
            new Names<>("subscriptions", METACACHE_PREFIX + "$subscriptions$",
                Subscription.Key.class, Subscription.class,
                Names.Storage.MetaData);

        /**
         * The cache that holds subscriber information.
         */
        public static final Names<SubscriberInfo.Key,SubscriberInfo> SUBSCRIBERS =
            new Names<>("subscribers", METACACHE_PREFIX + "$subscribers$",
                        SubscriberInfo.Key.class, SubscriberInfo.class,
                        Names.Storage.MetaData);

        /**
         * The cache used for notifying publishers and subscribers of full/empty events
         */
        public static final Names<NotificationKey, int[] /*channels*/> NOTIFICATIONS =
            new Names<>("notifications", METACACHE_PREFIX + "$notifications$",
                NotificationKey.class, int[].class, Names.Storage.MetaData);

        /**
         * The cache that holds usage data for a cache partition.
         */
        public static final Names<Usage.Key, Usage> USAGE =
            new Names<>("usage", METACACHE_PREFIX + "$usage$",
                Usage.Key.class, Usage.class, Names.Storage.Data);

        // ----- inner class: StorageType ---------------------------------------

        public enum Storage {Data, MetaData}

        // ----- data members ---------------------------------------------------

        /**
         * The name of this {@link Names}.
         */
        private final String f_sName;

        /**
         * The prefix to add to the topic name to obtain the cache name.
         */
        private final String f_sPrefix;

        /**
         * The key class of the cache.
         */
        private final Class<K> f_classKey;

        /**
         * The value class of the cache.
         */
        private final Class<V> f_classValue;

        /**
         * The {@link TypeAssertion} to use when obtaining the cache.
         */
        private final TypeAssertion<K,V> f_typeAssertion;

        /**
         * The storage location for the cache data.
         */
        private final Storage f_storage;
        }

    // ----- inner class: DeactivationListener ------------------------------

    /**
     * A listener to detect the topic caches being deactivated due to release, destroy,
     * service shutdown or all storage members departing.
     */
    class DeactivationListener
            extends AbstractMapListener
            implements NamedCacheDeactivationListener, MemberListener
        {
        @Override
        @SuppressWarnings("rawtypes")
        public void entryDeleted(MapEvent evt)
            {
            // destroy/disconnect event
            NamedCache cache      = (NamedCache) evt.getMap();
            boolean    fReleased  = cache.isReleased();
            boolean    fDestroyed = cache.isDestroyed();

            if (fReleased || fDestroyed)
                {
                PagedTopicCaches.this.releaseOrDestroy(fDestroyed);
                }
            }

        @Override
        public void memberLeft(MemberEvent evt)
            {
            DistributedCacheService service = (DistributedCacheService) evt.getService();
            if (evt.isLocal())
                {
                Logger.fine("Detected local member disconnect in service " + PagedTopicCaches.this);
                disconnected();
                }
            else
                {
                service.getOwnershipSenior();
                if (service.getOwnershipEnabledMembers().isEmpty())
                    {
                    Logger.fine("Detected loss of all storage members in service " + PagedTopicCaches.this);
                    disconnected();
                    }
                }
            }

        @Override
        public void memberJoined(MemberEvent evt)
            {
            }

        @Override
        public void memberLeaving(MemberEvent evt)
            {
            }

        @Override
        public boolean equals(Object oThat)
            {
            return oThat instanceof DeactivationListener && hashCode() == oThat.hashCode();
            }

        @Override
        public int hashCode()
            {
            return System.identityHashCode(this);
            }
        }

    // ----- inner interface: Listener --------------------------------------

    /**
     * A listener that can be registered with a {@link PagedTopicCaches} instance
     * to be notified of disconnection events.
     */
    public interface Listener
        {
        /**
         * The caches have been disconnected.
         */
        void onDisconnect();

        /**
         * The caches have been connected.
         */
        void onConnect();

        /**
         * The caches have been destroyed.
         */
        void onDestroy();

        /**
         * The caches have been released.
         */
        void onRelease();
        }

    // ----- inner enum: State ----------------------------------------------

    public enum State
        {
        Active,
        Released,
        Destroyed,
        Disconnected,
        Closed
        }

    // ----- data members ---------------------------------------------------

    /**
     * The topic name.
     */
    protected final String f_sTopicName;

    /**
     * The cache service.
     */
    protected final PagedTopicService f_topicService;

    /**
     * The cache service name (mainly used in logging but calls to serv,ce.getInfo().getServiceName()
     * can trigger a service restart - which is not always desirable).
     */
    protected final String f_sCacheServiceName;

    /**
     * The current state of this {@link PagedTopicCaches}.
     */
    private volatile State m_state;

    /**
     * The number of partitions in the cache service.
     */
    protected final int f_cPartition;

    /**
     * The caches which back the topic.
     */
    protected volatile Set<NamedCache> f_setCaches;

    /**
     * The cache that holds the topic pages.
     */
    public NamedCache<Page.Key, Page> Pages;

    /**
     * The cache that holds the topic elements.
     */
    public NamedCache<Binary, Binary> Data;

    /**
     * The cache that holds the topic subscriber partitions.
     */
    public NamedCache<Subscription.Key, Subscription> Subscriptions;

    /**
     * The cache that holds the topic subscriber information.
     */
    public NamedCache<SubscriberInfo.Key, SubscriberInfo> Subscribers;

    /**
     * The cache that is used to notify blocked publishers and subscribers that they topic is no longer full/empty.
     */
    public NamedCache<NotificationKey, int[]> Notifications;

    /**
     * The cache that holds the highest used topic pages for a cache partition.
     */
    public NamedCache<Usage.Key, Usage> Usages;

    private final BiFunction<String, ClassLoader, NamedCache> f_functionCache;

    /**
     * The deactivation listener used to detect topic destroy and service restarts.
     */
    private final DeactivationListener m_deactivationListener = new DeactivationListener();

    /**
     * The {@link Listener} instances to be notified of connection and disconnection events.
     */
    private final Map<Listener, Object> m_mapListener = new ConcurrentHashMap<>();

    /**
     * The {@link PagedTopicDependencies dependencies} for the topic.
     */
    private final PagedTopicDependencies f_dependencies;
    }
