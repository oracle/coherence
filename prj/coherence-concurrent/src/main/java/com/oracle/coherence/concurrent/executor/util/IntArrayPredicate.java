/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.executor.util;

import com.tangosol.util.function.Remote.Predicate;

import java.util.List;

/**
 * A Predicate whose rules are in a plain array of integer values. When asked to validate a value, this Predicate checks
 * if it is in the array.
 *
 * @author lh
 * @since 21.12
 */
class IntArrayPredicate
        implements Predicate<Integer>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Builds the Predicate.
     *
     * @param integers  an ArrayList of Integer elements, one for every value accepted by the predicate. The test()
     *                  method will return true only if its parameter will be one of this list.
     */
    public IntArrayPredicate(List<Integer> integers)
        {
        int size = integers.size();
        f_aiValues = new int[size];
        for (int i = 0; i < size; i++)
            {
            try
                {
                f_aiValues[i] = integers.get(i);
                }
            catch (Exception e)
                {
                throw new IllegalArgumentException(e.getMessage());
                }
            }
        }

    /**
     * Returns true if the given value is included in the predicate list.
     *
     * @param value  the value to test
     *
     * @return true if the given value is included in the predicate list
     */
    public boolean test(Integer value)
        {
        for (int i = 0; i < f_aiValues.length; i++)
            {
            if (f_aiValues[i] == value)
                {
                return true;
                }
            }
        return false;
        }

    /**
     * Returns the next integer value.
     *
     * @param value the starting value
     *
     * @return the next integer value
     */
    public int getNext(Integer value)
        {
        for (int i = 0; i < f_aiValues.length; i++)
            {
            if (f_aiValues[i] > value)
                {
                return f_aiValues[i];
                }
            }

        return f_aiValues[0];
        }

    // ----- data members ---------------------------------------------------

    /**
     * The accepted values.
     */
    protected final int[] f_aiValues;
    }
