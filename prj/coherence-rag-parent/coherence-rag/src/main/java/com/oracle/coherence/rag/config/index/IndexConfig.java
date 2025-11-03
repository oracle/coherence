/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.config.index;

import com.oracle.coherence.rag.config.AbstractConfig;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;

import jakarta.json.bind.annotation.JsonbSubtype;
import jakarta.json.bind.annotation.JsonbTypeInfo;
import java.io.IOException;
import java.util.Objects;

/**
 * Configuration class for document indexing settings.
 * <p/>
 * This class encapsulates configuration parameters that control how document chunks
 * are indexed in the Coherence RAG framework. The index type determines the
 * specific indexing strategy and algorithm used for document storage and retrieval.
 * <p/>
 * This class is designed to be serializable using Coherence POF (Portable Object Format)
 * and JSON-B for configuration persistence and transmission.
 * 
 * @param <T> the type of target object this configuration is for
 *
 * @author Aleks Seovic  2025.06.28
 * @since 25.09
 */
@SuppressWarnings("unused")
@JsonbTypeInfo(
    key = "type",
    value = {
        @JsonbSubtype(type = IndexConfig.class, alias = IndexConfig.TYPE),
        @JsonbSubtype(type = SimpleIndexConfig.class, alias = SimpleIndexConfig.TYPE),
        @JsonbSubtype(type = HnswIndexConfig.class, alias = "HNSW"),
        @JsonbSubtype(type = BinaryQuantIndexConfig.class, alias = "BINARY")
    }
)
public class IndexConfig<T>
        extends AbstractConfig<T>
    {
    // ---- methods ---------------------------------------------------------

    /**
     * Returns the type of the index this configuration is for.
     *
     * @return the type of the index this configuration is for
     */
    public String type()
        {
        return TYPE;
        }

    // ---- Object methods --------------------------------------------------

    @Override
    public boolean equals(Object o)
        {
        if (this == o)
            {
            return true;
            }
        return o != null && getClass() == o.getClass();
        }

    @Override
    public int hashCode()
        {
        return Objects.hash(TYPE);
        }

    @Override
    public String toString()
        {
        return "IndexConfig[type=NONE]";
        }

    // ---- AbstractEvolvable interface -------------------------------------

    @Override
    public int getImplVersion()
        {
        return IMPLEMENTATION_VERSION;
        }

    // ---- PortableObject interface ----------------------------------------

    @Override
    public void readExternal(PofReader reader) throws IOException
        {
        }

    @Override
    public void writeExternal(PofWriter writer) throws IOException
        {
        }

    // ---- constants -------------------------------------------------------

    /**
     * The type of the index this configuration is for.
     */
    public static final String TYPE = "NONE";

    /**
     * The implementation version for this class.
     */
    public static final int IMPLEMENTATION_VERSION = 1;
    }
