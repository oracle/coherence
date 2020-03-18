/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.common.base;

import java.util.Comparator;


/**
 * InverseComparator is a wrapper comparator which simply inverses the comparison result.
 *
 * @author mf  2014.08.28
 * @deprecated use {@link com.oracle.coherence.common.base.InverseComparator} instead
 */
@Deprecated
public class InverseComparator<T>
        extends com.oracle.coherence.common.base.InverseComparator<T>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct an InverseComparator which inverts the specified comparator.
     *
     * @param comparator the comparator to invert
     */
    public InverseComparator(Comparator<T> comparator)
        {
        super(comparator);
        }


    // ----- constants ------------------------------------------------------

    /**
     * InverseComparator singleton which inverts a Comparable objects natural comparison order.
     */
    public static final InverseComparator INSTANCE = new InverseComparator(NaturalComparator.INSTANCE);
    }
