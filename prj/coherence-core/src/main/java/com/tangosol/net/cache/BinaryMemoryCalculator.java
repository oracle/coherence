/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.cache;


import com.tangosol.util.Binary;


/**
* A UnitCalculator implementation that weighs a cache entry based upon the
* amount of physical memory (in bytes) required to store the entry.
* <p>
* This implementation can only determine an accurate entry size if both the
* entry key and value are {@link Binary} objects; otherwise, an exception will
* be thrown during the unit calculation.
*
* @author jh  2005.12.14
*/
public class BinaryMemoryCalculator
        extends SimpleMemoryCalculator
    {
    // ----- UnitCalculator interface ---------------------------------------

    /**
    * Calculate the approximate number of bytes required to cache the given
    * Binary key and value.
    *
    * @param oKey    the key
    * @param oValue  the value
    *
    * @return the number of bytes of memory necessary to cache the given
    *         key and value
    */
    public int calculateUnits(Object oKey, Object oValue)
        {
        try
            {
            int cb = SIZE_ENTRY + 2*SIZE_BINARY;

            if (oKey != null) // oKey can be null from from SimplMapIndex.initialize
                {
                cb += ((Binary) oKey).length();
                }
            if (oValue != null)
                {
                cb += ((Binary) oValue).length();
                }
            return padMemorySize(cb);
            }
        catch (ClassCastException e)
            {
            throw new IllegalArgumentException("Unsupported key or value: "
                    + "Key=" + oKey + ", Value=" + oValue);
            }
        }


    // ----- unit test ------------------------------------------------------

    /**
    * Unit test.
    * <p>
    * Usage:
    * <pre>
    * java com.tangosol.net.cache.BinaryMemoryCalculator
    * </pre>
    *
    * @param asArg  command line arguments
    */
    public static void main(String[] asArg)
        {
        log("Minimum entry size: " + INSTANCE.calculateUnits(new Binary(), new Binary()));
        }


    // ----- constants ------------------------------------------------------

    /**
    * Singleton BinaryMemoryCalculator instance.
    */
    public static final BinaryMemoryCalculator INSTANCE = new BinaryMemoryCalculator();
    }