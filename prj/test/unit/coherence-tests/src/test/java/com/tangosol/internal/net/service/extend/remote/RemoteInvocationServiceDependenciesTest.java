/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service.extend.remote;

import org.junit.Test;

/**
 * Unit tests for RemoteInvocationServiceDependenciesTest.
 *
 * @author pfm  2011.09.26
 */
public class RemoteInvocationServiceDependenciesTest
    {
    // ----- tests ----------------------------------------------------------

    /**
     * Test the setters and getters plus the clone.
     */
    @Test
    public void testAccessAndClone()
        {
        DefaultRemoteInvocationServiceDependencies deps1 =
            new DefaultRemoteInvocationServiceDependencies();
        System.out.println(deps1.toString());

        populate(deps1).validate();
        System.out.println(deps1.toString());

        DefaultRemoteInvocationServiceDependencies deps2 =
            new DefaultRemoteInvocationServiceDependencies(deps1);
        assertCloneEquals(deps1, deps2);
        deps2.validate();
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Assert that the two RemoteInvocationServiceDependencies are equal.
     *
     * @param deps1  the first RemoteInvocationServiceDependencies object
     * @param deps2  the second RemoteInvocationServiceDependencies object
     */
    public static void assertCloneEquals(RemoteInvocationServiceDependencies deps1,
            RemoteInvocationServiceDependencies deps2)
        {
        RemoteServiceDependenciesTest.assertCloneEquals(deps1, deps2);
        }

    /**
     * Populate the dependencies and test the getters.
     *
     * @param deps  the DefaultRemoteInvocationServiceDependencies to populate
     *
     * @return the DefaultRemoteInvocationServiceDependencies that was passed in
     */
    public static DefaultRemoteInvocationServiceDependencies populate(
            DefaultRemoteInvocationServiceDependencies deps)
        {
        RemoteServiceDependenciesTest.populate(deps);

        return deps;
        }
    }
