/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dslquery.operator;

import com.tangosol.util.FilterBuildingException;
import com.tangosol.util.QueryHelper;
import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests for {@link ComparisonOperator}.
 */
public class ComparisonOperatorTest
    {

    /**
     * Ensure that CohQL queries cannot have identifiers on both sides as
     * {@link QueryHelper} does not support this and should throw
     * {@link UnsupportedOperationException}. This can be achieved with standard filters.
     * Refer Bugs 31622320, 27250717
     */
    @Test
    public void testForIdentifiersOnBothSides()
        {
        assertFilterInvalid("value1 != value2");
        assertFilterInvalid("value1.fieldA != value2.fieldB");
        assertFilterInvalid("value1.fieldA = value2.fieldB");
        assertFilterInvalid("value1.fieldA <= value2.fieldB");
        assertFilterInvalid("value1.fieldA >= value2.fieldB");
        assertFilterInvalid("value1.fieldA > value2.fieldB");
        assertFilterInvalid("value1.fieldA != value2.fieldB.fieldC");
        assertFilterInvalid("value1.fieldA = value2.fieldB.fieldC");
        assertFilterInvalid("value1.fieldA <= value2.fieldB.fieldC");
        assertFilterInvalid("value1.fieldA >= value2.fieldB.fieldC");
        assertFilterInvalid("value1.fieldA > value2.fieldB.fieldC");
        assertFilterInvalid("value1.fieldA < value2.fieldB.fieldC");
        assertFilterInvalid("value1 = value2");
        assertFilterInvalid("value1 > value2");
        assertFilterInvalid("value1 < value2");
        assertFilterInvalid("value1 <= value2");
        assertFilterInvalid("value1 >= value2");
        }

    private void assertFilterInvalid(String sQuery)
        {
        try
            {
            QueryHelper.createFilter(sQuery);
            Assert.fail("Exception should have been thrown");
            }
        catch (Exception e)
            {
            assertThat(e, instanceOf(FilterBuildingException.class));
            assertThat(e.getCause(), is(instanceOf(UnsupportedOperationException.class)));
            }
        }
    }
