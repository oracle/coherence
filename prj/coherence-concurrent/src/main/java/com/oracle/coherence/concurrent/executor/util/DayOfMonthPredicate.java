/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.executor.util;

import java.util.List;

/**
 * A Predicate whose rules are in a plain array of integer values. When asked to validate a value, this ValueMatcher
 * checks if it is in the array and, if not, checks whether the last-day-of-month setting applies.
 *
 * @author lh
 * @since 21.12
 */
class DayOfMonthPredicate
        extends IntArrayPredicate
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Builds the ValueMatcher.
     *
     * @param listIntegers  a {@link List} of {@link Integer} elements, one for every value accepted by the matcher.
     *                      The match() method will return true only if its parameter will be one of this list or
     *                      the last-day-of-month setting applies.
     */
    public DayOfMonthPredicate(List<Integer> listIntegers)
        {
        super(listIntegers);
        }
    }
