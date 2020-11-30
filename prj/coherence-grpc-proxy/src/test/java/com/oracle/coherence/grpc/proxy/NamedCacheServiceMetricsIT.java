/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.proxy;

import com.google.protobuf.Int32Value;
import com.oracle.coherence.common.base.Classes;
import com.oracle.coherence.grpc.NamedCacheServiceGrpc;
import com.oracle.coherence.grpc.Requests;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.DefaultCacheServer;
import com.tangosol.net.NamedCache;
import com.tangosol.net.management.MBeanServerProxy;
import com.tangosol.net.management.Registry;
import com.tangosol.util.Base;
import com.tangosol.util.Filters;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.hamcrest.collection.IsMapContaining.hasKey;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;

/**
 * Integration tests to verify {@link NamedCacheServiceImpl}.
 *
 * @author Jonathan Knight  2019.11.01
 * @since 20.06
 */
public class NamedCacheServiceMetricsIT
    {
    // ----- test lifecycle -------------------------------------------------

    @BeforeAll
    static void setup()
        {
        System.setProperty("coherence.ttl", "0");
        System.setProperty("coherence.cluster", "NamedCacheServiceIT");
        System.setProperty("coherence.cacheconfig", "coherence-config.xml");
        System.setProperty("coherence.pof.config", "test-pof-config.xml");
        System.setProperty("coherence.override", "test-coherence-override.xml");
        System.setProperty(GrpcServerController.PROP_ENABLED, "true");

        DefaultCacheServer.startServerDaemon()
                .waitForServiceStart();

        s_ccf = CacheFactory.getCacheFactoryBuilder()
                .getConfigurableCacheFactory(Classes.getContextClassLoader());

        s_channel = ManagedChannelBuilder
                .forAddress("127.0.0.1", GrpcServerController.INSTANCE.getPort())
                .usePlaintext()
                .build();
        }

    @AfterAll
    static void cleanup()
        {
        s_channel.shutdownNow();
        DefaultCacheServer.shutdown();
        }

    // ----- test methods ---------------------------------------------------

    @Test
    public void shouldAccessCaches() throws Exception
        {
        String                     cacheName = "test-cache";
        NamedCache<String, String> cache     = s_ccf.ensureCache(cacheName, Base.getContextClassLoader());
        cache.put("key-1", "value-1");
        cache.put("key-2", "value-2");
        cache.put("key-3", "value-3");

        Registry         registry   = CacheFactory.getCluster().getManagement();
        MBeanServerProxy proxy      = registry.getMBeanServerProxy();
        Set<String>      setMBean   = proxy.queryNames(NamedCacheServiceImpl.MBEAN_NAME + ",*", Filters.always());
        String           sMBeanName = setMBean.stream().findFirst().orElse(null);

        assertThat(sMBeanName, is(notNullValue()));

        Map<String, Object>                         mapBefore = proxy.getAttributes(sMBeanName, Filters.always());
        NamedCacheServiceGrpc.NamedCacheServiceStub client    = NamedCacheServiceGrpc.newStub(s_channel);
        TestStreamObserver<Int32Value>              observer  = new TestStreamObserver<>();

        client.size(Requests.size(Requests.DEFAULT_SCOPE, cacheName), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors();

        // wait at least the required time for a metric snapshot update
        Thread.sleep(GrpcProxyMetrics.MIN_SNAPSHOT_REFRESH * 2);

        Map<String, Object> mapAfter = proxy.getAttributes(sMBeanName, Filters.always());

        TreeMap<String, Object> sorted = new TreeMap<>(mapBefore);
        for (Map.Entry<String, Object> entry : sorted.entrySet())
            {
            System.out.println(entry.getKey() + " " + entry.getValue() + " " + mapAfter.get(entry.getKey()));
            }

        assertThat(mapAfter, is(not(mapBefore)));
        }

    // ----- data members ---------------------------------------------------

    private static ConfigurableCacheFactory s_ccf;

    private static ManagedChannel s_channel;
    }
