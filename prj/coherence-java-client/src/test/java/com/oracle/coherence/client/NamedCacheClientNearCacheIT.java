/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.client;

import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * An extension of {@link NamedCacheClientIT} that will
 * use {@link com.tangosol.net.cache.NearCache}s on the
 * server.
 *
 * @author Jonathan Knight  2020.09.24
 */
public class NamedCacheClientNearCacheIT
        extends BaseNamedCacheClientIT
    {
    public NamedCacheClientNearCacheIT()
        {
        super(s_serverHelper);
        }

    @RegisterExtension
    static ServerHelper s_serverHelper = new ServerHelper()
            .setProperty("coherence.ttl", "0")
            .setProperty("coherence.clustername", "GrpcServer")
            .setProperty("coherence.override", "coherence-json-override.xml")
            .setProperty("coherence.pof.config", "test-pof-config.xml")
            .setProperty("coherence.cacheconfig", "coherence-config.xml")
            .setProperty("coherence.profile", "near");
    }
