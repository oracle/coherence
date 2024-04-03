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
 * A keyless vector store for {@code short} vectors.
 *
 * @param <M>  the type of the vector metadata
 */
public class ShortKeylessVectorStore<M>
        extends ShortVectorStore<Vector.Key, M>
        implements KeylessVectorStore<short[], M>
    {
    /**
     * Create a {@link ShortKeylessVectorStore}.
     *
     * @param map  the {@link NamedMap} containing the vector data.
     */
    public ShortKeylessVectorStore(NamedMap<Binary, Binary> map)
        {
        super(map);
        }

    @Override
    public void addVector(short[] vector, M metadata)
        {
        addShorts(vector, metadata);
        }

    @Override
    public void addVector(short[] vector)
        {
        addShorts(vector);
        }

    @Override
    public void addVectors(short[][] vectors)
        {
        addShorts(vectors);
        }

    @Override
    public void addVectors(short[][] vectors, int batch)
        {
        addShorts(vectors, batch);
        }
    }
