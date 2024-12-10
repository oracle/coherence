/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net;

import com.oracle.coherence.common.net.SocketProvider;

import com.tangosol.coherence.config.Config;

import java.io.IOException;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;

import java.nio.ByteBuffer;

import java.nio.channels.SocketChannel;

import java.util.concurrent.ConcurrentMap;


/**
* TCP based non-blocking datagram socket implementation.
*
* In order to provide a non-blocking API this implementation may drop packets
* if the underlying TCP transfer buffers are full.
*
* @author mf  2009.12.16
*/
public class NonBlockingTcpDatagramSocket
    extends TcpDatagramSocket
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Create a new NonBlockingTcpDatagramSocket that with a wildcard address
    * bound to an ephemeral port.
    *
    * @throws SocketException if any error happens during the bind, or if the
    *         port is unavailable
    */
    public NonBlockingTcpDatagramSocket()
        throws SocketException
        {
        this(new InetSocketAddress(0));
        }

    /**
    * Creates a new NonBlockingTcpDatagramSocket which will be bound to the
    * specified {@link SocketAddress address}.
    *
    * @param addr  the {@link SocketAddress address} to bind
    *
    * @throws SocketException if any error happens during the bind,
    *         or if the port is unavailable
    */
    public NonBlockingTcpDatagramSocket(SocketAddress addr)
        throws SocketException
        {
        this(new Impl());
        if (addr != null)
            {
            bind(addr);
            }
        }

    /**
    * Creates a new NonBlockingTcpDatagramSocket using the wildcard address
    * and the specified port number.
    * <p>
    * The port number should be between 0 and 65535. Zero means that the system
    * will pick an ephemeral port during the bind operation.
    *
    * @param nPort  the port to bind to
    *
    * @throws SocketException if any error happens during the bind,
    *         or if the port is unavailable
    */
    public NonBlockingTcpDatagramSocket(int nPort)
        throws SocketException
        {
        this(nPort, null);
        }

    /**
    * Creates a new NonBlockingTcpDatagramSocket using an {@link InetAddress
    * address} and a port number.
    * <p>
    * If <code>null</code> is specified as the address assigned will be the
    * wildcard address.
    * <p>
    * The port number should be between 0 and 65535. Zero means that the system
    * will pick an ephemeral port during the bind operation.
    *
    * @param nPort  the port number
    * @param addr   the IP address
    *
    * @throws SocketException if any error happens during the bind,
    *         or if the port is unavailable
    */
    public NonBlockingTcpDatagramSocket(int nPort, InetAddress addr)
        throws SocketException
        {
        this(new InetSocketAddress(addr, nPort));
        }

    /**
    * Creates a new NonBlockingTcpDatagramSocket using the
    * {@link SocketProvider provider}.
    *
    * @param provider  the {@link SocketProvider provider} to be used
    *
    * @throws SocketException if any error happens during the bind,
    *         or if the port is unavailable
    */
    public NonBlockingTcpDatagramSocket(SocketProvider provider)
        throws SocketException
        {
        this(new Impl(provider));
        }

    /**
    * Creates a new NonBlockingTcpDatagramSocket around an
    * {@link NonBlockingTcpDatagramSocket.Impl}.
    *
    * @param impl  a {@link NonBlockingTcpDatagramSocket.Impl}
    */
    protected NonBlockingTcpDatagramSocket(Impl impl)
        {
        super(impl);
        }

    // ----- inner class: Impl ----------------------------------------------

    /**
    * A specialization of {@link TcpDatagramSocket.Impl} which provides
    * non-blocking functionality, see {@link #send(DatagramPacket)}.
    */
    public static class Impl
        extends TcpDatagramSocket.Impl
        {
        // ----- constructors -------------------------------------------

        /**
        * Creates a new Impl.
        *
        * @throws SocketException if any error happens during the bind, or if
        *                         the port is unavailable
        */
        public Impl()
                throws SocketException
            {
            }

        /**
        * Creates a new Impl using a {@link SocketProvider provider}.
        *
        * @param provider  the {@link SocketProvider provider} used to create
        *                  internal sockets
        *
        * @throws SocketException if any error happens during the bind, or if
        *                         the port is unavailable
        */
        public Impl(SocketProvider provider)
                throws SocketException
            {
            super(provider);
            }


        // ----- Impl methods -------------------------------------------

        /**
        * {@inheritDoc}
        */
        protected boolean onConnectionHeader(ConnectionStatus status, SocketChannel chan)
                throws IOException
            {
            boolean fEOS = super.onConnectionHeader(status, chan);

            if (!SPLIT && status.m_state == ConnectionStatus.WAIT_HEAD)
                {
                // we've processed the header which means we have the address
                // and port
                ConcurrentMap<SocketAddress, Connection> map  = m_mapConnectionsOut;
                Connection                               conn = map.get(status.m_addr);
                if (conn == null)
                    {
                    conn = makeConnection(chan);
                    if (map.putIfAbsent(status.m_addr, conn) == null)
                        {
                        status.m_connection = conn;
                        }
                    }
                }

            return fEOS;
            }

        /**
        * Make a connection from an existing channel.
        *
        * @param chan  the channel
        *
        * @return the connection
        * @throws IOException if an I/O error occurs
        */
        protected Connection makeConnection(SocketChannel chan)
                throws IOException
            {
            // start the connection by sending magic and our acceptor port
            // this allows the receiving side to re-build the
            // UDP like source address
            int nPort = f_socket.socket().getLocalPort();
            ByteBuffer buff = ByteBuffer.allocate(16);
            buff.putInt(PROTOCOL_MAGIC);
            buff.putInt(m_nPacketMagic);
            buff.putInt(m_nPacketMagicMask);
            buff.putInt(nPort);
            buff.flip();

            return new NonBlockingConnection(chan, buff);
            }

        /**
        * {@inheritDoc}
        */
        protected Connection makeConnection(SocketAddress addr)
                throws IOException
            {
            NonBlockingConnection conn = (NonBlockingConnection)
                    makeConnection(m_provider.openSocketChannel());

            configureSocket(conn.m_socket);
            conn.m_channel.configureBlocking(false);
            if (conn.m_channel.connect(addr))
                {
                ConnectionStatus status = new ConnectionStatus();
                status.m_connection = conn;
                scheduleRegistration(conn.m_channel, status);
                }

            return conn;
            }

        /**
        * {@inheritDoc}
        */
        protected void send(DatagramPacket packet)
                throws IOException
            {
            NonBlockingConnection conn;
            SocketChannel         chan;
            SocketAddress         addr = packet.getSocketAddress();

            try
                {
                conn = (NonBlockingConnection) ensureConnection(addr);
                chan = conn.m_channel;
                }
            catch (IOException e)
                {
                logException(packet.getSocketAddress(), e);
                return;
                }

            synchronized (chan)
                {
                try
                    {
                    if (chan.isConnectionPending())
                        {
                        if (chan.finishConnect())
                            {
                            ConnectionStatus status = new ConnectionStatus();
                            status.m_connection = conn;
                            scheduleRegistration(conn.m_channel, status);
                            }
                        else
                            {
                            // we haven't finished connecting; drop new packet
                            return;
                            }
                        }

                    ByteBuffer[] aBuff = conn.m_pending;
                    int cbRemain = aBuff[1] == null
                            ? 0 // no pending packet
                            : aBuff[0].remaining() + aBuff[1].remaining();
                    if (cbRemain == 0 || chan.write(aBuff) == cbRemain)
                        {
                        // last packet has been completely written; send new

                        byte[]     ab         = packet.getData();
                        int        cb         = packet.getLength();
                        ByteBuffer buffPacket = ByteBuffer.wrap(ab, packet.getOffset(), cb).slice();

                        long cbTotal = cb;
                        switch (m_nPacketMagicMask)
                            {
                            case 0xFFFF0000: // short
                                if (cb > 0xFFFF)
                                    {
                                    throw new IOException(
                                            "packet length exceeds 2^16");
                                    }
                                buffPacket.put((byte) (cb >>> 8))
                                          .put((byte) cb)
                                          .position(0);
                                break;

                            case 0xFFFFFFF0: // trint and counter
                                buffPacket.put(3, (byte) ((conn.m_cTxPacket << 4) | (buffPacket.get(3) & 0x0F)));
                                // fall through

                            case 0xFFFFFF00: // trint
                                if (cb > 0xFFFFFF)
                                    {
                                    throw new IOException(
                                            "packet length exceeds 2^24");
                                    }
                                buffPacket.put((byte) (cb >> 16))
                                          .put((byte) (cb >> 8))
                                          .put((byte) cb)
                                          .position(0);
                                break;

                            case 0xFFFFFFFF: // int
                                buffPacket.putInt(cb)
                                          .position(0);
                                break;

                            default:
                                // write size into buffHead
                                ByteBuffer buffHead = aBuff[0];
                                buffHead.clear();
                                buffHead.putInt(cb);
                                buffHead.flip();
                                cbTotal += HEADER_SIZE;
                                break;
                            }

                        aBuff[1] = buffPacket;

                        // try to write the entire packet
                        long cbWrite = chan.write(aBuff);
                        if (cbWrite > 0)
                            {
                            // at least something was written, increment the
                            // packet counter (COH-4300)
                            ++conn.m_cTxPacket;
                            }

                        if (cbWrite == 0 || cbWrite == cbTotal)
                            {
                            // either wrote everything or nothing, either way
                            // we are done with this packet
                            aBuff[1] = null;
                            return;
                            }

                        // wrote part of the packet, we must finish writing
                        // this one before we can write any others, and we
                        // cannot hold onto buffPacket as the array is owned by
                        // the caller and could change after we return
                        ByteBuffer buff = ByteBuffer.allocate(buffPacket.remaining());
                        buff.put(buffPacket);
                        buff.flip();
                        aBuff[1] = buff;
                        }
                    // else; drop new packet
                    }
                catch (IOException e)
                    {
                    logException(addr, e);
                    closeOutbound(addr);
                    }
                }
            }

        // ---- inner class: NonBlockingConnection ----------------------

        /**
        * A specialized version of {@link Connection} where there is a
        * {@link SocketChannel channel} associated with the connection.
        */
        static class NonBlockingConnection
            extends Connection
            {
            /**
            * Create a new NonBlockingConnection using a SocketChannel and
            * a ByteBuffer containing the protocol header.
            *
            * @param chan  the connected channel
            * @param buff  the allocated {@link ByteBuffer} required protocol
            *              header
            */
            public NonBlockingConnection(SocketChannel chan, ByteBuffer buff)
                {
                super(chan.socket());
                m_channel    = chan;
                m_pending[1] = buff;
                }

            // ----- data members -----------------------------------------

            /**
            * The Channel associated with the connection.
            */
            final SocketChannel m_channel;

            /**
            * The buffer currently being transfered.
            */
            final ByteBuffer[] m_pending = {(ByteBuffer) ByteBuffer
                    .allocate(HEADER_SIZE).flip(), null, };
            }
        }


    // ----- data members ---------------------------------------------------

    /**
    * Flag indicating if split sockets should be used for TcpDatagram sockets.
    *
    * While this should ideally never be needed, testing on Linux has shown
    * that under heavy packet loads the socket can appear to stall and refuse
    * to accept or emit data.  Worse still while the socket is in this state
    * the NIC is transmitting ~300,000 packets/second, even when the process
    * is paused CTRL+Z'd.
    *
    * This setting is conceptually similar to the "coherence.datagram.splitsocket"
    * but only applies to TcpDatagram sockets.  Additionally it does not
    * require multiple listening ports, just multiple connections.
    *
    * As of 3.6.1 this value defaults to false. This became "safe" in 3.6.1
    * since this version began using multiple listening sockets, and avoids
    * the case which would be likely to trigger the Linux stall issue.
    */
    public static final boolean SPLIT = Config.getBoolean("coherence.tcpdatagram.splitsocket", false);
    }
