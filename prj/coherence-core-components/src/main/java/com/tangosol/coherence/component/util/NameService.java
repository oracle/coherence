
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.util.NameService

package com.tangosol.coherence.component.util;

import com.tangosol.coherence.component.net.Cluster;
import com.tangosol.coherence.component.net.extend.connection.TcpConnection;
import com.tangosol.coherence.component.net.extend.protocol.NameServiceProtocol;
import com.tangosol.coherence.component.net.extend.proxy.serviceProxy.NameServiceProxy;
import com.oracle.coherence.common.net.InetAddresses;
import com.oracle.coherence.common.net.InetSocketAddress32;
import com.tangosol.coherence.component.net.memberSet.actualMemberSet.ServiceMemberSet;
import com.tangosol.config.expression.NullParameterResolver;
import com.tangosol.internal.net.service.extend.DefaultNameServiceDependencies;
import com.tangosol.internal.net.service.extend.LegacyXmlNameServiceHelper;
import com.tangosol.internal.net.service.extend.NameServiceDependencies;
import com.tangosol.internal.net.service.peer.acceptor.DefaultNSTcpAcceptorDependencies;
import com.tangosol.internal.net.service.peer.acceptor.DefaultTcpAcceptorDependencies;
import com.tangosol.internal.net.service.peer.acceptor.TcpAcceptorDependencies;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.SocketAddressProvider;
import com.tangosol.net.internal.NameServicePofContext;
import com.tangosol.net.internal.WrapperSocketAddressProvider;
import com.tangosol.util.SafeHashSet;
import java.net.ServerSocket;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.IntPredicate;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NamingException;

