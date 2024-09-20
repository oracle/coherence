/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package grpc.client;

import com.oracle.bedrock.junit.CoherenceClusterExtension;
import com.oracle.bedrock.junit.SessionBuilders;
import com.oracle.bedrock.runtime.coherence.CoherenceCluster;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.coherence.options.CacheConfig;
import com.oracle.bedrock.runtime.coherence.options.ClusterName;
import com.oracle.bedrock.runtime.coherence.options.LocalHost;
import com.oracle.bedrock.runtime.coherence.options.OperationalOverride;
import com.oracle.bedrock.runtime.coherence.options.Pof;
import com.oracle.bedrock.runtime.coherence.options.RoleName;
import com.oracle.bedrock.runtime.coherence.options.WellKnownAddress;
import com.oracle.bedrock.runtime.java.options.ClassName;
import com.oracle.bedrock.runtime.java.options.IPv4Preferred;
import com.oracle.bedrock.runtime.java.options.SystemProperty;
import com.oracle.bedrock.runtime.options.DisplayName;
import com.oracle.bedrock.runtime.options.StabilityPredicate;
import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.testsupport.junit.TestLogsExtension;
import com.tangosol.net.Coherence;
import com.tangosol.net.CoherenceConfiguration;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.net.Session;
import com.tangosol.net.SessionConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@SuppressWarnings("resource")
public class DefaultScopedCacheConfigGrpcIT
    {
    @BeforeAll
    static void setupCluster() throws Exception
        {
        CoherenceCluster cluster = CLUSTER_EXTENSION.getCluster();

        for (CoherenceClusterMember member : cluster)
            {
            Eventually.assertDeferred(() -> member.invoke(IsGrpcProxyRunning.INSTANCE), is(true));
            }

        System.setProperty("coherence.cluster", CLUSTER_NAME);
        System.setProperty("coherence.serializer", "pof");
        System.setProperty("coherence.pof.config", "test-pof-config.xml");
        System.setProperty("coherence.client", Coherence.Mode.Grpc.getClient());
        System.setProperty("coherence.grpc.address", "127.0.0.1");
        System.setProperty("coherence.grpc.port", "7574");

        CoherenceConfiguration.Builder cfgBuilder = CoherenceConfiguration.builder();

        Set<String> setSessionName = new HashSet<>();

        for (String sName : MultiScopeServer.SCOPE_NAMES)
            {
            SessionConfiguration cfg = SessionConfiguration.builder()
                    .named(sName)
                    .withScopeName("grpc-" + sName)
                    .withMode(Coherence.Mode.Grpc)
                    .withParameter("coherence.grpc.remote.scope", sName)
                    .withParameter("coherence.serializer", "java")
                    .withParameter("coherence.profile", "thin")
                    .build();

            setSessionName.add(sName);
            cfgBuilder.withSession(cfg);

            SessionConfiguration extendCfg = SessionConfiguration.builder()
                    .named("extend-" + sName)
                    .withConfigUri("client-cache-config.xml")
                    .withScopeName(sName)
                    .withMode(Coherence.Mode.Client)
                    .withParameter("coherence.client", "name-service")
                    .build();
            cfgBuilder.withSession(extendCfg);
            setSessionName.add("extend-" + sName);
            }

        s_coherence = Coherence.create(cfgBuilder.build(), Coherence.Mode.Grpc)
                .start().get(5, TimeUnit.MINUTES);

        for (String sName : setSessionName)
            {
            Session session = s_coherence.getSession(sName);
            if (sName.startsWith("extend"))
                {
                EXTEND_SESSIONS.put(sName, session);
                }
            else
                {
                SESSIONS.put(sName, session);
                }
            }
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

    @Test
    public void shouldUseScopes()
        {
        for (Map.Entry<String, Session> entry : SESSIONS.entrySet())
            {
            String                     sName       = entry.getKey();
            Session                    session     = entry.getValue();
            Session                    sessionExtend = EXTEND_SESSIONS.get("extend-" + sName);
            NamedCache<String, String> cacheExtend = sessionExtend.getCache("test");
            NamedCache<String, String> cache       = session.getCache("test");
            String                     sKey        = "key-" + sName;
            String                     sValue      = "value-" + Math.random();

            assertThat(cacheExtend.get(sKey), is(nullValue()));
            cache.put(sKey, sValue);
            assertThat(cache.get(sKey), is(sValue));
            assertThat(cacheExtend.get(sKey), is(sValue));
            }
        }

    @Test
    public void shouldUseSystemSession()
        {
        Session                  session = s_coherence.getSession(Coherence.SYSTEM_SESSION);
        ConfigurableCacheFactory ccf     = CLUSTER_EXTENSION.createSession(SessionBuilders.extendClient("scoped://$SYS?" + Coherence.SYS_CCF_URI,
                        SystemProperty.of("coherence.scope", Coherence.SYSTEM_SCOPE),
                        SystemProperty.of("coherence.client", "remote"),
                        SystemProperty.of("coherence.system.cluster.address", "127.0.0.1"),
                        SystemProperty.of("coherence.system.cluster.port", 7574)));



        NamedCache<String, String> cacheExtend = ccf.ensureCache("sys$config-test", null);
        NamedCache<String, String> cache       = session.getCache("sys$config-test");
        String                     sKey        = "key-" + Math.random();
        String                     sValue      = "value-" + Math.random();

        assertThat(cacheExtend.get(sKey), is(nullValue()));
        cache.put(sKey, sValue);
        assertThat(cache.get(sKey), is(sValue));
        assertThat(cacheExtend.get(sKey), is(sValue));
        }

    // ----- data members ---------------------------------------------------

    static Coherence s_coherence;

    static final Map<String, Session> SESSIONS = new HashMap<>();

    static final Map<String, Session> EXTEND_SESSIONS = new HashMap<>();

    static final String CLUSTER_NAME = "DefaultScopedCacheConfigGrpcIT";

    static final int CLUSTER_SIZE = 3;

    @RegisterExtension
    static final TestLogsExtension TEST_LOGS = new TestLogsExtension(DefaultScopedCacheConfigGrpcIT.class);

    @RegisterExtension
    static final CoherenceClusterExtension CLUSTER_EXTENSION = new CoherenceClusterExtension()
            .with(ClassName.of(MultiScopeServer.class),
                  CacheConfig.of("coherence-config.xml"),
                  OperationalOverride.of("test-coherence-override.xml"),
                  Pof.config("test-pof-config.xml"),
                  SystemProperty.of("coherence.serializer", "pof"),
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
