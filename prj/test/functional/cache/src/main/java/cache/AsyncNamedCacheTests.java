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
import com.oracle.bedrock.runtime.coherence.options.LocalHost;
import com.oracle.bedrock.runtime.coherence.options.LocalStorage;
import com.oracle.bedrock.runtime.coherence.options.WellKnownAddress;
import com.oracle.bedrock.runtime.java.options.IPv4Preferred;
import com.oracle.bedrock.runtime.options.DisplayName;
import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.testsupport.junit.TestLogs;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.NamedCache;

import java.util.Arrays;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static com.tangosol.net.cache.TypeAssertion.withoutTypeChecking;
import static org.hamcrest.CoreMatchers.is;

@RunWith(Parameterized.class)
public class AsyncNamedCacheTests
        extends BaseAsyncNamedCacheTests
    {
    /**
     * Create a {@link AsyncNamedCacheTests} instance.
     *
     * @param sDescription  the human-readable cache type for the test description.
     * @param sCacheName    the cache name to test (should map to a valid name in the coherence-cache-config.xml file
     *                      in this module's resources/ folder).
     */
    public AsyncNamedCacheTests(String sDescription, String sCacheName)
        {
        super(sDescription, sCacheName);
        }

    @BeforeClass
    public static void _startup()
        {
        CoherenceCluster cluster = s_cluster.getCluster();
        Eventually.assertDeferred(cluster::getClusterSize, is(3));

        s_ccf = s_cluster.createSession(SessionBuilders.storageDisabledMember());
        }

    /**
     * Provide the parameters for the tests.
     * <p/>
     * This method returns arrays of parameter pairs that will be passed to
     * the constructor of this class. The first parameter is a descriptive
     * name of the type of cache being tested, the second is the name of
     * the cache. The cache name must map to a valid name in the
     * coherence-cache-config.xml file in this module's resources/ folder.
     *
     * @return parameters for the test
     */
    @Parameterized.Parameters(name="{0}")
    public static Iterable<Object[]> data()
        {
        return Arrays.asList(
            new Object[] {"Distributed Cache", "dist-test"},
            new Object[] {"Near Cache", "near-test"},
            new Object[] {"Elastic Flash", "flash-test"},
            new Object[] {"Elastic RAM", "ram-test"}
            );
        }

    @Override
    protected <K, V> NamedCache<K, V> getNamedCache(String sCacheName)
        {
        NamedCache<K, V> cache = s_ccf.ensureTypedCache(sCacheName, null, withoutTypeChecking());
        cache.clear();
        return cache;
        }

    @ClassRule
    public static final TestLogs s_testLogs = new TestLogs(AsyncNamedCacheTests.class);

    @ClassRule
    public static CoherenceClusterResource s_cluster = new CoherenceClusterResource()
            .with(ClusterName.of("AsyncNamedCacheTests"),
                    CacheConfig.of("coherence-cache-config.xml"),
                    WellKnownAddress.loopback(),
                    LocalHost.only())
            .include(3, CoherenceClusterMember.class,
                    LocalStorage.enabled(),
                    IPv4Preferred.autoDetect(),
                    DisplayName.of("Storage"),
                    s_testLogs);

    protected static ConfigurableCacheFactory s_ccf;
    }
