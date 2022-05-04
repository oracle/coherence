/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package events;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;

import com.tangosol.net.events.partition.cache.CacheLifecycleEvent;
import com.tangosol.net.events.partition.cache.CacheLifecycleEvent.Type;

import com.tangosol.util.CompositeKey;

import com.oracle.coherence.testing.AbstractFunctionalTest;

import org.hamcrest.Matchers;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static org.hamcrest.core.Is.is;

/**
 * Tests for {@link CacheLifecycleEvent}.
 *
 * @author bbc 2015-09-15
 */
public class CacheLifecycleEventTests
        extends AbstractFunctionalTest
    {

    // ----- constructors ---------------------------------------------------

    /**
    * Default Constructor.
    */
    public CacheLifecycleEventTests()
        {
        super(CFG_FILE);
        }

    // ----- test lifecycle -------------------------------------------------

    /**
     * Initialize the test class.
     */
    @BeforeClass
    public static void _startup()
        {
        AbstractFunctionalTest._startup();

        startCacheServer("cacheLife-1", "events", CFG_FILE);
        startCacheServer("cacheLife-2", "events", CFG_FILE);
        }

    // ----- test methods ---------------------------------------------------

    /**
     * Test that we can modify an insert event.
     */
    @Test
    public void testEvents()
            throws InterruptedException
        {
        NamedCache<CompositeKey<Integer, String>, String> cacheResults = getNamedCache("result");

        Integer NLocalId    = CacheFactory.getCluster().getLocalMember().getId();
        Integer NStorageId1 = findCacheServer("cacheLife-1").getId();
        Integer NStorageId2 = findCacheServer("cacheLife-2").getId();

        NamedCache<String, String> cache = getNamedCache("cacheLife");

        // start a cache server after the ensureCache
        startCacheServer("cacheLife-3", "events", CFG_FILE);

        Integer NStorageId3 = findCacheServer("cacheLife-3").getId();

        Eventually.assertDeferred(() -> cacheResults,
                Matchers.allOf(
                        Matchers.hasEntry(new CompositeKey<>(NLocalId,    Type.CREATED.name()), "cacheLife"),
                        Matchers.hasEntry(new CompositeKey<>(NStorageId1, Type.CREATED.name()), "cacheLife"),
                        Matchers.hasEntry(new CompositeKey<>(NStorageId2, Type.CREATED.name()), "cacheLife"),
                        Matchers.hasEntry(new CompositeKey<>(NStorageId3, Type.CREATED.name()), "cacheLife")));

        stopCacheServer("cacheLife-3");

        cache.put("k1", "v1");
        cache.put("k2", "v2");

        cache.truncate();

        Eventually.assertDeferred(() -> cacheResults,
                Matchers.allOf(
                        Matchers.hasEntry(new CompositeKey<>(NLocalId, Type.TRUNCATED.name()), "cacheLife"),
                        Matchers.hasEntry(new CompositeKey<>(NStorageId1, Type.TRUNCATED.name()), "cacheLife"),
                        Matchers.hasEntry(new CompositeKey<>(NStorageId2, Type.TRUNCATED.name()), "cacheLife")));

        cacheResults.clear();

        cache.put("k1", "v1");
        cache.put("k2", "v2");

        // test that uem event is raised for second time cache truncate
        cache.truncate();

        Eventually.assertDeferred(() -> cacheResults,
                Matchers.allOf(
                        Matchers.hasEntry(new CompositeKey<>(NLocalId, Type.TRUNCATED.name()), "cacheLife"),
                        Matchers.hasEntry(new CompositeKey<>(NStorageId1, Type.TRUNCATED.name()), "cacheLife"),
                        Matchers.hasEntry(new CompositeKey<>(NStorageId2, Type.TRUNCATED.name()), "cacheLife")));

        cache.put("k1", "v1");
        cache.put("k2", "v2");

        cache.destroy();

        Eventually.assertDeferred(() -> cacheResults,
                Matchers.allOf(
                        Matchers.hasEntry(new CompositeKey<>(NLocalId, Type.DESTROYED.name()), "cacheLife"),
                        Matchers.hasEntry(new CompositeKey<>(NStorageId1, Type.DESTROYED.name()), "cacheLife"),
                        Matchers.hasEntry(new CompositeKey<>(NStorageId2, Type.DESTROYED.name()), "cacheLife")));

        cacheResults.destroy();
        }

    // ----- data members ---------------------------------------------------

    /**
     * The Cache config file to use for these tests.
     */
    public static String CFG_FILE = "server-cache-config.xml";
    }