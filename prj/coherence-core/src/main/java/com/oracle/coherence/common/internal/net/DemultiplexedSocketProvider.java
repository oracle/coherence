/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.internal.net;

import java.io.IOException;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;

import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import com.oracle.coherence.common.net.InetSocketAddress32;
import com.oracle.coherence.common.net.InetSocketProvider;

/**
 * DemultiplexedSocketProvider is a bridge Socket provider that allows to use
 * MultiplexedSocketProvider without converting the socket addresses into
 * InetSocketAddres32.  DemultiplexedSocketProvider converts the socket addresses
 * with the passed in subport. The ServerSocketChannel created using the DemultiplexedSocketProvider
 * will bind the server socket to the given subport. Similarly client socket,
 * will try to connect to remote peer on the given subport.
 *
 * @author bb  2011.12.06
 */
public class DemultiplexedSocketProvider
    extends InetSocketProvider
    {
    /**
     * Construct a DemultiplexedSocketProvider
     *
     * @param delegate  the underlying MultiplexedSocketProvider
     * @param subport   subport to use to convert regular InetSocketAddresses, or -1 for standard port
     */
    public DemultiplexedSocketProvider(MultiplexedSocketProvider delegate, int subport)
        {
        if (subport == 0 || subport < -1 || subport > 65535)
            {
            // cannot create a DemultiplexedSocketProvider with ephemeral subport
            // since it will never be accessible outside of DemultiplexedSocketProvider
            throw new IllegalArgumentException("Illegal subport: "+subport);
            }
        m_nSubport = subport;
        m_delegate = delegate;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public ServerSocketChannel openServerSocketChannel()
            throws IOException
        {
        return new DemultiplexedServerSocketChannel(m_delegate);
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
        return new DemultiplexedSocketChannel(m_delegate);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public Socket openSocket()
            throws IOException
        {
        return new DemultiplexedSocket((MultiplexedSocketProvider.MultiplexedSocket) m_delegate.openSocket());
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
        {
        return "DemultiplexedSocketProvider(" + m_delegate + ")";
        }

    /**
     * Return the delegate SocketProvider.
     *
     * @return the delegate SocketProvider
     */
    public MultiplexedSocketProvider getDelegate()
        {
        return m_delegate;
        }

    /**
     * DemultiplexedServerSocketChannel extends MultiplexedServerSocketChannel
     * so that it can covert all the SocketAddresses into InetSocketAddress32.
     * It doesn't simply wrap the channel, as it needs to be compatible with
     * the delegate's selectors
     */
    protected class DemultiplexedServerSocketChannel
        extends MultiplexedSocketProvider.MultiplexedServerSocketChannel
        {
        /**
         * Create a DemultiplexedServerSocketChannel
         *
         * @param provider Underlying MultiplexedSocketProvider
         *
         * @throws IOException if I/O error occurs
         */
        public DemultiplexedServerSocketChannel(MultiplexedSocketProvider provider)
            throws IOException
            {
            super(provider);
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public SocketChannel accept()
            throws IOException
            {
            MultiplexedSocketProvider.MultiplexedSocketChannel channel =
                    (MultiplexedSocketProvider.MultiplexedSocketChannel) super.accept();
            return (channel == null)
                    ? channel
                    : new DemultiplexedSocketChannel(channel);
            }

        @Override
        protected ServerSocket createServerSocket()
            throws IOException
            {
            return new WrapperServerSocket(super.createServerSocket())
                {
                @Override
                public ServerSocketChannel getChannel()
                    {
                    return DemultiplexedServerSocketChannel.this;
                    }

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
                    super.bind(getMultiplexedSocketAddress(endpoint), backlog);
                    m_address = endpoint;
                    }

                @Override
                public int getLocalPort()
                    {
                    return MultiplexedSocketProvider.getBasePort(super.getLocalPort());
                    }

                @Override
                public SocketAddress getLocalSocketAddress()
                    {
                    SocketAddress addr = m_address;
                    if (addr == null)
                        {
                        addr = m_address = MultiplexedSocketProvider.getTransportAddress(
                            (InetSocketAddress32) super.getLocalSocketAddress());
                        }
                    return addr;
                    }

                /**
                 * Local address
                 */
                protected SocketAddress m_address;
                };
            }

        @Override
        protected ServerSocketChannel getChannel()
            {
            return this;
            }
        }

    /**
     * DemultiplexedSocketChannel extends MultiplexedSocketChannel
     * so that it can convert all the SocketAddresses into InetSocketAddress32
     */
    protected class DemultiplexedSocketChannel
        extends MultiplexedSocketProvider.MultiplexedSocketChannel
        {
        /**
         * Create a DemultiplexedSocketChannel
         *
         * @param delegate underlying SocketChannel
         */
        public DemultiplexedSocketChannel(MultiplexedSocketProvider.MultiplexedSocketChannel delegate)
            {
            // A DemultiplexedSocketChannel is also a MultiplexedSocketChannel. It works on top of the plain delegate socket channel
            // from the passed in MultiplexedSocketChannel.
            super(delegate.delegate(), delegate.m_addrLocal, delegate.m_bufHeaderIn);
            if (delegate.getClass() != MultiplexedSocketProvider.MultiplexedSocketChannel.class)
                {
                throw new IllegalArgumentException("DemultiplexedSocketChannel can only work with MultiplexedSocketChannel");
                }
            }

        /**
         * Create a DemultiplexedSocketChannel
         *
         * @param provider underlying MultiplexedSocketProvider
         *
         * @throws IOException if an IO error occurs
         */
        public DemultiplexedSocketChannel(MultiplexedSocketProvider provider)
            throws IOException
            {
            super(provider.getDependencies().getDelegateProvider().openSocketChannel(), /*addrLocal*/ null, /*bufHeader*/ null);
            }

        /**
         * {@inheritDoc}
         */
        @Override
         public boolean connect(SocketAddress remote)
                 throws IOException
             {
             return super.connect(getMultiplexedSocketAddress(remote));
             }

        @Override
        protected Socket wrapSocket(Socket socket)
            {
            return new DemultiplexedSocket((MultiplexedSocketProvider.MultiplexedSocket) super.wrapSocket(socket))
                {
                @Override
                public SocketChannel getChannel()
                    {
                    return DemultiplexedSocketChannel.this;
                    }
                };
            }
        }

    /**
     * DemultiplexedSocket wraps MultiplexedSocket
     * so that it can convert all the SocketAddresses into InetSocketAddress32
     */
    protected class DemultiplexedSocket
        extends WrapperSocket
        {
        /**
         * Create a DemultiplexedSocket.
         *
         * @param delegate  delegate socket
         */
        public DemultiplexedSocket(MultiplexedSocketProvider.MultiplexedSocket delegate)
            {
            super(delegate);
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public void bind(SocketAddress addr)
                throws IOException
                {
                super.bind(getMultiplexedSocketAddress(addr));
                m_addressLocal = addr;
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
                super.connect(getMultiplexedSocketAddress(addr), cMillis);
                m_addressRemote = addr;
                }

        /**
         * {@inheritDoc}
         */
        public int getLocalPort()
            {
            return MultiplexedSocketProvider.getBasePort(super.getLocalPort());
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public SocketAddress getLocalSocketAddress()
            {
            SocketAddress addrLocal = m_addressLocal;
            if (addrLocal == null)
                {
                addrLocal = m_addressLocal = MultiplexedSocketProvider.getTransportAddress(
                    (InetSocketAddress32) super.getLocalSocketAddress());
                }
            return addrLocal;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getPort()
            {
            return MultiplexedSocketProvider.getBasePort(super.getPort());
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public SocketAddress getRemoteSocketAddress()
            {
            SocketAddress addrRemote = m_addressRemote;
            if (addrRemote == null)
                {
                addrRemote = m_addressRemote = MultiplexedSocketProvider.getTransportAddress(
                    (InetSocketAddress32) super.getRemoteSocketAddress());
                }
            return addrRemote;
            }

        /**
         * Local address
         */
        protected SocketAddress m_addressLocal;

        /**
         * Peer address
         */
        protected SocketAddress m_addressRemote;
        }

    /**
     * Helper method to convert InetSocketAddress to InetSockeAddress32 for use
     * by MultiplexedSocketProvider
     *
     * @param address InetSocketAddress to convert
     * @return InetSocketAddress32
     */
    protected InetSocketAddress32 getMultiplexedSocketAddress(SocketAddress address)
        {
        if (address == null)
            {
            return null;
            }
        else if (address instanceof InetSocketAddress)
            {
            InetSocketAddress inetAddr = (InetSocketAddress) address;
            return new InetSocketAddress32(inetAddr.getAddress(),
                MultiplexedSocketProvider.getPort(inetAddr.getPort(), m_nSubport));
            }
        throw new IllegalArgumentException("Invalid socket address type: "+address);
        }

    /**
     * Underlying MultiplexedSocketProvider
     */
    protected final MultiplexedSocketProvider m_delegate;

    /**
     * Subport to be used by this DemultiplexedSocketProvider
     */
    protected final int m_nSubport;
    }
