
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.extend.proxy.serviceProxy.NameServiceProxy

package com.tangosol.coherence.component.net.extend.proxy.serviceProxy;

import com.tangosol.coherence.component.net.extend.message.request.NameServiceRequest;
import com.tangosol.coherence.component.net.extend.protocol.NameServiceProtocol;
import com.tangosol.net.NameService;

/**
 * Reciever implementation for the NameService Protocol.
 * 
 * The NameServiceProxy is the cluster-side handler (Proxy) for a
 * RemoteNameService. It enables non-clustered clients to look up resources by
 * name.
 * 
 * @see Component.Net.Extend.RemoteService.RemoteNameService
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class NameServiceProxy
        extends    com.tangosol.coherence.component.net.extend.proxy.ServiceProxy
    {
    // ---- Fields declarations ----
    
    /**
     * Property NameService
     *
     * The NameService passed to a NameServiceRequest. If a custom proxy has
     * not been configured, this property referes to this.
     */
    private com.tangosol.net.NameService __m_NameService;
    
    // Default constructor
    public NameServiceProxy()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public NameServiceProxy(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            setEnabled(true);
            setServiceVersion("14");
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
        return new com.tangosol.coherence.component.net.extend.proxy.serviceProxy.NameServiceProxy();
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
            clz = Class.forName("com.tangosol.coherence/component/net/extend/proxy/serviceProxy/NameServiceProxy".replace('/', '.'));
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
    public String getName()
        {
        return "NameService";
        }
    
    // Accessor for the property "NameService"
    /**
     * Getter for property NameService.<p>
    * The NameService passed to a NameServiceRequest. If a custom proxy has not
    * been configured, this property referes to this.
     */
    public com.tangosol.net.NameService getNameService()
        {
        return __m_NameService;
        }
    
    // Declared at the super level
    public com.tangosol.net.messaging.Protocol getProtocol()
        {
        // import Component.Net.Extend.Protocol.NameServiceProtocol;
        
        return NameServiceProtocol.getInstance();
        }
    
    // Declared at the super level
    public String getServiceType()
        {
        // import com.tangosol.net.NameService;
        
        return NameService.TYPE_REMOTE;
        }
    
    // Declared at the super level
    public void onMessage(com.tangosol.net.messaging.Message message)
        {
        // import Component.Net.Extend.Message.Request.NameServiceRequest;
        
        if (message instanceof NameServiceRequest)
            {
            NameServiceRequest request = (NameServiceRequest) message;
            request.setNameService(getNameService());
            }
        
        message.run();
        }
    
    // Accessor for the property "NameService"
    /**
     * Setter for property NameService.<p>
    * The NameService passed to a NameServiceRequest. If a custom proxy has not
    * been configured, this property referes to this.
     */
    public void setNameService(com.tangosol.net.NameService service)
        {
        __m_NameService = service;
        }
    }
