/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.ssl;

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
     * @param depsSocketProvider  the socket provider dependencies
     */
    public SSLContextProvider(String                               sProtocol,
                              SSLSocketProviderDefaultDependencies depsSocketProvider)
        {
        super(NAME, 1.0, "This provider provides the default Coherence SSLContext");

        m_depsSocketProvider = depsSocketProvider;

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
            SSLContextDependencies dependencies = m_depsSocketProvider.getSSLContextDependencies();
            dependencies.setProtocol(m_sProtocol);
            sslContext.setDependencies(dependencies);
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
     * The socket provider dependencies.
     */
    private final SSLSocketProviderDefaultDependencies m_depsSocketProvider;
    }
