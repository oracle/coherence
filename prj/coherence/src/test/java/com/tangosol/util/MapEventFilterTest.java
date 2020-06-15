/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


import com.tangosol.util.extractor.IdentityExtractor;
import com.tangosol.util.extractor.KeyExtractor;
import com.tangosol.util.filter.EqualsFilter;
import com.tangosol.util.filter.InFilter;
import com.tangosol.util.filter.MapEventFilter;
import com.tangosol.util.filter.GreaterFilter;

import com.tangosol.net.NamedCache;

import com.tangosol.net.cache.WrapperNamedCache;

import java.util.HashSet;
import java.util.Set;
import org.junit.Test;

import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;
import java.io.DataOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;

import static org.junit.Assert.*;


/**
 * A collection of unit tests for various methods of {@link MapEventFilter}.
 *
 * @author mf 2006.01.19
 */
public class MapEventFilterTest
       extends Base
    {
    /**
     * Wrapper for comparing MapEvents.
     */
    public static class ComparableEvent
        {
        public ComparableEvent(MapEvent event)
            {
            m_event = event;
            }


        public boolean equals(Object o)
            {
            if (!(o instanceof ComparableEvent))
                {
                return false;
                }
            MapEvent e1 = m_event;
            MapEvent e2 = ((ComparableEvent) o).getEvent();

            return  e1.getId() == e2.getId() &&
                    e1.getMap() == e2.getMap() &&
                    Base.equals(e1.getKey(), e2.getKey()) &&
                    Base.equals(e1.getOldValue(), e2.getOldValue()) &&
                    Base.equals(e1.getNewValue(), e2.getNewValue());
            }

        public int hashCode()
            {
            MapEvent e = m_event;
            return e.getId() +
                   e.getMap().hashCode() +
                   e.getKey().hashCode() +
                   (e.getOldValue() == null ? 0 : e.getOldValue().hashCode()) +
                   (e.getNewValue() == null ? 0 : e.getNewValue().hashCode());
            }

        public MapEvent getEvent()
            {
            return m_event;
            }

        public String toString()
            {
            return m_event.toString();
            }

        protected MapEvent m_event;
        }

    /**
     * MapEvent listener that compares received events against a list of
     * expected events, and asserts if an unexpected event is received.
     */
    public static class AssertingMapEventListener
           extends MultiplexingMapListener
        {

        /**
         * Construct with a list of expected events.
         * @param expectedEvents
         */
        public AssertingMapEventListener(List expectedEvents)
            {
            m_expectedEvents = expectedEvents;
            }

        /**
         * Helper which constructs from an array of expected events.
         * @param aExpectedEvent
         */
        public AssertingMapEventListener(MapEvent[] aExpectedEvent)
            {
            List expectedEvents = new ArrayList(aExpectedEvent.length);
            for (int i = 0, c = aExpectedEvent.length; i < c; ++i)
                {
                expectedEvents.add(new ComparableEvent(aExpectedEvent[i]));
                }
            m_expectedEvents = expectedEvents;
            }

        /**
         * Helper which constructs from a single expected event.
         * @param expectedEvent
         */
        public AssertingMapEventListener(MapEvent expectedEvent)
            {
            this(new MapEvent[]{expectedEvent});
            }

        /**
         * Process received event, asserting that it is the next expected event.
         * @param evt
         */
        public void onMapEvent(MapEvent evt)
            {
            ComparableEvent evtExpected = (m_expectedEvents.isEmpty() ? null :
                (ComparableEvent) m_expectedEvents.remove(0));


            ComparableEvent compEvt = new ComparableEvent(evt);
            if(!compEvt.equals(evtExpected))
                {
                fail("Unexpected event = " + compEvt +
                     "\nnext expected = " + evtExpected +
                     "\nrecorded = " + m_recordedEvents);
                }

            m_recordedEvents.add(compEvt);
            }

        /**
         * Final check to ensure that all expected events have been received.
         */
        public void assertState()
            {
            // simply check that all expected events were received
            assertTrue( m_expectedEvents.size() +
                    " expected event(s) did not occur.\n" + m_expectedEvents,
                    m_expectedEvents.isEmpty());
            }

        protected List m_expectedEvents;
        protected List m_recordedEvents = new LinkedList();
        }

    /**
    * Instantiate a NamedCache for the test.
    */
    protected NamedCache instantiateTestCache()
        {
        WrapperNamedCache cache = new WrapperNamedCache(new ObservableHashMap(), "test");
        // starting with Coherence 3.3 the WrapperNamedCache does not translate
        // events by default
        cache.setTranslateEvents(true);
        return cache;
        }

    /**
    * Generate an insert event.
    */
    @Test
    public void eventInsertFilter()
        {
        NamedCache map = instantiateTestCache();
        Object oKey = "foo";
        Object oOrigValue = new Integer(9);
        Object oUpdatedValue = new Integer(10);
        MapEvent[] aExpectedEvents = {
            new MapEvent(map, MapEvent.ENTRY_INSERTED, oKey, null, oOrigValue)
            };
        AssertingMapEventListener listener = new AssertingMapEventListener(aExpectedEvents);
        map.addMapListener(listener,
                           new MapEventFilter(MapEventFilter.E_INSERTED,
                                              new GreaterFilter("intValue", new Integer(5))),
                           false);
        map.put(oKey, oOrigValue);
        map.put(oKey, oUpdatedValue);
        map.remove(oKey);
        listener.assertState();
        }

    /**
    * Generate an insert event.
    */
    @Test
    public void eventInsertFilter_Negative()
        {
        NamedCache map = instantiateTestCache();
        Object oKey = "foo";
        Object oOrigValue = new Integer(1);
        Object oUpdatedValue = new Integer(10);
        MapEvent[] aExpectedEvents = {};
        AssertingMapEventListener listener = new AssertingMapEventListener(aExpectedEvents);
        map.addMapListener(listener,
                           new MapEventFilter(MapEventFilter.E_INSERTED,
                                              new GreaterFilter("intValue", new Integer(5))),
                           false);
        map.put(oKey, oOrigValue);
        map.put(oKey, oUpdatedValue);
        map.remove(oKey);
        listener.assertState();
        }

    /**
    * Generate an insert event.
    */
    @Test
    public void eventInsertFilter_all()
        {
        NamedCache map = instantiateTestCache();
        Object oKey = "foo";
        Object oOrigValue = new Integer(1);
        Object oUpdatedValue = new Integer(10);
        MapEvent[] aExpectedEvents = {
            new MapEvent(map, MapEvent.ENTRY_INSERTED, oKey, null, oOrigValue)
            };
        AssertingMapEventListener listener = new AssertingMapEventListener(aExpectedEvents);
        map.addMapListener(listener,
                           new MapEventFilter(MapEventFilter.E_INSERTED,
                                              null),
                           false);
        map.put(oKey, oOrigValue);
        map.put(oKey, oUpdatedValue);
        map.remove(oKey);
        listener.assertState();
        }

    /**
    * Generate an insert event.
    */
    @Test
    public void eventDeleteFilter()
        {
        NamedCache map = instantiateTestCache();
        Object oKey = "foo";
        Object oOrigValue = new Integer(1);
        Object oUpdatedValue = new Integer(10);
        MapEvent[] aExpectedEvents = {
            new MapEvent(map, MapEvent.ENTRY_DELETED, oKey, oUpdatedValue, null)
            };
        AssertingMapEventListener listener = new AssertingMapEventListener(aExpectedEvents);
        map.addMapListener(listener,
                           new MapEventFilter(MapEventFilter.E_DELETED,
                                              new GreaterFilter("intValue", new Integer(5))),
                           false);
        map.put(oKey, oOrigValue);
        map.put(oKey, oUpdatedValue);
        map.remove(oKey);

        listener.assertState();
        }

    /**
    * Generate an insert event.
    */
    @Test
    public void eventDeleteFilter_Negative()
        {
        NamedCache map = instantiateTestCache();
        Object oKey = "foo";
        Object oOrigValue = new Integer(10);
        Object oUpdatedValue = new Integer(1);
        MapEvent[] aExpectedEvents = {};
        AssertingMapEventListener listener = new AssertingMapEventListener(aExpectedEvents);
        map.addMapListener(listener,
                           new MapEventFilter(MapEventFilter.E_DELETED,
                                              new GreaterFilter("intValue", new Integer(5))),
                           false);
        map.put(oKey, oOrigValue);
        map.put(oKey, oUpdatedValue);
        map.remove(oKey);
        listener.assertState();
        }

    /**
    * Generate an insert event.
    */
    @Test
    public void eventDeleteFilter_all()
        {
        NamedCache map = instantiateTestCache();
        Object oKey = "foo";
        Object oOrigValue = new Integer(1);
        Object oUpdatedValue = new Integer(10);
        MapEvent[] aExpectedEvents = {
            new MapEvent(map, MapEvent.ENTRY_DELETED, oKey, oUpdatedValue, null)
            };
        AssertingMapEventListener listener = new AssertingMapEventListener(aExpectedEvents);
        map.addMapListener(listener,
                           new MapEventFilter(MapEventFilter.E_DELETED,
                                              null),
                           false);
        map.put(oKey, oOrigValue);
        map.put(oKey, oUpdatedValue);
        map.remove(oKey);

        listener.assertState();
        }


   /**
    * Generate an insert event.
    */
    @Test
    public void eventUpdateFilter_Pre()
        {
        NamedCache map = instantiateTestCache();
        Object oKey = "foo";
        Object oOrigValue = new Integer(10);
        Object oUpdatedValue = new Integer(1);
        MapEvent[] aExpectedEvents = {
                new MapEvent(map, MapEvent.ENTRY_UPDATED, oKey, oOrigValue, oUpdatedValue)
            };
        AssertingMapEventListener listener = new AssertingMapEventListener(aExpectedEvents);
        map.addMapListener(listener,
                           new MapEventFilter(MapEventFilter.E_UPDATED,
                                              new GreaterFilter("intValue", new Integer(5))),
                           false);
        map.put(oKey, oOrigValue);
        map.put(oKey, oUpdatedValue);
        map.remove(oKey);

        listener.assertState();
        }

   /**
    * Generate an insert event.
    */
    @Test
    public void eventUpdateFilter_Post()
        {
        NamedCache map = instantiateTestCache();
        Object oKey = "foo";
        Object oOrigValue = new Integer(1);
        Object oUpdatedValue = new Integer(10);
        MapEvent[] aExpectedEvents = {
                new MapEvent(map, MapEvent.ENTRY_UPDATED, oKey, oOrigValue, oUpdatedValue)
            };
        AssertingMapEventListener listener = new AssertingMapEventListener(aExpectedEvents);
        map.addMapListener(listener,
                           new MapEventFilter(MapEventFilter.E_UPDATED,
                                              new GreaterFilter("intValue", new Integer(5))),
                           false);
        map.put(oKey, oOrigValue);
        map.put(oKey, oUpdatedValue);
        map.remove(oKey);

        listener.assertState();
        }

   /**
    * Generate an insert event.
    */
    @Test
    public void eventUpdateFilter_Negative()
        {
        NamedCache map = instantiateTestCache();
        Object oKey = "foo";
        Object oOrigValue = new Integer(1);
        Object oUpdatedValue = new Integer(2);
        MapEvent[] aExpectedEvents = {};
        AssertingMapEventListener listener = new AssertingMapEventListener(aExpectedEvents);
        map.addMapListener(listener,
                           new MapEventFilter(MapEventFilter.E_UPDATED,
                                              new GreaterFilter("intValue", new Integer(5))),
                           false);
        map.put(oKey, oOrigValue);
        map.put(oKey, oUpdatedValue);
        map.remove(oKey);

        listener.assertState();
        }

   /**
    * Generate an insert event.
    */
    @Test
    public void eventUpdateFilter_all()
        {
        NamedCache map = instantiateTestCache();
        Object oKey = "foo";
        Object oOrigValue = new Integer(1);
        Object oUpdatedValue = new Integer(2);
        MapEvent[] aExpectedEvents = {
                new MapEvent(map, MapEvent.ENTRY_UPDATED, oKey, oOrigValue, oUpdatedValue)
            };
        AssertingMapEventListener listener = new AssertingMapEventListener(aExpectedEvents);
        map.addMapListener(listener,
                           new MapEventFilter(MapEventFilter.E_UPDATED,
                                              null),
                           false);
        map.put(oKey, oOrigValue);
        map.put(oKey, oUpdatedValue);
        map.remove(oKey);
        listener.assertState();
        }

   /**
    * Generate an insert event.
    */
    @Test
    public void eventEnteringFilter()
        {
        NamedCache map = instantiateTestCache();
        Object oKey = "foo";
        Object oOrigValue = new Integer(1);
        Object oUpdatedValue = new Integer(10);
        MapEvent[] aExpectedEvents = {
                new MapEvent(map, MapEvent.ENTRY_UPDATED, oKey, oOrigValue, oUpdatedValue)
            };
        AssertingMapEventListener listener = new AssertingMapEventListener(aExpectedEvents);
        map.addMapListener(listener,
                           new MapEventFilter(MapEventFilter.E_UPDATED_ENTERED,
                                              new GreaterFilter("intValue", new Integer(5))),
                           false);
        map.put(oKey, oOrigValue);
        map.put(oKey, oUpdatedValue);
        map.remove(oKey);

        listener.assertState();
        }

   /**
    * Generate an insert event.
    */
    @Test
    public void eventEnteringFilter_Negative()
        {
        NamedCache map = instantiateTestCache();
        Object oKey = "foo";
        Object oOrigValue = new Integer(10);
        Object oUpdatedValue = new Integer(1);
        MapEvent[] aExpectedEvents = {};
        AssertingMapEventListener listener = new AssertingMapEventListener(aExpectedEvents);
        map.addMapListener(listener,
                           new MapEventFilter(MapEventFilter.E_UPDATED_ENTERED,
                                              new GreaterFilter("intValue", new Integer(5))),
                           false);
        map.put(oKey, oOrigValue);
        map.put(oKey, oUpdatedValue);
        map.remove(oKey);

        listener.assertState();
        }


    /**
     * Generate an insert event.
     */
    @Test
     public void eventEnteringFilter_all()
         {
         NamedCache map = instantiateTestCache();
         Object oKey = "foo";
         Object oOrigValue = new Integer(10);
         Object oUpdatedValue = new Integer(1);
         MapEvent[] aExpectedEvents = {
                new MapEvent(map, MapEvent.ENTRY_UPDATED, oKey, oOrigValue, oUpdatedValue)
            };
         AssertingMapEventListener listener = new AssertingMapEventListener(aExpectedEvents);
         map.addMapListener(listener,
                            new MapEventFilter(MapEventFilter.E_UPDATED_ENTERED,
                                               null),
                            false);
         map.put(oKey, oOrigValue);
         map.put(oKey, oUpdatedValue);
         map.remove(oKey);

         listener.assertState();
         }

    /**
     * Generate an insert event.
     */
    @Test
     public void eventLeavingFilter()
         {
         NamedCache map = instantiateTestCache();
         Object oKey = "foo";
         Object oOrigValue = new Integer(10);
         Object oUpdatedValue = new Integer(1);
         MapEvent[] aExpectedEvents = {
                 new MapEvent(map, MapEvent.ENTRY_UPDATED, oKey, oOrigValue, oUpdatedValue)
             };
         AssertingMapEventListener listener = new AssertingMapEventListener(aExpectedEvents);
         map.addMapListener(listener,
                            new MapEventFilter(MapEventFilter.E_UPDATED_LEFT,
                                               new GreaterFilter("intValue", new Integer(5))),
                            false);
         map.put(oKey, oOrigValue);
         map.put(oKey, oUpdatedValue);
         map.remove(oKey);

         listener.assertState();
         }


    /**
     * Generate an insert event.
     */
    @Test
     public void eventLeavingFilter_Negative()
         {
         NamedCache map = instantiateTestCache();
         Object oKey = "foo";
         Object oOrigValue = new Integer(1);
         Object oUpdatedValue = new Integer(2);
         MapEvent[] aExpectedEvents = {};
         AssertingMapEventListener listener = new AssertingMapEventListener(aExpectedEvents);
         map.addMapListener(listener,
                            new MapEventFilter(MapEventFilter.E_UPDATED_LEFT,
                                               new GreaterFilter("intValue", new Integer(5))),
                            false);
         map.put(oKey, oOrigValue);
         map.put(oKey, oUpdatedValue);
         map.remove(oKey);

         listener.assertState();
         }

    /**
     * Generate an insert event.
     */
    @Test
     public void eventLeavingFilter_all()
         {
         NamedCache map = instantiateTestCache();
         Object oKey = "foo";
         Object oOrigValue = new Integer(1);
         Object oUpdatedValue = new Integer(10);
         MapEvent[] aExpectedEvents = {
                 new MapEvent(map, MapEvent.ENTRY_UPDATED, oKey, oOrigValue, oUpdatedValue)
             };
         AssertingMapEventListener listener = new AssertingMapEventListener(aExpectedEvents);
         map.addMapListener(listener,
                            new MapEventFilter(MapEventFilter.E_UPDATED_LEFT,
                                               null),
                            false);
         map.put(oKey, oOrigValue);
         map.put(oKey, oUpdatedValue);
         map.remove(oKey);

         listener.assertState();
         }


    /**
     * Generate an insert event.
     */
    @Test
    public void eventUpdatedWithinFilter()
        {
        NamedCache map = instantiateTestCache();
        Object oKey = "foo";
        Object[] oValue = {
                new Integer(1),
                new Integer(10),
                new Integer(11),
                new Integer(3)
        };
        MapEvent[] aExpectedEvents = {
                new MapEvent(map, MapEvent.ENTRY_UPDATED,  oKey, new Integer(10), new Integer(11))
        };
        AssertingMapEventListener listener = new AssertingMapEventListener(aExpectedEvents);
        map.addMapListener(listener,
                new MapEventFilter(MapEventFilter.E_UPDATED_WITHIN,
                        new GreaterFilter("intValue", new Integer(5))),
                false);
        for (int i = 0, c = oValue.length; i < c; ++i)
            {
            map.put(oKey, oValue[i]);
            }
        map.remove(oKey);
        listener.assertState();
        }


     /**
     * Generate an insert event.
     *
     */
     @Test
     public void eventIDUFilter()
         {
         NamedCache map = instantiateTestCache();
         Object oKey = "foo";
         Object[] oValue = {
                 new Integer(1),
                 new Integer(2),
                 new Integer(3)
         };
         MapEvent[] aExpectedEvents = {
                 new MapEvent(map, MapEvent.ENTRY_INSERTED, oKey, null,      oValue[0]),
                 new MapEvent(map, MapEvent.ENTRY_UPDATED,  oKey, oValue[0], oValue[1]),
                 new MapEvent(map, MapEvent.ENTRY_UPDATED,  oKey, oValue[1], oValue[2]),
                 new MapEvent(map, MapEvent.ENTRY_DELETED,  oKey, oValue[2], null),
                 new MapEvent(map, MapEvent.ENTRY_INSERTED, oKey, null,      oValue[0]),
                 new MapEvent(map, MapEvent.ENTRY_UPDATED,  oKey, oValue[0], oValue[1]),
                 new MapEvent(map, MapEvent.ENTRY_UPDATED,  oKey, oValue[1], oValue[2]),
                 new MapEvent(map, MapEvent.ENTRY_DELETED,  oKey, oValue[2], null)
             };
         AssertingMapEventListener listener = new AssertingMapEventListener(aExpectedEvents);
         map.addMapListener(listener,
                            new MapEventFilter(MapEventFilter.E_INSERTED|
                                               MapEventFilter.E_DELETED |
                                               MapEventFilter.E_UPDATED,
                                               null),
                            false);
         for (int i = 0, c = oValue.length; i < c; ++i)
             {
             map.put(oKey, oValue[i]);
             }
         map.remove(oKey);
         for (int i = 0, c = oValue.length; i < c; ++i)
             {
             map.put(oKey, oValue[i]);
             }
         map.remove(oKey);
         listener.assertState();
         }

    /**
     * Generate an insert event.
     */
    @Test
     public void eventFilter_within()
         {
         NamedCache map = instantiateTestCache();
         Object oKey = "foo";
         Object[] oValue = {
                 new Integer(1),
                 new Integer(2),
                 new Integer(7),
                 new Integer(8),
                 new Integer(3)
         };
         MapEvent[] aExpectedEvents = {
                 new MapEvent(map, MapEvent.ENTRY_UPDATED,  oKey, new Integer(2), new Integer(7)),
                 new MapEvent(map, MapEvent.ENTRY_UPDATED,  oKey, new Integer(7), new Integer(8)),
                 new MapEvent(map, MapEvent.ENTRY_UPDATED,  oKey, new Integer(8), new Integer(3))
             };
         AssertingMapEventListener listener = new AssertingMapEventListener(aExpectedEvents);
         // note this event mask evaulates to the same as E_UPDATED
         map.addMapListener(listener,
                            new MapEventFilter(MapEventFilter.E_UPDATED_ENTERED |
                                               MapEventFilter.E_UPDATED_LEFT |
                                               MapEventFilter.E_UPDATED,
                                               new GreaterFilter("intValue", new Integer(5))),
                            false);
         for (int i = 0, c = oValue.length; i < c; ++i)
             {
             map.put(oKey, oValue[i]);
             }
         map.remove(oKey);

         listener.assertState();
         }

    /**
     * Generate an insert event.
     */
    @Test
     public void eventEneteredFilter_within()
         {
         NamedCache map = instantiateTestCache();
         Object oKey = "foo";
         Object[] oValue = {
                 new Integer(1),
                 new Integer(2),
                 new Integer(7),
                 new Integer(8),
                 new Integer(3)
         };
         MapEvent[] aExpectedEvents = {
                 new MapEvent(map, MapEvent.ENTRY_UPDATED,  oKey, new Integer(2), new Integer(7)),
                 new MapEvent(map, MapEvent.ENTRY_UPDATED,  oKey, new Integer(7), new Integer(8)),
             };
         AssertingMapEventListener listener = new AssertingMapEventListener(aExpectedEvents);
         map.addMapListener(listener,
                            new MapEventFilter(MapEventFilter.E_UPDATED_ENTERED |
                                               MapEventFilter.E_UPDATED_WITHIN,
                                               new GreaterFilter("intValue", new Integer(5))),
                            false);
         for (int i = 0, c = oValue.length; i < c; ++i)
             {
             map.put(oKey, oValue[i]);
             }
         map.remove(oKey);

         listener.assertState();
         }

    /**
     * Generate an insert event.
     */
    @Test
     public void eventLeftFilter_within()
         {
         NamedCache map = instantiateTestCache();
         Object oKey = "foo";
         Object[] oValue = {
                 new Integer(1),
                 new Integer(2),
                 new Integer(7),
                 new Integer(8),
                 new Integer(3)
         };
         MapEvent[] aExpectedEvents = {
                 new MapEvent(map, MapEvent.ENTRY_UPDATED,  oKey, new Integer(7), new Integer(8)),
                 new MapEvent(map, MapEvent.ENTRY_UPDATED,  oKey, new Integer(8), new Integer(3)),
             };
         AssertingMapEventListener listener = new AssertingMapEventListener(aExpectedEvents);
         map.addMapListener(listener,
                            new MapEventFilter(MapEventFilter.E_UPDATED_LEFT |
                                               MapEventFilter.E_UPDATED_WITHIN,
                                               new GreaterFilter("intValue", new Integer(5))),
                            false);
         for (int i = 0, c = oValue.length; i < c; ++i)
             {
             map.put(oKey, oValue[i]);
             }
         map.remove(oKey);

         listener.assertState();
         }

    /**
     * Test filter serialization.
     */
    @Test
     public void filter_serialize()
            throws IOException
           {
           MapEventFilter mef = new MapEventFilter(MapEventFilter.E_UPDATED_LEFT |
                                               MapEventFilter.E_UPDATED_WITHIN,
                                               new GreaterFilter("intValue", new Integer(5)));

           ByteArrayOutputStream baos = new ByteArrayOutputStream();
           DataOutputStream out = new DataOutputStream(baos);
           ExternalizableHelper.writeObject(out, mef);

           ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
           DataInputStream in = new DataInputStream(bais);
           MapEventFilter mef2 = (MapEventFilter) ExternalizableHelper.readObject(in);

           assertTrue(mef.equals(mef2));
           }

    /**
     * Test for MapEventFilter using KeyExtractor.
     */
    @Test
    public void testMapEventFilterUsingKeyExtractor()
        {
        NamedCache<Integer, String> map = instantiateTestCache();

        Integer key1 = Integer.valueOf(1);
        String value1 = "one";
        String updatedValue1 = "ONE";
        Integer key2 = Integer.valueOf(2);
        String value2 = "two";
        String updatedValue2 = "TWO";

        MapEvent[] aExpectedEvents = {
            new MapEvent<Integer, String>(map, MapEvent.ENTRY_INSERTED, key1, null, value1)
        };
        AssertingMapEventListener listener = new AssertingMapEventListener(aExpectedEvents);
        Set<Integer> keySet = new HashSet<>();
        keySet.add(key1);
        Filter<String> filter =
            new InFilter<>(new KeyExtractor<>(IdentityExtractor.<String>INSTANCE()), keySet);

        map.addMapListener(listener,
                           new MapEventFilter<Integer, String>(MapEventFilter.E_INSERTED, filter),
                           false);

        map.put(key1, value1);
        map.put(key1, updatedValue1);
        map.remove(key1);

        map.put(key2, value2);
        map.put(key2, updatedValue2);
        map.remove(key2);

        listener.assertState();
        }

    /**
     * Test for MapEventFilter with Value Type.
     */
    @Test
    public void testMapEvenFiltertWithValueType()
        {
        NamedCache<Integer, String> map = instantiateTestCache();
        Integer key1 = Integer.valueOf(1);
        String value1 = "one";
        String updatedValue1 = "ONE";
        Integer key2 = Integer.valueOf(2);
        String value2 = "two";
        String updatedValue2 = "TWO";

        MapEvent[] aExpectedEvents = {
            new MapEvent<Integer, String>(map, MapEvent.ENTRY_INSERTED, key1, null, value1)
        };
        AssertingMapEventListener listener = new AssertingMapEventListener(aExpectedEvents);
        Filter<String> filter = new EqualsFilter<String, String>("toString", value1);

        map.addMapListener(listener,
                           new MapEventFilter<Integer, String>(MapEvent.ENTRY_INSERTED, filter),
                           false);

        map.put(key1, value1);
        map.put(key1, updatedValue1);
        map.remove(key1);

        map.put(key2, value2);
        map.put(key2, updatedValue2);
        map.remove(key2);

        listener.assertState();
        }
    }
