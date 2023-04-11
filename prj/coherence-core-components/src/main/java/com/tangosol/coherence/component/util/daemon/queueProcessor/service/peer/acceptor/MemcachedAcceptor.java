
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.MemcachedAcceptor

package com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor;

import com.oracle.coherence.common.net.InetAddresses;
import com.tangosol.coherence.config.builder.ParameterizedBuilder;
import com.tangosol.coherence.memcached.server.MemcachedServer;
import com.tangosol.config.expression.NullParameterResolver;
import com.tangosol.internal.net.service.peer.acceptor.DefaultMemcachedAcceptorDependencies;
import com.tangosol.internal.net.service.peer.acceptor.MemcachedAcceptorDependencies;
import com.tangosol.net.AddressProvider;
import com.tangosol.util.Base;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

/**
 * Base definition of a ConnectionAcceptor component.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class MemcachedAcceptor
        extends    com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.Acceptor
        implements java.util.concurrent.Executor
    {
    // ---- Fields declarations ----
    
    /**
     * Property AuthMethod
     *
     * The client authentication method to use.
     * 
     * Valid values "plain" for SASL PLAIN mechanism and "none" for no
     * authentication.
     */
    private String __m_AuthMethod;
    
    /**
     * Property BinaryPassThru
     *
     *  The binary-pass-thru element specifies that the memcached adaptor does
     * not need to pass the values coming in the memcached request thru the
     * configured cache service Serializer before storing it in the cache. This
     * is mainly required when the memcached client is using some Coherence
     * Serializer like PoF Serializer to convert the objects into byte[] and
     * the cache service has the same Serializer configured. In such cases, the
     * incoming byte[] is already in proper Binary encoded format.
     */
    private boolean __m_BinaryPassThru;
    
    /**
     * Property CacheName
     *
     * The underlying cache name associated with the embedded Memcached server.
     */
    private String __m_CacheName;
    
    /**
     * Property LocalAddress
     *
     * The address that the embedded Memcached server will listen on.
     */
    private String __m_LocalAddress;
    
    /**
     * Property LocalPort
     *
     * The port that the embedded Memcached server will listen on.
     */
    private int __m_LocalPort;
    
    /**
     * Property MemcachedServer
     *
     * The embedded Memcached server.
     * 
     * @see com.tangosol.coherence.memcached.MemcachedServer
     */
    private com.tangosol.coherence.memcached.server.MemcachedServer __m_MemcachedServer;
    
    /**
     * Property SocketProvider
     *
     * The SocketProvider that may be used by the MemcachedAcceptor to open
     * ServerSocketChannels.
     */
    private com.oracle.coherence.common.net.SocketProvider __m_SocketProvider;
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
        __mapChildren.put("DispatchEvent", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.DispatchEvent.get_CLASS());
        __mapChildren.put("MessageFactory", com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.Acceptor.MessageFactory.get_CLASS());
        __mapChildren.put("Queue", com.tangosol.coherence.component.util.daemon.QueueProcessor.Queue.get_CLASS());
        }
    
    // Default constructor
    public MemcachedAcceptor()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public MemcachedAcceptor(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            setConnectionPendingSet(new com.tangosol.util.SafeHashSet());
            setConnectionSet(new com.tangosol.util.SafeHashSet());
            setDaemonState(0);
            setDefaultGuardRecovery(0.9F);
            setDefaultGuardTimeout(60000L);
            setNotifier(new com.oracle.coherence.common.base.SingleWaiterMultiNotifier());
            setProtocolMap(new java.util.HashMap());
            setReceiverMap(new java.util.HashMap());
            setRequestTimeout(30000L);
            setSerializerMap(new java.util.WeakHashMap());
            }
        catch (java.lang.Exception e)
            {
            // re-throw as a runtime exception
            throw new com.tangosol.util.WrapperException(e);
            }
        
        // containment initialization: children
        _addChild(new com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.Acceptor.DaemonPool("DaemonPool", this, true), "DaemonPool");
        _addChild(new com.tangosol.coherence.component.util.daemon.queueProcessor.Service.EventDispatcher("EventDispatcher", this, true), "EventDispatcher");
        _addChild(new com.tangosol.coherence.component.util.Daemon.Guard("Guard", this, true), "Guard");
        _addChild(new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.Protocol("Protocol", this, true), "Protocol");
        
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
        return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.MemcachedAcceptor();
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
            clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/peer/acceptor/MemcachedAcceptor".replace('/', '.'));
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
    /**
     * Create a new Default dependencies object by copying the supplies
    * dependencies.  Each class or component that uses dependencies implements
    * a Default dependencies class which provides the clone functionality.  
    * The dependency injection design pattern requires every component in the
    * component hierarchy to implement clone, producing their variant of the
    * dependencies interface.
    * 
    * @return the cloned dependencies
     */
    protected com.tangosol.internal.net.service.DefaultServiceDependencies cloneDependencies(com.tangosol.net.ServiceDependencies deps)
        {
        // import com.tangosol.internal.net.service.peer.acceptor.DefaultMemcachedAcceptorDependencies;
        // import com.tangosol.internal.net.service.peer.acceptor.MemcachedAcceptorDependencies;
        
        return new DefaultMemcachedAcceptorDependencies((MemcachedAcceptorDependencies) deps);
        }
    
    // Declared at the super level
    public synchronized void configure(com.tangosol.run.xml.XmlElement xml)
        {
        // import com.tangosol.internal.net.service.peer.acceptor.DefaultMemcachedAcceptorDependencies;
        // import com.tangosol.internal.net.service.peer.acceptor.LegacyXmlMemcachedAcceptorHelper as com.tangosol.internal.net.service.peer.acceptor.LegacyXmlMemcachedAcceptorHelper;
        
        setDependencies(com.tangosol.internal.net.service.peer.acceptor.LegacyXmlMemcachedAcceptorHelper.fromXml(xml,
            new DefaultMemcachedAcceptorDependencies(), getOperationalContext(),
            getContextClassLoader()));
        }
    
    // From interface: java.util.concurrent.Executor
    /**
     * Execute the task
     */
    public void execute(Runnable task)
        {
        getDaemonPool().add(task);
        }
    
    // Accessor for the property "AuthMethod"
    /**
     * Getter for property AuthMethod.<p>
    * The client authentication method to use.
    * 
    * Valid values "plain" for SASL PLAIN mechanism and "none" for no
    * authentication.
     */
    public String getAuthMethod()
        {
        return __m_AuthMethod;
        }
    
    // Accessor for the property "CacheName"
    /**
     * Getter for property CacheName.<p>
    * The underlying cache name associated with the embedded Memcached server.
     */
    public String getCacheName()
        {
        return __m_CacheName;
        }
    
    // Declared at the super level
    /**
     * Getter for property Description.<p>
    * Human-readable description of additional Service properties. Used by
    * toString().
     */
    public String getDescription()
        {
        return "MemcachedAcceptor :" +
               " LocalAddress="   + getLocalAddress() +
               ",LocalPort="      + getLocalPort();
        }
    
    // Accessor for the property "LocalAddress"
    /**
     * Getter for property LocalAddress.<p>
    * The address that the embedded Memcached server will listen on.
     */
    public String getLocalAddress()
        {
        return __m_LocalAddress;
        }
    
    // Accessor for the property "LocalPort"
    /**
     * Getter for property LocalPort.<p>
    * The port that the embedded Memcached server will listen on.
     */
    public int getLocalPort()
        {
        return __m_LocalPort;
        }
    
    // Accessor for the property "MemcachedServer"
    /**
     * Getter for property MemcachedServer.<p>
    * The embedded Memcached server.
    * 
    * @see com.tangosol.coherence.memcached.MemcachedServer
     */
    public com.tangosol.coherence.memcached.server.MemcachedServer getMemcachedServer()
        {
        return __m_MemcachedServer;
        }
    
    // Accessor for the property "SocketProvider"
    /**
     * Getter for property SocketProvider.<p>
    * The SocketProvider that may be used by the MemcachedAcceptor to open
    * ServerSocketChannels.
     */
    public com.oracle.coherence.common.net.SocketProvider getSocketProvider()
        {
        return __m_SocketProvider;
        }
    
    // Declared at the super level
    /**
     * Factory method: create a new Connection.
    * 
    * Implementations must configure the Connection with a reference to this
    * ConnectionManager (see Connection#setConnectionManager).
    * 
    * @return a new Connection object that has yet to be opened
     */
    protected com.tangosol.coherence.component.net.extend.Connection instantiateConnection()
        {
        throw new UnsupportedOperationException();
        }
    
    // Accessor for the property "BinaryPassThru"
    /**
     * Getter for property BinaryPassThru.<p>
    *  The binary-pass-thru element specifies that the memcached adaptor does
    * not need to pass the values coming in the memcached request thru the
    * configured cache service Serializer before storing it in the cache. This
    * is mainly required when the memcached client is using some Coherence
    * Serializer like PoF Serializer to convert the objects into byte[] and the
    * cache service has the same Serializer configured. In such cases, the
    * incoming byte[] is already in proper Binary encoded format.
     */
    public boolean isBinaryPassThru()
        {
        return __m_BinaryPassThru;
        }
    
    // Declared at the super level
    /**
     * This event occurs when dependencies are injected into the component. 
    * First, call super.onDependencies to allow all super components to process
    * the Dependencies.  Each component is free to chose how it consumes
    * dependencies from within onDependencies.  Often (though not ideal), the 
    * dependencies are copied into the component's properties.  This technique
    * isolates Dependency Injection from the rest of the component code since
    * components continue to access properties just as they did before. 
    * 
    * PartitionedCacheDependencies deps = (PartitionedCacheDependencies)
    * getDependencies();
    * deps.getFoo();
     */
    protected void onDependencies(com.tangosol.net.ServiceDependencies deps)
        {
        // import com.tangosol.coherence.config.builder.ParameterizedBuilder;
        // import com.tangosol.config.expression.NullParameterResolver;
        // import com.tangosol.internal.net.service.peer.acceptor.MemcachedAcceptorDependencies;
        // import com.tangosol.net.AddressProvider;
        // import java.net.InetSocketAddress;
        
        super.onDependencies(deps);
        
        MemcachedAcceptorDependencies memcachedDeps = (MemcachedAcceptorDependencies) deps;
        
        setMemcachedServer(memcachedDeps.getMemcachedServer());
        setSocketProvider(memcachedDeps.getSocketProviderBuilder().realize(null, null, null));
        setCacheName(memcachedDeps.getCacheName());
        setAuthMethod(memcachedDeps.getAuthMethod());
        setBinaryPassThru(memcachedDeps.isBinaryPassThru());
        
        ParameterizedBuilder bldr = memcachedDeps.getAddressProviderBuilder();
        if (bldr != null)
            {
            AddressProvider provider = ((AddressProvider) bldr.realize(
                    new NullParameterResolver(), getContextClassLoader(), null));
        
            InetSocketAddress address = provider.getNextAddress();
            setLocalAddress(address.getAddress().getHostAddress());
            setLocalPort(address.getPort());
            }
        }
    
    // Declared at the super level
    /**
     * The default implementation of this method does nothing.
     */
    protected void onServiceStarting()
        {
        // import com.tangosol.coherence.memcached.server.MemcachedServer;
        // import com.tangosol.util.Base;
        // import com.oracle.coherence.common.net.InetAddresses;
        // import java.net.InetAddress;
        // import java.net.UnknownHostException;
        
        super.onServiceStarting();
        
        MemcachedServer server = getMemcachedServer();
        _assert(server != null);
        
        // configure and start the MemcachedServer
        try
            {
            server.setParentService(getParentService());
            server.setExecutor(this);
            server.setLocalAddress(getLocalAddress());
            server.setLocalPort(getLocalPort());
            server.setSocketProvider(getSocketProvider());    
            server.setCacheName(getCacheName());
            server.setAuthMethod(getAuthMethod());
            server.setBinaryPassthru(isBinaryPassThru());
            server.setIdentityAsserter(getOperationalContext().getIdentityAsserter());
            
            server.start();
            }
        catch (Exception e)
            {
            throw Base.ensureRuntimeException(e);
            }
        
        String sAddr = getLocalAddress();
        if (InetAddresses.isAnyLocalAddress(sAddr))
            {
            try
                {
                sAddr = InetAddress.getLocalHost().getHostName();
                }
            catch (UnknownHostException e) {}
            }
        
        _trace("MemcachedAcceptor now listening for connections on "
                + sAddr + ':' + getLocalPort(), 3);
        }
    
    // Declared at the super level
    /**
     * The default implementation of this method sets AcceptingClients to false.
     */
    protected void onServiceStopped()
        {
        // import com.tangosol.coherence.memcached.server.MemcachedServer;
        // import com.tangosol.util.Base;
        
        MemcachedServer server = getMemcachedServer();
        if (server != null)
            {
            try
                {
                server.stop();
                }
            catch (Exception e)
                {
                throw Base.ensureRuntimeException(e);
                }
             }
        }
    
    // Declared at the super level
    /**
     * The default implementation of this method sets AcceptingClients to false.
     */
    protected void onServiceStopping()
        {
        // import com.tangosol.coherence.memcached.server.MemcachedServer;
        // import com.tangosol.util.Base;
        
        MemcachedServer server = getMemcachedServer();
        if (server != null)
            {
            try
                {
                server.stop();
                }
            catch (Exception e)
                {
                throw Base.ensureRuntimeException(e);
                }
             }
        }
    
    // Accessor for the property "AuthMethod"
    /**
     * Setter for property AuthMethod.<p>
    * The client authentication method to use.
    * 
    * Valid values "plain" for SASL PLAIN mechanism and "none" for no
    * authentication.
     */
    public void setAuthMethod(String sMethod)
        {
        __m_AuthMethod = sMethod;
        }
    
    // Accessor for the property "BinaryPassThru"
    /**
     * Setter for property BinaryPassThru.<p>
    *  The binary-pass-thru element specifies that the memcached adaptor does
    * not need to pass the values coming in the memcached request thru the
    * configured cache service Serializer before storing it in the cache. This
    * is mainly required when the memcached client is using some Coherence
    * Serializer like PoF Serializer to convert the objects into byte[] and the
    * cache service has the same Serializer configured. In such cases, the
    * incoming byte[] is already in proper Binary encoded format.
     */
    public void setBinaryPassThru(boolean fThru)
        {
        __m_BinaryPassThru = fThru;
        }
    
    // Accessor for the property "CacheName"
    /**
     * Setter for property CacheName.<p>
    * The underlying cache name associated with the embedded Memcached server.
     */
    public void setCacheName(String sName)
        {
        __m_CacheName = sName;
        }
    
    // Accessor for the property "LocalAddress"
    /**
     * Setter for property LocalAddress.<p>
    * The address that the embedded Memcached server will listen on.
     */
    public void setLocalAddress(String sAddress)
        {
        __m_LocalAddress = sAddress;
        }
    
    // Accessor for the property "LocalPort"
    /**
     * Setter for property LocalPort.<p>
    * The port that the embedded Memcached server will listen on.
     */
    public void setLocalPort(int nPort)
        {
        __m_LocalPort = nPort;
        }
    
    // Accessor for the property "MemcachedServer"
    /**
     * Setter for property MemcachedServer.<p>
    * The embedded Memcached server.
    * 
    * @see com.tangosol.coherence.memcached.MemcachedServer
     */
    public void setMemcachedServer(com.tangosol.coherence.memcached.server.MemcachedServer serverMemcached)
        {
        __m_MemcachedServer = serverMemcached;
        }
    
    // Accessor for the property "SocketProvider"
    /**
     * Setter for property SocketProvider.<p>
    * The SocketProvider that may be used by the MemcachedAcceptor to open
    * ServerSocketChannels.
     */
    public void setSocketProvider(com.oracle.coherence.common.net.SocketProvider providerSocket)
        {
        __m_SocketProvider = providerSocket;
        }
    
    /**
     * Submits the task to the Proxy daemon pool for execution by proxy worker
    * threads.
    * 
    * @param task Runnable to execute 
     */
    public void submit(Runnable task)
        {
        }
    }
