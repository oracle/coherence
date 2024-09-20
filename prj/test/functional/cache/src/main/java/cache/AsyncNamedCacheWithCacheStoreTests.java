/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package cache;

import com.tangosol.net.Coherence;
import com.tangosol.net.NamedCache;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.tangosol.net.Session;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests for COH-20907 AsyncNamedCache putAll causing CacheLoader
 * to read through.
 *
 * @author cp  2020.04.29
 */
public class AsyncNamedCacheWithCacheStoreTests
    {
    @BeforeClass
    public static void setup() throws Exception
        {
        System.setProperty("coherence.ttl",        "0");
        System.setProperty("coherence.wka",        "127.0.0.1");
        System.setProperty("coherence.localhost",  "127.0.0.1");
        System.setProperty("coherence.cacheconfig", FILE_CFG_CACHE);

        Coherence coherence = Coherence.clusterMember().start().get(5, TimeUnit.MINUTES);
        s_session = coherence.getSession();
        }

    @Test
    public void testAsyncGetCallsLoadOnly() throws Exception
        {
        NamedCache<Integer, Integer> cache = s_session.getCache("test-coh20907-get");

        TestCacheStore.clear();
        cache.async().get(1).get(1, TimeUnit.MINUTES);

        assertThat(TestCacheStore.getErases().isEmpty(), is(true));
        assertThat(TestCacheStore.getStores().isEmpty(), is(true));
        Queue<?> loads = TestCacheStore.getLoads();
        assertThat(loads.size(), is(1));
        assertThat(loads.contains(1), is(true));
        }

    @Test
    public void testAsyncGetAllCallsLoadOnly() throws Exception
        {
        NamedCache<Integer, Integer> cache  = s_session.getCache("test-coh20907-getAll");
        Set<Integer>                 setKey = Set.of(1, 2, 3, 4);

        TestCacheStore.clear();
        cache.async().getAll(setKey).get(1, TimeUnit.MINUTES);

        assertThat(TestCacheStore.getErases().isEmpty(), is(true));
        assertThat(TestCacheStore.getStores().isEmpty(), is(true));
        Set<?> loads = TestCacheStore.getLoadsAsSet();
        assertThat(loads, is(setKey));
        }

    @Test
    public void testAsyncPutCallsStoreOnly() throws Exception
        {
        NamedCache<Integer, Integer> cache = s_session.getCache("test-coh20907-put");

        TestCacheStore.clear();
        cache.async().put(1, 100).get(1, TimeUnit.MINUTES);

        assertThat(TestCacheStore.getErases().isEmpty(), is(true));
        assertThat(TestCacheStore.getLoads().isEmpty(), is(true));
        assertThat(TestCacheStore.getStores().get(1), is(100));
        }

    @Test
    public void testAsyncPutAllCallsStoreOnly() throws Exception
        {
        NamedCache<Integer, Integer> cache = s_session.getCache("test-coh20907-putAll");
        Map<Integer, Integer>        map   = new HashMap<>();

        for (int i = 0; i < 10; ++i)
            {
            map.put(i, i);
            }

        TestCacheStore.clear();
        cache.async().putAll(map).get(1, TimeUnit.MINUTES);

        assertThat(TestCacheStore.getErases().isEmpty(), is(true));
        assertThat(TestCacheStore.getLoads().isEmpty(), is(true));
        assertThat(TestCacheStore.getStores(), is(map));
        }

    @Test
    public void testAsyncRemoveCallsLoadAndErase() throws Exception
        {
        NamedCache<Integer, Integer> cache = s_session.getCache("test-coh20907-remove");
        cache.clear();

        TestCacheStore.clear();
        TestCacheStore.put(1, 100);
        cache.async().remove(1).get(1, TimeUnit.MINUTES);

        assertThat(TestCacheStore.getStores().isEmpty(), is(true));

        Queue<?> loads = TestCacheStore.getLoads();
        assertThat(loads.size(), is(1));
        assertThat(loads.contains(1), is(true));

        Queue<?> erases = TestCacheStore.getErases();
        assertThat(erases.size(), is(1));
        assertThat(erases.contains(1), is(true));
        }

    @Test
    public void testAsyncRemoveKeyAndValueCallsLoadAndErase() throws Exception
        {
        NamedCache<Integer, Integer> cache = s_session.getCache("test-coh20907-removeKeyAndValue");
        cache.clear();

        TestCacheStore.clear();
        TestCacheStore.put(1, 100);
        cache.async().remove(1, 100).get(1, TimeUnit.MINUTES);

        assertThat(TestCacheStore.getStores().isEmpty(), is(true));

        Queue<?> loads = TestCacheStore.getLoads();
        assertThat(loads.size(), is(1));
        assertThat(loads.contains(1), is(true));

        Queue<?> erases = TestCacheStore.getErases();
        assertThat(erases.size(), is(1));
        assertThat(erases.contains(1), is(true));
        }

    @Test
    public void testAsyncRemoveAllCallsLoadAndErase() throws Exception
        {
        NamedCache<Integer, Integer> cache = s_session.getCache("test-coh20907-removeAll");
        Map<Integer, Integer>        map   = Map.of(1, 100, 2, 200, 3, 300, 4, 400);

        TestCacheStore.clear();
        TestCacheStore.putAll(map);
        cache.async().removeAll(map.keySet()).get(1, TimeUnit.MINUTES);

        assertThat(TestCacheStore.getStores().isEmpty(), is(true));

        Set<?> loads = TestCacheStore.getLoadsAsSet();
        assertThat(loads, is(map.keySet()));

        Set<?> erases = TestCacheStore.getErasesAsSet();
        assertThat(erases, is(map.keySet()));
        }

    // ----- constants and data members -------------------------------------

    /**
     * The name of cache configuration file.
     */
    public static final String FILE_CFG_CACHE = "server-cache-config.xml";

    private static Session s_session;
    }
