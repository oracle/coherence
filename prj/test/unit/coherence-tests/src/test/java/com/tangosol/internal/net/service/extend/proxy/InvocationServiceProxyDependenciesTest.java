/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service.extend.proxy;

import org.junit.Test;

/**
 * Unit tests for InvocationServiceProxyDependenciesTest.
 *
 * @author pfm  2011.09.26
 */
public class InvocationServiceProxyDependenciesTest
    {
    // ----- tests ----------------------------------------------------------

    /**
     * Test the setters and getters plus the clone.
     */
    @Test
    public void testAccessAndClone()
        {
        DefaultInvocationServiceProxyDependencies deps1 =
            new DefaultInvocationServiceProxyDependencies();
        deps1.validate();
        System.out.println(deps1.toString());

        populate(deps1).validate();
        System.out.println(deps1.toString());

        DefaultInvocationServiceProxyDependencies deps2 =
            new DefaultInvocationServiceProxyDependencies(deps1);
        assertCloneEquals(deps1, deps2);
        deps2.validate();
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Assert that the two InvocationServiceProxyDependencies are equal.
     *
     * @param deps1  the first InvocationServiceProxyDependencies object
     * @param deps2  the second InvocationServiceProxyDependencies object
     */
    public static void assertCloneEquals(InvocationServiceProxyDependencies deps1,
            InvocationServiceProxyDependencies deps2)
        {
        ServiceProxyDependenciesTest.assertCloneEquals(deps1, deps2);
        }

    /**
     * Populate the dependencies and test the getters.
     *
     * @param deps  the DefaultInvocationServiceProxyDependencies to populate
     *
     * @return the DefaultInvocationServiceProxyDependencies that was passed in
     */
    public static DefaultInvocationServiceProxyDependencies populate(
            DefaultInvocationServiceProxyDependencies deps)
        {
        ServiceProxyDependenciesTest.populate(deps);

        return deps;
        }
    }
