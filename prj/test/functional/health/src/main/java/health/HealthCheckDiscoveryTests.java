/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package health;

import com.oracle.bedrock.runtime.coherence.options.ClusterName;
import com.oracle.bedrock.runtime.coherence.options.Logging;
import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.tangosol.net.Coherence;
import com.tangosol.net.CoherenceConfiguration;
import com.tangosol.net.SessionConfiguration;
import com.tangosol.net.management.Registry;
import com.tangosol.util.HealthCheck;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;

public class HealthCheckDiscoveryTests
    {
    @BeforeAll
    static void setup() throws Exception
        {
        System.setProperty(ClusterName.PROPERTY, "HealthCheckDiscoveryTests");
        System.setProperty(Logging.PROPERTY_LEVEL, "9");

        CoherenceConfiguration config = CoherenceConfiguration.builder()
            .withSession(SessionConfiguration.defaultSession())
            .withSession(SessionConfiguration.builder()
                .named("Test")
                .withScopeName("Test")
                .withConfigUri("test-cache-config.xml")
                .build())
            .build();

        s_coherence = Coherence.clusterMember(config)
                .start()
                .get(5, TimeUnit.MINUTES);
        }

    @Test
    public void shouldDiscoverHealthCheck()
        {
        Registry                registry       = s_coherence.getCluster().getManagement();
        Collection<HealthCheck> colHealthCheck = registry.getHealthChecks();

        String sChecks = colHealthCheck.stream()
                .map(h -> h.getName() + " " + h.getClass())
                .collect(Collectors.joining("\n\t"));
        System.err.println("HealthChecks:\n\t" + sChecks);

        List<HealthCheck> list = colHealthCheck.stream()
                .filter(h -> DiscoveredHealthCheck.NAME.equals(h.getName()))
                .collect(Collectors.toList());

        assertThat(list.size(), is(1));

        HealthCheck           healthCheck = list.get(0);
        Optional<HealthCheck> optional    = registry.getHealthCheck(DiscoveredHealthCheck.NAME);

        assertThat(optional.isPresent(), is(true));

        HealthCheck healthCheckByName = optional.get();
        assertThat(healthCheckByName,  is(sameInstance(healthCheck)));

        Eventually.assertDeferred(registry::allHealthChecksReady, is(true));
        assertThat(healthCheck.isReady(), is(true));

        assertThat(registry.allHealthChecksLive(), is(true));
        assertThat(healthCheck.isLive(), is(true));

        assertThat(registry.allHealthChecksStarted(), is(true));
        assertThat(healthCheck.isStarted(), is(true));

        assertThat(registry.allHealthChecksSafe(), is(true));
        assertThat(healthCheck.isSafe(), is(true));
        }

    // ----- data members ---------------------------------------------------

    private static Coherence s_coherence;
    }
