/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net.queue.paged;

import com.tangosol.internal.net.queue.model.QueueKey;
import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.InvocableMap;
import com.tangosol.util.processor.AbstractProcessor;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * This entry processor is used to initialise an entry in the queue info
 * cache. The process method will insert a new {@link QueueInfo} value into
 * the cache if there is not already a value present for the key the entry
 * processor is executing against.
 * <p/>
 * The value returned will be the new {@link QueueInfo} value if one was
 * added or the existing {@link QueueInfo} value if one was already present.
 */
public class InitialiseQueueInfoProcessor
        extends AbstractProcessor<String,QueueInfo,QueueInfo>
        implements ExternalizableLite, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor for serialization.
     */
    public InitialiseQueueInfoProcessor()
        {
        }

    // ----- AbstractProcessor methods --------------------------------------

    /**
     * Add a new  {@link QueueInfo} to the cache if there is not already an entry
     * present.
     *
     * @param entry the cache entry to set the new {@link QueueInfo} value into
     *
     * @return the current {@link QueueInfo} value.
     */
    @Override
    public QueueInfo process(InvocableMap.Entry<String,QueueInfo> entry)
        {
        if (!entry.isPresent())
            {
            QueueInfo info = instantiateQueueInfo(entry);
            entry.setValue(info);
            }

        return entry.getValue();
        }

    // ----- PortableObject methods -----------------------------------------

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        }

    // ----- ExternalizableLite methods -------------------------------------

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        }

    // ----- helper methods -------------------------------------------------

    protected QueueInfo instantiateQueueInfo(InvocableMap.Entry<String,QueueInfo> entry)
        {
        // we base the head on the hash of the queue name so that not every queue starts in the same partition
        String sName = entry.getKey();
        int    nHash = Math.abs(QueueKey.calculateQueueHash(sName));
        int    nHead = nHash % 1000;
        return new QueueInfo(sName, nHead, nHead, new QueueVersionInfo());
        }

    // ----- constants ------------------------------------------------------

    /**
     * An instance of InitialiseQueueInfoProcessor.
     */
    public static final InitialiseQueueInfoProcessor INSTANCE = new InitialiseQueueInfoProcessor();
    }
