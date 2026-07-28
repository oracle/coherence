/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
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
import io.micrometer.core.instrument.Tag;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

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

        // micrometer registers meters by name and tags, so multiple metric holders can collapse
        // to one meter; compare the distinct meter identities instead of the raw counts
        Set<String> setExpected = metrics.values().stream()
                .map(CoherenceMicrometerGlobalIT::meterKey)
                .collect(Collectors.toSet());
        Set<String> setActual = meters.stream()
                .map(meter -> meterKey(meter.getId()))
                .collect(Collectors.toSet());

        assertThat(setActual, is(setExpected));
        }

    private static String meterKey(CoherenceMicrometerMetrics.Holder holder)
        {
        return meterKey(holder.getName(), holder.getTags());
        }

    private static String meterKey(Meter.Id id)
        {
        return meterKey(id.getName(), id.getTags());
        }

    private static String meterKey(String sName, Iterable<Tag> tags)
        {
        SortedSet<String> setTags = new TreeSet<>();
        tags.forEach(tag -> setTags.add(tag.getKey() + '=' + tag.getValue()));
        return sName + setTags;
        }
    }
