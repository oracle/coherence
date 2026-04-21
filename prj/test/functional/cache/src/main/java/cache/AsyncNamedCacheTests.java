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
import com.tangosol.net.AsyncNamedMap;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.NamedCache;

import java.util.Arrays;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import com.tangosol.net.NamedMap;
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
    @SuppressWarnings("unused")
    public AsyncNamedCacheTests(String sDescription, String sCacheName, AsyncNamedMap.Option[] opts)
        {
        super(sCacheName, opts);
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
    @SuppressWarnings("ConstantForZeroLengthArrayAllocation")
    public static Iterable<Object[]> data()
        {
        Executor               executor     = Executors.newSingleThreadExecutor();
        AsyncNamedMap.Option[] optsExecutor = new AsyncNamedMap.Option[]{AsyncNamedMap.Complete.using(executor)};
        AsyncNamedMap.Option[] opts         = new AsyncNamedMap.Option[0];

        return Arrays.asList(
            new Object[] {"Distributed Cache", "dist-test", opts},
            new Object[] {"Distributed Cache (with executor)", "dist-test", optsExecutor},
            new Object[] {"Near Cache", "near-test", opts},
            new Object[] {"Near Cache (with executor)", "near-test", optsExecutor},
            new Object[] {"Elastic Flash", "flash-test", opts},
            new Object[] {"Elastic Flash (with executor)", "flash-test", optsExecutor},
            new Object[] {"Elastic RAM", "ram-test", opts},
            new Object[] {"Elastic RAM (with executor)", "ram-test", optsExecutor},
            new Object[] {"View Cache", "view-dist-test", opts},
            new Object[] {"View Cache (with executor)", "view-dist-test", optsExecutor}
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
