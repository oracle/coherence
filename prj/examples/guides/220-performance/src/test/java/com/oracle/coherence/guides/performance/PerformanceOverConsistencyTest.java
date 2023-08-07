/*
 * Copyright (c) 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */


package com.oracle.coherence.guides.performance;

import com.oracle.bedrock.OptionsByType;

import com.oracle.bedrock.runtime.LocalPlatform;

import com.oracle.bedrock.runtime.coherence.CoherenceCacheServer;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.coherence.JMXManagementMode;
import com.oracle.bedrock.runtime.coherence.ServiceStatus;

import com.oracle.bedrock.runtime.coherence.options.CacheConfig;
import com.oracle.bedrock.runtime.coherence.options.ClusterName;
import com.oracle.bedrock.runtime.coherence.options.ClusterPort;
import com.oracle.bedrock.runtime.coherence.options.LocalStorage;
import com.oracle.bedrock.runtime.coherence.options.Logging;
import com.oracle.bedrock.runtime.coherence.options.MachineName;
import com.oracle.bedrock.runtime.coherence.options.MemberName;
import com.oracle.bedrock.runtime.coherence.options.Multicast;
import com.oracle.bedrock.runtime.coherence.options.WellKnownAddress;
import com.oracle.bedrock.runtime.java.options.SystemProperty;
import com.oracle.bedrock.runtime.java.profiles.JmxProfile;
import com.oracle.bedrock.runtime.network.AvailablePortIterator;
import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;

import com.tangosol.util.Processors;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;


import static org.hamcrest.CoreMatchers.is;


/**
 * Base test class for performance over consistency.
 *
 * @author Tim Middleton 2023.06.28
 */
public class PerformanceOverConsistencyTest {

    private static final String[] CUSTOMER_TYPES = new String[] {Customer.BRONZE, Customer.GOLD, Customer.SILVER};
    private static final String   SERVICE_NAME1  = "PartitionedCache";
    private static final String   SERVICE_NAME2  = "PartitionedCacheAsyncBackup";
    private static final String   SERVICE_NAME3  = "PartitionedCacheSchedBackup";
    private static final String   SERVICE_NAME4  = "PartitionedCacheReadLocator";
    private static final Random   RANDOM         = new Random();
    private static final String   CACHE_CONFIG   = "coherence-cache-config.xml";
    private static final String   TEST           = "perf";

    protected static AvailablePortIterator availablePortIteratorWKA;
    protected static CoherenceCacheServer  member1 = null;
    protected static CoherenceCacheServer  member2 = null;
    protected static CoherenceCacheServer  member3 = null;
    protected static CoherenceCacheServer  member4 = null;

    @BeforeAll
    public static void startup() {
        _startup();
    }

    @AfterAll
    public static void _shutdown() {
        CacheFactory.shutdown();
        destroyMember(member1);
        destroyMember(member2);
        destroyMember(member3);
        destroyMember(member4);
    }

    // tag::test[]
    @Test
    /**
     * Run the same test against different cache types.
     */
    public void testDifferentScenarios() {
        System.out.println("Running Tests");
        System.out.flush();
        final String headerFormat = "%-15s %12s %12s %12s %12s %12s\n";
        final String lineFormat   = "%-15s %,10dms %,10dms %,10dms %,10dms %,10dms\n";

        NamedCache<Integer, Customer> cache            = member1.getCache("base-customers");
        NamedCache<Integer, Customer> cacheReadLocator = member1.getCache("rl-customers");
        NamedCache<Integer, Customer> cacheAsyncBackup = member1.getCache("async-backup-customers");
        NamedCache<Integer, Customer> cacheSchedBackup = member1.getCache("sched-backup-customers");
        NamedCache<Integer, Customer> cacheNoBackup    = member1.getCache("no-backup-customers");

        List<TestResult> listResults = new ArrayList<>();

        log("");
        // discard the first run of each of the tests to ensure we have more consistent test results
        runTest(cache, "base");
        listResults.add(runTest(cache, "base"));

        runTest(cacheReadLocator, "base-rl");
        listResults.add(runTest(cacheReadLocator, "base-rl"));

        runTest(cacheAsyncBackup, "async-backup");
        listResults.add(runTest(cacheAsyncBackup, "async-backup"));

        runTest(cacheSchedBackup, "sched-backup");
        listResults.add(runTest(cacheSchedBackup, "sched-backup"));

        runTest(cacheNoBackup, "no-backup");
        listResults.add(runTest(cacheNoBackup, "no-backup"));

        // output the results
        System.out.printf(headerFormat, "Cache Type", "2k Put", "8 PutAll","100 Invoke", "2k Get", "100 GetAll");

        listResults.forEach(
                (v)->System.out.printf(lineFormat, v.getType(), v.getPutDuration(), v.getPutAllDuration(),
                        v.getInvokeDuration(), v.getGetDuration(), v.getGetAllDuration()));
        log("\nNote: The above times are to run the individual parts of tests, not to do an individual put/get, etc.\n");
    }
    // end::test[]

