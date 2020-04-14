/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.net;


import java.io.IOException;

import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;

import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import java.security.NoSuchAlgorithmException;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;

import com.oracle.coherence.common.internal.net.ssl.SSLSocket;
import com.oracle.coherence.common.internal.net.ssl.SSLSocketChannel;
import com.oracle.coherence.common.internal.net.ssl.SSLServerSocket;
import com.oracle.coherence.common.internal.net.ssl.SSLServerSocketChannel;
import com.oracle.coherence.common.internal.security.SecurityProvider;

import com.oracle.coherence.common.util.DaemonThreadFactory;



/**
 * SocketProvider that produces instances of socket and channel implementations
 * which utilize SSL.
 *
 * @author mf, jh  2010.04.21
 */
public class SSLSocketProvider
        implements SocketProvider
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct an SSLSocketProvider.
     */
    public SSLSocketProvider()
        {
        this (null);
        }

    /**
     * Construct an SSLSocketProvider.
     *
     * @param deps  the provider dependencies, or null
     */
    public SSLSocketProvider(Dependencies deps)
        {
        m_dependencies = copyDependencies(deps).validate();
        }


    // ----- SocketProvider interface ---------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public SocketAddress resolveAddress(String sAddr)
        {
        return getDependencies().getDelegateSocketProvider().resolveAddress(sAddr);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getAddressString(Socket socket)
        {
        return getDependencies().getDelegateSocketProvider().getAddressString(socket);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getAddressString(ServerSocket socket)
        {
        return getDependencies().getDelegateSocketProvider().getAddressString(socket);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public Socket openSocket()
            throws IOException
        {
        return new SSLSocket(getDependencies().getDelegateSocketProvider()
                .openSocket(), this);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public SocketChannel openSocketChannel()
            throws IOException
        {
        return new SSLSocketChannel(getDependencies().getDelegateSocketProvider()
                .openSocketChannel(), this);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public ServerSocket openServerSocket()
            throws IOException
        {
        return new SSLServerSocket(getDependencies().getDelegateSocketProvider()
                .openServerSocket(), this);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public ServerSocketChannel openServerSocketChannel()
            throws IOException
        {
        return new SSLServerSocketChannel(
                getDependencies().getDelegateSocketProvider()
                        .openServerSocketChannel(), this);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public SocketProvider getDelegate()
        {
        return getDependencies().getDelegateSocketProvider();
        }


    // ----- Object methods -------------------------------------------------

    /**
     * {@inheritDoc}
     */
    public String toString()
        {
        return "SSLSocketProvider(" + getDependencies().toString() + ")";
        }


    // ----- helpers --------------------------------------------------------

    /**
     * Ensure that supplied session is acceptable.
     *
     * @param session  the current SSLSession; must not be null
     * @param socket   the socket associated with the session
     *
     * @throws SSLException if the session is unacceptible
     */
    public void ensureSessionValidity(SSLSession session, Socket socket)
            throws SSLException
        {
        if (session == null || socket == null)
            {
            throw new IllegalArgumentException();
            }

        HostnameVerifier verifier = getDependencies().getHostnameVerifier();
        if (verifier == null ||
            verifier.verify(socket.getInetAddress().getHostName(), session))
            {
            getDependencies().getLogger().log(Level.FINE, "Established " + session.getCipherSuite() +
                    " connection with " + socket);
            }
        else
            {
            throw new SSLException("Unacceptible peer: " + socket);
            }
        }



    /**
     * Return the SocketProvider's dependencies.
     *
     * @return the dependencies
     */
    public Dependencies getDependencies()
        {
        return m_dependencies;
        }

    /**
     * Produce a shallow copy of the supplied dependencies.
     *
     * @param deps  the dependencies to copy
     *
     * @return the dependencies
     */
    protected DefaultDependencies copyDependencies(Dependencies deps)
        {
        return new DefaultDependencies(deps);
        }


    // ----- inner interface: Dependencies ----------------------------------

    /**
     * Dependencies specifies all dependency requirements of the SSLSocketProvider.
     */
    public interface Dependencies
        {
        /**
         * Return the SocketProvider to use in producing the underling sockets
         * which will be wrapped with SSL.
         *
         * @return the delegate SocketProvider
         */
        public SocketProvider getDelegateSocketProvider();

        /**
         * Return the SSLContext representing the SSL implementation and
         * configuration.
         *
         * @return  the SSLContext
         */
        public SSLContext getSSLContext();

        /**
         * Return a copy of the SSLParameters used by the SSLSocketProvider.
         *
         * @return a copy of the SSLParameters used by the SSLSocketProvider
         */
        public SSLParameters getSSLParameters();

        /**
         * Return true iff produced server sockets will require client
         * authentication.
         *
         * @return  true iff client authentication is required
         */
        public boolean isClientAuthenticationRequired();

        /**
         * Return the SSL HostnameVerifier to be used to verify hostnames
         * once an SSL session has been established.
         *
         * @return  the verifier, or null to disable
         */
        public HostnameVerifier getHostnameVerifier();

        /**
         * Return the set of enabled SSL cipher suites.
         *
         * @return  the enabled SSL cipher suites, or null for default
         */
        public String[] getEnabledCipherSuites();

        /**
         * Return the set of enabled protocol versions.
         *
         * @return  the enabled protocol versions, or null for default
         */
        public String[] getEnabledProtocolVersions();

        /**
         * Return the Executor to use in offloading delegated tasks.
         *
         * @return the Executor
         */
        public Executor getExecutor();

        /**
         * Return the Logger to use.
         *
         * @return the Logger
         */
        public Logger getLogger();
        }


    // ----- inner class: DefaultDependenceis -------------------------------

    /**
     * DefaultDependenceis is a basic implementation of the Dependencies
     * interface provding "setter" methods for each property.
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
                m_delegate                  = deps.getDelegateSocketProvider();
                m_ctx                       = deps.getSSLContext();
                m_fClientAuthRequired       = deps.isClientAuthenticationRequired();
                m_hostnameVerifier          = deps.getHostnameVerifier();
                m_asCipherSuitesEnabled     = deps.getEnabledCipherSuites();
                m_asProtocolVersionsEnabled = deps.getEnabledProtocolVersions();
                m_executor                  = deps.getExecutor();
                m_logger                    = deps.getLogger();
                }
            }

        /**
         * Apply the specified settingsSSL to the dependencies
         * 
         * @param settingsSSL  the SSLSettings object to apply
         *
         * @return this dependencies object
         */
        public DefaultDependencies applySSLSettings(SSLSettings settingsSSL)
            {
            return this.setSSLContext(settingsSSL.getSSLContext())
                    .setClientAuthenticationRequired(settingsSSL.isClientAuthenticationRequired())
                    .setHostnameVerifier(settingsSSL.getHostnameVerifier())
                    .setEnabledCipherSuites(settingsSSL.getEnabledCipherSuites())
                    .setEnabledProtocolVersions(settingsSSL.getEnabledProtocolVersions());
            }

        // ----- Dependencies interface ---------------------------------

        /**
         * {@inheritDoc}
         */
        @Override
        public SocketProvider getDelegateSocketProvider()
            {
            return m_delegate;
            }

        /**
         * Specify the SocketProvider to delegate to.
         *
         * @param delegate  the delegate SocketProvider
         *
         * @return this object
         */
        public DefaultDependencies setDelegate(SocketProvider delegate)
            {
            m_delegate = delegate;
            return this;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public SSLContext getSSLContext()
            {
            SSLContext ctx = m_ctx;
            try
                {
                return ctx == null ? SSLContext.getDefault() : ctx;
                }
            catch (NoSuchAlgorithmException e)
                {
                throw new IllegalStateException(e);
                }
            }

        /**
         * Specify the SSLContex to utilize.
         *
         * @param ctx  the SSLContext
         *
         * @return this object
         */
        public DefaultDependencies setSSLContext(SSLContext ctx)
            {
            m_ctx = ctx;
            return this;
            }

        /**
         * {@inheritDoc}
         */
        public SSLParameters getSSLParameters()
            {
            SSLContext    ctx         = getSSLContext();
            SSLParameters params      = ctx.getDefaultSSLParameters();
            String[]      asCiphers   = getEnabledCipherSuites();
            String[]      asProtocols = getEnabledProtocolVersions();
            if (asCiphers != null)
                {
                params.setCipherSuites(asCiphers);
                }

            if (asProtocols != null)
                {
                params.setProtocols(asProtocols);
                }

            params.setNeedClientAuth(isClientAuthenticationRequired());

            return params;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isClientAuthenticationRequired()
            {
            return m_fClientAuthRequired;
            }

        /**
         * Specify if client authentication is required.
         *
         * @param fRequired  true iff client authentication is required
         *
         * @return this object
         */
        public DefaultDependencies setClientAuthenticationRequired(boolean fRequired)
            {
            m_fClientAuthRequired = fRequired;
            return this;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public HostnameVerifier getHostnameVerifier()
            {
            return m_hostnameVerifier;
            }

        /**
         * Specify the HostnameVerifier.
         *
         * @param verifier  the HostnameVerifier
         *
         * @return this object
         */
        public DefaultDependencies setHostnameVerifier(HostnameVerifier verifier)
            {
            m_hostnameVerifier = verifier;
            return this;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public Executor getExecutor()
            {
            Executor executor = m_executor;
            return executor == null ? DEFAULT_EXECUTOR : executor;
            }

        /**
         * Specify the Executor to use.
         *
         * @param executor  the Executor
         *
         * @return this object
         */
        public DefaultDependencies setExecutor(Executor executor)
            {
            m_executor = executor;
            return this;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public String[] getEnabledCipherSuites()
            {
            return m_asCipherSuitesEnabled;
            }

        /**
         * Specify the enabled cipher suites.
         *
         * @param asCiphers  the enabled ciper suites
         *
         * @return this object
         */
        public DefaultDependencies setEnabledCipherSuites(String[] asCiphers)
            {
            m_asCipherSuitesEnabled = asCiphers;
            return this;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public String[] getEnabledProtocolVersions()
            {
            return m_asProtocolVersionsEnabled;
            }

        /**
         * Specify the enabled protocol versions.
         *
         * @param asProtocols  the enabled protocol versions
         *
         * @return this object
         */
        public DefaultDependencies setEnabledProtocolVersions(String[] asProtocols)
            {
            m_asProtocolVersionsEnabled = asProtocols;
            return this;
            }

        /**
         * {@inheritDoc}
         */
        public Logger getLogger()
            {
            Logger logger = m_logger;
            return logger == null ? LOGGER : logger;
            }


        /**
         * Specify the Logger to utilize.
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


        /**
         * {@inheritDoc}
         */
        @Override
        public String toString()
            {
            StringBuilder sb = new StringBuilder();
            sb.append(getSSLContext());

            if (getHostnameVerifier() != null)
                {
                sb.append(", hostname-verifier=enabled");
                }

            if (isClientAuthenticationRequired())
                {
                sb.append(", client-auth=enabled");
                }

            return sb.toString();
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
            ensureArgument(getExecutor(),               "Executor");
            ensureArgument(getLogger(),                 "Logger");
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

        // ----- data members -------------------------------------------

        /**
         * The SocketProvider which produces the underlying clear-text
         * sockets.
         */
        protected SocketProvider m_delegate = TcpSocketProvider.INSTANCE;

        /**
         * The SSLContext used by this SocketProvider.
         */
        protected SSLContext m_ctx;

        /**
         * True if client authentication is required.
         */
        protected boolean m_fClientAuthRequired;

        /**
         * The HostnameVerifier used by this SocketProvider.
         */
        protected HostnameVerifier m_hostnameVerifier;

        /**
         * The enabled cipher suites or null for default.
         */
        protected String[] m_asCipherSuitesEnabled;

        /**
         * The enabled protocol versions.
         */
        protected String[] m_asProtocolVersionsEnabled;

        /**
         * The SSL executor, or null for default.
         */
        protected Executor m_executor;

        /**
         * The Logger to use.
         */
        protected Logger m_logger;

        static
            {
            // make sure the commons security provider is loaded
            SecurityProvider.ensureRegistration();
            }
        }


    // ----- data members ---------------------------------------------------

    /**
     * The SSLSocketProvider's dependencies.
     */
    protected Dependencies m_dependencies;


    // ----- constants ------------------------------------------------------

    /**
     * The default Logger for all derivations of this class.
     */
    private static Logger LOGGER = Logger.getLogger(
            SSLSocketProvider.class.getName());

    /**
     * The default executor used by new SSLSocketProviders.
     */
    private static final Executor DEFAULT_EXECUTOR = Executors.
            newCachedThreadPool(new DaemonThreadFactory("SSLExecutor-"));


    }
