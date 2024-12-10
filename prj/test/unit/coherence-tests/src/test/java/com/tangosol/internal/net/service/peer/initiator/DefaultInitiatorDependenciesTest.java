/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service.peer.initiator;

import com.tangosol.internal.net.service.peer.PeerDependenciesTest;
import com.tangosol.internal.net.service.peer.initiator.DefaultInitiatorDependencies;

import com.tangosol.util.AssertionException;

import junit.framework.Assert;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

import java.util.Random;

/**
 * Unit tests for DefaultInitiatorDependenciesTest.
 *
 * @author pfm  2011.09.22
 */
public class DefaultInitiatorDependenciesTest
    {
    // ----- tests ----------------------------------------------------------

    /**
     * Test the setters and getters plus the clone.
     */
    @Test
    public void testAccessAndClone()
        {
        ConcreteInitiatorDependencies deps1 = new ConcreteInitiatorDependencies();

        System.out.println(deps1.toString());

        populate(deps1).validate();
        System.out.println(deps1.toString());

        DefaultInitiatorDependencies deps2 = new ConcreteInitiatorDependencies(deps1);

        assertCloneEquals(deps1, deps2);
        deps2.validate();
        }

    /**
     * Method description
     */
    @Test
    public void badConnectTimeout()
        {
        try
            {
            ConcreteInitiatorDependencies dependencies = new ConcreteInitiatorDependencies();

            populate(dependencies);
            dependencies.setConnectTimeoutMillis(-1);
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

    /**
     * Method description
     */
    @Test
    public void badRequestSendTimeout()
        {
        try
            {
            ConcreteInitiatorDependencies dependencies = new ConcreteInitiatorDependencies();

            populate(dependencies);
            dependencies.setRequestSendTimeoutMillis(-1);
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
     * Assert that the two DefaultInitiatorDependencies are equal.
     *
     * @param deps1  the first DefaultInitiatorDependencies object
     * @param deps2  the second DefaultInitiatorDependencies object
     */
    public static void assertCloneEquals(DefaultInitiatorDependencies deps1, DefaultInitiatorDependencies deps2)
        {
        assertEquals(deps1.getConnectTimeoutMillis(), deps2.getConnectTimeoutMillis());
        assertEquals(deps1.getRequestSendTimeoutMillis(), deps2.getRequestSendTimeoutMillis());
        }

    /**
     * Populate the dependencies and test the getters.
     *
     * @param deps  the DefaultInitiatorDependencies to populate
     *
     * @return the DefaultInitiatorDependencies that was passed in
     */
    public static DefaultInitiatorDependencies populate(DefaultInitiatorDependencies deps)
        {
        PeerDependenciesTest.populate(deps);

        long ln = deps.getConnectTimeoutMillis() + new Random().nextInt(1000);

        deps.setConnectTimeoutMillis(ln);
        assertEquals(ln, deps.getConnectTimeoutMillis());

        deps.setRequestSendTimeoutMillis(ln = deps.getRequestSendTimeoutMillis() + new Random().nextInt(1000));
        assertEquals(ln, deps.getRequestSendTimeoutMillis());

        return deps;
        }

    // ----- inner class ----------------------------------------------------

    /**
     * A concrete class used to test DefaultInitiatorDependencies
     */
    protected static class ConcreteInitiatorDependencies
            extends DefaultInitiatorDependencies
        {
        /**
         * Construct a ConcreteInitiatorDependencies object.
         */
        public ConcreteInitiatorDependencies()
            {
            this(null);
            }

        /**
         * Construct a ConcreteInitiatorDependencies object, copying the values from the
         * specified InitiatorDependencies object.
         *
         * @param deps  the dependencies to copy, or null
         */
        public ConcreteInitiatorDependencies(InitiatorDependencies deps)
            {
            super(deps);
            }
        }
    }
