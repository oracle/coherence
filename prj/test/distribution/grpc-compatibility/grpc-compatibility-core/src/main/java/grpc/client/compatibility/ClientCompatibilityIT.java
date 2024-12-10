/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package grpc.client.compatibility;


import com.oracle.bedrock.Option;

import com.oracle.bedrock.junit.CoherenceClusterExtension;
import com.oracle.bedrock.junit.SessionBuilders;

import com.oracle.bedrock.maven.Maven;

import com.oracle.bedrock.runtime.LocalPlatform;

import com.oracle.bedrock.runtime.coherence.CoherenceCluster;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;

import com.oracle.bedrock.runtime.coherence.options.CacheConfig;
import com.oracle.bedrock.runtime.coherence.options.ClusterName;
import com.oracle.bedrock.runtime.coherence.options.LocalHost;
import com.oracle.bedrock.runtime.coherence.options.Logging;
import com.oracle.bedrock.runtime.coherence.options.OperationalOverride;
import com.oracle.bedrock.runtime.coherence.options.Pof;
import com.oracle.bedrock.runtime.coherence.options.RoleName;
import com.oracle.bedrock.runtime.coherence.options.WellKnownAddress;

import com.oracle.bedrock.runtime.java.ClassPath;

import com.oracle.bedrock.runtime.java.options.IPv4Preferred;
import com.oracle.bedrock.runtime.java.options.SystemProperty;

import com.oracle.bedrock.runtime.options.DisplayName;
import com.oracle.bedrock.runtime.options.Ports;
import com.oracle.bedrock.runtime.options.StabilityPredicate;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.bedrock.testsupport.junit.TestLogsExtension;

import com.oracle.coherence.common.base.Exceptions;
import com.oracle.coherence.common.util.Threads;
import com.oracle.coherence.testing.Junit5CheckJDK;
import com.tangosol.coherence.config.Config;

import com.tangosol.io.Serializer;

import com.tangosol.net.Coherence;
import com.tangosol.net.CoherenceConfiguration;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.net.Session;
import com.tangosol.net.SessionConfiguration;

import grpc.client.AbstractGrpcClientIT;
import grpc.client.IsGrpcProxyRunning;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import org.junit.jupiter.api.extension.RegisterExtension;

import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;


