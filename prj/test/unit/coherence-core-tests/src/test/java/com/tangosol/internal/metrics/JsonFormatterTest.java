/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.metrics;


import com.tangosol.internal.metrics.MetricsHttpHandler.JsonFormatter;

import com.tangosol.net.metrics.MBeanMetric;

import java.io.IOException;
import java.io.StringWriter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import static com.tangosol.net.metrics.MBeanMetric.Scope.VENDOR;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;


/**
 * Tests for {@code JsonFormatter}
 *
 * @author as  2019.06.30
 */
public class JsonFormatterTest
    {
    @Test
    public void testMetricWithTags() throws IOException
        {
        MBeanMetric metric = new TestMetric(VENDOR, "coherence.clusterSize", tags(), "Cluster size", 3);

        StringWriter writer = new StringWriter();
        new JsonFormatter(false, Collections.singletonList(metric)).writeMetrics(writer);

        String expected = "[{\"name\":\"coherence.clusterSize\",\"tags\":{\"cluster.name\":\"testCluster\",\"site.name\":\"testSite\"},\"scope\":\"VENDOR\",\"value\":3}]";

        assertThat(writer.toString(), equalTo(expected));
        }

    @Test
    public void testExtendedMetricWithTags() throws IOException
        {
        MBeanMetric metric = new TestMetric(VENDOR, "coherence.clusterSize", tags(), "Cluster size", 3);

        StringWriter writer = new StringWriter();
        new JsonFormatter(true, Collections.singletonList(metric)).writeMetrics(writer);

        String expected = "[{\"name\":\"coherence.clusterSize\","
                          + "\"tags\":{\"cluster.name\":\"testCluster\",\"site.name\":\"testSite\"},"
                          + "\"scope\":\"VENDOR\",\"value\":3,\"description\":\"Cluster size\"}]";
        assertThat(writer.toString(), equalTo(expected));
        }

    @Test
    public void testMetricWithoutTags() throws IOException
        {
        MBeanMetric metric = new TestMetric(VENDOR, "coherence.clusterSize", Collections.emptyMap(), "Cluster size", 3);

        StringWriter writer = new StringWriter();
        new JsonFormatter(false, Collections.singletonList(metric)).writeMetrics(writer);

        String expected = "[{\"name\":\"coherence.clusterSize\",\"scope\":\"VENDOR\",\"value\":3}]";

        assertThat(writer.toString(), equalTo(expected));
        }

    @Test
    public void testExtendedMetricWithoutTags() throws IOException
        {
        MBeanMetric metric = new TestMetric(VENDOR, "coherence.clusterSize", Collections.emptyMap(), "Cluster size", 3);

        StringWriter writer = new StringWriter();
        new JsonFormatter(true, Collections.singletonList(metric)).writeMetrics(writer);

        String expected = "[{\"name\":\"coherence.clusterSize\",\"scope\":\"VENDOR\",\"value\":3,\"description\":\"Cluster size\"}]";

        assertThat(writer.toString(), equalTo(expected));
        }

    @Test
    public void testExtendedMetricWithoutDescription() throws IOException
        {
        MBeanMetric metric = new TestMetric(VENDOR, "coherence.clusterSize", Collections.emptyMap(), null, 3);

        StringWriter writer = new StringWriter();
        new JsonFormatter(true, Collections.singletonList(metric)).writeMetrics(writer);

        String expected = "[{\"name\":\"coherence.clusterSize\",\"scope\":\"VENDOR\",\"value\":3}]";

        assertThat(writer.toString(), equalTo(expected));
        }

    private Map<String, String> tags()
        {
        Map<String, String> tags = new HashMap<>();
        tags.put("cluster.name", "testCluster");
        tags.put("site.name", "testSite");
        return tags;
        }
    }
