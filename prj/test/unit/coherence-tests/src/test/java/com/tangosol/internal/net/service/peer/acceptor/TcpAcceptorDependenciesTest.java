/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service.peer.acceptor;

import com.tangosol.coherence.config.builder.AddressProviderBuilder;
import com.tangosol.coherence.config.builder.LocalAddressProviderBuilder;
import com.tangosol.coherence.config.builder.ParameterizedBuilder;
import com.tangosol.coherence.config.builder.SocketProviderBuilder;
import com.tangosol.coherence.config.builder.WrapperSocketAddressProviderBuilder;
import com.tangosol.internal.net.service.peer.acceptor.TcpAcceptorDependencies.BufferPoolConfig;

import com.tangosol.net.SocketOptions;

import com.oracle.coherence.common.net.SocketProvider;

import com.tangosol.util.Filter;

import java.util.Random;

import org.junit.Test;

import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for TcpAcceptorDependencies.
 *
 * @author pfm  2011.09.22
 */
public class TcpAcceptorDependenciesTest
    {
    // ----- tests ----------------------------------------------------------

    /**
     * Test the setters and getters plus the clone.
     */
    @Test
    public void testAccessAndClone()
        {
        DefaultTcpAcceptorDependencies deps1 = new DefaultTcpAcceptorDependencies();
        System.out.println(deps1.toString());

        populate(deps1).validate();
        System.out.println(deps1.toString());

        DefaultTcpAcceptorDependencies deps2 = new DefaultTcpAcceptorDependencies(deps1);
        assertCloneEquals(deps1, deps2);
        deps2.validate();
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Assert that the two DefaultTcpAcceptorDependencies are equal.
     *
     * @param deps1  the first DefaultTcpAcceptorDependencies object
     * @param deps2  the second DefaultTcpAcceptorDependencies object
     */
    public static void assertCloneEquals(DefaultTcpAcceptorDependencies deps1,
            DefaultTcpAcceptorDependencies deps2)
        {
        AcceptorDependenciesTest.assertCloneEquals(deps1, deps2);

        assertEquals(deps1.getAuthorizedHostFilterBuilder(), deps2.getAuthorizedHostFilterBuilder());
        assertEquals(deps1.getDefaultLimitBytes(),           deps2.getDefaultLimitBytes());
        assertEquals(deps1.getDefaultLimitMessages(),        deps2.getDefaultLimitMessages());
        assertEquals(deps1.getDefaultNominalBytes(),         deps2.getDefaultNominalBytes());
        assertEquals(deps1.getDefaultNominalMessages(),      deps2.getDefaultNominalMessages());
        assertEquals(deps1.getDefaultSuspectBytes(),         deps2.getDefaultSuspectBytes());
        assertEquals(deps1.getDefaultSuspectMessages(),      deps2.getDefaultSuspectMessages());
        assertEquals(deps1.getListenBacklog(),               deps2.getListenBacklog());
        assertEquals(deps1.getLocalAddressProviderBuilder(), deps2.getLocalAddressProviderBuilder());
        assertEquals(deps1.getOutgoingBufferPoolConfig(),    deps2.getOutgoingBufferPoolConfig());
        assertEquals(deps1.getSocketOptions(),               deps2.getSocketOptions());
        assertEquals(deps1.getSocketProviderBuilder(),       deps2.getSocketProviderBuilder());
        assertEquals(deps1.isSuspectProtocolEnabled(),       deps2.isSuspectProtocolEnabled());
        }

    /**
     * Populate the dependencies and test the getters.
     *
     * @param deps  the DefaultTcpAcceptorDependencies to populate
     *
     * @return the DefaultTcpAcceptorDependencies that was passed in
     */
    public static DefaultTcpAcceptorDependencies populate(DefaultTcpAcceptorDependencies deps)
        {
        AcceptorDependenciesTest.populate(deps);

        ParameterizedBuilder<Filter> bldrFilter = (ParameterizedBuilder<Filter>)Mockito.mock(ParameterizedBuilder.class);
        deps.setAuthorizedHostFilterBuilder(bldrFilter);
        assertEquals(bldrFilter, deps.getAuthorizedHostFilterBuilder());

        Random random = new Random();
        long ln = random.nextInt(1000);
        deps.setDefaultLimitBytes(ln);
        assertEquals(ln, deps.getDefaultLimitBytes());

        int n = random.nextInt(1000);
        deps.setDefaultLimitMessages(n);
        assertEquals(n, deps.getDefaultLimitMessages());

        deps.setDefaultNominalBytes(ln = random.nextInt(1000));
        assertEquals(ln, deps.getDefaultNominalBytes());

        deps.setDefaultNominalMessages(n = random.nextInt(1000));
        assertEquals(n, deps.getDefaultNominalMessages());

        deps.setDefaultSuspectBytes(ln = random.nextInt(1000));
        assertEquals(ln, deps.getDefaultSuspectBytes());

        deps.setDefaultSuspectMessages(n = random.nextInt(1000));
        assertEquals(n, deps.getDefaultSuspectMessages());

        deps.setListenBacklog(n = random.nextInt(1000));
        assertEquals(n, deps.getListenBacklog());

        BufferPoolConfig poolConfig = Mockito.mock(BufferPoolConfig.class);
        deps.setOutgoingBufferPoolConfig(poolConfig);
        assertEquals(poolConfig, deps.getOutgoingBufferPoolConfig());

        SocketOptions socketOptions = Mockito.mock(SocketOptions.class);
        deps.setSocketOptions(socketOptions);
        assertEquals(socketOptions, deps.getSocketOptions());

        SocketProvider socketProvider = Mockito.mock(SocketProvider.class);
        deps.setSocketProviderBuilder(new SocketProviderBuilder(socketProvider, false));
        assertEquals(socketProvider, deps.getSocketProviderBuilder().realize(null, null, null));

        deps.setSuspectProtocolEnabled(!deps.isSuspectProtocolEnabled());
        assertEquals(n, deps.getListenBacklog());

        AddressProviderBuilder bldrAddressProvider = new LocalAddressProviderBuilder("127.0.0.1", 9090, 9090);
        deps.setLocalAddressProviderBuilder(bldrAddressProvider);
        assertTrue(deps.getLocalAddressProviderBuilder() instanceof WrapperSocketAddressProviderBuilder);

        return deps;
        }
    }
