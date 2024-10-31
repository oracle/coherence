/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package netty.grpc.proxy;

import com.google.protobuf.ByteString;

import com.oracle.coherence.grpc.BinaryHelper;
import com.oracle.coherence.grpc.v0.Requests;

import com.oracle.coherence.grpc.messages.cache.v0.MapEventResponse;
import com.oracle.coherence.grpc.messages.cache.v0.MapListenerErrorResponse;
import com.oracle.coherence.grpc.messages.cache.v0.MapListenerRequest;
import com.oracle.coherence.grpc.messages.cache.v0.MapListenerResponse;
import com.oracle.coherence.grpc.messages.cache.v0.MapListenerSubscribedResponse;
import com.oracle.coherence.grpc.messages.cache.v0.MapListenerUnsubscribedResponse;

import com.oracle.coherence.grpc.proxy.common.ConfigurableCacheFactorySuppliers;
import com.oracle.coherence.grpc.proxy.common.v0.MapListenerProxy;
import com.oracle.coherence.grpc.proxy.common.v0.NamedCacheService;

import com.tangosol.io.DefaultSerializer;
import com.tangosol.io.NamedSerializerFactory;
import com.tangosol.io.Serializer;

import com.tangosol.io.pof.ConfigurablePofContext;

import com.tangosol.net.AsyncNamedCache;
import com.tangosol.net.BackingMapManager;
import com.tangosol.net.BackingMapManagerContext;
import com.tangosol.net.CacheService;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.NamedCache;

import com.tangosol.net.cache.CacheEvent;
import com.tangosol.net.cache.WrapperNamedCache;

import com.tangosol.net.grpc.GrpcDependencies;
import com.tangosol.util.Base;
import com.tangosol.util.Binary;
import com.tangosol.util.Converter;
import com.tangosol.util.ConverterCollections;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.Filter;
import com.tangosol.util.MapEvent;
import com.tangosol.util.MapListener;
import com.tangosol.util.MapListenerSupport;
import com.tangosol.util.MapTrigger;
import com.tangosol.util.MapTriggerListener;
import com.tangosol.util.NullImplementation;
import com.tangosol.util.ResourceRegistry;
import com.tangosol.util.SimpleResourceRegistry;

import com.tangosol.util.filter.EqualsFilter;
import com.tangosol.util.filter.InKeySetFilter;

import grpc.proxy.TestNamedCacheServiceProvider;
import grpc.proxy.TestStreamObserver;
import io.grpc.Status;

import java.io.Serializable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Optional;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;

import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;

import static org.hamcrest.MatcherAssert.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.same;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Jonathan Knight  2019.12.03
 * @since 20.06
 */
