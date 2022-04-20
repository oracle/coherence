/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service.extend.remote;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for RemoteCacheServiceDependenciesTest.
 *
 * @author pfm  2011.09.26
 */
public class RemoteCacheServiceDependenciesTest
    {
    // ----- tests ----------------------------------------------------------

    /**
     * Test the setters and getters plus the clone.
     */
    @Test
    public void testAccessAndClone()
        {
        DefaultRemoteCacheServiceDependencies deps1 =
            new DefaultRemoteCacheServiceDependencies();
        System.out.println(deps1.toString());

        populate(deps1).validate();
        System.out.println(deps1.toString());

        DefaultRemoteCacheServiceDependencies deps2 =
            new DefaultRemoteCacheServiceDependencies(deps1);
        assertCloneEquals(deps1, deps2);
        deps2.validate();
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Assert that the two RemoteCacheServiceDependencies are equal.
     *
     * @param deps1  the first RemoteCacheServiceDependencies object
     * @param deps2  the second RemoteCacheServiceDependencies object
     */
    public static void assertCloneEquals(RemoteCacheServiceDependencies deps1,
            RemoteCacheServiceDependencies deps2)
        {
        RemoteServiceDependenciesTest.assertCloneEquals(deps1, deps2);

        assertEquals(deps1.isDeferKeyAssociationCheck(), deps2.isDeferKeyAssociationCheck());
        }

    /**
     * Populate the dependencies and test the getters.
     *
     * @param deps  the DefaultRemoteCacheServiceDependencies to populate
     *
     * @return the DefaultRemoteCacheServiceDependencies that was passed in
     */
    public static DefaultRemoteCacheServiceDependencies populate(
            DefaultRemoteCacheServiceDependencies deps)
        {
        RemoteServiceDependenciesTest.populate(deps);

        boolean flag = !deps.isDeferKeyAssociationCheck();
        deps.setDeferKeyAssociationCheck(flag);
        assertEquals(flag, deps.isDeferKeyAssociationCheck());

        return deps;
        }
    }
