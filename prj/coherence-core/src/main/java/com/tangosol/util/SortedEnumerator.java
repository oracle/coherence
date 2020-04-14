/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


import java.util.Enumeration;
import java.util.Iterator;
import java.util.Arrays;


/**
* Sorts the contents of the passed enumerator then enumerates those contents.
*
* @author Cameron Purdy
* @version 1.00, 02/15/99
*/
public class SortedEnumerator
        extends SimpleEnumerator
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a Sorted enumerator.
    *
    * @param enmr  the Enumeration that needs to be sorted
    */
    public SortedEnumerator(Enumeration enmr)
        {
        super(toArray(enmr));
        }

    /**
    * Construct a Sorted enumerator.
    *
    * @param iterator  the Iterator that needs to be sorted
    */
    public SortedEnumerator(Iterator iterator)
        {
        super(toArray(iterator));
        }


    // ----- helpers --------------------------------------------------------

    /**
    * Return the contents of the passed Enumeration as a sorted array.
    *
    * @param enmr  an Enumeration of potentially unsorted objects
    *
    * @return  an array of sorted objects
    */
    public static Object[] toArray(Enumeration enmr)
        {
        Object[] ao = SimpleEnumerator.toArray(enmr);

        if (ao.length > 0)
            {
            Arrays.sort(ao);
            }

        return ao;
        }

    /**
    * Return the contents of the passed Iterator as a sorted array.
    *
    * @param iterator  an Iterator of potentially unsorted objects
    *
    * @return  an array of sorted objects
    */
    public static Object[] toArray(Iterator iterator)
        {
        Object[] ao = SimpleEnumerator.toArray(iterator);

        if (ao.length > 0)
            {
            Arrays.sort(ao);
            }

        return ao;
        }
    }
