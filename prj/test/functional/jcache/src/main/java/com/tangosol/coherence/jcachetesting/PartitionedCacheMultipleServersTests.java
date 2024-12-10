/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.jcachetesting;

import com.oracle.bedrock.OptionsByType;
import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.runtime.ApplicationConsoleBuilder;
import com.oracle.bedrock.runtime.LocalPlatform;
import com.oracle.bedrock.runtime.coherence.CoherenceCacheServer;
import com.oracle.bedrock.runtime.coherence.CoherenceCluster;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterBuilder;
import com.oracle.bedrock.runtime.coherence.JMXManagementMode;
import com.oracle.bedrock.runtime.coherence.options.CacheConfig;
import com.oracle.bedrock.runtime.coherence.options.LocalHost;
import com.oracle.bedrock.runtime.coherence.options.LocalStorage;
import com.oracle.bedrock.runtime.coherence.options.Pof;
import com.oracle.bedrock.runtime.console.FileWriterApplicationConsole;
import com.oracle.bedrock.runtime.java.options.Freeform;
import com.oracle.bedrock.runtime.java.options.Freeforms;
import com.oracle.bedrock.runtime.java.options.IPv4Preferred;
import com.oracle.bedrock.runtime.java.options.SystemProperty;
import com.oracle.bedrock.runtime.java.profiles.JmxProfile;

import com.oracle.bedrock.runtime.options.Console;
import com.tangosol.coherence.jcache.partitionedcache.PartitionedCacheConfiguration;
import com.tangosol.coherence.jcache.CoherenceBasedCompleteConfiguration;

import com.tangosol.net.CacheFactory;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;

import static com.oracle.coherence.testing.AbstractFunctionalTest.ensureOutputDir;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

import static org.hamcrest.number.OrderingComparison.greaterThanOrEqualTo;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import java.util.List;
import java.util.Map;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;

import javax.cache.configuration.FactoryBuilder;

import javax.cache.expiry.Duration;
import javax.cache.expiry.ExpiryPolicy;

/**
 * Junit test for Coherence adapter impl of jcache with multiple server nodes.
 * Test configures server s_cluster in startCluster method.
 *
 * @author  jf 2014.01.28
 * @since Coherence 12.1.3
 */
