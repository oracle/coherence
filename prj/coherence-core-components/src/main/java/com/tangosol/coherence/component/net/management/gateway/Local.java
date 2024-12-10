
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.management.gateway.Local

package com.tangosol.coherence.component.net.management.gateway;

import com.tangosol.coherence.component.manageable.ModelAdapter;
import com.tangosol.coherence.component.net.management.Gateway;
import com.tangosol.coherence.component.net.management.model.EmptyModel;
import com.tangosol.coherence.component.net.management.model.LocalModel;
import com.tangosol.net.management.MBeanHelper;
import com.tangosol.net.management.Registry;
import com.tangosol.util.Base;
import com.tangosol.util.ConcurrentMap;
import com.tangosol.util.LiteMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.management.Attribute;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.JMException;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

/**
 * The base component for the Coherence Management framework implementation.
 * 
 * Local gateway provides an ability to register MBeans with a local
 * MBeanServer.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class Local
        extends    com.tangosol.coherence.component.net.management.Gateway
    {
    // ---- Fields declarations ----
    
    /**
     * Property ModelAdapters
     *
     * A map of instantiated ModelAdapters keyed by ObjectNames.
     */
    private transient com.tangosol.util.ConcurrentMap __m_ModelAdapters;
    
    /**
     * Property RemoteGateway
     *
     * An associated remote gateway.
     */
    private transient com.tangosol.coherence.component.net.management.Gateway __m_RemoteGateway;
    
    /**
     * Property Server
     *
     * The MBeanServer
     */
    private javax.management.MBeanServer __m_Server;
    
    /**
     * Property ServiceUrl
     *
     * JMXServiceURL for the MBeanConnector.
     */
    private javax.management.remote.JMXServiceURL __m_ServiceUrl;
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
        __mapChildren.put("LocalMBeanServerProxy", Local.LocalMBeanServerProxy.get_CLASS());
        }
    
    // Default constructor
    public Local()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public Local(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            setDomainName("Coherence");
            setLocalModels(new com.tangosol.util.SafeHashMap());
            setMetricSupport(new com.tangosol.internal.metrics.MetricSupport());
            setModelAdapters(new com.tangosol.util.SegmentedConcurrentMap());
            setPrimary(false);
            setRegisteredHealthChecks(new com.tangosol.util.SafeHashMap());
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
        return new com.tangosol.coherence.component.net.management.gateway.Local();
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
            clz = Class.forName("com.tangosol.coherence/component/net/management/gateway/Local".replace('/', '.'));
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
    public void addNotificationListener(String sName, javax.management.NotificationListener listener, javax.management.NotificationFilter filter, Object oHandback)
        {
        try
            {
            getServer().addNotificationListener(getObjectName(extractTenantName(sName)), listener, filter, oHandback);
            }
        catch (Exception e)
            {
            throw ensureRuntimeException(e, sName, null);
            }
        }
    
    // Declared at the super level
    /**
     * Convert the passed exception to a RuntimeException if necessary.
     */
    protected RuntimeException ensureRuntimeException(Exception e, String sName, String sAttr)
        {
        // import javax.management.AttributeNotFoundException;
        // import javax.management.InstanceNotFoundException;
        // import javax.management.ListenerNotFoundException;
        // import javax.management.MalformedObjectNameException;
        
        if (e instanceof MalformedObjectNameException)
            {
            return new IllegalArgumentException("Invalid MBean name: " + sName, e);
            }
        
        if (e instanceof InstanceNotFoundException)
            {
            return new IllegalArgumentException("Cannot find MBean named: " + sName, e);
            }
        
        if (e instanceof AttributeNotFoundException)
            {
            return new IllegalArgumentException(
                "Unknown attribute \"" + sAttr + "\" for MBean " + sName, e);
            }
        
        if (e instanceof ListenerNotFoundException)
            {
            return new IllegalArgumentException("The specified NotificationListener has not been added to MBean: " + sName, e);
            }
        
        if (e instanceof RuntimeException)
            {
            return (RuntimeException) e;
            }
        
        return new RuntimeException(e);
        }
    
    // Declared at the super level
    public Object executeInternal(com.tangosol.util.function.Remote.Function function, com.oracle.coherence.common.base.Continuation cont)
        {
        if (cont == null)
            {
            return function.apply(getServer());
            }
        
        Object oResult;
        try
            {
            oResult = function.apply(getServer());
            }
        catch (Throwable t)
            {
            oResult = t;
            }
        
        cont.proceed(oResult);
        
        return null;
        }
    
    // Declared at the super level
    /**
     * @inheritDoc
     */
    public Object getAttribute(String sName, String sAttr)
        {
        // import javax.management.MalformedObjectNameException;
        // import javax.management.ObjectName;
        
        try
            {
            ObjectName name = getObjectName(extractTenantName(sName));
            try
                {
                return getServer().getAttribute(name, sAttr);
                }
            catch (Exception e)
                {
                throw ensureRuntimeException(e, name.toString(), sAttr);
                }
            }
        catch (MalformedObjectNameException e)
            {
            throw ensureRuntimeException(e, sName, sAttr);
            }
        }
    
    // Declared at the super level
    public java.util.Map getAttributes(String sName, com.tangosol.util.Filter filter)
        {
        // import com.tangosol.util.Base;
        // import com.tangosol.util.LiteMap;
        // import javax.management.MBeanAttributeInfo;
        // import javax.management.MBeanInfo;
        // import javax.management.MBeanServer;
        // import javax.management.ObjectName;
        // import java.util.Map;
        
        MBeanServer mbs       = getServer();
        Map         mapResult = new LiteMap();
        try
            {
            ObjectName           mbeanName   = new ObjectName(sName);
            MBeanInfo            info        = mbs.getMBeanInfo(mbeanName);
            MBeanAttributeInfo[] aAttributes = info.getAttributes();
        
            for (int i = 0, c = aAttributes.length; i < c; ++i)
                {
                String sAttribute = aAttributes[i].getName();
                if (filter.evaluate(sAttribute))
                    {
                    mapResult.put(sAttribute, mbs.getAttribute(mbeanName, sAttribute));
                    }
                }
        
            return mapResult;
            }
        catch (Exception e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }
    
    // Declared at the super level
    public javax.management.MBeanInfo getMBeanInfo(String sName)
        {
        // import Component.Manageable.ModelAdapter;
        // import javax.management.MalformedObjectNameException;
        // import javax.management.ObjectName;
        
        ModelAdapter adapter = null;
        try
            {
            ObjectName name = getObjectName(sName);
        
            adapter = (ModelAdapter) getModelAdapters().get(name);
            }
        catch (MalformedObjectNameException e) {}
        
        return adapter == null ? null : adapter.getMBeanInfo();
        }
    
    // Accessor for the property "ModelAdapters"
    /**
     * Getter for property ModelAdapters.<p>
    * A map of instantiated ModelAdapters keyed by ObjectNames.
     */
    public com.tangosol.util.ConcurrentMap getModelAdapters()
        {
        return __m_ModelAdapters;
        }
    
    // Accessor for the property "RemoteGateway"
    /**
     * Getter for property RemoteGateway.<p>
    * An associated remote gateway.
     */
    public com.tangosol.coherence.component.net.management.Gateway getRemoteGateway()
        {
        return __m_RemoteGateway;
        }
    
    // Accessor for the property "Server"
    /**
     * Getter for property Server.<p>
    * The MBeanServer
     */
    public javax.management.MBeanServer getServer()
        {
        return __m_Server;
        }
    
    // Accessor for the property "ServiceUrl"
    /**
     * Getter for property ServiceUrl.<p>
    * JMXServiceURL for the MBeanConnector.
     */
    public javax.management.remote.JMXServiceURL getServiceUrl()
        {
        return __m_ServiceUrl;
        }
    
    /**
     * Instantiate a Local gateway.
     */
    public static Local instantiate(com.tangosol.coherence.component.util.SafeCluster cluster, com.tangosol.coherence.component.net.management.Connector connector, com.tangosol.internal.net.management.GatewayDependencies deps)
        {
        // import com.tangosol.net.management.Registry;
        
        Local gateway = new Local();
        
        gateway.setCluster(cluster);
        gateway.setDependencies(deps);
        
        gateway.makePrimary(); // this registers the Cluster MBean
        
        gateway.register(gateway.ensureGlobalName(Registry.MANAGEMENT_TYPE), connector);
        
        return gateway;
        }
    
    // Declared at the super level
    public Object invoke(String sName, String sMethodName, Object[] aoParam, String[] asSignature)
        {
        try
            {
            return getServer().invoke(getObjectName(extractTenantName(sName)),
                               sMethodName, aoParam, asSignature);
            }
        catch (Exception e)
            {
            throw ensureRuntimeException(e, sName, null);
            }
        }
    
    /**
     * Check whether or not there is a Cluster MBean with the specified name
    * that represents a running cluster.
     */
    protected boolean isClusterRunning(String sDomain)
        {
        // import com.tangosol.net.management.Registry;
        // import javax.management.JMException;
        // import javax.management.ObjectName;
        
        try
            {
            ObjectName nameCluster = new ObjectName(sDomain + ':' + Registry.CLUSTER_TYPE);
            if (getServer().isRegistered(nameCluster))
                {
                Boolean FRunning = (Boolean) getServer().getAttribute(nameCluster, "Running");
                if (FRunning.booleanValue())
                    {
                    return true;
                    }
                }
            }
        catch (JMException e) {}
        
        return false;
        }
    
    // Declared at the super level
    public boolean isMBeanRegistered(String sName)
        {
        return isRegistered(sName);
        }
    
    // Declared at the super level
    /**
     * @inheritDoc
     */
    public boolean isRegistered(String sName)
        {
        // import com.tangosol.net.management.Registry;
        // import javax.management.JMException;
        
        if (sName.equals(ensureGlobalName(Registry.NODE_TYPE)))
            {
            // this path is reserved to ensure the management service is running
            // @see SimpleServiceMonitor#monitorServices
            Remote gatewayRemote = (Remote) getRemoteGateway();
            if (gatewayRemote != null)
                {
                gatewayRemote.ensureRunningConnector();
                }
        
            return getLocalModels().containsKey(sName);
            }
        
        try
            {
            return getServer().isRegistered(
                getObjectName(extractTenantName(sName)));
            }
        catch (JMException e)
            {
            return false;
            }
        }
    
    // Declared at the super level
    public com.tangosol.net.management.MBeanServerProxy local()
        {
        return (Local.LocalMBeanServerProxy) _newChild("LocalMBeanServerProxy");
        }
    
    /**
     * Acquire an exclusive lock iff the provided MBean is a responsibility
    * MBean.
    * 
    * @param oname  the ObjectName that exclusive access is required
     */
    protected void lock(javax.management.ObjectName oname)
        {
        if (isResponsibilityMBean(oname.getCanonicalName()))
            {
            getModelAdapters().lock(oname, -1L);
            }
        }
    
    // Declared at the super level
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
        // import com.tangosol.net.management.MBeanHelper;
        
        ensureObjectNameCache();
        
        String sDefaultDomain = deps.getDefaultDomain();
        
        // configure the MBeanServer
        setServer(MBeanHelper.findMBeanServer(sDefaultDomain, deps));
        
        setServiceUrl(MBeanHelper.findJMXServiceUrl(sDefaultDomain, deps));
        
        // ensure domain name uniqueness
        String sPrefix           = getDomainName();
        String sConfiguredSuffix = deps.getDomainNameSuffix();
        
        if (sConfiguredSuffix != null && sConfiguredSuffix.length() > 0)
            {
            sPrefix = sPrefix + "@" + sConfiguredSuffix;
            }
        
        String sSuffix = "";
        int    iClone  = 0;
        while (true)
            {
            String sDomain = sPrefix + sSuffix;
        
            // check whether or not there are other Coherence MBeans;
            if (isClusterRunning(sDomain))
                {
                // there is another cluster; clone the domain name for the beans
                sSuffix = "@" + (++iClone);
                }
            else
                {
                setDomainName(sDomain);
        
                // cleanup any leftovers from previous runs
                String sCanonicalName;
                if (deps.isExtendedMBeanName())
                    {
                    // we're using extended MBean names, so only clean MBeans with the correct cluster name
                    String sCluster = getCluster().getDependencies().getMemberIdentity().getClusterName();
                    sCanonicalName = "cluster=" + sCluster + ",*";
                    }
                else
                    {
                    sCanonicalName = "*";
                    }
                unregisterModelMBean(sCanonicalName, null);
        
                break;
                }
            }
        
        // start the Reporter
        getReportControl();
        }
    
    public void onRegistration(int nAction, String sCanonicalName, String sBeanClassName)
        {
        }
    
    // Declared at the super level
    public java.util.Set queryNames(String sPattern, com.tangosol.util.Filter filter)
        {
        // import com.tangosol.net.management.MBeanHelper$QueryExpFilter as com.tangosol.net.management.MBeanHelper.QueryExpFilter;
        // import java.util.Iterator;
        // import java.util.Set;
        // import java.util.HashSet;
        // import javax.management.ObjectName;
        
        try
            {
            ObjectName oname = sPattern == null ? null : getObjectName(sPattern);
            return queryNames(oname, filter);
            }
        catch (Exception e)
            {
            throw ensureRuntimeException(e, sPattern, null);
            }
        }
    
    // Declared at the super level
    public java.util.Set queryNames(javax.management.ObjectName oname, com.tangosol.util.Filter filter)
        {
        // import com.tangosol.net.management.MBeanHelper$QueryExpFilter as com.tangosol.net.management.MBeanHelper.QueryExpFilter;
        // import java.util.Iterator;
        // import java.util.Set;
        // import java.util.HashSet;
        
        Set setONames = getServer().queryNames(oname, new com.tangosol.net.management.MBeanHelper.QueryExpFilter(filter));
        Set setNames  = new HashSet(setONames.size());
        for (Iterator iter = setONames.iterator(); iter.hasNext(); )
            {
            setNames.add(iter.next().toString());
            }
        
        return setNames;
        }
    
    // Declared at the super level
    /**
     * Register LocalModel under a given name.
     */
    public void registerLocalModel(String sCanonicalName, com.tangosol.coherence.component.net.management.model.LocalModel model)
        {
        // import Component.Net.Management.Gateway;
        
        _assert(isPrimary());
        
        super.registerLocalModel(sCanonicalName, model);
        
        registerModelMBean(sCanonicalName, model);
        
        Gateway gatewayRemote = getRemoteGateway();
        if (gatewayRemote != null)
            {
            gatewayRemote.registerLocalModel(sCanonicalName, model);
            }
        
        registerMetrics(sCanonicalName);
        }
    
    /**
     * Register an MBean for the specified model with the MBeanServer.
    * 
    * @param model  the model to create and register an MBean for
     */
    public void registerModelMBean(String sCanonicalName, com.tangosol.coherence.component.net.management.Model model)
        {
        // import Component.Manageable.ModelAdapter;
        // import com.tangosol.util.Base;
        // import com.tangosol.util.ConcurrentMap;
        // import javax.management.InstanceAlreadyExistsException;
        // import javax.management.JMException;
        // import javax.management.MalformedObjectNameException;
        // import javax.management.MBeanServer;
        // import javax.management.ObjectName;
        
        MBeanServer server = getServer();
        try
            {
            boolean       fRetry      = true;
            ObjectName    name        = getObjectName(sCanonicalName);
            ConcurrentMap mapAdapters = getModelAdapters();
        
            lock(name);
            try
                {
                while (true)
                    {
                    if (server.isRegistered(name))
                        {
                        ModelAdapter adapter = (ModelAdapter) mapAdapters.get(name);
                        if (adapter != null)
                            {
                            // replace the model without re-registering the adapter
                            // (subscriptions will be transferred if necessary)
                            adapter.set_Model(model);
                            return;
                            }
        
                        server.unregisterMBean(name);
                        }
        
                    try
                        {
                        ModelAdapter adapter = instantiateModelMBean(model);
        
                        server.registerMBean(adapter, name);
        
                        mapAdapters.put(name, adapter);
                        break;
                        }
                    catch (InstanceAlreadyExistsException e)
                        {
                        if (fRetry)
                            {
                            // COH-2200: WebSphere adds its own attributes to
                            // MBean names - retry with the modified one
                            name   = new ObjectName(e.getMessage());
                            fRetry = false;
                            }
                        else
                            {
                            throw e;
                            }
                        }
                    }
                }
            finally
                {
                unlock(name);
                }
            }
        catch (MalformedObjectNameException e)
            {
            _trace("Failed to register MBean: " + model
                 + "; reason=" + getStackTrace(e), 1);
            }
        catch (JMException e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }
    
    // Declared at the super level
    public void removeNotificationListener(String sName, javax.management.NotificationListener listener)
        {
        try
            {
            getServer().removeNotificationListener(getObjectName(extractTenantName(sName)), listener);
            }
        catch (Exception e)
            {
            throw ensureRuntimeException(e, sName, null);
            }
        }
    
    // Declared at the super level
    public void removeNotificationListener(String sName, javax.management.NotificationListener listener, javax.management.NotificationFilter filter, Object oHandback)
        {
        try
            {
            getServer().removeNotificationListener(getObjectName(extractTenantName(sName)), listener, filter, oHandback);
            }
        catch (Exception e)
            {
            throw ensureRuntimeException(e, sName, null);
            }
        }
    
    // Declared at the super level
    /**
     * Reset all local data structures if necessary. This method is called when
    * the cluster services first started or restarted after abnormal
    * termination.
    * 
    * @see SafeCluster#ensureSafeCluster
     */
    public void reset()
        {
        // import Component.Net.Management.Gateway;
        
        super.reset();
        
        unregisterGlobalMBeans();
        
        Gateway gatewayRemote = getRemoteGateway();
        if (gatewayRemote != null)
            {
            gatewayRemote.reset();
            }
        }
    
    // Declared at the super level
    /**
     * @inheritDoc
     */
    public void setAttribute(String sName, String sAttr, Object oValue)
        {
        // import javax.management.Attribute;
        
        try
            {
            getServer().setAttribute(getObjectName(extractTenantName(sName)),
                        new Attribute(sAttr, oValue));
            }
        catch (Exception e)
            {
            throw ensureRuntimeException(e, sName, sAttr);
            }
        }
    
    // Accessor for the property "ModelAdapters"
    /**
     * Setter for property ModelAdapters.<p>
    * A map of instantiated ModelAdapters keyed by ObjectNames.
     */
    protected void setModelAdapters(com.tangosol.util.ConcurrentMap map)
        {
        __m_ModelAdapters = map;
        }
    
    // Accessor for the property "RemoteGateway"
    /**
     * Setter for property RemoteGateway.<p>
    * An associated remote gateway.
     */
    public void setRemoteGateway(com.tangosol.coherence.component.net.management.Gateway gateway)
        {
        __m_RemoteGateway = gateway;
        }
    
    // Accessor for the property "Server"
    /**
     * Setter for property Server.<p>
    * The MBeanServer
     */
    protected void setServer(javax.management.MBeanServer pServer)
        {
        __m_Server = pServer;
        }
    
    // Accessor for the property "ServiceUrl"
    /**
     * Setter for property ServiceUrl.<p>
    * JMXServiceURL for the MBeanConnector.
     */
    protected void setServiceUrl(javax.management.remote.JMXServiceURL url)
        {
        __m_ServiceUrl = url;
        }
    
    /**
     * Release an exclusive lock iff the provided MBean is a responsibility
    * MBean.
    * 
    * @param oname  the ObjectName that exclusive access should be relinquished
     */
    protected void unlock(javax.management.ObjectName oname)
        {
        if (isResponsibilityMBean(oname.getCanonicalName()))
            {
            getModelAdapters().unlock(oname);
            }
        }
    
    /**
     * Unregister all MBeans that have a global object name.
    * 
    * @see #isGlobal
     */
    public void unregisterGlobalMBeans()
        {
        // import java.util.Iterator;
        // import java.util.Map;
        // import java.util.Set;
        // import javax.management.JMException;
        // import javax.management.MBeanServer;
        // import javax.management.ObjectName;
        
        MBeanServer server = getServer();
        try
            {
            Map mapNames = getObjectNameCache();
            Set setNames = server.queryNames(getObjectName("*"), null);
        
            for (Iterator iter = setNames.iterator(); iter.hasNext();)
                {
                ObjectName oname = (ObjectName) iter.next();
                if (isGlobal(oname.getCanonicalName()))
                    {
                    lock(oname);
                    try
                        {
                        mapNames.remove(oname.getKeyPropertyListString());
                        server.unregisterMBean(oname);
                        }
                    catch (JMException e)
                        {
                        // ignore "not there" exception
                        // throw Base.ensureRuntimeException(e);
                        }
                    finally
                        {
                        unlock(oname);
                        }
                    }
                }
            }
        catch (JMException e)
            {
            _trace("Unregister query failed: " + e, 4);
            }
        }
    
    // Declared at the super level
    public void unregisterLocalModel(String sCanonicalName)
        {
        // import Component.Net.Management.Gateway;
        // import Component.Net.Management.Model.LocalModel;
        
        LocalModel model = (LocalModel) getLocalModels().get(sCanonicalName);
        
        super.unregisterLocalModel(sCanonicalName);
        
        unregisterModelMBean(sCanonicalName, model);
        
        Gateway gatewayRemote = getRemoteGateway();
        if (gatewayRemote != null)
            {
            gatewayRemote.unregisterLocalModel(sCanonicalName);
            }
        }
    
    public void unregisterModelMBean(String sCanonicalName, com.tangosol.coherence.component.net.management.Model model)
        {
        // import Component.Manageable.ModelAdapter;
        // import Component.Net.Management.Model.EmptyModel;
        // import java.util.Iterator;
        // import java.util.Map;
        // import java.util.Set;
        // import javax.management.JMException;
        // import javax.management.MBeanServer;
        // import javax.management.ObjectName;
        
        MBeanServer server = getServer();
        try
            {
            Map mapNames    = getObjectNameCache();
            Map mapAdapters = getModelAdapters();
        
            Set setNames = server.queryNames(getObjectName(sCanonicalName), null);
        
            for (Iterator iter = setNames.iterator(); iter.hasNext();)
                {
                ObjectName oname = null;
                try
                    {
                    lock(oname = (ObjectName) iter.next());
        
                    if (model == null)
                        {
                        mapAdapters.remove(oname);
                        mapNames.remove(oname.getKeyPropertyListString());
                        server.unregisterMBean(oname);
                        }
                    else
                        {
                        // ensure we *only* remove the ModelAdapter if the
                        // provided model is still linked to the adapter
        
                        ModelAdapter adapter = (ModelAdapter) mapAdapters.get(oname);
                        if (adapter != null && adapter.get_Model() == model)
                            {
                            if (isResponsibilityMBean(sCanonicalName) &&
                                    model.is_SubscribedTo())
                                {
                                // replace the MBean's model with a "dummy"
                                // to preserve the subscriptions
                                EmptyModel model0 = new EmptyModel();
                                model0.setMBeanInfo(adapter.getMBeanInfo());
        
                                adapter.set_Model(model0);
                                }
                            else
                                {
                                mapAdapters.remove(oname);
                                mapNames.remove(oname.getKeyPropertyListString());
                                server.unregisterMBean(oname);
                                }
                            }
                        }
                    }
                catch (Exception e)
                    {
                    // log the exception and continue
                    _trace("Failed to unregister MBean " + oname + "; " + e, 4);
                    }
                finally
                    {
                    if (oname != null)
                        {
                        unlock(oname);
                        }
                    }
                }
            mapNames.remove(sCanonicalName);
            }
        catch (JMException e)
            {
            _trace("Unregister query failed: " + e, 4);
            }
        }

    // ---- class: com.tangosol.coherence.component.net.management.gateway.Local$LocalMBeanServerProxy
    
    /**
     * An MBeanServerProxy implementation that sees only the MBeans that are
     * local to this member.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class LocalMBeanServerProxy
            extends    com.tangosol.coherence.component.Util
            implements com.tangosol.net.management.MBeanServerProxy
        {
        // ---- Fields declarations ----
        
        // Default constructor
        public LocalMBeanServerProxy()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public LocalMBeanServerProxy(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.net.management.gateway.Local.LocalMBeanServerProxy();
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
                clz = Class.forName("com.tangosol.coherence/component/net/management/gateway/Local$LocalMBeanServerProxy".replace('/', '.'));
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
        
        // From interface: com.tangosol.net.management.MBeanServerProxy
        public void addNotificationListener(String Param_1, javax.management.NotificationListener Param_2, javax.management.NotificationFilter Param_3, Object Param_4)
            {
            }
        
        // From interface: com.tangosol.net.management.MBeanServerProxy
        public Object execute(com.tangosol.util.function.Remote.Function function)
            {
            throw new UnsupportedOperationException("execute() is not supported by a local only MBeanServerProxy");
            }
        
        // From interface: com.tangosol.net.management.MBeanServerProxy
        public Object getAttribute(String sName, String sAttr)
            {
            // import Component.Net.Management.Model.LocalModel;
            // import javax.management.InstanceNotFoundException;
            
            Local    gateway = (Local) get_Module();
            LocalModel model   = gateway.getLocalModel(sName);
            if (model == null)
                {
                throw new IllegalArgumentException("MBean " + sName + " does not exist");
                }
            return gateway.getAttribute(sName, sAttr);
            }
        
        // From interface: com.tangosol.net.management.MBeanServerProxy
        public java.util.Map getAttributes(String sName, com.tangosol.util.Filter filter)
            {
            // import Component.Net.Management.Model.LocalModel;
            
            Local    gateway = (Local) get_Module();
            LocalModel model   = gateway.getLocalModel(sName);
            if (model == null)
                {
                throw new IllegalArgumentException("MBean " + sName + " does not exist");
                }
            return gateway.getAttributes(sName, filter);
            }
        
        // From interface: com.tangosol.net.management.MBeanServerProxy
        public javax.management.MBeanInfo getMBeanInfo(String sName)
            {
            // import Component.Net.Management.Model.LocalModel;
            // import javax.management.InstanceNotFoundException;
            // import javax.management.MBeanInfo;
            
            Local    gateway = (Local) get_Module();
            LocalModel model   = gateway.getLocalModel(sName);
            if (model == null)
                {
                throw new IllegalArgumentException("MBean " + sName + " does not exist");
                }
            return gateway.getMBeanInfo(sName);
            }
        
        // From interface: com.tangosol.net.management.MBeanServerProxy
        public Object invoke(String sName, String sMethodName, Object[] aoParam, String[] asSignature)
            {
            // import Component.Net.Management.Model.LocalModel;
            // import javax.management.InstanceNotFoundException;
            
            Local    gateway = (Local) get_Module();
            LocalModel model   = gateway.getLocalModel(sName);
            if (model == null)
                {
                throw new IllegalArgumentException("MBean " + sName + " does not exist");
                }
            return gateway.invoke(sName, sMethodName, aoParam, asSignature);
            }
        
        // From interface: com.tangosol.net.management.MBeanServerProxy
        public boolean isMBeanRegistered(String sName)
            {
            // import Component.Net.Management.Model.LocalModel;
            
            Local    gateway = (Local) get_Module();
            LocalModel model   = gateway.getLocalModel(sName);
            return model != null;
            }
        
        // From interface: com.tangosol.net.management.MBeanServerProxy
        public com.tangosol.net.management.MBeanServerProxy local()
            {
            return this;
            }
        
        // From interface: com.tangosol.net.management.MBeanServerProxy
        public java.util.Set queryNames(String sPattern, com.tangosol.util.Filter filter)
            {
            Local gateway = (Local) get_Module();
            return gateway.queryLocalNames(sPattern, filter);
            }
        
        // From interface: com.tangosol.net.management.MBeanServerProxy
        public java.util.Set queryNames(javax.management.ObjectName oname, com.tangosol.util.Filter filter)
            {
            Local gateway = (Local) get_Module();
            return gateway.queryLocalNames(oname, filter);
            }
        
        // From interface: com.tangosol.net.management.MBeanServerProxy
        public void removeNotificationListener(String Param_1, javax.management.NotificationListener Param_2)
            {
            }
        
        // From interface: com.tangosol.net.management.MBeanServerProxy
        public void removeNotificationListener(String Param_1, javax.management.NotificationListener Param_2, javax.management.NotificationFilter Param_3, Object Param_4)
            {
            }
        
        // From interface: com.tangosol.net.management.MBeanServerProxy
        public void setAttribute(String sName, String sAttr, Object oValue)
            {
            // import Component.Net.Management.Model.LocalModel;
            // import javax.management.InstanceNotFoundException;
            
            Local    gateway = (Local) get_Module();
            LocalModel model   = gateway.getLocalModel(sName);
            if (model == null)
                {
                throw new IllegalArgumentException("MBean " + sName + " does not exist");
                }
            gateway.setAttribute(sName, sAttr, oValue);
            }
        }
    }
