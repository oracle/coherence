
/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.management.Gateway

package com.tangosol.coherence.component.net.management;

import com.tangosol.coherence.component.application.console.Coherence;
import com.tangosol.coherence.component.manageable.ModelAdapter;
import com.tangosol.coherence.component.net.management.Connector;
import com.tangosol.coherence.component.net.management.model.LocalModel;
import com.tangosol.coherence.component.net.management.model.localModel.CacheModel;
import com.tangosol.coherence.component.net.management.model.localModel.ClusterModel;
import com.tangosol.coherence.component.net.management.model.localModel.ClusterNodeModel;
import com.tangosol.coherence.component.net.management.model.localModel.ConnectionManagerModel;
import com.tangosol.coherence.component.net.management.model.localModel.ConnectionModel;
import com.tangosol.coherence.component.net.management.model.localModel.ManagementModel;
import com.tangosol.coherence.component.net.management.model.localModel.PointToPointModel;
import com.tangosol.coherence.component.net.management.model.localModel.ReporterModel;
import com.tangosol.coherence.component.net.management.model.localModel.ServiceModel;
import com.tangosol.coherence.component.net.management.model.localModel.StorageManagerModel;
import com.tangosol.coherence.component.net.management.model.localModel.WrapperModel;
import com.tangosol.coherence.component.net.management.model.localModel.wrapperModel.WrapperJmxModel;
import com.tangosol.coherence.component.net.memberSet.actualMemberSet.serviceMemberSet.MasterMemberSet;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.Acceptor;
import com.oracle.coherence.common.base.Logger;
import com.tangosol.coherence.config.scheme.ServiceScheme;
import com.tangosol.coherence.reporter.ReportBatch;
import com.tangosol.coherence.reporter.ReportControl;
import com.tangosol.internal.health.HealthCheckWrapper;
import com.tangosol.internal.health.HealthCheckWrapperMBean;
import com.tangosol.internal.net.management.CustomMBeanDependencies;
import com.tangosol.internal.net.management.DefaultGatewayDependencies;
import com.tangosol.internal.net.management.GatewayDependencies;
import com.tangosol.net.Cluster;
import com.tangosol.net.Member;
import com.tangosol.net.Service;
import com.tangosol.net.cache.LocalCache;
import com.tangosol.net.management.MBeanHelper;
import com.tangosol.net.management.MBeanReference;
import com.tangosol.net.management.Registry;
import com.tangosol.net.security.LocalPermission;
import com.tangosol.util.Base;
import com.tangosol.util.ClassHelper;
import com.tangosol.util.Filter;
import com.tangosol.util.HealthCheck;
import com.tangosol.util.ImmutableArrayList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.management.AttributeNotFoundException;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

