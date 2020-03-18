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
* Comparator that reverses the result of another comparator.
*
* @author cp/gg 2002.11.01
*/
public class InverseComparator<T>
        extends SafeComparator<T>
        implements QueryMapComparator<T>
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor (for ExternalizableLite and PortableObject).
    */
    public InverseComparator()
        {
        }

    /**
    * Construct an InverseComparator.
    *
    * @param comparator  the comparator whose results are inverted by
    *                    this Comparator
    */
    public InverseComparator(Comparator<? super T> comparator)
        {
        super(comparator);
        }


    // ----- Comparator interface -------------------------------------------

    /**
    * Use the wrapped Comparator to compare the two arguments for order and
    * negate the result.
    *
    * @return a positive integer, zero, or a negative integer as the first
    * 	       argument is less than, equal to, or greater than the second
    */
    public int compare(T o1, T o2)
        {
        return -super.compare(o1, o2);
        }


    // ----- QueryMapComparator interface -----------------------------------

   /**
   * Compare two entries using the underlying comparator and negate the
   * result.
   */
    public int compareEntries(QueryMap.Entry entry1, QueryMap.Entry entry2)
        {
        return -super.compareEntries(entry1, entry2);
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Compare the InverseComparator with another object to determine
    * equality.
    *
    * @return true iff this InverseComparator and the passed object are
    *         equivalent InverseComparator
    */
    public boolean equals(Object o)
        {
        return o instanceof InverseComparator && super.equals(o);
        }
    }