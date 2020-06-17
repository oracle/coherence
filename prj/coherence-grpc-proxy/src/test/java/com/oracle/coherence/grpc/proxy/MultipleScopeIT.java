package com.oracle.coherence.grpc.proxy;

import com.google.protobuf.ByteString;
import com.oracle.coherence.cdi.Scope;
import com.oracle.coherence.grpc.BinaryHelper;
import com.oracle.coherence.grpc.OptionalValue;
import com.oracle.coherence.grpc.Requests;
import com.tangosol.io.pof.ConfigurablePofContext;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.DefaultCacheServer;
import com.tangosol.net.ExtensibleConfigurableCacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;
import com.tangosol.util.Base;
import com.tangosol.util.ExternalizableHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Test accessing the same cache name in different scopes.
 *
 * @author jk  2020.06.16
 */
public class MultipleScopeIT
    {
    // ----- test lifecycle -------------------------------------------------

    @BeforeAll
    protected static void setup()
        {
        System.setProperty("coherence.ttl",        "0");
        System.setProperty("coherence.cluster",    "NamedCacheServiceIT");
        System.setProperty("coherence.cacheconfig", "coherence-config.xml");
        System.setProperty("coherence.pof.config", "test-pof-config.xml");
        System.setProperty("coherence.override",   "test-coherence-override.xml");

        s_ccfDefault = CacheFactory.getCacheFactoryBuilder().getConfigurableCacheFactory(null);
        DefaultCacheServer.startServerDaemon(s_ccfDefault).waitForServiceStart();

        s_serializer = new ConfigurablePofContext("test-pof-config.xml");

        s_service = NamedCacheService.builder()
                        .configurableCacheFactorySupplier(MultipleScopeIT::ensureCCF)
                        .build();
        }

    // ----- tests ----------------------------------------------------------

    @Test
    public void shouldGetFromDifferentScopes() throws Exception
        {
        String                     sName         = "put-cache";
        String                     sKey          = "key1";
        ByteString                 bytes         = BinaryHelper.toByteString(ExternalizableHelper.toBinary(sKey, s_serializer));
        String                     sValueDefault = "value-default";
        String                     sValueOne     = "value-one";
        String                     sValueTwo     = "value-two";
        NamedCache<String, String> cacheDefault  = ensureCache(Scope.DEFAULT, sName);
        NamedCache<String, String> cacheScopeOne = ensureCache(SCOPE_ONE, sName);
        NamedCache<String, String> cacheScopeTwo = ensureCache(SCOPE_TWO, sName);

        cacheDefault.put(sKey, sValueDefault);
        cacheScopeOne.put(sKey, sValueOne);
        cacheScopeTwo.put(sKey, sValueTwo);

        assertThat(cacheDefault.get(sKey), is(sValueDefault));
        assertThat(cacheScopeOne.get(sKey), is(sValueOne));
        assertThat(cacheScopeTwo.get(sKey), is(sValueTwo));

        OptionalValue valueDefault = s_service.get(Requests.get(Scope.DEFAULT, sName, "pof", bytes))
                                              .toCompletableFuture()
                                              .get();
        assertThat(valueDefault.getPresent(), is(true));
        assertThat(BinaryHelper.fromByteString(valueDefault.getValue(), s_serializer), is(sValueDefault));

        OptionalValue valueOne = s_service.get(Requests.get(SCOPE_ONE, sName, "pof", bytes))
                                          .toCompletableFuture()
                                          .get();
        assertThat(valueOne.getPresent(), is(true));
        assertThat(BinaryHelper.fromByteString(valueOne.getValue(), s_serializer), is(sValueOne));

        OptionalValue valueTwo = s_service.get(Requests.get(SCOPE_TWO, sName, "pof", bytes))
                                          .toCompletableFuture()
                                          .get();
        assertThat(valueTwo.getPresent(), is(true));
        assertThat(BinaryHelper.fromByteString(valueTwo.getValue(), s_serializer), is(sValueTwo));
        }

    // ----- helper methods -------------------------------------------------

    protected <K, V> NamedCache<K, V> ensureCache(String sScope, String name)
        {
        ConfigurableCacheFactory ccf = MultipleScopeIT.ensureCCF(sScope);
        return ccf.ensureCache(name, null);
        }

    protected static ConfigurableCacheFactory ensureCCF(String sScope)
        {
        if (Scope.DEFAULT.equals(sScope))
            {
            return s_ccfDefault;
            }
        return s_mapCCF.computeIfAbsent(sScope, MultipleScopeIT::createCCF);
        }

    protected static ConfigurableCacheFactory createCCF(String sScope)
        {
        ClassLoader loader = Base.getContextClassLoader();
        XmlElement xmlConfig = XmlHelper.loadFileOrResource("coherence-config.xml", "Cache Configuration", loader);

        ExtensibleConfigurableCacheFactory.Dependencies deps = ExtensibleConfigurableCacheFactory.DependenciesHelper.newInstance(xmlConfig, loader, "test-pof-config.xml", sScope);
        ExtensibleConfigurableCacheFactory eccf = new ExtensibleConfigurableCacheFactory(deps);
        eccf.activate();
        return eccf;
        }

    // ----- constants ------------------------------------------------------

    public static final String SCOPE_ONE = "one";

    public static final String SCOPE_TWO = "two";

    // ----- data members ---------------------------------------------------

    private static ConfigurablePofContext s_serializer;

    private static ConfigurableCacheFactory s_ccfDefault;

    private static NamedCacheService s_service;

    private final static Map<String, ConfigurableCacheFactory> s_mapCCF = new ConcurrentHashMap<>();
    }