/**
 * The base component for the Coherence Management framework implementation.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class Gateway
        extends    com.tangosol.coherence.component.net.Management
        implements com.tangosol.net.management.MBeanServerProxy,
                   com.tangosol.net.management.NotificationManager,
                   com.tangosol.net.management.Registry
    {
    // ---- Fields declarations ----
    
    /**
     * Property Cluster
     *
     * Cluster this management gateway is associated with.
     */
    private com.tangosol.coherence.component.util.SafeCluster __m_Cluster;
    
    /**
     * Property CustomBeans
     *
     * A map that contains pre-configured custom mbeans with extended life
     * cycle keyed by their pre-configured names (without the "nodeId"
     * attribute). More specifically, the mbeans kept in this map will
     * "survive" an abnormal cluster node termination and will not be
     * instantiated again upon a restart due to the fact that the Gateway
     * component itself is held by the Util.SafeCluster rather than the "real"
     * Net.Cluster component.
     */
    private transient java.util.Map __m_CustomBeans;
    
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
    private com.tangosol.internal.net.management.GatewayDependencies __m_Dependencies;
    
    /**
     * Property DomainName
     *
     * Domain name for managed beans. For Local gateway the value is
     * "Coherence" (or "Coherence@NNN" in a case of a name conflict when
     * multiple clusters  run within the same JVM). 
     * For Remote gateway the value is an empty string.
     */
    private String __m_DomainName;
    
    /**
     * Property EXECUTE_PERMISSION
     *
     */
    public static final com.tangosol.net.security.LocalPermission EXECUTE_PERMISSION;
    
    /**
     * Property Filter
     *
     * The Filter used to evaluate whether or not to register a model with the
     * specified name.
     */
    private com.tangosol.util.Filter __m_Filter;
    
    /**
     * Property LocalModels
     *
     * A Map<String, LocalModel> of LocalModel components registered with this
     * gateway. The key is the MBean key property string (fully qualified name
     * sans the domain).
     */
    private java.util.Map __m_LocalModels;
    
    /**
     * Property MetricSupport
     *
     */
    private com.tangosol.internal.metrics.MetricSupport __m_MetricSupport;
    
    /**
     * Property ObjectNameCache
     *
     * Cache of ObjectName objects keyed by the canonical names.
     * 
     * @volatile
     */
    private volatile transient java.util.Map __m_ObjectNameCache;
    
    /**
     * Property Primary
     *
     * Specifies if this Gateway is the "primary" gateway (i.e. returned from
     * the Cluster.getManagement() method).
     *  
     */
    private boolean __m_Primary;
    
    /**
     * Property RegisteredHealthChecks
     *
     */
    private java.util.Map __m_RegisteredHealthChecks;
    
    /**
     * Property ReportControl
     *
     * The Reporter for the Gateway.
     */
    private transient com.tangosol.coherence.reporter.ReportControl __m_ReportControl;
    
    private static void _initStatic$Default()
        {
        }
    
    // Static initializer (from _initStatic)
    static
        {
        // import com.tangosol.net.security.LocalPermission;
        
        _initStatic$Default();
        
        EXECUTE_PERMISSION = new LocalPermission("MBeanServerProxy.execute");
        }
    
    // Default constructor
    public Gateway()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public Gateway(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            setCustomBeans(new com.tangosol.util.SafeHashMap());
            setDomainName("");
            setLocalModels(new com.tangosol.util.SafeHashMap());
            setMetricSupport(new com.tangosol.internal.metrics.MetricSupport());
            setPrimary(false);
            setRegisteredHealthChecks(new com.tangosol.util.SafeHashMap());
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
        return new com.tangosol.coherence.component.net.management.Gateway();
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
            clz = Class.forName("com.tangosol.coherence/component/net/management/Gateway".replace('/', '.'));
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
    
    // From interface: com.tangosol.net.management.MBeanServerProxy
    public void addNotificationListener(String sName, javax.management.NotificationListener listener, javax.management.NotificationFilter filter, Object oHandback)
        {
        throw new UnsupportedOperationException();
        }
    
    // From interface: com.tangosol.net.management.Registry
    public boolean allHealthChecksLive()
        {
        // import com.oracle.coherence.common.base.Logger;
        // import com.tangosol.util.HealthCheck;
        // import java.util.Map;
        
        Map map = getRegisteredHealthChecks();
        if (map.isEmpty())
            {
            // there are no health checks, so we're not live
            Logger.fine("Health: Liveness check failed, no health checks registered");
            return false;
            }
        
        Iterator it  = map.values().iterator();
        boolean  fOK = true;
        
        while (it.hasNext())
            {
            HealthCheck check = (HealthCheck) it.next();
            if (check.isMemberHealthCheck())
                {
                if (!check.isLive())
                    {
                    Logger.fine("Health: Liveness check failed for " + check.getName());
                    fOK = false;
                    }
                }
            }
        
        return fOK;
        }
    
    // From interface: com.tangosol.net.management.Registry
    public boolean allHealthChecksReady()
        {
        // import com.oracle.coherence.common.base.Logger;
        // import com.tangosol.util.HealthCheck;
        // import java.util.Map;
        
        Map map = getRegisteredHealthChecks();
        if (map.isEmpty())
            {
            // there are no health checks, so we're not live
            Logger.fine("Health: Readiness check failed, no health checks registered");
            return false;
            }
        
        Iterator it  = map.values().iterator();
        boolean  fOK = true;
        
        while (it.hasNext())
            {
            HealthCheck check = (HealthCheck) it.next();
            if (check.isMemberHealthCheck())
                {
                if (!check.isReady())
                    {
                    Logger.fine("Health: Readiness check failed for " + check.getName());
                    fOK = false;
                    }
                }
            }
        
        return fOK;
        }
    
    // From interface: com.tangosol.net.management.Registry
    public boolean allHealthChecksSafe()
        {
        // import com.oracle.coherence.common.base.Logger;
        // import com.tangosol.util.HealthCheck;
        // import java.util.Map;
        
        Map map = getRegisteredHealthChecks();
        if (map.isEmpty())
            {
            // there are no health checks, so we're not live
            Logger.fine("Health: Safety check failed, no health checks registered");
            return false;
            }
        
        Iterator it  = map.values().iterator();
        boolean  fOK = true;
        
        while (it.hasNext())
            {
            HealthCheck check = (HealthCheck) it.next();
            if (check.isMemberHealthCheck())
                {
                if (!check.isSafe())
                    {
                    Logger.fine("Health: Safety check failed for " + check.getName());
                    fOK = false;
                    }
                }
            }
        
        return fOK;
        }
    
    // From interface: com.tangosol.net.management.Registry
    public boolean allHealthChecksStarted()
        {
        // import com.oracle.coherence.common.base.Logger;
        // import com.tangosol.util.HealthCheck;
        // import java.util.Map;
        
        Map map = getRegisteredHealthChecks();
        if (map.isEmpty())
            {
            // there are no health checks, so we're not live
            Logger.fine("Health: Started check failed, no health checks registered");
            return false;
            }
        
        Iterator it  = map.values().iterator();
        boolean  fOK = true;
        
        while (it.hasNext())
            {
            HealthCheck check = (HealthCheck) it.next();
            if (check.isMemberHealthCheck())
                {
                if (!check.isStarted())
                    {
                    Logger.fine("Health: Started check failed for " + check.getName());
                    fOK = false;
                    }
                }
            }
        
        return fOK;
        }
    
    /**
     * Create a new Default dependencies object by cloning the input
    * dependencies.  Each class or component that uses dependencies implements
    * a Default dependencies class which provides the clone functionality.  
    * The dependency injection design pattern requires every component in the
    * component hierarchy to implement clone.
    * 
    * @return DefaultGatewayDependencies  the cloned dependencies
     */
    public com.tangosol.internal.net.management.DefaultGatewayDependencies cloneDependencies(com.tangosol.internal.net.management.GatewayDependencies deps)
        {
        // import com.tangosol.internal.net.management.DefaultGatewayDependencies;
        
        return new DefaultGatewayDependencies(deps);
        }
    
    /**
     * Encode the string to allow the string to be a JMX object name.
     */
    protected String convertSpecialCharacters(String s)
        {
        return s.replace(':', '_');
        }
    
    /**
     * Create the Management gateway, associate with the specified Cluster
    * object and bind it to a MBeanServer.
    * 
    * There are seven different ways  the Management topology could be
    * configured:
    * 
    * 1) Cluster -> null
    * 2) Cluster -> Local -> null
    * 3) Cluster -> Local -> Remote -> Connector -> null
    * 4) Cluster -> Local -> Remote -> Connector -> Local
    * 5) Cluster -> Local -> Mock -> Connector -> Local
    * 6) Cluster -> Remote -> Connector -> null
    * 7) Cluster -> Mock -> Connector -> Local
    * 8) Same as (6), but may transition to (4) for the senior
    * 
    * X = "managed-nodes"
    * Y = "allow-remote-management"
    * 
    *  X>    "none", "local-only", "remote-only", "all", "dynamic"
    *  Y
    *  V             
    * true    6)         3)                   N/A              4)            8)
    * false   1)         2)                   7)                  5)           
    * N/A
     */
    public static Gateway createGateway(com.tangosol.internal.net.management.GatewayDependencies deps, com.tangosol.coherence.component.util.SafeCluster cluster)
        {
        // import Component.Application.Console.Coherence;
        // import Component.Net.Member as com.tangosol.coherence.component.net.Member;
        // import Component.Net.Management.Connector;
        // import Component.Net.Management.Gateway.Local as com.tangosol.coherence.component.net.management.gateway.Local;
        // import Component.Net.Management.Gateway.Mock as com.tangosol.coherence.component.net.management.gateway.Mock;
        // import Component.Net.Management.Gateway.Remote as com.tangosol.coherence.component.net.management.gateway.Remote;
        
        String  sManage     = deps.getManagedNodes();
        boolean fRemoteMgmt = deps.isAllowRemoteManagement();
        int     nEdition    = ((Coherence) Coherence.get_Instance()).getEdition();
        
        // StandardEdition (a.k.a CachingEdition) does not allow running the InvocationService
        // (see InvocationService#_initStatic)
        if (nEdition == 2)
            {
            String sAction = null;
        
            if (sManage.equals("all"))
                {
                sManage = "local-only";
                sAction = "switching to the \"" + sManage + "\" mode";
                }
            else if (sManage.equals("remote-only") || sManage.equals("dynamic"))
                {
                sManage = "none";
                sAction = "disabling management";
                }
        
            if (fRemoteMgmt)
                {
                fRemoteMgmt = false;
                if (sAction == null)
                    {
                    sAction = "disabling remote management";
                    }
                }
        
            if (sAction != null)
                {
                _trace("Remote management is not supported for " +
                    com.tangosol.coherence.component.net.Member.EDITION_NAME[nEdition] + "; " + sAction, 3);
                }
            }
        
        if (sManage.equals("dynamic") && !fRemoteMgmt)
            {
            _trace("Turning on the \"remote\" mode for dynamic management", 3);
            fRemoteMgmt = true;
            }
        
        if (sManage.equals("none") && !fRemoteMgmt)
            {
            // this node does not have an MBeanServer and is not managed
            // (see doc scenario 1)
            return null;
            }
        
        String  sManageHttp  = deps.getHttpManagedNodes();
        boolean fHttpInherit;
        
        if (sManageHttp == null)
            {
            fHttpInherit = true;
            }
        else
            {
            fHttpInherit = sManageHttp.equals("inherit");
        
            if (sManageHttp.equals("all") && sManage.equals("none"))
                {
                _trace("Disabling management over HTTP as this server is not managed", 3);
                sManageHttp = "none";
                }
            }
        
        com.tangosol.coherence.component.net.management.gateway.Local     gatewayLocal  = null;
        com.tangosol.coherence.component.net.management.gateway.Remote    gatewayRemote = null;
        com.tangosol.coherence.component.net.management.gateway.Mock      gatewayMock   = null;
        Connector connector     = new Connector();
        connector.setManagingNode(sManage.equals("all") || sManage.equals("remote-only"));
        connector.setHttpManagingNode(fHttpInherit ? connector.isManagingNode() : sManageHttp.equals("all"));
        connector.setManagingDynamic(sManage.equals("dynamic"));
        connector.setHttpManagingDynamic(fHttpInherit ? connector.isManagingDynamic() : false);
        connector.setDependencies(deps.getConnectorDependencies());
        
        if (fRemoteMgmt)
            {
            gatewayRemote = com.tangosol.coherence.component.net.management.gateway.Remote.instantiate(cluster, connector, deps);
        
            if (sManage.equals("none") || sManage.equals("dynamic"))
                {
                // (see doc scenario 6)
                gatewayRemote.makePrimary();
                return gatewayRemote;
                }
            }
        
        // this node should have an MBeanServer; com.tangosol.coherence.component.net.management.gateway.Local gateway is always primary
        try
            {
            gatewayLocal = com.tangosol.coherence.component.net.management.gateway.Local.instantiate(cluster, connector, deps);
            }
        catch (Throwable e)
            {
            String sMsg = "This node is not configured for local management; " +
                (fRemoteMgmt ? "it can only be managed remotely."
                             : "it will not be managed");
            _trace(sMsg, 2);
            if (!(e instanceof NoClassDefFoundError))
                {
                _trace(e);
                }
            gatewayRemote.makePrimary();
            return gatewayRemote;
            } 
        
        if (fRemoteMgmt)
            {
            // local MBeans will be registered with a remote MBean server
            // (see doc scenario 3, 4)
            gatewayLocal.setRemoteGateway(gatewayRemote);
            }
        
        if (sManage.equals("local-only"))
            {
            // only local MBeans will be registered with the local MBean server
            // (see doc scenario 2)
            return gatewayLocal;
            }
        
        boolean fRemoteOnly = sManage.equals("remote-only");
        if (!fRemoteOnly)
            {
            _assert(sManage.equals("all"), 
                "Unsupported \"managed-nodes\" value: " + sManage);
            }
        
        connector.setLocalGateway(gatewayLocal);
        
        if (fRemoteMgmt)
            {
            // (see doc scenario 4 and NA)
            if (fRemoteOnly)
                {
                _trace("The 'managed-nodes' value of 'remote-only' is incompatible " +
                       "with 'allow-remote-management' value of 'true'; " +
                       "using the 'managed-nodes' value of 'all' instead", 2);
                }
            return gatewayLocal;
            }
        
        gatewayMock = new com.tangosol.coherence.component.net.management.gateway.Mock();
        gatewayMock.setConnector(connector);
        gatewayMock.setDependencies(deps);
        gatewayMock.setCluster(cluster);
        
        if (fRemoteOnly)
            {
            // local MBeans will NOT be registered with the MBean server
            // (see doc scenario 7)
            gatewayMock.makePrimary();
            return gatewayMock;
            }
        else
            {
            // both local and remote MBeans will be registered with the MBean server
            // (see doc scenario 5)
            gatewayLocal.setRemoteGateway(gatewayMock);
            return gatewayLocal;
            }
        }
    
    // From interface: com.tangosol.net.management.Registry
    /**
     * @inheritDoc
     */
    public String ensureGlobalName(String sName)
        {
        // import com.tangosol.net.Member;
        
        Member member = getCluster().getLocalMember();
        
        return member == null || sName.isEmpty()
            ? sName
            : ensureGlobalName(sName, member);
        }
    
    // From interface: com.tangosol.net.management.Registry
    public String ensureGlobalName(String sName, com.tangosol.net.Member member)
        {
        // import com.tangosol.net.management.MBeanHelper;
        // import com.tangosol.net.management.Registry;
        
        // in case of Responsibility MBeans, only the cluster name
        // key property should be added to the MBean name
        
        boolean fWildcard = false;
        
        if (sName.length() == 1 && sName.charAt(0) == '*')
            {
            fWildcard = true;
            sName      = "";
            }
        else if (sName.endsWith(",*"))
            {
            fWildcard = true;
            sName = sName.substring(0, sName.length() - 2);
            }
        
        StringBuilder sb  = new StringBuilder(sName);
        boolean fExtended = getDependencies().isExtendedMBeanName();
        
        if (fExtended)
            {
            sb.append(',')
              .append(Registry.KEY_CLUSTER).append(member.getClusterName());
            }
        
        if (MBeanHelper.isNonMemberMBean(sName))
            {
            if (fWildcard)
            {
                sb.append(",*");
                }
            return sb.toString();
            }
        
        if (fExtended)
            {
            String sMemberName = member.getMemberName();
            if (sMemberName != null && sMemberName.length() > 0)
                {
                sb.append(',')
                  .append(Registry.KEY_MEMBER).append(sMemberName);
                }
            }
        
        sb.append(',')
                 .append(Registry.KEY_NODE_ID)
          .append(member.getId());
        
        if (fWildcard)
            {
            sb.append(",*");
            }
        
        return sb.toString();
        }
    
    /**
     * Ensure that there exists a LocalModel for the given name.
     */
    protected com.tangosol.coherence.component.net.management.model.LocalModel ensureLocalModel(String sName)
        {
        // import Component.Net.Management.Model.LocalModel;
        // import java.util.Iterator;
        // import java.util.Map;
        
        Map        mapModels = getLocalModels();
        LocalModel model     = (LocalModel) mapModels.get(sName);
        
        if (model == null)
            {
            // it's most likely that the Gateway has just been "reset"
            // due to the Management service departure (see Connector.memberLeft),
            // in which case all the global MBeans are removed (see #reset).
            // If it’s not the case - throw an exception.
            
            for (Iterator iter = mapModels.keySet().iterator(); iter.hasNext();)
                {
                String mName = (String) iter.next();
        
                if (isGlobal(mName))
                    {
                    throw new IllegalStateException("Model not found: " + sName);
                    }
                }
            }
        return model;
        }
    
    /**
     * Ensure initialization of cache of ObjectName objects keyed by the
    * canonical names.
     */
    protected java.util.Map ensureObjectNameCache()
        {
        // import com.tangosol.net.cache.LocalCache;
        // import java.util.Map;
        
        Map mapCache = getObjectNameCache();
        
        if (mapCache == null)
            {
            synchronized (this)
                {
                mapCache = getObjectNameCache();
                if (mapCache == null)
                    {
                    setObjectNameCache(mapCache = new LocalCache(100));
                    }
                }
            }
        
        return mapCache;
        }
    
    /**
     * Convert the passed exception to a RuntimeException if necessary.
     */
    protected RuntimeException ensureRuntimeException(Exception e, String sName, String sAttr)
        {
        // import com.tangosol.util.Base;
        // import javax.management.AttributeNotFoundException;
        // import javax.management.MalformedObjectNameException;
        
        if (e instanceof MalformedObjectNameException)
            {
            return new IllegalArgumentException("Invalid MBean name: " + sName, e);
            }
        
        if (e instanceof AttributeNotFoundException)
            {
            return new IllegalArgumentException(
                "Unknown attribute \"" + sAttr + "\" for MBean " + sName, e);
            }
        
        return Base.ensureRuntimeException(e, "On attribute \"" + sAttr + "\" for MBean " + sName);
        }
    
    // From interface: com.tangosol.net.management.MBeanServerProxy
    public Object execute(com.tangosol.util.function.Remote.Function function)
        {
        // import com.tangosol.util.Base;
        
        SecurityManager security = System.getSecurityManager();
        if (security != null)
            {
            security.checkPermission(EXECUTE_PERMISSION);
            }
        
        return executeInternal(function, /*continuation*/ null);
        }
    
    protected Object executeInternal(com.tangosol.util.function.Remote.Function function, com.oracle.coherence.common.base.Continuation cont)
        {
        return null;
        }
    
    /**
     * Extract a nodeId from the specified MBean name.
     */
    protected static int extractMemberId(String sName)
        {
        // import com.tangosol.util.Base;
        
        StringBuilder sb      = new StringBuilder();
        String[]      asPair  = Base.parseDelimitedString(sName, ',');
        String        sKey    = "nodeId="; // key for the nodeId name
        int           ofValue = sKey.length();
        
        for (int i = 0, c = asPair.length; i < c; i++)
            {
            String  sPair = asPair[i];
        
            if (sPair.startsWith(sKey))
                {
                String sId = sPair.substring(ofValue);
                return Integer.parseInt(sId);
                }
            }
        
        return -1;
        }
    
    /**
     * Extract a tenant name from the service name in the specified MBean name.
     */
    protected static String extractTenantName(String sName)
        {
        // import com.tangosol.coherence.config.scheme.ServiceScheme;
        // import com.tangosol.net.management.Registry;
        // import com.tangosol.util.Base;
        
        if (sName.contains(ServiceScheme.DELIM_DOMAIN_PARTITION))
            {
            StringBuilder sb = new StringBuilder();
        
            // extract the domain-partition key-value pair from the service name;
            // there are two types of MBean names:
            //      type=Service,name=[service-name],...
            // and
            //      type=...,service=[service-name],...
        
            String[] asPair = Base.parseDelimitedString(sName, ',');
            String   sKey   = "service="; // key for the service name
        
            for (int i = 0, c = asPair.length; i < c; i++)
                {
                String  sPair = asPair[i];
                boolean fCopy = true;
        
                if (sPair.equals(Registry.SERVICE_TYPE) ||
                    sPair.equals(Registry.CONNECTION_MANAGER_TYPE))
                    {
                    sKey = "name=";
                    }
                else if (sPair.startsWith(sKey))
                    {
                    // e.g.: "service=tenantName/serviceName"
                    int    ofValue  = sKey.length();
                    int    ofTenant = sPair.indexOf(ServiceScheme.DELIM_DOMAIN_PARTITION);
        
                    if (ofTenant > 0)
                        {
                        String sTenant  = sPair.substring(ofValue, ofTenant);
                        String sService = sPair.substring(ofTenant + 1);
        
                        sb.append("domainPartition=").append(sTenant).append(',')
                          .append(sKey).append(sService);
                        fCopy = false;
                        }
                    }
        
                if (fCopy)
                    {
                    sb.append(sPair);
                    }
        
                sb.append(',');
                }
            return sb.substring(0, sb.length() - 1);
            }
        else
            {
            return sName;
            }
        }
    
    // From interface: com.tangosol.net.management.MBeanServerProxy
    public Object getAttribute(String sName, String sAttr)
        {
        return null;
        }
    
    // From interface: com.tangosol.net.management.MBeanServerProxy
    public java.util.Map getAttributes(String sName, com.tangosol.util.Filter filter)
        {
        return null;
        }
    
    // Accessor for the property "Cluster"
    /**
     * Getter for property Cluster.<p>
    * Cluster this management gateway is associated with.
     */
    public com.tangosol.coherence.component.util.SafeCluster getCluster()
        {
        return __m_Cluster;
        }
    
    // Accessor for the property "CustomBeans"
    /**
     * Getter for property CustomBeans.<p>
    * A map that contains pre-configured custom mbeans with extended life cycle
    * keyed by their pre-configured names (without the "nodeId" attribute).
    * More specifically, the mbeans kept in this map will "survive" an abnormal
    * cluster node termination and will not be instantiated again upon a
    * restart due to the fact that the Gateway component itself is held by the
    * Util.SafeCluster rather than the "real" Net.Cluster component.
     */
    public java.util.Map getCustomBeans()
        {
        return __m_CustomBeans;
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
    public com.tangosol.internal.net.management.GatewayDependencies getDependencies()
        {
        return __m_Dependencies;
        }
    
    // From interface: com.tangosol.net.management.Registry
    // Accessor for the property "DomainName"
    /**
     * @inheritDoc
     */
    public String getDomainName()
        {
        return __m_DomainName;
        }
    
    // Accessor for the property "Filter"
    /**
     * Getter for property Filter.<p>
    * The Filter used to evaluate whether or not to register a model with the
    * specified name.
     */
    public com.tangosol.util.Filter getFilter()
        {
        // import com.tangosol.internal.net.management.GatewayDependencies;
        
        GatewayDependencies dps = getDependencies();
        
        return dps == null ? null : dps.getFilter();
        }
    
    // From interface: com.tangosol.net.management.Registry
    public java.util.Collection getHealthChecks()
        {
        // import java.util.Collections;
        // import java.util.Map;
        
        Map map = getRegisteredHealthChecks();
        return Collections.unmodifiableCollection(map.values());
        }
    
    /**
     * Obtains a local model for the specified name.
     */
    public com.tangosol.coherence.component.net.management.model.LocalModel getLocalModel(String sName)
        {
        // import Component.Net.Management.Model.LocalModel;
        
        return (LocalModel) getLocalModels().get(sName);
        }
    
    // Accessor for the property "LocalModels"
    /**
     * Getter for property LocalModels.<p>
    * A Map<String, LocalModel> of LocalModel components registered with this
    * gateway. The key is the MBean key property string (fully qualified name
    * sans the domain).
     */
    public java.util.Map getLocalModels()
        {
        return __m_LocalModels;
        }
    
    // From interface: com.tangosol.net.management.MBeanServerProxy
    public javax.management.MBeanInfo getMBeanInfo(String sName)
        {
        throw new UnsupportedOperationException();
        }
    
    // From interface: com.tangosol.net.management.Registry
    public com.tangosol.net.management.MBeanServerProxy getMBeanServerProxy()
        {
        if (!isPrimary())
            {
            throw new IllegalStateException();
            }
        return this;
        }
    
    // Accessor for the property "MetricSupport"
    /**
     * Getter for property MetricSupport.<p>
     */
    public com.tangosol.internal.metrics.MetricSupport getMetricSupport()
        {
        return __m_MetricSupport;
        }
    
    // From interface: com.tangosol.net.management.Registry
    /**
     * @inheritDoc
     */
    public com.tangosol.net.management.NotificationManager getNotificationManager()
        {
        if (!isPrimary())
            {
            throw new IllegalStateException();
            }
        return this;
        }
    
    /**
     * Return a cached ObjectName for the provided canonicalized name sName.
     */
    protected javax.management.ObjectName getObjectName(String sName)
            throws javax.management.MalformedObjectNameException
        {
        // import com.tangosol.net.management.MBeanHelper;
        // import javax.management.ObjectName;
        // import java.util.Map;
        
        Map        map  = ensureObjectNameCache();
        ObjectName name = (ObjectName) map.get(sName);
        
        if (name == null)
            {
            String sDomain  = getDomainName();
            int    ofDomain = sName.indexOf(':');
            int    ofEquals = sName.indexOf('=');
        
            if (0 <= ofEquals && ofEquals < ofDomain)
                {
                // this colon is a part of a key-value pair and there is no domain
                ofDomain = -1;
                }
        
            if (ofDomain == -1)
                {
                name = new ObjectName(MBeanHelper.quoteCanonical(sDomain + ':' + sName));
                }
            else if (ofDomain == 0)
                {
                // empty domain name
                name = new ObjectName(MBeanHelper.quoteCanonical(sDomain + sName));
                }
            else
                {
                name = new ObjectName(MBeanHelper.quoteCanonical(sName));
                }
        
            map.put(sName, name);
            }
        
        return name;
        }
    
    // Accessor for the property "ObjectNameCache"
    /**
     * Cache of ObjectName objects keyed by the canonical names.
     */
    public java.util.Map getObjectNameCache()
        {
        return __m_ObjectNameCache;
        }
    
    // Accessor for the property "RegisteredHealthChecks"
    /**
     * Getter for property RegisteredHealthChecks.<p>
     */
    public java.util.Map getRegisteredHealthChecks()
        {
        return __m_RegisteredHealthChecks;
        }
    
    // Accessor for the property "ReportControl"
    /**
     * Getter for property ReportControl.<p>
    * The Reporter for the Gateway.
     */
    public synchronized com.tangosol.coherence.reporter.ReportControl getReportControl()
        {
        // import com.tangosol.coherence.reporter.ReportBatch;
        // import com.tangosol.coherence.reporter.ReportControl;
        
        ReportControl reporter = __m_ReportControl;
        
        if (reporter == null)
            {
            reporter = new ReportBatch();
            reporter.setDependencies(getDependencies().getReporterDependencies());
            setReportControl(reporter);
            }
        
        return reporter;
        }
    
    protected com.tangosol.coherence.component.net.management.model.localModel.CacheModel instantiateCacheModel(java.util.Map map)
        {
        // import Component.Net.Management.Model.LocalModel.CacheModel;
        
        CacheModel model = new CacheModel();
        model.set_Map(map);
        return model;
        }
    
    protected com.tangosol.coherence.component.net.management.model.localModel.ClusterModel instantiateClusterModel()
        {
        // import Component.Net.Management.Model.LocalModel.ClusterModel;
        
        ClusterModel model = new ClusterModel();
        model.set_Cluster(getCluster());
        return model;
        }
    
    protected com.tangosol.coherence.component.net.management.model.localModel.ClusterNodeModel instantiateClusterNodeModel(com.tangosol.net.Member member)
        {
        // import Component.Net.Management.Model.LocalModel.ClusterNodeModel;
        
        ClusterNodeModel model = new ClusterNodeModel();
        model.set_Cluster(getCluster());
        model.set_Member(member);
        return model;
        }
    
    protected com.tangosol.coherence.component.net.management.model.localModel.ConnectionManagerModel instantiateConnectionManagerModel(com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.Acceptor acceptor)
        {
        // import Component.Net.Management.Model.LocalModel.ConnectionManagerModel;
        
        ConnectionManagerModel model = new ConnectionManagerModel();
        model.set_Acceptor(acceptor);
        return model;
        }
    
    protected com.tangosol.coherence.component.net.management.model.localModel.ConnectionModel instantiateConnectionModel(com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.TcpAcceptor.TcpConnection connection)
        {
        // import Component.Net.Management.Model.LocalModel.ConnectionModel;
        
        ConnectionModel model = new ConnectionModel();
        model.set_TcpConnection(connection);
        return model;
        }
    
    protected com.tangosol.coherence.component.net.management.model.LocalModel instantiateLocalModel(Object oBean)
        {
        // import Component.Net.Management.Model.LocalModel;
        // import Component.Net.MemberSet.ActualMemberSet.ServiceMemberSet.MasterMemberSet;
        // import Component.Util.Daemon.QueueProcessor.Service.Peer.Acceptor;
        // import Component.Util.Daemon.QueueProcessor.Service.Peer.Acceptor.TcpAcceptor$TcpConnection as com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.TcpAcceptor.TcpConnection;
        // import Component.Util.Daemon.QueueProcessor.Service.Grid.PartitionedService.PartitionedCache$Storage as com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache.Storage;
        // import com.tangosol.coherence.reporter.ReportControl;
        // import com.tangosol.coherence.transaction.internal.storage.JmxStats as com.tangosol.coherence.transaction.internal.storage.JmxStats;
        // import com.tangosol.net.Cluster;
        // import com.tangosol.net.Service;
        // import com.tangosol.net.management.MBeanReference;
        // import com.tangosol.net.Member;
        // import java.util.Map;
        
        _assert(oBean != null);
        
        LocalModel model;
        if (oBean instanceof Cluster)
            {
            _assert(oBean == getCluster());
            model = instantiateClusterModel();
            }
        else if (oBean instanceof ReportControl)
            {
            model = instantiateReporterModel((ReportControl) oBean);
            }
        else if (oBean instanceof Connector)
            {
            model = instantiateManagementModel((Connector) oBean);
            }
        else if (oBean instanceof Member)
            {
            model = instantiateClusterNodeModel((Member) oBean);
            }
        else if (oBean instanceof Acceptor)
            {
            model = instantiateConnectionManagerModel((Acceptor) oBean);
            }
        else if (oBean instanceof com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.TcpAcceptor.TcpConnection)
            {
            model = instantiateConnectionModel((com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.TcpAcceptor.TcpConnection) oBean);
            }
        else if (oBean instanceof Service)
            {
            model = instantiateServiceModel((Service) oBean);
            }
        else if (oBean instanceof Map)
            {
            model = instantiateCacheModel((Map) oBean);
            }
        else if (oBean instanceof com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.partitionedCache.Storage)
            {
            model = instantiateStorageManagerModel((com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.partitionedCache.Storage) oBean);
            }
        else if (oBean instanceof MasterMemberSet)
            {
            model = instantiatePointToPointModel((MasterMemberSet) oBean);
            }
        else if (oBean instanceof MBeanReference)
            {
            model = instantiateWrapperJmxModel((MBeanReference) oBean);
            }
        else
            {
            model = instantiateWrapperModel(oBean);
            }
        
        if (isReadOnly())
            {
            model.set_ReadOnly(true);
            }
        return model;
        }
    
    protected com.tangosol.coherence.component.net.management.model.localModel.ManagementModel instantiateManagementModel(Connector conn)
        {
        // import Component.Net.Management.Model.LocalModel.ManagementModel;
        
        ManagementModel model = new ManagementModel();
        model.set_Connector(conn);
        return model;
        }
    
    public com.tangosol.coherence.component.manageable.ModelAdapter instantiateModelMBean(Model model)
        {
        // import Component.Manageable.ModelAdapter;
        
        String       sName = model.get_MBeanComponent();
        ModelAdapter mbean = (ModelAdapter) _newInstance(sName);
        
        _assert(mbean != null, "Invalid MBean name: " + sName);
        
        mbean.set_Model(model);
        return mbean;
        }
    
    protected com.tangosol.coherence.component.net.management.model.localModel.PointToPointModel instantiatePointToPointModel(com.tangosol.coherence.component.net.memberSet.actualMemberSet.serviceMemberSet.MasterMemberSet memberset)
        {
        // import Component.Net.Management.Model.LocalModel.PointToPointModel;
        
        PointToPointModel model = new PointToPointModel();
        model.set_MemberSet(memberset);
        return model;
        }
    
    protected com.tangosol.coherence.component.net.management.model.localModel.ReporterModel instantiateReporterModel(com.tangosol.coherence.reporter.ReportControl control)
        {
        // import Component.Net.Management.Model.LocalModel.ReporterModel;
        
        ReporterModel model = new ReporterModel();
        model.set_ReportControl(control);
        return model;
        }
    
    protected com.tangosol.coherence.component.net.management.model.localModel.ServiceModel instantiateServiceModel(com.tangosol.net.Service service)
        {
        // import Component.Net.Management.Model.LocalModel.ServiceModel;
        // import com.tangosol.net.management.Registry;
        
        ServiceModel model = new ServiceModel();
        model.set_Service(service);
        
        if (Registry.SERVICE_NAME.equals(service.getInfo().getServiceName()))
            {
            // the management service itself should not be modifiable
            model.set_ReadOnly(true);
            }
        return model;
        }
    
    protected com.tangosol.coherence.component.net.management.model.localModel.StorageManagerModel instantiateStorageManagerModel(com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.partitionedCache.Storage storage)
        {
        // import Component.Net.Management.Model.LocalModel.StorageManagerModel;
        
        StorageManagerModel model = new StorageManagerModel();
        model.set_Storage(storage);
        return model;
        }

    protected com.tangosol.coherence.component.net.management.model.localModel.wrapperModel.WrapperJmxModel instantiateWrapperJmxModel(com.tangosol.net.management.MBeanReference ref)
        {
        // import Component.Net.Management.Model.LocalModel.WrapperModel.WrapperJmxModel;
        
        WrapperJmxModel model = new WrapperJmxModel();
        model.initialize(ref);
        return model;
        }
    
    protected com.tangosol.coherence.component.net.management.model.localModel.WrapperModel instantiateWrapperModel(Object oBean)
        {
        // import Component.Net.Management.Model.LocalModel.WrapperModel;
        
        WrapperModel model = new WrapperModel();
        model.setMBean(oBean);
        return model;
        }
    
    // From interface: com.tangosol.net.management.MBeanServerProxy
    public Object invoke(String sName, String sMethodName, Object[] aoParam, String[] asSignature)
        {
        return null;
        }
    
    // From interface: com.tangosol.net.management.Registry
    public boolean isExtendedMBeanName()
        {
        return getDependencies().isExtendedMBeanName();
        }
    
    /**
     * Check whether or not the specified canonical name represents a bean
    * should be registered globally (with remote MBeanServer)
     */
    public static boolean isGlobal(String sCanonicalName)
        {
        // import com.tangosol.net.management.Registry;
         
        // all remote names must either contain the originator id
        // or a responsibility key
        return sCanonicalName.contains(Registry.KEY_NODE_ID)
            || isResponsibilityMBean(sCanonicalName);
        }
    
    // From interface: com.tangosol.net.management.MBeanServerProxy
    public boolean isMBeanRegistered(String sName)
        {
        return false;
        }
    
    // Accessor for the property "Primary"
    /**
     * Specifies if this Gateway is the "primary" gateway (i.e. returned from
    * the Cluster.getManagement() 
    * method).
     */
    public boolean isPrimary()
        {
        return __m_Primary;
        }
    
    /**
     * Specifies whether or not only the viewing of attributes is allowed.
    *   
     */
    public boolean isReadOnly()
        {
        return getDependencies().isReadOnly();
        }
    
    // From interface: com.tangosol.net.management.Registry
    /**
     * @inheritDoc
     */
    public boolean isRegistered(String sName)
        {
        throw new UnsupportedOperationException();
        }
    
    /**
     * Return true if the provided canonical name represents a responsibility
    * MBean.
     */
    public static boolean isResponsibilityMBean(String sCanonicalName)
        {
        // import com.tangosol.net.management.Registry;
         
        return sCanonicalName.contains(Registry.KEY_RESPONSIBILITY);
        }
    
    // From interface: com.tangosol.net.management.NotificationManager
    /**
     * @inheritDoc
     */
    public boolean isSubscribedTo(String sName)
        {
        // import Component.Net.Management.Model.LocalModel;
        
        LocalModel model = (LocalModel) getLocalModels().get(extractTenantName(sName));
        return model != null && model.is_SubscribedTo();
        }
    
    // From interface: com.tangosol.net.management.MBeanServerProxy
    public com.tangosol.net.management.MBeanServerProxy local()
        {
        throw new UnsupportedOperationException();
        }
    
    /**
     * Finalize the construction of the Gateway as a "Primary" one.
     */
    public void makePrimary()
        {
        // import com.tangosol.net.management.Registry;
        
        _assert(!isPrimary());
        setPrimary(true);
        
        register(ensureGlobalName(Registry.CLUSTER_TYPE), getCluster());
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
    * GatewayDependencies deps = (GatewayDependencies) getDependencies();
    * deps.getFoo();
     */
    protected void onDependencies(com.tangosol.internal.net.management.GatewayDependencies deps)
        {
        }
    
    /**
     * Query for local MBean names matching the name pattern and oprional filter.
     */
    public java.util.Set queryLocalNames(String sPattern, com.tangosol.util.Filter filter)
        {
        // import javax.management.MalformedObjectNameException;
        // import javax.management.ObjectName;
        
        try
            {
            ObjectName oname = sPattern == null ? null : getObjectName(sPattern);
            return queryNames(oname, filter);
            }
        catch (MalformedObjectNameException e)
            {
            throw ensureRuntimeException(e, sPattern, null);
            }
        }
    
    /**
     * Query for local MBean names matching the ObjectName pattern and oprional
    * filter.
     */
    public java.util.Set queryLocalNames(javax.management.ObjectName oname, com.tangosol.util.Filter filter)
        {
        // import com.tangosol.util.Base;
        // import java.util.HashSet;
        // import java.util.Iterator;
        // import java.util.Set;
        // import javax.management.MalformedObjectNameException;
        // import javax.management.ObjectName;
        
        Set setNames = new HashSet();
        
        for (Iterator iter = getLocalModels().keySet().iterator(); iter.hasNext();)
            {
            String sName = (String) iter.next();
            try
                {
                if (oname == null || oname.apply(getObjectName(sName)))
                    {
                    if (filter == null || filter.evaluate(oname))
                        {
                        setNames.add(sName);
                        }
                    }
                }        
            catch (MalformedObjectNameException e)
                {
                throw ensureRuntimeException(e, sName, null);
                }
            }
        
        return setNames;
        }
    
    // From interface: com.tangosol.net.management.MBeanServerProxy
    public java.util.Set queryNames(String sPattern, com.tangosol.util.Filter filter)
        {
        return null;
        }
    
    // From interface: com.tangosol.net.management.MBeanServerProxy
    public java.util.Set queryNames(javax.management.ObjectName oname, com.tangosol.util.Filter filter)
        {
        return null;
        }
    
    // From interface: com.tangosol.net.management.Registry
    public void register(com.tangosol.util.HealthCheck healthCheck)
        {
        // import com.oracle.coherence.common.base.Logger;
        // import com.tangosol.internal.health.HealthCheckWrapper;
        // import com.tangosol.internal.health.HealthCheckWrapperMBean;
        
        HealthCheckWrapperMBean wrapper = healthCheck instanceof HealthCheckWrapperMBean
                ? (HealthCheckWrapperMBean) healthCheck
                : new HealthCheckWrapper(healthCheck);
        
        String sName = ensureGlobalName(HealthCheckWrapper.getMBeanName(healthCheck));
        
        registerLocalModel(sName, instantiateLocalModel(wrapper));
        getRegisteredHealthChecks().put(sName, healthCheck);
        
        Logger.finest("Health: registered health check " + healthCheck.getName());
        }
    
    // From interface: com.tangosol.net.management.Registry
    /**
     * @inheritDoc
     */
    public void register(String sName, Object oBean)
        {
        // import Component.Net.Management.Model.LocalModel.ClusterModel;
        // import Component.Net.Management.Model.LocalModel.ServiceModel;
        // import com.tangosol.internal.health.HealthCheckWrapper;
        // import com.tangosol.internal.health.HealthCheckWrapperMBean;
        // import com.tangosol.util.Filter;
        // import com.tangosol.util.HealthCheck;
        
        sName = extractTenantName(sName);
        
        Filter filter = getFilter();
        if (filter == null || filter.evaluate(sName))
            {
            LocalModel model = instantiateLocalModel(oBean);
            
            registerLocalModel(sName, model);
        
            if (model instanceof HealthCheck)
                {
                String sSubType = HealthCheckWrapperMBean.SUBTYPE_APPLICATION;
        
                if (model instanceof ClusterModel)
                    {
                    sSubType = HealthCheckWrapperMBean.SUBTYPE_CLUSTER;
                    }
                else if (model instanceof ServiceModel)
                    {
                    sSubType = HealthCheckWrapperMBean.SUBTYPE_SERVICE;
                    }
        
                HealthCheckWrapper wrapper     = new HealthCheckWrapper((HealthCheck) model, sSubType);
                String             sHealthName = ensureGlobalName(HealthCheckWrapper.getMBeanName(wrapper));
        
                registerLocalModel(sHealthName, instantiateLocalModel(wrapper));
                getRegisteredHealthChecks().put(sHealthName, wrapper);
                }
            }
        }
    
    /**
     * Register all "custom" MBeans specified in the config.
     */
    public void registerCustomBeans()
        {
        // import com.tangosol.internal.net.management.CustomMBeanDependencies;
        // import com.tangosol.util.ClassHelper;
        // import java.util.Collection;
        // import java.util.Iterator;
        // import java.util.Map;
        
        Map      mapBeans     = getCustomBeans();
        Iterable iterBeanDeps = getDependencies().getCustomMBeanDependencies();
        
        for (Iterator iterBeans = iterBeanDeps.iterator(); iterBeans.hasNext();)
            {
            CustomMBeanDependencies mbd = (CustomMBeanDependencies) iterBeans.next();
        
            String  sFactory      = mbd.getMBeanFactory();
            String  sServerDomain = mbd.getMBeanServerDomain();
            String  sQuery        = mbd.getMBeanQuery();
            String  sClass        = mbd.getMBeanClass();
            String  sAccessor     = mbd.getMBeanAccessor();
            String  sMBeanName    = mbd.getMBeanName();
            boolean fLocal        = mbd.isLocalOnly();
        
            try
                {
                boolean fEnabled = mbd.isEnabled();
                boolean fKeep    = mbd.isExtendLifecycle();
        
                Class   clzMF;
                Object  oMBean;
        
                if (fEnabled)
                    {
                    // the custom definition is either query or class or factory
                    if (sQuery.length() == 0)
                        {
                        oMBean = fKeep ? mapBeans.get(sMBeanName) : null;
                        if (oMBean == null)
                            {
                            if (sClass.length() == 0)
                                {
                                clzMF  = Class.forName(sFactory);
                                oMBean = ClassHelper.invokeStatic(clzMF, sAccessor, null);
                                }
                            else
                                {
                                clzMF  = Class.forName(sClass);
                                oMBean = ClassHelper.newInstance(clzMF, null);
                                }
        
                            if (fKeep)
                                {
                                mapBeans.put(sMBeanName, oMBean);
                                }
                            }
        
                        if (oMBean instanceof Collection)
                            {
                            Collection colSubBeans = (Collection) oMBean;
                            for (Iterator iterSubBeans = colSubBeans.iterator(); iterSubBeans.hasNext();)
                                {
                                Object oSubMBean = iterSubBeans.next();
                                try
                                    {
                                    String sName = (String) ClassHelper.invoke(
                                        oSubMBean, "getName", ClassHelper.VOID);
                                    String sLocalName = sMBeanName + ",Name=" + sName;
        
                                    register(fLocal ? sLocalName : ensureGlobalName(sLocalName), oSubMBean);
                                    }
                                catch (Exception e) // NoSuchMethod, IllegalAccess, InvocationTarget[Exception]
                                    {
                                    _trace("Unable to call \"getName()\" for MBean \"" + oSubMBean
                                         + "\". The MBean is not registered.", 2);
                                    }
                                }
                            }
                        else if (oMBean != null)
                            {
                            register(fLocal ? sMBeanName : ensureGlobalName(sMBeanName), oMBean);
                            }
                        }
                    else
                        {
                        registerQueryMBeans(sServerDomain, sQuery, sMBeanName);
                        }
                    }
                }
            catch (Exception e) // ClassNotFound,  IllegalAccessException, InvocationTargetException
                {
                // This is a non-critical configuration error. 
                _trace("Unable to register MBean \"" + sMBeanName + "\" from factory \""
                     + sFactory + "\"." + getStackTrace(e), 2);
                }
            }
        }
    
    /**
     * Register LocalModel under a given name.
     */
    public void registerLocalModel(String sCanonicalName, com.tangosol.coherence.component.net.management.model.LocalModel model)
        {
        if (isPrimary())
            {
            // As of Coherence 3.7.1.2, we no longer strip the domain from the canonical name (COH-5974).
            model.set_ModelName(sCanonicalName);
            getLocalModels().put(sCanonicalName, model);
            }
        }
    
    protected void registerMetrics(String sCanonicalName)
        {
        if (isPrimary())
            {
            getMetricSupport().register(sCanonicalName);
            }
        }
    
    /**
     * Find all MBeans specified by the query at a local MBeanServer and
    * register them with this gateway. 
    * Node: the MBeanServer is not necessarily the one used by the gateway.
    * 
    * @param sMBeanServerDomain Name of the DefaultDomain of the MBeanServer
    * where the Query should be executed.
    * @param sQuery                         a JMX query that will be executed
    * and the result set registered.
    * @param sPrefix                          a target location that would be
    * prepended to converted MBean names
     */
    protected void registerQueryMBeans(String sMBeanServerDomain, String sQuery, String sPrefix)
        {
        // import com.tangosol.net.management.MBeanHelper;
        
        try
            {
            MBeanHelper.registerQueryMBeans(sMBeanServerDomain, sQuery, sPrefix, this);
            }
        catch (Throwable e)
            {
            _trace("The MBean query \"" + sQuery
                 + "\" failed and will be ignored:\n" + getStackTrace(e), 1);
            }
        }
    
    public void registerReporter()
        {
        // import com.tangosol.coherence.reporter.ReportControl;
        // import com.tangosol.net.management.Registry;
        
        ReportControl reporter = getReportControl();
        String        sName    = Registry.REPORTER_TYPE;
        
        if (!reporter.isCentralized())
            {
            sName = ensureGlobalName(sName);
            }
        
        register(sName, reporter);
        
        if (reporter.isAutoStart())
            {
            reporter.start();
            }
        }
    
    /**
     * Removes entries in the specified map with keys matching the given
    * pattern. The pattern wildcard (*) is assumed and should never be an
    * actual part of the key.
    * 
    * @return the set of keys that were removed
     */
    public static java.util.Set removeByPattern(java.util.Map mapRegistry, String sPattern)
        {
        // import com.tangosol.internal.health.HealthCheckWrapperMBean;
        // import com.tangosol.util.Base;
        // import com.tangosol.util.HealthCheck;
        // import com.tangosol.util.ImmutableArrayList;
        // import java.util.ArrayList;
        // import java.util.HashSet;
        // import java.util.Iterator;
        // import java.util.List;
        // import java.util.Map$Entry as java.util.Map.Entry;
        // import java.util.Set;
        
        Set  setPattern = new ImmutableArrayList(Base.parseDelimitedString(sPattern, ','));
        Set  setRemoved = new HashSet();
        List listModel  = new ArrayList();
        
        for (Iterator iter = mapRegistry.entrySet().iterator(); iter.hasNext();)
            {
            java.util.Map.Entry  entry = (java.util.Map.Entry) iter.next();
            String sName = (String) entry.getKey();
          
            Set setParts = new ImmutableArrayList(Base.parseDelimitedString(sName, ','));
            if (setParts.containsAll(setPattern))
                {
                listModel.add(entry.getValue());
                setRemoved.add(sName);
                iter.remove();
                }    
            }
        
        
        for (Iterator iter = listModel.iterator(); iter.hasNext();)
            {
            Model  model      = (Model) iter.next();
            String sHeathName = model.get_HealthModelName();
            if (sHeathName != null)
                {
                mapRegistry.remove(sHeathName);
                setRemoved.add(sHeathName);
                }
            }
        
        return setRemoved;
        }
    
    // From interface: com.tangosol.net.management.MBeanServerProxy
    public void removeNotificationListener(String sName, javax.management.NotificationListener listener)
        {
        throw new UnsupportedOperationException();
        }
    
    // From interface: com.tangosol.net.management.MBeanServerProxy
    public void removeNotificationListener(String sName, javax.management.NotificationListener listener, javax.management.NotificationFilter filter, Object oHandback)
        {
        throw new UnsupportedOperationException();
        }
    
    /**
     * Reset all local data structures if necessary. This method is called when
    * the cluster services first started or restarted after abnormal
    * termination.
    * 
    * @see SafeCluster#ensureSafeCluster
     */
    public void reset()
        {
        // import java.util.Iterator;
        
        if (isPrimary())
            {
            for (Iterator iter = getLocalModels().keySet().iterator(); iter.hasNext();)
                {
                String sName = (String) iter.next();
        
                if (isGlobal(sName))
                    {
                    iter.remove();
                    }
                }
            }
        }
    
    // From interface: com.tangosol.net.management.MBeanServerProxy
    public void setAttribute(String sName, String sAttr, Object oValue)
        {
        }
    
    // Accessor for the property "Cluster"
    /**
     * Setter for property Cluster.<p>
    * Cluster this management gateway is associated with.
     */
    public void setCluster(com.tangosol.coherence.component.util.SafeCluster cluster)
        {
        _assert(cluster == null || getCluster() == null, "Cluster is not resettable");
        
        __m_Cluster = (cluster);
        }
    
    // Accessor for the property "CustomBeans"
    /**
     * A map of the custom beans registered to avoid recreation.
     */
    protected void setCustomBeans(java.util.Map map)
        {
        __m_CustomBeans = map;
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
    public void setDependencies(com.tangosol.internal.net.management.GatewayDependencies deps)
        {
        if (getDependencies() != null)
            {
            throw new IllegalStateException("Dependencies already set");
            }
        
        __m_Dependencies = (cloneDependencies(deps).validate());
        
        // use the cloned dependencies
        onDependencies(getDependencies());
        }
    
    // Accessor for the property "DomainName"
    /**
     *  Domain name for managed beans. For Local gateway the value is
    * "Coherence" (or "Coherence@NNN" in a case of a 
    * name conflict when multiple clusters run within the same JVM).  For
    * Remote gateway the value is an empty string.
     */
    protected void setDomainName(String pDomainName)
        {
        __m_DomainName = pDomainName;
        }
    
    // Accessor for the property "Filter"
    /**
     * Setter for property Filter.<p>
    * The Filter used to evaluate whether or not to register a model with the
    * specified name.
     */
    protected void setFilter(com.tangosol.util.Filter filter)
        {
        __m_Filter = filter;
        }
    
    // Accessor for the property "LocalModels"
    /**
     * Setter for property LocalModels.<p>
    * A Map<String, LocalModel> of LocalModel components registered with this
    * gateway. The key is the MBean key property string (fully qualified name
    * sans the domain).
     */
    protected void setLocalModels(java.util.Map map)
        {
        __m_LocalModels = map;
        }
    
    // Accessor for the property "MetricSupport"
    /**
     * Setter for property MetricSupport.<p>
     */
    public void setMetricSupport(com.tangosol.internal.metrics.MetricSupport supportMetric)
        {
        __m_MetricSupport = supportMetric;
        }
    
    // Accessor for the property "ObjectNameCache"
    /**
     * Set Cache of ObjectName objects keyed by the canonical names.
     */
    protected void setObjectNameCache(java.util.Map map)
        {
        __m_ObjectNameCache = map;
        }
    
    // Accessor for the property "Primary"
    /**
     * Setter for property Primary.<p>
    * Specifies if this Gateway is the "primary" gateway (i.e. returned from
    * the Cluster.getManagement() method).
    *  
     */
    protected void setPrimary(boolean primary)
        {
        __m_Primary = primary;
        }
    
    // Accessor for the property "RegisteredHealthChecks"
    /**
     * Setter for property RegisteredHealthChecks.<p>
     */
    public void setRegisteredHealthChecks(java.util.Map mapChecks)
        {
        __m_RegisteredHealthChecks = mapChecks;
        }
    
    // Accessor for the property "ReportControl"
    /**
     * Setter for property ReportControl.<p>
    * The Reporter for the Gateway.
     */
    protected void setReportControl(com.tangosol.coherence.reporter.ReportControl rc)
        {
        __m_ReportControl = rc;
        }
    
    // From interface: com.tangosol.net.management.NotificationManager
    /**
     * @inheritDoc
     */
    public void trigger(String sName, String sType, String sMessage)
            throws java.lang.IllegalArgumentException
        {
        // import Component.Net.Management.Model.LocalModel;
        
        LocalModel model = ensureLocalModel(extractTenantName(sName));
        if (model != null)
            {
            model._handleNotification(sType, sMessage);
            }
        }
    
    // From interface: com.tangosol.net.management.NotificationManager
    /**
     * @inheritDoc
     */
    public void trigger(String sName, javax.management.Notification notification)
            throws java.lang.IllegalArgumentException
        {
        // import Component.Net.Management.Model.LocalModel;
        
        LocalModel model = ensureLocalModel(extractTenantName(sName));
        if (model != null)
            {
            model._handleNotification(notification);
            }
        }
    
    // From interface: com.tangosol.net.management.Registry
    public void unregister(com.tangosol.util.HealthCheck healthCheck)
        {
        // import com.tangosol.internal.health.HealthCheckWrapper;
        
        String sName = ensureGlobalName(HealthCheckWrapper.getMBeanName(healthCheck));
        
        unregister(sName);
        if (getRegisteredHealthChecks().remove(sName) != null)
            {
            Logger.finest("Health: unregistered health check " + healthCheck.getName());
            }
        }
    
    // From interface: com.tangosol.net.management.Registry
    /**
     * @inheritDoc
     */
    public void unregister(String sName)
        {
        if (getDependencies().isExtendedMBeanName() &&
            sName.charAt(sName.length() - 1) != '*')
            {
            // convert to a wild carded removal as the caller would not be aware
            // of what was added to the MBean name
            sName += ",*";
            }
        unregisterLocalModel(extractTenantName(sName));
        }
    
    public void unregisterLocalModel(String sCanonicalName)
        {
        // import com.tangosol.net.management.MBeanHelper;
        // import com.tangosol.util.HealthCheck;
        // import java.util.Iterator;
        // import java.util.Map;
        // import java.util.Set;
        
        if (isPrimary())
            {
            Map mapModels = getLocalModels();
        
            if (sCanonicalName.endsWith(",*"))
                {
                String sName    = MBeanHelper.stripDomain(sCanonicalName);
                String sPattern = sName.substring(0, sName.length() - 2);
                Set    setKeys  = removeByPattern(mapModels, sPattern);
        
                for (Iterator iter = setKeys.iterator(); iter.hasNext();)
                    {
                    String sKey = (String) iter.next();
                    
                    unregisterMetrics(sKey);
                    }
                }
            else
                {  
                Model model = (Model) mapModels.remove(sCanonicalName);
                unregisterMetrics(sCanonicalName);
                if (model != null)
                    {
                    String sHeathName = model.get_HealthModelName();
                    if (sHeathName != null)
                        {
                        mapModels.remove(sHeathName);
                        getRegisteredHealthChecks().remove(sHeathName);
                        } 
                    }
                }
            }
        }
    
    protected void unregisterMetrics(String sCanonicalName)
        {
        if (isPrimary())
            {
            getMetricSupport().remove(sCanonicalName);
            }
        }
    }
