/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.base;

import java.util.Comparator;


/**
 * NaturalComparator is a comparator which simply delegates to a Comparable object's compare implementation.
 *
 * @author mf  2014.08.28
 */
public class NaturalComparator<T extends Comparable<T>>
        implements Comparator<T>
    {
    @Override
    public int compare(T o1, T o2)
        {
        return o1.compareTo(o2);
        }


    // ----- constants ------------------------------------------------------

    /**
     * NaturalComparator singleton.
     */
    public static final NaturalComparator INSTANCE = new NaturalComparator();
    }
