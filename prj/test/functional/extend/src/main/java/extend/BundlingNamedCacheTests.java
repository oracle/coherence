/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package extend;

import com.oracle.coherence.common.base.Blocking;
import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.tangosol.net.NamedCache;

import com.oracle.coherence.testing.AbstractFunctionalTest;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
* A collection of functional tests for a Coherence*Extend client that uses
* operation bundling to perform cache operations.
*
* @author lsho  2012.05.02
*/
public class BundlingNamedCacheTests
        extends AbstractFunctionalTest
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    public BundlingNamedCacheTests()
        {
        super("client-cache-config-bundle.xml");
        }

    // ----- test lifecycle -------------------------------------------------

    /**
    * Initialize the test class.
    */
    @BeforeClass
    public static void startup()
        {
        CoherenceClusterMember member = startCacheServer("BundlingNamedCacheTests", "extend",
                                                "server-cache-config-bundle.xml");

        Eventually.assertThat(invoking(member).isServiceRunning("ExtendTcpProxyService"), is(true));
        }

    /**
    * Shutdown the test class.
    */
    @AfterClass
    public static void shutdown()
        {
        stopCacheServer("BundlingNamedCacheTests");
        }

    // ----- Operation bundling Extend tests --------------------------------

    /**
    * Test concurrent put operations.
    */
    @Test
    public void testConcurrentPut()
        {
        final int        COUNT = 1000;
        final NamedCache cache = getNamedCache(CACHE_NAME);

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

        testCacheContent(cache, THREADS * COUNT);
        cache.clear();
        }

    /**
    * Test concurrent putAll operations.
    */
    @Test
    public void testConcurrentPutAll()
        {
        final int        COUNT = 1000;
        final NamedCache cache = getNamedCache(CACHE_NAME);

        Runnable task = new Runnable()
            {
            public void run()
                {
                waitForSemaphore();
                int ofStart = getThreadIndex() * COUNT;
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

        testCacheContent(cache, THREADS * COUNT);
        cache.clear();
        }

    /**
    * Test concurrent get operations.
    */
    @Test
    public void testConcurrentGet()
        {
        final int        COUNT = 1000;
        final NamedCache cache = getNamedCache(CACHE_NAME);

        fill(cache, THREADS * COUNT);
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
        final int        COUNT = 1000;
        final NamedCache cache = getNamedCache(CACHE_NAME);

        fill(cache, THREADS * COUNT);
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

                        for (
                                Iterator iter = map.entrySet().iterator(); iter.hasNext();)
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

        synchronized (SEMAPHORE)
            {
            s_fStart = true;
            SEMAPHORE.notifyAll();
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
    * Retrive the thread index from its name.
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
        synchronized (SEMAPHORE)
            {
            while (!s_fStart)
                {
                try
                    {
                    Blocking.wait(SEMAPHORE);
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

    static final String     CACHE_NAME   = "dist-test";
    static final String     PREFIX       = "Thread-";
    static final Object     SEMAPHORE    = new Object();
    static final int        THREADS      = 25;
    static volatile boolean s_fStart;
    }
