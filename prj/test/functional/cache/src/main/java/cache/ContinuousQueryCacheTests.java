/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package cache;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.tangosol.internal.net.NamedCacheDeactivationListener;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.net.cache.ContinuousQueryCache;
import com.tangosol.util.Base;
import com.tangosol.util.MapEvent;
import com.tangosol.util.MultiplexingMapListener;
import com.tangosol.util.ValueExtractor;
import com.tangosol.util.ValueUpdater;
import com.tangosol.util.filter.AlwaysFilter;
import com.tangosol.util.filter.EqualsFilter;
import com.tangosol.util.processor.UpdaterProcessor;

import com.oracle.coherence.common.base.Blocking;
import org.junit.Test;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author jk 2014.08.22
 */
@SuppressWarnings({"DuplicatedCode", "unchecked", "rawtypes"})
public class ContinuousQueryCacheTests
    {

    /**
     * Regression test for COH-3847
     */
    @Test
    public void testCoh3847()
        {
        int        cItems = 25;
        NamedCache cache  = CacheFactory.getCache("test");
        Map mapInitial    = new HashMap();

        for (int i = 0; i < cItems; i++)
            {
            mapInitial.put(i, new Boolean[] { Boolean.FALSE });
            }
        cache.putAll(mapInitial);

        // start the cache updating thread before creating the CQC
        Thread thdUpdate = new Thread()
            {
            public void run()
                {
                NamedCache cache  = CacheFactory.getCache("test");
                Set keys = cache.keySet();
                for (Object key : keys)
                    {
                    try
                        {
                        // Increase sleep time to 50, problem goes away
                        Blocking.sleep(1);
                        }
                    catch (InterruptedException e)
                        {
                        throw Base.ensureRuntimeException(e);
                        }

                    cache.invoke(key, new UpdaterProcessor(new TestValueUpdater(), Boolean.TRUE));
                    }
                }
            };
        thdUpdate.start();

        // Now create the CQC
        ContinuousQueryCache cqc = new ContinuousQueryCache(
                    cache, new EqualsFilter(new ValueExtractor()
                        {
                        public Object extract(Object oTarget)
                            {
                            return ((Boolean[]) oTarget)[0];
                            }
                        }, Boolean.FALSE));

        // Allow the update thread to complete
        try
            {
            thdUpdate.join();
            }
        catch (InterruptedException e)
            {
            throw Base.ensureRuntimeException(e);
            }

        // The cqc should be completely empty
        Eventually.assertThat(invoking(cqc).keySet().toArray(), is(new Object[0]));
        }

    @Test
    public void shouldCorrectlyHandleTruncateEvent()
        {
        NamedCache<String, String> cache = CacheFactory.getCache("test-two");
        cache.put("key", "value");

        ContinuousQueryCache<String, String, String> cqc = new ContinuousQueryCache<>(cache, AlwaysFilter.INSTANCE());

        Listener<String, String> listenerCQC                     = new Listener<>();
        DeactivationListener     deactivationListenerCQCFilter   = new DeactivationListener();
        DeactivationListener     deactivationListenerCQCKey      = new DeactivationListener();
        Listener<String, String> listenerCache                   = new Listener<>();
        DeactivationListener     deactivationListenerCache       = new DeactivationListener();

        cache.addMapListener(listenerCache);
        cache.addMapListener(deactivationListenerCache);
        cqc.addMapListener(listenerCQC);
        cqc.addMapListener(deactivationListenerCQCFilter, AlwaysFilter.INSTANCE(), false);
        cqc.addMapListener(deactivationListenerCQCKey, "foo", false);

        cache.truncate();

        Eventually.assertDeferred(() -> deactivationListenerCache.getEvents().isEmpty(), is(false));
        Eventually.assertDeferred(() -> deactivationListenerCQCFilter.getEvents().isEmpty(), is(false));
        Eventually.assertDeferred(() -> deactivationListenerCQCKey.getEvents().isEmpty(), is(false));
        assertThat(listenerCache.getEvents().size(), is(0));
        assertThat(listenerCQC.getEvents().size(), is(0));

        MapEvent mapEvent = deactivationListenerCQCFilter.getEvents().get(0);
        assertThat(mapEvent.getId(), is(MapEvent.ENTRY_UPDATED));

        mapEvent = deactivationListenerCQCKey.getEvents().get(0);
        assertThat(mapEvent.getId(), is(MapEvent.ENTRY_UPDATED));
        }

    @Test
    public void shouldCorrectlyAddDeactivationListenerInConstructor()
        {
        NamedCache<String, String> cache = CacheFactory.getCache("test-two");
        cache.put("key", "value");

        DeactivationListener     deactivationListenerCQC   = new DeactivationListener();
        DeactivationListener     deactivationListenerCache = new DeactivationListener();

        new ContinuousQueryCache<>(cache, AlwaysFilter.INSTANCE(), deactivationListenerCQC);

        cache.addMapListener(deactivationListenerCache);
        cache.truncate();

        Eventually.assertDeferred(() -> deactivationListenerCache.getEvents().isEmpty(), is(false));
        Eventually.assertDeferred(() -> deactivationListenerCQC.getEvents().isEmpty(), is(false));

        MapEvent mapEvent = deactivationListenerCQC.getEvents().get(0);
        assertThat(mapEvent.getId(), is(MapEvent.ENTRY_UPDATED));
        }

    @Test
    public void shouldCorrectlyHandleDestroyEvent()
        {
        NamedCache<String, String> cache = CacheFactory.getCache("test-two");
        cache.put("key", "value");

        ContinuousQueryCache<String, String, String> cqc = new ContinuousQueryCache<>(cache, AlwaysFilter.INSTANCE());

        Listener<String, String> listenerCQC                     = new Listener<>();
        DeactivationListener     deactivationListenerCQCFilter   = new DeactivationListener();
        DeactivationListener     deactivationListenerCQCKey      = new DeactivationListener();
        Listener<String, String> listenerCache                   = new Listener<>();
        DeactivationListener     deactivationListenerCache       = new DeactivationListener();

        cache.addMapListener(listenerCache);
        cache.addMapListener(deactivationListenerCache);
        cqc.addMapListener(listenerCQC);
        cqc.addMapListener(deactivationListenerCQCFilter, AlwaysFilter.INSTANCE(), false);
        cqc.addMapListener(deactivationListenerCQCKey, "foo", false);

        cache.destroy();

        Eventually.assertDeferred(() -> deactivationListenerCache.getEvents().isEmpty(), is(false));
        Eventually.assertDeferred(() -> deactivationListenerCQCFilter.getEvents().isEmpty(), is(false));
        Eventually.assertDeferred(() -> deactivationListenerCQCKey.getEvents().isEmpty(), is(false));
        assertThat(listenerCache.getEvents().size(), is(0));
        assertThat(listenerCQC.getEvents().size(), is(0));

        MapEvent mapEvent = deactivationListenerCQCFilter.getEvents().get(0);
        assertThat(mapEvent.getId(), is(MapEvent.ENTRY_DELETED));

        mapEvent = deactivationListenerCQCKey.getEvents().get(0);
        assertThat(mapEvent.getId(), is(MapEvent.ENTRY_DELETED));
        }

    @Test
    public void shouldRemoveDeactivationListener()
        {
        NamedCache<String, String> cache = CacheFactory.getCache("test-two");

        ContinuousQueryCache<String, String, String> cqc = new ContinuousQueryCache<>(cache, AlwaysFilter.INSTANCE());

        DeactivationListener     deactivationListenerCQCFilterOne = new DeactivationListener();
        DeactivationListener     deactivationListenerCQCFilterTwo = new DeactivationListener();
        DeactivationListener     deactivationListenerCQCKeyOne    = new DeactivationListener();
        DeactivationListener     deactivationListenerCQCKeyTwo    = new DeactivationListener();
        DeactivationListener     deactivationListenerCache        = new DeactivationListener();

        cache.addMapListener(deactivationListenerCache);
        cqc.addMapListener(deactivationListenerCQCFilterOne, AlwaysFilter.INSTANCE(), false);
        cqc.addMapListener(deactivationListenerCQCFilterTwo, AlwaysFilter.INSTANCE(), false);
        cqc.addMapListener(deactivationListenerCQCKeyOne, "foo", false);
        cqc.addMapListener(deactivationListenerCQCKeyTwo, "foo", false);

        cache.put("key", "value");

        cqc.removeMapListener(deactivationListenerCQCFilterTwo, AlwaysFilter.INSTANCE());
        cqc.removeMapListener(deactivationListenerCQCKeyTwo, "foo");

        cache.truncate();

        Eventually.assertDeferred(() -> deactivationListenerCache.getEvents().isEmpty(), is(false));
        Eventually.assertDeferred(() -> deactivationListenerCQCFilterOne.getEvents().isEmpty(), is(false));
        Eventually.assertDeferred(() -> deactivationListenerCQCKeyOne.getEvents().isEmpty(), is(false));
        assertThat(deactivationListenerCQCFilterTwo.getEvents().size(), is(0));
        assertThat(deactivationListenerCQCKeyTwo.getEvents().size(), is(0));
        }

    // ----- inner class: TestValueUpdater ----------------------------------

    /**
    * ValueUpdater.
    */
    public static class TestValueUpdater
            implements ValueUpdater, Serializable
        {
        public TestValueUpdater()
            {
            }

        public void update(Object oTarget, Object oValue)
            {
            ((Boolean[]) oTarget)[0] = (Boolean) oValue;
            }
        }

    // ----- inner class: Listener ------------------------------------------

    public static class Listener<K, V>
            extends MultiplexingMapListener<K, V>
        {
        @Override
        protected void onMapEvent(MapEvent<K, V> evt)
            {
            m_listEvent.add(evt);
            }

        public List<MapEvent<K, V>> getEvents()
            {
            return m_listEvent;
            }

        // ----- data members -----------------------------------------------

        private final List<MapEvent<K, V>> m_listEvent = new ArrayList<>();
        }

    // ----- inner class: DeactivationListener ------------------------------

    @SuppressWarnings("rawtypes")
    public static class DeactivationListener
            extends MultiplexingMapListener
            implements NamedCacheDeactivationListener
        {
        @Override
        protected void onMapEvent(MapEvent evt)
            {
            m_listEvent.add(evt);
            }

        public List<MapEvent> getEvents()
            {
            return m_listEvent;
            }

        // ----- data members -----------------------------------------------

        private final List<MapEvent> m_listEvent = new ArrayList<>();
        }

    }
