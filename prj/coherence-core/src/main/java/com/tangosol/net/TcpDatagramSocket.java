/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net;


import com.oracle.coherence.common.base.Logger;

import com.tangosol.coherence.config.Config;

import com.tangosol.util.Base;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.DatagramSocketImpl;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import java.nio.ByteBuffer;

import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import java.nio.channels.spi.SelectorProvider;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.oracle.coherence.common.base.Blocking;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.net.ssl.SSLException;

import com.oracle.coherence.common.net.SocketProvider;


/**
* TCP based datagram socket implementation.
*
* @author mf  2009.12.03
*/
public class TcpDatagramSocket
    extends DatagramSocket
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Create a new TcpDatagramSocket that with a wildcard address bound to an
    * ephemeral port.
    *
    * @throws SocketException if any error happens during the bind, or if the
    *         port is unavailable
    */
    public TcpDatagramSocket()
            throws SocketException
        {
        this(new InetSocketAddress(0));
        }

    /**
    * Creates a new TcpDatagramSocket which will be bound to the specified
    * {@link SocketAddress address}.
    *
    * @param addr  the {@link SocketAddress address} to bind
    *
    * @throws SocketException if any error happens during the bind,
    *         or if the port is unavailable
    */
    public TcpDatagramSocket(SocketAddress addr)
            throws SocketException
        {
        this(new Impl());
        if (addr != null)
            {
            bind(addr);
            }
        }

    /**
    * Creates a new TcpDatagramSocket using the wildcard address and  the
    * specified port.
    * <p>
    * The port number should be between 0 and 65535. Zero means that the system
    * will pick an ephemeral port during the bind operation.
    *
    * @param nPort  the port to bind to
    *
    * @throws SocketException if any error happens during the bind,
    *         or if the port is unavailable
    */
    public TcpDatagramSocket(int nPort)
            throws SocketException
        {
        this(nPort, null);
        }

    /**
    * Creates a new TcpDatagramSocket using an {@link InetAddress address} and
    * a port number.
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
    public TcpDatagramSocket(int nPort, InetAddress addr)
            throws SocketException
        {
        this(new InetSocketAddress(addr, nPort));
        }

    /**
    * Creates a new TcpDatagramSocket using the {@link SocketProvider provider}.
    *
    * @param provider  the {@link SocketProvider provider} to be used
    *
    * @throws SocketException if any error happens during the bind,
    *         or if the port is unavailable
    */
    public TcpDatagramSocket(SocketProvider provider)
            throws SocketException
        {
        this(new Impl(provider));
        }

    /**
    * Creates a new TcpDatagramSocket using an {@link TcpDatagramSocket.Impl}.
    *
    * @param impl  a {@link TcpDatagramSocket.Impl}
    */
    protected TcpDatagramSocket(Impl impl)
        {
        super(impl);
        m_impl = impl;
        }

    // ----- TcpDatagramSocket methods --------------------------------------

    /**
    * Specify SocketOptions to be used to configure each of the underlying
    * TCP sockets. These options will be added to any previously specified
    * options.
    *
    * @param options  the SocketOptions
    *
    * @throws SocketException  if the options fail to be set
    */
    public void setSocketOptions(java.net.SocketOptions options)
            throws SocketException
        {
        m_impl.m_options.copyOptions(options);
        }

    /**
    * Specify the listen backlog for the server socket.
    *
    * @param n  the depth of the backlog, or &lt;=0 for the OS default.
    *
    * @throws IOException  if the port is unavailable
    */
    public void setListenBacklog(int n)
            throws IOException
        {
        if (isBound())
            {
            throw new IOException("already bound");
            }

        m_impl.m_nBacklog = n;
        }

    /**
    * Specify the packet header which is included at the start of every
    * packet.  Because this implementation is TCP based these headers can
    * be stripped off, and replaced on the far side without consuming any
    * network resources.
    *
    * @param nMagic  the packet header
    * @param nMask   the packet header bitmask identifying the bits used
    *
    * @throws IOException if the port is unavailable
    */
    public void setPacketMagic(int nMagic, int nMask)
            throws IOException
        {
        if (isBound())
            {
            throw new IOException("already bound");
            }

        m_impl.setPacketMagic(nMagic, nMask);
        }

    /**
    * Specify the frequency at which the DatagramSocket will advance over
    * the sub-sockets during receive.  A higher value will optimize for
    * throughput as well as latency when communicating with a small number of
    * peers. A low value will optimize for latency when communicating with
    * a large number of peers, but is likely to hurt overall throughput.
    *
    * @param nAdvance  the packet frequency at which to advance between peers
    */
    public void setAdvanceFrequency(int nAdvance)
        {
        m_impl.m_nAdvanceFrequency = Math.max(1, nAdvance);
        }


    // ----- DatagramSocket methods -----------------------------------------

    /**
    * {@inheritDoc}
    */
    public boolean isBound()
        {
        return m_impl.f_socket.socket().isBound();
        }

    /**
    * {@inheritDoc}
    */
    public void send(DatagramPacket p)
            throws IOException
        {
        // bypass the DatagramSocket security manager because:
        // - it is called once per packet
        // - we are going to go through the Socket security manager anyway

        m_impl.send(p);
        }

    /**
    * {@inheritDoc}
    */
    public void receive(DatagramPacket p)
            throws IOException
        {
        // bypass the DatagramSocket security manager because:
        // - it is called once per packet
        // - we are going to go through the Socket security manager anyway
        // - we don't implement the peek methods which are required by the
        //   security manager

        m_impl.receive(p);
        }

    // ----- Object methods -------------------------------------------------

    /**
    * {@inheritDoc}
    */
    public String toString()
        {
        return m_impl.toString();
        }


    // ----- inner class: Impl ----------------------------------------------

    /**
    * A specialized version of {@link DatagramSocketImpl}.
    */
    public static class Impl
            extends DatagramSocketImpl
        {
        // ----- constructors -------------------------------------------

        /**
        * Create a new new Impl.
        *
        * @throws SocketException if any error happens during the bind, or if
        *                         the port is unavailable
        */
        public Impl()
                throws SocketException
            {
            this(SocketProviderFactory.DEFAULT_SOCKET_PROVIDER);
            }

        /**
        * Create a new Impl using a {@link SocketProvider provider}.
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
            m_provider          = provider;
            m_mapConnectionsOut = new ConcurrentHashMap<SocketAddress, Connection>();
            m_mapOptions        = new ConcurrentHashMap<Integer, Object>();

            try
                {
                // set our default TCP specific options
                SocketOptions options = m_options;
                options.setOption(SocketOptions.TCP_NODELAY, true);
                options.setOption(SocketOptions.SO_LINGER, 0);

                ServerSocketChannel socket = f_socket = provider
                        .openServerSocketChannel();

                socket.configureBlocking(false);
                f_selector = socket.provider().openSelector();
                }
            catch (IOException e)
                {
                throw ensureSocketException(e);
                }
            }

        // ----- DatagramSocketImpl methods -----------------------------

        /**
        * Return the SelectorProvider associated with this socket.
        *
        * @return the SelectorProvider
        */
        public SelectorProvider provider()
            {
            return f_selector.provider();
            }

        /**
        * Specify the packet header which is included at the start of every
        * packet.  Because this implementation is TCP based these headers can
        * be stripped off, and replaced on the far side without consuming any
        * network resources.
        *
        * @param nMagic  the packet header
        * @param nMask   the packet header bitmask identifying the bits used
        *                the mask must be in byte increments
        */
        public void setPacketMagic(int nMagic, int nMask)
            {
            m_nPacketMagic      = nMagic & nMask;
            m_nPacketMagicMask  = nMask;
            }


        /**
        * {@inheritDoc}
        */
        protected void create()
                throws SocketException
            {
            // handled in constructor
            }

        /**
        * {@inheritDoc}
        */
        protected void bind(int nPort, InetAddress addr)
                throws SocketException
            {
            bind(new InetSocketAddress(addr, nPort));
            }

        /**
        * {@inheritDoc}
        */
        protected int getLocalPort()
            {
            return f_socket == null ? 0 : f_socket.socket().getLocalPort();
            }

        /**
        * {@inheritDoc}
        */
        protected void send(DatagramPacket packet)
                throws IOException
            {
            int          cb  = packet.getLength();
            OutputStream out;
            Connection   conn;
            try
                {
                conn = ensureConnection(packet.getSocketAddress());
                out  = conn.m_out;
                }
            catch (IOException e)
                {
                logException(packet.getSocketAddress(), e);
                return;
                }

            synchronized (out)
                {
                try
                    {
                    byte[] abPacket = packet.getData();

                    switch (m_nPacketMagicMask)
                        {
                        case 0xFFFF0000: // short
                            if (cb > 0xFFFF)
                                {
                                throw new IOException("packet length exceeds 2^16");
                                }
                            abPacket[0] = (byte) (cb >>> 8);
                            abPacket[1] = (byte) cb;
                            break;

                        case 0xFFFFFFF0: // trint and counter
                            abPacket[3] = (byte) ((conn.m_cTxPacket << 4) | (abPacket[3] & 0x0F));
                            // fall through

                        case 0xFFFFFF00: // trint
                            if (cb > 0xFFFFFF)
                                {
                                throw new IOException("packet length exceeds 2^24");
                                }
                            abPacket[0] = (byte) (cb >>> 16);
                            abPacket[1] = (byte) (cb >>> 8);
                            abPacket[2] = (byte) cb;
                            break;

                        case 0xFFFFFFFF: // int
                            abPacket[0] = (byte) (cb >>> 24);
                            abPacket[1] = (byte) (cb >>> 16);
                            abPacket[2] = (byte) (cb >>> 8);
                            abPacket[3] = (byte) cb;
                            break;

                        default:
                            out.write(cb >>> 24);
                            out.write(cb >>> 16);
                            out.write(cb >>> 8);
                            out.write(cb);
                            break;
                        }

                    out.write(packet.getData(), packet.getOffset(), cb);
                    out.flush();
                    ++conn.m_cTxPacket;
                    }
                catch (IOException e)
                    {
                    closeOutbound(packet.getSocketAddress());
                    }
                }
            }

        /**
        * {@inheritDoc}
        */
        protected int peek(InetAddress addr)
                throws IOException
            {
            return 0;
            }

        /**
        * {@inheritDoc}
        */
        protected int peekData(DatagramPacket packet)
                throws IOException
            {
            return 0;
            }

        /**
        * {@inheritDoc}
        */
        protected void receive(DatagramPacket packet)
                throws IOException
            {
            ByteBuffer buffPacket = ByteBuffer.wrap(packet.getData(),
                    packet.getOffset(), packet.getLength());

            // optimized common path for hot-spot friendliness
            SelectionKey key = m_keyCurrent;
            if (key == null || m_cKeyUses++ > m_nAdvanceFrequency
                    || !onRead(key, buffPacket))
                {
                try
                    {
                    key = null; // in case nextKey throws
                    key = nextKey(buffPacket);
                    }
                finally
                    {
                    m_keyCurrent = key;
                    m_cKeyUses   = 1;
                    }
                }

            // we have a packet
            ConnectionStatus status = (ConnectionStatus) key.attachment();
            packet.setLength(status.m_cbBody);
            packet.setSocketAddress(status.m_addr);
            packet.setPort(status.m_addr.getPort());
            }


        /**
        * Perform a blocking read, waiting for a complete packet.
        *
        * @param buffPacket  the packet buffer
        *
        * @return the corresponding SelectionKey
        *
        * @throws SocketTimeoutException if SO_TIMEOUT is exceeded
        */
        protected SelectionKey nextKey(ByteBuffer buffPacket)
                throws IOException
            {
            Selector                             selector         = f_selector;
            Map<SocketChannel, ConnectionStatus> mapSched         = m_mapRegScheduled;
            long                                 cMillisSoTimeout = m_cMillisSoTimeout;

            synchronized (selector)
                {
                Iterator<SelectionKey> iterPending = m_iterKeysPending;
                Set<SelectionKey> setPending       = selector.isOpen()
                                        ? selector.selectedKeys()
                                        : null;

                while (!m_fClosing)
                    {
                    try
                        {
                        if (iterPending == null || !iterPending.hasNext())
                            {
                            try
                                {
                                m_iterKeysPending = iterPending = null; // will be invalidated by select call

                                Blocking.select(selector, cMillisSoTimeout);

                                if (!mapSched.isEmpty())
                                    {
                                    processRegistrations();
                                    continue; // re-select
                                    }

                                if (setPending.isEmpty())
                                    {
                                    // lazily ensure binding; this is the only
                                    // way out of here if we are not yet bound
                                    ensureBound();
                                    throw new SocketTimeoutException();
                                    }

                                m_iterKeysPending = iterPending = selector.
                                        selectedKeys().iterator();
                                }
                            catch (ClosedChannelException e)
                                {
                                throw new SocketException("closed socket");
                                }
                            }

                        SelectionKey key = iterPending.next();
                        iterPending.remove();

                        if (key == null || !key.isValid())
                            {
                            // continue;
                            }
                        else if (key.isReadable())
                            {
                            if (onRead(key, buffPacket))
                                {
                                // we have a packet
                                return key;
                                }
                            }
                        else if (key.isAcceptable())
                            {
                            onAccept(key);
                            }
                        else if (key.readyOps() == 0)
                            {
                            // compensate for Java NIO bug:
                            // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6403933
                            closeInbound((ConnectionStatus) key.attachment(),
                                    (SocketChannel) key.channel());
                            }
                        }
                    catch (ClosedSelectorException e)
                        {
                        throw new SocketException("closed socket");
                        }
                    catch (CancelledKeyException e) {}
                    }
                }

            throw new SocketException("closed socket");
            }

        /**
        * Called when a channel is identified as have a new connection to
        * accept.
        *
        * @param key  the associated SelectionKey
        */
        protected void onAccept(SelectionKey key)
            {
            SocketChannel chan = null;
            try
                {
                // accept the connection and register it with the selector
                chan = ((ServerSocketChannel) key.channel()).accept();
                if (chan != null)
                    {
                    chan.configureBlocking(false);
                    configureSocket(chan.socket());
                    chan.register(f_selector, SelectionKey.OP_READ,
                            new ConnectionStatus());
                    }
                }
            catch (IOException e)
                {
                logException(chan == null
                        ? null
                        : chan.socket().getRemoteSocketAddress(),
                    e);
                }
            }


        /**
        * Called when a channel is detected as readable.
        *
        * @param key         the associated SelectionKey
        * @param buffPacket  a buffer in which to place any available full
        *                    packet
        * @return            true iff a packet has been read
        */
        protected boolean onRead(SelectionKey key, ByteBuffer buffPacket)
            {
            SocketChannel    chan;
            ConnectionStatus status;
            try
                {
                chan   = (SocketChannel) key.channel();
                status = (ConnectionStatus) key.attachment();
                }
            catch (CancelledKeyException e)
                {
                return false;
                }

            Socket     socket        = chan.socket();
            boolean    fEOS          = false;
            int        nLimitRestore = -1;
            int        nPosRestore   = -1;
            int        nPacketMagic  = m_nPacketMagic;
            int        nPacketMask   = m_nPacketMagicMask;
            int        nHead         = 0;
            ByteBuffer buff;

            try
                {
                switch (status.m_state)
                    {
                    case ConnectionStatus.WAIT_MAGIC:
                    case ConnectionStatus.WAIT_PORT:
                        fEOS = onConnectionHeader(status, chan);
                        if (status.m_state != ConnectionStatus.WAIT_HEAD)
                            {
                            break;
                            }
                        // fall through; try to read packet header

                    case ConnectionStatus.WAIT_HEAD:
                        // process packet header
                        buff = status.m_head;

                        // TODO: optimize packet read so that it can complete
                        // in a single chan.read, rather then at least two

                        fEOS = chan.read(buff) < 0;
                        if (buff.hasRemaining())
                            {
                            break; // work on other channels
                            }

                        buff.flip();
                        nHead = buff.getInt();

                        // decode the size from the first int based on the
                        // magic mask
                        switch (nPacketMask)
                            {
                            case 0xFFFF0000: // short
                                status.m_cbBody = (nHead >>> 16);
                                break;

                            case 0xFFFFFFF0: // trint and counter
                                if ((status.m_cRxPacket & 0x0F) != ((nHead & 0x0F0) >>> 4))
                                    {
                                    // indicates that we have a corrupted stream
                                    Logger.info("Recovering corrupted "
                                            + "packet stream from "
                                            + status.m_addr + " detected at packet "
                                            + status.m_cRxPacket + ", last packet"
                                            + " length " + status.m_cbBody);
                                    throw new IOException();
                                    }
                                // fall through

                            case 0xFFFFFF00: // trint
                                status.m_cbBody = (nHead >>> 8);
                                break;

                            case 0xFFFFFFFF: // int
                            default:
                                status.m_cbBody = nHead;
                                break;
                            }

                        ++status.m_cRxPacket;
                        buff.clear();
                        status.m_state = ConnectionStatus.WAIT_BODY;
                        // fall through; try to read the body

                    case ConnectionStatus.WAIT_BODY:
                        // process packet body
                        if (status.m_body.position() == 0)
                            {
                            // we haven't started reading the body yet
                            if (buffPacket.remaining() >= status.m_cbBody)
                                {
                                // use the packet directly; record positions
                                buff          = buffPacket;
                                nPosRestore   = buff.position();
                                nLimitRestore = buff.limit();
                                buff.limit(nPosRestore + status.m_cbBody);
                                }
                            else if (status.m_body.remaining() >= status.m_cbBody)
                                {
                                // use the temp buffer
                                buff = status.m_body;
                                buff.limit(status.m_cbBody);
                                }
                            else
                                {
                                // temp buffer isn't big enough
                                buff = status.m_body = ByteBuffer.allocate(status.m_cbBody);
                                }

                            // replace the magic if necessary
                            switch (nPacketMask)
                                {
                                case 0xFFFF0000: // short
                                case 0xFFFFFF00: // trint
                                case 0xFFFFFFF0: // trint and counter
                                case 0xFFFFFFFF: // int
                                    if (status.m_cbBody < 4)
                                        {
                                        Logger.info("Recovering corrupted"
                                                + " packet stream from "
                                                + status.m_addr + " detected at"
                                                + " packet " + status.m_cRxPacket
                                                + ", with packet length "
                                                + status.m_cbBody + ", buffer "
                                                + "capacity "
                                                + status.m_body.capacity());
                                        throw new IOException();
                                        }

                                    buff.putInt(nPacketMagic | (~nPacketMask & nHead));
                                    break;

                                default:
                                    // magic was not stripped from packet
                                    break;
                                }
                            }
                        else
                            {
                            // we have a partial body
                            buff = status.m_body;
                            }

                        fEOS = chan.read(buff) < 0;

                        if (!buff.hasRemaining())
                            {
                            // full packet has been read
                            if (buff == status.m_body)
                                {
                                // transfer into buffPacket
                                buff.rewind();
                                status.m_cbBody = transferBytes(buff, buffPacket);
                                status.m_body.clear();
                                }
                            // else; buffPacket holds the entire packet

                            status.m_state = ConnectionStatus.WAIT_HEAD;
                            return true; // we have a packet
                            }

                        // didn't complete the read
                        if (buff == buffPacket)
                            {
                            // we read directly into the packet; transfer read
                            // bytes into status.body
                            if (status.m_body.capacity() < status.m_cbBody)
                                {
                                status.m_body = ByteBuffer.allocate(status.m_cbBody);
                                }
                            else
                                {
                                status.m_body.limit(status.m_cbBody);
                                }

                            buffPacket.limit(buffPacket.position());
                            buffPacket.position(nPosRestore);

                            transferBytes(buffPacket, status.m_body);
                            }
                        // else; was reading into status.body nothing to do
                        break;

                    default:
                        throw new IllegalStateException();
                    }
                }
            catch (IOException e)
                {
                logException(status.m_addr == null
                        ? socket.getRemoteSocketAddress()
                        : status.m_addr, e);
                closeInbound(status, chan);
                }
            finally
                {
                if (nLimitRestore != -1)
                    {
                    // restore original packet position and limit if they were
                    // updated
                    buffPacket.limit(nLimitRestore);
                    buffPacket.position(nPosRestore);
                    }

                if (fEOS)
                    {
                    closeInbound(status, chan);
                    }
                }
            return false; // try another socket
            }

        /**
        * Process a pending connection header.
        *
        * @param status  the associated ConnectionStatus
        * @param chan    the associated channel
        *
        * @return true if EOS has been reached
        *
        * @throws IOException if an I/O error occurs
        */
        protected boolean onConnectionHeader(ConnectionStatus status, SocketChannel chan)
                throws IOException
            {
            Socket     socket = chan.socket();
            ByteBuffer buff   = status.m_head;
            boolean    fEOS   = false;

            switch (status.m_state)
                {
                case ConnectionStatus.WAIT_MAGIC:
                    fEOS = chan.read(buff) < 0;
                    if (buff.hasRemaining())
                        {
                        break; // work on other channels
                        }

                    buff.flip();
                    int nMagic = buff.getInt();
                    buff.clear();
                    if (nMagic != PROTOCOL_MAGIC)
                        {
                        if (nMagic == 0)
                            {
                            // special case; if nothing other then zeros are
                            // sent, then we stay in a holding pattern waiting
                            // for the magic.  The purpose of this is to all
                            // monitoring of the channel by plain TCP
                            // connections, i.e. TCPRing
                            break; // work on other channels
                            }
                        fEOS = true;
                        logProtocolWarning(socket.getRemoteSocketAddress(),
                                status, nMagic);
                        break; // work on other channels
                        }

                    status.m_state = ConnectionStatus.WAIT_PACKET_MAGIC;
                    // fall through

                case ConnectionStatus.WAIT_PACKET_MAGIC:
                    fEOS = chan.read(buff) < 0;
                    if (buff.hasRemaining())
                        {
                        break; // work on other channels
                        }

                    buff.flip();
                    int nPacketMagic = buff.getInt();
                    buff.clear();
                    if (nPacketMagic != m_nPacketMagic)
                        {
                        fEOS = true;
                        logProtocolWarning(socket.getRemoteSocketAddress(),
                                status, nPacketMagic);
                        break; // work on other channels
                        }

                    status.m_state = ConnectionStatus.WAIT_PACKET_MAGIC_MASK;
                    // fall through

                case ConnectionStatus.WAIT_PACKET_MAGIC_MASK:
                    fEOS = chan.read(buff) < 0;
                    if (buff.hasRemaining())
                        {
                        break; // work on other channels
                        }

                    buff.flip();
                    int nPacketMagicMask = buff.getInt();
                    buff.clear();
                    if (nPacketMagicMask != m_nPacketMagicMask)
                        {
                        fEOS = true;
                        logProtocolWarning(socket.getRemoteSocketAddress(),
                                status, nPacketMagicMask);
                        break; // work on other channels
                        }

                    status.m_state = ConnectionStatus.WAIT_PORT;
                    // fall through

                case ConnectionStatus.WAIT_PORT:
                    fEOS = chan.read(buff) < 0;
                    if (buff.hasRemaining())
                        {
                        break; // work on other channels
                        }

                    // create new buffer for the body
                    buff.flip();
                    status.m_addr = new InetSocketAddress(chan.socket().
                            getInetAddress(), buff.getInt());
                    buff.clear();
                    status.m_state = ConnectionStatus.WAIT_HEAD;
                    break;

                default:
                    throw new IllegalStateException();
                }

            return fEOS;
            }

        /**
        * {@inheritDoc}
        */
        protected void close()
            {
            Map<SocketAddress, Connection> map = m_mapConnectionsOut;
            synchronized (map)
                {
                for (Connection conn : map.values())
                    {
                    if (conn != null)
                        {
                        try
                            {
                            conn.m_socket.close();
                            }
                        catch (IOException e)
                            {
                            logException(conn.m_socket.getRemoteSocketAddress(),
                                    e);
                            }
                        }
                    }
                }

            Selector selector = f_selector;
            m_fClosing = true;
            selector.wakeup(); // terminate any existing calls to receive

            synchronized (selector) // wait for existing receive call to complete
                {
                for (SelectionKey key : selector.keys())
                    {
                    if (key != null && key.channel() instanceof SocketChannel)
                        {
                        closeInbound((ConnectionStatus) key.attachment(),
                                (SocketChannel) key.channel());
                        }
                    }

                try
                    {
                    selector.close();
                    }
                catch (IOException e) {}
                }

            // don't allow acceptor port to be reused until we've cleaned up
            // all TCP sockets
            try
                {
                f_socket.close();
                }
            catch (IOException e)
                {
                logException(f_socket.socket().getLocalSocketAddress(), e);
                }
            }

        /**
        * {@inheritDoc}
        */
        public void setOption(int nId, Object oValue)
                throws SocketException
            {
            m_mapOptions.put(nId, oValue);
            if (nId == java.net.SocketOptions.SO_TIMEOUT)
                {
                m_cMillisSoTimeout = ((Number) oValue).longValue();
                }

            // The only other setting which could map through from UDP to TCP
            // would be the send and receive space sizes. Though we could push
            // these settings down it would be unfair, the application had
            // requested that we use X bytes for buffering but we would use
            // X*N where N is the number of peers we end up conneted to.
            // Instead we just rely on the system defaults.

            // TODO: we could try to balance the buffer setting across all
            // sockets over time, perhaps only updating the sockets at most
            // once a minute, and only if the total memory allocation was
            // off by more tehn 10% of the configured limit?
            }

        /**
        * {@inheritDoc}
        */
        public Object getOption(int nId)
                throws SocketException
            {
            switch (nId)
                {
                case java.net.SocketOptions.SO_BINDADDR:
                    return f_socket == null ? null : f_socket.socket().getInetAddress();

                default:
                    return m_mapOptions.get(nId);
                }
            }

        // ----- unsupported operations ---------------------------------

        /**
        * {@inheritDoc}
        */
        protected void setTTL(byte ttl)
                throws IOException
            {
            throw new UnsupportedOperationException();
            }

        /**
        * {@inheritDoc}
        */
        protected byte getTTL()
                throws IOException
            {
            throw new UnsupportedOperationException();
            }

        /**
        * {@inheritDoc}
        */
        protected void setTimeToLive(int ttl)
                throws IOException
            {
            throw new UnsupportedOperationException();
            }

        /**
        * {@inheritDoc}
        */
        protected int getTimeToLive()
                throws IOException
            {
            throw new UnsupportedOperationException();
            }

        /**
        * {@inheritDoc}
        */
        protected void join(InetAddress inetaddr)
                throws IOException
            {
            throw new UnsupportedOperationException();
            }

        /**
        * {@inheritDoc}
        */
        protected void leave(InetAddress inetaddr)
                throws IOException
            {
            throw new UnsupportedOperationException();
            }

        /**
        * {@inheritDoc}
        */
        protected void joinGroup(SocketAddress mcastaddr, NetworkInterface netIf)
                throws IOException
            {
            throw new UnsupportedOperationException();
            }

        /**
        * {@inheritDoc}
        */
        protected void leaveGroup(SocketAddress mcastaddr, NetworkInterface netIf)
                throws IOException
            {
            throw new UnsupportedOperationException();
            }

        // ----- helper methods -----------------------------------------

        /**
        * Bind the socket to the specified address.
        *
        * @param addr  the address to bind to
        *
        * @throws SocketException  if an I/O error occurs
        */
        protected void bind(SocketAddress addr)
                throws SocketException
            {
            try
                {
                // custom server config
                ServerSocketChannel socket = f_socket;

                m_options.apply(socket.socket());
                socket.socket().bind(addr, m_nBacklog);
                socket.register(f_selector, SelectionKey.OP_ACCEPT);
                }
            catch (IOException e)
                {
                throw ensureSocketException(e);
                }
            }

        /**
        * Apply socket options to a socket.
        *
        * @param socket  the socket to configure
        *
        * @throws SocketException on configuration error
        */
        void configureSocket(Socket socket)
                throws SocketException
            {
            // apply socket configuration
            m_options.apply(socket);
            }

        /**
        * Ensure that the socket is bound.
        *
        * @throws SocketException if an I/O error occurs
        */
        void ensureBound()
                throws SocketException
            {
            ServerSocketChannel socket = f_socket;
            if (!socket.socket().isBound())
                {
                try
                    {
                    bind(null);
                    }
                catch (IOException e)
                    {
                    if (!socket.socket().isBound())
                        {
                        throw ensureSocketException(e);
                        }
                    }
                }
            }

        /**
        * Convert an IOException into a SocketException.
        *
        * @param e  the IOExcepotion
        *
        * @return the SocketException
        */
        protected static SocketException ensureSocketException(IOException e)
            {
            if (e instanceof SocketException)
                {
                return (SocketException) e;
                }

            SocketException es = new SocketException(e.getMessage());
            es.initCause(e);
            return es;
            }

        /**
        * Produce a new Connection for the specified destination address.
        *
        * @param addr  the destination address
        *
        * @return  the corresponding connection
        *
        * @throws IOException if an I/O error occurs
        */
        protected Connection makeConnection(SocketAddress addr)
                throws IOException
            {
            Connection   conn = new Connection(addr, m_provider, m_options);
            OutputStream out  = conn.m_out;

            // start the connection by sending magic
            int nMagic = PROTOCOL_MAGIC;
            out.write(nMagic >>> 24);
            out.write(nMagic >>> 16);
            out.write(nMagic >>> 8);
            out.write(nMagic);

            // then the packet magic
            int nPacketMagic = m_nPacketMagic;
            out.write(nPacketMagic >>> 24);
            out.write(nPacketMagic >>> 16);
            out.write(nPacketMagic >>> 8);
            out.write(nPacketMagic);

            // then the packet magic mask
            int nPacketMagicMask = m_nPacketMagicMask;
            out.write(nPacketMagicMask >>> 24);
            out.write(nPacketMagicMask >>> 16);
            out.write(nPacketMagicMask >>> 8);
            out.write(nPacketMagicMask);

            // and then our acceptor port this allows the receiving side to
            // re-build the UDP like source address
            int nPort = f_socket.socket().getLocalPort();
            out.write(nPort >>> 24);
            out.write(nPort >>> 16);
            out.write(nPort >>> 8);
            out.write(nPort);

            return conn;
            }

        /**
        * Obtain a Connection for the specified address.
        *
        * @param addr  the destination address
        *
        * @return  the corresponding connection
        *
        * @throws IOException if an I/O error occurs
        */
        protected Connection ensureConnection(SocketAddress addr)
                throws IOException
            {
            ConcurrentMap<SocketAddress, Connection> map  = m_mapConnectionsOut;
            Connection                               conn = map.get(addr);
            if (conn == null)
                {
                if (f_socket.socket().isClosed())
                    {
                    throw new SocketException("Socket is closed");
                    }
                ensureBound();
                conn = makeConnection(addr);
                if (map.putIfAbsent(addr, conn) == null)
                    {
                    return conn;
                    }
                else
                    {
                    map.get(addr);
                    }
                }
            return conn;
            }

        /**
        * Close the inbound {@link SocketChannel channel}.
        *
        * @param status  the {@link TcpDatagramSocket.Impl.ConnectionStatus
        *                ConnectionStatus} corresponding to the channel
        * @param chan    the channel to close
        */
        protected void closeInbound(ConnectionStatus status, SocketChannel chan)
            {
            if (status.m_connection != null && status.m_addr != null)
                {
                m_mapConnectionsOut.remove(status.m_addr, status.m_connection);
                }

            try
                {
                chan.close();
                }
            catch (IOException e)
                {
                logException(status.m_addr == null
                        ? chan.socket().getRemoteSocketAddress()
                        : status.m_addr, e);
                }
            }

        /**
        * Close the outbound socket.
        *
        * @param addr the {@link SocketAddress address} of the outbound socket
        */
        protected void closeOutbound(SocketAddress addr)
            {
            Connection conn = m_mapConnectionsOut.remove(addr);
            if (conn != null)
                {
                if (conn.m_out != null)
                    {
                    try
                        {
                        conn.m_out.flush();
                        }
                    catch (IOException e)
                        {
                        logException(addr, e);
                        }
                    }

                try
                    {
                    SocketChannel chan = conn.m_socket.getChannel();
                    if (chan == null)
                        {
                        conn.m_socket.close();
                        }
                    else
                        {
                        chan.close();
                        }
                    }
                catch (IOException e)
                    {
                    logException(addr, e);
                    }
                }
            }

        /**
        * Schedule a registration with the selector, and wake it up.
        *
        * @param chan    the channel to scheduled registration for
        * @param status  the associated ConnectionStatus to register
        */
        protected void scheduleRegistration(SocketChannel chan, ConnectionStatus status)
            {
            m_mapRegScheduled.put(chan, status);
            f_selector.wakeup();
            }

        /**
        * Process any scheduled selector registrations.
        */
        protected void processRegistrations()
            {
            Selector                              selector = f_selector;
            Map<SocketChannel, ConnectionStatus>  mapReg   = m_mapRegScheduled;

            synchronized (mapReg)
                {
                for (Iterator<Map.Entry<SocketChannel, ConnectionStatus>> iter
                        = m_mapRegScheduled.entrySet().iterator(); iter.hasNext(); )
                    {
                    Map.Entry<SocketChannel, ConnectionStatus> entry = iter.next();

                    if (entry != null)
                        {
                        iter.remove();
                        SocketChannel    chan   = entry.getKey();
                        ConnectionStatus status = entry.getValue();
                        try
                            {
                            chan.register(selector, SelectionKey.OP_READ, status);
                            }
                        catch (ClosedChannelException e)
                            {
                            closeInbound(status, chan);
                            }
                        }
                    }
                }
            }

        /**
        * Transfer bytes from the source to the destination buffer based on
        * their limits.
        *
        * @param buffSrc  the source buffer
        * @param buffDst  the destination buffer
        *
        * @return the number of bytes transfered
        */
        protected int transferBytes(ByteBuffer buffSrc, ByteBuffer buffDst)
            {
            int ofSrc = buffSrc.position();
            int cbSrc = buffSrc.remaining();
            int cbDst = buffDst.remaining();
            if (cbSrc > cbDst)
                {
                // dst can't hold entire src, copy as much as possible
                buffDst.put(buffSrc.array(), buffSrc.arrayOffset() + ofSrc, cbDst);
                buffSrc.position(ofSrc + cbDst);
                return cbDst;
                }
            else // cbSrc <= cbDst
                {
                // transfer the remaining contents
                buffDst.put(buffSrc);
                return cbSrc;
                }
            }

        /**
        * Log an exception which is handled internally by the TcpDatagramSocket.
        *
        * @param addr  the associated address
        * @param e     the exception
        */
        protected void logException(SocketAddress addr, IOException e)
            {
            int nLevel = e instanceof SSLException
                    ? Logger.WARNING // all security exceptions are logged
                    : IO_EXCEPTIONS_LOG_LEVEL;
            if (nLevel >= 0 && Logger.isEnabled(nLevel))
                {
                Logger.log(this + ", exception regarding peer "
                        + addr + ", " + Base.getDeepMessage(e, "; "), nLevel);

                if (IO_EXCEPTIONS_LOG_LEVEL >= 0
                        && Logger.isEnabled(IO_EXCEPTIONS_LOG_LEVEL))
                    {
                    // full stack traces are only logged if explictly requested
                    Logger.err(e);
                    }
                }
            }

        /**
        * Periodically log a warning when connections are made using an
        * unrecognized protocol.
        *
        * @param addr    the source address of the connection
        * @param status  the connection status
        * @param nMagic  the "magic" header they sent
        */
        protected void logProtocolWarning(SocketAddress addr, ConnectionStatus status, int nMagic)
            {
            long ldtNow = Base.getSafeTimeMillis();
            if (ldtNow - m_ldtLastWarn > 10000)
                {
                Logger.warn("Unexpected protocol header " + nMagic
                        + " in state " + status.m_state + " received from "
                        + addr + " dropping connection");
                m_ldtLastWarn = ldtNow;
                }
            }

        /**
        * {@inheritDoc}
        */
        public String toString()
            {
            return "TcpDatagramSocket{bind=" + f_socket.socket() + "}";
            }


        // ----- inner class: Connection --------------------------------

        /**
        * A representation of a outbound connection.
        */
        static class Connection
            {
            /**
            * Creates a new Connection. The {@link SocketProvider provider} is
            * used to create the underlying socket and the {@link SocketOptions
            * options} will be applied to the socket before it is connected to
            * {@link SocketAddress addr}. The outbound strem will then be
            * wrapped in a {@link BufferedOutputStream}.
            *
            * @param addr      the {@link SocketAddress address} to connect to
            * @param provider  the {@link SocketProvider provider} to use to
            *                  create the underlying socket
            * @param options   the {@link SocketOptions options} to apply to the
            *                  underlying socket
            *
            * @throws IOException  if an I/O error occurs when creating the
            *                      output stream or if the socket is not connected.
            */
            public Connection(SocketAddress addr, SocketProvider provider,
                SocketOptions options)
                    throws IOException
                {
                m_socket = provider.openSocket();
                options.apply(m_socket);

                m_socket.connect(addr);

                m_out = new BufferedOutputStream(m_socket.getOutputStream());
                }

            /**
            * Creates a new Connection using the {@link Socket socket} as the
            * underlying socket. Will set m_out to <code>null</code>.
            *
            * @param socket  the underlying socket
            */
            protected Connection(Socket socket)
                {
                m_out    = null;
                m_socket = socket;
                }

            // ----- data members -----------------------------------------

            /**
            * The underlying {@link Socket socket}.
            */
            final Socket m_socket;

            /**
            * The {@link OutputStream stream} used to send.
            */
            final OutputStream m_out;

            /**
            * The number of 'packets' sent.
            */
            int m_cTxPacket;
            }


        // ----- inner class: ConnectionStatus --------------------------

        /**
        * ConnectionStatus.
        */
        static class ConnectionStatus
            {
            // associated outbound connection, if any
            public Connection m_connection;

            public int m_state = WAIT_MAGIC;
            public InetSocketAddress m_addr;
            public ByteBuffer m_head = ByteBuffer.allocate(HEADER_SIZE);
            public ByteBuffer m_body = ByteBuffer.allocate(2048);
            public int m_cbBody;
            public int m_cRxPacket;

            public static final int WAIT_MAGIC             = 0;
            public static final int WAIT_PACKET_MAGIC      = 1;
            public static final int WAIT_PACKET_MAGIC_MASK = 2;
            public static final int WAIT_PORT              = 3;
            public static final int WAIT_HEAD              = 4;
            public static final int WAIT_BODY              = 5;
            }

        // ----- data members -----------------------------------------

        /**
        * SocketProvider to use in creating internal sockets.
        */
        final SocketProvider m_provider;

        /**
        * Configuration for the underlying sockets.
        */
        final SocketOptions m_options = new SocketOptions();

        /**
        * The TCP listen backlog for the server socket.
        */
        int m_nBacklog;

        /**
        * A map of pending selector registrations, key is chan, value is attachment.
        */
        final Map<SocketChannel, ConnectionStatus> m_mapRegScheduled
            = new ConcurrentHashMap<SocketChannel, ConnectionStatus>();

        /**
        * Selector.
        */
        final Selector f_selector;

        /**
        * The pending set of selector keys.
        */
        Iterator<SelectionKey> m_iterKeysPending;

        /**
        * The current key to read from.
        */
        SelectionKey m_keyCurrent;

        /**
        * The number of times the current key has been consequitively ready from.
        */
        int m_cKeyUses;

        /**
         * True iff the socket is closing.
         */
        boolean m_fClosing;

        /**
        * The maximum number of times to consequitively read from a key.
        */
        int m_nAdvanceFrequency = 32;

        /**
        * Acceptor socket.
        */
        final ServerSocketChannel f_socket;

        /**
        * Map of InetSocketAddress to corresponding outbound Socket connections.
        */
        ConcurrentMap<SocketAddress, Connection> m_mapConnectionsOut;

        /**
        * Socket options.
        */
        Map<Integer, Object> m_mapOptions;

        /**
        * SO_TIMEOUT.
        */
        long m_cMillisSoTimeout;

        /**
        * The timestamp of the last warning message.
        */
        long m_ldtLastWarn;

        /**
        * The header included in every packet.
        */
        int m_nPacketMagic;

        /**
        * The part of m_nPacketMagic which defines the header.
        */
        int m_nPacketMagicMask;


        // ----- constants ----------------------------------------------

        /**
        * The fixed header size for packets.
        */
        public static final int HEADER_SIZE = 4;

        /**
        * Protcol identifier used to identify that peers are also
        * TcpDatagramSockets. This is necessary so that we don't try to act
        * upon garbage in this class, for instance trying to allocate a
        * negative or gigabit sized packet.
        */
        public static final int PROTOCOL_MAGIC = 0x0DDF00DA;
        }


    // ----- constants ------------------------------------------------------

    /**
    * Debbuging flag for logging an IO exceptions which occur, set to a negative
    * to disable the logging.
    */
    public static final int IO_EXCEPTIONS_LOG_LEVEL = Config.getInteger("coherence.tcpdatagram.log.level", -1);


    // ----- data members ---------------------------------------------------

    protected Impl m_impl;
    }
