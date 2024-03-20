/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package concurrent.config;

import com.oracle.bedrock.junit.CoherenceClusterExtension;

import com.oracle.bedrock.runtime.LocalPlatform;

import com.oracle.bedrock.runtime.coherence.CoherenceCluster;
import com.oracle.bedrock.runtime.coherence.options.ClusterPort;
import com.oracle.bedrock.runtime.coherence.options.LocalHost;
import com.oracle.bedrock.runtime.coherence.options.LocalStorage;
import com.oracle.bedrock.runtime.coherence.options.Logging;
import com.oracle.bedrock.runtime.coherence.options.Multicast;
import com.oracle.bedrock.runtime.coherence.options.RoleName;

import com.oracle.bedrock.runtime.java.options.ClassName;
import com.oracle.bedrock.runtime.java.options.IPv4Preferred;
import com.oracle.bedrock.runtime.java.options.SystemProperty;

import com.oracle.bedrock.runtime.options.DisplayName;

import com.oracle.bedrock.runtime.options.StabilityPredicate;
import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.tangosol.net.Coherence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Test for using override with concurrent cache config.
 *
 * @author cp  03.08.22
 * @since 22.06
 */
public class ConcurrentCacheConfigOverrideTest
    {

    // ----- test methods ---------------------------------------------------

    @Test
    public void testConcurrentCacheConfigOverride()
            throws Exception
        {
        assertThat(clusterResource, is(notNullValue()));

        Eventually.assertThat(1, is(clusterResource.getCluster().getClusterSize()));

        // Check for ConcurrentProxy Service auto started due to cache config override specified
        Eventually.assertDeferred(() -> clusterResource.getCluster().findFirst()
                .get().isServiceRunning("$SYS:ConcurrentProxy"), is(true));
        }

    // ----- data members ---------------------------------------------------

    /**
     * Override file for concurrent cacheconfig xml.
     */
    public final static String FILE_CFG_CACHE_OVERRIDE = "concurrent-cache-config-override.xml";

    /**
     * A Bedrock JUnit5 extension to start a Coherence cluster with
     * storage-enabled member.
     */
    @RegisterExtension
    static CoherenceClusterExtension clusterResource =
            new CoherenceClusterExtension()
                    .using(LocalPlatform.get())
                    .with(ClassName.of(Coherence.class),
                          SystemProperty.of("coherence.concurrent.cacheconfig.override",
                                  FILE_CFG_CACHE_OVERRIDE),
                          Logging.at(9),
                          LocalHost.only(),
                          Multicast.ttl(0),
                          IPv4Preferred.yes(),
                          ClusterPort.automatic(),
                          StabilityPredicate.of(CoherenceCluster.Predicates.isCoherenceRunning()))
                    .include(1,
                             DisplayName.of("storage"),
                             RoleName.of("storage"),
                             LocalStorage.enabled(),
                             Logging.at(9));
    }
