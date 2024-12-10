/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net;

import com.tangosol.net.events.EventInterceptor;
import com.tangosol.net.events.InterceptorRegistry;

import com.tangosol.net.topic.NamedTopic;

import com.tangosol.util.ResourceRegistry;

import java.util.Collections;
import java.util.Optional;

/**
 * An implementation of a {@link Session} allowing applications to use
 * the new operator to create a {@link Session} via the default
 * {@link SessionProvider}.
 *
 * @see Session
 * @see SessionProvider
 *
 * @author bo  2015.09.27
 */
public class CoherenceSession
        implements Session
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a {@link CoherenceSession} using the default session configuration.
     *
     * @throws IllegalStateException if a session could not be created
     * @see SessionConfiguration#defaultSession()
     */
    public CoherenceSession()
        {
        this(SessionConfiguration.defaultSession(), Coherence.Mode.ClusterMember, Collections.emptyList());
        }

    /**
     * Constructs a {@link CoherenceSession} based on the specified {@link SessionConfiguration}.
     *
     * @param configuration  the {@link SessionConfiguration}s for the {@link CoherenceSession}
     * @param mode           the mode Coherence is running in
     * @param interceptors   optional {@link EventInterceptor interceptors} to add to
     *                       the session in addition to any in the configuration
     *
     * @throws IllegalStateException if a session could not be created
     */
    public CoherenceSession(SessionConfiguration configuration,
                Coherence.Mode mode, Iterable<? extends EventInterceptor<?>> interceptors)
        {
        Optional<Session> optional = SessionProvider.get().createSession(configuration, mode, interceptors);
        m_session = optional.orElseThrow(() ->
                new IllegalStateException("SessionProvider did not create a Session from the specified options"));
        }

    /**
     * Constructs a {@link CoherenceSession} based on the specified
     * {@link Option}s using the default {@link SessionProvider}.
     *
     * @param options  the {@link Option}s for the {@link CoherenceSession}
     *
     * @deprecated since 20.12 use {@link #CoherenceSession(SessionConfiguration, Coherence.Mode, Iterable)}
     *
     * @throws IllegalStateException if a session could not be created
     */
    @Deprecated
    public CoherenceSession(Option... options)
        {
        Session session = SessionProvider.get().createSession(options);
        if (session == null)
            {
            throw new IllegalStateException("SessionProvider did not create a Session from the specified options");
            }
        m_session = session;
        }

    // ----- Session methods ------------------------------------------------

    @Override
    public <K, V> NamedMap<K, V> getMap(String sName, NamedMap.Option... options)
        {
        return m_session.getMap(sName, options);
        }

    @Override
    public <K, V> NamedCache<K, V> getCache(String sName, NamedCache.Option... options)
        {
        return m_session.getCache(sName, options);
        }

    @Override
    public <V> NamedTopic<V> getTopic(String sName, NamedTopic.Option... options)
        {
        return m_session.getTopic(sName, options);
        }

    @Override
    public void close(NamedCollection col)
        {
        m_session.close(col);
        }

    @Override
    public void destroy(NamedCollection col)
        {
        m_session.destroy(col);
        }

    @Override
    public void close() throws Exception
        {
        m_session.close();
        }

    @Override
    public ResourceRegistry getResourceRegistry()
        {
        return m_session.getResourceRegistry();
        }

    @Override
    public InterceptorRegistry getInterceptorRegistry()
        {
        return m_session.getInterceptorRegistry();
        }

    @Override
    public String getName()
        {
        return m_session.getName();
        }

    @Override
    public String getScopeName()
        {
        return m_session.getScopeName();
        }

    @Override
    public boolean isCacheActive(String sCacheName, ClassLoader loader)
        {
        return m_session.isCacheActive(sCacheName, loader);
        }

    @Override
    public boolean isMapActive(String sMapName, ClassLoader loader)
        {
        return m_session.isMapActive(sMapName, loader);
        }

    @Override
    public boolean isTopicActive(String sTopicName, ClassLoader loader)
        {
        return m_session.isTopicActive(sTopicName, loader);
        }

    @Override
    public void activate()
        {
        m_session.activate();
        }

    @Override
    public boolean isActive()
        {
        return m_session.isActive();
        }

    @Override
    public Service getService(String sServiceName)
        {
        return m_session.getService(sServiceName);
        }

    // ----- data members ---------------------------------------------------

    /**
     * The underlying {@link Session} to which this implementation will
     * delegate requests.
     */
    protected Session m_session;
    }
