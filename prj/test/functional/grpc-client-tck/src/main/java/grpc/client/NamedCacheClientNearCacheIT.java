/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package grpc.client;

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
            .setProperty("coherence.wka", "127.0.0.1")
            .setProperty("coherence.localhost", "127.0.0.1")
            .setProperty("coherence.ttl", "0")
            .setProperty("coherence.clustername", "NamedCacheClientNearCacheIT")
            .setProperty("coherence.override", "coherence-json-override.xml")
            .setProperty("coherence.pof.config", "test-pof-config.xml")
            .setProperty("coherence.cacheconfig", "coherence-config.xml")
            .setProperty("coherence.profile", "near");
    }
