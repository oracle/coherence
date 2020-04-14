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

import com.tangosol.util.Filter;
import com.tangosol.util.InvocableMap;

import java.io.IOException;
import java.util.function.Function;

/**
 * The EnsureSubscriberPartitionProcessor ensures that the subscriber is known to each partition
 *
 * @author mf  2016.02.16
 * @since Coherence 14.1.1
 */
public class EnsureSubscriptionProcessor
        extends AbstractPagedTopicProcessor<Subscription.Key, Subscription, long[]>
        implements EvolvablePortableObject
    {
    // ----- constructors ---------------------------------------------------
    
    /**
     * Default constructor (for serialization).
     */
    public EnsureSubscriptionProcessor()
        {
        super(PagedTopicPartition::ensureTopic);
        }

    /**
     * Construct the processor.
     *
     * @param nPhase     the initialization phase
     * @param alPage     the page (by channel) at which to start pinning
     * @param filter     the filter indicating which values are of interest
     * @param fnConvert  the optional converter function to convert values before they are
     *                   returned to subscribers
     */
    public EnsureSubscriptionProcessor(int nPhase, long[] alPage, Filter filter, Function fnConvert)
        {
        super(PagedTopicPartition::ensureTopic);

        m_nPhase    = nPhase;
        m_alPage    = alPage;
        m_filter    = filter;
        m_fnConvert = fnConvert;
        }

    // ----- AbstractProcessor methods --------------------------------------

    @Override
    public long[] process(InvocableMap.Entry<Subscription.Key, Subscription> entry)
        {
        return ensureTopic(entry).ensureSubscription(
                entry.getKey().getGroupId(),
                m_nPhase, m_alPage, m_filter, m_fnConvert);
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
        m_nPhase    = in.readInt(0);
        m_alPage    = in.readLongArray(1);
        m_filter    = in.readObject(2);
        m_fnConvert = in.readObject(3);
        }

    @Override
    public void writeExternal(PofWriter out)
        throws IOException
        {
        out.writeInt(0, m_nPhase);
        out.writeLongArray(1, m_alPage);
        out.writeObject(2, m_filter);
        out.writeObject(3, m_fnConvert);
        }

    // ----- constants ------------------------------------------------------

    /**
     * Mode indicating that we just want to inquire about any pinned page.
     */
    public static final int PHASE_INQUIRE = 0;

    /**
     * Mode indicating that we want to pin pages.
     */
    public static final int PHASE_PIN = 1;

    /**
     * Mode indicating that we want to advance to the specified page.
     */
    public static final int PHASE_ADVANCE = 2;

    /**
     * {@link EvolvablePortableObject} data version of this class.
     */
    public static final int DATA_VERSION = 1;

    // ----- data members ---------------------------------------------------

    /**
     * The initialization phase.
     */
    private int m_nPhase;

    /**
     * The subscriber per-channel page to advance to, or null during inquire
     */
    private long[] m_alPage;

    /**
     * Optional subscriber filter.
     */
    private Filter m_filter;

    /**
     * The optional converter function to convert values before they are returned to subscribers.
     */
    private Function m_fnConvert;
    }
