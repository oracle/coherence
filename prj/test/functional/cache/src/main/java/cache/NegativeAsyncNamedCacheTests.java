/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package cache;

import com.oracle.bedrock.junit.CoherenceClusterResource;
import com.oracle.bedrock.junit.SessionBuilders;

import com.oracle.bedrock.runtime.coherence.CoherenceCluster;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.coherence.options.CacheConfig;
import com.oracle.bedrock.runtime.coherence.options.ClusterName;
import com.oracle.bedrock.runtime.coherence.options.LocalStorage;

import com.oracle.bedrock.runtime.java.options.IPv4Preferred;

import com.oracle.bedrock.runtime.options.DisplayName;

import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.testsupport.junit.TestLogs;

import com.oracle.coherence.testing.AbstractFunctionalTest;

import com.tangosol.net.AsyncNamedCache;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.NamedCache;

import java.util.Arrays;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static com.tangosol.net.cache.TypeAssertion.withoutTypeChecking;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(Parameterized.class)
public class NegativeAsyncNamedCacheTests
        extends AbstractFunctionalTest
    {
    public NegativeAsyncNamedCacheTests(String sDescription, String sCacheName)
        {
        this.sDescription = sDescription;
        this.sCacheName = sCacheName;
        }

    @BeforeClass
    public static void _startup()
        {
        CoherenceCluster cluster = s_cluster.getCluster();
        Eventually.assertDeferred(cluster::getClusterSize, is(1));

        s_ccf = s_cluster.createSession(SessionBuilders.storageDisabledMember());
        }

    /**
     * Provide the parameters for the tests.
     * <p>
     * This method returns arrays of parameter pairs that will be passed to
     * the constructor of this class. The first parameter is a descriptive
     * name of the type of cache being tested, the second is the name of
     * the cache. The cache name must map to a valid name in the
     * negative-async-cache-config.xml file in this module's resources/ folder.
     *
     * @return parameters for the test
     */
    @Parameterized.Parameters(name="{0}")
    public static Iterable<Object[]> data()
        {
        return Arrays.asList(
                new Object[] {"Caffeine Local Cache", "caffeine-local"},
                new Object[] {"Local Cache", "local"},
                new Object[] {"Remote Cache", "remote"},
                new Object[] {"Extend View Cache", "view-extend"},
                new Object[] {"Extend Near Cache", "near-extend"}
                );
        }

    /**
     * Return the cache used by the tests.
     * <p>
     * This should be the plain {@link NamedCache} not an
     * {@link com.tangosol.net.AsyncNamedCache}.
     *
     * @param <K> the cache key type
     * @param <V> the cache value type
     *
     * @return the cache used by the tests
     */
    protected <K, V> NamedCache<K, V> getNamedCache()
        {
        NamedCache cache;
        if (sCacheName.contains("remote") || sCacheName.contains("extend"))
            {
            ConfigurableCacheFactory session = s_cluster.createSession(SessionBuilders.extendClient(CLIENT_CACHE_CONFIG));
            cache = session.ensureCache(sCacheName, null);
            }
        else
            {
            cache = s_ccf.ensureTypedCache(sCacheName, null, withoutTypeChecking());
            cache.clear();
            }
        return cache;
        }

    /**
     * Verify that an UnsupportedOperationException is thrown by cache types which do not support async.
     */
    @Test
    public void negativeTest()
        {
        NamedCache cache = getNamedCache();
        try
            {
            cache.async();

            fail("UnsupportedOperatedException expected");
            }
        catch(UnsupportedOperationException e)
            {
            }
        }

    @ClassRule
    public static final TestLogs s_testLogs = new TestLogs(NegativeAsyncNamedCacheTests.class);

    @ClassRule
    public static CoherenceClusterResource s_cluster = new CoherenceClusterResource()
            .with(ClusterName.of("NegativeAsyncNamedCacheTests"),
                  CacheConfig.of("negative-async-cache-config.xml"),
                  ClusterName.of("myCluster"),
                  DisplayName.of("server"))
            .include(1, CoherenceClusterMember.class,
                     LocalStorage.enabled(),
                     IPv4Preferred.autoDetect(),
                     DisplayName.of("Storage"),
                     s_testLogs);

    private String sDescription;

    private String sCacheName;

    protected static ConfigurableCacheFactory s_ccf;

    public static final String CLIENT_CACHE_CONFIG = "client-negative-async-cache-config.xml";
    }