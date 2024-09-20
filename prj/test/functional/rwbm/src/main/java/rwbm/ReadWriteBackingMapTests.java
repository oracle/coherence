/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package rwbm;


import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.tangosol.coherence.memcached.processor.GetProcessor;

import com.oracle.coherence.testing.rwbm.EvictingRWBM;
import com.oracle.coherence.testing.CustomClasses;
import com.tangosol.internal.util.processor.CacheProcessors;

import com.tangosol.net.BackingMapManagerContext;
import com.tangosol.net.Member;
import com.tangosol.net.NamedCache;
import com.tangosol.net.PartitionedService;

import com.tangosol.net.cache.BinaryEntryStore;
import com.tangosol.net.cache.CacheEvent;
import com.tangosol.net.cache.ConfigurableCacheMap;
import com.tangosol.net.cache.LocalCache;
import com.tangosol.net.cache.ReadWriteBackingMap;

import com.tangosol.net.events.EventDispatcher;
import com.tangosol.net.events.EventDispatcherAwareInterceptor;
import com.tangosol.net.events.InterceptorRegistry;
import com.tangosol.net.events.partition.cache.EntryEvent;

import com.tangosol.util.AbstractMapListener;
import com.tangosol.util.Base;
import com.tangosol.util.Binary;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.ClassHelper;
import com.tangosol.util.Converter;
import com.tangosol.util.Daemon;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.Filter;
import com.tangosol.util.Filters;
import com.tangosol.util.ImmutableArrayList;
import com.tangosol.util.InvocableMap;

import com.tangosol.net.events.EventInterceptor;
import com.tangosol.net.events.annotation.EntryEvents;
import com.tangosol.net.events.annotation.Interceptor;
import com.tangosol.util.InvocableMap.Entry;
import com.tangosol.util.InvocableMap.StreamingAggregator;
import com.tangosol.util.ListMap;
import com.tangosol.util.MapEvent;
import com.tangosol.util.MapListener;
import com.tangosol.util.MapListenerSupport;
import com.tangosol.util.MultiplexingMapListener;
import com.tangosol.util.NullImplementation;
import com.tangosol.util.ObservableMap;
import com.tangosol.util.RegistrationBehavior;
import com.tangosol.util.SafeHashSet;

import com.tangosol.util.aggregator.Count;

import com.tangosol.util.extractor.IdentityExtractor;
import com.tangosol.util.extractor.PofUpdater;

import com.tangosol.util.filter.AlwaysFilter;
import com.tangosol.util.filter.LessEqualsFilter;
import com.tangosol.util.filter.PresentFilter;

import com.tangosol.util.processor.AbstractProcessor;
import com.tangosol.util.processor.CompositeProcessor;
import com.tangosol.util.processor.ConditionalRemove;
import com.tangosol.util.processor.NumberIncrementor;
import com.tangosol.util.processor.PreloadRequest;
import com.tangosol.util.processor.PropertyManipulator;

import com.oracle.coherence.testing.AbstractFunctionalTest;
import com.oracle.coherence.testing.AbstractTestStore;
import com.oracle.coherence.testing.TestBinaryCacheStore;
import com.oracle.coherence.testing.TestBinaryCacheStore.ExpireProcessor;
import com.oracle.coherence.testing.TestCacheStore;
import com.oracle.coherence.testing.TestHelper;
import com.oracle.coherence.testing.TestNonBlockingStore;

import data.Person;

import org.hamcrest.Matchers;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.Serializable;

import java.lang.reflect.Method;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static com.oracle.bedrock.deferred.DeferredHelper.within;

import static com.tangosol.util.Filters.equal;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


