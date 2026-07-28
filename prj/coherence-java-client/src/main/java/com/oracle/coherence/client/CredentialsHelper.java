/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.client;

import com.oracle.coherence.common.net.SSLSocketProvider;
import com.tangosol.coherence.config.builder.SocketProviderBuilder;
import com.tangosol.internal.net.ssl.SSLContextDependencies;
import com.tangosol.net.grpc.GrpcTransportSecurity;

import io.grpc.ChannelCredentials;
import io.grpc.InsecureChannelCredentials;
import io.grpc.netty.NettySslContextChannelCredentials;

/**
 * A helper class to resolve gRPC credentials.
 */
public class CredentialsHelper
    {
    /**
     * Private constructor for utility class.
     */
    private CredentialsHelper()
        {
        }

    /**
     * Create the {@link ChannelCredentials} to use for the client channel.
     *
     * @param socketBuilder  the channel {@link SocketProviderBuilder}
     *
     * @return the {@link ChannelCredentials} to use for the client channel.
     */
    public static ChannelCredentials createChannelCredentials(SocketProviderBuilder socketBuilder)
        {
        return createChannelCredentials(socketBuilder, GrpcTransportSecurity.SECURE_TRANSPORT_OPTIONAL);
        }

    /**
     * Create the {@link ChannelCredentials} to use for the client channel.
     *
     * @param socketBuilder      the channel {@link SocketProviderBuilder}
     * @param sSecureTransport  the secure transport policy
     *
     * @return the {@link ChannelCredentials} to use for the client channel.
     */
    public static ChannelCredentials createChannelCredentials(SocketProviderBuilder socketBuilder, String sSecureTransport)
        {
        GrpcTransportSecurity.Transport transport = GrpcTransportSecurity.enforce(socketBuilder, sSecureTransport);
        if (transport == GrpcTransportSecurity.Transport.TLS)
            {
            SSLSocketProvider.Dependencies dependencies           = GrpcTransportSecurity.getSSLDependencies(socketBuilder);
            SSLContextDependencies         sslContextDependencies = dependencies.getSSLContextDependencies();
            RefreshableSslContext          sslContext             = new RefreshableSslContext(sslContextDependencies, false);
            return NettySslContextChannelCredentials.create(sslContext);
            }
        return InsecureChannelCredentials.create();
        }
    }
