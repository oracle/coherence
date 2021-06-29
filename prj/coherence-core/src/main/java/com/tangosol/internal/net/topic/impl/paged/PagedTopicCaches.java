/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic.impl.paged;

import com.oracle.coherence.common.base.Exceptions;
import com.oracle.coherence.common.base.Logger;

import com.oracle.coherence.common.collections.ConcurrentHashMap;

import com.tangosol.internal.net.NamedCacheDeactivationListener;

import com.tangosol.internal.net.topic.impl.paged.agent.EnsureSubscriptionProcessor;

import com.tangosol.internal.net.topic.impl.paged.model.NotificationKey;
import com.tangosol.internal.net.topic.impl.paged.model.Page;
import com.tangosol.internal.net.topic.impl.paged.model.ContentKey;
import com.tangosol.internal.net.topic.impl.paged.model.PagedPosition;
import com.tangosol.internal.net.topic.impl.paged.model.SubscriberGroupId;
import com.tangosol.internal.net.topic.impl.paged.model.SubscriberInfo;
import com.tangosol.internal.net.topic.impl.paged.model.Subscription;
import com.tangosol.internal.net.topic.impl.paged.model.Usage;

import com.tangosol.io.ClassLoaderAware;
import com.tangosol.io.Serializer;

import com.tangosol.net.CacheService;
import com.tangosol.net.DistributedCacheService;
import com.tangosol.net.MemberEvent;
import com.tangosol.net.MemberListener;
import com.tangosol.net.NamedCache;

import com.tangosol.net.PartitionedService;
import com.tangosol.net.cache.TypeAssertion;

import com.tangosol.net.partition.PartitionSet;
import com.tangosol.net.topic.NamedTopic;
import com.tangosol.net.topic.Position;

import com.tangosol.net.topic.Subscriber;
import com.tangosol.util.AbstractMapListener;
import com.tangosol.util.Aggregators;
import com.tangosol.util.Base;
import com.tangosol.util.Filter;
import com.tangosol.util.Filters;
import com.tangosol.util.HashHelper;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.InvocableMapHelper;
import com.tangosol.util.MapEvent;
import com.tangosol.util.ValueExtractor;

import com.tangosol.util.aggregator.GroupAggregator;

