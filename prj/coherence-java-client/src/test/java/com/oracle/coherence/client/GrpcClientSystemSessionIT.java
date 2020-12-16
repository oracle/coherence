/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.client;

import com.tangosol.internal.net.ConfigurableCacheFactorySession;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.Coherence;
import com.tangosol.net.CoherenceConfiguration;
import com.tangosol.net.Session;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Client side system session tests.
 *
 * @author Jonathan Knight  2020.12.16
 */
public class GrpcClientSystemSessionIT
    {
    @BeforeAll
    static void setup()
        {
        System.setProperty("coherence.ttl", "0");
        System.setProperty("coherence.cluster", "CoherenceBootstrapTests");
        }

    @AfterEach
    void cleanup()
        {
        Coherence.closeAll();
        CacheFactory.getCacheFactoryBuilder().releaseAll(null);
        CacheFactory.shutdown();
        }

    @Test
    void shouldHaveSystemSessionOnClusterMember()
        {
        Coherence coherence = Coherence.clusterMember(CoherenceConfiguration.builder().build());

        assertThat(coherence.getMode(), is(Coherence.Mode.ClusterMember));

        coherence.start().join();

        assertThat(coherence.hasSession(Coherence.SYSTEM_SESSION), is(true));
        Session session = coherence.getSession(Coherence.SYSTEM_SESSION);
        assertThat(session, is(instanceOf(ConfigurableCacheFactorySession.class)));
        }

    @Test
    void shouldHaveSystemSessionOnClient()
        {
        Coherence coherence = Coherence.client(CoherenceConfiguration.builder().build());

        assertThat(coherence.getMode(), is(Coherence.Mode.Client));

        coherence.start().join();

        assertThat(coherence.hasSession(Coherence.SYSTEM_SESSION), is(true));
        Session session = coherence.getSession(Coherence.SYSTEM_SESSION);
        assertThat(session, is(instanceOf(GrpcRemoteSession.class)));
        }
    }
