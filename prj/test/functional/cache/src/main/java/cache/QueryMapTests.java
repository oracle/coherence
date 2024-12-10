/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package cache;

import com.oracle.bedrock.junit.CoherenceClusterResource;
import com.oracle.bedrock.junit.SessionBuilders;

import com.oracle.bedrock.runtime.coherence.CoherenceCluster;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;

import com.oracle.bedrock.runtime.coherence.options.CacheConfig;
import com.oracle.bedrock.runtime.coherence.options.ClusterName;
import com.oracle.bedrock.runtime.coherence.options.LocalHost;
import com.oracle.bedrock.runtime.coherence.options.LocalStorage;
import com.oracle.bedrock.runtime.coherence.options.WellKnownAddress;

import com.oracle.bedrock.runtime.java.options.IPv4Preferred;
import com.oracle.bedrock.runtime.options.DisplayName;

import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.testsupport.junit.TestLogs;

import com.oracle.coherence.testing.AbstractFunctionalTest;

import com.tangosol.io.ExternalizableLite;

import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.NamedCache;

import com.tangosol.util.Extractors;
import com.tangosol.util.Filter;
import com.tangosol.util.Filters;
import com.tangosol.util.QueryMap;
import com.tangosol.util.SimpleMapEntry;
import com.tangosol.util.filter.LimitFilter;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static com.tangosol.net.cache.TypeAssertion.withoutTypeChecking;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * A Test class to validate the functionality of QueryMap for various cache types
 *
 * @author er 2022-0208
 */
