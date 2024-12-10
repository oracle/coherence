/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service.peer;

import com.tangosol.util.AssertionException;
import com.tangosol.util.UUID;

import junit.framework.Assert;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

import java.util.Random;

/**
 * Unit tests for JmsDependencies.
 *
 * @author pfm  2011.09.22
 */
public class JmsDependenciesTest
    {
    // ----- tests ----------------------------------------------------------

    /**
     * Test the setters and getters plus the clone.
     */
    @Test
    public void testAccessAndClone()
        {
        CommonJmsDependencies deps1 = new CommonJmsDependencies();

        System.out.println(deps1.toString());

        populate(deps1).validate();
        System.out.println(deps1.toString());

        CommonJmsDependencies deps2 = new CommonJmsDependencies(deps1);

        assertCloneEquals(deps1, deps2);
        deps2.validate();
        }

    /**
     * Method description
     */
    @Test
    public void nullQueueName()
        {
        try
            {
            CommonJmsDependencies dependencies = populate(new CommonJmsDependencies());

            dependencies.setQueueName(null);
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
    public void emptyQueueName()
        {
        try
            {
            CommonJmsDependencies dependencies = populate(new CommonJmsDependencies());

            dependencies.setQueueName("");
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
    public void nullQueueConnectionFactoryName()
        {
        try
            {
            CommonJmsDependencies dependencies = populate(new CommonJmsDependencies());

            dependencies.setQueueConnectionFactoryName(null);
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
    public void emptyQueueConnectionFactoryName()
        {
        try
            {
            CommonJmsDependencies dependencies = populate(new CommonJmsDependencies());

            dependencies.setQueueConnectionFactoryName("");
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
     * Assert that the two JmsDependencies are equal.
     *
     * @param deps1  the first JmsDependencies object
     * @param deps2  the second JmsDependencies object
     */
    public static void assertCloneEquals(JmsDependencies deps1, JmsDependencies deps2)
        {
        assertEquals(deps1.getMessageDeliveryMode(), deps2.getMessageDeliveryMode());
        assertEquals(deps1.getMessageExpiration(), deps2.getMessageExpiration());
        assertEquals(deps1.getMessagePriority(), deps2.getMessagePriority());
        assertEquals(deps1.getQueueConnectionFactoryName(), deps2.getQueueConnectionFactoryName());
        assertEquals(deps1.getQueueName(), deps2.getQueueName());
        }

    /**
     * Populate the dependencies and test the getters.
     *
     * @param deps  the CommonJmsDependencies to populate
     *
     * @return the CommonJmsDependencies that was passed in
     */
    public static CommonJmsDependencies populate(CommonJmsDependencies deps)
        {
        Random random = new Random();
        int    n      = random.nextInt(10);

        deps.setMessageDeliveryMode(n);
        assertEquals(n, deps.getMessageDeliveryMode());

        long ln = random.nextInt(10000);

        deps.setMessageExpiration(ln);
        assertEquals(ln, deps.getMessageExpiration());

        deps.setMessagePriority(n = random.nextInt(100));
        assertEquals(n, deps.getMessagePriority());

        String s = "TestQCF-Name" + new UUID().toString();

        deps.setQueueConnectionFactoryName(s);
        assertEquals(s, deps.getQueueConnectionFactoryName());

        deps.setQueueName(s = "TestQ-Name" + new UUID().toString());
        assertEquals(s, deps.getQueueName());

        return deps;
        }
    }
