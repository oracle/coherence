/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.guides.health;

import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.tangosol.net.Coherence;
import com.tangosol.net.management.Registry;
import com.tangosol.util.HealthCheck;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * A simple integration test showing different parts of the
 * Coherence health check API.
 */
class BasicHealthIT {

    /**
     * Start Coherence, and wait a maximum of five minutes for start-up.
     * Start-up should be considerably less than this, so the test will
     * fail if start-up takes too long.
     *
     * @throws Exception if Coherence fails to start
     */
    // # tag::bootstrap[]
    @BeforeAll
    static void startCoherence() throws Exception {
        Coherence.clusterMember()
                 .start()
                 .get(5, TimeUnit.MINUTES);
    }
    // # end::bootstrap[]

    // # tag::cleanup[]
    @AfterAll
    static void cleanup() {
        Coherence coherence = Coherence.getInstance();
        if (coherence != null) {
            coherence.close();
        }
    }
    // # end::cleanup[]

    // # tag::started[]
    @Test
    void shouldEventuallyBeStarted() {
        Coherence coherence = Coherence.getInstance();
        Registry  registry  = coherence.getManagement();

        Eventually.assertDeferred(registry::allHealthChecksStarted, is(true));
    }
    // # end::started[]

    // # tag::ready[]
    @Test
    void shouldEventuallyBeReady() {
        Coherence coherence = Coherence.getInstance();
        Registry  registry  = coherence.getManagement();

        Eventually.assertDeferred(registry::allHealthChecksReady, is(true));
    }
    // # end::ready[]

    // # tag::safe[]
    @Test
    void shouldEventuallyBeSafe() {
        Coherence coherence = Coherence.getInstance();
        Registry  registry  = coherence.getManagement();

        Eventually.assertDeferred(registry::allHealthChecksSafe, is(true));
    }
    // # end::safe[]

    // # tag::get[]
    @Test
    void shouldGetHealthChecks() {
        Coherence coherence = Coherence.getInstance();
        Registry  registry  = coherence.getManagement();

        Collection<HealthCheck> healthChecks = registry.getHealthChecks();

        assertThat(healthChecks.isEmpty(), is(false));

        HealthCheck healthCheck = healthChecks.stream()
                                              .filter(h->"PartitionedCache".equals(h.getName()))
                                              .findFirst()
                                              .orElse(null);

        assertThat(healthCheck, is(notNullValue()));
    }
    // # end::get[]

    // # tag::name[]
    @Test
    void shouldGetHealthCheckByName() {
        Coherence coherence = Coherence.getInstance();
        Registry  registry  = coherence.getManagement();

        Optional<HealthCheck> optional = registry.getHealthCheck("PartitionedCache");

        assertThat(optional.isPresent(), is(true));
    }
    // # end::name[]
}
