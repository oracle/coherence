
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.management.gateway.Remote

package com.tangosol.coherence.component.net.management.gateway;

import com.tangosol.coherence.component.net.management.Connector;
import com.tangosol.coherence.component.net.management.model.LocalModel;
import com.tangosol.coherence.component.net.management.model.RemoteModel;
import com.tangosol.coherence.component.net.management.model.localModel.WrapperModel;
import com.tangosol.net.management.Registry;
import com.tangosol.util.Base;
import com.tangosol.util.ClassHelper;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;

/**
 * The base component for the Coherence Management framework implementation.
 * 
 * Remote Gateway provides an ability to register MBeans with a remote
 * MBeanServer.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class Remote
        extends    com.tangosol.coherence.component.net.management.Gateway
    {
    // ---- Fields declarations ----
    
    /**
     * Property Connector
     *
     * a reference to the connector.
     */
    private com.tangosol.coherence.component.net.management.Connector __m_Connector;
    
    /**
     * Property RedirectGateway
     *
     * This property could be not null only in the case of "dynamic" management
     * when this nodes is promoted to become a managing node. When the
     * RedirectGateway is set, this Remote gateway is not held by the
     * SafeCluster anymore and could only be kept alive for a short period of
     * time by some external registry-related logic.
     */
    private Local __m_RedirectGateway;
    
    /**
     * Property RemoteServers
     *
     * A set of Member objects for the managed nodes (have local MBeanServer)
     */
    private transient java.util.Set __m_RemoteServers;
    
    /**
     * Property Service
     *
     * Invocation service used for remote management.
     */
    private transient com.tangosol.net.InvocationService __m_Service;
    
    /**
     * Property Transitioning
     *
     * True iff register is for transitioning to managment senior; regster task
     * should not be sent to remote mbean server.
     */
    private boolean __m_Transitioning;
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
        __mapChildren.put("LocalMBeanServerProxy", Remote.LocalMBeanServerProxy.get_CLASS());
        }
    
    // Default constructor
    public Remote()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public Remote(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
        return new com.tangosol.coherence.component.net.management.gateway.Remote();
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
            clz = Class.forName("com.tangosol.coherence/component/net/management/gateway/Remote".replace('/', '.'));
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
        // import Component.Net.Management.Connector;
        // import Component.Net.Management.Model.LocalModel;
        // import Component.Net.Management.Model.RemoteModel;
        
        LocalModel model = getLocalModel(sName);
        if (model != null)
            {
            model._addNotificationListener(listener, filter, oHandback);
            }
        else
            {
            Connector conn = ensureRunningConnector();
        
            if (conn != null)
                {
                RemoteModel modelRemote = conn.ensureRemoteModel(sName, extractMemberId(sName));
        
                if (modelRemote == null)
                   {
                   // none of the members owns the specified MBean
                   throw new IllegalArgumentException("Unable to locate model for MBean " + sName);
                   }
        
                modelRemote._addNotificationListener(listener, filter, oHandback);
                }
            }
        }
    
    public com.tangosol.coherence.component.net.management.Connector ensureRunningConnector()
        {
        // import Component.Net.Management.Connector;
        
        Connector conn = null;
        if (getCluster().isRunning())
            {
            conn = getConnector();
            if (!conn.isStarted())
                {
                conn.startService(getCluster());
                }
            }
        return conn;
        }
    
    // Declared at the super level
    protected Object executeInternal(com.tangosol.util.function.Remote.Function function, com.oracle.coherence.common.base.Continuation cont)
        {
        // import Component.Net.Management.Connector;
        
        Connector conn = ensureRunningConnector();
        
        return conn == null ? null :
            conn.sendProxyRequest(conn.createExecuteRequest(function));
        }
    
    // Declared at the super level
    public Object getAttribute(String sName, String sAttr)
        {
        // import Component.Net.Management.Connector;
        // import Component.Net.Management.Model.LocalModel;
        
        Local gatewayRedirect = getRedirectGateway();
        
        LocalModel model = gatewayRedirect == null
                    ? getLocalModel(sName)
                    : gatewayRedirect.getLocalModel(sName);
        
        if (model != null)
            {
            return getLocalAttribute(model, sAttr);
            }
        
        Connector conn = ensureRunningConnector();
        
        return conn == null ? null :
            conn.sendProxyRequest(conn.createGetRequest(sName, sAttr));
        }
    
    // Declared at the super level
    public java.util.Map getAttributes(String sName, com.tangosol.util.Filter filter)
        {
        // import Component.Net.Management.Connector;
        // import Component.Net.Management.Model.LocalModel;
        // import java.util.Map;
        
        Local gatewayRedirect = getRedirectGateway();
        
        LocalModel model = gatewayRedirect == null
                    ? getLocalModel(sName)
                    : gatewayRedirect.getLocalModel(sName);
        
        if (model != null)
            {
            return getLocalAttributes(model, filter);
            }
        
        Connector conn = ensureRunningConnector();
        
        return conn == null ? null :
            (Map) conn.sendProxyRequest(conn.createGetRequest(sName, filter));
        }
    
    // Accessor for the property "Connector"
    /**
     * Getter for property Connector.<p>
    * a reference to the connector.
     */
    public com.tangosol.coherence.component.net.management.Connector getConnector()
        {
        return __m_Connector;
        }
    
    /**
     * Obtain the specified attribute from a local model.
     */
    public Object getLocalAttribute(com.tangosol.coherence.component.net.management.model.LocalModel model, String sAttr)
        {
        // import Component.Net.Management.Model.LocalModel.WrapperModel;
        // import Component.Net.Management.Model.RemoteModel;
        // import com.tangosol.util.Base;
        // import com.tangosol.util.ClassHelper;
        // import java.lang.reflect.Method;
        
        if (model instanceof WrapperModel)
            {
            try
                {
                return ((WrapperModel) model).invoke(RemoteModel.OP_GET, sAttr, null, null);
                }
            catch (NoSuchMethodException e)
                {
                // fall through
                }
            catch (Exception e)
                {
                throw Base.ensureRuntimeException(e);
                }
            }
        else
            {
            Method method = ClassHelper.findMethod(model.getClass(), "get" + sAttr, null, false);
            if (method == null)
                {
                method = ClassHelper.findMethod(model.getClass(), "is" + sAttr, null, false);
                }
        
            if (method != null)
                {
                try
                    {
                    return method.invoke(model, (Object[]) null);
                    }
                catch (Exception e)
                    {
                    throw Base.ensureRuntimeException(e.getCause());
                    }
                }
            }
        
        throw new IllegalArgumentException(
            "Unknown attribute " + sAttr + " for MBean " + model.get_ModelName());
        }
    
    public java.util.Map getLocalAttributes(com.tangosol.coherence.component.net.management.model.LocalModel model, com.tangosol.util.Filter filter)
        {
        // import Component.Net.Management.Model.LocalModel.WrapperModel;
        // import Component.Net.Management.Model.RemoteModel;
        // import com.tangosol.util.Base;
        // import java.lang.reflect.Method;
        // import java.util.HashMap;
        // import java.util.Map;
        // import javax.management.MBeanAttributeInfo;
        
        Map mapResult = new HashMap();
        try
            {
            if (model instanceof WrapperModel)
                {
                WrapperModel         bean  = (WrapperModel) model;
                MBeanAttributeInfo[] aAttr = bean.getMBeanInfo().getAttributes();
        
                for (int i = 0, c = aAttr.length; i < c; ++i)
                    {
                    String sAttr = aAttr[i].getName();
        
                    if (filter.evaluate(sAttr))
                        {
                        mapResult.put(sAttr, bean.invoke(RemoteModel.OP_GET, sAttr, null, null));
                        }
                    }
                }
            else
                {
                Method[] aMethod = model.getClass().getMethods();
            
                for (int i = 0, c = aMethod.length; i < c; ++i)
                    {
                    Method method = aMethod[i];
        
                    if (method.getParameterCount() == 0 && method.getReturnType() != Void.class)
                        {
                        String sName = method.getName();
                        String sAttr = null;
        
                        if (sName.startsWith("get"))
                            {
                            sAttr = sName.substring(3);
                            }
                        else if (sName.startsWith("is"))
                            {
                            sAttr = sName.substring(2);
                            }
        
                        if (sAttr != null && (filter == null || filter.evaluate(sAttr)))
                            {
                            mapResult.put(sAttr, method.invoke(model, (Object[]) null));
                            }
                        }
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
    /**
     * Getter for property LocalModels.<p>
    * A Map<String, LocalModel> of LocalModel components registered with this
    * gateway. The key is the MBean key property string (fully qualified name
    * sans the domain).
     */
    public synchronized java.util.Map getLocalModels()
        {
        Local gatewayRedirect = getRedirectGateway();
        return gatewayRedirect == null
            ? super.getLocalModels()
            : gatewayRedirect.getLocalModels();
        }
    
    // Declared at the super level
    public javax.management.MBeanInfo getMBeanInfo(String sName)
        {
        // import Component.Net.Management.Connector;
        // import Component.Net.Management.Model.LocalModel;
        // import javax.management.MBeanInfo;
        
        Local gatewayRedirect = getRedirectGateway();
        
        LocalModel model = gatewayRedirect == null
                    ? getLocalModel(sName)
                    : gatewayRedirect.getLocalModel(sName);
        
        if (model != null)
            {
            return instantiateModelMBean(model).getMBeanInfo();
            }
        
        Connector conn = ensureRunningConnector();
        
        return conn == null ? null :
            (MBeanInfo) conn.sendProxyRequest(conn.createGetMBeanInfoRequest(sName));
        }
    
    // Declared at the super level
    public com.tangosol.net.management.MBeanServerProxy getMBeanServerProxy()
        {
        Local gatewayRedirect = getRedirectGateway();
        return gatewayRedirect == null
            ? super.getMBeanServerProxy()
            : gatewayRedirect.getMBeanServerProxy();
        }
    
    // Declared at the super level
    /**
     * @inheritDoc
     */
    public synchronized com.tangosol.net.management.NotificationManager getNotificationManager()
        {
        Local gatewayRedirect = getRedirectGateway();
        return gatewayRedirect == null
            ? super.getNotificationManager()
            : gatewayRedirect.getNotificationManager();
        }
    
    // Accessor for the property "RedirectGateway"
    /**
     * Getter for property RedirectGateway.<p>
    * This property could be not null only in the case of "dynamic" management
    * when this nodes is promoted to become a managing node. When the
    * RedirectGateway is set, this Remote gateway is not held by the
    * SafeCluster anymore and could only be kept alive for a short period of
    * time by some external registry-related logic.
     */
    public Local getRedirectGateway()
        {
        return __m_RedirectGateway;
        }
    
    // Accessor for the property "RemoteServers"
    /**
     * Getter for property RemoteServers.<p>
    * A set of Member objects for the managed nodes (have local MBeanServer)
     */
    public java.util.Set getRemoteServers()
        {
        return __m_RemoteServers;
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
    
    /**
     * Instantiate a Remote gateway.
     */
    public static Remote instantiate(com.tangosol.coherence.component.util.SafeCluster cluster, com.tangosol.coherence.component.net.management.Connector connector, com.tangosol.internal.net.management.GatewayDependencies deps)
        {
        Remote gateway = new Remote();
        
        gateway.setCluster(cluster);
        gateway.setConnector(connector);
        gateway.setDependencies(deps);
        
        return gateway;
        }
    
    // Declared at the super level
    public Object invoke(String sName, String sMethodName, Object[] aoParam, String[] asSignature)
        {
        // import Component.Net.Management.Connector;
        // import Component.Net.Management.Model.LocalModel;
        
        Local gatewayRedirect = getRedirectGateway();
        
        LocalModel model = gatewayRedirect == null
                    ? getLocalModel(sName)
                    : gatewayRedirect.getLocalModel(sName);
        
        if (model != null)
            {
            return invokeLocal(model, sMethodName, aoParam, asSignature);
            }
        
        Connector conn = ensureRunningConnector();
        return conn == null ? null :
            conn.sendProxyRequest(conn.createInvokeRequest(sName, sMethodName, aoParam, asSignature));
        }
    
    /**
     * Invoke operation for a local model.
     */
    public Object invokeLocal(com.tangosol.coherence.component.net.management.model.LocalModel model, String sMethodName, Object[] aoParam, String[] asSignature)
        {
        // import Component.Net.Management.Model.RemoteModel;
        // import com.tangosol.util.Base;
        
        try
            {
            return model.invoke(RemoteModel.OP_INVOKE, sMethodName, aoParam, asSignature);
            }
        catch (NoSuchMethodException e)
            {
            throw new IllegalArgumentException(
                "Unknown operation " + sMethodName + " for MBean " + model.get_ModelName());
            }
        catch (Exception e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }
    
    /**
     * Check if the MBean name pattern could be resolved locally.
     */
    protected boolean isLocalPattern(String sPattern)
        {
        // import com.tangosol.net.management.Registry;
        
        // currently we only support PropertyListPattern (see ObjectName.isPropertyListPattern)
        // e.g.: ...nodeId=<local-id>,*
        
        int cChars = sPattern == null ? 0 : sPattern.length();
        
        return cChars > 0 && sPattern.charAt(cChars - 1) == '*' &&
                sPattern.contains(Registry.KEY_NODE_ID + getCluster().getLocalMember().getId() + ",");
        }
    
    // Declared at the super level
    public boolean isMBeanRegistered(String sName)
        {
        // import Component.Net.Management.Connector;
        // import Component.Net.Management.Model.LocalModel;
        
        Local gatewayRedirect = getRedirectGateway();
        
        LocalModel model = gatewayRedirect == null
                    ? getLocalModel(sName)
                    : gatewayRedirect.getLocalModel(sName);
        
        if (model != null)
            {
            return true;
            }
        
        Connector conn = ensureRunningConnector();
        
        return conn != null &&
            ((Boolean) conn.sendProxyRequest(conn.createIsRegisteredRequest(sName)))
            .booleanValue();
        }
    
    // Declared at the super level
    /**
     * @inheritDoc
     */
    public boolean isRegistered(String sName)
        {
        // import com.tangosol.net.management.Registry;
        
        if (sName.equals(ensureGlobalName(Registry.NODE_TYPE)))
            {
            // this path is reserved to ensure the management service is running
            // @see SimpleServiceMonitor#monitorServices
            ensureRunningConnector();
            }
        
        sName = extractTenantName(sName);
        
        // Note: we don't check for the RedirectGateway since it would use the same Connector anyway
        
        if (isGlobal(sName) && getCluster().isRunning())
            {
            return getConnector().isRegisteredModel(sName);
            }
        else
            {
            // does not really matter
            return false;
            }
        }
    
    // Accessor for the property "Transitioning"
    /**
     * Getter for property Transitioning.<p>
    * True iff register is for transitioning to managment senior; regster task
    * should not be sent to remote mbean server.
     */
    public boolean isTransitioning()
        {
        return __m_Transitioning;
        }
    
    // Declared at the super level
    public com.tangosol.net.management.MBeanServerProxy local()
        {
        return (Remote.LocalMBeanServerProxy) _newChild("LocalMBeanServerProxy");
        }
    
    // Declared at the super level
    public java.util.Set queryNames(String sPattern, com.tangosol.util.Filter filter)
        {
        // import Component.Net.Management.Connector;
        // import java.util.Collections;
        // import java.util.Set;
        
        if (isLocalPattern(sPattern))
            {
            return queryLocalNames(sPattern, filter);
            }
        
        Connector conn = ensureRunningConnector();
        
        return conn == null ? Collections.EMPTY_SET :
            (Set) conn.sendProxyRequest(conn.createQueryRequest(sPattern, filter));
        }
    
    // Declared at the super level
    public java.util.Set queryNames(javax.management.ObjectName oname, com.tangosol.util.Filter filter)
        {
        return queryNames(oname.getCanonicalName(), filter);
        }
    
    // Declared at the super level
    /**
     * Register LocalModel under a given name.
     */
    public void registerLocalModel(String sCanonicalName, com.tangosol.coherence.component.net.management.model.LocalModel model)
        {
        // import Component.Net.Management.Connector;
        
        synchronized (this)
            {
            Local gatewayRedirect = getRedirectGateway();
            if (gatewayRedirect == null)
                {
                super.registerLocalModel(sCanonicalName, model);
                registerMetrics(sCanonicalName);
                }
            else
                {
                gatewayRedirect.registerLocalModel(sCanonicalName, model);
        
                // the redirect gateway will call the connector
                return;
                }
            }
        
        if (isGlobal(sCanonicalName))
            {
            Connector conn = ensureRunningConnector();
            if (conn != null && !isTransitioning())
                {
                conn.registerModel(model);
                }
            }
        }
    
    // Declared at the super level
    public void removeNotificationListener(String sName, javax.management.NotificationListener listener)
        {
        // import Component.Net.Management.Connector;
        // import Component.Net.Management.Model.LocalModel;
        // import Component.Net.Management.Model.RemoteModel;
        
        LocalModel model = getLocalModel(sName);
        if (model != null)
            {
            model._removeNotificationListener(listener);
            }
        else
            {
            Connector conn = ensureRunningConnector();
        
            if (conn != null)
                {
                RemoteModel modelRemote = (RemoteModel) conn.getRemoteModels().get(sName);
        
                if (modelRemote != null)
                    {
                    modelRemote._removeNotificationListener(listener); 
                    }
                }
            }
        }
    
    // Declared at the super level
    public void removeNotificationListener(String sName, javax.management.NotificationListener listener, javax.management.NotificationFilter filter, Object oHandback)
        {
        // import Component.Net.Management.Connector;
        // import Component.Net.Management.Model.LocalModel;
        // import Component.Net.Management.Model.RemoteModel;
        
        LocalModel model = getLocalModel(sName);
        if (model != null)
            {
            model._removeNotificationListener(listener, filter, oHandback);
            }
        else
            {
            Connector conn = ensureRunningConnector();
        
            if (conn != null)
                {
                RemoteModel modelRemote = (RemoteModel) conn.getRemoteModels().get(sName);
        
                if (modelRemote != null)
                    {
                    modelRemote._removeNotificationListener(listener, filter, oHandback); 
                    }
                }
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
        super.reset();
        
        getConnector().getLocalRegistry().clear();
        }
    
    // Declared at the super level
    public void setAttribute(String sName, String sAttr, Object oValue)
        {
        // import Component.Net.Management.Connector;
        // import Component.Net.Management.Model.LocalModel;
        
        Local gatewayRedirect = getRedirectGateway();
        
        LocalModel model = gatewayRedirect == null
                    ? getLocalModel(sName)
                    : gatewayRedirect.getLocalModel(sName);
        
        if (model != null)
            {
            setLocalAttribute(model, sAttr, oValue);
            return;
            }
        
        Connector conn = ensureRunningConnector();
        if (conn != null)
            {
            conn.sendProxyRequest(conn.createSetRequest(sName, sAttr, oValue));
            }
        }
    
    // Accessor for the property "Connector"
    /**
     * Setter for property Connector.<p>
    * a reference to the connector.
     */
    public void setConnector(com.tangosol.coherence.component.net.management.Connector connector)
        {
        __m_Connector = connector;
        }
    
    /**
     * Set the specified attribute for a local model.
     */
    public void setLocalAttribute(com.tangosol.coherence.component.net.management.model.LocalModel model, String sAttr, Object oValue)
        {
        // import Component.Net.Management.Model.LocalModel.WrapperModel;
        // import Component.Net.Management.Model.RemoteModel;
        // import com.tangosol.util.Base;
        // import com.tangosol.util.ClassHelper;
        
        try
            {
            if (model instanceof WrapperModel)
                {
                ((WrapperModel) model).invoke(RemoteModel.OP_SET, sAttr, new Object[] {oValue}, null);
                }
            else
                {
                ClassHelper.invoke(model, "set" + sAttr, new Object[] {oValue});
                }
            }
        catch (NoSuchMethodException e)
            {
            throw new IllegalArgumentException(
                "Unknown or read-only attribute " + sAttr + " for MBean " + model.get_ModelName());
            }
        catch (Exception e)
            {
            throw Base.ensureRuntimeException(e.getCause());
            }
        }
    
    // Accessor for the property "RedirectGateway"
    /**
     * Setter for property RedirectGateway.<p>
    * This property could be not null only in the case of "dynamic" management
    * when this nodes is promoted to become a managing node. When the
    * RedirectGateway is set, this Remote gateway is not held by the
    * SafeCluster anymore and could only be kept alive for a short period of
    * time by some external registry-related logic.
     */
    public void setRedirectGateway(Local localGateway)
        {
        __m_RedirectGateway = localGateway;
        }
    
    // Accessor for the property "RemoteServers"
    /**
     * Setter for property RemoteServers.<p>
    * A set of Member objects for the managed nodes (have local MBeanServer)
     */
    protected void setRemoteServers(java.util.Set setMember)
        {
        __m_RemoteServers = setMember;
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
    
    // Accessor for the property "Transitioning"
    /**
     * Setter for property Transitioning.<p>
    * True iff register is for transitioning to managment senior; regster task
    * should not be sent to remote mbean server.
     */
    public void setTransitioning(boolean fTransitioning)
        {
        __m_Transitioning = fTransitioning;
        }
    
    /**
     * Transition the management topology from type (6) to type (4) as described
    * in Gateway.createGateway documentation. This method is called only if the
    * management type is "dynamic".
     */
    public synchronized void transitionToManaging()
        {
        // import Component.Net.Management.Connector;
        // import Component.Net.Management.Model.LocalModel;
        // import Component.Util.SafeCluster as com.tangosol.coherence.component.util.SafeCluster;
        // import com.tangosol.internal.net.management.GatewayDependencies as com.tangosol.internal.net.management.GatewayDependencies;
        // import java.util.Iterator;
        
        // This Remote Gateway needs to orchestrate the transition from a chain:
        //      com.tangosol.coherence.component.util.SafeCluster -> Remote -> Connector -> null
        // to:
        //      com.tangosol.coherence.component.util.SafeCluster -> Local -> Remote' -> Connector -> Local
        // where the Remote' is a "clone" of this one
        
        com.tangosol.coherence.component.util.SafeCluster       cluster   = (com.tangosol.coherence.component.util.SafeCluster) getCluster();
        Connector     connector = getConnector();
        com.tangosol.internal.net.management.GatewayDependencies  deps      = getDependencies();
        
        Remote gatewayRemote = Remote.instantiate(cluster, connector, deps);
        Local  gatewayLocal  = Local.instantiate(cluster, connector, deps);
        
        // re-register is local only
        gatewayRemote.setTransitioning(true);
        
        gatewayLocal.setRemoteGateway(gatewayRemote);
        
        // re-register all local models
        for (Iterator iter = getLocalModels().values().iterator(); iter.hasNext(); )
            {
            LocalModel model = (LocalModel) iter.next();
        
            gatewayLocal.registerLocalModel(model.get_ModelName(), model);
            }
        
        gatewayLocal.getRegisteredHealthChecks().putAll(getRegisteredHealthChecks());
        
        gatewayRemote.setTransitioning(false);
        
        connector.setLocalGateway(gatewayLocal);
        connector.setManagingNode(true);
        
        setRedirectGateway(gatewayLocal);
        
        cluster.setManagement(gatewayLocal);
        }
    
    // Declared at the super level
    /**
     * @inheritDoc
     */
    public synchronized void trigger(String sName, javax.management.Notification notification)
            throws java.lang.IllegalArgumentException
        {
        super.trigger(sName, notification);
        }
    
    // Declared at the super level
    public void unregisterLocalModel(String sCanonicalName)
        {
        synchronized (this)
            {
            Local gatewayRedirect = getRedirectGateway();
            if (gatewayRedirect == null)
                {
                super.unregisterLocalModel(sCanonicalName);
                }
            else
                {
                gatewayRedirect.unregisterLocalModel(sCanonicalName);
        
                // the redirect gateway will call the connector
                return;
                }
            }
        
        if (isGlobal(sCanonicalName))
            {
            getConnector().unregisterModel(sCanonicalName);
            }
        }

    // ---- class: com.tangosol.coherence.component.net.management.gateway.Remote$LocalMBeanServerProxy
    
    /**
     * An MBeanServerProxy implementation that sees only the MBeans that are
     * local to this member.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class LocalMBeanServerProxy
            extends    com.tangosol.coherence.component.Net
            implements com.tangosol.net.management.MBeanServerProxy
        {
        // ---- Fields declarations ----
        
        /**
         * Property Remote
         *
         */
        private Remote __m_Remote;
        
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
            return new com.tangosol.coherence.component.net.management.gateway.Remote.LocalMBeanServerProxy();
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
                clz = Class.forName("com.tangosol.coherence/component/net/management/gateway/Remote$LocalMBeanServerProxy".replace('/', '.'));
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
            
            Remote    gateway = (Remote) get_Module();
            LocalModel model   = gateway.getLocalModel(sName);
            if (model == null)
                {
                throw new IllegalArgumentException("MBean " + sName + " does not exist");
                }
            return gateway.getLocalAttribute(model, sAttr);
            }
        
        // From interface: com.tangosol.net.management.MBeanServerProxy
        public java.util.Map getAttributes(String sName, com.tangosol.util.Filter filter)
            {
            // import Component.Net.Management.Model.LocalModel;
            // import java.util.Collections;
            
            Remote    gateway = (Remote) get_Module();
            LocalModel model   = gateway.getLocalModel(sName);
            if (model == null)
                {
                throw new IllegalArgumentException("MBean " + sName + " does not exist");
                }
            return gateway.getLocalAttributes(model, filter);
            }
        
        // From interface: com.tangosol.net.management.MBeanServerProxy
        public javax.management.MBeanInfo getMBeanInfo(String sName)
            {
            // import Component.Net.Management.Model.LocalModel;
            // import javax.management.MBeanInfo;
            
            Remote    gateway = (Remote) get_Module();
            LocalModel model   = gateway.getLocalModel(sName);
            if (model == null)
                {
                throw new IllegalArgumentException("MBean " + sName + " does not exist");
                }
            return gateway.getMBeanInfo(sName);
            }
        
        // Accessor for the property "Remote"
        /**
         * Getter for property Remote.<p>
         */
        public Remote getRemote()
            {
            return __m_Remote;
            }
        
        // From interface: com.tangosol.net.management.MBeanServerProxy
        public Object invoke(String sName, String sMethodName, Object[] aoParam, String[] asSignature)
            {
            // import Component.Net.Management.Connector;
            // import Component.Net.Management.Model.LocalModel;
            
            Remote    gateway = (Remote) get_Module();
            LocalModel model   = gateway.getLocalModel(sName);
            if (model == null)
                {
                throw new IllegalArgumentException("MBean " + sName + " does not exist");
                }
            return gateway.invokeLocal(model, sMethodName, aoParam, asSignature);
            }
        
        // From interface: com.tangosol.net.management.MBeanServerProxy
        public boolean isMBeanRegistered(String sName)
            {
            // import Component.Net.Management.Model.LocalModel;
            
            Remote    gateway = (Remote) get_Module();
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
            Remote gateway = (Remote) get_Module();
            return gateway.queryLocalNames(sPattern, filter);
            }
        
        // From interface: com.tangosol.net.management.MBeanServerProxy
        public java.util.Set queryNames(javax.management.ObjectName oname, com.tangosol.util.Filter filter)
            {
            Remote gateway = (Remote) get_Module();
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
            
            Remote    gateway = (Remote) get_Module();
            LocalModel model   = gateway.getLocalModel(sName);
            if (model == null)
                {
                throw new IllegalArgumentException("MBean " + sName + " does not exist");
                }
            gateway.setLocalAttribute(model, sAttr, oValue);
            }
        
        // Accessor for the property "Remote"
        /**
         * Setter for property Remote.<p>
         */
        public void setRemote(Remote sProperty)
            {
            __m_Remote = sProperty;
            }
        }
    }
