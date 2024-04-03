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
 * A keyless vector store for {@code long} vectors.
 *
 * @param <M>  the type of the vector metadata
 */
public class LongKeylessVectorStore<M>
        extends LongVectorStore<Vector.Key, M>
        implements KeylessVectorStore<long[], M>
    {
    /**
     * Create a {@link FloatKeylessVectorStore}.
     *
     * @param map  the {@link NamedMap} containing the vector data.
     */
    public LongKeylessVectorStore(NamedMap<Binary, Binary> map)
        {
        super(map);
        }

    @Override
    public void addVector(long[] vector, M metadata)
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
