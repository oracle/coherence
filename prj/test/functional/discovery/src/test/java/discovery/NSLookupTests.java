/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package discovery;

import com.oracle.bedrock.deferred.DeferredCallable;

import com.oracle.bedrock.runtime.LocalPlatform;
import com.oracle.bedrock.runtime.coherence.CoherenceCacheServer;
import com.oracle.bedrock.runtime.coherence.CoherenceCluster;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterBuilder;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.coherence.options.ClusterName;
import com.oracle.bedrock.runtime.coherence.options.Logging;
import com.oracle.bedrock.runtime.coherence.options.OperationalOverride;
import com.oracle.bedrock.runtime.java.options.IPv4Preferred;
import com.oracle.bedrock.runtime.java.options.SystemProperty;
import com.oracle.bedrock.runtime.network.AvailablePortIterator;
import com.oracle.bedrock.runtime.options.DisplayName;

import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.testsupport.junit.TestLogs;

import com.oracle.bedrock.util.Capture;

import com.tangosol.discovery.NSLookup;

import com.tangosol.net.management.MBeanConnector;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import java.util.Collection;
import java.util.Set;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
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
    @BeforeClass
    public static void startup()
        {
        final LocalPlatform platform = LocalPlatform.get();
        s_sHostName = platform.getLoopbackAddress().getHostAddress();
        final AvailablePortIterator ports = platform.getAvailablePorts();
        s_nClusterPort = new Capture<>(ports).next();
        final Capture<Integer> nRMIPort = new Capture<>(ports);
        final Capture<Integer> nRMIRegistryPort = new Capture<>(ports);
        s_nProxyPort = new Capture<>(ports).next();
        s_addrNameService = new InetSocketAddress(s_sHostName, s_nClusterPort);

        CoherenceClusterBuilder clusterBuilder = new CoherenceClusterBuilder();

        clusterBuilder.include(1,
                               CoherenceCacheServer.class,
                               DisplayName.of("NSLookup"),
                               ClusterName.of("NSLookup"),
                               OperationalOverride.of("common/tangosol-coherence-override.xml"),
                               SystemProperty.of("coherence.wka", s_sHostName),
                               SystemProperty.of("test.multicast.port", s_nClusterPort),
                               SystemProperty.of("coherence.extend.port", s_nProxyPort),
                               SystemProperty.of("coherence.extend.enabled", "true"),
                               SystemProperty.of("tangosol.coherence.management", "dynamic"),
                               SystemProperty.of("coherence.management", "dynamic"),
                               SystemProperty.of("com.sun.management.jmxremote.authenticate", "false"),
                               SystemProperty.of("com.sun.management.jmxremote.ssl", "false"),
                               SystemProperty.of(MBeanConnector.RMI_HOST_PROPERTY, s_sHostName),
                               SystemProperty.of(MBeanConnector.RMI_CONNECTION_PORT_PROPERTY, nRMIPort),
                               SystemProperty.of(MBeanConnector.RMI_REGISTRY_PORT_PROPERTY, nRMIRegistryPort),
                               Logging.at(9),
                               IPv4Preferred.yes(),
                               s_testLogs);

        s_testCluster = clusterBuilder.build();

        Eventually.assertDeferred(s_testCluster::getClusterSize, is(1));
        CoherenceClusterMember member = s_testCluster.iterator().next();
        Eventually.assertThat(invoking(member).isServiceRunning(PROXY_SERVICE_NAME), is(true));
        }

    @AfterClass
    public static void cleanup()
        {
        if (s_testCluster != null)
            {
            s_testCluster.close();
            }
        }

    /**
     * Test the behavior of {@link NSLookup#lookupJMXServiceURL(java.net.SocketAddress)} ()}.
     */
    @Test
    public void testLookupJMXServiceURL() throws Exception
        {
        DeferredCallable<JMXServiceURL> deferredJmxURL =
                new DeferredCallable<>(() -> NSLookup.lookupJMXServiceURL(s_addrNameService), JMXServiceURL.class);

        Eventually.assertDeferred(deferredJmxURL, is(notNullValue()));

        JMXServiceURL jmxServiceURL = deferredJmxURL.get();

        assertThat(jmxServiceURL.getURLPath().startsWith("/jndi/rmi://"), is(true));

        JMXConnector          jmxConnector = JMXConnectorFactory.connect(jmxServiceURL, null);
        MBeanServerConnection conn         = jmxConnector.getMBeanServerConnection();
        Set<ObjectName>       setCluster   = conn.queryNames(new ObjectName("Coherence:type=Cluster,*"), null);

        assertThat(setCluster, is(notNullValue()));
        assertThat(setCluster.size(), is(1));
        }

    /**
     * Test the behavior of {@link NSLookup#lookupExtendProxy(String, SocketAddress, String)} ()}.
     */
    @Test
    public void testLookupProxy() throws Exception
        {
        Collection<SocketAddress> col = NSLookup.lookupExtendProxy("NSLookup", s_addrNameService, PROXY_SERVICE_NAME);

        assertThat(col.size(), is(1));
        assertThat("confirm known proxy address and port returned by NSLookup.lookupExtendProxy",
                col.iterator().next(), is(new InetSocketAddress(s_sHostName, s_nProxyPort)));
        }

    // ----- data members ---------------------------------------------------

    public static final String PROXY_SERVICE_NAME = "TcpProxyService";

    public static CoherenceCluster s_testCluster;
    public static String           s_sHostName;
    public static int              s_nClusterPort;
    public static SocketAddress    s_addrNameService;
    public static int              s_nProxyPort;

    @ClassRule
    public static final TestLogs s_testLogs = new TestLogs();
    }
