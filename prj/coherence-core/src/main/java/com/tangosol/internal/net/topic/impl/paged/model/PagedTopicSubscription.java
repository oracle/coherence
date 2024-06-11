/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic.impl.paged.model;

import com.oracle.coherence.common.util.SafeClock;

import com.tangosol.internal.net.topic.ChannelAllocationStrategy;

import com.tangosol.io.ExternalizableLite;

import com.tangosol.net.topic.TopicException;

import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.Filter;
import com.tangosol.util.ImmutableArrayList;
import com.tangosol.util.UUID;
import com.tangosol.util.ValueExtractor;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A representation of a subscription (subscriber group) in a topic.
 *
 * @author Jonathan Knight 2022.11.15
 * @since 22.06.4
 */
@SuppressWarnings("PatternVariableCanBeUsed")
public class PagedTopicSubscription
        implements ExternalizableLite
    {
    /**
     * Default constructor for serialization.
     */
    public PagedTopicSubscription()
        {
        }

    /**
     * Copy constructor.
     *
     * @param subscription  the subscription to copy
     */
    public PagedTopicSubscription(PagedTopicSubscription subscription)
        {
        m_key             = subscription.m_key;
        m_nSubscriptionId = subscription.m_nSubscriptionId;
        m_filter          = subscription.m_filter;
        m_extractor       = subscription.m_extractor;
        update(subscription);
        }

    /**
     * Return the name of the topic this subscription is for.
     *
     * @return the name of the topic this subscription is for
     */
    public String getTopicName()
        {
        return m_key == null ? null : m_key.getTopicName();
        }

    /**
     * Return the name of the subscriber group this subscription is for.
     *
     * @return the name of the subscriber group this subscription is for
     */
    public SubscriberGroupId getGroupId()
        {
        return m_key == null ? null : m_key.getGroupId();
        }

    /**
     * Return the name of the subscriber group id this subscription is for.
     *
     * @return the name of the subscriber group id this subscription is for
     */
    public SubscriberGroupId getSubscriberGroupId()
        {
        return m_key == null ? null : m_key.getGroupId();
        }

    /**
     * Set the {@link Key} of this subscription.
     *
     * @param sTopicName  the name of the topic this subscription is for
     * @param groupId     the subscriber group id this subscription is for
     */
    public void setKey(String sTopicName, SubscriberGroupId groupId)
        {
        setKey(new Key(sTopicName, groupId));
        }

    /**
     * Return the {@link Key} of this subscription.
     *
     * @return the {@link Key} of this subscription
     */
    public Key getKey()
        {
        return m_key;
        }

    /**
     * Set the {@link Key} of this subscription.
     *
     * @param key  the {@link Key} of this subscription
     */
    public void setKey(Key key)
        {
        m_key = key;
        }

    /**
     * Return the subscription id.
     *
     * @return the subscription id
     */
    public long getSubscriptionId()
        {
        return m_nSubscriptionId;
        }

    /**
     * Set the subscription id.
     *
     * @param lId  the subscription id
     */
    public void setSubscriptionId(long lId)
        {
        m_nSubscriptionId = lId;
        }

    /**
     * Copy the subscribers and channel allocations from the
     * specified subscription into this subscription.
     *
     * @param subscription  the subscription to copy
     */
    public void update(PagedTopicSubscription subscription)
        {
        f_lock.lock();
        try
            {
            m_aChannelAllocation = Arrays.copyOf(subscription.m_aChannelAllocation, subscription.m_aChannelAllocation.length);
            m_mapSubscriber.keySet().retainAll(subscription.m_mapSubscriber.keySet());
            m_mapSubscriber.putAll(subscription.m_mapSubscriber);
            m_mapSubscriberChannels.keySet().retainAll(subscription.m_mapSubscriberChannels.keySet());
            m_mapSubscriberChannels.putAll(subscription.m_mapSubscriberChannels);
            m_mapSubscriberTimestamp.keySet().retainAll(subscription.m_mapSubscriberTimestamp.keySet());
            m_mapSubscriberTimestamp.putAll(subscription.m_mapSubscriberTimestamp);
            }
        finally
            {
            f_lock.unlock();
            }
        }

    /**
     * Add one or more subscribers to this subscription.
     *
     * @param aSubscriberId  the ids of the subscriber to add
     *
     * @return {@code true} if one or more subscriber was added, of {@code false} if
     *         the subscription was unchanged
     */
    public boolean addSubscribers(SubscriberId... aSubscriberId)
        {
        if (aSubscriberId.length == 0)
            {
            return false;
            }

        boolean fModified = false;
        f_lock.lock();
        try
            {
            for (SubscriberId subscriberId : aSubscriberId)
                {
                if (SubscriberId.NullSubscriber.equals(subscriberId))
                    {
                    continue;
                    }
                boolean fAdded = m_mapSubscriber.putIfAbsent(subscriberId.getId(), subscriberId) == null;
                if (fAdded)
                    {
                    fModified = true;
                    m_mapSubscriberTimestamp.put(subscriberId.getId(), s_clock.getSafeTimeMillis());
                    }
                }
            }
        finally
            {
            f_lock.unlock();
            }
        return fModified;
        }

    /**
     * Return {@code true} if the specified subscriber is subscribed to this subscription.
     *
     * @param subscriberId  the subscriber to check
     *
     * @return {@code true} if the specified subscriber is subscribed to this subscription
     */
    public boolean hasSubscriber(SubscriberId subscriberId)
        {
        return SubscriberId.NullSubscriber.equals(subscriberId)
                || m_mapSubscriber.containsKey(subscriberId.getId());
        }

    /**
     * Return the subscriber's connection timestamp.
     *
     * @param id  the identifier of the subscriber
     *
     * @return  the subscriber's connection timestamp
     *          or {@link Long#MAX_VALUE} if not connected
     */
    public long getSubscriberTimestamp(SubscriberId id)
        {
        long nId = id.getId();
        if (nId == 0L)
            {
            return 0L;
            }
        Long lTimestamp = m_mapSubscriberTimestamp.get(nId);
        return lTimestamp == null ? Long.MAX_VALUE : lTimestamp;
        }

    /**
     * Return the identifiers of the subscribers subscribed to this subscription.
     *
     * @return the identifiers of the subscribers subscribed to this subscription
     */
    public Set<SubscriberId> getSubscriberIds()
        {
        f_lock.lock();
        try
            {
            return new HashSet<>(m_mapSubscriber.values());
            }
        finally
            {
            f_lock.unlock();
            }
        }

    /**
     * Add the subscribers subscribed to this subscription
     * to the specified map.
     *
     * @param map  the map to add the subscribers to
     */
    public void addSubscribersTo(Map<Long, SubscriberId> map)
        {
        f_lock.lock();
        try
            {
            map.putAll(m_mapSubscriber);
            }
        finally
            {
            f_lock.unlock();
            }
        }

    /**
     * Remove all subscribers from this subscription.
     *
     * @return {@code true} if any subscriber was removed, of {@code false} if
     *         this subscription has zero subscribers
     */
    public boolean removeAllSubscribers()
        {
        f_lock.lock();
        try
            {
            if (m_mapSubscriber.isEmpty())
                {
                return false;
                }
            m_mapSubscriber.clear();
            Arrays.fill(m_aChannelAllocation, 0L);
            m_mapSubscriberChannels.clear();
            m_mapSubscriberTimestamp.clear();
            return true;
            }
        finally
            {
            f_lock.unlock();
            }
        }

    /**
     * Remove one or more subscribers from this subscription.
     *
     * @param aSubscriberId  the ids of the subscribers to remove
     *
     * @return {@code true} if one or more subscribers were removed, of {@code false} if
     *         the subscription was unchanged
     */
    public boolean removeSubscribers(SubscriberId... aSubscriberId)
        {
        if (aSubscriberId.length == 0)
            {
            return false;
            }
        f_lock.lock();
        try
            {
            boolean fModified = false;
            for (SubscriberId subscriberId : aSubscriberId)
                {
                if (subscriberId == null)
                    {
                    continue;
                    }
                fModified = m_mapSubscriber.remove(subscriberId.getId()) != null || fModified;
                m_mapSubscriberTimestamp.remove(subscriberId.getId());
                m_mapSubscriberChannels.remove(subscriberId);
                }
            return fModified;
            }
        finally
            {
            f_lock.unlock();
            }
        }

    /**
     * Returns the number of channels in this subscription.
     *
     * @return the number of channels in this subscription
     */
    public int getChannelCount()
        {
        return m_aChannelAllocation == null ? 0 : m_aChannelAllocation.length;
        }

    /**
     * Return the {@link SubscriberId} for the subscriber that
     * owns the specified channel.
     *
     * @param nChannel  the channel to obtain the owner
     *
     * @return the {@link SubscriberId} for the subscriber that
     *         owns the specified channel or {@code null} if there
     *         is no owner or the channel does not exist
     */
    public SubscriberId getOwningSubscriber(int nChannel)
        {
        long[] aChannelAllocation = m_aChannelAllocation;
        if (aChannelAllocation.length > nChannel)
            {
            long lId = aChannelAllocation[nChannel];
            return m_mapSubscriber.get(lId);
            }
        return null;
        }

    /**
     * Returns the channel allocations for this subscription.
     * <p/>
     * The returned array is indexed by channel id and each element
     * is the subscriber assigned to that channel.
     *
     * @return the channel allocations for this subscription
     */
    public long[] getChannelAllocations()
        {
        f_lock.lock();
        try
            {
            long[] anCopy = new long[m_aChannelAllocation.length];
            System.arraycopy(m_aChannelAllocation, 0, anCopy, 0, m_aChannelAllocation.length);
            return anCopy;
            }
        finally
            {
            f_lock.unlock();
            }
        }

    @SuppressWarnings("unchecked")
    public SortedSet<Integer> getOwnedChannels(SubscriberId id)
        {
        Object oValue;
        f_lock.lock();
        try
            {
            oValue = id == null ? NO_CHANNELS : m_mapSubscriberChannels.getOrDefault(id, NO_CHANNELS);
            }
        finally
            {
            f_lock.unlock();
            }

        if (oValue instanceof Integer)
            {
            return new ImmutableArrayList(Collections.singleton(oValue));
            }
        return Collections.unmodifiableSortedSet((SortedSet<Integer>) oValue);
        }

    /**
     * Set the channel allocations for this subscription.
     *
     * @param strategy  the {@link ChannelAllocationStrategy} to use
     */
    public void updateChannelAllocations(ChannelAllocationStrategy strategy)
        {
        updateChannelAllocations(strategy, -1);
        }

    /**
     * Set the channel allocations for this subscription.
     *
     * @param strategy  the {@link ChannelAllocationStrategy} to use
     */
    @SuppressWarnings("unchecked")
    public void updateChannelAllocations(ChannelAllocationStrategy strategy, int cChannel)
        {
        f_lock.lock();
        try
            {
            if (cChannel <= 0)
                {
                cChannel = m_aChannelAllocation.length;
                }
            m_aChannelAllocation = strategy.allocate(m_mapSubscriber, cChannel);
            m_mapSubscriberChannels.clear();
            for (int i = 0; i < m_aChannelAllocation.length; i++)
                {
                int          nChannel     = i;
                SubscriberId subscriberId = m_mapSubscriber.getOrDefault(m_aChannelAllocation[i], SubscriberId.NullSubscriber);
                m_mapSubscriberChannels.compute(subscriberId, (k, oValue) ->
                    {
                    if (oValue == null)
                        {
                        return nChannel;
                        }
                    if (oValue instanceof Integer)
                        {
                        Integer n = (Integer) oValue;
                        oValue = new TreeSet<>();
                        ((TreeSet<Integer>) oValue).add(n);
                        }
                    ((TreeSet<Integer>) oValue).add(nChannel);
                    return oValue;
                    });
                }
            }
        finally
            {
            f_lock.unlock();
            }
        }

    /**
     * Returns the {@link Filter} to filter messages for this subscription.
     *
     * @return the {@link Filter} to filter messages for this subscription
     */
    public Filter<?> getFilter()
        {
        return m_filter;
        }

    /**
     * Set the {@link Filter} to filter messages for this subscription.
     *
     * @param filter  the {@link Filter} to filter messages for this subscription
     */
    public void setFilter(Filter<?> filter)
        {
        m_filter = filter;
        }

    /**
     * Returns the {@link ValueExtractor} to convert messages for this subscription.
     *
     * @return the {@link ValueExtractor} to convert messages for this subscription
     */
    public ValueExtractor<?, ?> getConverter()
        {
        return m_extractor;
        }

    /**
     * Set the {@link ValueExtractor} to convert messages for this subscription.
     *
     * @param extractor  the {@link ValueExtractor} to convert messages for this subscription
     */
    public void setConverter(ValueExtractor<?, ?> extractor)
        {
        m_extractor = extractor;
        }

    /**
     * Return {@code true} if this is an anonymous subscription.
     *
     * @return  {@code true} if this is an anonymous subscription
     */
    public boolean isAnonymous()
        {
        return m_key.getGroupId().isAnonymous();
        }

    /**
     * Assert that the specified {@link Filter} and {@link ValueExtractor}
     * match those of this {@link PagedTopicSubscription}.
     *
     * @param filter     the {@link Filter} to validate
     * @param extractor  the {@link ValueExtractor} to validate
     *
     * @throws TopicException if the {@link Filter} or {@link ValueExtractor}
     *                        are invalid
     */
    public void assertFilterAndConverter(Filter<?> filter, ValueExtractor<?, ?> extractor)
        {
        if (filter != null && !Objects.equals(filter, m_filter))
            {
            // do not allow new subscription request with a different filter
            throw new TopicException("Cannot change the Filter in existing Subscriber group \""
                    + m_key.getGroupName() + "\" current=" + m_filter + " new=" + filter);
            }
        else if (extractor != null && !Objects.equals(m_extractor, extractor))
            {
            // do not allow new subscription request with a different converter function
            throw  new TopicException("Cannot change the converter function in existing Subscriber group \""
                    + m_key.getGroupName() + "\" current=" + m_extractor + " new=" + extractor);
            }
        }

    public Set<SubscriberId> getDepartedSubscribers(Set<UUID> setMember)
        {
        Set<SubscriberId> setDeparted = new HashSet<>();
        for (SubscriberId id : m_mapSubscriber.values())
            {
            if (!setMember.contains(id.getUID()))
                {
                setDeparted.add(id);
                }
            }
        return setDeparted;
        }

    // ----- ExternalizableLite methods -------------------------------------

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        f_lock.lock();
        try
            {
            m_key                = ExternalizableHelper.readObject(in);
            m_nSubscriptionId    = in.readLong();
            m_filter             = ExternalizableHelper.readObject(in);
            m_extractor          = ExternalizableHelper.readObject(in);
            m_aChannelAllocation = ExternalizableHelper.readObject(in);
            m_mapSubscriber.clear();
            ExternalizableHelper.readMap(in, m_mapSubscriber, null);
            m_mapSubscriberChannels.clear();
            ExternalizableHelper.readMap(in, m_mapSubscriberChannels, null);
            m_mapSubscriberTimestamp.clear();
            ExternalizableHelper.readMap(in, m_mapSubscriberTimestamp, null);
            }
        finally
            {
            f_lock.unlock();
            }
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        f_lock.lock();
        try
            {
            ExternalizableHelper.writeObject(out, m_key);
            out.writeLong(m_nSubscriptionId);
            ExternalizableHelper.writeObject(out, m_filter);
            ExternalizableHelper.writeObject(out, m_extractor);
            ExternalizableHelper.writeObject(out, m_aChannelAllocation);
            ExternalizableHelper.writeMap(out, m_mapSubscriber);
            ExternalizableHelper.writeMap(out, m_mapSubscriberChannels);
            ExternalizableHelper.writeMap(out, m_mapSubscriberTimestamp);
            }
        finally
            {
            f_lock.unlock();
            }
        }

    // ----- Object methods -------------------------------------------------

    @Override
    public String toString()
        {
        return "PagedTopicSubscription{" +
                "key=" + m_key +
                " subscriptionId=" + m_nSubscriptionId +
                ", filter=" + m_filter +
                ", extractor=" + m_extractor +
                ", subscribers=" + m_mapSubscriber +
                ", channelAllocations=" + Arrays.toString(m_aChannelAllocation) +
                ", subscriberAllocations=" + m_mapSubscriberChannels +
                ", timestamps=" + m_mapSubscriberTimestamp +
                '}';
        }

    // ----- inner class: Key -----------------------------------------------

    /**
     * The topic config map key to represent a subscription.
     */
    public static class Key
            implements ExternalizableLite
        {
        /**
         * Default constructor for serialization.
         */
        public Key()
            {
            }

        /**
         * Create a {@link Key}.
         *
         * @param sTopicName  the topic name
         * @param groupId     the {@link SubscriberGroupId subscriber group id}
         */
        public Key(String sTopicName, SubscriberGroupId groupId)
            {
            m_sTopicName = sTopicName;
            m_groupId    = groupId;
            }

        /**
         * Return the topic name.
         *
         * @return the topic name
         */
        public String getTopicName()
            {
            return m_sTopicName;
            }

        /**
         * Return the {@link SubscriberGroupId subscriber group id}.
         *
         * @return the {@link SubscriberGroupId subscriber group id}
         */
        public SubscriberGroupId getGroupId()
            {
            return m_groupId;
            }

        /**
         * Return the subscriber group name.
         *
         * @return the subscriber group name
         */
        public String getGroupName()
            {
            return m_groupId.getGroupName();
            }

        @Override
        public void readExternal(DataInput in) throws IOException
            {
            m_sTopicName = ExternalizableHelper.readSafeUTF(in);
            m_groupId    = ExternalizableHelper.readObject(in);
            }

        @Override
        public void writeExternal(DataOutput out) throws IOException
            {
            ExternalizableHelper.writeSafeUTF(out, m_sTopicName);
            ExternalizableHelper.writeObject(out, m_groupId);
            }

        // ----- Object methods ---------------------------------------------

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
            Key key = (Key) o;
            return Objects.equals(m_sTopicName, key.m_sTopicName) && Objects.equals(m_groupId, key.m_groupId);
            }

        @Override
        public int hashCode()
            {
            return Objects.hash(m_sTopicName, m_groupId);
            }

        @Override
        public String toString()
            {
            return "PagedTopicSubscription.Key{" +
                    "topic='" + m_sTopicName + '\'' +
                    ", group='" + m_groupId + '\'' +
                    '}';
            }

        // ----- data members -----------------------------------------------

        /**
         * The topic name.
         */
        public String m_sTopicName;

        /**
         * The subscriber group id.
         */
        public SubscriberGroupId m_groupId;
        }

    // ----- inner interface: Listener --------------------------------------

    /**
     * A listener that will be notified of changes to {@link PagedTopicSubscription subscriptions}.
     */
    public interface Listener
        {
        /**
         * Called when a {@link PagedTopicSubscription} is updated.
         *
         * @param subscription  the updated {@link PagedTopicSubscription}
         */
        void onUpdate(PagedTopicSubscription subscription);

        /**
         * Called when a {@link PagedTopicSubscription} is deleted.
         *
         * @param subscription  the deleted {@link PagedTopicSubscription}
         */
        void onDelete(PagedTopicSubscription subscription);
        }

    // ----- constants ------------------------------------------------------

    /**
     * A singleton empty long array.
     */
    private static final long[] EMPTY = new long[0];

    /**
     * A singleton empty channel allocation array.
     */
    @SuppressWarnings("unchecked")
    public static final SortedSet<Integer> NO_CHANNELS = new ImmutableArrayList(Collections.emptyList());

    /**
     * The clock to use to add timestamps to subscriptions.
     */
    private static final SafeClock s_clock = SafeClock.INSTANCE;

    // ----- data members ---------------------------------------------------

    /**
     * The lock to synchronize access to state.
     */
    private final Lock f_lock = new ReentrantLock();

    /**
     * The key of this subscription.
     */
    private Key m_key;

    /**
     * The id of this subscription.
     */
    private long m_nSubscriptionId;

    /**
     * The {@link Filter} to filter messages for this subscription.
     */
    private Filter<?> m_filter;

    /**
     * The {@link ValueExtractor} to convert messages for this subscription.
     */
    private ValueExtractor<?, ?> m_extractor;

    /**
     * The subscribers subscribed to this subscription.
     */
    private final SortedMap<Long, SubscriberId> m_mapSubscriber = new TreeMap<>();

    /**
     * The channel allocations for this subscription.
     */
    private long[] m_aChannelAllocation = EMPTY;

    /**
     * A map of subscriber identifiers to owned channels.
     */
    private final Map<SubscriberId, Object> m_mapSubscriberChannels = new HashMap<>();

    /**
     * The timestamps of the subscriber's subcription.
     */
    private final Map<Long, Long> m_mapSubscriberTimestamp = new HashMap<>();
    }
