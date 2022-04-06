/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.ssl;

import com.oracle.coherence.common.util.Duration;

import java.security.NoSuchAlgorithmException;
import java.security.Provider;

import java.util.Collections;
import java.util.Objects;

/**
 * A custom SSLContext {@link Provider} used by Coherence to add additional
 * functionality to an SSLContext. For example auto-renewing keys and certs.
 *
 * @author Jonathan Knight  2020.01.25
 * @since 22.06
 */
public class SSLContextProvider
        extends Provider
    {
    /**
     * Create a Coherence {@link SSLContextProvider}.
     *
     * @param sProtocol           the protocol to use (if {@code null} then defaults
     *                            to {@link SSLSocketProviderDefaultDependencies#DEFAULT_SSL_PROTOCOL})
     * @param provider            an optional {@link Provider} to use to provide the {@link javax.net.ssl.SSLContext}
     * @param sProviderName       an optional {@link Provider} name to use to provide the {@link javax.net.ssl.SSLContext}
     * @param depsSocketProvider  the socket provider dependencies
     * @param depsIdMgr           the identity manager dependencies
     * @param depsTrustMgr        the trust manager dependencies
     */
    public SSLContextProvider(String                               sProtocol,
                              Provider                             provider,
                              String                               sProviderName,
                              SSLSocketProviderDefaultDependencies depsSocketProvider,
                              ManagerDependencies                  depsIdMgr,
                              ManagerDependencies                  depsTrustMgr)
        {
        super(NAME, 1.0, "This provider provides the default Coherence SSLContext");

        m_provider           = provider;
        m_sProviderName      = sProviderName;
        m_depsSocketProvider = depsSocketProvider;
        m_depsIdMgr          = depsIdMgr;
        m_depsTrustMgr       = depsTrustMgr;

        if (sProtocol == null || sProtocol.isEmpty())
            {
            sProtocol = SSLSocketProviderDefaultDependencies.DEFAULT_SSL_PROTOCOL;
            }

        putService(new SSLContextService(this, sProtocol));
        }

    // ----- inner class: SSLContextService ---------------------------------

    /**
     * A custom {@link java.security.Provider.Service} used to create
     * a custom {@link javax.net.ssl.SSLContext}
     */
    protected class SSLContextService
            extends Service
        {
        /**
         * Construct a new service.
         *
         * @param provider   the provider that offers this service
         * @param sProtocol  the protocol name
         *
         * @throws NullPointerException if any of the parameters are null
         */
        protected SSLContextService(Provider provider, String sProtocol)
            {
            super(provider, SERVICE_TYPE, Objects.requireNonNull(sProtocol), SSLContextSpiImpl.class.getName(),
                    Collections.emptyList(), Collections.emptyMap());

            m_sProtocol = sProtocol;
            }

        // ----- Service methods --------------------------------------------

        @Override
        public Object newInstance(Object constructorParameter) throws NoSuchAlgorithmException
            {
            SSLContextSpiImpl sslContext = (SSLContextSpiImpl) super.newInstance(constructorParameter);

            sslContext.setProtocol(m_sProtocol);
            sslContext.setProvider(m_provider, m_sProviderName);
            sslContext.setDependencies(m_depsSocketProvider, m_depsIdMgr, m_depsTrustMgr);
            sslContext.setPeerAuthentication(m_depsSocketProvider.isClientAuthenticationRequired());
            sslContext.setRefreshPeriodInMillis(m_depsSocketProvider.getRefreshPeriod().as(Duration.Magnitude.MILLI));

            return sslContext;
            }

        // ----- data members -----------------------------------------------

        /**
         * The SSL protocol name.
         */
        private final String m_sProtocol;
        }

    // ----- constants ------------------------------------------------------

    /**
     * The name of this service.
     */
    public static final String NAME = "CoherenceSSLContextProvider";

    /**
     * The type of the service.
     */
    public static final String SERVICE_TYPE = "SSLContext";

    // ----- data members ---------------------------------------------------

    /**
     * An optional {@link Provider} to use to provide the {@link javax.net.ssl.SSLContext}.
     */
    private final Provider m_provider;

    /**
     * An optional {@link Provider} name to use to provide the {@link javax.net.ssl.SSLContext}.
     */
    private final String m_sProviderName;

    /**
     * The socket provider dependencies.
     */
    private final SSLSocketProviderDefaultDependencies m_depsSocketProvider;

    /**
     * The identity manager dependencies.
     */
    private final ManagerDependencies m_depsIdMgr;

    /**
     * The trust manager dependencies.
     */
    private final ManagerDependencies m_depsTrustMgr;
    }
