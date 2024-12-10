
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.SimpleCache

package com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid;

import com.tangosol.coherence.component.net.Lease;
import com.tangosol.net.BackingMapManager;
import com.tangosol.net.cache.CacheMap;
import com.tangosol.util.Binary;
import com.tangosol.util.Converter;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.Filter;
import com.tangosol.util.InvocableMapHelper;
import com.tangosol.util.MapEvent;
import com.tangosol.util.NullImplementation;
import com.tangosol.util.SimpleEnumerator;
import com.tangosol.util.WrapperException;
import java.io.Serializable;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * The SimpleCache component represents a clustered service that provides means
 * for handling a collection of resources replicated across a cluster. The
 * replication srategy is overly optimistic and doesn't have any concurrency
 * control. The Map representring  the ResourceMap property is assumed to be
 * thread safe.
 * This service is mostly used as a primary point of reference for the
 * performanece and throughput measurment.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public final class SimpleCache
        extends    com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid
        implements com.tangosol.net.CacheService,
                   com.tangosol.net.NamedCache
    {
    // ---- Fields declarations ----
    
    /**
     * Property BackingMapManager
     *
     * The BackingMapManager.
     */
    private transient com.tangosol.net.BackingMapManager __m_BackingMapManager;
    
    /**
     * Property ConverterFromInternal
     *
     */
    private transient com.tangosol.util.Converter __m_ConverterFromInternal;
    
    /**
     * Property ConverterToInternal
     *
     */
    private transient com.tangosol.util.Converter __m_ConverterToInternal;
    
    /**
     * Property ListenerSupport
     *
     * Registered listeners to the MapEvent notifications.
     */
    private transient com.tangosol.util.MapListenerSupport __m_ListenerSupport;
    
    /**
     * Property ResourceMap
     *
     * This (protected) Map is the model for ReplicatedCache service.
     */
    private transient java.util.Map __m_ResourceMap;
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
        __mapChildren.put("Acknowledgement", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.Acknowledgement.get_CLASS());
        __mapChildren.put("BackingMapManagerContext", SimpleCache.BackingMapManagerContext.get_CLASS());
        __mapChildren.put("BusEventMessage", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.BusEventMessage.get_CLASS());
        __mapChildren.put("ConfigRequest", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.ConfigRequest.get_CLASS());
        __mapChildren.put("ConfigResponse", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.ConfigResponse.get_CLASS());
        __mapChildren.put("ConfigSync", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.ConfigSync.get_CLASS());
        __mapChildren.put("ConfigUpdate", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.ConfigUpdate.get_CLASS());
        __mapChildren.put("ConverterFromInternal", SimpleCache.ConverterFromInternal.get_CLASS());
        __mapChildren.put("ConverterToInternal", SimpleCache.ConverterToInternal.get_CLASS());
        __mapChildren.put("DispatchEvent", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.DispatchEvent.get_CLASS());
        __mapChildren.put("DispatchNotification", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.DispatchNotification.get_CLASS());
        __mapChildren.put("MemberConfigUpdate", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.MemberConfigUpdate.get_CLASS());
        __mapChildren.put("MemberJoined", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.MemberJoined.get_CLASS());
        __mapChildren.put("MemberWelcome", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.MemberWelcome.get_CLASS());
        __mapChildren.put("MemberWelcomeRequest", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.MemberWelcomeRequest.get_CLASS());
        __mapChildren.put("MemberWelcomeRequestTask", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.MemberWelcomeRequestTask.get_CLASS());
        __mapChildren.put("NotifyConnectionClose", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyConnectionClose.get_CLASS());
        __mapChildren.put("NotifyConnectionOpen", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyConnectionOpen.get_CLASS());
        __mapChildren.put("NotifyMemberJoined", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyMemberJoined.get_CLASS());
        __mapChildren.put("NotifyMemberLeaving", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyMemberLeaving.get_CLASS());
        __mapChildren.put("NotifyMemberLeft", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyMemberLeft.get_CLASS());
        __mapChildren.put("NotifyMessageReceipt", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyMessageReceipt.get_CLASS());
        __mapChildren.put("NotifyPollClosed", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyPollClosed.get_CLASS());
        __mapChildren.put("NotifyResponse", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyResponse.get_CLASS());
        __mapChildren.put("NotifyServiceAnnounced", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyServiceAnnounced.get_CLASS());
        __mapChildren.put("NotifyServiceJoining", SimpleCache.NotifyServiceJoining.get_CLASS());
        __mapChildren.put("NotifyServiceLeaving", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyServiceLeaving.get_CLASS());
        __mapChildren.put("NotifyServiceLeft", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyServiceLeft.get_CLASS());
        __mapChildren.put("NotifyServiceQuiescence", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyServiceQuiescence.get_CLASS());
        __mapChildren.put("NotifyShutdown", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyShutdown.get_CLASS());
        __mapChildren.put("NotifyStartup", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyStartup.get_CLASS());
        __mapChildren.put("PingRequest", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.PingRequest.get_CLASS());
        __mapChildren.put("ProtocolContext", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.ProtocolContext.get_CLASS());
        __mapChildren.put("ResourceRemove", SimpleCache.ResourceRemove.get_CLASS());
        __mapChildren.put("ResourceRemoveAll", SimpleCache.ResourceRemoveAll.get_CLASS());
        __mapChildren.put("ResourceUpdate", SimpleCache.ResourceUpdate.get_CLASS());
        __mapChildren.put("Response", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.Response.get_CLASS());
        __mapChildren.put("WrapperGuardable", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.WrapperGuardable.get_CLASS());
        }
    
    // Default constructor
    public SimpleCache()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public SimpleCache(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            setDaemonState(0);
            setDefaultGuardRecovery(0.9F);
            setDefaultGuardTimeout(60000L);
            setMessageClassMap(new java.util.HashMap());
            setNotifier(new com.oracle.coherence.common.base.SingleWaiterMultiNotifier());
            setOldestPendingRequestSUIDCounter(new java.util.concurrent.atomic.AtomicLong());
            setResourceMap(new com.tangosol.util.SafeHashMap());
            setResourceRegistry(new com.tangosol.util.SimpleResourceRegistry());
            setSerializerMap(new java.util.WeakHashMap());
            setSuspendPollLimit(new java.util.concurrent.atomic.AtomicLong());
            }
        catch (java.lang.Exception e)
            {
            // re-throw as a runtime exception
            throw new com.tangosol.util.WrapperException(e);
            }
        
        // containment initialization: children
        _addChild(new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.DaemonPool("DaemonPool", this, true), "DaemonPool");
        _addChild(new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.EventDispatcher("EventDispatcher", this, true), "EventDispatcher");
        _addChild(new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.Guard("Guard", this, true), "Guard");
        _addChild(new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.MemberConfigListener("MemberConfigListener", this, true), "MemberConfigListener");
        _addChild(new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.PollArray("PollArray", this, true), "PollArray");
        _addChild(new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.ReceiveQueue("ReceiveQueue", this, true), "ReceiveQueue");
        _addChild(new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.ServiceConfig("ServiceConfig", this, true), "ServiceConfig");
        
        // signal the end of the initialization
        set_Constructed(true);
        }
    
    // Private initializer
    protected void __initPrivate()
        {
        
        super.__initPrivate();
        }
    
    // Getter for virtual constant ServiceType
    public String getServiceType()
        {
        return "SimpleCache";
        }
    
    //++ getter for static property _Instance
    /**
     * Getter for property _Instance.<p>
    * Auto generated
     */
    public static com.tangosol.coherence.Component get_Instance()
        {
        return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.SimpleCache();
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
            clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/SimpleCache".replace('/', '.'));
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
    
    // From interface: com.tangosol.net.NamedCache
    public void addIndex(com.tangosol.util.ValueExtractor extractor, boolean fOrdered, java.util.Comparator comparator)
        {
        throw new UnsupportedOperationException();
        }
    
    // From interface: com.tangosol.net.NamedCache
    public void addMapListener(com.tangosol.util.MapListener listener)
        {
        // import com.tangosol.util.Filter;
        
        addMapListener(listener, (Filter) null, false);
        }
    
    // From interface: com.tangosol.net.NamedCache
    public synchronized void addMapListener(com.tangosol.util.MapListener listener, com.tangosol.util.Filter filter, boolean fLite)
        {
        // import com.tangosol.util.MapListenerSupport as com.tangosol.util.MapListenerSupport;
        
        _assert(listener != null);
        
        com.tangosol.util.MapListenerSupport support = getListenerSupport();
        if (support == null)
            {
            setListenerSupport(support = new com.tangosol.util.MapListenerSupport());
            }
        
        support.addListener(listener, filter, fLite);
        }
    
    // From interface: com.tangosol.net.NamedCache
    public synchronized void addMapListener(com.tangosol.util.MapListener listener, Object oKey, boolean fLite)
        {
        // import com.tangosol.util.MapListenerSupport as com.tangosol.util.MapListenerSupport;
        
        _assert(listener != null);
        
        com.tangosol.util.MapListenerSupport support = getListenerSupport();
        if (support == null)
            {
            setListenerSupport(support = new com.tangosol.util.MapListenerSupport());
            }
        
        support.addListener(listener, oKey, fLite);
        }
    
    // From interface: com.tangosol.net.NamedCache
    public Object aggregate(com.tangosol.util.Filter filter, com.tangosol.util.InvocableMap.EntryAggregator agent)
        {
        return aggregate(keySet(filter), agent);
        }
    
    // From interface: com.tangosol.net.NamedCache
    public Object aggregate(java.util.Collection collKeys, com.tangosol.util.InvocableMap.EntryAggregator agent)
        {
        // import com.tangosol.util.InvocableMapHelper as com.tangosol.util.InvocableMapHelper;
        
        return agent.aggregate(com.tangosol.util.InvocableMapHelper.makeEntrySet(this, collKeys, true));
        }
    
    /**
     * Check the service availability.
     */
    protected void checkStatus()
        {
        if (getServiceState() != SERVICE_STARTED)
            {
            throw new IllegalStateException("Service is not running: " + this);
            }
        
        if (!isActive())
            {
            throw new IllegalStateException("Map has been invalidated:\n" + this);
            }
        }
    
    // From interface: com.tangosol.net.NamedCache
    public void clear()
        {
        checkStatus();
        
        getResourceMap().clear();
        
        SimpleCache.ResourceRemoveAll msg =
            (SimpleCache.ResourceRemoveAll) instantiateMessage("ResourceRemoveAll");
        msg.setToMemberSet(getOthersMemberSet());
        
        send(msg);
        }
    
    // From interface: com.tangosol.net.NamedCache
    public boolean containsKey(Object oKey)
        {
        checkStatus();
        
        return getResourceMap().containsKey(oKey);
        }
    
    // From interface: com.tangosol.net.NamedCache
    public boolean containsValue(Object oValue)
        {
        checkStatus();
        
        return getResourceMap().containsValue(oValue);
        }
    
    // From interface: com.tangosol.net.NamedCache
    public void destroy()
        {
        clear();
        
        try
            {
            Thread.sleep(500);
            }
        catch (InterruptedException e)
            {
            Thread.currentThread().interrupt();
            }
        
        shutdown();
        }
    
    // From interface: com.tangosol.net.CacheService
    public void destroyCache(com.tangosol.net.NamedCache map)
        {
        _assert(map == this);
        
        destroy();
        }
    
    /**
     * Remove the specified resource. This method is called on the service
    * thread and should not be called externally.
    * 
    * @param oKey the resource key
     */
    public void doResourceRemove(Object oKey)
        {
        // import com.tangosol.util.MapEvent;
        // import com.tangosol.util.MapListenerSupport as com.tangosol.util.MapListenerSupport;
        // import java.util.Map;
        
        Map map = getResourceMap();
        
        if (map.containsKey(oKey))
            {
            Object oValueOld = map.remove(oKey);
        
            if (isActive())
                {
                com.tangosol.util.MapListenerSupport support = getListenerSupport();
                if (support != null)
                    {
                    MapEvent event =
                        new MapEvent(this, MapEvent.ENTRY_DELETED, oKey, oValueOld, null);
        
                    // ensure lazy event data conversion
                    event = com.tangosol.util.MapListenerSupport.convertEvent(event, this, null, getConverterFromInternal());
                    support.fireEvent(event, true);
                    }
                }
            }
        }
    
    /**
     * Remove all the resources. This method is called on the service thread and
    * should not be called externally.
    * 
     */
    public void doResourceRemoveAll()
        {
        getResourceMap().clear();
        }
    
    /**
     * Update the specified resource. This method is called on the service
    * thread and should not be called externally.
    * 
    * @param oKey the resource key
    * @param oResource  the resource value (Serializable, XmlElement or
    * XmlSerializable); may be null
    * 
     */
    public void doResourceUpdate(Object oKey, Object oValue)
        {
        // import com.tangosol.util.MapEvent;
        // import com.tangosol.util.MapListenerSupport as com.tangosol.util.MapListenerSupport;
        // import java.util.Map;
        
        Map     map       = getResourceMap();
        boolean fUpdate   = map.containsKey(oKey);
        Object  oValueOld = map.put(oKey, oValue);
        
        if (isActive())
            {
            com.tangosol.util.MapListenerSupport support = getListenerSupport();
            if (support != null)
                {
                MapEvent event = new MapEvent(this,
                    fUpdate ? MapEvent.ENTRY_UPDATED : MapEvent.ENTRY_INSERTED,
                        oKey, oValueOld, oValue);
        
                // ensure lazy event data conversion
                event = com.tangosol.util.MapListenerSupport.convertEvent(event, this, null, getConverterFromInternal());
                support.fireEvent(event, true);
                }
            }
        }
    
    /**
     * Welcome a new member by updating it with the leases issued by this
    * member. This method is called on the service thread and should not be
    * called externally.
    * 
    * @param member the member that has left
     */
    public void doWelcome(com.tangosol.coherence.component.net.Member member)
        {
        // import Component.Net.Lease;
        // import java.util.Enumeration;
        
        if (getThisMember() == getServiceOldestMember())
            {
            // the oldest member updates the newcomer
            for (Enumeration e = getResourceKeys(); e.hasMoreElements();)
                {
                Object oKey = e.nextElement();
        
                SimpleCache.ResourceUpdate msg =
                    (SimpleCache.ResourceUpdate) instantiateMessage("ResourceUpdate");
                msg.addToMember(member);
                msg.setLease(Lease.instantiate(-1, oKey, this));
                msg.setResource(getResourceMap().get(oKey));
        
                send(msg);
                }
            }
        }
    
    // From interface: com.tangosol.net.CacheService
    public synchronized com.tangosol.net.NamedCache ensureCache(String sName, ClassLoader loader)
        {
        // import com.tangosol.net.BackingMapManager as com.tangosol.net.BackingMapManager;
        // import java.util.Map;
        
        if (sName.equals(getServiceName()))
            {
            com.tangosol.net.BackingMapManager manager = getBackingMapManager();
            if (manager != null)
                {
                Map map = manager.instantiateBackingMap(sName);
                if (map == null)
                    {
                    throw new RuntimeException(
                        "BackingMapManager returned \"null\" for map " + sName);
                    }
                map.putAll(getResourceMap());
                setResourceMap(map);
                }
        
            if (loader == null)
                {
                loader = getContextClassLoader();
                if (loader == null)
                    {
                    loader = getClass().getClassLoader();
                    _assert(loader != null, "ClassLoader must be specified");
                    }
                }
        
            if (loader != getContextClassLoader())
                {
                releaseClassLoader();
                setContextClassLoader(loader);
                }
            return this;
            }
        else
            {
            throw new UnsupportedOperationException("Only one cache per service is allowed");
            }
        }
    
    // From interface: com.tangosol.net.NamedCache
    public java.util.Set entrySet()
        {
        // import java.util.Collections;
        
        checkStatus();
        
        return Collections.unmodifiableSet(getResourceMap().entrySet());
        }
    
    // From interface: com.tangosol.net.NamedCache
    public java.util.Set entrySet(com.tangosol.util.Filter filter)
        {
        // import com.tangosol.util.InvocableMapHelper;
        
        return InvocableMapHelper.query(this, filter, true, false, null);
        }
    
    // From interface: com.tangosol.net.NamedCache
    public java.util.Set entrySet(com.tangosol.util.Filter filter, java.util.Comparator comparator)
        {
        // import com.tangosol.util.InvocableMapHelper;
        
        return InvocableMapHelper.query(this, filter, true, true, comparator);
        }
    
    // From interface: com.tangosol.net.NamedCache
    public Object get(Object oKey)
        {
        // import com.tangosol.util.Binary;
        // import com.tangosol.util.WrapperException;
        // import java.util.Map;
        
        checkStatus();
        
        Map    mapResource = getResourceMap();
        Object oResource   = mapResource.get(oKey);
        
        if (oResource instanceof Binary)
            {
            Binary binValue = (Binary) oResource;
            try
                {
                oResource = getConverterFromInternal().convert(binValue);
                }
            catch (WrapperException e)
                {
                throw new WrapperException(e.getOriginalException(),
                    "CacheName=" + getCacheName() + ", Key=" + oKey);
                }
        
            synchronized (mapResource)
                {
                // make sure the resource has not changed since we got it
                if (binValue == mapResource.get(oKey))
                    {
                    mapResource.put(oKey, oResource);
                    }
                }
            }
        
        return oResource;
        }
    
    // From interface: com.tangosol.net.NamedCache
    public java.util.Map getAll(java.util.Collection colKeys)
        {
        // import java.util.HashMap;
        // import java.util.Iterator;
        // import java.util.Map;
        
        Map mapResult = new HashMap(colKeys.size()); 
        for (Iterator iter = colKeys.iterator(); iter.hasNext(); )
            {
            Object oKey = iter.next();
            Object oVal = get(oKey);
            if (oVal != null || containsKey(oKey))
                {
                mapResult.put(oKey, oVal);
                }
            }
        return mapResult;
        }
    
    // From interface: com.tangosol.net.CacheService
    // Accessor for the property "BackingMapManager"
    /**
     * Getter for property BackingMapManager.<p>
    * The BackingMapManager.
     */
    public com.tangosol.net.BackingMapManager getBackingMapManager()
        {
        return __m_BackingMapManager;
        }
    
    // From interface: com.tangosol.net.NamedCache
    public String getCacheName()
        {
        return getServiceName();
        }
    
    // From interface: com.tangosol.net.CacheService
    public java.util.Enumeration getCacheNames()
        {
        // import com.tangosol.util.SimpleEnumerator;
        
        return new SimpleEnumerator(new String[] {getServiceName()});
        }
    
    // From interface: com.tangosol.net.NamedCache
    public com.tangosol.net.CacheService getCacheService()
        {
        return this;
        }
    
    // Accessor for the property "ConverterFromInternal"
    /**
     * Getter for property ConverterFromInternal.<p>
     */
    public com.tangosol.util.Converter getConverterFromInternal()
        {
        // import com.tangosol.util.Converter;
        
        Converter conv = __m_ConverterFromInternal;
        if (conv == null)
            {
            synchronized (this)
                {
                conv = __m_ConverterFromInternal;
                if (conv == null)
                    {
                    conv = instantiateConverterFromInternal(getContextClassLoader());
                    setConverterFromInternal(conv);
                    }
                }
            }
        return conv;
        }
    
    // Accessor for the property "ConverterToInternal"
    /**
     * Getter for property ConverterToInternal.<p>
     */
    public com.tangosol.util.Converter getConverterToInternal()
        {
        // import com.tangosol.util.Converter;
        
        Converter conv = __m_ConverterToInternal;
        if (conv == null)
            {
            synchronized (this)
                {
                conv = __m_ConverterToInternal;
                if (conv == null)
                    {
                    conv = instantiateConverterToInternal(getContextClassLoader());
                    setConverterToInternal(conv);
                    }
                }
            }
        return conv;
        }
    
    // Accessor for the property "ListenerSupport"
    /**
     * Getter for property ListenerSupport.<p>
    * Registered listeners to the MapEvent notifications.
     */
    public com.tangosol.util.MapListenerSupport getListenerSupport()
        {
        return __m_ListenerSupport;
        }
    
    // Accessor for the property "ResourceKeys"
    /**
     * Getter for property ResourceKeys.<p>
    * Calculated property returning the Enumeration of resource keys.
     */
    public java.util.Enumeration getResourceKeys()
        {
        // import com.tangosol.util.SimpleEnumerator;
        
        return new SimpleEnumerator(getResourceMap().keySet().toArray());
        }
    
    // Accessor for the property "ResourceMap"
    /**
     * Getter for property ResourceMap.<p>
    * This (protected) Map is the model for ReplicatedCache service.
     */
    public java.util.Map getResourceMap()
        {
        return __m_ResourceMap;
        }
    
    protected com.tangosol.util.Converter instantiateConverterFromInternal(ClassLoader loader)
        {
        SimpleCache.ConverterFromInternal conv =
            (SimpleCache.ConverterFromInternal) _newChild("ConverterFromInternal");
        
        conv.setSerializer(ensureSerializer(loader));
        
        return conv;
        }
    
    protected com.tangosol.util.Converter instantiateConverterToInternal(ClassLoader loader)
        {
        SimpleCache.ConverterToInternal conv = (SimpleCache.ConverterToInternal) _newChild("ConverterToInternal");
        
        conv.setSerializer(ensureSerializer(loader));
        
        return conv;
        }
    
    // From interface: com.tangosol.net.NamedCache
    public Object invoke(Object oKey, com.tangosol.util.InvocableMap.EntryProcessor agent)
        {
        // import com.tangosol.util.InvocableMapHelper as com.tangosol.util.InvocableMapHelper;
        
        return com.tangosol.util.InvocableMapHelper.invokeLocked(this, com.tangosol.util.InvocableMapHelper.makeEntry(this, oKey), agent);
        }
    
    // From interface: com.tangosol.net.NamedCache
    public java.util.Map invokeAll(com.tangosol.util.Filter filter, com.tangosol.util.InvocableMap.EntryProcessor agent)
        {
        return invokeAll(keySet(filter), agent);
        }
    
    // From interface: com.tangosol.net.NamedCache
    public java.util.Map invokeAll(java.util.Collection collKeys, com.tangosol.util.InvocableMap.EntryProcessor agent)
        {
        // import com.tangosol.util.InvocableMapHelper as com.tangosol.util.InvocableMapHelper;
        
        return com.tangosol.util.InvocableMapHelper.invokeAllLocked(this, com.tangosol.util.InvocableMapHelper.makeEntrySet(this, collKeys, false), agent);
        }
    
    // From interface: com.tangosol.net.NamedCache
    // Accessor for the property "Active"
    /**
     * Getter for property Active.<p>
    * Calculated property specifiying whether or not this cache is currently
    * active (has a valid ClassLoader)
     */
    public boolean isActive()
        {
        return getContextClassLoader() != null;
        }
    
    // From interface: com.tangosol.net.NamedCache
    public boolean isEmpty()
        {
        checkStatus();
        
        return getResourceMap().isEmpty();
        }
    
    // From interface: com.tangosol.net.NamedCache
    public java.util.Set keySet()
        {
        // import java.util.Collections;
        
        checkStatus();
        
        return Collections.unmodifiableSet(getResourceMap().keySet());
        }
    
    // From interface: com.tangosol.net.NamedCache
    public java.util.Set keySet(com.tangosol.util.Filter filter)
        {
        // import com.tangosol.util.InvocableMapHelper;
        
        return InvocableMapHelper.query(this, filter, false, false, null);
        }
    
    // From interface: com.tangosol.net.NamedCache
    public boolean lock(Object oKey)
        {
        return lock(oKey, 0);
        }
    
    // From interface: com.tangosol.net.NamedCache
    public boolean lock(Object oKey, long cWait)
        {
        throw new UnsupportedOperationException();
        }
    
    // Declared at the super level
    /**
     * The default implementation of this method sets AcceptingClients to true.
    * If the Service has not completed preparing at this point, then the
    * Service must override this implementation and only set AcceptingClients
    * to true when the Service has actually "finished starting".
     */
    public void onServiceStarted()
        {
        // import com.tangosol.net.BackingMapManager;
        
        // per BackingMapManager contract: call init()
        BackingMapManager manager = getBackingMapManager();
        if (manager != null)
            {
            SimpleCache.BackingMapManagerContext ctx =
                (SimpleCache.BackingMapManagerContext) _newChild("BackingMapManagerContext");
            ctx.setManager(manager);
        
            manager.init(ctx);
            }
        
        super.onServiceStarted();
        }
    
    // From interface: com.tangosol.net.NamedCache
    public Object put(Object oKey, Object oValue)
        {
        return put(oKey, oValue, 0L);
        }
    
    // From interface: com.tangosol.net.NamedCache
    public Object put(Object oKey, Object oValue, long cMillis)
        {
        // import Component.Net.Lease;
        // import com.tangosol.net.cache.CacheMap;
        // import java.io.Serializable;
        // import java.util.Map;
        
        checkStatus();
        
        if (oValue != null && !(oValue instanceof Serializable))
            {
            throw new IllegalArgumentException("Resource is not serializable: " +
                "Key=" + oKey + ", Value=" + oValue);
            }
        
        Object oValueOld = get(oKey);
        
        Map map = getResourceMap();
        
        if (map instanceof CacheMap)
            {
            ((CacheMap) map).put(oKey, oValue, cMillis);
            }
        else if (cMillis <= 0)
            {
            map.put(oKey, oValue);
            }
        else
            {
            throw new UnsupportedOperationException(
                "Class \"" + map.getClass().getName() +
                "\" does not implement CacheMap interface");
            }
        
        SimpleCache.ResourceUpdate msg =
            (SimpleCache.ResourceUpdate) instantiateMessage("ResourceUpdate");
        msg.setToMemberSet(getOthersMemberSet());
        msg.setLease(Lease.instantiate(-1, oKey, this));
        msg.setResource(oValue);
        
        send(msg);
        
        return oValueOld;
        }
    
    // From interface: com.tangosol.net.NamedCache
    public void putAll(java.util.Map map)
        {
        // import Component.Util.Collections as com.tangosol.coherence.component.util.Collections;
        
        com.tangosol.coherence.component.util.Collections.putAll(this, map);
        }
    
    // From interface: com.tangosol.net.NamedCache
    public void release()
        {
        shutdown();
        }
    
    // From interface: com.tangosol.net.CacheService
    public void releaseCache(com.tangosol.net.NamedCache map)
        {
        _assert(map == this);
        
        release();
        }
    
    protected void releaseClassLoader()
        {
        // import com.tangosol.util.WrapperException;
        // import java.util.Iterator;
        // import java.util.Map;
        
        // serialize all the relevant resources to release the class loader
        ClassLoader loader = getContextClassLoader();
        if (loader != null)
            {
            // deactivate before proceeding
            setContextClassLoader(null);
        
            Map map = getResourceMap();
        
            for (Iterator iter = keySet().iterator(); iter.hasNext();)
                {
                Object oKey   = iter.next();
                Object oValue = map.get(oKey);
        
                try
                    {
                    Object oInternal = getConverterToInternal().convert(oValue);
                    if (oInternal != oValue)
                        {
                        map.put(oKey, oValue = oInternal);
                        }
                    }
                catch (WrapperException e)
                    {
                    throw new WrapperException(e.getOriginalException(),
                        "CacheName=" + getCacheName() + ", Key=" + oKey);
                    }
                }
            }
        }
    
    // From interface: com.tangosol.net.NamedCache
    public Object remove(Object oKey)
        {
        // import Component.Net.Lease;
        
        checkStatus();
        
        Object oValue = get(oKey);
        
        getResourceMap().remove(oKey);
        
        SimpleCache.ResourceRemove msg =
            (SimpleCache.ResourceRemove) instantiateMessage("ResourceRemove");
        msg.setToMemberSet(getOthersMemberSet());
        msg.setLease(Lease.instantiate(-1, oKey, this));
        
        send(msg);
        
        return oValue;
        }
    
    // From interface: com.tangosol.net.NamedCache
    public void removeIndex(com.tangosol.util.ValueExtractor extractor)
        {
        throw new UnsupportedOperationException();
        }
    
    // From interface: com.tangosol.net.NamedCache
    public void removeMapListener(com.tangosol.util.MapListener listener)
        {
        // import com.tangosol.util.Filter;
        
        removeMapListener(listener, (Filter) null);
        }
    
    // From interface: com.tangosol.net.NamedCache
    public synchronized void removeMapListener(com.tangosol.util.MapListener listener, com.tangosol.util.Filter filter)
        {
        // import com.tangosol.util.MapListenerSupport as com.tangosol.util.MapListenerSupport;
        
        _assert(listener != null);
        
        com.tangosol.util.MapListenerSupport support = getListenerSupport();
        if (support != null)
            {
            support.removeListener(listener, filter);
            if (support.isEmpty())
                {
                setListenerSupport(null);
                }
            }
        }
    
    // From interface: com.tangosol.net.NamedCache
    public synchronized void removeMapListener(com.tangosol.util.MapListener listener, Object oKey)
        {
        // import com.tangosol.util.MapListenerSupport as com.tangosol.util.MapListenerSupport;
        
        _assert(listener != null);
        
        com.tangosol.util.MapListenerSupport support = getListenerSupport();
        if (support != null)
            {
            support.removeListener(listener, oKey);
            if (support.isEmpty())
                {
                setListenerSupport(null);
                }
            }
        }
    
    // From interface: com.tangosol.net.CacheService
    // Accessor for the property "BackingMapManager"
    /**
     * Setter for property BackingMapManager.<p>
    * The BackingMapManager.
     */
    public void setBackingMapManager(com.tangosol.net.BackingMapManager manager)
        {
        __m_BackingMapManager = manager;
        }
    
    // Accessor for the property "ConverterFromInternal"
    /**
     * Setter for property ConverterFromInternal.<p>
     */
    protected void setConverterFromInternal(com.tangosol.util.Converter conv)
        {
        __m_ConverterFromInternal = conv;
        }
    
    // Accessor for the property "ConverterToInternal"
    /**
     * Setter for property ConverterToInternal.<p>
     */
    protected void setConverterToInternal(com.tangosol.util.Converter pConverterToInternal)
        {
        __m_ConverterToInternal = pConverterToInternal;
        }
    
    // Accessor for the property "ListenerSupport"
    /**
     * Setter for property ListenerSupport.<p>
    * Registered listeners to the MapEvent notifications.
     */
    protected void setListenerSupport(com.tangosol.util.MapListenerSupport support)
        {
        __m_ListenerSupport = support;
        }
    
    // Accessor for the property "ResourceMap"
    /**
     * Setter for property ResourceMap.<p>
    * This (protected) Map is the model for ReplicatedCache service.
     */
    protected void setResourceMap(java.util.Map map)
        {
        __m_ResourceMap = map;
        }
    
    // From interface: com.tangosol.net.NamedCache
    public int size()
        {
        checkStatus();
        
        return getResourceMap().size();
        }
    
    // From interface: com.tangosol.net.CacheService
    // Declared at the super level
    /**
     * Hard-stop the Service. Use shutdown() for normal  termination.
     */
    public void stop()
        {
        // import java.util.Collections;
        
        super.stop();
        
        setContextClassLoader(null);
        setResourceMap(Collections.EMPTY_MAP);
        }
    
    // From interface: com.tangosol.net.NamedCache
    public boolean unlock(Object oKey)
        {
        throw new UnsupportedOperationException();
        }
    
    // From interface: com.tangosol.net.NamedCache
    public java.util.Collection values()
        {
        // import java.util.Collections;
        
        checkStatus();
        
        return Collections.unmodifiableCollection(getResourceMap().values());
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.SimpleCache$BackingMapManagerContext
    
    /**
     * The BackingMapManagerContext implementation.
     * 
     * Added decoration support methods in Coherence 3.2.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class BackingMapManagerContext
            extends    com.tangosol.coherence.component.util.BackingMapManagerContext
        {
        // ---- Fields declarations ----
        
        // Default constructor
        public BackingMapManagerContext()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public BackingMapManagerContext(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.SimpleCache.BackingMapManagerContext();
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
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/SimpleCache$BackingMapManagerContext".replace('/', '.'));
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
        
        // Declared at the super level
        /**
         * Getter for property ClassLoader.<p>
        * The ClassLoader associated with this context.
         */
        public ClassLoader getClassLoader()
            {
            return ((SimpleCache) get_Module()).getContextClassLoader();
            }
        
        // Declared at the super level
        public com.tangosol.util.Converter getKeyFromInternalConverter()
            {
            // import com.tangosol.util.NullImplementation;
            
            return NullImplementation.getConverter();
            }
        
        // Declared at the super level
        public com.tangosol.util.Converter getKeyToInternalConverter()
            {
            // import com.tangosol.util.NullImplementation;
            
            return NullImplementation.getConverter();
            }
        
        // Declared at the super level
        public com.tangosol.util.Converter getValueFromInternalConverter()
            {
            return ((SimpleCache) get_Module()).getConverterFromInternal();
            }
        
        // Declared at the super level
        public com.tangosol.util.Converter getValueToInternalConverter()
            {
            // import com.tangosol.util.NullImplementation;
            
            return NullImplementation.getConverter();
            }
        
        // Declared at the super level
        /**
         * Setter for property ClassLoader.<p>
        * The ClassLoader associated with this context.
         */
        public void setClassLoader(ClassLoader loader)
            {
            ((SimpleCache) get_Module()).setContextClassLoader(loader);
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.SimpleCache$ConverterFromInternal
    
    /**
     * A converter that takes service specific "transmittable" serializable
     * objects and converts them via deserialization (etc.) to the objects
     * expected by clients of the cache.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class ConverterFromInternal
            extends    com.tangosol.coherence.component.util.Converter
        {
        // ---- Fields declarations ----
        
        // Default constructor
        public ConverterFromInternal()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public ConverterFromInternal(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.SimpleCache.ConverterFromInternal();
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
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/SimpleCache$ConverterFromInternal".replace('/', '.'));
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
        
        // Declared at the super level
        public Object convert(Object o)
            {
            // import com.tangosol.util.Binary;
            // import com.tangosol.util.ExternalizableHelper;
            
            if (o instanceof Binary)
                {
                o = ExternalizableHelper.fromBinary((Binary) o, getSerializer());
                }
            
            return o;
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.SimpleCache$ConverterToInternal
    
    /**
     * A converter that takes ClassLoader dependent objects and converts them
     * via serialization into Binary objects.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class ConverterToInternal
            extends    com.tangosol.coherence.component.util.Converter
        {
        // ---- Fields declarations ----
        
        // Default constructor
        public ConverterToInternal()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public ConverterToInternal(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.SimpleCache.ConverterToInternal();
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
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/SimpleCache$ConverterToInternal".replace('/', '.'));
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
        
        // Declared at the super level
        public Object convert(Object o)
            {
            // import com.tangosol.util.ExternalizableHelper;
            
            return ExternalizableHelper.toBinary(o, getSerializer());
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.SimpleCache$NotifyServiceJoining
    
    /**
     * This internal Message is sent to a service when a cluster Member that
     * previously did not expose the same service has started the same service.
     * In other words, if cluster Members are (A, B, C), and (A, B) have a
     * service #3, and C subsequently registers a service #3, then the service
     * #3 on (A, B) will be notified that C has started service #3.
     * 
     * As of Coherence 3.7.1, this notification is a poll that is sent by the
     * ClustersService BEFORE the specified member is added to the
     * correspondning ServiceMemberSet. The service join protocol will be
     * blocked until this service closes the underlying poll.
     * 
     * Subsequent $NotifyServiceJoined notification will be sent when the
     * member has finished starting the service.
     * 
     * Attributes:
     *     NotifyMember
     *     NotifyMemberConfigMap
     *     NotifyServiceEndPointName (@since Coherence 3.7.1)
     *     NotifyServiceJoinTime
     *     NotifyServiceVersion
     *     ContinuationMessage (@since Coherence 3.7.1)
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class NotifyServiceJoining
            extends    com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyServiceJoining
        {
        // ---- Fields declarations ----
        
        // Default constructor
        public NotifyServiceJoining()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public NotifyServiceJoining(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                setMessageType(-10);
                }
            catch (java.lang.Exception e)
                {
                // re-throw as a runtime exception
                throw new com.tangosol.util.WrapperException(e);
                }
            
            // containment initialization: children
            _addChild(new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyServiceJoining.Poll("Poll", this, true), "Poll");
            
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.SimpleCache.NotifyServiceJoining();
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
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/SimpleCache$NotifyServiceJoining".replace('/', '.'));
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
        
        // Declared at the super level
        /**
         * This is the event that is executed when a Message is received.
        * <p>
        * It is the main processing event of the Message called by the
        * <code>Service.onMessage()</code> event. With regards to the use of
        * Message components within clustered Services, Services are designed
        * by dragging Message components into them as static children. These
        * Messages are the components that a Service can send to other running
        * instances of the same Service within a cluster. When the onReceived
        * event is invoked by a Service, it means that the Message has been
        * received; the code in the onReceived event is therefore the Message
        * specific logic for processing a received Message. For example, when
        * onReceived is invoked on a Message named FindData, the onReceived
        * event should do the work to "find the data", because it is being
        * invoked by the Service that received the "find the data" Message.
         */
        public void onReceived()
            {
            super.onReceived();
            
            SimpleCache service = (SimpleCache) getService();
            
            service.doWelcome(getNotifyMember());
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.SimpleCache$ResourceRemove
    
    /**
     * Message:
     *    ResourceRemove
     * 
     * Purpose:
     *     This Message informs all Members of the service that the specified
     * resource should be removed.
     * 
     * Description:
     *     In response to this mesage all Members should remove the specified
     * resource.
     * 
     * Attributes:
     *     ResourceKey
     * 
     * Response to:
     *     n/a
     * Expected responses:
     *     n/a
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class ResourceRemove
            extends    com.tangosol.coherence.component.net.message.LeaseMessage
        {
        // ---- Fields declarations ----
        
        // Default constructor
        public ResourceRemove()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public ResourceRemove(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                setMessageType(2);
                }
            catch (java.lang.Exception e)
                {
                // re-throw as a runtime exception
                throw new com.tangosol.util.WrapperException(e);
                }
            
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.SimpleCache.ResourceRemove();
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
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/SimpleCache$ResourceRemove".replace('/', '.'));
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
        
        // Declared at the super level
        /**
         * This is the event that is executed when a Message is received.
        * <p>
        * It is the main processing event of the Message called by the
        * <code>Service.onMessage()</code> event. With regards to the use of
        * Message components within clustered Services, Services are designed
        * by dragging Message components into them as static children. These
        * Messages are the components that a Service can send to other running
        * instances of the same Service within a cluster. When the onReceived
        * event is invoked by a Service, it means that the Message has been
        * received; the code in the onReceived event is therefore the Message
        * specific logic for processing a received Message. For example, when
        * onReceived is invoked on a Message named FindData, the onReceived
        * event should do the work to "find the data", because it is being
        * invoked by the Service that received the "find the data" Message.
         */
        public void onReceived()
            {
            super.onReceived();
            
            SimpleCache service = (SimpleCache) getService();
            
            service.doResourceRemove(getLease().getResourceKey());
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.SimpleCache$ResourceRemoveAll
    
    /**
     * Message:
     *    ResourceRemoveAll
     * 
     * Purpose:
     *     This Message informs all Members of the service that the resource
     * map should be cleared.
     * 
     * Description:
     *     In response to this mesage all Members should clear their resource
     * maps and (optionally) shutdown the service.
     * 
     * Attributes:
     *     Shutdown
     * 
     * Response to:
     *     n/a
     * Expected responses:
     *     n/a
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class ResourceRemoveAll
            extends    com.tangosol.coherence.component.net.Message
        {
        // ---- Fields declarations ----
        
        /**
         * Property Shutdown
         *
         */
        private boolean __m_Shutdown;
        
        // Default constructor
        public ResourceRemoveAll()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public ResourceRemoveAll(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                setMessageType(3);
                }
            catch (java.lang.Exception e)
                {
                // re-throw as a runtime exception
                throw new com.tangosol.util.WrapperException(e);
                }
            
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.SimpleCache.ResourceRemoveAll();
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
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/SimpleCache$ResourceRemoveAll".replace('/', '.'));
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
        
        // Accessor for the property "Shutdown"
        /**
         * Getter for property Shutdown.<p>
         */
        public boolean isShutdown()
            {
            return __m_Shutdown;
            }
        
        // Declared at the super level
        /**
         * This is the event that is executed when a Message is received.
        * <p>
        * It is the main processing event of the Message called by the
        * <code>Service.onMessage()</code> event. With regards to the use of
        * Message components within clustered Services, Services are designed
        * by dragging Message components into them as static children. These
        * Messages are the components that a Service can send to other running
        * instances of the same Service within a cluster. When the onReceived
        * event is invoked by a Service, it means that the Message has been
        * received; the code in the onReceived event is therefore the Message
        * specific logic for processing a received Message. For example, when
        * onReceived is invoked on a Message named FindData, the onReceived
        * event should do the work to "find the data", because it is being
        * invoked by the Service that received the "find the data" Message.
         */
        public void onReceived()
            {
            super.onReceived();
            
            SimpleCache service = (SimpleCache) getService();
            
            if (isShutdown())
                {
                service.shutdown();
                }
            else
                {
                service.doResourceRemoveAll();
                }
            }
        
        // Declared at the super level
        public void read(com.tangosol.io.ReadBuffer.BufferInput input)
                throws java.io.IOException
            {
            super.read(input);
            
            setShutdown(input.readBoolean());
            }
        
        // Accessor for the property "Shutdown"
        /**
         * Setter for property Shutdown.<p>
         */
        public void setShutdown(boolean pShutdown)
            {
            __m_Shutdown = pShutdown;
            }
        
        // Declared at the super level
        public void write(com.tangosol.io.WriteBuffer.BufferOutput output)
                throws java.io.IOException
            {
            super.write(output);
            
            output.writeBoolean(isShutdown());
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.SimpleCache$ResourceUpdate
    
    /**
     * Message:
     *    ResourceUpdate
     * 
     * Purpose:
     *     This Message informs all Members of the service that the specified
     * resource should be updated.
     * 
     * Description:
     *     In response to this mesage all Members should update their maps to
     * reflect the new value of the resource.
     * 
     * Attributes:
     *     ResourceKey
     *     ResourceValue
     * 
     * Response to:
     *     n/a
     * 
     * Expected responses:
     *     n/a
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class ResourceUpdate
            extends    com.tangosol.coherence.component.net.message.leaseMessage.ResourceMessage
        {
        // ---- Fields declarations ----
        
        // Default constructor
        public ResourceUpdate()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public ResourceUpdate(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                setMessageType(1);
                }
            catch (java.lang.Exception e)
                {
                // re-throw as a runtime exception
                throw new com.tangosol.util.WrapperException(e);
                }
            
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.SimpleCache.ResourceUpdate();
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
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/SimpleCache$ResourceUpdate".replace('/', '.'));
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
        
        // Declared at the super level
        /**
         * This is the event that is executed when a Message is received.
        * <p>
        * It is the main processing event of the Message called by the
        * <code>Service.onMessage()</code> event. With regards to the use of
        * Message components within clustered Services, Services are designed
        * by dragging Message components into them as static children. These
        * Messages are the components that a Service can send to other running
        * instances of the same Service within a cluster. When the onReceived
        * event is invoked by a Service, it means that the Message has been
        * received; the code in the onReceived event is therefore the Message
        * specific logic for processing a received Message. For example, when
        * onReceived is invoked on a Message named FindData, the onReceived
        * event should do the work to "find the data", because it is being
        * invoked by the Service that received the "find the data" Message.
         */
        public void onReceived()
            {
            super.onReceived();
            
            SimpleCache service = (SimpleCache) getService();
            
            service.doResourceUpdate(getLease().getResourceKey(), getResource());
            }
        }
    }
