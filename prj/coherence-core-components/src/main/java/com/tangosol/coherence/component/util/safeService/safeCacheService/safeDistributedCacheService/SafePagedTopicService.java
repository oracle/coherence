
/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.util.safeService.safeCacheService.safeDistributedCacheService.SafePagedTopicService

package com.tangosol.coherence.component.util.safeService.safeCacheService.safeDistributedCacheService;

import com.tangosol.coherence.component.util.SafeNamedTopic;

import com.tangosol.coherence.component.util.safeNamedTopic.SafePagedTopic;

import com.tangosol.coherence.component.util.safeService.SafeTopicService;

import com.tangosol.coherence.component.util.safeService.safeCacheService.SafeDistributedCacheService;

import com.tangosol.internal.net.topic.impl.paged.PagedTopicBackingMapManager;
import com.tangosol.internal.net.topic.impl.paged.PagedTopicCaches;

import com.tangosol.internal.net.topic.impl.paged.model.PagedTopicSubscription;

import com.tangosol.internal.net.topic.impl.paged.model.SubscriberGroupId;
import com.tangosol.net.PagedTopicService;
import com.tangosol.net.Service;

import com.tangosol.net.internal.ScopedTopicReferenceStore;

import com.tangosol.net.topic.Subscriber;
import com.tangosol.net.topic.TopicBackingMapManager;

import com.tangosol.util.Filter;
import com.tangosol.util.ListMap;
import com.tangosol.util.ValueExtractor;

import java.util.Map;

