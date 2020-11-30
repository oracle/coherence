/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net;

import com.tangosol.net.events.InterceptorRegistry;

import com.tangosol.net.topic.NamedTopic;

import com.tangosol.util.ResourceRegistry;

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
     * Constructs a {@link CoherenceSession} based on the specified
     * {@link Option}s using the default {@link SessionProvider}.
     *
     * @param options  the {@link Option}s for the {@link CoherenceSession}
     */
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
