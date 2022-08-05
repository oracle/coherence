/*
 * Copyright (c) 2020, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.grpc.proxy;

import com.oracle.coherence.common.base.Exceptions;
import com.oracle.coherence.common.base.Logger;
import com.oracle.coherence.grpc.CredentialsHelper;
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

import jakarta.annotation.Priority;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
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
            ServerCredentials credentials = CredentialsHelper.createServerCredentials();
            return Grpc.newServerBuilderForPort(nPort, credentials);
            }

        @Override
        public InProcessServerBuilder getInProcessServerBuilder(String sName)
            {
            return InProcessServerBuilder.forName(sName);
            }

        /**
         * Resolve any password required for the TLS keys.
         *
         * @return  the password required for the TLS keys
         *
         * @throws IOException if the password cannot be resolved
         */
        private String resolvePassword() throws IOException
            {
            String sTlsPass = Config.getProperty(Requests.PROP_TLS_KEYPASS);
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
        };
    }
