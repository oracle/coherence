/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.ai.internal;

import com.oracle.coherence.ai.Vector;

import com.tangosol.io.ReadBuffer;

import com.tangosol.net.Service;

import java.util.Optional;

/**
 * A vector store that takes vectors, keys and metadata as
 * pre-serialized binary data.
 */
public interface BinaryVectorStore
    {
    /**
     * Add a vector to the store.
     *
     * @param vector  the binary vector to add
     * @param key     the binary key for the vector
     *
     * @throws NullPointerException if either the vector or key is {@code null}
     */
    void addBinaryVector(BinaryVector vector, ReadBuffer key);

    /**
     * Add a vector to the store.
     *
     * @param vector    the binary vector to add
     * @param sequence  the {@link com.oracle.coherence.ai.Vector.KeySequence} to use to generate a key
     *
     * @throws NullPointerException if either the vector or sequence is {@code null}
     */
    void addBinaryVector(BinaryVector vector, Vector.KeySequence<?> sequence);

    /**
     * Obtain the {@link BinaryVector} for the specified key.
     *
     * @param key  the key of the vector to find
     *
     * @return an {@link Optional} containing the vector or an empty
     *         {@link Optional} if no vector is mapped to the specified
     *         key.
     */
    Optional<BinaryVector> getVector(ReadBuffer key);

    /**
     * Clear the contents of the store.
     */
    void clear();

    /**
     * Destroy the store.
     */
    void destroy();

    /**
     * Release local references to the store.
     */
    void release();

    /**
     * Return the service managing the underlying vector storage.
     *
     * @return  the service managing the underlying vector storage
     */
    Service getService();
    }
