/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.grpc.proxy;

import com.oracle.coherence.common.base.Exceptions;
import com.oracle.coherence.common.base.Logger;
import com.oracle.coherence.grpc.Requests;
import com.tangosol.coherence.config.Config;
import com.tangosol.config.ConfigurationException;
import com.tangosol.util.Resources;
import io.grpc.Grpc;
import io.grpc.InsecureServerCredentials;
import io.grpc.ServerBuilder;
import io.grpc.ServerCredentials;
import io.grpc.TlsServerCredentials;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NettyServerBuilder;
import io.grpc.netty.NettySslContextServerCredentials;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContextBuilder;

import javax.annotation.Priority;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;

/**
 * A class that can provide an instance of a {@link io.grpc.ServerBuilder}
 * to be used by the gRPC proxy to create a server, and an instance of a
 * {@link InProcessServerBuilder} to build an in-process gRPC server.
 * <p>
 * Instances of this class will be discovered using the {@link java.util.ServiceLoader}.
 * If multiple instances are on the classpath the instance with the the highest priority
 * will be used. If multiple discovered instances share the highest priority the instance
 * used will be undetermined.
 *
 * @author Jonathan Knight  2020.11.24
 * @since 20.12
 */
public interface GrpcServerBuilderProvider
        extends Comparable<GrpcServerBuilderProvider>
    {
    /**
     * Returns a {@link ServerBuilder} that may be used to build
     * a gRPC server.
     *
     * @param nPort  the default port to bind to
     *
     * @return a {@link ServerBuilder} that may be used to build
     *         a gRPC server
     */
    ServerBuilder<?> getServerBuilder(int nPort);

    /**
     * Returns a {@link InProcessServerBuilder} that may be used to build
     * an in-process gRPC server.
     *
     * @param sName  the default name for the in-process server
     *
     * @return a {@link InProcessServerBuilder} that may be used to build
     *         an in-process gRPC server
     */
    InProcessServerBuilder getInProcessServerBuilder(String sName);

    /**
     * Obtain the priority of this {@link GrpcServerBuilderProvider}.
     * <p>
     * If multiple {@link GrpcServerBuilderProvider} instances are discovered
     * by the {@link java.util.ServiceLoader} then the one with the highest
     * priority will be used. If multiple instances have the same highest
     * priority then the instance used is undetermined.
     *
     * @return the priority of this {@link GrpcServerBuilderProvider}
     */
    default int getPriority()
        {
        Priority annotation = getClass().getAnnotation(Priority.class);
        return annotation == null ? DEFAULT_PRIORITY : annotation.value();
        }

    @Override
    default int compareTo(GrpcServerBuilderProvider o)
        {
        // order with the highest priority first
        return 0 - Integer.compare(getPriority(), o.getPriority());
        }

    /**
     * The default priority for providers without a specific priority.
     */
    int DEFAULT_PRIORITY = 0;

    /**
     * The default {@link GrpcServerBuilderProvider}.
     */
    GrpcServerBuilderProvider INSTANCE = new GrpcServerBuilderProvider()
        {
        @Override
        public ServerBuilder<?> getServerBuilder(int nPort)
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
                    String sTlsPass    = Config.getProperty(Requests.PROP_TLS_KEYPASS);
                    String sTlsCA      = Config.getProperty(Requests.PROP_TLS_CA);
                    String sClientAuth = Config.getProperty(Requests.PROP_TLS_CLIENT_AUTH, ClientAuth.NONE.name()).toUpperCase();

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

            return Grpc.newServerBuilderForPort(nPort, credentials);
            }

        @Override
        public InProcessServerBuilder getInProcessServerBuilder(String sName)
            {
            return InProcessServerBuilder.forName(sName);
            }
        };
    }