/**
 * The NameService is a Service that provides a registration and lookup
 * mechanism similar to JNDI for resources available in the Cluster.
 * 
 * For example, a ProxyService will register itself with the NameService when
 * it is started via bind(), and Coherence*Extend clients can obtain the
 * ConnectionAcceptor listening address for that ProxyService via
 * lookup(ProxyServiceName).
 * 
 * The NameService uses a ConnectionAcceptor to accept and process incoming
 * requests from non-clustered clients. The ConnectionAcceptor is started
 * immediately after the NameService successfully starts and is shutdown upon
 * service termination.
 * 
 * During ConnectionAcceptor configuration, the NameService registers the
 * following Protocols:
 * 
 * (1) NameService Protocol
 * 
 * Additionally, a NameServiceProxy is registered as a Receiver for the
 * NameService Protocol.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class NameService
        extends    com.tangosol.coherence.component.Util
        implements com.oracle.coherence.common.base.Disposable,
                   com.tangosol.io.SerializerFactory,
                   com.tangosol.net.NameService,
                   com.tangosol.net.NameService.LookupCallback
    {
    // ---- Fields declarations ----
    
    /**
     * Property Acceptor
     *
     * ConnectionAcceptor used by the NameService to accept connections from
     * non-clustered Service clients (Stubs).
     */
    private NameService.TcpAcceptor __m_Acceptor;
    
    /**
     * Property BinderMap
     *
     * Map<Channel, Set<String>> A map of channel's to their associated
     * bindings.  When a channel is closed its bindings are to be automatically
     * removed.
     */
    private java.util.concurrent.ConcurrentHashMap __m_BinderMap;
    
    /**
     * Property Cluster
     *
     */
    private com.tangosol.net.Cluster __m_Cluster;
    
    /**
     * Property ContextClassLoader
     *
     * @see com.tangosol.io.ClassLoaderAware
     */
    private ClassLoader __m_ContextClassLoader;
    
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
     * Property Directory
     *
     * Map<String, Object> of Objects inserted via bind(), retrieved via
     * lookup() and removed via unbind().
     */
    private transient java.util.Map __m_Directory;
    
    /**
     * Property LookupCallbacks
     *
     * List<NameService.LookupCallback> of callbacks to be invoked during
     * lookup() when the name is not found in the Directory.
     */
    private transient java.util.List __m_LookupCallbacks;
    
    /**
     * Property NameServiceProxy
     *
     * The cluster side portion (Proxy) of the client-to-cluster NameService
     * Adapter.
     */
    private com.tangosol.coherence.component.net.extend.proxy.serviceProxy.NameServiceProxy __m_NameServiceProxy;
    
    /**
     * Property OperationalContext
     *
     * The OperationalContext for this Service.
     */
    private com.tangosol.net.OperationalContext __m_OperationalContext;
    
    /**
     * Property ServiceName
     *
     * The name of this Service.
     */
    private String __m_ServiceName;
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
        __mapChildren.put("RequestContext", NameService.RequestContext.get_CLASS());
        }
    
    // Default constructor
    public NameService()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public NameService(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            setBinderMap(new java.util.concurrent.ConcurrentHashMap());
            setDirectory(new java.util.concurrent.ConcurrentHashMap());
            setLookupCallbacks(new com.tangosol.util.SafeLinkedList());
            }
        catch (java.lang.Exception e)
            {
            // re-throw as a runtime exception
            throw new com.tangosol.util.WrapperException(e);
            }
        
        // containment initialization: children
        _addChild(new NameService.TcpAcceptor("TcpAcceptor", this, true), "TcpAcceptor");
        
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
        return new com.tangosol.coherence.component.util.NameService();
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
            clz = Class.forName("com.tangosol.coherence/component/util/NameService".replace('/', '.'));
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
    /**
     * Register a LookupCallback to be used to perform lookups on names that are
    * not bound to the NameService's directory. If more than one LookupCallback
    * is registered, they are called in the order in which they are registered
    * with the NameService.
    * 
    * @param callback  the LookupCallback to register
     */
    public void addLookupCallback(com.tangosol.net.NameService.LookupCallback callback)
        {
        getLookupCallbacks().add(callback);
        }
    
    // From interface: com.tangosol.net.NameService
    public void addMemberListener(com.tangosol.net.MemberListener l)
        {
        throw new UnsupportedOperationException();
        }
    
    // From interface: com.tangosol.net.NameService
    public void addServiceListener(com.tangosol.util.ServiceListener l)
        {
        throw new UnsupportedOperationException();
        }
    
    // From interface: com.tangosol.net.NameService
    /**
     * Binds a name to an object.
    * 
    * @param sName  the name to bind; may not be empty
    * @param obj    the object to bind; possibly null
    * 
    * @throws NameAlreadyBoundException if name is already bound
    * @throws NamingException if a naming exception is encountered
     */
    public void bind(String sName, Object o)
            throws javax.naming.NamingException
        {
        bind(sName, o, /*chan*/ null);
        }
    
    /**
     * Binds a name to an object.
    * 
    * @param sName  the name to bind; may not be empty
    * @param obj        the object to bind; possibly null
    * @param chan     the associated channel, if non-null the resource will be
    * automatically unbound when the channel is closed
    * 
    * @throws NameAlreadyBoundException if name is already bound
    * @throws NamingException if a naming exception is encountered
     */
    public void bind(String sName, Object o, com.tangosol.net.messaging.Channel chan)
            throws javax.naming.NameAlreadyBoundException,
                   javax.naming.NamingException
        {
        // import Component.Net.Cluster;
        // import Component.Net.Extend.Connection.TcpConnection;
        // import com.tangosol.net.NameService$Resolvable as com.tangosol.net.NameService.Resolvable;
        // import com.tangosol.util.SafeHashSet;
        // import com.oracle.coherence.common.net.InetAddresses;
        // import java.util.Map;
        // import java.util.Set;
        // import javax.naming.NameAlreadyBoundException;
        
        if (chan != null)
            {
            // interprocess registrations have special handling
            Cluster cluster = (Cluster) getCluster();
            if (!InetAddresses.isLocalAddress(((TcpConnection) chan.getConnection()).getSocket().getInetAddress()))
                {
                throw new UnsupportedOperationException("non-local bind attempt");
                }
            else if (!cluster.isClusterPortSharingEnabled())
                {
                throw new UnsupportedOperationException("cluster port sharing is not supported");
                }
            else if (o instanceof com.tangosol.net.NameService.Resolvable)
                {
                throw new UnsupportedOperationException("remote com.tangosol.net.NameService.Resolvable bind is not supported");
                }
            }
            
        if (getDirectory().putIfAbsent(sName, o) == null)
            {
            if (chan != null)
                {
                // interprocess registrations have special handling
                Map mapBind  = getBinderMap();
                Set setNames = (Set) mapBind.get(chan);
                if (setNames == null)
                    {
                    setNames = new SafeHashSet();
                    mapBind.put(chan, setNames);
                    }
                setNames.add(sName);
                }
            }
        else
            {
            throw new NameAlreadyBoundException(sName);
            }
        }
    
    /**
     * Create a new Default dependencies object by copying the supplied
    * dependencies.  Each class or component that uses dependencies implements
    * a Default dependencies class which provides the clone functionality.  
    * The dependency injection design pattern requires every component in the
    * component hierarchy to implement clone, producing their variant of the
    * depencies interface.
    * 
    * @return DefaultNameServiceDependencies  the cloned dependencies
     */
    protected com.tangosol.internal.net.service.extend.DefaultNameServiceDependencies cloneDependencies(com.tangosol.net.ServiceDependencies deps)
        {
        // import com.tangosol.internal.net.service.extend.DefaultNameServiceDependencies;
        // import com.tangosol.internal.net.service.extend.NameServiceDependencies;
        
        return new DefaultNameServiceDependencies((NameServiceDependencies) deps);
        }
    
    // From interface: com.tangosol.net.NameService
    public void configure(com.tangosol.run.xml.XmlElement xml)
        {
        // import com.tangosol.internal.net.service.extend.DefaultNameServiceDependencies;
        // import com.tangosol.internal.net.service.extend.LegacyXmlNameServiceHelper;
        
        setDependencies(LegacyXmlNameServiceHelper.fromXml(xml,
            new DefaultNameServiceDependencies(), getOperationalContext(), getContextClassLoader()));
        }
    
    // From interface: com.tangosol.io.SerializerFactory
    /**
     * Custom SerializerFactory which always returns a NameServicePofContext.
     */
    public com.tangosol.io.Serializer createSerializer(ClassLoader loader)
        {
        return getSerializer();
        }
    
    // From interface: com.oracle.coherence.common.base.Disposable
    /**
     * Shut down the NameService.
    * 
    * See @com.oracle.common.Disposable#dispose()
     */
    public void dispose()
        {
        shutdown();
        }
    
    // Accessor for the property "Acceptor"
    /**
     * Getter for property Acceptor.<p>
    * ConnectionAcceptor used by the NameService to accept connections from
    * non-clustered Service clients (Stubs).
     */
    public NameService.TcpAcceptor getAcceptor()
        {
        return __m_Acceptor;
        }
    
    // Accessor for the property "BinderMap"
    /**
     * Getter for property BinderMap.<p>
    * Map<Channel, Set<String>> A map of channel's to their associated
    * bindings.  When a channel is closed its bindings are to be automatically
    * removed.
     */
    public java.util.concurrent.ConcurrentHashMap getBinderMap()
        {
        return __m_BinderMap;
        }
    
    // From interface: com.tangosol.net.NameService
    // Accessor for the property "Cluster"
    /**
     * Get the running Cluster for this node.
    * 
    * @return the running Cluster for this node
     */
    public com.tangosol.net.Cluster getCluster()
        {
        return __m_Cluster;
        }
    
    // From interface: com.tangosol.net.NameService
    // Accessor for the property "ContextClassLoader"
    /**
     * Getter for property ContextClassLoader.<p>
    * @see com.tangosol.io.ClassLoaderAware
     */
    public ClassLoader getContextClassLoader()
        {
        return __m_ContextClassLoader;
        }
    
    // From interface: com.tangosol.net.NameService
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
    
    // Accessor for the property "Directory"
    /**
     * Getter for property Directory.<p>
    * Map<String, Object> of Objects inserted via bind(), retrieved via
    * lookup() and removed via unbind().
     */
    public java.util.Map getDirectory()
        {
        return __m_Directory;
        }
    
    // From interface: com.tangosol.net.NameService
    public com.tangosol.net.ServiceInfo getInfo()
        {
        throw new UnsupportedOperationException();
        }
    
    // From interface: com.tangosol.net.NameService
    // Accessor for the property "LocalAddress"
    /**
     * Getter for property LocalAddress.<p>
    * The running or configured listening address.
     */
    public java.net.InetAddress getLocalAddress()
        {
        // import com.oracle.coherence.common.net.InetSocketAddress32;
        // import com.tangosol.config.expression.NullParameterResolver;
        // import com.tangosol.internal.net.service.extend.NameServiceDependencies;
        // import com.tangosol.internal.net.service.peer.acceptor.DefaultTcpAcceptorDependencies;
        // import com.tangosol.net.SocketAddressProvider;
        
        NameService.TcpAcceptor acceptor = getAcceptor();
        return ((InetSocketAddress32)
            (!acceptor.isRunning()
             ? ((SocketAddressProvider)
                  ((DefaultTcpAcceptorDependencies)
                      ((NameServiceDependencies) getDependencies()
                       ).getAcceptorDependencies()
                   ).getLocalAddressProviderBuilder()
                    .realize(new NullParameterResolver(), getContextClassLoader(), null)
               ).getNextAddress()
             : acceptor.getLocalAddress())
            ).getAddress();
        }
    
    // Accessor for the property "LookupCallbacks"
    /**
     * Getter for property LookupCallbacks.<p>
    * List<NameService.LookupCallback> of callbacks to be invoked during
    * lookup() when the name is not found in the Directory.
     */
    public java.util.List getLookupCallbacks()
        {
        return __m_LookupCallbacks;
        }
    
    // Accessor for the property "NameServiceProxy"
    /**
     * Getter for property NameServiceProxy.<p>
    * The cluster side portion (Proxy) of the client-to-cluster NameService
    * Adapter.
     */
    public com.tangosol.coherence.component.net.extend.proxy.serviceProxy.NameServiceProxy getNameServiceProxy()
        {
        return __m_NameServiceProxy;
        }
    
    // Accessor for the property "OperationalContext"
    /**
     * Getter for property OperationalContext.<p>
    * The OperationalContext for this Service.
     */
    public com.tangosol.net.OperationalContext getOperationalContext()
        {
        return __m_OperationalContext;
        }
    
    // From interface: com.tangosol.net.NameService
    public com.tangosol.util.ResourceRegistry getResourceRegistry()
        {
        return null;
        }
    
    // From interface: com.tangosol.net.NameService
    public com.tangosol.io.Serializer getSerializer()
        {
        // import com.tangosol.net.internal.NameServicePofContext;
        
        return NameServicePofContext.INSTANCE;
        }
    
    // Accessor for the property "ServiceName"
    /**
     * Getter for property ServiceName.<p>
    * The name of this Service.
     */
    public String getServiceName()
        {
        return __m_ServiceName;
        }
    
    // From interface: com.tangosol.net.NameService
    public Object getUserContext()
        {
        throw new UnsupportedOperationException();
        }
    
    // From interface: com.tangosol.net.NameService
    public boolean isRunning()
        {
        return getAcceptor().isRunning();
        }
    
    // From interface: com.tangosol.net.NameService
    public boolean isSuspended()
        {
        return false;
        }
    
    // From interface: com.tangosol.net.NameService
    /**
     * Retrieves the named object.
    * 
    * @param sName  the name of the object to look up
    * 
    * @return the object bound to sName
    * 
    * @throws NamingException if a naming exception is encountered
     */
    public Object lookup(String sName)
            throws javax.naming.NamingException
        {
        NameService.RequestContext context = new NameService.RequestContext();
        context.setMember(getCluster().getLocalMember());
        
        return lookup(sName, context);
        }
    
    // From interface: com.tangosol.net.NameService$LookupCallback
    public Object lookup(String sName, com.tangosol.net.Cluster cluster, com.tangosol.net.NameService.RequestContext ctx)
            throws javax.naming.NamingException
        {
        // import java.util.Arrays;
        
        String sPrefix = "NameService/"; // NS names are services; or service scoped, so we use the ClusterService name
        if (sName != null && sName.startsWith(sPrefix))
            {
            sName = sName.substring(sPrefix.length());
            String sString = "string/";
            if (sName.startsWith(sString))
                {        
                Object o = lookup(sName.substring(sString.length()), ctx);
                return o == null
                    ? null
                    : o instanceof Object[]
                        ? Arrays.toString((Object[]) o)
                        : o.toString();
                }
            else if (sName.equals("directory"))
                {
                return getDirectory().keySet();
                }
            else if (sName.equals("localInetAddress"))
                {
                // note the caller knows the port and if we included the Acceptor.Processor.ServerSocket.getLocalAddress it would
                // have the sub-port encoded in it which is not meant to be externally highlighted
                return getLocalAddress();
                }
            }
        
        return null;
        }
    
    public Object lookup(String sName, com.tangosol.net.NameService.RequestContext context)
            throws javax.naming.NamingException
        {
        // import com.tangosol.net.NameService$LookupCallback as com.tangosol.net.NameService.LookupCallback;
        // import com.tangosol.net.NameService$Resolvable as com.tangosol.net.NameService.Resolvable;
        // import java.util.Iterator;
        // import java.util.Map;
        // import javax.naming.NamingException;
        
        if (sName == null)
            {
            throw new NamingException("lookup name must be specified");
            }
        
        Map    map     = getDirectory();
        Object oResult = getDirectory().get(sName);
        
        // allow for a null value to be returned for a key in the directory
        if (oResult == null && !map.containsKey(sName))
            {
            for (Iterator iter = getLookupCallbacks().iterator(); iter.hasNext(); )
                {
                oResult = ((com.tangosol.net.NameService.LookupCallback) iter.next()).lookup(sName, getCluster(), context);
        
                // unlike the directory lookup, a null here means not found
                if (oResult != null)
                    {
                    break;
                    }
                }
            }
        
        return oResult instanceof com.tangosol.net.NameService.Resolvable
            ? ((com.tangosol.net.NameService.Resolvable) oResult).resolve(context)
            : oResult;
        }
    
    /**
     * Process the NameService dependencies.
    * 
    * See @Component.Util.Daemon.QueueProcessor.Service#onDependencies()
     */
    protected void onDependencies(com.tangosol.internal.net.service.extend.NameServiceDependencies deps)
        {
        // import Component.Net.Extend.Protocol.NameServiceProtocol;
        // import Component.Net.Extend.Proxy.ServiceProxy.NameServiceProxy;
        // import com.oracle.coherence.common.internal.net.MultiplexedSocketProvider$WellKnownSubPorts as com.oracle.coherence.common.internal.net.MultiplexedSocketProvider.WellKnownSubPorts;
        // import com.tangosol.net.internal.WrapperSocketAddressProvider;
        // import com.tangosol.internal.net.service.extend.NameServiceDependencies;
        
        // ensure that we've been configured with an OperationalContext
        if (getOperationalContext() == null)
            {
            throw new IllegalStateException("missing required OperationalContext");
            }
        
        // create, set and configure the proxies
        NameServiceProxy nameServiceProxy = new NameServiceProxy();
        setNameServiceProxy(nameServiceProxy);
        nameServiceProxy.setNameService(this);
        
        // configure the Acceptor
        NameService.TcpAcceptor acceptor = getAcceptor();
        acceptor.setOperationalContext(getOperationalContext());
        acceptor.setDependencies(((NameServiceDependencies) getDependencies())
                .getAcceptorDependencies());
        acceptor.setServiceName(getServiceName() + ':' + acceptor.getServiceName());
        acceptor.setParentService(this);
        
        // add the NameService well known subport to the acceptor's listen port(s)
        acceptor.setLocalAddressProvider(new WrapperSocketAddressProvider(
                acceptor.getLocalAddressProvider(),
                com.oracle.coherence.common.internal.net.MultiplexedSocketProvider.WellKnownSubPorts.COHERENCE_NAME_SERVICE.getSubPort()));
        
        // configure the Acceptor Serializer (hardcoded to POF)
        acceptor.setSerializerFactory(this);
        
        // don't use any filters for NameService requests
        acceptor.setWrapperStreamFactoryList(null);
        
        // register all Protocols with the Acceptor
        acceptor.registerProtocol(NameServiceProtocol.getInstance());
        
        // register all Receivers with the Acceptor
        if (getNameServiceProxy().isEnabled())
            {
            acceptor.registerReceiver(getNameServiceProxy());
            }
        
        // set the ConnectionAcceptor ClassLoader
        acceptor.setContextClassLoader(getContextClassLoader());
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
        // import Component.Util.NameService$TcpAcceptor as NameService.TcpAcceptor;
        // import com.tangosol.net.NameService as com.tangosol.net.NameService;
        
        super.onInit();
        
        // set the default service name
        setServiceName(com.tangosol.net.NameService.NAME_DEFAULT);
        setAcceptor((NameService.TcpAcceptor) _findChild("TcpAcceptor"));
        addLookupCallback(this);
        }
    
    // From interface: com.tangosol.net.NameService
    public void removeMemberListener(com.tangosol.net.MemberListener l)
        {
        throw new UnsupportedOperationException();
        }
    
    // From interface: com.tangosol.net.NameService
    public void removeServiceListener(com.tangosol.util.ServiceListener l)
        {
        throw new UnsupportedOperationException();
        }
    
    // Accessor for the property "Acceptor"
    /**
     * Setter for property Acceptor.<p>
    * ConnectionAcceptor used by the NameService to accept connections from
    * non-clustered Service clients (Stubs).
     */
    protected void setAcceptor(NameService.TcpAcceptor acceptor)
        {
        __m_Acceptor = acceptor;
        }
    
    // Accessor for the property "BinderMap"
    /**
     * Setter for property BinderMap.<p>
    * Map<Channel, Set<String>> A map of channel's to their associated
    * bindings.  When a channel is closed its bindings are to be automatically
    * removed.
     */
    public void setBinderMap(java.util.concurrent.ConcurrentHashMap mapBinder)
        {
        __m_BinderMap = mapBinder;
        }
    
    // Accessor for the property "Cluster"
    /**
     * Setter for property Cluster.<p>
     */
    public void setCluster(com.tangosol.net.Cluster cluster)
        {
        __m_Cluster = cluster;
        }
    
    // From interface: com.tangosol.net.NameService
    // Accessor for the property "ContextClassLoader"
    /**
     * Setter for property ContextClassLoader.<p>
    * @see com.tangosol.io.ClassLoaderAware
     */
    public void setContextClassLoader(ClassLoader loader)
        {
        __m_ContextClassLoader = (loader);
        
        getAcceptor().setContextClassLoader(loader);
        }
    
    // From interface: com.tangosol.net.NameService
    // Accessor for the property "Dependencies"
    /**
     * Inject the Dependencies object into the component.  First clone the
    * dependencies, then validate the cloned copy.  Note that the validate
    * method may modify the cloned dependencies, so it is important to use the
    * cloned dependencies for all subsequent operations.  Once the dependencies
    * have been validated, call onDependencies so that each Componenet in the
    * class hierarchy can process the dependencies as needed.  
    * 
    * NOTE: This method is final and it is not intended that derived components
    * intercept the call to setDepedencies.  Instead they should hook in via
    * cloneDepedencies and onDependencies.
     */
    public void setDependencies(com.tangosol.net.ServiceDependencies deps)
        {
        // import com.tangosol.internal.net.service.extend.NameServiceDependencies;
        
        if (getDependencies() != null)
            {
            throw new IllegalStateException("Dependencies already set");
            }
        
        __m_Dependencies = (cloneDependencies(deps).validate());
        
        // use the cloned dependencies
        onDependencies((NameServiceDependencies) getDependencies());
        }
    
    // Accessor for the property "Directory"
    /**
     * Setter for property Directory.<p>
    * Map<String, Object> of Objects inserted via bind(), retrieved via
    * lookup() and removed via unbind().
     */
    protected void setDirectory(java.util.Map map)
        {
        __m_Directory = map;
        }
    
    // Accessor for the property "LookupCallbacks"
    /**
     * Setter for property LookupCallbacks.<p>
    * List<NameService.LookupCallback> of callbacks to be invoked during
    * lookup() when the name is not found in the Directory.
     */
    public void setLookupCallbacks(java.util.List listCallbacks)
        {
        __m_LookupCallbacks = listCallbacks;
        }
    
    // Accessor for the property "NameServiceProxy"
    /**
     * Setter for property NameServiceProxy.<p>
    * The cluster side portion (Proxy) of the client-to-cluster NameService
    * Adapter.
     */
    protected void setNameServiceProxy(com.tangosol.coherence.component.net.extend.proxy.serviceProxy.NameServiceProxy proxy)
        {
        __m_NameServiceProxy = proxy;
        }
    
    // Accessor for the property "OperationalContext"
    /**
     * Setter for property OperationalContext.<p>
    * The OperationalContext for this Service.
     */
    public void setOperationalContext(com.tangosol.net.OperationalContext ctx)
        {
        __m_OperationalContext = ctx;
        }
    
    // Accessor for the property "ServiceName"
    /**
     * Setter for property ServiceName.<p>
    * The name of this Service.
     */
    public void setServiceName(String sName)
        {
        __m_ServiceName = sName;
        }
    
    // From interface: com.tangosol.net.NameService
    public void setUserContext(Object oCtx)
        {
        throw new UnsupportedOperationException();
        }
    
    // From interface: com.tangosol.net.NameService
    /**
     * Stop the NameService.
     */
    public void shutdown()
        {
        // perform a graceful shutdown of the Acceptor
        try
            {
            getAcceptor().shutdown();
            }
        catch (Exception e)
            {
            // ignore
            }
        
        stop();
        }
    
    // From interface: com.tangosol.net.NameService
    /**
     * Start the NameService.
     */
    public void start()
        {
        getAcceptor().start();
        }
    
    // From interface: com.tangosol.net.NameService
    /**
     * Hard-stop the NameService. Use shutdown() for normal  termination.
     */
    public void stop()
        {
        // force the ConnectionAcceptor to stop if it is still running
        try
            {
            getAcceptor().stop();
            }
        catch (Exception e)
            {
            // ignore
            }
        }
    
    // Declared at the super level
    public String toString()
        {
        // import java.net.ServerSocket;
        
        ServerSocket socket = getAcceptor().getProcessor().getServerSocket();
        return get_Name() + "(" + (socket == null ? (Object) getAcceptor().getLocalAddressProvider() : (Object) socket)  + ")";
        }
    
    // From interface: com.tangosol.net.NameService
    /**
     * Unbinds the named object.
    * 
    * @param sName  the name to bind; may not be empty
    * 
    * @throws NamingException if a naming exception is encountered
     */
    public void unbind(String sName)
            throws javax.naming.NamingException
        {
        getDirectory().remove(sName);
        }

    @Override
    public boolean isVersionCompatible(int nMajor, int nMinor, int nMicro, int nPatchSet, int nPatch)
        {
        int nEncoded = ServiceMemberSet.encodeVersion(nMajor, nMinor, nMicro, nPatchSet, nPatch);
        return CacheFactory.VERSION_ENCODED >= nEncoded;
        }

    @Override
    public boolean isVersionCompatible(int nYear, int nMonth, int nPatch)
        {
        int nEncoded = ServiceMemberSet.encodeVersion(nYear, nMonth, nPatch);
        return CacheFactory.VERSION_ENCODED >= nEncoded;
        }

    @Override
    public boolean isVersionCompatible(int nVersion)
        {
        return CacheFactory.VERSION_ENCODED >= nVersion;
        }

    @Override
    public boolean isVersionCompatible(IntPredicate predicate)
        {
        return predicate.test(CacheFactory.VERSION_ENCODED);
        }

    @Override
    public int getMinimumServiceVersion()
        {
        return CacheFactory.VERSION_ENCODED;
        }

    // ---- class: com.tangosol.coherence.component.util.NameService$RequestContext
    
    /**
     * Stores information about a NameService request.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class RequestContext
            extends    com.tangosol.coherence.component.Util
            implements com.tangosol.net.NameService.RequestContext
        {
        // ---- Fields declarations ----
        
        /**
         * Property AcceptAddress
         *
         * the {@link InetAddress} that the NameService received the request
         * on. May return <tt>null</tt> if the request is local.
         */
        private java.net.InetAddress __m_AcceptAddress;
        
        /**
         * Property Member
         *
         * The client {@link Member} that sent the request. May return
         * <tt>null</tt> unknown or if the request is local.
         */
        private com.tangosol.net.Member __m_Member;
        
        /**
         * Property SourceAddress
         *
         * The {@link InetAddress} that the request originated from. May return
         * <tt>null</tt> if the request is local.
         */
        private java.net.InetAddress __m_SourceAddress;
        
        // Default constructor
        public RequestContext()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public RequestContext(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.util.NameService.RequestContext();
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
                clz = Class.forName("com.tangosol.coherence/component/util/NameService$RequestContext".replace('/', '.'));
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
        
        // From interface: com.tangosol.net.NameService$RequestContext
        // Accessor for the property "AcceptAddress"
        /**
         * Getter for property AcceptAddress.<p>
        * the {@link InetAddress} that the NameService received the request on.
        * May return <tt>null</tt> if the request is local.
         */
        public java.net.InetAddress getAcceptAddress()
            {
            return __m_AcceptAddress;
            }
        
        // From interface: com.tangosol.net.NameService$RequestContext
        // Accessor for the property "Member"
        /**
         * Getter for property Member.<p>
        * The client {@link Member} that sent the request. May return
        * <tt>null</tt> unknown or if the request is local.
         */
        public com.tangosol.net.Member getMember()
            {
            return __m_Member;
            }
        
        // From interface: com.tangosol.net.NameService$RequestContext
        // Accessor for the property "SourceAddress"
        /**
         * Getter for property SourceAddress.<p>
        * The {@link InetAddress} that the request originated from. May return
        * <tt>null</tt> if the request is local.
         */
        public java.net.InetAddress getSourceAddress()
            {
            return __m_SourceAddress;
            }
        
        // Accessor for the property "AcceptAddress"
        /**
         * Setter for property AcceptAddress.<p>
        * the {@link InetAddress} that the NameService received the request on.
        * May return <tt>null</tt> if the request is local.
         */
        public void setAcceptAddress(java.net.InetAddress addressAccept)
            {
            __m_AcceptAddress = addressAccept;
            }
        
        // Accessor for the property "Member"
        /**
         * Setter for property Member.<p>
        * The client {@link Member} that sent the request. May return
        * <tt>null</tt> unknown or if the request is local.
         */
        public void setMember(com.tangosol.net.Member member)
            {
            __m_Member = member;
            }
        
        // Accessor for the property "SourceAddress"
        /**
         * Setter for property SourceAddress.<p>
        * The {@link InetAddress} that the request originated from. May return
        * <tt>null</tt> if the request is local.
         */
        public void setSourceAddress(java.net.InetAddress addressSource)
            {
            __m_SourceAddress = addressSource;
            }
        }

    // ---- class: com.tangosol.coherence.component.util.NameService$TcpAcceptor
    
    /**
     * A ConnectionAcceptor implementation that accepts Connections over TCP/IP.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class TcpAcceptor
            extends    com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.TcpAcceptor
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
            __mapChildren.put("BufferPool", NameService.TcpAcceptor.BufferPool.get_CLASS());
            __mapChildren.put("DispatchEvent", NameService.TcpAcceptor.DispatchEvent.get_CLASS());
            __mapChildren.put("MessageBuffer", NameService.TcpAcceptor.MessageBuffer.get_CLASS());
            __mapChildren.put("MessageFactory", NameService.TcpAcceptor.MessageFactory.get_CLASS());
            __mapChildren.put("Queue", NameService.TcpAcceptor.Queue.get_CLASS());
            __mapChildren.put("TcpConnection", NameService.TcpAcceptor.TcpConnection.get_CLASS());
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
            _addChild(new NameService.TcpAcceptor.DaemonPool("DaemonPool", this, true), "DaemonPool");
            _addChild(new NameService.TcpAcceptor.EventDispatcher("EventDispatcher", this, true), "EventDispatcher");
            _addChild(new Daemon.Guard("Guard", this, true), "Guard");
            _addChild(new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.Protocol("Protocol", this, true), "Protocol");
            _addChild(new NameService.TcpAcceptor.TcpProcessor("TcpProcessor", this, true), "TcpProcessor");
            
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
            return new com.tangosol.coherence.component.util.NameService.TcpAcceptor();
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
                clz = Class.forName("com.tangosol.coherence/component/util/NameService$TcpAcceptor".replace('/', '.'));
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
         * Validates a token in order to establish a user's identity. 
        * NameService doesn't need to concern about security context.  So, just
        * return null for Subject.
        * 
        * @param oToken  an identity assertion, a statement that asserts an
        * identity
        * 
        * @return null
         */
        public javax.security.auth.Subject assertIdentityToken(Object oToken)
            {
            return null;
            }
        
        // Declared at the super level
        /**
         * Create a new Default dependencies object by copying the supplies
        * dependencies.  Each class or component that uses dependencies
        * implements a Default dependencies class which provides the clone
        * functionality.   The dependency injection design pattern requires
        * every component in the component hierarchy to implement clone,
        * producing their variant of the dependencies interface.
        * 
        * @return the cloned dependencies
         */
        protected com.tangosol.internal.net.service.DefaultServiceDependencies cloneDependencies(com.tangosol.net.ServiceDependencies deps)
            {
            // import com.tangosol.internal.net.service.peer.acceptor.DefaultNSTcpAcceptorDependencies;
            // import com.tangosol.internal.net.service.peer.acceptor.TcpAcceptorDependencies;
            
            return new DefaultNSTcpAcceptorDependencies((TcpAcceptorDependencies) deps);
            }
        
        // Declared at the super level
        /**
         * Don't deserialize the identity token bytes since NameService does not
        * check security context.  So, just return null for identity token.
        * 
        * @param abToken  the identity token
        * 
        * @return null
         */
        public Object deserializeIdentityToken(byte[] abToken)
            {
            return null;
            }
        
        // Declared at the super level
        /**
         * Serialize an identity token.  Always return null for NameService
        * because it doesn't need to be concerned about security context.
        * 
        * @param oToken  the identity token object to serialize
        * 
        * @return null
         */
        public byte[] serializeIdentityToken(Object oToken)
            {
            return null;
            }
        
        // Declared at the super level
        /**
         * Starts the daemon thread associated with this component. If the
        * thread is already starting or has started, invoking this method has
        * no effect.
        * 
        * Synchronization is used here to verify that the start of the thread
        * occurs; the lock is obtained before the thread is started, and the
        * daemon thread notifies back that it has started from the run() method.
         */
        public synchronized void start()
            {
            // import Component.Net.Cluster;
            
            // guard the acceptor now that we're going to start
            ((Cluster) ((NameService) get_Module()).getCluster()).getClusterService().guard(getGuardable());
            
            super.start();
            }

        // ---- class: com.tangosol.coherence.component.util.NameService$TcpAcceptor$BufferPool
        
        /**
         * A GrowablePool of ByteBufferWriteBuffer objects. The size and type
         * of pooled objects are controlled by the following properties:
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
                extends    com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.TcpAcceptor.BufferPool
            {
            // ---- Fields declarations ----
            
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
                return new com.tangosol.coherence.component.util.NameService.TcpAcceptor.BufferPool();
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
                    clz = Class.forName("com.tangosol.coherence/component/util/NameService$TcpAcceptor$BufferPool".replace('/', '.'));
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
            }

        // ---- class: com.tangosol.coherence.component.util.NameService$TcpAcceptor$DaemonPool
        
        /**
         * DaemonPool is a class thread pool implementation for processing
         * queued operations on one or more daemon threads.
         * 
         * The designable properties are:
         *     AutoStart
         *     DaemonCount
         * 
         * The simple API for the DaemonPool is:
         *     public void start()
         *     public boolean isStarted()
         *     public void add(Runnable task)
         *     public void stop()
         * 
         * The advanced API for the DaemonPool is:
         *     DaemonCount property
         *     Daemons property
         *     Queues property
         *     ThreadGroup property
         * 
         * The DaemonPool is composed of two key components:
         * 
         * 1) An array of WorkSlot components that may or may not share Queues
         * with other WorkSlots. 
         * 
         * 2) An array of Daemon components feeding off the Queues. This
         * collection is accessed by the DaemonCount and Daemons properties,
         * and is managed by the DaemonCount mutator.
         */
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class DaemonPool
                extends    com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.Acceptor.DaemonPool
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
                __mapChildren.put("Daemon", NameService.TcpAcceptor.DaemonPool.Daemon.get_CLASS());
                __mapChildren.put("ResizeTask", NameService.TcpAcceptor.DaemonPool.ResizeTask.get_CLASS());
                __mapChildren.put("ScheduleTask", NameService.TcpAcceptor.DaemonPool.ScheduleTask.get_CLASS());
                __mapChildren.put("StartTask", NameService.TcpAcceptor.DaemonPool.StartTask.get_CLASS());
                __mapChildren.put("StopTask", NameService.TcpAcceptor.DaemonPool.StopTask.get_CLASS());
                __mapChildren.put("WorkSlot", NameService.TcpAcceptor.DaemonPool.WorkSlot.get_CLASS());
                __mapChildren.put("WrapperTask", NameService.TcpAcceptor.DaemonPool.WrapperTask.get_CLASS());
                }
            
            // Default constructor
            public DaemonPool()
                {
                this(null, null, true);
                }
            
            // Initializing constructor
            public DaemonPool(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                    setAbandonThreshold(8);
                    setDaemonCountMax(2147483647);
                    setDaemonCountMin(1);
                    setScheduledTasks(new java.util.HashSet());
                    setStatsTaskAddCount(new java.util.concurrent.atomic.AtomicLong());
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
                return new com.tangosol.coherence.component.util.NameService.TcpAcceptor.DaemonPool();
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
                    clz = Class.forName("com.tangosol.coherence/component/util/NameService$TcpAcceptor$DaemonPool".replace('/', '.'));
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

            // ---- class: com.tangosol.coherence.component.util.NameService$TcpAcceptor$DaemonPool$Daemon
            
            /**
             * The prototypical Daemon thread component that will belong to the
             * DaemonPool. An instance of this component is created for each
             * thread in the pool.
             */
            @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
            public static class Daemon
                    extends    com.tangosol.coherence.component.util.daemon.queueProcessor.Service.DaemonPool.Daemon
                {
                // ---- Fields declarations ----
                
                // Default constructor
                public Daemon()
                    {
                    this(null, null, true);
                    }
                
                // Initializing constructor
                public Daemon(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                        setThreadName("Worker");
                        }
                    catch (java.lang.Exception e)
                        {
                        // re-throw as a runtime exception
                        throw new com.tangosol.util.WrapperException(e);
                        }
                    
                    // containment initialization: children
                    _addChild(new NameService.TcpAcceptor.DaemonPool.Daemon.Guard("Guard", this, true), "Guard");
                    
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
                    return new com.tangosol.coherence.component.util.NameService.TcpAcceptor.DaemonPool.Daemon();
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
                        clz = Class.forName("com.tangosol.coherence/component/util/NameService$TcpAcceptor$DaemonPool$Daemon".replace('/', '.'));
                        }
                    catch (ClassNotFoundException e)
                        {
                        throw new NoClassDefFoundError(e.getMessage());
                        }
                    return clz;
                    }
                
                //++ getter for autogen property _Module
                /**
                 * This is an auto-generated method that returns the global
                * [design time] parent component.
                * 
                * Note: the class generator will ignore any custom
                * implementation for this behavior.
                 */
                private com.tangosol.coherence.Component get_Module()
                    {
                    return this.get_Parent().get_Parent().get_Parent();
                    }

                // ---- class: com.tangosol.coherence.component.util.NameService$TcpAcceptor$DaemonPool$Daemon$Guard
                
                /**
                 * Guard provides the Guardable interface implementation for
                 * the Daemon.
                 */
                @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
                public static class Guard
                        extends    com.tangosol.coherence.component.util.daemon.queueProcessor.Service.DaemonPool.Daemon.Guard
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
                        __mapChildren.put("Abandon", NameService.TcpAcceptor.DaemonPool.Daemon.Guard.Abandon.get_CLASS());
                        }
                    
                    // Default constructor
                    public Guard()
                        {
                        this(null, null, true);
                        }
                    
                    // Initializing constructor
                    public Guard(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                        return new com.tangosol.coherence.component.util.NameService.TcpAcceptor.DaemonPool.Daemon.Guard();
                        }
                    
                    //++ getter for static property _CLASS
                    /**
                     * Getter for property _CLASS.<p>
                    * Property with auto-generated accessor that returns the
                    * Class object for a given component.
                     */
                    public static Class get_CLASS()
                        {
                        Class clz;
                        try
                            {
                            clz = Class.forName("com.tangosol.coherence/component/util/NameService$TcpAcceptor$DaemonPool$Daemon$Guard".replace('/', '.'));
                            }
                        catch (ClassNotFoundException e)
                            {
                            throw new NoClassDefFoundError(e.getMessage());
                            }
                        return clz;
                        }
                    
                    //++ getter for autogen property _Module
                    /**
                     * This is an auto-generated method that returns the global
                    * [design time] parent component.
                    * 
                    * Note: the class generator will ignore any custom
                    * implementation for this behavior.
                     */
                    private com.tangosol.coherence.Component get_Module()
                        {
                        return this.get_Parent().get_Parent().get_Parent().get_Parent();
                        }
                    
                    //++ getter for autogen property _ChildClasses
                    /**
                     * This is an auto-generated method that returns the map of
                    * design time [static] children.
                    * 
                    * Note: the class generator will ignore any custom
                    * implementation for this behavior.
                     */
                    protected java.util.Map get_ChildClasses()
                        {
                        return __mapChildren;
                        }

                    // ---- class: com.tangosol.coherence.component.util.NameService$TcpAcceptor$DaemonPool$Daemon$Guard$Abandon
                    
                    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
                    public static class Abandon
                            extends    com.tangosol.coherence.component.util.daemon.queueProcessor.Service.DaemonPool.Daemon.Guard.Abandon
                        {
                        // ---- Fields declarations ----
                        
                        // Default constructor
                        public Abandon()
                            {
                            this(null, null, true);
                            }
                        
                        // Initializing constructor
                        public Abandon(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                            return new com.tangosol.coherence.component.util.NameService.TcpAcceptor.DaemonPool.Daemon.Guard.Abandon();
                            }
                        
                        //++ getter for static property _CLASS
                        /**
                         * Getter for property _CLASS.<p>
                        * Property with auto-generated accessor that returns
                        * the Class object for a given component.
                         */
                        public static Class get_CLASS()
                            {
                            Class clz;
                            try
                                {
                                clz = Class.forName("com.tangosol.coherence/component/util/NameService$TcpAcceptor$DaemonPool$Daemon$Guard$Abandon".replace('/', '.'));
                                }
                            catch (ClassNotFoundException e)
                                {
                                throw new NoClassDefFoundError(e.getMessage());
                                }
                            return clz;
                            }
                        
                        //++ getter for autogen property _Module
                        /**
                         * This is an auto-generated method that returns the
                        * global [design time] parent component.
                        * 
                        * Note: the class generator will ignore any custom
                        * implementation for this behavior.
                         */
                        private com.tangosol.coherence.Component get_Module()
                            {
                            return this.get_Parent().get_Parent().get_Parent().get_Parent().get_Parent();
                            }
                        }
                    }
                }

            // ---- class: com.tangosol.coherence.component.util.NameService$TcpAcceptor$DaemonPool$ResizeTask
            
            /**
             * Runnable periodic task used to implement the dynamic resizing
             * algorithm.
             */
            @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
            public static class ResizeTask
                    extends    com.tangosol.coherence.component.util.daemon.queueProcessor.Service.DaemonPool.ResizeTask
                {
                // ---- Fields declarations ----
                
                // Default constructor
                public ResizeTask()
                    {
                    this(null, null, true);
                    }
                
                // Initializing constructor
                public ResizeTask(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                    return new com.tangosol.coherence.component.util.NameService.TcpAcceptor.DaemonPool.ResizeTask();
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
                        clz = Class.forName("com.tangosol.coherence/component/util/NameService$TcpAcceptor$DaemonPool$ResizeTask".replace('/', '.'));
                        }
                    catch (ClassNotFoundException e)
                        {
                        throw new NoClassDefFoundError(e.getMessage());
                        }
                    return clz;
                    }
                
                //++ getter for autogen property _Module
                /**
                 * This is an auto-generated method that returns the global
                * [design time] parent component.
                * 
                * Note: the class generator will ignore any custom
                * implementation for this behavior.
                 */
                private com.tangosol.coherence.Component get_Module()
                    {
                    return this.get_Parent().get_Parent().get_Parent();
                    }
                }

            // ---- class: com.tangosol.coherence.component.util.NameService$TcpAcceptor$DaemonPool$ScheduleTask
            
            /**
             * Runnable task that is used to schedule a task to be added to the
             * DaemonPool.
             */
            @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
            public static class ScheduleTask
                    extends    com.tangosol.coherence.component.util.daemon.queueProcessor.Service.DaemonPool.ScheduleTask
                {
                // ---- Fields declarations ----
                
                // Default constructor
                public ScheduleTask()
                    {
                    this(null, null, true);
                    }
                
                // Initializing constructor
                public ScheduleTask(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                    return new com.tangosol.coherence.component.util.NameService.TcpAcceptor.DaemonPool.ScheduleTask();
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
                        clz = Class.forName("com.tangosol.coherence/component/util/NameService$TcpAcceptor$DaemonPool$ScheduleTask".replace('/', '.'));
                        }
                    catch (ClassNotFoundException e)
                        {
                        throw new NoClassDefFoundError(e.getMessage());
                        }
                    return clz;
                    }
                
                //++ getter for autogen property _Module
                /**
                 * This is an auto-generated method that returns the global
                * [design time] parent component.
                * 
                * Note: the class generator will ignore any custom
                * implementation for this behavior.
                 */
                private com.tangosol.coherence.Component get_Module()
                    {
                    return this.get_Parent().get_Parent().get_Parent();
                    }
                }

            // ---- class: com.tangosol.coherence.component.util.NameService$TcpAcceptor$DaemonPool$StartTask
            
            /**
             * Runnable pseudo-task that is used to start one and only one
             * daemon thread.
             */
            @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
            public static class StartTask
                    extends    com.tangosol.coherence.component.util.daemon.queueProcessor.Service.DaemonPool.StartTask
                {
                // ---- Fields declarations ----
                
                // Default constructor
                public StartTask()
                    {
                    this(null, null, true);
                    }
                
                // Initializing constructor
                public StartTask(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                    return new com.tangosol.coherence.component.util.NameService.TcpAcceptor.DaemonPool.StartTask();
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
                        clz = Class.forName("com.tangosol.coherence/component/util/NameService$TcpAcceptor$DaemonPool$StartTask".replace('/', '.'));
                        }
                    catch (ClassNotFoundException e)
                        {
                        throw new NoClassDefFoundError(e.getMessage());
                        }
                    return clz;
                    }
                
                //++ getter for autogen property _Module
                /**
                 * This is an auto-generated method that returns the global
                * [design time] parent component.
                * 
                * Note: the class generator will ignore any custom
                * implementation for this behavior.
                 */
                private com.tangosol.coherence.Component get_Module()
                    {
                    return this.get_Parent().get_Parent().get_Parent();
                    }
                }

            // ---- class: com.tangosol.coherence.component.util.NameService$TcpAcceptor$DaemonPool$StopTask
            
            /**
             * Runnable pseudo-task that is used to terminate one and only one
             * daemon thread.
             */
            @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
            public static class StopTask
                    extends    com.tangosol.coherence.component.util.daemon.queueProcessor.Service.DaemonPool.StopTask
                {
                // ---- Fields declarations ----
                
                // Default constructor
                public StopTask()
                    {
                    this(null, null, true);
                    }
                
                // Initializing constructor
                public StopTask(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                    return new com.tangosol.coherence.component.util.NameService.TcpAcceptor.DaemonPool.StopTask();
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
                        clz = Class.forName("com.tangosol.coherence/component/util/NameService$TcpAcceptor$DaemonPool$StopTask".replace('/', '.'));
                        }
                    catch (ClassNotFoundException e)
                        {
                        throw new NoClassDefFoundError(e.getMessage());
                        }
                    return clz;
                    }
                
                //++ getter for autogen property _Module
                /**
                 * This is an auto-generated method that returns the global
                * [design time] parent component.
                * 
                * Note: the class generator will ignore any custom
                * implementation for this behavior.
                 */
                private com.tangosol.coherence.Component get_Module()
                    {
                    return this.get_Parent().get_Parent().get_Parent();
                    }
                }

            // ---- class: com.tangosol.coherence.component.util.NameService$TcpAcceptor$DaemonPool$WorkSlot
            
            /**
             * To reduce the contention across the worker threads, all tasks
             * added to the DaemonPool are directed to one of the WorkSlots in
             * a way that respects the association between tasks. The total
             * number of slots is fixed and calculated based on the number of
             * processors. Depending on the number of daemon threads, different
             * slots may share the queues.
             */
            @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
            public static class WorkSlot
                    extends    com.tangosol.coherence.component.util.daemon.queueProcessor.Service.DaemonPool.WorkSlot
                {
                // ---- Fields declarations ----
                
                // Default constructor
                public WorkSlot()
                    {
                    this(null, null, true);
                    }
                
                // Initializing constructor
                public WorkSlot(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                        setIndex(-1);
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
                    return new com.tangosol.coherence.component.util.NameService.TcpAcceptor.DaemonPool.WorkSlot();
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
                        clz = Class.forName("com.tangosol.coherence/component/util/NameService$TcpAcceptor$DaemonPool$WorkSlot".replace('/', '.'));
                        }
                    catch (ClassNotFoundException e)
                        {
                        throw new NoClassDefFoundError(e.getMessage());
                        }
                    return clz;
                    }
                
                //++ getter for autogen property _Module
                /**
                 * This is an auto-generated method that returns the global
                * [design time] parent component.
                * 
                * Note: the class generator will ignore any custom
                * implementation for this behavior.
                 */
                private com.tangosol.coherence.Component get_Module()
                    {
                    return this.get_Parent().get_Parent().get_Parent();
                    }
                }

            // ---- class: com.tangosol.coherence.component.util.NameService$TcpAcceptor$DaemonPool$WrapperTask
            
            /**
             * A task that is used to wrap the actual tasks.
             */
            @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
            public static class WrapperTask
                    extends    com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.DaemonPool.WrapperTask
                {
                // ---- Fields declarations ----
                
                // Default constructor
                public WrapperTask()
                    {
                    this(null, null, true);
                    }
                
                // Initializing constructor
                public WrapperTask(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                    return new com.tangosol.coherence.component.util.NameService.TcpAcceptor.DaemonPool.WrapperTask();
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
                        clz = Class.forName("com.tangosol.coherence/component/util/NameService$TcpAcceptor$DaemonPool$WrapperTask".replace('/', '.'));
                        }
                    catch (ClassNotFoundException e)
                        {
                        throw new NoClassDefFoundError(e.getMessage());
                        }
                    return clz;
                    }
                
                //++ getter for autogen property _Module
                /**
                 * This is an auto-generated method that returns the global
                * [design time] parent component.
                * 
                * Note: the class generator will ignore any custom
                * implementation for this behavior.
                 */
                private com.tangosol.coherence.Component get_Module()
                    {
                    return this.get_Parent().get_Parent().get_Parent();
                    }
                }
            }

        // ---- class: com.tangosol.coherence.component.util.NameService$TcpAcceptor$DispatchEvent
        
        /**
         * Runnable event.
         */
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class DispatchEvent
                extends    com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.DispatchEvent
            {
            // ---- Fields declarations ----
            
            // Default constructor
            public DispatchEvent()
                {
                this(null, null, true);
                }
            
            // Initializing constructor
            public DispatchEvent(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                return new com.tangosol.coherence.component.util.NameService.TcpAcceptor.DispatchEvent();
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
                    clz = Class.forName("com.tangosol.coherence/component/util/NameService$TcpAcceptor$DispatchEvent".replace('/', '.'));
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
            }

        // ---- class: com.tangosol.coherence.component.util.NameService$TcpAcceptor$EventDispatcher
        
        /**
         * This is a Daemon component that waits for items to process from a
         * Queue. Whenever the Queue contains an item, the onNotify event
         * occurs. It is expected that sub-classes will process onNotify as
         * follows:
         * <pre><code>
         * Object o;
         * while ((o = getQueue().removeNoWait()) != null)
         *     {
         *     // process the item
         *     // ...
         *     }
         * </code></pre>
         * <p>
         * The Queue is used as the synchronization point for the daemon.
         */
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class EventDispatcher
                extends    com.tangosol.coherence.component.util.daemon.queueProcessor.Service.EventDispatcher
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
                __mapChildren.put("Queue", NameService.TcpAcceptor.EventDispatcher.Queue.get_CLASS());
                }
            
            // Default constructor
            public EventDispatcher()
                {
                this(null, null, true);
                }
            
            // Initializing constructor
            public EventDispatcher(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                    setCloggedCount(1024);
                    setCloggedDelay(32);
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
                _addChild(new com.tangosol.coherence.component.util.daemon.queueProcessor.Service.EventDispatcher.Guard("Guard", this, true), "Guard");
                
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
                return new com.tangosol.coherence.component.util.NameService.TcpAcceptor.EventDispatcher();
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
                    clz = Class.forName("com.tangosol.coherence/component/util/NameService$TcpAcceptor$EventDispatcher".replace('/', '.'));
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

            // ---- class: com.tangosol.coherence.component.util.NameService$TcpAcceptor$EventDispatcher$Queue
            
            /**
             * This is the Queue to which items that need to be processed are
             * added, and from which the daemon pulls items to process.
             */
            @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
            public static class Queue
                    extends    com.tangosol.coherence.component.util.daemon.queueProcessor.Service.EventDispatcher.Queue
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
                    __mapChildren.put("Iterator", NameService.TcpAcceptor.EventDispatcher.Queue.Iterator.get_CLASS());
                    }
                
                // Default constructor
                public Queue()
                    {
                    this(null, null, true);
                    }
                
                // Initializing constructor
                public Queue(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                        setElementList(new com.tangosol.util.RecyclingLinkedList());
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
                    return new com.tangosol.coherence.component.util.NameService.TcpAcceptor.EventDispatcher.Queue();
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
                        clz = Class.forName("com.tangosol.coherence/component/util/NameService$TcpAcceptor$EventDispatcher$Queue".replace('/', '.'));
                        }
                    catch (ClassNotFoundException e)
                        {
                        throw new NoClassDefFoundError(e.getMessage());
                        }
                    return clz;
                    }
                
                //++ getter for autogen property _Module
                /**
                 * This is an auto-generated method that returns the global
                * [design time] parent component.
                * 
                * Note: the class generator will ignore any custom
                * implementation for this behavior.
                 */
                private com.tangosol.coherence.Component get_Module()
                    {
                    return this.get_Parent().get_Parent().get_Parent();
                    }
                
                //++ getter for autogen property _ChildClasses
                /**
                 * This is an auto-generated method that returns the map of
                * design time [static] children.
                * 
                * Note: the class generator will ignore any custom
                * implementation for this behavior.
                 */
                protected java.util.Map get_ChildClasses()
                    {
                    return __mapChildren;
                    }

                // ---- class: com.tangosol.coherence.component.util.NameService$TcpAcceptor$EventDispatcher$Queue$Iterator
                
                /**
                 * Iterator of a snapshot of the List object that backs the
                 * Queue. Supports remove(). Uses the Queue as the monitor if
                 * any synchronization is required.
                 */
                @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
                public static class Iterator
                        extends    com.tangosol.coherence.component.util.daemon.queueProcessor.Service.EventDispatcher.Queue.Iterator
                    {
                    // ---- Fields declarations ----
                    
                    // Default constructor
                    public Iterator()
                        {
                        this(null, null, true);
                        }
                    
                    // Initializing constructor
                    public Iterator(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                        return new com.tangosol.coherence.component.util.NameService.TcpAcceptor.EventDispatcher.Queue.Iterator();
                        }
                    
                    //++ getter for static property _CLASS
                    /**
                     * Getter for property _CLASS.<p>
                    * Property with auto-generated accessor that returns the
                    * Class object for a given component.
                     */
                    public static Class get_CLASS()
                        {
                        Class clz;
                        try
                            {
                            clz = Class.forName("com.tangosol.coherence/component/util/NameService$TcpAcceptor$EventDispatcher$Queue$Iterator".replace('/', '.'));
                            }
                        catch (ClassNotFoundException e)
                            {
                            throw new NoClassDefFoundError(e.getMessage());
                            }
                        return clz;
                        }
                    
                    //++ getter for autogen property _Module
                    /**
                     * This is an auto-generated method that returns the global
                    * [design time] parent component.
                    * 
                    * Note: the class generator will ignore any custom
                    * implementation for this behavior.
                     */
                    private com.tangosol.coherence.Component get_Module()
                        {
                        return this.get_Parent().get_Parent().get_Parent().get_Parent();
                        }
                    }
                }
            }

        // ---- class: com.tangosol.coherence.component.util.NameService$TcpAcceptor$MessageBuffer
        
        /**
         * ReadBuffer implementation that holds an encoded Message. This
         * component delegates all ReadBuffer operations to a wrapped
         * ReadBuffer and holds a reference to a BufferPool and an array of
         * WriteBuffer objects that the wrapped ReadBuffer is based upon so
         * that they can be released.
         */
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class MessageBuffer
                extends    com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.TcpAcceptor.MessageBuffer
            {
            // ---- Fields declarations ----
            
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
                return new com.tangosol.coherence.component.util.NameService.TcpAcceptor.MessageBuffer();
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
                    clz = Class.forName("com.tangosol.coherence/component/util/NameService$TcpAcceptor$MessageBuffer".replace('/', '.'));
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
            }

        // ---- class: com.tangosol.coherence.component.util.NameService$TcpAcceptor$MessageFactory
        
        /**
         * MessageFactory implementation for version 2 of the
         * MessagingProtocol. This MessageFactory contains Message classes
         * necessary to manage the lifecycle of Connections and Channels.
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
         * messages used a common Response type with type identifier 0. Since
         * the first Response that a client expects to receive is an
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
                extends    com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.TcpAcceptor.MessageFactory
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
                __mapChildren.put("AcceptChannel", NameService.TcpAcceptor.MessageFactory.AcceptChannel.get_CLASS());
                __mapChildren.put("AcceptChannelRequest", NameService.TcpAcceptor.MessageFactory.AcceptChannelRequest.get_CLASS());
                __mapChildren.put("AcceptChannelResponse", NameService.TcpAcceptor.MessageFactory.AcceptChannelResponse.get_CLASS());
                __mapChildren.put("CloseChannel", NameService.TcpAcceptor.MessageFactory.CloseChannel.get_CLASS());
                __mapChildren.put("CloseConnection", NameService.TcpAcceptor.MessageFactory.CloseConnection.get_CLASS());
                __mapChildren.put("CreateChannel", NameService.TcpAcceptor.MessageFactory.CreateChannel.get_CLASS());
                __mapChildren.put("EncodedMessage", NameService.TcpAcceptor.MessageFactory.EncodedMessage.get_CLASS());
                __mapChildren.put("NotifyChannelClosed", NameService.TcpAcceptor.MessageFactory.NotifyChannelClosed.get_CLASS());
                __mapChildren.put("NotifyConnectionClosed", NameService.TcpAcceptor.MessageFactory.NotifyConnectionClosed.get_CLASS());
                __mapChildren.put("NotifyShutdown", NameService.TcpAcceptor.MessageFactory.NotifyShutdown.get_CLASS());
                __mapChildren.put("NotifyStartup", NameService.TcpAcceptor.MessageFactory.NotifyStartup.get_CLASS());
                __mapChildren.put("OpenChannel", NameService.TcpAcceptor.MessageFactory.OpenChannel.get_CLASS());
                __mapChildren.put("OpenChannelRequest", NameService.TcpAcceptor.MessageFactory.OpenChannelRequest.get_CLASS());
                __mapChildren.put("OpenChannelResponse", NameService.TcpAcceptor.MessageFactory.OpenChannelResponse.get_CLASS());
                __mapChildren.put("OpenConnection", NameService.TcpAcceptor.MessageFactory.OpenConnection.get_CLASS());
                __mapChildren.put("OpenConnectionRequest", NameService.TcpAcceptor.MessageFactory.OpenConnectionRequest.get_CLASS());
                __mapChildren.put("OpenConnectionResponse", NameService.TcpAcceptor.MessageFactory.OpenConnectionResponse.get_CLASS());
                __mapChildren.put("PingRequest", NameService.TcpAcceptor.MessageFactory.PingRequest.get_CLASS());
                __mapChildren.put("PingResponse", NameService.TcpAcceptor.MessageFactory.PingResponse.get_CLASS());
                __mapChildren.put("Response", NameService.TcpAcceptor.MessageFactory.Response.get_CLASS());
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
                return new com.tangosol.coherence.component.util.NameService.TcpAcceptor.MessageFactory();
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
                    clz = Class.forName("com.tangosol.coherence/component/util/NameService$TcpAcceptor$MessageFactory".replace('/', '.'));
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

            // ---- class: com.tangosol.coherence.component.util.NameService$TcpAcceptor$MessageFactory$AcceptChannel
            
            /**
             * Internal Request used to accept a Channel.
             */
            @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
            public static class AcceptChannel
                    extends    com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.AcceptChannel
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
                    __mapChildren.put("Status", NameService.TcpAcceptor.MessageFactory.AcceptChannel.Status.get_CLASS());
                    }
                
                // Default constructor
                public AcceptChannel()
                    {
                    this(null, null, true);
                    }
                
                // Initializing constructor
                public AcceptChannel(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                    return new com.tangosol.coherence.component.util.NameService.TcpAcceptor.MessageFactory.AcceptChannel();
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
                        clz = Class.forName("com.tangosol.coherence/component/util/NameService$TcpAcceptor$MessageFactory$AcceptChannel".replace('/', '.'));
                        }
                    catch (ClassNotFoundException e)
                        {
                        throw new NoClassDefFoundError(e.getMessage());
                        }
                    return clz;
                    }
                
                //++ getter for autogen property _Module
                /**
                 * This is an auto-generated method that returns the global
                * [design time] parent component.
                * 
                * Note: the class generator will ignore any custom
                * implementation for this behavior.
                 */
                private com.tangosol.coherence.Component get_Module()
                    {
                    return this.get_Parent().get_Parent().get_Parent();
                    }
                
                //++ getter for autogen property _ChildClasses
                /**
                 * This is an auto-generated method that returns the map of
                * design time [static] children.
                * 
                * Note: the class generator will ignore any custom
                * implementation for this behavior.
                 */
                protected java.util.Map get_ChildClasses()
                    {
                    return __mapChildren;
                    }

                // ---- class: com.tangosol.coherence.component.util.NameService$TcpAcceptor$MessageFactory$AcceptChannel$Status
                
                /**
                 * Implementation of the Request$Status interface.
                 */
                @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
                public static class Status
                        extends    com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.AcceptChannel.Status
                    {
                    // ---- Fields declarations ----
                    
                    // Default constructor
                    public Status()
                        {
                        this(null, null, true);
                        }
                    
                    // Initializing constructor
                    public Status(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                        return new com.tangosol.coherence.component.util.NameService.TcpAcceptor.MessageFactory.AcceptChannel.Status();
                        }
                    
                    //++ getter for static property _CLASS
                    /**
                     * Getter for property _CLASS.<p>
                    * Property with auto-generated accessor that returns the
                    * Class object for a given component.
                     */
                    public static Class get_CLASS()
                        {
                        Class clz;
                        try
                            {
                            clz = Class.forName("com.tangosol.coherence/component/util/NameService$TcpAcceptor$MessageFactory$AcceptChannel$Status".replace('/', '.'));
                            }
                        catch (ClassNotFoundException e)
                            {
                            throw new NoClassDefFoundError(e.getMessage());
                            }
                        return clz;
                        }
                    
                    //++ getter for autogen property _Module
                    /**
                     * This is an auto-generated method that returns the global
                    * [design time] parent component.
                    * 
                    * Note: the class generator will ignore any custom
                    * implementation for this behavior.
                     */
                    private com.tangosol.coherence.Component get_Module()
                        {
                        return this.get_Parent().get_Parent().get_Parent().get_Parent();
                        }
                    }
                }

            // ---- class: com.tangosol.coherence.component.util.NameService$TcpAcceptor$MessageFactory$AcceptChannelRequest
            
            /**
             * This Request is used to accept a Channel that was spawned by a
             * peer.
             */
            @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
            public static class AcceptChannelRequest
                    extends    com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.AcceptChannelRequest
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
                    __mapChildren.put("Status", NameService.TcpAcceptor.MessageFactory.AcceptChannelRequest.Status.get_CLASS());
                    }
                
                // Default constructor
                public AcceptChannelRequest()
                    {
                    this(null, null, true);
                    }
                
                // Initializing constructor
                public AcceptChannelRequest(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                    return new com.tangosol.coherence.component.util.NameService.TcpAcceptor.MessageFactory.AcceptChannelRequest();
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
                        clz = Class.forName("com.tangosol.coherence/component/util/NameService$TcpAcceptor$MessageFactory$AcceptChannelRequest".replace('/', '.'));
                        }
                    catch (ClassNotFoundException e)
                        {
                        throw new NoClassDefFoundError(e.getMessage());
                        }
                    return clz;
                    }
                
                //++ getter for autogen property _Module
                /**
                 * This is an auto-generated method that returns the global
                * [design time] parent component.
                * 
                * Note: the class generator will ignore any custom
                * implementation for this behavior.
                 */
                private com.tangosol.coherence.Component get_Module()
                    {
                    return this.get_Parent().get_Parent().get_Parent();
                    }
                
                //++ getter for autogen property _ChildClasses
                /**
                 * This is an auto-generated method that returns the map of
                * design time [static] children.
                * 
                * Note: the class generator will ignore any custom
                * implementation for this behavior.
                 */
                protected java.util.Map get_ChildClasses()
                    {
                    return __mapChildren;
                    }

                // ---- class: com.tangosol.coherence.component.util.NameService$TcpAcceptor$MessageFactory$AcceptChannelRequest$Status
                
                /**
                 * Implementation of the Request$Status interface.
                 */
                @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
                public static class Status
                        extends    com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.AcceptChannelRequest.Status
                    {
                    // ---- Fields declarations ----
                    
                    // Default constructor
                    public Status()
                        {
                        this(null, null, true);
                        }
                    
                    // Initializing constructor
                    public Status(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                        return new com.tangosol.coherence.component.util.NameService.TcpAcceptor.MessageFactory.AcceptChannelRequest.Status();
                        }
                    
                    //++ getter for static property _CLASS
                    /**
                     * Getter for property _CLASS.<p>
                    * Property with auto-generated accessor that returns the
                    * Class object for a given component.
                     */
                    public static Class get_CLASS()
                        {
                        Class clz;
                        try
                            {
                            clz = Class.forName("com.tangosol.coherence/component/util/NameService$TcpAcceptor$MessageFactory$AcceptChannelRequest$Status".replace('/', '.'));
                            }
                        catch (ClassNotFoundException e)
                            {
                            throw new NoClassDefFoundError(e.getMessage());
                            }
                        return clz;
                        }
                    
                    //++ getter for autogen property _Module
                    /**
                     * This is an auto-generated method that returns the global
                    * [design time] parent component.
                    * 
                    * Note: the class generator will ignore any custom
                    * implementation for this behavior.
                     */
                    private com.tangosol.coherence.Component get_Module()
                        {
                        return this.get_Parent().get_Parent().get_Parent().get_Parent();
                        }
                    }
                }

            // ---- class: com.tangosol.coherence.component.util.NameService$TcpAcceptor$MessageFactory$AcceptChannelResponse
            
            /**
             * Response to an AcceptChannelRequest.
             */
            @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
            public static class AcceptChannelResponse
                    extends    com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.AcceptChannelResponse
                {
                // ---- Fields declarations ----
                
                // Default constructor
                public AcceptChannelResponse()
                    {
                    this(null, null, true);
                    }
                
                // Initializing constructor
                public AcceptChannelResponse(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                    return new com.tangosol.coherence.component.util.NameService.TcpAcceptor.MessageFactory.AcceptChannelResponse();
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
                        clz = Class.forName("com.tangosol.coherence/component/util/NameService$TcpAcceptor$MessageFactory$AcceptChannelResponse".replace('/', '.'));
                        }
                    catch (ClassNotFoundException e)
                        {
                        throw new NoClassDefFoundError(e.getMessage());
                        }
                    return clz;
                    }
                
                //++ getter for autogen property _Module
                /**
                 * This is an auto-generated method that returns the global
                * [design time] parent component.
                * 
                * Note: the class generator will ignore any custom
                * implementation for this behavior.
                 */
                private com.tangosol.coherence.Component get_Module()
                    {
                    return this.get_Parent().get_Parent().get_Parent();
                    }
                }

            // ---- class: com.tangosol.coherence.component.util.NameService$TcpAcceptor$MessageFactory$CloseChannel
            
            /**
             * Internal Request used to close a Channel.
             */
            @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
            public static class CloseChannel
                    extends    com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.CloseChannel
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
                    __mapChildren.put("Status", NameService.TcpAcceptor.MessageFactory.CloseChannel.Status.get_CLASS());
                    }
                
                // Default constructor
                public CloseChannel()
                    {
                    this(null, null, true);
                    }
                
                // Initializing constructor
                public CloseChannel(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                    return new com.tangosol.coherence.component.util.NameService.TcpAcceptor.MessageFactory.CloseChannel();
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
                        clz = Class.forName("com.tangosol.coherence/component/util/NameService$TcpAcceptor$MessageFactory$CloseChannel".replace('/', '.'));
                        }
                    catch (ClassNotFoundException e)
                        {
                        throw new NoClassDefFoundError(e.getMessage());
                        }
                    return clz;
                    }
                
                //++ getter for autogen property _Module
                /**
                 * This is an auto-generated method that returns the global
                * [design time] parent component.
                * 
                * Note: the class generator will ignore any custom
                * implementation for this behavior.
                 */
                private com.tangosol.coherence.Component get_Module()
                    {
                    return this.get_Parent().get_Parent().get_Parent();
                    }
                
                //++ getter for autogen property _ChildClasses
                /**
                 * This is an auto-generated method that returns the map of
                * design time [static] children.
                * 
                * Note: the class generator will ignore any custom
                * implementation for this behavior.
                 */
                protected java.util.Map get_ChildClasses()
                    {
                    return __mapChildren;
                    }

                // ---- class: com.tangosol.coherence.component.util.NameService$TcpAcceptor$MessageFactory$CloseChannel$Status
                
                /**
                 * Implementation of the Request$Status interface.
                 */
                @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
                public static class Status
                        extends    com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.CloseChannel.Status
                    {
                    // ---- Fields declarations ----
                    
                    // Default constructor
                    public Status()
                        {
                        this(null, null, true);
                        }
                    
                    // Initializing constructor
                    public Status(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                        return new com.tangosol.coherence.component.util.NameService.TcpAcceptor.MessageFactory.CloseChannel.Status();
                        }
                    
                    //++ getter for static property _CLASS
                    /**
                     * Getter for property _CLASS.<p>
                    * Property with auto-generated accessor that returns the
                    * Class object for a given component.
                     */
                    public static Class get_CLASS()
                        {
                        Class clz;
                        try
                            {
                            clz = Class.forName("com.tangosol.coherence/component/util/NameService$TcpAcceptor$MessageFactory$CloseChannel$Status".replace('/', '.'));
                            }
                        catch (ClassNotFoundException e)
                            {
                            throw new NoClassDefFoundError(e.getMessage());
                            }
                        return clz;
                        }
                    
                    //++ getter for autogen property _Module
                    /**
                     * This is an auto-generated method that returns the global
                    * [design time] parent component.
                    * 
                    * Note: the class generator will ignore any custom
                    * implementation for this behavior.
                     */
                    private com.tangosol.coherence.Component get_Module()
                        {
                        return this.get_Parent().get_Parent().get_Parent().get_Parent();
                        }
                    }
                }

            // ---- class: com.tangosol.coherence.component.util.NameService$TcpAcceptor$MessageFactory$CloseConnection
            
            /**
             * Internal Message used to close a Connection.
             */
            @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
            public static class CloseConnection
                    extends    com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.CloseConnection
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
                    __mapChildren.put("Status", NameService.TcpAcceptor.MessageFactory.CloseConnection.Status.get_CLASS());
                    }
                
                // Default constructor
                public CloseConnection()
                    {
                    this(null, null, true);
                    }
                
                // Initializing constructor
                public CloseConnection(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                    return new com.tangosol.coherence.component.util.NameService.TcpAcceptor.MessageFactory.CloseConnection();
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
                        clz = Class.forName("com.tangosol.coherence/component/util/NameService$TcpAcceptor$MessageFactory$CloseConnection".replace('/', '.'));
                        }
                    catch (ClassNotFoundException e)
                        {
                        throw new NoClassDefFoundError(e.getMessage());
                        }
                    return clz;
                    }
                
                //++ getter for autogen property _Module
                /**
                 * This is an auto-generated method that returns the global
                * [design time] parent component.
                * 
                * Note: the class generator will ignore any custom
                * implementation for this behavior.
                 */
                private com.tangosol.coherence.Component get_Module()
                    {
                    return this.get_Parent().get_Parent().get_Parent();
                    }
                
                //++ getter for autogen property _ChildClasses
                /**
                 * This is an auto-generated method that returns the map of
                * design time [static] children.
                * 
                * Note: the class generator will ignore any custom
                * implementation for this behavior.
                 */
                protected java.util.Map get_ChildClasses()
                    {
                    return __mapChildren;
                    }

                // ---- class: com.tangosol.coherence.component.util.NameService$TcpAcceptor$MessageFactory$CloseConnection$Status
                
                /**
                 * Implementation of the Request$Status interface.
                 */
                @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
                public static class Status
                        extends    com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.CloseConnection.Status
                    {
                    // ---- Fields declarations ----
                    
                    // Default constructor
                    public Status()
                        {
                        this(null, null, true);
                        }
                    
                    // Initializing constructor
                    public Status(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                        return new com.tangosol.coherence.component.util.NameService.TcpAcceptor.MessageFactory.CloseConnection.Status();
                        }
                    
                    //++ getter for static property _CLASS
                    /**
                     * Getter for property _CLASS.<p>
                    * Property with auto-generated accessor that returns the
                    * Class object for a given component.
                     */
                    public static Class get_CLASS()
                        {
                        Class clz;
                        try
                            {
                            clz = Class.forName("com.tangosol.coherence/component/util/NameService$TcpAcceptor$MessageFactory$CloseConnection$Status".replace('/', '.'));
                            }
                        catch (ClassNotFoundException e)
                            {
                            throw new NoClassDefFoundError(e.getMessage());
                            }
                        return clz;
                        }
                    
                    //++ getter for autogen property _Module
                    /**
                     * This is an auto-generated method that returns the global
                    * [design time] parent component.
                    * 
                    * Note: the class generator will ignore any custom
                    * implementation for this behavior.
                     */
                    private com.tangosol.coherence.Component get_Module()
                        {
                        return this.get_Parent().get_Parent().get_Parent().get_Parent();
                        }
                    }
                }

            // ---- class: com.tangosol.coherence.component.util.NameService$TcpAcceptor$MessageFactory$CreateChannel
            
            /**
             * Internal Request used to create a Channel.
             */
            @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
            public static class CreateChannel
                    extends    com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.CreateChannel
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
                    __mapChildren.put("Status", NameService.TcpAcceptor.MessageFactory.CreateChannel.Status.get_CLASS());
                    }
                
                // Default constructor
                public CreateChannel()
                    {
                    this(null, null, true);
                    }
                
                // Initializing constructor
                public CreateChannel(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                    return new com.tangosol.coherence.component.util.NameService.TcpAcceptor.MessageFactory.CreateChannel();
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
                        clz = Class.forName("com.tangosol.coherence/component/util/NameService$TcpAcceptor$MessageFactory$CreateChannel".replace('/', '.'));
                        }
                    catch (ClassNotFoundException e)
                        {
                        throw new NoClassDefFoundError(e.getMessage());
                        }
                    return clz;
                    }
                
                //++ getter for autogen property _Module
                /**
                 * This is an auto-generated method that returns the global
                * [design time] parent component.
                * 
                * Note: the class generator will ignore any custom
                * implementation for this behavior.
                 */
                private com.tangosol.coherence.Component get_Module()
                    {
                    return this.get_Parent().get_Parent().get_Parent();
                    }
                
                //++ getter for autogen property _ChildClasses
                /**
                 * This is an auto-generated method that returns the map of
                * design time [static] children.
                * 
                * Note: the class generator will ignore any custom
                * implementation for this behavior.
                 */
                protected java.util.Map get_ChildClasses()
                    {
                    return __mapChildren;
                    }

                // ---- class: com.tangosol.coherence.component.util.NameService$TcpAcceptor$MessageFactory$CreateChannel$Status
                
                /**
                 * Implementation of the Request$Status interface.
                 */
                @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
                public static class Status
                        extends    com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.CreateChannel.Status
                    {
                    // ---- Fields declarations ----
                    
                    // Default constructor
                    public Status()
                        {
                        this(null, null, true);
                        }
                    
                    // Initializing constructor
                    public Status(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                        return new com.tangosol.coherence.component.util.NameService.TcpAcceptor.MessageFactory.CreateChannel.Status();
                        }
                    
                    //++ getter for static property _CLASS
                    /**
                     * Getter for property _CLASS.<p>
                    * Property with auto-generated accessor that returns the
                    * Class object for a given component.
                     */
                    public static Class get_CLASS()
                        {
                        Class clz;
                        try
                            {
                            clz = Class.forName("com.tangosol.coherence/component/util/NameService$TcpAcceptor$MessageFactory$CreateChannel$Status".replace('/', '.'));
                            }
                        catch (ClassNotFoundException e)
                            {
                            throw new NoClassDefFoundError(e.getMessage());
                            }
                        return clz;
                        }
                    
                    //++ getter for autogen property _Module
                    /**
                     * This is an auto-generated method that returns the global
                    * [design time] parent component.
                    * 
                    * Note: the class generator will ignore any custom
                    * implementation for this behavior.
                     */
                    private com.tangosol.coherence.Component get_Module()
                        {
                        return this.get_Parent().get_Parent().get_Parent().get_Parent();
                        }
                    }
                }

            // ---- class: com.tangosol.coherence.component.util.NameService$TcpAcceptor$MessageFactory$EncodedMessage
            
            /**
             * A Message with a ReadBuffer that contains an encoded Message.
             * The service thread will decode the Message using the configured
             * Codec before dispatching it for execution.
             */
            @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
            public static class EncodedMessage
                    extends    com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.EncodedMessage
                {
                // ---- Fields declarations ----
                
                // Default constructor
                public EncodedMessage()
                    {
                    this(null, null, true);
                    }
                
                // Initializing constructor
                public EncodedMessage(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                    return new com.tangosol.coherence.component.util.NameService.TcpAcceptor.MessageFactory.EncodedMessage();
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
                        clz = Class.forName("com.tangosol.coherence/component/util/NameService$TcpAcceptor$MessageFactory$EncodedMessage".replace('/', '.'));
                        }
                    catch (ClassNotFoundException e)
                        {
                        throw new NoClassDefFoundError(e.getMessage());
                        }
                    return clz;
                    }
                
                //++ getter for autogen property _Module
                /**
                 * This is an auto-generated method that returns the global
                * [design time] parent component.
                * 
                * Note: the class generator will ignore any custom
                * implementation for this behavior.
                 */
                private com.tangosol.coherence.Component get_Module()
                    {
                    return this.get_Parent().get_Parent().get_Parent();
                    }
                }

            // ---- class: com.tangosol.coherence.component.util.NameService$TcpAcceptor$MessageFactory$NotifyChannelClosed
            
            /**
             * This Message is sent to the peer when a Channel has been closed.
             * This allows the peer to collect any resources held by the
             * Channel.
             */
            @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
            public static class NotifyChannelClosed
                    extends    com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.NotifyChannelClosed
                {
                // ---- Fields declarations ----
                
                // Default constructor
                public NotifyChannelClosed()
                    {
                    this(null, null, true);
                    }
                
                // Initializing constructor
                public NotifyChannelClosed(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                    return new com.tangosol.coherence.component.util.NameService.TcpAcceptor.MessageFactory.NotifyChannelClosed();
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
                        clz = Class.forName("com.tangosol.coherence/component/util/NameService$TcpAcceptor$MessageFactory$NotifyChannelClosed".replace('/', '.'));
                        }
                    catch (ClassNotFoundException e)
                        {
                        throw new NoClassDefFoundError(e.getMessage());
                        }
                    return clz;
                    }
                
                //++ getter for autogen property _Module
                /**
                 * This is an auto-generated method that returns the global
                * [design time] parent component.
                * 
                * Note: the class generator will ignore any custom
                * implementation for this behavior.
                 */
                private com.tangosol.coherence.Component get_Module()
                    {
                    return this.get_Parent().get_Parent().get_Parent();
                    }
                }

            // ---- class: com.tangosol.coherence.component.util.NameService$TcpAcceptor$MessageFactory$NotifyConnectionClosed
            
            /**
             * This Message is sent to the peer when a Connection has been
             * closed. This allows the peer to collect any resources held by
             * the Connection.
             */
            @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
            public static class NotifyConnectionClosed
                    extends    com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.NotifyConnectionClosed
                {
                // ---- Fields declarations ----
                
                // Default constructor
                public NotifyConnectionClosed()
                    {
                    this(null, null, true);
                    }
                
                // Initializing constructor
                public NotifyConnectionClosed(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                    return new com.tangosol.coherence.component.util.NameService.TcpAcceptor.MessageFactory.NotifyConnectionClosed();
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
                        clz = Class.forName("com.tangosol.coherence/component/util/NameService$TcpAcceptor$MessageFactory$NotifyConnectionClosed".replace('/', '.'));
                        }
                    catch (ClassNotFoundException e)
                        {
                        throw new NoClassDefFoundError(e.getMessage());
                        }
                    return clz;
                    }
                
                //++ getter for autogen property _Module
                /**
                 * This is an auto-generated method that returns the global
                * [design time] parent component.
                * 
                * Note: the class generator will ignore any custom
                * implementation for this behavior.
                 */
                private com.tangosol.coherence.Component get_Module()
                    {
                    return this.get_Parent().get_Parent().get_Parent();
                    }
                }

            // ---- class: com.tangosol.coherence.component.util.NameService$TcpAcceptor$MessageFactory$NotifyShutdown
            
            /**
             * This internal Message is sent to a ConnectionManager it is
             * supposed to shut down. The ConnectionManager must clean up and
             * unregister itself. Note that the only task of the shut-down is
             * to begin the process of shutting down the service; technically
             * the ConnectionManager does not have to be stopped by the time
             * the shutdown Message completes its processing, although the
             * default implementation does stop it immediately.
             */
            @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
            public static class NotifyShutdown
                    extends    com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.NotifyShutdown
                {
                // ---- Fields declarations ----
                
                // Default constructor
                public NotifyShutdown()
                    {
                    this(null, null, true);
                    }
                
                // Initializing constructor
                public NotifyShutdown(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                    return new com.tangosol.coherence.component.util.NameService.TcpAcceptor.MessageFactory.NotifyShutdown();
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
                        clz = Class.forName("com.tangosol.coherence/component/util/NameService$TcpAcceptor$MessageFactory$NotifyShutdown".replace('/', '.'));
                        }
                    catch (ClassNotFoundException e)
                        {
                        throw new NoClassDefFoundError(e.getMessage());
                        }
                    return clz;
                    }
                
                //++ getter for autogen property _Module
                /**
                 * This is an auto-generated method that returns the global
                * [design time] parent component.
                * 
                * Note: the class generator will ignore any custom
                * implementation for this behavior.
                 */
                private com.tangosol.coherence.Component get_Module()
                    {
                    return this.get_Parent().get_Parent().get_Parent();
                    }
                }

            // ---- class: com.tangosol.coherence.component.util.NameService$TcpAcceptor$MessageFactory$NotifyStartup
            
            /**
             * This internal Message is sent to a ConnectionManager when it
             * first has been started.
             */
            @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
            public static class NotifyStartup
                    extends    com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.NotifyStartup
                {
                // ---- Fields declarations ----
                
                // Default constructor
                public NotifyStartup()
                    {
                    this(null, null, true);
                    }
                
                // Initializing constructor
                public NotifyStartup(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                    return new com.tangosol.coherence.component.util.NameService.TcpAcceptor.MessageFactory.NotifyStartup();
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
                        clz = Class.forName("com.tangosol.coherence/component/util/NameService$TcpAcceptor$MessageFactory$NotifyStartup".replace('/', '.'));
                        }
                    catch (ClassNotFoundException e)
                        {
                        throw new NoClassDefFoundError(e.getMessage());
                        }
                    return clz;
                    }
                
                //++ getter for autogen property _Module
                /**
                 * This is an auto-generated method that returns the global
                * [design time] parent component.
                * 
                * Note: the class generator will ignore any custom
                * implementation for this behavior.
                 */
                private com.tangosol.coherence.Component get_Module()
                    {
                    return this.get_Parent().get_Parent().get_Parent();
                    }
                }

            // ---- class: com.tangosol.coherence.component.util.NameService$TcpAcceptor$MessageFactory$OpenChannel
            
            /**
             * Internal Request used to open a Channel.
             */
            @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
            public static class OpenChannel
                    extends    com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.OpenChannel
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
                    __mapChildren.put("Status", NameService.TcpAcceptor.MessageFactory.OpenChannel.Status.get_CLASS());
                    }
                
                // Default constructor
                public OpenChannel()
                    {
                    this(null, null, true);
                    }
                
                // Initializing constructor
                public OpenChannel(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                    return new com.tangosol.coherence.component.util.NameService.TcpAcceptor.MessageFactory.OpenChannel();
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
                        clz = Class.forName("com.tangosol.coherence/component/util/NameService$TcpAcceptor$MessageFactory$OpenChannel".replace('/', '.'));
                        }
                    catch (ClassNotFoundException e)
                        {
                        throw new NoClassDefFoundError(e.getMessage());
                        }
                    return clz;
                    }
                
                //++ getter for autogen property _Module
                /**
                 * This is an auto-generated method that returns the global
                * [design time] parent component.
                * 
                * Note: the class generator will ignore any custom
                * implementation for this behavior.
                 */
                private com.tangosol.coherence.Component get_Module()
                    {
                    return this.get_Parent().get_Parent().get_Parent();
                    }
                
                //++ getter for autogen property _ChildClasses
                /**
                 * This is an auto-generated method that returns the map of
                * design time [static] children.
                * 
                * Note: the class generator will ignore any custom
                * implementation for this behavior.
                 */
                protected java.util.Map get_ChildClasses()
                    {
                    return __mapChildren;
                    }

                // ---- class: com.tangosol.coherence.component.util.NameService$TcpAcceptor$MessageFactory$OpenChannel$Status
                
                /**
                 * Implementation of the Request$Status interface.
                 */
                @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
                public static class Status
                        extends    com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.OpenChannel.Status
                    {
                    // ---- Fields declarations ----
                    
                    // Default constructor
                    public Status()
                        {
                        this(null, null, true);
                        }
                    
                    // Initializing constructor
                    public Status(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                        return new com.tangosol.coherence.component.util.NameService.TcpAcceptor.MessageFactory.OpenChannel.Status();
                        }
                    
                    //++ getter for static property _CLASS
                    /**
                     * Getter for property _CLASS.<p>
                    * Property with auto-generated accessor that returns the
                    * Class object for a given component.
                     */
                    public static Class get_CLASS()
                        {
                        Class clz;
                        try
                            {
                            clz = Class.forName("com.tangosol.coherence/component/util/NameService$TcpAcceptor$MessageFactory$OpenChannel$Status".replace('/', '.'));
                            }
                        catch (ClassNotFoundException e)
                            {
                            throw new NoClassDefFoundError(e.getMessage());
                            }
                        return clz;
                        }
                    
                    //++ getter for autogen property _Module
                    /**
                     * This is an auto-generated method that returns the global
                    * [design time] parent component.
                    * 
                    * Note: the class generator will ignore any custom
                    * implementation for this behavior.
                     */
                    private com.tangosol.coherence.Component get_Module()
                        {
                        return this.get_Parent().get_Parent().get_Parent().get_Parent();
                        }
                    }
                }

            // ---- class: com.tangosol.coherence.component.util.NameService$TcpAcceptor$MessageFactory$OpenChannelRequest
            
            /**
             * This Request is used to open a new Channel.
             */
            @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
            public static class OpenChannelRequest
                    extends    com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.OpenChannelRequest
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
                    __mapChildren.put("Status", NameService.TcpAcceptor.MessageFactory.OpenChannelRequest.Status.get_CLASS());
                    }
                
                // Default constructor
                public OpenChannelRequest()
                    {
                    this(null, null, true);
                    }
                
                // Initializing constructor
                public OpenChannelRequest(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                    return new com.tangosol.coherence.component.util.NameService.TcpAcceptor.MessageFactory.OpenChannelRequest();
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
                        clz = Class.forName("com.tangosol.coherence/component/util/NameService$TcpAcceptor$MessageFactory$OpenChannelRequest".replace('/', '.'));
                        }
                    catch (ClassNotFoundException e)
                        {
                        throw new NoClassDefFoundError(e.getMessage());
                        }
                    return clz;
                    }
                
                //++ getter for autogen property _Module
                /**
                 * This is an auto-generated method that returns the global
                * [design time] parent component.
                * 
                * Note: the class generator will ignore any custom
                * implementation for this behavior.
                 */
                private com.tangosol.coherence.Component get_Module()
                    {
                    return this.get_Parent().get_Parent().get_Parent();
                    }
                
                //++ getter for autogen property _ChildClasses
                /**
                 * This is an auto-generated method that returns the map of
                * design time [static] children.
                * 
                * Note: the class generator will ignore any custom
                * implementation for this behavior.
                 */
                protected java.util.Map get_ChildClasses()
                    {
                    return __mapChildren;
                    }

                // ---- class: com.tangosol.coherence.component.util.NameService$TcpAcceptor$MessageFactory$OpenChannelRequest$Status
                
                /**
                 * Implementation of the Request$Status interface.
                 */
                @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
                public static class Status
                        extends    com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.OpenChannelRequest.Status
                    {
                    // ---- Fields declarations ----
                    
                    // Default constructor
                    public Status()
                        {
                        this(null, null, true);
                        }
                    
                    // Initializing constructor
                    public Status(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                        return new com.tangosol.coherence.component.util.NameService.TcpAcceptor.MessageFactory.OpenChannelRequest.Status();
                        }
                    
                    //++ getter for static property _CLASS
                    /**
                     * Getter for property _CLASS.<p>
                    * Property with auto-generated accessor that returns the
                    * Class object for a given component.
                     */
                    public static Class get_CLASS()
                        {
                        Class clz;
                        try
                            {
                            clz = Class.forName("com.tangosol.coherence/component/util/NameService$TcpAcceptor$MessageFactory$OpenChannelRequest$Status".replace('/', '.'));
                            }
                        catch (ClassNotFoundException e)
                            {
                            throw new NoClassDefFoundError(e.getMessage());
                            }
                        return clz;
                        }
                    
                    //++ getter for autogen property _Module
                    /**
                     * This is an auto-generated method that returns the global
                    * [design time] parent component.
                    * 
                    * Note: the class generator will ignore any custom
                    * implementation for this behavior.
                     */
                    private com.tangosol.coherence.Component get_Module()
                        {
                        return this.get_Parent().get_Parent().get_Parent().get_Parent();
                        }
                    }
                }

            // ---- class: com.tangosol.coherence.component.util.NameService$TcpAcceptor$MessageFactory$OpenChannelResponse
            
            /**
             * Response to an OpenChannelRequest.
             */
            @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
            public static class OpenChannelResponse
                    extends    com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.OpenChannelResponse
                {
                // ---- Fields declarations ----
                
                // Default constructor
                public OpenChannelResponse()
                    {
                    this(null, null, true);
                    }
                
                // Initializing constructor
                public OpenChannelResponse(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                    return new com.tangosol.coherence.component.util.NameService.TcpAcceptor.MessageFactory.OpenChannelResponse();
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
                        clz = Class.forName("com.tangosol.coherence/component/util/NameService$TcpAcceptor$MessageFactory$OpenChannelResponse".replace('/', '.'));
                        }
                    catch (ClassNotFoundException e)
                        {
                        throw new NoClassDefFoundError(e.getMessage());
                        }
                    return clz;
                    }
                
                //++ getter for autogen property _Module
                /**
                 * This is an auto-generated method that returns the global
                * [design time] parent component.
                * 
                * Note: the class generator will ignore any custom
                * implementation for this behavior.
                 */
                private com.tangosol.coherence.Component get_Module()
                    {
                    return this.get_Parent().get_Parent().get_Parent();
                    }
                }

            // ---- class: com.tangosol.coherence.component.util.NameService$TcpAcceptor$MessageFactory$OpenConnection
            
            /**
             * Internal Request used to open a Connection.
             */
            @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
            public static class OpenConnection
                    extends    com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.Acceptor.MessageFactory.OpenConnection
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
                    __mapChildren.put("Status", NameService.TcpAcceptor.MessageFactory.OpenConnection.Status.get_CLASS());
                    }
                
                // Default constructor
                public OpenConnection()
                    {
                    this(null, null, true);
                    }
                
                // Initializing constructor
                public OpenConnection(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                    return new com.tangosol.coherence.component.util.NameService.TcpAcceptor.MessageFactory.OpenConnection();
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
                        clz = Class.forName("com.tangosol.coherence/component/util/NameService$TcpAcceptor$MessageFactory$OpenConnection".replace('/', '.'));
                        }
                    catch (ClassNotFoundException e)
                        {
                        throw new NoClassDefFoundError(e.getMessage());
                        }
                    return clz;
                    }
                
                //++ getter for autogen property _Module
                /**
                 * This is an auto-generated method that returns the global
                * [design time] parent component.
                * 
                * Note: the class generator will ignore any custom
                * implementation for this behavior.
                 */
                private com.tangosol.coherence.Component get_Module()
                    {
                    return this.get_Parent().get_Parent().get_Parent();
                    }
                
                //++ getter for autogen property _ChildClasses
                /**
                 * This is an auto-generated method that returns the map of
                * design time [static] children.
                * 
                * Note: the class generator will ignore any custom
                * implementation for this behavior.
                 */
                protected java.util.Map get_ChildClasses()
                    {
                    return __mapChildren;
                    }

                // ---- class: com.tangosol.coherence.component.util.NameService$TcpAcceptor$MessageFactory$OpenConnection$Status
                
                /**
                 * Implementation of the Request$Status interface.
                 */
                @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
                public static class Status
                        extends    com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.OpenConnection.Status
                    {
                    // ---- Fields declarations ----
                    
                    // Default constructor
                    public Status()
                        {
                        this(null, null, true);
                        }
                    
                    // Initializing constructor
                    public Status(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                        return new com.tangosol.coherence.component.util.NameService.TcpAcceptor.MessageFactory.OpenConnection.Status();
                        }
                    
                    //++ getter for static property _CLASS
                    /**
                     * Getter for property _CLASS.<p>
                    * Property with auto-generated accessor that returns the
                    * Class object for a given component.
                     */
                    public static Class get_CLASS()
                        {
                        Class clz;
                        try
                            {
                            clz = Class.forName("com.tangosol.coherence/component/util/NameService$TcpAcceptor$MessageFactory$OpenConnection$Status".replace('/', '.'));
                            }
                        catch (ClassNotFoundException e)
                            {
                            throw new NoClassDefFoundError(e.getMessage());
                            }
                        return clz;
                        }
                    
                    //++ getter for autogen property _Module
                    /**
                     * This is an auto-generated method that returns the global
                    * [design time] parent component.
                    * 
                    * Note: the class generator will ignore any custom
                    * implementation for this behavior.
                     */
                    private com.tangosol.coherence.Component get_Module()
                        {
                        return this.get_Parent().get_Parent().get_Parent().get_Parent();
                        }
                    }
                }

            // ---- class: com.tangosol.coherence.component.util.NameService$TcpAcceptor$MessageFactory$OpenConnectionRequest
            
            /**
             * This Request is used to open a new Channel.
             */
            @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
            public static class OpenConnectionRequest
                    extends    com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.TcpAcceptor.MessageFactory.OpenConnectionRequest
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
                    __mapChildren.put("Status", NameService.TcpAcceptor.MessageFactory.OpenConnectionRequest.Status.get_CLASS());
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
                    return new com.tangosol.coherence.component.util.NameService.TcpAcceptor.MessageFactory.OpenConnectionRequest();
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
                        clz = Class.forName("com.tangosol.coherence/component/util/NameService$TcpAcceptor$MessageFactory$OpenConnectionRequest".replace('/', '.'));
                        }
                    catch (ClassNotFoundException e)
                        {
                        throw new NoClassDefFoundError(e.getMessage());
                        }
                    return clz;
                    }
                
                //++ getter for autogen property _Module
                /**
                 * This is an auto-generated method that returns the global
                * [design time] parent component.
                * 
                * Note: the class generator will ignore any custom
                * implementation for this behavior.
                 */
                private com.tangosol.coherence.Component get_Module()
                    {
                    return this.get_Parent().get_Parent().get_Parent();
                    }
                
                //++ getter for autogen property _ChildClasses
                /**
                 * This is an auto-generated method that returns the map of
                * design time [static] children.
                * 
                * Note: the class generator will ignore any custom
                * implementation for this behavior.
                 */
                protected java.util.Map get_ChildClasses()
                    {
                    return __mapChildren;
                    }

                // ---- class: com.tangosol.coherence.component.util.NameService$TcpAcceptor$MessageFactory$OpenConnectionRequest$Status
                
                /**
                 * Implementation of the Request$Status interface.
                 */
                @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
                public static class Status
                        extends    com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.OpenConnectionRequest.Status
                    {
                    // ---- Fields declarations ----
                    
                    // Default constructor
                    public Status()
                        {
                        this(null, null, true);
                        }
                    
                    // Initializing constructor
                    public Status(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                        return new com.tangosol.coherence.component.util.NameService.TcpAcceptor.MessageFactory.OpenConnectionRequest.Status();
                        }
                    
                    //++ getter for static property _CLASS
                    /**
                     * Getter for property _CLASS.<p>
                    * Property with auto-generated accessor that returns the
                    * Class object for a given component.
                     */
                    public static Class get_CLASS()
                        {
                        Class clz;
                        try
                            {
                            clz = Class.forName("com.tangosol.coherence/component/util/NameService$TcpAcceptor$MessageFactory$OpenConnectionRequest$Status".replace('/', '.'));
                            }
                        catch (ClassNotFoundException e)
                            {
                            throw new NoClassDefFoundError(e.getMessage());
                            }
                        return clz;
                        }
                    
                    //++ getter for autogen property _Module
                    /**
                     * This is an auto-generated method that returns the global
                    * [design time] parent component.
                    * 
                    * Note: the class generator will ignore any custom
                    * implementation for this behavior.
                     */
                    private com.tangosol.coherence.Component get_Module()
                        {
                        return this.get_Parent().get_Parent().get_Parent().get_Parent();
                        }
                    }
                }

            // ---- class: com.tangosol.coherence.component.util.NameService$TcpAcceptor$MessageFactory$OpenConnectionResponse
            
            /**
             * Response to an OpenChannelRequest.
             */
            @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
            public static class OpenConnectionResponse
                    extends    com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.TcpAcceptor.MessageFactory.OpenConnectionResponse
                {
                // ---- Fields declarations ----
                
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
                    return new com.tangosol.coherence.component.util.NameService.TcpAcceptor.MessageFactory.OpenConnectionResponse();
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
                        clz = Class.forName("com.tangosol.coherence/component/util/NameService$TcpAcceptor$MessageFactory$OpenConnectionResponse".replace('/', '.'));
                        }
                    catch (ClassNotFoundException e)
                        {
                        throw new NoClassDefFoundError(e.getMessage());
                        }
                    return clz;
                    }
                
                //++ getter for autogen property _Module
                /**
                 * This is an auto-generated method that returns the global
                * [design time] parent component.
                * 
                * Note: the class generator will ignore any custom
                * implementation for this behavior.
                 */
                private com.tangosol.coherence.Component get_Module()
                    {
                    return this.get_Parent().get_Parent().get_Parent();
                    }
                }

            // ---- class: com.tangosol.coherence.component.util.NameService$TcpAcceptor$MessageFactory$PingRequest
            
            /**
             * This Request is used to test the integrity of a Connection.
             */
            @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
            public static class PingRequest
                    extends    com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.TcpAcceptor.MessageFactory.PingRequest
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
                    __mapChildren.put("Status", NameService.TcpAcceptor.MessageFactory.PingRequest.Status.get_CLASS());
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
                    return new com.tangosol.coherence.component.util.NameService.TcpAcceptor.MessageFactory.PingRequest();
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
                        clz = Class.forName("com.tangosol.coherence/component/util/NameService$TcpAcceptor$MessageFactory$PingRequest".replace('/', '.'));
                        }
                    catch (ClassNotFoundException e)
                        {
                        throw new NoClassDefFoundError(e.getMessage());
                        }
                    return clz;
                    }
                
                //++ getter for autogen property _Module
                /**
                 * This is an auto-generated method that returns the global
                * [design time] parent component.
                * 
                * Note: the class generator will ignore any custom
                * implementation for this behavior.
                 */
                private com.tangosol.coherence.Component get_Module()
                    {
                    return this.get_Parent().get_Parent().get_Parent();
                    }
                
                //++ getter for autogen property _ChildClasses
                /**
                 * This is an auto-generated method that returns the map of
                * design time [static] children.
                * 
                * Note: the class generator will ignore any custom
                * implementation for this behavior.
                 */
                protected java.util.Map get_ChildClasses()
                    {
                    return __mapChildren;
                    }

                // ---- class: com.tangosol.coherence.component.util.NameService$TcpAcceptor$MessageFactory$PingRequest$Status
                
                /**
                 * Implementation of the Request$Status interface.
                 */
                @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
                public static class Status
                        extends    com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.PingRequest.Status
                    {
                    // ---- Fields declarations ----
                    
                    // Default constructor
                    public Status()
                        {
                        this(null, null, true);
                        }
                    
                    // Initializing constructor
                    public Status(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                        return new com.tangosol.coherence.component.util.NameService.TcpAcceptor.MessageFactory.PingRequest.Status();
                        }
                    
                    //++ getter for static property _CLASS
                    /**
                     * Getter for property _CLASS.<p>
                    * Property with auto-generated accessor that returns the
                    * Class object for a given component.
                     */
                    public static Class get_CLASS()
                        {
                        Class clz;
                        try
                            {
                            clz = Class.forName("com.tangosol.coherence/component/util/NameService$TcpAcceptor$MessageFactory$PingRequest$Status".replace('/', '.'));
                            }
                        catch (ClassNotFoundException e)
                            {
                            throw new NoClassDefFoundError(e.getMessage());
                            }
                        return clz;
                        }
                    
                    //++ getter for autogen property _Module
                    /**
                     * This is an auto-generated method that returns the global
                    * [design time] parent component.
                    * 
                    * Note: the class generator will ignore any custom
                    * implementation for this behavior.
                     */
                    private com.tangosol.coherence.Component get_Module()
                        {
                        return this.get_Parent().get_Parent().get_Parent().get_Parent();
                        }
                    }
                }

            // ---- class: com.tangosol.coherence.component.util.NameService$TcpAcceptor$MessageFactory$PingResponse
            
            /**
             * Response to a PingRequest
             */
            @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
            public static class PingResponse
                    extends    com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.PingResponse
                {
                // ---- Fields declarations ----
                
                // Default constructor
                public PingResponse()
                    {
                    this(null, null, true);
                    }
                
                // Initializing constructor
                public PingResponse(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                    return new com.tangosol.coherence.component.util.NameService.TcpAcceptor.MessageFactory.PingResponse();
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
                        clz = Class.forName("com.tangosol.coherence/component/util/NameService$TcpAcceptor$MessageFactory$PingResponse".replace('/', '.'));
                        }
                    catch (ClassNotFoundException e)
                        {
                        throw new NoClassDefFoundError(e.getMessage());
                        }
                    return clz;
                    }
                
                //++ getter for autogen property _Module
                /**
                 * This is an auto-generated method that returns the global
                * [design time] parent component.
                * 
                * Note: the class generator will ignore any custom
                * implementation for this behavior.
                 */
                private com.tangosol.coherence.Component get_Module()
                    {
                    return this.get_Parent().get_Parent().get_Parent();
                    }
                }

            // ---- class: com.tangosol.coherence.component.util.NameService$TcpAcceptor$MessageFactory$Response
            
            /**
             * Generic Response used for all internal Requests.
             */
            @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
            public static class Response
                    extends    com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.Response
                {
                // ---- Fields declarations ----
                
                // Default constructor
                public Response()
                    {
                    this(null, null, true);
                    }
                
                // Initializing constructor
                public Response(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                    return new com.tangosol.coherence.component.util.NameService.TcpAcceptor.MessageFactory.Response();
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
                        clz = Class.forName("com.tangosol.coherence/component/util/NameService$TcpAcceptor$MessageFactory$Response".replace('/', '.'));
                        }
                    catch (ClassNotFoundException e)
                        {
                        throw new NoClassDefFoundError(e.getMessage());
                        }
                    return clz;
                    }
                
                //++ getter for autogen property _Module
                /**
                 * This is an auto-generated method that returns the global
                * [design time] parent component.
                * 
                * Note: the class generator will ignore any custom
                * implementation for this behavior.
                 */
                private com.tangosol.coherence.Component get_Module()
                    {
                    return this.get_Parent().get_Parent().get_Parent();
                    }
                }
            }

        // ---- class: com.tangosol.coherence.component.util.NameService$TcpAcceptor$Queue
        
        /**
         * This is the Queue to which items that need to be processed are
         * added, and from which the daemon pulls items to process.
         */
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class Queue
                extends    com.tangosol.coherence.component.util.daemon.QueueProcessor.Queue
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
                __mapChildren.put("Iterator", NameService.TcpAcceptor.Queue.Iterator.get_CLASS());
                }
            
            // Default constructor
            public Queue()
                {
                this(null, null, true);
                }
            
            // Initializing constructor
            public Queue(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                    setElementList(new com.tangosol.util.RecyclingLinkedList());
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
                return new com.tangosol.coherence.component.util.NameService.TcpAcceptor.Queue();
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
                    clz = Class.forName("com.tangosol.coherence/component/util/NameService$TcpAcceptor$Queue".replace('/', '.'));
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

            // ---- class: com.tangosol.coherence.component.util.NameService$TcpAcceptor$Queue$Iterator
            
            /**
             * Iterator of a snapshot of the List object that backs the Queue.
             * Supports remove(). Uses the Queue as the monitor if any
             * synchronization is required.
             */
            @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
            public static class Iterator
                    extends    com.tangosol.coherence.component.util.daemon.QueueProcessor.Queue.Iterator
                {
                // ---- Fields declarations ----
                
                // Default constructor
                public Iterator()
                    {
                    this(null, null, true);
                    }
                
                // Initializing constructor
                public Iterator(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                    return new com.tangosol.coherence.component.util.NameService.TcpAcceptor.Queue.Iterator();
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
                        clz = Class.forName("com.tangosol.coherence/component/util/NameService$TcpAcceptor$Queue$Iterator".replace('/', '.'));
                        }
                    catch (ClassNotFoundException e)
                        {
                        throw new NoClassDefFoundError(e.getMessage());
                        }
                    return clz;
                    }
                
                //++ getter for autogen property _Module
                /**
                 * This is an auto-generated method that returns the global
                * [design time] parent component.
                * 
                * Note: the class generator will ignore any custom
                * implementation for this behavior.
                 */
                private com.tangosol.coherence.Component get_Module()
                    {
                    return this.get_Parent().get_Parent().get_Parent();
                    }
                }
            }

        // ---- class: com.tangosol.coherence.component.util.NameService$TcpAcceptor$TcpConnection
        
        /**
         * Connection implementation that wraps a non-blocking TCP/IP Socket.
         */
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class TcpConnection
                extends    com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.TcpAcceptor.TcpConnection
            {
            // ---- Fields declarations ----
            
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
                return new com.tangosol.coherence.component.util.NameService.TcpAcceptor.TcpConnection();
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
                    clz = Class.forName("com.tangosol.coherence/component/util/NameService$TcpAcceptor$TcpConnection".replace('/', '.'));
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
             * Unregister the given Channel from the ChannelArray.
            * 
            * @param channel  the Channel to unregister; must not be null
             */
            public void unregisterChannel(com.tangosol.net.messaging.Channel channel)
                {
                // import java.util.Iterator;
                // import java.util.Map;
                // import java.util.Set;
                // import javax.naming.NamingException;
                
                super.unregisterChannel(channel);
                
                NameService ns      = (NameService) get_Module();
                Map     mapBind = ns.getBinderMap();
                Set     setName = (Set) mapBind.remove(channel);
                
                if (setName != null)
                    {
                    for (Iterator iterName = setName.iterator(); iterName.hasNext(); )
                        {
                        try
                            {
                            ns.unbind((String) iterName.next());
                            }
                        catch (NamingException e) {}
                        }
                    }
                }
            }

        // ---- class: com.tangosol.coherence.component.util.NameService$TcpAcceptor$TcpProcessor
        
        /**
         * Daemon used to perform all non-blocking TCP/IP I/O operations.
         */
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class TcpProcessor
                extends    com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.TcpAcceptor.TcpProcessor
            {
            // ---- Fields declarations ----
            
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
                _addChild(new Daemon.Guard("Guard", this, true), "Guard");
                
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
                return new com.tangosol.coherence.component.util.NameService.TcpAcceptor.TcpProcessor();
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
                    clz = Class.forName("com.tangosol.coherence/component/util/NameService$TcpAcceptor$TcpProcessor".replace('/', '.'));
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
            }
        }
    }
