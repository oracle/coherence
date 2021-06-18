/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic.impl.paged.model;

import com.tangosol.internal.util.Primes;

import com.tangosol.io.AbstractEvolvable;

import com.tangosol.io.pof.EvolvablePortableObject;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.net.partition.KeyPartitioningStrategy;

import com.tangosol.util.Filter;
import com.tangosol.util.HashHelper;
import com.tangosol.util.ValueExtractor;

import java.io.IOException;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import java.util.function.Function;

/**
 * Subscriber group data for a particular cache partition.
 *
 * @author mf  2016.02.22
 * @since Coherence 14.1.1
 */
public class Subscription
    extends AbstractEvolvable
    implements EvolvablePortableObject
    {
    // ----- Constructors ---------------------------------------------------

    /**
     * Empty constructor for serialization.
     */
    public Subscription()
        {
        }

    // ----- TopicPartitionSubscription methods -----------------------

    /**
     * Return the subscriber's head page across all partitions
     *
     * @return the head page
     */
    public long getSubscriptionHead()
        {
        return m_lHeadSubscription;
        }

    /**
     * Set the subscriber's head page across all partitions
     *
     * @param lPage  the head page
     */
    public void setSubscriptionHead(long lPage)
        {
        m_lHeadSubscription = lPage;
        }

    /**
     * Return the subscriber's page within the partition
     *
     * @return the page
     */
    public long getPage()
        {
        return m_lPage;
        }

    /**
     * Set the subscriber's page within the partition
     *
     * @param lPage the page
     */
    public void setPage(long lPage)
        {
        m_lPage = lPage;
        }

    /**
     * Return the read position for this partition's active page.
     *
     * @return the read position
     */
    public int getPosition()
        {
        return m_nPosition;
        }

    /**
     * Set the read position for this partition's active page.
     *
     * @param nPosition the position
     */
    public void setPosition(int nPosition)
        {
        m_nPosition = nPosition;
        }

    /**
     * Return the head {@link PagedPosition} for this partition's active page.
     *
     * @return the head {@link PagedPosition}
     */
    public PagedPosition getHeadPosition()
        {
        return new PagedPosition(m_lPage, m_nPosition);
        }

    /**
     * Set the committed position in the committed page.
     *
     * @param posCommit    the committed position
     * @param posRollback  the position to rollback to on reconnection or channel redistribution
     */
    public void setCommittedPosition(PagedPosition posCommit, PagedPosition posRollback)
        {
        m_posCommitted = posCommit;
        m_posRollback  = posRollback;
        }

    /**
     * Returns the last committed {@link PagedPosition}.
     *
     * @return the last committed {@link PagedPosition}
     */
    public PagedPosition getCommittedPosition()
        {
        return m_posCommitted;
        }

    /**
     * Returns the {@link PagedPosition} the subscription will rollback to.
     *
     * @return the {@link PagedPosition} the subscription will rollback to
     */
    public PagedPosition getRollbackPosition()
        {
        return m_posRollback;
        }

    /**
     * Return the subscription's filter, or null
     *
     * @return the filter
     */
    public Filter<?> getFilter()
        {
        return m_filter;
        }

    /**
     * Set the subscription's filter
     *
     * @param filter the filter
     */
    public void setFilter(Filter<?> filter)
        {
        m_filter = filter;
        }

    /**
     * Return the subscription's converter, or null.
     *
     * @return the converter {@link Function}
     */
    public Function<?, ?> getConverter()
        {
        return m_fnConvert;
        }

    /**
     * Set the subscription's converter {@link Function}.
     *
     * @param fnConvert  the filter
     */
    public void setConverter(Function<?, ?> fnConvert)
        {
        m_fnConvert = fnConvert;
        }

    /**
     * Returns {@code true} if a given subscriber is allocated to this owns the channel,
     * or zero if no subscriber owns the channel.
     *
     * @return the identifier of the subscriber that owns the channel
     */
    public long getOwningSubscriber()
        {
        return m_nOwningSubscriber;
        }

    /**
     * Set the identifier of the subscriber that owns the channel,
     * or zero if no subscriber owns the channel.
     *
     * @param nOwningSubscriber  the identifier of the subscriber that owns the channel
     */
    public void setOwningSubscriber(long nOwningSubscriber)
        {
        if (m_nOwningSubscriber != nOwningSubscriber)
            {
            // the owner has changed so we need to reset the position to the last committed page
            // and the next position after the last committed position
            m_nOwningSubscriber = nOwningSubscriber;
            rollback();
            }
        }

    /**
     * Roll the subscriber back to the last committed position.
     */
    public void rollback()
        {
        m_lPage     = m_posRollback.getPage();
        m_nPosition = m_posRollback.getOffset();
        }

    /**
     * Returns the identifier of the subscriber owning the specified channel
     *
     * @param nChannel  the channel to obtain the owning subscriber identifier for
     *
     * @return  the owning subscriber identifier, or zero if no subscriber owns the channel
     */
    public long getChannelOwner(int nChannel)
        {
        return m_aChannel == null ? m_nOwningSubscriber : m_aChannel[nChannel];
        }

    /**
     * Add a subscriber to the subscription.
     *
     * @param nSubscriber  the unique identifier of the subscriber to add
     * @param cChannel     the number of channels to distribute across the subscribers
     */
    public synchronized void addSubscriber(long nSubscriber, int cChannel)
        {
        if (nSubscriber == 0)
            {
            return;
            }

        if (m_setSubscriber == null)
            {
            m_setSubscriber = new TreeSet<>();
            }

        if (m_setSubscriber.add(nSubscriber))
            {
            refresh(m_setSubscriber, cChannel, createRing(cChannel));
            }
        }

    /**
     * Remove a subscriber from the subscription.
     *
     * @param nSubscriber  the unique identifier of the subscriber to remove
     * @param cChannel     the number of channels to distribute across the subscribers
     */
    public synchronized void removeSubscriber(long nSubscriber, int cChannel)
        {
        if (nSubscriber == 0)
            {
            return;
            }

        if (m_setSubscriber != null)
            {
            if (m_setSubscriber.remove(nSubscriber))
                {
                refresh(m_setSubscriber, cChannel, createRing(cChannel));
                }
            }
        else if (m_nOwningSubscriber == nSubscriber)
            {
            m_nOwningSubscriber = 0;
            }
        }

    /**
     * Returns the set of subscriber identifiers that this subscription knows about.
     *
     * @return  the set of subscriber identifiers that this subscription knows about
     */
    public Set<Long> getSubscribers()
        {
        return m_setSubscriber == null ? Collections.emptySet() : m_setSubscriber;
        }

    /**
     * Returns {@code true} if the subscriber identifier is known to this {@link Subscription}.
     *
     * @param id  the identifier of the subscriber
     *
     * @return {@code true} if the subscriber identifier is known to this {@link Subscription},
     *         otherwise {@code false}
     */
    public boolean hasSubscriber(long id)
        {
        return m_setSubscriber == null ? (m_nOwningSubscriber == id) : m_setSubscriber.contains(id);
        }

    /**
     * Returns the channels allocated to the specified subscriber.
     *
     * @param nSubscriber  the identifier of the subscriber
     * @param cChannel     the total number of channels
     *
     * @return the channels allocated to the specified subscriber or
     *         an empty list if the subscriber is not allocated any \
     *         channels
     */
    public int[] getChannels(long nSubscriber, int cChannel)
        {
        if (m_aChannel == null || m_aChannel.length == 0)
            {
            if (nSubscriber == m_nOwningSubscriber)
                {
                int[] anChannel = new int[cChannel];
                for (int i = 0; i < cChannel; i++)
                    {
                    anChannel[i] = i;
                    }
                return anChannel;
                }
            return new int[0];
            }


        int   cMatch    = (int) Arrays.stream(m_aChannel).filter(s -> s == nSubscriber).count();
        int[] anChannel = new int[cMatch];
        int   nIndex    = 0;
        for (int i = 0; i < m_aChannel.length; i++)
            {
            if (m_aChannel[i] == nSubscriber)
                {
                anChannel[nIndex++] = i;
                }
            }
        return anChannel;
        }

    /**
     * Returns an immutable {@link Map} of channel allocations.
     *
     * @return an immutable {@link Map} of channel allocations where the key is
     *         a channel number and the value is the owning subscriber identifier
     */
    long[] getChannelAllocations()
        {
        return m_aChannel;
        }

    /**
     * Create the {@link Key} to the {@link Subscription} used as the sync for the subscriptions
     * global information.
     *
     * @param subscriberGroupId  the subscriber group identifier
     * @param nChannel           the channel identifier
     * @param cParts             the number of partitions
     *
     * @return the {@link Key} to the {@link Subscription} used as the sync for the subscriptions
     *         global information
     */
    public static Key createSyncKey(SubscriberGroupId subscriberGroupId, int nChannel, int cParts)
        {
        // we don't just use (0,chan) as that would concentrate extra load on a single partitions when there are many groups
        int nPart = Math.abs((HashHelper.hash(subscriberGroupId.hashCode(), nChannel) % cParts));
        return new Subscription.Key(nPart, nChannel, subscriberGroupId);
        }

    // ----- EvolvablePortableObject interface ------------------------------

    @Override
    public int getImplVersion()
        {
        return DATA_VERSION;
        }

    @Override
    public void readExternal(PofReader in)
        throws IOException
        {
        m_lHeadSubscription = in.readLong(0);
        m_lPage             = in.readLong(1);
        m_nPosition         = in.readInt(2);
        m_filter            = in.readObject(3);
        m_fnConvert         = in.readObject(4);
        m_setSubscriber     = new TreeSet<>();
        if (getDataVersion() >= 2)
            {
            m_nOwningSubscriber = in.readLong(5);
            m_setSubscriber     = in.readCollection(6, m_setSubscriber);
            m_aChannel          = in.readLongArray(7);
            m_posCommitted      = in.readObject(8);
            m_posRollback       = in.readObject(9);
            }
        }

    @Override
    public void writeExternal(PofWriter out)
            throws IOException
        {
        out.writeLong(0, m_lHeadSubscription);
        out.writeLong(1, m_lPage);
        out.writeInt(2, m_nPosition);
        out.writeObject(3, m_filter);
        out.writeObject(4, m_fnConvert);
        out.writeLong(5, m_nOwningSubscriber);
        out.writeCollection(6, m_setSubscriber);
        out.writeLongArray(7, m_aChannel);
        out.writeObject(8, m_posCommitted);
        out.writeObject(9, m_posRollback);
        }

    // ----- Object methods -------------------------------------------------

    @Override
    public String toString()
        {
        return getClass().getSimpleName() + "(head=" + m_lHeadSubscription
            + ", page=" + m_lPage + ", position=" + m_nPosition
            + ", committed=" + (m_posCommitted.getPage() == Page.NULL_PAGE ? "None" : m_posCommitted)
            + ", rollback=" + (m_posRollback.getPage() == Page.NULL_PAGE ? "Unset" : m_posRollback)
            + ", filter=" + m_filter + ", converter=" + m_fnConvert
            + ", owner=" + m_nOwningSubscriber + " subscribers=" + m_setSubscriber + ")";
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Returns the number of virtual subscribers for a given channel count.
     * <p>
     * Subscriber allocation uses consistent hashing to hash subscribers into
     * a ring. In order to get better distribution additional virtual copies
     * of each subscriber are also hashed into the ring.
     *
     * @param cChannel  the channel count
     *
     * @return the virtual subscriber count for the given channel count
     */
    private byte getReplicaCount(int cChannel)
        {
        return (byte) Math.min(Byte.MAX_VALUE, Primes.next(cChannel / 2));
        }

    /**
     * Create the ring of subscriber hashes that will be used to allocate channels
     * to subscribers.
     * <p>
     * Subscribers are added to the ring using consistent hashing, this gives the same
     * layout of subscribers (and their virtual copies) in the ring and ensures that
     * all members create the same channel allocation.
     *
     * @param cChannel  the channel count
     *
     * @return  a ring of hashes used to allocate subscribers to channels
     */
    private SortedMap<Integer, Long> createRing(int cChannel)
        {
        byte                     cReplica = getReplicaCount(cChannel);
        SortedMap<Integer, Long> mapRing  = new TreeMap<>();
        for (Long nId : m_setSubscriber)
            {
            byte[] abId = idToBytes(nId);
            for (byte i = 0; i < cReplica; i++)
                {
                abId[0] = i;
                mapRing.put(getSubscriberHash(abId, cChannel, cReplica), nId);
                }
            }
        return mapRing;
        }

    /**
     * Create the consistent hash of a subscriber identifier.
     * <p>
     * The hash is a position in the ring used to allocate channels to subscribers.
     *
     * @param abId      the subscriber identifier as a byte array
     * @param cChannel  the channel count
     * @param cReplica  the number of virtual copies of the subscriber
     *
     * @return Create the consistent hash of a subscriber identifier
     */
    private int getSubscriberHash(byte[] abId, int cChannel, byte cReplica)
        {
        return Math.abs(java.util.Arrays.hashCode(abId) % (cChannel * cReplica));
        }

    /**
     * Convert the {@code long} subscriber identifier into its byte array representation.
     *
     * @param id  the subscriber identifier
     *
     * @return  the byte array representation of the {@code long} subscriber identifier
     */
    private byte[] idToBytes(long id)
        {
        byte[] abId = new byte[Long.BYTES + 1];
        for (int i = Long.BYTES - 1; i >= 0; i--)
            {
            abId[i + 1] = (byte) (id & 0xFF);
            id >>= Byte.SIZE;
            }
        return abId;
        }

    /**
     * Refresh the channel allocations.
     * <p>
     * A consistent hashing algorithm is used to ensure that the same allocation is
     * made across cluster members without needing central coordination.
     * <p>
     * The algorithm used will ensure that subscribers are allocated at least one channel, apart
     * from where there are more subscribers than channels, in which case some subscribers will
     * then be allocated zero channels.
     * <p>
     * The allocation is not guaranteed to be evenly balanced (although it should be close enough),
     * some subscribers may have more or less channels than others, but where possible at least one.
     *
     * @param setSubscriber  the set of subscribers
     * @param cChannel       the number of channels to allocate
     * @param mapRing        the consistent hash ring to use to allocate subscribers
     */
    private void refresh(SortedSet<Long> setSubscriber, int cChannel, SortedMap<Integer, Long> mapRing)
        {
        byte                          cReplica   = getReplicaCount(cChannel);
        long[]                        aChannel   = new long[cChannel];
        Map<Long, SortedSet<Integer>> mapAlloc   = new HashMap<>();
        SortedSet<Long>               setUnused  = new TreeSet<>(setSubscriber);
        Map<Long,Integer>             mapUsed    = new HashMap<>();

        if (!mapRing.isEmpty())
            {
            for (int i = 0; i < (cChannel * cReplica); i += cReplica)
                {
                SortedMap<Integer, Long> mapTail = mapRing.tailMap(i);
                Integer                  key     = mapTail.isEmpty() ? mapRing.firstKey() : mapTail.firstKey();
                int                      nKey    = i / cReplica;
                long                     nId     = mapRing.get(key);
                setUnused.remove(nId);
                mapUsed.compute(nId, (k, nUsage) -> nUsage == null ? 1 : nUsage + 1);
                mapAlloc.compute(nId, (k, set) ->
                    {
                    if (set == null)
                        {
                        set = new TreeSet<>();
                        }
                    set.add(nKey);
                    return set;
                    });

                aChannel[nKey] = nId;
                }
            }

        while (mapUsed.size() < cChannel && !setUnused.isEmpty())
            {
            // we have unallocated subscribers and some subscribers with more than one allocation,
            // we will try to allocate to any subscribers that did not get an allocation
            //
            // We do this by taking the lowest channel from the subscriber with the most channels
            // in a loop until we have allocated to all subscribers. Where multiple subscribers
            // have the most channels, the lowest channel is taken from the subscriber with the
            // lowest identifier. We loop until all subscribers have channels or we run out of
            // channels (i.e. there are more subscribers than channels).

            long nId   = setUnused.first();
            Long nTake = mapUsed.entrySet()
                                .stream()
                                .sorted((e1, e2) ->
                                        {
                                        int i = Integer.compare(e2.getValue(), e1.getValue());
                                        if (i == 0)
                                            {
                                            return Long.compare(e1.getKey(), e2.getKey());
                                            }
                                        return i;
                                        })
                                .map(Map.Entry::getKey) // lowest subscriber with most allocations
                                .findFirst()
                                .orElse(-1L);

            if (nTake == -1)
                {
                break;
                }

            Integer c   = mapAlloc.get(nTake).first();
            aChannel[c] = nId;

            mapUsed.compute(nId, (k, nUsage) -> nUsage == null ? 1 : nUsage + 1);
            mapUsed.compute(nTake, (k, nUsage) -> nUsage == null ? 0 : nUsage - 1);

            mapAlloc.compute(nId, (k, set) ->
                {
                if (set == null)
                    {
                    set = new TreeSet<>();
                    }
                set.add(c);
                return set;
                });

            mapAlloc.compute(nTake, (k, set) ->
                {
                if (set == null)
                    {
                    set = new TreeSet<>();
                    }
                set.remove(c);
                return set;
                });
            setUnused.remove(nId);
            }

        m_aChannel = aChannel;
        }

    // ----- Key class ------------------------------------------------------

    /**
     * Key for the parent class
     */
    // implementation note: Key does not implement Evolvable
    // because adding fields would affect the "equality"
    // of a key
    public static class Key
            implements KeyPartitioningStrategy.PartitionAwareKey, PortableObject
        {
        // ----- constructors -----------------------------------------------

        /**
         * A default constructor used for serialization.
         */
        public Key()
            {
            }

        /**
         * Create a {@link Key} for the specified partition
         * and subscriber.
         *
         * @param nPartition  the partition id
         * @param nChannel    the channel id
         * @param groupId     the subscriber id
         */
        public Key(int nPartition, int nChannel, SubscriberGroupId groupId)
            {
            m_nPartition = nPartition;
            m_nChannel   = nChannel;
            m_groupId    = groupId;
            }

        // ----- accessor methods -------------------------------------------

        /**
         * Obtain the subscriber that this key is for.
         *
         * @return the subscriber that this key is for
         */
        public SubscriberGroupId getGroupId()
            {
            return m_groupId;
            }

        /**
         * Return the channel id.
         *
         * @return the channel id.
         */
        public int getChannelId()
            {
            return m_nChannel;
            }

        // ----- PartitionAwareKey methods ----------------------------------

        @Override
        public int getPartitionId()
            {
            return m_nPartition;
            }

        // ----- PortableObject methods -------------------------------------

        @Override
        public void readExternal(PofReader in) throws IOException
            {
            m_nPartition = in.readInt(0);
            m_nChannel   = in.readInt(1);
            m_groupId    = in.readObject(2);
            }

        @Override
        public void writeExternal(PofWriter out) throws IOException
            {
            out.writeInt(0, m_nPartition);
            out.writeInt(1, m_nChannel);
            out.writeObject(2, m_groupId);
            }

        // ----- object methods ---------------------------------------------

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

            Key that = (Key) o;
            return m_nPartition == that.m_nPartition &&
                   m_nChannel   == that.m_nChannel   &&
                   Objects.equals(m_groupId, that.m_groupId);
            }

        @Override
        public int hashCode()
            {
            int hash = HashHelper.hash(m_nPartition, m_nChannel);

            return HashHelper.hash(m_groupId, hash);
            }

        @Override
        public String toString()
            {
            return "SubscriberPartitionKey(Partition=" + m_nPartition +
                    ", channel=" + m_nChannel +
                    ", subscriberGroup='" + m_groupId + "')";
            }

        // ----- data members -----------------------------------------------

        /**
         * The partition id of this key.
         */
        private int m_nPartition;

        /**
         * The channel id.
         */
        private int m_nChannel;

        /**
         * The subscriber that this key is for.
         */
        private SubscriberGroupId m_groupId;
        }

    // ----- inner class HeadExtractor --------------------------------------

    /**
     * A {@link ValueExtractor} that can extract the head position from
     * a {@link Subscription} for a given subscriber identifier.
     */
    public static class HeadExtractor
            implements ValueExtractor<Subscription, PagedPosition>, PortableObject
        {
        /**
         * Default constructor for serialization.
         */
        @SuppressWarnings("unused")
        public HeadExtractor()
            {
            }

        /**
         * Create a head extractor for the specified subscriber.
         *
         * @param nSubscriberId  the subscriber identifier
         */
        public HeadExtractor(long nSubscriberId)
            {
            m_nSubscriberId = nSubscriberId;
            }

        @Override
        public PagedPosition extract(Subscription subscription)
            {
            PagedPosition position;
            if (subscription.getOwningSubscriber() == m_nSubscriberId)
                {
                position = subscription.getHeadPosition();
                }
            else
                {
                position = subscription.getRollbackPosition();
                if (position.getPage() == Page.EMPTY)
                    {
                    position = subscription.getHeadPosition();
                    }
                }

            if (position.getOffset() == Integer.MAX_VALUE && position.getPage() != Page.EMPTY)
                {
                position = new PagedPosition(position.getPage() + 1, 0);
                }

            return position.getPage() == Page.EMPTY ? null : position;
            }

        // ----- PortableObject methods -------------------------------------

        @Override
        public void readExternal(PofReader in) throws IOException
            {
            m_nSubscriberId = in.readLong(0);
            }

        @Override
        public void writeExternal(PofWriter out) throws IOException
            {
            out.writeLong(0, m_nSubscriberId);
            }

        // ----- data members -----------------------------------------------

        /**
         * The subscriber identifier.
         */
        private long m_nSubscriberId;
        }

    // ----- constants ------------------------------------------------------

    /**
     * {@link EvolvablePortableObject} data version of this class.
     */
    public static final int DATA_VERSION = 2;

    // ----- data members ---------------------------------------------------

    /**
     * The head page across all partitions for this subscriber.
     *
     * Note: this value is only maintained within a single well known partition once the subscription has been initialized.
     */
    private long m_lHeadSubscription = Page.NULL_PAGE;

    /**
     * The page of interest to the subscriber within this partition.
     */
    private long m_lPage = Page.NULL_PAGE;

    /**
     * The read position within the active page, or Integer.MAX_VALUE when the current page is exhausted, but another has
     * yet to arrive for this partition.
     */
    private int m_nPosition;

    /**
     * The last committed position or a position with a {@link Page#NULL_PAGE} page if there have
     * been no commits.
     */
    private PagedPosition m_posCommitted = PagedPosition.NULL_POSITION;

    /**
     * The position to rollback to on reconnection.
     * This is typically the position after the committed page position, but could be a
     * {@link Page#NULL_PAGE} if the next page is unknown.
     */
    private PagedPosition m_posRollback = PagedPosition.NULL_POSITION;

    /**
     * The subscriber filter, or null.
     */
    private Filter<?> m_filter;

    /**
     * The optional converter to use to convert the value before returning to it the subscriber.
     */
    private Function<?, ?> m_fnConvert;

    /**
     * The subscriber owning this channel.
     */
    private long m_nOwningSubscriber;

    /**
     * The set of subscriber ids.
     * <p>
     * This is a sorted set to ensure that subscribers are always iterated over in a consistent order.
     * <p>
     * This set is typically only populated for channel zero.
     */
    private SortedSet<Long> m_setSubscriber;

    /**
     * The an array of subscriber identifiers assigned to channels, the channel number being the index into the array.
     * <p>
     * This set is typically only populated for channel zero.
     */
    private long[] m_aChannel;
    }
