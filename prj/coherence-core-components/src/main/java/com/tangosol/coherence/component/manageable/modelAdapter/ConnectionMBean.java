
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.manageable.modelAdapter.ConnectionMBean

package com.tangosol.coherence.component.manageable.modelAdapter;

/**
 * ConnectionMBean contains statistics and usage information for a remote
 * client connection.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class ConnectionMBean
        extends    com.tangosol.coherence.component.manageable.ModelAdapter
    {
    // ---- Fields declarations ----
    
    /**
     * Property MessagingDebug
     *
     * Debug flag.  When true and the node's logging level is 6 or higher, sent
     * and received messages will be logged for this connection.   If this
     * attribute in the ConnectionManager MBean is true and the node's logging
     * level is 6 or higher, sent and received messages will also be logged.
     */
    private transient boolean __m_MessagingDebug;
    
    // Default constructor
    public ConnectionMBean()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public ConnectionMBean(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
        return new com.tangosol.coherence.component.manageable.modelAdapter.ConnectionMBean();
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
            clz = Class.forName("com.tangosol.coherence/component/manageable/modelAdapter/ConnectionMBean".replace('/', '.'));
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
    /**
     * Auto-generated for concrete Components.
     */
    protected Object[] get_ComponentInfo()
        {
        return new Object[]
            {
            "ConnectionMBean contains statistics and usage information for a remote client connection.",
            null,
            };
        }
    /**
     * Auto-generated for concrete Components, for Properties that have:
    * 
    * - instance scope
    * - manual origin; not from super
    * - name _not_ starting with underscore
    * - at least one public accessor
     */
    protected java.util.Map get_PropertyInfo()
        {
        java.util.Map mapInfo = super.get_PropertyInfo();
        
        // property ClientAddress
            {
            mapInfo.put("ClientAddress", new Object[]
                {
                "The client's IP address.",
                "getClientAddress",
                null,
                "Ljava/lang/String;",
                "metrics.tag=clientAddress",
                });
            }
        
        // property ClientProcessName
            {
            mapInfo.put("ClientProcessName", new Object[]
                {
                "The client process name.",
                "getClientProcessName",
                null,
                "Ljava/lang/String;",
                "metrics.tag=clientProcessName",
                });
            }
        
        // property ClientRole
            {
            mapInfo.put("ClientRole", new Object[]
                {
                "The client role.",
                "getClientRole",
                null,
                "Ljava/lang/String;",
                "metrics.tag=clientRole",
                });
            }
        
        // property ConnectionTimeMillis
            {
            mapInfo.put("ConnectionTimeMillis", new Object[]
                {
                "The time duration (in milliseconds) that the client has been connected.",
                "getConnectionTimeMillis",
                null,
                "J",
                "metrics.value=_default",
                });
            }
        
        // property Member
            {
            mapInfo.put("Member", new Object[]
                {
                "Member information of the client connection.",
                "getMember",
                null,
                "Ljava/lang/String;",
                null,
                });
            }
        
        // property MessagingDebug
            {
            mapInfo.put("MessagingDebug", new Object[]
                {
                "Debug flag.  When true and the node's logging level is 6 or higher, sent and received messages will be logged for this connection.   If this attribute in the ConnectionManager MBean is true and the node's logging level is 6 or higher, sent and received messages will also be logged.",
                "isMessagingDebug",
                "setMessagingDebug",
                "Z",
                null,
                });
            }
        
        // property OutgoingByteBacklog
            {
            mapInfo.put("OutgoingByteBacklog", new Object[]
                {
                "The backlog (in bytes) of the outgoing queue",
                "getOutgoingByteBacklog",
                null,
                "J",
                "metrics.value=_default",
                });
            }
        
        // property OutgoingMessageBacklog
            {
            mapInfo.put("OutgoingMessageBacklog", new Object[]
                {
                "The backlog of the outgoing message queue.",
                "getOutgoingMessageBacklog",
                null,
                "I",
                "metrics.value=_default",
                });
            }
        
        // property RemoteAddress
            {
            mapInfo.put("RemoteAddress", new Object[]
                {
                "The IP address of the corresponding client.",
                "getRemoteAddress",
                null,
                "Ljava/lang/String;",
                "metrics.tag=remoteAddress",
                });
            }
        
        // property RemotePort
            {
            mapInfo.put("RemotePort", new Object[]
                {
                "The port of the corresponding client.",
                "getRemotePort",
                null,
                "I",
                "metrics.tag=remotePort",
                });
            }
        
        // property Timestamp
            {
            mapInfo.put("Timestamp", new Object[]
                {
                "The date/time value (in local time) that the corresponding client connected to the Proxy.",
                "getTimestamp",
                null,
                "Ljava/util/Date;",
                null,
                });
            }
        
        // property TotalBytesReceived
            {
            mapInfo.put("TotalBytesReceived", new Object[]
                {
                "The total number of bytes received since the last time the statistics were reset.",
                "getTotalBytesReceived",
                null,
                "J",
                "metrics.value=_default",
                });
            }
        
        // property TotalBytesSent
            {
            mapInfo.put("TotalBytesSent", new Object[]
                {
                "The total number of bytes sent since the last time the statistics were reset.",
                "getTotalBytesSent",
                null,
                "J",
                "metrics.value=_default",
                });
            }
        
        // property TotalMessagesReceived
            {
            mapInfo.put("TotalMessagesReceived", new Object[]
                {
                "The total number of messages received since the last time the statistics were reset.",
                "getTotalMessagesReceived",
                null,
                "J",
                "metrics.value=_default",
                });
            }
        
        // property TotalMessagesSent
            {
            mapInfo.put("TotalMessagesSent", new Object[]
                {
                "The total number of messages sent since the last time the statistics were reset.",
                "getTotalMessagesSent",
                null,
                "J",
                "metrics.value=_default",
                });
            }
        
        // property UUID
            {
            mapInfo.put("UUID", new Object[]
                {
                "The unique identifier for this connection.",
                "getUUID",
                null,
                "Ljava/lang/String;",
                null,
                });
            }
        
        return mapInfo;
        }
    /**
     * Auto-generated for concrete Components, for Behaviors that have:
    * 
    * - instance scope
    * - manual origin; not from super
    * - name _not_ starting with underscore
    * - public access
     */
    protected java.util.Map get_MethodInfo()
        {
        java.util.Map mapInfo = super.get_MethodInfo();
        
        // behavior closeConnection()
            {
            mapInfo.put("closeConnection()", new Object[]
                {
                "Close the corresponding connection.",
                "closeConnection",
                "V",
                new String[] {},
                new String[] {},
                null,
                });
            }
        
        // behavior resetStatistics()
            {
            mapInfo.put("resetStatistics()", new Object[]
                {
                "Reset statistics.",
                "resetStatistics",
                "V",
                new String[] {},
                new String[] {},
                null,
                });
            }
        
        return mapInfo;
        }
    
    /**
     * Close the corresponding connection.
     */
    public void closeConnection()
        {
        }
    
    // Accessor for the property "ClientAddress"
    /**
     * Getter for property ClientAddress.<p>
    * The client's IP address.
    * 
    * @descriptor metrics.tag=clientAddress
     */
    public String getClientAddress()
        {
        return null;
        }
    
    // Accessor for the property "ClientProcessName"
    /**
     * Getter for property ClientProcessName.<p>
    * The client process name.
    * 
    * @descriptor metrics.tag=clientProcessName
     */
    public String getClientProcessName()
        {
        return null;
        }
    
    // Accessor for the property "ClientRole"
    /**
     * Getter for property ClientRole.<p>
    * The client role.
    * 
    * @descriptor metrics.tag=clientRole
     */
    public String getClientRole()
        {
        return null;
        }
    
    // Accessor for the property "ConnectionTimeMillis"
    /**
     * Getter for property ConnectionTimeMillis.<p>
    * The time duration (in milliseconds) that the client has been connected.
    * 
    * @descriptor metrics.value=_default
     */
    public long getConnectionTimeMillis()
        {
        return 0L;
        }
    
    // Accessor for the property "Member"
    /**
     * Getter for property Member.<p>
    * Member information of the client connection.
     */
    public String getMember()
        {
        return null;
        }
    
    // Accessor for the property "OutgoingByteBacklog"
    /**
     * Getter for property OutgoingByteBacklog.<p>
    * The backlog (in bytes) of the outgoing queue
    * 
    * @descriptor metrics.value=_default
     */
    public long getOutgoingByteBacklog()
        {
        return 0L;
        }
    
    // Accessor for the property "OutgoingMessageBacklog"
    /**
     * Getter for property OutgoingMessageBacklog.<p>
    * The backlog of the outgoing message queue.
    * 
    * @descriptor metrics.value=_default
     */
    public int getOutgoingMessageBacklog()
        {
        return 0;
        }
    
    // Accessor for the property "RemoteAddress"
    /**
     * Getter for property RemoteAddress.<p>
    * The IP address of the corresponding client.
    * 
    * @descriptor metrics.tag=remoteAddress
     */
    public String getRemoteAddress()
        {
        return null;
        }
    
    // Accessor for the property "RemotePort"
    /**
     * Getter for property RemotePort.<p>
    * The port of the corresponding client.
    * 
    * @descriptor metrics.tag=remotePort
     */
    public int getRemotePort()
        {
        return 0;
        }
    
    // Accessor for the property "Timestamp"
    /**
     * Getter for property Timestamp.<p>
    * The date/time value (in local time) that the corresponding client
    * connected to the Proxy.
     */
    public java.util.Date getTimestamp()
        {
        return null;
        }
    
    // Accessor for the property "TotalBytesReceived"
    /**
     * Getter for property TotalBytesReceived.<p>
    * The total number of bytes received since the last time the statistics
    * were reset.
    * 
    * @descriptor metrics.value=_default
     */
    public long getTotalBytesReceived()
        {
        return 0L;
        }
    
    // Accessor for the property "TotalBytesSent"
    /**
     * Getter for property TotalBytesSent.<p>
    * The total number of bytes sent since the last time the statistics were
    * reset.
    * 
    * @descriptor metrics.value=_default
     */
    public long getTotalBytesSent()
        {
        return 0L;
        }
    
    // Accessor for the property "TotalMessagesReceived"
    /**
     * Getter for property TotalMessagesReceived.<p>
    * The total number of messages received since the last time the statistics
    * were reset.
    * 
    * @descriptor metrics.value=_default
     */
    public long getTotalMessagesReceived()
        {
        return 0L;
        }
    
    // Accessor for the property "TotalMessagesSent"
    /**
     * Getter for property TotalMessagesSent.<p>
    * The total number of messages sent since the last time the statistics were
    * reset.
    * 
    * @descriptor metrics.value=_default
     */
    public long getTotalMessagesSent()
        {
        return 0L;
        }
    
    // Accessor for the property "UUID"
    /**
     * Getter for property UUID.<p>
    * The unique identifier for this connection.
     */
    public String getUUID()
        {
        return null;
        }
    
    // Accessor for the property "MessagingDebug"
    /**
     * Getter for property MessagingDebug.<p>
    * Debug flag.  When true and the node's logging level is 6 or higher, sent
    * and received messages will be logged for this connection.   If this
    * attribute in the ConnectionManager MBean is true and the node's logging
    * level is 6 or higher, sent and received messages will also be logged.
     */
    public boolean isMessagingDebug()
        {
        return __m_MessagingDebug;
        }
    
    /**
     * Reset statistics.
     */
    public void resetStatistics()
        {
        }
    
    // Accessor for the property "MessagingDebug"
    /**
     * Setter for property MessagingDebug.<p>
    * Debug flag.  When true and the node's logging level is 6 or higher, sent
    * and received messages will be logged for this connection.   If this
    * attribute in the ConnectionManager MBean is true and the node's logging
    * level is 6 or higher, sent and received messages will also be logged.
     */
    public void setMessagingDebug(boolean fDebug)
        {
        __m_MessagingDebug = fDebug;
        }
    }
