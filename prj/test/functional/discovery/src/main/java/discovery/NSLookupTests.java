/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package discovery;

import com.oracle.bedrock.deferred.DeferredCallable;
import com.oracle.bedrock.runtime.coherence.options.Logging;
import com.oracle.bedrock.runtime.java.options.IPv4Preferred;
import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.bedrock.runtime.LocalPlatform;
import com.oracle.bedrock.runtime.coherence.CoherenceCacheServer;
import com.oracle.bedrock.runtime.coherence.CoherenceCluster;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterBuilder;

import com.oracle.bedrock.runtime.coherence.options.ClusterName;
import com.oracle.bedrock.runtime.coherence.options.OperationalOverride;
import com.oracle.bedrock.runtime.java.options.SystemProperty;
import com.oracle.bedrock.runtime.java.profiles.JmxProfile;
import com.oracle.bedrock.runtime.network.AvailablePortIterator;

import com.oracle.bedrock.runtime.options.DisplayName;
import com.oracle.bedrock.testsupport.junit.TestLogs;
import com.oracle.bedrock.util.Capture;
import com.tangosol.discovery.NSLookup;

import com.tangosol.net.management.MBeanConnector;
import com.tangosol.util.Base;

import java.net.InetSocketAddress;

import java.util.Set;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import com.oracle.coherence.testing.AbstractFunctionalTest;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests for NSLookup helper class.
 *
 * @author bb  2014.04.15
 */
public class NSLookupTests
    {
    /**
     * Test the behavior of {@link NSLookup#lookupJMXServiceURL(java.net.SocketAddress)} ()},
     */
    @Test
    public void testLookup() throws Exception
        {
        final LocalPlatform         platform     = LocalPlatform.get();
        final String                hostName     = platform.getLoopbackAddress().getHostAddress();
        final AvailablePortIterator ports        = platform.getAvailablePorts();
        final Capture<Integer>      nClusterPort = new Capture<>(ports);
        final Capture<Integer>      nRMIPort     = new Capture<>(ports);

        CoherenceClusterBuilder clusterBuilder = new CoherenceClusterBuilder();

        clusterBuilder.include(1,
                               CoherenceCacheServer.class,
                               DisplayName.of("NSLookup"),
                               ClusterName.of("NSLookup"),
                               OperationalOverride.of("common/tangosol-coherence-override.xml"),
                               SystemProperty.of("test.multicast.address",
                                                 AbstractFunctionalTest.generateUniqueAddress(true)),
                               SystemProperty.of("test.multicast.port", nClusterPort),
                               SystemProperty.of("coherence.management", "dynamic"),
                               JmxProfile.enabled(),
                               JmxProfile.authentication(false),
                               JmxProfile.hostname(hostName),
                               SystemProperty.of(MBeanConnector.RMI_CONNECTION_PORT_PROPERTY, nRMIPort),
                               Logging.atMax(),
                               IPv4Preferred.yes(),
                               m_testLogs.builder()
                               );

        try (CoherenceCluster testCluster = clusterBuilder.build())
            {
            // ensure the clusters are established
            Eventually.assertDeferred(testCluster::getClusterSize, is(1));
            Eventually.assertDeferred(testCluster::isReady, is(true));

            InetSocketAddress address = new InetSocketAddress(hostName, nClusterPort.get());

            DeferredCallable<JMXServiceURL> deferredJmxURL = new DeferredCallable<>(() ->
                    NSLookup.lookupJMXServiceURL(address), JMXServiceURL.class);

            Eventually.assertDeferred(deferredJmxURL, is(notNullValue()));

            JMXServiceURL         jmxServiceURL = deferredJmxURL.get();
            JMXConnector          jmxConnector  = JMXConnectorFactory.connect(jmxServiceURL, null);
            MBeanServerConnection conn          = jmxConnector.getMBeanServerConnection();
            Set<ObjectName>       setCluster    = conn.queryNames(new ObjectName("Coherence:type=Cluster,*"), null);

            assertThat(setCluster, is(notNullValue()));
            assertThat(setCluster.size(), is(1));
            }
        }

    // ----- data members ---------------------------------------------------

    @Rule
    public final TestLogs m_testLogs = new TestLogs(NSLookupTests.class);
    }
