/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io.internal;

import java.io.ObjectInputFilter;

import java.util.Date;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for {@link DefaultObjectInputFilter}.
 *
 * @author Aleks Seovic  2026.04.29
 * @since 26.04
 */
public class DefaultObjectInputFilterTest
    {
    @Test
    public void testUserFilterRejectionWins()
        {
        ObjectInputFilter filter = DefaultObjectInputFilter.forTesting(info ->
                info.serialClass() == Date.class
                        ? ObjectInputFilter.Status.REJECTED
                        : ObjectInputFilter.Status.UNDECIDED);

        assertEquals(ObjectInputFilter.Status.REJECTED, filter.checkInput(new TestFilterInfo(Date.class)));
        }

    @Test
    public void testDefaultDenyListWins()
        {
        ObjectInputFilter filter = DefaultObjectInputFilter.forTesting(info ->
                info.serialClass() == Runtime.class
                        ? ObjectInputFilter.Status.ALLOWED
                        : ObjectInputFilter.Status.UNDECIDED);

        assertEquals(ObjectInputFilter.Status.REJECTED, filter.checkInput(new TestFilterInfo(Runtime.class)));
        }

    @Test
    public void testDefaultAllowSurvivesUserUndecided()
        {
        ObjectInputFilter filter = DefaultObjectInputFilter.forTesting(info ->
                ObjectInputFilter.Status.UNDECIDED);

        assertEquals(ObjectInputFilter.Status.ALLOWED, filter.checkInput(new TestFilterInfo(Date.class)));
        }

    private record TestFilterInfo(Class<?> serialClass)
            implements ObjectInputFilter.FilterInfo
        {
        @Override
        public long arrayLength()
            {
            return -1L;
            }

        @Override
        public long depth()
            {
            return 1L;
            }

        @Override
        public long references()
            {
            return 0L;
            }

        @Override
        public long streamBytes()
            {
            return 0L;
            }
        }
    }
