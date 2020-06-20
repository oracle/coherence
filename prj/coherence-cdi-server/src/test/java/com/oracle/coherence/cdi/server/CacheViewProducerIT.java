/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.cdi.server;

import com.oracle.coherence.cdi.AlwaysFilter;
import com.oracle.coherence.cdi.ChainedExtractor;
import com.oracle.coherence.cdi.CoherenceExtension;
import com.oracle.coherence.cdi.ExtractorProducer;
import com.oracle.coherence.cdi.FilterProducer;
import com.oracle.coherence.cdi.Name;
import com.oracle.coherence.cdi.PropertyExtractor;
import com.oracle.coherence.cdi.Scope;
import com.oracle.coherence.cdi.View;
import com.oracle.coherence.cdi.WhereFilter;

import com.oracle.coherence.cdi.server.data.Person;
import com.oracle.coherence.cdi.server.data.PhoneNumber;

import com.tangosol.net.NamedCache;
import com.tangosol.net.cache.CacheMap;
import com.tangosol.net.cache.ContinuousQueryCache;
import com.tangosol.util.ConcurrentMap;
import com.tangosol.util.Filters;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.ObservableMap;
import com.tangosol.util.QueryMap;

import java.lang.annotation.Annotation;

import java.time.LocalDate;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;

import javax.inject.Inject;

import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldSetup;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Integration test for the CQC producers using the Weld JUnit extension.
 *
 * @author Jonathan Knight  2019.10.24
 */
