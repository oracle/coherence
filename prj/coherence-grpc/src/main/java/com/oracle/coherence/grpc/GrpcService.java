/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc;

import com.tangosol.application.Context;

import com.tangosol.io.Serializer;
import com.tangosol.net.ConfigurableCacheFactory;

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
    }
