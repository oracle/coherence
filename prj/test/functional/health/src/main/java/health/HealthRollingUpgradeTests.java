/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package health;

import com.oracle.bedrock.runtime.LocalPlatform;

import com.oracle.bedrock.runtime.coherence.CoherenceCluster;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterBuilder;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;

import com.oracle.bedrock.runtime.coherence.options.ClusterName;
import com.oracle.bedrock.runtime.coherence.options.LocalHost;
import com.oracle.bedrock.runtime.coherence.options.LocalStorage;
import com.oracle.bedrock.runtime.coherence.options.WellKnownAddress;

import com.oracle.bedrock.runtime.concurrent.RemoteCallable;
import com.oracle.bedrock.runtime.java.options.IPv4Preferred;

import com.oracle.bedrock.runtime.options.DisplayName;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.bedrock.testsupport.junit.TestLogsExtension;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.coherence.component.net.management.gateway.Local;

import com.tangosol.net.Coherence;

import com.tangosol.net.management.Registry;

import com.tangosol.util.HealthCheck;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Collection;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * A test for BUG 35000403 - Health checks disappear when the management senior transitions to a new member.
 *
 * @author Jonathan Knight  2023.01.24
 */
public class HealthRollingUpgradeTests
    {
    @Test
    public void shouldNotLoseHealthChecks() throws Exception
        {
        LocalPlatform platform = LocalPlatform.get();

        CoherenceClusterBuilder builder = new CoherenceClusterBuilder()
                .with(WellKnownAddress.loopback(),
                      ClusterName.of(getClass().getSimpleName()),
                      DisplayName.of("Storage"),
                      LocalHost.only(),
                      LocalStorage.enabled(),
                      IPv4Preferred.yes(),
                      m_testLogs)
                .include(3, CoherenceClusterMember.class);

        try (CoherenceCluster cluster = builder.build(platform))
            {
            assertHealthChecks(cluster);
            cluster.relaunch();
            assertHealthChecks(cluster);
            }
        }

    protected void assertHealthChecks(CoherenceCluster cluster)
        {
        for (CoherenceClusterMember member : cluster)
            {
            Eventually.assertDeferred(member::isCoherenceRunning, is(true));
            }

        Eventually.assertDeferred(() -> hasManagementSenior(cluster), is(true));

        for (CoherenceClusterMember member : cluster)
            {
            assertThat(member.invoke(HasHealthChecks.INSTANCE), is(true));
            }
        }

    protected boolean hasManagementSenior(CoherenceCluster cluster)
        {
        for (CoherenceClusterMember member : cluster)
            {
            if (member.invoke(HasManagementSenior.INSTANCE))
                {
                return true;
                }
            }
        return false;
        }

    // ----- inner class: HasHealthChecks -----------------------------------

    public static class HasHealthChecks
            implements RemoteCallable<Boolean>
        {
        @Override
        public Boolean call()
            {
            Logger.info("HealthRollingUpgradeTests: checking for health checks");
            Registry management = Coherence.getInstance().getCluster().getManagement();
            Collection<HealthCheck> healthChecks = management.getHealthChecks();
            Logger.info("HealthRollingUpgradeTests: found health checks " + healthChecks);
            return !healthChecks.isEmpty();
            }

        public static final HasHealthChecks INSTANCE = new HasHealthChecks();
        }

    // ----- inner class: HasHealthChecks -----------------------------------

    public static class HasManagementSenior
            implements RemoteCallable<Boolean>
        {
        @Override
        public Boolean call()
            {
            Logger.info("HealthRollingUpgradeTests: checking for management senior");
            Registry management = Coherence.getInstance().getCluster().getManagement();
            Logger.info("HealthRollingUpgradeTests: management is " + management.getClass());
            return management instanceof Local;
            }

        public static final HasManagementSenior INSTANCE = new HasManagementSenior();
        }

    // ----- data members ---------------------------------------------------

    @RegisterExtension
    static final TestLogsExtension m_testLogs = new TestLogsExtension(HealthRollingUpgradeTests.class);
    }
