/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service;

import com.tangosol.io.ConfigurableSerializerFactory;

import com.tangosol.util.AssertionException;

import junit.framework.Assert;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Random;

/**
 * Unit tests for ServiceDependencies.
 *
 * @author pfm  2011.07.18
 */
public class ServiceDependenciesTest
    {
    // ----- tests ----------------------------------------------------------

    /**
     * Test the setters and getters plus the clone.
     */
    @Test
    public void testAccessAndClone()
        {
        DefaultServiceDependencies deps1 = new DefaultServiceDependencies();

        deps1.validate();
        System.out.println(deps1.toString());

        populate(deps1);
        deps1.validate();
        System.out.println(deps1.toString());

        DefaultServiceDependencies deps2 = new DefaultServiceDependencies(deps1);

        assertCloneEquals(deps1, deps2);
        deps2.validate();
        }

    /**
     * Method description
     */
    @Test
    public void badRequestTimeout()
        {
        try
            {
            DefaultServiceDependencies deps = new DefaultServiceDependencies();

            populate(deps);
            deps.setRequestTimeoutMillis(-1);
            deps.validate();
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
    public void badTaskHungThreshold()
        {
        try
            {
            DefaultServiceDependencies deps = new DefaultServiceDependencies();

            populate(deps);
            deps.setTaskHungThresholdMillis(-1);
            deps.validate();
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
    public void badTaskTimeout()
        {
        try
            {
            DefaultServiceDependencies deps = new DefaultServiceDependencies();

            populate(deps);
            deps.setTaskTimeoutMillis(-1);
            deps.validate();
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
     * Assert that the two ServiceDependencies are equal.
     *
     * @param deps1  the first ServiceDependencies object
     * @param deps2  the second ServiceDependencies object
     */
    public static void assertCloneEquals(ServiceDependencies deps1, ServiceDependencies deps2)
        {
        assertEquals(deps1.getRequestTimeoutMillis(), deps2.getRequestTimeoutMillis());
        assertEquals(deps1.getTaskHungThresholdMillis(), deps2.getTaskHungThresholdMillis());
        assertEquals(deps1.getTaskTimeoutMillis(), deps2.getTaskTimeoutMillis());
        assertEquals(deps1.getWorkerThreadCount(), deps2.getWorkerThreadCount());
        assertEquals(deps1.getSerializerFactory(), deps2.getSerializerFactory());
        }

    /**
     * Populate the dependencies and test the getters.
     *
     * @param deps  the DefaultServiceDependencies to populate
     *
     * @return the DefaultServiceDependencies that was passed in
     */
    public static void populate(DefaultServiceDependencies deps)
        {
        Random random = new Random();
        long   ln     = deps.getRequestTimeoutMillis() + random.nextInt(1000);

        deps.setRequestTimeoutMillis(ln);
        assertEquals(ln, deps.getRequestTimeoutMillis());

        deps.setSerializerFactory(new ConfigurableSerializerFactory());
        assertNotNull(deps.getSerializerFactory());

        deps.setTaskHungThresholdMillis(ln = random.nextInt(1000));
        assertEquals(ln, deps.getTaskHungThresholdMillis());

        deps.setTaskTimeoutMillis(ln = random.nextInt(1000));
        assertEquals(ln, deps.getTaskTimeoutMillis());

        int n = random.nextInt(20);
        if (n > 0)
            {
            n = Math.max(n, deps.getWorkerThreadCountMin());
            n = Math.min(n, deps.getWorkerThreadCountMax());
            }

        deps.setWorkerThreadCount(n);
        assertEquals(n, deps.getWorkerThreadCount());
        }
    }
