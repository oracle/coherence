/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.net;


import java.net.SocketAddress;
import java.net.InetSocketAddress;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.ServerSocket;
import java.net.Socket;


/**
 * InetSocketProvider is a SocketProvider which utilizes InetSocketAddresses.
 *
 * @author mf  2011.01.11
 */
public abstract class InetSocketProvider
        implements SocketProvider
    {
    /**
     * {@inheritDoc}
     */
    @Override
    public SocketAddress resolveAddress(String sAddr)
        {
        String sHost;
        int    nPort;

        if (sAddr.startsWith("["))
            {
            // ipv6 formatted: [addr]:port
            int ofPort = sAddr.lastIndexOf("]:");
            if (ofPort == 2)
                {
                throw new IllegalArgumentException("address does not contain an hostname or ip");
                }
            else if (ofPort == -1)
                {
                throw new IllegalArgumentException("address does not contain a port");
                }

            sHost = sAddr.substring(1, ofPort - 1);
            nPort = Integer.parseInt(sAddr.substring(ofPort + 2));
            }
        else
            {
            // ipv4 formatted: addr:port
            int ofPort = sAddr.lastIndexOf(':');
            if (ofPort == 0)
                {
                throw new IllegalArgumentException("address does not contain an hostname of ip");
                }
            else if (ofPort == -1)
                {
                throw new IllegalArgumentException("address does not contain a port");
                }

            sHost = sAddr.substring(0, ofPort);
            nPort = Integer.parseInt(sAddr.substring(ofPort + 1));
            }

        return new InetSocketAddress(sHost, nPort);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getAddressString(Socket socket)
        {
        InetAddress addr  = socket.getInetAddress();
        if (addr == null)
            {
            return null;
            }

        String sAddr = addr.getHostAddress();
        if (sAddr.contains(":"))
            {
            // ipv6 representation
            sAddr = "[" + sAddr + "]";
            }

        return sAddr + ":" + socket.getPort();
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getAddressString(ServerSocket socket)
        {
        InetAddress addr = socket.getInetAddress();
        boolean     fAny = addr.isAnyLocalAddress();
        String      sAddr;
        if (fAny)
            {
            // replace wildcard address with local hostname as this
            try
                {
                addr = InetAddress.getLocalHost();
                }
            catch (UnknownHostException e) {}

            sAddr = addr.getCanonicalHostName();
            }
        else
            {
            // use host ip address
            sAddr = addr.getHostAddress();
            }

        if (sAddr.contains(":"))
            {
            // ipv6 representation
            sAddr = "[" + sAddr + "]";
            }

        return sAddr + ":" + socket.getLocalPort();
        }
    }
