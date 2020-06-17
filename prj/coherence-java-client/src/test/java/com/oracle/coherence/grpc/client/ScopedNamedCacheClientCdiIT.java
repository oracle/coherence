/*
 * Copyright (c) 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.client;

import com.oracle.coherence.cdi.ConfigUri;
import com.oracle.coherence.cdi.Name;
import com.oracle.coherence.cdi.Remote;
import com.oracle.coherence.cdi.Scope;
import com.oracle.coherence.cdi.ScopeInitializer;
import com.tangosol.io.Serializer;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.NamedCache;
import io.helidon.microprofile.server.Server;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Jonathan Knight  2020.06.16
 * @since 20.06
 */
class ScopedNamedCacheClientCdiIT
    {
    // ----- test lifecycle -------------------------------------------------

    @BeforeAll
    static void setupBaseTest()
        {
        System.setProperty("coherence.ttl",         "0");
        System.setProperty("coherence.clustername", "ScopedNamedCacheClientCdiIT");
        System.setProperty("coherence.cache.config", "coherence-config.xml");
        System.setProperty("coherence.pof.config",  "test-pof-config.xml");
        System.setProperty("coherence.pof.enabled", "true");

        s_server = Server.create().start();
        }

    @AfterAll
    static void cleanupBaseTest()
        {
        s_server.stop();
        }

    // ----- test methods ---------------------------------------------------

    @Test
    public void shouldUseScopedClients() throws Exception
        {
        Instance<Clients> instance = CDI.current().select(Clients.class);
        assertThat(instance.isResolvable(), is(true));
        Clients clients = instance.get();

        String sOldValue = clients.getCacheOne().put("key-1", "value-1");
        assertThat(sOldValue, is(nullValue()));

        sOldValue = clients.getCacheTwo().put("key-1", "value-2");
        assertThat(sOldValue, is(nullValue()));

        assertThat(clients.getCacheOne().get("key-1"), is("value-1"));
        assertThat(clients.getCacheTwo().get("key-1"), is("value-2"));
        }


    // ----- inner class Clients --------------------------------------------

    @ApplicationScoped
    public static class Clients
        {
        public NamedCache<String, String> getCacheOne()
            {
            return cacheOne;
            }

        public NamedCache<String, String> getCacheTwo()
            {
            return cacheTwo;
            }

        @Inject
        @Remote
        @Scope("one")
        @Name("test-cache")
        private NamedCache<String, String> cacheOne;

        @Inject
        @Remote
        @Scope("two")
        @Name("test-cache")
        private NamedCache<String, String> cacheTwo;
        }


    // ----- inner class CacheFactoryOne ------------------------------------

    @ApplicationScoped
    @Named("one")
    @ConfigUri("coherence-config.xml")
    public static class CacheFactoryOne
            implements ScopeInitializer
        {}

    // ----- inner class CacheFactoryTwo ------------------------------------

    @ApplicationScoped
    @Named("two")
    @ConfigUri("coherence-config.xml")
    public static class CacheFactoryTwo
            implements ScopeInitializer
        {}

    // ----- data members ---------------------------------------------------

    private static Server s_server;
    }
