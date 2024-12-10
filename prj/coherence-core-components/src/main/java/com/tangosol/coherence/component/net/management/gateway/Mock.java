
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.management.gateway.Mock

package com.tangosol.coherence.component.net.management.gateway;

import com.tangosol.coherence.component.net.management.Connector;
import com.tangosol.coherence.component.net.management.model.RemoteModel;
import com.tangosol.net.management.Registry;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * The base component for the Coherence Management framework implementation.
 * 
 * Mock object is a "null implementation" registry.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class Mock
        extends    com.tangosol.coherence.component.net.management.Gateway
    {
    // ---- Fields declarations ----
    
    /**
     * Property Connector
     *
     * a reference to the connector.
     */
    private com.tangosol.coherence.component.net.management.Connector __m_Connector;
    
    // Default constructor
    public Mock()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public Mock(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
        return new com.tangosol.coherence.component.net.management.gateway.Mock();
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
            clz = Class.forName("com.tangosol.coherence/component/net/management/gateway/Mock".replace('/', '.'));
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
    
    // Declared at the super level
    public void addNotificationListener(String sName, javax.management.NotificationListener listener, javax.management.NotificationFilter filter, Object oHandback)
        {
        // import Component.Net.Management.Connector;
        // import Component.Net.Management.Model.RemoteModel;
        
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
    /**
     * @inheritDoc
     */
    public Object getAttribute(String sName, String sAttr)
        {
        // import Component.Net.Management.Connector;
        
        Connector conn = ensureRunningConnector();
        
        return conn == null ? null :
            conn.sendProxyRequest(conn.createGetRequest(sName, sAttr));
        }
    
    // Declared at the super level
    public java.util.Map getAttributes(String sName, com.tangosol.util.Filter filter)
        {
        // import Component.Net.Management.Connector;
        // import java.util.Map;
        
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
    
    // Declared at the super level
    /**
     * @inheritDoc
     */
    public String getDomainName()
        {
        Local localGateway = getLocalGateway();
        return localGateway == null ?
            super.getDomainName() : localGateway.getDomainName();
        }
    
    // Accessor for the property "LocalGateway"
    /**
     * Getter for property LocalGateway.<p>
    * The local (MBeanServer bound) gateway.
     */
    public Local getLocalGateway()
        {
        // import Component.Net.Management.Connector;
        
        Connector conn = getConnector();
        return conn == null ? null : conn.getLocalGateway();
        }
    
    // Declared at the super level
    public Object invoke(String sName, String sMethodName, Object[] aoParam, String[] asSignature)
        {
        // import Component.Net.Management.Connector;
        // import Component.Net.Management.Connector$InvokeRemote as com.tangosol.coherence.component.net.management.Connector.InvokeRemote;
        
        Connector conn = ensureRunningConnector();
        return conn == null ? null :
            conn.sendProxyRequest(conn.createInvokeRequest(sName, sMethodName, aoParam, asSignature));
        }
    
    // Declared at the super level
    public boolean isMBeanRegistered(String sName)
        {
        // import Component.Net.Management.Connector;
        // import Component.Net.Management.Connector$InvokeRemote as com.tangosol.coherence.component.net.management.Connector.InvokeRemote;
        
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
        
        try
            {
            return !isGlobal(sName)
                && getLocalGateway().isRegistered(extractTenantName(sName));
            }
        catch (NullPointerException e)
            {
            return false;
            }
        }
    
    // Declared at the super level
    public java.util.Set queryNames(String sPattern, com.tangosol.util.Filter filter)
        {
        // import Component.Net.Management.Connector;
        // import java.util.Collections;
        // import java.util.Set;
        
        Connector conn = ensureRunningConnector();
        
        return conn == null ? Collections.EMPTY_SET :
            (Set) conn.sendProxyRequest(conn.createQueryRequest(sPattern, filter));
        }
    
    // Declared at the super level
    /**
     * Register LocalModel under a given name.
     */
    public void registerLocalModel(String sCanonicalName, com.tangosol.coherence.component.net.management.model.LocalModel model)
        {
        // don't call super() as the global models are not registered remotely
        
        if (isGlobal(sCanonicalName))
            {
            ensureRunningConnector();
            }
        else if (isPrimary())
            {
            Local localGateway = getLocalGateway();
            if (localGateway != null)
                {
                localGateway.registerLocalModel(sCanonicalName, model);
                }
            }
        }
    
    // Declared at the super level
    public void registerReporter()
        {
        Local localGateway = getLocalGateway();
        if (localGateway != null)
            {
            localGateway.registerReporter();
            }
        }
    
    // Declared at the super level
    public void removeNotificationListener(String sName, javax.management.NotificationListener listener)
        {
        // import Component.Net.Management.Connector;
        // import Component.Net.Management.Model.RemoteModel;
        
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
    
    // Declared at the super level
    public void removeNotificationListener(String sName, javax.management.NotificationListener listener, javax.management.NotificationFilter filter, Object oHandback)
        {
        // import Component.Net.Management.Connector;
        // import Component.Net.Management.Model.RemoteModel;
        
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
    
    // Declared at the super level
    /**
     * @inheritDoc
     */
    public void setAttribute(String sName, String sAttr, Object oValue)
        {
        // import Component.Net.Management.Connector;
        // import Component.Net.Management.Connector$InvokeRemote as com.tangosol.coherence.component.net.management.Connector.InvokeRemote;
        
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
    
    // Declared at the super level
    public void unregisterLocalModel(String sCanonicalName)
        {
        // don't call super() as the global moders are not registered
        
        if (!isGlobal(sCanonicalName) && isPrimary())
            {
            Local localGateway = getLocalGateway();
            if (localGateway != null)
                {
                localGateway.unregisterLocalModel(sCanonicalName);
                }
            }
        }
    }
