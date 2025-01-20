
/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.GrpcAcceptor

package com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor;

import com.tangosol.coherence.component.net.extend.Channel;
import com.tangosol.coherence.component.net.extend.Connection;

import com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.grpcAcceptor.GrpcConnection;
import com.tangosol.internal.net.service.peer.acceptor.DefaultGrpcAcceptorDependencies;
import com.tangosol.internal.net.service.peer.acceptor.GrpcAcceptorDependencies;

import com.tangosol.net.grpc.GrpcAcceptorController;

import javax.security.auth.Subject;

import java.net.URI;

/**
 * Base definition of a ConnectionAcceptor component.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class GrpcAcceptor
        extends    com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.Acceptor
    {
    // ---- Fields declarations ----
    
    /**
     * Property Controller
     *
     */
    private com.tangosol.net.grpc.GrpcAcceptorController __m_Controller;
    
    /**
     * Property ListenAddress
     *
     */
    private String __m_ListenAddress;
    
    /**
     * Property ListenPort
     *
     */
    private int __m_ListenPort;
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
    public GrpcAcceptor()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public GrpcAcceptor(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
        return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.GrpcAcceptor();
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
            clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/peer/acceptor/GrpcAcceptor".replace('/', '.'));
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
        // import com.tangosol.internal.net.service.peer.acceptor.DefaultGrpcAcceptorDependencies;
        // import com.tangosol.internal.net.service.peer.acceptor.GrpcAcceptorDependencies;
        
        return new DefaultGrpcAcceptorDependencies((GrpcAcceptorDependencies) deps);
        }
    
    // Declared at the super level
    public synchronized void configure(com.tangosol.run.xml.XmlElement xml)
        {
        DefaultGrpcAcceptorDependencies dependencies = new DefaultGrpcAcceptorDependencies();

        setDependencies(com.tangosol.internal.net.service.peer.acceptor.LegacyXmlGrpcAcceptorHelper.fromXml(xml,
            dependencies, getOperationalContext(), getContextClassLoader()));
        
        setServiceConfig(xml);
        }
    
    // Accessor for the property "Controller"
    /**
     * Getter for property Controller.<p>
     */
    public com.tangosol.net.grpc.GrpcAcceptorController getController()
        {
        return __m_Controller;
        }
    
    // Declared at the super level
    /**
     * Getter for property Description.<p>
    * Human-readable description of additional Service properties. Used by
    * toString().
     */
    public String getDescription()
        {
        // import com.tangosol.net.grpc.GrpcAcceptorController as com.tangosol.net.grpc.GrpcAcceptorController;
        
        com.tangosol.net.grpc.GrpcAcceptorController controller = getController();
        if (controller == null)
            {
            return "GrpcAcceptor : LocalAddress=0.0.0.0,LocalPort=0"; 
            }
        else
            {
            return "GrpcAcceptor :" +
                   " LocalAddress=" + getListenAddress() +
                   ",LocalPort="    + getListenPort(); 
            }
        }
    
    // Accessor for the property "ListenAddress"
    /**
     * Getter for property ListenAddress.<p>
     */
    public String getListenAddress()
        {
        return __m_ListenAddress;
        }
    
    // Accessor for the property "ListenPort"
    /**
     * Getter for property ListenPort.<p>
     */
    public int getListenPort()
        {
        return __m_ListenPort;
        }
    
    // Declared at the super level

    /**
     * Factory method: create a new Connection.
     * <p>
     * Implementations must configure the Connection with a reference to this
     * ConnectionManager (see Connection#setConnectionManager).
     *
     * @return a new Connection object that has yet to be opened
     */
    @Override
    protected com.tangosol.coherence.component.net.extend.Connection instantiateConnection()
        {
        GrpcConnection connection = createConnection();
        connection.setConnectionManager(this);
//        connection.setMessageFactoryMap(getProtocolMap());
        return connection;
        }

    public GrpcConnection openConnection()
        {
        GrpcConnection connection = (GrpcConnection) instantiateConnection();
        connection.open();
        return connection;
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
        // import com.tangosol.internal.net.service.peer.acceptor.GrpcAcceptorDependencies;
        
        super.onDependencies(deps);
        
        GrpcAcceptorDependencies grpcDeps = (GrpcAcceptorDependencies) deps;
        
        setController(grpcDeps.getController());
        }
    
    // Declared at the super level
    /**
     * The default implementation of this method does nothing.
     */
    protected void onServiceStarting()
        {
        GrpcAcceptorController controller = getController();
        _assert(controller != null);
        
        GrpcAcceptorDependencies grpcDeps = (GrpcAcceptorDependencies) getDependencies();

        controller.setAcceptor(this);
        controller.setDependencies(grpcDeps);
        
        getParentService().getResourceRegistry()
                .registerResource(GrpcAcceptorController.class, controller);
        
        super.onServiceStarting();
                
        controller.start();
        
        String sAddr = controller.getLocalAddress();
        int    nPort = controller.getLocalPort();
        
        setListenAddress(sAddr);
        setListenPort(nPort);
        
        _trace("GrpcAcceptor now listening for connections on "
                + sAddr + ':' + nPort, 3);
        }
    
    // Declared at the super level
    /**
     * The default implementation of this method sets AcceptingClients to false.
     */
    protected void onServiceStopped()
        {
        // import com.tangosol.net.grpc.GrpcAcceptorController as com.tangosol.net.grpc.GrpcAcceptorController;
        
        super.onServiceStopped();
        
        com.tangosol.net.grpc.GrpcAcceptorController controller = getController();
        if (controller != null)
            {
            controller.stop();
            }
        }
    
    // Declared at the super level
    /**
     * The default implementation of this method sets AcceptingClients to false.
     */
    protected void onServiceStopping()
        {
        // import com.tangosol.net.grpc.GrpcAcceptorController as com.tangosol.net.grpc.GrpcAcceptorController;
        
        super.onServiceStopping();
        
        com.tangosol.net.grpc.GrpcAcceptorController controller = getController();
        if (controller != null)
            {
            controller.stop();
            }
        }
    
    // Accessor for the property "Controller"
    /**
     * Setter for property Controller.<p>
     */
    public void setController(com.tangosol.net.grpc.GrpcAcceptorController controller)
        {
        __m_Controller = controller;
        }
    
    // Accessor for the property "ListenAddress"
    /**
     * Setter for property ListenAddress.<p>
     */
    public void setListenAddress(String sAddress)
        {
        __m_ListenAddress = sAddress;
        }
    
    // Accessor for the property "ListenPort"
    /**
     * Setter for property ListenPort.<p>
     */
    public void setListenPort(int nPort)
        {
        __m_ListenPort = nPort;
        }

    public GrpcConnection getConnection()
        {
        return (GrpcConnection) super.get_Connection();
        }

    @Override
    protected GrpcConnection createConnection()
        {
        return new GrpcConnection();
        }

    @Override
    public com.tangosol.net.messaging.Channel acceptChannel(Connection connection, URI uri, ClassLoader loader, Channel.Receiver receiver, Subject subject)
        {
        Object oToken = generateIdentityToken(subject);
        int    nId    = Integer.parseInt(uri.getSchemeSpecificPart());
        connection.acceptChannelRequest
            (
            nId,
            assertIdentityToken(oToken),
            getAccessAdapter()
            );
        return (com.tangosol.net.messaging.Channel) connection.getChannelArray().get(nId);
        }

    public void registerGrpcProtocol(com.tangosol.net.messaging.Protocol protocol)
        {
        if (protocol == null)
            {
            throw new IllegalArgumentException("protocol cannot be null");
            }

        String sName = protocol.getName();
        if (sName == null)
            {
            throw new IllegalArgumentException("missing protocol name: " + protocol);
            }

        getProtocolMap().put(sName, protocol);
        GrpcConnection connection = getConnection();
        if (connection != null)
            {
            connection.getMessageFactoryMap().put(sName, protocol.getMessageFactory(protocol.getCurrentVersion()));
            }
        }

