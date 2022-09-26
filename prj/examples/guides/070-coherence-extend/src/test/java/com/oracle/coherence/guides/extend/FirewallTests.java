/*
 * Copyright (c) 2022 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.guides.extend;

import com.oracle.bedrock.runtime.LocalPlatform;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.coherence.callables.IsServiceRunning;
import com.oracle.bedrock.runtime.coherence.options.CacheConfig;
import com.oracle.bedrock.runtime.coherence.options.ClusterName;
import com.oracle.bedrock.runtime.java.options.IPv4Preferred;
import com.oracle.bedrock.runtime.options.DisplayName;
import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.coherence.guides.extend.model.Country;
import com.oracle.coherence.guides.extend.utils.CoherenceHelper;
import com.tangosol.net.NamedCache;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;

/**
 * A test class showing the usage of Coherence*Extend for the Firewall use-case.
 *
 * @author Gunnar Hillert  2022.09.22
 */
class FirewallTests {
    // # tag::bootstrap[]
    static CoherenceClusterMember server;

    @BeforeAll
    static void setup() {

        final LocalPlatform platform = LocalPlatform.get();

        // Start the Coherence server
        server = platform.launch(CoherenceClusterMember.class,
                CacheConfig.of("firewall/server-coherence-cache-config.xml"),
                IPv4Preferred.yes(),
                ClusterName.of("myCluster"),
                DisplayName.of("server"));

        // Wait for Coherence to start
        Eventually.assertDeferred(() -> server.invoke(new IsServiceRunning("MyCountryExtendService")), is(true));
    }
    // # end::bootstrap[]

    // # tag::cleanup[]
    @AfterAll
    static void shutdownCoherence() {
        if (server != null) {
            server.close();
        }
    }
    // # end::cleanup[]

    @AfterEach
    void cleanupCountriesCache() {
        CoherenceHelper.cleanup();
    }

    // # tag::testFirewallUseCase[]
    @Test
    void testFirewallUseCase() {
        System.setProperty("coherence.tcmp.enabled", "false");
        System.setProperty("coherence.cluster", "myCluster");
        CoherenceHelper.startCoherenceClient(
                CoherenceHelper.FIREWALL_INSTANCE_NAME,
                "firewall/client-coherence-cache-config.xml");
        NamedCache<String, Country> countries = CoherenceHelper.getMap(CoherenceHelper.FIREWALL_INSTANCE_NAME, "countries"); // <4>
        countries.put("de", new Country("Germany", "Berlin", 83.2));
    }
    // # end::testFirewallUseCase[]
}
