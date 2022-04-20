/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.events.internal;

import com.tangosol.net.events.Event;
import com.tangosol.net.events.EventInterceptor;

import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

/**
 * Test the event class ensuring the contracts on executing interceptors are properly supported
 * @author nsa 2011.08.09
 * @since 3.7.1
 */
public class EventTest
    {
    /**
     * Test nextInterceptor() calls each interceptor in order
     */
    @Test
    public void testNextInterceptor()
        {
        //reset the counter
        m_iCounter = 1;

        TestInterceptor int1 = new TestInterceptor();
        TestInterceptor int2 = new TestInterceptor();
        TestInterceptor int3 = new TestInterceptor();

        List<EventInterceptor> listInterceptors = new LinkedList<EventInterceptor>();
        listInterceptors.add(int1);
        listInterceptors.add(int2);
        listInterceptors.add(int3);

        TestEvent evt = new TestEvent(false);
        evt.dispatch(listInterceptors);

        assertEquals(1, int1.m_iOrder);
        assertEquals(2, int2.m_iOrder);
        assertEquals(3, int3.m_iOrder);
        }

    /**
     * Test nextInterceptor() calls stop flowing after an exception is thrown
     */
    @Test
    public void testNextInterceptorMutableException()
        {
        //reset the counter
        m_iCounter = 1;

        TestInterceptor int1 = new TestInterceptor();
        TestInterceptor int2 = new TestInterceptor(true);
        TestInterceptor int3 = new TestInterceptor();

        List<EventInterceptor> listInterceptors = new LinkedList<EventInterceptor>();
        listInterceptors.add(int1);
        listInterceptors.add(int2);
        listInterceptors.add(int3);

        TestEvent evt = new TestEvent(true);

        boolean fCaught = false;

        try
            {
            evt.dispatch(listInterceptors);
            }
        catch(RuntimeException e)
            {
            fCaught = true;
            }

        assertTrue(fCaught);
        assertEquals(1, int1.m_iOrder);
        assertEquals(2, int2.m_iOrder);
        assertEquals(0, int3.m_iOrder);
        }

    /**
     * Test nextInterceptor() calls stop flowing after an exception is thrown
     */
    @Test
    public void testNextInterceptorImmutableException()
        {
        //reset the counter
        m_iCounter = 1;

        TestInterceptor int1 = new TestInterceptor();
        TestInterceptor int2 = new TestInterceptor(true);
        TestInterceptor int3 = new TestInterceptor(false);

        List<EventInterceptor> listInterceptors = new LinkedList<EventInterceptor>();
        listInterceptors.add(int1);
        listInterceptors.add(int2);
        listInterceptors.add(int3);

        TestEvent evt = new TestEvent(false);

        boolean fCaught = false;

        try
            {
            evt.dispatch(listInterceptors);
            }
        catch(Exception e)
            {
            fCaught = true;
            }

        assertFalse(fCaught);
        assertEquals(1, int1.m_iOrder);
        assertEquals(2, int2.m_iOrder);
        assertEquals(3, int3.m_iOrder);
        }

    /**
     * Test interceptor implementaiton that can throw an exception and tracks what order it was executed
     */
    public class TestInterceptor
            implements EventInterceptor
        {
        public TestInterceptor()
            {
            super();
            }

        public TestInterceptor(boolean fThrow)
            {
            m_fThrow = fThrow;
            }

        public void onEvent(Event event)
            {
            m_iOrder = m_iCounter++;

            if (m_fThrow)
                {
                throw new RuntimeException("TEST");
                }

            event.nextInterceptor();
            }

        protected int     m_iOrder;
        protected boolean m_fThrow;
        }

    /**
     * A Test event implementation that allows for testing the AbstractEvent class and it's mutability
     */
    public static class TestEvent
            extends AbstractEvent
        {
        public TestEvent(boolean fMutable)
            {
            super(Type.TEST_TYPE);
            m_fMutable = fMutable;
            }

        /**
         * {@inheritDoc}
         */
        protected boolean isMutableEvent()
            {
            // for now, all transfer events are immutable
            return m_fMutable;
            }

        protected String getDescription()
            {
            return super.getDescription() + "Test Event";
            }

        public enum Type
            {
            TEST_TYPE
            }

        private boolean m_fMutable;
        }

    protected static int m_iCounter = 1;
    }
