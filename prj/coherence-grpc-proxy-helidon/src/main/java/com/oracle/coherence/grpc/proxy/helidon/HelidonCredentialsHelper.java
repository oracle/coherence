/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.proxy.helidon;

import com.oracle.coherence.common.net.SSLSocketProvider;

import com.tangosol.coherence.config.builder.SocketProviderBuilder;

import com.tangosol.net.grpc.GrpcTransportSecurity;

import io.helidon.common.tls.TlsConfig;

import javax.net.ssl.SSLContext;
import java.util.Optional;

/**
 * A helper to create credential for Helidon.
 */
public class HelidonCredentialsHelper
    {
    /**
     * Create a Helidon {@link TlsConfig} from a {@link SocketProviderBuilder}.
     *
     * @param socketBuilder  the {@link SocketProviderBuilder} to use to create the TLS configuration
     *
     * @return a Helidon {@link TlsConfig}
     */
    public static Optional<TlsConfig> createTlsConfig(SocketProviderBuilder socketBuilder)
        {
        return createTlsConfig(socketBuilder, GrpcTransportSecurity.SECURE_TRANSPORT_OPTIONAL);
        }

    /**
     * Create a Helidon {@link TlsConfig} from a {@link SocketProviderBuilder}.
     *
     * @param socketBuilder      the {@link SocketProviderBuilder} to use to create the TLS configuration
     * @param sSecureTransport  the secure transport policy
     *
     * @return a Helidon {@link TlsConfig}
     */
    public static Optional<TlsConfig> createTlsConfig(SocketProviderBuilder socketBuilder, String sSecureTransport)
        {
        GrpcTransportSecurity.Transport transport = GrpcTransportSecurity.enforce(socketBuilder, sSecureTransport);
        if (transport == GrpcTransportSecurity.Transport.INSECURE_EXPLICIT)
            {
            return Optional.of(TlsConfig.builder().enabled(false).buildPrototype());
            }

        if (transport == GrpcTransportSecurity.Transport.TLS)
            {
            SSLSocketProvider.Dependencies dependencies = GrpcTransportSecurity.getSSLDependencies(socketBuilder);
            SSLContext                     sslContext   = dependencies.getSSLContext();
            TlsConfig                      tlsConfig    = TlsConfig.builder()
                    .sslContext(sslContext)
                    .buildPrototype();

            return Optional.of(tlsConfig);
            }

        return Optional.empty();
        }
    }
