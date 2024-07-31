/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.ai;

import com.oracle.coherence.common.base.Classes;
import com.tangosol.io.AbstractEvolvable;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.EvolvablePortableObject;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.util.ExternalizableHelper;
import jakarta.json.bind.annotation.JsonbProperty;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A chunk of text extracted from a document.
 */
public final class DocumentChunk
        extends AbstractEvolvable
        implements EvolvablePortableObject, ExternalizableLite
    {
    /**
     * Default constructor for serialization.
     */
    public DocumentChunk()
        {
        }

    /**
     * Create a {@link DocumentChunk}.
     *
     * @param text  the chunk of text extracted from a document
     */
    public DocumentChunk(String text)
        {
        this(text, null, null);
        }

    /**
     * Create a {@link DocumentChunk}.
     *
     * @param text    the chunk of text extracted from a document
     * @param vector  the vector associated with the document chunk
     */
    public DocumentChunk(String text, Float32Vector vector)
        {
        this(text, null, vector);
        }

    /**
     * Create a {@link DocumentChunk}.
     *
     * @param text     the chunk of text extracted from a document
     * @param metadata optional document metadata
     */
    public DocumentChunk(String text, Map<String, Object> metadata)
        {
        this(text, metadata, null);
        }

    /**
     * Create a {@link DocumentChunk}.
     *
     * @param text     the chunk of text extracted from a document
     * @param metadata optional document metadata
     * @param vector   the vector associated with the document chunk
     */
    public DocumentChunk(String text, Map<String, Object> metadata, Float32Vector vector)
        {
        m_text        = text;
        m_mapMetadata = metadata == null ? new LinkedHashMap<>() : metadata;
        m_vector      = vector;
        }

    /**
     * Return the text chunk extracted from the document.
     *
     * @return the text chunk extracted from the document
     */
    public String text()
        {
        return m_text;
        }

    /**
     * Returns the optional metadata associated with this document chunk.
     *
     * @return the optional metadata associated with this document chunk
     */
    public Map<String, Object> metadata()
        {
        return m_mapMetadata;
        }

    /**
     * Return {@code true} if this chunk has a vector embedding.
     *
     * @return {@code true} if this chunk has a vector embedding;
     * {@code false} otherwise
     */
    public boolean isEmbedded()
        {
        return m_vector != null;
        }

    /**
     * Get the float vector for this chunk.
     *
     * @return the float vector for this chunk
     */
    public Vector<float[]> vector()
        {
        return m_vector;
        }

    /**
     * Set the float vector for this chunk.
     *
     * @param vector the float vector for this chunk
     */
    public DocumentChunk setVector(float[] vector)
        {
        return setVector(new Float32Vector(vector));
        }

    /**
     * Set the float vector for this chunk.
     *
     * @param vector the float vector for this chunk
     */
    public DocumentChunk setVector(Float32Vector vector)
        {
        m_vector = vector;
        return this;
        }

    @Override
    public boolean equals(Object obj)
        {
        if (obj == this)
            {
            return true;
            }
        if (obj == null || obj.getClass() != this.getClass())
            {
            return false;
            }
        var that = (DocumentChunk) obj;
        return Objects.equals(m_text, that.m_text) &&
                Objects.equals(m_mapMetadata, that.m_mapMetadata) &&
                Objects.equals(m_vector, that.m_vector);
        }

    @Override
    public int hashCode()
        {
        return Objects.hash(m_text, m_mapMetadata, m_vector);
        }

    @Override
    public String toString()
        {
        return "DocumentChunk[" +
                "text=" + m_text + ", " +
                "metadata=" + m_mapMetadata +
                "vector=" + m_vector +
                ']';
        }

    @Override
    public int getImplVersion()
        {
        return IMPL_VERSION;
        }

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        m_vector      = in.readObject(0);
        m_text        = in.readString(1);
        m_mapMetadata = in.readMap(2, new LinkedHashMap<>());
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeObject(0, m_vector);
        out.writeString(1, m_text);
        out.writeMap(2, m_mapMetadata);
        }

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        m_vector = ExternalizableHelper.readObject(in);
        m_text   = ExternalizableHelper.readSafeUTF(in);
        m_mapMetadata = new LinkedHashMap<>();
        ExternalizableHelper.readMap(in, m_mapMetadata, Classes.getContextClassLoader());
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        ExternalizableHelper.writeObject(out, m_vector);
        ExternalizableHelper.writeSafeUTF(out, m_text);
        ExternalizableHelper.writeMap(out, m_mapMetadata);
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Create an {@link Id}.
     *
     * @param sDocId the document identifier
     * @param nIndex the chunk index
     *
     * @return an {@link Id} with the specified document identifier and chunk index
     */
    public static Id id(String sDocId, int nIndex)
        {
        return new Id(sDocId, nIndex);
        }

    // ----- inner class: Id ------------------------------------------------

    /**
     * A document chunk identifier.
     */
    public static class Id
            implements PortableObject, ExternalizableLite
        {
        /**
         * Default constructor for serialization.
         */
        public Id()
            {
            }

        /**
         * Create a document chunk identifier.
         *
         * @param sDocId  the document identifier
         * @param nIndex  the chunk index
         */
        public Id(String sDocId, int nIndex)
            {
            this.m_sDocId = sDocId;
            this.m_nIndex = nIndex;
            }

        /**
         * Return the document identifier.
         *
         * @return the document identifier
         */
        public String docId()
            {
            return m_sDocId;
            }

        /**
         * Return the document chunk index.
         *
         * @return the document chunk index
         */
        public int index()
            {
            return m_nIndex;
            }

        @Override
        public void readExternal(PofReader in) throws IOException
            {
            m_sDocId = in.readString(0);
            m_nIndex = in.readInt(1);
            }

        @Override
        public void writeExternal(PofWriter out) throws IOException
            {
            out.writeString(0, m_sDocId);
            out.writeInt(1, m_nIndex);
            }

        @Override
        public void readExternal(DataInput in) throws IOException
            {
            m_sDocId = ExternalizableHelper.readSafeUTF(in);
            m_nIndex = ExternalizableHelper.readInt(in);
            }

        @Override
        public void writeExternal(DataOutput out) throws IOException
            {
            ExternalizableHelper.writeSafeUTF(out, m_sDocId);
            ExternalizableHelper.writeInt(out, m_nIndex);
            }

        public String toString()
            {
            if (m_nIndex == 0)
                {
                return m_sDocId;
                }
            return m_sDocId + '#' + m_nIndex;
            }

        @Override
        public boolean equals(Object o)
            {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Id id = (Id) o;
            return m_nIndex == id.m_nIndex && Objects.equals(m_sDocId, id.m_sDocId);
            }

        @Override
        public int hashCode()
            {
            return Objects.hash(m_sDocId, m_nIndex);
            }

        /**
         * Parse a string value into a document chunk id.
         *
         * @param s the string value to parse
         *
         * @return a document chunk id
         *
         * @throws NullPointerException if the string is {@code null}
         */
        public static Id parse(String s)
            {
            int idx = s.lastIndexOf('#');
            if (idx != -1)
                {
                return new Id(s.substring(0, idx), Integer.parseInt(s.substring(idx + 1)));
                }
            return new Id(s, 0);
            }

        // ----- data members ---------------------------------------------------

        @JsonbProperty("docId")
        private String m_sDocId;

        @JsonbProperty("index")
        private int m_nIndex;
        }

    // ----- constants ------------------------------------------------------

    /**
     * The POF implementation version.
     */
    public static final int IMPL_VERSION = 0;

    // ----- data members ---------------------------------------------------

    /**
     * The text chunk extracted from a document.
     */
    @JsonbProperty("text")
    private String m_text;

    /**
     * Optional document metadata.
     */
    @JsonbProperty("metadata")
    private Map<String, Object> m_mapMetadata;

    /**
     * The float array vector.
     */
    @JsonbProperty("vector")
    private Float32Vector m_vector;
    }
