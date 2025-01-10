/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.component.util.safeService.safeTopicService;

import com.tangosol.coherence.component.util.SafeNamedTopic;
import com.tangosol.coherence.component.util.safeService.SafeTopicService;
import com.tangosol.internal.net.topic.impl.paged.model.SubscriberGroupId;
import com.tangosol.net.Service;
import com.tangosol.net.TopicService;
import com.tangosol.net.internal.ScopedTopicReferenceStore;
import com.tangosol.net.topic.NamedTopic;
import com.tangosol.net.topic.Publisher;
import com.tangosol.net.topic.Subscriber;
import com.tangosol.net.topic.TopicBackingMapManager;
import com.tangosol.util.ListMap;

import java.security.PrivilegedAction;
import java.util.Set;

/**
 * A simple safe wrapper around a {@link TopicService}.
 *
 * @author Jonathan Knight  2024.11.26
 */
@SuppressWarnings("rawtypes")
public class SafeSimpleTopicService
        extends    com.tangosol.coherence.component.util.SafeService
        implements SafeTopicService
    {
    /**
     * Property ScopedTopicStore
     *
     */
    private ScopedTopicReferenceStore __m_ScopedTopicStore;

    /**
     * Property TopicBackingMapManager
     *
     */
    private transient TopicBackingMapManager __m_TopicBackingMapManager;

    private static ListMap<String, Class<?>> __mapChildren;

    static
        {
        __initStatic();
        }

    private static void __initStatic()
        {
        // register child classes
        __mapChildren = new ListMap<>();
        __mapChildren.put("DestroyTopicAction", DestroyTopicAction.class);
        __mapChildren.put("EnsureServiceAction", EnsureServiceAction.class);
        __mapChildren.put("ReleaseTopicAction", ReleaseTopicAction.class);
        __mapChildren.put("StartAction", StartAction.class);
        __mapChildren.put("Unlockable", Unlockable.class);
        }

    public SafeSimpleTopicService()
        {
        this(null, null, true);
        }

    public SafeSimpleTopicService(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
        {
        super(sName, compParent, false);
        if (fInit)
            {
            __init();
            }
        }

    @Override
    public void __init()
        {
        // private initialization
        __initPrivate();

        // state initialization: public and protected properties
        try
            {
            setLock(new java.util.concurrent.locks.ReentrantLock());
            setResourceRegistry(new com.tangosol.util.SimpleResourceRegistry());
            setSafeServiceState(0);
            setScopedTopicStore(new com.tangosol.net.internal.ScopedTopicReferenceStore());
            }
        catch (Exception e)
            {
            // re-throw as a runtime exception
            throw new com.tangosol.util.WrapperException(e);
            }

        // containment initialization: children

        // signal the end of the initialization
        set_Constructed(true);
        }

    @Override
    protected void __initPrivate()
        {

        super.__initPrivate();
        }

    /**
     * This is an auto-generated method that returns the map of design time
     * [static] children.
     * <p/>
     * Note: the class generator will ignore any custom implementation for this
     * behavior.
     */
    @Override
    protected java.util.Map get_ChildClasses()
        {
        return __mapChildren;
        }

    @Override
    public int ensureChannelCount(String sName, int cChannel)
        {
        return getRunningTopicService().ensureChannelCount(sName, cChannel);
        }

    @Override
    public int ensureChannelCount(String Param_1, int Param_2, int Param_3)
        {
        return getRunningTopicService().ensureChannelCount(Param_1, Param_2, Param_3);
        }

    @Override
    public int getChannelCount(String Param_1)
        {
        return getRunningTopicService().getChannelCount(Param_1);
        }

    @Override
    protected void cleanup()
        {
        super.cleanup();
        setTopicBackingMapManager(null);
        getScopedTopicStore().clear();
        }

    @Override
    public SafeNamedTopic createSafeTopic(String sName)
        {
        return new SafeSimpleNamedTopic();
        }

    public Publisher createPublisher(String sTopicName)
        {
        return createPublisher(sTopicName, new com.tangosol.net.topic.Publisher.Option[0]);
        }

    public Publisher createPublisher(String sTopicName, Publisher.Option[] options)
        {
        throw new UnsupportedOperationException("Implement this method!");
        }

    public Subscriber createSubscriber(String sTopicName)
        {
        return createSubscriber(sTopicName, new Subscriber.Option[0]);
        }

    public com.tangosol.net.topic.Subscriber createSubscriber(String sTopicName, Subscriber.Option[] options)
        {
        throw new UnsupportedOperationException("Implement this method!");
        }

    @Override
    public void checkClientThread(String sName)
        {
        super.checkClientThread(sName);
        }

    @Override
    public Service getInternalService()
        {
        return super.getInternalService();
        }

    @Override
    public void destroyTopic(NamedTopic topic)
        {
        SafeNamedTopic topicSafe = (SafeNamedTopic) topic;
        removeTopicReference(topicSafe);

        DestroyTopicAction action = new DestroyTopicAction();
        action.setSafeNamedTopic(topicSafe);
        action.setTopicService((TopicService) getInternalService());
        action.run();
        }

    /**
     * Getter for property RunningTopicService.<p>
     */
    @Override
    public TopicService getRunningTopicService()
        {
        return (TopicService) getRunningService();
        }

    // Accessor for the property "ScopedTopicStore"
    /**
     * Getter for property ScopedTopicStore.<p>
     */
    public com.tangosol.net.internal.ScopedTopicReferenceStore getScopedTopicStore()
        {
        return __m_ScopedTopicStore;
        }

    @Override
    public Set<SubscriberGroupId> getSubscriberGroups(String sTopicName)
        {
        return getRunningTopicService().getSubscriberGroups(sTopicName);
        }

    /**
     * Getter for property TopicBackingMapManager.<p>
     */
    @Override
    public TopicBackingMapManager getTopicBackingMapManager()
        {
        return __m_TopicBackingMapManager;
        }

    @Override
    public Set<String> getTopicNames()
        {
        return getRunningTopicService().getTopicNames();
        }

    @Override
    public void releaseTopic(NamedTopic topic)
        {
        SafeNamedTopic topicSafe = (SafeNamedTopic) topic;

        removeTopicReference(topicSafe);

        ReleaseTopicAction action = (ReleaseTopicAction) _newChild("ReleaseTopicAction");
        action.setSafeNamedTopic(topicSafe);
        action.setTopicService((TopicService) getInternalService());

        action.run();
        }

    @Override
    public void removeTopicReference(SafeNamedTopic topicSafe)
        {
        topicSafe.setReleased(true);
        getScopedTopicStore().releaseTopic(topicSafe);
        }

    /**
     * Setter for property ScopedTopicStore.<p>
     */
    public void setScopedTopicStore(com.tangosol.net.internal.ScopedTopicReferenceStore storeTopic)
        {
        __m_ScopedTopicStore = storeTopic;
        }

    /**
     * Setter for property TopicBackingMapManager.<p>
     */
    @Override
    public void setTopicBackingMapManager(com.tangosol.net.topic.TopicBackingMapManager managerMap)
        {
        __m_TopicBackingMapManager = managerMap;
        getRunningTopicService().setTopicBackingMapManager(managerMap);
        }

    @Override
    protected void startService(com.tangosol.net.Service service)
        {
        ((TopicService) service).setTopicBackingMapManager(getTopicBackingMapManager());
        super.startService(service);
        }

    // ----- inner class: DestroyTopicAction --------------------------------

    public static class DestroyTopicAction
            extends    com.tangosol.coherence.component.Util
            implements PrivilegedAction
        {
        /**
         * Property SafeNamedTopic
         *
         */
        private SafeNamedTopic __m_SafeNamedTopic;

        /**
         * Property TopicService
         *
         */
        private TopicService __m_TopicService;

        public DestroyTopicAction()
            {
            this(null, null, true);
            }

        public DestroyTopicAction(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
            {
            super(sName, compParent, false);

            if (fInit)
                {
                __init();
                }
            }

        @Override
        public void __init()
            {
            // private initialization
            __initPrivate();
            // signal the end of the initialization
            set_Constructed(true);
            }

        @Override
        protected void __initPrivate()
            {
            super.__initPrivate();
            }

        /**
         * Getter for property SafeNamedTopic.<p>
         */
        public SafeNamedTopic getSafeNamedTopic()
            {
            return __m_SafeNamedTopic;
            }

        /**
         * Getter for property TopicService.<p>
         */
        public TopicService getTopicService()
            {
            return __m_TopicService;
            }

        @Override
        public Object run()
            {
            TopicService serviceInternal = getTopicService();
            NamedTopic   topicInternal   = getSafeNamedTopic().getNamedTopic();

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

        /**
         * Setter for property SafeNamedTopic.<p>
         */
        public void setSafeNamedTopic(SafeNamedTopic topicNamed)
            {
            __m_SafeNamedTopic = topicNamed;
            }

        /**
         * Setter for property TopicService.<p>
         */
        public void setTopicService(TopicService serviceTopic)
            {
            __m_TopicService = serviceTopic;
            }
        }

    // ----- inner class: ReleaseTopicAction --------------------------------

    public static class ReleaseTopicAction
            extends    com.tangosol.coherence.component.Util
            implements java.security.PrivilegedAction
        {
        // ---- Fields declarations ----

        /**
         * Property SafeNamedTopic
         *
         */
        private SafeNamedTopic __m_SafeNamedTopic;

        /**
         * Property TopicService
         *
         */
        private TopicService __m_TopicService;

        // Default constructor
        public ReleaseTopicAction()
            {
            this(null, null, true);
            }

        // Initializing constructor
        public ReleaseTopicAction(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
            {
            super(sName, compParent, false);

            if (fInit)
                {
                __init();
                }
            }

        // Main initializer
        public void __init()
            {
            // private initialization
            __initPrivate();


            // signal the end of the initialization
            set_Constructed(true);
            }

        // Private initializer
        protected void __initPrivate()
            {

            super.__initPrivate();
            }

        //++ getter for static property _Instance
        /**
         * Getter for property _Instance.<p>
        * Auto generated
         */
        public static com.tangosol.coherence.Component get_Instance()
            {
            return new ReleaseTopicAction();
            }

        //++ getter for static property _CLASS
        /**
         * Getter for property _CLASS.<p>
        * Property with auto-generated accessor that returns the Class object
        * for a given component.
         */
        public static Class get_CLASS()
            {
            Class clz;
            try
                {
                clz = Class.forName("com.tangosol.coherence/component/util/safeService/SafeTopicService$ReleaseTopicAction".replace('/', '.'));
                }
            catch (ClassNotFoundException e)
                {
                throw new NoClassDefFoundError(e.getMessage());
                }
            return clz;
            }

        //++ getter for autogen property _Module
        /**
         * This is an auto-generated method that returns the global [design
        * time] parent component.
        * <p/>
        * Note: the class generator will ignore any custom implementation for
        * this behavior.
         */
        private com.tangosol.coherence.Component get_Module()
            {
            return this.get_Parent();
            }

        // Accessor for the property "SafeNamedTopic"
        /**
         * Getter for property SafeNamedTopic.<p>
         */
        public SafeNamedTopic getSafeNamedTopic()
            {
            return __m_SafeNamedTopic;
            }

        // Accessor for the property "TopicService"
        /**
         * Getter for property TopicService.<p>
         */
        public TopicService getTopicService()
            {
            return __m_TopicService;
            }

        // From interface: java.security.PrivilegedAction
        public Object run()
            {
            // import com.tangosol.net.TopicService;
            // import com.tangosol.net.topic.NamedTopic;

            TopicService serviceInternal = getTopicService();
            NamedTopic   topicInternal   = getSafeNamedTopic().getNamedTopic();

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

        // Accessor for the property "SafeNamedTopic"
        /**
         * Setter for property SafeNamedTopic.<p>
         */
        public void setSafeNamedTopic(SafeNamedTopic topicNamed)
            {
            __m_SafeNamedTopic = topicNamed;
            }

        // Accessor for the property "TopicService"
        /**
         * Setter for property TopicService.<p>
         */
        public void setTopicService(TopicService serviceTopic)
            {
            __m_TopicService = serviceTopic;
            }
        }
    }
