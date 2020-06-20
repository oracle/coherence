/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.cdi.server;

import com.oracle.coherence.cdi.CoherenceExtension;
import com.oracle.coherence.cdi.ExtractorProducer;
import com.oracle.coherence.cdi.FilterProducer;
import com.oracle.coherence.cdi.Name;
import com.oracle.coherence.cdi.Scope;

import com.tangosol.net.AsyncNamedMap;
import com.tangosol.net.NamedMap;
import com.tangosol.net.cache.CacheMap;

import com.tangosol.util.ConcurrentMap;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.ObservableMap;
import com.tangosol.util.QueryMap;

import java.lang.annotation.Annotation;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;


import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldSetup;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Integration test for the {@link NamedCacheProducer}
 * using the Weld JUnit extension.
 *
 * @author Jonathan Knight  2019.10.19
 */
@SuppressWarnings("rawtypes")
@ExtendWith(WeldJunit5Extension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NamedMapProducerIT
    {

    @WeldSetup
    private WeldInitiator weld = WeldInitiator.of(WeldInitiator.createWeld()
                                                          .addBeanClass(CtorBean.class)
                                                          .addBeanClass(NamedMapFieldsBean.class)
                                                          .addBeanClass(AsyncNamedMapFieldsBean.class)
                                                          .addBeanClass(SuperTypesBean.class)
                                                          .addBeanClass(DifferentCacheFactoryBean.class)
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
                                                          .addBeanClass(NamedCacheProducer.class)
                                                          .addBeanClass(CacheFactoryUriResolver.Default.class)
                                                          .addBeanClass(ConfigurableCacheFactoryProducer.class)
                                                          .addExtension(new CoherenceExtension())
                                                          .addExtension(new CoherenceServerExtension()));

    @Test
    void shouldGetDynamicNamedMap()
        {
        Annotation qualifier = Name.Literal.of("numbers");
        Instance<NamedMap> instance = weld.select(NamedMap.class, qualifier);

        assertThat(instance.isResolvable(), is(true));
        assertThat(instance.get().getName(), is("numbers"));
        }

    @Test
    void shouldInjectNamedMapUsingFieldName()
        {
        NamedMapFieldsBean bean = weld.select(NamedMapFieldsBean.class).get();
        assertThat(bean.getNumbers(), is(notNullValue()));
        assertThat(bean.getNumbers().getName(), is("numbers"));
        }

    @Test
    void shouldInjectQualifiedNamedMap()
        {
        NamedMapFieldsBean bean = weld.select(NamedMapFieldsBean.class).get();
        assertThat(bean.getNamedMap(), is(notNullValue()));
        assertThat(bean.getNamedMap().getName(), is("numbers"));
        }

    @Test
    void shouldInjectNamedMapWithGenerics()
        {
        NamedMapFieldsBean bean = weld.select(NamedMapFieldsBean.class).get();
        assertThat(bean.getGenericCache(), is(notNullValue()));
        assertThat(bean.getGenericCache().getName(), is("numbers"));
        }

    @Test
    void shouldInjectNamedMapWithGenericKeys()
        {
        NamedMapFieldsBean bean = weld.select(NamedMapFieldsBean.class).get();
        assertThat(bean.getGenericKeys(), is(notNullValue()));
        assertThat(bean.getGenericKeys().getName(), is("genericKeys"));
        }

    @Test
    void shouldInjectNamedMapWithGenericValues()
        {
        NamedMapFieldsBean bean = weld.select(NamedMapFieldsBean.class).get();
        assertThat(bean.getGenericValues(), is(notNullValue()));
        assertThat(bean.getGenericValues().getName(), is("genericValues"));
        }

    @Test
    void shouldInjectAsyncNamedMapUsingFieldName()
        {
        AsyncNamedMapFieldsBean bean = weld.select(AsyncNamedMapFieldsBean.class).get();
        assertThat(bean.getNumbers(), is(notNullValue()));
        assertThat(bean.getNumbers().getNamedMap().getName(), is("numbers"));
        }

    @Test
    void shouldInjectQualifiedAsyncNamedMap()
        {
        AsyncNamedMapFieldsBean bean = weld.select(AsyncNamedMapFieldsBean.class).get();
        assertThat(bean.getNamedMap(), is(notNullValue()));
        assertThat(bean.getNamedMap().getNamedMap().getName(), is("numbers"));
        }

    @Test
    void shouldInjectAsyncNamedMapWithGenerics()
        {
        AsyncNamedMapFieldsBean bean = weld.select(AsyncNamedMapFieldsBean.class).get();
        assertThat(bean.getGenericCache(), is(notNullValue()));
        assertThat(bean.getGenericCache().getNamedMap().getName(), is("numbers"));
        }

    @Test
    void shouldInjectAsyncNamedMapWithGenericKeys()
        {
        AsyncNamedMapFieldsBean bean = weld.select(AsyncNamedMapFieldsBean.class).get();
        assertThat(bean.getGenericKeys(), is(notNullValue()));
        assertThat(bean.getGenericKeys().getNamedMap().getName(), is("genericKeys"));
        }

    @Test
    void shouldInjectAsyncNamedMapWithGenericValues()
        {
        AsyncNamedMapFieldsBean bean = weld.select(AsyncNamedMapFieldsBean.class).get();
        assertThat(bean.getGenericValues(), is(notNullValue()));
        assertThat(bean.getGenericValues().getNamedMap().getName(), is("genericValues"));
        }

    @Test
    void shouldInjectCachesFromDifferentCacheFactories()
        {
        DifferentCacheFactoryBean bean = weld.select(DifferentCacheFactoryBean.class).get();

        assertThat(bean.getDefaultCcfNumbers(), is(notNullValue()));
        assertThat(bean.getDefaultCcfNumbers().getName(), is("numbers"));
        assertThat(bean.getDefaultCcfAsyncNumbers(), is(notNullValue()));
        assertThat(bean.getDefaultCcfAsyncNumbers().getNamedMap().getName(), is("numbers"));
        assertThat(bean.getDefaultCcfAsyncNumbers().getNamedMap(), is(bean.getDefaultCcfNumbers()));

        assertThat(bean.getSpecificCcfNumbers(), is(notNullValue()));
        assertThat(bean.getSpecificCcfNumbers().getName(), is("numbers"));
        assertThat(bean.getSpecificCcfAsyncNumbers(), is(notNullValue()));
        assertThat(bean.getSpecificCcfAsyncNumbers().getNamedMap().getName(), is("numbers"));
        assertThat(bean.getSpecificCcfAsyncNumbers().getNamedMap(), is(bean.getSpecificCcfNumbers()));

        assertThat(bean.getDefaultCcfNumbers(), is(not(bean.getSpecificCcfNumbers())));
        }

    @Test
    void testCtorInjection()
        {
        CtorBean bean = weld.select(CtorBean.class).get();

        assertThat(bean.getNumbers(), notNullValue());
        assertThat(bean.getNumbers().getName(), is("numbers"));
        assertThat(bean.getLetters(), notNullValue());
        assertThat(bean.getLetters().getNamedMap().getName(), is("letters"));
        }

    @Test
    void shouldInjectSuperTypeInvocableMap()
        {
        SuperTypesBean bean = weld.select(SuperTypesBean.class).get();
        InvocableMap map = bean.getInvocableMap();
        assertThat(map, is(notNullValue()));
        assertThat(map, is(sameInstance(bean.getNamedMap())));
        }

    @Test
    void shouldInjectSuperTypeObservableMap()
        {
        SuperTypesBean bean = weld.select(SuperTypesBean.class).get();
        ObservableMap map = bean.getObservableMap();
        assertThat(map, is(notNullValue()));
        assertThat(map, is(sameInstance(bean.getNamedMap())));
        }

    @Test
    void shouldInjectSuperTypeConcurrentMap()
        {
        SuperTypesBean bean = weld.select(SuperTypesBean.class).get();
        ConcurrentMap map = bean.getConcurrentMap();
        assertThat(map, is(notNullValue()));
        assertThat(map, is(sameInstance(bean.getNamedMap())));
        }

    @Test
    void shouldInjectSuperTypeQueryMap()
        {
        SuperTypesBean bean = weld.select(SuperTypesBean.class).get();
        QueryMap map = bean.getQueryMap();
        assertThat(map, is(notNullValue()));
        assertThat(map, is(sameInstance(bean.getNamedMap())));
        }

    @Test
    void shouldInjectSuperTypeCacheMap()
        {
        SuperTypesBean bean = weld.select(SuperTypesBean.class).get();
        CacheMap map = bean.getCacheMap();
        assertThat(map, is(notNullValue()));
        assertThat(map, is(sameInstance(bean.getNamedMap())));
        }

    // ----- test beans -----------------------------------------------------

    @ApplicationScoped
    private static class NamedMapFieldsBean
        {
        @Inject
        private NamedMap numbers;

        @Inject
        @Name("numbers")
        private NamedMap namedMap;

        @Inject
        @Name("numbers")
        private NamedMap<Integer, String> genericCache;

        @Inject
        private NamedMap<List<String>, String> genericKeys;

        @Inject
        private NamedMap<String, List<String>> genericValues;

        public NamedMap getNumbers()
            {
            return numbers;
            }

        public NamedMap getNamedMap()
            {
            return namedMap;
            }

        public NamedMap<Integer, String> getGenericCache()
            {
            return genericCache;
            }

        public NamedMap<List<String>, String> getGenericKeys()
            {
            return genericKeys;
            }

        public NamedMap<String, List<String>> getGenericValues()
            {
            return genericValues;
            }
        }

    @ApplicationScoped
    private static class AsyncNamedMapFieldsBean
        {
        @Inject
        private AsyncNamedMap numbers;

        @Inject
        @Name("numbers")
        private AsyncNamedMap namedMap;

        @Inject
        @Name("numbers")
        private AsyncNamedMap<Integer, String> genericCache;

        @Inject
        private AsyncNamedMap<List<String>, String> genericKeys;

        @Inject
        private AsyncNamedMap<String, List<String>> genericValues;

        public AsyncNamedMap getNumbers()
            {
            return numbers;
            }

        public AsyncNamedMap getNamedMap()
            {
            return namedMap;
            }

        public AsyncNamedMap<Integer, String> getGenericCache()
            {
            return genericCache;
            }

        public AsyncNamedMap<List<String>, String> getGenericKeys()
            {
            return genericKeys;
            }

        public AsyncNamedMap<String, List<String>> getGenericValues()
            {
            return genericValues;
            }
        }

    @ApplicationScoped
    private static class DifferentCacheFactoryBean
        {
        @Inject
        @Name("numbers")
        private NamedMap defaultCcfNumbers;

        @Inject
        @Name("numbers")
        private AsyncNamedMap defaultCcfAsyncNumbers;

        @Inject
        @Name("numbers")
        @Scope("test-config.xml")
        private NamedMap specificCcfNumbers;

        @Inject
        @Name("numbers")
        @Scope("test-config.xml")
        private AsyncNamedMap specificCcfAsyncNumbers;

        public NamedMap getDefaultCcfNumbers()
            {
            return defaultCcfNumbers;
            }

        public AsyncNamedMap getDefaultCcfAsyncNumbers()
            {
            return defaultCcfAsyncNumbers;
            }

        public NamedMap getSpecificCcfNumbers()
            {
            return specificCcfNumbers;
            }

        public AsyncNamedMap getSpecificCcfAsyncNumbers()
            {
            return specificCcfAsyncNumbers;
            }
        }

    @ApplicationScoped
    private static class CtorBean
        {

        private final NamedMap<Integer, String> numbers;

        private final AsyncNamedMap<String, String> letters;

        @Inject
        CtorBean(@Name("numbers") NamedMap<Integer, String> numbers,
                 @Name("letters") AsyncNamedMap<String, String> letters)
            {

            this.numbers = numbers;
            this.letters = letters;
            }

        NamedMap<Integer, String> getNumbers()
            {
            return numbers;
            }

        AsyncNamedMap<String, String> getLetters()
            {
            return letters;
            }
        }

    @ApplicationScoped
    private static class SuperTypesBean
        {
        @Inject
        @Name("numbers")
        private NamedMap<Integer, String> namedMap;

        @Inject
        @Name("numbers")
        private InvocableMap<Integer, String> invocableMap;

        @Inject
        @Name("numbers")
        private ObservableMap<Integer, String> observableMap;

        @Inject
        @Name("numbers")
        private ConcurrentMap<Integer, String> concurrentMap;

        @Inject
        @Name("numbers")
        private QueryMap<Integer, String> queryMap;

        @Inject
        @Name("numbers")
        private CacheMap<Integer, String> cacheMap;

        NamedMap<Integer, String> getNamedMap()
            {
            return namedMap;
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
    }
