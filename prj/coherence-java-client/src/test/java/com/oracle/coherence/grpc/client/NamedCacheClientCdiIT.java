/*
 * Copyright (c) 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.client;

import com.oracle.coherence.cdi.Name;
import com.oracle.coherence.cdi.Remote;
import com.oracle.coherence.cdi.Scope;
import com.oracle.coherence.cdi.SerializerFormat;
import com.tangosol.io.Serializer;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.NamedCache;

import io.helidon.microprofile.server.Server;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.inject.Instance;

import javax.enterprise.inject.spi.CDI;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * An integration test for {@link NamedCacheClient} that creates instances of
 * {@link NamedCacheClient} using CDI to discover the {@link GrpcRemoteSession}
 * for the required channel name and serializer format and then obtain the
 * {@link NamedCacheClient} instance from the session.
 *
 * @author Jonathan Knight  2019.11.07
 * @since 20.06
 */
class NamedCacheClientCdiIT
        extends BaseNamedCacheClientIT
    {
    // ----- test lifecycle -------------------------------------------------

    @BeforeAll
    static void setupBaseTest()
        {
        System.setProperty("coherence.ttl",         "0");
        System.setProperty("coherence.clustername", "NamedCacheClientCdiIT");
        System.setProperty("coherence.cache.config",  "coherence-config.xml");
        System.setProperty("coherence.pof.config",  "test-pof-config.xml");
        System.setProperty("coherence.pof.enabled", "true");

        s_server = Server.create().start();

        s_ccf = CacheFactory.getCacheFactoryBuilder()
                .getConfigurableCacheFactory("coherence-config.xml", null);
        }

    @AfterAll
    static void cleanupBaseTest()
        {
        s_listClients.forEach(client -> {
        if (client.isActive())
            {
            client.destroy();
            }
        });
        s_server.stop();
        }

    // ----- BaseNamedCacheClientIT methods ---------------------------------

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    protected <K, V> NamedCacheClient<K, V> createClient(String sCacheName, String sSerializerFormat, Serializer serializer)
        {
        String sessionName = "".equals(sSerializerFormat) ? "test" : "test-" + sSerializerFormat;

        Instance<NamedCacheClient> cacheInstance = CDI.current().getBeanManager()
                .createInstance()
                .select(NamedCacheClient.class,
                        Name.Literal.of(sCacheName),
                        Remote.Literal.of(sessionName));

        assertThat(cacheInstance.isResolvable(), is(true));

        NamedCacheClient<K, V> client = cacheInstance.get();

        if (!sSerializerFormat.isEmpty())
            {
            assertThat(client.getAsyncClient().getFormat(), is(sSerializerFormat));
            }

        s_listClients.add(client);
        return client;
        }

    @Override
    protected <K, V> NamedCache<K, V> ensureCache(String sName, ClassLoader loader)
        {
        return s_ccf.ensureCache(sName, loader);
        }

    // ----- data members ---------------------------------------------------

    protected static ConfigurableCacheFactory s_ccf;

    private static Server s_server;

    protected static List<NamedCacheClient<?, ?>> s_listClients = new ArrayList<>();
    }
