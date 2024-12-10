/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package ssl;

import com.oracle.bedrock.OptionsByType;
import com.oracle.bedrock.runtime.LocalPlatform;

import com.oracle.bedrock.runtime.coherence.CoherenceCluster;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterBuilder;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.coherence.JMXManagementMode;

import com.oracle.bedrock.runtime.coherence.options.CacheConfig;
import com.oracle.bedrock.runtime.coherence.options.ClusterName;
import com.oracle.bedrock.runtime.coherence.options.ClusterPort;
import com.oracle.bedrock.runtime.coherence.options.LocalHost;
import com.oracle.bedrock.runtime.coherence.options.LocalStorage;
import com.oracle.bedrock.runtime.coherence.options.OperationalOverride;
import com.oracle.bedrock.runtime.coherence.options.WellKnownAddress;

import com.oracle.bedrock.runtime.concurrent.RemoteCallable;

import com.oracle.bedrock.runtime.java.features.JmxFeature;

import com.oracle.bedrock.runtime.java.options.IPv4Preferred;
import com.oracle.bedrock.runtime.java.options.SystemProperty;

import com.oracle.bedrock.runtime.java.profiles.JmxProfile;

import com.oracle.bedrock.runtime.network.AvailablePortIterator;

import com.oracle.bedrock.runtime.options.DisplayName;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.bedrock.testsupport.junit.TestLogs;

import com.oracle.bedrock.util.Capture;

import com.oracle.coherence.common.base.Classes;

import com.oracle.coherence.common.net.SSLSocketProvider;

import com.tangosol.coherence.component.util.SafeCluster;

import com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.HttpAcceptor;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.MemcachedAcceptor;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.TcpAcceptor;

import com.tangosol.coherence.component.util.safeService.SafeProxyService;

import com.tangosol.internal.net.metrics.MetricsHttpHelper;

import com.tangosol.net.Cluster;
import com.tangosol.net.Coherence;
import com.tangosol.net.NameService;
import com.tangosol.net.NamedCache;
import com.tangosol.net.ProxyService;
import com.tangosol.net.Session;
import com.tangosol.net.SocketProviderFactory;

import com.tangosol.net.messaging.ConnectionAcceptor;

import com.tangosol.util.Resources;
import com.tangosol.util.UUID;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import javax.management.ObjectName;

import java.net.URL;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.tangosol.internal.net.metrics.MetricsHttpHelper.PROP_METRICS_ENABLED;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.containsString;

