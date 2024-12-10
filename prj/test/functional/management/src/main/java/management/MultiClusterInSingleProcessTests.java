/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package management;

import com.oracle.bedrock.OptionsByType;
import com.oracle.bedrock.options.Timeout;
import com.oracle.bedrock.runtime.coherence.CoherenceCluster;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.coherence.ServiceStatus;
import com.oracle.bedrock.runtime.coherence.callables.GetServiceStatus;
import com.oracle.bedrock.runtime.coherence.callables.IsReady;
import com.oracle.bedrock.runtime.concurrent.RemoteCallable;
import com.oracle.bedrock.runtime.java.features.JmxFeature;
import com.oracle.bedrock.runtime.java.options.JavaModules;
import com.oracle.bedrock.runtime.java.options.SystemProperty;
import com.oracle.bedrock.runtime.java.profiles.JmxProfile;
import com.oracle.bedrock.runtime.options.StabilityPredicate;
import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.coherence.testing.util.VersionUtils;
import com.tangosol.internal.net.management.HttpHelper;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static com.oracle.bedrock.deferred.DeferredHelper.within;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.lessThan;

/**
 * Test Management over REST when multiple Coherence clusters run in the same JVM.
 * <p>
 * This is similar to environments like Web Servers or WLS where a single JVM may
 * contain more than one Coherence cluster, isolated by class loader.
 */
//@Ignore
public class MultiClusterInSingleProcessTests
        extends BaseManagementInfoResourceTests
    {
    public MultiClusterInSingleProcessTests()
        {
        super(CLUSTER_NAME, MultiClusterInSingleProcessTests::invokeInCluster);
        super.EXPECTED_SERVICE_COUNT = 6;
        }

    @BeforeClass
    public static void _startup()
        {
        String sJava = System.getProperty("java.version");
        Assume.assumeThat("Skipping test on Java 19 and higher",
                          VersionUtils.compare(sJava, "19"), is(lessThan(0)));

        String sClusterNames = CLUSTER_NAME + ",Foo";
        startTestCluster(MultiCluster.class,
                         CLUSTER_NAME,
                         MultiClusterInSingleProcessTests::assertMultiClusterReady,
                         MultiClusterInSingleProcessTests::invokeInCluster,
                         MultiClusterInSingleProcessTests::configureOptions,
                         SystemProperty.of(MultiCluster.PROP_CLUSTER_NAMES, sClusterNames),
                         StabilityPredicate.of(assembly -> true));
        }

    // ----- Overridden tests -----------------------------------------------

    @Test
    @Override
    public void testJmxJfr()
        {
        // skipped
        }

    @Test
    @Override
    public void testServiceStartAndStop()
        {
        // skipped
        }

    @Test
    @Override
    public void testServiceMemberStartAndStop()
        {
        // skipped
        }

    @Test
    @Override
    public void testHealthChecks()
        {
        // skipped
        }

    // ----- helper methods -------------------------------------------------

    private static OptionsByType configureOptions(OptionsByType options)
        {
        options.remove(JmxProfile.class);
        options.add(SystemProperty.of(JmxFeature.SUN_MANAGEMENT_JMXREMOTE));
        options.add(SystemProperty.of(JmxFeature.SUN_MANAGEMENT_JMXREMOTE_PORT, 0));
        options.add(SystemProperty.of(JmxFeature.SUN_MANAGEMENT_JMXREMOTE_AUTHENTICATE, false));
        options.add(SystemProperty.of(JmxFeature.SUN_MANAGEMENT_JMXREMOTE_SSL, false));
        return options;
        }

    /**
     * There are multiple clusters running in the remote process, so this
     * method wil invoke the remote runnable in the correct cluster ClassLoader
     * in the remote process.
     *
     * @param sCluster  the name of the cluster in the remote process
     * @param sMember   the name of the cluster member in the remote process
     * @param callable  the {@link RemoteCallable} to execute
     */
    protected static void invokeInCluster(String sCluster, String sMember, RemoteCallable<Void> callable)
        {
        CoherenceClusterMember member;
        if (sMember == null)
            {
            member = s_cluster.getAny();
            }
        else
            {
            member = s_cluster.get(sMember);
            }
        MultiCluster.invokeInCluster(member, sCluster, callable);
        }

    /**
     * Assert that the remote {@link CoherenceCluster} is ready.
     *
     * @param cluster  the cluster to check
     */
    private static void assertMultiClusterReady(CoherenceCluster cluster)
        {
        MultiCluster.assertClusterStarted(cluster, CLUSTER_NAME);
        MultiCluster.assertClusterStarted(cluster, "Foo");

        CoherenceClusterMember member = cluster.getAny();
        Eventually.assertDeferred("assert " + HttpHelper.getServiceName() + " is running in one cluster member of cluster " + CLUSTER_NAME,
            () ->
                {
                // ensure one cluster member is running ManagementHttpProxy service. not always the first one in cluster list.
                ServiceStatus status = null;
                for (CoherenceClusterMember member1 : cluster)
                    {
                    status = MultiCluster.invokeInCluster(member1, CLUSTER_NAME, new GetServiceStatus(HttpHelper.getServiceName()));
                    if (ServiceStatus.RUNNING.equals(status))
                        {
                        break;
                        }
                    }
                return status;
                }, is(ServiceStatus.RUNNING), Timeout.of(5, TimeUnit.MINUTES));
        Eventually.assertDeferred(() -> MultiCluster.invokeInCluster(member, CLUSTER_NAME, new GetServiceStatus(SERVICE_NAME)), is(ServiceStatus.NODE_SAFE), Timeout.of(5, TimeUnit.MINUTES));
        Eventually.assertDeferred(() -> MultiCluster.invokeInCluster(member, CLUSTER_NAME, new GetServiceStatus(ACTIVE_SERVICE)), is(ServiceStatus.NODE_SAFE), Timeout.of(5, TimeUnit.MINUTES));
        Eventually.assertDeferred(() -> MultiCluster.invokeInCluster(member, CLUSTER_NAME, IsReady.INSTANCE), is(true), within(5, TimeUnit.MINUTES));
        }
    }
