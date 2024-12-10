/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
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
 * EntryProcessor which destroys subscribers within a partition.
 *
 * @author mf  2016.02.26
 * @since Coherence 14.1.1
 */
public class DestroySubscriptionProcessor
        extends AbstractPagedTopicProcessor<Subscription.Key, Subscription, Void>
        implements EvolvablePortableObject
    {
    // ----- constructors ---------------------------------------------------
    
    /**
     * Default constructor for serialization.
     */
    public DestroySubscriptionProcessor()
        {
        super(PagedTopicPartition::ensureTopic);
        }


    /**
     * Create a {@link DestroySubscriptionProcessor}.
     *
     * @param lSubscriptionId  the id of the subscription to destroy
     */
    public DestroySubscriptionProcessor(long lSubscriptionId)
        {
        super(PagedTopicPartition::ensureTopic);
        m_lSubscriptionId = lSubscriptionId;
        }

    // ----- AbstractProcessor methods --------------------------------------

    @Override
    public Void process(InvocableMap.Entry<Subscription.Key, Subscription> entry)
        {
        ensureTopic(entry).removeSubscription(entry.getKey().getGroupId(), m_lSubscriptionId);
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
        int nVersion = getDataVersion();
        if (nVersion > 1)
            {
            m_lSubscriptionId = in.readLong(0);
            }
        }

    @Override
    public void writeExternal(PofWriter out)
            throws IOException
        {
        out.writeLong(0, m_lSubscriptionId);
        }

    // ----- constants ------------------------------------------------------

    /**
     * {@link EvolvablePortableObject} data version of this class.
     */
    public static final int DATA_VERSION = 2;

    // ----- data members ---------------------------------------------------

    /**
     * The subscription identifier.
     */
    private long m_lSubscriptionId;
    }
