
/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ProxyService

package com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid;

import com.tangosol.coherence.component.net.Poll;
import com.tangosol.coherence.component.net.ServiceInfo;
import com.tangosol.coherence.component.net.extend.protocol.CacheServiceProtocol;
import com.tangosol.coherence.component.net.extend.protocol.InvocationServiceProtocol;
import com.tangosol.coherence.component.net.extend.protocol.NamedCacheProtocol;
import com.tangosol.coherence.component.net.extend.proxy.serviceProxy.CacheServiceProxy;
import com.tangosol.coherence.component.net.extend.proxy.serviceProxy.InvocationServiceProxy;
import com.tangosol.coherence.component.net.memberSet.actualMemberSet.ServiceMemberSet;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.Acceptor;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.GrpcAcceptor;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.HttpAcceptor;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.MemcachedAcceptor;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.TcpAcceptor;
import com.oracle.coherence.common.net.InetSocketAddress32;
import com.oracle.coherence.common.net.SSLSocketProvider;
import com.tangosol.coherence.config.builder.ServiceLoadBalancerBuilder;
import com.tangosol.internal.net.service.grid.DefaultProxyServiceDependencies;
import com.tangosol.internal.net.service.grid.ProxyServiceDependencies;
import com.tangosol.net.ActionPolicy;
import com.tangosol.net.CacheService;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.InetAddressHelper;
import com.tangosol.net.Member;
import com.tangosol.net.NameService;
import com.tangosol.net.NamedCache;
import com.tangosol.net.NamedCollection;
import com.tangosol.net.OperationalContext;
import com.tangosol.net.RequestPolicyException;
import com.tangosol.net.Session;
import com.tangosol.net.management.Registry;
import com.tangosol.net.messaging.Connection;
import com.tangosol.net.messaging.ConnectionAcceptor;
import com.tangosol.net.messaging.ConnectionEvent;
import com.tangosol.net.messaging.ConnectionException;
import com.tangosol.net.proxy.ProxyServiceLoadBalancer;
import com.tangosol.net.proxy.RemoteMember;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.util.Base;
import com.tangosol.util.LiteMap;
import com.tangosol.util.ObservableMap;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.TreeSet;
import javax.security.auth.Subject;

