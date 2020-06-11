/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.events.internal;

import com.tangosol.net.BackingMapContext;
import com.tangosol.net.events.*;
import com.tangosol.net.events.partition.cache.CacheLifecycleEvent;
import com.tangosol.net.events.partition.cache.EntryEvent;
import com.tangosol.net.events.partition.cache.EntryProcessorEvent;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.InvocableMap;
import java.util.Collections;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

/**
 * Test the public ServiceDispatcher methods
 * @author nsa 2011.08.09
 * @since 3.7.1
 */
public class StorageDispatcherTest
    {
    /**
     * Test the getBAckingMapContext() method
     */
    @Test
    public void testGetBackingMapContext()
        {
        BackingMapContext mockCtx    = mock(BackingMapContext.class);
        StorageDispatcher dispatcher = new StorageDispatcher(mockCtx);

        assertEquals(dispatcher.getBackingMapContext(), mockCtx);
        }

    /**
     * Test the supported types for the StorageDispatcher
     */
    @Test
    public void testSupportedTypes()
        {
        BackingMapContext mockCtx    = mock(BackingMapContext.class);
        StorageDispatcher dispatcher = new StorageDispatcher(mockCtx);
        Set<Enum>         types      = dispatcher.getSupportedTypes();

        assertEquals(14, types.size());
        assertTrue(types.contains(EntryEvent.Type.INSERTED));
        assertTrue(types.contains(EntryEvent.Type.INSERTING));
        assertTrue(types.contains(EntryEvent.Type.REMOVED));
        assertTrue(types.contains(EntryEvent.Type.REMOVING));
        assertTrue(types.contains(EntryEvent.Type.UPDATED));
        assertTrue(types.contains(EntryEvent.Type.UPDATING));
        assertTrue(types.contains(EntryProcessorEvent.Type.EXECUTED));
        assertTrue(types.contains(EntryProcessorEvent.Type.EXECUTING));
        assertTrue(types.contains(CacheLifecycleEvent.Type.CREATED));
        assertTrue(types.contains(CacheLifecycleEvent.Type.TRUNCATED));
        assertTrue(types.contains(CacheLifecycleEvent.Type.DESTROYED));
        }

    /**
     * Test isSubscribed() with no subscribers
     */
    @Test
    public void testNotSubscribed()
        {
        BackingMapContext mockCtx    = mock(BackingMapContext.class);
        StorageDispatcher dispatcher = new StorageDispatcher(mockCtx);

        assertFalse(dispatcher.isSubscribed(EntryEvent.Type.INSERTED));
        assertFalse(dispatcher.isSubscribed(EntryEvent.Type.INSERTING));
        assertFalse(dispatcher.isSubscribed(EntryEvent.Type.UPDATED));
        assertFalse(dispatcher.isSubscribed(EntryEvent.Type.UPDATING));
        assertFalse(dispatcher.isSubscribed(EntryEvent.Type.REMOVED));
        assertFalse(dispatcher.isSubscribed(EntryEvent.Type.REMOVING));
        assertFalse(dispatcher.isSubscribed(EntryProcessorEvent.Type.EXECUTING));
        assertFalse(dispatcher.isSubscribed(EntryProcessorEvent.Type.EXECUTED));
        assertFalse(dispatcher.isSubscribed(CacheLifecycleEvent.Type.CREATED));
        assertFalse(dispatcher.isSubscribed(CacheLifecycleEvent.Type.TRUNCATED));
        assertFalse(dispatcher.isSubscribed(CacheLifecycleEvent.Type.DESTROYED));
        }

    /**
     * Test isSubscribed() with no subscribers
     */
    @Test
    public void testSubscribedAll()
        {
        BackingMapContext mockCtx     = mock(BackingMapContext.class);
        StorageDispatcher dispatcher  = new StorageDispatcher(mockCtx);
        TestInterceptor   interceptor = new TestInterceptor(false);
        String            sKey        = "testInterceptor";

        dispatcher.addEventInterceptor(sKey, interceptor, null, false);

        assertTrue(dispatcher.isSubscribed(EntryEvent.Type.INSERTED));
        assertTrue(dispatcher.isSubscribed(EntryEvent.Type.INSERTING));
        assertTrue(dispatcher.isSubscribed(EntryEvent.Type.UPDATED));
        assertTrue(dispatcher.isSubscribed(EntryEvent.Type.UPDATING));
        assertTrue(dispatcher.isSubscribed(EntryEvent.Type.REMOVED));
        assertTrue(dispatcher.isSubscribed(EntryEvent.Type.REMOVING));
        assertTrue(dispatcher.isSubscribed(EntryProcessorEvent.Type.EXECUTING));
        assertTrue(dispatcher.isSubscribed(EntryProcessorEvent.Type.EXECUTED));
        assertTrue(dispatcher.isSubscribed(CacheLifecycleEvent.Type.CREATED));
        assertTrue(dispatcher.isSubscribed(CacheLifecycleEvent.Type.TRUNCATED));
        assertTrue(dispatcher.isSubscribed(CacheLifecycleEvent.Type.DESTROYED));
        }

    /**
     * Test isSubscribed() for INSERTED only
     */
    @Test
    public void testSubscribedInserted()
        {
        BackingMapContext        mockCtx    = mock(BackingMapContext.class);
        StorageDispatcher        dispatcher = new StorageDispatcher(mockCtx);
        String                   sKey       = "testInterceptor";
        HashSet<EntryEvent.Type> setTypes   = new HashSet<EntryEvent.Type>();

        setTypes.add(EntryEvent.Type.INSERTED);
        TestInterceptor<EntryEvent<?, ?>> interceptor = new TestInterceptor<EntryEvent<?, ?>>(false);

        dispatcher.addEventInterceptor(sKey, interceptor, setTypes, false);

        assertTrue(dispatcher.isSubscribed(EntryEvent.Type.INSERTED));
        assertFalse(dispatcher.isSubscribed(EntryEvent.Type.INSERTING));
        assertFalse(dispatcher.isSubscribed(EntryEvent.Type.UPDATED));
        assertFalse(dispatcher.isSubscribed(EntryEvent.Type.UPDATING));
        assertFalse(dispatcher.isSubscribed(EntryEvent.Type.REMOVED));
        assertFalse(dispatcher.isSubscribed(EntryEvent.Type.REMOVING));
        assertFalse(dispatcher.isSubscribed(EntryProcessorEvent.Type.EXECUTING));
        assertFalse(dispatcher.isSubscribed(EntryProcessorEvent.Type.EXECUTED));
        }

    /**
     * Test isSubscribed() for INSERTING only
     */
    @Test
    public void testSubscribedInserting()
        {
        BackingMapContext mockCtx     = mock(BackingMapContext.class);
        StorageDispatcher dispatcher  = new StorageDispatcher(mockCtx);
        String            sKey        = "testInterceptor";
        HashSet           setTypes    = new HashSet();

        setTypes.add(EntryEvent.Type.INSERTING);
        TestInterceptor interceptor = new TestInterceptor(false);

        dispatcher.addEventInterceptor(sKey, interceptor, setTypes, false);

        assertFalse(dispatcher.isSubscribed(EntryEvent.Type.INSERTED));
        assertTrue(dispatcher.isSubscribed(EntryEvent.Type.INSERTING));
        assertFalse(dispatcher.isSubscribed(EntryEvent.Type.UPDATED));
        assertFalse(dispatcher.isSubscribed(EntryEvent.Type.UPDATING));
        assertFalse(dispatcher.isSubscribed(EntryEvent.Type.REMOVED));
        assertFalse(dispatcher.isSubscribed(EntryEvent.Type.REMOVING));
        assertFalse(dispatcher.isSubscribed(EntryProcessorEvent.Type.EXECUTING));
        assertFalse(dispatcher.isSubscribed(EntryProcessorEvent.Type.EXECUTED));
        }

    /**
     * Test isSubscribed() for UPDATED only
     */
    @Test
    public void testSubscribedUpdated()
        {
        BackingMapContext mockCtx     = mock(BackingMapContext.class);
        StorageDispatcher dispatcher  = new StorageDispatcher(mockCtx);
        String            sKey        = "testInterceptor";
        HashSet           setTypes    = new HashSet();

        setTypes.add(EntryEvent.Type.UPDATED);
        TestInterceptor interceptor = new TestInterceptor(false);

        dispatcher.addEventInterceptor(sKey, interceptor, setTypes, false);

        assertFalse(dispatcher.isSubscribed(EntryEvent.Type.INSERTED));
        assertFalse(dispatcher.isSubscribed(EntryEvent.Type.INSERTING));
        assertTrue(dispatcher.isSubscribed(EntryEvent.Type.UPDATED));
        assertFalse(dispatcher.isSubscribed(EntryEvent.Type.UPDATING));
        assertFalse(dispatcher.isSubscribed(EntryEvent.Type.REMOVED));
        assertFalse(dispatcher.isSubscribed(EntryEvent.Type.REMOVING));
        assertFalse(dispatcher.isSubscribed(EntryProcessorEvent.Type.EXECUTING));
        assertFalse(dispatcher.isSubscribed(EntryProcessorEvent.Type.EXECUTED));
        }


    /**
     * Test isSubscribed() for UPDATING only
     */
    @Test
    public void testSubscribedUpdating()
        {
        BackingMapContext mockCtx     = mock(BackingMapContext.class);
        StorageDispatcher dispatcher  = new StorageDispatcher(mockCtx);
        String            sKey        = "testInterceptor";
        HashSet           setTypes    = new HashSet();

        setTypes.add(EntryEvent.Type.UPDATING);
        TestInterceptor interceptor = new TestInterceptor(false);

        dispatcher.addEventInterceptor(sKey, interceptor, setTypes, false);

        assertFalse(dispatcher.isSubscribed(EntryEvent.Type.INSERTED));
        assertFalse(dispatcher.isSubscribed(EntryEvent.Type.INSERTING));
        assertFalse(dispatcher.isSubscribed(EntryEvent.Type.UPDATED));
        assertTrue(dispatcher.isSubscribed(EntryEvent.Type.UPDATING));
        assertFalse(dispatcher.isSubscribed(EntryEvent.Type.REMOVED));
        assertFalse(dispatcher.isSubscribed(EntryEvent.Type.REMOVING));
        assertFalse(dispatcher.isSubscribed(EntryProcessorEvent.Type.EXECUTING));
        assertFalse(dispatcher.isSubscribed(EntryProcessorEvent.Type.EXECUTED));
        }

    /**
     * Test isSubscribed() for REMOVED only
     */
    @Test
    public void testSubscribedRemoved()
        {
        BackingMapContext mockCtx     = mock(BackingMapContext.class);
        StorageDispatcher dispatcher  = new StorageDispatcher(mockCtx);
        String            sKey        = "testInterceptor";
        HashSet           setTypes    = new HashSet();

        setTypes.add(EntryEvent.Type.REMOVED);
        TestInterceptor interceptor = new TestInterceptor(false);

        dispatcher.addEventInterceptor(sKey, interceptor, setTypes, false);

        assertFalse(dispatcher.isSubscribed(EntryEvent.Type.INSERTED));
        assertFalse(dispatcher.isSubscribed(EntryEvent.Type.INSERTING));
        assertFalse(dispatcher.isSubscribed(EntryEvent.Type.UPDATED));
        assertFalse(dispatcher.isSubscribed(EntryEvent.Type.UPDATING));
        assertTrue(dispatcher.isSubscribed(EntryEvent.Type.REMOVED));
        assertFalse(dispatcher.isSubscribed(EntryEvent.Type.REMOVING));
        assertFalse(dispatcher.isSubscribed(EntryProcessorEvent.Type.EXECUTING));
        assertFalse(dispatcher.isSubscribed(EntryProcessorEvent.Type.EXECUTED));
        }

    /**
     * Test isSubscribed() for REMOVING only
     */
    @Test
    public void testSubscribedRemoving()
        {
        BackingMapContext mockCtx     = mock(BackingMapContext.class);
        StorageDispatcher dispatcher  = new StorageDispatcher(mockCtx);
        String            sKey        = "testInterceptor";
        HashSet           setTypes    = new HashSet();

        setTypes.add(EntryEvent.Type.REMOVING);
        TestInterceptor interceptor = new TestInterceptor(false);

        dispatcher.addEventInterceptor(sKey, interceptor, setTypes, false);

        assertFalse(dispatcher.isSubscribed(EntryEvent.Type.INSERTED));
        assertFalse(dispatcher.isSubscribed(EntryEvent.Type.INSERTING));
        assertFalse(dispatcher.isSubscribed(EntryEvent.Type.UPDATED));
        assertFalse(dispatcher.isSubscribed(EntryEvent.Type.UPDATING));
        assertFalse(dispatcher.isSubscribed(EntryEvent.Type.REMOVED));
        assertTrue(dispatcher.isSubscribed(EntryEvent.Type.REMOVING));
        assertFalse(dispatcher.isSubscribed(EntryProcessorEvent.Type.EXECUTING));
        assertFalse(dispatcher.isSubscribed(EntryProcessorEvent.Type.EXECUTED));
        }

    /**
     * Test isSubscribed() for EXECUTING only
     */
    @Test
    public void testSubscribedExecuting()
        {
        BackingMapContext mockCtx     = mock(BackingMapContext.class);
        StorageDispatcher dispatcher  = new StorageDispatcher(mockCtx);
        String            sKey        = "testInterceptor";
        HashSet           setTypes    = new HashSet();

        setTypes.add(EntryProcessorEvent.Type.EXECUTING);
        TestInterceptor interceptor = new TestInterceptor(false);

        dispatcher.addEventInterceptor(sKey, interceptor, setTypes, false);

        assertFalse(dispatcher.isSubscribed(EntryEvent.Type.INSERTED));
        assertFalse(dispatcher.isSubscribed(EntryEvent.Type.INSERTING));
        assertFalse(dispatcher.isSubscribed(EntryEvent.Type.UPDATED));
        assertFalse(dispatcher.isSubscribed(EntryEvent.Type.UPDATING));
        assertFalse(dispatcher.isSubscribed(EntryEvent.Type.REMOVED));
        assertFalse(dispatcher.isSubscribed(EntryEvent.Type.REMOVING));
        assertTrue(dispatcher.isSubscribed(EntryProcessorEvent.Type.EXECUTING));
        assertFalse(dispatcher.isSubscribed(EntryProcessorEvent.Type.EXECUTED));
        }

    /**
     * Test isSubscribed() for EXECUTING only
     */
    @Test
    public void testSubscribedExecuted()
        {
        BackingMapContext mockCtx     = mock(BackingMapContext.class);
        StorageDispatcher dispatcher  = new StorageDispatcher(mockCtx);
        String            sKey        = "testInterceptor";
        HashSet           setTypes    = new HashSet();

        setTypes.add(EntryProcessorEvent.Type.EXECUTED);
        TestInterceptor interceptor = new TestInterceptor(false);

        dispatcher.addEventInterceptor(sKey, interceptor, setTypes, false);

        assertFalse(dispatcher.isSubscribed(EntryEvent.Type.INSERTED));
        assertFalse(dispatcher.isSubscribed(EntryEvent.Type.INSERTING));
        assertFalse(dispatcher.isSubscribed(EntryEvent.Type.UPDATED));
        assertFalse(dispatcher.isSubscribed(EntryEvent.Type.UPDATING));
        assertFalse(dispatcher.isSubscribed(EntryEvent.Type.REMOVED));
        assertFalse(dispatcher.isSubscribed(EntryEvent.Type.REMOVING));
        assertFalse(dispatcher.isSubscribed(EntryProcessorEvent.Type.EXECUTING));
        assertTrue(dispatcher.isSubscribed(EntryProcessorEvent.Type.EXECUTED));
        }

    /**
     * Test Dispatching an INSERTING Event
     */
    @Test
    public void testDispatchInserting()
        {
        BackingMapContext mockCtx     = mock(BackingMapContext.class);
        StorageDispatcher dispatcher  = new StorageDispatcher(mockCtx);
        String            sKey        = "testInterceptor";
        HashSet           setTypes    = new HashSet();
        Set<BinaryEntry>  setEntries  = Collections.singleton(mock(BinaryEntry.class));

        setTypes.add(EntryEvent.Type.INSERTING);
        TestInterceptor interceptor = new TestInterceptor(false);

        dispatcher.addEventInterceptor(sKey, interceptor, setTypes, false);

        dispatcher.getEntryEventContinuation(EntryEvent.Type.INSERTING, setEntries, null).proceed(Boolean.TRUE);

        assertNotNull(interceptor.m_Event);
        assertEquals(EntryEvent.Type.INSERTING, interceptor.m_Event.getType());
        assertTrue(interceptor.m_Event instanceof EntryEvent);

        AbstractEvent entryEvent = (AbstractEvent) interceptor.m_Event;
        assertTrue(entryEvent.isMutableEvent());
        }

     /**
     * Test Dispatching an INSERTED Event
     */
    @Test
    public void testDispatchInserted()
        {
        BackingMapContext mockCtx     = mock(BackingMapContext.class);
        StorageDispatcher dispatcher  = new StorageDispatcher(mockCtx);
        String            sKey        = "testInterceptor";
        HashSet           setTypes    = new HashSet();
        Set<BinaryEntry>  setEntries  = Collections.singleton(mock(BinaryEntry.class));

        setTypes.add(EntryEvent.Type.INSERTED);
        TestInterceptor interceptor = new TestInterceptor(false);

        dispatcher.addEventInterceptor(sKey, interceptor, setTypes, false);

        dispatcher.getEntryEventContinuation(EntryEvent.Type.INSERTED, setEntries, null).proceed(Boolean.TRUE);

        assertNotNull(interceptor.m_Event);
        assertEquals(EntryEvent.Type.INSERTED, interceptor.m_Event.getType());
        assertTrue(interceptor.m_Event instanceof EntryEvent);

        AbstractEvent entryEvent = (AbstractEvent) interceptor.m_Event;
        assertFalse(entryEvent.isMutableEvent());
        }

    /**
     * Test Dispatching an REMOVING Event
     */
    @Test
    public void testDispatchRemoving()
        {
        BackingMapContext mockCtx     = mock(BackingMapContext.class);
        StorageDispatcher dispatcher  = new StorageDispatcher(mockCtx);
        String            sKey        = "testInterceptor";
        HashSet           setTypes    = new HashSet();
        Set<BinaryEntry>  setEntries  = Collections.singleton(mock(BinaryEntry.class));

        setTypes.add(EntryEvent.Type.REMOVING);
        TestInterceptor interceptor = new TestInterceptor(false);

        dispatcher.addEventInterceptor(sKey, interceptor, setTypes, false);

        dispatcher.getEntryEventContinuation(EntryEvent.Type.REMOVING, setEntries, null).proceed(Boolean.TRUE);

        assertNotNull(interceptor.m_Event);
        assertEquals(EntryEvent.Type.REMOVING, interceptor.m_Event.getType());
        assertTrue(interceptor.m_Event instanceof EntryEvent);

        AbstractEvent entryEvent = (AbstractEvent) interceptor.m_Event;
        assertTrue(entryEvent.isMutableEvent());
        }

    /**
     * Test Dispatching an REMOVED Event
     */
    @Test
    public void testDispatchRemoved()
        {
        BackingMapContext mockCtx     = mock(BackingMapContext.class);
        StorageDispatcher dispatcher  = new StorageDispatcher(mockCtx);
        String            sKey        = "testInterceptor";
        HashSet           setTypes    = new HashSet();
        Set<BinaryEntry>  setEntries  = Collections.singleton(mock(BinaryEntry.class));

        setTypes.add(EntryEvent.Type.REMOVED);
        TestInterceptor interceptor = new TestInterceptor(false);

        dispatcher.addEventInterceptor(sKey, interceptor, setTypes, false);

        dispatcher.getEntryEventContinuation(EntryEvent.Type.REMOVED, setEntries, null).proceed(Boolean.TRUE);

        assertNotNull(interceptor.m_Event);
        assertEquals(EntryEvent.Type.REMOVED, interceptor.m_Event.getType());
        assertTrue(interceptor.m_Event instanceof EntryEvent);

        AbstractEvent entryEvent = (AbstractEvent) interceptor.m_Event;
        assertFalse(entryEvent.isMutableEvent());
        }

    /**
     * Test Dispatching an UPDATING Event
     */
    @Test
    public void testDispatchUpdating()
        {
        BackingMapContext mockCtx     = mock(BackingMapContext.class);
        StorageDispatcher dispatcher  = new StorageDispatcher(mockCtx);
        String            sKey        = "testInterceptor";
        HashSet           setTypes    = new HashSet();
        Set<BinaryEntry>  setEntries  = Collections.singleton(mock(BinaryEntry.class));

        setTypes.add(EntryEvent.Type.UPDATING);
        TestInterceptor interceptor = new TestInterceptor(false);

        dispatcher.addEventInterceptor(sKey, interceptor, setTypes, false);

        dispatcher.getEntryEventContinuation(EntryEvent.Type.UPDATING, setEntries, null).proceed(Boolean.TRUE);

        assertNotNull(interceptor.m_Event);
        assertEquals(EntryEvent.Type.UPDATING, interceptor.m_Event.getType());
        assertTrue(interceptor.m_Event instanceof EntryEvent);

        AbstractEvent entryEvent = (AbstractEvent) interceptor.m_Event;
        assertTrue(entryEvent.isMutableEvent());
        }

    /**
     * Test Dispatching an UPDATED Event
     */
    @Test
    public void testDispatchUpdated()
        {
        BackingMapContext mockCtx     = mock(BackingMapContext.class);
        StorageDispatcher dispatcher  = new StorageDispatcher(mockCtx);
        String            sKey        = "testInterceptor";
        HashSet           setTypes    = new HashSet();
        Set<BinaryEntry>  setEntries  = Collections.singleton(mock(BinaryEntry.class));

        setTypes.add(EntryEvent.Type.UPDATED);
        TestInterceptor interceptor = new TestInterceptor(false);

        dispatcher.addEventInterceptor(sKey, interceptor, setTypes, false);

        dispatcher.getEntryEventContinuation(EntryEvent.Type.UPDATED, setEntries, null).proceed(Boolean.TRUE);

        assertNotNull(interceptor.m_Event);
        assertEquals(EntryEvent.Type.UPDATED, interceptor.m_Event.getType());
        assertTrue(interceptor.m_Event instanceof EntryEvent);

        AbstractEvent entryEvent = (AbstractEvent) interceptor.m_Event;
        assertFalse(entryEvent.isMutableEvent());
        }

    /**
     * Test Dispatching an EXECUTING Event
     */
    @Test
    public void testDispatchExecuting()
        {
        BackingMapContext mockCtx     = mock(BackingMapContext.class);
        StorageDispatcher dispatcher  = new StorageDispatcher(mockCtx);
        String            sKey        = "testInterceptor";
        HashSet           setTypes    = new HashSet();
        Set<BinaryEntry>  setEntries  = mock(HashSet.class);

        setTypes.add(EntryProcessorEvent.Type.EXECUTING);
        TestInterceptor interceptor = new TestInterceptor(false);

        dispatcher.addEventInterceptor(sKey, interceptor, setTypes, false);

        dispatcher.getEntryProcessorEventContinuation(EntryProcessorEvent.Type.EXECUTING,
                mock(InvocableMap.EntryProcessor.class), setEntries, null).proceed(Boolean.TRUE);

        assertNotNull(interceptor.m_Event);
        assertEquals(EntryProcessorEvent.Type.EXECUTING, interceptor.m_Event.getType());
        assertTrue(interceptor.m_Event instanceof EntryProcessorEvent);

        AbstractEvent invocationEvent = (AbstractEvent) interceptor.m_Event;
        assertTrue(invocationEvent.isMutableEvent());
        }

    /**
     * Test Dispatching an EXECUTED Event
     */
    @Test
    public void testDispatchExecuted()
        {
        BackingMapContext mockCtx     = mock(BackingMapContext.class);
        StorageDispatcher dispatcher  = new StorageDispatcher(mockCtx);
        String            sKey        = "testInterceptor";
        HashSet           setTypes    = new HashSet();
        Set<BinaryEntry>  setEntries  = mock(HashSet.class);

        setTypes.add(EntryProcessorEvent.Type.EXECUTED);
        TestInterceptor interceptor = new TestInterceptor(false);

        dispatcher.addEventInterceptor(sKey, interceptor, setTypes, false);

        dispatcher.getEntryProcessorEventContinuation(EntryProcessorEvent.Type.EXECUTED,
                mock(InvocableMap.EntryProcessor.class), setEntries, null).proceed(Boolean.TRUE);

        assertNotNull(interceptor.m_Event);
        assertEquals(EntryProcessorEvent.Type.EXECUTED, interceptor.m_Event.getType());
        assertTrue(interceptor.m_Event instanceof EntryProcessorEvent);

        AbstractEvent invocationEvent = (AbstractEvent) interceptor.m_Event;
        assertFalse(invocationEvent.isMutableEvent());
        }

    /**
     * Test isSubscribed() for CREATED only
     */
    @Test
    public void testSubscribedCreated()
        {
        BackingMapContext mockCtx     = mock(BackingMapContext.class);
        StorageDispatcher dispatcher  = new StorageDispatcher(mockCtx);
        String            sKey        = "testInterceptor";
        HashSet           setTypes    = new HashSet();

        setTypes.add(CacheLifecycleEvent.Type.CREATED);
        TestInterceptor interceptor = new TestInterceptor(false);

        dispatcher.addEventInterceptor(sKey, interceptor, setTypes, false);

        assertTrue(dispatcher.isSubscribed(CacheLifecycleEvent.Type.CREATED));
        assertFalse(dispatcher.isSubscribed(CacheLifecycleEvent.Type.DESTROYED));
        assertFalse(dispatcher.isSubscribed(CacheLifecycleEvent.Type.TRUNCATED));
        }

    /**
     * Test Dispatching an cache CREATED Event
     */
    @Test
    public void testDispatchCreated()
        {
        BackingMapContext mockCtx     = mock(BackingMapContext.class);
        StorageDispatcher dispatcher  = new StorageDispatcher(mockCtx);
        String            sKey        = "testInterceptor";
        String            sCache      = "cacheTest";
        HashSet           setTypes    = new HashSet();

        setTypes.add(CacheLifecycleEvent.Type.CREATED);
        TestInterceptor interceptor = new TestInterceptor(false);

        dispatcher.addEventInterceptor(sKey, interceptor, setTypes, false);

        dispatcher.getCacheLifecycleEventContinuation(CacheLifecycleEvent.Type.CREATED, sCache, null).proceed(Boolean.TRUE);

        assertNotNull(interceptor.m_Event);
        assertEquals(CacheLifecycleEvent.Type.CREATED, interceptor.m_Event.getType());
        assertTrue(interceptor.m_Event instanceof CacheLifecycleEvent);

        AbstractEvent cacheEvent = (AbstractEvent) interceptor.m_Event;
        assertFalse(cacheEvent.isMutableEvent());
        }

    /**
     * Test an exception being thrown from the interceptor. TransferEvents are non-terminal so the exception should not
     * propagate back to us, it should only get logged.
     */
    //@Test(expected=IllegalStateException.class)
    public void testTerminalException()
        {
        BackingMapContext mockCtx     = mock(BackingMapContext.class);
        StorageDispatcher disp        = new StorageDispatcher(mockCtx);
        String            sKey        = "testInterceptor";
        HashSet           setTypes    = new HashSet();
        Set<BinaryEntry>  setEntries  = mock(Set.class);

        setTypes.add(EntryEvent.Type.INSERTING);
        TestInterceptor interceptor = new TestInterceptor(true);

        disp.addEventInterceptor(sKey, interceptor, setTypes, false);

        disp.getEntryEventContinuation(EntryEvent.Type.INSERTING,
                setEntries, null).proceed(Boolean.TRUE);

        assertNotNull(interceptor.m_Event);
        assertEquals(EntryEvent.Type.INSERTING, interceptor.m_Event.getType());
        }

    /**
     * Test interceptor class used to support the StorageDispatcher tests.
     */
    public class TestInterceptor<T extends Event<? extends Enum>>
            implements EventInterceptor<T>
        {
        public TestInterceptor(boolean fThrow)
            {
            m_fThrow = fThrow;
            }

        public void onEvent(T event)
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
