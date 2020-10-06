/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.client;

import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * An integration test for {@link NamedCacheClient} that uses scoped
 * instances of {@link NamedCacheClient}.
 *
 * @author Jonathan Knight  2019.11.07
 * @since 20.06
 */
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
            .setProperty("coherence.clustername", "GrpcServer")
            .setProperty("coherence.override", "coherence-json-override.xml")
            .setProperty("coherence.pof.config", "test-pof-config.xml")
            .setProperty("coherence.cacheconfig", "coherence-config.xml")
            .setProperty("coherence.cacheconfig." + SCOPE_NAME, "coherence-cache-config.xml");
    }
