
/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.util.safeService.safeCacheService.safeDistributedCacheService.SafePagedTopicService

package com.tangosol.coherence.component.util.safeService.safeCacheService.safeDistributedCacheService;

import com.tangosol.coherence.component.net.Security;
import com.tangosol.coherence.component.util.SafeNamedTopic;
import com.tangosol.internal.net.topic.impl.paged.PagedTopic;
import com.tangosol.internal.net.topic.impl.paged.PagedTopicBackingMapManager;
import com.tangosol.internal.net.topic.impl.paged.PagedTopicCaches;
import com.tangosol.internal.net.topic.impl.paged.PagedTopicConfigMap;
import com.tangosol.internal.net.topic.impl.paged.model.PagedTopicSubscription;
import com.tangosol.net.PagedTopicService;
import com.tangosol.net.TopicService;
import com.tangosol.net.security.SecurityHelper;
import com.tangosol.net.topic.NamedTopic;
import java.security.AccessController;

/*
* Integrates
*     com.tangosol.net.PagedTopicService
*     using Component.Dev.Compiler.Integrator.Wrapper
*/
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class SafePagedTopicService
        extends    com.tangosol.coherence.component.util.safeService.safeCacheService.SafeDistributedCacheService
        implements com.tangosol.net.PagedTopicService
    {
    // ---- Fields declarations ----
    
    /**
     * Property ScopedTopicStore
     *
     */
    private com.tangosol.net.internal.ScopedTopicReferenceStore __m_ScopedTopicStore;
    private static com.tangosol.util.ListMap __mapChildren;
    
    // Static initializer
    static
        {
        __initStatic();
        }
    
    // Default static initializer
    private static void __initStatic()
        {
        // register child classes
        __mapChildren = new com.tangosol.util.ListMap();
        __mapChildren.put("DestroyCacheAction", com.tangosol.coherence.component.util.safeService.SafeCacheService.DestroyCacheAction.get_CLASS());
        __mapChildren.put("DestroyTopicAction", SafePagedTopicService.DestroyTopicAction.get_CLASS());
        __mapChildren.put("EnsureServiceAction", com.tangosol.coherence.component.util.SafeService.EnsureServiceAction.get_CLASS());
        __mapChildren.put("ReleaseCacheAction", com.tangosol.coherence.component.util.safeService.SafeCacheService.ReleaseCacheAction.get_CLASS());
        __mapChildren.put("ReleaseTopicAction", SafePagedTopicService.ReleaseTopicAction.get_CLASS());
        __mapChildren.put("StartAction", com.tangosol.coherence.component.util.SafeService.StartAction.get_CLASS());
        __mapChildren.put("Unlockable", com.tangosol.coherence.component.util.SafeService.Unlockable.get_CLASS());
        }
    
    // Default constructor
    public SafePagedTopicService()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public SafePagedTopicService(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
        
        // state initialization: public and protected properties
        try
            {
            setLock(new java.util.concurrent.locks.ReentrantLock());
            setResourceRegistry(new com.tangosol.util.SimpleResourceRegistry());
            setSafeServiceState(0);
            setScopedCacheStore(new com.tangosol.net.internal.ScopedCacheReferenceStore());
            }
        catch (java.lang.Exception e)
            {
            // re-throw as a runtime exception
            throw new com.tangosol.util.WrapperException(e);
            }
        
        // containment initialization: children
        
        // signal the end of the initialization
        set_Constructed(true);
        }
    
    // Private initializer
    protected void __initPrivate()
        {
        
        super.__initPrivate();
        
        // state initialization: private properties
        try
            {
            __m_ScopedTopicStore = new com.tangosol.net.internal.ScopedTopicReferenceStore();
            }
        catch (java.lang.Exception e)
            {
            // re-throw as a runtime exception
            throw new com.tangosol.util.WrapperException(e);
            }
        }
    
    //++ getter for static property _Instance
    /**
     * Getter for property _Instance.<p>
    * Auto generated
     */
    public static com.tangosol.coherence.Component get_Instance()
        {
        return new com.tangosol.coherence.component.util.safeService.safeCacheService.safeDistributedCacheService.SafePagedTopicService();
        }
    
    //++ getter for static property _CLASS
    /**
     * Getter for property _CLASS.<p>
    * Property with auto-generated accessor that returns the Class object for a
    * given component.
     */
    public static Class get_CLASS()
        {
        Class clz;
        try
            {
            clz = Class.forName("com.tangosol.coherence/component/util/safeService/safeCacheService/safeDistributedCacheService/SafePagedTopicService".replace('/', '.'));
            }
        catch (ClassNotFoundException e)
            {
            throw new NoClassDefFoundError(e.getMessage());
            }
        return clz;
        }
    
    //++ getter for autogen property _Module
    /**
     * This is an auto-generated method that returns the global [design time]
    * parent component.
    * 
    * Note: the class generator will ignore any custom implementation for this
    * behavior.
     */
    private com.tangosol.coherence.Component get_Module()
        {
        return this;
        }
    
    //++ getter for autogen property _ChildClasses
    /**
     * This is an auto-generated method that returns the map of design time
    * [static] children.
    * 
    * Note: the class generator will ignore any custom implementation for this
    * behavior.
     */
    protected java.util.Map get_ChildClasses()
        {
        return __mapChildren;
        }
    
    //++ com.tangosol.net.PagedTopicService integration
    // Access optimization
    // properties integration
    // methods integration
    @Override
    public void destroySubscriberGroup(String Param_1, String Param_2)
        {
        ((com.tangosol.net.PagedTopicService) getRunningCacheService()).destroySubscriberGroup(Param_1, Param_2);
        }
    @Override
    public void destroySubscription(long lSubscriptionId)
        {
        ((com.tangosol.net.PagedTopicService) getRunningCacheService()).destroySubscription(lSubscriptionId);
        }
    @Override
    public void destroySubscription(long Param_1, com.tangosol.net.topic.Subscriber.Id Param_2)
        {
        ((com.tangosol.net.PagedTopicService) getRunningCacheService()).destroySubscription(Param_1, Param_2);
        }
    @Override
    public int ensureChannelCount(String Param_1, int Param_2)
        {
        return ((com.tangosol.net.PagedTopicService) getRunningCacheService()).ensureChannelCount(Param_1, Param_2);
        }
    @Override
    public int ensureChannelCount(String Param_1, int Param_2, int Param_3)
        {
        return ((com.tangosol.net.PagedTopicService) getRunningCacheService()).ensureChannelCount(Param_1, Param_2, Param_3);
        }
    @Override
    public long ensureSubscriberGroup(String Param_1, String Param_2)
        {
        return ((com.tangosol.net.PagedTopicService) getRunningCacheService()).ensureSubscriberGroup(Param_1, Param_2);
        }
    @Override
    public long ensureSubscriberGroup(String sTopicName, String sGroupName, com.tangosol.util.Filter filter, com.tangosol.util.ValueExtractor extractor)
        {
        return ((com.tangosol.net.PagedTopicService) getRunningCacheService()).ensureSubscriberGroup(sTopicName, sGroupName, filter, extractor);
        }
    @Override
    public void ensureSubscription(String Param_1, long Param_2, com.tangosol.net.topic.Subscriber.Id Param_3)
        {
        ((com.tangosol.net.PagedTopicService) getRunningCacheService()).ensureSubscription(Param_1, Param_2, Param_3);
        }
    @Override
    public void ensureSubscription(String Param_1, long Param_2, com.tangosol.net.topic.Subscriber.Id Param_3, boolean Param_4)
        {
        ((com.tangosol.net.PagedTopicService) getRunningCacheService()).ensureSubscription(Param_1, Param_2, Param_3, Param_4);
        }
    @Override
    public long ensureSubscription(String Param_1, com.tangosol.internal.net.topic.impl.paged.model.SubscriberGroupId Param_2, com.tangosol.net.topic.Subscriber.Id Param_3, com.tangosol.util.Filter Param_4, com.tangosol.util.ValueExtractor Param_5)
        {
        return ((com.tangosol.net.PagedTopicService) getRunningCacheService()).ensureSubscription(Param_1, Param_2, Param_3, Param_4, Param_5);
        }
    private com.tangosol.net.topic.NamedTopic ensureTopic$Router(String sName, ClassLoader loader)
        {
        return ((com.tangosol.net.PagedTopicService) getRunningCacheService()).ensureTopic(sName, loader);
        }
    @Override
    public com.tangosol.net.topic.NamedTopic ensureTopic(String sName, ClassLoader loader)
        {
        // import Component.Net.Security;
        // import Component.Util.SafeNamedTopic;
        // import com.tangosol.internal.net.topic.impl.paged.PagedTopicCaches;
        // import com.tangosol.internal.net.topic.impl.paged.PagedTopic;
        // import com.tangosol.net.security.SecurityHelper;
        // import com.tangosol.net.internal.ScopedTopicReferenceStore as com.tangosol.net.internal.ScopedTopicReferenceStore;
        
        checkClientThread("ensureTopic");
        
        if (sName == null || sName.length() == 0)
            {
            sName = "Default";
            }
        
        if (loader == null)
            {
            loader = getContextClassLoader();
            }
        
        Security.checkPermission(getSafeCluster(), getServiceName(), sName, "join");
        
        com.tangosol.net.internal.ScopedTopicReferenceStore     store     = getScopedTopicStore();
        SafeNamedTopic topicSafe = (SafeNamedTopic) store.getTopic(sName, loader);
        
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
            PagedTopic topic = (PagedTopic) ensureTopic$Router(sName, loader);
        
            topicSafe = new SafeNamedTopic();
            topicSafe.setSubject(SecurityHelper.getCurrentSubject());
            topicSafe.setSafeTopicService(this);
            topicSafe.setTopicName(sName);
            topicSafe.setClassLoader(loader);
            topicSafe.setInternalNamedTopic(topic);
            topicSafe.setStarted(true);
            topicSafe.setPagedTopicCaches(new PagedTopicCaches(sName, this));
        
            if (store.putTopicIfAbsent(topicSafe, loader) == null)
                {
                break;
                }
        
            topicSafe = (SafeNamedTopic) store.getTopic(sName, loader);
            }
        
        return topicSafe;
        }
    @Override
    public int getChannelCount(String sTopic)
        {
        return ((com.tangosol.net.PagedTopicService) getRunningCacheService()).getChannelCount(sTopic);
        }
    @Override
    public java.util.Set getSubscriberGroups(String Param_1)
        {
        return ((com.tangosol.net.PagedTopicService) getRunningCacheService()).getSubscriberGroups(Param_1);
        }
    @Override
    public java.util.Set getSubscribers(String Param_1, com.tangosol.internal.net.topic.impl.paged.model.SubscriberGroupId Param_2)
        {
        return ((com.tangosol.net.PagedTopicService) getRunningCacheService()).getSubscribers(Param_1, Param_2);
        }

    @Override
    public boolean hasSubscribers(String sTopicName)
        {
        return ((com.tangosol.net.PagedTopicService) getRunningCacheService()).hasSubscribers(sTopicName);
        }

    @Override
    public long getSubscriptionCount(String sTopicName)
        {
        return ((com.tangosol.net.PagedTopicService) getRunningCacheService()).getSubscriptionCount(sTopicName);
        }

    @Override
    public com.tangosol.internal.net.topic.impl.paged.model.PagedTopicSubscription getSubscription(long Param_1)
        {
        return ((com.tangosol.net.PagedTopicService) getRunningCacheService()).getSubscription(Param_1);
        }
    @Override
    public long getSubscriptionId(String Param_1, com.tangosol.internal.net.topic.impl.paged.model.SubscriberGroupId Param_2)
        {
        return ((com.tangosol.net.PagedTopicService) getRunningCacheService()).getSubscriptionId(Param_1, Param_2);
        }
    @Override
    public java.util.Set getTopicNames()
        {
        return ((com.tangosol.net.PagedTopicService) getRunningCacheService()).getTopicNames();
        }
    @Override
    public com.tangosol.internal.net.topic.impl.paged.statistics.PagedTopicStatistics getTopicStatistics(String Param_1)
        {
        return ((com.tangosol.net.PagedTopicService) getRunningCacheService()).getTopicStatistics(Param_1);
        }
    @Override
    public boolean hasSubscription(long Param_1)
        {
        return ((com.tangosol.net.PagedTopicService) getRunningCacheService()).hasSubscription(Param_1);
        }
    @Override
    public boolean isSubscriptionDestroyed(long Param_1)
        {
        return ((com.tangosol.net.PagedTopicService) getRunningCacheService()).isSubscriptionDestroyed(Param_1);
        }
    //-- com.tangosol.net.PagedTopicService integration
    
    // From interface: com.tangosol.net.PagedTopicService
    @Override
    public void destroyTopic(com.tangosol.net.topic.NamedTopic topic)
        {
        // import Component.Net.Security;
        // import Component.Util.SafeNamedTopic;
        // import com.tangosol.net.TopicService;
        // import com.tangosol.net.topic.NamedTopic;
        // import java.security.AccessController;
        
        Security.checkPermission(getSafeCluster(),
            getServiceName(), topic.getName(), "destroy");
        
        SafeNamedTopic topicSafe = (SafeNamedTopic) topic;
        
        removeTopicReference(topicSafe);
        
        SafePagedTopicService.DestroyTopicAction action = (SafePagedTopicService.DestroyTopicAction) _newChild("DestroyTopicAction");
        action.setSafeNamedTopic(topicSafe);
        action.setTopicService((TopicService) getInternalService());
        
        AccessController.doPrivileged(action);
        }
    
    // Accessor for the property "RunningTopicService"
    /**
     * Getter for property RunningTopicService.<p>
     */
    public com.tangosol.net.TopicService getRunningTopicService()
        {
        // import com.tangosol.net.TopicService;
        
        return (TopicService) getRunningService();
        }
    
    // Accessor for the property "ScopedTopicStore"
    /**
     * Getter for property ScopedTopicStore.<p>
     */
    private com.tangosol.net.internal.ScopedTopicReferenceStore getScopedTopicStore()
        {
        return __m_ScopedTopicStore;
        }
    
    // From interface: com.tangosol.net.PagedTopicService
    @Override
    public com.tangosol.internal.net.topic.impl.paged.PagedTopicBackingMapManager getTopicBackingMapManager()
        {
        // import com.tangosol.internal.net.topic.impl.paged.PagedTopicBackingMapManager;
        
        return (PagedTopicBackingMapManager) getBackingMapManager();
        }
    
    // From interface: com.tangosol.net.PagedTopicService
    @Override
    public void releaseTopic(com.tangosol.net.topic.NamedTopic topic)
        {
        // import Component.Util.SafeNamedTopic;
        // import com.tangosol.net.TopicService;
        // import com.tangosol.net.topic.NamedTopic;
        // import java.security.AccessController;
        
        SafeNamedTopic topicSafe = (SafeNamedTopic) topic;
        
        removeTopicReference(topicSafe);
        
        SafePagedTopicService.ReleaseTopicAction action = (SafePagedTopicService.ReleaseTopicAction) _newChild("ReleaseTopicAction");
        action.setSafeNamedTopic(topicSafe);
        action.setTopicService((TopicService) getInternalService());
        
        AccessController.doPrivileged(action);
        }
    
    protected void removeTopicReference(com.tangosol.coherence.component.util.SafeNamedTopic topicSafe)
        {
        topicSafe.setReleased(true);
        getScopedTopicStore().releaseTopic(topicSafe);
        }
    
    // Accessor for the property "ScopedTopicStore"
    /**
     * Setter for property ScopedTopicStore.<p>
     */
    private void setScopedTopicStore(com.tangosol.net.internal.ScopedTopicReferenceStore storeTopic)
        {
        __m_ScopedTopicStore = storeTopic;
        }

    // ---- class: com.tangosol.coherence.component.util.safeService.safeCacheService.safeDistributedCacheService.SafePagedTopicService$DestroyTopicAction
    
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class DestroyTopicAction
            extends    com.tangosol.coherence.component.Util
            implements java.security.PrivilegedAction
        {
        // ---- Fields declarations ----
        
        /**
         * Property SafeNamedTopic
         *
         */
        private com.tangosol.coherence.component.util.SafeNamedTopic __m_SafeNamedTopic;
        
        /**
         * Property TopicService
         *
         */
        private com.tangosol.net.TopicService __m_TopicService;
        
        // Default constructor
        public DestroyTopicAction()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public DestroyTopicAction(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.util.safeService.safeCacheService.safeDistributedCacheService.SafePagedTopicService.DestroyTopicAction();
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
                clz = Class.forName("com.tangosol.coherence/component/util/safeService/safeCacheService/safeDistributedCacheService/SafePagedTopicService$DestroyTopicAction".replace('/', '.'));
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
        * 
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
        public com.tangosol.coherence.component.util.SafeNamedTopic getSafeNamedTopic()
            {
            return __m_SafeNamedTopic;
            }
        
        // Accessor for the property "TopicService"
        /**
         * Getter for property TopicService.<p>
         */
        public com.tangosol.net.TopicService getTopicService()
            {
            return __m_TopicService;
            }
        
        // From interface: java.security.PrivilegedAction
        public Object run()
            {
            // import Component.Util.SafeNamedTopic;
            // import com.tangosol.net.TopicService;
            // import com.tangosol.net.topic.NamedTopic;
            
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
        
        // Accessor for the property "SafeNamedTopic"
        /**
         * Setter for property SafeNamedTopic.<p>
         */
        public void setSafeNamedTopic(com.tangosol.coherence.component.util.SafeNamedTopic topicNamed)
            {
            __m_SafeNamedTopic = topicNamed;
            }
        
        // Accessor for the property "TopicService"
        /**
         * Setter for property TopicService.<p>
         */
        public void setTopicService(com.tangosol.net.TopicService serviceTopic)
            {
            __m_TopicService = serviceTopic;
            }
        }

    // ---- class: com.tangosol.coherence.component.util.safeService.safeCacheService.safeDistributedCacheService.SafePagedTopicService$ReleaseTopicAction
    
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class ReleaseTopicAction
            extends    com.tangosol.coherence.component.Util
            implements java.security.PrivilegedAction
        {
        // ---- Fields declarations ----
        
        /**
         * Property SafeNamedTopic
         *
         */
        private com.tangosol.coherence.component.util.SafeNamedTopic __m_SafeNamedTopic;
        
        /**
         * Property TopicService
         *
         */
        private com.tangosol.net.TopicService __m_TopicService;
        
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
            return new com.tangosol.coherence.component.util.safeService.safeCacheService.safeDistributedCacheService.SafePagedTopicService.ReleaseTopicAction();
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
                clz = Class.forName("com.tangosol.coherence/component/util/safeService/safeCacheService/safeDistributedCacheService/SafePagedTopicService$ReleaseTopicAction".replace('/', '.'));
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
        * 
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
        public com.tangosol.coherence.component.util.SafeNamedTopic getSafeNamedTopic()
            {
            return __m_SafeNamedTopic;
            }
        
        // Accessor for the property "TopicService"
        /**
         * Getter for property TopicService.<p>
         */
        public com.tangosol.net.TopicService getTopicService()
            {
            return __m_TopicService;
            }
        
        // From interface: java.security.PrivilegedAction
        public Object run()
            {
            // import Component.Util.SafeNamedTopic;
            // import com.tangosol.net.TopicService;
            // import com.tangosol.net.topic.NamedTopic;
            
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
        
        // Accessor for the property "SafeNamedTopic"
        /**
         * Setter for property SafeNamedTopic.<p>
         */
        public void setSafeNamedTopic(com.tangosol.coherence.component.util.SafeNamedTopic topicNamed)
            {
            __m_SafeNamedTopic = topicNamed;
            }
        
        // Accessor for the property "TopicService"
        /**
         * Setter for property TopicService.<p>
         */
        public void setTopicService(com.tangosol.net.TopicService serviceTopic)
            {
            __m_TopicService = serviceTopic;
            }
        }

    @Override
    public void addSubscriptionListener(PagedTopicSubscription.Listener listener)
        {
        ((PagedTopicService) getRunningTopicService()).addSubscriptionListener(listener);
        }

    @Override
    public void removeSubscriptionListener(PagedTopicSubscription.Listener listener)
        {
        ((PagedTopicService) getRunningTopicService()).removeSubscriptionListener(listener);
        }
    }
