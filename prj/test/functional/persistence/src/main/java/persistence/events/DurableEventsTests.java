/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package persistence.events;

import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.concurrent.RemoteCallable;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.tangosol.internal.util.listener.VersionAwareListeners;

import com.tangosol.io.FileHelper;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.CacheService;
import com.tangosol.net.NamedMap;
import com.tangosol.net.PartitionedService;

import com.tangosol.net.partition.DefaultVersionedPartitions;
import com.tangosol.net.partition.KeyPartitioningStrategy;
import com.tangosol.net.partition.VersionedPartitions.VersionedIterator;

import com.tangosol.persistence.CachePersistenceHelper;

import com.tangosol.util.Base;
import com.tangosol.util.Filter;
import com.tangosol.util.Filters;
import com.tangosol.util.MapEvent;
import com.tangosol.util.MapListener;
import com.tangosol.util.ValueExtractor;

import com.tangosol.util.filter.MapEventFilter;

import com.tangosol.util.function.Remote.Function;

import com.tangosol.util.listener.SimpleMapListener;

import com.oracle.coherence.testing.AbstractFunctionalTest;

import org.hamcrest.CoreMatchers;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;

import static com.tangosol.util.MapEvent.ENTRY_INSERTED;
import static com.tangosol.util.MapEvent.ENTRY_UPDATED;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for durable events feature.
 *
 * @author hr  2021.01.26
 */
