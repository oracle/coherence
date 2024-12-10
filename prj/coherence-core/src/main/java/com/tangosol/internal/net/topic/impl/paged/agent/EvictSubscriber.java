/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic.impl.paged.agent;

import com.tangosol.internal.net.topic.impl.paged.model.SubscriberInfo;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;

import com.tangosol.util.InvocableMap;

import com.tangosol.util.processor.AbstractEvolvableProcessor;

import java.io.IOException;

/**
 * An entry processor to evict a subscriber entry.
 */
public class EvictSubscriber
        extends AbstractEvolvableProcessor<SubscriberInfo.Key,SubscriberInfo, Boolean>
    {
    @Override
    public Boolean process(InvocableMap.Entry<SubscriberInfo.Key, SubscriberInfo> entry)
        {
        if (entry.isPresent())
            {
            entry.remove(true);
            return true;
            }
        return false;
        }

    @Override
    public int getImplVersion()
        {
        return 0;
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
     * A singleton instance of an {@link EvictSubscriber} processor.
     */
    public static final EvictSubscriber INSTANCE = new EvictSubscriber();
    }
