
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.manageable.modelAdapter.ClusterNodeMBean

package com.tangosol.coherence.component.manageable.modelAdapter;

/**
 * ClusterNodeMBean represents a Cluster member.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class ClusterNodeMBean
        extends    com.tangosol.coherence.component.manageable.ModelAdapter
    {
    // ---- Fields declarations ----
    
    /**
     * Property BufferPublishSize
     *
     * The buffer size of the unicast datagram socket used by the Publisher,
     * measured in the number of packets. Changing this value at runtime is an
     * inherently unsafe operation that will pause all network communications
     * and may result in the termination of all cluster services.
     */
    private transient int __m_BufferPublishSize;
    
    /**
     * Property BufferReceiveSize
     *
     * The buffer size of the unicast datagram socket used by the Receiver,
     * measured in the number of packets. Changing this value at runtime is an
     * inherently unsafe operation that will pause all network communications
     * and may result in the termination of all cluster services.
     */
    private transient int __m_BufferReceiveSize;
    
    /**
     * Property LoggingFormat
     *
     * Specifies how messages will be formatted before being passed to the log
     * destination
     */
    private transient String __m_LoggingFormat;
    
    /**
     * Property LoggingLevel
     *
     * Specifies which logged messages will be output to the log destination.
     * Valid values are non-negative integers or -1 to disable all logger
     * output.
     */
    private transient int __m_LoggingLevel;
    
    /**
     * Property LoggingLimit
     *
     * The maximum number of characters that the logger daemon will process
     * from the message queue before discarding all remaining messages in the
     * queue. Valid values are integers in the range [0...]. Zero implies no
     * limit.
     */
    private transient int __m_LoggingLimit;
    
    /**
     * Property MulticastThreshold
     *
     * The percentage (0 to 100) of the servers in the cluster that a packet
     * will be sent to, above which the packet will be multicasted and below
     * which it will be unicasted.
     */
    private transient int __m_MulticastThreshold;
    
    /**
     * Property ResendDelay
     *
     * The minimum number of milliseconds that a packet will remain queued in
     * the Publisher`s re-send queue before it is resent to the recipient(s) if
     * the packet has not been acknowledged. Setting this value too low can
     * overflow the network with unnecessary repetitions. Setting the value too
     * high can increase the overall latency by delaying the re-sends of
     * dropped packets. Additionally, change of this value may need to be
     * accompanied by a change in SendAckDelay value.
     */
    private transient int __m_ResendDelay;
    
    /**
     * Property SendAckDelay
     *
     * The minimum number of milliseconds between the queueing of an Ack packet
     * and the sending of the same. This value should be not more then a half
     * of the ResendDelay value.
     */
    private transient int __m_SendAckDelay;
    
    /**
     * Property TracingSamplingRatio
     *
     * The ratio of spans to trace when tracing is enabled. 
     */
    private float __m_TracingSamplingRatio;
    
    /**
     * Property TrafficJamCount
     *
     * The maximum total number of packets in the send and resend queues that
     * forces the publisher to pause client threads. Zero means no limit.
     */
    private transient int __m_TrafficJamCount;
    
    /**
     * Property TrafficJamDelay
     *
     * The number of milliseconds to pause client threads when a traffic jam
     * condition has been reached. Anything less than one (e.g. zero) is
     * treated as one millisecond.
     */
    private transient int __m_TrafficJamDelay;
    
    /**
     * Property TransportBacklogDelay
     *
     * The total number of milliseconds that requests were delayed due to
     * draining the backlog since the node statistics were last reset.
     * 
     * @descriptor metrics.value=_default
     */
    private long __m_TransportBacklogDelay;
    
    /**
     * Property TransportRetainedBytes
     *
     * The number of bytes retained by the service-dedicated transport awaiting
     * delivery acknowledgment.  This memory is allocated outside of the Java
     * GC heap space.
     * 
     * @descriptor metrics.value=_default
     */
    private long __m_TransportRetainedBytes;
    
    // Default constructor
    public ClusterNodeMBean()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public ClusterNodeMBean(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
        return new com.tangosol.coherence.component.manageable.modelAdapter.ClusterNodeMBean();
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
            clz = Class.forName("com.tangosol.coherence/component/manageable/modelAdapter/ClusterNodeMBean".replace('/', '.'));
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
            "ClusterNodeMBean represents a Cluster member.",
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
        
        // property BufferPublishSize
            {
            mapInfo.put("BufferPublishSize", new Object[]
                {
                "The buffer size of the unicast datagram socket used by the Publisher, measured in the number of packets. Changing this value at runtime is an inherently unsafe operation that will pause all network communications and may result in the termination of all cluster services.",
                "getBufferPublishSize",
                "setBufferPublishSize",
                "I",
                null,
                });
            }
        
        // property BufferReceiveSize
            {
            mapInfo.put("BufferReceiveSize", new Object[]
                {
                "The buffer size of the unicast datagram socket used by the Receiver, measured in the number of packets. Changing this value at runtime is an inherently unsafe operation that will pause all network communications and may result in the termination of all cluster services.",
                "getBufferReceiveSize",
                "setBufferReceiveSize",
                "I",
                null,
                });
            }
        
        // property CpuCount
            {
            mapInfo.put("CpuCount", new Object[]
                {
                "Number of CPU cores for the machine this Member is running on.",
                "getCpuCount",
                null,
                "I",
                null,
                });
            }
        
        // property FlowControlEnabled
            {
            mapInfo.put("FlowControlEnabled", new Object[]
                {
                "Indicates whether or not FlowControl is enabled.",
                "isFlowControlEnabled",
                null,
                "Z",
                null,
                });
            }
        
        // property GuardRecoverCount
            {
            mapInfo.put("GuardRecoverCount", new Object[]
                {
                "The number of recovery attempts executed for all guardables on this node since the node statistics were last reset.",
                "getGuardRecoverCount",
                null,
                "I",
                "metrics.value=_default",
                });
            }
        
        // property GuardTerminateCount
            {
            mapInfo.put("GuardTerminateCount", new Object[]
                {
                "The number of termination attempts executed for all guardables on this node since the node statistics were last reset.",
                "getGuardTerminateCount",
                null,
                "I",
                "metrics.value=_default",
                });
            }
        
        // property Id
            {
            mapInfo.put("Id", new Object[]
                {
                "The short Member id that uniquely identifies the Member at this point in time and does not change for the life of this Member.",
                "getId",
                null,
                "I",
                null,
                });
            }
        
        // property LoggingDestination
            {
            mapInfo.put("LoggingDestination", new Object[]
                {
                "The output device used by the logging system. Valid values are stdout, stderr, jdk, log4j2, or a file name.",
                "getLoggingDestination",
                null,
                "Ljava/lang/String;",
                null,
                });
            }
        
        // property LoggingFormat
            {
            mapInfo.put("LoggingFormat", new Object[]
                {
                "Specifies how messages will be formatted before being passed to the log destination",
                "getLoggingFormat",
                "setLoggingFormat",
                "Ljava/lang/String;",
                null,
                });
            }
        
        // property LoggingLevel
            {
            mapInfo.put("LoggingLevel", new Object[]
                {
                "Specifies which logged messages will be output to the log destination. Valid values are non-negative integers or -1 to disable all logger output.",
                "getLoggingLevel",
                "setLoggingLevel",
                "I",
                null,
                });
            }
        
        // property LoggingLimit
            {
            mapInfo.put("LoggingLimit", new Object[]
                {
                "The maximum number of characters that the logger daemon will process from the message queue before discarding all remaining messages in the queue. Valid values are integers in the range [0...]. Zero implies no limit.",
                "getLoggingLimit",
                "setLoggingLimit",
                "I",
                null,
                });
            }
        
        // property MachineId
            {
            mapInfo.put("MachineId", new Object[]
                {
                "The Member`s machine Id.",
                "getMachineId",
                null,
                "I",
                null,
                });
            }
        
        // property MachineName
            {
            mapInfo.put("MachineName", new Object[]
                {
                "A configured name that should be the same for all Members that are on the same physical machine, and different for Members that are on different physical machines.",
                "getMachineName",
                null,
                "Ljava/lang/String;",
                null,
                });
            }
        
        // property MemberName
            {
            mapInfo.put("MemberName", new Object[]
                {
                "A configured name that must be unique for every Member.",
                "getMemberName",
                null,
                "Ljava/lang/String;",
                null,
                });
            }
        
        // property MemoryAvailableMB
            {
            mapInfo.put("MemoryAvailableMB", new Object[]
                {
                "The total amount of memory in the JVM available for new objects in MB.",
                "getMemoryAvailableMB",
                null,
                "I",
                null,
                });
            }
        
        // property MemoryMaxMB
            {
            mapInfo.put("MemoryMaxMB", new Object[]
                {
                "The maximum amount of memory that the JVM will attempt to use in MB.",
                "getMemoryMaxMB",
                null,
                "I",
                null,
                });
            }
        
        // property MulticastAddress
            {
            mapInfo.put("MulticastAddress", new Object[]
                {
                "The IP address of the Member`s MulticastSocket for group communication.",
                "getMulticastAddress",
                null,
                "Ljava/lang/String;",
                null,
                });
            }
        
        // property MulticastEnabled
            {
            mapInfo.put("MulticastEnabled", new Object[]
                {
                "Specifies whether or not this Member uses multicast for group communication. If false, this Member will use the WellKnownAddresses to join the cluster and point-to-point unicast to communicate with other Members of the cluster.",
                "isMulticastEnabled",
                null,
                "Z",
                null,
                });
            }
        
        // property MulticastPort
            {
            mapInfo.put("MulticastPort", new Object[]
                {
                "The port of the Member`s MulticastSocket for group communication.",
                "getMulticastPort",
                null,
                "I",
                null,
                });
            }
        
        // property MulticastThreshold
            {
            mapInfo.put("MulticastThreshold", new Object[]
                {
                "The percentage (0 to 100) of the servers in the cluster that a packet will be sent to, above which the packet will be multicasted and below which it will be unicasted.",
                "getMulticastThreshold",
                "setMulticastThreshold",
                "I",
                null,
                });
            }
        
        // property MulticastTTL
            {
            mapInfo.put("MulticastTTL", new Object[]
                {
                "The time-to-live for multicast packets sent out on this Member`s MulticastSocket.",
                "getMulticastTTL",
                null,
                "I",
                null,
                });
            }
        
        // property NackEnabled
            {
            mapInfo.put("NackEnabled", new Object[]
                {
                "Indicates whether or not the early packet loss detection protocol is enabled.",
                "isNackEnabled",
                null,
                "Z",
                null,
                });
            }
        
        // property NackSent
            {
            mapInfo.put("NackSent", new Object[]
                {
                "The total number of NACK packets sent since the node statistics were last reset.",
                "getNackSent",
                null,
                "J",
                null,
                });
            }
        
        // property PacketDeliveryEfficiency
            {
            mapInfo.put("PacketDeliveryEfficiency", new Object[]
                {
                "The efficiency of packet loss detection and retransmission.  A low efficiency is an indication that there is a high rate of unnecessary packet retransmissions.",
                "getPacketDeliveryEfficiency",
                null,
                "F",
                null,
                });
            }
        
        // property PacketsBundled
            {
            mapInfo.put("PacketsBundled", new Object[]
                {
                "The total number of packets which were bundled prior to transmission.  The total number of network transmissions is equal to (PacketsSent - PacketsBundled).",
                "getPacketsBundled",
                null,
                "J",
                null,
                });
            }
        
        // property PacketsReceived
            {
            mapInfo.put("PacketsReceived", new Object[]
                {
                "The number of packets received since the node statistics were last reset.",
                "getPacketsReceived",
                null,
                "J",
                "metrics.value=_default",
                });
            }
        
        // property PacketsRepeated
            {
            mapInfo.put("PacketsRepeated", new Object[]
                {
                "The number of duplicate packets received since the node statistics were last reset.",
                "getPacketsRepeated",
                null,
                "J",
                "metrics.value=_default",
                });
            }
        
        // property PacketsResent
            {
            mapInfo.put("PacketsResent", new Object[]
                {
                "The number of packets resent since the node statistics were last reset. A packet is resent when there is no ACK received within a timeout period.",
                "getPacketsResent",
                null,
                "J",
                "metrics.value=_default",
                });
            }
        
        // property PacketsResentEarly
            {
            mapInfo.put("PacketsResentEarly", new Object[]
                {
                "The total number of packets resent ahead of schedule. A packet is resent ahead of schedule when there is a NACK indicating that the packet has not been received.",
                "getPacketsResentEarly",
                null,
                "J",
                null,
                });
            }
        
        // property PacketsResentExcess
            {
            mapInfo.put("PacketsResentExcess", new Object[]
                {
                "The total number of packet retransmissions which were later proven unnecessary.",
                "getPacketsResentExcess",
                null,
                "J",
                null,
                });
            }
        
        // property PacketsSent
            {
            mapInfo.put("PacketsSent", new Object[]
                {
                "The number of packets sent since the node statistics were last reset.",
                "getPacketsSent",
                null,
                "J",
                "metrics.value=_default",
                });
            }
        
        // property Priority
            {
            mapInfo.put("Priority", new Object[]
                {
                "The priority or \"weight\" of the Member; used to determine tie-breakers.",
                "getPriority",
                null,
                "I",
                null,
                });
            }
        
        // property ProcessName
            {
            mapInfo.put("ProcessName", new Object[]
                {
                "A configured name that should be the same for Members that are in the same process (JVM), and different for Members that are in different processes. If not explicitly provided, for processes running with JRE 1.5 or higher the name will be calculated internally as the Name attribute of the system RuntimeMXBean, which normally represents the process identifier (PID).",
                "getProcessName",
                null,
                "Ljava/lang/String;",
                null,
                });
            }
        
        // property ProductEdition
            {
            mapInfo.put("ProductEdition", new Object[]
                {
                "The product edition this Member is running. Possible values  are: Standard Edition (SE), Enterprise Edition (EE), Grid Edition (GE).",
                "getProductEdition",
                null,
                "Ljava/lang/String;",
                null,
                });
            }
        
        // property PublisherPacketUtilization
            {
            mapInfo.put("PublisherPacketUtilization", new Object[]
                {
                "The publisher packet utilization for this cluster node since the node socket was last reopened. This value is a ratio of the number of bytes sent to the number that would have been sent had all packets been full. A low utilization indicates that data is not being sent in large enough chunks to make efficient use of the network.",
                "getPublisherPacketUtilization",
                null,
                "F",
                null,
                });
            }
        
        // property PublisherSuccessRate
            {
            mapInfo.put("PublisherSuccessRate", new Object[]
                {
                "The publisher success rate for this cluster node since the node statistics were last reset. Publisher success rate is a ratio of the number of packets successfully delivered in a first attempt to the total number of sent packets. A failure count is incremented when there is no ACK received within a timeout period. It could be caused by either very high network latency or a high packet drop rate.",
                "getPublisherSuccessRate",
                null,
                "F",
                "metrics.value=_default",
                });
            }
        
        // property QuorumStatus
            {
            mapInfo.put("QuorumStatus", new Object[]
                {
                "The current state of the cluster quorum.",
                "getQuorumStatus",
                null,
                "Ljava/lang/String;",
                null,
                });
            }
        
        // property RackName
            {
            mapInfo.put("RackName", new Object[]
                {
                "A configured name that should be the same for Members that are on the same physical \"rack\" (or frame or cage), and different for Members that are on different physical \"racks\".",
                "getRackName",
                null,
                "Ljava/lang/String;",
                null,
                });
            }
        
        // property ReceiverPacketUtilization
            {
            mapInfo.put("ReceiverPacketUtilization", new Object[]
                {
                "The receiver packet utilization for this cluster node since the socket was last reopened. This value is a ratio of the number of bytes received to the number that would have been received had all packets been full. A low utilization indicates that data is not being sent in large enough chunks to make efficient use of the network.",
                "getReceiverPacketUtilization",
                null,
                "F",
                null,
                });
            }
        
        // property ReceiverSuccessRate
            {
            mapInfo.put("ReceiverSuccessRate", new Object[]
                {
                "The receiver success rate for this cluster node since the node statistics were last reset. Receiver success rate is a ratio of the number of packets successfully acknowledged in a first attempt to the total number of received packets. A failure count is incremented when a re-delivery of previously received packet is detected. It could be caused by either very high inbound network latency or lost ACK packets.",
                "getReceiverSuccessRate",
                null,
                "F",
                "metrics.value=_default",
                });
            }
        
        // property ResendDelay
            {
            mapInfo.put("ResendDelay", new Object[]
                {
                "The minimum number of milliseconds that a packet will remain queued in the Publisher`s re-send queue before it is resent to the recipient(s) if the packet has not been acknowledged. Setting this value too low can overflow the network with unnecessary repetitions. Setting the value too high can increase the overall latency by delaying the re-sends of dropped packets. Additionally, change of this value may need to be accompanied by a change in SendAckDelay value.",
                "getResendDelay",
                "setResendDelay",
                "I",
                null,
                });
            }
        
        // property RoleName
            {
            mapInfo.put("RoleName", new Object[]
                {
                "A configured name that can be used to indicate the role of a Member to the application. While managed by Coherence, this property is used only by the application.",
                "getRoleName",
                null,
                "Ljava/lang/String;",
                null,
                });
            }
        
        // property SendAckDelay
            {
            mapInfo.put("SendAckDelay", new Object[]
                {
                "The minimum number of milliseconds between the queueing of an Ack packet and the sending of the same. This value should be not more then a half of the ResendDelay value.",
                "getSendAckDelay",
                "setSendAckDelay",
                "I",
                null,
                });
            }
        
        // property SendQueueSize
            {
            mapInfo.put("SendQueueSize", new Object[]
                {
                "The number of packets currently scheduled for delivery. This number includes both packets that are to be sent immediately and packets that have already been sent and awaiting for acknowledgment. Packets that do not receive an acknowledgment within ResendDelay interval will be automatically resent.",
                "getSendQueueSize",
                null,
                "I",
                "metrics.value=_default",
                });
            }
        
        // property SiteName
            {
            mapInfo.put("SiteName", new Object[]
                {
                "A configured name that should be the same for Members that are on the same physical site (e.g. data center), and different for Members that are on different physical sites.",
                "getSiteName",
                null,
                "Ljava/lang/String;",
                null,
                });
            }
        
        // property SocketCount
            {
            mapInfo.put("SocketCount", new Object[]
                {
                "Number of CPU sockets for the machine this Member is running on.",
                "getSocketCount",
                null,
                "I",
                null,
                });
            }
        
        // property Statistics
            {
            mapInfo.put("Statistics", new Object[]
                {
                "Statistics for this cluster node in a human readable format.",
                "getStatistics",
                null,
                "Ljava/lang/String;",
                null,
                });
            }
        
        // property TcpRingFailures
            {
            mapInfo.put("TcpRingFailures", new Object[]
                {
                "The number of recovered TcpRing disconnects since the node statistics were last reset. A recoverable disconnect is an abnormal event that is registered when the TcpRing peer drops the TCP connection, but recovers after no more then maximum configured number of attempts.This value will be -1 if the TcpRing is disabled.",
                "getTcpRingFailures",
                null,
                "J",
                null,
                });
            }
        
        // property Timestamp
            {
            mapInfo.put("Timestamp", new Object[]
                {
                "The date/time value (in cluster time) that this Member joined the cluster.",
                "getTimestamp",
                null,
                "Ljava/util/Date;",
                null,
                });
            }
        
        // property TracingSamplingRatio
            {
            mapInfo.put("TracingSamplingRatio", new Object[]
                {
                "The ratio of spans to trace when tracing is enabled.",
                "getTracingSamplingRatio",
                "setTracingSamplingRatio",
                "F",
                null,
                });
            }
        
        // property TrafficJamCount
            {
            mapInfo.put("TrafficJamCount", new Object[]
                {
                "The maximum total number of packets in the send and resend queues that forces the publisher to pause client threads. Zero means no limit.",
                "getTrafficJamCount",
                "setTrafficJamCount",
                "I",
                null,
                });
            }
        
        // property TrafficJamDelay
            {
            mapInfo.put("TrafficJamDelay", new Object[]
                {
                "The number of milliseconds to pause client threads when a traffic jam condition has been reached. Anything less than one (e.g. zero) is treated as one millisecond.",
                "getTrafficJamDelay",
                "setTrafficJamDelay",
                "I",
                null,
                });
            }
        
        // property TransportBacklogDelay
            {
            mapInfo.put("TransportBacklogDelay", new Object[]
                {
                "The total number of milliseconds that requests were delayed due to draining the backlog since the node statistics were last reset.",
                "getTransportBacklogDelay",
                "setTransportBacklogDelay",
                "J",
                "metrics.value=_default",
                });
            }
        
        // property TransportReceivedBytes
            {
            mapInfo.put("TransportReceivedBytes", new Object[]
                {
                "The number of bytes received by the service-dedicated transport since the last time the statistics were reset.",
                "getTransportReceivedBytes",
                null,
                "J",
                "metrics.value=_default",
                });
            }
        
        // property TransportReceivedMessages
            {
            mapInfo.put("TransportReceivedMessages", new Object[]
                {
                "The number of messages received by the service-dedicated transport since the last time the statistics were reset.",
                "getTransportReceivedMessages",
                null,
                "J",
                "metrics.value=_default",
                });
            }
        
        // property TransportRetainedBytes
            {
            mapInfo.put("TransportRetainedBytes", new Object[]
                {
                "The number of bytes retained by the service-dedicated transport awaiting delivery acknowledgment.  This memory is allocated outside of the Java GC heap space.",
                "getTransportRetainedBytes",
                "setTransportRetainedBytes",
                "J",
                "metrics.value=_default",
                });
            }
        
        // property TransportSentBytes
            {
            mapInfo.put("TransportSentBytes", new Object[]
                {
                "The number of bytes sent by the service-dedicated transport since the last time the statistics were reset.",
                "getTransportSentBytes",
                null,
                "J",
                "metrics.value=_default",
                });
            }
        
        // property TransportSentMessages
            {
            mapInfo.put("TransportSentMessages", new Object[]
                {
                "The number of messages sent by the service-dedicated transport since the last time the statistics were reset.",
                "getTransportSentMessages",
                null,
                "J",
                "metrics.value=_default",
                });
            }
        
        // property TransportStatus
            {
            mapInfo.put("TransportStatus", new Object[]
                {
                "The service-dedicated transport status information.",
                "getTransportStatus",
                null,
                "Ljava/lang/String;",
                null,
                });
            }
        
        // property UID
            {
            mapInfo.put("UID", new Object[]
                {
                "The unique identifier of the Member which is calculated based on its Timestamp, Address, Port and MachineId.  This identifier is unique throughout the life of the cluster.",
                "getUID",
                null,
                "Ljava/lang/String;",
                null,
                });
            }
        
        // property UnicastAddress
            {
            mapInfo.put("UnicastAddress", new Object[]
                {
                "The IP address of the Member`s DatagramSocket for point-to-point communication.",
                "getUnicastAddress",
                null,
                "Ljava/lang/String;",
                null,
                });
            }
        
        // property UnicastPort
            {
            mapInfo.put("UnicastPort", new Object[]
                {
                "The port of the Member`s DatagramSocket for point-to-point communication.",
                "getUnicastPort",
                null,
                "I",
                null,
                });
            }
        
        // property WeakestChannel
            {
            mapInfo.put("WeakestChannel", new Object[]
                {
                "The id of the cluster node to which this node is having the most difficulty communicating, or -1 if none is found.  A channel is considered to be weak if either the point-to-point publisher or receiver success rates are below 1.0.",
                "getWeakestChannel",
                null,
                "I",
                null,
                });
            }
        
        // property WellKnownAddresses
            {
            mapInfo.put("WellKnownAddresses", new Object[]
                {
                "An array of well-known socket addresses that this Member uses to join the cluster.",
                "getWellKnownAddresses",
                null,
                "[Ljava/lang/String;",
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
        
        // behavior dumpHeap(String sFileName)
            {
            mapInfo.put("dumpHeap(Ljava.lang.String;)", new Object[]
                {
                "Dump heap on the node.  The parameter \"sFileName\" specify the file in which to store the heap dump, or directory in which a dynamically named hprof file will be saved, or null for a dynamic file in the system temp directory.  The method return the name of the stored file.",
                "dumpHeap",
                "Ljava/lang/String;",
                new String[] {"sFileName", },
                new String[] {"Ljava.lang.String;", },
                null,
                });
            }
        
        // behavior ensureService(String sServiceName)
            {
            mapInfo.put("ensureService(Ljava.lang.String;)", new Object[]
                {
                "Ensure that a specified Service runs at the cluster node represented by this MBean. This method will use the configurable cache factory to find out which service to start if necessary. Return value indicates the service type; null if a match could not be found.",
                "ensureService",
                "Ljava/lang/String;",
                new String[] {"sServiceName", },
                new String[] {"Ljava.lang.String;", },
                null,
                });
            }
        
        // behavior getNodeDescription()
            {
            mapInfo.put("getNodeDescription()", new Object[]
                {
                "Get member description.",
                "getNodeDescription",
                "Ljava/lang/String;",
                new String[] {},
                new String[] {},
                null,
                });
            }

        // behavior logNodeState()
            {
            mapInfo.put("logNodeState()", new Object[]
                {
                "Log a full thread dump and outstanding polls for the services running on the node.",
                "logNodeState",
                "V",
                new String[] {},
                new String[] {},
                null,
                });
            }
        
        // behavior reportEnvironment()
            {
            mapInfo.put("reportEnvironment()", new Object[]
                {
                "Return the environment information for this node. This includes details of the JVM as well as system properties.",
                "reportEnvironment",
                "Ljava/lang/String;",
                new String[] {},
                new String[] {},
                null,
                });
            }
        
        // behavior reportNodeState()
            {
            mapInfo.put("reportNodeState()", new Object[]
                {
                "Return a full thread dump and outstanding polls for the services running on the node.",
                "reportNodeState",
                "Ljava/lang/String;",
                new String[] {},
                new String[] {},
                null,
                });
            }
        
        // behavior resetStatistics()
            {
            mapInfo.put("resetStatistics()", new Object[]
                {
                "Reset the cluster node statistics.",
                "resetStatistics",
                "V",
                new String[] {},
                new String[] {},
                null,
                });
            }
        
        // behavior showTransportStatus()
            {
            mapInfo.put("showTransportStatus()", new Object[]
                {
                "Show detailed status information on the reliable transport.",
                "showTransportStatus",
                "Ljava/lang/String;",
                new String[] {},
                new String[] {},
                null,
                });
            }
        
        // behavior shutdown()
            {
            mapInfo.put("shutdown()", new Object[]
                {
                "Stop all the clustered services running at this node (controlled shutdown). The management of this node will not be available until the node is restarted (manually or programmatically).",
                "shutdown",
                "V",
                new String[] {},
                new String[] {},
                null,
                });
            }
        
        return mapInfo;
        }
    
    /**
     * Dump heap on the node.  The parameter "sFileName" specify the file in
    * which to store the heap dump, or directory in which a dynamically named
    * hprof file will be saved, or null for a dynamic file in the system temp
    * directory.  The method return the name of the stored file.
     */
    public String dumpHeap(String sFileName)
        {
        return null;
        }
    
    /**
     * Ensure that a specified Service runs at the cluster node represented by
    * this MBean. This method will use the configurable cache factory to find
    * out which service to start if necessary. Return value indicates the
    * service type; null if a match could not be found.
     */
    public String ensureService(String sServiceName)
        {
        return null;
        }
    
    // Accessor for the property "BufferPublishSize"
    /**
     * Getter for property BufferPublishSize.<p>
    * The buffer size of the unicast datagram socket used by the Publisher,
    * measured in the number of packets. Changing this value at runtime is an
    * inherently unsafe operation that will pause all network communications
    * and may result in the termination of all cluster services.
     */
    public int getBufferPublishSize()
        {
        return __m_BufferPublishSize;
        }
    
    // Accessor for the property "BufferReceiveSize"
    /**
     * Getter for property BufferReceiveSize.<p>
    * The buffer size of the unicast datagram socket used by the Receiver,
    * measured in the number of packets. Changing this value at runtime is an
    * inherently unsafe operation that will pause all network communications
    * and may result in the termination of all cluster services.
     */
    public int getBufferReceiveSize()
        {
        return __m_BufferReceiveSize;
        }
    
    // Accessor for the property "CpuCount"
    /**
     * Getter for property CpuCount.<p>
    * Number of CPU cores for the machine this Member is running on.
     */
    public int getCpuCount()
        {
        return 0;
        }
    
    // Accessor for the property "GuardRecoverCount"
    /**
     * Getter for property GuardRecoverCount.<p>
    * The number of recovery attempts executed for all guardables on this node
    * since the node statistics were last reset.
    * 
    * @descriptor metrics.value=_default
     */
    public int getGuardRecoverCount()
        {
        return 0;
        }
    
    // Accessor for the property "GuardTerminateCount"
    /**
     * Getter for property GuardTerminateCount.<p>
    * The number of termination attempts executed for all guardables on this
    * node since the node statistics were last reset.
    * 
    * @descriptor metrics.value=_default
     */
    public int getGuardTerminateCount()
        {
        return 0;
        }
    
    // Accessor for the property "Id"
    /**
     * Getter for property Id.<p>
    * The short Member id that uniquely identifies the Member at this point in
    * time and does not change for the life of this Member.
     */
    public int getId()
        {
        return 0;
        }
    
    // Accessor for the property "LoggingDestination"
    /**
     * Getter for property LoggingDestination.<p>
    * The output device used by the logging system. Valid values are stdout,
    * stderr, jdk, log4j2, or a file name.
     */
    public String getLoggingDestination()
        {
        return null;
        }
    
    // Accessor for the property "LoggingFormat"
    /**
     * Getter for property LoggingFormat.<p>
    * Specifies how messages will be formatted before being passed to the log
    * destination
     */
    public String getLoggingFormat()
        {
        return __m_LoggingFormat;
        }
    
    // Accessor for the property "LoggingLevel"
    /**
     * Getter for property LoggingLevel.<p>
    * Specifies which logged messages will be output to the log destination.
    * Valid values are non-negative integers or -1 to disable all logger output.
     */
    public int getLoggingLevel()
        {
        return __m_LoggingLevel;
        }
    
    // Accessor for the property "LoggingLimit"
    /**
     * Getter for property LoggingLimit.<p>
    * The maximum number of characters that the logger daemon will process from
    * the message queue before discarding all remaining messages in the queue.
    * Valid values are integers in the range [0...]. Zero implies no limit.
     */
    public int getLoggingLimit()
        {
        return __m_LoggingLimit;
        }
    
    // Accessor for the property "MachineId"
    /**
     * Getter for property MachineId.<p>
    * The Member`s machine Id. 
     */
    public int getMachineId()
        {
        return 0;
        }
    
    // Accessor for the property "MachineName"
    /**
     * Getter for property MachineName.<p>
    * A configured name that should be the same for all Members that are on the
    * same physical machine, and different for Members that are on different
    * physical machines.
     */
    public String getMachineName()
        {
        return null;
        }
    
    // Accessor for the property "MemberName"
    /**
     * Getter for property MemberName.<p>
    * A configured name that must be unique for every Member.
     */
    public String getMemberName()
        {
        return null;
        }
    
    // Accessor for the property "MemoryAvailableMB"
    /**
     * Getter for property MemoryAvailableMB.<p>
    * The total amount of memory in the JVM available for new objects in MB.
     */
    public int getMemoryAvailableMB()
        {
        return 0;
        }
    
    // Accessor for the property "MemoryMaxMB"
    /**
     * Getter for property MemoryMaxMB.<p>
    * The maximum amount of memory that the JVM will attempt to use in MB.
     */
    public int getMemoryMaxMB()
        {
        return 0;
        }
    
    // Accessor for the property "MulticastAddress"
    /**
     * Getter for property MulticastAddress.<p>
    * The IP address of the Member`s MulticastSocket for group communication.
     */
    public String getMulticastAddress()
        {
        return null;
        }
    
    // Accessor for the property "MulticastPort"
    /**
     * Getter for property MulticastPort.<p>
    * The port of the Member`s MulticastSocket for group communication.
     */
    public int getMulticastPort()
        {
        return 0;
        }
    
    // Accessor for the property "MulticastThreshold"
    /**
     * Getter for property MulticastThreshold.<p>
    * The percentage (0 to 100) of the servers in the cluster that a packet
    * will be sent to, above which the packet will be multicasted and below
    * which it will be unicasted.
     */
    public int getMulticastThreshold()
        {
        return __m_MulticastThreshold;
        }
    
    // Accessor for the property "MulticastTTL"
    /**
     * Getter for property MulticastTTL.<p>
    * The time-to-live for multicast packets sent out on this Member`s
    * MulticastSocket.
     */
    public int getMulticastTTL()
        {
        return 0;
        }
    
    // Accessor for the property "NackSent"
    /**
     * Getter for property NackSent.<p>
    * The total number of NACK packets sent since the node statistics were last
    * reset.
     */
    public long getNackSent()
        {
        return 0L;
        }
    
    // Accessor for the property "PacketDeliveryEfficiency"
    /**
     * Getter for property PacketDeliveryEfficiency.<p>
    * The efficiency of packet loss detection and retransmission.  A low
    * efficiency is an indication that there is a high rate of unnecessary
    * packet retransmissions.
     */
    public float getPacketDeliveryEfficiency()
        {
        return 0.0F;
        }
    
    // Accessor for the property "PacketsBundled"
    /**
     * Getter for property PacketsBundled.<p>
    * The total number of packets which were bundled prior to transmission. 
    * The total number of network transmissions is equal to (PacketsSent -
    * PacketsBundled).
     */
    public long getPacketsBundled()
        {
        return 0L;
        }
    
    // Accessor for the property "PacketsReceived"
    /**
     * Getter for property PacketsReceived.<p>
    * The number of packets received since the node statistics were last reset.
    * 
    * @descriptor metrics.value=_default
     */
    public long getPacketsReceived()
        {
        return 0L;
        }
    
    // Accessor for the property "PacketsRepeated"
    /**
     * Getter for property PacketsRepeated.<p>
    * The number of duplicate packets received since the node statistics were
    * last reset.
    * 
    * @descriptor metrics.value=_default
     */
    public long getPacketsRepeated()
        {
        return 0L;
        }
    
    // Accessor for the property "PacketsResent"
    /**
     * Getter for property PacketsResent.<p>
    * The number of packets resent since the node statistics were last reset. A
    * packet is resent when there is no ACK received within a timeout period.
    * 
    * @descriptor metrics.value=_default
     */
    public long getPacketsResent()
        {
        return 0L;
        }
    
    // Accessor for the property "PacketsResentEarly"
    /**
     * Getter for property PacketsResentEarly.<p>
    * The total number of packets resent ahead of schedule. A packet is resent
    * ahead of schedule when there is a NACK indicating that the packet has not
    * been received.
     */
    public long getPacketsResentEarly()
        {
        return 0L;
        }
    
    // Accessor for the property "PacketsResentExcess"
    /**
     * Getter for property PacketsResentExcess.<p>
    * The total number of packet retransmissions which were later proven
    * unnecessary.
     */
    public long getPacketsResentExcess()
        {
        return 0L;
        }
    
    // Accessor for the property "PacketsSent"
    /**
     * Getter for property PacketsSent.<p>
    * The number of packets sent since the node statistics were last reset.
    * 
    * @descriptor metrics.value=_default
     */
    public long getPacketsSent()
        {
        return 0L;
        }
    
    // Accessor for the property "Priority"
    /**
     * Getter for property Priority.<p>
    * The priority or "weight" of the Member; used to determine tie-breakers.
     */
    public int getPriority()
        {
        return 0;
        }
    
    // Accessor for the property "ProcessName"
    /**
     * Getter for property ProcessName.<p>
    * A configured name that should be the same for Members that are in the
    * same process (JVM), and different for Members that are in different
    * processes. If not explicitly provided, for processes running with JRE 1.5
    * or higher the name will be calculated internally as the Name attribute of
    * the system RuntimeMXBean, which normally represents the process
    * identifier (PID).
     */
    public String getProcessName()
        {
        return null;
        }
    
    // Accessor for the property "ProductEdition"
    /**
     * Getter for property ProductEdition.<p>
    * The product edition this Member is running. Possible values  are:
    * Standard Edition (SE), Enterprise Edition (EE), Grid Edition (GE).
     */
    public String getProductEdition()
        {
        return null;
        }
    
    // Accessor for the property "PublisherPacketUtilization"
    /**
     * Getter for property PublisherPacketUtilization.<p>
    * The publisher packet utilization for this cluster node since the node
    * socket was last reopened. This value is a ratio of the number of bytes
    * sent to the number that would have been sent had all packets been full. A
    * low utilization indicates that data is not being sent in large enough
    * chunks to make efficient use of the network.
     */
    public float getPublisherPacketUtilization()
        {
        return 0.0F;
        }
    
    // Accessor for the property "PublisherSuccessRate"
    /**
     * Getter for property PublisherSuccessRate.<p>
    * The publisher success rate for this cluster node since the node
    * statistics were last reset. Publisher success rate is a ratio of the
    * number of packets successfully delivered in a first attempt to the total
    * number of sent packets. A failure count is incremented when there is no
    * ACK received within a timeout period. It could be caused by either very
    * high network latency or a high packet drop rate.
    * 
    * @descriptor metrics.value=_default
     */
    public float getPublisherSuccessRate()
        {
        return 0.0F;
        }
    
    // Accessor for the property "QuorumStatus"
    /**
     * Getter for property QuorumStatus.<p>
    * The current state of the cluster quorum.
     */
    public String getQuorumStatus()
        {
        return null;
        }
    
    // Accessor for the property "RackName"
    /**
     * Getter for property RackName.<p>
    * A configured name that should be the same for Members that are on the
    * same physical "rack" (or frame or cage), and different for Members that
    * are on different physical "racks".
     */
    public String getRackName()
        {
        return null;
        }
    
    // Accessor for the property "ReceiverPacketUtilization"
    /**
     * Getter for property ReceiverPacketUtilization.<p>
    * The receiver packet utilization for this cluster node since the socket
    * was last reopened. This value is a ratio of the number of bytes received
    * to the number that would have been received had all packets been full. A
    * low utilization indicates that data is not being sent in large enough
    * chunks to make efficient use of the network.
     */
    public float getReceiverPacketUtilization()
        {
        return 0.0F;
        }
    
    // Accessor for the property "ReceiverSuccessRate"
    /**
     * Getter for property ReceiverSuccessRate.<p>
    * The receiver success rate for this cluster node since the node statistics
    * were last reset. Receiver success rate is a ratio of the number of
    * packets successfully acknowledged in a first attempt to the total number
    * of received packets. A failure count is incremented when a re-delivery of
    * previously received packet is detected. It could be caused by either very
    * high inbound network latency or lost ACK packets.
    * 
    * @descriptor metrics.value=_default
     */
    public float getReceiverSuccessRate()
        {
        return 0.0F;
        }
    
    // Accessor for the property "ResendDelay"
    /**
     * Getter for property ResendDelay.<p>
    * The minimum number of milliseconds that a packet will remain queued in
    * the Publisher`s re-send queue before it is resent to the recipient(s) if
    * the packet has not been acknowledged. Setting this value too low can
    * overflow the network with unnecessary repetitions. Setting the value too
    * high can increase the overall latency by delaying the re-sends of dropped
    * packets. Additionally, change of this value may need to be accompanied by
    * a change in SendAckDelay value.
     */
    public int getResendDelay()
        {
        return __m_ResendDelay;
        }
    
    // Accessor for the property "RoleName"
    /**
     * Getter for property RoleName.<p>
    * A configured name that can be used to indicate the role of a Member to
    * the application. While managed by Coherence, this property is used only
    * by the application.
     */
    public String getRoleName()
        {
        return null;
        }
    
    // Accessor for the property "SendAckDelay"
    /**
     * Getter for property SendAckDelay.<p>
    * The minimum number of milliseconds between the queueing of an Ack packet
    * and the sending of the same. This value should be not more then a half of
    * the ResendDelay value.
     */
    public int getSendAckDelay()
        {
        return __m_SendAckDelay;
        }
    
    // Accessor for the property "SendQueueSize"
    /**
     * Getter for property SendQueueSize.<p>
    * The number of packets currently scheduled for delivery. This number
    * includes both packets that are to be sent immediately and packets that
    * have already been sent and awaiting for acknowledgment. Packets that do
    * not receive an acknowledgment within ResendDelay interval will be
    * automatically resent.
    * 
    * @descriptor metrics.value=_default
     */
    public int getSendQueueSize()
        {
        return 0;
        }
    
    // Accessor for the property "SiteName"
    /**
     * Getter for property SiteName.<p>
    * A configured name that should be the same for Members that are on the
    * same physical site (e.g. data center), and different for Members that are
    * on different physical sites.
     */
    public String getSiteName()
        {
        return null;
        }
    
    // Accessor for the property "SocketCount"
    /**
     * Getter for property SocketCount.<p>
    * Number of CPU sockets for the machine this Member is running on.
     */
    public int getSocketCount()
        {
        return 0;
        }
    
    // Accessor for the property "Statistics"
    /**
     * Getter for property Statistics.<p>
    * Statistics for this cluster node in a human readable format.
     */
    public String getStatistics()
        {
        return null;
        }
    
    // Accessor for the property "TcpRingFailures"
    /**
     * Getter for property TcpRingFailures.<p>
    * The number of recovered TcpRing disconnects since the node statistics
    * were last reset. A recoverable disconnect is an abnormal event that is
    * registered when the TcpRing peer drops the TCP connection, but recovers
    * after no more then maximum configured number of attempts.This value will
    * be -1 if the TcpRing is disabled.
     */
    public long getTcpRingFailures()
        {
        return 0L;
        }
    
    // Accessor for the property "Timestamp"
    /**
     * Getter for property Timestamp.<p>
    * The date/time value (in cluster time) that this Member joined the cluster.
     */
    public java.util.Date getTimestamp()
        {
        return null;
        }
    
    // Accessor for the property "TracingSamplingRatio"
    /**
     * Getter for property TracingSamplingRatio.<p>
    * The ratio of spans to trace when tracing is enabled. 
     */
    public float getTracingSamplingRatio()
        {
        return __m_TracingSamplingRatio;
        }
    
    // Accessor for the property "TrafficJamCount"
    /**
     * Getter for property TrafficJamCount.<p>
    * The maximum total number of packets in the send and resend queues that
    * forces the publisher to pause client threads. Zero means no limit.
     */
    public int getTrafficJamCount()
        {
        return __m_TrafficJamCount;
        }
    
    // Accessor for the property "TrafficJamDelay"
    /**
     * Getter for property TrafficJamDelay.<p>
    * The number of milliseconds to pause client threads when a traffic jam
    * condition has been reached. Anything less than one (e.g. zero) is treated
    * as one millisecond.
     */
    public int getTrafficJamDelay()
        {
        return __m_TrafficJamDelay;
        }
    
    // Accessor for the property "TransportBacklogDelay"
    /**
     * Getter for property TransportBacklogDelay.<p>
    * The total number of milliseconds that requests were delayed due to
    * draining the backlog since the node statistics were last reset.
    * 
    * @descriptor metrics.value=_default
     */
    public long getTransportBacklogDelay()
        {
        return __m_TransportBacklogDelay;
        }
    
    // Accessor for the property "TransportReceivedBytes"
    /**
     * Getter for property TransportReceivedBytes.<p>
    * The number of bytes received by the service-dedicated transport since the
    * last time the statistics were reset.
    * 
    * @descriptor metrics.value=_default
     */
    public long getTransportReceivedBytes()
        {
        return 0L;
        }
    
    // Accessor for the property "TransportReceivedMessages"
    /**
     * Getter for property TransportReceivedMessages.<p>
    * The number of messages received by the service-dedicated transport since
    * the last time the statistics were reset.
    * 
    * @descriptor metrics.value=_default
     */
    public long getTransportReceivedMessages()
        {
        return 0L;
        }
    
    // Accessor for the property "TransportRetainedBytes"
    /**
     * Getter for property TransportRetainedBytes.<p>
    * The number of bytes retained by the service-dedicated transport awaiting
    * delivery acknowledgment.  This memory is allocated outside of the Java GC
    * heap space.
    * 
    * @descriptor metrics.value=_default
     */
    public long getTransportRetainedBytes()
        {
        return __m_TransportRetainedBytes;
        }
    
    // Accessor for the property "TransportSentBytes"
    /**
     * Getter for property TransportSentBytes.<p>
    * The number of bytes sent by the service-dedicated transport since the
    * last time the statistics were reset.
    * 
    * @descriptor metrics.value=_default
     */
    public long getTransportSentBytes()
        {
        return 0L;
        }
    
    // Accessor for the property "TransportSentMessages"
    /**
     * Getter for property TransportSentMessages.<p>
    * The number of messages sent by the service-dedicated transport since the
    * last time the statistics were reset.
    * 
    * @descriptor metrics.value=_default
     */
    public long getTransportSentMessages()
        {
        return 0L;
        }
    
    // Accessor for the property "TransportStatus"
    /**
     * Getter for property TransportStatus.<p>
    * The service-dedicated transport status information. 
     */
    public String getTransportStatus()
        {
        return null;
        }
    
    // Accessor for the property "UID"
    /**
     * Getter for property UID.<p>
    * The unique identifier of the Member which is calculated based on its
    * Timestamp, Address, Port and MachineId.  This identifier is unique
    * throughout the life of the cluster.
     */
    public String getUID()
        {
        return null;
        }
    
    // Accessor for the property "UnicastAddress"
    /**
     * Getter for property UnicastAddress.<p>
    * The IP address of the Member`s DatagramSocket for point-to-point
    * communication.
     */
    public String getUnicastAddress()
        {
        return null;
        }
    
    // Accessor for the property "UnicastPort"
    /**
     * Getter for property UnicastPort.<p>
    * The port of the Member`s DatagramSocket for point-to-point communication.
     */
    public int getUnicastPort()
        {
        return 0;
        }
    
    // Accessor for the property "WeakestChannel"
    /**
     * Getter for property WeakestChannel.<p>
    * The id of the cluster node to which this node is having the most
    * difficulty communicating, or -1 if none is found.  A channel is
    * considered to be weak if either the point-to-point publisher or receiver
    * success rates are below 1.0.
     */
    public int getWeakestChannel()
        {
        return 0;
        }
    
    // Accessor for the property "WellKnownAddresses"
    /**
     * Getter for property WellKnownAddresses.<p>
    * An array of well-known socket addresses that this Member uses to join the
    * cluster.
     */
    public String[] getWellKnownAddresses()
        {
        return null;
        }
    
    // Accessor for the property "FlowControlEnabled"
    /**
     * Getter for property FlowControlEnabled.<p>
    * Indicates whether or not FlowControl is enabled.
     */
    public boolean isFlowControlEnabled()
        {
        return false;
        }
    
    // Accessor for the property "MulticastEnabled"
    /**
     * Getter for property MulticastEnabled.<p>
    * Specifies whether or not this Member uses multicast for group
    * communication. If false, this Member will use the WellKnownAddresses to
    * join the cluster and point-to-point unicast to communicate with other
    * Members of the cluster.
     */
    public boolean isMulticastEnabled()
        {
        return false;
        }
    
    // Accessor for the property "NackEnabled"
    /**
     * Getter for property NackEnabled.<p>
    * Indicates whether or not the early packet loss detection protocol is
    * enabled.
     */
    public boolean isNackEnabled()
        {
        return false;
        }
    
    /**
     * Log a full thread dump and outstanding polls for the services running on
    * the node.
     */
    public void logNodeState()
        {
        }
    
    /**
     * Return the environment information for this node. This includes details
    * of the JVM as well as system properties.
     */
    public String reportEnvironment()
        {
        return null;
        }
    
    /**
     * Return a full thread dump and outstanding polls for the services running
    * on the node.
     */
    public String reportNodeState()
        {
        return null;
        }
    
    /**
     * Reset the cluster node statistics.
     */
    public void resetStatistics()
        {
        }
    
    // Accessor for the property "BufferPublishSize"
    /**
     * Setter for property BufferPublishSize.<p>
    * The buffer size of the unicast datagram socket used by the Publisher,
    * measured in the number of packets. Changing this value at runtime is an
    * inherently unsafe operation that will pause all network communications
    * and may result in the termination of all cluster services.
     */
    public void setBufferPublishSize(int cPackets)
        {
        __m_BufferPublishSize = cPackets;
        }
    
    // Accessor for the property "BufferReceiveSize"
    /**
     * Setter for property BufferReceiveSize.<p>
    * The buffer size of the unicast datagram socket used by the Receiver,
    * measured in the number of packets. Changing this value at runtime is an
    * inherently unsafe operation that will pause all network communications
    * and may result in the termination of all cluster services.
     */
    public void setBufferReceiveSize(int cPackets)
        {
        __m_BufferReceiveSize = cPackets;
        }
    
    // Accessor for the property "LoggingFormat"
    /**
     * Setter for property LoggingFormat.<p>
    * Specifies how messages will be formatted before being passed to the log
    * destination
     */
    public void setLoggingFormat(String sFormat)
        {
        __m_LoggingFormat = sFormat;
        }
    
    // Accessor for the property "LoggingLevel"
    /**
     * Setter for property LoggingLevel.<p>
    * Specifies which logged messages will be output to the log destination.
    * Valid values are non-negative integers or -1 to disable all logger output.
     */
    public void setLoggingLevel(int nLevel)
        {
        __m_LoggingLevel = nLevel;
        }
    
    // Accessor for the property "LoggingLimit"
    /**
     * Setter for property LoggingLimit.<p>
    * The maximum number of characters that the logger daemon will process from
    * the message queue before discarding all remaining messages in the queue.
    * Valid values are integers in the range [0...]. Zero implies no limit.
     */
    public void setLoggingLimit(int cChars)
        {
        __m_LoggingLimit = cChars;
        }
    
    // Accessor for the property "MulticastThreshold"
    /**
     * Setter for property MulticastThreshold.<p>
    * The percentage (0 to 100) of the servers in the cluster that a packet
    * will be sent to, above which the packet will be multicasted and below
    * which it will be unicasted.
     */
    public void setMulticastThreshold(int nThreshold)
        {
        __m_MulticastThreshold = nThreshold;
        }
    
    // Accessor for the property "ResendDelay"
    /**
     * Setter for property ResendDelay.<p>
    * The minimum number of milliseconds that a packet will remain queued in
    * the Publisher`s re-send queue before it is resent to the recipient(s) if
    * the packet has not been acknowledged. Setting this value too low can
    * overflow the network with unnecessary repetitions. Setting the value too
    * high can increase the overall latency by delaying the re-sends of dropped
    * packets. Additionally, change of this value may need to be accompanied by
    * a change in SendAckDelay value.
     */
    public void setResendDelay(int cDelay)
        {
        __m_ResendDelay = cDelay;
        }
    
    // Accessor for the property "SendAckDelay"
    /**
     * Setter for property SendAckDelay.<p>
    * The minimum number of milliseconds between the queueing of an Ack packet
    * and the sending of the same. This value should be not more then a half of
    * the ResendDelay value.
     */
    public void setSendAckDelay(int cDelay)
        {
        __m_SendAckDelay = cDelay;
        }
    
    // Accessor for the property "TracingSamplingRatio"
    /**
     * Setter for property TracingSamplingRatio.<p>
    * The ratio of spans to trace when tracing is enabled. 
     */
    public void setTracingSamplingRatio(float fRatio)
        {
        __m_TracingSamplingRatio = fRatio;
        }
    
    // Accessor for the property "TrafficJamCount"
    /**
     * Setter for property TrafficJamCount.<p>
    * The maximum total number of packets in the send and resend queues that
    * forces the publisher to pause client threads. Zero means no limit.
     */
    public void setTrafficJamCount(int cPackets)
        {
        __m_TrafficJamCount = cPackets;
        }
    
    // Accessor for the property "TrafficJamDelay"
    /**
     * Setter for property TrafficJamDelay.<p>
    * The number of milliseconds to pause client threads when a traffic jam
    * condition has been reached. Anything less than one (e.g. zero) is treated
    * as one millisecond.
     */
    public void setTrafficJamDelay(int cDelay)
        {
        __m_TrafficJamDelay = cDelay;
        }
    
    // Accessor for the property "TransportBacklogDelay"
    /**
     * Setter for property TransportBacklogDelay.<p>
    * The total number of milliseconds that requests were delayed due to
    * draining the backlog since the node statistics were last reset.
    * 
    * @descriptor metrics.value=_default
     */
    public void setTransportBacklogDelay(long lDelay)
        {
        __m_TransportBacklogDelay = lDelay;
        }
    
    // Accessor for the property "TransportRetainedBytes"
    /**
     * Setter for property TransportRetainedBytes.<p>
    * The number of bytes retained by the service-dedicated transport awaiting
    * delivery acknowledgment.  This memory is allocated outside of the Java GC
    * heap space.
    * 
    * @descriptor metrics.value=_default
     */
    public void setTransportRetainedBytes(long lBytes)
        {
        __m_TransportRetainedBytes = lBytes;
        }
    
    /**
     * Show detailed status information on the reliable transport.
     */
    public String showTransportStatus()
        {
        return null;
        }
    
    /**
     * Stop all the clustered services running at this node (controlled
    * shutdown). The management of this node will not be available until the
    * node is restarted (manually or programmatically).
     */
    public void shutdown()
        {
        }
    }
