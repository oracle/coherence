/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.client;

import com.oracle.coherence.grpc.Requests;

import com.tangosol.io.pof.ConfigurablePofContext;

import com.tangosol.net.Coherence;
import com.tangosol.net.Session;

import com.tangosol.net.SessionConfiguration;
import com.tangosol.net.grpc.GrpcDependencies;
import com.tangosol.net.options.WithConfiguration;
import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

/**
 * An integration test for {@link GrpcSessions}.
 *
 * @author Jonathan Knight  2020.09.25
 */
@SuppressWarnings("OptionalGetWithoutIsPresent")
public class GrpcSessionsTest
    {
    @Test
    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public void shouldGetSessionFromCoherenceSessionsFactory()
        {
        ManagedChannel           channel       = mock(ManagedChannel.class);
        GrpcSessionConfiguration configuration = GrpcSessionConfiguration.builder(channel).named("foo").build();
        Optional<Session>        optional      = Session.create(configuration);

        assertThat(optional, is(notNullValue()));
        assertThat(optional.isPresent(), is(notNullValue()));

        Session session = optional.get();
        assertThat(session, is(notNullValue()));
        assertThat(session, is(instanceOf(GrpcRemoteSession.class)));
        assertThat(((GrpcRemoteSession) session).getChannel(), is(sameInstance(channel)));
        assertThat(session.getScopeName(), is(GrpcDependencies.DEFAULT_SCOPE));
        }

    @Test
    public void shouldNotGetSessionForNonGrpcSessionConfiguration()
        {
        GrpcSessions      factory  = new GrpcSessions();
        Optional<Session> optional = factory.createSession(SessionConfiguration.builder().named("foo").build(), Coherence.Mode.Client);
        assertThat(optional, is(notNullValue()));
        assertThat(optional.isPresent(), is(false));
        }

    @Test
    public void shouldGetSameSessionForSameChannel()
        {
        Channel                  channel       = ManagedChannelBuilder.forAddress("localhost", 1408).build();
        GrpcSessionConfiguration configuration = GrpcSessionConfiguration.builder(channel).named("foo").build();
        GrpcSessions             factory       = new GrpcSessions();
        GrpcRemoteSession session1   = factory.createSession(configuration, Coherence.Mode.Client)
                                              .map(GrpcRemoteSession.class::cast).get();
        GrpcRemoteSession session2   = factory.createSession(configuration, Coherence.Mode.Client)
                                              .map(GrpcRemoteSession.class::cast).get();
        assertThat(session1, is(sameInstance(session2)));
        assertThat(session1.getChannel(), is(sameInstance(session2.getChannel())));
        }

    @Test
    public void shouldGetNewSessionIfOriginalSessionClosed()
        {
        Channel                  channel       = ManagedChannelBuilder.forAddress("localhost", 1408).build();
        GrpcSessionConfiguration configuration = GrpcSessionConfiguration.builder(channel).named("foo").build();
        GrpcSessions             factory       = new GrpcSessions();
        GrpcRemoteSession        session1      = factory.createSession(configuration, Coherence.Mode.Client)
                                                        .map(GrpcRemoteSession.class::cast).get();

        session1.close();

        GrpcRemoteSession session2 = factory.createSession(configuration, Coherence.Mode.Client)
                .map(GrpcRemoteSession.class::cast).get();

        assertThat(session1, is(not(sameInstance(session2))));
        assertThat(session1.getChannel(), is(sameInstance(session2.getChannel())));
        }

    @Test
    public void shouldGetSameSessionForSameScope()
        {
        Channel                  channel       = ManagedChannelBuilder.forAddress("localhost", 1408).build();
        GrpcSessionConfiguration configuration = GrpcSessionConfiguration.builder(channel)
                .named("foo")
                .withScopeName("foo")
                .build();

        GrpcSessions      factory    = new GrpcSessions();
        GrpcRemoteSession session1   = factory.createSession(configuration, Coherence.Mode.Client)
                                              .map(GrpcRemoteSession.class::cast).get();
        GrpcRemoteSession session2   = factory.createSession(configuration, Coherence.Mode.Client)
                                              .map(GrpcRemoteSession.class::cast).get();
        assertThat(session1, is(sameInstance(session2)));
        assertThat(session1.getChannel(), is(sameInstance(session2.getChannel())));
        assertThat(session1.getScopeName(), is(session2.getScopeName()));
        }

    @Test
    public void shouldGetDifferentSessionForDifferentScope()
        {
        Channel                  channel        = ManagedChannelBuilder.forAddress("localhost", 1408).build();
        GrpcSessionConfiguration configuration1 = GrpcSessionConfiguration.builder(channel)
                .named("foo")
                .withScopeName("foo")
                .build();
        GrpcSessionConfiguration configuration2 = GrpcSessionConfiguration.builder(channel)
                .named("foo")
                .withScopeName("bar")
                .build();

        GrpcSessions      factory    = new GrpcSessions();
        GrpcRemoteSession session1   = factory.createSession(configuration1, Coherence.Mode.Client)
                                              .map(GrpcRemoteSession.class::cast).get();
        GrpcRemoteSession session2   = factory.createSession(configuration2, Coherence.Mode.Client)
                                              .map(GrpcRemoteSession.class::cast).get();
        assertThat(session1, is(not(sameInstance(session2))));
        }

    @Test
    public void shouldGetSameSessionForSameSerializerFormat()
        {
        Channel                  channel       = ManagedChannelBuilder.forAddress("localhost", 1408).build();
        GrpcSessionConfiguration configuration = GrpcSessionConfiguration.builder(channel)
                .named("foo")
                .withSerializerFormat("pof")
                .build();

        GrpcSessions      factory    = new GrpcSessions();
        GrpcRemoteSession session1   = factory.createSession(configuration, Coherence.Mode.Client)
                                              .map(GrpcRemoteSession.class::cast).get();
        GrpcRemoteSession session2   = factory.createSession(configuration, Coherence.Mode.Client)
                                              .map(GrpcRemoteSession.class::cast).get();
        assertThat(session1, is(sameInstance(session2)));
        assertThat(session1.getChannel(), is(sameInstance(session2.getChannel())));
        assertThat(session1.getSerializerFormat(), is(session2.getSerializerFormat()));
        }

    @Test
    public void shouldGetDifferentSessionForSerializerFormat()
        {
        Channel                  channel        = ManagedChannelBuilder.forAddress("localhost", 1408).build();
        GrpcSessionConfiguration configuration1 = GrpcSessionConfiguration.builder(channel)
                .named("foo")
                .withSerializerFormat("pof")
                .build();
        GrpcSessionConfiguration configuration2 = GrpcSessionConfiguration.builder(channel)
                .named("foo")
                .withSerializerFormat("java")
                .build();

        GrpcSessions      factory    = new GrpcSessions();
        GrpcRemoteSession session1   = factory.createSession(configuration1, Coherence.Mode.Client)
                                              .map(GrpcRemoteSession.class::cast).get();
        GrpcRemoteSession session2   = factory.createSession(configuration2, Coherence.Mode.Client)
                                              .map(GrpcRemoteSession.class::cast).get();
        assertThat(session1, is(not(sameInstance(session2))));
        }

    @Test
    public void shouldGetSameSessionForSameSerializer()
        {
        Channel                  channel       = ManagedChannelBuilder.forAddress("localhost", 1408).build();
        GrpcSessionConfiguration configuration = GrpcSessionConfiguration.builder(channel)
                .named("foo")
                .withSerializer(new ConfigurablePofContext())
                .build();

        GrpcSessions      factory       = new GrpcSessions();
        GrpcRemoteSession session1      = factory.createSession(configuration, Coherence.Mode.Client)
                                                 .map(GrpcRemoteSession.class::cast).get();
        GrpcRemoteSession session2      = factory.createSession(configuration, Coherence.Mode.Client)
                                                 .map(GrpcRemoteSession.class::cast).get();
        assertThat(session1, is(sameInstance(session2)));
        assertThat(session1.getChannel(), is(sameInstance(session2.getChannel())));
        assertThat(session1.getSerializerFormat(), is(session2.getSerializerFormat()));
        }

    @Test
    public void shouldGetDifferentSessionForSerializer()
        {
        Channel                  channel        = ManagedChannelBuilder.forAddress("localhost", 1408).build();
        GrpcSessionConfiguration configuration1 = GrpcSessionConfiguration.builder(channel)
                .named("foo")
                .withSerializerFormat("pof")
                .withSerializer(new ConfigurablePofContext())
                .build();
        GrpcSessionConfiguration configuration2 = GrpcSessionConfiguration.builder(channel)
                .named("foo")
                .withSerializerFormat("pof")
                .withSerializer(new ConfigurablePofContext())
                .build();

        GrpcSessions      factory        = new GrpcSessions();
        GrpcRemoteSession session1       = factory.createSession(configuration1, Coherence.Mode.Client)
                                                  .map(GrpcRemoteSession.class::cast).get();
        GrpcRemoteSession session2       = factory.createSession(configuration2, Coherence.Mode.Client)
                                                  .map(GrpcRemoteSession.class::cast).get();
        assertThat(session1, is(not(sameInstance(session2))));
        }
    }
