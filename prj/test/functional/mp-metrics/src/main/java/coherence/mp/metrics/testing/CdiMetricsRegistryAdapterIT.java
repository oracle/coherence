/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package coherence.mp.metrics.testing;

import com.oracle.coherence.cdi.CoherenceExtension;

import com.oracle.coherence.cdi.server.CoherenceServerExtension;

import com.oracle.coherence.mp.metrics.CdiMetricsRegistryAdapter;
import com.tangosol.net.metrics.MBeanMetric;
import com.tangosol.net.metrics.MetricsRegistryAdapter;

import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldSetup;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.enterprise.context.ApplicationScoped;

import javax.inject.Inject;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Unit tests for {@link CdiMetricsRegistryAdapter}.
 *
 * @author Aleks Seovic  2019.10.12
 */
@ExtendWith(WeldJunit5Extension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CdiMetricsRegistryAdapterIT
    {
    @WeldSetup
    private final WeldInitiator weld = WeldInitiator.of(WeldInitiator.createWeld()
                                                          .addExtension(new CoherenceExtension())
                                                          .addPackages(CoherenceExtension.class)
                                                          .addExtension(new CoherenceServerExtension())
                                                          .addPackages(CoherenceServerExtension.class)
                                                          .addBeanClass(TestAdapter.class));

    @Inject
    TestAdapter adapter;

    @Test
    void testAdapter()
        {
        assertThat(adapter.getMetrics().get("Coherence.Cluster.Size"), is(1));
        }

    @ApplicationScoped
    static class TestAdapter
            implements MetricsRegistryAdapter
        {
        Map<String, Object> metrics = new HashMap<>();

        public Map<String, Object> getMetrics()
            {
            return metrics;
            }

        public void register(MBeanMetric metric)
            {
            metrics.put(metric.getName(), metric.getValue());
            }

        public void remove(MBeanMetric.Identifier identifier)
            {
            }
        }
    }
