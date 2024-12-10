/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package bundler;

import com.tangosol.net.NamedCache;

import com.tangosol.net.cache.AbstractBundler;
import com.tangosol.net.cache.BundlingNamedCache;
import com.tangosol.net.cache.ReadWriteBackingMap;

import com.oracle.coherence.common.base.Blocking;
import com.oracle.coherence.testing.AbstractFunctionalTest;
import com.oracle.coherence.testing.AbstractTestStore;
import com.oracle.coherence.testing.TestBinaryCacheStore;
import com.oracle.coherence.testing.TestHelper;
import com.oracle.coherence.testing.TestNonBlockingStore;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * BundlingNamedCache functional tests.
 *
 * NOTE: These tests use prj/test/bundler/coherence-cache-config.xml.
 *
 * @author gg Feb 19, 2007
 */
public class BundlingNamedCacheTests
        extends AbstractFunctionalTest
    {
    // ----- test lifecycle -------------------------------------------------

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

    // ----- test methods ---------------------------------------------------

    /**
     * Test concurrent put operations.
     */
   @Test
    public void testConcurrentPut()
        {
        testConcurrentPut("dist-test");
        testConcurrentPut("dist-test-rwbm");
        testConcurrentPut("dist-test-rwbm-bin");
        testConcurrentPut("dist-test-rwbm-bin-nb");
        }

    /**
     * Test concurrent put operations.
     *
     * @param sCache  the cache name
     */
    protected void testConcurrentPut(String sCache)
        {
        final int        COUNT = 1000;
        final NamedCache cache = getNamedCache(sCache);

        Runnable task = new Runnable()
            {
            public void run()
                {
                waitForSemaphore();
                int ofStart = getThreadIndex()*COUNT;
                for (int i = 0; i < COUNT; i++)
                    {
                    cache.put(ofStart + i, String.valueOf(ofStart + i));
                    }
                }
            };

        resetSemaphore();
        runParallel(task, THREADS);

        testCacheContent(cache, THREADS*COUNT);
        cache.clear();
        }

    /**
     * Test concurrent put operations with a BinaryEntryStore or NonBlockingStore
     * that modifies the entries during store() operation.
     */
    @Test
    public void testConcurrentPut_BinaryEntryStore_Modify()
        {
        testConcurrentPut_BinaryEntryStore_Modify("dist-test-rwbm-bin");
        testConcurrentPut_BinaryEntryStore_Modify("dist-test-rwbm-bin-nb");
        }

    public void testConcurrentPut_BinaryEntryStore_Modify(String sCacheName)
        {
        final NamedCache cache  = getNamedCache(sCacheName);
        final int        COUNT  = 1000;
        final Object     oValue = "value";

        Object store = getBinaryEntryStore(cache);
        TestBinaryCacheStore binaryStore = null;
        TestNonBlockingStore nonBlockingStore = null;

        if (store instanceof TestBinaryCacheStore)
            {
            binaryStore = (TestBinaryCacheStore) store;
            binaryStore.setStoreValue(oValue);
            }
        else if (store instanceof TestNonBlockingStore)
            {
            nonBlockingStore = (TestNonBlockingStore) store;
            nonBlockingStore.setStoreValue(oValue);
            }

        Runnable task = new Runnable()
            {
            public void run()
                {
                waitForSemaphore();
                int ofStart = getThreadIndex()*COUNT;
                for (int i = 0; i < COUNT; i++)
                    {
                    cache.put(ofStart + i, String.valueOf(ofStart + i));
                    }
                }
            };

        resetSemaphore();
        runParallel(task, THREADS);

        testCacheContent(cache, THREADS*COUNT, oValue);
        cache.clear();

        if (store instanceof TestBinaryCacheStore)
            {
            binaryStore.setStoreValue(null);
            }
        else if (store instanceof TestNonBlockingStore)
            {
            nonBlockingStore.setStoreValue(null);
            }
        }

    /**
     * Test concurrent putAll operations.
     */
    @Test
    public void testConcurrentPutAll()
        {
        testConcurrentPutAll("dist-test");
        testConcurrentPutAll("dist-test-rwbm");
        testConcurrentPutAll("dist-test-rwbm-bin");
        testConcurrentPutAll("dist-test-rwbm-bin-nb");
        }

    /**
     * Test concurrent putAll operations.
     *
     * @param sCache  the cache name
     */
    protected void testConcurrentPutAll(String sCache)
        {
        final int        COUNT = 1000;
        final NamedCache cache = getNamedCache(sCache);

        Runnable task = new Runnable()
            {
            public void run()
                {
                waitForSemaphore();
                int ofStart = getThreadIndex()*COUNT;
                Map mapTemp = new HashMap();
                for (int i = 0; i < COUNT; i++)
                    {
                    mapTemp.put(ofStart + i, String.valueOf(ofStart + i));

                    if (mapTemp.size() > getRandom().nextInt(5) ||
                            i == COUNT - 1)
                        {
                        cache.putAll(mapTemp);
                        mapTemp.clear();
                        }
                    }
                }
            };

        resetSemaphore();
        runParallel(task, THREADS);

        testCacheContent(cache, THREADS*COUNT);
        cache.clear();
        }

    /**
     * Test concurrent putAll operations with a BinaryEntryStore that modifies
     * the entries during store() operation.
     */
    @Test
    public void testConcurrentPutAll_BinaryEntryStore_Modify()
        {
        testConcurrentPutAll_BinaryEntryStore_Modify("dist-test-rwbm-bin");
        testConcurrentPutAll_BinaryEntryStore_Modify("dist-test-rwbm-bin-nb");
        }

    public void testConcurrentPutAll_BinaryEntryStore_Modify(String sCacheName)
        {
        final NamedCache cache  = getNamedCache(sCacheName);
        final int        COUNT  = 1000;
        final Object     oValue = "value";

        Object store = getBinaryEntryStore(cache);
        TestBinaryCacheStore binaryStore = null;
        TestNonBlockingStore nonBlockingStore = null;

        if (store instanceof TestBinaryCacheStore)
            {
            binaryStore = (TestBinaryCacheStore) store;
            binaryStore.setStoreValue(oValue);
            }
        else if (store instanceof TestNonBlockingStore)
            {
            nonBlockingStore = (TestNonBlockingStore) store;
            nonBlockingStore.setStoreValue(oValue);
            }

        Runnable task = new Runnable()
            {
            public void run()
                {
                waitForSemaphore();
                int ofStart = getThreadIndex()*COUNT;
                Map mapTemp = new HashMap();
                for (int i = 0; i < COUNT; i++)
                    {
                    mapTemp.put(ofStart + i, String.valueOf(ofStart + i));

                    if (mapTemp.size() > getRandom().nextInt(5) ||
                            i == COUNT - 1)
                        {
                        cache.putAll(mapTemp);
                        mapTemp.clear();
                        }
                    }
                }
            };

        resetSemaphore();
        runParallel(task, THREADS);

        testCacheContent(cache, THREADS*COUNT, oValue);
        cache.clear();

        if (store instanceof TestBinaryCacheStore)
            {
            binaryStore.setStoreValue(null);
            }
        else if (store instanceof TestNonBlockingStore)
            {
            nonBlockingStore.setStoreValue(null);
            }
        }

    /**
     * Test concurrent get operations.
     */
    @Test
    public void testConcurrentGet()
        {
        testConcurrentGet("dist-test");
        testConcurrentGet("dist-test-rwbm");
        testConcurrentGet("dist-test-rwbm-bin");
        testConcurrentGet("dist-test-rwbm-bin-nb");
        }

    /**
     * Test concurrent get operations.
     *
     * @param sCache  the cache name
     */
    protected void testConcurrentGet(String sCache)
        {
        final int        COUNT = 1000;
        final NamedCache cache = getNamedCache(sCache);

        fill(cache, THREADS*COUNT);
        Runnable task = new Runnable()
            {
            public void run()
                {
                waitForSemaphore();
                for (int i = 0; i < COUNT; i++)
                    {
                    int    iKey = getRandom().nextInt(cache.size());
                    Object oVal = cache.get(iKey);
                    assertEquals(String.valueOf(iKey), oVal);
                    }
                }
            };

        resetSemaphore();
        runParallel(task, THREADS);

        // test all threads hitting the same key
        task = new Runnable()
            {
            public void run()
                {
                waitForSemaphore();
                for (int i = 0; i < COUNT; i++)
                    {
                    Object oVal = cache.get(0);
                    assertEquals("0", oVal);
                    }
                }
            };

        resetSemaphore();
        runParallel(task, THREADS);

        cache.clear();
        }

    /**
     * Test concurrent getAll operations.
     */
    @Test
    public void testConcurrentGetAll()
        {
        testConcurrentGetAll("dist-test");
        testConcurrentGetAll("dist-test-rwbm");
        testConcurrentGetAll("dist-test-rwbm-bin");
        testConcurrentGetAll("dist-test-rwbm-bin-nb");
        }

    /**
     * Test concurrent getAll operations.
     *
     * @param sCache  the cache name
     */
    protected void testConcurrentGetAll(String sCache)
        {
        final int        COUNT = 1000;
        final NamedCache cache = getNamedCache(sCache);

        fill(cache, THREADS*COUNT);
        Runnable task = new Runnable()
            {
            public void run()
                {
                waitForSemaphore();
                Set setKeys = new HashSet();
                for (int i = 0; i < COUNT; i++)
                    {
                    Integer IKey = getRandom().nextInt(cache.size());

                    setKeys.add(IKey);
                    if (setKeys.size() > getRandom().nextInt(5))
                        {
                        Map map = cache.getAll(setKeys);

                        assertEquals(setKeys.size(), map.size());
                        setKeys.clear();

                        for (Iterator iter = map.entrySet().iterator(); iter.hasNext();)
                            {
                            Map.Entry entry = (Map.Entry) iter.next();

                            assertEquals(entry.getKey().toString(), entry.getValue());
                            }
                        }
                    }
                }
            };

        resetSemaphore();
        runParallel(task, THREADS);
        cache.clear();
        }

    /**
     * Test non-blocking store with incomplete result set due to exception
     */
    @Test
    public void testReadAllAsyncException()
        {
        NamedCache cache      = getNamedCache("dist-test-rwbm-bin-nb");
        String     errorKey   = "Key87";
        try
            {
            cache.clear();
            AbstractTestStore store = getStore(cache);
            store.resetStats();

            store.setFailureKeyLoadAll(errorKey);

            // prime the cache contents
            Map mapContents = new HashMap();
            for (int i = 0 ; i < 200; i++)
                {
                mapContents.put("Key" + i, "Value" + i);
                }
            store.getStorageMap().putAll(mapContents);

            Map resultMap = cache.getAll(mapContents.keySet());

            assertEquals(store.getStorageMap().size() - 1, cache.size());
            mapContents.remove(errorKey);
            assertEquals(mapContents, resultMap);
            }
        finally
            {
            cache.destroy();
            }
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
     * Test a disabled bundle configuration.  All bundles are disabled when the
     * preferred-size is zero (the default).
     */
    @Test
    public void testDistBundlingDisabledConfig()
        {
        final BundlingNamedCache cache = (BundlingNamedCache) getNamedCache("dist-test-bundled-config-disabled");
        assertNull(cache.getGetBundler());
        assertNull(cache.getPutBundler());
        assertNull(cache.getRemoveBundler());
        }

    /**
     * Test a default bundle configuration.  The preferred-size must be > 0 so that
     * the default operation-name of "all" takes effect.
     */
    @Test
    public void testDistBundlingDefaultConfig()
        {
        final BundlingNamedCache cache = (BundlingNamedCache) getNamedCache("dist-test-bundled-config-defaults");
        validateDefaultBundler(cache.getGetBundler());
        validateDefaultBundler(cache.getPutBundler());
        validateDefaultBundler(cache.getRemoveBundler());
        }

    /**
     * Test a bundle configuration with customized values.
     */
    @Test
    public void testDistBundlingCustomizedConfig()
        {
        final BundlingNamedCache cache = (BundlingNamedCache) getNamedCache("dist-test-bundled-config-customized");
        validateCustomizedBundler(cache.getGetBundler());
        validateCustomizedBundler(cache.getPutBundler());
        validateCustomizedBundler(cache.getRemoveBundler());
        }

    /**
     * Test multiple bundle configurations.
     */
    @Test
    public void testDistBundlingMultipleConfig()
        {
        final BundlingNamedCache cache = (BundlingNamedCache) getNamedCache("dist-test-bundled-config-multiple");
        validateCustomizedBundler(cache.getGetBundler());
        validateDefaultBundler(cache.getPutBundler());
        assertNull(cache.getRemoveBundler());
        }

    /**
     * Test a disabled CacheStore bundle configuration.   All bundles are disabled when the
     * preferred-size is zero (the default).
     */
    @Test
    public void testCacheStoreBundlingDisabledConfig()
        {
        final NamedCache cache = getNamedCache("dist-test-bundled-cachestore-config-disabled");
        ReadWriteBackingMap.StoreWrapper wrapper =
            ((ReadWriteBackingMap) TestHelper.getBackingMap(cache)).getCacheStore();

        assertNull(wrapper.getLoadBundler());
        assertNull(wrapper.getStoreBundler());
        assertNull(wrapper.getEraseBundler());
        }

    /**
     * Test a default CacheStore bundle configuration.  The preferred-size must be > 0 so that
     * the default operation-name of "all" takes effect.
     */
    @Test
    public void testCacheStoreBundlingDefaultConfig()
        {
        final NamedCache cache = getNamedCache("dist-test-bundled-cachestore-config-defaults");
        ReadWriteBackingMap.StoreWrapper wrapper =
            ((ReadWriteBackingMap) TestHelper.getBackingMap(cache)).getCacheStore();

        validateDefaultBundler(wrapper.getLoadBundler());
        validateDefaultBundler(wrapper.getStoreBundler());
        validateDefaultBundler(wrapper.getEraseBundler());
        }

    /**
     * Test a CacheStore bundle configuration with customized values.
     */
    @Test
    public void testCacheStoreBundlingCustomizedConfig()
        {
        final NamedCache cache = getNamedCache("dist-test-bundled-cachestore-config-customized");
        ReadWriteBackingMap.StoreWrapper wrapper =
            ((ReadWriteBackingMap) TestHelper.getBackingMap(cache)).getCacheStore();

        validateCustomizedBundler(wrapper.getLoadBundler());
        validateCustomizedBundler(wrapper.getStoreBundler());
        validateCustomizedBundler(wrapper.getEraseBundler());
        }

    /**
     * Test multiple CacheStore bundles
     * bundle configurations.
     */
    @Test
    public void testCacheStoreBundlingMultipleConfig()
        {
        final NamedCache cache = getNamedCache("dist-test-bundled-cachestore-config-multiple");
        ReadWriteBackingMap.StoreWrapper wrapper =
            ((ReadWriteBackingMap) TestHelper.getBackingMap(cache)).getCacheStore();

        validateCustomizedBundler(wrapper.getLoadBundler());
        validateDefaultBundler(wrapper.getStoreBundler());
        assertNull(wrapper.getEraseBundler());
        }

    /**
     * Validate the default bundler.
     *
     * @param bundler  the bundler
     */
    public static void validateDefaultBundler(AbstractBundler bundler)
        {
        assertEquals(10, bundler.getSizeThreshold()); // preferred-size
        assertEquals(1, bundler.getDelayMillis());
        assertEquals(4, bundler.getThreadThreshold());
        assertFalse(bundler.isAllowAutoAdjust());
        }

    /**
     * Validate the bundler that has customized values.
     *
     * @param bundler  the bundler
     */
    public static void validateCustomizedBundler(AbstractBundler bundler)
        {
        assertEquals(10, bundler.getSizeThreshold()); // preferred-size
        assertEquals(100, bundler.getDelayMillis());
        assertEquals(15, bundler.getThreadThreshold());
        assertTrue(bundler.isAllowAutoAdjust());
        }

    /**
     * Return the TestBinaryCacheStore for the given NamedCache.
     *
     * @param cache  the NamedCache
     *
     * @return the TestBinaryCacheStore
     *
     * @throws IllegalArgumentException if a TestBinaryCacheStore could not be
     *         be found
     */
    protected Object getBinaryEntryStore(NamedCache cache)
        {
        ReadWriteBackingMap.StoreWrapper wrapper =
            ((ReadWriteBackingMap) TestHelper.getBackingMap(cache)).getCacheStore();

        Object store = wrapper.getStore();
        if (store instanceof TestBinaryCacheStore || store instanceof TestNonBlockingStore)
            {
            return store;
            }

        throw new IllegalArgumentException();
        }

    /**
     * Run the specified task on multiple cThreads and wait for completion.
     *
     * @param task      the task to run
     * @param cThreads  the number of threads
     */
    protected static void runParallel(Runnable task, int cThreads)
        {
        Thread aThread[] = new Thread[cThreads];
        for (int i = 0; i < cThreads; i++)
            {
            aThread[i] = new Thread(task);
            aThread[i].setName(PREFIX + i);
            aThread[i].start();
            }

        synchronized (SEMAPHOR)
            {
            s_fStart = true;
            SEMAPHOR.notifyAll();
            }

        try
            {
            for (int i = 0; i < cThreads; i++)
                {
                aThread[i].join();
                }
            }
        catch (InterruptedException e) {/*do nothing*/}
        }

    /**
     * Retrieve the thread index from its name.
     *
     * @return the thread index
     */
    protected static int getThreadIndex()
        {
        String sName = Thread.currentThread().getName();
        int    ofIx  = sName.lastIndexOf(PREFIX);
        assertTrue(ofIx >= 0);
        return Integer.parseInt(sName.substring(ofIx + PREFIX.length()));
        }

    /**
     * Thread synchronization support.
     */
    protected static void resetSemaphore()
        {
        s_fStart = false;
        }

    /**
     * Thread synchronization support.
     */
    protected static void waitForSemaphore()
        {
        synchronized (SEMAPHOR)
            {
            while (!s_fStart)
                {
                try
                    {
                    Blocking.wait(SEMAPHOR);
                    }
                catch (InterruptedException e) {/*do nothing*/}
                }
            }
        }

    /**
     * Test the cache content.
     *
     * @param cache  the cache to test
     * @param cnt    the count
     */
    private static void testCacheContent(Map cache, int cnt)
        {
        testCacheContent(cache, cnt, null);
        }

    /**
     * Test the cache content.
     *
     * @param cache      the cache to test
     * @param cnt        the count
     * @param oExpected  the expected value
     */
    private static void testCacheContent(Map cache, int cnt, Object oExpected)
        {
        assertEquals(cnt, cache.size());
        for (int i = 0; i < cnt; i++)
            {
            Object oVal = cache.get(i);
            assertEquals(oExpected == null ? String.valueOf(i) : oExpected, oVal);
            }
        }

    /**
     * Fill the specified map with <Integer, String> entries.
     *
     * @param cache  the cache to fill
     * @param cnt    the count
     */
    private static void fill(Map cache, int cnt)
        {
        for (int i = 0; i <= cnt; ++i)
            {
            cache.put(i, String.valueOf(i));
            }
        }

    // ----- fields and constants -------------------------------------------

    static final String PREFIX   = "Thread-";
    static final Object SEMAPHOR = new Object();
    static final int    THREADS  = 25;

    static volatile boolean s_fStart;
    }
