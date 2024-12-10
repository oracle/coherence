/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service.grid;

import com.tangosol.io.DecoratedBinaryDeltaCompressor;
import com.tangosol.io.DeltaCompressor;

import com.tangosol.util.AssertionException;
import com.tangosol.util.NullImplementation;

import junit.framework.Assert;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

import java.util.Random;

/**
 * Unit tests for PartitionedCacheDependencies.
 *
 * @author pfm  2011.07.18
 */
public class PartitionedCacheDependenciesTest
    {
    // ----- tests ----------------------------------------------------------

    /**
     * Test the setters and getters plus the clone.
     */
    @Test
    public void testAccessAndClone()
        {
        DefaultPartitionedCacheDependencies deps1 = new DefaultPartitionedCacheDependencies();

        deps1.validate();
        System.out.println(deps1.toString());

        populate(deps1).validate();
        System.out.println(deps1.toString());

        DefaultPartitionedCacheDependencies deps2 = new DefaultPartitionedCacheDependencies(deps1);

        assertCloneEquals(deps1, deps2);
        deps2.validate();
        }

    /**
     * Tests behavior of invalid standard lease milliseconds settings.
     */
    @Test
    public void badStandardLeaseMillis()
        {
        try
            {
            DefaultPartitionedCacheDependencies deps =  new DefaultPartitionedCacheDependencies();
            populate(deps).setStandardLeaseMillis(-1);
            deps.validate();
            }
        catch (IllegalArgumentException e)
            {
            return;
            }
        catch (AssertionException e)
            {
            return;
            }

        Assert.fail();
        }

    /**
     * Tests behavior of invalid lease granularity settings.
     */
    @Test
    public void badLeaseGranularity()
        {
        try
            {
            DefaultPartitionedCacheDependencies deps =  new DefaultPartitionedCacheDependencies();
            populate(deps).setLeaseGranularity(-1);
            deps.validate();
            }
        catch (IllegalArgumentException e)
            {
            return;
            }
        catch (AssertionException e)
            {
            return;
            }

        Assert.fail();
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Assert that the two PartitionedCacheDependencies are equal.
     *
     * @param deps1  the first PartitionedCacheDependencies object
     * @param deps2  the second PartitionedCacheDependencies object
     */
    public static void assertCloneEquals(PartitionedCacheDependencies deps1, PartitionedCacheDependencies deps2)
        {
        PartitionedServiceDependenciesTest.assertCloneEquals(deps1, deps2);

        assertEquals(deps1.getBackupCountAfterWriteBehind(), deps2.getBackupCountAfterWriteBehind());
        assertEquals(deps1.getDeltaCompressor(), deps2.getDeltaCompressor());
        assertEquals(deps1.getLeaseGranularity(), deps2.getLeaseGranularity());
        assertEquals(deps1.getStandardLeaseMillis(), deps2.getStandardLeaseMillis());
        assertEquals(deps1.isStrictPartitioning(), deps2.isStrictPartitioning());
        }

    /**
     * Populate the dependencies and test the getters.
     *
     * @param deps  the DefaultPartitionedCacheDependencies to populate
     *
     * @return the DefaultPartitionedCacheDependencies that was passed in
     */
    public static DefaultPartitionedCacheDependencies populate(DefaultPartitionedCacheDependencies deps)
        {
        PartitionedServiceDependenciesTest.populate(deps);

        Random random = new Random();
        int    n      = random.nextInt(2);

        deps.setBackupCountAfterWriteBehind(n);
        assertEquals(n, deps.getBackupCountAfterWriteBehind());

        DeltaCompressor dc = new DecoratedBinaryDeltaCompressor(NullImplementation.getDeltaCompressor());

        deps.setDeltaCompressor(dc);
        assertEquals(dc, deps.getDeltaCompressor());

        deps.setLeaseGranularity(n = random.nextInt(1000));
        assertEquals(n, deps.getLeaseGranularity());

        long ln = random.nextInt(1000);

        deps.setStandardLeaseMillis(ln);
        assertEquals(ln, deps.getStandardLeaseMillis());

        boolean flag = !deps.isStrictPartitioning();

        deps.setStrictPartitioning(flag);
        assertEquals(flag, deps.isStrictPartitioning());

        return deps;
        }
    }
