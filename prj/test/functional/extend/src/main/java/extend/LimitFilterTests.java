/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package extend;

import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.tangosol.net.NamedCache;

import com.tangosol.util.Filter;
import com.tangosol.util.ValueExtractor;

import com.tangosol.util.extractor.ReflectionExtractor;

import com.tangosol.util.filter.AlwaysFilter;
import com.tangosol.util.filter.EqualsFilter;
import com.tangosol.util.filter.LimitFilter;

import com.oracle.coherence.testing.AbstractFunctionalTest;
import com.oracle.coherence.testing.AirDealComparator;
import com.oracle.coherence.testing.AirDealComparator.AirDeal;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
* A collection of functional tests for a Coherence*Extend client that uses
* LimitFilter.
*
* @author lh  2012.02.08
*/
public class LimitFilterTests
        extends AbstractFunctionalTest
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    public LimitFilterTests()
        {
        super(AbstractExtendTests.FILE_CLIENT_CFG_CACHE);
        }

    // ----- test lifecycle -------------------------------------------------

    /**
    * Initialize the test class.
    */
    @BeforeClass
    public static void startup()
        {
        CoherenceClusterMember memberProxy = startCacheServer("LimitFilterTests", "extend", AbstractExtendTests.FILE_SERVER_CFG_CACHE);
        Eventually.assertThat(invoking(memberProxy).isServiceRunning("ExtendTcpProxyService"), is(true));
        }

    /**
    * Shutdown the test class.
    */
    @AfterClass
    public static void shutdown()
        {
        stopCacheServer("LimitFilterTests");
        }

    @Before
    public void prepare()
        {
        NamedCache cache = getNamedCache(AbstractExtendTests.CACHE_DIST_EXTEND_DIRECT);

        cache.clear();
        int size = CACHE_SIZE;
        for (int i = 0; i < size; i++)
            {
            cache.put(i, "value" + i);
            }
        }

    // ----- LimitFilter tests ----------------------------------------------

    @Test
    public void testLimitFilter()
        {
        NamedCache           cache          = getNamedCache(AbstractExtendTests.CACHE_DIST_EXTEND_DIRECT);
        LimitFilter          limitFilter    = new LimitFilter(AlwaysFilter.INSTANCE, 10);
        Map<Integer, String> mapReturn      = new HashMap<>();
        boolean              fEntryReturned = true;
        int                  cTotal         = 0,
                             cUnique        = 0;

        while (fEntryReturned)
            {
            fEntryReturned = false;
            for (Iterator iter = cache.entrySet(limitFilter).iterator();
            		iter.hasNext(); )
                {
                Entry entry = (Entry) iter.next();
                ++cTotal;
                fEntryReturned = true;
                if (!mapReturn.containsKey(entry.getKey()))
                    {
                    mapReturn.put((Integer) entry.getKey(),
                            (String) entry.getValue());
                    ++cUnique;
                    }
                }
            limitFilter.nextPage();
            }

        assertThat(CACHE_SIZE, is(cTotal));
        assertThat(cTotal, is(cUnique));
        }

    @Test
    public void testComparator()
        {
        NamedCache cache = getNamedCache(AbstractExtendTests.CACHE_DIST_EXTEND_DIRECT);
        Random     rand  = new Random();
        for (int i=0; i < 10000; i++)
            {
            AirDeal deal = new AirDeal(i, "SFO", "JFK", rand.nextFloat());
            cache.put(deal.getOid(), deal);
            }
        ValueExtractor ve = new ReflectionExtractor("getOrigAirport");
        cache.addIndex(ve, true, null);

        Filter     primaryFilter = new EqualsFilter(ve, "SFO");
        Filter     filterLimit   = new LimitFilter(primaryFilter, 40);
        Set<Entry> setReturn     = cache.entrySet(filterLimit, new AirDealComparator());
        assertThat(setReturn.size(), is(40));
        }

    /**
     * Validate that the expected entries are in the returned LimitFilter page.
     * <p>
     * See .NET: Tangosol.Util.Filter.FilterTests.TestGetEntriesLimitFilterComparer()
     * and C++:  RemoteNamedCacheTest::testEntrySetLimitFilterComparator()
     */
    @Test
    public void testGetEntriesLimitFilterComparer()
        {
        final int                    PAGE_SIZE   = 10;
        Filter<Integer>              filterLimit = new LimitFilter<>(new AlwaysFilter<>(), PAGE_SIZE);
        Map<Integer, Integer>        map         = new HashMap<>();
        NamedCache<Integer, Integer> cache       = getNamedCache(AbstractExtendTests.CACHE_DIST_EXTEND_DIRECT);

        cache.clear();

        for(int i = 0; i < 1000; i++)
            {
            map.put(i, i);
            }

        cache.putAll(map);

        Comparator                       comparer   = new AbstractExtendTests.IntegerComparator();
        Set<Map.Entry<Integer, Integer>> setEntries = cache.entrySet(filterLimit, comparer);

        Assert.assertNotNull(setEntries);
        Assert.assertEquals(PAGE_SIZE, setEntries.size());

        int k = 1000;
        for (Map.Entry entry : setEntries)
            {
            k--;
            Assert.assertEquals(k, entry.getValue());
            }
        }

    private static final int CACHE_SIZE = 84;
    }
