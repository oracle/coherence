/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service.peer.initiator;

import com.oracle.coherence.common.net.SocketProvider;

import com.tangosol.coherence.config.builder.LocalAddressProviderBuilder;
import com.tangosol.coherence.config.builder.SocketProviderBuilder;
import com.tangosol.coherence.config.builder.WrapperSocketAddressProviderBuilder;

import com.tangosol.net.SocketOptions;

import com.tangosol.util.AssertionException;

import junit.framework.Assert;

import org.junit.Test;

import org.mockito.Mockito;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.net.InetSocketAddress;

/**
 * Unit tests for TcpInitiatorDependencies.
 *
 * @author pfm  2011.09.22
 */
public class TcpInitiatorDependenciesTest
    {
    // ----- tests ----------------------------------------------------------

    /**
     * Test the setters and getters plus the clone.
     */
    @Test
    public void testAccessAndClone()
        {
        DefaultTcpInitiatorDependencies deps1 = new DefaultTcpInitiatorDependencies();

        populate(deps1).validate();

        DefaultTcpInitiatorDependencies deps2 = new DefaultTcpInitiatorDependencies(deps1);

        assertCloneEquals(deps1, deps2);
        deps2.validate();
        }

    /**
     * Method description
     */
    @Test
    public void missingSocketProvider()
        {
        try
            {
            DefaultTcpInitiatorDependencies dependencies = new DefaultTcpInitiatorDependencies();

            populate(dependencies);
            dependencies.setSocketProviderBuilder(null);
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
     * Assert that the two DefaultTcpInitiatorDependencies are equal.
     *
     * @param deps1  the first DefaultTcpInitiatorDependencies object
     * @param deps2  the second DefaultTcpInitiatorDependencies object
     */
    public static void assertCloneEquals(DefaultTcpInitiatorDependencies deps1, DefaultTcpInitiatorDependencies deps2)
        {
        DefaultInitiatorDependenciesTest.assertCloneEquals(deps1, deps2);

        assertSame(deps1.getLocalAddress(),                 deps2.getLocalAddress());
        assertSame(deps1.getRemoteAddressProviderBuilder(), deps2.getRemoteAddressProviderBuilder());
        assertSame(deps1.getSocketOptions(),                deps2.getSocketOptions());
        assertSame(deps1.getSocketProviderBuilder(),        deps2.getSocketProviderBuilder());
        }

    /**
     * Populate the dependencies and test the getters.
     *
     * @param deps  the DefaultTcpInitiatorDependencies to populate
     *
     * @return the DefaultTcpInitiatorDependencies that was passed in
     */
    public static DefaultTcpInitiatorDependencies populate(DefaultTcpInitiatorDependencies deps)
        {
        DefaultInitiatorDependenciesTest.populate(deps);

        InetSocketAddress addr = new InetSocketAddress("1.1.1.0",1);
        deps.setLocalAddress(addr);
        assertSame(addr, deps.getLocalAddress());

        deps.setRemoteAddressProviderBuilder(new LocalAddressProviderBuilder("1.1.1.0", 9090, 9090));
        assertTrue(deps.getRemoteAddressProviderBuilder() instanceof WrapperSocketAddressProviderBuilder);

        SocketOptions socketOptions = Mockito.mock(SocketOptions.class);

        deps.setSocketOptions(socketOptions);
        assertSame(socketOptions, deps.getSocketOptions());

        SocketProvider socketProvider = Mockito.mock(SocketProvider.class);
        deps.setSocketProviderBuilder(new SocketProviderBuilder(socketProvider, false));
        assertSame(socketProvider, deps.getSocketProviderBuilder().realize(null, null, null));

        return deps;
        }
    }
