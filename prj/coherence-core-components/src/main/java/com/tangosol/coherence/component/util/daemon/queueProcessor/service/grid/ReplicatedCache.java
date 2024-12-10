
/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ReplicatedCache

package com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid;

import com.tangosol.coherence.component.net.Lease;
import com.tangosol.coherence.component.net.Member;
import com.tangosol.coherence.component.net.MemberSet;
import com.tangosol.coherence.component.net.memberSet.DependentMemberSet;
import com.tangosol.coherence.component.net.memberSet.EmptyMemberSet;
import com.tangosol.coherence.component.net.memberSet.SingleMemberSet;
import com.tangosol.coherence.component.net.memberSet.actualMemberSet.ServiceMemberSet;
import com.tangosol.internal.net.service.grid.DefaultReplicatedCacheDependencies;
import com.tangosol.internal.net.service.grid.ReplicatedCacheDependencies;
import com.tangosol.net.BackingMapManager;
import com.tangosol.run.component.EventDeathException;
import com.tangosol.util.Base;
import com.tangosol.util.Binary;
import com.tangosol.util.ConcurrentMap;
import com.tangosol.util.Converter;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.FilterEnumerator;
import com.tangosol.util.NullFilter;
import com.tangosol.util.SafeHashMap;
import com.tangosol.util.WrapperException;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

