/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.testing.matcher;

import org.hamcrest.Matcher;

/**
 * This class provides static helpers to fluently express assertion
 * statements using the JUnit
 * {@link org.junit.Assert#assertThat(Object, Matcher)} notation.
 *
 * @author hr  2012.06.29
 *
 * @since Coherence 12.1.2
 */
public class CoherenceMatchers
    {
    // ----- static helpers -------------------------------------------------

    /**
     * Return a {@link Matcher} implementation when given a string,
     * representing a thread group name, and determine the active thread count
     * for the thread group forwarding it to the provided {@link Matcher} or
     * null if a thread group could not be located.
     *
     * @param matcher  the matcher to apply to the active thread count or null
     * @param <T>      the type the matcher will operate against (null or Integer)
     *
     * @return  {@link Matcher} that locates a thread group and passes the
     *          active thread count or null to the provided {@link Matcher}.
     */
    public static <T> Matcher<String> hasThreadGroupSize(Matcher<T> matcher)
        {
        return new ThreadGroupMatcher<T>(matcher);
        }
    }
