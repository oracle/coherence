
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.jmxHelper.HttpAdapter

package com.tangosol.coherence.component.net.jmxHelper;

import com.tangosol.coherence.component.net.Management;
import com.tangosol.util.Base;
import com.tangosol.util.ClassHelper;
import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

/**
 * Wrapper around com.sun.jdmk.comm.HtmlAdaptorServer class.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class HttpAdapter
        extends    com.tangosol.coherence.component.net.JmxHelper
    {
    // ---- Fields declarations ----
    
    /**
     * Property AdapterMName
     *
     * Adapter's name.
     */
    private transient javax.management.ObjectName __m_AdapterMName;
    
    /**
     * Property Server
     *
     * Adapter's server.
     */
    private transient javax.management.MBeanServer __m_Server;
    
    // Default constructor
    public HttpAdapter()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public HttpAdapter(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
        return new com.tangosol.coherence.component.net.jmxHelper.HttpAdapter();
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
            clz = Class.forName("com.tangosol.coherence/component/net/jmxHelper/HttpAdapter".replace('/', '.'));
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
    
    // Accessor for the property "AdapterMName"
    /**
     * Getter for property AdapterMName.<p>
    * Adapter's name.
     */
    public javax.management.ObjectName getAdapterMName()
        {
        return __m_AdapterMName;
        }
    
    // Accessor for the property "Server"
    /**
     * Getter for property Server.<p>
    * Adapter's server.
     */
    public javax.management.MBeanServer getServer()
        {
        return __m_Server;
        }
    
    // Accessor for the property "AdapterMName"
    /**
     * Setter for property AdapterMName.<p>
    * Adapter's name.
     */
    protected void setAdapterMName(javax.management.ObjectName name)
        {
        __m_AdapterMName = name;
        }
    
    // Accessor for the property "Server"
    /**
     * Setter for property Server.<p>
    * Adapter's server.
     */
    protected void setServer(javax.management.MBeanServer server)
        {
        __m_Server = server;
        }
    
    public void start(int nPort, com.tangosol.net.Cluster cluster)
        {
        // import Component.Net.Management;
        // import com.tangosol.util.Base;
        // import com.tangosol.util.ClassHelper;
        // import javax.management.InstanceAlreadyExistsException;
        // import javax.management.MBeanServer;
        // import javax.management.ObjectName;
        
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
            String     sAdapter     = "HttpAdapter:port=" + nPort;
            ObjectName nameAdapter  = new ObjectName(sAdapter);
            Object     mbeanAdapter = ClassHelper.newInstance(
                Class.forName("com.sun.jdmk.comm.HtmlAdaptorServer"),
                new Object[] {Base.makeInteger(nPort)});
        
            server.registerMBean(mbeanAdapter, nameAdapter);
            server.invoke(nameAdapter, "start", null, null);
        
            setServer(server);
            setAdapterMName(nameAdapter);
            }
        catch (InstanceAlreadyExistsException e)
            {
            }
        catch (Exception e)
            {
            throw Base.ensureRuntimeException(e, "Failed to start the HttpAdapter");
            }
        }
    
    public void stop()
        {
        // import javax.management.MBeanServer;
        
        MBeanServer server = getServer();
        if (server != null)
            {
            try
                {
                server.invoke(getAdapterMName(), "stop", null, null);
                server.unregisterMBean(getAdapterMName());
                }
            catch (Exception e)
                {
                }
            }
        }
    
    // Declared at the super level
    public String toString()
        {
        return String.valueOf(getAdapterMName());
        }
    }
