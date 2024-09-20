/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package grpc.client;

import com.oracle.bedrock.junit.CoherenceClusterExtension;
import com.oracle.bedrock.junit.SessionBuilders;
import com.oracle.bedrock.runtime.LocalPlatform;
import com.oracle.bedrock.runtime.coherence.CoherenceCluster;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.coherence.options.CacheConfig;
import com.oracle.bedrock.runtime.coherence.options.ClusterName;
import com.oracle.bedrock.runtime.coherence.options.LocalHost;
import com.oracle.bedrock.runtime.coherence.options.OperationalOverride;
import com.oracle.bedrock.runtime.coherence.options.Pof;
import com.oracle.bedrock.runtime.coherence.options.RoleName;
import com.oracle.bedrock.runtime.coherence.options.WellKnownAddress;
import com.oracle.bedrock.runtime.java.options.IPv4Preferred;
import com.oracle.bedrock.runtime.java.options.SystemProperty;
import com.oracle.bedrock.runtime.network.AvailablePortIterator;
import com.oracle.bedrock.runtime.options.DisplayName;
import com.oracle.bedrock.runtime.options.Ports;
import com.oracle.bedrock.runtime.options.StabilityPredicate;
import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.testsupport.junit.TestLogsExtension;
import com.tangosol.io.Serializer;
import com.tangosol.net.Coherence;
import com.tangosol.net.CoherenceConfiguration;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.net.Session;
import com.tangosol.net.SessionConfiguration;
import com.tangosol.net.grpc.GrpcDependencies;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@SuppressWarnings("resource")
public class DefaultCacheConfigNameServiceGrpcIT
        extends AbstractGrpcClientIT
    {
    @BeforeAll
    @SuppressWarnings("DuplicatedCode")
    static void setupCluster() throws Exception
        {
        CoherenceCluster cluster = CLUSTER_EXTENSION.getCluster();

        for (CoherenceClusterMember member : cluster)
            {
            Eventually.assertDeferred(() -> member.invoke(IsGrpcProxyRunning.INSTANCE), is(true));
            }

        CoherenceClusterMember member      = cluster.getAny();
        int                    nExtendPort = member.getExtendProxyPort();

        System.setProperty("coherence.pof.config", "test-pof-config.xml");
        System.setProperty("coherence.cluster", CLUSTER_NAME);

        CoherenceConfiguration.Builder cfgBuilder = CoherenceConfiguration.builder();

        Set<String> setSessionName = new HashSet<>();
        Set<String> setSerializer  = serializers().map(a -> String.valueOf(a.get()[0]))
                .collect(Collectors.toSet());

        for (String sSerializer : setSerializer)
            {
            String sName = sessionNameFromSerializerName(sSerializer);
            SessionConfiguration cfg = SessionConfiguration.builder()
                    .named(sName)
                    .withScopeName(sName)
                    .withMode(Coherence.Mode.Grpc)
                    .withParameter("coherence.serializer", sSerializer)
                    .withParameter("coherence.profile", "thin")
//                    .withParameter("coherence.grpc.remote.proxy", )
                    .withParameter("coherence.grpc.address", "127.0.0.1")
                    .withParameter("coherence.grpc.port", "7574")
                    .build();

            setSessionName.add(sName);
            cfgBuilder.withSession(cfg);
            }

        Coherence coherence = Coherence.client(cfgBuilder.build()).start().get(5, TimeUnit.MINUTES);

        for (String sName : setSessionName)
            {
            Session session = coherence.getSession(sName);
            SESSIONS.put(sName, session);
            }

        s_ccfExtend = CLUSTER_EXTENSION.createSession(SessionBuilders.extendClient("client-cache-config.xml",
                SystemProperty.of("coherence.extend.port", nExtendPort)));
        }

    @AfterAll
    static void shutdownCoherence()
        {
        Coherence.closeAll();
        }

    @BeforeEach
    public void logStart(TestInfo info)
        {
        String sClass  = info.getTestClass().map(Class::toString).orElse("");
        String sMethod = info.getTestMethod().map(Method::toString).orElse("");
        String sMsg = ">>>>>>> Starting test " + sClass + "." + sMethod + " - " + info.getDisplayName();
        for (CoherenceClusterMember member : CLUSTER_EXTENSION.getCluster())
            {
            member.submit(() ->
                {
                System.err.println(sMsg);
                System.err.flush();
                return null;
                }).join();
            }
        }

    @AfterEach
    public void logEnd(TestInfo info)
        {
        String sClass  = info.getTestClass().map(Class::toString).orElse("");
        String sMethod = info.getTestMethod().map(Method::toString).orElse("");
        String sMsg = ">>>>>>> Finished test " + sClass + "." + sMethod + " - " + info.getDisplayName();
        for (CoherenceClusterMember member : CLUSTER_EXTENSION.getCluster())
            {
            member.submit(() ->
                {
                System.err.println(sMsg);
                System.err.flush();
                return null;
                }).join();
            }
        }

    @Override
    protected <K, V> NamedCache<K, V> createClient(String sCacheName, String sSerializerName, Serializer serializer)
        {
        String sName = sessionNameFromSerializerName(sSerializerName);
        Session session = SESSIONS.get(sName);
        assertThat(session, is(notNullValue()));
        return session.getCache(sCacheName);
        }

    protected static String sessionNameFromSerializerName(String sSerializerName)
        {
        return sSerializerName.isEmpty() ? "default" : sSerializerName;
        }

    @Override
    protected <K, V> NamedCache<K, V> ensureCache(String sName, ClassLoader loader)
        {
        return s_ccfExtend.ensureCache(sName, loader);
        }

    // ----- data members ---------------------------------------------------

    static ConfigurableCacheFactory s_ccfExtend;

    static final Map<String, Session> SESSIONS = new HashMap<>();

    static final String CLUSTER_NAME = "DefaultCacheConfigGrpcIT";

    static final LocalPlatform PLATFORM = LocalPlatform.get();

    static final AvailablePortIterator PORTS = PLATFORM.getAvailablePorts();

    static final int CLUSTER_SIZE = 3;

    @RegisterExtension
    static final TestLogsExtension TEST_LOGS = new TestLogsExtension(DefaultCacheConfigNameServiceGrpcIT.class);

    @RegisterExtension
    static final CoherenceClusterExtension CLUSTER_EXTENSION = new CoherenceClusterExtension()
            .with(CacheConfig.of("coherence-config.xml"),
                  OperationalOverride.of("test-coherence-override.xml"),
                  Pof.config("test-pof-config.xml"),
                  SystemProperty.of("coherence.serializer", "pof"),
                  SystemProperty.of("coherence.extend.port", PORTS, Ports.capture()),
                  WellKnownAddress.loopback(),
                  ClusterName.of(CLUSTER_NAME),
                  DisplayName.of("storage"),
                  RoleName.of("storage"),
                  LocalHost.only(),
                  IPv4Preferred.yes(),
                  StabilityPredicate.of(CoherenceCluster.Predicates.isCoherenceRunning()),
                  TEST_LOGS)
            .include(CLUSTER_SIZE, CoherenceClusterMember.class);
    }