/**
 * The ReplicatedCache component represents a clustered service that provides
 * means for handling a collection of resources replicated across a cluster
 * with concurrent access control based on the lease model.
 * 
 * Each member of the cluster running the ReplicatedCache service holds a
 * collection of resource maps that are replicated by all members of the
 * cluster. Each resource map is handled by a CacheHandler child component. The
 * resources stored in these maps are either Serializable or "Xmlizable"
 * objects. In order for a member of the cluster to get a resource from the
 * cache with a purpose to update or put a new resource into the cache it must
 * first obtain a lease for that resource.
 * There is also a way to get a "dirty" resource without getting the lease.
 * 
 * Leases are known to be owned by members and have the effective and
 * expiration timestamps measured in the cluster time.
 * Each cached resource could have one of the following lease status respective
 * to the member holding this lease:
 * 
 * * Unknown (the resource is not known to exist)
 * * Available (no one is known to hold a lease for the existing resource)
 * * Locked (this member is a lessee of the resource)
 * * Dirty (someone else holds a lease for the resource)
 * 
 * A usual flow of events is:
 * 
 * * Member M1 is asked to get a lease for resource R.
 *   1. M1 checks the state of R - it is "Unknown";
 *   2. M1 ensures the resource R - it becomes "Available";
 *   3. M1 creates a lease for R;
 *   4. M1 announces that there is a lease for R held by M1 to all service
 * members;
 *   5. At this point M1 is the holder of R. The state of resource R is
 * "Locked" on M1 and "Dirty" everywhere else.
 * 
 * * Member M2 is asked to get a lease for R for period T.
 *   1. M2 checks the state of R - it is "Dirty";
 *   2. M2 rejects the request.
 * 
 * * Member M1 is asked to update and release resource R.
 *   1. M1 updates and releases R.
 *   2. M1 announces the release of R attaching the updated R;
 *   3. At this point resource R becomes "Available" everywhere.
 * 
 * * Member M2 is asked to get a lease for R for period T.
 *   1. M2 checks the state of R - it is "Available";
 *   2. M2 creates a lease for R; 
 *   3. M2 announces that there is a lease for R held by M2 to all service
 * members;
 *   4. At this point M2 is the holder of R. The state of resource R is
 * "Locked" on M2 and "Dirty" everywhere else.
 * 
 * * Member M2 is asked to update R.
 *   1. M2 verifies that there is a current lease for R;
 *   2. M2 announces the change of R to all service members. 
 * 
 * * Member M2 is asked to renew the lease for R for period T.
 *   1. M2 renews the lease;
 *   2. M2 announces that the lease for R is renewed to all service members. 
 * 
 * * Member M2 dies or doesn't release R within the period T.
 *   1. All service members clear the lease;
 *   2. At this point R becomes "Available everywhere.
 * 
 * 
 * The message range from [33,128] is reserved for usage by the ReplicatedCache
 * component.
 * 
 * Currently used MessageTypes:
 * [1-32]  Reserved by Grid
 * 33         LeaseIssue
 * 34         LeaseIssueRequest
 * 35         LeaseLock
 * 36         LeaseLockRequest
 * 37         LeaseRemove
 * 38         LeaseRemoveRequest
 * 39         LeaseUnlock
 * 40         LeaseUnlockRequest
 * 41         LeaseUpdate
 * 42         LeaseUpdateRequest
 * 43         CacheUpdate
 * 44         CacheUpdateRequest
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class ReplicatedCache
        extends    com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid
        implements com.tangosol.net.CacheService
    {
    // ---- Fields declarations ----
    
    /**
     * Property BackingMapContext
     *
     * The BackingMapContext (lazily created) is used by the BackingMapManager
     * (if provided).
     */
    private transient ReplicatedCache.BackingMapContext __m_BackingMapContext;
    
    /**
     * Property BackingMapManager
     *
     * Interface that provides the backing map storage implementations for the
     * cache.
     */
    private transient com.tangosol.net.BackingMapManager __m_BackingMapManager;
    
    /**
     * Property CacheHandler
     *
     * The array of CacheHandlers. It contains at least one element (at the
     * reserved index zero) which is a handler with a reserved name managing
     * the coherently shared map (referred to as the "Cache Catalog") holding
     * the configuration info (such as name, index, etc.) for all the cache
     * holders keyed by the cache name.
     */
    private transient com.tangosol.coherence.component.util.CacheHandler[] __m_CacheHandler;
    
    /**
     * Property LockingNextMillis
     *
     * The LockingNextMillis value is the time (in local system millis) at
     * which the next deferred lock evaluation will be performed.
     * 
     * Initial value is Long.MAX_VALUE.
     */
    private transient long __m_LockingNextMillis;
    
    /**
     * Property LockRequestQueue
     *
     * The queue of pending LockRequest messages.
     */
    private java.util.List __m_LockRequestQueue;
    
    /**
     * Property UpdatingMemberSet
     *
     * This member set contains the service members of the cluster that this
     * service is allowed to listen to. This set may not be equal to the
     * ServiceMemberSet only during the service initialization when this member
     * is welcomed (updated) by others. As soon as another service member
     * welcomes this service it gets included into the UpdatingMemberSett
     * allowing all the incoming LeaseMessages to be processed.
     */
    private transient com.tangosol.coherence.component.net.MemberSet __m_UpdatingMemberSet;
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
        __mapChildren.put("BackingMapContext", ReplicatedCache.BackingMapContext.get_CLASS());
        __mapChildren.put("BusEventMessage", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.BusEventMessage.get_CLASS());
        __mapChildren.put("CacheHandler", ReplicatedCache.CacheHandler.get_CLASS());
        __mapChildren.put("CacheUpdate", ReplicatedCache.CacheUpdate.get_CLASS());
        __mapChildren.put("CacheUpdateRequest", ReplicatedCache.CacheUpdateRequest.get_CLASS());
        __mapChildren.put("ConfigRequest", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.ConfigRequest.get_CLASS());
        __mapChildren.put("ConfigResponse", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.ConfigResponse.get_CLASS());
        __mapChildren.put("ConfigSync", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.ConfigSync.get_CLASS());
        __mapChildren.put("ConfigUpdate", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.ConfigUpdate.get_CLASS());
        __mapChildren.put("ConverterFromInternal", ReplicatedCache.ConverterFromInternal.get_CLASS());
        __mapChildren.put("ConverterToInternal", ReplicatedCache.ConverterToInternal.get_CLASS());
        __mapChildren.put("DispatchEvent", ReplicatedCache.DispatchEvent.get_CLASS());
        __mapChildren.put("DispatchNotification", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.DispatchNotification.get_CLASS());
        __mapChildren.put("LeaseIssue", ReplicatedCache.LeaseIssue.get_CLASS());
        __mapChildren.put("LeaseIssueRequest", ReplicatedCache.LeaseIssueRequest.get_CLASS());
        __mapChildren.put("LeaseLock", ReplicatedCache.LeaseLock.get_CLASS());
        __mapChildren.put("LeaseLockRequest", ReplicatedCache.LeaseLockRequest.get_CLASS());
        __mapChildren.put("LeaseRemove", ReplicatedCache.LeaseRemove.get_CLASS());
        __mapChildren.put("LeaseRemoveRequest", ReplicatedCache.LeaseRemoveRequest.get_CLASS());
        __mapChildren.put("LeaseUnlock", ReplicatedCache.LeaseUnlock.get_CLASS());
        __mapChildren.put("LeaseUnlockRequest", ReplicatedCache.LeaseUnlockRequest.get_CLASS());
        __mapChildren.put("LeaseUpdate", ReplicatedCache.LeaseUpdate.get_CLASS());
        __mapChildren.put("LeaseUpdateRequest", ReplicatedCache.LeaseUpdateRequest.get_CLASS());
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
        __mapChildren.put("NotifyServiceJoining", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyServiceJoining.get_CLASS());
        __mapChildren.put("NotifyServiceLeaving", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyServiceLeaving.get_CLASS());
        __mapChildren.put("NotifyServiceLeft", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyServiceLeft.get_CLASS());
        __mapChildren.put("NotifyServiceQuiescence", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyServiceQuiescence.get_CLASS());
        __mapChildren.put("NotifyShutdown", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyShutdown.get_CLASS());
        __mapChildren.put("NotifyStartup", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyStartup.get_CLASS());
        __mapChildren.put("PingRequest", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.PingRequest.get_CLASS());
        __mapChildren.put("ProtocolContext", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.ProtocolContext.get_CLASS());
        __mapChildren.put("Response", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.Response.get_CLASS());
        __mapChildren.put("WrapperGuardable", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.WrapperGuardable.get_CLASS());
        }
    
    // Default constructor
    public ReplicatedCache()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public ReplicatedCache(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            setLockingNextMillis(9223372036854775807L);
            setLockRequestQueue(new java.util.LinkedList());
            setMessageClassMap(new java.util.HashMap());
            setNotifier(new com.oracle.coherence.common.base.SingleWaiterMultiNotifier());
            setOldestPendingRequestSUIDCounter(new java.util.concurrent.atomic.AtomicLong());
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
        _addChild(new ReplicatedCache.CatalogHandler("CatalogHandler", this, true), "CatalogHandler");
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
        return "ReplicatedCache";
        }
    
    //++ getter for static property _Instance
    /**
     * Getter for property _Instance.<p>
    * Auto generated
     */
    public static com.tangosol.coherence.Component get_Instance()
        {
        return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ReplicatedCache();
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
            clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/ReplicatedCache".replace('/', '.'));
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
    
    // Declared at the super level
    public void _imports()
        {
        // import Component.Net.Lease;
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
        }
    
    /**
     * Instantiate the cache handler for the specified cache index and name.
    * 
    * @param iCache the cache index. If -1 is passed, teh first unused slot
    * should be used
    * @param sName  the cache name
    * @param loader  the class loader for this cache
    * 
    * Note: this method should be called only while holding the synchronization
    * monitor for the Cache Catalog (cache handler index zero) except when the
    * Catalog itself gets instantiated.
    * 
    * @see #getCacheHandler(int)
    * @see #start
     */
    protected com.tangosol.coherence.component.util.CacheHandler cloneCacheHandler(com.tangosol.coherence.component.util.CacheHandler handler, ClassLoader loader)
        {
        // import Component.Util.CacheHandler as com.tangosol.coherence.component.util.CacheHandler;
        
        com.tangosol.coherence.component.util.CacheHandler handlerClone = (com.tangosol.coherence.component.util.CacheHandler) _newChild("CacheHandler");
        
        // Note: setCacheName() must follow both
        // setNextHandler and setResourceMap call
        
        handlerClone.setNextHandler(handler);
        handlerClone.setClassLoader(loader);
        handlerClone.setLeaseMap(handler.getLeaseMap());
        handlerClone.setResourceMap(handler.getResourceMap());
        handlerClone.setStandardLeaseMillis(handler.getStandardLeaseMillis());
        handlerClone.setCacheIndex(handler.getCacheIndex());
        handlerClone.setCacheName(handler.getCacheName());
        
        return handlerClone;
        }
    
    // Declared at the super level
    /**
     * Create a new Default dependencies object by copying the supplies
    * dependencies.  Each class or component that uses dependencies implements
    * a Default dependencies class which provides the clone functionality.  
    * The dependency injection design pattern requires every component in the
    * component hierarchy to implement clone, producing their variant of the
    * dependencies interface.
    * 
    * @return the cloned dependencies
     */
    protected com.tangosol.internal.net.service.DefaultServiceDependencies cloneDependencies(com.tangosol.net.ServiceDependencies deps)
        {
        // import com.tangosol.internal.net.service.grid.DefaultReplicatedCacheDependencies;
        // import com.tangosol.internal.net.service.grid.ReplicatedCacheDependencies;
        
        return new DefaultReplicatedCacheDependencies(
            (ReplicatedCacheDependencies) deps);
        }
    
    // From interface: com.tangosol.net.CacheService
    // Declared at the super level
    public synchronized void configure(com.tangosol.run.xml.XmlElement xml)
        {
        // import com.tangosol.internal.net.service.grid.DefaultReplicatedCacheDependencies;
        // import com.tangosol.internal.net.service.grid.LegacyXmlReplicatedCacheHelper as com.tangosol.internal.net.service.grid.LegacyXmlReplicatedCacheHelper;
        
        setServiceConfig(xml);
        
        setDependencies(com.tangosol.internal.net.service.grid.LegacyXmlReplicatedCacheHelper.fromXml(xml,
            new DefaultReplicatedCacheDependencies(), getOperationalContext(),
            getContextClassLoader()));
        }
    
    // From interface: com.tangosol.net.CacheService
    public void destroyCache(com.tangosol.net.NamedCache map)
        {
        // import Component.Util.CacheHandler as com.tangosol.coherence.component.util.CacheHandler;
        
        _assert(map instanceof com.tangosol.coherence.component.util.CacheHandler, "Invalid map");
        
        com.tangosol.coherence.component.util.CacheHandler handler  = (com.tangosol.coherence.component.util.CacheHandler) map;
        com.tangosol.coherence.component.util.CacheHandler hCatalog = getCacheHandler(0);
        if (handler == hCatalog)
            {
            // the catalog is not removeable
            return;
            }
        
        if (getThread() == Thread.currentThread())
            {
            // the request came on the service thread;
            // invalidate the entire chain
            // TODO: we should recycle this handler's index later
        
            while (true)
                {
                handler.invalidate();
        
                com.tangosol.coherence.component.util.CacheHandler hNext = handler.getNextHandler();
                if (hNext == null)
                    {
                    break;
                    }
                else
                    {
                    handler.setNextHandler(null);
                    handler = hNext;
                    }
                }
             }
        else
            {
            // notify everyone to remove the cache
            String sCacheName   = handler.getCacheName();
            String sCatalogName = hCatalog.getCacheName();
        
            _assert(sCacheName != null && !sCacheName.equals(sCatalogName));
        
            lockResource(hCatalog, sCatalogName, 0L, -1L);
            try
                {
                removeResource(hCatalog, sCacheName, false);
                }
            finally
                {
                unlockResource(hCatalog, sCatalogName);
                }
            }
        }
    
    // Declared at the super level
    /**
     * Wait for the service's associated backlog to drain.
    * 
    * @param setMembers  the members of interest
    * @param cMillis             the maximum amount of time to wait, or 0 for
    * infinite
    * 
    * @return the remaining timeout
    * 
    * @throw RequestTimeoutException on timeout
     */
    public long drainOverflow(com.tangosol.coherence.component.net.MemberSet setMembers, long cMillis)
            throws java.lang.InterruptedException
        {
        // account for the replicated cache usage model where requests are queue'd to the local member which then
        // broadcasts them to all other members
        
        return setMembers != null && setMembers.size() == 1 && setMembers.contains(getThisMember())
            ? super.drainOverflow(getOthersMemberSet(), cMillis)
            : super.drainOverflow(setMembers, cMillis);
        }
    
    // From interface: com.tangosol.net.CacheService
    /**
     * Ensure the existence of a cache handler for the specified cache name and
    * class loader.
    * 
    * @param sName  the cache name
    * @param loader  class loader to be used by the cache
    * 
    * @return the appropriate cache handler
    * 
    * @see #removeCache
     */
    public com.tangosol.net.NamedCache ensureCache(String sName, ClassLoader loader)
        {
        // import Component.Util.CacheHandler as com.tangosol.coherence.component.util.CacheHandler;
        // import com.tangosol.util.Base;
        // import com.tangosol.util.SafeHashMap;
        // import com.tangosol.util.WrapperException;
        
        checkStatus();
        
        if (sName == null || sName.length() == 0)
            {
            sName = "Default";
            }
        
        if (loader == null)
            {
            loader = getContextClassLoader();
            _assert(loader != null, "ContextClassLoader is missing");
            }
        
        com.tangosol.coherence.component.util.CacheHandler hCatalog = getCacheHandler(0);
        String       sCatalog = hCatalog.getCacheName();
        
        // first, do a quick lookup
        if (sName.equals(sCatalog))
            {
            return hCatalog;
            }
        
        com.tangosol.coherence.component.util.CacheHandler[] aHandler = getCacheHandler();
        
        for (int iCache = 1, c = aHandler.length; iCache < c; iCache++)
            {
            com.tangosol.coherence.component.util.CacheHandler handler = aHandler[iCache];
            if (handler != null && sName.equals(handler.getCacheName()))
                {
                do
                    {
                    if (loader == handler.getClassLoader())
                        {
                        return handler;
                        }
                    handler = handler.getNextHandler();
                    }
                while (handler != null);
                }
            }
        
        // TODO: replace with CacheIdRequest poll to the senior
        
        // now let's go all the way
        lockResource(hCatalog, sCatalog, 0L, -1L);
        try
            {
            com.tangosol.coherence.component.util.CacheHandler handler;
            long         cEnsureCacheTimeout =
                Base.getSafeTimeMillis() + getReplicatedCacheDependencies().getEnsureCacheTimeoutMillis();
        
            synchronized (hCatalog)
                {
                aHandler = getCacheHandler();
        
            findMatchingCache:
                for (int iCache = 1, c = aHandler.length; iCache < c; iCache++)
                    {
                    handler = aHandler[iCache];
                    if (handler == null)
                        {
                        // we may reuse this spot later if no match is found
                        continue;
                        }
        
                    while (handler.getCacheName() == null && handler.isValid())
                        {
                        if (getServiceOldestMember() == getThisMember())
                            {
                            // everyone who new about this cache has already died;
                            // consider this handler available
                            handler.getLeaseMap().clear();
                            handler.setResourceMap(new SafeHashMap());
                            continue findMatchingCache;
                            }
        
                        if (Base.getSafeTimeMillis() > cEnsureCacheTimeout)
                            {
                            // COH-1094: Somehow we missed catalog update
                            throw new IllegalStateException("Timeout waiting for catalog update");
                            }
        
                        _trace("Waiting for catalog update: " + iCache, 3);
                        try
                            {
                            // An anonymous cache handler has already been created,
                            // but the catalog has not been fully replicated yet.
                            // This can only happen during a very short period of time
                            // when existing service members have started sending
                            // updates, but the coressponding catalog (from the issuer)
                            // has not came yet
                            hCatalog.wait(1000);
        
                            handler = aHandler[iCache];
                            if (handler == null)
                                {
                                continue findMatchingCache;
                                }
                            }
                        catch (InterruptedException e)
                            {
                            Thread.currentThread().interrupt();
                            throw new WrapperException(e);
                            }
                        }
        
                    if (sName.equals(handler.getCacheName()))
                        {
                        if (handler.isValid())
                            {
                            com.tangosol.coherence.component.util.CacheHandler handlerNext = handler;
                            do
                                {
                                ClassLoader loaderCache = handlerNext.getClassLoader();
                                if (loaderCache == null)
                                    {
                                    // this is an orphaned handler -- reuse it
                                    handlerNext.setClassLoader(loader);
                                    return handlerNext;
                                    }
                                 else if (loaderCache == loader)
                                    {
                                    // we've got this already
                                    return handlerNext;
                                    }
        
                                handlerNext = handlerNext.getNextHandler();
                                }
                            while (handlerNext != null);
        
                            // no match - we need to clone the handler and insert into the chain
                            // don't set the name till the next handler and resource map are set
                            // (since Coherence 2.5)
                            handlerNext = cloneCacheHandler(handler, loader);
                            setCacheHandler(iCache, handlerNext);
                            return handlerNext;
                            }
                        else
                            {
                            // this is an invalid handler -- remove it
                            setCacheHandler(iCache, null);
                            break;
                            }
                        }
                    }
        
                // no match has been found -- create a new handler
                handler = instantiateCacheHandler(-1, sName, loader);
                handler.setStandardLeaseMillis(getReplicatedCacheDependencies().getStandardLeaseMillis());
                setCacheHandler(handler.getCacheIndex(), handler);
                }
        
            if (hCatalog.getCachedResource(sCatalog) == null)
                {
                // put the catalog header
                updateResource(hCatalog, sCatalog, hCatalog.toXml(), 0L, false, false);
                }
        
            // update the Catalog
            updateResource(hCatalog, sName, handler.toXml(), 0L, true, false);
            return handler;
            }
        finally
            {
            unlockResource(hCatalog, sCatalog);
            }
        }
    
    /**
     * Obtain a cache handler for the specified cache index.
     */
    public com.tangosol.coherence.component.util.CacheHandler ensureCacheHandler(int iCache)
        {
        // import Component.Util.CacheHandler as com.tangosol.coherence.component.util.CacheHandler;
        // import com.tangosol.run.component.EventDeathException;
        
        com.tangosol.coherence.component.util.CacheHandler handler = getCacheHandler(iCache);
        
        if (handler == null)
            {
            com.tangosol.coherence.component.util.CacheHandler hCatalog = getCacheHandler(0);
            _assert(hCatalog != null, "Catalog is missing");
        
            synchronized (hCatalog)
                {
                handler = getCacheHandler(iCache);
                if (handler == null)
                    {
                    handler = instantiateCacheHandler(iCache, null, getContextClassLoader());
                    setCacheHandler(iCache, handler);
                    }
                }
            }
        
        return handler;
        }
    
    /**
     * Evaluate all pending lock requests. Called on the service thread only.
    * 
    * @param fValidate  if true validate the leases; otherwise just check the
    * timeouts
     */
    protected void evaluateLockRequests()
        {
        // import Component.Net.Lease;
        // import Component.Net.Member;
        // import com.tangosol.run.component.EventDeathException;
        // import java.util.ArrayList;
        // import java.util.List;
        // import java.util.Iterator;
        
        long lTime = getClusterTime();
        long lNext = Long.MAX_VALUE;
        
        // reset; this must be done before "scheduleLockEvaluation" is called
        setLockingNextMillis(lNext);
        
        List listPending = getLockRequestQueue();
        int  cPending    = listPending.size();
        if (cPending > 0)
            {
            List listRetry = new ArrayList(cPending);
        
            for (Iterator iter = listPending.iterator(); iter.hasNext();)
                {
                ReplicatedCache.LeaseLockRequest msgLockRequest = (ReplicatedCache.LeaseLockRequest) iter.next();
        
                // clear up the lock requests by dead members
                Member member = msgLockRequest.getFromMember();
                if (!getServiceMemberSet().contains(member))
                    {
                    iter.remove();
                    continue;
                    }
        
                long lTimeout = msgLockRequest.getWaitTimeout();
                if (lTimeout <= lTime)
                    {
                    iter.remove();
        
                    // this is going to be a last attempt
                    listRetry.add(msgLockRequest);
                    continue;
                    }
        
                Lease  leaseRequest = msgLockRequest.getLease();
                Object oKey         = leaseRequest.getResourceKey();
                int    iCache       = leaseRequest.getCacheIndex();
                Lease  leaseCurrent = ensureCacheHandler(iCache).ensureLease(oKey);
        
                leaseCurrent.validate();
        
                switch (leaseCurrent.getStatus())
                    {
                    case Lease.LEASE_LOCKED:
                        // still locked
                        lTimeout = Math.min(lTimeout, leaseCurrent.getExpirationTime());
                        break;
                    default:
                        // expired (the lease holder is probably gone)
                        iter.remove();
                        listRetry.add(msgLockRequest);
                        continue;
                    }
        
               if (lTimeout != Long.MAX_VALUE)
                    {
                    lNext = Math.min(lNext, lTimeout);
                    }
                }
        
            for (Iterator iter = listRetry.iterator(); iter.hasNext();)
                {
                ReplicatedCache.LeaseLockRequest msgLockRequest = (ReplicatedCache.LeaseLockRequest) iter.next();
                try
                    {
                    onLeaseLockRequest(msgLockRequest);
                    }
                catch (EventDeathException e) {}
                }
            }
        
        if (lNext != Long.MAX_VALUE)
            {
            scheduleLockEvaluation(lNext - lTime);
            }
        }
    
    /**
     * Fire all pending locks for the specified lease (there could be many due
    * to the "member" lease ownership model). This method is called on the
    * service thread when a current lease is unlocked.
    * 
    * @see perfromUnlock
     */
    protected void firePendingLocks(com.tangosol.coherence.component.net.Lease lease)
        {
        // import com.tangosol.run.component.EventDeathException;
        // import com.tangosol.util.Base;
        // import java.util.ArrayList;
        // import java.util.List;
        // import java.util.Iterator;
        
        List listRetry   = null;
        List listPending = getLockRequestQueue();
        int  cPending    = listPending.size();
        if (cPending > 0)
            {
            listRetry = new ArrayList(cPending);
            for (Iterator iter = listPending.iterator(); iter.hasNext();)
                {
                ReplicatedCache.LeaseLockRequest msgLockRequest = (ReplicatedCache.LeaseLockRequest) iter.next();
        
                Lease  leaseRequest = msgLockRequest.getLease();
                int    iCache       = leaseRequest.getCacheIndex();
                Object oKey         = leaseRequest.getResourceKey();
        
                if (iCache == lease.getCacheIndex() &&
                    Base.equals(oKey, lease.getResourceKey()))
                    {
                    iter.remove();
                    leaseRequest.copyVersion(lease);
                    listRetry.add(msgLockRequest);
                    }
                }
            }
        
        if (listRetry != null && !listRetry.isEmpty())
            {
            for (Iterator iter = listRetry.iterator(); iter.hasNext();)
                {
                ReplicatedCache.LeaseLockRequest msgLockRequest = (ReplicatedCache.LeaseLockRequest) iter.next();
        
                // at least the first should succeed now
                try
                    {
                    onLeaseLockRequest(msgLockRequest);
                    }
                catch (EventDeathException e) {}
                }
            }
        }
    
    // Accessor for the property "BackingMapContext"
    /**
     * Getter for property BackingMapContext.<p>
    * The BackingMapContext (lazily created) is used by the BackingMapManager
    * (if provided).
     */
    public ReplicatedCache.BackingMapContext getBackingMapContext()
        {
        ReplicatedCache.BackingMapContext context = __m_BackingMapContext;
        if (context == null)
            {
            synchronized (this)
                {
                context = __m_BackingMapContext;
                if (context == null)
                    {
                    context = (ReplicatedCache.BackingMapContext) _newChild("BackingMapContext");
                    setBackingMapContext(context);
                    }
                }
            }
        return context;
        }
    
    // From interface: com.tangosol.net.CacheService
    // Accessor for the property "BackingMapManager"
    /**
     * Getter for property BackingMapManager.<p>
    * Interface that provides the backing map storage implementations for the
    * cache.
     */
    public com.tangosol.net.BackingMapManager getBackingMapManager()
        {
        return __m_BackingMapManager;
        }
    
    // Accessor for the property "CacheHandler"
    /**
     * Getter for property CacheHandler.<p>
    * The array of CacheHandlers. It contains at least one element (at the
    * reserved index zero) which is a handler with a reserved name managing the
    * coherently shared map (referred to as the "Cache Catalog") holding the
    * configuration info (such as name, index, etc.) for all the cache holders
    * keyed by the cache name.
     */
    protected com.tangosol.coherence.component.util.CacheHandler[] getCacheHandler()
        {
        return __m_CacheHandler;
        }
    
    // Accessor for the property "CacheHandler"
    /**
     * Getter for property CacheHandler.<p>
    * The array of CacheHandlers. It contains at least one element (at the
    * reserved index zero) which is a handler with a reserved name managing the
    * coherently shared map (referred to as the "Cache Catalog") holding the
    * configuration info (such as name, index, etc.) for all the cache holders
    * keyed by the cache name.
     */
    public com.tangosol.coherence.component.util.CacheHandler getCacheHandler(int iCache)
        {
        // import Component.Util.CacheHandler as com.tangosol.coherence.component.util.CacheHandler;
        
        com.tangosol.coherence.component.util.CacheHandler[] aHandler  = getCacheHandler();
        int            cHandlers = aHandler == null ? 0 : aHandler.length;
        
        return iCache >= cHandlers ? null : aHandler[iCache];
        }
    
    // From interface: com.tangosol.net.CacheService
    // Accessor for the property "CacheNames"
    /**
     * Getter for property CacheNames.<p>
    * Returns an enumeration of String objects, one for each cache name.
    * 
    * @see #ensureCache(String, ClassLoader)
     */
    public java.util.Enumeration getCacheNames()
        {
        // import Component.Util.CacheHandler as com.tangosol.coherence.component.util.CacheHandler;
        // import com.tangosol.util.FilterEnumerator;
        // import com.tangosol.util.NullFilter;
        
        checkStatus();
        
        com.tangosol.coherence.component.util.CacheHandler[] aHandler  = getCacheHandler();
        int            cHandlers = aHandler.length;
        String[]       aName     = new String[cHandlers];
        
        for (int i = 1; i < cHandlers; i++)
            {
            com.tangosol.coherence.component.util.CacheHandler handler = aHandler[i];
            if (handler != null && handler.isValid())
                {
                aName[i] = handler.getCacheName();
                }
            }
        
        return new FilterEnumerator(aName, NullFilter.getInstance());
        }
    
    // Accessor for the property "LockingNextMillis"
    /**
     * Getter for property LockingNextMillis.<p>
    * The LockingNextMillis value is the time (in local system millis) at which
    * the next deferred lock evaluation will be performed.
    * 
    * Initial value is Long.MAX_VALUE.
     */
    public long getLockingNextMillis()
        {
        return __m_LockingNextMillis;
        }
    
    // Accessor for the property "LockRequestQueue"
    /**
     * Getter for property LockRequestQueue.<p>
    * The queue of pending LockRequest messages.
     */
    public java.util.List getLockRequestQueue()
        {
        return __m_LockRequestQueue;
        }
    
    public com.tangosol.internal.net.service.grid.ReplicatedCacheDependencies getReplicatedCacheDependencies()
        {
        // import com.tangosol.internal.net.service.grid.ReplicatedCacheDependencies;
        
        return (ReplicatedCacheDependencies) getDependencies();
        }
    
    /**
     * Calculate the Lease status with regard to the current thread. The return
    * value is one of:
    * <ul>
    * <li> LEASE_UNISSUED - a request for a lease issue has not been confirmed
    * yet or the issuer is gone
    * <li> LEASE_AVAILABLE - the lease is known to be available
    * <li> LEASE_LOCKED - the lease is known to be held by the calling thread
    * on this service member 
    * <li> LEASE_DIRTY - the lease is known to be held by another service
    * member or another thread on this member
    * </ul>
    * 
    * Note: this method is calling Lease.validate() internally and therefore
    * may require synchronization on the lease.
     */
    public int getThreadStatus(com.tangosol.coherence.component.net.Lease lease)
        {
        lease.validate();
        
        int nStatus = lease.getStatus();
        if (getReplicatedCacheDependencies().getLeaseGranularity() == Lease.BY_MEMBER)
            {
            return nStatus;
            }
        else
            {
            return nStatus == Lease.LEASE_LOCKED &&
                Lease.getCurrentThreadId() != lease.getHolderThreadId() ?
                    Lease.LEASE_DIRTY : nStatus;
            }
        }
    
    // Accessor for the property "UpdatingMemberSet"
    /**
     * Getter for property UpdatingMemberSet.<p>
    * This member set contains the service members of the cluster that this
    * service is allowed to listen to. This set may not be equal to the
    * ServiceMemberSet only during the service initialization when this member
    * is welcomed (updated) by others. As soon as another service member
    * welcomes this service it gets included into the UpdatingMemberSett
    * allowing all the incoming LeaseMessages to be processed.
     */
    public com.tangosol.coherence.component.net.MemberSet getUpdatingMemberSet()
        {
        return __m_UpdatingMemberSet;
        }
    
    // Declared at the super level
    /**
     * Getter for property WaitMillis.<p>
    * The number of milliseconds that the daemon will wait for notification.
    * Zero means to wait indefinitely. Negative value means to skip waiting
    * altogether.
    * 
    * @see #onWait
     */
    public long getWaitMillis()
        {
        // import com.tangosol.util.Base;
        
        long cWait1 = super.getWaitMillis();
        long cWait2 = Math.max(1L,
            getLockingNextMillis() - Base.getSafeTimeMillis());
        return cWait1 <= 0L ? cWait2 : Math.min(cWait1, cWait2);
        }
    
    /**
     * Instantiate the cache handler for the specified cache index and name.
    * 
    * @param iCache the cache index. If -1 is passed, teh first unused slot
    * should be used
    * @param sName  the cache name
    * @param loader  the class loader for this cache
    * 
    * Note: this method should be called only while holding the synchronization
    * monitor for the Cache Catalog (cache handler index zero) except when the
    * Catalog itself gets instantiated.
    * 
    * @see #getCacheHandler(int)
    * @see #start
     */
    protected com.tangosol.coherence.component.util.CacheHandler instantiateCacheHandler(int iCache, String sName, ClassLoader loader)
        {
        // import Component.Util.CacheHandler as com.tangosol.coherence.component.util.CacheHandler;
        
        com.tangosol.coherence.component.util.CacheHandler handler = (com.tangosol.coherence.component.util.CacheHandler) _newChild("CacheHandler");
        
        if (iCache == -1)
            {
            com.tangosol.coherence.component.util.CacheHandler[] aHandler = getCacheHandler();
        
            iCache = 1;
            for (int c = aHandler.length;
                iCache < c && aHandler[iCache] != null;
                iCache++)
                {
                }
            }
        
        handler.setCacheIndex(iCache);
        handler.setCacheName(sName);
        handler.setClassLoader(loader);
        
        return handler;
        }
    
    public com.tangosol.util.Converter instantiateConverterFromInternal(ClassLoader loader)
        {
        ReplicatedCache.ConverterFromInternal conv =
            (ReplicatedCache.ConverterFromInternal) _newChild("ConverterFromInternal");
        
        conv.setSerializer(ensureSerializer(loader));
        
        return conv;
        }
    
    public com.tangosol.util.Converter instantiateConverterToInternal(ClassLoader loader)
        {
        ReplicatedCache.ConverterToInternal conv = (ReplicatedCache.ConverterToInternal) _newChild("ConverterToInternal");
        
        conv.setSerializer(ensureSerializer(loader));
        
        return conv;
        }
    
    /**
     * Obtain a hold on a Lease for the entire cache. This method should only be
    * called on a client's thread.
    * 
    * @param handler  the cache handler
    * @param cLeaseMillis  the duration of the Lease in milliseconds starting
    * immediately; pass 0 for indefinite lease duration
    * @param cWaitMillis  the maximum time to wait in milliseconds; if zero
    * this method returns right away; if negative, it blocks until the lease
    * can be obtained
    * 
    * @return true if the cache was successfully locked; false otherwise.
    * Obviously, if the negative <code>cWaitMillis </code> has been passed,
    * this method can only return true.
     */
    protected boolean lockCache(com.tangosol.coherence.component.util.CacheHandler handler, long cLeaseMillis, long cWaitMillis)
        {
        // import Component.Util.CacheHandler as com.tangosol.coherence.component.util.CacheHandler;
        // import java.util.Enumeration;
        
        com.tangosol.coherence.component.util.CacheHandler hCatalog   = getCacheHandler(0);
        String       sCacheName = handler.getCacheName();
        
        // TODO: this has to be redone using the poll --
        // all the members have to agree to the cache lock
        
        if (lockResource(hCatalog, sCacheName, 0L, cWaitMillis))
            {
            boolean fDirty = false;
            try
                {
                // make sure that there are no dirty resources
                for (Enumeration e = handler.getLeaseKeys(); e.hasMoreElements();)
                    {
                    Object oKey  = e.nextElement();
                    Lease  lease = handler.getLease(oKey);
                    
                    if (lease != null)
                        {
                        while ((fDirty = (getThreadStatus(lease) == Lease.LEASE_DIRTY)) &&
                               cWaitMillis != 0)
                            {
                            cWaitMillis = waitForUnlock(lease, cWaitMillis);
                            }
                        
                        if (fDirty)
                            {
                            return false;
                            }
                        }
                    }
                }
            finally
                {
                if (fDirty)
                    {
                    unlockResource(hCatalog, sCacheName);
                    }
                }
            return true;
            }
        return false;
        }
    
    /**
     * Obtain a hold on a Lease for a resource with the specified name for the
    * specified duration. This method should only be called on a client's
    * thread.
    * 
    * @param handler  the cache handler
    * @param oKey  the resource key
    * @param cLeaseMillis  the duration of the Lease in milliseconds starting
    * immediately after the lock is acquired; pass 0 for indefinite lease
    * duration
    * @param cWaitMillis  the maximum time to wait in milliseconds; if zero
    * this method returns right away; if negative, it blocks until the lease
    * can be obtained
    * 
    * @return true if the resource was successfully locked; false otherwise.
    * Obviously, if the negative <code>cWaitMillis </code> has been passed,
    * this method can only return true.
     */
    public boolean lockResource(com.tangosol.coherence.component.util.CacheHandler handler, Object oKey, long cLeaseMillis, long cWaitMillis)
        {
        // import Component.Net.Member;
        // import Component.Util.CacheHandler as com.tangosol.coherence.component.util.CacheHandler;
        // import com.tangosol.util.Base;
        // import com.tangosol.util.ConcurrentMap;
        
        if (oKey == ConcurrentMap.LOCK_ALL)
            {
            return lockCache(handler, cLeaseMillis, cWaitMillis);
            }
        
        com.tangosol.coherence.component.util.CacheHandler hCatalog     = getCacheHandler(0);
        String       sCacheName   = handler.getCacheName();
        long         lWaitTimeout = adjustWaitTime(cWaitMillis, TIME_SAFE);
        while (true)
            {
            // make sure that the cache is not locked
            Lease leaseCache = hCatalog.getLease(sCacheName);
        
            if (leaseCache != null && getThreadStatus(leaseCache) == Lease.LEASE_DIRTY)
                {
                if (cWaitMillis == 0)
                    {
                    return false;
                    }
                cWaitMillis = waitForUnlock(leaseCache, cWaitMillis);
                continue;
                }
        
            Lease lease = handler.ensureLease(oKey);
            switch (getThreadStatus(lease))
                {
                case Lease.LEASE_UNISSUED:
                    requestIssue(lease);
        
                    // if the lease was removed while issuing, try again;
                    // if the lease was issued, this will do as well
                    break;
        
                case Lease.LEASE_AVAILABLE:
                    {
                    Member memberIssuer = getServiceMemberSet().getMember(lease.getIssuerId());
                    if (memberIssuer == null)
                        {
                        // the issuer is gone, try again
                        break;
                        }
        
                    if (requestLock(lease, memberIssuer, cLeaseMillis, cWaitMillis, false))
                        {
                        return true;
                        }
                    // the lease was removed or rejected while locking, try again;
                    break;
                    }
        
                case Lease.LEASE_LOCKED:
                    {
                    Member memberIssuer = getServiceMemberSet().getMember(lease.getIssuerId());
                    if (memberIssuer == null)
                        {
                        // the issuer is gone, this member should become one
                        memberIssuer = getThisMember();
                        }
        
                    // renewing the lease is similar to locking ...
                    if (requestLock(lease, memberIssuer, cLeaseMillis, cWaitMillis, true))
                        {
                        return true;
                        }
                    // the lease was removed or rejected while locking, try again;
                    break;
                    }
        
                case Lease.LEASE_DIRTY:
                    if (Base.getSafeTimeMillis() >= lWaitTimeout)
                        {
                        return false;
                        }
                    Member memberIssuer = getServiceMemberSet().getMember(lease.getIssuerId());
                    if (memberIssuer == null)
                        {
                        // the issuer is gone, the holder should become one
                        memberIssuer = getServiceMemberSet().getMember(lease.getHolderId());
                        if (memberIssuer == null)
                            {
                            break;
                            }
                        }
        
                    if (requestLock(lease, memberIssuer, cLeaseMillis, cWaitMillis, false))
                        {
                        return true;
                        }
                    // the lease was removed or rejected while locking, try again;
                    break;
        
                default:
                    throw new IllegalStateException();
                }
            }
        }
    
    /**
     * Inform a member about a lease status. This serves as a confirmation or
    * rejection for a request based on incorrect IssueId. This method is called
    * on the service thread.
    * 
    * @param lease  the current Lease
    * @param msgRequest the request message
    * @param msgResponse the response message; if null the LeaseIssue message
    * is used
     */
    protected void notifyLease(com.tangosol.coherence.component.net.Lease lease, com.tangosol.coherence.component.net.message.requestMessage.LeaseRequest msgRequest, com.tangosol.coherence.component.net.message.LeaseMessage msgResponse)
        {
        if (msgResponse == null)
            {
            msgResponse = (ReplicatedCache.LeaseIssue) instantiateMessage("LeaseIssue");
            }
        msgResponse.respondTo(msgRequest);
        msgResponse.setLease(lease);
        
        post(msgResponse);
        }
    
    /**
     * Welcome a new member by updating it with the leases issued by this
    * member. This method is called on the service thread and should not be
    * called externally.
    * 
    * @param msgRequest the request message to respond to
     */
    public void onCacheUpdateRequest(com.tangosol.coherence.component.net.message.RequestMessage msgRequest)
        {
        // import Component.Net.MemberSet;
        // import Component.Net.MemberSet.SingleMemberSet;
        // import Component.Util.CacheHandler as com.tangosol.coherence.component.util.CacheHandler;
        // import java.util.Enumeration;
        
        com.tangosol.coherence.component.util.CacheHandler[] aHandler  = getCacheHandler();
        int            cHandlers = aHandler.length;
        MemberSet      setTo     = SingleMemberSet.instantiate(msgRequest.getFromMember());
        ReplicatedCache.CacheUpdate   msg       = null;
        int            cbSize    = 102400; // preferred size is 100KB a pop
        
        for (int i = 0; i < cHandlers; i++)
            {
            com.tangosol.coherence.component.util.CacheHandler handler = aHandler[i];
            if (handler != null && handler.isValid())
                {
                Enumeration enumMore = null;
                do
                    {
                    if (msg != null && msg.getLeaseCount() > 0)
                        {
                        send(msg);
        
                        // yield to prevent excessive memory usage by the outgoing packet queue
                        Thread.yield();
                        }
                    msg = (ReplicatedCache.CacheUpdate) instantiateMessage("CacheUpdate");
                    msg.setToMemberSet(setTo);
                    msg.setCacheIndex(i);
        
                    enumMore = handler.populateUpdateMessage(msg, cbSize, enumMore);
                    }
                while (enumMore != null);
                }
            }
        
        _assert(msg != null); // at least the catalog should be present
        
        // the last "CacheUpdate" message serves as the poll response
        msg.respondTo(msgRequest);
        post(msg);
        }
    
    /**
     * Remove a catalog entry according to the specified xml. This method is
    * called on the service thread and should not be called externally.
    * 
    * @param xml  an XmlElement carrying a catalog entry information
     */
    public void onCatalogRemove(com.tangosol.run.xml.XmlElement xml)
        {
        // import Component.Util.CacheHandler as com.tangosol.coherence.component.util.CacheHandler;
        
        com.tangosol.coherence.component.util.CacheHandler hCatalog = getCacheHandler(0);
        
        synchronized (hCatalog)
            {
            int iCache = xml.getElement("CacheIndex").getInt();
        
            _assert(iCache > 0, "Attempt to remove the catalog");
        
            com.tangosol.coherence.component.util.CacheHandler handler = getCacheHandler(iCache);
            if (handler != null)
                {
                destroyCache(handler);
                }
        
            hCatalog.notifyAll();
            }
        }
    
    /**
     * Update a catalog entry according to the specified xml. This method is
    * called on the service thread and should not be called externally.
    * 
    * @param xml  an XmlElement carrying a catalog entry information
     */
    public void onCatalogUpdate(com.tangosol.run.xml.XmlElement xml)
        {
        // import Component.Util.CacheHandler as com.tangosol.coherence.component.util.CacheHandler;
        
        com.tangosol.coherence.component.util.CacheHandler hCatalog = getCacheHandler(0);
        
        synchronized (hCatalog)
            {
            int    iCache = xml.getElement("CacheIndex").getInt();
            String sName  = xml.getElement("CacheName").getString();
        
            com.tangosol.coherence.component.util.CacheHandler handler = getCacheHandler(iCache);
            if (handler == null || !handler.isValid())
                {
                handler = instantiateCacheHandler(iCache, sName, null);
                setCacheHandler(iCache, handler);
                }
            handler.fromXml(xml);
        
            hCatalog.notifyAll();
            }
        }
    
    // Declared at the super level
    /**
     * This event occurs when dependencies are injected into the component. 
    * First, call super.onDependencies to allow all super components to process
    * the Dependencies.  Each component is free to chose how it consumes
    * dependencies from within onDependencies.  Often (though not ideal), the 
    * dependencies are copied into the component's properties.  This technique
    * isolates Dependency Injection from the rest of the component code since
    * components continue to access properties just as they did before. 
    * 
    * PartitionedCacheDependencies deps = (PartitionedCacheDependencies)
    * getDependencies();
    * deps.getFoo();
     */
    protected void onDependencies(com.tangosol.net.ServiceDependencies deps)
        {
        // ReplicateCache doesn't copy dependencies data to component properties like
        // other Service components.  Instead, its dependencies are stored in
        // Service.Dependencies and accessed via ReplicateCache.getReplicatedCacheDependencies().
        // In essence, ReplicateCache$CacheConfig is being replaced by Service.Dependencies.
        
        super.onDependencies(deps);
        }
    
    // Declared at the super level
    /**
     * Event notification called right before the daemon thread terminates. This
    * method is guaranteed to be called only once and on the daemon's thread.
     */
    protected void onExit()
        {
        // import Component.Util.CacheHandler as com.tangosol.coherence.component.util.CacheHandler;
        
        super.onExit();
        
        com.tangosol.coherence.component.util.CacheHandler[] aHandler = getCacheHandler();
        if (aHandler != null)
            {
            for (int i = aHandler.length - 1; i >= 0; i--)
                {
                com.tangosol.coherence.component.util.CacheHandler handler = aHandler[i];
                if (handler != null)
                    {
                    handler.invalidate();
                    }
                }
            setCacheHandler(new com.tangosol.coherence.component.util.CacheHandler[0]);
            }
        }
    
    /**
     * Process the issue request. This method is called on the service thread
    * and should not be called externally.
    * 
    * @param msgRequest the request message
     */
    public void onLeaseIssueRequest(ReplicatedCache.LeaseIssueRequest msgRequest)
        {
        // import Component.Net.Member;
        // import Component.Net.MemberSet;
        
        Lease  leaseRequest = msgRequest.getLease();
        Object oKey         = leaseRequest.getResourceKey();
        int    iCache       = leaseRequest.getCacheIndex();
        Lease  leaseCurrent = ensureCacheHandler(iCache).ensureLease(oKey);
        
        leaseCurrent.validate();
        
        // In most cases the following hold true:
        //      getThisMember() == getServiceOldestMember()
        // However, it could happen (though quite unlikely) that a senior
        // member has just died and while the applicant already knows that,
        // this cluster member has not been updated yet
        
        if (leaseCurrent.getStatus() == Lease.LEASE_UNISSUED)
            {
            Member memberFrom = msgRequest.getFromMember();
            if (getReplicatedCacheDependencies().isMobileIssues())
                {
                // for backward compatibility with "mobile issues",
                // make the requestor be the issuer
                leaseCurrent.setIssuerId(memberFrom.getId());
                }
            else
                {
                // by default, make the service senior the issuer
                leaseCurrent.setIssuerId(getThisMember().getId());
                }
            leaseCurrent.incrementLeaseVersion();
        
            // inform everyone, but the applicant
            MemberSet setOther = getOthersMemberSet();
            setOther.remove(memberFrom);
        
            if (!setOther.isEmpty())
                {
                ReplicatedCache.LeaseIssue msg =
                    (ReplicatedCache.LeaseIssue) instantiateMessage("LeaseIssue");
                msg.setToMemberSet(setOther);
                msg.setLease(leaseCurrent);
        
                post(msg);
                }
            }
        
        // inform the applicant
        notifyLease(leaseCurrent, msgRequest, null);
        }
    
    /**
     * Process the lock request. This method is called on the service thread and
    * should not be called externally.
    * 
    * @param msgRequest the request message
     */
    public void onLeaseLockRequest(ReplicatedCache.LeaseLockRequest msgRequest)
        {
        Lease   leaseRequest = msgRequest.getLease();
        Object  oKey         = leaseRequest.getResourceKey();
        int     iCache       = leaseRequest.getCacheIndex();
        Lease   leaseCurrent = ensureCacheHandler(iCache).ensureLease(oKey);
        int     nThisId      = getThisMember().getId();
        boolean fReject      = false;
        
        leaseCurrent.validate();
        
        switch (leaseCurrent.getStatus())
            {
            case Lease.LEASE_UNISSUED:
                // assume that the issue is on its way
                leaseCurrent.setIssuerId(nThisId);
                // break through
            case Lease.LEASE_AVAILABLE:
                if (leaseCurrent.getIssuerId() != nThisId)
                    {
                    // the issuer has changed
                    fReject = true;
                    }
                else
                    {
                    // we will proceed with the request even if lease version
                    // [as known by the requestor at request time] is off;
                    // no harm could be done since this request doesn't change the resource
                    leaseRequest.copyVersion(leaseCurrent);
                    fReject = !performLock(msgRequest);
                    }
                break;
        
            case Lease.LEASE_LOCKED:
            case Lease.LEASE_DIRTY:
                if (leaseCurrent.getIssuerId() != nThisId)
                    {
                    // the issuer has changed
                    fReject = true;
                    }
                else if (
                    leaseCurrent.getHolderId()       == msgRequest.getFromMember().getId() &&
                    leaseCurrent.getHolderThreadId() == leaseRequest.getHolderThreadId())
                    {
                    fReject = !performLock(msgRequest);
                    }
                else
                    {
                    // someone else is holding the lock; queue the request if necessary
                    long lWaitTimeout = msgRequest.getWaitTimeout();
                    long lTime        = getClusterTime();
                    long lVariance    = getClusterTimeVariance();
        
                    if (lTime + lVariance < lWaitTimeout)
                        {
                        // we are queueing the lock request even if the lease version
                        // [as known by the requestor at request time] is off;
                        // the requestor will be updated when a lock is granted
                        getLockRequestQueue().add(msgRequest);
        
                        long lExpirationTime = leaseCurrent.getExpirationTime();
                        if (lWaitTimeout != Long.MAX_VALUE || lExpirationTime != Long.MAX_VALUE)
                            {
                            scheduleLockEvaluation(
                                Math.min(lWaitTimeout - lTime, lExpirationTime - lTime));
                            }
                        }
                    else
                        {
                        fReject = true;
                        }
                    }
                break;
        
            default:
                throw new IllegalStateException();
            }
        
        if (fReject)
            {
            _trace("Rejecting lock: " + leaseCurrent +
                 "\n by member=" + msgRequest.getFromMember().getId() +
                 ", " + leaseRequest, 4);
            notifyLease(leaseCurrent, msgRequest, null);
            }
        }
    
    /**
     * Process the remove request. This method is called on the service thread
    * and should not be called externally.
    * 
    * @param msgRequest the request message
     */
    public void onLeaseRemoveRequest(ReplicatedCache.LeaseRemoveRequest msgRequest)
        {
        // import Component.Net.Member;
        
        Lease  leaseRequest = msgRequest.getLease();
        Object oKey         = leaseRequest.getResourceKey();
        int    iCache       = leaseRequest.getCacheIndex();
        Lease  leaseCurrent = ensureCacheHandler(iCache).ensureLease(oKey);
        int    nThisId      = getThisMember().getId();
        
        leaseCurrent.validate();
        
        switch (leaseCurrent.getStatus())
            {
            case Lease.LEASE_UNISSUED:
                // assume that the issue is on its way
                leaseCurrent.setIssuerId(nThisId);
                // break through
            case Lease.LEASE_AVAILABLE:
                if (leaseCurrent.getIssuerId() != nThisId ||
                    !performRemove(msgRequest))
                    {
                    notifyLease(leaseCurrent, msgRequest, null);
                    }
                break;
        
            case Lease.LEASE_LOCKED:
            case Lease.LEASE_DIRTY:
                if (leaseCurrent.getIssuerId() != nThisId ||
                    leaseCurrent.getHolderId() != msgRequest.getFromMember().getId() ||
                    !performRemove(msgRequest))
                    {
                    notifyLease(leaseCurrent, msgRequest, null);
                    }
                break;
        
            default:
                throw new IllegalStateException();
            }
        }
    
    /**
     * Process the unlock request. This method is called on the service thread
    * and should not be called externally.
    * 
    * @param msgRequest the request message
     */
    public void onLeaseUnlockRequest(ReplicatedCache.LeaseUnlockRequest msgRequest)
        {
        // import Component.Util.CacheHandler as com.tangosol.coherence.component.util.CacheHandler;
        // import com.tangosol.util.Base;
        // import java.util.Iterator;
        // import java.util.List;
        
        Lease        leaseRequest = msgRequest.getLease();
        Object       oKey         = leaseRequest.getResourceKey();
        int          iCache       = leaseRequest.getCacheIndex();
        com.tangosol.coherence.component.util.CacheHandler handler      = ensureCacheHandler(iCache);
        Lease        leaseCurrent = handler.ensureLease(oKey);
        int          nThisId      = getThisMember().getId();
        boolean      fReject      = false;
        
        leaseCurrent.validate();
        
        switch (leaseCurrent.getStatus())
            {
            case Lease.LEASE_UNISSUED:
                // assume that the issue is on its way
                leaseCurrent.setIssuerId(nThisId);
                // break through
            case Lease.LEASE_AVAILABLE:
                fReject = leaseCurrent.getIssuerId() != nThisId ||
                          !performUnlock(msgRequest);
                break;
        
            case Lease.LEASE_LOCKED:
            case Lease.LEASE_DIRTY:
                if (leaseCurrent.getIssuerId() != nThisId)
                    {
                    // the issuer has changed
                    fReject = true;
                    }
                else if (
                    leaseCurrent.getHolderId()       == msgRequest.getFromMember().getId() &&
                    leaseCurrent.getHolderThreadId() == leaseRequest.getHolderThreadId())
                    {
                    fReject = !performUnlock(msgRequest);
                    }
                else
                    {
                    // check the request queue and remove pending lock requests
                    // by the same requestor (member/thread)
                    List listPending = getLockRequestQueue();
                    for (Iterator iter = listPending.iterator(); iter.hasNext();)
                        {
                        ReplicatedCache.LeaseLockRequest msgLockRequest = (ReplicatedCache.LeaseLockRequest) iter.next();
        
                        Lease leaseLockRequest = msgLockRequest.getLease();
                        if (leaseLockRequest.getCacheIndex()     == iCache       &&
                            Base.equals(leaseLockRequest.getResourceKey(), oKey) &&
                            leaseLockRequest.getHolderId()       == msgRequest.getFromMember().getId() &&
                            leaseLockRequest.getHolderThreadId() == leaseRequest.getHolderThreadId())
                            {
                            iter.remove();
                            notifyLease(leaseCurrent, msgLockRequest, null);
                            }
                        }
                    fReject = true;
                    }
                break;
        
            default:
                throw new IllegalStateException();
            }
        
        if (fReject)
            {
            _trace("Rejecting unlock: " + leaseCurrent +
                 "\n by member=" + msgRequest.getFromMember().getId() +
                 ", " + leaseRequest, 4);
            notifyLease(leaseCurrent, msgRequest, null);
            }
        }
    
    /**
     * Process the update request. This method is called on the service thread
    * and should not be called externally.
    * 
    * @param msgRequest the request message
    * @param oResource the resource value
     */
    public void onLeaseUpdateRequest(ReplicatedCache.LeaseUpdateRequest msgRequest, Object oResource, com.tangosol.util.Binary binResource)
        {
        // import Component.Net.Member;
        
        Lease   leaseRequest = msgRequest.getLease();
        Object  oKey         = leaseRequest.getResourceKey();
        int     iCache       = leaseRequest.getCacheIndex();
        Lease   leaseCurrent = ensureCacheHandler(iCache).ensureLease(oKey);
        int     nThisId      = getThisMember().getId();
        boolean fReject      = false;
        
        leaseCurrent.validate();
        
        switch (leaseCurrent.getStatus())
            {
            case Lease.LEASE_UNISSUED:
                // assume that the issue is on its way
                // or this is a very first put (see updateResource())
                int nIssuerId = leaseRequest.getIssuerId();
                leaseCurrent.setIssuerId(nIssuerId == 0 ? nThisId : nIssuerId);
                // break through
            case Lease.LEASE_AVAILABLE:
                {
                fReject = leaseCurrent.getIssuerId() != nThisId ||
                          !performUpdate(msgRequest, oResource, binResource);
                break;
                }
        
            case Lease.LEASE_LOCKED:
            case Lease.LEASE_DIRTY:
                fReject = leaseCurrent.getIssuerId() != nThisId ||
                          leaseCurrent.getHolderId() != msgRequest.getFromMember().getId() ||
                          !performUpdate(msgRequest, oResource, binResource);
                break;
        
            default:
                throw new IllegalStateException();
            }
        
        if (fReject)
            {
            _trace("Rejecting update: " + leaseCurrent +
                "\n by member=" + msgRequest.getFromMember().getId() +
                ", " + leaseRequest, 4);
            notifyLease(leaseCurrent, msgRequest, null);
            }
        }
    
    // Declared at the super level
    /**
     * Event notification to perform a regular daemon activity. To get it
    * called, another thread has to set Notification to true:
    * <code>daemon.setNotification(true);</code>
    * 
    * @see #onWait
     */
    protected void onNotify()
        {
        // import com.tangosol.util.Base;
        
        super.onNotify();
        
        if (isRunning())
            {
            if (Base.getSafeTimeMillis() >= getLockingNextMillis())
                {
                evaluateLockRequests();
                }
            }
        }
    
    // Declared at the super level
    /**
     * Called to complete the "service-left" processing for the specified
    * member.  This notification is processed only after the associated
    * endpoint has been released by the message handler.  See
    * $NotifyServiceLeft#onReceived/#proceed.
    * Called on the service thread only.
    * Update the resource maps due to departure of the specified member. This
    * method is called on the ReplicatedCache service thread and should not be
    * called externally.
    * 
    * @param member the member that has left
     */
    public void onNotifyServiceLeft(com.tangosol.coherence.component.net.Member member)
        {
        // import Component.Util.CacheHandler as com.tangosol.coherence.component.util.CacheHandler;
        // import java.util.List;
        // import java.util.Iterator;
        
        super.onNotifyServiceLeft(member);
        
        com.tangosol.coherence.component.util.CacheHandler[] aHandler = getCacheHandler();
        for (int i = 0, c = aHandler.length; i < c; i++)
            {
            com.tangosol.coherence.component.util.CacheHandler handler = aHandler[i];
            if (handler != null && handler.isValid())
                {
                handler.onFarewell(member);
                }
            }
        
        evaluateLockRequests();
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
            ReplicatedCache.BackingMapContext ctx = getBackingMapContext();
            ctx.setManager(manager);
        
            manager.init(ctx);
            }
        
        setAcceptingOthers(true);
        notifyServiceJoined();
        
        // do not call super because that will signal that clients are
        // now able to call in; only after all other Members have welcomed
        // this Member will the startup be complete
        // (see ReplicatedCache.CacheUpdateRequest.Poll.onCompletion)
        requestCacheUpdate();
        }
    
    /**
     * Perform the lock. This method is called on the service thread and should
    * not be called externally.
    * 
    * @param msgRequest  the request message
     */
    protected boolean performLock(ReplicatedCache.LeaseLockRequest msgRequest)
        {
        // import Component.Net.MemberSet;
        // import com.tangosol.run.component.EventDeathException;
        
        Lease lease        = msgRequest.getLease();
        long  cLeaseMillis = msgRequest.getLeaseMillis();
        
        lease.incrementLeaseVersion();
        lease.setExpirationTime(adjustWaitTime(
            cLeaseMillis - 1L, TIME_CLUSTER)); // zero means indefinite lease duration
        
        try
            {
            ensureCacheHandler(lease.getCacheIndex()).onLeaseUpdate(lease, false, null, 0L);
            }
        catch (EventDeathException e)
            {
            return false;
            }
        
        // inform everyone, but the applicant
        MemberSet setOther = getOthersMemberSet();
        setOther.remove(msgRequest.getFromMember());
        
        if (!setOther.isEmpty())
            {
            ReplicatedCache.LeaseLock msg = (ReplicatedCache.LeaseLock) instantiateMessage("LeaseLock");
            msg.setToMemberSet(setOther);
            msg.setLease(lease);
        
            post(msg);
            }
        
        // inform the applicant
        notifyLease(lease, msgRequest,
            (ReplicatedCache.LeaseLock) instantiateMessage("LeaseLock"));
        
        return true;
        }
    
    /**
     * Perform the remove. This method is called on the service thread and
    * should not be called externally.
    * 
    * @param msgRequest  the request message
     */
    protected boolean performRemove(ReplicatedCache.LeaseRemoveRequest msgRequest)
        {
        // import Component.Net.MemberSet;
        // import com.tangosol.run.component.EventDeathException;
        
        Lease lease = msgRequest.getLease();
        
        lease.incrementResourceVersion();
        
        try
            {
            ensureCacheHandler(lease.getCacheIndex()).onLeaseRemove(lease);
            }
        catch (EventDeathException e)
            {
            return false;
            }
        
        // inform everyone, but the applicant
        MemberSet setOther = getOthersMemberSet();
        setOther.remove(msgRequest.getFromMember());
        
        if (!setOther.isEmpty())
            {
            ReplicatedCache.LeaseRemove msg = (ReplicatedCache.LeaseRemove) instantiateMessage("LeaseRemove");
            msg.setToMemberSet(setOther);
            msg.setLease(lease);
        
            post(msg);
            }
        
        // inform the applicant
        notifyLease(lease, msgRequest,
            (ReplicatedCache.LeaseRemove) instantiateMessage("LeaseRemove"));
        
        return true;
        }
    
    /**
     * Perform the unlock. This method is called on the service thread and
    * should not be called externally.
    * 
    * @param msgRequest  the request message
     */
    protected boolean performUnlock(ReplicatedCache.LeaseUnlockRequest msgRequest)
        {
        // import Component.Net.MemberSet;
        // import com.tangosol.run.component.EventDeathException;
        
        Lease lease = msgRequest.getLease();
        
        lease.unlock();
        lease.incrementLeaseVersion();
        
        try
            {
            ensureCacheHandler(lease.getCacheIndex()).onLeaseUpdate(lease, false, null, 0L);
            }
        catch (EventDeathException e)
            {
            return false;
            }
        
        // inform everyone, but the applicant
        MemberSet setOther = getOthersMemberSet();
        setOther.remove(msgRequest.getFromMember());
        
        if (!setOther.isEmpty())
            {
            ReplicatedCache.LeaseUnlock msg = (ReplicatedCache.LeaseUnlock) instantiateMessage("LeaseUnlock");
            msg.setToMemberSet(setOther);
            msg.setLease(lease);
        
            post(msg);
            }
        
        // inform the applicant
        notifyLease(lease, msgRequest,
            (ReplicatedCache.LeaseUnlock) instantiateMessage("LeaseUnlock"));
        
        firePendingLocks(lease);
        
        return true;
        }
    
    /**
     * Perform the update. This method is called on the service thread and
    * should not be called externally.
    * 
    * @param msgRequest  the request message
     */
    protected boolean performUpdate(ReplicatedCache.LeaseUpdateRequest msgRequest, Object oResource, com.tangosol.util.Binary binResource)
        {
        // import Component.Net.MemberSet;
        // import com.tangosol.run.component.EventDeathException;
        
        Lease lease   = msgRequest.getLease();
        long  cExpiry = msgRequest.getResourceExpiry();
        
        lease.incrementResourceVersion();
        
        try
            {
            ensureCacheHandler(lease.getCacheIndex()).
                onLeaseUpdate(lease, true, oResource, cExpiry);
            }
        catch (EventDeathException e)
            {
            return false;
            }
        
        // inform everyone, but the applicant
        MemberSet setOther = getOthersMemberSet();
        setOther.remove(msgRequest.getFromMember());
        
        if (!setOther.isEmpty())
            {
            ReplicatedCache.LeaseUpdate msg = (ReplicatedCache.LeaseUpdate) instantiateMessage("LeaseUpdate");
            msg.setToMemberSet(setOther);
            msg.setLease(lease);
            msg.setResource(oResource);
            msg.setResourceBinary(binResource);
            msg.setResourceExpiry(cExpiry);
            post(msg);
            }
        
        // inform the applicant
        ReplicatedCache.LeaseUpdate msgResponse = (ReplicatedCache.LeaseUpdate) instantiateMessage("LeaseUpdate");
        msgResponse.setResource(oResource);
        msgResponse.setResourceBinary(binResource);
        msgResponse.setResourceExpiry(cExpiry);
        notifyLease(lease, msgRequest, msgResponse);
        
        return true;
        }
    
    // From interface: com.tangosol.net.CacheService
    public void releaseCache(com.tangosol.net.NamedCache map)
        {
        // import Component.Util.CacheHandler as com.tangosol.coherence.component.util.CacheHandler;
        
        _assert(map instanceof com.tangosol.coherence.component.util.CacheHandler, "Invalid map");
        
        ReplicatedCache.CacheHandler handler  = (ReplicatedCache.CacheHandler) map;
        com.tangosol.coherence.component.util.CacheHandler  hCatalog = getCacheHandler(0);
        
        synchronized (hCatalog)
            {
            int             iCache  = handler.getCacheIndex();
            com.tangosol.coherence.component.util.CacheHandler[] aHandler = getCacheHandler();
            
            for (com.tangosol.coherence.component.util.CacheHandler hNext = aHandler[iCache], hPrev = null; hNext != null;)
                {
                if (hNext == handler)
                    {
                    hNext = hNext.getNextHandler();
        
                    // unlink this handler from the chain
                    // unless it's the last one
                    if (hPrev == null)
                        {
                        if (hNext != null)
                            {
                            aHandler[iCache] = hNext;
                            }
                        }
                    else
                        {
                        hPrev.setNextHandler(hNext);
                        }
                    handler.releaseIndexMap();
                    handler.releaseClassLoader();
                    return;
                    }
        
                hPrev = hNext;
                hNext = hNext.getNextHandler();
                }
        
            // the handler has already been released
            }
        }
    
    /**
     * Remove the specified resource. For this operation to succed, this member
    * of the Cluster must hold a current Lease for this resource or must be
    * able to obtain one. When this method returns, this member no longer holds
    * the Lease for the specified resource. This method should only be called
    * on a client's thread.
    * 
    * @param handler  the cache handler
    * @param oKey  the resource key
    * @param fReturn  if true, the return value is required; otherwise it will
    * be ignored
    * 
    * @return previous resource value
    * 
    * @exception RuntimeException  if the Lease for the resource cannot be
    * obtained
     */
    public Object removeResource(com.tangosol.coherence.component.util.CacheHandler handler, Object oKey, boolean fReturn)
            throws java.util.ConcurrentModificationException
        {
        // import Component.Net.Member;
        // import Component.Util.CacheHandler as com.tangosol.coherence.component.util.CacheHandler;
        // import java.util.ConcurrentModificationException;
        
        com.tangosol.coherence.component.util.CacheHandler hCatalog   = getCacheHandler(0);
        String       sCacheName = handler.getCacheName();
        
        while (true)
            {
            // make sure that the cache is not locked
            Lease leaseCache = hCatalog.getLease(sCacheName);
        
            if (leaseCache != null && getThreadStatus(leaseCache) == Lease.LEASE_DIRTY)
                {
                throw new ConcurrentModificationException("Cache \"" + sCacheName +
                    "\" is locked by another thread or member: " + leaseCache);
                }
        
            Lease lease = handler.ensureLease(oKey);
        
            switch (getThreadStatus(lease))
                {
                case Lease.LEASE_UNISSUED:
                    requestIssue(lease);
        
                    // if the lease was removed while issuing, try again;
                    // if the lease was issued, this will do as well
                    break;
        
                case Lease.LEASE_AVAILABLE:
                    {
                    Member memberIssuer = getServiceMemberSet().getMember(lease.getIssuerId());
                    if (memberIssuer == null)
                        {
                        // the issuer is gone, try again
                        break;
                        }
                    Object oValue = fReturn ? handler.getCachedResource(oKey) : null;
        
                    if (requestRemove(lease, memberIssuer))
                        {
                        return oValue;
                        }
                    // the issuer is gone, try again
                    break;
                    }
        
                case Lease.LEASE_LOCKED:
                    {
                    Member memberIssuer = getServiceMemberSet().getMember(lease.getIssuerId());
                    if (memberIssuer == null)
                        {
                        // the issuer is gone, this member should become one
                        memberIssuer = getThisMember();
                        }
        
                    Object oValue = fReturn ? handler.getCachedResource(oKey) : null;
        
                    if (requestRemove(lease, memberIssuer))
                        {
                        return oValue;
                        }
                    // the issuer is gone, try again
                    break;
                    }
        
                case Lease.LEASE_DIRTY:
                    throw new ConcurrentModificationException("Cache \"" + sCacheName +
                        "\": Resource is locked by another thread or member: " + lease);
        
                default:
                    throw new IllegalStateException();
                }
            }
        }
    
    /**
     * Request the cache update from the existing service members and is used
    * during startup. This method is called on the service thread and should
    * not be called externally.
     */
    public void requestCacheUpdate()
        {
        // import Component.Net.MemberSet;
        // import Component.Net.MemberSet.ActualMemberSet.ServiceMemberSet;
        // import Component.Net.MemberSet.DependentMemberSet;
        
        // we should send the request to members that have not joined yet;
        // service members that are still in the MemberWelcomeRequest protocol
        // may not be able to process the (non-internal) "CacheUpdateRequest" message
        // while waiting on a response from this node - thus causing a deadlock
        
        MemberSet setOther = getOthersMemberSet(ServiceMemberSet.MEMBER_JOINED);
        if (setOther.isEmpty())
            {
            // there is no one but this member
            // Note: we could also use "if (getServiceOldestMember() == getThisMember())
            setAcceptingClients(true);
            }
        else
            {
            DependentMemberSet setUpdating = new DependentMemberSet();
            setUpdating.setBaseSet(setOther);
            setUpdatingMemberSet(setUpdating);
        
            ReplicatedCache.CacheUpdateRequest msg =
                (ReplicatedCache.CacheUpdateRequest) instantiateMessage("CacheUpdateRequest");
            msg.setToMemberSet(setOther);
            post(msg);
            }
        }
    
    /**
     * Request a lease issue
    * 
    * @param lease the lease
    * 
    * @return true if the request has been successfully delivered; false
    * otherwise
     */
    protected boolean requestIssue(com.tangosol.coherence.component.net.Lease lease)
        {
        checkStatus();
        
        ReplicatedCache.LeaseIssueRequest msg =
            (ReplicatedCache.LeaseIssueRequest) instantiateMessage("LeaseIssueRequest");
        msg.addToMember(getServiceOldestMember());
        msg.setLease(lease);
        
        poll(msg);
        
        return msg.getRequestPoll().getLeftMemberSet().isEmpty();
        }
    
    /**
     * Request a lease lock
    * 
    * @param lease the lease
    * @param memberIssuer the issuer
    * @param cLeaseMillis the lease duration
    * @param cWaitMillis  the maximum time to wait in milliseconds; if zero
    * return right away; if negative, wait indefinitely
    * @param fRenew  true if the lock is currently held by the calling thread
    * and the effective time of the lease shold be let alone; otherwise it is a
    * new lock and the effective time should be reset
    * 
    * @return true if the request has been successfully delivered and the lock
    * was obtained; false otherwise
     */
    protected boolean requestLock(com.tangosol.coherence.component.net.Lease lease, com.tangosol.coherence.component.net.Member memberIssuer, long cLeaseMillis, long cWaitMillis, boolean fRenew)
        {
        // import Component.Net.MemberSet.SingleMemberSet;
        
        checkStatus();
        
        long lCurrentTime = getClusterTime();
        
        // (LeaseMillis == 0) means indefinite lease duration
        long lExpirationTime = adjustWaitTime(cLeaseMillis - 1L, lCurrentTime);
        
        if (fRenew && lExpirationTime <= lease.getExpirationTime())
            {
            // current lock covers the request; no action required
            return true;
            }
        
        ReplicatedCache.LeaseLockRequest msg =
            (ReplicatedCache.LeaseLockRequest) instantiateMessage("LeaseLockRequest");
        msg.setToMemberSet(SingleMemberSet.instantiate(memberIssuer));
        msg.setWaitMillis(cWaitMillis);
        msg.setLeaseMillis(cLeaseMillis);
        msg.setLease(lease);
        
        Lease leaseMsg  = msg.getLease();
        long  lThreadId = getReplicatedCacheDependencies().getLeaseGranularity() == Lease.BY_THREAD ?
            Lease.getCurrentThreadId() : 0L;
        
        leaseMsg.setIssuerId(memberIssuer.getId());
        leaseMsg.setHolderId(getThisMember().getId());
        leaseMsg.setHolderThreadId(lThreadId);
        if (!fRenew)
            {
            leaseMsg.setEffectiveTime(lCurrentTime);
            }
        
        try
            {
            long cTimeout = getRequestTimeout();
            if (cTimeout > 0L)
                {
                cTimeout = adjustWaitTime(cWaitMillis, cTimeout);
                }
            poll(msg, cTimeout);
        
            return msg.getRequestPoll().getLeftMemberSet().isEmpty() &&
                   getThreadStatus(lease) == Lease.LEASE_LOCKED;
            }
        catch (RuntimeException e)
            {
            // the most probable cause of the exception is the thread interrupt;
            // since we cannot know whether or not the lock would have been acquired,
            // request the unlock unconditionally
            boolean fInterrupt = Thread.interrupted();
            try
                {
                requestUnlock(leaseMsg, memberIssuer, false);
                }
            catch (RuntimeException x) {}
        
            if (fInterrupt)
                {
                Thread.currentThread().interrupt();
                }
            throw e;
            }
        }
    
    /**
     * Request a lease removal
    * 
    * @param lease the lease
    * @param memberIssuer the issuer
    * 
    * @return true if the request has been successfully delivered; false
    * otherwise
     */
    protected boolean requestRemove(com.tangosol.coherence.component.net.Lease lease, com.tangosol.coherence.component.net.Member memberIssuer)
        {
        // import Component.Net.MemberSet.SingleMemberSet;
        
        checkStatus();
        
        ReplicatedCache.LeaseRemoveRequest msg =
            (ReplicatedCache.LeaseRemoveRequest) instantiateMessage("LeaseRemoveRequest");
        msg.setToMemberSet(SingleMemberSet.instantiate(memberIssuer));
        msg.setLease(lease);
        
        poll(msg);
        
        return msg.getRequestPoll().getLeftMemberSet().isEmpty();
        }
    
    /**
     * Request a lease unlocking
    * 
    * @param lease the lease
    * @param memberIssuer the issuer
    * @param fWait true is the request should be synchronous (poll); false
    * otherwise
    * 
    * @return true if the request has been successfully delivered; false
    * otherwise
     */
    protected boolean requestUnlock(com.tangosol.coherence.component.net.Lease lease, com.tangosol.coherence.component.net.Member memberIssuer, boolean fWait)
        {
        // import Component.Net.MemberSet.SingleMemberSet;
        
        checkStatus();
        
        ReplicatedCache.LeaseUnlockRequest msg =
            (ReplicatedCache.LeaseUnlockRequest) instantiateMessage("LeaseUnlockRequest");
        msg.setToMemberSet(SingleMemberSet.instantiate(memberIssuer));
        msg.setLease(lease);
        
        Lease leaseMsg  = msg.getLease();
        long  lThreadId = getReplicatedCacheDependencies().getLeaseGranularity() == Lease.BY_THREAD ?
            Lease.getCurrentThreadId() : 0L;
        
        leaseMsg.setIssuerId(memberIssuer.getId());
        leaseMsg.setHolderId(getThisMember().getId());
        leaseMsg.setHolderThreadId(lThreadId);
        
        if (fWait)
            {
            poll(msg);
            }
        else
            {
            send(msg);
            }
        
        return msg.getRequestPoll().getLeftMemberSet().isEmpty();
        }
    
    /**
     * Request a lease update
    * 
    * @param lease the lease
    * @param memberIssuer the issuer
    * @param oResource the resource value
    * @param lResourceExpire the resource expiry millis
    * @param fResetEffective if true, the effective time of the lease should be
    * reset; otherwise let alone
    * @param fResetExpire  if true,  the expiration time of the lease should be
    * reset; otherwise let alone
    * 
    * @return true if the request has been successfully delivered; false
    * otherwise
     */
    protected boolean requestUpdate(com.tangosol.coherence.component.net.Lease lease, com.tangosol.coherence.component.net.Member memberIssuer, Object oResource, long lResourceExpiry, boolean fResetEffective, boolean fResetExpire)
        {
        // import Component.Net.MemberSet.SingleMemberSet;
        // import com.tangosol.util.Binary;
        // import com.tangosol.util.ExternalizableHelper;
        checkStatus();
        
        ReplicatedCache.LeaseUpdateRequest msg =
            (ReplicatedCache.LeaseUpdateRequest) instantiateMessage("LeaseUpdateRequest");
        msg.setToMemberSet(SingleMemberSet.instantiate(memberIssuer));
        msg.setLease(lease);
        msg.setResource(oResource);
        msg.setResourceExpiry(lResourceExpiry);
        
        // if there is more than one member running the service the resource will need
        // to be serialized in order to be communicated to other members, and it's 
        // preferrable to serialize it now so that a) any exceptions that may occur as
        // part of serialization will occur on the current (the caller's) thread
        // instead of on the service thread; b) the service will scale better when
        // running on multi-processor machines
        if (getServiceMemberSet().size() > 1)
            {
            msg.setResourceBinary(oResource instanceof Binary ?
                (Binary) oResource : ExternalizableHelper.toBinary(oResource, ensureSerializer()));
            }
        
        Lease leaseMsg = msg.getLease();
        
        leaseMsg.setIssuerId(memberIssuer.getId());
        if (fResetEffective)
            {
            leaseMsg.setEffectiveTime(getClusterTime());
            }
        if (fResetExpire)
            {
            leaseMsg.setExpirationTime(getClusterTime());
            }
        
        poll(msg);
        
        return msg.getRequestPoll().getLeftMemberSet().isEmpty();
        }
    
    /**
     * Schedule the pending locks evaluation to perform no later then in
    * specified number of milliseconds. This call has no effect if the
    * evaluation is already scheduled to an earlier time.
     */
    protected synchronized void scheduleLockEvaluation(long cMillis)
        {
        // import com.tangosol.util.Base;
        
        long lNext = cMillis == Long.MAX_VALUE ?
            cMillis : Base.getSafeTimeMillis() + cMillis;
        setLockingNextMillis(Math.min(getLockingNextMillis(), lNext));
        }
    
    // Declared at the super level
    /**
     * Setter for property AcceptingClients.<p>
    * Set to true when the Service has advanced to the state at which it can
    * accept requests from client threads.
     */
    public void setAcceptingClients(boolean fAccepting)
        {
        // import Component.Net.MemberSet;
        // import Component.Net.MemberSet.EmptyMemberSet;
        
        setUpdatingMemberSet(fAccepting ?
            getServiceMemberSet() : (MemberSet) EmptyMemberSet.get_Instance());
        
        super.setAcceptingClients(fAccepting);
        }
    
    // Accessor for the property "BackingMapContext"
    /**
     * Setter for property BackingMapContext.<p>
    * The BackingMapContext (lazily created) is used by the BackingMapManager
    * (if provided).
     */
    protected void setBackingMapContext(ReplicatedCache.BackingMapContext ctx)
        {
        __m_BackingMapContext = ctx;
        }
    
    // From interface: com.tangosol.net.CacheService
    // Accessor for the property "BackingMapManager"
    /**
     * Setter for property BackingMapManager.<p>
    * Interface that provides the backing map storage implementations for the
    * cache.
     */
    public synchronized void setBackingMapManager(com.tangosol.net.BackingMapManager manager)
        {
        if (isRunning())
            {
            throw new IllegalStateException("Service is already running");
            }
        
        __m_BackingMapManager = (manager);
        }
    
    // Accessor for the property "CacheHandler"
    /**
     * Setter for property CacheHandler.<p>
    * The array of CacheHandlers. It contains at least one element (at the
    * reserved index zero) which is a handler with a reserved name managing the
    * coherently shared map (referred to as the "Cache Catalog") holding the
    * configuration info (such as name, index, etc.) for all the cache holders
    * keyed by the cache name.
     */
    protected void setCacheHandler(com.tangosol.coherence.component.util.CacheHandler[] aHandler)
        {
        __m_CacheHandler = aHandler;
        }
    
    // Accessor for the property "CacheHandler"
    /**
     * Setter for property CacheHandler.<p>
    * The array of CacheHandlers. It contains at least one element (at the
    * reserved index zero) which is a handler with a reserved name managing the
    * coherently shared map (referred to as the "Cache Catalog") holding the
    * configuration info (such as name, index, etc.) for all the cache holders
    * keyed by the cache name.
     */
    protected void setCacheHandler(int iCache, com.tangosol.coherence.component.util.CacheHandler handler)
        {
        // import Component.Util.CacheHandler as com.tangosol.coherence.component.util.CacheHandler;
        
        com.tangosol.coherence.component.util.CacheHandler hCatalog = getCacheHandler(0);
        
        synchronized (hCatalog)
            {
            com.tangosol.coherence.component.util.CacheHandler[] aHandler      = getCacheHandler();
            boolean        fBeyondBounds = aHandler == null || iCache >= aHandler.length;
        
            if (handler != null && fBeyondBounds)
                {
                // resize, making the array at least two spots large
                // than is necessary
                com.tangosol.coherence.component.util.CacheHandler[] aHandlerNew = new com.tangosol.coherence.component.util.CacheHandler[iCache + 2];
        
                // copy original handlers
                if (aHandler != null)
                    {
                    System.arraycopy(aHandler, 0, aHandlerNew, 0, aHandler.length);
                    }
        
                aHandler = aHandlerNew;
                setCacheHandler(aHandler);
        
                fBeyondBounds = false;
                }
        
                
            if (!fBeyondBounds)
                {
                aHandler[iCache] = handler;
                }
            }
        }
    
    // Accessor for the property "LockingNextMillis"
    /**
     * Setter for property LockingNextMillis.<p>
    * The LockingNextMillis value is the time (in local system millis) at which
    * the next deferred lock evaluation will be performed.
    * 
    * Initial value is Long.MAX_VALUE.
     */
    public void setLockingNextMillis(long ltMillis)
        {
        __m_LockingNextMillis = ltMillis;
        }
    
    // Accessor for the property "LockRequestQueue"
    /**
     * Setter for property LockRequestQueue.<p>
    * The queue of pending LockRequest messages.
     */
    protected void setLockRequestQueue(java.util.List list)
        {
        __m_LockRequestQueue = list;
        }
    
    // Accessor for the property "UpdatingMemberSet"
    /**
     * Setter for property UpdatingMemberSet.<p>
    * This member set contains the service members of the cluster that this
    * service is allowed to listen to. This set may not be equal to the
    * ServiceMemberSet only during the service initialization when this member
    * is welcomed (updated) by others. As soon as another service member
    * welcomes this service it gets included into the UpdatingMemberSett
    * allowing all the incoming LeaseMessages to be processed.
     */
    public void setUpdatingMemberSet(com.tangosol.coherence.component.net.MemberSet setMember)
        {
        __m_UpdatingMemberSet = setMember;
        }
    
    // From interface: com.tangosol.net.CacheService
    // Declared at the super level
    /**
     * Starts the daemon thread associated with this component. If the thread is
    * already starting or has started, invoking this method has no effect.
    * 
    * Synchronization is used here to verify that the start of the thread
    * occurs; the lock is obtained before the thread is started, and the daemon
    * thread notifies back that it has started from the run() method.
     */
    public synchronized void start()
        {
        // import Component.Net.MemberSet;
        // import Component.Net.MemberSet.EmptyMemberSet;
        // import Component.Util.CacheHandler as com.tangosol.coherence.component.util.CacheHandler;
        
        com.tangosol.coherence.component.util.CacheHandler[] aHandler = new com.tangosol.coherence.component.util.CacheHandler[2];
        com.tangosol.coherence.component.util.CacheHandler   hCatalog = (com.tangosol.coherence.component.util.CacheHandler) _findChild("CatalogHandler");
        
        ClassLoader loader = getContextClassLoader();
        if (loader == null)
            {
            loader = Thread.currentThread().getContextClassLoader();
            if (loader == null)
                {
                loader = getClass().getClassLoader();
                _assert(loader != null, "ContextClassLoader is missing");
                }
            setContextClassLoader(loader);
            }
        
        hCatalog.setClassLoader(loader);
        aHandler[0] = hCatalog;
        setCacheHandler(aHandler);
        
        setUpdatingMemberSet((MemberSet) EmptyMemberSet.get_Instance());
        
        super.start();
        }
    
    /**
     * Abandon a Lease for the specified resource. This method should only be
    * called on a client's thread.
    * 
    * @param handler  the cache handler
    * @param oKey  the resource key
    * 
    * @return true if the resource was successfully unlocked; false otherwise
     */
    public boolean unlockResource(com.tangosol.coherence.component.util.CacheHandler handler, Object oKey)
        {
        // import Component.Net.Member;
        // import Component.Util.CacheHandler as com.tangosol.coherence.component.util.CacheHandler;
        // import com.tangosol.util.ConcurrentMap;
        
        if (oKey == ConcurrentMap.LOCK_ALL)
            {
            com.tangosol.coherence.component.util.CacheHandler hCatalog   = getCacheHandler(0);
            String       sCacheName = handler.getCacheName();
        
            return unlockResource(hCatalog, sCacheName);
            }
        
        boolean fWasLocked = false;
        while (true)
            {
            Lease lease = handler.getLease(oKey);
            if (lease == null)
                {
                return true;
                }
        
            switch (getThreadStatus(lease))
                {
                case Lease.LEASE_UNKNOWN:
                case Lease.LEASE_UNISSUED:
                case Lease.LEASE_AVAILABLE:
                    return true;
        
                case Lease.LEASE_LOCKED:
                    {
                    Member memberIssuer = getServiceMemberSet().getMember(lease.getIssuerId());
                    if (memberIssuer == null)
                        {
                        // the issuer is gone, this member should become one
                        memberIssuer = getThisMember();
                        }
        
                    if (requestUnlock(lease, memberIssuer, true))
                        {
                        return true;
                        }
                    // has not unlocked, try again
                    fWasLocked = true;
                    break;
                    }
        
                case Lease.LEASE_DIRTY:
                    return fWasLocked;
        
                default:
                    throw new IllegalStateException();
                }
            }
        }
    
    /**
     * Update the specified resource with the specified value. If the fFInal
    * flag is true, the resource should be immediately unlocked - this
    * operation is an optimization that allows to combine two network operation
    * (update and unlock).  For these operation to succed, this member of the
    * Cluster must hold or be able to get a Lease for the resource. This method
    * should only be called on a client's thread.
    * 
    * @param handler  the cache handler
    * @param oKey  the resource key
    * @param oValue  the resource value (Serializable or XmlSerializable); may
    * be null
    * @param cMillis  the number of milliseconds until the cache entry will
    * expire; pass 0 to use the cache's default setting; pass -1 to indicate
    * that the cache entry should never expire
    * @param fFinal specifies whether the lease for the resource should be
    * immediately unlocked
    * @param fReturn  if true, the return value is required; otherwise it will
    * be ignored
    * 
    * @return previous resource value
    * 
    * @exception IllegalArgumentException  if the value is neither Serializable
    * nor XmlSerializable
    * @exception ConcurrentModificationException  if the Lease for the resource
    * cannot be obtained
    * @throws UnsupportedOperationException if the requested expiry is a
    * positive value and the implementation does not support expiry of cache
    * entries
     */
    public Object updateResource(com.tangosol.coherence.component.util.CacheHandler handler, Object oKey, Object oValue, long cExpireMillis, boolean fFinal, boolean fReturn)
            throws java.util.ConcurrentModificationException
        {
        // import Component.Net.Member;
        // import Component.Util.CacheHandler as com.tangosol.coherence.component.util.CacheHandler;
        // import java.util.ConcurrentModificationException;
        
        com.tangosol.coherence.component.util.CacheHandler hCatalog   = getCacheHandler(0);
        String       sCacheName = handler.getCacheName();
        
        while (true)
            {
            // make sure that the cache is not locked
            Lease leaseCache = hCatalog.getLease(sCacheName);
        
            if (leaseCache != null && getThreadStatus(leaseCache) == Lease.LEASE_DIRTY)
                {
                throw new ConcurrentModificationException("Cache \"" + sCacheName +
                    "\" is locked by another thread or member: " + leaseCache);
                }
        
            Lease lease = handler.ensureLease(oKey);
        
            switch (getThreadStatus(lease))
                {
                case Lease.LEASE_UNISSUED:
                    {
                    if (getReplicatedCacheDependencies().isMobileIssues())
                        {
                        // To improve the performance with "mobile" issuers,
                        // optimistically perform the update assuming that 
                        // this member is the issuer.
                        Member memberIssuer = getThisMember();
                        if (requestUpdate(lease, memberIssuer, oValue, cExpireMillis, true, false))
                            {
                            return null;
                            }
                        // the issuer is gone, try again
                        }
                    else
                        {
                        requestIssue(lease);
                        }
                    break;
                    }
        
                case Lease.LEASE_AVAILABLE:
                    {
                    // resource is not currently locked;
                    // request an immediate update
                    Member memberIssuer = getServiceMemberSet().getMember(lease.getIssuerId());
                    if (memberIssuer == null)
                        {
                        // the issuer is gone, try again
                        break;
                        }
        
                    Object oValueOld = fReturn ? handler.getCachedResource(oKey) : null;
        
                    // an optimization attempt using
                    //  Base.equalsDeep(oValue, oValueOld))
                    // would not be appropriate here:
                    // 1) a client could modify an object field while
                    //    holding a reference
                    // 2) a client may want to bump the resource version
        
                    if (requestUpdate(lease, memberIssuer, oValue, cExpireMillis, true, false))
                        {
                        return oValueOld;
                        }
                    // the issuer is gone, try again
                    break;
                    }
        
                case Lease.LEASE_LOCKED:
                    {
                    Member memberIssuer = getServiceMemberSet().getMember(lease.getIssuerId());
                    if (memberIssuer == null)
                        {
                        // the issuer is gone, this member should become one
                        memberIssuer = getThisMember();
                        }
        
                    Object oValueOld = fReturn ? handler.getCachedResource(oKey) : null;
        
                    if (requestUpdate(lease, memberIssuer, oValue, cExpireMillis, false, fFinal))
                        {
                        return oValueOld;
                        }
                    // the issuer is gone, try again
                    break;
                    }
        
                case Lease.LEASE_DIRTY:
                    throw new ConcurrentModificationException("Cache \"" + sCacheName +
                        "\": Resource is locked by another thread or member: " + lease);
        
                default:
                    throw new IllegalStateException();
                }
            }
        }
    
    /**
     * Wait for the lease unlock. It could come as a result of notification or
    * lease expiration. Since a lease could theoretically be removed while we
    * are waiting, the actual waitng time is just one second and the caller is
    * expected to call this method repeatedly
    * 
    * @param lease  the lease
    * @param cWaitMillis  the time left to wait
    * 
    * @return the updated wait time
     */
    protected long waitForUnlock(com.tangosol.coherence.component.net.Lease lease, long cWaitMillis)
        {
        // import com.tangosol.util.Base;
        
        checkStatus();
        
        // two events can change the lease status:
        // 1) explicit unlocking (calls "notify")
        // 2) lease expiration
        long lTime = Base.getSafeTimeMillis();
        
        synchronized (lease)
            {
            try
                {
                lease.wait(1000);
                }
            catch (InterruptedException e)
                {
                Thread.currentThread().interrupt();
                throw Base.ensureRuntimeException(e);
                }
            }
        
        if (cWaitMillis > 0)
            {
            // reduce the waiting time by the elapsed amount
            cWaitMillis -= Base.getSafeTimeMillis() - lTime;
            cWaitMillis = Math.max(0, cWaitMillis);
            }
        
        return cWaitMillis;
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ReplicatedCache$BackingMapContext
    
    /**
     * The BackingMapManagerContext implementation.
     * 
     * Added decoration support methods in Coherence 3.2.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class BackingMapContext
            extends    com.tangosol.coherence.component.util.BackingMapManagerContext
        {
        // ---- Fields declarations ----
        
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
        
        // Default constructor
        public BackingMapContext()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public BackingMapContext(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ReplicatedCache.BackingMapContext();
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
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/ReplicatedCache$BackingMapContext".replace('/', '.'));
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
        
        // Accessor for the property "ConverterFromInternal"
        /**
         * Getter for property ConverterFromInternal.<p>
         */
        protected com.tangosol.util.Converter getConverterFromInternal()
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
                        conv = getService().instantiateConverterFromInternal(getClassLoader());
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
        protected com.tangosol.util.Converter getConverterToInternal()
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
                        conv = getService().instantiateConverterToInternal(getClassLoader());
                        setConverterToInternal(conv);
                        }
                    }
                }
            return conv;
            }
        
        // Accessor for the property "Service"
        /**
         * Getter for property Service.<p>
         */
        public ReplicatedCache getService()
            {
            return (ReplicatedCache) get_Module();
            }
        
        // Declared at the super level
        public com.tangosol.util.Converter getValueFromInternalConverter()
            {
            return getConverterFromInternal();
            }
        
        // Declared at the super level
        public com.tangosol.util.Converter getValueToInternalConverter()
            {
            return getConverterToInternal();
            }
        
        // Declared at the super level
        /**
         * The "component has been initialized" method-notification called out
        * of setConstructed() for the topmost component and that in turn
        * notifies all the children.
        * 
        * This notification gets called before the control returns back to this
        * component instantiator (using <code>new Component.X()</code> or
        * <code>_newInstance(sName)</code>) and on the same thread. In
        * addition, visual components have a "posted" notification
        * <code>onInitUI</code> that is called after (or at the same time as)
        * the control returns back to the instantiator and possibly on a
        * different thread.
         */
        public void onInit()
            {
            setConfigKey(Integer.valueOf((int) '@'));
            
            super.onInit();
            }
        
        // Declared at the super level
        /**
         * Setter for property ClassLoader.<p>
        * The ClassLoader associated with this context.
         */
        public synchronized void setClassLoader(ClassLoader loader)
            {
            if (loader != getClassLoader())
                {
                super.setClassLoader(loader);
            
                // invalidate the "From" converter that uses ClassLoader
                setConverterFromInternal(null);
                }
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
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ReplicatedCache$CacheHandler
    
    /**
     * CacheHandler represents a named Cache handled by the ReplicatedCache
     * service. During creation each handler is assigned an index that could be
     * used to obtain this handler out of the indexed property "CacheHandler"
     * maintained by the ReplicatedCache service. For the same index there
     * could be a list of handlers that differ only by the value of ClassLoader
     * property. The NextHandler property is used to maintain this list.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class CacheHandler
            extends    com.tangosol.coherence.component.util.CacheHandler
        {
        // ---- Fields declarations ----
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
            __mapChildren.put("BackingMapListener", ReplicatedCache.CacheHandler.BackingMapListener.get_CLASS());
            __mapChildren.put("EntrySet", ReplicatedCache.CacheHandler.EntrySet.get_CLASS());
            __mapChildren.put("KeySet", ReplicatedCache.CacheHandler.KeySet.get_CLASS());
            __mapChildren.put("Validator", ReplicatedCache.CacheHandler.Validator.get_CLASS());
            }
        
        // Default constructor
        public CacheHandler()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public CacheHandler(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                setCacheIndex(-1);
                setDeactivationListeners(new com.tangosol.util.Listeners());
                setIgnoreKey(new java.lang.Object());
                setPutExpiryWarned(false);
                setStandardLeaseMillis(20000L);
                setUseEventDaemon(true);
                setValid(true);
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
            }
        
        //++ getter for static property _Instance
        /**
         * Getter for property _Instance.<p>
        * Auto generated
         */
        public static com.tangosol.coherence.Component get_Instance()
            {
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ReplicatedCache.CacheHandler();
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
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/ReplicatedCache$CacheHandler".replace('/', '.'));
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
        
        //++ getter for autogen property _ChildClasses
        /**
         * This is an auto-generated method that returns the map of design time
        * [static] children.
        * 
        * Note: the class generator will ignore any custom implementation for
        * this behavior.
         */
        protected java.util.Map get_ChildClasses()
            {
            return __mapChildren;
            }
        
        // Declared at the super level
        /**
         * Getter for property ConverterFromInternal.<p>
        * A converter that takes service specific "transmittable" serializable
        * objects and converts them via deserialization (etc.) to the objects
        * expected by clients of the cache.
         */
        public com.tangosol.util.Converter getConverterFromInternal()
            {
            return super.getConverterFromInternal();
            }

        // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ReplicatedCache$CacheHandler$BackingMapListener
        
        /**
         * The backing map listener. Used only if a custom backing map manager
         * uses an ObservableMap to implement the local storage.
         * 
         * @see CacheHandler#instantiateBackingMap
         */
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class BackingMapListener
                extends    com.tangosol.coherence.component.util.CacheHandler.BackingMapListener
            {
            // ---- Fields declarations ----
            
            // Default constructor
            public BackingMapListener()
                {
                this(null, null, true);
                }
            
            // Initializing constructor
            public BackingMapListener(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ReplicatedCache.CacheHandler.BackingMapListener();
                }
            
            //++ getter for static property _CLASS
            /**
             * Getter for property _CLASS.<p>
            * Property with auto-generated accessor that returns the Class
            * object for a given component.
             */
            public static Class get_CLASS()
                {
                Class clz;
                try
                    {
                    clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/ReplicatedCache$CacheHandler$BackingMapListener".replace('/', '.'));
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
            * Note: the class generator will ignore any custom implementation
            * for this behavior.
             */
            private com.tangosol.coherence.Component get_Module()
                {
                return this.get_Parent().get_Parent();
                }
            }

        // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ReplicatedCache$CacheHandler$EntrySet
        
        /**
         * @see CacheHandler#entrySet
         */
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class EntrySet
                extends    com.tangosol.coherence.component.util.CacheHandler.EntrySet
            {
            // ---- Fields declarations ----
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
                __mapChildren.put("Entry", ReplicatedCache.CacheHandler.EntrySet.Entry.get_CLASS());
                __mapChildren.put("Iterator", ReplicatedCache.CacheHandler.EntrySet.Iterator.get_CLASS());
                }
            
            // Default constructor
            public EntrySet()
                {
                this(null, null, true);
                }
            
            // Initializing constructor
            public EntrySet(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                
                
                // containment initialization: children
                
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
                return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ReplicatedCache.CacheHandler.EntrySet();
                }
            
            //++ getter for static property _CLASS
            /**
             * Getter for property _CLASS.<p>
            * Property with auto-generated accessor that returns the Class
            * object for a given component.
             */
            public static Class get_CLASS()
                {
                Class clz;
                try
                    {
                    clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/ReplicatedCache$CacheHandler$EntrySet".replace('/', '.'));
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
            * Note: the class generator will ignore any custom implementation
            * for this behavior.
             */
            private com.tangosol.coherence.Component get_Module()
                {
                return this.get_Parent().get_Parent();
                }
            
            //++ getter for autogen property _ChildClasses
            /**
             * This is an auto-generated method that returns the map of design
            * time [static] children.
            * 
            * Note: the class generator will ignore any custom implementation
            * for this behavior.
             */
            protected java.util.Map get_ChildClasses()
                {
                return __mapChildren;
                }

            // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ReplicatedCache$CacheHandler$EntrySet$Entry
            
            @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
            public static class Entry
                    extends    com.tangosol.coherence.component.util.CacheHandler.EntrySet.Entry
                {
                // ---- Fields declarations ----
                
                // Default constructor
                public Entry()
                    {
                    this(null, null, true);
                    }
                
                // Initializing constructor
                public Entry(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                    return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ReplicatedCache.CacheHandler.EntrySet.Entry();
                    }
                
                //++ getter for static property _CLASS
                /**
                 * Getter for property _CLASS.<p>
                * Property with auto-generated accessor that returns the Class
                * object for a given component.
                 */
                public static Class get_CLASS()
                    {
                    Class clz;
                    try
                        {
                        clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/ReplicatedCache$CacheHandler$EntrySet$Entry".replace('/', '.'));
                        }
                    catch (ClassNotFoundException e)
                        {
                        throw new NoClassDefFoundError(e.getMessage());
                        }
                    return clz;
                    }
                
                //++ getter for autogen property _Module
                /**
                 * This is an auto-generated method that returns the global
                * [design time] parent component.
                * 
                * Note: the class generator will ignore any custom
                * implementation for this behavior.
                 */
                private com.tangosol.coherence.Component get_Module()
                    {
                    return this.get_Parent().get_Parent().get_Parent();
                    }
                }

            // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ReplicatedCache$CacheHandler$EntrySet$Iterator
            
            @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
            public static class Iterator
                    extends    com.tangosol.coherence.component.util.CacheHandler.EntrySet.Iterator
                {
                // ---- Fields declarations ----
                
                // Default constructor
                public Iterator()
                    {
                    this(null, null, true);
                    }
                
                // Initializing constructor
                public Iterator(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                    return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ReplicatedCache.CacheHandler.EntrySet.Iterator();
                    }
                
                //++ getter for static property _CLASS
                /**
                 * Getter for property _CLASS.<p>
                * Property with auto-generated accessor that returns the Class
                * object for a given component.
                 */
                public static Class get_CLASS()
                    {
                    Class clz;
                    try
                        {
                        clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/ReplicatedCache$CacheHandler$EntrySet$Iterator".replace('/', '.'));
                        }
                    catch (ClassNotFoundException e)
                        {
                        throw new NoClassDefFoundError(e.getMessage());
                        }
                    return clz;
                    }
                
                //++ getter for autogen property _Module
                /**
                 * This is an auto-generated method that returns the global
                * [design time] parent component.
                * 
                * Note: the class generator will ignore any custom
                * implementation for this behavior.
                 */
                private com.tangosol.coherence.Component get_Module()
                    {
                    return this.get_Parent().get_Parent().get_Parent();
                    }
                }
            }

        // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ReplicatedCache$CacheHandler$KeySet
        
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class KeySet
                extends    com.tangosol.coherence.component.util.CacheHandler.KeySet
            {
            // ---- Fields declarations ----
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
                __mapChildren.put("Iterator", ReplicatedCache.CacheHandler.KeySet.Iterator.get_CLASS());
                }
            
            // Default constructor
            public KeySet()
                {
                this(null, null, true);
                }
            
            // Initializing constructor
            public KeySet(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                
                
                // containment initialization: children
                
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
                return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ReplicatedCache.CacheHandler.KeySet();
                }
            
            //++ getter for static property _CLASS
            /**
             * Getter for property _CLASS.<p>
            * Property with auto-generated accessor that returns the Class
            * object for a given component.
             */
            public static Class get_CLASS()
                {
                Class clz;
                try
                    {
                    clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/ReplicatedCache$CacheHandler$KeySet".replace('/', '.'));
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
            * Note: the class generator will ignore any custom implementation
            * for this behavior.
             */
            private com.tangosol.coherence.Component get_Module()
                {
                return this.get_Parent().get_Parent();
                }
            
            //++ getter for autogen property _ChildClasses
            /**
             * This is an auto-generated method that returns the map of design
            * time [static] children.
            * 
            * Note: the class generator will ignore any custom implementation
            * for this behavior.
             */
            protected java.util.Map get_ChildClasses()
                {
                return __mapChildren;
                }

            // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ReplicatedCache$CacheHandler$KeySet$Iterator
            
            @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
            public static class Iterator
                    extends    com.tangosol.coherence.component.util.CacheHandler.KeySet.Iterator
                {
                // ---- Fields declarations ----
                
                // Default constructor
                public Iterator()
                    {
                    this(null, null, true);
                    }
                
                // Initializing constructor
                public Iterator(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                    return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ReplicatedCache.CacheHandler.KeySet.Iterator();
                    }
                
                //++ getter for static property _CLASS
                /**
                 * Getter for property _CLASS.<p>
                * Property with auto-generated accessor that returns the Class
                * object for a given component.
                 */
                public static Class get_CLASS()
                    {
                    Class clz;
                    try
                        {
                        clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/ReplicatedCache$CacheHandler$KeySet$Iterator".replace('/', '.'));
                        }
                    catch (ClassNotFoundException e)
                        {
                        throw new NoClassDefFoundError(e.getMessage());
                        }
                    return clz;
                    }
                
                //++ getter for autogen property _Module
                /**
                 * This is an auto-generated method that returns the global
                * [design time] parent component.
                * 
                * Note: the class generator will ignore any custom
                * implementation for this behavior.
                 */
                private com.tangosol.coherence.Component get_Module()
                    {
                    return this.get_Parent().get_Parent().get_Parent();
                    }
                }
            }

        // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ReplicatedCache$CacheHandler$Validator
        
        /**
         * The default Validator that uses the Lease version to resolve the 
         * update conflicts [in the case of optimistic concurrency].
         * 
         * Deprecated since Coherence 2.3
         */
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class Validator
                extends    com.tangosol.coherence.component.util.CacheHandler.Validator
            {
            // ---- Fields declarations ----
            
            // Default constructor
            public Validator()
                {
                this(null, null, true);
                }
            
            // Initializing constructor
            public Validator(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                    setLeaseMap(new com.tangosol.util.SafeHashMap());
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
                return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ReplicatedCache.CacheHandler.Validator();
                }
            
            //++ getter for static property _CLASS
            /**
             * Getter for property _CLASS.<p>
            * Property with auto-generated accessor that returns the Class
            * object for a given component.
             */
            public static Class get_CLASS()
                {
                Class clz;
                try
                    {
                    clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/ReplicatedCache$CacheHandler$Validator".replace('/', '.'));
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
            * Note: the class generator will ignore any custom implementation
            * for this behavior.
             */
            private com.tangosol.coherence.Component get_Module()
                {
                return this.get_Parent().get_Parent();
                }
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ReplicatedCache$CacheUpdate
    
    /**
     * Message:
     *     CacheUpdate
     * 
     * Purpose:
     *     Update the applicant with all the resources in a cache with the
     * specified index owned by the sender.
     * 
     * Attributes:
     *     CacheIndex
     *     CacheData
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class CacheUpdate
            extends    com.tangosol.coherence.component.net.message.CacheMessage
        {
        // ---- Fields declarations ----
        
        // Default constructor
        public CacheUpdate()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public CacheUpdate(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                setMessageType(43);
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ReplicatedCache.CacheUpdate();
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
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/ReplicatedCache$CacheUpdate".replace('/', '.'));
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
            
            ReplicatedCache service = (ReplicatedCache) getService();
            
            service.ensureCacheHandler(getCacheIndex()).populateCache(this);
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ReplicatedCache$CacheUpdateRequest
    
    /**
     * Message:
     *     CacheUpdateRequest
     * 
     * Purpose:
     *     Request an update of the content of all caches owned by the
     * recipient.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class CacheUpdateRequest
            extends    com.tangosol.coherence.component.net.message.RequestMessage
        {
        // ---- Fields declarations ----
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
            __mapChildren.put("Poll", ReplicatedCache.CacheUpdateRequest.Poll.get_CLASS());
            }
        
        // Default constructor
        public CacheUpdateRequest()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public CacheUpdateRequest(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                setMessageType(44);
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
            }
        
        //++ getter for static property _Instance
        /**
         * Getter for property _Instance.<p>
        * Auto generated
         */
        public static com.tangosol.coherence.Component get_Instance()
            {
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ReplicatedCache.CacheUpdateRequest();
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
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/ReplicatedCache$CacheUpdateRequest".replace('/', '.'));
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
        
        //++ getter for autogen property _ChildClasses
        /**
         * This is an auto-generated method that returns the map of design time
        * [static] children.
        * 
        * Note: the class generator will ignore any custom implementation for
        * this behavior.
         */
        protected java.util.Map get_ChildClasses()
            {
            return __mapChildren;
            }
        
        // Declared at the super level
        protected com.tangosol.coherence.component.net.Poll instantiatePoll()
            {
            // import Component.Net.Poll as com.tangosol.coherence.component.net.Poll;
            
            return (com.tangosol.coherence.component.net.Poll) _newChild("Poll");
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
            
            ReplicatedCache service = (ReplicatedCache) getService();
            
            service.onCacheUpdateRequest(this);
            }
        
        // Declared at the super level
        /**
         * Setter for property RequestPoll.<p>
        * This is the Poll that the RequestMessage creates to collect responses.
         */
        public void setRequestPoll(com.tangosol.coherence.component.net.Poll poll)
            {
            super.setRequestPoll(poll);
            }

        // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ReplicatedCache$CacheUpdateRequest$Poll
        
        /**
         * The Poll contains information regarding a request sent to one or
         * more Cluster Members that require responses. A Service may poll
         * other Members that are running the same Service, and the Poll is
         * used to wait for and assemble the responses from each of those
         * Members. A client thread may also use the Poll to block on a
         * response or set of responses, thus waiting for the completion of the
         * Poll. In its simplest form, which is a Poll that is sent to one
         * Member of the Cluster, the Poll actually represents the
         * request/response model.
         */
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class Poll
                extends    com.tangosol.coherence.component.net.Poll
            {
            // ---- Fields declarations ----
            
            // Default constructor
            public Poll()
                {
                this(null, null, true);
                }
            
            // Initializing constructor
            public Poll(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ReplicatedCache.CacheUpdateRequest.Poll();
                }
            
            //++ getter for static property _CLASS
            /**
             * Getter for property _CLASS.<p>
            * Property with auto-generated accessor that returns the Class
            * object for a given component.
             */
            public static Class get_CLASS()
                {
                Class clz;
                try
                    {
                    clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/ReplicatedCache$CacheUpdateRequest$Poll".replace('/', '.'));
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
            * Note: the class generator will ignore any custom implementation
            * for this behavior.
             */
            private com.tangosol.coherence.Component get_Module()
                {
                return this.get_Parent().get_Parent();
                }
            
            // Declared at the super level
            /**
             * This is the event that is executed when all the Members that were
            * polled have responded or have left the Service.
             */
            protected void onCompletion()
                {
                ReplicatedCache service = (ReplicatedCache) getService();
                
                if (service.getServiceState() == ReplicatedCache.SERVICE_STARTED)
                    {
                    if (getLeftMemberSet().isEmpty())
                        {
                        // everyone responded; we are good to go
                        service.setAcceptingClients(true);
                        }
                    else
                        {
                        // we have to redo the poll
                        service.requestCacheUpdate();
                        }
                    }
                
                super.onCompletion();
                }
            
            // Declared at the super level
            /**
             * This event occurs when a response from a Member is processed.
             */
            public void onResponded(com.tangosol.coherence.component.net.Member member)
                {
                ((ReplicatedCache) getService()).getUpdatingMemberSet().add(member);
                
                super.onResponded(member);
                }
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ReplicatedCache$CatalogHandler
    
    /**
     * CacheHandler represents a named Cache handled by the ReplicatedCache
     * service. During creation each handler is assigned an index that could be
     * used to obtain this handler out of the indexed property "CacheHandler"
     * maintained by the ReplicatedCache service. For the same index there
     * could be a list of handlers that differ only by the value of ClassLoader
     * property. The NextHandler property is used to maintain this list.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class CatalogHandler
            extends    com.tangosol.coherence.component.util.cacheHandler.CatalogHandler
        {
        // ---- Fields declarations ----
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
            __mapChildren.put("BackingMapListener", ReplicatedCache.CatalogHandler.BackingMapListener.get_CLASS());
            __mapChildren.put("EntrySet", ReplicatedCache.CatalogHandler.EntrySet.get_CLASS());
            __mapChildren.put("KeySet", ReplicatedCache.CatalogHandler.KeySet.get_CLASS());
            __mapChildren.put("Validator", ReplicatedCache.CatalogHandler.Validator.get_CLASS());
            }
        
        // Default constructor
        public CatalogHandler()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public CatalogHandler(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                setCacheIndex(0);
                setCacheName("$$$");
                setDeactivationListeners(new com.tangosol.util.Listeners());
                setIgnoreKey(new java.lang.Object());
                setLeaseMap(new com.tangosol.util.SafeHashMap());
                setPutExpiryWarned(false);
                setResourceMap(new com.tangosol.util.SafeHashMap());
                setStandardLeaseMillis(0L);
                setUseEventDaemon(false);
                setValid(true);
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
            }
        
        //++ getter for static property _Instance
        /**
         * Getter for property _Instance.<p>
        * Auto generated
         */
        public static com.tangosol.coherence.Component get_Instance()
            {
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ReplicatedCache.CatalogHandler();
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
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/ReplicatedCache$CatalogHandler".replace('/', '.'));
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
        
        //++ getter for autogen property _ChildClasses
        /**
         * This is an auto-generated method that returns the map of design time
        * [static] children.
        * 
        * Note: the class generator will ignore any custom implementation for
        * this behavior.
         */
        protected java.util.Map get_ChildClasses()
            {
            return __mapChildren;
            }
        
        // Declared at the super level
        /**
         * Getter for property ConverterFromInternal.<p>
        * A converter that takes service specific "transmittable" serializable
        * objects and converts them via deserialization (etc.) to the objects
        * expected by clients of the cache.
         */
        public com.tangosol.util.Converter getConverterFromInternal()
            {
            return super.getConverterFromInternal();
            }

        // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ReplicatedCache$CatalogHandler$BackingMapListener
        
        /**
         * The backing map listener. Used only if a custom backing map manager
         * uses an ObservableMap to implement the local storage.
         * 
         * @see CacheHandler#instantiateBackingMap
         */
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class BackingMapListener
                extends    com.tangosol.coherence.component.util.CacheHandler.BackingMapListener
            {
            // ---- Fields declarations ----
            
            // Default constructor
            public BackingMapListener()
                {
                this(null, null, true);
                }
            
            // Initializing constructor
            public BackingMapListener(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ReplicatedCache.CatalogHandler.BackingMapListener();
                }
            
            //++ getter for static property _CLASS
            /**
             * Getter for property _CLASS.<p>
            * Property with auto-generated accessor that returns the Class
            * object for a given component.
             */
            public static Class get_CLASS()
                {
                Class clz;
                try
                    {
                    clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/ReplicatedCache$CatalogHandler$BackingMapListener".replace('/', '.'));
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
            * Note: the class generator will ignore any custom implementation
            * for this behavior.
             */
            private com.tangosol.coherence.Component get_Module()
                {
                return this.get_Parent().get_Parent();
                }
            }

        // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ReplicatedCache$CatalogHandler$EntrySet
        
        /**
         * @see CacheHandler#entrySet
         */
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class EntrySet
                extends    com.tangosol.coherence.component.util.CacheHandler.EntrySet
            {
            // ---- Fields declarations ----
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
                __mapChildren.put("Entry", ReplicatedCache.CatalogHandler.EntrySet.Entry.get_CLASS());
                __mapChildren.put("Iterator", ReplicatedCache.CatalogHandler.EntrySet.Iterator.get_CLASS());
                }
            
            // Default constructor
            public EntrySet()
                {
                this(null, null, true);
                }
            
            // Initializing constructor
            public EntrySet(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                
                
                // containment initialization: children
                
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
                return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ReplicatedCache.CatalogHandler.EntrySet();
                }
            
            //++ getter for static property _CLASS
            /**
             * Getter for property _CLASS.<p>
            * Property with auto-generated accessor that returns the Class
            * object for a given component.
             */
            public static Class get_CLASS()
                {
                Class clz;
                try
                    {
                    clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/ReplicatedCache$CatalogHandler$EntrySet".replace('/', '.'));
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
            * Note: the class generator will ignore any custom implementation
            * for this behavior.
             */
            private com.tangosol.coherence.Component get_Module()
                {
                return this.get_Parent().get_Parent();
                }
            
            //++ getter for autogen property _ChildClasses
            /**
             * This is an auto-generated method that returns the map of design
            * time [static] children.
            * 
            * Note: the class generator will ignore any custom implementation
            * for this behavior.
             */
            protected java.util.Map get_ChildClasses()
                {
                return __mapChildren;
                }

            // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ReplicatedCache$CatalogHandler$EntrySet$Entry
            
            @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
            public static class Entry
                    extends    com.tangosol.coherence.component.util.CacheHandler.EntrySet.Entry
                {
                // ---- Fields declarations ----
                
                // Default constructor
                public Entry()
                    {
                    this(null, null, true);
                    }
                
                // Initializing constructor
                public Entry(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                    return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ReplicatedCache.CatalogHandler.EntrySet.Entry();
                    }
                
                //++ getter for static property _CLASS
                /**
                 * Getter for property _CLASS.<p>
                * Property with auto-generated accessor that returns the Class
                * object for a given component.
                 */
                public static Class get_CLASS()
                    {
                    Class clz;
                    try
                        {
                        clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/ReplicatedCache$CatalogHandler$EntrySet$Entry".replace('/', '.'));
                        }
                    catch (ClassNotFoundException e)
                        {
                        throw new NoClassDefFoundError(e.getMessage());
                        }
                    return clz;
                    }
                
                //++ getter for autogen property _Module
                /**
                 * This is an auto-generated method that returns the global
                * [design time] parent component.
                * 
                * Note: the class generator will ignore any custom
                * implementation for this behavior.
                 */
                private com.tangosol.coherence.Component get_Module()
                    {
                    return this.get_Parent().get_Parent().get_Parent();
                    }
                }

            // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ReplicatedCache$CatalogHandler$EntrySet$Iterator
            
            @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
            public static class Iterator
                    extends    com.tangosol.coherence.component.util.CacheHandler.EntrySet.Iterator
                {
                // ---- Fields declarations ----
                
                // Default constructor
                public Iterator()
                    {
                    this(null, null, true);
                    }
                
                // Initializing constructor
                public Iterator(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                    return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ReplicatedCache.CatalogHandler.EntrySet.Iterator();
                    }
                
                //++ getter for static property _CLASS
                /**
                 * Getter for property _CLASS.<p>
                * Property with auto-generated accessor that returns the Class
                * object for a given component.
                 */
                public static Class get_CLASS()
                    {
                    Class clz;
                    try
                        {
                        clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/ReplicatedCache$CatalogHandler$EntrySet$Iterator".replace('/', '.'));
                        }
                    catch (ClassNotFoundException e)
                        {
                        throw new NoClassDefFoundError(e.getMessage());
                        }
                    return clz;
                    }
                
                //++ getter for autogen property _Module
                /**
                 * This is an auto-generated method that returns the global
                * [design time] parent component.
                * 
                * Note: the class generator will ignore any custom
                * implementation for this behavior.
                 */
                private com.tangosol.coherence.Component get_Module()
                    {
                    return this.get_Parent().get_Parent().get_Parent();
                    }
                }
            }

        // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ReplicatedCache$CatalogHandler$KeySet
        
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class KeySet
                extends    com.tangosol.coherence.component.util.CacheHandler.KeySet
            {
            // ---- Fields declarations ----
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
                __mapChildren.put("Iterator", ReplicatedCache.CatalogHandler.KeySet.Iterator.get_CLASS());
                }
            
            // Default constructor
            public KeySet()
                {
                this(null, null, true);
                }
            
            // Initializing constructor
            public KeySet(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                
                
                // containment initialization: children
                
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
                return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ReplicatedCache.CatalogHandler.KeySet();
                }
            
            //++ getter for static property _CLASS
            /**
             * Getter for property _CLASS.<p>
            * Property with auto-generated accessor that returns the Class
            * object for a given component.
             */
            public static Class get_CLASS()
                {
                Class clz;
                try
                    {
                    clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/ReplicatedCache$CatalogHandler$KeySet".replace('/', '.'));
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
            * Note: the class generator will ignore any custom implementation
            * for this behavior.
             */
            private com.tangosol.coherence.Component get_Module()
                {
                return this.get_Parent().get_Parent();
                }
            
            //++ getter for autogen property _ChildClasses
            /**
             * This is an auto-generated method that returns the map of design
            * time [static] children.
            * 
            * Note: the class generator will ignore any custom implementation
            * for this behavior.
             */
            protected java.util.Map get_ChildClasses()
                {
                return __mapChildren;
                }

            // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ReplicatedCache$CatalogHandler$KeySet$Iterator
            
            @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
            public static class Iterator
                    extends    com.tangosol.coherence.component.util.CacheHandler.KeySet.Iterator
                {
                // ---- Fields declarations ----
                
                // Default constructor
                public Iterator()
                    {
                    this(null, null, true);
                    }
                
                // Initializing constructor
                public Iterator(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                    return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ReplicatedCache.CatalogHandler.KeySet.Iterator();
                    }
                
                //++ getter for static property _CLASS
                /**
                 * Getter for property _CLASS.<p>
                * Property with auto-generated accessor that returns the Class
                * object for a given component.
                 */
                public static Class get_CLASS()
                    {
                    Class clz;
                    try
                        {
                        clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/ReplicatedCache$CatalogHandler$KeySet$Iterator".replace('/', '.'));
                        }
                    catch (ClassNotFoundException e)
                        {
                        throw new NoClassDefFoundError(e.getMessage());
                        }
                    return clz;
                    }
                
                //++ getter for autogen property _Module
                /**
                 * This is an auto-generated method that returns the global
                * [design time] parent component.
                * 
                * Note: the class generator will ignore any custom
                * implementation for this behavior.
                 */
                private com.tangosol.coherence.Component get_Module()
                    {
                    return this.get_Parent().get_Parent().get_Parent();
                    }
                }
            }

        // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ReplicatedCache$CatalogHandler$Validator
        
        /**
         * The default Validator that uses the Lease version to resolve the 
         * update conflicts [in the case of optimistic concurrency].
         * 
         * Deprecated since Coherence 2.3
         */
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class Validator
                extends    com.tangosol.coherence.component.util.CacheHandler.Validator
            {
            // ---- Fields declarations ----
            
            // Default constructor
            public Validator()
                {
                this(null, null, true);
                }
            
            // Initializing constructor
            public Validator(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                    setLeaseMap(new com.tangosol.util.SafeHashMap());
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
                return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ReplicatedCache.CatalogHandler.Validator();
                }
            
            //++ getter for static property _CLASS
            /**
             * Getter for property _CLASS.<p>
            * Property with auto-generated accessor that returns the Class
            * object for a given component.
             */
            public static Class get_CLASS()
                {
                Class clz;
                try
                    {
                    clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/ReplicatedCache$CatalogHandler$Validator".replace('/', '.'));
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
            * Note: the class generator will ignore any custom implementation
            * for this behavior.
             */
            private com.tangosol.coherence.Component get_Module()
                {
                return this.get_Parent().get_Parent();
                }
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ReplicatedCache$ConverterFromInternal
    
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ReplicatedCache.ConverterFromInternal();
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
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/ReplicatedCache$ConverterFromInternal".replace('/', '.'));
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

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ReplicatedCache$ConverterToInternal
    
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ReplicatedCache.ConverterToInternal();
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
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/ReplicatedCache$ConverterToInternal".replace('/', '.'));
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
            
            if (!(o instanceof Binary))
                {
                o = ExternalizableHelper.toBinary(o, getSerializer());
                }
            
            return o;
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ReplicatedCache$DispatchEvent
    
    /**
     * Runnable event.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class DispatchEvent
            extends    com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.DispatchEvent
        {
        // ---- Fields declarations ----
        
        // Default constructor
        public DispatchEvent()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public DispatchEvent(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ReplicatedCache.DispatchEvent();
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
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/ReplicatedCache$DispatchEvent".replace('/', '.'));
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
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ReplicatedCache$LeaseIssue
    
    /**
     * Message:
     *     LeaseIssue
     * 
     * Purpose:
     *     This Message informs all Members of ReplicatedCache service that a
     * lease for the specified resource has been issued.
     * 
     * Description:
     *     In response to this mesage all Members should update their Lease
     * maps to reflect the presence of the resource. All synchronization
     * requests regarding this Lease should be sent to the issuer.
     * 
     * Response to:
     *     LeaseIssueRequest
     * 
     * Expected responses:
     *     n/a
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class LeaseIssue
            extends    com.tangosol.coherence.component.net.message.LeaseMessage
        {
        // ---- Fields declarations ----
        
        // Default constructor
        public LeaseIssue()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public LeaseIssue(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                setMessageType(33);
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ReplicatedCache.LeaseIssue();
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
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/ReplicatedCache$LeaseIssue".replace('/', '.'));
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
            // import Component.Net.Lease;
            // import Component.Net.Member;
            
            super.onReceived();
            
            ReplicatedCache service = (ReplicatedCache) getService();
            
            Member memberFrom = getFromMember();
            Member memberThis = service.getThisMember();
            if (memberFrom != memberThis && service.getUpdatingMemberSet().contains(memberFrom))
                {
                Lease lease = getLease();
            
                // copy the new info unconditionally
                service.ensureCacheHandler(lease.getCacheIndex()).
                    ensureLease(lease.getResourceKey()).copyFrom(lease);
                }
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ReplicatedCache$LeaseIssueRequest
    
    /**
     * Message:
     *     LeaseIssueRequest
     * 
     * Purpose:
     *     This Message is sent to the senior Member of the Cluster to request
     * an issuance of a Lease for the specified resource.
     * 
     * Description:
     *     In response to this mesage the senior Member sends the LeaseIssue
     * message to every Member of the cluster.
     * 
     * Response to:
     *     n/a
     * 
     * Expected responses:
     *     LeaseIssue
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class LeaseIssueRequest
            extends    com.tangosol.coherence.component.net.message.requestMessage.LeaseRequest
        {
        // ---- Fields declarations ----
        
        // Default constructor
        public LeaseIssueRequest()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public LeaseIssueRequest(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                setMessageType(34);
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ReplicatedCache.LeaseIssueRequest();
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
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/ReplicatedCache$LeaseIssueRequest".replace('/', '.'));
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
            
            ((ReplicatedCache) getService()).onLeaseIssueRequest(this);
            }
        
        // Declared at the super level
        /**
         * Setter for property RequestPoll.<p>
        * This is the Poll that the RequestMessage creates to collect responses.
         */
        public void setRequestPoll(com.tangosol.coherence.component.net.Poll poll)
            {
            super.setRequestPoll(poll);
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ReplicatedCache$LeaseLock
    
    /**
     * Message:
     *     LeaseLock
     * 
     * Purpose:
     *     This Message informs all Members of ReplicatedCache service that a
     * Lease for the specified resource is assigned.
     * 
     * Description:
     *     In response to this mesage all Members should update their Lease
     * maps to reflect the state of the resource.
     * 
     * Response to:
     *     LeaseLockRequest
     * 
     * Expected responses:
     *     n/a
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class LeaseLock
            extends    com.tangosol.coherence.component.net.message.LeaseMessage
        {
        // ---- Fields declarations ----
        
        // Default constructor
        public LeaseLock()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public LeaseLock(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                setMessageType(35);
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ReplicatedCache.LeaseLock();
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
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/ReplicatedCache$LeaseLock".replace('/', '.'));
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
            // import Component.Net.Lease;
            // import Component.Net.Member;
            
            super.onReceived();
            
            ReplicatedCache service = (ReplicatedCache) getService();
            
            Member memberFrom = getFromMember();
            Member memberThis = service.getThisMember();
            if (memberFrom != memberThis && service.getUpdatingMemberSet().contains(memberFrom))
                {
                Lease lease = getLease();
            
                // copy the new info unconditionally
                service.ensureCacheHandler(lease.getCacheIndex()).
                    ensureLease(lease.getResourceKey()).copyFrom(lease);
                }
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ReplicatedCache$LeaseLockRequest
    
    /**
     * Message:
     *     LeaseLockRequest
     * 
     * Purpose:
     *     This Message is sent to the issuer of a Lease to request an
     * assignment of the Lease for the specified Effective and Expiration
     * times.
     * 
     * Description:
     *     In response to this mesage the Lease issuer sends the LeaseLock
     * message to every Member of the cluster.
     * 
     * Response to:
     *     n/a
     * 
     * Expected responses:
     *     LeaseLock
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class LeaseLockRequest
            extends    com.tangosol.coherence.component.net.message.requestMessage.LeaseRequest
        {
        // ---- Fields declarations ----
        
        /**
         * Property LeaseMillis
         *
         * The LeaseMillis value is the duration of the Lease in milliseconds
         * starting immediately after the lock is acquired; 0 means indefinite
         * lease duration
         */
        private long __m_LeaseMillis;
        
        /**
         * Property WaitMillis
         *
         * The LeaseWaitMillis value is the time interval by which this
         * LeaseLockRequest must be replied to.
         */
        private long __m_WaitMillis;
        
        /**
         * Property WaitTimeout
         *
         * The WaitTimeout value is the Cluster time by which this
         * LeaseLockRequest must be replied to. This value is calculated based
         * on the WaitMillis value and cached upon the very first access.
         */
        private transient long __m_WaitTimeout;
        
        // Default constructor
        public LeaseLockRequest()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public LeaseLockRequest(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                setMessageType(36);
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ReplicatedCache.LeaseLockRequest();
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
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/ReplicatedCache$LeaseLockRequest".replace('/', '.'));
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
         * Getter for property Description.<p>
        * Used for debugging purposes (from toString). Create a human-readable
        * description of the specific Message data.
         */
        public String getDescription()
            {
            return super.getDescription() +
                "\nWaitTimeout=" + getWaitTimeout() +
                ", LeaseDuration=" + getLeaseMillis();
            }
        
        // Accessor for the property "LeaseMillis"
        /**
         * Getter for property LeaseMillis.<p>
        * The LeaseMillis value is the duration of the Lease in milliseconds
        * starting immediately after the lock is acquired; 0 means indefinite
        * lease duration
         */
        public long getLeaseMillis()
            {
            return __m_LeaseMillis;
            }
        
        // Accessor for the property "WaitMillis"
        /**
         * Getter for property WaitMillis.<p>
        * The LeaseWaitMillis value is the time interval by which this
        * LeaseLockRequest must be replied to.
         */
        public long getWaitMillis()
            {
            return __m_WaitMillis;
            }
        
        // Accessor for the property "WaitTimeout"
        /**
         * Getter for property WaitTimeout.<p>
        * The WaitTimeout value is the Cluster time by which this
        * LeaseLockRequest must be replied to. This value is calculated based
        * on the WaitMillis value and cached upon the very first access.
         */
        public long getWaitTimeout()
            {
            long ldtTimeout = __m_WaitTimeout;
            if (ldtTimeout == 0L)
                {
                ReplicatedCache service = (ReplicatedCache) getService();
            
                ldtTimeout = service.adjustWaitTime(getWaitMillis(), ReplicatedCache.TIME_CLUSTER);
                setWaitTimeout(ldtTimeout);
                }
            return ldtTimeout;
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
            
            ReplicatedCache service = (ReplicatedCache) getService();
            
            service.onLeaseLockRequest(this);
            }
        
        // Declared at the super level
        public void read(com.tangosol.io.ReadBuffer.BufferInput input)
                throws java.io.IOException
            {
            // import com.tangosol.util.ExternalizableHelper;
            
            super.read(input);
            
            setWaitMillis(ExternalizableHelper.readLong(input));
            setLeaseMillis(ExternalizableHelper.readLong(input));
            }
        
        // Accessor for the property "LeaseMillis"
        /**
         * Setter for property LeaseMillis.<p>
        * The LeaseMillis value is the duration of the Lease in milliseconds
        * starting immediately after the lock is acquired; 0 means indefinite
        * lease duration
         */
        public void setLeaseMillis(long pLeaseMillis)
            {
            __m_LeaseMillis = pLeaseMillis;
            }
        
        // Accessor for the property "WaitMillis"
        /**
         * Setter for property WaitMillis.<p>
        * The LeaseWaitMillis value is the time interval by which this
        * LeaseLockRequest must be replied to.
         */
        public void setWaitMillis(long cMillis)
            {
            __m_WaitMillis = (cMillis);
            setWaitTimeout(0L); // clear a cached value
            }
        
        // Accessor for the property "WaitTimeout"
        /**
         * Setter for property WaitTimeout.<p>
        * The WaitTimeout value is the Cluster time by which this
        * LeaseLockRequest must be replied to. This value is calculated based
        * on the WaitMillis value and cached upon the very first access.
         */
        protected void setWaitTimeout(long ldtTimeout)
            {
            __m_WaitTimeout = ldtTimeout;
            }
        
        // Declared at the super level
        public void write(com.tangosol.io.WriteBuffer.BufferOutput output)
                throws java.io.IOException
            {
            // import com.tangosol.util.ExternalizableHelper;
            
            super.write(output);
            
            ExternalizableHelper.writeLong(output, getWaitMillis());
            ExternalizableHelper.writeLong(output, getLeaseMillis());
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ReplicatedCache$LeaseRemove
    
    /**
     * Message:
     *     LeaseRemove
     * 
     * Purpose:
     *     This Message informs all Members of ReplicatedCache service that the
     * specified resource should be removed. It could only come from the
     * current Lease holder.
     * 
     * Description:
     *     In response to this mesage all Members should update their Lease
     * maps to reflect the state of the resource.
     * 
     * Response to:
     *     n/a
     * 
     * Expected responses:
     *     n/a
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class LeaseRemove
            extends    com.tangosol.coherence.component.net.message.LeaseMessage
        {
        // ---- Fields declarations ----
        
        // Default constructor
        public LeaseRemove()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public LeaseRemove(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                setMessageType(37);
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ReplicatedCache.LeaseRemove();
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
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/ReplicatedCache$LeaseRemove".replace('/', '.'));
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
            // import Component.Net.Lease;
            // import Component.Net.Member;
            
            super.onReceived();
            
            ReplicatedCache service = (ReplicatedCache) getService();
            
            Member memberFrom = getFromMember();
            Member memberThis = service.getThisMember();
            if (memberFrom != memberThis && service.getUpdatingMemberSet().contains(memberFrom))
                {
                Lease lease = getLease();
            
                service.ensureCacheHandler(lease.getCacheIndex()).onLeaseRemove(lease);
                }
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ReplicatedCache$LeaseRemoveRequest
    
    /**
     * Message:
     *     LeaseRemoveRequest
     * 
     * Purpose:
     *     This Message is sent to the issuer of a Lease to request a removal
     * of a resource. This message is an optimization for the sequence of
     * "LeaseLockRequest", "LeaseRemove".  It can only be send to the Lease
     * issuer.
     * 
     * Description:
     *     In response to this mesage the Lease issuer sends the LeaseRemove
     * message to every Member of the cluster.
     * 
     * Response to:
     *     n/a
     * 
     * Expected responses:
     *     LeaseRemove
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class LeaseRemoveRequest
            extends    com.tangosol.coherence.component.net.message.requestMessage.LeaseRequest
        {
        // ---- Fields declarations ----
        
        // Default constructor
        public LeaseRemoveRequest()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public LeaseRemoveRequest(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                setMessageType(38);
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ReplicatedCache.LeaseRemoveRequest();
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
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/ReplicatedCache$LeaseRemoveRequest".replace('/', '.'));
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
            
            ReplicatedCache service = (ReplicatedCache) getService();
            
            service.onLeaseRemoveRequest(this);
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ReplicatedCache$LeaseUnlock
    
    /**
     * Message:
     *     LeaseUnlock
     * 
     * Purpose:
     *     This Message informs all Members of ReplicatedCache service that the
     * specified resource should be unlocked. It could only come from the
     * current Lease holder.
     * 
     * Description:
     *     In response to this mesage all Members should update their Lease
     * maps to reflect the state of the resource.
     * 
     * Response to:
     *     n/a
     * 
     * Expected responses:
     *     n/a
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class LeaseUnlock
            extends    com.tangosol.coherence.component.net.message.LeaseMessage
        {
        // ---- Fields declarations ----
        
        // Default constructor
        public LeaseUnlock()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public LeaseUnlock(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                setMessageType(39);
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ReplicatedCache.LeaseUnlock();
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
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/ReplicatedCache$LeaseUnlock".replace('/', '.'));
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
            // import Component.Net.Lease;
            // import Component.Net.Member;
            
            super.onReceived();
            
            ReplicatedCache service = (ReplicatedCache) getService();
            
            Member memberFrom = getFromMember();
            Member memberThis = service.getThisMember();
            if (memberFrom != memberThis && service.getUpdatingMemberSet().contains(memberFrom))
                {
                Lease lease = getLease();
            
                service.ensureCacheHandler(lease.getCacheIndex()).
                    onLeaseUpdate(lease, false, null, 0L);
                }
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ReplicatedCache$LeaseUnlockRequest
    
    /**
     * Message:
     *     LeaseUnockRequest
     * 
     * Purpose:
     *     This Message is sent to the issuer of a Lease to request an ulocking
     * of the lease
     * 
     * Description:
     *     In response to this mesage the Lease issuer sends the LeaseUnlock
     * message to every Member of the cluster.
     * 
     * Response to:
     *     n/a
     * 
     * Expected responses:
     *     LeaseUnlock
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class LeaseUnlockRequest
            extends    com.tangosol.coherence.component.net.message.requestMessage.LeaseRequest
        {
        // ---- Fields declarations ----
        
        // Default constructor
        public LeaseUnlockRequest()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public LeaseUnlockRequest(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                setMessageType(40);
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ReplicatedCache.LeaseUnlockRequest();
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
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/ReplicatedCache$LeaseUnlockRequest".replace('/', '.'));
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
            
            ReplicatedCache service = (ReplicatedCache) getService();
            
            service.onLeaseUnlockRequest(this);
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ReplicatedCache$LeaseUpdate
    
    /**
     * Message:
     *     LeaseUpdate
     * 
     * Purpose:
     *     This Message informs all Members of ReplicatedCache service that the
     * specified resource should be updated. It could only come from the
     * current Lease holder.
     * 
     * Description:
     *     In response to this mesage all Members should update their Lease
     * maps to reflect the state and value of the resource.
     * 
     * Response to:
     *     n/a
     * 
     * Expected responses:
     *     n/a
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class LeaseUpdate
            extends    com.tangosol.coherence.component.net.message.leaseMessage.ResourceMessage
        {
        // ---- Fields declarations ----
        
        // Default constructor
        public LeaseUpdate()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public LeaseUpdate(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                setMessageType(41);
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ReplicatedCache.LeaseUpdate();
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
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/ReplicatedCache$LeaseUpdate".replace('/', '.'));
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
            // import Component.Net.Lease;
            // import Component.Net.Member;
            
            super.onReceived();
            
            ReplicatedCache service = (ReplicatedCache) getService();
            
            Member memberFrom = getFromMember();
            Member memberThis = service.getThisMember();
            if (memberFrom != memberThis && service.getUpdatingMemberSet().contains(memberFrom))
                {
                Lease lease = getLease();
            
                service.ensureCacheHandler(lease.getCacheIndex()).
                    onLeaseUpdate(lease, true, getResource(), getResourceExpiry());
                }
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ReplicatedCache$LeaseUpdateRequest
    
    /**
     * Message:
     *     LeaseUpdateRequest
     * 
     * Purpose:
     *     This Message is sent to the issuer of a Lease to request an update
     * of a resource. This message is an optimization for the sequence of
     * "LeaseLockRequest", "LeaseUpdate".  It can only be send to the Lease
     * issuer.
     * 
     * Description:
     *     In response to this mesage the Lease issuer sends the LeaseUpdate
     * message to every Member of the cluster.
     * 
     * Response to:
     *     n/a
     * 
     * Expected responses:
     *     LeaseUpdate
     * 
     * NOTE: the methods "read", "write" and "getLease" are identical to the
     * ones at ResourceMessage. We cannot inherit it since the ResourceMessage
     * is not a RequestMessage
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class LeaseUpdateRequest
            extends    com.tangosol.coherence.component.net.message.requestMessage.LeaseRequest
        {
        // ---- Fields declarations ----
        
        /**
         * Property Resource
         *
         * The resource value.
         */
        private Object __m_Resource;
        
        /**
         * Property ResourceBinary
         *
         * Binary form of the resource value; may be null.
         * 
         * This property allows the value to be "pre-serialized" to allow a
         * client thread to perform the serialization to make sure that the
         * resource is indeed serializable, and if not, to report the exception
         * to the caller.
         * 
         * Furthermore, this property allows the resource to be explicitly
         * handled as a binary without having to deserialize/reserialze.
         */
        private com.tangosol.util.Binary __m_ResourceBinary;
        
        /**
         * Property ResourceExpiry
         *
         * The resource expiry value.
         */
        private long __m_ResourceExpiry;
        
        // Default constructor
        public LeaseUpdateRequest()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public LeaseUpdateRequest(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                setMessageType(42);
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ReplicatedCache.LeaseUpdateRequest();
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
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/ReplicatedCache$LeaseUpdateRequest".replace('/', '.'));
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
         * Getter for property Lease.<p>
        * Reference to a Lease object that this request carries an information
        * about. This object is always just a copy of an actual Lease.
         */
        public com.tangosol.coherence.component.net.Lease getLease()
            {
            // import Component.Net.Lease;
            // import com.tangosol.util.Binary;
            
            Lease lease = super.getLease();
            
            // we may need the resource size for later
            Binary binResource = getResourceBinary();
            if (binResource != null)
                {
                lease.setResourceSize(binResource.length());
                }
            else
                {
                Object oResource = getResource();
                if (oResource instanceof Binary)
                    {
                    Binary binValue = (Binary) oResource;
                    lease.setResourceSize(binValue.length());
                    }
                }
            
            return lease;
            }
        
        // Accessor for the property "Resource"
        /**
         * Getter for property Resource.<p>
        * The resource value.
         */
        public Object getResource()
            {
            return __m_Resource;
            }
        
        // Accessor for the property "ResourceBinary"
        /**
         * Getter for property ResourceBinary.<p>
        * Binary form of the resource value; may be null.
        * 
        * This property allows the value to be "pre-serialized" to allow a
        * client thread to perform the serialization to make sure that the
        * resource is indeed serializable, and if not, to report the exception
        * to the caller.
        * 
        * Furthermore, this property allows the resource to be explicitly
        * handled as a binary without having to deserialize/reserialze.
         */
        public com.tangosol.util.Binary getResourceBinary()
            {
            return __m_ResourceBinary;
            }
        
        // Accessor for the property "ResourceExpiry"
        /**
         * Getter for property ResourceExpiry.<p>
        * The resource expiry value.
         */
        public long getResourceExpiry()
            {
            return __m_ResourceExpiry;
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
            
            ReplicatedCache service = (ReplicatedCache) getService();
            
            service.onLeaseUpdateRequest(this, getResource(), getResourceBinary());
            }
        
        // Declared at the super level
        public void read(com.tangosol.io.ReadBuffer.BufferInput input)
                throws java.io.IOException
            {
            // import com.tangosol.util.Base;
            // import com.tangosol.util.Binary;
            // import com.tangosol.util.ExternalizableHelper;
            
            super.read(input);
            
            setResourceExpiry(ExternalizableHelper.readLong(input));
            
            // load the binary value of the resource
            Binary binResource = new Binary(Base.read(input));
            setResourceBinary(binResource);
            
            // we have to defer the deserialization until the ClassLoader is known
            // (see CacheHandler#getCachedResource());
            // since Coherence 2.2: keep it as Binary
            setResource(binResource);
            }
        
        // Accessor for the property "Resource"
        /**
         * Setter for property Resource.<p>
        * The resource value.
         */
        public void setResource(Object oResource)
            {
            __m_Resource = oResource;
            }
        
        // Accessor for the property "ResourceBinary"
        /**
         * Setter for property ResourceBinary.<p>
        * Binary form of the resource value; may be null.
        * 
        * This property allows the value to be "pre-serialized" to allow a
        * client thread to perform the serialization to make sure that the
        * resource is indeed serializable, and if not, to report the exception
        * to the caller.
        * 
        * Furthermore, this property allows the resource to be explicitly
        * handled as a binary without having to deserialize/reserialze.
         */
        public void setResourceBinary(com.tangosol.util.Binary binResource)
            {
            __m_ResourceBinary = binResource;
            }
        
        // Accessor for the property "ResourceExpiry"
        /**
         * Setter for property ResourceExpiry.<p>
        * The resource expiry value.
         */
        public void setResourceExpiry(long cMillis)
            {
            __m_ResourceExpiry = cMillis;
            }
        
        // Declared at the super level
        public void write(com.tangosol.io.WriteBuffer.BufferOutput output)
                throws java.io.IOException
            {
            // import com.tangosol.util.Binary;
            // import com.tangosol.util.ExternalizableHelper;
            
            super.write(output);
            
            ExternalizableHelper.writeLong(output, getResourceExpiry());
            
            Binary bin = getResourceBinary();
            if (bin == null)
                {
                bin = ExternalizableHelper.toBinary(getResource(), getService().ensureSerializer());
                }
            bin.writeTo(output);
            
            // cleanup no longer needed data as soon as we can
            setResource(null);
            setResourceBinary(null);
            }
        }
    }
