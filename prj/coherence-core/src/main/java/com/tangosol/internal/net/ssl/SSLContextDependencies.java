/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.ssl;

import com.oracle.coherence.common.base.Exceptions;
import com.oracle.coherence.common.base.Logger;
import com.oracle.coherence.common.net.SSLSocketProvider;
import com.tangosol.net.ssl.RefreshPolicy;
import com.tangosol.util.Base;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManager;
import javax.net.ssl.TrustManager;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.Provider;
import java.security.SecureRandom;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The dependencies required for building SSL contexts.
 *
 * @author Jonathan Knight  2022.08.25
 * @since 22.06.2
 */
public class SSLContextDependencies
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a {@link SSLContextDependencies}.
     *
     * @param listener a consumer that will be called whenever the ssl context
     *                       dependencies are updated
     */
    public SSLContextDependencies(Listener listener)
        {
        m_listener = listener;
        }

    /**
     * Copy constructor.
     *
     * @param deps       the dependencies to copy
     * @param listener   the listener for the new dependencies
     */
    public SSLContextDependencies(SSLContextDependencies deps, Listener listener)
        {
        this(listener);
        if (deps != null)
            {
            m_secureRandom         = deps.m_secureRandom;
            m_aKeyManager          = deps.m_aKeyManager;
            m_aTrustManagers       = deps.m_aTrustManagers;
            m_keyManagersBuilder   = deps.m_keyManagersBuilder;
            m_trustManagersBuilder = deps.m_trustManagersBuilder;
            m_deps                 = deps.m_deps;
            m_depsIdMgr            = deps.m_depsIdMgr;
            m_depsTrustMgr         = deps.m_depsTrustMgr;
            m_sProtocol            = deps.m_sProtocol;
            m_provider             = deps.m_provider;
            m_sProviderName        = deps.m_sProviderName;
            m_clientAuthMode       = deps.m_clientAuthMode;
            m_fClientAuthModeUnset = deps.m_clientAuthMode == null;
            m_nRefreshPeriodMillis = deps.m_nRefreshPeriodMillis;
            }
        }

    // ----- SSLContextDependencies methods ---------------------------------

    /**
     * Initialise the context.
     */
    public void init()
        {
        init(m_aKeyManager, m_aTrustManagers, m_secureRandom);
        }

    /**
     * Initialise the context.
     *
     * @param keyManagers    the array of {@link KeyManager key managers} to use
     * @param trustManagers  the array of {@link TrustManager trust managers} to use
     * @param secureRandom   the {@link SecureRandom} to use
     */
    protected void init(KeyManager[] keyManagers, TrustManager[] trustManagers, SecureRandom secureRandom)
        {
        m_aKeyManager    = keyManagers;
        m_aTrustManagers = trustManagers;
        m_secureRandom   = secureRandom;
        try
            {
            update();

            if (m_nRefreshPeriodMillis > 0)
                {
                ScheduledExecutorService refreshExecutor = Executors.newSingleThreadScheduledExecutor(SSLContextDependencies::makeRefreshThread);
                refreshExecutor.scheduleAtFixedRate(this::onScheduledUpdate, m_nRefreshPeriodMillis, m_nRefreshPeriodMillis, TimeUnit.MILLISECONDS);
                }
            }
        catch (Exception e)
            {
            throw Exceptions.ensureRuntimeException(e);
            }
        }

    // ----- accessor methods -----------------------------------------------

    /**
     * Set the dependencies to use for the SSL context.
     *
     * @param deps          the {@link SSLSocketProvider.Dependencies socket provider} dependencies
     * @param depsIdMgr     the {@link ManagerDependencies key manager dependencies}
     * @param depsTrustMgr  the {@link ManagerDependencies trust manager dependencies}
     */
    public void setDependencies(SSLSocketProvider.Dependencies deps, ManagerDependencies depsIdMgr, ManagerDependencies depsTrustMgr)
        {
        m_deps         = deps;
        m_depsIdMgr    = depsIdMgr;
        m_depsTrustMgr = depsTrustMgr;
        }

    /**
     * Return the {@link SecureRandom} to use.
     *
     * @return  the {@link SecureRandom} to use
     */
    public SecureRandom getSecureRandom()
        {
        return m_secureRandom;
        }

    /**
     * Set the {@link SecureRandom} to use.
     *
     * @param secureRandom  the {@link SecureRandom} to use
     */
    public void setSecureRandom(SecureRandom secureRandom)
        {
        m_secureRandom = secureRandom;
        }

    /**
     * Return the array of {@link KeyManager key managers} to use.
     *
     * @return the array of {@link KeyManager key managers} to use
     */
    public KeyManager[] getKeyManagers()
        {
        return m_aKeyManager;
        }

    /**
     * Return the array of {@link TrustManager trust managers} to use.
     *
     * @return the array of {@link TrustManager trust managers} to use
     */
    public TrustManager[] getTrustManagers()
        {
        return m_aTrustManagers;
        }

    /**
     * Return the {@link HostnameVerifier} to use.
     *
     * @return  the {@link HostnameVerifier} to use
     */
    public HostnameVerifier getHostnameVerifier()
        {
        return m_deps.getHostnameVerifier();
        }

    /**
     * Set the client authentication mode.
     *
     * @param mode  the {@link com.oracle.coherence.common.net.SSLSocketProvider.ClientAuthMode}
     */
    public void setClientAuth(SSLSocketProvider.ClientAuthMode mode)
        {
        m_fClientAuthModeUnset = mode == null;
        m_clientAuthMode       = mode;
        }

    /**
     * Return the client authentication mode.
     *
     * @return  the {@link com.oracle.coherence.common.net.SSLSocketProvider.ClientAuthMode}
     */
    public SSLSocketProvider.ClientAuthMode getClientAuth()
        {
        return m_clientAuthMode;
        }

    /**
     * Set the Java security provider and name
     *
     * @param provider       the {@link Provider} implementation
     * @param sProviderName  the provider name
     */
    public void setProvider(Provider provider, String sProviderName)
        {
        m_provider      = provider;
        m_sProviderName = sProviderName;
        }

    /**
     * Return the {@link Provider} implementation.
     *
     * @return the {@link Provider} implementation
     */
    public Provider getProvider()
        {
        return m_provider;
        }

    /**
     * Return the provider name.
     *
     * @return the provider name
     */
    public String getProviderName()
        {
        return m_sProviderName;
        }

    /**
     * Set the context refresh period, in milliseconds.
     *
     * @param nRefreshPeriodInMillis  the context refresh period, in milliseconds
     */
    public void setRefreshPeriodInMillis(long nRefreshPeriodInMillis)
        {
        m_nRefreshPeriodMillis = nRefreshPeriodInMillis;
        }

    /**
     * Returns the context refresh period, in milliseconds.
     *
     * @return  the context refresh period, in milliseconds
     */
    public long getRefreshPeriodMillis()
        {
        return m_nRefreshPeriodMillis;
        }

    /**
     * Return the set of enabled SSL cipher suites.
     *
     * @return the enabled SSL cipher suites, or {@code null} for default
     */
    public String[] getEnabledCipherSuites()
        {
        return m_deps.getEnabledCipherSuites();
        }

    /**
     * Set the security protocol to use, the default is TLS.
     *
     * @param sProtocol  the security protocol to use
     */
    public void setProtocol(String sProtocol)
        {
        m_sProtocol = sProtocol;
        }

    /**
     * Return the security protocol to use, the default is TLS.
     *
     * @return the security protocol to use
     */
    public String getProtocol()
        {
        if (m_sProtocol == null || m_sProtocol.isEmpty())
            {
            return SSLSocketProviderDefaultDependencies.DEFAULT_SSL_PROTOCOL;
            }
        return m_sProtocol;
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Ensure the {@link KeyManagersBuilder} field is set.
     *
     * @return  the {@link KeyManagersBuilder}
     */
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

    /**
     * Set the {@link KeyManagersBuilder}.
     *
     * @param keyManagersBuilder  the {@link KeyManagersBuilder}
     */
    protected void setKeyManagersBuilder(KeyManagersBuilder keyManagersBuilder)
        {
        m_keyManagersBuilder = keyManagersBuilder;
        }

    /**
     * Ensure the {@link TrustManagersBuilder} field is set.
     *
     * @return  the {@link TrustManagersBuilder}
     */
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

    /**
     * Set the {@link TrustManagersBuilder}.
     *
     * @param trustManagersBuilder  the {@link TrustManagersBuilder}
     */
    protected void setTrustManagersBuilder(TrustManagersBuilder trustManagersBuilder)
        {
        m_trustManagersBuilder = trustManagersBuilder;
        }

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
                update();
                }
            }
        catch (Throwable t)
            {
            Logger.err("Failed to update keystores", t);
            }
        }

    /**
     * Update the SSL context.
     *
     * @throws KeyManagementException if an error occurs
     */
    protected synchronized void update() throws KeyManagementException
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

            m_aKeyManager    = keyManagersBuilder.buildKeyManagers(m_depsIdMgr, sbDesc);
            m_aTrustManagers = trustManagersBuilder.buildTrustManagers(m_depsTrustMgr, sbDesc.append(", "));

            if (m_fClientAuthModeUnset)
                {
                m_clientAuthMode = m_aTrustManagers == null || m_aTrustManagers.length == 0
                        ? SSLSocketProvider.ClientAuthMode.none
                        : SSLSocketProvider.ClientAuthMode.required;
                }
            m_deps.setClientAuth(m_clientAuthMode);

            if (m_listener != null)
                {
                m_listener.onUpdate(this);
                }
            logDescription(sbDesc);
            }
        catch (Throwable e)
            {
            if (m_listener != null)
                {
                m_listener.onError(this, e);
                }
            else
                {
                throw new KeyManagementException("Could not create SSLContext dependencies", e);
                }
            }
        }

    /**
     * Log a description of the SSL context.
     *
     * @param sbDesc  the {@link StringBuilder} containing the description
     */
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
                         : m_aTrustManagers == null ? "one-way server"
                            : m_clientAuthMode == SSLSocketProvider.ClientAuthMode.none ? "one-way" : "two-way";

        sbDesc.insert(0, "SSLSocketProvider(auth=" + sAuth + ", ")
                .append(", clientAuth=").append(m_clientAuthMode)
                .append(')');

        m_deps.setDescription(sbDesc.toString());
        Logger.fine("instantiated SSLSocketProviderDependencies: " + sbDesc);
        }

    // ----- inner class: Listener ------------------------------------------

    /**
     * A listener that will be notified of SSL context updates.
     */
    public interface Listener
        {
        /**
         * Called when the SSL context has been updated.
         *
         * @param dependencies  the updated dependencies
         *
         * @throws GeneralSecurityException if an error occurs
         */
        void onUpdate(SSLContextDependencies dependencies) throws GeneralSecurityException;

        /**
         * Called when an error occurred updating the SSL context.
         *
         * @param dependencies  the updated dependencies
         *
         * @throws KeyManagementException if an error occurs
         */
        void onError(SSLContextDependencies dependencies, Throwable t) throws KeyManagementException;
        }

    // ----- data members ---------------------------------------------------

    private volatile SecureRandom m_secureRandom;

    private volatile KeyManager[] m_aKeyManager;

    private volatile TrustManager[] m_aTrustManagers;

    private volatile KeyManagersBuilder m_keyManagersBuilder;

    private volatile TrustManagersBuilder m_trustManagersBuilder;

    private SSLSocketProvider.Dependencies m_deps;

    private ManagerDependencies m_depsIdMgr;

    private ManagerDependencies m_depsTrustMgr;

    private String m_sProtocol;

    private Provider m_provider;

    private String m_sProviderName;

    private SSLSocketProvider.ClientAuthMode m_clientAuthMode;

    private boolean m_fClientAuthModeUnset;

    private long m_nRefreshPeriodMillis;

    private static final AtomicLong INSTANCE_COUNT = new AtomicLong(0L);

    private final Listener m_listener;
    }
