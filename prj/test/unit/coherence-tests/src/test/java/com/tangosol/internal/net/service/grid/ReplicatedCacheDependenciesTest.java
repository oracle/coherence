/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service.grid;

import java.util.Random;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;


/**
 * Unit tests for ReplicatedCacheDependencies.
 *
 * @author pfm  2011.07.18
 */
public class ReplicatedCacheDependenciesTest
    {
    // ----- tests ----------------------------------------------------------

    /**
     * Test the setters and getters plus the clone.
     */
    @Test
    public void testAccessAndClone()
        {
        DefaultReplicatedCacheDependencies deps1 = new DefaultReplicatedCacheDependencies();
        deps1.validate();
        System.out.println(deps1.toString());

        populate(deps1).validate();
        System.out.println(deps1.toString());

        DefaultReplicatedCacheDependencies deps2 =
            new DefaultReplicatedCacheDependencies(deps1);
        assertCloneEquals(deps1, deps2);
        deps2.validate();
        }

    /**
     * Test the defaults.
     */
    @Test
    public void testDefaults()
        {
        DefaultReplicatedCacheDependencies deps = new DefaultReplicatedCacheDependencies();

        assertEquals(30000, deps.getEnsureCacheTimeoutMillis());
        assertEquals(0,     deps.getGraveyardSize());
        assertEquals(0,     deps.getLeaseGranularity());
        assertEquals(20000, deps.getStandardLeaseMillis());

        assertFalse(deps.isMobileIssues());
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Assert that the two ReplicatedCacheDependencies are equal.
     *
     * @param deps1  the first ReplicatedCacheDependencies object
     * @param deps2  the second ReplicatedCacheDependencies object
     */
    public static void assertCloneEquals(ReplicatedCacheDependencies deps1,
            ReplicatedCacheDependencies deps2)
        {
        GridDependenciesTest.assertCloneEquals(deps1, deps2);

        assertEquals(deps1.getEnsureCacheTimeoutMillis(), deps2.getEnsureCacheTimeoutMillis());
        assertEquals(deps1.getGraveyardSize(),            deps2.getGraveyardSize());
        assertEquals(deps1.isMobileIssues(),              deps2.isMobileIssues());
        }

    /**
     * Populate the dependencies and test the getters.
     *
     * @param deps  the DefaultReplicatedCacheDependencies to populate
     *
     * @return the DefaultReplicatedCacheDependencies that was passed in
     */
    public static DefaultReplicatedCacheDependencies populate(
            DefaultReplicatedCacheDependencies deps)
        {
        boolean flag;
        long    ln;
        int     n;
        Random  random = new Random();

        GridDependenciesTest.populate(deps);

        deps.setEnsureCacheTimeoutMillis(ln = random.nextInt(1000));
        assertEquals(ln, deps.getEnsureCacheTimeoutMillis());

        deps.setGraveyardSize(n = random.nextInt(1000));
        assertEquals(n, deps.getGraveyardSize());

        deps.setMobileIssues(flag = !deps.isMobileIssues());
        assertEquals(flag, deps.isMobileIssues());

        return deps;
        }
    }
