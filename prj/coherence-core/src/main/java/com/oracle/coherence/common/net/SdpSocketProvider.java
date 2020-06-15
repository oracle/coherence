/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.net;


import com.oracle.coherence.common.internal.net.MultiplexedSocketProvider;

import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.lang.reflect.Method;


/**
 * SdpSocketProvider produces SDP based sockets.
 * <p>
 * This provider is only functional in environments which support SDP.
 *
 * @author mf 2010.12.27
 */
public class SdpSocketProvider
        extends InetSocketProvider
    {
    @Override
    public ServerSocketChannel openServerSocketChannel()
            throws IOException
        {
        try
            {
            return (ServerSocketChannel) METHOD_OPEN_SERVER_CHANNEL.invoke(null);
            }
        catch (Exception e)
            {
            verifySDP();
            throw ensureIOException(e);
            }
        }

    @Override
    public ServerSocket openServerSocket()
            throws IOException
        {
        try
            {
            return (ServerSocket) METHOD_OPEN_SERVER.invoke(null);
            }
        catch (Exception e)
            {
            verifySDP();
            throw ensureIOException(e);
            }
        }

    @Override
    public SocketChannel openSocketChannel()
            throws IOException
        {
        try
            {
            return (SocketChannel) METHOD_OPEN_CLIENT_CHANNEL.invoke(null);
            }
        catch (Exception e)
            {
            verifySDP();
            throw ensureIOException(e);
            }
        }

    @Override
    public Socket openSocket()
            throws IOException
        {
        try
            {
            return (Socket) METHOD_OPEN_CLIENT.invoke(null);
            }
        catch (Exception e)
            {
            verifySDP();
            throw ensureIOException(e);
            }
        }

    @Override
    public SocketAddress resolveAddress(String sAddr)
        {
        try
            {
            verifySDP();
            }
        catch (IOException e)
            {
            throw new IllegalArgumentException(e.getMessage());
            }
        return super.resolveAddress(sAddr);
        }

    @Override
    public SocketProvider getDelegate()
        {
        return null;
        }


    // ----- helpers --------------------------------------------------------

    /**
     * Verify that an SDP socket implementation is available.
     *
     * @throws IOException if no implementation is available
     */
    public static void verifySDP()
            throws IOException
        {
        if (METHOD_OPEN_SERVER_CHANNEL == null)
            {
            throw new IOException("SDP classes are unavailable");
            }
        else if (!Boolean.getBoolean("java.net.preferIPv4Stack"))
            {
            throw new IOException("SDP requires the java.net.preferIPv4Stack" +
                    " system property be set to true");
            }
        }

    /**
     * Return an IOException for the specified Exception
     *
     * @param e  the exception to ensure
     *
     * @return the associated IOException
     */
    public static IOException ensureIOException(Exception e)
        {
        if (e instanceof IOException)
            {
            return (IOException) e;
            }
        else if (e.getCause() instanceof IOException)
            {
            return (IOException) e.getCause();
            }
        else
            {
            return new IOException(e);
            }
        }


    // ----- constants ------------------------------------------------------

    /**
     * A default SdpSocketProvider instance.
     */
    public static final SdpSocketProvider INSTANCE = new SdpSocketProvider();

    /**
     * A default Multiplexed SdpSocketProvider.
     */
    public static final MultiplexedSocketProvider MULTIPLEXED =
            new MultiplexedSocketProvider(new MultiplexedSocketProvider
                    .DefaultDependencies()
                    .setDelegateProvider(INSTANCE))
            {
            @Override
            public SocketAddress resolveAddress(String sAddr)
                {
                try
                    {
                    verifySDP();
                    }
                catch (IOException e)
                    {
                    throw new IllegalArgumentException(e.getMessage());
                    }
                return super.resolveAddress(sAddr);
                }
            };

    /**
     * The method for openeing SDP ServerSocketChannels.
     */
    protected static final Method METHOD_OPEN_SERVER_CHANNEL;

    /**
     * The method for openeing SDP ServerSocket.
     */
    protected static final Method METHOD_OPEN_SERVER;

    /**
     * The method for openeing SDP SocketChannels.
     */
    protected static final Method METHOD_OPEN_CLIENT_CHANNEL;

    /**
     * The method for openeing SDP Socket.
     */
    protected static final Method METHOD_OPEN_CLIENT;

    static
        {
        Method metServerChan = null;
        Method metServer     = null;
        Method metClientChan = null;
        Method metClient     = null;
        try
            {
            Class clz = Class.forName("com.oracle.net.Sdp");
            metServerChan = clz.getMethod("openServerSocketChannel");
            metServer     = clz.getMethod("openServerSocket");
            metClientChan = clz.getMethod("openSocketChannel");
            metClient     = clz.getMethod("openSocket");
            }
        catch (Throwable t) {}
        finally
            {
            METHOD_OPEN_SERVER_CHANNEL = metServerChan;
            METHOD_OPEN_SERVER         = metServer;
            METHOD_OPEN_CLIENT_CHANNEL = metClientChan;
            METHOD_OPEN_CLIENT         = metClient;
            }
        }
    }