public class GlobalSocketProviderTests
    {
    @BeforeClass
    public static void setup()
        {
        System.setProperty(OperationalOverride.PROPERTY, "global-ssl-test-override.xml");
        System.setProperty(CacheConfig.PROPERTY, "global-ssl-client-cache-config.xml");
        System.setProperty(IPv4Preferred.JAVA_NET_PREFER_IPV4_STACK, "true");
        System.setProperty(SocketProviderFactory.PROP_GLOBAL_PROVIDER, "one");
        System.setProperty(LocalStorage.PROPERTY, "false");
        URL urlKeystore = Resources.findFileOrResource("server.jks", Classes.getContextClassLoader());
        System.setProperty("coherence.security.keystore", urlKeystore.toExternalForm());
        URL urlTruststore = Resources.findFileOrResource("trust.jks", Classes.getContextClassLoader());
        System.setProperty("coherence.security.truststore", urlTruststore.toExternalForm());

        System.setProperty("coherence.localhost", "127.0.0.1");
        System.setProperty("coherence.wka", "127.0.0.1");
        System.setProperty("coherence.role", "client");
        System.setProperty(MetricsHttpHelper.PROP_METRICS_ENABLED, "false");

        m_ports = LocalPlatform.get().getAvailablePorts();
        }

    @Before
    public void generateCLusterName()
        {
        m_sClusterName = "GlobalSocketProviderTests" + m_nClusterId.incrementAndGet();
        System.setProperty(ClusterName.PROPERTY, m_sClusterName);
        m_nClusterPort = m_ports.next();
        System.setProperty(ClusterPort.PROPERTY, String.valueOf(m_nClusterPort));
        }

    @Test
    public void shouldUseGlobalSocketProviderWithTMB() throws Exception
        {
        LocalPlatform platform = LocalPlatform.get();

        OptionsByType optionsByType = OptionsByType.of(
                OperationalOverride.of("global-ssl-test-override.xml"),
                SystemProperty.of(SocketProviderFactory.PROP_GLOBAL_PROVIDER, "one"),
                ClusterName.of(m_sClusterName),
                ClusterPort.of(m_nClusterPort),
                JMXManagementMode.ALL,
                JmxProfile.enabled(),
                WellKnownAddress.of("127.0.0.1"),
                LocalHost.only(),
                IPv4Preferred.yes(),
                m_logs,
                DisplayName.of("storage"));

        optionsByType.addAll(m_exOptionsByType);
        CoherenceClusterBuilder clusterBuilder = new CoherenceClusterBuilder()
                .include(3, CoherenceClusterMember.class, optionsByType.asArray());

        try (CoherenceCluster cluster = clusterBuilder.build(platform))
            {
            Eventually.assertDeferred(cluster::isReady, is(true));

            for (CoherenceClusterMember member : cluster)
                {
                assertThat(member.getClusterSize(), is(3));
                }

            CoherenceClusterMember member     = cluster.getAny();
            JmxFeature             jmxFeature = member.get(JmxFeature.class);
            int                    nId        = member.getLocalMemberId();
            ObjectName             objectName = new ObjectName("Coherence:type=Node,nodeId=" + nId);
            String                 sStatus    = jmxFeature.getMBeanAttribute(objectName, "TransportStatus", String.class);

            assertThat(sStatus, containsString("tmbs://"));
            assertThat(member.invoke(new IsSecureUDP()), is(true));
            // health should not be secure
            String sServiceName = "$SYS:HealthHttpProxy";
            Eventually.assertDeferred(() -> member.isServiceRunning(sServiceName), is(true));
            assertThat(member.invoke(new IsSecureProxy(sServiceName)), is(false));
            }
        }

    @Test
    public void shouldUseGlobalSocketProviderWithDatagram()
        {
        LocalPlatform platform = LocalPlatform.get();

        OptionsByType optionsByType = OptionsByType.of(
                OperationalOverride.of("global-ssl-test-override.xml"),
                SystemProperty.of(SocketProviderFactory.PROP_GLOBAL_PROVIDER, "one"),
                ClusterName.of(m_sClusterName),
                ClusterPort.of(m_nClusterPort),
                SystemProperty.of("coherence.transport.reliable", "datagram"),
                JMXManagementMode.ALL,
                JmxProfile.enabled(),
                WellKnownAddress.of("127.0.0.1"),
                LocalHost.only(),
                IPv4Preferred.yes(),
                m_logs,
                DisplayName.of("storage"));

        optionsByType.addAll(m_exOptionsByType);
        CoherenceClusterBuilder clusterBuilder = new CoherenceClusterBuilder()
                .include(3, CoherenceClusterMember.class, optionsByType.asArray());

        try (CoherenceCluster cluster = clusterBuilder.build(platform))
            {
            Eventually.assertDeferred(cluster::isReady, is(true));

            for (CoherenceClusterMember member : cluster)
                {
                assertThat(member.getClusterSize(), is(3));
                }

            CoherenceClusterMember member  = cluster.getAny();

            assertThat(member.invoke(new IsSecureUDP()), is(true));
            // health should not be secure
            String sServiceName = "$SYS:HealthHttpProxy";
            Eventually.assertDeferred(() -> member.isServiceRunning(sServiceName), is(true));
            assertThat(member.invoke(new IsSecureProxy(sServiceName)), is(false));
            }
        }

    @Test
    public void shouldUseGlobalSocketProviderWithExtendProxy() throws Exception
        {
        LocalPlatform    platform   = LocalPlatform.get();
        Capture<Integer> extendPort = new Capture<>(platform.getAvailablePorts());

        CoherenceClusterBuilder clusterBuilder = new CoherenceClusterBuilder()
                .include(3, CoherenceClusterMember.class,
                         OperationalOverride.of("global-ssl-test-override.xml"),
                         SystemProperty.of(SocketProviderFactory.PROP_GLOBAL_PROVIDER, "one"),
                         SystemProperty.of("coherence.extend.port", extendPort),
                         ClusterName.of(m_sClusterName),
                         ClusterPort.of(m_nClusterPort),
                         JMXManagementMode.ALL,
                         JmxProfile.enabled(),
                         WellKnownAddress.of("127.0.0.1"),
                         LocalHost.only(),
                         IPv4Preferred.yes(),
                         m_logs,
                         DisplayName.of("storage"));

        try (CoherenceCluster cluster = clusterBuilder.build(platform))
            {
            Eventually.assertDeferred(cluster::isReady, is(true));

            for (CoherenceClusterMember member : cluster)
                {
                Eventually.assertDeferred(() -> member.isServiceRunning("Proxy"), is(true));
                assertThat(member.getClusterSize(), is(3));
                assertThat(member.invoke(new IsSecureProxy()), is(true));
                }

            System.setProperty(PROP_METRICS_ENABLED, "false");
            System.setProperty("coherence.extend.address", "127.0.0.1");
            System.setProperty("coherence.extend.port", String.valueOf(extendPort.get()));

            Coherence coherence = Coherence.client().start().get(5, TimeUnit.MINUTES);
            Session session = coherence.getSession();
            try (NamedCache<String, String> cache = session.getCache("nameservice-test"))
                {
                String value = new UUID().toString();
                cache.put("key-1", value);
                assertThat(cache.get("key-1"), is(value));
                }

            try (NamedCache<String, String> cache = session.getCache("fixed-test"))
                {
                String value = new UUID().toString();
                cache.put("key-1", value);
                assertThat(cache.get("key-1"), is(value));
                }
            }
        }

    @Test
    public void shouldUseGlobalSocketProviderWithMetrics()
        {
        LocalPlatform    platform    = LocalPlatform.get();
        Capture<Integer> metricsPort = new Capture<>(platform.getAvailablePorts());

        OptionsByType optionsByType = OptionsByType.of(
                OperationalOverride.of("global-ssl-test-override.xml"),
                SystemProperty.of(SocketProviderFactory.PROP_GLOBAL_PROVIDER, "one"),
                SystemProperty.of(MetricsHttpHelper.PROP_METRICS_ENABLED, true),
                SystemProperty.of("coherence.metrics.http.port", metricsPort),
                ClusterName.of(m_sClusterName),
                ClusterPort.of(m_nClusterPort),
                JMXManagementMode.ALL,
                JmxProfile.enabled(),
                WellKnownAddress.of("127.0.0.1"),
                LocalHost.only(),
                IPv4Preferred.yes(),
                m_logs,
                DisplayName.of("storage"));

        optionsByType.addAll(m_exOptionsByType);
        CoherenceClusterBuilder clusterBuilder = new CoherenceClusterBuilder()
                .include(1, CoherenceClusterMember.class, optionsByType.asArray());

        try (CoherenceCluster cluster = clusterBuilder.build(platform))
            {
            Eventually.assertDeferred(cluster::isReady, is(true));
            CoherenceClusterMember member       = cluster.getAny();
            String                 sServiceName = MetricsHttpHelper.getServiceName();
            Eventually.assertDeferred(() -> member.isServiceRunning(sServiceName), is(true));
            assertThat(member.invoke(new IsSecureProxy(sServiceName)), is(true));
            }
        }

    // ----- inner class: IsSecureUDP ---------------------------------------

    public static class IsSecureUDP
            implements RemoteCallable<Boolean>
        {
        @Override
        public Boolean call()
            {
            Cluster cluster = Coherence.getInstance().getCluster();
            if (cluster instanceof SafeCluster)
                {
                cluster = ((SafeCluster) cluster).getCluster();
                }
            return ((com.tangosol.coherence.component.net.Cluster) cluster).getPointListener()
                    .getUdpSocket()
                    .getDatagramSocketProvider()
                    .isSecure();
            }
        }

    // ----- inner class: IsSecureProxy -------------------------------------

    public static class IsSecureProxy
            implements RemoteCallable<Boolean>
        {
        public IsSecureProxy()
            {
            this("Proxy");
            }

        public IsSecureProxy(String sService)
            {
            f_sService = sService;
            }

        @Override
        public Boolean call()
            {
            Cluster cluster = Coherence.getInstance().getCluster();
            if (cluster instanceof SafeCluster)
                {
                cluster = ((SafeCluster) cluster).getCluster();
                }
            ProxyService proxy = (ProxyService) cluster.getService(f_sService);
            if (proxy instanceof SafeProxyService)
                {
                proxy = (ProxyService) ((SafeProxyService) proxy).getRunningService();
                }
            if (proxy != null)
                {
                ConnectionAcceptor acceptor = ((com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ProxyService) proxy).getAcceptor();
                if (acceptor instanceof TcpAcceptor)
                    {
                    return ((TcpAcceptor) acceptor).getSocketProvider() instanceof SSLSocketProvider;
                    }
                if (acceptor instanceof HttpAcceptor)
                    {
                    return ((HttpAcceptor) acceptor).getSocketProvider() instanceof SSLSocketProvider;
                    }
                if (acceptor instanceof MemcachedAcceptor)
                    {
                    return ((MemcachedAcceptor) acceptor).getSocketProvider() instanceof SSLSocketProvider;
                    }
                }
            return false;
            }

        // ----- data members ---------------------------------------------------

        private final String f_sService;
        }

    // ----- inner class: IsSecureNameService -------------------------------

    public static class IsSecureNameService
            implements RemoteCallable<Boolean>
        {
        @Override
        public Boolean call()
            {
            Cluster cluster = Coherence.getInstance().getCluster();
            if (cluster instanceof SafeCluster)
                {
                cluster = ((SafeCluster) cluster).getCluster();
                }
            NameService ns = cluster.getResourceRegistry().getResource(NameService.class);
            com.tangosol.coherence.component.net.Cluster.NameService nameService = (com.tangosol.coherence.component.net.Cluster.NameService) ns;
            TcpAcceptor acceptor = nameService.getAcceptor();
            return acceptor.getSocketProvider() instanceof SSLSocketProvider;
            }
        }

    // ----- data members ---------------------------------------------------

    @Rule
    public final TestLogs m_logs = new TestLogs(GlobalSocketProviderTests.class);

    /**
     * An {@link AtomicInteger} used to generate a cluster name.
     */
    protected final AtomicInteger m_nClusterId = new AtomicInteger(0);

    /**
     * The cluster name for a test.
     */
    protected String m_sClusterName;

    protected static AvailablePortIterator m_ports;

    protected int m_nClusterPort;

    protected static OptionsByType m_exOptionsByType = OptionsByType.empty();
    }
