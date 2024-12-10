
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.extend.RemoteService

package com.tangosol.coherence.component.net.extend;

import com.tangosol.coherence.component.net.extend.Channel;
import com.tangosol.coherence.component.net.extend.remoteService.RemoteNameService;
import com.tangosol.coherence.component.net.memberSet.actualMemberSet.ServiceMemberSet;
import com.tangosol.coherence.component.util.daemon.queueProcessor.Service;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.Initiator;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.initiator.TcpInitiator;
import com.oracle.coherence.common.net.InetSocketAddress32;
import com.tangosol.coherence.config.builder.SocketProviderBuilder;
import com.tangosol.internal.net.service.extend.remote.DefaultRemoteNameServiceDependencies;
import com.tangosol.internal.net.service.extend.remote.DefaultRemoteServiceDependencies;
import com.tangosol.internal.net.service.extend.remote.RemoteServiceDependencies;
import com.tangosol.internal.net.service.peer.initiator.DefaultTcpInitiatorDependencies;
import com.tangosol.internal.net.service.peer.initiator.InitiatorDependencies;
import com.tangosol.internal.net.service.peer.initiator.TcpInitiatorDependencies;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.CompositeSocketAddressProvider;
import com.tangosol.net.Member;
import com.tangosol.net.MemberEvent;
import com.tangosol.net.NameService;
import com.tangosol.net.OperationalContext;
import com.tangosol.net.SocketProviderFactory;
import com.tangosol.net.messaging.Connection;
import com.tangosol.net.messaging.ConnectionException;
import com.tangosol.net.messaging.ConnectionInitiator;
import com.tangosol.net.messaging.ConnectionManager;
import com.tangosol.util.Listeners;
import com.tangosol.util.ServiceEvent;
import java.util.Collections;
import java.util.function.IntPredicate;

