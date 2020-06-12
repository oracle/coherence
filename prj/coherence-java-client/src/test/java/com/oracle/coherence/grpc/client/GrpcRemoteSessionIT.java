/*
 * Copyright (c) 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.client;

import com.tangosol.io.DefaultSerializer;
import com.tangosol.io.Serializer;

import com.tangosol.io.pof.ConfigurablePofContext;

import com.tangosol.net.NamedCache;

import io.grpc.ManagedChannel;

import io.helidon.config.Config;

import io.helidon.microprofile.server.Server;

import javax.enterprise.inject.spi.BeanManager;

import javax.enterprise.inject.spi.CDI;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;

import static org.hamcrest.MatcherAssert.assertThat;

import static org.junit.jupiter.api.Assertions.assertThrows;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Jonathan Knight  2019.11.28
 * @since 20.06
 */
class GrpcRemoteSessionIT
    {
    // ----- test lifecycle -------------------------------------------------

    @BeforeAll
    static void setupBaseTest()
        {
        System.setProperty("mp.initializer.allow",  "true");
        System.setProperty("coherence.ttl",         "0");
        System.setProperty("coherence.clustername", "RemoteSessionIT");
        System.setProperty("coherence.pof.config",  "coherence-grpc-proxy-pof-config.xml");
        s_server = Server.create().start();
        }

    @AfterAll
    static void cleanupBaseTest()
        {
        s_server.stop();
        }

    // ----- test methods ---------------------------------------------------

    @Test
    void shouldCreateDefaultSession()
        {
        GrpcRemoteSession session = RemoteSessions.instance().createSession();
        assertThat(session.isClosed(), is(false));
        assertThat(session, is(notNullValue()));
        assertThat(session.getSerializerFormat(), is("java"));
        assertThat(session.getSerializer(), is(instanceOf(DefaultSerializer.class)));
        assertThat(session.getChannel(), is(notNullValue()));
        assertThat(session.getBeanManager(), is(instanceOf(CDI.current().getBeanManager().getClass())));
        }

    @Test
    void shouldCreateNamedCache()
        {
        GrpcRemoteSession          session = RemoteSessions.instance().createSession();
        NamedCache<String, String> cache   = session.getCache("test-cache");
        cache.put("key-1", "value-1");
        assertThat(cache.get("key-1"), is("value-1"));
        }

    @Test
    void shouldClose()
        {
        GrpcRemoteSession session = RemoteSessions.instance().createSession();
        assertThat(session.isClosed(), is(false));
        session.close();
        assertThat(session.isClosed(), is(true));
        }

    @Test
    void shouldReleaseCachesOnClose()
        {
        GrpcRemoteSession          session  = RemoteSessions.instance().createSession();
        NamedCache<String, String> cacheOne = session.getCache("test-cache-one");
        NamedCache<String, String> cacheTwo = session.getCache("test-cache-one");
        session.close();
        assertThat(cacheOne.isReleased(), is(true));
        assertThat(cacheTwo.isReleased(), is(true));
        }

    @Test
    void shouldNotGetCacheAfterClose()
        {
        GrpcRemoteSession session = RemoteSessions.instance().createSession();
        session.close();

        assertThrows(IllegalStateException.class, () -> session.getCache("test-cache-one"));
        }

    @Test
    void shouldGetSameInstanceOfNamedCache()
        {
        GrpcRemoteSession          session = RemoteSessions.instance().createSession();
        NamedCache<String, String> cache1  = session.getCache("test-cache");
        NamedCache<String, String> cache2  = session.getCache("test-cache");
        assertThat(cache1, is(sameInstance(cache2)));
        }

    @Test
    void shouldGetNewInstanceOfReleasedNamedCache()
        {
        GrpcRemoteSession          session = RemoteSessions.instance().createSession();
        NamedCache<String, String> cache1  = session.getCache("test-cache");
        cache1.release();
        NamedCache<String, String> cache2 = session.getCache("test-cache");
        assertThat(cache1, is(not(sameInstance(cache2))));
        }

    @Test
    void shouldGetNewInstanceOfDestroyedNamedCache()
        {
        GrpcRemoteSession          session = RemoteSessions.instance().createSession();
        NamedCache<String, String> cache1  = session.getCache("test-cache");
        cache1.destroy();
        NamedCache<String, String> cache2 = session.getCache("test-cache");
        assertThat(cache1, is(not(sameInstance(cache2))));
        }

    @Test
    void shouldBuildSession()
        {
        Serializer     serializer  = mock(Serializer.class);
        String         format      = "foo";
        ManagedChannel channel     = mock(ManagedChannel.class);
        BeanManager    beanManager = mock(BeanManager.class);

        GrpcRemoteSession session = GrpcRemoteSession.builder()
                .serializer(serializer, format)
                .channel(channel)
                .beanManager(beanManager)
                .build();


        assertThat(session, is(notNullValue()));
        assertThat(session.getSerializerFormat(), is(format));
        assertThat(session.getSerializer(), is(sameInstance(serializer)));
        assertThat(session.getChannel(), is(sameInstance(channel)));
        assertThat(session.getBeanManager(), is(sameInstance(beanManager)));
        }

    @Test
    void shouldBuildSessionWithConfig()
        {
        Serializer     serializer  = mock(Serializer.class);
        String         format      = "foo";
        ManagedChannel channel     = mock(ManagedChannel.class);
        BeanManager    beanManager = mock(BeanManager.class);
        Config         config      = Config.builder().build();

        GrpcRemoteSession session = GrpcRemoteSession.builder(config)
                .serializer(serializer, format)
                .channel(channel)
                .beanManager(beanManager)
                .build();


        assertThat(session, is(notNullValue()));
        assertThat(session.getSerializerFormat(), is(format));
        assertThat(session.getSerializer(), is(sameInstance(serializer)));
        assertThat(session.getChannel(), is(sameInstance(channel)));
        assertThat(session.getBeanManager(), is(sameInstance(beanManager)));
        }

    @Test
    void shouldBuildSessionWithNullConfig()
        {
        Serializer     serializer  = mock(Serializer.class);
        String         format      = "foo";
        ManagedChannel channel     = mock(ManagedChannel.class);
        BeanManager    beanManager = mock(BeanManager.class);

        GrpcRemoteSession session = GrpcRemoteSession.builder(null)
                .serializer(serializer, format)
                .channel(channel)
                .beanManager(beanManager)
                .build();


        assertThat(session, is(notNullValue()));
        assertThat(session.getSerializerFormat(), is(format));
        assertThat(session.getSerializer(), is(sameInstance(serializer)));
        assertThat(session.getChannel(), is(sameInstance(channel)));
        assertThat(session.getBeanManager(), is(sameInstance(beanManager)));
        }

    @Test
    void shouldBuildSessionWithSerializer()
        {
        Serializer serializer = mock(Serializer.class);

        when(serializer.getName()).thenReturn("foo");

        GrpcRemoteSession session = GrpcRemoteSession.builder()
                .serializer(serializer)
                .build();

        assertThat(session, is(notNullValue()));
        assertThat(session.getSerializer(), is(sameInstance(serializer)));
        assertThat(session.getSerializerFormat(), is("foo"));
        }

    @Test
    void shouldBuildSessionWithNamedSerializer()
        {
        GrpcRemoteSession session = GrpcRemoteSession.builder()
                .serializer(null, "pof")
                .build();

        assertThat(session, is(notNullValue()));
        assertThat(session.getSerializer(), is(instanceOf(ConfigurablePofContext.class)));
        }

    @Test
    void shouldBuildSessionWithNamedChannel()
        {
        GrpcRemoteSession session = GrpcRemoteSession.builder()
                .channelName("default")
                .build();

        assertThat(session, is(notNullValue()));
        assertThat(session.getChannel(), is(notNullValue()));
        }

    @Test
    void shouldBuildSessionWithInvalidChannelName()
        {
        GrpcRemoteSession session = GrpcRemoteSession.builder().channelName("foo").build();
        assertThat(session.name(), is(GrpcRemoteSession.DEFAULT_NAME));
        }

    @Test
    void shouldBuildSessionWithSerializerNameAsFormat()
        {
        Serializer     serializer  = mock(Serializer.class);
        String         format      = "foo";
        ManagedChannel channel     = mock(ManagedChannel.class);
        BeanManager    beanManager = mock(BeanManager.class);

        when(serializer.getName()).thenReturn("foo");

        GrpcRemoteSession session = GrpcRemoteSession.builder()
                .serializer(serializer, null)
                .channel(channel)
                .beanManager(beanManager)
                .build();


        assertThat(session, is(notNullValue()));
        assertThat(session.getSerializerFormat(), is(format));
        assertThat(session.getSerializer(), is(sameInstance(serializer)));
        assertThat(session.getChannel(), is(sameInstance(channel)));
        assertThat(session.getBeanManager(), is(sameInstance(beanManager)));
        }

    // ----- data members ---------------------------------------------------

    protected static Server s_server;
    }
