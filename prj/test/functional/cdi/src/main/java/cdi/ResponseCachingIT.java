/*
 * Copyright (c) 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package cdi;

import com.oracle.coherence.cdi.CacheAdd;
import com.oracle.coherence.cdi.CacheGet;
import com.oracle.coherence.cdi.CacheKey;
import com.oracle.coherence.cdi.CachePut;
import com.oracle.coherence.cdi.CacheRemove;
import com.oracle.coherence.cdi.CacheValue;
import com.oracle.coherence.cdi.CoherenceExtension;

import com.oracle.coherence.cdi.Name;
import com.oracle.coherence.cdi.SessionName;
import com.oracle.coherence.cdi.events.CacheName;

import com.tangosol.internal.cdi.MethodKey;

import com.tangosol.net.NamedMap;

import javax.enterprise.context.ApplicationScoped;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.spi.DefinitionException;

import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldSetup;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@ExtendWith(WeldJunit5Extension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ResponseCachingIT
    {
    @WeldSetup
    private WeldInitiator weld = WeldInitiator.of(WeldInitiator.createWeld()
                                                          .addPackages(CoherenceExtension.class)
                                                          .addExtension(new CoherenceExtension())
                                                          .addBeanClass(TestServerCoherenceProducer.class)
                                                          .addBeanClass(ResponseCachingIT.GetResource.class)
                                                          .addBeanClass(ResponseCachingIT.PutResource.class)
                                                          .addBeanClass(ResponseCachingIT.AddResource.class)
                                                          .addBeanClass(ResponseCachingIT.RemoveResource.class)
                                                          .addBeanClass(ResponseCachingIT.CacheNameResource.class)
                                                          .addBeanClass(ResponseCachingIT.SessionNameResource.class));

    @BeforeAll
    static void setup()
        {
        System.setProperty("coherence.distributed.localstorage", "true");
        System.setProperty("coherence.ttl", "0");
        }

    private NamedMap getCache;
    private NamedMap putCache;
    private NamedMap addCache;
    private NamedMap removeCache;
    private NamedMap methodCache;
    private NamedMap classCache;

    @BeforeEach
    void setupCaches()
        {
        getCache    = weld.select(NamedMap.class, Name.Literal.of("get-cache")).get();
        putCache    = weld.select(NamedMap.class, Name.Literal.of("put-cache")).get();
        addCache    = weld.select(NamedMap.class, Name.Literal.of("add-cache")).get();
        removeCache = weld.select(NamedMap.class, Name.Literal.of("remove-cache")).get();
        methodCache = weld.select(NamedMap.class, Name.Literal.of("method-cache")).get();
        classCache  = weld.select(NamedMap.class, Name.Literal.of("class-cache")).get();
        getCache.clear();
        putCache.clear();
        addCache.clear();
        removeCache.clear();
        methodCache.clear();
        classCache.clear();
        }

    @Test
    void shouldCacheGetWithoutParams()
        {
        NamedMap cache = weld.select(NamedMap.class, Name.Literal.of("get-cache")).get();
        cache.clear();
        GetResource bean = weld.select(GetResource.class, Any.Literal.INSTANCE).get();
        MethodKey key = new MethodKey(new Object[] {}, new Integer[] {});

        assertThat(cache.size(), is(0));
        assertThat(bean.getGetWithoutParamsCounter(), is(0));

        assertThat(bean.getWithoutParams(), is("something"));
        assertThat(cache.size(), is(1));
        assertThat(bean.getGetWithoutParamsCounter(), is(1));
        assertThat(cache.get(key), is("something"));

        assertThat(bean.getWithoutParams(), is("something"));
        assertThat(cache.size(), is(1));
        assertThat(bean.getGetWithoutParamsCounter(), is(1));

        cache.put(key, "different");
        assertThat(bean.getWithoutParams(), is("different"));
        assertThat(bean.getGetWithoutParamsCounter(), is(1));
        }

    @Test
    void shouldCacheGetWithParams()
        {
        GetResource bean = weld.select(GetResource.class, Any.Literal.INSTANCE).get();
        MethodKey key = new MethodKey(new Object[] {"one", "two"}, new Integer[] {0, 1});

        final String cachedValue12 = "Response for parameters one & two";
        assertThat(bean.getWithParams("one", "two"), is(cachedValue12));
        assertThat(getCache.size(), is(1));
        assertThat(getCache.get(key), is(cachedValue12));
        assertThat(bean.getGetWithParamsCounter(), is(1));

        assertThat(bean.getWithParams("one", "two"), is(cachedValue12));
        assertThat(getCache.get(key), is(cachedValue12));
        assertThat(bean.getGetWithParamsCounter(), is(1));

        MethodKey key34 = new MethodKey(new Object[] {"three", "four"}, new Integer[] {0, 1});
        final String cachedValue34 = "Response for parameters three & four";
        assertThat(bean.getWithParams("three", "four"), is(cachedValue34));
        assertThat(getCache.get(key34), is(cachedValue34));
        assertThat(bean.getGetWithParamsCounter(), is(2));

        getCache.put(key, "different");
        assertThat(bean.getWithParams("one", "two"), is("different"));
        assertThat(bean.getWithParams("three", "four"), is(cachedValue34));
        assertThat(bean.getGetWithParamsCounter(), is(2));
        }

    @Test
    void shouldCacheGetWithKey()
        {
        final String messageFormat = "Test message for key %s is %s";
        GetResource bean = weld.select(GetResource.class, Any.Literal.INSTANCE).get();

        final String cachedValue = String.format(messageFormat, "K", "two");
        assertThat(bean.getWithKey("K", "two"), is(cachedValue));
        assertThat(getCache.size(), is(1));
        assertThat(getCache.get("K"), is(cachedValue));
        assertThat(bean.getGetWithKeyCounter(), is(1));

        assertThat(bean.getWithKey("K", "two"), is(cachedValue));
        assertThat(getCache.get("K"), is(cachedValue));
        assertThat(bean.getGetWithKeyCounter(), is(1));

        final String cachedValue34 = String.format(messageFormat, "X", "four");
        assertThat(bean.getWithKey("X", "four"), is(cachedValue34));
        assertThat(bean.getGetWithKeyCounter(), is(2));
        assertThat(getCache.get("X"), is(cachedValue34));

        assertThat(bean.getWithKey("X", "four"), is(cachedValue34));
        assertThat(bean.getGetWithKeyCounter(), is(2));

        assertThat(bean.getWithKey("X", "five"), is(cachedValue34));
        assertThat(bean.getGetWithKeyCounter(), is(2));
        }

    @Test
    void shouldCachePutWithParams()
        {
        PutResource bean = weld.select(PutResource.class, Any.Literal.INSTANCE).get();
        final String messageFormat = "Value for keys %s & %s is %s";

        MethodKey key12 = new MethodKey(new Object[] {"one", "two", "three"}, new Integer[] {0, 1});
        final String cachedValue123 = String.format(messageFormat, "one", "two", "three");
        assertThat(bean.putWithParams("one", "two", "three"), is(cachedValue123));
        assertThat(putCache.size(), is(1));
        assertThat(bean.getPutWithParamsCounter(), is(1));
        assertThat(putCache.get(key12), is("three"));

        MethodKey key10 = new MethodKey(new Object[] {"one", "zero", "nine"}, new Integer[] {0, 1});
        final String cachedValue109 = String.format(messageFormat, "one", "zero", "nine");
        assertThat(bean.putWithParams("one", "zero", "nine"), is(cachedValue109));
        assertThat(putCache.size(), is(2));
        assertThat(bean.getPutWithParamsCounter(), is(2));
        assertThat(putCache.get(key10), is("nine"));

        MethodKey key34 = new MethodKey(new Object[] {"three", "four", "five"}, new Integer[] {0, 1});
        final String cachedValue345 = String.format(messageFormat, "three", "four", "five");
        assertThat(bean.putWithParams("three", "four", "five"), is(cachedValue345));
        assertThat(putCache.size(), is(3));
        assertThat(bean.getPutWithParamsCounter(), is(3));
        assertThat(putCache.get(key34), is("five"));

        putCache.put(key12, "different");
        assertThat(bean.putWithParams("one", "two", "three"), is(cachedValue123));
        assertThat(bean.getPutWithParamsCounter(), is(4));
        assertThat(putCache.size(), is(3));
        assertThat(putCache.get(key12), is("three"));
        }

    @Test
    void shouldCachePutWithKey()
        {
        PutResource bean = weld.select(PutResource.class, Any.Literal.INSTANCE).get();
        final String messageFormat = "Value for key %s is %s";

        final String cachedValue123 = String.format(messageFormat, "one", "three");
        assertThat(bean.putWithKey("one", "ignore", "three"), is(cachedValue123));
        assertThat(putCache.size(), is(1));
        assertThat(putCache.get("one"), is("three"));
        assertThat(bean.getPutWithKeyCounter(), is(1));

        final String cachedValue109 = String.format(messageFormat, "one", "nine");
        assertThat(bean.putWithKey("one", "ignore", "nine"), is(cachedValue109));
        assertThat(putCache.size(), is(1));
        assertThat(putCache.get("one"), is("nine"));
        assertThat(bean.getPutWithKeyCounter(), is(2));

        final String cachedValue345 = String.format(messageFormat, "three", "five");
        assertThat(bean.putWithKey("three", "ignore", "five"), is(cachedValue345));
        assertThat(putCache.size(), is(2));
        assertThat(putCache.get("three"), is("five"));
        assertThat(bean.getPutWithKeyCounter(), is(3));

        putCache.put("one", "different");
        assertThat(bean.putWithKey("one", "ignore", "three"), is(cachedValue123));
        assertThat(bean.getPutWithKeyCounter(), is(4));
        assertThat(putCache.get("one"), is("three"));
        assertThat(putCache.size(), is(2));
        }

    @Test
    void shouldCacheAddWithParams()
        {
        AddResource bean = weld.select(AddResource.class, Any.Literal.INSTANCE).get();

        final String cachedValue12 = "This is value for keys one & two";
        assertThat(bean.addWithParams("one", "two"), is(cachedValue12));
        assertThat(addCache.size(), is(1));
        assertThat(bean.getAddWithParamsCounter(), is(1));
        MethodKey key = new MethodKey(new Object[] {"one", "two"}, new Integer[] {0, 1});
        assertThat(addCache.get(key), is(cachedValue12));

        addCache.put(key, "something completely different");
        assertThat(bean.addWithParams("one", "two"), is(cachedValue12));
        assertThat(addCache.size(), is(1));
        assertThat(bean.getAddWithParamsCounter(), is(2));
        assertThat(addCache.get(key), is(cachedValue12));
        }

    @Test
    void shouldCacheAddWithKey()
        {
        AddResource bean = weld.select(AddResource.class, Any.Literal.INSTANCE).get();

        final String key = "K";
        final String cachedValue12 = "Value for key K is one-two";
        assertThat(bean.addWithKey(key, "one", "two"), is(cachedValue12));
        assertThat(addCache.size(), is(1));
        assertThat(bean.getAddWithKeyCounter(), is(1));
        assertThat(addCache.get(key), is(cachedValue12));

        addCache.put(key, "something completely different");
        final String cachedValue34 = "Value for key K is three-four";
        assertThat(bean.addWithKey(key, "three", "four"), is(cachedValue34));
        assertThat(addCache.size(), is(1));
        assertThat(bean.getAddWithKeyCounter(), is(2));
        assertThat(addCache.get(key), is(cachedValue34));
        }

    @Test
    void shouldRemove()
        {
        RemoveResource bean = weld.select(RemoveResource.class, Any.Literal.INSTANCE).get();

        MethodKey key = new MethodKey(new Object[] {"one", "two"}, new Integer[] {0, 1});
        removeCache.put(key, "test");
        assertThat(removeCache.size(), is(1));
        bean.remove("one", "two");
        assertThat(removeCache.size(), is(0));
        assertThat(bean.getRemoveCounter(), is(1));
        }

    @Test
    void shouldRemoveWithKey()
        {
        RemoveResource bean = weld.select(RemoveResource.class, Any.Literal.INSTANCE).get();

        removeCache.put("K", "test");
        assertThat(removeCache.size(), is(1));
        bean.removeWithKey("K", "one", "two");
        assertThat(removeCache.size(), is(0));
        assertThat(bean.getRemoveWithKeyCounter(), is(1));
        }

    @Test
    void shouldPickCacheNameFromClass()
        {
        CacheNameResource bean = weld.select(CacheNameResource.class, Any.Literal.INSTANCE).get();

        bean.get("X");
        assertThat(methodCache.size(), is(0));
        assertThat(classCache.size(), is(1));
        }

    @Test
    void shouldPickCacheNameFromMethod()
        {
        CacheNameResource bean = weld.select(CacheNameResource.class, Any.Literal.INSTANCE).get();

        bean.getAnother("Y");
        assertThat(methodCache.size(), is(1));
        assertThat(classCache.size(), is(0));
        }

    @Test
    void shouldPickSessionNameFromClass()
        {
        SessionNameResource bean = weld.select(SessionNameResource.class, Any.Literal.INSTANCE).get();

        DefinitionException exception = Assert.assertThrows(DefinitionException.class, () -> bean.get("X"));
        assertThat(exception.getMessage(), is("No Session is configured with name class-session-name"));
        }

    @Test
    void shouldPickSessionNameFromMethod()
        {
        SessionNameResource bean = weld.select(SessionNameResource.class, Any.Literal.INSTANCE).get();

        DefinitionException exception = Assert.assertThrows(DefinitionException.class, () -> bean.getAnother("X"));
        assertThat(exception.getMessage(), is("No Session is configured with name method-session-name"));
        }

    // ----- test beans -----------------------------------------------------

    @ApplicationScoped
    @CacheName("get-cache")
    private static class GetResource
        {
        public GetResource()
            {
            }

        @CacheGet
        public String getWithoutParams()
            {
            getWithoutParamsCounter++;
            return "something";
            }

        @CacheGet
        public String getWithParams(String first, String second)
            {
            getGetWithParamsCounter++;
            return String.format("Response for parameters %s & %s", first, second);
            }

        @CacheGet
        public String getWithKey(@CacheKey String key, String arg)
            {
            getGetWithKeyCounter++;
            return String.format("Test message for key %s is %s", key, arg);
            }

        private int getWithoutParamsCounter;

        private int getGetWithParamsCounter;

        private int getGetWithKeyCounter;

        int getGetWithoutParamsCounter()
            {
            return getWithoutParamsCounter;
            }

        int getGetWithParamsCounter()
            {
            return getGetWithParamsCounter;
            }

        int getGetWithKeyCounter()
            {
            return getGetWithKeyCounter;
            }
        }

    @ApplicationScoped
    @CacheName("put-cache")
    private static class PutResource
        {
        public PutResource()
            {
            }

        @CachePut
        public String putWithParams(String first, String second, @CacheValue String value)
            {
            getPutWithParamsCounter++;
            return String.format("Value for keys %s & %s is %s", first, second, value);
            }

        @CachePut
        public String putWithKey(@CacheKey String key, String second, @CacheValue String value)
            {
            getPutWithKeyCounter++;
            return String.format("Value for key %s is %s", key, value);
            }

        int getPutWithParamsCounter()
            {
            return getPutWithParamsCounter;
            }

        int getPutWithKeyCounter()
            {
            return getPutWithKeyCounter;
            }

        private int getPutWithParamsCounter;

        private int getPutWithKeyCounter;
        }

    @ApplicationScoped
    @CacheName("add-cache")
    private static class AddResource
        {
        public AddResource()
            {
            }

        @CacheAdd
        public String addWithParams(String first, String second)
            {
            getAddWithParamsCounter++;
            return String.format("This is value for keys %s & %s", first, second);
            }

        @CacheAdd
        public String addWithKey(@CacheKey String key, String first, String second)
            {
            getAddWithKeyCounter++;
            return String.format("Value for key %s is %s-%s", key, first, second);
            }

        int getAddWithParamsCounter()
            {
            return getAddWithParamsCounter;
            }

        int getAddWithKeyCounter()
            {
            return getAddWithKeyCounter;
            }

        private int getAddWithParamsCounter;

        private int getAddWithKeyCounter;
        }

    @ApplicationScoped
    @CacheName("remove-cache")
    private static class RemoveResource
        {
        public RemoveResource()
            {
            }

        @CacheRemove
        public void remove(String first, String second)
            {
            getRemoveCounter++;
            }

        @CacheRemove
        public void removeWithKey(@CacheKey String key, String first, String second)
            {
            getRemoveWithKeyCounter++;
            }

        int getRemoveCounter()
            {
            return getRemoveCounter;
            }

        int getRemoveWithKeyCounter()
            {
            return getRemoveWithKeyCounter;
            }

        private int getRemoveCounter;

        private int getRemoveWithKeyCounter;
        }

    @ApplicationScoped
    @CacheName("class-cache")
    private static class CacheNameResource
        {
        public CacheNameResource()
            {
            }

        @CacheGet
        public String get(String arg)
            {
            return String.format("Value for key %s", arg);
            }

        @CacheGet
        @CacheName("method-cache")
        public String getAnother(String arg)
            {
            return String.format("Value for key %s", arg);
            }
        }

    @ApplicationScoped
    @CacheName("class-cache")
    @SessionName("class-session-name")
    private static class SessionNameResource
        {
        public SessionNameResource()
            {
            }

        @CacheGet
        public String get(String arg)
            {
            return String.format("Value for key %s", arg);
            }

        @CacheGet
        @SessionName("method-session-name")
        public String getAnother(String arg)
            {
            return String.format("Value for key %s", arg);
            }
        }
    }