/**
 * Service implementation that allows a JVM to use a remote clustered Service
 * without having to join the Cluster.
 * 
 * @see Component.Util.Daemon.QueueProcessor.Service.Grid.ProxyService
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public abstract class RemoteService
        extends    com.tangosol.coherence.component.net.Extend
        implements com.tangosol.net.Service,
                   com.tangosol.net.ServiceInfo,
                   com.tangosol.net.messaging.ConnectionListener,
                   com.tangosol.util.ServiceListener
    {
    // ---- Fields declarations ----
    
    /**
     * Property Channel
     *
     * The Channel used to send and receive Messages to/from the remote
     * ProxyService.
     * 
     * @volatile
     */
    private volatile com.tangosol.net.messaging.Channel __m_Channel;
    
    /**
     * Property Cluster
     *
     * @see com.tangosol.net.Service#getCluster
     */
    private com.tangosol.net.Cluster __m_Cluster;
    
    /**
     * Property ContextClassLoader
     *
     * @see com.tangosol.util.Controllable#getContextClassLoader
     */
    private ClassLoader __m_ContextClassLoader;
    
    /**
     * Property Dependencies
     *
     * The external dependencies needed by this component. The dependencies
     * object must be full populated and validated before this property is set.
     *  See setDependencies.  
     * 
     * The mechanism for creating and populating dependencies is hidden from
     * this component. Typically, the dependencies object is populated using
     * data from some external configuration, such as XML, but this may not
     * always be the case.
     */
    private com.tangosol.net.ServiceDependencies __m_Dependencies;
    
    /**
     * Property Initiator
     *
     * The ConnectionInitiator used to connect to a ProxyService.
     * 
     * @volatile
     */
    private volatile com.tangosol.net.messaging.ConnectionInitiator __m_Initiator;
    
    /**
     * Property MemberListeners
     *
     * The collection of registered MemberListener objects.
     * 
     * @see #addMemberListener
     * @see #removeMemberListener
     */
    private com.tangosol.util.Listeners __m_MemberListeners;
    
    /**
     * Property NameServiceAddressProvider
     *
     * Whether the remote AddressProvider addresses are to be used to look up
     * the remote address of the ProxyService.
     */
    private boolean __m_NameServiceAddressProvider;
    
    /**
     * Property OperationalContext
     *
     * The OperationalContext for this Service.
     */
    private com.tangosol.net.OperationalContext __m_OperationalContext;
    
    /**
     * Property ResourceRegistry
     *
     * ResourceRegistry associated with this Service.
     */
    private com.tangosol.util.ResourceRegistry __m_ResourceRegistry;
    
    /**
     * Property ScopeName
     *
     * The scope name for this Service.  ScopeName will be pre-pended to the
     * ProxyServiceName (if defined) when doing a NameService.lookup().
     */
    private String __m_ScopeName;
    
    /**
     * Property ServiceListeners
     *
     * The collection of registered ServiceListener objects.
     * 
     * @see #addServiceListener
     * @see #removeServiceListener
     */
    private com.tangosol.util.Listeners __m_ServiceListeners;
    
    /**
     * Property ServiceName
     *
     * @see com.tangosol.net.ServiceInfo#getServiceName
     */
    private String __m_ServiceName;
    
    /**
     * Property ServiceVersion
     *
     * @see com.tangosol.net.ServiceInfo#getServiceVersion
     */
    private String __m_ServiceVersion;
    
    /**
     * Property UserContext
     *
     * @see com.tangosol.util.Service#getUserContext
     */
    private Object __m_UserContext;
    
    // Initializing constructor
    public RemoteService(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            clz = Class.forName("com.tangosol.coherence/component/net/extend/RemoteService".replace('/', '.'));
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
    
    // From interface: com.tangosol.net.Service
    public void addMemberListener(com.tangosol.net.MemberListener listener)
        {
        getMemberListeners().add(listener);
        }
    
    // From interface: com.tangosol.net.Service
    public void addServiceListener(com.tangosol.util.ServiceListener listener)
        {
        getServiceListeners().add(listener);
        }
    
    /**
     * Create a new Default dependencies object by cloning the input
    * dependencies.  Each class or component that uses dependencies implements
    * a Default dependencies class which provides the clone functionality.  
    * The dependency injection design pattern requires every component in the
    * component hierarchy to implement clone.
    * 
    * @return DefaultRemoteServiceDependencies  the cloned dependencies
     */
    protected com.tangosol.internal.net.service.extend.remote.DefaultRemoteServiceDependencies cloneDependencies(com.tangosol.internal.net.service.extend.remote.RemoteServiceDependencies deps)
        {
        // import com.tangosol.internal.net.service.extend.remote.DefaultRemoteServiceDependencies;
        
        return new DefaultRemoteServiceDependencies(deps);
        }
    
    // From interface: com.tangosol.net.Service
    public synchronized void configure(com.tangosol.run.xml.XmlElement xml)
        {
        _assert(!isRunning());
        
        doConfigure(xml);
        }
    
    // From interface: com.tangosol.net.messaging.ConnectionListener
    public void connectionClosed(com.tangosol.net.messaging.ConnectionEvent evt)
        {
        // import com.tangosol.net.MemberEvent;
        
        setChannel(null);
        
        dispatchMemberEvent(MemberEvent.MEMBER_LEAVING);
        dispatchMemberEvent(MemberEvent.MEMBER_LEFT);
        }
    
    // From interface: com.tangosol.net.messaging.ConnectionListener
    public void connectionError(com.tangosol.net.messaging.ConnectionEvent evt)
        {
        // import com.tangosol.net.MemberEvent;
        
        setChannel(null);
        
        dispatchMemberEvent(MemberEvent.MEMBER_LEAVING);
        dispatchMemberEvent(MemberEvent.MEMBER_LEFT);
        }
    
    // From interface: com.tangosol.net.messaging.ConnectionListener
    public void connectionOpened(com.tangosol.net.messaging.ConnectionEvent evt)
        {
        // import com.tangosol.net.MemberEvent;
        
        dispatchMemberEvent(MemberEvent.MEMBER_JOINED);
        }
    
    /**
     * Create and dispatch a new local MemberEvent with the given identifier to
    * the collection of register MemberListener.
    * 
    * @param nId  the type of MemberEvent to create and dispatch
     */
    protected void dispatchMemberEvent(int nId)
        {
        // import com.tangosol.net.MemberEvent;
        // import com.tangosol.util.Listeners;
        
        Listeners listeners = getMemberListeners();
        if (!listeners.isEmpty())
            {
            new MemberEvent(this, nId, getLocalMember()).dispatch(listeners);
            }
        }
    
    /**
     * Create and dispatch a new ServiceEvent with the given identifier to the
    * collection of register ServiceListener.
    * 
    * @param nId  the type of ServiceEvent to create and dispatch
     */
    protected void dispatchServiceEvent(int nId)
        {
        // import com.tangosol.util.Listeners;
        // import com.tangosol.util.ServiceEvent;
        
        Listeners listeners = getServiceListeners();
        if (!listeners.isEmpty())
            {
            new ServiceEvent(this, nId).dispatch(listeners);
            }
        }
    
    /**
     * The configure() implementation method. This method must only be called by
    * a thread that has synchronized on this RemoteService.
    * 
    * @param xml  the XmlElement containing the new configuration for this
    * RemoteService
     */
    protected void doConfigure(com.tangosol.run.xml.XmlElement xml)
        {
        // import com.tangosol.internal.net.service.extend.remote.DefaultRemoteServiceDependencies;
        // import com.tangosol.internal.net.service.extend.remote.LegacyXmlRemoteServiceHelper as com.tangosol.internal.net.service.extend.remote.LegacyXmlRemoteServiceHelper;
        
        setDependencies(com.tangosol.internal.net.service.extend.remote.LegacyXmlRemoteServiceHelper.fromXml(xml,
            new DefaultRemoteServiceDependencies(), getOperationalContext(),
            getContextClassLoader()));
        }
    
    /**
     * The shutdown() implementation method. This method must only be called by
    * a thread that has synchronized on this RemoteService.
     */
    protected void doShutdown()
        {
        getInitiator().shutdown();
        }
    
    /**
     * The start() implementation method. This method must only be called by a
    * thread that has synchronized on this RemoteService.
     */
    protected void doStart()
        {
        // import com.tangosol.net.messaging.ConnectionInitiator;
        
        ConnectionInitiator initiator = getInitiator();
        _assert(initiator != null);
        
        initiator.addConnectionListener(this);
        initiator.addServiceListener(this);
        initiator.setContextClassLoader(getContextClassLoader());
        initiator.start();
        
        ensureChannel();
        }
    
    /**
     * The stop() implementation method. This method must only be called by a
    * thread that has synchronized on this RemoteService.
     */
    protected void doStop()
        {
        getInitiator().stop();
        }
    
    /**
     * Return the Channel used by this Service. If the Channel is null or is not
    * open, a new Channel is opened.
    * 
    * @return a Channel that can be used to exchange Messages with the remote
    * ProxyService
     */
    protected synchronized com.tangosol.net.messaging.Channel ensureChannel()
        {
        // import com.tangosol.net.messaging.Channel as com.tangosol.net.messaging.Channel;
        
        com.tangosol.net.messaging.Channel channel = getChannel();
        if (channel == null || !channel.isOpen())
            {
            setChannel(channel = openChannel());
            }
        
        return channel;
        }
    
    /**
     * Return a running QueueProcessor used to dispatch events to registered
    * listeners.
    * 
    * @return a running QueueProcessor
     */
    protected com.tangosol.coherence.component.util.daemon.QueueProcessor ensureEventDispatcher()
        {
        // import Component.Net.Extend.Channel;
        
        Channel channel = (Channel) ensureChannel();
        return channel.getConnectionManager().ensureEventDispatcher();
        }
    
    // Accessor for the property "Channel"
    /**
     * Getter for property Channel.<p>
    * The Channel used to send and receive Messages to/from the remote
    * ProxyService.
    * 
    * @volatile
     */
    public com.tangosol.net.messaging.Channel getChannel()
        {
        return __m_Channel;
        }
    
    // From interface: com.tangosol.net.Service
    // Accessor for the property "Cluster"
    /**
     * Getter for property Cluster.<p>
    * @see com.tangosol.net.Service#getCluster
     */
    public com.tangosol.net.Cluster getCluster()
        {
        return __m_Cluster;
        }
    
    // From interface: com.tangosol.net.Service
    // Accessor for the property "ContextClassLoader"
    /**
     * Getter for property ContextClassLoader.<p>
    * @see com.tangosol.util.Controllable#getContextClassLoader
     */
    public ClassLoader getContextClassLoader()
        {
        return __m_ContextClassLoader;
        }
    
    // From interface: com.tangosol.net.Service
    // Accessor for the property "Dependencies"
    /**
     * Getter for property Dependencies.<p>
    * The external dependencies needed by this component. The dependencies
    * object must be full populated and validated before this property is set. 
    * See setDependencies.  
    * 
    * The mechanism for creating and populating dependencies is hidden from
    * this component. Typically, the dependencies object is populated using
    * data from some external configuration, such as XML, but this may not
    * always be the case.
     */
    public com.tangosol.net.ServiceDependencies getDependencies()
        {
        return __m_Dependencies;
        }
    
    // Declared at the super level
    /**
     * Return a human-readable description of this component.
    * 
    * @return a String representation of this component
     */
    protected String getDescription()
        {
        return "Name=" + getServiceName();
        }
    
    // From interface: com.tangosol.net.Service
    public com.tangosol.net.ServiceInfo getInfo()
        {
        return this;
        }
    
    // Accessor for the property "Initiator"
    /**
     * Getter for property Initiator.<p>
    * The ConnectionInitiator used to connect to a ProxyService.
    * 
    * @volatile
     */
    public com.tangosol.net.messaging.ConnectionInitiator getInitiator()
        {
        return __m_Initiator;
        }
    
    // Accessor for the property "LocalMember"
    /**
     * Getter for property LocalMember.<p>
    * The Member object that represents the JVM running this RemoteService.
     */
    protected com.tangosol.net.Member getLocalMember()
        {
        // import com.tangosol.net.OperationalContext;
        
        OperationalContext ctx = getOperationalContext();
        return ctx == null ? null : ctx.getLocalMember();
        }
    
    // Accessor for the property "MemberListeners"
    /**
     * Getter for property MemberListeners.<p>
    * The collection of registered MemberListener objects.
    * 
    * @see #addMemberListener
    * @see #removeMemberListener
     */
    protected com.tangosol.util.Listeners getMemberListeners()
        {
        return __m_MemberListeners;
        }
    
    // From interface: com.tangosol.net.ServiceInfo
    public com.tangosol.net.Member getOldestMember()
        {
        return getLocalMember();
        }
    
    // Accessor for the property "OperationalContext"
    /**
     * Getter for property OperationalContext.<p>
    * The OperationalContext for this Service.
     */
    public com.tangosol.net.OperationalContext getOperationalContext()
        {
        return __m_OperationalContext;
        }
    
    // Accessor for the property "RemoteClusterName"
    /**
     * Getter for property RemoteClusterName.<p>
    * The remote cluster name or null.
     */
    public String getRemoteClusterName()
        {
        // import com.tangosol.internal.net.service.extend.remote.RemoteServiceDependencies;
        
        String sName = ((RemoteServiceDependencies) getDependencies()).getRemoteClusterName();
        if (sName == null || sName.isEmpty())
            {
            // NS lookups and corresponding redirects are always done with a cluster name since multiple
            // clusters may effectivley share the cluster port we don't know what cluster we'd land in.
            // remote-address based lookups on the other hand use the cluster name configured in the remote
            // scheme, which is allowed to be null.  This is because a remote-address based lookup is pointing
            // at an explict unsharable port and it is presumed the configuration is correct.    
            return isNameServiceAddressProvider()
                ? getOperationalContext().getLocalMember().getClusterName()
                : null;
            }
        return sName;
        }
    
    // Accessor for the property "RemoteServiceName"
    /**
     * Getter for property RemoteServiceName.<p>
    * The remote service name or null.
     */
    public String getRemoteServiceName()
        {
        // import com.tangosol.internal.net.service.extend.remote.RemoteServiceDependencies;
        
        String sName = ((RemoteServiceDependencies) getDependencies()).getRemoteServiceName();
        if (sName == null || sName.isEmpty())
            {
            return isNameServiceAddressProvider()
                ? getServiceName() // already scoped
                : null;
            }
        String sScopeName = getScopeName();
        return (sScopeName == null || sScopeName.length() == 0)
                ? sName
                : sScopeName + ':' + sName;
        }
    
    // From interface: com.tangosol.net.Service
    // Accessor for the property "ResourceRegistry"
    /**
     * Getter for property ResourceRegistry.<p>
    * ResourceRegistry associated with this Service.
     */
    public com.tangosol.util.ResourceRegistry getResourceRegistry()
        {
        return __m_ResourceRegistry;
        }
    
    // Accessor for the property "ScopeName"
    /**
     * Getter for property ScopeName.<p>
    * The scope name for this Service.  ScopeName will be pre-pended to the
    * ProxyServiceName (if defined) when doing a NameService.lookup().
     */
    public String getScopeName()
        {
        return __m_ScopeName;
        }
    
    // From interface: com.tangosol.net.Service
    public com.tangosol.io.Serializer getSerializer()
        {
        // import com.tangosol.net.messaging.Connection;
        // import com.tangosol.net.messaging.ConnectionManager;
        // import Component.Util.Daemon.QueueProcessor.Service;
        
        Connection connection = ensureChannel().getConnection();
        if (connection != null)
            {
            ConnectionManager manager = connection.getConnectionManager();
            if (manager instanceof Service)
                {
                return ((Service) manager).getSerializer();
                }
            }
        return null;
        }
    
    // Accessor for the property "ServiceListeners"
    /**
     * Getter for property ServiceListeners.<p>
    * The collection of registered ServiceListener objects.
    * 
    * @see #addServiceListener
    * @see #removeServiceListener
     */
    protected com.tangosol.util.Listeners getServiceListeners()
        {
        return __m_ServiceListeners;
        }
    
    // From interface: com.tangosol.net.ServiceInfo
    public com.tangosol.net.Member getServiceMember(int nId)
        {
        // import com.tangosol.net.Member;
        
        Member member = getLocalMember();
        if (member != null && member.getId() == nId)
            {
            return member;
            }
        
        return null;
        }
    
    // From interface: com.tangosol.net.ServiceInfo
    public java.util.Set getServiceMembers()
        {
        // import com.tangosol.net.Member;
        // import java.util.Collections;
        
        Member member = getLocalMember();
        return member == null ? Collections.EMPTY_SET : Collections.singleton(member);
        }
    
    // From interface: com.tangosol.net.ServiceInfo
    // Accessor for the property "ServiceName"
    /**
     * Getter for property ServiceName.<p>
    * @see com.tangosol.net.ServiceInfo#getServiceName
     */
    public String getServiceName()
        {
        return __m_ServiceName;
        }
    
    // From interface: com.tangosol.net.ServiceInfo
    public String getServiceType()
        {
        return null;
        }
    
    // Accessor for the property "ServiceVersion"
    /**
     * Getter for property ServiceVersion.<p>
    * @see com.tangosol.net.ServiceInfo#getServiceVersion
     */
    public String getServiceVersion()
        {
        return __m_ServiceVersion;
        }
    
    // From interface: com.tangosol.net.ServiceInfo
    // Accessor for the property "ServiceVersion"
    /**
     * Getter for property ServiceVersion.<p>
    * @see com.tangosol.net.ServiceInfo#getServiceVersion
     */
    public String getServiceVersion(com.tangosol.net.Member member)
        {
        return getServiceVersion();
        }
    
    // From interface: com.tangosol.net.Service
    // Accessor for the property "UserContext"
    /**
     * Getter for property UserContext.<p>
    * @see com.tangosol.util.Service#getUserContext
     */
    public Object getUserContext()
        {
        return __m_UserContext;
        }
    
    // Accessor for the property "NameServiceAddressProvider"
    /**
     * Getter for property NameServiceAddressProvider.<p>
    * Whether the remote AddressProvider addresses are to be used to look up
    * the remote address of the ProxyService.
     */
    public boolean isNameServiceAddressProvider()
        {
        return __m_NameServiceAddressProvider;
        }
    
    // From interface: com.tangosol.net.Service
    public boolean isRunning()
        {
        // import com.tangosol.net.messaging.ConnectionInitiator;
        
        ConnectionInitiator initiator = getInitiator();
        return initiator == null ? false : initiator.isRunning();
        }
    
    /**
     * Return true if the current thread is one of the Service threads.
    * 
    * @param fStrict if true then only the service thread and event dispatcher
    * thread are considered to be service threads, if false, then DaemonPool
    * threads are also considered to be service threads.
     */
    public boolean isServiceThread(boolean fStrict)
        {
        // import com.tangosol.net.messaging.ConnectionInitiator;
        // import Component.Util.Daemon.QueueProcessor.Service.Peer.Initiator;
        
        ConnectionInitiator initiator = getInitiator();
        if (initiator instanceof Initiator)
            {
            return ((Initiator) initiator).isServiceThread(fStrict);
            }
        return false;
        }
    
    // From interface: com.tangosol.net.Service
    public boolean isSuspended()
        {
        return false;
        }
    
    /**
     * Obtains the connect address of the ProxyService from a remote NameService.
     */
    protected void lookupProxyServiceAddress()
        {
        // import Component.Net.Extend.RemoteService.RemoteNameService;
        // import Component.Util.Daemon.QueueProcessor.Service.Peer.Initiator.TcpInitiator;
        // import com.oracle.coherence.common.net.InetSocketAddress32;
        // import com.tangosol.coherence.config.builder.SocketProviderBuilder;
        // import com.tangosol.internal.net.service.extend.remote.DefaultRemoteNameServiceDependencies;
        // import com.tangosol.internal.net.service.extend.remote.DefaultRemoteServiceDependencies;
        // import com.tangosol.internal.net.service.extend.remote.RemoteServiceDependencies;
        // import com.tangosol.internal.net.service.extend.remote.LegacyXmlRemoteNameServiceHelper as com.tangosol.internal.net.service.extend.remote.LegacyXmlRemoteNameServiceHelper;
        // import com.tangosol.internal.net.service.peer.initiator.DefaultTcpInitiatorDependencies;
        // import com.tangosol.internal.net.service.peer.initiator.TcpInitiatorDependencies;
        // import com.tangosol.net.CacheFactory;
        // import com.tangosol.net.CompositeSocketAddressProvider;
        // import com.tangosol.net.NameService;
        // import com.tangosol.net.messaging.ConnectionException;
        // import com.tangosol.net.messaging.ConnectionInitiator;
        // import com.tangosol.net.SocketProviderFactory;
        
        if (isNameServiceAddressProvider())
            {
            ConnectionInitiator initiator = getInitiator();
            if (initiator instanceof TcpInitiator)
                {
                // attempt to lookup the ProxyService address from a NameService
        
                RemoteServiceDependencies deps         = (RemoteServiceDependencies) getDependencies();
                TcpInitiator              tcpInitiator = (TcpInitiator) initiator;
                RemoteNameService         serviceNS    = new RemoteNameService();
        
                serviceNS.setOperationalContext(getOperationalContext());
                serviceNS.setContextClassLoader(getContextClassLoader());
                serviceNS.setServiceName(getServiceName() + ':' + NameService.TYPE_REMOTE);
        
                DefaultRemoteNameServiceDependencies nameServiceDeps = 
                        com.tangosol.internal.net.service.extend.remote.LegacyXmlRemoteNameServiceHelper.fromXml(
                                CacheFactory.getServiceConfig(NameService.TYPE_REMOTE),
                                new DefaultRemoteNameServiceDependencies(),
                                getOperationalContext(),
                                getContextClassLoader());
        
                // clone and inject the RemoteAddressProvider from this service's dependencies
                // into the RemoteNameService
                DefaultTcpInitiatorDependencies depsNsTcp = new DefaultTcpInitiatorDependencies((TcpInitiatorDependencies) deps.getInitiatorDependencies());
        
                // use the default socket provder, as we don't want to inherit SSL settings, NS is always in the clear
                depsNsTcp.setSocketProviderBuilder(new SocketProviderBuilder(SocketProviderFactory.DEFAULT_SOCKET_PROVIDER, false));
                
                nameServiceDeps.setInitiatorDependencies(depsNsTcp);
        
                String sClusterRemote = getRemoteClusterName();
                String sServiceRemote = getRemoteServiceName();
        
                nameServiceDeps.setRemoteClusterName(sClusterRemote);
                nameServiceDeps.setRemoteServiceName("NameService");
                serviceNS.setDependencies(nameServiceDeps);
        
                RuntimeException e = null;
                try
                    {
                    tcpInitiator.getCloseOnExit().add(serviceNS);
                    serviceNS.start();
        
                    Object[] ao = (Object[]) serviceNS.lookup(sServiceRemote);
                    if (ao == null)
                        {
                        // we got an answer, which means we found the cluster, but not the service
                        e = new ConnectionException("Unable to locate ProxyService '" + sServiceRemote
                            + "' within cluster '" + sClusterRemote + "'");
                        }
                    else
                        {
                        tcpInitiator.setRemoteAddressProvider(new CompositeSocketAddressProvider(
                                new InetSocketAddress32((String) ao[0], ((Integer) ao[1]).intValue())));
                        }
                    }
                catch (Exception ex)
                    {
                    // we failed to connect, thus the cluster was not reachable
                    e = new ConnectionException("Unable to locate cluster '" + sClusterRemote + "' while looking for its ProxyService '"
                            + sServiceRemote + "'", ex);
                    }
                finally
                    {
                    tcpInitiator.getCloseOnExit().remove(serviceNS);
                    serviceNS.stop();
                    }
                if (e != null)
                    {
                    throw e;
                    }
                }
            }
        }
    
    /**
     * This event occurs when dependencies are injected into the component. 
    * First, call super.onDependencies to allow all super components to process
    * the Dependencies.  Each component is free to chose how it consumes
    * dependencies.  Typically, the  dependencies are copied into the
    * component's properties.  This technique isolates Dependency Injection
    * from the rest of the component code since components continue to access
    * properties just as they did before. 
    * 
    * However, for read-only dependency properties, the component can access
    * the dependencies directly as shown in the example below for
    * RemoteCacheService dependencies.  The advantage to this technique is that
    * the property only exists in the dependencies object, it is not duplicated
    * in the component properties.
    * 
    * RemoteCacheServiceDependencies deps = (RemoteCacheServiceDependencies)
    * getDependencies();
    * deps.getFoo();
     */
    protected void onDependencies(com.tangosol.internal.net.service.extend.remote.RemoteServiceDependencies deps)
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Peer.Initiator;
        // import com.tangosol.internal.net.service.peer.initiator.InitiatorDependencies;
        // import com.tangosol.internal.net.service.peer.initiator.TcpInitiatorDependencies;
        // import com.tangosol.net.messaging.ConnectionInitiator;
        // import com.tangosol.net.OperationalContext;
        
        // ensure that we've been configured with an OperationalContext
        OperationalContext ctx = getOperationalContext();
        if (ctx == null)
            {
            throw new IllegalStateException("missing required OperationalContext");
            }
        
        InitiatorDependencies initiatorDeps = deps.getInitiatorDependencies();
        if (initiatorDeps instanceof TcpInitiatorDependencies)
            {
            setNameServiceAddressProvider(((TcpInitiatorDependencies) initiatorDeps)
                    .isNameServiceAddressProvider());
            }
        
        // create and configure the initiator
        ConnectionInitiator initiator = Initiator.createInitiator(
            initiatorDeps, getOperationalContext());
            
        if (initiator instanceof Initiator)
            {
            Initiator initiatorImpl = (Initiator) initiator;
            initiatorImpl.setServiceName(getServiceName() + ':'
                    + initiatorImpl.getServiceName());
            initiatorImpl.setParentService(this);
            }
        setInitiator(initiator);
        }
    
    /**
     * Open a Channel to the remote ProxyService.
     */
    protected com.tangosol.net.messaging.Channel openChannel()
        {
        return null;
        }
    
    /**
     * Prepare a service for termination.
     */
    protected void prepareExit()
        {
        }
    
    // From interface: com.tangosol.net.Service
    public void removeMemberListener(com.tangosol.net.MemberListener listener)
        {
        getMemberListeners().remove(listener);
        }
    
    // From interface: com.tangosol.net.Service
    public void removeServiceListener(com.tangosol.util.ServiceListener listener)
        {
        getServiceListeners().remove(listener);
        }
    
    // From interface: com.tangosol.util.ServiceListener
    public void serviceStarted(com.tangosol.util.ServiceEvent evt)
        {
        dispatchServiceEvent(evt.getId());
        }
    
    // From interface: com.tangosol.util.ServiceListener
    public void serviceStarting(com.tangosol.util.ServiceEvent evt)
        {
        dispatchServiceEvent(evt.getId());
        }
    
    // From interface: com.tangosol.util.ServiceListener
    public void serviceStopped(com.tangosol.util.ServiceEvent evt)
        {
        dispatchServiceEvent(evt.getId());
        setChannel(null);
        }
    
    // From interface: com.tangosol.util.ServiceListener
    public void serviceStopping(com.tangosol.util.ServiceEvent evt)
        {
        dispatchServiceEvent(evt.getId());
        }
    
    // Accessor for the property "Channel"
    /**
     * Setter for property Channel.<p>
    * The Channel used to send and receive Messages to/from the remote
    * ProxyService.
    * 
    * @volatile
     */
    protected void setChannel(com.tangosol.net.messaging.Channel channel)
        {
        __m_Channel = channel;
        }
    
    // Accessor for the property "Cluster"
    /**
     * Setter for property Cluster.<p>
    * @see com.tangosol.net.Service#getCluster
     */
    public void setCluster(com.tangosol.net.Cluster cluster)
        {
        __m_Cluster = cluster;
        }
    
    // From interface: com.tangosol.net.Service
    // Accessor for the property "ContextClassLoader"
    /**
     * Setter for property ContextClassLoader.<p>
    * @see com.tangosol.util.Controllable#getContextClassLoader
     */
    public synchronized void setContextClassLoader(ClassLoader loader)
        {
        // import com.tangosol.net.messaging.ConnectionInitiator;
        
        __m_ContextClassLoader = (loader);
        
        ConnectionInitiator initiator = getInitiator();
        if (initiator != null)
            {
            initiator.setContextClassLoader(loader);
            }
        }
    
    // From interface: com.tangosol.net.Service
    // Accessor for the property "Dependencies"
    /**
     * Inject the Dependencies object into the component.  First clone the
    * dependencies, then validate the cloned copy.  Note that the validate
    * method may modify the cloned dependencies, so it is important to use the
    * cloned dependencies for all subsequent operations.  Once the dependencies
    * have been validated, call onDependencies so that each Componenet in the
    * class hierarchy can process the dependencies as needed.
     */
    public void setDependencies(com.tangosol.net.ServiceDependencies deps)
        {
        // import com.tangosol.internal.net.service.extend.remote.RemoteServiceDependencies;
        
        if (getDependencies() != null)
            {
            throw new IllegalStateException("Dependencies already set");
            }
        
        __m_Dependencies = (cloneDependencies((RemoteServiceDependencies) deps).validate());
        
        // use the cloned dependencies
        onDependencies((RemoteServiceDependencies) getDependencies());
        }
    
    // Accessor for the property "Initiator"
    /**
     * Setter for property Initiator.<p>
    * The ConnectionInitiator used to connect to a ProxyService.
    * 
    * @volatile
     */
    protected void setInitiator(com.tangosol.net.messaging.ConnectionInitiator initiator)
        {
        _assert(getInitiator() == null);
        
        __m_Initiator = (initiator);
        }
    
    // Accessor for the property "MemberListeners"
    /**
     * Setter for property MemberListeners.<p>
    * The collection of registered MemberListener objects.
    * 
    * @see #addMemberListener
    * @see #removeMemberListener
     */
    protected void setMemberListeners(com.tangosol.util.Listeners listeners)
        {
        __m_MemberListeners = listeners;
        }
    
    // Accessor for the property "NameServiceAddressProvider"
    /**
     * Setter for property NameServiceAddressProvider.<p>
    * Whether the remote AddressProvider addresses are to be used to look up
    * the remote address of the ProxyService.
     */
    protected void setNameServiceAddressProvider(boolean fNameService)
        {
        __m_NameServiceAddressProvider = fNameService;
        }
    
    // Accessor for the property "OperationalContext"
    /**
     * Setter for property OperationalContext.<p>
    * The OperationalContext for this Service.
     */
    public void setOperationalContext(com.tangosol.net.OperationalContext ctx)
        {
        _assert(getOperationalContext() == null);
        
        __m_OperationalContext = (ctx);
        }
    
    // Accessor for the property "ResourceRegistry"
    /**
     * Setter for property ResourceRegistry.<p>
    * ResourceRegistry associated with this Service.
     */
    protected void setResourceRegistry(com.tangosol.util.ResourceRegistry registry)
        {
        __m_ResourceRegistry = registry;
        }
    
    // Accessor for the property "ScopeName"
    /**
     * Setter for property ScopeName.<p>
    * The scope name for this Service.  ScopeName will be pre-pended to the
    * ProxyServiceName (if defined) when doing a NameService.lookup().
     */
    public void setScopeName(String sName)
        {
        __m_ScopeName = sName;
        }
    
    // Accessor for the property "ServiceListeners"
    /**
     * Setter for property ServiceListeners.<p>
    * The collection of registered ServiceListener objects.
    * 
    * @see #addServiceListener
    * @see #removeServiceListener
     */
    protected void setServiceListeners(com.tangosol.util.Listeners listeners)
        {
        __m_ServiceListeners = listeners;
        }
    
    // Accessor for the property "ServiceName"
    /**
     * Setter for property ServiceName.<p>
    * @see com.tangosol.net.ServiceInfo#getServiceName
     */
    public void setServiceName(String sName)
        {
        __m_ServiceName = sName;
        }
    
    // Accessor for the property "ServiceVersion"
    /**
     * Setter for property ServiceVersion.<p>
    * @see com.tangosol.net.ServiceInfo#getServiceVersion
     */
    protected void setServiceVersion(String sVersion)
        {
        __m_ServiceVersion = sVersion;
        }
    
    // From interface: com.tangosol.net.Service
    // Accessor for the property "UserContext"
    /**
     * Setter for property UserContext.<p>
    * @see com.tangosol.util.Service#getUserContext
     */
    public void setUserContext(Object oCtx)
        {
        __m_UserContext = oCtx;
        }
    
    // From interface: com.tangosol.net.Service
    public void shutdown()
        {
        doShutdown();
        }
    
    // From interface: com.tangosol.net.Service
    public synchronized void start()
        {
        if (!isRunning())
            {
            try
                {
                doStart();
                }
            catch (RuntimeException e)
                {
                doStop();
                throw e;
                }
            }
        }
    
    // From interface: com.tangosol.net.Service
    public void stop()
        {
        doStop();
        }

    @Override
    public boolean isVersionCompatible(int nMajor, int nMinor, int nMicro, int nPatchSet, int nPatch)
        {
        int nEncoded = ServiceMemberSet.encodeVersion(nMajor, nMinor, nMicro, nPatchSet, nPatch);
        return CacheFactory.VERSION_ENCODED >= nEncoded;
        }

    @Override
    public boolean isVersionCompatible(int nYear, int nMonth, int nPatch)
        {
        int nEncoded = ServiceMemberSet.encodeVersion(nYear, nMonth, nPatch);
        return CacheFactory.VERSION_ENCODED >= nEncoded;
        }

    @Override
    public boolean isVersionCompatible(int nVersion)
        {
        return CacheFactory.VERSION_ENCODED >= nVersion;
        }

    @Override
    public boolean isVersionCompatible(IntPredicate predicate)
        {
        return predicate.test(CacheFactory.VERSION_ENCODED);
        }

    @Override
    public int getMinimumServiceVersion()
        {
        return CacheFactory.VERSION_ENCODED;
        }
    }
