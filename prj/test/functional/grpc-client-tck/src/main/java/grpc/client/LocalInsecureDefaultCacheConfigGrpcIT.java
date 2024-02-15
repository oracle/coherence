/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package grpc.client;

import org.junit.jupiter.api.BeforeAll;

public class LocalInsecureDefaultCacheConfigGrpcIT
        extends BaseLocalDefaultCacheConfigGrpcIT
    {
    @BeforeAll
    static void setupCluster() throws Exception
        {
        System.setProperty("coherence.grpc.server.socketprovider", "grpc-insecure");
        System.setProperty("coherence.grpc.socketprovider", "grpc-insecure");

        runCluster();
        }
    }
