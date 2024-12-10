/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.net;


import com.oracle.coherence.common.internal.net.DemultiplexedSocketProvider;
import com.oracle.coherence.common.internal.net.MultiplexedSocketProvider;

import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;


/**
 * TcpSocketProvider produces standard TCP sockets.
 *
 * @author mf 2010.12.27
 */
public class TcpSocketProvider
        extends InetSocketProvider
    {
    @Override
    public ServerSocketChannel openServerSocketChannel()
            throws IOException
        {
        return ServerSocketChannel.open();
        }

    @Override
    public ServerSocket openServerSocket()
            throws IOException
        {
        return new ServerSocket();
        }

    @Override
    public SocketChannel openSocketChannel()
            throws IOException
        {
        return SocketChannel.open();
        }

    @Override
    public Socket openSocket()
            throws IOException
        {
        return new Socket();
        }

    @Override
    public SocketProvider getDelegate()
        {
        return null;
        }


    // ----- constants ------------------------------------------------------

    /**
     * A default TcpSocketProvider instance.
     */
    public static final TcpSocketProvider INSTANCE = new TcpSocketProvider();

    /**
     * A default Multiplexed TcpSocketProvider.
     */
    public static final MultiplexedSocketProvider MULTIPLEXED =
            new MultiplexedSocketProvider(new MultiplexedSocketProvider
                    .DefaultDependencies()
                    .setDelegateProvider(INSTANCE));

    /**
     * A default Demultiplexed TcpSocketProvider.  Bindings from this provider will
     * always be on the base port, but they will not block others from doing multiplexed
     * bindings on that same port.  Additionally since this provider sits on-top of the
     * multiplexed provider it supports NAT bindings.
     */
    public static final DemultiplexedSocketProvider DEMULTIPLEXED =
            new DemultiplexedSocketProvider(TcpSocketProvider.MULTIPLEXED, -1);
    }
