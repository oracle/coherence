/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.common.base;

/**
 * NaturalComparator is a comparator which simply delegates to a Comparable object's compare implementation.
 *
 * @author mf  2014.08.28
 * @deprecated use {@link com.oracle.coherence.common.base.NaturalComparator} instead
 */
@Deprecated
public class NaturalComparator<T extends Comparable<T>>
        extends com.oracle.coherence.common.base.NaturalComparator<T>
    {


    // ----- constants ------------------------------------------------------

    /**
     * NaturalComparator singleton.
     */
    public static final NaturalComparator INSTANCE = new NaturalComparator();
    }
