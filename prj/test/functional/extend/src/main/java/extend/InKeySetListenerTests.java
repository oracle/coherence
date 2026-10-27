/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package extend;

import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.coherence.testing.AbstractFunctionalTest;

import com.tangosol.net.CacheService;
import com.tangosol.net.NamedCache;

import com.tangosol.util.CompositeKey;
import com.tangosol.util.Filter;
import com.tangosol.util.MapEvent;
import com.tangosol.util.MultiplexingMapListener;
import com.tangosol.util.filter.InKeySetFilter;
import com.tangosol.util.filter.MapEventFilter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Verify mixed key-set and filter listeners over Extend and direct TCMP.
 *
 * @author phf  2026.10.21
 * @since 26.10
 */
@RunWith(Parameterized.class)
public class InKeySetListenerTests
        extends AbstractFunctionalTest
    {
    @Parameterized.Parameters(name = "extend={0}, keySetFirst={1}, nullFilter={2}, keySetLite={3}, otherLite={4}")
    public static Collection<Object[]> parameters()
        {
        List<Object[]> parameters = new ArrayList<>();
        for (boolean fExtend : new boolean[] {false, true})
            {
            for (boolean fFirst : new boolean[] {false, true})
                {
                for (boolean fNull : new boolean[] {false, true})
                    {
                    for (boolean fLite : new boolean[] {false, true})
                        {
                        for (boolean fOtherLite : new boolean[] {false, true})
                            {
                            parameters.add(new Object[] {fExtend, fFirst, fNull, fLite, fOtherLite});
                            }
                        }
                    }
                }
            }
        return parameters;
        }

    public InKeySetListenerTests(boolean fExtend, boolean fFirst, boolean fNull,
                                boolean fLite, boolean fOtherLite)
        {
        super(fExtend ? "client-cache-config.xml" : "inkeyset-tcmp-cache-config.xml");
        f_fExtend    = fExtend;
        f_fFirst     = fFirst;
        f_fNull      = fNull;
        f_fLite      = fLite;
        f_fOtherLite = fOtherLite;
        }

    @BeforeClass
    public static void _startup()
        {
        s_sCluster = System.getProperty("coherence.cluster");
        s_sStorage = System.getProperty("coherence.distributed.localstorage");
        System.setProperty("coherence.cluster", "InKeySetListenerTests-" + UUID.randomUUID());
        System.setProperty("coherence.distributed.localstorage", "false");
        AbstractFunctionalTest._startup();

        CoherenceClusterMember member = startCacheServer("InKeySetListenerTests", "extend", "server-cache-config.xml");
        Eventually.assertDeferred(() -> member.isServiceRunning("ExtendTcpProxyService"), is(true));
        }

    @AfterClass
    public static void _shutdown()
        {
        try
            {
            AbstractFunctionalTest._shutdown();
            }
        finally
            {
            restoreProperty("coherence.cluster", s_sCluster);
            restoreProperty("coherence.distributed.localstorage", s_sStorage);
            }
        }

    @Test
    public void shouldDeliverInsertUpdateAndDeleteEvents() throws Exception
        {
        assertEvents(1, 2, 3);
        }

    @Test
    public void shouldDeliverEventsForAssociatedKeys() throws Exception
        {
        assertEvents(new CompositeKey<>("account", 1), new CompositeKey<>("account", 2),
                new CompositeKey<>("account", 3));
        }

    @Test
    public void shouldRemoveAndReregisterListeners() throws Exception
        {
        NamedCache<Object, String> cache  = getTestCache();
        EventCollector             target = new EventCollector();
        EventCollector             other  = new EventCollector();
        Filter                     keys   = new InKeySetFilter<>(null, new HashSet<>(Set.of(1, 2)));
        Filter                     filter = otherFilter();
        try
            {
            register(cache, target, keys, other, filter);
            cache.put(1, "one");
            target.awaitEvent(MapEvent.ENTRY_INSERTED, 1, null, "one", f_fLite);
            other.awaitEvent(MapEvent.ENTRY_INSERTED, 1, null, "one", f_fOtherLite);

            cache.removeMapListener(other, filter);
            cache.put(2, "two");
            target.awaitEvent(MapEvent.ENTRY_INSERTED, 2, null, "two", f_fLite);

            cache.addMapListener(other, filter, f_fOtherLite);
            cache.removeMapListener(target, keys);
            cache.put(1, "removed");
            other.awaitEvent(MapEvent.ENTRY_UPDATED, 1, "one", "removed", f_fOtherLite);
            target.assertEmpty();

            cache.addMapListener(target, keys, f_fLite);
            cache.put(1, "restored");
            target.awaitEvent(MapEvent.ENTRY_UPDATED, 1, "removed", "restored", f_fLite);
            other.awaitEvent(MapEvent.ENTRY_UPDATED, 1, "removed", "restored", f_fOtherLite);
            target.assertEmpty();
            }
        finally
            {
            cache.removeMapListener(target, keys);
            cache.removeMapListener(other, filter);
            cache.destroy();
            }
        }

    private void assertEvents(Object first, Object second, Object third) throws Exception
        {
        NamedCache<Object, String> cache  = getTestCache();
        EventCollector             target = new EventCollector();
        EventCollector             other  = new EventCollector();
        Filter                     keys   = new InKeySetFilter<>(null, new HashSet<>(Set.of(first, second)));
        Filter                     filter = otherFilter();
        Object[]                   all    = {first, second, third};
        try
            {
            register(cache, target, keys, other, filter);
            for (Object key : all)
                {
                cache.put(key, "inserted");
                }
            target.awaitEvent(MapEvent.ENTRY_INSERTED, first, null, "inserted", f_fLite);
            target.awaitEvent(MapEvent.ENTRY_INSERTED, second, null, "inserted", f_fLite);
            for (Object key : all)
                {
                other.awaitEvent(MapEvent.ENTRY_INSERTED, key, null, "inserted", f_fOtherLite);
                }

            for (Object key : all)
                {
                cache.put(key, "updated");
                }
            target.awaitEvent(MapEvent.ENTRY_UPDATED, first, "inserted", "updated", f_fLite);
            target.awaitEvent(MapEvent.ENTRY_UPDATED, second, "inserted", "updated", f_fLite);
            for (Object key : all)
                {
                other.awaitEvent(MapEvent.ENTRY_UPDATED, key, "inserted", "updated", f_fOtherLite);
                }

            for (Object key : all)
                {
                cache.remove(key);
                }
            target.awaitEvent(MapEvent.ENTRY_DELETED, first, "updated", null, f_fLite);
            target.awaitEvent(MapEvent.ENTRY_DELETED, second, "updated", null, f_fLite);
            for (Object key : all)
                {
                other.awaitEvent(MapEvent.ENTRY_DELETED, key, "updated", null, f_fOtherLite);
                }
            target.assertEmpty();
            other.assertEmpty();
            }
        finally
            {
            cache.removeMapListener(target, keys);
            cache.removeMapListener(other, filter);
            cache.destroy();
            }
        }

    private NamedCache<Object, String> getTestCache()
        {
        NamedCache<Object, String> cache = getNamedCache("dist-extend-direct");
        assertEquals(f_fExtend ? CacheService.TYPE_REMOTE : CacheService.TYPE_DISTRIBUTED,
                cache.getCacheService().getInfo().getServiceType());
        cache.clear();
        return cache;
        }

    private Filter otherFilter()
        {
        return f_fNull ? null : new MapEventFilter<>(MapEventFilter.E_ALL);
        }

    private void register(NamedCache<Object, String> cache, EventCollector target, Filter keys,
                          EventCollector other, Filter filter)
        {
        if (f_fFirst)
            {
            cache.addMapListener(target, keys, f_fLite);
            }
        cache.addMapListener(other, filter, f_fOtherLite);
        if (!f_fFirst)
            {
            cache.addMapListener(target, keys, f_fLite);
            }
        }

    private static void restoreProperty(String name, String value)
        {
        if (value == null)
            {
            System.clearProperty(name);
            }
        else
            {
            System.setProperty(name, value);
            }
        }

    private static class EventCollector
            extends MultiplexingMapListener<Object, String>
        {
        @Override
        protected void onMapEvent(MapEvent<Object, String> event)
            {
            f_events.add(event);
            }

        void awaitEvent(int id, Object key, String oldValue, String newValue, boolean fLite) throws Exception
            {
            MapEvent<Object, String> event = f_events.poll(10, TimeUnit.SECONDS);
            assertNotNull("missing event " + id + " for key " + key, event);
            assertEquals(id, event.getId());
            assertEquals(key, event.getKey());
            if (!fLite)
                {
                assertEquals(oldValue, event.getOldValue());
                assertEquals(newValue, event.getNewValue());
                }
            }

        void assertEmpty() throws Exception
            {
            assertNull("unexpected event", f_events.poll(250, TimeUnit.MILLISECONDS));
            }

        private final BlockingQueue<MapEvent<Object, String>> f_events = new LinkedBlockingQueue<>();
        }

    private final boolean f_fExtend;
    private final boolean f_fFirst;
    private final boolean f_fNull;
    private final boolean f_fLite;
    private final boolean f_fOtherLite;

    private static String s_sCluster;
    private static String s_sStorage;
    }
