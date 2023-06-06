/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
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
import com.oracle.bedrock.runtime.coherence.options.WellKnownAddress;
import com.oracle.bedrock.runtime.java.features.JmxFeature;
import com.oracle.bedrock.runtime.java.options.IPv4Preferred;
import com.oracle.bedrock.runtime.java.options.SystemProperty;
import com.oracle.bedrock.runtime.java.profiles.JmxProfile;
import com.oracle.bedrock.runtime.options.DisplayName;
import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.util.Capture;

import com.tangosol.internal.net.metrics.MetricsHttpHelper;
import com.tangosol.net.Coherence;
import com.tangosol.net.NamedCache;
import com.tangosol.net.Session;
import com.tangosol.util.UUID;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.management.ObjectName;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class SecuredProductionTests extends GlobalSocketProviderTests
    {
    @BeforeClass
    public static void setup()
        {
        System.setProperty(CacheConfig.PROPERTY, "global-ssl-client-cache-config.xml");
        System.setProperty(IPv4Preferred.JAVA_NET_PREFER_IPV4_STACK, "true");
        System.setProperty("coherence.security.keystore", "server.jks");
        System.setProperty("coherence.security.truststore", "trust.jks");
        System.setProperty("coherence.security.password", "password");
        System.setProperty("coherence.security.truststore.password", "password");
        System.setProperty("coherence.security.key.password", "private");
        System.setProperty("coherence.mode", "prod");
        System.setProperty("coherence.secured.production", "true");
        System.setProperty(LocalStorage.PROPERTY, "false");

        System.setProperty("coherence.localhost", "127.0.0.1");
        System.setProperty("coherence.wka", "127.0.0.1");
        System.setProperty("coherence.role", "client");
        System.setProperty(MetricsHttpHelper.PROP_METRICS_ENABLED, "true");

        m_exOptionsByType.addAll(
                SystemProperty.of("coherence.security.keystore", "server.jks"),
                SystemProperty.of("coherence.security.truststore", "trust.jks"),
                SystemProperty.of("coherence.security.password", "password"),
                SystemProperty.of("coherence.security.truststore.password", "password"),
                SystemProperty.of("coherence.security.key.password", "private"),
                SystemProperty.of("coherence.mode", "prod"),
                SystemProperty.of("coherence.secured.production", "true"));

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
    @Override
    public void shouldUseGlobalSocketProviderWithExtendProxy() throws Exception
        {
        LocalPlatform    platform   = LocalPlatform.get();
        Capture<Integer> extendPort = new Capture<>(platform.getAvailablePorts());

        OptionsByType optionsByType = OptionsByType.of(
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

        optionsByType.addAll(m_exOptionsByType);
        CoherenceClusterBuilder clusterBuilder = new CoherenceClusterBuilder()
                .include(3, CoherenceClusterMember.class, optionsByType.asArray());

        try (CoherenceCluster cluster = clusterBuilder.build(platform))
            {
            Eventually.assertDeferred(cluster::isReady, is(true));

            for (CoherenceClusterMember member : cluster)
                {
                assertThat(member.getClusterSize(), is(3));
                assertThat(member.invoke(new IsSecureProxy()), is(true));
                }

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
    @Override
    public void shouldUseGlobalSocketProviderWithSecuredProduction()
        {
        // noop
        }

    @Test
    public void canDisableSecuredProduction() throws Exception
        {
        LocalPlatform platform = LocalPlatform.get();

        CoherenceClusterBuilder clusterBuilder = new CoherenceClusterBuilder()
                .include(3, CoherenceClusterMember.class,
                        ClusterName.of(m_sClusterName),
                        ClusterPort.of(m_nClusterPort),
                        JMXManagementMode.ALL,
                        JmxProfile.enabled(),
                        WellKnownAddress.of("127.0.0.1"),
                        LocalHost.only(),
                        IPv4Preferred.yes(),
                        m_logs,
                        DisplayName.of("storage"),
                        SystemProperty.of("coherence.mode", "prod"),
                        SystemProperty.of("coherence.secured.production", "false"));

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

            assertThat(sStatus, containsString("tmb://"));
            assertThat(member.invoke(new IsSecureUDP()), is(false));
            }
        }

    @Test
    public void shouldDisableSecuredProductionInDev() throws Exception
        {
        LocalPlatform platform = LocalPlatform.get();

        CoherenceClusterBuilder clusterBuilder = new CoherenceClusterBuilder()
                .include(3, CoherenceClusterMember.class,
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
            Eventually.assertDeferred(cluster::isReady, is(true), Eventually.within(5, TimeUnit.MINUTES));

            for (CoherenceClusterMember member : cluster)
                {
                assertThat(member.getClusterSize(), is(3));
                }

            CoherenceClusterMember member     = cluster.getAny();
            JmxFeature             jmxFeature = member.get(JmxFeature.class);
            int                    nId        = member.getLocalMemberId();
            ObjectName             objectName = new ObjectName("Coherence:type=Node,nodeId=" + nId);
            String                 sStatus    = jmxFeature.getMBeanAttribute(objectName, "TransportStatus", String.class);

            assertThat(sStatus, containsString("tmb://"));
            assertThat(member.invoke(new IsSecureUDP()), is(false));
            }
        }

    @Test
    public void noSecuredProductionInDev() throws Exception
        {
        LocalPlatform platform      = LocalPlatform.get();
        String        coherenceMode = System.getProperty("coherence.mode");

        // This is not a valid test for production mode tests
        if (!coherenceMode.isEmpty() &&
             coherenceMode.equalsIgnoreCase("prod"))
            {
            return;
            }

        CoherenceClusterBuilder clusterBuilder = new CoherenceClusterBuilder()
                .include(3, CoherenceClusterMember.class,
                        ClusterName.of(m_sClusterName),
                        ClusterPort.of(m_nClusterPort),
                        JMXManagementMode.ALL,
                        JmxProfile.enabled(),
                        WellKnownAddress.of("127.0.0.1"),
                        LocalHost.only(),
                        IPv4Preferred.yes(),
                        m_logs,
                        DisplayName.of("storage"),
                        SystemProperty.of("coherence.secured.production", "true"));

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

            assertThat(sStatus, containsString("tmb://"));
            assertThat(member.invoke(new IsSecureUDP()), is(false));
            }
        }
    }
