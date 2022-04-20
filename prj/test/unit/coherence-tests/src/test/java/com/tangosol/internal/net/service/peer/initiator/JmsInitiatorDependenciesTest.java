/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service.peer.initiator;

import com.tangosol.internal.net.service.peer.JmsDependenciesTest;

import org.junit.Test;

/**
 * Unit tests for JmsInitiatorDependencies.
 *
 * @author pfm  2011.09.22
 */
public class JmsInitiatorDependenciesTest
    {
    // ----- tests ----------------------------------------------------------

    /**
     * Test the setters and getters plus the clone.
     */
    @Test
    public void testAccessAndClone()
        {
        DefaultJmsInitiatorDependencies deps1 = new DefaultJmsInitiatorDependencies();
        System.out.println(deps1.toString());

        populate(deps1).validate();
        System.out.println(deps1.toString());

        DefaultJmsInitiatorDependencies deps2 = new DefaultJmsInitiatorDependencies(deps1);
        deps2.validate();
        DefaultInitiatorDependenciesTest.assertCloneEquals(deps1, deps2);
        JmsDependenciesTest.assertCloneEquals(deps1, deps2);
        }

    /**
     * Populate the dependencies and test the getters.
     *
     * @param deps  the DefaultJmsInitiatorDependencies to populate
     *
     * @return the DefaultJmsInitiatorDependencies that was passed in
     */
    public static DefaultJmsInitiatorDependencies populate(
            DefaultJmsInitiatorDependencies deps)
        {
        DefaultInitiatorDependenciesTest.populate(deps);
        JmsDependenciesTest.populate(deps.getCommonDependencies());

        return deps;
        }
    }