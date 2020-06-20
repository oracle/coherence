/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.cdi.server;

import com.oracle.coherence.cdi.CoherenceExtension;
import com.oracle.coherence.cdi.ConfigUri;
import com.oracle.coherence.cdi.ExtractorProducer;
import com.oracle.coherence.cdi.FilterProducer;
import com.oracle.coherence.cdi.Scope;

import com.oracle.coherence.cdi.server.data.Account;

import com.oracle.coherence.cdi.server.data.Person;
import com.tangosol.net.NamedCache;

import com.tangosol.net.NamedMap;
import javax.enterprise.context.ApplicationScoped;

import javax.inject.Inject;
import javax.inject.Named;

import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldSetup;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Integration test for the {@link ScopeInitializer} using the Weld JUnit
 * extension.
 *
 * @author Aleks Seovic  2020.04.03
*/
@ExtendWith(WeldJunit5Extension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ScopeInitializerIT
    {
    @WeldSetup
    private final WeldInitiator weld = WeldInitiator.of(WeldInitiator.createWeld()
                                                          .addExtension(new CoherenceExtension())
                                                          .addExtension(new CoherenceServerExtension())
                                                          .addBeanClass(CacheFactoryUriResolver.Default.class)
                                                          .addBeanClass(ConfigurableCacheFactoryProducer.class)
                                                          .addBeanClass(NamedCacheProducer.class)
                                                          .addBeanClass(FilterProducer.class)
                                                          .addBeanClass(ExtractorProducer.class)
                                                          .addBeanClass(EventsScope.class)
                                                          .addBeanClass(TestScope.class));

    @ApplicationScoped
    @Named("events")
    @ConfigUri("cdi-events-config.xml")
    private static class EventsScope
            implements ScopeInitializer
        {}

    @ApplicationScoped
    @Named("test")
    private static class TestScope
            implements ScopeInitializer
        {}

    @Inject
    @Scope("events")
    private NamedCache<String, Person> people;

    @Inject
    @Scope("test")
    private NamedMap<String, String> test;

    @Test
    void shouldUseInjectableConfig()
        {
        assertThat(people.getService().getBackingMapManager().getCacheFactory().getScopeName(), is("events"));
        assertThat(people.getService().getInfo().getServiceName(), is("events:People"));
        }

    @Test
    void shouldUseDefaultConfig()
        {
        assertThat(test.getService().getBackingMapManager().getCacheFactory().getScopeName(), is("test"));
        assertThat(test.getService().getInfo().getServiceName(), is("test:StorageService"));
        }
    }
