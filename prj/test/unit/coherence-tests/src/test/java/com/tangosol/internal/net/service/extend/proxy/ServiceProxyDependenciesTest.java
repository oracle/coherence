/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service.extend.proxy;

import com.tangosol.run.xml.XmlElement;

import org.junit.Test;

import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for ServiceProxyDependencies.
 *
 * @author pfm  2011.09.26
 */
public class ServiceProxyDependenciesTest
    {
    // ----- tests ----------------------------------------------------------

    /**
     * Test the setters and getters plus the clone.
     */
    @Test
    public void testAccessAndClone()
        {
        DefaultServiceProxyDependencies deps1 = new DefaultServiceProxyDependencies();
        deps1.validate();
        System.out.println(deps1.toString());

        populate(deps1).validate();
        System.out.println(deps1.toString());

        DefaultServiceProxyDependencies deps2 = new DefaultServiceProxyDependencies(deps1);
        assertCloneEquals(deps1, deps2);
        deps2.validate();
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Assert that the two ServiceProxyDependencies are equal.
     *
     * @param deps1  the first ServiceProxyDependencies object
     * @param deps2  the second ServiceProxyDependencies object
     */
    public static void assertCloneEquals(ServiceProxyDependencies deps1,
            ServiceProxyDependencies deps2)
        {
        assertEquals(deps1.getServiceClassConfig(), deps2.getServiceClassConfig());
        }

    /**
     * Populate the dependencies and test the getters.
     *
     * @param deps  the DefaultServiceProxyDependencies to populate
     *
     * @return the DefaultServiceProxyDependencies that was passed in
     */
    public static DefaultServiceProxyDependencies populate(
            DefaultServiceProxyDependencies deps)
        {
        XmlElement xml = Mockito.mock(XmlElement.class);
        deps.setServiceClassConfig(xml);
        assertEquals(xml, deps.getServiceClassConfig());

        return deps;
        }
    }
