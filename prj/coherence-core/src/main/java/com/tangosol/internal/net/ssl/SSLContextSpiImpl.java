/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.ssl;

import com.oracle.coherence.common.base.Exceptions;
import com.oracle.coherence.common.base.Logger;

import com.oracle.coherence.common.net.SSLSocketProvider;

import com.tangosol.net.ssl.RefreshPolicy;
import com.tangosol.util.Base;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLContextSpi;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSessionContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

import java.security.KeyManagementException;
import java.security.Provider;
import java.security.SecureRandom;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import java.util.concurrent.atomic.AtomicLong;

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
    protected void engineInit(KeyManager[] keyManagers, TrustManager[] trustManagers, SecureRandom secureRandom)
        {
        m_aKeyManager    = keyManagers;
        m_aTrustManagers = trustManagers;
        m_secureRandom   = secureRandom;
        try
            {
            updateSSLContext();

            if (m_nRefreshPeriodMillis > 0)
                {
                ScheduledExecutorService refreshExecutor = Executors.newSingleThreadScheduledExecutor(SSLContextSpiImpl::makeRefreshThread);
                refreshExecutor.scheduleAtFixedRate(this::onScheduledUpdate, m_nRefreshPeriodMillis, m_nRefreshPeriodMillis, TimeUnit.MILLISECONDS);
                }
            }
        catch (Exception e)
            {
            throw Exceptions.ensureRuntimeException(e);
            }
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

    protected void setDependencies(SSLSocketProviderDefaultDependencies deps, ManagerDependencies depsIdMgr, ManagerDependencies depsTrustMgr)
        {
        m_deps         = deps;
        m_depsIdMgr    = depsIdMgr;
        m_depsTrustMgr = depsTrustMgr;
        }

    protected void setPeerAuthentication(boolean fPeerAuthentication)
        {
        m_fPeerAuthentication = fPeerAuthentication;
        }

    protected boolean isPeerAuthentication()
        {
        return m_fPeerAuthentication;
        }

    protected void setProvider(Provider provider, String sProviderName)
        {
        m_provider      = provider;
        m_sProviderName = sProviderName;
        }

    protected void setRefreshPeriodInMillis(long nRefreshPeriodInMillis)
        {
        m_nRefreshPeriodMillis = nRefreshPeriodInMillis;
        }

    protected long getRefreshPeriodMillis()
        {
        return m_nRefreshPeriodMillis;
        }

    protected String getProtocol()
        {
        return m_sProtocol;
        }

    protected void setProtocol(String sProtocol)
        {
        m_sProtocol = sProtocol;
        }

    protected KeyManagersBuilder ensureKeyManagersBuilder()
        {
        if (m_keyManagersBuilder == null)
            {
            synchronized (this)
                {
                if (m_keyManagersBuilder == null)
                    {
                    KeyManagersBuilder builder;
                    if (m_provider instanceof KeyManagersBuilder)
                        {
                        builder = (KeyManagersBuilder) m_provider;
                        }
                    else
                        {
                        builder = new DefaultKeyManagerBuilder();
                        }
                    m_keyManagersBuilder = builder;
                    }
                }
            }
        return m_keyManagersBuilder;
        }

    protected void setKeyManagersBuilder(KeyManagersBuilder keyManagersBuilder)
        {
        m_keyManagersBuilder = keyManagersBuilder;
        }

    protected TrustManagersBuilder ensureTrustManagersBuilder()
        {
        if (m_trustManagersBuilder == null)
            {
            synchronized (this)
                {
                if (m_trustManagersBuilder == null)
                    {
                    TrustManagersBuilder builder;
                    if (m_provider instanceof TrustManagersBuilder)
                        {
                        builder = (TrustManagersBuilder) m_provider;
                        }
                    else
                        {
                        builder = new DefaultTrustManagerBuilder();
                        }
                    m_trustManagersBuilder = builder;
                    }
                }
            }
        return m_trustManagersBuilder;
        }

    protected void setTrustManagersBuilder(TrustManagersBuilder trustManagersBuilder)
        {
        m_trustManagersBuilder = trustManagersBuilder;
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Create a {@link Thread} used to run the refresh.
     *
     * @param runnable  the refresh {@link Runnable}
     *
     * @return a {@link Thread} used to run the refresh
     */
    protected static Thread makeRefreshThread(Runnable runnable)
        {
        String sName = "SSLContextRefreshThread:" + INSTANCE_COUNT.incrementAndGet();
        return Base.makeThread(null, runnable, sName);
        }

    /**
     * Perform a scheduled refresh.
     */
    protected void onScheduledUpdate()
        {
        try
            {
            RefreshPolicy policy = m_deps.getRefreshPolicy();
            if (policy == null || policy.shouldRefresh(m_deps, m_depsIdMgr, m_depsTrustMgr))
                {
                updateSSLContext();
                }
            }
        catch (Throwable t)
            {
            Logger.err("Failed to update keystores", t);
            }
        }

    protected synchronized void updateSSLContext() throws KeyManagementException
        {
        try
            {
            StringBuilder        sbDesc               = new StringBuilder();
            KeyManagersBuilder   keyManagersBuilder   = ensureKeyManagersBuilder();
            TrustManagersBuilder trustManagersBuilder = ensureTrustManagersBuilder();

            if (!keyManagersBuilder.isRefreshable(m_depsIdMgr) && !trustManagersBuilder.isRefreshable(m_depsTrustMgr))
                {
                // nothing to refresh
                return;
                }

            m_aKeyManager         = keyManagersBuilder.buildKeyManagers(m_depsIdMgr, sbDesc);
            m_aTrustManagers      = trustManagersBuilder.buildTrustManagers(m_depsTrustMgr, sbDesc.append(", "));
            m_fPeerAuthentication = m_aTrustManagers != null;

            m_deps.setClientAuthenticationRequired(m_fPeerAuthentication);

            SSLContext ctx;
            if (m_provider != null)
                {
                ctx = SSLContext.getInstance(m_sProtocol, m_provider);
                }
            else if (m_sProviderName != null && !m_sProviderName.isEmpty())
                {
                ctx = SSLContext.getInstance(m_sProtocol, m_sProviderName);
                }
            else
                {
                ctx = SSLContext.getInstance(m_sProtocol);
                }

            ctx.init(m_aKeyManager, m_aTrustManagers, m_secureRandom);

            logDescription(sbDesc);

            m_sslContext = ctx;
            }
        catch (Throwable e)
            {
            if (m_sslContext == null)
                {
                throw new KeyManagementException("Could not create first SSLContext. Expect communication errors.", e);
                }
            else
                {
                Logger.err("Could not properly instantiate SSLContext. The existing SSLContext will be used", e);
                }
            }
        }

    protected void logDescription(StringBuilder sbDesc)
        {
        if (m_deps.getHostnameVerifier() != null)
            {
            sbDesc.append(", hostname-verifier=custom");
            }

        String sAuth = m_aKeyManager == null && m_aTrustManagers == null
                       ? "none"
                       : m_aKeyManager == null
                         ? "one-way client"
                         : m_aTrustManagers == null ? "one-way server" : "two-way";

        m_deps.setDescription(sbDesc.insert(0, "SSLSocketProvider(auth=" + sAuth + ", ").append(')').toString());
        Logger.fine("instantiated SSLSocketProviderDependencies: " + sbDesc);
        }

    // ----- data members ---------------------------------------------------

    private volatile SecureRandom m_secureRandom;

    private volatile KeyManager[] m_aKeyManager;

    private volatile TrustManager[] m_aTrustManagers;

    private volatile SSLContext m_sslContext;

    private volatile KeyManagersBuilder m_keyManagersBuilder;

    private volatile TrustManagersBuilder m_trustManagersBuilder;

    private SSLSocketProvider.Dependencies m_deps;

    private ManagerDependencies m_depsIdMgr;

    private ManagerDependencies m_depsTrustMgr;

    private String m_sProtocol;

    private Provider m_provider;

    private String m_sProviderName;

    private boolean m_fPeerAuthentication;

    private long m_nRefreshPeriodMillis;

    private static final AtomicLong INSTANCE_COUNT = new AtomicLong(0L);
    }