public class DurableEventsTests
        extends AbstractFunctionalTest
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a test suite for durable events.
     */
    public DurableEventsTests()
        {
        super(FILE_CFG);
        }

    /**
     * Set up storage nodes.
     */
    @BeforeClass
    public static void _startup()
        {
        String sBase, sEvents;
        try
            {
            s_fileBase   = FileHelper.createTempDir();
            s_fileEvents = new File(s_fileBase, "events");

            sBase   = s_fileBase.getCanonicalPath();
            sEvents = s_fileEvents.getCanonicalPath();
            }
        catch (IOException e)
            {
            throw ensureRuntimeException(e);
            }

        Properties props = new Properties();

        props.put("coherence.distributed.persistence.mode", "active");
        props.put(CachePersistenceHelper.DEFAULT_BASE_DIR_PROPERTY, sBase);
        props.put("coherence.distributed.persistence.events.dir", sEvents);
        props.put("coherence.profile", "thin");

        if (SERVERS < 1)
            {
            props.put("coherence.distributed.localstorage", "true");
            }

        System.getProperties().putAll(props);

        AbstractFunctionalTest._startup();

        List<String>           setServerNames = new ArrayList<>(SERVERS);
        CoherenceClusterMember member         = null;
        for (int i = 1; i <= SERVERS; ++i)
            {
            String sServer = "durable-event-store-" + i;
            member = startCacheServer(sServer, "persistence", FILE_CFG, props);

            setServerNames.add(sServer);
            }

        Eventually.assertThat(invoking(member).getClusterSize(), CoreMatchers.is(SERVERS + 1));

        s_listServerNames = Collections.synchronizedList(setServerNames);
        }

    /**
     * Pre-test initialization.
     */
    @Before
    public void setUp()
        {
        final String CACHE_NAME = "foo";

        NamedMap<Integer, String> cache = getNamedCache(CACHE_NAME);

        Eventually.assertDeferred(
                () -> ((PartitionedService) cache.getService()).getOwnershipEnabledMembers().size(),
                is(SERVERS));

        waitForBalanced(cache.getService());
        }

    /**
     * A test case that illustrates the fundamental functionality: a client
     * receiving events missed due to a service restart.
     */
    @Test
    public void testBasicKeyClient()
        {
        assumeThat(s_listServerNames.size(), greaterThanOrEqualTo(1));

        final String CACHE_NAME = "foo";

        NamedMap<Integer, String> cache = getNamedCache(CACHE_NAME);
        try
            {
            // tag::simple-registration[]
            List<MapEvent> listEvents = Collections.synchronizedList(new ArrayList<>());

            MapListener<Integer, String> listener = new SimpleMapListener<Integer, String>()
                    .addEventHandler(listEvents::add)
                    .versioned();

            cache.addMapListener(listener, 1, false);
            // end::simple-registration[]

            CoherenceClusterMember member = findServer();

            // insert 5 versions of the same entry on a remote node
            member.invoke(() ->
                {
                NamedMap<Integer, String> cacheFoo = CacheFactory.getCache(CACHE_NAME);

                for (int i = 0, c = 5; i < c; ++i)
                    {
                    cacheFoo.put(1, "version " + i);
                    }
                return 5;
                });

            Eventually.assertDeferred(listEvents::size, is(5));

            // cause disconnect
            causeServiceDisruption(cache);

            // insert 5 more versions of the same entry on a remote node
            member.invoke(() ->
                {
                NamedMap<Integer, String> cacheFoo = CacheFactory.getCache(CACHE_NAME);

                for (int i = 5, c = 10; i < c; ++i)
                    {
                    cacheFoo.put(1, "version " + i);
                    }
                return 5;
                });

            // invoke an operation on cache that will result in the cache being
            // restarted and the listener being re-registered
            assertEquals(1, cache.size());

            //
            Eventually.assertDeferred(listEvents::size, is(10));
            assertEquals(5, listEvents.get(5).getVersion() - listEvents.get(0).getVersion());
            }
        finally
            {
            cache.truncate(); // committed to all members
            cache.destroy();  // sent to the senior synchronously
            }
        }

    /**
     * A test case that illustrates the fundamental functionality: a client
     * receiving events missed due to a service restart.
     */
    @Test
    public void testBasicFilterClient()
        {
        assumeThat(s_listServerNames.size(), greaterThanOrEqualTo(1));

        final String CACHE_NAME = "bar";

        NamedMap<Integer, Integer> cache = getNamedCache(CACHE_NAME);
        try
            {
            List<MapEvent> listEvents = Collections.synchronizedList(new ArrayList<>());

            MapListener<Integer, Integer> listener = new SimpleMapListener<Integer, Integer>()
                    .addEventHandler(listEvents::add)
                    .versioned();

            cache.addMapListener(listener, new MapEventFilter<>(MapEventFilter.E_ALL,
                    Filters.greaterEqual(ValueExtractor.identity(), 10)), false);

            CoherenceClusterMember member = findServer();

            // insert 5 versions of the same entry on a remote node
            member.invoke(() ->
                {
                NamedMap<Integer, Integer> cacheFoo = CacheFactory.getCache(CACHE_NAME);

                // update keys 5-20 5 times
                for (int i = 1; i <= 5; i++)
                    {
                    for (int j = 5, c = 20; j < c; ++j)
                        {
                        cacheFoo.put(j, j);
                        }
                    }
                return null;
                });
            Eventually.assertDeferred(listEvents::size, is(5 * 10));

            // cause disconnect
            causeServiceDisruption(cache);

            // insert 5 more versions of the same entry on a remote node
            member.invoke(() ->
                {
                NamedMap<Integer, Integer> cacheFoo = CacheFactory.getCache(CACHE_NAME);

                // update keys 5-20 5 times
                for (int i = 1; i <= 5; i++)
                    {
                    for (int j = 5, c = 20; j < c; ++j)
                        {
                        cacheFoo.put(j, j);
                        }
                    }
                return null;
                });

            // invoke an operation on cache that will result in the cache being
            // restarted and the listener being re-registered
            assertEquals(15, cache.size());

            //
            Eventually.assertDeferred(listEvents::size, is(2 * 5 * 10));
            }
        finally
            {
            cache.truncate(); // committed to all members
            cache.destroy();  // sent to the senior synchronously
            }
        }

    /**
     * A test case that ensures events for partitions that occurred during
     * disconnect, however had no events before the disconnect, are received.
     */
    @Test
    public void testFilterClientWithMutePartition()
        {
        assumeThat(s_listServerNames.size(), greaterThanOrEqualTo(1));

        final String CACHE_NAME = "bar";

        NamedMap<Integer, Integer> cache = getNamedCache(CACHE_NAME);
        try
            {
            List<MapEvent> listEvents = Collections.synchronizedList(new ArrayList<>());

            MapListener<Integer, Integer> listener = new SimpleMapListener<Integer, Integer>()
                    .addEventHandler(listEvents::add)
                    .versioned();

            cache.addMapListener(listener, new MapEventFilter<>(MapEventFilter.E_ALL,
                    Filters.greaterEqual(ValueExtractor.identity(), 10)), false);

            CoherenceClusterMember member = findServer();

            Function<Integer, Integer> functionToPartition = ((PartitionedService) cache.getService()).
                    getKeyPartitioningStrategy()::getKeyPartition;

            int[] anKeys = new int[] {10, 11};
            int   iPart1 = functionToPartition.apply(anKeys[0]);
            while (functionToPartition.apply(anKeys[1]) == iPart1)
                {
                anKeys[1]++;
                }

            // insert 5 versions of the same entry on a remote node
            member.invoke(() ->
                {
                NamedMap<Integer, Integer> cacheFoo = CacheFactory.getCache(CACHE_NAME);

                // update nKey1 5 times
                for (int i = 1; i <= 5; i++)
                    {
                    cacheFoo.put(anKeys[0], anKeys[0]);
                    }
                return null;
                });
            Eventually.assertDeferred(listEvents::size, is(5));

            // cause disconnect
            causeServiceDisruption(cache);

            // insert 5 more versions of the same entry on a remote node
            member.invoke(() ->
                {
                NamedMap<Integer, Integer> cacheFoo = CacheFactory.getCache(CACHE_NAME);

                // update anKeys 5 times
                for (int i = 1; i <= 5; i++)
                    {
                    for (int j = 0, c = anKeys.length; j < c; ++j)
                        {
                        cacheFoo.put(anKeys[j], anKeys[j]);
                        }
                    }
                return null;
                });

            // invoke an operation on cache that will result in the cache being
            // restarted and the listener being re-registered
            assertEquals(anKeys.length, cache.size());

            // 5 updates for key 1 followed by 5 updates to key 1 and 2
            Eventually.assertDeferred(listEvents::size, is(3 * 5));
            }
        finally
            {
            cache.truncate(); // committed to all members
            cache.destroy();  // sent to the senior synchronously
            }
        }

    /**
     * A more advanced use case where a listener is explicitly requesting to
     * receive events after a certain version.
     */
    @Test
    public void testAdvancedClient()
        {
        NamedMap<Integer, String> cache = getNamedCache("foo");
        try
            {
            for (int i = 0; i < 10; ++i)
                {
                cache.put(1, "version " + (i + 1));
                }

            List<MapEvent> listEvents = Collections.synchronizedList(new ArrayList<>());

            MapListener<Integer, String> listener = VersionAwareListeners.createListener(
                    new SimpleMapListener<Integer, String>().addEventHandler(listEvents::add),
                    5L, 1, cache);

            cache.addMapListener(listener, 1, false);

            Eventually.assertDeferred(listEvents::size, is(6));
            assertEquals(5, listEvents.get(5).getVersion() - listEvents.get(0).getVersion());
            }
        finally
            {
            cache.truncate(); // committed to all members
            cache.destroy();  // sent to the senior synchronously
            }
        }

    /**
     * A more advanced use case where a listener is explicitly requesting to
     * receive events after a certain version.
     */
    @Test
    public void testAdvancedFilterClient()
        {
        NamedMap<Integer, Integer> cache = getNamedCache("bar");
        try
            {
            Filter filter = Filters.greaterEqual(ValueExtractor.identity(), 10);

            PartitionVersionTracker<Integer> tracker = new PartitionVersionTracker<>((PartitionedService) cache.getService());

            Set<Integer> setKeys      = IntStream.range(5, 10).boxed().collect(Collectors.toSet());
            Set<Integer> setKeysMatch = IntStream.range(10, 20).boxed().collect(Collectors.toSet());

            // update keys 5-10 10 times that do not match the filter
            for (int i = 0; i <= 10; i++)
                {
                setKeys.forEach(j ->
                    {
                    cache.put(j, j);
                    tracker.onPut(j);
                    });
                }
            // update keys 10-20 10 times that will match the filter
            for (int i = 0; i <= 10; i++)
                {
                setKeysMatch.forEach(j ->
                    {
                    cache.put(j, j);
                    tracker.onPut(j);
                    });
                }

            DefaultVersionedPartitions versions = tracker.getVersions(setKeysMatch);

            int cCount = 0;
            for (VersionedIterator iter = versions.iterator(); iter.hasNext(); )
                {
                long lVersion = iter.nextVersion();

                versions.setPartitionVersion(iter.getPartition(), lVersion - 5);
                cCount += 6;
                }

            List<MapEvent> listEvents = Collections.synchronizedList(new ArrayList<>());

            MapListener<Integer, Integer> listener = VersionAwareListeners.createListener(
                    new SimpleMapListener<Integer, Integer>().addEventHandler(listEvents::add),
                    versions);

            cache.addMapListener(listener, new MapEventFilter<>(MapEventFilter.E_ALL, filter), false);

            // we expect to see 6 versions per partition as the versions that
            // are replayed are inclusive to the version that is sent
            Eventually.assertDeferred(listEvents::size, is(cCount));
            }
        finally
            {
            cache.truncate(); // committed to all members
            cache.destroy();  // sent to the senior synchronously
            }
        }

    /**
     * A test case to ensure that the correct event types and old/new values are returned based upon
     * an insert, update or delete.
     */
    @Test
    public void testAdvancedClientDisconnectedEventTypes()
        {
        final String CACHE_NAME = "foo";
        NamedMap<Integer, String> cache = getNamedCache(CACHE_NAME);
        try
            {
            CoherenceClusterMember member = findServer();
            assertEquals(0, cache.size());
            // cause disconnect
            causeServiceDisruption(cache);
    
            // insert, update and delete
            member.invoke(() ->
                {
                NamedMap<Integer, String> cacheFoo = CacheFactory.getCache(CACHE_NAME);
    
                cacheFoo.put(1, "Initial Value");
                cacheFoo.put(1, "Updated Value");
                cacheFoo.remove(1);
                return null;
                });
    
            // invoke an operation on cache that will result in the cache being restarted
            cache.size();
    
            Eventually.assertDeferred(cache::size, is(0));
    
            List<MapEvent> listEvents = Collections.synchronizedList(new ArrayList<>());
            MapListener<Integer, String> listener = VersionAwareListeners.createListener(
                    new SimpleMapListener<Integer, String>().addEventHandler(listEvents::add),
                    1L, 1, cache);
            cache.addMapListener(listener, 1, false);
            Eventually.assertDeferred(listEvents::size, is(3));
    
            for (int i = 0; i < 3; i++)
                {
                MapEvent mapEvent = listEvents.get(i);
                int eventType = mapEvent.getId();
                if (eventType == ENTRY_UPDATED)
                    {
                    assertNotNull(mapEvent.getOldValue());
                    assertNotNull(mapEvent.getNewValue());
                    }
                else if (eventType == ENTRY_INSERTED)
                    {
                    assertNotNull(mapEvent.getNewValue());
                    assertNull(mapEvent.getOldValue());
                    }
                else // ENTRY_DELETED
                    {
                    assertNull(mapEvent.getNewValue());
                    assertNotNull(mapEvent.getOldValue());
                    }
                }
            }
        finally
            {
            cache.truncate(); // committed to all members
            cache.destroy();  // sent to the senior synchronously
            }
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Randomly pick a server that was started as a part of this test suite.
     *
     * @return a randomly picked member
     */
    protected CoherenceClusterMember findServer()
        {
        if (s_listServerNames.isEmpty())
            {
            // only provision for the member.invoke to be called
            CoherenceClusterMember memberMock = mock(CoherenceClusterMember.class);

            when(memberMock.invoke(any())).thenAnswer(invocation ->
                {
                RemoteCallable callable = invocation.getArgument(0, RemoteCallable.class);

                return callable.call();
                });

            return  memberMock;
            }

        String sServer = s_listServerNames.get(Base.getRandom().nextInt(s_listServerNames.size()));

        return findApplication(sServer);
        }

    /**
     * Stop the inner service.
     *
     * @param cache  the cache hosted by the service to stop
     */
    protected void causeServiceDisruption(NamedMap cache)
        {
        CacheService serviceSafe = cache.getService();
        try
            {
            Method       methRunningService = serviceSafe.getClass().getMethod("getRunningService");
            CacheService serviceInternal    = (CacheService) methRunningService.invoke(serviceSafe);

            serviceInternal.stop();
            }
        catch (NoSuchMethodException e)
            {
            fail("Unexpected service: " + serviceSafe);
            }
        catch (IllegalAccessException | InvocationTargetException e)
            {
            fail("Failed to call getRunningService on: " + serviceSafe);
            }
        }

    // ----- inner class: InsertDataCallable --------------------------------

    /**
     * RemoteCallable that inserts data for a given key,
     * @param <K>
     */
    public static class InsertDataCallable<K>
            implements RemoteCallable<Integer>
        {
        public InsertDataCallable()
            {
            }

        public InsertDataCallable(String sCache, K key, int of, int cData)
            {
            m_sCacheName = sCache;
            m_key        = key;
            m_of         = of;
            m_cData      = cData;
            }

        @Override
        public Integer call()
                throws Exception
            {
            NamedMap<K, String> cache = CacheFactory.getCache(m_sCacheName);

            int i = m_of;
            for (int c = i + m_cData; i < c; ++i)
                {
                cache.put(m_key, "version " + i);
                }
            
            return i - m_of;
            }

        // ----- data members -----------------------------------------------

        protected String m_sCacheName;
        protected K      m_key;
        protected int    m_of;
        protected int    m_cData;
        }

    static class PartitionVersionTracker<K>
        {
        protected PartitionVersionTracker(PartitionedService service)
            {
            f_keyToPartition = service.getKeyPartitioningStrategy();
            }

        public void onPut(K key)
            {
            int  iPart    = f_keyToPartition.getKeyPartition(key);
            long lVersion = f_versions.getVersion(iPart);

            f_versions.setPartitionVersion(iPart, lVersion + 1);
            }

        public DefaultVersionedPartitions getVersions()
            {
            return new DefaultVersionedPartitions(f_versions);
            }

        public DefaultVersionedPartitions getVersions(Set<K> setKeys)
            {
            Set<Integer> setParts = new HashSet<>();
            setKeys.forEach(key -> setParts.add(f_keyToPartition.getKeyPartition(key)));

            DefaultVersionedPartitions versions = new DefaultVersionedPartitions();

            setParts.forEach(IPart -> versions.setPartitionVersion(IPart, f_versions.getVersion(IPart)));

            return versions;
            }

        protected final KeyPartitioningStrategy f_keyToPartition;
        protected final DefaultVersionedPartitions f_versions = new DefaultVersionedPartitions();
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Number of servers to start.
     */
    public static final int    SERVERS = 3;
    public static final String FILE_CFG = "coherence-cache-config.xml";

    // ----- static members -------------------------------------------------

    private static List<String> s_listServerNames;
    private static File         s_fileBase;
    private static File         s_fileEvents;
    }
