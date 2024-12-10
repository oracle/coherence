/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package management;

import com.oracle.bedrock.Option;
import com.oracle.bedrock.OptionsByType;
import com.oracle.bedrock.runtime.coherence.CoherenceCluster;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.coherence.options.ClusterName;
import com.oracle.bedrock.runtime.coherence.options.WellKnownAddress;
import com.oracle.bedrock.runtime.concurrent.RemoteCallable;
import com.oracle.bedrock.runtime.concurrent.callable.RemoteCallableStaticMethod;

import com.oracle.bedrock.runtime.java.ContainerBasedJavaApplicationLauncher;
import com.oracle.bedrock.runtime.java.JavaVirtualMachine;
import com.oracle.bedrock.runtime.java.container.ContainerClassLoader;

import com.oracle.bedrock.runtime.java.options.ClassName;

import com.oracle.bedrock.runtime.options.Console;
import com.oracle.bedrock.runtime.options.DisplayName;
import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.tangosol.net.Coherence;
import com.tangosol.net.DefaultCacheServer;

import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.hamcrest.CoreMatchers.is;

/**
 * A class that runs multiple Coherence cluster members inside a single JVM.
 */
public class MultiCluster
    {
    public static void main(String[] args)
        {
        try
            {
            JavaVirtualMachine jvm       = JavaVirtualMachine.get();
            String             sClusters = System.getProperty(PROP_CLUSTER_NAMES, BaseManagementInfoResourceTests.CLUSTER_NAME);
            int                cMember   = Integer.getInteger(PROP_MEMBER_PER_CLUSTER, 1);

            System.out.println("Starting Coherence clusters: " + sClusters + " members-per-cluster=" + cMember);
            for (String sCluster : sClusters.split(","))
                {
                CoherenceClusterMember[] aMember = new CoherenceClusterMember[cMember];
                for (int i = 0; i < cMember; i++)
                    {
                    CoherenceClusterMember member = jvm.launch(CoherenceClusterMember.class,
                                                               new CoherenceController(),
                                                               ClusterName.of(sCluster),
                                                               WellKnownAddress.of("127.0.0.1"),
                                                               Console.system(),
                                                               DisplayName.of(sCluster));
                    aMember[i] = member;
                    Thread.sleep(5000);
                    }

                f_mapCluster.put(sCluster, aMember);
                }

            synchronized (WAITER)
                {
                WAITER.wait();
                }
            }
        catch (Throwable t)
            {
            t.printStackTrace();
            }
        }

    /**
     * Get the {@link CoherenceClusterMember} for the specified cluster name.
     *
     * @param sName  the name of the cluster
     *
     * @return the {@link CoherenceClusterMember} for the specified cluster name
     *         or {@code null} if the cluster does not exist
     */
    public static CoherenceClusterMember getCluster(String sName)
        {
        CoherenceClusterMember[] aMember = f_mapCluster.get(sName);
        return aMember == null || aMember.length == 0 ? null : aMember[0];
        }

    /**
     * Assert that a cluster is running in a remote process.
     *
     * @param custer        the {@link CoherenceCluster} wrapping the remote processes
     * @param sClusterName  the name of the cluster to check
     * @param opts          additional options (e.g. timeout)
     */
    public static void assertClusterStarted(CoherenceCluster custer, String sClusterName, Option... opts)
        {
        for (CoherenceClusterMember member : custer)
            {
            assertClusterStarted(member, sClusterName, opts);
            }
        }

    /**
     * Assert that a cluster is running in a remote process.
     *
     * @param member        the {@link CoherenceClusterMember} running the cluster
     * @param sClusterName  the name of the cluster to check
     * @param opts          additional options (e.g. timeout)
     */
    public static void assertClusterStarted(CoherenceClusterMember member, String sClusterName, Option... opts)
        {
        Eventually.assertDeferred(() -> member.invoke(new IsClusterStarted(sClusterName)), is(true), opts);
        }

    /**
     * Invoke a {@link RemoteCallable} in a cluster in a remote process.
     *
     * @param member    the {@link CoherenceClusterMember} running the cluster
     * @param sCluster  the name of the cluster
     * @param callable  the {@link RemoteCallable} to execute
     * @param <T>       the return type of the {@link RemoteCallable}
     *
     * @return  the result returned from executing the {@link RemoteCallable}
     */
    public static <T> T invokeInCluster(CoherenceClusterMember member, String sCluster, RemoteCallable<T> callable)
        {
        return member.invoke(new ClusterRemoteCallable<>(sCluster, callable));
        }

    // ----- inner class: IsClusterStarted ----------------------------------

    /**
     * A {@link RemoteCallable} to determine whether a cluster is running.
     */
    public static class IsClusterStarted
            implements RemoteCallable<Boolean>
        {
        public IsClusterStarted(String sCluster)
            {
            f_sCluster = sCluster;
            }

        @Override
        public Boolean call()
            {
            CoherenceClusterMember member = MultiCluster.getCluster(f_sCluster);
            if (member != null && member.getClusterSize() != 0)
                {
                return member.invoke(() ->
                    {
                    try
                        {
                        DefaultCacheServer dcs = DefaultCacheServer.getInstance();
                        return !dcs.isMonitorStopped();
                        }
                    catch (Exception e)
                        {
                        return false;
                        }
                    });
                }
            return false;
            }

        private final String f_sCluster;
        }

    // ----- inner class: ClusterRemoteCallable -----------------------------

    /**
     * A {@link RemoteCallable} that executes a another {@link RemoteCallable}
     * inside a specific cluster.
     *
     * @param <T>  the return type of the {@link RemoteCallable}
     */
    public static class ClusterRemoteCallable<T>
            implements RemoteCallable<T>
        {
        public ClusterRemoteCallable(String sCluster, RemoteCallable<T> callable)
            {
            f_sCluster = sCluster;
            f_callable = callable;
            }

        @Override
        public T call() throws Exception
            {
            CoherenceClusterMember member = MultiCluster.getCluster(f_sCluster);
            if (member == null)
                {
                throw new IllegalArgumentException("No cluster exists with name " + f_sCluster);
                }
            return member.invoke(f_callable);
            }

        private final String f_sCluster;

        private final RemoteCallable<T> f_callable;
        }

    // ----- inner class: CoherenceController ----------------------------------------------

    /**
     * A Bedrock {@link ContainerBasedJavaApplicationLauncher.ApplicationController}
     * to start and stop clusters in a single JVM.
     */
    public static class CoherenceController
            implements ContainerBasedJavaApplicationLauncher.ApplicationController
        {
        @Override
        public CompletableFuture<Void> start(ContainerBasedJavaApplicationLauncher.ControllableApplication application)
            {
            return application.submit(new StartCoherenceCallable());
            }

        @Override
        public CompletableFuture<Void> destroy(ContainerBasedJavaApplicationLauncher.ControllableApplication application)
            {
            return application.submit(new RemoteCallableStaticMethod<Void>("com.tangosol.net.DefaultCacheServer", "stop"));
            }

        @Override
        public void configure(ContainerClassLoader containerClassLoader, PipedOutputStream pipedOutputStream, PipedInputStream pipedInputStream, OptionsByType optionsByType)
            {
            ClassName className = optionsByType.getOrSetDefault(ClassName.class, ClassName.of(Coherence.class));
            ContainerBasedJavaApplicationLauncher.configureRemoteChannel(containerClassLoader,
                                                                         pipedOutputStream,
                                                                         pipedInputStream,
                                                                         className.getName());
            }
        }

    // ----- inner class: StartCoherenceCallable ----------------------------

    /**
     * A {@link RemoteCallable} to start a {@link DefaultCacheServer}.
     */
    public static class StartCoherenceCallable
        implements RemoteCallable<Void>
        {
        @Override
        public Void call()
            {
            DefaultCacheServer.startServerDaemon();
            return null;
            }
        }

    // ----- constants ------------------------------------------------------

    public static final String PROP_CLUSTER_NAMES = "test.cluster.names";

    public static final String PROP_MEMBER_PER_CLUSTER = "test.cluster.member.count";

    private static final Object WAITER = new Object();

    private static final Map<String, CoherenceClusterMember[]> f_mapCluster = new HashMap<>();
    }
