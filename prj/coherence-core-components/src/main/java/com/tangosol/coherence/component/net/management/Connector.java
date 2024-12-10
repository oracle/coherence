
/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.management.Connector

package com.tangosol.coherence.component.net.management;

import com.tangosol.coherence.component.application.console.Coherence;
import com.tangosol.coherence.component.net.Cluster;
import com.tangosol.coherence.component.net.management.Gateway;
import com.tangosol.coherence.component.net.management.Model;
import com.tangosol.coherence.component.net.management.listenerHolder.LocalHolder;
import com.tangosol.coherence.component.net.management.listenerHolder.RemoteHolder;
import com.tangosol.coherence.component.net.management.model.LocalModel;
import com.tangosol.coherence.component.net.management.model.RemoteModel;
import com.tangosol.coherence.component.net.management.model.localModel.WrapperModel;
import com.tangosol.coherence.component.net.management.model.localModel.wrapperModel.WrapperJmxModel;
import com.tangosol.coherence.component.net.management.notificationHandler.RemoteHandler;
import com.tangosol.coherence.component.net.memberSet.actualMemberSet.ServiceMemberSet;
import com.tangosol.coherence.component.util.SafeCluster;
import com.tangosol.coherence.component.util.SafeService;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid;
import com.oracle.coherence.common.base.Continuation;
import com.oracle.coherence.common.net.InetAddresses;
import com.tangosol.coherence.config.Config;
import com.tangosol.discovery.NSLookup;
import com.tangosol.internal.health.HealthHttpHandler;
import com.tangosol.internal.net.management.ConnectorDependencies;
import com.tangosol.internal.net.management.DefaultConnectorDependencies;
import com.tangosol.internal.net.management.HttpHelper;
import com.tangosol.internal.net.metrics.MetricsHttpHelper;
import com.tangosol.net.InetAddressHelper;
import com.tangosol.net.InvocationService;
import com.tangosol.net.Member;
import com.tangosol.net.management.MBeanConnector;
import com.tangosol.net.management.MBeanHelper;
import com.tangosol.net.management.Registry;
import com.tangosol.run.xml.XmlDocument;
import com.tangosol.run.xml.XmlHelper;
import com.tangosol.util.Base;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.Filter;
import com.tangosol.util.Resources;
import java.io.EOFException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.management.MBeanInfo;
import javax.management.Notification;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXServiceURL;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NamingException;

