/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.ai.stores;

import com.oracle.coherence.ai.KeylessVectorStore;

import com.oracle.coherence.ai.Vector;

import com.tangosol.net.Session;

/**
 * A keyless vector store for {@code long} vectors.
 *
 * @param <MetadataType>  the type of the vector metadata
 */
public class LongKeylessVectorStore<MetadataType>
        extends LongVectorStore<Vector.Key, MetadataType>
        implements KeylessVectorStore<long[], MetadataType>
    {
    /**
     * Create a {@link LongKeylessVectorStore}.
     *
     * @param session  the {@link Session} managing the underlying caches
     * @param sName    the name of the vector store
     */
    public LongKeylessVectorStore(Session session, String sName)
        {
        super(session, sName);
        }

    @Override
    public void addVector(long[] vector, MetadataType metadata)
        {
        addLongs(vector, metadata);
        }

    @Override
    public void addVector(long[] vector)
        {
        addLongs(vector);
        }

    @Override
    public void addVectors(long[][] vectors)
        {
        addLongs(vectors);
        }

    @Override
    public void addVectors(long[][] vectors, int batch)
        {
        addLongs(vectors, batch);
        }
    }
