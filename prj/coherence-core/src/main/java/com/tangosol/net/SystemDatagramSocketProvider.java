/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net;

import java.io.IOException;

import java.net.DatagramSocket;
import java.net.MulticastSocket;

/**
 * SystemDatagramSocketProvider produces JVM default datagram sockets
 *
 * @author bb 2011.11.21
 * @since Coherence 12.1.2
 */
public class SystemDatagramSocketProvider
        implements DatagramSocketProvider
    {
    @Override
    public DatagramSocket openDatagramSocket()
            throws IOException
        {
        return new DatagramSocket(null);
        }

    @Override
    public MulticastSocket openMulticastSocket()
            throws IOException
        {
        return new MulticastSocket(null);
        }

    @Override
    public boolean isSecure()
        {
        return false;
        }

    @Override
    public String toString()
        {
        return "SystemDatagramSocketProvider";
        }

    // ----- constants ------------------------------------------------------

    /**
     * A default SocketProvider instance.
     */
    public static final SystemDatagramSocketProvider INSTANCE = new SystemDatagramSocketProvider();
    }
