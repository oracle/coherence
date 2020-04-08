/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net;


import java.io.IOException;

import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.MulticastSocket;

import java.nio.channels.DatagramChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;


/**
* SocketProvider defines an abstraction for creating various types of sockets.
*
* @author mf, jh  2010.04.21
* @since Coherence 3.6
*/
public interface SocketProvider
    {
    /**
    * Return a new Socket.
    *
    * @return the Socket
    *
    * @throws IOException  if an I/O related error occurs
    */
    public Socket openSocket()
            throws IOException;

    /**
    * Return a new SocketChannel.
    *
    * @return the connected SocketChannel
    *
    * @throws IOException if an I/O related error occurs
    */
    public SocketChannel openSocketChannel()
            throws IOException;

    /**
    * Return a new ServerSocket.
    *
    * @return the ServerSocket
    *
    * @throws IOException  if an I/O related error occurs
    */
    public ServerSocket openServerSocket()
            throws IOException;

    /**
    * Return a new ServerSocketChannel.
    *
    * @return the ServerSocketChannel
    *
    * @throws IOException  if an I/O related error occurs
    */
    public ServerSocketChannel openServerSocketChannel()
            throws IOException;

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
    * Return a new DatagramChannel.
    *
    * @return the DatagramChannel
    *
    * @throws IOException  if an I/O related error occurs
    */
    public DatagramChannel openDatagramChannel()
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
    * Specify the SocketProviderFactory associated with this provider.
    *
    * @param factory  the associated factory
    */
    public void setFactory(SocketProviderFactory factory);

    /**
    * Return the factory associated with this provider.
    *
    * @return the associated factory
    */
    public SocketProviderFactory getFactory();
    }