//    // ----- inner class: GrpcConnection ------------------------------------
//
//    /**
//     * A gRPC specific {@link Connection}.
//     *
//     * @param <Resp>  the type of the response message sent down
//     *                the channel to the client
//     */
//    public static class GrpcConnection<Resp extends Message>
//            extends Connection
//        {
//        public GrpcConnection()
//            {
//            }
//
//        @Override
//        protected GrpcChannel<Resp> createChannel()
//            {
//            GrpcChannel<Resp> channel = new GrpcChannel<>();
//            channel.setConnection(this);
//            return channel;
//            }
//
//        @Override
//        public void setMessageFactoryMap(Map map)
//            {
//            // we must ensure the map is writeable
//            super.setMessageFactoryMap(new HashMap(map));
//            }
//
//        /**
//         * Add a {@link Protocol.MessageFactory}.
//         *
//         * @param clz      they protocol class
//         * @param factory  the factory to add
//         * @param <P>      the type of the protocol
//         */
//        @SuppressWarnings("unchecked")
//        public  <P extends  com.tangosol.coherence.component.net.extend.Protocol>
//        void addMessageFactory(Class<P> clz, Protocol.MessageFactory factory)
//            {
//            Map map = getMessageFactoryMap();
//            if (map == null)
//                {
//                map = new HashMap();
//                setMessageFactoryMap(map);
//                }
//            map.put(clz.getSimpleName(), factory);
//            }
//
//        public StreamObserver<Resp> getStreamObserver()
//            {
//            return m_observer;
//            }
//
//        public void setStreamObserver(StreamObserver<Resp> observer)
//            {
//            m_observer = observer;
//            }
//
//        // ----- data members ---------------------------------------------------
//
//        private StreamObserver<Resp> m_observer;
//        }
//
//    // ----- inner class: GrpcChannel ---------------------------------------
//
//    /**
//     * An implementation of a {@link Channel} used by gRPC proxies.
//     *
//     * @param <Resp>  the type of the response message sent down
//     *                the channel to the client
//     */
//    public static class GrpcChannel<Resp extends Message>
//            extends Channel
//        {
//        public GrpcChannel()
//            {
//            }
//
//        @Override
//        public GrpcConnection getConnection()
//            {
//            return (GrpcConnection) super.getConnection();
//            }
//
//        @Override
//        public void setConnection(com.tangosol.net.messaging.Connection connection)
//            {
//            super.setConnection(connection);
//            }
//
//        public StreamObserver<Resp> getStreamObserver()
//            {
//            return m_observer;
//            }
//
//        public void setStreamObserver(StreamObserver<Resp> observer)
//            {
//            m_observer = observer;
//            }
//
//        /**
//         * Accept requests on the specified sub-channel.
//         *
//         * @param nChannelId  the channel identifier
//         */
//        public void acceptChannelRequest(int nChannelId)
//            {
//            Connection connection = getConnection();
//            connection.acceptChannelRequest(nChannelId, null, null);
//            }
//
//        /**
//         * Add a {@link com.tangosol.coherence.component.net.extend.MessageFactory}.
//         *
//         * @param clz      they protocol class
//         * @param factory  the factory to add
//         * @param <P>      the type of the protocol
//         */
//        @SuppressWarnings("unchecked")
//        protected <P extends Protocol> void addMessageFactory(Class<P> clz, MessageFactory factory)
//            {
//            getConnection().getMessageFactoryMap().put(clz.getSimpleName(), factory);
//            }
//
//        /**
//         * Obtain a sub-channel.
//         *
//         * @param nChannelId  the channel identifier
//         *
//         * @return the sub-channel or an empty {@link Optional} if there
//         *         is no sub-channel with the specified identifier
//         */
//        public GrpcChannel getSubChannel(int nChannelId)
//            {
//            return (GrpcChannel) getConnection().getChannel(nChannelId);
//            }
//
//        /**
//         * Obtain the receiver for a sub-channel.
//         *
//         * @param nChannelId  the channel identifier
//         * @param <R>         the type of the receiver to return
//         *
//         * @return the receiver for a sub-channel
//         */
//        @SuppressWarnings("unchecked")
//        public <R extends Channel.Receiver> R getChannelReceiver(int nChannelId)
//            {
//            Channel channel = getSubChannel(nChannelId);
//            return channel == null ? null : (R) channel.getReceiver();
//            }
//
//        @Override
//        public void post(com.tangosol.net.messaging.Message message)
//            {
//            if (message instanceof GrpcResponse)
//                {
//                GrpcResponse         response = (GrpcResponse) message;
//                StreamObserver<Resp> observer = (StreamObserver<Resp>) response.getStreamObserver();
//                if (response.isFailure())
//                    {
//                    Throwable error = (Throwable) response.getResult();
//                    observer.onError(error);
//                    }
//                else
//                    {
//                    GrpcMessageFactory<?, Resp> factory       = (GrpcMessageFactory) getMessageFactory();
//                    Resp                         protoResponse = factory.createResponse(response);
//                    observer.onNext(protoResponse);
//                    if (response.completeStream())
//                        {
//                        observer.onCompleted();
//                        }
//                    }
//                }
//            else
//                {
//                int nId = getId();
//                if (nId != 0)
//                    {
//                    GrpcMessageFactory<?, Resp> factory      = (GrpcMessageFactory) getMessageFactory();
//                    Resp                        protoMessage = factory.toProtoMessage(message, nId);
//                    if (protoMessage != null)
//                        {
//                        m_observer.onNext(protoMessage);
//                        }
//                    }
//                else
//                    {
//                    super.post(message);
//                    }
//                }
//            }
//
//        // ----- data members ---------------------------------------------------
//
//        private StreamObserver<Resp> m_observer;
//        }
//
    }
