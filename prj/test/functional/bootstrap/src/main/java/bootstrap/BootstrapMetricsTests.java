/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package bootstrap;

import com.tangosol.internal.net.metrics.MetricsHttpHelper;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.Coherence;
import com.tangosol.net.Service;
import com.tangosol.util.Resources;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class BootstrapMetricsTests
    {
    @BeforeAll
    static void setup()
        {
        System.setProperty("coherence.wka", "127.0.0.1");
        System.setProperty("coherence.localhost", "127.0.0.1");
        System.setProperty("coherence.ttl", "0");
        System.setProperty("coherence.cacheconfig", Resources.DEFAULT_RESOURCE_PACKAGE + "/coherence-cache-config.xml");
        System.setProperty(MetricsHttpHelper.PROP_METRICS_ENABLED, "true");
        }

    @AfterEach
    public void cleanup()
        {
        Coherence.closeAll();
        }

    @Test
    public void shouldStartMetricsWhenClusterMember() throws Exception
        {
        try (Coherence coherence = Coherence.clusterMember())
            {
            coherence.start().get(5, TimeUnit.MINUTES);
            Service service = coherence.getCluster().getService(MetricsHttpHelper.getServiceName());
            assertThat(service, is(notNullValue()));
            assertThat(service.isRunning(), is(true));
            }

        Service service = CacheFactory.getCluster().getService(MetricsHttpHelper.getServiceName());
        assertThat(service, is(notNullValue()));
        assertThat(service.isRunning(), is(true));

        Coherence.closeAll();
        assertThat(service.isRunning(), is(false));
        }

    @Test
    public void shouldStartMetricsWhenClient() throws Exception
        {
        try (Coherence coherence = Coherence.client())
            {
            coherence.start().get(5, TimeUnit.MINUTES);
            Service service = coherence.getCluster().getService(MetricsHttpHelper.getServiceName());
            assertThat(service, is(notNullValue()));
            assertThat(service.isRunning(), is(true));
            }

        Service service = CacheFactory.getCluster().getService(MetricsHttpHelper.getServiceName());
        assertThat(service, is(notNullValue()));
        assertThat(service.isRunning(), is(true));

        Coherence.closeAll();
        assertThat(service.isRunning(), is(false));
        }
    }
