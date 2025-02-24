/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc;

import com.tangosol.application.Context;

import com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.GrpcAcceptor;
import com.tangosol.coherence.config.Config;
import com.tangosol.io.Serializer;
import com.tangosol.net.ConfigurableCacheFactory;

import java.io.Closeable;
import java.util.Optional;

/**
 * A gRPC server side service.
 */
public interface GrpcService
    {
    /**
     * Return a {@link ConfigurableCacheFactory} for the specified scope name.
     *
     * @param sScope  the scope name to use to obtain a {@link ConfigurableCacheFactory}
     *
     * @return a {@link ConfigurableCacheFactory} for the specified scope name
     */
    ConfigurableCacheFactory getCCF(String sScope);

    /**
     * Return a {@link Serializer}.
     *
     * @param sFormat  the format (name) of the serializer
     * @param loader   the {@link ClassLoader} for the serializer
     *
     * @return  the named serializer
     */
    Serializer getSerializer(String sFormat, ClassLoader loader);

    /**
     * Return the service dependencies.
     *
     * @return the service dependencies
     */
    Dependencies getDependencies();

    /**
     * Add a {@link Closeable} to be closed when this service closes.
     *
     * @param closeable  the {@link Closeable} to be closed when this service closes
     * @see GrpcService#removeCloseable(Closeable)
     */
    void addCloseable(Closeable closeable);

    /**
     * Remove a previously added {@link Closeable} from this service.
     *
     * @param closeable  the {@link Closeable} to be removed
     * @see GrpcService#addCloseable(Closeable)
     */
    void removeCloseable(Closeable closeable);

    /**
     * Return the parent {@link GrpcAcceptor}.
     *
     * @return  the parent {@link GrpcAcceptor}
     */
    GrpcAcceptor getGrpcAcceptor();

    /**
     * The dependencies for this service.
     */
    interface Dependencies
        {

        /**
         * Return the optional application {@link Context}.
         *
         * @return the optional application {@link Context}
         */
        Optional<Context> getContext();
        }

    /**
     * The name of the property to indicate whether to log gRPC messages sent and received.
     */
    String PROP_LOG_MESSAGES = "coherence.grpc.log.messages";

    /**
     * A flag to indicate whether to log messages sent and received.
     */
    boolean LOG_MESSAGES = Config.getBoolean(PROP_LOG_MESSAGES, false);
    }