/**
 * Connector provides peer-to-peer communications for management related
 * requests.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class Connector
        extends    com.tangosol.coherence.component.net.Management
        implements com.tangosol.net.MemberListener,
                   com.tangosol.net.NameService.Resolvable,
                   com.tangosol.util.SynchronousListener
    {
    // ---- Fields declarations ----
    
    /**
     * Property Announced
     *
     * Indicates that the handshake protocol ($Announce) has completed.
     */
    private boolean __m_Announced;
    
    /**
     * Property ConnectorServer
     *
     * A JMXConnectorServer started by this Connector.
     */
    private javax.management.remote.JMXConnectorServer __m_ConnectorServer;
    
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
    private com.tangosol.internal.net.management.ConnectorDependencies __m_Dependencies;
    
    /**
     * Property HttpManagingDynamic
     *
     * Specifies that this node should be an "HttpManagingNode" at the time it
     * becomes the senior Management service node.
     * 
     * @since 12.2.1.4
     */
    private boolean __m_HttpManagingDynamic;
    
    /**
     * Property HttpManagingNode
     *
     * ﻿Specifies whether this node has an HTTP management service enabled.
     * 
     * @since 12.2.1.4
     */
    private boolean __m_HttpManagingNode;
    
    /**
     * Property JmxListenAddresses
     *
     * For a JMXServiceURL which is bound to the wildcard address this is the
     * expanded set of addresses which that wildcard address maps to.
     */
    private java.util.Collection __m_JmxListenAddresses;
    
    /**
     * Property JmxServiceUrl
     *
     * The string representation of JMXServiceURL of the MBeanConnector; null
     * if this node is not a dynamic management node.
     */
    private javax.management.remote.JMXServiceURL __m_JmxServiceUrl;
    
    /**
     * Property LocalGateway
     *
     * The local (MBeanServer bound) gateway.
     */
    private com.tangosol.coherence.component.net.management.gateway.Local __m_LocalGateway;
    
    /**
     * Property LocalRegistry
     *
     * A cache (Map<<String>, <LocalModel>>) of managed Model objects
     * registered by this node with any of the available MBeanServers.
     */
    private java.util.Map __m_LocalRegistry;
    
    /**
     * Property ManagingDynamic
     *
     * Specifies that this node should be a "ManagingNode" at the time it
     * becomes the senior Management service node.
     */
    private boolean __m_ManagingDynamic;
    
    /**
     * Property ManagingNode
     *
     * Specifies whether this Connector represents a managing node (has 
     * in-process MBeanServer).
     */
    private boolean __m_ManagingNode;
    
    /**
     * Property MsgFailure
     *
     * Licensing and security support.
     */
    private static transient String __s_MsgFailure;
    
    /**
     * Property RefreshAttributeTimeoutMillis
     *
     * The timeout interval for making remote refresh calls.
     */
    private long __m_RefreshAttributeTimeoutMillis;
    
    /**
     * Property RemoteModels
     *
     * A map of RemoteModel objects keyed by the canonical MBean name
     * registered with an MBeanServer colocated with this node.
     */
    private java.util.Map __m_RemoteModels;
    
    /**
     * Property RemoteServers
     *
     * A set of Member objects for the managed nodes (have local MBeanServers
     * and are willing to manage remote nodes).
     */
    private java.util.Set __m_RemoteServers;
    
    /**
     * Property RequestTimeout
     *
     * The request timeout for the underlying InvocationService
     */
    private long __m_RequestTimeout;
    
    /**
     * Property Service
     *
     * Invocation service used for remote management.
     */
    private com.tangosol.net.InvocationService __m_Service;
    
    /**
     * Property StatsNotificationCount
     *
     * The number of remote notifications received by the node since the last
     * time the statistics were reset.
     */
    private long __m_StatsNotificationCount;
    
    /**
     * Property StatsRefreshCount
     *
     * The total number of snapshots retrieved since the statistics were last
     * reset.
     */
    private long __m_StatsRefreshCount;
    
    /**
     * Property StatsRefreshExcessCount
     *
     * The number of times a snapshot was retrieved predictively but not used
     * before the expiry time.
     */
    private long __m_StatsRefreshExcessCount;
    
    /**
     * Property StatsRefreshPredictionCount
     *
     * The number of times snapshots were retrieved prior to a client request
     * based on the refresh policy.
     */
    private long __m_StatsRefreshPredictionCount;
    
    /**
     * Property StatsRefreshTimeoutCount
     *
     * The number of times that the remote refresh times out.
     */
    private long __m_StatsRefreshTimeoutCount;
    
    /**
     * Property ThreadLocalContinuation
     *
     * A Continuation set on a thread local to be used by a RemoteModel. 
     * 
     * As the RemoteModel is called via an MBeanServer the Connector lacks an
     * ability to pass directly to the RemoteModel thus uses the stack to share
     * the Continuation.
     * 
     * The Continuation allows the RemoteModel to perform a non-blocking call
     * and call the Continuation once the non-blockintg call completes.
     * 
     * @see  #onRemoteInvoke
     */
    private ThreadLocal __m_ThreadLocalContinuation;
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
        __mapChildren.put("Announce", Connector.Announce.get_CLASS());
        __mapChildren.put("ExecuteFunction", Connector.ExecuteFunction.get_CLASS());
        __mapChildren.put("InvokeRemote", Connector.InvokeRemote.get_CLASS());
        __mapChildren.put("LookupCallback", Connector.LookupCallback.get_CLASS());
        __mapChildren.put("Notify", Connector.Notify.get_CLASS());
        __mapChildren.put("Publish", Connector.Publish.get_CLASS());
        __mapChildren.put("Register", Connector.Register.get_CLASS());
        __mapChildren.put("Subscribe", Connector.Subscribe.get_CLASS());
        __mapChildren.put("Unregister", Connector.Unregister.get_CLASS());
        }
    
    // Default constructor
    public Connector()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public Connector(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            setLocalRegistry(new com.tangosol.util.SafeHashMap());
            setRefreshAttributeTimeoutMillis(250L);
            setRemoteModels(new java.util.concurrent.ConcurrentHashMap());
            setRemoteServers(new com.tangosol.util.SafeHashSet());
            setThreadLocalContinuation(new java.lang.ThreadLocal());
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
        return new com.tangosol.coherence.component.net.management.Connector();
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
            clz = Class.forName("com.tangosol.coherence/component/net/management/Connector".replace('/', '.'));
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
     * Initiate the handshake with other members of the Management service.
     */
    protected void announce(com.tangosol.net.InvocationService service)
        {
        // import com.tangosol.net.Member;
        // import java.util.Set;
        
        // do a handshake with all the management-aware nodes
        // NOTE: prior to Coherence 3.6, this part was done using the synchronous request
        // which had a potential of blocking the Cluster startup sequence;
        // doing this asynchronously requires deferral of some registration messages
        // until we know whether or not the recipient is a managing node
        
        Member memberThis = service.getCluster().getLocalMember();
        Set    setOther   = service.getInfo().getServiceMembers();
        setOther.remove(memberThis);
        
        Connector.Announce task = (Connector.Announce) _newChild("Announce");
        task.setMemberFrom(memberThis.getId());
        task.setManagingNode(isManagingNode());
        
        service.execute(task, setOther, task);
        }
    
    /**
     * Transition this node to become a management node, start an MBeanConnector
    * and register the JMX service URL with the NameService. This method is
    * called only if the management type is "dynamic". 
     */
    protected void assumeManagement()
        {
        // import Component.Net.Management.Gateway.Remote as com.tangosol.coherence.component.net.management.gateway.Remote;
        // import Component.Util.SafeCluster;
        // import com.tangosol.net.InetAddressHelper;
        // import java.net.InetAddress;
        // import java.util.Collections;
        // import javax.management.remote.JMXConnectorServer;
        // import javax.management.remote.JMXServiceURL;
        
        SafeCluster cluster = getCluster();
        
        com.tangosol.coherence.component.net.management.gateway.Remote remoteGateway = (com.tangosol.coherence.component.net.management.gateway.Remote) cluster.getManagement();
        remoteGateway.transitionToManaging();
        
        JMXServiceURL jmxUrl = getLocalGateway().getServiceUrl();
        
        if (jmxUrl == null)
            {
            // start the JMX connector
            JMXConnectorServer jmxServer = jmxStartConnector();
            if (jmxServer != null)
                {
                setConnectorServer(jmxServer);
        
                jmxUrl = jmxServer.getAddress();
                }
            }
        
        if (jmxUrl != null)
            {
            synchronized (this)
                {
                setJmxServiceUrl(jmxUrl);
                
                InetAddress addrDisc = cluster.getDependencies().getLocalDiscoveryAddress();
                setJmxListenAddresses(addrDisc.isAnyLocalAddress()
                        ? InetAddressHelper.getLocalBindableAddresses()
                        : Collections.singleton(addrDisc));
                }
        
            // publish it to all Management service members
            jmxPublishConnector(null);
            }
        }
    
    /**
     * Pick up a remote member that runs an MBean server.
     */
    protected com.tangosol.net.Member chooseRemoteServer(int nAction)
        {
        // import Component.Net.Member as com.tangosol.coherence.component.net.Member;
        // import java.util.Iterator;
        // import java.util.Set;
        
        // pick any of the available servers
        Set setServers = getRemoteServers();
        if (setServers.isEmpty())
            {
            throw new RuntimeException("None of the nodes are managed");
            }
        
        String sReason = null;
        
        for (Iterator iter = setServers.iterator(); iter.hasNext();)
            {
            com.tangosol.coherence.component.net.Member member = (com.tangosol.coherence.component.net.Member) iter.next();
            switch (nAction)
                {
                case Connector.InvokeRemote.ACTION_QUERY:
                    // MBeanServerProxy.queryNames() method is new in 12.2.1.1.0
                    if (isVersionCompatible(member, 12, 2, 1, 1, 0))
                        {
                        return member;
                        }
                    sReason = "queryNames(...) method";
                    break;
        
                case Connector.InvokeRemote.ACTION_FIND_OWNER:
                    // MBeanServerProxy.addNotificationListener() method is new in 12.2.1.4.0
                    if (isVersionCompatible(member, 12, 2, 1, 4, 0))
                        {
                        return member;
                        }
                    sReason = "addNotificationListener(...) method";
                    break;
        
                default:
                    return member;
                }
            }
        
        throw new UnsupportedOperationException(
            "All managed nodes running an older version that does not support " + sReason);
        }
    
    /**
     * Create a new Default dependencies object by cloning the input
    * dependencies.  Each class or component that uses dependencies implements
    * a Default dependencies class which provides the clone functionality.  
    * The dependency injection design pattern requires every component in the
    * component hierarchy to implement clone.
    * 
    * @return DefaultConnectorDependencies  the cloned dependencies
     */
    public com.tangosol.internal.net.management.DefaultConnectorDependencies cloneDependencies(com.tangosol.internal.net.management.ConnectorDependencies deps)
        {
        // import com.tangosol.internal.net.management.DefaultConnectorDependencies;
        
        return new DefaultConnectorDependencies(deps);
        }
    
    /**
     * Create a remote MBeanServer invocation task for getAttribute.
     */
    public Connector.InvokeRemote createExecuteRequest(com.tangosol.util.function.Remote.Function function)
        {
        Connector.InvokeRemote task = (Connector.InvokeRemote) _newChild("InvokeRemote");
        
        task.setAction(Connector.InvokeRemote.ACTION_EXECUTE);
        task.setFunction(function);
        
        return task;
        }
    
    /**
     * Create a remote MBeanServer invocation task to find the owner of a model.
     */
    public Connector.InvokeRemote createFindOwnerRequest(String sName)
        {
        Connector.InvokeRemote task = (Connector.InvokeRemote) _newChild("InvokeRemote");
        
        task.setAction(Connector.InvokeRemote.ACTION_FIND_OWNER);
        task.setName(sName);
        
        return task;
        }
    
    /**
     * Create a remote MBeanServer invocation task for getAttribute.
     */
    public Connector.InvokeRemote createGetMBeanInfoRequest(String sName)
        {
        Connector.InvokeRemote task = (Connector.InvokeRemote) _newChild("InvokeRemote");
        
        task.setAction(Connector.InvokeRemote.ACTION_MBEAN_INFO);
        task.setName(sName);
        
        return task;
        }
    
    /**
     * Create a remote MBeanServer invocation task for getAttribute.
     */
    public Connector.InvokeRemote createGetRequest(String sName, com.tangosol.util.Filter filter)
        {
        Connector.InvokeRemote task = (Connector.InvokeRemote) _newChild("InvokeRemote");
        
        task.setAction(Connector.InvokeRemote.ACTION_GET);
        task.setName(sName);
        task.setQueryFilter(filter);
        
        return task;
        }
    
    /**
     * Create a remote MBeanServer invocation task for getAttribute.
     */
    public Connector.InvokeRemote createGetRequest(String sName, String sAttr)
        {
        Connector.InvokeRemote task = (Connector.InvokeRemote) _newChild("InvokeRemote");
        
        task.setAction(Connector.InvokeRemote.ACTION_GET);
        task.setName(sName);
        task.setAttributeName(sAttr);
        
        return task;
        }
    
    /**
     * Create a remote MBeanServer invocation task for invoke.
     */
    public Connector.InvokeRemote createInvokeRequest(String sName, String sMethod, Object[] aoParam, String[] asSignature)
        {
        Connector.InvokeRemote task = (Connector.InvokeRemote) _newChild("InvokeRemote");
        
        task.setAction(Connector.InvokeRemote.ACTION_INVOKE);
        task.setName(sName);
        task.setMethodName(sMethod);
        task.setParameters(aoParam);
        task.setSignatures(asSignature);
        
        return task;
        }
    
    /**
     * Create a remote MBeanServer invocation task for isRegistered.
     */
    public Connector.InvokeRemote createIsRegisteredRequest(String sName)
        {
        Connector.InvokeRemote task = (Connector.InvokeRemote) _newChild("InvokeRemote");
        
        task.setAction(Connector.InvokeRemote.ACTION_CHECK);
        task.setName(sName);
        
        return task;
        }
    
    /**
     * Create a remote MBeanServer invocation task for queryNames.
     */
    public Connector.InvokeRemote createQueryRequest(String sPattern, com.tangosol.util.Filter filter)
        {
        Connector.InvokeRemote task = (Connector.InvokeRemote) _newChild("InvokeRemote");
        
        task.setAction(Connector.InvokeRemote.ACTION_QUERY);
        task.setQueryPattern(sPattern);
        task.setQueryFilter(filter);
        
        return task;
        }
    
    /**
     * Create a remote MBeanServer invocation task for set.
     */
    public Connector.InvokeRemote createSetRequest(String sName, String sAttr, Object oValue)
        {
        Connector.InvokeRemote task = (Connector.InvokeRemote) _newChild("InvokeRemote");
        
        task.setAction(Connector.InvokeRemote.ACTION_SET);
        task.setName(sName);
        task.setAttributeName(sAttr);
        task.setAttributeValue(oValue);
        
        return task;
        }
    
    /**
     * ensureFreshSnapShot determines the refresh method and then either
    * refreshes the data "As Needed",  "pre-refresh" or "post-refresh".
     */
    public com.tangosol.coherence.component.net.management.model.LocalModel ensureFreshSnapshot(com.tangosol.coherence.component.net.management.model.RemoteModel model)
        {
        // import com.tangosol.internal.net.management.ConnectorDependencies;
        
        int nPolicy = getRefreshPolicy();
        if (nPolicy == ConnectorDependencies.REFRESH_ONQUERY)
            {
            // using mutex just to ensure remote refresh has completed
            model.acquireExecuteMutex(getRefreshRequestTimeoutMillis());
            model.releaseExecuteMutex();
            }
        else if (model.isRefreshRequired())
            {
            setStatsRefreshCount(getStatsRefreshCount() + 1L);
            if (!model.isAccessed())
                {
                setStatsRefreshExcessCount(getStatsRefreshExcessCount() + 1L);
                } 
        
            switch (nPolicy)
                {
                case ConnectorDependencies.REFRESH_EXPIRED:
                     model.invokeRemote(model.OP_GET, null, null);
                     break;
         
                case ConnectorDependencies.REFRESH_AHEAD:
                     model.invokeRemote(model.OP_GET, null, null);
                     refreshActiveModels(model);
                     break;
        
                case ConnectorDependencies.REFRESH_BEHIND:
                     {
                     boolean fInvoked = model.invokeRemoteAsync();
                     if (fInvoked)
                        {
                        setStatsRefreshPredictionCount(getStatsRefreshPredictionCount() + 1L);
                        }
                     break;
                     }
                }
            }
        
        return model.getSnapshot();
        }
    
    /**
     * Obtain the RemoteModel for a given MBean name and owning Member Id,
    * creating the RemoteModel instance if required.
     */
    public com.tangosol.coherence.component.net.management.model.RemoteModel ensureRemoteModel(String sName, int nMember)
        {
        // import Component.Net.Management.Model.RemoteModel;
        // import com.tangosol.net.Member;
        // import java.util.Map;
        
        Map         mapModel    = getRemoteModels();
        RemoteModel modelRemote = (RemoteModel) mapModel.get(sName);
        
        if (modelRemote == null)
            {
            Member member = nMember > 0 ? getMember(nMember) : findModelOwner(sName);
        
            if (member == null)
                {
                throw new IllegalArgumentException("Unable to locate owning Member for MBean " + sName);
                }
            
            mapModel.put(sName, modelRemote = new RemoteModel());
            modelRemote.set_ModelName(sName);
            modelRemote.setConnector(this);
            modelRemote.setAccessed(false);
            modelRemote.setModelOwner(member);
            }
        
        return modelRemote;
        }
    
    /**
     * Obtain the owning Member Id for a given MBean name. If the MBean name
    * contains a nodeId key then the value of the nodeId will be returned
    * otherwise the MBeanServer will be queried to obtain the owning Member Id.
     */
    public void extractMemberId(String sName)
        {
        }
    
    /**
     * Return the owning Member for the specified MBean name or null if no
    * owning Member is found.
     */
    public com.tangosol.net.Member findModelOwner(String sName)
        {
        if (isRegisteredModel(sName))
            {
            return getMember(getLocalMemberId());
            }
        
        Connector.InvokeRemote task     = createFindOwnerRequest(sName);
        Integer       NOwnerId = (Integer) sendProxyRequest(task);
        
        return NOwnerId == null ? null : getMember(NOwnerId.intValue());
        }
    
    /**
     * Convert the specified RefreshPolicy value to a human-readable string.
     */
    public String formatRefreshPolicy(int nPolicy)
        {
        // import com.tangosol.internal.net.management.ConnectorDependencies;
        
        switch (nPolicy)
            {
            case ConnectorDependencies.REFRESH_EXPIRED:
                 return "refresh-expired";
        
            case ConnectorDependencies.REFRESH_AHEAD: 
                 return "refresh-ahead";
                 
            case ConnectorDependencies.REFRESH_BEHIND: 
                 return "refresh-behind";
        
            case ConnectorDependencies.REFRESH_ONQUERY: 
                 return "refresh-onquery";
            }
        return "n/a";
        }
    
    // Accessor for the property "Cluster"
    /**
     * Getter for property Cluster.<p>
     */
    public com.tangosol.coherence.component.util.SafeCluster getCluster()
        {
        // import Component.Application.Console.Coherence;
        // import Component.Util.SafeCluster;
        // import com.tangosol.net.Service as com.tangosol.net.Service;
        
        com.tangosol.net.Service service = getService();
        return service == null ? Coherence.getCluster() : (SafeCluster) service.getCluster();
        }
    
    // Accessor for the property "ConnectorServer"
    /**
     * Getter for property ConnectorServer.<p>
    * A JMXConnectorServer started by this Connector.
     */
    public javax.management.remote.JMXConnectorServer getConnectorServer()
        {
        return __m_ConnectorServer;
        }
    
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
    public com.tangosol.internal.net.management.ConnectorDependencies getDependencies()
        {
        return __m_Dependencies;
        }
    
    /**
     * Obtain the oldest member among the nodes with "dynamic" management type.
     */
    public com.tangosol.net.Member getDynamicSenior()
        {
        // import Component.Net.MemberSet.ActualMemberSet.ServiceMemberSet;
        // import Component.Util.Daemon.QueueProcessor.Service.Grid;
        // import Component.Util.SafeService;
        // import com.tangosol.net.Member;
        // import java.util.Iterator;
        
        Member           memberOldest = null;
        long             ldtOldest    = Long.MAX_VALUE;
        SafeService      service      = (SafeService) getService();
        Grid            _service      = (Grid) service.getService();
        Member           memberThis   = _service.getThisMember();
        ServiceMemberSet setMember    = _service.getServiceMemberSet();
        
        for (Iterator iter = setMember.iterator(); iter.hasNext();)
            {
            Member member = (Member) iter.next();
        
            int     nMember = member.getId();
            boolean fDynamic;
        
            if (member == memberThis)
                {
                // it's possible we didn't prime our MemberConfigMap yet
                fDynamic = isManagingDynamic();
                }
            else
                {
                fDynamic = Boolean.TRUE.equals(
                    (Boolean) setMember.getMemberConfigMap(nMember).get("dynamic-management"));
                }
        
            if (fDynamic)
                {
                long ldt = setMember.getServiceJoinTime(nMember);
                if (ldt < ldtOldest)
                    {
                    memberOldest = member;
                    ldtOldest    = ldt;
                    }
                }
            }
        
        return memberOldest;
        }
    
    // Accessor for the property "InvokeContinuation"
    /**
     * Getter for property InvokeContinuation.<p>
    * The Continuation to be used by the RemoteModel when an invocation agianst
    * a RemoteModel is to be performed on the service thread.
    * 
    * @see  #onRemoteInvoke
     */
    public com.oracle.coherence.common.base.Continuation getInvokeContinuation()
        {
        // import com.oracle.coherence.common.base.Continuation;
        
        return (Continuation) getThreadLocalContinuation().get();
        }
    
    // Accessor for the property "JmxListenAddresses"
    /**
     * Getter for property JmxListenAddresses.<p>
    * For a JMXServiceURL which is bound to the wildcard address this is the
    * expanded set of addresses which that wildcard address maps to.
     */
    public java.util.Collection getJmxListenAddresses()
        {
        return __m_JmxListenAddresses;
        }
    
    // Accessor for the property "JmxServiceUrl"
    /**
     * Getter for property JmxServiceUrl.<p>
    * The string representation of JMXServiceURL of the MBeanConnector; null if
    * this node is not a dynamic management node.
     */
    public javax.management.remote.JMXServiceURL getJmxServiceUrl()
        {
        return __m_JmxServiceUrl;
        }
    
    // Accessor for the property "LocalGateway"
    /**
     * Getter for property LocalGateway.<p>
    * The local (MBeanServer bound) gateway.
     */
    public com.tangosol.coherence.component.net.management.gateway.Local getLocalGateway()
        {
        return __m_LocalGateway;
        }
    
    // Accessor for the property "LocalMemberId"
    /**
     * Getter for property LocalMemberId.<p>
    * The local cluster member id.
     */
    public int getLocalMemberId()
        {
        // import Component.Util.SafeCluster;
        
        SafeCluster cluster = getCluster();
        return cluster == null ? 0 : cluster.getLocalMember().getId();
        }
    
    // Accessor for the property "LocalRegistry"
    /**
     * Getter for property LocalRegistry.<p>
    * A cache (Map<<String>, <LocalModel>>) of managed Model objects registered
    * by this node with any of the available MBeanServers.
     */
    public java.util.Map getLocalRegistry()
        {
        return __m_LocalRegistry;
        }
    
    /**
     * Return the Member object from a given Node Id. Can be called on the
    * service thread.
     */
    public com.tangosol.coherence.component.net.Member getMember(int nId)
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid;
        // import Component.Util.SafeService;
        
        SafeService serviceSafe = (SafeService) getService();
        if (serviceSafe != null)
            {
            com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid service = (com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid) serviceSafe.getService();
            if (service != null && service.isRunning())
                {
                return service.getServiceMemberSet().getMember(nId);
                }
            }
        return null;
        }
    
    // Accessor for the property "MsgFailure"
    /**
     * Getter for property MsgFailure.<p>
    * Licensing and security support.
     */
    private static String getMsgFailure()
        {
        return __s_MsgFailure;
        }
    
    protected com.tangosol.net.NameService getNameService()
        {
        return getCluster().getCluster().getNameService();
        }
    
    // Accessor for the property "RefreshAttributeTimeoutMillis"
    /**
     * Getter for property RefreshAttributeTimeoutMillis.<p>
    * The timeout interval for making remote refresh calls.
     */
    public long getRefreshAttributeTimeoutMillis()
        {
        return __m_RefreshAttributeTimeoutMillis;
        }
    
    public int getRefreshPolicy()
        {
        return getDependencies().getRefreshPolicy();
        }
    
    public long getRefreshRequestTimeoutMillis()
        {
        return getDependencies().getRefreshRequestTimeoutMillis();
        }
    
    public long getRefreshTimeoutMillis()
        {
        return getDependencies().getRefreshTimeoutMillis();
        }
    
    // Accessor for the property "RemoteModels"
    /**
     * Getter for property RemoteModels.<p>
    * A map of RemoteModel objects keyed by the canonical MBean name registered
    * with an MBeanServer colocated with this node.
     */
    public java.util.Map getRemoteModels()
        {
        return __m_RemoteModels;
        }
    
    // Accessor for the property "RemoteServers"
    /**
     * Getter for property RemoteServers.<p>
    * A set of Member objects for the managed nodes (have local MBeanServers
    * and are willing to manage remote nodes).
     */
    public java.util.Set getRemoteServers()
        {
        return __m_RemoteServers;
        }
    
    // Accessor for the property "RequestTimeout"
    /**
     * Getter for property RequestTimeout.<p>
    * The request timeout for the underlying InvocationService
     */
    public long getRequestTimeout()
        {
        return __m_RequestTimeout;
        }
    
    // Accessor for the property "Service"
    /**
     * Getter for property Service.<p>
    * Invocation service used for remote management.
     */
    public com.tangosol.net.InvocationService getService()
        {
        return __m_Service;
        }
    
    // Accessor for the property "StatsNotificationCount"
    /**
     * Getter for property StatsNotificationCount.<p>
    * The number of remote notifications received by the node since the last
    * time the statistics were reset.
     */
    public long getStatsNotificationCount()
        {
        return __m_StatsNotificationCount;
        }
    
    // Accessor for the property "StatsRefreshCount"
    /**
     * Getter for property StatsRefreshCount.<p>
    * The total number of snapshots retrieved since the statistics were last
    * reset.
     */
    public long getStatsRefreshCount()
        {
        return __m_StatsRefreshCount;
        }
    
    // Accessor for the property "StatsRefreshExcessCount"
    /**
     * Getter for property StatsRefreshExcessCount.<p>
    * The number of times a snapshot was retrieved predictively but not used
    * before the expiry time.
     */
    public long getStatsRefreshExcessCount()
        {
        return __m_StatsRefreshExcessCount;
        }
    
    // Accessor for the property "StatsRefreshPredictionCount"
    /**
     * Getter for property StatsRefreshPredictionCount.<p>
    * The number of times snapshots were retrieved prior to a client request
    * based on the refresh policy.
     */
    public long getStatsRefreshPredictionCount()
        {
        return __m_StatsRefreshPredictionCount;
        }
    
    // Accessor for the property "StatsRefreshTimeoutCount"
    /**
     * Getter for property StatsRefreshTimeoutCount.<p>
    * The number of times that the remote refresh times out.
     */
    public long getStatsRefreshTimeoutCount()
        {
        return __m_StatsRefreshTimeoutCount;
        }
    
    // Accessor for the property "ThreadLocalContinuation"
    /**
     * Getter for property ThreadLocalContinuation.<p>
    * A Continuation set on a thread local to be used by a RemoteModel. 
    * 
    * As the RemoteModel is called via an MBeanServer the Connector lacks an
    * ability to pass directly to the RemoteModel thus uses the stack to share
    * the Continuation.
    * 
    * The Continuation allows the RemoteModel to perform a non-blocking call
    * and call the Continuation once the non-blockintg call completes.
    * 
    * @see  #onRemoteInvoke
     */
    public ThreadLocal getThreadLocalContinuation()
        {
        return __m_ThreadLocalContinuation;
        }
    
    // Accessor for the property "Announced"
    /**
     * Getter for property Announced.<p>
    * Indicates that the handshake protocol ($Announce) has completed.
     */
    public boolean isAnnounced()
        {
        return __m_Announced;
        }
    
    // Accessor for the property "HttpManagingDynamic"
    /**
     * Getter for property HttpManagingDynamic.<p>
    * Specifies that this node should be an "HttpManagingNode" at the time it
    * becomes the senior Management service node.
    * 
    * @since 12.2.1.4
     */
    public boolean isHttpManagingDynamic()
        {
        return __m_HttpManagingDynamic;
        }
    
    // Accessor for the property "HttpManagingNode"
    /**
     * Getter for property HttpManagingNode.<p>
    * ﻿Specifies whether this node has an HTTP management service enabled.
    * 
    * @since 12.2.1.4
     */
    public boolean isHttpManagingNode()
        {
        return __m_HttpManagingNode;
        }
    
    // Accessor for the property "ManagingDynamic"
    /**
     * Getter for property ManagingDynamic.<p>
    * Specifies that this node should be a "ManagingNode" at the time it
    * becomes the senior Management service node.
     */
    public boolean isManagingDynamic()
        {
        return __m_ManagingDynamic;
        }
    
    // Accessor for the property "ManagingNode"
    /**
     * Getter for property ManagingNode.<p>
    * Specifies whether this Connector represents a managing node (has 
    * in-process MBeanServer).
     */
    public boolean isManagingNode()
        {
        return __m_ManagingNode;
        }
    
    /**
     * API for the Remote Gateway.
     */
    public boolean isRegisteredModel(String sName)
        {
        // import com.tangosol.net.management.MBeanHelper;
        
        return getLocalRegistry().containsKey(sName);
        }
    
    // Accessor for the property "Started"
    /**
     * Getter for property Started.<p>
    * Specifies whether or not the Invocation service has been started.
     */
    public boolean isStarted()
        {
        return getService() != null;
        }
    
    protected boolean isVersionCompatible(com.tangosol.coherence.component.net.Member member, int nMajor, int nMinor, int nMicro, int nPatchSet, int nPatch)
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid;
        // import Component.Util.SafeService;
        
        SafeService serviceSafe = (SafeService) getService();
        if (serviceSafe != null)
            {
            com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid service = (com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid) serviceSafe.getService();
            if (service != null && service.isRunning())
                {
                return service.isVersionCompatible(member,
                        nMajor, nMinor, nMicro, nPatchSet, nPatch);
                }
            }
        
        return false;
        }
    
    /**
     * Publish JMXServiceURL of  the connector to all members.
     */
    protected void jmxPublishConnector(java.util.Set setMembers)
        {
        // import com.tangosol.net.InvocationService as com.tangosol.net.InvocationService;
        
        com.tangosol.net.InvocationService service = getService();
        
        Connector.Publish task = (Connector.Publish) _newChild("Publish");
        synchronized (this)
            {
            task.setJMXServiceURL(getJmxServiceUrl());
            task.setListenAddresses(getJmxListenAddresses());
            }
        
        service.execute(task, setMembers, null);
        }
    
    /**
     * Start an MBeanConnector on availabe ephemeral ports, and return the
    * started connector,  or null if failed to start.
     */
    protected javax.management.remote.JMXConnectorServer jmxStartConnector()
        {
        // import Component.Net.Cluster;
        // import com.tangosol.coherence.config.Config;
        // import com.tangosol.net.management.MBeanConnector;
        // import com.tangosol.net.management.MBeanHelper;
        // import com.oracle.coherence.common.net.InetAddresses;
        // import java.net.InetAddress;
        // import java.util.HashMap;
        // import javax.management.remote.JMXConnectorServer;
        
        Cluster     cluster    = getCluster().getCluster();
        InetAddress addrDisc   = cluster.getDependencies().getLocalDiscoveryAddress();
        InetAddress addrMember = cluster.getLocalMember().getAddress();
        InetAddress addrRMI    = addrDisc.isAnyLocalAddress()
            ? InetAddresses.isLocalAddress(addrMember)
                ? addrDisc   // non-NAT wildcard (the default) is allowable
                : addrMember // must be NAT, RMI doesn't allow the connection unless we give it the NAT address
            : addrDisc; // non-default value; use it    
        
        String  sAddr    = Config.getProperty(MBeanConnector.RMI_HOST_PROPERTY, addrRMI.getHostAddress());
        HashMap mapEnv   = new HashMap();
        int     nPortMax = MBeanConnector.getConnectionPortMax();
        int     nPort;
        
        if (cluster.getSocketManager().getUnicastUdpSocketProvider().isSecure())
            {
            // TCMP is configured for SSL, ensure that JMX defaults to SSL as well
            mapEnv.put("com.oracle.coherence.tcmp.ssl", "true");
            nPort = MBeanConnector.getConnectionPort();
            }
        else
            {
            // when not using SSL we can actually multiplex the JMX RMI connection onto our member port
            // unless user explicity requested something else
            String sPort = Config.getProperty(MBeanConnector.RMI_CONNECTION_PORT_PROPERTY);
            nPort = sPort == null
                ? cluster.getLocalMember().getPort()
                : MBeanConnector.getConnectionPort();
            }
        
        Exception eLast = null;
        do
            {
            try
                {
                // Start Jmx Connector with dynamic service url
                JMXConnectorServer connector = MBeanHelper.startRmiConnector(sAddr, 0, nPort, getLocalGateway().getServer(), mapEnv);
                _trace("JMXConnectorServer now listening for connections on " +
                    (InetAddresses.isAnyLocalAddress(sAddr) ? InetAddress.getLocalHost().getHostName() : sAddr) +
                    ":" + connector.getAddress().getPort(), 3);
                return connector;
                }
            catch (Exception e)
                {
                eLast = e;
                }
            }
        while (nPort != 0 && ++nPort <= nPortMax);
        
        _trace("Could not start JMXConnectorServer on " + sAddr + ":" + nPort + "\n" + eLast.getMessage(), 1);
        
        return null;
        }
    
    // From interface: com.tangosol.net.MemberListener
    public void memberJoined(com.tangosol.net.MemberEvent event)
        {
        // import com.tangosol.net.InvocationService as com.tangosol.net.InvocationService;
        // import java.util.Collections;
        
        com.tangosol.net.InvocationService service = getService();
        if (event.isLocal())
            {
            // as we are a SynchronousListener we will never hear ourselves join; instead
            // we explicitly call initialization routines (announce) in startService
            }
        else if (service != null && getDynamicSenior() == service.getCluster().getLocalMember())
            {
            // this is management node, publish JMXServiceURL
            jmxPublishConnector(Collections.singleton(event.getMember()));
            }
        }
    
    // From interface: com.tangosol.net.MemberListener
    public void memberLeaving(com.tangosol.net.MemberEvent event)
        {
        }
    
    // From interface: com.tangosol.net.MemberListener
    /**
     * Removes the registry information when a node leaves the grid.
     */
    public void memberLeft(com.tangosol.net.MemberEvent event)
        {
        // import Component.Net.Management.Gateway.Local as com.tangosol.coherence.component.net.management.gateway.Local;
        // import Component.Net.Management.Model;
        // import Component.Net.Management.Model.RemoteModel;
        // import Component.Util.SafeCluster;
        // import com.tangosol.net.InvocationService as com.tangosol.net.InvocationService;
        // import com.tangosol.net.Member;
        // import com.tangosol.net.management.Registry;
        // import java.util.HashSet;
        // import java.util.Iterator;
        // import java.util.Map;
        // import java.util.Map$Entry as java.util.Map.Entry;
        // import java.util.Set;
        // import javax.management.remote.JMXConnectorServer;
        
        SafeCluster  cluster      = getCluster();
        com.tangosol.coherence.component.net.management.gateway.Local localGateway = getLocalGateway();
        
        Member member  = event.getMember();
        String sNodeId = Registry.KEY_NODE_ID + member.getId();
        
        if (event.isLocal())
            {
            // the Management service was stopped; in case of the cluster node
            // termination the gateway will be reset (by the SafeCluster);
            if (!cluster.isRunning())
                {
                if (localGateway != null)
                    {
                    localGateway.reset();
                    }
        
                removeLocalSubscriptions();
                }
        
            // note that we don't clear the local registry as it's only done by reset()
            getRemoteServers().clear();
            getRemoteModels().clear();
        
            JMXConnectorServer jmxServer = getConnectorServer();
            if (jmxServer != null)
                {
                try
                    {
                    jmxServer.stop();
                    }
                catch (Throwable ignored) {}
                }
        
            // force a new handshake upon the service restart
            setService(null);
            }
        else
            {
            getRemoteServers().remove(member);
        
            removeRemoteSubscriptions(member);
        
            // make sure we remove all the MBeans we know about including ones registered by
            // us outside our domain (COH-5974)
        
            Map mapRemoteModels = getRemoteModels();
            Set setRemoved      = new HashSet();
        
            for (Iterator iter = mapRemoteModels.entrySet().iterator(); iter.hasNext(); )
                {
                java.util.Map.Entry       entry = (java.util.Map.Entry) iter.next();
                RemoteModel model = (RemoteModel) entry.getValue();
        
                if (member.equals(model.getModelOwner()))
                    {
                    setRemoved.add(entry);
        
                    iter.remove();
                    }
                }
        
            if (localGateway != null)
                {
                for (Iterator iter = setRemoved.iterator(); iter.hasNext();)
                    {
                    java.util.Map.Entry entry = (java.util.Map.Entry) iter.next();
                    localGateway.unregisterModelMBean((String) entry.getKey(), (Model) entry.getValue());
                    }
        
                // this is likely unnecessary due to the logic above from COH-5974 but we're
                // leaving it in to be safe
                localGateway.unregisterModelMBean(sNodeId + ",*", null);
                }
        
            com.tangosol.net.InvocationService service = getService();
            if (!isManagingNode() && service != null &&
                    getDynamicSenior() == service.getCluster().getLocalMember())
                {
                assumeManagement();
                announce(service);
                }
            }
        }
    
    /**
     * Method called when a MBean Server is added to the cluster to announce its
    * presence to the rest of the cluster.
     */
    public void onAnnouncement(Connector.Announce taskAnnounce)
        {
        // import Component.Net.Member;
        // import java.util.Collections;
        
        if (taskAnnounce.isManagingNode())
            {
            Member memberFrom = getMember(taskAnnounce.getMemberFrom());
            if (memberFrom != null)
                {
                getRemoteServers().add(memberFrom);
                registerAll(Collections.singleton(memberFrom));
                }
            }
        taskAnnounce.setResult(isManagingNode() ? Boolean.TRUE : Boolean.FALSE);
        }
    
    public void onAnnouncementCompleted()
        {
        // import com.tangosol.net.InvocationService as com.tangosol.net.InvocationService;
        
        com.tangosol.net.InvocationService service = getService();
        if (service != null && service.isRunning())
            {
            setAnnounced(true);
            registerAll(getRemoteServers());
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
    * the dependencies directly as shown in the example below for Gateway
    * dependencies.  The advantage to this technique is that the property only
    * exists in the dependencies object, it is not duplicated in the component
    * properties.
    * 
    * ConnectorDependencies deps = (ConnectorDependencies) getDependencies();
    * deps.getFoo();
     */
    protected void onDependencies(com.tangosol.internal.net.management.ConnectorDependencies deps)
        {
        }
    
    public void onNotify(Connector.Notify taskNotify)
        {
        // import Component.Net.Management.Model.RemoteModel;
        
        String      sName = taskNotify.getName();
        RemoteModel model = (RemoteModel) getRemoteModels().get(sName);
        
        if (model == null)
            {
            _trace("Error handling notification " + sName, 3);
            }
        else
            {
            model._handleNotification(taskNotify.getNotification());
        
            setStatsNotificationCount(getStatsNotificationCount() + 1L);
            }
        }
    
    /**
     * Bind the published JMXServiceURL of the connector to NameSerice locally.
     */
    public void onPublish(Connector.Publish taskPublish)
        {
        synchronized (this)
            {
            setJmxServiceUrl(taskPublish.getJMXServiceURL());
            setJmxListenAddresses(taskPublish.getListenAddresses());
            }
        }
    
    /**
     * Registers the MBeans from each of the managed nodes with the MBeans
    * server.
     */
    public void onRegister(Connector.Register taskRegister)
        {
        // import Component.Net.Management.Gateway.Local as com.tangosol.coherence.component.net.management.gateway.Local;
        // import Component.Net.Management.Model.LocalModel;
        // import Component.Net.Management.Model.RemoteModel;
        // import com.tangosol.net.Member;
        // import java.util.Map;
        
        com.tangosol.coherence.component.net.management.gateway.Local localGateway = getLocalGateway();
        if (localGateway != null)
            {
            Member member = getMember(taskRegister.getMemberFrom());
            if (member == null)
                {
                // it looks like the member left immediately after registering
                return;
                }
        
            Map mapRemoteModels = getRemoteModels();
            LocalModel[] aModel = taskRegister.getSnapshot();
        
            for (int i = 0, c = aModel.length; i < c; i++)
                {
                LocalModel model = aModel[i];
        
                String      sName       = model.get_ModelName();
                RemoteModel modelRemote = (RemoteModel) mapRemoteModels.get(sName);
        
                if (modelRemote == null)
                    {
                    mapRemoteModels.put(sName, modelRemote = new RemoteModel());
        
                    modelRemote.set_ModelName(sName);
                    modelRemote.setConnector(this);
                    modelRemote.setAccessed(false);
                    }
                else if (!modelRemote.isResponsibilityMBean())
                    {
                    // it is possible during startup that announcements overlap
                    // resulting in multiple registerAll calls, however non-
                    // responsibility MBeans should never have multiple owners
                    if (!member.equals(modelRemote.getModelOwner()))
                        {
                        _trace("Unexpected multi-ownership for MBean: " + sName, 1);
                        }
                    }
                modelRemote.setSnapshot(model);
                modelRemote.setModelOwner(member);
        
                try
                    {
                    localGateway.registerModelMBean(sName, modelRemote);
                    }
                catch (RuntimeException e)
                    {
                    _trace("Failed to register MBean: " + sName, 1);
                    _trace(e);
                    }
                }
            }
        }
    
    /**
     * Process requests from remote MBeanServerProxy.
     */
    public void onRemoteInvoke(Connector.InvokeRemote taskInvoke, com.oracle.coherence.common.base.Continuation cont)
        {
        // import Component.Net.Management.Model.LocalModel;
        // import Component.Net.Management.Model.RemoteModel;
        // import Component.Util.Daemon.QueueProcessor.Service.Grid as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid;
        // import Component.Util.SafeService;
        // import com.tangosol.util.Filter;
        
        Exception exception = taskInvoke.getReadException();
        if (exception != null)
            {
            cont.proceed(exception);
            return;
            }
        
        int         nAction = taskInvoke.getAction();
        com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid     service = (com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid) ((SafeService) getService()).getService();
        String      sName   = taskInvoke.getName();
        ThreadLocal tlCont  = getThreadLocalContinuation();
        boolean     fAsync  = nAction == Connector.InvokeRemote.ACTION_EXECUTE    ||
                              nAction != Connector.InvokeRemote.ACTION_CHECK      &&
                              nAction != Connector.InvokeRemote.ACTION_QUERY      &&
                              nAction != Connector.InvokeRemote.ACTION_FIND_OWNER &&
                              sName != null && getRemoteModels().containsKey(sName) &&
                              service.getThread() == Thread.currentThread();
        if (fAsync)
            {
            // we will "proceed" with the result from the execution observer
            // (see RemoteModel: invokeRemote and invocationCompleted)
            tlCont.set(cont);
            }
        
        Object oResult;
        try
            {
            switch (nAction)
                {
                case Connector.InvokeRemote.ACTION_GET:
                    Filter filter = taskInvoke.getQueryFilter();
        
                    oResult = filter == null
                        ? getLocalGateway().getAttribute(sName, taskInvoke.getAttributeName())
                        : getLocalGateway().getAttributes(sName, filter);
                    break;
        
                case Connector.InvokeRemote.ACTION_SET:
                    getLocalGateway().setAttribute(
                        sName, taskInvoke.getAttributeName(), taskInvoke.getAttributeValue());
                    oResult = null;
                    break;
        
                case Connector.InvokeRemote.ACTION_INVOKE:
                    oResult = getLocalGateway().invoke(
                        sName, taskInvoke.getMethodName(), taskInvoke.getParameters(), taskInvoke.getSignatures());
                    break;
        
                case Connector.InvokeRemote.ACTION_EXECUTE:
                    // dispatch the function to be executed on the ED thread
        
                    Connector.ExecuteFunction function = (Connector.ExecuteFunction) _newChild("ExecuteFunction");
                    function.setInvocable(taskInvoke);
                    function.setContinuation(cont);
                    
                    service.ensureEventDispatcher().getQueue().add(function);
        
                    // result will be sent via continuation on the ED thread
                    oResult = null;
                    break;
        
                case Connector.InvokeRemote.ACTION_CHECK:
                    oResult = Boolean.valueOf(getLocalGateway().isMBeanRegistered(sName));
                    break;
        
                case Connector.InvokeRemote.ACTION_QUERY:
                    // @since 12.2.1.1
                    oResult = getLocalGateway().queryNames(taskInvoke.getQueryPattern(), taskInvoke.getQueryFilter());
                    break;
        
                case Connector.InvokeRemote.ACTION_MBEAN_INFO:
                    // @since 12.2.1.4
                    oResult = getLocalGateway().getMBeanInfo(sName);
                    break;
        
                case Connector.InvokeRemote.ACTION_FIND_OWNER:
                    // @since 12.2.1.4
                    LocalModel  modelLocal  = (LocalModel) getLocalRegistry().get(sName);
                    RemoteModel modelRemote = (RemoteModel)
                                    (modelLocal == null ? getRemoteModels().get(sName) : null);
        
                    oResult = modelLocal == null
                                ? modelRemote == null
                                    ? null
                                    : Integer.valueOf(modelRemote.getModelOwner().getId())
                                : Integer.valueOf(getLocalMemberId());
                    break;
        
                default:
                    oResult = new IllegalArgumentException("Invalid action: " + taskInvoke.getAction());
                    break; 
                }
            }
        catch (Throwable e)
            {
            fAsync  = false;
            oResult = e;
            }
        
        tlCont.set(null);
        
        if (!fAsync)
            {
            cont.proceed(oResult);
            }
        }
    
    public void onSubscribe(Connector.Subscribe taskSubscribe)
        {
        // import Component.Net.Management.Gateway;
        // import Component.Net.Management.Model.LocalModel;
        // import Component.Net.Management.ListenerHolder.RemoteHolder;
        
        String     sName = taskSubscribe.getName();
        LocalModel model = (LocalModel) getLocalRegistry().get(sName);
          
        if (model == null)
            {
            if (!Gateway.isResponsibilityMBean(sName))
                {
                _trace("Error updating subscription; unable to locate local model: " + sName, 2);
                }
            }
        else
            {
            if (taskSubscribe.getAction() == Connector.Subscribe.ACTION_SUBSCRIBE)
                {
                model._addRemoteNotificationListener(taskSubscribe.getHolder(), this);
                }
            else
                {
                int    nMemberId = taskSubscribe.getMemberId();
                long[] alId      = taskSubscribe.getHolderId();
        
                for (int i = 0, c = alId.length; i < c; i++)
                    {
                    model._removeRemoteNotificationListener(nMemberId, alId[i]);
                    }
                }
            }
        }
    
    /**
     * Unregisters the MBean from each of the managed nodes with the MBeans
    * server.
     */
    public void onUnregister(Connector.Unregister taskUnregister)
        {
        // import Component.Net.Management.Gateway.Local as com.tangosol.coherence.component.net.management.gateway.Local;
        // import Component.Net.Management.Model.RemoteModel;
        // import com.tangosol.util.Base;
        // import java.util.Map;
        
        com.tangosol.coherence.component.net.management.gateway.Local localGateway = getLocalGateway();
        if (localGateway != null)
            {
            try
                {
                String sName     = taskUnregister.getName();
                Map    mapModels = getRemoteModels();
        
                if (sName.endsWith(",*"))
                    {
                    localGateway.unregisterModelMBean(sName, null);
        
                    String sPattern = sName.substring(0, sName.length() - 2);
                    Gateway.removeByPattern(mapModels, sPattern);
                    }
                else
                    {
                    RemoteModel model = (RemoteModel) mapModels.get(sName);
                    if (model != null &&
                        Base.equals(model.getModelOwner(), getMember(taskUnregister.getMemberFrom())))
                        {
                        localGateway.unregisterModelMBean(sName, model);
        
                        mapModels.remove(sName);
                        }
                    }
                }
            catch (RuntimeException e)
                {
                _trace("Ignoring UnregisterRequest exception\n" + getStackTrace(e), 5);
                }
            }
        }
    
    /**
     * Refresh all active expired models.
    * 
    * @param model  a model that is retrieved unconditionally
     */
    public void refreshActiveModels(com.tangosol.coherence.component.net.management.model.RemoteModel model)
        {
        // import Component.Net.Management.Model.RemoteModel;
        // import java.util.Iterator;
        // import java.util.Map$Entry as java.util.Map.Entry;
        
        long cRefresh = 0L;
        for (Iterator iter = getRemoteModels().values().iterator(); iter.hasNext();)
            {
            RemoteModel modelNext = (RemoteModel) iter.next();
        
            if (modelNext.isRefreshRequired() && modelNext.isAccessed()
                   && modelNext.invokeRemoteAsync())
                {
                ++cRefresh;
                }
            }
        
        setStatsRefreshPredictionCount(getStatsRefreshPredictionCount() + cRefresh);
        }
    
    /**
     * Refresh a collection of specified MBeans. The passed-in Set contains
    * ObjectName or ObjectInstance references. This method is only used
    * "externally" by the WrapperMBeanServer.
     */
    public void refreshRemoteModels(java.util.Set setModels)
        {
        // import Component.Net.Management.Model.RemoteModel;
        // import java.util.Iterator;
        // import java.util.Map;
        // import javax.management.ObjectInstance;
        // import javax.management.ObjectName;
        // import com.tangosol.net.management.MBeanHelper;
        
        if (setModels != null)
            {
            Map mapModels = getRemoteModels();
            
            long cRefresh = 0L;
            for (Iterator iter = setModels.iterator(); iter.hasNext();)
                {
                Object oMBean = iter.next();
        
                ObjectName oname = oMBean instanceof ObjectInstance ?
                    ((ObjectInstance) oMBean).getObjectName() : (ObjectName) oMBean;
        
                RemoteModel model = (RemoteModel) mapModels.get(oname.getKeyPropertyListString());
                if (model != null)
                    {
                    if (!model.isAccessed())
                        {
                        setStatsRefreshExcessCount(getStatsRefreshExcessCount() + 1L);
                        } 
                    if (model.invokeRemoteAsync())
                        {
                        ++cRefresh;
                        }
                    }
                }
            setStatsRefreshPredictionCount(getStatsRefreshPredictionCount() + cRefresh);
            setStatsRefreshCount(getStatsRefreshCount() + cRefresh);
            }
        }
    
    /**
     * Register all beans in local registry with the specified set of MBean
    * servers.
     */
    protected void registerAll(java.util.Set setMembers)
        {
        // import Component.Net.Management.Model.LocalModel;
        
        LocalModel[] aModel = (LocalModel[])
            getLocalRegistry().values().toArray(new LocalModel[0]);
        
        if (aModel != null && aModel.length > 0)
            {
            sendRegister(setMembers, aModel);
            }
        }
    
    public void registerMemberListener(com.tangosol.net.MemberListener listener)
        {
        getService().addMemberListener(listener);
        }
    
    /**
     * API for the Remote Gateway.
    * 
    * @param model  the model to register
     */
    public void registerModel(com.tangosol.coherence.component.net.management.model.LocalModel model)
        {
        // import Component.Net.Management.Gateway;
        // import Component.Net.Management.Model.LocalModel;
        // import Component.Net.Management.Model.LocalModel.WrapperModel;
        // import Component.Net.Management.Model.LocalModel.WrapperModel.WrapperJmxModel;
        // import com.tangosol.net.management.Registry;
        // import java.util.Set;
        
        String sName = model.get_ModelName();
        
        _assert(!model.is_Snapshot());
        _assert(sName != null);
        
        LocalModel modelOld  = (LocalModel) getLocalRegistry().put(sName, model);
        
        // we should not skip registration for MBeans that could have been previously
        // registered by other nodes (see COH-8251)
        if (modelOld != null && sName.indexOf(Registry.KEY_NODE_ID) >= 0)
            {
            // there is already a local model with that name;
            // if the model type is the same, there is no need to send 
            // the registration request again (see COH-4025 and COH-3682); 
            // the only implication is that the "snapshot" held by MBeanServer
            // will be stale for the remainder of the "refresh-timeout" interval
        
            boolean fSkipSend;
            if (model instanceof WrapperJmxModel && modelOld instanceof WrapperJmxModel)
                {
                // check the underlying MBeanInfo instead of the model type
                fSkipSend = ((WrapperJmxModel) model).getMBeanInfo().getClassName() ==
                            ((WrapperJmxModel) modelOld).getMBeanInfo().getClassName(); 
                }
            else if (model instanceof WrapperModel && modelOld instanceof WrapperModel)
                {
                // check the underlying MBean type instead of the model type
                fSkipSend = ((WrapperModel) model).getMBean().getClass() ==
                            ((WrapperModel) modelOld).getMBean().getClass(); 
                }
            else
                {
                fSkipSend = model.getClass() == modelOld.getClass();
                }
            
            if (fSkipSend)
                {
                return;
                }
            }
        
        if (getService() != null && isAnnounced() && Gateway.isGlobal(sName))
            {
            Set setServers = getRemoteServers();
            if (!setServers.isEmpty())
                {
                sendRegister(setServers, new LocalModel[] {model});
                }
            }
        }
    
    /**
     * Remove all local subscriptions.
     */
    protected void removeLocalSubscriptions()
        {
        // import Component.Net.Management.Model.LocalModel;
        // import java.util.Iterator;
        
        for (Iterator iter = getLocalRegistry().values().iterator(); iter.hasNext();)
            {
            ((LocalModel) iter.next())._removeNotificationListeners();
            }
        }
    
    /**
     * Remove all the subscriptions by the given member.
     */
    protected void removeRemoteSubscriptions(com.tangosol.net.Member member)
        {
        // import Component.Net.Management.Model.LocalModel;
        // import Component.Net.Management.NotificationHandler.RemoteHandler;
        // import java.util.Iterator;
        
        for (Iterator iter = getLocalRegistry().values().iterator(); iter.hasNext();)
            {
            LocalModel model = (LocalModel) iter.next();
        
            RemoteHandler handler = model.get_RemoteNotificationHandler();
            if (handler != null)
                {
                handler.unsubscribeMember(member.getId());
                }
            }
        }
    
    /**
     * For debugging only.
     */
    public String reportSubscriptions()
        {
        // import Component.Net.Management.Model.LocalModel;
        // import Component.Net.Management.Model.RemoteModel;
        // import java.util.Iterator;
        
        StringBuilder sb = new StringBuilder("LocalModels=\n");
        
        for (Iterator iter = getLocalRegistry().values().iterator(); iter.hasNext();)
            {
            LocalModel local = (LocalModel) iter.next();
            sb.append("Name=")
              .append(local.get_ModelName())
              .append(",LocalHandler=")
              .append(local.get_LocalNotificationHandler())
              .append(",RemoteHandler=")
              .append(local.get_RemoteNotificationHandler())
              .append('\n');
            }
        
        sb.append("RemoteModels=\n");
        for (Iterator iter = getRemoteModels().values().iterator(); iter.hasNext();)
            {
            RemoteModel remote = (RemoteModel) iter.next();
            sb.append("Name=")
              .append(remote.get_ModelName())
              .append(",LocalHandler=")
              .append(remote.get_LocalNotificationHandler())
              .append('\n');
            }
        return sb.toString();
        }
    
    /**
     * Reset the management performance statistics.
     */
    public void resetStatistics()
        {
        setStatsRefreshCount(0L);
        setStatsRefreshPredictionCount(0L);
        setStatsRefreshExcessCount(0L);
        setStatsRefreshTimeoutCount(0L);
        setStatsNotificationCount(0L);
        }
    
    // From interface: com.tangosol.net.NameService$Resolvable
    public Object resolve(com.tangosol.net.NameService.RequestContext ctx)
        {
        // import com.tangosol.net.InetAddressHelper;
        // import java.net.InetAddress;
        // import java.net.MalformedURLException;
        // import java.util.Iterator;
        // import java.util.Collection;
        // import javax.management.remote.JMXServiceURL;
        
        JMXServiceURL url;
        Collection    colInet;
        
        synchronized (this)
            {
            url     = getJmxServiceUrl();
            colInet = getJmxListenAddresses();
            }
        
        if (url == null)
            {
            return null;
            }
        
        String sHost = url.getHost();
        if (InetAddressHelper.isAnyLocalAddress(sHost)) // common
            {
            // the listener is on the wildcard address; resolve to actual IP
        
            // determine if the destinations are local
            boolean fLocalDest = true;
            for (Iterator iter = colInet.iterator(); fLocalDest && iter.hasNext(); )
                {
                fLocalDest = InetAddressHelper.isLocalAddress((InetAddress) iter.next());
                }
                
            Collection colAddresses = InetAddressHelper.getRoutableAddresses(ctx.getAcceptAddress(),
                InetAddressHelper.isLocalAddress(ctx.getSourceAddress()), colInet, fLocalDest);
        
            if (colAddresses == null || colAddresses.isEmpty())
                {
                return null;
                }
        
            InetAddress addr = (InetAddress) colAddresses.iterator().next();
        
            try
                {
                url = new JMXServiceURL(url.getProtocol(), addr.getHostAddress(), url.getPort(), url.getURLPath());
                }
            catch (MalformedURLException e)
                {
                return null;
                }
            }
        
        return url.toString();
        }
    
    /**
     * Send the Notify task.
     */
    public void sendNotification(java.util.Set setMembers, String sName, javax.management.Notification notification)
        {
        // import com.tangosol.net.InvocationService;
        // import java.util.Collections;
        // import java.util.Iterator;
        
        InvocationService service = getService();
        Connector.Notify           task    = (Connector.Notify) _newChild("Notify");
        
        task.setName(sName);
        task.setNotification(notification);
        
        for (Iterator iter = setMembers.iterator(); iter.hasNext(); )
            {
            service.execute(task, Collections.singleton(iter.next()), null);
            }
        }
    
    /**
     * Process an MBeanServerProxy request.
     */
    public Object sendProxyRequest(Connector.InvokeRemote task)
        {
        // import com.tangosol.net.Member;
        // import com.tangosol.util.Base;
        // import java.util.Collections;
        // import java.util.Map;
        
        while (true)
            {
            Member member = chooseRemoteServer(task.getAction());
        
            Map    mapResult = getService().query(task, Collections.singleton(member));
            Object oResult   = mapResult.get(member);
        
            if (oResult instanceof Throwable)
                {
                throw Base.ensureRuntimeException((Throwable) oResult);
                }
        
            if (oResult != null || mapResult.containsKey(member))
                {
                return oResult;
                }
            // the server must have left during the request, try another one
            }
        }
    
    /**
     * Send the Register task.
     */
    protected void sendRegister(java.util.Set setMembers, com.tangosol.coherence.component.net.management.model.LocalModel[] aModel)
        {
        // import com.tangosol.net.InvocationService;
        // import java.util.Collections;
        // import java.util.Iterator;
        
        InvocationService service = getService();
        Connector.Register         task    = (Connector.Register) _newChild("Register");
        
        task.setMemberFrom(getLocalMemberId());
        task.setSnapshot(aModel);
        
        try
            {
            // the serialized representation of models could be conditional based
            // on the version compatibility between the sender and the recipient;
            // to avoid a conflict in case recipients are of different versions,
            // we need to send the models individually peer-to-peer
            for (Iterator iter = setMembers.iterator(); iter.hasNext(); )
                {
                service.execute(task, Collections.singleton(iter.next()), null);
                }
            }
        catch (Exception e)
            {
            _trace("Failed to register " + aModel.length + " MBeans "
                + getStackTrace(e), 1);
            }
        }
    
    // Accessor for the property "Announced"
    /**
     * Setter for property Announced.<p>
    * Indicates that the handshake protocol ($Announce) has completed.
     */
    protected void setAnnounced(boolean f)
        {
        __m_Announced = f;
        }
    
    // Accessor for the property "ConnectorServer"
    /**
     * Setter for property ConnectorServer.<p>
    * A JMXConnectorServer started by this Connector.
     */
    protected void setConnectorServer(javax.management.remote.JMXConnectorServer server)
        {
        __m_ConnectorServer = server;
        }
    
    // Accessor for the property "Dependencies"
    /**
     * Inject the Dependencies object into the component.  First clone the
    * dependencies, then validate the cloned copy. 
    * Note that the validate method may modify the cloned dependencies, so it
    * is important to use the cloned dependencies for all subsequent
    * operations.  Once the dependencies have been validated, call
    * onDependencies so that each Component in the class hierarchy can process
    * the dependencies as needed.
     */
    public void setDependencies(com.tangosol.internal.net.management.ConnectorDependencies deps)
        {
        // import com.tangosol.internal.net.management.ConnectorDependencies;
        
        if (getDependencies() != null)
            {
            throw new IllegalStateException("Dependencies already set");
            }
        
        __m_Dependencies = (cloneDependencies(deps).validate());
        
        // use the cloned dependencies
        onDependencies(getDependencies());
        }
    
    // Accessor for the property "HttpManagingDynamic"
    /**
     * Setter for property HttpManagingDynamic.<p>
    * Specifies that this node should be an "HttpManagingNode" at the time it
    * becomes the senior Management service node.
    * 
    * @since 12.2.1.4
     */
    public void setHttpManagingDynamic(boolean fDynamic)
        {
        __m_HttpManagingDynamic = fDynamic;
        }
    
    // Accessor for the property "HttpManagingNode"
    /**
     * Setter for property HttpManagingNode.<p>
    * ﻿Specifies whether this node has an HTTP management service enabled.
    * 
    * @since 12.2.1.4
     */
    public void setHttpManagingNode(boolean fNode)
        {
        __m_HttpManagingNode = fNode;
        }
    
    // Accessor for the property "JmxListenAddresses"
    /**
     * Setter for property JmxListenAddresses.<p>
    * For a JMXServiceURL which is bound to the wildcard address this is the
    * expanded set of addresses which that wildcard address maps to.
     */
    public void setJmxListenAddresses(java.util.Collection sUrl)
        {
        __m_JmxListenAddresses = sUrl;
        }
    
    // Accessor for the property "JmxServiceUrl"
    /**
     * Setter for property JmxServiceUrl.<p>
    * The string representation of JMXServiceURL of the MBeanConnector; null if
    * this node is not a dynamic management node.
     */
    public void setJmxServiceUrl(javax.management.remote.JMXServiceURL lUrl)
        {
        __m_JmxServiceUrl = lUrl;
        }
    
    // Accessor for the property "LocalGateway"
    /**
     * Setter for property LocalGateway.<p>
    * The local (MBeanServer bound) gateway.
     */
    public void setLocalGateway(com.tangosol.coherence.component.net.management.gateway.Local gateway)
        {
        __m_LocalGateway = gateway;
        }
    
    // Accessor for the property "LocalRegistry"
    /**
     * Setter for property LocalRegistry.<p>
    * A cache (Map<<String>, <LocalModel>>) of managed Model objects registered
    * by this node with any of the available MBeanServers.
     */
    protected void setLocalRegistry(java.util.Map map)
        {
        __m_LocalRegistry = map;
        }
    
    // Accessor for the property "ManagingDynamic"
    /**
     * Setter for property ManagingDynamic.<p>
    * Specifies that this node should be a "ManagingNode" at the time it
    * becomes the senior Management service node.
     */
    public void setManagingDynamic(boolean fDynamic)
        {
        __m_ManagingDynamic = fDynamic;
        }
    
    // Accessor for the property "ManagingNode"
    /**
     * Setter for property ManagingNode.<p>
    * Specifies whether this Connector represents a managing node (has 
    * in-process MBeanServer).
     */
    public void setManagingNode(boolean fManaging)
        {
        __m_ManagingNode = fManaging;
        }
    
    // Accessor for the property "MsgFailure"
    /**
     * Setter for property MsgFailure.<p>
    * Licensing and security support.
     */
    private static void setMsgFailure(String sMsg)
        {
        __s_MsgFailure = sMsg;
        }
    
    // Accessor for the property "RefreshAttributeTimeoutMillis"
    /**
     * Setter for property RefreshAttributeTimeoutMillis.<p>
    * The timeout interval for making remote refresh calls.
     */
    protected void setRefreshAttributeTimeoutMillis(long cMillis)
        {
        __m_RefreshAttributeTimeoutMillis = cMillis;
        }
    
    public void setRefreshPolicy(String sRefreshPolicy)
        {
        getDependencies().setRefreshPolicy(sRefreshPolicy);
        }
    
    public void setRefreshTimeoutMillis(long lRefreshTimeoutMillis)
        {
        getDependencies().setRefreshTimeoutMillis(lRefreshTimeoutMillis);
        }
    
    // Accessor for the property "RemoteModels"
    /**
     * Setter for property RemoteModels.<p>
    * A map of RemoteModel objects keyed by the canonical MBean name registered
    * with an MBeanServer colocated with this node.
     */
    protected void setRemoteModels(java.util.Map mapModels)
        {
        __m_RemoteModels = mapModels;
        }
    
    // Accessor for the property "RemoteServers"
    /**
     * Setter for property RemoteServers.<p>
    * A set of Member objects for the managed nodes (have local MBeanServers
    * and are willing to manage remote nodes).
     */
    protected void setRemoteServers(java.util.Set setMember)
        {
        __m_RemoteServers = setMember;
        }
    
    // Accessor for the property "RequestTimeout"
    /**
     * Setter for property RequestTimeout.<p>
    * The request timeout for the underlying InvocationService
     */
    public void setRequestTimeout(long lTimeout)
        {
        __m_RequestTimeout = lTimeout;
        }
    
    // Accessor for the property "Service"
    /**
     * Setter for property Service.<p>
    * Invocation service used for remote management.
     */
    protected void setService(com.tangosol.net.InvocationService service)
        {
        __m_Service = service;
        }
    
    // Accessor for the property "StatsNotificationCount"
    /**
     * Setter for property StatsNotificationCount.<p>
    * The number of remote notifications received by the node since the last
    * time the statistics were reset.
     */
    protected void setStatsNotificationCount(long cNotifications)
        {
        __m_StatsNotificationCount = cNotifications;
        }
    
    // Accessor for the property "StatsRefreshCount"
    /**
     * Setter for property StatsRefreshCount.<p>
    * The total number of snapshots retrieved since the statistics were last
    * reset.
     */
    protected void setStatsRefreshCount(long cSync)
        {
        __m_StatsRefreshCount = cSync;
        }
    
    // Accessor for the property "StatsRefreshExcessCount"
    /**
     * Setter for property StatsRefreshExcessCount.<p>
    * The number of times a snapshot was retrieved predictively but not used
    * before the expiry time.
     */
    protected void setStatsRefreshExcessCount(long cMiss)
        {
        __m_StatsRefreshExcessCount = cMiss;
        }
    
    // Accessor for the property "StatsRefreshPredictionCount"
    /**
     * Setter for property StatsRefreshPredictionCount.<p>
    * The number of times snapshots were retrieved prior to a client request
    * based on the refresh policy.
     */
    protected void setStatsRefreshPredictionCount(long cRefresh)
        {
        __m_StatsRefreshPredictionCount = cRefresh;
        }
    
    // Accessor for the property "StatsRefreshTimeoutCount"
    /**
     * Setter for property StatsRefreshTimeoutCount.<p>
    * The number of times that the remote refresh times out.
     */
    public void setStatsRefreshTimeoutCount(long cCount)
        {
        __m_StatsRefreshTimeoutCount = cCount;
        }
    
    // Accessor for the property "ThreadLocalContinuation"
    /**
     * Setter for property ThreadLocalContinuation.<p>
    * A Continuation set on a thread local to be used by a RemoteModel. 
    * 
    * As the RemoteModel is called via an MBeanServer the Connector lacks an
    * ability to pass directly to the RemoteModel thus uses the stack to share
    * the Continuation.
    * 
    * The Continuation allows the RemoteModel to perform a non-blocking call
    * and call the Continuation once the non-blockintg call completes.
    * 
    * @see  #onRemoteInvoke
     */
    protected void setThreadLocalContinuation(ThreadLocal cont)
        {
        __m_ThreadLocalContinuation = cont;
        }
    
    /**
     * Start the InvocationService used for management.
     */
    public void startService(com.tangosol.coherence.component.util.SafeCluster cluster)
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid;
        // import Component.Util.SafeService;
        // import com.tangosol.discovery.NSLookup;
        // import com.tangosol.internal.net.management.HttpHelper;
        // import com.tangosol.net.InvocationService;
        // import com.tangosol.net.management.Registry;
        // import com.tangosol.run.xml.XmlHelper;
        // import com.tangosol.run.xml.XmlDocument;
        // import com.tangosol.util.Base;
        // import com.tangosol.util.Resources;
        // import java.net.URL;
        // import javax.naming.NameAlreadyBoundException;
        // import javax.naming.NamingException;
        
        InvocationService service;
        
        synchronized (this)
            {
            if (isStarted() || getMsgFailure() != null)
                {
                return;
                }
        
            if (cluster == null)
                {
                cluster = getCluster();
        
                if (cluster == null)
                    {
                    return;
                    }
                }
            }
        
        // ensure we hold the SafeCluster monitor as an ensureService call is iminent
        // and lock order of SafeCluster->Connector should be honoured
        
        cluster.ensureLocked();
        try
            {
            synchronized (this)
                {
                if (isStarted() || getMsgFailure() != null)
                    {
                    return;
                    }
        
                try
                    {
                    service = (InvocationService) cluster.ensureService(
                        Registry.SERVICE_NAME, InvocationService.TYPE_DEFAULT);
                    setService(service);
        
                    // use default configuration; no daemon pool is necessary
                    service.setContextClassLoader(getClass().getClassLoader());
                    service.setUserContext(this);
        
                    // This is to use the Default Serializer when POF is enabled.
                    URL url = Resources.findFileOrResourceOrDefault("management-config.xml", null);
                    if (url != null)
                        {
                        try
                            {
                            XmlDocument xml = XmlHelper.loadXml(url.openStream());
                            if (xml != null)
                                {
                                _trace("Loaded management configuration from \"" + url + '"', 3);
                                service.configure(xml);
                                }
                             }
                        catch (Exception e)
                            {
                            throw Base.ensureRuntimeException(e);
                            }
                        }
        
                    SafeService serviceSafe = (SafeService) service;
                    if (serviceSafe.isRestarting())
                        {
                        // we are called as a result of the safe tier restart;
                        // it's a recursive call - prevent a deadlock (COH-4729);
                        // the message is intentionally a bit obscure - mainly for debugging
                        _trace("Resetting the JMX Connector; State="
                             + serviceSafe.getSafeServiceState(), 3);
                        }
                    else
                        {
                        service.start();
        
                        // add the synchronous listener after the service has started
                        service.addMemberListener(this);
        
                        // prime the global knowledge of whether this node is dynamic
                        Grid _service = (Grid) serviceSafe.getService();
        
                        _service.getThisMemberConfigMap().put(
                            "dynamic-management", Boolean.valueOf(isManagingDynamic()));
                        setRequestTimeout(_service.getRequestTimeout());
        
                        // initialize
                        if (!isManagingNode() &&
                            getDynamicSenior() == service.getCluster().getLocalMember())
                            {
                            assumeManagement();
        
                            if (isHttpManagingDynamic())
                                {
                                HttpHelper.startService(cluster);
                                }
                            }
        
                        if (isHttpManagingNode())
                            {
                            HttpHelper.startService(cluster);
                            }
        
                        // register with the NameService
                        try
                            {
                            getNameService().bind(NSLookup.JMX_CONNECTOR_URL, this);
                            }
                        catch (NameAlreadyBoundException e)
                            {
                            // this is a restart
                            }
                        catch (NamingException e)
                            {
                            throw Base.ensureRuntimeException(e);
                            }
        
                        announce(service);
                        }
                    }
                catch (Exception e)
                    {
                    String sMsg = e.toString();
                    if (e instanceof SecurityException)
                        {
                        // chop off the stack trace
                        int of = sMsg.indexOf('\n');
                        if (of > 0)
                            {
                            sMsg = sMsg.substring(0, of);
                            }
                        }
                    setMsgFailure(sMsg);
                    _trace("Failure to initialize JMX remote management caused by: " + sMsg +
                        ". It will not be possible to manage this node remotely.", 1);
                    }
                }
            }
        finally
            {
            cluster.unlock();
            }
        }
    
    /**
     * Register a remote subscription.
     */
    public void subscribe(String sName, com.tangosol.net.Member member, com.tangosol.coherence.component.net.management.listenerHolder.RemoteHolder holder)
        {
        // import java.util.Collections;
        
        holder.setMemberId(getLocalMemberId());
        
        Connector.Subscribe task = new Connector.Subscribe();
        task.setAction(Connector.Subscribe.ACTION_SUBSCRIBE);
        task.setName(sName);
        task.setHolder(holder);
        
        getService().execute(task, Collections.singleton(member), null);
        }
    
    /**
     * API for the Remote Gateway.
    * 
    * @param sGivenName  an original name (a.k.a PropertyKeyListString) the
    * model is registered with
     */
    public void unregisterModel(String sGivenName)
        {
        // import Component.Net.Management.Gateway;
        // import com.tangosol.net.InvocationService as com.tangosol.net.InvocationService;
        // import com.tangosol.net.management.MBeanHelper;
        // import java.util.Map;
        // import java.util.Set;
        
        Map mapRegistry = getLocalRegistry();
        
        if (sGivenName.endsWith(",*"))
            {
            String sName    = MBeanHelper.stripDomain(sGivenName);
            String sPattern = sName.substring(0, sName.length() - 2);
        
            Gateway.removeByPattern(mapRegistry, sPattern);
            }
        else
            {  
            mapRegistry.remove(sGivenName);
            }
        
        com.tangosol.net.InvocationService service = getService();
        if (service != null && service.isRunning() && isAnnounced() && Gateway.isGlobal(sGivenName))
            {
            Set setServers = getRemoteServers();
            if (!setServers.isEmpty())
                {
                Connector.Unregister task = (Connector.Unregister) _newChild("Unregister");
                task.setMemberFrom(getLocalMemberId());
                task.setName(sGivenName);
        
                service.execute(task, setServers, null);
                }
            }
        }
    
    /**
     * Unregister a set of remote subscriptions.
     */
    public void unsubscribe(String sName, com.tangosol.net.Member member, java.util.Set setHolders)
        {
        // import Component.Net.Management.ListenerHolder.LocalHolder;
        // import Component.Net.Management.ListenerHolder.RemoteHolder;
        // import java.util.Collections;
        // import java.util.Iterator;
        
        long[] alId = new long[setHolders.size()];
        int    ix   = 0;
        for (Iterator iter = setHolders.iterator(); iter.hasNext();)
            {
            LocalHolder holder = (LocalHolder) iter.next();
        
            RemoteHolder hRemote = holder.getRemoteHolder();
            if (hRemote != null)
                {
                alId[ix++] = hRemote.getHolderId();
                }
            }
        
        Connector.Subscribe task = new Connector.Subscribe();
        task.setAction(Connector.Subscribe.ACTION_UNSUBSCRIBE);
        task.setName(sName);
        task.setMemberId(getLocalMemberId());
        task.setHolderId(alId);
        
        getService().execute(task, Collections.singleton(member), null);
        }

    // ---- class: com.tangosol.coherence.component.net.management.Connector$Announce
    
    /**
     * Announce the presence of a new managing node.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class Announce
            extends    com.tangosol.coherence.component.Net
            implements com.tangosol.io.ExternalizableLite,
                       com.tangosol.net.Invocable,
                       com.tangosol.net.InvocationObserver
        {
        // ---- Fields declarations ----
        
        /**
         * Property ManagingNode
         *
         * Specifies whether the announcer is a managing node (has in-process
         * MBeanServer)
         */
        private boolean __m_ManagingNode;
        
        /**
         * Property MemberFrom
         *
         * The originator's node id.
         */
        private int __m_MemberFrom;
        
        /**
         * Property Result
         *
         * Boolean.TRUE iff the responder is managing node.
         */
        private transient Object __m_Result;
        
        /**
         * Property Service
         *
         * The Management Service.
         */
        private transient com.tangosol.net.InvocationService __m_Service;
        
        // Default constructor
        public Announce()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public Announce(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.net.management.Connector.Announce();
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
                clz = Class.forName("com.tangosol.coherence/component/net/management/Connector$Announce".replace('/', '.'));
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
        
        // Accessor for the property "MemberFrom"
        /**
         * Getter for property MemberFrom.<p>
        * The originator's node id.
         */
        public int getMemberFrom()
            {
            return __m_MemberFrom;
            }
        
        // From interface: com.tangosol.net.Invocable
        // Accessor for the property "Result"
        /**
         * Getter for property Result.<p>
        * Boolean.TRUE iff the responder is managing node.
         */
        public Object getResult()
            {
            return __m_Result;
            }
        
        // Accessor for the property "Service"
        /**
         * Getter for property Service.<p>
        * The Management Service.
         */
        public com.tangosol.net.InvocationService getService()
            {
            return __m_Service;
            }
        
        // From interface: com.tangosol.net.Invocable
        public void init(com.tangosol.net.InvocationService service)
            {
            setService(service);
            }
        
        // From interface: com.tangosol.net.InvocationObserver
        public void invocationCompleted()
            {
            ((Connector) get_Module()).onAnnouncementCompleted();
            }
        
        // Accessor for the property "ManagingNode"
        /**
         * Getter for property ManagingNode.<p>
        * Specifies whether the announcer is a managing node (has in-process
        * MBeanServer)
         */
        public boolean isManagingNode()
            {
            return __m_ManagingNode;
            }
        
        // From interface: com.tangosol.net.InvocationObserver
        public void memberCompleted(com.tangosol.net.Member member, Object oResult)
            {
            Boolean FServer = (Boolean) oResult;
            if (FServer != null && FServer.booleanValue())
                {
                ((Connector) get_Module()).getRemoteServers().add(member);
                }
            }
        
        // From interface: com.tangosol.net.InvocationObserver
        public void memberFailed(com.tangosol.net.Member member, Throwable e)
            {
            // should not ever happen
            _trace("Failed announcement request at " + member + "\n" + getStackTrace(e), 1);
            }
        
        // From interface: com.tangosol.net.InvocationObserver
        public void memberLeft(com.tangosol.net.Member member)
            {
            }
        
        // From interface: com.tangosol.io.ExternalizableLite
        public void readExternal(java.io.DataInput in)
                throws java.io.IOException
            {
            // import com.tangosol.util.ExternalizableHelper;
            
            setMemberFrom(ExternalizableHelper.readInt(in));
            setManagingNode(in.readBoolean());
            }
        
        // From interface: com.tangosol.net.Invocable
        public void run()
            {
            Connector conn = (Connector) getService().getUserContext();
            conn.onAnnouncement(this);
            }
        
        // Accessor for the property "ManagingNode"
        /**
         * Setter for property ManagingNode.<p>
        * Specifies whether the announcer is a managing node (has in-process
        * MBeanServer)
         */
        public void setManagingNode(boolean f)
            {
            __m_ManagingNode = f;
            }
        
        // Accessor for the property "MemberFrom"
        /**
         * Setter for property MemberFrom.<p>
        * The originator's node id.
         */
        public void setMemberFrom(int nMember)
            {
            __m_MemberFrom = nMember;
            }
        
        // Accessor for the property "Result"
        /**
         * Setter for property Result.<p>
        * Boolean.TRUE iff the responder is managing node.
         */
        public void setResult(Object oResult)
            {
            __m_Result = oResult;
            }
        
        // Accessor for the property "Service"
        /**
         * Setter for property Service.<p>
        * The Management Service.
         */
        protected void setService(com.tangosol.net.InvocationService service)
            {
            __m_Service = service;
            }
        
        // Declared at the super level
        public String toString()
            {
            return get_Name() + " from Member " + getMemberFrom();
            }
        
        // From interface: com.tangosol.io.ExternalizableLite
        public void writeExternal(java.io.DataOutput out)
                throws java.io.IOException
            {
            // import com.tangosol.util.ExternalizableHelper;
            
            ExternalizableHelper.writeInt(out, getMemberFrom());
            out.writeBoolean(isManagingNode());
            }
        }

    // ---- class: com.tangosol.coherence.component.net.management.Connector$ExecuteFunction
    
    /**
     * A Runnable to be executed on the EventDispatcher thread to call the
     * function passed to an MBeanServerProxy.execute invocation.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class ExecuteFunction
            extends    com.tangosol.coherence.component.Util
            implements com.oracle.coherence.common.base.Continuation,
                       Runnable
        {
        // ---- Fields declarations ----
        
        /**
         * Property Continuation
         *
         * The continuation to call with the return of the function.
         */
        private com.oracle.coherence.common.base.Continuation __m_Continuation;
        
        /**
         * Property Invocable
         *
         * The Invocable (InvokeRemote) sent over the InvocationService to
         * execute a function passed to MBeanServerProxy.execute.
         */
        private Connector.InvokeRemote __m_Invocable;
        
        // Default constructor
        public ExecuteFunction()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public ExecuteFunction(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.net.management.Connector.ExecuteFunction();
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
                clz = Class.forName("com.tangosol.coherence/component/net/management/Connector$ExecuteFunction".replace('/', '.'));
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
        
        // Accessor for the property "Continuation"
        /**
         * Getter for property Continuation.<p>
        * The continuation to call with the return of the function.
         */
        public com.oracle.coherence.common.base.Continuation getContinuation()
            {
            return __m_Continuation;
            }
        
        // Accessor for the property "Invocable"
        /**
         * Getter for property Invocable.<p>
        * The Invocable (InvokeRemote) sent over the InvocationService to
        * execute a function passed to MBeanServerProxy.execute.
         */
        public Connector.InvokeRemote getInvocable()
            {
            return __m_Invocable;
            }
        
        // From interface: com.oracle.coherence.common.base.Continuation
        public void proceed(Object oResult)
            {
            // import Component.Net.Management.Connector$InvokeRemote as Connector.InvokeRemote;
            // import com.oracle.coherence.common.base.Continuation;
            // import com.tangosol.util.Base;
            
            Connector.InvokeRemote invocable = getInvocable();
            Continuation cont      = getContinuation();
            
            if (oResult instanceof Throwable)
                {
                if (cont == null)
                    {
                    // the absence of the continuation means we have no recepient to throw
                    // the exception back to, hence log
            
                    _trace("Unable to execute MBeanServerProxy.execute or throw exception to client due to: \n" +
                        Base.getStackTrace((Throwable) oResult) + '\n' + 
                        invocable, 5);
                    }
                }
            
            if (cont != null)
                {
                cont.proceed(oResult);
                }
            }
        
        // From interface: java.lang.Runnable
        public void run()
            {
            // call the local gateway with the appropriate function passing this as the
            // continuation to allow the InvokeRemote to be logged
            
            ((Connector) get_Module()).getLocalGateway().executeInternal(
                            getInvocable().getFunction(),
                            this);
            }
        
        // Accessor for the property "Continuation"
        /**
         * Setter for property Continuation.<p>
        * The continuation to call with the return of the function.
         */
        public void setContinuation(com.oracle.coherence.common.base.Continuation function)
            {
            __m_Continuation = function;
            }
        
        // Accessor for the property "Invocable"
        /**
         * Setter for property Invocable.<p>
        * The Invocable (InvokeRemote) sent over the InvocationService to
        * execute a function passed to MBeanServerProxy.execute.
         */
        public void setInvocable(Connector.InvokeRemote remoteInvocable)
            {
            __m_Invocable = remoteInvocable;
            }
        }

    // ---- class: com.tangosol.coherence.component.net.management.Connector$InvokeRemote
    
    /**
     * Remote MBeanServer invocation task on behalf of the MBeanServerProxy.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class InvokeRemote
            extends    com.tangosol.coherence.component.Net
            implements com.tangosol.io.ExternalizableLite,
                       com.tangosol.net.NonBlockingInvocable
        {
        // ---- Fields declarations ----
        
        /**
         * Property Action
         *
         * The action. Valid values are any of the ACTION_ constants.
         */
        private int __m_Action;
        
        /**
         * Property ACTION_CHECK
         *
         * The "isMBeanRegistered" action.
         */
        public static final int ACTION_CHECK = 3;
        
        /**
         * Property ACTION_EXECUTE
         *
         * The "execute" action on the MBeansServerProxy.
         */
        public static final int ACTION_EXECUTE = 5;
        
        /**
         * Property ACTION_FIND_OWNER
         *
         * The action required as a part of addNotificationListener support on
         * the MBSProxy.
         */
        public static final int ACTION_FIND_OWNER = 7;
        
        /**
         * Property ACTION_GET
         *
         * The "getAttribute" action.
         */
        public static final int ACTION_GET = 0;
        
        /**
         * Property ACTION_INVOKE
         *
         * The "invoke" action.
         */
        public static final int ACTION_INVOKE = 1;
        
        /**
         * Property ACTION_MBEAN_INFO
         *
         * The "getMBeanInfo" action.
         */
        public static final int ACTION_MBEAN_INFO = 6;
        
        /**
         * Property ACTION_QUERY
         *
         * The "queryNames" action.
         */
        public static final int ACTION_QUERY = 4;
        
        /**
         * Property ACTION_SET
         *
         * The "setAttribute" action.
         */
        public static final int ACTION_SET = 2;
        
        /**
         * Property AttributeName
         *
         * The attribute name.
         */
        private String __m_AttributeName;
        
        /**
         * Property AttributeValue
         *
         * The attribute value to set for setAttribute() call.
         */
        private Object __m_AttributeValue;
        
        /**
         * Property Function
         *
         * The result of invocation from getAttribute(), setAttribute() or
         * invoke(). It can also be an exception thrown by the remote
         * MBeanServer.
         */
        private transient com.tangosol.util.function.Remote.Function __m_Function;
        
        /**
         * Property MethodName
         *
         * The name of the method to be invoked for invoke() call.
         */
        private String __m_MethodName;
        
        /**
         * Property Name
         *
         * The name of the MBean.
         */
        private String __m_Name;
        
        /**
         * Property Parameters
         *
         * An array of parameters for the invoke() call.
         */
        private Object[] __m_Parameters;
        
        /**
         * Property QueryFilter
         *
         * The filter to be applied for the retrieved MBeans in queryNames()
         * call.
         */
        private com.tangosol.util.Filter __m_QueryFilter;
        
        /**
         * Property QueryPattern
         *
         * The MBean name pattern identifying the MBean names to be retrieved
         * for queryNames().
         */
        private String __m_QueryPattern;
        
        /**
         * Property ReadException
         *
         * (Transient) An Exception that occurred during the readExternal() and
         * has been deferred to be processed during onRemoteInvoke().
         */
        private Exception __m_ReadException;
        
        /**
         * Property Result
         *
         * The result of invocation from getAttribute(), setAttribute() or
         * invoke(). It can also be an exception thrown by the remote
         * MBeanServer.
         */
        private Object __m_Result;
        
        /**
         * Property Service
         *
         * The Management Service.
         */
        private transient com.tangosol.net.InvocationService __m_Service;
        
        /**
         * Property Signatures
         *
         */
        private String[] __m_Signatures;
        
        // Default constructor
        public InvokeRemote()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public InvokeRemote(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.net.management.Connector.InvokeRemote();
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
                clz = Class.forName("com.tangosol.coherence/component/net/management/Connector$InvokeRemote".replace('/', '.'));
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
        
        // Accessor for the property "Action"
        /**
         * Getter for property Action.<p>
        * The action. Valid values are any of the ACTION_ constants.
         */
        public int getAction()
            {
            return __m_Action;
            }
        
        // Accessor for the property "AttributeName"
        /**
         * Getter for property AttributeName.<p>
        * The attribute name.
         */
        public String getAttributeName()
            {
            return __m_AttributeName;
            }
        
        // Accessor for the property "AttributeValue"
        /**
         * Getter for property AttributeValue.<p>
        * The attribute value to set for setAttribute() call.
         */
        public Object getAttributeValue()
            {
            return __m_AttributeValue;
            }
        
        // Accessor for the property "Function"
        /**
         * Getter for property Function.<p>
        * The result of invocation from getAttribute(), setAttribute() or
        * invoke(). It can also be an exception thrown by the remote
        * MBeanServer.
         */
        public com.tangosol.util.function.Remote.Function getFunction()
            {
            return __m_Function;
            }
        
        // Accessor for the property "MethodName"
        /**
         * Getter for property MethodName.<p>
        * The name of the method to be invoked for invoke() call.
         */
        public String getMethodName()
            {
            return __m_MethodName;
            }
        
        // Accessor for the property "Name"
        /**
         * Getter for property Name.<p>
        * The name of the MBean.
         */
        public String getName()
            {
            return __m_Name;
            }
        
        // Accessor for the property "Parameters"
        /**
         * Getter for property Parameters.<p>
        * An array of parameters for the invoke() call.
         */
        public Object[] getParameters()
            {
            return __m_Parameters;
            }
        
        // Accessor for the property "QueryFilter"
        /**
         * Getter for property QueryFilter.<p>
        * The filter to be applied for the retrieved MBeans in queryNames()
        * call.
         */
        public com.tangosol.util.Filter getQueryFilter()
            {
            return __m_QueryFilter;
            }
        
        // Accessor for the property "QueryPattern"
        /**
         * Getter for property QueryPattern.<p>
        * The MBean name pattern identifying the MBean names to be retrieved
        * for queryNames().
         */
        public String getQueryPattern()
            {
            return __m_QueryPattern;
            }
        
        // Accessor for the property "ReadException"
        /**
         * Getter for property ReadException.<p>
        * (Transient) An Exception that occurred during the readExternal() and
        * has been deferred to be processed during onRemoteInvoke().
         */
        public Exception getReadException()
            {
            return __m_ReadException;
            }
        
        // From interface: com.tangosol.net.NonBlockingInvocable
        // Accessor for the property "Result"
        /**
         * Getter for property Result.<p>
        * The result of invocation from getAttribute(), setAttribute() or
        * invoke(). It can also be an exception thrown by the remote
        * MBeanServer.
         */
        public Object getResult()
            {
            return __m_Result;
            }
        
        // Accessor for the property "Service"
        /**
         * Getter for property Service.<p>
        * The Management Service.
         */
        public com.tangosol.net.InvocationService getService()
            {
            return __m_Service;
            }
        
        // Accessor for the property "Signatures"
        /**
         * Getter for property Signatures.<p>
         */
        public String[] getSignatures()
            {
            return __m_Signatures;
            }
        
        // From interface: com.tangosol.net.NonBlockingInvocable
        public void init(com.tangosol.net.InvocationService service)
            {
            setService(service);
            }
        
        // From interface: com.tangosol.io.ExternalizableLite
        public void readExternal(java.io.DataInput in)
                throws java.io.IOException
            {
            // import com.tangosol.util.function.Remote$Function as com.tangosol.util.function.Remote.Function;
            // import com.tangosol.util.Base;
            // import com.tangosol.util.ExternalizableHelper as com.tangosol.util.ExternalizableHelper;
            // import com.tangosol.util.Filter;
            
            int nAction = com.tangosol.util.ExternalizableHelper.readInt(in);
            
            setAction(nAction);
            setName(com.tangosol.util.ExternalizableHelper.readSafeUTF(in));
            
            switch (nAction)
                {
                case ACTION_GET:
                    setAttributeName(com.tangosol.util.ExternalizableHelper.readSafeUTF(in));
                    try
                        {
                        if (com.tangosol.util.ExternalizableHelper.isVersionCompatible(in, 12, 2, 1, 4, 0))
                            {
                            setQueryFilter((Filter) com.tangosol.util.ExternalizableHelper.readObject(in));
                            }
                        }
                    catch (Throwable t)
                        {
                        setReadException(Base.ensureRuntimeException(t));
                        }
                    break;
            
                case ACTION_SET:
                    setAttributeName(com.tangosol.util.ExternalizableHelper.readSafeUTF(in));
                    setAttributeValue(com.tangosol.util.ExternalizableHelper.readObject(in));
                    break;
            
                case ACTION_QUERY:
                    setQueryPattern(com.tangosol.util.ExternalizableHelper.readSafeUTF(in));
                    try
                        {
                        setQueryFilter((Filter) com.tangosol.util.ExternalizableHelper.readObject(in));
                        }
                    catch (Throwable t)
                        {
                        setReadException(Base.ensureRuntimeException(t));
                        }
                    break;
            
                case ACTION_INVOKE:
                    {
                    setMethodName(com.tangosol.util.ExternalizableHelper.readSafeUTF(in));
            
                    int c = com.tangosol.util.ExternalizableHelper.readInt(in);
                    if (c > 0)
                        {
                        Object[] aoParam = new Object[c];
                        try
                            {
                            for (int i = 0; i < c; i++)
                                {
                                aoParam[i] = com.tangosol.util.ExternalizableHelper.readObject(in);
                                }
                            }
                        catch (Throwable t)
                            {
                            setReadException(Base.ensureRuntimeException(t));
                            }
            
                        setParameters(aoParam);
                        }
            
                    if (in.readBoolean())
                        {
                        setSignatures(com.tangosol.util.ExternalizableHelper.readStringArray(in));
                        }
                    break;
                    }
            
                case ACTION_EXECUTE:
                    {
                    try
                        {
                        setFunction((com.tangosol.util.function.Remote.Function) com.tangosol.util.ExternalizableHelper.readObject(in));
                        }
                    catch (Throwable t)
                        {
                        setReadException(Base.ensureRuntimeException(t));
                        }
                    break;
                    }
                }
            }
        
        // From interface: com.tangosol.net.NonBlockingInvocable
        public void run()
            {
            throw new IllegalStateException();
            }
        
        // From interface: com.tangosol.net.NonBlockingInvocable
        public void run(com.oracle.coherence.common.base.Continuation cont)
            {
            Connector conn = (Connector) getService().getUserContext();
            
            conn.onRemoteInvoke(this, cont);
            }
        
        // Accessor for the property "Action"
        /**
         * Setter for property Action.<p>
        * The action. Valid values are any of the ACTION_ constants.
         */
        public void setAction(int nAction)
            {
            __m_Action = nAction;
            }
        
        // Accessor for the property "AttributeName"
        /**
         * Setter for property AttributeName.<p>
        * The attribute name.
         */
        public void setAttributeName(String sName)
            {
            __m_AttributeName = sName;
            }
        
        // Accessor for the property "AttributeValue"
        /**
         * Setter for property AttributeValue.<p>
        * The attribute value to set for setAttribute() call.
         */
        public void setAttributeValue(Object oValue)
            {
            __m_AttributeValue = oValue;
            }
        
        // Accessor for the property "Function"
        /**
         * Setter for property Function.<p>
        * The result of invocation from getAttribute(), setAttribute() or
        * invoke(). It can also be an exception thrown by the remote
        * MBeanServer.
         */
        public void setFunction(com.tangosol.util.function.Remote.Function oResult)
            {
            __m_Function = oResult;
            }
        
        // Accessor for the property "MethodName"
        /**
         * Setter for property MethodName.<p>
        * The name of the method to be invoked for invoke() call.
         */
        public void setMethodName(String sName)
            {
            __m_MethodName = sName;
            }
        
        // Accessor for the property "Name"
        /**
         * Setter for property Name.<p>
        * The name of the MBean.
         */
        public void setName(String f)
            {
            __m_Name = f;
            }
        
        // Accessor for the property "Parameters"
        /**
         * Setter for property Parameters.<p>
        * An array of parameters for the invoke() call.
         */
        public void setParameters(Object[] aoParameters)
            {
            __m_Parameters = aoParameters;
            }
        
        // Accessor for the property "QueryFilter"
        /**
         * Setter for property QueryFilter.<p>
        * The filter to be applied for the retrieved MBeans in queryNames()
        * call.
         */
        public void setQueryFilter(com.tangosol.util.Filter filterQuery)
            {
            __m_QueryFilter = filterQuery;
            }
        
        // Accessor for the property "QueryPattern"
        /**
         * Setter for property QueryPattern.<p>
        * The MBean name pattern identifying the MBean names to be retrieved
        * for queryNames().
         */
        public void setQueryPattern(String sPattern)
            {
            __m_QueryPattern = sPattern;
            }
        
        // Accessor for the property "ReadException"
        /**
         * Setter for property ReadException.<p>
        * (Transient) An Exception that occurred during the readExternal() and
        * has been deferred to be processed during onRemoteInvoke().
         */
        public void setReadException(Exception e)
            {
            __m_ReadException = e;
            }
        
        // Accessor for the property "Result"
        /**
         * Setter for property Result.<p>
        * The result of invocation from getAttribute(), setAttribute() or
        * invoke(). It can also be an exception thrown by the remote
        * MBeanServer.
         */
        public void setResult(Object oResult)
            {
            __m_Result = oResult;
            }
        
        // Accessor for the property "Service"
        /**
         * Setter for property Service.<p>
        * The Management Service.
         */
        protected void setService(com.tangosol.net.InvocationService service)
            {
            __m_Service = service;
            }
        
        // Accessor for the property "Signatures"
        /**
         * Setter for property Signatures.<p>
         */
        public void setSignatures(String[] asSignatures)
            {
            __m_Signatures = asSignatures;
            }
        
        // Declared at the super level
        public String toString()
            {
            // import java.util.Arrays;
            
            String s = "InvokeRemote(action=";
            
            switch (getAction())
                {
                case ACTION_GET:
                    return s + "GET, name=" + getName() + ", attributeName=" + getAttributeName() + ')';
                case ACTION_INVOKE:
                    return s + "INVOKE, name=" + getName() + ", methodName=" + getMethodName() +
                        ", paramNames=" + Arrays.deepToString(getParameters()) +
                        ", sigantures=" + Arrays.deepToString(getSignatures()) + ')';
                case ACTION_SET:
                    return s + "SET, name=" + getName() + ", attributeName=" + getAttributeName() +
                        ", attributeValue=" + getAttributeValue() + ')';
                case ACTION_CHECK:
                    return s + "CHECK, name=" + getName() + ')';
                case ACTION_QUERY:
                    return s + "QUERY, queryPattern=" + getQueryPattern() + ", queryFilter=" + getQueryFilter() + ')';
                case ACTION_EXECUTE:
                    return s + "EXECUTE, function=" + getFunction() + ')';
                }
            
            return "UNKNOWN)";
            }
        
        // From interface: com.tangosol.io.ExternalizableLite
        public void writeExternal(java.io.DataOutput out)
                throws java.io.IOException
            {
            // import com.tangosol.util.ExternalizableHelper as com.tangosol.util.ExternalizableHelper;
            // import com.tangosol.util.Filter;
            // import java.io.IOException;
            // import java.util.Map;
            // import java.util.HashMap;
            
            int nAction = getAction();
            
            com.tangosol.util.ExternalizableHelper.writeInt(out, nAction);
            com.tangosol.util.ExternalizableHelper.writeSafeUTF(out, getName());
            
            switch (nAction)
                {
                case ACTION_GET:
                    com.tangosol.util.ExternalizableHelper.writeSafeUTF(out, getAttributeName());
                    Filter filter = getQueryFilter();
                    if (filter != null)
                        {
                        if (!com.tangosol.util.ExternalizableHelper.isVersionCompatible(out, 12, 2, 1, 4, 0))
                            {
                            throw new UnsupportedOperationException(
                                "MBeanServerProxy.getAttributes(String, Filter) not supported by management node");
                            }
                        }
                    try
                        {
                        // Filter parameter added in 12.2.1.4.0
                        com.tangosol.util.ExternalizableHelper.writeObject(out, getQueryFilter());
                        }
                    catch (IOException e)
                        {
                        _trace("Query Filter is not serializable: " + getQueryFilter(), 1);
                        throw e;
                        }
                    break;
                
                case ACTION_SET:
                    com.tangosol.util.ExternalizableHelper.writeSafeUTF(out, getAttributeName());
                    com.tangosol.util.ExternalizableHelper.writeObject(out, getAttributeValue());
                    break;
            
                 case ACTION_QUERY:
                    com.tangosol.util.ExternalizableHelper.writeSafeUTF(out, getQueryPattern());
                    try
                        {
                        com.tangosol.util.ExternalizableHelper.writeObject(out, getQueryFilter());
                        }
                    catch (IOException e)
                        {
                        _trace("Query Filter is not serializable: " + getQueryFilter(), 1);
                        throw e;
                        }
                    break;
                    
                case ACTION_INVOKE:
                    {
                    com.tangosol.util.ExternalizableHelper.writeSafeUTF(out, getMethodName());
            
                    Object[] aoParam = getParameters();
                    if (aoParam == null)
                        {
                        com.tangosol.util.ExternalizableHelper.writeInt(out, 0);
                        }
                    else
                        {
                        int c = aoParam.length;
            
                        com.tangosol.util.ExternalizableHelper.writeInt(out, c);
                        for (int i = 0; i < c; i++)
                            {
                            try
                                {
                                com.tangosol.util.ExternalizableHelper.writeObject(out, aoParam[i]);
                                }
                            catch (IOException e)
                                {
                                _trace("The invoke parameter [" + i + "] for \""
                                    + getMethodName() + "\" is not serializable; ", 1);
                                throw e;
                                }
                            }
                        }
            
                    String[] asSig = getSignatures();
                    if (asSig == null)
                        {
                        out.writeBoolean(false);
                        }
                    else
                        {
                        out.writeBoolean(true);
                        com.tangosol.util.ExternalizableHelper.writeStringArray(out, asSig);
                        }
            
                    break;
                    }
            
                case ACTION_EXECUTE:
                    {
                    if (!com.tangosol.util.ExternalizableHelper.isVersionCompatible(out, 12, 2, 1, 4, 0))
                        {
                        throw new UnsupportedOperationException(
                            "MBeanServerProxy.execute(Remote.Function) not supported by management node");
                        }
                    com.tangosol.util.ExternalizableHelper.writeObject(out, getFunction());
                    break;
                    }
            
                case ACTION_MBEAN_INFO:
                    {
                    if (!com.tangosol.util.ExternalizableHelper.isVersionCompatible(out, 12, 2, 1, 4, 0))
                        {
                        throw new UnsupportedOperationException(
                            "MBeanServerProxy.getMBeanInfo(String name) not supported by management node");
                        }
                    break;
                    }
                }
            }
        }

    // ---- class: com.tangosol.coherence.component.net.management.Connector$LookupCallback
    
    /**
     * Publish JMXServiceURL task.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class LookupCallback
            extends    com.tangosol.coherence.component.Net
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
            return new com.tangosol.coherence.component.net.management.Connector.LookupCallback();
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
                clz = Class.forName("com.tangosol.coherence/component/net/management/Connector$LookupCallback".replace('/', '.'));
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
        public Object lookup(String sName, com.tangosol.net.Cluster cluster, com.tangosol.net.NameService.RequestContext ctx)
                throws javax.naming.NamingException
            {
            // import Component.Net.Cluster as com.tangosol.coherence.component.net.Cluster;
            // import com.tangosol.discovery.NSLookup;
            // import com.tangosol.internal.health.HealthHttpHandler;
            // import com.tangosol.internal.net.management.HttpHelper;
            // import com.tangosol.internal.net.metrics.MetricsHttpHelper;
            // import com.tangosol.util.Base;
            // import java.net.URL;
            // import java.util.ArrayList;
            // import java.util.Collection;
            // import java.util.Iterator;
            // import java.util.Set;
            
            if (sName.startsWith(NSLookup.HTTP_MANAGEMENT_URL))
                {
                // Management over REST acceptor lookup
                try
                    {
                    Set setAddresses = (Set) ((com.tangosol.coherence.component.net.Cluster) cluster).getNameService()
                        .lookup(HttpHelper.getServiceName() + "/addresses", ctx);
            
                    if (setAddresses == null || setAddresses.isEmpty())
                        {
                        return null;
                        }
            
                    Collection colUrls = new ArrayList();
                    for (Iterator iter = setAddresses.iterator(); iter.hasNext(); )
                        {
                        String[] asSockAddr = ((String) iter.next()).split(":");
                        if (asSockAddr.length > 2)
                            {
                            colUrls.add(HttpHelper.composeURL(asSockAddr[0], Integer.parseInt(asSockAddr[1]), asSockAddr[2]));
                            }
                        else
                            {
                            colUrls.add(HttpHelper.composeURL(asSockAddr[0], Integer.parseInt(asSockAddr[1])));
                            }
                        }
                    return colUrls;
                    }
                catch (Exception e)
                    {
                    // unusual issue with the NameService lookup
                    _trace("Failed to obtain HTTP Management service listen address:\n" + Base.printStackTrace(e), 1);
                    }
                }
            else if (sName.startsWith(NSLookup.HTTP_METRICS_URL))
                {
                // Metrics acceptor lookup
                try
                    {
                    Set setAddresses = (Set) ((com.tangosol.coherence.component.net.Cluster) cluster).getNameService()
                        .lookup(MetricsHttpHelper.getServiceName() + "/addresses", ctx);
            
                    if (setAddresses == null || setAddresses.isEmpty())
                        {
                        return null;
                        }
            
                    Collection colUrls = new ArrayList();
                    for (Iterator iter = setAddresses.iterator(); iter.hasNext(); )
                        {
                        String[] asSockAddr = ((String) iter.next()).split(":");
                        if (asSockAddr.length > 2)
                            {
                            colUrls.add(MetricsHttpHelper.composeURL(asSockAddr[0], Integer.parseInt(asSockAddr[1]), asSockAddr[2]));
                            }
                        else
                            {
                            colUrls.add(MetricsHttpHelper.composeURL(asSockAddr[0], Integer.parseInt(asSockAddr[1])));
                            }
                        }
                    return colUrls;
                    }
                catch (Exception e)
                    {
                    // unusual issue with the NameService lookup
                    _trace("Failed to obtain " + MetricsHttpHelper.getServiceName() + " service listen address:\n" + Base.printStackTrace(e), 1);
                    }
                }
            else if (sName.startsWith(NSLookup.HTTP_HEALTH_URL))
                {
                // Health acceptor lookup
                try
                    {
                    Set setAddresses = (Set) ((com.tangosol.coherence.component.net.Cluster) cluster).getNameService()
                        .lookup(HealthHttpHandler.getServiceName() + "/addresses", ctx);
            
                    if (setAddresses == null || setAddresses.isEmpty())
                        {
                        return null;
                        }
            
                    Collection colUrls = new ArrayList();
                    for (Iterator iter = setAddresses.iterator(); iter.hasNext(); )
                        {
                        String[] asSockAddr = ((String) iter.next()).split(":");
                        if (asSockAddr.length > 2)
                            {
                            colUrls.add(new URL(asSockAddr[2], asSockAddr[0], Integer.parseInt(asSockAddr[1]), "/"));
                            }
                        else
                            {
                            colUrls.add(new URL("http", asSockAddr[0], Integer.parseInt(asSockAddr[1]), "/"));
                            }
                        }
                    return colUrls;
                    }
                catch (Exception e)
                    {
                    // unusual issue with the NameService lookup
                    _trace("Failed to obtain " + HealthHttpHandler.getServiceName() + " service listen address:\n" + Base.printStackTrace(e), 1);
                    }
                }
            
            return null;
            }
        }

    // ---- class: com.tangosol.coherence.component.net.management.Connector$Notify
    
    /**
     * Send a remote notification.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class Notify
            extends    com.tangosol.coherence.component.Net
            implements com.tangosol.io.ExternalizableLite,
                       com.tangosol.net.Invocable
        {
        // ---- Fields declarations ----
        
        /**
         * Property Name
         *
         * The notifying MBean name (internal).
         */
        private String __m_Name;
        
        /**
         * Property Notification
         *
         * The notification to be handled.
         */
        private javax.management.Notification __m_Notification;
        
        /**
         * Property Service
         *
         * The Management Service.
         */
        private com.tangosol.net.InvocationService __m_Service;
        
        // Default constructor
        public Notify()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public Notify(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.net.management.Connector.Notify();
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
                clz = Class.forName("com.tangosol.coherence/component/net/management/Connector$Notify".replace('/', '.'));
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
        
        // Accessor for the property "Name"
        /**
         * Getter for property Name.<p>
        * The notifying MBean name (internal).
         */
        public String getName()
            {
            return __m_Name;
            }
        
        // Accessor for the property "Notification"
        /**
         * Getter for property Notification.<p>
        * The notification to be handled.
         */
        public javax.management.Notification getNotification()
            {
            return __m_Notification;
            }
        
        // From interface: com.tangosol.net.Invocable
        public Object getResult()
            {
            return null;
            }
        
        // Accessor for the property "Service"
        /**
         * Getter for property Service.<p>
        * The Management Service.
         */
        public com.tangosol.net.InvocationService getService()
            {
            return __m_Service;
            }
        
        // From interface: com.tangosol.net.Invocable
        public void init(com.tangosol.net.InvocationService service)
            {
            setService(service);
            }
        
        // From interface: com.tangosol.io.ExternalizableLite
        public void readExternal(java.io.DataInput in)
                throws java.io.IOException
            {
            // import com.tangosol.util.ExternalizableHelper;
            // import javax.management.Notification;
            // import java.io.EOFException;
            
            setName(ExternalizableHelper.readUTF(in));
            try
                {
                setNotification((Notification) ExternalizableHelper.readObject(in));
                }
            catch (Throwable e)
                {
                if (!(e instanceof EOFException))
                    {
                    _trace("The Notification is not deserializable; " + e, 3);
                    }
                }
            }
        
        // From interface: com.tangosol.net.Invocable
        public void run()
            {
            Connector conn = (Connector) getService().getUserContext();
            conn.onNotify(this);
            }
        
        // Accessor for the property "Name"
        /**
         * Setter for property Name.<p>
        * The notifying MBean name (internal).
         */
        public void setName(String sName)
            {
            __m_Name = sName;
            }
        
        // Accessor for the property "Notification"
        /**
         * Setter for property Notification.<p>
        * The notification to be handled.
         */
        public void setNotification(javax.management.Notification notification)
            {
            __m_Notification = notification;
            }
        
        // Accessor for the property "Service"
        /**
         * Setter for property Service.<p>
        * The Management Service.
         */
        protected void setService(com.tangosol.net.InvocationService service)
            {
            __m_Service = service;
            }
        
        // Declared at the super level
        public String toString()
            {
            return "Notification for " + getName();
            }
        
        // From interface: com.tangosol.io.ExternalizableLite
        public void writeExternal(java.io.DataOutput out)
                throws java.io.IOException
            {
            // import com.tangosol.util.ExternalizableHelper;
            
            ExternalizableHelper.writeUTF(out, getName());
            try
                {
                ExternalizableHelper.writeObject(out, getNotification());
                }
            catch (Throwable e)
                {
                _trace("The Notification \"" + getNotification().getClass().getName()
                     + "\" is not serializable; " + e, 3);
                }
            }
        }

    // ---- class: com.tangosol.coherence.component.net.management.Connector$Publish
    
    /**
     * Publish JMXServiceURL task.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class Publish
            extends    com.tangosol.coherence.component.Net
            implements com.tangosol.io.ExternalizableLite,
                       com.tangosol.net.Invocable
        {
        // ---- Fields declarations ----
        
        /**
         * Property JMXServiceURL
         *
         * The JMXServiceURL of the MBeanConnector.
         */
        private javax.management.remote.JMXServiceURL __m_JMXServiceURL;
        
        /**
         * Property ListenAddresses
         *
         * For a JMXServiceURL which is bound to the wildcard address this is
         * the expanded set of addresses which that wildcard address maps to.
         */
        private java.util.Collection __m_ListenAddresses;
        
        /**
         * Property Service
         *
         * The Management Service.
         */
        private transient com.tangosol.net.InvocationService __m_Service;
        
        // Default constructor
        public Publish()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public Publish(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.net.management.Connector.Publish();
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
                clz = Class.forName("com.tangosol.coherence/component/net/management/Connector$Publish".replace('/', '.'));
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
        
        // Accessor for the property "JMXServiceURL"
        /**
         * Getter for property JMXServiceURL.<p>
        * The JMXServiceURL of the MBeanConnector.
         */
        public javax.management.remote.JMXServiceURL getJMXServiceURL()
            {
            return __m_JMXServiceURL;
            }
        
        // Accessor for the property "ListenAddresses"
        /**
         * Getter for property ListenAddresses.<p>
        * For a JMXServiceURL which is bound to the wildcard address this is
        * the expanded set of addresses which that wildcard address maps to.
         */
        public java.util.Collection getListenAddresses()
            {
            return __m_ListenAddresses;
            }
        
        // From interface: com.tangosol.net.Invocable
        public Object getResult()
            {
            return null;
            }
        
        // Accessor for the property "Service"
        /**
         * Getter for property Service.<p>
        * The Management Service.
         */
        public com.tangosol.net.InvocationService getService()
            {
            return __m_Service;
            }
        
        // From interface: com.tangosol.net.Invocable
        public void init(com.tangosol.net.InvocationService service)
            {
            setService(service);
            }
        
        // From interface: com.tangosol.io.ExternalizableLite
        public void readExternal(java.io.DataInput in)
                throws java.io.IOException
            {
            // import com.tangosol.util.ExternalizableHelper;
            // import java.util.HashSet;
            // import javax.management.remote.JMXServiceURL;
            
            setJMXServiceURL((JMXServiceURL) ExternalizableHelper.readObject(in));
            
            HashSet setInet = new HashSet();
            ExternalizableHelper.readCollection(in, setInet, null);
            setListenAddresses(setInet);
            }
        
        // From interface: com.tangosol.net.Invocable
        public void run()
            {
            Connector conn = (Connector) getService().getUserContext();
            conn.onPublish(this);
            }
        
        // Accessor for the property "JMXServiceURL"
        /**
         * Setter for property JMXServiceURL.<p>
        * The JMXServiceURL of the MBeanConnector.
         */
        public void setJMXServiceURL(javax.management.remote.JMXServiceURL url)
            {
            __m_JMXServiceURL = url;
            }
        
        // Accessor for the property "ListenAddresses"
        /**
         * Setter for property ListenAddresses.<p>
        * For a JMXServiceURL which is bound to the wildcard address this is
        * the expanded set of addresses which that wildcard address maps to.
         */
        public void setListenAddresses(java.util.Collection colAddresses)
            {
            __m_ListenAddresses = colAddresses;
            }
        
        // Accessor for the property "Service"
        /**
         * Setter for property Service.<p>
        * The Management Service.
         */
        protected void setService(com.tangosol.net.InvocationService service)
            {
            __m_Service = service;
            }
        
        // From interface: com.tangosol.io.ExternalizableLite
        public void writeExternal(java.io.DataOutput out)
                throws java.io.IOException
            {
            // import com.tangosol.util.ExternalizableHelper;
            
            ExternalizableHelper.writeObject(out, getJMXServiceURL());
            ExternalizableHelper.writeCollection(out, getListenAddresses());
            }
        }

    // ---- class: com.tangosol.coherence.component.net.management.Connector$Register
    
    /**
     * Register task.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class Register
            extends    com.tangosol.coherence.component.Net
            implements com.tangosol.io.ExternalizableLite,
                       com.tangosol.net.Invocable
        {
        // ---- Fields declarations ----
        
        /**
         * Property MemberFrom
         *
         * The originator's member id.
         */
        private transient int __m_MemberFrom;
        
        /**
         * Property Result
         *
         * The result of invocation.
         */
        private transient Object __m_Result;
        
        /**
         * Property Service
         *
         * The Management Service.
         */
        private transient com.tangosol.net.InvocationService __m_Service;
        
        /**
         * Property Snapshot
         *
         * An array of models' snapshots.
         */
        private com.tangosol.coherence.component.net.management.model.LocalModel[] __m_Snapshot;
        
        // Default constructor
        public Register()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public Register(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.net.management.Connector.Register();
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
                clz = Class.forName("com.tangosol.coherence/component/net/management/Connector$Register".replace('/', '.'));
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
        
        // Accessor for the property "MemberFrom"
        /**
         * Getter for property MemberFrom.<p>
        * The originator's member id.
         */
        public int getMemberFrom()
            {
            return __m_MemberFrom;
            }
        
        // From interface: com.tangosol.net.Invocable
        // Accessor for the property "Result"
        /**
         * Getter for property Result.<p>
        * The result of invocation.
         */
        public Object getResult()
            {
            return __m_Result;
            }
        
        // Accessor for the property "Service"
        /**
         * Getter for property Service.<p>
        * The Management Service.
         */
        public com.tangosol.net.InvocationService getService()
            {
            return __m_Service;
            }
        
        // Accessor for the property "Snapshot"
        /**
         * Getter for property Snapshot.<p>
        * An array of models' snapshots.
         */
        public com.tangosol.coherence.component.net.management.model.LocalModel[] getSnapshot()
            {
            return __m_Snapshot;
            }
        
        // From interface: com.tangosol.net.Invocable
        public void init(com.tangosol.net.InvocationService service)
            {
            setService(service);
            }
        
        // From interface: com.tangosol.io.ExternalizableLite
        public void readExternal(java.io.DataInput in)
                throws java.io.IOException
            {
            // import com.tangosol.util.ExternalizableHelper;
            
            setMemberFrom(ExternalizableHelper.readInt(in));
            
            setSnapshot(readLocalModelArray(in));
            }
        
        /**
         * Read an array of LocalModel instances with length larger than {@link
        * #CHUNK_THRESHOLD} {@literal >>} 4.
        * 
        * @param in             a DataInput stream to read from
        * @param cLength  length to read
        * 
        * @return an array of LocalModel instances
        * 
        * @throws IOException  if an I/O exception occurs
        * 
        * @since 22.09
         */
        private com.tangosol.coherence.component.net.management.model.LocalModel[] readLargeLocalModelArray(java.io.DataInput in, int cLength)
                throws java.io.IOException
            {
            // import com.tangosol.util.ExternalizableHelper;
            
            int          cBatchMax = ExternalizableHelper.CHUNK_SIZE >> 4;
            int          cBatch    = cLength / cBatchMax + 1;
            LocalModel[] aMerged   = null;
            int          cRead     = 0;
            int          cAllocate = cBatchMax;
            
            LocalModel[] ao;
            for (int i = 0; i < cBatch && cRead < cLength; i++)
                {
                ao      = readLocalModelArray(in, cAllocate);
                aMerged = (LocalModel[]) ExternalizableHelper.mergeArray(aMerged, ao);
                cRead  += ao.length;
            
                cAllocate = Math.min(cLength - cRead, cBatchMax);
                }
            
            return aMerged;
            }
        
        /**
         * Read an array of LocalModel instances from a DataInput stream.
        * 
        * @param in  a DataInput stream to read from
        * 
        * @return an array of LocalModel instances
        * 
        * @throws IOException  if an I/O exception occurs
        * 
        * @since 22.09
         */
        private com.tangosol.coherence.component.net.management.model.LocalModel[] readLocalModelArray(java.io.DataInput in)
                throws java.io.IOException
            {
            // import com.tangosol.util.ExternalizableHelper;
            
            int c = ExternalizableHelper.readInt(in);
            
            // JEP-290 - ensure we can allocate this array
            ExternalizableHelper.validateLoadArray(Object[].class, c, in);
            
            return c <= 0
                       ? new LocalModel[0]
                       : c < ExternalizableHelper.CHUNK_THRESHOLD >> 4
                           ? readLocalModelArray(in, c)
                           : readLargeLocalModelArray(in, c);
            }
        
        /**
         * Read an array of the specified number of LocalModel instances from a
        * DataInput stream.
        * 
        * @param in             a DataInput stream to read from
        * @param cLength  length to read
        *   
        * @return an array of LocalModel instances
        * 
        * @throws IOException  if an I/O exception occurs
        * 
        * @since 22.09
        *   
         */
        private com.tangosol.coherence.component.net.management.model.LocalModel[] readLocalModelArray(java.io.DataInput in, int cLength)
                throws java.io.IOException
            {
            // import com.tangosol.util.ExternalizableHelper;
            // import javax.management.MBeanInfo;
            
            LocalModel[] aModel = new LocalModel[cLength];
            ClassLoader  loader = getClass().getClassLoader();
            
            for (int i = 0; i < cLength; i++)
                {
                LocalModel model = aModel[i] =
                    (LocalModel) ExternalizableHelper.readObject(in, loader);
            
                if (model instanceof WrapperModel)
                    {
                    MBeanInfo mbeanInfo = (MBeanInfo) ExternalizableHelper.readObject(in, loader); 
                    ((WrapperModel) model).setMBeanInfo(mbeanInfo);
                    }
                }
            
            return aModel;
            }
        
        // From interface: com.tangosol.net.Invocable
        public void run()
            {
            Connector conn = (Connector) getService().getUserContext();
            conn.onRegister(this);
            }
        
        // Accessor for the property "MemberFrom"
        /**
         * Setter for property MemberFrom.<p>
        * The originator's member id.
         */
        public void setMemberFrom(int nId)
            {
            __m_MemberFrom = nId;
            }
        
        // Accessor for the property "Result"
        /**
         * Setter for property Result.<p>
        * The result of invocation.
         */
        public void setResult(Object oResult)
            {
            __m_Result = oResult;
            }
        
        // Accessor for the property "Service"
        /**
         * Setter for property Service.<p>
        * The Management Service.
         */
        protected void setService(com.tangosol.net.InvocationService service)
            {
            __m_Service = service;
            }
        
        // Accessor for the property "Snapshot"
        /**
         * Setter for property Snapshot.<p>
        * An array of models' snapshots.
         */
        public void setSnapshot(com.tangosol.coherence.component.net.management.model.LocalModel[] aModel)
            {
            __m_Snapshot = aModel;
            }
        
        // From interface: com.tangosol.io.ExternalizableLite
        public void writeExternal(java.io.DataOutput out)
                throws java.io.IOException
            {
            // import Component.Net.Management.Model.LocalModel;
            // import Component.Net.Management.Model.LocalModel.WrapperModel;
            // import com.tangosol.util.ExternalizableHelper;
            
            ExternalizableHelper.writeInt(out, getMemberFrom());
            
            LocalModel[] aModel = getSnapshot();
            int          c      = aModel.length;
            
            ExternalizableHelper.writeInt(out, c);
            for (int i = 0; i < c; i++)
                {
                LocalModel model = aModel[i];
            
                ExternalizableHelper.writeObject(out, model);
            
                if (model instanceof WrapperModel)
                    {
                    ExternalizableHelper.writeObject(out,
                        ((WrapperModel) model).getMBeanInfo());
                    }
                }
            }
        }

    // ---- class: com.tangosol.coherence.component.net.management.Connector$Subscribe
    
    /**
     * Subscribe/unsubscribe task.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class Subscribe
            extends    com.tangosol.coherence.component.Net
            implements com.tangosol.io.ExternalizableLite,
                       com.tangosol.net.Invocable
        {
        // ---- Fields declarations ----
        
        /**
         * Property Action
         *
         * One of ACTION_* constants.
         */
        private transient int __m_Action;
        
        /**
         * Property ACTION_SUBSCRIBE
         *
         * Subscribe action.
         */
        public static final int ACTION_SUBSCRIBE = 0;
        
        /**
         * Property ACTION_UNSUBSCRIBE
         *
         * Unsubscribe action.
         */
        public static final int ACTION_UNSUBSCRIBE = 1;
        
        /**
         * Property Holder
         *
         * The RemoteHolder used by the "register" action.
         */
        private transient com.tangosol.coherence.component.net.management.listenerHolder.RemoteHolder __m_Holder;
        
        /**
         * Property HolderId
         *
         * An array of HolderId used by the "unregister" action.
         */
        private long[] __m_HolderId;
        
        /**
         * Property MemberId
         *
         * The subscriber's Member id.
         */
        private int __m_MemberId;
        
        /**
         * Property Name
         *
         * The name of the MBean.
         */
        private String __m_Name;
        
        /**
         * Property Service
         *
         * The Management Service.
         */
        private com.tangosol.net.InvocationService __m_Service;
        
        // Default constructor
        public Subscribe()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public Subscribe(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.net.management.Connector.Subscribe();
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
                clz = Class.forName("com.tangosol.coherence/component/net/management/Connector$Subscribe".replace('/', '.'));
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
        
        // Accessor for the property "Action"
        /**
         * Getter for property Action.<p>
        * One of ACTION_* constants.
         */
        public int getAction()
            {
            return __m_Action;
            }
        
        // Accessor for the property "Holder"
        /**
         * Getter for property Holder.<p>
        * The RemoteHolder used by the "register" action.
         */
        public com.tangosol.coherence.component.net.management.listenerHolder.RemoteHolder getHolder()
            {
            return __m_Holder;
            }
        
        // Accessor for the property "HolderId"
        /**
         * Getter for property HolderId.<p>
        * An array of HolderId used by the "unregister" action.
         */
        public long[] getHolderId()
            {
            return __m_HolderId;
            }
        
        // Accessor for the property "MemberId"
        /**
         * Getter for property MemberId.<p>
        * The subscriber's Member id.
         */
        public int getMemberId()
            {
            return __m_MemberId;
            }
        
        // Accessor for the property "Name"
        /**
         * Getter for property Name.<p>
        * The name of the MBean.
         */
        public String getName()
            {
            return __m_Name;
            }
        
        // From interface: com.tangosol.net.Invocable
        public Object getResult()
            {
            return null;
            }
        
        // Accessor for the property "Service"
        /**
         * Getter for property Service.<p>
        * The Management Service.
         */
        public com.tangosol.net.InvocationService getService()
            {
            return __m_Service;
            }
        
        // From interface: com.tangosol.net.Invocable
        public void init(com.tangosol.net.InvocationService service)
            {
            setService(service);
            }
        
        // From interface: com.tangosol.io.ExternalizableLite
        public void readExternal(java.io.DataInput in)
                throws java.io.IOException
            {
            // import Component.Net.Management.ListenerHolder.RemoteHolder;
            // import com.tangosol.util.ExternalizableHelper;
            
            int nAction = ExternalizableHelper.readInt(in);
            
            setAction(nAction);
            setName(ExternalizableHelper.readUTF(in));
            
            if (nAction == ACTION_SUBSCRIBE)
                {
                RemoteHolder holder = new RemoteHolder();
            
                holder.readExternal(in);
                setHolder(holder);
                }
            else
                {
                setMemberId(ExternalizableHelper.readInt(in));
                setHolderId(readLongArray(in));
                }
            }
        
        /**
         * Read an array of longs with length larger than {@link
        * #CHUNK_THRESHOLD} {@literal >>} 4.
        * 
        * @param in             a DataInput stream to read from
        * @param cLength  length to read
        * 
        * @return an array of longs
        * 
        * @throws IOException  if an I/O exception occurs
        * 
        * @since 22.09
         */
        private static long[] readLargeLongArray(java.io.DataInput in, int cLength)
                throws java.io.IOException
            {
            // import com.tangosol.util.ExternalizableHelper;
            
            int    cBatchMax = ExternalizableHelper.CHUNK_SIZE >> 3;
            int    cBatch    = cLength / cBatchMax + 1;
            long[] aMerged   = null;
            int    cRead     = 0;
            int    cAllocate = cBatchMax;
            long[] al;
            
            for (int i = 0; i < cBatch && cRead < cLength; i++)
                {
                al      = readLongArray(in, cAllocate);
                aMerged = ExternalizableHelper.mergeLongArray(aMerged, al);
                cRead  += al.length;
                cAllocate = Math.min(cLength - cRead, cBatchMax);
                }
            
            return aMerged;
            }
        
        /**
         * Read an array of long numbers using the provided DataInput stream.
        * 
        * @param in  a DataInput stream to read from
        * 
        * @return an array of longs
        * 
        * @throws IOException  if an I/O exception occurs
        * 
        * @since 22.09
         */
        private static long[] readLongArray(java.io.DataInput in)
                throws java.io.IOException
            {
            // import com.tangosol.util.ExternalizableHelper;
            
            int c = ExternalizableHelper.readInt(in);
            
            // JEP-290 - ensure we can allocate this array
            ExternalizableHelper.validateLoadArray(long[].class, c, in);
            
            return c <= 0
                       ? new long[0]
                       : c < ExternalizableHelper.CHUNK_THRESHOLD >> 3
                           ? readLongArray(in, c)
                           : readLargeLongArray(in, c);
            }
        
        /**
         * Read an array of the specified number of longs from using the
        * provided DataInput stream.
        * 
        * @param in             a DataInput stream to read from
        * @param cLength  length to read
        * 
        * @return an array of longs
        * 
        * @throws IOException  if an I/O exception occurs
        * 
        * @since 22.09
         */
        private static long[] readLongArray(java.io.DataInput in, int cLength)
                throws java.io.IOException
            {
            long[] al = new long[cLength];
            
            for (int i = 0; i < cLength; i++)
                {
                al[i] = ExternalizableHelper.readLong(in);
                }
            
            return al;
            }
        
        // From interface: com.tangosol.net.Invocable
        public void run()
            {
            Connector conn = (Connector) getService().getUserContext();
            conn.onSubscribe(this);
            }
        
        // Accessor for the property "Action"
        /**
         * Setter for property Action.<p>
        * One of ACTION_* constants.
         */
        public void setAction(int nAction)
            {
            __m_Action = nAction;
            }
        
        // Accessor for the property "Holder"
        /**
         * Setter for property Holder.<p>
        * The RemoteHolder used by the "register" action.
         */
        public void setHolder(com.tangosol.coherence.component.net.management.listenerHolder.RemoteHolder holder)
            {
            __m_Holder = holder;
            }
        
        // Accessor for the property "HolderId"
        /**
         * Setter for property HolderId.<p>
        * An array of HolderId used by the "unregister" action.
         */
        public void setHolderId(long[] aId)
            {
            __m_HolderId = aId;
            }
        
        // Accessor for the property "MemberId"
        /**
         * Setter for property MemberId.<p>
        * The subscriber's Member id.
         */
        public void setMemberId(int nId)
            {
            __m_MemberId = nId;
            }
        
        // Accessor for the property "Name"
        /**
         * Setter for property Name.<p>
        * The name of the MBean.
         */
        public void setName(String sName)
            {
            __m_Name = sName;
            }
        
        // Accessor for the property "Service"
        /**
         * Setter for property Service.<p>
        * The Management Service.
         */
        protected void setService(com.tangosol.net.InvocationService service)
            {
            __m_Service = service;
            }
        
        // Declared at the super level
        public String toString()
            {
            int nAction = getAction();
            return (nAction == ACTION_SUBSCRIBE  ? "Subscribe" : "Unsubcribe")
                +  " to " + getName();
            }
        
        // From interface: com.tangosol.io.ExternalizableLite
        public void writeExternal(java.io.DataOutput out)
                throws java.io.IOException
            {
            // import com.tangosol.util.ExternalizableHelper;
            
            int nAction = getAction();
            
            ExternalizableHelper.writeInt(out, nAction);
            ExternalizableHelper.writeUTF(out, getName());
            
            if (nAction == ACTION_SUBSCRIBE)
                {
                getHolder().writeExternal(out);
                }
            else
                {
                ExternalizableHelper.writeInt(out, getMemberId());
            
                long[] alId = getHolderId();
                int    c    = alId == null ? 0 : alId.length;
            
                ExternalizableHelper.writeInt(out, c);
                for (int i = 0; i < c; i++)
                    {
                    ExternalizableHelper.writeLong(out, alId[i]);
                    }
                }
            }
        }

    // ---- class: com.tangosol.coherence.component.net.management.Connector$Unregister
    
    /**
     * Unregister task.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class Unregister
            extends    com.tangosol.coherence.component.Net
            implements com.tangosol.io.ExternalizableLite,
                       com.tangosol.net.Invocable
        {
        // ---- Fields declarations ----
        
        /**
         * Property MemberFrom
         *
         * The originator's member id.
         */
        private transient int __m_MemberFrom;
        
        /**
         * Property Name
         *
         * The model's name.
         */
        private String __m_Name;
        
        /**
         * Property Result
         *
         * The result of invocation.
         */
        private transient Object __m_Result;
        
        /**
         * Property Service
         *
         * The Management Service.
         */
        private transient com.tangosol.net.InvocationService __m_Service;
        
        // Default constructor
        public Unregister()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public Unregister(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.net.management.Connector.Unregister();
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
                clz = Class.forName("com.tangosol.coherence/component/net/management/Connector$Unregister".replace('/', '.'));
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
        
        // Accessor for the property "MemberFrom"
        /**
         * Getter for property MemberFrom.<p>
        * The originator's member id.
         */
        public int getMemberFrom()
            {
            return __m_MemberFrom;
            }
        
        // Accessor for the property "Name"
        /**
         * Getter for property Name.<p>
        * The model's name.
         */
        public String getName()
            {
            return __m_Name;
            }
        
        // From interface: com.tangosol.net.Invocable
        // Accessor for the property "Result"
        /**
         * Getter for property Result.<p>
        * The result of invocation.
         */
        public Object getResult()
            {
            return __m_Result;
            }
        
        // Accessor for the property "Service"
        /**
         * Getter for property Service.<p>
        * The Management Service.
         */
        public com.tangosol.net.InvocationService getService()
            {
            return __m_Service;
            }
        
        // From interface: com.tangosol.net.Invocable
        public void init(com.tangosol.net.InvocationService service)
            {
            setService(service);
            }
        
        // From interface: com.tangosol.io.ExternalizableLite
        public void readExternal(java.io.DataInput in)
                throws java.io.IOException
            {
            // import com.tangosol.util.ExternalizableHelper;
            
            setMemberFrom(ExternalizableHelper.readInt(in));
            setName(ExternalizableHelper.readSafeUTF(in));
            }
        
        // From interface: com.tangosol.net.Invocable
        public void run()
            {
            Connector conn = (Connector) getService().getUserContext();
            conn.onUnregister(this);
            }
        
        // Accessor for the property "MemberFrom"
        /**
         * Setter for property MemberFrom.<p>
        * The originator's member id.
         */
        public void setMemberFrom(int nId)
            {
            __m_MemberFrom = nId;
            }
        
        // Accessor for the property "Name"
        /**
         * Setter for property Name.<p>
        * The model's name.
         */
        public void setName(String sName)
            {
            __m_Name = sName;
            }
        
        // Accessor for the property "Result"
        /**
         * Setter for property Result.<p>
        * The result of invocation.
         */
        public void setResult(Object oResult)
            {
            __m_Result = oResult;
            }
        
        // Accessor for the property "Service"
        /**
         * Setter for property Service.<p>
        * The Management Service.
         */
        protected void setService(com.tangosol.net.InvocationService service)
            {
            __m_Service = service;
            }
        
        // From interface: com.tangosol.io.ExternalizableLite
        public void writeExternal(java.io.DataOutput out)
                throws java.io.IOException
            {
            // import com.tangosol.util.ExternalizableHelper;
            
            ExternalizableHelper.writeInt(out, getMemberFrom());
            ExternalizableHelper.writeSafeUTF(out, getName());
            }
        }
    }
