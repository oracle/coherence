/*
 * Copyright (c) 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.client;

import com.tangosol.net.Coherence;

import com.tangosol.net.events.partition.cache.CacheLifecycleEvent;

import io.grpc.Channel;
import io.grpc.ManagedChannelBuilder;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Unit test for {@link GrpcCacheLifecycleEventDispatcher}.
 *
 * @since 23.03
 */
@SuppressWarnings("deprecation")
public class GrpcCacheLifecycleEventDispatcherTest
    {
    @SuppressWarnings({"OptionalGetWithoutIsPresent", "unchecked"})
    @Test
    public void shouldGetSessionNameWhenNoServiceAvailable()
        {
        Channel                  channel       = ManagedChannelBuilder.forAddress("localhost", 1408).build();
        GrpcSessionConfiguration configuration = GrpcSessionConfiguration.builder(channel).named("foo").build();
        GrpcSessions             factory       = new GrpcSessions();
        GrpcRemoteSession        session       = factory.createSession(configuration, Coherence.Mode.Client)
                .map(GrpcRemoteSession.class::cast).get();


        AsyncNamedCacheClient<String, String> async = mock(AsyncNamedCacheClient.class);

        GrpcCacheLifecycleEventDispatcher dispatcher =
                new GrpcCacheLifecycleEventDispatcher("test", session);
        GrpcCacheLifecycleEventDispatcher.GrpcCacheLifecycleEvent event =
                new GrpcCacheLifecycleEventDispatcher.GrpcCacheLifecycleEvent(
                        dispatcher, CacheLifecycleEvent.Type.CREATED, async.getNamedCache());

        // this call should fail if the issue persists
        assertThat(event.getSessionName(), is("foo"));
        }
    }
