/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
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
     * Default constructor (for serialization).
     */
    public DestroySubscriptionProcessor()
        {
        super(PagedTopicPartition::ensureTopic);
        }

    // ----- AbstractProcessor methods --------------------------------------

    @Override
    public Void process(InvocableMap.Entry<Subscription.Key, Subscription> entry)
        {
        ensureTopic(entry).removeSubscription(entry.getKey().getGroupId());
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
        }

    @Override
    public void writeExternal(PofWriter out)
            throws IOException
        {
        }

    // ----- constants ------------------------------------------------------

    /**
     * {@link EvolvablePortableObject} data version of this class.
     */
    public static final int DATA_VERSION = 1;

    /**
     * Singleton destroyer.
     */
    public static final DestroySubscriptionProcessor INSTANCE = new DestroySubscriptionProcessor();
    }