/**
 * A safe wrapper around a paged topic service.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class SafePagedTopicService
        extends SafeDistributedCacheService
        implements PagedTopicService, SafeTopicService
    {
    /**
     * Property ScopedTopicStore
     *
     */
    private ScopedTopicReferenceStore __m_ScopedTopicStore;

    private static ListMap<String, Class<?>> __mapChildren;
    
    static
        {
        __initStatic();
        }
    
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
    
    public SafePagedTopicService()
        {
        this(null, null, true);
        }
    
    public SafePagedTopicService(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
    
    @Override
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
    
    /**
     * Getter for property _Instance.<p>
    * Auto generated
     */
    public static com.tangosol.coherence.Component get_Instance()
        {
        return new SafePagedTopicService();
        }
    
    /**
     * This is an auto-generated method that returns the map of design time
     * [static] children.
     * <p>
     * Note: the class generator will ignore any custom implementation for this
     * behavior.
     */
    @Override
    protected Map<String, Class<?>> get_ChildClasses()
        {
        return __mapChildren;
        }

    @Override
    public PagedTopicService getRunningTopicService()
        {
        return (PagedTopicService) super.getRunningService();
        }

    @Override
    public void destroySubscription(long lSubscriptionId)
        {
        getRunningTopicService().destroySubscription(lSubscriptionId);
        }

    @Override
    public void destroySubscription(long Param_1, com.tangosol.net.topic.Subscriber.Id Param_2)
        {
        getRunningTopicService().destroySubscription(Param_1, Param_2);
        }

    @Override
    public int ensureChannelCount(String Param_1, int Param_2)
        {
        return getRunningTopicService().ensureChannelCount(Param_1, Param_2);
        }

    @Override
    public void ensureSubscription(String Param_1, long Param_2, com.tangosol.net.topic.Subscriber.Id Param_3)
        {
        getRunningTopicService().ensureSubscription(Param_1, Param_2, Param_3);
        }

    @Override
    public void ensureSubscription(String Param_1, long Param_2, com.tangosol.net.topic.Subscriber.Id Param_3, boolean Param_4)
        {
        getRunningTopicService().ensureSubscription(Param_1, Param_2, Param_3, Param_4);
        }

    @Override
    public void ensureSubscription(String Param_1, long Param_2, com.tangosol.net.topic.Subscriber.Id Param_3, boolean Param_4, int[] anChannel)
        {
        getRunningTopicService().ensureSubscription(Param_1, Param_2, Param_3, Param_4, anChannel);
        }

    @Override
    public long ensureSubscription(String Param_1, SubscriberGroupId Param_2, Subscriber.Id Param_3, Filter Param_4, ValueExtractor Param_5)
        {
        return getRunningTopicService().ensureSubscription(Param_1, Param_2, Param_3, Param_4, Param_5, null);
        }

    @Override
    public long ensureSubscription(String Param_1, SubscriberGroupId Param_2, Subscriber.Id Param_3, Filter Param_4, ValueExtractor Param_5, int[] anChannel)
        {
        return getRunningTopicService().ensureSubscription(Param_1, Param_2, Param_3, Param_4, Param_5, anChannel);
        }

    @Override
    public void checkClientThread(String sName)
        {
        super.checkClientThread(sName);
        }

    @Override
    public com.tangosol.util.Service getRunningService()
        {
        return super.getRunningService();
        }

    @Override
    public Service getInternalService()
        {
        return super.getInternalService();
        }

    @Override
    public SafeNamedTopic createSafeTopic(String sName)
        {
        SafePagedTopic topicSafe = new SafePagedTopic<>();
        topicSafe.setPagedTopicCaches(new PagedTopicCaches(sName, this, true));
        return topicSafe;
        }

    @Override
    public java.util.Set getSubscribers(String Param_1, com.tangosol.internal.net.topic.impl.paged.model.SubscriberGroupId Param_2)
        {
        return getRunningTopicService().getSubscribers(Param_1, Param_2);
        }

    @Override
    public boolean hasSubscribers(String sTopicName)
        {
        return getRunningTopicService().hasSubscribers(sTopicName);
        }

    @Override
    public long getSubscriptionCount(String sTopicName)
        {
        return getRunningTopicService().getSubscriptionCount(sTopicName);
        }

    @Override
    public com.tangosol.internal.net.topic.impl.paged.model.PagedTopicSubscription getSubscription(long Param_1)
        {
        return getRunningTopicService().getSubscription(Param_1);
        }

    @Override
    public long getSubscriptionId(String Param_1, com.tangosol.internal.net.topic.impl.paged.model.SubscriberGroupId Param_2)
        {
        return getRunningTopicService().getSubscriptionId(Param_1, Param_2);
        }

    @Override
    public com.tangosol.internal.net.topic.impl.paged.statistics.PagedTopicStatistics getTopicStatistics(String Param_1)
        {
        return getRunningTopicService().getTopicStatistics(Param_1);
        }

    @Override
    public boolean hasSubscription(long Param_1)
        {
        return getRunningTopicService().hasSubscription(Param_1);
        }

    @Override
    public boolean isSubscriptionDestroyed(long Param_1)
        {
        return getRunningTopicService().isSubscriptionDestroyed(Param_1);
        }

    /**
     * Getter for property ScopedTopicStore.<p>
     */
    @Override
    public ScopedTopicReferenceStore getScopedTopicStore()
        {
        return __m_ScopedTopicStore;
        }
    
    @Override
    public PagedTopicBackingMapManager getTopicBackingMapManager()
        {
        return (PagedTopicBackingMapManager) getBackingMapManager();
        }
    
    /**
     * Setter for property ScopedTopicStore.<p>
     */
    private void setScopedTopicStore(com.tangosol.net.internal.ScopedTopicReferenceStore storeTopic)
        {
        __m_ScopedTopicStore = storeTopic;
        }

    @Override
    public void addSubscriptionListener(PagedTopicSubscription.Listener listener)
        {
        getRunningTopicService().addSubscriptionListener(listener);
        }

    @Override
    public void removeSubscriptionListener(PagedTopicSubscription.Listener listener)
        {
        getRunningTopicService().removeSubscriptionListener(listener);
        }

    @Override
    public void setTopicBackingMapManager(TopicBackingMapManager manager)
        {
        }

    @Override
    public int getCurrentClusterTopicsApiVersion()
        {
        return getRunningTopicService().getCurrentClusterTopicsApiVersion();
        }

    @Override
    public int getRemainingMessages(String sTopic, SubscriberGroupId subscriberGroupId, int... anChannel)
        {
        return getRunningTopicService().getRemainingMessages(sTopic, subscriberGroupId, anChannel);
        }
    }
