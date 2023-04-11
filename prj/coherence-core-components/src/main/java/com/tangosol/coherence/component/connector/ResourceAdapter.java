
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.connector.ResourceAdapter

package com.tangosol.coherence.component.connector;

import com.tangosol.net.CacheFactory;
import com.tangosol.util.Base;
import com.tangosol.util.Listeners;
import jakarta.resource.ResourceException;
import jakarta.resource.spi.ConnectionEvent;
import jakarta.resource.spi.ConnectionEventListener;
import jakarta.resource.spi.ConnectionManager;
import jakarta.resource.spi.ConnectionRequestInfo;
import jakarta.resource.spi.ManagedConnectionFactory;
import java.io.PrintWriter;
import java.util.EventListener;
import java.util.Iterator;

/**
 * This component is a factory of both ManagedConnection and EIS-specific
 * connection factory instances. Default implementation assumes existence of
 * the following static children components:
 * <ul>
 * <li>ConnectionFactory
 * <li>ManagedConnection
 * <li>DefaultConnectionManager
 * </ul>
 * 
 * @see javax.resource.spi.ManagedConnectionFactory
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public abstract class ResourceAdapter
        extends    com.tangosol.coherence.component.Connector
        implements jakarta.resource.spi.ManagedConnectionFactory
    {
    // ---- Fields declarations ----
    
    /**
     * Property LogWriter
     *
     */
    private transient java.io.PrintWriter __m_LogWriter;
    
    /**
     * Property Strict
     *
     * Specifies whether or not the implementation will strictly adhere to the
     * J2CA specification.
     * 
     * Configurable property; @see ra.xml
     */
    private Boolean __m_Strict;
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
        __mapChildren.put("ConnectionFactory", ResourceAdapter.ConnectionFactory.get_CLASS());
        __mapChildren.put("DefaultConnectionManager", ResourceAdapter.DefaultConnectionManager.get_CLASS());
        __mapChildren.put("ManagedConnection", ResourceAdapter.ManagedConnection.get_CLASS());
        }
    
    // Initializing constructor
    public ResourceAdapter(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            clz = Class.forName("com.tangosol.coherence/component/connector/ResourceAdapter".replace('/', '.'));
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
    
    // From interface: jakarta.resource.spi.ManagedConnectionFactory
    /**
     * Creates a connection factory instance. The connection factory instance
    * gets initialized with a default ConnectionManager provided by the
    * resource adapter.
    * 
    * @return a specific connection factory instance or
    * javax.resource.cci.ConnectionFactory instance
    * 
    * @exception javax.resource.ResourceException generic exception
    * @exception javax.resource.spi.ResourceAdapterInternalException  resource
    * adapter related error condition
     */
    public Object createConnectionFactory()
            throws jakarta.resource.ResourceException
        {
        // this method is used in a non-managed application scenario
        return createConnectionFactory(null);
        }
    
    // From interface: jakarta.resource.spi.ManagedConnectionFactory
    public Object createConnectionFactory(jakarta.resource.spi.ConnectionManager cxManager)
            throws jakarta.resource.ResourceException
        {
        ResourceAdapter.ConnectionFactory cxFactory = (ResourceAdapter.ConnectionFactory) _newChild("ConnectionFactory");
        
        cxFactory.setConnectionManager(cxManager);
        
        return cxFactory;
        }
    
    // From interface: jakarta.resource.spi.ManagedConnectionFactory
    public jakarta.resource.spi.ManagedConnection createManagedConnection(javax.security.auth.Subject subject, jakarta.resource.spi.ConnectionRequestInfo cxRequestInfo)
            throws jakarta.resource.ResourceException
        {
        ResourceAdapter.ManagedConnection connection = (ResourceAdapter.ManagedConnection) _newChild("ManagedConnection");
        
        connection.authenticate(subject, cxRequestInfo);
        
        return connection;
        }
    
    // From interface: jakarta.resource.spi.ManagedConnectionFactory
    // Accessor for the property "LogWriter"
    /**
     * Getter for property LogWriter.<p>
     */
    public java.io.PrintWriter getLogWriter()
        {
        return __m_LogWriter;
        }
    
    // Accessor for the property "Strict"
    /**
     * Getter for property Strict.<p>
    * Specifies whether or not the implementation will strictly adhere to the
    * J2CA specification.
    * 
    * Configurable property; @see ra.xml
     */
    public Boolean getStrict()
        {
        return __m_Strict;
        }
    
    public void log(String sMsg, int nLevel)
        {
        // import com.tangosol.net.CacheFactory;
        // import java.io.PrintWriter;
        
        PrintWriter pw = getLogWriter();
        if (pw != null)
            {
            pw.println(Thread.currentThread() + ": " + sMsg);
            }
        else
            {
            CacheFactory.log(sMsg, nLevel);
            }
        }
    
    // From interface: jakarta.resource.spi.ManagedConnectionFactory
    public jakarta.resource.spi.ManagedConnection matchManagedConnections(java.util.Set setConnection, javax.security.auth.Subject subject, jakarta.resource.spi.ConnectionRequestInfo info)
            throws jakarta.resource.ResourceException
        {
        // import java.util.Iterator;
        
        for (Iterator iter = setConnection.iterator(); iter.hasNext();)
            {
            Object o = iter.next();
        
            if (o instanceof ResourceAdapter.ManagedConnection)
                {
                ResourceAdapter.ManagedConnection connection = (ResourceAdapter.ManagedConnection) o;
                if (connection.matches(subject, info))
                    {
                    return connection;
                    }
                }
            }
        
        return null;
        }
    
    // From interface: jakarta.resource.spi.ManagedConnectionFactory
    // Accessor for the property "LogWriter"
    /**
     * Setter for property LogWriter.<p>
     */
    public void setLogWriter(java.io.PrintWriter out)
        {
        __m_LogWriter = out;
        }
    
    // Accessor for the property "Strict"
    /**
     * Setter for property Strict.<p>
    * Specifies whether or not the implementation will strictly adhere to the
    * J2CA specification.
    * 
    * Configurable property; @see ra.xml
     */
    public void setStrict(Boolean fStrict)
        {
        __m_Strict = fStrict;
        }
    
    // Declared at the super level
    public String toString()
        {
        return get_Name() + "@" + hashCode();
        }

    // ---- class: com.tangosol.coherence.component.connector.ResourceAdapter$ConnectionFactory
    
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class ConnectionFactory
            extends    com.tangosol.coherence.component.Connector
        {
        // ---- Fields declarations ----
        
        /**
         * Property ConnectionManager
         *
         * ConnectionManager associated with this connection factory instance. 
         * In the managed scenario, ConnectionManager is provided by an
         * application server.  It provides a hook for a resource adapter to
         * pass a connection request to an application server.
         * 
         * @see ResourceAdapter#createConnectionFactory(ConnectionManager)
         */
        private jakarta.resource.spi.ConnectionManager __m_ConnectionManager;
        
        // Default constructor
        public ConnectionFactory()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public ConnectionFactory(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.connector.ResourceAdapter.ConnectionFactory();
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
                clz = Class.forName("com.tangosol.coherence/component/connector/ResourceAdapter$ConnectionFactory".replace('/', '.'));
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
        
        // Accessor for the property "ConnectionManager"
        /**
         * Getter for property ConnectionManager.<p>
        * ConnectionManager associated with this connection factory instance. 
        * In the managed scenario, ConnectionManager is provided by an
        * application server.  It provides a hook for a resource adapter to
        * pass a connection request to an application server.
        * 
        * @see ResourceAdapter#createConnectionFactory(ConnectionManager)
         */
        public jakarta.resource.spi.ConnectionManager getConnectionManager()
            {
            // import jakarta.resource.spi.ConnectionManager;
            
            ConnectionManager cm = __m_ConnectionManager;
            if (cm == null)
                {
                cm = (ResourceAdapter.DefaultConnectionManager) get_Parent()._newChild("DefaultConnectionManager");
                setConnectionManager(cm);
                }
            return cm;
            }
        
        // Accessor for the property "ManagedConnectionFactory"
        /**
         * Getter for property ManagedConnectionFactory.<p>
        * ManagedConnectionFactory that created this connection factory
        * instance.
        * 
        * @see ResourceAdapter#createConnectionFactory()
         */
        public jakarta.resource.spi.ManagedConnectionFactory getManagedConnectionFactory()
            {
            // import jakarta.resource.spi.ManagedConnectionFactory;
            
            return (ManagedConnectionFactory) get_Parent();
            }
        
        // Accessor for the property "ConnectionManager"
        /**
         * Setter for property ConnectionManager.<p>
        * ConnectionManager associated with this connection factory instance. 
        * In the managed scenario, ConnectionManager is provided by an
        * application server.  It provides a hook for a resource adapter to
        * pass a connection request to an application server.
        * 
        * @see ResourceAdapter#createConnectionFactory(ConnectionManager)
         */
        public void setConnectionManager(jakarta.resource.spi.ConnectionManager manager)
            {
            __m_ConnectionManager = manager;
            }
        }

    // ---- class: com.tangosol.coherence.component.connector.ResourceAdapter$DefaultConnectionManager
    
    /**
     * The default ConnectionManager implementation for the non-managed
     * scenario. It provieds a hook for a resource adapter to pass a connection
     * request to an application server.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class DefaultConnectionManager
            extends    com.tangosol.coherence.component.Connector
            implements jakarta.resource.spi.ConnectionManager
        {
        // ---- Fields declarations ----
        
        // Default constructor
        public DefaultConnectionManager()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public DefaultConnectionManager(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.connector.ResourceAdapter.DefaultConnectionManager();
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
                clz = Class.forName("com.tangosol.coherence/component/connector/ResourceAdapter$DefaultConnectionManager".replace('/', '.'));
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
        
        // From interface: jakarta.resource.spi.ConnectionManager
        public Object allocateConnection(jakarta.resource.spi.ManagedConnectionFactory mcf, jakarta.resource.spi.ConnectionRequestInfo info)
                throws jakarta.resource.ResourceException
            {
            // import jakarta.resource.spi.ManagedConnection as jakarta.resource.spi.ManagedConnection;
            
            jakarta.resource.spi.ManagedConnection connection = mcf.createManagedConnection(null, info);
            
            return connection.getConnection(null, info);
            }
        }

    // ---- class: com.tangosol.coherence.component.connector.ResourceAdapter$ManagedConnection
    
    /**
     * ManagedConnection represents a physical connection to
     * an underlying EIS (Chapter 5.5.4 of JCA 1.0 specification)
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static abstract class ManagedConnection
            extends    com.tangosol.coherence.component.Connector
            implements jakarta.resource.spi.ManagedConnection
        {
        // ---- Fields declarations ----
        
        /**
         * Property Closed
         *
         */
        private transient boolean __m_Closed;
        
        /**
         * Property ConnectionInfo
         *
         * Placeholder for a RequestInfo object associated with this MC
         */
        private transient jakarta.resource.spi.ConnectionRequestInfo __m_ConnectionInfo;
        
        /**
         * Property ConnectionListeners
         *
         */
        private transient com.tangosol.util.Listeners __m_ConnectionListeners;
        
        /**
         * Property LogWriter
         *
         */
        private transient java.io.PrintWriter __m_LogWriter;
        
        /**
         * Property Subject
         *
         * Placeholder for a Subject object associated with this MC
         */
        private transient javax.security.auth.Subject __m_Subject;
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
            __mapChildren.put("LocalTransaction", ResourceAdapter.ManagedConnection.LocalTransaction.get_CLASS());
            __mapChildren.put("XAResource", ResourceAdapter.ManagedConnection.XAResource.get_CLASS());
            }
        
        // Initializing constructor
        public ManagedConnection(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
        * Property with auto-generated accessor that returns the Class object
        * for a given component.
         */
        public static Class get_CLASS()
            {
            Class clz;
            try
                {
                clz = Class.forName("com.tangosol.coherence/component/connector/ResourceAdapter$ManagedConnection".replace('/', '.'));
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
        
        // From interface: jakarta.resource.spi.ManagedConnection
        public void addConnectionEventListener(jakarta.resource.spi.ConnectionEventListener l)
            {
            // import com.tangosol.util.Listeners;
            // import jakarta.resource.spi.ConnectionEventListener;
            
            if (l == null)
                {
                return;
                }
            Listeners listeners = getConnectionListeners();
            if (listeners == null)
                {
                setConnectionListeners(listeners = new Listeners());
                }
            listeners.add(l);
            }
        
        // From interface: jakarta.resource.spi.ManagedConnection
        public void associateConnection(Object connection)
                throws jakarta.resource.ResourceException
            {
            }
        
        /**
         * Check the connection subject and info.
        * 
        * @see ResourceAdapter#createManagedConnection
         */
        public void authenticate(javax.security.auth.Subject subject, jakarta.resource.spi.ConnectionRequestInfo cxInfo)
                throws jakarta.resource.ResourceException
            {
            setSubject(subject);
            setConnectionInfo(cxInfo);
            }
        
        protected void checkStatus()
            {
            if (isClosed())
                {
                throw new IllegalStateException(get_Name() + " is closed");
                }
            }
        
        // From interface: jakarta.resource.spi.ManagedConnection
        public void cleanup()
                throws jakarta.resource.ResourceException
            {
            checkStatus();
            }
        
        // From interface: jakarta.resource.spi.ManagedConnection
        public void destroy()
                throws jakarta.resource.ResourceException
            {
            setClosed(true);
            }
        
        public void fireConnectionEvent(int nEventType)
            {
            fireConnectionEvent(nEventType, null, null);
            }
        
        public void fireConnectionEvent(int nEventType, Exception exception)
            {
            fireConnectionEvent(nEventType, exception, null);
            }
        
        public void fireConnectionEvent(int nEventType, Exception exception, Object oConnectionHandle)
            {
            // import com.tangosol.util.Listeners;
            // import java.util.EventListener;
            // import jakarta.resource.spi.ConnectionEvent;
            // import jakarta.resource.spi.ConnectionEventListener;
            
            Listeners listeners = getConnectionListeners();
            if (listeners != null)
                {
                ConnectionEvent ce = exception == null ?
                    new ConnectionEvent(this, nEventType) :
                    new ConnectionEvent(this, nEventType, exception);
            
                if (oConnectionHandle != null)
                    {
                    ce.setConnectionHandle(oConnectionHandle);
                    }
            
                EventListener[] targets = listeners.listeners();
                for (int i = targets.length; --i >= 0;)
                    {
                    ConnectionEventListener target = (ConnectionEventListener) targets[i];
            
                    switch (nEventType)
                        {
                        case ConnectionEvent.CONNECTION_CLOSED:
                            target.connectionClosed(ce);
                            break;
                        case ConnectionEvent.LOCAL_TRANSACTION_STARTED:
                            target.localTransactionStarted(ce);
                            break;
                        case ConnectionEvent.LOCAL_TRANSACTION_COMMITTED:
                            target.localTransactionCommitted(ce);
                            break;
                        case ConnectionEvent.LOCAL_TRANSACTION_ROLLEDBACK:
                            target.localTransactionRolledback(ce);
                            break;
                        case ConnectionEvent.CONNECTION_ERROR_OCCURRED:
                            target.connectionErrorOccurred(ce);
                            break;
                        default:
                            throw new IllegalArgumentException("Illegal eventType: " +
                                nEventType);
                       }
                    }
                }
            }
        
        // From interface: jakarta.resource.spi.ManagedConnection
        public Object getConnection(javax.security.auth.Subject subject, jakarta.resource.spi.ConnectionRequestInfo cxInfo)
                throws jakarta.resource.ResourceException
            {
            return null;
            }
        
        // Accessor for the property "ConnectionInfo"
        /**
         * Getter for property ConnectionInfo.<p>
        * Placeholder for a RequestInfo object associated with this MC
         */
        public jakarta.resource.spi.ConnectionRequestInfo getConnectionInfo()
            {
            return __m_ConnectionInfo;
            }
        
        // Accessor for the property "ConnectionListeners"
        /**
         * Getter for property ConnectionListeners.<p>
         */
        protected com.tangosol.util.Listeners getConnectionListeners()
            {
            return __m_ConnectionListeners;
            }
        
        // From interface: jakarta.resource.spi.ManagedConnection
        public jakarta.resource.spi.LocalTransaction getLocalTransaction()
                throws jakarta.resource.ResourceException
            {
            checkStatus();
            
            return (ResourceAdapter.ManagedConnection.LocalTransaction) _newChild("LocalTransaction");
            }
        
        // From interface: jakarta.resource.spi.ManagedConnection
        // Accessor for the property "LogWriter"
        /**
         * Getter for property LogWriter.<p>
         */
        public java.io.PrintWriter getLogWriter()
                throws jakarta.resource.ResourceException
            {
            return __m_LogWriter;
            }
        
        // From interface: jakarta.resource.spi.ManagedConnection
        public jakarta.resource.spi.ManagedConnectionMetaData getMetaData()
                throws jakarta.resource.ResourceException
            {
            checkStatus();
            
            return (ResourceAdapter.ManagedConnection.ManagedConnectionMetaData) _findChild("ManagedConnectionMetaData");
            }
        
        // Accessor for the property "Subject"
        /**
         * Getter for property Subject.<p>
        * Placeholder for a Subject object associated with this MC
         */
        public javax.security.auth.Subject getSubject()
            {
            return __m_Subject;
            }
        
        // From interface: jakarta.resource.spi.ManagedConnection
        public javax.transaction.xa.XAResource getXAResource()
                throws jakarta.resource.ResourceException
            {
            checkStatus();
            
            return (ResourceAdapter.ManagedConnection.XAResource) _newChild("XAResource");
            }
        
        // Accessor for the property "Closed"
        /**
         * Getter for property Closed.<p>
         */
        public boolean isClosed()
            {
            return __m_Closed;
            }
        
        public void log(String sMsg, int nLevel)
                throws jakarta.resource.ResourceException
            {
            // import java.io.PrintWriter;
            
            PrintWriter pw = getLogWriter();
            if (pw != null)
                {
                pw.println(sMsg);
                }
            else
                {
                ((ResourceAdapter) get_Module()).log(sMsg, nLevel);
                }
            }
        
        /**
         * Checks whether or not this connection object matches to the specified
        * security info (subject) and connection request information (info)
         */
        public boolean matches(javax.security.auth.Subject subject, jakarta.resource.spi.ConnectionRequestInfo cxInfo)
            {
            // import com.tangosol.util.Base;
            
            return Base.equals(subject, getSubject()) &&
                   Base.equals(cxInfo,  getConnectionInfo());
            }
        
        // From interface: jakarta.resource.spi.ManagedConnection
        public void removeConnectionEventListener(jakarta.resource.spi.ConnectionEventListener l)
            {
            // import com.tangosol.util.Listeners;
            // import jakarta.resource.spi.ConnectionEventListener;
            
            Listeners listeners = getConnectionListeners();
            if (listeners != null)
                {
                listeners.remove(l);
            
                if (listeners.listeners().length == 0)
                    {
                    setConnectionListeners(null);
                    }
                }
            }
        
        // Accessor for the property "Closed"
        /**
         * Setter for property Closed.<p>
         */
        protected void setClosed(boolean fClosed)
            {
            __m_Closed = fClosed;
            }
        
        // Accessor for the property "ConnectionInfo"
        /**
         * Setter for property ConnectionInfo.<p>
        * Placeholder for a RequestInfo object associated with this MC
         */
        public void setConnectionInfo(jakarta.resource.spi.ConnectionRequestInfo pConnectionInfo)
            {
            __m_ConnectionInfo = pConnectionInfo;
            }
        
        // Accessor for the property "ConnectionListeners"
        /**
         * Setter for property ConnectionListeners.<p>
         */
        protected void setConnectionListeners(com.tangosol.util.Listeners liisteners)
            {
            __m_ConnectionListeners = liisteners;
            }
        
        // From interface: jakarta.resource.spi.ManagedConnection
        // Accessor for the property "LogWriter"
        /**
         * Setter for property LogWriter.<p>
         */
        public void setLogWriter(java.io.PrintWriter out)
                throws jakarta.resource.ResourceException
            {
            __m_LogWriter = out;
            }
        
        // Accessor for the property "Subject"
        /**
         * Setter for property Subject.<p>
        * Placeholder for a Subject object associated with this MC
         */
        public void setSubject(javax.security.auth.Subject subject)
            {
            __m_Subject = subject;
            }
        
        // Declared at the super level
        public String toString()
            {
            return get_Name() + "@" + hashCode() + ": cxInfo=" + getConnectionInfo();
            }

        // ---- class: com.tangosol.coherence.component.connector.ResourceAdapter$ManagedConnection$LocalTransaction
        
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static abstract class LocalTransaction
                extends    com.tangosol.coherence.component.Connector
                implements jakarta.resource.spi.LocalTransaction
            {
            // ---- Fields declarations ----
            
            // Initializing constructor
            public LocalTransaction(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            * Property with auto-generated accessor that returns the Class
            * object for a given component.
             */
            public static Class get_CLASS()
                {
                Class clz;
                try
                    {
                    clz = Class.forName("com.tangosol.coherence/component/connector/ResourceAdapter$ManagedConnection$LocalTransaction".replace('/', '.'));
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
            
            // From interface: jakarta.resource.spi.LocalTransaction
            public void begin()
                    throws jakarta.resource.ResourceException
                {
                }
            
            // From interface: jakarta.resource.spi.LocalTransaction
            public void commit()
                    throws jakarta.resource.ResourceException
                {
                }
            
            // Accessor for the property "ManagedConnection"
            /**
             * Getter for property ManagedConnection.<p>
             */
            public ResourceAdapter.ManagedConnection getManagedConnection()
                {
                return (ResourceAdapter.ManagedConnection) get_Parent();
                }
            
            // From interface: jakarta.resource.spi.LocalTransaction
            public void rollback()
                    throws jakarta.resource.ResourceException
                {
                }
            }

        // ---- class: com.tangosol.coherence.component.connector.ResourceAdapter$ManagedConnection$ManagedConnectionMetaData
        
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class ManagedConnectionMetaData
                extends    com.tangosol.coherence.component.Data
                implements jakarta.resource.spi.ManagedConnectionMetaData
            {
            // ---- Fields declarations ----
            
            /**
             * Property EISProductName
             *
             */
            private String __m_EISProductName;
            
            /**
             * Property EISProductVersion
             *
             */
            private String __m_EISProductVersion;
            
            /**
             * Property MaxConnections
             *
             */
            private int __m_MaxConnections;
            
            // Default constructor
            public ManagedConnectionMetaData()
                {
                this(null, null, true);
                }
            
            // Initializing constructor
            public ManagedConnectionMetaData(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                return new com.tangosol.coherence.component.connector.ResourceAdapter.ManagedConnection.ManagedConnectionMetaData();
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
                    clz = Class.forName("com.tangosol.coherence/component/connector/ResourceAdapter$ManagedConnection$ManagedConnectionMetaData".replace('/', '.'));
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
            public boolean equals(Object obj)
                {
                if (obj instanceof ResourceAdapter.ManagedConnection.ManagedConnectionMetaData)
                    {
                    ResourceAdapter.ManagedConnection.ManagedConnectionMetaData that = (ResourceAdapter.ManagedConnection.ManagedConnectionMetaData) obj;
                    
                    return getEISProductName()   .equals(that.getEISProductName()) &&
                           getEISProductVersion().equals(that.getEISProductVersion());
                    }
                return false;
                }
            
            // From interface: jakarta.resource.spi.ManagedConnectionMetaData
            // Accessor for the property "EISProductName"
            /**
             * Getter for property EISProductName.<p>
             */
            public String getEISProductName()
                {
                return __m_EISProductName;
                }
            
            // From interface: jakarta.resource.spi.ManagedConnectionMetaData
            // Accessor for the property "EISProductVersion"
            /**
             * Getter for property EISProductVersion.<p>
             */
            public String getEISProductVersion()
                {
                return __m_EISProductVersion;
                }
            
            // From interface: jakarta.resource.spi.ManagedConnectionMetaData
            // Accessor for the property "MaxConnections"
            /**
             * Getter for property MaxConnections.<p>
             */
            public int getMaxConnections()
                {
                return __m_MaxConnections;
                }
            
            // From interface: jakarta.resource.spi.ManagedConnectionMetaData
            // Accessor for the property "UserName"
            /**
             * Getter for property UserName.<p>
             */
            public String getUserName()
                    throws jakarta.resource.ResourceException
                {
                // import jakarta.resource.spi.ConnectionRequestInfo;
                // import jakarta.resource.ResourceException;
                
                ResourceAdapter.ManagedConnection mc = (ResourceAdapter.ManagedConnection) get_Parent();
                
                if (mc.isClosed())
                    {
                    throw new ResourceException("ManagedConnection has been destroyed");
                    }
                
                ConnectionRequestInfo cxInfo = mc.getConnectionInfo();
                return cxInfo instanceof ConnectionInfo ?
                    ((ConnectionInfo) cxInfo).getUserName() : null;
                }
            
            // Declared at the super level
            public int hashCode()
                {
                return getEISProductName().hashCode() + getEISProductVersion().hashCode();
                }
            
            // Accessor for the property "EISProductName"
            /**
             * Setter for property EISProductName.<p>
             */
            public void setEISProductName(String pEISProductName)
                {
                __m_EISProductName = pEISProductName;
                }
            
            // Accessor for the property "EISProductVersion"
            /**
             * Setter for property EISProductVersion.<p>
             */
            public void setEISProductVersion(String pEISProductVersion)
                {
                __m_EISProductVersion = pEISProductVersion;
                }
            
            // Accessor for the property "MaxConnections"
            /**
             * Setter for property MaxConnections.<p>
             */
            public void setMaxConnections(int pMaxConnections)
                {
                __m_MaxConnections = pMaxConnections;
                }
            }

        // ---- class: com.tangosol.coherence.component.connector.ResourceAdapter$ManagedConnection$XAResource
        
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class XAResource
                extends    com.tangosol.coherence.component.Connector
                implements javax.transaction.xa.XAResource
            {
            // ---- Fields declarations ----
            
            /**
             * Property Timeout
             *
             */
            private int __m_Timeout;
            
            // Default constructor
            public XAResource()
                {
                this(null, null, true);
                }
            
            // Initializing constructor
            public XAResource(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                return new com.tangosol.coherence.component.connector.ResourceAdapter.ManagedConnection.XAResource();
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
                    clz = Class.forName("com.tangosol.coherence/component/connector/ResourceAdapter$ManagedConnection$XAResource".replace('/', '.'));
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
            
            // From interface: javax.transaction.xa.XAResource
            public void commit(javax.transaction.xa.Xid xid, boolean fOnePhase)
                    throws javax.transaction.xa.XAException
                {
                }
            
            // From interface: javax.transaction.xa.XAResource
            public void end(javax.transaction.xa.Xid xid, int iFlags)
                    throws javax.transaction.xa.XAException
                {
                }
            
            // From interface: javax.transaction.xa.XAResource
            public void forget(javax.transaction.xa.Xid xid)
                    throws javax.transaction.xa.XAException
                {
                }
            
            // Accessor for the property "ManagedConnection"
            /**
             * Getter for property ManagedConnection.<p>
             */
            public ResourceAdapter.ManagedConnection getManagedConnection()
                {
                return (ResourceAdapter.ManagedConnection) get_Parent();
                }
            
            // Accessor for the property "Timeout"
            /**
             * Getter for property Timeout.<p>
             */
            public int getTimeout()
                {
                return __m_Timeout;
                }
            
            // From interface: javax.transaction.xa.XAResource
            public int getTransactionTimeout()
                    throws javax.transaction.xa.XAException
                {
                return getTimeout();
                }
            
            // From interface: javax.transaction.xa.XAResource
            public boolean isSameRM(javax.transaction.xa.XAResource resource)
                    throws javax.transaction.xa.XAException
                {
                return this == resource;
                }
            
            // From interface: javax.transaction.xa.XAResource
            public int prepare(javax.transaction.xa.Xid xid)
                    throws javax.transaction.xa.XAException
                {
                return 0;
                }
            
            // From interface: javax.transaction.xa.XAResource
            public javax.transaction.xa.Xid[] recover(int iFlags)
                    throws javax.transaction.xa.XAException
                {
                return null;
                }
            
            // From interface: javax.transaction.xa.XAResource
            public void rollback(javax.transaction.xa.Xid xid)
                    throws javax.transaction.xa.XAException
                {
                }
            
            // Accessor for the property "Timeout"
            /**
             * Setter for property Timeout.<p>
             */
            public void setTimeout(int pTimeout)
                {
                __m_Timeout = pTimeout;
                }
            
            // From interface: javax.transaction.xa.XAResource
            public boolean setTransactionTimeout(int nSeconds)
                    throws javax.transaction.xa.XAException
                {
                setTimeout(nSeconds);
                return true;
                }
            
            // From interface: javax.transaction.xa.XAResource
            public void start(javax.transaction.xa.Xid xid, int iFlags)
                    throws javax.transaction.xa.XAException
                {
                }
            }
        }
    }
