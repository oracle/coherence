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
 * A keyless vector store for {@code double} vectors.
 *
 * @param <M>  the type of the vector metadata
 */
public class DoubleKeylessVectorStore<M>
        extends DoubleVectorStore<Vector.Key, M>
        implements KeylessVectorStore<double[], M>
    {
    /**
     * Create a {@link DoubleKeylessVectorStore}.
     *
     * @param map  the {@link NamedMap} containing the vector data.
     */
    public DoubleKeylessVectorStore(NamedMap<Binary, Binary> map)
        {
        super(map);
        }

    @Override
    public void addVector(double[] vector, M metadata)
        {
        addDoubles(vector, metadata);
        }

    @Override
    public void addVector(double[] vector)
        {
        addDoubles(vector);
        }

    @Override
    public void addVectors(double[][] vectors)
        {
        addDoubles(vectors);
        }

    @Override
    public void addVectors(double[][] vectors, int batch)
        {
        addDoubles(vectors, batch);
        }
    }