@SuppressWarnings({"unchecked", "rawtypes"})
class MapListenerProxyTest
    {
    // ----- test lifecycle -------------------------------------------------

    @BeforeAll
    static void setup()
        {
        s_binary1    = ExternalizableHelper.toBinary(ONE, SERIALIZER);
        s_binaryKey1 = s_binary1;
        s_bytesKey1  = BinaryHelper.toByteString(s_binaryKey1);
        s_bytes1     = BinaryHelper.toByteString(ONE, SERIALIZER);
        s_binary2    = ExternalizableHelper.toBinary(TWO, SERIALIZER);
        s_bytes2     = BinaryHelper.toByteString(TWO, SERIALIZER);
        s_binary3    = ExternalizableHelper.toBinary(THREE, SERIALIZER);
        s_bytes3     = BinaryHelper.toByteString(THREE, SERIALIZER);

        s_serializerProducer = mock(NamedSerializerFactory.class);
        when(s_serializerProducer.getNamedSerializer(eq(JAVA_FORMAT), any(ClassLoader.class))).thenReturn(SERIALIZER);
        when(s_serializerProducer.getNamedSerializer(eq(POF_FORMAT), any(ClassLoader.class))).thenReturn(POF_SERIALIZER);

        s_filter              = new EqualsFilter<>("foo", "bar");
        s_filterBytes         = BinaryHelper.toByteString(s_filter, SERIALIZER);
        s_inKeySetFilter      = new InKeySetFilter<>(s_filter, Collections.singleton(s_bytes1));
        s_inKeySetFilterBytes = BinaryHelper.toByteString(s_inKeySetFilter, SERIALIZER);

        Optional<TestNamedCacheServiceProvider> optional = TestNamedCacheServiceProvider.getProvider();
        assertThat(optional.isPresent(), is(true));
        s_serviceProvider = optional.get();
        }

    @BeforeEach
    @SuppressWarnings("resource")
    void setupEach()
        {
        m_testCCF       = mock(ConfigurableCacheFactory.class);
        CacheStub cache = createCache(TEST_CACHE_NAME);
        s_namedCache    = cache.getMockCache();

        when(m_testCCF.ensureCache(eq(TEST_CACHE_NAME), any(ClassLoader.class))).thenReturn(s_namedCache);
        when(m_testCCF.getScopeName()).thenReturn(GrpcDependencies.DEFAULT_SCOPE);

        m_ccfSupplier = ConfigurableCacheFactorySuppliers.fixed(m_testCCF);
        }

    // ----- test methods ---------------------------------------------------

    @Test
    public void shouldNotAddMapListenerIfRequestHasNoCacheName()
        {
        NamedCacheService.DefaultDependencies deps = new NamedCacheService.DefaultDependencies(GrpcDependencies.ServerType.Asynchronous);
        deps.setConfigurableCacheFactorySupplier(m_ccfSupplier);
        deps.setSerializerFactory(s_serializerProducer);

        NamedCacheService service = s_serviceProvider.getService(deps);

        TestStreamObserver<MapListenerResponse> observer = new TestStreamObserver<>();
        MapListenerProxy                        proxy    = new MapListenerProxy(service, observer, NamedCacheService.Dependencies.NO_EVENTS_HEARTBEAT);
        MapListenerRequest                      request = MapListenerRequest.newBuilder().setUid("foo").build();
        proxy.onNext(request);

        observer.awaitCount(1)
                .assertNoErrors()
                .assertNotComplete();

        MapListenerResponse response = observer.valueAt(0);
        assertThat(response.getResponseTypeCase(), is(MapListenerResponse.ResponseTypeCase.ERROR));
        MapListenerErrorResponse error = response.getError();
        assertThat(error.getMessage(), endsWith(INVALID_CACHE_NAME_MESSAGE));
        assertThat(error.getCode(),    is(Status.Code.INVALID_ARGUMENT.value()));
        assertThat(error.getUid(),     is(request.getUid()));
        }

    @Test
    public void shouldAddMapListenerForKey()
        {
        NamedCacheService.DefaultDependencies deps = new NamedCacheService.DefaultDependencies(GrpcDependencies.ServerType.Asynchronous);
        deps.setConfigurableCacheFactorySupplier(m_ccfSupplier);
        deps.setSerializerFactory(s_serializerProducer);

        NamedCacheService service = s_serviceProvider.getService(deps);
        TestStreamObserver<MapListenerResponse> observer = new TestStreamObserver<>();
        MapListenerProxy                        proxy    = new MapListenerProxy(service, observer, NamedCacheService.Dependencies.NO_EVENTS_HEARTBEAT);
        MapListenerRequest                      request  = Requests.addKeyMapListener(GrpcDependencies.DEFAULT_SCOPE,
                                                                                      TEST_CACHE_NAME, JAVA_FORMAT,
                                                                                      s_bytes1, false, false,
                                                                                      ByteString.EMPTY);
        proxy.onNext(request);

        observer.awaitCount(1)
                .assertValueCount(1)
                .assertNoErrors()
                .assertNotComplete();

        MapListenerResponse response = observer.values().get(0);
        assertThat(response, is(notNullValue()));
        assertThat(response.getResponseTypeCase(), is(MapListenerResponse.ResponseTypeCase.SUBSCRIBED));
        MapListenerSubscribedResponse subscribed = response.getSubscribed();
        assertThat(subscribed.getUid(), is(request.getUid()));

        ArgumentCaptor<MapListener> captor = ArgumentCaptor.forClass(MapListener.class);
        verify(s_namedCache).addMapListener(captor.capture(), eq(ONE), eq(false));
        }

    @Test
    public void shouldNotAddMapListenerForDifferentCache()
        {
        NamedCacheService.DefaultDependencies deps = new NamedCacheService.DefaultDependencies(GrpcDependencies.ServerType.Asynchronous);
        deps.setConfigurableCacheFactorySupplier(m_ccfSupplier);
        deps.setSerializerFactory(s_serializerProducer);

        NamedCacheService service = s_serviceProvider.getService(deps);
        TestStreamObserver<MapListenerResponse> observer = new TestStreamObserver<>();
        MapListenerProxy                        proxy    = new MapListenerProxy(service, observer, NamedCacheService.Dependencies.NO_EVENTS_HEARTBEAT);
        proxy.onNext(Requests.addKeyMapListener(GrpcDependencies.DEFAULT_SCOPE,
                                                TEST_CACHE_NAME, JAVA_FORMAT, s_bytes1,
                                                false, false, ByteString.EMPTY));

        observer.awaitCount(1)
                .assertValueCount(1)
                .assertNoErrors()
                .assertNotComplete();

        MapListenerRequest request = Requests.addKeyMapListener(GrpcDependencies.DEFAULT_SCOPE,
                                                                "foo", JAVA_FORMAT, s_bytes1,
                                                                false, false, ByteString.EMPTY);
        proxy.onNext(request);

        observer.awaitCount(2)
                .assertValueCount(2)
                .assertNoErrors()
                .assertNotComplete();

        MapListenerResponse response = observer.values().get(1);
        assertThat(response, is(notNullValue()));
        assertThat(response.getResponseTypeCase(), is(MapListenerResponse.ResponseTypeCase.ERROR));
        MapListenerErrorResponse error = response.getError();
        assertThat(error.getCode(), is(Status.Code.INVALID_ARGUMENT.value()));
        assertThat(error.getUid(), is(request.getUid()));
        }

    @Test
    public void shouldAddMapListenerForKeyAndLiteEvents()
        {
        NamedCacheService.DefaultDependencies deps = new NamedCacheService.DefaultDependencies(GrpcDependencies.ServerType.Asynchronous);
        deps.setConfigurableCacheFactorySupplier(m_ccfSupplier);
        deps.setSerializerFactory(s_serializerProducer);

        NamedCacheService service = s_serviceProvider.getService(deps);
        TestStreamObserver<MapListenerResponse> observer = new TestStreamObserver<>();
        MapListenerProxy                        proxy    = new MapListenerProxy(service, observer, NamedCacheService.Dependencies.NO_EVENTS_HEARTBEAT);
        MapListenerRequest                      request  = Requests.addKeyMapListener(GrpcDependencies.DEFAULT_SCOPE,
                                                                                      TEST_CACHE_NAME, JAVA_FORMAT,
                                                                                      s_bytes1, true, false,
                                                                                      ByteString.EMPTY);
        proxy.onNext(request);

        observer.awaitCount(1)
                .assertValueCount(1)
                .assertNoErrors()
                .assertNotComplete();

        MapListenerResponse response = observer.values().get(0);
        assertThat(response, is(notNullValue()));
        assertThat(response.getResponseTypeCase(), is(MapListenerResponse.ResponseTypeCase.SUBSCRIBED));
        MapListenerSubscribedResponse subscribed = response.getSubscribed();
        assertThat(subscribed.getUid(), is(request.getUid()));

        ArgumentCaptor<MapListener> captor = ArgumentCaptor.forClass(MapListener.class);
        verify(s_namedCache).addMapListener(captor.capture(), eq(ONE), eq(true));
        }

    @Test
    public void shouldRemoveMapListenerForKey()
        {
        NamedCacheService.DefaultDependencies deps = new NamedCacheService.DefaultDependencies(GrpcDependencies.ServerType.Asynchronous);
        deps.setConfigurableCacheFactorySupplier(m_ccfSupplier);
        deps.setSerializerFactory(s_serializerProducer);

        NamedCacheService service = s_serviceProvider.getService(deps);
        TestStreamObserver<MapListenerResponse> observer = new TestStreamObserver<>();
        MapListenerProxy                        proxy = new MapListenerProxy(service, observer, NamedCacheService.Dependencies.NO_EVENTS_HEARTBEAT);
        proxy.onNext(Requests.addKeyMapListener(GrpcDependencies.DEFAULT_SCOPE,
                                                TEST_CACHE_NAME, JAVA_FORMAT, s_bytes1,
                                                true, false, ByteString.EMPTY));

        observer.awaitCount(1)
                .assertValueCount(1)
                .assertNoErrors()
                .assertNotComplete();

        MapListenerRequest request = Requests.removeKeyMapListener(GrpcDependencies.DEFAULT_SCOPE,
                                                                   TEST_CACHE_NAME, JAVA_FORMAT,
                                                                   s_bytes1, false, ByteString.EMPTY);
        proxy.onNext(request);

        observer.awaitCount(2)
                .assertValueCount(2)
                .assertNoErrors()
                .assertNotComplete();

        MapListenerResponse response = observer.values().get(1);
        assertThat(response, is(notNullValue()));
        assertThat(response.getResponseTypeCase(), is(MapListenerResponse.ResponseTypeCase.UNSUBSCRIBED));
        MapListenerUnsubscribedResponse unsubscribed = response.getUnsubscribed();
        assertThat(unsubscribed.getUid(), is(request.getUid()));

        ArgumentCaptor<MapListener> captor = ArgumentCaptor.forClass(MapListener.class);
        verify(s_namedCache).removeMapListener(captor.capture(), eq(ONE));
        }

    @Test
    public void shouldAddPrimingMapListenerForKey()
        {
        NamedCacheService.DefaultDependencies deps = new NamedCacheService.DefaultDependencies(GrpcDependencies.ServerType.Asynchronous);
        deps.setConfigurableCacheFactorySupplier(m_ccfSupplier);
        deps.setSerializerFactory(s_serializerProducer);

        NamedCacheService service = s_serviceProvider.getService(deps);
        TestStreamObserver<MapListenerResponse> observer = new TestStreamObserver<>();
        MapListenerProxy                        proxy    = new MapListenerProxy(service, observer, NamedCacheService.Dependencies.NO_EVENTS_HEARTBEAT);
        MapListenerRequest                      request  = Requests.addKeyMapListener(GrpcDependencies.DEFAULT_SCOPE,
                                                                                      TEST_CACHE_NAME, JAVA_FORMAT,
                                                                                      s_bytes1, false, true,
                                                                                      ByteString.EMPTY);
        proxy.onNext(request);

        observer.awaitCount(1)
                .assertValueCount(1)
                .assertNoErrors()
                .assertNotComplete();

        MapListenerResponse response = observer.values().get(0);
        assertThat(response, is(notNullValue()));
        assertThat(response.getResponseTypeCase(), is(MapListenerResponse.ResponseTypeCase.SUBSCRIBED));
        MapListenerSubscribedResponse subscribed = response.getSubscribed();
        assertThat(subscribed.getUid(), is(request.getUid()));

        ArgumentCaptor<MapListener> captor = ArgumentCaptor.forClass(MapListener.class);
        verify(s_namedCache).addMapListener(captor.capture(), eq(ONE), eq(false));
        MapListener listener = captor.getValue();
        assertThat(MapListenerSupport.isPrimingListener(listener), is(true));
        }

    @Test
    public void shouldAddSecondPrimingMapListenerForKey()
        {
        NamedCacheService.DefaultDependencies deps = new NamedCacheService.DefaultDependencies(GrpcDependencies.ServerType.Asynchronous);
        deps.setConfigurableCacheFactorySupplier(m_ccfSupplier);
        deps.setSerializerFactory(s_serializerProducer);

        NamedCacheService service = s_serviceProvider.getService(deps);
        TestStreamObserver<MapListenerResponse> observer = new TestStreamObserver<>();
        MapListenerProxy                        proxy    = new MapListenerProxy(service, observer, NamedCacheService.Dependencies.NO_EVENTS_HEARTBEAT);
        MapListenerRequest                      request1 = Requests.addKeyMapListener(GrpcDependencies.DEFAULT_SCOPE,
                                                                                      TEST_CACHE_NAME, JAVA_FORMAT,
                                                                                      s_bytes1, true, true,
                                                                                      ByteString.EMPTY);
        MapListenerRequest                      request2 = Requests.addKeyMapListener(GrpcDependencies.DEFAULT_SCOPE,
                                                                                      TEST_CACHE_NAME, JAVA_FORMAT,
                                                                                      s_bytes1, true, true,
                                                                                      ByteString.EMPTY);
        proxy.onNext(request1);

        observer.awaitCount(1)
                .assertValueCount(1)
                .assertNoErrors()
                .assertNotComplete();

        proxy.onNext(request2);

        observer.awaitCount(3)
                .assertValueCount(3)
                .assertNoErrors()
                .assertNotComplete();

        MapListenerResponse response = observer.values().get(0);
        assertThat(response, is(notNullValue()));
        assertThat(response.getResponseTypeCase(), is(MapListenerResponse.ResponseTypeCase.SUBSCRIBED));
        MapListenerSubscribedResponse subscribed = response.getSubscribed();
        assertThat(subscribed.getUid(), is(request1.getUid()));

        response = observer.values().get(1);
        assertThat(response, is(notNullValue()));
        assertThat(response.getResponseTypeCase(), is(MapListenerResponse.ResponseTypeCase.EVENT));

        response = observer.values().get(2);
        assertThat(response, is(notNullValue()));
        assertThat(response.getResponseTypeCase(), is(MapListenerResponse.ResponseTypeCase.SUBSCRIBED));
        subscribed = response.getSubscribed();
        assertThat(subscribed.getUid(), is(request2.getUid()));

        ArgumentCaptor<MapListener> captor = ArgumentCaptor.forClass(MapListener.class);
        verify(s_namedCache).addMapListener(captor.capture(), eq(ONE), eq(true));
        MapListener listener = captor.getValue();
        assertThat(MapListenerSupport.isPrimingListener(listener), is(true));
        }

    @Test
    public void shouldRemovePrimingMapListenerForKey()
        {
        NamedCacheService.DefaultDependencies deps = new NamedCacheService.DefaultDependencies(GrpcDependencies.ServerType.Asynchronous);
        deps.setConfigurableCacheFactorySupplier(m_ccfSupplier);
        deps.setSerializerFactory(s_serializerProducer);

        NamedCacheService service = s_serviceProvider.getService(deps);
        TestStreamObserver<MapListenerResponse> observer = new TestStreamObserver<>();
        MapListenerProxy                        proxy    = new MapListenerProxy(service, observer, NamedCacheService.Dependencies.NO_EVENTS_HEARTBEAT);

        proxy.onNext(Requests.addKeyMapListener(GrpcDependencies.DEFAULT_SCOPE,
                                                TEST_CACHE_NAME, JAVA_FORMAT, s_bytes1,
                                                false, true, ByteString.EMPTY));

        observer.awaitCount(1)
                .assertValueCount(1)
                .assertNoErrors()
                .assertNotComplete();

        MapListenerRequest request = Requests.removeKeyMapListener(GrpcDependencies.DEFAULT_SCOPE,
                                                                   TEST_CACHE_NAME, JAVA_FORMAT, s_bytes1,
                                                                   true, ByteString.EMPTY);
        proxy.onNext(request);

        observer.awaitCount(2)
                .assertValueCount(2)
                .assertNoErrors()
                .assertNotComplete();

        MapListenerResponse response = observer.values().get(1);
        assertThat(response, is(notNullValue()));
        assertThat(response.getResponseTypeCase(), is(MapListenerResponse.ResponseTypeCase.UNSUBSCRIBED));
        MapListenerUnsubscribedResponse unsubscribed = response.getUnsubscribed();
        assertThat(unsubscribed.getUid(), is(request.getUid()));

        ArgumentCaptor<MapListener> captor = ArgumentCaptor.forClass(MapListener.class);
        verify(s_namedCache).removeMapListener(captor.capture(), eq(ONE));
        MapListener listener = captor.getValue();
        assertThat(MapListenerSupport.isPrimingListener(listener), is(true));
        }

    @Test
    public void shouldAddMapTriggerListenerForKey()
        {
        NamedCacheService.DefaultDependencies deps = new NamedCacheService.DefaultDependencies(GrpcDependencies.ServerType.Asynchronous);
        deps.setConfigurableCacheFactorySupplier(m_ccfSupplier);
        deps.setSerializerFactory(s_serializerProducer);

        NamedCacheService service = s_serviceProvider.getService(deps);
        MapTrigger                 trigger = new MapTriggerStub();
        ByteString                              triggerBytes = BinaryHelper.toByteString(trigger, SERIALIZER);
        TestStreamObserver<MapListenerResponse> observer     = new TestStreamObserver<>();
        MapListenerProxy                        proxy        = new MapListenerProxy(service, observer, NamedCacheService.Dependencies.NO_EVENTS_HEARTBEAT);
        MapListenerRequest                      request      = Requests.addKeyMapListener(GrpcDependencies.DEFAULT_SCOPE,
                                                                                          TEST_CACHE_NAME, JAVA_FORMAT,
                                                                                          s_bytes1, false, false,
                                                                                          triggerBytes);
        proxy.onNext(request);

        observer.awaitCount(1)
                .assertValueCount(1)
                .assertNoErrors()
                .assertNotComplete();

        MapListenerResponse response = observer.values().get(0);
        assertThat(response, is(notNullValue()));
        assertThat(response.getResponseTypeCase(), is(MapListenerResponse.ResponseTypeCase.SUBSCRIBED));
        MapListenerSubscribedResponse subscribed = response.getSubscribed();
        assertThat(subscribed.getUid(), is(request.getUid()));

        ArgumentCaptor<MapListener> captor = ArgumentCaptor.forClass(MapListener.class);
        verify(s_namedCache).addMapListener(captor.capture(), eq(ONE), eq(false));
        MapListener listener = captor.getValue();
        assertThat(listener, is(instanceOf(MapTriggerListener.class)));
        assertThat(((MapTriggerListener) listener).getTrigger(), is(instanceOf(MapTriggerStub.class)));
        }

    @Test
    public void shouldRemoveMapTriggerListenerForKey()
        {
        NamedCacheService.DefaultDependencies deps = new NamedCacheService.DefaultDependencies(GrpcDependencies.ServerType.Asynchronous);
        deps.setConfigurableCacheFactorySupplier(m_ccfSupplier);
        deps.setSerializerFactory(s_serializerProducer);

        NamedCacheService service = s_serviceProvider.getService(deps);
        MapTrigger                 trigger = new MapTriggerStub();
        ByteString                              triggerBytes = BinaryHelper.toByteString(trigger, SERIALIZER);
        TestStreamObserver<MapListenerResponse> observer     = new TestStreamObserver<>();
        MapListenerProxy                        proxy        = new MapListenerProxy(service, observer, NamedCacheService.Dependencies.NO_EVENTS_HEARTBEAT);

        proxy.onNext(Requests.addKeyMapListener(GrpcDependencies.DEFAULT_SCOPE,
                                                TEST_CACHE_NAME, JAVA_FORMAT, s_bytes1,
                                                false, false, triggerBytes));

        observer.awaitCount(1)
                .assertValueCount(1)
                .assertNoErrors()
                .assertNotComplete();

        MapListenerRequest request = Requests.removeKeyMapListener(GrpcDependencies.DEFAULT_SCOPE,
                                                                   TEST_CACHE_NAME, JAVA_FORMAT, s_bytes1,
                                                                   false, triggerBytes);
        proxy.onNext(request);

        observer.awaitCount(2)
                .assertValueCount(2)
                .assertNoErrors()
                .assertNotComplete();

        MapListenerResponse response = observer.values().get(1);
        assertThat(response, is(notNullValue()));
        assertThat(response.getResponseTypeCase(), is(MapListenerResponse.ResponseTypeCase.UNSUBSCRIBED));
        MapListenerUnsubscribedResponse unsubscribed = response.getUnsubscribed();
        assertThat(unsubscribed.getUid(), is(request.getUid()));

        ArgumentCaptor<MapListener> captor = ArgumentCaptor.forClass(MapListener.class);
        verify(s_namedCache).removeMapListener(captor.capture(), eq(ONE));
        MapListener listener = captor.getValue();
        assertThat(listener, is(instanceOf(MapTriggerListener.class)));
        assertThat(((MapTriggerListener) listener).getTrigger(), is(instanceOf(MapTriggerStub.class)));
        }

    @Test
    public void shouldAddMapListenerForFilter()
        {
        NamedCacheService.DefaultDependencies deps = new NamedCacheService.DefaultDependencies(GrpcDependencies.ServerType.Asynchronous);
        deps.setConfigurableCacheFactorySupplier(m_ccfSupplier);
        deps.setSerializerFactory(s_serializerProducer);

        NamedCacheService service = s_serviceProvider.getService(deps);
        TestStreamObserver<MapListenerResponse> observer = new TestStreamObserver<>();
        MapListenerProxy                        proxy    = new MapListenerProxy(service, observer, NamedCacheService.Dependencies.NO_EVENTS_HEARTBEAT);
        MapListenerRequest                      request  = Requests.addFilterMapListener(GrpcDependencies.DEFAULT_SCOPE,
                                                                                         TEST_CACHE_NAME, JAVA_FORMAT,
                                                                                         s_filterBytes, 1L, false,
                                                                                         false,
                                                                                         ByteString.EMPTY);
        proxy.onNext(request);

        observer.awaitCount(1)
                .assertValueCount(1)
                .assertNoErrors()
                .assertNotComplete();

        MapListenerResponse response = observer.values().get(0);
        assertThat(response, is(notNullValue()));
        assertThat(response.getResponseTypeCase(), is(MapListenerResponse.ResponseTypeCase.SUBSCRIBED));
        MapListenerSubscribedResponse subscribed = response.getSubscribed();
        assertThat(subscribed.getUid(), is(request.getUid()));

        ArgumentCaptor<MapListener> captor = ArgumentCaptor.forClass(MapListener.class);
        verify(s_namedCache).addMapListener(captor.capture(), eq(s_filter), eq(false));
        }

    @Test
    public void shouldAddMapListenerForFilterAndLiteEvents()
        {
        NamedCacheService.DefaultDependencies deps = new NamedCacheService.DefaultDependencies(GrpcDependencies.ServerType.Asynchronous);
        deps.setConfigurableCacheFactorySupplier(m_ccfSupplier);
        deps.setSerializerFactory(s_serializerProducer);

        NamedCacheService service = s_serviceProvider.getService(deps);
        TestStreamObserver<MapListenerResponse> observer = new TestStreamObserver<>();
        MapListenerProxy                        proxy    = new MapListenerProxy(service, observer, NamedCacheService.Dependencies.NO_EVENTS_HEARTBEAT);
        MapListenerRequest                      request  = Requests.addFilterMapListener(GrpcDependencies.DEFAULT_SCOPE,
                                                                                         TEST_CACHE_NAME, JAVA_FORMAT,
                                                                                         s_filterBytes, 1L, true, false,
                                                                                         ByteString.EMPTY);
        proxy.onNext(request);

        observer.awaitCount(1)
                .assertValueCount(1)
                .assertNoErrors()
                .assertNotComplete();

        MapListenerResponse response = observer.values().get(0);
        assertThat(response, is(notNullValue()));
        assertThat(response.getResponseTypeCase(), is(MapListenerResponse.ResponseTypeCase.SUBSCRIBED));
        MapListenerSubscribedResponse subscribed = response.getSubscribed();
        assertThat(subscribed.getUid(), is(request.getUid()));

        ArgumentCaptor<MapListener> captor = ArgumentCaptor.forClass(MapListener.class);
        verify(s_namedCache).addMapListener(captor.capture(), eq(s_filter), eq(true));
        }

    @Test
    public void shouldRemoveMapListenerForFilter()
        {
        NamedCacheService.DefaultDependencies deps = new NamedCacheService.DefaultDependencies(GrpcDependencies.ServerType.Asynchronous);
        deps.setConfigurableCacheFactorySupplier(m_ccfSupplier);
        deps.setSerializerFactory(s_serializerProducer);

        NamedCacheService service = s_serviceProvider.getService(deps);
        TestStreamObserver<MapListenerResponse> observer = new TestStreamObserver<>();
        MapListenerProxy                        proxy    = new MapListenerProxy(service, observer, NamedCacheService.Dependencies.NO_EVENTS_HEARTBEAT);

        proxy.onNext(Requests.addFilterMapListener(GrpcDependencies.DEFAULT_SCOPE,
                                                   TEST_CACHE_NAME, JAVA_FORMAT, s_filterBytes,
                                                   1L, false, false, ByteString.EMPTY));

        observer.awaitCount(1)
                .assertValueCount(1)
                .assertNoErrors()
                .assertNotComplete();

        MapListenerRequest request = Requests.removeFilterMapListener(GrpcDependencies.DEFAULT_SCOPE,
                                                                      TEST_CACHE_NAME, JAVA_FORMAT, s_filterBytes,
                                                                      1L, false, false, ByteString.EMPTY);
        proxy.onNext(request);

        observer.awaitCount(2)
                .assertValueCount(2)
                .assertNoErrors()
                .assertNotComplete();

        MapListenerResponse response = observer.values().get(1);
        assertThat(response, is(notNullValue()));
        assertThat(response.getResponseTypeCase(), is(MapListenerResponse.ResponseTypeCase.UNSUBSCRIBED));
        MapListenerUnsubscribedResponse unsubscribed = response.getUnsubscribed();
        assertThat(unsubscribed.getUid(), is(request.getUid()));

        ArgumentCaptor<MapListener> captor = ArgumentCaptor.forClass(MapListener.class);
        verify(s_namedCache).removeMapListener(captor.capture(), eq(s_filter));
        }

    @Test
    public void shouldAddPrimingMapListenerForFilter()
        {
        NamedCacheService.DefaultDependencies deps = new NamedCacheService.DefaultDependencies(GrpcDependencies.ServerType.Asynchronous);
        deps.setConfigurableCacheFactorySupplier(m_ccfSupplier);
        deps.setSerializerFactory(s_serializerProducer);

        NamedCacheService service = s_serviceProvider.getService(deps);
        TestStreamObserver<MapListenerResponse> observer = new TestStreamObserver<>();
        MapListenerProxy                        proxy    = new MapListenerProxy(service, observer, NamedCacheService.Dependencies.NO_EVENTS_HEARTBEAT);
        MapListenerRequest                      request  = Requests.addFilterMapListener(GrpcDependencies.DEFAULT_SCOPE,
                                                                                         TEST_CACHE_NAME, JAVA_FORMAT,
                                                                                         s_inKeySetFilterBytes, 1L,
                                                                                         false, true, ByteString.EMPTY);
        proxy.onNext(request);

        observer.awaitCount(1)
                .assertValueCount(1)
                .assertNoErrors()
                .assertNotComplete();

        MapListenerResponse response = observer.values().get(0);
        assertThat(response, is(notNullValue()));
        assertThat(response.getResponseTypeCase(), is(MapListenerResponse.ResponseTypeCase.SUBSCRIBED));
        MapListenerSubscribedResponse subscribed = response.getSubscribed();
        assertThat(subscribed.getUid(), is(request.getUid()));

        ArgumentCaptor<MapListener> captor = ArgumentCaptor.forClass(MapListener.class);
        verify(s_namedCache).addMapListener(captor.capture(), isA(InKeySetFilter.class), eq(false));
        MapListener listener = captor.getValue();
        assertThat(MapListenerSupport.isPrimingListener(listener), is(true));
        }

    @Test
    public void shouldNotAddPrimingMapListenerForNonInKeySetFilter()
        {
        NamedCacheService.DefaultDependencies deps = new NamedCacheService.DefaultDependencies(GrpcDependencies.ServerType.Asynchronous);
        deps.setConfigurableCacheFactorySupplier(m_ccfSupplier);
        deps.setSerializerFactory(s_serializerProducer);

        NamedCacheService service = s_serviceProvider.getService(deps);
        TestStreamObserver<MapListenerResponse> observer = new TestStreamObserver<>();
        MapListenerProxy                        proxy    = new MapListenerProxy(service, observer, NamedCacheService.Dependencies.NO_EVENTS_HEARTBEAT);
        MapListenerRequest                      request  = Requests.addFilterMapListener(GrpcDependencies.DEFAULT_SCOPE,
                                                                                         TEST_CACHE_NAME, JAVA_FORMAT,
                                                                                         s_filterBytes, 1L, false, true,
                                                                                         ByteString.EMPTY);
        proxy.onNext(request);

        observer.awaitCount(1)
                .assertValueCount(1)
                .assertNoErrors()
                .assertNotComplete();

        MapListenerResponse response = observer.valueAt(0);
        assertThat(response.getResponseTypeCase(), is(MapListenerResponse.ResponseTypeCase.ERROR));
        MapListenerErrorResponse error = response.getError();
        assertThat(error.getCode(), is(Status.Code.INVALID_ARGUMENT.value()));
        assertThat(error.getUid(), is(request.getUid()));
        }

    @Test
    public void shouldRemovePrimingMapListenerForFilter()
        {
        NamedCacheService.DefaultDependencies deps = new NamedCacheService.DefaultDependencies(GrpcDependencies.ServerType.Asynchronous);
        deps.setConfigurableCacheFactorySupplier(m_ccfSupplier);
        deps.setSerializerFactory(s_serializerProducer);

        NamedCacheService service = s_serviceProvider.getService(deps);
        TestStreamObserver<MapListenerResponse> observer = new TestStreamObserver<>();
        MapListenerProxy                        proxy = new MapListenerProxy(service, observer, NamedCacheService.Dependencies.NO_EVENTS_HEARTBEAT);

        proxy.onNext(Requests.addFilterMapListener(GrpcDependencies.DEFAULT_SCOPE,
                                                   TEST_CACHE_NAME, JAVA_FORMAT, s_inKeySetFilterBytes,
                                                   1L, false, true, ByteString.EMPTY));

        observer.awaitCount(1)
                .assertValueCount(1)
                .assertNoErrors()
                .assertNotComplete();

        MapListenerRequest request = Requests.removeFilterMapListener(GrpcDependencies.DEFAULT_SCOPE,
                                                                      TEST_CACHE_NAME, JAVA_FORMAT,
                                                                      s_inKeySetFilterBytes, 1L, false, true, ByteString.EMPTY);
        proxy.onNext(request);

        observer.awaitCount(2)
                .assertValueCount(2)
                .assertNoErrors()
                .assertNotComplete();

        MapListenerResponse response = observer.values().get(1);
        assertThat(response, is(notNullValue()));
        assertThat(response.getResponseTypeCase(), is(MapListenerResponse.ResponseTypeCase.UNSUBSCRIBED));
        MapListenerUnsubscribedResponse unsubscribed = response.getUnsubscribed();
        assertThat(unsubscribed.getUid(), is(request.getUid()));

        ArgumentCaptor<MapListener> captor = ArgumentCaptor.forClass(MapListener.class);
        verify(s_namedCache).removeMapListener(captor.capture(), isA(InKeySetFilter.class));
        MapListener listener = captor.getValue();
        assertThat(MapListenerSupport.isPrimingListener(listener), is(true));
        }

    @Test
    public void shouldAddMapTriggerListenerForFilter()
        {
        NamedCacheService.DefaultDependencies deps = new NamedCacheService.DefaultDependencies(GrpcDependencies.ServerType.Asynchronous);
        deps.setConfigurableCacheFactorySupplier(m_ccfSupplier);
        deps.setSerializerFactory(s_serializerProducer);

        NamedCacheService service = s_serviceProvider.getService(deps);
        MapTrigger                 trigger = new MapTriggerStub();
        ByteString                              triggerBytes = BinaryHelper.toByteString(trigger, SERIALIZER);
        TestStreamObserver<MapListenerResponse> observer     = new TestStreamObserver<>();
        MapListenerProxy                        proxy        = new MapListenerProxy(service, observer, NamedCacheService.Dependencies.NO_EVENTS_HEARTBEAT);

        MapListenerRequest request = Requests.addFilterMapListener(GrpcDependencies.DEFAULT_SCOPE,
                                                                   TEST_CACHE_NAME, JAVA_FORMAT, s_filterBytes,
                                                                   1L, false, false, triggerBytes);
        proxy.onNext(request);

        observer.awaitCount(1)
                .assertValueCount(1)
                .assertNoErrors()
                .assertNotComplete();

        MapListenerResponse response = observer.values().get(0);
        assertThat(response, is(notNullValue()));
        assertThat(response.getResponseTypeCase(), is(MapListenerResponse.ResponseTypeCase.SUBSCRIBED));
        MapListenerSubscribedResponse subscribed = response.getSubscribed();
        assertThat(subscribed.getUid(), is(request.getUid()));

        ArgumentCaptor<MapListener> captor = ArgumentCaptor.forClass(MapListener.class);
        verify(s_namedCache).addMapListener(captor.capture(), eq(s_filter), eq(false));
        MapListener listener = captor.getValue();
        assertThat(listener, is(instanceOf(MapTriggerListener.class)));
        assertThat(((MapTriggerListener) listener).getTrigger(), is(instanceOf(MapTriggerStub.class)));
        }

    @Test
    public void shouldRemoveMapTriggerListenerForFilter()
        {
        NamedCacheService.DefaultDependencies deps = new NamedCacheService.DefaultDependencies(GrpcDependencies.ServerType.Asynchronous);
        deps.setConfigurableCacheFactorySupplier(m_ccfSupplier);
        deps.setSerializerFactory(s_serializerProducer);

        NamedCacheService service = s_serviceProvider.getService(deps);
        MapTrigger                 trigger = new MapTriggerStub();
        ByteString                              triggerBytes = BinaryHelper.toByteString(trigger, SERIALIZER);
        TestStreamObserver<MapListenerResponse> observer     = new TestStreamObserver<>();
        MapListenerProxy                        proxy        = new MapListenerProxy(service, observer, NamedCacheService.Dependencies.NO_EVENTS_HEARTBEAT);

        proxy.onNext(Requests.addFilterMapListener(GrpcDependencies.DEFAULT_SCOPE,
                                                   TEST_CACHE_NAME, JAVA_FORMAT, s_filterBytes,
                                                   1L, false, false, triggerBytes));

        observer.awaitCount(1)
                .assertValueCount(1)
                .assertNoErrors()
                .assertNotComplete();

        MapListenerRequest request = Requests.removeFilterMapListener(GrpcDependencies.DEFAULT_SCOPE,
                                                                      TEST_CACHE_NAME, JAVA_FORMAT,
                                                                      s_filterBytes, 1L, false, false, triggerBytes);
        proxy.onNext(request);

        observer.awaitCount(2)
                .assertValueCount(2)
                .assertNoErrors()
                .assertNotComplete();

        MapListenerResponse response = observer.values().get(1);
        assertThat(response, is(notNullValue()));
        assertThat(response.getResponseTypeCase(), is(MapListenerResponse.ResponseTypeCase.UNSUBSCRIBED));
        MapListenerUnsubscribedResponse unsubscribed = response.getUnsubscribed();
        assertThat(unsubscribed.getUid(), is(request.getUid()));

        ArgumentCaptor<MapListener> captor = ArgumentCaptor.forClass(MapListener.class);
        verify(s_namedCache).removeMapListener(captor.capture(), eq(s_filter));
        MapListener listener = captor.getValue();
        assertThat(listener, is(instanceOf(MapTriggerListener.class)));
        assertThat(((MapTriggerListener) listener).getTrigger(), is(instanceOf(MapTriggerStub.class)));
        }

    @Test
    public void shouldAddDeactivationListenerOnInit()
        {
        NamedCacheService.DefaultDependencies deps = new NamedCacheService.DefaultDependencies(GrpcDependencies.ServerType.Asynchronous);
        deps.setConfigurableCacheFactorySupplier(m_ccfSupplier);
        deps.setSerializerFactory(s_serializerProducer);

        NamedCacheService service = s_serviceProvider.getService(deps);
        TestStreamObserver<MapListenerResponse> observer = new TestStreamObserver<>();
        MapListenerProxy                        proxy                = new MapListenerProxy(service, observer, NamedCacheService.Dependencies.NO_EVENTS_HEARTBEAT);
        MapListener<Object, Object>             deactivationListener = proxy.getDeactivationListener();
        proxy.onNext(Requests.initListenerChannel(GrpcDependencies.DEFAULT_SCOPE,TEST_CACHE_NAME, JAVA_FORMAT));

        observer.awaitCount(1)
                .assertValueCount(1)
                .assertNoErrors();

        verify(s_namedCache).addMapListener(same(deactivationListener));
        }

    @Test
    public void shouldRemoveDeactivationListenerOnCompleted()
        {
        NamedCacheService.DefaultDependencies deps = new NamedCacheService.DefaultDependencies(GrpcDependencies.ServerType.Asynchronous);
        deps.setConfigurableCacheFactorySupplier(m_ccfSupplier);
        deps.setSerializerFactory(s_serializerProducer);

        NamedCacheService service = s_serviceProvider.getService(deps);
        TestStreamObserver<MapListenerResponse> observer = new TestStreamObserver<>();
        MapListenerProxy                        proxy                = new MapListenerProxy(service, observer, NamedCacheService.Dependencies.NO_EVENTS_HEARTBEAT);
        MapListener<Object, Object>             deactivationListener = proxy.getDeactivationListener();
        proxy.onNext(Requests.initListenerChannel(GrpcDependencies.DEFAULT_SCOPE,TEST_CACHE_NAME, JAVA_FORMAT));

        observer.awaitCount(1)
                .assertValueCount(1)
                .assertNoErrors();

        proxy.onCompleted();

        InOrder inOrder = Mockito.inOrder(s_namedCache);
        inOrder.verify(s_namedCache).addMapListener(same(deactivationListener));
        inOrder.verify(s_namedCache).removeMapListener(same(deactivationListener));
        }

    @Test
    public void shouldRemoveMapListenerForKeyOnCompleted()
        {
        NamedCacheService.DefaultDependencies deps = new NamedCacheService.DefaultDependencies(GrpcDependencies.ServerType.Asynchronous);
        deps.setConfigurableCacheFactorySupplier(m_ccfSupplier);
        deps.setSerializerFactory(s_serializerProducer);

        NamedCacheService service = s_serviceProvider.getService(deps);
        TestStreamObserver<MapListenerResponse> observer = new TestStreamObserver<>();
        MapListenerProxy                        proxy    = new MapListenerProxy(service, observer, NamedCacheService.Dependencies.NO_EVENTS_HEARTBEAT);

        proxy.onNext(Requests.addKeyMapListener(GrpcDependencies.DEFAULT_SCOPE,
                                                TEST_CACHE_NAME, JAVA_FORMAT, s_bytes1, false,
                                                false, ByteString.EMPTY));

        observer.awaitCount(1)
                .assertValueCount(1)
                .assertNoErrors();

        proxy.onCompleted();

        ArgumentCaptor<MapListener> captorAdd = ArgumentCaptor.forClass(MapListener.class);
        verify(s_namedCache).addMapListener(captorAdd.capture(), eq(ONE), eq(false));

        ArgumentCaptor<MapListener> captorRemove = ArgumentCaptor.forClass(MapListener.class);
        verify(s_namedCache).removeMapListener(captorRemove.capture(), eq(ONE));

        MapListener added   = captorAdd.getValue();
        MapListener removed = captorRemove.getValue();
        assertThat(removed, is(sameInstance(added)));
        }

    @Test
    public void shouldRemoveMapListenerForFilterOnCompleted()
        {
        NamedCacheService.DefaultDependencies deps = new NamedCacheService.DefaultDependencies(GrpcDependencies.ServerType.Asynchronous);
        deps.setConfigurableCacheFactorySupplier(m_ccfSupplier);
        deps.setSerializerFactory(s_serializerProducer);

        NamedCacheService service = s_serviceProvider.getService(deps);
        TestStreamObserver<MapListenerResponse> observer = new TestStreamObserver<>();
        MapListenerProxy                        proxy    = new MapListenerProxy(service, observer, NamedCacheService.Dependencies.NO_EVENTS_HEARTBEAT);

        proxy.onNext(Requests.addFilterMapListener(GrpcDependencies.DEFAULT_SCOPE,
                                                   TEST_CACHE_NAME, JAVA_FORMAT,
                                                   s_filterBytes, 1L, false, false, ByteString.EMPTY));

        observer.awaitCount(1)
                .assertValueCount(1)
                .assertNoErrors();

        proxy.onCompleted();

        ArgumentCaptor<MapListener> captorAdd = ArgumentCaptor.forClass(MapListener.class);
        verify(s_namedCache).addMapListener(captorAdd.capture(), eq(s_filter), eq(false));

        ArgumentCaptor<MapListener> captorRemove = ArgumentCaptor.forClass(MapListener.class);
        verify(s_namedCache).removeMapListener(captorRemove.capture(), eq(s_filter));

        MapListener added   = captorAdd.getValue();
        MapListener removed = captorRemove.getValue();
        assertThat(removed, is(sameInstance(added)));
        }

    @Test
    public void shouldNotRemoveMapListenerForKeyOnCompletedIfCacheInactive()
        {
        NamedCacheService.DefaultDependencies deps = new NamedCacheService.DefaultDependencies(GrpcDependencies.ServerType.Asynchronous);
        deps.setConfigurableCacheFactorySupplier(m_ccfSupplier);
        deps.setSerializerFactory(s_serializerProducer);

        NamedCacheService service = s_serviceProvider.getService(deps);
        TestStreamObserver<MapListenerResponse> observer = new TestStreamObserver<>();
        MapListenerProxy                        proxy    = new MapListenerProxy(service, observer, NamedCacheService.Dependencies.NO_EVENTS_HEARTBEAT);

        proxy.onNext(Requests.addKeyMapListener(GrpcDependencies.DEFAULT_SCOPE,
                                                TEST_CACHE_NAME, JAVA_FORMAT, s_bytes1,
                                                false, false, ByteString.EMPTY));

        observer.awaitCount(1)
                .assertValueCount(1)
                .assertNoErrors();

        when(s_namedCache.isActive()).thenReturn(false);

        proxy.onCompleted();

        verify(s_namedCache, never()).removeMapListener(any(MapListener.class));
        verify(s_namedCache, never()).removeMapListener(any(MapListener.class), any());
        verify(s_namedCache, never()).removeMapListener(any(MapListener.class), any(Filter.class));
        }

    @Test
    public void shouldNotRemoveMapListenerForFilterOnCompletedIfCacheInactive()
        {
        NamedCacheService.DefaultDependencies deps = new NamedCacheService.DefaultDependencies(GrpcDependencies.ServerType.Asynchronous);
        deps.setConfigurableCacheFactorySupplier(m_ccfSupplier);
        deps.setSerializerFactory(s_serializerProducer);

        NamedCacheService service = s_serviceProvider.getService(deps);
        TestStreamObserver<MapListenerResponse> observer = new TestStreamObserver<>();
        MapListenerProxy                        proxy    = new MapListenerProxy(service, observer, NamedCacheService.Dependencies.NO_EVENTS_HEARTBEAT);

        proxy.onNext(Requests.addFilterMapListener(GrpcDependencies.DEFAULT_SCOPE, TEST_CACHE_NAME, JAVA_FORMAT, s_filterBytes,
                                                   1L, false, false, ByteString.EMPTY));

        observer.awaitCount(1)
                .assertValueCount(1)
                .assertNoErrors();

        when(s_namedCache.isActive()).thenReturn(false);

        proxy.onCompleted();

        verify(s_namedCache, never()).removeMapListener(any(MapListener.class));
        verify(s_namedCache, never()).removeMapListener(any(MapListener.class), any());
        verify(s_namedCache, never()).removeMapListener(any(MapListener.class), any(Filter.class));
        }

    @Test
    public void shouldRemoveMapListenerForKeyOnError()
        {
        NamedCacheService.DefaultDependencies deps = new NamedCacheService.DefaultDependencies(GrpcDependencies.ServerType.Asynchronous);
        deps.setConfigurableCacheFactorySupplier(m_ccfSupplier);
        deps.setSerializerFactory(s_serializerProducer);

        NamedCacheService service = s_serviceProvider.getService(deps);
        TestStreamObserver<MapListenerResponse> observer = new TestStreamObserver<>();
        MapListenerProxy                        proxy    = new MapListenerProxy(service, observer, NamedCacheService.Dependencies.NO_EVENTS_HEARTBEAT);

        proxy.onNext(Requests.addKeyMapListener(GrpcDependencies.DEFAULT_SCOPE, TEST_CACHE_NAME, JAVA_FORMAT, s_bytes1,
                                                false, false, ByteString.EMPTY));

        observer.awaitCount(1)
                .assertValueCount(1)
                .assertNoErrors();

        proxy.onError(new RuntimeException("Computer says No!"));

        ArgumentCaptor<MapListener> captorAdd = ArgumentCaptor.forClass(MapListener.class);
        verify(s_namedCache).addMapListener(captorAdd.capture(), eq(ONE), eq(false));

        ArgumentCaptor<MapListener> captorRemove = ArgumentCaptor.forClass(MapListener.class);
        verify(s_namedCache).removeMapListener(captorRemove.capture(), eq(ONE));

        MapListener added   = captorAdd.getValue();
        MapListener removed = captorRemove.getValue();
        assertThat(removed, is(sameInstance(added)));
        }

    @Test
    public void shouldRemoveMapListenerForFilterOnError()
        {
        NamedCacheService.DefaultDependencies deps = new NamedCacheService.DefaultDependencies(GrpcDependencies.ServerType.Asynchronous);
        deps.setConfigurableCacheFactorySupplier(m_ccfSupplier);
        deps.setSerializerFactory(s_serializerProducer);

        NamedCacheService service = s_serviceProvider.getService(deps);
        TestStreamObserver<MapListenerResponse> observer = new TestStreamObserver<>();
        MapListenerProxy                        proxy    = new MapListenerProxy(service, observer, NamedCacheService.Dependencies.NO_EVENTS_HEARTBEAT);

        proxy.onNext(Requests.addFilterMapListener(GrpcDependencies.DEFAULT_SCOPE,
                                                   TEST_CACHE_NAME, JAVA_FORMAT, s_filterBytes,
                                                   1L, false, false, ByteString.EMPTY));

        observer.awaitCount(1)
                .assertValueCount(1)
                .assertNoErrors();

        proxy.onError(new RuntimeException("Computer says No!"));

        ArgumentCaptor<MapListener> captorAdd = ArgumentCaptor.forClass(MapListener.class);
        verify(s_namedCache).addMapListener(captorAdd.capture(), eq(s_filter), eq(false));

        ArgumentCaptor<MapListener> captorRemove = ArgumentCaptor.forClass(MapListener.class);
        verify(s_namedCache).removeMapListener(captorRemove.capture(), eq(s_filter));

        MapListener added   = captorAdd.getValue();
        MapListener removed = captorRemove.getValue();
        assertThat(removed, is(sameInstance(added)));
        }

    @Test
    public void shouldPublishInsertEvent()
        {
        NamedCacheService.DefaultDependencies deps = new NamedCacheService.DefaultDependencies(GrpcDependencies.ServerType.Asynchronous);
        deps.setConfigurableCacheFactorySupplier(m_ccfSupplier);
        deps.setSerializerFactory(s_serializerProducer);

        NamedCacheService service = s_serviceProvider.getService(deps);
        TestStreamObserver<MapListenerResponse> observer = new TestStreamObserver<>();
        MapListenerProxy                        proxy    = new MapListenerProxy(service, observer, NamedCacheService.Dependencies.NO_EVENTS_HEARTBEAT);

        proxy.onNext(Requests.addKeyMapListener(GrpcDependencies.DEFAULT_SCOPE,
                                                TEST_CACHE_NAME, JAVA_FORMAT, s_bytesKey1,
                                                false, false, ByteString.EMPTY));

        observer.awaitCount(1)
                .assertValueCount(1)
                .assertNoErrors();

        MapEvent event = new MapEvent<>(s_namedCache, MapEvent.ENTRY_INSERTED, ONE, TWO, THREE);
        proxy.entryInserted(event);

        observer.awaitCount(2)
                .assertValueCount(2)
                .assertNoErrors();

        MapListenerResponse response = observer.values().get(1);
        assertThat(response,                       is(notNullValue()));
        assertThat(response.getResponseTypeCase(), is(MapListenerResponse.ResponseTypeCase.EVENT));

        MapEventResponse eventResponse = response.getEvent();
        assertThat(eventResponse,                                                        is(notNullValue()));
        assertThat(eventResponse.getId(),                                                is(MapEvent.ENTRY_INSERTED));
        assertThat(BinaryHelper.fromByteString(eventResponse.getKey(), SERIALIZER),      is(ONE));
        assertThat(BinaryHelper.fromByteString(eventResponse.getOldValue(), SERIALIZER), is(TWO));
        assertThat(BinaryHelper.fromByteString(eventResponse.getNewValue(), SERIALIZER), is(THREE));
        assertThat(eventResponse.getSynthetic(),                                         is(false));
        assertThat(eventResponse.getPriming(),                                           is(false));
        }

    @Test
    public void shouldPublishUpdateEvent()
        {
        NamedCacheService.DefaultDependencies deps = new NamedCacheService.DefaultDependencies(GrpcDependencies.ServerType.Asynchronous);
        deps.setConfigurableCacheFactorySupplier(m_ccfSupplier);
        deps.setSerializerFactory(s_serializerProducer);

        NamedCacheService service = s_serviceProvider.getService(deps);
        TestStreamObserver<MapListenerResponse> observer = new TestStreamObserver<>();
        MapListenerProxy                        proxy    = new MapListenerProxy(service, observer, NamedCacheService.Dependencies.NO_EVENTS_HEARTBEAT);

        proxy.onNext(Requests.addKeyMapListener(GrpcDependencies.DEFAULT_SCOPE,
                                                TEST_CACHE_NAME, JAVA_FORMAT, s_bytesKey1,
                                                false, false, ByteString.EMPTY));

        observer.awaitCount(1)
                .assertValueCount(1)
                .assertNoErrors();

        MapEvent event = new MapEvent<>(s_namedCache, MapEvent.ENTRY_UPDATED, ONE, TWO, THREE);
        proxy.entryUpdated(event);

        observer.awaitCount(2)
                .assertValueCount(2)
                .assertNoErrors();

        MapListenerResponse response = observer.values().get(1);
        assertThat(response,                       is(notNullValue()));
        assertThat(response.getResponseTypeCase(), is(MapListenerResponse.ResponseTypeCase.EVENT));

        MapEventResponse eventResponse = response.getEvent();
        assertThat(eventResponse,                                                        is(notNullValue()));
        assertThat(eventResponse.getId(),                                                is(MapEvent.ENTRY_UPDATED));
        assertThat(BinaryHelper.fromByteString(eventResponse.getKey(), SERIALIZER),      is(ONE));
        assertThat(BinaryHelper.fromByteString(eventResponse.getOldValue(), SERIALIZER), is(TWO));
        assertThat(BinaryHelper.fromByteString(eventResponse.getNewValue(), SERIALIZER), is(THREE));
        assertThat(eventResponse.getSynthetic(),                                         is(false));
        assertThat(eventResponse.getPriming(),                                           is(false));
        }

    @Test
    public void shouldPublishDeleteEvent()
        {
        NamedCacheService.DefaultDependencies deps = new NamedCacheService.DefaultDependencies(GrpcDependencies.ServerType.Asynchronous);
        deps.setConfigurableCacheFactorySupplier(m_ccfSupplier);
        deps.setSerializerFactory(s_serializerProducer);

        NamedCacheService service = s_serviceProvider.getService(deps);
        TestStreamObserver<MapListenerResponse> observer = new TestStreamObserver<>();
        MapListenerProxy                        proxy    = new MapListenerProxy(service, observer, NamedCacheService.Dependencies.NO_EVENTS_HEARTBEAT);

        proxy.onNext(Requests.addKeyMapListener(GrpcDependencies.DEFAULT_SCOPE,
                                                TEST_CACHE_NAME, JAVA_FORMAT, s_bytesKey1,
                                                false, false, ByteString.EMPTY));

        observer.awaitCount(1)
                .assertValueCount(1)
                .assertNoErrors();

        MapEvent event = new MapEvent<>(s_namedCache, MapEvent.ENTRY_DELETED, ONE, TWO, THREE);
        proxy.entryDeleted(event);

        observer.awaitCount(2)
                .assertValueCount(2)
                .assertNoErrors();

        MapListenerResponse response = observer.values().get(1);
        assertThat(response,                       is(notNullValue()));
        assertThat(response.getResponseTypeCase(), is(MapListenerResponse.ResponseTypeCase.EVENT));

        MapEventResponse eventResponse = response.getEvent();
        assertThat(eventResponse,                                                        is(notNullValue()));
        assertThat(eventResponse.getId(),                                                is(MapEvent.ENTRY_DELETED));
        assertThat(BinaryHelper.fromByteString(eventResponse.getKey(), SERIALIZER),      is(ONE));
        assertThat(BinaryHelper.fromByteString(eventResponse.getOldValue(), SERIALIZER), is(TWO));
        assertThat(BinaryHelper.fromByteString(eventResponse.getNewValue(), SERIALIZER), is(THREE));
        assertThat(eventResponse.getSynthetic(),                                         is(false));
        assertThat(eventResponse.getPriming(),                                           is(false));
        }

    @Test
    public void shouldPublishCacheEvent()
        {
        NamedCacheService.DefaultDependencies deps = new NamedCacheService.DefaultDependencies(GrpcDependencies.ServerType.Asynchronous);
        deps.setConfigurableCacheFactorySupplier(m_ccfSupplier);
        deps.setSerializerFactory(s_serializerProducer);

        NamedCacheService service = s_serviceProvider.getService(deps);
        TestStreamObserver<MapListenerResponse> observer = new TestStreamObserver<>();
        MapListenerProxy                        proxy    = new MapListenerProxy(service, observer, NamedCacheService.Dependencies.NO_EVENTS_HEARTBEAT);

        proxy.onNext(Requests.addKeyMapListener(GrpcDependencies.DEFAULT_SCOPE,
                                                TEST_CACHE_NAME, JAVA_FORMAT, s_bytesKey1,
                                                false, false, ByteString.EMPTY));

        observer.awaitCount(1)
                .assertValueCount(1)
                .assertNoErrors();

        CacheEvent cacheEvent = new CacheEvent<>(s_namedCache, MapEvent.ENTRY_INSERTED, ONE, TWO, THREE,
                                                 true, CacheEvent.TransformationState.TRANSFORMABLE, true);
        MapEvent   event      = ConverterCollections.getMapEvent(s_namedCache, cacheEvent,
                                                                 NullImplementation.getConverter(),
                                                                 NullImplementation.getConverter());
        proxy.entryInserted(event);

        observer.awaitCount(2)
                .assertValueCount(2)
                .assertNoErrors();

        MapListenerResponse response = observer.values().get(1);
        assertThat(response,                       is(notNullValue()));
        assertThat(response.getResponseTypeCase(), is(MapListenerResponse.ResponseTypeCase.EVENT));

        MapEventResponse eventResponse = response.getEvent();
        assertThat(eventResponse,                                                        is(notNullValue()));
        assertThat(eventResponse.getId(),                                                is(MapEvent.ENTRY_INSERTED));
        assertThat(BinaryHelper.fromByteString(eventResponse.getKey(), SERIALIZER),      is(ONE));
        assertThat(BinaryHelper.fromByteString(eventResponse.getOldValue(), SERIALIZER), is(TWO));
        assertThat(BinaryHelper.fromByteString(eventResponse.getNewValue(), SERIALIZER), is(THREE));
        assertThat(eventResponse.getSynthetic(),                                         is(true));
        assertThat(eventResponse.getPriming(),                                           is(true));
        }

    @Test
    public void shouldProcessInsertInWrappedPrimingListener()
        {
        MapListener<Object, Object>             wrapped  = mock(MapListener.class);
        MapEvent<Object, Object>                event    = mock(MapEvent.class);
        MapListenerProxy.WrapperPrimingListener listener = new MapListenerProxy.WrapperPrimingListener(wrapped);
        listener.entryInserted(event);
        verify(wrapped).entryInserted(same(event));
        }

    @Test
    public void shouldProcessUpdateInWrappedPrimingListener()
        {
        MapListener<Object, Object>             wrapped  = mock(MapListener.class);
        MapEvent<Object, Object>                event    = mock(MapEvent.class);
        MapListenerProxy.WrapperPrimingListener listener = new MapListenerProxy.WrapperPrimingListener(wrapped);
        listener.entryUpdated(event);
        verify(wrapped).entryUpdated(same(event));
        }

    @Test
    public void shouldProcessDeleteInWrappedPrimingListener()
        {
        MapListener<Object, Object>             wrapped  = mock(MapListener.class);
        MapEvent<Object, Object>                event    = mock(MapEvent.class);
        MapListenerProxy.WrapperPrimingListener listener = new MapListenerProxy.WrapperPrimingListener(wrapped);
        listener.entryDeleted(event);
        verify(wrapped).entryDeleted(same(event));
        }

    // ----- helper methods -------------------------------------------------

    @SuppressWarnings("SameParameterValue")
    protected <K, V> CacheStub<K, V> createCache(String sName)
        {
        return new CacheStub<>(sName);
        }

    // ----- NamedCache stub ------------------------------------------------

    protected static class CacheStub<K, V>
            extends WrapperNamedCache<K, V>
        {

        private final AsyncNamedCache<K, V> async;

        private final NamedCache mockCache;

        private CacheStub(String sName)
            {
            super(new HashMap<>(), sName, mock(CacheService.class));
            async     = mock(AsyncNamedCache.class);
            mockCache = spy(this);

            when(async.getNamedCache()).thenReturn(mockCache);

            ResourceRegistry         registry     = new SimpleResourceRegistry();
            CacheService             cacheService = getCacheService();
            BackingMapManager        bmm          = mock(BackingMapManager.class);
            BackingMapManagerContext ctx          = mock(BackingMapManagerContext.class);
            when(cacheService.getResourceRegistry()).thenReturn(registry);
            when(cacheService.getSerializer()).thenReturn(CACHE_SERIALIZER);
            when(cacheService.getContextClassLoader()).thenReturn(Base.getContextClassLoader());
            when(cacheService.getBackingMapManager()).thenReturn(bmm);
            when(bmm.getContext()).thenReturn(ctx);
            when(ctx.getKeyFromInternalConverter()).thenReturn(new ConverterUp());
            when(ctx.getValueFromInternalConverter()).thenReturn(new ConverterUp());
            when(ctx.getKeyToInternalConverter()).thenReturn(new ConverterDown());
            when(ctx.getValueToInternalConverter()).thenReturn(new ConverterDown());
            }

        private NamedCache getMockCache()
            {
            return mockCache;
            }

        @Override
        public AsyncNamedCache<K, V> async()
            {
            return async;
            }

        @Override
        public AsyncNamedCache<K, V> async(AsyncNamedCache.Option... options)
            {
            return async;
            }
        }

    protected static class ConverterDown
            implements Converter<Object, Binary>
        {
        @Override
        public Binary convert(Object o)
            {
            return ExternalizableHelper.toBinary(o, CACHE_SERIALIZER);
            }
        }

    protected static class ConverterUp
            implements Converter<Binary, Object>
        {
        @Override
        public Object convert(Binary bin)
            {
            return ExternalizableHelper.fromBinary(bin, CACHE_SERIALIZER);
            }
        }

    // ----- MapTrigger Stub ------------------------------------------------

    protected static class MapTriggerStub
            implements MapTrigger, Serializable
        {

        @Override
        public void process(Entry entry)
            {
            }
        }

    // ----- constants ------------------------------------------------------

    protected static final String INVALID_CACHE_NAME_MESSAGE = "invalid request, cache name cannot be null or empty";

    protected static final String TEST_CACHE_NAME = "test-cache";

    protected static final String JAVA_FORMAT = "java";

    protected static final String POF_FORMAT = "pof";

    protected static final Serializer SERIALIZER = new DefaultSerializer();

    protected static final Serializer CACHE_SERIALIZER = new DefaultSerializer();

    protected static final Serializer POF_SERIALIZER = new ConfigurablePofContext("test-pof-config.xml");

    public static final String ONE = "one";

    public static final String TWO = "two";

    public static final String THREE = "three";

    // ----- data members ---------------------------------------------------

    protected static NamedSerializerFactory s_serializerProducer;
    
    protected static Function<String, ConfigurableCacheFactory> m_ccfSupplier;

    protected static Binary s_binary1;

    protected static Binary s_binaryKey1;

    protected static ByteString s_bytesKey1;

    protected static ByteString s_bytes1;

    protected static Binary s_binary2;

    protected static ByteString s_bytes2;

    protected static Binary s_binary3;

    protected static ByteString s_bytes3;

    protected static NamedCache s_namedCache;

    protected static Filter<Binary> s_filter;

    protected static ByteString s_filterBytes;

    protected static InKeySetFilter<Binary> s_inKeySetFilter;

    protected static ByteString s_inKeySetFilterBytes;
    
    protected static TestNamedCacheServiceProvider s_serviceProvider;

    protected ConfigurableCacheFactory m_testCCF;
    }
