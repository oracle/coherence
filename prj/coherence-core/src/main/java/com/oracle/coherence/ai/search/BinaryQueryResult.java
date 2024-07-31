/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.ai.search;

import com.tangosol.util.Binary;

/**
 * A {@link com.oracle.coherence.ai.QueryResult} where data is held
 * in serialized binary format.
 */
public class BinaryQueryResult
        extends BaseQueryResult<Binary, Binary>
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
     * @param distance  the calculated vector distance
     * @param key       the key of the associated entry in binary format
     * @param value     the value in binary format
     */
    public BinaryQueryResult(double distance, Binary key, Binary value)
        {
        super(distance, key, value);
        }

    /**
     * Set the distance for this result.
     *
     * @param distance  the distance for this result
     */
    void setDistance(double distance)
        {
        m_distance = distance;
        }

    @Override
    public int compareTo(BinaryQueryResult other)
        {
        int c = Double.compare(m_distance, other.m_distance);
        if (c == 0)
            {
            c = m_key.compareTo(other.m_key);
            }
        return c;
        }
    }
