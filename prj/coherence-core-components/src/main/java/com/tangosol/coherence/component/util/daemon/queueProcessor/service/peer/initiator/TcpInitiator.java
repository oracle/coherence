
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.initiator.TcpInitiator

package com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.initiator;

import com.tangosol.coherence.component.net.extend.util.TcpUtil;
import com.oracle.coherence.common.base.Blocking;
import com.oracle.coherence.common.net.InetSocketAddress32;
import com.tangosol.coherence.config.builder.FactoryBasedAddressProviderBuilder;
import com.tangosol.coherence.config.builder.ParameterizedBuilder;
import com.tangosol.config.expression.NullParameterResolver;
import com.tangosol.internal.net.service.peer.initiator.DefaultTcpInitiatorDependencies;
import com.tangosol.internal.net.service.peer.initiator.TcpInitiatorDependencies;
import com.tangosol.io.ByteArrayReadBuffer;
import com.tangosol.io.ByteArrayWriteBuffer;
import com.tangosol.net.AddressProviderFactory;
import com.tangosol.net.OperationalContext;
import com.tangosol.net.RequestTimeoutException;
import com.tangosol.net.SocketAddressProvider;
import com.tangosol.net.SocketOptions;
import com.tangosol.net.SocketProviderFactory;
import com.tangosol.net.messaging.ConnectionException;
import com.tangosol.util.Base;
import com.tangosol.util.ExternalizableHelper;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

