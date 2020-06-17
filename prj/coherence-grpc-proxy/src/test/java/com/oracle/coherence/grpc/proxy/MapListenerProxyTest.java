/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.proxy;

import com.google.protobuf.ByteString;

import com.oracle.coherence.cdi.Scope;
import com.oracle.coherence.cdi.SerializerProducer;

import com.oracle.coherence.grpc.BinaryHelper;
import com.oracle.coherence.grpc.MapEventResponse;
import com.oracle.coherence.grpc.MapListenerErrorResponse;
import com.oracle.coherence.grpc.MapListenerRequest;
import com.oracle.coherence.grpc.MapListenerResponse;
import com.oracle.coherence.grpc.MapListenerSubscribedResponse;
import com.oracle.coherence.grpc.MapListenerUnsubscribedResponse;
import com.oracle.coherence.grpc.Requests;

import com.tangosol.io.DefaultSerializer;
import com.tangosol.io.Serializer;

import com.tangosol.io.pof.ConfigurablePofContext;

import com.tangosol.net.AsyncNamedCache;
import com.tangosol.net.BackingMapManager;
import com.tangosol.net.BackingMapManagerContext;
import com.tangosol.net.CacheService;
import com.tangosol.net.Cluster;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.NamedCache;

import com.tangosol.net.cache.CacheEvent;
import com.tangosol.net.cache.WrapperNamedCache;

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

import io.grpc.Status;

import io.helidon.config.Config;
import io.helidon.config.ConfigSources;

