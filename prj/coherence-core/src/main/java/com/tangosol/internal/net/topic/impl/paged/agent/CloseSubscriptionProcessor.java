/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic.impl.paged.agent;

import com.tangosol.internal.net.topic.impl.paged.PagedTopicPartition;

import com.tangosol.internal.net.topic.impl.paged.model.Subscription;

import com.tangosol.io.pof.EvolvablePortableObject;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;

import com.tangosol.util.InvocableMap;

import java.io.IOException;

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
     *
     * @param nSubscriberId  the subscriber being closed
     */
    public CloseSubscriptionProcessor(long nSubscriberId)
        {
        super(PagedTopicPartition::ensureTopic);
        m_nSubscriberId = nSubscriberId;
        }

    // ----- AbstractProcessor methods --------------------------------------

    @Override
    public long[] process(InvocableMap.Entry<Subscription.Key, Subscription> entry)
        {
        ensureTopic(entry).closeSubscription(entry.getKey().getGroupId(), m_nSubscriberId);
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
        m_nSubscriberId = in.readLong(0);
        }

    @Override
    public void writeExternal(PofWriter out)
        throws IOException
        {
        out.writeObject(0, m_nSubscriberId);
        }

    // ----- constants ------------------------------------------------------

    /**
     * {@link EvolvablePortableObject} data version of this class.
     */
    public static final int DATA_VERSION = 1;

    // ----- data members ---------------------------------------------------

    /**
     * The subscriber identifier.
     */
    private long m_nSubscriberId;
    }
