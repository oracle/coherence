/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.helidon.client;

import com.oracle.coherence.cdi.Remote;
import com.oracle.coherence.cdi.Scope;

import com.oracle.coherence.cdi.SerializerProducer;
import com.oracle.coherence.client.GrpcRemoteSession;
import com.oracle.coherence.grpc.Requests;
import com.oracle.coherence.helidon.client.GrpcSessionProducer;
import com.tangosol.io.Serializer;
import com.tangosol.io.pof.ConfigurablePofContext;

import io.grpc.Channel;
import io.helidon.config.Config;

import io.helidon.config.MapConfigSource;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.literal.NamedLiteral;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionPoint;

import java.lang.annotation.Annotation;

import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Jonathan Knight  2020.09.25
 */
@SuppressWarnings("unchecked")
public class GrpcSessionProducerTest
    {
    @Test
    public void shouldGetDefaultSession()
        {
        BeanManager                  beanManager        = mock(BeanManager.class);
        Instance<Object>             instance           = mock(Instance.class);
        Instance<Channel>            instanceChannel    = mock(Instance.class);
        Channel                      channel            = mock(Channel.class);
        Instance<SerializerProducer> instanceSerializer = mock(Instance.class);
        SerializerProducer           serializerProducer = mock(SerializerProducer.class);
        Serializer                   serializer         = mock(Serializer.class);
        Config                       config             = Config.empty();

        when(beanManager.createInstance()).thenReturn(instance);
        when(instance.select(eq(Channel.class), any(Annotation.class))).thenReturn(instanceChannel);
        when(instance.select(eq(SerializerProducer.class), any(Annotation.class))).thenReturn(instanceSerializer);
        when(instanceChannel.isResolvable()).thenReturn(true);
        when(instanceChannel.get()).thenReturn(channel);
        when(instanceSerializer.isResolvable()).thenReturn(true);
        when(instanceSerializer.get()).thenReturn(serializerProducer);
        when(serializerProducer.getNamedSerializer(anyString(), any(ClassLoader.class))).thenReturn(serializer);

        GrpcSessionProducer producer = new GrpcSessionProducer(beanManager, config);

        Set<Annotation> quals  = new HashSet<>();
        Remote          remote = Remote.Literal.of(Remote.DEFAULT_NAME);
        Scope           scope  = Scope.Literal.of(Scope.DEFAULT);
        quals.add(remote);
        quals.add(scope);

        InjectionPoint ip = mock(InjectionPoint.class);
        when(ip.getQualifiers()).thenReturn(quals);

        GrpcRemoteSession session = producer.getSession(ip);
        assertThat(session, is(notNullValue()));
        assertThat(session.getName(), is(Remote.DEFAULT_NAME));
        assertThat(session.getScope(), is(Requests.DEFAULT_SCOPE));

        ArgumentCaptor<Annotation> captor = ArgumentCaptor.forClass(Annotation.class);
        verify(instance).select(eq(Channel.class), captor.capture());
        List<Annotation> values = captor.getAllValues();
        assertThat(values, containsInAnyOrder(NamedLiteral.of(Remote.DEFAULT_NAME)));
        }

    @Test
    public void shouldGetDefaultSessionForScope()
        {
        BeanManager                  beanManager        = mock(BeanManager.class);
        Instance<Object>             instance           = mock(Instance.class);
        Instance<Channel>            instanceChannel    = mock(Instance.class);
        Channel                      channel            = mock(Channel.class);
        Instance<SerializerProducer> instanceSerializer = mock(Instance.class);
        SerializerProducer           serializerProducer = mock(SerializerProducer.class);
        Serializer                   serializer         = mock(Serializer.class);
        Config                       config             = Config.empty();

        when(beanManager.createInstance()).thenReturn(instance);
        when(instance.select(eq(Channel.class), any(Annotation.class))).thenReturn(instanceChannel);
        when(instance.select(eq(SerializerProducer.class), any(Annotation.class))).thenReturn(instanceSerializer);
        when(instanceChannel.isResolvable()).thenReturn(true);
        when(instanceChannel.get()).thenReturn(channel);
        when(instanceSerializer.isResolvable()).thenReturn(true);
        when(instanceSerializer.get()).thenReturn(serializerProducer);
        when(serializerProducer.getNamedSerializer(anyString(), any(ClassLoader.class))).thenReturn(serializer);

        GrpcSessionProducer producer = new GrpcSessionProducer(beanManager, config);

        Set<Annotation> quals  = new HashSet<>();
        Remote          remote = Remote.Literal.of(Remote.DEFAULT_NAME);
        Scope           scope  = Scope.Literal.of("foo");
        quals.add(remote);
        quals.add(scope);

        InjectionPoint ip = mock(InjectionPoint.class);
        when(ip.getQualifiers()).thenReturn(quals);

        GrpcRemoteSession session = producer.getSession(ip);
        assertThat(session, is(notNullValue()));
        assertThat(session.getName(), is(Remote.DEFAULT_NAME));
        assertThat(session.getScope(), is("foo"));

        ArgumentCaptor<Annotation> captor = ArgumentCaptor.forClass(Annotation.class);
        verify(instance).select(eq(Channel.class), captor.capture());
        List<Annotation> values = captor.getAllValues();
        assertThat(values, containsInAnyOrder(NamedLiteral.of(Remote.DEFAULT_NAME)));
        }

    @Test
    public void shouldGetNamedSession()
        {
        BeanManager                  beanManager        = mock(BeanManager.class);
        Instance<Object>             instance           = mock(Instance.class);
        Instance<Channel>            instanceChannel    = mock(Instance.class);
        Channel                      channel            = mock(Channel.class);
        Instance<SerializerProducer> instanceSerializer = mock(Instance.class);
        SerializerProducer           serializerProducer = mock(SerializerProducer.class);
        Serializer                   serializer         = mock(Serializer.class);

        when(beanManager.createInstance()).thenReturn(instance);
        when(instance.select(eq(Channel.class), any(Annotation.class))).thenReturn(instanceChannel);
        when(instance.select(eq(SerializerProducer.class), any(Annotation.class))).thenReturn(instanceSerializer);
        when(instanceChannel.isResolvable()).thenReturn(true);
        when(instanceChannel.get()).thenReturn(channel);
        when(instanceSerializer.isResolvable()).thenReturn(true);
        when(instanceSerializer.get()).thenReturn(serializerProducer);
        when(serializerProducer.getNamedSerializer(anyString(), any(ClassLoader.class))).thenReturn(serializer);

        Properties cfgProperties = new Properties();
        cfgProperties.setProperty("coherence.sessions.foo.channel", "bar");

        Config              config   = Config.create(MapConfigSource.create(cfgProperties));
        GrpcSessionProducer producer = new GrpcSessionProducer(beanManager, config);

        Set<Annotation> quals  = new HashSet<>();
        Remote          remote = Remote.Literal.of("foo");
        Scope           scope  = Scope.Literal.of(Scope.DEFAULT);
        quals.add(remote);
        quals.add(scope);

        InjectionPoint ip = mock(InjectionPoint.class);
        when(ip.getQualifiers()).thenReturn(quals);

        GrpcRemoteSession session = producer.getSession(ip);
        assertThat(session, is(notNullValue()));
        assertThat(session.getName(), is("foo"));
        assertThat(session.getScope(), is(Requests.DEFAULT_SCOPE));

        ArgumentCaptor<Annotation> captor = ArgumentCaptor.forClass(Annotation.class);
        verify(instance).select(eq(Channel.class), captor.capture());
        List<Annotation> values = captor.getAllValues();
        assertThat(values, containsInAnyOrder(NamedLiteral.of("bar")));
        }

    @Test
    public void shouldGetNamedSessionWithSerializer()
        {
        BeanManager                  beanManager        = mock(BeanManager.class);
        Instance<Object>             instance           = mock(Instance.class);
        Instance<Channel>            instanceChannel    = mock(Instance.class);
        Channel                      channel            = mock(Channel.class);
        Instance<SerializerProducer> instanceSerializer = mock(Instance.class);
        SerializerProducer           serializerProducer = mock(SerializerProducer.class);
        Serializer                   serializer         = mock(Serializer.class);

        when(beanManager.createInstance()).thenReturn(instance);
        when(instance.select(eq(Channel.class), any(Annotation.class))).thenReturn(instanceChannel);
        when(instance.select(eq(SerializerProducer.class), any(Annotation.class))).thenReturn(instanceSerializer);
        when(instanceChannel.isResolvable()).thenReturn(true);
        when(instanceChannel.get()).thenReturn(channel);
        when(instanceSerializer.isResolvable()).thenReturn(true);
        when(instanceSerializer.get()).thenReturn(serializerProducer);
        when(serializerProducer.getNamedSerializer(anyString(), any(ClassLoader.class))).thenReturn(serializer);

        Properties cfgProperties = new Properties();
        cfgProperties.setProperty("coherence.sessions.foo.channel", "bar");
        cfgProperties.setProperty("coherence.sessions.foo.serializer", "pof");

        Config              config   = Config.create(MapConfigSource.create(cfgProperties));
        GrpcSessionProducer producer = new GrpcSessionProducer(beanManager, config);

        Set<Annotation> quals  = new HashSet<>();
        Remote          remote = Remote.Literal.of("foo");
        Scope           scope  = Scope.Literal.of(Scope.DEFAULT);
        quals.add(remote);
        quals.add(scope);

        InjectionPoint ip = mock(InjectionPoint.class);
        when(ip.getQualifiers()).thenReturn(quals);

        GrpcRemoteSession session = producer.getSession(ip);
        assertThat(session, is(notNullValue()));
        assertThat(session.getName(), is("foo"));
        assertThat(session.getScope(), is(Requests.DEFAULT_SCOPE));
        assertThat(session.getSerializerFormat(), is("pof"));
        assertThat(session.getSerializer(), is(sameInstance(serializer)));

        verify(serializerProducer).getNamedSerializer(eq("pof"), any(ClassLoader.class));

        ArgumentCaptor<Annotation> captor = ArgumentCaptor.forClass(Annotation.class);
        verify(instance).select(eq(Channel.class), captor.capture());

        List<Annotation> values = captor.getAllValues();
        assertThat(values, containsInAnyOrder(NamedLiteral.of("bar")));
        }
    }
