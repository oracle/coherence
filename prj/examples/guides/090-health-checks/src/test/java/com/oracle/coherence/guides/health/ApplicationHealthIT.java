/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.guides.health;

import com.tangosol.net.Coherence;
import com.tangosol.net.management.Registry;
import com.tangosol.util.HealthCheck;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ApplicationHealthIT {

    @BeforeAll
    static void startCoherence() throws Exception {
        Coherence.clusterMember()
                 .start()
                 .get(5, TimeUnit.MINUTES);
    }

    @Test
    public void shouldRegisterApplicationHealthCheck() {
        // # tag::register[]
        Coherence coherence = Coherence.getInstance();
        Registry  registry  = coherence.getManagement();

        ApplicationHealth healthCheck = new ApplicationHealth();

        registry.register(healthCheck);

        Optional<HealthCheck> optional = registry.getHealthCheck(ApplicationHealth.NAME);
        assertThat(optional.isPresent(), is(true));
        // # end::register[]

        // # tag::unregister[]
        registry.unregister(healthCheck);
        optional = registry.getHealthCheck(ApplicationHealth.NAME);
        assertThat(optional.isPresent(), is(false));
        // # end::unregister[]
    }
}