/**
 * Service that allows non-clustered client JVMs to use other clustered
 * Services.
 * 
 * The ProxyService uses a ConnectionAcceptor to accept and process incoming
 * requests from non-clustered clients. The ConnectionAcceptor is started
 * immediately after the ProxyService successfully starts and is shutdown upon
 * service termination.
 * 
 * During ConnectionAcceptor configuration, the ProxyService registers the
 * following Protocols:
 * 
 * (1) CacheService Protocol
 * (2) InvocationService Protocol
 * (3) NamedCache Protocol
 * 
 * Additionally, a CacheServiceProxy is registered as a Receiver for the
 * CacheService Protocol and an InvocationService is registered as a Receiver
 * for the InvocationService Protocol.
 * 
 * Currently used MessageTypes:
 * [-32, ?1]  Reserved by Grid
 * 257        NotifyConnectionOpened
 * 258        NotifyConnectionClosed
 * 259        RedirectRequest
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class ProxyService
        extends    com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid
        implements com.tangosol.net.NameService.Resolvable,
                   com.tangosol.net.ProxyService,
                   com.tangosol.net.Session,
                   com.tangosol.net.messaging.ConnectionFilter,
                   com.tangosol.net.messaging.ConnectionListener,
                   com.tangosol.util.SynchronousListener
    {
    // ---- Fields declarations ----
    
    /**
     * Property Acceptor
     *
     * ConnectionAcceptor used by the ProxyService to accept connections from
     * non-clustered Service clients (Stubs).
     */
    private com.tangosol.net.messaging.ConnectionAcceptor __m_Acceptor;
    
    /**
     * Property CacheServiceProxy
     *
     * The cluster side portion (Proxy) of the client-to-cluster CacheService
     * Adapter.
     */
    private com.tangosol.coherence.component.net.extend.proxy.serviceProxy.CacheServiceProxy __m_CacheServiceProxy;
    
    /**
     * Property InvocationServiceProxy
     *
     * The cluster side portion (Proxy) of the client-to-cluster
     * InvocationService Adapter.
     */
    private com.tangosol.coherence.component.net.extend.proxy.serviceProxy.InvocationServiceProxy __m_InvocationServiceProxy;
    
    /**
     * Property LoadBalancer
     *
     * The configured ProxyServiceLoadBalancer for this ProxyService.
     */
    private com.tangosol.net.proxy.ProxyServiceLoadBalancer __m_LoadBalancer;
    
    /**
     * Property MemberListenAddresses
     *
     * A sorted set of TCP/IP listen addresses for the current service Members.
     */
    private java.util.NavigableSet __m_MemberListenAddresses;
    
    /**
     * Property ResolveAllAddresses
     *
     */
    private boolean __m_ResolveAllAddresses;
    
    /**
     * Property ServiceLoad
     *
     * The ServiceLoad for this ProxyService.
     */
    private ProxyService.ServiceLoad __m_ServiceLoad;
    
    /**
     * Property ServiceLoadMap
     *
     * A Map of ServiceLoad objects, keyed by their associated Member objects.
     */
    private java.util.Map __m_ServiceLoadMap;
    
    /**
     * Property ServiceLoadPublished
     *
     * The last published ServiceLoad for this ProxyService.
     */
    private ProxyService.ServiceLoad __m_ServiceLoadPublished;
    
    /**
     * Property ServiceLoadTimeMillis
     *
     * The last time the ServiceLoad was evaluated via the updateServiceLoad()
     * method.
     */
    private long __m_ServiceLoadTimeMillis;
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
        __mapChildren.put("BusEventMessage", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.BusEventMessage.get_CLASS());
        __mapChildren.put("ConfigRequest", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.ConfigRequest.get_CLASS());
        __mapChildren.put("ConfigResponse", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.ConfigResponse.get_CLASS());
        __mapChildren.put("ConfigSync", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.ConfigSync.get_CLASS());
        __mapChildren.put("ConfigUpdate", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.ConfigUpdate.get_CLASS());
        __mapChildren.put("DispatchEvent", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.DispatchEvent.get_CLASS());
        __mapChildren.put("DispatchNotification", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.DispatchNotification.get_CLASS());
        __mapChildren.put("LoadBalancerActionGetList", ProxyService.LoadBalancerActionGetList.get_CLASS());
        __mapChildren.put("LoadBalancerActionUpdate", ProxyService.LoadBalancerActionUpdate.get_CLASS());
        __mapChildren.put("LookupCallback", ProxyService.LookupCallback.get_CLASS());
        __mapChildren.put("MemberConfigUpdate", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.MemberConfigUpdate.get_CLASS());
        __mapChildren.put("MemberJoined", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.MemberJoined.get_CLASS());
        __mapChildren.put("MemberWelcome", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.MemberWelcome.get_CLASS());
        __mapChildren.put("MemberWelcomeRequest", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.MemberWelcomeRequest.get_CLASS());
        __mapChildren.put("MemberWelcomeRequestTask", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.MemberWelcomeRequestTask.get_CLASS());
        __mapChildren.put("NotifyConnectionClose", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyConnectionClose.get_CLASS());
        __mapChildren.put("NotifyConnectionClosed", ProxyService.NotifyConnectionClosed.get_CLASS());
        __mapChildren.put("NotifyConnectionOpen", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyConnectionOpen.get_CLASS());
        __mapChildren.put("NotifyConnectionOpened", ProxyService.NotifyConnectionOpened.get_CLASS());
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
        __mapChildren.put("RedirectRequest", ProxyService.RedirectRequest.get_CLASS());
        __mapChildren.put("Response", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.Response.get_CLASS());
        __mapChildren.put("ServiceLoad", ProxyService.ServiceLoad.get_CLASS());
        __mapChildren.put("ServiceLoadListener", ProxyService.ServiceLoadListener.get_CLASS());
        __mapChildren.put("WrapperGuardable", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.WrapperGuardable.get_CLASS());
        }
    
    // Default constructor
    public ProxyService()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public ProxyService(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            setMemberListenAddresses(new java.util.TreeSet());
            setMessageClassMap(new java.util.HashMap());
            setNotifier(new com.oracle.coherence.common.base.SingleWaiterMultiNotifier());
            setOldestPendingRequestSUIDCounter(new java.util.concurrent.atomic.AtomicLong());
            setResolveAllAddresses(false);
            setResourceRegistry(new com.tangosol.util.SimpleResourceRegistry());
            setSerializerMap(new java.util.WeakHashMap());
            setServiceLoadMap(new com.tangosol.util.SafeHashMap());
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
        return "Proxy";
        }
    
    //++ getter for static property _Instance
    /**
     * Getter for property _Instance.<p>
    * Auto generated
     */
    public static com.tangosol.coherence.Component get_Instance()
        {
        return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ProxyService();
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
            clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/ProxyService".replace('/', '.'));
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
    
    // From interface: com.tangosol.net.Session
    public void activate()
        {
        }
    
    // From interface: com.tangosol.net.messaging.ConnectionFilter
    public void checkConnection(com.tangosol.net.messaging.Connection connection)
            throws com.tangosol.net.messaging.ConnectionException
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Peer.Acceptor.TcpAcceptor$TcpConnection as com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.TcpAcceptor.TcpConnection;
        // import com.tangosol.net.ProxyService$ProxyAction as com.tangosol.net.ProxyService.ProxyAction;
        // import com.tangosol.net.RequestPolicyException;
        // import com.tangosol.net.messaging.ConnectionException;
        
        if (connection instanceof com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.TcpAcceptor.TcpConnection)
            {
            // ensure that the client has landed where they intended, a mismatch could happen
            // if they'd been routed by a redirect to a processes just as it shutdown and this
            // process just started and ended up on the same port.  In such a case we refuse
            // to accept the connection and force the client to retry.
            
            com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.TcpAcceptor.TcpConnection connectionImpl = (com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.TcpAcceptor.TcpConnection) connection;
            String sCluster = connectionImpl.getClusterName();
            if (sCluster != null && !sCluster.equals(getCluster().getClusterName()))
                {
                throw new ConnectionException("connection rejected, cluster mismatch");
                }
        
            String sService = connectionImpl.getServiceName();
            if (sService != null && !sService.equals(getServiceName()))
                {
                throw new ConnectionException("connection rejected, service mismatch");
                }    
            }
        
        if (!getActionPolicy().isAllowed(this, com.tangosol.net.ProxyService.ProxyAction.CONNECT))
            {
            throw new ConnectionException("connection rejected",
                new RequestPolicyException("client connections are disallowed by the action policy"));
            }
        
        ProxyService.RedirectRequest request = (ProxyService.RedirectRequest) instantiateMessage("RedirectRequest");
        request.setConnection(connection);
        request.addToMember(getThisMember());
        
        Object oResult = poll(request);
        if (oResult instanceof Boolean && ((Boolean) oResult).booleanValue())
            {
            throw new ConnectionException("connection redirected");
            }
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
        // import com.tangosol.internal.net.service.grid.DefaultProxyServiceDependencies;
        // import com.tangosol.internal.net.service.grid.ProxyServiceDependencies;
        
        return new DefaultProxyServiceDependencies((ProxyServiceDependencies) deps);
        }
    
    // From interface: com.tangosol.net.Session
    public void close()
            throws java.lang.Exception
        {
        }
    
    // From interface: com.tangosol.net.ProxyService
    // Declared at the super level
    public synchronized void configure(com.tangosol.run.xml.XmlElement xml)
        {
        // import com.tangosol.internal.net.service.grid.DefaultProxyServiceDependencies;
        // import com.tangosol.internal.net.service.grid.LegacyXmlProxyServiceHelper as com.tangosol.internal.net.service.grid.LegacyXmlProxyServiceHelper;
        
        setDependencies(com.tangosol.internal.net.service.grid.LegacyXmlProxyServiceHelper.fromXml(xml,
            new DefaultProxyServiceDependencies(), getOperationalContext(),
            getContextClassLoader()));
        
        setServiceConfig(xml);
        }
    
    // From interface: com.tangosol.net.messaging.ConnectionListener
    public void connectionClosed(com.tangosol.net.messaging.ConnectionEvent evt)
        {
        // import Component.Net.Extend.Connection as com.tangosol.coherence.component.net.extend.Connection;
        // import Component.Net.Member as com.tangosol.coherence.component.net.Member;
        // import com.tangosol.net.MemberEvent as com.tangosol.net.MemberEvent;
        
        com.tangosol.coherence.component.net.extend.Connection              con    = (com.tangosol.coherence.component.net.extend.Connection) evt.getConnection();
        com.tangosol.coherence.component.net.Member                  member = (com.tangosol.coherence.component.net.Member) con.getMember();
        ProxyService.NotifyConnectionClosed msg    = (ProxyService.NotifyConnectionClosed)
                instantiateMessage("NotifyConnectionClosed");
        
        msg.setConnection(con);
        msg.addToMember(getThisMember());
        
        send(msg);
        
        doNotifyConnectionEvent(evt.getId(), member, com.tangosol.net.MemberEvent.MEMBER_LEFT);
        }
    
    // From interface: com.tangosol.net.messaging.ConnectionListener
    public void connectionError(com.tangosol.net.messaging.ConnectionEvent evt)
        {
        connectionClosed(evt);
        }
    
    // From interface: com.tangosol.net.messaging.ConnectionListener
    public void connectionOpened(com.tangosol.net.messaging.ConnectionEvent evt)
        {
        // import Component.Net.Extend.Connection as com.tangosol.coherence.component.net.extend.Connection;
        // import Component.Net.Member as com.tangosol.coherence.component.net.Member;
        // import com.tangosol.net.MemberEvent as com.tangosol.net.MemberEvent;
        
        com.tangosol.coherence.component.net.extend.Connection              con    = (com.tangosol.coherence.component.net.extend.Connection) evt.getConnection();
        com.tangosol.coherence.component.net.Member                  member = (com.tangosol.coherence.component.net.Member) con.getMember();
        ProxyService.NotifyConnectionOpened msg    = (ProxyService.NotifyConnectionOpened)
                instantiateMessage("NotifyConnectionOpened");
        
        msg.setConnection(con);
        msg.addToMember(getThisMember());
        
        send(msg);
        
        doNotifyConnectionEvent(evt.getId(), member, com.tangosol.net.MemberEvent.MEMBER_JOINED);
        }
    
    /**
     * Notify all member aware services that an extend client member open and
    * close/loses a connection with proxy service.
    * On receiving NotifyConnectionOpen or NotifyConnectionClose event, a
    * service member dispatches the appropriate 
    * MemberEvent MEMBER_JOINED or MEMBER_LEFT to registered MemberListeners.
    * 
    * @param nConnectionEventType   a connecction event type
    * @param notifyMember                    the connection's extend client
    * member 
    * @param nMemberEventType          the MemberEvent type to be sent oncer
    * connection
    *                                                             notifications
    * have been sent
    * 
    * @since 22.06
     */
    protected void doNotifyConnectionEvent(int nConnectionEventType, com.tangosol.coherence.component.net.Member notifyMember, int nMemberEventType)
        {
        // import Component.Net.Cluster as com.tangosol.coherence.component.net.Cluster;
        // import Component.Net.Member as com.tangosol.coherence.component.net.Member;
        // import Component.Util.Daemon.QueueProcessor.Service.Grid as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid;
        // import Component.Util.Daemon.QueueProcessor.Service.Grid.ClusterService as ClusterService;
        // import com.tangosol.net.MemberEvent as com.tangosol.net.MemberEvent;
        // import com.tangosol.net.messaging.ConnectionEvent;
        
        // notify all services that an extend client member opened/left a connection with proxy service
        ClusterService clusterService = getClusterService();
        
        if (clusterService.isVersionCompatible(clusterService.getServiceMemberSet(), 22, 6, 0))
            {
            for (int i = 0, cServices = clusterService.getServiceCount(); i < cServices; ++i)
                {
                com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid   service = ((com.tangosol.coherence.component.net.Cluster) getCluster()).getClusterService().getService(i);
                String sName   = service != null ? service.getInfo().getServiceName() : "";
        
                // skip notifying com.tangosol.coherence.component.net.Cluster, TransportService and Management about extend client member connection event
                if (service != null &&
                    service != clusterService &&
                    !sName.equals("TransportService") &&
                    !sName.equals("Management") &&
                    service != this)
                    {
                    switch (nConnectionEventType)
                        {
                        case ConnectionEvent.CONNECTION_OPENED:
                            ProxyService.NotifyConnectionOpen msgOpen = (ProxyService.NotifyConnectionOpen) service.instantiateMessage("NotifyConnectionOpen");
                            msgOpen.setNotifyMember(notifyMember);
                            msgOpen.setToMemberSet(service.getServiceMemberSet());
                            service.send(msgOpen);
                            break;
        
                        case ConnectionEvent.CONNECTION_CLOSED:
                        case ConnectionEvent.CONNECTION_ERROR:
                            ProxyService.NotifyConnectionClose msgClose = (ProxyService.NotifyConnectionClose) service.instantiateMessage("NotifyConnectionClose");
                            msgClose.setNotifyMember(notifyMember);
                            msgClose.setToMemberSet(service.getServiceMemberSet());
                            service.send(msgClose);
                            break;
                        }
                    }
                }
                dispatchMemberEvent(notifyMember, nMemberEventType);
            }
        }
    
    // Declared at the super level
    /**
     * @return a human-readable description of the Service statistics
     */
    public String formatStats()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Peer.Acceptor;
        // import com.tangosol.net.messaging.ConnectionAcceptor;
        
        ConnectionAcceptor acceptor = getAcceptor();
        if (acceptor instanceof Acceptor)
            {
            return ((Acceptor) acceptor).formatStats();
            }
        return super.formatStats();
        }
    
    // Accessor for the property "Acceptor"
    /**
     * Getter for property Acceptor.<p>
    * ConnectionAcceptor used by the ProxyService to accept connections from
    * non-clustered Service clients (Stubs).
     */
    public com.tangosol.net.messaging.ConnectionAcceptor getAcceptor()
        {
        return __m_Acceptor;
        }
    
    // From interface: com.tangosol.net.Session
    public com.tangosol.net.NamedCache getCache(String sName, com.tangosol.net.NamedMap.Option[] options)
        {
        // import com.tangosol.net.Session;
        // import com.tangosol.net.CacheService;
        // import com.tangosol.util.Base;
        
        CacheService service = getCacheServiceProxy().getCacheService();
        if (service instanceof Session)
            {
            return ((Session) service).getCache(sName, options);
            }
        else
            {
            return service.ensureCache(sName, Base.ensureClassLoader(null));
            }
        }
    
    // Accessor for the property "CacheServiceProxy"
    /**
     * Getter for property CacheServiceProxy.<p>
    * The cluster side portion (Proxy) of the client-to-cluster CacheService
    * Adapter.
     */
    public com.tangosol.coherence.component.net.extend.proxy.serviceProxy.CacheServiceProxy getCacheServiceProxy()
        {
        return __m_CacheServiceProxy;
        }
    
    // From interface: com.tangosol.net.Session
    public com.tangosol.net.events.InterceptorRegistry getInterceptorRegistry()
        {
        return null;
        }
    
    // Accessor for the property "InvocationServiceProxy"
    /**
     * Getter for property InvocationServiceProxy.<p>
    * The cluster side portion (Proxy) of the client-to-cluster
    * InvocationService Adapter.
     */
    public com.tangosol.coherence.component.net.extend.proxy.serviceProxy.InvocationServiceProxy getInvocationServiceProxy()
        {
        return __m_InvocationServiceProxy;
        }
    
    /**
     * Return the TCP/IP listen address for the specified service Member.
    * 
    * @param member  the service Member
    * 
    * @return the TCP/IP listen address or null if the service Member isn't
    * currently listening for incoming TCP/IP connections
     */
    protected String getListenAddress(com.tangosol.net.Member member)
        {
        // import Component.Net.MemberSet.ActualMemberSet.ServiceMemberSet;
        // import com.tangosol.net.proxy.RemoteMember;
        // import java.util.Map;
        
        if (member instanceof RemoteMember)
            {
            return member.getAddress().getHostAddress();
            }
        
        ServiceMemberSet setMember = getServiceMemberSet();
        if (setMember == null)
            {
            return null;
            }
        Map map = setMember.getMemberConfigMap(member.getId());
        return map == null ? null : (String) map.get("listen-address");
        }
    
    /**
     * Return the TCP/IP listen addresses for the specified service Member.
    * 
    * @param member  the service Member
    * 
    * @return the TCP/IP listen addresses as a Collection<InetAddress> or null
    * if the service Member isn't currently listening for incoming TCP/IP
    * connections
     */
    protected java.util.Collection getListenAddresses(com.tangosol.net.Member member)
        {
        // import Component.Net.MemberSet.ActualMemberSet.ServiceMemberSet;
        // import com.tangosol.net.proxy.RemoteMember;
        // import java.util.Collections;
        
        if (member instanceof RemoteMember)
            {
            return Collections.singleton(member.getAddress());
            }
        
        ServiceMemberSet setMember = getServiceMemberSet();
        
        return setMember == null
            ? null
            : getListenAddresses(setMember.getMemberConfigMap(member.getId()));
        }
    
    /**
     * Return the TCP/IP listen addresses for a given service Member's map
    * config.
    * 
    * @param mapConfig  the service Member's map config <String, String>
    * 
    * @return the TCP/IP listen addresses as a Collection<InetAddress> or null
    * if the service Member isn't currently listening for incoming TCP/IP
    * connections
     */
    public static java.util.Collection getListenAddresses(java.util.Map mapConfig)
        {
        // import java.net.InetAddress;
        // import java.net.UnknownHostException;
        // import java.util.Collection;
        // import java.util.HashSet;
        
        if (mapConfig == null)
            {
            return null;
            }
        
        String sAddrList = (String) mapConfig.get("listen-addresses");
        if (sAddrList == null)
            {
            return null;
            }
        
        String[]   saAddr  = sAddrList.split(",");
        Collection colAddr = new HashSet(saAddr.length);
        for (int i = 0; i < saAddr.length; )
            {
            try
                {
                colAddr.add(InetAddress.getByName(saAddr[i++]));
                }
            catch (UnknownHostException e)
                {
                // skip - likely a link-local IPv6 address for another machine
                //        containing an interface identifier
                }    
            }
        
        return colAddr;
        }
    
    /**
     * Return the TCP/IP listen port for the specified service Member.
    * 
    * @param member  the service Member
    * 
    * @return the TCP/IP listen port or null if the service Member isn't
    * currently listening for incoming TCP/IP connections
     */
    protected Integer getListenPort(com.tangosol.net.Member member)
        {
        // import com.tangosol.net.proxy.RemoteMember;
        // import java.util.Map;
        
        if (member instanceof RemoteMember)
            {
            return Integer.valueOf(member.getPort());
            }
        else
            {
            Map map = getServiceMemberSet().getMemberConfigMap(member.getId());
            return (Integer) map.get("listen-port");
            }
        }
    
    /**
     * Return the TCP/IP listen address for the specified service Member.
    * 
    * @param member  the service Member
    * 
    * @return the TCP/IP listen address in the form of "<IP_ADDRESS>:<PORT>" 
     */
    protected String getListenSocketAddress(com.tangosol.net.Member member)
        {
        String sAddress = getListenAddress(member);
        if (sAddress == null)
            {
            return null;
            }
        else if (sAddress.indexOf(':') >= 0 && sAddress.charAt(0) != '[')    // IPv6
            {
            sAddress = '[' + sAddress + ']';
            }
        
        return sAddress + ':' + getListenPort(member);
        }
    
    // Accessor for the property "LoadBalancer"
    /**
     * Getter for property LoadBalancer.<p>
    * The configured ProxyServiceLoadBalancer for this ProxyService.
     */
    public com.tangosol.net.proxy.ProxyServiceLoadBalancer getLoadBalancer()
        {
        return __m_LoadBalancer;
        }
    
    // From interface: com.tangosol.net.Session
    public com.tangosol.net.NamedMap getMap(String Param_1, com.tangosol.net.NamedMap.Option[] Param_2)
        {
        return null;
        }
    
    // Accessor for the property "MemberListenAddresses"
    /**
     * Return a sorted list of the TCP/IP listen addresses in the string form of
    * "<IP_ADDRESS>:<PORT>" for all the Members of this proxy service.
    * 
    * @return a sorted list of the TCP/IP listen addresses for all the Members
    * of this proxy service.
     */
    public java.util.NavigableSet getMemberListenAddresses()
        {
        return __m_MemberListenAddresses;
        }
    
    // From interface: com.tangosol.net.Session
    public String getName()
        {
        return null;
        }
    
    // Accessor for the property "NameService"
    /**
     * Getter for property NameService.<p>
    * The NameService running on this node.
     */
    protected com.tangosol.net.NameService getNameService()
        {
        // import com.tangosol.net.NameService;
        
        return (NameService) getCluster().getResourceRegistry().getResource(NameService.class);
        }
    
    /**
     * Return an address for a given ProxyService member that is routable from
    * client address in the NameService$RequestContext.
    * 
    * @param member  the ProxyService Member to check
    * @param ctx  the request context
    * 
    * @return an Object[] where Object[0] is an InetAddress represented as a
    * String, and Object[1] is a port number in String form, or null
    * 
    * @see InetAddressHelper#getRoutableAddresses()
     */
    protected Object[] getRoutableAddress(com.tangosol.net.Member member, com.tangosol.net.NameService.RequestContext ctx)
        {
        // import com.tangosol.net.proxy.RemoteMember;
        
        return getRoutableAddress(member,
            member instanceof RemoteMember ? null : getServiceMemberSet().getMemberConfigMap(member.getId()),
            getCluster(),
            ctx);
        }
    
    /**
     * Return an address for a given ProxyService member that is routable from
    * the specified local {@link Socket}.
    * 
    * @param member  the ProxyService Member to check
    * @param socket  the client Socket connection to this Coherence node
    * 
    * @return an Object[] where Object[0] is an InetAddress represented as a
    * String, and Object[1] is a port number in String form, or null
    * 
    * @see InetAddressHelper#getRoutableAddresses()
     */
    protected Object[] getRoutableAddress(com.tangosol.net.Member member, java.net.Socket socket)
        {
        // import com.tangosol.net.InetAddressHelper;
        // import com.tangosol.net.proxy.RemoteMember;
        // import java.net.InetAddress;
        
        boolean     fLocalSrc;
        InetAddress addrLocal;
        if (socket == null)
            {
            fLocalSrc = true;
            addrLocal = null;
            }
        else
            {
            fLocalSrc = InetAddressHelper.isLocalAddress(InetAddressHelper.getAddress(socket.getRemoteSocketAddress()));
            addrLocal = socket.getLocalAddress();
            }
        
        return getRoutableAddress(member,
            member instanceof RemoteMember ? null : getServiceMemberSet().getMemberConfigMap(member.getId()),
            getCluster(), addrLocal, fLocalSrc);
        }
    
    /**
     * Return an address for a given ProxyService member that is routable from
    * the NameService$RequestContext.
    * 
    * @param member  the ProxyService Member to check
    * @param mapMemberConfig  the MapMemberConfig for the ProxyService
    * @param cluster  this Cluster
    * @param ctx  the request context
    * 
    * @return an Object[] where Object[0] is an InetAddress represented as a
    * String, and Object[1] is a port number in String form, or null
    * 
    * @see InetAddressHelper#getRoutableAddresses()
     */
    public static Object[] getRoutableAddress(com.tangosol.net.Member member, java.util.Map mapMemberConfig, com.tangosol.net.Cluster cluster, com.tangosol.net.NameService.RequestContext ctx)
        {
        // import com.tangosol.net.InetAddressHelper;
        // import java.net.InetAddress;
        
        InetAddress addrSource = ctx.getSourceAddress();
        
        return getRoutableAddress(member, mapMemberConfig, cluster,
            ctx.getAcceptAddress(),
            addrSource == null ? false : InetAddressHelper.isLocalAddress(addrSource));
        }
    
    /**
     * Return an address for a given ProxyService member that is routable from
    * the local address.
    * 
    * @param member  the ProxyService Member to check
    * @param mapMemberConfig  the MapMemberConfig for the ProxyService
    * @param cluster  this Cluster
    * @param addrLocal  the local address
    * @param fLocalSrc  whether the "client" is local to this machine
    * 
    * @return an Object[] where Object[0] is an InetAddress represented as a
    * String, and Object[1] is a port number in String form, or null
    * 
    * @see InetAddressHelper#getRoutableAddresses()
     */
    public static Object[] getRoutableAddress(com.tangosol.net.Member member, java.util.Map mapMemberConfig, com.tangosol.net.Cluster cluster, java.net.InetAddress addrLocal, boolean fLocalSrc)
        {
        // import com.tangosol.net.InetAddressHelper;
        // import com.tangosol.net.proxy.RemoteMember;
        // import java.net.InetAddress;
        // import java.util.Collection;
        
        // for RemoteMembers, just return its listen address
        if (member instanceof RemoteMember)
            {
            return new Object[]
                {
                member.getAddress().getHostAddress(),
                Integer.valueOf(member.getPort())
                };
            }
        
        Collection colAddresses = InetAddressHelper.getRoutableAddresses(
            addrLocal, fLocalSrc,
            getListenAddresses(mapMemberConfig), InetAddressHelper.isLocalAddress(member.getAddress()));
        
        String sProtocol = (String) mapMemberConfig.get("http-protocol");
        return colAddresses == null || colAddresses.isEmpty()
            ? null
            : sProtocol == null
                ? new Object[]
                  {
                  ((InetAddress) colAddresses.iterator().next()).getHostAddress(),
                      mapMemberConfig.get("listen-port")
                  }
                 :
                 new Object[]
                  {
                  ((InetAddress) colAddresses.iterator().next()).getHostAddress(),
                      mapMemberConfig.get("listen-port"),
                      sProtocol
                  };
        }
    
    /**
     * Return an address for a ProxyService member that is routable from the
    * client address in the NameService$RequestContext
    * 
    * @param colMembers  the collection of ProxyService Members to check
    * @param ctx  the request context
    * 
    * @return an Object[] where Object[0] is an InetAddress represented as a
    * String, and Object[1] is a port number in String form, or null
    * 
    * @see InetAddressHelper#getRoutableAddresses()
     */
    protected Object[] getRoutableAddress(java.util.Collection colMembers, com.tangosol.net.NameService.RequestContext ctx)
        {
        // import com.tangosol.net.Member;
        // import java.util.Iterator;
        
        Object[] result = null;
        
        for (Iterator iter = colMembers.iterator(); iter.hasNext() && result == null; )
            {
            result = getRoutableAddress((Member) iter.next(), ctx);
            }
        
        return result;
        }
    
    /**
     * Return all the address for a ProxyService member that are routable from
    * the client address in the NameService$RequestContext
    * 
    * @param colMembers  the collection of ProxyService Members to check
    * @param ctx  the request context
    * 
    * @return an Object[] where Object[0] is an InetAddress represented as a
    * String, and Object[1] is a port number in String form, or null
    * 
    * @see InetAddressHelper#getRoutableAddresses()
     */
    protected Object[] getRoutableAddresses(java.util.Collection colMembers, com.tangosol.net.NameService.RequestContext ctx)
        {
        // import com.tangosol.net.Member;
        // import java.util.ArrayList;
        // import java.util.Iterator;
        
        List     list   = new ArrayList();
        Object[] result = null;
        
        for (Iterator iter = colMembers.iterator(); iter.hasNext(); )
            {
            result = getRoutableAddress((Member) iter.next(), ctx);
            if (result != null)
                {
                list.add(result);
                }
            }
        
        if (!list.isEmpty())
            {
            result = new Object[list.size() * 2];
            int i = 0;
            for (Iterator iter = list.iterator(); iter.hasNext(); )
                {
                Object[] ao = (Object[]) iter.next();
                if (ao != null && ao.length >= 2)
                    {
                    Integer nPort = (Integer) ao[1];
                    if (nPort != null && nPort.intValue() > 0)
                        {
                        result[i++] = ao[0];
                        result[i++] = ao[1];
                        }
                    }
                }
            }
        
        return result;
        }
    
    /**
     * Return a collection of listen addresses for this ProxyService's members
    * that are routable from the local address of the Socket.
    * 
    * @param socket  the Socket
    * 
    * @return a collection of routable addresses of the form
    * "<address>:<port>", or null
    * 
    * @see InetAddressHelper#getRoutableAddresses()
     */
    public java.util.Set getRoutableMemberAddresses(java.net.Socket socket)
        {
        // import com.tangosol.net.InetAddressHelper;
        // import java.net.InetAddress;
        
        boolean     fLocalSrc;
        InetAddress addrLocal;
        if (socket == null)
            {
            fLocalSrc = true;
            addrLocal = null;
            }
        else
            {
            fLocalSrc = InetAddressHelper.isLocalAddress(InetAddressHelper.getAddress(socket.getRemoteSocketAddress()));
            addrLocal = socket.getLocalAddress();
            }
        
        return getRoutableMemberAddresses(getServiceMemberSet(), getCluster(), addrLocal, fLocalSrc);
        }
    
    /**
     * Return a collection of listen addresses for a map of ProxyService members
    * that is routable from the NameService$RequestContext.
    * 
    * @param setMembers  the MemberSet for the ProxyService
    * @param cluster  this Cluster
    * @param ctx  the request context
    * 
    * @return a collection of routable addresses of the form
    * "<address>:<port>", or null
    * 
    * @see InetAddressHelper#getRoutableAddresses()
     */
    public static java.util.Set getRoutableMemberAddresses(com.tangosol.coherence.component.net.memberSet.actualMemberSet.ServiceMemberSet setMembers, com.tangosol.net.Cluster cluster, com.tangosol.net.NameService.RequestContext ctx)
        {
        // import com.tangosol.net.InetAddressHelper;
        // import java.net.InetAddress;
        
        InetAddress addrSource = ctx.getSourceAddress();
        
        return getRoutableMemberAddresses(setMembers, cluster, ctx.getAcceptAddress(),
            addrSource == null ? false : InetAddressHelper.isLocalAddress(addrSource));
        }
    
    /**
     * Return a collection of listen addresses for a map of ProxyService members
    * that is routable from the local address.
    * 
    * @param setMembers  the MemberSet for the ProxyService
    * @param cluster  this Cluster
    * @param addrLocal  the local address
    * @param fLocalSrc  whether the client should be considered to be local to
    * this machine
    * 
    * @return a collection of routable addresses of the form
    * "<address>:<port>", or null
    * 
    * @see InetAddressHelper#getRoutableAddresses()
     */
    public static java.util.Set getRoutableMemberAddresses(com.tangosol.coherence.component.net.memberSet.actualMemberSet.ServiceMemberSet setMembers, com.tangosol.net.Cluster cluster, java.net.InetAddress addrLocal, boolean fLocalSrc)
        {
        // import com.tangosol.net.Member;
        // import java.util.Iterator;
        // import java.util.Set;
        // import java.util.TreeSet;
        
        Set setResult = new TreeSet(); // maintain ordering for checksum consistency
        
        for (Iterator iter = setMembers.iterator(); iter.hasNext(); )
            {
            Member member    = (Member) iter.next();
            int    nMemberID = member.getId();
        
            Object[] aoAddress = getRoutableAddress(member, setMembers.getMemberConfigMap(nMemberID), cluster, addrLocal, fLocalSrc);
            if (aoAddress != null)
                {
                if (aoAddress.length > 2)
                    {
                    setResult.add(new StringBuilder().append(aoAddress[0]).append(':').append(aoAddress[1]).append(':').append(aoAddress[2]).toString());
                    }
                else
                    {
                    setResult.add(new StringBuilder().append(aoAddress[0]).append(':').append(aoAddress[1]).toString());
                    }
                }
            }
        
        return setResult;
        }
    
    // From interface: com.tangosol.net.Session
    public String getScopeName()
        {
        return null;
        }
    
    // From interface: com.tangosol.net.Session
    public com.tangosol.net.Service getService(String Param_1)
        {
        return null;
        }
    
    // Accessor for the property "ServiceLoad"
    /**
     * Getter for property ServiceLoad.<p>
    * The ServiceLoad for this ProxyService.
     */
    public ProxyService.ServiceLoad getServiceLoad()
        {
        ProxyService.ServiceLoad load = __m_ServiceLoad;
        if (load == null)
            {
            setServiceLoad(load = new ProxyService.ServiceLoad());
            }
        return load;
        }
    
    // Accessor for the property "ServiceLoadMap"
    /**
     * Getter for property ServiceLoadMap.<p>
    * A Map of ServiceLoad objects, keyed by their associated Member objects.
     */
    public java.util.Map getServiceLoadMap()
        {
        return __m_ServiceLoadMap;
        }
    
    // Accessor for the property "ServiceLoadPublished"
    /**
     * Getter for property ServiceLoadPublished.<p>
    * The last published ServiceLoad for this ProxyService.
     */
    public ProxyService.ServiceLoad getServiceLoadPublished()
        {
        return __m_ServiceLoadPublished;
        }
    
    // Accessor for the property "ServiceLoadTimeMillis"
    /**
     * Getter for property ServiceLoadTimeMillis.<p>
    * The last time the ServiceLoad was evaluated via the updateServiceLoad()
    * method.
     */
    public long getServiceLoadTimeMillis()
        {
        return __m_ServiceLoadTimeMillis;
        }
    
    // From interface: com.tangosol.net.Session
    public com.tangosol.net.topic.NamedTopic getTopic(String Param_1, com.tangosol.net.NamedCollection.Option[] Param_2)
        {
        return null;
        }

    @Override
    public void close(NamedCollection col)
        {
        if (col instanceof NamedCache<?,?>)
            {
            CacheService service = getCacheServiceProxy().getCacheService();
            if (service instanceof Session)
                {
                ((Session) service).close(col);
                }
            else
                {
                service.releaseCache((NamedCache) col);
                }
            }
        }

    @Override
    public void destroy(NamedCollection col)
        {
        if (col instanceof NamedCache<?,?>)
            {
            CacheService service = getCacheServiceProxy().getCacheService();
            if (service instanceof Session)
                {
                ((Session) service).destroy(col);
                }
            else
                {
                service.destroyCache((NamedCache) col);
                }
            }
        }

    // Declared at the super level
    /**
     * Initialize the service config for this member.
    * 
    * @return  the config element to put in the service config map.
     */
    protected com.tangosol.run.xml.XmlElement initServiceConfig()
        {
        // import com.tangosol.net.ActionPolicy;
        // import com.tangosol.run.xml.XmlElement;
        
        XmlElement xmlConfig = super.initServiceConfig();
        
        ActionPolicy quorumPolicy  = getActionPolicy();
        String       sQuorumPolicy = quorumPolicy == null ? "none" : quorumPolicy.getClass().getName();
        
        // There is no chance for this node to become a senior while older version nodes are
        // still around. Since we don't support rolling downgrades, no need to be concerned
        // about backward compatibility during the service config initialization.
        
        // store the quorum-policy config
        xmlConfig.addAttribute("quorum-policy").setString(sQuorumPolicy);
        
        return xmlConfig;
        }
    
    // From interface: com.tangosol.net.Session
    public boolean isActive()
        {
        return false;
        }
    
    // From interface: com.tangosol.net.Session
    public boolean isCacheActive(String Param_1, ClassLoader Param_2)
        {
        return false;
        }
    
    // From interface: com.tangosol.net.Session
    public boolean isMapActive(String Param_1, ClassLoader Param_2)
        {
        return false;
        }
    
    // Accessor for the property "ResolveAllAddresses"
    /**
     * Getter for property ResolveAllAddresses.<p>
     */
    public boolean isResolveAllAddresses()
        {
        return __m_ResolveAllAddresses;
        }
    
    // From interface: com.tangosol.net.ProxyService
    // Declared at the super level
    /**
     * Getter for property Running.<p>
    * Calculated helper property; returns true if the service state is
    * SERVICE_STARTED.
     */
    public boolean isRunning()
        {
        // import com.tangosol.net.messaging.ConnectionAcceptor;
        
        if (super.isRunning())
            {
            ConnectionAcceptor acceptor = getAcceptor();
            if (acceptor != null)
                {
                if (acceptor.isRunning())
                    {
                    return true;
                    }
                else
                    {
                    stop();
                    }
                }
            }
        
        return false;
        }

    /**
     * @return the description of the service
     */
    @Override
    public String getDescription()
        {
        return super.getDescription() + ", Serializer=" + getSerializer().getName();
        }
    
    // From interface: com.tangosol.net.Session
    public boolean isTopicActive(String Param_1, ClassLoader Param_2)
        {
        return false;
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
        // import Component.Net.Extend.Protocol.CacheServiceProtocol;
        // import Component.Net.Extend.Protocol.InvocationServiceProtocol;
        // import Component.Net.Extend.Protocol.NamedCacheProtocol;
        // import Component.Net.Extend.Proxy.ServiceProxy.CacheServiceProxy;
        // import Component.Net.Extend.Proxy.ServiceProxy.InvocationServiceProxy;
        // import Component.Util.Daemon.QueueProcessor.Service.Peer.Acceptor;
        // import Component.Util.Daemon.QueueProcessor.Service.Peer.Acceptor$DaemonPool as com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.Acceptor.DaemonPool;
        // import Component.Util.Daemon.QueueProcessor.Service.Peer.Acceptor.GrpcAcceptor;
        // import Component.Util.Daemon.QueueProcessor.Service.Peer.Acceptor.HttpAcceptor;
        // import Component.Util.Daemon.QueueProcessor.Service.Peer.Acceptor.MemcachedAcceptor;
        // import com.tangosol.coherence.config.builder.ServiceLoadBalancerBuilder;
        // import com.tangosol.internal.net.service.grid.ProxyServiceDependencies;
        // import com.tangosol.net.ActionPolicy;
        // import com.tangosol.net.ConfigurableCacheFactory;
        // import com.tangosol.net.OperationalContext;
        // import com.tangosol.net.messaging.ConnectionAcceptor;
        // import com.tangosol.net.proxy.ProxyServiceLoadBalancer;
        // import com.tangosol.util.Base;
        
        super.onDependencies(deps);
        
        ProxyServiceDependencies proxyDeps = (ProxyServiceDependencies) deps;
        
        ActionPolicy policy = (ActionPolicy) proxyDeps.getActionPolicyBuilder().realize(createResolver(), getContextClassLoader(), null);
        setActionPolicy(policy);
        
        // ensure that we've been configured with an OperationalContext
        OperationalContext ctx = getOperationalContext();
        if (ctx == null)
            {
            throw new IllegalStateException("missing required OperationalContext");
            }
        
        // create, set and configure the proxies
        CacheServiceProxy cacheServiceProxy = new CacheServiceProxy();
        setCacheServiceProxy(cacheServiceProxy);
        cacheServiceProxy.setDependencies(proxyDeps.getCacheServiceProxyDependencies());
        cacheServiceProxy.setContextClassLoader(getContextClassLoader());
        cacheServiceProxy.setCacheFactory((ConfigurableCacheFactory) getResourceRegistry().getResource(ConfigurableCacheFactory.class, "ConfigurableCacheFactory"));
        
        InvocationServiceProxy invocationServiceProxy = new InvocationServiceProxy();
        setInvocationServiceProxy(invocationServiceProxy);
        invocationServiceProxy.setDependencies(proxyDeps
            .getInvocationServiceProxyDependencies());
        
        // create, set, and configure the Acceptor
        ConnectionAcceptor acceptor =
            Acceptor.createAcceptor(proxyDeps.getAcceptorDependencies(), ctx);    
        setAcceptor(acceptor);  
        if (acceptor instanceof Acceptor)
            {
            Acceptor acceptorImpl         = (Acceptor) acceptor;
            String   sAcceptorServiceName = getThreadName() + ':' + acceptorImpl.getServiceName();
        
            acceptorImpl.setServiceName(sAcceptorServiceName);
            acceptorImpl.setParentService(this);
            acceptorImpl.setDefaultGuardTimeout(getDefaultGuardTimeout());
        
            // COH-7185: Configure the Acceptor DaemonPool, but only if the Acceptor isn't
            //           an HttpAcceptor since it doesn't use its DaemonPool.
            if (!(acceptor instanceof HttpAcceptor))
                {
                com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.Acceptor.DaemonPool pool = (com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.Acceptor.DaemonPool) acceptorImpl.getDaemonPool();
        
                int cThreads = proxyDeps.getWorkerThreadCountMin();
                if (cThreads > 0)
                    {
                    pool.setDaemonCount(cThreads);
                    pool.setDaemonCountMax(proxyDeps.getWorkerThreadCountMax());
                    pool.setDaemonCountMin(cThreads);
                    pool.setHungThreshold(proxyDeps.getTaskHungThresholdMillis());
                    pool.setName(sAcceptorServiceName);
                    pool.setTaskTimeout(proxyDeps.getTaskTimeoutMillis());
                    pool.setThreadPriority(proxyDeps.getWorkerThreadPriority());
                    cacheServiceProxy.setDaemonPool(pool);
                    invocationServiceProxy.setDaemonPool(pool);
                    }
                }
            }
        
        // register all Protocols with the Acceptor
        acceptor.registerProtocol(CacheServiceProtocol.getInstance());
        acceptor.registerProtocol(InvocationServiceProtocol.getInstance());
        acceptor.registerProtocol(NamedCacheProtocol.getInstance());
        
        // register all Receivers with the Acceptor
        if (getCacheServiceProxy().isEnabled())
            {
            acceptor.registerReceiver(getCacheServiceProxy());
            }
        if (getInvocationServiceProxy().isEnabled())
            {
            acceptor.registerReceiver(getInvocationServiceProxy());
            }
        
        // disable pass-through optimizations and configure a wrapper CCF if an
        // HttpAcceptor or MemcachedAcceptor is being used
        if (acceptor instanceof HttpAcceptor)
            {
            cacheServiceProxy.setPassThroughEnabled(false);
            ((HttpAcceptor) acceptor).setSession(this);
            }
        else if (acceptor instanceof MemcachedAcceptor)
            {
            cacheServiceProxy.setPassThroughEnabled(false);
            }
        else if (acceptor instanceof GrpcAcceptor)
            {
            cacheServiceProxy.setPassThroughEnabled(false);
            }
        
        // Configure the load balancer
        ServiceLoadBalancerBuilder builder = proxyDeps.getLoadBalancerBuilder();
        if (builder != null)
            {
            ProxyServiceLoadBalancer balancer =
                    (ProxyServiceLoadBalancer) builder.realize(createResolver(), getContextClassLoader(), null);
            if (balancer != null)
                {
                setLoadBalancer(balancer);
                try
                    {
                    balancer.init(this);
                    }
                catch (RuntimeException e)
                    {
                    Base.ensureRuntimeException(e, "error initializing load balancer");
                    }
                }
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
        // import Component.Util.Daemon.QueueProcessor.Service.Peer.Acceptor;
        // import Component.Util.Daemon.QueueProcessor.Service.Peer.Acceptor.TcpAcceptor;
        // import Component.Util.Daemon.QueueProcessor.Service.Peer.Acceptor.TcpAcceptor$TcpConnection as com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.TcpAcceptor.TcpConnection;
        // import Component.Util.DaemonPool as com.tangosol.coherence.component.util.DaemonPool;
        // import com.tangosol.net.messaging.ConnectionAcceptor;
        // import com.tangosol.util.Base;
        // import java.util.ConcurrentModificationException;
        // import java.util.Iterator;
        
        super.onNotify();
        
        final long UPDATE_PERIOD = 10000L;
        if (getServiceLoadTimeMillis() + UPDATE_PERIOD <= Base.getLastSafeTimeMillis())
            {
            ProxyService.ServiceLoad load = getServiceLoad();
        
            // update our load information
            ConnectionAcceptor acceptor = getAcceptor();
            if (acceptor instanceof Acceptor)
                {
                Acceptor acceptorImpl = (Acceptor) acceptor;
                load.setConnectionPendingCount(acceptorImpl.getConnectionPendingSet().size());
        
                com.tangosol.coherence.component.util.DaemonPool pool = acceptorImpl.getDaemonPool();
                if (pool.isStarted())
                    {
                    load.setDaemonActiveCount(pool.getActiveDaemonCount());
                    load.setMessageBacklogIncoming(pool.getBacklog());
                    }
        
                if (acceptor instanceof TcpAcceptor)
                    {
                    int cBacklog = 0;
                    try
                        {
                        for (Iterator iter = acceptorImpl.getConnectionSet().iterator();
                             iter.hasNext(); )
                            {
                            com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.TcpAcceptor.TcpConnection conn = (com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.TcpAcceptor.TcpConnection) iter.next();
                            if (conn != null)
                                {
                                cBacklog += conn.getOutgoingQueue().size();
                                }
                            }
                        }
                    catch (ConcurrentModificationException e)
                        {
                        // ignore
                        }
                    load.setMessageBacklogOutgoing(cBacklog);
                    }
                }
        
            updateServiceLoad();
            }
        }
    
    /**
     * Called by the service thread upon receipt of a NotifyConnectionClosed
    * message.
    * 
    * @parm request  the NotifyConnectionClosed message.
     */
    public void onNotifyConnectionClosed(ProxyService.NotifyConnectionClosed msg)
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Peer.Acceptor.TcpAcceptor$TcpConnection as com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.TcpAcceptor.TcpConnection;
        // import com.tangosol.net.management.Registry;
        // import com.tangosol.net.messaging.Connection;
        
        Connection connection = msg.getConnection();
        if (connection instanceof com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.TcpAcceptor.TcpConnection)
            {
            // unregister the ConnectionMBean
            Registry registry = getCluster().getManagement();
            if (registry != null)
                {
                String sName = registry.ensureGlobalName(Registry.CONNECTION_TYPE +
                        ",name=" + getServiceName()) + ",UUID=" + connection.getId();
                registry.unregister(sName);
                }
            }
        
        // update our load information
        ProxyService.ServiceLoad load = getServiceLoad();
        load.updateConnectionCount(-1);
        updateServiceLoad();
        updateLoadBalancer(getThisMember(), load, connection);
        }
    
    /**
     * Called by the service thread upon receipt of a NotifyConnectionOpened
    * message.
    * 
    * @parm request  the NotifyConnectionOpened message.
     */
    public void onNotifyConnectionOpened(ProxyService.NotifyConnectionOpened msg)
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Peer.Acceptor.TcpAcceptor$TcpConnection as com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.TcpAcceptor.TcpConnection;
        // import com.tangosol.net.management.Registry;
        // import com.tangosol.net.messaging.Connection;
        
        Connection connection = msg.getConnection();
        if (connection instanceof com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.TcpAcceptor.TcpConnection)
            {
            // register the ConnectionMBean 
            Registry registry = getCluster().getManagement();
            if (registry != null)
                {
                String sName = registry.ensureGlobalName(Registry.CONNECTION_TYPE +
                        ",name=" + getServiceName()) + ",UUID=" + connection.getId();
                registry.register(sName, connection);
                }
            }
        
        // update our load information
        ProxyService.ServiceLoad load = getServiceLoad();
        load.updateConnectionCount(1);
        updateServiceLoad();
        updateLoadBalancer(getThisMember(), load, connection);
        }
    
    // Declared at the super level
    /**
     * Called to complete the "service-joined" processing for the specified
    * member.
    * Called on the service thread only.
     */
    public void onNotifyServiceJoined(com.tangosol.coherence.component.net.Member member)
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Peer.Acceptor.GrpcAcceptor;
        // import Component.Util.Daemon.QueueProcessor.Service.Peer.Acceptor.HttpAcceptor;
        // import Component.Util.Daemon.QueueProcessor.Service.Peer.Acceptor.TcpAcceptor;
        // import com.tangosol.net.messaging.ConnectionAcceptor;
        // import com.tangosol.util.ObservableMap;
        // import java.util.Map;
        // import java.util.Set;
        
        super.onNotifyServiceJoined(member);
        
        ObservableMap map = getServiceMemberSet().getMemberConfigMap(member.getId());
        
        Map mapLoad = (Map) map.get("service-load");
        if (mapLoad != null)
            {
            ProxyService.ServiceLoadListener listener = (ProxyService.ServiceLoadListener) _newChild("ServiceLoadListener");
            listener.setMember(member);
        
            ProxyService.ServiceLoad load = new ProxyService.ServiceLoad();
            load.fromMap(mapLoad);
            load.setListener(listener);
        
            getServiceLoadMap().put(member, load);
            map.addMapListener(listener);
            updateLoadBalancer(member, load, null);
            }
        
        ConnectionAcceptor acceptor = getAcceptor();
        if (acceptor instanceof TcpAcceptor
            || acceptor instanceof HttpAcceptor
            || acceptor instanceof GrpcAcceptor)
            {
            getMemberListenAddresses().add(getListenSocketAddress(member));
            }
        }
    
    // Declared at the super level
    /**
     * Called to complete the "service-left" processing for the specified
    * member.  This notification is processed only after the associated
    * endpoint has been released by the message handler.  See
    * $NotifyServiceLeft#onReceived/#proceed.
    * Called on the service thread only.
    * Called on the service thread only.
     */
    public void onNotifyServiceLeft(com.tangosol.coherence.component.net.Member member)
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Peer.Acceptor.GrpcAcceptor;
        // import Component.Util.Daemon.QueueProcessor.Service.Peer.Acceptor.HttpAcceptor;
        // import Component.Util.Daemon.QueueProcessor.Service.Peer.Acceptor.TcpAcceptor;
        // import com.tangosol.net.messaging.ConnectionAcceptor;
        // import java.util.Set;
        
        Set                setAddr  = getMemberListenAddresses();
        ConnectionAcceptor acceptor = getAcceptor();
        
        if (!setAddr.isEmpty() && (acceptor instanceof TcpAcceptor
            || acceptor instanceof HttpAcceptor || acceptor instanceof GrpcAcceptor))
            {
            setAddr.remove(getListenSocketAddress(member));
            }
        
        super.onNotifyServiceLeft(member);
        
        ProxyService.ServiceLoad load = (ProxyService.ServiceLoad) getServiceLoadMap().remove(member);
        if (load != null)
            {
            ProxyService.ServiceLoadListener listener = load.getListener();
            if (listener != null)
                {
                getServiceMemberSet().getMemberConfigMap(member.getId())
                        .removeMapListener(listener);
                }
        
            updateLoadBalancer(member, null, null);
            }
        }
    
    /**
     * Called by the service thread upon receipt of a RedirectRequest to
    * determine whether or a Connection should be redirected.
    * 
    * @parm request  the RedirectRequest
     */
    public void onRedirectRequest(ProxyService.RedirectRequest request)
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Peer.Acceptor.TcpAcceptor$TcpConnection as com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.TcpAcceptor.TcpConnection;
        // import com.tangosol.net.Member;
        // import com.tangosol.net.messaging.Connection;
        // import com.tangosol.net.proxy.ProxyServiceLoadBalancer;
        // import com.tangosol.util.Base;
        // import java.net.Socket;
        // import java.util.ArrayList;
        // import java.util.Iterator;
        // import java.util.List;
        // import javax.security.auth.Subject;
        
        boolean fRedirect = false;
        
        Connection connection = request.getConnection();
        if (connection instanceof com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.TcpAcceptor.TcpConnection)
            {
            com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.TcpAcceptor.TcpConnection            connectionImpl = (com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.TcpAcceptor.TcpConnection) connection;
            ProxyServiceLoadBalancer balancer       = getLoadBalancer();
        
            // attempt to locate a better proxy for this connection
            List list;
        
            if (balancer == null
                    || connectionImpl.isRedirect()
                    || !connectionImpl.isRedirectSupported())
                {
                list = null;
                }
            else
                {
                Subject subject = connection.getChannel(0).getSubject();
                if (subject == null)
                    {
                    list = balancer.getMemberList(connectionImpl.getMember());
                    }
                else
                    {
                    ProxyService.LoadBalancerActionGetList action = new ProxyService.LoadBalancerActionGetList();
                    action.setBalancer(balancer);
                    action.setConnection(connectionImpl);
                    list = (List) Subject.doAs(subject, action);
                    }
                }
        
            // redirect, if possible
            if (list != null && !list.isEmpty())
                {
                Member memberThis = getThisMember();
                if (Base.equals(memberThis, list.get(0)))
                    {
                    // if the list starts with the local member, we are done
                    }
                else
                    {
                    // update the connection with redirection information
                    boolean          fConnectionPendingUpdated = false;
                    List             listAddr                  = new ArrayList(list.size());
                    Socket           socket                    = connectionImpl.getSocket();
        
                    for (Iterator iter = list.iterator(); iter.hasNext(); )
                        {
                        Member   member = (Member) iter.next();
                        if (Base.equals(memberThis, member))
                            {
                            // stop iterating once this member is reached
                            break;
                            }
        
                        Object[] result = getRoutableAddress(member, socket);
        
                        if (result != null)
                            {
                            listAddr.add(result);
                            if (!fConnectionPendingUpdated)
                                {
                                // assume that the client will reconnect to the first routable member
                                ProxyService.ServiceLoad load = (ProxyService.ServiceLoad) getServiceLoadMap().get(member);
                                if (load != null)
                                    {
                                    load.updateConnectionPendingCount(1);
                                    updateLoadBalancer(member, load, connectionImpl);
                                    }
                                fConnectionPendingUpdated = true;
                                }
                            }
                        }
                    if (!listAddr.isEmpty())
                        {
                        connectionImpl.setRedirect(true);
                        connectionImpl.setRedirectList(listAddr);
                        fRedirect = true;
                        }
                    }
                }
            }
        
        ProxyService.Response response = (ProxyService.Response) instantiateMessage("Response");
        request.ensureRequestPoll().setResult(Boolean.valueOf(fRedirect));
        response.respondTo(request);
        send(response);
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
        // import Component.Net.Member as com.tangosol.coherence.component.net.Member;
        // import Component.Util.Daemon.QueueProcessor.Service.Peer.Acceptor.GrpcAcceptor;
        // import Component.Util.Daemon.QueueProcessor.Service.Peer.Acceptor.HttpAcceptor;
        // import Component.Util.Daemon.QueueProcessor.Service.Peer.Acceptor.TcpAcceptor;
        // import com.tangosol.net.NameService;
        // import com.tangosol.net.messaging.ConnectionAcceptor;
        // import com.tangosol.util.Base;
        // import java.util.Iterator;
        
        com.tangosol.coherence.component.net.Member memberThis = getThisMember();
        for (Iterator iter = getServiceMemberSet().iterator(); iter.hasNext(); )
            {
            com.tangosol.coherence.component.net.Member member = (com.tangosol.coherence.component.net.Member) iter.next();
            if (!Base.equals(member, memberThis))
                {
                onNotifyServiceJoined(member);
                }
            }
        
        ConnectionAcceptor acceptor = getAcceptor();
        
        boolean fIsTcp  = acceptor instanceof TcpAcceptor;
        boolean fIsHttp = !fIsTcp && acceptor instanceof HttpAcceptor;
        boolean fIsGrpc = !fIsTcp && !fIsHttp && acceptor instanceof GrpcAcceptor;
        
        setResolveAllAddresses(fIsGrpc);
        
        if (fIsTcp || fIsHttp || fIsGrpc)
            {
            getMemberListenAddresses().add(getListenSocketAddress(memberThis));
            
            try
                {
                // register with the NameService
                NameService service = getNameService();
                if (service != null)
                    {
                    String sServiceName = getServiceName();
                    service.bind(sServiceName, this);
                    }
                }
            catch (Throwable e)
                {
                _trace("Failed to bind \"" + getServiceName() + "\" to the NameService: " + e, 2);
                }
            }
        
        super.onServiceStarted();
        }
    
    // Declared at the super level
    /**
     * The default implementation of this method does nothing.
     */
    protected void onServiceStarting()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Peer.Acceptor;
        // import Component.Util.Daemon.QueueProcessor.Service.Peer.Acceptor.GrpcAcceptor;
        // import Component.Util.Daemon.QueueProcessor.Service.Peer.Acceptor.HttpAcceptor;
        // import Component.Util.Daemon.QueueProcessor.Service.Peer.Acceptor.TcpAcceptor;
        // import com.oracle.coherence.common.net.InetSocketAddress32;
        // import com.oracle.coherence.common.net.SSLSocketProvider;
        // import com.tangosol.net.InetAddressHelper;
        // import com.tangosol.net.management.Registry;
        // import com.tangosol.net.messaging.ConnectionAcceptor;
        // import com.tangosol.util.LiteMap;
        // import java.net.InetAddress;
        // import java.util.Collection;
        // import java.util.Collections;
        // import java.util.Iterator;
        // import java.util.List;
        // import java.util.Map;
        // import java.util.StringJoiner;
        
        super.onServiceStarting();
        
        ConnectionAcceptor acceptor = getAcceptor();
        
        // set the ConnectionAcceptor ClassLoader
        acceptor.setContextClassLoader(getContextClassLoader());
        
        // register the connection filter
        acceptor.setConnectionFilter(this);
        
        // configure the proxy Serializers
        if (acceptor instanceof Acceptor)
            {
            Acceptor acceptorImpl = (Acceptor) acceptor;
            getCacheServiceProxy().setSerializer(acceptorImpl.ensureSerializer());
            getInvocationServiceProxy().setSerializer(acceptorImpl.ensureSerializer());
            }
        else
            {
            getCacheServiceProxy().setSerializer(ensureSerializer());
            getInvocationServiceProxy().setSerializer(ensureSerializer());
            }
        
        // update our load information
        ProxyService.ServiceLoad load = getServiceLoad();
        if (acceptor instanceof Acceptor)
            {
            Acceptor acceptorImpl = (Acceptor) acceptor;
            load.setConnectionLimit(acceptorImpl.getConnectionLimit());
            load.setDaemonCount(acceptorImpl.getDaemonPool().getDaemonCount());
            }
        updateServiceLoad();
        updateLoadBalancer(getThisMember(), load, null);
        
        // register the connection listener to maintain the ConnectionMBeans
        if (acceptor instanceof TcpAcceptor)
            {
            ((TcpAcceptor) acceptor).addConnectionListener(this);
            }
        
        // COH-4604: Guard the acceptor
        if (acceptor instanceof Acceptor)
            {
            guard(((Acceptor) acceptor).getGuardable());
            }
        
        // start the acceptor
        acceptor.start();
        
        // register the ConnectionManagerMBean
        Registry registry = getCluster().getManagement();
        if (registry != null)
            {
            String sName = registry.ensureGlobalName(Registry.CONNECTION_MANAGER_TYPE +
                    ",name=" + getServiceName());
            registry.register(sName, acceptor);
            }
        
        boolean fIsTcp  = acceptor instanceof TcpAcceptor;
        boolean fIsHttp = !fIsTcp && acceptor instanceof HttpAcceptor;
        boolean fIsGrpc = !fIsTcp && !fIsHttp && acceptor instanceof GrpcAcceptor;
        if (fIsTcp || fIsHttp || fIsGrpc)
            {
            // update the listen address(es) and port
            Map                 map = new LiteMap();
            InetSocketAddress32 sockAddr;
            String              sProtocol = null;
        
            if (fIsTcp)
                {
                sockAddr = (InetSocketAddress32) ((TcpAcceptor) acceptor)
                        .getProcessor().getServerSocket().getLocalSocketAddress();
                }
            else if (fIsHttp)
                {
                HttpAcceptor acceptorImpl = (HttpAcceptor) acceptor;
                
                sockAddr = new InetSocketAddress32(acceptorImpl.getListenAddress(), acceptorImpl.getListenPort());
                sProtocol = acceptorImpl.getSocketProvider() instanceof SSLSocketProvider ? "https" : "http";
                }
            else
                {
                GrpcAcceptor acceptorImpl = (GrpcAcceptor) acceptor;
                sockAddr = new InetSocketAddress32(acceptorImpl.getListenAddress(), acceptorImpl.getListenPort());
                }
        
            InetAddress addr = sockAddr.getAddress();
        
            map.put("listen-address", addr.getHostAddress());
            map.put("listen-port",    Integer.valueOf(sockAddr.getPort()));
            if (sProtocol != null)
                {
                map.put("http-protocol", sProtocol);
                }
        
            Collection colAddr = addr.isAnyLocalAddress()
                ? InetAddressHelper.getLocalBindableAddresses()
                : Collections.singletonList(addr);
        
            StringJoiner sjFull = new StringJoiner(",");
            for (Iterator iter = colAddr.iterator(); iter.hasNext(); )
                {
                sjFull.add(((InetAddress) iter.next()).getHostAddress());
                }
            map.put("listen-addresses", sjFull.toString());
        
            getThisMemberConfigMap().putAll(map);
            }
        }
    
    // Declared at the super level
    /**
     * The default implementation of this method sets AcceptingClients to false.
     */
    protected void onServiceStopped()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Peer.Acceptor.GrpcAcceptor;
        // import Component.Util.Daemon.QueueProcessor.Service.Peer.Acceptor.HttpAcceptor;
        // import Component.Util.Daemon.QueueProcessor.Service.Peer.Acceptor.TcpAcceptor;
        // import com.tangosol.net.NameService;
        // import com.tangosol.net.management.Registry;
        // import com.tangosol.net.messaging.ConnectionAcceptor;
        
        super.onServiceStopped();
        
        // force the ConnectionAcceptor to stop if it is still running
        ConnectionAcceptor acceptor = getAcceptor();
        acceptor.stop();
        
        // update our load information
        updateLoadBalancer(getThisMember(), null, null);
        
        // unregister the ConnectionManagerMBean
        Registry registry = getCluster().getManagement();
        if (registry != null)
            {
            String sName = registry.ensureGlobalName(Registry.CONNECTION_MANAGER_TYPE +
                    ",name=" + getServiceName());
            registry.unregister(sName);
            }
            
        if (acceptor instanceof TcpAcceptor
            || acceptor instanceof HttpAcceptor
            || acceptor instanceof GrpcAcceptor)
            {
            String sAddr = getListenSocketAddress(getThisMember());
            if (sAddr != null)
                {
                getMemberListenAddresses().remove(sAddr);
                }
        
            // deregister from the NameService
            NameService service = getNameService();
            if (service != null)
                {
                try
                    {
                    service.unbind(getServiceName());
                    }
                catch (Throwable e)
                    {
                    _trace("Failed to unbind \"" + getServiceName() + "\" from the NameService: " + e, 2);
                    }
                }
            }
        }
    
    // Declared at the super level
    /**
     * The default implementation of this method sets AcceptingClients to false.
     */
    protected void onServiceStopping()
        {
        super.onServiceStopping();
        
        // attempt to perform a "graceful" shutdown of the ConnectionAcceptor
        getAcceptor().shutdown();
        }
    
    // Declared at the super level
    /**
     * Reset the Service statistics.
    * Reset the statistics.
     */
    public void resetStats()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Peer.Acceptor;
        // import com.tangosol.net.messaging.ConnectionAcceptor;
        
        ConnectionAcceptor acceptor = getAcceptor();
        if (acceptor instanceof Acceptor)
            {
            ((Acceptor) acceptor).resetStats();
            }
        super.resetStats();
        }
    
    // From interface: com.tangosol.net.NameService$Resolvable
    /**
     * Obtain the object that will be returned by NameService.lookup()
    * 
    * @param ctx  the lookup request context
    * 
    * @return the resolved object
     */
    public Object resolve(com.tangosol.net.NameService.RequestContext ctx)
        {
        // import com.tangosol.net.InetAddressHelper;
        // import com.tangosol.net.Member;
        // import com.tangosol.net.proxy.ProxyServiceLoadBalancer;
        // import com.tangosol.util.Base;
        // import java.util.HashSet;
        // import java.util.List;
        // import java.util.Set;
        
        ProxyServiceLoadBalancer balancer = getLoadBalancer();
        Object[]                 result   = null;
        
        List list = balancer == null ? null : balancer.getMemberList(ctx.getMember());
        if (list != null)
            {
            if (isResolveAllAddresses())
                {
                result = getRoutableAddresses(list, ctx);
                }
            else
                {
                result = getRoutableAddress(list, ctx);
                }
            }
        
        if (result == null)
            {
            // check remaining higher loaded members
            Set setRemainingMembers;
        
            if (list == null)
                {
                setRemainingMembers = getServiceMemberSet();
                }
            else
                {
                setRemainingMembers = new HashSet(getServiceMemberSet());
                setRemainingMembers.removeAll(list);
                }
        
            result = getRoutableAddress(Base.randomize(setRemainingMembers), ctx);
            }
        
        if (result == null)
            {
            // failed to find a routable member
            Member member = getThisMember();
            result = new Object[]
                {
                getListenAddress(member),
                getListenPort(member)
                };
            _trace("No route could be determined from "
                + ctx.getMember() + " to \"" + getServiceName()
                + "\".  Returning \"" + result[0] + ':'
                + InetAddressHelper.toString(((Integer) result[1]).intValue()) + '\"', 2);
            }
        
        return result;
        }
    
    // Accessor for the property "Acceptor"
    /**
     * Setter for property Acceptor.<p>
    * ConnectionAcceptor used by the ProxyService to accept connections from
    * non-clustered Service clients (Stubs).
     */
    protected void setAcceptor(com.tangosol.net.messaging.ConnectionAcceptor acceptor)
        {
        _assert(getAcceptor() == null);
        __m_Acceptor = (acceptor);
        }
    
    // Accessor for the property "CacheServiceProxy"
    /**
     * Setter for property CacheServiceProxy.<p>
    * The cluster side portion (Proxy) of the client-to-cluster CacheService
    * Adapter.
     */
    protected void setCacheServiceProxy(com.tangosol.coherence.component.net.extend.proxy.serviceProxy.CacheServiceProxy proxy)
        {
        _assert(getCacheServiceProxy() == null);
        __m_CacheServiceProxy = (proxy);
        }
    
    // From interface: com.tangosol.net.ProxyService
    // Declared at the super level
    /**
     * Setter for property ContextClassLoader.<p>
    * @see com.tangosol.io.ClassLoaderAware
     */
    public void setContextClassLoader(ClassLoader loader)
        {
        // import Component.Net.Extend.Proxy.ServiceProxy.CacheServiceProxy;
        // import com.tangosol.net.messaging.ConnectionAcceptor;
        
        super.setContextClassLoader(loader);
        
        ConnectionAcceptor acceptor = getAcceptor();
        if (acceptor != null)
            {
            acceptor.setContextClassLoader(loader);
            }
        
        CacheServiceProxy proxy = getCacheServiceProxy();
        if (proxy != null)
            {
            proxy.setContextClassLoader(loader);
            }
        }
    
    // Accessor for the property "InvocationServiceProxy"
    /**
     * Setter for property InvocationServiceProxy.<p>
    * The cluster side portion (Proxy) of the client-to-cluster
    * InvocationService Adapter.
     */
    protected void setInvocationServiceProxy(com.tangosol.coherence.component.net.extend.proxy.serviceProxy.InvocationServiceProxy proxy)
        {
        _assert(getInvocationServiceProxy() == null);
        __m_InvocationServiceProxy = (proxy);
        }
    
    // Accessor for the property "LoadBalancer"
    /**
     * Setter for property LoadBalancer.<p>
    * The configured ProxyServiceLoadBalancer for this ProxyService.
     */
    protected void setLoadBalancer(com.tangosol.net.proxy.ProxyServiceLoadBalancer balancer)
        {
        _assert(balancer != null);
        __m_LoadBalancer = (balancer);
        }
    
    // Accessor for the property "MemberListenAddresses"
    /**
     * Setter for property MemberListenAddresses.<p>
    * A sorted set of TCP/IP listen addresses for the current service Members.
     */
    protected void setMemberListenAddresses(java.util.NavigableSet setMember)
        {
        __m_MemberListenAddresses = setMember;
        }
    
    // Accessor for the property "ResolveAllAddresses"
    /**
     * Setter for property ResolveAllAddresses.<p>
     */
    public void setResolveAllAddresses(boolean fAddresses)
        {
        __m_ResolveAllAddresses = fAddresses;
        }
    
    // Accessor for the property "ServiceLoad"
    /**
     * Setter for property ServiceLoad.<p>
    * The ServiceLoad for this ProxyService.
     */
    protected void setServiceLoad(ProxyService.ServiceLoad load)
        {
        __m_ServiceLoad = load;
        }
    
    // Accessor for the property "ServiceLoadMap"
    /**
     * Setter for property ServiceLoadMap.<p>
    * A Map of ServiceLoad objects, keyed by their associated Member objects.
     */
    protected void setServiceLoadMap(java.util.Map map)
        {
        _assert(map != null);
        __m_ServiceLoadMap = (map);
        }
    
    // Accessor for the property "ServiceLoadPublished"
    /**
     * Setter for property ServiceLoadPublished.<p>
    * The last published ServiceLoad for this ProxyService.
     */
    protected void setServiceLoadPublished(ProxyService.ServiceLoad load)
        {
        __m_ServiceLoadPublished = load;
        }
    
    // Accessor for the property "ServiceLoadTimeMillis"
    /**
     * Setter for property ServiceLoadTimeMillis.<p>
    * The last time the ServiceLoad was evaluated via the updateServiceLoad()
    * method.
     */
    protected void setServiceLoadTimeMillis(long c)
        {
        __m_ServiceLoadTimeMillis = c;
        }
    
    /**
     * Update the load balancing strategy in response to a change in a
    * ProxyService utilization.
    * 
    *  @param member     the Member for which the utilization changed
    *  @param load       the updated ProxyServiceLoad
    *  @param connection the connection for this request
     */
    public void updateLoadBalancer(com.tangosol.net.Member member, ProxyService.ServiceLoad load, com.tangosol.net.messaging.Connection connection)
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Peer.Acceptor.TcpAcceptor$TcpConnection as com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.TcpAcceptor.TcpConnection;
        // import com.tangosol.net.proxy.ProxyServiceLoadBalancer;
        // import javax.security.auth.Subject;
        
        ProxyServiceLoadBalancer balancer = getLoadBalancer();
        if (balancer != null)
            {
            if (load != null)
                {
                // clone the member's ServiceLoad instance for the LoadBalancer
                ProxyService.ServiceLoad loadService = new ProxyService.ServiceLoad();
                loadService.fromMap(load.toMap());
                load = loadService;
                }
        
            Subject subject = connection instanceof com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.TcpAcceptor.TcpConnection
                    ? ((com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.TcpAcceptor.TcpConnection) connection).getChannel(0).getSubject() : null;
            if (subject == null)
                {
                balancer.update(member, load);
                }
            else
                {
                ProxyService.LoadBalancerActionUpdate action = new ProxyService.LoadBalancerActionUpdate();
                action.setBalancer(balancer);
                action.setLoad(load);
                action.setmember(member);
                Subject.doAs(subject, action);
                }
            }
        }
    
    /**
     * Update the Member config map with a representation of the local
    * ServiceLoad.
     */
    protected void updateServiceLoad()
        {
        // import com.tangosol.net.ServiceLoadBalancer as com.tangosol.net.ServiceLoadBalancer;
        // import com.tangosol.util.Base;
        // import java.util.Map;
        
        // convert the ServiceLoad to a map representation and persist it in the
        // Member config map
        
        ProxyService.ServiceLoad load     = getServiceLoad();
        ProxyService.ServiceLoad loadLast = getServiceLoadPublished();
        com.tangosol.net.ServiceLoadBalancer balancer = getLoadBalancer();
        
        if (loadLast == null ||
            (balancer == null ? load.compareTo(loadLast) : balancer.compare(load, loadLast)) != 0)
            {
            // only publish the updated load if it differs (according to the comparator)
            // to the last published load. This is important in large clusters as this is
            // published to the full set of members.
        
            Map mapLoad = load.toMap();
            getThisMemberConfigMap().put("service-load", mapLoad);
        
            if (loadLast == null)
                {
                loadLast = new ProxyService.ServiceLoad();
                setServiceLoadPublished(loadLast);
                }
        
            loadLast.fromMap(mapLoad);
            }
        
        setServiceLoadTimeMillis(Base.getSafeTimeMillis());
        }
    
    // Declared at the super level
    /**
     * Validate the specified service config against this member.
    * 
    * @param xmlConfig   the service config
    * 
    * @return true  if this member is consistent with the specified config
     */
    protected boolean validateServiceConfig(com.tangosol.run.xml.XmlElement xmlConfig)
        {
        // import com.tangosol.net.ActionPolicy;
        // import com.tangosol.run.xml.XmlElement;
        
        ActionPolicy quorumPolicy  = getActionPolicy();
        String       sQuorumPolicy = quorumPolicy == null ? "none" : quorumPolicy.getClass().getName();
        
        // 1) as of Coherence 12.2.1.1.0 the default quorum policy class has changed from
        //      com.tangosol.net.ConfigurableQuorumPolicy$WrapperQuorumPolicy
        // to
        //      com.tangosol.util.NullImplementation$NullActionPolicy
        
        // Even if all the nodes are of the current version now, there is a chance that
        // the original service senior was of an old version and we need to allow to proceed
        // with a legacy configuration elements
        
        final String QUORUM_DEFAULT_LEGACY = "com.tangosol.net.ConfigurableQuorumPolicy$WrapperQuorumPolicy";
        final String QUORUM_DEFAULT_12211  = "com.tangosol.util.NullImplementation$NullActionPolicy";
        
        String sQuorumGlobal = xmlConfig.getSafeAttribute("quorum-policy").getString();
        if (QUORUM_DEFAULT_LEGACY.equals(sQuorumGlobal))
            {
            // the new default is compatible with the legacy default
            sQuorumGlobal = QUORUM_DEFAULT_12211;
            }
        
        return super.validateServiceConfig(xmlConfig) &&
               verifyFeature("QuorumPolicy", sQuorumPolicy, sQuorumGlobal);
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ProxyService$LoadBalancerActionGetList
    
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class LoadBalancerActionGetList
            extends    com.tangosol.coherence.component.Util
            implements java.security.PrivilegedAction
        {
        // ---- Fields declarations ----
        
        /**
         * Property Balancer
         *
         * The proxy service load balancer.
         */
        private com.tangosol.net.proxy.ProxyServiceLoadBalancer __m_Balancer;
        
        /**
         * Property Connection
         *
         * The client connection.
         */
        private com.tangosol.net.messaging.Connection __m_Connection;
        
        // Default constructor
        public LoadBalancerActionGetList()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public LoadBalancerActionGetList(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ProxyService.LoadBalancerActionGetList();
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
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/ProxyService$LoadBalancerActionGetList".replace('/', '.'));
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
        
        // Accessor for the property "Balancer"
        /**
         * Getter for property Balancer.<p>
        * The proxy service load balancer.
         */
        public com.tangosol.net.proxy.ProxyServiceLoadBalancer getBalancer()
            {
            return __m_Balancer;
            }
        
        // Accessor for the property "Connection"
        /**
         * Getter for property Connection.<p>
        * The client connection.
         */
        public com.tangosol.net.messaging.Connection getConnection()
            {
            return __m_Connection;
            }
        
        // From interface: java.security.PrivilegedAction
        /**
         * Get the list of memebers a client should be redirected to.
         */
        public Object run()
            {
            // import Component.Util.Daemon.QueueProcessor.Service.Peer.Acceptor.TcpAcceptor$TcpConnection as com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.TcpAcceptor.TcpConnection;
            // import com.tangosol.net.messaging.Connection;
            
            Connection connection = getConnection();
            return connection instanceof com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.TcpAcceptor.TcpConnection
                    ? getBalancer().getMemberList(((com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.TcpAcceptor.TcpConnection) connection).getMember()) : null;
            }
        
        // Accessor for the property "Balancer"
        /**
         * Setter for property Balancer.<p>
        * The proxy service load balancer.
         */
        public void setBalancer(com.tangosol.net.proxy.ProxyServiceLoadBalancer balancer)
            {
            __m_Balancer = balancer;
            }
        
        // Accessor for the property "Connection"
        /**
         * Setter for property Connection.<p>
        * The client connection.
         */
        public void setConnection(com.tangosol.net.messaging.Connection connection)
            {
            __m_Connection = connection;
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ProxyService$LoadBalancerActionUpdate
    
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class LoadBalancerActionUpdate
            extends    com.tangosol.coherence.component.Util
            implements java.security.PrivilegedAction
        {
        // ---- Fields declarations ----
        
        /**
         * Property Balancer
         *
         * The proxy service load balancer.
         */
        private com.tangosol.net.proxy.ProxyServiceLoadBalancer __m_Balancer;
        
        /**
         * Property Load
         *
         * The service load for the member.
         */
        private com.tangosol.net.proxy.ProxyServiceLoad __m_Load;
        
        /**
         * Property member
         *
         * The member of the load to update.
         */
        private com.tangosol.net.Member __m_member;
        
        // Default constructor
        public LoadBalancerActionUpdate()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public LoadBalancerActionUpdate(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ProxyService.LoadBalancerActionUpdate();
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
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/ProxyService$LoadBalancerActionUpdate".replace('/', '.'));
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
        
        // Accessor for the property "Balancer"
        /**
         * Getter for property Balancer.<p>
        * The proxy service load balancer.
         */
        public com.tangosol.net.proxy.ProxyServiceLoadBalancer getBalancer()
            {
            return __m_Balancer;
            }
        
        // Accessor for the property "Load"
        /**
         * Getter for property Load.<p>
        * The service load for the member.
         */
        public com.tangosol.net.proxy.ProxyServiceLoad getLoad()
            {
            return __m_Load;
            }
        
        // Accessor for the property "member"
        /**
         * Getter for property member.<p>
        * The member of the load to update.
         */
        public com.tangosol.net.Member getmember()
            {
            return __m_member;
            }
        
        // From interface: java.security.PrivilegedAction
        /**
         * Upate the load of a member.
         */
        public Object run()
            {
            getBalancer().update(getmember(), getLoad());
            return null;
            }
        
        // Accessor for the property "Balancer"
        /**
         * Setter for property Balancer.<p>
        * The proxy service load balancer.
         */
        public void setBalancer(com.tangosol.net.proxy.ProxyServiceLoadBalancer balancer)
            {
            __m_Balancer = balancer;
            }
        
        // Accessor for the property "Load"
        /**
         * Setter for property Load.<p>
        * The service load for the member.
         */
        public void setLoad(com.tangosol.net.proxy.ProxyServiceLoad load)
            {
            __m_Load = load;
            }
        
        // Accessor for the property "member"
        /**
         * Setter for property member.<p>
        * The member of the load to update.
         */
        public void setmember(com.tangosol.net.Member member)
            {
            __m_member = member;
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ProxyService$LookupCallback
    
    /**
     * Implementation of the NameService$LookupCallBack interface.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class LookupCallback
            extends    com.tangosol.coherence.component.Util
            implements com.tangosol.net.NameService.LookupCallback
        {
        // ---- Fields declarations ----
        
        // Default constructor
        public LookupCallback()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public LookupCallback(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ProxyService.LookupCallback();
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
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/ProxyService$LookupCallback".replace('/', '.'));
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
        
        // From interface: com.tangosol.net.NameService$LookupCallback
        /**
         * Retrieves the named object.
        * 
        * @param sName    the name of the object to look up
        * @param cluster  the running Cluster
        * @param ctx      the lookup request context
        * 
        * @return the object bound to sName, or <tt>null</tt> if not found
         */
        public Object lookup(String sName, com.tangosol.net.Cluster cluster, com.tangosol.net.NameService.RequestContext ctx)
                throws javax.naming.NamingException
            {
            // import Component.Net.MemberSet.ActualMemberSet.ServiceMemberSet;
            // import Component.Net.ServiceInfo;
            // import com.tangosol.net.Member;
            // import com.tangosol.net.ProxyService as com.tangosol.net.ProxyService;
            // import com.tangosol.util.Base;
            // import java.util.Iterator;
            
            Object      result      = null;
            ServiceInfo serviceInfo = (ServiceInfo) cluster.getServiceInfo(sName);
            
            if (serviceInfo != null &&
                Base.equals(serviceInfo.getServiceType(), com.tangosol.net.ProxyService.TYPE_DEFAULT))
                {
                ServiceMemberSet memberSet = serviceInfo.getMemberSet();
            
                // select a random member for basic load balancing
                int nMemberIdRandom = memberSet.random();
                if (nMemberIdRandom != 0)
                    {
                    result = ProxyService.getRoutableAddress(memberSet.getMember(nMemberIdRandom),
                        memberSet.getMemberConfigMap(nMemberIdRandom), cluster, ctx);
            
                    if (result == null)
                        {
                        // no routable address found to the random member
                        // iterate over the remaining members
                        for (Iterator iter = memberSet.iterator(); iter.hasNext() && result == null; )
                            {
                            Member member    = (Member) iter.next();
                            int    nMemberID = member.getId();
                            if (nMemberID != nMemberIdRandom)
                                {
                                result = ProxyService.getRoutableAddress(member, memberSet.getMemberConfigMap(nMemberID), cluster, ctx);
                                }
                            }
                        }
                    }
                }
            else if (sName.endsWith("/addresses")) // discovery request
                {
                String serviceName = sName.split("/")[0];
                serviceInfo = (ServiceInfo) cluster.getServiceInfo(serviceName);
                if (serviceInfo != null &&
                    Base.equals(serviceInfo.getServiceType(), com.tangosol.net.ProxyService.TYPE_DEFAULT))
                    {
                    ServiceMemberSet memberSet = serviceInfo.getMemberSet();
            
                    result = ProxyService.getRoutableMemberAddresses(memberSet, cluster, ctx);
                    }
                }
            
            return result;
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ProxyService$NotifyConnectionClosed
    
    /**
     * Message sent by an Acceptor service thread when an existing Connection
     * is closed (see #onConnectionClosed).
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class NotifyConnectionClosed
            extends    com.tangosol.coherence.component.net.Message
        {
        // ---- Fields declarations ----
        
        /**
         * Property Connection
         *
         * The newly closed Connection.
         */
        private com.tangosol.net.messaging.Connection __m_Connection;
        
        // Default constructor
        public NotifyConnectionClosed()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public NotifyConnectionClosed(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                setMessageType(258);
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ProxyService.NotifyConnectionClosed();
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
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/ProxyService$NotifyConnectionClosed".replace('/', '.'));
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
        
        // Accessor for the property "Connection"
        /**
         * Getter for property Connection.<p>
        * The newly closed Connection.
         */
        public com.tangosol.net.messaging.Connection getConnection()
            {
            return __m_Connection;
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
            
            ((ProxyService) getService()).onNotifyConnectionClosed(this);
            }
        
        // Accessor for the property "Connection"
        /**
         * Setter for property Connection.<p>
        * The newly closed Connection.
         */
        public void setConnection(com.tangosol.net.messaging.Connection connection)
            {
            __m_Connection = connection;
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ProxyService$NotifyConnectionOpened
    
    /**
     * Message sent by an Acceptor service thread when a new Connection is
     * established (see #onConnectionOpened).
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class NotifyConnectionOpened
            extends    com.tangosol.coherence.component.net.Message
        {
        // ---- Fields declarations ----
        
        /**
         * Property Connection
         *
         * The newly established Connection.
         */
        private com.tangosol.net.messaging.Connection __m_Connection;
        
        // Default constructor
        public NotifyConnectionOpened()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public NotifyConnectionOpened(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                setMessageType(257);
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ProxyService.NotifyConnectionOpened();
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
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/ProxyService$NotifyConnectionOpened".replace('/', '.'));
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
        
        // Accessor for the property "Connection"
        /**
         * Getter for property Connection.<p>
        * The newly established Connection.
         */
        public com.tangosol.net.messaging.Connection getConnection()
            {
            return __m_Connection;
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
            
            ((ProxyService) getService()).onNotifyConnectionOpened(this);
            }
        
        // Accessor for the property "Connection"
        /**
         * Setter for property Connection.<p>
        * The newly established Connection.
         */
        public void setConnection(com.tangosol.net.messaging.Connection connection)
            {
            __m_Connection = connection;
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ProxyService$RedirectRequest
    
    /**
     * Request sent by an Acceptor service thread when a new Connection is
     * established (see #checkConnection) to determine whether or not the
     * Connection should be redirected.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class RedirectRequest
            extends    com.tangosol.coherence.component.net.message.RequestMessage
        {
        // ---- Fields declarations ----
        
        /**
         * Property Connection
         *
         */
        private com.tangosol.net.messaging.Connection __m_Connection;
        
        // Default constructor
        public RedirectRequest()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public RedirectRequest(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                setMessageType(259);
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ProxyService.RedirectRequest();
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
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/ProxyService$RedirectRequest".replace('/', '.'));
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
        
        // Accessor for the property "Connection"
        /**
         * Getter for property Connection.<p>
         */
        public com.tangosol.net.messaging.Connection getConnection()
            {
            return __m_Connection;
            }
        
        // Declared at the super level
        protected com.tangosol.coherence.component.net.Poll instantiatePoll()
            {
            // import Component.Net.Poll;
            
            return new Poll();
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
            
            ((ProxyService) getService()).onRedirectRequest(this);
            }
        
        // Accessor for the property "Connection"
        /**
         * Setter for property Connection.<p>
         */
        public void setConnection(com.tangosol.net.messaging.Connection connection)
            {
            __m_Connection = connection;
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ProxyService$ServiceLoad
    
    /**
     * Implementation of the ProxyServiceLoad interface.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class ServiceLoad
            extends    com.tangosol.coherence.component.net.ServiceLoad
            implements com.tangosol.net.proxy.ProxyServiceLoad
        {
        // ---- Fields declarations ----
        
        /**
         * Property Listener
         *
         * The ServiceLoadListener that is reponsible for updating this
         * ServiceLoad.
         */
        private ProxyService.ServiceLoadListener __m_Listener;
        
        // Default constructor
        public ServiceLoad()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public ServiceLoad(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                setConnectionFactor(-1);
                setDaemonFactor(-1);
                setMessageBacklogFactor(-1);
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ProxyService.ServiceLoad();
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
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/ProxyService$ServiceLoad".replace('/', '.'));
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
        public boolean equals(Object obj)
            {
            if (obj == this)
                {
                return true;
                }
            
            if (obj instanceof ProxyService.ServiceLoad)
                {
                return compareTo(obj) == 0;
                }
            
            return false;
            }
        
        // Accessor for the property "Listener"
        /**
         * Getter for property Listener.<p>
        * The ServiceLoadListener that is reponsible for updating this
        * ServiceLoad.
         */
        public ProxyService.ServiceLoadListener getListener()
            {
            return __m_Listener;
            }
        
        // Declared at the super level
        public int hashCode()
            {
            return getConnectionFactor();
            }
        
        // Accessor for the property "Listener"
        /**
         * Setter for property Listener.<p>
        * The ServiceLoadListener that is reponsible for updating this
        * ServiceLoad.
         */
        public void setListener(ProxyService.ServiceLoadListener listener)
            {
            __m_Listener = listener;
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ProxyService$ServiceLoadListener
    
    /**
     * MapListener that is responsible for updating the
     * ProxyServiceLoadBalancer with the ServiceLoad for it's associated Member.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class ServiceLoadListener
            extends    com.tangosol.coherence.component.net.extend.Util
            implements com.tangosol.util.MapListener
        {
        // ---- Fields declarations ----
        
        /**
         * Property Member
         *
         * The associated Member.
         */
        private com.tangosol.net.Member __m_Member;
        
        // Default constructor
        public ServiceLoadListener()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public ServiceLoadListener(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ProxyService.ServiceLoadListener();
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
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/ProxyService$ServiceLoadListener".replace('/', '.'));
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
            entryUpdated(evt);
            }
        
        // From interface: com.tangosol.util.MapListener
        public void entryInserted(com.tangosol.util.MapEvent evt)
            {
            entryUpdated(evt);
            }
        
        // From interface: com.tangosol.util.MapListener
        public void entryUpdated(com.tangosol.util.MapEvent evt)
            {
            // import com.tangosol.net.Member;
            // import java.util.Map;
            
            if ("service-load".equals(evt.getKey()))
                {
                ProxyService module = (ProxyService) get_Module();
                Member  member = getMember();
            
                // update the ServiceLoad for the Member
                ProxyService.ServiceLoad load = null;
                if (evt.getNewValue() instanceof Map)
                    {
                    load = (ProxyService.ServiceLoad) module.getServiceLoadMap().get(member);
                    if (load != null)
                        {
                        load.fromMap((Map) evt.getNewValue());
                        }
                    }
            
                // update the load balancing strategy
                module.updateLoadBalancer(member, load, null);
                }
            }
        
        // Declared at the super level
        public boolean equals(Object obj)
            {
            // import com.tangosol.util.Base;
            
            if (obj == this)
                {
                return true;
                }
            
            if (obj instanceof ProxyService.ServiceLoadListener)
                {
                ServiceLoadListener that = (ServiceLoadListener) obj;
                return Base.equals(getMember(), that.getMember());
                }
            
            return false;
            }
        
        // Accessor for the property "Member"
        /**
         * Getter for property Member.<p>
        * The associated Member.
         */
        public com.tangosol.net.Member getMember()
            {
            return __m_Member;
            }
        
        // Accessor for the property "Member"
        /**
         * Setter for property Member.<p>
        * The associated Member.
         */
        public void setMember(com.tangosol.net.Member member)
            {
            _assert(member != null);
            __m_Member = (member);
            }
        }
    }
