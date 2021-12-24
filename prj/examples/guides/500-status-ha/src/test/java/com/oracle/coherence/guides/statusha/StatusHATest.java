/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.guides.statusha;

import java.net.Socket;
import java.util.Set;

import com.oracle.bedrock.junit.CoherenceClusterExtension;
import com.oracle.bedrock.junit.SessionBuilders;
import com.oracle.bedrock.runtime.LocalPlatform;
import com.oracle.bedrock.runtime.coherence.JMXManagementMode;
import com.oracle.bedrock.runtime.coherence.options.CacheConfig;
import com.oracle.bedrock.runtime.coherence.options.ClusterName;
import com.oracle.bedrock.runtime.coherence.options.ClusterPort;
import com.oracle.bedrock.runtime.coherence.options.LocalHost;
import com.oracle.bedrock.runtime.coherence.options.LocalStorage;
import com.oracle.bedrock.runtime.coherence.options.Logging;
import com.oracle.bedrock.runtime.coherence.options.Multicast;
import com.oracle.bedrock.runtime.coherence.options.RoleName;
import com.oracle.bedrock.runtime.coherence.options.WellKnownAddress;
import com.oracle.bedrock.runtime.java.options.ClassName;
import com.oracle.bedrock.runtime.java.options.IPv4Preferred;
import com.oracle.bedrock.runtime.java.options.SystemProperty;
import com.oracle.bedrock.runtime.network.AvailablePortIterator;

