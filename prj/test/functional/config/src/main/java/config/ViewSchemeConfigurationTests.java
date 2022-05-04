/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package config;

import com.tangosol.net.CacheService;
import com.tangosol.net.NamedCache;

import com.tangosol.net.cache.ContinuousQueryCache;

import com.tangosol.util.MapEvent;
import com.tangosol.util.MapListener;

import com.tangosol.util.extractor.IdentityExtractor;
import com.tangosol.util.extractor.UniversalExtractor;

import com.tangosol.util.filter.AlwaysFilter;
import com.tangosol.util.filter.GreaterFilter;
import com.tangosol.util.filter.NeverFilter;

import com.oracle.coherence.testing.AbstractFunctionalTest;

import java.util.Objects;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertThat;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.nullValue;

/**
 * Unit tests to validate the {@link ContinuousQueryCache} returned by the cache factory.
 *
 * @author rlubke
 * @since 12.2.1.4
 */
public class ViewSchemeConfigurationTests
        extends AbstractFunctionalTest
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor.
     */
    public ViewSchemeConfigurationTests()
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

        AbstractFunctionalTest._startup();
        }

    // ----- test methods ---------------------------------------------------

    /**
     * Test {@code view-scheme} with a custom {@link com.tangosol.util.Filter} class.
     */
    @Test
    public void shouldUseCustomFilter()
        {
        ContinuousQueryCache cache = validateIsCqc(getNamedCache("view-filter"));
        assertThat(cache.getFilter(), is(NeverFilter.INSTANCE));
        assertThat(cache.isTransformed(), is(false));
        assertThat(cache.getTransformer(), is(nullValue()));
        assertThat(cache.getMapListener(), is(nullValue()));
        assertThat(cache.getReconnectInterval(), is(0L));
        assertThat(cache.isReadOnly(), is(false));
        assertThat(cache.isCacheValues(), is(true));
        }

    /**
     * Test {@code view-scheme} with a custom {@link com.tangosol.util.Filter} class with init params.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void shouldUseCustomFilterWithInitParams()
        {
        ContinuousQueryCache cache = validateIsCqc(getNamedCache("view-filter-with-params"));
        assertThat(cache.getFilter(), is(new GreaterFilter("foo", 10)));
        assertThat(cache.isTransformed(), is(false));
        assertThat(cache.getTransformer(), is(nullValue()));
        assertThat(cache.getMapListener(), is(nullValue()));
        assertThat(cache.getReconnectInterval(), is(0L));
        assertThat(cache.isReadOnly(), is(false));
        assertThat(cache.isCacheValues(), is(true));
        }

    /**
     * Test {@code view-scheme} using a custom transformer.
     */
    @Test
    public void shouldUseCustomTransformer()
        {
        ContinuousQueryCache cache = validateIsCqc(getNamedCache("view-transformer"));
        assertThat(cache.getFilter(), is(AlwaysFilter.INSTANCE));
        assertThat(cache.isTransformed(), is(true));
        assertThat(cache.getTransformer(), is(IdentityExtractor.INSTANCE));
        assertThat(cache.getMapListener(), is(nullValue()));
        assertThat(cache.getReconnectInterval(), is(0L));
        assertThat(cache.isReadOnly(), is(true)); // use of a transformer overrides xml configuration
        assertThat(cache.isCacheValues(), is(true));
        }

    /**
     * Test {@code view-scheme} using a custom transformer with init params.
     */
    @Test
    public void shouldUseCustomTransformerWithInitParams()
        {
        ContinuousQueryCache cache = validateIsCqc(getNamedCache("view-transformer-with-params"));
        assertThat(cache.getFilter(), is(AlwaysFilter.INSTANCE));
        assertThat(cache.isTransformed(), is(true));
        assertThat(cache.getTransformer(), is(new UniversalExtractor("foo")));
        assertThat(cache.getMapListener(), is(nullValue()));
        assertThat(cache.getReconnectInterval(), is(0L));
        assertThat(cache.isReadOnly(), is(true)); // use of a transformer overrides xml configuration
        assertThat(cache.isCacheValues(), is(true));
        }

    /**
     * Test {@code view-scheme} configuring {@code read-only}.
     */
    @Test
    public void shouldUseConfiguredReadOnly()
        {
        ContinuousQueryCache cache = validateIsCqc(getNamedCache("view-read-only"));
        assertThat(cache.getFilter(), is(AlwaysFilter.INSTANCE));
        assertThat(cache.isTransformed(), is(false));
        assertThat(cache.getTransformer(), is(nullValue()));
        assertThat(cache.getMapListener(), is(nullValue()));
        assertThat(cache.getReconnectInterval(), is(0L));
        assertThat(cache.isReadOnly(), is(true));
        assertThat(cache.isCacheValues(), is(true));
        }

    /**
     * Test {@code view-scheme} configuring {@code cache-values}.
     */
    @Test
    public void shouldUseConfiguredCacheValues()
        {
        ContinuousQueryCache cache = validateIsCqc(getNamedCache("view-keys-only"));
        assertThat(cache.getFilter(), is(AlwaysFilter.INSTANCE));
        assertThat(cache.isTransformed(), is(false));
        assertThat(cache.getTransformer(), is(nullValue()));
        assertThat(cache.getMapListener(), is(nullValue()));
        assertThat(cache.getReconnectInterval(), is(0L));
        assertThat(cache.isReadOnly(), is(false));
        assertThat(cache.isCacheValues(), is(false));
        }

    /**
     * Test {@code view-scheme} configuring {@code reconnect-interval}.
     */
    @Test
    public void shouldUseConfiguredReconnectInterval()
        {
        ContinuousQueryCache cache = validateIsCqc(getNamedCache("view-reconnect-interval"));
        assertThat(cache.getFilter(), is(AlwaysFilter.INSTANCE));
        assertThat(cache.isTransformed(), is(false));
        assertThat(cache.getTransformer(), is(nullValue()));
        assertThat(cache.getMapListener(), is(nullValue()));
        assertThat(cache.getReconnectInterval(), is(1000L));
        assertThat(cache.isReadOnly(), is(false));
        assertThat(cache.isCacheValues(), is(true));
        }

    /**
     * Test {@code view-scheme} using a custom {@link MapListener}.
     */
    @Test
    public void shouldUseConfiguredListener()
        {
        ContinuousQueryCache cache = validateIsCqc(getNamedCache("view-with-listener"));
        assertThat(cache.getFilter(), is(AlwaysFilter.INSTANCE));
        assertThat(cache.isTransformed(), is(false));
        assertThat(cache.getTransformer(), is(nullValue()));
        assertThat(cache.getMapListener(), is(new TestListener()));
        assertThat(cache.getReconnectInterval(), is(0L));
        assertThat(cache.isReadOnly(), is(false));
        assertThat(cache.isCacheValues(), is(true));
        }

    /**
     * Test {@code view-scheme} with a custom {@link com.tangosol.util.Filter} class with init params and macros.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void shouldUseCustomFilterWithInitParamsAndMacro()
        {
        ContinuousQueryCache cache = validateIsCqc(getNamedCache("view-with-macro"));
        assertThat(cache.getFilter(), is(new GreaterFilter("foo", 50)));
        assertThat(cache.isTransformed(), is(false));
        assertThat(cache.getTransformer(), is(nullValue()));
        assertThat(cache.getMapListener(), is(nullValue()));
        assertThat(cache.getReconnectInterval(), is(0L));
        assertThat(cache.isReadOnly(), is(false));
        assertThat(cache.isCacheValues(), is(true));
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Assert that the provided {@link NamedCache} is using the expected service type and is an
     * instance of {@link ContinuousQueryCache}.
     *
     * @param cache  the {@link NamedCache} to validate
     *
     * @return the validated {@code cache} cast as {@link ContinuousQueryCache}
     */
    protected ContinuousQueryCache validateIsCqc(NamedCache cache)
        {
        assertThat(cache.getCacheService().getInfo().getServiceType(), is(CacheService.TYPE_DISTRIBUTED));
        assertThat(cache, instanceOf(ContinuousQueryCache.class));
        return (ContinuousQueryCache) cache;
        }

    // ----- inner class: TestListener --------------------------------------

    public static final class TestListener implements MapListener
        {
        public void entryInserted(MapEvent evt)
            {
            }

        public void entryUpdated(MapEvent evt)
            {
            }

        public void entryDeleted(MapEvent evt)
            {
            }

        @Override
        public boolean equals(Object o)
            {
            return o.getClass().equals(this.getClass());
            }


        @Override
        public int hashCode()
            {
            return Objects.hash();
            }
        }

    // ----- constants ------------------------------------------------------

    /**
     * The file name of the default cache configuration file used by this test.
     */
    public static String FILE_CFG_CACHE = "view-scheme-cache-config.xml";
    }
