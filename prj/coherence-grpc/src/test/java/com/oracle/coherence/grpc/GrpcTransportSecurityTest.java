/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc;

import com.tangosol.coherence.config.builder.SocketProviderBuilder;
import com.tangosol.net.SocketProviderFactory;
import com.tangosol.net.grpc.GrpcTransportSecurity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link GrpcTransportSecurity}.
 *
 * @author Aleks Seovic  2026.05.20
 * @since 26.04
 */
class GrpcTransportSecurityTest
    {
    @Test
    void shouldNormalizeSecureTransportValues()
        {
        assertEquals(GrpcTransportSecurity.SECURE_TRANSPORT_OPTIONAL, GrpcTransportSecurity.normalize(null));
        assertEquals(GrpcTransportSecurity.SECURE_TRANSPORT_OPTIONAL, GrpcTransportSecurity.normalize(" "));
        assertEquals(GrpcTransportSecurity.SECURE_TRANSPORT_REQUIRED, GrpcTransportSecurity.normalize(" Required "));
        assertThrows(IllegalArgumentException.class, () -> GrpcTransportSecurity.normalize("mandatory"));
        }

    @Test
    void shouldClassifyNullBuilderAsCompatibilityCleartext()
        {
        assertEquals(GrpcTransportSecurity.Transport.INSECURE_COMPAT, GrpcTransportSecurity.resolve(null));
        }

    @Test
    void shouldClassifyGrpcInsecureAsExplicitCleartext()
        {
        SocketProviderFactory.DefaultDependencies deps = new SocketProviderFactory.DefaultDependencies();
        SocketProviderBuilder builder = new SocketProviderBuilder(
                SocketProviderFactory.Dependencies.ProviderType.GRPC.getName(), deps, false);

        assertEquals(GrpcTransportSecurity.Transport.INSECURE_EXPLICIT, GrpcTransportSecurity.resolve(builder));
        }

    @Test
    void shouldClassifySslWithoutDependenciesAsMisconfiguredSecure()
        {
        SocketProviderFactory.DefaultDependencies deps = new SocketProviderFactory.DefaultDependencies();
        SocketProviderBuilder builder = new SocketProviderBuilder(
                SocketProviderFactory.Dependencies.ProviderType.SSL.getName(), deps, false);

        assertEquals(GrpcTransportSecurity.Transport.MISCONFIGURED_SECURE, GrpcTransportSecurity.resolve(builder));
        }

    @Test
    void shouldRejectRequiredWithoutTlsCredentials()
        {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> GrpcTransportSecurity.enforce(null, GrpcTransportSecurity.SECURE_TRANSPORT_REQUIRED));

        assertEquals(GrpcTransportSecurity.SECURE_REQUIRED_MESSAGE, e.getMessage());
        }

    @Test
    void shouldRejectSslProviderWithoutDependenciesWhenOptional()
        {
        SocketProviderFactory.DefaultDependencies deps = new SocketProviderFactory.DefaultDependencies();
        SocketProviderBuilder builder = new SocketProviderBuilder(
                SocketProviderFactory.Dependencies.ProviderType.SSL.getName(), deps, false);

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> GrpcTransportSecurity.enforce(builder, GrpcTransportSecurity.SECURE_TRANSPORT_OPTIONAL));

        assertEquals(GrpcTransportSecurity.MISSING_TLS_CREDENTIALS_MESSAGE, e.getMessage());
        }
    }
