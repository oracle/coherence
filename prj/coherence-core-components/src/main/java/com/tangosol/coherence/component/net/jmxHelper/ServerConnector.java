
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.jmxHelper.ServerConnector

package com.tangosol.coherence.component.net.jmxHelper;

import com.tangosol.coherence.component.net.Management;
import com.tangosol.util.Base;
import com.tangosol.util.ClassHelper;
import javax.management.MBeanServer;

/**
 * HttpAdapter to MBeanServer
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class ServerConnector
        extends    com.tangosol.coherence.component.net.JmxHelper
    {
    // ---- Fields declarations ----
    
    /**
     * Property Connector
     *
     * javax.management.remote.JMXConnectorServer
     */
    private transient Object __m_Connector;
    
    // Default constructor
    public ServerConnector()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public ServerConnector(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
        return new com.tangosol.coherence.component.net.jmxHelper.ServerConnector();
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
            clz = Class.forName("com.tangosol.coherence/component/net/jmxHelper/ServerConnector".replace('/', '.'));
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
    
    // Accessor for the property "Connector"
    /**
     * Getter for property Connector.<p>
    * javax.management.remote.JMXConnectorServer
     */
    public Object getConnector()
        {
        return __m_Connector;
        }
    
    // Accessor for the property "Connector"
    /**
     * Setter for property Connector.<p>
    * javax.management.remote.JMXConnectorServer
     */
    public void setConnector(Object pConnector)
        {
        __m_Connector = pConnector;
        }
    
    public void start(String sUrl, com.tangosol.net.Cluster cluster)
        {
        // import Component.Net.Management;
        // import com.tangosol.util.Base;
        // import com.tangosol.util.ClassHelper;
        // import javax.management.MBeanServer;
        // import javax.management.remote.JMXConnectorServerFactory;
        // import javax.management.remote.JMXConnectorServer;
        // import javax.management.remote.JMXServiceURL;
        
        Management mgmt = (Management) cluster.getManagement();
        if (mgmt == null)
            {
            throw new RuntimeException("Management is disabled");
            }
        
        MBeanServer server;
        try
            {
            server = (MBeanServer) ClassHelper.invoke(mgmt,
                "getServer", ClassHelper.VOID);
            }
        catch (Exception e)
            {
            throw new RuntimeException("Local management is disabled");
            }
        
        try
            {
            /*
            JMXServiceURL url = new JMXServiceURL(sUrl);
            JMXConnectorServer cs =
                  JMXConnectorServerFactory.newJMXConnectorServer(url, null, adapter.getServer());
            cs.start();
            _trace("ConnectorServer: " + cs.getAddress());
            */
            Object url = ClassHelper.newInstance(
                Class.forName("javax.management.remote.JMXServiceURL"),
                new Object[] {sUrl});
            Object cs  = ClassHelper.invokeStatic(
                Class.forName("javax.management.remote.JMXConnectorServerFactory"),
                "newJMXConnectorServer",
                new Object[] {url, null, server});
            ClassHelper.invoke(cs, "start", ClassHelper.VOID);
        
            setConnector(cs);
            }
        catch (Throwable e)
            {
            throw Base.ensureRuntimeException(e, "Failed to start the ServerConnector");
            }
        }
    
    public void stop()
        {
        // import com.tangosol.util.ClassHelper;
        
        try
            {
            ClassHelper.invoke(getConnector(), "stop", ClassHelper.VOID);
            }
        catch (Throwable e)
            {
            }
        }
    
    // Declared at the super level
    public String toString()
        {
        // import com.tangosol.util.ClassHelper;
        
        try
            {
            return "ConnectorServer: " +
                ClassHelper.invoke(getConnector(), "getAddress", ClassHelper.VOID);
            }
        catch (Throwable e)
            {
            return "ConnectorServer: " + getConnector();
            }
        }
    }
