/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util.listener;

import com.tangosol.util.MapEvent;
import com.tangosol.util.MapListener;
import com.tangosol.util.listener.SimpleMapListener;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for VersionAwareListeners
 *
 * @author hr  2021.02.10
 */
public class VersionAwareListenersTest
    {
    @Test
    public void testEquality()
        {
        MapListener<Integer, String> listener          = new SimpleMapListener<Integer, String>().versioned();
        MapListener<Integer, String> listenerVersioned = VersionAwareListeners.createListener(listener);

        assertEquals(listener, listenerVersioned);
        assertEquals(listenerVersioned, listener);

        assertEquals(listener.hashCode(), listenerVersioned.hashCode());
        assertEquals(listenerVersioned.hashCode(), listener.hashCode());
        }

    @Test
    public void testGap()
        {
        AtomicInteger atomicCounter = new AtomicInteger();

        MapListener<Integer, String> listener = new SimpleMapListener<Integer, String>().
                addEventHandler(event -> atomicCounter.incrementAndGet()).versioned();

        MapListener<Integer, String> listenerVersioned = VersionAwareListeners.createListener(listener);

        MapEvent<Integer, String>[] aEvent = new MapEvent[5];

        for (int i = 0, c = aEvent.length; i < c; ++i)
            {
            MapEvent<Integer, String> event = aEvent[i] = mock(MapEvent.class);

            when(event.getPartition()).thenReturn(17);
            when(event.getVersion()).thenReturn(i + 1L);
            }

        listenerVersioned.entryInserted(aEvent[0]);
        listenerVersioned.entryInserted(aEvent[2]); // future version
        listenerVersioned.entryInserted(aEvent[3]); // another future
        listenerVersioned.entryInserted(aEvent[2]); // duplicate event
        listenerVersioned.entryInserted(aEvent[1]); // expected version
        listenerVersioned.entryInserted(aEvent[4]);

        assertEquals(5, atomicCounter.get());
        }

    @Test
    public void testGapThreshold()
        {
        final int GAP_THRESHOLD = 0xFFFF;

        MapEvent<Integer, String>[] aEvent = new MapEvent[GAP_THRESHOLD + 4];

        // mock up enough events to simulate surpassing the GAP threshold causing
        // an event to get dropped (version=16) and the processed events to be skipped
        // this test will get stuck on another gap (version=18) and when processed
        // it will skip all processed versions and all future versions

        for (int i = 0, c = aEvent.length; i < c; ++i)
            {
            MapEvent<Integer, String> event = aEvent[i] = mock(MapEvent.class);

            when(event.getPartition()).thenReturn(17);
            }

        // test surpassing the gap threshold both within the truncated version
        // and as the truncated version wraps / overflows
        for (long of : new long[]{15L, (VersionAwareListeners.DefaultVersionedListener.MASK_VERSION - GAP_THRESHOLD + 5)})
            {
            AtomicInteger atomicCounter = new AtomicInteger();

            MapListener<Integer, String> listener = new SimpleMapListener<Integer, String>().
                    addEventHandler(event -> atomicCounter.incrementAndGet()).versioned();

            MapListener<Integer, String> listenerVersioned = VersionAwareListeners.createListener(listener);

            for (int i = 0, c = aEvent.length; i < c; ++i)
                {
                MapEvent<Integer, String> event = aEvent[i];

                when(event.getVersion()).thenReturn(of + i);
                }

            // test not receiving 16 & 18 and make sure 16 gets dropped and 18 is processed

            listenerVersioned.entryInserted(aEvent[0]); // 15
            listenerVersioned.entryInserted(aEvent[2]); // 17

            long lExpectedVersion = 0xFFFFL + aEvent[1].getVersion(); // 16 || ?
            int  iProcessed       = 4;
            for (int c = aEvent.length; iProcessed < c && aEvent[iProcessed].getVersion() <= lExpectedVersion; ++iProcessed)
                {
                listenerVersioned.entryInserted(aEvent[iProcessed]);
                }

            listenerVersioned.entryInserted(aEvent[iProcessed++]); // cause the dropping of 16
            listenerVersioned.entryInserted(aEvent[3]);            // process version 18
            listenerVersioned.entryInserted(aEvent[iProcessed]);   // process last event ensuring it gets processed

            assertEquals(aEvent.length - 1, atomicCounter.get());
            }
        }
    }