/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.grpc.proxy.helidon;

import com.oracle.coherence.common.net.SSLSocketProvider;

import com.tangosol.coherence.config.builder.SocketProviderBuilder;
import com.tangosol.net.SocketProviderFactory;
import com.tangosol.net.grpc.GrpcTransportSecurity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link HelidonCredentialsHelper}.
 *
 * @author Aleks Seovic  2026.05.20
 * @since 26.07
 */
class HelidonCredentialsHelperTest
    {
    @Test
    void shouldCreateOptionalCleartextConfig()
        {
        assertFalse(HelidonCredentialsHelper.createTlsConfig(null).isPresent());
        }

    @Test
    void shouldCreateExplicitInsecureConfig()
        {
        assertTrue(HelidonCredentialsHelper.createTlsConfig(grpcInsecureBuilder()).isPresent());
        }

    @Test
    void shouldRejectRequiredCleartext()
        {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> HelidonCredentialsHelper.createTlsConfig(null, GrpcTransportSecurity.SECURE_TRANSPORT_REQUIRED));

        assertEquals(GrpcTransportSecurity.SECURE_REQUIRED_MESSAGE, e.getMessage());
        }

    @Test
    void shouldRejectRequiredGrpcInsecure()
        {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> HelidonCredentialsHelper.createTlsConfig(
                        grpcInsecureBuilder(), GrpcTransportSecurity.SECURE_TRANSPORT_REQUIRED));

        assertEquals(GrpcTransportSecurity.SECURE_REQUIRED_MESSAGE, e.getMessage());
        }

    @Test
    void shouldRejectMissingSslDependencies()
        {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> HelidonCredentialsHelper.createTlsConfig(missingSslBuilder()));

        assertEquals(GrpcTransportSecurity.MISSING_TLS_CREDENTIALS_MESSAGE, e.getMessage());
        }

    @Test
    void shouldCreateSslConfig()
        {
        assertTrue(HelidonCredentialsHelper.createTlsConfig(tlsBuilder(),
                GrpcTransportSecurity.SECURE_TRANSPORT_REQUIRED).isPresent());
        }

    private static SocketProviderBuilder grpcInsecureBuilder()
        {
        SocketProviderFactory.DefaultDependencies deps = new SocketProviderFactory.DefaultDependencies();
        return new SocketProviderBuilder(SocketProviderFactory.Dependencies.ProviderType.GRPC.getName(), deps, false);
        }

    private static SocketProviderBuilder missingSslBuilder()
        {
        SocketProviderFactory.DefaultDependencies deps = new SocketProviderFactory.DefaultDependencies();
        return new SocketProviderBuilder(SocketProviderFactory.Dependencies.ProviderType.SSL.getName(), deps, false);
        }

    private static SocketProviderBuilder tlsBuilder()
        {
        SSLSocketProvider.DefaultDependencies depsSSL     = new SSLSocketProvider.DefaultDependencies();
        SocketProviderFactory.DefaultDependencies depsFactory = new SocketProviderFactory.DefaultDependencies();
        depsFactory.addNamedSSLDependencies(SocketProviderFactory.Dependencies.ProviderType.SSL.getName(), depsSSL);
        return new SocketProviderBuilder(SocketProviderFactory.Dependencies.ProviderType.SSL.getName(), depsFactory, false);
        }
    }
