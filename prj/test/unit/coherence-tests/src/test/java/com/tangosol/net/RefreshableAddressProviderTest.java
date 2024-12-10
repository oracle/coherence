/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net;


import org.junit.Test;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author jk  2013.11.20
 */
public class RefreshableAddressProviderTest
    {
    /**
     * This is a test for bug COH-10698
     */
    @Test
    public void shouldGetNextAddressAfterRejectingFinalAddressInList() throws Exception
        {
        InetAddress       host    = InetAddress.getByName("127.0.0.1");
        InetSocketAddress socket1 = new InetSocketAddress(host, 10000);
        InetSocketAddress socket2 = new InetSocketAddress(host, 10001);
        AddressProvider   ap      = mock(AddressProvider.class);

        when(ap.getNextAddress()).thenReturn(socket1, socket2, null);

        RefreshableAddressProvider refreshableAddressProvider = new RefreshableAddressProvider(ap);
        refreshableAddressProvider.getNextAddress();
        refreshableAddressProvider.getNextAddress();
        refreshableAddressProvider.reject(new RuntimeException());
        InetSocketAddress address = refreshableAddressProvider.getNextAddress();
        // address should be null as the list has been exhausted
        assertThat(address, is(nullValue()));
        }

    @Test
    public void shouldReinitializeListAfterAllAddressesHaveBeenRead() throws Exception
        {
        InetAddress       host    = InetAddress.getByName("127.0.0.1");
        InetSocketAddress socket1 = new InetSocketAddress(host, 10000);
        InetSocketAddress socket2 = new InetSocketAddress(host, 10001);
        AddressProvider   ap      = mock(AddressProvider.class);
        InetSocketAddress result;

        when(ap.getNextAddress()).thenReturn(socket1, socket2, null);

        RefreshableAddressProvider refreshableAddressProvider = new RefreshableAddressProvider(ap);

        result = refreshableAddressProvider.getNextAddress();
        assertThat(result, is(socket1));

        result = refreshableAddressProvider.getNextAddress();
        assertThat(result, is(socket2));

        result = refreshableAddressProvider.getNextAddress();
        assertThat(result, is(nullValue()));

        result = refreshableAddressProvider.getNextAddress();
        assertThat(result, is(socket1));
        }

    @Test
    public void shouldNotThrowExceptionIfRejectCalledBeforeGetNextAddress() throws Exception
        {
        AddressProvider ap = mock(AddressProvider.class);

        RefreshableAddressProvider refreshableAddressProvider = new RefreshableAddressProvider(ap);

        // reject no longer throws if unadvanced as it can be used concurrently and as such there is no "before"
        refreshableAddressProvider.reject(new RuntimeException());
        }

    }
