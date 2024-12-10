/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package transformer;


import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.tangosol.net.NamedCache;

import com.tangosol.util.Filter;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.MapEvent;
import com.tangosol.util.MapEventTransformer;
import com.tangosol.util.MapListenerSupport;
import com.tangosol.util.MultiplexingMapListener;
import com.tangosol.util.SafeLinkedList;
import com.tangosol.util.ValueExtractor;
import com.tangosol.util.ValueManipulator;

import com.tangosol.util.extractor.IdentityExtractor;
import com.tangosol.util.extractor.MultiExtractor;
import com.tangosol.util.extractor.ReflectionExtractor;

import com.tangosol.util.filter.AlwaysFilter;
import com.tangosol.util.filter.MapEventFilter;
import com.tangosol.util.filter.MapEventTransformerFilter;

import com.tangosol.util.processor.NumberIncrementor;

import com.tangosol.util.transformer.ExtractorEventTransformer;
import com.tangosol.util.transformer.SemiLiteEventTransformer;

import com.oracle.coherence.testing.AbstractFunctionalTest;

import org.junit.Test;

import java.io.Serializable;

import java.util.List;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;


/**
* A collection of functional tests for the MapEventTransformer functionality.
*
* @author gg 2008.03.14
*/
public abstract class AbstractMapEventTransformerTests
        extends AbstractFunctionalTest
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Create a new AbstractMapEventTransformerTests that will use the cache
    * with the given name in all test methods.
    *
    * @param sCache  the test cache name
    */
    public AbstractMapEventTransformerTests(String sCache)
        {
        if (sCache == null || sCache.trim().length() == 0)
            {
            throw new IllegalArgumentException("Invalid cache name");
            }

        m_sCache = sCache.trim();
        }


    // ----- AbstractMapEventTransformerTests methods -----------------------

    /**
    * Return the cache used in all test methods.
    *
    * @return the test cache
    */
    protected NamedCache getNamedCache()
        {
        return getNamedCache(getCacheName());
        }


    // ----- test methods ---------------------------------------------------

    /**
    * Test of the put operations.
    */
    @Test
    public void testPut()
        {
        NamedCache cache = getNamedCache();
        cache.clear();

        SyncEventCollector collectorSemi  = new SyncEventCollector();
        cache.addMapListener(collectorSemi, s_transformerSemi, false);

        cache.put("1", Integer.valueOf(1));
        cache.put("1", Integer.valueOf(2));

        MapEvent event = collectorSemi.getLastEvent();
        assertTrue(event.toString(), event.getOldValue() == null);

        collectorSemi.clearEvents();

        SyncEventCollector collectorHeavy = new SyncEventCollector();
        cache.addMapListener(collectorHeavy);

        cache.put("1", Integer.valueOf(3));

        event = collectorSemi.getLastEvent();
        assertTrue(event.toString(), event.getOldValue() == null);

        event = collectorHeavy.getLastEvent();
        assertTrue(event.toString(), event.getOldValue() != null);

        SyncEventCollector collectorMutating = new SyncEventCollector();
        cache.addMapListener(collectorMutating, s_transformerMutating, false);

        SyncEventCollector collectorExtractor = new SyncEventCollector();
        cache.addMapListener(collectorExtractor, s_transformerExtractor, false);

        cache.put("1", Integer.valueOf(3));

        event = collectorSemi.getLastEvent();
        assertTrue(event.toString(), event.getOldValue() == null);

        event = collectorHeavy.getLastEvent();
        assertTrue(event.toString(), event.getOldValue() != null);

        event = collectorMutating.getLastEvent();
        assertTrue(event.toString(), String.valueOf(event.getKey()).startsWith("str-"));
        assertTrue(event.toString(), String.valueOf(event.getOldValue()).startsWith("str-"));
        assertTrue(event.toString(), String.valueOf(event.getNewValue()).startsWith("str-"));

        cache.put("1", Integer.valueOf(4));

        event = collectorExtractor.getLastEvent();

        assertTrue(event.toString(), event.getOldValue() instanceof List);
        assertTrue(event.toString(), event.getNewValue() instanceof List);
        assertTrue(event.toString(), ((List) event.getNewValue()).size() == 2);
        assertTrue(event.toString(), ((List) event.getNewValue()).get(0).equals(Integer.valueOf(4)));

        cache.removeMapListener(collectorHeavy);
        cache.removeMapListener(collectorSemi, s_transformerSemi);
        cache.removeMapListener(collectorMutating, s_transformerMutating);
        cache.removeMapListener(collectorExtractor, s_transformerExtractor);

        collectorSemi.clearEvents();
        collectorHeavy.clearEvents();
        collectorMutating.clearEvents();
        collectorExtractor.clearEvents();

        cache.put("1", Integer.valueOf(5));

        event = collectorHeavy.getLastEvent();
        assertTrue(String.valueOf(event), event == null);

        event = collectorSemi.getLastEvent();
        assertTrue(String.valueOf(event), event == null);

        event = collectorMutating.getLastEvent();
        assertTrue(String.valueOf(event), event == null);
        }

    /**
    * Test of the remove operations.
    */
    @Test
    public void testRemove()
        {
        NamedCache cache = getNamedCache();
        cache.clear();

        SyncEventCollector collectorSemi  = new SyncEventCollector();
        cache.addMapListener(collectorSemi, s_transformerSemi, false);

        cache.put("1", Integer.valueOf(1));
        cache.remove("1");

        MapEvent event = collectorSemi.getLastEvent();
        assertTrue(event.toString(), event.getOldValue() == null);

        collectorSemi.clearEvents();

        SyncEventCollector collectorHeavy = new SyncEventCollector();
        cache.addMapListener(collectorHeavy);

        cache.put("1", Integer.valueOf(1));
        cache.remove("1");

        event = collectorSemi.getLastEvent();
        assertTrue(event.toString(), event.getOldValue() == null);

        event = collectorHeavy.getLastEvent();
        assertTrue(event.toString(), event.getOldValue() != null);

        cache.removeMapListener(collectorHeavy);
        cache.removeMapListener(collectorSemi, s_transformerSemi);

        collectorSemi.clearEvents();
        collectorHeavy.clearEvents();

        cache.put("1", Integer.valueOf(1));
        cache.remove("1");

        event = collectorHeavy.getLastEvent();
        assertTrue(String.valueOf(event), event == null);

        event = collectorSemi.getLastEvent();
        assertTrue(String.valueOf(event), event == null);
        }

    /**
    * Test of the invoke operations.
    */
    @Test
    public void testInvoke()
        {
        NamedCache cache = getNamedCache();
        cache.clear();

        InvocableMap.EntryProcessor agent = new NumberIncrementor(
            (ValueManipulator) null, Integer.valueOf(1), false);

        SyncEventCollector collectorSemi  = new SyncEventCollector();
        cache.addMapListener(collectorSemi, s_transformerSemi, false);

        cache.put("1", Integer.valueOf(1));
        cache.invoke("1", agent);

        MapEvent event = collectorSemi.getLastEvent();
        assertTrue(event.toString(), event.getOldValue() == null);

        collectorSemi.clearEvents();

        SyncEventCollector collectorHeavy = new SyncEventCollector();
        cache.addMapListener(collectorHeavy);

        cache.invoke("1", agent);

        event = collectorSemi.getLastEvent();
        assertTrue(event.toString(), event.getOldValue() == null);

        event = collectorHeavy.getLastEvent();
        assertTrue(event.toString(), event.getOldValue() != null);

        cache.removeMapListener(collectorHeavy);
        cache.removeMapListener(collectorSemi, s_transformerSemi);

        collectorSemi.clearEvents();
        collectorHeavy.clearEvents();

        cache.invoke("1", agent);

        event = collectorHeavy.getLastEvent();
        assertTrue(String.valueOf(event), event == null);

        event = collectorSemi.getLastEvent();
        assertTrue(String.valueOf(event), event == null);
        }

    @Test
    public void testCoh9355Sync()
        {
        NamedCache cache = getNamedCache();
        cache.clear();

        EventCollector collectorKey    = new SyncEventCollector();
        EventCollector collectorFilter = new SyncEventCollector();
        EventCollector collectorTrans  = new SyncEventCollector();

        cache.addMapListener(collectorKey, "key1", false);
        cache.addMapListener(collectorFilter, AlwaysFilter.INSTANCE, false);
        cache.addMapListener(collectorTrans, new MapEventTransformerFilter(AlwaysFilter.INSTANCE, new SemiLiteEventTransformer()), false);

        cache.put("key1", 1);
        assertSingleEvent(collectorKey,    new MapEventFilter(MapEventFilter.E_INSERTED), true);
        assertSingleEvent(collectorFilter, new MapEventFilter(MapEventFilter.E_INSERTED), true);
        assertSingleEvent(collectorTrans,  new MapEventFilter(MapEventFilter.E_INSERTED), true);

        cache.put("key1", 2);
        assertSingleEvent(collectorKey,    new MapEventFilter(MapEventFilter.E_UPDATED), true);
        assertSingleEvent(collectorFilter, new MapEventFilter(MapEventFilter.E_UPDATED), true);
        assertSingleEvent(collectorTrans,  new MapEventFilter(MapEventFilter.E_UPDATED), true);

        cache.remove("key1");
        assertSingleEvent(collectorKey,    new MapEventFilter(MapEventFilter.E_DELETED), true);
        assertSingleEvent(collectorFilter, new MapEventFilter(MapEventFilter.E_DELETED), true);
        assertSingleEvent(collectorTrans,  new MapEventFilter(MapEventFilter.E_DELETED), true);
        }

    @Test
    public void testCoh9355Async()
        {
        NamedCache cache = getNamedCache();
        cache.clear();

        EventCollector collectorKey    = new EventCollector();
        EventCollector collectorFilter = new EventCollector();
        EventCollector collectorTrans  = new EventCollector();

        cache.addMapListener(collectorKey, "key1", false);
        cache.addMapListener(collectorFilter, AlwaysFilter.INSTANCE, false);
        cache.addMapListener(collectorTrans, new MapEventTransformerFilter(AlwaysFilter.INSTANCE, new SemiLiteEventTransformer()), false);

        cache.put("key1", 1);

        Eventually.assertThat(invoking(this).isSingleEvent(collectorKey, new MapEventFilter(MapEventFilter.E_INSERTED), true), is(true));
        Eventually.assertThat(invoking(this).isSingleEvent(collectorFilter, new MapEventFilter(MapEventFilter.E_INSERTED), true), is(true));
        Eventually.assertThat(invoking(this).isSingleEvent(collectorTrans,  new MapEventFilter(MapEventFilter.E_INSERTED), true), is(true));

        cache.put("key1", 2);

        Eventually.assertThat(invoking(this).isSingleEvent(collectorKey, new MapEventFilter(MapEventFilter.E_UPDATED), true), is(true));
        Eventually.assertThat(invoking(this).isSingleEvent(collectorFilter, new MapEventFilter(MapEventFilter.E_UPDATED), true), is(true));
        Eventually.assertThat(invoking(this).isSingleEvent(collectorTrans, new MapEventFilter(MapEventFilter.E_UPDATED), true), is(true));

        cache.remove("key1");

        Eventually.assertThat(invoking(this).isSingleEvent(collectorKey, new MapEventFilter(MapEventFilter.E_DELETED), true), is(true));
        Eventually.assertThat(invoking(this).isSingleEvent(collectorFilter, new MapEventFilter(MapEventFilter.E_DELETED), true), is(true));
        Eventually.assertThat(invoking(this).isSingleEvent(collectorTrans, new MapEventFilter(MapEventFilter.E_DELETED), true), is(true));
        }

    // ----- helper methods ------------------------------------------------

    protected void assertSingleEvent(EventCollector collector, Filter filter, boolean fClear)
        {
        assertTrue(isSingleEvent(collector, filter, fClear));
        }

    public boolean isSingleEvent(EventCollector collector, Filter filter, boolean fClear)
        {
        if (collector.m_list.size() == 1)
            {
            if (filter.evaluate(collector.getLastEvent()))
                {
                if (fClear)
                    {
                    collector.clearEvents();
                    }
                return true;
                }
            }

        return false;
        }

    // ----- inner classes -------------------------------------------------

    public static class EventCollector
            extends MultiplexingMapListener
        {
        public EventCollector()
            {
            this(new SafeLinkedList());
            }

        public EventCollector(List listEvents)
            {
            m_list = listEvents;
            }

        protected void onMapEvent(MapEvent evt)
            {
            m_list.add(evt);
            }

        public void clearEvents()
            {
            m_list.clear();
            }

        public MapEvent getLastEvent()
            {
            List list = m_list;
            int c = list.size();
            return c == 0 ? null : (MapEvent) list.get(c-1);
            }

        protected List m_list;
        }

    public static class SyncEventCollector
            extends EventCollector
            implements MapListenerSupport.SynchronousListener
        {
        public SyncEventCollector()
            {
            super();
            }

        public SyncEventCollector(List listEvents)
            {
            super(listEvents);
            }
        }

   public static class StringEventTransformer
            implements MapEventTransformer, Serializable
        {
        public MapEvent transform(MapEvent evt)
            {
            return new MapEvent(evt.getMap(), evt.getId(),
                "str-" + evt.getKey(),
                "str-" + evt.getOldValue(),
                "str-" + evt.getNewValue());
            }

        public int hashCode()
            {
            return 1;
            }
        public boolean equals(Object o)
            {
            return o instanceof StringEventTransformer;
            }
        }


    // ----- constants ------------------------------------------------------

    public final static Filter s_transformerSemi =
        new MapEventTransformerFilter(AlwaysFilter.INSTANCE,
            SemiLiteEventTransformer.INSTANCE);

    public final static Filter s_transformerMutating =
        new MapEventTransformerFilter(null, new StringEventTransformer());

    public final static Filter s_transformerExtractor =
        new MapEventTransformerFilter(null,
            new ExtractorEventTransformer(
                new MultiExtractor(
                    new ValueExtractor[]
                        {
                        IdentityExtractor.INSTANCE,
                        new ReflectionExtractor("hashCode"),
                        }
                )));


    // ----- accessors ------------------------------------------------------

    /**
    * Return the name of the cache used in all test methods.
    *
    * @return the name of the cache used in all test methods
    */
    protected String getCacheName()
        {
        return m_sCache;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The name of the cache used in all test methods.
    */
    protected final String m_sCache;
    }