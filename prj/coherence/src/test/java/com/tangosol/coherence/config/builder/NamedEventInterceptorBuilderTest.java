/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.builder;

import com.tangosol.coherence.config.ResolvableParameterList;

import com.tangosol.config.expression.NullParameterResolver;
import com.tangosol.config.expression.Parameter;
import com.tangosol.config.expression.ParameterResolver;

import com.tangosol.net.BackingMapContext;
import com.tangosol.net.BackingMapManagerContext;
import com.tangosol.net.CacheService;
import com.tangosol.net.PartitionedService;
import com.tangosol.net.ServiceInfo;
import com.tangosol.net.events.annotation.EntryEvents;
import com.tangosol.net.events.annotation.TransferEvents;
import com.tangosol.net.events.internal.NamedEventInterceptor;
import com.tangosol.net.events.partition.PartitionedServiceDispatcher;
import com.tangosol.net.events.partition.TransferEvent;
import com.tangosol.net.events.partition.cache.EntryEvent;
import com.tangosol.net.events.partition.cache.EntryEvent.Type;
import com.tangosol.net.events.Event;
import com.tangosol.net.events.EventInterceptor;
import com.tangosol.net.events.annotation.Interceptor;

import com.tangosol.net.events.partition.cache.PartitionedCacheDispatcher;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.text.ParseException;

import java.util.Arrays;
import java.util.HashSet;

/**
 * {@link NamedEventInterceptorBuilderTest} provides unit tests for {@link NamedEventInterceptorBuilder}s.
 *
 * @author hr  2011.12.06
 *
 * @since 12.1.2.0
 */
