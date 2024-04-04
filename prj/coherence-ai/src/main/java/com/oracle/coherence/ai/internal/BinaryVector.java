/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.ai.internal;

import com.tangosol.io.AbstractEvolvable;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.ReadBuffer;

import com.tangosol.io.pof.EvolvablePortableObject;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;

import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;

import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.json.bind.annotation.JsonbTransient;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.Objects;
import java.util.Optional;

/**
 * A representation of a binary vector and its metadata.
 * <p/>
 * This is the class that is used for the value in a vector store cache.
 * <p/>
 * The vector is always kept as binary data and is actually added to the serialized
 * binary cache value as a decoration with the id {@link ExternalizableHelper#DECO_VECTOR}.
 * <p/>
 * Any metadata is lazily deserialized. The metadata can be serialized with any named
 * serializer configured in Coherence. The {@link #m_sFormat} field holds the name of
 * the serializer. This means that the metadata is never deserialized on the server
 * unless it is actually required, for example in filtering.
 */
public class BinaryVector
        extends AbstractEvolvable
        implements ExternalizableLite, EvolvablePortableObject, ExternalizableHelper.DecorationAware
    {
    /**
     * Default constructor for serialization.
     */
    @SuppressWarnings("unused")
    public BinaryVector()
        {
        }

    /**
     * Create a {@link BinaryVector}.
     *
     * @param binMetadata  the metadata in serialized binary form
     * @param sFormat      the serialization format used for the metadata
     */
    public BinaryVector(ReadBuffer binMetadata, String sFormat)
        {
        this(binMetadata, sFormat, null);
        }

    /**
     * Create a {@link BinaryVector}.
     *
     * @param binMetadata  the metadata in serialized binary form
     * @param sFormat      the serialization format used for the metadata
     * @param binVector    the vector data
     */
    public BinaryVector(ReadBuffer binMetadata, String sFormat, ReadBuffer binVector)
        {
        m_binMetadata = Objects.requireNonNullElse(binMetadata, Binary.NO_BINARY);
        m_sFormat     = sFormat;
        m_binVector   = binVector;
        }

    /**
     * Return the binary vector.
     *
     * @return  the binary vector
     */
    public ReadBuffer getVector()
        {
        return m_binVector;
        }

    /**
     * Return the metadata in object form.
     *
     * @return the metadata in object form
     */
    public Optional<ReadBuffer> getMetadata()
        {
        if (m_binMetadata.length() == 0)
            {
            return Optional.empty();
            }
        return Optional.ofNullable(m_binMetadata);
        }

    /**
     * Return the format of the binary metadata.
     *
     * @return  the format of the binary metadata
     */
    public String getFormat()
        {
        return m_sFormat;
        }

    @Override
    public void storeDecorations(ReadBuffer buffer)
        {
        if (ExternalizableHelper.isDecorated(buffer))
            {
            m_binVector = ExternalizableHelper.getDecoration(buffer, ExternalizableHelper.DECO_VECTOR);
            }
        }

    @Override
    public ReadBuffer applyDecorations(ReadBuffer buffer)
        {
        if (m_binVector != null)
            {
            return ExternalizableHelper.decorate(buffer, ExternalizableHelper.DECO_VECTOR, m_binVector);
            }
        return buffer;
        }

    @Override
    public int getImplVersion()
        {
        return 0;
        }

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        m_sFormat     = in.readString(0);
        m_binMetadata = in.readBinary(1);
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeString(0, m_sFormat);
        out.writeBinary(1, m_binMetadata.toBinary());
        }

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        m_sFormat     = ExternalizableHelper.readSafeUTF(in);
        m_binMetadata = ExternalizableHelper.readObject(in);
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        ExternalizableHelper.writeSafeUTF(out, m_sFormat);
        ExternalizableHelper.writeObject(out, m_binMetadata);
        }

    // ----- data members ---------------------------------------------------

    /**
     * The format of the binary metadata.
     */
    @JsonbProperty("format")
    private String m_sFormat;

    /**
     * The metadata in object form.
     */
    @JsonbProperty("metadata")
    private ReadBuffer m_binMetadata;

    /**
     * The binary vector.
     */
    @JsonbTransient
    private transient ReadBuffer m_binVector;
    }
