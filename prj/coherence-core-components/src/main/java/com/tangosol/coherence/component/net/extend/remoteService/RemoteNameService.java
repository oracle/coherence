
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.extend.remoteService.RemoteNameService

package com.tangosol.coherence.component.net.extend.remoteService;

import com.tangosol.coherence.component.net.extend.protocol.NameServiceProtocol;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.Initiator;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.initiator.TcpInitiator;
import com.oracle.coherence.common.base.Timeout;
import com.oracle.coherence.common.internal.net.MultiplexedSocketProvider;
import com.oracle.coherence.common.net.InetSocketAddress32;
import com.tangosol.discovery.NSLookup;
import com.tangosol.internal.net.service.extend.remote.RemoteServiceDependencies;
import com.tangosol.internal.net.service.peer.initiator.TcpInitiatorDependencies;
import com.tangosol.net.NameService;
import com.tangosol.net.OperationalContext;
import com.tangosol.net.SocketAddressProvider;
import com.tangosol.net.internal.NameServicePofContext;
import com.tangosol.net.internal.WrapperSocketAddressProvider;
import com.tangosol.net.messaging.Channel;
import com.tangosol.net.messaging.ConnectionException;
import com.tangosol.net.messaging.ConnectionInitiator;
import com.tangosol.net.security.SecurityHelper;
import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;
import java.io.DataInputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Set;

