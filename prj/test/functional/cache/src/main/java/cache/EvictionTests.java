/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package cache;

import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.coherence.ServiceStatus;
import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.coherence.common.base.Converter;

import com.oracle.coherence.testing.AbstractFunctionalTest;

import com.tangosol.net.BackingMapManagerContext;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.net.PartitionedService;

import com.tangosol.net.partition.KeyAssociator;

import com.tangosol.util.Base;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.MapEvent;
import com.tangosol.util.MultiplexingMapListener;
import com.tangosol.util.ValueExtractor;

import com.tangosol.util.aggregator.DistinctValues;

import com.tangosol.util.extractor.ReflectionExtractor;

import com.tangosol.util.filter.AlwaysFilter;

import com.tangosol.util.processor.AbstractProcessor;

import data.Person;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import org.hamcrest.CoreMatchers;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.oracle.bedrock.deferred.DeferredHelper.within;
import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Test class for new cache eviction features, such as proactive eviction
 * and sliding expiry support
 *
 * @author bbc 2015-07-09
 */
public class EvictionTests
        extends AbstractFunctionalTest
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor.
     */
    public EvictionTests()
        {
        super(FILE_CFG_CACHE);
        }

    @After
    public void cleanup()
        {
        stopAllApplications();
        CacheFactory.shutdown();
        }

    // ----- test listeners -------------------------------------------------

    /**
     * Initialize the test class.
     */
    @BeforeClass
    public static void _startup()
        {
        // this test requires local storage to be enabled
        System.setProperty("coherence.distributed.localstorage", "true");
        AbstractFunctionalTest._startup();
        }

    @Test
    public void testActiveEviction()
        {
        System.setProperty("test.expiry.sliding", "false");
        NamedCache cache = getNamedCache("test-expiry");
        doActiveEvictionTest(cache);
        cleanup();
        }

    @Test
    public void testExpirySliding()
        {
        System.setProperty("test.expiry.sliding", "true");
        NamedCache cache = getNamedCache("test-expiry");
        doExpirySlidingTest(cache);
        cleanup();
        }


    @Test
    public void testQueryExpirySliding()
        {
        System.setProperty("test.expiry.sliding", "true");
        NamedCache cache = getNamedCache("test-expiry");
        doQueryExpirySlidingTest(cache);
        cleanup();
        }

    @Test
    public void COH16231RegressionTest()
        {
        System.setProperty("coherence.distribution.2server", "false");
        AbstractFunctionalTest._startup();

        CoherenceClusterMember clusterMember = startCacheServer("COH16231Regression-1", "cache", FILE_CFG_CACHE);

        Eventually.assertThat(clusterMember.getClusterSize(), is(2));

        NamedCache cache = getNamedCache("test-coh16231");
        String     sName = cache.getCacheService().getInfo().getServiceName();

        Eventually.assertThat(invoking(this).getServiceStatus(clusterMember,sName),
                CoreMatchers.is(ServiceStatus.NODE_SAFE.name()), within(5, TimeUnit.MINUTES));

        int cSize = 20000;  // high-unit is 10000

        Map<Integer, Integer> map = new HashMap<Integer, Integer>();
        for (int i = 0; i < cSize; i ++)
            {
            map.put(i, i);
            }

        cache.putAll(map);

        assertTrue(cache.size() > cSize * 0.9);

        // safe to kill
        stopCacheServer("COH16231Regression-1", false);

        // cache prune should be called when partitions are successfully restored;
        // Note, Eventually.assertThat cannot be used to wait for partitions restoring
        // to finished, it defeats the purpose of this test.  In normal case, one second
        // wait should be enough.
        Base.sleep(1000);

        assertThat("Cache prune did not happen! ", cache.size(), lessThanOrEqualTo(cSize / 2));

        cleanup();
        }


    /**
     * Test the behavior of EvictionTask.
     */
    public void doActiveEvictionTest(NamedCache cache)
        {
        long cSleep = 1000L;

        // EvictionTask should be scheduled for cache with expiry configured.
        ClientListener listener = new ClientListener();
        cache.addMapListener(listener);

        cache.put(1, EvictionTests.VALUE);
        listener.assertDeleteEvent(1, true);
        assertNull(cache.get(1));
        listener.clear();

        // EvictionTask should be rescheduled if new entry expiry time is sooner
        // than already scheduled.
        cache.put(1, EvictionTests.VALUE);
        Base.sleep(cSleep);

        cache.put(2, EvictionTests.VALUE, 1000L);
        listener.assertDeleteEvent(2, true);

        Base.sleep(EXPIRY - cSleep + 250L);
        listener.assertDeleteEvent(1, true);
        assertNull(cache.get(1));
        assertNull(cache.get(2));
        }

    /**
     * Test the expiry sliding support.
     */
    public void doExpirySlidingTest(NamedCache cache)
        {
        long       cSleep      = EXPIRY/2;
        int        cSize       = 10;
        Map        map         = new HashMap();
        Set        setKeys     = new HashSet();

        for (int i = 0; i < cSize; i++)
            {
            map.put(i, i);
            setKeys.add(i);
            }

        // key 1's expiry should be extended after the get call
        cache.putAll(map);
        Base.sleep(cSleep);
        cache.get(1);
        Base.sleep(cSleep + (cSleep / 2));
        assertEquals(1, cache.size());
        cache.clear();

        // all keys' expiry should be extended after the getAll call
        cache.putAll(map);
        Base.sleep(cSleep);
        cache.getAll(setKeys);
        Base.sleep(cSleep + (cSleep / 2));
        assertEquals(cSize, cache.size());
        cache.clear();

        // test EP that generate two update events,  expiry only event binEntry.getValue()
        // and normal update event from binEntry.setValue().  Both key's expiry should
        // be extended.
        cache.putAll(map);
        Base.sleep(cSleep);
        TestProcessor processor = new TestProcessor();
        cache.invoke(1, processor);
        Base.sleep(cSleep + (cSleep / 2));
        assertEquals(2, cache.size());
        cache.clear();
        cleanup();
        }

    /**
     * Test the expiry sliding support.
     */
    public void doQueryExpirySlidingTest(NamedCache cache)
        {
        long       cSleep      = EXPIRY/2;
        int        cSize       = 10;

        ValueExtractor extractor = new ReflectionExtractor("getLastName");

        final DistinctValues agent = new DistinctValues(extractor);

        Person.fillRandom(cache, cSize);

        assertEquals(cSize, cache.size());
        Base.sleep(cSleep);

        // aggregate DOES NOT extend expiry
        cache.aggregate(AlwaysFilter.INSTANCE, agent);
        Base.sleep(cSleep + (cSleep / 2));
        assertEquals(0, cache.size());

        cache.clear();
        cleanup();
        }

    public String getServiceStatus(CoherenceClusterMember member, String sService)
        {
        return member.getServiceStatus(sService).name();
        }


    // ----- inner helper classes -------------------------------------


    public static class TestProcessor
            extends AbstractProcessor
        {
        @Override
        public Object process(InvocableMap.Entry entry)
            {
            BinaryEntry              binEntry = (BinaryEntry) entry;
            BackingMapManagerContext ctx      = binEntry.getContext();
            Converter                convDown = ctx.getKeyToInternalConverter();
            Integer                  IKey     = (Integer) binEntry.getKey();
            Integer                  IKeyNew  = Integer.valueOf(IKey.intValue() + 1);

            BinaryEntry binEntry2 = (BinaryEntry) binEntry.getBackingMapContext().
                    getBackingMapEntry(convDown.convert(IKeyNew));

            binEntry.setValue(IKeyNew);
            binEntry2.getValue();

            return null;
            }
        }

    public static class Associator
            implements KeyAssociator
        {
        /**
         * {@inheritDoc}
         */
        public void init(PartitionedService service)
            {
            }

        /**
         * {@inheritDoc}
         */
        public Object getAssociatedKey(Object oKey)
            {
            return "Key";
            }
        }

    public static class ClientListener
            extends MultiplexingMapListener
        {
        public ClientListener()
            {
            }

        /**
         * Assert that the client received the listener event
         *
         * @param fExpected  true if client event expected
         */
        protected void assertDeleteEvent(Object oKey, boolean fExpected)
             {
             Eventually.assertThat(invoking(this).wasDeleted(oKey), is(fExpected));
             }

        /**
         * Check if deleted event was received for the specified key
         *
         * @param oKey  the key to check
         *
         * @return true iff deleted event was received for the specified key
         */
        public boolean wasDeleted(Object oKey)
            {
            for (Object e : m_listEvents)
                {
                MapEvent evt = (MapEvent) e;
                if (evt.getId() == MapEvent.ENTRY_DELETED &&
                        ((Integer) evt.getKey()).intValue() == ((Integer) oKey).intValue())
                    {
                    return true;
                    }
                }
            return false;
            }

        protected void clear()
            {
            m_listEvents.clear();
            }

        @Override
        protected void onMapEvent(MapEvent evt)
             {
             m_listEvents.add(evt);
             }

        /**
         * List of events that occurred.
         */
        private volatile List m_listEvents = new CopyOnWriteArrayList();
        }

    // ----- constants ------------------------------------------------------

    /**
     * The file name of the default cache configuration file used by this test.
     */
    public static String FILE_CFG_CACHE = "server-cache-config.xml";

    /**
     * The value put into the cache.
     */
    private final static String VALUE = "foo";

    /**
     * The expiry specified in the cache configuration.
     */
    private final static long EXPIRY = 3_000L;
}
