
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.extend.Proxy

package com.tangosol.coherence.component.net.extend;

import com.tangosol.internal.net.service.extend.proxy.DefaultProxyDependencies;
import com.tangosol.internal.net.service.extend.proxy.ProxyDependencies;

/**
 * Base component for all Proxy components used by the ProxyService. A Proxy is
 * a cluster-side handler for remote service requests. 
 * 
 * @see Component.Util.Daemon.QueueProcessor.Service.Grid.ProxyService
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public abstract class Proxy
        extends    com.tangosol.coherence.component.net.Extend
        implements com.tangosol.run.xml.XmlConfigurable
    {
    // ---- Fields declarations ----
    
    /**
     * Property Config
     *
     * The XML configuration for this Adapter.
     */
    private com.tangosol.run.xml.XmlElement __m_Config;
    
    /**
     * Property DaemonPool
     *
     * 
     * DaemonPool used to cleanup the proxy when it has been released or
     * disconnected.
     * 
     * The pool runs shutdowns tasks for releasing listeners and locks.
     * 
     */
    private Object __m_DaemonPool;
    
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
     * Property Enabled
     *
     * True iff this Proxy has been enabled.
     * 
     * @see setConfig()
     */
    private boolean __m_Enabled;
    
    // Initializing constructor
    public Proxy(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            clz = Class.forName("com.tangosol.coherence/component/net/extend/Proxy".replace('/', '.'));
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
    
    /**
     * Create a new Default dependencies object by cloning the input
    * dependencies.  Each class or component that uses dependencies implements
    * a Default dependencies class which provides the clone functionality.  
    * The dependency injection design pattern requires every component in the
    * component hierarchy to implement clone.
    * 
    * @return DefaultProxyDependencies  the cloned dependencies
     */
    protected com.tangosol.internal.net.service.extend.proxy.DefaultProxyDependencies cloneDependencies(com.tangosol.internal.net.service.extend.proxy.ProxyDependencies deps)
        {
        // import com.tangosol.internal.net.service.extend.proxy.DefaultProxyDependencies;
        
        return new DefaultProxyDependencies(deps);
        }
    
    // From interface: com.tangosol.run.xml.XmlConfigurable
    // Accessor for the property "Config"
    /**
     * Getter for property Config.<p>
    * The XML configuration for this Adapter.
     */
    public com.tangosol.run.xml.XmlElement getConfig()
        {
        return __m_Config;
        }
    
    // Accessor for the property "DaemonPool"
    /**
     * Getter for property DaemonPool.<p>
    * 
    * DaemonPool used to cleanup the proxy when it has been released or
    * disconnected.
    * 
    * The pool runs shutdowns tasks for releasing listeners and locks.
    * 
     */
    public Object getDaemonPool()
        {
        return __m_DaemonPool;
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
    public com.tangosol.net.ServiceDependencies getDependencies()
        {
        return __m_Dependencies;
        }
    
    // Accessor for the property "Enabled"
    /**
     * Getter for property Enabled.<p>
    * True iff this Proxy has been enabled.
    * 
    * @see setConfig()
     */
    public boolean isEnabled()
        {
        return __m_Enabled;
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
    * CacheServiceProxy dependencies.  The advantage to this technique is that
    * the property only exists in the dependencies object, it is not duplicated
    * in the component properties.
    * 
    * CacheServiceProxyDependencies deps = (CacheServiceProxyDependencies)
    * getDependencies();
    * deps.getFoo();
     */
    protected void onDependencies(com.tangosol.internal.net.service.extend.proxy.ProxyDependencies deps)
        {
        setEnabled(deps.isEnabled());
        }
    
    // From interface: com.tangosol.run.xml.XmlConfigurable
    // Accessor for the property "Config"
    /**
     * Setter for property Config.<p>
    * The XML configuration for this Adapter.
     */
    public void setConfig(com.tangosol.run.xml.XmlElement xml)
        {
        // import com.tangosol.internal.net.service.extend.proxy.DefaultProxyDependencies;
        // import com.tangosol.internal.net.service.extend.proxy.LegacyXmlProxyHelper as com.tangosol.internal.net.service.extend.proxy.LegacyXmlProxyHelper;
        
        __m_Config = (xml);
        
        setDependencies(com.tangosol.internal.net.service.extend.proxy.LegacyXmlProxyHelper.fromXml(xml, new DefaultProxyDependencies()));
        }
    
    // Accessor for the property "DaemonPool"
    /**
     * Setter for property DaemonPool.<p>
    * 
    * DaemonPool used to cleanup the proxy when it has been released or
    * disconnected.
    * 
    * The pool runs shutdowns tasks for releasing listeners and locks.
    * 
     */
    public void setDaemonPool(Object pool)
        {
        __m_DaemonPool = pool;
        }
    
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
        // import com.tangosol.internal.net.service.extend.proxy.ProxyDependencies;
        
        if (getDependencies() != null)
            {
            throw new IllegalStateException("Dependencies already set");
            }
        
        __m_Dependencies = (cloneDependencies((ProxyDependencies) deps).validate());
        
        // use the cloned dependencies
        onDependencies((ProxyDependencies) getDependencies());
        }
    
    // Accessor for the property "Enabled"
    /**
     * Setter for property Enabled.<p>
    * True iff this Proxy has been enabled.
    * 
    * @see setConfig()
     */
    protected void setEnabled(boolean fEnabled)
        {
        __m_Enabled = fEnabled;
        }
    }
