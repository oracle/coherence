/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service.peer;

import com.tangosol.internal.net.service.ServiceDependenciesTest;

import com.tangosol.io.WrapperStreamFactory;
import com.tangosol.net.messaging.Codec;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.Test;

import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for PeerDependencies.
 *
 * @author pfm  2011.009.22
 */
public class PeerDependenciesTest
    {
    // ----- tests ----------------------------------------------------------

    /**
     * Test the setters and getters plus the clone.
     */
    @Test
    public void testAccessAndClone()
        {
        DefaultPeerDependencies deps1 = new DefaultPeerDependencies();
        deps1.validate();
        System.out.println(deps1.toString());

        populate(deps1).validate();
        System.out.println(deps1.toString());

        DefaultPeerDependencies deps2 = new DefaultPeerDependencies(deps1);
        assertCloneEquals(deps1, deps2);
        deps2.validate();
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Assert that the two PeerDependencies are equal.
     *
     * @param deps1  the first PeerDependencies object
     * @param deps2  the second PeerDependencies object
     */
    public static void assertCloneEquals(PeerDependencies deps1, PeerDependencies deps2)
        {
        ServiceDependenciesTest.assertCloneEquals(deps1, deps2);

        assertEquals(deps1.getFilterList(),             deps2.getFilterList());
        assertEquals(deps1.getMessageCodec(),           deps2.getMessageCodec());
        assertEquals(deps1.getPingIntervalMillis(),     deps2.getPingIntervalMillis());
        assertEquals(deps1.getPingTimeoutMillis(),      deps2.getPingTimeoutMillis());
        assertEquals(deps1.getMaxIncomingMessageSize(), deps2.getMaxIncomingMessageSize());
        assertEquals(deps1.getMaxOutgoingMessageSize(), deps2.getMaxOutgoingMessageSize());
        }

    /**
     * Populate the dependencies and test the getters.
     *
     * @param deps  the DefaultPeerDependencies to populate
     *
     * @return the DefaultPeerDependencies that was passed in
     */
    public static DefaultPeerDependencies populate(DefaultPeerDependencies deps)
        {
        ServiceDependenciesTest.populate(deps);

        List<WrapperStreamFactory> listFilter = new ArrayList<WrapperStreamFactory>(1);
        deps.setFilterList(listFilter);
        assertEquals(listFilter, deps.getFilterList());

        Codec codec = Mockito.mock(Codec.class);
        deps.setMessageCodec(codec);
        assertEquals(codec, deps.getMessageCodec());

        Random random = new Random();
        long ln = random.nextInt(1000);
        deps.setPingIntervalMillis(ln);
        assertEquals(ln, deps.getPingIntervalMillis());

        deps.setPingTimeoutMillis(ln = random.nextInt(1000));
        assertEquals(ln, deps.getPingTimeoutMillis());

        int cbSize = 4096;
        deps.setMaxIncomingMessageSize(cbSize);
        assertEquals(cbSize, deps.getMaxIncomingMessageSize());

        cbSize = 1024;
        deps.setMaxOutgoingMessageSize(cbSize);
        assertEquals(cbSize, deps.getMaxOutgoingMessageSize());

        return deps;
        }
    }
