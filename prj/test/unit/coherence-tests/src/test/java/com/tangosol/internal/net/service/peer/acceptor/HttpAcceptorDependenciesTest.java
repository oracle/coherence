/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service.peer.acceptor;

import com.tangosol.util.AssertionException;

import junit.framework.Assert;

import org.junit.Test;

import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.Map;
import java.util.Random;

/**
 * Unit tests for HttpAcceptorDependencies.
 *
 * @author pfm  2011.09.22
 */
public class HttpAcceptorDependenciesTest
    {
    // ----- tests ----------------------------------------------------------

    /**
     * Test the setters and getters plus the clone.
     */
    @Test
    public void testAccessAndClone()
        {
        DefaultHttpAcceptorDependencies deps1 = new DefaultHttpAcceptorDependencies();

        System.out.println(deps1.toString());

        populate(deps1).validate();
        System.out.println(deps1.toString());

        DefaultHttpAcceptorDependencies deps2 = new DefaultHttpAcceptorDependencies(deps1);

        assertCloneEquals(deps1, deps2);
        deps2.validate();
        }

    /**
     * Method description
     */
    @Test
    public void nullHttpServer()
        {
        try
            {
            DefaultHttpAcceptorDependencies dependencies = populate(new DefaultHttpAcceptorDependencies());

            dependencies.setHttpServer(null);
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
    public void badPort()
        {
        try
            {
            DefaultHttpAcceptorDependencies dependencies = populate(new DefaultHttpAcceptorDependencies());

            dependencies.setLocalPort(65536);
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
    public void badAuthMethod()
        {
        try
            {
            Map<String, Object>             mapConfig = Collections.singletonMap("foobar", Mockito.mock(Object.class));

            DefaultHttpAcceptorDependencies dependencies = populate(new DefaultHttpAcceptorDependencies());

            dependencies.setResourceConfig(mapConfig);
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
     * Assert that the two DefaultHttpAcceptorDependencies are equal.
     *
     * @param deps1  the first DefaultHttpAcceptorDependencies object
     * @param deps2  the second DefaultHttpAcceptorDependencies object
     */
    public static void assertCloneEquals(DefaultHttpAcceptorDependencies deps1, DefaultHttpAcceptorDependencies deps2)
        {
        AcceptorDependenciesTest.assertCloneEquals(deps1, deps2);

        assertEquals(deps1.getConnectionLimit(), deps2.getConnectionLimit());
        assertEquals(deps1.getHttpServer(), deps2.getHttpServer());
        assertEquals(deps1.getLocalAddress(), deps2.getLocalAddress());
        assertEquals(deps1.getLocalPort(), deps2.getLocalPort());
        assertEquals(deps1.getResourceConfig(), deps2.getResourceConfig());
        assertEquals(deps1.getAuthMethod(), deps2.getAuthMethod());
        }

    /**
     * Populate the dependencies and test the getters.
     *
     * @param deps  the DefaultHttpAcceptorDependencies to populate
     *
     * @return the DefaultHttpAcceptorDependencies that was passed in
     */
    public static DefaultHttpAcceptorDependencies populate(DefaultHttpAcceptorDependencies deps)
        {
        AcceptorDependenciesTest.populate(deps);

        Object oServer = Mockito.mock(Object.class);

        deps.setHttpServer(oServer);
        assertEquals(oServer, deps.getHttpServer());

        String sAddr = "127.0.0.1";

        deps.setLocalAddress(sAddr);
        assertEquals(sAddr, deps.getLocalAddress());

        int nPort = 0;

        deps.setLocalPort(nPort);
        assertEquals(nPort, deps.getLocalPort());

        Map<String, Object> mapConfig = Collections.singletonMap("/", Mockito.mock(Object.class));

        deps.setResourceConfig(mapConfig);
        assertEquals(mapConfig, deps.getResourceConfig());

        return deps;
        }
    }