@RunWith(Parameterized.class)
public class QueryMapTests
        extends AbstractFunctionalTest
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a {@link QueryMapTests} instance.
     *
     * @param f_sDescription  the human-readable cache type for the test description.
     * @param f_sCacheName    the cache name to test (should map to a valid name in the coherence-cache-config.xml file
     *                        in this module's resources/ folder).
     */
    public QueryMapTests(String f_sDescription, String f_sCacheName)
        {
        this.f_sDescription = f_sDescription;
        this.f_sCacheName   = f_sCacheName;
        }

    @BeforeClass
    public static void _startup()
        {
        CoherenceCluster cluster = s_cluster.getCluster();
        Eventually.assertDeferred(cluster::getClusterSize, is(2));

        s_ccf = s_cluster.createSession(SessionBuilders.storageDisabledMember());
        }

    @ClassRule
    public static final TestLogs s_testLogs = new TestLogs(QueryMapTests.class);

    @ClassRule
    public static CoherenceClusterResource s_cluster = new CoherenceClusterResource()
            .with(ClusterName.of("QueryMappedTests"),
                  CacheConfig.of("coherence-cache-config.xml"),
                  WellKnownAddress.loopback(),
                  LocalHost.only())
            .include(2, CoherenceClusterMember.class,
                     LocalStorage.enabled(),
                     IPv4Preferred.autoDetect(),
                     DisplayName.of("Storage"),
                     s_testLogs);

    /**
     * Provide the parameters for the tests.
     * <p>
     * This method returns arrays of parameter pairs that will be passed to
     * the constructor of this class. The first parameter is a descriptive
     * name of the type of cache being tested, the second is the name of
     * the cache. The cache name must map to a valid name in the
     * coherence-cache-config.xml file in this module's resources/ folder.
     *
     * @return parameters for the test
     */
    @Parameterized.Parameters(name="{0}")
    public static Iterable<Object[]> data()
        {
        return Arrays.asList(
            new Object[] {"Dist Cache", "dist-test"},
            new Object[] {"View Cache", "view-dist-direct"},
            new Object[] {"Local Cache", "local-dist-direct"},
            new Object[] {"Near Cache", "near-dist-direct"}
            );
        }

    /**
     * Return the cache used by the tests.
     * <p>
     * This should be the plain {@link NamedCache}.
     *
     * @param <K>  the cache key type
     * @param <V>  the cache value type
     *
     * @return the cache used by the tests
     */
    protected <K, V> NamedCache<K, V> getNamedCache()
        {
        s_cDeserializationCount = 0;

        NamedCache<K, V> cache = s_ccf.ensureTypedCache(f_sCacheName, null, withoutTypeChecking());
        cache.clear();
        return cache;
        }

    // ----- QueryMap test methods ----------------------------------------

    /**
     * Verify that call to {@link QueryMap#values(Filter filter)} uses ConverterCollections to lazily deserialize results returned by the server.
     * <p>
     * {@link QueryMap#values(Filter, Comparator)} will return collection of already deserialized values
     */
    @Test
    public void valuesLazyDeserializationTest()
        {
        // skip cache types which eagerly deserialize values
        if (f_sCacheName.contains("view") || f_sCacheName.contains("local"))
            {
            return;
            }
        
        NamedCache<String, MyObject> cache = getNamedCache();

        Set<MyObject> setOne   = new HashSet();
        Set<MyObject> setTwo   = new HashSet();
        MyObject      valueOne = new MyObject(1);
        setOne.add(valueOne);
        setTwo.add(new MyObject(2));
        setTwo.add(new MyObject(3));

        for (int i = 1; i < 4; i++)
            {
            cache.put("key-" + i, new MyObject(i));
            }

        Filter<Integer>      filter = Filters.less(Extractors.extract("value"), 2);
        Collection<MyObject> colVal = cache.values(filter);

        assertThat(getDeserializationCount(), is(0));
        assertThat(colVal.contains(valueOne), is(true));
        assertThat(getDeserializationCount(), is(1));
        assertThat(colVal.size(), is(setOne.size()));
        assertThat(colVal.containsAll(setOne), is(true));
        assertThat(getDeserializationCount(), is(2));

        setDeserializationCount(0);

        filter = Filters.greater(Extractors.extract("value"), 1);
        colVal = cache.values(filter);

        assertThat(getDeserializationCount(), is(0));
        assertThat(colVal.size(), is(setTwo.size()));
        assertThat(colVal.containsAll(setTwo), is(true));
        assertThat(getDeserializationCount(), is(3));
        }

    /**
     * Test the behavior of {@link QueryMap#values(Filter)}.
     */
    @Test
    public void valuesFilterTest()
        {
        NamedCache<String, Integer> cache = getNamedCache();

        Set<Integer> setOne = new HashSet();
        Set<Integer> setTwo = new HashSet();
        setOne.add(1);
        setTwo.add(2);
        setTwo.add(3);

        for (int i = 1; i < 4; i++)
            {
            cache.put("key-" + i, i);
            }

        Filter<Integer>     filter = Filters.less(Extractors.identity(), 2);
        Collection<Integer> colVal = cache.values(filter);

        assertThat(colVal.size(), is(setOne.size()));
        assertThat(colVal.contains(1), is(true));
        assertThat(colVal.containsAll(setOne), is(true));

        filter = Filters.greater(Extractors.identity(), 1);
        colVal = cache.values(filter);

        assertThat(colVal.size(), is(setTwo.size()));
        assertThat(colVal.contains(2), is(true));
        assertThat(colVal.contains(3), is(true));
        assertThat(colVal.containsAll(setTwo), is(true));

        filter = Filters.less(Extractors.identity(), 1);
        colVal = cache.values(filter);

        assertThat(colVal.isEmpty(), is(true));
        }

    /**
     * Test the behavior of {@link QueryMap#values(Filter)} with a LimitFilter.
     */
    @Test
    public void valuesLimitFilterTest()
        {
        NamedCache<String, Integer> cache = getNamedCache();

        final int    PAGE_SIZE = 10;
        Set<Integer> set       = new HashSet();

        for (int i = 0; i < 10; i++)
            {
            set.add(i);
            }

        for (int i = 0; i < 100; i++)
            {
            cache.put("key-" + i, i);
            }

        Filter<Integer>     filter      = Filters.less(Extractors.identity(), 10);
        LimitFilter         filterLimit = new LimitFilter(filter, PAGE_SIZE);
        Collection<Integer> colVal      = cache.values(filterLimit);

        assertThat(colVal, notNullValue());
        assertThat(colVal.size(), is(PAGE_SIZE));
        assertThat(colVal.size(), is(set.size()));
        assertThat(colVal.containsAll(set), is(true));

        filter      = Filters.less(Extractors.identity(), 0);
        filterLimit = new LimitFilter(filter, PAGE_SIZE);
        colVal      = cache.values(filterLimit);

        assertThat(colVal.isEmpty(), is(true));
        }

    /**
     * Test the behavior of {@link QueryMap#values(Filter, Comparator)}.
     */
    @Test
    public void valuesFilterComparatorTest()
        {
        NamedCache<String, Integer> cache = getNamedCache();

        Set<Integer> setOne = new HashSet();
        Set<Integer> setTwo = new HashSet();
        setOne.add(1);
        setTwo.add(2);
        setTwo.add(3);

        for (int i = 1; i < 4; i++)
            {
            cache.put("key-" + i, i);
            }

        Comparator<Integer>  comparator = new IntegerComparator();
        Filter<Integer>      filter     = Filters.less(Extractors.identity(), 2);
        Collection<Integer>  colVal     = cache.values(filter, comparator);

        assertThat(colVal.size(), is(setOne.size()));
        assertThat(colVal.contains(1), is(true));
        assertThat(colVal.containsAll(setOne), is(true));

        filter = Filters.greater(Extractors.identity(), 1);
        colVal = cache.values(filter, comparator);

        assertThat(colVal.size(), is(setTwo.size()));
        assertThat(colVal.contains(2), is(true));
        assertThat(colVal.contains(3), is(true));
        assertThat(colVal.containsAll(setTwo), is(true));

        filter = Filters.less(Extractors.identity(), 1);
        colVal = cache.values(filter, comparator);

        assertThat(colVal.isEmpty(), is(true));
        }

    /**
     * Test the behavior of {@link QueryMap#values(Filter)} with a LimitFilter.
     */
    @Test
    public void valuesLimitFilterComparatorTest()
        {
        NamedCache<String, Integer> cache = getNamedCache();

        final int    PAGE_SIZE = 10;
        Set<Integer> set       = new HashSet();

        for (int i = 0; i < 10; i++)
            {
            set.add(i);
            }

        for (int i = 0; i < 100; i++)
            {
            cache.put("key-" + i, i);
            }

        Comparator<Integer> comparator  = new IntegerComparator();
        Filter<Integer>     filter      = Filters.less(Extractors.identity(), 10);
        LimitFilter         filterLimit = new LimitFilter(filter, PAGE_SIZE);
        Collection<Integer> colVal      = cache.values(filterLimit, comparator);

        assertThat(colVal, notNullValue());
        assertThat(colVal.size(), is(PAGE_SIZE));
        assertThat(colVal.size(), is(set.size()));
        assertThat(colVal.containsAll(set), is(true));

        filter      = Filters.less(Extractors.identity(), 0);
        filterLimit = new LimitFilter(filter, PAGE_SIZE);
        colVal      = cache.values(filterLimit, comparator);

        assertThat(colVal.isEmpty(), is(true));
        }

    /**
     * Test the behavior of {@link QueryMap#entrySet(Filter)}.
     */
    @Test
    public void entrySetFilterTest()
        {
        NamedCache<String, Integer> cache = getNamedCache();

        Set<SimpleMapEntry<String,Integer>> setOne = new HashSet();
        Set<SimpleMapEntry<String,Integer>> setTwo = new HashSet();
        setOne.add(new SimpleMapEntry<>("key-1", 1));
        setTwo.add(new SimpleMapEntry<>("key-2", 2));
        setTwo.add(new SimpleMapEntry<>("key-3", 3));

        for (int i = 1; i < 4; i++)
            {
            cache.put("key-" + i, i);
            }

        Filter<Integer>                 filter     = Filters.less(Extractors.identity(), 2);
        Set<Map.Entry<String, Integer>> setEntries = cache.entrySet(filter);

        assertThat(setEntries.size(), is(setOne.size()));
        assertThat(setEntries.containsAll(setOne), is(true));

        filter     = Filters.greater(Extractors.identity(), 1);
        setEntries = cache.entrySet(filter);

        assertThat(setEntries.size(), is(setTwo.size()));
        assertThat(setEntries.containsAll(setTwo), is(true));

        filter     = Filters.less(Extractors.identity(), 1);
        setEntries = cache.entrySet(filter);

        assertThat(setEntries.isEmpty(), is(true));
        }

    /**
     * Test the behavior of {@link QueryMap#entrySet(Filter)} with a LimitFilter.
     */
    @Test
    public void entrySetLimitFilterTest()
        {
        NamedCache<String, Integer> cache = getNamedCache();

        final int PAGE_SIZE = 10;

        Set<SimpleMapEntry<String,Integer>> set = new HashSet();

        for (int i = 0; i < 10; i++)
            {
            set.add(new SimpleMapEntry<>("key-" + i, i));
            }

        for (int i = 0; i < 100; i++)
            {
            cache.put("key-" + i, i);
            }

        Filter<Integer>                  filter      = Filters.less(Extractors.identity(), 10);
        LimitFilter                      filterLimit = new LimitFilter(filter, PAGE_SIZE);
        Set<Map.Entry<String, Integer>>  setEntries  = cache.entrySet(filterLimit);

        assertThat(setEntries, notNullValue());
        assertThat(setEntries.size(), is(PAGE_SIZE));
        assertThat(setEntries.size(), is(set.size()));
        assertThat(setEntries.containsAll(set), is(true));

        filter      = Filters.less(Extractors.identity(), 0);
        filterLimit = new LimitFilter(filter, PAGE_SIZE);
        setEntries  = cache.entrySet(filterLimit);

        assertThat(setEntries.isEmpty(), is(true));
        }

    /**
     * Test the behavior of {@link QueryMap#keySet(Filter)}.
     */
    @Test
    public void keySetFilterTest()
        {
        NamedCache<String, Integer> cache = getNamedCache();

        Set<String> setOne = new HashSet();
        Set<String> setTwo = new HashSet();
        setOne.add("key-1");
        setTwo.add("key-2");
        setTwo.add("key-3");

        for (int i = 1; i < 4; i++)
            {
            cache.put("key-" + i, i);
            }

        Filter<Integer> filter  = Filters.less(Extractors.identity(), 2);
        Set<String>     setKeys = cache.keySet(filter);

        assertThat(setKeys.size(), is(setOne.size()));
        assertThat(setKeys.containsAll(setOne), is(true));

        filter  = Filters.greater(Extractors.identity(), 1);
        setKeys = cache.keySet(filter);

        assertThat(setKeys.size(), is(setTwo.size()));
        assertThat(setKeys.containsAll(setTwo), is(true));

        filter  = Filters.less(Extractors.identity(), 1);
        setKeys = cache.keySet(filter);

        assertThat(setKeys.isEmpty(), is(true));
        }

    /**
     * Test the behavior of {@link QueryMap#entrySet(Filter)} with a LimitFilter.
     */
    @Test
    public void keySetLimitFilterTest()
        {
        NamedCache<String, Integer> cache = getNamedCache();

        final int   PAGE_SIZE = 10;
        Set<String> set       = new HashSet();

        for (int i = 0; i < 10; i++)
            {
            set.add("key-" + i);
            }

        for (int i = 0; i < 100; i++)
            {
            cache.put("key-" + i, i);
            }

        Filter<Integer> filter      = Filters.less(Extractors.identity(), 10);
        LimitFilter     filterLimit = new LimitFilter(filter, PAGE_SIZE);
        Set<String>     setKeys     = cache.keySet(filterLimit);

        assertThat(setKeys, notNullValue());
        assertThat(setKeys.size(), is(PAGE_SIZE));
        assertThat(setKeys.size(), is(set.size()));
        assertThat(setKeys.containsAll(set), is(true));

        filter      = Filters.less(Extractors.identity(), 0);
        filterLimit = new LimitFilter(filter, PAGE_SIZE);
        setKeys     = cache.keySet(filterLimit);

        assertThat(setKeys.isEmpty(), is(true));
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Total number of times a MyObject is deserialized.
     *
     * @return number of times MyObject is deserialized
     */
    public int getDeserializationCount()
        {
        return s_cDeserializationCount;
        }

    /**
     * Reset the number of times MyObject is deserialized
     *
     * @param cDeserializationCount  the deserialization count
     */
    public void setDeserializationCount(int cDeserializationCount)
        {
        s_cDeserializationCount = cDeserializationCount;
        }

    // ----- inner class: IntegerComparator ---------------------------------

    public static class IntegerComparator
            implements Comparator, ExternalizableLite
        {
        // ----- constructors -----------------------------------------------

        public IntegerComparator()
            {
            }

        // ----- Comparator interface ---------------------------------------

        public int compare(Object o1, Object o2)
            {
            if (o1 instanceof Map.Entry)
                {
                o1 = ((Map.Entry) o1).getValue();
                }
            if (o2 instanceof Map.Entry)
                {
                o2 = ((Map.Entry) o2).getValue();
                }
            return -((Integer) o1).compareTo((Integer) o2);
            }

        // ----- ExternalizableLite interface -------------------------------

        public void readExternal(DataInput in)
                throws IOException
            {
            }

        public void writeExternal(DataOutput out)
                throws IOException
            {
            }

        }

    // ----- inner class: MyObject ---------------------------------------

    public static class MyObject
            implements ExternalizableLite, Comparable
        {
        // ----- constructors --------------------------------------------

        public MyObject()
            {
            }

        public MyObject(int value)
            {
            m_nValue = value;
            }

        // ----- ExternalizableLite  interface -----------------------------

        public void readExternal(DataInput in) throws IOException
            {
            m_nValue = in.readInt();
            s_cDeserializationCount++;
            }

        public void writeExternal(DataOutput out) throws IOException
            {
            out.writeInt(m_nValue);
            }

        // ----- Comparable interface ------------------------------------

        public int compareTo(Object o)
            {
            MyObject that = (MyObject) o;

            if (m_nValue != that.m_nValue)
                {
                return m_nValue < that.m_nValue ? -1 : 1;
                }
            return 0;
            }

        // ----- Object methods ------------------------------------------

        @Override
        public boolean equals(Object o)
            {
            if (this == o)
                {
                return true;
                }
            if (o == null || getClass() != o.getClass())
                {
                return false;
                }
            MyObject that = (MyObject) o;
            return m_nValue == that.m_nValue;
            }

        @Override
        public int hashCode()
            {
            return Objects.hash(m_nValue);
            }

        // ----- helpers ----------------------------------------------

        public int getValue()
            {
            return m_nValue;
            }

        public void setValue(int value)
            {
            m_nValue = value;
            }

        // ----- data members ------------------------------------------

        protected int m_nValue;
        }

    // ----- data members --------------------------------------------------

    protected static ConfigurableCacheFactory s_ccf;

    private final String f_sDescription;

    private final String f_sCacheName;

    protected static int s_cDeserializationCount;
    }