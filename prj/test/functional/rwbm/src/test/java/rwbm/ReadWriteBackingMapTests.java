/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package rwbm;


import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.tangosol.coherence.config.CustomClasses;
import com.tangosol.internal.util.processor.CacheProcessors;
import com.tangosol.net.BackingMapManagerContext;
import com.tangosol.net.NamedCache;

import com.tangosol.net.cache.CacheEvent;
import com.tangosol.net.cache.ConfigurableCacheMap;
import com.tangosol.net.cache.LocalCache;
import com.tangosol.net.cache.ReadWriteBackingMap;

import com.tangosol.util.AbstractMapListener;
import com.tangosol.util.Base;
import com.tangosol.util.Binary;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.ClassHelper;
import com.tangosol.util.Converter;
import com.tangosol.util.Daemon;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.ImmutableArrayList;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.InvocableMap.Entry;
import com.tangosol.util.InvocableMap.StreamingAggregator;
import com.tangosol.util.ListMap;
import com.tangosol.util.MapEvent;
import com.tangosol.util.MapListener;
import com.tangosol.util.MapListenerSupport;
import com.tangosol.util.MultiplexingMapListener;
import com.tangosol.util.NullImplementation;
import com.tangosol.util.ObservableMap;
import com.tangosol.util.SafeHashSet;

import com.tangosol.util.aggregator.Count;

import com.tangosol.util.extractor.PofUpdater;

import com.tangosol.util.filter.AlwaysFilter;
import com.tangosol.util.filter.PresentFilter;

import com.tangosol.util.processor.AbstractProcessor;
import com.tangosol.util.processor.CompositeProcessor;
import com.tangosol.util.processor.ConditionalRemove;
import com.tangosol.util.processor.NumberIncrementor;
import com.tangosol.util.processor.PreloadRequest;
import com.tangosol.util.processor.PropertyManipulator;

import common.AbstractFunctionalTest;
import common.AbstractTestStore;
import common.TestBinaryCacheStore;
import common.TestBinaryCacheStore.ExpireProcessor;
import common.TestCacheStore;
import common.TestHelper;


import data.Person;

import org.hamcrest.Matchers;

