/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic.impl.paged.model;

import com.tangosol.internal.net.topic.impl.paged.PagedTopicSubscriber;
import com.tangosol.io.AbstractEvolvable;

import com.tangosol.io.pof.EvolvablePortableObject;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.util.UUID;
import com.tangosol.util.ValueExtractor;
import com.tangosol.util.extractor.EntryExtractor;

import java.io.IOException;

import java.time.LocalDateTime;

import java.util.Map;
import java.util.Objects;

/**
 * Information about a subscriber to a topic.
 * <p>
 * Subscriber info entries are cached with an expiry delay so that when they
 * expire they effectively signal that the subscriber has died, or deadlocked,
 * or just stopped calling receive. For durable subscribers in groups this avoids
 * the case where a channel is starved of subscribers as the timed-out subscriber's
 * channel allocation will be reallocated to remaining live subscribers in the same
 * group.
 * <p>
 * Subscribers send a heartbeat on every call to receive that resets the expiry time
 * of the subscriber's info entry.
 *
 * @author Jonathan Knight 2021.04.27
 * @since 21.06
 */
public class SubscriberInfo
        extends AbstractEvolvable
        implements EvolvablePortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor for serialization.
     */
    public SubscriberInfo()
        {
        }

    // ----- accessors ------------------------------------------------------

    /**
     * Returns the timestamp of the last heartbeat.
     *
     * @return the timestamp of the last heartbeat
     */
    public LocalDateTime getLastHeartbeat()
        {
        return m_dtLastHeartbeat;
        }

    /**
     * Set the timestamp of the last heartbeat.
     *
     * @param timestamp  the timestamp of the last heartbeat
     */
    public void setLastHeartbeat(LocalDateTime timestamp)
        {
        m_dtLastHeartbeat = timestamp;
        }

    /**
     * Returns the maximum time (in millis) allowed between heartbeats before
     * the subscriber expires.
     *
     * @return the maximum time (in millis) allowed between heartbeats before
     *         the subscriber expires
     */
    public long getTimeoutMillis()
        {
        return m_cTimeoutMillis;
        }

    /**
     * Set the maximum time (in millis) allowed between heartbeats before
     * the subscriber expires.
     *
     * @param cTimeoutMillis  the maximum time (in millis) allowed between
     *                        heartbeats before the subscriber expires
     */
    public void setTimeoutMillis(long cTimeoutMillis)
        {
        m_cTimeoutMillis = cTimeoutMillis;
        }

    /**
     * Returns the {@link UUID} of the owning member.
     *
     * @return the {@link UUID} of the owning member
     */
    public UUID getOwningUid()
        {
        return m_owningUid;
        }

    /**
     * Set the {@link UUID} of the owning member.
     *
     * @param uuid  the {@link UUID} of the owning member
     */
    public void setOwningUid(UUID uuid)
        {
        m_owningUid = uuid;
        }

    /**
     * Return the unique identifier of the subscription.
     *
     * @return the unique identifier of the subscription
     */
    public long getSubscriptionId()
        {
        return m_lSubscriptionId;
        }

    /**
     * Set the unique identifier of the subscription.
     *
     * @param lSubscriptionId  the unique identifier of the subscription
     */
    public void setSubscriptionId(long lSubscriptionId)
        {
        m_lSubscriptionId = lSubscriptionId;
        }

    /**
     * Returns the subscriber's connection timestamp.
     *
     * @return the subscriber's connection timestamp
     */
    public long getConnectionTimestamp()
        {
        return m_lConnectionTimestamp;
        }

    /**
     * Set the subscriber's connection timestamp.
     *
     * @param lTimestamp  the subscriber's connection timestamp
     */
    public void setlConnectionTimestamp(long lTimestamp)
        {
        m_lConnectionTimestamp = lTimestamp;
        }


