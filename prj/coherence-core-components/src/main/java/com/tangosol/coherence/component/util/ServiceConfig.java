
/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.util.ServiceConfig

package com.tangosol.coherence.component.util;

import com.tangosol.coherence.component.net.Member;
import com.tangosol.coherence.component.net.Message;
import com.tangosol.coherence.component.net.Poll;
import com.tangosol.coherence.component.net.memberSet.actualMemberSet.ServiceMemberSet;
import com.tangosol.util.LiteMap;
import com.tangosol.util.MapListener;
import com.tangosol.util.ObservableMap;
import com.tangosol.util.WrapperObservableMap;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ServiceConfig provides a service-wide configuration map.  All updates to a
 * service config are published service-wide by the configuration coordinator.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class ServiceConfig
        extends    com.tangosol.coherence.component.Util
    {
    // ---- Fields declarations ----
    
    /**
     * Property Map
     *
     * Wrapped Map
     */
    private ServiceConfig.Map __m_Map;
    
    /**
     * Property PendingConfigUpdates
     *
     * A list of received ConfigSync/ConfigUpdate messages that are waiting to
     * be processed.  ServiceConfig messages are queued up if they are received
     * from a member other than the current coordinator until notification
     * arrives of the old coordinator's departure.
     */
    private java.util.List __m_PendingConfigUpdates;
    
    /**
     * Property PendingPolls
     *
     * A Map of pending "ConfigRequest" polls keyed by corresponding resource
     * keys. An Entry is held in this map only until the corresponding poll is
     * closed. 
     * 
     * In the meantime, all "ConfigUpdate" messages that come for a key that
     * has a pending poll are ignored. This strategy solves a problem of
     * simultaneous update requests for the same key issued by different nodes.
     */
    private java.util.Map __m_PendingPolls;
    
    // Default constructor
    public ServiceConfig()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public ServiceConfig(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            // identified pendingPolls required thread-safe data structure and
            // verified that pendingConfigUpdates only accessed on single service thread, see details in COH-30132.
            setPendingConfigUpdates(new java.util.LinkedList());
            setPendingPolls(new ConcurrentHashMap<>());
            }
        catch (java.lang.Exception e)
            {
            // re-throw as a runtime exception
            throw new com.tangosol.util.WrapperException(e);
            }
        
        // containment initialization: children
        _addChild(new ServiceConfig.ConfigListener("ConfigListener", this, true), "ConfigListener");
        _addChild(new ServiceConfig.Map("Map", this, true), "Map");
        
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
        return new com.tangosol.coherence.component.util.ServiceConfig();
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
            clz = Class.forName("com.tangosol.coherence/component/util/ServiceConfig".replace('/', '.'));
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
    
    /**
     * Subscribe the $ConfigListener to config map updates.
     */
    public void attachConfigListener()
        {
        // import com.tangosol.util.MapListener;
        
        getMap().addMapListener((MapListener) _findChild("ConfigListener"));
        }
    
    // Accessor for the property "ConfigCoordinator"
    /**
     * Getter for property ConfigCoordinator.<p>
    * The configuration coordinator is the service member that serves as a
    * point of serialization for configuration updates.
     */
    public com.tangosol.coherence.component.net.Member getConfigCoordinator()
        {
        // default implementation returns the service senior
        return getService().getServiceMemberSet().getOldestMember();
        }
    
    // Accessor for the property "Map"
    /**
     * Getter for property Map.<p>
    * Wrapped Map
     */
    public ServiceConfig.Map getMap()
        {
        return __m_Map;
        }
    
    // Accessor for the property "PendingConfigUpdates"
    /**
     * Getter for property PendingConfigUpdates.<p>
    * A list of received ConfigSync/ConfigUpdate messages that are waiting to
    * be processed.  ServiceConfig messages are queued up if they are received
    * from a member other than the current coordinator until notification
    * arrives of the old coordinator's departure.
     */
    public java.util.List getPendingConfigUpdates()
        {
        return __m_PendingConfigUpdates;
        }
    
    // Accessor for the property "PendingPolls"
    /**
     * Getter for property PendingPolls.<p>
    * A Map of pending "ConfigRequest" polls keyed by corresponding resource
    * keys. An Entry is held in this map only until the corresponding poll is
    * closed. 
    * 
    * In the meantime, all "ConfigUpdate" messages that come for a key that has
    * a pending poll are ignored. This strategy solves a problem of
    * simultaneous update requests for the same key issued by different nodes.
     */
    public java.util.Map getPendingPolls()
        {
        return __m_PendingPolls;
        }
    
    // Accessor for the property "Service"
    /**
     * Getter for property Service.<p>
     */
    public com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid getService()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid;
        
        return (com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid) get_Parent();
        }
    
    // Declared at the super level
    /**
     * The "component has been initialized" method-notification called out of
    * setConstructed() for the topmost component and that in turn notifies all
    * the children.
    * 
    * This notification gets called before the control returns back to this
    * component instantiator (using <code>new Component.X()</code> or
    * <code>_newInstance(sName)</code>) and on the same thread. In addition,
    * visual components have a "posted" notification <code>onInitUI</code> that
    * is called after (or at the same time as) the control returns back to the
    * instantiator and possibly on a different thread.
     */
    public void onInit()
        {
        super.onInit();
        
        setMap((ServiceConfig.Map) _findChild("Map"));
        }
    
    /**
     * Called when no config coordinator member is found to receive config
    * requests to update the map.
     */
    public void onMissingCoordinator()
        {
        throw new IllegalStateException();
        }
    
    /**
     * Deserialize a ConfigMap related object from the specified DataInput.
    * 
    * @param in  the DataInput containing a serialized object
    * 
    * @return the deserialized object
     */
    public Object readObject(java.io.DataInput in)
            throws java.io.IOException
        {
        return getService().readObject(in);
        }
    
    /**
     * Unsbscribe the $ConfigListener from receiving config map updates.
     */
    public void removeConfigListener()
        {
        // import com.tangosol.util.MapListener;
        
        getMap().removeMapListener((MapListener) _findChild("ConfigListener"));
        }
    
    // Accessor for the property "Map"
    /**
     * Setter for property Map.<p>
    * Wrapped Map
     */
    protected void setMap(ServiceConfig.Map map)
        {
        __m_Map = map;
        }
    
    // Accessor for the property "PendingConfigUpdates"
    /**
     * Setter for property PendingConfigUpdates.<p>
    * A list of received ConfigSync/ConfigUpdate messages that are waiting to
    * be processed.  ServiceConfig messages are queued up if they are received
    * from a member other than the current coordinator until notification
    * arrives of the old coordinator's departure.
     */
    protected void setPendingConfigUpdates(java.util.List mapPending)
        {
        __m_PendingConfigUpdates = mapPending;
        }
    
    // Accessor for the property "PendingPolls"
    /**
     * Setter for property PendingPolls.<p>
    * A Map of pending "ConfigRequest" polls keyed by corresponding resource
    * keys. An Entry is held in this map only until the corresponding poll is
    * closed. 
    * 
    * In the meantime, all "ConfigUpdate" messages that come for a key that has
    * a pending poll are ignored. This strategy solves a problem of
    * simultaneous update requests for the same key issued by different nodes.
     */
    protected void setPendingPolls(java.util.Map mapPending)
        {
        __m_PendingPolls = mapPending;
        }
    
    /**
     * Serialize a ConfigMap related object to the specified DataOutput.
    * 
    * @param out  the DataOutput
    * @param o  the object to serialize
     */
    public void writeObject(java.io.DataOutput out, Object o)
            throws java.io.IOException
        {
        getService().writeObject(out, o);
        }

    // ---- class: com.tangosol.coherence.component.util.ServiceConfig$ConfigListener
    
    /**
     * ConfigListener is used to receive config map updates for this
     * ServiceConfig.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class ConfigListener
            extends    com.tangosol.coherence.component.Util
            implements com.tangosol.util.MapListener
        {
        // ---- Fields declarations ----
        
        // Default constructor
        public ConfigListener()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public ConfigListener(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.util.ServiceConfig.ConfigListener();
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
                clz = Class.forName("com.tangosol.coherence/component/util/ServiceConfig$ConfigListener".replace('/', '.'));
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
        
        // From interface: com.tangosol.util.MapListener
        public void entryDeleted(com.tangosol.util.MapEvent evt)
            {
            }
        
        // From interface: com.tangosol.util.MapListener
        public void entryInserted(com.tangosol.util.MapEvent evt)
            {
            }
        
        // From interface: com.tangosol.util.MapListener
        public void entryUpdated(com.tangosol.util.MapEvent evt)
            {
            }
        }

    // ---- class: com.tangosol.coherence.component.util.ServiceConfig$Map
    
    /**
     * The "live" configuration map.  Mutations on this Map through the
     * java.util.Map interface will be published to all members sharing the
     * ServiceConfig.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class Map
            extends    com.tangosol.coherence.component.util.collections.WrapperMap
            implements com.tangosol.util.ObservableMap
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
            __mapChildren.put("EntrySet", ServiceConfig.Map.EntrySet.get_CLASS());
            __mapChildren.put("KeySet", ServiceConfig.Map.KeySet.get_CLASS());
            __mapChildren.put("Values", ServiceConfig.Map.Values.get_CLASS());
            }
        
        // Default constructor
        public Map()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public Map(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.util.ServiceConfig.Map();
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
                clz = Class.forName("com.tangosol.coherence/component/util/ServiceConfig$Map".replace('/', '.'));
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
        
        // From interface: com.tangosol.util.ObservableMap
        public void addMapListener(com.tangosol.util.MapListener listener)
            {
            // import com.tangosol.util.ObservableMap;
            
            ((ObservableMap) getMap()).addMapListener(listener);
            }
        
        // From interface: com.tangosol.util.ObservableMap
        public void addMapListener(com.tangosol.util.MapListener listener, com.tangosol.util.Filter filter, boolean fLite)
            {
            // import com.tangosol.util.ObservableMap;
            
            ((ObservableMap) getMap()).addMapListener(listener, filter, fLite);
            }
        
        // From interface: com.tangosol.util.ObservableMap
        public void addMapListener(com.tangosol.util.MapListener listener, Object oKey, boolean fLite)
            {
            // import com.tangosol.util.ObservableMap;
            
            ((ObservableMap) getMap()).addMapListener(listener, oKey, fLite);
            }
        
        // From interface: com.tangosol.util.ObservableMap
        // Declared at the super level
        public void clear()
            {
            // import java.util.Iterator;
            // import java.util.Map$Entry as java.util.Map.Entry;
            
            for (Iterator iter = entrySet().iterator(); iter.hasNext(); )
                {
                java.util.Map.Entry entry = (java.util.Map.Entry) iter.next();
                remove(entry.getKey());
                }
            }
        
        /**
         * Clear any pending config requests (polls) on this service config.
         */
        public void clearPendingPolls()
            {
            getConfig().getPendingPolls().clear();
            }
        
        /**
         * Defer the specified ConfigUpdate or ConfigSync message. Processing of
        * config updates/syncs received from an unexpected config coordinator
        * are deferred until membership notifications are received.
        * @see #onServiceLeft().
        * 
        * @param message  the ConfigUpdate or ConfigSync message to defer
         */
        public void deferConfigUpdate(com.tangosol.coherence.component.net.Message message)
            {
            getConfig().getPendingConfigUpdates().add(message);
            }
        
        // Accessor for the property "Config"
        /**
         * Getter for property Config.<p>
        * The ServiceConfig that this map belongs to.
         */
        public ServiceConfig getConfig()
            {
            return (ServiceConfig) get_Module();
            }
        
        // Accessor for the property "ConfigCoordinator"
        /**
         * Getter for property ConfigCoordinator.<p>
        * @see ServiceConfig#ConfigCoordinator
         */
        public com.tangosol.coherence.component.net.Member getConfigCoordinator()
            {
            return getConfig().getConfigCoordinator();
            }
        
        // Accessor for the property "MapType"
        /**
         * Getter for property MapType.<p>
        * An integer value, unique to the Service using this map, that defines
        * the type of this config map.
        * 
        * @see Grid#getConfigMap(int)
         */
        public int getMapType()
            {
            return 0;
            }
        
        // Accessor for the property "PendingConfigRequestCount"
        /**
         * Getter for property PendingConfigRequestCount.<p>
        * The number of outstanding pending polls on this config map.
        * @see ServiceConfig#PendingPolls
         */
        public int getPendingConfigRequestCount()
            {
            return getConfig().getPendingPolls().size();
            }
        
        // Accessor for the property "Service"
        /**
         * Getter for property Service.<p>
        * @see ServiceConfig#Service
         */
        public com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid getService()
            {
            return getConfig().getService();
            }
        
        /**
         * Return true iff there is a pending config request (poll) for the
        * specified key.
         */
        public boolean isRequestPending(Object oKey)
            {
            return getConfig().getPendingPolls().containsKey(oKey);
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
            // import com.tangosol.util.WrapperObservableMap;
            // import java.util.concurrent.ConcurrentHashMap;
            
            super.onInit();
            
            // while the actual performance measurements are inconclusive,
            // JProfiler shows an advantage of ConcurrentHashMap over ObservableHashMap
            setMap(new WrapperObservableMap(new ConcurrentHashMap()));
            }
        
        /**
         * Called when a member has left the service that this config map is
        * associated with.
        * Called on the service thread only.
        * 
        * @param member   the member that has left the service
        * @param fCoordinator  indicates whether the member that has left was a
        * configuration coordinator
         */
        public void onServiceLeft(com.tangosol.coherence.component.net.Member member, boolean fCoordinator)
            {
            // import Component.Net.Member;
            // import Component.Net.MemberSet.ActualMemberSet.ServiceMemberSet;
            // import Component.Net.Message;
            // import Component.Util.Daemon.QueueProcessor.Service.Grid as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid;
            // import java.util.Iterator;
            
            com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid          service           = getService();
            ServiceMemberSet setService        = service.getServiceMemberSet();
            ServiceConfig          config            = (ServiceConfig) get_Module();
            Member           memberCoordinator = getConfigCoordinator();
            
            // process any queued up ConfigSyncs or ConfigUpdates
            if (config.getPendingConfigUpdates().size() > 0)
                {
                for (Iterator iter = getConfig().getPendingConfigUpdates().iterator();
                     iter.hasNext(); )
                    {
                    Message msgDeferred = (Message) iter.next();
                    Member  memberFrom  = msgDeferred.getFromMember();
                    
                    if (memberFrom == memberCoordinator)
                        {           
                        // process any queued up updates from the new coordinator
                        msgDeferred.onReceived();
                        iter.remove();
                        }
                    else if (setService.contains(memberFrom))
                        {
                        // If we got a ConfigUpdate/ConfigSync from a different service
                        // member, either we still haven't received notification of the
                        // original coordinator leaving, or it must be yet another new
                        // coordinator. In either case, put off processing for the rest
                        // of the queue.
                        break;
                        }
                    else
                        {
                        _trace("Ignoring stale " + msgDeferred.get_Name() +
                               " from " + memberFrom, 3);
                        iter.remove();
                        }
                    }
                }
            
            // check if this Member is now the config coordinator
            // and the member that left was coordinator before
            Member memberThis = service.getThisMember();
            if (memberCoordinator == memberThis && fCoordinator)
                {
                publishConfig(null);
                }
            }
        
        /**
         * Publish the configuration (via "sync") to the specified member (or
        * service-wide).
        * Called on the service thread only.
        * 
        * @param member  the member to publish the config to, or 'null' to
        * publish to all service members
         */
        public void publishConfig(com.tangosol.coherence.component.net.Member member)
            {
            // import Component.Util.Daemon.QueueProcessor.Service.Grid as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid;
            // import Component.Util.Daemon.QueueProcessor.Service.Grid$ConfigSync as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.ConfigSync;
            
            com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid    service = getService();
            com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.ConfigSync msg     = (com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.ConfigSync) service.instantiateMessage("ConfigSync");
            String     sConfig = getConfig().get_Name();
            
            if (member == null)
                {    
                _trace("Service " + service.getServiceName() +
                       ": sending " + sConfig + " com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.ConfigSync to all", 3);
                msg.setToMemberSet(service.getOthersMemberSet());
                }
            else
                {
                _trace("Service " + service.getServiceName() +
                       ": sending " + sConfig + " com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.ConfigSync containing " +
                       size() + " entries to Member "+ member.getId(), 3);
                msg.addToMember(member);
                }
            msg.setConfigMap(this);
            msg.setSyncMap(getMap());
            
            service.send(msg);
            }
        
        // From interface: com.tangosol.util.ObservableMap
        // Declared at the super level
        public Object put(Object oKey, Object oValue)
            {
            // import Component.Net.Member;
            // import Component.Util.Daemon.QueueProcessor.Service.Grid as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid;
            // import Component.Util.Daemon.QueueProcessor.Service.Grid$ConfigRequest as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.ConfigRequest;
            // import com.tangosol.util.LiteMap;
            // import java.util.Iterator as java.util.Iterator;
            // import java.util.Map as java.util.Map;
            
            java.util.Map mapUpdate = new LiteMap();
            mapUpdate.put(oKey, oValue);
            
            java.util.Map mapLocal = getMap();
            synchronized (mapLocal)
                {
                Member memberCoordinator = getConfigCoordinator();
                if (memberCoordinator == null)
                    {
                    getConfig().onMissingCoordinator();
                    }
                else
                    {
                    // Send the update request first to prevent a recursive
                    // call by a config-map listener that changes the value
                    // of one of the keys from triggering an out-of-order
                    // update request.
                    com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid service = getService();
            
                    // request change to be published service-wide by the coordinator
                    com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.ConfigRequest msgRequest = (com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.ConfigRequest)
                        service.instantiateMessage("ConfigRequest");
                    msgRequest.setConfigMap(this);
                    msgRequest.setUpdateMap(mapUpdate);
                    msgRequest.addToMember(memberCoordinator);
            
                    // register a pending-poll
                    ((ServiceConfig) get_Module()).getPendingPolls().put(oKey, msgRequest.ensureRequestPoll());
            
                    // send the config request (poll)
                    service.send(msgRequest);
                    }
                
                // lastly, update the local copy
                return updateInternal(oKey, oValue, false);
                }
            }
        
        // From interface: com.tangosol.util.ObservableMap
        // Declared at the super level
        public void putAll(java.util.Map map)
            {
            // import Component.Net.Member;
            // import Component.Net.Poll;
            // import Component.Util.Daemon.QueueProcessor.Service.Grid as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid;
            // import Component.Util.Daemon.QueueProcessor.Service.Grid$ConfigRequest as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.ConfigRequest;
            // import java.util.Iterator as java.util.Iterator;
            // import java.util.Map as java.util.Map;
            
            java.util.Map mapLocal = getMap();
            synchronized (mapLocal)
                {
                Member memberCoordinator = getConfigCoordinator();
                
                if (memberCoordinator == null)
                    {
                    getConfig().onMissingCoordinator();
                    }
                else
                    {
                    // Send the update request first to prevent a recursive
                    // call by a config-map listener that changes the value
                    // of one of the keys from triggering an out-of-order
                    // update request.
                    com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid service = getService();
            
                    // request change to be published service-wide by the coordinator
                    com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.ConfigRequest msgRequest = (com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.ConfigRequest)
                        service.instantiateMessage("ConfigRequest");
                    msgRequest.setConfigMap(this);
                    msgRequest.setUpdateMap(map);
                    msgRequest.addToMember(memberCoordinator);
            
                    // register a pending-poll for each key involved in the update
                    Poll poll       = msgRequest.ensureRequestPoll();
                    java.util.Map  mapPending = ((ServiceConfig) get_Module()).getPendingPolls();
                    for (java.util.Iterator iter = map.keySet().iterator(); iter.hasNext(); )
                        {
                        Object oKey = iter.next();
                        mapPending.put(oKey, poll);
                        }
            
                    // send the config request (poll)
                    service.send(msgRequest);
                    }
                
                // lastly, update the local copy
                updateInternal(map, false);
                }
            }
        
        /**
         * Deserialize a ConfigMap related object from the specified DataInput.
        * 
        * @param in  the DataInput containing a serialized object
        * 
        * @return the deserialized object
         */
        public Object readObject(java.io.DataInput stream)
                throws java.io.IOException
            {
            return getConfig().readObject(stream);
            }
        
        // From interface: com.tangosol.util.ObservableMap
        // Declared at the super level
        public Object remove(Object oKey)
            {
            // import Component.Net.Member;
            // import Component.Util.Daemon.QueueProcessor.Service.Grid as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid;
            // import Component.Util.Daemon.QueueProcessor.Service.Grid$ConfigRequest as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.ConfigRequest;
            // import com.tangosol.util.LiteMap;
            // import java.util.Map as java.util.Map;
            
            java.util.Map map = getMap();
            synchronized (map)
                {
                if (containsKey(oKey))
                    {
                    Object oOrig             = updateInternal(oKey, null, true);
                    Member memberCoordinator = getConfigCoordinator();
            
                    if (memberCoordinator == null)
                        {
                        getConfig().onMissingCoordinator();
                        }
                    else
                        {
                        // check if the value is still removed to prevent a recursive
                        // call by a ServiceConfigMap listener changing the value;
                        // that would reverse the orderof updates and make the content
                        // of this map unsynchronized with the rest of the nodes
                        if (map.containsKey(oKey))
                            {
                            _trace("Recursive ConfigMap.remove() call for key=" + oKey
                                 + " was replaced by " + map.get(oKey), 4);
                            }
                        else
                            {
                            // request change to be published cluster-wide by the coordinator
                            ServiceConfig       config  = (ServiceConfig) get_Module();
                            com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid       service = config.getService();
                            com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.ConfigRequest msg     = (com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.ConfigRequest)
                                    service.instantiateMessage("ConfigRequest");
                            msg.addToMember(memberCoordinator);
            
                            java.util.Map mapUpdate = new LiteMap();
                            mapUpdate.put(oKey, null);
                            msg.setConfigMap(this);
                            msg.setUpdateMap(mapUpdate);
                            msg.setRemove(true);
            
                            config.getPendingPolls().put(oKey, msg.ensureRequestPoll());
                            service.send(msg); // poll
                            }
                        }
                    return oOrig;
                    }
                else
                    {
                    return null;
                    }
                }
            }
        
        // From interface: com.tangosol.util.ObservableMap
        public void removeMapListener(com.tangosol.util.MapListener listener)
            {
            // import com.tangosol.util.ObservableMap;
            
            ((ObservableMap) getMap()).removeMapListener(listener);
            }
        
        // From interface: com.tangosol.util.ObservableMap
        public void removeMapListener(com.tangosol.util.MapListener listener, com.tangosol.util.Filter filter)
            {
            // import com.tangosol.util.ObservableMap;
            
            ((ObservableMap) getMap()).removeMapListener(listener, filter);
            }
        
        // From interface: com.tangosol.util.ObservableMap
        public void removeMapListener(com.tangosol.util.MapListener listener, Object oKey)
            {
            // import com.tangosol.util.ObservableMap;
            
            ((ObservableMap) getMap()).removeMapListener(listener, oKey);
            }
        
        /**
         * Update the local contents of the config map, returning the previously
        * associated value.  Called only on the service thread.
        * 
        * @param oKey         the key to update
        * @param oValue      the associated value (or null if fRemove)
        * @param fRemove   true iff the specified key should be removed
         */
        public Object updateInternal(Object oKey, Object oValue, boolean fRemove)
            {
            // import java.util.Map as java.util.Map;
            
            java.util.Map mapInternal = getMap();
            synchronized (mapInternal)
                {
                return fRemove ? mapInternal.remove(oKey) : mapInternal.put(oKey, oValue);
                }
            }
        
        /**
         * Update the local contents of the config map.  Called only on the
        * service thread.
        * 
        * @param mapUpdate  the mappings to update the config map with
        * @param fRemove      true iff the specified keys should be removed
         */
        public void updateInternal(java.util.Map mapUpdate, boolean fRemove)
            {
            // import java.util.Iterator;
            // import java.util.Map$Entry as java.util.Map.Entry;
            
            synchronized (getMap())
                {
                for (Iterator iter = mapUpdate.entrySet().iterator(); iter.hasNext(); )
                    {
                    java.util.Map.Entry entry = (java.util.Map.Entry) iter.next();
            
                    updateInternal(entry.getKey(), entry.getValue(), fRemove);
                    }
                }
            }
        
        /**
         * Serialize a ConfigMap related object to the specified DataOutput.
        * 
        * @param out  the DataOutput
        * @param o  the object to serialize
         */
        public void writeObject(java.io.DataOutput stream, Object object)
                throws java.io.IOException
            {
            getConfig().writeObject(stream, object);
            }

        // ---- class: com.tangosol.coherence.component.util.ServiceConfig$Map$EntrySet
        
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class EntrySet
                extends    com.tangosol.coherence.component.util.collections.WrapperMap.EntrySet
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
                __mapChildren.put("Entry", ServiceConfig.Map.EntrySet.Entry.get_CLASS());
                __mapChildren.put("Iterator", ServiceConfig.Map.EntrySet.Iterator.get_CLASS());
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
                return new com.tangosol.coherence.component.util.ServiceConfig.Map.EntrySet();
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
                    clz = Class.forName("com.tangosol.coherence/component/util/ServiceConfig$Map$EntrySet".replace('/', '.'));
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

            // ---- class: com.tangosol.coherence.component.util.ServiceConfig$Map$EntrySet$Entry
            
            @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
            public static class Entry
                    extends    com.tangosol.coherence.component.util.collections.WrapperMap.EntrySet.Entry
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
                    return new com.tangosol.coherence.component.util.ServiceConfig.Map.EntrySet.Entry();
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
                        clz = Class.forName("com.tangosol.coherence/component/util/ServiceConfig$Map$EntrySet$Entry".replace('/', '.'));
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

            // ---- class: com.tangosol.coherence.component.util.ServiceConfig$Map$EntrySet$Iterator
            
            @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
            public static class Iterator
                    extends    com.tangosol.coherence.component.util.collections.WrapperMap.EntrySet.Iterator
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
                    return new com.tangosol.coherence.component.util.ServiceConfig.Map.EntrySet.Iterator();
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
                        clz = Class.forName("com.tangosol.coherence/component/util/ServiceConfig$Map$EntrySet$Iterator".replace('/', '.'));
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

        // ---- class: com.tangosol.coherence.component.util.ServiceConfig$Map$KeySet
        
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class KeySet
                extends    com.tangosol.coherence.component.util.collections.WrapperMap.KeySet
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
                __mapChildren.put("Iterator", ServiceConfig.Map.KeySet.Iterator.get_CLASS());
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
                return new com.tangosol.coherence.component.util.ServiceConfig.Map.KeySet();
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
                    clz = Class.forName("com.tangosol.coherence/component/util/ServiceConfig$Map$KeySet".replace('/', '.'));
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

            // ---- class: com.tangosol.coherence.component.util.ServiceConfig$Map$KeySet$Iterator
            
            @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
            public static class Iterator
                    extends    com.tangosol.coherence.component.util.collections.WrapperMap.KeySet.Iterator
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
                    return new com.tangosol.coherence.component.util.ServiceConfig.Map.KeySet.Iterator();
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
                        clz = Class.forName("com.tangosol.coherence/component/util/ServiceConfig$Map$KeySet$Iterator".replace('/', '.'));
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

        // ---- class: com.tangosol.coherence.component.util.ServiceConfig$Map$Values
        
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class Values
                extends    com.tangosol.coherence.component.util.collections.WrapperMap.Values
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
                __mapChildren.put("Iterator", ServiceConfig.Map.Values.Iterator.get_CLASS());
                }
            
            // Default constructor
            public Values()
                {
                this(null, null, true);
                }
            
            // Initializing constructor
            public Values(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                return new com.tangosol.coherence.component.util.ServiceConfig.Map.Values();
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
                    clz = Class.forName("com.tangosol.coherence/component/util/ServiceConfig$Map$Values".replace('/', '.'));
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

            // ---- class: com.tangosol.coherence.component.util.ServiceConfig$Map$Values$Iterator
            
            @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
            public static class Iterator
                    extends    com.tangosol.coherence.component.util.collections.WrapperMap.Values.Iterator
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
                    return new com.tangosol.coherence.component.util.ServiceConfig.Map.Values.Iterator();
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
                        clz = Class.forName("com.tangosol.coherence/component/util/ServiceConfig$Map$Values$Iterator".replace('/', '.'));
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
        }
    }
