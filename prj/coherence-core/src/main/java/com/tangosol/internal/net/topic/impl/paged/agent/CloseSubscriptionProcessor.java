/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic.impl.paged.agent;

import com.tangosol.internal.net.topic.impl.paged.PagedTopicPartition;

import com.tangosol.internal.net.topic.impl.paged.model.SubscriberId;
import com.tangosol.internal.net.topic.impl.paged.model.Subscription;

import com.tangosol.io.pof.EvolvablePortableObject;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;

import com.tangosol.util.InvocableMap;

import java.io.IOException;
import java.util.Objects;

/**
 * An entry processor to close a topic subscription.
 *
 * @author Jonathan Knight 2021.04.27
 * @since 21.06
 */
public class CloseSubscriptionProcessor
        extends AbstractPagedTopicProcessor<Subscription.Key, Subscription, long[]>
        implements EvolvablePortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor (for serialization).
     */
    public CloseSubscriptionProcessor()
        {
        super(PagedTopicPartition::ensureTopic);
        }

    /**
     * Construct the processor.
     * <p>
     * Passing {@link SubscriberId#NullSubscriber} as the subscriber identifier
     * will cause all subscribers to be disconnected.
     *
     * @param subscriberId  the subscriber being closed
     *
     * @throws NullPointerException if the subscriber identifier is {@code null}
     */
    public CloseSubscriptionProcessor(SubscriberId subscriberId)
        {
        super(PagedTopicPartition::ensureTopic);
        m_subscriberId = Objects.requireNonNull(subscriberId);
        }

    // ----- AbstractProcessor methods --------------------------------------

    @Override
    public long[] process(InvocableMap.Entry<Subscription.Key, Subscription> entry)
        {
        ensureTopic(entry).closeSubscription(entry.getKey(), m_subscriberId);
        return null;
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
        int  nVersion = in.getVersionId();
        long nId      = in.readLong(0);

        if (nVersion >= 2)
            {
            m_subscriberId = in.readObject(1);
            }
        else
            {
            m_subscriberId = new SubscriberId(nId, null);
            }
        }

    @Override
    public void writeExternal(PofWriter out)
        throws IOException
        {
        out.writeObject(0, m_subscriberId.getId());
        out.writeObject(1, m_subscriberId);
        }

    // ----- constants ------------------------------------------------------

    /**
     * {@link EvolvablePortableObject} data version of this class.
     */
    public static final int DATA_VERSION = 2;

    // ----- data members ---------------------------------------------------

    /**
     * The subscriber identifier.
     */
    private SubscriberId m_subscriberId;
    }
