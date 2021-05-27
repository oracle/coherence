/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic.impl.paged.agent;

import com.tangosol.internal.net.topic.impl.paged.PagedTopicPartition;

import com.tangosol.internal.net.topic.impl.paged.model.PagedPosition;
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
        this(PagedTopicPartition::ensureTopic, null, 0);
        }

    /**
     * Create a {@link CommitProcessor} to commit the specified position.
     *
     * @param position       the position to commit
     * @param nSubscriberId  the identifier of the subscriber performing the commit
     */
    public CommitProcessor(PagedPosition position, long nSubscriberId)
        {
        this(PagedTopicPartition::ensureTopic, position, nSubscriberId);
        }

    /**
     * Create a {@link CommitProcessor} to commit the specified position.
     *
     * @param supplier       the supplier to provide a {@link PagedTopicPartition} from a {@link BinaryEntry}
     * @param position       the position to commit
     * @param nSubscriberId  the identifier of the subscriber performing the commit
     */
    @SuppressWarnings("rawtypes")
    CommitProcessor(Function<BinaryEntry, PagedTopicPartition> supplier, PagedPosition position, long nSubscriberId)
        {
        super(supplier);
        m_position      = position;
        m_nSubscriberId = nSubscriberId;
        }

    // ----- AbstractProcessor methods --------------------------------------

    @Override
    public Subscriber.CommitResult process(InvocableMap.Entry<Subscription.Key, Subscription> entry)
        {
        return ensureTopic(entry).commitPosition((BinaryEntry<Subscription.Key, Subscription>) entry,
                m_position, m_nSubscriberId);
        }

    @Override
    public int getImplVersion()
        {
        return DATA_VERSION;
        }

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        m_nSubscriberId = in.readLong(0);
        m_position      = in.readObject(1);
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeLong(0, m_nSubscriberId);
        out.writeObject(1, m_position);
        }

    // ----- constants ------------------------------------------------------

    /**
     * {@link EvolvablePortableObject} data version of this class.
     */
    public static final int DATA_VERSION = 1;

    // ----- data members ---------------------------------------------------

    /**
     * The identifier of the subscriber performing the commit.
     */
    private long m_nSubscriberId;

    /**
     * The position to commit.
     */
    private PagedPosition m_position;
    }
