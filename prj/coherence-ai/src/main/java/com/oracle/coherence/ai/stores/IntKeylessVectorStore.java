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
 * A keyless vector store for {@code int} vectors.
 *
 * @param <M>  the type of the vector metadata
 */
public class IntKeylessVectorStore<M>
        extends IntVectorStore<Vector.Key, M>
        implements KeylessVectorStore<int[], M>
    {
    /**
     * Create a {@link IntKeylessVectorStore}.
     *
     * @param map  the {@link NamedMap} containing the vector data.
     */
    public IntKeylessVectorStore(NamedMap<Binary, Binary> map)
        {
        super(map);
        }

    @Override
    public void addVector(int[] vector, M metadata)
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