public class PartitionedCacheMultipleServersTests
        extends AbstractCoherenceCacheTests
    {
    /**
     * Start a s_cluster with {@link #C_CLUSTER_MEMBERS} storage-enabled members.
     */

    @BeforeClass
    public static void startCluster()
            throws Exception
        {
        Caching.getCachingProvider().close();
        CacheFactory.shutdown();
        String sLocalStorage = System.getProperty("coherence.distributed.localstorage", "false");
        System.setProperty("coherence.distributed.localstorage", sLocalStorage);
        System.setProperty("coherence.serializer", "pof");


        beforeClassSetup();

        OptionsByType           storage        = createStorageNodeOptions();

        CoherenceClusterBuilder clusterBuilder = new CoherenceClusterBuilder();

        clusterBuilder.include(C_CLUSTER_MEMBERS,
                CoherenceCacheServer.class,
                storage.asArray());

        ApplicationConsoleBuilder bldrConsole =
            FileWriterApplicationConsole.builder(ensureOutputDir("jcache").getAbsolutePath(),
                PartitionedCacheMultipleServersTests.class.getSimpleName());

        s_cluster = clusterBuilder.build(Console.of(bldrConsole.build("JCache.log")));

        Eventually.assertThat(invoking(s_cluster).getClusterSize(), greaterThanOrEqualTo(C_CLUSTER_MEMBERS));
        System.out.println("Cluster started with " + s_cluster.getClusterSize() + " members");

        // Assert the s_cluster is ready
        Assert.assertThat(s_cluster, is(notNullValue()));
        }

    @Override
    protected <K, V> CoherenceBasedCompleteConfiguration<K, V> getConfiguration()
        {
        return new PartitionedCacheConfiguration<K, V>();
        }

    @Before
    public void setupTest()
        {
        super.setupTest();
        lsConfiguration = new PartitionedCacheConfiguration<Long, String>();
        ((PartitionedCacheConfiguration) lsConfiguration).setTypes(Long.class, String.class);
        ((PartitionedCacheConfiguration) lsConfiguration).setExpiryPolicyFactory(FactoryBuilder.factoryOf(
            (StaticInnerNonSerializableExpiryPolicy.class)));
        spConfiguration = new PartitionedCacheConfiguration<String, Point>();
        ((PartitionedCacheConfiguration) spConfiguration).setTypes(String.class, Point.class);
        ((PartitionedCacheConfiguration) spConfiguration).setExpiryPolicyFactory(FactoryBuilder.factoryOf(
            StaticInnerNonSerializableExpiryPolicy.class));

        snpConfiguration = new PartitionedCacheConfiguration<String, NonPofPoint>();
        ((PartitionedCacheConfiguration) snpConfiguration).setTypes(String.class, NonPofPoint.class);
        ((PartitionedCacheConfiguration) snpConfiguration).setExpiryPolicyFactory(FactoryBuilder.factoryOf(
            StaticInnerNonSerializableExpiryPolicy.class));

        iiConfiguration = new PartitionedCacheConfiguration<Integer, Integer>();
        ((PartitionedCacheConfiguration) iiConfiguration).setTypes(Integer.class, Integer.class);
        ((PartitionedCacheConfiguration) iiConfiguration).setExpiryPolicyFactory(FactoryBuilder.factoryOf(
            StaticInnerNonSerializableExpiryPolicy.class));

        ssConfiguration = new PartitionedCacheConfiguration<String, String>();
        ((PartitionedCacheConfiguration) ssConfiguration).setTypes(String.class, String.class);
        ((PartitionedCacheConfiguration) ssConfiguration).setExpiryPolicyFactory(FactoryBuilder.factoryOf(
            StaticInnerNonSerializableExpiryPolicy.class));

        slConfiguration = new PartitionedCacheConfiguration<String, List>();
        ((PartitionedCacheConfiguration) slConfiguration).setTypes(String.class, List.class);
        ((PartitionedCacheConfiguration) slConfiguration).setExpiryPolicyFactory(FactoryBuilder.factoryOf(
            StaticInnerNonSerializableExpiryPolicy.class));

        smConfiguration = new PartitionedCacheConfiguration<String, Map>();
        ((PartitionedCacheConfiguration) smConfiguration).setTypes(String.class, Map.class);
        ((PartitionedCacheConfiguration) smConfiguration).setExpiryPolicyFactory(FactoryBuilder.factoryOf(
            StaticInnerNonSerializableExpiryPolicy.class));

        }

    @After
    public void cleanupAfterTest()
        {
        super.cleanupAfterTest();
        }

    @AfterClass
    public static void stopCluster()
        {
        if (s_cluster != null)
            {
            s_cluster.close();
            s_cluster = null;
            }
        afterClassSetup();
        }

    /**
     * create storage node schema
     *
     * @return storage enabled {@link OptionsByType}
     */
    private static OptionsByType createStorageNodeOptions()
        {
        OptionsByType optionsByType = createCommonOptions().add(LocalStorage.enabled(true));

        if (Boolean.getBoolean("tangosol.pof.enabled"))
            {
            optionsByType.addAll(SystemProperty.of("coherence.serializer", "pof"), Pof.config("coherence-jcache-junit-pof-config.xml"), Pof.enabled());
            }

        return optionsByType;
        }

    /**
     * Create {@link OptionsByType} that will be used to configure cluster members.
     *
     * @return the {@link OptionsByType}
     */
    private static OptionsByType createCommonOptions()
        {
        String localHostName = LocalPlatform.get().getLoopbackAddress().getHostAddress();

        System.out.println("OraclebedrockGetLocalHost()=" + localHostName);

        String cacheconfigfile  = System.getProperty("coherence.cacheconfig",
                                     "coherence-jcache-cache-config.xml");
        String[] defaultJvmOpts = new String[]{ "-server",
                    "-XX:+HeapDumpOnOutOfMemoryError",
                    "-XX:HeapDumpPath=" + ensureOutputDir("jcache").getAbsolutePath(),
                    "-XX:+ExitOnOutOfMemoryError"};

        return  OptionsByType.of(
                CacheConfig.of(cacheconfigfile),
                new Freeforms(new Freeform(defaultJvmOpts)),
                JmxProfile.enabled(),
                JMXManagementMode.LOCAL_ONLY,
                JmxProfile.authentication(false),
                JmxProfile.hostname(localHostName),
                SystemProperty.of("java.rmi.server.hostname", localHostName),
                LocalHost.only(),
                IPv4Preferred.yes()
                );
        }

    /**
     * Regression test for jcache failures in BugDB 19439602
     *
     * Ensures that user-defined scheme is compatible with default
     * scheme generation by JCacheNamespace.  The cache servers are started with
     * distributed-scheme defined in cache-config, this client joins them with
     * a cache configuration entirely generated by JCacheNamespace handler.
     * This test will catch discrepancy between defined distirbuted-scheme
     * and JCacheNamespace generated distributed-scheme.
     *
     * Here was error message:
     *
     * (thread=DistributedCache:jcache-configurations-distributed-service):
     * Incompatible PersistenceEnvironment implementation: this node is
     * configured to use none, but the service senior is using
     * com.tangosol.persistence.bdb.BerkeleyDBEnvironment; stopping the service.
     */

    // disable till fix checked in for JCacheNamespace.
    // @Test
    public void testJCacheNamespaceConfigurationGeneration()
            throws URISyntaxException
        {
        // open cache manager with a cache-config that is completely generated via JCacheNamespace.
        CacheManager mgr =
            Caching.getCachingProvider().getCacheManager(new URI("coherence-jcache-all-generated-cache-config.xml"),
                null, null);
        Cache cache = null;

        try
            {
            cache = mgr.createCache("foo", getConfiguration());
            cache.put(1, 2);
            assertEquals(2, cache.get(1));
            }
        finally
            {
            if (cache != null)
                {
                mgr.destroyCache(cache.getName());
                }

            if (mgr != null)
                {
                mgr.close();
                }
            }
        }

    @Test
    public void loadAll_1Found1Not()
            throws Exception
        {
        // EXCLUDING:
        //
        // this test is not written properly to work for distributed multiple servers.
        // there is a loader in each storage-enabled server, this does not work correctly to
        // perform a loader assertion only in client. need to aggregate map in loader across
        // all distributed servers.

        // just disable this test for now.
        }

    // ----- inner class ----------------------------------------------------

    /**
     * A NonSerializableExpiryPolicy
     *
     * @author  jf 2014.05.13
     */
    public static class StaticInnerNonSerializableExpiryPolicy
            implements ExpiryPolicy
        {
        @Override
        public Duration getExpiryForCreation()
            {
            return Duration.ETERNAL;
            }

        @Override
        public Duration getExpiryForAccess()
            {
            return Duration.ETERNAL;
            }

        @Override
        public Duration getExpiryForUpdate()
            {
            return Duration.ETERNAL;
            }
        }

    // ----- constants ------------------------------------------------------

    /**
     * Number of members in s_cluster.  Tested up to 4.
     */
    public static final int C_CLUSTER_MEMBERS = 2;

    // ----- data members ---------------------------------------------------

    /**
     * common port for s_cluster.
     */
    private static CoherenceCluster s_cluster;
    }