// ----- EvolvablePortableObject methods --------------------------------

    @Override
    public int getImplVersion()
        {
        return DATA_VERSION;
        }

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        int nVersion = in.getVersionId();

        m_dtLastHeartbeat = in.readLocalDateTime(0);
        m_cTimeoutMillis  = in.readLong(1);

        if (nVersion >= 2)
            {
            m_owningUid = in.readObject(2);
            }

        if (nVersion >= 3)
            {
            m_lSubscriptionId      = in.readLong(3);
            m_lConnectionTimestamp = in.readLong(4);
            }
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeDateTime(0, m_dtLastHeartbeat);
        out.writeLong(1, m_cTimeoutMillis);
        out.writeObject(2, m_owningUid);
        out.writeLong(3, m_lSubscriptionId);
        out.writeLong(4, m_lConnectionTimestamp);
        }

    // ----- Object methods ---------------------------------------------

    @Override
    public String toString()
        {
        return "SubscriberInfo(" +
                "lastHeartbeat=" + m_dtLastHeartbeat +
                "uid=" + m_owningUid +
                "subscription=" + m_lSubscriptionId +
                "connectedTime=" + m_lConnectionTimestamp +
                ')';
        }

    // ----- inner class Key ------------------------------------------------

    /**
     * The key class for the subscriber info cache.
     */
    public static class Key
            implements PortableObject
        {
        // ----- constructors -----------------------------------------------

        /**
         * Default constructor for serialization.
         */
        public Key()
            {
            }

        /**
         * Create a {@link Key}.
         *
         * @param groupId       the subscriber group identifier
         * @param subscriberId  the subscriber identifier
         */
        public Key(SubscriberGroupId groupId, long subscriberId)
            {
            m_groupId      = groupId;
            m_subscriberId = subscriberId;
            }

        // ----- accessors --------------------------------------------------

        /**
         * Returns the subscriber's group identifier.
         *
         * @return the subscriber's group identifier
         */
        public SubscriberGroupId getGroupId()
            {
            return m_groupId;
            }

        /**
         * Returns the subscriber's unique identifier.
         *
         * @return the subscriber's unique identifier
         */
        public long getSubscriberId()
            {
            return m_subscriberId;
            }

        // ----- PortableObject implementation ------------------------------

        @Override
        public void readExternal(PofReader in) throws IOException
            {
            m_groupId      = in.readObject(0);
            m_subscriberId = in.readLong(1);
            }

        @Override
        public void writeExternal(PofWriter out) throws IOException
            {
            out.writeObject(0, m_groupId);
            out.writeLong(1, m_subscriberId);
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
            return m_subscriberId == key.m_subscriberId && Objects.equals(m_groupId, key.m_groupId);
            }

        @Override
        public int hashCode()
            {
            return Objects.hash(m_groupId, m_subscriberId);
            }

        @Override
        public String toString()
            {
            return "SubscriberInfo.Key(" +
                    "groupId=" + m_groupId +
                    ", subscriberId=" + m_subscriberId +
                    ", memberId=" + PagedTopicSubscriber.memberIdFromId(m_subscriberId) +
                    ')';
            }

        // ----- data members -----------------------------------------------

        /**
         * The subscriber group identifier.
         */
        private SubscriberGroupId m_groupId;

        /**
         * The unique identifier for the subscriber.
         */
        private long m_subscriberId;
        }

    // ----- inner class: GroupIdExtractor ----------------------------------

    /**
     * An {@link EntryExtractor} that can extract the {@link SubscriberGroupId} from a {@link Key}.
     */
    public static class GroupIdExtractor
            extends EntryExtractor
            implements PortableObject
        {
        @Override
        @SuppressWarnings("rawtypes")
        public Object extractFromEntry(Map.Entry entry)
            {
            return ((Key) entry.getKey()).m_groupId;
            }

        /**
         * A singleton instance of {@link GroupIdExtractor}.
         */
        @SuppressWarnings("unchecked")
        public static ValueExtractor<Map.Entry<Key, SubscriberInfo>, SubscriberGroupId> INSTANCE = new GroupIdExtractor();
        }

    // ----- inner class: MemberIdExtractor ---------------------------------

    /**
     * An {@link EntryExtractor} that can extract the cluster member id from a {@link Key}.
     */
    public static class MemberIdExtractor
            extends EntryExtractor
            implements PortableObject
        {
        @Override
        @SuppressWarnings("rawtypes")
        public Object extractFromEntry(Map.Entry entry)
            {
            return PagedTopicSubscriber.memberIdFromId(((Key) entry.getKey()).m_subscriberId);
            }

        /**
         * A singleton instance of {@link GroupIdExtractor}.
         */
        @SuppressWarnings("unchecked")
        public static ValueExtractor<Map.Entry<Key, SubscriberInfo>, Integer> INSTANCE = new MemberIdExtractor();
        }

    // ----- constants ------------------------------------------------------

    /**
     * {@link EvolvablePortableObject} data version of this class.
     */
    public static final int DATA_VERSION = 3;

    // ----- data members ---------------------------------------------------

    /**
     * The timestamp of the subscriber's connection.
     */
    private long m_lConnectionTimestamp;

    /**
     * The last heartbeat time.
     */
    private LocalDateTime m_dtLastHeartbeat;

    /**
     * The subscriber timeout value.
     */
    private long m_cTimeoutMillis;

    /**
     * The {@link UUID} of the owning member.
     */
    private UUID m_owningUid;

    /**
     * The unique identifier of the subscription the subscriber is subscribed to.
     */
    private long m_lSubscriptionId;
    }
