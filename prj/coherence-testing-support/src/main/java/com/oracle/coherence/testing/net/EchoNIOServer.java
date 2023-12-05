/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.testing.net;


import com.oracle.coherence.common.base.Blocking;
import com.oracle.coherence.common.internal.net.DemultiplexedSocketProvider;
import com.oracle.coherence.common.internal.net.MultiplexedSocketProvider;
import com.oracle.coherence.common.net.SocketProvider;

import com.tangosol.net.SocketProviderFactory;

import com.tangosol.util.ClassHelper;

import java.io.EOFException;
import java.io.IOException;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import java.nio.ByteBuffer;

import java.nio.channels.CancelledKeyException;
import java.nio.channels.Channel;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import javax.net.ssl.SSLException;


/**
* Simple server that echos back data sent by a client using NIO.
*
* @author jh  2010.04.27
*/
public class EchoNIOServer
        extends EchoServer
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Create a new EchoNIOServer.
    *
    * @param provider  the SocketProvider used by the new EchoNIOServer to
    *                  create a ServerSocket
    * @param nPort     the port to listen on for client connections
    */
    public EchoNIOServer(SocketProvider provider, int nPort)
        {
        super(provider, nPort);
        }


    // ----- EchoNIOServer methods ------------------------------------------

    /**
    * Add the given selection key to the list of selection keys for open
    * socket channels.
    *
    * @param key  the key to add
    */
    protected void addSelectionKey(SelectionKey key)
        {
        Channel channel = key.channel();
        if (channel instanceof SocketChannel)
            {
            key.attach(new Message());

            SocketChannel socketChannel = (SocketChannel) channel;

            Set<SelectionKey> setKey = m_setKey;
            synchronized (setKey)
                {
                setKey.add(key);
                }
            Set<SocketChannel> setChannel = m_setChannel;
            synchronized (setChannel)
                {
                setChannel.add(socketChannel);
                }
            Set<Socket> setSocket = m_setSocket;
            synchronized (setSocket)
                {
                Socket socket = socketChannel.socket();
                if (setSocket.add(socket))
                    {
                    log(ClassHelper.getSimpleName(getClass()) +
                            " accepted connection from: " +
                            socket.getRemoteSocketAddress());
                    }
                }
            }
        }

    /**
    * Remove the given selection key from the list of selection keys for open
    * socket channels.
    *
    * @param key  the key to remove
    */
    protected void removeSelectionKey(SelectionKey key)
        {
        Channel channel = key.channel();
        if (channel instanceof SocketChannel)
            {
            SocketChannel socketChannel = (SocketChannel) channel;

            key.cancel();
            try
                {
                socketChannel.close();
                }
            catch (IOException e)
                {
                // ignore
                }
            try
                {
                socketChannel.socket().close();
                }
            catch (IOException e)
                {
                // ignore
                }

            Set<SelectionKey> setKey = m_setKey;
            synchronized (setKey)
                {
                setKey.remove(key);
                }
            Set<SocketChannel> setChannel = m_setChannel;
            synchronized (setChannel)
                {
                setChannel.remove(socketChannel);
                }
            Set<Socket> setSocket = m_setSocket;
            synchronized (setSocket)
                {
                Socket socket = socketChannel.socket();
                if (setSocket.remove(socket))
                    {
                    log(ClassHelper.getSimpleName(getClass()) +
                            " dropped connection from: " +
                            socket.getRemoteSocketAddress());
                    }
                }
            }
        }

    /**
    * Called when the selector returns with a non-empty set of selected keys.
    *
    * @param setKey  the set of selected keys
    *
    * @throws IOException on fatal I/O error
    */
    protected void onSelect(Set<SelectionKey> setKey)
            throws IOException
        {
        for (Iterator<SelectionKey> iter = setKey.iterator(); iter.hasNext(); )
            {
            SelectionKey key = iter.next();
            iter.remove();

            // skip invalid keys
            if (!key.isValid())
                {
                continue;
                }

            // compensate for Java NIO bug:
            // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6403933
            if (key.readyOps() == 0)
               {
               err("readyOps==0, closing connection to client");
                try
                    {
                    key.channel().close();
                    }
                catch (IOException e)
                    {
                    e.printStackTrace();
                    }
                continue;
                }

            try
                {
                // handle new Connections
                if (key.isAcceptable())
                    {
                    onAccept(key);
                    }

                // handle reads
                if (key.isReadable())
                    {
                    onRead(key);
                    }

                // handle writes
                if (key.isWritable())
                    {
                    onWrite(key);
                    }
                }
            catch (CancelledKeyException e)
                {
                if (key.channel() instanceof SocketChannel)
                    {
                    removeSelectionKey(key);
                    }
                }
            }
        }

    /**
    * Called when a channel has been selected for accept.
    *
    * @param key  the selected key
    *
    * @throws IOException on fatal I/O error
    */
    protected void onAccept(SelectionKey key) throws IOException
        {
        ServerSocketChannel channelServer = (ServerSocketChannel) key.channel();
        SocketChannel       channel       = channelServer.accept();

        if (channel == null)
            {
            return;
            }

        SelectionKey keyNew;
        try
            {
            channel.configureBlocking(false);
            keyNew = channel.register(m_selector, SelectionKey.OP_READ);
            }
        catch (IOException e)
            {
            log("Error accepting connection: " + printStackTrace(e));
            try
                {
                channel.close();
                }
            catch (IOException ee)
                {
                // ignore
                }
            return;
            }

        addSelectionKey(keyNew);
        }

    /**
    * Called when a channel has been selected for read.
    *
    * @param key  the selected key
    */
    protected void onRead(SelectionKey key)
        {
        Channel channel = key.channel();
        if (channel instanceof SocketChannel)
            {
            SocketChannel socketChannel = (SocketChannel) channel;
            try
                {
                Message message = (Message) key.attachment();

                if (!m_fEcho)
                    {
                    // just read and discard
                    socketChannel.read(ByteBuffer.allocate(1024));
                    return;
                    }

                // make sure we've read the length of the body
                ByteBuffer bufLength = message.m_bufLength;
                if (bufLength.hasRemaining())
                    {
                    if (socketChannel.read(bufLength) == -1)
                        {
                        throw new EOFException();
                        }
                    if (bufLength.hasRemaining())
                        {
                        return;
                        }
                    }

                // make sure we've read the message body
                ByteBuffer bufBody = message.m_bufBody;
                if (bufBody == null)
                    {
                    // check message type
                    bufLength.flip();
                    if(bufLength.getInt() != EchoNIOClient.MAGIC)
                        {
                        throw new IOException();
                        }
                    message.m_bufBody = bufBody = ByteBuffer.allocateDirect(bufLength.getInt());
                    }
                if (bufBody.hasRemaining())
                    {
                    if (socketChannel.read(bufBody) == -1)
                        {
                        throw new EOFException();
                        }
                    if (bufBody.hasRemaining())
                        {
                        return;
                        }
                    }

                bufLength.flip();
                bufBody.flip();
                key.interestOps(key.interestOps() & ~SelectionKey.OP_READ);
                key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
                }
            catch (SSLException e)
                {
                removeSelectionKey(key);
                }
            catch (Exception e)
                {
                removeSelectionKey(key);
                }
            }
        }

    /**
    * Called when a channel has been selected for write.
    *
    * @param key  the selected key
    */
    protected void onWrite(SelectionKey key)
        {
        Channel channel = key.channel();
        if (channel instanceof SocketChannel)
            {
            SocketChannel socketChannel = (SocketChannel) channel;
            try
                {
                Message message = (Message) key.attachment();

                // make sure we've written the length of the body
                ByteBuffer bufLength = message.m_bufLength;
                if (bufLength.hasRemaining())
                    {
                    socketChannel.write(bufLength);
                    if (bufLength.hasRemaining())
                        {
                        return;
                        }
                    }

                // make sure we've written the message body
                ByteBuffer bufBody = message.m_bufBody;
                if (bufBody.hasRemaining())
                    {
                    socketChannel.write(bufBody);
                    if (bufBody.hasRemaining())
                        {
                        return;
                        }
                    }

                bufLength.clear();
                message.m_bufBody = null;
                key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
                key.interestOps(key.interestOps() | SelectionKey.OP_READ);
                }
            catch (SSLException e)
                {
                removeSelectionKey(key);
                }
            catch (Exception e)
                {
                removeSelectionKey(key);
                }
            }
        }


    // ----- EchoServer methods ---------------------------------------------

    /**
    * {@inheritDoc}
    */
    public int getConnectionCount()
        {
        Set<SelectionKey> set = m_setKey;
        synchronized (set)
            {
            return set.size();
            }
        }

    /**
    * Release any resources created by the EchoNIOServer.
    */
    protected synchronized void cleanup()
        {
        SelectionKey key = m_key;
        if (key != null)
            {
            key.cancel();
            m_key = null;
            }

        Channel channel = m_channel;
        if (channel != null)
            {
            try
                {
                m_channel.close();
                }
            catch (IOException e)
                {
                // ignore
                }
            m_channel = null;
            }

        Set<SelectionKey> setKey = m_setKey;
        synchronized (setKey)
            {
            for (Iterator<SelectionKey> iter = setKey.iterator(); iter.hasNext();)
                {
                SelectionKey keyClient = iter.next();
                iter.remove();
                keyClient.cancel();
                }
            }

        Set<SocketChannel> setChannel = m_setChannel;
        synchronized (setChannel)
            {
            for (Iterator<SocketChannel> iter = setChannel.iterator(); iter.hasNext(); )
                {
                SocketChannel channelClient = iter.next();
                iter.remove();
                try
                    {
                    channelClient.close();
                    }
                catch (IOException e)
                    {
                    // ignore
                    }
                }
            }

        Selector selector = m_selector;
        if (selector != null)
            {
            try
                {
                m_selector.close();
                }
            catch (IOException e)
                {
                // ignore
                }
            m_selector = null;
            }

        super.cleanup();
        }


    // ----- Daemon methods -------------------------------------------------

    /**
    * {@inheritDoc}
    */
    public void run()
        {
        Selector selector = m_selector;
        try
            {
            while (isRunning())
                {
                selector.select();
                onSelect(selector.selectedKeys());
                }
            }
        catch (IOException e)
            {
            if (!isStopping())
                {
                log(ClassHelper.getSimpleName(getClass()) +
                        " exiting due to unhandled exception: " +
                        printStackTrace(e));
                }
            }
        catch (ClosedSelectorException e)
            {
            if (!isStopping())
                {
                log(ClassHelper.getSimpleName(getClass()) +
                        " exiting due to unhandled exception: " +
                        printStackTrace(e));
                }
            }
        catch (Exception e)
            {
            log(ClassHelper.getSimpleName(getClass()) +
                    " exiting due to unhandled exception: " +
                    printStackTrace(e));
            }
        finally
            {
            cleanup();
            }
        }

    /**
    * {@inheritDoc}
    */
    public synchronized void start()
        {
        if (m_channel == null)
            {
            try
                {
                ServerSocketChannel channel  = m_provider.openServerSocketChannel();
                ServerSocket        socket   = channel.socket();
                Selector            selector = channel.provider().openSelector();

                channel.configureBlocking(false);
                socket.setReuseAddress(true);
                socket.bind(new InetSocketAddress("127.0.0.1", m_nPort));

                m_key = channel.register(selector, SelectionKey.OP_ACCEPT);

                log(ClassHelper.getSimpleName(getClass()) +
                        " listening for connections on " +
                        socket.getInetAddress() + ":" +
                        socket.getLocalPort());

                m_channel  = channel;
                m_socket   = socket;
                m_selector = selector;
                }
            catch (IOException e)
                {
                cleanup();
                throw ensureRuntimeException(e);
                }
            }

        super.start();
        }

    /**
    * {@inheritDoc}
    */
    public synchronized void stop()
        {
        super.stop();
        cleanup();
        }

    /**
     * Returns the server's SocketChannels.
     *
     * @return the socket channels
     */
    public Set<SocketChannel> getChannels()
        {
        return m_setChannel;
        }


    // ----- application entry point ----------------------------------------

    /**
    * Usage: java com.oracle.coherence.testing.net.EchoNIOServer [port]
    *
    * @param asArg  command line arguments
    */
    public static void main(String[] asArg)
        {
        if (asArg.length != 1)
            {
            log("Usage: java com.oracle.coherence.testing.net.EchoNIOServer [port]");
            return;
            }

        int nPort;
        try
            {
            nPort = Integer.valueOf(asArg[0]);
            }
        catch (NumberFormatException e)
            {
            log("Usage: java com.oracle.coherence.testing.net.EchoNIOServer [port]");
            return;
            }

        EchoServer server = new EchoNIOServer(
            new DemultiplexedSocketProvider(
                (MultiplexedSocketProvider)SocketProviderFactory.DEFAULT_SOCKET_PROVIDER, 1), nPort);
        server.start();
        while (server.isRunning())
            {
            synchronized (server)
                {
                try
                    {
                    Blocking.wait(server);
                    }
                catch (InterruptedException e)
                    {
                    server.stop();
                    }
                }
            }
        }


    // ----- inner class: Message -------------------------------------------

    /**
    * Class that contains a message sent from a client.
    */
    public static class Message
        {
        /**
        * ByteBuffer that stores the length of the body in bytes.
        */
        public final ByteBuffer m_bufLength = ByteBuffer.allocate(8);

        /**
        * The message body.
        */
        public ByteBuffer m_bufBody;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The set of SocketChannels for open Socket connections.
    */
    protected final Set<SocketChannel> m_setChannel = new HashSet<SocketChannel>();

    /**
    * The set of SelectionKeys for open Socket connections.
    */
    protected final Set<SelectionKey> m_setKey = new HashSet<SelectionKey>();

    /**
    * The ServerSocketChannel used by the EchoNIOServer.
    */
    protected ServerSocketChannel m_channel;

    /**
    * The Selector used to select from the various SelectableChannel objects
    * created by this EchoNIOServer.
    */
    protected Selector m_selector;

    /**
    * The SelectionKey that represents the registration of the
    * ServerSocketChannel with the Selector used by this EchoNIOServer.
    */
    protected SelectionKey m_key;
    }
