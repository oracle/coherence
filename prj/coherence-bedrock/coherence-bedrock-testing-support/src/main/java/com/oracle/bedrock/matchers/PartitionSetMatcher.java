/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.bedrock.matchers;

import com.tangosol.net.partition.PartitionSet;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import java.util.HashSet;

public class PartitionSetMatcher extends TypeSafeMatcher<PartitionSet>
{
    /**
     * The partition identifiers to match.
     */
    private final HashSet<Integer> partitionIds;


    /**
     * Constructs a {@link PartitionSetMatcher}.
     *
     * @param partitionIds  the partition identifiers to match
     */
    private PartitionSetMatcher(int... partitionIds)
    {
        this.partitionIds = new HashSet<Integer>();

        if (partitionIds != null)
        {
            for (int partitionId : partitionIds)
            {
                this.partitionIds.add(partitionId);
            }
        }
    }


    @Override
    protected boolean matchesSafely(PartitionSet partitionSet)
    {
        for (int partitionId : partitionIds)
        {
            if (!partitionSet.contains(partitionId))
            {
                return false;
            }
        }

        return true;
    }


    @Override
    protected void describeMismatchSafely(PartitionSet partitionSet,
                                          Description  mismatchDescription)
    {
        mismatchDescription.appendText("partitionSet was " + partitionSet);
    }


    @Override
    public void describeTo(Description description)
    {
        description.appendText("partitionSet containing ").appendValueList("{", ", ", "}", partitionIds);
    }


    public static Matcher<PartitionSet> contains(int... partitionIds)
    {
        return new PartitionSetMatcher(partitionIds);
    }
}
