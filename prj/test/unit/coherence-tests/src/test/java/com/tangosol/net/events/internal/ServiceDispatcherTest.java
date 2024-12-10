/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.events.internal;

import com.tangosol.net.Member;
import com.tangosol.net.PartitionedService;
import com.tangosol.net.events.Event;
import com.tangosol.net.events.EventInterceptor;
import com.tangosol.net.events.NamedEventInterceptor;
import com.tangosol.net.events.internal.AbstractEventDispatcher.DispatcherInterceptorEvent;
import com.tangosol.net.events.partition.TransferEvent;

import org.junit.Test;

import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Test the public ServiceDispatcher methods.
 *
 * @author nsa 2011.08.09
 * @since 3.7.1
 */
public class ServiceDispatcherTest
    {
    /**
     * Test the getService method.
     */
    @Test
    public void testGetService()
        {
        PartitionedService mockSvc = mock(PartitionedService.class, Mockito.RETURNS_DEEP_STUBS);
        ServiceDispatcher  disp    = new ServiceDispatcher(mockSvc);

        assertEquals(disp.getService(), mockSvc);
        }

    /**
     * Test the supported types for the ServiceDispatcher
     */
    @Test
    public void testSupportedTypes()
        {
        PartitionedService mockSvc = mock(PartitionedService.class, Mockito.RETURNS_DEEP_STUBS);
        ServiceDispatcher  disp    = new ServiceDispatcher(mockSvc);
        Set<Enum>          types   = disp.getSupportedTypes();

        assertEquals(13, types.size());
        assertTrue(types.contains(TransferEvent.Type.ARRIVED));
        assertTrue(types.contains(TransferEvent.Type.DEPARTING));
        }

    /**
     * Test isSubscribed() with no subscribers
     */
    @Test
    public void testNotSubscribed()
        {
        PartitionedService mockSvc = mock(PartitionedService.class, Mockito.RETURNS_DEEP_STUBS);
        ServiceDispatcher  disp    = new ServiceDispatcher(mockSvc);

        assertFalse(disp.isSubscribed(TransferEvent.Type.ARRIVED));
        assertFalse(disp.isSubscribed(TransferEvent.Type.DEPARTING));
        }

    /**
     * Test isSubscribed with a subscriber registered for all events
     */
    @Test
    public void testSubscribedAll()
        {
        PartitionedService mockSvc     = mock(PartitionedService.class, Mockito.RETURNS_DEEP_STUBS);
        ServiceDispatcher  disp        = new ServiceDispatcher(mockSvc);
        TestInterceptor    interceptor = new TestInterceptor(false);
        String             sKey        = "testInterceptor";

        disp.addEventInterceptor(sKey, interceptor);
        assertTrue(disp.isSubscribed(TransferEvent.Type.ARRIVED));
        assertTrue(disp.isSubscribed(TransferEvent.Type.DEPARTING));
        }

    /**
     * Test isSubscribed for ARRIVED only events.
     */
    @Test
    public void testSubscribedArrived()
        {
        PartitionedService mockSvc     = mock(PartitionedService.class, Mockito.RETURNS_DEEP_STUBS);
        ServiceDispatcher  disp        = new ServiceDispatcher(mockSvc);
        String             sKey        = "testInterceptor";
        HashSet            setTypes    = new HashSet();

        setTypes.add(TransferEvent.Type.ARRIVED);
        TestInterceptor interceptor = new TestInterceptor(false);

        disp.addEventInterceptor(sKey, interceptor, setTypes, false);
        assertTrue(disp.isSubscribed(TransferEvent.Type.ARRIVED));
        assertFalse(disp.isSubscribed(TransferEvent.Type.DEPARTING));
        }

    /**
     * Test isSubscribed for DEPARTING only events.
     */
    @Test
    public void testSubscribedDeparting()
        {
        PartitionedService mockSvc     = mock(PartitionedService.class, Mockito.RETURNS_DEEP_STUBS);
        ServiceDispatcher  disp        = new ServiceDispatcher(mockSvc);
        String             sKey        = "testInterceptor";
        HashSet            setTypes    = new HashSet();

        setTypes.add(TransferEvent.Type.DEPARTING);
        TestInterceptor interceptor = new TestInterceptor(false);

        disp.addEventInterceptor(sKey, interceptor, setTypes, false);
        assertFalse(disp.isSubscribed(TransferEvent.Type.ARRIVED));
        assertTrue(disp.isSubscribed(TransferEvent.Type.DEPARTING));
        }

    /**
     * Test dispatching ARRIVED events
     */
    @Test
    public void testDispatchArrived()
        {
        PartitionedService mockSvc     = mock(PartitionedService.class, Mockito.RETURNS_DEEP_STUBS);
        ServiceDispatcher  disp        = new ServiceDispatcher(mockSvc);
        String             sKey        = "testInterceptor";
        HashSet            setTypes    = new HashSet();
        Map                mapEntries  = new HashMap()
                {{
                put("testCache", mock(Set.class));
                }};

        setTypes.add(TransferEvent.Type.ARRIVED);
        TestInterceptor interceptor = new TestInterceptor(false);

        disp.addEventInterceptor(sKey, interceptor, setTypes, false);
        disp.getTransferEventContinuation(TransferEvent.Type.ARRIVED,
                0, mock(Member.class), mock(Member.class), mapEntries, null).proceed(Boolean.TRUE);

        assertNotNull(interceptor.m_Event);
        assertEquals(TransferEvent.Type.ARRIVED, interceptor.m_Event.getType());
        assertTrue(((TransferEvent) interceptor.m_Event).getEntries().containsKey("testCache"));
        }

    /**
     * Test dispatching a DEPARTING event with only an ARRIVED interceptor registered
     */
    @Test
    public void testDispatchDepartingForArrived()
        {
        PartitionedService mockSvc     = mock(PartitionedService.class, Mockito.RETURNS_DEEP_STUBS);
        ServiceDispatcher  disp        = new ServiceDispatcher(mockSvc);
        String             sKey        = "testInterceptor";
        HashSet            setTypes    = new HashSet();
        Map                mapEntries  = new HashMap()
                    {{
                    put("testCache", mock(Set.class));
                    }};

        setTypes.add(TransferEvent.Type.ARRIVED);
        TestInterceptor interceptor = new TestInterceptor(false);

        disp.addEventInterceptor(sKey, interceptor, setTypes, false);
        disp.getTransferEventContinuation(TransferEvent.Type.DEPARTING,
                0, mock(Member.class), mock(Member.class), mapEntries, null).proceed(Boolean.TRUE);

        assertNull(interceptor.m_Event);
        }

    /**
     * Test dispatching a DEPARTING event.
     */
    @Test
    public void testDispatchDeparting()
        {
        PartitionedService mockSvc     = mock(PartitionedService.class, Mockito.RETURNS_DEEP_STUBS);
        ServiceDispatcher  disp        = new ServiceDispatcher(mockSvc);
        String             sKey        = "testInterceptor";
        HashSet            setTypes    = new HashSet();
        Map                mapEntries  = new HashMap()
                    {{
                    put("testCache", mock(Set.class));
                    }};

        setTypes.add(TransferEvent.Type.DEPARTING);
        TestInterceptor interceptor = new TestInterceptor(false);

        disp.addEventInterceptor(sKey, interceptor, setTypes, false);
        disp.getTransferEventContinuation(TransferEvent.Type.DEPARTING,
                0, mock(Member.class), mock(Member.class), mapEntries, null).proceed(Boolean.TRUE);

        assertNotNull(interceptor.m_Event);
        assertEquals(TransferEvent.Type.DEPARTING, interceptor.m_Event.getType());
        assertTrue(((TransferEvent) interceptor.m_Event).getEntries().containsKey("testCache"));
        }

    /**
     * Test dispatching an ARRIVED event with only DEPARTING registered
     */
    @Test
    public void testDispatchArrivedForDeparting()
        {
        PartitionedService mockSvc     = mock(PartitionedService.class, Mockito.RETURNS_DEEP_STUBS);
        ServiceDispatcher  disp        = new ServiceDispatcher(mockSvc);
        String             sKey        = "testInterceptor";
        HashSet            setTypes    = new HashSet();
        Map                mapEntries  = new HashMap()
                    {{
                    put("testCache", mock(Set.class));
                    }};

        setTypes.add(TransferEvent.Type.DEPARTING);
        TestInterceptor interceptor = new TestInterceptor(false);

        disp.addEventInterceptor(sKey, interceptor, setTypes, false);
        disp.getTransferEventContinuation(TransferEvent.Type.ARRIVED,
                0, mock(Member.class), mock(Member.class), mapEntries, null).proceed(Boolean.TRUE);

        assertNull(interceptor.m_Event);
        }

    /**
     * Test an exception being thrown from the interceptor. TransferEvents are non-terminal so the exception should not
     * propagate back to us, it should only get logged.
     */
    @Test
    public void testException()
        {
        PartitionedService mockSvc     = mock(PartitionedService.class, Mockito.RETURNS_DEEP_STUBS);
        ServiceDispatcher  disp        = new ServiceDispatcher(mockSvc);
        String             sKey        = "testInterceptor";
        HashSet            setTypes    = new HashSet();
        Map                mapEntries  = new HashMap()
                    {{
                    put("testCache", mock(Set.class));
                    }};


        setTypes.add(TransferEvent.Type.ARRIVED);
        TestInterceptor interceptor = new TestInterceptor(true);

        disp.addEventInterceptor(sKey, interceptor, setTypes, false);
        disp.getTransferEventContinuation(TransferEvent.Type.ARRIVED,
                0, mock(Member.class), mock(Member.class), mapEntries, null).proceed(Boolean.TRUE);

        assertNotNull(interceptor.m_Event);
        assertEquals(TransferEvent.Type.ARRIVED, interceptor.m_Event.getType());
        assertTrue(((TransferEvent) interceptor.m_Event).getEntries().containsKey("testCache"));
        }

    @Test
    public void testDispatcherInterceptorEvents()
        {
        PartitionedService mockSvc    = mock(PartitionedService.class, Mockito.RETURNS_DEEP_STUBS);
        ServiceDispatcher  disp       = new ServiceDispatcher(mockSvc);
        EventInterceptor   incptrMock = mock(EventInterceptor.class);

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);

        disp.addEventInterceptor("test", incptrMock, new HashSet(
                Arrays.asList(DispatcherInterceptorEvent.Type.values())), true);

        disp.addEventInterceptor("test2", new TestInterceptor(false));

        // receive the INSERTED for test and INSERTING & INSERTED for test2
        verify(incptrMock, times(3)).onEvent(any(DispatcherInterceptorEvent.class));

        // verify that if we throw during the INSERTING event the interceptor
        // is not registered
        doThrow(new RuntimeException("Nada")).when(incptrMock).onEvent(any(Event.class));

        Throwable err = null;
        try
            {
            disp.addEventInterceptor("test3", new TestInterceptor(false));
            }
        catch (Throwable t)
            {
            err = t;
            }
        assertThat(err, notNullValue());

        // test that we can replace the interceptor as a part of the INSERTING event
        final EventInterceptor incptrReplaceMock = mock(EventInterceptor.class);
        doAnswer(new Answer()
            {
            public Object answer(InvocationOnMock inv) throws Throwable
                {
                DispatcherInterceptorEvent event = (DispatcherInterceptorEvent) inv.getArguments()[0];
                if (event.getType() == DispatcherInterceptorEvent.Type.INSERTING)
                    {
                    event.setInterceptor(incptrReplaceMock);
                    }
                return null;
                }
            }).when(incptrMock).onEvent(any(Event.class));

        disp.addEventInterceptor("test4", new TestInterceptor(false));

        boolean fMatch = false;
        for (NamedEventInterceptor incptrNamed : disp.getInterceptorMap().get(TransferEvent.Type.ARRIVED))
            {
            if (incptrNamed.getInterceptor() == incptrReplaceMock)
                {
                fMatch = true;
                break;
                }
            }
        assertTrue("Interceptor was not replaced as expected", fMatch);
        }

    /**
     * Test interceptor class used to support the unit tests
     */
    public class TestInterceptor
            implements EventInterceptor
        {
        public TestInterceptor(boolean fThrow)
            {
            m_fThrow = fThrow;
            }

        public void onEvent(Event event)
            {
            m_Event = event;

            if (m_fThrow)
                {
                throw new IllegalStateException();
                }
            }

        protected Event   m_Event;
        protected boolean m_fThrow;
        }
    }
