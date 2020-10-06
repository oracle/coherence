/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.client;

import com.oracle.coherence.grpc.Requests;

import com.tangosol.io.pof.ConfigurablePofContext;

import com.tangosol.net.Session;

import io.grpc.Channel;
import io.grpc.ManagedChannelBuilder;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

/**
 * An integration test for {@link GrpcSessions}.
 *
 * @author Jonathan Knight  2020.09.25
 */
public class GrpcSessionsTest
    {
    @Test
    public void shouldGetSessionFromCoherenceSessionsFactory()
        {
        Channel channel = mock(Channel.class);
        Session session = Session.create(GrpcSessions.channel(channel));
        assertThat(session, is(notNullValue()));
        assertThat(session, is(instanceOf(GrpcRemoteSession.class)));
        assertThat(((GrpcRemoteSession) session).getChannel(), is(sameInstance(channel)));
        assertThat(((GrpcRemoteSession) session).getScope(), is(Requests.DEFAULT_SCOPE));
        }

    @Test
    public void shouldNotGetSessionWithoutChannel()
        {
        GrpcSessions factory = new GrpcSessions();
        Session      session = factory.createSession();
        assertThat(session, is(nullValue()));
        }

    @Test
    public void shouldGetSessionWithInProcessChannel()
        {
        GrpcSessions      factory = new GrpcSessions();
        GrpcRemoteSession session = factory.createSession(GrpcSessions.inProcessChannel());
        assertThat(session, is(notNullValue()));
        assertThat(session.getChannel(), is(notNullValue()));
        }

    @Test
    public void shouldGetSessionWithInProcessChannelIfSpecifiedChannelIsNull()
        {
        GrpcSessions      factory  = new GrpcSessions();
        GrpcRemoteSession session1 = factory.createSession(GrpcSessions.inProcessChannel());
        GrpcRemoteSession session2 = factory.createSession(GrpcSessions.channel(null));
        assertThat(session1, is(notNullValue()));
        assertThat(session2, is(notNullValue()));
        assertThat(session1.getChannel(), is(sameInstance(session2.getChannel())));
        }

    @Test
    public void shouldGetSameSessionForSameChannel()
        {
        Channel           channel    = ManagedChannelBuilder.forAddress("localhost", 1408).build();
        GrpcSessions      factory    = new GrpcSessions();
        Session.Option    optChannel = GrpcSessions.channel(channel);
        GrpcRemoteSession session1   = factory.createSession(optChannel);
        GrpcRemoteSession session2   = factory.createSession(optChannel);
        assertThat(session1, is(sameInstance(session2)));
        assertThat(session1.getChannel(), is(sameInstance(session2.getChannel())));
        }

    @Test
    public void shouldGetNewSessionIfOriginalSessionClosed()
        {
        Channel           channel    = ManagedChannelBuilder.forAddress("localhost", 1408).build();
        GrpcSessions      factory    = new GrpcSessions();
        Session.Option    optChannel = GrpcSessions.channel(channel);
        GrpcRemoteSession session1   = factory.createSession(optChannel);

        session1.close();

        GrpcRemoteSession session2 = factory.createSession(optChannel);

        assertThat(session1, is(not(sameInstance(session2))));
        assertThat(session1.getChannel(), is(sameInstance(session2.getChannel())));
        }

    @Test
    public void shouldGetSameSessionForSameScope()
        {
        Channel           channel    = ManagedChannelBuilder.forAddress("localhost", 1408).build();
        Session.Option    optScope   = GrpcSessions.scope("foo");
        GrpcSessions      factory    = new GrpcSessions();
        Session.Option    optChannel = GrpcSessions.channel(channel);
        GrpcRemoteSession session1   = factory.createSession(optChannel, optScope);
        GrpcRemoteSession session2   = factory.createSession(optChannel, optScope);
        assertThat(session1, is(sameInstance(session2)));
        assertThat(session1.getChannel(), is(sameInstance(session2.getChannel())));
        assertThat(session1.getScope(), is(session2.getScope()));
        }

    @Test
    public void shouldGetDifferentSessionForDifferentScope()
        {
        Channel           channel    = ManagedChannelBuilder.forAddress("localhost", 1408).build();
        GrpcSessions      factory    = new GrpcSessions();
        Session.Option    optChannel = GrpcSessions.channel(channel);
        GrpcRemoteSession session1   = factory.createSession(optChannel, GrpcSessions.scope("foo"));
        GrpcRemoteSession session2   = factory.createSession(optChannel, GrpcSessions.scope("bar"));
        assertThat(session1, is(not(sameInstance(session2))));
        }

    @Test
    public void shouldGetSameSessionForSameSerializerFormat()
        {
        Channel           channel    = ManagedChannelBuilder.forAddress("localhost", 1408).build();
        Session.Option    optFormat  = GrpcSessions.serializerFormat("pof");
        GrpcSessions      factory    = new GrpcSessions();
        Session.Option    optChannel = GrpcSessions.channel(channel);
        GrpcRemoteSession session1   = factory.createSession(optChannel, optFormat);
        GrpcRemoteSession session2   = factory.createSession(optChannel, optFormat);
        assertThat(session1, is(sameInstance(session2)));
        assertThat(session1.getChannel(), is(sameInstance(session2.getChannel())));
        assertThat(session1.getSerializerFormat(), is(session2.getSerializerFormat()));
        }

    @Test
    public void shouldGetDifferentSessionForSerializerFormat()
        {
        Channel           channel    = ManagedChannelBuilder.forAddress("localhost", 1408).build();
        GrpcSessions      factory    = new GrpcSessions();
        Session.Option    optChannel = GrpcSessions.channel(channel);
        GrpcRemoteSession session1   = factory.createSession(optChannel, GrpcSessions.serializerFormat("pof"));
        GrpcRemoteSession session2   = factory.createSession(optChannel, GrpcSessions.serializerFormat("java"));
        assertThat(session1, is(not(sameInstance(session2))));
        }

    @Test
    public void shouldGetSameSessionForSameSerializer()
        {
        Channel           channel       = ManagedChannelBuilder.forAddress("localhost", 1408).build();
        Session.Option    optFormat     = GrpcSessions.serializerFormat("pof");
        Session.Option    optSerializer = GrpcSessions.serializer(new ConfigurablePofContext());
        GrpcSessions      factory       = new GrpcSessions();
        Session.Option    optChannel    = GrpcSessions.channel(channel);
        GrpcRemoteSession session1      = factory.createSession(optChannel, optFormat, optSerializer);
        GrpcRemoteSession session2      = factory.createSession(optChannel, optFormat, optSerializer);
        assertThat(session1, is(sameInstance(session2)));
        assertThat(session1.getChannel(), is(sameInstance(session2.getChannel())));
        assertThat(session1.getSerializerFormat(), is(session2.getSerializerFormat()));
        }

    @Test
    public void shouldGetDifferentSessionForSerializer()
        {
        Channel           channel        = ManagedChannelBuilder.forAddress("localhost", 1408).build();
        Session.Option    optFormat      = GrpcSessions.serializerFormat("pof");
        Session.Option    optSerializer1 = GrpcSessions.serializer(new ConfigurablePofContext());
        Session.Option    optSerializer2 = GrpcSessions.serializer(new ConfigurablePofContext());
        GrpcSessions      factory        = new GrpcSessions();
        Session.Option    optChannel     = GrpcSessions.channel(channel);
        GrpcRemoteSession session1       = factory.createSession(optChannel, optFormat, optSerializer1);
        GrpcRemoteSession session2       = factory.createSession(optChannel, optFormat, optSerializer2);
        assertThat(session1, is(not(sameInstance(session2))));
        }
    }
