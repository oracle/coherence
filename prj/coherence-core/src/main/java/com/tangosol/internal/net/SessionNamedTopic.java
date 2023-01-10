/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net;

import com.tangosol.net.Service;
import com.tangosol.net.Session;
import com.tangosol.net.ValueTypeAssertion;

import com.tangosol.net.topic.NamedTopic;
import com.tangosol.net.topic.Publisher;
import com.tangosol.net.topic.Subscriber;

import com.tangosol.util.Base;
import com.tangosol.util.Filter;
import com.tangosol.util.HashHelper;
import com.tangosol.util.ValueExtractor;

import java.util.Objects;
import java.util.Set;

/**
 * A {@link Session}-based implementation of {@link NamedTopic}, that delegates
 * requests onto an internal {@link NamedTopic} and isolates developer provided
 * resources so that when a {@link Session} is closed the resources are released.
 *
 * @see Session
 *
 * @author jk 2015.11.27
 * @since Coherence 14.1.1
 */
public class SessionNamedTopic<V>
        implements NamedTopic<V>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a {@link SessionNamedTopic}.
     *
     * @param session        the {@link ConfigurableCacheFactorySession} that produced this {@link SessionNamedTopic}
     * @param topic          the {@link NamedTopic} to which requests will be delegated
     * @param typeAssertion  the {@link ValueTypeAssertion} for the NamedTopic
     */
    @SuppressWarnings("unused")
    public SessionNamedTopic(ConfigurableCacheFactorySession session, NamedTopic<V> topic,
                             ValueTypeAssertion<V> typeAssertion)
        {
        this(session, topic, session.getConfigurableCacheFactory().getClass().getClassLoader(), typeAssertion);
        }

    /**
     * Constructs a {@link SessionNamedTopic}.
     *
     * @param session        the {@link ConfigurableCacheFactorySession} that produced this {@link SessionNamedTopic}
     * @param topic          the {@link NamedTopic} to which requests will be delegated
     * @param loader         the {@link ClassLoader} associated with the topic
     * @param typeAssertion  the {@link ValueTypeAssertion} for the NamedTopic
     */
    public SessionNamedTopic(ConfigurableCacheFactorySession session,
                             NamedTopic<V> topic,
                             ClassLoader loader,
                             ValueTypeAssertion<V> typeAssertion)
        {
        f_topic         = Objects.requireNonNull(topic);
        f_session       = Objects.requireNonNull(session);
        f_typeAssertion = typeAssertion;
        f_loader        = Objects.requireNonNull(loader);

        m_fActive       = f_topic.isActive();
        }


    // ----- SessionNamedTopic methods --------------------------------------

    /**
     * Obtain the wrapped {@link NamedTopic}.
     *
     * @return  the wrapped {@link NamedTopic}
     */
    public NamedTopic<V> getInternalNamedTopic()
        {
        return f_topic;
        }

    /**
     * Obtain the {@link ValueTypeAssertion} to use to
     * assert the type of topic values.
     *
     * @return the {@link ValueTypeAssertion} to use to
     * assert the type of topic values
     */
    @SuppressWarnings("unchecked")
    ValueTypeAssertion<V> getTypeAssertion()
        {
        return f_typeAssertion;
        }

    /**
     * Perform any pre-close actions.
     */
    void onClosing()
        {
        m_fActive = false;
        }

    /**
     * Perform any post-close actions.
     */
    void onClosed()
        {
        }

    /**
     * Perform any pre-destroy actions.
     */
    void onDestroying()
        {
        m_fActive = false;
        }

    /**
     * Perform any post-destroy actions.
     */
    void onDestroyed()
        {
        }

    // ----- NamedTopic methods ---------------------------------------------

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Subscriber<V> createSubscriber(Subscriber.Option... options)
        {
        return f_topic.createSubscriber(options);
        }

    @Override
    public void destroySubscriberGroup(String sGroupName)
        {
        f_topic.destroySubscriberGroup(sGroupName);
        }

    @Override
    public Set<String> getSubscriberGroups()
        {
        return f_topic.getSubscriberGroups();
        }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Publisher<V> createPublisher(Publisher.Option... options)
        {
        return f_topic.createPublisher(options);
        }

    @Override
    public void release()
        {
        if (m_fActive)
            {
            // closing a NamedTopic is always delegated to the Session to manage
            f_session.onClose(this);
            }
        }

    @Override
    public void destroy()
        {
        if (m_fActive)
            {
            // destroying a NamedTopic is always delegated to the Session to manage
            f_session.onDestroy(this);
            }
        }

    @Override
    public String getName()
        {
        return f_topic.getName();
        }

    @Override
    public Service getService()
        {
        return f_topic.getService();
        }

    @Override
    public boolean isActive()
        {
        return m_fActive && f_topic.isActive();
        }

    @Override
    public boolean isDestroyed()
        {
        return f_topic.isDestroyed();
        }

    @Override
    public boolean isReleased()
        {
        return f_topic.isReleased();
        }

    @Override
    public int getChannelCount()
        {
        return f_topic.getChannelCount();
        }

    @Override
    public void ensureSubscriberGroup(String sName, Filter<?> filter, ValueExtractor<?, ?> extractor)
        {
        f_topic.ensureSubscriberGroup(sName, filter, extractor);
        }

    /**
     * Return the {@link ClassLoader} used by the wrapped topic.
     *
     * @return the {@link ClassLoader} used by the wrapped topic
     */
    ClassLoader getContextClassLoader()
        {
        return f_loader;
        }

    @Override
    public int getRemainingMessages(String sSubscriberGroup, int... anChannel)
        {
        return f_topic.getRemainingMessages(sSubscriberGroup, anChannel);
        }

    // ----- object methods -------------------------------------------------

    @Override
    @SuppressWarnings("rawtypes")
    public boolean equals(Object obj)
        {
        if (obj instanceof SessionNamedTopic)
            {
            SessionNamedTopic sessionOther = (SessionNamedTopic) obj;

            return Base.equals(f_session, sessionOther.f_session)
                    && Base.equals(f_topic, sessionOther.f_topic)
                    && f_loader.equals(sessionOther.f_loader);
            }

        return false;
        }

    @Override
    public int hashCode()
        {
        int hash = HashHelper.hash(f_session, 31);

        return HashHelper.hash(f_topic, hash) + f_loader.hashCode();
        }

    @Override
    public String toString()
        {
        return f_topic.toString();
        }

    // ----- data members ---------------------------------------------------

    /**
     * The wrapped {@link NamedTopic}.
     */
    private final NamedTopic<V> f_topic;

    /**
     * The {@link ClassLoader} associated with this session's topic.
     */
    private final ClassLoader f_loader;

    /**
     * The {@link ConfigurableCacheFactorySession} used to create the topic.
     */
    private final ConfigurableCacheFactorySession f_session;

    /**
     * The {@link ValueTypeAssertion} used to assert the topic value types.
     */
    @SuppressWarnings("rawtypes")
    private final ValueTypeAssertion f_typeAssertion;

    /**
     * A flag indicating whether the topic is active.
     */
    private volatile boolean m_fActive;
    }
