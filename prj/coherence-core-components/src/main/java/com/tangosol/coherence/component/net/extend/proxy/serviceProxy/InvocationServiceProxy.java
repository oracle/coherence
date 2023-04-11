
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.extend.proxy.serviceProxy.InvocationServiceProxy

package com.tangosol.coherence.component.net.extend.proxy.serviceProxy;

import com.tangosol.coherence.component.net.extend.message.request.InvocationServiceRequest;
import com.tangosol.coherence.component.net.extend.protocol.InvocationServiceProtocol;
import com.tangosol.coherence.config.ResolvableParameterList;
import com.tangosol.coherence.config.builder.InstanceBuilder;
import com.tangosol.coherence.config.builder.ParameterizedBuilder;
import com.tangosol.config.expression.NullParameterResolver;
import com.tangosol.config.expression.Parameter;
import com.tangosol.internal.net.service.extend.proxy.DefaultInvocationServiceProxyDependencies;
import com.tangosol.internal.net.service.extend.proxy.InvocationServiceProxyDependencies;
import com.tangosol.net.InvocationService;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;
import com.tangosol.util.Base;
import java.util.Collections;
import java.util.Iterator;

/**
 * The ServiceProxy is the base component of cluster-side handlers (Proxy) for
 * Services. It enables non-clustered clients to invoke Service methods within
 * the cluster.
 * 
 * @see Component.Net.Extend.RemoteService
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class InvocationServiceProxy
        extends    com.tangosol.coherence.component.net.extend.proxy.ServiceProxy
        implements com.tangosol.net.InvocationService
    {
    // ---- Fields declarations ----
    
    /**
     * Property InvocationService
     *
     * The InvocationService passed to an InvocationServiceRequest. If a custom
     * proxy has not been configured, this method returns this.
     */
    private com.tangosol.net.InvocationService __m_InvocationService;
    
    // Default constructor
    public InvocationServiceProxy()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public InvocationServiceProxy(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
        return new com.tangosol.coherence.component.net.extend.proxy.serviceProxy.InvocationServiceProxy();
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
            clz = Class.forName("com.tangosol.coherence/component/net/extend/proxy/serviceProxy/InvocationServiceProxy".replace('/', '.'));
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
        // import com.tangosol.internal.net.service.extend.proxy.DefaultInvocationServiceProxyDependencies;
        // import com.tangosol.internal.net.service.extend.proxy.InvocationServiceProxyDependencies;
        
        return new DefaultInvocationServiceProxyDependencies((InvocationServiceProxyDependencies) deps);
        }
    
    // From interface: com.tangosol.net.InvocationService
    public void execute(com.tangosol.net.Invocable task, java.util.Set set, com.tangosol.net.InvocationObserver observer)
        {
        throw new UnsupportedOperationException();
        }
    
    // Accessor for the property "InvocationService"
    /**
     * Getter for property InvocationService.<p>
    * The InvocationService passed to an InvocationServiceRequest. If a custom
    * proxy has not been configured, this method returns this.
     */
    public com.tangosol.net.InvocationService getInvocationService()
        {
        return __m_InvocationService;
        }
    
    // Declared at the super level
    public String getName()
        {
        return "InvocationServiceProxy";
        }
    
    // Declared at the super level
    public com.tangosol.net.messaging.Protocol getProtocol()
        {
        // import Component.Net.Extend.Protocol.InvocationServiceProtocol;
        
        return InvocationServiceProtocol.getInstance();
        }
    
    // Declared at the super level
    public String getServiceType()
        {
        // import com.tangosol.net.InvocationService;
        
        return InvocationService.TYPE_REMOTE;
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
        // import com.tangosol.coherence.config.ResolvableParameterList;
        // import com.tangosol.coherence.config.builder.InstanceBuilder;
        // import com.tangosol.coherence.config.builder.ParameterizedBuilder;
        // import com.tangosol.config.expression.NullParameterResolver;
        // import com.tangosol.config.expression.Parameter;
        // import com.tangosol.internal.net.service.extend.proxy.InvocationServiceProxyDependencies;
        // import com.tangosol.net.InvocationService;
        // import com.tangosol.run.xml.XmlElement;
        // import com.tangosol.run.xml.XmlHelper;
        // import com.tangosol.util.Base;
        // import java.util.Iterator;
        
        super.onDependencies(deps);
        
        InvocationServiceProxyDependencies proxyDeps = (InvocationServiceProxyDependencies) deps;
        
        // For ECCF based config, a custom service builder may be injected by CODI.
        // For DCCF, we are still using the XML for custom services.  
        ParameterizedBuilder bldrService  = proxyDeps.getServiceBuilder();
        if (bldrService == null)
            {
            // DCCF style
            XmlElement xml = proxyDeps.getServiceClassConfig();
            if (xml != null)
                {
                try
                    {
                    setInvocationService((InvocationService) XmlHelper.createInstance(xml,
                        Base.getContextClassLoader(), /*resolver*/ this, InvocationService.class));
                    }
                catch (Exception e)
                    {
                    throw ensureRuntimeException(e);
                    }
                }
            }
        else
            {
            // ECCF style - only an InstanceBuilder is supported
            ResolvableParameterList listParams = new ResolvableParameterList();
            listParams.add(new Parameter("cache-service", this));
        
            if (bldrService instanceof InstanceBuilder)
                {
                // Add any remaining params, skip the first param which is the service
                Iterator iterParams = ((InstanceBuilder) bldrService).getConstructorParameterList().iterator();
                if (iterParams.hasNext())
                    {
                    iterParams.next();
                    }     
                while (iterParams.hasNext())
                    {
                    listParams.add((Parameter) iterParams.next());
                    }
                }
            setInvocationService((InvocationService) bldrService.realize(new NullParameterResolver(),
                    Base.getContextClassLoader(), listParams));    
            }
        }
    
    // Declared at the super level
    /**
     * The "component has been initialized" method-notification called out of
    * setConstructed() for the topmost component and that in turn notifies all
    * the children.
    * 
    * This notification gets called before the control returns back to this
    * component instantiator (using <code>new Component.X()</code> or
    * <code>_newInstance(sName)</code>) and on the same thread. In addition,
    * visual components have a "posted" notification <code>onInitUI</code> that
    * is called after (or at the same time as) the control returns back to the
    * instantiator and possibly on a different thread.
     */
    public void onInit()
        {
        setInvocationService(this);
        
        super.onInit();
        }
    
    // Declared at the super level
    public void onMessage(com.tangosol.net.messaging.Message message)
        {
        // import Component.Net.Extend.Message.Request.InvocationServiceRequest;
        
        if (message instanceof InvocationServiceRequest)
            {
            InvocationServiceRequest request = (InvocationServiceRequest) message;
            request.setInvocationService(getInvocationService());
            }
        
        message.run();
        }
    
    // From interface: com.tangosol.net.InvocationService
    public java.util.Map query(com.tangosol.net.Invocable task, java.util.Set set)
        {
        // import java.util.Collections;
        
        if (set == null)
            {
            task.init(this);
            task.run();
            return Collections.singletonMap(getCluster().getLocalMember(), task.getResult());
            }
        else
            {
            throw new IllegalArgumentException("directed query not supported; "
                    + "the specified Member set must be null");
            }
        }
    
    // Declared at the super level
    public Object resolveParameter(String sType, String sValue)
        {
        // import com.tangosol.net.InvocationService;
        
        if (InvocationService.class.getName().equals(sType) && "{service}".equals(sValue))
            {
            return this;
            }
        
        return super.resolveParameter(sType, sValue);
        }
    
    // Declared at the super level
    /**
     * Setter for property Config.<p>
    * The XML configuration for this Adapter.
     */
    public void setConfig(com.tangosol.run.xml.XmlElement xml)
        {
        // import com.tangosol.internal.net.service.extend.proxy.DefaultInvocationServiceProxyDependencies;
        // import com.tangosol.internal.net.service.extend.proxy.LegacyXmlInvocationServiceProxyHelper as com.tangosol.internal.net.service.extend.proxy.LegacyXmlInvocationServiceProxyHelper;
        
        setDependencies(com.tangosol.internal.net.service.extend.proxy.LegacyXmlInvocationServiceProxyHelper.fromXml(xml, new DefaultInvocationServiceProxyDependencies()));
        }
    
    // Accessor for the property "InvocationService"
    /**
     * Setter for property InvocationService.<p>
    * The InvocationService passed to an InvocationServiceRequest. If a custom
    * proxy has not been configured, this method returns this.
     */
    protected void setInvocationService(com.tangosol.net.InvocationService service)
        {
        __m_InvocationService = service;
        }
    }
