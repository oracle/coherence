/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net;

import com.tangosol.net.cache.ContinuousQueryCache;
import com.tangosol.net.cache.WrapperNamedCache;

import com.tangosol.util.MapEvent;
import com.tangosol.util.MapListener;
import com.tangosol.util.ValueExtractor;

import com.tangosol.util.extractor.IdentityExtractor;

import com.tangosol.util.filter.AlwaysFilter;
import com.tangosol.util.filter.EqualsFilter;

import java.util.Comparator;
import java.util.HashMap;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

/**
 * Unit tests for {@link ViewBuilder}.
 *
 * @author rl 5.23.19
 * @since 12.2.1.4
 */
public class ViewBuilderTest
    {
    // ----- test methods ---------------------------------------------------

    @Test
    public void testViewBuilderDefaults()
        {
        NamedCache<String, String> cache =
                new ViewBuilder<String, String>(() -> getCache("defaults")).build();
        assertThat(cache, is(instanceOf(ContinuousQueryCache.class)));

        ContinuousQueryCache<String, String, String> queryCache =
                (ContinuousQueryCache<String, String, String>) cache;

        assertThat(queryCache.getFilter(),            is(AlwaysFilter.INSTANCE));
        assertThat(queryCache.isReadOnly(),           is(false));
        assertThat(queryCache.getMapListener(),       is(nullValue()));
        assertThat(queryCache.getTransformer(),       is(nullValue()));
        assertThat(queryCache.getReconnectInterval(), is(0L));
        assertThat(queryCache.getCacheName(),         containsString("ContinuousQueryCache"));
        }

    @Test
    public void testViewFromNamedCache()
        {
        NamedCache<String, String> backCache = getCache("fromNamedCache");
        NamedCache<String, String> view      = backCache.view().build();

        assertThat(view, is(instanceOf(ContinuousQueryCache.class)));

        ContinuousQueryCache<String, String, String> queryCache =
                (ContinuousQueryCache<String, String, String>) view;

        assertThat(queryCache.getFilter(),            is(AlwaysFilter.INSTANCE));
        assertThat(queryCache.isReadOnly(),           is(false));
        assertThat(queryCache.getMapListener(),       is(nullValue()));
        assertThat(queryCache.getTransformer(),       is(nullValue()));
        assertThat(queryCache.getReconnectInterval(), is(0L));
        assertThat(queryCache.getCacheName(),         containsString("ContinuousQueryCache"));
        assertThat(queryCache.isCacheValues(),        is(true));
        }

    @Test
    public void testViewBuilderWithFilter()
        {
        EqualsFilter<String, String> filter = new EqualsFilter<>("foo", "bar");
        NamedCache<String, String>   cache  = new ViewBuilder<String, String>(
                getCache("filter")).filter(filter).build();

        assertThat(cache, is(instanceOf(ContinuousQueryCache.class)));

        ContinuousQueryCache<String, String, String> queryCache =
                (ContinuousQueryCache<String, String, String>) cache;

        assertThat(queryCache.getFilter(), is(filter));
        }

    @Test
    public void testViewBuilderWithListener()
        {
        MapListener<String, String> listener = new TestListener<>();
        NamedCache<String, String>  cache    = new ViewBuilder<String, String>(
                getCache("listener")).listener(listener).build();

        assertThat(cache, is(instanceOf(ContinuousQueryCache.class)));

        ContinuousQueryCache<String, String, String> queryCache =
                (ContinuousQueryCache<String, String, String>) cache;

        assertThat(queryCache.getMapListener(), is(listener));
        }

    @Test
    public void testViewBuilderWithTransformer()
        {
        ValueExtractor<String, String> transformer = new IdentityExtractor<>();
        NamedCache<String, String>     cache       = new ViewBuilder<String, String>(
                getCache("transformer")).map(transformer).build();

        assertThat(cache, is(instanceOf(ContinuousQueryCache.class)));

        ContinuousQueryCache<String, String, String> queryCache =
                (ContinuousQueryCache<String, String, String>) cache;

        assertThat(queryCache.getTransformer(), is(transformer));
        assertThat(queryCache.isReadOnly(),     is(true));
        }

    @Test
    public void testViewBuilderCacheValues()
        {
        NamedCache<String, String> cache = new ViewBuilder<String, String>(
                getCache("cachingValues")).values().build();

        assertThat(cache, is(instanceOf(ContinuousQueryCache.class)));

        ContinuousQueryCache<String, String, String> queryCache =
                (ContinuousQueryCache<String, String, String>) cache;

        assertThat(queryCache.isCacheValues(), is(true));
        }

    @Test
    public void testViewBuilderKeysOnly()
        {
        NamedCache<String, String> cache = new ViewBuilder<String, String>(
                getCache("keysOnly")).keys().build();

        assertThat(cache, is(instanceOf(ContinuousQueryCache.class)));

        ContinuousQueryCache<String, String, String> queryCache =
                (ContinuousQueryCache<String, String, String>) cache;

        assertThat(queryCache.isCacheValues(), is(false));
        }

    @Test
    public void testViewBuilderMapFunction()
        {
        NamedCache<String, Data> underlying = getCache("cachingValues");
        underlying.put("1", new Data("A", 42));

        NamedCache<String, String> cacheString =
                new ViewBuilder<>(underlying)
                        .map(Data::getString).build();


        NamedCache<String, Integer> cacheInt =
                new ViewBuilder<>(underlying)
                        .map(Data::getInteger).build();


        assertThat(cacheString.get("1"), is("A"));
        assertThat(cacheInt.get("1"),    is(42));
        }

    @Test
    public void testViewBuilderSorted()
        {
        NamedCache<Integer, Data> underlying = getCache("cachingValues");
        underlying.put(1, new Data("A", 42));
        underlying.put(2, new Data("B", 32));
        underlying.put(3, new Data("C", 22));
        underlying.put(4, new Data("D", 12));

        NamedCache<Integer, Data> viewData =
                new ViewBuilder<>(underlying)
                        .sorted(Comparator.comparing(Data::getString)).build();
        assertThat(viewData.keySet(), contains(1, 2, 3, 4));

        NamedCache<Integer, Integer> viewInt =
                new ViewBuilder<>(underlying)
                        .map(Data::getInteger)
                        .sorted()
                        .build();
        assertThat(viewInt.keySet(), contains(4, 3, 2, 1));
        assertThat(viewInt.values(), contains(12, 22, 32, 42));
        }

    // ----- inner class : TestListener -------------------------------------

    private static final class TestListener<K, V> implements MapListener<K, V>
        {
        // ----- MapListener methods ----------------------------------------

        public void entryInserted(MapEvent<K, V> evt)
            {

            }

        public void entryUpdated(MapEvent<K, V> evt)
            {

            }

        public void entryDeleted(MapEvent<K, V> evt)
            {

            }

        // ----- Object methods ---------------------------------------------

        public int hashCode()
            {
            return super.hashCode();
            }

        public boolean equals(Object obj)
            {
            return obj instanceof TestListener;
            }
        }

    // ----- helper methods -------------------------------------------------

    protected <K, V> NamedCache<K, V> getCache(String sName)
        {
        return new WrapperNamedCache<>(new HashMap<>(), sName);
        }

    // ---- inner class: Data -----------------------------------------------

    public static class Data
        {
        // ----- constructors ----------------------------------------------

        public Data(String sString, Integer nInteger)
            {
            m_sString = sString;
            m_nInteger = nInteger;
            }

        // ----- accessors --------------------------------------------------

        public String getString()
            {
            return m_sString;
            }

        public Integer getInteger()
            {
            return m_nInteger;
            }

        // ----- data members ----------------------------------------------

        protected String m_sString;
        protected Integer m_nInteger;
        }
    }