import java.io.Serializable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Properties;

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
        s_binaryKey1 = BinaryHelper.toBinaryKey(s_binary1);
        s_bytesKey1  = BinaryHelper.toByteString(s_binaryKey1);
        s_bytes1     = BinaryHelper.toByteString(ONE, SERIALIZER);
        s_binary2    = ExternalizableHelper.toBinary(TWO, SERIALIZER);
        s_bytes2     = BinaryHelper.toByteString(TWO, SERIALIZER);
        s_binary3    = ExternalizableHelper.toBinary(THREE, SERIALIZER);
        s_bytes3     = BinaryHelper.toByteString(THREE, SERIALIZER);

        s_serializerProducer = mock(SerializerProducer.class);
        when(s_serializerProducer.getNamedSerializer(eq(JAVA_FORMAT), any(ClassLoader.class))).thenReturn(SERIALIZER);
        when(s_serializerProducer.getNamedSerializer(eq(POF_FORMAT), any(ClassLoader.class))).thenReturn(POF_SERIALIZER);

        s_filter              = new EqualsFilter<>("foo", "bar");
        s_filterBytes         = BinaryHelper.toByteString(s_filter, SERIALIZER);
        s_inKeySetFilter      = new InKeySetFilter<>(s_filter, Collections.singleton(s_bytes1));
        s_inKeySetFilterBytes = BinaryHelper.toByteString(s_inKeySetFilter, SERIALIZER);
        }

    @BeforeEach
    void setupEach()
        {
        m_testCluster   = mock(Cluster.class);
        m_testCCF       = mock(ConfigurableCacheFactory.class);
        CacheStub cache = createCache(TEST_CACHE_NAME);
        s_namedCache    = cache.getMockCache();

        when(m_testCCF.ensureCache(eq(TEST_CACHE_NAME), any(ClassLoader.class))).thenReturn(s_namedCache);
        when(m_testCCF.getScopeName()).thenReturn(Scope.DEFAULT);

        m_ccfSupplier = new NamedCacheService.FixedCacheFactorySupplier(m_testCCF);
        }

    // ----- test methods ---------------------------------------------------

    @Test
    public void shouldNotAddMapListenerIfRequestHasNoCacheName()
        {
        NamedCacheService                       service = new NamedCacheService(m_testCluster, 
                                                                                m_ccfSupplier,
                                                                                s_serializerProducer, 
                                                                                defaultConfig());
        TestStreamObserver<MapListenerResponse> observer = new TestStreamObserver<>();
        MapListenerProxy                        proxy    = new MapListenerProxy(service, observer);
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
        NamedCacheService                       service = new NamedCacheService(m_testCluster, m_ccfSupplier,
                                                                                s_serializerProducer, defaultConfig());
        TestStreamObserver<MapListenerResponse> observer = new TestStreamObserver<>();
        MapListenerProxy                        proxy    = new MapListenerProxy(service, observer);
        MapListenerRequest                      request  = Requests.addKeyMapListener(Scope.DEFAULT,
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
        NamedCacheService                       service  = new NamedCacheService(m_testCluster, m_ccfSupplier,
                                                                                 s_serializerProducer, defaultConfig());
        TestStreamObserver<MapListenerResponse> observer = new TestStreamObserver<>();
        MapListenerProxy                        proxy    = new MapListenerProxy(service, observer);
        proxy.onNext(Requests.addKeyMapListener(Scope.DEFAULT,
                                                TEST_CACHE_NAME, JAVA_FORMAT, s_bytes1,
                                                false, false, ByteString.EMPTY));

        observer.awaitCount(1)
                .assertValueCount(1)
                .assertNoErrors()
                .assertNotComplete();

        MapListenerRequest request = Requests.addKeyMapListener(Scope.DEFAULT,
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
        NamedCacheService                       service  = new NamedCacheService(m_testCluster, m_ccfSupplier,
                                                                                s_serializerProducer, defaultConfig());
        TestStreamObserver<MapListenerResponse> observer = new TestStreamObserver<>();
        MapListenerProxy                        proxy    = new MapListenerProxy(service, observer);
        MapListenerRequest                      request  = Requests.addKeyMapListener(Scope.DEFAULT,
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
        NamedCacheService                       service = new NamedCacheService(m_testCluster, m_ccfSupplier,
                                                                                s_serializerProducer, defaultConfig());
        TestStreamObserver<MapListenerResponse> observer = new TestStreamObserver<>();
        MapListenerProxy                        proxy = new MapListenerProxy(service, observer);
        proxy.onNext(Requests.addKeyMapListener(Scope.DEFAULT,
                                                TEST_CACHE_NAME, JAVA_FORMAT, s_bytes1,
                                                true, false, ByteString.EMPTY));

        observer.awaitCount(1)
                .assertValueCount(1)
                .assertNoErrors()
                .assertNotComplete();

        MapListenerRequest request = Requests.removeKeyMapListener(Scope.DEFAULT,
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
        NamedCacheService                       service  = new NamedCacheService(m_testCluster, m_ccfSupplier,
                                                                                 s_serializerProducer, defaultConfig());
        TestStreamObserver<MapListenerResponse> observer = new TestStreamObserver<>();
        MapListenerProxy                        proxy    = new MapListenerProxy(service, observer);
        MapListenerRequest                      request  = Requests.addKeyMapListener(Scope.DEFAULT,
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
        NamedCacheService                       service = new NamedCacheService(m_testCluster, m_ccfSupplier,
                                                                                s_serializerProducer, defaultConfig());
        TestStreamObserver<MapListenerResponse> observer = new TestStreamObserver<>();
        MapListenerProxy                        proxy    = new MapListenerProxy(service, observer);
        MapListenerRequest                      request1 = Requests.addKeyMapListener(Scope.DEFAULT,
                                                                                      TEST_CACHE_NAME, JAVA_FORMAT,
                                                                                      s_bytes1, true, true,
                                                                                      ByteString.EMPTY);
        MapListenerRequest                      request2 = Requests.addKeyMapListener(Scope.DEFAULT,
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
        NamedCacheService                       service = new NamedCacheService(m_testCluster, m_ccfSupplier,
                                                                                s_serializerProducer, defaultConfig());
        TestStreamObserver<MapListenerResponse> observer = new TestStreamObserver<>();
        MapListenerProxy                        proxy    = new MapListenerProxy(service, observer);

        proxy.onNext(Requests.addKeyMapListener(Scope.DEFAULT,
                                                TEST_CACHE_NAME, JAVA_FORMAT, s_bytes1,
                                                false, true, ByteString.EMPTY));

        observer.awaitCount(1)
                .assertValueCount(1)
                .assertNoErrors()
                .assertNotComplete();

        MapListenerRequest request = Requests.removeKeyMapListener(Scope.DEFAULT,
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
        NamedCacheService                       service      = new NamedCacheService(m_testCluster, m_ccfSupplier,
                                                                                     s_serializerProducer,
                                                                                     defaultConfig());
        MapTrigger                              trigger      = new MapTriggerStub();
        ByteString                              triggerBytes = BinaryHelper.toByteString(trigger, SERIALIZER);
        TestStreamObserver<MapListenerResponse> observer     = new TestStreamObserver<>();
        MapListenerProxy                        proxy        = new MapListenerProxy(service, observer);
        MapListenerRequest                      request      = Requests.addKeyMapListener(Scope.DEFAULT,
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
        NamedCacheService                       service      = new NamedCacheService(m_testCluster, m_ccfSupplier,
                                                                                     s_serializerProducer,
                                                                                     defaultConfig());
        MapTrigger                              trigger      = new MapTriggerStub();
        ByteString                              triggerBytes = BinaryHelper.toByteString(trigger, SERIALIZER);
        TestStreamObserver<MapListenerResponse> observer     = new TestStreamObserver<>();
        MapListenerProxy                        proxy        = new MapListenerProxy(service, observer);

        proxy.onNext(Requests.addKeyMapListener(Scope.DEFAULT,
                                                TEST_CACHE_NAME, JAVA_FORMAT, s_bytes1,
                                                false, false, triggerBytes));

        observer.awaitCount(1)
                .assertValueCount(1)
                .assertNoErrors()
                .assertNotComplete();

        MapListenerRequest request = Requests.removeKeyMapListener(Scope.DEFAULT,
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
        NamedCacheService                       service  = new NamedCacheService(m_testCluster, m_ccfSupplier,
                                                                                 s_serializerProducer, defaultConfig());
        TestStreamObserver<MapListenerResponse> observer = new TestStreamObserver<>();
        MapListenerProxy                        proxy    = new MapListenerProxy(service, observer);
        MapListenerRequest                      request  = Requests.addFilterMapListener(Scope.DEFAULT,
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
        NamedCacheService                       service  = new NamedCacheService(m_testCluster, m_ccfSupplier,
                                                                                 s_serializerProducer, defaultConfig());
        TestStreamObserver<MapListenerResponse> observer = new TestStreamObserver<>();
        MapListenerProxy                        proxy    = new MapListenerProxy(service, observer);
        MapListenerRequest                      request  = Requests.addFilterMapListener(Scope.DEFAULT,
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
        NamedCacheService                       service  = new NamedCacheService(m_testCluster, m_ccfSupplier,
                                                                                 s_serializerProducer, defaultConfig());
        TestStreamObserver<MapListenerResponse> observer = new TestStreamObserver<>();
        MapListenerProxy                        proxy    = new MapListenerProxy(service, observer);

        proxy.onNext(Requests.addFilterMapListener(Scope.DEFAULT,
                                                   TEST_CACHE_NAME, JAVA_FORMAT, s_filterBytes,
                                                   1L, false, false, ByteString.EMPTY));

        observer.awaitCount(1)
                .assertValueCount(1)
                .assertNoErrors()
                .assertNotComplete();

        MapListenerRequest request = Requests.removeFilterMapListener(Scope.DEFAULT,
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
        NamedCacheService                       service  = new NamedCacheService(m_testCluster, m_ccfSupplier,
                                                                                 s_serializerProducer, defaultConfig());
        TestStreamObserver<MapListenerResponse> observer = new TestStreamObserver<>();
        MapListenerProxy                        proxy    = new MapListenerProxy(service, observer);
        MapListenerRequest                      request  = Requests.addFilterMapListener(Scope.DEFAULT,
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
        NamedCacheService                       service  = new NamedCacheService(m_testCluster, m_ccfSupplier,
                                                                                 s_serializerProducer,
                                                                                 defaultConfig());
        TestStreamObserver<MapListenerResponse> observer = new TestStreamObserver<>();
        MapListenerProxy                        proxy    = new MapListenerProxy(service, observer);
        MapListenerRequest                      request  = Requests.addFilterMapListener(Scope.DEFAULT,
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
        NamedCacheService                       service  = new NamedCacheService(m_testCluster, m_ccfSupplier,
                                                                                 s_serializerProducer, defaultConfig());
        TestStreamObserver<MapListenerResponse> observer = new TestStreamObserver<>();
        MapListenerProxy                        proxy = new MapListenerProxy(service, observer);

        proxy.onNext(Requests.addFilterMapListener(Scope.DEFAULT,
                                                   TEST_CACHE_NAME, JAVA_FORMAT, s_inKeySetFilterBytes,
                                                   1L, false, true, ByteString.EMPTY));

        observer.awaitCount(1)
                .assertValueCount(1)
                .assertNoErrors()
                .assertNotComplete();

        MapListenerRequest request = Requests.removeFilterMapListener(Scope.DEFAULT,
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
        NamedCacheService                       service      = new NamedCacheService(m_testCluster, m_ccfSupplier,
                                                                                     s_serializerProducer,
                                                                                     defaultConfig());
        MapTrigger                              trigger      = new MapTriggerStub();
        ByteString                              triggerBytes = BinaryHelper.toByteString(trigger, SERIALIZER);
        TestStreamObserver<MapListenerResponse> observer     = new TestStreamObserver<>();
        MapListenerProxy                        proxy        = new MapListenerProxy(service, observer);

        MapListenerRequest request = Requests.addFilterMapListener(Scope.DEFAULT,
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
        NamedCacheService                       service      = new NamedCacheService(m_testCluster, m_ccfSupplier,
                                                                                     s_serializerProducer,
                                                                                     defaultConfig());
        MapTrigger                              trigger      = new MapTriggerStub();
        ByteString                              triggerBytes = BinaryHelper.toByteString(trigger, SERIALIZER);
        TestStreamObserver<MapListenerResponse> observer     = new TestStreamObserver<>();
        MapListenerProxy                        proxy        = new MapListenerProxy(service, observer);

        proxy.onNext(Requests.addFilterMapListener(Scope.DEFAULT,
                                                   TEST_CACHE_NAME, JAVA_FORMAT, s_filterBytes,
                                                   1L, false, false, triggerBytes));

        observer.awaitCount(1)
                .assertValueCount(1)
                .assertNoErrors()
                .assertNotComplete();

        MapListenerRequest request = Requests.removeFilterMapListener(Scope.DEFAULT,
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
        NamedCacheService                       service              = new NamedCacheService(m_testCluster, m_ccfSupplier,
                                                                                             s_serializerProducer,
                                                                                             defaultConfig());
        TestStreamObserver<MapListenerResponse> observer             = new TestStreamObserver<>();
        MapListenerProxy                        proxy                = new MapListenerProxy(service, observer);
        MapListener<Object, Object>             deactivationListener = proxy.getDeactivationListener();
        proxy.onNext(Requests.initListenerChannel(Scope.DEFAULT,TEST_CACHE_NAME, JAVA_FORMAT));

        observer.awaitCount(1)
                .assertValueCount(1)
                .assertNoErrors();

        verify(s_namedCache).addMapListener(same(deactivationListener));
        }

    @Test
    public void shouldRemoveDeactivationListenerOnCompleted()
        {
        NamedCacheService                       service              = new NamedCacheService(m_testCluster, m_ccfSupplier,
                                                                                             s_serializerProducer,
                                                                                             defaultConfig());
        TestStreamObserver<MapListenerResponse> observer             = new TestStreamObserver<>();
        MapListenerProxy                        proxy                = new MapListenerProxy(service, observer);
        MapListener<Object, Object>             deactivationListener = proxy.getDeactivationListener();
        proxy.onNext(Requests.initListenerChannel(Scope.DEFAULT,TEST_CACHE_NAME, JAVA_FORMAT));

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
        NamedCacheService                       service  = new NamedCacheService(m_testCluster, m_ccfSupplier,
                                                                                 s_serializerProducer, defaultConfig());
        TestStreamObserver<MapListenerResponse> observer = new TestStreamObserver<>();
        MapListenerProxy                        proxy    = new MapListenerProxy(service, observer);

        proxy.onNext(Requests.addKeyMapListener(Scope.DEFAULT,
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
        NamedCacheService                       service  = new NamedCacheService(m_testCluster, m_ccfSupplier,
                                                                                 s_serializerProducer, defaultConfig());
        TestStreamObserver<MapListenerResponse> observer = new TestStreamObserver<>();
        MapListenerProxy                        proxy    = new MapListenerProxy(service, observer);

        proxy.onNext(Requests.addFilterMapListener(Scope.DEFAULT,
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
        NamedCacheService                       service  = new NamedCacheService(m_testCluster, m_ccfSupplier,
                                                                                 s_serializerProducer, defaultConfig());
        TestStreamObserver<MapListenerResponse> observer = new TestStreamObserver<>();
        MapListenerProxy                        proxy    = new MapListenerProxy(service, observer);

        proxy.onNext(Requests.addKeyMapListener(Scope.DEFAULT,
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
        NamedCacheService                       service  = new NamedCacheService(m_testCluster, m_ccfSupplier,
                                                                                 s_serializerProducer, defaultConfig());
        TestStreamObserver<MapListenerResponse> observer = new TestStreamObserver<>();
        MapListenerProxy                        proxy    = new MapListenerProxy(service, observer);

        proxy.onNext(Requests.addFilterMapListener(Scope.DEFAULT, TEST_CACHE_NAME, JAVA_FORMAT, s_filterBytes,
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
        NamedCacheService                       service  = new NamedCacheService(m_testCluster, m_ccfSupplier,
                                                                                 s_serializerProducer, defaultConfig());
        TestStreamObserver<MapListenerResponse> observer = new TestStreamObserver<>();
        MapListenerProxy                        proxy    = new MapListenerProxy(service, observer);

        proxy.onNext(Requests.addKeyMapListener(Scope.DEFAULT, TEST_CACHE_NAME, JAVA_FORMAT, s_bytes1,
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
        NamedCacheService                       service  = new NamedCacheService(m_testCluster, m_ccfSupplier,
                                                                                 s_serializerProducer, defaultConfig());
        TestStreamObserver<MapListenerResponse> observer = new TestStreamObserver<>();
        MapListenerProxy                        proxy    = new MapListenerProxy(service, observer);

        proxy.onNext(Requests.addFilterMapListener(Scope.DEFAULT,
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
        NamedCacheService                       service  = new NamedCacheService(m_testCluster, m_ccfSupplier,
                                                                                 s_serializerProducer, defaultConfig());
        TestStreamObserver<MapListenerResponse> observer = new TestStreamObserver<>();
        MapListenerProxy                        proxy    = new MapListenerProxy(service, observer);

        proxy.onNext(Requests.addKeyMapListener(Scope.DEFAULT,
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
        NamedCacheService                       service  = new NamedCacheService(m_testCluster, m_ccfSupplier,
                                                                                 s_serializerProducer, defaultConfig());
        TestStreamObserver<MapListenerResponse> observer = new TestStreamObserver<>();
        MapListenerProxy                        proxy    = new MapListenerProxy(service, observer);

        proxy.onNext(Requests.addKeyMapListener(Scope.DEFAULT,
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
        NamedCacheService                       service  = new NamedCacheService(m_testCluster, m_ccfSupplier,
                                                                                 s_serializerProducer, defaultConfig());
        TestStreamObserver<MapListenerResponse> observer = new TestStreamObserver<>();
        MapListenerProxy                        proxy    = new MapListenerProxy(service, observer);

        proxy.onNext(Requests.addKeyMapListener(Scope.DEFAULT,
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
        NamedCacheService                       service  = new NamedCacheService(m_testCluster, m_ccfSupplier,
                                                                                 s_serializerProducer, defaultConfig());
        TestStreamObserver<MapListenerResponse> observer = new TestStreamObserver<>();
        MapListenerProxy                        proxy    = new MapListenerProxy(service, observer);

        proxy.onNext(Requests.addKeyMapListener(Scope.DEFAULT,
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

    protected <K, V> CacheStub<K, V> createCache(String sName)
        {
        return new CacheStub<>(sName);
        }

    protected Config defaultConfig()
        {
        Properties props = new Properties();
        props.setProperty(NamedCacheService.CONFIG_PREFIX + "." + NamedCacheService.CONFIG_USE_DAEMON_POOL, "false");

        return Config.create(() -> ConfigSources.create(props).build());
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

    protected static SerializerProducer s_serializerProducer;
    
    protected static NamedCacheService.FixedCacheFactorySupplier m_ccfSupplier;

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

    protected ConfigurableCacheFactory m_testCCF;

    protected Cluster m_testCluster;
    }
