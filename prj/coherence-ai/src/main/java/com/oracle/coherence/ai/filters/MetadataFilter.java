/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.ai.filters;

import com.oracle.coherence.ai.Converters;
import com.oracle.coherence.ai.internal.BinaryVector;
import com.tangosol.io.AbstractEvolvable;
import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.ReadBuffer;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.net.cache.BackingMapBinaryEntry;

import com.tangosol.util.Binary;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.Filter;
import com.tangosol.util.InvocableMapHelper;
import com.tangosol.util.filter.EntryFilter;

import jakarta.json.bind.annotation.JsonbProperty;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.Map;

/**
 * An {@link EntryFilter} that wraps another {@link Filter}
 * that is applied to a vector entry metadata.
 *
 * @param <T>  the type of value the filter is applied to
 */
public class MetadataFilter<T>
        extends AbstractEvolvable
        implements EntryFilter<BinaryVector, T>, ExternalizableLite, PortableObject
    {
    /**
     * Default constructor for serialization.
     */
    public MetadataFilter()
        {
        }

    /**
     * Create a {@link MetadataFilter}.
     *
     * @param wrapped  the actual {@link Filter} that will be applied to the metadata
     */
    public MetadataFilter(Filter<?> wrapped)
        {
        m_wrapped = wrapped;
        }

    @Override
    public boolean evaluate(T o)
        {
        return false;
        }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public boolean evaluateEntry(Map.Entry entry)
        {
        BinaryEntry binaryEntry = (BinaryEntry) entry;
        ReadBuffer  bufMetadata = Converters.extractMetadata(binaryEntry.getBinaryValue());

        if (bufMetadata != null)
            {
            Binary      binMetadata   = bufMetadata.toBinary();
            BinaryEntry entryMetadata = new BackingMapBinaryEntry(binaryEntry.getBinaryKey(), binMetadata,
                                                    binMetadata, binaryEntry.getContext());
            return InvocableMapHelper.evaluateEntry(m_wrapped, entryMetadata);
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
        m_wrapped = in.readObject(0);
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeObject(0, m_wrapped);
        }

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        m_wrapped = ExternalizableHelper.readObject(in);
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        ExternalizableHelper.writeObject(out, m_wrapped);
        }

    // ----- data members ---------------------------------------------------

    /**
     * The actual {@link Filter} that will be applied to the metadata.
     */
    @JsonbProperty("wrapped")
    private Filter<?> m_wrapped;
    }
