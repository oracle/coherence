/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic.impl.paged.agent;

import com.tangosol.internal.net.topic.impl.paged.model.Subscription;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.EvolvablePortableObject;

import com.tangosol.util.InvocableMap;
import com.tangosol.util.processor.AbstractEvolvableProcessor;

import java.io.IOException;

/**
 * This entry processor advances the page for the subscriber
 * if the supplied value is greater then the current value.
 *
 * @author jk 2015.05.16
 * @since Coherence 14.1.1
 */
public class HeadAdvancer
        extends AbstractEvolvableProcessor<Subscription.Key, Subscription, Long>
        implements EvolvablePortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor for serialization
     */
    public HeadAdvancer()
        {
        }

    /**
     * Create a new SubscriptionHeadAdvancer.
     *
     * @param lNewHead    the new head value
     */
    public HeadAdvancer(long lNewHead)
        {
        m_lNewHead = lNewHead;
        }

    // ----- AbstractProcessor methods --------------------------------------

    @SuppressWarnings("unchecked")
    @Override
    public Long process(InvocableMap.Entry<Subscription.Key, Subscription> entry)
        {
        Subscription subscriberPartition = entry.getValue();
        if (subscriberPartition == null)
            {
            throw new IllegalStateException("unknown subscriber");
            }

        long lHeadCur = subscriberPartition.getSubscriptionHead();
        long lHeadNew = m_lNewHead;
        if (lHeadNew > lHeadCur)
            {
            subscriberPartition.setSubscriptionHead(lHeadNew);
            entry.setValue(subscriberPartition);
            }

        return lHeadCur; // return the prior value, this allows the caller to infer if their CAS succeeded
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
        m_lNewHead = in.readLong(0);
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeLong(0, m_lNewHead);
        }

    // ----- constants ------------------------------------------------------

    /**
     * {@link EvolvablePortableObject} data version of this class.
     */
    public static final int DATA_VERSION = 1;

    // ----- data members ---------------------------------------------------

    /**
     * The new head value.
     */
    protected long m_lNewHead;
    }
