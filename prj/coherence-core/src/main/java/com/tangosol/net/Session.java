/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net;

import com.tangosol.net.cache.TypeAssertion;

import com.tangosol.net.events.InterceptorRegistry;

import com.tangosol.net.topic.NamedTopic;

import com.tangosol.util.ResourceRegistry;

/**
 * A thread-safe mechanism to request Coherence-based resources, like
 * {@link NamedCache}s, from a deployed module.
 * <p>
 * Resources provided by a {@link Session} are scoped to the {@link Session}.
 * When a {@link Session} is closed, all resources are closed.
 * Once closed, references to resources are no longer valid. Any
 * attempt to use a closed {@link Session} or closed resource may throw an
 * IllegalStateException.
 * <p>
 * The effect of closing a resource is specific to the type of resource.
 * For example, resources provided by a {@link Session} may represent
 * shared (ie: clustered) data-structures and services.  In such circumstances,
 * closing these resources only closes the {@link Session}-based representation,
 * not the underlying shared infrastructure, which may remain active for other
 * {@link Session}s.  Future requests of a {@link Session} for previously closed
 * resources of this kind will likely yield a new reference to logically the
 * same underlying resource.  To destroy such resources, instead of close,
 * resource-specific methods should be used.
 * <p>
 * Applications making use of {@link Session}s are expected to maintain their
 * own references to {@link Session}s.   Coherence provides no mechanism to
 * identify and obtain previously created {@link Session}s.   Furthermore,
 * applications are expected to correctly close {@link Session}s when they are
 * no longer required.
 *
 * @see SessionProvider
 *
 * @author bo 2015.07.27
 */
public interface Session extends AutoCloseable
    {
    // ----- Session methods ------------------------------------------------

    /**
     * Acquire a {@link NamedMap} using the specified {@link com.tangosol.net.NamedMap.Option}s,
     * for example a {@link TypeAssertion}.
     *
     * @param sName    the name of the {@link NamedMap}
     * @param options  the {@link com.tangosol.net.NamedMap.Option}s
     *
     * @param <K>  the type of keys for the {@link NamedMap}
     * @param <V>  the type of values for the {@link NamedMap}
     *
     * @return a {@link NamedMap}
     */
    <K, V> NamedMap<K, V> getMap(String sName, NamedMap.Option... options);

    /**
     * Acquire a {@link NamedCache} using the specified {@link com.tangosol.net.NamedCache.Option}s,
     * for example a {@link TypeAssertion}.
     *
     * @param sName    the name of the {@link NamedCache}
     * @param options  the {@link com.tangosol.net.NamedCache.Option}s
     *
     * @param <K>  the type of keys for the {@link NamedCache}
     * @param <V>  the type of values for the {@link NamedCache}
     *
     * @return a {@link NamedCache}
     */
    <K, V> NamedCache<K, V> getCache(String sName, NamedCache.Option... options);

    /**
     * Acquire a {@link NamedTopic} using the
     * specified {@link ValueTypeAssertion}.
     *
     * @param sName          the name of the {@link NamedTopic}
     *
     * @param <V>  the type of elements for the {@link NamedTopic}
     *
     * @return a {@link NamedCache}
     *
     * @since Coherence 14.1.1
     */
    <V> NamedTopic<V> getTopic(String sName, NamedTopic.Option... options);

    /**
     * Return the {@link ResourceRegistry} for this session.
     *
     * @return the ResourceRegistry for this session
     */
    ResourceRegistry getResourceRegistry();

    /**
     * Return the {@link InterceptorRegistry} for this session.
     * {@link com.tangosol.net.events.EventInterceptor}s registered with
     * this session will be scoped to services and caches created by
     * this session.
     *
     * @return the {@link InterceptorRegistry} for this session
     */
    InterceptorRegistry getInterceptorRegistry();

    /**
     * Validate whether a map with the given name is active in the context
     * of the given {@link ClassLoader}. The ClassLoader should be the same
     * as provided to a previous call to {@link #getMap(String, NamedMap.Option...)}.
     *
     * @param sMapName  the map name
     * @param loader    the ClassLoader used to originally obtain the map
     *
     * @return true if map is active in context of the provided ClassLoader
     */
    boolean isMapActive(String sMapName, ClassLoader loader);

    /**
     * Validate whether a cache with the given name is active in the context
     * of the given {@link ClassLoader}. The ClassLoader should be the same
     * as provided to a previous call to {@link #getCache(String, NamedCache.Option...)}.
     *
     * @param sCacheName  the cache name
     * @param loader      the ClassLoader used to originally obtain the cache
     *
     * @return true if cache is active in context of the provided ClassLoader
     */
    boolean isCacheActive(String sCacheName, ClassLoader loader);

    /**
     * Validate whether a topic with the given name is active in the context
     * of the given {@link ClassLoader}. The ClassLoader should be the same
     * as provided to a previous call to {@link #getTopic(String, NamedTopic.Option...)}.
     *
     * @param sTopicName  the cache name
     * @param loader      the ClassLoader used to originally obtain the topic
     *
     * @return true if topic is active in context of the provided ClassLoader
     */
    boolean isTopicActive(String sTopicName, ClassLoader loader);

    /**
     * Return the name of this session, if available.
     *
     * @return the name of this session, if available; {@code null} otherwise
     */
    String getName();

    /**
     * Return the scope name of this cache factory, if available.
     *
     * @return the scope name of this cache factory, if available; {@code null} otherwise
     */
    String getScopeName();

    /**
     * Return {@code true} if this {@link Session} is active
     * and has not been closed.
     *
     * @return  {@code true} if this {@link Session} is active
     */
    boolean isActive();

    // ----- Option interface -----------------------------------------------

    /**
     * An immutable option for creating and configuring {@link Session}s.
     */
    interface Option
        {
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Create a {@link Session} based on the current calling
     * context and provided {@link Option}s, using the default (auto-detected)
     * {@link SessionProvider},
     *
     * @param options  the {@link Option}s for the {@link Session}
     *
     * @return a {@link Session}
     *
     * @throws IllegalArgumentException
     *              when a {@link Session} or {@link SessionProvider} can't be
     *              acquired using the specified {@link Option}s
     *
     * @throws IllegalStateException
     *              when a {@link SessionProvider} can't be auto-detected
     *
     * @see SessionProvider#createSession(Option...)
     */
    static Session create(Option... options)
            throws IllegalArgumentException, IllegalStateException
        {
        return SessionProvider.get().createSession(options);
        }
    }
