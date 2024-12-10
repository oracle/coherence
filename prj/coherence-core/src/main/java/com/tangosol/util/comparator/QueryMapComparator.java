/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util.comparator;


import com.tangosol.util.QueryMap;

import java.util.Comparator;


/**
* This interface is used by Comparator implementations that can use value
* extraction optimization exposed by the
* {@link com.tangosol.util.QueryMap.Entry} interface.
*
* @author cp/gg 2002.12.13, 2006.06.12
*/
public interface QueryMapComparator<T>
        extends Comparator<T>
    {
    /**
    * Compare two entries based on the rules specified by {@link Comparator}.
    * <p>
    * If possible, use the {@link com.tangosol.util.QueryMap.Entry#extract
    * extract} method to optimize the value extraction process.
    * <p>
    * This method is expected to be implemented by Comparator wrappers,
    * such as {@link ChainedComparator} and {@link InverseComparator},
    * which simply pass on this invocation to the wrapped Comparator objects
    * if they too implement this interface, or to invoke their default
    * compare method passing the actual objects (not the extracted values)
    * obtained from the extractor using the passed entries.
    * <p>
    * This interface is also expected to be implemented by ValueExtractor
    * implementations that implement the Comparator interface. It is expected
    * that in most cases, the Comparator wrappers will eventually terminate
    * at (i.e. delegate to) ValueExtractors that also implement this
    * interface.
    *
    * @param entry1      the first entry to compare values from; read-only
    * @param entry2      the second entry to compare values from; read-only
    *
    * @return a negative integer, zero, or a positive integer as the first
    *         entry denotes a value that is is less than, equal to, or
    *         greater than the value denoted by the second entry
    *
    * @throws ClassCastException if the arguments' types prevent them from
    * 	       being compared by this Comparator.
    * @throws com.tangosol.util.WrapperException if the extractor encounters
    *         an exception in the course of extracting the value
    * @throws IllegalArgumentException if the extractor cannot handle
    *         the passed objects for any other reason; an implementor should
    *         include a descriptive message
    * @since Coherence 3.2
    */
    public int compareEntries(QueryMap.Entry<?, T> entry1, QueryMap.Entry<?, T> entry2);
    }
