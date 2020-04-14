/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.cdi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Member;
import java.util.Collections;
import java.util.Set;

import javax.enterprise.inject.spi.InjectionPoint;

import com.oracle.coherence.common.util.Options;

import com.tangosol.net.CacheFactoryBuilder;
import com.tangosol.net.Session;
import com.tangosol.net.options.WithClassLoader;
import com.tangosol.net.options.WithConfiguration;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Jonathan Knight  2019.11.06
 */
@SuppressWarnings("unchecked")
class SessionProducerTest
    {

    @Test
    void shouldProduceDefaultSession()
        {
        CacheFactoryBuilder builder = mock(CacheFactoryBuilder.class);
        CacheFactoryUriResolver resolver = mock(CacheFactoryUriResolver.class);
        Session session = mock(Session.class);
        InjectionPoint injectionPoint = mock(InjectionPoint.class);
        Member member = mock(Member.class);
        Class cls = getClass();

        when(injectionPoint.getMember()).thenReturn(member);
        when(member.getDeclaringClass()).thenReturn(cls);
        when(resolver.resolve(anyString())).thenReturn("foo");
        when(builder.createSession(any(Session.Option.class))).thenReturn(session);

        SessionProducer producer = new SessionProducer(resolver, builder);
        Session result = producer.getDefaultSession(injectionPoint);
        assertThat(result, is(sameInstance(session)));

        ArgumentCaptor<Session.Option> args = ArgumentCaptor.forClass(Session.Option.class);
        verify(builder).createSession(args.capture());

        Options<Session.Option> options = Options.from(Session.Option.class, args.getAllValues().toArray(new Session.Option[0]));
        WithConfiguration withConfiguration = options.get(WithConfiguration.class);
        WithClassLoader withClassLoader = options.get(WithClassLoader.class);

        assertThat(withConfiguration, is(notNullValue()));
        assertThat(withConfiguration.getLocation(), is(WithConfiguration.autoDetect().getLocation()));
        assertThat(withClassLoader, is(notNullValue()));
        assertThat(withClassLoader.getClassLoader(), is(sameInstance(cls.getClassLoader())));
        }

    @Test
    void shouldProduceNamedSession()
        {
        CacheFactoryBuilder builder = mock(CacheFactoryBuilder.class);
        CacheFactoryUriResolver resolver = mock(CacheFactoryUriResolver.class);
        Session session = mock(Session.class);
        InjectionPoint injectionPoint = mock(InjectionPoint.class);
        Member member = mock(Member.class);
        Class cls = getClass();
        Set<Annotation> qualifiers = Collections.singleton(CacheFactory.Literal.of("foo"));

        when(injectionPoint.getMember()).thenReturn(member);
        when(injectionPoint.getQualifiers()).thenReturn(qualifiers);
        when(member.getDeclaringClass()).thenReturn(cls);
        when(resolver.resolve(anyString())).thenReturn("bar");
        when(builder.createSession(any(Session.Option.class))).thenReturn(session);

        SessionProducer producer = new SessionProducer(resolver, builder);
        Session result = producer.getDefaultSession(injectionPoint);
        assertThat(result, is(sameInstance(session)));

        ArgumentCaptor<Session.Option> args = ArgumentCaptor.forClass(Session.Option.class);
        verify(builder).createSession(args.capture());
        verify(resolver).resolve("foo");

        Options<Session.Option> options = Options.from(Session.Option.class, args.getAllValues().toArray(new Session.Option[0]));
        WithConfiguration withConfiguration = options.get(WithConfiguration.class);
        WithClassLoader withClassLoader = options.get(WithClassLoader.class);

        assertThat(withConfiguration, is(notNullValue()));
        assertThat(withConfiguration.getLocation(), is("bar"));
        assertThat(withClassLoader, is(notNullValue()));
        assertThat(withClassLoader.getClassLoader(), is(sameInstance(cls.getClassLoader())));
        }

    @Test
    void shouldProduceSameSessionWithSameQualifiers()
        {
        CacheFactoryBuilder builder = mock(CacheFactoryBuilder.class);
        CacheFactoryUriResolver resolver = mock(CacheFactoryUriResolver.class);
        Session session = mock(Session.class);
        Class cls = getClass();
        InjectionPoint injectionPointOne = mock(InjectionPoint.class);
        Member memberOne = mock(Member.class);
        Set<Annotation> qualifiersOne = Collections.singleton(CacheFactory.Literal.of("foo"));
        InjectionPoint injectionPointTwo = mock(InjectionPoint.class);
        Member memberTwo = mock(Member.class);
        Set<Annotation> qualifiersTwo = Collections.singleton(CacheFactory.Literal.of("foo"));

        when(injectionPointOne.getMember()).thenReturn(memberOne);
        when(injectionPointOne.getQualifiers()).thenReturn(qualifiersOne);
        when(memberOne.getDeclaringClass()).thenReturn(cls);
        when(injectionPointTwo.getMember()).thenReturn(memberTwo);
        when(injectionPointTwo.getQualifiers()).thenReturn(qualifiersTwo);
        when(memberTwo.getDeclaringClass()).thenReturn(cls);
        when(resolver.resolve(anyString())).thenReturn("bar");
        when(builder.createSession(any(Session.Option.class))).thenReturn(session);

        SessionProducer producer = new SessionProducer(resolver, builder);

        Session resultOne = producer.getDefaultSession(injectionPointOne);
        Session resultTwo = producer.getDefaultSession(injectionPointTwo);
        assertThat(resultOne, is(sameInstance(session)));
        assertThat(resultTwo, is(sameInstance(session)));

        ArgumentCaptor<Session.Option> args = ArgumentCaptor.forClass(Session.Option.class);
        verify(builder, times(1)).createSession(args.capture());
        }

    @Test
    void shouldProduceDifferentSessionWithSameQualifiers()
        {
        CacheFactoryBuilder builder = mock(CacheFactoryBuilder.class);
        CacheFactoryUriResolver resolver = mock(CacheFactoryUriResolver.class);
        Session sessionOne = mock(Session.class);
        Session sessionTwo = mock(Session.class);
        Class cls = getClass();
        InjectionPoint injectionPointOne = mock(InjectionPoint.class);
        Member memberOne = mock(Member.class);
        Set<Annotation> qualifiersOne = Collections.singleton(CacheFactory.Literal.of("foo"));
        InjectionPoint injectionPointTwo = mock(InjectionPoint.class);
        Member memberTwo = mock(Member.class);
        Set<Annotation> qualifiersTwo = Collections.singleton(CacheFactory.Literal.of("bar"));

        when(injectionPointOne.getMember()).thenReturn(memberOne);
        when(injectionPointOne.getQualifiers()).thenReturn(qualifiersOne);
        when(memberOne.getDeclaringClass()).thenReturn(cls);
        when(injectionPointTwo.getMember()).thenReturn(memberTwo);
        when(injectionPointTwo.getQualifiers()).thenReturn(qualifiersTwo);
        when(memberTwo.getDeclaringClass()).thenReturn(cls);
        when(resolver.resolve("foo")).thenReturn("foo-uri");
        when(resolver.resolve("bar")).thenReturn("bar-uri");
        when(builder.createSession(any(Session.Option.class))).thenReturn(sessionOne, sessionTwo);

        SessionProducer producer = new SessionProducer(resolver, builder);

        Session resultOne = producer.getDefaultSession(injectionPointOne);
        Session resultTwo = producer.getDefaultSession(injectionPointTwo);
        assertThat(resultOne, is(sameInstance(sessionOne)));
        assertThat(resultTwo, is(sameInstance(sessionTwo)));

        ArgumentCaptor<Session.Option> args = ArgumentCaptor.forClass(Session.Option.class);
        verify(builder, times(2)).createSession(args.capture());
        }

    }
