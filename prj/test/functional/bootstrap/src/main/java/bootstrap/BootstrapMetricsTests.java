/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package bootstrap;

import com.oracle.bedrock.runtime.LocalPlatform;

import com.tangosol.internal.net.metrics.MetricsHttpHelper;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.Coherence;
import com.tangosol.net.Service;

import com.tangosol.util.Resources;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.TestInfo;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class BootstrapMetricsTests
    {
    @BeforeAll
    static void setup()
        {
        String sAddress = LocalPlatform.get().getLoopbackAddress().getHostAddress();

        System.setProperty("coherence.wka", sAddress);
        System.setProperty("coherence.localhost", sAddress);
        System.setProperty("coherence.ttl", "0");
        System.setProperty("java.net.preferIPv4Stack", "true");
        System.setProperty("test.unicast.address", sAddress);
        System.setProperty("test.unicast.port", "0");
        System.setProperty("coherence.cacheconfig", Resources.DEFAULT_RESOURCE_PACKAGE + "/coherence-cache-config.xml");
        System.setProperty(MetricsHttpHelper.PROP_METRICS_ENABLED, "true");
        }

    @BeforeEach
    public void setupTest(TestInfo info)
        {
        System.err.println(">>>> Starting test " + info.getDisplayName());

        m_nClusterPort = LocalPlatform.get().getAvailablePorts().next();
        System.setProperty("test.multicast.port", String.valueOf(m_nClusterPort));
        }

    @AfterEach
    public void cleanup(TestInfo info)
        {
        Coherence.closeAll();
        CacheFactory.getCacheFactoryBuilder().releaseAll(null);
        CacheFactory.shutdown();
        System.err.println(">>>> Completed clean-up after test " + info.getDisplayName());
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
        }

    // ----- data members ---------------------------------------------------

    public static int m_nClusterPort;
    }
