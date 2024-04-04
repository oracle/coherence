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
 * A keyless vector store for {@code double} vectors.
 *
 * @param <MetadataType>  the type of the vector metadata
 */
public class DoubleKeylessVectorStore<MetadataType>
        extends DoubleVectorStore<Vector.Key, MetadataType>
        implements KeylessVectorStore<double[], MetadataType>
    {
    /**
     * Create a {@link DoubleKeylessVectorStore}.
     *
     * @param session  the {@link Session} managing the underlying caches
     * @param sName    the name of the vector store
     */
    public DoubleKeylessVectorStore(Session session, String sName)
        {
        super(session, sName);
        }

    @Override
    public void addVector(double[] vector, MetadataType metadata)
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
