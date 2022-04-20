/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service.peer.acceptor;

import com.tangosol.internal.net.service.peer.JmsDependenciesTest;

import org.junit.Test;

/**
 * Unit tests for JmsAcceptorDependencies.
 *
 * @author pfm  2011.09.22
 */
public class JmsAcceptorDependenciesTest
    {
    // ----- tests ----------------------------------------------------------

    /**
     * Test the setters and getters plus the clone.
     */
    @Test
    public void testAccessAndClone()
        {
        DefaultJmsAcceptorDependencies deps1 = new DefaultJmsAcceptorDependencies();
        System.out.println(deps1.toString());

        populate(deps1).validate();
        System.out.println(deps1.toString());

        DefaultJmsAcceptorDependencies deps2 = new DefaultJmsAcceptorDependencies(deps1);
        deps2.validate();
        AcceptorDependenciesTest.assertCloneEquals(deps1, deps2);
        JmsDependenciesTest.assertCloneEquals(deps1, deps2);
        }

    /**
     * Populate the dependencies and test the getters.
     *
     * @param deps  the DefaultJmsAcceptorDependencies to populate
     *
     * @return the DefaultJmsAcceptorDependencies that was passed in
     */
    public static DefaultJmsAcceptorDependencies populate(
            DefaultJmsAcceptorDependencies deps)
        {
        AcceptorDependenciesTest.populate(deps);
        JmsDependenciesTest.populate(deps.getCommonDependencies());

        return deps;
        }
    }
