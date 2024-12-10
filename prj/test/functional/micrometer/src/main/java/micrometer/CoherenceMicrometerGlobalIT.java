/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package micrometer;

import com.oracle.coherence.micrometer.CoherenceMicrometerMetrics;
import com.tangosol.net.DefaultCacheServer;
import com.tangosol.net.metrics.MBeanMetric;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Metrics;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Integration test for {@link CoherenceMicrometerMetrics}
 * automatically using the Micrometer global registry.
 *
 * @author Jonathan Knight  2020.10.09
 */
public class CoherenceMicrometerGlobalIT
    {
    @BeforeAll
    static void startCoherence()
        {
        System.setProperty(CoherenceMicrometerMetrics.PROP_USE_GLOBAL_REGISTRY, "true");

        DefaultCacheServer.startServerDaemon()
                .waitForServiceStart();
        }

    @Test
    public void shouldHaveAllMetrics()
        {
        Map<MBeanMetric.Identifier, CoherenceMicrometerMetrics.Holder> metrics = CoherenceMicrometerMetrics.INSTANCE.getMetrics();
        List<Meter> meters = Metrics.globalRegistry.getMeters();
        assertThat(meters.size(), is(metrics.size()));
        }
    }
