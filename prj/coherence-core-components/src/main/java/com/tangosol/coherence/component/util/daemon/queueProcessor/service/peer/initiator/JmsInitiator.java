
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.initiator.JmsInitiator

package com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.initiator;

import com.tangosol.coherence.component.net.extend.util.JmsUtil;
import com.tangosol.coherence.component.net.extend.util.JndiUtil;
import com.tangosol.internal.net.service.peer.initiator.DefaultJmsInitiatorDependencies;
import com.tangosol.internal.net.service.peer.initiator.JmsInitiatorDependencies;
import com.tangosol.net.messaging.ConnectionException;
import com.tangosol.util.Base;
import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueReceiver;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.naming.NamingException;

/**
 * A ConnectionInitiator implementation that initiates Connections over JMS.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class JmsInitiator
        extends    com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.Initiator
        implements javax.jms.ExceptionListener
    {
    // ---- Fields declarations ----
    
    /**
     * Property MessageDeliveryMode
     *
     * The delivery mode of JMS Messages sent by the JmsInitiator and all
     * JmsConnections created by the JmsInitiator. Must be one of
     * DeliveryMode.NON_PERSISTENT or DeliveryMode.PERSISTENT.
     * 
     * @see javax.jms.DeliveryMode
     */
    private int __m_MessageDeliveryMode;
    
    /**
     * Property MessageExpiration
     *
     * The expiration value of JMS Messages sent by the JmsInitiator and all
     * JmsConnections created by the JmsInitiator.
     * 
     * @see javax.jms.Message#getJMSExpiration
     */
    private long __m_MessageExpiration;
    
    /**
     * Property MessagePriority
     *
     * The priority of JMS Messages sent by the JmsInitiator and all
     * JmsConnections created by the JmsInitiator.
     * 
     * @see javax.jms.Message#getJMSPriority
     */
    private int __m_MessagePriority;
    
    /**
     * Property QueueConnection
     *
     * The JMS QueueConnection used by this JmsInitiator.
     */
    private javax.jms.QueueConnection __m_QueueConnection;
    
    /**
     * Property QueueConnectionFactoryName
     *
     * The JNDI name of the JMS QueueConnectionFactory used by this
     * JmsInitiator.
     */
    private String __m_QueueConnectionFactoryName;
    
    /**
     * Property QueueDiscovery
     *
     * The Queue used to send JMS Messages to an arbitrary JmsAcceptor. This
     * Queue is used by the JmsInitiator to establish a point-to-point
     * JmsConnection (via two TemporaryQueues) with a single JmsAcceptor.
     * 
     * @see #getQueueSender
     */
    private javax.jms.Queue __m_QueueDiscovery;
    
    /**
     * Property QueueName
     *
     * The JNDI name of the JMS "discovery" Queue used by this JmsInitiator.
     */
    private String __m_QueueName;
    
    /**
     * Property QueueSender
     *
     * The JMS QueueSender used by this JmsInitiator to send JMS Messages to
     * the Queue specified by the QueueDiscovery property.
     * 
     * @see #getQueueDiscovery
     */
    private javax.jms.QueueSender __m_QueueSender;
    
    /**
     * Property QueueSession
     *
     * The JMS QueueSession used by this JmsInitiator.
     */
    private javax.jms.QueueSession __m_QueueSession;
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
        __mapChildren.put("JmsConnection", JmsInitiator.JmsConnection.get_CLASS());
        __mapChildren.put("MessageFactory", com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.Initiator.MessageFactory.get_CLASS());
        __mapChildren.put("Queue", com.tangosol.coherence.component.util.daemon.QueueProcessor.Queue.get_CLASS());
        }
    
    // Default constructor
    public JmsInitiator()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public JmsInitiator(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
        return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.initiator.JmsInitiator();
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
            clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/peer/initiator/JmsInitiator".replace('/', '.'));
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
        // import com.tangosol.internal.net.service.peer.initiator.DefaultJmsInitiatorDependencies;
        // import com.tangosol.internal.net.service.peer.initiator.JmsInitiatorDependencies;
        
        return new DefaultJmsInitiatorDependencies((JmsInitiatorDependencies) deps);
        }
    
    // Declared at the super level
    public synchronized void configure(com.tangosol.run.xml.XmlElement xml)
        {
        // import com.tangosol.internal.net.service.peer.initiator.DefaultJmsInitiatorDependencies;
        // import com.tangosol.internal.net.service.peer.initiator.LegacyXmlJmsInitiatorHelper as com.tangosol.internal.net.service.peer.initiator.LegacyXmlJmsInitiatorHelper;
        
        setDependencies(com.tangosol.internal.net.service.peer.initiator.LegacyXmlJmsInitiatorHelper.fromXml(xml,
            new DefaultJmsInitiatorDependencies(), getOperationalContext(),
            getContextClassLoader()));
        
        setServiceConfig(xml);
        }
    
    // Declared at the super level
    /**
     * Getter for property Description.<p>
    * Human-readable description of additional Service properties. Used by
    * toString().
     */
    public String getDescription()
        {
        // import javax.jms.DeliveryMode;
        
        String sMode;
        switch (getMessageDeliveryMode())
            {
            case DeliveryMode.PERSISTENT:
                sMode = "PERSISTENT";
                break;
        
            case DeliveryMode.NON_PERSISTENT:
                sMode = "NON_PERSISTENT";
                break;
        
            default:
                sMode = "UNKNOWN";
            }
        
        return super.getDescription()
                + ", QueueConnectionFactoryName=" + getQueueConnectionFactoryName()
                + ", QueueName="                  + getQueueName()
                + ", MessageExpiration="          + getMessageExpiration()
                + ", MessagePriority="            + getMessageDeliveryMode()
                + ", MessageDeliveryMode="        + sMode;
        }
    
    // Accessor for the property "MessageDeliveryMode"
    /**
     * Getter for property MessageDeliveryMode.<p>
    * The delivery mode of JMS Messages sent by the JmsInitiator and all
    * JmsConnections created by the JmsInitiator. Must be one of
    * DeliveryMode.NON_PERSISTENT or DeliveryMode.PERSISTENT.
    * 
    * @see javax.jms.DeliveryMode
     */
    public int getMessageDeliveryMode()
        {
        return __m_MessageDeliveryMode;
        }
    
    // Accessor for the property "MessageExpiration"
    /**
     * Getter for property MessageExpiration.<p>
    * The expiration value of JMS Messages sent by the JmsInitiator and all
    * JmsConnections created by the JmsInitiator.
    * 
    * @see javax.jms.Message#getJMSExpiration
     */
    public long getMessageExpiration()
        {
        return __m_MessageExpiration;
        }
    
    // Accessor for the property "MessagePriority"
    /**
     * Getter for property MessagePriority.<p>
    * The priority of JMS Messages sent by the JmsInitiator and all
    * JmsConnections created by the JmsInitiator.
    * 
    * @see javax.jms.Message#getJMSPriority
     */
    public int getMessagePriority()
        {
        return __m_MessagePriority;
        }
    
    // Accessor for the property "QueueConnection"
    /**
     * Getter for property QueueConnection.<p>
    * The JMS QueueConnection used by this JmsInitiator.
     */
    public javax.jms.QueueConnection getQueueConnection()
        {
        return __m_QueueConnection;
        }
    
    // Accessor for the property "QueueConnectionFactoryName"
    /**
     * Getter for property QueueConnectionFactoryName.<p>
    * The JNDI name of the JMS QueueConnectionFactory used by this JmsInitiator.
     */
    public String getQueueConnectionFactoryName()
        {
        return __m_QueueConnectionFactoryName;
        }
    
    // Accessor for the property "QueueDiscovery"
    /**
     * Getter for property QueueDiscovery.<p>
    * The Queue used to send JMS Messages to an arbitrary JmsAcceptor. This
    * Queue is used by the JmsInitiator to establish a point-to-point
    * JmsConnection (via two TemporaryQueues) with a single JmsAcceptor.
    * 
    * @see #getQueueSender
     */
    public javax.jms.Queue getQueueDiscovery()
        {
        return __m_QueueDiscovery;
        }
    
    // Accessor for the property "QueueName"
    /**
     * Getter for property QueueName.<p>
    * The JNDI name of the JMS "discovery" Queue used by this JmsInitiator.
     */
    public String getQueueName()
        {
        return __m_QueueName;
        }
    
    // Accessor for the property "QueueSender"
    /**
     * Getter for property QueueSender.<p>
    * The JMS QueueSender used by this JmsInitiator to send JMS Messages to the
    * Queue specified by the QueueDiscovery property.
    * 
    * @see #getQueueDiscovery
     */
    public javax.jms.QueueSender getQueueSender()
        {
        return __m_QueueSender;
        }
    
    // Accessor for the property "QueueSession"
    /**
     * Getter for property QueueSession.<p>
    * The JMS QueueSession used by this JmsInitiator.
     */
    public javax.jms.QueueSession getQueueSession()
        {
        return __m_QueueSession;
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
        JmsInitiator.JmsConnection connection = (JmsInitiator.JmsConnection) _newChild("JmsConnection");
        connection.setConnectionManager(this);
        
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
        // import com.tangosol.internal.net.service.peer.initiator.JmsInitiatorDependencies;
        
        super.onDependencies(deps);
        
        JmsInitiatorDependencies jmsDeps = (JmsInitiatorDependencies) deps;
        
        setMessageDeliveryMode(jmsDeps.getMessageDeliveryMode());
        setMessageExpiration(jmsDeps.getMessageExpiration());
        setMessagePriority(jmsDeps.getMessagePriority());
        setQueueConnectionFactoryName(jmsDeps.getQueueConnectionFactoryName());
        setQueueName(jmsDeps.getQueueName());
        }
    
    // From interface: javax.jms.ExceptionListener
    public void onException(javax.jms.JMSException e)
        {
        _trace(e, "Stopping " + getServiceName() + " due to a fatal JMS exception.");
        stop();
        }
    
    // Declared at the super level
    /**
     * The default implementation of this method does nothing.
     */
    protected void onServiceStarting()
        {
        // import Component.Net.Extend.Util.JndiUtil;
        // import com.tangosol.util.Base;
        // import javax.jms.JMSException;
        // import javax.jms.Queue as javax.jms.Queue;
        // import javax.jms.QueueConnection;
        // import javax.jms.QueueConnectionFactory;
        // import javax.jms.QueueSession;
        // import javax.jms.Session;
        // import javax.naming.NamingException;
        
        super.onServiceStarting();
        
        // look up the javax.jms.Queue in JNDI
        javax.jms.Queue queue;
        try
            {
            setQueueDiscovery(queue = (javax.jms.Queue) JndiUtil.lookup(getQueueName(), javax.jms.Queue.class));
            }
        catch (NamingException e)
            {
            throw Base.ensureRuntimeException(e, "error retrieving a javax.jms.Queue from JNDI"
                    + " using the name \"" + getQueueName() + "\"");
            }
        
        // look up a QueueConnectionFactory in JNDI
        QueueConnectionFactory factory;
        try
            {
            factory = (QueueConnectionFactory)
                    JndiUtil.lookup(getQueueConnectionFactoryName(),
                            QueueConnectionFactory.class);
            }
        catch (NamingException e)
            {
            throw Base.ensureRuntimeException(e, "error retrieving a "
                    + " QueueConnectionFactory from JNDI using the name \""
                    + getQueueConnectionFactoryName() + "\"");
            }
        
        // create a QueueConnection
        QueueConnection connection;
        try
            {
            setQueueConnection(connection = factory.createQueueConnection());
            }
        catch (JMSException e)
            {
            throw Base.ensureRuntimeException(e, "error creating a QueueConnection");
            }
        
        // create a QueueSession
        QueueSession session;
        try
            {
            setQueueSession(session = connection.createQueueSession(false,
                    Session.AUTO_ACKNOWLEDGE));
            }
        catch (JMSException e)
            {
            throw Base.ensureRuntimeException(e, "error creating a QueueSession");
            }
        
        // create a QueueSender
        try
            {
            setQueueSender(session.createSender(queue));
            }
        catch (JMSException e)
            {
            throw Base.ensureRuntimeException(e, "error creating a QueueSender");
            }
        
        // add ourself as a JMS ExceptionListener
        try
            {
            connection.setExceptionListener(this);
            }
        catch (JMSException e)
            {
            throw Base.ensureRuntimeException(e, "error registering an ExceptionListener");
            }
        
        // start the QueueConnection
        try
            {
            connection.start();
            }
        catch (JMSException e)
            {
            throw Base.ensureRuntimeException(e, "error starting a QueueConnection");
            }
        }
    
    // Declared at the super level
    /**
     * The default implementation of this method sets AcceptingClients to false.
     */
    protected void onServiceStopped()
        {
        // import Component.Net.Extend.Util.JmsUtil;
        
        super.onServiceStopped();
        
        JmsUtil.close(getQueueSender());
        JmsUtil.close(getQueueSession());
        JmsUtil.close(getQueueConnection());
        }
    
    // Accessor for the property "MessageDeliveryMode"
    /**
     * Setter for property MessageDeliveryMode.<p>
    * The delivery mode of JMS Messages sent by the JmsInitiator and all
    * JmsConnections created by the JmsInitiator. Must be one of
    * DeliveryMode.NON_PERSISTENT or DeliveryMode.PERSISTENT.
    * 
    * @see javax.jms.DeliveryMode
     */
    protected void setMessageDeliveryMode(int nMode)
        {
        __m_MessageDeliveryMode = nMode;
        }
    
    // Accessor for the property "MessageExpiration"
    /**
     * Setter for property MessageExpiration.<p>
    * The expiration value of JMS Messages sent by the JmsInitiator and all
    * JmsConnections created by the JmsInitiator.
    * 
    * @see javax.jms.Message#getJMSExpiration
     */
    protected void setMessageExpiration(long cMillis)
        {
        __m_MessageExpiration = cMillis;
        }
    
    // Accessor for the property "MessagePriority"
    /**
     * Setter for property MessagePriority.<p>
    * The priority of JMS Messages sent by the JmsInitiator and all
    * JmsConnections created by the JmsInitiator.
    * 
    * @see javax.jms.Message#getJMSPriority
     */
    protected void setMessagePriority(int nPriority)
        {
        __m_MessagePriority = nPriority;
        }
    
    // Accessor for the property "QueueConnection"
    /**
     * Setter for property QueueConnection.<p>
    * The JMS QueueConnection used by this JmsInitiator.
     */
    protected void setQueueConnection(javax.jms.QueueConnection connection)
        {
        __m_QueueConnection = connection;
        }
    
    // Accessor for the property "QueueConnectionFactoryName"
    /**
     * Setter for property QueueConnectionFactoryName.<p>
    * The JNDI name of the JMS QueueConnectionFactory used by this JmsInitiator.
     */
    protected void setQueueConnectionFactoryName(String sName)
        {
        __m_QueueConnectionFactoryName = sName;
        }
    
    // Accessor for the property "QueueDiscovery"
    /**
     * Setter for property QueueDiscovery.<p>
    * The Queue used to send JMS Messages to an arbitrary JmsAcceptor. This
    * Queue is used by the JmsInitiator to establish a point-to-point
    * JmsConnection (via two TemporaryQueues) with a single JmsAcceptor.
    * 
    * @see #getQueueSender
     */
    protected void setQueueDiscovery(javax.jms.Queue queue)
        {
        __m_QueueDiscovery = queue;
        }
    
    // Accessor for the property "QueueName"
    /**
     * Setter for property QueueName.<p>
    * The JNDI name of the JMS "discovery" Queue used by this JmsInitiator.
     */
    protected void setQueueName(String sName)
        {
        __m_QueueName = sName;
        }
    
    // Accessor for the property "QueueSender"
    /**
     * Setter for property QueueSender.<p>
    * The JMS QueueSender used by this JmsInitiator to send JMS Messages to the
    * Queue specified by the QueueDiscovery property.
    * 
    * @see #getQueueDiscovery
     */
    protected void setQueueSender(javax.jms.QueueSender sender)
        {
        __m_QueueSender = sender;
        }
    
    // Accessor for the property "QueueSession"
    /**
     * Setter for property QueueSession.<p>
    * The JMS QueueSession used by this JmsInitiator.
     */
    protected void setQueueSession(javax.jms.QueueSession session)
        {
        __m_QueueSession = session;
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.initiator.JmsInitiator$JmsConnection
    
    /**
     * Connection implementation that wraps a JMS QueueSession.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class JmsConnection
            extends    com.tangosol.coherence.component.net.extend.connection.JmsConnection
        {
        // ---- Fields declarations ----
        
        // Default constructor
        public JmsConnection()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public JmsConnection(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.initiator.JmsInitiator.JmsConnection();
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
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/peer/initiator/JmsInitiator$JmsConnection".replace('/', '.'));
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
         * Open the Connection.
         */
        public void open()
            {
            // import Component.Net.Extend.Util.JmsUtil;
            // import com.tangosol.net.messaging.ConnectionException;
            // import com.tangosol.util.Base;
            // import javax.jms.JMSException;
            // import javax.jms.Message;
            // import javax.jms.Queue as javax.jms.Queue;
            // import javax.jms.QueueReceiver;
            // import javax.jms.QueueSession;
            // import javax.jms.Session;
            
            JmsInitiator initiator = (JmsInitiator) getConnectionManager();
            _assert(initiator != null);
            
            QueueReceiver receiver = null;
            QueueSession  session  = null;
            javax.jms.Queue         queueIn  = null;
            
            try
                {
                // create a TemporaryQueue for the new JmsConnection; the JmsConnection
                // will use this javax.jms.Queue to receive incoming Messages
                queueIn = initiator.getQueueSession().createTemporaryQueue();
            
                // create a temporary QueueReceiver to receive the discovery reply
                receiver = initiator.getQueueSession().createReceiver(queueIn);
            
                // create a connection request Message
                Message message = initiator.getQueueSession().createMessage();
                message.setJMSDeliveryMode(initiator.getMessageDeliveryMode());
                message.setJMSExpiration(initiator.getMessageExpiration());
                message.setJMSPriority(initiator.getMessagePriority());
                message.setJMSReplyTo(queueIn);
            
                // send the connection request Message
                initiator.getQueueSender().send(message);
            
                // wait for a reply
                Message reply;
                javax.jms.Queue   queueOut;
                do
                    {
                    reply = receiver.receive(initiator.getConnectTimeout());
                    if (reply == null)
                        {
                        throw new JMSException("did not receive a reply within "
                                + initiator.getConnectTimeout() + " millis");
                        }
            
                    Object oQueue = reply.getJMSReplyTo();
                    if (oQueue instanceof javax.jms.Queue)
                        {
                        queueOut = (javax.jms.Queue) oQueue;
                        }
                    else
                        {
                        throw new JMSException("received an unexpected reply-to JMS"
                                + " Destination: " + oQueue);
                        }
                    }
                while (!Base.equals(message.getJMSMessageID(), reply.getJMSCorrelationID()));
                JmsUtil.close(receiver);
            
                // create a new Session for the JmsConnection; use an acknowledgement
                // mode of DUPS_OK_ACKNOWLEDGE, as the Channel will protect against
                // duplicate Messages
                session = initiator.getQueueConnection().createQueueSession(false,
                        Session.DUPS_OK_ACKNOWLEDGE);
            
                // configure the JmsConnection
                setQueueIn(queueIn);
                setQueueOut(queueOut);
                setQueueSession(session);
                setMessageDeliveryMode(initiator.getMessageDeliveryMode());
                setMessageExpiration(initiator.getMessageExpiration());
                setMessagePriority(initiator.getMessagePriority());
            
                _trace("Connected to " + queueOut, 3);
                }
            catch (JMSException e)
                {
                JmsUtil.close(receiver);
                JmsUtil.close(session);
                JmsUtil.delete(queueIn);    
                throw new ConnectionException("error opening JMS connection", e);
                }
            
            super.open();
            }
        }
    }
