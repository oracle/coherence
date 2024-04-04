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
     * @param session  the {@link Session} managing the underlying caches
     * @param sName    the name of the vector store
     */
    public ShortKeylessVectorStore(Session session, String sName)
        {
        super(session, sName);
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
