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
 * A keyless vector store for {@code int} vectors.
 *
 * @param <MetadataType>  the type of the vector metadata
 */
public class IntKeylessVectorStore<MetadataType>
        extends IntVectorStore<Vector.Key, MetadataType>
        implements KeylessVectorStore<int[], MetadataType>
    {
    /**
     * Create a {@link IntKeylessVectorStore}.
     *
     * @param session  the {@link Session} managing the underlying caches
     * @param sName    the name of the vector store
     */
    public IntKeylessVectorStore(Session session, String sName)
        {
        super(session, sName);
        }

    @Override
    public void addVector(int[] vector, MetadataType metadata)
        {
        addInts(vector, metadata);
        }

    @Override
    public void addVector(int[] vector)
        {
        addInts(vector);
        }

    @Override
    public void addVectors(int[][] vectors)
        {
        addInts(vectors);
        }

    @Override
    public void addVectors(int[][] vectors, int batch)
        {
        addInts(vectors, batch);
        }
    }
