/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.guides.statusha;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Properties;
import java.util.Set;

import com.oracle.bedrock.OptionsByType;
import com.oracle.bedrock.runtime.LocalPlatform;
import com.oracle.bedrock.runtime.coherence.CoherenceCacheServer;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.coherence.options.ClusterName;
import com.oracle.bedrock.runtime.coherence.options.ClusterPort;
import com.oracle.bedrock.runtime.coherence.options.LocalStorage;
import com.oracle.bedrock.runtime.coherence.options.Logging;
import com.oracle.bedrock.runtime.coherence.options.Multicast;
import com.oracle.bedrock.runtime.coherence.options.RoleName;
import com.oracle.bedrock.runtime.coherence.options.WellKnownAddress;
import com.oracle.bedrock.runtime.java.options.SystemProperty;
import com.oracle.bedrock.runtime.network.AvailablePortIterator;

import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.coherence.guides.statusha.fetcher.DataFetcher;
import com.oracle.coherence.guides.statusha.fetcher.HTTPDataFetcher;
import com.oracle.coherence.guides.statusha.fetcher.JMXDataFetcher;
import com.oracle.coherence.guides.statusha.fetcher.MBeanServerProxyDataFetcher;
import com.oracle.coherence.guides.statusha.model.ServiceData;

import com.tangosol.discovery.NSLookup;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.Coherence;
import com.tangosol.net.NamedMap;
import com.tangosol.net.Session;

import javax.management.remote.JMXServiceURL;
import org.hamcrest.CoreMatchers;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;


import static com.oracle.bedrock.deferred.DeferredHelper.invoking;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;

/**
 * Test to ensure the {@link DataFetcher}s for this example work correctly.
 *
 * @author tam 2021.08.02
 */
public class StatusHATest {

    /**
     * Various constants.
     */
    private static final String CLUSTER_NAME = "test-cluster";
    private static final String CACHE_CONFIG = "test-cache-config.xml";
    private static final String SERVICE1 = "PartitionedCache1";
    private static final String SERVICE2 = "PartitionedCache2";

    /**
     * Cache servers.
     */
    private static CoherenceCacheServer member1;
    private static CoherenceCacheServer member2;

    /**
     * Properties for server startup.
     */
    private static Properties props;

    /**
     * JMX Port.
     */
    private static int jmxPort;

    /**
     * Management Port.
     */
    private static int managementPort;

    /**
     * Startup the Coherence cluster members.
     */
    @BeforeAll
    public static void startup() {
        LocalPlatform platform = LocalPlatform.get();
        AvailablePortIterator availablePortIterator = LocalPlatform.get().getAvailablePorts();
        jmxPort = availablePortIterator.next();
        managementPort = availablePortIterator.next();

        props = new Properties();
        props.put("coherence.cluster", CLUSTER_NAME);
        props.put("coherence.cacheconfig", CACHE_CONFIG);

        OptionsByType optionsByType = OptionsByType.empty();
        optionsByType.addAll(LocalStorage.enabled(), Multicast.ttl(0), Logging.at(9),
                WellKnownAddress.of("127.0.0.1"), ClusterName.of(CLUSTER_NAME));

        // add the properties to the Bedrock startup
        props.forEach((k, v) -> optionsByType.add(SystemProperty.of((String) k, (String) v)));

        OptionsByType optionsByTypeMember1 = OptionsByType.of(optionsByType)
                .addAll(RoleName.of("member1"),
                        SystemProperty.of("com.sun.management.jmxremote", "true"),
                        SystemProperty.of("com.sun.management.jmxremote.authenticate", "false"),
                        SystemProperty.of("com.sun.management.jmxremote.ssl", "false"),
                        SystemProperty.of("com.sun.management.jmxremote.ssl", "false"),
                        SystemProperty.of("com.sun.management.jmxremote.port", jmxPort),
                        SystemProperty.of("coherence.management.http.host", "127.0.0.1"),
                        SystemProperty.of("coherence.management.http.port", managementPort),
                        SystemProperty.of("coherence.management.http", "inherit")
                );
        OptionsByType optionsByTypeMember2 = OptionsByType.of(optionsByType).add(RoleName.of("member2"));

        member1 = platform.launch(CoherenceCacheServer.class, optionsByTypeMember1.asArray());
        member2 = platform.launch(CoherenceCacheServer.class, optionsByTypeMember2.asArray());

        Eventually.assertThat(invoking(member1).getClusterSize(), CoreMatchers.is(2));

        // set client properties
        System.setProperty("coherence.wka", "127.0.0.1");
        System.setProperty("coherence.cluster", CLUSTER_NAME);
        System.setProperty("coherence.distributed.localstorage", "false");
        System.setProperty("coherence.cacheconfig", CACHE_CONFIG);

        Coherence coherence = Coherence.client();
        coherence.start().join();
        Session session = coherence.getSession();

        NamedMap<Integer, String> namedMap1 = session.getMap("test1-cache");
        NamedMap<Integer, String> namedMap2 = session.getMap("test2-cache");

        for (int i = 0; i < 10000; i++) {
            namedMap1.put(i, "Value-" + i);
            namedMap2.put(i, "Value-" + i);
        }
    }

