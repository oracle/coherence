/*
 * Copyright (c) 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.client;

import com.tangosol.io.Serializer;

import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.NamedCache;

import io.grpc.Channel;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

/**
 * An integration test for {@link NamedCacheClient} that creates instances of
 * {@link NamedCacheClient} using the {@link AsyncNamedCacheClient.Builder}.
 * <p>
 * This integration test runs without a CDI environment.
 *
 * @author Jonathan Knight  2019.11.07
 * @since 14.1.2
 */
class NamedCacheClientIT
        extends BaseNamedCacheClientIT
    {
    // ----- test lifecycle -------------------------------------------------

    @BeforeAll
    static void setupBaseTest() throws Exception
        {
        serverHelper = new ServerHelper();
        serverHelper.start();

        ccf     = serverHelper.getCCF();
        channel = serverHelper.getChannel();
        }

    @AfterAll
    static void cleanupBaseTest()
        {
        clients.values()
                .stream()
                .flatMap(m -> m.values().stream())
                .forEach(client -> {
                if (client.isActiveInternal())
                    {
                    client.destroy().join();
                    }
                });
        serverHelper.shutdown();
        }

    // ----- BaseNamedCacheClientIT methods ---------------------------------

    @Override
    @SuppressWarnings("unchecked")
    protected <K, V> NamedCacheClient<K, V> createClient(String sCacheName, String sSerializerName,
                                                         Serializer serializer)
        {
        Map<String, AsyncNamedCacheClient<?, ?>> map = clients.computeIfAbsent(sCacheName, k -> new HashMap<>());
        AsyncNamedCacheClient<K, V> async = (AsyncNamedCacheClient<K, V>)
                map.computeIfAbsent(sSerializerName, k -> AsyncNamedCacheClient.builder(sCacheName)
                        .channel(channel)
                        .serializer(serializer, sSerializerName)
                        .build());

        if (!async.isActiveInternal())
            {
            map.remove(sSerializerName);
            return createClient(sCacheName, sSerializerName, serializer);
            }
        return (NamedCacheClient<K, V>) async.getNamedCache();
        }

    @Override
    protected <K, V> NamedCache<K, V> ensureCache(String sName, ClassLoader loader)
        {
        return ccf.ensureCache(sName, loader);
        }

    // ----- data members ---------------------------------------------------

    private static final Map<String, Map<String, AsyncNamedCacheClient<?, ?>>> clients = new HashMap<>();

    private static ServerHelper serverHelper;

    private static ConfigurableCacheFactory ccf;

    private static Channel channel;
    }
