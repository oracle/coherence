/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service.extend.proxy;

import com.tangosol.util.AssertionException;

import junit.framework.Assert;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

import java.util.Random;

/**
 * Unit tests for CacheServiceProxyDependenciesTest.
 *
 * @author pfm  2011.09.26
 */
public class CacheServiceProxyDependenciesTest
    {
    // ----- tests ----------------------------------------------------------

    /**
     * Test the setters and getters plus the clone.
     */
    @Test
    public void testAccessAndClone()
        {
        DefaultCacheServiceProxyDependencies deps1 = new DefaultCacheServiceProxyDependencies();

        deps1.validate();
        System.out.println(deps1.toString());

        populate(deps1).validate();
        System.out.println(deps1.toString());

        DefaultCacheServiceProxyDependencies deps2 = new DefaultCacheServiceProxyDependencies(deps1);

        assertCloneEquals(deps1, deps2);
        deps2.validate();
        }

    /**
     * Method description
     */
    @Test
    public void badTransferThreshold()
        {
        try
            {
            DefaultCacheServiceProxyDependencies dependencies = populate(new DefaultCacheServiceProxyDependencies());

            dependencies.setTransferThreshold(-1);
            dependencies.validate();
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
     * Assert that the two CacheServiceProxyDependencies are equal.
     *
     * @param deps1  the first CacheServiceProxyDependencies object
     * @param deps2  the second CacheServiceProxyDependencies object
     */
    public static void assertCloneEquals(CacheServiceProxyDependencies deps1, CacheServiceProxyDependencies deps2)
        {
        ServiceProxyDependenciesTest.assertCloneEquals(deps1, deps2);

        assertEquals(deps1.isReadOnly(), deps2.isReadOnly());
        assertEquals(deps1.isLockEnabled(), deps2.isLockEnabled());
        assertEquals(deps1.getTransferThreshold(), deps2.getTransferThreshold());
        }

    /**
     * Populate the dependencies and test the getters.
     *
     * @param deps  the DefaultCacheServiceProxyDependencies to populate
     *
     * @return the DefaultCacheServiceProxyDependencies that was passed in
     */
    public static DefaultCacheServiceProxyDependencies populate(DefaultCacheServiceProxyDependencies deps)
        {
        ServiceProxyDependenciesTest.populate(deps);

        boolean flag = !deps.isReadOnly();

        deps.setReadOnly(flag);
        assertEquals(flag, deps.isReadOnly());

        deps.setLockEnabled(flag = !deps.isLockEnabled());
        assertEquals(flag, deps.isLockEnabled());

        long n = new Random().nextInt(1000);

        deps.setTransferThreshold(n);
        assertEquals(n, deps.getTransferThreshold());

        return deps;
        }
    }
