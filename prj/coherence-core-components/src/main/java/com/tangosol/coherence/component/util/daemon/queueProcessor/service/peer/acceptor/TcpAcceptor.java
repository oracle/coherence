
/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.TcpAcceptor

package com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor;

import com.tangosol.coherence.component.net.extend.util.TcpUtil;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ProxyService;
import com.tangosol.coherence.component.util.queue.concurrentQueue.DualQueue;
import com.oracle.coherence.common.base.Blocking;
import com.tangosol.coherence.config.builder.ParameterizedBuilder;
import com.tangosol.config.expression.NullParameterResolver;
import com.tangosol.internal.net.service.peer.acceptor.DefaultTcpAcceptorDependencies;
import com.tangosol.internal.net.service.peer.acceptor.TcpAcceptorDependencies;
import com.tangosol.io.WriteBuffer;
import com.tangosol.io.nio.ByteBufferWriteBuffer;
import com.tangosol.net.InetAddressHelper;
import com.tangosol.net.OperationalContext;
import com.tangosol.net.Service;
import com.tangosol.net.SocketAddressProvider;
import com.tangosol.net.SocketOptions;
import com.tangosol.net.SocketProviderFactory;
import com.tangosol.net.messaging.Channel;
import com.tangosol.net.messaging.ConnectionException;
import com.tangosol.net.messaging.SuspectConnectionException;
import com.tangosol.util.Base;
import com.tangosol.util.Filter;
import com.tangosol.util.ThreadGate;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import javax.net.ssl.SSLException;

