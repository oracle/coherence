/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.component.util.safeService;

import com.tangosol.coherence.component.net.Security;

import com.tangosol.coherence.component.util.SafeCluster;
import com.tangosol.coherence.component.util.SafeNamedTopic;

import com.tangosol.internal.net.topic.impl.paged.model.SubscriberGroupId;

import com.tangosol.net.Service;
import com.tangosol.net.TopicService;

import com.tangosol.net.internal.ScopedTopicReferenceStore;

import com.tangosol.net.security.SecurityHelper;

import com.tangosol.net.topic.NamedTopic;

import java.security.PrivilegedAction;

import java.util.Set;

/**
 * The safe layer for a {@link TopicService}.
 *
 * @author Jonathan Knight  2024.11.26
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public interface SafeTopicService
        extends TopicService
    {
    /**
     * Check if the current thread is either the service thread or one of its
     * daemons.  If true, issue a warning that this could lead to deadlock.
     * <p>
     * This is used to issue a warning for the following type of execution:
     * <p>
     * 1. Client thread calls a method M which is normally intended for client
     * threads. Method M obtains a syncronization monitor S, issues a request to
     * a service, and blocks waiting for a response.
     * 2. The service thread or a service worker thread handles the request,
     * calls an external module, which in turn calls a method M (or any other
     * method that may obtain monitor S).
     * 3. Deadlock may occur.
     * <p>
     * In this case the method M should include a call to this method.
     */
    void checkClientThread(String sName);

    /**
     * Obtain the SafeCluster this SafeService belongs to.
     *
     * @return the SafeCluster this SafeService belongs to
     */
    SafeCluster getSafeCluster();

    /**
     * Obtain the name of this service.
     *
     * @return the name of this service
     */
    String getServiceName();

    /**
     * Obtain the underlying service.
     *
     * @return the underlying service
     */
    com.tangosol.util.Service getRunningService();

    /**
     * Obtain the underlying {@link TopicService}.
     *
     * @return the underlying {@link TopicService}
     */
    default TopicService getRunningTopicService()
        {
        return (TopicService) getRunningService();
        }

    /**
     * Obtain the underlying service.
     *
     * @return the underlying service
     */
    Service getInternalService();

    /**
     * Obtain the {@link ScopedTopicReferenceStore} holding the topics
     * managed by this service.
     *
     * @return the {@link ScopedTopicReferenceStore} holding the topics
     *         managed by this service
     */
    ScopedTopicReferenceStore getScopedTopicStore();

    /**
     * Create a safe topic.
     *
     * @param sName  the name of the topic
     *
     * @return the new safe topic instance
     */
    SafeNamedTopic createSafeTopic(String sName);

    @Override
    default <T> NamedTopic<T> ensureTopic(String sName, ClassLoader loader)
        {
        checkClientThread("ensureTopic");

        if (sName == null || sName.isEmpty())
            {
            sName = "Default";
            }

        if (loader == null)
            {
            loader = getContextClassLoader();
            }

        Security.checkPermission(getSafeCluster(), getServiceName(), sName, "join");

        com.tangosol.net.internal.ScopedTopicReferenceStore store     = getScopedTopicStore();
        SafeNamedTopic                                      topicSafe = (SafeNamedTopic) store.getTopic(sName, loader);

        if (topicSafe != null)
            {
            if (topicSafe.isActive()
                    || (!topicSafe.isDestroyed()
                    && !topicSafe.isReleased()))
                {
                return topicSafe;
                }
            else
                {
                // don't return a released/destroyed SafeNamedTopic; allow a new one to be returned.
                topicSafe = null;
                }
            }

        // ensure no released/destroyed topic refs in store
        store.clearInactiveTopicRefs();

        while (topicSafe == null)
            {
            NamedTopic topic = getRunningTopicService().ensureTopic(sName, loader);

            topicSafe = createSafeTopic(sName);
            topicSafe.setSubject(SecurityHelper.getCurrentSubject());
            topicSafe.setSafeTopicService(this);
            topicSafe.setTopicName(sName);
            topicSafe.setClassLoader(loader);
            topicSafe.setInternalNamedTopic(topic);
            topicSafe.setStarted(true);

            if (store.putTopicIfAbsent(topicSafe, loader) == null)
                {
                break;
                }

            topicSafe = (SafeNamedTopic) store.getTopic(sName, loader);
            }

        return topicSafe;
        }

    @Override
    default void releaseTopic(NamedTopic<?> topic)
        {
        SafeNamedTopic topicSafe = (SafeNamedTopic) topic;

        removeTopicReference(topicSafe);

        ReleaseTopicAction action = new ReleaseTopicAction();
        action.setSafeNamedTopic(topicSafe);
        action.setTopicService((TopicService) getInternalService());

        action.run();
        }

    @Override
    default void destroyTopic(NamedTopic<?> topic)
        {
        Security.checkPermission(getSafeCluster(),
                getServiceName(), topic.getName(), "destroy");

        SafeNamedTopic topicSafe = (SafeNamedTopic) topic;

        removeTopicReference(topicSafe);

        DestroyTopicAction action = new DestroyTopicAction();
        action.setSafeNamedTopic(topicSafe);
        action.setTopicService((TopicService) getInternalService());

        action.run();
        }

    @Override
    default int getChannelCount(String sName)
        {
        return getRunningTopicService().getChannelCount(sName);
        }

    @Override
    default int ensureChannelCount(String sName, int cRequired, int cChannel)
        {
        return getRunningTopicService().ensureChannelCount(sName, cRequired, cChannel);
        }

    @Override
    default Set<String> getTopicNames()
        {
        return getRunningTopicService().getTopicNames();
        }

    @Override
    default Set<SubscriberGroupId> getSubscriberGroups(String sTopicName)
        {
        return getRunningTopicService().getSubscriberGroups(sTopicName);
        }

    /**
     * Remove the stored reference to a topic.
     *
     * @param topicSafe  the topic reference to remove
     */
    default void removeTopicReference(SafeNamedTopic topicSafe)
        {
        topicSafe.setReleased(true);
        getScopedTopicStore().releaseTopic(topicSafe);
        }

    // ----- inner class: DestroyTopicAction --------------------------------

    /**
     * A {@link PrivilegedAction} to destroy a topic.
     */
    class DestroyTopicAction
            extends com.tangosol.coherence.component.Util
            implements PrivilegedAction
        {
        /**
         * Property SafeNamedTopic
         */
        private SafeNamedTopic __m_SafeNamedTopic;

        /**
         * Property TopicService
         */
        private TopicService __m_TopicService;

        public DestroyTopicAction()
            {
            super(null, null, true);
            }

        /**
         * Getter for property SafeNamedTopic.<p>
         */
        public SafeNamedTopic getSafeNamedTopic()
            {
            return __m_SafeNamedTopic;
            }

        /**
         * Setter for property SafeNamedTopic.<p>
         */
        public void setSafeNamedTopic(SafeNamedTopic topicNamed)
            {
            __m_SafeNamedTopic = topicNamed;
            }

        /**
         * Getter for property TopicService.<p>
         */
        public TopicService getTopicService()
            {
            return __m_TopicService;
            }

        /**
         * Setter for property TopicService.<p>
         */
        public void setTopicService(TopicService serviceTopic)
            {
            __m_TopicService = serviceTopic;
            }

        @Override
        public Object run()
            {
            TopicService   serviceInternal = getTopicService();
            SafeNamedTopic topicSafe       = getSafeNamedTopic();
            NamedTopic     topicInternal   = topicSafe.getNamedTopic();

            if (topicInternal == null)
                {
                throw new IllegalStateException("Topic is already released");
                }

            try
                {
                serviceInternal.destroyTopic(topicInternal);
                }
            catch (RuntimeException e)
                {
                if (serviceInternal != null && serviceInternal.isRunning())
                    {
                    throw e;
                    }
                }

            return null;
            }
        }

    // ----- inner class: ReleaseTopicAction --------------------------------

    /**
     * A {@link PrivilegedAction} to release a topic.
     */
    class ReleaseTopicAction
            extends com.tangosol.coherence.component.Util
            implements PrivilegedAction
        {
        /**
         * Property SafeNamedTopic
         */
        private SafeNamedTopic __m_SafeNamedTopic;

        /**
         * Property TopicService
         */
        private TopicService __m_TopicService;

        public ReleaseTopicAction()
            {
            super(null, null, true);
            }

        /**
         * Getter for property SafeNamedTopic.<p>
         */
        public SafeNamedTopic getSafeNamedTopic()
            {
            return __m_SafeNamedTopic;
            }

        /**
         * Setter for property SafeNamedTopic.<p>
         */
        public void setSafeNamedTopic(SafeNamedTopic topicNamed)
            {
            __m_SafeNamedTopic = topicNamed;
            }

        /**
         * Getter for property TopicService.<p>
         */
        public TopicService getTopicService()
            {
            return __m_TopicService;
            }

        /**
         * Setter for property TopicService.<p>
         */
        public void setTopicService(TopicService serviceTopic)
            {
            __m_TopicService = serviceTopic;
            }

        @Override
        public Object run()
            {
            TopicService   serviceInternal = getTopicService();
            SafeNamedTopic topicSafe       = getSafeNamedTopic();
            NamedTopic     topicInternal   = topicSafe.getNamedTopic();

            if (topicInternal == null)
                {
                throw new IllegalStateException("Topic is already released");
                }

            try
                {
                serviceInternal.releaseTopic(topicInternal);
                }
            catch (RuntimeException e)
                {
                if (serviceInternal != null && serviceInternal.isRunning())
                    {
                    throw e;
                    }
                }

            return null;
            }
        }
    }