/**
* A collection of unit tests for {@link ReadWriteBackingMap}.
*
* @author jh  2005.10.04
*/
public class ReadWriteBackingMapTests
        extends AbstractFunctionalTest
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor.
     */
    public ReadWriteBackingMapTests()
        {
        super(FILE_CFG_CACHE);
        }


    // ----- test lifecycle -------------------------------------------------

    /**
     * Initialize the test class.
     */
    @BeforeClass
    public static void _startup()
        {
        // this test requires local storage to be enabled
        System.setProperty("coherence.distributed.localstorage", "true");
        System.setProperty("coherence.rwbm.requeue.delay", "5000");

        // the thread count must be 1 for testRemoveAll to have the
        // expected results; otherwise, the removeAll request may
        // split into multiple jobs with different result.
        System.setProperty("tangosol.coherence.distributed.threads.min", "1");
        System.setProperty("tangosol.coherence.distributed.threads.max", "1");

        AbstractFunctionalTest._startup();
        }


    // ----- test methods ---------------------------------------------------

    /**
     * Test the behavior of put() on a ReadWriteBackingMap with a
     * write-batch-factor of one.
     */
    @Test
    public void putWithWriteBatchFactorOne()
        {
        putWithWriteBatchFactorOne("dist-rwbm-wb");
        putWithWriteBatchFactorOne("dist-rwbm-wb-bin");
        }

    private void putWithWriteBatchFactorOne(String sCacheName)
        {
        NamedCache cache = getNamedCache(sCacheName);
        ReadWriteBackingMap rwbm = getReadWriteBackingMap(cache);
        AbstractTestStore store = getStore(cache);
        ObservableMap map = store.getStorageMap();
        long cMillis = rwbm.getWriteBehindSeconds() * 1000L;

        // assert RWBM configuration
        assertTrue("write-batch-factor != 0.0", rwbm.getWriteBatchFactor() == 0.0);
        assertTrue("write-behind-sec <= 0", cMillis > 0);
        rwbm.flush();

        // configure the RWBM
        rwbm.setWriteBatchFactor(1.0);

        store.resetStats();
        map.clear();

        try
            {
            rwbm.flush();

            cache.put("Key4", "Value4");
            cache.put("Key5", "Value5");

            // write batch factor of 1 implies that the store write should be immediate
            // we'll sleep minimally to ensure store has been called
            definiteSleep(0xFFL << 1);

            assertThat(map.size(), is(2));
            
            // we expect to see either 2 individual store calls, or 1 storeAll call
            verifyStoreStats("putWithWriteBatchFactorOne-" + sCacheName, store, equal("store", 2).or(equal("storeAll", 1)));
            }
        finally
            {
            rwbm.setWriteBatchFactor(0.0);
            cache.destroy();
            }
        }

    /**
     * Test the behavior of put() on a ReadWriteBackingMap with a
     * write-batch-factor of one half.
     */
    @Test
    public void putWithWriteBatchFactorOneHalf()
        {
        putWithWriteBatchFactorOneHalf("dist-rwbm-wb");
        putWithWriteBatchFactorOneHalf("dist-rwbm-wb-bin");
        }

    private void putWithWriteBatchFactorOneHalf(String sCacheName)
        {
        NamedCache cache = getNamedCache(sCacheName);
        ReadWriteBackingMap rwbm = getReadWriteBackingMap(cache);
        AbstractTestStore store = getStore(cache);
        ObservableMap map = store.getStorageMap();
        long cMillis = rwbm.getWriteBehindSeconds() * 1000L;

        // assert RWBM configuration
        assertTrue("write-batch-factor != 0.0", rwbm.getWriteBatchFactor() == 0.0);
        assertTrue("write-behind-sec <= 0", cMillis > 0);
        rwbm.flush();

        // configure the RWBM
        rwbm.setWriteBatchFactor(0.5);

        try
            {
            Map mapStore = new ListMap();
            mapStore.put("Key1", "Value1");
            mapStore.put("Key2", "Value2");
            mapStore.put("Key3", "Value3");

            // simple putAll() test
            map.clear();
            store.resetStats();
            cache.putAll(mapStore);

            Eventually.assertThat(invoking(map).size(), is(3));
            verifyStoreStats("putWithWriteBatchFactorOneHalf-" + sCacheName, store, 0, 0, 0, 0, 1, 0);

            // make sure the first put() does not cause an immediate store
            map.clear();
            store.resetStats();
            cache.put("Key4", "Value4");
            definiteSleep(20L);
            assertEquals("put() caused an immediate store.", map.size(), 0);

            // perform another put before the "soft-ripe" write-behind delay
            definiteSleep(cMillis / 4);
            cache.put("Key5", "Value5");

            // perform another put after the "soft-ripe" write-behind delay
            definiteSleep(cMillis / 2);
            cache.put("Key6", "Value6");

            // wait until the write-behind delay has elapsed
            definiteSleep(cMillis / 4 + 100);
            assertEquals("bulk write did not occur.", map.size(), 2);
            verifyStoreStats("putWithWriteBatchFactorOneHalf-" + sCacheName, store, 0, 0, 0, 0, 1, 0);

            // make sure the last write doesn't occur until a full write-behind delay interval
            definiteSleep(100L);
            assertEquals("premature write occurred.", map.size(), 2);

            Eventually.assertThat(invoking(map).size(), is(3));
            verifyStoreStats("putWithWriteBatchFactorOneHalf-" + sCacheName, store, 0, 1, 0, 0, 1, 0);
            }
        finally
            {
            cache.destroy();
            }
        }

    /**
     * Test the behavior of put() on a ReadWriteBackingMap with a
     * write-batch-factor of zero.
     */
    @Test
    public void putWithWriteBatchFactorZero()
        {
        putWithWriteBatchFactorZero("dist-rwbm-wb");
        putWithWriteBatchFactorZero("dist-rwbm-wb-bin");
        }

    private void putWithWriteBatchFactorZero(String sCacheName)
        {
        NamedCache cache = getNamedCache(sCacheName);
        ReadWriteBackingMap rwbm = getReadWriteBackingMap(cache);
        AbstractTestStore store = getStore(cache);
        ObservableMap map = store.getStorageMap();
        long cMillis = rwbm.getWriteBehindSeconds() * 1000L;

        // assert RWBM configuration
        assertTrue("write-batch-factor != 0.0", rwbm.getWriteBatchFactor() == 0.0);
        assertTrue("write-behind-sec <= 0", cMillis > 0);
        rwbm.flush();

        try
            {
            map.clear();
            cache.put("Key1", "Value1");
            definiteSleep(0xFFL);
            assertEquals("put() caused an immediate store.", map.size(), 0);

            definiteSleep(cMillis);
            assertEquals("write did not occur.", map.size(), 1);
            verifyStoreStats("putWithWriteBatchFactorZero-" + sCacheName, store, 0, 1, 0, 0, 0, 0);

            cache.put("Key2", "Value2");
            cache.put("Key3", "Value3");

            definiteSleep(cMillis + 0xFFL);
            assertEquals("write did not occur.", map.size(), 3);
            // we expect to see either 3 individual store calls, or 1 store and 1 storeAll call
            verifyStoreStats("putWithWriteBatchFactorZero-" + sCacheName, store,
                        equal("store", 1).and(equal("storeAll", 1))
                    .or(equal("store", 3)));
            }
        finally
            {
            releaseNamedCache(cache);
            }
        }

    /**
     * Test the behavior of put() on a ReadWriteBackingMap with a
     * write-batch-factor of zero and a high latency CacheStore.
     */
    @Test
    public void writeBehindWithLongStore()
        {
        writeBehindWithLongStore("dist-rwbm-wb");
        writeBehindWithLongStore("dist-rwbm-wb-bin");
        writeBehindWithLongStore("dist-rwbm-wb-remove");
        writeBehindWithLongStore("dist-rwbm-wb-bin-remove");
        }

    private void writeBehindWithLongStore(String sCacheName)
        {
        NamedCache cache = getNamedCache(sCacheName);
        ReadWriteBackingMap rwbm = getReadWriteBackingMap(cache);
        AbstractTestStore store = getStore(cache);
        ObservableMap map = store.getStorageMap();
        long cMillis = rwbm.getWriteBehindSeconds() * 1000L;
        Object sValue;

        // assert RWBM configuration
        assertTrue("write-batch-factor != 0.0", rwbm.getWriteBatchFactor() == 0.0);
        assertTrue("write-behind-sec <= 0", cMillis > 0);
        rwbm.flush();

        // configure the RWBM
        rwbm.setWriteBehindSeconds(1); // write-behind   = 1sec
        store.setDurationStore(1000L); // store duration = 1sec

        try
            {
            map.clear();
            cache.put("Key", "Value1");
            assertTrue("put() caused an immediate store.", map.size() == 0);

            sValue = cache.get("Key");
            assertTrue("get() returned wrong value: " + sValue, "Value1".equals(sValue));
            definiteSleep(1500); // we are in a middle of store() now
            cache.put("Key", "Value2");

            sValue = cache.get("Key");
            assertTrue("get() returned wrong value: " + sValue, "Value2".equals(sValue));
            definiteSleep(2500); // wait for write-delay plus store delay
            assertTrue("put() did not result in a store.", "Value2".equals(map.get("Key")));

            sValue = cache.get("Key");
            assertTrue("get() returned wrong value: " + sValue, "Value2".equals(sValue));

            // write a large batch
            store.setDurationStore(0L);
            cache.clear();
            if (sCacheName.contains("remove"))
                {
                assertEquals("Failed to clear", 1, map.size());
                }
            else
                {
                assertEquals("Failed to clear", 0, map.size());
                }

            Eventually.assertThat(invoking(map).size(), is(0));

            int cEntries = rwbm.getWriteMaxBatchSize()*3;
            Map mapTemp  = new HashMap();
            for (int i = 0; i < cEntries; i++)
                {
                mapTemp.put(i, i);
                }
            cache.putAll(mapTemp);
            Eventually.assertThat(invoking(map).size(), is(cEntries));
            }
        finally
            {
            cache.destroy();
            }
        }

    /**
     * Test the behavior of the ReadWriteBackingMap$WriteThread while calling
     * destroy().
     */
    @Test
    public void writeBehindDuringDestroy() throws Exception
        {
        writeBehindDuringDestroy("dist-rwbm-wb");
        writeBehindDuringDestroy("dist-rwbm-wb-bin");
        }

    private void writeBehindDuringDestroy(String sCacheName) throws Exception
        {
        Method method = ReadWriteBackingMap.class.getDeclaredMethod("getWriteThread");
        List listThreads = new ArrayList(10);

        method.setAccessible(true);
        for (int i = 0; i < 10; ++i)
            {
            NamedCache cache = getNamedCache(sCacheName);
            ReadWriteBackingMap rwbm = getReadWriteBackingMap(cache);

            Object oDaemon = method.invoke(rwbm, ClassHelper.VOID);
            assertTrue(oDaemon instanceof Daemon);

            listThreads.add(((Daemon) oDaemon).getThread());
            cache.destroy();

            // cannot invoke on method as deferred
            Base.sleep(100);
            assertTrue(method.invoke(rwbm, ClassHelper.VOID) == null);
            }

        for (Iterator iter = listThreads.iterator(); iter.hasNext(); )
            {
            Thread thread = (Thread) iter.next();
            thread.join(1100);
            Eventually.assertThat(invoking(this).isThreadAlive(thread), is(false));
            }
        }

    /**
     * Test the requeue behavior.
     */
    @Test
    public void writeBehindDuringRequeue() throws Exception
        {
        writeBehindDuringRequeue("dist-rwbm-wb");
        writeBehindDuringRequeue("dist-rwbm-wb-bin");
        }

    private void writeBehindDuringRequeue(String sCacheName) throws Exception
        {
        NamedCache cache = getNamedCache(sCacheName);
        ReadWriteBackingMap rwbm = getReadWriteBackingMap(cache);
        AbstractTestStore store = getStore(cache);
        ObservableMap map = store.getStorageMap();
        long cMillis = rwbm.getWriteBehindSeconds() * 1000L;

        // reset the storage map
        map.clear();

        // assert RWBM configuration
        assertTrue("write-batch-factor != 0.0", rwbm.getWriteBatchFactor() == 0.0);
        assertTrue("write-behind-sec <= 0", cMillis > 0);

        store.setFailureKeyStore("Key0");
        cache.put("Key0", "Value0");

        definiteSleep(cMillis + 10);
        assertEquals("store did not fail", 0, map.size());

        store.setFailureKeyStore(null);

        Eventually.assertThat(invoking(map).size(), is(1));

        cache.clear();
        map.clear();

        // even though we put everything at once using putALl, there is a
        // possibility that the first one will be persisted as a part of a smaller
        // (less than 10 items) batch or by itself
        store.setFailureKeyStore("Key0");
        store.setFailureKeyStoreAll("Key0");

        Map mapBatch = new HashMap();
        for (int i = 0; i < 10; i++)
            {
            mapBatch.put("Key" + i, "Value" + i);
            }
        cache.putAll(mapBatch);

        definiteSleep(cMillis + 100);
        assertTrue("store did not fail", map.size() < 10);

        store.setFailureKeyStoreAll(null);
        store.setFailureKeyStore(null);

        Eventually.assertThat(invoking(map).size(), is(10));
        }

    /**
     * Test basic read-through CacheStore functionality.
     */
    @Test
    public void readThroughBasic()
        {
        readThroughBasic("dist-rwbm-wt");
        readThroughBasic("dist-rwbm-wt-bin");
        }

    private void readThroughBasic(String sCacheName)
        {
        NamedCache cache = getNamedCache(sCacheName);
        try
            {
            // prime the cache contents
            Map mapContents = new HashMap();
            for (int i = 0; i < 200; i++)
                {
                mapContents.put("Key" + i, "Value" + i);
                }
            AbstractTestStore store = getStore(cache);
            store.getStorageMap().putAll(mapContents);

            // test load()
            assertEquals(0, cache.size());
            for (Object oEntry : mapContents.entrySet())
                {
                Map.Entry entry = (Map.Entry) oEntry;
                assertEquals(entry.getValue(), cache.get(entry.getKey()));
                }

            // verify the store load() method was called the expected number of times
            verifyStoreStats("readThroughBasic-" + sCacheName, store, mapContents.size(), 0, 0, 0, 0, 0);

            // reset
            cache.clear();
            store.getStorageMap().putAll(mapContents);
            store.resetStats();

            // test loadAll()
            assertEquals(0, cache.size());
            assertEquals(mapContents, cache.getAll(mapContents.keySet()));

            // verify the store loadAll() method was called
            verifyStoreStats("readThroughBasic-" + sCacheName, store, 0, 0, 0, 1, 0, 0);
            }
        finally
            {
            cache.destroy();
            }
        }

    /**
     * Test non-blocking failover in the middle of a put
     */
    @Test
    public void testNonBlockingFailover()
        {
        String sCacheName = "dist-rwbm-nonblocking";
        NamedCache cache = getNamedCache(sCacheName);
        AbstractTestStore store = getStore(cache);
        ReadWriteBackingMap rwbm = getReadWriteBackingMap(cache);
        BackingMapManagerContext ctx = cache.getCacheService().getBackingMapManager().getContext();
        Converter convDown = ctx.getKeyToInternalConverter();
        int nPartitions = ((PartitionedService) cache.getCacheService()).getPartitionCount();
        int cOwned = 0;
        int cTotal = 10;

        cache.clear();
        store.getStorageMap().clear();

        String sServerName = "storage1";
        CoherenceClusterMember clusterMember1 = startCacheServer(sServerName, "UnavailableTimeLogging", "rwbm-cache-config.xml");
        waitForServer(clusterMember1);
        waitForBalanced(cache.getCacheService());

        for (int i = 0; i < cTotal; i++)
            {
            boolean own = ctx.isKeyOwned(ExternalizableHelper.toBinary("Key" + i));
            if (own)
                {
                cache.put("Key" + i, "Failover" + i);
                cOwned++;
                }
            else
                {
                cache.put("Key" + i, "DontOwn" + i);
                }
            }

        // stop second member and check that restore/index build gets triggered for partitions coming back
        stopCacheServer("storage1");
        // wait for server to stop
        Eventually.assertDeferred(() -> cache.getCacheService().getCluster().getMemberSet().size(),
                                  Matchers.is(1), within(5, TimeUnit.MINUTES));

        PartitionedService service = (PartitionedService) cache.getCacheService();
        Member member = service.getCluster().getLocalMember();
        // wait for re-distribution
        Eventually.assertThat(invoking(service).getOwnedPartitions(member).cardinality(), is(nPartitions));

        assertTrue(store.getStorageMap().size() == cOwned);

        // wait for failover to happen
        definiteSleep(30000);

        assertTrue(store.getStorageMap().size() == cTotal);
        }

    /**
     * Test basic async CacheStore functionality.
     */
    @Test
    public void readAsyncBasic()
        {
        String sCacheName = "dist-rwbm-nonblocking";

        NamedCache cache = getNamedCache(sCacheName);
        try
            {
            cache.clear();
            AbstractTestStore store = getStore(cache);
            store.resetStats();

            // prime the store contents
            Map mapContents = new HashMap();
            for (int i = 0; i < 200; i++)
                {
                mapContents.put("Key" + i, "Value" + i);
                }
            store.getStorageMap().putAll(mapContents);

            // test load()
            assertEquals(0, cache.size());
            for (Object oEntry : mapContents.entrySet())
                {
                Map.Entry entry = (Map.Entry) oEntry;
                assertEquals(entry.getValue(), cache.get(entry.getKey()));
                }

            // verify the store load() method was called the expected number of times
            verifyStoreStats("readAsyncBasic-" + sCacheName, store, mapContents.size(), 0, 0, 0, 0, 0);

            // reset
            cache.clear();
            store.getStorageMap().putAll(mapContents);
            store.resetStats();

            // test loadAll()
            assertEquals(0, cache.size());
            assertEquals(mapContents, cache.getAll(mapContents.keySet()));

            // verify the store loadAll() method was called
            verifyStoreStats("readAsyncBasic-" + sCacheName, store, 0, 0, 0, 1, 0, 0);

            // test store not calling onNext/onError
            // clear store cache contents
            store.getStorageMap().clear();

            assertEquals(null, cache.get("someKey"));
            cache.get("IllegalState");

            definiteSleep(3000);

            assertEquals("IllegalStateException", store.getStorageMap().get("IllegalState"));
            }
        finally
            {
            cache.destroy();
            }
        }

    /**
    * Test async CacheStore functionality with exceptions.
    */
    @Test
    public void readAllAsyncException()
        {
        readAllAsyncException(false);
        readAllAsyncException(true);
        }

    public void readAllAsyncException(boolean fGlobal)
        {
        String     sCacheName = "dist-rwbm-nonblocking";
        NamedCache cache      = getNamedCache(sCacheName);
        String     errorKey   = "Key87";
        try
            {
            cache.clear();
            AbstractTestStore store = getStore(cache);
            store.resetStats();

            if (!fGlobal)
                {
                store.setFailureKeyLoadAll(errorKey);
                }

            String sValue = fGlobal ? "Exception" : "Value";

            // prime the cache contents
            Map mapContents = new HashMap();
            for (int i = 0 ; i < 200; i++)
                {
                mapContents.put("Key" + i, sValue + i);
                }
            store.getStorageMap().putAll(mapContents);

            if (fGlobal)
                {
                assertThrows(RuntimeException.class, () -> cache.getAll(mapContents.keySet()));
                }
            else
                {
                Map resultMap = cache.getAll(mapContents.keySet());

                assertEquals(store.getStorageMap().size() - 1, cache.size());
                mapContents.remove(errorKey);
                assertEquals(mapContents, resultMap);
                }
            }
        finally
            {
            cache.destroy();
            }
        }

    @Test
    public void writeThroughPutBasic() throws Exception
        {
        writeThroughPutBasic("dist-rwbm-wt-put");
        }

    @Test
    public void writeThroughPutBasicBinary() throws Exception
        {
        writeThroughPutBasic("dist-rwbm-wt-put-bin");
        }

    public void writeThroughPutBasic(String sCacheName) throws Exception
        {
        NamedCache cache = getNamedCache(sCacheName);

        Map<String,String> mapData = new HashMap<>();
        for (int i = 0 ; i < 200; i++)
            {
            mapData.put("Key" + i, "Value" + i);
            }

        AbstractTestStore store = getStore(cache);
        ObservableMap mapStorage = store.getStorageMap();
        mapStorage.clear();
        store.resetStats();

        // Individual puts should call store() on CacheStore
        for (Map.Entry<String,String> entry : mapData.entrySet())
            {
            cache.put(entry.getKey(), entry.getValue());
            }

        verifyStoreStats("writeThroughBasic-" + sCacheName, store, 0, mapData.size(), 0, 0, 0, 0);
        assertThat(mapStorage.size(), is(mapData.size()));
        }

    /**
     * Test basic write non-blocking store functionality.
     */
    @Test
    public void writeAsyncPutBasic() throws Exception
        {
        String sCacheName = "dist-rwbm-nonblocking";

        NamedCache cache = getNamedCache(sCacheName);

        Map<String,String> mapData = new HashMap<>();
        for (int i = 0 ; i < 200; i++)
            {
            mapData.put("Key" + i, "Value" + i);
            }

        AbstractTestStore store = getStore(cache);
        ObservableMap mapStorage = store.getStorageMap();
        mapStorage.clear();
        store.resetStats();

        // Individual puts should call store() on CacheStore
        for (Map.Entry<String,String> entry : mapData.entrySet())
            {
            cache.put(entry.getKey(), entry.getValue());
            }

        // wait for async operations to finish
        definiteSleep(10000);

        verifyStoreStats("writeAsyncBasic-" + sCacheName, store, 0, mapData.size(), 0, 0, 0, 0);
        assertThat(mapStorage.size(), is(mapData.size()));
        }

    /**
     * Test basic write non-blocking store functionality, putAll
     */
    @Test
    public void writeAsyncPutAllBasic() throws Exception
        {
        NamedCache cache = getNamedCache("dist-rwbm-nonblocking-putall");
        cache.clear();

        Map<String,String> mapData = new HashMap<>();
        for (int i = 0 ; i < 50; i++)
            {
            mapData.put("Key" + i, "Value" + i);
            }

        AbstractTestStore store = getStore(cache);
        ObservableMap mapStorage = store.getStorageMap();
        mapStorage.clear();
        store.resetStats();

        // cache.putAll() should call storeAll on CacheStore
        cache.putAll(mapData);

        Eventually.assertDeferred(mapStorage::size, is(mapData.size()));
        }

    /**
     * Test write non-blocking store functionality, partial putAll by store
     */
    @Test
    public void writeAsyncPutAllPartial() throws Exception
        {
        NamedCache cache = getNamedCache("dist-rwbm-nonblocking-putall");
        cache.clear();

        Map<String,String> mapData = new HashMap<>();
        for (int i = 0 ; i < 50; i++)
            {
            mapData.put("Key" + i, "Partial" + i);
            }

        AbstractTestStore store = getStore(cache);
        ObservableMap mapStorage = store.getStorageMap();
        mapStorage.clear();
        store.resetStats();

        // cache.putAll() should call storeAll on CacheStore
        cache.putAll(mapData);

        Eventually.assertDeferred(mapStorage::size, is(mapData.size() - 10));
        }

    /**
     * Test basic write non-blocking store functionality, putAll with global exception
     */
    @Test
    public void writeAsyncPutAllException() throws Exception
        {
        writeAsyncPutAllException(false);
        writeAsyncPutAllException(true);
        }

    public void writeAsyncPutAllException(boolean fGlobal) throws Exception
        {
        NamedCache cache = getNamedCache("dist-rwbm-nonblocking-putall");
        cache.clear();

        Map<String,String> mapData = new HashMap<>();
        for (int i = 0 ; i < 5000; i++)
            {
            if (fGlobal)
                {
                mapData.put("Key" + i, "Exception" + i);
                }
            else
                {
                mapData.put("Key" + i, "Value" + i);
                }
            }

        AbstractTestStore store = getStore(cache);
        ObservableMap mapStorage = store.getStorageMap();
        mapStorage.clear();
        store.resetStats();

        if (!fGlobal)
            {
            store.setFailureKeyStoreAll("Key32");
            }

        // cache.putAll() should call storeAll on CacheStore
        cache.putAll(mapData);

        if (fGlobal)
            {
            Eventually.assertDeferred(mapStorage::size, is(0));
            }
        else
            {
            Eventually.assertDeferred(mapStorage::size, is(mapData.size() - 1));
            }
        }

    @Test
    public void writeThroughPutAllBasic() throws Exception
        {
        writeThroughPutAllBasic("dist-rwbm-wt-putall");
        }

    @Test
    public void writeThroughPutAllBasicBinary() throws Exception
        {
        writeThroughPutAllBasic("dist-rwbm-wt-putall-bin");
        }

    public void writeThroughPutAllBasic(String sCacheName) throws Exception
        {
        NamedCache cache = getNamedCache(sCacheName);

        Map<String,String> mapData = new HashMap<>();
        for (int i = 0 ; i < 50; i++)
            {
            mapData.put("Key" + i, "Value" + i);
            }

        AbstractTestStore store = getStore(cache);
        ObservableMap mapStorage = store.getStorageMap();
        mapStorage.clear();
        store.resetStats();

        // cache.putAll() should call storeAll on CacheStore
        cache.putAll(mapData);

        assertThat(mapStorage.size(), is(mapData.size()));
        }

    /**
     * Test basic refresh-ahead functionality.
     */
    @Test
    public void readAheadBasic()
            throws Exception
        {
        readAheadBasic("dist-rwbm-ra");
        readAheadBasic("dist-rwbm-ra-bin");
        }

    private void readAheadBasic(String sCacheName)
            throws Exception
        {
        NamedCache          cache       = getNamedCache(sCacheName);
        ReadWriteBackingMap rwbm        = getReadWriteBackingMap(cache);
        LocalCache          mapInternal = (LocalCache) rwbm.getInternalCache();
        long                cExpiry     = mapInternal.getExpiryDelay();
        try
            {
            // prime the cache contents
            Map mapContents = new HashMap();
            for (int i = 0 ; i < 200; i++)
                {
                mapContents.put("Key" + i, "Value" + i);
                }
            cache.putAll(mapContents);

            // test over 3 expiry periods
            long ldtEnd = Base.getSafeTimeMillis() + 3 * cExpiry;
            do
                {
                // assert that the cache contents match the expected values;
                // this will ensure that the refresh-ahead is working properly
                assertEquals(mapContents.size(), cache.size());
                assertEquals(mapContents, cache);

                definiteSleep(100);
                }
            while (Base.getSafeTimeMillis() < ldtEnd);
            }
        finally
            {
            cache.destroy();
            }
        }

    /**
    * Test the behavior of the ReadWriteBackingMap$ReadThread while calling
    * destroy().
    */
    @Test
    public void readAheadDuringDestroy() throws Exception
        {
        readAheadDuringDestroy("dist-rwbm-ra");
        readAheadDuringDestroy("dist-rwbm-ra-bin");
        }

    private void readAheadDuringDestroy(String sCacheName) throws Exception
        {
        Method method = ReadWriteBackingMap.class.getDeclaredMethod("getReadThread");
        List   listThreads = new ArrayList(10);

        method.setAccessible(true);
        for (int i = 0; i < 10; ++i)
            {
            NamedCache          cache = getNamedCache(sCacheName);
            ReadWriteBackingMap rwbm  = getReadWriteBackingMap(cache);

            Object oDaemon = method.invoke(rwbm, ClassHelper.VOID);
            assertTrue(oDaemon instanceof Daemon);

            listThreads.add(((Daemon) oDaemon).getThread());
            cache.destroy();

            assertTrue(method.invoke(rwbm, ClassHelper.VOID) == null);
            }

        for (Iterator iter = listThreads.iterator(); iter.hasNext(); )
            {
            Thread thread = (Thread) iter.next();
            thread.join(1100);
            assertFalse(thread.isAlive());
            }
        }

    /**
    * Test the behavior of the ReadWriteBackingMap/WriteBehind
    * with aggressive eviction
    */
    @Test
    public void testEviction()
        {
        final int COUNT_UPDATERS = 5;
        final int TEST_DURATION  = 30000;

        BackingMapManagerContext ctxNull = NullImplementation.getBackingMapManagerContext();

        final ReadWriteBackingMap rwbm = new ReadWriteBackingMap(
            ctxNull, new LocalCache(10), null, new TestCacheStore(), false, 0, 0.0, false);

        final long ldtStop = getSafeTimeMillis() + TEST_DURATION;

        final Set setFailures = new SafeHashSet();

        log("ReadWriteBackingMap:testEviction will run for " +
                (TEST_DURATION / 1000) + " seconds");

        Runnable taskUpdate = new Runnable()
            {
            public void run()
                {
                Random random = new Random();

                while (getSafeTimeMillis() < ldtStop)
                    {
                    Object oKey = ExternalizableHelper.toBinary(
                        random.nextInt(100000));
                    rwbm.put(oKey, oKey);
                    if (rwbm.get(oKey) == null)
                        {
                        setFailures.add(oKey);
                        }
                    }
                }
            };

        Thread[] aThreadUpdater = new Thread[COUNT_UPDATERS];

        for (int i = 0; i < COUNT_UPDATERS; i++)
            {
            aThreadUpdater[i] = new Thread(taskUpdate);
            aThreadUpdater[i].setDaemon(true);
            aThreadUpdater[i].start();
            }

        try
            {
            for (int i = 0; i < COUNT_UPDATERS; i++)
                {
                aThreadUpdater[i].join();
                }

            assertTrue("Number of false misses:" + setFailures.size(),
                    setFailures.isEmpty());
            }
        catch (InterruptedException e)
            {
            throw ensureRuntimeException(e);
            }
        }

    /**
     * Test for ReadWriteBackingMap with sliding-expiry set to true.
     */
    @Test
    public void testReadWriteBackingMapWithSlidingExpiry()
       {
        NamedCache            cache       = getNamedCache("dist-rwbm-wb-sliding-expiry");
        ReadWriteBackingMap   rwbm        = getReadWriteBackingMap(cache);
        ConfigurableCacheMap  mapInternal = (ConfigurableCacheMap) rwbm.getInternalCache();
        Map<Integer, Integer> map         = new HashMap<>();

        cache.put(0, 0);

        assertEquals(cache.get(0), 0);
        assertEquals(1, cache.size());
        Eventually.assertThat(invoking(cache).isEmpty(), is(true));

        try
            {
            // Loading entry from CacheLoader and ensure no exception should be thrown
            cache.get(0);
            }
        catch (Exception e)
            {
            fail("No Exception should have thrown!! Got exception : " + e);
            }

        assertEquals(cache.get(0), 0);
        Base.sleep(1000);
        assertThat(cache.containsKey(0), is(true));

        // access cache to slide/extend expiry for 2s
        cache.get(0);
        Base.sleep(1000);
        assertThat(cache.containsKey(0), is(true));

        // access cache to slide/extend expiry for 2s
        cache.get(0);
        Base.sleep(1000);
        assertThat(cache.containsKey(0), is(true));

        // now eventually let the entry expired and verify
        Eventually.assertThat(invoking(cache).isEmpty(), is(true));
        assertEquals(0, cache.size());
       }

    /**
     * Test that EvictionTask is scheduled so that expired entries are auto-evicted.
     */
    @Test
    public void testInternalMapEviction()
        {
        internalMapEviction("dist-rwbm-wb-expiry");
        internalMapEviction("dist-rwbm-wb-expiry-remove");
        }

    protected void internalMapEviction(String sCacheName)
        {
        NamedCache            cache       = getNamedCache(sCacheName);
        ReadWriteBackingMap   rwbm        = getReadWriteBackingMap(cache);
        ConfigurableCacheMap  mapInternal = (ConfigurableCacheMap) rwbm.getInternalCache();
        Map<Integer, Integer> map         = new HashMap<>();
        TestCacheStore        store       = (TestCacheStore) getStore(cache);

        for (int i = 0; i < 1000; i++)
            {
            map.put(i, i);
            }

        cache.putAll(map);
        assertEquals("testInternalMapEviction", map.size(), mapInternal.size());
        assertEquals("testInternalMapEviction", 0, store.getStorageMap().size());

        Eventually.assertThat(invoking(store.getStorageMap()).size(), is(1000));

        Eventually.assertThat(invoking(mapInternal).keySet().size(), is(0));
        }

    /**
    * Test the behavior of the ReadWriteBackingMap/WriteBehind
    * when calling release.
    */
    @Test
    public void testWriteBehindFlush()
        {
        testWriteBehindFlush("dist-rwbm-wb");
        testWriteBehindFlush("dist-rwbm-wb-bin");
        testWriteBehindFlush("dist-rwbm-wb-remove");
        testWriteBehindFlush("dist-rwbm-wb-bin-remove");
        }

    private void testWriteBehindFlush(String sCacheName)
        {
        NamedCache          cache    = getNamedCache(sCacheName);
        ReadWriteBackingMap rwbm     = getReadWriteBackingMap(cache);
        AbstractTestStore   store    = getStore(cache);
        ObservableMap       mapStore = store.getStorageMap();
        long                cMillis  = rwbm.getWriteBehindSeconds() * 1000L;
        Map                 buffer   = new ListMap();

        // assert RWBM configuration
        assertTrue("write-batch-factor != 0.0", rwbm.getWriteBatchFactor() == 0.0);
        assertTrue("write-behind-sec <= 0", cMillis > 0);
        rwbm.flush();

        // configure the RWBM
        rwbm.setWriteBehindSeconds(60); // write-behind   = 60sec
        store.setDurationStore(10L); // store duration = 10ms

        try
            {
            mapStore.clear();
            store.resetStats();
            for (int i = 0; i < 10; i++)
                {
                buffer.put(i, i);
                }
            cache.putAll(buffer);
            assertTrue("put() caused an immediate store.", mapStore.size() == 0);
            rwbm.flush();

            // wait for the flush to complete
            definiteSleep(1000L);

            int nStoreAllCount = 0;
            try
                {
                nStoreAllCount = ((Integer) store.getStatsMap().get("storeAll")).intValue();
                }
            catch (NullPointerException e) {}
            assertTrue("storeAll() was invoked " + nStoreAllCount + " times",
                    nStoreAllCount == 1);

            int nStoreCount = store.getStorageMap().size();
            assertTrue(nStoreCount + " items in store", nStoreCount == 10);
            }
        finally
            {
            cache.destroy();
            }
        }

    /**
    * Test the behavior of the ReadWriteBackingMap with PreloadRequest entry
    * processor (COH-2795, COH-2892)
    */
    @Test
    public void testPreload()
        {
        testPreload("dist-rwbm-wt");
        testPreload("dist-rwbm-wt-bin");
        }

    private void testPreload(final String sCacheName)
        {
        //
        // We are testing the following combinations:
        //
        // With Read-through:
        //
        //    Processor  | Side effect during processing | Events
        //    ---------------------------------------------------
        // a1) Read      | None                          | INSERT
        // a2) Update    | None                          | INSERT + UPDATE
        // a3) Remove    | None                          | INSERT + DELETE
        // a4) Read      | Evict (explicit) after load   | INSERT + DELETE
        // a5) Update    | Evict before update           | INSERT + DELETE + INSERT
        // a6) Remove    | Evict before remove           | INSERT + DELETE
        // a7) Read      | Evict before read             | DELETE + INSERT
        // a8) None      | Evict                         | DELETE
        // a9) Update    | Evict after update            | INSERT + UPDATE + DELETE
        //
        // Without Read-through:
        //
        //    Processor  | Side effect during processing | Events
        //    ---------------------------------------------------
        // b1) Remove    | None                          | None
        // b2) Update    | Evict before update           | DELETE + INSERT
        // b3) Remove    | Evict before remove           | DELETE
        // b4) Update    | Evict after update            | UPDATE + DELETE
        //

        NamedCache        cache    = getNamedCache(sCacheName);
        AbstractTestStore store    = getStore(cache);
        ObservableMap     mapStore = store.getStorageMap();

        cache.clear();
        mapStore.clear();
        for (int i = 0; i < 10; i++)
            {
            mapStore.put(i, i);
            }

        assertEquals(0, cache.size());

        final List listEvents = new ArrayList();
        MapListener listener = new MultiplexingMapListener()
            {
            protected void onMapEvent(MapEvent evt)
                {
                listEvents.add(evt);
                }
            };
        cache.addMapListener(
            new MapListenerSupport.WrapperSynchronousListener(listener));

        // a1) Read (Preload)
        InvocableMap.EntryProcessor processor = PreloadRequest.INSTANCE;
        testUpdateProcessor("a1) ", cache, store, processor, listEvents,
                            new EventVerifier(new int[] { MapEvent.ENTRY_INSERTED }),
                            10, 0, 0, 0, 0, 0, 0);

        // a2) Read (Preload) - Update
        processor = new CompositeProcessor(new InvocableMap.EntryProcessor[]
            {
            PreloadRequest.INSTANCE,
            new NumberIncrementor((PropertyManipulator) null, 1, false)
            });
        testUpdateProcessor("a2) ", cache, store, processor, listEvents,
                            new EventVerifier(new int[] { MapEvent.ENTRY_INSERTED,
                                                          MapEvent.ENTRY_UPDATED }),
                            10, 2, 0, 0, 1, 0, 0);
        // a3) Read (Preload) - Remove
        processor = new CompositeProcessor(new InvocableMap.EntryProcessor[]
            {
            PreloadRequest.INSTANCE,
            new ConditionalRemove(PresentFilter.INSTANCE)
            });
        testRemoveProcessor("a3) ", cache, store, processor, listEvents,
                            new EventVerifier(new int[] { MapEvent.ENTRY_INSERTED,
                                                          MapEvent.ENTRY_DELETED }),
                            10, 0, 2, 0, 0, 1, 0);

        // a4) Read (Preload) - Evict
        testRemoveProcessor("a4) ", cache, store, new CustomProcessor1(sCacheName), listEvents,
                            new EventVerifier(new int[] { MapEvent.ENTRY_INSERTED,
                                                          MapEvent.ENTRY_DELETED }),
                            10, 0, 0, 0, 0, 0, 0);

        // a5) Read (Preload) - Evict before update (put)
        testUpdateProcessor("a5) ", cache, store, new CustomProcessor2(sCacheName), listEvents,
                            new EventVerifier(new int[] { MapEvent.ENTRY_INSERTED,
                                                          MapEvent.ENTRY_DELETED,
                                                          MapEvent.ENTRY_INSERTED }),
                            10, 2, 0, 0, 1, 0, 0);

        // a6) Read (Preload) - Evict before remove
        testRemoveProcessor("a6) ", cache, store, new CustomProcessor3(sCacheName), listEvents,
                            new EventVerifier(new int[] { MapEvent.ENTRY_INSERTED,
                                                          MapEvent.ENTRY_DELETED }),
                            10, 0, 2, 0, 0, 1, 0);

        // a7) Evict before Read (Preload)
        for (int i = 0; i < 10; i++)
            {
            cache.put(i, i);
            }
        testUpdateProcessor("a7) ", cache, store, new CustomProcessor4(sCacheName), listEvents,
                            new EventVerifier(new int[] { MapEvent.ENTRY_DELETED,
                                                          MapEvent.ENTRY_INSERTED}),
                            10, 0, 0, 0, 0, 0, 10);

        // a8) Evict before no-op
        for (int i = 0; i < 10; i++)
            {
            cache.put(i, i);
            }
        testRemoveProcessor("a8) ", cache, store, new CustomProcessor5(sCacheName), listEvents,
                            new EventVerifier(new int[] { MapEvent.ENTRY_DELETED }),
                            0, 0, 0, 0, 0, 0, 10);

        // a9) Read (Preload) - Evict after update (put)
        testRemoveProcessor("a9) ", cache, store, new CustomProcessor6(sCacheName), listEvents,
                            new EventVerifier(new int[] { MapEvent.ENTRY_INSERTED,
                                                          MapEvent.ENTRY_UPDATED,
                                                          MapEvent.ENTRY_DELETED}),
                            10, 2, 0, 0, 1, 0, 0);

        // b1) Remove without Preload
        // since the backing map is empty, the erase() is called,
        // but REMOVE events are not generated
        testRemoveProcessor("b1) ", cache, store, new CustomProcessor7(sCacheName), listEvents,
                            new EventVerifier(new int[] { }),
                            0, 0, 2, 0, 0, 1, 0);

        // b2) Evict before Update
        for (int i = 0; i < 10; i++)
            {
            cache.put(i, i);
            }
        testUpdateProcessor("b2) ", cache, store, new CustomProcessor8(sCacheName), listEvents,
                            new EventVerifier(new int[] { MapEvent.ENTRY_DELETED,
                                                          MapEvent.ENTRY_INSERTED}),
                            0, 2, 0, 0, 1, 0, 10);

        // b3) Evict before Remove
        for (int i = 0; i < 10; i++)
            {
            cache.put(i, i);
            }
        testRemoveProcessor("b3) ", cache, store, new CustomProcessor9(sCacheName), listEvents,
                            new EventVerifier(new int[] { MapEvent.ENTRY_DELETED}),
                            0, 0, 2, 0, 0, 1, 10);

        // b4) Evict after Update
        for (int i = 0; i < 10; i++)
            {
            cache.put(i, i);
            }
        testRemoveProcessor("b4) ", cache, store, new CustomProcessor10(sCacheName), listEvents,
                            new EventVerifier(new int[] { MapEvent.ENTRY_UPDATED,
                                                          MapEvent.ENTRY_DELETED}),
                            0, 2, 0, 0, 1, 0, 10);

        cache.destroy();
        }

    /**
    * Test the behaviour of remove(fSynthetic);
    */
    @Test
    public void testRemoveSynthetic()
        {
        testRemoveSynthetic("dist-rwbm-wt");
        testRemoveSynthetic("dist-rwbm-wt-bin");

        testRemoveSynthetic("dist-rwbm-nb-nonpc");
        }

    private void testRemoveSynthetic(String sCacheName)
        {
        NamedCache          cache = getNamedCache(sCacheName);
        AbstractTestStore   store = getStore(cache);
        ReadWriteBackingMap rwbm  = getReadWriteBackingMap(cache);
        LocalCache          mapInternal = (LocalCache) rwbm.getInternalCache();

        cache.clear();
        store.getStorageMap().clear();
        store.resetStats();

        Map<Integer,Integer> map = mapOfIntegers(10);

        cache.putAll(map);

        Eventually.assertDeferred(() -> store.getStorageMap().size(), is(map.size()));
        assertEquals("testRemoveSynthetic-" + sCacheName, 10, mapInternal.size());

        store.getStatsMap().clear();

        if (sCacheName.equals("dist-rwbm-nb-nonpc"))
            {
            // async stores: race condition between end of putAll() and invokeAll()
            // give a chance to storeAll() and onNext() to go through
            definiteSleep(2000);
            }

        cache.invokeAll(map.keySet(), new CustomProcessor11());

        assertEquals("testRemoveSynthetic-" + sCacheName, 10, store.getStorageMap().size());
        assertEquals("testRemoveSynthetic-" + sCacheName, 0, mapInternal.size());
        // Verify no interactions with CacheStore
        verifyStoreStats("testRemoveSynthetic-" + sCacheName, store, 0, 0, 0, 0, 0, 0);

        cache.destroy();
        }

    @Test
    public void testRemoveAll()
        {
        testRemoveAll("dist-rwbm-wt");
        testRemoveAll("dist-rwbm-wt-bin");
        testRemoveAll("dist-rwbm-nb-nonpc");
        }

    private void testRemoveAll(String sCacheName)
        {
        NamedCache          cache = getNamedCache(sCacheName);
        AbstractTestStore   store = getStore(cache);
        ReadWriteBackingMap rwbm  = getReadWriteBackingMap(cache);
        LocalCache          mapInternal = (LocalCache) rwbm.getInternalCache();

        cache.clear();
        store.getStorageMap().clear();
        store.resetStats();

        Map<Integer,Integer> map = mapOfIntegers(10);

        // test invokeAll
        cache.putAll(map);

        Eventually.assertDeferred(() -> store.getStorageMap().size(), is(map.size()));
        store.getStatsMap().clear();

        cache.invokeAll(map.keySet(), new ConditionalRemove(AlwaysFilter.INSTANCE));

        assertEquals("testRemoveAll-" + sCacheName, 0, store.getStorageMap().size());
        assertEquals("testRemoveAll-" + sCacheName, 0, mapInternal.size());
        // Verify interactions with CacheStore
        verifyStoreStats("testRemoveAll-" + sCacheName, store, 0, 0, 0, 0, 0, 1);

        // test removeAll() on keySet()
        cache.putAll(map);

        Eventually.assertDeferred(() -> store.getStorageMap().size(), is(map.size()));
        store.getStatsMap().clear();

        cache.keySet().removeAll(map.keySet());

        assertEquals("testRemoveAll-" + sCacheName, 0, store.getStorageMap().size());
        assertEquals("testRemoveAll-" + sCacheName, 0, mapInternal.size());
        // Verify interactions with CacheStore
        verifyStoreStats("testRemoveAll-" + sCacheName, store, 0, 0, 0, 0, 0, 1);

        // test removeAll() on entrySet()
        cache.putAll(map);

        Eventually.assertDeferred(() -> store.getStorageMap().size(), is(map.size()));
        store.getStatsMap().clear();

        cache.entrySet().removeAll(map.entrySet());

        assertEquals("testRemoveAll-" + sCacheName, 0, store.getStorageMap().size());
        assertEquals("testRemoveAll-" + sCacheName, 0, mapInternal.size());
        // Verify interactions with CacheStore
        verifyStoreStats("testRemoveAll-" + sCacheName, store, 0, 0, 0, 0, 0, 1);

        cache.destroy();
        }

    /**
    * Test the behaviour of remove(fSynthetic);
    */
    @Test
    public void testUpdateSynthetic()
        {
        testUpdateSynthetic("dist-rwbm-wt");
        testUpdateSynthetic("dist-rwbm-wt-bin");

        testUpdateSynthetic("dist-rwbm-nb-nonpc");
        }

    private void testUpdateSynthetic(String sCacheName)
        {
        NamedCache          cache = getNamedCache(sCacheName);
        AbstractTestStore   store = getStore(cache);
        ReadWriteBackingMap rwbm  = getReadWriteBackingMap(cache);
        LocalCache          mapInternal = (LocalCache) rwbm.getInternalCache();

        cache.clear();
        store.getStorageMap().clear();
        store.resetStats();

        Map<Integer,Integer> map = mapOfIntegers(10);

        cache.putAll(map);

        Eventually.assertDeferred(() -> store.getStorageMap().size(), is(map.size()));
        assertEquals("testUpdateSynthetic-" + sCacheName, 10, mapInternal.size());

        store.getStatsMap().clear();

        if (sCacheName.equals("dist-rwbm-nb-nonpc"))
            {
            // async stores: race condition between end of putAll() and invokeAll()
            // give a chance to storeAll() and onNext() to go through
            definiteSleep(2000);
            }

        cache.invokeAll(map.keySet(), new CustomProcessor12());

        assertEquals("testUpdateSynthetic-" + sCacheName, 10, store.getStorageMap().size());
        assertEquals("testUpdateSynthetic-" + sCacheName, 10, mapInternal.size());

        for (int i = 0; i < 10; i++)
            {
            final int I = i;
            Eventually.assertDeferred(() -> cache.get(I), is(-i));
            }

        verifyStoreStats("testUpdateSynthetic-" + sCacheName, store, 0, 0, 0, 0, 0, 0);

        cache.destroy();
        }

    /**
    * Test the behaviour of BinaryEntry.updateBinaryValue(binValue, fSynthetic);
    */
    @Test
    public void testUpdateBinarySynthetic()
        {
        testUpdateBinarySynthetic("dist-rwbm-wt");
        testUpdateBinarySynthetic("dist-rwbm-wt-bin");

        testUpdateBinarySynthetic("dist-rwbm-nb-nonpc");
        }

    private void testUpdateBinarySynthetic(String sCacheName)
        {
        NamedCache          cache = getNamedCache(sCacheName);
        AbstractTestStore   store = getStore(cache);
        ReadWriteBackingMap rwbm  = getReadWriteBackingMap(cache);
        LocalCache          mapInternal = (LocalCache) rwbm.getInternalCache();

        cache.clear();
        store.getStorageMap().clear();
        store.resetStats();

        Map<Integer,Integer> map = mapOfIntegers(10);

        cache.putAll(map);

        Eventually.assertDeferred(() -> store.getStorageMap().size(), is(map.size()));
        assertEquals("testUpdateBinarySynthetic-" + sCacheName, 10, mapInternal.size());

        store.getStatsMap().clear();

        if (sCacheName.equals("dist-rwbm-nb-nonpc"))
            {
            // async stores: race condition between end of putAll() and invokeAll()
            // give a chance to storeAll() and onNext() to go through
            definiteSleep(2000);
            }

        cache.invokeAll(map.keySet(), new CustomProcessor13());

        assertEquals("testUpdateBinarySynthetic-" + sCacheName, 10, store.getStorageMap().size());
        assertEquals("testUpdateBinarySynthetic-" + sCacheName, 10, mapInternal.size());

        for (int i = 0; i < 10; i++)
            {
            int I = i;
            Eventually.assertDeferred(() -> cache.get(I), is(-i));
            }

        verifyStoreStats("testUpdateBinarySynthetic-" + sCacheName, store, 0, 0, 0, 0, 0, 0);

        cache.destroy();
        }

    /**
    * Test the behaviour of put() when write-through binary store removes the entry.
    */
    @Test
    public void testCacheStoreRemoveOnWriteThroughPut()
        {
        testCacheStoreRemove("dist-rwbm-wt-bin", false);
        }

    /**
    * Test the behaviour of putAll() when write-through binary store removes the entry.
    */
    @Test
    public void testCacheStoreRemoveOnWriteThroughPutAll()
        {
        testCacheStoreRemove("dist-rwbm-wt-bin", true);
        }

    /**
    * Test the behaviour of put() when write-behind binary store removes the entry.
    */
    @Test
    public void testCacheStoreRemoveOnWriteBehindPut()
        {
        testCacheStoreRemove("dist-rwbm-wb-bin", false);
        testCacheStoreRemove("dist-rwbm-wb-bin-remove", false);
        }

    /**
    * Test the behaviour of putAll() when write-behind binary store removes the entry.
    */
    @Test
    public void testCacheStoreRemoveOnWriteBehindPutAll()
        {
        testCacheStoreRemove("dist-rwbm-wb-bin", true);
        testCacheStoreRemove("dist-rwbm-wb-bin-remove", true);
        }

    /**
     * Test the behaviour of put() when write-behind binary store removes the entry.
     */
    @Test
    public void testCacheStoreRemoveOnAsyncPut()
        {
        testCacheStoreRemove("dist-rwbm-nb-nonpc", false);
        testCacheStoreRemove("dist-rwbm-nb-nonpc", true);
        }

    private void testCacheStoreRemove(String sCacheName, boolean fUsePutAll)
        {
        String               testName    = "testCacheStoreRemove-" + sCacheName + (fUsePutAll ? "-PutAll" : "Put");
        NamedCache           cache       = getNamedCache(sCacheName);
        AbstractTestStore    store       = getStore(cache);
        ReadWriteBackingMap  rwbm        = getReadWriteBackingMap(cache);
        long                 cDelay      = rwbm.isWriteBehind() ?  rwbm.getWriteBehindMillis() + 500 : 0;
        LocalCache           mapInternal = (LocalCache) rwbm.getInternalCache();

        cache.clear();
        store.getStorageMap().clear();
        store.resetStats();

        if (store instanceof TestBinaryCacheStore)
            {
            ((TestBinaryCacheStore) store).setProcessor(TestBinaryCacheStore.REMOVING_PROCESSOR);
            }
        else
            {
            ((TestNonBlockingStore) store).setProcessor(TestNonBlockingStore.REMOVING_PROCESSOR);
            }
        try
            {
            cache.put(0, 0);

            if (cDelay > 0)
                {
                definiteSleep(cDelay);
                }
            else
                {
                // currently the synchronous remove is implemented as an expiry
                definiteSleep(300);
                mapInternal.evict();
                }

            assertEquals(testName, 1, store.getStorageMap().size());
            assertEquals(testName, 0, mapInternal.size());

            Map<Integer,Integer> mapData = mapOfIntegers(10);

            updateCache(cache, mapData, fUsePutAll);

            if (cDelay > 0)
                {
                definiteSleep(cDelay);
                }
            else
                {
                // currently the synchronous remove is implemented as an expiry
                definiteSleep(300);
                mapInternal.evict();
                }

            assertEquals(testName, mapData.size(), store.getStorageMap().size());
            assertEquals(testName, 0, mapInternal.size());
            }
        finally
            {
            cache.destroy();
            }
        }

    /**
    * Test the behaviour of put() when write-through binary store reverts the entry.
    */
    @Test
    public void testCacheStoreRevertOnPutWithWriteThrough()
        {
        testCacheStoreRevert("dist-rwbm-wt-bin", false);
        }

    /**
    * Test the behaviour of putAll() when write-through binary store reverts the entry.
    */
    @Test
    public void testCacheStoreRevertOnPutAllWithWriteThrough()
        {
        testCacheStoreRevert("dist-rwbm-wt-bin", false);
        }

    /**
    * Test the behaviour of put() when write-behind binary store reverts the entry.
    */
    @Test
    public void testCacheStoreRevertOnPutWithWriteBehind()
        {
        testCacheStoreRevert("dist-rwbm-wb-bin", false);
        testCacheStoreRevert("dist-rwbm-wb-bin-remove", false);
        }

    @Test
    public void testCacheStoreRevertOnPutWithNonBlocking()
        {
        testCacheStoreRevert("dist-rwbm-nonblocking", false);
        }

    /**
    * Test the behaviour of putAll() when write-behind binary store reverts the entry.
    */
    @Test
    public void testCacheStoreRevertOnPutAllWithWriteBehind()
        {
        testCacheStoreRevert("dist-rwbm-wb-bin", true);
        testCacheStoreRevert("dist-rwbm-wb-bin-remove", true);
        }

    private void testCacheStoreRevert(String sCacheName, boolean fUsePutAll)
        {
        String               testName = "testCacheStoreRevert-" + sCacheName + (fUsePutAll ? "-PutAll" : "Put");
        NamedCache           cache    = getNamedCache(sCacheName);
        AbstractTestStore    store    = getStore(cache);
        TestBinaryCacheStore binStore = null;
        TestNonBlockingStore nbStore  = null;
        ReadWriteBackingMap  rwbm     = getReadWriteBackingMap(cache);
        long                 cDelay   = rwbm.isWriteBehind() ? rwbm.getWriteBehindMillis() + 500 : 0;

        cache.clear();

        if (store instanceof BinaryEntryStore)
            {
            binStore = (TestBinaryCacheStore) store;
            binStore.getStorageMap().clear();
            }
        else
            {
            // add delay for NB store, so actual storing goes through
            nbStore = (TestNonBlockingStore) store;
            nbStore.getStorageMap().clear();
            cDelay = 5000;
            }

        Map<Integer,Integer> mapInitial = mapOfIntegers(10);
        updateCache(cache, mapInitial, fUsePutAll);

        if (cDelay > 0)
            {
            // have the store operation to go through
            definiteSleep(cDelay);
            }

        if (store instanceof BinaryEntryStore)
            {
            binStore.setProcessor(TestBinaryCacheStore.REVERTING_PROCESSOR);
            }
        else
            {
            nbStore.setProcessor(TestNonBlockingStore.REVERTING_PROCESSOR);
            }

        try
            {
            Map<Integer,Integer> mapUpdate = mapOfIntegers(10, 0, 10);
            updateCache(cache, mapUpdate, fUsePutAll);

            assertThat(cache.size(), is(mapInitial.size()));

            if (cDelay > 0)
                {
                definiteSleep(cDelay);
                }

            for (int i : mapInitial.keySet())
                {
                assertThat(testName + " wrong cache contents for key=" + i, cache.get(i), is(mapInitial.get(i)));
                }
            }
        finally
            {
            cache.destroy();
            }
        }

    /**
    * Test the behaviour of updating the binary via a PofUpdater for both
    * write-through, write-behind and non-blocking using put() and putAll()
    * to update the cache.
    */
    @Test
    public void testCacheStoreUpdateOnWriteThroughPut()
        {
        testCacheStoreUpdate("dist-rwbm-wt-bin-pof", false);
        }

    @Test
    public void testCacheStoreUpdateOnWriteThroughPutAll()
        {
        testCacheStoreUpdate("dist-rwbm-wt-bin-pof", true);
        }

    @Test
    public void testCacheStoreUpdateOnWriteBehindPut()
        {
        testCacheStoreUpdate("dist-rwbm-wb-bin-pof", false);
        }

    @Test
    public void testCacheStoreUpdateOnWriteBehindPutAll()
        {
        testCacheStoreUpdate("dist-rwbm-wb-bin-pof", true);
        }

    @Test
    public void testCacheStoreUpdateOnWriteAsyncPut()
        {
        testCacheStoreUpdate("dist-rwbm-nonblocking-pof", false);
        }

    @Test
    public void testCacheStoreUpdateOnWriteAsyncPutAll()
        {
        testCacheStoreUpdate("dist-rwbm-nonblocking-pof", true);
        }

    private void testCacheStoreUpdate(String sCacheName, boolean fUsePutAll)
        {
        String               testName = "testCacheStoreUpdate-" + sCacheName + (fUsePutAll ? "-PutAll" : "Put");
        NamedCache           cache    = getNamedCache(sCacheName);
        AbstractTestStore    store    = getStore(cache);
        ReadWriteBackingMap  rwbm     = getReadWriteBackingMap(cache);
        long                 cDelay   = rwbm.isWriteBehind() ? rwbm.getWriteBehindMillis() + 500 : 0;
        Converter            convDown = cache.getCacheService()
                                                .getBackingMapManager().getContext().getKeyToInternalConverter();

        cache.clear();
        store.getStorageMap().clear();
        store.resetStats();

        if (store instanceof TestBinaryCacheStore)
            {
            ((TestBinaryCacheStore) store).setProcessor(new AbstractProcessor()
                {
                public Object process(Entry entry)
                    {
                    entry.update(new PofUpdater(Person.MOTHER_SSN), "STORED");

                    return null;
                    }
                });
            }
        else
            {
            ((TestNonBlockingStore) store).setProcessor(new AbstractProcessor()
                {
                public Object process(Entry entry)
                    {
                    entry.update(new PofUpdater(Person.MOTHER_SSN), "STORED");

                    return null;
                    }
                });
            // non-blocking needs delay
            cDelay = 5000L;
            }

        try
            {
            Map<Integer,Person> mapData = mapOfPeople(10);
            updateCache(cache, mapData, fUsePutAll);

            if (cDelay > 0)
                {
                // have the store operation to go through
                definiteSleep(cDelay);
                }

            for (int i = 0; i < mapData.size(); i++)
                {
                Eventually.assertThat(testName,
                    invoking(this).getPerson(cache, i).getMotherId(), is("STORED"));
                if (cDelay > 0)
                    {
                    final int ii = i;

                    // allow async processing to finish
                    Eventually.assertDeferred(() -> ExternalizableHelper.isDecorated((Binary)
                                                         rwbm.getInternalCache().get(convDown.convert(ii)),
                                                         ExternalizableHelper.DECO_STORE),
                                              is (false));
                    }
                }
            }
        finally
            {
            cache.destroy();
            }
        }

    /**
     * A helper method for an "eventual" assert.
     */
    public Person getPerson(NamedCache cache, Integer NKey)
        {
        return (Person) cache.get(NKey);
        }

    /**
     * Test the getAll() functionality.
     */
     @Test
     public void testGetAll()
         {
         NamedCache          cache       = getNamedCache("dist-rwbm-ra-wb-timeout");
         ReadWriteBackingMap rwbm        = getReadWriteBackingMap(cache);
         LocalCache          mapInternal = (LocalCache) rwbm.getInternalCache();
         AbstractTestStore   store       = getStore(cache);
         ObservableMap       map         = store.getStorageMap();

         cache.clear();
         map.clear();
         store.getStatsMap().clear();

         cache.put(1, "One");
         cache.put(3, "Three");
         cache.put(5, "Five");
         cache.put(7, "Seven");
         rwbm.flush();

         assertEquals(4, map.size());

         mapInternal.clear();
         assertEquals(4, map.size());
         assertEquals(0, mapInternal.size());

         cache.put(2, "Two");

         HashSet setKeys = new HashSet();
         for (int i = 0; i < 6; i++)
             {
             setKeys.add(i);
             }

         Map all = cache.getAll(setKeys);
         assertEquals(1, (int) store.getStatsMap().get("loadAll"));
         assertEquals(4, all.size());

         assertEquals("One",all.get(1));
         assertEquals("Two",all.get(2));
         assertEquals("Three",all.get(3));
         assertEquals("Five",all.get(5));
         }


     /**
      * Test the getAll() with empty set.
      */
      @Test
      public void testGetAllEmptyKeys()
          {
          NamedCache          cache       = getNamedCache("dist-rwbm-ra-wb-timeout");
          ReadWriteBackingMap rwbm        = getReadWriteBackingMap(cache);
          LocalCache          mapInternal = (LocalCache) rwbm.getInternalCache();
          AbstractTestStore   store       = getStore(cache);
          ObservableMap       map         = store.getStorageMap();

          store.getStatsMap().clear();

          cache.clear();
          map.clear();

          cache.put(1, "One");
          cache.put(3, "Three");
          cache.put(5, "Five");
          cache.put(7, "Seven");
          rwbm.flush();

          assertEquals(4, map.size());

          mapInternal.clear();
          assertEquals(4, map.size());
          assertEquals(0, mapInternal.size());

          cache.put(2, "Two");

          HashSet setKeys = new HashSet();
          Map all = cache.getAll(setKeys);
          assertNull(store.getStatsMap().get("loadAll"));

          assertEquals(0, all.size());
          }

      /**
       * Test the getAll() with empty set.
       */
       @Test
       public void testGetAllEmptyCache()
           {
           NamedCache        cache  = getNamedCache("dist-rwbm-ra-wb-timeout");
           AbstractTestStore store  = getStore(cache);
           ObservableMap     map    = store.getStorageMap();

           cache.clear();
           map.clear();
           store.getStatsMap().clear();

           HashSet setKeys = new HashSet();
           for (int i = 0; i < 6; i++)
               {
               setKeys.add(i);
               }

           Map all = cache.getAll(setKeys);
           assertEquals(1, (int) store.getStatsMap().get("loadAll"));
           assertEquals(0, all.size());
           }

    /**
    * Test the behavior of expiry set by a write-through CacheStore on a put()
    */
    @Test
    public void testCacheStoreExpireOnPutWithWriteThrough()
        {
        testCacheStoreExpire("dist-rwbm-wt-bin", 1000, false);
        }

    /**
    * Test the behavior of expiry set by a write-through CacheStore on a put()
    */
    @Test
    public void testCacheStoreExpireOnPutAllWithWriteThrough()
        {
        testCacheStoreExpire("dist-rwbm-wt-bin", 1000, true);
        }

    /**
    * Test the behavior of expiry set by a write-through CacheStore on a put()
    */
    @Test
    public void testCacheStoreExpireOnPutWithWriteBehind()
        {
        testCacheStoreExpire("dist-rwbm-wb-bin", 1000, false);
        testCacheStoreExpire("dist-rwbm-wb-bin-remove", 1000, false);
        }

    /**
    * Test the behavior of expiry set by a write-through CacheStore on a put()
    */
    @Test
    public void testCacheStoreExpireOnPutAllWithWriteBehind()
        {
        testCacheStoreExpire("dist-rwbm-wb-bin", 1000, true);
        testCacheStoreExpire("dist-rwbm-wb-bin-remove", 1000, true);
        }

    private void testCacheStoreExpire(String sCacheName, long cExpiryMillis, boolean fUsePutAll)
        {
        String               testName    = "testCacheStoreExpire-" + sCacheName + (fUsePutAll ? "-PutAll" : "Put");
        NamedCache           cache       = getNamedCache(sCacheName);
        TestBinaryCacheStore store       = (TestBinaryCacheStore) getStore(cache);
        ReadWriteBackingMap  rwbm        = getReadWriteBackingMap(cache);
        LocalCache           mapInternal = (LocalCache) rwbm.getInternalCache();

        long cDelay = rwbm.isWriteBehind() ?
                rwbm.getWriteBehindMillis() + 500 : 0;

        cache.clear();
        store.getStorageMap().clear();

        Map<Integer,Integer> mapInitial = mapOfIntegers(10);
        updateCache(cache, mapInitial, fUsePutAll);

        if (cDelay > 0)
            {
            // have the store operation to go through
            definiteSleep(cDelay);
            }

        store.setProcessor(new ExpireProcessor(cExpiryMillis));
        try
            {
            Map<Integer,Integer> mapUpdate = mapOfIntegers(10, 0, 10);
            updateCache(cache, mapUpdate, fUsePutAll);

            if (cDelay > 0)
                {
                definiteSleep(cDelay);
                }
            assertEquals(testName, 10, store.getStorageMap().size());
            Eventually.assertDeferred(testName, mapInternal::size, is(10));

            definiteSleep(cExpiryMillis + 100L);

            mapInternal.evict();

            assertEquals(testName, 10, store.getStorageMap().size());
            assertEquals(testName, 0, mapInternal.size());
            }
        finally
            {
            cache.destroy();
            }
        }

    @Test
    public void testNoneDefaultExpiry()
        {
        noneDefaultExpiry("dist-rwbm-wb-bin");
        noneDefaultExpiry("dist-rwbm-wb-bin-remove");
        }

    protected void noneDefaultExpiry(String sCacheName)
        {
        NamedCache           cache         = getNamedCache(sCacheName);
        TestBinaryCacheStore store         = (TestBinaryCacheStore) getStore(cache);
        ReadWriteBackingMap  rwbm          = getReadWriteBackingMap(cache);
        LocalCache           mapInternal   = (LocalCache) rwbm.getInternalCache();
        long                 cExpiryMillis = 2000L;
        long                 cDelay        = 1000L ; // RWBM.ACCELERATE_MIN

        cache.clear();
        store.getStorageMap().clear();

        for (int i = 0; i < 10; i++)
            {
            cache.put(i, i, cExpiryMillis);
            }

        definiteSleep(cExpiryMillis + 100L);

        mapInternal.evict();

        // the entries ripe time on the write queue should be accelerated and
        // should be written to the store within 1 second
        definiteSleep(cDelay + 10L);

        assertEquals("testNoneDefaultExpiry-dist-rwbm-wb-bin", 10, store.getStorageMap().size());
        assertEquals("testNoneDefaultExpiry-dist-rwbm-wb-bin", 0, mapInternal.size());

        cache.destroy();
        }

    @Test
    public void testBinaryStoreNoneDefaultExpiry()
        {
        NamedCache           cache         = getNamedCache("dist-rwbm-wb-bin");
        TestBinaryCacheStore store         = (TestBinaryCacheStore) getStore(cache);
        ReadWriteBackingMap  rwbm          = getReadWriteBackingMap(cache);
        LocalCache           mapInternal   = (LocalCache) rwbm.getInternalCache();
        long                 cExpiryMillis = 2000L;

        long cDelay = rwbm.isWriteBehind() ?
                rwbm.getWriteBehindMillis() + 500 : 0;

        cache.clear();
        store.getStorageMap().clear();

        for (int i = 0; i < 10; i++)
            {
            cache.put(i, i);
            }

        if (cDelay > 0)
            {
            // have the store operation to go through
            definiteSleep(cDelay);
            }

        store.setProcessor(new ExpireProcessor(cExpiryMillis));
        try
            {
            for (int i = 0; i < 10; i++)
                {
                cache.put(i, 10 + i, cExpiryMillis);
                }

            if (cDelay > 0)
                {
                definiteSleep(cDelay);
                }

            assertEquals("testBinaryStoreNoneDefaultExpiry-dist-rwbm-wb-bin", 10, store.getStorageMap().size());

            definiteSleep(cExpiryMillis + 250L);

            assertEquals("testBinaryStoreNoneDefaultExpiry-dist-rwbm-wb-bin", 10, store.getStorageMap().size());
            assertEquals("testBinaryStoreNoneDefaultExpiry-dist-rwbm-wb-bin", 0, mapInternal.size());
            }
        finally
            {
            cache.destroy();
            }
        }

    @Test
    public void testCacheStoreTimeout()
        {
        NamedCache          cache       = getNamedCache("dist-rwbm-ra-wb-timeout");
        ReadWriteBackingMap rwbm        = getReadWriteBackingMap(cache);
        LocalCache          mapInternal = (LocalCache) rwbm.getInternalCache();
        AbstractTestStore   store       = getStore(cache);
        ObservableMap       map         = store.getStorageMap();
        long                cWriteDelay = rwbm.getCacheStoreTimeoutMillis();
        long                cTimeout    = rwbm.getCacheStoreTimeoutMillis();
        long                cStoreDelay = 2 * cTimeout;
        long                cExpiry     = mapInternal.getExpiryDelay();
        Object              sValue;

        store.setDurationLoad      (cStoreDelay);
        store.setDurationLoadAll   (cStoreDelay);
        store.setDurationStore     (cStoreDelay);
        store.setDurationStoreAll  (cStoreDelay);
        store.setInterruptThreshold(0);
        store.setVerbose           (true);

        out("+++ testCacheStoreTimeout: This test is expected to produce guardian error messages +++");
        try
            {
            map.clear();
            cache.put("Key1", "Value1");
            assertTrue("put() caused an immediate store.", map.isEmpty());

            sValue = cache.get("Key1");
            assertTrue("get() returned wrong value: " + sValue, "Value1".equals(sValue));

            definiteSleep(cWriteDelay + 500);
            assertTrue("write-behind should not have generated a store.", map.isEmpty());

            definiteSleep(cStoreDelay); // we are finished with the 1st store()
            assertTrue("CacheService should not have restarted",
                       rwbm == getReadWriteBackingMap(cache));


            cache.put("Key2", "Value2");
            assertTrue("put() caused an immediate store.", map.size() == 1);

            sValue = cache.get("Key2");
            assertTrue("get() returned wrong value: " + sValue, "Value2".equals(sValue));

            definiteSleep(cWriteDelay + 500);
            assertTrue("write-behind should not have generated a store.", map.size() == 1);

            definiteSleep(cStoreDelay); // we are finished with the 2nd store()
            assertTrue("CacheService should not have restarted",
                       rwbm == getReadWriteBackingMap(cache));

            definiteSleep(cExpiry);
            assertTrue("CacheService should not have restarted",
                       rwbm == getReadWriteBackingMap(cache));
            }
        finally
            {
            cache.destroy();
            out("--- testCacheStoreTimeout ---");
            }
        }
    /**
    * Regression test for flushing of out-of-band updates (COH-4631).
    */
    @Test
    public void testOutOfBandUpdates()
        {
        outOfBandUpdates("dist-rwbm-wb");
        outOfBandUpdates("dist-rwbm-wb-remove");
        }

    protected void outOfBandUpdates(String sCacheName)
        {
        NamedCache           cache = getNamedCache(sCacheName);
        TestCacheStore       store = (TestCacheStore) getStore(cache);
        ReadWriteBackingMap  rwbm  = getReadWriteBackingMap(cache);

        assertTrue("must be write-behind", rwbm.isWriteBehind());

        cache.clear();
        store.getStorageMap().clear();

        final int SIZE = 1000;
        for (int i = 0; i < SIZE; i++)
            {
            cache.put(i, i);
            }

        // wait for all store operation to go through
        long cDelay = rwbm.getWriteBehindMillis() + 100;
        for (int i = 0; ; i++)
            {
            definiteSleep(cDelay);
            if (store.getStorageMap().size() == SIZE)
                {
                break;
                }
            assertTrue("failed to store within " + i*cDelay/1000 + " seconds", i < 5);
            }

        // now check that the event fabric gets flushed eventually
        com.tangosol.coherence.component.util.daemon.queueProcessor.
            service.grid.partitionedService.PartitionedCache
                service =
        (com.tangosol.coherence.component.util.daemon.queueProcessor.
            service.grid.partitionedService.PartitionedCache)
                rwbm.getContext().getCacheService();

        com.tangosol.internal.util.BMEventFabric.LinkedQueue queueOOB =
            service.getResourceCoordinator().getUnmanagedEventQueue();

        for (int i = 0; ; i++)
            {
            int cPending = queueOOB.getSize();

            // internally hardcoded threshold - see PartitionedCache#onNotify
            if (cPending < 32)
                {
                break;
                }

            long cWaitMillis = Math.max(service.getWaitMillis(), 1000L);
            definiteSleep(cWaitMillis);
            assertTrue("failed to flush the queue within " + i + " seconds; " +
                       "pending " + cPending + " events", i < 5);
            }
        }

    /**
    * test for expiry value on store/storeAll and load/loadAll from BinaryEntryStore (COH-5479).
    */
    @Test
    public void testExpiryOnStoreAndLoad()
        {
        NamedCache          cache           = getNamedCache("rwbm-bin-entry-expiry");
        ReadWriteBackingMap rwbm            = getReadWriteBackingMap(cache);
        AbstractTestStore   store           = getStore(cache);
        ObservableMap       map             = store.getStorageMap();
        long                cWriteDelay     = rwbm.getWriteBehindSeconds() * 1000L + 2000L;
        long                cBinEntryExpiry = 4000L;
        Object              sValue;
        try
            {
            map.clear();
            cache.clear();

            map.put("Key1", "Value1");
            map.put("Key2", "Value2");
            assertEquals(0, cache.size());
            assertEquals(2, map.size());

            cache.put("Key3", "Value3");
            cache.put("Key4", "Value4");
            definiteSleep(cWriteDelay);
            assertEquals(4, map.size());
            definiteSleep(cBinEntryExpiry);
            assertEquals("rwbm does not respect expiry value on store ", 0, cache.size());

            sValue = cache.get("Key1");
            assertEquals(1, cache.size());
            assertEquals(4, map.size());
            assertTrue("get() returned wrong value: " + sValue, "Value1".equals(sValue));
            definiteSleep(cBinEntryExpiry);
            assertEquals(4, map.size());
            assertEquals("rwbm does not respect expiry value on load ", 0, cache.size());

            Map mapTemp = new HashMap();
            for (int i = 0; i < 2; i++)
                {
                mapTemp.put(i, i);
                }
            cache.putAll(mapTemp);
            assertEquals(2, cache.size());
            definiteSleep(cWriteDelay);
            assertEquals(6, map.size());

            definiteSleep(cBinEntryExpiry);
            assertEquals("rwbm does not respect expiry value on storeAll ", 0, cache.size());

            definiteSleep(6000);
            HashSet setKeys = new HashSet();
            for (int i = 1; i < 3; i++)
                {
                setKeys.add("Key"+i);
                }

            mapTemp = cache.getAll(setKeys);
            assertEquals(2, mapTemp.size());
            definiteSleep(cBinEntryExpiry);
            assertEquals("rwbm does not respect expiry value on loadAll ", 0, cache.size());
            }
        finally
            {
            cache.destroy();
            out("--- testExpiryOnStoreAndLoad done ---");
            }
        }

    /**
     * Test the RWBM listener functionality where the listener is declared as a child
     * of RWBM.
     */
     @Test
     public void testListener()
         {
         NamedCache cache = getNamedCache("dist-rwbm-listener");

         Listener.clearInserted();
         cache.put(1, "One");
         assertTrue(cache.get(1).equals("One"));
         assertTrue(Listener.wasInserted());
         }

     /**
      * Test the RWBM listener functionality where the listener is declared as a child
      * of RWBM internal map.
      */
      @Test
      public void testListenerInternal()
          {
          NamedCache cache = getNamedCache("dist-rwbm-listener-internal");

          Listener.clearInserted();
          cache.put(2, "Two");
          assertTrue(cache.get(2).equals("Two"));
          assertTrue(Listener.wasInserted());
          }

    /**
     * Test the RWBM cache EntryEvent interceptor
     */
    @Test
    public void testInterceptor()
        {
        NamedCache cache = getNamedCache("dist-rwbm-interceptor");

        // test load from store
        AbstractTestStore store    = getStore(cache);
        ObservableMap     mapStore = store.getStorageMap();

        cache.clear();
        mapStore.clear();
        CacheEventInterceptor.clearEvent();
        mapStore.put(1, "One");

        assertTrue(cache.get(1).equals("One"));
        assertTrue(CacheEventInterceptor.getEventList().contains(EntryEvent.Type.INSERTING));
        Eventually.assertThat(invoking(this).getEventList(), hasItems(EntryEvent.Type.INSERTED));

        CacheEventInterceptor.clearEvent();
        cache.put(1, "OneOne");
        assertTrue(cache.get(1).equals("OneOne"));
        assertTrue(CacheEventInterceptor.getEventList().contains(EntryEvent.Type.UPDATING));
        Eventually.assertThat(invoking(this).getEventList(), hasItem(EntryEvent.Type.UPDATED));
        }

    /**
    * Verifies that a put followed by remove of the key
    * does not lose the remove when the store of the put
    * value is delayed (COH-6033).
    */
    @Test
    public void testRemoveAfterDelayedPut()
        {
        removeAfterDelayedPut("dist-rwbm-wb");
        removeAfterDelayedPut("dist-rwbm-wb-remove");
        }

    protected void removeAfterDelayedPut(String sCacheName)
        {
        NamedCache cache = getNamedCache(sCacheName);
        TestCacheStore store = (TestCacheStore) getStore(cache);
        ReadWriteBackingMap rwbm = getReadWriteBackingMap(cache);

        rwbm.setWriteBehindSeconds(4);
        store.setDurationStore(2000l);

        cache.clear();
        store.getStorageMap().clear();

        store.setVerboseErase(true);
        store.setVerboseStore(true);

        cache.put("removeAfterDelayedPut-key", "removeAfterDelayedPut-value");

        definiteSleep(5000l);
        cache.remove("removeAfterDelayedPut-key");
        definiteSleep(2000l);
        Object result = cache.get("removeAfterDelayedPut-key");
        if (sCacheName.contains("remove"))
            {
            assertEquals(null, result);
            }
        else
            {
            assertNull(result);
            }
        cache.destroy();
        }

    /*
    * Test the use of ConfigurableCacheMap.EvictionApprover in RWBM.
    * Eviction is disallowed if the entry is pending to be stored,
    * because the eviction would block a service thread instead of
    * executing the store on the write-behind thread (COH-6163).
    *
    * The test fails if the cache service thread is terminated by the guardian,
    * because it is stalled by the long-running store operation.
    */
    @Test
    public void evictionApprover()
        {
        NamedCache cache = getNamedCache("dist-rwbm-wb-ea");
        ConditionalDelayCacheStore store = (ConditionalDelayCacheStore) getStore(cache);

        store.removeStoreDelayConditions();
        store.addStoreDelayConditionMethod("evict");
        store.addStoreDelayConditionClass("ReadWriteBackingMap.WriteThread");
        store.setDurationStore(1000000L);
        store.setVerboseStore(true);

        cache.clear();
        store.getStorageMap().clear();

        for (int i = 0; i < 100; i = i + 5)
            {
            cache.put(i,   "evictionApprover-" + i);
            cache.put(i+1, "evictionApprover-" + (i+1));
            cache.put(i+2, "evictionApprover-" + (i+2));
            cache.put(i+3, "evictionApprover-" + (i+3));
            cache.put(i+4, "evictionApprover-" + (i+4));
            definiteSleep(120l);  // expiry-delay is 100ms
            }

        // probe a few keys
        Object result = cache.get(0);
        assertEquals("Incorrect result for key 0", "evictionApprover-0", result);
        result = cache.get(54);
        assertEquals("Incorrect result for key 54", "evictionApprover-54", result);
        result = cache.get(99);
        assertEquals("Incorrect result for key 99", "evictionApprover-99", result);

        store.removeStoreDelayConditions();
        cache.destroy();

        }

    /*
     * Test the default configuration settings that a applicable when RWBM uses write-behind.
     * NOTE: This is just testing configuration, not the behavior associated with
     * that configuration.
     */
    @Test
    public void testConfigDefaults()
        {
        NamedCache cache = getNamedCache("dist-rwbm-config-defaults");
        try
            {
            ReadWriteBackingMap rwbm = getReadWriteBackingMap(cache);

            // assert RWBM configuration
            assertEquals(rwbm.getWriteMaxBatchSize(), 128);
            assertNull(rwbm.getMissesCache());
            assertFalse(rwbm.isReadOnly());
            assertEquals(rwbm.getWriteBehindMillis(), 0);
            assertTrue(rwbm.getWriteBatchFactor() == 0);
            assertEquals(rwbm.getWriteRequeueThreshold(), 0);
            assertTrue(rwbm.getRefreshAheadFactor() == 0);
            assertEquals(rwbm.getCacheStoreTimeoutMillis(), 0);
            assertTrue(rwbm.isRethrowExceptions());
            assertFalse(rwbm.isWriteBehindRemove());
            }
        finally
            {
            cache.destroy();
            }
        }

    /*
     * Test the default configuration with read-only set to true.
     * NOTE: This is just testing configuration, not the behavior associated with
     * that configuration.
     */
    @Test
    public void testConfigReadOnly()
        {
        NamedCache cache = getNamedCache("dist-rwbm-config-readonly");
        try
            {
            ReadWriteBackingMap rwbm = getReadWriteBackingMap(cache);

            assertTrue(rwbm.isReadOnly());
            }
        finally
            {
            cache.destroy();
            }
        }

    /*
     * Test if the unit calculator defined in config is honored
     */
    @Test
    public void testUnitCalculatorConfig()
        {
        NamedCache          cache       = getNamedCache("dist-rwbm-coh10009");
        ReadWriteBackingMap mapRWBM     = getReadWriteBackingMap(cache);
        Map                 mapInternal = mapRWBM.getInternalCache();

        if (mapInternal instanceof ConfigurableCacheMap)
            {
            assertNotNull(((ConfigurableCacheMap)mapInternal).getUnitCalculator());
            }
        }

    /*
     * Test the configuration settings that a applicable when RWBM uses write-behind.
     * NOTE: This is just testing configuration, not the behavior associated with
     * that configuration.
     */
    @Test
    public void testConfigWb()
        {
        NamedCache cache = getNamedCache("dist-rwbm-config-wb");
        try
            {
            ReadWriteBackingMap rwbm = getReadWriteBackingMap(cache);

            // assert RWBM configuration
            assertEquals(rwbm.getWriteMaxBatchSize(), 200);
            assertEquals(rwbm.getMissesCache().getClass(), CustomClasses.CustomLocalCache.class);
            assertEquals(rwbm.getWriteBehindMillis(), 10 * 1000);
            assertTrue(rwbm.getWriteBatchFactor() == .6);
            assertEquals(rwbm.getWriteRequeueThreshold(), 100);
            assertTrue(rwbm.getRefreshAheadFactor() == .5);
            assertEquals(rwbm.getCacheStoreTimeoutMillis(), 20 * 1000);
            assertFalse(rwbm.isRethrowExceptions());
            assertFalse(rwbm.isWriteBehindRemove());
            }
        finally
            {
            cache.destroy();
            }
        }

    @Test
    public void testConfigWbRemove()
        {
        NamedCache cache = getNamedCache("dist-rwbm-wb-expiry-remove");
        try
            {
            ReadWriteBackingMap rwbm = getReadWriteBackingMap(cache);
            assertTrue(rwbm.isWriteBehindRemove());
            }
        finally
            {
            cache.destroy();
            }
        }

    /**
     * Tests to ensure a mutation that only changes the expiry does not result
     * in a CacheStore.store.
     */
    @Test
    public void testExpiringProcessor()
        {
        testExpiringProcessor("dist-rwbm-wt-bin", 1000);
        testExpiringProcessor("dist-rwbm-wb-bin", 1000);
        }

    private void testExpiringProcessor(String sCacheName, long cExpiry)
        {
        NamedCache           cache = getNamedCache(sCacheName);
        TestBinaryCacheStore store = (TestBinaryCacheStore) getStore(cache);
        ReadWriteBackingMap  rwbm  = getReadWriteBackingMap(cache);

        long cDelay = rwbm.isWriteBehind()
                        ? rwbm.getWriteBehindMillis() + 500 : 0;

        cache.clear();
        store.getStorageMap().clear();

        cache.put(1, 1);

        definiteSleep(cDelay);

        Integer IStoreCount = (Integer) store.getStatsMap().get("store");
        assertEquals("expected store to be called", Integer.valueOf(1), IStoreCount);

        cache.invoke(1, new CustomProcessor14(cExpiry));

        definiteSleep(cDelay);

        IStoreCount = (Integer) store.getStatsMap().get("store");
        assertEquals("expected store not to be called", Integer.valueOf(1), IStoreCount);
        }

    /**
     * Tests cache.truncate
     */
    @Test
    public void testCacheTruncate()
        {
        testTruncate("dist-rwbm-wt-bin");
        testTruncate("dist-rwbm-wb-bin");
        }

    private void testTruncate(String sCacheName)
        {
        NamedCache           cache = getNamedCache(sCacheName);
        TestBinaryCacheStore store = (TestBinaryCacheStore) getStore(cache);
        ObservableMap        map   = store.getStorageMap();
        ReadWriteBackingMap  rwbm  = getReadWriteBackingMap(cache);

        long cDelay = rwbm.isWriteBehind()
                ? rwbm.getWriteBehindMillis() + 500 : 0;

        cache.clear();
        map.clear();

        cache.put("key1", "val1");
        definiteSleep(cDelay);

        cache.truncate();

        assertTrue(cache.isActive());
        System.out.println(" cache size " + cache.size() + "store size " + map.size());

        cache.put("key2", "val2");
        definiteSleep(cDelay);

        cache.truncate();

        assertTrue(cache.isActive());
        System.out.println(" cache size " + cache.size() + "store size " + map.size());
        }

    /**
    * Internal helper. It calls:
    *
    * 1) invoke for key 0
    * 2) invokeAll for a singleton set containing key 1
    * 3) invokeAll for keys 2-10
    */
    private void testUpdateProcessor(String sTest, NamedCache cache,
        AbstractTestStore store, InvocableMap.EntryProcessor processor,
        List listEvents, EventVerifier verifier, int cLoad, int cStore, int cErase,
        int cLoadAll, int cStoreAll, int cEraseAll, int cInitSize)
        {
        Integer ICount;
        int     nCount;
        Set     setKeys;
        Map     mapStore = store.getStorageMap();

        out(sTest + "testUpdateProcessor");
        listEvents.clear();
        store.resetStats();
        mapStore.clear();
        for (int i = 0; i < 10; i++)
            {
            mapStore.put(i, i);
            }

        int cExpect = Math.max(1, cInitSize);

        cache.invoke(0, processor);
        assertEquals(sTest + "invoke failed to load", cExpect, cache.size());

        ICount = (Integer) cache.aggregate(AlwaysFilter.INSTANCE, new Count());
        assertEquals(sTest + "invoke failed to update key index", cExpect, ICount.intValue());

        nCount = cache.keySet(AlwaysFilter.INSTANCE).size();
        assertEquals(sTest + "invoke failed to update key index", cExpect, nCount);

        assertTrue(sTest + " missing entry 0", cache.keySet().contains(0));

        cExpect = Math.max(2, cInitSize);
        setKeys = Collections.singleton(1);
        cache.invokeAll(setKeys, processor);
        assertEquals(sTest + "invokeAll(single) failed", cExpect, cache.size());

        ICount = (Integer) cache.aggregate(AlwaysFilter.INSTANCE, new Count());
        assertEquals(sTest + "invokeAll(single) failed to update key index", cExpect, ICount.intValue());

        nCount = cache.keySet(AlwaysFilter.INSTANCE).size();
        assertEquals(sTest + "invokeAll(single) failed to update key index", cExpect, nCount);

        assertTrue(sTest + " missing entries " + setKeys, cache.keySet().containsAll(setKeys));

        setKeys = new HashSet();
        for (int i = 2; i < 10; i++)
            {
            setKeys.add(i);
            }

        cExpect = Math.max(10, cInitSize);
        cache.invokeAll(setKeys, processor);
        assertEquals(sTest + "invokeAll(set) failed", cExpect, cache.size());

        ICount = (Integer) cache.aggregate(AlwaysFilter.INSTANCE, new Count());
        assertEquals(sTest + "invokeAll(set) failed to update key index", cExpect, ICount.intValue());

        nCount = cache.keySet(AlwaysFilter.INSTANCE).size();
        assertEquals(sTest + "invokeAll(set) failed to update key index", cExpect, nCount);

        assertTrue(sTest + " missing entries " + setKeys, cache.keySet().containsAll(setKeys));

        verifyEventStats(sTest, listEvents, verifier);
        verifyStoreStats(sTest, store, cLoad, cStore, cErase, cLoadAll, cStoreAll, cEraseAll);

        cache.clear();
        assertEquals(sTest + "clear failed", 0, cache.size());
        }

    /**
    * Internal helper.
    */
    private void testRemoveProcessor(String sTest, NamedCache cache,
        AbstractTestStore store, InvocableMap.EntryProcessor processor,
        List listEvents, EventVerifier verifier,
        int cLoad, int cStore, int cErase,
        int cLoadAll, int cStoreAll, int cEraseAll, int cInitSize)
        {
        Integer ICount;
        int     nCount;
        Set     setKeys;
        Map     mapStore = store.getStorageMap();

        out(sTest + "testRemoveProcessor");
        listEvents.clear();
        store.resetStats();
        mapStore.clear();
        for (int i = 0; i < 10; i++)
            {
            mapStore.put(i, i);
            }

        int cExpect = Math.max(0, cInitSize - 1);
        cache.invoke(0, processor);

        assertEquals(sTest + "invoke failed", cExpect, cache.size());

        ICount = (Integer) cache.aggregate(AlwaysFilter.INSTANCE, new Count());
        assertEquals(sTest + "invoke failed to update key index", cExpect, ICount.intValue());

        nCount = cache.keySet(AlwaysFilter.INSTANCE).size();
        assertEquals(sTest + "invoke failed to update key index", cExpect, nCount);

        cExpect = Math.max(0, cInitSize - 2);
        cache.invokeAll(Collections.singleton(1), processor);
        assertEquals(sTest + "invokeAll(single) failed", cExpect, cache.size());

        ICount = (Integer) cache.aggregate(AlwaysFilter.INSTANCE, new Count());
        assertEquals(sTest + "invokeAll(single) failed to update key index", cExpect, ICount.intValue());

        nCount = cache.keySet(AlwaysFilter.INSTANCE).size();
        assertEquals(sTest + "invokeAll(single) failed to update key index", cExpect, nCount);

        setKeys = new HashSet();
        for (int i = 2; i < 10; i++)
            {
            setKeys.add(i);
            }

        cExpect = Math.max(0, cInitSize - 10);
        cache.invokeAll(setKeys, processor);
        assertEquals(sTest + "invokeAll(set) failed", cExpect, cache.size());

        ICount = (Integer) cache.aggregate(AlwaysFilter.INSTANCE, new Count());
        assertEquals(sTest + "invokeAll(set) failed to update key index", cExpect, ICount.intValue());

        nCount = cache.keySet(AlwaysFilter.INSTANCE).size();
        assertEquals(sTest + "invokeAll(set) failed to update key index", cExpect, nCount);

        verifyEventStats(sTest, listEvents, verifier);
        verifyStoreStats(sTest, store, cLoad, cStore, cErase, cLoadAll, cStoreAll, cEraseAll);

        cache.clear();
        assertEquals(sTest + "clear failed", 0, cache.size());
        }

    /**
    * Internal helper.
    */
    private static void verifyEventStats(String sTest, List listEvents, EventVerifier verifier)
        {
        // sort events by key
        Map mapByKey = new HashMap();
        for (Iterator iter = listEvents.iterator(); iter.hasNext(); )
            {
            MapEvent event = (MapEvent) iter.next();
            Object   oKey  = event.getKey();
            List     list  = (List) mapByKey.get(oKey);
            if (list == null)
                {
                mapByKey.put(oKey, list = new LinkedList());
                }
            list.add(event);
            }

        for (Iterator iter = mapByKey.values().iterator(); iter.hasNext(); )
            {
            verifier.verifyEventsForKey(sTest, (List) iter.next());
            }
        }

    public static class EventVerifier
        {
        public EventVerifier(int[] anEvent)
            {
            m_anEvent = anEvent;
            }

        public void verifyEventsForKey(String sTest, List listEvents)
            {
            assertEquals(sTest + "invoke generated the wrong number of events", m_anEvent.length, listEvents.size());

            for (int i = 0; i < m_anEvent.length; i++)
                {
                MapEvent event = (MapEvent) listEvents.get(i);
                assertEquals(sTest + "invoke generated invalid event " + event,
                             m_anEvent[i], event.getId());
                }
            }

        protected int[] m_anEvent;
        }

    /**
    * Internal helper.
    */
    private static void verifyStoreStats(String sTest, AbstractTestStore store,
            int cLoad, int cStore, int cErase, int cLoadAll, int cStoreAll, int cEraseAll)
        {
        verifyStoreStats(sTest, store, "load", cLoad);
        verifyStoreStats(sTest, store, "loadAll", cLoadAll);
        verifyStoreStats(sTest, store, "store", cStore);
        verifyStoreStats(sTest, store, "storeAll", cStoreAll);
        verifyStoreStats(sTest, store, "erase", cErase);
        verifyStoreStats(sTest, store, "eraseAll", cEraseAll);
        }

    private static void verifyStoreStats(String sTest, AbstractTestStore store, Filter condition)
        {
        boolean fResult = condition.evaluate(store.getStatsMap());
        String sMessage = condition.toExpression() + " is " + String.valueOf(fResult).toUpperCase() + "; stats=" + store.getStatsMap();
        assertTrue(sMessage, fResult);
        System.out.println(sMessage);
        }

    private static void verifyStoreStats(String sTest, AbstractTestStore store, String sStatistic, int expected)
        {
        Integer count = store.getStatsMap().get(sStatistic);
        assertEquals(sTest + " " + sStatistic + " operations: expected=" + expected
                     + " actual=" + count + " otherStats=" + store.getStatsMap(),
                     expected, count == null ? 0 : count);
        }

    /**
    * test event type on update from entry processor.
    */
    @Test
    public void testNonSyntheticUpdatewithEP()
        {
        NamedCache     cache   = getNamedCache("dist-rwbm-coh10009");
        TestCacheStore store   = (TestCacheStore) getStore(cache);
        ObservableMap  map     = store.getStorageMap();
        List<String>   listMsg = new ArrayList<String>(2);

        map.put("somekey", "mydummy");

        cache.addMapListener(
            new MapListener()
                {
                public void entryInserted(MapEvent event)
                    {
                    if (!((CacheEvent)event).isSynthetic())
                        {
                        listMsg.add("Failed");
                        }
                    }
                public void entryUpdated(MapEvent event)
                    {
                    if (((CacheEvent)event).isSynthetic())
                        {
                        listMsg.add("Failed");
                        }
                    }
                public void entryDeleted(MapEvent event)
                    {
                    }
                }
            );
        cache.invoke("somekey", new CustomProcessor15());
        Eventually.assertThat(invoking(listMsg).isEmpty(), is(true));
        }

    /**
     * Test to make sure that erase method of Store is not called when
     * the expiry time is close to store delay.
     *
     * Regression test for Bug 26101585
     */
    @Test
    public void testExpiryOnWrite()
        {
        NamedCache          cache = getNamedCache("dist-rwbm-expiry-on-store");
        AbstractTestStore   store = getStore(cache);
        ReadWriteBackingMap rwbm  = getReadWriteBackingMap(cache);

        List<MapEvent> listNonSyntheticMsg = new ArrayList<MapEvent>();
        List<MapEvent> listDeleteMsg       = new ArrayList<MapEvent>();

        cache.addMapListener(
                new AbstractMapListener()
                    {
                    public void entryDeleted(MapEvent event)
                        {
                        // the delete must have been by expiry and hence non-synthetic
                        if (!((CacheEvent)event).isSynthetic())
                            {
                            listNonSyntheticMsg.add(event);
                            }
                        listDeleteMsg.add(event);
                        }
                    }
        );

        cache.put("key", "value");

        Eventually.assertThat(invoking(cache).size(), is(0));

        assertEquals("The erase method of the Store should not have been called on expiry."
                ,1, store.getStorageMap().size());

        // make sure that the event has been processed
        Eventually.assertThat(invoking(listDeleteMsg).size(), is(1));

        assertEquals("The erase method of the Store should not have resulted in a non synthetic event."
                ,0, listNonSyntheticMsg.size());
        cache.destroy();
        }

    /*
     * Test various cache operations with a cache store to make sure
     * read-through happens when the entries in the RWBM expired.
     */
    @Test
    public void testCacheStoreReadThrough()
        {
        NamedCache<String, Integer> cache = getNamedCache("dist-rwbm-ra");
        try
            {
            cache.put("1", 1);
            cache.put("2", 2);
            cache.put("3", 3);

            // wait for the entries to expire
            Eventually.assertThat(invoking(cache).size(), is(0));

            assertEquals(3, (int) cache.invoke("k", new GetStoreSizeProcessor()));

            // putIfAbsent
            assertEquals(1, (int) cache.putIfAbsent("1", 10));
            assertEquals(1, (int) cache.get("1"));
            assertNull(cache.putIfAbsent("10", 10));
            assertEquals(10, (int) cache.get("10"));

            // computeIfPresent
            assertEquals(2, (int) cache.computeIfPresent("1", (k, v) -> v + v));
            assertNull(cache.computeIfPresent("1", (k, v) -> null));
            assertFalse(cache.containsKey("1"));
            assertNull(cache.computeIfPresent("20", (k, v) -> v * v));
            assertFalse(cache.containsKey("20"));

            // replace
            cache.replace("2", 20);
            assertEquals(20, (int) cache.get("2"));

            // removeValue
            assertTrue(cache.invoke("3", new CacheProcessors.RemoveValue<>(3)));
            assertFalse(cache.containsKey("3"));

            // remove
            cache.remove("2");
            assertFalse(cache.containsKey("2"));
            }
        finally
            {
            cache.destroy();
            }
        }

    /*
     * Test a ReadOnly BinaryEntry will result in a read-through.
     */
    @Test
    public void testCacheStoreReadThroughWithReadOnlyEntry()
        {
        final String CACHE_RELATED = "dist-rwbm-ra-related";

        NamedCache<String, Integer> cache       = getNamedCache("dist-rwbm-ra");
        NamedCache<String, Integer> cacheJoined = getNamedCache(CACHE_RELATED);
        try
            {
            cache      .put("1", 1);
            cacheJoined.put("1", 1);
            cache      .put("2", 1);
            cacheJoined.put("2", 1);

            // confirm aggregation returns the correct result including entries that are not present
            Integer NResult = (Integer) cache.aggregate(new ImmutableArrayList(new Object[] {"1"}).getSet(),
                    new CustomAggregator(CACHE_RELATED, false));
            assertThat(NResult, Matchers.allOf(Matchers.notNullValue(), is(2)));

            // confirm aggregation returns the correct result only for entries that are present
            NResult = (Integer) cache.aggregate(new ImmutableArrayList(new Object[] {"2"}).getSet(),
                    new CustomAggregator(CACHE_RELATED, true));
            assertThat(NResult, Matchers.allOf(Matchers.notNullValue(), is(2)));

            // wait for the entries to expire
            Eventually.assertThat(invoking(cache).size(), is(0));
            Eventually.assertThat(invoking(cacheJoined).size(), is(0));

            // same aggregation call should cause a read through
            NResult = (Integer) cache.aggregate(new ImmutableArrayList(new Object[] {"1"}).getSet(),
                    new CustomAggregator(CACHE_RELATED, false));
            assertThat(NResult, Matchers.allOf(Matchers.notNullValue(), is(2)));

            // aggregation for only present entries should not cause a load
            NResult = (Integer) cache.aggregate(new ImmutableArrayList(new Object[] {"2"}).getSet(),
                    new CustomAggregator(CACHE_RELATED, true));
            assertThat(NResult, Matchers.anyOf(Matchers.nullValue(), is(0)));

            // force a load on the 'driving' cache to ensure getReadOnlyEntry
            // does not result in a load
            assertEquals(Integer.valueOf(1), cache.get("2"));

            NResult = (Integer) cache.aggregate(new ImmutableArrayList(new Object[] {"2"}).getSet(),
                    new CustomAggregator(CACHE_RELATED, true));
            assertThat(NResult, Matchers.allOf(Matchers.notNullValue(), is(1)));
            }
        finally
            {
            cache.destroy();
            cacheJoined.destroy();
            }
        }

    @Test
    public void testCacheStoreRemoveOnWriteBehind()
        {
        cacheStoreRemoveOnWriteBehind("dist-rwbm-wb-bin");
        cacheStoreRemoveOnWriteBehind("dist-rwbm-wb-bin-remove");
        }

    protected void cacheStoreRemoveOnWriteBehind(String sCacheName)
        {
        final String         testName    = "testCacheStoreRemove-" + sCacheName;
        final String         WB_REMOVE   = "remove";
        NamedCache           cache       = getNamedCache(sCacheName);
        TestBinaryCacheStore store       = (TestBinaryCacheStore) getStore(cache);
        ObservableMap        storeMap    = store.getStorageMap();
        ReadWriteBackingMap  rwbm        = getReadWriteBackingMap(cache);
        long                 cDelay      = rwbm.getWriteBehindMillis() + 100;
        LocalCache           mapInternal = (LocalCache) rwbm.getInternalCache();

        cache.clear();
        storeMap.clear();
        store.resetStats();

        try
            {

            cache.put(Integer.valueOf(0), Integer.valueOf(0));
            assertEquals(testName, 0, storeMap.size());
            assertEquals(testName, 1, mapInternal.size());

            Eventually.assertThat(invoking(storeMap).size(), is(1));

            cache.remove(Integer.valueOf(0));
            assertEquals(testName, false, cache.containsKey(Integer.valueOf(0)));
            assertEquals(testName, false, cache.keySet().contains(Integer.valueOf(0)));
            if (sCacheName.contains(WB_REMOVE))
                {
                assertEquals(testName, 1, mapInternal.size());
                }
            else
                {
                assertEquals(testName, 0, mapInternal.size());
                }
            assertEquals(cache.get(Integer.valueOf(0)), null);
            assertEquals(cache.size(), 0);
            if (sCacheName.contains(WB_REMOVE))
                {
                assertEquals(testName, 1, storeMap.size());
                Eventually.assertThat(invoking(storeMap).size(), is(0));
                }
            else
                {
                assertEquals(testName, 0, storeMap.size());
                }

            Map<Integer,Integer> mapData = mapOfIntegers(10);

            updateCache(cache, mapData, false);
            assertEquals(testName, 0, storeMap.size());
            assertEquals(testName, mapData.size(), mapInternal.size());
            Eventually.assertThat(invoking(cache).size(), is(mapData.size()));
            Eventually.assertThat(invoking(storeMap).size(), is(mapData.size()));

            cache.clear();
            assertEquals(testName, 0, cache.aggregate(AlwaysFilter.INSTANCE, new Count()));
            assertEquals(0, (cache.invokeAll(AlwaysFilter.INSTANCE, new GetProcessor())).size());
            if (sCacheName.contains(WB_REMOVE))
                {
                assertEquals(testName, 10, storeMap.size());
                assertEquals(testName, 10, mapInternal.size());
                }
            else
                {
                assertEquals(testName, 0, storeMap.size());
                assertEquals(testName, 0, mapInternal.size());
                }
            assertEquals(testName, 0, cache.size());
            Eventually.assertThat(invoking(storeMap).size(), is(0));

            // test with index

            Map mapBatch = new HashMap();
            for (int i = 0; i < 10; i++)
                {
                mapBatch.put("key" + i, + i);
                }

            cache.addIndex(IdentityExtractor.INSTANCE(), true, null);
            cache.putAll(mapBatch);
            Eventually.assertThat(invoking(storeMap).size(), is(10));
            cache.remove("key1");

            assertEquals(cache.aggregate(new LessEqualsFilter(IdentityExtractor.INSTANCE, 5), new Count<>()), 5);

            int nSize = (Integer) cache.aggregate(AlwaysFilter.INSTANCE, new Count<>());
            assertEquals(cache.size(), nSize);

            cache.removeIndex(IdentityExtractor.INSTANCE());
            cache.clear();
            mapBatch.clear();
            Eventually.assertThat(invoking(storeMap).size(), is(0));

            // Test exception handling

            String sKey   = "Key0";
            String sValue = "Value0";
            cache.put(sKey, sValue);
            Eventually.assertThat(invoking(storeMap).size(), is(1));

            store.setFailureKeyErase(sKey);
            try
                {
                cache.remove(sKey);
                if (!sCacheName.contains(WB_REMOVE))
                    {
                    fail("Didn't get Exception on remove!");
                    }
                }
            catch (Exception e)
                {
                if (sCacheName.contains(WB_REMOVE))
                    {
                    fail("Should not get Exception on remove!");
                    }
                }

            definiteSleep(cDelay);
            if (sCacheName.contains(WB_REMOVE))
                {
                assertFalse("remove should succeed", cache.containsKey(sKey));
                }
            else
                {
                assertTrue("remove did not fail as expected", cache.containsKey(sKey));
                }
            assertTrue("remove did not fail as expected", 1 == storeMap.size());

            store.setFailureKeyErase(null);
            if (!sCacheName.contains(WB_REMOVE))
                {
                cache.remove(sKey);
                assertEquals(testName, 0, cache.size());
                assertEquals(testName, 0, storeMap.size());
                }
            else
                {
                assertEquals(testName, 0, cache.size());
                Eventually.assertThat(invoking(storeMap).size(), is(0));
                }

            if (sCacheName.contains(WB_REMOVE))
                {
                store.setFailureKeyEraseAll(sKey);
                }
            else
                {
                store.setFailureKeyErase(sKey);
                }

            for (int i = 0; i < 10; i++)
                {
                mapBatch.put("Key" + i, "Value" + i);
                }
            cache.putAll(mapBatch);
            definiteSleep(cDelay);

            if (cDelay > 0)
                {
                definiteSleep(cDelay + 100);
                }
            assertEquals(testName, mapBatch.size(), storeMap.size());

            try
                {
                cache.clear();
                if (!sCacheName.contains(WB_REMOVE))
                    {
                    fail("Didn't get Exception on clear!");
                    }
                }
            catch (Exception e)
                {
                if (sCacheName.contains(WB_REMOVE))
                    {
                    fail("Should not get Exception on clear!");
                    }
                }

            definiteSleep(cDelay + 100);
            assertTrue("eraseAll did not fail as expected!", storeMap.size() > 0 && storeMap.size() <= 10);
            if (sCacheName.contains(WB_REMOVE))
                {
                assertEquals("eraseAll failed!", 0, cache.size());
                }
            else
                {
                assertTrue("eraseAll did not fail as expected!", cache.size() > 0 && cache.size() <= 10);
                }

            if (sCacheName.contains(WB_REMOVE))
                {
                store.setFailureKeyEraseAll(null);
                Eventually.assertThat(invoking(storeMap).size(), is(0));
                }
            else
                {
                store.setFailureKeyErase(null);
                cache.clear();
                assertEquals(testName, 0, storeMap.size());
                }

            definiteSleep(cDelay + 100);

            // test with interceptor

            EventDispatcherAwareInterceptor incptrEntry = new EventDispatcherAwareInterceptor<EntryEvent<?, ?>>()
                {
                public void introduceEventDispatcher(String sIdentifier, EventDispatcher dispatcher)
                    {
                    dispatcher.addEventInterceptor(sIdentifier, this, new ImmutableArrayList(EntryEvent.Type.values()).getSet(), false);
                    }

                public void onEvent(EntryEvent event)
                    {
                    System.out.println("Received event: " + event);
                    if (event.getType() == EntryEvent.Type.REMOVING)
                        {
                        System.out.println("Incremented m_nRemovingCounter; " + Base.printStackTrace(new Exception()));
                        m_nRemovingCounter++;
                        }
                    else if (event.getType() == EntryEvent.Type.REMOVED)
                        {
                        System.out.println("Incremented m_nRemovedCounter; " + Base.printStackTrace(new Exception()));
                        m_nRemovedCounter++;
                        }
                    }
                };

            m_nRemovingCounter = 0;
            m_nRemovedCounter  = 0;
            InterceptorRegistry registry       = cache.getCacheService().getBackingMapManager()
                    .getCacheFactory().getInterceptorRegistry();
            final String        RB_REMOVE_TEST = "RBRemoveTest";
            registry.registerEventInterceptor(RB_REMOVE_TEST, incptrEntry, RegistrationBehavior.FAIL);

            try
                {
                cache.put(1, 1);
                Eventually.assertThat(invoking(storeMap).size(), is(1));
                cache.remove(1);
                Eventually.assertThat(invoking(storeMap).size(), is(0));

                assertEquals(1, m_nRemovingCounter);
                definiteSleep(cDelay);
                Assert.assertThat(m_nRemovedCounter, is(greaterThan(0)));
                }
            finally
                {
                registry.unregisterEventInterceptor(RB_REMOVE_TEST);
                }
            }
        finally
            {
            cache.destroy();
            }
        }

    // ----- helper methods -------------------------------------------------

    /**
    * Update the specified cache with the values in the specified map.
    * If the fUsePutAll flag is true then a putAll() call will be used to update
    * the cache otherwise multiple put() calls will be used.
    *
    * @param cache
    * @param mapData
    * @param fUsePutAll
    */
    protected void updateCache(NamedCache cache, Map<?,?> mapData, boolean fUsePutAll)
        {
        if (fUsePutAll)
            {
            cache.putAll(mapData);
            }
        else
            {
            for (Map.Entry entry : mapData.entrySet())
                {
                cache.put(entry.getKey(), entry.getValue());
                }
            }
        }

    protected Map<Integer,Integer> mapOfIntegers(int size)
        {
        return mapOfIntegers(size, 0, 0);
        }

    protected Map<Integer,Integer> mapOfIntegers(int size, int firstKey, int firstValue)
        {
        Map<Integer,Integer> map = new HashMap<>();
        for (int i=0; i<size; i++)
            {
            map.put(firstKey + i, firstValue + i);
            }
        return map;
        }

    protected Map<Integer,Person> mapOfPeople(int size)
        {
        Map<Integer,Person> map = new HashMap<>();
        for (int i = 0; i < size; i++)
            {
            map.put(i, new Person(String.valueOf(i)));
            }
        return map;
        }

    /**
    * Return the ReadWriteBackingMap for the given NamedCache.
    *
    * @param cache  the NamedCache
    *
    * @throws IllegalArgumentException if a ReadWriteBackingMap could not be
    *         be found
    */
    protected ReadWriteBackingMap getReadWriteBackingMap(NamedCache cache)
        {
        Map map = TestHelper.getBackingMap(cache);
        if (map instanceof ReadWriteBackingMap)
            {
            return (ReadWriteBackingMap) map;
            }

        throw new IllegalArgumentException();
        }

    /**
    * Return the AbstractTestStore for the given NamedCache.
    *
    * @param cache  the NamedCache
    *
    * @throws IllegalArgumentException if a CacheStore could not be found
    */
    protected AbstractTestStore getStore(NamedCache cache)
        {
        ReadWriteBackingMap.StoreWrapper wrapper =
                getReadWriteBackingMap(cache).getCacheStore();

        return (AbstractTestStore) wrapper.getStore();
        }

    public boolean isThreadAlive(Thread thread)
        {
        return thread.isAlive();
        }

    /**
     * Helper method for testInterceptor.
     *
     * @return the received event list
     */
    public List<EntryEvent.Type> getEventList()
        {
        return CacheEventInterceptor.getEventList();
        }

    protected int getRemovedCount()
        {
        return m_nRemovedCounter;
        }


    // ----- inner classes ---------------------------------------------------

    public abstract static class CustomProcessor
            extends AbstractProcessor
        {
        protected EvictingRWBM getEvictingRWBM(InvocableMap.Entry entry, String sCache)
            {
            BinaryEntry binEntry = (BinaryEntry) entry;
            return (EvictingRWBM) binEntry.getContext().getBackingMap(sCache);
            }

        protected LocalCache getInternalCache(InvocableMap.Entry entry, String sCache)
            {
            BinaryEntry binEntry = (BinaryEntry) entry;
            return (LocalCache) getEvictingRWBM(binEntry, sCache).getInternalCache();
            }
        }

    public static class CustomProcessor1
            extends CustomProcessor
        {
        public CustomProcessor1(String sCacheName)
            {
            m_sCacheName = sCacheName;
            }

        public Object process(InvocableMap.Entry entry)
            {
            azzert(!entry.isPresent());
            entry.getValue(); // preload

            BinaryEntry binEntry = (BinaryEntry) entry;
            getInternalCache(binEntry, m_sCacheName).evict(binEntry.getBinaryKey()); // evict
            return null;
            }

        String m_sCacheName;
        }

    public static class CustomProcessor2
        extends CustomProcessor
        {
        public CustomProcessor2(String sCacheName)
            {
            m_sCacheName = sCacheName;
            }

        public Object process(InvocableMap.Entry entry)
            {
            azzert(!entry.isPresent());
            Integer IValue = (Integer) entry.getValue(); // preload

             // simulation of the evict during put
            getEvictingRWBM(entry, m_sCacheName).forceEvict(entry, true);

            entry.setValue(IValue + 1);
            return entry.getValue();
            }

        String m_sCacheName;
        }

    public static class CustomProcessor3
        extends CustomProcessor
        {
        public CustomProcessor3(String sCacheName)
            {
            m_sCacheName = sCacheName;
            }

        public Object process(InvocableMap.Entry entry)
            {
            azzert(!entry.isPresent());
            Integer IValue = (Integer) entry.getValue(); // preload

             // simulate an eviction during remove
            getEvictingRWBM(entry, m_sCacheName).forceEvict(entry, true);

            entry.remove(false);
            return IValue;
            }

        String m_sCacheName;
        }

    public static class CustomProcessor4
        extends CustomProcessor
        {
        public CustomProcessor4(String sCacheName)
            {
            m_sCacheName = sCacheName;
            }

        public Object process(InvocableMap.Entry entry)
            {
            // simulate an eviction during containsKey()
            getEvictingRWBM(entry, m_sCacheName).forceEvict(entry, true);

            azzert(!entry.isPresent());
            return entry.getValue(); // evict-preload
            }

        String m_sCacheName;
        }

    public static class CustomProcessor5
        extends CustomProcessor
        {
        public CustomProcessor5(String sCacheName)
            {
            m_sCacheName = sCacheName;
            }

        public Object process(InvocableMap.Entry entry)
            {
            // simulate an eviction during containsKey()
            getEvictingRWBM(entry, m_sCacheName).forceEvict(entry, true);

            azzert(!entry.isPresent());
            return null;
            }

        String m_sCacheName;
        }

    public static class CustomProcessor6
        extends CustomProcessor
        {
        public CustomProcessor6(String sCacheName)
            {
            m_sCacheName = sCacheName;
            }

        public Object process(InvocableMap.Entry entry)
            {
            azzert(!entry.isPresent());
            Integer IValue = (Integer) entry.getValue(); // preload

             // simulation of the evict during put
            getEvictingRWBM(entry, m_sCacheName).forceEvict(entry, false);

            entry.setValue(IValue.intValue() + 1);
            return entry.getValue();
            }

        String m_sCacheName;
        }

    public static class CustomProcessor7
        extends CustomProcessor
        {
        public CustomProcessor7(String sCacheName)
            {
            m_sCacheName = sCacheName;
            }

        public Object process(InvocableMap.Entry entry)
            {
            azzert(!entry.isPresent());
            entry.remove(false);
            return null;
            }

        String m_sCacheName;
        }

    public static class CustomProcessor8
        extends CustomProcessor
        {
        public CustomProcessor8(String sCacheName)
            {
            m_sCacheName = sCacheName;
            }

        public Object process(InvocableMap.Entry entry)
            {
            azzert(entry.isPresent());
            Integer IValue = (Integer) entry.getValue();

            // simulate an eviction during update
            getEvictingRWBM(entry, m_sCacheName).forceEvict(entry, true);

            entry.setValue(IValue.intValue() + 1);
            return IValue;
            }

        String m_sCacheName;
        }

    public static class CustomProcessor9
        extends CustomProcessor
        {
        public CustomProcessor9(String sCacheName)
            {
            m_sCacheName = sCacheName;
            }

        public Object process(InvocableMap.Entry entry)
            {
            azzert(entry.isPresent());
            Integer IValue = (Integer) entry.getValue();

            // an eviction during remove (should not change the behavior)
            getEvictingRWBM(entry, m_sCacheName).forceEvict(entry, true);

            entry.remove(false);
            return IValue;
            }

        String m_sCacheName;
        }

    public static class CustomProcessor10
        extends CustomProcessor
        {
        public CustomProcessor10(String sCacheName)
            {
            m_sCacheName = sCacheName;
            }

        public Object process(InvocableMap.Entry entry)
            {
            azzert(entry.isPresent());
            Integer IValue = (Integer) entry.getValue();

            // simulate an eviction during update
            getEvictingRWBM(entry, m_sCacheName).forceEvict(entry, false);

            entry.setValue(IValue.intValue() + 1);
            return IValue;
            }

        String m_sCacheName;
        }

    public static class CustomProcessor11
        extends CustomProcessor
        {
        public CustomProcessor11() {}

        public Object process(InvocableMap.Entry entry)
            {
            entry.remove(true);
            return null;
            }
        }

    public static class CustomProcessor12
        extends CustomProcessor
        {
        public CustomProcessor12() {}

        public Object process(InvocableMap.Entry entry)
            {
            Integer I = (Integer) entry.getValue();
            entry.setValue(-1 * I, /*fSynthetic*/ true);
            return null;
            }
        }

    public static class CustomProcessor13
        extends CustomProcessor
        {
        public CustomProcessor13() {}

        public Object process(InvocableMap.Entry entry)
            {
            BinaryEntry binEntry = (BinaryEntry) entry;
            Integer I = ((Integer) binEntry.getValue()) * -1;

            binEntry.updateBinaryValue((Binary) binEntry.getBackingMapContext().
                    getManagerContext().getValueToInternalConverter().convert(I), /*fSynthetic*/ true);
            return null;
            }
        }

    public static class CustomProcessor14
            extends CustomProcessor
        {
        public CustomProcessor14(long cExpire)
            {
            m_cExpire = cExpire;
            }

        public Object process(Entry entry)
            {
            ((BinaryEntry) entry).expire(m_cExpire);
            return null;
            }

        long m_cExpire;
        }

    public static class CustomProcessor15
            extends AbstractProcessor
        {
        public Object process(Entry entry)
            {
            entry.getValue();
            entry.setValue("someValue", false);
            return null;
            }
        }

    /**
     * Listener class to test if an entry has been inserted.
     */
    public static class Listener
            extends MultiplexingMapListener
        {
        static boolean wasInserted()
            {
            return m_fInserted;
            }

        static void clearInserted()
            {
            m_fInserted = false;
            }

        protected void onMapEvent(MapEvent evt)
            {
            m_fInserted =  (evt.getId() == MapEvent.ENTRY_INSERTED);
            }

        static private volatile boolean m_fInserted;
        }

    /**
     * Interceptor class to test if an entry event is received.
=     */
    @Interceptor(identifier = "CacheEventInterceptor")
    @EntryEvents({EntryEvent.Type.INSERTING, EntryEvent.Type.INSERTED, EntryEvent.Type.UPDATING, EntryEvent.Type.UPDATED})
    public static class CacheEventInterceptor
            implements EventInterceptor<EntryEvent<?, ?>>, Serializable
        {
        static List<EntryEvent.Type> getEventList()
            {
            return m_listEventType;
            }

        static void clearEvent()
            {
            m_listEventType.clear();
            }

        @Override
        public void onEvent(EntryEvent<?, ?> evt)
            {
            Base.log("Receive event: " + evt);
            m_listEventType.add((EntryEvent.Type) evt.getType());
            }

        static private volatile List<EntryEvent.Type> m_listEventType = Collections.synchronizedList(new ArrayList<>());
        }

    // ----- EntryProcessors ------------------------------------------------

    /**
     * An EntryProcessor implementation to get the current size of the
     * AbstractCacheStore being used.
     */
    public static class GetStoreSizeProcessor
            implements InvocableMap.EntryProcessor<String, Integer, Integer>
        {
        public GetStoreSizeProcessor()
            {
            }

        // ----- InvocableMap.EntryProcessor --------------------------------------

        @Override
        public Integer process(InvocableMap.Entry<String, Integer> entry)
            {
            ReadWriteBackingMap backingMap =
                    (ReadWriteBackingMap) ((BinaryEntry) entry).getBackingMapContext().getBackingMap();
            return ((AbstractTestStore) backingMap.getCacheStore().getStore()).getStorageMap().size();
            }
        }

    // ----- inner class: CustomAggregator ----------------------------------

    public static class CustomAggregator
            implements StreamingAggregator
        {
        // ----- constructors -----------------------------------------------

        public CustomAggregator() {}

        public CustomAggregator(String sCacheName, boolean fPresentOnly)
            {
            m_sCacheName       = sCacheName;
            m_nCharacteristics = PARALLEL | (fPresentOnly ? PRESENT_ONLY : 0);
            }

        @Override
        public StreamingAggregator supply()
            {
            return new CustomAggregator();
            }

        @Override
        public boolean accumulate(Entry entry)
            {
            BinaryEntry binEntryThis = (BinaryEntry) entry;
            BinaryEntry binEntryThat = (BinaryEntry) binEntryThis.getContext().getBackingMapContext(m_sCacheName).
                    getReadOnlyEntry(binEntryThis.getBinaryKey());

            Integer NValThis = (Integer) binEntryThis.getValue();
            Integer NValThat = (Integer) (binEntryThat == null ? null : binEntryThat.getValue());

            m_nValue += (NValThis == null ? 0 : NValThis.intValue()) +
                    (NValThat == null ? 0 : NValThat.intValue());

            return true;
            }

        @Override
        public boolean combine(Object oPartial)
            {
            if (oPartial instanceof Integer)
                {
                m_nValue += ((Integer) oPartial).intValue();
                }
            return true;
            }

        @Override
        public Object getPartialResult()
            {
            return m_nValue;
            }

        @Override
        public Object finalizeResult()
            {
            return m_nValue;
            }

        @Override
        public int characteristics()
            {
            return m_nCharacteristics;
            }

        // ----- data members -----------------------------------------------

        protected int    m_nValue;
        protected String m_sCacheName;
        protected int    m_nCharacteristics;
        }

    // ----- static helpers -------------------------------------------------

    /**
    * Ensure the current thread sleeps for at least {@code cSleep} millis.
    *
    * @param cSleep  the number of milliseconds to sleep
    */
    protected static void definiteSleep(long cSleep)
        {
        while (cSleep > 0)
            {
            long lStart = System.currentTimeMillis();
            Base.sleep(cSleep);
            long lDelta = System.currentTimeMillis() - lStart;
            if (lDelta > 0)
                {
                cSleep -= lDelta;
                }
            // else clock jumped backwards; it will eventually go forward
            }
        }

    /**
    * Unit test.
    */
    public static void main(String[] asArg)
        {
        new ReadWriteBackingMapTests().testCacheStoreRemoveOnWriteThroughPut();
        new ReadWriteBackingMapTests().testCacheStoreRemoveOnWriteThroughPutAll();
        new ReadWriteBackingMapTests().testCacheStoreRemoveOnWriteBehindPut();
        new ReadWriteBackingMapTests().testCacheStoreRemoveOnWriteBehindPutAll();
        }


    // ----- constants ------------------------------------------------------

    /**
    * The file name of the default cache configuration file used by this test.
    */
    public static String FILE_CFG_CACHE = "rwbm-cache-config.xml";

    // ----- data members ---------------------------------------------------

    /**
     * A EntryEvent.Type.REMOVING counter for interceptor test.
     *
     * @since 12.2.1.4.18
     */
    private static volatile int m_nRemovingCounter;

    /**
     * A EntryEvent.Type.REMOVED counter for interceptor test.
     *
     * @since 12.2.1.4.18
     */
    private static volatile int m_nRemovedCounter;
    }
