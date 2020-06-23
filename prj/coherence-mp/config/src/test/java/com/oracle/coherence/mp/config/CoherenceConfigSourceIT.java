/*
 * Copyright (c) 2019, 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.mp.config;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.coherence.cdi.CoherenceExtension;
import com.oracle.coherence.cdi.CdiMapListenerManager;
import com.oracle.coherence.cdi.ExtractorProducer;
import com.oracle.coherence.cdi.FilterProducer;
import com.oracle.coherence.cdi.MapEventTransformerProducer;
import com.oracle.coherence.cdi.server.CoherenceServerExtension;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.eclipse.microprofile.config.spi.ConfigSource;

import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldSetup;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.hamcrest.MatcherAssert.assertThat;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;


/**
 * Unit tests for {@link CoherenceConfigSource}.
 *
 * @author Aleks Seovic  2019.10.12
 */
@ExtendWith(WeldJunit5Extension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CoherenceConfigSourceIT
    {
    @WeldSetup
    private final WeldInitiator weld = WeldInitiator.of(WeldInitiator.createWeld()
                                                          .addExtension(new CoherenceExtension())
                                                          .addExtension(new CoherenceServerExtension())
                                                          .addBeanClass(CdiMapListenerManager.class)
                                                          .addBeanClass(FilterProducer.class)
                                                          .addBeanClass(ExtractorProducer.class)
                                                          .addBeanClass(MapEventTransformerProducer.class)
                                                          .addBeanClass(TestObserver.class)
                                                          .addBeanClass(CoherenceConfigSource.class));

    @BeforeAll
    static void setup()
        {
        System.setProperty("coherence.member", "sysprop01");
        System.setProperty("config.value", "sysprop");
        }

    private static Config getConfig()
        {
        ConfigProviderResolver resolver = ConfigProviderResolver.instance();
        ConfigBuilder builder = resolver.getBuilder();
        return builder
                .addDefaultSources()
                .addDiscoveredSources()
                .build();
        }

    private static CoherenceConfigSource getCoherenceSource(Config config)
        {
        for (ConfigSource source : config.getConfigSources())
            {
            if (source instanceof CoherenceConfigSource)
                {
                return (CoherenceConfigSource) source;
                }
            }
        throw new IllegalStateException("CoherenceConfigSource is not in a list of sources");
        }

    @Inject
    private TestObserver observer;

    private Config config;
    private CoherenceConfigSource source;

    @BeforeEach
    void clearSystemProperties()
        {
        config = getConfig();
        source = getCoherenceSource(config);
        source.getConfigMap().truncate();
        }

    @Test
    void testDefaults()
        {
        source.setValue("config.value", "value");

        assertThat(source.getProperties().entrySet(), hasSize(1));
        assertThat(source.getProperties(), hasKey("config.value"));
        assertThat(source.getPropertyNames(), hasSize(1));
        assertThat(source.getPropertyNames(), hasItem("config.value"));
        assertThat(source.getValue("config.value"), is("value"));
        assertThat(source.getOrdinal(), is(500));
        assertThat(source.getName(), is("CoherenceConfigSource"));
        }

    @Test
    void testDefaultPriority()
        {
        source.setValue("config.value", "cache");

        assertThat(config.getValue("coherence.cluster", String.class), is("test"));
        assertThat(config.getValue("coherence.role", String.class), is("proxy"));
        assertThat(config.getValue("coherence.member", String.class), is("sysprop01"));
        assertThat(config.getValue("coherence.distributed.localstorage", String.class), is("true"));
        assertThat(config.getValue("config.value", String.class), is("cache"));
        }

    @Test
    void testLowPriority()
        {
        System.setProperty("coherence.config.ordinal", "100");

        Config config = getConfig();
        assertThat(config.getValue("config.value", String.class), is("sysprop"));
        }

    @Test
    void testChangeNotification()
        {
        source.setValue("config.value", "one");
        Eventually.assertDeferred(() -> observer.getLatestValue(), is("one"));

        source.setValue("config.value", "two");
        Eventually.assertDeferred(() -> observer.getLatestValue(), is("two"));

        source.getConfigMap().remove("config.value");
        Eventually.assertDeferred(() -> observer.getLatestValue(), is(nullValue()));
        }

    @ApplicationScoped
    static class TestObserver
        {
        volatile String latestValue;

        public String getLatestValue()
            {
            return latestValue;
            }

        void observer(@Observes ConfigPropertyChanged event)
            {
            System.out.println(event);
            latestValue = event.getValue();
            }
        }
    }
