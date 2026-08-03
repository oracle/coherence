/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.grpc;

import com.oracle.coherence.common.net.SSLSocketProvider;

import com.tangosol.coherence.config.builder.SocketProviderBuilder;
import com.tangosol.net.SocketProviderFactory;

import java.util.Locale;

/**
 * Helper methods for gRPC transport security configuration.
 *
 * @author Aleks Seovic  2026.05.20
 * @since 26.07
 */
public final class GrpcTransportSecurity
    {
    /**
     * Utility class.
     */
    private GrpcTransportSecurity()
        {
        }

    /**
     * Normalize a configured secure-transport policy value.
     *
     * @param sValue  the configured value
     *
     * @return the normalized value
     */
    public static String normalize(String sValue)
        {
        if (sValue == null || sValue.isBlank())
            {
            return SECURE_TRANSPORT_OPTIONAL;
            }

        String sNormalized = sValue.trim().toLowerCase(Locale.ROOT);
        if (SECURE_TRANSPORT_OPTIONAL.equals(sNormalized) || SECURE_TRANSPORT_REQUIRED.equals(sNormalized))
            {
            return sNormalized;
            }
        throw new IllegalArgumentException("unsupported gRPC secure-transport: " + sValue);
        }

    /**
     * Return whether a secure transport policy requires TLS credentials.
     *
     * @param sSecureTransport  the configured secure-transport policy
     *
     * @return {@code true} if TLS credentials are required
     */
    public static boolean isRequired(String sSecureTransport)
        {
        return SECURE_TRANSPORT_REQUIRED.equals(normalize(sSecureTransport));
        }

    /**
     * Resolve the security posture of a gRPC socket provider.
     *
     * @param socketBuilder  the socket provider builder
     *
     * @return the resolved transport security
     */
    public static Transport resolve(SocketProviderBuilder socketBuilder)
        {
        if (socketBuilder == null)
            {
            return Transport.INSECURE_COMPAT;
            }

        SocketProviderFactory.Dependencies depsFactory = socketBuilder.getDependencies();
        if (depsFactory == null)
            {
            return Transport.INSECURE_COMPAT;
            }

        String                                          sSocketId = socketBuilder.getId();
        SocketProviderFactory.Dependencies.ProviderType type      = depsFactory.getProviderType(sSocketId);
        if (type == SocketProviderFactory.Dependencies.ProviderType.GRPC)
            {
            return Transport.INSECURE_EXPLICIT;
            }

        if (depsFactory.getSSLDependencies(sSocketId) != null)
            {
            return Transport.TLS;
            }

        return type == SocketProviderFactory.Dependencies.ProviderType.SSL
                ? Transport.MISCONFIGURED_SECURE
                : Transport.INSECURE_COMPAT;
        }

    /**
     * Resolve and enforce the configured secure-transport policy.
     *
     * @param socketBuilder      the socket provider builder
     * @param sSecureTransport  the configured secure-transport policy
     *
     * @return the resolved transport security
     */
    public static Transport enforce(SocketProviderBuilder socketBuilder, String sSecureTransport)
        {
        String    sPolicy   = normalize(sSecureTransport);
        Transport transport = resolve(socketBuilder);

        if (transport == Transport.MISCONFIGURED_SECURE)
            {
            throw new IllegalStateException(MISSING_TLS_CREDENTIALS_MESSAGE);
            }

        if (SECURE_TRANSPORT_REQUIRED.equals(sPolicy) && transport != Transport.TLS)
            {
            throw new IllegalStateException(SECURE_REQUIRED_MESSAGE);
            }

        return transport;
        }

    /**
     * Return the SSL dependencies for a socket provider builder.
     *
     * @param socketBuilder  the socket provider builder
     *
     * @return the SSL dependencies, or {@code null}
     */
    public static SSLSocketProvider.Dependencies getSSLDependencies(SocketProviderBuilder socketBuilder)
        {
        SocketProviderFactory.Dependencies depsFactory = socketBuilder == null ? null : socketBuilder.getDependencies();
        return depsFactory == null ? null : depsFactory.getSSLDependencies(socketBuilder.getId());
        }

    /**
     * Secure transport is optional; cleartext remains compatible.
     */
    public static final String SECURE_TRANSPORT_OPTIONAL = "optional";

    /**
     * Secure transport is required; cleartext is rejected.
     */
    public static final String SECURE_TRANSPORT_REQUIRED = "required";

    /**
     * Server-side secure-transport system property.
     */
    public static final String PROP_SERVER_SECURE_TRANSPORT = "coherence.grpc.server.secure-transport";

    /**
     * Bounded error message for required TLS without TLS credentials.
     */
    public static final String SECURE_REQUIRED_MESSAGE =
            "secure gRPC transport is required but TLS credentials are not configured";

    /**
     * Bounded error message for a secure socket provider without TLS credentials.
     */
    public static final String MISSING_TLS_CREDENTIALS_MESSAGE =
            "gRPC secure transport socket provider is configured but TLS credentials are not configured";

    /**
     * Resolved gRPC transport security posture.
     */
    public enum Transport
        {
        /**
         * TLS credentials are configured.
         */
        TLS,

        /**
         * Cleartext was explicitly selected with the gRPC insecure provider.
         */
        INSECURE_EXPLICIT,

        /**
         * Cleartext is selected by compatibility defaults.
         */
        INSECURE_COMPAT,

        /**
         * A secure provider was selected but TLS credentials are missing.
         */
        MISCONFIGURED_SECURE
        }
    }
