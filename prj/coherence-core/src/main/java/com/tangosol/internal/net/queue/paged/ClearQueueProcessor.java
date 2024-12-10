/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net.queue.paged;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.InvocableMap;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * An entry processor to clear the contents of a paged queue.
 */
public class ClearQueueProcessor
        extends BasePagedQueueProcessor<Void>
    {
    /**
     * Default constructor for serialization.
     */
    public ClearQueueProcessor()
        {
        }

    /**
     * Create a {@link ClearQueueProcessor}.
     *
     * @param fSynthetic  {@code true} if the deletions should be synthetic.
     */
    public ClearQueueProcessor(boolean fSynthetic)
        {
        m_fSynthetic = fSynthetic;
        }

    @Override
    public Void process(InvocableMap.Entry<Integer, Bucket> entry)
        {
        if (entry.isPresent())
            {
            Bucket                                 bucket       = entry.getValue();
            BinaryEntry<Integer, Bucket>           binaryEntry  = entry.asBinaryEntry();
            BinaryEntry<Integer, QueueVersionInfo> versionEntry = getVersionBinaryEntry(binaryEntry);

            versionEntry.setValue(bucket.getVersion());

            String        cacheName         = binaryEntry.getBackingMapContext().getCacheName();
            String        elementsCacheName = PagedQueueCacheNames.Elements.getCacheName(cacheName);
            int           nHead             = bucket.getHead();
            int           nTail             = bucket.getTail();
            PagedQueueKey key               = new PagedQueueKey(bucket.getId(), nHead);

            for (int i = nHead; i <= nTail; i++)
                {
                key.setElementId(i);
                BinaryEntry<?, ?> elementEntry = binaryEntry.getAssociatedEntry(elementsCacheName, key);
                if (elementEntry.isPresent())
                    {
                    elementEntry.remove(m_fSynthetic);
                    }
                }

            entry.remove(m_fSynthetic);
            }
        return null;
        }

    @Override
    public int getImplVersion()
        {
        return POF_IMPL_VERSION;
        }

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        m_fSynthetic = in.readBoolean(0);
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeBoolean(0, m_fSynthetic);
        }

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        m_fSynthetic = in.readBoolean();
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        out.writeBoolean(m_fSynthetic);
        }

    // ----- data members ---------------------------------------------------

    public static final int POF_IMPL_VERSION = 0;

    /**
     * Whether the deletions are synthetic.
     */
    private boolean m_fSynthetic;
    }
