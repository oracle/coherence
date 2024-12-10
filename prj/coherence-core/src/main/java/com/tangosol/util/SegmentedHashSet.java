/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


import java.util.Map;


/**
* An implementation of java.util.Set that is optimized for heavy concurrent
* use.
*
* @author jh  2009.08.28
* @since Coherence 3.5.1
*/
public class SegmentedHashSet
        extends MapSet
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a thread-safe hash set using the default settings.
    */
    public SegmentedHashSet()
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
    public SegmentedHashSet(int cInitialBuckets, float flLoadFactor, float flGrowthRate)
        {
        super(new SegmentedHashMap(cInitialBuckets, flLoadFactor, flGrowthRate));
        }


    // ----- internal -------------------------------------------------------

    /**
    * {@inheritDoc}
    */
    protected Map instantiateMap()
        {
        return new SegmentedHashMap();
        }
    }
