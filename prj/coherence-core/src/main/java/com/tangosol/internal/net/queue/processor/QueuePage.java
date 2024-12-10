/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net.queue.processor;

import com.tangosol.internal.net.queue.extractor.QueueKeyExtractor;
import com.tangosol.internal.net.queue.model.QueueKey;
import com.tangosol.internal.net.queue.model.QueuePageResult;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.EvolvablePortableObject;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.net.BackingMapContext;
import com.tangosol.net.BackingMapManagerContext;
import com.tangosol.util.Binary;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.Converter;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.ObservableMap;

import javax.json.bind.annotation.JsonbProperty;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

public class QueuePage<K extends QueueKey, E>
        extends AbstractQueueProcessor<K, E, QueuePageResult>
        implements EvolvablePortableObject, ExternalizableLite
    {
    /**
     * Default constructor required for serialization.
     */
    public QueuePage()
        {
        }

    public QueuePage(boolean fHead, long nPageSize, long nLastId, boolean fPoll)
        {
        m_fHead      = fHead;
        m_nPageSize  = nPageSize;
        m_nLastId    = nLastId;
        m_fPoll      = fPoll;
        }

    @Override
    @SuppressWarnings({"unchecked", "deprecation"})
    public QueuePageResult process(InvocableMap.Entry<K, E> entry)
        {
        BinaryEntry<K, E>             binaryEntry = entry.asBinaryEntry();
        QueueKeyExtractor.QueueIndex  index       = assertQueueIndex(binaryEntry);
        BackingMapContext             context     = binaryEntry.getBackingMapContext();
        ObservableMap<Object, Binary> backingMap  = context.getBackingMap();
        BackingMapManagerContext      bmContext   = context.getManagerContext();
        Converter<E, Binary>          toBinary    = bmContext.getValueToInternalConverter();
        Converter<Binary, E>          fromBinary  = bmContext.getValueFromInternalConverter();
        List<Binary>                  listBinary  = new ArrayList<>();
        long                          count       = 0;
        long                          lastId     = 0L;

        SortedMap<Long, Object> map = m_fHead
                ? index.tailMap(m_nLastId)
                : index.headMap(m_nLastId);

        for (Map.Entry<Long, Object> keyEntry : map.entrySet())
            {
            if (count >= m_nPageSize)
                {
                break;
                }
            Object oKey   = keyEntry.getValue();
            Binary binary = null;
            lastId = keyEntry.getKey();
            if (m_fPoll)
                {
                InvocableMap.Entry<QueueKey, E> entryPoll = context.getBackingMapEntry(oKey);
                if (entryPoll.isPresent())
                    {
                    binary = entryPoll.asBinaryEntry().getBinaryValue();
                    entryPoll.remove(false);
                    }
                }
            else
                {
                binary = backingMap.get(oKey);
                }
            if (binary != null)
                {
                listBinary.add(binary);
                count++;
                }
            }

        return new QueuePageResult(lastId, listBinary);
        }

    @Override
    public int getImplVersion()
        {
        return IMPL_VERSION;
        }

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        m_fHead     = in.readBoolean(0);
        m_nLastId   = in.readLong(1);
        m_nPageSize = in.readLong(2);
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeBoolean(0, m_fHead);
        out.writeLong(1, m_nLastId);
        out.writeLong(2, m_nPageSize);
        }

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        m_fHead     = in.readBoolean();
        m_nLastId   = in.readLong();
        m_nPageSize = in.readLong();
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        out.writeBoolean(m_fHead);
        out.writeLong(m_nLastId);
        out.writeLong(m_nPageSize);
        }

    // ----- helper methods -------------------------------------------------


    // ----- data members ---------------------------------------------------

    /**
     * The {@link EvolvablePortableObject} implementation version.
     */
    public static final int IMPL_VERSION = 1;

    @JsonbProperty("head")
    private boolean m_fHead;

    @JsonbProperty("size")
    private long m_nPageSize;

    @JsonbProperty("lastId")
    private long m_nLastId;

    @JsonbProperty("isPoll")
    private boolean m_fPoll;
    }
