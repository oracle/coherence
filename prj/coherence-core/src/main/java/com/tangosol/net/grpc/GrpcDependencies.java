/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.grpc;

import com.tangosol.net.Coherence;

/**
 * Coherence gRPC dependencies.
 *
 * @author Jonathan Knight  2022.08.25
 * @since 22.06.2
 */
public interface GrpcDependencies
    {
    /**
     * The default scope value.
     */
    String DEFAULT_SCOPE = Coherence.DEFAULT_SCOPE;

    /**
     * The cache configuration alias for the default scope name.
     */
    String DEFAULT_SCOPE_ALIAS = "$DEFAULT$";

    /**
     * The default name for the in-process gRPC server.
     */
    String DEFAULT_IN_PROCESS_NAME = "default";

    /**
     * The system property that sets the gRPC proxy port.
     */
    String PROP_PORT = "coherence.grpc.server.port";

    /**
     * The System property to enable or disable the gRPC proxy server.
     */
    String PROP_ENABLED = "coherence.grpc.enabled";

    /**
     * The name of the gRPC proxy cache configuration file.
     */
    String GRPC_PROXY_CACHE_CONFIG = "grpc-proxy-cache-config.xml";

    /**
     * The scope of the gRPC proxy in the gRPC cache config.
     */
    String PROXY_SERVICE_SCOPE_NAME = "$GRPC";
    // This value here must match the name used for the gRPC proxy
    // in grpc-proxy-cache-config.xml file and in the NSLookup.GRPC_PROXY_URL field

    /**
     * The name of the gRPC proxy in the gRPC cache config.
     */
    String PROXY_SERVICE_NAME = "GrpcProxy";
    // This value here must match the name used for the gRPC proxy
    // in grpc-proxy-cache-config.xml file and in the NSLookup.GRPC_PROXY_URL field

    /**
     * The fully qualified name of the gRPC proxy in the gRPC cache config.
     */
    String SCOPED_PROXY_SERVICE_NAME = PROXY_SERVICE_SCOPE_NAME + ":" + PROXY_SERVICE_NAME;
    // This suffix here must match the fully scoped name used for the gRPC proxy
    // in grpc-proxy-cache-config.xml file and for the NSLookup.GRPC_PROXY_URL field

    /**
     * The default max page size used by the gRPC debug Channelz service.
     */
    int DEFAULT_CHANNELZ_MAX_PAGE_SIZE = 100;

    /**
     * The default rpc deadline used for gRPC client requests.
     */
    long DEFAULT_DEADLINE_MILLIS = 30000;
    // ----- inner enum: ServerType -----------------------------------------

    /**
     * An enum representing different types of server the service
     * can be deployed into.
     */
    enum ServerType
        {
        /**
         * The server is a standard asynchronous gRPC server, e.g. the Java Netty
         * server, where requests are handed off to a daemon pool to free up the
         * gRPC request threads as soon as possible.
         */
        Asynchronous,
        /**
         * The server is a synchronous gRPC server, e.g. a Helidon 4 gRPC server,
         * where gRPC request threads are virtual threads, so there is no need
         * to hand off work to another thread.
         */
        Synchronous
        }
    }
