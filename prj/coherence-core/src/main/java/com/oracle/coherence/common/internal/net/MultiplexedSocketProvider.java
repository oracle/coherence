/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.internal.net;


import com.oracle.coherence.common.base.Blocking;
import com.oracle.coherence.common.collections.UnmodifiableSetCollection;
import com.oracle.coherence.common.io.Buffers;
import com.oracle.coherence.common.net.InetAddressComparator;
import com.oracle.coherence.common.net.InetAddresses;
import com.oracle.coherence.common.net.InetSocketAddress32;
import com.oracle.coherence.common.net.SafeSelectionHandler;
import com.oracle.coherence.common.net.SelectionService;
import com.oracle.coherence.common.net.SelectionServices;
import com.oracle.coherence.common.net.SocketProvider;
import com.oracle.coherence.common.net.TcpSocketProvider;
import com.oracle.coherence.common.util.Duration;

import java.net.ProtocolFamily;
import java.net.SocketOption;
import java.net.SocketTimeoutException;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.channels.spi.AbstractSelector;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.SelectorProvider;

import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.IOException;
import java.io.OutputStream;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import java.util.concurrent.*;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogRecord;


/**
 * MultiplexedSocketProvider produces a family of sockets which utilize
 * {@link InetSocketAddress32 extended port} values to allow for multiplexing
 * of sockets. The primary benefit of multiplexed server sockets is that it
 * allows a process to host many logical server sockets over a single inet
 * port, thus reducing firewall configuration.
 * <p>
 * This SocketProvider makes use of {@link InetSocketAddress32} based addresses.
 * The break down of the port range is as follows.
 * <ul>
 * <li>
 *     0x00000000 .. 0x0000FFFF maps directly to standard inet based addresses,
 *     i.e. non-multiplexed
 * </li>
 * <li>
 *     0x00010000 .. 0xFFFFFFFF maps to multiplexed sockets
 * </li>
 * </ul>
 * <p>
 * Non-multiplexed sockets produced by this provider can communicate with
 * standard sockets, while multiplexed sockets can only communicate with other
 * multiplexed socket instances.
 * <p>
 * In the case of a multiplexed socket port, the upper sixteen bits represent
 * the actual inet port binding, and the lower sixteen bits represents the
 * sub-port or channel within the actual socket. The encoding of these two
 * 16-bit port values into a 32-bit int is as follows:
 * <p><code>nPort = ~(nPortBase &lt;&lt;&lt; 16 | nPortSub)</code></p>
 * A sub-port of 0 represents a sub-ephemeral address. With the above encoding a
 * 32-bit port value of -1 thus represents a "double" ephemeral port which means
 * an ephemeral sub-port on an ephemeral port.
 * <p>
 * As a matter of convenience {@link #resolveAddress} supports resolving ports in a
 * dot delimited format, for instance 80.1 represents inet port 80, and sub-port 1.
 * <p>
 * Client sockets do not support local port bindings for ports above 0xFFFF.
 * <p>
 * Sub-ports in the range of 1..1023 inclusive are considered to be "well known"
 * and are not for general use.  To make use of a sub port in this range, one
 * must be associated with a service and recorded in the {@link WellKnownSubPorts}
 * enum.  Generally applications will either use ephemeral sub-ports, or sub-ports
 * of 1024 or greater.
 * <p>
 * The MultiplexedSocketProvider also supports bindings to non-local NAT addresses.
 * Specifically if there is a NAT address which routes to a local address, it is allowable
 * to bind to NAT address using this provider.
 * </p>
 *
 * @see InetSocketAddress32
 *
 * @author mf  2010.12.27
 */