/**
 * A ConnectionInitiator implementation that initiates Connections over TCP/IP.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class TcpInitiator
        extends    com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.Initiator
    {
    // ---- Fields declarations ----
    
    /**
     * Property LocalAddress
     *
     * The local SocketAddress that all Socket objects created by this
     * TcpInitiator will be bound to. If null, a SocketAddress created from an
     * ephemeral port and a valid local address will be used.
     */
    private java.net.SocketAddress __m_LocalAddress;
    
    /**
     * Property RemoteAddressProvider
     *
     * The SocketAddressProvider used by the TcpInitiator to obtain the
     * address(es) of the remote TcpAcceptor(s) that it will connect to.
     */
    private com.tangosol.net.SocketAddressProvider __m_RemoteAddressProvider;
    
    /**
     * Property SocketOptions
     *
     * The Initiator's socket configuration. 
     */
    private transient com.tangosol.net.SocketOptions __m_SocketOptions;
    
    /**
     * Property SocketProvider
     *
     * The SocketProvider used by the TcpAcceptor to open ServerSocketChannels.
     */
    private com.oracle.coherence.common.net.SocketProvider __m_SocketProvider;
    
    /**
     * Property SocketProviderFactory
     *
     * The factory to use in creating the SocketProvider.
     */
    private com.tangosol.net.SocketProviderFactory __m_SocketProviderFactory;
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
        __mapChildren.put("MessageFactory", TcpInitiator.MessageFactory.get_CLASS());
        __mapChildren.put("Queue", com.tangosol.coherence.component.util.daemon.QueueProcessor.Queue.get_CLASS());
        __mapChildren.put("TcpConnection", TcpInitiator.TcpConnection.get_CLASS());
        }
    
    // Default constructor
    public TcpInitiator()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public TcpInitiator(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            setCloseOnExit(new com.tangosol.util.SafeHashSet());
            setDaemonState(0);
            setDefaultGuardRecovery(0.9F);
            setDefaultGuardTimeout(60000L);
            setNotifier(new com.oracle.coherence.common.base.SingleWaiterMultiNotifier());
            setProtocolMap(new java.util.HashMap());
            setReceiverMap(new java.util.HashMap());
            setRequestTimeout(30000L);
            setSerializerMap(new java.util.WeakHashMap());
            setSocketOptions(new com.tangosol.net.SocketOptions());
            }
        catch (java.lang.Exception e)
            {
            // re-throw as a runtime exception
            throw new com.tangosol.util.WrapperException(e);
            }
        
        // containment initialization: children
        _addChild(new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.DaemonPool("DaemonPool", this, true), "DaemonPool");
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
        return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.initiator.TcpInitiator();
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
            clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/peer/initiator/TcpInitiator".replace('/', '.'));
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
        // import com.tangosol.internal.net.service.peer.initiator.DefaultTcpInitiatorDependencies;
        // import com.tangosol.internal.net.service.peer.initiator.TcpInitiatorDependencies;
        
        return new DefaultTcpInitiatorDependencies((TcpInitiatorDependencies) deps);
        }
    
    // Declared at the super level
    public synchronized void configure(com.tangosol.run.xml.XmlElement xml)
        {
        // import com.tangosol.internal.net.service.peer.initiator.DefaultTcpInitiatorDependencies;
        // import com.tangosol.internal.net.service.peer.initiator.LegacyXmlTcpInitiatorHelper as com.tangosol.internal.net.service.peer.initiator.LegacyXmlTcpInitiatorHelper;
        
        setDependencies(com.tangosol.internal.net.service.peer.initiator.LegacyXmlTcpInitiatorHelper.fromXml(xml,
            new DefaultTcpInitiatorDependencies(), getOperationalContext(),
            getContextClassLoader()));
        
        setServiceConfig(xml);
        }
    
    /**
     * Configure the given Socket.
    * 
    * @param socket  the Socket to configure
     */
    protected void configureSocket(java.net.Socket socket)
        {
        // import Component.Net.Extend.Util.TcpUtil;
        // import com.tangosol.net.SocketOptions;
        // import com.tangosol.util.Base;
        // import java.net.SocketAddress;
        
        try
            {
            getSocketOptions().apply(socket);
            }
        catch (Exception e)
            {
            throw Base.ensureRuntimeException(e, "error configuring Socket");
            }
        
        // bind the socket to the local address
        SocketAddress addr = getLocalAddress();
        if (addr != null)
            {
            _trace("Binding Socket to " + TcpUtil.toString(addr), 6);
            try
                {
                socket.bind(addr);
                }
            catch (Exception e)
                {
                throw Base.ensureRuntimeException(e, "error binding Socket to "
                        + TcpUtil.toString(addr));
                }
        
            if (!socket.isBound())
                {
                throw new RuntimeException("could not bind Socket to "
                        + TcpUtil.toString(addr));
                }
            }
        }
    
    // Declared at the super level
    /**
     * Getter for property Description.<p>
    * Human-readable description of additional Service properties. Used by
    * toString().
     */
    public String getDescription()
        {
        // import Component.Net.Extend.Util.TcpUtil;
        // import java.net.SocketAddress;
        // import java.net.SocketOptions;
        
        StringBuffer sb = new StringBuffer(super.getDescription());
        
        sb.append(", SocketProvider=")
          .append(getSocketProvider());
        
        SocketAddress addr = getLocalAddress();
        if (addr != null)
            {
            sb.append(", LocalAddress=")
              .append(TcpUtil.toString(addr));
            }
        
        sb.append(", RemoteAddresses=")
          .append(getRemoteAddressProvider());
        
        SocketOptions options = getSocketOptions();
        if (options != null)
            {
            sb.append(", ").append(options);
            }
        
        return sb.toString();
        }
    
    // Accessor for the property "LocalAddress"
    /**
     * Getter for property LocalAddress.<p>
    * The local SocketAddress that all Socket objects created by this
    * TcpInitiator will be bound to. If null, a SocketAddress created from an
    * ephemeral port and a valid local address will be used.
     */
    public java.net.SocketAddress getLocalAddress()
        {
        return __m_LocalAddress;
        }
    
    // Accessor for the property "RemoteAddressProvider"
    /**
     * Getter for property RemoteAddressProvider.<p>
    * The SocketAddressProvider used by the TcpInitiator to obtain the
    * address(es) of the remote TcpAcceptor(s) that it will connect to.
     */
    public com.tangosol.net.SocketAddressProvider getRemoteAddressProvider()
        {
        return __m_RemoteAddressProvider;
        }
    
    // Accessor for the property "SocketOptions"
    /**
     * Getter for property SocketOptions.<p>
    * The Initiator's socket configuration. 
     */
    public com.tangosol.net.SocketOptions getSocketOptions()
        {
        return __m_SocketOptions;
        }
    
    // Accessor for the property "SocketProvider"
    /**
     * Getter for property SocketProvider.<p>
    * The SocketProvider used by the TcpAcceptor to open ServerSocketChannels.
     */
    public com.oracle.coherence.common.net.SocketProvider getSocketProvider()
        {
        return __m_SocketProvider;
        }
    
    // Accessor for the property "SocketProviderFactory"
    /**
     * Getter for property SocketProviderFactory.<p>
    * The factory to use in creating the SocketProvider.
     */
    public com.tangosol.net.SocketProviderFactory getSocketProviderFactory()
        {
        // import com.tangosol.net.OperationalContext;
        // import com.tangosol.net.SocketProviderFactory;
        
        SocketProviderFactory factory = __m_SocketProviderFactory;
        if (factory == null)
            {
            OperationalContext ctx = getOperationalContext();
            if (ctx == null)
                {
                factory = new SocketProviderFactory();
                }
            else
                {
                factory = ctx.getSocketProviderFactory();
                }
            setSocketProviderFactory(factory);
            }
        
        return factory;
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
        TcpInitiator.TcpConnection connection = (TcpInitiator.TcpConnection) _newChild("TcpConnection");
        connection.setConnectionManager(this);
        connection.setRequestSendTimeout(getRequestSendTimeout());
        
        return connection;
        }
    
    /**
     * Factory method: create and configure a new Socket.
    * 
    * @return a new Socket
     */
    public java.net.Socket instantiateSocket()
        {
        // import com.tangosol.util.Base;
        // import java.io.IOException;
        // import java.net.Socket;
        
        Socket socket;
        try
            {
            socket = getSocketProvider().openSocket();
            }
        catch (IOException e)
            {
            throw Base.ensureRuntimeException(e, "error creating Socket");
            }
        
        configureSocket(socket);
        
        return socket;
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
        // import com.tangosol.coherence.config.builder.FactoryBasedAddressProviderBuilder;
        // import com.tangosol.coherence.config.builder.ParameterizedBuilder;
        // import com.tangosol.config.expression.NullParameterResolver;
        // import com.tangosol.internal.net.service.peer.initiator.TcpInitiatorDependencies;
        // import com.tangosol.net.AddressProviderFactory;
        // import com.tangosol.net.SocketAddressProvider;
        // import com.oracle.coherence.common.net.InetSocketAddress32;
        // import java.net.InetSocketAddress;
        // import java.net.SocketAddress;
        
        super.onDependencies(deps);
        
        TcpInitiatorDependencies tcpDeps = (TcpInitiatorDependencies) deps;
        
        SocketAddress addrLocal = tcpDeps.getLocalAddress();
        if (addrLocal instanceof InetSocketAddress)
            {
            InetSocketAddress addrLocalInet = (InetSocketAddress) addrLocal;
            addrLocal = new InetSocketAddress32(addrLocalInet.getAddress(), addrLocalInet.getPort());
            } 
        
        setLocalAddress(addrLocal);
        setSocketOptions(tcpDeps.getSocketOptions());
        setSocketProvider(tcpDeps.getSocketProviderBuilder().realize(null, null, null));
        
        ParameterizedBuilder bldr = tcpDeps.getRemoteAddressProviderBuilder();
        if (bldr == null)
            {
            // default to the "cluster-discovery" address provider which consists of MC or WKAs
            AddressProviderFactory factory = (AddressProviderFactory) getOperationalContext().getAddressProviderMap().get("cluster-discovery");
            if (factory != null)
                {
                if (factory instanceof ParameterizedBuilder)
                    {
                    bldr = (ParameterizedBuilder) factory;
                    }
                else
                    {
                    bldr = new FactoryBasedAddressProviderBuilder(factory);
                    }
                }
            }
        
        if (bldr != null)
            {
            setRemoteAddressProvider((SocketAddressProvider) bldr.realize(
                    new NullParameterResolver(), getContextClassLoader(), null));
            }
        }
    
    // Declared at the super level
    /**
     * The "component has been initialized" method-notification (kind of
    * WM_NCCREATE event) called out of setConstructed() for the topmost
    * component and that in turn notifies all the children. <p>
    * 
    * This notification gets called before the control returns back to this
    * component instantiator (using <code>new Component.X()</code> or
    * <code>_newInstance(sName)</code>) and on the same thread. In addition,
    * visual components have a "posted" notification <code>onInitUI</code> that
    * is called after (or at the same time as)  the control returns back to the
    * instatiator and possibly on a different thread.
     */
    public void onInit()
        {
        // import com.tangosol.net.SocketOptions;
        // import com.tangosol.util.Base;
        // import java.net.SocketException;
        
        super.onInit();
        
        try
            {
            SocketOptions options = getSocketOptions();
        
            // set TcpAcceptor socket option defaults
            options.setOption(SocketOptions.SO_KEEPALIVE, Boolean.TRUE);
            options.setOption(SocketOptions.TCP_NODELAY, Boolean.TRUE);
            options.setOption(SocketOptions.SO_LINGER, Integer.valueOf(0));    
            }
        catch (SocketException e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }
    
    // Declared at the super level
    /**
     * Open and return a new Connection.
    * 
    * @return a newly opened Connection
     */
    protected com.tangosol.coherence.component.net.extend.Connection openConnection()
        {
        // import Component.Net.Extend.Util.TcpUtil;
        // import com.oracle.coherence.common.net.InetSocketAddress32;
        // import com.tangosol.net.SocketAddressProvider;
        // import com.tangosol.net.messaging.ConnectionException;
        // import com.oracle.coherence.common.base.Blocking;
        // import java.net.Socket;
        // import java.net.SocketAddress;
        // import java.util.ArrayList;
        // import java.util.Iterator;
        // import java.util.LinkedList;
        // import java.util.List;
        // import java.util.Set;
        
        SocketAddressProvider provider = getRemoteAddressProvider();
        _assert(provider != null);
        
        // determine the Socket connect timeout
        int cMillis = Math.max(0, (int) getConnectTimeout());
        
        // open a new connection
        List      listAddr     = new LinkedList();
        Iterator  iterRedirect = null;
        Throwable cause        = null; 
        
        for ( ; ; )
            {
            TcpInitiator.TcpConnection connection = (TcpInitiator.TcpConnection) instantiateConnection();
        
            SocketAddress addr;
            if (iterRedirect == null || !iterRedirect.hasNext())
                {
                addr = provider.getNextAddress();
        
                // reset redirection information
                iterRedirect = null;
                }
            else
                {
                addr = (SocketAddress) iterRedirect.next();
        
                // update redirection information
                connection.setRedirect(true);
                }
        
            if (addr == null)
                {
                break;
                }
            else if (((InetSocketAddress32) addr).getAddress().isMulticastAddress())
                {
                // Note having such an address is only useful to the RemoteNameService
                continue;
                }
        
            String sAddr = TcpUtil.toString(addr);
            listAddr.add(sAddr);
        
            // create and configure a new Socket; otherwise, some JVMs
            // may throw a SocketException when the Socket is reused
            Socket socket = instantiateSocket();
        
            Set setClose = getCloseOnExit();
            setClose.add(socket); // in case of concurrent stop
            
            try
                {
                if (iterRedirect == null)
                    {
                    _trace("Connecting Socket to " + sAddr, 6);
                    }
                else
                    {
                    _trace("Redirecting Socket to " + sAddr, 6);
                    }
        
                Blocking.connect(socket, addr, cMillis);
                
                connection.setSocket(socket);
                }
            catch (Exception e)
                {
                _trace("Error connecting Socket to " + sAddr + ": " + e, 6);
                TcpUtil.close(socket);
        
                // if we aren't currently redirecting, or we've tried the last redirect
                // address, reject the last address supplied by the address provider
                if (iterRedirect == null || !iterRedirect.hasNext())
                    {
                    provider.reject(e);
                    }
                continue;
                }
            finally
                {
                setClose.remove(socket);
                }
        
            try
                {
                connection.open();
                }
            catch (Exception e)
                {
                if (iterRedirect == null && connection.isRedirect())
                    {
                    List list = connection.getRedirectList();
        
                    // create a SocketAddress list from from the redirect list
                    List listRedirect = new ArrayList(list.size());
                    for (Iterator iter = list.iterator(); iter.hasNext(); )
                        {
                        Object[] ao = (Object[]) iter.next();
                        String   s  = (String) ao[0];
                        int      n  = ((Integer) ao[1]).intValue();
                        listRedirect.add(new InetSocketAddress32(s, n));
                        }
                    iterRedirect = listRedirect.iterator();
                    }
                else
                    {
                    cause = e;
                    _trace("Error establishing a connection with " +  sAddr + ": " + e, 6);
        
                    // if we aren't current redirecting, or we've tried the last redirect
                    // address, reject the last address supplied by the address provider
                    if (iterRedirect == null || !iterRedirect.hasNext())
                        {
                        provider.reject(e);
                        }
                    }
                continue;
                }
        
            provider.accept();
            return connection;
            }
        
        String sMessage = "could not establish a connection to one of the following addresses: " + listAddr;
        throw cause == null
                ? new ConnectionException(sMessage)
                : new ConnectionException(sMessage, cause);
        }
    
    // Accessor for the property "LocalAddress"
    /**
     * Setter for property LocalAddress.<p>
    * The local SocketAddress that all Socket objects created by this
    * TcpInitiator will be bound to. If null, a SocketAddress created from an
    * ephemeral port and a valid local address will be used.
     */
    protected void setLocalAddress(java.net.SocketAddress addr)
        {
        __m_LocalAddress = addr;
        }
    
    // Accessor for the property "RemoteAddressProvider"
    /**
     * Setter for property RemoteAddressProvider.<p>
    * The SocketAddressProvider used by the TcpInitiator to obtain the
    * address(es) of the remote TcpAcceptor(s) that it will connect to.
     */
    public void setRemoteAddressProvider(com.tangosol.net.SocketAddressProvider provider)
        {
        __m_RemoteAddressProvider = provider;
        }
    
    // Accessor for the property "SocketOptions"
    /**
     * Setter for property SocketOptions.<p>
    * The Initiator's socket configuration. 
     */
    protected void setSocketOptions(com.tangosol.net.SocketOptions options)
        {
        _assert(options != null);
        
        __m_SocketOptions = (options);
        }
    
    // Accessor for the property "SocketProvider"
    /**
     * Setter for property SocketProvider.<p>
    * The SocketProvider used by the TcpAcceptor to open ServerSocketChannels.
     */
    public void setSocketProvider(com.oracle.coherence.common.net.SocketProvider pSocketProvider)
        {
        __m_SocketProvider = pSocketProvider;
        }
    
    // Accessor for the property "SocketProviderFactory"
    /**
     * Setter for property SocketProviderFactory.<p>
    * The factory to use in creating the SocketProvider.
     */
    public void setSocketProviderFactory(com.tangosol.net.SocketProviderFactory factory)
        {
        __m_SocketProviderFactory = factory;
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.initiator.TcpInitiator$MessageFactory
    
    /**
     * MessageFactory implementation for version 2 of the MessagingProtocol.
     * This MessageFactory contains Message classes necessary to manage the
     * lifecycle of Connections and Channels.
     *  
     * The type identifiers of the Message classes instantiated by this
     * MessageFactory are organized as follows:
     * 
     * Internal (<0):
     * 
     * (-1)  AcceptChannel
     * (-2)  CloseChannel
     * (-3)  CloseConnection
     * (-4)  CreateChannel
     * (-5)  NotifyShutdown
     * (-6)  NotifyStartup
     * (-7)  OpenChannel
     * (-8)  OpenConnection
     * (-9)  Response
     * (-10) EncodedMessage
     * 
     * Connection Lifecycle (0 - 10):
     * 
     * (0)  OpenConnectionResponse (*)
     * (1)  OpenConnectionRequest
     * (3)  PingRequest
     * (4)  PingResponse
     * (10) NotifyConnectionClosed
     * 
     * * The OpenConnectionResponse has type identifier 0 for historical
     * reasons. Prior to version 2 of the Messaging Protocol, all Request
     * messages used a common Response type with type identifier 0. Since the
     * first Response that a client expects to receive is an
     * OpenConnectionResponse, this allows version 2 and newer servers to
     * rejects connection attempts from version 1 clients.
     * 
     * Channel Lifecycle (11-20):
     * 
     * (11) OpenChannelRequest
     * (12) OpenChannelResponse
     * (13) AcceptChannelRequest
     * (14) AcceptChannelResponse
     * (20) NotifyChannelClosed
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class MessageFactory
            extends    com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.Initiator.MessageFactory
        {
        // ---- Fields declarations ----
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
            __mapChildren.put("AcceptChannel", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.AcceptChannel.get_CLASS());
            __mapChildren.put("AcceptChannelRequest", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.AcceptChannelRequest.get_CLASS());
            __mapChildren.put("AcceptChannelResponse", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.AcceptChannelResponse.get_CLASS());
            __mapChildren.put("CloseChannel", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.CloseChannel.get_CLASS());
            __mapChildren.put("CloseConnection", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.CloseConnection.get_CLASS());
            __mapChildren.put("CreateChannel", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.CreateChannel.get_CLASS());
            __mapChildren.put("EncodedMessage", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.EncodedMessage.get_CLASS());
            __mapChildren.put("NotifyChannelClosed", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.NotifyChannelClosed.get_CLASS());
            __mapChildren.put("NotifyConnectionClosed", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.NotifyConnectionClosed.get_CLASS());
            __mapChildren.put("NotifyShutdown", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.NotifyShutdown.get_CLASS());
            __mapChildren.put("NotifyStartup", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.NotifyStartup.get_CLASS());
            __mapChildren.put("OpenChannel", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.OpenChannel.get_CLASS());
            __mapChildren.put("OpenChannelRequest", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.OpenChannelRequest.get_CLASS());
            __mapChildren.put("OpenChannelResponse", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.OpenChannelResponse.get_CLASS());
            __mapChildren.put("OpenConnection", com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.Initiator.MessageFactory.OpenConnection.get_CLASS());
            __mapChildren.put("OpenConnectionRequest", TcpInitiator.MessageFactory.OpenConnectionRequest.get_CLASS());
            __mapChildren.put("OpenConnectionResponse", TcpInitiator.MessageFactory.OpenConnectionResponse.get_CLASS());
            __mapChildren.put("PingRequest", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.PingRequest.get_CLASS());
            __mapChildren.put("PingResponse", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.PingResponse.get_CLASS());
            __mapChildren.put("Response", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.Response.get_CLASS());
            }
        
        // Default constructor
        public MessageFactory()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public MessageFactory(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.initiator.TcpInitiator.MessageFactory();
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
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/peer/initiator/TcpInitiator$MessageFactory".replace('/', '.'));
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
        
        //++ getter for autogen property _ChildClasses
        /**
         * This is an auto-generated method that returns the map of design time
        * [static] children.
        * 
        * Note: the class generator will ignore any custom implementation for
        * this behavior.
         */
        protected java.util.Map get_ChildClasses()
            {
            return __mapChildren;
            }

        // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.initiator.TcpInitiator$MessageFactory$OpenConnectionRequest
        
        /**
         * This Request is used to open a new Channel.
         */
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class OpenConnectionRequest
                extends    com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.Initiator.MessageFactory.OpenConnectionRequest
            {
            // ---- Fields declarations ----
            
            /**
             * Property Redirect
             *
             * True if the TcpConnection is being opened in response to a
             * redirection.
             */
            private boolean __m_Redirect;
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
                __mapChildren.put("Status", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.OpenConnectionRequest.Status.get_CLASS());
                }
            
            // Default constructor
            public OpenConnectionRequest()
                {
                this(null, null, true);
                }
            
            // Initializing constructor
            public OpenConnectionRequest(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.initiator.TcpInitiator.MessageFactory.OpenConnectionRequest();
                }
            
            //++ getter for static property _CLASS
            /**
             * Getter for property _CLASS.<p>
            * Property with auto-generated accessor that returns the Class
            * object for a given component.
             */
            public static Class get_CLASS()
                {
                Class clz;
                try
                    {
                    clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/peer/initiator/TcpInitiator$MessageFactory$OpenConnectionRequest".replace('/', '.'));
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
            * Note: the class generator will ignore any custom implementation
            * for this behavior.
             */
            private com.tangosol.coherence.Component get_Module()
                {
                return this.get_Parent().get_Parent();
                }
            
            //++ getter for autogen property _ChildClasses
            /**
             * This is an auto-generated method that returns the map of design
            * time [static] children.
            * 
            * Note: the class generator will ignore any custom implementation
            * for this behavior.
             */
            protected java.util.Map get_ChildClasses()
                {
                return __mapChildren;
                }
            
            // Declared at the super level
            /**
             * Return a human-readable description of this component.
            * 
            * @return a String representation of this component
             */
            protected String getDescription()
                {
                return super.getDescription()
                        + ", RedirectSupported=" + isRedirectSupported()
                        + ", Redirect="          + isRedirect();
                }
            
            // Accessor for the property "Redirect"
            /**
             * Getter for property Redirect.<p>
            * True if the TcpConnection is being opened in response to a
            * redirection.
             */
            public boolean isRedirect()
                {
                return __m_Redirect;
                }
            
            // Accessor for the property "RedirectSupported"
            /**
             * Getter for property RedirectSupported.<p>
            * True if the TcpInitiator supports redirection.
             */
            public boolean isRedirectSupported()
                {
                return true;
                }
            
            // Declared at the super level
            public void readExternal(com.tangosol.io.pof.PofReader in)
                    throws java.io.IOException
                {
                super.readExternal(in);
                
                if (in.readBoolean(10)) /* redirect supported? */
                    {
                    setRedirect(in.readBoolean(11));
                    }
                }
            
            // Declared at the super level
            /**
             * Setter for property ConnectionOpen.<p>
            * The Connection to open.
             */
            public void setConnectionOpen(com.tangosol.coherence.component.net.extend.Connection connection)
                {
                setRedirect(((TcpInitiator.TcpConnection) connection).isRedirect());
                super.setConnectionOpen(connection);
                }
            
            // Accessor for the property "Redirect"
            /**
             * Setter for property Redirect.<p>
            * True if the TcpConnection is being opened in response to a
            * redirection.
             */
            public void setRedirect(boolean f)
                {
                __m_Redirect = f;
                }
            
            // Declared at the super level
            public void writeExternal(com.tangosol.io.pof.PofWriter out)
                    throws java.io.IOException
                {
                super.writeExternal(out);
                
                out.writeBoolean(10, isRedirectSupported());
                out.writeBoolean(11, isRedirect());
                }
            }

        // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.initiator.TcpInitiator$MessageFactory$OpenConnectionResponse
        
        /**
         * Response to an OpenChannelRequest.
         */
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class OpenConnectionResponse
                extends    com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.Initiator.MessageFactory.OpenConnectionResponse
            {
            // ---- Fields declarations ----
            
            /**
             * Property Redirect
             *
             * True if the TcpConnection should be redirected.
             */
            private boolean __m_Redirect;
            
            /**
             * Property RedirectList
             *
             * A list of TCP/IP addresses that the TcpConnection should be
             * redirected to. Each element of the list is a two element array,
             * with the first element being the IP address in string format and
             * the second being the port number. 
             */
            private java.util.List __m_RedirectList;
            
            // Default constructor
            public OpenConnectionResponse()
                {
                this(null, null, true);
                }
            
            // Initializing constructor
            public OpenConnectionResponse(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.initiator.TcpInitiator.MessageFactory.OpenConnectionResponse();
                }
            
            //++ getter for static property _CLASS
            /**
             * Getter for property _CLASS.<p>
            * Property with auto-generated accessor that returns the Class
            * object for a given component.
             */
            public static Class get_CLASS()
                {
                Class clz;
                try
                    {
                    clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/peer/initiator/TcpInitiator$MessageFactory$OpenConnectionResponse".replace('/', '.'));
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
            * Note: the class generator will ignore any custom implementation
            * for this behavior.
             */
            private com.tangosol.coherence.Component get_Module()
                {
                return this.get_Parent().get_Parent();
                }
            
            // Declared at the super level
            /**
             * Return a human-readable description of this component.
            * 
            * @return a String representation of this component
             */
            protected String getDescription()
                {
                return super.getDescription()
                        + ", Redirect="     + isRedirect()
                        + ", RedirectList=" + getRedirectList();
                }
            
            // Accessor for the property "RedirectList"
            /**
             * Getter for property RedirectList.<p>
            * A list of TCP/IP addresses that the TcpConnection should be
            * redirected to. Each element of the list is a two element array,
            * with the first element being the IP address in string format and
            * the second being the port number. 
             */
            public java.util.List getRedirectList()
                {
                return __m_RedirectList;
                }
            
            // Accessor for the property "Redirect"
            /**
             * Getter for property Redirect.<p>
            * True if the TcpConnection should be redirected.
             */
            public boolean isRedirect()
                {
                return __m_Redirect;
                }
            
            // Declared at the super level
            public void readExternal(com.tangosol.io.pof.PofReader in)
                    throws java.io.IOException
                {
                // import java.util.ArrayList;
                // import java.util.List;
                
                super.readExternal(in);
                
                setRedirect(in.readBoolean(10));
                if (isRedirect())
                    {
                    setRedirectList((List) in.readCollection(11, new ArrayList()));
                    }
                }
            
            // Declared at the super level
            public void run()
                {
                // update the connection with redirection information
                TcpInitiator.TcpConnection connection = (TcpInitiator.TcpConnection) getChannel().getConnection();
                connection.setRedirect(isRedirect());
                connection.setRedirectList(getRedirectList());
                
                super.run();
                }
            
            // Accessor for the property "Redirect"
            /**
             * Setter for property Redirect.<p>
            * True if the TcpConnection should be redirected.
             */
            public void setRedirect(boolean f)
                {
                __m_Redirect = f;
                }
            
            // Accessor for the property "RedirectList"
            /**
             * Setter for property RedirectList.<p>
            * A list of TCP/IP addresses that the TcpConnection should be
            * redirected to. Each element of the list is a two element array,
            * with the first element being the IP address in string format and
            * the second being the port number. 
             */
            public void setRedirectList(java.util.List list)
                {
                __m_RedirectList = list;
                }
            
            // Declared at the super level
            public void writeExternal(com.tangosol.io.pof.PofWriter out)
                    throws java.io.IOException
                {
                super.writeExternal(out);
                
                out.writeBoolean(10, isRedirect());
                if (isRedirect())
                    {
                    out.writeCollection(11, getRedirectList());
                    }
                }
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.initiator.TcpInitiator$TcpConnection
    
    /**
     * Connection implementation that wraps a TCP/IP Socket.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class TcpConnection
            extends    com.tangosol.coherence.component.net.extend.connection.TcpConnection
        {
        // ---- Fields declarations ----
        
        /**
         * Property DataInputStream
         *
         * The DataInputStream around the underlying Socket InputStream.
         */
        private java.io.DataInputStream __m_DataInputStream;
        
        /**
         * Property DataOutputStream
         *
         * The DataOutputStream around the underlying Socket OutputStream.
         */
        private java.io.DataOutputStream __m_DataOutputStream;
        
        /**
         * Property DataOutputStreamLock
         *
         * Lock used to protect access to DataOutputStream. 
         * @see #send(WriteBuffer wb)
         */
        private transient java.util.concurrent.locks.ReentrantLock __m_DataOutputStreamLock;
        
        /**
         * Property Reader
         *
         */
        private TcpInitiator.TcpConnection.TcpReader __m_Reader;
        
        /**
         * Property Redirect
         *
         * True if this TcpConnection has been or should be redirected.
         */
        private boolean __m_Redirect;
        
        /**
         * Property RedirectList
         *
         * A list of TCP/IP addresses that the TcpConnection should be
         * redirected to. Each element of the list is a two element array, with
         * the first element being the IP address in string format and the
         * second being the port number. 
         */
        private java.util.List __m_RedirectList;
        
        /**
         * Property RequestSendTimeout
         *
         * The send time of the last outstanding PingRequest or 0 if a
         * PingRequest is not outstanding.
         */
        private long __m_RequestSendTimeout;
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
            __mapChildren.put("TcpReader", TcpInitiator.TcpConnection.TcpReader.get_CLASS());
            }
        
        // Default constructor
        public TcpConnection()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public TcpConnection(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                setChannelArray(new com.tangosol.util.SparseArray());
                setChannelPendingArray(new com.tangosol.util.SparseArray());
                setDataOutputStreamLock(new java.util.concurrent.locks.ReentrantLock());
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.initiator.TcpInitiator.TcpConnection();
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
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/peer/initiator/TcpInitiator$TcpConnection".replace('/', '.'));
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
        
        //++ getter for autogen property _ChildClasses
        /**
         * This is an auto-generated method that returns the map of design time
        * [static] children.
        * 
        * Note: the class generator will ignore any custom implementation for
        * this behavior.
         */
        protected java.util.Map get_ChildClasses()
            {
            return __mapChildren;
            }
        
        // Declared at the super level
        /**
         * The close() implementation method. This method is called on the
        * service thread.
        * 
        * @param fNotify  if true, notify the peer that the Connection is being
        * closed
        * @param e  the optional reason why the Connection is being closed
        * @param cMillis  the number of milliseconds to wait for the Connection
        * to close; pass 0L to perform a non-blocking close or -1L to wait
        * forever
        * 
        * @return true iff the invocation of this method closed the Connection
         */
        public boolean closeInternal(boolean fNotify, Throwable e, long cMillis)
            {
            // import Component.Net.Extend.Util.TcpUtil;
            // import java.io.InputStream;
            // import java.io.IOException;
            // import java.io.OutputStream;
            
            if (super.closeInternal(fNotify, e, cMillis))
                {
                TcpInitiator.TcpConnection.TcpReader reader = getReader();
                if (reader != null)
                    {
                    reader.stop();
                    setReader(null);
                    }
            
                InputStream in = getDataInputStream();
                if (in != null)
                    {
                    try
                        {
                        in.close();
                        }
                    catch (IOException ee) { /*ignore*/ }
                    setDataInputStream(null);
                    }
            
                OutputStream out = getDataOutputStream();
                if (out != null)
                    {
                    try
                        {
                        out.close();
                        }
                    catch (IOException ee) { /*ignore*/ }
                    setDataOutputStream(null);
                    }
            
                TcpUtil.close(getSocket());
            
                return true;
                }
            
            return false;
            }
        
        // Accessor for the property "DataInputStream"
        /**
         * Getter for property DataInputStream.<p>
        * The DataInputStream around the underlying Socket InputStream.
         */
        public java.io.DataInputStream getDataInputStream()
            {
            return __m_DataInputStream;
            }
        
        // Accessor for the property "DataOutputStream"
        /**
         * Getter for property DataOutputStream.<p>
        * The DataOutputStream around the underlying Socket OutputStream.
         */
        public java.io.DataOutputStream getDataOutputStream()
            {
            return __m_DataOutputStream;
            }
        
        // Accessor for the property "DataOutputStreamLock"
        /**
         * Getter for property DataOutputStreamLock.<p>
        * Lock used to protect access to DataOutputStream. 
        * @see #send(WriteBuffer wb)
         */
        public java.util.concurrent.locks.ReentrantLock getDataOutputStreamLock()
            {
            return __m_DataOutputStreamLock;
            }
        
        // Accessor for the property "Reader"
        /**
         * Getter for property Reader.<p>
         */
        public TcpInitiator.TcpConnection.TcpReader getReader()
            {
            return __m_Reader;
            }
        
        // Accessor for the property "RedirectList"
        /**
         * Getter for property RedirectList.<p>
        * A list of TCP/IP addresses that the TcpConnection should be
        * redirected to. Each element of the list is a two element array, with
        * the first element being the IP address in string format and the
        * second being the port number. 
         */
        public java.util.List getRedirectList()
            {
            return __m_RedirectList;
            }
        
        // Accessor for the property "RequestSendTimeout"
        /**
         * Getter for property RequestSendTimeout.<p>
        * The send time of the last outstanding PingRequest or 0 if a
        * PingRequest is not outstanding.
         */
        public long getRequestSendTimeout()
            {
            return __m_RequestSendTimeout;
            }
        
        // Accessor for the property "Redirect"
        /**
         * Getter for property Redirect.<p>
        * True if this TcpConnection has been or should be redirected.
         */
        public boolean isRedirect()
            {
            return __m_Redirect;
            }
        
        // Declared at the super level
        /**
         * The open() implementation method. This method is called on the
        * service thread.
         */
        public void openInternal()
            {
            // import java.io.BufferedInputStream;
            // import java.io.BufferedOutputStream;
            // import java.io.DataInputStream;
            // import java.io.DataOutputStream;
            // import java.io.IOException;
            // import java.net.Socket;
            
            super.openInternal();
            
            Socket socket = getSocket();
            _assert(socket != null);
            
            try
                {
                // COH-1253:
                // wrap the Socket input/output streams with buffered input/output streams
                // with internal buffers large enough to hold a packed integer; this
                // avoids multiple system calls while writing/reading the message size
                setDataInputStream(new DataInputStream(
                        new BufferedInputStream(socket.getInputStream(), 5)));
                setDataOutputStream(new DataOutputStream(
                        new BufferedOutputStream(socket.getOutputStream(), 5)));
                }
            catch (IOException e)
                {
                closeInternal(false, e, -1L);
                throw ensureRuntimeException(e, "error opening connection");
                }
            
            TcpInitiator.TcpConnection.TcpReader reader = (TcpInitiator.TcpConnection.TcpReader) _newChild("TcpReader");
            reader.start();
            setReader(reader);
            }
        
        // Declared at the super level
        /**
         * Send the given WriteBuffer over this Connection.
        * 
        * @param wb  the WriteBuffer to send
        * 
        * @throws ConnectionException on fatal Connection error
         */
        public void send(com.tangosol.io.WriteBuffer wb)
                throws com.tangosol.net.messaging.ConnectionException
            {
            // import com.tangosol.io.ByteArrayWriteBuffer;
            // import com.tangosol.net.RequestTimeoutException;
            // import com.tangosol.net.messaging.ConnectionException;
            // import com.tangosol.util.ExternalizableHelper;
            // import java.io.DataOutputStream;
            // import java.io.IOException;
            // import java.util.concurrent.TimeUnit;
            // import java.util.concurrent.locks.Lock;
            
            // write the length-encoded Message to the Socket OutputStream. According
            // to the following post, there is no guarantee that the write operation
            // is thread safe, so we must synchronize access to the output stream:
            //
            // http://forum.java.sun.com/thread.jspa?threadID=792640&tstart=165
            
            super.send(wb);
            
            byte[]           ab      = ((ByteArrayWriteBuffer) wb).getRawByteArray();
            int              cb      = wb.length();
            DataOutputStream out     = getDataOutputStream();
            Lock             lock    = getDataOutputStreamLock();
            boolean          fLock   = false;
            long             cMillis = getRequestSendTimeout();
            
            try
                {
                // request send timeout has been set
                if (cMillis > 0)
                    {
                    fLock = lock.tryLock(cMillis, TimeUnit.MILLISECONDS);
                    }
                else
                    {
                    lock.lockInterruptibly();
                    fLock = true;
                    }
                }
            catch (InterruptedException e)
                {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
                }
                
            if (fLock)
                {
                try
                    {
                    // Message length
                    ExternalizableHelper.writeInt(out, cb);
            
                    // Message contents
                    out.write(ab, 0, cb);
            
                    // flush
                    out.flush();
                    }
                catch (IOException e)
                    {
                    throw new ConnectionException(e, this);
                    }
                finally
                    {
                    lock.unlock();
                    }            
                }
            else
                {
                throw new RequestTimeoutException("socket write timed out after " +
                            cMillis);    
                }
            }
        
        // Accessor for the property "DataInputStream"
        /**
         * Setter for property DataInputStream.<p>
        * The DataInputStream around the underlying Socket InputStream.
         */
        protected void setDataInputStream(java.io.DataInputStream in)
            {
            __m_DataInputStream = in;
            }
        
        // Accessor for the property "DataOutputStream"
        /**
         * Setter for property DataOutputStream.<p>
        * The DataOutputStream around the underlying Socket OutputStream.
         */
        protected void setDataOutputStream(java.io.DataOutputStream out)
            {
            __m_DataOutputStream = out;
            }
        
        // Accessor for the property "DataOutputStreamLock"
        /**
         * Setter for property DataOutputStreamLock.<p>
        * Lock used to protect access to DataOutputStream. 
        * @see #send(WriteBuffer wb)
         */
        protected void setDataOutputStreamLock(java.util.concurrent.locks.ReentrantLock out)
            {
            __m_DataOutputStreamLock = out;
            }
        
        // Accessor for the property "Reader"
        /**
         * Setter for property Reader.<p>
         */
        protected void setReader(TcpInitiator.TcpConnection.TcpReader reader)
            {
            __m_Reader = reader;
            }
        
        // Accessor for the property "Redirect"
        /**
         * Setter for property Redirect.<p>
        * True if this TcpConnection has been or should be redirected.
         */
        public void setRedirect(boolean f)
            {
            __m_Redirect = f;
            }
        
        // Accessor for the property "RedirectList"
        /**
         * Setter for property RedirectList.<p>
        * A list of TCP/IP addresses that the TcpConnection should be
        * redirected to. Each element of the list is a two element array, with
        * the first element being the IP address in string format and the
        * second being the port number. 
         */
        public void setRedirectList(java.util.List list)
            {
            __m_RedirectList = list;
            }
        
        // Accessor for the property "RequestSendTimeout"
        /**
         * Setter for property RequestSendTimeout.<p>
        * The send time of the last outstanding PingRequest or 0 if a
        * PingRequest is not outstanding.
         */
        public void setRequestSendTimeout(long ldt)
            {
            __m_RequestSendTimeout = ldt;
            }

        // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.initiator.TcpInitiator$TcpConnection$TcpReader
        
        /**
         * The Daemon that is resposible for reading encoded Messages off the
         * parent TcpConnection InputStream and dispatching them to the
         * TcpInitiator.
         */
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class TcpReader
                extends    com.tangosol.coherence.component.util.Daemon
            {
            // ---- Fields declarations ----
            
            // Default constructor
            public TcpReader()
                {
                this(null, null, true);
                }
            
            // Initializing constructor
            public TcpReader(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                    setDaemonState(0);
                    setDefaultGuardRecovery(0.9F);
                    setDefaultGuardTimeout(60000L);
                    setNotifier(new com.oracle.coherence.common.base.SingleWaiterMultiNotifier());
                    }
                catch (java.lang.Exception e)
                    {
                    // re-throw as a runtime exception
                    throw new com.tangosol.util.WrapperException(e);
                    }
                
                // containment initialization: children
                _addChild(new com.tangosol.coherence.component.util.Daemon.Guard("Guard", this, true), "Guard");
                
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
                return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.initiator.TcpInitiator.TcpConnection.TcpReader();
                }
            
            //++ getter for static property _CLASS
            /**
             * Getter for property _CLASS.<p>
            * Property with auto-generated accessor that returns the Class
            * object for a given component.
             */
            public static Class get_CLASS()
                {
                Class clz;
                try
                    {
                    clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/peer/initiator/TcpInitiator$TcpConnection$TcpReader".replace('/', '.'));
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
            * Note: the class generator will ignore any custom implementation
            * for this behavior.
             */
            private com.tangosol.coherence.Component get_Module()
                {
                return this.get_Parent().get_Parent();
                }
            
            // Declared at the super level
            /**
             * Getter for property ThreadName.<p>
            * Specifies the name of the daemon thread. If not specified, the
            * component name will be used.
            * 
            * This property can be set at design time or runtime. If set at
            * runtime, this property must be configured before start() is
            * invoked to cause the daemon thread to have the specified name.
             */
            public String getThreadName()
                {
                return ((TcpInitiator) get_Module()).getServiceName() + ':' + super.getThreadName();
                }
            
            // Declared at the super level
            /**
             * This event occurs when an exception is thrown from onEnter,
            * onWait, onNotify and onExit.
            * 
            * If the exception should terminate the daemon, call stop(). The
            * default implementation prints debugging information and
            * terminates the daemon.
            * 
            * @param e  the Throwable object (a RuntimeException or an Error)
            * 
            * @throws RuntimeException may be thrown; will terminate the daemon
            * @throws Error may be thrown; will terminate the daemon
             */
            protected void onException(Throwable e)
                {
                // see TcpConnection.closeInternal()
                if (!isExiting())
                    {
                    try
                        {
                        TcpInitiator.TcpConnection connection = (TcpInitiator.TcpConnection) get_Parent();
                        connection.close(/*fNotify*/ false, e, /*fWait*/ false);
                        }
                    catch (Exception ee) {}
                    }
                
                super.onException(e);
                }
            
            // Declared at the super level
            /**
             * Event notification to perform a regular daemon activity. To get
            * it called, another thread has to set Notification to true:
            * <code>daemon.setNotification(true);</code>
            * 
            * @see #onWait
             */
            protected void onNotify()
                {
                // import com.tangosol.io.ByteArrayReadBuffer;
                // import com.tangosol.net.messaging.ConnectionException;
                // import com.tangosol.util.ExternalizableHelper;
                // import java.io.DataInput;
                // import java.io.IOException;
                
                TcpInitiator        manager    = (TcpInitiator) get_Module();
                TcpInitiator.TcpConnection connection = (TcpInitiator.TcpConnection) get_Parent();
                DataInput      in         = connection.getDataInputStream();
                
                while (!isExiting())
                    {
                    try
                        {
                        int cb = ExternalizableHelper.readInt(in); // Message length
                        manager.enforceMaxIncomingMessageSize(cb);
                
                        if (cb < 0)
                            {
                            throw new IOException("Received a message with a negative length");
                            }
                        else if (cb == 0)
                            {
                            throw new IOException("Received a message with a length of zero");
                            }
                        else
                            {
                            byte[] ab = new byte[cb];
                            in.readFully(ab);
                
                            // update stats
                            connection.setStatsBytesReceived(connection.getStatsBytesReceived() + cb);
                            connection.setStatsReceived(connection.getStatsReceived() + 1);
                
                            // dispatch Message
                            manager.receive(new ByteArrayReadBuffer(ab), connection);
                            }
                        }
                    catch (IOException e)
                        {
                        // see TcpConnection.closeInternal()
                        if (!isExiting())
                            {
                            try
                                {
                                connection.close(
                                        /*fNotify*/ false,
                                        new ConnectionException(e, connection),
                                        /*fWait*/ false);
                                }
                            catch (Exception ee) {}
                            stop();
                            }
                        }
                    }
                }
            
            // Declared at the super level
            /**
             * Event notification called when  the daemon's Thread is waiting
            * for work.
            * 
            * @see #run
             */
            protected void onWait()
                    throws java.lang.InterruptedException
                {
                // all work is done in onNotify()
                return;
                }
            }
        }
    }
