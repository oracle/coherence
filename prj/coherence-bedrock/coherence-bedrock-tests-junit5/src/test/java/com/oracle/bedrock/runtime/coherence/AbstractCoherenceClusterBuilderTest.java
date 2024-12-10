/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.bedrock.runtime.coherence;

import com.oracle.bedrock.OptionsByType;
import com.oracle.bedrock.options.Decoration;
import com.oracle.bedrock.runtime.ApplicationListener;
import com.oracle.bedrock.runtime.LocalPlatform;
import com.oracle.bedrock.runtime.Platform;
import com.oracle.bedrock.runtime.coherence.options.CacheConfig;
import com.oracle.bedrock.runtime.coherence.options.ClusterName;
import com.oracle.bedrock.runtime.coherence.options.ClusterPort;
import com.oracle.bedrock.runtime.coherence.options.LocalHost;
import com.oracle.bedrock.runtime.coherence.options.LocalStorage;
import com.oracle.bedrock.runtime.coherence.options.WellKnownAddress;
import com.oracle.bedrock.runtime.java.JavaApplicationLauncher;
import com.oracle.bedrock.runtime.java.options.IPv4Preferred;
import com.oracle.bedrock.runtime.java.options.SystemProperty;
import com.oracle.bedrock.runtime.network.AvailablePortIterator;
import com.oracle.bedrock.runtime.options.DisplayName;
import com.oracle.bedrock.runtime.options.StabilityPredicate;
import com.oracle.bedrock.testsupport.junit.AbstractTest;
import com.oracle.bedrock.testsupport.junit.TestLogsExtension;
import com.oracle.bedrock.util.Capture;
import com.oracle.bedrock.util.Trilean;
import com.tangosol.net.NamedCache;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.HashSet;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static com.oracle.bedrock.testsupport.deferred.Eventually.assertThat;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.greaterThan;