    @AfterAll
    public static void shutdown() {
        CacheFactory.shutdown();

        destroyMember(member1);
        destroyMember(member2);
    }

    @Test
    public void testMBeanServerProxyAllServices() {
        DataFetcher dataFetcher = new MBeanServerProxyDataFetcher(null);
        assertDataFetcher(new MBeanServerProxyDataFetcher(null), 2);
        Set<String> serviceNames = dataFetcher.getServiceNames();
        assertThat(serviceNames.contains(SERVICE1), is(true));
        assertThat(serviceNames.contains(SERVICE2), is(true));
    }

    @Test
    public void testMBeanServerProxyOneService() {
        DataFetcher dataFetcher = new MBeanServerProxyDataFetcher(SERVICE1);
        assertDataFetcher(dataFetcher, 1);
        Set<String> serviceNames = dataFetcher.getServiceNames();
        assertThat(serviceNames.contains(SERVICE1), is(true));
        assertThat(serviceNames.contains(SERVICE2), is(false));
    }

    @Test
    public void testJMXAllServices()  {
        DataFetcher dataFetcher = new JMXDataFetcher("service:jmx:rmi:///jndi/rmi://127.0.0.1:" + jmxPort + "/jmxrmi", null);
        assertDataFetcher(dataFetcher, 2);
        Set<String> serviceNames = dataFetcher.getServiceNames();
        assertThat(serviceNames.contains(SERVICE1), is(true));
        assertThat(serviceNames.contains(SERVICE2), is(true));
    }

    @Test
    public void testJMXOneService() {
        DataFetcher dataFetcher = new JMXDataFetcher("service:jmx:rmi:///jndi/rmi://127.0.0.1:" + jmxPort + "/jmxrmi", SERVICE2);
        assertDataFetcher(dataFetcher, 1);
        Set<String> serviceNames = dataFetcher.getServiceNames();
        assertThat(serviceNames.contains(SERVICE1), is(false));
        assertThat(serviceNames.contains(SERVICE2), is(true));
    }

    @Test
    public void testHttpAllServices() {
        DataFetcher dataFetcher = new HTTPDataFetcher("http://127.0.0.1:" + managementPort + "/management/coherence/cluster", null);
        assertDataFetcher(dataFetcher, 2);
        Set<String> serviceNames = dataFetcher.getServiceNames();
        assertThat(serviceNames.contains(SERVICE1), is(true));
        assertThat(serviceNames.contains(SERVICE2), is(true));
    }

    @Test
    public void testHttpOneService() {
        DataFetcher dataFetcher = new HTTPDataFetcher("http://127.0.0.1:" + managementPort + "/management/coherence/cluster", SERVICE2);
        assertDataFetcher(dataFetcher, 1);
        Set<String> serviceNames = dataFetcher.getServiceNames();
        assertThat(serviceNames.contains(SERVICE1), is(false));
        assertThat(serviceNames.contains(SERVICE2), is(true));
    }

    /**
     * Assert that the {@link DataFetcher} returns what is expected.
     *
     * @param dataFetcher      {@link DataFetcher} to test
     * @param expectedServices expected number of services
     */
    private static void assertDataFetcher(DataFetcher dataFetcher, int expectedServices) {
        assertThat(dataFetcher.getClusterName(), is(CLUSTER_NAME));
        assertThat(dataFetcher.getServiceNames().size(), is(expectedServices));

        Set<ServiceData> setData = dataFetcher.getStatusHaData();
        assertThat(setData, is(notNullValue()));
        assertThat(setData.size(), is(expectedServices));

        setData.forEach(StatusHATest::assertValid);
    }

    /**
     * Assert that a {@link ServiceData} object is valid.
     *
     * @param data {@link ServiceData} to check
     */
    private static void assertValid(ServiceData data) {
        assertThat(data, is(notNullValue()));
        assertThat(data.getHAStatus(), is(notNullValue()));
        assertThat(data.getStatus(), is(notNullValue()));
        assertThat(data.getPartitionCount(), is(notNullValue()));
        assertThat(data.getPartitionsEndangered(), is(notNullValue()));
        assertThat(data.getPartitionsVulnerable(), is(notNullValue()));
        assertThat(data.getPartitionsUnbalanced(), is(notNullValue()));
        assertThat(data.getStorageCount(), is(2));
    }

    /**
     * Destroy a member and ignore any errors.
     *
     * @param member the {@link CoherenceClusterMember} to destroy
     */
    private static void destroyMember(CoherenceClusterMember member) {
        try {
            if (member != null) {
                member.close();
            }
        } catch (Throwable thrown) {
            // ignored
        }
    }
}
