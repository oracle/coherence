/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.internal;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * A test for the data structure {@link PartitionVersionExclusionList}.
 *
 * @author hr  2015.07.13
 */
public class PartitionVersionExclusionListTest
    {
    @Test
    public void testCoreFunctionality()
        {
        PartitionVersionExclusionList storage = new PartitionVersionExclusionList();

        Random rnd = new Random();

        assertFalse(storage.isExcluded(17, 1));

        for (int i = 0; i < 10000; ++i)
            {
            int iPart    = rnd.nextInt(0xFFFF);
            int nVersion = rnd.nextInt(Integer.MAX_VALUE); // positive only

            // exclude and test the exclusion of a partition and version

            storage.exclude(iPart, nVersion);

            assertTrue(storage.isExcluded(iPart, nVersion));

            // 3/10 of the time reset a partition or (partition and version)

            if (rnd.nextInt(10) < 3)
                {
                if (rnd.nextBoolean())
                    {
                    storage.reset(iPart, nVersion);

                    assertFalse(storage.isExcluded(iPart, nVersion));
                    }
                else
                    {
                    List<Integer> listVersions = new ArrayList<>();
                    storage.forEach(entry ->
                        {
                        if (entry.getPartition() == iPart)
                            {
                            listVersions.add(entry.getVersion());
                            }
                        });

                    storage.reset(iPart);
                    for (int nRemovedVersion : listVersions)
                        {
                        assertFalse(storage.isExcluded(iPart, nRemovedVersion));
                        }
                    }
                }
            }
        }

    @Test
    public void testPartitionExclude()
        {
        PartitionVersionExclusionList exclusionList = new PartitionVersionExclusionList();

        exclusionList.exclude(0, 1);
        exclusionList.exclude(0, 2);
        exclusionList.exclude(0, 3);

        exclusionList.reset(0);

        assertFalse(exclusionList.isExcluded(0, 1));
        assertFalse(exclusionList.isExcluded(0, 2));
        assertFalse(exclusionList.isExcluded(0, 3));
        }
    }