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
 * This entry processor advances the {@link Usage}
 * publication tail if the new value greater than
 * the current value.
 *
 * @author jk 2015.05.16
 * @since Coherence 14.1.1
 */
public class TailAdvancer
        extends AbstractPagedTopicProcessor<Usage.Key, Usage, Long>
        implements EvolvablePortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor for serialization
     */
    public TailAdvancer()
        {
        super(PagedTopicPartition::ensureTopic);
        }

    /**
     * Create a {@link TailAdvancer}.
     *
     * @param lTailNew    the expected value to compare the current
     *                         tail id value to
     */
    public TailAdvancer(long lTailNew)
        {
        this(lTailNew, PagedTopicPartition::ensureTopic);
        }

    /**
     * Create a {@link TailAdvancer}.
     *
     * @param lTailNew    the new value
     * @param supplier    the {@link Function} to use to provide a {@link PagedTopicPartition} instance
     */
    protected TailAdvancer(long lTailNew, Function<BinaryEntry<Usage.Key, Usage>, PagedTopicPartition> supplier)
        {
        super(supplier);

        m_lTailNew = lTailNew;
        }

    public long getNewTail()
        {
        return m_lTailNew;
        }

    // ----- AbstractProcessor methods --------------------------------------

    /**
     * This method will update the tail page ID of the metadata
     * if the new tail will be greater than the current tail.
     *
     * @param entry  the cache entry containing the {@link Usage} to
     *               be updated.
     *
     * @return the up-to-date tail page id from the {@link Usage} in the entry
     */
    @Override
    public Long process(InvocableMap.Entry<Usage.Key, Usage> entry)
        {
        Usage usage = entry.getValue();

        long lTailCur = usage.getPublicationTail();
        long lTailNew = m_lTailNew;

        if (lTailNew > lTailCur)
            {
            lTailCur = lTailNew;
            usage.setPublicationTail(lTailCur);
            entry.setValue(usage);
            }

        return lTailCur;
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
        m_lTailNew = in.readLong(0);
        }

    @Override
    public void writeExternal(PofWriter out)
            throws IOException
        {
        out.writeLong(0, m_lTailNew);
        }

    // ----- constants ------------------------------------------------------

    /**
     * {@link EvolvablePortableObject} data version of this class.
     */
    public static final int DATA_VERSION = 1;

    // ----- data members ---------------------------------------------------

    /**
     * The expected value of the tail id field in {@link Usage}.
     */
    protected long m_lTailNew;
    }
