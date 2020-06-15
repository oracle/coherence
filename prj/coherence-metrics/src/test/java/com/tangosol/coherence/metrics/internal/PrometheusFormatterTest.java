/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.metrics.internal;


import com.tangosol.coherence.metrics.internal.MetricsResource.PrometheusFormatter;
import com.tangosol.internal.metrics.MetricSupport;
import com.tangosol.net.metrics.MBeanMetric;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;

import static com.tangosol.net.metrics.MBeanMetric.Scope.VENDOR;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;


/**
 * Tests for {@code PrometheusFormatter}
 *
 * @author as  2019.06.30
 */
public class PrometheusFormatterTest
    {
    @Test
    public void testMetricWithTags() throws IOException
        {
        MBeanMetric metric = new TestMetric(VENDOR, "coherence.clusterSize", tags(), "Cluster size", 3);

        StringWriter writer = new StringWriter();
        new PrometheusFormatter(false, true, Collections.singletonList(metric)).writeMetrics(writer);

        String expected = "vendor:coherence_cluster_size{cluster=\"testCluster\", site=\"testSite\"} 3\n";

        assertThat(writer.toString(), equalTo(expected));
        }

    @Test
    public void testMetricWithMicroprofileName() throws IOException
        {
        MBeanMetric metric = new TestMetric(VENDOR, "Coherence.ClusterSize", tags(), "Cluster size", 3);

        StringWriter writer = new StringWriter();
        new PrometheusFormatter(false, false, Collections.singletonList(metric)).writeMetrics(writer);

        String expected = "vendor_Coherence_ClusterSize{cluster=\"testCluster\", site=\"testSite\"} 3\n";

        assertThat(writer.toString(), equalTo(expected));
        }

    @Test
    public void testExtendedMetricWithTags() throws IOException
        {
        MBeanMetric metric = new TestMetric(VENDOR, "coherence.clusterSize", tags(), "Cluster size", 3);

        StringWriter writer = new StringWriter();
        new PrometheusFormatter(true, true, Collections.singletonList(metric)).writeMetrics(writer);

        String expected = "# TYPE vendor:coherence_cluster_size gauge \n"
                          + "# HELP vendor:coherence_cluster_size Cluster size\n"
                          + "vendor:coherence_cluster_size{cluster=\"testCluster\", site=\"testSite\"} 3\n";

        assertThat(writer.toString(), equalTo(expected));
        }

    @Test
    public void testMetricWithoutTags() throws IOException
        {
        MBeanMetric metric = new TestMetric(VENDOR, "coherence.clusterSize", Collections.emptyMap(), "Cluster size", 3);

        StringWriter writer = new StringWriter();
        new PrometheusFormatter(false, true, Collections.singletonList(metric)).writeMetrics(writer);

        String expected = "vendor:coherence_cluster_size 3\n";

        assertThat(writer.toString(), equalTo(expected));
        }

    @Test
    public void testExtendedMetricWithoutTags() throws IOException
        {
        MBeanMetric metric = new TestMetric(VENDOR, "coherence.clusterSize", Collections.emptyMap(), "Cluster size", 3);

        StringWriter writer = new StringWriter();
        new PrometheusFormatter(true, true, Collections.singletonList(metric)).writeMetrics(writer);

        String expected = "# TYPE vendor:coherence_cluster_size gauge \n"
                          + "# HELP vendor:coherence_cluster_size Cluster size\n"
                          + "vendor:coherence_cluster_size 3\n";

        assertThat(writer.toString(), equalTo(expected));
        }

    @Test
    public void testExtendedMetricWithoutDescription() throws IOException
        {
        MBeanMetric metric = new TestMetric(VENDOR, "coherence.clusterSize", Collections.emptyMap(), null, 3);

        StringWriter writer = new StringWriter();
        new PrometheusFormatter(true, true, Collections.singletonList(metric)).writeMetrics(writer);

        String expected = "# TYPE vendor:coherence_cluster_size gauge \n"
                          + "vendor:coherence_cluster_size 3\n";

        assertThat(writer.toString(), equalTo(expected));
        }

    private Map<String, String> tags()
        {
        Map<String, String> tags = new HashMap<>();
        tags.put(MetricSupport.GLOBAL_TAG_CLUSTER, "testCluster");
        tags.put(MetricSupport.GLOBAL_TAG_SITE, "testSite");
        return tags;
        }
    }
