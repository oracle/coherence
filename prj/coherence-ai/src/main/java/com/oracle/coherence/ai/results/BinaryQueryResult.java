/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.ai.results;

import com.tangosol.io.ReadBuffer;

import com.tangosol.util.Binary;

import java.util.Optional;

/**
 * A {@link com.oracle.coherence.ai.QueryResult} where data is held
 * in serialized binary format.
 */
public class BinaryQueryResult
        extends BaseQueryResult<Binary, ReadBuffer, ReadBuffer>
        implements Comparable<BinaryQueryResult>
    {
    /**
     * Default constructor for serialization.
     */
    public BinaryQueryResult()
        {
        }

    /**
     * Create a {@link BinaryQueryResult}.
     *
     * @param result    the result of the query
     * @param key       the key of the associated vector in binary format
     * @param vector    the vector data in binary format
     * @param metadata  the metadata in binary format
     */
    public BinaryQueryResult(float result, Binary key, ReadBuffer vector, ReadBuffer metadata)
        {
        super(result, key, vector, metadata);
        }

    @Override
    public Optional<ReadBuffer> getVector()
        {
        return getBinaryVector();
        }

    @Override
    public int compareTo(BinaryQueryResult other)
        {
        int c = Float.compare(m_result, other.m_result);
        if (c == 0)
            {
            c = m_key.compareTo(other.m_key);
            }
        return c;
        }

    }
