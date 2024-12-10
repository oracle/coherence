
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.extend.remoteService.RemoteInvocationService

package com.tangosol.coherence.component.net.extend.remoteService;

import com.tangosol.coherence.component.net.extend.protocol.InvocationServiceProtocol;
import com.tangosol.internal.net.service.extend.remote.DefaultRemoteInvocationServiceDependencies;
import com.tangosol.internal.net.service.extend.remote.RemoteInvocationServiceDependencies;
import com.tangosol.net.InvocationService;
import com.tangosol.net.Member;
import com.tangosol.net.messaging.Channel;
import com.tangosol.net.messaging.Connection;
import com.tangosol.net.messaging.ConnectionInitiator;
import com.tangosol.net.security.SecurityHelper;
import java.util.Collections;

/**
 * InvocationService implementation that allows a JVM to use a remote
 * InvocationService without having to join the Cluster.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class RemoteInvocationService
        extends    com.tangosol.coherence.component.net.extend.RemoteService
        implements com.tangosol.net.InvocationService
    {
    // ---- Fields declarations ----
    
    // Default constructor
    public RemoteInvocationService()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public RemoteInvocationService(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            setMemberListeners(new com.tangosol.util.Listeners());
            setResourceRegistry(new com.tangosol.util.SimpleResourceRegistry());
            setServiceListeners(new com.tangosol.util.Listeners());
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
        return new com.tangosol.coherence.component.net.extend.remoteService.RemoteInvocationService();
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
            clz = Class.forName("com.tangosol.coherence/component/net/extend/remoteService/RemoteInvocationService".replace('/', '.'));
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
    * @return DefaultRemoteServiceDependencies  the cloned dependencies
     */
    protected com.tangosol.internal.net.service.extend.remote.DefaultRemoteServiceDependencies cloneDependencies(com.tangosol.internal.net.service.extend.remote.RemoteServiceDependencies deps)
        {
        // import com.tangosol.internal.net.service.extend.remote.DefaultRemoteInvocationServiceDependencies;
        // import com.tangosol.internal.net.service.extend.remote.RemoteInvocationServiceDependencies;
        
        return new DefaultRemoteInvocationServiceDependencies((RemoteInvocationServiceDependencies) deps);
        }
    
    // Declared at the super level
    /**
     * The configure() implementation method. This method must only be called by
    * a thread that has synchronized on this RemoteService.
    * 
    * @param xml  the XmlElement containing the new configuration for this
    * RemoteService
     */
    protected void doConfigure(com.tangosol.run.xml.XmlElement xml)
        {
        // import com.tangosol.internal.net.service.extend.remote.DefaultRemoteInvocationServiceDependencies;
        // import com.tangosol.internal.net.service.extend.remote.LegacyXmlRemoteInvocationServiceHelper as com.tangosol.internal.net.service.extend.remote.LegacyXmlRemoteInvocationServiceHelper;
        
        setDependencies(com.tangosol.internal.net.service.extend.remote.LegacyXmlRemoteInvocationServiceHelper.fromXml(xml,
            new DefaultRemoteInvocationServiceDependencies(), getOperationalContext(),
            getContextClassLoader()));
        }
    
    // From interface: com.tangosol.net.InvocationService
    public void execute(com.tangosol.net.Invocable task, java.util.Set setMembers, com.tangosol.net.InvocationObserver observer)
        {
        throw new UnsupportedOperationException();
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
    * RemoteCacheService dependencies.  The advantage to this technique is that
    * the property only exists in the dependencies object, it is not duplicated
    * in the component properties.
    * 
    * RemoteCacheServiceDependencies deps = (RemoteCacheServiceDependencies)
    * getDependencies();
    * deps.getFoo();
     */
    protected void onDependencies(com.tangosol.internal.net.service.extend.remote.RemoteServiceDependencies deps)
        {
        // import Component.Net.Extend.Protocol.InvocationServiceProtocol;
        // import com.tangosol.net.messaging.ConnectionInitiator;
        
        super.onDependencies(deps);
        
        // register all Protocols
        ConnectionInitiator initiator = getInitiator();
        initiator.registerProtocol(InvocationServiceProtocol.getInstance());
        }
    
    // Declared at the super level
    /**
     * Open a Channel to the remote ProxyService.
     */
    protected com.tangosol.net.messaging.Channel openChannel()
        {
        // import Component.Net.Extend.Protocol.InvocationServiceProtocol;
        // import com.tangosol.net.messaging.Connection;
        // import com.tangosol.net.security.SecurityHelper;
        
        lookupProxyServiceAddress();
        
        Connection connection = getInitiator().ensureConnection();
        return connection.openChannel(InvocationServiceProtocol.getInstance(),
                "InvocationServiceProxy",
                null,
                null,
                SecurityHelper.getCurrentSubject());
        }
    
    // From interface: com.tangosol.net.InvocationService
    public java.util.Map query(com.tangosol.net.Invocable task, java.util.Set setMembers)
        {
        // import Component.Net.Extend.MessageFactory.InvocationServiceFactory$InvocationRequest as com.tangosol.coherence.component.net.extend.messageFactory.InvocationServiceFactory.InvocationRequest;
        // import com.tangosol.net.Member;
        // import com.tangosol.net.messaging.Channel;
        // import com.tangosol.net.messaging.Protocol$MessageFactory as com.tangosol.net.messaging.Protocol.MessageFactory;
        // import java.util.Collections;
        
        if (task == null)
            {
            throw new IllegalArgumentException("task cannot be null");
            }
        
        if (setMembers != null)
            {
            throw new IllegalArgumentException("directed query not supported; "
                    + "the specified Member set must be null");
            }
        
        Channel channel = ensureChannel();
        com.tangosol.net.messaging.Protocol.MessageFactory factory = channel.getMessageFactory();
        com.tangosol.coherence.component.net.extend.messageFactory.InvocationServiceFactory.InvocationRequest request = (com.tangosol.coherence.component.net.extend.messageFactory.InvocationServiceFactory.InvocationRequest) factory.createMessage(com.tangosol.coherence.component.net.extend.messageFactory.InvocationServiceFactory.InvocationRequest.TYPE_ID);
        
        request.setTask(task);
        
        Object oResult = channel.request(request);
        Member member  = getLocalMember();
        
        return Collections.singletonMap(member, oResult);
        }
    }
