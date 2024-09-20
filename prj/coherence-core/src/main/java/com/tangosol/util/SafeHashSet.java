/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;

import com.oracle.coherence.common.collections.NullableConcurrentMap;

import java.util.Map;


/**
* An implementation of java.util.Set that is synchronized, but minimally so.
* This class is for use in situation where high concurrency is required,
* but so is data integrity.
*
* @author gg 09/16/05
* @since Coherence 3.1
*/
public class SafeHashSet
        extends MapSet
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a thread-safe hash set using the default settings.
    */
    public SafeHashSet()
        {
        super();
        }

    /**
    * Construct a thread-safe hash set using the specified settings.
    *
    * @param cInitialBuckets  the initial number of hash buckets, 0 &lt; n
    * @param flLoadFactor     the acceptable load factor before resizing
    *                         occurs, 0 &lt; n, such that a load factor of
    *                         1.0 causes resizing when the number of entries
    *                         exceeds the number of buckets
    * @param flGrowthRate     the rate of bucket growth when a resize occurs,
    *                         0 &lt; n, such that a growth rate of 1.0 will
    *                         double the number of buckets:
    *                         bucketcount = bucketcount * (1 + growthrate)
    */
    public SafeHashSet(int cInitialBuckets, float flLoadFactor, float flGrowthRate)
        {
        super(new NullableConcurrentMap(cInitialBuckets, flLoadFactor));
        }


    // ----- internal -------------------------------------------------------

    /**
    * {@inheritDoc}
    */
    protected Map instantiateMap()
        {
        return new NullableConcurrentMap();
        }
    }
