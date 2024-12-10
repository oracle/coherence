/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package grpc.proxy.version_0;

import com.google.protobuf.ByteString;

import com.oracle.coherence.grpc.BinaryHelper;
import com.oracle.coherence.grpc.v0.Requests;

import com.oracle.coherence.grpc.messages.cache.v0.OptionalValue;
import com.oracle.coherence.grpc.proxy.common.v0.NamedCacheService;

import com.tangosol.io.pof.ConfigurablePofContext;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.DefaultCacheServer;
import com.tangosol.net.ExtensibleConfigurableCacheFactory;
import com.tangosol.net.NamedCache;

import com.tangosol.net.grpc.GrpcAcceptorController;
import com.tangosol.net.grpc.GrpcDependencies;

import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;

import com.tangosol.util.Base;
import com.tangosol.util.ExternalizableHelper;

import grpc.proxy.TestNamedCacheServiceProvider;
import grpc.proxy.TestStreamObserver;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Test accessing the same cache name in different scopes.
 *
 * @author jk  2020.06.16
 */
@SuppressWarnings("resource")
public class NamedCacheServiceImplMultipleScopeIT
    {
    // ----- test lifecycle -------------------------------------------------

    @BeforeAll
    protected static void setup()
        {
        System.setProperty("coherence.ttl",         "0");
        System.setProperty("coherence.wka",         "127.0.0.1");
        System.setProperty("coherence.localhost",   "127.0.0.1");
        System.setProperty("coherence.cluster",     "NamedCacheServiceIT");
        System.setProperty("coherence.cacheconfig", "coherence-config.xml");
        System.setProperty("coherence.pof.config",  "test-pof-config.xml");
        System.setProperty("coherence.override",    "test-coherence-override.xml");

        s_ccfDefault = CacheFactory.getCacheFactoryBuilder().getConfigurableCacheFactory(null);
        DefaultCacheServer.startServerDaemon(s_ccfDefault).waitForServiceStart();

        s_serializer = new ConfigurablePofContext("test-pof-config.xml");

        GrpcAcceptorController                controller = GrpcAcceptorController.discoverController();
        NamedCacheService.DefaultDependencies deps       = new NamedCacheService.DefaultDependencies(controller.getServerType());
        deps.setConfigurableCacheFactorySupplier(NamedCacheServiceImplMultipleScopeIT::ensureCCF);

        Optional<TestNamedCacheServiceProvider> optional = TestNamedCacheServiceProvider.getProvider();
        assertThat(optional.isPresent(), is(true));
        s_service = optional.get().getService(deps);
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
        NamedCache<String, String> cacheDefault  = ensureCache(GrpcDependencies.DEFAULT_SCOPE, sName);
        NamedCache<String, String> cacheScopeOne = ensureCache(SCOPE_ONE, sName);
        NamedCache<String, String> cacheScopeTwo = ensureCache(SCOPE_TWO, sName);

        cacheDefault.put(sKey, sValueDefault);
        cacheScopeOne.put(sKey, sValueOne);
        cacheScopeTwo.put(sKey, sValueTwo);

        assertThat(cacheDefault.get(sKey), is(sValueDefault));
        assertThat(cacheScopeOne.get(sKey), is(sValueOne));
        assertThat(cacheScopeTwo.get(sKey), is(sValueTwo));

        TestStreamObserver<OptionalValue> observer;

        observer = new TestStreamObserver<>();
        s_service.get(Requests.get(GrpcDependencies.DEFAULT_SCOPE, sName, "pof", bytes), observer);
        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertValueCount(1)
                .assertNoErrors();

        OptionalValue valueDefault = observer.valueAt(0);
        assertThat(valueDefault.getPresent(), is(true));
        assertThat(BinaryHelper.fromByteString(valueDefault.getValue(), s_serializer), is(sValueDefault));

        observer = new TestStreamObserver<>();
        s_service.get(Requests.get(SCOPE_ONE, sName, "pof", bytes), observer);
        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertValueCount(1)
                .assertNoErrors();

        OptionalValue valueOne = observer.valueAt(0);
        assertThat(valueOne.getPresent(), is(true));
        assertThat(BinaryHelper.fromByteString(valueOne.getValue(), s_serializer), is(sValueOne));

        observer = new TestStreamObserver<>();
        s_service.get(Requests.get(SCOPE_TWO, sName, "pof", bytes), observer);
        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertValueCount(1)
                .assertNoErrors();

        OptionalValue valueTwo = observer.valueAt(0);
        assertThat(valueTwo.getPresent(), is(true));
        assertThat(BinaryHelper.fromByteString(valueTwo.getValue(), s_serializer), is(sValueTwo));
        }

    // ----- helper methods -------------------------------------------------

    protected <K, V> NamedCache<K, V> ensureCache(String sScope, String name)
        {
        ConfigurableCacheFactory ccf = NamedCacheServiceImplMultipleScopeIT.ensureCCF(sScope);
        return ccf.ensureCache(name, null);
        }

    protected static ConfigurableCacheFactory ensureCCF(String sScope)
        {
        if (GrpcDependencies.DEFAULT_SCOPE.equals(sScope))
            {
            return s_ccfDefault;
            }
        return s_mapCCF.computeIfAbsent(sScope, NamedCacheServiceImplMultipleScopeIT::createCCF);
        }

    protected static ConfigurableCacheFactory createCCF(String sScope)
        {
        ClassLoader loader = Base.getContextClassLoader();
        XmlElement xmlConfig = XmlHelper.loadFileOrResource("coherence-config.xml", "Cache Configuration", loader);

        ExtensibleConfigurableCacheFactory.Dependencies deps = ExtensibleConfigurableCacheFactory.DependenciesHelper.newInstance(xmlConfig, loader, "test-pof-config.xml", sScope, null);
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