    // tag::runTest[]
    /**
     * Run various cache operations to test different configurations to achieve performance over consistency.
     *
     * @param cache {@link NamedCache} to test against
     * @param type  type of test
     * @return the test results
     */
    private TestResult runTest(NamedCache<Integer, Customer> cache, String type) {
        System.err.println("Starting " + type);
        cache.clear();

        long start = System.currentTimeMillis();

        System.err.println("Starting put " + type);
        // insert multiple customers using individual put()
        for (int i = 1; i <= 2_000; i++) {
            Customer c = getCustomer(i);
            cache.put(c.getId(), c);
        }
        long putDuration = System.currentTimeMillis() - start;

        Map<Integer, Customer> buffer = new HashMap<>();

        System.err.println("Starting putAll " + type);
        start = System.currentTimeMillis();
        // insert customers using putAll in batches
        for (int i = 2_001; i <= 10_000; i++) {
            Customer c = getCustomer(i);
            buffer.put(c.getId(), c);
            if (i % 1_000 == 0) {
                cache.putAll(buffer);
                buffer.clear();
            }
        }
        if (!buffer.isEmpty()) {
            cache.putAll(buffer);

        }

        long putAllDuration = System.currentTimeMillis() - start;

        System.err.println("Starting get " + type);
        start = System.currentTimeMillis();
        // issue 2,000 get() operations
        for (int i = 1; i < 2_000; i++) {
            Customer value = cache.get(i);
        }

        long getDuration = System.currentTimeMillis() - start;

        System.err.println("Starting getAll " + type);
        start = System.currentTimeMillis();
        // issue 100 getAll() operations
        for (int i = 1; i < 100; i++) {
            Map<Integer, Customer> all = cache.getAll(Set.of(i, i + 1, i + 2, i + 3, i + 4, i + 5));
        }

        long getAllDuration = System.currentTimeMillis() - start;

        start = System.currentTimeMillis();
        System.err.println("Starting entry processor " + type);
        // issue 100 entry processor updates which require backup updates
        for (int i = 1; i < 100L; i++) {
            cache.invoke(i, Processors.update(Customer::setCustomerType, Customer.GOLD));
        }
        long invokeDuration = System.currentTimeMillis() - start;

        cache.clear();
        return new TestResult(type, putDuration, putAllDuration, getDuration, getAllDuration, invokeDuration);
    }
    // end::runTest[]

    /**
     * Startup 4 cache servers.
     */
    public static void _startup() {
        LocalPlatform platform = LocalPlatform.get();
        availablePortIteratorWKA = platform.getAvailablePorts();
        availablePortIteratorWKA.next();

        int clusterPort = availablePortIteratorWKA.next();

        OptionsByType member1Options = createCacheServerOptions(clusterPort, TEST, 1, CACHE_CONFIG);
        OptionsByType member2Options = createCacheServerOptions(clusterPort, TEST, 2, CACHE_CONFIG);
        OptionsByType member3Options = createCacheServerOptions(clusterPort, TEST, 3, CACHE_CONFIG);
        OptionsByType member4Options = createCacheServerOptions(clusterPort, TEST, 4, CACHE_CONFIG);

        member1 = platform.launch(CoherenceCacheServer.class, member1Options.asArray());
        member2 = platform.launch(CoherenceCacheServer.class, member2Options.asArray());
        member3 = platform.launch(CoherenceCacheServer.class, member3Options.asArray());
        member4 = platform.launch(CoherenceCacheServer.class, member4Options.asArray());

        Eventually.assertDeferred(()->member1.getClusterSize(), is(4));

        Eventually.assertDeferred(()->member1.getServiceStatus(SERVICE_NAME1), is(ServiceStatus.MACHINE_SAFE));
        Eventually.assertDeferred(()->member1.getServiceStatus(SERVICE_NAME2), is(ServiceStatus.MACHINE_SAFE));
        Eventually.assertDeferred(()->member1.getServiceStatus(SERVICE_NAME3), is(ServiceStatus.MACHINE_SAFE));
        Eventually.assertDeferred(()->member1.getServiceStatus(SERVICE_NAME4), is(ServiceStatus.MACHINE_SAFE));
    }

    protected static OptionsByType createCacheServerOptions(int clusterPort, String testName, int member, String cacheConfig) {
        OptionsByType optionsByType = OptionsByType.empty();
        String        machine       = member % 2 == 0 ? "machine1" : "machine2";

        optionsByType.addAll(JMXManagementMode.ALL,
                JmxProfile.enabled(),
                LocalStorage.enabled(),
                WellKnownAddress.of("127.0.0.1"),
                Multicast.ttl(0),
                CacheConfig.of(cacheConfig),
                Logging.at(2),
                MemberName.of(testName + "-" + member),
                MachineName.of(machine),
                SystemProperty.of("-Xmx1g"),
                SystemProperty.of("-Xms1g"),
                ClusterName.of("performance"),
                ClusterPort.of(clusterPort));

        return optionsByType;
    }

    private static void destroyMember(CoherenceClusterMember member) {
        try {
            if (member != null) {
                member.close();
            }
        }
        catch (Exception eIgnore) {
            // ignored
        }
    }

    protected void log(String msg) {
        System.out.println("#### " + msg);
        System.out.flush();
    }

    protected static Customer getCustomer(int id) {
        return new Customer(id, "Customer Name " + id, "Address " + id,
                CUSTOMER_TYPES[RANDOM.nextInt(3)], RANDOM.nextLong(100_000L) + 1000L);
    }
}
