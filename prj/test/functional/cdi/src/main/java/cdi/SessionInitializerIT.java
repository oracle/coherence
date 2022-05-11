/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package cdi;

import com.oracle.coherence.cdi.CoherenceExtension;
import com.oracle.coherence.cdi.CoherenceProducer;
import com.oracle.coherence.cdi.ConfigUri;
import com.oracle.coherence.cdi.ExtractorProducer;
import com.oracle.coherence.cdi.FilterProducer;
import com.oracle.coherence.cdi.NamedCacheProducer;
import com.oracle.coherence.cdi.Scope;
import com.oracle.coherence.cdi.SessionInitializer;
import com.oracle.coherence.cdi.SessionName;
import com.oracle.coherence.cdi.SessionProducer;
import com.tangosol.net.Coherence;
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
 * Integration test for the {@link SessionInitializer} using the Weld JUnit
 * extension.
 *
 * @author Aleks Seovic  2020.04.03
*/
@ExtendWith(WeldJunit5Extension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SessionInitializerIT
    {
    @WeldSetup
    private final WeldInitiator weld = WeldInitiator.of(WeldInitiator.createWeld()
                                                          .addExtension(new CoherenceExtension())
                                                          .addBeanClass(CoherenceProducer.class)
                                                          .addBeanClass(TestServerCoherenceProducer.class)
                                                          .addBeanClass(NamedCacheProducer.class)
                                                          .addBeanClass(SessionProducer.class)
                                                          .addBeanClass(FilterProducer.class)
                                                          .addBeanClass(ExtractorProducer.class)
                                                          .addBeanClass(EventsSession.class)
                                                          .addBeanClass(TestSession.class));

    @ApplicationScoped
    @Named("events")
    @ConfigUri("cdi-events-config.xml")
    private static class EventsSession
            implements SessionInitializer
        {}

    @ApplicationScoped
    @Named("test")
    @Scope("test")
    private static class TestSession
            implements SessionInitializer
        {}

    @Inject
    @SessionName("events")
    private NamedCache<String, String> people;

    @Inject
    @SessionName("test")
    private NamedMap<String, String> test;

    @Test
    void shouldUseInjectableConfig()
        {
        assertThat(people.getService().getBackingMapManager().getCacheFactory().getScopeName(), is(Coherence.DEFAULT_SCOPE));
        assertThat(people.getService().getInfo().getServiceName(), is("People"));
        }

    @Test
    void shouldUseDefaultConfig()
        {
        assertThat(test.getService().getBackingMapManager().getCacheFactory().getScopeName(), is("test"));
        assertThat(test.getService().getInfo().getServiceName(), is("test:PartitionedCache"));
        }
    }
