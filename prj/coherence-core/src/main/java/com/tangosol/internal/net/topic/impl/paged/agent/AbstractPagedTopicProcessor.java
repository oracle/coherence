/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic.impl.paged.agent;

import com.tangosol.internal.net.topic.impl.paged.PagedTopicPartition;

import com.tangosol.util.BinaryEntry;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.processor.AbstractEvolvableProcessor;

import java.util.function.Function;

/**
 * An {@link AbstractEvolvableProcessor} that provides utility methods
 * for processors operating against topics.
 *
 * @param <K>  the type of the Map entry key
 * @param <V>  the type of the Map entry value
 * @param <R>  the type of value returned by the EntryProcessor
 *
 * @author jk 2015.05.16
 * @since Coherence 14.1.1
 */
public abstract class AbstractPagedTopicProcessor<K, V, R>
        extends AbstractEvolvableProcessor<K, V, R>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create an AbstractTopicProcessor.
     *
     * @param supplier  the {@link Function} to use to  provide a
     *                  {@link PagedTopicPartition} instance
     */
    protected AbstractPagedTopicProcessor(Function<BinaryEntry<K, V>, PagedTopicPartition> supplier)
        {
        f_supplierTopic = supplier;
        }

    // ----- accessors ------------------------------------------------------

    /**
     * Return the topics API version for the caller of this processor.
     *
     * @return the topics API version for the caller of this processor
     */
    public int getApiVersion()
        {
        return getDataVersion();
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Create an instance of a {@link PagedTopicPartition} using the
     * {@link Function} held internally in {@link #f_supplierTopic}.
     *
     * @param entry  the {@link InvocableMap.Entry} toi use to create
     *               the {@link PagedTopicPartition}
     *
     * @return an instance of a {@link PagedTopicPartition}
     */
    protected PagedTopicPartition ensureTopic(InvocableMap.Entry<K, V> entry)
        {
        return f_supplierTopic.apply(entry.asBinaryEntry());
        }

    // ----- data members ---------------------------------------------------

    /**
     * The {@link Function} to use to create instances of {@link PagedTopicPartition}.
     */
    private transient final Function<BinaryEntry<K, V>,PagedTopicPartition> f_supplierTopic;
    }