/**
 * A ConnectionAcceptor implementation that accepts Connections over TCP/IP.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class TcpAcceptor
        extends    com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.Acceptor
    {
    // ---- Fields declarations ----
    
    /**
     * Property AuthorizedHostFilter
     *
     * An optional com.tangosol.util.Filter that is used by the TcpAcceptor to
     * determine whether to accept a particular TcpInitiator. The evaluate()
     * method will be passed the java.net.InetAddress of the client.
     * Implementations
     * should return "true" to allow the client to connect.
     */
    private com.tangosol.util.Filter __m_AuthorizedHostFilter;
    
    /**
     * Property BufferPoolIn
     *
     * A pool of WriteBuffers used to read incoming Messages.
     */
    private transient TcpAcceptor.BufferPool __m_BufferPoolIn;
    
    /**
     * Property BufferPoolOut
     *
     * A pool of WriteBuffers used to write outgoing Messages.
     */
    private transient TcpAcceptor.BufferPool __m_BufferPoolOut;
    
    /**
     * Property ConnectionFlushQueue
     *
     * A Queue of TcpConnection objects that have outgoing data ready to send.
     */
    private com.tangosol.coherence.component.util.Queue __m_ConnectionFlushQueue;
    
    /**
     * Property ConnectionReleaseQueue
     *
     * A Queue of TcpConnection objects that have been closed.
     */
    private com.tangosol.coherence.component.util.Queue __m_ConnectionReleaseQueue;
    
    /**
     * Property DefaultLimitBytes
     *
     * The default size of the backlog (in bytes) at which point a connection
     * must be killed.
     * 
     * @since Coherence 3.4
     * @see $TcpConnection#checkSuspect
     */
    private long __m_DefaultLimitBytes;
    
    /**
     * Property DefaultLimitLength
     *
     * The default length of the backlog (in messages) at which point a
     * connection must be killed.
     * 
     * @since Coherence 3.4
     * @see $TcpConnection#checkSuspect
     */
    private int __m_DefaultLimitLength;
    
    /**
     * Property DefaultNominalBytes
     *
     * The default size of the backlog (in bytes) at which point a connection
     * is no longer considered suspect.
     * 
     * @since Coherence 3.4
     * @see $TcpConnection#checkSuspect
     */
    private long __m_DefaultNominalBytes;
    
    /**
     * Property DefaultNominalLength
     *
     * The default length of the backlog (in messages) at which point a
     * connection is no longer considered suspect.
     * 
     * @since Coherence 3.4
     * @see $TcpConnection#checkSuspect
     */
    private int __m_DefaultNominalLength;
    
    /**
     * Property DefaultSuspectBytes
     *
     * The default size of the backlog (in bytes) at which point a connection
     * may become suspect.
     * 
     * @since Coherence 3.4
     * @see $TcpConnection#checkSuspect
     */
    private long __m_DefaultSuspectBytes;
    
    /**
     * Property DefaultSuspectLength
     *
     * The default length of the backlog (in messages) at which point a
     * connection may become suspect.
     * 
     * @since Coherence 3.4
     * @see $TcpConnection#checkSuspect
     */
    private int __m_DefaultSuspectLength;
    
    /**
     * Property LastThrottleWarningTimestamp
     *
     * The timestamp of the last warning message indicating that a send() was
     * throttled due to outgoing BufferPool overflow.
     */
    private transient long __m_LastThrottleWarningTimestamp;
    
    /**
     * Property ListenBacklog
     *
     * The listen backlog of the ServerSocket created by this TcpAcceptor. If
     * equal or less than 0, then the default value will be used.
     */
    private int __m_ListenBacklog;
    
    /**
     * Property LocalAddress
     *
     * The local SocketAddress that all Socket objects created by this
     * TcpAccepter will be bound to.
     */
    private java.net.SocketAddress __m_LocalAddress;
    
    /**
     * Property LocalAddressProvider
     *
     * The SocketAddressProvider used by the TcpAcceptor to obtain the address
     * to bind to.
     */
    private com.tangosol.net.SocketAddressProvider __m_LocalAddressProvider;
    
    /**
     * Property Processor
     *
     * The TCP/IP I/O processor daemon.
     */
    private transient TcpAcceptor.TcpProcessor __m_Processor;
    
    /**
     * Property SocketOptions
     *
     * The Acceptor's socket configuration.
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
    
    /**
     * Property StatsUnauthorizedConnectionAttempts
     *
     * The number of connection attempts from unauthorized hosts.
     */
    private long __m_StatsUnauthorizedConnectionAttempts;
    
    /**
     * Property SuspectProtocolEnabled
     *
     * True if connection suspect protocol is enabled.
     * 
     * @since Coherence 3.4
     * @see $TcpConnection#send
     * @see $TcpConnection#checkSuspect
     */
    private boolean __m_SuspectProtocolEnabled;
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
        __mapChildren.put("BufferPool", TcpAcceptor.BufferPool.get_CLASS());
        __mapChildren.put("DispatchEvent", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.DispatchEvent.get_CLASS());
        __mapChildren.put("MessageBuffer", TcpAcceptor.MessageBuffer.get_CLASS());
        __mapChildren.put("MessageFactory", TcpAcceptor.MessageFactory.get_CLASS());
        __mapChildren.put("Queue", com.tangosol.coherence.component.util.daemon.QueueProcessor.Queue.get_CLASS());
        __mapChildren.put("TcpConnection", TcpAcceptor.TcpConnection.get_CLASS());
        }
    
    // Default constructor
    public TcpAcceptor()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public TcpAcceptor(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            setDefaultLimitBytes(100000000L);
            setDefaultLimitLength(60000);
            setDefaultNominalBytes(2000000L);
            setDefaultNominalLength(2000);
            setDefaultSuspectBytes(10000000L);
            setDefaultSuspectLength(10000);
            setNotifier(new com.oracle.coherence.common.base.SingleWaiterMultiNotifier());
            setProtocolMap(new java.util.HashMap());
            setReceiverMap(new java.util.HashMap());
            setRequestTimeout(30000L);
            setSerializerMap(new java.util.WeakHashMap());
            setSocketOptions(new com.tangosol.net.SocketOptions());
            setSuspectProtocolEnabled(true);
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
        _addChild(new TcpAcceptor.TcpProcessor("TcpProcessor", this, true), "TcpProcessor");
        
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
        return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.TcpAcceptor();
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
            clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/peer/acceptor/TcpAcceptor".replace('/', '.'));
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
     * Allocate a WriteBuffer for encoding a Message. After the Message has been
    * encoded and the resulting ReadBuffer sent via the Connection, the
    * WriteBuffer is released via the releaseWriteBuffer() method.
    * 
    * This method is called on both client and service threads.
    * 
    * @return a WriteBuffer that can be used to encode a Message
     */
    protected com.tangosol.io.WriteBuffer allocateWriteBuffer()
        {
        // import com.tangosol.io.MultiBufferWriteBuffer as com.tangosol.io.MultiBufferWriteBuffer;
        
        return new com.tangosol.io.MultiBufferWriteBuffer(getBufferPoolOut());
        }
    
    protected void bind(java.net.ServerSocket socket, java.net.SocketAddress addr, int nBacklog)
            throws java.io.IOException
        {
        socket.bind(addr, nBacklog);
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
        // import com.tangosol.internal.net.service.peer.acceptor.DefaultTcpAcceptorDependencies;
        // import com.tangosol.internal.net.service.peer.acceptor.TcpAcceptorDependencies;
        
        return new DefaultTcpAcceptorDependencies((TcpAcceptorDependencies) deps);
        }
    
    // Declared at the super level
    public synchronized void configure(com.tangosol.run.xml.XmlElement xml)
        {
        // import com.tangosol.internal.net.service.peer.acceptor.DefaultTcpAcceptorDependencies;
        // import com.tangosol.internal.net.service.peer.acceptor.LegacyXmlTcpAcceptorHelper as com.tangosol.internal.net.service.peer.acceptor.LegacyXmlTcpAcceptorHelper;
        
        setDependencies(com.tangosol.internal.net.service.peer.acceptor.LegacyXmlTcpAcceptorHelper.fromXml(
            xml, new DefaultTcpAcceptorDependencies(), getOperationalContext(),
            getContextClassLoader()));
        
        setServiceConfig(xml);
        }
    
    /**
     * Configure the BufferPool child component using the supplied dependencies.
     */
    private void configureBufferPool(TcpAcceptor.BufferPool pool, com.tangosol.internal.net.service.peer.acceptor.TcpAcceptorDependencies.BufferPoolConfig config)
        {
        pool.setBufferSize(config.getBufferSize());
        pool.setBufferType(config.getBufferType());
        pool.setCapacity(config.getCapacity());
        }
    
    /**
     * Configure the given ServerSocket.
    * 
    * @param socket  the ServerSocket to configure
     */
    public void configureSocket(java.net.ServerSocket socket)
        {
        // import Component.Net.Extend.Util.TcpUtil;
        // import com.tangosol.net.SocketAddressProvider;
        // import com.tangosol.util.Base;
        // import java.net.SocketAddress;
        
        try
            {
            // configured socket options
            getSocketOptions().apply(socket);
            }
        catch (Exception e)
            {
            throw Base.ensureRuntimeException(e, "error configuring ServerSocket");
            }
        
        SocketAddressProvider provider = getLocalAddressProvider();
        
        SocketAddress address;
        Exception eLast = null;
        while ((address = provider.getNextAddress()) != null)
            {
            try
                {
                bind(socket, address, getListenBacklog());
                validateLocalAddress(socket.getInetAddress());
                // used by the JMX reporting
                setLocalAddress(socket.getLocalSocketAddress());
        
                provider.accept();
                break;
                }
            catch (Exception e)
                {
                provider.reject(e);
                eLast = e;
                }
            }
        
        if (address == null || !socket.isBound())
            {
            throw Base.ensureRuntimeException(eLast, "Error binding ServerSocket to any of the supplied ports: " + provider);
            }
        }
    
    /**
     * Configure the given Socket.
    * 
    * @param socket  the Socket to configure
     */
    public void configureSocket(java.net.Socket socket)
        {
        // import Component.Net.Extend.Util.TcpUtil;
        // import com.tangosol.net.SocketOptions;
        // import com.tangosol.util.Base;
        
        try
            {    
            getSocketOptions().apply(socket);
            }
        catch (Exception e)
            {
            throw Base.ensureRuntimeException(e, "error configuring Socket");
            }
        }
    
    // Declared at the super level
    /**
     * Encode the given Message with the configured Codec into the specified
    * BufferOutput.
    * 
    * @param message  the Message to encode
    * @param bo  the BufferOutput that will be used to write out the encoded
    * Message
    * @param fFilter  if true, the BufferOutput will be filtered using the list
    * of configured WrapperStreamFactory objects
    * 
    * @throws IOException on encoding error
     */
    protected void encodeMessage(com.tangosol.net.messaging.Message message, com.tangosol.io.WriteBuffer.BufferOutput out, boolean fFilter)
            throws java.io.IOException
        {
        // reserve space at the front of the WriteBuffer to accomodate the encoded
        // Message length (see TcpProcessor#onWrite())
        for (int i = 0; i < 5; ++i)
            {
            out.write(0);
            }
        
        super.encodeMessage(message, out, fFilter);
        }
    
    // Accessor for the property "AuthorizedHostFilter"
    /**
     * Getter for property AuthorizedHostFilter.<p>
    * An optional com.tangosol.util.Filter that is used by the TcpAcceptor to
    * determine whether to accept a particular TcpInitiator. The evaluate()
    * method will be passed the java.net.InetAddress of the client.
    * Implementations
    * should return "true" to allow the client to connect.
     */
    public com.tangosol.util.Filter getAuthorizedHostFilter()
        {
        return __m_AuthorizedHostFilter;
        }
    
    // Accessor for the property "BufferPoolIn"
    /**
     * Getter for property BufferPoolIn.<p>
    * A pool of WriteBuffers used to read incoming Messages.
     */
    public TcpAcceptor.BufferPool getBufferPoolIn()
        {
        return __m_BufferPoolIn;
        }
    
    // Accessor for the property "BufferPoolOut"
    /**
     * Getter for property BufferPoolOut.<p>
    * A pool of WriteBuffers used to write outgoing Messages.
     */
    public TcpAcceptor.BufferPool getBufferPoolOut()
        {
        return __m_BufferPoolOut;
        }
    
    // Accessor for the property "ConnectionFlushQueue"
    /**
     * Getter for property ConnectionFlushQueue.<p>
    * A Queue of TcpConnection objects that have outgoing data ready to send.
     */
    public com.tangosol.coherence.component.util.Queue getConnectionFlushQueue()
        {
        return __m_ConnectionFlushQueue;
        }
    
    // Accessor for the property "ConnectionReleaseQueue"
    /**
     * Getter for property ConnectionReleaseQueue.<p>
    * A Queue of TcpConnection objects that have been closed.
     */
    public com.tangosol.coherence.component.util.Queue getConnectionReleaseQueue()
        {
        return __m_ConnectionReleaseQueue;
        }
    
    // Accessor for the property "DefaultLimitBytes"
    /**
     * Getter for property DefaultLimitBytes.<p>
    * The default size of the backlog (in bytes) at which point a connection
    * must be killed.
    * 
    * @since Coherence 3.4
    * @see $TcpConnection#checkSuspect
     */
    public long getDefaultLimitBytes()
        {
        return __m_DefaultLimitBytes;
        }
    
    // Accessor for the property "DefaultLimitLength"
    /**
     * Getter for property DefaultLimitLength.<p>
    * The default length of the backlog (in messages) at which point a
    * connection must be killed.
    * 
    * @since Coherence 3.4
    * @see $TcpConnection#checkSuspect
     */
    public int getDefaultLimitLength()
        {
        return __m_DefaultLimitLength;
        }
    
    // Accessor for the property "DefaultNominalBytes"
    /**
     * Getter for property DefaultNominalBytes.<p>
    * The default size of the backlog (in bytes) at which point a connection is
    * no longer considered suspect.
    * 
    * @since Coherence 3.4
    * @see $TcpConnection#checkSuspect
     */
    public long getDefaultNominalBytes()
        {
        return __m_DefaultNominalBytes;
        }
    
    // Accessor for the property "DefaultNominalLength"
    /**
     * Getter for property DefaultNominalLength.<p>
    * The default length of the backlog (in messages) at which point a
    * connection is no longer considered suspect.
    * 
    * @since Coherence 3.4
    * @see $TcpConnection#checkSuspect
     */
    public int getDefaultNominalLength()
        {
        return __m_DefaultNominalLength;
        }
    
    // Accessor for the property "DefaultSuspectBytes"
    /**
     * Getter for property DefaultSuspectBytes.<p>
    * The default size of the backlog (in bytes) at which point a connection
    * may become suspect.
    * 
    * @since Coherence 3.4
    * @see $TcpConnection#checkSuspect
     */
    public long getDefaultSuspectBytes()
        {
        return __m_DefaultSuspectBytes;
        }
    
    // Accessor for the property "DefaultSuspectLength"
    /**
     * Getter for property DefaultSuspectLength.<p>
    * The default length of the backlog (in messages) at which point a
    * connection may become suspect.
    * 
    * @since Coherence 3.4
    * @see $TcpConnection#checkSuspect
     */
    public int getDefaultSuspectLength()
        {
        return __m_DefaultSuspectLength;
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
        // import com.tangosol.util.Base;
        // import java.net.SocketOptions;
        
        StringBuffer sb = new StringBuffer(super.getDescription());
        
        sb.append(", SocketProvider=")
          .append(getSocketProvider());
        
        sb.append(", LocalAddress=")
          .append(getLocalAddressProvider());
        
        SocketOptions options = getSocketOptions();
        if (options != null)
            {
            sb.append(", ").append(options);
            }
        
        sb.append(", ListenBacklog=")
          .append(getListenBacklog())  
          .append(", BufferPoolIn=")
          .append(getBufferPoolIn())
          .append(", BufferPoolOut=")
          .append(getBufferPoolOut());
        
        return sb.toString();
        }
    
    // Accessor for the property "LastThrottleWarningTimestamp"
    /**
     * Getter for property LastThrottleWarningTimestamp.<p>
    * The timestamp of the last warning message indicating that a send() was
    * throttled due to outgoing BufferPool overflow.
     */
    public long getLastThrottleWarningTimestamp()
        {
        return __m_LastThrottleWarningTimestamp;
        }
    
    // Accessor for the property "ListenBacklog"
    /**
     * Getter for property ListenBacklog.<p>
    * The listen backlog of the ServerSocket created by this TcpAcceptor. If
    * equal or less than 0, then the default value will be used.
     */
    public int getListenBacklog()
        {
        return __m_ListenBacklog;
        }
    
    // Accessor for the property "LocalAddress"
    /**
     * Getter for property LocalAddress.<p>
    * The local SocketAddress that all Socket objects created by this
    * TcpAccepter will be bound to.
     */
    public java.net.SocketAddress getLocalAddress()
        {
        return __m_LocalAddress;
        }
    
    // Accessor for the property "LocalAddressProvider"
    /**
     * Getter for property LocalAddressProvider.<p>
    * The SocketAddressProvider used by the TcpAcceptor to obtain the address
    * to bind to.
     */
    public com.tangosol.net.SocketAddressProvider getLocalAddressProvider()
        {
        return __m_LocalAddressProvider;
        }
    
    // Accessor for the property "Processor"
    /**
     * Getter for property Processor.<p>
    * The TCP/IP I/O processor daemon.
     */
    public TcpAcceptor.TcpProcessor getProcessor()
        {
        TcpAcceptor.TcpProcessor processor = __m_Processor;
        if (processor == null)
            {
            setProcessor(processor = (TcpAcceptor.TcpProcessor) _findChild("TcpProcessor"));
            }
        return processor;
        }
    
    // Accessor for the property "SocketOptions"
    /**
     * Getter for property SocketOptions.<p>
    * The Acceptor's socket configuration.
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
    
    // Accessor for the property "StatsUnauthorizedConnectionAttempts"
    /**
     * Getter for property StatsUnauthorizedConnectionAttempts.<p>
    * The number of connection attempts from unauthorized hosts.
     */
    public long getStatsUnauthorizedConnectionAttempts()
        {
        return __m_StatsUnauthorizedConnectionAttempts;
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
    public com.tangosol.coherence.component.net.extend.Connection instantiateConnection()
        {
        TcpAcceptor.TcpConnection connection = (TcpAcceptor.TcpConnection) _newChild("TcpConnection");
        connection.setConnectionManager(this);
        
        return connection;
        }
    
    // Declared at the super level
    /**
     * Getter for property AcceptingConnections.<p>
    * If true, this Acceptor will accept new connections; otherwise, new
    * connection attempts will be denied.
    * 
    * @volatile
     */
    public boolean isAcceptingConnections()
        {
        return super.isAcceptingConnections() && getBufferPoolOut().getOverflow() <= 0;
        }
    
    /**
     * Helper method returning true iff the specified address "checks" againts
    * the authorized host list or filter.
    * 
    * @param addr  the InetAddress to check
     */
    public boolean isAuthorizedHost(java.net.InetAddress addr)
        {
        // import com.tangosol.util.Filter;
        
        Filter filterHost = getAuthorizedHostFilter();
        
        if (filterHost == null)
            {
            return true;
            }
        else
            {
            try
                {
                if (filterHost.evaluate(addr))
                    {
                    return true;
                    }
                }
            catch (Throwable e) {}
            }
        
        return false;
        }
    
    // Accessor for the property "SuspectProtocolEnabled"
    /**
     * Getter for property SuspectProtocolEnabled.<p>
    * True if connection suspect protocol is enabled.
    * 
    * @since Coherence 3.4
    * @see $TcpConnection#send
    * @see $TcpConnection#checkSuspect
     */
    public boolean isSuspectProtocolEnabled()
        {
        return __m_SuspectProtocolEnabled;
        }
    
    // Declared at the super level
    /**
     * Called after a Connection has closed. This method is called on the
    * service thread.
    * 
    * @param connection  the Connection that was closed
     */
    public void onConnectionClosed(com.tangosol.coherence.component.net.extend.Connection connection)
        {
        super.onConnectionClosed(connection);
        
        if (get_Connection() == connection)
            {
            return;
            }
        
        getConnectionReleaseQueue().add(connection);
        getProcessor().wakeup();
        }
    
    // Declared at the super level
    /**
     * Called after a Connection is closed due to an error or exception. This
    * method is called on the service thread.
    * 
    * @param connection  the Connection that was closed
    * @param e  the reason the Connection was closed
     */
    public void onConnectionError(com.tangosol.coherence.component.net.extend.Connection connection, Throwable e)
        {
        super.onConnectionError(connection, e);
        
        if (get_Connection() == connection)
            {
            return;
            }
        
        getConnectionReleaseQueue().add(connection);
        getProcessor().wakeup();
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
        // import com.tangosol.internal.net.service.peer.acceptor.TcpAcceptorDependencies;
        // import com.tangosol.net.SocketAddressProvider;
        // import com.tangosol.util.Filter;
        
        super.onDependencies(deps);
        
        TcpAcceptorDependencies tcpDeps = (TcpAcceptorDependencies) deps;
        
        ParameterizedBuilder bldrFilter = tcpDeps.getAuthorizedHostFilterBuilder();
        if (bldrFilter != null)
            {
            setAuthorizedHostFilter((Filter) bldrFilter.realize(
                    new NullParameterResolver(), getContextClassLoader(), null));
            }
        
        setDefaultLimitBytes(tcpDeps.getDefaultLimitBytes());
        setDefaultLimitLength(tcpDeps.getDefaultLimitMessages());
        setDefaultNominalBytes(tcpDeps.getDefaultNominalBytes());
        setDefaultNominalLength(tcpDeps.getDefaultNominalMessages());
        setDefaultSuspectBytes(tcpDeps.getDefaultSuspectBytes());
        setDefaultSuspectLength(tcpDeps.getDefaultSuspectMessages());
        setListenBacklog(tcpDeps.getListenBacklog());
        setSocketOptions(tcpDeps.getSocketOptions());
        setSocketProvider(tcpDeps.getSocketProviderBuilder().realize(null, null, null));
        setSuspectProtocolEnabled(tcpDeps.isSuspectProtocolEnabled());
        
        ParameterizedBuilder bldr = tcpDeps.getLocalAddressProviderBuilder();
        if (bldr != null)
            {
            setLocalAddressProvider((SocketAddressProvider) bldr.realize(
                    new NullParameterResolver(), getContextClassLoader(), null));
            }
        
        configureBufferPool(getBufferPoolIn(),  tcpDeps.getIncomingBufferPoolConfig());
        configureBufferPool(getBufferPoolOut(), tcpDeps.getOutgoingBufferPoolConfig());
        }
    
    // Declared at the super level
    /**
     * Event notification called right before the daemon thread terminates. This
    * method is guaranteed to be called only once and on the daemon's thread.
     */
    protected void onExit()
        {
        super.onExit();
        getProcessor().stop();
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
        // import Component.Util.Queue.ConcurrentQueue.DualQueue;
        // import com.tangosol.net.SocketOptions;
        // import com.tangosol.util.Base;
        // import java.net.SocketException;
        
        setBufferPoolIn((TcpAcceptor.BufferPool) _newChild("BufferPool"));
        setBufferPoolOut((TcpAcceptor.BufferPool) _newChild("BufferPool"));
        setConnectionFlushQueue(new DualQueue());
        setConnectionReleaseQueue(new DualQueue());
        
        getBufferPoolIn().setName("Incoming");
        getBufferPoolOut().setName("Outgoing");
        
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
     * The default implementation of this method does nothing.
     */
    protected void onServiceStarting()
        {
        super.onServiceStarting();
        
        TcpAcceptor.TcpProcessor processor = getProcessor();
        processor.start();
        
        _trace("TcpAcceptor now listening for connections on "
                + getSocketProvider().getAddressString(processor.getServerSocket()), 3);
        }
    
    // Declared at the super level
    /**
     * The default implementation of this method sets AcceptingClients to false.
     */
    protected void onServiceStopping()
        {
        // import Component.Net.Extend.Util.TcpUtil;
        
        // stop accepting new Socket connections before closing open Connections
        TcpUtil.cancel(getProcessor().getServerSocketKey());
        
        super.onServiceStopping();
        }
    
    // Declared at the super level
    /**
     * Release a ReadBuffer that contains an encode a Message. This method is
    * called on the service thread.
    * 
    * @param rb  the ReadBuffer to release
     */
    protected void releaseReadBuffer(com.tangosol.io.ReadBuffer rb)
        {
        _assert(rb instanceof TcpAcceptor.MessageBuffer);
        
        ((TcpAcceptor.MessageBuffer) rb).release();
        }
    
    // Declared at the super level
    /**
     * Release a WriteBuffer that was used to encode a Message. This method is
    * called on both client and service threads.
    * 
    * @param wb  the WriteBuffer to release
    * @param e  if not null, the WriteBuffer is being released due to a
    * decoding or send error
    * 
    * @see #allocateWriteBufer
     */
    protected void releaseWriteBuffer(com.tangosol.io.WriteBuffer wb, Throwable e)
        {
        // import com.tangosol.io.MultiBufferWriteBuffer as com.tangosol.io.MultiBufferWriteBuffer;
        
        _assert(wb instanceof com.tangosol.io.MultiBufferWriteBuffer);
        
        if (e == null)
            {
            // no-op: since TcpConnection.send() is an asynchronous operation (i.e.
            // outgoing Messages are queued), the WriteBuffer will be released when
            // the encoded Message is actually sent via the SocketChannel
            }
        else
            {
            TcpAcceptor.BufferPool      pool = getBufferPoolOut();
            com.tangosol.io.MultiBufferWriteBuffer mwb  = (com.tangosol.io.MultiBufferWriteBuffer) wb;
            for (int i = 0, c = mwb.getBufferCount(); i < c; ++i)
                {
                pool.release(mwb.getBuffer(i));
                }
            }
        }
    
    // Declared at the super level
    /**
     * Reset the Service statistics.
     */
    public void resetStats()
        {
        super.resetStats();
        setStatsUnauthorizedConnectionAttempts(0L);
        }
    
    // Accessor for the property "AuthorizedHostFilter"
    /**
     * Setter for property AuthorizedHostFilter.<p>
    * An optional com.tangosol.util.Filter that is used by the TcpAcceptor to
    * determine whether to accept a particular TcpInitiator. The evaluate()
    * method will be passed the java.net.InetAddress of the client.
    * Implementations
    * should return "true" to allow the client to connect.
     */
    protected void setAuthorizedHostFilter(com.tangosol.util.Filter filter)
        {
        __m_AuthorizedHostFilter = filter;
        }
    
    // Accessor for the property "BufferPoolIn"
    /**
     * Setter for property BufferPoolIn.<p>
    * A pool of WriteBuffers used to read incoming Messages.
     */
    protected void setBufferPoolIn(TcpAcceptor.BufferPool pool)
        {
        __m_BufferPoolIn = pool;
        }
    
    // Accessor for the property "BufferPoolOut"
    /**
     * Setter for property BufferPoolOut.<p>
    * A pool of WriteBuffers used to write outgoing Messages.
     */
    protected void setBufferPoolOut(TcpAcceptor.BufferPool pool)
        {
        __m_BufferPoolOut = pool;
        }
    
    // Accessor for the property "ConnectionFlushQueue"
    /**
     * Setter for property ConnectionFlushQueue.<p>
    * A Queue of TcpConnection objects that have outgoing data ready to send.
     */
    protected void setConnectionFlushQueue(com.tangosol.coherence.component.util.Queue queue)
        {
        __m_ConnectionFlushQueue = queue;
        }
    
    // Accessor for the property "ConnectionReleaseQueue"
    /**
     * Setter for property ConnectionReleaseQueue.<p>
    * A Queue of TcpConnection objects that have been closed.
     */
    protected void setConnectionReleaseQueue(com.tangosol.coherence.component.util.Queue queue)
        {
        __m_ConnectionReleaseQueue = queue;
        }
    
    // Accessor for the property "DefaultLimitBytes"
    /**
     * Setter for property DefaultLimitBytes.<p>
    * The default size of the backlog (in bytes) at which point a connection
    * must be killed.
    * 
    * @since Coherence 3.4
    * @see $TcpConnection#checkSuspect
     */
    protected void setDefaultLimitBytes(long cb)
        {
        __m_DefaultLimitBytes = cb;
        }
    
    // Accessor for the property "DefaultLimitLength"
    /**
     * Setter for property DefaultLimitLength.<p>
    * The default length of the backlog (in messages) at which point a
    * connection must be killed.
    * 
    * @since Coherence 3.4
    * @see $TcpConnection#checkSuspect
     */
    protected void setDefaultLimitLength(int cMsgs)
        {
        __m_DefaultLimitLength = cMsgs;
        }
    
    // Accessor for the property "DefaultNominalBytes"
    /**
     * Setter for property DefaultNominalBytes.<p>
    * The default size of the backlog (in bytes) at which point a connection is
    * no longer considered suspect.
    * 
    * @since Coherence 3.4
    * @see $TcpConnection#checkSuspect
     */
    protected void setDefaultNominalBytes(long cb)
        {
        __m_DefaultNominalBytes = cb;
        }
    
    // Accessor for the property "DefaultNominalLength"
    /**
     * Setter for property DefaultNominalLength.<p>
    * The default length of the backlog (in messages) at which point a
    * connection is no longer considered suspect.
    * 
    * @since Coherence 3.4
    * @see $TcpConnection#checkSuspect
     */
    protected void setDefaultNominalLength(int cMsgs)
        {
        __m_DefaultNominalLength = cMsgs;
        }
    
    // Accessor for the property "DefaultSuspectBytes"
    /**
     * Setter for property DefaultSuspectBytes.<p>
    * The default size of the backlog (in bytes) at which point a connection
    * may become suspect.
    * 
    * @since Coherence 3.4
    * @see $TcpConnection#checkSuspect
     */
    protected void setDefaultSuspectBytes(long cb)
        {
        __m_DefaultSuspectBytes = cb;
        }
    
    // Accessor for the property "DefaultSuspectLength"
    /**
     * Setter for property DefaultSuspectLength.<p>
    * The default length of the backlog (in messages) at which point a
    * connection may become suspect.
    * 
    * @since Coherence 3.4
    * @see $TcpConnection#checkSuspect
     */
    protected void setDefaultSuspectLength(int cMsgs)
        {
        __m_DefaultSuspectLength = cMsgs;
        }
    
    // Accessor for the property "LastThrottleWarningTimestamp"
    /**
     * Setter for property LastThrottleWarningTimestamp.<p>
    * The timestamp of the last warning message indicating that a send() was
    * throttled due to outgoing BufferPool overflow.
     */
    public void setLastThrottleWarningTimestamp(long ldt)
        {
        __m_LastThrottleWarningTimestamp = ldt;
        }
    
    // Accessor for the property "ListenBacklog"
    /**
     * Setter for property ListenBacklog.<p>
    * The listen backlog of the ServerSocket created by this TcpAcceptor. If
    * equal or less than 0, then the default value will be used.
     */
    protected void setListenBacklog(int cConn)
        {
        __m_ListenBacklog = cConn;
        }
    
    // Accessor for the property "LocalAddress"
    /**
     * Setter for property LocalAddress.<p>
    * The local SocketAddress that all Socket objects created by this
    * TcpAccepter will be bound to.
     */
    public void setLocalAddress(java.net.SocketAddress addr)
        {
        __m_LocalAddress = addr;
        }
    
    // Accessor for the property "LocalAddressProvider"
    /**
     * Setter for property LocalAddressProvider.<p>
    * The SocketAddressProvider used by the TcpAcceptor to obtain the address
    * to bind to.
     */
    public void setLocalAddressProvider(com.tangosol.net.SocketAddressProvider pLocalAddressProvider)
        {
        __m_LocalAddressProvider = pLocalAddressProvider;
        }
    
    // Accessor for the property "Processor"
    /**
     * Setter for property Processor.<p>
    * The TCP/IP I/O processor daemon.
     */
    protected void setProcessor(TcpAcceptor.TcpProcessor processor)
        {
        __m_Processor = processor;
        }
    
    // Accessor for the property "SocketOptions"
    /**
     * Setter for property SocketOptions.<p>
    * The Acceptor's socket configuration.
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
    protected void setSocketProvider(com.oracle.coherence.common.net.SocketProvider provider)
        {
        __m_SocketProvider = provider;
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
    
    // Accessor for the property "StatsUnauthorizedConnectionAttempts"
    /**
     * Setter for property StatsUnauthorizedConnectionAttempts.<p>
    * The number of connection attempts from unauthorized hosts.
     */
    public void setStatsUnauthorizedConnectionAttempts(long cMillis)
        {
        __m_StatsUnauthorizedConnectionAttempts = cMillis;
        }
    
    // Accessor for the property "SuspectProtocolEnabled"
    /**
     * Setter for property SuspectProtocolEnabled.<p>
    * True if connection suspect protocol is enabled.
    * 
    * @since Coherence 3.4
    * @see $TcpConnection#send
    * @see $TcpConnection#checkSuspect
     */
    public void setSuspectProtocolEnabled(boolean pSuspectProtocolEnabled)
        {
        __m_SuspectProtocolEnabled = pSuspectProtocolEnabled;
        }
    
    /**
     * Validate the given local InetAddress.
    * 
    * @param addr  the InetAddress to validate
     */
    protected static void validateLocalAddress(java.net.InetAddress addr)
        {
        // import com.tangosol.net.InetAddressHelper;
        // import java.net.InetAddress;
        // import java.util.Iterator;
        // import java.util.List;
        
        if (InetAddressHelper.isLoopbackAddress(addr))
            {
            _trace("The specified local address \"" + addr + "\" is a loopback address"
                    + "; clients running on remote machines will not be able to connect"
                    + " to this TcpAcceptor", 3);
            }
        else
            {
            // Linux allows you to bind to an IPv6 address that is not a local host;
            // issue a warning if this happens
            // (see http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4742177)
        
            byte[] abAddr = addr.getAddress();
            if (abAddr.length == 16)
                {
                boolean fSuspect  = true;
                List    listLocal = InetAddressHelper.getAllLocalAddresses();
                for (Iterator iter = listLocal.iterator(); iter.hasNext();)
                    {
                    InetAddress addrLocal = (InetAddress) iter.next();
                    if (InetAddressHelper.virtuallyEqual(addrLocal.getAddress(), abAddr))
                        {
                        fSuspect = false;
                        break;
                        }
                    }
        
                if (fSuspect)
                    {
                    _trace("The local IPv6 address \"" + InetAddressHelper.toString(addr)
                            + "\" does not correspond to any of the local interface addresses; "
                            + "this address may not be reachable by IPv4-bound nodes", 2);
                    }
                }
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.TcpAcceptor$BufferPool
    
    /**
     * A GrowablePool of ByteBufferWriteBuffer objects. The size and type of
     * pooled objects are controlled by the following properties:
     * 
     *   BufferSize
     *   BufferType
     * 
     * This component also implements the
     * com.tangosol.io.MultiBufferWriteBuffer$WriteBufferPool interface to
     * provide ByteBufferWriteBuffer objects to a MultiBufferWriteBuffer on
     * demand.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class BufferPool
            extends    com.tangosol.coherence.component.util.pool.simplePool.GrowablePool
            implements com.tangosol.io.MultiBufferWriteBuffer.WriteBufferPool
        {
        // ---- Fields declarations ----
        
        /**
         * Property BufferSize
         *
         * The size (in bytes) of newly allocated ByteBuffer objects.
         */
        private int __m_BufferSize;
        
        /**
         * Property BufferType
         *
         * The type of newly allocated ByteBuffer objects. Must be one of:
         * 
         * TYPE_DIRECT
         * TYPE_HEAP
         */
        private int __m_BufferType;
        
        /**
         * Property Name
         *
         * Optional name for the WriteBufferPool. This name is used when
         * logging information about the WriteBufferPool.
         */
        private String __m_Name;
        
        /**
         * Property Overflow
         *
         * The number of resources above the pool's capacity that have been
         * created.
         */
        private int __m_Overflow;
        
        /**
         * Property TYPE_DIRECT
         *
         * Constant representing a direct (off-heap) Java NIO ByteBuffer.
         */
        public static final int TYPE_DIRECT = 0;
        
        /**
         * Property TYPE_HEAP
         *
         * Constant representing a non-direct (on-heap) Java NIO ByteBuffer.
         */
        public static final int TYPE_HEAP = 1;
        
        // Default constructor
        public BufferPool()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public BufferPool(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                setBufferSize(2048);
                setBufferType(0);
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.TcpAcceptor.BufferPool();
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
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/peer/acceptor/TcpAcceptor$BufferPool".replace('/', '.'));
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
        
        // Declared at the super level
        /**
         * Borrow an element from the Pool.  If the pool contains no available
        * elements but is under capacity it will be grown, otherwise the call
        * will block until an element becomes available.
        * 
        * @return a pool element
         */
        public Object acquire()
            {
            // import com.tangosol.io.nio.ByteBufferWriteBuffer;
            
            ByteBufferWriteBuffer wb = (ByteBufferWriteBuffer) getStorage().removeNoWait();
            if (wb == null)
                {
                boolean fGrow;
                synchronized (this)
                    {
                    int cSize     = getSize();
                    int cCapacity = getCapacity();
                    if (cSize < cCapacity || cCapacity <= 0)
                        {
                        // grow the pool
                        setSize(cSize + 1);
                        fGrow = true;
                        }
                    else
                        {
                        // increment the overflow count
                        setOverflow(getOverflow() + 1);
                        fGrow = false;
                        }
                    }
            
                wb = (ByteBufferWriteBuffer) instantiateResource();
                if (_isTraceEnabled(9))
                    {    
                    if (fGrow)
                        {
                        _trace(getName() + "BufferPool increased to "
                                + (getSize() * getBufferSize())
                                + " bytes total", 9);
                        }
                    else
                        {
                        _trace(getName() + "BufferPool allocated "
                                + getBufferSize()
                                + " bytes", 9);
                        }
                    }
                }
            else
                {
                wb.getByteBuffer().clear();
                }
            
            return wb;
            }
        
        // From interface: com.tangosol.io.MultiBufferWriteBuffer$WriteBufferPool
        public com.tangosol.io.WriteBuffer allocate(int cbPreviousTotal)
            {
            // import com.tangosol.io.nio.ByteBufferWriteBuffer;
            
            return (ByteBufferWriteBuffer) acquire();
            }
        
        // Accessor for the property "BufferSize"
        /**
         * Getter for property BufferSize.<p>
        * The size (in bytes) of newly allocated ByteBuffer objects.
         */
        public int getBufferSize()
            {
            return __m_BufferSize;
            }
        
        // Accessor for the property "BufferType"
        /**
         * Getter for property BufferType.<p>
        * The type of newly allocated ByteBuffer objects. Must be one of:
        * 
        * TYPE_DIRECT
        * TYPE_HEAP
         */
        public int getBufferType()
            {
            return __m_BufferType;
            }
        
        // From interface: com.tangosol.io.MultiBufferWriteBuffer$WriteBufferPool
        public int getMaximumCapacity()
            {
            return getBufferSize() * getCapacity();
            }
        
        // Accessor for the property "Name"
        /**
         * Getter for property Name.<p>
        * Optional name for the WriteBufferPool. This name is used when logging
        * information about the WriteBufferPool.
         */
        public String getName()
            {
            String sName = __m_Name;
            return sName == null ? "" : sName;
            }
        
        // Accessor for the property "Overflow"
        /**
         * Getter for property Overflow.<p>
        * The number of resources above the pool's capacity that have been
        * created.
         */
        public int getOverflow()
            {
            return __m_Overflow;
            }
        
        /**
         * Return the type identifier for the ByteBuffer type described by the
        * given string.
        * 
        * @return the type identifier corresponding to the given string; one of
        * TYPE_DIRECT or TYPE_HEAP
         */
        public static int getType(String sType)
            {
            if (sType.equalsIgnoreCase("HEAP"))
                {
                return TYPE_HEAP;
                }
            return TYPE_DIRECT;
            }
        
        // Declared at the super level
        /**
         * Allocate a new element of the type held by this pool.  Derived
        * implementations must implement this method.
         */
        protected Object instantiateResource()
            {
            // import com.tangosol.io.nio.ByteBufferWriteBuffer;
            // import java.nio.ByteBuffer;
            
            ByteBuffer buf;
            switch (getBufferType())
                {
                case TYPE_DIRECT:
                  buf = ByteBuffer.allocateDirect(getBufferSize());
                  break;
                  
                case TYPE_HEAP:
                default:
                  buf = ByteBuffer.allocate(getBufferSize());
                }
            
            return new ByteBufferWriteBuffer(buf);
            }
        
        // Declared at the super level
        /**
         * Instantiate the storage which will back this SimplePool.
         */
        protected com.tangosol.coherence.component.util.Queue instantiateStorage()
            {
            // import Component.Util.Queue.ConcurrentQueue.DualQueue;
            
            return new DualQueue();
            }
        
        // Accessor for the property "CapacityLimited"
        /**
         * Getter for property CapacityLimited.<p>
        * True if the capacity of the buffer pool is limited.
        * 
        * @since Coherence 3.4
         */
        public boolean isCapacityLimited()
            {
            return getCapacity() > 0;
            }
        
        /**
         * Release all pooled ByteBufferWriteBuffer objects.
         */
        public synchronized void release()
            {
            // import Component.Util.Queue as com.tangosol.coherence.component.util.Queue;
            
            for (com.tangosol.coherence.component.util.Queue queue = getStorage(); !queue.isEmpty(); queue.removeNoWait());
            setOverflow(0);
            setSize(0);
            }
        
        // From interface: com.tangosol.io.MultiBufferWriteBuffer$WriteBufferPool
        public void release(com.tangosol.io.WriteBuffer wb)
            {
            release((Object) wb);
            }
        
        // Declared at the super level
        /**
         * Return an element to the pool.  The Pool implementation is free to
        * keep or discard this element.
        * 
        * @param oElement the element to return to the pool
         */
        public void release(Object oElement)
            {
            // import com.tangosol.io.nio.ByteBufferWriteBuffer;
            
            _assert(oElement instanceof ByteBufferWriteBuffer);
            
            // determine if the ByteBufferWriteBuffer should be released or GC'ed
            int     cOverflow;
            boolean fRelease;
            synchronized (this)
                {
                cOverflow = getOverflow();
                if (cOverflow == 0)
                    {
                    fRelease = true;
                    }
                else
                    {
                    setOverflow(--cOverflow);
                    fRelease = false;
                    }
                }
            
            if (fRelease)
                {
                super.release(oElement);
                }
            else
                {
                if (getBufferType() == TYPE_DIRECT && cOverflow == 0)
                    {
                    // see http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4857305
                    System.gc();
                    }
            
                if (_isTraceEnabled(9))
                    {
                    _trace(getName() + "BufferPool released "
                            + ((ByteBufferWriteBuffer) oElement).getCapacity()
                            + " bytes", 9);
                    }
                }
            }
        
        // Accessor for the property "BufferSize"
        /**
         * Setter for property BufferSize.<p>
        * The size (in bytes) of newly allocated ByteBuffer objects.
         */
        public void setBufferSize(int cb)
            {
            _assert(cb > 0);
            __m_BufferSize = (cb);
            }
        
        // Accessor for the property "BufferType"
        /**
         * Setter for property BufferType.<p>
        * The type of newly allocated ByteBuffer objects. Must be one of:
        * 
        * TYPE_DIRECT
        * TYPE_HEAP
         */
        public void setBufferType(int nType)
            {
            _assert(nType == TYPE_DIRECT || nType == TYPE_HEAP);
            __m_BufferType = (nType);
            }
        
        // Accessor for the property "Name"
        /**
         * Setter for property Name.<p>
        * Optional name for the WriteBufferPool. This name is used when logging
        * information about the WriteBufferPool.
         */
        public void setName(String sName)
            {
            if (sName != null && sName.length() > 0 && !sName.endsWith(" "))
                {
                sName += ' ';
                }
            __m_Name = (sName);
            }
        
        // Accessor for the property "Overflow"
        /**
         * Setter for property Overflow.<p>
        * The number of resources above the pool's capacity that have been
        * created.
         */
        public void setOverflow(int cElements)
            {
            __m_Overflow = cElements;
            }
        
        // Declared at the super level
        public String toString()
            {
            // import com.tangosol.util.Base;
            
            String sCapacity;
            if (getMaximumCapacity() == 0)
                {
                sCapacity="Unlimited";
                }
            else
                {
                sCapacity=Base.toMemorySizeString(getMaximumCapacity());
                }
            
            
            return get_Name()
                    + '('
                    + "BufferSize="   + Base.toMemorySizeString(getBufferSize())
                    + ", BufferType=" + (getBufferType() == TYPE_DIRECT ? "DIRECT" : "HEAP")
                    + ", Capacity="   + sCapacity
                    + ')';
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.TcpAcceptor$MessageBuffer
    
    /**
     * ReadBuffer implementation that holds an encoded Message. This component
     * delegates all ReadBuffer operations to a wrapped ReadBuffer and holds a
     * reference to a BufferPool and an array of WriteBuffer objects that the
     * wrapped ReadBuffer is based upon so that they can be released.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class MessageBuffer
            extends    com.tangosol.coherence.component.net.extend.Util
            implements com.tangosol.io.ReadBuffer
        {
        // ---- Fields declarations ----
        
        /**
         * Property _ReadBuffer
         *
         * The delegate ReadBuffer.
         */
        private com.tangosol.io.ReadBuffer __m__ReadBuffer;
        
        /**
         * Property _WriteBuffer
         *
         * The array of WriteBuffer objects that contain the encoded Message
         * data. These objects must be released to the BufferPool after the
         * Message has been processed.
         */
        private com.tangosol.io.WriteBuffer[] __m__WriteBuffer;
        
        /**
         * Property BufferPool
         *
         * The BufferPool that the Message's WriteBuffer objects were acquried
         * from.
         */
        private TcpAcceptor.BufferPool __m_BufferPool;
        
        // Default constructor
        public MessageBuffer()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public MessageBuffer(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.TcpAcceptor.MessageBuffer();
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
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/peer/acceptor/TcpAcceptor$MessageBuffer".replace('/', '.'));
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
        
        // From interface: com.tangosol.io.ReadBuffer
        public byte byteAt(int of)
            {
            return get_ReadBuffer().byteAt(of);
            }
        
        // From interface: com.tangosol.io.ReadBuffer
        // Declared at the super level
        public Object clone()
            {
            try
                {
                return super.clone();
                }
            catch (java.lang.CloneNotSupportedException e)
                {
                // re-throw as a runtime exception
                throw new com.tangosol.util.WrapperException(e);
                }
            }
        
        // From interface: com.tangosol.io.ReadBuffer
        public void copyBytes(int ofBegin, int ofEnd, byte[] abDest, int ofDest)
            {
            get_ReadBuffer().copyBytes(ofBegin, ofEnd, abDest, ofDest);
            }
        
        // Accessor for the property "_ReadBuffer"
        /**
         * Getter for property _ReadBuffer.<p>
        * The delegate ReadBuffer.
         */
        public com.tangosol.io.ReadBuffer get_ReadBuffer()
            {
            return __m__ReadBuffer;
            }
        
        // Accessor for the property "_WriteBuffer"
        /**
         * Getter for property _WriteBuffer.<p>
        * The array of WriteBuffer objects that contain the encoded Message
        * data. These objects must be released to the BufferPool after the
        * Message has been processed.
         */
        public com.tangosol.io.WriteBuffer[] get_WriteBuffer()
            {
            return __m__WriteBuffer;
            }
        
        // Accessor for the property "_WriteBuffer"
        /**
         * Getter for property _WriteBuffer.<p>
        * The array of WriteBuffer objects that contain the encoded Message
        * data. These objects must be released to the BufferPool after the
        * Message has been processed.
         */
        protected com.tangosol.io.WriteBuffer get_WriteBuffer(int i)
            {
            return get_WriteBuffer()[i];
            }
        
        // From interface: com.tangosol.io.ReadBuffer
        public com.tangosol.io.ReadBuffer.BufferInput getBufferInput()
            {
            return get_ReadBuffer().getBufferInput();
            }
        
        // Accessor for the property "BufferPool"
        /**
         * Getter for property BufferPool.<p>
        * The BufferPool that the Message's WriteBuffer objects were acquried
        * from.
         */
        public TcpAcceptor.BufferPool getBufferPool()
            {
            return __m_BufferPool;
            }
        
        // From interface: com.tangosol.io.ReadBuffer
        public com.tangosol.io.ReadBuffer getReadBuffer(int of, int cb)
            {
            return get_ReadBuffer().getReadBuffer(of, cb);
            }
        
        // From interface: com.tangosol.io.ReadBuffer
        public int length()
            {
            return get_ReadBuffer().length();
            }
        
        // Declared at the super level
        /**
         * The "component has been initialized" method-notification called out
        * of setConstructed() for the topmost component and that in turn
        * notifies all the children.
        * 
        * This notification gets called before the control returns back to this
        * component instantiator (using <code>new Component.X()</code> or
        * <code>_newInstance(sName)</code>) and on the same thread. In
        * addition, visual components have a "posted" notification
        * <code>onInitUI</code> that is called after (or at the same time as)
        * the control returns back to the instantiator and possibly on a
        * different thread.
         */
        public void onInit()
            {
            // no-op: no children
            }
        
        /**
         * Release the array of WriteBuffer objects that contain the encoded
        * Message data back to the BufferPool that they were acquired from.
         */
        public void release()
            {
            // import com.tangosol.io.WriteBuffer;
            
            TcpAcceptor.BufferPool   pool = getBufferPool();
            WriteBuffer[] awb  = get_WriteBuffer();
            
            for (int i = 0, c = awb.length; i < c; ++i)
                {
                WriteBuffer wb = awb[i];
                if (wb != null)
                    {
                    pool.release(wb);
                    }
                }
            }
        
        // Accessor for the property "_ReadBuffer"
        /**
         * Setter for property _ReadBuffer.<p>
        * The delegate ReadBuffer.
         */
        public void set_ReadBuffer(com.tangosol.io.ReadBuffer rb)
            {
            __m__ReadBuffer = rb;
            }
        
        // Accessor for the property "_WriteBuffer"
        /**
         * Setter for property _WriteBuffer.<p>
        * The array of WriteBuffer objects that contain the encoded Message
        * data. These objects must be released to the BufferPool after the
        * Message has been processed.
         */
        public void set_WriteBuffer(com.tangosol.io.WriteBuffer[] awb)
            {
            __m__WriteBuffer = awb;
            }
        
        // Accessor for the property "_WriteBuffer"
        /**
         * Setter for property _WriteBuffer.<p>
        * The array of WriteBuffer objects that contain the encoded Message
        * data. These objects must be released to the BufferPool after the
        * Message has been processed.
         */
        protected void set_WriteBuffer(int i, com.tangosol.io.WriteBuffer wb)
            {
            get_WriteBuffer()[i] = wb;
            }
        
        // Accessor for the property "BufferPool"
        /**
         * Setter for property BufferPool.<p>
        * The BufferPool that the Message's WriteBuffer objects were acquried
        * from.
         */
        public void setBufferPool(TcpAcceptor.BufferPool pool)
            {
            __m_BufferPool = pool;
            }
        
        // From interface: com.tangosol.io.ReadBuffer
        public com.tangosol.util.ByteSequence subSequence(int ofStart, int ofEnd)
            {
            return get_ReadBuffer().subSequence(ofStart, ofEnd);
            }
        
        // From interface: com.tangosol.io.ReadBuffer
        public com.tangosol.util.Binary toBinary()
            {
            return get_ReadBuffer().toBinary();
            }
        
        // From interface: com.tangosol.io.ReadBuffer
        public com.tangosol.util.Binary toBinary(int of, int cb)
            {
            return get_ReadBuffer().toBinary(of, cb);
            }
        
        // From interface: com.tangosol.io.ReadBuffer
        public byte[] toByteArray()
            {
            return get_ReadBuffer().toByteArray();
            }
        
        // From interface: com.tangosol.io.ReadBuffer
        public byte[] toByteArray(int of, int cb)
            {
            return get_ReadBuffer().toByteArray(of, cb);
            }
        
        // From interface: com.tangosol.io.ReadBuffer
        public java.nio.ByteBuffer toByteBuffer()
            {
            return null;
            }
        
        // From interface: com.tangosol.io.ReadBuffer
        public java.nio.ByteBuffer toByteBuffer(int of, int cb)
            {
            return null;
            }
        
        // From interface: com.tangosol.io.ReadBuffer
        public void writeTo(java.io.DataOutput out)
                throws java.io.IOException
            {
            get_ReadBuffer().writeTo(out);
            }
        
        // From interface: com.tangosol.io.ReadBuffer
        public void writeTo(java.io.DataOutput out, int of, int cb)
                throws java.io.IOException
            {
            get_ReadBuffer().writeTo(out, of, cb);
            }
        
        // From interface: com.tangosol.io.ReadBuffer
        public void writeTo(java.io.OutputStream out)
                throws java.io.IOException
            {
            get_ReadBuffer().writeTo(out);
            }
        
        // From interface: com.tangosol.io.ReadBuffer
        public void writeTo(java.io.OutputStream out, int of, int cb)
                throws java.io.IOException
            {
            get_ReadBuffer().writeTo(out, of, cb);
            }
        
        // From interface: com.tangosol.io.ReadBuffer
        public void writeTo(java.nio.ByteBuffer buf)
            {
            get_ReadBuffer().writeTo(buf);
            }
        
        // From interface: com.tangosol.io.ReadBuffer
        public void writeTo(java.nio.ByteBuffer buf, int of, int cb)
                throws java.io.IOException
            {
            get_ReadBuffer().writeTo(buf, of, cb);
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.TcpAcceptor$MessageFactory
    
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
            extends    com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.Acceptor.MessageFactory
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
            __mapChildren.put("OpenConnection", com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.Acceptor.MessageFactory.OpenConnection.get_CLASS());
            __mapChildren.put("OpenConnectionRequest", TcpAcceptor.MessageFactory.OpenConnectionRequest.get_CLASS());
            __mapChildren.put("OpenConnectionResponse", TcpAcceptor.MessageFactory.OpenConnectionResponse.get_CLASS());
            __mapChildren.put("PingRequest", TcpAcceptor.MessageFactory.PingRequest.get_CLASS());
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.TcpAcceptor.MessageFactory();
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
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/peer/acceptor/TcpAcceptor$MessageFactory".replace('/', '.'));
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

        // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.TcpAcceptor$MessageFactory$OpenConnectionRequest
        
        /**
         * This Request is used to open a new Channel.
         */
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class OpenConnectionRequest
                extends    com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.Acceptor.MessageFactory.OpenConnectionRequest
            {
            // ---- Fields declarations ----
            
            /**
             * Property Redirect
             *
             * True if the TcpConnection is being opened in response to a
             * redirection.
             */
            private boolean __m_Redirect;
            
            /**
             * Property RedirectSupported
             *
             * True if the peer TcpInitiator supports redirection.
             */
            private boolean __m_RedirectSupported;
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
                return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.TcpAcceptor.MessageFactory.OpenConnectionRequest();
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
                    clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/peer/acceptor/TcpAcceptor$MessageFactory$OpenConnectionRequest".replace('/', '.'));
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
            * True if the peer TcpInitiator supports redirection.
             */
            public boolean isRedirectSupported()
                {
                return __m_RedirectSupported;
                }
            
            // Declared at the super level
            /**
             * Called when the Request is run.
            * 
            * @param response  the Response that should be populated with the
            * result of running the Request
             */
            protected void onRun(com.tangosol.coherence.component.net.extend.message.Response response)
                {
                // import com.tangosol.net.messaging.Channel;
                
                Channel channel0 = getChannel();
                _assert(channel0.getId() == 0);
                
                TcpAcceptor.TcpConnection connection = (TcpAcceptor.TcpConnection) channel0.getConnection();
                _assert(connection != null);
                
                // update the connection with redirection information
                connection.setRedirect(isRedirect());
                connection.setRedirectSupported(isRedirectSupported());
                
                connection.setClusterName(getClusterName());
                connection.setServiceName(getServiceName());
                
                super.onRun(response);
                
                // update the response with redirection information
                TcpAcceptor.MessageFactory.OpenConnectionResponse responseImpl =
                        (TcpAcceptor.MessageFactory.OpenConnectionResponse) response;
                
                responseImpl.setRedirect(connection.isRedirect());
                responseImpl.setRedirectList(connection.getRedirectList());
                }
            
            // Declared at the super level
            public void readExternal(com.tangosol.io.pof.PofReader in)
                    throws java.io.IOException
                {
                super.readExternal(in);
                
                setRedirectSupported(in.readBoolean(10));
                setRedirect(in.readBoolean(11));
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
            
            // Accessor for the property "RedirectSupported"
            /**
             * Setter for property RedirectSupported.<p>
            * True if the peer TcpInitiator supports redirection.
             */
            public void setRedirectSupported(boolean f)
                {
                __m_RedirectSupported = f;
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

        // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.TcpAcceptor$MessageFactory$OpenConnectionResponse
        
        /**
         * Response to an OpenChannelRequest.
         */
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class OpenConnectionResponse
                extends    com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.Acceptor.MessageFactory.OpenConnectionResponse
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
                return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.TcpAcceptor.MessageFactory.OpenConnectionResponse();
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
                    clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/peer/acceptor/TcpAcceptor$MessageFactory$OpenConnectionResponse".replace('/', '.'));
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

        // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.TcpAcceptor$MessageFactory$PingRequest
        
        /**
         * This Request is used to test the integrity of a Connection.
         */
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class PingRequest
                extends    com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.PingRequest
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
                __mapChildren.put("Status", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.PingRequest.Status.get_CLASS());
                }
            
            // Default constructor
            public PingRequest()
                {
                this(null, null, true);
                }
            
            // Initializing constructor
            public PingRequest(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.TcpAcceptor.MessageFactory.PingRequest();
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
                    clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/peer/acceptor/TcpAcceptor$MessageFactory$PingRequest".replace('/', '.'));
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
             * Called when the Request is run. If the request ID is 1, and the
            * parent service is a ProxyService, the Response result will be the
            * collection returned by ProxyService.getMemberListenAddresses().
            * 
            * @param response  the Response that should be populated with the
            * result of running the Request
             */
            protected void onRun(com.tangosol.coherence.component.net.extend.message.Response response)
                {
                // import com.tangosol.net.Service;
                // import Component.Net.Extend.Connection.TcpConnection as com.tangosol.coherence.component.net.extend.connection.TcpConnection;
                // import Component.Util.Daemon.QueueProcessor.Service.Grid.ProxyService;
                // import Component.Util.Daemon.QueueProcessor.Service.Peer.Acceptor.TcpAcceptor as TcpAcceptor;
                
                super.onRun(response);
                
                if (getId() == 1)
                    {
                    com.tangosol.coherence.component.net.extend.connection.TcpConnection conn     = (com.tangosol.coherence.component.net.extend.connection.TcpConnection) getChannel().getConnection();
                    Service       service  = ((TcpAcceptor) conn.getConnectionManager()).getParentService();
                    if (service instanceof ProxyService)
                        {   
                        response.setResultAsCollection(((ProxyService) service).getRoutableMemberAddresses(conn.getSocket()));
                        }
                    }
                }
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.TcpAcceptor$TcpConnection
    
    /**
     * Connection implementation that wraps a non-blocking TCP/IP Socket.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class TcpConnection
            extends    com.tangosol.coherence.component.net.extend.connection.TcpConnection
        {
        // ---- Fields declarations ----
        
        /**
         * Property ClusterName
         *
         * The name of the cluster the peer wishes to connect to.
         * 
         * @since 12.2.1
         */
        private String __m_ClusterName;
        
        /**
         * Property ConnectTimeMillis
         *
         * The date/time value when the client connected.
         */
        private transient long __m_ConnectTimeMillis;
        
        /**
         * Property IN_DECODED
         *
         * The length of the next Message has been read and the Message data is
         * ready to be read.
         */
        public static final int IN_DECODED = 1;
        
        /**
         * Property IN_INITIAL
         *
         * The Connection is ready for a new Message to be read.
         */
        public static final int IN_INITIAL = 0;
        
        /**
         * Property IN_PREPARED
         *
         * The length of the next Message has been decoded and the Message data
         * is ready to be read.
         */
        public static final int IN_PREPARED = 2;
        
        /**
         * Property IncomingByteBuffer
         *
         * A ByteBuffer used to read the length of the next incoming Message.
         */
        private java.nio.ByteBuffer __m_IncomingByteBuffer;
        
        /**
         * Property IncomingBytesRead
         *
         * The number of bytes of the current Message being read that have been
         * read from the Socket associated with this TcpConnection.
         */
        private int __m_IncomingBytesRead;
        
        /**
         * Property IncomingBytesTotal
         *
         * The length (in bytes) of the current Message being read from the
         * Socket associated with this TcpConnection.
         */
        private int __m_IncomingBytesTotal;
        
        /**
         * Property IncomingDisabled
         *
         */
        private boolean __m_IncomingDisabled;
        
        /**
         * Property IncomingState
         *
         * The current state of the next incoming Message. The value of this
         * property may be one of:
         *   IN_INITIAL
         *   IN_DECODED
         *   IN_PREPARED
         */
        private int __m_IncomingState;
        
        /**
         * Property IncomingWriteBufferArray
         *
         * An array of ByteBufferWriteBuffer objects used to hold the content
         * of the Message that is currently being read from the Socket
         * associated with this TcpConnection.
         */
        private com.tangosol.io.nio.ByteBufferWriteBuffer[] __m_IncomingWriteBufferArray;
        
        /**
         * Property IncomingWriteBufferIndex
         *
         * The index of the IncomingWriteBufferArray that contains the
         * ByteBufferWriteBuffer that is being used to hold the portion of the
         * Message that is currently being read from the Socket associated with
         * this TcpConnection.
         */
        private int __m_IncomingWriteBufferIndex;
        
        /**
         * Property OUT_ENCODED
         *
         * The length of the next Message has been writen and the Message data
         * is ready to be sent.
         */
        public static final int OUT_ENCODED = 2;
        
        /**
         * Property OUT_INITIAL
         *
         * The Connection is ready for a new Message to be sent.
         */
        public static final int OUT_INITIAL = 0;
        
        /**
         * Property OUT_PREPARED
         *
         * The next Message is prepared and is ready to be length encoded and
         * sent.
         */
        public static final int OUT_PREPARED = 1;
        
        /**
         * Property OutgoingByteBuffer
         *
         * A ByteBuffer used to write the length of the next outgoing Message.
         */
        private java.nio.ByteBuffer __m_OutgoingByteBuffer;
        
        /**
         * Property OutgoingBytesTotal
         *
         * The length (in bytes) of the current Message being written to the
         * Socket associated with this TcpConnection.
         */
        private int __m_OutgoingBytesTotal;
        
        /**
         * Property OutgoingBytesWritten
         *
         * The number of bytes of the current Message being written that have
         * been written to the Socket associated with this TcpConnection.
         */
        private int __m_OutgoingBytesWritten;
        
        /**
         * Property OutgoingDisabled
         *
         */
        private boolean __m_OutgoingDisabled;
        
        /**
         * Property OutgoingMessage
         *
         * A MultiBufferWriteBuffer containing the contents of the current
         * outgoing Message or null if no Message is currently being sent.
         */
        private com.tangosol.io.MultiBufferWriteBuffer __m_OutgoingMessage;
        
        /**
         * Property OutgoingQueue
         *
         * A Queue of encoded Message objects (stored as a
         * MultiBufferWriteBuffer of one or more ByteArrayWriteBuffer objects)
         * that need to be written to the Socket associated with this
         * TcpConnection.
         */
        private com.tangosol.coherence.component.util.Queue __m_OutgoingQueue;
        
        /**
         * Property OutgoingState
         *
         * The current state of the next outgoing Message. The value of this
         * property may be one of:
         *   OUT_INITIAL
         *   OUT_PREPARED
         *   OUT_ENCODED
         */
        private int __m_OutgoingState;
        
        /**
         * Property OutgoingWriteBufferArray
         *
         * An array of ByteBufferWriteBuffer objects used to hold the content
         * of the Message that is currently being written to the Socket
         * associated with this TcpConnection.
         */
        private com.tangosol.io.nio.ByteBufferWriteBuffer[] __m_OutgoingWriteBufferArray;
        
        /**
         * Property OutgoingWriteBufferIndex
         *
         * The index of the OutgoingWriteBufferArray that contains the
         * ByteBufferWriteBuffer that holds the portion of the Message that is
         * currently being written to the Socket associated with this
         * TcpConnection.
         */
        private int __m_OutgoingWriteBufferIndex;
        
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
         * Property RedirectSupported
         *
         * True if the TcpConnection supports redirection.
         */
        private boolean __m_RedirectSupported;
        
        /**
         * Property SelectionKey
         *
         * The SelectionKey representing the registration of this
         * TcpConnection's SocketChannel with its TcpAcceptor's Selector.
         */
        private java.nio.channels.SelectionKey __m_SelectionKey;
        
        /**
         * Property ServiceName
         *
         * The name of the service the peer wishes to connect to.
         * 
         * @since 12.2.1
         */
        private String __m_ServiceName;
        
        /**
         * Property SocketChannel
         *
         * The SocketChannel used to send and receive data via the Socket
         * associated with this TcpConnection.
         */
        private java.nio.channels.SocketChannel __m_SocketChannel;
        
        /**
         * Property StatsBytesQueued
         *
         * Statistics: total number of bytes submitted to be sent (i.e. queued)
         * over this Connection. This value can be compared against the
         * StatsBytesSent to determine how many bytes have been queued but not
         * actually "sent" (where "sent" doesn't mean received by another
         * process or even sent on the wire, but rather copied to the OS's
         * TCP/IP buffers from which the information is actually sent).
         * 
         * @since Coherence 3.4
         */
        private transient long __m_StatsBytesQueued;
        
        /**
         * Property StatsQueued
         *
         * Statistics: total number of messages submitted to be sent (i.e.
         * queued)  over this Connection. This value can be compared against
         * the StatsSent to determine how many messages have been queued but
         * not actually "sent" (where "sent" doesn't mean received by another
         * process or even sent on the wire, but rather copied to the OS's
         * TCP/IP buffers from which the information is actually sent). The
         * difference should be the same (give or take due to multi-threading)
         * as the outgoing queue length.
         * 
         * @since Coherence 3.4
         */
        private transient long __m_StatsQueued;
        
        /**
         * Property Suspect
         *
         * True if this connection has been determined to be a "suspect", i.e.
         * a resource-hogging / runaway train that will ultimately crash the
         * server by sucking up too much memory.
         * 
         * @since Coherence 3.4
         */
        private boolean __m_Suspect;
        
        /**
         * Property SuspectBytesWorseCount
         *
         * Number of checks that showed the suspect getting worse in terms of
         * the number of bytes in the backlog. (Only relevant if
         * Suspect==true.)
         * 
         * @since Coherence 3.4
         */
        private int __m_SuspectBytesWorseCount;
        
        /**
         * Property SuspectCheckCount
         *
         * Number of times that the suspect has been checked after being
         * declared a suspect. (Only relevant if Suspect==true.)
         * 
         * @since Coherence 3.4
         */
        private int __m_SuspectCheckCount;
        
        /**
         * Property SuspectInitialBytes
         *
         * The number of bytes queued when the initial check was done that
         * determined that this connection is suspect. (Only relevant if
         * Suspect==true.)
         */
        private long __m_SuspectInitialBytes;
        
        /**
         * Property SuspectInitialLength
         *
         * The length of the queue when the initial check was done that
         * determined that this connection is suspect. (Only relevant if
         * Suspect==true.)
         */
        private long __m_SuspectInitialLength;
        
        /**
         * Property SuspectInitialMillis
         *
         * The time at which the connection was determined to be a suspect.
         * (Only relevant if Suspect==true.)
         */
        private long __m_SuspectInitialMillis;
        
        /**
         * Property SuspectLatestBytes
         *
         * The number of bytes queued when the last check was done. (Only
         * relevant if Suspect==true.)
         */
        private long __m_SuspectLatestBytes;
        
        /**
         * Property SuspectLatestLength
         *
         * The length of the queue when the last check was done. (Only
         * relevant if Suspect==true.)
         */
        private long __m_SuspectLatestLength;
        
        /**
         * Property SuspectLatestMillis
         *
         * The time at which the connection suspect status was last evaluated.
         */
        private long __m_SuspectLatestMillis;
        
        /**
         * Property SuspectLengthWorseCount
         *
         * Number of checks that showed the suspect queue length getting worse.
         * (Only relevant if Suspect==true.)
         */
        private int __m_SuspectLengthWorseCount;
        
        /**
         * Property SuspectTargetBytes
         *
         * The target number of bytes queued to get below for the connection to
         * lose its "suspect" status. (Only relevant if Suspect==true.)
         */
        private long __m_SuspectTargetBytes;
        
        /**
         * Property SuspectTargetLength
         *
         * The length of the queue to get below for the connection to lose its
         * "suspect" status. (Only relevant if Suspect==true.)
         */
        private long __m_SuspectTargetLength;
        
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.TcpAcceptor.TcpConnection();
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
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/peer/acceptor/TcpAcceptor$TcpConnection".replace('/', '.'));
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
        
        // Declared at the super level
        /**
         * @throws ConnectionException if the Connection is closed or closing
         */
        public void assertOpen()
            {
            super.assertOpen();
            }
        
        /**
         * Diagnosing and potentially kill "runaway" connections, i.e.
        * connections that get further and further behind, eventually depleting
        * the resources of this server and causing it to crash.
         */
        protected void checkSuspect(boolean fSuspect, long cQueued, long cbQueued, long cSent, long cbSent)
            {
            // import Component.Util.Queue as com.tangosol.coherence.component.util.Queue;
            // import com.tangosol.net.messaging.SuspectConnectionException;
            // import com.tangosol.util.Base;
            // import java.util.Iterator;
            // import java.util.ConcurrentModificationException;
            
            TcpAcceptor  acceptor = (TcpAcceptor) getConnectionManager();
            TcpAcceptor.BufferPool  pool     = acceptor.getBufferPoolOut();
            com.tangosol.coherence.component.util.Queue        queue    = getOutgoingQueue();
            
            long cBehind  = cQueued - cSent;
            long cbBehind = cbQueued - cbSent;
            if (cQueued < 0L || cSent < 0L
                    || cBehind < 0L || cBehind > Integer.MAX_VALUE
                    || cbQueued < 0L || cbSent < 0L
                    || cbBehind < 0L || cbBehind > Integer.MAX_VALUE)
                {
                // ignore the stats for this iteration since they're being
                // updated by multiple threads and we might have picked
                // up a bad stat; note that this means that we ignore
                // queues longer than 2GB, which were not feasible in 2008
                // but may be common-place by the time you are reading this
                // comment
                return;
                }
            
            if (fSuspect)
                {
                synchronized (queue)
                    {
                    if (isSuspect() && isSuspectEvaluate())
                        {
                        final long LIMIT_BYTES  = acceptor.getDefaultLimitBytes();
                        final long LIMIT_LENGTH = acceptor.getDefaultLimitLength();
            
                        // the connection must do one of three things:
                        // 1) prove that it is no longer suspect
                        // 2) prove that it is so bad that it must be killed
                        // 3) failing either of the above, it just keeps track
                        //    of its progress while it remains suspect
                        boolean fKill    = false;
                        boolean fClear   = false;
                        String  sCause   = null;
                        long    cTarget  = getSuspectTargetLength();
                        long    cbTarget = getSuspectTargetBytes();
                        int     cMax     = pool.getCapacity();
                        if (pool.isCapacityLimited() &&
                                (cBehind > cMax || cbBehind > cMax * pool.getBufferSize()))
                            {
                            // this one connection has used up the entire capacity of
                            // the pool; kill it
                            fKill  = true;
                            sCause = "This one connection has used up the entire"
                                    + " configured capacity of the buffer pool;"
                                    + " the connection is " + cBehind
                                    + " messages behind (" + cbBehind
                                    + " bytes); the buffer pool is configured for "
                                    + cMax + " messages (" + (cMax * pool.getBufferSize())
                                    + " bytes).";
                            }
                        else if (!pool.isCapacityLimited()
                                && (cBehind > LIMIT_LENGTH || cbBehind > LIMIT_BYTES))
                            {
                            fKill  = true;
                            sCause = "This connection is " + cBehind
                                    + " messages behind (" + cbBehind
                                    + " bytes); the limit is " + LIMIT_LENGTH
                                    + " messages (" + LIMIT_BYTES + " bytes).";
                            }
                        else if (cbBehind < cbTarget)
                            {
                            fClear = true;
                            sCause = "The connection has reduced its backlog to "
                                    + cbBehind + " bytes; the target was "
                                    + cbTarget + " bytes.";
                            }
                        else
                            {
                            long cMillis      = getSuspectLatestMillis();
                            int  cElapsedSecs = (int) ((cMillis -
                                    getSuspectInitialMillis()) / 1000L);
                            long cOrigLength  = getSuspectInitialLength();
                            long cbOrigBytes  = getSuspectInitialBytes();
                            long cPrevLength  = getSuspectLatestLength();
                            long cbPrevBytes  = getSuspectLatestBytes();
            
                            int  cChecks      = getSuspectCheckCount();
                            int  cBytesWorse  = getSuspectBytesWorseCount();
                            int  cLengthWorse = getSuspectLengthWorseCount();
            
                            boolean fBytesWorse, fLengthWorse;
                            if (fBytesWorse = cbBehind > cbPrevBytes)
                                {
                                ++cBytesWorse;
                                }
                            if (fLengthWorse = cBehind > cPrevLength)
                                {
                                ++cLengthWorse;
                                }
                            ++cChecks;
            
                            int cPctBytesChange  = (int) (cbBehind * 100L / cbOrigBytes);
                            int cPctLengthChange = (int) (cBehind * 100L / cOrigLength);
                            int cPctBytesWorse   = cBytesWorse * 100 / cChecks;
                            int cPctLengthWorse  = cLengthWorse * 100 / cChecks;
            
                            if (cbBehind > cbOrigBytes &&
                                    ((cChecks > 20 && cElapsedSecs > 60
                                    && (cPctBytesWorse > 90 || cPctLengthWorse > 90))
                                ||  (cChecks > 6 && cElapsedSecs > 20
                                    && (cPctBytesWorse == 100 || cPctLengthWorse == 100))))
                                {
                                fKill  = true;
                                sCause = "The connection has been monitored as a "
                                        + "suspect for the past " + cElapsedSecs
                                        + " seconds and its backlog has increased "
                                        + "with alarming consistency; the backlog "
                                        + "length has increased " + cPctLengthWorse
                                        + "% of the time and the backlog memory "
                                        + "usage has increased " + cPctBytesWorse
                                        + "% of the time; the connection is now "
                                        + cBehind + " messages behind (" + cbBehind
                                        + " bytes).";
                                }
                            else if (cbBehind < cbOrigBytes
                                    && cChecks > 20 && cElapsedSecs > 60
                                    && (cPctBytesWorse < 10 || cPctLengthWorse < 10))
                                {
                                fClear = true;
                                sCause = "The connection has been monitored as a "
                                        + "suspect for the past " + cElapsedSecs
                                        + " seconds and its backlog has decreased "
                                        + "with reassuring consistency; the backlog "
                                        + "length has decreased " + (100-cPctLengthWorse)
                                        + "% of the time and the backlog memory "
                                        + "usage has decreased " + (100-cPctBytesWorse)
                                        + "% of the time; the connection is now only "
                                        + cBehind + " messages behind (" + cbBehind
                                        + " bytes).";
                                }
            
                            // update stats
                            setSuspectLatestBytes(cbBehind);
                            setSuspectLatestLength(cBehind);
                            setSuspectCheckCount(cChecks);
                            setSuspectLengthWorseCount(cLengthWorse);
                            setSuspectBytesWorseCount(cBytesWorse);
                            }
            
                        if (fClear)
                            {
                            _trace("Extend*TCP has determined that "
                                    + toString()
                                    + " is no longer a suspect: "
                                    + sCause, 3);
                            setSuspect(false);
                            }
                        else if (fKill)
                            {
                            _trace("Extend*TCP has determined that "
                                    + toString()
                                    + " must be closed to maintain "
                                    + "system stability: " + sCause, 1);
                            setSuspect(false);
            
                            // see Channel#post
                            setCloseOnExit(true);
                            setCloseNotify(false);
                            setCloseThrowable(new SuspectConnectionException(sCause, this));
                            }
                        }
                    }
                }
            else if ((pool.getOverflow() > 0
                        || cBehind > acceptor.getDefaultSuspectLength()
                        || cbBehind > acceptor.getDefaultSuspectBytes())
                    && isSuspectEvaluate())
                {
                final long NOMINAL_BYTES  = acceptor.getDefaultNominalBytes();
                final long NOMINAL_LENGTH = acceptor.getDefaultNominalLength();
                final long SUSPECT_BYTES  = acceptor.getDefaultSuspectBytes();
                final long SUSPECT_LENGTH = acceptor.getDefaultSuspectLength();
            
                // the Acceptor is either in an overflow state or this connection's
                // queue has grown long or big enough to qualify as a potential
                // run-away queue
                long    cTarget  = NOMINAL_LENGTH;
                long    cbTarget = NOMINAL_BYTES;
                String  sCause   = null;
            
                // use the defaults to determine if the connection is automatically
                // a suspect
                if (cBehind > SUSPECT_LENGTH || cbBehind > SUSPECT_BYTES)
                    {
                    fSuspect = true;
                    sCause   = "The connection has fallen " + cBehind + " messages ("
                            + cbBehind + " bytes) behind; the threshold is "
                            + SUSPECT_LENGTH + " messages or " + SUSPECT_BYTES
                            + " bytes.";
                    }
            
                // potentially adjust the "correction target" if the capacity has
                // been specified; don't kick in the capacity setting until a client
                // has time to "warm up", since it may initially use more than its
                // "fair share" as defined by capacity
                int cAliveSecs = (int) ((getSuspectLatestMillis() -
                        getConnectTimeMillis()) / 1000L);
                if (pool.isCapacityLimited() && cAliveSecs > 30)
                    {
                    // the buffer capacity has been exhausted, which means that the
                    // the Acceptor is potentially contributing to system
                    // instability; if the queue is growing boundlessly, then it
                    // will never catch up and eventually crash the system; determine
                    // if this connection is a suspect contributor to the problem by
                    // using more than its "fair share" of the pool
                    int cConnections = acceptor.getConnectionSet().size();
                    int cbCapacity   = pool.getCapacity() * pool.getBufferSize();
                    int cbSuspect    = (int) Math.min(SUSPECT_BYTES, Math.max(
                            NOMINAL_BYTES, cbCapacity / cConnections / 2 * 3));
                    if (cbBehind > cbSuspect)
                        {
                        // compute "fair" limit
                        cbTarget = Math.min(SUSPECT_BYTES, Math.max(
                            NOMINAL_BYTES, cbCapacity / cConnections));
            
                        fSuspect = true;
                        sCause   = "The connection has fallen " + cBehind + " messages ("
                                + cbBehind + " bytes) behind; with " + cConnections
                                + " connections, the fair share per connection is"
                                + cbTarget + " bytes.";
                        }
                    }
            
                if (fSuspect)
                    {
                    // before accepting that this connection is a suspect, compare
                    // it to the other connections to see how many are worse
                    int cConns = 0, cWorse = 0;
                    try
                        {
                        for (Iterator iter = acceptor.getConnectionSet().iterator();
                                iter.hasNext(); )
                            {
                            TcpAcceptor.TcpConnection conn = (TcpAcceptor.TcpConnection) iter.next();
                            if (conn != null && conn != this)
                                {
                                ++cConns;
            
                                long cThatBehind  = conn.getStatsQueued()
                                        - conn.getStatsSent();
                                long cbThatBehind = conn.getStatsBytesQueued()
                                        - conn.getStatsBytesSent();
                                if ((cThatBehind > 0L && cThatBehind < Integer.MAX_VALUE
                                        && cThatBehind > cBehind) ||
                                        (cbThatBehind > 0L && cbThatBehind < Integer.MAX_VALUE
                                        && cbThatBehind > cbBehind))
                                    {
                                    ++cWorse;
                                    }
                                }
                            }
                        }
                    catch (ConcurrentModificationException e)
                        {
                        // it's OK to ignore because we'll check the connection
                        // again in a few seconds
                        return;
                        }
            
                    // only mark the "worst 5%" as suspects
                    if (cWorse < Math.max(3, cConns / 20))
                        {
                        synchronized (queue)
                            {
                            // store the information about the suspect determination
                            setSuspect(true);
                            setSuspectInitialMillis(getSuspectLatestMillis());
                            setSuspectInitialBytes(cbBehind);
                            setSuspectInitialLength(cBehind);
                            setSuspectLatestBytes(cbBehind);
                            setSuspectLatestLength(cBehind);
                            setSuspectCheckCount(0);
                            setSuspectLengthWorseCount(0);
                            setSuspectBytesWorseCount(0);
                            setSuspectTargetBytes(cbTarget);
                            setSuspectTargetLength(cTarget);
                            }
            
                        _trace("Extend*TCP has marked " + toString() 
                                + " as suspect: " + sCause, 3);
                        }
                    }
                }
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
            // import com.tangosol.util.ThreadGate;
            
            ThreadGate gate = getThreadGate();
            gate.barEntry(-1L);
            try
                {
                notifyWaitingThreads();
                return super.closeInternal(fNotify, e, cMillis);
                }
            finally
                {
                gate.open();
                }
            }
        
        // Accessor for the property "ClusterName"
        /**
         * Getter for property ClusterName.<p>
        * The name of the cluster the peer wishes to connect to.
        * 
        * @since 12.2.1
         */
        public String getClusterName()
            {
            return __m_ClusterName;
            }
        
        // Accessor for the property "ConnectTimeMillis"
        /**
         * Getter for property ConnectTimeMillis.<p>
        * The date/time value when the client connected.
         */
        public long getConnectTimeMillis()
            {
            return __m_ConnectTimeMillis;
            }
        
        // Accessor for the property "IncomingByteBuffer"
        /**
         * Getter for property IncomingByteBuffer.<p>
        * A ByteBuffer used to read the length of the next incoming Message.
         */
        public java.nio.ByteBuffer getIncomingByteBuffer()
            {
            return __m_IncomingByteBuffer;
            }
        
        // Accessor for the property "IncomingBytesRead"
        /**
         * Getter for property IncomingBytesRead.<p>
        * The number of bytes of the current Message being read that have been
        * read from the Socket associated with this TcpConnection.
         */
        public int getIncomingBytesRead()
            {
            return __m_IncomingBytesRead;
            }
        
        // Accessor for the property "IncomingBytesTotal"
        /**
         * Getter for property IncomingBytesTotal.<p>
        * The length (in bytes) of the current Message being read from the
        * Socket associated with this TcpConnection.
         */
        public int getIncomingBytesTotal()
            {
            return __m_IncomingBytesTotal;
            }
        
        // Accessor for the property "IncomingState"
        /**
         * Getter for property IncomingState.<p>
        * The current state of the next incoming Message. The value of this
        * property may be one of:
        *   IN_INITIAL
        *   IN_DECODED
        *   IN_PREPARED
         */
        public int getIncomingState()
            {
            return __m_IncomingState;
            }
        
        // Accessor for the property "IncomingWriteBufferArray"
        /**
         * Getter for property IncomingWriteBufferArray.<p>
        * An array of ByteBufferWriteBuffer objects used to hold the content of
        * the Message that is currently being read from the Socket associated
        * with this TcpConnection.
         */
        public com.tangosol.io.nio.ByteBufferWriteBuffer[] getIncomingWriteBufferArray()
            {
            return __m_IncomingWriteBufferArray;
            }
        
        // Accessor for the property "IncomingWriteBufferIndex"
        /**
         * Getter for property IncomingWriteBufferIndex.<p>
        * The index of the IncomingWriteBufferArray that contains the
        * ByteBufferWriteBuffer that is being used to hold the portion of the
        * Message that is currently being read from the Socket associated with
        * this TcpConnection.
         */
        public int getIncomingWriteBufferIndex()
            {
            return __m_IncomingWriteBufferIndex;
            }
        
        // Accessor for the property "OutgoingByteBuffer"
        /**
         * Getter for property OutgoingByteBuffer.<p>
        * A ByteBuffer used to write the length of the next outgoing Message.
         */
        public java.nio.ByteBuffer getOutgoingByteBuffer()
            {
            return __m_OutgoingByteBuffer;
            }
        
        // Accessor for the property "OutgoingBytesTotal"
        /**
         * Getter for property OutgoingBytesTotal.<p>
        * The length (in bytes) of the current Message being written to the
        * Socket associated with this TcpConnection.
         */
        public int getOutgoingBytesTotal()
            {
            return __m_OutgoingBytesTotal;
            }
        
        // Accessor for the property "OutgoingBytesWritten"
        /**
         * Getter for property OutgoingBytesWritten.<p>
        * The number of bytes of the current Message being written that have
        * been written to the Socket associated with this TcpConnection.
         */
        public int getOutgoingBytesWritten()
            {
            return __m_OutgoingBytesWritten;
            }
        
        // Accessor for the property "OutgoingMessage"
        /**
         * Getter for property OutgoingMessage.<p>
        * A MultiBufferWriteBuffer containing the contents of the current
        * outgoing Message or null if no Message is currently being sent.
         */
        public com.tangosol.io.MultiBufferWriteBuffer getOutgoingMessage()
            {
            return __m_OutgoingMessage;
            }
        
        // Accessor for the property "OutgoingQueue"
        /**
         * Getter for property OutgoingQueue.<p>
        * A Queue of encoded Message objects (stored as a
        * MultiBufferWriteBuffer of one or more ByteArrayWriteBuffer objects)
        * that need to be written to the Socket associated with this
        * TcpConnection.
         */
        public com.tangosol.coherence.component.util.Queue getOutgoingQueue()
            {
            return __m_OutgoingQueue;
            }
        
        // Accessor for the property "OutgoingState"
        /**
         * Getter for property OutgoingState.<p>
        * The current state of the next outgoing Message. The value of this
        * property may be one of:
        *   OUT_INITIAL
        *   OUT_PREPARED
        *   OUT_ENCODED
         */
        public int getOutgoingState()
            {
            return __m_OutgoingState;
            }
        
        // Accessor for the property "OutgoingWriteBufferArray"
        /**
         * Getter for property OutgoingWriteBufferArray.<p>
        * An array of ByteBufferWriteBuffer objects used to hold the content of
        * the Message that is currently being written to the Socket associated
        * with this TcpConnection.
         */
        public com.tangosol.io.nio.ByteBufferWriteBuffer[] getOutgoingWriteBufferArray()
            {
            return __m_OutgoingWriteBufferArray;
            }
        
        // Accessor for the property "OutgoingWriteBufferIndex"
        /**
         * Getter for property OutgoingWriteBufferIndex.<p>
        * The index of the OutgoingWriteBufferArray that contains the
        * ByteBufferWriteBuffer that holds the portion of the Message that is
        * currently being written to the Socket associated with this
        * TcpConnection.
         */
        public int getOutgoingWriteBufferIndex()
            {
            return __m_OutgoingWriteBufferIndex;
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
        
        // Accessor for the property "SelectionKey"
        /**
         * Getter for property SelectionKey.<p>
        * The SelectionKey representing the registration of this
        * TcpConnection's SocketChannel with its TcpAcceptor's Selector.
         */
        public java.nio.channels.SelectionKey getSelectionKey()
            {
            return __m_SelectionKey;
            }
        
        // Accessor for the property "ServiceName"
        /**
         * Getter for property ServiceName.<p>
        * The name of the service the peer wishes to connect to.
        * 
        * @since 12.2.1
         */
        public String getServiceName()
            {
            return __m_ServiceName;
            }
        
        // Accessor for the property "SocketChannel"
        /**
         * Getter for property SocketChannel.<p>
        * The SocketChannel used to send and receive data via the Socket
        * associated with this TcpConnection.
         */
        public java.nio.channels.SocketChannel getSocketChannel()
            {
            return __m_SocketChannel;
            }
        
        // Accessor for the property "StatsBytesQueued"
        /**
         * Getter for property StatsBytesQueued.<p>
        * Statistics: total number of bytes submitted to be sent (i.e. queued)
        * over this Connection. This value can be compared against the
        * StatsBytesSent to determine how many bytes have been queued but not
        * actually "sent" (where "sent" doesn't mean received by another
        * process or even sent on the wire, but rather copied to the OS's
        * TCP/IP buffers from which the information is actually sent).
        * 
        * @since Coherence 3.4
         */
        public long getStatsBytesQueued()
            {
            return __m_StatsBytesQueued;
            }
        
        // Accessor for the property "StatsQueued"
        /**
         * Getter for property StatsQueued.<p>
        * Statistics: total number of messages submitted to be sent (i.e.
        * queued)  over this Connection. This value can be compared against the
        * StatsSent to determine how many messages have been queued but not
        * actually "sent" (where "sent" doesn't mean received by another
        * process or even sent on the wire, but rather copied to the OS's
        * TCP/IP buffers from which the information is actually sent). The
        * difference should be the same (give or take due to multi-threading)
        * as the outgoing queue length.
        * 
        * @since Coherence 3.4
         */
        public long getStatsQueued()
            {
            return __m_StatsQueued;
            }
        
        // Accessor for the property "SuspectBytesBetterCount"
        /**
         * Getter for property SuspectBytesBetterCount.<p>
        * Number of checks that showed the suspect improving in terms of its
        * backlog of bytes to send. (Only relevant if Suspect==true.)
        * 
        * @since Coherence 3.4
         */
        public int getSuspectBytesBetterCount()
            {
            return getSuspectCheckCount() - getSuspectBytesWorseCount();
            }
        
        // Accessor for the property "SuspectBytesWorseCount"
        /**
         * Getter for property SuspectBytesWorseCount.<p>
        * Number of checks that showed the suspect getting worse in terms of
        * the number of bytes in the backlog. (Only relevant if Suspect==true.)
        * 
        * @since Coherence 3.4
         */
        public int getSuspectBytesWorseCount()
            {
            return __m_SuspectBytesWorseCount;
            }
        
        // Accessor for the property "SuspectCheckCount"
        /**
         * Getter for property SuspectCheckCount.<p>
        * Number of times that the suspect has been checked after being
        * declared a suspect. (Only relevant if Suspect==true.)
        * 
        * @since Coherence 3.4
         */
        public int getSuspectCheckCount()
            {
            return __m_SuspectCheckCount;
            }
        
        // Accessor for the property "SuspectInitialBytes"
        /**
         * Getter for property SuspectInitialBytes.<p>
        * The number of bytes queued when the initial check was done that
        * determined that this connection is suspect. (Only relevant if
        * Suspect==true.)
         */
        public long getSuspectInitialBytes()
            {
            return __m_SuspectInitialBytes;
            }
        
        // Accessor for the property "SuspectInitialLength"
        /**
         * Getter for property SuspectInitialLength.<p>
        * The length of the queue when the initial check was done that
        * determined that this connection is suspect. (Only relevant if
        * Suspect==true.)
         */
        public long getSuspectInitialLength()
            {
            return __m_SuspectInitialLength;
            }
        
        // Accessor for the property "SuspectInitialMillis"
        /**
         * Getter for property SuspectInitialMillis.<p>
        * The time at which the connection was determined to be a suspect.
        * (Only relevant if Suspect==true.)
         */
        public long getSuspectInitialMillis()
            {
            return __m_SuspectInitialMillis;
            }
        
        // Accessor for the property "SuspectLatestBytes"
        /**
         * Getter for property SuspectLatestBytes.<p>
        * The number of bytes queued when the last check was done. (Only
        * relevant if Suspect==true.)
         */
        public long getSuspectLatestBytes()
            {
            return __m_SuspectLatestBytes;
            }
        
        // Accessor for the property "SuspectLatestLength"
        /**
         * Getter for property SuspectLatestLength.<p>
        * The length of the queue when the last check was done. (Only
        * relevant if Suspect==true.)
         */
        public long getSuspectLatestLength()
            {
            return __m_SuspectLatestLength;
            }
        
        // Accessor for the property "SuspectLatestMillis"
        /**
         * Getter for property SuspectLatestMillis.<p>
        * The time at which the connection suspect status was last evaluated.
         */
        public long getSuspectLatestMillis()
            {
            return __m_SuspectLatestMillis;
            }
        
        // Accessor for the property "SuspectLengthBetterCount"
        /**
         * Getter for property SuspectLengthBetterCount.<p>
        * Number of checks that showed the suspect queue length improving.
        * (Only relevant if Suspect==true.)
         */
        public int getSuspectLengthBetterCount()
            {
            return getSuspectCheckCount() - getSuspectLengthWorseCount();
            }
        
        // Accessor for the property "SuspectLengthWorseCount"
        /**
         * Getter for property SuspectLengthWorseCount.<p>
        * Number of checks that showed the suspect queue length getting worse.
        * (Only relevant if Suspect==true.)
         */
        public int getSuspectLengthWorseCount()
            {
            return __m_SuspectLengthWorseCount;
            }
        
        // Accessor for the property "SuspectTargetBytes"
        /**
         * Getter for property SuspectTargetBytes.<p>
        * The target number of bytes queued to get below for the connection to
        * lose its "suspect" status. (Only relevant if Suspect==true.)
         */
        public long getSuspectTargetBytes()
            {
            return __m_SuspectTargetBytes;
            }
        
        // Accessor for the property "SuspectTargetLength"
        /**
         * Getter for property SuspectTargetLength.<p>
        * The length of the queue to get below for the connection to lose its
        * "suspect" status. (Only relevant if Suspect==true.)
         */
        public long getSuspectTargetLength()
            {
            return __m_SuspectTargetLength;
            }
        
        // Accessor for the property "IncomingDisabled"
        /**
         * Getter for property IncomingDisabled.<p>
         */
        public boolean isIncomingDisabled()
            {
            return __m_IncomingDisabled;
            }
        
        // Accessor for the property "OutgoingDisabled"
        /**
         * Getter for property OutgoingDisabled.<p>
         */
        public boolean isOutgoingDisabled()
            {
            return __m_OutgoingDisabled;
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
        
        // Accessor for the property "RedirectSupported"
        /**
         * Getter for property RedirectSupported.<p>
        * True if the TcpConnection supports redirection.
         */
        public boolean isRedirectSupported()
            {
            return __m_RedirectSupported;
            }
        
        // Accessor for the property "Suspect"
        /**
         * Getter for property Suspect.<p>
        * True if this connection has been determined to be a "suspect", i.e. a
        * resource-hogging / runaway train that will ultimately crash the
        * server by sucking up too much memory.
        * 
        * @since Coherence 3.4
         */
        public boolean isSuspect()
            {
            return __m_Suspect;
            }
        
        // Accessor for the property "SuspectEvaluate"
        /**
         * Getter for property SuspectEvaluate.<p>
        * Returns true if enough time has passed to evaluate the suspect
        * status. This property only returns true after a certain period of
        * time has passed since it last returned true.
        * 
        * @since Coherence 3.4
         */
        protected boolean isSuspectEvaluate()
            {
            // import com.tangosol.util.Base;
            
            boolean fEvaluate = false;
            
            synchronized (getOutgoingQueue())
                {
                long ldtCurrent  = Base.getSafeTimeMillis();
                long ldtPrevious = getSuspectLatestMillis();
                if (ldtCurrent > ldtPrevious + 3000L)
                    {
                    setSuspectLatestMillis(ldtCurrent);
                    fEvaluate = true;
                    }
                }
            
            return fEvaluate;
            }
        
        /**
         * Notify all Threads currently waiting for outgoing Messages to be
        * sent.
        * 
        * @see #send
         */
        protected void notifyWaitingThreads()
            {
            // import java.util.Iterator;
            
            Object o = getOutgoingMessage();
            if (o != null)
                {
                synchronized (o)
                    {
                    o.notify();
                    }
                }
            
            for (Iterator iter = getOutgoingQueue().iterator(); iter.hasNext(); )
                {
                o = iter.next();
                synchronized (o)
                    {
                    o.notify();
                    }
                }
            }
        
        // Declared at the super level
        /**
         * The "component has been initialized" method-notification called out
        * of setConstructed() for the topmost component and that in turn
        * notifies all the children.
        * 
        * This notification gets called before the control returns back to this
        * component instantiator (using <code>new Component.X()</code> or
        * <code>_newInstance(sName)</code>) and on the same thread. In
        * addition, visual components have a "posted" notification
        * <code>onInitUI</code> that is called after (or at the same time as)
        * the control returns back to the instantiator and possibly on a
        * different thread.
         */
        public void onInit()
            {
            // import Component.Util.Queue.ConcurrentQueue.DualQueue;
            
            setOutgoingQueue(new DualQueue());
            setConnectTimeMillis(System.currentTimeMillis());
            
            super.onInit();
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
            // import Component.Util.Queue as com.tangosol.coherence.component.util.Queue;
            
            assertOpen();
            
            TcpAcceptor  acceptor = (TcpAcceptor) getConnectionManager();
            com.tangosol.coherence.component.util.Queue        queue    = getOutgoingQueue();
            
            synchronized (wb)
                {
                int cb = wb.length();
            
                boolean fFlush, fSuspect;
                long    cQueued, cbQueued, cSent, cbSent;

                queue.lock();  // prevent unnecessary flushes
                try
                    {
                    queue.add(wb);
                    fFlush = queue.size() == 1;
            
                    // update queueing stats (this must be done while synchronized since
                    // multiple threads can call send concurrently)
                    cQueued  = getStatsQueued() + 1;
                    cbQueued = getStatsBytesQueued() + cb;
            
                    setStatsQueued(cQueued);
                    setStatsBytesQueued(cbQueued);
            
                    // snapshot the amount sent so far while we're at it
                    cSent  = getStatsSent();
                    cbSent = getStatsBytesSent();
            
                    fSuspect = isSuspect();
                    }
                finally
                    {
                    queue.unlock();
                    }
            
                if (fFlush)
                    {
                    acceptor.getConnectionFlushQueue().add(this);
                    acceptor.getProcessor().wakeup();
                    }
            
                if (acceptor.isSuspectProtocolEnabled())
                    {
                    checkSuspect(fSuspect, cQueued, cbQueued, cSent, cbSent);
                    }
                }
            }
        
        // Accessor for the property "ClusterName"
        /**
         * Setter for property ClusterName.<p>
        * The name of the cluster the peer wishes to connect to.
        * 
        * @since 12.2.1
         */
        public void setClusterName(String sName)
            {
            __m_ClusterName = sName;
            }
        
        // Accessor for the property "ConnectTimeMillis"
        /**
         * Setter for property ConnectTimeMillis.<p>
        * The date/time value when the client connected.
         */
        protected void setConnectTimeMillis(long cMillis)
            {
            __m_ConnectTimeMillis = cMillis;
            }
        
        // Accessor for the property "IncomingByteBuffer"
        /**
         * Setter for property IncomingByteBuffer.<p>
        * A ByteBuffer used to read the length of the next incoming Message.
         */
        public void setIncomingByteBuffer(java.nio.ByteBuffer bb)
            {
            __m_IncomingByteBuffer = bb;
            }
        
        // Accessor for the property "IncomingBytesRead"
        /**
         * Setter for property IncomingBytesRead.<p>
        * The number of bytes of the current Message being read that have been
        * read from the Socket associated with this TcpConnection.
         */
        public void setIncomingBytesRead(int cb)
            {
            __m_IncomingBytesRead = cb;
            }
        
        // Accessor for the property "IncomingBytesTotal"
        /**
         * Setter for property IncomingBytesTotal.<p>
        * The length (in bytes) of the current Message being read from the
        * Socket associated with this TcpConnection.
         */
        public void setIncomingBytesTotal(int cb)
            {
            __m_IncomingBytesTotal = cb;
            }
        
        // Accessor for the property "IncomingDisabled"
        /**
         * Setter for property IncomingDisabled.<p>
         */
        public void setIncomingDisabled(boolean fDisabled)
            {
            // import java.nio.channels.SelectionKey;
            
            if (fDisabled != isIncomingDisabled())
                {
                SelectionKey key = getSelectionKey();
                if (key != null)
                    {
                    if (fDisabled)
                        {
                        key.interestOps(key.interestOps() & ~SelectionKey.OP_READ);
                        }
                    else
                        {
                        key.interestOps(key.interestOps() | SelectionKey.OP_READ);
                        }
                    __m_IncomingDisabled = (fDisabled);
                    }
                }
            }
        
        // Accessor for the property "IncomingState"
        /**
         * Setter for property IncomingState.<p>
        * The current state of the next incoming Message. The value of this
        * property may be one of:
        *   IN_INITIAL
        *   IN_DECODED
        *   IN_PREPARED
         */
        public void setIncomingState(int nState)
            {
            __m_IncomingState = nState;
            }
        
        // Accessor for the property "IncomingWriteBufferArray"
        /**
         * Setter for property IncomingWriteBufferArray.<p>
        * An array of ByteBufferWriteBuffer objects used to hold the content of
        * the Message that is currently being read from the Socket associated
        * with this TcpConnection.
         */
        public void setIncomingWriteBufferArray(com.tangosol.io.nio.ByteBufferWriteBuffer[] awb)
            {
            __m_IncomingWriteBufferArray = awb;
            }
        
        // Accessor for the property "IncomingWriteBufferIndex"
        /**
         * Setter for property IncomingWriteBufferIndex.<p>
        * The index of the IncomingWriteBufferArray that contains the
        * ByteBufferWriteBuffer that is being used to hold the portion of the
        * Message that is currently being read from the Socket associated with
        * this TcpConnection.
         */
        public void setIncomingWriteBufferIndex(int i)
            {
            __m_IncomingWriteBufferIndex = i;
            }
        
        // Accessor for the property "OutgoingByteBuffer"
        /**
         * Setter for property OutgoingByteBuffer.<p>
        * A ByteBuffer used to write the length of the next outgoing Message.
         */
        public void setOutgoingByteBuffer(java.nio.ByteBuffer bb)
            {
            __m_OutgoingByteBuffer = bb;
            }
        
        // Accessor for the property "OutgoingBytesTotal"
        /**
         * Setter for property OutgoingBytesTotal.<p>
        * The length (in bytes) of the current Message being written to the
        * Socket associated with this TcpConnection.
         */
        public void setOutgoingBytesTotal(int cb)
            {
            __m_OutgoingBytesTotal = cb;
            }
        
        // Accessor for the property "OutgoingBytesWritten"
        /**
         * Setter for property OutgoingBytesWritten.<p>
        * The number of bytes of the current Message being written that have
        * been written to the Socket associated with this TcpConnection.
         */
        public void setOutgoingBytesWritten(int cb)
            {
            __m_OutgoingBytesWritten = cb;
            }
        
        // Accessor for the property "OutgoingDisabled"
        /**
         * Setter for property OutgoingDisabled.<p>
         */
        public void setOutgoingDisabled(boolean fDisabled)
            {
            // import java.nio.channels.SelectionKey;
            
            if (fDisabled != isOutgoingDisabled())
                {
                SelectionKey key = getSelectionKey();
                if (key != null)
                    {
                    if (fDisabled)
                        {
                        key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
                        }
                    else
                        {
                        key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
                        }
                    __m_OutgoingDisabled = (fDisabled);
                    }
                }
            }
        
        // Accessor for the property "OutgoingMessage"
        /**
         * Setter for property OutgoingMessage.<p>
        * A MultiBufferWriteBuffer containing the contents of the current
        * outgoing Message or null if no Message is currently being sent.
         */
        public void setOutgoingMessage(com.tangosol.io.MultiBufferWriteBuffer mwb)
            {
            __m_OutgoingMessage = mwb;
            }
        
        // Accessor for the property "OutgoingQueue"
        /**
         * Setter for property OutgoingQueue.<p>
        * A Queue of encoded Message objects (stored as a
        * MultiBufferWriteBuffer of one or more ByteArrayWriteBuffer objects)
        * that need to be written to the Socket associated with this
        * TcpConnection.
         */
        public void setOutgoingQueue(com.tangosol.coherence.component.util.Queue queue)
            {
            __m_OutgoingQueue = queue;
            }
        
        // Accessor for the property "OutgoingState"
        /**
         * Setter for property OutgoingState.<p>
        * The current state of the next outgoing Message. The value of this
        * property may be one of:
        *   OUT_INITIAL
        *   OUT_PREPARED
        *   OUT_ENCODED
         */
        public void setOutgoingState(int nState)
            {
            __m_OutgoingState = nState;
            }
        
        // Accessor for the property "OutgoingWriteBufferArray"
        /**
         * Setter for property OutgoingWriteBufferArray.<p>
        * An array of ByteBufferWriteBuffer objects used to hold the content of
        * the Message that is currently being written to the Socket associated
        * with this TcpConnection.
         */
        public void setOutgoingWriteBufferArray(com.tangosol.io.nio.ByteBufferWriteBuffer[] awb)
            {
            __m_OutgoingWriteBufferArray = awb;
            }
        
        // Accessor for the property "OutgoingWriteBufferIndex"
        /**
         * Setter for property OutgoingWriteBufferIndex.<p>
        * The index of the OutgoingWriteBufferArray that contains the
        * ByteBufferWriteBuffer that holds the portion of the Message that is
        * currently being written to the Socket associated with this
        * TcpConnection.
         */
        public void setOutgoingWriteBufferIndex(int i)
            {
            __m_OutgoingWriteBufferIndex = i;
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
        
        // Accessor for the property "RedirectSupported"
        /**
         * Setter for property RedirectSupported.<p>
        * True if the TcpConnection supports redirection.
         */
        public void setRedirectSupported(boolean f)
            {
            __m_RedirectSupported = f;
            }
        
        // Accessor for the property "SelectionKey"
        /**
         * Setter for property SelectionKey.<p>
        * The SelectionKey representing the registration of this
        * TcpConnection's SocketChannel with its TcpAcceptor's Selector.
         */
        public void setSelectionKey(java.nio.channels.SelectionKey key)
            {
            __m_SelectionKey = key;
            }
        
        // Accessor for the property "ServiceName"
        /**
         * Setter for property ServiceName.<p>
        * The name of the service the peer wishes to connect to.
        * 
        * @since 12.2.1
         */
        public void setServiceName(String sName)
            {
            __m_ServiceName = sName;
            }
        
        // Accessor for the property "SocketChannel"
        /**
         * Setter for property SocketChannel.<p>
        * The SocketChannel used to send and receive data via the Socket
        * associated with this TcpConnection.
         */
        public void setSocketChannel(java.nio.channels.SocketChannel channel)
            {
            __m_SocketChannel = channel;
            }
        
        // Accessor for the property "StatsBytesQueued"
        /**
         * Setter for property StatsBytesQueued.<p>
        * Statistics: total number of bytes submitted to be sent (i.e. queued)
        * over this Connection. This value can be compared against the
        * StatsBytesSent to determine how many bytes have been queued but not
        * actually "sent" (where "sent" doesn't mean received by another
        * process or even sent on the wire, but rather copied to the OS's
        * TCP/IP buffers from which the information is actually sent).
        * 
        * @since Coherence 3.4
         */
        public void setStatsBytesQueued(long cb)
            {
            __m_StatsBytesQueued = cb;
            }
        
        // Accessor for the property "StatsQueued"
        /**
         * Setter for property StatsQueued.<p>
        * Statistics: total number of messages submitted to be sent (i.e.
        * queued)  over this Connection. This value can be compared against the
        * StatsSent to determine how many messages have been queued but not
        * actually "sent" (where "sent" doesn't mean received by another
        * process or even sent on the wire, but rather copied to the OS's
        * TCP/IP buffers from which the information is actually sent). The
        * difference should be the same (give or take due to multi-threading)
        * as the outgoing queue length.
        * 
        * @since Coherence 3.4
         */
        public void setStatsQueued(long cMessage)
            {
            __m_StatsQueued = cMessage;
            }
        
        // Accessor for the property "Suspect"
        /**
         * Setter for property Suspect.<p>
        * True if this connection has been determined to be a "suspect", i.e. a
        * resource-hogging / runaway train that will ultimately crash the
        * server by sucking up too much memory.
        * 
        * @since Coherence 3.4
         */
        protected void setSuspect(boolean fSuspect)
            {
            __m_Suspect = fSuspect;
            }
        
        // Accessor for the property "SuspectBytesWorseCount"
        /**
         * Setter for property SuspectBytesWorseCount.<p>
        * Number of checks that showed the suspect getting worse in terms of
        * the number of bytes in the backlog. (Only relevant if Suspect==true.)
        * 
        * @since Coherence 3.4
         */
        protected void setSuspectBytesWorseCount(int cChecks)
            {
            __m_SuspectBytesWorseCount = cChecks;
            }
        
        // Accessor for the property "SuspectCheckCount"
        /**
         * Setter for property SuspectCheckCount.<p>
        * Number of times that the suspect has been checked after being
        * declared a suspect. (Only relevant if Suspect==true.)
        * 
        * @since Coherence 3.4
         */
        protected void setSuspectCheckCount(int cChecks)
            {
            __m_SuspectCheckCount = cChecks;
            }
        
        // Accessor for the property "SuspectInitialBytes"
        /**
         * Setter for property SuspectInitialBytes.<p>
        * The number of bytes queued when the initial check was done that
        * determined that this connection is suspect. (Only relevant if
        * Suspect==true.)
         */
        protected void setSuspectInitialBytes(long cb)
            {
            __m_SuspectInitialBytes = cb;
            }
        
        // Accessor for the property "SuspectInitialLength"
        /**
         * Setter for property SuspectInitialLength.<p>
        * The length of the queue when the initial check was done that
        * determined that this connection is suspect. (Only relevant if
        * Suspect==true.)
         */
        protected void setSuspectInitialLength(long c)
            {
            __m_SuspectInitialLength = c;
            }
        
        // Accessor for the property "SuspectInitialMillis"
        /**
         * Setter for property SuspectInitialMillis.<p>
        * The time at which the connection was determined to be a suspect.
        * (Only relevant if Suspect==true.)
         */
        protected void setSuspectInitialMillis(long cMillis)
            {
            __m_SuspectInitialMillis = cMillis;
            }
        
        // Accessor for the property "SuspectLatestBytes"
        /**
         * Setter for property SuspectLatestBytes.<p>
        * The number of bytes queued when the last check was done. (Only
        * relevant if Suspect==true.)
         */
        protected void setSuspectLatestBytes(long cb)
            {
            __m_SuspectLatestBytes = cb;
            }
        
        // Accessor for the property "SuspectLatestLength"
        /**
         * Setter for property SuspectLatestLength.<p>
        * The length of the queue when the last check was done. (Only
        * relevant if Suspect==true.)
         */
        protected void setSuspectLatestLength(long c)
            {
            __m_SuspectLatestLength = c;
            }
        
        // Accessor for the property "SuspectLatestMillis"
        /**
         * Setter for property SuspectLatestMillis.<p>
        * The time at which the connection suspect status was last evaluated.
         */
        protected void setSuspectLatestMillis(long cMillis)
            {
            __m_SuspectLatestMillis = cMillis;
            }
        
        // Accessor for the property "SuspectLengthWorseCount"
        /**
         * Setter for property SuspectLengthWorseCount.<p>
        * Number of checks that showed the suspect queue length getting worse.
        * (Only relevant if Suspect==true.)
         */
        protected void setSuspectLengthWorseCount(int cChecks)
            {
            __m_SuspectLengthWorseCount = cChecks;
            }
        
        // Accessor for the property "SuspectTargetBytes"
        /**
         * Setter for property SuspectTargetBytes.<p>
        * The target number of bytes queued to get below for the connection to
        * lose its "suspect" status. (Only relevant if Suspect==true.)
         */
        protected void setSuspectTargetBytes(long cb)
            {
            __m_SuspectTargetBytes = cb;
            }
        
        // Accessor for the property "SuspectTargetLength"
        /**
         * Setter for property SuspectTargetLength.<p>
        * The length of the queue to get below for the connection to lose its
        * "suspect" status. (Only relevant if Suspect==true.)
         */
        protected void setSuspectTargetLength(long c)
            {
            __m_SuspectTargetLength = c;
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.TcpAcceptor$TcpProcessor
    
    /**
     * Daemon used to perform all non-blocking TCP/IP I/O operations.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class TcpProcessor
            extends    com.tangosol.coherence.component.util.Daemon
        {
        // ---- Fields declarations ----
        
        /**
         * Property Acceptor
         *
         * A direct reference to the TcpAcceptor module.
         */
        private transient TcpAcceptor __m_Acceptor;
        
        /**
         * Property Selector
         *
         * The Selector used to select from the various SelectableChannel
         * objects created by this TcpProcessor.
         */
        private java.nio.channels.Selector __m_Selector;
        
        /**
         * Property ServerSocket
         *
         * The ServerSocket used by this TcpProcessor to accept Socket
         * connections.
         */
        private java.net.ServerSocket __m_ServerSocket;
        
        /**
         * Property ServerSocketChannel
         *
         * The ServerSocketChannel used by this TcpProcessor to create its
         * ServerSocket.
         */
        private java.nio.channels.ServerSocketChannel __m_ServerSocketChannel;
        
        // Default constructor
        public TcpProcessor()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public TcpProcessor(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.TcpAcceptor.TcpProcessor();
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
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/peer/acceptor/TcpAcceptor$TcpProcessor".replace('/', '.'));
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
        
        /**
         * Return the configured ServerSocketChannel.
         */
        public java.nio.channels.ServerSocketChannel ensureServerSocketChannel()
            {
            // import Component.Net.Extend.Util.TcpUtil;
            // import com.tangosol.util.Base;
            // import java.io.IOException;
            // import java.net.ServerSocket;
            // import java.nio.channels.ServerSocketChannel;
            
            ServerSocketChannel channel = getServerSocketChannel();
            
            if (channel == null)
                {
                try
                    {
                    channel = getAcceptor().getSocketProvider().openServerSocketChannel();
                    }
                catch (IOException e)
                    {
                    throw Base.ensureRuntimeException(e, "error opening ServerSocketChannel");
                    }
            
                try
                    {
                    channel.configureBlocking(false);
                    ServerSocket socket = channel.socket();
                    getAcceptor().configureSocket(socket);
            
                    setServerSocket(socket);
                    setServerSocketChannel(channel);
                    }
                catch (Exception e)
                    {
                    TcpUtil.close(channel);
                    throw Base.ensureRuntimeException(e);
                    }
                }
            
            return channel;
            }
        
        /**
         * Iterate through the TcpConnection objects in the TcpAcceptor's
        * ConnectionFlushQueue, registering interest in OP_WRITE on each
        * TcpConnection's SelectionKey.
         */
        protected void flushConnections()
            {
            // import Component.Util.Queue as com.tangosol.coherence.component.util.Queue;
            // import java.nio.channels.CancelledKeyException;
            // import java.nio.channels.SelectionKey;
            
            com.tangosol.coherence.component.util.Queue queue = getAcceptor().getConnectionFlushQueue();
            for (TcpAcceptor.TcpConnection connection = (TcpAcceptor.TcpConnection) queue.removeNoWait();
                 connection != null;
                 connection = (TcpAcceptor.TcpConnection) queue.removeNoWait())
                {
                SelectionKey key = connection.getSelectionKey();
                if (key != null)
                    {
                    try
                        {
                        key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
                        }
                    catch (CancelledKeyException e)
                        {
                        // the Connection has been closed or released
                        }
                    }
                }
            }
        
        // Accessor for the property "Acceptor"
        /**
         * Getter for property Acceptor.<p>
        * A direct reference to the TcpAcceptor module.
         */
        public TcpAcceptor getAcceptor()
            {
            TcpAcceptor acceptor = __m_Acceptor;
            if (acceptor == null)
                {
                setAcceptor(acceptor = (TcpAcceptor) get_Module());
                }
            return acceptor;
            }
        
        // Accessor for the property "Selector"
        /**
         * Getter for property Selector.<p>
        * The Selector used to select from the various SelectableChannel
        * objects created by this TcpProcessor.
         */
        public java.nio.channels.Selector getSelector()
            {
            return __m_Selector;
            }
        
        // Accessor for the property "ServerSocket"
        /**
         * Getter for property ServerSocket.<p>
        * The ServerSocket used by this TcpProcessor to accept Socket
        * connections.
         */
        public java.net.ServerSocket getServerSocket()
            {
            return __m_ServerSocket;
            }
        
        // Accessor for the property "ServerSocketChannel"
        /**
         * Getter for property ServerSocketChannel.<p>
        * The ServerSocketChannel used by this TcpProcessor to create its
        * ServerSocket.
         */
        public synchronized java.nio.channels.ServerSocketChannel getServerSocketChannel()
            {
            return __m_ServerSocketChannel;
            }
        
        // Accessor for the property "ServerSocketKey"
        /**
         * Getter for property ServerSocketKey.<p>
        * The key associated with the ServerSocketChannel
         */
        public java.nio.channels.SelectionKey getServerSocketKey()
            {
            return getServerSocketChannel().keyFor(getSelector());
            }
        
        // Declared at the super level
        /**
         * Getter for property ThreadName.<p>
        * Specifies the name of the daemon thread. If not specified, the
        * component name will be used.
        * 
        * This property can be set at design time or runtime. If set at
        * runtime, this property must be configured before start() is invoked
        * to cause the daemon thread to have the specified name.
         */
        public String getThreadName()
            {
            return ((TcpAcceptor) get_Module()).getServiceName() + ':' + super.getThreadName();
            }
        
        /**
         * Called when a new SocketChannel has been accepted by the
        * ServerSocketChannel. This method opens a new TcpConnection for the
        * newly accepted SocketChannel.
        * 
        * @param key  the SelectionKey that has been selected for accept
         */
        protected void onAccept(java.nio.channels.SelectionKey key)
            {
            // import com.tangosol.net.InetAddressHelper;
            // import Component.Net.Extend.Util.TcpUtil;
            // import java.io.IOException;
            // import java.nio.channels.ClosedChannelException;
            // import java.nio.channels.SelectionKey;
            // import java.nio.channels.ServerSocketChannel;
            // import java.nio.channels.SocketChannel;
            // import java.net.InetAddress;
            // import java.net.Socket;
            // import javax.net.ssl.SSLException;
            
            if (!key.isValid())
                {
                return;
                }
            
            TcpAcceptor acceptor = getAcceptor();
            
            // accept the SocketChannel
            SocketChannel channel;
            try
                {
                ServerSocketChannel channelServer = (ServerSocketChannel) key.channel();
                channel = channelServer.accept();
                if (channel == null)
                    {
                    // COH-4483: #accept() can spuriously return null
                    return;
                    }
                }
            catch (ClosedChannelException e)
                {
                if (!isExiting())
                    {
                    _trace(e, "Caught an exception while accepting a Socket connection");
                    }
                return;
                }
            catch (SSLException e)
                {
                _trace("Could not accept a Socket connection due to: " + e, 2);
                return;
                }
            catch (IOException e)
                {
                _trace(e, "Error accepting a Socket connection");
                return;
                }
            
            // configure the SocketChannel (COH-4108)
            TcpUtil.setBlockingMode(channel, false);
            
            Socket socket = channel.socket();
            
            // authorize the remote host
            InetAddress remoteAddress = socket.getInetAddress();
            if (!acceptor.isAuthorizedHost(remoteAddress))
                {
                acceptor.setStatsUnauthorizedConnectionAttempts(
                         acceptor.getStatsUnauthorizedConnectionAttempts() + 1L);
                _trace("Received a connection attempt from remote address " +
                       InetAddressHelper.toString(remoteAddress) +
                       " that was not an authorized host", 6);
                TcpUtil.close(socket);
                TcpUtil.close(channel);
                return;
                }
            
            // configure the Socket
            try
                {
                acceptor.configureSocket(socket);
                }
            catch (RuntimeException e)
                {
                _trace(e, "Error accepting a connection for \"" + socket + '"');
                TcpUtil.close(socket);
                TcpUtil.close(channel);
                return;
                }
            
            // register interest in OP_READ for the new SocketChannel
            SelectionKey keyConnection;
            try
                {
                keyConnection = channel.register(getSelector(),
                        SelectionKey.OP_READ);
                }
            catch (ClosedChannelException e)
                {
                TcpUtil.close(socket);
                TcpUtil.close(channel);
                return;
                }
            
            // open a new TcpConnection
            TcpAcceptor.TcpConnection connection;
            try
                {
                connection = (TcpAcceptor.TcpConnection) acceptor.instantiateConnection();
                connection.setSelectionKey(keyConnection);
                connection.setSocket(socket);
                connection.setSocketChannel(channel);
                connection.open();
                }
            catch (Throwable e)
                {
                _trace(e, "An error occurred while creating a TcpConnection");
                TcpUtil.cancel(keyConnection);
                TcpUtil.close(socket);
                TcpUtil.close(channel);
                return;
                }
            
            // attach the Connection to the SelectionKey
            keyConnection.attach(connection);
            }
        
        // Declared at the super level
        /**
         * Event notification called once the daemon's thread starts and before
        * the daemon thread goes into the "wait - perform" loop. Unlike the
        * <code>onInit()</code> event, this method executes on the daemon's
        * thread.
        * 
        * Note1: this method is called while the caller's thread is still
        * waiting for a notification to  "unblock" itself.
        * Note2: any exception thrown by this method will terminate the thread
        * immediately
         */
        protected void onEnter()
            {
            // import com.tangosol.util.Base;
            // import java.io.IOException;
            // import java.net.ServerSocket;
            // import java.nio.channels.SelectionKey;
            // import java.nio.channels.Selector;
            // import java.nio.channels.ServerSocketChannel;
            
            super.onEnter();
            
            ServerSocketChannel channel = ensureServerSocketChannel();
            Selector            selector;
            try
                {
                setSelector(selector = channel.provider().openSelector());
                channel.register(selector, SelectionKey.OP_ACCEPT);
                }
            catch (Throwable t) // IOException, NullPointerException
                {
                // NOTE: http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6427854
                throw Base.ensureRuntimeException(t, "error opening Selector");
                }
            }
        
        /**
         * Called when a SocketChannel has reached end-of-stream
        * 
        * This method unregisters the key in order to prevent subsequent EOS
        * triggers, and schedules a gracefull cleanup of the connection.
        * 
        * @param key                the SelectionKey that has been selected for
        * read
        * @param connection  the associated connection
         */
        public void onEOS(java.nio.channels.SelectionKey key, TcpAcceptor.TcpConnection connection)
            {
            key.cancel();
            connection.close(false, null, false);
            }
        
        // Declared at the super level
        /**
         * This event occurs when an exception is thrown from onEnter, onWait,
        * onNotify and onExit.
        * 
        * If the exception should terminate the daemon, call stop(). The
        * default implementation prints debugging information and terminates
        * the daemon.
        * 
        * @param e  the Throwable object (a RuntimeException or an Error)
        * 
        * @throws RuntimeException may be thrown; will terminate the daemon
        * @throws Error may be thrown; will terminate the daemon
         */
        protected void onException(Throwable e)
            {
            if (isExiting())
                {
                super.onException(e);
                }
            else
                {
                // unhandled exception on the TcpProcessor is as fatal
                // as on the TcpAcceptor itself
                ((TcpAcceptor) get_Module()).onException(e);
                }
            }
        
        // Declared at the super level
        /**
         * Event notification called right before the daemon thread terminates.
        * This method is guaranteed to be called only once and on the daemon's
        * thread.
         */
        protected void onExit()
            {
            // import Component.Net.Extend.Util.TcpUtil;
            // import java.nio.channels.SelectionKey;
            // import java.nio.channels.Selector;
            // import java.util.Iterator;
            
            TcpUtil.cancel(getServerSocketKey());
            TcpUtil.close(getServerSocketChannel());
            TcpUtil.close(getServerSocket());
            
            // release all Connections
            Selector selector = getSelector();
            if (selector != null)
                {
                releaseConnections();
                for (Iterator iter = selector.keys().iterator(); iter.hasNext(); )
                    {
                    SelectionKey key = (SelectionKey) iter.next();
            
                    // add the attached TcpConnection to the release queue
                    Object o = key.attachment();
                    if (o instanceof TcpAcceptor.TcpConnection)
                        {
                        getAcceptor().getConnectionReleaseQueue().add(o);
                        }
                    else
                        {
                        TcpUtil.close(key.channel());
                        }
                    }
                releaseConnections();
                }
            
            TcpUtil.close(selector);
            }
        
        // Declared at the super level
        /**
         * Event notification to perform a regular daemon activity. To get it
        * called, another thread has to set Notification to true:
        * <code>daemon.setNotification(true);</code>
        * 
        * @see #onWait
         */
        protected void onNotify()
            {
            // import com.oracle.coherence.common.base.Blocking;
            // import java.io.IOException;
            // import java.nio.channels.ClosedSelectorException;
            // import java.nio.channels.Selector;
            
            Selector selector = getSelector();
            _assert(selector != null);
            
            while (!isExiting())
                {
                try
                    {
                    Blocking.select(selector, 500L);
                    onSelect(selector.selectedKeys());
            
                    flushConnections();
                    releaseConnections();
                    }
                catch (IOException e)
                    {
                    // NOTE:
                    // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4504001
                    // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6322825
                    if (!isExiting())
                        {
                        _trace(e, "Caught an I/O exception while processing a Socket; "
                                + "the exception has been logged and will be ignored");
                        }
                    }
                catch (ClosedSelectorException e)
                    {
                    onException(e);
                    }
                }
            }
        
        /**
         * Called when a SocketChannel has been selected for a read operation.
        * 
        * This method reads available data from the SocketChannel and updates
        * the corresponding TcpConnection with the read data. If a complete
        * Message is read from the SocketChannel, the encoded Message is
        * dispatched to the TcpAcceptor.
        * 
        * @param key  the SelectionKey that has been selected for read
         */
        protected void onRead(java.nio.channels.SelectionKey key)
            {
            // import com.tangosol.io.MultiBufferReadBuffer as com.tangosol.io.MultiBufferReadBuffer;
            // import com.tangosol.io.nio.ByteBufferWriteBuffer as com.tangosol.io.nio.ByteBufferWriteBuffer;
            // import com.tangosol.io.nio.ByteBufferReadBuffer as com.tangosol.io.nio.ByteBufferReadBuffer;
            // import com.tangosol.net.messaging.ConnectionException;
            // import java.net.Socket;
            // import java.nio.ByteBuffer;
            // import java.nio.channels.SelectionKey;
            // import java.nio.channels.SocketChannel;
            // import javax.net.ssl.SSLException;
            
            if (!key.isValid())
                {
                return;
                }
            
            TcpAcceptor.TcpConnection connection = (TcpAcceptor.TcpConnection) key.attachment();
            if (connection == null || !connection.isOpen())
                {
                key.interestOps(key.interestOps() & ~SelectionKey.OP_READ);
                return;
                }
            
            TcpAcceptor   acceptor = getAcceptor();
            SocketChannel channel  = (SocketChannel) key.channel();
            TcpAcceptor.BufferPool   pool     = acceptor.getBufferPoolIn();
            ByteBuffer    bb       = connection.getIncomingByteBuffer();
            int           cbRead   = connection.getIncomingBytesRead();
            int           cbTotal  = connection.getIncomingBytesTotal();
            int           nState   = connection.getIncomingState();
            com.tangosol.io.nio.ByteBufferWriteBuffer[] awb      = connection.getIncomingWriteBufferArray();
            int           iBuffer  = connection.getIncomingWriteBufferIndex();
            
            try
                {
                do
                    {
                    switch (nState)
                        {
                        default:
                            throw new IllegalStateException("unknown state: " + nState);
            
                        case TcpAcceptor.TcpConnection.IN_INITIAL:
                            {
                            if (bb == null)
                                {
                                // allocate a ByteBuffer large enough to store any packed int
                                connection.setIncomingByteBuffer(bb = ByteBuffer.allocate(5));
                                }
            
                            // attempt to read the incoming Message length from the Channel
                            if (channel.read(bb) == -1)
                                {
                                // EOF
                                onEOS(key, connection);
                                return;
                                }
            
                            cbTotal = readMessageLength(bb);
                            if (cbTotal < 0)
                                {
                                // the O/S buffer must be empty
                                return;
                                }
                            acceptor.enforceMaxIncomingMessageSize(cbTotal);
            
                            // the length of the Message is now decoded
                            nState = TcpAcceptor.TcpConnection.IN_DECODED;
                            }
            
                        case TcpAcceptor.TcpConnection.IN_DECODED:
                            {
                            int cbBuffer = pool.getBufferSize();
                            int cBuffer  = cbTotal / cbBuffer;
                            int cbLast   = cbTotal % cbBuffer;
            
                            if (cbLast > 0)
                                {
                                cBuffer++;
                                }
            
                            // allocate the com.tangosol.io.nio.ByteBufferWriteBuffer array and the first com.tangosol.io.nio.ByteBufferWriteBuffer
                            awb    = new com.tangosol.io.nio.ByteBufferWriteBuffer[cBuffer];
                            awb[0] = (com.tangosol.io.nio.ByteBufferWriteBuffer) pool.acquire();
            
                            // limit the first ByteBuffer, if necessary
                            if (cBuffer == 1 && cbLast > 0)
                                {
                                awb[0].getByteBuffer().limit(cbLast);
                                }
            
                            // copy any remaining data from the message length buffer
                            cbRead = bb.remaining();
                            if (cbRead <= cbTotal) // common case
                                {
                                awb[0].getByteBuffer().put(bb);
                                bb.clear();
                                }
                            else // cbRead > cbTotal; very rare multiple messages are in the 5 byte block we read
                                {
                                // copy the valid portion of this message from bb and slide the
                                // portion for the next message to the front of bb
                                slide(bb, awb[0].getByteBuffer(), cbTotal);
                                }
            
                            // the Connection is now prepared for read
                            nState = TcpAcceptor.TcpConnection.IN_PREPARED;
                            }
            
                        case TcpAcceptor.TcpConnection.IN_PREPARED:
                            {
                            // attempt to read incoming Messages from the Channel,
                            // continue from where we left off
                            int        cBuffers   = awb.length - iBuffer;
                            ByteBuffer aBuffers[] = new ByteBuffer[cBuffers];
                            for (int indx = 0; indx < cBuffers; ++indx)
                                {
                                // acquire a pooled com.tangosol.io.nio.ByteBufferWriteBuffer, if necessary
                                com.tangosol.io.nio.ByteBufferWriteBuffer wb = awb[iBuffer + indx];
                                if (wb == null)
                                    {
                                    awb[iBuffer + indx] = wb = (com.tangosol.io.nio.ByteBufferWriteBuffer) pool.acquire();
                                    // limit the last ByteBuffer
                                    if (indx == cBuffers - 1)
                                        {
                                        int nRemainder = cbTotal % pool.getBufferSize();
                                        if (nRemainder > 0)
                                            {
                                            wb.getByteBuffer().limit(nRemainder);
                                            }
                                        }
                                    }
                                aBuffers[indx] = wb.getByteBuffer();
                                }
            
                            long cb = 0;
                            int  of = 0;
                            do
                                {
                                cb = channel.read(aBuffers, of, cBuffers);
                                if (cb == -1)
                                   {
                                   // EOF
                                   onEOS(key, connection);
                                   return;
                                   }
            
                                cbRead += cb;
                            
                                while (cBuffers > 0 && !aBuffers[of].hasRemaining())
                                    {
                                    of++;
                                    --cBuffers;
                                    ++iBuffer; // avoid need to try to read into already full buffers on next onRead call for large messages
                                    }
                                }
                            while (cBuffers > 0 && cb > 0);
            
                            if (cbRead == cbTotal)
                                {
                                // create a MessageBuffer wrapper for the Message data
                                TcpAcceptor.MessageBuffer mb = new TcpAcceptor.MessageBuffer();
                                mb.setBufferPool(pool);
                                mb.set_WriteBuffer(awb);
            
                                int cBuffer = awb.length;
                                if (cBuffer == 1)
                                    {
                                    mb.set_ReadBuffer(awb[0].getUnsafeReadBuffer());
                                    }
                                else
                                    {
                                    com.tangosol.io.nio.ByteBufferReadBuffer[] arb = new com.tangosol.io.nio.ByteBufferReadBuffer[cBuffer];
                                    for (int i = 0; i < cBuffer; ++i)
                                        {
                                        arb[i] = (com.tangosol.io.nio.ByteBufferReadBuffer) awb[i].getUnsafeReadBuffer();
                                        }
                                    mb.set_ReadBuffer(new com.tangosol.io.MultiBufferReadBuffer(arb));
                                    }
            
                                // dispatch the encoded Message
                                acceptor.receive(mb, connection);
            
                                // reset state
                                awb     = null;
                                iBuffer = 0;
                                cbTotal = 0;
                                cbRead  = 0;
                                nState  = TcpAcceptor.TcpConnection.IN_INITIAL;
                                }
                            else if (cbRead < cbTotal)
                                {
                                // the O/S buffer must be empty
                                _assert(iBuffer < awb.length);
                                }
                            else
                                {
                                _assert(false, "expected to read " + cbTotal + " bytes; "
                                        + "read " + cbRead + " bytes instead");
                                }
                            }
                        }
                    }
                while (bb.position() > 0); // we've read at least part of the next message
            
                // Note: we technically must have the above do/while loop in order to ensure that if the last message read is fully
                // contained within bb that we actually process it. Without the loop we'd not process it until the next time the socket
                // became read ready with some other message.
            
                // TODO: consider limiting the number of iterations here, we could get stuck only processing on connection. This isn't
                // that simple as to do it correctly we'd also need to ensure we didn't leave any fully read messages unprocessed either.
                // given that it would only be possible if we had an endless stream of messages consisting of 1..3 bytes it seems it
                // could only happen because of a deliberate attack and we aren't setup to defend against attacks anyway.
                }
            catch (SSLException e)
                {
                Socket socket = connection.getSocket();
                if (socket == null)
                    {
                    _trace("Exception receiving from peer: " + e, 2);
                    }
                else
                    {
                    _trace("Exception regarding peer " + socket.getRemoteSocketAddress()
                            + ": " + e, 2);
                    }
                connection.close(false, new ConnectionException(e, connection), false);
                }
            catch (Throwable e) // rather then Exception so we don't terminate the service due to a things like OOME
                {
                connection.close(false, new ConnectionException(e, connection), false);
                }
            finally
                {
                connection.setIncomingBytesRead(cbRead);
                connection.setIncomingBytesTotal(cbTotal);
                connection.setIncomingState(nState);
                connection.setIncomingWriteBufferArray(awb);
                connection.setIncomingWriteBufferIndex(iBuffer);
                }
            }
        
        /**
         * Called when a one or more SelectionKey objects have been selected.
        * 
        * @param setKey the Set of selected SelectionKey objects
         */
        protected void onSelect(java.util.Set setKey)
            {
            // import Component.Net.Extend.Util.TcpUtil;
            // import java.nio.channels.CancelledKeyException;
            // import java.nio.channels.SelectionKey;
            // import java.util.Iterator;
            
            for (Iterator iter = setKey.iterator(); iter.hasNext(); )
                {
                SelectionKey key = (SelectionKey) iter.next();
                iter.remove();
            
                // skip invalid keys
                if (!key.isValid())
                    {
                    continue;
                    }
            
                // compensate for Java NIO bug:
                // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6403933
                if (key.readyOps() == 0)
                    {
                    TcpAcceptor.TcpConnection connection = (TcpAcceptor.TcpConnection) key.attachment();
                    if (connection == null || !connection.isOpen())
                        {
                        TcpUtil.close(key.channel());
                        }
                    else
                        {
                        onEOS(key, connection);
                        }
                    continue;
                    }
            
                try
                    {
                    // handle new Connections
                    if (key.isAcceptable())
                        {
                        onAccept(key);
                        }
            
                    // handle reads
                    if (key.isReadable())
                        {
                        onRead(key);
                        }
            
                    // handle writes
                    if (key.isWritable())
                        {
                        onWrite(key);
                        }
                    }
                catch (CancelledKeyException e) {}
                }
            }
        
        // Declared at the super level
        /**
         * Event notification called when  the daemon's Thread is waiting for
        * work.
        * 
        * @see #run
         */
        protected void onWait()
                throws java.lang.InterruptedException
            {
            // all work is done in onNotify()
            return;
            }
        
        /**
         * Called when a SocketChannel has been selected for a write operation.
        * This method writes available data from the corresponding
        * TcpConnection to the selected SocketChannel.
        * 
        * @param key  the SelectionKey that has been selected for write
         */
        protected void onWrite(java.nio.channels.SelectionKey key)
            {
            // import Component.Util.Queue as com.tangosol.coherence.component.util.Queue;
            // import com.tangosol.io.MultiBufferWriteBuffer as com.tangosol.io.MultiBufferWriteBuffer;
            // import com.tangosol.io.nio.ByteBufferWriteBuffer as com.tangosol.io.nio.ByteBufferWriteBuffer;
            // import com.tangosol.net.messaging.ConnectionException;
            // import java.net.Socket;
            // import java.nio.ByteBuffer;
            // import java.nio.channels.SelectionKey;
            // import java.nio.channels.SocketChannel;
            // import javax.net.ssl.SSLException;
            
            if (!key.isValid())
                {
                return;
                }
            
            TcpAcceptor.TcpConnection connection = (TcpAcceptor.TcpConnection) key.attachment();
            if (connection == null || !connection.isOpen())
                {
                key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
                return;
                }
            
            TcpAcceptor      acceptor  = getAcceptor();
            SocketChannel    channel   = (SocketChannel) key.channel();
            TcpAcceptor.BufferPool      pool      = acceptor.getBufferPoolOut();
            ByteBuffer       bb        = connection.getOutgoingByteBuffer();
            int              cbTotal   = connection.getOutgoingBytesTotal();
            int              cbWritten = connection.getOutgoingBytesWritten();
            com.tangosol.io.MultiBufferWriteBuffer mwb       = connection.getOutgoingMessage();
            com.tangosol.coherence.component.util.Queue            queue     = connection.getOutgoingQueue();
            int              nState    = connection.getOutgoingState();
            com.tangosol.io.nio.ByteBufferWriteBuffer[]    awb       = connection.getOutgoingWriteBufferArray();
            int              iBuffer   = connection.getOutgoingWriteBufferIndex();
            long             cSent     = connection.getStatsSent();
            long             cbSent    = connection.getStatsBytesSent();
            
            try
                {
                while (true)
                    {
                    switch (nState)
                        {
                        default:
                            throw new IllegalStateException("unknown state: " + nState);
            
                        case TcpAcceptor.TcpConnection.OUT_INITIAL:
                            {
                            if (bb == null)
                                {
                                // allocate a ByteBuffer large enough to store any packed int
                                connection.setOutgoingByteBuffer(bb = ByteBuffer.allocate(5));
                                }
            
                            // get the next Message from the front of the outgoing queue
                            mwb = (com.tangosol.io.MultiBufferWriteBuffer) queue.removeNoWait();
                            if (mwb == null)
                                {
                                // the Connection has been flushed; unregister interest in OP_WRITE
                                key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
                                return;
                                }
            
                            cbTotal = mwb.length() - 5; // see TcpAcceptor#encodeMessage()
                            if (cbTotal <= 0)
                                {
                                // should never happen, but just in case...
                                continue;
                                }
            
                            // construct an array of outgoing com.tangosol.io.nio.ByteBufferWriteBuffer objects
                            int cBuffer = mwb.getBufferCount();
                            awb = new com.tangosol.io.nio.ByteBufferWriteBuffer[cBuffer];
                            for (int i = 0; i < cBuffer; ++i)
                                {
                                com.tangosol.io.nio.ByteBufferWriteBuffer wb = (com.tangosol.io.nio.ByteBufferWriteBuffer) mwb.getBuffer(i);
                                wb.getByteBuffer().flip();
                                awb[i] = wb;
                                }
            
                            // write the outgoing Message length
                            writeMessageLength(bb, cbTotal);
            
                            // the Connection is now prepared for write
                            nState = TcpAcceptor.TcpConnection.OUT_PREPARED;
                            }
            
                        case TcpAcceptor.TcpConnection.OUT_PREPARED:
                            {
                            int cb = bb.remaining();
                            int of = bb.capacity() - cb;
            
                            // write the outgoing Message length
                            ByteBuffer bbMsg = awb[0].getByteBuffer();
                            bbMsg.position(of);
                            bbMsg.put(bb);
                            bbMsg.position(of);
            
                            // adjust the total to take the Message length into account
                            cbTotal += cb;
                            cbSent  += of; // COH-2307
            
                            // clear the outgoing Message length ByteBuffer
                            bb.clear();
            
                            // the length of the Message is now encoded
                            nState = TcpAcceptor.TcpConnection.OUT_ENCODED;
                            }
            
                        case TcpAcceptor.TcpConnection.OUT_ENCODED:
                            {
                            // attempt to write the Message data
            
                            int        cBuffers   = awb.length - iBuffer;
                            ByteBuffer aBuffers[] = new ByteBuffer[cBuffers];
                            for (int indx = 0; indx < cBuffers; ++indx)
                                {
                                aBuffers[indx] = awb[iBuffer + indx].getByteBuffer();
                                }
                            long cbSum = 0;
                            long cb    = 0;
                            int  of    = 0;
                            do
                                {
                                cbSum += cb = (cBuffers == 1)
                                            ? channel.write(aBuffers[of])
                                            : channel.write(aBuffers, of, cBuffers);
                                while (cBuffers > 0 && !aBuffers[of].hasRemaining())
                                    {
                                    pool.release(awb[iBuffer]);
                                    awb[iBuffer++] = null;
                                    of++;
                                    --cBuffers;
                                    }
                                }
                            while (cBuffers > 0 && cb > 0);
            
                            if (cbSum == 0)
                                {
                                 // the O/S buffer must be full
                                return;
                                }
            
                            cbWritten += cbSum;
                            cbSent    += cbSum;
            
                            if (cbWritten == cbTotal)
                                {
                                // another message has been sent
                                ++cSent;
            
                                // reset state
                                mwb       = null;
                                awb       = null;
                                iBuffer   = 0;
                                cbTotal   = 0;
                                cbWritten = 0;
                                nState    = TcpAcceptor.TcpConnection.OUT_INITIAL;
                                }
                            else if (cbWritten < cbTotal)
                                {
                                // the O/S buffer must be full
                                _assert(iBuffer < awb.length);
                                return;
                                }
                            else
                                {
                                _assert(false, "expected to write " + cbTotal + " bytes; "
                                        + "wrote " + cbWritten + " bytes instead");
                                return;
                                }
                            }
                        }
                    }
                }
            catch (SSLException e)
                {
                Socket socket = connection.getSocket();
                if (socket == null)
                    {
                    _trace("Exception sending to peer: " + e, 2);
                    }
                else
                    {
                    _trace("Exception regarding peer " + socket.getRemoteSocketAddress()
                            + ": " + e, 2);
                    }
                connection.close(false, new ConnectionException(e, connection), false);
                }
            catch (Exception e)
                {
                connection.close(false, new ConnectionException(e, connection), false);
                }
            finally
                {
                connection.setOutgoingBytesTotal(cbTotal);
                connection.setOutgoingBytesWritten(cbWritten);
                connection.setOutgoingMessage(mwb);
                connection.setOutgoingState(nState);
                connection.setOutgoingWriteBufferArray(awb); 
                connection.setOutgoingWriteBufferIndex(iBuffer);
                connection.setStatsSent(cSent);
                connection.setStatsBytesSent(cbSent);
                }
            }
        
        /**
         * Read a message length from the given ByteBuffer as a packed integer
        * value.
        * 
        * @param bb  the ByteBuffer to read from
        * 
        * @return the non-negative message length read from the ByteBuffer or
        * -1 if the ByteBuffer does not contain a complete packed integer value
        * 
        * @throws IllegalArgumentException if the ByteBuffer contains a
        * non-positive packed integer value
         */
        protected static int readMessageLength(java.nio.ByteBuffer bb)
            {
            int nPos = bb.position();
            if (nPos == 0)
                {
                return -1;
                }
            
            int b = bb.get(0) & 0xFF; // readUnsignedByte()
            if ((b & 0x40) != 0)
                {
                throw new IllegalArgumentException("Received a message with a negative length");
                }
            
            int cb    = b & 0x3F; // only 6 bits of data in first byte
            int i     = 1;
            int cBits = 6;
            
            while ((b & 0x80) != 0)
                {
                if (cBits > 31)  // invalid length
                    {
                    cb = -1;
                    break;
                    }
                
                if (i == nPos)   // incomplete
                    {
                    return -1;
                    }
            
                b      = bb.get(i++) & 0xFF; // readUnsignedByte()
                cb    |= ((b & 0x7F) << cBits);
                cBits += 7;
                }
            
            // prepare the buffer for reading any additional data
            bb.limit(bb.position());
            bb.position(i);
            
            if (cb <= 0)
                {
                throw new IllegalArgumentException("Received a message with an invalid length");
                }
            
            return cb;
            }
        
        /**
         * Release all TcpConnections in the TcpAcceptor's
        * ConnectionReleaseQueue.
         */
        protected void releaseConnections()
            {
            // import Component.Net.Extend.Util.TcpUtil;
            // import Component.Util.Queue as com.tangosol.coherence.component.util.Queue;
            // import com.tangosol.io.MultiBufferWriteBuffer as com.tangosol.io.MultiBufferWriteBuffer;
            // import com.tangosol.io.WriteBuffer;
            // import java.nio.ByteBuffer;
            
            TcpAcceptor acceptor = getAcceptor();
            TcpAcceptor.BufferPool poolIn   = acceptor.getBufferPoolIn();
            TcpAcceptor.BufferPool poolOut  = acceptor.getBufferPoolOut();
            com.tangosol.coherence.component.util.Queue       queue    = acceptor.getConnectionReleaseQueue();
            
            for (TcpAcceptor.TcpConnection connection = (TcpAcceptor.TcpConnection) queue.removeNoWait();
                 connection != null;
                 connection = (TcpAcceptor.TcpConnection) queue.removeNoWait())
                {
                TcpUtil.cancel(connection.getSelectionKey());
                TcpUtil.close(connection.getSocket());
                TcpUtil.close(connection.getSocketChannel());
            
                // release incoming WriteBuffers
                WriteBuffer[] awb = connection.getIncomingWriteBufferArray();
                if (awb != null)
                    {
                    for (int i = 0, c = awb.length; i < c; ++i)
                        {
                        WriteBuffer wb = awb[i];
                        if (wb != null)
                            {
                            poolIn.release(wb);
                            }
                        }
                    }
            
                // release outgoing WriteBuffers
                awb = connection.getOutgoingWriteBufferArray();
                if (awb != null)
                    {
                    for (int i = 0, c = awb.length; i < c; ++i)
                        {
                        WriteBuffer wb = awb[i];
                        if (wb != null)
                            {
                            poolOut.release(wb);
                            }
                        }
                    }
            
                // release outgoing Messages
                com.tangosol.coherence.component.util.Queue queueOut = connection.getOutgoingQueue();
                for (com.tangosol.io.MultiBufferWriteBuffer mwb = (com.tangosol.io.MultiBufferWriteBuffer) queueOut.removeNoWait();
                     mwb != null;
                     mwb = (com.tangosol.io.MultiBufferWriteBuffer) queueOut.removeNoWait())
                    {
                    for (int i = 0, c = mwb.getBufferCount(); i < c; ++i)
                        {
                        poolOut.release(mwb.getBuffer(i));
                        }
                    }
            
                if (_isTraceEnabled(6) && connection.getId() != null)
                    {
                    _trace("Released: " + connection, 6);
                    }
                }
            }
        
        // Accessor for the property "Acceptor"
        /**
         * Setter for property Acceptor.<p>
        * A direct reference to the TcpAcceptor module.
         */
        protected void setAcceptor(TcpAcceptor acceptor)
            {
            __m_Acceptor = acceptor;
            }
        
        // Accessor for the property "Selector"
        /**
         * Setter for property Selector.<p>
        * The Selector used to select from the various SelectableChannel
        * objects created by this TcpProcessor.
         */
        protected void setSelector(java.nio.channels.Selector selector)
            {
            __m_Selector = selector;
            }
        
        // Accessor for the property "ServerSocket"
        /**
         * Setter for property ServerSocket.<p>
        * The ServerSocket used by this TcpProcessor to accept Socket
        * connections.
         */
        protected void setServerSocket(java.net.ServerSocket socket)
            {
            __m_ServerSocket = socket;
            }
        
        // Accessor for the property "ServerSocketChannel"
        /**
         * Setter for property ServerSocketChannel.<p>
        * The ServerSocketChannel used by this TcpProcessor to create its
        * ServerSocket.
         */
        protected void setServerSocketChannel(java.nio.channels.ServerSocketChannel channel)
            {
            __m_ServerSocketChannel = channel;
            }
        
        /**
         * Transfer cb bytes starting at pos from bufSrc into bufDst and slide
        * the remaing contents (up to bufSrc.limit) to the zero position of
        * bufSrc.
         */
        public static void slide(java.nio.ByteBuffer bufSrc, java.nio.ByteBuffer bufDst, int cb)
            {
            int nLimit = bufSrc.limit();
            
            bufSrc.limit(bufSrc.position() + cb);
            bufDst.put(bufSrc);
            bufSrc.limit(nLimit);
            
            // slide any remaining content to the front as it is for the next message
            int nPos = 0;
            while (bufSrc.hasRemaining())
                {
                bufSrc.put(nPos++, bufSrc.get());
                }
            bufSrc.position(nPos).limit(bufSrc.capacity()); // for next socket read
            }
        
        // Declared at the super level
        /**
         * Stops the daemon thread associated with this component.
         */
        public void stop()
            {
            if (isStarted())
                {
                setExiting(true);
            
                Thread thread = getThread();
                if (thread != null && thread != Thread.currentThread())
                    {
                    wakeup();
                    }
                }
            }
        
        /**
         * Wake up the TcpProcessor from any blocking selection operation.
         */
        public void wakeup()
            {
            // import java.nio.channels.Selector;
            
            Selector selector = getSelector();
            if (selector != null)
                {
                selector.wakeup();
                }
            }
        
        /**
         * Write the specified Message length to the given ByteBuffer as a
        * packed integer value.
        * 
        * @param bb  the ByteBuffer to write to
        * @param cb  the size of the Message in bytes; must be non-negative
         */
        protected static void writeMessageLength(java.nio.ByteBuffer bb, int cb)
            {
            _assert(cb >= 0);
            
            // first byte contains a sign bit and 6 data bits
            int b = (byte) (cb & 0x3F);
            cb >>>= 6;
            
            while (cb != 0)
                {
                b |= 0x80; // bit 8 is a continuation bit
                bb.put((byte) b);
            
                b = (cb & 0x7F);
                cb >>>= 7;
                }
            
            // remaining byte
            bb.put((byte) b);
            
            // prepare the buffer for reading the message length
            bb.flip();
            }
        }
    }
