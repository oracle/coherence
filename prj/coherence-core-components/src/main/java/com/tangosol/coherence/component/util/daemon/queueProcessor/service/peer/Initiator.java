
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.Initiator

package com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer;

import com.tangosol.coherence.component.net.extend.Channel;
import com.tangosol.coherence.component.net.extend.Connection;
import com.tangosol.coherence.component.net.extend.RemoteService;
import com.tangosol.internal.net.service.peer.initiator.InitiatorDependencies;
import com.tangosol.internal.net.service.peer.initiator.JmsInitiatorDependencies;
import com.tangosol.internal.net.service.peer.initiator.TcpInitiatorDependencies;
import com.tangosol.net.RequestTimeoutException;
import com.tangosol.net.Service;
import com.tangosol.net.security.SecurityHelper;
import com.tangosol.util.Base;
import com.tangosol.util.UUID;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.security.auth.Subject;

/**
 * Base definition of a ConnectionAcceptor component.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public abstract class Initiator
        extends    com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer
        implements com.tangosol.net.messaging.ConnectionInitiator
    {
    // ---- Fields declarations ----
    
    /**
     * Property CloseOnExit
     *
     * A set of AutoCloseables to close on exit.
     */
    private com.tangosol.util.SafeHashSet __m_CloseOnExit;
    
    /**
     * Property Connection
     *
     * The Connection managed by this Initiator.
     * 
     * @see com.tangosol.net.messaging.ConnectionInitiator#ensureConnection
     */
    private com.tangosol.coherence.component.net.extend.Connection __m_Connection;
    
    /**
     * Property ConnectTimeout
     *
     * The maximum amount of time (in milliseconds) that the Initiator will
     * wait for a new Connection to be established. If 0, the Initiator will
     * wait indefinitely. This property defaults to the value of the
     * RequestTimeout property.
     */
    private long __m_ConnectTimeout;
    
    /**
     * Property RequestSendTimeout
     *
     * The maximum amount of time (in milliseconds) that the Initiator will
     * wait for a request send. If 0, the Initiator will wait indefinitely.
     * This property defaults to the value of the RequestTimeout property.
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
        __mapChildren.put("DispatchEvent", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.DispatchEvent.get_CLASS());
        __mapChildren.put("MessageFactory", Initiator.MessageFactory.get_CLASS());
        __mapChildren.put("Queue", com.tangosol.coherence.component.util.daemon.QueueProcessor.Queue.get_CLASS());
        }
    
    // Initializing constructor
    public Initiator(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
        {
        super(sName, compParent, false);
        }
    
    // Private initializer
    protected void __initPrivate()
        {
        
        super.__initPrivate();
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
            clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/peer/Initiator".replace('/', '.'));
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
     * Check the Connection(s) managed by this ConnectionManager for a ping
    * timeout. A Connection that has not received a PingResponse for an
    * oustanding PingRequest within the configured PingTimeout will be closed.
     */
    protected void checkPingTimeouts()
        {
        // import Component.Net.Extend.Connection;
        
        Connection connection = getConnection();
        if (connection != null)
            {
            checkPingTimeout(connection);
            }
        }
    
    /**
     * Factory method: create and configure a new ConnectionInitiator for the
    * given dependencies.
    * 
    * @param deps  the InititatorDependencies used to create a new
    * ConnectionInitiator
    * @param ctx     the OperationalContext for the new ConnectionInitiator
    * 
    * @return a new ConnectionInitiator
     */
    public static com.tangosol.net.messaging.ConnectionInitiator createInitiator(com.tangosol.internal.net.service.peer.initiator.InitiatorDependencies deps, com.tangosol.net.OperationalContext ctx)
        {
        // import com.tangosol.internal.net.service.peer.initiator.JmsInitiatorDependencies;
        // import com.tangosol.internal.net.service.peer.initiator.TcpInitiatorDependencies;
        
        Initiator initiator;
        if (deps instanceof JmsInitiatorDependencies)
            {
            initiator = (Initiator) _newInstance(
                    "Component.Util.Daemon.QueueProcessor.Service.Peer.Initiator.JmsInitiator");
            }
        else if (deps instanceof TcpInitiatorDependencies)
            {
            initiator = (Initiator) _newInstance(
                    "Component.Util.Daemon.QueueProcessor.Service.Peer.Initiator.TcpInitiator");
            }
        else
            {
            throw new IllegalArgumentException("unsupported initiator dependencies :\n"
                    + deps);
            }
        
        initiator.setOperationalContext(ctx);
        initiator.setDependencies(deps);
        return initiator;
        }
    
    // From interface: com.tangosol.net.messaging.ConnectionInitiator
    public synchronized com.tangosol.net.messaging.Connection ensureConnection()
        {
        // import Component.Net.Extend.Connection;
        
        if (!isRunning())
            {
            throw new IllegalStateException(getServiceName() + " is not running");
            }
        
        Connection connection = getConnection();
        if (connection == null || !connection.isOpen())
            {
            connection = openConnection();
            }
        
        return connection;
        }
    
    // Accessor for the property "CloseOnExit"
    /**
     * Getter for property CloseOnExit.<p>
    * A set of AutoCloseables to close on exit.
     */
    public com.tangosol.util.SafeHashSet getCloseOnExit()
        {
        return __m_CloseOnExit;
        }
    
    // Accessor for the property "Connection"
    /**
     * Getter for property Connection.<p>
    * The Connection managed by this Initiator.
    * 
    * @see com.tangosol.net.messaging.ConnectionInitiator#ensureConnection
     */
    public com.tangosol.coherence.component.net.extend.Connection getConnection()
        {
        return __m_Connection;
        }
    
    // Accessor for the property "ConnectTimeout"
    /**
     * Getter for property ConnectTimeout.<p>
    * The maximum amount of time (in milliseconds) that the Initiator will wait
    * for a new Connection to be established. If 0, the Initiator will wait
    * indefinitely. This property defaults to the value of the RequestTimeout
    * property.
     */
    public long getConnectTimeout()
        {
        return __m_ConnectTimeout;
        }
    
    // Declared at the super level
    /**
     * Getter for property Description.<p>
    * Human-readable description of additional Service properties. Used by
    * toString().
     */
    public String getDescription()
        {
        return super.getDescription() + ", ConnectTimeout=" + getConnectTimeout();
        }
    
    // Accessor for the property "RequestSendTimeout"
    /**
     * Getter for property RequestSendTimeout.<p>
    * The maximum amount of time (in milliseconds) that the Initiator will wait
    * for a request send. If 0, the Initiator will wait indefinitely. This
    * property defaults to the value of the RequestTimeout property.
     */
    public long getRequestSendTimeout()
        {
        return __m_RequestSendTimeout;
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
        if (getConnection() == connection)
            {
            setConnection(null);
            super.onConnectionClosed(connection);
            }
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
        if (getConnection() == connection)
            {
            setConnection(null);
            super.onConnectionError(connection, e);
            }
        }
    
    // Declared at the super level
    /**
     * Called after a Connection has been successfully established. This method
    * is called on the service thread.
    * 
    * @param connection  the Connection that was opened
     */
    public void onConnectionOpened(com.tangosol.coherence.component.net.extend.Connection connection)
        {
        if (get_Connection() == connection)
            {
            return;
            }
        
        if (getConnection() == null)
            {
            setConnection(connection);
            super.onConnectionOpened(connection);
            }
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
        // import com.tangosol.internal.net.service.peer.initiator.InitiatorDependencies;
        
        super.onDependencies(deps);
        
        InitiatorDependencies initiatorDeps = (InitiatorDependencies) deps;
        
        setConnectTimeout(initiatorDeps.getConnectTimeoutMillis());
        setRequestSendTimeout(initiatorDeps.getRequestSendTimeoutMillis());
        }
    
    // Declared at the super level
    /**
     * Event notification called right before the daemon thread terminates. This
    * method is guaranteed to be called only once and on the daemon's thread.
     */
    protected void onExit()
        {
        // import java.util.Iterator;
        // import java.util.Set;
        
        Set setClose = getCloseOnExit();
        synchronized (setClose)
            {
            for (Iterator iter = setClose.iterator(); iter.hasNext(); )
                {
                try
                    {
                    ((AutoCloseable) iter.next()).close();
                    }
                catch (Exception ignored) {}
                }
            }
        
        
        super.onExit();
        }
    
    // Declared at the super level
    /**
     * The default implementation of this method sets AcceptingClients to false.
     */
    protected void onServiceStopped()
        {
        // import Component.Net.Extend.Connection;
        
        Connection connection = getConnection();
        if (connection != null)
            {
            connection.closeInternal(true, null, 100L);
            if (connection.isOpen())
                {
                // we were unable to close the Connection because a daemon thread is
                // currently executing within the Connection; interrupt all daemons
                getDaemonPool().stop();
        
                connection.closeInternal(true, null, 1000L);
                if (connection.isOpen())
                    {
                    _trace("Unable to close \"" + connection
                            + "\"; this Connection will be abandoned", 1);
                    }
                }
            }
        
        super.onServiceStopped();
        }
    
    // Declared at the super level
    /**
     * The default implementation of this method sets AcceptingClients to false.
     */
    protected void onServiceStopping()
        {
        // import Component.Net.Extend.Connection;
        
        Connection connection = getConnection();
        if (connection != null)
            {
            connection.closeInternal(true, null, 0L);
            }
        
        super.onServiceStopping();
        }
    
    /**
     * Open and return a new Connection.
    * 
    * @return a newly opened Connection
     */
    protected com.tangosol.coherence.component.net.extend.Connection openConnection()
        {
        // import Component.Net.Extend.Connection;
        
        Connection connection = instantiateConnection();
        connection.open();
        
        return connection;
        }
    
    // Declared at the super level
    /**
     * Open the given Connection. This method is called by Connection.open() and
    * is always run on client threads.
    * 
    * @param connection  the Connection to open
     */
    public void openConnection(com.tangosol.coherence.component.net.extend.Connection connection)
        {
        // import com.tangosol.net.RequestTimeoutException;
        // import com.tangosol.net.messaging.Channel;
        // import com.tangosol.net.messaging.Protocol$MessageFactory as com.tangosol.net.messaging.Protocol.MessageFactory;
        // import com.tangosol.net.messaging.Request$Status as com.tangosol.net.messaging.Request.Status;
        // import com.tangosol.net.security.SecurityHelper;
        // import javax.security.auth.Subject;
        
        _assert(connection != null);
        
        Channel channel0 = get_Channel();
        com.tangosol.net.messaging.Protocol.MessageFactory factory0 = channel0.getMessageFactory();
        
        Initiator.MessageFactory.OpenConnection request = (Initiator.MessageFactory.OpenConnection)
                factory0.createMessage(Initiator.MessageFactory.OpenConnection.TYPE_ID);
        
        Subject subject = SecurityHelper.getCurrentSubject();
        
        request.setConnectionOpen(connection);
        request.setIdentityToken(serializeIdentityToken(generateIdentityToken(subject)));
        request.setSubject(subject);
        
        com.tangosol.net.messaging.Request.Status status = (com.tangosol.net.messaging.Request.Status) channel0.request(request);
        if (status != null)
            {
            try
                {
                status.waitForResponse(getConnectTimeout());
                }
            catch (RequestTimeoutException e)
                {
                connection.close(false, e);
                throw e;
                }
            }
        }
    
    // Declared at the super level
    /**
     * Ping the Connection(s) managed by this ConnectionManager.
     */
    protected void ping()
        {
        // import Component.Net.Extend.Connection;
        
        Connection connection = getConnection();
        if (connection != null)
            {
            connection.ping();
            }
        }
    
    // Accessor for the property "CloseOnExit"
    /**
     * Setter for property CloseOnExit.<p>
    * A set of AutoCloseables to close on exit.
     */
    public void setCloseOnExit(com.tangosol.util.SafeHashSet setExit)
        {
        __m_CloseOnExit = setExit;
        }
    
    // Accessor for the property "Connection"
    /**
     * Setter for property Connection.<p>
    * The Connection managed by this Initiator.
    * 
    * @see com.tangosol.net.messaging.ConnectionInitiator#ensureConnection
     */
    public void setConnection(com.tangosol.coherence.component.net.extend.Connection connection)
        {
        __m_Connection = connection;
        }
    
    // Accessor for the property "ConnectTimeout"
    /**
     * Setter for property ConnectTimeout.<p>
    * The maximum amount of time (in milliseconds) that the Initiator will wait
    * for a new Connection to be established. If 0, the Initiator will wait
    * indefinitely. This property defaults to the value of the RequestTimeout
    * property.
     */
    protected void setConnectTimeout(long cMillis)
        {
        __m_ConnectTimeout = cMillis;
        }
    
    // Accessor for the property "RequestSendTimeout"
    /**
     * Setter for property RequestSendTimeout.<p>
    * The maximum amount of time (in milliseconds) that the Initiator will wait
    * for a request send. If 0, the Initiator will wait indefinitely. This
    * property defaults to the value of the RequestTimeout property.
     */
    protected void setRequestSendTimeout(long cMillis)
        {
        __m_RequestSendTimeout = cMillis;
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.Initiator$MessageFactory
    
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
            extends    com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory
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
            __mapChildren.put("OpenConnection", Initiator.MessageFactory.OpenConnection.get_CLASS());
            __mapChildren.put("OpenConnectionRequest", Initiator.MessageFactory.OpenConnectionRequest.get_CLASS());
            __mapChildren.put("OpenConnectionResponse", Initiator.MessageFactory.OpenConnectionResponse.get_CLASS());
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.Initiator.MessageFactory();
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
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/peer/Initiator$MessageFactory".replace('/', '.'));
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

        // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.Initiator$MessageFactory$OpenConnection
        
        /**
         * Internal Request used to open a Connection.
         */
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class OpenConnection
                extends    com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.OpenConnection
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
                __mapChildren.put("Status", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.OpenConnection.Status.get_CLASS());
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
                return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.Initiator.MessageFactory.OpenConnection();
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
                    clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/peer/Initiator$MessageFactory$OpenConnection".replace('/', '.'));
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
             * Called when the Request is run.
            * 
            * @param response  the Response that should be populated with the
            * result of running the Request
             */
            protected void onRun(com.tangosol.coherence.component.net.extend.message.Response response)
                {
                // import Component.Net.Extend.Connection;
                // import Component.Net.Extend.RemoteService;
                // import com.tangosol.net.messaging.Channel as com.tangosol.net.messaging.Channel;
                // import com.tangosol.net.messaging.Protocol$MessageFactory as com.tangosol.net.messaging.Protocol.MessageFactory;
                // import com.tangosol.net.Service;
                // import com.tangosol.util.Base;
                
                Connection connection = getConnectionOpen();
                _assert(!connection.isOpen());
                
                Initiator module = (Initiator) getChannel().getReceiver();
                
                connection.openInternal();
                
                try
                    {
                    com.tangosol.net.messaging.Channel channel0 = connection.getChannel(0);
                    com.tangosol.net.messaging.Protocol.MessageFactory factory0 = channel0.getMessageFactory();
                
                    // sent a OpenConnectionRequest to the peer via "Channel0"
                    Initiator.MessageFactory.OpenConnectionRequest request = (Initiator.MessageFactory.OpenConnectionRequest)
                            factory0.createMessage(Initiator.MessageFactory.OpenConnectionRequest.TYPE_ID);
                
                    request.setClientId(module.getProcessId());
                    request.setConnectionOpen(connection);
                    request.setEdition(module.getOperationalContext().getEdition());
                    request.setIdentityToken(getIdentityToken());    
                    request.setMember(module.getOperationalContext().getLocalMember());
                    request.setProtocolVersionMap(module.getProtocolVersionMap());
                    request.setSubject(getSubject());
                
                    Service svcParent = module.getParentService();
                    if (svcParent instanceof RemoteService)
                        {
                        RemoteService svcRemote = (RemoteService) svcParent;
                        request.setClusterName(svcRemote.getRemoteClusterName());
                        request.setServiceName(svcRemote.getRemoteServiceName());
                        }
                
                    response.setResult(channel0.send(request));
                    }
                catch (Throwable e)
                    {
                    connection.closeInternal(false, e, -1L);
                    throw Base.ensureRuntimeException(e);
                    }
                }
            }

        // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.Initiator$MessageFactory$OpenConnectionRequest
        
        /**
         * This Request is used to open a new Channel.
         */
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class OpenConnectionRequest
                extends    com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.OpenConnectionRequest
            {
            // ---- Fields declarations ----
            
            /**
             * Property ConnectionOpen
             *
             * The Connection to open.
             */
            private transient com.tangosol.coherence.component.net.extend.Connection __m_ConnectionOpen;
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
                return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.Initiator.MessageFactory.OpenConnectionRequest();
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
                    clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/peer/Initiator$MessageFactory$OpenConnectionRequest".replace('/', '.'));
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
            
            // Accessor for the property "ConnectionOpen"
            /**
             * Getter for property ConnectionOpen.<p>
            * The Connection to open.
             */
            public com.tangosol.coherence.component.net.extend.Connection getConnectionOpen()
                {
                return __m_ConnectionOpen;
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
                _assert(getChannel().getId() == 0);
                }
            
            // Accessor for the property "ConnectionOpen"
            /**
             * Setter for property ConnectionOpen.<p>
            * The Connection to open.
             */
            public void setConnectionOpen(com.tangosol.coherence.component.net.extend.Connection connection)
                {
                __m_ConnectionOpen = connection;
                }
            }

        // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.Initiator$MessageFactory$OpenConnectionResponse
        
        /**
         * Response to an OpenChannelRequest.
         */
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class OpenConnectionResponse
                extends    com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.OpenConnectionResponse
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
                return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.Initiator.MessageFactory.OpenConnectionResponse();
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
                    clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/peer/Initiator$MessageFactory$OpenConnectionResponse".replace('/', '.'));
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
            public void run()
                {
                // import Component.Net.Extend.Channel;
                // import Component.Net.Extend.Connection;
                // import com.tangosol.net.messaging.Protocol as com.tangosol.net.messaging.Protocol;
                // import com.tangosol.util.UUID;
                // import java.util.Collections;
                // import java.util.HashMap;
                // import java.util.Iterator;
                // import java.util.Map;
                // import java.util.Map$Entry as java.util.Map.Entry;
                
                Channel channel0 = (Channel) getChannel();
                _assert(channel0.getId() == 0);
                
                if (isFailure())
                    {
                    Connection connection = (Connection) channel0.getConnection();
                    Object     oResult    = getResult();
                    Throwable  eResult    = oResult instanceof Throwable
                            ? (Throwable) oResult : null;
                
                    connection.closeInternal(false, eResult, -1L);
                    return;
                    }
                
                Connection connection = (Connection) channel0.getConnection();
                Initiator    module     = (Initiator) channel0.getReceiver();
                Object[]   ao         = (Object[]) getResult();
                
                _assert(ao != null && ao.length == 2);
                
                // extract the "Channel0" configuration from the OpenConnectionRequest
                Initiator.MessageFactory.OpenConnectionRequest request = (Initiator.MessageFactory.OpenConnectionRequest) channel0.getRequest(getRequestId());
                if (request == null)
                    {
                    // request had timed-out and will be closed by the thread which created it
                    return;
                    }
                
                connection.setId((UUID) ao[0]);
                connection.setMember(request.getMember());
                connection.setPeerId((UUID) ao[1]);
                channel0.setSubject(request.getSubject());
                
                // configure the MessageFactory map for the Connection
                Map mapProtocol = module.getProtocolMap();
                Map mapFactory  = new HashMap(mapProtocol.size());
                Map mapVersion  = getProtocolVersionMap();
                if (mapVersion != null)
                    {
                    for (Iterator iter = mapVersion.entrySet().iterator(); iter.hasNext(); )
                        {
                        java.util.Map.Entry    entry    = (java.util.Map.Entry) iter.next();
                        String   sName    = (String) entry.getKey();
                        Integer  IVersion = (Integer) entry.getValue();
                        com.tangosol.net.messaging.Protocol protocol = (com.tangosol.net.messaging.Protocol) mapProtocol.get(sName);
                    
                        mapFactory.put(sName, protocol.getMessageFactory(IVersion.intValue()));
                        }
                    }
                for (Iterator iter = mapProtocol.entrySet().iterator(); iter.hasNext(); )
                    {
                    java.util.Map.Entry  entry = (java.util.Map.Entry) iter.next();
                    String sName = (String) entry.getKey();
                    
                    if (!mapFactory.containsKey(sName))
                        {
                        com.tangosol.net.messaging.Protocol protocol = (com.tangosol.net.messaging.Protocol) entry.getValue();
                        mapFactory.put(sName, protocol.getMessageFactory(protocol.getCurrentVersion()));
                        }
                    }
                connection.setMessageFactoryMap(Collections.unmodifiableMap(mapFactory));
                
                // the Connection is now ready for use
                module.onConnectionOpened(connection);
                }
            }
        }
    }