/**
 * NameService implementation that allows a JVM to use a remote NameService
 * without having to join the Cluster.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class RemoteNameService
        extends    com.tangosol.coherence.component.net.extend.RemoteService
        implements com.tangosol.io.SerializerFactory,
                   com.tangosol.net.NameService,
                   AutoCloseable
    {
    // ---- Fields declarations ----
    
    /**
     * Property MulticastAddress
     *
     * A multicast address which can be used for issuing NS lookups.  If null
     * then only unicast TCP lookups are supported.
     * 
     * @since 12.2.1
     */
    private java.net.InetSocketAddress __m_MulticastAddress;
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
        __mapChildren.put("InterruptTask", RemoteNameService.InterruptTask.get_CLASS());
        }
    
    // Default constructor
    public RemoteNameService()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public RemoteNameService(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            setServiceVersion("1");
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
        return new com.tangosol.coherence.component.net.extend.remoteService.RemoteNameService();
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
            clz = Class.forName("com.tangosol.coherence/component/net/extend/remoteService/RemoteNameService".replace('/', '.'));
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
    
    // From interface: com.tangosol.net.NameService
    public void addLookupCallback(com.tangosol.net.NameService.LookupCallback callback)
        {
        throw new UnsupportedOperationException();
        }
    
    // From interface: com.tangosol.net.NameService
    public void bind(String sName, Object o)
            throws javax.naming.NamingException
        {
        // import Component.Net.Extend.MessageFactory.NameServiceFactory$BindRequest as com.tangosol.coherence.component.net.extend.messageFactory.NameServiceFactory.BindRequest;
        // import com.tangosol.net.messaging.Channel;
        
        Channel     channel = ensureChannel();
        com.tangosol.coherence.component.net.extend.messageFactory.NameServiceFactory.BindRequest request = (com.tangosol.coherence.component.net.extend.messageFactory.NameServiceFactory.BindRequest) channel.getMessageFactory().createMessage(com.tangosol.coherence.component.net.extend.messageFactory.NameServiceFactory.BindRequest.TYPE_ID);
        
        request.setName(sName);
        request.setResource(o);
        
        channel.request(request);
        }
    
    // From interface: java.lang.AutoCloseable
    public void close()
        {
        shutdown(); // see RemoteService#lookupProxyService
        }
    
    // From interface: com.tangosol.io.SerializerFactory
    /**
     * Custom SerializerFactory which always returns ConfigurablePofContext.
     */
    public com.tangosol.io.Serializer createSerializer(ClassLoader loader)
        {
        // import com.tangosol.net.internal.NameServicePofContext;
        
        return NameServicePofContext.INSTANCE;
        }
    
    // Declared at the super level
    /**
     * The start() implementation method. This method must only be called by a
    * thread that has synchronized on this RemoteService.
     */
    protected void doStart()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Peer.Initiator.TcpInitiator;
        // import com.tangosol.net.messaging.ConnectionException;
        // import com.tangosol.net.SocketAddressProvider;
        // import com.oracle.coherence.common.net.InetSocketAddress32;
        // import com.oracle.coherence.common.internal.net.MultiplexedSocketProvider;
        // import java.net.InetSocketAddress;
        
        try
            {
            super.doStart();
            }
        catch (ConnectionException e)
            {
            // scan address list and see if it contains any MC addresses we can use
            SocketAddressProvider provider = ((TcpInitiator) getInitiator()).getRemoteAddressProvider();
            for (InetSocketAddress32 addr = (InetSocketAddress32) provider.getNextAddress(); addr != null; addr = (InetSocketAddress32) provider.getNextAddress())
                {
                if (addr.getAddress().isMulticastAddress())
                    {
                    setMulticastAddress(new InetSocketAddress(addr.getAddress(), MultiplexedSocketProvider.getBasePort(addr.getPort())));
                    provider.accept();
                    return; // consider ourselves "started"
                    }
                provider.reject(null);
                }
        
            throw e;
            }
        }
    
    // Declared at the super level
    /**
     * The stop() implementation method. This method must only be called by a
    * thread that has synchronized on this RemoteService.
     */
    protected void doStop()
        {
        setMulticastAddress(null);
        super.doStop();
        }
    
    // From interface: com.tangosol.net.NameService
    public java.net.InetAddress getLocalAddress()
        {
        return null;
        }
    
    // Accessor for the property "MulticastAddress"
    /**
     * Getter for property MulticastAddress.<p>
    * A multicast address which can be used for issuing NS lookups.  If null
    * then only unicast TCP lookups are supported.
    * 
    * @since 12.2.1
     */
    public java.net.InetSocketAddress getMulticastAddress()
        {
        return __m_MulticastAddress;
        }
    
    // Declared at the super level
    public String getServiceType()
        {
        // import com.tangosol.net.NameService;
        
        return NameService.TYPE_REMOTE;
        }
    
    // From interface: com.tangosol.net.NameService
    // Declared at the super level
    public boolean isRunning()
        {
        return super.isRunning() || getMulticastAddress() != null;
        }
    
    // From interface: com.tangosol.net.NameService
    public Object lookup(String sName)
            throws javax.naming.NamingException
        {
        // import Component.Net.Extend.MessageFactory.NameServiceFactory$LookupRequest as com.tangosol.coherence.component.net.extend.messageFactory.NameServiceFactory.LookupRequest;
        // import Component.Util.Daemon.QueueProcessor.Service.Peer.Initiator;
        // import com.tangosol.discovery.NSLookup;
        // import com.tangosol.internal.net.service.extend.remote.RemoteServiceDependencies;
        // import com.tangosol.internal.net.service.peer.initiator.TcpInitiatorDependencies;
        // import com.tangosol.net.messaging.Channel;
        // import com.tangosol.net.messaging.ConnectionException;
        // import com.tangosol.net.OperationalContext;
        // import com.tangosol.util.Binary;
        // import com.tangosol.util.ExternalizableHelper;
        // import com.oracle.coherence.common.base.Timeout;
        // import java.io.DataInputStream;
        // import java.net.InetAddress;
        // import java.net.InetSocketAddress;
        // import java.util.Set;
        
        InetSocketAddress addrMC = getMulticastAddress();
        if (addrMC == null)
            {
            Channel channel = ensureChannel();
            com.tangosol.coherence.component.net.extend.messageFactory.NameServiceFactory.LookupRequest request = (com.tangosol.coherence.component.net.extend.messageFactory.NameServiceFactory.LookupRequest) channel.getMessageFactory().createMessage(com.tangosol.coherence.component.net.extend.messageFactory.NameServiceFactory.LookupRequest.TYPE_ID);
        
            request.setLookupName(sName);
        
            return channel.request(request);
            }
        // else; MC based lookup
        
        RemoteServiceDependencies deps          = (RemoteServiceDependencies) getDependencies();
        TcpInitiatorDependencies  depsInit      = (TcpInitiatorDependencies) deps.getInitiatorDependencies();
        Initiator                 initiator     = ((Initiator) getInitiator());
        Set                       setClose      = initiator.getCloseOnExit();
        RemoteNameService.InterruptTask            task          = new RemoteNameService.InterruptTask();
        OperationalContext        ctx           = getOperationalContext();
        InetSocketAddress         addrLocal     = (InetSocketAddress) depsInit.getLocalAddress();
        InetAddress               addrMember    = ctx.getLocalMember().getAddress();
        InetAddress               addrDisc      = ctx.getDiscoveryInterface();
        InetAddress               addrLocalInet = addrLocal == null
            ? (addrDisc == null || addrDisc.isAnyLocalAddress()) && (addrMember != null && addrMember.isLoopbackAddress())
                ? addrMember // legacy indication that the cluster is restricted to loopback and thus MC is loopback only
                : addrDisc
            : addrLocal.getAddress();
        
        synchronized (initiator)
            {
            if (initiator.isRunning())
                {
                task.setThread(Thread.currentThread());
                setClose.add(task);
                }
            else
                {
                throw new IllegalStateException("service has been shutdown");
                }
            }
        
        try
            {
            Binary          binMember      = ExternalizableHelper.toBinary(getOperationalContext().getLocalMember(), initiator.getSerializer());
            long            cMillisTimeout = Math.min(depsInit.getConnectTimeoutMillis(), Timeout.remainingTimeoutMillis());
            DataInputStream in             = NSLookup.datagramLookupRaw(deps.getRemoteClusterName(), sName, addrMC,
                addrLocalInet, (int) cMillisTimeout, ctx.getDiscoveryTimeToLive(), binMember.toByteArray());
        
            Binary binResult = new Binary(in);
            return binResult.length() == 0
                ? null
                : ExternalizableHelper.fromBinary(binResult, initiator.getSerializer());
            }
        catch (Exception e)
            {
            throw new ConnectionException(e);
            }
        finally
            {
            setClose.remove(task);
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
        // import Component.Net.Extend.Protocol.NameServiceProtocol;
        // import Component.Util.Daemon.QueueProcessor.Service.Peer.Initiator;
        // import Component.Util.Daemon.QueueProcessor.Service.Peer.Initiator.TcpInitiator;
        // import com.oracle.coherence.common.internal.net.MultiplexedSocketProvider$WellKnownSubPorts as com.oracle.coherence.common.internal.net.MultiplexedSocketProvider.WellKnownSubPorts;
        // import com.tangosol.net.internal.WrapperSocketAddressProvider;
        // import com.tangosol.net.messaging.ConnectionInitiator;
        
        super.onDependencies(deps);
        
        ConnectionInitiator initiator = getInitiator();
        if (initiator instanceof Initiator)
            {
            Initiator initiatorImpl = (Initiator) initiator;
        
            // always use POF for NameService requests
            initiatorImpl.setSerializerFactory(this);
        
            // don't use any filters for NameService requests
            initiatorImpl.setWrapperStreamFactoryList(null);
        
            if (initiator instanceof TcpInitiator)
                {
                TcpInitiator tcpInitiator = (TcpInitiator) initiator;
        
                // configure the SocketAddressProvider
                tcpInitiator.setRemoteAddressProvider(new WrapperSocketAddressProvider(
                    tcpInitiator.getRemoteAddressProvider(),
                    com.oracle.coherence.common.internal.net.MultiplexedSocketProvider.WellKnownSubPorts.COHERENCE_NAME_SERVICE.getSubPort()));
                }
            }
        
        // register all Protocols
        initiator.registerProtocol(NameServiceProtocol.getInstance());
        }
    
    // Declared at the super level
    /**
     * Open a Channel to the remote NameService.
     */
    protected com.tangosol.net.messaging.Channel openChannel()
        {
        // import Component.Net.Extend.Protocol.NameServiceProtocol;
        // import com.tangosol.net.security.SecurityHelper;
        
        return getInitiator().ensureConnection().openChannel(NameServiceProtocol.getInstance(),
            "NameService",
            null,
            null,
            SecurityHelper.getCurrentSubject());
        }
    
    // Accessor for the property "MulticastAddress"
    /**
     * Setter for property MulticastAddress.<p>
    * A multicast address which can be used for issuing NS lookups.  If null
    * then only unicast TCP lookups are supported.
    * 
    * @since 12.2.1
     */
    public void setMulticastAddress(java.net.InetSocketAddress addressMulticast)
        {
        __m_MulticastAddress = addressMulticast;
        }
    
    // From interface: com.tangosol.net.NameService
    public void unbind(String sName)
            throws javax.naming.NamingException
        {
        throw new UnsupportedOperationException();
        }

    // ---- class: com.tangosol.coherence.component.net.extend.remoteService.RemoteNameService$InterruptTask
    
    /**
     * A helper task to interrupt a thread in MC lookup if the service is
     * concurrently closed.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class InterruptTask
            extends    com.tangosol.coherence.Component
            implements AutoCloseable
        {
        // ---- Fields declarations ----
        
        /**
         * Property Thread
         *
         * The thread to interrupt
         */
        private Thread __m_Thread;
        
        // Default constructor
        public InterruptTask()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public InterruptTask(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.net.extend.remoteService.RemoteNameService.InterruptTask();
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
                clz = Class.forName("com.tangosol.coherence/component/net/extend/remoteService/RemoteNameService$InterruptTask".replace('/', '.'));
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
        
        // From interface: java.lang.AutoCloseable
        public void close()
            {
            // NSLookup will abort an MC lookup if interrupted, closing its temporary socket in the process
            getThread().interrupt();
            }
        
        // Accessor for the property "Thread"
        /**
         * Getter for property Thread.<p>
        * The thread to interrupt
         */
        public Thread getThread()
            {
            return __m_Thread;
            }
        
        // Accessor for the property "Thread"
        /**
         * Setter for property Thread.<p>
        * The thread to interrupt
         */
        public void setThread(Thread sProperty)
            {
            __m_Thread = sProperty;
            }
        }
    }
