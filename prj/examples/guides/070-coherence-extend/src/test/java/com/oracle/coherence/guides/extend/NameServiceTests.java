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
import com.oracle.bedrock.runtime.java.options.SystemProperty;
import com.oracle.bedrock.runtime.options.DisplayName;
import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.coherence.guides.extend.model.Country;
import com.oracle.coherence.guides.extend.utils.CoherenceHelper;

import com.tangosol.net.NamedCache;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.is;

/**
 * A test class showing the usage of Coherence*Extend for the name service use-case.
 *
 * @author Gunnar Hillert  2022.09.22
 */
class NameServiceTests {
    // # tag::bootstrap[]
    static CoherenceClusterMember server;

    @BeforeAll
    static void setup() {

        final LocalPlatform platform = LocalPlatform.get();

        // Start the Coherence server
        server = platform.launch(CoherenceClusterMember.class,
                CacheConfig.of("name-service/server-coherence-cache-config.xml"), // <1>
                IPv4Preferred.yes(),
                SystemProperty.of("coherence.wka", "127.0.0.1"),
                ClusterName.of("myCluster"), // <2>
                DisplayName.of("server"));

        // Wait for Coherence to start
        Eventually.assertDeferred(() -> server.invoke(
                new IsServiceRunning("MyCountryExtendService")), is(true)); // <3>
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

    // # tag::testNameServiceUseCase[]
    @Test
    void testNameServiceUseCase() {
        System.setProperty("coherence.tcmp.enabled", "false"); // <1>
        System.setProperty("coherence.cluster", "myCluster"); // <2>
        System.setProperty("coherence.wka", "127.0.0.1");
        CoherenceHelper.startCoherenceClient(
                CoherenceHelper.NAME_SERVICE_INSTANCE_NAME,
                "name-service/client-coherence-cache-config.xml"); // <3>
        NamedCache<String, Country> countries = CoherenceHelper.getMap(CoherenceHelper.NAME_SERVICE_INSTANCE_NAME,"countries"); // <4>
        countries.put("de", new Country("Germany", "Berlin", 83.2));
    }
    // # end::testNameServiceUseCase[]
}
