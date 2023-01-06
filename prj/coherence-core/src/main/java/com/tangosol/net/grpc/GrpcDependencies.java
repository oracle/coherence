/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.grpc;

/**
 * Coherence gRPC dependencies.
 *
 * @author Jonathan Knight  2022.08.25
 * @since 22.06.2
 */
public interface GrpcDependencies
    {
    /**
     * The default gRPC port.
     */
    int DEFAULT_PORT = 1408;

    /**
     * The default scope value.
     */
    String DEFAULT_SCOPE = "";

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
     * The system property that sets the name of the in-process gRPC server.
     */
    String PROP_IN_PROCESS_NAME = "coherence.grpc.inprocess.name";

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
    }