@SuppressWarnings("resource")
public class ClientCompatibilityIT
        extends AbstractGrpcClientIT
    {
    @BeforeAll
    static void setupCluster(TestInfo info) throws Exception
        {
        int    cTimeoutMinute  = 35;
        String sMinJavaVersion = Config.getProperty("coherence.compatability.minJavaVersion");
        String sMaxJavaVersion = Config.getProperty("coherence.compatability.maxJavaVersion");

        if (sMinJavaVersion != null && !sMinJavaVersion.isBlank())
            {
            Junit5CheckJDK.assumeJDKVersionGreaterThanEqual(sMinJavaVersion);
            }

        if (sMaxJavaVersion != null && !sMaxJavaVersion.isBlank())
            {
            Junit5CheckJDK.assumeJDKVersionLessThanOrEqual(sMaxJavaVersion);
            }

        System.setProperty("coherence.grpc.heartbeat.interval", "1000");
        System.setProperty("coherence.grpc.heartbeat.ack", "true");

        executor.schedule(() ->
            {
            System.err.println("***** Exiting due to probable hanging test *****");
            System.err.println(Threads.getThreadDump(true));
            System.exit(1);
            return null;
            }, cTimeoutMinute + 5, TimeUnit.MINUTES);

        sClusterExtension = new CoherenceClusterExtension();

        CompletableFuture<CoherenceClusterExtension> future = CompletableFuture.supplyAsync(() ->
            {
            try
                {
                sClusterExtension.with(CacheConfig.of("coherence-config.xml"),
                                OperationalOverride.of("test-coherence-override.xml"),
                                createClasspath(),
                                Pof.config("test-pof-config.xml"),
                                SystemProperty.of("coherence.serializer", "pof"),
                                SystemProperty.of("coherence.extend.port", PORTS, Ports.capture()),
                                WellKnownAddress.loopback(),
                                ClusterName.of(CLUSTER_NAME),
                                DisplayName.of("storage"),
                                RoleName.of("storage"),
                                Logging.atMax(),
                                LocalHost.only(),
                                IPv4Preferred.yes(),
                                StabilityPredicate.none(),
                                TEST_LOGS)
                        .include(CLUSTER_SIZE, CoherenceClusterMember.class);

                sClusterExtension.beforeAll(null);
                return sClusterExtension;
                }
            catch (Throwable t)
                {
                t.printStackTrace(System.err);
                throw Exceptions.ensureRuntimeException(t);
                }
            });

        try
            {
            future.get(cTimeoutMinute, TimeUnit.MINUTES);
            }
        catch (Throwable throwable)
            {
            if (throwable instanceof TimeoutException)
                {
                throwable.printStackTrace(System.out);
                System.err.println("Test timed out: " + info.getDisplayName());
                System.err.println(Threads.getThreadDump(true));
                }
            throw Exceptions.ensureRuntimeException(throwable);
            }

        CoherenceCluster cluster = sClusterExtension.getCluster();

        for (CoherenceClusterMember member : cluster)
            {
            Eventually.assertDeferred(() -> member.invoke(IsGrpcProxyRunning.INSTANCE), is(true));
            }

        CoherenceClusterMember member      = cluster.getAny();
        int                    nGrpcPort   = member.getGrpcProxyPort();
        int                    nExtendPort = member.getExtendProxyPort();

        System.setProperty("coherence.pof.config", "test-pof-config.xml");

        CoherenceConfiguration.Builder cfgBuilder = CoherenceConfiguration.builder();

        Set<String> setSessionName = new HashSet<>();
        Set<String> setSerializer = serializers().map(a -> String.valueOf(a.get()[0]))
                .collect(Collectors.toSet());

        for (String sSerializer : setSerializer)
            {
            String sName = sessionNameFromSerializerName(sSerializer);
            SessionConfiguration cfg = SessionConfiguration.builder()
                    .named(sName)
                    .withScopeName(sName)
                    .withMode(Coherence.Mode.GrpcFixed)
                    .withParameter("coherence.serializer", sSerializer)
                    .withParameter("coherence.profile", "thin")
                    .withParameter("coherence.grpc.address", "127.0.0.1")
                    .withParameter("coherence.grpc.port", nGrpcPort)
                    .withParameter("coherence.extend.port", nExtendPort)
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

        s_ccfExtend = sClusterExtension.createSession(SessionBuilders.extendClient("client-cache-config.xml",
                SystemProperty.of("coherence.extend.port", nExtendPort)));
        }

    @AfterAll
    static void shutdownCoherence()
        {
        try
            {
            Coherence.closeAll();
            }
        catch (Throwable t)
            {
            t.printStackTrace(System.err);
            }

        if (sClusterExtension != null)
            {
            try
                {
                sClusterExtension.afterAll(null);
                }
            catch (Throwable t)
                {
                t.printStackTrace(System.err);
                }
            }
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    @Override
    public void shouldReplaceAllWithKeySet(String sSerializerName, Serializer serializer)
        {
        Assumptions.assumeTrue(!"prod".equals(COHERENCE_MODE));
        super.shouldReplaceAllWithKeySet(sSerializerName, serializer);
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    @Override
    public void shouldReplaceAllWithFilter(String sSerializerName, Serializer serializer)
        {
        Assumptions.assumeTrue(!"prod".equals(COHERENCE_MODE));
        super.shouldReplaceAllWithFilter(sSerializerName, serializer);
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    @Override
    public void shouldReturnTrueForContainsAssociatedKeyWithExistingMapping(String sSerializerName, Serializer serializer)
        {
        Assumptions.assumeTrue(!"prod".equals(COHERENCE_MODE));
        super.shouldReturnTrueForContainsAssociatedKeyWithExistingMapping(sSerializerName, serializer);
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    @Override
    public void shouldReturnFalseForContainsKeyWithNonExistentMapping(String sSerializerName, Serializer serializer)
        {
        Assumptions.assumeTrue(!"prod".equals(COHERENCE_MODE));
        super.shouldReturnTrueForContainsAssociatedKeyWithExistingMapping(sSerializerName, serializer);
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    @Override
    public void shouldComputeAndUpdateEntry(String sSerializerName, Serializer serializer)
        {
        Assumptions.assumeTrue(!"prod".equals(COHERENCE_MODE));
        super.shouldComputeAndUpdateEntry(sSerializerName, serializer);
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    @Override
    public void shouldComputeAndUpdateEntryWithAssociatedKey(String sSerializerName, Serializer serializer)
        {
        Assumptions.assumeTrue(!"prod".equals(COHERENCE_MODE));
        super.shouldComputeAndUpdateEntryWithAssociatedKey(sSerializerName, serializer);
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    @Override
    public void shouldReplaceAllWithAssociatedKeySet(String sSerializerName, Serializer serializer)
        {
        Assumptions.assumeTrue(!"prod".equals(COHERENCE_MODE));
        super.shouldReplaceAllWithAssociatedKeySet(sSerializerName, serializer);
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    @Override
    public void shouldReturnTrueForContainsKeyWithExistingMapping(String sSerializerName, Serializer serializer)
        {
        Assumptions.assumeTrue(!"prod".equals(COHERENCE_MODE));
        super.shouldReturnTrueForContainsAssociatedKeyWithExistingMapping(sSerializerName, serializer);
        }

    @Override
    protected <K, V> NamedCache<K, V> createClient(String sCacheName, String sSerializerName, Serializer serializer)
        {
        String  sName   = sessionNameFromSerializerName(sSerializerName);
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


    public static Option createClasspath()
        {
        try
            {
            String sSettings    = Config.getProperty("coherence.compatability.settings");
            File   fileSettings = null;
            System.out.println("Maven Settings: " + sSettings);
            if (sSettings != null && !sSettings.isBlank())
                {
                fileSettings = new File(sSettings);
                if (fileSettings.exists())
                    {
                    assertThat(fileSettings.isFile(), is(true));
                    }
                }

            String sVersion = Config.getProperty("coherence.compatability.version");
            assertThat("coherence.compatability.version property sis not set", sVersion, notNullValue());
            assertThat("coherence.compatability.version property sis not set", sVersion.isBlank(), is(false));

            String sGroup = Config.getProperty("coherence.compatability.groupId", "com.oracle.coherence");

            String sDeps = Config.getProperty("coherence.compatability.modules", "coherence-grpc-proxy,coherence-json");
            assertThat("coherence.compatability.modules property is not set", sDeps, notNullValue());
            assertThat("coherence.compatability.modules property is not set", sDeps.isBlank(), is(false));
            String[] asDeps = sDeps.split(",");

            String sCoherenceVersion;
            if (sVersion.charAt(0) == '[' || sVersion.charAt(0) == '(')
                {
                Maven maven = sSettings == null ? Maven.autoDetect() : Maven.settings(fileSettings);

                System.out.println("**** Looking up versions of : " + sGroup + ":" + asDeps[0] + ":" + sVersion);
                List<String> list = maven.versionsOf(sGroup, asDeps[0], sVersion);
                System.out.println("**** Found versions: " + list);

                boolean fIncludeSnapshots = Config.getBoolean("coherence.compatability.allow.snapshots", false);

                String[] asVersion = list.stream()
                        .filter(s -> fIncludeSnapshots || !s.contains("SNAPSHOT")) // skip snapshots if configured
                        .filter(s -> !s.endsWith("-Int")) // skip interim builds
                        .filter(s -> !s.endsWith("d")) // skip debug builds
                        .toArray(String[]::new);

                assertThat("Could not find any usable versions in range " + sVersion, asVersion.length, is(not(0)));
                sCoherenceVersion = asVersion[asVersion.length - 1];
                }
            else
                {
                sCoherenceVersion = sVersion;
                }

            assertThat(sCoherenceVersion, is(notNullValue()));
            assertThat(sCoherenceVersion.isBlank(), is(false));
            System.out.println("Running gRPC Compatability tests using Coherence version " + sCoherenceVersion);

            ClassPath cp = ClassPath.of(
                    ClassPath.ofClass(ClientCompatibilityIT.class),
                    ClassPath.ofClass(AbstractGrpcClientIT.class),
                    ClassPath.ofClass(CoherenceClusterExtension.class),
                    ClassPath.ofClass(Test.class),
                    ClassPath.ofClass(Arguments.class)
            );


            List<Maven> listMaven = new ArrayList<>();
            listMaven.add(Maven.include(cp));
            for (String s : asDeps)
                {
                listMaven.add(Maven.artifact(sGroup, s, sCoherenceVersion));
                }
            if (fileSettings != null)
                {
                listMaven.add(Maven.settings(fileSettings));
                }

            return Maven.from(listMaven.toArray(Maven[]::new));
            }
        catch (Exception e)
            {
            throw new RuntimeException(e);
            }
        }

    // ----- data members ---------------------------------------------------

    static ConfigurableCacheFactory s_ccfExtend;

    static final Map<String, Session> SESSIONS = new HashMap<>();

    static final String CLUSTER_NAME = "ClientCompatibilityIT";

    static final LocalPlatform PLATFORM = LocalPlatform.get();

    static final Iterator<Integer> PORTS = PLATFORM.getAvailablePorts();

    static final int CLUSTER_SIZE = 1;

    @RegisterExtension
    TestExecutionExceptionHandler timeoutExceptionHandler = (context, throwable) ->
        {
        if (throwable instanceof TimeoutException || throwable instanceof InterruptedException)
            {
            throwable.printStackTrace(System.err);
            System.err.println("Test timed out: " + context.getDisplayName());
            System.err.println(Threads.getThreadDump(true));
            }
        throw throwable;
        };

    @RegisterExtension
    static final TestLogsExtension TEST_LOGS = new TestLogsExtension(ClientCompatibilityIT.class);

    static CoherenceClusterExtension sClusterExtension;

    static final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    static final String COHERENCE_MODE = Config.getProperty("coherence.mode");
    }