import org.junit.BeforeClass;
import org.junit.Test;

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

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static org.hamcrest.CoreMatchers.both;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;


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
        System.setProperty("tangosol.coherence.distributed.localstorage", "true");
        System.setProperty("tangosol.coherence.rwbm.requeue.delay", "5000");

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
        NamedCache          cache   = getNamedCache(sCacheName);
        ReadWriteBackingMap rwbm    = getReadWriteBackingMap(cache);
        AbstractTestStore   store   = getStore(cache);
        ObservableMap       map     = store.getStorageMap();
        long                cMillis = rwbm.getWriteBehindSeconds() * 1000L;

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
            // Sleep so we should be in-between store calls
            rwbm.flush();
            definiteSleep(cMillis / 2);

            cache.put("Key4", "Value4");
            cache.put("Key5", "Value5");

            // wait until the write-behind delay has elapsed
            definiteSleep(cMillis * 2);

            assertThat(map.size(), is(2));
            verifyStoreStats("putWithWriteBatchFactorOne-" + sCacheName, store, 0, 0, 0, 0, 1, 0);
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
        NamedCache          cache   = getNamedCache(sCacheName);
        ReadWriteBackingMap rwbm    = getReadWriteBackingMap(cache);
        AbstractTestStore   store   = getStore(cache);
        ObservableMap       map     = store.getStorageMap();
        long                cMillis = rwbm.getWriteBehindSeconds() * 1000L;

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

            // make sure the last write doesn't occur until a full write-
            // behind delay interval
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
        NamedCache          cache   = getNamedCache(sCacheName);
        ReadWriteBackingMap rwbm    = getReadWriteBackingMap(cache);
        AbstractTestStore   store   = getStore(cache);
        ObservableMap       map     = store.getStorageMap();
        long                cMillis = rwbm.getWriteBehindSeconds() * 1000L;

        // assert RWBM configuration
        assertTrue("write-batch-factor != 0.0", rwbm.getWriteBatchFactor() == 0.0);
        assertTrue("write-behind-sec <= 0", cMillis > 0);
        rwbm.flush();

        try
            {
            map.clear();
            cache.put("Key1", "Value1");
            definiteSleep(20L);
            assertEquals("put() caused an immediate store.", map.size(), 0);

            // wait until the write-behind delay has elapsed
            definiteSleep(cMillis + 20L);
            assertEquals("write did not occur.", map.size(), 1);

            // verify the store store() method was called once
            verifyStoreStats("readThroughBasic-" + sCacheName, store, 0, 1, 0, 0, 0, 0);
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
        }

    private void writeBehindWithLongStore(String sCacheName)
        {
        NamedCache          cache   = getNamedCache(sCacheName);
        ReadWriteBackingMap rwbm    = getReadWriteBackingMap(cache);
        AbstractTestStore   store   = getStore(cache);
        ObservableMap       map     = store.getStorageMap();
        long                cMillis = rwbm.getWriteBehindSeconds() * 1000L;
        Object              sValue;

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
            assertEquals("Failed to clear", 0, map.size());

            int cEntries = rwbm.getWriteMaxBatchSize()*3;
            Map mapTemp  = new HashMap();
            for (int i = 0; i < cEntries; i++)
                {
                mapTemp.put(i, i);
                }
            cache.putAll(mapTemp);
            definiteSleep(1500);
            assertEquals("Failed to store all entries", cEntries, map.size());
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
        NamedCache          cache   = getNamedCache(sCacheName);
        ReadWriteBackingMap rwbm    = getReadWriteBackingMap(cache);
        AbstractTestStore   store   = getStore(cache);
        ObservableMap       map     = store.getStorageMap();
        long                cMillis = rwbm.getWriteBehindSeconds() * 1000L;

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
            for (int i = 0 ; i < 200; i++)
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
            ctxNull, new LocalCache(10), null, new TestCacheStore(), false, 0, 0.0);

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
                        Integer.valueOf(random.nextInt(100000)));
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
    * Test the behavior of the ReadWriteBackingMap/WriteBehind
    * when calling release.
    */
    @Test
    public void testWriteBehindFlush()
        {
        testWriteBehindFlush("dist-rwbm-wb");
        testWriteBehindFlush("dist-rwbm-wb-bin");
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
            new NumberIncrementor((PropertyManipulator) null, Integer.valueOf(1), false)
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
                            10, 0, 10, 0, 0, 0, 0);

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
                            10, 0, 10, 0, 0, 0, 0);

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
                            0, 0, 10, 0, 0, 0, 0);

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
                            0, 0, 10, 0, 0, 0, 10);

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

        assertEquals("testRemoveSynthetic-" + sCacheName, 10, store.getStorageMap().size());
        assertEquals("testRemoveSynthetic-" + sCacheName, 10, mapInternal.size());

        store.getStatsMap().clear();

        cache.invokeAll(map.keySet(), new CustomProcessor11());

        assertEquals("testRemoveSynthetic-" + sCacheName, 10, store.getStorageMap().size());
        assertEquals("testRemoveSynthetic-" + sCacheName, 0, mapInternal.size());
        // Verify no interactions with CacheStore
        verifyStoreStats("testRemoveSynthetic-" + sCacheName, store, 0, 0, 0, 0, 0, 0);

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

        assertEquals("testUpdateSynthetic-" + sCacheName, 10, store.getStorageMap().size());
        assertEquals("testUpdateSynthetic-" + sCacheName, 10, mapInternal.size());

        store.getStatsMap().clear();

        cache.invokeAll(map.keySet(), new CustomProcessor12());

        assertEquals("testUpdateSynthetic-" + sCacheName, 10, store.getStorageMap().size());
        assertEquals("testUpdateSynthetic-" + sCacheName, 10, mapInternal.size());

        for (int i = 0; i < 10; i++)
            {
            assertEquals(-i, cache.get(i));
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

        assertEquals("testUpdateBinarySynthetic-" + sCacheName, 10, store.getStorageMap().size());
        assertEquals("testUpdateBinarySynthetic-" + sCacheName, 10, mapInternal.size());

        store.getStatsMap().clear();

        cache.invokeAll(map.keySet(), new CustomProcessor13());

        assertEquals("testUpdateBinarySynthetic-" + sCacheName, 10, store.getStorageMap().size());
        assertEquals("testUpdateBinarySynthetic-" + sCacheName, 10, mapInternal.size());

        for (int i = 0; i < 10; i++)
            {
            assertEquals(-i, cache.get(i));
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
        }

    /**
    * Test the behaviour of putAll() when write-behind binary store removes the entry.
    */
    @Test
    public void testCacheStoreRemoveOnWriteBehindPutAll()
        {
        testCacheStoreRemove("dist-rwbm-wb-bin", true);
        }

    private void testCacheStoreRemove(String sCacheName, boolean fUsePutAll)
        {
        String               testName    = "testCacheStoreRemove-" + sCacheName + (fUsePutAll ? "-PutAll" : "Put");
        NamedCache           cache       = getNamedCache(sCacheName);
        TestBinaryCacheStore store       = (TestBinaryCacheStore) getStore(cache);
        ReadWriteBackingMap  rwbm        = getReadWriteBackingMap(cache);
        long                 cDelay      = rwbm.isWriteBehind() ?  rwbm.getWriteBehindMillis() + 500 : 0;
        LocalCache           mapInternal = (LocalCache) rwbm.getInternalCache();

        cache.clear();
        store.getStorageMap().clear();
        store.resetStats();

        store.setProcessor(TestBinaryCacheStore.REMOVING_PROCESSOR);
        try
            {
            cache.put(Integer.valueOf(0), Integer.valueOf(0));

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
        }

    /**
    * Test the behaviour of putAll() when write-behind binary store reverts the entry.
    */
    @Test
    public void testCacheStoreRevertOnPutAllWithWriteBehind()
        {
        testCacheStoreRevert("dist-rwbm-wb-bin", true);
        }

    private void testCacheStoreRevert(String sCacheName, boolean fUsePutAll)
        {
        String               testName = "testCacheStoreRevert-" + sCacheName + (fUsePutAll ? "-PutAll" : "Put");
        NamedCache           cache    = getNamedCache(sCacheName);
        TestBinaryCacheStore store    = (TestBinaryCacheStore) getStore(cache);
        ReadWriteBackingMap  rwbm     = getReadWriteBackingMap(cache);
        long                 cDelay   = rwbm.isWriteBehind() ? rwbm.getWriteBehindMillis() + 500 : 0;

        cache.clear();
        store.getStorageMap().clear();

        Map<Integer,Integer> mapInitial = mapOfIntegers(10);
        updateCache(cache, mapInitial, fUsePutAll);

        if (cDelay > 0)
            {
            // have the store operation to go through
            definiteSleep(cDelay);
            }

        store.setProcessor(TestBinaryCacheStore.REVERTING_PROCESSOR);
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
    * write-through and write-behind using put() and putAll() to update the cache.
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

    private void testCacheStoreUpdate(String sCacheName, boolean fUsePutAll)
        {
        String               testName = "testCacheStoreUpdate-" + sCacheName + (fUsePutAll ? "-PutAll" : "Put");
        NamedCache           cache    = getNamedCache(sCacheName);
        TestBinaryCacheStore store    = (TestBinaryCacheStore) getStore(cache);
        ReadWriteBackingMap  rwbm     = getReadWriteBackingMap(cache);
        long                 cDelay   = rwbm.isWriteBehind() ? rwbm.getWriteBehindMillis() + 500 : 0;
        Converter            convDown = cache.getCacheService()
                                                .getBackingMapManager().getContext().getKeyToInternalConverter();

        cache.clear();
        store.getStorageMap().clear();
        store.resetStats();

        store.setProcessor(new AbstractProcessor()
            {
            public Object process(Entry entry)
                {
                entry.update(new PofUpdater(Person.MOTHER_SSN), "STORED");

                return null;
                }
            });

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
                    boolean hasDeco = ExternalizableHelper.isDecorated((Binary)
                                                rwbm.getInternalCache().get(convDown.convert(i)),
                                                ExternalizableHelper.DECO_STORE);
                    assertFalse(testName + " should not have DECO_STORE:", hasDeco);
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
         assertEquals(1, store.getStatsMap().get("loadAll"));
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
           assertEquals(1, store.getStatsMap().get("loadAll"));
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
        }

    /**
    * Test the behavior of expiry set by a write-through CacheStore on a put()
    */
    @Test
    public void testCacheStoreExpireOnPutAllWithWriteBehind()
        {
        testCacheStoreExpire("dist-rwbm-wb-bin", 1000, true);
        }

    private void testCacheStoreExpire(String sCacheName, long cExpiryMillis, boolean fUsePutAll)
        {
        String               testName    = "testCacheStoreUpdate-" + sCacheName + (fUsePutAll ? "-PutAll" : "Put");
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
            assertEquals(testName, 10, mapInternal.size());

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
        NamedCache           cache         = getNamedCache("dist-rwbm-wb-bin");
        TestBinaryCacheStore store         = (TestBinaryCacheStore) getStore(cache);
        ReadWriteBackingMap  rwbm          = getReadWriteBackingMap(cache);
        LocalCache           mapInternal   = (LocalCache) rwbm.getInternalCache();
        long                 cExpiryMillis = 2000L;
        long                 cDelay        = 1000L ; // RWBM.ACCELERATE_MIN

        cache.clear();
        store.getStorageMap().clear();

        for (int i = 0; i < 10; i++)
            {
            cache.put(i, Integer.valueOf(i), cExpiryMillis);
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
                cache.put(i, Integer.valueOf(10 + i), cExpiryMillis);
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
        NamedCache           cache = getNamedCache("dist-rwbm-wb");
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
    * Verifies that a put followed by remove of the key
    * does not lose the remove when the store of the put
    * value is delayed (COH-6033).
    */
    @Test
    public void removeAfterDelayedPut()
        {
        NamedCache cache = getNamedCache("dist-rwbm-wb");
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
        assertNull(result);

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

        cache.invoke(Integer.valueOf(0), processor);
        assertEquals(sTest + "invoke failed to load", cExpect, cache.size());

        ICount = (Integer) cache.aggregate(AlwaysFilter.INSTANCE, new Count());
        assertEquals(sTest + "invoke failed to update key index", cExpect, ICount.intValue());

        nCount = cache.keySet(AlwaysFilter.INSTANCE).size();
        assertEquals(sTest + "invoke failed to update key index", cExpect, nCount);

        assertTrue(sTest + " missing entry 0", cache.keySet().contains(Integer.valueOf(0)));

        cExpect = Math.max(2, cInitSize);
        setKeys = Collections.singleton(Integer.valueOf(1));
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
        cache.invoke(Integer.valueOf(0), processor);

        assertEquals(sTest + "invoke failed", cExpect, cache.size());

        ICount = (Integer) cache.aggregate(AlwaysFilter.INSTANCE, new Count());
        assertEquals(sTest + "invoke failed to update key index", cExpect, ICount.intValue());

        nCount = cache.keySet(AlwaysFilter.INSTANCE).size();
        assertEquals(sTest + "invoke failed to update key index", cExpect, nCount);

        cExpect = Math.max(0, cInitSize - 2);
        cache.invokeAll(Collections.singleton(Integer.valueOf(1)), processor);
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

    private static void verifyStoreStats(String sTest, AbstractTestStore store, String sStatistic, int expected)
        {
        Integer count = (Integer) store.getStatsMap().get(sStatistic);
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
            assertThat(NResult, Matchers.allOf(Matchers.notNullValue(), is(Integer.valueOf(2))));

            // confirm aggregation returns the correct result only for entries that are present
            NResult = (Integer) cache.aggregate(new ImmutableArrayList(new Object[] {"2"}).getSet(),
                    new CustomAggregator(CACHE_RELATED, true));
            assertThat(NResult, Matchers.allOf(Matchers.notNullValue(), is(Integer.valueOf(2))));

            // wait for the entries to expire
            Eventually.assertThat(invoking(cache).size(), is(0));
            Eventually.assertThat(invoking(cacheJoined).size(), is(0));

            // same aggregation call should cause a read through
            NResult = (Integer) cache.aggregate(new ImmutableArrayList(new Object[] {"1"}).getSet(),
                    new CustomAggregator(CACHE_RELATED, false));
            assertThat(NResult, Matchers.allOf(Matchers.notNullValue(), is(Integer.valueOf(2))));

            // aggregation for only present entries should not cause a load
            NResult = (Integer) cache.aggregate(new ImmutableArrayList(new Object[] {"2"}).getSet(),
                    new CustomAggregator(CACHE_RELATED, true));
            assertThat(NResult, Matchers.anyOf(Matchers.nullValue(), is(Integer.valueOf(0))));

            // force a load on the 'driving' cache to ensure getReadOnlyEntry
            // does not result in a load
            assertEquals(Integer.valueOf(1), cache.get("2"));

            NResult = (Integer) cache.aggregate(new ImmutableArrayList(new Object[] {"2"}).getSet(),
                    new CustomAggregator(CACHE_RELATED, true));
            assertThat(NResult, Matchers.allOf(Matchers.notNullValue(), is(Integer.valueOf(1))));
            }
        finally
            {
            cache.destroy();
            cacheJoined.destroy();
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

            entry.setValue(new Integer(IValue.intValue() + 1));
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

            entry.setValue(new Integer(IValue.intValue() + 1));
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

            entry.setValue(new Integer(IValue.intValue() + 1));
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

            entry.setValue(new Integer(IValue.intValue() + 1));
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
    }
