/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service.peer.acceptor;

import com.tangosol.internal.net.service.peer.PeerDependenciesTest;

import com.tangosol.util.AssertionException;

import java.util.Random;

import junit.framework.Assert;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for AbstractAcceptorDependencies.
 *
 * @author pfm  2011.09.22
 */
public class AcceptorDependenciesTest
    {
    // ----- tests ----------------------------------------------------------

    /**
     * Test the setters and getters plus the clone.
     */
    @Test
    public void testAccessAndClone()
        {
        AbstractAcceptorDependencies deps1 = new ConcreteAcceptorDependencies();
        System.out.println(deps1.toString());

        populate(deps1).validate();
        System.out.println(deps1.toString());

        AbstractAcceptorDependencies deps2 = new ConcreteAcceptorDependencies(deps1);
        assertCloneEquals(deps1, deps2);
        deps2.validate();
        }

    @Test
    public void badConnectionLimit()
        {
        try
            {
            //TODO: refactor this when new validation is in place
            //populate(new ConcreteAcceptorDependencies()).setConnectionLimit(-1).validate();
            //Assert.fail();
            }
        catch (IllegalArgumentException e)
            {
            return;
            }
        catch (AssertionException e)
            {
            return;
            }
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Assert that the two AbstractAcceptorDependencies are equal.
     *
     * @param deps1  the first AbstractAcceptorDependencies object
     * @param deps2  the second AbstractAcceptorDependencies object
     */
    public static void assertCloneEquals(AbstractAcceptorDependencies deps1,
            AbstractAcceptorDependencies deps2)
        {
        PeerDependenciesTest.assertCloneEquals(deps1, deps2);

        assertEquals(deps1.getConnectionLimit(), deps2.getConnectionLimit());
        }

    /**
     * Populate the dependencies and test the getters.
     *
     * @param deps  the AbstractAcceptorDependencies to populate
     *
     * @return the AbstractAcceptorDependencies that was passed in
     */
    public static AbstractAcceptorDependencies populate(AbstractAcceptorDependencies deps)
        {
        PeerDependenciesTest.populate(deps);

        int n = new Random().nextInt(100);
        deps.setConnectionLimit(n);
        assertEquals(n, deps.getConnectionLimit());

        return deps;
        }

    // ----- inner class ----------------------------------------------------

    /**
     * A concrete class used to test AbstractAcceptorDependencies
     */
    protected static class ConcreteAcceptorDependencies
            extends AbstractAcceptorDependencies
        {
        /**
         * Construct a ConcreteAcceptorDependencies object.
         */
        public ConcreteAcceptorDependencies()
            {
            this(null);
            }

        /**
         * Construct a ConcreteAcceptorDependencies object, copying the values from the
         * specified AcceptorDependencies object.
         *
         * @param deps  the dependencies to copy, or null
         */
        public ConcreteAcceptorDependencies(AcceptorDependencies deps)
            {
            super(deps);
            }
        }
    }
