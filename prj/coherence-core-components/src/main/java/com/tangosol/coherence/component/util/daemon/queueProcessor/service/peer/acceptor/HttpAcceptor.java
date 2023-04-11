
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.HttpAcceptor

package com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor;

import com.oracle.coherence.common.net.InetAddresses;
import com.tangosol.internal.net.service.peer.acceptor.DefaultHttpAcceptorDependencies;
import com.tangosol.internal.net.service.peer.acceptor.HttpAcceptorDependencies;
import com.tangosol.util.Base;
import com.tangosol.util.ClassHelper;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;

/**
 * A ConnectionAcceptor implementation that accepts Connections over HTTP.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class HttpAcceptor
        extends    com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.Acceptor
    {
    // ---- Fields declarations ----
    
    /**
     * Property AuthMethod
     *
     * The client authentication method to use.
     * 
     * Valid values "basic" for HTTP basic authentication, "cert" for client
     * certificate authentication, and "none" for no authentication.
     */
    private String __m_AuthMethod;
    
    /**
     * Property HttpServer
     *
     * The embedded HTTP server.
     * 
     * @see com.tangosol.coherence.rest.server.HttpServer
     */
    private Object __m_HttpServer;
    
    /**
     * Property ListenAddress
     *
     * The address the HttpServer is listening on.
     */
    private String __m_ListenAddress;
    
    /**
     * Property ListenPort
     *
     * The port the HttpServer is listening on.
     */
    private int __m_ListenPort;
    
    /**
     * Property LocalAddress
     *
     * The configured address that the embedded HTTP server will listen on.
     */
    private String __m_LocalAddress;
    
    /**
     * Property LocalPort
     *
     * The configured port that the embedded HTTP server will listen on (may be
     * zero for ephemeral).
     */
    private int __m_LocalPort;
    
    /**
     * Property ResourceConfig
     *
     * The resource configuration object.
     * 
     * @see com.sun.jersey.api.core.ResourceConfig
     */
    private java.util.Map __m_ResourceConfig;
    
    /**
     * Property Session
     *
     * The Session used to acquire NamedCache instances.
     */
    private com.tangosol.net.Session __m_Session;
    
    /**
     * Property SocketProvider
     *
     * The SocketProvider that may be used by the HttpAcceptor to open
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
    public HttpAcceptor()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public HttpAcceptor(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            setAuthMethod("\"none\"");
            setConnectionPendingSet(new com.tangosol.util.SafeHashSet());
            setConnectionSet(new com.tangosol.util.SafeHashSet());
            setDaemonState(0);
            setDefaultGuardRecovery(0.9F);
            setDefaultGuardTimeout(60000L);
            setLocalAddress("localhost");
            setLocalPort(0);
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
        return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.HttpAcceptor();
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
            clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/peer/acceptor/HttpAcceptor".replace('/', '.'));
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
        // import com.tangosol.internal.net.service.peer.acceptor.DefaultHttpAcceptorDependencies;
        // import com.tangosol.internal.net.service.peer.acceptor.HttpAcceptorDependencies;
        
        return new DefaultHttpAcceptorDependencies((HttpAcceptorDependencies) deps);
        }
    
    // Declared at the super level
    public synchronized void configure(com.tangosol.run.xml.XmlElement xml)
        {
        // import com.tangosol.internal.net.service.peer.acceptor.DefaultHttpAcceptorDependencies;
        // import com.tangosol.internal.net.service.peer.acceptor.LegacyXmlHttpAcceptorHelper as com.tangosol.internal.net.service.peer.acceptor.LegacyXmlHttpAcceptorHelper;
        
        setDependencies(com.tangosol.internal.net.service.peer.acceptor.LegacyXmlHttpAcceptorHelper.fromXml(xml,
            new DefaultHttpAcceptorDependencies(), getOperationalContext(),
            getContextClassLoader()));
        
        setServiceConfig(xml);
        }
    
    // Accessor for the property "AuthMethod"
    /**
     * Getter for property AuthMethod.<p>
    * The client authentication method to use.
    * 
    * Valid values "basic" for HTTP basic authentication, "cert" for client
    * certificate authentication, and "none" for no authentication.
     */
    public String getAuthMethod()
        {
        return __m_AuthMethod;
        }
    
    // Declared at the super level
    /**
     * Getter for property Description.<p>
    * Human-readable description of additional Service properties. Used by
    * toString().
     */
    public String getDescription()
        {
        return "HttpServer="       + getHttpServer() +
               ", LocalAddress="   + getLocalAddress() +
               ", LocalPort="      + getLocalPort() +
               ", ResourceConfig=" + String.valueOf(getResourceConfig());
        }
    
    // Accessor for the property "HttpServer"
    /**
     * Getter for property HttpServer.<p>
    * The embedded HTTP server.
    * 
    * @see com.tangosol.coherence.rest.server.HttpServer
     */
    public Object getHttpServer()
        {
        return __m_HttpServer;
        }
    
    // Accessor for the property "ListenAddress"
    /**
     * Getter for property ListenAddress.<p>
    * The address the HttpServer is listening on.
     */
    public String getListenAddress()
        {
        return __m_ListenAddress;
        }
    
    // Accessor for the property "ListenPort"
    /**
     * Getter for property ListenPort.<p>
    * The port the HttpServer is listening on.
     */
    public int getListenPort()
        {
        return __m_ListenPort;
        }
    
    // Accessor for the property "LocalAddress"
    /**
     * Getter for property LocalAddress.<p>
    * The configured address that the embedded HTTP server will listen on.
     */
    public String getLocalAddress()
        {
        return __m_LocalAddress;
        }
    
    // Accessor for the property "LocalPort"
    /**
     * Getter for property LocalPort.<p>
    * The configured port that the embedded HTTP server will listen on (may be
    * zero for ephemeral).
     */
    public int getLocalPort()
        {
        return __m_LocalPort;
        }
    
    // Accessor for the property "ResourceConfig"
    /**
     * Getter for property ResourceConfig.<p>
    * The resource configuration object.
    * 
    * @see com.sun.jersey.api.core.ResourceConfig
     */
    public java.util.Map getResourceConfig()
        {
        return __m_ResourceConfig;
        }
    
    // Accessor for the property "Session"
    /**
     * Getter for property Session.<p>
    * The Session used to acquire NamedCache instances.
     */
    public com.tangosol.net.Session getSession()
        {
        return __m_Session;
        }
    
    // Accessor for the property "SocketProvider"
    /**
     * Getter for property SocketProvider.<p>
    * The SocketProvider that may be used by the HttpAcceptor to open
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
        // import com.tangosol.internal.net.service.peer.acceptor.HttpAcceptorDependencies;
        
        super.onDependencies(deps);
        
        HttpAcceptorDependencies httpDeps = (HttpAcceptorDependencies) deps;
        
        setHttpServer(httpDeps.getHttpServer());
        setSocketProvider(httpDeps.getSocketProviderBuilder().realize(null, null, null));
        setLocalAddress(httpDeps.getLocalAddress());
        setLocalPort(httpDeps.getLocalPort());
        setResourceConfig(httpDeps.getResourceConfig());
        setAuthMethod(httpDeps.getAuthMethod());
        }
    
    // Declared at the super level
    /**
     * The default implementation of this method does nothing.
     */
    protected void onServiceStarting()
        {
        // import com.tangosol.util.Base;
        // import com.tangosol.util.ClassHelper;
        // import com.oracle.coherence.common.net.InetAddresses;
        // import java.net.InetAddress;
        // import java.net.UnknownHostException;
        // import java.util.Map;
        
        super.onServiceStarting();
        
        Object oServer = getHttpServer();
        _assert(oServer != null);
        
        Map mapConfig = getResourceConfig();
        _assert(mapConfig != null);
        
        // configure and start the HttpServer
        try
            {
            ClassHelper.invoke(oServer, "setAuthMethod",     new Object[] {getAuthMethod()});
            ClassHelper.invoke(oServer, "setSession",        new Object[] {getSession()});
            ClassHelper.invoke(oServer, "setLocalAddress",   new Object[] {getLocalAddress()});
            ClassHelper.invoke(oServer, "setLocalPort",      new Object[] {Integer.valueOf(getLocalPort())});
            ClassHelper.invoke(oServer, "setResourceConfig", new Object[] {mapConfig});
            ClassHelper.invoke(oServer, "setParentService",  new Object[] {getParentService()});
            ClassHelper.invoke(oServer, "setSocketProvider", new Object[] {getSocketProvider()});    
            
            ClassHelper.invoke(oServer, "start", ClassHelper.VOID);
            }
        catch (Exception e)
            {
            throw Base.ensureRuntimeException(e);
            }
        
        // find out the runtime address and port
        String sAddr = getLocalAddress();
        int    nPort = getLocalPort();
        try
            {
            sAddr = (String)   ClassHelper.invoke(oServer, "getListenAddress", null);
            nPort = ((Integer) ClassHelper.invoke(oServer, "getListenPort",    null)).intValue();
            }
        catch (Exception e)
            {
            _trace("Unable to obtain the HttpServer's listen address and port:\n" + Base.printStackTrace(e), 2);
            }
        
        setListenAddress(sAddr);
        setListenPort(nPort);
        
        if (InetAddresses.isAnyLocalAddress(sAddr))
            {
            try
                {
                sAddr = InetAddress.getLocalHost().getHostName();
                }
            catch (UnknownHostException e) {}
            }
        
        _trace("HttpAcceptor now listening for connections on "
                + sAddr + ':' + nPort, 3);
        }
    
    // Declared at the super level
    /**
     * The default implementation of this method sets AcceptingClients to false.
     */
    protected void onServiceStopped()
        {
        // import com.tangosol.util.Base;
        // import com.tangosol.util.ClassHelper;
        
        super.onServiceStopped();
        
        try
            {
            Object oServer = getHttpServer();
            if (oServer != null)
                {
                ClassHelper.invoke(oServer, "stop", ClassHelper.VOID);
                }
            }
        catch (Exception e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }
    
    // Declared at the super level
    /**
     * The default implementation of this method sets AcceptingClients to false.
     */
    protected void onServiceStopping()
        {
        // import com.tangosol.util.Base;
        // import com.tangosol.util.ClassHelper;
        
        super.onServiceStopping();
        
        try
            {
            Object oServer = getHttpServer();
            if (oServer != null)
                {
                ClassHelper.invoke(oServer, "stop", ClassHelper.VOID);
                }
            }
        catch (Exception e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }
    
    // Declared at the super level
    /**
     * Reset the Service statistics.
     */
    public void resetStats()
        {
        // import com.tangosol.util.Base;
        // import com.tangosol.util.ClassHelper;
        
        super.resetStats();
        
        Object oServer = getHttpServer();
        _assert(oServer != null);
        
        try
            {
            // all Http servers are in coherence-rest.jar which is not
            // guaranteed to be in classpath so we must use reflection to
            // reset stats
            ClassHelper.invoke(oServer, "resetStats", ClassHelper.VOID);
            }
        catch (Exception e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }
    
    // Accessor for the property "AuthMethod"
    /**
     * Setter for property AuthMethod.<p>
    * The client authentication method to use.
    * 
    * Valid values "basic" for HTTP basic authentication, "cert" for client
    * certificate authentication, and "none" for no authentication.
     */
    protected void setAuthMethod(String sMethod)
        {
        __m_AuthMethod = sMethod;
        }
    
    // Accessor for the property "HttpServer"
    /**
     * Setter for property HttpServer.<p>
    * The embedded HTTP server.
    * 
    * @see com.tangosol.coherence.rest.server.HttpServer
     */
    public void setHttpServer(Object pHttpServer)
        {
        __m_HttpServer = pHttpServer;
        }
    
    // Accessor for the property "ListenAddress"
    /**
     * Setter for property ListenAddress.<p>
    * The address the HttpServer is listening on.
     */
    protected void setListenAddress(String sAddress)
        {
        __m_ListenAddress = sAddress;
        }
    
    // Accessor for the property "ListenPort"
    /**
     * Setter for property ListenPort.<p>
    * The port the HttpServer is listening on.
     */
    protected void setListenPort(int nPort)
        {
        __m_ListenPort = nPort;
        }
    
    // Accessor for the property "LocalAddress"
    /**
     * Setter for property LocalAddress.<p>
    * The configured address that the embedded HTTP server will listen on.
     */
    protected void setLocalAddress(String sAddress)
        {
        __m_LocalAddress = sAddress;
        }
    
    // Accessor for the property "LocalPort"
    /**
     * Setter for property LocalPort.<p>
    * The configured port that the embedded HTTP server will listen on (may be
    * zero for ephemeral).
     */
    protected void setLocalPort(int nPort)
        {
        __m_LocalPort = nPort;
        }
    
    // Accessor for the property "ResourceConfig"
    /**
     * Setter for property ResourceConfig.<p>
    * The resource configuration object.
    * 
    * @see com.sun.jersey.api.core.ResourceConfig
     */
    protected void setResourceConfig(java.util.Map map)
        {
        __m_ResourceConfig = map;
        }
    
    // Accessor for the property "Session"
    /**
     * Setter for property Session.<p>
    * The Session used to acquire NamedCache instances.
     */
    public void setSession(com.tangosol.net.Session session)
        {
        __m_Session = session;
        }
    
    // Accessor for the property "SocketProvider"
    /**
     * Setter for property SocketProvider.<p>
    * The SocketProvider that may be used by the HttpAcceptor to open
    * ServerSocketChannels.
     */
    protected void setSocketProvider(com.oracle.coherence.common.net.SocketProvider provider)
        {
        __m_SocketProvider = provider;
        }
    }
