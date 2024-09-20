/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package grpc.client;

import org.junit.jupiter.api.BeforeAll;

public class LocalDefaultCacheConfigGrpcIT
        extends BaseLocalDefaultCacheConfigGrpcIT
    {
    @BeforeAll
    static void setupCluster() throws Exception
        {
        System.setProperty("coherence.grpc.heartbeat.interval", "1000");
        System.setProperty("coherence.grpc.heartbeat.ack", "true");
        runCluster();
        }
    }
