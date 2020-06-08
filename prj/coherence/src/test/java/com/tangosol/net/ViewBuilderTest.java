/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net;

import com.tangosol.net.NamedCache;
import com.tangosol.net.ViewBuilder;

import com.tangosol.net.cache.ContinuousQueryCache;
import com.tangosol.net.cache.WrapperNamedCache;
import com.tangosol.util.MapEvent;
import com.tangosol.util.MapListener;
import com.tangosol.util.ValueExtractor;

import com.tangosol.util.extractor.IdentityExtractor;

import com.tangosol.util.filter.AlwaysFilter;
import com.tangosol.util.filter.EqualsFilter;

import java.util.HashMap;

import org.junit.Test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

import static org.junit.Assert.assertThat;

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
                new ViewBuilder<String, String, String>(() -> getCache("defaults")).build();
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
        NamedCache<String, String> view      = backCache.<String>view().build();

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
        NamedCache<String, String>   cache  = new ViewBuilder<String, String, String>(
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
        NamedCache<String, String>  cache    = new ViewBuilder<String, String, String>(
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
        NamedCache<String, String>     cache       = new ViewBuilder<String, String, String>(
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
        NamedCache<String, String> cache = new ViewBuilder<String, String, String>(
                getCache("cachingValues")).values().build();

        assertThat(cache, is(instanceOf(ContinuousQueryCache.class)));

        ContinuousQueryCache<String, String, String> queryCache =
                (ContinuousQueryCache<String, String, String>) cache;

        assertThat(queryCache.isCacheValues(), is(true));
        }

    @Test
    public void testViewBuilderKeysOnly()
        {
        NamedCache<String, String> cache = new ViewBuilder<String, String, String>(
                getCache("keysOnly")).keys().build();

        assertThat(cache, is(instanceOf(ContinuousQueryCache.class)));

        ContinuousQueryCache<String, String, String> queryCache =
                (ContinuousQueryCache<String, String, String>) cache;

        assertThat(queryCache.isCacheValues(), is(false));
        }

    // ----- inner class : TestListener -------------------------------------

    private static final class TestListener<K, V> implements MapListener<K, V>
        {
        // ----- MapListener methods ----------------------------------------

        public void entryInserted(MapEvent evt)
            {

            }

        public void entryUpdated(MapEvent evt)
            {

            }

        public void entryDeleted(MapEvent evt)
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
    }
