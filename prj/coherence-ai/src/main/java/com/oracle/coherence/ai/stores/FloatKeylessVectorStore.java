/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.ai.stores;

import com.oracle.coherence.ai.KeylessVectorStore;
import com.oracle.coherence.ai.Vector;

import com.tangosol.net.NamedMap;

import com.tangosol.util.Binary;

/**
 * A keyless vector store for {@code float} vectors.
 *
 * @param <M>  the type of the vector metadata
 */
public class FloatKeylessVectorStore<M>
        extends FloatVectorStore<Vector.Key, M>
        implements KeylessVectorStore<float[], M>
    {
    /**
     * Create a {@link FloatKeylessVectorStore}.
     *
     * @param map  the {@link NamedMap} containing the vector data.
     */
    public FloatKeylessVectorStore(NamedMap<Binary, Binary> map)
        {
        super(map);
        }

    @Override
    public void addVector(float[] vector, M metadata)
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
