/*
 * Copyright (c) 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.cache.grpc.client;

import com.tangosol.net.AsyncNamedCache;
import com.tangosol.net.NamedCache;

import java.lang.annotation.Annotation;

import java.util.Set;

import javax.enterprise.context.ApplicationScoped;

import javax.enterprise.inject.Produces;

import javax.enterprise.inject.spi.InjectionPoint;

import javax.inject.Inject;

/**
 * A CDI producer for {@link NamedCache} gRPC service clients.
 *
 * @author Jonathan Knight  2019.11.07
 * @since 14.1.2
 */
@ApplicationScoped
public class NamedCacheProducer
    {
    // ----- constructors ---------------------------------------------------

    @Inject
    NamedCacheProducer(RemoteSessions sessionProducer)
        {
        this.f_sessionProducer = sessionProducer;
        }

    // ----- public methods -------------------------------------------------

    /**
     * Produce a {@link NamedCache} gRPC client using the name from the {@link RemoteCache}
     * qualifier as the cache name and the name from the optional {@link RemoteSession}
     * qualifier to identify the gRPC {@link com.tangosol.net.Session} to use to connect to
     * the server.
     * <p>
     * If no {@link RemoteSession} qualifier is present the default gRPC session will be used.
     *
     * @param injectionPoint  the injection point to inject the {@link NamedCache} into
     * @param <K>             the type of the cache keys
     * @param <V>             the type of the cache values
     *
     * @return a {@link NamedCache} using the name from the {@link RemoteCache} qualifier
     *         as the cache name and the name from the optional {@link RemoteSession} qualifier
     */
    @Produces
    @RemoteCache
    @RemoteSession("")
    public <K, V> NamedCacheClient<K, V> getRemoteNamedCache(InjectionPoint injectionPoint)
        {
        AsyncNamedCacheClient<K, V> async = getAsyncNamedCacheClient(injectionPoint);
        return async.getNamedCacheClient();
        }

    /**
     * Produce an {@link AsyncNamedCache} gRPC client using the name from the {@link RemoteCache}
     * qualifier as the cache name and the name from the optional {@link RemoteSession} qualifier
     * to identify the gRPC {@link com.tangosol.net.Session} to use to connect to the server.
     * <p>
     * If no {@link RemoteSession} qualifier is present the default gRPC channel will be used.
     *
     * @param injectionPoint  the injection point to inject the {@link AsyncNamedCache} into
     * @param <K>             the type of the cache keys
     * @param <V>             the type of the cache values
     *
     * @return a {@link NamedCache} using the name from the {@link RemoteCache} qualifier
     *         as the cache name and the name from the optional {@link RemoteSession} qualifier
     */
    @Produces
    @RemoteCache
    @RemoteSession("")
    public <K, V> AsyncNamedCacheClient<K, V> getRemoteAsyncNamedCache(InjectionPoint injectionPoint)
        {
        return getAsyncNamedCacheClient(injectionPoint);
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Helper to produce an {@link AsyncNamedCacheClient} for required injection points.
     *
     * @param injectionPoint  the target injection point
     * @param <K>             the cache key type
     * @param <V>             the cache value type
     *
     * @return a new {@link AsyncNamedCacheClient} for the specified {@link InjectionPoint}
     */
    protected <K, V> AsyncNamedCacheClient<K, V> getAsyncNamedCacheClient(InjectionPoint injectionPoint)
        {
        Set<Annotation> qualifiers = injectionPoint.getQualifiers();

        String name = qualifiers.stream()
                .filter(q -> RemoteCache.class.isAssignableFrom(q.getClass()))
                .map(q -> ((RemoteCache) q).value())
                .findFirst()
                .orElse(null);

        if (name == null || name.isEmpty())
            {
            name = injectionPoint.getMember().getName();
            }

        GrpcRemoteSession session = f_sessionProducer.getSession(injectionPoint);
        return session.getAsyncCache(name);
        }

    // ----- data members ---------------------------------------------------

    /**
     * Producer for {@link RemoteSessions}.
     */
    private final RemoteSessions f_sessionProducer;
    }
