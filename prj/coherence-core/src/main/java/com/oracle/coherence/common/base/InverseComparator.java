/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.base;

import java.util.Comparator;


/**
 * InverseComparator is a wrapper comparator which simply inverses the comparison result.
 *
 * @author mf  2014.08.28
 */
public class InverseComparator<T>
    implements Comparator<T>
    {
    /**
     * Construct an InverseComparator which inverts the specified comparator.
     *
     * @param comparator the comparator to invert
     */
    public InverseComparator(Comparator<T> comparator)
        {
        f_comparator = comparator;
        }

    @Override
    public int compare(T o1, T o2)
        {
        return -f_comparator.compare(o1, o2);
        }


    // ----- data members ---------------------------------------------------

    /**
     * The comparator to invert.
     */
    protected final Comparator<T> f_comparator;


    // ----- constants ------------------------------------------------------

    /**
     * InverseComparator singleton which inverts a Comparable objects natural comparison order.
     */
    public static final InverseComparator INSTANCE = new InverseComparator(NaturalComparator.INSTANCE);
    }
