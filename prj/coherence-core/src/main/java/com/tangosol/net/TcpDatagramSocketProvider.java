/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net;

import com.oracle.coherence.common.net.SocketProvider;

import com.tangosol.config.annotation.Injectable;

import java.io.IOException;

import java.net.DatagramSocket;
import java.net.MulticastSocket;

/**
 * TcpDatagramSocketProvider produces datagram sockets that uses
 * TCP sockets underneath for unicast communication. For multicast, it still uses
 * the MulticastSockets.
 *
 * @author bb 2011.11.21
 * @since Coherence 12.1.2
 */
public class TcpDatagramSocketProvider
        implements DatagramSocketProvider
    {
    /**
     * Construct a TcpDatagramSocketProvider.
     */
    public TcpDatagramSocketProvider()
        {
        this(null);
        }

    /**
     * Construct a TcpDatagramSocketProvider
     * @param deps the provider dependencies, or null
     */
    public TcpDatagramSocketProvider(Dependencies deps)
        {
        m_dependencies = new DefaultDependencies(deps).validate();
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public DatagramSocket openDatagramSocket()
            throws IOException
        {
        return configure(m_dependencies.isBlocking()
                         ? new TcpDatagramSocket(m_dependencies.getDelegateSocketProvider())
                         : new NonBlockingTcpDatagramSocket(m_dependencies.getDelegateSocketProvider()));
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public MulticastSocket openMulticastSocket()
            throws IOException
        {
        return new MulticastSocket(null);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSecure()
        {
        return false;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
        {
        return "TCPDatagramSocketProvider[Delegate: " + m_dependencies.getDelegateSocketProvider() + "]";
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Configure the socket.
     *
     * @param socket  the socket
     *
     * @return the configured socket
     */
    protected TcpDatagramSocket configure(TcpDatagramSocket socket)
        {
        int nAdvanceFrequency = m_dependencies.getAdvanceFrequency();

        if (nAdvanceFrequency > 0)
            {
            socket.setAdvanceFrequency(nAdvanceFrequency);
            }

        return socket;
        }

    // ----- inner interface: Dependencies ----------------------------------

    /**
     * Dependencies specifies all dependency requirements of the TcpDatagramSocketProvider.
     */
    public interface Dependencies
        {
        /**
         * Return the SocketProvider to use in producing the underling sockets
         * which will be wrapped with DatagramSocket.
         *
         * @return the delegate SocketProvider
         */
        SocketProvider getDelegateSocketProvider();

        /**
         * Check if datagram sockets should be blocking
         * the use of "blocking" datagram sockets is not meant for production
         * use and this setting should remain undocumented
         *
         * @return true if use blocking sockets
         */
        boolean isBlocking();

        /**
         * Get the frequency at which the DatagramSocket will advance over
         * the sub-sockets during receive
         *
         * @return int frequency
         */
        int getAdvanceFrequency();
        }

    /**
     * DefaultDependenceis is a basic implementation of the Dependencies
     * interface providing "setter" methods for each property.
     * <p>
     * Additionally this class serves as a source of default dependency values.
     */
    public static class DefaultDependencies
            implements Dependencies
        {
        /**
         * Construct a DefaultDependencies object.
         */
        public DefaultDependencies()
            {
            }

        /**
         * Construct a DefaultDependencies object copying the values from the
         * specified dependencies object
         *
         * @param deps  the dependencies to copy, or null
         */
        public DefaultDependencies(Dependencies deps)
            {
            if (deps != null)
                {
                m_delegateSocketProvider = deps.getDelegateSocketProvider();
                m_fBlocking              = deps.isBlocking();
                m_nAdvanceFrequency      = deps.getAdvanceFrequency();
                }
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public SocketProvider getDelegateSocketProvider()
            {
            return m_delegateSocketProvider;
            }

        /**
         * Set Delegate SocketProvider
         *
         * @param provider
         */
        public DefaultDependencies setDelegateSocketProvider(SocketProvider provider)
            {
            m_delegateSocketProvider = provider;

            return this;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isBlocking()
            {
            return m_fBlocking;
            }

        /**
         * Set if datagram socket is blocking
         *
         * @param fBlocking
         */
        @Injectable("blocking")
        public void setBlocking(boolean fBlocking)
            {
            m_fBlocking = fBlocking;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getAdvanceFrequency()
            {
            return m_nAdvanceFrequency;
            }

        /**
         * Set frequency for datagram socket
         *
         * @param frequency
         */
        @Injectable("advance-frequency")
        public void setAdvanceFrequency(int frequency)
            {
            m_nAdvanceFrequency = frequency;
            }

        // ----- helpers ------------------------------------------------

        /**
         * Validate the dependencies.
         *
         * @return this object
         *
         * @throws IllegalArgumentException if the dependencies are invalid
         */
        protected DefaultDependencies validate()
                throws IllegalArgumentException
            {
            ensureArgument(getDelegateSocketProvider(), "DelegateSocketProvider");

            return this;
            }

        /**
         * Ensure that the specified object is non-null
         *
         * @param o      the object to ensure
         * @param sName  the name of the corresponding parameter
         *
         * @throws IllegalArgumentException if o is null
         */
        protected static void ensureArgument(Object o, String sName)
            {
            if (o == null)
                {
                throw new IllegalArgumentException(sName + " cannot be null");
                }
            }

        /**
         * Underlying TCP socket provider
         */
        protected SocketProvider m_delegateSocketProvider = SocketProviderFactory.DEFAULT_SOCKET_PROVIDER;

        /**
         * Specifies if the provider is to produce blocking datagram sockets.
         */
        protected boolean m_fBlocking = false;

        /**
         * The TcpDatagramSocket advance frequency.
         */
        protected int m_nAdvanceFrequency;
        }


    // ----- constants ------------------------------------------------------

    /**
     * A default SocketProvider instance.
     */
    public static final TcpDatagramSocketProvider INSTANCE = new TcpDatagramSocketProvider();


    // ----- data members ---------------------------------------------------

    /**
     * TcpDatagramSocketProvider Dependencies
     */
    protected Dependencies m_dependencies;
    }
