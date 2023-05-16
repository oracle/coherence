/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service;

import com.tangosol.io.ConfigurableSerializerFactory;

import com.tangosol.util.AssertionException;

import junit.framework.Assert;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

import java.util.Random;

/**
 * Unit tests for ServiceDependencies.
 *
 * @author pfm  2011.07.18
 */
@SuppressWarnings("deprecation")
public class ServiceDependenciesTest
    {
    // ----- tests ----------------------------------------------------------

    @Test
    public void testThreadCounts()
        {
        DefaultServiceDependencies deps = new DefaultServiceDependencies();

        deps.setWorkerThreadCountMin(4);
        deps.setWorkerThreadCount(0);


        deps.validate();  // should not throw

        // deprecated setting will take precedence over
        // new configuration settings
        assertEquals(0, deps.getWorkerThreadCountMin());
        assertEquals(0, deps.getWorkerThreadCountMax());


        // simulate behavior on windows
        deps = new DefaultServiceDependencies();
        deps.setWorkerThreadCount(0);
        deps.setWorkerThreadCountMin(4);

        deps.validate();  // should not throw

        // deprecated setting will take precedence over
        // new configuration settings
        assertEquals(0, deps.getWorkerThreadCountMin());
        assertEquals(0, deps.getWorkerThreadCountMax());

        // validate min > max - this should result in max being set to min
        deps = new DefaultServiceDependencies();
        deps.setWorkerThreadCountMin(4);
        deps.setWorkerThreadCountMax(0);

        deps.validate();

        assertEquals(4, deps.getWorkerThreadCountMin());
        assertEquals(4, deps.getWorkerThreadCountMax());

        // validate min/max -1
        deps = new DefaultServiceDependencies();
        deps.setWorkerThreadCountMin(-1);
        deps.setWorkerThreadCountMax(-1);

        deps.validate();  // should not throw

        assertEquals(-1, deps.getWorkerThreadCountMin());
        assertEquals(-1, deps.getWorkerThreadCountMax());

        // validate min -1 and max Integer.MAX_VALUE
        deps = new DefaultServiceDependencies();
        deps.setWorkerThreadCountMin(-1);
        deps.setWorkerThreadCountMax(Integer.MAX_VALUE);

        deps.validate();  // should not throw

        assertEquals(-1,                deps.getWorkerThreadCountMin());
        assertEquals(Integer.MAX_VALUE, deps.getWorkerThreadCountMax());

        // validate min -1 and max some other value other than -1
        // or Integer.MAX_VALUE
        deps = new DefaultServiceDependencies();
        deps.setWorkerThreadCountMin(-1);
        deps.setWorkerThreadCountMax(5);

        assertThrows(AssertionException.class, deps::validate);
        }

    /**
     * Test the setters and getters plus the clone.
     */
    @Test
    public void testAccessAndClone()
        {
        DefaultServiceDependencies deps1 = new DefaultServiceDependencies();

        deps1.validate();
        System.out.println(deps1);

        populate(deps1);
        deps1.validate();
        System.out.println(deps1);

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
        catch (IllegalArgumentException | AssertionException e)
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
        catch (IllegalArgumentException | AssertionException e)
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
        catch (IllegalArgumentException | AssertionException e)
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
