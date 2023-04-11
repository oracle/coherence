
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.manageable.modelAdapter.ConnectionManagerMBean

package com.tangosol.coherence.component.manageable.modelAdapter;

/**
 * ConnectionManagerMBean contains statistics for throughput and connection
 * information of Proxy hosts.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class ConnectionManagerMBean
        extends    com.tangosol.coherence.component.manageable.ModelAdapter
    {
    // ---- Fields declarations ----
    
    /**
     * Property MessagingDebug
     *
     * Debug flag.  When true and the node's logging level is 6 or higher, sent
     * and received messages will be logged for all the connections under this
     * service.
     */
    private transient boolean __m_MessagingDebug;
    
    // Default constructor
    public ConnectionManagerMBean()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public ConnectionManagerMBean(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
        return new com.tangosol.coherence.component.manageable.modelAdapter.ConnectionManagerMBean();
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
            clz = Class.forName("com.tangosol.coherence/component/manageable/modelAdapter/ConnectionManagerMBean".replace('/', '.'));
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
            "ConnectionManagerMBean contains statistics for throughput and connection information of Proxy hosts.",
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
        
        // property AverageRequestTime
            {
            mapInfo.put("AverageRequestTime", new Object[]
                {
                "The average processing time in milliseconds for HTTP requests.",
                "getAverageRequestTime",
                null,
                "F",
                "metrics.value=_default",
                });
            }
        
        // property ConnectionCount
            {
            mapInfo.put("ConnectionCount", new Object[]
                {
                "The number of client connections.",
                "getConnectionCount",
                null,
                "I",
                "metrics.value=_default",
                });
            }
        
        // property HostIP
            {
            mapInfo.put("HostIP", new Object[]
                {
                "The IP address and port of the Proxy host.",
                "getHostIP",
                null,
                "Ljava/lang/String;",
                "metrics.tag=host",
                });
            }
        
        // property HttpServerType
            {
            mapInfo.put("HttpServerType", new Object[]
                {
                "The type of HTTP server or n/a if not using HTTP protocol.",
                "getHttpServerType",
                null,
                "Ljava/lang/String;",
                null,
                });
            }
        
        // property IncomingBufferPoolCapacity
            {
            mapInfo.put("IncomingBufferPoolCapacity", new Object[]
                {
                "The pool capacity (in bytes) of the incoming buffer.",
                "getIncomingBufferPoolCapacity",
                null,
                "J",
                null,
                });
            }
        
        // property IncomingBufferPoolSize
            {
            mapInfo.put("IncomingBufferPoolSize", new Object[]
                {
                "The number of buffers in the incoming pool.",
                "getIncomingBufferPoolSize",
                null,
                "I",
                null,
                });
            }
        
        // property MessagingDebug
            {
            mapInfo.put("MessagingDebug", new Object[]
                {
                "Debug flag.  When true and the node's logging level is 6 or higher, sent and received messages will be logged for all the connections under this service.",
                "isMessagingDebug",
                "setMessagingDebug",
                "Z",
                null,
                });
            }
        
        // property OutgoingBufferPoolCapacity
            {
            mapInfo.put("OutgoingBufferPoolCapacity", new Object[]
                {
                "The pool capacity (in bytes) of the outgoing buffer.",
                "getOutgoingBufferPoolCapacity",
                null,
                "J",
                null,
                });
            }
        
        // property OutgoingBufferPoolSize
            {
            mapInfo.put("OutgoingBufferPoolSize", new Object[]
                {
                "The number of buffers in the outgoing pool.",
                "getOutgoingBufferPoolSize",
                null,
                "I",
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
                "J",
                "metrics.value=_default",
                });
            }
        
        // property Protocol
            {
            mapInfo.put("Protocol", new Object[]
                {
                "Protocol associated with this ConnectionManagerMBean. Valid values are tcp or http.",
                "getProtocol",
                null,
                "Ljava/lang/String;",
                "metrics.tag=protocol",
                });
            }
        
        // property RequestsPerSecond
            {
            mapInfo.put("RequestsPerSecond", new Object[]
                {
                "The number of HTTP requests per second since the statistics were reset.",
                "getRequestsPerSecond",
                null,
                "F",
                "metrics.value=_default",
                });
            }
        
        // property ResponseCount1xx
            {
            mapInfo.put("ResponseCount1xx", new Object[]
                {
                "The number of HTTP responses in the 100-199 range.",
                "getResponseCount1xx",
                null,
                "J",
                "metrics.value=response_1xx_count",
                });
            }
        
        // property ResponseCount2xx
            {
            mapInfo.put("ResponseCount2xx", new Object[]
                {
                "The number of HTTP responses in the 200-299 range.",
                "getResponseCount2xx",
                null,
                "J",
                "metrics.value=response_2xx_count",
                });
            }
        
        // property ResponseCount3xx
            {
            mapInfo.put("ResponseCount3xx", new Object[]
                {
                "The number of HTTP responses in the 300-399 range.",
                "getResponseCount3xx",
                null,
                "J",
                "metrics.value=response_3xx_count",
                });
            }
        
        // property ResponseCount4xx
            {
            mapInfo.put("ResponseCount4xx", new Object[]
                {
                "The number of HTTP responses in the 400-499 range.",
                "getResponseCount4xx",
                null,
                "J",
                "metrics.value=response_4xx_count",
                });
            }
        
        // property ResponseCount5xx
            {
            mapInfo.put("ResponseCount5xx", new Object[]
                {
                "The number of HTTP responses in the 500-599 range.",
                "getResponseCount5xx",
                null,
                "J",
                "metrics.value=response_5xx_count",
                });
            }
        
        // property TotalBytesReceived
            {
            mapInfo.put("TotalBytesReceived", new Object[]
                {
                "The total number of bytes received by the Proxy host since the statistics were last reset.",
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
                "The total number of bytes sent by the Proxy host since the statistics were last reset.",
                "getTotalBytesSent",
                null,
                "J",
                "metrics.value=_default",
                });
            }
        
        // property TotalErrorCount
            {
            mapInfo.put("TotalErrorCount", new Object[]
                {
                "The number of HTTP requests that caused errors.",
                "getTotalErrorCount",
                null,
                "J",
                "metrics.value=_default",
                });
            }
        
        // property TotalMessagesReceived
            {
            mapInfo.put("TotalMessagesReceived", new Object[]
                {
                "The total number of messgaes received by the Proxy host since the statistics were last reset.",
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
                "The total number of messgaes sent by the Proxy host since the statistics were last reset.",
                "getTotalMessagesSent",
                null,
                "J",
                "metrics.value=_default",
                });
            }
        
        // property TotalRequestCount
            {
            mapInfo.put("TotalRequestCount", new Object[]
                {
                "The number of requests serviced since the HTTP server was started or the statistics were reset.",
                "getTotalRequestCount",
                null,
                "J",
                "metrics.value=_default",
                });
            }
        
        // property UnauthorizedConnectionAttempts
            {
            mapInfo.put("UnauthorizedConnectionAttempts", new Object[]
                {
                "The number of connection attempts from unauthorized hosts.",
                "getUnauthorizedConnectionAttempts",
                null,
                "J",
                "metrics.value=_default",
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
        
        // behavior resetStatistics()
            {
            mapInfo.put("resetStatistics()", new Object[]
                {
                "",
                "resetStatistics",
                "V",
                new String[] {},
                new String[] {},
                null,
                });
            }
        
        return mapInfo;
        }
    
    // Accessor for the property "AverageRequestTime"
    /**
     * Getter for property AverageRequestTime.<p>
    * The average processing time in milliseconds for HTTP requests.
    * 
    * @descriptor metrics.value=_default
     */
    public float getAverageRequestTime()
        {
        return 0.0F;
        }
    
    // Accessor for the property "ConnectionCount"
    /**
     * Getter for property ConnectionCount.<p>
    * The number of client connections.
    * 
    * @descriptor metrics.value=_default
     */
    public int getConnectionCount()
        {
        return 0;
        }
    
    // Accessor for the property "HostIP"
    /**
     * Getter for property HostIP.<p>
    * The IP address and port of the Proxy host.
    * 
    * @descriptor metrics.tag=host
     */
    public String getHostIP()
        {
        return null;
        }
    
    // Accessor for the property "HttpServerType"
    /**
     * Getter for property HttpServerType.<p>
    * The type of HTTP server or n/a if not using HTTP protocol.
     */
    public String getHttpServerType()
        {
        return null;
        }
    
    // Accessor for the property "IncomingBufferPoolCapacity"
    /**
     * Getter for property IncomingBufferPoolCapacity.<p>
    * The pool capacity (in bytes) of the incoming buffer.
     */
    public long getIncomingBufferPoolCapacity()
        {
        return 0L;
        }
    
    // Accessor for the property "IncomingBufferPoolSize"
    /**
     * Getter for property IncomingBufferPoolSize.<p>
    * The number of buffers in the incoming pool.
     */
    public int getIncomingBufferPoolSize()
        {
        return 0;
        }
    
    // Accessor for the property "OutgoingBufferPoolCapacity"
    /**
     * Getter for property OutgoingBufferPoolCapacity.<p>
    * The pool capacity (in bytes) of the outgoing buffer.
     */
    public long getOutgoingBufferPoolCapacity()
        {
        return 0L;
        }
    
    // Accessor for the property "OutgoingBufferPoolSize"
    /**
     * Getter for property OutgoingBufferPoolSize.<p>
    * The number of buffers in the outgoing pool.
     */
    public int getOutgoingBufferPoolSize()
        {
        return 0;
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
    public long getOutgoingMessageBacklog()
        {
        return 0L;
        }
    
    // Accessor for the property "Protocol"
    /**
     * Getter for property Protocol.<p>
    * Protocol associated with this ConnectionManagerMBean. Valid values are
    * tcp or http.
    * 
    * @descriptor metrics.tag=protocol
     */
    public String getProtocol()
        {
        return null;
        }
    
    // Accessor for the property "RequestsPerSecond"
    /**
     * Getter for property RequestsPerSecond.<p>
    * The number of HTTP requests per second since the statistics were reset.
    * 
    * @descriptor metrics.value=_default
     */
    public float getRequestsPerSecond()
        {
        return 0.0F;
        }
    
    // Accessor for the property "ResponseCount1xx"
    /**
     * Getter for property ResponseCount1xx.<p>
    * The number of HTTP responses in the 100-199 range.
    * 
    * @descriptor metrics.value=response_1xx_count
     */
    public long getResponseCount1xx()
        {
        return 0L;
        }
    
    // Accessor for the property "ResponseCount2xx"
    /**
     * Getter for property ResponseCount2xx.<p>
    * The number of HTTP responses in the 200-299 range.
    * 
    * @descriptor metrics.value=response_2xx_count
     */
    public long getResponseCount2xx()
        {
        return 0L;
        }
    
    // Accessor for the property "ResponseCount3xx"
    /**
     * Getter for property ResponseCount3xx.<p>
    * The number of HTTP responses in the 300-399 range.
    * 
    * @descriptor metrics.value=response_3xx_count
     */
    public long getResponseCount3xx()
        {
        return 0L;
        }
    
    // Accessor for the property "ResponseCount4xx"
    /**
     * Getter for property ResponseCount4xx.<p>
    * The number of HTTP responses in the 400-499 range.
    * 
    * @descriptor metrics.value=response_4xx_count
     */
    public long getResponseCount4xx()
        {
        return 0L;
        }
    
    // Accessor for the property "ResponseCount5xx"
    /**
     * Getter for property ResponseCount5xx.<p>
    * The number of HTTP responses in the 500-599 range.
    * 
    * @descriptor metrics.value=response_5xx_count
     */
    public long getResponseCount5xx()
        {
        return 0L;
        }
    
    // Accessor for the property "TotalBytesReceived"
    /**
     * Getter for property TotalBytesReceived.<p>
    * The total number of bytes received by the Proxy host since the statistics
    * were last reset.
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
    * The total number of bytes sent by the Proxy host since the statistics
    * were last reset.
    * 
    * @descriptor metrics.value=_default
     */
    public long getTotalBytesSent()
        {
        return 0L;
        }
    
    // Accessor for the property "TotalErrorCount"
    /**
     * Getter for property TotalErrorCount.<p>
    * The number of HTTP requests that caused errors.
    * 
    * @descriptor metrics.value=_default
     */
    public long getTotalErrorCount()
        {
        return 0L;
        }
    
    // Accessor for the property "TotalMessagesReceived"
    /**
     * Getter for property TotalMessagesReceived.<p>
    * The total number of messgaes received by the Proxy host since the
    * statistics were last reset.
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
    * The total number of messgaes sent by the Proxy host since the statistics
    * were last reset.
    * 
    * @descriptor metrics.value=_default
     */
    public long getTotalMessagesSent()
        {
        return 0L;
        }
    
    // Accessor for the property "TotalRequestCount"
    /**
     * Getter for property TotalRequestCount.<p>
    * The number of requests serviced since the HTTP server was started or the
    * statistics were reset.
    * 
    * @descriptor metrics.value=_default
     */
    public long getTotalRequestCount()
        {
        return 0L;
        }
    
    // Accessor for the property "UnauthorizedConnectionAttempts"
    /**
     * Getter for property UnauthorizedConnectionAttempts.<p>
    * The number of connection attempts from unauthorized hosts.
    * 
    * @descriptor metrics.value=_default
     */
    public long getUnauthorizedConnectionAttempts()
        {
        return 0L;
        }
    
    // Accessor for the property "MessagingDebug"
    /**
     * Getter for property MessagingDebug.<p>
    * Debug flag.  When true and the node's logging level is 6 or higher, sent
    * and received messages will be logged for all the connections under this
    * service.
     */
    public boolean isMessagingDebug()
        {
        return __m_MessagingDebug;
        }
    
    public void resetStatistics()
        {
        }
    
    // Accessor for the property "MessagingDebug"
    /**
     * Setter for property MessagingDebug.<p>
    * Debug flag.  When true and the node's logging level is 6 or higher, sent
    * and received messages will be logged for all the connections under this
    * service.
     */
    public void setMessagingDebug(boolean fDebug)
        {
        __m_MessagingDebug = fDebug;
        }
    }
