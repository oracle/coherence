
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.Acceptor

package com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer;

import com.tangosol.coherence.component.net.extend.Channel;
import com.tangosol.coherence.component.net.extend.Connection;
import com.tangosol.internal.net.service.peer.acceptor.AcceptorDependencies;
import com.tangosol.internal.net.service.peer.acceptor.GrpcAcceptorDependencies;
import com.tangosol.internal.net.service.peer.acceptor.HttpAcceptorDependencies;
import com.tangosol.internal.net.service.peer.acceptor.JmsAcceptorDependencies;
import com.tangosol.internal.net.service.peer.acceptor.MemcachedAcceptorDependencies;
import com.tangosol.internal.net.service.peer.acceptor.TcpAcceptorDependencies;
import com.tangosol.net.messaging.ConnectionException;
import com.tangosol.net.messaging.ConnectionFilter;
import com.tangosol.util.UUID;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

/**
 * Base definition of a ConnectionAcceptor component.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public abstract class Acceptor
        extends    com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer
        implements com.tangosol.net.messaging.ConnectionAcceptor
    {
    // ---- Fields declarations ----
    
    /**
     * Property AcceptingConnections
     *
     * If true, this Acceptor will accept new connections; otherwise, new
     * connection attempts will be denied.
     * 
     * @volatile
     */
    private volatile boolean __m_AcceptingConnections;
    
    /**
     * Property ConnectionFilter
     *
     * The filter used by this Acceptor to evaluate whether or not to accept a
     * connection.
     */
    private com.tangosol.net.messaging.ConnectionFilter __m_ConnectionFilter;
    
    /**
     * Property ConnectionLimit
     *
     * The maximum number of simultaneous connections allowed by the Acceptor.
     * A value of 0 implies no limit.
     */
    private int __m_ConnectionLimit;
    
    /**
     * Property ConnectionPendingSet
     *
     * The Set Connection objects that have been accepted but not yet opened.
     */
    private java.util.Set __m_ConnectionPendingSet;
    
    /**
     * Property ConnectionSet
     *
     * The Set of open Connection objects managed by this Acceptor.
     */
    private java.util.Set __m_ConnectionSet;
    private static com.tangosol.util.ListMap __mapChildren;
    
    private static void _initStatic$Default()
        {
        __initStatic();
        }
    
    // Static initializer (from _initStatic)
    static
        {
        _initStatic$Default();
        
        // license suport
        try
            {
            new com.tangosol.license.CoherenceCachingEdition();
            }
        catch (RuntimeException e)
            {
            setLicenseError(e.getMessage());
            }
        }
    
    // Default static initializer
    private static void __initStatic()
        {
        // register child classes
        __mapChildren = new com.tangosol.util.ListMap();
        __mapChildren.put("DispatchEvent", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.DispatchEvent.get_CLASS());
        __mapChildren.put("MessageFactory", Acceptor.MessageFactory.get_CLASS());
        __mapChildren.put("Queue", com.tangosol.coherence.component.util.daemon.QueueProcessor.Queue.get_CLASS());
        }
    
    // Initializing constructor
    public Acceptor(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
        {
        super(sName, compParent, false);
        }
    
    // Private initializer
    protected void __initPrivate()
        {
        
        super.__initPrivate();
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
            clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/peer/Acceptor".replace('/', '.'));
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
    /**
     * Check the Connection(s) managed by this ConnectionManager for a ping
    * timeout. A Connection that has not received a PingResponse for an
    * oustanding PingRequest within the configured PingTimeout will be closed.
     */
    protected void checkPingTimeouts()
        {
        // import Component.Net.Extend.Connection;
        // import java.util.Iterator;
        
        for (Iterator iter = getConnectionSet().iterator(); iter.hasNext(); )
            {
            Connection connection = (Connection) iter.next();
            checkPingTimeout(connection);
            }
        }
    
    /**
     * Factory method: create and configure a new ConnectionAcceptor for the
    * given dependencies.
    * 
    * @param deps  the AcceptorDependencies used to create a new
    * ConnectionAcceptor
    * @param ctx     the OperationalContext for the new ConnectionAcceptor
    * 
    * @return a new ConnectionAcceptor
     */
    public static com.tangosol.net.messaging.ConnectionAcceptor createAcceptor(com.tangosol.internal.net.service.peer.acceptor.AcceptorDependencies deps, com.tangosol.net.OperationalContext ctx)
        {
        // import com.tangosol.internal.net.service.peer.acceptor.GrpcAcceptorDependencies;
        // import com.tangosol.internal.net.service.peer.acceptor.HttpAcceptorDependencies;
        // import com.tangosol.internal.net.service.peer.acceptor.JmsAcceptorDependencies;
        // import com.tangosol.internal.net.service.peer.acceptor.MemcachedAcceptorDependencies;
        // import com.tangosol.internal.net.service.peer.acceptor.TcpAcceptorDependencies;
        
        Acceptor acceptor;
        if (deps instanceof HttpAcceptorDependencies)
            {
            acceptor = (Acceptor) _newInstance(
                    "Component.Util.Daemon.QueueProcessor.Service.Peer.Acceptor.HttpAcceptor");
            }
        else if (deps instanceof JmsAcceptorDependencies)
            {
            acceptor = (Acceptor) _newInstance(
                    "Component.Util.Daemon.QueueProcessor.Service.Peer.Acceptor.JmsAcceptor");
            }
        else if (deps instanceof TcpAcceptorDependencies)
            {
            acceptor = (Acceptor) _newInstance(
                    "Component.Util.Daemon.QueueProcessor.Service.Peer.Acceptor.TcpAcceptor");
            }
        else if (deps instanceof MemcachedAcceptorDependencies)
            {
            acceptor = (Acceptor) _newInstance(
                    "Component.Util.Daemon.QueueProcessor.Service.Peer.Acceptor.MemcachedAcceptor");
            }
        else if (deps instanceof GrpcAcceptorDependencies)
            {
            acceptor = (Acceptor) _newInstance(
                    "Component.Util.Daemon.QueueProcessor.Service.Peer.Acceptor.GrpcAcceptor");
            }
        else
            {
            throw new IllegalArgumentException("unsupported acceptor dependencies :\n"
                    + deps);
            }
        
        acceptor.setOperationalContext(ctx);
        acceptor.setDependencies(deps);
        return acceptor;
        }
    
    // Declared at the super level
    /**
     * @return a human-readable description of the Service statistics
     */
    public String formatStats()
        {
        return "Connections=" + getConnectionSet().size() + ", " + super.formatStats();
        }
    
    // From interface: com.tangosol.net.messaging.ConnectionAcceptor
    // Accessor for the property "ConnectionFilter"
    /**
     * Getter for property ConnectionFilter.<p>
    * The filter used by this Acceptor to evaluate whether or not to accept a
    * connection.
     */
    public com.tangosol.net.messaging.ConnectionFilter getConnectionFilter()
        {
        return __m_ConnectionFilter;
        }
    
    // Accessor for the property "ConnectionLimit"
    /**
     * Getter for property ConnectionLimit.<p>
    * The maximum number of simultaneous connections allowed by the Acceptor. A
    * value of 0 implies no limit.
     */
    public int getConnectionLimit()
        {
        return __m_ConnectionLimit;
        }
    
    // Accessor for the property "ConnectionPendingSet"
    /**
     * Getter for property ConnectionPendingSet.<p>
    * The Set Connection objects that have been accepted but not yet opened.
     */
    public java.util.Set getConnectionPendingSet()
        {
        return __m_ConnectionPendingSet;
        }
    
    // From interface: com.tangosol.net.messaging.ConnectionAcceptor
    public java.util.Collection getConnections()
        {
        // import java.util.Collections;
        
        return Collections.unmodifiableSet(getConnectionSet());
        }
    
    // Accessor for the property "ConnectionSet"
    /**
     * Getter for property ConnectionSet.<p>
    * The Set of open Connection objects managed by this Acceptor.
     */
    public java.util.Set getConnectionSet()
        {
        return __m_ConnectionSet;
        }
    
    // Declared at the super level
    /**
     * Getter for property DaemonPool.<p>
    * The daemon pool.
    * 
    * @see #configure(XmlElement)
     */
    public com.tangosol.coherence.component.util.DaemonPool getDaemonPool()
        {
        return (Acceptor.DaemonPool) _findChild("DaemonPool");
        }
    
    // Declared at the super level
    /**
     * Getter for property Description.<p>
    * Human-readable description of additional Service properties. Used by
    * toString().
     */
    public String getDescription()
        {
        int cLimit = getConnectionLimit();
        if (cLimit > 0)
            {
            return super.getDescription() + ", ConnectionLimit=" + cLimit;
            }
        else
            {
            return super.getDescription();
            }
        }
    
    // Accessor for the property "AcceptingConnections"
    /**
     * Getter for property AcceptingConnections.<p>
    * If true, this Acceptor will accept new connections; otherwise, new
    * connection attempts will be denied.
    * 
    * @volatile
     */
    public boolean isAcceptingConnections()
        {
        int cLimit = getConnectionLimit();
        if (cLimit > 0 && getConnectionSet().size() >= cLimit)
            {
            return false;
            }
        
        return __m_AcceptingConnections;
        }
    
    /**
     * Negotiate an appropriate set of Protocols for the given Connection.
    * 
    * @param connection  the Connection that is being accepted
    * @param mapProtocols  map of required Protocols; the keys are the names of
    * the required Protocols and the values are the required version numbers or
    * range of version numbers of the corresponding Protocol
    * 
    * @return a map of negotiated Protocols; the keys are the names of the
    * Protocols and the values are the negotiated version numbers
    * 
    * @throws RuntimeException  if an appropriate set of Protocols could not be
    * negotiated
     */
    public java.util.Map negotiateProtocols(com.tangosol.coherence.component.net.extend.Connection connection, java.util.Map mapRequired)
        {
        // import com.tangosol.net.messaging.Protocol as com.tangosol.net.messaging.Protocol;
        // import java.util.Collections;
        // import java.util.HashMap;
        // import java.util.Iterator;
        // import java.util.Map;
        // import java.util.Map$Entry as java.util.Map.Entry;
        
        _assert(connection != null);
        _assert(mapRequired != null);
        
        Map mapProtcol = getProtocolMap();
        Map mapReturn  = new HashMap();
        Map mapFactory = new HashMap(mapRequired);
        for (Iterator iter = mapRequired.entrySet().iterator(); iter.hasNext(); )
            {
            java.util.Map.Entry  entry    = (java.util.Map.Entry) iter.next();
            String sName    = (String) entry.getKey();
            Object oVersion = entry.getValue();
        
            // look up the required com.tangosol.net.messaging.Protocol by name
            com.tangosol.net.messaging.Protocol protocol = getProtocol(sName);
            if (protocol == null)
                {
                throw new RuntimeException("unsupported protocol: " + sName);
                }
        
            int nVersion;
            if (oVersion instanceof Integer)
                {
                // strict version requirement
                nVersion = ((Integer) oVersion).intValue();
                }
            else
                {
                // version range supported
                int nRequestCurrent;
                int nRequestSupport;
        
                // .NET client sends an int[]        
                if (oVersion instanceof int[])
                    {
                    int[] aVersions = (int[]) oVersion;
                    nRequestCurrent = aVersions[0];
                    nRequestSupport = aVersions[1];
                    }
                else
                    {
                    Object[] aVersions = (Object[]) oVersion;
                    nRequestCurrent = ((Integer) aVersions[0]).intValue();
                    nRequestSupport = ((Integer) aVersions[1]).intValue();
                    }               
        
                int nVersionCurrent = protocol.getCurrentVersion();
                int nVersionSupport = protocol.getSupportedVersion();
        
                // negotiate the highest version possible
                nVersion = Math.min(nRequestCurrent, nVersionCurrent);
        
                // make sure we can both support the negotiated version
                if (nVersion < nRequestSupport || nVersion < nVersionSupport)
                    {
                    throw new RuntimeException("could not negotiate protocol: " + sName);
                    }
        
                // if we had to negotiate the protocol version, indicate so
                if (nVersion != nRequestCurrent)
                    {
                    mapReturn.put(sName, Integer.valueOf(nVersion));
                    }
                }
        
            mapFactory.put(sName, protocol.getMessageFactory(nVersion));
            }
        connection.setMessageFactoryMap(Collections.unmodifiableMap(mapFactory));
        
        // For older clients that do not support version range, we need to return null
        // instead of empty map to avoid running into COH-9572. We also handle this
        // case in newer clients (see Initiator$MessageFactory$OpenConnectionResponse).
        return mapReturn.isEmpty() ? null : mapReturn;
        }
    
    // Declared at the super level
    /**
     * Called after a Connection has closed. This method is called on the
    * service thread.
    * 
    * @param connection  the Connection that was closed
     */
    public void onConnectionClosed(com.tangosol.coherence.component.net.extend.Connection connection)
        {
        if (get_Connection() == connection)
            {
            return;
            }
        
        if (!getConnectionPendingSet().remove(connection) &&
            getConnectionSet().remove(connection))
            {
            super.onConnectionClosed(connection);
            }
        }
    
    // Declared at the super level
    /**
     * Called after a Connection is closed due to an error or exception. This
    * method is called on the service thread.
    * 
    * @param connection  the Connection that was closed
    * @param e  the reason the Connection was closed
     */
    public void onConnectionError(com.tangosol.coherence.component.net.extend.Connection connection, Throwable e)
        {
        if (get_Connection() == connection)
            {
            return;
            }
        
        if (!getConnectionPendingSet().remove(connection) &&
            getConnectionSet().remove(connection))
            {
            super.onConnectionError(connection, e);
            }
        }
    
    // Declared at the super level
    /**
     * Called after a Connection has been successfully established. This method
    * is called on the service thread.
    * 
    * @param connection  the Connection that was opened
     */
    public void onConnectionOpened(com.tangosol.coherence.component.net.extend.Connection connection)
        {
        if (get_Connection() == connection)
            {
            return;
            }
        
        if (getConnectionPendingSet().remove(connection) &&
            getConnectionSet().add(connection))
            {
            super.onConnectionOpened(connection);
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
        // import com.tangosol.internal.net.service.peer.acceptor.AcceptorDependencies;
        
        super.onDependencies(deps);
        
        AcceptorDependencies acceptorDeps = (AcceptorDependencies) deps;
        
        setConnectionLimit(acceptorDeps.getConnectionLimit());
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
        setAcceptingConnections(true);
        
        super.onServiceStarted();
        }
    
    // Declared at the super level
    /**
     * The default implementation of this method sets AcceptingClients to false.
     */
    protected void onServiceStopped()
        {
        // import Component.Net.Extend.Connection;
        // import java.util.HashSet;
        // import java.util.Iterator;
        
        setAcceptingConnections(false);
        
        for (Iterator iter = getConnectionPendingSet().iterator(); iter.hasNext(); )
            {
            Connection connection = (Connection) iter.next();
            iter.remove(); // see #onConnectionClosed
        
            connection.closeInternal(true, null, 0L);
            }
        
        // attempt to close all open Connections
        for (Iterator iter = new HashSet(getConnectionSet()).iterator(); iter.hasNext(); )
            {
            Connection connection = (Connection) iter.next();
            connection.closeInternal(true, null, 0L);
            }
        
        // perform a hard close on any remaining open Connections
        for (Iterator iter = getConnectionSet().iterator(); iter.hasNext(); )
            {
            Connection connection = (Connection) iter.next();
            iter.remove(); // see #onConnectionClosed
        
            connection.closeInternal(true, null, 100L);
            if (connection.isOpen())
                {
                // we were unable to close the Connection because a daemon thread is
                // currently executing within the Connection; interrupt all daemons
                getDaemonPool().stop();
        
                connection.closeInternal(true, null, 1000L);
                if (connection.isOpen())
                    {
                    _trace("Unable to close \"" + connection
                            + "\"; this Connection will be abandoned", 1);
                    }
                }
            }
        
        super.onServiceStopped();
        }
    
    // Declared at the super level
    /**
     * The default implementation of this method sets AcceptingClients to false.
     */
    protected void onServiceStopping()
        {
        // import Component.Net.Extend.Connection;
        // import java.util.Iterator;
        
        setAcceptingConnections(false);
        
        for (Iterator iter = getConnectionPendingSet().iterator(); iter.hasNext(); )
            {
            Connection connection = (Connection) iter.next();
            iter.remove(); // see #onConnectionClosed
        
            connection.closeInternal(true, null, 0L);
            }
        
        for (Iterator iter = getConnectionSet().iterator(); iter.hasNext(); )
            {
            Connection connection = (Connection) iter.next();
            iter.remove(); // see #onConnectionClosed
        
            connection.closeInternal(true, null, 0L);
            }
        
        super.onServiceStopping();
        }
    
    // Declared at the super level
    /**
     * Ping the Connection(s) managed by this ConnectionManager.
     */
    protected void ping()
        {
        // import Component.Net.Extend.Connection;
        // import java.util.Iterator;
        
        for (Iterator iter = getConnectionSet().iterator(); iter.hasNext(); )
            {
            Connection connection = (Connection) iter.next();
            connection.ping();
            }
        }
    
    // Accessor for the property "AcceptingConnections"
    /**
     * Setter for property AcceptingConnections.<p>
    * If true, this Acceptor will accept new connections; otherwise, new
    * connection attempts will be denied.
    * 
    * @volatile
     */
    protected void setAcceptingConnections(boolean fAccepting)
        {
        __m_AcceptingConnections = fAccepting;
        }
    
    // From interface: com.tangosol.net.messaging.ConnectionAcceptor
    // Accessor for the property "ConnectionFilter"
    /**
     * Setter for property ConnectionFilter.<p>
    * The filter used by this Acceptor to evaluate whether or not to accept a
    * connection.
     */
    public void setConnectionFilter(com.tangosol.net.messaging.ConnectionFilter filter)
        {
        __m_ConnectionFilter = filter;
        }
    
    // Accessor for the property "ConnectionLimit"
    /**
     * Setter for property ConnectionLimit.<p>
    * The maximum number of simultaneous connections allowed by the Acceptor. A
    * value of 0 implies no limit.
     */
    protected void setConnectionLimit(int cLimit)
        {
        __m_ConnectionLimit = cLimit;
        }
    
    // Accessor for the property "ConnectionPendingSet"
    /**
     * Setter for property ConnectionPendingSet.<p>
    * The Set Connection objects that have been accepted but not yet opened.
     */
    protected void setConnectionPendingSet(java.util.Set set)
        {
        __m_ConnectionPendingSet = set;
        }
    
    // Accessor for the property "ConnectionSet"
    /**
     * Setter for property ConnectionSet.<p>
    * The Set of open Connection objects managed by this Acceptor.
     */
    protected void setConnectionSet(java.util.Set set)
        {
        __m_ConnectionSet = set;
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.Acceptor$DaemonPool
    
    /**
     * DaemonPool is a class thread pool implementation for processing queued
     * operations on one or more daemon threads.
     * 
     * The designable properties are:
     *     AutoStart
     *     DaemonCount
     * 
     * The simple API for the DaemonPool is:
     *     public void start()
     *     public boolean isStarted()
     *     public void add(Runnable task)
     *     public void stop()
     * 
     * The advanced API for the DaemonPool is:
     *     DaemonCount property
     *     Daemons property
     *     Queues property
     *     ThreadGroup property
     * 
     * The DaemonPool is composed of two key components:
     * 
     * 1) An array of WorkSlot components that may or may not share Queues with
     * other WorkSlots. 
     * 
     * 2) An array of Daemon components feeding off the Queues. This collection
     * is accessed by the DaemonCount and Daemons properties, and is managed by
     * the DaemonCount mutator.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class DaemonPool
            extends    com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.DaemonPool
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
            __mapChildren.put("Daemon", com.tangosol.coherence.component.util.daemon.queueProcessor.Service.DaemonPool.Daemon.get_CLASS());
            __mapChildren.put("ResizeTask", com.tangosol.coherence.component.util.daemon.queueProcessor.Service.DaemonPool.ResizeTask.get_CLASS());
            __mapChildren.put("ScheduleTask", com.tangosol.coherence.component.util.daemon.queueProcessor.Service.DaemonPool.ScheduleTask.get_CLASS());
            __mapChildren.put("StartTask", com.tangosol.coherence.component.util.daemon.queueProcessor.Service.DaemonPool.StartTask.get_CLASS());
            __mapChildren.put("StopTask", com.tangosol.coherence.component.util.daemon.queueProcessor.Service.DaemonPool.StopTask.get_CLASS());
            __mapChildren.put("WorkSlot", com.tangosol.coherence.component.util.daemon.queueProcessor.Service.DaemonPool.WorkSlot.get_CLASS());
            __mapChildren.put("WrapperTask", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.DaemonPool.WrapperTask.get_CLASS());
            }
        
        // Default constructor
        public DaemonPool()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public DaemonPool(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                setAbandonThreshold(8);
                setDaemonCountMax(2147483647);
                setDaemonCountMin(1);
                setScheduledTasks(new java.util.HashSet());
                setStatsTaskAddCount(new java.util.concurrent.atomic.AtomicLong());
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.Acceptor.DaemonPool();
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
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/peer/Acceptor$DaemonPool".replace('/', '.'));
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
         * Getter for property Dynamic.<p>
        * Flag that indicates whether this DaemonPool dynamically changes its
        * thread count to maximize
        * throughput and resource utilization.
         */
        public boolean isDynamic()
            {
            return getDaemonCount() > 0 && getDaemonCountMax() != getDaemonCountMin();
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.Acceptor$MessageFactory
    
    /**
     * MessageFactory implementation for version 2 of the MessagingProtocol.
     * This MessageFactory contains Message classes necessary to manage the
     * lifecycle of Connections and Channels.
     *  
     * The type identifiers of the Message classes instantiated by this
     * MessageFactory are organized as follows:
     * 
     * Internal (<0):
     * 
     * (-1)  AcceptChannel
     * (-2)  CloseChannel
     * (-3)  CloseConnection
     * (-4)  CreateChannel
     * (-5)  NotifyShutdown
     * (-6)  NotifyStartup
     * (-7)  OpenChannel
     * (-8)  OpenConnection
     * (-9)  Response
     * (-10) EncodedMessage
     * 
     * Connection Lifecycle (0 - 10):
     * 
     * (0)  OpenConnectionResponse (*)
     * (1)  OpenConnectionRequest
     * (3)  PingRequest
     * (4)  PingResponse
     * (10) NotifyConnectionClosed
     * 
     * * The OpenConnectionResponse has type identifier 0 for historical
     * reasons. Prior to version 2 of the Messaging Protocol, all Request
     * messages used a common Response type with type identifier 0. Since the
     * first Response that a client expects to receive is an
     * OpenConnectionResponse, this allows version 2 and newer servers to
     * rejects connection attempts from version 1 clients.
     * 
     * Channel Lifecycle (11-20):
     * 
     * (11) OpenChannelRequest
     * (12) OpenChannelResponse
     * (13) AcceptChannelRequest
     * (14) AcceptChannelResponse
     * (20) NotifyChannelClosed
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class MessageFactory
            extends    com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory
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
            __mapChildren.put("AcceptChannel", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.AcceptChannel.get_CLASS());
            __mapChildren.put("AcceptChannelRequest", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.AcceptChannelRequest.get_CLASS());
            __mapChildren.put("AcceptChannelResponse", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.AcceptChannelResponse.get_CLASS());
            __mapChildren.put("CloseChannel", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.CloseChannel.get_CLASS());
            __mapChildren.put("CloseConnection", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.CloseConnection.get_CLASS());
            __mapChildren.put("CreateChannel", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.CreateChannel.get_CLASS());
            __mapChildren.put("EncodedMessage", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.EncodedMessage.get_CLASS());
            __mapChildren.put("NotifyChannelClosed", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.NotifyChannelClosed.get_CLASS());
            __mapChildren.put("NotifyConnectionClosed", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.NotifyConnectionClosed.get_CLASS());
            __mapChildren.put("NotifyShutdown", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.NotifyShutdown.get_CLASS());
            __mapChildren.put("NotifyStartup", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.NotifyStartup.get_CLASS());
            __mapChildren.put("OpenChannel", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.OpenChannel.get_CLASS());
            __mapChildren.put("OpenChannelRequest", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.OpenChannelRequest.get_CLASS());
            __mapChildren.put("OpenChannelResponse", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.OpenChannelResponse.get_CLASS());
            __mapChildren.put("OpenConnection", Acceptor.MessageFactory.OpenConnection.get_CLASS());
            __mapChildren.put("OpenConnectionRequest", Acceptor.MessageFactory.OpenConnectionRequest.get_CLASS());
            __mapChildren.put("OpenConnectionResponse", Acceptor.MessageFactory.OpenConnectionResponse.get_CLASS());
            __mapChildren.put("PingRequest", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.PingRequest.get_CLASS());
            __mapChildren.put("PingResponse", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.PingResponse.get_CLASS());
            __mapChildren.put("Response", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.Response.get_CLASS());
            }
        
        // Default constructor
        public MessageFactory()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public MessageFactory(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.Acceptor.MessageFactory();
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
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/peer/Acceptor$MessageFactory".replace('/', '.'));
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

        // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.Acceptor$MessageFactory$OpenConnection
        
        /**
         * Internal Request used to open a Connection.
         */
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class OpenConnection
                extends    com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.OpenConnection
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
                __mapChildren.put("Status", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.OpenConnection.Status.get_CLASS());
                }
            
            // Default constructor
            public OpenConnection()
                {
                this(null, null, true);
                }
            
            // Initializing constructor
            public OpenConnection(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.Acceptor.MessageFactory.OpenConnection();
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
                    clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/peer/Acceptor$MessageFactory$OpenConnection".replace('/', '.'));
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
            
            // Declared at the super level
            /**
             * Called when the Request is run.
            * 
            * @param response  the Response that should be populated with the
            * result of running the Request
             */
            protected void onRun(com.tangosol.coherence.component.net.extend.message.Response response)
                {
                // import Component.Net.Extend.Connection;
                // import java.util.Map;
                
                Connection connection = getConnectionOpen();
                _assert(!connection.isOpen());
                
                Acceptor module = (Acceptor) getChannel().getReceiver();
                
                connection.openInternal();
                
                module.getConnectionPendingSet().add(connection);
                }
            }

        // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.Acceptor$MessageFactory$OpenConnectionRequest
        
        /**
         * This Request is used to open a new Channel.
         */
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class OpenConnectionRequest
                extends    com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.OpenConnectionRequest
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
                __mapChildren.put("Status", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.OpenConnectionRequest.Status.get_CLASS());
                }
            
            // Default constructor
            public OpenConnectionRequest()
                {
                this(null, null, true);
                }
            
            // Initializing constructor
            public OpenConnectionRequest(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.Acceptor.MessageFactory.OpenConnectionRequest();
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
                    clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/peer/Acceptor$MessageFactory$OpenConnectionRequest".replace('/', '.'));
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
            
            // Declared at the super level
            /**
             * Called when a RuntimException is caught while executing the
            * Request.
            * 
            * @see #run
             */
            protected void onException(RuntimeException e)
                {
                // import com.tangosol.net.messaging.Channel as com.tangosol.net.messaging.Channel;
                
                super.onException(e);
                
                com.tangosol.net.messaging.Channel channel = getChannel();
                Acceptor module  = (Acceptor) channel.getReceiver();
                
                module.getConnectionPendingSet().remove(channel.getConnection());
                }
            
            // Declared at the super level
            /**
             * Called when the Request is run.
            * 
            * @param response  the Response that should be populated with the
            * result of running the Request
             */
            protected void onRun(com.tangosol.coherence.component.net.extend.message.Response response)
                {
                // import Component.Net.Extend.Channel;
                // import Component.Net.Extend.Connection;
                // import com.tangosol.net.messaging.ConnectionException;
                // import com.tangosol.net.messaging.ConnectionFilter;
                // import com.tangosol.util.UUID;
                // import java.util.Map;
                
                Channel channel0 = (Channel) getChannel();
                _assert(channel0.getId() == 0);
                
                Connection connection = (Connection) channel0.getConnection();
                _assert(connection != null);
                
                Acceptor module     = (Acceptor) getChannel().getReceiver();
                UUID    uuid       = getClientId();
                Map     mapVersion = getProtocolVersionMap();
                
                _assert(uuid != null);
                _assert(mapVersion != null && !mapVersion.isEmpty());
                
                try
                    {
                    connection.setId(new UUID());
                    connection.setMember(getMember());
                    connection.setPeerEdition(getEdition());
                    connection.setPeerId(uuid);
                
                    channel0.setSubject(module.assertIdentityToken(
                            module.deserializeIdentityToken(getIdentityToken())));
                    channel0.setAccessAdapter(module.getAccessAdapter());
                
                    ConnectionFilter filter = module.getConnectionFilter();
                    if (filter != null)
                        {
                        filter.checkConnection(connection);
                        }
                
                    // defer isAcceptingConnections() check until after the ConnectionFilters
                    // check the connection (e.g. for redirects)
                    if (!module.isAcceptingConnections())
                        {
                        throw new ConnectionException("connection rejected");
                        }
                
                    Map map = module.negotiateProtocols(connection, mapVersion);
                    ((Acceptor.MessageFactory.OpenConnectionResponse) response).setProtocolVersionMap(map);
                
                    UUID[] auuid = new UUID[] {connection.getId(), module.getProcessId()};
                    response.setResult(auuid);
                
                    // the Connection is now ready for use
                    module.onConnectionOpened(connection);
                    }
                catch (ConnectionException e)
                    {
                    response.setFailure(true);
                    response.setResult(e);
                    }
                catch (SecurityException e)
                    {
                    _trace("An exception occurred while processing the identity token: " + e, 2);
                    response.setFailure(true);
                    // best practice is not to send SecurityException information back to
                    // client; send an empty one instead.
                    response.setResult(new SecurityException("connection rejected"));
                    }
                catch (RuntimeException e)
                    {
                    response.setFailure(true);
                    response.setResult(new ConnectionException("connection rejected", e));
                    }
                
                if (response.isFailure())
                    {
                    module.getConnectionPendingSet().remove(connection);
                    }
                }
            }

        // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.Acceptor$MessageFactory$OpenConnectionResponse
        
        /**
         * Response to an OpenChannelRequest.
         */
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class OpenConnectionResponse
                extends    com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.OpenConnectionResponse
            {
            // ---- Fields declarations ----
            
            // Default constructor
            public OpenConnectionResponse()
                {
                this(null, null, true);
                }
            
            // Initializing constructor
            public OpenConnectionResponse(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.Acceptor.MessageFactory.OpenConnectionResponse();
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
                    clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/peer/Acceptor$MessageFactory$OpenConnectionResponse".replace('/', '.'));
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
            public void run()
                {
                _assert(getChannel().getId() == 0);
                }
            }
        }
    }
