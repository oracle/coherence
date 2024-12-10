/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package micrometer;

import com.oracle.coherence.common.base.Logger;

import com.oracle.coherence.micrometer.CoherenceMicrometerMetrics;
import com.tangosol.net.DefaultCacheServer;

import com.tangosol.net.metrics.MBeanMetric;

import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;

import io.prometheus.client.Collector;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Integration test for {@link CoherenceMicrometerMetrics}.
 *
 * @author Jonathan Knight  2020.10.09
 */
public class CoherenceMicrometerMetricsIT
    {
    @BeforeAll
    static void startCoherence()
        {
        s_prometheusRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);

        s_prometheusRegistry.config().onMeterRegistrationFailed((id, err) ->
            {
            Logger.err("Registration of " + id + " failed with registry " + s_prometheusRegistry + ": " + err);
            });

        CoherenceMicrometerMetrics.INSTANCE.bindTo(s_prometheusRegistry);

        DefaultCacheServer.startServerDaemon()
                .waitForServiceStart();
        }

    @Test
    public void shouldHaveAllMetricsWithSamePrometheusNameAsCoherenceMetrics()
        {
        SortedSet<String> setExpected = new TreeSet<>();
        SortedSet<String> setActual   = new TreeSet<>();

        Map<MBeanMetric.Identifier, CoherenceMicrometerMetrics.Holder> metrics
                = CoherenceMicrometerMetrics.INSTANCE.getMetrics();

        for (CoherenceMicrometerMetrics.Holder holder : CoherenceMicrometerMetrics.INSTANCE.getMetrics().values())
            {
            // name without scope
            String sName = holder.getIdentifier().getLegacyName().substring(7);
            StringBuilder str = new StringBuilder(sName);
            for (Map.Entry<String, String> entry : holder.getIdentifier().getPrometheusTags().entrySet())
                {
                str.append(" ").append(entry.getKey()).append("=").append(entry.getValue());
                }
            setExpected.add(str.toString());
            }

        Enumeration<Collector.MetricFamilySamples> samples = s_prometheusRegistry.getPrometheusRegistry().metricFamilySamples();
        while (samples.hasMoreElements())
            {
            Collector.MetricFamilySamples sample = samples.nextElement();

            sample.samples.forEach(s -> setActual.add(sampleToString(s)));
            }

        Iterator<String> itExpected = setExpected.iterator();
        Iterator<String> itActual   = setActual.iterator();
        while (itExpected.hasNext())
            {
            assertThat(itActual.hasNext(), is(true));
            assertThat(itActual.next(), is(itExpected.next()));
            }
        }

    private String sampleToString(Collector.MetricFamilySamples.Sample sample)
        {
        StringBuilder str = new StringBuilder(sample.name);
        for (int i=0; i<sample.labelNames.size(); i++)
            {
            str.append(" ").append(sample.labelNames.get(i)).append("=").append(sample.labelValues.get(i));
            }
        return str.toString();
        }

    // ----- data members ---------------------------------------------------

    private static PrometheusMeterRegistry s_prometheusRegistry;
    }
