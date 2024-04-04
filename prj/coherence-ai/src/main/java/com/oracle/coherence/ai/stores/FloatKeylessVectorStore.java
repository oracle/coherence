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
 * A keyless vector store for {@code float} vectors.
 *
 * @param <MetadataType>  the type of the vector metadata
 */
public class FloatKeylessVectorStore<MetadataType>
        extends FloatVectorStore<Vector.Key, MetadataType>
        implements KeylessVectorStore<float[], MetadataType>
    {
    /**
     * Create a {@link FloatKeylessVectorStore}.
     *
     * @param session  the {@link Session} managing the underlying caches
     * @param sName    the name of the vector store
     */
    public FloatKeylessVectorStore(Session session, String sName)
        {
        super(session, sName);
        }

    @Override
    public void addVector(float[] vector, MetadataType metadata)
        {
        addFloats(vector, metadata);
        }

    @Override
    public void addVector(float[] vector)
        {
        addFloats(vector);
        }

    @Override
    public void addVectors(float[][] vectors)
        {
        addFloats(vectors);
        }

    @Override
    public void addVectors(float[][] vectors, int batch)
        {
        addFloats(vectors, batch);
        }
    }