public class MultiplexedSocketProvider
        implements SocketProvider
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a MultiplexedSocketProvider.
     *
     * @param deps  the provider dependencies.
     */
    public MultiplexedSocketProvider(Dependencies deps)
        {
        m_dependencies = copyDependencies(deps).validate();
        }


    // ----- MultiplexedSocketProvider interface ----------------------------

    /**
     * Return the provider's dependencies.
     *
     * @return the provider's dependencies
     */
    public Dependencies getDependencies()
        {
        return m_dependencies;
        }

    // ----- SocketProvider interface ---------------------------------------

    /**
     * {@inheritDoc}
     * <p>
     * The address may be specified as is host:port, or host:base.sub where
     * the former uses an explicit 32 bit port, and the latter represents
     * the port as two 16 bit pairs from which a 32 bit port will be computed.
     */
    @Override
    public SocketAddress resolveAddress(String sAddr)
        {
        int ofPort;
        int ofAddrEnd;

        if (sAddr.startsWith("["))
            {
            // ipv6 formatted: [addr]:port
            ofAddrEnd = sAddr.lastIndexOf("]:") + 1;
            if (ofAddrEnd == 2) // 2 for []
                {
                throw new IllegalArgumentException("address does not contain an hostname or ip");
                }
            else if (ofAddrEnd == -1)
                {
                throw new IllegalArgumentException("address does not contain a port");
                }

            ofPort = ofAddrEnd + 1;
            }
        else
            {
            // ipv4 formatted: addr:port
            ofAddrEnd = sAddr.lastIndexOf(':');
            if (ofAddrEnd == 0)
                {
                throw new IllegalArgumentException("address does not contain an hostname of ip");
                }
            else if (ofAddrEnd == -1)
                {
                throw new IllegalArgumentException("address does not contain a port");
                }

            ofPort = ofAddrEnd + 1;
            }

        String sHost     = sAddr.substring(0, ofAddrEnd);
        int    ofPortSub = sAddr.indexOf('.', ofPort);
        if (ofPortSub == -1)
            {
            int nPort = Integer.parseInt(sAddr.substring(ofPort));

            return new InetSocketAddress32(sHost, nPort);
            }
        else
            {
            int nPortBase = Integer.parseInt(sAddr.substring(ofPort, ofPortSub));
            int nPortSub  = Integer.parseInt(sAddr.substring(ofPortSub + 1));

            return new InetSocketAddress32(sHost, getPort(nPortBase, nPortSub));
            }
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getAddressString(Socket socket)
        {
        InetAddress addr = socket.getInetAddress();
        if (addr == null)
            {
            return null;
            }

        // use host ip address
        String sAddr = addr.getHostAddress();
        if (sAddr.contains(":"))
            {
            // ipv6 representation
            sAddr = "[" + sAddr + "]";
            }

        int nPort = socket.getPort();
        return sAddr + ":" + (isPortExtended(nPort)
                     ? getBasePort(nPort) + "." + getSubPort(nPort)
                     : nPort);
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

            sAddr = addr.getHostName(); // Note: using addr.getCanonicalHostname generally returns an IP, thus defeating the purpose
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

        int nPort = socket.getLocalPort();
        return sAddr + ":" + (isPortExtended(nPort)
                     ? getBasePort(nPort) + "." + getSubPort(nPort)
                     : nPort);
        }


    /**
     * {@inheritDoc}
     */
    @Override
    public ServerSocketChannel openServerSocketChannel()
            throws IOException
        {
        return new MultiplexedServerSocketChannel(this);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public ServerSocket openServerSocket()
            throws IOException
        {
        return openServerSocketChannel().socket();
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public SocketChannel openSocketChannel()
            throws IOException
        {
        return new MultiplexedSocketChannel(getDependencies()
                .getDelegateProvider().openSocketChannel(), /*addrLocal*/ null, /*bufHeader*/ null);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public Socket openSocket()
            throws IOException
        {
        return new MultiplexedSocket(getDependencies().getDelegateProvider()
            .openSocket(), /*channel*/ null);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public SocketProvider getDelegate()
        {
        return getDependencies().getDelegateProvider();
        }

    // ----- inner class: MultiplexedSelectorProvider -----------------------

    /**
     * MultiplexedSelectorProvider provides a SelectorProvider interface to
     * this SocketProvider.
     */
    protected static class MultiplexedSelectorProvider
            extends SelectorProvider
        {
        // ----- constructors -------------------------------------------

        public MultiplexedSelectorProvider(SelectorProvider delegate)
            {
            m_delegate = delegate;
            }

        public MultiplexedSelectorProvider(SocketProvider providerSocket)
                throws IOException
            {
            ServerSocketChannel chan = providerSocket.openServerSocketChannel();
            m_delegate = chan.provider();
            chan.close();
            }

        // ----- SelectorProvider interface -----------------------------

        @Override
        public DatagramChannel openDatagramChannel()
                throws IOException
            {
            throw new UnsupportedOperationException();
            }

        @Override
        public DatagramChannel openDatagramChannel(ProtocolFamily family)
                throws IOException
            {
            throw new UnsupportedOperationException();
            }

        @Override
        public Pipe openPipe()
                throws IOException
            {
            throw new UnsupportedOperationException();
            }

        @Override
        public AbstractSelector openSelector()
                throws IOException
            {
            return new MultiplexedSelector(m_delegate.openSelector(), this);
            }

        @Override
        public ServerSocketChannel openServerSocketChannel()
                throws IOException
            {
            throw new UnsupportedOperationException();
            }

        @Override
        public SocketChannel openSocketChannel()
                throws IOException
            {
            throw new UnsupportedOperationException();
            }


        // ----- Object interface ---------------------------------------

        @Override
        public boolean equals(Object o)
            {
            if (o == this)
                {
                return true;
                }
            else if (o instanceof MultiplexedSelectorProvider)
                {
                return m_delegate.equals(((MultiplexedSelectorProvider) o).m_delegate);
                }
            else
                {
                return false;
                }
            }

        @Override
        public int hashCode()
            {
            return m_delegate.hashCode();
            }

        // ----- data members -------------------------------------------

        /**
         * The delegate SelectorProvider.
         */
        protected SelectorProvider m_delegate;
        }


    // ----- inner class: MultiplexedSelector -------------------------------

    /**
     * MultiplexedSelector is a Selector implementation for use with
     * Sockets produced by this provider.
     */
    protected static class MultiplexedSelector
            extends WrapperSelector
        {
        // ----- constructors -------------------------------------------

        /**
         * Construct a MultiplexedSelector.
         *
         * @param delegate  the delegate selector
         * @param provider  the corresponding SelectorProvider
         *
         * @throws IOException if an I/O error occurs
         */
        protected MultiplexedSelector(Selector delegate, SelectorProvider provider)
                throws IOException
            {
            super(delegate, provider);
            m_setKeysRO = new UnmodifiableSetCollection<SelectionKey>(
                    m_setKeys, super.keys());
            }

        // ----- Selector interface -------------------------------------

        @Override
        public Set<SelectionKey> keys()
            {
            Set<SelectionKey> setKeys = m_setKeysRO;
            ensureOpen();
            return setKeys;
            }

        @Override
        public Set<SelectionKey> selectedKeys()
            {
            Set<SelectionKey> setReady = m_setReady;
            ensureOpen();
            return setReady;
            }

        @Override
        public int selectNow()
                throws IOException
            {
            return select(-1);
            }

        @Override
        public synchronized int select(long cMillisTimeout)
                throws IOException
            {
            Set<SelectionKey> setKeys   = m_setKeys;
            Set<SelectionKey> setKeysRO = m_setKeysRO;
            Set<SelectionKey> setReady  = m_setReady;
            Set<SelectionKey> setPend   = m_setPending;
            Selector          delegate  = m_delegate;

            ensureOpen();

            synchronized (setKeysRO) // required by Selector doc
                {
                synchronized (setReady) // required by Selector doc
                    {
                    int cNew;
                    synchronized (setPend)
                        {
                        if (!m_setCancelled.isEmpty())
                            {
                            // we need to clear the canceled set, and ensure that any
                            // canceled keys are removed from setKeys and setPend.  Since
                            // the canceled set can be updated concurrently, we must do
                            // this a key at a time rather then via setCancel.clear(), this
                            // way we ensure we've don't leave orphans in setKeys or setPend
                            for (Iterator<SelectionKey> iter = m_setCancelled.iterator();
                                    iter.hasNext(); )
                                {
                                SelectionKey key = iter.next();
                                setKeys.remove(key);
                                setPend.remove(key);
                                iter.remove();
                                }
                            }
                        cNew = processPendingKeys();
                        }

                    try
                        {
                        if (cMillisTimeout >= 0 && cNew == 0)
                            {
                            Blocking.select(delegate, cMillisTimeout);
                            synchronized (setPend)
                                {
                                cNew = processPendingKeys();
                                }
                            }
                        else
                            {
                            delegate.selectNow();
                            }
                        }
                    finally
                        {
                        cleanupCancelledKeys();
                        }

                    Set<SelectionKey> setClient = delegate.selectedKeys();
                    for (SelectionKey key : setClient)
                        {
                        if (setReady.add((SelectionKey) key.attachment()))
                            {
                            ++cNew;
                            }
                        }
                    setClient.clear();

                    return cNew;
                    }
                }
            }

        @Override
        public int select()
                throws IOException
            {
            return select(0);
            }


        // ----- AbstractSelector interface -----------------------------

        @Override
        protected void implCloseSelector() throws IOException
            {
            Set<SelectionKey> setKeys   = m_setKeys;
            Set<SelectionKey> setKeysRO = m_setKeysRO;
            Set<SelectionKey> setReady  = m_setReady;
            super.implCloseSelector(); //let the thread in select return and release the locks
            synchronized(this)
                {
                synchronized (setKeysRO)
                    {
                    synchronized (setReady)
                        {
                        synchronized (setKeys)
                            {
                            for (Iterator<SelectionKey> iter = setKeys.iterator();
                                 iter.hasNext(); )
                                {
                                SelectionKey key = iter.next();
                                if (key.isValid())
                                    {
                                    key.cancel();
                                    }
                                iter.remove();
                                }
                            }
                        }
                    }
                }
            }

        @Override
        protected SelectionKey register(final AbstractSelectableChannel ch, int ops,
                Object att)
            {
            Set<SelectionKey> setKeys = m_setKeys;

            synchronized (setKeys) // ensures key can't be returned from selector without being visible in setKeys()
                {
                SelectionKey key;
                if (ch instanceof MultiplexedServerSocketChannel)
                    {
                    key = ((MultiplexedServerSocketChannel) ch).makeKey(this);

                    key.interestOps(ops);
                    key.attach(att);

                    setKeys.add(key);
                    }
                else
                    {
                    key = super.register(ch, ops, att);
                    }

                if (((MultiplexedChannel) ch).readyOps() != 0)
                    {
                    addPendingKey(key);
                    }

                return key;
                }
            }

        // ----- Object interface --------------------------------------

        /**
         * {@inheritDoc}
         */
        public String toString()
            {
            return "MultiplexedSelector(" + m_delegate + ")";
            }

        // ----- helper methods ----------------------------------------

        /**
         * Ensure that the Selector is open.
         */
        protected void ensureOpen()
            {
            if (!isOpen())
                {
                throw new ClosedSelectorException();
                }
            }

        /**
         * Add the ready SelectionKey to the pending set.
         *
         * @param key  the ready key
         */
        protected void addPendingKey(SelectionKey key)
            {
            Set<SelectionKey> set = m_setPending;
            synchronized (set)
                {
                set.add(key);
                }
            }

        /**
         * Update Selector ready set with ready keys added to the pending set
         * from the underlying Selector.
         *
         * @return number of new keys added to the readySet.
         */
        protected int processPendingKeys()
            {
            Set<SelectionKey> setPend  = m_setPending;
            Set<SelectionKey> setReady = m_setReady;
            int               cNew     = 0;

            if (!setPend.isEmpty())
                {
                for (Iterator<SelectionKey> iter = setPend.iterator(); iter.hasNext();)
                    {
                    SelectionKey key  = iter.next();
                    int          nOps = ((MultiplexedChannel) key.channel()).readyOps();
                    if (nOps == 0)
                        {
                        iter.remove();
                        }
                    else if ((key.interestOps() & nOps) != 0 && setReady.add(key))
                        {
                        ++cNew;
                        }
                    }
                }
            return cNew;
            }

        // ----- data members -------------------------------------------

        /**
         * The registered key set.
         */
        protected Set<SelectionKey> m_setKeys = new HashSet();

        /**
         * The exposed registered key set.
         */
        protected Set<SelectionKey> m_setKeysRO;

        /**
         * The ready key set.
         */
        protected Set<SelectionKey> m_setReady = new HashSet();

        /**
         * The pending ready key set.
         */
        protected Set<SelectionKey> m_setPending = new HashSet();

        /**
         * The cancelled key set.
         */
        protected Set<SelectionKey> m_setCancelled = Collections.newSetFromMap(new ConcurrentHashMap());
        }


    // ----- inner class: MultiplexedChannel --------------------------------

    /**
     * Common interface implemented by all channels serviced by this provider.
     */
    protected interface MultiplexedChannel
        {
        /**
         * Return the operations that can be satisfied by already buffered data.
         *
         * @return the operations that can be satisfied by already buffered data.
         */
        public int readyOps();
        }


    // ----- inner class: MultiplexedServerSocketChannel --------------------

    /**
     * MultiplexedServerSocketChannel is an implementation of a
     * ServerSocketChannel which shares an underlying ServerSocketChannel with
     * a number of other MultiplexedServerSocketChannels.
     */
    protected static class MultiplexedServerSocketChannel
            extends ServerSocketChannel
            implements MultiplexedChannel
        {
        // ----- constructors -------------------------------------------

        public MultiplexedServerSocketChannel(MultiplexedSocketProvider provider)
                throws IOException
            {
            super(new MultiplexedSelectorProvider(provider.getDependencies().getDelegateProvider()));

            m_provider = provider;
            m_socket   = createServerSocket();
            }

        protected ServerSocket createServerSocket()
            throws IOException
            {
            return new ServerSocket()
                {
                @Override
                public void bind(SocketAddress endpoint)
                        throws IOException
                    {
                    bind(endpoint, 0);
                    }

                @Override
                public void bind(SocketAddress endpoint, int backlog)
                        throws IOException
                    {
                    if (isBound())
                        {
                        throw new IOException("already bound");
                        }
                    else if (endpoint == null || endpoint instanceof InetSocketAddress32)
                        {
                        InetSocketAddress32 addr = (InetSocketAddress32) endpoint;

                        m_address = m_provider.open(addr, MultiplexedServerSocketChannel.this, null);
                        }
                    else
                        {
                        throw new IllegalArgumentException("unsupported SocketAddress type");
                        }
                    }

                @Override
                public InetAddress getInetAddress()
                    {
                    return m_address.getAddress();
                    }

                @Override
                public int getLocalPort()
                    {
                    return m_address.getPort();
                    }

                @Override
                public SocketAddress getLocalSocketAddress()
                    {
                    return m_address;
                    }

                @Override
                public ServerSocketChannel getChannel()
                    {
                    return MultiplexedServerSocketChannel.this;
                    }

                @Override
                public boolean isBound()
                    {
                    return m_address != null;
                    }

                @Override
                public boolean isClosed()
                    {
                    return m_fClosed;
                    }

                @Override
                public void close()
                        throws IOException
                    {
                    BlockingQueue<SocketChannel> queue = m_queue;
                    boolean                      fClosed;
                    synchronized (queue)
                        {
                        super.close(); // just to free underlying FD

                        fClosed   = m_fClosed;
                        m_fClosed = true;
                        }

                    if (!fClosed)
                        {
                        InetSocketAddress32 addr = (InetSocketAddress32) getLocalSocketAddress();
                        if (addr != null)
                            {
                            m_provider.close(addr);
                            }

                        for (ServerSelectionKey key = m_keyHead; key != null; key = key.m_next)
                            {
                            key.cancel();
                            }

                        // close all pending client channels in the queue, otherwise they will
                        // be leaked and could live a long time before bing GC'd and the other
                        // side sees the socket get closed
                        for (SocketChannel chan = queue.poll(); chan != null; chan = queue.poll())
                            {
                            try
                                {
                                chan.close();
                                }
                            catch (IOException ioe) {}
                            }
                        // Its possible that other threads might be doing blocking accept on the
                        // ServerSocketChannel. This marker channel allows them to unblock and
                        // identify that the ServerChannel has been closed.
                        queue.add(SERVER_CHANNEL_CLOSED_MARKER);
                        }
                    }

                @Override
                public Socket accept()
                        throws IOException
                    {
                    ServerSocketChannel chanServer = getChannel();
                    if (chanServer.isBlocking())
                        {
                        SocketChannel channel = chanServer.accept();
                        long          cMillis = chanServer.socket().getSoTimeout();

                        if (channel == null && cMillis > 0)
                            {
                            throw new SocketTimeoutException("SocketTimeout after configured SO_TIMEOUT " + cMillis + "ms");
                            }

                        return channel.socket();
                        }
                    throw new IllegalBlockingModeException();
                    }

                @Override
                public String toString()
                    {
                    if (isBound())
                        {
                        return "MultiplexedServerSocket[addr=" + m_address.getAddress() +
                                ",port=" + MultiplexedSocketProvider.getBasePort(m_address.getPort()) +
                                ",subport=" + MultiplexedSocketProvider.getSubPort(m_address.getPort())  + "]";
                        }
                    return "MultiplexedServerSocket[unbound]";
                    }

                InetSocketAddress32 m_address;
                };
            }


        protected ServerSocketChannel getChannel()
            {
            return this;
            }

        // ----- ServerSocketChannel interface --------------------------

        @Override
        public ServerSocket socket()
            {
            return m_socket;
            }

        @Override
        public SocketChannel accept()
                throws IOException
            {
            if (!socket().isBound())
                {
                throw new IOException("not bound");
                }

            try
                {
                BlockingQueue<SocketChannel> queue = m_queue;
                SocketChannel                chan;
                if (isBlocking())
                    {
                    long cMillis = socket().getSoTimeout();
                    chan = cMillis == 0
                            ? queue.take()
                            : queue.poll(cMillis, TimeUnit.MILLISECONDS);
                    }
                else
                    {
                    chan = queue.poll();
                    }

                if (chan == null)
                    {
                    return chan;
                    }
                else if (chan == SERVER_CHANNEL_CLOSED_MARKER)
                    {
                    //requeue to unblock other threads that might be blocked in accept()
                    queue.add(SERVER_CHANNEL_CLOSED_MARKER);
                    throw new IOException("socket closed");
                    }
                else
                    {
                    try
                        {
                        chan.socket().setReceiveBufferSize(socket().getReceiveBufferSize());
                        }
                    catch (IOException e)
                        {
                        // apparently the accepted socket has been closed; while we could try to
                        // pull another from the queue that would increase the complexity of this
                        // method, especially in the case of a timed wait.  So instead we return
                        // a closed socket, which is perfectly allowable
                        }
                    }

                return chan;
                }
            catch (InterruptedException e)
                {
                throw new InterruptedIOException(e.getMessage());
                }
            }

        @Override
        protected void implCloseSelectableChannel()
                throws IOException
            {
            socket().close();
            }

        @Override
        protected void implConfigureBlocking(boolean block)
                throws IOException
            {
            // nothing to do
            }

        @Override
        public ServerSocketChannel bind(SocketAddress local, int backlog)
                throws IOException
            {
            m_socket.bind(local, backlog);
            return this;
            }

        @Override
        public <T> ServerSocketChannel setOption(SocketOption<T> name, T value)
                throws IOException
            {
            if (name == StandardSocketOptions.SO_RCVBUF)
                {
                socket().setReceiveBufferSize(((Integer) value).intValue());
                }
            else if (name == StandardSocketOptions.SO_REUSEADDR)
                {
                socket().setReuseAddress(((Boolean) value).booleanValue());
                }
            else
                {
                throw new UnsupportedOperationException(name.toString());
                }

            return this;
            }

        // ----- NetworkChannel methods --------------------------------

        @Override
        public SocketAddress getLocalAddress()
                throws IOException
            {
            return socket().getLocalSocketAddress();
            }

        @Override
        public <T> T getOption(SocketOption<T> name)
                throws IOException
            {
            if (name == StandardSocketOptions.SO_RCVBUF)
                {
                return (T) Integer.valueOf(socket().getReceiveBufferSize());
                }
            else if (name == StandardSocketOptions.SO_REUSEADDR)
                {
                return (T) Boolean.valueOf(socket().getReuseAddress());
                }
            else
                {
                throw new UnsupportedOperationException(name.toString());
                }
            }

        @Override
        public Set<SocketOption<?>> supportedOptions()
            {
            return SERVER_OPTIONS;
            }

        // ----- helpers ------------------------------------------------

        /**
         * Add an SocketChannel to the accept queue
         *
         * @param chan  the channel
         *
         * @return true iff the channel was queued
         */
        protected boolean add(SocketChannel chan)
            {
            BlockingQueue<SocketChannel> queue = m_queue;
            synchronized (queue)
                {
                if (!m_fClosed && queue.offer(chan))
                    {
                    for (ServerSelectionKey key = m_keyHead; key != null; key = key.m_next)
                        {
                        MultiplexedSelector selectorMultiplexed = (MultiplexedSelector) key.selector();
                        selectorMultiplexed.addPendingKey(key);
                        selectorMultiplexed.wakeup();
                        }
                    return true;
                    }
                }
            return false;
            }

        /**
         * Register this channel with specified selector.
         *
         * @param selector  the selector to register with
         *
         * @return the selection key
         */
        protected SelectionKey makeKey(Selector selector)
            {
            ServerSelectionKey key = new ServerSelectionKey(selector);
            synchronized (m_queue)
                {
                key.m_next = m_keyHead;
                m_keyHead = key;
                }
            return key;
            }

        @Override
        public int readyOps()
            {
            return m_queue.isEmpty() ? 0 : SelectionKey.OP_ACCEPT;
            }

        @Override
        public String toString()
            {
            return "MultiplexedServerSocketChannel(" + socket() + ")";
            }


        // ---- inner class: ServerSelectionKey -------------------------

        class ServerSelectionKey
            extends SelectionKey
            {
            ServerSelectionKey(Selector selector)
                {
                m_selector = selector;
                }

            @Override
            public SelectableChannel channel()
                {
                return getChannel();
                }

            @Override
            public Selector selector()
                {
                return m_selector;
                }

            @Override
            public boolean isValid()
                {
                return !m_fCanceled;
                }

            @Override
            public void cancel()
                {
                m_fCanceled = true;
                ((MultiplexedSelector) m_selector).m_setCancelled.add(this);

                synchronized (MultiplexedServerSocketChannel.this.m_queue)
                    {
                    ServerSelectionKey keyLast = null;
                    for (ServerSelectionKey key = m_keyHead;
                         key != null; key = key.m_next)
                        {
                        if (key == this)
                            {
                            if (keyLast == null)
                                {
                                m_keyHead = m_next;
                                }
                            else
                                {
                                keyLast.m_next = m_next;
                                }
                            break;
                            }
                        keyLast = key;
                        }
                    }
                }

            @Override
            public int interestOps()
                {
                ensureValid();
                return m_nInterest;
                }

            @Override
            public SelectionKey interestOps(int ops)
                {
                ensureValid();
                if (ops == 0 || ops == OP_ACCEPT)
                    {
                    m_nInterest = ops;
                    return this;
                    }
                throw new IllegalArgumentException();
                }

            @Override
            public int readyOps()
                {
                // The only valid op for ServerSelectionKey is OP_ACCEPT.
                // The key will be returned in the MultiplexedSelector selected set
                // only if there is a pending socket for the MultiplexedServerSocketChannel
                // In that case, OP_ACCEPT is the readyOps.
                return OP_ACCEPT;
                }

            protected void ensureValid()
                {
                if (m_fCanceled)
                    {
                    throw new CancelledKeyException();
                    }
                }

            /**
             * The associated selector.
             */
            protected Selector m_selector;

            /**
             * True iff the key has been canceled.
             */
            protected boolean m_fCanceled;

            /**
             * The registered interest set.
             */
            protected int m_nInterest;

            /**
             * The next key associated with this channel.
             */
            protected ServerSelectionKey m_next;
            }

        // ----- data members -------------------------------------------

        /**
         * The queue of ready client channels.
         */
        protected final BlockingQueue<SocketChannel> m_queue = new LinkedBlockingDeque<SocketChannel>();

        /**
         * The ServerSocket representation of this channel.
         */
        protected ServerSocket m_socket;

        /**
         * The head of the SelectionKey linked-list.
         */
        protected ServerSelectionKey m_keyHead;

        /**
         * MultiplexedSocketProvider associated with this ServerSocketChannel
         */
        protected MultiplexedSocketProvider m_provider;

        /**
         * Flag indicating if the channel is closed.
         */
        protected boolean m_fClosed;

        /**
         * Special SocketChannel that is added to the client channel queue to
         * indicate that this ServerSocketChannel is closed. This is needed to
         * unblock threads waiting for client sockets in accept().
         */
        protected static final SocketChannel SERVER_CHANNEL_CLOSED_MARKER = new SocketChannel(null)
            {
            @Override
            public Socket socket()
                {
                return null;
                }

            @Override
            public boolean isConnected()
                {
                return false;
                }

            @Override
            public boolean isConnectionPending()
                {
                return false;
                }

            @Override
            public boolean connect(SocketAddress remote)
                    throws IOException
                {
                return false;
                }

            @Override
            public boolean finishConnect()
                    throws IOException
                {
                return false;
                }

            @Override
            public int read(ByteBuffer dst)
                    throws IOException
                {
                return 0;
                }

            @Override
            public long read(ByteBuffer[] dsts, int offset, int length)
                    throws IOException
                {
                return 0;
                }

            @Override
            public int write(ByteBuffer src)
                    throws IOException
                {
                return 0;
                }

            @Override
            public long write(ByteBuffer[] srcs, int offset, int length)
                    throws IOException
                {
                return 0;
                }

            @Override
            protected void implCloseSelectableChannel()
                    throws IOException
                {
                }

            @Override
            protected void implConfigureBlocking(boolean block)
                    throws IOException
                {
                }

            @Override
            public SocketChannel bind(SocketAddress local)
                    throws IOException
                {
                return null;
                }

            @Override
            public <T> SocketChannel setOption(SocketOption<T> name, T value)
                    throws IOException
                {
                return null;
                }

            @Override
            public SocketChannel shutdownInput()
                    throws IOException
                {
                return null;
                }

            @Override
            public SocketChannel shutdownOutput()
                    throws IOException
                {
                return null;
                }

            @Override
            public SocketAddress getRemoteAddress()
                    throws IOException
                {
                return null;
                }

            @Override
            public SocketAddress getLocalAddress()
                    throws IOException
                {
                return null;
                }

            @Override
            public <T> T getOption(SocketOption<T> name)
                    throws IOException
                {
                return null;
                }

            @Override
            public Set<SocketOption<?>> supportedOptions()
                {
                return null;
                }
            };
        }


    // ----- inner class: MultiplexedSocketChannel --------------------------

    /**
     * MultiplexedSocketChannel
     */
    protected static class MultiplexedSocketChannel extends WrapperSocketChannel implements MultiplexedChannel
        {
        // ----- constructors -------------------------------------------

        /**
         * Create a MultiplexedSocketChannel for an incoming SocketChannel
         *
         * @param delegate   incoming socket channel delegate
         * @param addrLocal  the local address associated with this socket, or null
         * @param bufIn      initial bytes to be returned from read calls before socket data, or null
         */
        public MultiplexedSocketChannel(SocketChannel delegate, SocketAddress addrLocal, ByteBuffer bufIn)
            {
            super(delegate, new MultiplexedSelectorProvider(delegate.provider()));

            // Note: addrLocal is generally an InetSocketAddress32, the only exception is in the case that
            // we are accepting from a DemultiplexedServerSocket in which case the supplied address is just
            // an InetSocketAddress, which we ignore
            m_addrLocal = addrLocal instanceof InetSocketAddress32
                    ? (InetSocketAddress32) addrLocal
                    : null;
            m_bufHeaderIn = bufIn;
            }


        // ----- WrapperSocketChannel methods ---------------------------

        /**
         * Return the delegate channel
         *
         * @return the delegate channel
         */
        protected SocketChannel delegate()
            {
            return f_delegate;
            }

        /**
         * Produce a wrapper around the specified socket.
         *
         * @param socket  the socket to wrap
         * @return the wrapper socket
         */
        protected Socket wrapSocket(Socket socket)
            {
            return new MultiplexedSocket(socket, this);
            }

        // ----- SocketChannel methods ----------------------------------

        @Override
        public boolean isConnected()
            {
            return super.isConnected() && m_bufHeaderOut == null;
            }

        @Override
        public boolean isConnectionPending()
            {
            return super.isConnectionPending() || m_bufHeaderOut != null;
            }

        @Override
        public boolean connect(SocketAddress remote)
                throws IOException
            {
            if (!(remote instanceof InetSocketAddress32))
                {
                throw new IllegalArgumentException("unsupported SocketAddress type");
                }

            InetSocketAddress32 addrPeer = (InetSocketAddress32) remote;
            if (addrPeer.isUnresolved())
                {
                throw new UnresolvedAddressException();
                }

            int nPort = addrPeer.getPort();
            boolean fConnected = super.connect(getTransportAddress(addrPeer));

            m_addrPeer = addrPeer;

            if (isPortExtended(nPort))
                {
                ByteBuffer buf = m_bufHeaderOut = ByteBuffer.allocate(8);
                buf.putInt(PROTOCOL_ID).putInt(getSubPort(nPort)).flip();

                return finishConnect();
                }
            else
                {
                return fConnected;
                }
            }

        @Override
        public boolean finishConnect()
                throws IOException
            {
            boolean fResult = super.finishConnect();
            if (fResult)
                {
                ByteBuffer buf = m_bufHeaderOut;
                if (buf != null)
                    {
                    delegate().write(buf);
                    if (buf.hasRemaining())
                        {
                        LOGGER.log(Level.FINEST,
                                   "{0} physical connection established, {2} of multiplexed" + " protocol header pending for logical connection to be established",
                                   new Object[]{this, buf.remaining()});
                        return false;
                        }

                    LOGGER.log(Level.FINEST, "{0} multiplexed connection established", new Object[]{this});
                    m_bufHeaderOut = null;
                    }
                }

            return fResult;
            }


        @Override
        public int write(ByteBuffer src)
                throws IOException
            {
            return m_bufHeaderOut == null || finishConnect()
                    ? super.write(src)
                    : 0;
            }

        @Override
        public long write(ByteBuffer[] srcs, int offset, int length)
                throws IOException
            {
            return m_bufHeaderOut == null || finishConnect()
                    ? super.write(srcs, offset, length)
                    : 0;
            }

        @Override
        public int read(ByteBuffer dst)
                throws IOException
            {
            if (m_bufHeaderIn == null)
                {
                return super.read(dst);
                }
            else if (socket().isClosed())
                {
                throw new ClosedChannelException();
                }
            else
                {
                return readHeader(new ByteBuffer[]{dst}, 0, 1);
                }
            }

        @Override
        public long read(ByteBuffer[] dsts, int offset, int length)
                throws IOException
            {
            if (m_bufHeaderIn == null)
                {
                return super.read(dsts, offset, length);
                }
            else if (socket().isClosed())
                {
                throw new ClosedChannelException();
                }
            else
                {
                return readHeader(dsts, offset, length);
                }
            }

        @Override
        protected void implCloseSelectableChannel()
                throws IOException
            {
            super.implCloseSelectableChannel();
            m_bufHeaderIn = null;
            }

        @Override
        public int readyOps()
            {
            return m_bufHeaderIn == null
                    ? 0
                    : SelectionKey.OP_READ;
            }

        @Override
        public String toString()
            {
            return "MultiplexedSocketChannel(" + socket() + ")";
            }

        // ----- helpers -----------------------------------------------

        /**
         * Transfer as many bytes as possible from the inbound header buffer to the supplied buffer
         *
         * @param aBufDst  the destination buffers
         * @param offset   the starting offset to write into
         * @param length   the maximum number of output buffers to access
         *
         * @return the number of bytes transferred
         */
        protected int readHeader(ByteBuffer[] aBufDst, int offset, int length)
            {
            ByteBuffer bufIn = m_bufHeaderIn;
            int cbIn = bufIn.remaining();
            int cb = 0;
            for (int i = 0; i < length && cbIn > 0; ++i)
                {
                ByteBuffer bufOut = aBufDst[offset + i];
                int cbOut = bufOut.remaining();

                for (int j = 0, c = Math.min(cbOut, cbIn); j < c; ++j)
                    {
                    bufOut.put(bufIn.get());
                    ++cb;
                    --cbIn;
                    }
                }

            if (cbIn == 0)
                {
                m_bufHeaderIn = null; // header has been transferred
                }

            return cb;
            }

        @Override
        public WrapperSelector.WrapperSelectionKey registerInternal(WrapperSelector selector, int ops, Object att)
                throws IOException
            {
            WrapperSelector.WrapperSelectionKey key = new SocketSelectionKey(selector, f_delegate
                    .register(selector.getDelegate(), 0), att);
            key.interestOps(ops);
            return key;
            }

        /**
         * SelectionKey which is aware of the state of the channel's inbound buffer.
         */
        protected class SocketSelectionKey extends WrapperSelector.WrapperSelectionKey
            {
            public SocketSelectionKey(WrapperSelector selector, SelectionKey key, Object att)
                {
                super(selector, key, att);
                }

            @Override
            public SelectableChannel channel()
                {
                return MultiplexedSocketChannel.this;
                }

            @Override
            public SelectionKey interestOps(int ops)
                {
                // handle the case where we've connected the underlying socket but not yet pushed the protocol
                // header. In such as case we tell the user that we're not connected, thus they express OP_CONNECT
                // interest, but what we really need is OP_WRITE interest
                boolean fConnecting = m_bufHeaderOut != null &&  // logical connection is still pending
                        !delegate().isConnectionPending() &&  // physical connection is complete
                        (ops & OP_CONNECT) != 0 &&  // app expressed interest in OP_CONNECT
                        (ops & OP_WRITE) == 0;              // didn't ask for OP_WRITE

                super.interestOps(fConnecting
                                          ? (ops | OP_WRITE) & ~OP_CONNECT
                                          // add in OP_WRITE and remove OP_CONNECT, since physical connect is complete
                                          : ops);

                m_nOpsInterest = ops;

                return this;
                }

            @Override
            public int interestOps()
                {
                return m_nOpsInterest;
                }

            @Override
            public int readyOps()
                {
                // TODO: this is a little broken, as it relies on the current value of
                // interestOps rather then the value used during the last call into the
                // selector.
                return (super.readyOps() | MultiplexedSocketChannel.this.readyOps()) & interestOps();
                }

            @Override
            public String toString()
                {
                return "MultiplexedSocketChannel{" + delegate() + "}";
                }

            /**
             * The cached interest ops.
             */
            protected int m_nOpsInterest;
            }

        // ----- data members ------------------------------------------

        /**
         * The peer's address.
         */
        protected InetSocketAddress32 m_addrPeer;

        /**
         * The socket's local address.
         */
        protected InetSocketAddress32 m_addrLocal;

        /**
         * The outbound protocol header.
         */
        protected ByteBuffer m_bufHeaderOut;

        /**
         * The inbound protocol header, or more specifically bytes which were read looking for the header but
         * need to be returned from socket read calls before any further socket data.
         */
        protected ByteBuffer m_bufHeaderIn;
        }

    // ----- inner class: MultiplexedSocket ------------------------------

    /**
     * MultiplexedSocket is an implementation of a Socket that works with
     * multiplexed socket addresses represented by InetSocketAddress32.
     */
    protected static class MultiplexedSocket extends WrapperSocket
        {
        // ----- constructors -------------------------------------------

        /**
         * Construct a MultiplexedSocket
         *
         * @param delegate  underlying delegate socket
         * @param channel   SocketChannel to be associated with the MultiplexedSocket.
         *                  Could be null for an outbound socket.
         */
        public MultiplexedSocket(Socket delegate, MultiplexedSocketChannel channel)
            {
            super(delegate);
            f_channel = channel;
            if (channel == null)
                {
                f_out = null;
                f_in  = null;
                }
            else
                {
                f_out = new SocketChannelOutputStream(f_channel);
                f_in  = new SocketChannelInputStream(f_channel);
                }
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public SocketChannel getChannel()
            {
            return f_channel;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public void connect(SocketAddress addr)
                throws IOException
            {
            connect(addr, 0);
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public void connect(SocketAddress addr, int cMillis)
                throws IOException
            {
            if (!(addr instanceof InetSocketAddress32))
                {
                throw new IllegalArgumentException("unsupported SocketAddress type");
                }

            InetSocketAddress32 addr32 = (InetSocketAddress32) addr;
            super.connect(getTransportAddress(addr32), cMillis);
            m_addrPeer = addr32;

            if (isPortExtended(addr32.getPort()))
                {
                ByteBuffer buff = ByteBuffer.allocate(8);
                buff.putInt(PROTOCOL_ID).putInt(getSubPort(addr32.getPort())).flip();

                getOutputStream().write(buff.array());
                getOutputStream().flush();
                }
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public void bind(SocketAddress addr)
                throws IOException
            {
            if (addr == null || addr instanceof InetSocketAddress32)
                {
                InetSocketAddress32 addrBind = (InetSocketAddress32) addr;
                if (addrBind != null)
                    {
                    int nSub = getSubPort(addrBind.getPort());
                    if (nSub != 0 && nSub != -1)
                        {
                        throw new IOException("cannot bind client sockets to non-zero sub-ports");
                        }
                    }

                if (addrBind == null)
                    {
                    super.bind(null);
                    addrBind = new InetSocketAddress32(super.getLocalAddress(), super.getLocalPort());
                    }
                else
                    {
                    super.bind(getTransportAddress(addrBind));
                    }
                m_addrLocal = addrBind;
                }
            else
                {
                throw new IllegalArgumentException("unsupported SocketAddress type");
                }
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public SocketAddress getLocalSocketAddress()
            {
            InetSocketAddress32 addr = m_addrLocal;
            if (addr == null)
                {
                if (f_channel != null)
                    {
                    // in the case of an accepted channel we need to use the server socket's address
                    addr = m_addrLocal = f_channel.m_addrLocal;
                    }

                if (addr == null)
                    {
                    InetSocketAddress addrReal = (InetSocketAddress) super.getLocalSocketAddress();
                    if (addrReal != null)
                        {
                        addr = m_addrLocal = new InetSocketAddress32(addrReal.getAddress(), addrReal.getPort());
                        }
                    }
                }
            return addr;
            }

        @Override
        public InetAddress getLocalAddress()
            {
            InetSocketAddress32 addr = (InetSocketAddress32) getLocalSocketAddress();
            return addr == null ? InetAddresses.ADDR_ANY : addr.getAddress();
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getLocalPort()
            {
            InetSocketAddress32 addr = (InetSocketAddress32) getLocalSocketAddress();
            return addr == null
                    ? -1
                    : addr.getPort();
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public SocketAddress getRemoteSocketAddress()
            {
            InetSocketAddress32 addr = m_addrPeer;
            if (addr == null)
                {
                // compute from delegate socket
                if (f_channel != null)
                    {
                    // in the case this is a channel associated socket, use its address info
                    addr = m_addrPeer = f_channel.m_addrPeer;
                    }

                if (addr == null)
                    {
                    // compute from delegate socket
                    InetSocketAddress addrReal = (InetSocketAddress) super.getRemoteSocketAddress();
                    if (addrReal != null)
                        {
                        addr = m_addrPeer = new InetSocketAddress32(addrReal.getAddress(), addrReal.getPort());
                        }
                    }
                }
            return addr;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getPort()
            {
            InetSocketAddress32 addr = (InetSocketAddress32) getRemoteSocketAddress();
            return addr == null
                    ? 0
                    : addr.getPort();
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isInputShutdown()
            {
            MultiplexedSocketChannel chan = f_channel;
            return chan == null
                    ? super.isInputShutdown()
                    : chan.m_bufHeaderIn == null && super.isInputShutdown();
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public void shutdownInput()
                throws IOException
            {
            super.shutdownInput();

            MultiplexedSocketChannel chan = f_channel;
            if (chan != null)
                {
                chan.m_bufHeaderIn = null;
                }
            }

        @Override
        public InputStream getInputStream()
                throws IOException
            {
            return f_in == null ? super.getInputStream() : f_in;
            }

        @Override
        public OutputStream getOutputStream()
                throws IOException
            {
            return f_out == null ? super.getOutputStream() : f_out;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public void close()
                throws IOException
            {
            super.close();

            MultiplexedSocketChannel chan = f_channel;
            if (chan != null)
                {
                chan.m_bufHeaderIn = null;
                }
            }

        @Override
        public String toString()
            {
            return "MultiplexedSocket{" + super.toString() + "}";
            }

        /**
         * Associated Socket channel. Could be null for an outbound socket.
         */
        protected final MultiplexedSocketChannel f_channel;

        /**
         * OutuputStream for channel based sockets.
         */
        protected final SocketChannelOutputStream f_out;

        /**
         * InputStream for channel based sockets.
         */
        protected final SocketChannelInputStream f_in;

        /**
         * Local address
         */
        protected InetSocketAddress32 m_addrLocal;

        /**
         * Peer address
         */
        protected InetSocketAddress32 m_addrPeer;
        }


    // ----- helpers --------------------------------------------------------

    /**
     * Start listening for connections on the specified address.
     *
     * @param addr            the address to listen on
     * @param server          the server socket
     * @param setBaseExclude  the set of base ports to exclude during ephemeral bind, or null
     *
     * @return the bound address
     *
     * @throws IOException on an I/O error
     */
    protected InetSocketAddress32 open(InetSocketAddress32 addr, MultiplexedServerSocketChannel server, Set<Integer> setBaseExclude)
            throws IOException
        {
        if (addr == null)
            {
            addr = new InetSocketAddress32(InetAddress.getLocalHost(), 0);
            }
        else if (addr.isUnresolved())
            {
            throw new SocketException(addr.getHostName());
            }

        InetSocketAddress addrListen = getTransportAddress(addr);
        InetAddress addrIp = addr.getAddress();
        int nPort = addr.getPort();
        ServerSocket socket = server.socket();

        int nPortSub = getSubPort(nPort);
        if (nPortSub > 0 && nPortSub <= WELL_KNOWN_SUB_PORT_END)
            {
            // ensure that the sub port has been registered as a WellKnownSubPort.  This is meant to prevent
            // accidental usage of the space by apps which have not had a well known port assigned to them.
            // this is not a security feature.
            boolean fKnown = false;
            for (WellKnownSubPorts port : WellKnownSubPorts.values())
                {
                if (port.getSubPort() == nPortSub)
                    {
                    fKnown = true;
                    break;
                    }
                }
            if (!fKnown)
                {
                throw new IOException(
                        "attempt to bind to unassigned sub-port " + nPortSub + " in well known subport range");
                }
            }

        // handle NAT addresses
        if (addrIp != null && InetAddresses.isNatLocalAddress(addrIp, nPort))
            {
            addrIp = InetAddresses.ADDR_ANY;
            }

        // handle the case where doing a multiplexed binding to all IPs
        if (addrIp != null && addrIp.isAnyLocalAddress())
            {
            boolean fEphemeral = getSubPort(nPort) == 0 /*ephemeral sub*/ || getBasePort(nPort) == 0 /*ephemeral base*/;

            // manually add a listener for each IP in the system
            Set<InetSocketAddress32> setAddrAcquired = new HashSet<>();
            Set<InetAddress>         setIpAll        = new TreeSet<>(InetAddressComparator.INSTANCE); // ordered to avoid "deadlock" in case of concurrent process bind

            // COH-12612: the IPv6 zone ID can be mangled on some operating systems (e.g. OS X).
            // skip over duplicate addresses to avoid "address already in use" errors.
            for (Enumeration<NetworkInterface> iter = NetworkInterface.getNetworkInterfaces(); iter
                    .hasMoreElements(); )
                {
                NetworkInterface nic = iter.nextElement();
                for (Enumeration<InetAddress> iterAddr = nic.getInetAddresses(); iterAddr.hasMoreElements(); )
                    {
                    setIpAll.add(iterAddr.nextElement());
                    }

                for (Enumeration<NetworkInterface> iterSub = nic.getSubInterfaces(); iterSub.hasMoreElements(); )
                    {
                    for (Enumeration<InetAddress> iterAddr = iterSub.nextElement().getInetAddresses(); iterAddr
                            .hasMoreElements(); )
                        {
                        setIpAll.add(iterAddr.nextElement());
                        }
                    }
                }

            try
                {
                for (Iterator<InetAddress> iter = setIpAll.iterator(); iter.hasNext(); )
                    {
                    InetAddress addrNext = iter.next();
                    if (!addrNext.isAnyLocalAddress()) // Solaris lists wildcard as one of the local IPs
                        {
                        try
                            {
                            // re-enter for a single IP
                            InetSocketAddress32 addrAdd = open(new InetSocketAddress32(addrNext, nPort), server, setBaseExclude);
                            setAddrAcquired.add(addrAdd);
                            if (getSubPort(nPort) == 0 || getBasePort(nPort) == 0)
                                {
                                // first acquisition from ephemeral; switch to
                                // actual for the remainder of this pass
                                nPort = addrAdd.getPort();
                                }
                            }
                        catch (IOException e)
                            {
                            // this may be a non-bindable IP, such as IPv6 temporary IPs rather then a port
                            // conflict.  Validate by seeing by testing for port conflict

                            try
                                {
                                close(open(new InetSocketAddress32(addrNext, 0), server, setBaseExclude));
                                }
                            catch (IOException e2)
                                {
                                // this must be an outbound only IP so we shouldn't have attempted to bind it
                                continue;
                                }

                            // if we get here then apparently the original port was simply not available
                            if (fEphemeral && nPort != addr.getPort())
                                {
                                // since we're emulating a ephemeral binding on wildcard it's possible that the
                                // actual ephemeral allocation on the first real IP isn't actually available
                                // on one of the subsequent IPs

                                // Note: we hold onto the ports we've already acquired, not because we'll use
                                // them but because we don't want to get them again and keep failing

                                nPort = addr.getPort();      // try again
                                iter  = setIpAll.iterator(); // from the beginning

                                if (setBaseExclude == null)
                                    {
                                    setBaseExclude = new HashSet<>();
                                    }

                                // avoid this port on future binds, this is necessary in case of a double ephemeral
                                // binding, we'd endlessly reslect the same base port and fail forever
                                setBaseExclude.add(getBasePort(nPort));
                                }
                            else
                                {
                                throw e;
                                }
                            }
                        }
                    }

                // clear out the addresses we're going to keep so they don't get freed in the finally
                // leaving just those temporary ephemeral bindings which we were not able to acquire
                // on all IPs
                for (Iterator<InetSocketAddress32> iter = setAddrAcquired.iterator(); iter.hasNext(); )
                    {
                    InetSocketAddress32 addr32 = iter.next();
                    if (addr32.getPort() == nPort)
                        {
                        iter.remove();
                        }
                    }

                return fEphemeral
                    ? new InetSocketAddress32(addr.getAddress(), nPort)
                    : addr;
                }
            finally // clean up any bindings we aren't going to keep
                {
                for (InetSocketAddress32 addrDrop : setAddrAcquired)
                    {
                    try
                        {
                        close(addrDrop);
                        }
                    catch (IOException e) {}
                    }
                }
            }

        int nPortEphemeralLow = m_nPortEphemeralLow;
        int nPortEphemeralHi = m_nPortEphemeralHi;

        // handle the case where it is a double ephemeral address
        ConcurrentMap<InetSocketAddress, Listener> mapListener = m_mapListener;
        if (getBasePort(nPort) == 0 && !mapListener.isEmpty())
            {
            // select any multiplexed listener
            for (Map.Entry<InetSocketAddress, Listener> entry : mapListener.entrySet())
                {
                int nPortUsed = entry.getKey().getPort();
                if ((setBaseExclude == null || setBaseExclude.contains(nPortUsed)) &&
                        nPortUsed >= nPortEphemeralLow &&
                        nPortUsed <= nPortEphemeralHi &&
                        entry.getKey().getAddress().equals(addrIp))
                    {
                    try
                        {
                        // re-enter for each possible port
                        return open(new InetSocketAddress32(addrIp, getPort(nPortUsed, getSubPort(nPort))),
                                    server, null);
                        }
                    catch (IOException e)
                        {
                        }
                    }
                }
            }

        // handle the basic case for a single IP
        ServerSocketChannel chanGarbage = null;
        while (true)
            {
            Listener listener = mapListener.get(addrListen);

            if (listener == null)
                {
                // try to bind to the address
                ServerSocketChannel chanListen = new ListenChannel(
                        getDependencies().getDelegateProvider().openServerSocketChannel());
                try
                    {
                    ServerSocket socketListen = chanListen.socket();

                    socketListen.setReceiveBufferSize(socket.getReceiveBufferSize());
                    socketListen.bind(addrListen, getDependencies().getBacklog());

                    if (getBasePort(nPort) == 0)
                        {
                        // "learn" the native ephemeral base port range as we bind to real ephemeral ports
                        int nPortBind = chanListen.socket().getLocalPort();
                        if (nPortBind == 65535)
                            {
                            // we don't want this ephemeral port since we would not be able to bind sub-ports to it;
                            // retry while holding it to obtain a different ephemeral base.
                            chanGarbage = chanListen; // will be closed after next bind
                            continue;
                            }
                        else if (nPortBind < nPortEphemeralLow)
                            {
                            m_nPortEphemeralLow = nPortBind;
                            }
                        if (nPortBind > nPortEphemeralHi)
                            {
                            m_nPortEphemeralHi = nPortBind;
                            }

                        addrListen = (InetSocketAddress) chanListen.socket().getLocalSocketAddress();
                        }

                    chanListen.configureBlocking(false);
                    listener = new Listener(chanListen);

                    getDependencies().getSelectionService().register(chanListen, listener);
                    mapListener.put(addrListen, listener);
                    }
                catch (IOException e)
                    {
                    chanListen.close();
                    if (chanGarbage != null)
                        {
                        chanGarbage.close();
                        }
                    throw e;
                    }
                }

            if (chanGarbage != null)
                {
                chanGarbage.close();
                chanGarbage = null;
                }

            synchronized (listener)
                {
                ServerSocketChannel chanListen = listener.getChannel();
                if (chanListen.isOpen())
                    {
                    return listener.register(getSubPort(nPort), server);
                    }
                }
            }
        }

    /**
     * Stop listening for connections on the specified address.
     *
     * @param addr  the address to stop listening on
     *
     * @throws IOException on an I/O error
     */
    protected void close(InetSocketAddress32 addr)
            throws IOException
        {
        InetAddress addrIp = addr.getAddress();
        int nPort = addr.getPort();

        if (addrIp != null && addrIp.isAnyLocalAddress())
            {
            // manually remove a listener for each IP in the system
            for (Enumeration<NetworkInterface> iter = NetworkInterface.getNetworkInterfaces(); iter.hasMoreElements(); )
                {
                for (Enumeration<InetAddress> iterAddr = iter.nextElement().getInetAddresses(); iterAddr
                        .hasMoreElements(); )
                    {
                    try
                        {
                        InetAddress addrIface = iterAddr.nextElement();
                        // COH-17162: uniquely Solaris IPMP permits having an any local address
                        // within the network interface
                        if (!addrIface.isAnyLocalAddress())
                            {
                            close(new InetSocketAddress32(addrIface, addr.getPort()));
                            }
                        }
                    catch (IOException e)
                        {
                        }
                    }
                }
            return;
            }

        ConcurrentMap<InetSocketAddress, Listener> mapListener = m_mapListener;

        SocketAddress addrListen = getTransportAddress(addr);
        Listener listener = mapListener.get(addrListen);

        if (listener == null)
            {
            throw new IOException("not bound");
            }
        else
            {
            synchronized (listener)
                {
                if (listener.deregister(getSubPort(nPort)))
                    {
                    listener.getChannel().close();
                    mapListener.remove(addrListen);
                    }
                }
            }
        }

    // ----- inner class: Listener ------------------------------------------

    /**
     * Listener is a SelectionHandler which waits on the real
     * ServerSocketChannel for new connections.
     */
    protected class Listener extends SafeSelectionHandler<ServerSocketChannel>
        {
        public Listener(ServerSocketChannel chan)
                throws IOException
            {
            super(chan);
            }

        @Override
        protected int onReadySafe(int nOps)
                throws IOException
            {
            SelectionService svc = getDependencies().getSelectionService();
            SocketChannel chan;

            while ((chan = getChannel().accept()) != null)
                {
                try
                    {
                    chan.configureBlocking(false);
                    svc.register(chan, new Switcher(chan));
                    }
                catch (IOException e)
                    {
                    // accepted chan is no longer usable
                    try
                        {
                        chan.close();
                        }
                    catch (IOException e2)
                        {
                        }
                    }
                }

            return OP_ACCEPT;
            }

        @Override
        protected int onException(Throwable t)
            {
            if (getChannel().isOpen())
                {
                LogRecord rec = new LogRecord(Level.WARNING, "unhandled exception; continuing");
                rec.setThrown(t);
                getDependencies().getLogger().log(rec);
                return OP_ACCEPT;
                }
            // else in shutdown; ignore
            return 0;
            }

        /**
         * Register an acceptor queue with a sub-port on this listener
         *
         * @param nPortSub  the sub-port, or -1 for standard port binding
         * @param server    the server socket
         *
         * @throws IOException on registration failure
         *
         * @return the bound address
         */
        protected InetSocketAddress32 register(int nPortSub, MultiplexedServerSocketChannel server)
                throws IOException
            {
            ConcurrentNavigableMap<Integer, MultiplexedServerSocketChannel> map = m_mapBindings;

            ServerSocket socket = getChannel().socket();

            if (nPortSub == 0)
                {
                // first try a "random" port
                nPortSub = EPHEMERAL_SUB_PORT_START + (System
                        .identityHashCode(server) % (0xFFFF - EPHEMERAL_SUB_PORT_START));
                if (map.putIfAbsent(nPortSub, server) == null)
                    {
                    // port acquired
                    return new InetSocketAddress32(socket.getInetAddress(), getPort(socket.getLocalPort(), nPortSub));

                    }

                // scan through and find a free port
                nPortSub = 0xFFFF;
                for (Iterator<Integer> iter = m_setEphemeral.iterator(); nPortSub >= EPHEMERAL_SUB_PORT_START; )
                    {
                    int nPortUsed = Math.max(EPHEMERAL_SUB_PORT_START - 1, iter.hasNext()
                            ? iter.next()
                            : 0);

                    for (; nPortSub > nPortUsed; --nPortSub)
                        {
                        // nPortSub is free
                        if (map.putIfAbsent(nPortSub, server) == null)
                            {
                            // port acquired
                            return new InetSocketAddress32(socket.getInetAddress(),
                                                           getPort(socket.getLocalPort(), nPortSub));
                            }
                        }
                    nPortSub = nPortUsed - 1;
                    }

                throw new IOException("no available ephemeral sub-ports within base port " + socket.getLocalPort());
                }
            else if (map.putIfAbsent(nPortSub, server) != null)
                {
                throw new IOException("address already in use: " + getAddressString(socket) + '.' + nPortSub);
                }

            return new InetSocketAddress32(socket.getInetAddress(), getPort(socket.getLocalPort(), nPortSub));
            }

        /**
         * Deregister an acceptor.
         *
         * @param nPortSub  the sub-port
         *
         * @return true iff this was the last registered port
         *
         * @throws IOException on deregistration failure
         */
        protected boolean deregister(int nPortSub)
                throws IOException
            {
            ConcurrentMap<Integer, MultiplexedServerSocketChannel> map = m_mapBindings;

            if (map.remove(nPortSub) == null)
                {
                throw new IOException("not bound");
                }

            return map.isEmpty();
            }

        // ----- inner class: Switcher ----------------------------------

        /**
         * Switcher handles the initial protocol header from new connections.
         */
        public class Switcher extends SafeSelectionHandler<SocketChannel>
            {
            public Switcher(SocketChannel chan)
                {
                super(chan);
                }

            @Override
            protected int onReadySafe(int nOps)
                    throws IOException
                {
                final SocketChannel chan = getChannel();
                final ByteBuffer buf = m_buf;
                if (chan.read(buf) < 0)
                    {
                    // socket closed before, process based on what has been read thus far
                    accept();
                    return 0;
                    }

                // check for multiplexed protocol header
                boolean fStandard = false;
                switch (buf.position())
                    {
                    default:
                        fStandard |= buf.get(3) != (byte) PROTOCOL_ID;
                    case 3:
                        fStandard |= buf.get(2) != (byte) (PROTOCOL_ID >>> 8);
                    case 2:
                        fStandard |= buf.get(1) != (byte) (PROTOCOL_ID >>> 16);
                    case 1:
                        fStandard |= buf.get(0) != (byte) (PROTOCOL_ID >>> 24);
                    case 0:
                        break;
                    }

                Map<Integer, MultiplexedServerSocketChannel> mapBindings = m_mapBindings;
                if (fStandard ||                                            // non-multiplexed header bytes
                        !buf.hasRemaining() ||                                  // enough bytes to accept if multiplexed
                        mapBindings.containsKey(-1) && mapBindings.size() == 1) // no sub-port listeners
                    {
                    accept();
                    return 0;
                    }
                else if (m_timer == null)
                    {
                    // this only occurs on our initial registration, i.e. first pass
                    // if we failed to accept on first pass setup a timer to route this
                    // as a "standard" socket if we fail to accept within timeout
                    // Note: we do this here to avoid registering the non-cancelable timer
                    // if we don't have to
                    final Dependencies deps = getDependencies();
                    Runnable timer = m_timer = new Runnable()
                    {
                    public void run()
                        {
                        if (isPending())
                            {
                            // timeout; to be here we either receive nothing at all on the connection; or we've received
                            // only a partial protocol header
                            try
                                {
                                // perform one final read attempt
                                switch (chan.read(buf))
                                    {
                                    case -1: // socket had actually been closed, but we hadn't detected it
                                        LOGGER.log(Level.WARNING,
                                                   "{0} handling delayed close of accepted connection from {1} after {2} ms",
                                                   new Object[]{Listener.this.getChannel().socket()
                                                           .getLocalSocketAddress(), chan.socket()
                                                           .getRemoteSocketAddress(), deps
                                                           .getIdentificationTimeoutMillis()});
                                        break;

                                    case 0: // "expected" result
                                        LOGGER.log(Level.WARNING,
                                                   "{0} failed to identify protocol from {1} bytes for connection from {2} after {3} ms, " + "handling as non-multiplexed",
                                                   new Object[]{Listener.this.getChannel().socket()
                                                           .getLocalSocketAddress(), buf.position(), chan.socket()
                                                           .getRemoteSocketAddress(), deps
                                                           .getIdentificationTimeoutMillis()});
                                        break;

                                    default: // more data was available, but we hadn't detected it
                                        LOGGER.log(Level.WARNING,
                                                   "{0} handling delayed read on accepted connection from {1} after {2} ms",
                                                    new Object[] {Listener.this.getChannel().socket().getLocalSocketAddress(),
                                                        chan.socket().getRemoteSocketAddress(),
                                                        deps.getIdentificationTimeoutMillis()});
                                            break;
                                        }

                                    accept();
                                    }
                                catch (IOException e)
                                    {
                                    onException(e);
                                    }
                                }
                            }
                        };
                    deps.getSelectionService().invoke(chan, timer, deps.getIdentificationTimeoutMillis());
                    }

                return OP_READ;
                }

            @Override
            protected int onException(Throwable t)
                {
                if (LOGGER.isLoggable(Level.FINEST))
                    {
                    LogRecord record = new LogRecord(Level.FINEST, "{0} exception while waiting for multiplexed header on {1}");
                    record.setParameters(new Object[]{
                            Listener.this.getChannel().socket().getLocalSocketAddress(),
                            getChannel().socket().getRemoteSocketAddress()});
                    record.setThrown(t);
                    LOGGER.log(record);
                    }

                return super.onException(t);
                }

            /**
             * Accept the chanel and queue it to the appropriate MultiplexedServerSocketChannel
             *
             * @throws IOException if an I/O error occurs
             */
            protected void accept()
                    throws IOException
                {
                SocketChannel chan = getChannel();
                ByteBuffer          buf  = m_buf;

                // identify if the protocol header is present
                int nSubPort;
                if (!buf.hasRemaining() && buf.getInt(0) == PROTOCOL_ID)
                    {
                    // multiplexed client is connecting
                    nSubPort = buf.getInt(4);
                    buf      = null;
                    }
                else
                    {
                    // standard client connecting
                    nSubPort = -1;
                    if (!buf.flip().hasRemaining())
                        {
                        buf = null;
                        }
                    // else give the bytes back to the channel
                    }

                // find SeverSocketChannel
                final SelectionService svc = getDependencies().getSelectionService();
                svc.register(chan, /*handler*/ null);

                // use invocation to ensure that the channel has been unregistered before continuing, specifically we
                // must be unregisterd before setting chan to blocking mode
                final int           nFinSubPort = nSubPort;
                final ByteBuffer    bufFin      = buf;
                final SocketChannel chanFin     = chan;
                svc.invoke(chan, new Runnable()
                    {
                    @Override
                    public void run()
                        {
                        SocketChannel chan = chanFin;
                        try
                            {
                            try
                                {
                                chan.configureBlocking(true); // look like every accepted socket
                                }
                            catch (IllegalBlockingModeException e)
                                {
                                // deregistration hasn't occurred  yet (there is no order between register and invoke)
                                svc.invoke(chan, this, 0);
                                return;
                                }

                            MultiplexedServerSocketChannel server = m_mapBindings.get(nFinSubPort);
                            if (server != null)
                                {
                                // The MultiplexedChannel is replaced with another MultiplexedChannel to address a deficiency in
                                // the WrapperChannel infrastructure which prevents a channel from being registered, canceled, and
                                // re-registered with the same Selector.  This is caused by the fact that WrapperSocketChannel extends
                                // AbstractSocketChannel which in turn hides any reasonable way of removing formerly registered
                                // SelectionKeys.  So here we work-around the issue by producing a new channel.

                                // Also we take this opportunity to swap the local address for the accepted connection if
                                // it appears to be a NAT based connection.

                                SocketAddress addrSrv   = server.getLocalAddress();
                                InetAddress   addrSrvIP = InetAddresses.getAddress(addrSrv);
                                InetAddress   addrLocal = InetAddresses.isNatLocalAddress(addrSrv)
                                        ? addrSrvIP                                                                      // substitute the local NAT address
                                        : addrSrvIP.isAnyLocalAddress() && InetAddresses.hasNatLocalAddress()
                                            ? InetAddresses.getRoutes(InetAddresses.getLocalBindableAddresses(),
                                                Collections.singleton(chan.socket().getInetAddress())).iterator().next() // find best (possibly NAT) address
                                            : chan.socket().getLocalAddress();                                           // don't change

                                chan = new MultiplexedSocketChannel(((MultiplexedSocketChannel) chan).delegate(),
                                        new InetSocketAddress32(addrLocal, chan.socket().getLocalPort()), bufFin);
                                }

                            if (server == null || !server.add(chan))
                                {
                                // no registered ServerSocketChannel, or server queue was full
                                LogRecord record = new LogRecord(Level.FINE,
                                    "{0} rejecting connection from {1} to subport {2} due to {3}, header {4}");
                                record.setParameters(new Object[] {
                                        Listener.this.getChannel().socket().getLocalSocketAddress(),
                                        chan.socket().getRemoteSocketAddress(),
                                        nFinSubPort,
                                        server == null
                                            ? "absence of corresponding MultiplexedServerSocket"
                                            : "backlogged MultiplexedServerSocket",
                                        bufFin == null ? null : Buffers.toString(bufFin)});
                                LOGGER.log(record);

                                // TODO: it would be nice to be able to send back some indicator that this is meant to appear
                                // as a reject rather then an EOS. As is a multiplexed client would only see a rejected connection
                                // if there was no listener for the base port, but no listener for a sub-port looks like a connect/eos
                                // the problem with doing this would be that we couldn't cover all cases, such as ones where we
                                // don't yet know if the client is multiplexed.  So while we could add the feature we still couldn't
                                // cover all cases, thus any user of multiplexing must be able to handle a connect/eos case as if
                                // it was a reject.

                                chan.close();
                                }
                            }
                        catch (IOException e)
                            {
                            try
                                {
                                chan.close();
                                }
                            catch (IOException e2) {}
                            }
                        }
                    }, 0);

                m_buf = null; // mark as accepted
                }

            /**
             * Return true iff the channel has yet to be "accepted".
             *
             * @return true iff the channel has yet to be "accepted"
             */
            protected boolean isPending()
                {
                return getChannel().isOpen() && m_buf != null;
                }

            /**
             * Holder for protocol header.
             */
            protected ByteBuffer m_buf = ByteBuffer.allocate(8);

            /**
             * Timeout task watching for non-multiplexed connections.
             */
            protected Runnable m_timer;
            }

        /**
         * Map of port to servers.
         */
        protected final ConcurrentNavigableMap<Integer, MultiplexedServerSocketChannel>
                m_mapBindings = new ConcurrentSkipListMap();

        /**
         * The Set of allocated sub-ports in the ephemeral range.
         */
        protected final SortedSet<Integer> m_setEphemeral = m_mapBindings.descendingKeySet();
        }


    // ----- helpers --------------------------------------------------------

    /**
     * Return the underlying transport address for the specified address.
     *
     * @param addr  the multiplexed address
     *
     * @return the transport address
     */
    public static InetSocketAddress getTransportAddress(InetSocketAddress32 addr)
        {
        if (addr == null)
            {
            return null;
            }
        return new InetSocketAddress(addr.getAddress(), getBasePort(addr.getPort()));
        }

    /**
     * Return true iff the specified port represents an extended port.
     *
     * @param nPort  the port to test
     *
     * @return true iff the specified port represents a extended port
     */
    public static boolean isPortExtended(int nPort)
        {
        return (nPort & 0xFFFF0000) != 0;
        }

    /**
     * Return the base (transport) port for a given 32b port.
     *
     * @param nPort the port
     *
     * @return the base port
     */
    public static int getBasePort(int nPort)
        {
        return isPortExtended(nPort)
            ? ~nPort >>> 16
            : nPort;
        }

    /**
     * Return the sub-port for a given 32b port.
     *
     * @param nPort the port
     *
     * @return the sub-port, or -1 if none
     */
    public static int getSubPort(int nPort)
        {
        return isPortExtended(nPort)
            ? ~nPort & 0x0FFFF
            : -1;
        }

    /**
     * Return the 32 bit port for the specified base and sub port
     *
     * @param nPortBase  the base port
     * @param nPortSub   the sub port, or -1 for none
     *
     * @return  the 32 bit port version
     */
    public static int getPort(int nPortBase, int nPortSub)
        {
        if (nPortBase < 0 || nPortBase > 0xFFFF)
            {
            throw new IllegalArgumentException("base port " + nPortBase + " is out of range");
            }
        if (nPortSub < -1 || nPortSub > 0xFFFF)
            {
            throw new IllegalArgumentException("sub port " + nPortSub + " is out of range");
            }
        if (nPortBase == 0xFFFF && nPortSub >= 0)
            {
            throw new IllegalArgumentException("base port of 65535 does not support sub ports");
            }

        return nPortSub == -1
            ? nPortBase
            : ~(nPortBase << 16 | nPortSub);
        }


    // ----- inner class: ListenChannel ---------------------------------------------------

    /**
     * Helper wrapper for the real ServerSocketChannel to allow it to be managed by the
     * multiplexed SelectionService.
     */
    protected class ListenChannel
        extends WrapperServerSocketChannel
        implements MultiplexedChannel
        {
        public ListenChannel(ServerSocketChannel delegate)
                throws IOException
            {
            super(delegate, new MultiplexedSelectorProvider(delegate.provider()));
            }

        @Override
        public SocketChannel accept()
                throws IOException
            {
            SocketChannel chan = f_delegate.accept();
            return chan == null
                    ? null : new MultiplexedSocketChannel(chan, this.socket().getLocalSocketAddress(), null);
            }


        @Override
        public int readyOps()
            {
            return 0;
            }
        }

    // ----- interface: Dependencies ----------------------------------------

    /**
     * Dependencies describes the MultiplexedSocketProvider's dependencies.
     */
    public interface Dependencies
        {
        /**
         * Return the underlying SocketProvider to use.
         *
         * @return the SocketProvider
         */
        public SocketProvider getDelegateProvider();

        /**
         * Return the SelectionService to utilize for processing IO.
         *
         * @return the SelectionService
         */
        public SelectionService getSelectionService();

        /**
         * Return the backlog setting for the underlying SocketProvider.
         *
         * @return the backlog setting
         */
        public int getBacklog();

        /**
         * Return the number of milliseconds an accepted connection has to provide a multiplexed protocol header
         * before it is considered to be a standard (non-multiplexed) connection.
         * <p>
         * A high timeout will only negatively impact the arguably rare use-case of a non-multiplexed clients which
         * connects, sends nothing, and waits for the server-side to perform the first transmission. This initial server
         * transmission to a "quiet" non-multiplexed client will be artificially delayed by the identification timeout.
         * All other usage patterns will not be negatively impacted by a high timeout though would be negatively
         * impacted by a low timeout as even minimal packet loss could cause them to be incorrectly identified,
         * resulting in improper connection routing or socket closure.
         * </p>
         * <p>
         * See <a href=http://www.ietf.org/proceedings/75/slides/tcpm-1.pdf>Tuning TCP Parameters for the 21st Century</a>
         * for details on TCP's initial retransmission timeout. With even minimal packet loss the initial transmission
         * could easily take tens of seconds. It is this initial retransmission timeout which makes a low identification
         * timeout unsafe.
         * </p>
         * <p>
         * The default value may be controlled by the <tt>com.oracle.coherence.common.internal.net.MultiplexedSocketProvider.server.identification.timeout</tt>
         * system property and defaults to one minute.
         * </p>
         *
         * @return the timeout in milliseconds
         */
        public long getIdentificationTimeoutMillis();

        /**
         * Return the Logger to use.
         *
         * @return the logger
         */
        public Logger getLogger();
        }


    // ----- inner class: DefaultDependencies -------------------------------

    /**
     * Produce a copy of the specified Dependencies object.
     *
     * @param deps  the dependencies to copy
     *
     * @return the copied Dependencies as a DefaultDependencies object.
     */
    protected DefaultDependencies copyDependencies(Dependencies deps)
        {
        return new DefaultDependencies(deps);
        }

    /**
     * DefaultDependencies provides a default implementation of the Dependencies
     * interface.
     */
    public static class DefaultDependencies
            implements Dependencies
        {
        /**
         * Produce a DefaultDependencies object initialized with all defaults.
         */
        public DefaultDependencies()
            {
            this (null);
            }

        /**
         * Produce a copy based on the supplied Dependencies.
         *
         * @param deps the Dependencies to copy
         */
        public DefaultDependencies(Dependencies deps)
            {
            if (deps != null)
                {
                m_provider      = deps.getDelegateProvider();
                m_service       = deps.getSelectionService();
                m_nBacklog      = deps.getBacklog();
                m_cMillisIdentify = deps.getIdentificationTimeoutMillis();
                m_logger        = deps.getLogger();
                }
            }

        // ----- DefaultDependencies methods ----------------------------

        /**
         * Validate the dependencies object.
         *
         * @return this object
         */
        protected DefaultDependencies validate()
            {
            return this;
            }

        // ----- Dependencies methods -----------------------------------

        @Override
        public SocketProvider getDelegateProvider()
            {
            SocketProvider provider = m_provider;
            if (provider == null)
                {
                m_provider = provider = TcpSocketProvider.INSTANCE;
                }
            return provider;
            }

        /**
         * Specify the SocketProvider to which the MultiplexedSocketProvider
         * will delegate to.
         *
         * @param provider  the provider to delegate to
         *
         * @return this object
         */
        public DefaultDependencies setDelegateProvider(SocketProvider provider)
            {
            m_provider = provider;
            return this;
            }

        @Override
        public SelectionService getSelectionService()
            {
            SelectionService service = m_service;
            if (service == null)
                {
                m_service = service = SelectionServices.getDefaultService();
                }
            return service;
            }

        /**
         * Specify the SelectionService to use for IO processing.
         *
         * @param service  the SelectionService to use
         *
         * @return this object
         */
        public DefaultDependencies setSelectionService(SelectionService service)
            {
            m_service = service;
            return this;
            }

        @Override
        public int getBacklog()
            {
            return m_nBacklog;
            }

        /**
         * Specify the backlog to use when binding the underlying ServerSocket.
         *
         * @param nBacklog  the backlog
         *
         * @return this object
         */
        public DefaultDependencies setBacklog(int nBacklog)
            {
            m_nBacklog = nBacklog;
            return this;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public long getIdentificationTimeoutMillis()
            {
            return m_cMillisIdentify;
            }

        /**
         * Specify the identification timeout in milliseconds.
         *
         * @param cMillis  the identification timeout
         *
         * @return this object
         */
        public DefaultDependencies setIdentificationTimeoutMillis(long cMillis)
            {
            m_cMillisIdentify = cMillis;
            return this;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public Logger getLogger()
            {
            Logger logger = m_logger;
            return logger == null ? LOGGER : logger;
            }

        /**
         * Specify the Logger to use.
         *
         * @param logger  the logger
         *
         * @return this object
         */
        public DefaultDependencies setLogger(Logger logger)
            {
            m_logger = logger;
            return this;
            }

        // ----- constants ----------------------------------------------

        /**
         * The default backlog value.
         */
        protected static final int s_nBacklogDefault;

        /**
         * The default accept timeout.
         */
        protected static final long s_cMillisIdentifyDefault;

        static
            {
            // we default the backlog to as high as the underlying OS will allow, as multiplexed sockets
            // may see a significant connection rate, note that the Java translates a value of 0 to 50, and
            // thus relying on the Java default is simply insufficient for many multiplexed cases
            // on Linux the maximum allowed backlog is /proc/sys/net/core/somaxconn
            // on Windows a value of SOMAXCONN will allow the winsock impl to choose a reasonable value
            // while SOMAXCONN is not available in Java, the Windows value happens to be Integer.MAX_VALUE
            int nBacklog = Integer.MAX_VALUE;
            try
                {
                nBacklog = Integer.parseInt(System.getProperty(
                        MultiplexedSocketProvider.class.getName() +
                                ".server.backlog", Integer.toString(nBacklog)));
                }
            catch (Throwable t) {}
            s_nBacklogDefault = nBacklog;

            // Set the default identification timeout. This value governs how long we will wait for an accepted
            // connection to identify itself as multiplexed before assuming it is not. To identify itself
            // as multiplexed it only needs to send the 8-byte protocol header, and thus it would seem
            // that a very low timeout would be sufficient. Unfortunately though we have to account for
            // the possibility that this initial packet gets dropped and must wait on the TCP retransmit
            // timeout. This timeout is implementation dependent but appears to start at 3s in most OSs,
            // and increase from there in response to packet loss, i.e. 3, 6, 12,...  Based on how the TCP
            // 3-way handshake works the client should always see the connection as being established
            // before the server does. So the only dropped packet we need to worry about is the first
            // user-data packet, i.e. the one with our protocol header. Given the above timings and assuming
            // near-zero delivery time if this packet were to be dropped once it would take the server about
            // 3s to see it, or 9s if it were dropped twice, or 21s if dropped thrice. Since we can't possibly
            // rely on zero packet loss, it would seem our minimum safe default is going to be >3s, with >9s
            // being the next safe option. Based on this we reluctantly have a default to a large timeout.
            // This should only impose a negative impact on non-multiplexed "polite" clients, i.e. clients
            // which connect and wait for the server to speak first.
            //
            // For more details on these timings see:
            // http://www.ietf.org/proceedings/75/slides/tcpm-1.pdf
            // http://us.generation-nt.com/answer/patch-tcp-expose-initial-rto-via-new-sysctl-help-203365542.html
            long cMillisId = 0;
            try
                {
                cMillisId = new Duration(System.getProperty(
                        MultiplexedSocketProvider.class.getName() +
                                ".server.identification.timeout", "1m")).as(Duration.Magnitude.MILLI); // default doc'd on getIdentificationTimeoutMillis
                }
            catch (Throwable t) {}
            s_cMillisIdentifyDefault = cMillisId;
            }

        // ----- data members -------------------------------------------

        /**
         * The SocketProvider to utilize.
         */
        protected SocketProvider m_provider;

        /**
         * The SelectionService to utilize.
         */
        protected SelectionService m_service;

        /**
         * The backlog.
         */
        protected int m_nBacklog = s_nBacklogDefault;

        /**
         * The accept timeout.
         */
        protected long m_cMillisIdentify = s_cMillisIdentifyDefault;

        /**
         * The Logger.
         */
        protected Logger m_logger;
        }


    // ----- constants ------------------------------------------------------

    /**
     * WellKnownSubports are sub-ports that are reserved for use by components.
     */
    public static enum WellKnownSubPorts
        {
        COHERENCE_TCP_RING (1),
        COHERENCE_TCMP_DATAGRAM (2),
        COHERENCE_NAME_SERVICE (3);

        /**
         * Construct a well known subport for MultiplexedSocketProvider
         * @param subPort
         */
        WellKnownSubPorts(int subPort)
            {
            m_nSubPort = subPort;
            }

        /**
         * Return the sub-port.
         *
         * @return the sub-port
         */
        public int getSubPort()
            {
            return m_nSubPort;
            }

        /**
         * Return the 32 bit port number consisting of the supplied base-port and this sub-port.
         *
         * @param nPortBase  the base port
         *
         * @return the port
         */
        public int getPort(int nPortBase)
            {
            return MultiplexedSocketProvider.getPort(nPortBase, m_nSubPort);
            }

        // ----- data members -------------------------------------------

        private final int m_nSubPort;
        }

    /**
     * The protocol identifier.
     */
    protected static final int PROTOCOL_ID = ProtocolIdentifiers.MULTIPLEXED_SOCKET;

    /**
     * The end of the well-known sub-port range.
     */
    public static final int WELL_KNOWN_SUB_PORT_END = 1023;

    /**
     * The start of the ephemeral sub-port range.
     */
    protected static final int EPHEMERAL_SUB_PORT_START = 32768;

    /**
     * The default Logger for the provider.
     */
    private static Logger LOGGER = Logger.getLogger(MultiplexedSocketProvider.class.getName());

    static final Set<SocketOption<?>> SERVER_OPTIONS;

    static
        {
        Set<SocketOption<?>> setOpt = new HashSet<SocketOption<?>>();
        setOpt.add(StandardSocketOptions.SO_RCVBUF);
        setOpt.add(StandardSocketOptions.SO_REUSEADDR);
        SERVER_OPTIONS = Collections.unmodifiableSet(setOpt);
        }


    // ----- data members ---------------------------------------------------

    /**
     * The provider's dependencies.
     */
    protected final Dependencies m_dependencies;

    /**
     * Map of Listener addresses to their corresponding Listener object
     */
    protected final ConcurrentMap<InetSocketAddress, Listener>
            m_mapListener = new ConcurrentHashMap();

    /**
     * The minimum base ephemeral port number which has at some point been
     * allocated.
     */
    protected int m_nPortEphemeralLow = Integer.MAX_VALUE;

    /**
     * The maximum base ephemeral port number which has at some point been
     * allocated.
     */
    protected int m_nPortEphemeralHi = Integer.MIN_VALUE;
    }