import com.tangosol.util.extractor.EntryExtractor;
import com.tangosol.util.extractor.ReflectionExtractor;
import com.tangosol.util.filter.AlwaysFilter;
import com.tangosol.util.filter.PartitionedFilter;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import java.util.concurrent.ExecutionException;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import java.util.stream.Collectors;

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
    implements ClassLoaderAware
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a {@link PagedTopicCaches}.
     *
     * @param sName         the name of the topic
     * @param cacheService  the {@link CacheService} owning the underlying caches
     */
    public PagedTopicCaches(String sName, CacheService cacheService)
        {
        this(sName, cacheService, null);
        }

    /**
     * Create a {@link PagedTopicCaches}.
     *
     * @param sName          the name of the topic
     * @param cacheService   the {@link CacheService} owning the underlying caches
     * @param functionCache  the function to invoke to obtain each underlying cache
     */
    public PagedTopicCaches(String sName, CacheService cacheService,
                            BiFunction<String, ClassLoader, NamedCache> functionCache)
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
        f_cacheService      = cacheService;
        f_sCacheServiceName = cacheService.getInfo().getServiceName();
        f_cPartition        = ((DistributedCacheService) cacheService).getPartitionCount();
        f_functionCache     = functionCache;

        initializeCaches();

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
        return f_cacheService.getSerializer();
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
     * Return whether or not the caches are active,
     * specifically the page cache for the topic.
     *
     * @return true if the caches are active; false otherwise
     */
    public boolean isActive()
        {
        State state = m_state;
        return state == State.Active || state == State.Disconnected;
        }

    /**
     * Return whether or not the caches are destroyed,
     * specifically the page cache for the topic.
     *
     * @return true if the caches are destroyed; false otherwise
     */
    public boolean isDestroyed()
        {
        return Pages.isDestroyed();
        }

    /**
     * Return whether or not the caches are released,
     * specifically the page cache for the topic.
     *
     * @return true if the caches are released; false otherwise
     */
    public boolean isReleased()
        {
        return Pages.isReleased();
        }

    public synchronized void addListener(Listener listener)
        {
        ensureListeners();
        m_mapListener.put(listener, Boolean.TRUE);
        }

    public synchronized void removeListener(Listener listener)
        {
        m_mapListener.remove(listener);
        }

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
        return f_cacheService.getContextClassLoader();
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
        return getDependencies().getChannelCount(f_cPartition);
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
     * Return the associated CacheService.
     *
     * @return the cache service
     */
    public CacheService getCacheService()
        {
        return f_cacheService;
        }

    /**
     * Return the {@link PagedTopic.Dependencies}.
     *
     * @return the {@link PagedTopic.Dependencies}
     */
    public PagedTopic.Dependencies getDependencies()
        {
        return f_cacheService.getResourceRegistry()
            .getResource(PagedTopic.Dependencies.class, getTopicName());
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
        ValueExtractor<Subscription.Key, Integer>           extractorChannel = new ReflectionExtractor<>("getChannelId", new Object[0], EntryExtractor.KEY);
        ValueExtractor<Subscription.Key, SubscriberGroupId> extractorGroup   = new ReflectionExtractor<>("getGroupId", new Object[0], EntryExtractor.KEY);
        Filter<Subscription.Key>                            filter           = Filters.equal(extractorGroup, subscriberGroupId);
        Filter<PagedPosition>                               filterPosition   = Filters.not(Filters.equal(PagedPosition::getPage, Page.NULL_PAGE));

        InvocableMap.EntryAggregator<Subscription.Key, Subscription, Position> aggregatorPosn
                = Aggregators.comparableMax(Subscription::getCommittedPosition);

        // Aggregate the subscription commits and remove any null values from the returned map
        return Subscriptions.aggregate(filter, GroupAggregator.createInstance(extractorChannel, aggregatorPosn, filterPosition))
                .entrySet()
                .stream()
                .filter(e -> e.getKey() != Page.EMPTY && e.getValue() != null)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }

    /**
     * Returns a {@link Map} of the {@link Position} of the head for each channel for a subscriber group.
     *
     * @param subscriberGroupId  the {@link SubscriberGroupId identifier} of the subscriber
     *                           group to obtain the heads for
     *
     * @return a {@link Map} of the {@link Position} of the head for each channel for a subscriber group
     */
    public Map<Integer, Position> getHeads(SubscriberGroupId subscriberGroupId, long nSubscriberId)
        {
        ValueExtractor<Subscription.Key, Integer>           extractorChannel = new ReflectionExtractor<>("getChannelId", new Object[0], EntryExtractor.KEY);
        ValueExtractor<Subscription.Key, SubscriberGroupId> extractorGroup   = new ReflectionExtractor<>("getGroupId", new Object[0], EntryExtractor.KEY);
        Filter<Subscription.Key>                            filter           = Filters.equal(extractorGroup, subscriberGroupId);
        Filter<PagedPosition>                               filterPosition   = Filters.not(Filters.equal(PagedPosition::getPage, Page.NULL_PAGE));

        InvocableMap.EntryAggregator<Subscription.Key, Subscription, PagedPosition> aggregatorPosn
                = Aggregators.comparableMin(new Subscription.HeadExtractor(nSubscriberId));

        // Aggregate the subscription commits and remove any null values from the returned map
        return Subscriptions.aggregate(filter, GroupAggregator.createInstance(extractorChannel, aggregatorPosn, filterPosition))
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
     * There is no guarantee that all of the subscribers are actually still active. If a subscriber
     * process exits without closing the subscriber, the identifier remains in the cache until it
     * is timed-out.
     *
     * @param sSubscriberGroup  the subscriber group name to get subscribers for
     *
     * @return the identifiers for all the subscribers belonging to a subscriber group
     */
    public Set<SubscriberInfo.Key> getSubscribers(String sSubscriberGroup)
        {
        ReflectionExtractor<SubscriberInfo.Key, SubscriberGroupId> extractor
                = new ReflectionExtractor<>("getGroupId", new Object[0], EntryExtractor.KEY);

        return Subscribers.keySet(Filters.equal(extractor, SubscriberGroupId.withName(sSubscriberGroup)));
        }

    /**
     * Return the set of {@link Subscriber.Name named} subscriber group(s) and statically configured subscriber-group(s).
     *
     * @return the set of named subscriber groups.
     */
    public Set<String> getSubscriberGroups()
        {
        int          cParts  = ((PartitionedService) Subscriptions.getCacheService()).getPartitionCount();
        PartitionSet setPart = new PartitionSet(cParts);

        setPart.add(Base.getRandom().nextInt(cParts));

        Set<Subscription.Key> setSubs = Subscriptions.keySet(new PartitionedFilter<>(AlwaysFilter.INSTANCE(), setPart));

        return setSubs.stream()
                .filter((key) -> key.getGroupId().getMemberTimestamp() == 0)
                .map((key) -> key.getGroupId().getGroupName())
                .collect(Collectors.toSet());
        }

    /**
     * Ensure the specified subscriber group exists.
     *
     * @param sName        the name of the group
     * @param filter       the filter to use to filter messages received by the group
     * @param fnConverter  the converter function to convert the messages received by the group
     */
    protected void ensureSubscriberGroup(String sName, Filter<?> filter, Function<?, ?> fnConverter)
        {
        SubscriberGroupId subscriberGroupId = SubscriberGroupId.withName(sName);
        initializeSubscription(subscriberGroupId, 1, filter, fnConverter, false, true, false);
        }

    /**
     * Initialise a subscription.
     *
     * @param subscriberGroupId  the subscriber group identifier
     * @param nSubscriberId      the subscriber identifier
     * @param filter             the filter to use to filter messages received by the subscription
     * @param fnConverter        the converter function to convert the messages received by the subscription
     * @param fReconnect         {@code true} if this is a reconnection
     * @param fCreateGroupOnly   {@code true} if this is to only create a subscriber group
     * @param fDisconnected      {@code true} if this is an existing disconnected subscription
     *
     * @return the pages that are the heads of the channels
     */
    protected long[] initializeSubscription(SubscriberGroupId subscriberGroupId,
                                            long              nSubscriberId,
                                            Filter<?>         filter,
                                            Function<?, ?>    fnConverter,
                                            boolean           fReconnect,
                                            boolean           fCreateGroupOnly,
                                            boolean           fDisconnected)
        {
        try
            {
            int     cChannel = getChannelCount();
            String  sName    = subscriberGroupId.getGroupName();

            Set<Subscription.Key> setSubKeys = new HashSet<>(f_cPartition);
            for (int i = 0; i < f_cPartition; ++i)
                {
                // Note: we ensure against channel 0 in each partition, and it will in turn initialize all channels
                setSubKeys.add(new Subscription.Key(i, /*nChannel*/ 0, subscriberGroupId));
                }

            // outside of any lock discover if pages are already pinned.  Note that since we don't
            // hold a lock, this is only useful if the group was already fully initialized (under lock) earlier.
            // Otherwise there is no guarantee that there isn't gaps in our pinned pages.
            // check results to verify if initialization has already completed
            EnsureSubscriptionProcessor processor = new EnsureSubscriptionProcessor(EnsureSubscriptionProcessor.PHASE_INQUIRE, null, filter,  fnConverter, nSubscriberId, fReconnect, fCreateGroupOnly);
            Collection<EnsureSubscriptionProcessor.Result> results = sName == null
                ? null
                : InvocableMapHelper.invokeAllAsync(Subscriptions, setSubKeys,
                            key -> getUnitOfOrder(key.getPartitionId()), processor).get().values();

            Collection<long[]> colPages = EnsureSubscriptionProcessor.Result.assertPages(results);

            long[] alHead = new long[cChannel];
            if (colPages == null || colPages.contains(null) || fDisconnected)
                {
                alHead = initialiseSubscriptionPages(subscriberGroupId, nSubscriberId, filter, fnConverter, fReconnect, fCreateGroupOnly, setSubKeys);
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

            return alHead;
            }
        catch (InterruptedException | ExecutionException e)
            {
            throw Exceptions.ensureRuntimeException(e);
            }
        }

    /**
     * Initialise the head pages for the subscriber.
     *
     * @param subscriberGroupId  the subscriber group identifier
     * @param setSubKeys  the set of {@link Subscription.Key keys} to use to initialise the subscription pages
     *
     * @return an array of head pages
     *
     * @throws InterruptedException if any asynchronous operations are interrupted
     * @throws ExecutionException if any asynchronous operations fail
     */
    @SuppressWarnings("OptionalGetWithoutIsPresent")
    protected long[] initialiseSubscriptionPages(SubscriberGroupId     subscriberGroupId,
                                                 long                  nSubscriberId,
                                                 Filter<?>             filter,
                                                 Function<?, ?>        fnConverter,
                                                 boolean               fReconnect,
                                                 boolean               fCreateGroupOnly,
                                                 Set<Subscription.Key> setSubKeys)
            throws InterruptedException, ExecutionException
        {
        String sGroupName = subscriberGroupId.getGroupName();

        // The subscription doesn't exist in at least some partitions, create it under lock. A lock is used only
        // to protect against concurrent create/destroy/create resulting an gaps in the pinned pages.  Specifically
        // it would be safe for multiple subscribers to concurrently "create" the subscription, it is only unsafe
        // if there is also a concurrent destroy as this could result in gaps in the pinned pages.
        if (sGroupName != null)
            {
            Subscriptions.lock(subscriberGroupId, -1);
            }

        try
            {
            EnsureSubscriptionProcessor processor
                    = new EnsureSubscriptionProcessor(EnsureSubscriptionProcessor.PHASE_PIN, null,
                                                      filter, fnConverter, nSubscriberId, fReconnect, fCreateGroupOnly);

            Collection<EnsureSubscriptionProcessor.Result> results = InvocableMapHelper.invokeAllAsync(
                    Subscriptions, setSubKeys, key -> getUnitOfOrder(key.getPartitionId()), processor)
                .get().values();

            Collection<long[]> colPages = EnsureSubscriptionProcessor.Result.assertPages(results);

            PagedTopic.Dependencies dependencies = getDependencies();
            int                     cChannel      = getChannelCount();
            long                    lPageBase     = getBasePage();
            long[]                  alHead        = new long[cChannel];

            // mapPages now reflects pinned pages
            for (int nChannel = 0; nChannel < cChannel; ++nChannel)
                {
                final int finChan = nChannel;

                if (fReconnect || dependencies.isRetainConsumed())
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
            processor = new EnsureSubscriptionProcessor(EnsureSubscriptionProcessor.PHASE_ADVANCE, alHead, filter, fnConverter, nSubscriberId, fReconnect, fCreateGroupOnly);
            InvocableMapHelper.invokeAllAsync(Subscriptions, setSubKeys,
                    key -> getUnitOfOrder(key.getPartitionId()), processor).join();

            return alHead;
            }
        finally
            {
            if (sGroupName != null)
                {
                Subscriptions.unlock(subscriberGroupId);
                }
            }
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

        return f_sTopicName.equals(that.f_sTopicName) && f_cacheService.equals(that.f_cacheService);
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
    private synchronized void initializeCaches()
        {
        f_cacheService.start();

        ClassLoader loader = f_cacheService.getContextClassLoader();

        Pages               = f_functionCache.apply(Names.PAGES.cacheNameForTopicName(f_sTopicName), loader);
        Data                = f_functionCache.apply(Names.CONTENT.cacheNameForTopicName(f_sTopicName), loader);
        Subscribers         = f_functionCache.apply(Names.SUBSCRIBERS.cacheNameForTopicName(f_sTopicName), loader);
        Notifications       = f_functionCache.apply(Names.NOTIFICATIONS.cacheNameForTopicName(f_sTopicName), loader);
        Usages              = f_functionCache.apply(Names.USAGE.cacheNameForTopicName(f_sTopicName), loader);
        Subscriptions       = f_functionCache.apply(Names.SUBSCRIPTIONS.cacheNameForTopicName(f_sTopicName), loader);

        Set<NamedCache> setCaches = f_setCaches = new HashSet<>();

        setCaches.add(Pages);
        setCaches.add(Data);
        setCaches.add(Subscriptions);
        setCaches.add(Subscribers);
        setCaches.add(Notifications);
        setCaches.add(Usages);

        ensureListeners();
        }

    @SuppressWarnings("unchecked")
    private synchronized void ensureListeners()
        {
        DeactivationListener listener = m_deactivationListener;
        Pages.addMapListener(listener);
        f_cacheService.addMemberListener(listener);
        }

    @SuppressWarnings("unchecked")
    private synchronized void removeListeners()
        {
        DeactivationListener listener = m_deactivationListener;
        if (Pages.isActive())
            {
            Pages.removeMapListener(listener);
            }
        f_cacheService.removeMemberListener(listener);
        }

    /**
     * Release or destroy the PagedTopicCaches.
     *
     * @param fDestroy  true to destroy, false to release
     */
    private void releaseOrDestroy(boolean fDestroy)
        {
        if (!isActive())
            {
            return;
            }

        synchronized (this)
            {
            if (isActive())
                {
                m_state = fDestroy ? State.Destroyed : State.Released;

                Consumer<NamedCache> function    = fDestroy ? this::destroyCache : this::releaseCache;
                Set<Listener>        setListener = m_mapListener.keySet();

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
                    synchronized (this)
                        {
                        if (f_setCaches != null)
                            {
                            f_setCaches.forEach(function);
                            f_setCaches = null;
                            }
                        }
                    }
                }
            }
        }

    private void destroyCache(NamedCache<?, ?> cache)
        {
        if (cache.isActive())
            {
            cache.destroy();
            }
        }

    private void releaseCache(NamedCache<?, ?> cache)
        {
        if (cache.isActive())
            {
            cache.release();
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
     * An pseudo enumeration representing the different caches used
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
         * Return true if corresponding cache should be considered an internal submapping.
         *
         * @return true if corresponding sub cache mapping should be considered internal.
         */
        public boolean isInternal()
            {
            return Storage.MetaData.equals(getStorage());
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
         * The cache that holds the topic content.
         *
         * Use of no prefix rather than METACACHE_PREFIX since it is being used to filter out internal topic meta caches.
         */
        public static final Names<ContentKey,Object> CONTENT =
            new Names<>("content", "$topic$", ContentKey.class, Object.class, Names.Storage.Data);

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
                String sReason = fReleased ? "release" : "destroy";
                Logger.fine("Detected " + sReason + " of topic " + f_sTopicName);
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
            else if (service.getOwnershipEnabledMembers().isEmpty())
                {
                Logger.fine("Detected loss of all storage members in service " + PagedTopicCaches.this);
                disconnected();
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
    protected final CacheService f_cacheService;

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
    protected Set<NamedCache> f_setCaches;

    /**
     * The cache that holds the topic pages.
     */
    public NamedCache<Page.Key, Page> Pages;

    /**
     * The cache that holds the topic elements.
     */
    public NamedCache<ContentKey, Object> Data;

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
    }
