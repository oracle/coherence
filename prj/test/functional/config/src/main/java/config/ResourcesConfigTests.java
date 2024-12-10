/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package config;

import com.tangosol.coherence.config.builder.NamedResourceBuilder;
import com.tangosol.coherence.config.builder.ParameterizedBuilder;
import com.tangosol.coherence.config.builder.ParameterizedBuilderRegistry;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.Cluster;
import com.tangosol.net.NamedCache;
import com.tangosol.net.OperationalContext;

import com.tangosol.net.cache.CacheLoader;
import com.tangosol.net.cache.LocalCache;

import com.oracle.coherence.testing.SystemPropertyIsolation;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import java.util.Map;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@SuppressWarnings("rawtypes")
public class ResourcesConfigTests
    {
    @BeforeClass
    public static void reset()
        {
        System.setProperty("coherence.override", "custom-resource-config.xml");
        System.setProperty("coherence.cacheconfig", "custom-cache-config.xml");
        System.setProperty("resource-name", "test-store");
        }

    @Test
    public void shouldHaveCustomResource()
        {
        Cluster                      cluster  = CacheFactory.getCluster();
        ParameterizedBuilderRegistry registry = ((OperationalContext) cluster).getBuilderRegistry();
        ParameterizedBuilder<NamedResourceBuilder> builder = registry.getBuilder(NamedResourceBuilder.class, "test");
        assertThat(builder, is(notNullValue()));

        Object oResource = builder.realize(null, null, null);
        assertThat(oResource, is(instanceOf(CustomResourceImpl.class)));
        }

    @Test
    public void shouldInjectCustomResourceFromIdAttribute()
        {
        String sCacheName = "one";
        NamedCache<Object, Object> cache = CacheFactory.getCache(sCacheName);

        Map map = cache.getCacheService().getBackingMapManager().getContext().getBackingMap(sCacheName);
        assertThat(map, is(instanceOf(LocalCache.class)));

        CacheLoader loader = ((LocalCache) map).getCacheLoader();
        assertThat(loader, is(instanceOf(CustomStore.class)));
        assertThat(((CustomStore) loader).getParam(), is(nullValue()));
        }

    @Test
    public void shouldInjectCustomResourceFromElementValue()
        {
        String sCacheName = "two";
        NamedCache<Object, Object> cache = CacheFactory.getCache(sCacheName);

        Map map = cache.getCacheService().getBackingMapManager().getContext().getBackingMap(sCacheName);
        assertThat(map, is(instanceOf(LocalCache.class)));

        CacheLoader loader = ((LocalCache) map).getCacheLoader();
        assertThat(loader, is(instanceOf(CustomStore.class)));
        assertThat(((CustomStore) loader).getParam(), is(nullValue()));
        }

    @Test
    public void shouldInjectCustomResourceFromSystemProperty()
        {
        String sCacheName = "three";
        NamedCache<Object, Object> cache = CacheFactory.getCache(sCacheName);

        Map map = cache.getCacheService().getBackingMapManager().getContext().getBackingMap(sCacheName);
        assertThat(map, is(instanceOf(LocalCache.class)));

        CacheLoader loader = ((LocalCache) map).getCacheLoader();
        assertThat(loader, is(instanceOf(CustomStore.class)));
        assertThat(((CustomStore) loader).getParam(), is(nullValue()));
        }

    @Test
    public void shouldInjectCustomResourceWithParams()
        {
        String sCacheName = "four";
        NamedCache<Object, Object> cache = CacheFactory.getCache(sCacheName);

        Map map = cache.getCacheService().getBackingMapManager().getContext().getBackingMap(sCacheName);
        assertThat(map, is(instanceOf(LocalCache.class)));

        CacheLoader loader = ((LocalCache) map).getCacheLoader();
        assertThat(loader, is(instanceOf(CustomStore.class)));
        assertThat(((CustomStore) loader).getParam(), is("Four"));
        }

    @Test
    public void shouldInjectStaticCustomResource()
        {
        String sCacheName = "five";
        NamedCache<Object, Object> cache = CacheFactory.getCache(sCacheName);

        Map map = cache.getCacheService().getBackingMapManager().getContext().getBackingMap(sCacheName);
        assertThat(map, is(instanceOf(LocalCache.class)));

        CacheLoader loader = ((LocalCache) map).getCacheLoader();
        assertThat(loader, is(instanceOf(CustomStore.class)));
        assertThat(((CustomStore) loader).getParam(), is(nullValue()));
        }

    @Test
    public void shouldInjectStaticCustomResourceWithParams()
        {
        String sCacheName = "six";
        NamedCache<Object, Object> cache = CacheFactory.getCache(sCacheName);

        Map map = cache.getCacheService().getBackingMapManager().getContext().getBackingMap(sCacheName);
        assertThat(map, is(instanceOf(LocalCache.class)));

        CacheLoader loader = ((LocalCache) map).getCacheLoader();
        assertThat(loader, is(instanceOf(CustomStore.class)));
        assertThat(((CustomStore) loader).getParam(), is("Six"));
        }

    @Test
    public void shouldInjectStaticCustomResourceWithParamsFromOverride()
        {
        String sCacheName = "seven";
        NamedCache<Object, Object> cache = CacheFactory.getCache(sCacheName);

        Map map = cache.getCacheService().getBackingMapManager().getContext().getBackingMap(sCacheName);
        assertThat(map, is(instanceOf(LocalCache.class)));

        CacheLoader loader = ((LocalCache) map).getCacheLoader();
        assertThat(loader, is(instanceOf(CustomStore.class)));
        assertThat(((CustomStore) loader).getParam(), is("foo"));
        }

    @Test
    public void shouldInjectStaticCustomResourceOverridingParamsFromOverride()
        {
        String sCacheName = "eight";
        NamedCache<Object, Object> cache = CacheFactory.getCache(sCacheName);

        Map map = cache.getCacheService().getBackingMapManager().getContext().getBackingMap(sCacheName);
        assertThat(map, is(instanceOf(LocalCache.class)));

        CacheLoader loader = ((LocalCache) map).getCacheLoader();
        assertThat(loader, is(instanceOf(CustomStore.class)));
        assertThat(((CustomStore) loader).getParam(), is("Eight"));
        }

    // ----- data members ---------------------------------------------------

    /**
     * A {@link org.junit.Rule} to isolate system properties set between test executions.
     */
    @Rule
    public SystemPropertyIsolation s_systemPropertyIsolation = new SystemPropertyIsolation();
    }
