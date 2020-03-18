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
 * DatagramSocketProivder defines an interface for creating datagram and multicast
 * sockets.
 *
 * @author bb 2011.11.21
 * @since Coherence 12.1.2
 */
public interface DatagramSocketProvider
    {
    /**
     * Return a new DatagramSocket.
     *
     * @return the DatagramSocket
     *
     * @throws IOException  if an I/O related error occurs
     */
     public DatagramSocket openDatagramSocket()
             throws IOException;

     /**
     * Return a new MulticastSocket.
     *
     * @return the MulticastSocket
     *
     * @throws IOException  if an I/O related error occurs
    */
    public MulticastSocket openMulticastSocket()
            throws IOException;

    /**
     * Return true iff the provider returns sockets which are secured, for instance by TLS.
     *
     * @return true iff the provider returns secured sockets
     */
    public boolean isSecure();
    }
