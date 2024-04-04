/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.ai;

import com.tangosol.io.ReadBuffer;

import java.util.Optional;

/**
 * The result of executing a query on a vector store.
 *
 * @param <VectorType>    the type of the vector
 * @param <KeyType>       the type of the keys used to identify vectors
 * @param <MetadataType>  the type of the vector's metadata
 */
public interface QueryResult<VectorType, KeyType, MetadataType>
    {
    /**
     * The result value obtained by executing the query on the vector.
     *
     * @return the result value obtained by executing the query on the vector
     */
    float getResult();

    /**
     * Returns the key for the vector this result matches.
     *
     * @return the key for the vector this result matches
     */
    Optional<KeyType> getKey();

    /**
     * The result vector as bytes wrapped in a {@link ReadBuffer}.
     *
     * @return result vector as bytes wrapped in a {@link ReadBuffer}
     */
    Optional<ReadBuffer> getBinaryVector();

    /**
     * Return the actual result vector.
     *
     * @return  the actual result vector
     */
    Optional<VectorType> getVector();

    /**
     * Return the result vector's metadata.
     *
     * @return the result vector's metadata
     */
    Optional<MetadataType> getMetadata();
    }
