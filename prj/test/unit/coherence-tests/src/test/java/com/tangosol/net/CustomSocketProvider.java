/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.MulticastSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.DatagramChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;


/**
* SocketProvider implementation for testing custom SocketProvider
* configuration.
*
* @author jh  2010.04.26
*/
public class CustomSocketProvider
    implements SocketProvider
    {
    public CustomSocketProvider(int nParam)
        {
        m_nInitParam = nParam;
        }

    public int getInitParam()
        {
        return m_nInitParam;
        }

    public void setFactory(SocketProviderFactory factory)
        {
        m_factory = factory;
        }

    public SocketProviderFactory getFactory()
        {
        return m_factory;
        }

    @Override
    public DatagramChannel openDatagramChannel()
            throws IOException
        {
        return null;
        }

    @Override
    public DatagramSocket openDatagramSocket()
            throws IOException
        {
        return null;
        }

    @Override
    public MulticastSocket openMulticastSocket()
            throws IOException
        {
        return null;
        }

    @Override
    public ServerSocket openServerSocket()
            throws IOException
        {
        return null;
        }

    @Override
    public ServerSocketChannel openServerSocketChannel()
            throws IOException
        {
        return null;
        }

    @Override
    public Socket openSocket()
            throws IOException
        {
        return null;
        }

    @Override
    public SocketChannel openSocketChannel()
            throws IOException
        {
        return null;
        }
    
    @Override
    public String toString()
        {
        return "CustomSocketProvider";
        }

    private int m_nInitParam;

    private SocketProviderFactory m_factory;
    }
