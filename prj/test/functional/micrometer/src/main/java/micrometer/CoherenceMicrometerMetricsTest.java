/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package micrometer;

import com.oracle.coherence.micrometer.CoherenceMicrometerMetrics;
import com.tangosol.net.metrics.MBeanMetric;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Unit test for {@link CoherenceMicrometerMetrics}.
 *
 * @author Jonathan Knight  2020.10.08
 */
public class CoherenceMicrometerMetricsTest
    {
    @Test
    public void shouldBindRegistryAndRegisterNewMetric()
        {
        String                 sName      = "coherence.test";
        Map<String, String>    mapTag     = Collections.singletonMap("foo", "bar");
        Long                   nValue     = 19L;
        String                 sDescr     = "test metric";
        MBeanMetric.Identifier identifier = new MBeanMetric.Identifier(MBeanMetric.Scope.VENDOR, sName, mapTag);
        MBeanMetric            metric     = new MetricStub(identifier, sDescr, nValue);
        Meter.Id               id         = new Meter.Id(sName, Tags.of(Tag.of("foo", "bar")), null, sDescr, Meter.Type.GAUGE);
        MeterRegistry          registry   = new SimpleMeterRegistry();

        // bind registry
        CoherenceMicrometerMetrics.INSTANCE.bindTo(registry);
        
        // register the Coherence metric
        CoherenceMicrometerMetrics.INSTANCE.register(metric);

        // should not have registered in Global registry
        assertThat(Metrics.globalRegistry.find(sName).gauge(), is(nullValue()));

        // should have registered metric in registry
        Gauge gauge = registry.find(sName).gauge();
        assertThat(gauge, is(notNullValue()));
        assertThat(gauge.getId(), is(id));
        assertThat(gauge.value(), is(nValue.doubleValue()));
        }

    @Test
    public void shouldBindRegistryAndRegisterExistingMetric()
        {
        String                 sName      = "coherence.test";
        Map<String, String>    mapTag     = Collections.singletonMap("foo", "bar");
        Long                   nValue     = 19L;
        String                 sDescr     = "test metric";
        MBeanMetric.Identifier identifier = new MBeanMetric.Identifier(MBeanMetric.Scope.VENDOR, sName, mapTag);
        MBeanMetric            metric     = new MetricStub(identifier, sDescr, nValue);
        Meter.Id               id         = new Meter.Id(sName, Tags.of(Tag.of("foo", "bar")), null, sDescr, Meter.Type.GAUGE);
        MeterRegistry          registry   = new SimpleMeterRegistry();

        // register the Coherence metric
        CoherenceMicrometerMetrics.INSTANCE.register(metric);

        // bind registry
        CoherenceMicrometerMetrics.INSTANCE.bindTo(registry);

        // should not have registered in Global registry
        assertThat(Metrics.globalRegistry.find(sName).gauge(), is(nullValue()));

        // should have registered metric in registry
        Gauge gauge = registry.find(sName).gauge();
        assertThat(gauge, is(notNullValue()));
        assertThat(gauge.getId(), is(id));
        assertThat(gauge.value(), is(nValue.doubleValue()));
        }

    @Test
    public void shouldBindMultipleRegistries()
        {
        String                 sName       = "coherence.test";
        Map<String, String>    mapTag      = Collections.singletonMap("foo", "bar");
        Long                   nValue      = 19L;
        String                 sDescr      = "test metric";
        MBeanMetric.Identifier identifier  = new MBeanMetric.Identifier(MBeanMetric.Scope.VENDOR, sName, mapTag);
        MBeanMetric            metric      = new MetricStub(identifier, sDescr, nValue);
        Meter.Id               id          = new Meter.Id(sName, Tags.of(Tag.of("foo", "bar")), null, sDescr, Meter.Type.GAUGE);
        MeterRegistry          registryOne = new SimpleMeterRegistry();
        MeterRegistry          registryTwo = new SimpleMeterRegistry();

        // bind registry
        CoherenceMicrometerMetrics.INSTANCE.bindTo(registryOne);
        CoherenceMicrometerMetrics.INSTANCE.bindTo(registryTwo);

        // register the Coherence metric
        CoherenceMicrometerMetrics.INSTANCE.register(metric);

        // should have registered metric in registry one
        Gauge gauge = registryOne.find(sName).gauge();
        assertThat(gauge, is(notNullValue()));
        assertThat(gauge.getId(), is(id));
        assertThat(gauge.value(), is(nValue.doubleValue()));

        // should have registered metric in registry one
        gauge = registryTwo.find(sName).gauge();
        assertThat(gauge, is(notNullValue()));
        assertThat(gauge.getId(), is(id));
        assertThat(gauge.value(), is(nValue.doubleValue()));
        }


    // ----- inner class: MetricStub ----------------------------------------

    private static class MetricStub
            implements MBeanMetric
        {
        public MetricStub(Identifier identifier, String sDescription, Object oValue)
            {
            f_identifier  = identifier;
            f_sDescription = sDescription;
            f_oValue      = oValue;
            }

        @Override
        public Identifier getIdentifier()
            {
            return f_identifier;
            }

        @Override
        public String getMBeanName()
            {
            return "";
            }

        @Override
        public String getDescription()
            {
            return f_sDescription;
            }

        @Override
        public Object getValue()
            {
            return f_oValue;
            }

        // ----- data members -----------------------------------------------

        private final Identifier f_identifier;
        private final String f_sDescription;
        private final Object f_oValue;
        }

    }
