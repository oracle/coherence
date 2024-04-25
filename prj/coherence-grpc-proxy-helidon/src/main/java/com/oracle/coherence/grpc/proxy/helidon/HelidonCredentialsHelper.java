/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.proxy.helidon;

import com.oracle.coherence.common.net.SSLSocketProvider;

import com.tangosol.coherence.config.builder.SocketProviderBuilder;

import com.tangosol.net.SocketProviderFactory;

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
        if (socketBuilder != null)
            {
            SocketProviderFactory.Dependencies depsFactory = socketBuilder.getDependencies();
            if (depsFactory == null)
                {
                return Optional.empty();
                }

            String                                          sSocketId   = socketBuilder.getId();
            SocketProviderFactory.Dependencies.ProviderType type        = depsFactory.getProviderType(sSocketId);

            if (type == SocketProviderFactory.Dependencies.ProviderType.GRPC)
                {
                return Optional.of(TlsConfig.builder().enabled(false).buildPrototype());
                }

            SSLSocketProvider.Dependencies dependencies = depsFactory.getSSLDependencies(sSocketId);
            if (dependencies != null)
                {
                SSLContext sslContext = dependencies.getSSLContext();
                TlsConfig tlsConfig = TlsConfig.builder()
                        .sslContext(sslContext)
                        .buildPrototype();

                return Optional.of(tlsConfig);
                }
            }

        return Optional.empty();
        }
    }
