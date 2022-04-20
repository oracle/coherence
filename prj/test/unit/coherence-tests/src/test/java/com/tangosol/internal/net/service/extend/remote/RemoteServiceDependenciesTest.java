/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service.extend.remote;

import com.tangosol.internal.net.service.peer.initiator.DefaultTcpInitiatorDependencies;
import com.tangosol.internal.net.service.peer.initiator.InitiatorDependencies;

import com.tangosol.util.AssertionException;

import junit.framework.Assert;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for RemoteServiceDependencies.
 *
 * @author pfm  2011.09.26
 */
public class RemoteServiceDependenciesTest
    {
    // ----- tests ----------------------------------------------------------

    /**
     * Test the setters and getters plus the clone.
     */
    @Test
    public void testAccessAndClone()
        {
        DefaultRemoteServiceDependencies deps1 = new DefaultRemoteServiceDependencies();

        System.out.println(deps1.toString());

        populate(deps1).validate();
        System.out.println(deps1.toString());

        DefaultRemoteServiceDependencies deps2 = new DefaultRemoteServiceDependencies(deps1);

        assertCloneEquals(deps1, deps2);
        deps2.validate();
        }

    /**
     * Method description
     */
    @Test
    public void nullInitiatorDependencies()
        {
        try
            {
            DefaultRemoteServiceDependencies dependencies = new DefaultRemoteCacheServiceDependencies();

            populate(dependencies);
            dependencies.setInitiatorDependencies(null);
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
     * Assert that the two RemoteServiceDependencies are equal.
     *
     * @param deps1  the first RemoteServiceDependencies object
     * @param deps2  the second RemoteServiceDependencies object
     */
    public static void assertCloneEquals(RemoteServiceDependencies deps1, RemoteServiceDependencies deps2)
        {
        assertEquals(deps1.getInitiatorDependencies(), deps2.getInitiatorDependencies());
        }

    /**
     * Populate the dependencies and test the getters.
     *
     * @param deps  the DefaultRemoteServiceDependencies to populate
     *
     * @return the DefaultRemoteServiceDependencies that was passed in
     */
    public static DefaultRemoteServiceDependencies populate(DefaultRemoteServiceDependencies deps)
        {
        InitiatorDependencies initiatorDeps = new DefaultTcpInitiatorDependencies();

        deps.setInitiatorDependencies(initiatorDeps);
        assertEquals(initiatorDeps, deps.getInitiatorDependencies());

        return deps;
        }
    }