public abstract class AbstractCoherenceClusterBuilderTest
        extends AbstractTest
    {
    /**
     * Creates a new {@link JavaApplicationLauncher} to use for a tests in this class and/or sub-classes.
     *
     * @return the {@link JavaApplicationLauncher}
     */
    public abstract Platform getPlatform();


    /**
     * Ensure we can build and destroy a {@link CoherenceCluster} containing storage-enabled
     * {@link CoherenceCacheServer}s.
     */
    @Test
    public void shouldBuildStorageEnabledCluster()
        {
        final int CLUSTER_SIZE = 3;

        AvailablePortIterator availablePorts = LocalPlatform.get().getAvailablePorts();
        ClusterPort clusterPort = ClusterPort.of(new Capture<>(availablePorts));

        CoherenceClusterBuilder builder = new CoherenceClusterBuilder();

        builder.include(CLUSTER_SIZE,
                        CoherenceClusterMember.class,
                        clusterPort,
                        LocalHost.only(),
                        s_testLogs.builder(),
                        ClusterName.of("Storage-Only"));

        try (CoherenceCluster cluster = builder.build(getPlatform()))
            {
            assertThat(invoking(cluster).getClusterSize(), is(CLUSTER_SIZE));
            }
        }


    /**
     * Ensure we can build and close a {@link CoherenceCluster}
     * of storage enabled members with a proxy server.
     */
    @Test
    public void shouldBuildStorageAndProxyCluster()
        {
        AvailablePortIterator availablePorts = LocalPlatform.get().getAvailablePorts();
        ClusterPort clusterPort = ClusterPort.of(new Capture<>(availablePorts));

        CoherenceClusterBuilder builder = new CoherenceClusterBuilder();

        builder.include(2,
                        CoherenceClusterMember.class,
                        DisplayName.of("storage"),
                        clusterPort,
                        ClusterName.of("Storage-Proxy"),
                        CacheConfig.of("test-cache-config.xml"),
                        LocalHost.only(),
                        s_testLogs.builder(),
                        LocalStorage.enabled());

        builder.include(1,
                        CoherenceClusterMember.class,
                        DisplayName.of("extend"),
                        clusterPort,
                        ClusterName.of("Storage-Proxy"),
                        CacheConfig.of("test-extend-proxy-config.xml"),
                        LocalHost.only(),
                        s_testLogs.builder(),
                        LocalStorage.disabled(),
                        SystemProperty.of("coherence.extend.port", availablePorts));

        try (CoherenceCluster cluster = builder.build(getPlatform()))
            {
            // ensure the cluster size is as expected
            assertThat(invoking(cluster).getClusterSize(), is(3));

            // ensure the member id's are different
            HashSet<Integer> memberIds = new HashSet<>();

            for (CoherenceClusterMember member : cluster)
                {
                // ensure the member id is not -1 (it may not have a member id yet)
                assertThat(invoking(member).getLocalMemberId(), is(greaterThan(0)));

                int memberId = member.getLocalMemberId();

                System.out.println("Member ID: " + memberId);
                memberIds.add(memberId);
                }

            Assertions.assertEquals(3, memberIds.size());

            CoherenceClusterMember extendMember = cluster.get("extend-1");

            assertThat(invoking(extendMember).isServiceRunning("ExtendTcpProxyService"), is(true));

            for (CoherenceClusterMember storageMember : cluster.getAll("storage"))
                {
                assertThat(invoking(storageMember).isServiceRunning("ExtendTcpProxyService"), is(false));
                }
            }
        }


    /**
     * Ensure that we can build a cluster using WKA.
     */
    @Test
    //@Disabled
    public void shouldBuildWKABasedStorageCluster()
        {
        Assumptions.assumeFalse(Boolean.getBoolean("github.build"), "Skipping test in GitHub");

        ClusterPort clusterPort = ClusterPort.of(new Capture<>(LocalPlatform.get().getAvailablePorts()));
        String      localHost   = "127.0.0.1";

        String clusterName = "WKA" + getClass().getSimpleName();

        int desiredClusterSize = 4;

        CoherenceClusterBuilder clusterBuilder = new CoherenceClusterBuilder();

        clusterBuilder.include(desiredClusterSize,
                               CoherenceClusterMember.class,
                               DisplayName.of("storage"),
                               LocalStorage.enabled(),
                               WellKnownAddress.of(localHost),
                               ClusterName.of(clusterName),
                               IPv4Preferred.yes(),
                               LocalHost.only(),
                               s_testLogs.builder(),
                               clusterPort);

        try (CoherenceCluster cluster = clusterBuilder.build())
            {
            assertThat(invoking(cluster).getClusterSize(), is(desiredClusterSize));
            }
        }


    /**
     * Ensure we perform a rolling restart of a {@link CoherenceCluster}
     */
    @Test
    public void shouldPerformRollingRestartOfCluster()
        {
        final int CLUSTER_SIZE = 4;

        AvailablePortIterator availablePorts = LocalPlatform.get().getAvailablePorts();
        ClusterPort clusterPort = ClusterPort.of(new Capture<>(availablePorts));
        String clusterName = "Rolling" + getClass().getSimpleName();
        CoherenceClusterBuilder builder = new CoherenceClusterBuilder();

        builder.include(CLUSTER_SIZE,
                        CoherenceClusterMember.class,
                        DisplayName.of("DCS"),
                        clusterPort,
                        ClusterName.of(clusterName),
                        LocalHost.only(),
                        s_testLogs.builder());

        try (CoherenceCluster cluster = builder.build(getPlatform()))
            {
            assertThat(invoking(cluster).getClusterSize(), is(CLUSTER_SIZE));

            StabilityPredicate<CoherenceCluster> predicate =
                    StabilityPredicate.of(CoherenceCluster.Predicates.autoStartServicesSafe());

            cluster.unordered().relaunch(predicate);

            assertThat(invoking(cluster).getClusterSize(), is(CLUSTER_SIZE));

            cluster.unordered().limit(2).relaunch(predicate);

            assertThat(invoking(cluster).getClusterSize(), is(CLUSTER_SIZE));

            cluster.unordered().limit(2).relaunch(predicate);

            assertThat(invoking(cluster).getClusterSize(), is(CLUSTER_SIZE));
            }
        }


    /**
     * Ensure that we can create and use a NamedCache via a CoherenceCacheServer.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void shouldAccessNamedCache()
        {
        final int CLUSTER_SIZE = 3;

        AvailablePortIterator availablePorts = LocalPlatform.get().getAvailablePorts();
        ClusterPort clusterPort = ClusterPort.of(new Capture<>(availablePorts));

        CoherenceClusterBuilder builder = new CoherenceClusterBuilder();

        builder.include(CLUSTER_SIZE, CoherenceClusterMember.class, clusterPort, ClusterName.of("Access"));

        try (CoherenceCluster cluster = builder.build(getPlatform(), s_testLogs.builder()))
            {
            assertThat(invoking(cluster).getClusterSize(), is(CLUSTER_SIZE));

            NamedCache<String, String> namedCache = cluster.getCache("dist-example");

            assertThat(namedCache.size(), is(0));
            namedCache.put("key", "hello");

            assertThat(namedCache.size(), is(1));
            assertThat(namedCache.get("key"), is("hello"));
            }
        }


    /**
     * Ensure that a {@link NamedCache} produced by a {@link CoherenceCluster} {@link CoherenceClusterMember}
     * is failed over to another {@link CoherenceClusterMember} when the original {@link CoherenceClusterMember}
     * is closed.
     */
    @Test
    public void shouldFailOverNamedCache()
        {
        final int CLUSTER_SIZE = 3;

        AvailablePortIterator availablePorts = LocalPlatform.get().getAvailablePorts();
        ClusterPort clusterPort = ClusterPort.of(new Capture<>(availablePorts));

        CoherenceClusterBuilder builder = new CoherenceClusterBuilder();

        builder.include(CLUSTER_SIZE,
                        CoherenceClusterMember.class,
                        clusterPort,
                        ClusterName.of("FailOver"),
                        DisplayName.of("DCS"));

        try (CoherenceCluster cluster = builder.build(getPlatform(), s_testLogs.builder()))
            {
            assertThat(invoking(cluster).getClusterSize(), is(CLUSTER_SIZE));

            // acquire a NamedCache from a specific cluster member
            CoherenceClusterMember member = cluster.get("DCS-1");

            NamedCache<String, String> cache = member.getCache("dist-example");

            // use the cache to put some data
            cache.put("message", "hello");

            assertThat(cluster.get("DCS-2").getCache("dist-example").get("message"), is("hello"));

            // close the cluster member
            member.close();

            // ensure that it's not in the cluster
            assertThat(invoking(cluster).getClusterSize(), is(CLUSTER_SIZE - 1));

            // attempt to use the cache
            assertThat(invoking(cache).get("message"), is("hello"));
            }
        }


    /**
     * Ensure that the explicitly closing a CoherenceCacheServer removes it from
     * the CoherenceCluster in which is it defined.
     */
    @Test
    public void shouldRemoveCoherenceClusterMemberFromCoherenceCluster()
        {
        final int CLUSTER_SIZE = 3;

        AvailablePortIterator availablePorts = LocalPlatform.get().getAvailablePorts();
        ClusterPort clusterPort = ClusterPort.of(new Capture<>(availablePorts));

        CoherenceClusterBuilder builder = new CoherenceClusterBuilder();

        builder.include(CLUSTER_SIZE,
                        CoherenceClusterMember.class,
                        clusterPort,
                        ClusterName.of("Access"),
                        DisplayName.of("DCS"));

        try (CoherenceCluster cluster = builder.build(getPlatform(), s_testLogs.builder()))
            {
            assertThat(invoking(cluster).getClusterSize(), is(CLUSTER_SIZE));

            // acquire a cluster member
            CoherenceClusterMember member = cluster.get("DCS-1");

            // close it
            member.close();

            // ensure that it's not in the cluster
            assertThat(invoking(cluster).getClusterSize(), is(CLUSTER_SIZE - 1));
            }
        }


    /**
     * Ensure we can clone {@link CoherenceClusterMember}s in a {@link CoherenceCluster}.
     */
    @Test
    public void shouldCloneMembersOfACluster()
        {
        final int CLUSTER_SIZE = 1;

        AvailablePortIterator availablePorts = LocalPlatform.get().getAvailablePorts();
        ClusterPort clusterPort = ClusterPort.of(new Capture<>(availablePorts));
        String clusterName = "Cloning" + getClass().getSimpleName();
        CoherenceClusterBuilder builder = new CoherenceClusterBuilder();

        builder.include(CLUSTER_SIZE,
                        CoherenceClusterMember.class,
                        DisplayName.of("DCS"),
                        clusterPort,
                        ClusterName.of(clusterName),
                        LocalHost.only(),
                        s_testLogs.builder());

        try (CoherenceCluster cluster = builder.build(getPlatform()))
            {
            assertThat(invoking(cluster).getClusterSize(), is(CLUSTER_SIZE));

            cluster.limit(1).clone(1);

            assertThat(invoking(cluster).getClusterSize(), is(CLUSTER_SIZE + 1));

            cluster.unordered().limit(2).clone(2);

            assertThat(invoking(cluster).getClusterSize(), is(CLUSTER_SIZE + 5));
            }
        }


    /**
     * Ensure we can expand {@link CoherenceClusterMember}s in a {@link CoherenceCluster}.
     */
    @Test
    public void shouldExpandMembersOfACluster()
        {
        final int CLUSTER_SIZE = 1;

        AvailablePortIterator availablePorts = LocalPlatform.get().getAvailablePorts();
        ClusterPort clusterPort = ClusterPort.of(new Capture<>(availablePorts));
        String clusterName = "Cloning" + getClass().getSimpleName();
        CoherenceClusterBuilder builder = new CoherenceClusterBuilder();

        builder.include(CLUSTER_SIZE,
                        CoherenceClusterMember.class,
                        DisplayName.of("DCS"),
                        clusterPort,
                        ClusterName.of(clusterName),
                        LocalHost.only(),
                        s_testLogs.builder());

        try (CoherenceCluster cluster = builder.build(getPlatform()))
            {
            assertThat(invoking(cluster).getClusterSize(), is(CLUSTER_SIZE));

            // ensure that the PartitionedCache service for the first (and only) cluster member is storage disabled
            assertThat(invoking(cluster.get("DCS-1")).isStorageEnabled("PartitionedCache"), is(Trilean.TRUE));

            // ensure that the InvocationService service for the first (and only) cluster member is unknown
            assertThat(invoking(cluster.get("DCS-1")).isStorageEnabled("InvocationService"), is(Trilean.UNKNOWN));

            cluster.expand(1,
                           LocalPlatform.get(),
                           CoherenceClusterMember.class,
                           DisplayName.of("DCS"),
                           clusterPort,
                           ClusterName.of(clusterName),
                           LocalHost.only(),
                           s_testLogs.builder(),
                           LocalStorage.disabled());

            // ensure that the cluster is bigger by one
            assertThat(invoking(cluster).getClusterSize(), is(CLUSTER_SIZE + 1));

            // ensure that the PartitionedCache service for the new member is storage disabled
            assertThat(invoking(cluster.get("DCS-2")).isStorageEnabled("PartitionedCache"), is(Trilean.FALSE));

            // ensure that the InvocationService service for the new member is unknown
            assertThat(invoking(cluster.get("DCS-2")).isStorageEnabled("InvocationService"), is(Trilean.UNKNOWN));
            }
        }


    /**
     * Ensure we clean up an unsuccessfully created {@link CoherenceCluster}.
     */
    @Test
    public void shouldAvoidPartialClusterCreation()
        {
        AvailablePortIterator availablePorts = LocalPlatform.get().getAvailablePorts();
        ClusterPort clusterPort = ClusterPort.of(new Capture<>(availablePorts));

        CoherenceClusterBuilder builder = new CoherenceClusterBuilder();

        // these two should start
        builder.include(2, CoherenceClusterMember.class, clusterPort, LocalHost.only(), ClusterName.of("Storage-Only"));

        // this one will start but fail as the listener raises an exception
        builder.include(1,
                        CoherenceClusterMember.class,
                        clusterPort,
                        LocalHost.only(),
                        ClusterName.of("Storage-Only"),
                        Decoration.of(new ApplicationListener<CoherenceClusterMember>()
                            {
                            @Override
                            public void onClosing(
                                    CoherenceClusterMember application,
                                    OptionsByType optionsByType)
                                {
                                // do nothing
                                }

                            @Override
                            public void onClosed(
                                    CoherenceClusterMember application,
                                    OptionsByType optionsByType)
                                {
                                // do nothing
                                }

                            @Override
                            public void onLaunched(CoherenceClusterMember application)
                                {
                                throw new IllegalStateException("Let's not start this application");
                                }
                            }));

        try (CoherenceCluster ignored = builder.build(getPlatform(), s_testLogs.builder()))
            {
            Assertions.fail("The cluster should not have started");
            }
        catch (RuntimeException e)
            {
            assertThat(e.getMessage(),
                       containsString("Failed to launch one of the desired CoherenceClusterMember(s)"));
            }
        }

    // ----- data members ---------------------------------------------------

    @RegisterExtension
    public static final TestLogsExtension s_testLogs = new TestLogsExtension(AbstractCoherenceClusterBuilderTest.class);
    }
