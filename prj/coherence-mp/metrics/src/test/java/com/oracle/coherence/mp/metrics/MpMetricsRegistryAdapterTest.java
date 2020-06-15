/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.mp.metrics;

import com.tangosol.internal.metrics.BaseMBeanMetric;
import com.tangosol.net.metrics.MBeanMetric;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.Tag;

import org.junit.jupiter.api.Test;

import org.mockito.ArgumentCaptor;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link MpMetricsRegistryAdapter}.
 *
 * @author Jonathan Knight  2020.01.08
 */
@SuppressWarnings("unchecked")
public class MpMetricsRegistryAdapterTest
    {

    @Test
    void shouldRegisterVendorMetric()
        {
        MetricRegistry vendorRegistry = mock(MetricRegistry.class);
        MetricRegistry appRegistry = mock(MetricRegistry.class);
        Map<String, String> tagsMap = createTagsMap("key1", "value1", "key2", "value2");
        Tag[] tags = createTags(tagsMap);
        MBeanMetric.Identifier id = new MBeanMetric.Identifier(MBeanMetric.Scope.VENDOR, "foo", tagsMap);
        MBeanMetric metric = new MBeanMetricStub(id, "Coherence,type=foo", "test mBean", 1234);
        SortedMap<MetricID, Gauge> gauges = new TreeMap<>();

        when(vendorRegistry.getGauges()).thenReturn(gauges);

        MpMetricsRegistryAdapter adapter = new MpMetricsRegistryAdapter(vendorRegistry, appRegistry);
        adapter.register(metric);

        ArgumentCaptor<Metadata> metadataArgument = ArgumentCaptor.forClass(Metadata.class);
        ArgumentCaptor<Gauge<Object>> gaugeArgument = ArgumentCaptor.forClass(Gauge.class);
        ArgumentCaptor<Tag> tagsArgument = ArgumentCaptor.forClass(Tag.class);

        verify(vendorRegistry).register(metadataArgument.capture(), gaugeArgument.capture(), tagsArgument.capture());
        verifyNoMoreInteractions(appRegistry);

        Metadata metadata = metadataArgument.getValue();
        assertThat(metadata, is(notNullValue()));
        assertThat(metadata.getName(), is(metric.getName()));
        assertThat(metadata.getDescription().isPresent(), is(true));
        assertThat(metadata.getDescription().get(), is(metric.getDescription()));
        assertThat(metadata.getTypeRaw(), is(MetricType.GAUGE));

        Gauge<Object> gauge = gaugeArgument.getValue();
        assertThat(gauge, is(notNullValue()));
        assertThat(gauge.getValue(), is(metric.getValue()));

        List<Tag> tagList = tagsArgument.getAllValues();
        assertThat(tagList, containsInAnyOrder(tags));
        }

    @Test
    void shouldNotRegisterExistingVendorMetric()
        {
        MetricRegistry vendorRegistry = mock(MetricRegistry.class);
        MetricRegistry appRegistry = mock(MetricRegistry.class);
        Map<String, String> tagsMap = createTagsMap("key1", "value1", "key2", "value2");
        Tag[] tags = createTags(tagsMap);
        MBeanMetric.Identifier id = new MBeanMetric.Identifier(MBeanMetric.Scope.VENDOR, "foo", tagsMap);
        MBeanMetric metric = new MBeanMetricStub(id, "Coherence,type=foo", "test mBean", 1234);
        SortedMap<MetricID, Gauge> gauges = new TreeMap<>();

        when(vendorRegistry.getGauges()).thenReturn(gauges);

        gauges.put(new MetricID(metric.getName(), tags), mock(Gauge.class));

        MpMetricsRegistryAdapter adapter = new MpMetricsRegistryAdapter(vendorRegistry, appRegistry);
        adapter.register(metric);

        verify(vendorRegistry).getGauges();
        verifyNoMoreInteractions(vendorRegistry);
        verifyNoMoreInteractions(appRegistry);
        }

    @Test
    void shouldNotIncludeInvalidTagInVendorMetric()
        {
        MetricRegistry vendorRegistry = mock(MetricRegistry.class);
        MetricRegistry appRegistry = mock(MetricRegistry.class);
        Map<String, String> tagsMap = createTagsMap("@£!", "value1", "key2", "value2");
        Tag[] tags = new Tag[] {new Tag("key2", "value2")};
        MBeanMetric.Identifier id = new MBeanMetric.Identifier(MBeanMetric.Scope.VENDOR, "foo", tagsMap);
        MBeanMetric metric = new MBeanMetricStub(id, "Coherence,type=foo", "test mBean", 1234);

        MpMetricsRegistryAdapter adapter = new MpMetricsRegistryAdapter(vendorRegistry, appRegistry);
        adapter.register(metric);

        ArgumentCaptor<Metadata> metadataArgument = ArgumentCaptor.forClass(Metadata.class);
        ArgumentCaptor<Gauge<Object>> gaugeArgument = ArgumentCaptor.forClass(Gauge.class);
        ArgumentCaptor<Tag> tagsArgument = ArgumentCaptor.forClass(Tag.class);

        verify(vendorRegistry).register(metadataArgument.capture(), gaugeArgument.capture(), tagsArgument.capture());
        verifyNoMoreInteractions(appRegistry);

        Metadata metadata = metadataArgument.getValue();
        assertThat(metadata, is(notNullValue()));
        assertThat(metadata.getName(), is(metric.getName()));
        assertThat(metadata.getDescription().isPresent(), is(true));
        assertThat(metadata.getDescription().get(), is(metric.getDescription()));
        assertThat(metadata.getTypeRaw(), is(MetricType.GAUGE));

        Gauge<Object> gauge = gaugeArgument.getValue();
        assertThat(gauge, is(notNullValue()));
        assertThat(gauge.getValue(), is(metric.getValue()));

        List<Tag> tagList = tagsArgument.getAllValues();
        assertThat(tagList, containsInAnyOrder(tags));
        }

    @Test
    void shouldRemoveVendorMetric()
        {
        MetricRegistry vendorRegistry = mock(MetricRegistry.class);
        MetricRegistry appRegistry = mock(MetricRegistry.class);
        Map<String, String> tagsMap = createTagsMap("key1", "value1", "key2", "value2");
        MBeanMetric.Identifier id = new MBeanMetric.Identifier(MBeanMetric.Scope.VENDOR, "foo", tagsMap);
        Tag[] tags = createTags(tagsMap);
        MetricID metricID = new MetricID(id.getName(), tags);
        Gauge<?> gauge = mock(Gauge.class);

        when(vendorRegistry.getGauges()).thenReturn(new TreeMap<>(Collections.singletonMap(metricID, gauge)));
        when(appRegistry.getGauges()).thenReturn(Collections.emptySortedMap());

        MpMetricsRegistryAdapter adapter = new MpMetricsRegistryAdapter(vendorRegistry, appRegistry);
        adapter.remove(id);

        verify(vendorRegistry).getGauges();
        verify(vendorRegistry).remove(metricID);
        verifyNoMoreInteractions(vendorRegistry);
        verifyNoMoreInteractions(appRegistry);
        }

    @Test
    void shouldNotRemoveNonExistentVendorMetric()
        {
        MetricRegistry vendorRegistry = mock(MetricRegistry.class);
        MetricRegistry appRegistry = mock(MetricRegistry.class);
        Map<String, String> tagsMap = createTagsMap("key1", "value1", "key2", "value2");
        MBeanMetric.Identifier id = new MBeanMetric.Identifier(MBeanMetric.Scope.VENDOR, "foo", tagsMap);
        Tag[] tags = createTags(tagsMap);
        MetricID metricID = new MetricID(id.getName(), tags);
        Gauge<?> gauge = mock(Gauge.class);

        when(vendorRegistry.getGauges()).thenReturn(Collections.emptySortedMap());
        when(appRegistry.getGauges()).thenReturn(new TreeMap<>(Collections.singletonMap(metricID, gauge)));

        MpMetricsRegistryAdapter adapter = new MpMetricsRegistryAdapter(vendorRegistry, appRegistry);
        adapter.remove(id);

        verify(vendorRegistry).getGauges();
        verifyNoMoreInteractions(vendorRegistry);
        verifyNoMoreInteractions(appRegistry);
        }

    @Test
    void shouldRegisterApplicationMetric()
        {
        MetricRegistry vendorRegistry = mock(MetricRegistry.class);
        MetricRegistry appRegistry = mock(MetricRegistry.class);
        Map<String, String> tagsMap = createTagsMap("key1", "value1", "key2", "value2");
        Tag[] tags = createTags(tagsMap);
        MBeanMetric.Identifier id = new MBeanMetric.Identifier(MBeanMetric.Scope.APPLICATION, "foo", tagsMap);
        MBeanMetric metric = new MBeanMetricStub(id, "Coherence,type=foo", "test mBean", 1234);

        MpMetricsRegistryAdapter adapter = new MpMetricsRegistryAdapter(vendorRegistry, appRegistry);
        adapter.register(metric);

        ArgumentCaptor<Metadata> metadataArgument = ArgumentCaptor.forClass(Metadata.class);
        ArgumentCaptor<Gauge<Object>> gaugeArgument = ArgumentCaptor.forClass(Gauge.class);
        ArgumentCaptor<Tag> tagsArgument = ArgumentCaptor.forClass(Tag.class);

        verify(appRegistry).register(metadataArgument.capture(), gaugeArgument.capture(), tagsArgument.capture());
        verifyNoMoreInteractions(vendorRegistry);

        Metadata metadata = metadataArgument.getValue();
        assertThat(metadata, is(notNullValue()));
        assertThat(metadata.getName(), is(metric.getName()));
        assertThat(metadata.getDescription().isPresent(), is(true));
        assertThat(metadata.getDescription().get(), is(metric.getDescription()));
        assertThat(metadata.getTypeRaw(), is(MetricType.GAUGE));

        Gauge<Object> gauge = gaugeArgument.getValue();
        assertThat(gauge, is(notNullValue()));
        assertThat(gauge.getValue(), is(metric.getValue()));

        List<Tag> tagList = tagsArgument.getAllValues();
        assertThat(tagList, containsInAnyOrder(tags));
        }

    @Test
    void shouldNotRegisterExistingApplicationMetric()
        {
        MetricRegistry vendorRegistry = mock(MetricRegistry.class);
        MetricRegistry appRegistry = mock(MetricRegistry.class);
        Map<String, String> tagsMap = createTagsMap("key1", "value1", "key2", "value2");
        Tag[] tags = createTags(tagsMap);
        MBeanMetric.Identifier id = new MBeanMetric.Identifier(MBeanMetric.Scope.APPLICATION, "foo", tagsMap);
        MBeanMetric metric = new MBeanMetricStub(id, "Coherence,type=foo", "test mBean", 1234);
        SortedMap<MetricID, Gauge> gauges = new TreeMap<>();

        when(appRegistry.getGauges()).thenReturn(gauges);

        gauges.put(new MetricID(metric.getName(), tags), mock(Gauge.class));

        MpMetricsRegistryAdapter adapter = new MpMetricsRegistryAdapter(vendorRegistry, appRegistry);
        adapter.register(metric);

        verify(appRegistry).getGauges();
        verifyNoMoreInteractions(appRegistry);
        verifyNoMoreInteractions(vendorRegistry);
        }

    @Test
    void shouldNotIncludeInvalidTagInApplicationMetric()
        {
        MetricRegistry vendorRegistry = mock(MetricRegistry.class);
        MetricRegistry appRegistry = mock(MetricRegistry.class);
        Map<String, String> tagsMap = createTagsMap("@£!", "value1", "key2", "value2");
        Tag[] tags = new Tag[] {new Tag("key2", "value2")};
        MBeanMetric.Identifier id = new MBeanMetric.Identifier(MBeanMetric.Scope.APPLICATION, "foo", tagsMap);
        MBeanMetric metric = new MBeanMetricStub(id, "Coherence,type=foo", "test mBean", 1234);

        MpMetricsRegistryAdapter adapter = new MpMetricsRegistryAdapter(vendorRegistry, appRegistry);
        adapter.register(metric);

        ArgumentCaptor<Metadata> metadataArgument = ArgumentCaptor.forClass(Metadata.class);
        ArgumentCaptor<Gauge<Object>> gaugeArgument = ArgumentCaptor.forClass(Gauge.class);
        ArgumentCaptor<Tag> tagsArgument = ArgumentCaptor.forClass(Tag.class);

        verify(appRegistry).register(metadataArgument.capture(), gaugeArgument.capture(), tagsArgument.capture());
        verifyNoMoreInteractions(vendorRegistry);

        Metadata metadata = metadataArgument.getValue();
        assertThat(metadata, is(notNullValue()));
        assertThat(metadata.getName(), is(metric.getName()));
        assertThat(metadata.getDescription().isPresent(), is(true));
        assertThat(metadata.getDescription().get(), is(metric.getDescription()));
        assertThat(metadata.getTypeRaw(), is(MetricType.GAUGE));

        Gauge<Object> gauge = gaugeArgument.getValue();
        assertThat(gauge, is(notNullValue()));
        assertThat(gauge.getValue(), is(metric.getValue()));

        List<Tag> tagList = tagsArgument.getAllValues();
        assertThat(tagList, containsInAnyOrder(tags));
        }

    @Test
    void shouldRemoveApplicationMetric()
        {
        MetricRegistry vendorRegistry = mock(MetricRegistry.class);
        MetricRegistry appRegistry = mock(MetricRegistry.class);
        Map<String, String> tagsMap = createTagsMap("key1", "value1", "key2", "value2");
        MBeanMetric.Identifier id = new MBeanMetric.Identifier(MBeanMetric.Scope.APPLICATION, "foo", tagsMap);
        Tag[] tags = createTags(tagsMap);
        MetricID metricID = new MetricID(id.getName(), tags);
        Gauge<?> gauge = mock(Gauge.class);

        when(vendorRegistry.getGauges()).thenReturn(Collections.emptySortedMap());
        when(appRegistry.getGauges()).thenReturn(new TreeMap<>(Collections.singletonMap(metricID, gauge)));

        MpMetricsRegistryAdapter adapter = new MpMetricsRegistryAdapter(vendorRegistry, appRegistry);
        adapter.remove(id);

        verify(appRegistry).getGauges();
        verify(appRegistry).remove(metricID);
        verifyNoMoreInteractions(appRegistry);
        verifyNoMoreInteractions(vendorRegistry);
        }

    @Test
    void shouldNotRemoveNonExistentApplicationMetric()
        {
        MetricRegistry vendorRegistry = mock(MetricRegistry.class);
        MetricRegistry appRegistry = mock(MetricRegistry.class);
        Map<String, String> tagsMap = createTagsMap("key1", "value1", "key2", "value2");
        MBeanMetric.Identifier id = new MBeanMetric.Identifier(MBeanMetric.Scope.APPLICATION, "foo", tagsMap);
        Tag[] tags = createTags(tagsMap);
        MetricID metricID = new MetricID(id.getName(), tags);
        Gauge<?> gauge = mock(Gauge.class);

        when(appRegistry.getGauges()).thenReturn(Collections.emptySortedMap());
        when(vendorRegistry.getGauges()).thenReturn(new TreeMap<>(Collections.singletonMap(metricID, gauge)));

        MpMetricsRegistryAdapter adapter = new MpMetricsRegistryAdapter(vendorRegistry, appRegistry);
        adapter.remove(id);

        verify(appRegistry).getGauges();
        verifyNoMoreInteractions(appRegistry);
        verifyNoMoreInteractions(vendorRegistry);
        }

    @Test
    void shouldNotRegisterBaseMetric()
        {
        MetricRegistry vendorRegistry = mock(MetricRegistry.class);
        MetricRegistry appRegistry = mock(MetricRegistry.class);
        Map<String, String> tagsMap = createTagsMap("key1", "value1", "key2", "value2");
        MBeanMetric.Identifier id = new MBeanMetric.Identifier(MBeanMetric.Scope.BASE, "foo", tagsMap);
        MBeanMetric metric = new MBeanMetricStub(id, "Coherence,type=foo", "test mBean", 1234);

        MpMetricsRegistryAdapter adapter = new MpMetricsRegistryAdapter(vendorRegistry, appRegistry);
        adapter.register(metric);

        verifyNoMoreInteractions(vendorRegistry);
        verifyNoMoreInteractions(appRegistry);
        }

    @Test
    void shouldNotRemoveBaseMetric()
        {
        MetricRegistry vendorRegistry = mock(MetricRegistry.class);
        MetricRegistry appRegistry = mock(MetricRegistry.class);
        Map<String, String> tagsMap = createTagsMap("key1", "value1", "key2", "value2");
        MBeanMetric.Identifier id = new MBeanMetric.Identifier(MBeanMetric.Scope.BASE, "foo", tagsMap);

        MpMetricsRegistryAdapter adapter = new MpMetricsRegistryAdapter(vendorRegistry, appRegistry);
        adapter.remove(id);

        verifyNoMoreInteractions(vendorRegistry);
        verifyNoMoreInteractions(appRegistry);
        }

    private Tag[] createTags(Map<String, String> map)
        {
        return map.entrySet()
                .stream()
                .map(e -> new Tag(e.getKey(), e.getValue()))
                .toArray(Tag[]::new);
        }

    private Map<String, String> createTagsMap(String... namesAndValues)
        {
        if (namesAndValues.length % 2 != 0)
            {
            throw new IllegalArgumentException("namesAnValues length must be an even number");
            }

        Map<String, String> tags = new HashMap<>();
        for (int i = 0; i < namesAndValues.length; i++)
            {
            String key = namesAndValues[i++];
            String value = namesAndValues[i];
            tags.put(key, value);
            }
        return tags;
        }

    private static class MBeanMetricStub
            extends BaseMBeanMetric
            implements MBeanMetric
        {

        private Object value;

        private MBeanMetricStub(Identifier identifier, String sMBeanName, String sDescription, Object value)
            {
            super(identifier, sMBeanName, sDescription);
            this.value = value;
            }

        @Override
        public Object getValue()
            {
            return value;
            }
        }
    }
