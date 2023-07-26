/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.ssl;

import com.oracle.coherence.common.base.Logger;
import com.oracle.coherence.common.net.SSLSocketProvider;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLContextSpi;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSessionContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.Provider;
import java.security.SecureRandom;

/**
 * A custom {@link SSLContextSpi} used by Coherence to add additional
 * functionality to an SSLContext. For example auto-renewing keys and
 * certs.
 *
 * @author Jonathan Knight  2020.01.25
 * @since 22.06
 */
public class SSLContextSpiImpl
        extends SSLContextSpi
        implements SSLContextDependencies.Listener
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default empty constructor, required to meet the Provider SPI contract.
     */
    public SSLContextSpiImpl()
        {
        }

    // ----- SSLContextSpi methods ------------------------------------------

    @Override
    public void onUpdate(SSLContextDependencies dependencies) throws GeneralSecurityException
        {
        SSLContext ctx;
        Provider provider      = m_dependencies.getProvider();
        String   sProviderName = m_dependencies.getProviderName();
        String   sProtocol     = m_dependencies.getProtocol();
        if (provider != null)
            {
            ctx = SSLContext.getInstance(sProtocol, provider);
            }
        else if (sProviderName != null && !sProviderName.isBlank())
            {
            ctx = SSLContext.getInstance(sProtocol, sProviderName);
            }
        else
            {
            ctx = SSLContext.getInstance(sProtocol);
            }


        KeyManager[]   aKeyManager    = m_dependencies.getKeyManagers();
        TrustManager[] aTrustManagers = m_dependencies.getTrustManagers();
        SecureRandom   secureRandom   = m_dependencies.getSecureRandom();

        ctx.init(aKeyManager, aTrustManagers, secureRandom);

        SSLSocketProvider.ClientAuthMode mode = m_dependencies.getClientAuth();
        //noinspection EnhancedSwitchMigration
        switch (mode)
            {
            case wanted:
                ctx.getDefaultSSLParameters().setWantClientAuth(true);
                ctx.getDefaultSSLParameters().setNeedClientAuth(false);
                break;
            case required:
                ctx.getDefaultSSLParameters().setWantClientAuth(true);
                ctx.getDefaultSSLParameters().setNeedClientAuth(true);
                break;
            case none:
            default:
                ctx.getDefaultSSLParameters().setWantClientAuth(false);
                ctx.getDefaultSSLParameters().setNeedClientAuth(false);
                break;
            }

        m_sslContext = ctx;
        }

    @Override
    public void onError(SSLContextDependencies dependencies, Throwable t) throws KeyManagementException
        {
        if (m_sslContext == null)
            {
            throw new KeyManagementException("Could not create first SSLContext. Expect communication errors.", t);
            }
        else
            {
            Logger.err("Could not properly instantiate SSLContext. The existing SSLContext will be used", t);
            }
        }

    @Override
    protected void engineInit(KeyManager[] keyManagers, TrustManager[] trustManagers, SecureRandom secureRandom)
        {
        m_dependencies.init(keyManagers, trustManagers, secureRandom);
        }

    @Override
    protected SSLSocketFactory engineGetSocketFactory()
        {
        return m_sslContext.getSocketFactory();
        }

    @Override
    protected SSLServerSocketFactory engineGetServerSocketFactory()
        {
        return m_sslContext.getServerSocketFactory();
        }

    @Override
    protected SSLEngine engineCreateSSLEngine()
        {
        return m_sslContext.createSSLEngine();
        }

    @Override
    protected SSLEngine engineCreateSSLEngine(String remoteHost, int remotePort)
        {
        return m_sslContext.createSSLEngine(remoteHost, remotePort);
        }

    @Override
    protected SSLSessionContext engineGetServerSessionContext()
        {
        return m_sslContext.getServerSessionContext();
        }

    @Override
    protected SSLSessionContext engineGetClientSessionContext()
        {
        return m_sslContext.getClientSessionContext();
        }

    // ----- accessor methods -----------------------------------------------

    public void setDependencies(SSLContextDependencies dependencies)
        {
        m_dependencies = new SSLContextDependencies(dependencies, this);
        m_dependencies.init();
        }

    // ----- data members ---------------------------------------------------

    private SSLContextDependencies m_dependencies;

    private volatile SSLContext m_sslContext;
    }
