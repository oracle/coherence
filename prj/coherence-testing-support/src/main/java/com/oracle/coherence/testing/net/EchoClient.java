/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.testing.net;


import com.oracle.coherence.common.internal.net.DemultiplexedSocketProvider;
import com.oracle.coherence.common.internal.net.MultiplexedSocketProvider;
import com.oracle.coherence.common.net.SocketProvider;
import com.tangosol.net.SocketProviderFactory;

import com.tangosol.util.Base;
import com.tangosol.util.ClassHelper;
import com.tangosol.util.ExternalizableHelper;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import java.net.InetSocketAddress;
import java.net.Socket;


/**
* Simple client sends data to a server and reads the server's response.
*
* @author jh  2010.04.27
*
* @see EchoServer
*/
public class EchoClient
        extends Base
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Create a new EchoClient.
    *
    * @param provider  the SocketProvider used by the new EchoClient to
    *                  create a Socket
    * @param nPort     the port to connect to
    */
    public EchoClient(SocketProvider provider, int nPort)
        {
        if (provider == null || nPort <= 0)
            {
            throw new IllegalArgumentException();
            }
        m_provider = provider;
        m_nPort    = nPort;
        }


    // ----- EchoClient methods ---------------------------------------------

    /**
    * Explicitly connect to the server, if the client is not already
    * connected.
    *
    * @throws IOException on connection error
    */
    public  void connect()
            throws IOException
        {
        connect(0);
        }

    public synchronized void connect(int cMillisConnect)
            throws IOException
        {
        if (m_socket == null)
            {
            Socket socket = m_provider.openSocket();
            try
                {
                socket.setTcpNoDelay(true);
                socket.connect(new InetSocketAddress("127.0.0.1", m_nPort), cMillisConnect);
                }
            catch (IOException e)
                {
                try
                    {
                    socket.close();
                    }
                catch (IOException ee)
                    {
                    // ignore
                    }
                throw e;
                }

            m_socket = socket;
            log(ClassHelper.getSimpleName(getClass()) + " connected to " +
                    socket.getInetAddress() + ":" + socket.getPort());
            }
        }

    /**
    * Explicitly disconnect from the server, if the client is currently
    * connected.
    */
    public synchronized void disconnect()
        {
        Socket socket = m_socket;
        if (socket != null)
            {
            try
                {
                socket.close();
                }
            catch (IOException e)
                {
                // ignore
                }
            m_socket = null;
            log(ClassHelper.getSimpleName(getClass()) + " disconnected.");
            }
        }

    /**
    * Send the supplied message to the server and return the server's
    * response.
    *
    * @param sMsg  the message to send
    *
    * @return the server's response
    *
    * @throws IOException on I/O error
    */
    public synchronized String echo(String sMsg)
            throws IOException
        {
        connect();

        Socket           socket = m_socket;
        DataInputStream  in     = new DataInputStream(socket.getInputStream());
        DataOutputStream out    = new DataOutputStream(socket.getOutputStream());
        byte[]           ab     = ExternalizableHelper.toByteArray(sMsg);
        int              cb     = ab.length;

        out.writeInt(EchoNIOClient.MAGIC);
        out.writeInt(cb);
        out.write(ab);
        if (in.readInt() != EchoNIOClient.MAGIC)
            {
            throw new IOException();
            }
        ab = new byte[in.readInt()];
        in.readFully(ab);

        return (String) ExternalizableHelper.fromByteArray(ab);
        }


    // ----- application entry point ----------------------------------------

    /**
    * Usage: java com.oracle.coherence.testing.net.EchoClient [port] [message]
    *
    * @param asArg  command line arguments
    *
    * @throws IOException on I/O error
    */
    public static void main(String[] asArg)
            throws IOException
        {
        if (asArg.length != 2)
            {
            log("Usage: java com.oracle.coherence.testing.net.EchoClient [port] [message]");
            return;
            }

        int nPort;
        try
            {
            nPort = Integer.valueOf(asArg[0]);
            }
        catch (NumberFormatException e)
            {
            log("Usage: java com.oracle.coherence.testing.net.EchoClient [port] [message]");
            return;
            }

        String sMsg = asArg[1];

        EchoClient client = new EchoClient(
            new DemultiplexedSocketProvider(
                (MultiplexedSocketProvider)SocketProviderFactory.DEFAULT_SOCKET_PROVIDER, 1), nPort);
        try
            {
            log(client.echo(sMsg));
            }
        finally
            {
            client.disconnect();
            }
        }


    // ----- data members ---------------------------------------------------

    /**
    * The SocketProvider used by the Echo to create a Socket.
    */
    protected final SocketProvider m_provider;

    /**
    * The port to connect to.
    */
    protected final int m_nPort;

    /**
    * The client Socket.
    */
    protected Socket m_socket;
    }
