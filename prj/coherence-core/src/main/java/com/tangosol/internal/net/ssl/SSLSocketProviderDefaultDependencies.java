/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.ssl;

import com.oracle.coherence.common.net.SSLSocketProvider;
import com.oracle.coherence.common.net.SocketProvider;
import com.tangosol.coherence.config.builder.SocketProviderBuilder;
import com.tangosol.config.annotation.Injectable;
import com.tangosol.net.SocketProviderFactory;

import com.tangosol.util.DaemonThreadFactory;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * Injectable SSLSocketProviderDependency. Replaced deprecated {@link LegacyXmlSSLSocketProviderDependencies}.
 *
 * @author jf 2015.11.11
 * @since Coherence 12.2.1.1
 */
public class SSLSocketProviderDefaultDependencies
        extends SSLSocketProvider.DefaultDependencies
    {

    /**
     * Construct SSLSocketProviderDependencies object
     *
     * @param dependencies  SocketProviderFactory dependencies
     */
    public SSLSocketProviderDefaultDependencies(SocketProviderFactory.Dependencies dependencies)
        {
        m_DependenciesProviderFactory = (dependencies == null)
                                            ? new SocketProviderFactory.DefaultDependencies()
                                            : dependencies;

        // set default.
        setDelegate(SocketProviderFactory.DEFAULT_SOCKET_PROVIDER);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public SocketProvider getDelegateSocketProvider()
        {
        return super.getDelegateSocketProvider();
        }

    @Injectable("socket-provider")
    public void setDelegateSocketProviderBuilder(SocketProviderBuilder builder)
        {
        if (builder == null)
            {
            super.setDelegate(SocketProviderFactory.DEFAULT_SOCKET_PROVIDER);
            }
        else
            {
            super.setDelegate(builder.realize(null, null, null));
            }
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public SSLContext getSSLContext()
        {
        return super.getSSLContext();
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isClientAuthenticationRequired()
        {
        return super.isClientAuthenticationRequired();
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public HostnameVerifier getHostnameVerifier()
        {
        return super.getHostnameVerifier();
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] getEnabledCipherSuites()
        {
        return super.getEnabledCipherSuites();
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] getEnabledProtocolVersions()
        {
        return super.getEnabledProtocolVersions();
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public Executor getExecutor()
        {
        return super.getExecutor();
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public Logger getLogger()
        {
        return super.getLogger();
        }

    /**
     * {@inheritDoc}
     */
    @Injectable("provider")
    @Override
    public SSLSocketProvider.DefaultDependencies setSSLContext(SSLContext ctx)
        {
        return super.setSSLContext(ctx);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public SSLSocketProvider.DefaultDependencies setClientAuthenticationRequired(boolean fRequired)
        {
        return super.setClientAuthenticationRequired(fRequired);
        }

    /**
     * {@inheritDoc}
     */
    @Injectable("hostname-verifier")
    @Override
    public SSLSocketProvider.DefaultDependencies setHostnameVerifier(HostnameVerifier verifier)
        {
        return super.setHostnameVerifier(verifier);
        }

    /**
     * {@inheritDoc}
     */
    @Injectable("executor")
    @Override
    public SSLSocketProvider.DefaultDependencies setExecutor(Executor executor)
        {
        return super.setExecutor(executor);
        }

    /**
     * {@inheritDoc}
     */
    @Injectable("cipher-suites")
    @Override
    public SSLSocketProvider.DefaultDependencies setEnabledCipherSuites(String[] asCiphers)
        {
        return super.setEnabledCipherSuites(asCiphers);
        }

    /**
     * {@inheritDoc}
     */
    @Injectable("protocol-versions")
    @Override
    public SSLSocketProvider.DefaultDependencies setEnabledProtocolVersions(String[] asProtocols)
        {
        return super.setEnabledProtocolVersions(asProtocols);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public SSLSocketProvider.DefaultDependencies setLogger(Logger logger)
        {
        return super.setLogger(logger);
        }

    /**
     * Log decription of instantiation of this.
     *
     * @param s  SSLSocketProviderDefaultDependencies instantiation log description
     */
    public void setDescription(String s)
        {
        m_sDescription = s;
        }

    // ----- Object methods ----------------------------------------------

   /**
    * {@inheritDoc}
    */
    public String toString()
        {
        return m_sDescription;
        }

    // ----- data members ------------------------------------------------
    /**
     * The description of the provider.
     */
    protected String m_sDescription = "SSLSocketProvider()";


    /**
     * SocketProviderFactory dependencies
     */
    protected SocketProviderFactory.Dependencies m_DependenciesProviderFactory;

    // ----- constants ------------------------------------------------------

     /**
     * The default SSL protocol.
     */
     public static final String DEFAULT_SSL_PROTOCOL = "TLS";

     /**
     * The default identity management algorithm.
     */
     public static final String DEFAULT_IDENTITY_ALGORITHM = "SunX509";

     /**
     * The default trust management algorithm.
     */
     public static final String DEFAULT_TRUST_ALGORITHM = "SunX509";

     /**
     * The default keystore type.
     */
     public static final String DEFAULT_KEYSTORE_TYPE = "JKS";

    /**
     * The default executor used by new SSLSocketProviders.
     */
    public static final Executor DEFAULT_EXECUTOR = Executors.
            newCachedThreadPool(new DaemonThreadFactory("SSLExecutor-"));
}
