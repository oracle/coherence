/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package grpc.proxy;

import com.oracle.bedrock.runtime.coherence.callables.IsServiceRunning;

import com.oracle.bedrock.runtime.concurrent.RemoteCallable;

import com.tangosol.net.grpc.GrpcDependencies;

/**
 * A Bedrock {@link RemoteCallable} to check whether the
 * system gRPC proxy is running.
 */
public class IsGrpcProxyRunning
        extends IsServiceRunning
    {
    public IsGrpcProxyRunning()
        {
        super(GrpcDependencies.SCOPED_PROXY_SERVICE_NAME);
        }

    public static boolean locally()
        {
        return INSTANCE.call();
        }

    // ----- data members ---------------------------------------------------

    public static final IsGrpcProxyRunning INSTANCE = new IsGrpcProxyRunning();
    }
