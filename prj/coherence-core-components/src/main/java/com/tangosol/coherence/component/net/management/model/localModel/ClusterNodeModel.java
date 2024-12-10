
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.management.model.localModel.ClusterNodeModel

package com.tangosol.coherence.component.net.management.model.localModel;

import com.tangosol.coherence.component.application.console.Coherence;
import com.tangosol.coherence.component.net.Member;
import com.tangosol.coherence.component.net.MessageHandler;
import com.tangosol.coherence.component.net.Poll;
import com.tangosol.coherence.component.net.TcpRing;
import com.tangosol.coherence.component.net.socket.UdpSocket;
import com.tangosol.coherence.component.util.Queue;
import com.tangosol.coherence.component.util.SafeCluster;
import com.tangosol.coherence.component.util.daemon.queueProcessor.Logger;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.TransportService;
import com.tangosol.coherence.component.util.queue.concurrentQueue.balancedQueue.BundlingQueue;
import com.oracle.coherence.common.base.SingleWaiterCooperativeNotifier;
import com.oracle.coherence.common.internal.net.socketbus.AbstractSocketBus;
import com.oracle.coherence.common.internal.util.HeapDump;
import com.oracle.coherence.common.net.exabus.MessageBus;
import com.tangosol.net.ActionPolicy;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.Cluster;
import com.tangosol.net.ClusterDependencies;
import com.tangosol.net.DescribableAddressProvider;
import com.tangosol.net.GuardSupport;
import com.tangosol.net.InetAddressHelper;
import com.tangosol.net.Service;
import com.tangosol.util.Base;
import com.tangosol.util.ExternalizableHelper;
import java.lang.ref.WeakReference;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Model components implement the JMX-managed functionality of the
 * corresponding MBeans without being dependent on any JMX classes and could be
 * used both in-process and out-of-process (relative to an MBeanServer).
 * 
 * The LocalModel components operate in two distinct modes: live and snapshot.
 * In the live mode all model methods call corresponding methods on managed
 * objects. The snapshot mode uses the _SnapshotMap to keep the attribute
 * values.
 * 
 * Every time a remote invocation is used by the RemoteModel to do a
 * setAttribute or invoke call, the snapshot model is refreshed.
 * 
 * MemberMBean represents a cluster member.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class ClusterNodeModel
        extends    com.tangosol.coherence.component.net.management.model.LocalModel
    {
    // ---- Fields declarations ----
    
    /**
     * Property _Cluster
     *
     * The Cluster object associated with this model.
     */
    private transient com.tangosol.coherence.component.util.SafeCluster __m__Cluster;
    
    /**
     * Property _ClusterRef
     *
     * The Cluster object associated with this model, wrapped into
     * WeakReference to avoid resource leakage.
     */
    private transient java.lang.ref.WeakReference __m__ClusterRef;
    
    /**
     * Property _Member
     *
     * The Member object associated with this model.
     */
    private transient com.tangosol.net.Member __m__Member;
    
    /**
     * Property _MemberRef
     *
     * The Member object associated with this model, wrapped into WeakReference
     * to avoid resource leakage.
     */
    private transient java.lang.ref.WeakReference __m__MemberRef;
    
    /**
     * Property BufferPublishSize
     *
     */
    private int __m_BufferPublishSize;
    
    /**
     * Property BufferReceiveSize
     *
     */
    private int __m_BufferReceiveSize;
    
    /**
     * Property LoggingFormat
     *
     */
    private String __m_LoggingFormat;
    
    /**
     * Property LoggingLevel
     *
     */
    private int __m_LoggingLevel;
    
    /**
     * Property LoggingLimit
     *
     */
    private int __m_LoggingLimit;
    
    /**
     * Property MulticastThreshold
     *
     * The percentage (0 to 100) of the servers in the cluster that a packet
     * will be sent to, above which the packet will be multicasted and below
     * which it will be unicasted.
     */
    private int __m_MulticastThreshold;
    
    /**
     * Property ResendDelay
     *
     */
    private int __m_ResendDelay;
    
    /**
     * Property SendAckDelay
     *
     */
    private int __m_SendAckDelay;
    
    /**
     * Property TracingSamplingRatio
     *
     * The ratio of spans to trace when tracing is enabled. 
     */
    private float __m_TracingSamplingRatio;
    
    /**
     * Property TrafficJamCount
     *
     */
    private int __m_TrafficJamCount;
    
    /**
     * Property TrafficJamDelay
     *
     */
    private int __m_TrafficJamDelay;
    
    // Default constructor
    public ClusterNodeModel()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public ClusterNodeModel(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            set_SnapshotMap(new java.util.HashMap());
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
        return new com.tangosol.coherence.component.net.management.model.localModel.ClusterNodeModel();
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
            clz = Class.forName("com.tangosol.coherence/component/net/management/model/localModel/ClusterNodeModel".replace('/', '.'));
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
     * Return a String with the state of a node including a full thread dump, 
    * outstanding polls and any threads waiting on the polls.
     */
    protected String createNodeState()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid;
        // import Component.Util.Daemon.QueueProcessor.Service.Grid.ClusterService;
        // import Component.Net.Poll;
        // import com.oracle.coherence.common.base.SingleWaiterCooperativeNotifier;
        // import com.tangosol.net.GuardSupport;
        // import java.util.Iterator;
        // import java.util.List;
        
        checkReadOnly("reportNodeState");
        
        StringBuilder sb = new StringBuilder(GuardSupport.getThreadDump());
        
        ClusterService clusterService = get_ClusterService();
        int            cServices      = clusterService.getServiceCount();
        for (int i = 0; i < cServices; i++)
            {
            Grid service   = clusterService.getService(i);
            List listPolls = service == null ? null : service.getOutstandingPolls(0);
            if (listPolls != null && !listPolls.isEmpty())
                {
                sb.append("\n\n *** Outstanding polls: ***");
                for (Iterator iter = listPolls.iterator(); iter.hasNext(); )
                    {
                    Poll poll = (Poll) iter.next();
                    sb.append('\n').append(poll);
        
                    SingleWaiterCooperativeNotifier notifier = poll.getNotifier();
                    if (notifier != null)
                        {
                        Thread thread = notifier.getWaitingThread();
                        if (thread != null)
                            {
                            sb.append("\nWaiting Thread: ").append(thread).append('\n');
                            StackTraceElement[] aElements = thread.getStackTrace();
                            for (int j = 0; j < aElements.length ; j++)
                                {
                                sb.append('\t').append(aElements[j]).append('\n');
                                }
                            }
                        }
                    }
                }
            }
        
        return sb.toString();
        }
    
    public String dumpHeap(String sFileName)
        {
        // import com.oracle.coherence.common.internal.util.HeapDump;
        
        checkReadOnly("dumpHeap");
        
        return HeapDump.dumpHeap(sFileName, true);
        }
    
    public String ensureService(String sServiceName)
        {
        // import com.tangosol.net.CacheFactory;
        // import com.tangosol.net.Service;
        
        checkReadOnly("ensureService");
        
        Service service = CacheFactory.getService(sServiceName);
        return service == null ? null : service.getInfo().getServiceType();
        }
    
    // Accessor for the property "_Cluster"
    /**
     * Getter for property _Cluster.<p>
    * The Cluster object associated with this model.
     */
    public com.tangosol.coherence.component.util.SafeCluster get_Cluster()
        {
        // import Component.Util.SafeCluster;
        // import java.lang.ref.WeakReference;
        
        WeakReference wr = get_ClusterRef();
        return wr == null ? null : (SafeCluster) wr.get();
        }
    
    // Accessor for the property "_ClusterRef"
    /**
     * Getter for property _ClusterRef.<p>
    * The Cluster object associated with this model, wrapped into WeakReference
    * to avoid resource leakage.
     */
    protected java.lang.ref.WeakReference get_ClusterRef()
        {
        return __m__ClusterRef;
        }
    
    // Accessor for the property "_ClusterService"
    /**
     * Getter for property _ClusterService.<p>
     */
    public com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService get_ClusterService()
        {
        // import Component.Util.SafeCluster;
        
        SafeCluster clusterSafe = get_Cluster();
        return clusterSafe != null && clusterSafe.isRunning() ?
            clusterSafe.getCluster().getClusterService() : null;
        }
    
    // Accessor for the property "_ListenerPreferred"
    /**
     * Getter for property _ListenerPreferred.<p>
     */
    public com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketListener get_ListenerPreferred()
        {
        // import Component.Util.SafeCluster;
        
        SafeCluster clusterSafe = get_Cluster();
        return clusterSafe != null && clusterSafe.isRunning() ?
            clusterSafe.getCluster().getPreferredListener() : null;
        }
    
    // Accessor for the property "_Member"
    /**
     * Getter for property _Member.<p>
    * The Member object associated with this model.
     */
    public com.tangosol.net.Member get_Member()
        {
        // import com.tangosol.net.Member;
        // import java.lang.ref.WeakReference;
        
        WeakReference wr = get_MemberRef();
        return wr == null ? null : (Member) wr.get();
        }
    
    // Accessor for the property "_MemberRef"
    /**
     * Getter for property _MemberRef.<p>
    * The Member object associated with this model, wrapped into WeakReference
    * to avoid resource leakage.
     */
    protected java.lang.ref.WeakReference get_MemberRef()
        {
        return __m__MemberRef;
        }
    
    public com.tangosol.coherence.component.net.MessageHandler get_MessageHandler()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid.TransportService;
        
        TransportService transportSvc = get_TransportService();
        return transportSvc == null ? null : transportSvc.getMessageHandler();
        }
    
    // Accessor for the property "_Publisher"
    /**
     * Getter for property _Publisher.<p>
     */
    public com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketPublisher get_Publisher()
        {
        // import Component.Util.SafeCluster;
        
        SafeCluster clusterSafe = get_Cluster();
        return clusterSafe != null && clusterSafe.isRunning() ?
            clusterSafe.getCluster().getPublisher() : null;
        }
    
    // Accessor for the property "_Receiver"
    /**
     * Getter for property _Receiver.<p>
     */
    public com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketReceiver get_Receiver()
        {
        // import Component.Util.SafeCluster;
        
        SafeCluster clusterSafe = get_Cluster();
        return clusterSafe != null && clusterSafe.isRunning() ?
            clusterSafe.getCluster().getReceiver() : null;
        }
    
    // Accessor for the property "_Speaker"
    /**
     * Getter for property _Speaker.<p>
     */
    public com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketSpeaker get_Speaker()
        {
        // import Component.Util.SafeCluster;
        
        SafeCluster clusterSafe = get_Cluster();
        return clusterSafe != null && clusterSafe.isRunning() ?
            clusterSafe.getCluster().getSpeaker() : null;
        }
    
    // Accessor for the property "_TcpRing"
    /**
     * Getter for property _TcpRing.<p>
     */
    public com.tangosol.coherence.component.net.TcpRing get_TcpRing()
        {
        // import Component.Util.SafeCluster;
        
        SafeCluster clusterSafe = get_Cluster();
        return clusterSafe != null && clusterSafe.isRunning() ?
            clusterSafe.getCluster().getClusterService().getTcpRing() : null;
        }
    
    public com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.TransportService get_TransportService()
        {
        // import Component.Util.SafeCluster;
        
        SafeCluster clusterSafe = get_Cluster();
        return clusterSafe != null && clusterSafe.isRunning() ?
            clusterSafe.getCluster().getTransportService() : null;
        }
    
    // Accessor for the property "BufferPublishSize"
    /**
     * Getter for property BufferPublishSize.<p>
     */
    public int getBufferPublishSize()
        {
        // import Component.Util.Daemon.QueueProcessor.PacketProcessor.PacketPublisher as com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketPublisher;
        
        com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketPublisher publisher = get_Publisher();
        return publisher == null ? -1 : publisher.getUdpSocketUnicast().getBufferSentBytes() / publisher.getPreferredPacketLength();
        }
    
    // Accessor for the property "BufferReceiveSize"
    /**
     * Getter for property BufferReceiveSize.<p>
     */
    public int getBufferReceiveSize()
        {
        // import Component.Util.Daemon.QueueProcessor.PacketProcessor.PacketListener as com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketListener;
        
        com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketListener listener = get_ListenerPreferred();
        return listener == null ? -1 : listener.getUdpSocket().getBufferReceivedBytes() / listener.getPacketLength();
        }
    
    // Accessor for the property "CpuCount"
    /**
     * Getter for property CpuCount.<p>
    * Number of CPU cores for the machine this Member is running on.
     */
    public int getCpuCount()
        {
        // import Component.Net.Member;
        
        Member member = (Member) get_Member();
        return member == null ? -1 : member.getCpuCount();
        }
    
    // Declared at the super level
    /**
     * Getter for property Description.<p>
    * Human readable description.
    * 
    * @see Manageable.ModelAdapter#toString()
     */
    public String getDescription()
        {
        // import com.tangosol.net.Member;
        
        Member member = (Member) get_Member();
        return member == null ? "n/a" : "Uid=" + member.getUid();
        }
    
    // Accessor for the property "GuardRecoverCount"
    /**
     * Getter for property GuardRecoverCount.<p>
    * The number of recovery attempts executed for all guardables on this node
    * since the node statistics were last reset.
     */
    public int getGuardRecoverCount()
        {
        // import Component.Util.SafeCluster;
        
        SafeCluster clusterSafe = get_Cluster();
        return clusterSafe != null && clusterSafe.isRunning() ?
            clusterSafe.getCluster().getGuardRecoverCount() : -1;
        }
    
    // Accessor for the property "GuardTerminateCount"
    /**
     * Getter for property GuardTerminateCount.<p>
    * The number of termination attempts executed for all guardables on this
    * node since the node statistics were last reset.
     */
    public int getGuardTerminateCount()
        {
        // import Component.Util.SafeCluster;
        
        SafeCluster clusterSafe = get_Cluster();
        return clusterSafe != null && clusterSafe.isRunning() ?
            clusterSafe.getCluster().getGuardTerminateCount() : -1;
        }
    
    // Accessor for the property "Id"
    /**
     * Getter for property Id.<p>
    * Return a small number that uniquely identifies the Member at this point
    * in time and does not change for the life of this Member. 
     */
    public int getId()
        {
        // import com.tangosol.net.Member;
        
        Member member = (Member) get_Member();
        return member == null ? -1 : member.getId();
        }
    
    // Accessor for the property "LoggingDestination"
    /**
     * Getter for property LoggingDestination.<p>
     */
    public String getLoggingDestination()
        {
        // import Component.Application.Console.Coherence;
        // import Component.Util.Daemon.QueueProcessor.Logger;
        
        Coherence singleton = (Coherence) Coherence.get_Instance();
        Logger    logger    = singleton.getLogger();
        
        return logger == null ? null : logger.getDestination();
        }
    
    // Accessor for the property "LoggingFormat"
    /**
     * Getter for property LoggingFormat.<p>
     */
    public String getLoggingFormat()
        {
        // import Component.Application.Console.Coherence;
        // import Component.Util.Daemon.QueueProcessor.Logger;
        
        Coherence singleton = (Coherence) Coherence.get_Instance();
        Logger    logger    = singleton.getLogger();
        
        return logger == null ? null : logger.getFormat();
        }
    
    // Accessor for the property "LoggingLevel"
    /**
     * Getter for property LoggingLevel.<p>
     */
    public int getLoggingLevel()
        {
        // import Component.Application.Console.Coherence;
        // import Component.Util.Daemon.QueueProcessor.Logger;
        
        Coherence singleton = (Coherence) Coherence.get_Instance();
        Logger    logger    = singleton.getLogger();
        
        return logger == null ? -1 : logger.getLevel();
        }
    
    // Accessor for the property "LoggingLimit"
    /**
     * Getter for property LoggingLimit.<p>
     */
    public int getLoggingLimit()
        {
        // import Component.Application.Console.Coherence;
        // import Component.Util.Daemon.QueueProcessor.Logger;
        
        Coherence singleton = (Coherence) Coherence.get_Instance();
        Logger    logger    = singleton.getLogger();
        
        return logger == null ? -1 : logger.getLimit();
        }
    
    // Accessor for the property "MachineId"
    /**
     * Getter for property MachineId.<p>
    * Return the Member's machine Id. 
     */
    public int getMachineId()
        {
        // import com.tangosol.net.Member;
        
        Member member = (Member) get_Member();
        return member == null ? -1 : member.getMachineId();
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
        // import com.tangosol.net.Member;
        
        Member member = (Member) get_Member();
        return member == null ?  canonicalString(null) : canonicalString(member.getMachineName());
        }
    
    // Accessor for the property "MemberName"
    /**
     * Getter for property MemberName.<p>
    * A configured name that must be unique for every Member.
     */
    public String getMemberName()
        {
        // import com.tangosol.net.Member;
        
        Member member = (Member) get_Member();
        return member == null ? canonicalString(null) : canonicalString(member.getMemberName());
        }
    
    // Accessor for the property "MemoryAvailableMB"
    /**
     * Getter for property MemoryAvailableMB.<p>
    * The total amount of memory in the JVM available for new objects in MB.
     */
    public int getMemoryAvailableMB()
        {
        Runtime runtime = Runtime.getRuntime();
        
        return (int) ((runtime.freeMemory() + runtime.maxMemory() - runtime.totalMemory()) / (1024*1024));
        }
    
    // Accessor for the property "MemoryMaxMB"
    /**
     * Getter for property MemoryMaxMB.<p>
    * The maximum amount of memory that the JVM will attempt to use in MB.
     */
    public int getMemoryMaxMB()
        {
        return (int) (Runtime.getRuntime().maxMemory() / (1024*1024));
        }
    
    // Accessor for the property "MulticastAddress"
    /**
     * Getter for property MulticastAddress.<p>
    * The IP address of the Member's MulticastSocket for group communication.
     */
    public String getMulticastAddress()
        {
        // import Component.Util.Daemon.QueueProcessor.PacketProcessor.PacketPublisher as com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketPublisher;
        // import java.net.InetSocketAddress;
        
        com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketPublisher         publisher = get_Publisher();
        InetSocketAddress addr      = publisher == null ? null : publisher.getMulticastAddress();
        
        return addr == null ? canonicalString(null) : String.valueOf(addr.getAddress());
        }
    
    // Accessor for the property "MulticastPort"
    /**
     * Getter for property MulticastPort.<p>
    * The port of the Member's MulticastSocket for group communication.
     */
    public int getMulticastPort()
        {
        // import com.tangosol.net.Cluster;
        // import com.tangosol.net.ClusterDependencies;
        
        Cluster cluster = get_Cluster();
        if (cluster != null)
            {
            ClusterDependencies clusterDeps = cluster.getDependencies();
            if (clusterDeps != null)
                {
                return clusterDeps.getGroupPort();
                }
            }
        return -1;
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
        // import Component.Util.Daemon.QueueProcessor.PacketProcessor.PacketPublisher as com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketPublisher;
        
        com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketPublisher publisher = get_Publisher();
        return publisher == null ? -1 : (int) (100*publisher.getMulticastThreshold());
        }
    
    // Accessor for the property "MulticastTTL"
    /**
     * Getter for property MulticastTTL.<p>
    * The time-to-live for multicast packets sent out on this Member's
    * MulticastSocket.
     */
    public int getMulticastTTL()
        {
        // import Component.Util.SafeCluster;
        
        SafeCluster cluster = get_Cluster();
        return cluster == null ? -1 : cluster.getCluster().getDependencies().getGroupTimeToLive();
        }
    
    // Accessor for the property "NackSent"
    /**
     * Getter for property NackSent.<p>
    * The total number of NACK packets sent since the node statistics were last
    * reset.
     */
    public long getNackSent()
        {
        // import Component.Util.Daemon.QueueProcessor.PacketProcessor.PacketPublisher as com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketPublisher;
        
        com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketPublisher publisher = get_Publisher();
        return publisher == null ? -1L : publisher.getStatsNacksSent();
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
        // import Component.Util.Daemon.QueueProcessor.PacketProcessor.PacketPublisher as com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketPublisher;
        
        com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketPublisher publisher = get_Publisher();
        if (publisher == null)
            {
            return 0.0f;
            }
        else
            {
            long lSent   = publisher.getStatsSent();
            long lWasted = publisher.getStatsResentExcess();
        
            return lSent == 0L ? 1.0f
                               : (float) (1.0 - ((double) lWasted) / ((double) lSent));
            }
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
        // import Component.Util.Daemon.QueueProcessor.PacketProcessor.PacketSpeaker as com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketSpeaker;
        // import Component.Util.Queue.ConcurrentQueue.BalancedQueue.BundlingQueue;
        
        com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketSpeaker speaker = get_Speaker();
        return speaker == null ? -1L : ((BundlingQueue) speaker.getQueue()).getStatsBundled();
        }
    
    // Accessor for the property "PacketsReceived"
    /**
     * Getter for property PacketsReceived.<p>
     */
    public long getPacketsReceived()
        {
        // import Component.Util.Daemon.QueueProcessor.PacketProcessor.PacketReceiver as com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketReceiver;
        
        com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketReceiver receiver = get_Receiver();
        return receiver == null ? -1L : receiver.getStatsReceived();
        }
    
    // Accessor for the property "PacketsRepeated"
    /**
     * Getter for property PacketsRepeated.<p>
     */
    public long getPacketsRepeated()
        {
        // import Component.Util.Daemon.QueueProcessor.PacketProcessor.PacketReceiver as com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketReceiver;
        
        com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketReceiver receiver = get_Receiver();
        return receiver == null ? -1L : receiver.getStatsRepeated();
        }
    
    // Accessor for the property "PacketsResent"
    /**
     * Getter for property PacketsResent.<p>
     */
    public long getPacketsResent()
        {
        // import Component.Util.Daemon.QueueProcessor.PacketProcessor.PacketPublisher as com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketPublisher;
        
        com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketPublisher publisher = get_Publisher();
        return publisher == null ? -1L : publisher.getStatsResent();
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
        // import Component.Util.Daemon.QueueProcessor.PacketProcessor.PacketPublisher as com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketPublisher;
        
        com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketPublisher publisher = get_Publisher();
        return publisher == null ? -1L : publisher.getStatsResentEarly();
        }
    
    // Accessor for the property "PacketsResentExcess"
    /**
     * Getter for property PacketsResentExcess.<p>
    * The total number of packet retransmissions which were later proven
    * unnecessary.
     */
    public long getPacketsResentExcess()
        {
        // import Component.Util.Daemon.QueueProcessor.PacketProcessor.PacketPublisher as com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketPublisher;
        
        com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketPublisher publisher = get_Publisher();
        return publisher == null ? -1L : publisher.getStatsResentExcess();
        }
    
    // Accessor for the property "PacketsSent"
    /**
     * Getter for property PacketsSent.<p>
     */
    public long getPacketsSent()
        {
        // import Component.Util.Daemon.QueueProcessor.PacketProcessor.PacketPublisher as com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketPublisher;
        
        com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketPublisher publisher = get_Publisher();
        return publisher == null ? -1L : publisher.getStatsSent();
        }
    
    // Accessor for the property "Priority"
    /**
     * Getter for property Priority.<p>
    * The priority or "weight" of the Member; used to determine tie-breakers.
     */
    public int getPriority()
        {
        // import com.tangosol.net.Member;
        
        Member member = (Member) get_Member();
        return member == null ? -1 : member.getPriority();
        }
    
    // Accessor for the property "ProcessName"
    /**
     * Getter for property ProcessName.<p>
    * A configured name that should be the same for Members that are in the
    * same process (JVM), and different for Members that are in different
    * processes.
     */
    public String getProcessName()
        {
        // import com.tangosol.net.Member;
        
        Member member = (Member) get_Member();
        return member == null ? canonicalString(null) : canonicalString(member.getProcessName());
        }
    
    // Accessor for the property "ProductEdition"
    /**
     * Getter for property ProductEdition.<p>
    * A configured name that should be the same for Members that are in the
    * same process (JVM), and different for Members that are in different
    * processes.
     */
    public String getProductEdition()
        {
        // import Component.Net.Member;
        
        Member member = (Member) get_Member();
        return member == null ? "n/a" : Member.EDITION_NAME[member.getEdition()];
        }
    
    // Accessor for the property "PublisherPacketUtilization"
    /**
     * Getter for property PublisherPacketUtilization.<p>
     */
    public float getPublisherPacketUtilization()
        {
        // import Component.Net.Cluster$SocketManager as com.tangosol.coherence.component.net.Cluster.SocketManager;
        // import Component.Net.Socket.UdpSocket;
        // import Component.Util.SafeCluster;
        
        SafeCluster clusterSafe = get_Cluster();
        if (clusterSafe != null && clusterSafe.isRunning())
            {
            com.tangosol.coherence.component.net.Cluster.SocketManager manager = clusterSafe.getCluster().getSocketManager();
            UdpSocket socket      = manager.getUnicastUdpSocket();    
            UdpSocket socketPref  = manager.getPreferredUnicastUdpSocket();    
            UdpSocket socketBroad = manager.getBroadcastUdpSocket();
            long      cPackets    = socketPref.getCountSent() + socket.getCountSent() + socketBroad.getCountSent();
            long      cBytes      = socketPref.getBytesSent() + socket.getBytesSent() + socketBroad.getBytesSent();
            
            if (cPackets == 0L)
                {
                return 1.0f;
                }
            
            return (float) (((double) cBytes / cPackets)
                 / (double) socketPref.getPacketLength());
            }
        else
            {
            return 0.0f;    
            }
        }
    
    // Accessor for the property "PublisherSuccessRate"
    /**
     * Getter for property PublisherSuccessRate.<p>
     */
    public float getPublisherSuccessRate()
        {
        // import Component.Util.Daemon.QueueProcessor.PacketProcessor.PacketPublisher as com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketPublisher;
        
        com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketPublisher publisher = get_Publisher();
        if (publisher == null)
            {
            return 0.0f;
            }
        else
            {
            long lSent   = publisher.getStatsSent();
            long lResent = publisher.getStatsResent();
        
            return lSent == 0L ? 1.0f : (float) (1.0 - ((double) lResent)/((double) lSent));
            }
        }
    
    // Accessor for the property "QuorumStatus"
    /**
     * Getter for property QuorumStatus.<p>
    * The current state of the cluster quorum.
     */
    public String getQuorumStatus()
        {
        // import com.tangosol.net.ActionPolicy;
        // import com.tangosol.net.ConfigurableQuorumPolicy as com.tangosol.net.ConfigurableQuorumPolicy;
        // import com.tangosol.util.NullImplementation$NullActionPolicy as com.tangosol.util.NullImplementation.NullActionPolicy;
        // import Component.Util.Daemon.QueueProcessor.Service.Grid.ClusterService;
        
        StringBuilder  sbQuorum = new StringBuilder();
        ClusterService service  = get_ClusterService();
        
        if (service != null)
            {
            ActionPolicy policy  = service.getActionPolicy();
            String       sPolicy = null;
            String       sStatus = service.getQuorumControl().formatStatus();
        
            if (sStatus.length() > 0)
                {
                sbQuorum.append(sStatus).append("; ");
                }
        
            if (policy instanceof com.tangosol.util.NullImplementation.NullActionPolicy)
                {
                // special-case the common scenario of no quorum-policy configured
                sPolicy = "Not configured";
                }
            else if (policy instanceof com.tangosol.net.ConfigurableQuorumPolicy)
                {
                sPolicy = ((com.tangosol.net.ConfigurableQuorumPolicy) policy).getStatusDescription();
                }
            else if (policy != null)
                {
                sPolicy = policy.toString();
                }
        
            if (sPolicy != null)
                {
                sbQuorum.append(sPolicy);
                }
            }
        
        return canonicalString(sbQuorum.toString());
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
        // import com.tangosol.net.Member;
        
        Member member = (Member) get_Member();
        return member == null ? canonicalString(null) : canonicalString(member.getRackName());
        }
    
    // Accessor for the property "ReceiverPacketUtilization"
    /**
     * Getter for property ReceiverPacketUtilization.<p>
     */
    public float getReceiverPacketUtilization()
        {
        // import Component.Net.Cluster$SocketManager as com.tangosol.coherence.component.net.Cluster.SocketManager;
        // import Component.Net.Socket.UdpSocket;
        // import Component.Util.SafeCluster;
        
        SafeCluster clusterSafe = get_Cluster();
        if (clusterSafe != null && clusterSafe.isRunning())
            {
            com.tangosol.coherence.component.net.Cluster.SocketManager manager = clusterSafe.getCluster().getSocketManager();
            UdpSocket socket      = manager.getUnicastUdpSocket();    
            UdpSocket socketPref  = manager.getPreferredUnicastUdpSocket();    
            UdpSocket socketBroad = manager.getBroadcastUdpSocket();
            long      cPackets    = socketPref.getCountReceived() + socket.getCountReceived() + socketBroad.getCountReceived();
            long      cBytes      = socketPref.getBytesReceived() + socket.getBytesReceived() + socketBroad.getBytesReceived();
            
            if (cPackets == 0L)
                {
                return 1.0f;
                }
            
            return (float) (((double) cBytes / cPackets)
                 / (double) socketPref.getPacketLength());
            }
        else
            {
            return 0.0f;    
            }
        }
    
    // Accessor for the property "ReceiverSuccessRate"
    /**
     * Getter for property ReceiverSuccessRate.<p>
     */
    public float getReceiverSuccessRate()
        {
        // import Component.Util.Daemon.QueueProcessor.PacketProcessor.PacketReceiver as com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketReceiver;
        
        com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketReceiver receiver = get_Receiver();
        if (receiver == null)
            {
            return 0.0f;
            }
        else
            {
            long lReceived = receiver.getStatsReceived();
            long lRepeated = receiver.getStatsRepeated();
        
            return lReceived == 0L ? 1.0f : (float) (1.0 - ((double) lRepeated)/((double) lReceived));
            }
        }
    
    // Accessor for the property "ResendDelay"
    /**
     * Getter for property ResendDelay.<p>
     */
    public int getResendDelay()
        {
        // import Component.Util.Daemon.QueueProcessor.PacketProcessor.PacketPublisher as com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketPublisher;
        
        com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketPublisher publisher = get_Publisher();
        return publisher == null ? -1 : (int) publisher.getResendDelay();
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
        // import com.tangosol.net.Member;
        
        Member member = (Member) get_Member();
        return member == null ? canonicalString(null) : canonicalString(member.getRoleName());
        }
    
    // Accessor for the property "SendAckDelay"
    /**
     * Getter for property SendAckDelay.<p>
     */
    public int getSendAckDelay()
        {
        // import Component.Util.Daemon.QueueProcessor.PacketProcessor.PacketPublisher as com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketPublisher;
        
        com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketPublisher publisher = get_Publisher();
        return publisher == null ? -1 : (int) publisher.getAckDelay();
        }
    
    // Accessor for the property "SendQueueSize"
    /**
     * Getter for property SendQueueSize.<p>
     */
    public int getSendQueueSize()
        {
        // import Component.Util.Daemon.QueueProcessor.PacketProcessor.PacketPublisher as com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketPublisher;
        // import Component.Util.Daemon.QueueProcessor.PacketProcessor.PacketPublisher$InQueue as com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketPublisher.InQueue;
        // import Component.Util.Queue;
        
        com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketPublisher publisher = get_Publisher();
        
        if (publisher == null)
            {
            return -1;
            }
        else
            {
            int iSize = ((com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketPublisher.InQueue) publisher.getQueue()).getPacketAdapter().size() +
                        publisher.getResendQueue().size();
            
            Queue queueDeferred = publisher.getDeferredReadyQueue();
            if (queueDeferred != null)
                {
                iSize += queueDeferred.size();
                }
            // TODO: take into account the per-member deferred queues
            return iSize;
            }
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
        // import com.tangosol.net.Member;
        
        Member member = (Member) get_Member();
        return member == null ? canonicalString(null) : canonicalString(member.getSiteName());
        }
    
    // Accessor for the property "SocketCount"
    /**
     * Getter for property SocketCount.<p>
    * Number of CPU sockets for the machine this Member is running on.
     */
    public int getSocketCount()
        {
        // import Component.Net.Member;
        
        Member member = (Member) get_Member();
        return member != null ? -1 : member.getSocketCount();
        }
    
    // Accessor for the property "Statistics"
    /**
     * Getter for property Statistics.<p>
    * Returns statisics for this cluster node.
     */
    public String getStatistics()
        {
        // import Component.Util.SafeCluster;
        
        SafeCluster clusterSafe = get_Cluster();
        if (clusterSafe != null && clusterSafe.isRunning())
            {
            return clusterSafe.getCluster().formatStats();
            }
        else
            {
            return "n/a";
            }
        }
    
    // Accessor for the property "TcpRingFailures"
    /**
     * Getter for property TcpRingFailures.<p>
     */
    public long getTcpRingFailures()
        {
        // import Component.Net.TcpRing;
        
        TcpRing ring = get_TcpRing();
        return ring != null ? ring.getStatsFailures() : -1L;
        }
    
    // Accessor for the property "Timestamp"
    /**
     * Getter for property Timestamp.<p>
    * Return the date/time value (in cluster time) that this Member joined the
    * cluster.
     */
    public java.util.Date getTimestamp()
        {
        // import com.tangosol.net.Member;
        // import java.util.Date;
        
        Member member = (Member) get_Member();
        return member == null ? new Date(0) : new Date(member.getTimestamp());
        }
    
    // Accessor for the property "TracingSamplingRatio"
    /**
     * Getter for property TracingSamplingRatio.<p>
    * The ratio of spans to trace when tracing is enabled. 
     */
    public float getTracingSamplingRatio()
        {
        // import Component.Util.SafeCluster;
        // import com.tangosol.internal.tracing.TracingShim$Control as com.tangosol.internal.tracing.TracingShim.Control;
        
        SafeCluster clusterSafe = get_Cluster();
        if (clusterSafe != null && clusterSafe.isRunning())
            {
            com.tangosol.internal.tracing.TracingShim.Control control = clusterSafe.getCluster().getTracingControl();
        
            if (control != null)
                {
                return control.getDependencies().getSamplingRatio();
                }
            }
        
        return -1;
        }
    
    // Accessor for the property "TrafficJamCount"
    /**
     * Getter for property TrafficJamCount.<p>
     */
    public int getTrafficJamCount()
        {
        // import Component.Util.Daemon.QueueProcessor.PacketProcessor.PacketPublisher as com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketPublisher;
        
        com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketPublisher publisher = get_Publisher();
        return publisher == null ? -1 : (int) publisher.getCloggedCount();
        }
    
    // Accessor for the property "TrafficJamDelay"
    /**
     * Getter for property TrafficJamDelay.<p>
     */
    public int getTrafficJamDelay()
        {
        // import Component.Util.Daemon.QueueProcessor.PacketProcessor.PacketPublisher as com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketPublisher;
        
        com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketPublisher publisher = get_Publisher();
        return publisher == null ? -1 : (int) publisher.getCloggedDelay();
        }
    
    // Accessor for the property "TransportBacklogDelay"
    /**
     * Getter for property TransportBacklogDelay.<p>
     */
    public long getTransportBacklogDelay()
        {
        // import Component.Net.MessageHandler;
        
        MessageHandler handler = get_MessageHandler();
        
        return handler == null
            ? -1L
            : handler.getStatsDrainOverflowDuration().longValue();
        }
    
    // Accessor for the property "TransportReceivedBytes"
    /**
     * Getter for property TransportReceivedBytes.<p>
     */
    public long getTransportReceivedBytes()
        {
        // import Component.Net.MessageHandler;
        
        MessageHandler handler = get_MessageHandler();
        
        return handler == null
            ? -1L
            : handler.getStatsBusBytesIn();
        }
    
    // Accessor for the property "TransportReceivedMessages"
    /**
     * Getter for property TransportReceivedMessages.<p>
     */
    public long getTransportReceivedMessages()
        {
        // import Component.Net.MessageHandler;
        
        MessageHandler handler = get_MessageHandler();
        
        return handler == null
            ? -1L
            : handler.getStatsBusReceives();
        }
    
    // Accessor for the property "TransportRetainedBytes"
    /**
     * Getter for property TransportRetainedBytes.<p>
     */
    public long getTransportRetainedBytes()
        {
        // import Component.Net.MessageHandler;
        
        MessageHandler handler = get_MessageHandler();
        
        return handler == null
            ? -1L
            : handler.getStatsBusBytesOutBuffered().get();
        }
    
    // Accessor for the property "TransportSentBytes"
    /**
     * Getter for property TransportSentBytes.<p>
     */
    public long getTransportSentBytes()
        {
        // import Component.Net.MessageHandler;
        
        MessageHandler handler = get_MessageHandler();
        
        return handler == null
            ? -1L
            : handler.getStatsBusBytesOut();
        }
    
    // Accessor for the property "TransportSentMessages"
    /**
     * Getter for property TransportSentMessages.<p>
     */
    public long getTransportSentMessages()
        {
        // import Component.Net.MessageHandler;
        
        MessageHandler handler = get_MessageHandler();
        
        return handler == null
            ? -1L
            : handler.getStatsBusSends();
        }
    
    // Accessor for the property "TransportStatus"
    /**
     * Getter for property TransportStatus.<p>
     */
    public String getTransportStatus()
        {
        return getTransportStatus(false);
        }
    
    // Accessor for the property "TransportStatus"
    /**
     * Getter for property TransportStatus.<p>
     */
    public String getTransportStatus(boolean fVerbose)
        {
        // import com.oracle.coherence.common.internal.net.socketbus.AbstractSocketBus;
        // import com.oracle.coherence.common.net.exabus.MessageBus;
        // import Component.Net.MessageHandler;
        
        MessageHandler handler = get_MessageHandler();
        if (handler != null) {
            MessageBus messageBus = handler.getMessageBus();
            return messageBus instanceof AbstractSocketBus
                   ? ((AbstractSocketBus) messageBus).toString(fVerbose)
                   : messageBus.toString();
        }
        
        StringBuilder sb = new StringBuilder("Datagram(");
        
        String sAddress = getUnicastAddress();
        if (!"n/a".equals(sAddress)) {
            sb.append('/').append(sAddress);
        }
        
        int nPort = getUnicastPort();
        if (nPort != -1) {
            sb.append(':').append(nPort);
        }
        
        String sStats = getStatistics();
        if (!"n/a".equals(sStats)) {
            if (sb.length() > 9) {
              sb.append(',');
            }
            sb.append(sStats);
        }
        sb.append(')');
        return sb.toString();
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
        // import com.tangosol.net.Member;
        
        Member member = (Member) get_Member();
        return member == null ? "n/a" : member.getUid().toString();
        }
    
    // Accessor for the property "UnicastAddress"
    /**
     * Getter for property UnicastAddress.<p>
    * The IP address of the Member's DatagramSocket for point-to-point
    * communication.
     */
    public String getUnicastAddress()
        {
        // import com.tangosol.net.Member;
        
        Member member = (Member) get_Member();
        return member == null ? "n/a" : String.valueOf(member.getAddress());
        }
    
    // Accessor for the property "UnicastPort"
    /**
     * Getter for property UnicastPort.<p>
    * The port of the Member's DatagramSocket for point-to-point communication.
     */
    public int getUnicastPort()
        {
        // import com.tangosol.net.Member;
        
        Member member = (Member) get_Member();
        return member == null ? -1 : member.getPort();
        }
    
    // Accessor for the property "WeakestChannel"
    /**
     * Getter for property WeakestChannel.<p>
    * The id of the cluster node to which this node is having the most
    * difficulty communicating, or -1 if none is found.
     */
    public int getWeakestChannel()
        {
        // import Component.Net.Member;
        // import Component.Util.Daemon.QueueProcessor.PacketProcessor.PacketPublisher as com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketPublisher;
        
        com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketPublisher publisher = get_Publisher();
        if (publisher != null)
            {
            Member memberWorst = Member.findWeakestMember(publisher.getMemberSet());
            if (memberWorst != null)
                {
                return memberWorst.getId();
                }
            }
        
        return -1;
        }
    
    // Accessor for the property "WellKnownAddresses"
    /**
     * Getter for property WellKnownAddresses.<p>
    * An array of well-known socket addresses that this Member uses to join the
    * cluster.
     */
    public String[] getWellKnownAddresses()
        {
        // import Component.Util.SafeCluster;
        // import com.tangosol.net.DescribableAddressProvider;
        // import com.tangosol.net.InetAddressHelper;
        // import java.net.InetSocketAddress;
        // import java.util.Iterator;
        // import java.util.Set;
        
        SafeCluster clusterSafe = get_Cluster();
        if (clusterSafe == null)
            {
            return new String[0];
            }
        
        Set setWka = clusterSafe.isRunning() ?
            clusterSafe.getCluster().getClusterService().getWellKnownAddresses() : null;
        
        if (setWka == null)
            {
            return new String[0];
            }
        
        if (setWka instanceof DescribableAddressProvider)
            {
            return ((DescribableAddressProvider) setWka).getAddressDescriptions();
            }
        
        return InetAddressHelper.getAddressDescriptions(setWka);
        }
    
    // Accessor for the property "FlowControlEnabled"
    /**
     * Getter for property FlowControlEnabled.<p>
    * Indicates if FlowControl is enabled.
     */
    public boolean isFlowControlEnabled()
        {
        // import Component.Net.Member$FlowControl as com.tangosol.coherence.component.net.Member.FlowControl;
        
        return com.tangosol.coherence.component.net.Member.FlowControl.isEnabled();
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
        // import Component.Util.Daemon.QueueProcessor.PacketProcessor.PacketPublisher as com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketPublisher;
        
        com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketPublisher publisher = get_Publisher();
        return publisher == null ? false : publisher.isMulticastEnabled();
        }
    
    // Accessor for the property "NackEnabled"
    /**
     * Getter for property NackEnabled.<p>
    * Indicates if NACK packets are enabled.
     */
    public boolean isNackEnabled()
        {
        // import Component.Util.Daemon.QueueProcessor.PacketProcessor.PacketPublisher as com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketPublisher;
        
        com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketPublisher publisher = get_Publisher();
        return publisher == null ? false : publisher.isNackEnabled();
        }
    
    /**
     * Log a full thread dump and outstanding polls for the services running on
    * the node.
     */
    public void logNodeState()
        {
        checkReadOnly("logNodeState");
        
        _trace(createNodeState(), 1);
        }

    /**
     * Get member description.
     */
    public String getNodeDescription()
        {
        Member member = (Member) get_Member();
        return member == null ? "n/a" : member.toString(Member.SHOW_LICENSE);
        }
    
    // Declared at the super level
    /**
     * Must be supplemented at each specific Model implementation.
     */
    public void readExternal(java.io.DataInput in)
            throws java.io.IOException
        {
        // import com.tangosol.util.Base;
        // import com.tangosol.util.ExternalizableHelper;
        // import java.util.Date;
        // import java.util.Map;
        
        super.readExternal(in);
        
        Map mapSnapshot = get_SnapshotMap();
        
        mapSnapshot.put("BufferPublishSize", Base.makeInteger(ExternalizableHelper.readInt(in)));
        mapSnapshot.put("BufferReceiveSize", Base.makeInteger(ExternalizableHelper.readInt(in)));
        mapSnapshot.put("CpuCount", Base.makeInteger(ExternalizableHelper.readInt(in)));
        mapSnapshot.put("FlowControlEnabled", Boolean.valueOf(in.readBoolean()));
        mapSnapshot.put("GuardRecoverCount", Base.makeInteger(ExternalizableHelper.readInt(in)));
        mapSnapshot.put("GuardTerminateCount", Base.makeInteger(ExternalizableHelper.readInt(in)));
        mapSnapshot.put("Id", Base.makeInteger(ExternalizableHelper.readInt(in)));
        mapSnapshot.put("LoggingDestination", ExternalizableHelper.readSafeUTF(in));
        mapSnapshot.put("LoggingFormat", ExternalizableHelper.readSafeUTF(in));
        mapSnapshot.put("LoggingLevel", Base.makeInteger(ExternalizableHelper.readInt(in)));
        mapSnapshot.put("LoggingLimit", Base.makeInteger(ExternalizableHelper.readInt(in)));
        mapSnapshot.put("MachineId", Base.makeInteger(ExternalizableHelper.readInt(in)));
        mapSnapshot.put("MachineName", ExternalizableHelper.readSafeUTF(in));
        mapSnapshot.put("MemberName", ExternalizableHelper.readSafeUTF(in));
        mapSnapshot.put("MemoryAvailableMB", Base.makeInteger(ExternalizableHelper.readInt(in)));
        mapSnapshot.put("MemoryMaxMB", Base.makeInteger(ExternalizableHelper.readInt(in)));
        mapSnapshot.put("MulticastAddress", ExternalizableHelper.readSafeUTF(in));
        mapSnapshot.put("MulticastEnabled", Boolean.valueOf(in.readBoolean()));
        mapSnapshot.put("MulticastPort", Base.makeInteger(ExternalizableHelper.readInt(in)));
        mapSnapshot.put("MulticastThreshold", Base.makeInteger(ExternalizableHelper.readInt(in)));
        mapSnapshot.put("MulticastTTL", Base.makeInteger(ExternalizableHelper.readInt(in)));
        mapSnapshot.put("NackEnabled", Boolean.valueOf(in.readBoolean()));
        mapSnapshot.put("NackSent", Base.makeLong(ExternalizableHelper.readLong(in)));
        mapSnapshot.put("PacketDeliveryEfficiency", Float.valueOf(in.readFloat()));
        mapSnapshot.put("PacketsBundled", Base.makeLong(ExternalizableHelper.readLong(in)));
        mapSnapshot.put("PacketsReceived", Base.makeLong(ExternalizableHelper.readLong(in)));
        mapSnapshot.put("PacketsRepeated", Base.makeLong(ExternalizableHelper.readLong(in)));
        mapSnapshot.put("PacketsResent", Base.makeLong(ExternalizableHelper.readLong(in)));
        mapSnapshot.put("PacketsResentEarly", Base.makeLong(ExternalizableHelper.readLong(in)));
        mapSnapshot.put("PacketsResentExcess", Base.makeLong(ExternalizableHelper.readLong(in)));
        mapSnapshot.put("PacketsSent", Base.makeLong(ExternalizableHelper.readLong(in)));
        mapSnapshot.put("Priority", Base.makeInteger(ExternalizableHelper.readInt(in)));
        mapSnapshot.put("ProcessName", ExternalizableHelper.readSafeUTF(in));
        mapSnapshot.put("ProductEdition", ExternalizableHelper.readSafeUTF(in));
        mapSnapshot.put("PublisherPacketUtilization", Float.valueOf(in.readFloat()));
        mapSnapshot.put("PublisherSuccessRate", Float.valueOf(in.readFloat()));
        mapSnapshot.put("QuorumStatus", ExternalizableHelper.readSafeUTF(in));
        mapSnapshot.put("RackName", ExternalizableHelper.readSafeUTF(in));
        mapSnapshot.put("ReceiverPacketUtilization", Float.valueOf(in.readFloat()));
        mapSnapshot.put("ReceiverSuccessRate", Float.valueOf(in.readFloat()));
        mapSnapshot.put("ResendDelay", Base.makeInteger(ExternalizableHelper.readInt(in)));
        mapSnapshot.put("RoleName", ExternalizableHelper.readSafeUTF(in));
        mapSnapshot.put("SendAckDelay", Base.makeInteger(ExternalizableHelper.readInt(in)));
        mapSnapshot.put("SendQueueSize", Base.makeInteger(ExternalizableHelper.readInt(in)));
        mapSnapshot.put("SiteName", ExternalizableHelper.readSafeUTF(in));
        mapSnapshot.put("SocketCount", Base.makeInteger(ExternalizableHelper.readInt(in)));
        mapSnapshot.put("Statistics", ExternalizableHelper.readSafeUTF(in));
        mapSnapshot.put("TcpRingFailures", Base.makeLong(ExternalizableHelper.readLong(in)));
        mapSnapshot.put("Timestamp", new Date(ExternalizableHelper.readLong(in)));
        mapSnapshot.put("TrafficJamCount", Base.makeInteger(ExternalizableHelper.readInt(in)));
        mapSnapshot.put("TrafficJamDelay", Base.makeInteger(ExternalizableHelper.readInt(in)));
        mapSnapshot.put("UID", ExternalizableHelper.readSafeUTF(in));
        mapSnapshot.put("UnicastAddress", ExternalizableHelper.readSafeUTF(in));
        mapSnapshot.put("UnicastPort", Base.makeInteger(ExternalizableHelper.readInt(in)));
        mapSnapshot.put("WellKnownAddresses", ExternalizableHelper.readStringArray(in));
        mapSnapshot.put("WeakestChannel", Base.makeInteger(ExternalizableHelper.readInt(in)));
        
        // sender with older verion does not include the following fields
        if (ExternalizableHelper.isVersionCompatible(in, 12, 2, 1, 4, 0))
            {
            mapSnapshot.put("TransportReceivedBytes", Long.valueOf(ExternalizableHelper.readLong(in)));
            mapSnapshot.put("TransportReceivedMessages", Long.valueOf(ExternalizableHelper.readLong(in)));
            mapSnapshot.put("TransportRetainedBytes", Long.valueOf(ExternalizableHelper.readLong(in)));
            mapSnapshot.put("TransportSentBytes", Long.valueOf(ExternalizableHelper.readLong(in)));
            mapSnapshot.put("TransportSentMessages", Long.valueOf(ExternalizableHelper.readLong(in)));
            mapSnapshot.put("TransportStatus", ExternalizableHelper.readSafeUTF(in));
            mapSnapshot.put("TransportBacklogDelay", Long.valueOf(ExternalizableHelper.readLong(in)));
            }
        // sender with older verion does not include the following fields
        if (ExternalizableHelper.isVersionCompatible(in, 14, 1, 1, 0, 0))
            {
            mapSnapshot.put("TracingSamplingRatio", Float.valueOf(ExternalizableHelper.readBigDecimal(in).floatValue()));
            }
        }
    
    /**
     * Return the environment information for this node. This includes details
    * of the JVM as well as system properties.
     */
    public String reportEnvironment()
        {
        // import java.util.Iterator;
        // import java.util.Properties;
        // import java.util.Set;
        // import java.util.SortedSet;
        // import java.util.TreeSet;
        
        checkReadOnly("logEnvironment");
        
        String sJavaVersion   = System.getProperty("java.version");
        String sVendorName    = System.getProperty("java.vendor");
        String sVendorVersion = System.getProperty("java.vendor.version");
        String sVmName        = System.getProperty("java.vm.name");
        String sVmVendor      = System.getProperty("java.vm.vendor");
        String sVmVersion     = System.getProperty("java.vm.version");
        String sRtName        = System.getProperty("java.runtime.name");
        String sRtVersion     = System.getProperty("java.runtime.version");
        
        StringBuilder sb = new StringBuilder();
        
        sb.append("Java Version: " + sJavaVersion).append('\n')
        
          .append("Java Vendor:").append('\n')
          .append(" - Name: " + sVendorName).append('\n')
          .append(" - Version: " + sVendorVersion).append('\n')
        
          .append("Java Virtual Machine:").append('\n')
          .append(" - Name: " + sVmName).append('\n')
          .append(" - Vendor: " + sVmVendor).append('\n')
          .append(" - Version: " + sVmVersion).append('\n')
        
          .append("Java Runtime Environment:").append('\n')
          .append(" - Name: " + sRtName).append('\n')
          .append(" - Version: " + sRtVersion).append('\n');
            
        Properties properties = System.getProperties();
        Set keys = properties.stringPropertyNames();
        SortedSet sortedKeys = new TreeSet(keys);
        Iterator iterator = sortedKeys.iterator();
        
        sb.append('\n')
          .append(sortedKeys.size() + " System Properties:\n");
          
        while (iterator.hasNext()) {
          String sKey = (String) iterator.next();
          sb.append(sKey + " : " + System.getProperty(sKey) + "\n");
        }
        
        return sb.toString();
        }
    
    /**
     * Return a full thread dump and outstanding polls for the services running
    * on the node.
     */
    public String reportNodeState()
        {
        checkReadOnly("reportNodeState");
        
        return createNodeState();
        }
    
    public void resetStatistics()
        {
        // import Component.Util.SafeCluster;
        
        SafeCluster clusterSafe = get_Cluster();
        if (clusterSafe != null && clusterSafe.isRunning())
            {
            clusterSafe.getCluster().resetStats();
            }
        }
    
    // Accessor for the property "_Cluster"
    /**
     * Setter for property _Cluster.<p>
    * The Cluster object associated with this model.
     */
    public void set_Cluster(com.tangosol.coherence.component.util.SafeCluster cluster)
        {
        // import java.lang.ref.WeakReference;
        
        set_ClusterRef(new WeakReference(cluster));
        }
    
    // Accessor for the property "_ClusterRef"
    /**
     * Setter for property _ClusterRef.<p>
    * The Cluster object associated with this model, wrapped into WeakReference
    * to avoid resource leakage.
     */
    protected void set_ClusterRef(java.lang.ref.WeakReference refCluster)
        {
        __m__ClusterRef = refCluster;
        }
    
    // Accessor for the property "_Member"
    /**
     * Setter for property _Member.<p>
    * The Member object associated with this model.
     */
    public void set_Member(com.tangosol.net.Member member)
        {
        // import java.lang.ref.WeakReference;
        
        set_MemberRef(new WeakReference(member));
        }
    
    // Accessor for the property "_MemberRef"
    /**
     * Setter for property _MemberRef.<p>
    * The Member object associated with this model, wrapped into WeakReference
    * to avoid resource leakage.
     */
    protected void set_MemberRef(java.lang.ref.WeakReference refMember)
        {
        __m__MemberRef = refMember;
        }
    
    // Accessor for the property "BufferPublishSize"
    /**
     * Setter for property BufferPublishSize.<p>
     */
    public void setBufferPublishSize(int cPackets)
        {
        // import Component.Util.Daemon.QueueProcessor.PacketProcessor.PacketPublisher as com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketPublisher;
        // import Component.Net.Socket.UdpSocket;
        // import com.tangosol.util.Base;
        
        checkReadOnly("setBufferPublishSize");
        
        com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketPublisher publisher = get_Publisher();
        if (publisher != null && cPackets != getBufferPublishSize())
            {
            checkRange("BufferPublishSize", cPackets, 1, Integer.MAX_VALUE);
        
            try
                {
                UdpSocket socket = publisher.getUdpSocketUnicast();
                synchronized (socket.getLock())
                    {
                    socket.close();
                    socket.setBufferSentBytes(cPackets * publisher.getPreferredPacketLength());
                    socket.open();
                    }
                }
            catch (Exception e)
                {
                _trace("Buffer resizing failed; stopping cluster service", 1);
                get_Cluster().stop();
                throw Base.ensureRuntimeException(e);
                }
            }
        }
    
    // Accessor for the property "BufferReceiveSize"
    /**
     * Setter for property BufferReceiveSize.<p>
     */
    public void setBufferReceiveSize(int cPackets)
        {
        // import Component.Util.Daemon.QueueProcessor.PacketProcessor.PacketListener as com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketListener;
        // import Component.Net.Socket.UdpSocket;
        // import com.tangosol.util.Base;
        
        checkReadOnly("setBufferReceiveSize");
        
        com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketListener listener = get_ListenerPreferred();
        if (listener != null && cPackets != getBufferReceiveSize())
            {
            checkRange("BufferReceiveSize", cPackets, 1, Integer.MAX_VALUE);
        
            try
                {
                UdpSocket socket = listener.getUdpSocket();
                synchronized (socket.getLock())
                    {
                    socket.close();
                    socket.setBufferReceivedBytes(cPackets * listener.getPacketLength());
                    socket.open();
                    }
                }
            catch (Exception e)
                {
                _trace("Buffer resizing failed; stopping cluster service", 1);
                get_Cluster().stop();
                throw Base.ensureRuntimeException(e);
                }
            }
        }
    
    // Accessor for the property "LoggingFormat"
    /**
     * Setter for property LoggingFormat.<p>
     */
    public void setLoggingFormat(String sFormat)
        {
        // import Component.Application.Console.Coherence;
        // import Component.Util.Daemon.QueueProcessor.Logger;
        // import com.tangosol.util.Base;
        
        Coherence singleton = (Coherence) Coherence.get_Instance();
        Logger    logger    = singleton.getLogger();
        
        if (logger != null && !Base.equals(sFormat, getLoggingFormat()))
            {
            checkReadOnly("setLoggerFormat");
            logger.setFormat(sFormat);
            }
        }
    
    // Accessor for the property "LoggingLevel"
    /**
     * Setter for property LoggingLevel.<p>
     */
    public void setLoggingLevel(int nLevel)
        {
        // import Component.Application.Console.Coherence;
        // import Component.Util.Daemon.QueueProcessor.Logger;
        
        Coherence singleton = (Coherence) Coherence.get_Instance();
        Logger    logger    = singleton.getLogger();
        
        if (logger != null && nLevel != getLoggingLevel())
            {
            checkRange("setLoggerLevel", nLevel, -1, Integer.MAX_VALUE);
            logger.setLevel(nLevel);
            }
        }
    
    // Accessor for the property "LoggingLimit"
    /**
     * Setter for property LoggingLimit.<p>
     */
    public void setLoggingLimit(int cChars)
        {
        // import Component.Application.Console.Coherence;
        // import Component.Util.Daemon.QueueProcessor.Logger;
        
        Coherence singleton = (Coherence) Coherence.get_Instance();
        Logger    logger    = singleton.getLogger();
        
        if (logger != null && cChars != getLoggingLimit())
            {
            checkRange("setLoggerLimit", cChars, 0, Integer.MAX_VALUE);
            logger.setLimit(cChars);
            }
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
        // import Component.Util.Daemon.QueueProcessor.PacketProcessor.PacketPublisher as com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketPublisher;
        
        checkReadOnly("setMulticastThreshold");
        
        com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketPublisher publisher = get_Publisher();
        if (publisher != null && nThreshold != getMulticastThreshold())
            {
            checkRange("setMulticastThreshold", nThreshold, 0, 100);
            publisher.setMulticastThreshold(0.01 * nThreshold);
            }
        }
    
    // Accessor for the property "ResendDelay"
    /**
     * Setter for property ResendDelay.<p>
     */
    public void setResendDelay(int cMillis)
        {
        // import Component.Util.Daemon.QueueProcessor.PacketProcessor.PacketPublisher as com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketPublisher;
        
        checkReadOnly("setResendDelay");
        
        com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketPublisher publisher = get_Publisher();
        if (publisher != null && cMillis != getResendDelay())
            {
            checkRange("setResendDelay", cMillis, 10, 1000);
            publisher.setResendDelay(cMillis);
            }
        }
    
    // Accessor for the property "SendAckDelay"
    /**
     * Setter for property SendAckDelay.<p>
     */
    public void setSendAckDelay(int cMillis)
        {
        // import Component.Util.Daemon.QueueProcessor.PacketProcessor.PacketPublisher as com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketPublisher;
        
        checkReadOnly("setSendAckDelay");
        
        com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketPublisher publisher = get_Publisher();
        if (publisher != null && cMillis != getSendAckDelay())
            {
            checkRange("setSendAckDelay", cMillis, 1, 100);
            publisher.setAckDelay(cMillis);
            }
        }
    
    // Accessor for the property "TracingSamplingRatio"
    /**
     * Setter for property TracingSamplingRatio.<p>
    * The ratio of spans to trace when tracing is enabled. 
     */
    public void setTracingSamplingRatio(float nRatio)
        {
        // import Component.Util.SafeCluster;
        // import com.tangosol.internal.tracing.TracingShim$Control as com.tangosol.internal.tracing.TracingShim.Control;
        // import com.tangosol.internal.tracing.TracingShim$Dependencies as com.tangosol.internal.tracing.TracingShim.Dependencies;
        // import com.tangosol.internal.tracing.TracingShim$DefaultDependencies as com.tangosol.internal.tracing.TracingShim.DefaultDependencies;
        
        SafeCluster clusterSafe = get_Cluster();
        if (clusterSafe != null && clusterSafe.isRunning())
            {
            com.tangosol.internal.tracing.TracingShim.Control      control = clusterSafe.getCluster().getTracingControl();
            com.tangosol.internal.tracing.TracingShim.Dependencies deps    = new com.tangosol.internal.tracing.TracingShim.DefaultDependencies(control == null ? null : control.getDependencies())
                                        .setSamplingRatio(nRatio);
            if (!clusterSafe.getCluster().configureTracing(deps))
                {
                throw new UnsupportedOperationException(
                    "initialization failed; tracing is already enabled by an external source");
                }
            }
        }
    
    // Accessor for the property "TrafficJamCount"
    /**
     * Setter for property TrafficJamCount.<p>
     */
    public void setTrafficJamCount(int cPackets)
        {
        // import Component.Util.Daemon.QueueProcessor.PacketProcessor.PacketPublisher as com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketPublisher;
        
        checkReadOnly("setTrafficJamCount");
        
        com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketPublisher publisher = get_Publisher();
        if (publisher != null && cPackets != getTrafficJamCount())
            {
            checkReadOnly("setTrafficJamCount");
            publisher.setCloggedCount(cPackets);
            }
        }
    
    // Accessor for the property "TrafficJamDelay"
    /**
     * Setter for property TrafficJamDelay.<p>
     */
    public void setTrafficJamDelay(int cMillis)
        {
        // import Component.Util.Daemon.QueueProcessor.PacketProcessor.PacketPublisher as com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketPublisher;
        
        checkReadOnly("setTrafficJamDelay");
        
        com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketPublisher publisher = get_Publisher();
        if (publisher != null && cMillis != getTrafficJamDelay())
            {
            checkReadOnly("setTrafficJamDelay");
            publisher.setCloggedDelay(cMillis);
            }
        }
    
    /**
     * Show detailed information on the reliable transport.
     */
    public String showTransportStatus()
        {
        return getTransportStatus(true);
        }
    
    public void shutdown()
        {
        // import Component.Util.SafeCluster;
        
        checkReadOnly("shutdown");
        SafeCluster clusterSafe = get_Cluster();
        if (clusterSafe != null)
            {
            clusterSafe.shutdown();
            }
        }
    
    // Declared at the super level
    /**
     * Must be supplemented at each specific Model implementation.
     */
    public void writeExternal(java.io.DataOutput out)
            throws java.io.IOException
        {
        // import com.tangosol.util.ExternalizableHelper;
        // import java.math.BigDecimal;
        
        super.writeExternal(out);
        
        ExternalizableHelper.writeInt(out, getBufferPublishSize());
        ExternalizableHelper.writeInt(out, getBufferReceiveSize());
        ExternalizableHelper.writeInt(out, getCpuCount());
        out.writeBoolean(isFlowControlEnabled());
        ExternalizableHelper.writeInt(out, getGuardRecoverCount());
        ExternalizableHelper.writeInt(out, getGuardTerminateCount());
        ExternalizableHelper.writeInt(out, getId());
        ExternalizableHelper.writeSafeUTF(out, getLoggingDestination());
        ExternalizableHelper.writeSafeUTF(out, getLoggingFormat());
        ExternalizableHelper.writeInt(out, getLoggingLevel());
        ExternalizableHelper.writeInt(out, getLoggingLimit());
        ExternalizableHelper.writeInt(out, getMachineId());
        ExternalizableHelper.writeSafeUTF(out, getMachineName());
        ExternalizableHelper.writeSafeUTF(out, getMemberName());
        ExternalizableHelper.writeInt(out, getMemoryAvailableMB());
        ExternalizableHelper.writeInt(out, getMemoryMaxMB());
        ExternalizableHelper.writeSafeUTF(out, getMulticastAddress());
        out.writeBoolean(isMulticastEnabled());
        ExternalizableHelper.writeInt(out, getMulticastPort());
        ExternalizableHelper.writeInt(out, getMulticastThreshold());
        ExternalizableHelper.writeInt(out, getMulticastTTL());
        out.writeBoolean(isNackEnabled());
        ExternalizableHelper.writeLong(out, getNackSent());
        out.writeFloat(getPacketDeliveryEfficiency());
        ExternalizableHelper.writeLong(out, getPacketsBundled());
        ExternalizableHelper.writeLong(out, getPacketsReceived());
        ExternalizableHelper.writeLong(out, getPacketsRepeated());
        ExternalizableHelper.writeLong(out, getPacketsResent());
        ExternalizableHelper.writeLong(out, getPacketsResentEarly());
        ExternalizableHelper.writeLong(out, getPacketsResentExcess());
        ExternalizableHelper.writeLong(out, getPacketsSent());
        ExternalizableHelper.writeInt(out, getPriority());
        ExternalizableHelper.writeSafeUTF(out, getProcessName());
        ExternalizableHelper.writeSafeUTF(out, getProductEdition());
        out.writeFloat(getPublisherPacketUtilization());
        out.writeFloat(getPublisherSuccessRate());
        ExternalizableHelper.writeSafeUTF(out, getQuorumStatus());
        ExternalizableHelper.writeSafeUTF(out, getRackName());
        out.writeFloat(getReceiverPacketUtilization());
        out.writeFloat(getReceiverSuccessRate());
        ExternalizableHelper.writeInt(out, getResendDelay());
        ExternalizableHelper.writeSafeUTF(out, getRoleName());
        ExternalizableHelper.writeInt(out, getSendAckDelay());
        ExternalizableHelper.writeInt(out, getSendQueueSize());
        ExternalizableHelper.writeSafeUTF(out, getSiteName());
        ExternalizableHelper.writeInt(out, getSocketCount());
        ExternalizableHelper.writeSafeUTF(out, getStatistics());
        ExternalizableHelper.writeLong(out, getTcpRingFailures());
        ExternalizableHelper.writeLong(out, getTimestamp().getTime());
        ExternalizableHelper.writeInt(out, getTrafficJamCount());
        ExternalizableHelper.writeInt(out, getTrafficJamDelay());
        ExternalizableHelper.writeSafeUTF(out, getUID());
        ExternalizableHelper.writeSafeUTF(out, getUnicastAddress());
        ExternalizableHelper.writeInt(out, getUnicastPort());
        ExternalizableHelper.writeStringArray(out, getWellKnownAddresses());
        ExternalizableHelper.writeInt(out, getWeakestChannel());
        if (ExternalizableHelper.isVersionCompatible(out, 12, 2, 1, 4, 0))
            {
            ExternalizableHelper.writeLong(out, getTransportReceivedBytes());
            ExternalizableHelper.writeLong(out, getTransportReceivedMessages());
            ExternalizableHelper.writeLong(out, getTransportRetainedBytes());
            ExternalizableHelper.writeLong(out, getTransportSentBytes());
            ExternalizableHelper.writeLong(out, getTransportSentMessages());
            ExternalizableHelper.writeSafeUTF(out, getTransportStatus());
            ExternalizableHelper.writeLong(out, getTransportBacklogDelay());
            }
        if (ExternalizableHelper.isVersionCompatible(out, 14, 1, 1, 0, 0))
            {
            ExternalizableHelper.writeBigDecimal(out, BigDecimal.valueOf(getTracingSamplingRatio()));
            }
        }
    }
