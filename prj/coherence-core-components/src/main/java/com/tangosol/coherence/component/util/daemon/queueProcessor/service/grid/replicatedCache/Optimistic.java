
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.replicatedCache.Optimistic

package com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.replicatedCache;

import com.tangosol.coherence.component.net.Lease;
import com.tangosol.coherence.component.net.Member;
import java.util.Map;

/**
 * The message range from [128,256] is reserved for usage by the
 * ReplicatedCache component.
 * 
 * Currently used MessageTypes:
 * [1-128]  Reserved by ReplicatedCache
 * 129         LeaseRemoveAll
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class Optimistic
        extends    com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ReplicatedCache
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
        __mapChildren.put("Acknowledgement", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.Acknowledgement.get_CLASS());
        __mapChildren.put("BackingMapContext", com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ReplicatedCache.BackingMapContext.get_CLASS());
        __mapChildren.put("BusEventMessage", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.BusEventMessage.get_CLASS());
        __mapChildren.put("CacheHandler", Optimistic.CacheHandler.get_CLASS());
        __mapChildren.put("CacheUpdate", com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ReplicatedCache.CacheUpdate.get_CLASS());
        __mapChildren.put("CacheUpdateRequest", com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ReplicatedCache.CacheUpdateRequest.get_CLASS());
        __mapChildren.put("ConfigRequest", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.ConfigRequest.get_CLASS());
        __mapChildren.put("ConfigResponse", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.ConfigResponse.get_CLASS());
        __mapChildren.put("ConfigSync", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.ConfigSync.get_CLASS());
        __mapChildren.put("ConfigUpdate", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.ConfigUpdate.get_CLASS());
        __mapChildren.put("ConverterFromInternal", com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ReplicatedCache.ConverterFromInternal.get_CLASS());
        __mapChildren.put("ConverterToInternal", com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ReplicatedCache.ConverterToInternal.get_CLASS());
        __mapChildren.put("DispatchEvent", com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ReplicatedCache.DispatchEvent.get_CLASS());
        __mapChildren.put("DispatchNotification", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.DispatchNotification.get_CLASS());
        __mapChildren.put("LeaseIssue", com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ReplicatedCache.LeaseIssue.get_CLASS());
        __mapChildren.put("LeaseIssueRequest", com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ReplicatedCache.LeaseIssueRequest.get_CLASS());
        __mapChildren.put("LeaseLock", com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ReplicatedCache.LeaseLock.get_CLASS());
        __mapChildren.put("LeaseLockRequest", com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ReplicatedCache.LeaseLockRequest.get_CLASS());
        __mapChildren.put("LeaseRemove", com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ReplicatedCache.LeaseRemove.get_CLASS());
        __mapChildren.put("LeaseRemoveAll", Optimistic.LeaseRemoveAll.get_CLASS());
        __mapChildren.put("LeaseRemoveRequest", com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ReplicatedCache.LeaseRemoveRequest.get_CLASS());
        __mapChildren.put("LeaseUnlock", com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ReplicatedCache.LeaseUnlock.get_CLASS());
        __mapChildren.put("LeaseUnlockRequest", com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ReplicatedCache.LeaseUnlockRequest.get_CLASS());
        __mapChildren.put("LeaseUpdate", com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ReplicatedCache.LeaseUpdate.get_CLASS());
        __mapChildren.put("LeaseUpdateRequest", com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ReplicatedCache.LeaseUpdateRequest.get_CLASS());
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
    public Optimistic()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public Optimistic(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
        _addChild(new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ReplicatedCache.CatalogHandler("CatalogHandler", this, true), "CatalogHandler");
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
        return "OptimisticCache";
        }
    
    //++ getter for static property _Instance
    /**
     * Getter for property _Instance.<p>
    * Auto generated
     */
    public static com.tangosol.coherence.Component get_Instance()
        {
        return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.replicatedCache.Optimistic();
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
            clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/replicatedCache/Optimistic".replace('/', '.'));
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
    
    /**
     * Clear the cache. This method should only be called on a client's thread.
    * 
    * @param handler  the cache handler
     */
    public void clearOptimistically(com.tangosol.coherence.component.util.CacheHandler handler)
        {
        Optimistic.LeaseRemoveAll msg =
            (Optimistic.LeaseRemoveAll) instantiateMessage("LeaseRemoveAll");
        msg.setCacheIndex(handler.getCacheIndex());
        msg.ensureToMemberSet().addAll(getServiceMemberSet());
        
        // send to everyone including itself
        // and wait a signal from itself (see #onCacheClear)
        synchronized (handler)
            {
            send(msg);
        
            try
                {
                // play it safe (limiting the wait to 1 sec)
                // in case a handler is destroyed or the service is stopped
                handler.wait(1000);
                }
            catch (InterruptedException e)
                {
                Thread.currentThread().interrupt();
                }
            }
        }
    
    /**
     * Clear the cache. This method is called on the service thread and should
    * not be called externally.
    * 
    * @param iCache the cache index
     */
    public void onCacheClear(int iCache)
        {
        // import Component.Util.CacheHandler as com.tangosol.coherence.component.util.CacheHandler;
        // import java.util.Map;
        
        _assert(iCache > 0);
        
        com.tangosol.coherence.component.util.CacheHandler handler = getCacheHandler(iCache);
        if (handler != null)
            {
            Map mapLease = handler.getLeaseMap();
            try
                {
                // COH-8870: synchronize on lease map to avoid deadlock between concurrent put and clear
                synchronized (mapLease)
                    {
                    mapLease.clear();
                    }
                handler.getResourceMap().clear();
                }
            finally
                {
                synchronized (handler)
                    {
                    handler.notifyAll();
                    }
                }
            }
        }
    
    /**
     * Remove the specified resource. This method should only be called on a
    * client's thread.
    * 
    * @param handler  the cache handler
    * @param oKey  the resource key
    * 
    * @return previous resource value
     */
    public Object removeResourceOptimistically(com.tangosol.coherence.component.util.CacheHandler handler, Object oKey)
        {
        // import Component.Net.Lease;
        // import Component.Net.Member;
        
        Lease  lease      = handler.ensureLease(oKey);
        Member memberThis = getThisMember();
        
        lease.setIssuerId(memberThis.getId());
        
        Object oValueOld = handler.getCachedResource(oKey);
        
        requestRemove(lease, memberThis);
        
        return oValueOld;
        }
    
    /**
     * Update the specified resource with the specified value. This method
    * should only be called on a client's thread.
    * 
    * @param handler  the cache handler
    * @param oKey  the resource key
    * @param oValue  the resource value (Serializable or XmlSerializable); may
    * be null
    * 
    * @return previous resource value
    * 
    * @exception IllegalArgumentException  if the value is neither Serializable
    * nor XmlSerializable
     */
    public Object updateResourceOptimistically(com.tangosol.coherence.component.util.CacheHandler handler, Object oKey, Object oValue, long cExpireMillis)
        {
        // import Component.Net.Lease;
        // import Component.Net.Member;
        
        Lease  lease      = handler.ensureLease(oKey);
        Member memberThis = getThisMember();
        
        lease.setIssuerId(memberThis.getId());
        
        Object oValueOld = handler.getCachedResource(oKey);
        
        requestUpdate(lease, memberThis, oValue, cExpireMillis, true, false);
        
        return oValueOld;
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.replicatedCache.Optimistic$CacheHandler
    
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
            extends    com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ReplicatedCache.CacheHandler
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
            __mapChildren.put("BackingMapListener", com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ReplicatedCache.CacheHandler.BackingMapListener.get_CLASS());
            __mapChildren.put("EntrySet", com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ReplicatedCache.CacheHandler.EntrySet.get_CLASS());
            __mapChildren.put("KeySet", com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ReplicatedCache.CacheHandler.KeySet.get_CLASS());
            __mapChildren.put("Validator", com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ReplicatedCache.CacheHandler.Validator.get_CLASS());
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.replicatedCache.Optimistic.CacheHandler();
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
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/replicatedCache/Optimistic$CacheHandler".replace('/', '.'));
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
        public void clear()
            {
            checkAccess();
            
            ((Optimistic) get_Module()).clearOptimistically(this);
            }
        
        // Declared at the super level
        public boolean lock(Object oKey)
            {
            throw new UnsupportedOperationException();
            }
        
        // Declared at the super level
        public Object put(Object oKey, Object oValue)
            {
            checkAccess();
            
            return ((Optimistic) get_Module()).updateResourceOptimistically(this, oKey, oValue, 0L);
            }
        
        // Declared at the super level
        public Object put(Object oKey, Object oValue, long cMillis)
            {
            checkAccess();
            
            return ((Optimistic) get_Module()).updateResourceOptimistically(this, oKey, oValue, cMillis);
            }
        
        // Declared at the super level
        public Object putFinal(Object oKey, Object oValue, boolean fReturn)
            {
            return put(oKey, oValue);
            }
        
        // Declared at the super level
        public Object remove(Object oKey)
            {
            checkAccess();
            
            return ((Optimistic) get_Module()).removeResourceOptimistically(this, oKey);
            }
        
        // Declared at the super level
        public boolean unlock(Object oKey)
            {
            throw new UnsupportedOperationException();
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.replicatedCache.Optimistic$LeaseRemoveAll
    
    /**
     * Message:
     *    LeaseRemoveAll
     * 
     * Purpose:
     *     This Message informs all Members of the service that the cache
     * should be cleared.
     * 
     * Description:
     *     In response to this mesage all Members should clear their cache
     * maps.
     * 
     * Attributes:
     *    CacheIndex
     * 
     * Response to:
     *     n/a
     * Expected responses:
     *     n/a
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class LeaseRemoveAll
            extends    com.tangosol.coherence.component.net.Message
        {
        // ---- Fields declarations ----
        
        /**
         * Property CacheIndex
         *
         * Index of the corresponding Cache
         */
        private int __m_CacheIndex;
        
        // Default constructor
        public LeaseRemoveAll()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public LeaseRemoveAll(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                setMessageType(129);
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.replicatedCache.Optimistic.LeaseRemoveAll();
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
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/replicatedCache/Optimistic$LeaseRemoveAll".replace('/', '.'));
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
        
        // Accessor for the property "CacheIndex"
        /**
         * Getter for property CacheIndex.<p>
        * Index of the corresponding Cache
         */
        public int getCacheIndex()
            {
            return __m_CacheIndex;
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
            
            Optimistic service = (Optimistic) getService();
            
            service.onCacheClear(getCacheIndex());
            }
        
        // Declared at the super level
        public void read(com.tangosol.io.ReadBuffer.BufferInput input)
                throws java.io.IOException
            {
            super.read(input);
            
            setCacheIndex(input.readInt());
            }
        
        // Accessor for the property "CacheIndex"
        /**
         * Setter for property CacheIndex.<p>
        * Index of the corresponding Cache
         */
        public void setCacheIndex(int pCacheIndex)
            {
            __m_CacheIndex = pCacheIndex;
            }
        
        // Declared at the super level
        public void write(com.tangosol.io.WriteBuffer.BufferOutput output)
                throws java.io.IOException
            {
            super.write(output);
            
            output.writeInt(getCacheIndex());
            }
        }
    }
