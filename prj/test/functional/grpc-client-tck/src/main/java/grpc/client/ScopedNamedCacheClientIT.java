/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package grpc.client;

import org.junit.jupiter.api.extension.RegisterExtension;

class ScopedNamedCacheClientIT
        extends BaseNamedCacheClientIT
    {
    // ----- constructors ---------------------------------------------------

    public ScopedNamedCacheClientIT()
        {
        super(s_serverHelper, SCOPE_NAME);
        }

    // ----- data members ---------------------------------------------------

    public static final String SCOPE_NAME = "testing";

    @RegisterExtension
    protected static ServerHelper s_serverHelper = new ServerHelper()
            .setScope(SCOPE_NAME)
            .setProperty("coherence.ttl", "0")
            .setProperty("coherence.wka", "127.0.0.1")
            .setProperty("coherence.localhost", "127.0.0.1")
            .setProperty("coherence.clustername", "GrpcServer")
            .setProperty("coherence.override", "coherence-json-override.xml")
            .setProperty("coherence.pof.config", "test-pof-config.xml")
            .setProperty("coherence.cacheconfig", "coherence-config.xml")
            .setProperty("coherence.cacheconfig." + SCOPE_NAME, "coherence-cache-config.xml");
    }
