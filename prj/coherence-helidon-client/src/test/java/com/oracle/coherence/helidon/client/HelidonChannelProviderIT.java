/*
 * Copyright (c) 2020, 2021 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.helidon.client;

import com.oracle.coherence.cdi.CoherenceExtension;
import com.oracle.coherence.client.ChannelProviders;
import com.oracle.coherence.grpc.proxy.GrpcServerController;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.Coherence;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.DefaultCacheServer;
import com.tangosol.net.NamedCache;
import io.grpc.Channel;
import io.helidon.microprofile.grpc.client.GrpcClientCdiExtension;
import io.helidon.microprofile.server.Server;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.enterprise.inject.spi.CDI;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Jonathan Knight  2020.12.18
 */
public class HelidonChannelProviderIT
    {
    // ----- test lifecycle -------------------------------------------------

    @BeforeAll
    static void setupBaseTest() throws Exception
        {
        System.setProperty("coherence.ttl",              "0");
        System.setProperty("coherence.clustername",      "MapEventsIT");
        System.setProperty("coherence.cacheconfig",      "coherence-config.xml");
        System.setProperty("coherence.pof.config",       "test-pof-config.xml");
        System.setProperty("coherence.pof.enabled",      "true");
        System.setProperty("coherence.log.level",        "9");

        // The CDI server will start Coherence
        s_server = Server.create().start();
        }

    @AfterAll
    static void cleanupBaseTest()
        {
        CacheFactory.getCluster().shutdown();
        if (s_server != null)
            {
            s_server.stop();
            }
        }

    // ----- test methods ---------------------------------------------------

    @Test
    public void shouldHaveConfiguredChannel()
        {
        HelidonChannelProvider provider = new HelidonChannelProvider();
        Optional<Channel> optional = provider.getChannel("test");
        assertThat(optional, is(notNullValue()));
        assertThat(optional.isPresent(), is(true));
        }

    @Test
    public void shouldHaveConfiguredChannelFromChannelProviders()
        {
        Optional<Channel> optional = ChannelProviders.INSTANCE.findChannel("test");
        assertThat(optional, is(notNullValue()));
        assertThat(optional.isPresent(), is(true));
        }

    // ----- data members ---------------------------------------------------

    private static Server s_server;
    }
