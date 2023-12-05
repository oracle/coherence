/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.testing.net;


import com.oracle.coherence.common.internal.net.DemultiplexedSocketProvider;
import com.oracle.coherence.common.internal.net.MultiplexedSocketProvider;
import com.oracle.coherence.common.net.SocketProvider;

import com.tangosol.net.SocketProviderFactory;

import com.tangosol.util.ClassHelper;
import com.tangosol.util.ExternalizableHelper;

import java.io.EOFException;
import java.io.IOException;

import java.net.InetSocketAddress;
import java.net.Socket;

import java.nio.ByteBuffer;

import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

import java.util.Iterator;


/**
* Simple client sends data to a server and reads the server's response using
* NIO.
*
* @author jh  2010.05.04
*
* @see EchoNIOServer
*/
public class EchoNIOClient
        extends EchoClient
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Create a new EchoNIOClient.
    *
    * @param provider  the SocketProvider used by the new EchoNIOClient to
    *                  create a Socket
    * @param nPort     the port to connect to
    */
    public EchoNIOClient(SocketProvider provider, int nPort)
        {
        super(provider, nPort);
        }


    // ----- EchoClient methods ---------------------------------------------

    /**
    * {@inheritDoc}
    */
    public synchronized void connect()
            throws IOException
        {
        if (m_channel == null)
            {
            SocketChannel channel  = null;
            Selector      selector = null;
            SelectionKey  key      = null;
            Socket        socket   = null;
            try
                {
                channel  = m_provider.openSocketChannel();
                selector = channel.provider().openSelector();
                socket   = channel.socket();

                channel.configureBlocking(false);
                socket.setTcpNoDelay(true);

                key = channel.register(selector, SelectionKey.OP_CONNECT);
                if (!channel.connect(new InetSocketAddress("127.0.0.1", m_nPort)))
                    {
                    for (boolean fConnecting = true; fConnecting; )
                        {
                        selector.select();
                        for (Iterator<SelectionKey> iter = selector.selectedKeys().iterator();
                             iter.hasNext(); )
                            {
                            SelectionKey keySelected = iter.next();
                            iter.remove();

                            if (keySelected.isValid() && keySelected.isConnectable())
                                {
                                channel.finishConnect();
                                fConnecting = false;
                                }
                            }
                        }
                    }

                key.interestOps(key.interestOps() & ~SelectionKey.OP_CONNECT);
                }
            catch (IOException e)
                {
                if (key != null)
                    {
                    key.cancel();
                    }
                if (channel != null)
                    {
                    try
                        {
                        channel.close();
                        }
                    catch (IOException ee)
                        {
                        // ignore
                        }
                    }
                if (selector != null)
                    {
                    try
                        {
                        selector.close();
                        }
                    catch (IOException ee)
                        {
                        // ignore
                        }
                    }
                if (socket != null)
                    {
                    try
                        {
                        socket.close();
                        }
                    catch (IOException ee)
                        {
                        // ignore
                        }
                    }
                throw e;
                }

            m_channel  = channel;
            m_selector = selector;
            m_key      = key;
            m_socket   = socket;

            log(ClassHelper.getSimpleName(getClass()) + " connected to " +
                    socket.getInetAddress() + ":" + socket.getPort());
            }
        }

    /**
    * {@inheritDoc}
    */
    public synchronized String echo(String sMsg)
            throws IOException
        {
        connect();

        Selector      selector  = m_selector;
        SelectionKey  key       = m_key;
        SocketChannel channel   = m_channel;
        byte[]        ab        = ExternalizableHelper.toByteArray(sMsg);
        int           cb        = ab.length;
        ByteBuffer    bufLength = ByteBuffer.allocate(8);
        ByteBuffer    bufBody   = ByteBuffer.wrap(ab);

        bufLength.putInt(MAGIC).putInt(cb);
        bufLength.flip();

        // write the message
        key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
        for (boolean fWriting = true; fWriting; )
            {
            selector.select();
            for (Iterator<SelectionKey> iter = selector.selectedKeys().iterator();
                 iter.hasNext(); )
                {
                SelectionKey keySelected = iter.next();
                iter.remove();

                if (keySelected.isValid() && keySelected.isWritable())
                    {
                    // make sure we've written the length of the body
                    if (bufLength.hasRemaining())
                        {
                        channel.write(bufLength);
                        if (bufLength.hasRemaining())
                            {
                            continue;
                            }
                        }

                    // make sure we've written the message body
                    if (bufBody.hasRemaining())
                        {
                        channel.write(bufBody);
                        if (bufBody.hasRemaining())
                            {
                            continue;
                            }
                        }
                    fWriting = false;
                    }
                }
            }

        // read the response
        bufLength.clear();
        bufBody = null;
        key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
        key.interestOps(key.interestOps() | SelectionKey.OP_READ);
        for (boolean fReading = true; fReading; )
            {
            selector.select();
            for (Iterator<SelectionKey> iter = selector.selectedKeys().iterator();
                 iter.hasNext(); )
                {
                SelectionKey keySelected = iter.next();
                iter.remove();

                if (!keySelected.isValid())
                    {
                    continue;
                    }

                if (keySelected.isReadable())
                    {
                    // make sure we've read the length of the body
                    if (bufLength.hasRemaining())
                        {
                        if (channel.read(bufLength) == -1)
                            {
                            throw new EOFException();
                            }
                        if (bufLength.hasRemaining())
                            {
                            continue;
                            }

                        bufLength.flip();
                        if (bufLength.getInt() != EchoNIOClient.MAGIC)
                            {
                            throw new IOException();
                            }
                        bufBody = ByteBuffer.wrap(new byte[bufLength.getInt()]);
                        }

                    // make sure we've read the message body
                    if (bufBody.hasRemaining())
                        {
                        if (channel.read(bufBody) == -1)
                            {
                            throw new EOFException();
                            }
                        if (bufBody.hasRemaining())
                            {
                            continue;
                            }
                        }
                    fReading = false;
                    }
                else if (key.readyOps() == 0)
                    {
                    err("readyOps==0, closing connection to server");
                    // compensate for Java NIO bug:
                    // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6403933
                    try
                        {
                        fReading = false;
                        key.channel().close();
                        }
                    catch (IOException e)
                        {
                        e.printStackTrace();
                        }
                    }
                }
            }

        return (String) ExternalizableHelper.fromByteArray(bufBody.array());
        }

    /**
    * {@inheritDoc}
    */
    public synchronized void disconnect()
        {
        SelectionKey key = m_key;
        if (key != null)
            {
            key.cancel();
            m_key = null;
            }

        SocketChannel channel = m_channel;
        if (channel != null)
            {
            try
                {
                channel.close();
                }
            catch (IOException ee)
                {
                // ignore
                }
            m_channel = null;
            }

        Selector selector = m_selector;
        if (selector != null)
            {
            try
                {
                selector.close();
                }
            catch (IOException ee)
                {
                // ignore
                }
            m_selector = null;
            }

        super.disconnect();
        }

    /**
    * Returns the client's SocketChannel.
    *
    * @return the socket channel
    */
    public SocketChannel getChannel()
        {
        return m_channel;
        }

    // ----- application entry point ----------------------------------------

    /**
    * Usage: java com.oracle.coherence.testing.net.EchoNIOClient [port] [message]
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
            log("Usage: java com.oracle.coherence.testing.net.EchoNIOClient [port] [message]");
            return;
            }

        int nPort;
        try
            {
            nPort = Integer.valueOf(asArg[0]);
            }
        catch (NumberFormatException e)
            {
            log("Usage: java com.oracle.coherence.testing.net.EchoNIOClient [port] [message]");
            return;
            }

        String sMsg = asArg[1];

        EchoClient client = new EchoNIOClient(
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
     * The ServerSocketChannel used by the EchoNIOClient.
     */
    protected SocketChannel m_channel;

    /**
     * The Selector used to by this EchoNIOClient.
     */
    protected Selector m_selector;

    /**
     * The SelectionKey that represents the registration of the
     * SocketChannel with the Selector used by this EchoNIOClient.
     */
    protected SelectionKey m_key;

    /**
     * The magic message type that uniquely identify the message from ssl Tests
     */
    public static final int MAGIC = 1234;
    }
