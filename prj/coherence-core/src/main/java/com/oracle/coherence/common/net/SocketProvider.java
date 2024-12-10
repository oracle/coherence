/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.net;


import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;


/**
 * SocketProvider defines an interface for creating sockets.
 *
 * @author mf 2010.12.27
 */
public interface SocketProvider
    {
    /**
     * Resolve the specified address.
     *
     * @param sAddr  the address to resolve
     *
     * @return the resolved address
     *
     * @throws IllegalArgumentException if the address is not resolvable
     */
    public SocketAddress resolveAddress(String sAddr);

    /**
     * Return the string form of of the address to which this socket is connected,
     * suitable for resolving via {@link #resolveAddress}, on a remote host.
     *
     * @param socket  the socket
     *
     * @return the string address, or null if the socket is not connected
     */
    public String getAddressString(Socket socket);

    /**
     * Return the string form of of the server's address, suitable for
     * resolving via {@link #resolveAddress}, on a remote host.
     *
     * @param socket  the socket
     *
     * @return the string address
     */
    public String getAddressString(ServerSocket socket);

    /**
     * Create an ServerSocketChannel.
     *
     * @return the ServerSocketChannel
     *
     * @throws IOException if an I/O error occurs
     */
    public ServerSocketChannel openServerSocketChannel()
            throws IOException;

    /**
     * Create an ServerSocket.
     *
     * @return the ServerSocket
     *
     * @throws IOException if an I/O error occurs
     */
    public ServerSocket openServerSocket()
            throws IOException;

    /**
     * Create a SocketChanel.
     *
     * @return the Socket
     *
     * @throws IOException if an I/O error occurs
     */
    public SocketChannel openSocketChannel()
            throws IOException;

    /**
     * Create a Socket.
     *
     * @return the Socket
     *
     * @throws IOException if an I/O error occurs
     */
    public Socket openSocket()
            throws IOException;

    /**
     * Return the SocketProvider which this provider delegates to, or null if this is a root provider.
     *
     * @return the delegate provider or null
     */
    public SocketProvider getDelegate();
    }
