/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package coherence.mp.config.testing;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.Cluster;

import io.helidon.config.Config;
import io.helidon.config.MapConfigSource;
import io.helidon.config.spi.ConfigSource;
import io.helidon.microprofile.server.Server;

import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * This test is for GitHub issue https://github.com/oracle/coherence/issues/35
 *
 * @author Jonathan Knight  2020.12.03
 */
class HelidonConfigPropertyResolverIT
    {
    @Test
    void testPropertyInHelidonServer()
        {
        System.clearProperty("coherence.cluster");

        String     sClusterName = "cdi";
        Properties properties   = new Properties();

        properties.setProperty("coherence.cluster", sClusterName);

        // Use a custom config to start the server to ensure that Coherence
        // is configured from properties in this source
        ConfigSource source = MapConfigSource.create(properties);
        Config       config = Config.create(source);
        Server       server = Server.builder().config(config).build();

        server.start();

        try
            {
            Cluster cluster = CacheFactory.getCluster();
            assertThat(cluster.getClusterName(), is(sClusterName));
            }
        finally
            {
            server.stop();
            }
        }
    }
