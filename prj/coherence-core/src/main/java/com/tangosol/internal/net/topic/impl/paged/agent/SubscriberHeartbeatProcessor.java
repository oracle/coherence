/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic.impl.paged.agent;

import com.tangosol.internal.net.topic.impl.paged.PagedTopicPartition;

import com.tangosol.internal.net.topic.impl.paged.model.SubscriberInfo;

import com.tangosol.io.pof.EvolvablePortableObject;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;

import com.tangosol.util.BinaryEntry;
import com.tangosol.util.InvocableMap;

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
    @SuppressWarnings("rawtypes")
    SubscriberHeartbeatProcessor(Function<BinaryEntry, PagedTopicPartition> supplier)
        {
        super(supplier);
        }

    // ----- AbstractProcessor methods --------------------------------------

    @Override
    public Void process(InvocableMap.Entry<SubscriberInfo.Key, SubscriberInfo> entry)
        {
        ensureTopic(entry).heartbeat(entry);
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
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        }

    // ----- constants ------------------------------------------------------

    /**
     * {@link EvolvablePortableObject} data version of this class.
     */
    public static final int DATA_VERSION = 1;
    }
