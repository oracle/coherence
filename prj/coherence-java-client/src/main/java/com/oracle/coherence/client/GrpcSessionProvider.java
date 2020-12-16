/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.client;

import com.tangosol.net.SessionProvider;

/**
 * A provider of gRPC sessions.
 * <p>
 * Implementations of this class should have a public default constructor,
 * as they will be discovered via the {@link java.util.ServiceLoader}.
 *
 * @author Jonathan Knight  2020.12.14
 * @since 20.12
 */
public interface GrpcSessionProvider
        extends SessionProvider
    {
    }