public class NamedEventInterceptorBuilderTest
    {
    /**
     * Ensure that we can instantiate a minimal interceptor.
     */
    @Test
    public void testMinimal()
        {
        NamedEventInterceptorBuilder bldr     = new NamedEventInterceptorBuilder();
        ParameterResolver            resolver = new NullParameterResolver();

        bldr.setName("AristotolesInterceptor");
        bldr.setCustomBuilder(new InstanceBuilder<EventInterceptor>(AristotlesInterceptor.class));

        EventInterceptor eventInterceptor = bldr.realize(resolver, null, null);

        assertThat(eventInterceptor, allOf(notNullValue(), instanceOf(NamedEventInterceptor.class)));

        NamedEventInterceptor incptrNamed = (NamedEventInterceptor) eventInterceptor;

        assertFalse(incptrNamed.isFirst());
        assertNull(incptrNamed.getEventTypes());
        }

    /**
     * Method description
     */
    @Test
    public void testSimpleInterceptor()
        {
        NamedEventInterceptorBuilder bldr     = new NamedEventInterceptorBuilder();
        ParameterResolver       resolver = new NullParameterResolver();

        bldr.setName("AristotlesSimpleInterceptor");
        bldr.setCustomBuilder(new InstanceBuilder<EventInterceptor>(AristotlesSimpleInterceptor.class));

        EventInterceptor eventInterceptor = bldr.realize(resolver, null, null);

        assertThat(eventInterceptor, notNullValue());
        }

    /**
     * Method description
     */
    @Test
    public void testAnnotatedInterceptor()
        {
        NamedEventInterceptorBuilder bldr     = new NamedEventInterceptorBuilder();
        ParameterResolver            resolver = new NullParameterResolver();

        bldr.setName("AnnotatedInterceptor");
        bldr.setCustomBuilder(new InstanceBuilder<EventInterceptor>(AnnotatedInterceptor.class, "bonjour"));

        NamedEventInterceptor incptrNamed = bldr.realize(resolver, null, null);

        assertThat(incptrNamed.getInterceptor(), allOf(notNullValue(), instanceOf(AnnotatedInterceptor.class)));

        AnnotatedInterceptor interceptor = (AnnotatedInterceptor) incptrNamed.getInterceptor();

        assertTrue(incptrNamed.isFirst());
        assertEquals(new HashSet(Arrays.asList(EntryEvent.Type.INSERTING, EntryEvent.Type.INSERTED,
            TransferEvent.Type.DEPARTING)), incptrNamed.getEventTypes());

        // should be the name we specify and not the name on the annotation
        assertThat(bldr.getName(), is("AnnotatedInterceptor"));
        assertThat(interceptor.m_sSomeString, is("bonjour"));
        }

    /**
     * Method description
     *
     * @throws ParseException
     */
    @Test
    public void testServiceAndCacheName()
            throws ParseException
        {
        BackingMapManagerContext     bmmc = mock(BackingMapManagerContext.class);
        BackingMapContext            bmc  = mock(BackingMapContext.class);
        PartitionedCacheDispatcher   bmd  = mock(PartitionedCacheDispatcher.class);
        PartitionedServiceDispatcher psd  = mock(PartitionedServiceDispatcher.class);

        // mimic call stacks
        when(psd.getServiceName()).thenReturn("PartitionedService");
        when(bmd.getServiceName()).thenReturn("PartitionedService");
        when(bmd.getCacheName()).thenReturn("dist-bonjour");

        NamedEventInterceptorBuilder bldr = new NamedEventInterceptorBuilder();

        bldr.setName("AristotlesQueryInterceptor");
        bldr.setOrder(Interceptor.Order.HIGH);
        bldr.setCustomBuilder(new InstanceBuilder<EventInterceptor>(AristotlesQueryInterceptor.class));

        ResolvableParameterList resolver = new ResolvableParameterList();

        resolver.add(new Parameter("cache-name", "dist-*"));
        resolver.add(new Parameter("service-name", "PartitionedService"));

        NamedEventInterceptor incptrNamed = bldr.realize(resolver, null, null);

        assertThat(incptrNamed.getInterceptor(), allOf(notNullValue(), instanceOf(AristotlesQueryInterceptor.class)));

        assertTrue(incptrNamed.isFirst());
        assertNull(incptrNamed.getEventTypes());

        // should be the name we specify and not the name on the annotation
        assertThat(bldr.getName(), is("AristotlesQueryInterceptor"));

        // verify service-name filtering
        assertTrue(incptrNamed.isAcceptable(bmd));
        assertTrue(incptrNamed.isAcceptable(psd));
        when(bmd.getServiceName()).thenReturn("WrongPartService");
        when(psd.getServiceName()).thenReturn("WrongPartService");
        assertFalse(incptrNamed.isAcceptable(bmd));
        assertFalse(incptrNamed.isAcceptable(psd));

        // verify cache-name filtering
        when(bmd.getServiceName()).thenReturn("PartitionedService");
        when(psd.getServiceName()).thenReturn("PartitionedService");
        when(bmd.getCacheName()).thenReturn("repl-bonjour");
        assertFalse(incptrNamed.isAcceptable(bmd));
        assertTrue(incptrNamed.isAcceptable(psd));
        }

    // ----- inner class: AnnotatedInterceptor ------------------------------

    /**
     * Class description
     *
     * @version        Enter version here..., 12/02/14
     * @author         Enter your name here...
     */
    @Interceptor(
        identifier     = "sIdentifier",
        order          = Interceptor.Order.HIGH
        )
    @EntryEvents({Type.INSERTING, Type.INSERTED})
    @TransferEvents(TransferEvent.Type.DEPARTING)
    public static class AnnotatedInterceptor
            implements EventInterceptor<EntryEvent<?, ?>>
        {

        public AnnotatedInterceptor(String sSomeString)
            {
            m_sSomeString = sSomeString;
            }

        /**
         * {@inheritDoc}
         */
        public void onEvent(EntryEvent<?, ?> event)
            {
            throw new UnsupportedOperationException();
            }

        protected String m_sSomeString;
        }

    // ----- inner class: AristotlesInterceptor -----------------------------

    /**
     * Class description
     *
     * @version        Enter version here..., 12/02/14
     * @author         Enter your name here...
     */
    public static class AristotlesInterceptor
            implements EventInterceptor
        {

        // ----- EventInterceptor members -----------------------------------

        /**
         * {@inheritDoc}
         */
        public void onEvent(Event event)
            {
            throw new UnsupportedOperationException();
            }
        }

    // ----- inner class: AristotlesQueryInterceptor ------------------------

    /**
     * Class description
     *
     * @version        Enter version here..., 12/02/14
     * @author         Enter your name here...
     */
    public static class AristotlesQueryInterceptor
            implements EventInterceptor
        {
        // ----- EventInterceptor members -----------------------------------

        /**
         * {@inheritDoc}
         */
        public void onEvent(Event event)
            {
            throw new UnsupportedOperationException();
            }
        }

    // ----- inner class: TestInterceptor -----------------------------------

    /**
     * Class description
     *
     * @version        Enter version here..., 12/02/14
     * @author         Enter your name here...
     */
    public static class AristotlesSimpleInterceptor
            implements EventInterceptor
        {
        /**
         * {@inheritDoc}
         */
        public void onEvent(Event event)
            {
            throw new UnsupportedOperationException();
            }
        }
    }
