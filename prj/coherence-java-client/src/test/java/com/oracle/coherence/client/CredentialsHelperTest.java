/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.client;

import com.oracle.coherence.common.net.SSLSocketProvider;

import com.tangosol.coherence.config.builder.SocketProviderBuilder;
import com.tangosol.internal.net.ssl.KeyManagersBuilder;
import com.tangosol.internal.net.ssl.SSLContextDependencies;
import com.tangosol.internal.net.ssl.TrustManagersBuilder;
import com.tangosol.net.SocketProviderFactory;
import com.tangosol.net.grpc.GrpcTransportSecurity;

import io.grpc.InsecureChannelCredentials;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManager;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link CredentialsHelper}.
 *
 * @author Aleks Seovic  2026.05.20
 * @since 26.07
 */
class CredentialsHelperTest
    {
    @Test
    void shouldCreateOptionalCleartextCredentials()
        {
        assertInstanceOf(InsecureChannelCredentials.class, CredentialsHelper.createChannelCredentials(null));
        }

    @Test
    void shouldCreateExplicitInsecureCredentials()
        {
        assertInstanceOf(InsecureChannelCredentials.class, CredentialsHelper.createChannelCredentials(grpcInsecureBuilder()));
        }

    @Test
    void shouldRejectRequiredCleartext()
        {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> CredentialsHelper.createChannelCredentials(null, GrpcTransportSecurity.SECURE_TRANSPORT_REQUIRED));

        assertEquals(GrpcTransportSecurity.SECURE_REQUIRED_MESSAGE, e.getMessage());
        }

    @Test
    void shouldRejectMissingSslDependencies()
        {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> CredentialsHelper.createChannelCredentials(missingSslBuilder()));

        assertEquals(GrpcTransportSecurity.MISSING_TLS_CREDENTIALS_MESSAGE, e.getMessage());
        }

    @Test
    void shouldCreateSslCredentials()
            throws Exception
        {
        assertNotNull(CredentialsHelper.createChannelCredentials(
                tlsBuilder(), GrpcTransportSecurity.SECURE_TRANSPORT_REQUIRED));
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
            throws Exception
        {
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());

        try (java.io.InputStream in = Files.newInputStream(findProjectRoot().resolve(
                "test/unit/coherence-tests/src/test/resources/internal/keystore.jks")))
            {
            keyStore.load(in, PASSWORD);
            }

        KeyManagerFactory factory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        factory.init(keyStore, PASSWORD);

        SSLSocketProvider.DefaultDependencies depsSSL = new SSLSocketProvider.DefaultDependencies();
        SSLContextDependencies                depsCtx = new SSLContextDependencies(null);
        depsCtx.setDependencies(depsSSL, null, null);
        setField(depsCtx, "m_keyManagersBuilder",
                (KeyManagersBuilder) (deps, sb) -> factory.getKeyManagers());
        setField(depsCtx, "m_trustManagersBuilder",
                (TrustManagersBuilder) (deps, sb) -> new TrustManager[0]);
        depsSSL.setSSLContextDependencies(depsCtx);

        SocketProviderFactory.DefaultDependencies depsFactory = new SocketProviderFactory.DefaultDependencies();
        depsFactory.addNamedSSLDependencies(SocketProviderFactory.Dependencies.ProviderType.SSL.getName(), depsSSL);
        return new SocketProviderBuilder(SocketProviderFactory.Dependencies.ProviderType.SSL.getName(), depsFactory, false);
        }

    private static void setField(Object target, String sName, Object value)
            throws Exception
        {
        Field field = SSLContextDependencies.class.getDeclaredField(sName);
        field.setAccessible(true);
        field.set(target, value);
        }

    private static Path findProjectRoot()
        {
        Path path = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (path != null)
            {
            if (Files.isDirectory(path.resolve("test/unit/coherence-tests/src/test/resources/internal")))
                {
                return path;
                }
            path = path.getParent();
            }
        throw new IllegalStateException("Cannot find project root from " + System.getProperty("user.dir"));
        }

    private static final char[] PASSWORD = "password".toCharArray();
    }
