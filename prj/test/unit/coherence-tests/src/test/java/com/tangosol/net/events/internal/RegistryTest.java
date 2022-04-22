/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.events.internal;

import com.tangosol.net.BackingMapContext;
import com.tangosol.net.BackingMapManagerContext;
import com.tangosol.net.CacheService;
import com.tangosol.net.PartitionedService;
import com.tangosol.net.ServiceInfo;

import com.tangosol.net.events.Event;
import com.tangosol.net.events.EventDispatcher;
import com.tangosol.net.events.EventDispatcher.InterceptorRegistrationEvent;
import com.tangosol.net.events.EventDispatcherAwareInterceptor;
import com.tangosol.net.events.EventInterceptor;
import com.tangosol.net.events.NamedEventInterceptor;
import com.tangosol.net.events.annotation.Interceptor.Order;
import com.tangosol.net.events.internal.AbstractEventDispatcher.DispatcherInterceptorEvent;

import com.tangosol.util.RegistrationBehavior;

import org.hamcrest.Matchers;

import org.junit.Test;

import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.Matchers.startsWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test the public registry methods.
 *
 * @author nsa 2011.08.09
 * @since 3.7.1
 */
public class RegistryTest
    {
    /**
     * Test that we can simply register and unregister an interceptor
     */
    @Test
    public void testRegisterUnregisterInterceptor()
        {
        EventInterceptor mockInterceptor = mock(EventInterceptor.class);
        Registry registry = new Registry();
        String   sKey     = "testInterceptor";

        registry.registerEventInterceptor(sKey, mockInterceptor, null);
        assertEquals(registry.getEventInterceptor(sKey), mockInterceptor);

        registry.unregisterEventInterceptor(sKey);
        assertEquals(registry.getEventInterceptor(sKey), null);
        }

    /**
     * Test that registering an interceptor twice will throw an exception
     */
    @Test(expected=IllegalArgumentException.class)
    public void testRegisterTwice()
        {
        EventInterceptor mockInterceptor = mock(EventInterceptor.class);
        Registry registry = new Registry();
        String   sKey     = "testInterceptor";

        registry.registerEventInterceptor(sKey, mockInterceptor, null);
        registry.registerEventInterceptor(sKey, mockInterceptor, null);
        }

    /**
     * Test that we can re-register an interceptor if we've unregistered it first.
     */
    @Test
    public void testRegisterUnregisterRegister()
        {
        EventInterceptor mockInterceptor = mock(EventInterceptor.class);
        Registry registry = new Registry();
        String   sKey     = "testInterceptor";

        registry.registerEventInterceptor(sKey, mockInterceptor, null);
        registry.unregisterEventInterceptor(sKey);
        registry.registerEventInterceptor(sKey, mockInterceptor, null);

        assertEquals(registry.getEventInterceptor(sKey), mockInterceptor);

        // identity based unregistration
        registry.unregisterEventInterceptor(sKey);
        sKey = registry.registerEventInterceptor(mockInterceptor);

        assertEquals(mockInterceptor.getClass().getName(), sKey);
        assertEquals(mockInterceptor, registry.getEventInterceptor(sKey));
        registry.unregisterEventInterceptor(sKey);
        assertNull(registry.getEventInterceptor(sKey));
        }

    /**
     * Test registration and uregistration of an interceptor with a dispatcher registered
     */
    @Test
    public void testRegisterUnRegisterInterceptorWithDispatcher()
        {
        TestInterceptor           interceptor = new TestInterceptor();
        BackingMapContext         mockCtx     = mockBMC("DistributedCache", "foo");
        AbstractEventDispatcher   dispatcher  = new StorageDispatcher(mockCtx);
        Registry                  registry    = new Registry();
        String                    sKey        = "testInterceptor";

        registry.registerEventDispatcher(dispatcher);
        registry.registerEventInterceptor(sKey, interceptor, null);

        assertEquals(registry.getEventInterceptor(sKey), interceptor);

        Map      mapInterceptors = dispatcher.getInterceptorMap();
        Iterator iter            = mapInterceptors.values().iterator();

        while (iter.hasNext())
            {
            List<NamedEventInterceptor> interceptorList = (List<NamedEventInterceptor>) iter.next();
            NamedEventInterceptor namedInterceptor = interceptorList.get(0);
            assertEquals(interceptor, namedInterceptor.getInterceptor());
            }

        registry.unregisterEventInterceptor(sKey);
        assertEquals(mapInterceptors.size(), 0);
        }

    /**
     * Test registering two interceptors with dispatchers already registered using the default ordering
     */
    @Test
    public void testRegisterUnregisterInterceptorsDefaultOrderWithDispatcher()
        {
        TestInterceptor           interceptor  = new TestInterceptor();
        TestInterceptor           interceptor2 = new TestInterceptor();
        BackingMapContext         mockCtx      = mockBMC("DistributedCache", "foo");
        AbstractEventDispatcher   dispatcher   = new StorageDispatcher(mockCtx);
        Registry                  registry     = new Registry();
        String                    sKey         = "testInterceptor";
        String                    sKey2        = "testInterceptor2";


        registry.registerEventDispatcher(dispatcher);
        registry.registerEventInterceptor(sKey, interceptor, null);
        registry.registerEventInterceptor(sKey2, interceptor2, null);

        assertEquals(registry.getEventInterceptor(sKey), interceptor);

        Map      mapInterceptors = dispatcher.getInterceptorMap();
        Iterator iter            = mapInterceptors.values().iterator();

        while (iter.hasNext())
            {
            List<NamedEventInterceptor> interceptorList = (List<NamedEventInterceptor>) iter.next();
            assertEquals(interceptorList.size(), 2);
            NamedEventInterceptor namedInt = interceptorList.get(0);
            assertEquals(interceptor, namedInt.getInterceptor());
            namedInt = interceptorList.get(1);
            assertEquals(interceptor2, namedInt.getInterceptor());
            }

        registry.unregisterEventInterceptor(sKey);

        iter = mapInterceptors.values().iterator();

        while (iter.hasNext())
            {
            List<NamedEventInterceptor> interceptorList = (List<NamedEventInterceptor>) iter.next();
            assertEquals(interceptorList.size(), 1);
            NamedEventInterceptor namedInt = interceptorList.get(0);
            assertEquals(interceptor2, namedInt.getInterceptor());
            }
        }

    /**
     * Test registering two dispatchers with specific expected ordering of interceptor2 being registered before interceptor1
     */
    @Test
    public void testRegisterInterceptorsSpecialOrderWithDispatcher()
        {
        String                    sKey         = "testInterceptor";
        String                    sKey2        = "testInterceptor2";
        TestInterceptor           interceptor  = new TestInterceptor();
        NamedEventInterceptor     interceptor2 = new NamedEventInterceptor(sKey2, new TestInterceptor(), null, null, Order.HIGH, null);
        BackingMapContext         mockCtx      = mockBMC("DistributedCache", "foo");
        AbstractEventDispatcher   dispatcher   = new StorageDispatcher(mockCtx);
        Registry                  registry     = new Registry();

        registry.registerEventDispatcher(dispatcher);
        registry.registerEventInterceptor(sKey, interceptor, null);
        registry.registerEventInterceptor(sKey2, interceptor2, null);

        assertEquals(registry.getEventInterceptor(sKey), interceptor);

        Map      mapInterceptors = dispatcher.getInterceptorMap();
        Iterator iter            = mapInterceptors.values().iterator();

        while (iter.hasNext())
            {
            List<NamedEventInterceptor> interceptorList = (List<NamedEventInterceptor>) iter.next();
            NamedEventInterceptor namedInt = interceptorList.get(0);
            assertEquals(interceptor2, namedInt);
            namedInt = interceptorList.get(1);
            assertEquals(interceptor, namedInt.getInterceptor());
            }

        registry.unregisterEventInterceptor(sKey);

        iter = mapInterceptors.values().iterator();
        while (iter.hasNext())
            {
            List<NamedEventInterceptor> interceptorList = (List<NamedEventInterceptor>) iter.next();
            assertEquals(interceptorList.size(), 1);
            NamedEventInterceptor namedInt = interceptorList.get(0);
            assertEquals(interceptor2, namedInt);
            }
        }

    /**
     * Test registering the dispatcher after registering an interceptor
     */
    @Test
    public void testDispatcherLast()
        {
        TestInterceptor           interceptor  = new TestInterceptor();
        BackingMapContext         mockCtx      = mockBMC("DistributedCache", "foo");
        AbstractEventDispatcher   dispatcher   = new StorageDispatcher(mockCtx);
        Registry                  registry     = new Registry();
        String                    sKey         = "testInterceptor";

        registry.registerEventInterceptor(sKey, interceptor, null);
        registry.registerEventDispatcher(dispatcher);

        assertEquals(registry.getEventInterceptor(sKey), interceptor);

        Map      mapInterceptors = dispatcher.getInterceptorMap();
        Iterator iter            = mapInterceptors.values().iterator();

        while (iter.hasNext())
            {
            List<NamedEventInterceptor> interceptorList = (List<NamedEventInterceptor>) iter.next();
            NamedEventInterceptor namedInterceptor = interceptorList.get(0);
            assertEquals(interceptor, namedInterceptor.getInterceptor());
            }
        }

    /**
     * Test having multiple interceptors registered first and then registering a dispatcher. Ordering in this case
     * won't be guaranteed unless we specify it which is what we've done for this test.
     */
    @Test
    public void testMultipleInterceptorDispatcherLast()
        {
        TestInterceptor           interceptor  = new TestInterceptor();
        TestInterceptor           interceptor2 = new TestInterceptor();
        BackingMapContext         mockCtx      = mockBMC("DistributedCache", "foo");
        AbstractEventDispatcher   dispatcher   = new StorageDispatcher(mockCtx);
        Registry                  registry     = new Registry();
        String                    sKey         = "testInterceptor";
        String                    sKey2        = "testInterceptor2";

        registry.registerEventInterceptor(sKey, interceptor, null);
        registry.registerEventInterceptor(sKey2, interceptor2, null);
        registry.registerEventDispatcher(dispatcher);

        assertEquals(registry.getEventInterceptor(sKey), interceptor);

        Map      mapInterceptors = dispatcher.getInterceptorMap();
        Iterator iter            = mapInterceptors.values().iterator();

        while (iter.hasNext())
            {
            List<NamedEventInterceptor> interceptorList = (List<NamedEventInterceptor>) iter.next();
            assertEquals(interceptorList.size(), 2);
            NamedEventInterceptor namedInt = interceptorList.get(0);
            assertEquals(interceptor, namedInt.getInterceptor());
            namedInt = interceptorList.get(1);
            assertEquals(interceptor2, namedInt.getInterceptor());
            }
        }

    /**
     * Test interceptor identifier generation works as expected.
     */
    @Test
    public void testRegisterWithoutIdentifier()
        {
        EventInterceptor incptrMock = mock(EventInterceptor.class);
        Registry         registry   = new Registry();

        String sName = registry.registerEventInterceptor(incptrMock);

        assertSame(incptrMock, registry.getEventInterceptor(sName));

        registry.unregisterEventInterceptor(sName);
        assertNull(registry.getEventInterceptor(sName));
        }

    /**
     * Test registering a NamedEventInterceptor works as expected.
     */
    @Test
    public void testRegisterNamedInterceptor()
        {
        String           sName       = "testRegisterNamedInterceptor";
        EventInterceptor incptrMock  = mock(EventInterceptor.class);
        EventInterceptor incptrNamed = new NamedEventInterceptor(sName, incptrMock);
        Registry         registry    = new Registry();

        String sReturnedName = registry.registerEventInterceptor(incptrNamed);
        assertEquals(sName, sReturnedName);
        assertSame(incptrMock, registry.getEventInterceptor(sName));

        registry.unregisterEventInterceptor(sName);
        assertNull(registry.getEventInterceptor(sName));
        }

    /**
     * Test an interceptor that implements EventDispatcherAwareInterceptor
     * is responsible for introducing itself to a EventDispatcher.
     */
    @Test
    public void testEventDispatcherAwareInterceptor()
        {
        EventDispatcherAwareInterceptor incptr = mock(EventDispatcherAwareInterceptor.class);

        BackingMapContext       mockCtx     = mockBMC("DistributedCache", "foo");
        AbstractEventDispatcher dispatcher  = new StorageDispatcher(mockCtx);
        AbstractEventDispatcher dispatcher2 = new StorageDispatcher(mockCtx);
        Registry                registry    = new Registry();

        ArgumentCaptor<String>          captorIdentifier = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<EventDispatcher> captorDispatcher = ArgumentCaptor.forClass(EventDispatcher.class);

        registry.registerEventDispatcher(dispatcher);
        String sKey = registry.registerEventInterceptor(incptr);

        verify(incptr).introduceEventDispatcher(captorIdentifier.capture(), captorDispatcher.capture());

        assertSame(incptr, registry.getEventInterceptor(sKey));
        assertEquals(sKey, captorIdentifier.getValue());
        assertSame(dispatcher, captorDispatcher.getValue());

        registry.registerEventDispatcher(dispatcher2);
        verify(incptr, times(2)).introduceEventDispatcher(captorIdentifier.capture(), captorDispatcher.capture());

        assertEquals(sKey, captorIdentifier.getValue());
        assertSame(dispatcher2, captorDispatcher.getValue());

        registry.unregisterEventInterceptor(sKey);
        assertNull(registry.getEventInterceptor(sKey));
        }

    /**
     * Test providing a cache name and service name that they are honoured
     * in two sequences of calls {regInterceptor, regDispatcher}
     * & {regDispatcher, regInterceptor}.
     */
    @Test
    public void testFilteringInterceptor()
        {
        String                  sServiceName = "DistributedCache";
        String                  sCacheName   = "dist-hi";
        String                  sCacheName2  = "dist-hi-bye";
        EventInterceptor        incptr       = mock(EventInterceptor.class);
        NamedEventInterceptor   incptrNamed  = new NamedEventInterceptor("testFilteringInterceptor",  incptr, sCacheName, sServiceName, Order.LOW, null);
        NamedEventInterceptor   incptrNamed2 = new NamedEventInterceptor("testFilteringInterceptor2", incptr, sCacheName2, sServiceName, Order.LOW, null);
        NamedEventInterceptor   incptrNamed3 = new NamedEventInterceptor(null,  incptr, sCacheName, sServiceName, Order.LOW, null);
        NamedEventInterceptor   incptrNamed4 = new NamedEventInterceptor(null, incptr, sCacheName + "-bye", sServiceName, Order.LOW, null);
        BackingMapContext       mockCtx      = mockBMC(sServiceName, sCacheName);
        AbstractEventDispatcher dispatcher   = new StorageDispatcher(mockCtx);
        Registry                registry     = new Registry();

        assertTrue(dispatcher.getInterceptorMap().isEmpty());

        // cache name | service name scoping is not applied when an identifier
        // is provided
        assertEquals("testFilteringInterceptor", registry.registerEventInterceptor(incptrNamed));
        registry.registerEventDispatcher(dispatcher);

        assertFalse(dispatcher.getInterceptorMap().isEmpty());
        registry.unregisterEventInterceptor(incptrNamed.getRegisteredName());
        assertTrue(dispatcher.getInterceptorMap().isEmpty());

        assertEquals("testFilteringInterceptor2", registry.registerEventInterceptor(incptrNamed2));
        assertTrue(dispatcher.getInterceptorMap().isEmpty());
        registry.unregisterEventInterceptor(incptrNamed2.getRegisteredName());

        // ensure cache name | service name scoping is applied
        String sName = registry.registerEventInterceptor(incptrNamed3);
        assertThat(sName, allOf(containsString(sCacheName), containsString(sServiceName),
                containsString(incptr.getClass().getName())));
        assertFalse(dispatcher.getInterceptorMap().isEmpty());
        registry.unregisterEventInterceptor(sName);
        assertTrue(dispatcher.getInterceptorMap().isEmpty());

        sName = registry.registerEventInterceptor(incptrNamed4);
        assertThat(sName, allOf(containsString(sCacheName2), containsString(sServiceName),
                        containsString(incptr.getClass().getName())));
        assertTrue(dispatcher.getInterceptorMap().isEmpty());
        registry.unregisterEventInterceptor(sName);
        }

    /**
     * Test the allowed registration behavioral options.
     */
    @Test
    public void testRegisterBehavior()
        {
        EventInterceptor incptrMock  = new TestInterceptor();
        EventInterceptor incptrMock2 = new TestInterceptor();
        Registry         registry    = new Registry();
        String           sClassName  = incptrMock.getClass().getName();
        String           sKey        = "foo";

        // we validate using the same key during registration twice throws an
        // exception with a null RegistrationBehavior in testRegisterTwice

        // ensure 2nd interceptor registration is ignored
        assertThat(registry.registerEventInterceptor(incptrMock), is(sClassName));
        String sName2 = registry.registerEventInterceptor(incptrMock2);
        assertThat(sName2, startsWith(sClassName));
        assertThat(registry.getEventInterceptor(sClassName),
                Matchers.<EventInterceptor>allOf(sameInstance(incptrMock), not(sameInstance(incptrMock2))));
        registry.unregisterEventInterceptor(sClassName);
        registry.unregisterEventInterceptor(sName2);

        // ensure 2nd interceptor registration overwrites original
        RegistrationBehavior behavior = RegistrationBehavior.REPLACE;
        assertThat(registry.registerEventInterceptor(incptrMock, behavior), is(sClassName));
        assertThat(registry.registerEventInterceptor(incptrMock2, behavior), is(sClassName));
        assertThat(registry.getEventInterceptor(sClassName),
                Matchers.<EventInterceptor>allOf(sameInstance(incptrMock2), not(sameInstance(incptrMock))));
        registry.unregisterEventInterceptor(sClassName);

        assertThat(registry.registerEventInterceptor(sKey, incptrMock, behavior), is(sKey));
        assertThat(registry.registerEventInterceptor(sKey, incptrMock2, behavior), is(sKey));
        assertThat(registry.getEventInterceptor(sKey),
                Matchers.<EventInterceptor>allOf(sameInstance(incptrMock2), not(sameInstance(incptrMock))));
        registry.unregisterEventInterceptor(sKey);

        // ensure 2nd interceptor registration creates a unique identifier
        behavior = RegistrationBehavior.ALWAYS;
        assertThat(registry.registerEventInterceptor(incptrMock, behavior), is(sClassName));
        String sIncptrName = registry.registerEventInterceptor(incptrMock2, behavior);
        assertThat(sIncptrName, Matchers.<String>allOf(not(is(sClassName)), startsWith(sClassName)));
        assertThat(registry.getEventInterceptor(sClassName), sameInstance(incptrMock));
        assertThat(registry.getEventInterceptor(sIncptrName), sameInstance(incptrMock2));
        registry.unregisterEventInterceptor(sClassName);
        registry.unregisterEventInterceptor(sIncptrName);

        assertThat(registry.registerEventInterceptor(sKey, incptrMock, behavior), is(sKey));
        sIncptrName = registry.registerEventInterceptor(sKey, incptrMock2, behavior);
        assertThat(sIncptrName, Matchers.<String>allOf(not(is(sKey)), startsWith(sKey)));
        assertThat(registry.getEventInterceptor(sKey), sameInstance(incptrMock));
        assertThat(registry.getEventInterceptor(sIncptrName), sameInstance(incptrMock2));
        registry.unregisterEventInterceptor(sKey);
        registry.unregisterEventInterceptor(sIncptrName);
        }

    /**
     * Verify an EventInterceptor registered with a Registry receives events
     * from the eventual registration of the EventDispatcher.
     */
    @Test
    public void testDispatcherInterceptorEvents()
        {
        Registry registry = new Registry();
        ServiceDispatcher dispatcher = new ServiceDispatcher(mock(PartitionedService.class, Mockito.RETURNS_DEEP_STUBS));
        EventDispatcherAwareInterceptor incptr = mock(EventDispatcherAwareInterceptor.class);

        doAnswer(new Answer()
            {
            public Object answer(InvocationOnMock inv) throws Throwable
                {
                String          sIdentifier = (String) inv.getArguments()[0];
                EventDispatcher dispatcher  = (EventDispatcher) inv.getArguments()[1];

                dispatcher.addEventInterceptor(sIdentifier, (EventInterceptor) inv.getMock(),
                        new HashSet(Arrays.asList(DispatcherInterceptorEvent.Type.values())), false);

                return null;
                }
            }).when(incptr).introduceEventDispatcher(anyString(), org.mockito.Matchers.any(EventDispatcher.class));

        registry.registerEventInterceptor(incptr);
        registry.registerEventDispatcher(dispatcher);
        registry.registerEventInterceptor(mock(EventInterceptor.class));

        verify(incptr, times(3)).onEvent(org.mockito.Matchers.any(DispatcherInterceptorEvent.class));
        }

    protected BackingMapContext mockBMC(String sServiceName, String sCacheName)
        {
        ServiceInfo              info   = mock(ServiceInfo.class);
        CacheService             cs     = mock(CacheService.class);
        BackingMapManagerContext ctxMgr = mock(BackingMapManagerContext.class);
        BackingMapContext        ctx    = mock(BackingMapContext.class);

        when(info.getServiceName()).thenReturn(sServiceName);
        when(cs.getInfo()).thenReturn(info);
        when(ctxMgr.getCacheService()).thenReturn(cs);
        when(ctx.getManagerContext()).thenReturn(ctxMgr);
        when(ctx.getCacheName()).thenReturn(sCacheName);

        return ctx;
        }

    /**
     * Simple TestInterceptor class for the unit tests
     */
    public class TestInterceptor
            implements EventInterceptor
        {
        public TestInterceptor()
            {
            super();
            }

        public void onEvent(Event event)
            {
            if (!(event instanceof InterceptorRegistrationEvent))
                {
                throw new UnsupportedOperationException();
                }
            }
        }
    }
