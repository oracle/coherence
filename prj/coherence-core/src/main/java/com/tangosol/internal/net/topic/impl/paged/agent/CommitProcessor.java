/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic.impl.paged.agent;

import com.tangosol.internal.net.topic.impl.paged.PagedTopicPartition;

import com.tangosol.internal.net.topic.impl.paged.model.PagedPosition;
import com.tangosol.internal.net.topic.impl.paged.model.SubscriberId;
import com.tangosol.internal.net.topic.impl.paged.model.Subscription;

import com.tangosol.io.pof.EvolvablePortableObject;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;

import com.tangosol.net.topic.Subscriber;

import com.tangosol.util.BinaryEntry;
import com.tangosol.util.InvocableMap;

import java.io.IOException;

import java.util.function.Function;

/**
 * An entry processor that commits a position in a topic.
 *
 * @author Jonathan Knight 2021.04.27
 * @since 21.06
 */
public class CommitProcessor
        extends AbstractPagedTopicProcessor<Subscription.Key, Subscription, Subscriber.CommitResult>
        implements EvolvablePortableObject
    {
    /**
     * Default constructor for serialization.
     */
    public CommitProcessor()
        {
        this(PagedTopicPartition::ensureTopic, null, SubscriberId.NullSubscriber);
        }

    /**
     * Create a {@link CommitProcessor} to commit the specified position.
     *
     * @param position      the position to commit
     * @param subscriberId  the identifier of the subscriber performing the commit
     */
    public CommitProcessor(PagedPosition position, SubscriberId subscriberId)
        {
        this(PagedTopicPartition::ensureTopic, position, subscriberId);
        }

    /**
     * Create a {@link CommitProcessor} to commit the specified position.
     *
     * @param supplier      the supplier to provide a {@link PagedTopicPartition} from a {@link BinaryEntry}
     * @param position      the position to commit
     * @param subscriberId  the identifier of the subscriber performing the commit
     */
    CommitProcessor(Function<BinaryEntry<Subscription.Key, Subscription>, PagedTopicPartition> supplier,
                    PagedPosition position, SubscriberId subscriberId)
        {
        super(supplier);
        m_position     = position;
        m_subscriberId = subscriberId;
        }

    // ----- AbstractProcessor methods --------------------------------------

    @Override
    public Subscriber.CommitResult process(InvocableMap.Entry<Subscription.Key, Subscription> entry)
        {
        return ensureTopic(entry).commitPosition((BinaryEntry<Subscription.Key, Subscription>) entry,
                m_position, m_subscriberId);
        }

    @Override
    public int getImplVersion()
        {
        return DATA_VERSION;
        }

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        int  nVersion = in.getVersionId();
        long nId      = in.readLong(0);
        m_position    = in.readObject(1);
        if (nVersion >= 2)
            {
            m_subscriberId = in.readObject(2);
            }
        else
            {
            m_subscriberId = new SubscriberId(nId, null);
            }

        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeLong(0, m_subscriberId.getId());
        out.writeObject(1, m_position);
        out.writeObject(2, m_subscriberId);
        }

    // ----- constants ------------------------------------------------------

    /**
     * {@link EvolvablePortableObject} data version of this class.
     */
    public static final int DATA_VERSION = 2;

    // ----- data members ---------------------------------------------------

    /**
     * The identifier of the subscriber performing the commit.
     */
    private SubscriberId m_subscriberId;

    /**
     * The position to commit.
     */
    private PagedPosition m_position;
    }