@SuppressWarnings("rawtypes")
@ExtendWith(WeldJunit5Extension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CacheViewProducerIT
    {

    @WeldSetup
    private WeldInitiator weld = WeldInitiator.of(WeldInitiator.createWeld()
                                                          .addBeanClass(CtorBean.class)
                                                          .addBeanClass(ContinuousQueryCacheFieldsBean.class)
                                                          .addBeanClass(SuperTypesBean.class)
                                                          .addBeanClass(DifferentCacheFactoryBean.class)
                                                          .addBeanClass(ContinuousQueryCacheWithFiltersBean.class)
                                                          .addBeanClass(WithTransformersBean.class)
                                                          .addBeanClass(NamedCacheProducer.class)
                                                          .addBeanClass(FilterProducer.class)
                                                          .addBeanClass(FilterProducer.AlwaysFilterSupplier.class)
                                                          .addBeanClass(FilterProducer.WhereFilterSupplier.class)
                                                          .addBeanClass(ExtractorProducer.class)
                                                          .addBeanClass(ExtractorProducer.UniversalExtractorSupplier.class)
                                                          .addBeanClass(ExtractorProducer.UniversalExtractorsSupplier.class)
                                                          .addBeanClass(ExtractorProducer.ChainedExtractorSupplier.class)
                                                          .addBeanClass(ExtractorProducer.ChainedExtractorsSupplier.class)
                                                          .addBeanClass(ExtractorProducer.PofExtractorSupplier.class)
                                                          .addBeanClass(ExtractorProducer.PofExtractorsSupplier.class)
                                                          .addBeanClass(CacheFactoryUriResolver.Default.class)
                                                          .addBeanClass(ConfigurableCacheFactoryProducer.class)
                                                          .addExtension(new CoherenceExtension())
                                                          .addExtension(new CoherenceServerExtension()));

    @BeforeAll
    static void setup()
        {
        System.setProperty("coherence.distributed.localstorage", "true");
        System.setProperty("coherence.ttl", "0");
        }

    @Test
    void shouldGetDynamicContinuousQueryCache()
        {
        Annotation cache = Name.Literal.of("numbers");
        Annotation cacheView = View.Literal.INSTANCE;
        Instance<ContinuousQueryCache> instance = weld.select(ContinuousQueryCache.class, cache, cacheView);

        assertThat(instance.isResolvable(), is(true));
        ContinuousQueryCache cqc = instance.get();
        assertThat(cqc, is(instanceOf(ContinuousQueryCache.class)));
        assertThat(cqc.getCache().getCacheName(), is("numbers"));
        }

    @Test
    void shouldInjectContinuousQueryCacheUsingFieldName()
        {
        ContinuousQueryCacheFieldsBean bean = weld.select(ContinuousQueryCacheFieldsBean.class).get();
        assertThat(bean.getNumbers(), is(notNullValue()));
        assertThat(bean.getNumbers(), is(instanceOf(ContinuousQueryCache.class)));
        assertThat(bean.getNumbers().getCache().getCacheName(), is("numbers"));
        }

    @Test
    void shouldInjectQualifiedNamedCache()
        {
        ContinuousQueryCacheFieldsBean bean = weld.select(ContinuousQueryCacheFieldsBean.class).get();
        assertThat(bean.getNamedCache(), is(notNullValue()));
        assertThat(bean.getNamedCache(), is(instanceOf(ContinuousQueryCache.class)));
        assertThat(bean.getNamedCache().getCache().getCacheName(), is("numbers"));
        }

    @Test
    void shouldInjectContinuousQueryCacheWithGenerics()
        {
        ContinuousQueryCacheFieldsBean bean = weld.select(ContinuousQueryCacheFieldsBean.class).get();
        assertThat(bean.getGenericCache(), is(notNullValue()));
        assertThat(bean.getGenericCache(), is(instanceOf(ContinuousQueryCache.class)));
        assertThat(bean.getGenericCache().getCache().getCacheName(), is("numbers"));
        }

    @Test
    void shouldInjectContinuousQueryCacheWithGenericKeys()
        {
        ContinuousQueryCacheFieldsBean bean = weld.select(ContinuousQueryCacheFieldsBean.class).get();
        assertThat(bean.getGenericKeys(), is(notNullValue()));
        assertThat(bean.getGenericKeys(), is(instanceOf(ContinuousQueryCache.class)));
        assertThat(bean.getGenericKeys().getCache().getCacheName(), is("genericKeys"));
        }

    @Test
    void shouldInjectContinuousQueryCacheWithGenericValues()
        {
        ContinuousQueryCacheFieldsBean bean = weld.select(ContinuousQueryCacheFieldsBean.class).get();
        assertThat(bean.getGenericValues(), is(notNullValue()));
        assertThat(bean.getGenericValues(), is(instanceOf(ContinuousQueryCache.class)));
        assertThat(bean.getGenericValues().getCache().getCacheName(), is("genericValues"));
        }

    @Test
    void shouldInjectCachesFromDifferentCacheFactories()
        {
        DifferentCacheFactoryBean bean = weld.select(DifferentCacheFactoryBean.class).get();

        assertThat(bean.getDefaultCcfNumbers(), is(notNullValue()));
        assertThat(bean.getDefaultCcfNumbers(), is(instanceOf(ContinuousQueryCache.class)));
        assertThat(bean.getDefaultCcfNumbers().getCache().getCacheName(), is("numbers"));

        assertThat(bean.getSpecificCcfNumbers(), is(notNullValue()));
        assertThat(bean.getSpecificCcfNumbers(), is(instanceOf(ContinuousQueryCache.class)));
        assertThat(bean.getSpecificCcfNumbers().getCache().getCacheName(), is("numbers"));

        assertThat(bean.getDefaultCcfNumbers().getCache().getCacheService(),
                                 is(not(bean.getSpecificCcfNumbers().getCache().getCacheService())));
        }

    @Test
    void testCtorInjection()
        {
        CtorBean bean = weld.select(CtorBean.class).get();

        assertThat(bean.getNumbers(), notNullValue());
        assertThat(bean.getNumbers(), is(instanceOf(ContinuousQueryCache.class)));
        assertThat(bean.getNumbers().getCache().getCacheName(), is("numbers"));
        }

    @Test
    void shouldInjectSuperTypeContinuousQueryCache()
        {
        SuperTypesBean bean = weld.select(SuperTypesBean.class).get();
        ContinuousQueryCache cache = bean.getContinuousQueryCache();
        assertThat(cache, is(notNullValue()));
        assertThat(cache, is(sameInstance(bean.getContinuousQueryCache())));
        }

    @Test
    void shouldInjectSuperTypeNamedCache()
        {
        SuperTypesBean bean = weld.select(SuperTypesBean.class).get();
        NamedCache cache = bean.getNamedCache();
        assertThat(cache, is(notNullValue()));
        }

    @Test
    void shouldInjectSuperTypeInvocableMap()
        {
        SuperTypesBean bean = weld.select(SuperTypesBean.class).get();
        InvocableMap map = bean.getInvocableMap();
        assertThat(map, is(notNullValue()));
        }

    @Test
    void shouldInjectSuperTypeObservableMap()
        {
        SuperTypesBean bean = weld.select(SuperTypesBean.class).get();
        ObservableMap map = bean.getObservableMap();
        assertThat(map, is(notNullValue()));
        }

    @Test
    void shouldInjectSuperTypeConcurrentMap()
        {
        SuperTypesBean bean = weld.select(SuperTypesBean.class).get();
        ConcurrentMap map = bean.getConcurrentMap();
        assertThat(map, is(notNullValue()));
        }

    @Test
    void shouldInjectSuperTypeQueryMap()
        {
        SuperTypesBean bean = weld.select(SuperTypesBean.class).get();
        QueryMap map = bean.getQueryMap();
        assertThat(map, is(notNullValue()));
        }

    @Test
    void shouldInjectSuperTypeCacheMap()
        {
        SuperTypesBean bean = weld.select(SuperTypesBean.class).get();
        CacheMap map = bean.getCacheMap();
        assertThat(map, is(notNullValue()));
        }

    @Test
    void shouldInjectContinuousQueryCacheWithFilters()
        {
        ContinuousQueryCacheWithFiltersBean withFilters = weld.select(ContinuousQueryCacheWithFiltersBean.class).get();
        NamedCache<String, Person> cache = withFilters.getCache();
        ContinuousQueryCache<String, Person, Person> always = withFilters.getAlways();
        ContinuousQueryCache<String, Person, Person> foo = withFilters.getFoo();

        // populate the underlying cache
        populate(cache);
        assertThat(always.size(), is(cache.size()));

        Set<Map.Entry<String, Person>> entries = cache.entrySet(Filters.equal("lastName", "foo"));
        assertThat(foo.size(), is(entries.size()));
        for (Map.Entry<String, Person> entry : entries)
            {
            assertThat(foo.get(entry.getKey()), is(entry.getValue()));
            }
        }

    @Test
    void shouldInjectContinuousQueryCacheWithTransformer()
        {
        WithTransformersBean bean = weld.select(WithTransformersBean.class).get();
        NamedCache<String, Person> cache = bean.getNamedCache();
        ContinuousQueryCache<String, Person, String> names = bean.getNames();

        // populate the underlying cache
        populate(cache);

        assertThat(names.size(), is(cache.size()));
        for (Map.Entry<String, Person> entry : cache.entrySet())
            {
            assertThat(names.get(entry.getKey()), is(entry.getValue().getFirstName()));
            }
        }

    @Test
    void shouldInjectContinuousQueryCacheWithTransformerAndFilter()
        {
        WithTransformersBean bean = weld.select(WithTransformersBean.class).get();
        NamedCache<String, Person> cache = bean.getNamedCache();
        ContinuousQueryCache<String, Person, String> filtered = bean.getFilteredNames();

        // populate the underlying cache
        populate(cache);

        Set<Map.Entry<String, Person>> entries = cache.entrySet(Filters.equal("lastName", "foo"));
        assertThat(filtered.size(), is(entries.size()));
        for (Map.Entry<String, Person> entry : entries)
            {
            assertThat(filtered.get(entry.getKey()), is(entry.getValue().getPhoneNumber().getNumber()));
            }
        }

    @Test
    void shouldInjectContinuousQueryCacheWithKeysOnly()
        {
        WithTransformersBean bean = weld.select(WithTransformersBean.class).get();
        NamedCache<String, Person> cache = bean.getNamedCache();
        ContinuousQueryCache<String, Person, String> keysOnly = bean.getKeysOnly();

        // populate the underlying cache
        populate(cache);

        assertThat(keysOnly.size(), is(cache.size()));
        assertThat(keysOnly.isCacheValues(), is(false));
        }

    private void populate(NamedCache<String, Person> cache)
        {
        for (int i = 0; i < 100; i++)
            {
            String lastName = (i % 2 == 0) ? "foo" : "bar";
            Person bean = new Person(String.valueOf(i),
                                     lastName,
                                     LocalDate.now(),
                                     new PhoneNumber(44, "12345" + i));

            cache.put(lastName + "-" + i, bean);
            }
        }

    // ----- test beans -----------------------------------------------------

    @ApplicationScoped
    private static class ContinuousQueryCacheFieldsBean
        {
        @Inject
        private ContinuousQueryCache numbers;

        @Inject
        @Name("numbers")
        @View
        private ContinuousQueryCache namedCache;

        @Inject
        @Name("numbers")
        @View
        private ContinuousQueryCache<Integer, String, String> genericCache;

        @Inject
        @View
        private ContinuousQueryCache<List<String>, String, String> genericKeys;

        @Inject
        @View
        private ContinuousQueryCache<String, List<String>, String> genericValues;

        public ContinuousQueryCache getNumbers()
            {
            return numbers;
            }

        public ContinuousQueryCache getNamedCache()
            {
            return namedCache;
            }

        public ContinuousQueryCache<Integer, String, String> getGenericCache()
            {
            return genericCache;
            }

        public ContinuousQueryCache<List<String>, String, String> getGenericKeys()
            {
            return genericKeys;
            }

        public ContinuousQueryCache<String, List<String>, String> getGenericValues()
            {
            return genericValues;
            }
        }

    @ApplicationScoped
    private static class ContinuousQueryCacheWithFiltersBean
        {
        @Inject
        private NamedCache<String, Person> beans;

        @Inject
        @AlwaysFilter
        @Name("beans")
        @View
        private ContinuousQueryCache<String, Person, Person> always;

        @Inject
        @WhereFilter("lastName = 'foo'")
        @Name("beans")
        @View
        private ContinuousQueryCache<String, Person, Person> foo;

        public NamedCache<String, Person> getCache()
            {
            return beans;
            }

        public ContinuousQueryCache<String, Person, Person> getAlways()
            {
            return always;
            }

        public ContinuousQueryCache<String, Person, Person> getFoo()
            {
            return foo;
            }
        }

    @ApplicationScoped
    private static class DifferentCacheFactoryBean
        {
        @Inject
        @Name("numbers")
        @View
        private ContinuousQueryCache defaultCcfNumbers;

        @Inject
        @Name("numbers")
        @View
        @Scope("test-config.xml")
        private ContinuousQueryCache specificCcfNumbers;

        @Inject
        @Scope("test-config.xml")
        private NamedCache numbers;

        public ContinuousQueryCache getDefaultCcfNumbers()
            {
            return defaultCcfNumbers;
            }

        public ContinuousQueryCache getSpecificCcfNumbers()
            {
            return specificCcfNumbers;
            }
        }

    @ApplicationScoped
    private static class CtorBean
        {
        private final NamedCache<Integer, String> view;

        private final ContinuousQueryCache<Integer, String, String> numbers;

        @Inject
        CtorBean(@Name("numbers") @View NamedCache<Integer, String> view,
                 @Name("numbers") ContinuousQueryCache<Integer, String, String> numbers)
            {
            this.view = view;
            this.numbers = numbers;
            }

        NamedCache<Integer, String> getView()
            {
            return view;
            }

        ContinuousQueryCache<Integer, String, String> getNumbers()
            {
            return numbers;
            }
        }

    @ApplicationScoped
    private static class SuperTypesBean
        {
        @Inject
        @Name("numbers")
        @View
        private ContinuousQueryCache<Integer, String, String> cqc;

        @Inject
        @Name("numbers")
        @View
        private NamedCache<Integer, String> namedCache;

        @Inject
        @Name("numbers")
        @View
        private InvocableMap<Integer, String> invocableMap;

        @Inject
        @Name("numbers")
        @View
        private ObservableMap<Integer, String> observableMap;

        @Inject
        @Name("numbers")
        @View
        private ConcurrentMap<Integer, String> concurrentMap;

        @Inject
        @Name("numbers")
        @View
        private QueryMap<Integer, String> queryMap;

        @Inject
        @Name("numbers")
        @View
        private CacheMap<Integer, String> cacheMap;

        ContinuousQueryCache<Integer, String, String> getContinuousQueryCache()
            {
            return cqc;
            }

        NamedCache<Integer, String> getNamedCache()
            {
            return namedCache;
            }

        InvocableMap<Integer, String> getInvocableMap()
            {
            return invocableMap;
            }

        ObservableMap<Integer, String> getObservableMap()
            {
            return observableMap;
            }

        ConcurrentMap<Integer, String> getConcurrentMap()
            {
            return concurrentMap;
            }

        QueryMap<Integer, String> getQueryMap()
            {
            return queryMap;
            }

        CacheMap<Integer, String> getCacheMap()
            {
            return cacheMap;
            }
        }

    @ApplicationScoped
    private static class WithTransformersBean
        {
        @Inject
        @Name("people")
        private NamedCache<String, Person> namedCache;

        @Inject
        @Name("people")
        @View(cacheValues = false)
        private ContinuousQueryCache<String, Person, String> keysOnly;

        @Inject
        @Name("people")
        @View
        @PropertyExtractor("firstName")
        private ContinuousQueryCache<String, Person, String> names;

        @Inject
        @Name("people")
        @View
        @ChainedExtractor({"phoneNumber", "number"})
        @WhereFilter("lastName = 'foo'")
        private ContinuousQueryCache<String, Person, String> filteredNames;

        NamedCache<String, Person> getNamedCache()
            {
            return namedCache;
            }

        ContinuousQueryCache<String, Person, String> getNames()
            {
            return names;
            }

        ContinuousQueryCache<String, Person, String> getFilteredNames()
            {
            return filteredNames;
            }

        ContinuousQueryCache<String, Person, String> getKeysOnly()
            {
            return keysOnly;
            }
        }
    }
