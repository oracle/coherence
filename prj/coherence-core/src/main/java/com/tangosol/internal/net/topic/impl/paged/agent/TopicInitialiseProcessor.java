/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic.impl.paged.agent;

import com.tangosol.internal.net.topic.impl.paged.PagedTopicPartition;
import com.tangosol.internal.net.topic.impl.paged.model.Usage;

import com.tangosol.io.pof.EvolvablePortableObject;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;

import com.tangosol.util.BinaryEntry;
import com.tangosol.util.InvocableMap;

import java.io.IOException;

import java.util.function.Function;

/**
 * This entry processor initialises a topic.
 *
 * @author jk 2015.06.19
 * @since Coherence 14.1.1
 */
public class TopicInitialiseProcessor
        extends AbstractPagedTopicProcessor<Usage.Key, Usage, Long>
        implements EvolvablePortableObject
    {
    // ----- constructors ---------------------------------------------------

    public TopicInitialiseProcessor()
        {
        this(PagedTopicPartition::ensureTopic);
        }

    public TopicInitialiseProcessor(Function<BinaryEntry<Usage.Key, Usage>, PagedTopicPartition> supplier)
        {
        super(supplier);
        }

    // ----- AbstractProcessor methods --------------------------------------

    @Override
    public Long process(InvocableMap.Entry<Usage.Key, Usage> entry)
        {
        return ensureTopic(entry).initialiseTopic((BinaryEntry<Usage.Key, Usage>) entry);
        }

    // ----- EvolvablePortableObject interface ------------------------------

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
