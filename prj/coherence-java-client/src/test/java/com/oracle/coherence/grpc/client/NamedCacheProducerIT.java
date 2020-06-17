/*
 * Copyright (c) 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.client;

import com.oracle.coherence.cdi.Name;
import com.oracle.coherence.cdi.Remote;
import com.oracle.coherence.cdi.Scope;
import com.tangosol.net.AsyncNamedCache;
import com.tangosol.net.NamedCache;

import com.tangosol.net.NamedMap;
import com.tangosol.net.cache.CacheMap;

import com.tangosol.util.ConcurrentMap;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.ObservableMap;
import com.tangosol.util.QueryMap;

import io.helidon.microprofile.server.Server;
import java.lang.annotation.Annotation;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import javax.enterprise.inject.Instance;

import javax.enterprise.inject.spi.CDI;

import javax.inject.Inject;

import org.hamcrest.Matchers;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Jonathan Knight  2019.11.07
 * @since 20.06
 */
class NamedCacheProducerIT
    {
    // ----- test lifecycle -------------------------------------------------

    @BeforeAll
    static void setup()
        {
        System.setProperty("coherence.ttl",         "0");
        System.setProperty("coherence.clustername", "NamedCacheServiceCdiIT");

        s_server = Server.create().start();
        }

    @AfterAll
    static void cleanup()
        {
        s_server.stop();
        }

    // ----- test methods ---------------------------------------------------

    @SuppressWarnings("rawtypes")
    @Test
    void shouldGetDynamicNamedCache()
        {
        Annotation           name     = Name.Literal.of("numbers");
        Instance<NamedCache> instance = CDI.current().select(NamedCache.class, name, Remote.Literal.DEFAULT);

        assertThat(instance.isResolvable(), is(true));
        assertThat(instance.get().getName(), is("numbers"));
        }

    @Test
    void shouldInjectNamedCacheUsingFieldName()
        {
        NamedCacheFieldsBean bean = CDI.current().select(NamedCacheFieldsBean.class).get();
        assertThat(bean.getNumbers(), is(notNullValue()));
        assertThat(bean.getNumbers().getName(), is("numbers"));
        }

    @Test
    void shouldInjectQualifiedNamedCache()
        {
        NamedCacheFieldsBean bean = CDI.current().select(NamedCacheFieldsBean.class).get();
        assertThat(bean.getNamedCache(), is(notNullValue()));
        assertThat(bean.getNamedCache().getName(), is("numbers"));
        }

    @Test
    void shouldInjectNamedCacheWithGenerics()
        {
        NamedCacheFieldsBean bean = CDI.current().select(NamedCacheFieldsBean.class).get();
        assertThat(bean.getGenericCache(), is(notNullValue()));
        assertThat(bean.getGenericCache().getName(), is("numbers"));
        }

    @Test
    void shouldInjectNamedCacheWithGenericKeys()
        {
        NamedCacheFieldsBean bean = CDI.current().select(NamedCacheFieldsBean.class).get();
        assertThat(bean.getGenericKeys(), is(notNullValue()));
        assertThat(bean.getGenericKeys().getName(), is("genericKeys"));
        }

    @Test
    void shouldInjectNamedCacheWithGenericValues()
        {
        NamedCacheFieldsBean bean = CDI.current().select(NamedCacheFieldsBean.class).get();
        assertThat(bean.getGenericValues(), is(notNullValue()));
        assertThat(bean.getGenericValues().getName(), is("genericValues"));
        }

    @Test
    void shouldInjectAsyncNamedCacheUsingFieldName()
        {
        AsyncNamedCacheFieldsBean bean = CDI.current().select(AsyncNamedCacheFieldsBean.class).get();
        assertThat(bean.getNumbers(), is(notNullValue()));
        assertThat(bean.getNumbers().getNamedCache().getName(), is("numbers"));
        }

    @Test
    void shouldInjectQualifiedAsyncNamedCache()
        {
        AsyncNamedCacheFieldsBean bean = CDI.current().select(AsyncNamedCacheFieldsBean.class).get();
        assertThat(bean.getNamedCache(), is(notNullValue()));
        assertThat(bean.getNamedCache().getNamedCache().getName(), is("numbers"));
        }

    @Test
    void shouldInjectAsyncNamedCacheWithGenerics()
        {
        AsyncNamedCacheFieldsBean bean = CDI.current().select(AsyncNamedCacheFieldsBean.class).get();
        assertThat(bean.getGenericCache(), is(notNullValue()));
        assertThat(bean.getGenericCache().getNamedCache().getName(), is("numbers"));
        }

    @Test
    void shouldInjectAsyncNamedCacheWithGenericKeys()
        {
        AsyncNamedCacheFieldsBean bean = CDI.current().select(AsyncNamedCacheFieldsBean.class).get();
        assertThat(bean.getGenericKeys(), is(notNullValue()));
        assertThat(bean.getGenericKeys().getNamedCache().getName(), is("genericKeys"));
        }

    @Test
    void shouldInjectAsyncNamedCacheWithGenericValues()
        {
        AsyncNamedCacheFieldsBean bean = CDI.current().select(AsyncNamedCacheFieldsBean.class).get();
        assertThat(bean.getGenericValues(), is(notNullValue()));
        assertThat(bean.getGenericValues().getNamedCache().getName(), is("genericValues"));
        }

    @Test
    void shouldInjectCachesFromDifferentChannels()
        {
        DifferentCacheFactoryBean bean = CDI.current().select(DifferentCacheFactoryBean.class).get();

        assertThat(bean.getDefaultCcfNumbers(), is(notNullValue()));
        assertThat(bean.getDefaultCcfNumbers().getName(), Matchers.is("numbers"));
        assertThat(bean.getDefaultCcfAsyncNumbers(), is(notNullValue()));
        assertThat(bean.getDefaultCcfAsyncNumbers().getNamedCache().getName(), Matchers.is("numbers"));
        assertThat(bean.getDefaultCcfAsyncNumbers().getNamedCache(), is(bean.getDefaultCcfNumbers()));

        assertThat(bean.getSpecificCcfNumbers(), is(notNullValue()));
        assertThat(bean.getSpecificCcfNumbers().getName(), Matchers.is("numbers"));
        assertThat(bean.getSpecificCcfAsyncNumbers(), is(notNullValue()));
        assertThat(bean.getSpecificCcfAsyncNumbers().getNamedCache().getName(), Matchers.is("numbers"));
        assertThat(bean.getSpecificCcfAsyncNumbers().getNamedCache(), is(bean.getSpecificCcfNumbers()));

        assertThat(bean.getDefaultCcfNumbers(), is(not(sameInstance(bean.getSpecificCcfNumbers()))));
        }

    @Test
    void testCtorInjection()
        {
        CtorBean bean = CDI.current().select(CtorBean.class).get();

        assertThat(bean.getNumbers(), Matchers.notNullValue());
        assertThat(bean.getNumbers().getName(), Matchers.is("numbers"));
        assertThat(bean.getLetters(), Matchers.notNullValue());
        assertThat(bean.getLetters().getNamedCache().getName(), Matchers.is("letters"));
        }

    @SuppressWarnings("rawtypes")
    @Test
    void shouldInjectSuperTypeNamedMap()
        {
        SuperTypesBean bean = CDI.current().select(SuperTypesBean.class).get();
        NamedMap       map  = bean.getNamedMap();
        assertThat(map, is(notNullValue()));
        assertThat(map, is(sameInstance(bean.getNamedCache())));
        }

    @SuppressWarnings("rawtypes")
    @Test
    void shouldInjectSuperTypeInvocableMap()
        {
        SuperTypesBean bean = CDI.current().select(SuperTypesBean.class).get();
        InvocableMap   map  = bean.getInvocableMap();
        assertThat(map, is(notNullValue()));
        assertThat(map, is(sameInstance(bean.getNamedCache())));
        }

    @SuppressWarnings("rawtypes")
    @Test
    void shouldInjectSuperTypeObservableMap()
        {
        SuperTypesBean bean = CDI.current().select(SuperTypesBean.class).get();
        ObservableMap  map  = bean.getObservableMap();
        assertThat(map, is(notNullValue()));
        assertThat(map, is(sameInstance(bean.getNamedCache())));
        }

    @SuppressWarnings("rawtypes")
    @Test
    void shouldInjectSuperTypeConcurrentMap()
        {
        SuperTypesBean bean = CDI.current().select(SuperTypesBean.class).get();
        ConcurrentMap  map  = bean.getConcurrentMap();
        assertThat(map, is(notNullValue()));
        assertThat(map, is(sameInstance(bean.getNamedCache())));
        }

    @SuppressWarnings("rawtypes")
    @Test
    void shouldInjectSuperTypeQueryMap()
        {
        SuperTypesBean bean = CDI.current().select(SuperTypesBean.class).get();
        QueryMap       map  = bean.getQueryMap();
        assertThat(map, is(notNullValue()));
        assertThat(map, is(sameInstance(bean.getNamedCache())));
        }

    @SuppressWarnings("rawtypes")
    @Test
    void shouldInjectSuperTypeCacheMap()
        {
        SuperTypesBean bean = CDI.current().select(SuperTypesBean.class).get();
        CacheMap       map  = bean.getCacheMap();
        assertThat(map, is(notNullValue()));
        assertThat(map, is(sameInstance(bean.getNamedCache())));
        }

    // ----- inner class NamedCacheFieldsBean -------------------------------

    @SuppressWarnings("rawtypes")
    @ApplicationScoped
    protected static class NamedCacheFieldsBean
        {
        // ----- accessors --------------------------------------------------

        protected NamedCache getNumbers()
            {
            return numbers;
            }

        protected NamedCache getNamedCache()
            {
            return m_namedCache;
            }

        protected NamedCache<Integer, String> getGenericCache()
            {
            return m_genericCache;
            }

        protected NamedCache<List<String>, String> getGenericKeys()
            {
            return m_genericKeys;
            }

        protected NamedCache<String, List<String>> getGenericValues()
            {
            return m_genericValues;
            }

        // ----- data members -----------------------------------------------

        @Inject
        @Remote
        protected NamedCache numbers;

        @Inject
        @Remote
        @Name("numbers")
        protected NamedCache m_namedCache;

        @Inject
        @Remote
        @Name("numbers")
        protected NamedCache<Integer, String> m_genericCache;

        @Inject
        @Remote
        @Name("genericKeys")
        protected NamedCache<List<String>, String> m_genericKeys;

        @Inject
        @Remote
        @Name("genericValues")
        protected NamedCache<String, List<String>> m_genericValues;
        }

    // ----- inner class: AsyncNamedCacheFieldsBean -------------------------

    @SuppressWarnings("rawtypes")
    @ApplicationScoped
    protected static class AsyncNamedCacheFieldsBean
        {
        // ----- accessors --------------------------------------------------

        protected AsyncNamedCache getNumbers()
            {
            return numbers;
            }

        protected AsyncNamedCache getNamedCache()
            {
            return m_namedCache;
            }

        protected AsyncNamedCache<Integer, String> getGenericCache()
            {
            return m_genericCache;
            }

        protected AsyncNamedCache<List<String>, String> getGenericKeys()
            {
            return m_genericKeys;
            }

        protected AsyncNamedCache<String, List<String>> getGenericValues()
            {
            return m_genericValues;
            }

        // ----- data members -----------------------------------------------

        @Inject
        @Remote
        protected AsyncNamedCache numbers;

        @Inject
        @Remote
        @Name("numbers")
        protected AsyncNamedCache m_namedCache;

        @Inject
        @Remote
        @Name("numbers")
        protected AsyncNamedCache<Integer, String> m_genericCache;

        @Inject
        @Remote
        @Name("genericKeys")
        protected AsyncNamedCache<List<String>, String> m_genericKeys;

        @Inject
        @Remote
        @Name("genericValues")
        protected AsyncNamedCache<String, List<String>> m_genericValues;
        }

    // ----- inner class: DifferentCacheFactoryBean -------------------------

    @SuppressWarnings("rawtypes")
    @ApplicationScoped
    protected static class DifferentCacheFactoryBean
        {
        // ----- accessors --------------------------------------------------

        protected NamedCache getDefaultCcfNumbers()
            {
            return m_defaultCcfNumbers;
            }

        protected AsyncNamedCache getDefaultCcfAsyncNumbers()
            {
            return m_defaultCcfAsyncNumbers;
            }

        protected NamedCache getSpecificCcfNumbers()
            {
            return m_specificCcfNumbers;
            }

        protected AsyncNamedCache getSpecificCcfAsyncNumbers()
            {
            return m_specificCcfAsyncNumbers;
            }

        // ----- data members -----------------------------------------------

        @Inject
        @Remote
        @Name("numbers")
        protected NamedCache m_defaultCcfNumbers;

        @Inject
        @Remote
        @Name("numbers")
        protected AsyncNamedCache m_defaultCcfAsyncNumbers;

        @Inject
        @Remote("test")
        @Name("numbers")
        protected NamedCache m_specificCcfNumbers;

        @Inject
        @Remote("test")
        @Name("numbers")
        protected AsyncNamedCache m_specificCcfAsyncNumbers;
        }

    // ----- inner class: CtorBean ------------------------------------------

    @ApplicationScoped
    protected static class CtorBean
        {
        // ----- constructors -----------------------------------------------

        @Inject
        public CtorBean(@Remote @Name("numbers") NamedCache<Integer, String> numbers,
                        @Remote @Name("letters") AsyncNamedCache<String, String> letters)
            {
            this.m_numbers = numbers;
            this.m_letters = letters;
            }

        // ----- accessors --------------------------------------------------

        protected NamedCache<Integer, String> getNumbers()
            {
            return m_numbers;
            }

        protected AsyncNamedCache<String, String> getLetters()
            {
            return m_letters;
            }

        // ----- data members -----------------------------------------------

        protected final NamedCache<Integer, String> m_numbers;

        protected final AsyncNamedCache<String, String> m_letters;
        }
    
    // ----- inner class: SuperTypesBean ------------------------------------

    @ApplicationScoped
    protected static class SuperTypesBean
        {
        // ----- accessors --------------------------------------------------

        protected NamedCache<Integer, String> getNamedCache()
            {
            return m_namedCache;
            }

         protected NamedMap<Integer, String> getNamedMap()
            {
            return m_namedCache;
            }

        protected InvocableMap<Integer, String> getInvocableMap()
            {
            return m_invocableMap;
            }

        protected ObservableMap<Integer, String> getObservableMap()
            {
            return m_observableMap;
            }

        protected ConcurrentMap<Integer, String> getConcurrentMap()
            {
            return m_concurrentMap;
            }

        protected QueryMap<Integer, String> getQueryMap()
            {
            return m_queryMap;
            }

        protected CacheMap<Integer, String> getCacheMap()
            {
            return m_cacheMap;
            }

        // ----- data members -----------------------------------------------

        @Inject
        @Remote
        @Name("numbers")
        protected NamedCache<Integer, String> m_namedCache;

        @Inject
        @Remote
        @Name("numbers")
        protected NamedMap<Integer, String> m_namedMap;

        @Inject
        @Remote
        @Name("numbers")
        protected InvocableMap<Integer, String> m_invocableMap;

        @Inject
        @Remote
        @Name("numbers")
        protected ObservableMap<Integer, String> m_observableMap;

        @Inject
        @Remote
        @Name("numbers")
        protected ConcurrentMap<Integer, String> m_concurrentMap;

        @Inject
        @Remote
        @Name("numbers")
        protected QueryMap<Integer, String> m_queryMap;

        @Inject
        @Remote
        @Name("numbers")
        protected CacheMap<Integer, String> m_cacheMap;
        }

    // ----- data members ---------------------------------------------------

    protected static Server s_server;
    }
