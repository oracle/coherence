/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.client;

import com.tangosol.net.SessionProvider;

/**
 * A provider of gRPC sessions.
 * <p>
 * Implementations of this class should have a public default constructor,
 * as they will be discovered via the {@link java.util.ServiceLoader}.
 *
 * @deprecated Configure a grpc-remote-cache-scheme in the cache configuration file.
 *
 * @author Jonathan Knight  2020.12.14
 * @since 20.12
 */
@Deprecated(since = "22.06.2")
@SuppressWarnings("DeprecatedIsStillUsed")
public interface GrpcSessionProvider
        extends SessionProvider
    {
    }