import com.oracle.bedrock.runtime.options.DisplayName;
import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.testsupport.junit.TestLogsExtension;
import com.oracle.bedrock.util.Capture;
import com.oracle.coherence.guides.statusha.fetcher.DataFetcher;
import com.oracle.coherence.guides.statusha.fetcher.HTTPDataFetcher;
import com.oracle.coherence.guides.statusha.fetcher.JMXDataFetcher;
import com.oracle.coherence.guides.statusha.fetcher.MBeanServerProxyDataFetcher;
import com.oracle.coherence.guides.statusha.model.ServiceData;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.Coherence;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.NamedMap;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

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
     * A collection of available TCP ports.
     */
    private static final AvailablePortIterator ports = LocalPlatform.get().getAvailablePorts();

    /**
     * JMX Port.
     */
    private static final Capture<Integer> jmxPort = new Capture<>(ports);

    /**
     * Management Port.
     */
    private static final Capture<Integer> managementPort = new Capture<>(ports);

    /**
     * A Bedrock utility to capture logs of spawned processes into files
     * under target/test-output. This is added as an option to the cluster.
     */
    @RegisterExtension
    static TestLogsExtension logs = new TestLogsExtension(StatusHATest.class);

    /**
     * A Bedrock JUnit5 extension that starts a Coherence cluster, in this case made up of
     * two storage enabled members. The first member ("member1") is configured to
     * run both the JMX server and Coherence management over REST server.
     */
    @RegisterExtension
    static CoherenceClusterExtension coherenceCluster =
            new CoherenceClusterExtension()
                    .using(LocalPlatform.get())
                    .with(ClassName.of(Coherence.class),
                          Logging.at(9),
                          LocalHost.only(),
                          Multicast.ttl(0),
                          IPv4Preferred.yes(),
                          logs,
                          LocalStorage.enabled(),
                          CacheConfig.of(CACHE_CONFIG),
                          ClusterName.of(CLUSTER_NAME),
                          WellKnownAddress.of("127.0.0.1"),
                          JMXManagementMode.ALL,
                          ClusterPort.automatic())
                    .include(1,
                             RoleName.of("member1"),
                             DisplayName.of("member1"),
                             SystemProperty.of("com.sun.management.jmxremote", "true"),
                             SystemProperty.of("com.sun.management.jmxremote.authenticate", "false"),
                             SystemProperty.of("com.sun.management.jmxremote.ssl", "false"),
                             SystemProperty.of("com.sun.management.jmxremote.ssl", "false"),
                             SystemProperty.of("com.sun.management.jmxremote.port", jmxPort),
                             SystemProperty.of("java.rmi.server.hostname", "127.0.0.1"),
                             SystemProperty.of("coherence.management.http.host", "127.0.0.1"),
                             SystemProperty.of("coherence.management.http.port", managementPort),
                             SystemProperty.of("coherence.management.http", "inherit"))
                    .include(1,
                             RoleName.of("member2"),
                             DisplayName.of("member2"));

    /**
     * Startup the Coherence cluster members.
     */
    @BeforeAll
    public static void startup() {
        // Make sure the MBean server in member-1 is listening before we start
        Eventually.assertDeferred(StatusHATest::isMBeanServerListening, is(true));
        // Make sure the Management over REST server in member-1 is listening before we start
        Eventually.assertDeferred(StatusHATest::isRestServerListening, is(true));

        // Make this test a storage disabled cluster member using the properties from the cluster started above.
        ConfigurableCacheFactory ccf = coherenceCluster.createSession(SessionBuilders.storageDisabledMember());

        // Populate two caches
        NamedMap<Integer, String> namedMap1 = ccf.ensureCache("test1-cache", null);
        NamedMap<Integer, String> namedMap2 = ccf.ensureCache("test2-cache", null);

        for (int i = 0; i < 10000; i++) {
            namedMap1.put(i, "Value-" + i);
            namedMap2.put(i, "Value-" + i);
        }
    }

    @AfterAll
    public static void shutdown() {
        CacheFactory.shutdown();
    }

    @Test
    public void testMBeanServerProxyAllServices() {
        DataFetcher dataFetcher = new MBeanServerProxyDataFetcher(null);
        assertDataFetcher(new MBeanServerProxyDataFetcher(null), 3);
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
        DataFetcher dataFetcher = new JMXDataFetcher(getJmxURL(), null);
        assertDataFetcher(dataFetcher, 3);
        Set<String> serviceNames = dataFetcher.getServiceNames();
        assertThat(serviceNames.contains(SERVICE1), is(true));
        assertThat(serviceNames.contains(SERVICE2), is(true));
    }

    @Test
    public void testJMXOneService() {
        DataFetcher dataFetcher = new JMXDataFetcher(getJmxURL(), SERVICE2);
        assertDataFetcher(dataFetcher, 1);
        Set<String> serviceNames = dataFetcher.getServiceNames();
        assertThat(serviceNames.contains(SERVICE1), is(false));
        assertThat(serviceNames.contains(SERVICE2), is(true));
    }

    @Test
    public void testHttpAllServices() {
        DataFetcher dataFetcher = new HTTPDataFetcher(getManagementURL(), null);
        assertDataFetcher(dataFetcher, 3);
        Set<String> serviceNames = dataFetcher.getServiceNames();
        assertThat(serviceNames.contains(SERVICE1), is(true));
        assertThat(serviceNames.contains(SERVICE2), is(true));
    }

    @Test
    public void testHttpOneService() {
        DataFetcher dataFetcher = new HTTPDataFetcher(getManagementURL(), SERVICE2);
        assertDataFetcher(dataFetcher, 1);
        Set<String> serviceNames = dataFetcher.getServiceNames();
        assertThat(serviceNames.contains(SERVICE1), is(false));
        assertThat(serviceNames.contains(SERVICE2), is(true));
    }

    /**
     * Returns the JMX url to use to connect to the Coherence JMX server.
     *
     * @return the JMX url to use to connect to the Coherence JMX server
     */
    private String getJmxURL() {
        return "service:jmx:rmi:///jndi/rmi://127.0.0.1:" + jmxPort.get() + "/jmxrmi";
    }

    /**
     * Returns the management over rest url to use to connect to the Coherence management server.
     *
     * @return the management over rest url to use to connect to the Coherence management server
     */
    private String getManagementURL() {
        return "http://127.0.0.1:" + managementPort.get() + "/management/coherence/cluster";
    }

    /**
     * Test whether the remote MBean server is listening by trying to connect to the socket.
     *
     * @return {@code true} if the remote MBean server is listening
     */
    private static boolean isMBeanServerListening() {
        return isListening(jmxPort.get());
    }

    /**
     * Test whether the remote Management over REST server is listening by trying to connect to the socket.
     *
     * @return {@code true} if the remote Management over REST server is listening
     */
    private static boolean isRestServerListening() {
        return isListening(managementPort.get());
    }

    private static boolean isListening(int port) {
        try {
            try (Socket ignored = new Socket("127.0.0.1", port)) {
                return true;
            }
        } catch (Throwable t) {
            return false;
        }
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
}
