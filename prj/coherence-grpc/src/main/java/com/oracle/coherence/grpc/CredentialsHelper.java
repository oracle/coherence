/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.grpc;

import com.oracle.coherence.common.base.Exceptions;
import com.oracle.coherence.common.base.Logger;
import com.oracle.coherence.common.net.SSLSocketProvider;
import com.tangosol.coherence.config.Config;
import com.tangosol.coherence.config.builder.SocketProviderBuilder;
import com.tangosol.config.ConfigurationException;
import com.tangosol.internal.net.ssl.SSLContextDependencies;
import com.tangosol.net.SocketProviderFactory;
import com.tangosol.util.Resources;
import io.grpc.ChannelCredentials;
import io.grpc.InsecureChannelCredentials;
import io.grpc.InsecureServerCredentials;
import io.grpc.ServerCredentials;
import io.grpc.TlsServerCredentials;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NettySslContextChannelCredentials;
import io.grpc.netty.NettySslContextServerCredentials;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

import javax.net.ssl.SSLException;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Arrays;

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
     * @return the {@link ServerCredentials} to use for the gRPC Proxy
     *
     * @deprecated server credentials are configured by the socket provider for a gRPC proxy service,
     *             see {@link #createServerCredentials(SocketProviderBuilder)}
     */
    @Deprecated(since = "22.06.2")
    @SuppressWarnings("DeprecatedIsStillUsed")
    public static ServerCredentials createServerCredentials()
        {
        String            sCredentials = Config.getProperty(Requests.PROP_CREDENTIALS, Requests.CREDENTIALS_INSECURE);
        ServerCredentials credentials;

        if (Requests.CREDENTIALS_INSECURE.equalsIgnoreCase(sCredentials))
            {
            Logger.info("Creating gRPC server using insecure credentials");
            credentials = InsecureServerCredentials.create();
            }
        else if (Requests.CREDENTIALS_TLS.equalsIgnoreCase(sCredentials))
            {
            try
                {
                String sTlsCert    = Config.getProperty(Requests.PROP_TLS_CERT);
                String sTlsKey     = Config.getProperty(Requests.PROP_TLS_KEY);
                String sTlsCA      = Config.getProperty(Requests.PROP_TLS_CA);
                String sClientAuth = Config.getProperty(Requests.PROP_TLS_CLIENT_AUTH, ClientAuth.NONE.name()).toUpperCase();
                String sTlsPass    = resolveServerPassword();

                if (sTlsKey == null || sTlsCert == null)
                    {
                    String sReason = "Invalid gRPC configuration, "
                            + ((sTlsKey == null) ? "no key file specified" : "no cert file specfied");
                    throw new ConfigurationException(sReason,
                                                     "When configuring gRPC TLS both the key and cert files must be configured"
                            + " key=\"" + sTlsKey + "\" cert=\"" + sTlsCert + "\"");
                    }

                URL urlCert = Resources.findFileOrResource(sTlsCert, null);
                if (urlCert == null)
                    {
                    throw new ConfigurationException("Cannot find configured TLS cert: " + sTlsCert,
                                                     "Ensure the TLS cert exists");
                    }

                URL urlKey = Resources.findFileOrResource(sTlsKey, null);
                if (urlKey == null)
                    {
                    throw new ConfigurationException("Cannot find configured TLS key: " + sTlsCert,
                                                     "Ensure the TLS key exists");
                    }

                if (sTlsCA == null || ClientAuth.NONE.name().equals(sClientAuth))
                    {
                    credentials = TlsServerCredentials.newBuilder()
                             .keyManager(urlCert.openStream(), urlKey.openStream(), sTlsPass)
                             .build();
                    }
                else
                    {
                    URL urlCA = Resources.findFileOrResource(sTlsCA, null);
                    if (urlCA == null)
                        {
                        throw new ConfigurationException("Cannot find configured TLS CA: " + sTlsCA,
                                                         "Ensure the TLS CA exists");
                        }

                    ClientAuth clientAuth;
                    try
                        {
                        clientAuth = ClientAuth.valueOf(sClientAuth);
                        }
                    catch (IllegalArgumentException e)
                        {
                        throw new ConfigurationException("Cannot find configured TLS client auth value: "
                                + sClientAuth, "Valid values are one of " + Arrays.toString(ClientAuth.values()));
                        }

                    SslContextBuilder builder = SslContextBuilder
                            .forServer(urlCert.openStream(), urlKey.openStream(), sTlsPass)
                            .trustManager(urlCA.openStream())
                            .clientAuth(clientAuth);

                    Logger.info("Creating gRPC server using TLS credentials. key="
                            + urlKey + " cert=" + urlCert + " ca=" + urlCA + " clientAuth=" + sClientAuth);

                    credentials = NettySslContextServerCredentials
                            .create(GrpcSslContexts.configure(builder).build());
                    }
                }
            catch (IOException e)
                {
                throw Exceptions.ensureRuntimeException(e);
                }
            }
        else
            {
            throw new ConfigurationException("Invalid gRPC credentials type \"" + sCredentials + "\"",
                                             "Valid values are \"" + Requests.CREDENTIALS_INSECURE + "\" or \""
                                            + Requests.CREDENTIALS_TLS + "\"");
            }

        return credentials;
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
                return createServerCredentials();
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
        return createServerCredentials();
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
     * @param sChannelName  the name of the channel
     *
     * @return the {@link ChannelCredentials} to use for the client channel.
     *
     * @deprecated server credentials are configured by the socket provider for a gRPC channel,
     *             see {@link #createChannelCredentials(String, SocketProviderBuilder)}
     */
    @Deprecated(since = "22.06.2")
    @SuppressWarnings("DeprecatedIsStillUsed")
    public static ChannelCredentials createChannelCredentials(String sChannelName)
        {
        String             sType = getProperty(PROP_CREDENTIALS, sChannelName, Requests.CREDENTIALS_PLAINTEXT);
        ChannelCredentials credentials;

        if (Requests.CREDENTIALS_PLAINTEXT.equals(sType))
            {
            credentials = InsecureChannelCredentials.create();
            }
        else if (Requests.CREDENTIALS_INSECURE.equals(sType))
            {
            try
                {
                SslContext sslContext = GrpcSslContexts.forClient()
                        .trustManager(InsecureTrustManagerFactory.INSTANCE)
                        .build();

                credentials = NettySslContextChannelCredentials.create(sslContext);
                }
            catch (SSLException e)
                {
                throw Exceptions.ensureRuntimeException(e);
                }
            }
        else if (Requests.CREDENTIALS_TLS.equals(sType))
            {
            try
                {
                SslContextBuilder builder = SslContextBuilder.forClient();

                String sTlsCert = getProperty(PROP_TLS_CERT, sChannelName);
                String sTlsKey  = getProperty(PROP_TLS_KEY, sChannelName);
                String sTlsCA   = getProperty(PROP_TLS_CA, sChannelName);
                String sTlsPass = resolveChannelPassword(sChannelName);
                URL    urlCert  = null;
                URL    urlKey   = null;
                URL    urlCA    = null;

                if ((sTlsKey != null && sTlsCert == null) || (sTlsKey == null && sTlsCert != null))
                    {
                    String sReason = "Invalid gRPC configuration for channel \"" + sChannelName + "\", "
                            + ((sTlsKey == null) ? "no key file specified" : "no cert file specified");
                    throw new ConfigurationException(sReason,
                            "When configuring gRPC TLS both the key and cert files must be configured"
                            + " key=\"" + sTlsKey + "\" cert=\"" + sTlsCert + "\"");
                    }
                else if (sTlsKey != null && sTlsCert != null)
                    {
                    urlCert = Resources.findFileOrResource(sTlsCert, null);
                    if (urlCert == null)
                        {
                        throw new ConfigurationException("Cannot find configured TLS cert for channel \""
                                                                 + sChannelName + "\": " + sTlsCert,
                                                         "Ensure the TLS cert exists");
                        }

                    urlKey = Resources.findFileOrResource(sTlsKey, null);
                    if (urlKey == null)
                        {
                        throw new ConfigurationException("Cannot find configured TLS key for channel \""
                                                                 + sChannelName + "\": " + sTlsCert,
                                                         "Ensure the TLS key exists");
                        }

                    builder.keyManager(urlCert.openStream(), urlKey.openStream(), sTlsPass);
                    }

                if (sTlsCA != null)
                    {
                    urlCA = Resources.findFileOrResource(sTlsCA, null);
                    if (urlCA == null)
                        {
                        throw new ConfigurationException("Cannot find configured TLS CA: for channel \""
                                + sChannelName + "\": " + sTlsCA,
                                "Ensure the TLS CA exists");
                        }

                    builder.trustManager(urlCA.openStream());
                    }

                builder = GrpcSslContexts.configure(builder);

                Logger.info("Creating gRPC Channel \"" + sChannelName
                                    + "\" using TLS credentials. key="
                                    + urlKey + " cert=" + urlCert + " ca=" + urlCA);

                credentials = NettySslContextChannelCredentials.create(builder.build());
                }
            catch (IOException e)
                {
                throw Exceptions.ensureRuntimeException(e);
                }
            }
        else
            {
            throw new ConfigurationException("Invalid credentials type for channel " + sChannelName,
                    "Valid values are " + Requests.CREDENTIALS_INSECURE + " " + Requests.CREDENTIALS_TLS
                    + " " + Requests.CREDENTIALS_PLAINTEXT);
            }

        return credentials;
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
     * @param sChannelName  the name of the channel
     *
     * @return the {@link ChannelCredentials} to use for the client channel.
     */
    public static ChannelCredentials createChannelCredentials(String sChannelName, SocketProviderBuilder socketBuilder)
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
        return createChannelCredentials(sChannelName);
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
