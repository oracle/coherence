/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package filter;


import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.tangosol.net.CacheService;
import com.tangosol.net.DistributedCacheService;
import com.tangosol.net.NamedCache;

import com.tangosol.util.Base;
import com.tangosol.util.extractor.AbstractExtractor;
import com.tangosol.util.extractor.KeyExtractor;

import com.tangosol.util.filter.AlwaysFilter;
import com.tangosol.util.filter.LimitFilter;

import com.oracle.coherence.testing.AbstractFunctionalTest;

import data.Person;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

import junit.framework.Assert;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertTrue;


/**
*
* @author coh 2010.08.25
*/
public class LimitFilterTests
        extends AbstractFunctionalTest
    {
    @Before
    public void prepare()
        {
        NamedCache cache = getNamedCache();

        // until COH-6496 is fixed, LimitFilter implementation can be "unstable"
        // during redistribution
        CacheService service = cache.getCacheService();
        if (service instanceof DistributedCacheService)
            {
            DistributedCacheService serviceDist = (DistributedCacheService) service;
            if (serviceDist.getOwnershipEnabledMembers().size() > 1)
                {
                waitForBalanced(serviceDist);
                }
            }

        cache.clear();
        int size = CACHE_SIZE;
        for (int i = 0; i < size; i++)
            {
            cache.put(i, "value" + i);
            }
        out("Populated: Cache size: " + cache.size());
        }

    @Test
    public void testPageSizeDouble()
        {
        out("testPageSizeDouble");
        NamedCache cache = getNamedCache();
        LimitFilter pagedFilter = new LimitFilter(AlwaysFilter.INSTANCE,
                CACHE_SIZE * 2);
        //all entries, one per page
        Set<?> data;
        Set allData = new HashSet();
        int iterations = 0;
        while (true)
            {
            data = cache.entrySet(pagedFilter);

            if (data.isEmpty())
                {
                break;
                }
            allData.addAll(data);
            iterations++;
            pagedFilter.nextPage();
            }
        out("Number of entries fetched " + allData.size()
                + ", iterations " + iterations
                + " : Number of items in Cache: " + cache.size());
        Assert.assertEquals(CACHE_SIZE, allData.size());
        }

    @Test
    public void testPageSizeHalf()
        {
        out("testPageSizeHalf");
        NamedCache cache = getNamedCache();
        LimitFilter pagedFilter = new LimitFilter(AlwaysFilter.INSTANCE,
                CACHE_SIZE / 2);
        //all entries, one per page
        Set<?> data;
        Set allData = new HashSet();
        int iterations = 0;
        while (true)
            {
            data = cache.entrySet(pagedFilter);

            if (data.isEmpty())
                {
                break;
                }
            allData.addAll(data);
            iterations++;
            pagedFilter.nextPage();
            }
        out("Number of entries fetched " + allData.size()
                + ", iterations " + iterations
                + " : Number of items in Cache: " + cache.size());
        Assert.assertEquals(CACHE_SIZE, allData.size());
        }

    @Test
    public void testPageSizeOne()
        {
        out("testPageSizeOne");
        NamedCache cache = getNamedCache();
        LimitFilter pagedFilter = new LimitFilter(AlwaysFilter.INSTANCE, 1);
        //all entries, one per page
        Set<?> data;
        Set allData = new HashSet();
        int iterations = 0;
        while (true)
            {
            data = cache.entrySet(pagedFilter);

            if (data.isEmpty())
                {
                break;
                }
            allData.addAll(data);
            iterations++;
            pagedFilter.nextPage();
            }
        out("Number of entries fetched " + allData.size()
                + ", iterations " + iterations
                + " : Number of items in Cache: " + cache.size());
        Assert.assertEquals(CACHE_SIZE, allData.size());
        }

    @Test
    public void testPageSizeSeven()
        {
        out("testPageSizeSeven");
        NamedCache cache = getNamedCache();
        LimitFilter pagedFilter = new LimitFilter(AlwaysFilter.INSTANCE, 7);
        //all entries, one per page
        Set<?> data;
        Set allData = new HashSet();
        int iterations = 0;
        while (true)
            {
            data = cache.entrySet(pagedFilter);

            if (data.isEmpty())
                {
                break;
                }
            allData.addAll(data);
            iterations++;
            pagedFilter.nextPage();
            }
        out("Number of entries fetched " + allData.size()
                + ", iterations " + iterations
                + " : Number of items in Cache: " + cache.size());
        Assert.assertEquals(CACHE_SIZE, allData.size());
        }

    @Test
    public void testPageSizeThird()
        {
        out("testPageSizeThird");
        NamedCache cache = getNamedCache();
        LimitFilter pagedFilter = new LimitFilter(AlwaysFilter.INSTANCE,
                CACHE_SIZE / 3);
        //all entries, one per page
        Set<?> data;
        Set allData = new HashSet();
        int iterations = 0;

        Base.sleep(1000);
        while (true)
            {
            data = cache.entrySet(pagedFilter);

            if (data.isEmpty())
                {
                break;
                }
            allData.addAll(data);
            iterations++;
            pagedFilter.nextPage();
            }
        out("Number of entries fetched " + allData.size()
                + ", iterations " + iterations
                + " : Number of items in Cache: " + cache.size());
        Assert.assertEquals(CACHE_SIZE, allData.size());
        }

    @Test
    public void testSetPage()
        {
        out("testSetPage");
        NamedCache cache   = getNamedCache();
        int pageSize       = CACHE_SIZE / 10;
        int totalPages     = CACHE_SIZE / pageSize + (CACHE_SIZE % pageSize > 0 ? 1 : 0);
        LimitFilter filter = new LimitFilter(AlwaysFilter.INSTANCE, pageSize);

        filter.setPage(new Random().nextInt(totalPages));
        // same pages have same keys
        assertEqualKeySet(cache.keySet(filter), cache.keySet(filter));
        }

    @Test
    public void testForwardAndBackward()
        {
        out("testForwardAndBackward");
        NamedCache cache   = getNamedCache();
        int pageSize       = CACHE_SIZE / 10;
        int totalPages     = CACHE_SIZE / pageSize + (CACHE_SIZE % pageSize > 0 ? 1 : 0);
        LimitFilter filter = new LimitFilter(AlwaysFilter.INSTANCE, pageSize);

        Set<?>[] forward = new Set<?>[totalPages];
        filter.setPage(0);
        for (int i = 0; i < totalPages; i++)
            {
            forward[i] = cache.keySet(filter);
            filter.nextPage();
            }

        Set<?>[] backward = new Set<?>[totalPages];
        filter.setPage(totalPages-1);
        for (int i = totalPages - 1; i >= 0; i--)
            {
            backward[i] = cache.keySet(filter);

            if (i != 0)
                {
                filter.previousPage();
                }
            }

        for (int i = 0; i < totalPages; i++)
            {
            assertEqualKeySet(forward[i], backward[i]);
            }
        }

    @Test
    public void testQuery()
        {
        final int SIZE = 1000;
        NamedCache cache = getNamedCache();

        cache.clear();
        Person.fillRandom(cache, SIZE);
        out("Populated Cache with " + cache.size() + " persons.");

        KeyExtractor keyExtractor = new KeyExtractor("getId");
        testQueryWithExtractor(cache, SIZE, keyExtractor);

        MyExtractor  valueExtractor = new MyExtractor("getId");
        testQueryWithExtractor(cache, SIZE, valueExtractor);
        }

    void testQueryWithExtractor(NamedCache cache, int size, AbstractExtractor extractor)
        {
        int pageSize   = 100;
        int totalPages = size / pageSize + (CACHE_SIZE % pageSize > 0 ? 1 : 0);

        LimitFilter  pagedFilter = new LimitFilter(AlwaysFilter.INSTANCE, pageSize);

        out("testQuery using extractor: " + extractor);

        Collection data;
        Set allData = new HashSet();
        int iterations = 0;

        Base.sleep(1000);

        Person pageFirst  = null;
        Person pageSecond = null;
        while (iterations < totalPages)
            {
            data = cache.values(pagedFilter, extractor);

            if (data.isEmpty())
                {
                break;
                }

            int    i = 0;
            int    nFirst     = new Random().nextInt(pageSize/2);
            int    nSecond    = new Random().nextInt(pageSize/2, pageSize - 1);
            Person first      = null;
            Person second     = null;

            Iterator<Person> iterator = data.iterator();
            while (iterator.hasNext())
                {
                if (i == nFirst)
                    {
                    first = iterator.next();
                    }
                if (i == nSecond)
                    {
                    second = iterator.next();
                    }
                else
                    {
                    iterator.next();
                    }
                i++;
                }
            assertTrue(((String) extractor.extract(second)).compareTo((String) extractor.extract(first)) > 0);

            if (iterations == 1)
                {
                pageFirst = second;
                }
            else if (iterations == 5)
                {
                pageSecond = first;
                }

            allData.addAll(data);
            iterations++;
            pagedFilter.nextPage();
            }
        assertTrue(((String) extractor.extract(pageSecond)).compareTo((String) extractor.extract(pageFirst)) > 0);
        assertTrue(cache.size() == allData.size());

        // Compare forward/backward sets
        Collection[] forward = new Collection[totalPages];
        pagedFilter.setPage(0);
        for (int i = 0; i < totalPages; i++)
            {
            forward[i] = cache.values(pagedFilter, extractor);
            pagedFilter.nextPage();
            }

        Collection[] backward = new Collection[totalPages];
        pagedFilter.setPage(totalPages-1);
        for (int i = totalPages - 1; i >= 0; i--)
            {
            backward[i] = cache.values(pagedFilter, extractor);

            if (i != 0)
                {
                pagedFilter.previousPage();
                }
            }

        for (int i = 0; i < totalPages; i++)
            {
            Set<String> setForward = new HashSet<>(forward[i]);
            Set<String> setBackward = new HashSet<>(backward[i]);
            assertEqualKeySet(setForward, setBackward);
            }
        }

    @After
    public void unprepare()
        {
        getNamedCache().clear();
        }

    /**
    *
    * @return
    */
    private String getCacheName()
        {
        return "dist-test";
        }

    /**
     * Return the cache used in all test methods.
     *
     * @return the test cache
     */
    protected NamedCache getNamedCache()
        {
        return getNamedCache(getCacheName());
        }

    /**
    * Shutdown the test class.
    */
    @AfterClass
    public static void shutdown()
        {
        stopCacheServer("DistLimitFilterTests-1");
        stopCacheServer("DistLimitFilterTests-2");
        stopCacheServer("DistLimitFilterTests-3");
        }

    /**
     * Initialize the test class.
     */
    @BeforeClass
    public static void startup()
        {
        startCacheServer("DistLimitFilterTests-1", "filter");
        startCacheServer("DistLimitFilterTests-2", "filter");
        startCacheServer("DistLimitFilterTests-3", "filter");
        }

    @Before
    public void beforeTest()
        {
        NamedCache              cache   = getNamedCache();
        DistributedCacheService service = (DistributedCacheService) cache.getCacheService();
        Eventually.assertThat(invoking(service).getOwnershipEnabledMembers().size(), is(3));
        }

    /**
    *
    */
    private static final int CACHE_SIZE = 50;
    }
