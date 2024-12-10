/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic.impl.paged.agent;

import com.tangosol.internal.net.topic.impl.paged.PagedTopicPartition;

import com.tangosol.internal.net.topic.impl.paged.model.SubscriberInfo;

import com.tangosol.io.pof.EvolvablePortableObject;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;

import com.tangosol.util.BinaryEntry;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.UUID;

import java.io.IOException;

import java.util.function.Function;

/**
 * An entry processor that registers a heart beat for a subscriber.
 *
 * @author Jonathan Knight 2021.04.27
 * @since 21.06
 */
public class SubscriberHeartbeatProcessor
        extends AbstractPagedTopicProcessor<SubscriberInfo.Key, SubscriberInfo, Void>
        implements EvolvablePortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor for serialization.
     */
    public SubscriberHeartbeatProcessor()
        {
        this(PagedTopicPartition::ensureTopic);
        }

    /**
     * Create a {@link SubscriberHeartbeatProcessor}.
     *
     * @param supplier  the supplier to provide a {@link PagedTopicPartition} from a {@link BinaryEntry}
     */
    SubscriberHeartbeatProcessor(Function<BinaryEntry<SubscriberInfo.Key, SubscriberInfo>, PagedTopicPartition> supplier)
        {
        super(supplier);
        }

    // ----- accessors ------------------------------------------------------

    /**
     * Return the UUID of the subscriber's owning member.
     *
     * @return the UUID of the subscriber's owning member
     */
    public UUID getUuid()
        {
        return m_uuid;
        }

    /**
     * Set the UUID of the subscriber's owning member.
     *
     * @param uuid  the UUID of the subscriber's owning member
     */
    public void setUuid(UUID uuid)
        {
        m_uuid = uuid;
        }

    /**
     * Returns the unique identifier of the subscriber's subscription.
     *
     * @return the unique identifier of the subscriber's subscription
     */
    public long getSubscription()
        {
        return m_lSubscription;
        }

    /**
     * Set the unique identifier of the subscriber's subscription.
     *
     * @param lSubscription  the unique identifier of the subscriber's subscription
     */
    public void setSubscription(long lSubscription)
        {
        m_lSubscription = lSubscription;
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

    // ----- AbstractProcessor methods --------------------------------------

    @Override
    public Void process(InvocableMap.Entry<SubscriberInfo.Key, SubscriberInfo> entry)
        {
        ensureTopic(entry).heartbeat(entry, this);
        return null;
        }

    @Override
    public int getImplVersion()
        {
        return DATA_VERSION;
        }

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        int nVersion = getDataVersion();
        if (nVersion >= 2)
            {
            m_uuid                 = in.readObject(0);
            m_lSubscription        = in.readLong(1);
            m_lConnectionTimestamp = in.readLong(2);
            }
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeObject(0, m_uuid);
        out.writeLong(1, m_lSubscription);
        out.writeLong(2, m_lConnectionTimestamp);
        }

    // ----- constants ------------------------------------------------------

    /**
     * {@link EvolvablePortableObject} data version of this class.
     */
    public static final int DATA_VERSION = 2;

    // ----- data members ---------------------------------------------------

    /**
     * The UUID of the member owning the subscriber
     */
    private UUID m_uuid;

    /**
     * The unique subscription
     */
    private long m_lSubscription;

    /**
     * The subscriber's connection timestamp.
     */
    private long m_lConnectionTimestamp;
    }
