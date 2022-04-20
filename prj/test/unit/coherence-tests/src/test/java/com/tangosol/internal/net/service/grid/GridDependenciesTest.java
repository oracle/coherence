/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service.grid;

import com.tangosol.coherence.config.builder.ParameterizedBuilder;
import com.tangosol.coherence.config.builder.ServiceFailurePolicyBuilder;
import com.tangosol.internal.net.cluster.DefaultServiceFailurePolicy;
import com.tangosol.internal.net.service.ServiceDependenciesTest;

import com.tangosol.net.MemberListener;
import com.tangosol.net.ServiceFailurePolicy;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Unit tests for GridDependencies.
 *
 * @author pfm  2011.07.18
 */
public class GridDependenciesTest
    {
    // ----- tests ----------------------------------------------------------

    /**
     * Test the setters and getters plus the clone.
     */
    @Test
    public void testAccessAndClone()
        {
        DefaultGridDependencies deps1 = new DefaultGridDependencies();

        deps1.validate();
        System.out.println(deps1.toString());

        populate(deps1).validate();
        System.out.println(deps1.toString());

        DefaultGridDependencies deps2 = new DefaultGridDependencies(deps1);

        assertCloneEquals(deps1, deps2);
        deps2.validate();
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Assert that the two GridDependencies are equal.
     *
     * @param deps1  the first GridDependencies object
     * @param deps2  the second GridDependencies object
     */
    public static void assertCloneEquals(GridDependencies deps1, GridDependencies deps2)
        {
        ServiceDependenciesTest.assertCloneEquals(deps1, deps2);

        assertEquals(deps1.getDefaultGuardTimeoutMillis(), deps2.getDefaultGuardTimeoutMillis());
        assertEquals(deps1.getServiceFailurePolicyBuilder(), deps2.getServiceFailurePolicyBuilder());
        assertEquals(deps1.getMemberListenerBuilders(), deps2.getMemberListenerBuilders());
        }

    /**
     * Populate the dependencies and test the getters.
     *
     * @param deps  the DefaultGridDependencies to populate
     *
     * @return the DefaultGridDependencies that was passed in
     */
    public static DefaultGridDependencies populate(DefaultGridDependencies deps)
        {
        ServiceDependenciesTest.populate(deps);

        long ln = new Random().nextInt(1000);

        deps.setDefaultGuardTimeoutMillis(ln);
        assertEquals(ln, deps.getDefaultGuardTimeoutMillis());

        ServiceFailurePolicyBuilder builder = new ServiceFailurePolicyBuilder(DefaultServiceFailurePolicy.POLICY_EXIT_CLUSTER);
        deps.setServiceFailurePolicyBuilder(builder);
        assertEquals(builder, deps.getServiceFailurePolicyBuilder());

        List<ParameterizedBuilder<MemberListener>> listeners = new ArrayList<>(1);

        deps.setMemberListenerBuilders(listeners);
        assertEquals(listeners, deps.getMemberListenerBuilders());

        return deps;
        }
    }
