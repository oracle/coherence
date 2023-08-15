/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.grpc;

import com.oracle.coherence.common.net.SSLSocketProvider;
import com.tangosol.coherence.config.Config;
import com.tangosol.coherence.config.builder.SocketProviderBuilder;
import com.tangosol.internal.net.ssl.SSLContextDependencies;
import com.tangosol.net.SocketProviderFactory;
import com.tangosol.util.Resources;
import io.grpc.ChannelCredentials;
import io.grpc.InsecureChannelCredentials;
import io.grpc.InsecureServerCredentials;
import io.grpc.ServerCredentials;
import io.grpc.netty.NettySslContextChannelCredentials;
import io.grpc.netty.NettySslContextServerCredentials;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

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
     * Create the {@link ServerCredentials} to use for the gRPC Proxy.
     *
     * @param socketBuilder  the optional {@link SocketProviderBuilder} to use to provide the TLS configuration
     *
     * @return the {@link ServerCredentials} to use for the gRPC Proxy
     */
    public static ServerCredentials createServerCredentials(SocketProviderBuilder socketBuilder)
        {
        if (socketBuilder != null)
            {
            SocketProviderFactory.Dependencies depsFactory = socketBuilder.getDependencies();
            if (depsFactory == null)
                {
                return InsecureServerCredentials.create();
                }

            String                                          sSocketId   = socketBuilder.getId();
            SocketProviderFactory.Dependencies.ProviderType type        = depsFactory.getProviderType(sSocketId);

            if (type == SocketProviderFactory.Dependencies.ProviderType.GRPC)
                {
                return InsecureServerCredentials.create();
                }

            SSLSocketProvider.Dependencies dependencies = depsFactory.getSSLDependencies(sSocketId);
            if (dependencies != null)
                {
                SSLContextDependencies sslContextDependencies = dependencies.getSSLContextDependencies();
                RefreshableSslContext sslContext = new RefreshableSslContext(sslContextDependencies, true);

                return NettySslContextServerCredentials.create(sslContext);
                }
            }
        return InsecureServerCredentials.create();
        }

    /**
     * Create the {@link ChannelCredentials} to use for the client channel.
     * <p>
     * If the property {@link #PROP_CREDENTIALS} is "plaintext" then a non-TLS credentials
     * will be created.
     * <p>
     * If the property {@link #PROP_CREDENTIALS} is "insecure" then TLS credentials will
     * be created using an insecure trust manager that will not verify the server certs.
     * <p>
     * If the property {@link #PROP_CREDENTIALS} is "tls" then TLS credentials will be created
     * using a specified key (from the {@link #PROP_TLS_KEY}) and cert from (from the {@link #PROP_TLS_CERT}).
     * Optionally a key password (from the {@link #PROP_TLS_KEYPASS}) and a CA (from the {@link #PROP_TLS_CA})
     * may also be provided.
     * <p>
     * If the property {@link #PROP_CREDENTIALS} is not set, "plaintext" will be used.
     *
     * @param socketBuilder  the channel {@link SocketProviderBuilder}
     *
     * @return the {@link ChannelCredentials} to use for the client channel.
     */
    public static ChannelCredentials createChannelCredentials(SocketProviderBuilder socketBuilder)
        {
        if (socketBuilder != null)
            {
            SocketProviderFactory.Dependencies              depsFactory = socketBuilder.getDependencies();
            String                                          sSocketId   = socketBuilder.getId();
            SocketProviderFactory.Dependencies.ProviderType type        = depsFactory.getProviderType(sSocketId);

            if (type == SocketProviderFactory.Dependencies.ProviderType.GRPC)
                {
                return InsecureChannelCredentials.create();
                }

            SSLSocketProvider.Dependencies dependencies = depsFactory.getSSLDependencies(sSocketId);
            if (dependencies != null)
                {
                SSLContextDependencies sslContextDependencies = dependencies.getSSLContextDependencies();
                RefreshableSslContext  sslContext             = new RefreshableSslContext(sslContextDependencies, false);
                return NettySslContextChannelCredentials.create(sslContext);
                }
            }
        return InsecureChannelCredentials.create();
        }

    /**
     * Resolve any password required for the TLS keys for a given channel.
     *
     * @return  the password required for the TLS keys
     *
     * @throws IOException if the password cannot be resolved
     */
    private static String resolveChannelPassword(String sChannel) throws IOException
        {
        String sPasswordProperty = String.format(PROP_TLS_KEYPASS, sChannel);
        String sURIProperty      = String.format(PROP_TLS_KEYPASS_URI, sChannel);
        return resolvePassword(sPasswordProperty, sURIProperty);
        }

    /**
     * Resolve any password required for the TLS keys for a given channel.
     *
     * @return  the password required for the TLS keys
     *
     * @throws IOException if the password cannot be resolved
     */
    private static String resolveServerPassword() throws IOException
        {
        return resolvePassword(Requests.PROP_TLS_KEYPASS, Requests.PROP_TLS_KEYPASS_URI);
        }

    /**
     * Resolve any password required for the TLS keys.
     *
     * @param sPasswordProperty  the system property that may contain the password
     * @param sURIProperty       the system property that may contain a URI to read the password from
     *
     * @return  the password required for the TLS keys
     *
     * @throws IOException if the password cannot be resolved
     */
    private static String resolvePassword(String sPasswordProperty, String sURIProperty) throws IOException
        {
        String sTlsPass = Config.getProperty(sPasswordProperty);
        if (sTlsPass != null)
            {
            return sTlsPass;
            }

        String sURI = Config.getProperty(Requests.PROP_TLS_KEYPASS_URI);
        if (sURI != null)
            {
            final URL url = Resources.findFileOrResource(sURI, null);
            if (url == null)
                {
                throw new FileNotFoundException("Cannot locate password file: " + sURI);
                }
            try (InputStream in = url.openStream())
                {
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                return reader.readLine();
                }
            }

        return null;
        }

    private static String getProperty(String sProperty, String sChannelName)
        {
        return Config.getProperty(String.format(sProperty, sChannelName));
        }

    private static String getProperty(String sProperty, String sChannelName, String sDefault)
        {
        return Config.getProperty(String.format(sProperty, sChannelName), sDefault);
        }

    // ----- constants ------------------------------------------------------

    /**
     * The system property that sets the location of the TLS key file.
     */
    public static final String PROP_TLS_KEY = "coherence.grpc.channels.%s.tls.key";

    /**
     * The system property that sets the password for the TLS key file.
     */
    public static final String PROP_TLS_KEYPASS = "coherence.grpc.channels.%s.tls.password";

    /**
     * The system property that sets the URI of a file to read to obtain the password for the TLS key file.
     */
    public static final String PROP_TLS_KEYPASS_URI = "coherence.grpc.channels.%s.tls.password";

    /**
     * The system property that sets the location of the TLS cert file.
     */
    public static final String PROP_TLS_CERT = "coherence.grpc.channels.%s.tls.cert";

    /**
     * The system property that sets the location of the TLS CA file.
     */
    public static final String PROP_TLS_CA = "coherence.grpc.channels.%s.tls.ca";

    /**
     * The system property to use to set the type of credentials to use.
     */
    public static final String PROP_CREDENTIALS = "coherence.grpc.channels.%s.credentials";
    }
