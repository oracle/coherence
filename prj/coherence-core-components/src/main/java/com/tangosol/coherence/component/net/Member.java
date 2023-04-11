
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.Member

package com.tangosol.coherence.component.net;

import com.tangosol.coherence.component.net.packet.MessagePacket;
import com.tangosol.coherence.component.util.WindowedArray;
import com.oracle.coherence.common.net.InetAddresses;
import com.tangosol.coherence.config.Config;
import com.tangosol.net.InetAddressHelper;
import com.tangosol.net.internal.PacketComparator;
import com.tangosol.util.Base;
import com.tangosol.util.UID;
import com.tangosol.util.UUID;
import com.tangosol.util.WrapperException;
import java.io.DataInput;
import java.io.DataOutput;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.sql.Timestamp;
import java.util.ConcurrentModificationException;
import java.util.Iterator;

/**
 * Member component represents a cluster member. Prior to Coherence 3.0 it was
 * represented by two components:
 * Net.Member and Net.Member.ClusterMember.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class Member
        extends    com.tangosol.coherence.component.Net
        implements com.tangosol.io.ExternalizableLite,
                   com.tangosol.io.pof.PortableObject,
                   com.tangosol.net.Member,
                   java.io.Externalizable
    {
    // ---- Fields declarations ----
    
    /**
     * Property Address
     *
     * The IP address of the member's DatagramSocket for point-to-point
     * communication. This is part of the unique identifier of the Member
     * (Timestamp, Address, Port, MachineId).
     */
    private transient java.net.InetAddress __m_Address;
    
    /**
     * Property ByteMask
     *
     * A 32-bit integer value between 1 and 128 inclusive, with only one bit
     * set. When bitanded with the byte value from ByteOffset in a byte array
     * holding member indicators, the value will = 0 (member not specified) or
     * != 0 (member specified).
     * 
     * (This is actually an "int mask", not a "byte mask".)
     */
    private transient int __m_ByteMask;
    
    /**
     * Property ByteOffset
     *
     * An offset into a byte array holding member indicators, used in
     * conjunction with the ByteMask property.
     * 
     * (This is actually an "int index", not a "byte offset".)
     */
    private transient int __m_ByteOffset;
    
    /**
     * Property ClusterName
     *
     * A configured name that should be the same for all Members that should be
     * able to join the same cluster. Like the Mode property, this is used only
     * to verify that the join can be successful.
     */
    private String __m_ClusterName;
    
    /**
     * Property ContiguousFromPacketId
     *
     * The packet id of last contiguous packet received by us from this Member.
     *  This id will be transmitted back to this Member in Acks, and will be
     * stored as ContiguousToPacketId.  It will be used by the remote member to
     * detect and fill in wholes caused by missing Acks. 
     * 
     * @volatile
     */
    private volatile transient com.tangosol.net.internal.PacketIdentifier __m_ContiguousFromPacketId;
    
    /**
     * Property ContiguousToPacketId
     *
     * As reported by the remote member, this is the id of the last packet in
     * the contigous message stream to have been received without gaps from us.
     *  Any packet ids before this one, have therefore been received.
     * 
     * This propery is maintained by the receiver, see onPacketAck.
     * @volatile
     */
    private volatile transient com.tangosol.net.internal.PacketIdentifier __m_ContiguousToPacketId;
    
    /**
     * Property CpuCount
     *
     * Number of CPU cores for the machine this Member is running on.
     */
    private int __m_CpuCount;
    
    /**
     * Property Dead
     *
     * Set to true when the Member is determined to be dead.
     */
    private transient boolean __m_Dead;
    
    /**
     * Property Deaf
     *
     * Set to true to indicates that the Member has failed to acknowledge at
     * least one directed packet within a timeout interval.
     * 
     * That could mean one of two things:
     * a) the member has indeed left the cluster and has to be removed;
     * b) the local member is the deaf one and has to be stopped
     * 
     * Note: A deaf member represents a danger to the cluster integrity since
     * it does not lisen to any advice from others, but can send (broadcast)
     * incoherent and confusing messages. A dumb (mute) node does not cause any
     * problems to the outside world.
     * 
     * Note: If the Member is marked as Dead, the Deaf flag indicates that it
     * became a "zombie".
     * 
     * @see Zombie
     */
    private transient boolean __m_Deaf;
    
    /**
     * Property Edition
     *
     * The Edition is the product type.
     * 
     * 0=Data Client (DC)
     * 1=Real-Time Client (RTC)
     * 2=Compute Client (CC)
     * 3=Standard Edition (SE)
     * 4=Enterprise Edition (EE)
     * 5=DataGrid Edition (GE)
     * 
     * To be in a cluster, a member must be running CC, SE, EE or GE. The valid
     * combinations are:
     * 
     * CC: can join a GE cluster (i.e. servers running GE and/or CC)
     * SE: can join a SE cluster
     * EE: can join an EE cluster
     * GE: can join a GE cluster (i.e. servers running GE and/or CC)
     */
    private int __m_Edition;
    
    /**
     * Property EDITION_NAME
     *
     */
    public static final String[] EDITION_NAME;
    
    /**
     * Property FlowControl
     *
     * The Member's flow control related properties.  This object also acts as
     * a synchronization point for all flow control operations.  This property
     * is null if flow control is disabled.
     */
    private transient Member.FlowControl __m_FlowControl;
    
    /**
     * Property Id
     *
     * A small number that uniquely identifies the Member at this point in time
     * and does not change for the life of this Member. This value does not
     * uniquely identify the Member throughout the duration of the cluster
     * because Members that existed but left the cluster before this Member
     * existed may have had the same mini-id value and the same goes for
     * Members that may join the cluster after this Member leaves the cluster.
     * 
     * (Sometimes referred to as a "MiniId" in comparison to the "Uid".)
     */
    private int __m_Id;
    
    /**
     * Property LastHeuristicDeathMillis
     *
     * The most recent datetime value that this Member failed to acknowledge at
     * least one directed packet within a heuristic timeout interval.  This
     * information is used in the witness protocol to help identify poorly
     * performing but still reachable members.
     * 
     * @since 12.2.1.4
     */
    private transient long __m_LastHeuristicDeathMillis;
    
    /**
     * Property LastIncomingMessageId
     *
     * The Id of the last Message (actually, unique Directed Packets) sent from
     * the Member to us. Approximate (may be in the wrong trint range);
     * maintained by PacketReceiver.
     */
    private transient long __m_LastIncomingMessageId;
    
    /**
     * Property LastIncomingMillis
     *
     * Datetime value that the last direct acknowlegement (AckPacket) from this
     * Member was received by the local Member. Maintained by the
     * PacketListener.
     * 
     * Note: Prior to Coherence 2.5.1 this property referred to ANY direct
     * incoming communication. The major reason for this change was realization
     * of the fact that a deaf, but still speaking member, is even more
     * dangerous than a completely dead (deaf and mute) one. Therefore, an
     * unsolicited communication (even a directed one) cannot serve as a proof
     * of a functioning communication chanel and cannot be taken into
     * consideration.
     */
    private transient long __m_LastIncomingMillis;
    
    /**
     * Property LastOutgoingMessageId
     *
     * The Id of the last message  (actually, unique Directed Packets sent to
     * this Member.
     */
    private transient long __m_LastOutgoingMessageId;
    
    /**
     * Property LastOutgoingMillis
     *
     * Datetime value that the last packet that requires a direct
     * acknowlegement was sent to this Member by the local Member. Maintained
     * by the PacketPublisher.
     * 
     * Note: Prior to Coherence 2.5.1 this property referred to ANY direct
     * outgoing communication. The major reason for this change was realization
     * of the fact that a deaf, but still speaking member, is even more
     * dangerous than a completely dead (deaf and mute) one. Therefore, an
     * unsolicited communication (even a directed one) cannot serve as a proof
     * of a functioning communication chanel and cannot be taken into
     * consideration.
     */
    private transient long __m_LastOutgoingMillis;
    
    /**
     * Property LastSlowMillis
     *
     * The most recent datetime value that this Member failed to acknowledge at
     * least one directed packet within a 3/4 of the packet timeout interval.
     * This information allows us to see how many Members are likely to leave
     * the cluster in the near future (within a timeout interval). If a local
     * Member sees not more than one "slow" Member, it's quite more likely that
     * there is something wrong with that member than with the local one.
     * Alternatively, presence of many "slow" Members may point to this Member
     * being a culprit. Maintained by the PacketPublisher.
     * 
     * @since Coherence 3.2; added to improve handling for a "partial delivery
     * failure".
     */
    private transient long __m_LastSlowMillis;
    
    /**
     * Property LastTimeoutMillis
     *
     * The most recent datetime value that this Member timed-out (exceeded its
     * configured timeout; either packet-timeout or ip-timeout).
     * 
     * @since Coherence 3.6
     */
    private transient long __m_LastTimeoutMillis;
    
    /**
     * Property Leaving
     *
     * Set to true when the Member is known to be leaving the cluster.
     * 
     * Overloaded to indicate if the notification to all members is in doubt.
     * Having "leaving" and "dead" both true means that the local node is
     * unsure if all nodes have been informed of the death of the member.
     */
    private transient boolean __m_Leaving;
    
    /**
     * Property MachineId
     *
     * An identifier that should be the same for Members that are on the same
     * physical machine, and different for Members that are on different
     * physical machines. This is part of the unique identifier of the Member
     * (Timestamp, Address, Port, MachineId).
     */
    private transient int __m_MachineId;
    
    /**
     * Property MachineName
     *
     * A configured name that should be the same for Members that are on the
     * same physical machine, and different for Members that are on different
     * physical machines.
     */
    private String __m_MachineName;
    
    /**
     * Property MemberName
     *
     * A configured name that must be unique for every Member.
     */
    private String __m_MemberName;
    
    /**
     * Property MessageIncoming
     *
     * [ambient] Used by the PacketReceiver to collect out-of-order Messages
     * and Message parts in case of partial delivery.
     */
    private transient com.tangosol.coherence.component.util.WindowedArray __m_MessageIncoming;
    
    /**
     * Property MessagePile
     *
     * [ambient] Sequel Message Packets from the Member that have not yet been
     * associated with a Message because the head (Directed) Packet has not
     * been received yet. Maintained by the PacketReceiver. Key is a long value
     * corresponding to the "from" Message id. Value is a SimpleLongArray of
     * Sequel Packets, or a Message object once the Directed packet is received
     * and until the Message is complete (i.e. delivered to a Service).
     */
    private transient com.tangosol.util.SparseArray __m_MessagePile;
    
    /**
     * Property Mode
     *
     * The Mode is the "license type", i.e. evaluation, development or
     * production use.
     * 
     * 0=evaluation
     * 1=development
     * 2=production
     * 
     * It is important that members of different modes are not mixed and
     * matched within a particular cluster, since it is likely an accident
     * (e.g. a developer accidentally connecting to a production cluster).
     */
    private int __m_Mode;
    
    /**
     * Property MODE_NAME
     *
     */
    public static final String[] MODE_NAME;
    
    /**
     * Property NewestFromPacketId
     *
     * The message id of newest packet known to be sent from this Member to us.
     * 
     * @volatile
     */
    private volatile transient com.tangosol.net.internal.PacketIdentifier __m_NewestFromPacketId;
    
    /**
     * Property NewestToPacketId
     *
     * The packet id of the newest ackable packet sent to this Member from us.
     * 
     * Note: this is not the last packet sent to this member, i.e. packet
     * resends will not update this value.
     * 
     * @volatile
     */
    private volatile transient com.tangosol.net.internal.PacketIdentifier __m_NewestToPacketId;
    
    /**
     * Property PacketAck
     *
     * [ambient] An Ack Packet queued to be sent to the Member. The Packet is
     * created by the PacketReceiver and stored in this property, and the
     * PacketPublisher clears this property when the Packet is removed from the
     * send Queue.
     * 
     * @volatile the PacketPublisher and PacketReceiver both operate on this
     * value outside of synchronization.
     */
    private volatile transient com.tangosol.coherence.component.net.packet.notifyPacket.Ack __m_PacketAck;
    
    /**
     * Property Port
     *
     * The port of the member's DatagramSocket for point-to-point
     * communication. This is part of the unique identifier of the Member
     * (Timestamp, Address, Port, MachineId).
     */
    private transient int __m_Port;
    
    /**
     * Property PreferredAckSize
     *
     * The AckSize (as a number of acknowledged packets) which this node
     * prefers to receive.
     */
    private int __m_PreferredAckSize;
    
    /**
     * Property PreferredPacketLength
     *
     * The Member's preferred packet length.  When sending packets to this
     * member it is preferred that they not exceede the minimum of this value
     * and the sending member's value.
     * 
     * This value is updated when handshaking with the member during the
     * ClusterService welcome messages.
     * 
     * @since Coherence 3.6.1
     */
    private int __m_PreferredPacketLength;
    
    /**
     * Property PreferredPort
     *
     * The port of the member's DatagramSocket which is ideal for
     * point-to-point communication of packets at or under the preferred packet
     * length.
     * 
     * This value is updated when handshaking with the member during the
     * ClusterService welcome messages.
     * 
     * @since Coherence 3.6.1
     * @since Coherence 12.2.1 left unset if the member runs the
     * TransportService
     */
    private transient int __m_PreferredPort;
    
    /**
     * Property PreferredSocketAddress
     *
     * Lazily calculated InetSocketAddress of the member's preferred
     * DatagramSocket for point-to-point communication.
     * 
     * This value is lazily computed, and will return the SocketAddress in the
     * case that the PreferredPort has not been set.
     * 
     * @since Coherence 3.6.1
     */
    private transient java.net.InetSocketAddress __m_PreferredSocketAddress;
    
    /**
     * Property Priority
     *
     * The priority or "weight" of the member; used to determine tie-breakers.
     */
    private int __m_Priority;
    
    /**
     * Property ProcessName
     *
     * A configured name that should be the same for Members that are in the
     * same process (JVM), and different for Members that are in different
     * processes.
     */
    private String __m_ProcessName;
    
    /**
     * Property RackName
     *
     * A configured name that should be the same for Members that are on the
     * same physical "rack" (or frame or cage), and different for Members that
     * are on different physical "racks".
     */
    private String __m_RackName;
    
    /**
     * Property RecentPacketQueue
     *
     * A queue tracking the order in which packets were sent to this member. 
     * The queue only contains confirmationRequired packets. As packets are
     * confirmed or lost they are removed from the queue; once resent a packet
     * is moved to the tail of the queue.  This is used as part of the early
     * packet loss detection (see Receiver.doEarlyLossDetection). At any point
     * the queue may contain both ack'd and unack'd packets.  The tail is
     * handled by the publisher, while the head is handled by the receiver.
     */
    private transient Member.SentQueue __m_RecentPacketQueue;
    
    /**
     * Property RoleName
     *
     * A configured name that can be used to indicate the role of a member to
     * the application. While managed by Coherence, this property is used only
     * by the application.
     */
    private String __m_RoleName;
    
    /**
     * Property SHOW_ALL
     *
     * Show all available information.
     */
    public static final int SHOW_ALL = -1;
    
    /**
     * Property SHOW_LICENSE
     *
     * Show the license related information.
     */
    public static final int SHOW_LICENSE = 1;
    
    /**
     * Property SHOW_STATS
     *
     * Show the member's network stats.
     */
    public static final int SHOW_STATS = 2;
    
    /**
     * Property SHOW_STD
     *
     * Show the minimal (standard) amount of information.
     */
    public static final int SHOW_STD = 0;
    
    /**
     * Property SiteName
     *
     * A configured name that should be the same for Members that are on the
     * same physical site (e.g. data center), and different for Members that
     * are on different physical sites.
     */
    private String __m_SiteName;
    
    /**
     * Property SocketAddress
     *
     * Lazily calculated InetSocketAddress of the member's DatagramSocket for
     * point-to-point communication.
     */
    private transient java.net.InetSocketAddress __m_SocketAddress;
    
    /**
     * Property SocketCount
     *
     * Number of CPU sockets for the machine this Member is running on.
     */
    private int __m_SocketCount;
    
    /**
     * Property StatsReceived
     *
     * Statistics: total number of received packets.
     */
    private transient long __m_StatsReceived;
    
    /**
     * Property StatsRepeated
     *
     * Statistics: number of repeated packets (recieved more then once)
     */
    private transient long __m_StatsRepeated;
    
    /**
     * Property StatsResent
     *
     * Statistics: total number of re-sent packets.
     */
    private transient long __m_StatsResent;
    
    /**
     * Property StatsReset
     *
     * Statistics: Date/time value that the stats have been reset.
     */
    private transient long __m_StatsReset;
    
    /**
     * Property StatsSent
     *
     * Statistics: total number of sent packets.
     */
    private transient long __m_StatsSent;
    
    /**
     * Property TcpRingPort
     *
     * The port on which this member is listening for TcpRing connections. 
     * Note that when the member is using a configured port this will match the
     * Port property.
     * 
     * @since 12.2.1
     */
    private int __m_TcpRingPort;
    
    /**
     * Property Timestamp
     *
     * Date/time value that the Member was born. This is part of the unique
     * identifier of the Member (Timestamp, Address, Port, MachineId).
     * 
     * Note: If the Member is marked as Dead, the Timestamp value indicates the
     * departure time (local clock).  In the case of zombies, the timestamp is
     * periodically updated after death as part of the panic protocol.
     * 
     * @see declareZombie
     */
    private transient long __m_Timestamp;
    
    /**
     * Property TxDebugDropCount
     *
     * The number of times to artificially drop each packet sent to this member
     * before actually sending them.  Used for debugging only.
     */
    private int __m_TxDebugDropCount;
    
    /**
     * Property Uid32
     *
     * The unique identifier of the Member (Timestamp, Address, Port,
     * MachineId) packed into a UID. This is a new 32 byte UID that replaced
     * the old 16 bytes one.
     */
    private com.tangosol.util.UUID __m_Uid32;
    private static com.tangosol.util.ListMap __mapChildren;
    
    // Static initializer
    static
        {
        try
            {
            String[] a0 = new String[6];
                {
                a0[0] = "Data Client";
                a0[1] = "Real-Time Client";
                a0[2] = "Standard Edition";
                a0[3] = "Community Edition";
                a0[4] = "Enterprise Edition";
                a0[5] = "Grid Edition";
                }
            EDITION_NAME = a0;
            String[] a1 = new String[3];
                {
                a1[0] = "Evaluation";
                a1[1] = "Development";
                a1[2] = "Production";
                }
            MODE_NAME = a1;
            }
        catch (java.lang.Exception e)
            {
            // re-throw as a runtime exception
            throw new com.tangosol.util.WrapperException(e);
            }
        __initStatic();
        }
    
    // Default static initializer
    private static void __initStatic()
        {
        // register child classes
        __mapChildren = new com.tangosol.util.ListMap();
        __mapChildren.put("FlowControl", Member.FlowControl.get_CLASS());
        __mapChildren.put("SentQueue", Member.SentQueue.get_CLASS());
        }
    
    // Default constructor
    public Member()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public Member(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            setMessagePile(new com.tangosol.util.SparseArray());
            setPreferredAckSize(65535);
            setTxDebugDropCount(0);
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
        return new com.tangosol.coherence.component.net.Member();
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
            clz = Class.forName("com.tangosol.coherence/component/net/Member".replace('/', '.'));
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
    
    public static int calcByteMask(int nId)
        {
        return 1 << ((nId - 1) % 32);
        }
    
    public static int calcByteOffset(int nId)
        {
        return (nId - 1) / 32;
        }
    
    // Declared at the super level
    /**
     * Compares this object with the specified object for order.  Returns a
    * negative integer, zero, or a positive integer as this object is less
    * than, equal to, or greater than the specified object.
    * 
    * @param o  the Object to be compared.
    * @return  a negative integer, zero, or a positive integer as this object
    * is less than, equal to, or greater than the specified object.
    * 
    * @throws ClassCastException if the specified object's type prevents it
    * from being compared to this Object.
     */
    public int compareTo(Object o)
        {
        // import com.tangosol.util.UUID;
        
        UUID uidThis = getUid32();
        UUID uidThat = ((Member) o).getUid32();
        
        return uidThis.compareTo(uidThat);
        }
    
    /**
     * Used to configure an announce member
     */
    public void configure(com.tangosol.net.ClusterDependencies deps, java.net.InetAddress addr, int nPort, int nPortTcpRing, int[] an)
        {
        configure(deps.getMemberIdentity(), addr);
        
        setPort(nPort);
        setTcpRingPort(nPortTcpRing);
        setMachineId(an[2]);
        setEdition(deps.getEdition());
        setMode(deps.getMode());
        setCpuCount(an[0]);
        setSocketCount(an[1]);
        }
    
    /**
     * Used to configure an announce member
     */
    public void configure(com.tangosol.net.MemberIdentity identity, java.net.InetAddress addr)
        {
        // import com.tangosol.util.Base;
        // import com.oracle.coherence.common.net.InetAddresses;
        // import java.net.UnknownHostException;
        
        try
            {
            setAddress(addr == null ? InetAddresses.getLocalHost() : addr);
            }
        catch (UnknownHostException e)
            {
            // ignore
            }
        
        setTimestamp(Base.getSafeTimeMillis());
        setClusterName(identity.getClusterName());
        setSiteName(identity.getSiteName());
        setRackName(identity.getRackName());
        setMachineName(identity.getMachineName());
        setProcessName(identity.getProcessName());
        setMemberName(identity.getMemberName());
        setRoleName(identity.getRoleName());
        setPriority(identity.getPriority());
        }
    
    protected void configure(com.tangosol.util.UUID uid)
        {
        // import com.tangosol.net.InetAddressHelper;
        // import com.tangosol.util.WrapperException;
        // import java.net.InetAddress;
        // import java.net.UnknownHostException;
        
        _assert(uid != null);
        
        long   ldt        = uid.getTimestamp();
        byte[] abAddr     = uid.getAddress();
        int    nPort;
        int    nMachineId;
        
        // convert address value into an InetAddress
        InetAddress addr;
        if (getId() > 0 || uid.isAddressIncluded())
            {
            try
                {
                addr       = InetAddressHelper.getByAddress(abAddr);
                nPort      = uid.getPort();
                nMachineId = uid.getCount();
                }
            catch (UnknownHostException e)
                {
                throw new WrapperException(e);
                }
            }
        else // legal only for non-cluster members
            {
            addr       = InetAddressHelper.ADDR_ANY;
            nPort      = 0;
            nMachineId = 0;
            }
        
        // store member info
        setTimestamp(ldt);
        setAddress(addr);
        setPort(nPort);
        setMachineId(nMachineId);
        setUid32(uid);
        }
    
    public void configure(Member member, long ldtTimestamp)
        {
        _assert(getCpuCount() == 0 && getSocketCount() == 0
                && member.getCpuCount() > 0 && member.getSocketCount() > 0);
        
        setTimestamp(ldtTimestamp <= 0L ? member.getTimestamp() : ldtTimestamp);
        setAddress(member.getAddress());
        setPort(member.getPort());
        setTcpRingPort(member.getTcpRingPort());
        setMachineId(member.getMachineId());
        setEdition(member.getEdition());
        setMode(member.getMode());
        setCpuCount(member.getCpuCount());
        setSocketCount(member.getSocketCount());
        setClusterName(member.getClusterName());
        setSiteName(member.getSiteName());
        setRackName(member.getRackName());
        setMachineName(member.getMachineName());
        setProcessName(member.getProcessName());
        setMemberName(member.getMemberName());
        setRoleName(member.getRoleName());
        setPriority(member.getPriority());
        }
    
    /**
     * Configure a dead member with minimal information.
     */
    public void configureDead(int nID, com.tangosol.util.UUID uuid, long ldtDeath)
        {
        setId(nID);
        configure(uuid);
        setDead(true);
        setTimestamp(ldtDeath);
        }
    
    /**
     * Used to configure an temporary member for use in discovery replies
     */
    public void configureTemp(java.net.InetSocketAddress addr)
        {
        setAddress(addr.getAddress());
        setPort(addr.getPort());
        }
    
    /**
     * (Re)declare this member as a zombie.  A zombie is a dead member which
    * continues to emit a cluster heartbeat, indicating that it is unable to
    * hear the other senior, i.e. it's also deaf.  Each time the member is
    * redeclared as a zombie its timestamp is updated, indicating its inability
    * to die.
     */
    public void declareZombie()
        {
        // import com.tangosol.util.Base;
        
        _assert(isDead());
        
        setTimestamp(Base.getSafeTimeMillis());
        setDeaf(true);
        }
    
    // Declared at the super level
    public boolean equals(Object obj)
        {
        // import com.tangosol.util.Base;
        
        if (this == obj)
            {
            return true;
            }
        
        if (obj instanceof Member)
            {
            Member that = (Member) obj;
            return Base.equals(this.getUid32(), that.getUid32());
            }
        
        return false;
        }
    
    /**
     * Search the supplied member set and return the member which this member
    * has the most issues communicating with or null if everyone is perfect.
     */
    public static Member findWeakestMember(java.util.Set setMembers)
        {
        // import java.util.ConcurrentModificationException;
        // import java.util.Iterator;
        
        long   lRateWorst  = Long.MAX_VALUE;
        Member memberWorst = null;
        do
            {
            try
                {
                for (Iterator iter = setMembers.iterator(); iter.hasNext(); )
                    {
                    Member member = (Member) iter.next();
                    if (member != null)
                        {
                        long cErrors = member.getStatsResent() + member.getStatsRepeated();
                        if (cErrors > 0)
                            {
                            long cTotal = member.getStatsSent() + member.getStatsReceived();
                            long lRate  = cTotal / cErrors;
                            if (lRate < lRateWorst)
                                {
                                lRateWorst = lRate;
                                memberWorst = member;
                                }
                            }
                        }
                    }
                break;
                }
            catch (ConcurrentModificationException e)
                {
                // just doing stats, repeat
                continue;
                }
            }
        while (true);
        
        return memberWorst;
        }
    
    /**
     * Return point to point stats for communication from us to the Member.
     */
    public String formatStats()
        {
        // import com.tangosol.util.Base;
        
        Member.FlowControl flowControl = getFlowControl();
        long         ldtNow      = Base.getSafeTimeMillis();
        long         ldtIn       = getLastIncomingMillis();
        long         ldtOut      = getLastOutgoingMillis();
        long         ldtSlow     = getLastSlowMillis();
        long         ldtHDeath   = getLastHeuristicDeathMillis();
        double       dTxSuccess  = getStatsPublisherSuccessRate();
        double       dRxSuccess  = getStatsReceiverSuccessRate();
        
        // round rates
        dTxSuccess = ((int) (dTxSuccess * 10000)) / 10000D;
        dRxSuccess = ((int) (dRxSuccess * 10000)) / 10000D;
        
        return "PublisherSuccessRate=" + dTxSuccess +
               ", ReceiverSuccessRate="  + dRxSuccess +
               (flowControl == null ? "" : ", " + flowControl.formatStats(true)) +
               ", LastIn="             + (ldtIn     == 0L ? "n/a" : (ldtNow - ldtIn)     + "ms") +
               ", LastOut="            + (ldtOut    == 0L ? "n/a" : (ldtNow - ldtOut)    + "ms") +
               ", LastSlow="           + (ldtSlow   == 0L ? "n/a" : (ldtNow - ldtSlow)   + "ms") +
               ", LastHeuristicDeath=" + (ldtHDeath == 0L ? "n/a" : (ldtNow - ldtHDeath) + "ms");
        }
    
    // From interface: com.tangosol.net.Member
    // Accessor for the property "Address"
    /**
     * Getter for property Address.<p>
    * The IP address of the member's DatagramSocket for point-to-point
    * communication. This is part of the unique identifier of the Member
    * (Timestamp, Address, Port, MachineId).
     */
    public java.net.InetAddress getAddress()
        {
        return __m_Address;
        }
    
    // Accessor for the property "ByteMask"
    /**
     * Getter for property ByteMask.<p>
    * A 32-bit integer value between 1 and 128 inclusive, with only one bit
    * set. When bitanded with the byte value from ByteOffset in a byte array
    * holding member indicators, the value will = 0 (member not specified) or
    * != 0 (member specified).
    * 
    * (This is actually an "int mask", not a "byte mask".)
     */
    public int getByteMask()
        {
        return __m_ByteMask;
        }
    
    // Accessor for the property "ByteOffset"
    /**
     * Getter for property ByteOffset.<p>
    * An offset into a byte array holding member indicators, used in
    * conjunction with the ByteMask property.
    * 
    * (This is actually an "int index", not a "byte offset".)
     */
    public int getByteOffset()
        {
        return __m_ByteOffset;
        }
    
    // From interface: com.tangosol.net.Member
    // Accessor for the property "ClusterName"
    /**
     * Getter for property ClusterName.<p>
    * A configured name that should be the same for all Members that should be
    * able to join the same cluster. Like the Mode property, this is used only
    * to verify that the join can be successful.
     */
    public String getClusterName()
        {
        return __m_ClusterName;
        }
    
    // Accessor for the property "ContiguousFromPacketId"
    /**
     * Getter for property ContiguousFromPacketId.<p>
    * The packet id of last contiguous packet received by us from this Member. 
    * This id will be transmitted back to this Member in Acks, and will be
    * stored as ContiguousToPacketId.  It will be used by the remote member to
    * detect and fill in wholes caused by missing Acks. 
    * 
    * @volatile
     */
    public com.tangosol.net.internal.PacketIdentifier getContiguousFromPacketId()
        {
        return __m_ContiguousFromPacketId;
        }
    
    // Accessor for the property "ContiguousToPacketId"
    /**
     * Getter for property ContiguousToPacketId.<p>
    * As reported by the remote member, this is the id of the last packet in
    * the contigous message stream to have been received without gaps from us. 
    * Any packet ids before this one, have therefore been received.
    * 
    * This propery is maintained by the receiver, see onPacketAck.
    * @volatile
     */
    public com.tangosol.net.internal.PacketIdentifier getContiguousToPacketId()
        {
        return __m_ContiguousToPacketId;
        }
    
    // Accessor for the property "CpuCount"
    /**
     * Getter for property CpuCount.<p>
    * Number of CPU cores for the machine this Member is running on.
     */
    public int getCpuCount()
        {
        return __m_CpuCount;
        }
    
    // Accessor for the property "Edition"
    /**
     * Getter for property Edition.<p>
    * The Edition is the product type.
    * 
    * 0=Data Client (DC)
    * 1=Real-Time Client (RTC)
    * 2=Compute Client (CC)
    * 3=Standard Edition (SE)
    * 4=Enterprise Edition (EE)
    * 5=DataGrid Edition (GE)
    * 
    * To be in a cluster, a member must be running CC, SE, EE or GE. The valid
    * combinations are:
    * 
    * CC: can join a GE cluster (i.e. servers running GE and/or CC)
    * SE: can join a SE cluster
    * EE: can join an EE cluster
    * GE: can join a GE cluster (i.e. servers running GE and/or CC)
     */
    public int getEdition()
        {
        return __m_Edition;
        }
    
    // Accessor for the property "FlowControl"
    /**
     * Getter for property FlowControl.<p>
    * The Member's flow control related properties.  This object also acts as a
    * synchronization point for all flow control operations.  This property is
    * null if flow control is disabled.
     */
    public Member.FlowControl getFlowControl()
        {
        return __m_FlowControl;
        }
    
    // From interface: com.tangosol.net.Member
    // Accessor for the property "Id"
    /**
     * Getter for property Id.<p>
    * A small number that uniquely identifies the Member at this point in time
    * and does not change for the life of this Member. This value does not
    * uniquely identify the Member throughout the duration of the cluster
    * because Members that existed but left the cluster before this Member
    * existed may have had the same mini-id value and the same goes for Members
    * that may join the cluster after this Member leaves the cluster.
    * 
    * (Sometimes referred to as a "MiniId" in comparison to the "Uid".)
     */
    public int getId()
        {
        return __m_Id;
        }
    
    // Accessor for the property "LastHeuristicDeathMillis"
    /**
     * Getter for property LastHeuristicDeathMillis.<p>
    * The most recent datetime value that this Member failed to acknowledge at
    * least one directed packet within a heuristic timeout interval.  This
    * information is used in the witness protocol to help identify poorly
    * performing but still reachable members.
    * 
    * @since 12.2.1.4
     */
    public long getLastHeuristicDeathMillis()
        {
        return __m_LastHeuristicDeathMillis;
        }
    
    // Accessor for the property "LastIncomingMessageId"
    /**
     * Getter for property LastIncomingMessageId.<p>
    * The Id of the last Message (actually, unique Directed Packets) sent from
    * the Member to us. Approximate (may be in the wrong trint range);
    * maintained by PacketReceiver.
     */
    public long getLastIncomingMessageId()
        {
        return __m_LastIncomingMessageId;
        }
    
    // Accessor for the property "LastIncomingMillis"
    /**
     * Getter for property LastIncomingMillis.<p>
    * Datetime value that the last direct acknowlegement (AckPacket) from this
    * Member was received by the local Member. Maintained by the
    * PacketListener.
    * 
    * Note: Prior to Coherence 2.5.1 this property referred to ANY direct
    * incoming communication. The major reason for this change was realization
    * of the fact that a deaf, but still speaking member, is even more
    * dangerous than a completely dead (deaf and mute) one. Therefore, an
    * unsolicited communication (even a directed one) cannot serve as a proof
    * of a functioning communication chanel and cannot be taken into
    * consideration.
     */
    public long getLastIncomingMillis()
        {
        return __m_LastIncomingMillis;
        }
    
    // Accessor for the property "LastOutgoingMessageId"
    /**
     * Getter for property LastOutgoingMessageId.<p>
    * The Id of the last message  (actually, unique Directed Packets sent to
    * this Member.
     */
    protected long getLastOutgoingMessageId()
        {
        return __m_LastOutgoingMessageId;
        }
    
    // Accessor for the property "LastOutgoingMillis"
    /**
     * Getter for property LastOutgoingMillis.<p>
    * Datetime value that the last packet that requires a direct acknowlegement
    * was sent to this Member by the local Member. Maintained by the
    * PacketPublisher.
    * 
    * Note: Prior to Coherence 2.5.1 this property referred to ANY direct
    * outgoing communication. The major reason for this change was realization
    * of the fact that a deaf, but still speaking member, is even more
    * dangerous than a completely dead (deaf and mute) one. Therefore, an
    * unsolicited communication (even a directed one) cannot serve as a proof
    * of a functioning communication chanel and cannot be taken into
    * consideration.
     */
    public long getLastOutgoingMillis()
        {
        return __m_LastOutgoingMillis;
        }
    
    // Accessor for the property "LastSlowMillis"
    /**
     * Getter for property LastSlowMillis.<p>
    * The most recent datetime value that this Member failed to acknowledge at
    * least one directed packet within a 3/4 of the packet timeout interval.
    * This information allows us to see how many Members are likely to leave
    * the cluster in the near future (within a timeout interval). If a local
    * Member sees not more than one "slow" Member, it's quite more likely that
    * there is something wrong with that member than with the local one.
    * Alternatively, presence of many "slow" Members may point to this Member
    * being a culprit. Maintained by the PacketPublisher.
    * 
    * @since Coherence 3.2; added to improve handling for a "partial delivery
    * failure".
     */
    public long getLastSlowMillis()
        {
        return __m_LastSlowMillis;
        }
    
    // Accessor for the property "LastTimeoutMillis"
    /**
     * Getter for property LastTimeoutMillis.<p>
    * The most recent datetime value that this Member timed-out (exceeded its
    * configured timeout; either packet-timeout or ip-timeout).
    * 
    * @since Coherence 3.6
     */
    public long getLastTimeoutMillis()
        {
        return __m_LastTimeoutMillis;
        }
    
    public String getLicenseInfo()
        {
        return "Edition=" + EDITION_NAME[getEdition()]
             + ", Mode=" + MODE_NAME[getMode()]
             + ", CpuCount=" + getCpuCount()
             + ", SocketCount=" + getSocketCount();
        }
    
    // Accessor for the property "LocationInfo"
    /**
     * Getter for property LocationInfo.<p>
    * Human-readable string that contains a concatenation of location related
    * properties.
    * 
    * After considering a URI-like format:
    *     site.rack.member
    * we decided to use the DN-like concept:
    *     site:boston,machine:dev1
     */
    public String getLocationInfo()
        {
        StringBuffer sb = new StringBuffer();
        
        String sSite = getSiteName();
        if (sSite != null)
            {
            sb.append(",site:").append(sSite);
            }
        
        String sRack = getRackName();
        if (sRack != null)
            {
            sb.append(",rack:").append(sRack);
            }
        
        String sMachine = getMachineName();
        if (sMachine != null)
            {
            sb.append(",machine:").append(sMachine);
            }
        
        String sProcess = getProcessName();
        if (sProcess != null)
            {
            sb.append(",process:").append(sProcess);
            }
        
        String sMember = getMemberName();
        if (sMember != null)
            {
            sb.append(",member:").append(sMember);
            }
        
        return sb.length() == 0 ? "" : sb.substring(1);
        }
    
    // From interface: com.tangosol.net.Member
    // Accessor for the property "MachineId"
    /**
     * Getter for property MachineId.<p>
    * An identifier that should be the same for Members that are on the same
    * physical machine, and different for Members that are on different
    * physical machines. This is part of the unique identifier of the Member
    * (Timestamp, Address, Port, MachineId).
     */
    public int getMachineId()
        {
        return __m_MachineId;
        }
    
    // From interface: com.tangosol.net.Member
    // Accessor for the property "MachineName"
    /**
     * Getter for property MachineName.<p>
    * A configured name that should be the same for Members that are on the
    * same physical machine, and different for Members that are on different
    * physical machines.
     */
    public String getMachineName()
        {
        return __m_MachineName;
        }
    
    // From interface: com.tangosol.net.Member
    // Accessor for the property "MemberName"
    /**
     * Getter for property MemberName.<p>
    * A configured name that must be unique for every Member.
     */
    public String getMemberName()
        {
        return __m_MemberName;
        }
    
    // Accessor for the property "MessageIncoming"
    /**
     * Getter for property MessageIncoming.<p>
    * [ambient] Used by the PacketReceiver to collect out-of-order Messages and
    * Message parts in case of partial delivery.
     */
    public com.tangosol.coherence.component.util.WindowedArray getMessageIncoming()
        {
        return __m_MessageIncoming;
        }
    
    // Accessor for the property "MessagePile"
    /**
     * Getter for property MessagePile.<p>
    * [ambient] Sequel Message Packets from the Member that have not yet been
    * associated with a Message because the head (Directed) Packet has not been
    * received yet. Maintained by the PacketReceiver. Key is a long value
    * corresponding to the "from" Message id. Value is a SimpleLongArray of
    * Sequel Packets, or a Message object once the Directed packet is received
    * and until the Message is complete (i.e. delivered to a Service).
     */
    public com.tangosol.util.SparseArray getMessagePile()
        {
        return __m_MessagePile;
        }
    
    // Accessor for the property "Mode"
    /**
     * Getter for property Mode.<p>
    * The Mode is the "license type", i.e. evaluation, development or
    * production use.
    * 
    * 0=evaluation
    * 1=development
    * 2=production
    * 
    * It is important that members of different modes are not mixed and matched
    * within a particular cluster, since it is likely an accident (e.g. a
    * developer accidentally connecting to a production cluster).
     */
    public int getMode()
        {
        return __m_Mode;
        }
    
    // Accessor for the property "NewestFromPacketId"
    /**
     * Getter for property NewestFromPacketId.<p>
    * The message id of newest packet known to be sent from this Member to us.
    * 
    * @volatile
     */
    public com.tangosol.net.internal.PacketIdentifier getNewestFromPacketId()
        {
        return __m_NewestFromPacketId;
        }
    
    // Accessor for the property "NewestToPacketId"
    /**
     * Getter for property NewestToPacketId.<p>
    * The packet id of the newest ackable packet sent to this Member from us.
    * 
    * Note: this is not the last packet sent to this member, i.e. packet
    * resends will not update this value.
    * 
    * @volatile
     */
    public com.tangosol.net.internal.PacketIdentifier getNewestToPacketId()
        {
        return __m_NewestToPacketId;
        }
    
    // Accessor for the property "NextDestinationMessageId"
    /**
     * Getter for property NextDestinationMessageId.<p>
    * The trint value of the next Directed Message id that will be sent to the
    * Member.
     */
    public int getNextDestinationMessageId()
        {
        // note: as of Coherence 2.2, the message id will start with 1 and
        //       not zero to avoid issues with trints of zero and windowed
        //       arrays!
        
        long c = getLastOutgoingMessageId();
        setLastOutgoingMessageId(++c);
        return Packet.makeTrint(c);
        }
    
    // Accessor for the property "PacketAck"
    /**
     * Getter for property PacketAck.<p>
    * [ambient] An Ack Packet queued to be sent to the Member. The Packet is
    * created by the PacketReceiver and stored in this property, and the
    * PacketPublisher clears this property when the Packet is removed from the
    * send Queue.
    * 
    * @volatile the PacketPublisher and PacketReceiver both operate on this
    * value outside of synchronization.
     */
    public com.tangosol.coherence.component.net.packet.notifyPacket.Ack getPacketAck()
        {
        return __m_PacketAck;
        }
    
    // From interface: com.tangosol.net.Member
    // Accessor for the property "Port"
    /**
     * Getter for property Port.<p>
    * The port of the member's DatagramSocket for point-to-point communication.
    * This is part of the unique identifier of the Member (Timestamp, Address,
    * Port, MachineId).
     */
    public int getPort()
        {
        return __m_Port;
        }
    
    // Accessor for the property "PreferredAckSize"
    /**
     * Getter for property PreferredAckSize.<p>
    * The AckSize (as a number of acknowledged packets) which this node prefers
    * to receive.
     */
    public int getPreferredAckSize()
        {
        return __m_PreferredAckSize;
        }
    
    // Accessor for the property "PreferredPacketLength"
    /**
     * Getter for property PreferredPacketLength.<p>
    * The Member's preferred packet length.  When sending packets to this
    * member it is preferred that they not exceede the minimum of this value
    * and the sending member's value.
    * 
    * This value is updated when handshaking with the member during the
    * ClusterService welcome messages.
    * 
    * @since Coherence 3.6.1
     */
    public int getPreferredPacketLength()
        {
        return __m_PreferredPacketLength;
        }
    
    // Accessor for the property "PreferredPort"
    /**
     * Getter for property PreferredPort.<p>
    * The port of the member's DatagramSocket which is ideal for point-to-point
    * communication of packets at or under the preferred packet length.
    * 
    * This value is updated when handshaking with the member during the
    * ClusterService welcome messages.
    * 
    * @since Coherence 3.6.1
    * @since Coherence 12.2.1 left unset if the member runs the TransportService
     */
    public int getPreferredPort()
        {
        return __m_PreferredPort;
        }
    
    // Accessor for the property "PreferredSocketAddress"
    /**
     * Getter for property PreferredSocketAddress.<p>
    * Lazily calculated InetSocketAddress of the member's preferred
    * DatagramSocket for point-to-point communication.
    * 
    * This value is lazily computed, and will return the SocketAddress in the
    * case that the PreferredPort has not been set.
    * 
    * @since Coherence 3.6.1
     */
    public java.net.InetSocketAddress getPreferredSocketAddress()
        {
        // import java.net.InetSocketAddress;
        
        InetSocketAddress addr = __m_PreferredSocketAddress;
        if (addr == null)
            {
            int nPort = getPreferredPort();
            if (nPort == 0)
                {
                return getSocketAddress();
                }
            setPreferredSocketAddress(addr = new InetSocketAddress(getAddress(), nPort));
            }
        return addr;
        }
    
    // From interface: com.tangosol.net.Member
    // Accessor for the property "Priority"
    /**
     * Getter for property Priority.<p>
    * The priority or "weight" of the member; used to determine tie-breakers.
     */
    public int getPriority()
        {
        return __m_Priority;
        }
    
    // From interface: com.tangosol.net.Member
    // Accessor for the property "ProcessName"
    /**
     * Getter for property ProcessName.<p>
    * A configured name that should be the same for Members that are in the
    * same process (JVM), and different for Members that are in different
    * processes.
     */
    public String getProcessName()
        {
        return __m_ProcessName;
        }
    
    // From interface: com.tangosol.net.Member
    // Accessor for the property "RackName"
    /**
     * Getter for property RackName.<p>
    * A configured name that should be the same for Members that are on the
    * same physical "rack" (or frame or cage), and different for Members that
    * are on different physical "racks".
     */
    public String getRackName()
        {
        return __m_RackName;
        }
    
    // Accessor for the property "RecentPacketQueue"
    /**
     * Getter for property RecentPacketQueue.<p>
    * A queue tracking the order in which packets were sent to this member. 
    * The queue only contains confirmationRequired packets. As packets are
    * confirmed or lost they are removed from the queue; once resent a packet
    * is moved to the tail of the queue.  This is used as part of the early
    * packet loss detection (see Receiver.doEarlyLossDetection). At any point
    * the queue may contain both ack'd and unack'd packets.  The tail is
    * handled by the publisher, while the head is handled by the receiver.
     */
    public Member.SentQueue getRecentPacketQueue()
        {
        return __m_RecentPacketQueue;
        }
    
    // From interface: com.tangosol.net.Member
    // Accessor for the property "RoleName"
    /**
     * Getter for property RoleName.<p>
    * A configured name that can be used to indicate the role of a member to
    * the application. While managed by Coherence, this property is used only
    * by the application.
     */
    public String getRoleName()
        {
        return __m_RoleName;
        }
    
    // From interface: com.tangosol.net.Member
    // Accessor for the property "SiteName"
    /**
     * Getter for property SiteName.<p>
    * A configured name that should be the same for Members that are on the
    * same physical site (e.g. data center), and different for Members that are
    * on different physical sites.
     */
    public String getSiteName()
        {
        return __m_SiteName;
        }
    
    // Accessor for the property "SocketAddress"
    /**
     * Getter for property SocketAddress.<p>
    * Lazily calculated InetSocketAddress of the member's DatagramSocket for
    * point-to-point communication.
     */
    public java.net.InetSocketAddress getSocketAddress()
        {
        // import java.net.InetSocketAddress;
        
        InetSocketAddress addr = __m_SocketAddress;
        if (addr == null)
            {
            setSocketAddress(addr = new InetSocketAddress(getAddress(), getPort()));
            }
        return addr;
        }
    
    // Accessor for the property "SocketCount"
    /**
     * Getter for property SocketCount.<p>
    * Number of CPU sockets for the machine this Member is running on.
     */
    public int getSocketCount()
        {
        return __m_SocketCount;
        }
    
    // Accessor for the property "StatsCount"
    /**
     * Getter for property StatsCount.<p>
    * *** LICENSE ***
    * WARNING: This property name is obfuscated.
    * 
    * [0] - core count
    * [1] - Socket count
     */
    public int[] getStatsCount()
        {
        return new int[]
            {
            getCpuCount(),
            getSocketCount()
            };
        }
    
    // Accessor for the property "StatsPublisherSuccessRate"
    /**
     * Getter for property StatsPublisherSuccessRate.<p>
    * The publisher success rate to this member since the stats were last reset.
     */
    public double getStatsPublisherSuccessRate()
        {
        long lSent   = getStatsSent();
        long lResent = getStatsResent();
        
        return lSent == 0L ? 1.0D : 1.0D - ((double) lResent)/((double) lSent);
        }
    
    // Accessor for the property "StatsReceived"
    /**
     * Getter for property StatsReceived.<p>
    * Statistics: total number of received packets.
     */
    public long getStatsReceived()
        {
        return __m_StatsReceived;
        }
    
    // Accessor for the property "StatsReceiverSuccessRate"
    /**
     * Getter for property StatsReceiverSuccessRate.<p>
    * The receiver success rate to this member since the stats were last reset.
     */
    public double getStatsReceiverSuccessRate()
        {
        long lReceived = getStatsReceived();
        long lRepeated = getStatsRepeated();
        
        return lReceived == 0L ? 1.0D : 1.0D - ((double) lRepeated)/((double) lReceived);
        }
    
    // Accessor for the property "StatsRepeated"
    /**
     * Getter for property StatsRepeated.<p>
    * Statistics: number of repeated packets (recieved more then once)
     */
    public long getStatsRepeated()
        {
        return __m_StatsRepeated;
        }
    
    // Accessor for the property "StatsResent"
    /**
     * Getter for property StatsResent.<p>
    * Statistics: total number of re-sent packets.
     */
    public long getStatsResent()
        {
        return __m_StatsResent;
        }
    
    // Accessor for the property "StatsReset"
    /**
     * Getter for property StatsReset.<p>
    * Statistics: Date/time value that the stats have been reset.
     */
    public long getStatsReset()
        {
        return __m_StatsReset;
        }
    
    // Accessor for the property "StatsSent"
    /**
     * Getter for property StatsSent.<p>
    * Statistics: total number of sent packets.
     */
    public long getStatsSent()
        {
        return __m_StatsSent;
        }
    
    // Accessor for the property "TcpRingPort"
    /**
     * Getter for property TcpRingPort.<p>
    * The port on which this member is listening for TcpRing connections.  Note
    * that when the member is using a configured port this will match the Port
    * property.
    * 
    * @since 12.2.1
     */
    public int getTcpRingPort()
        {
        return __m_TcpRingPort;
        }
    
    // From interface: com.tangosol.net.Member
    // Accessor for the property "Timestamp"
    /**
     * Getter for property Timestamp.<p>
    * Date/time value that the Member was born. This is part of the unique
    * identifier of the Member (Timestamp, Address, Port, MachineId).
    * 
    * Note: If the Member is marked as Dead, the Timestamp value indicates the
    * departure time (local clock).  In the case of zombies, the timestamp is
    * periodically updated after death as part of the panic protocol.
    * 
    * @see declareZombie
     */
    public long getTimestamp()
        {
        return __m_Timestamp;
        }
    
    // Accessor for the property "TxDebugDropCount"
    /**
     * Getter for property TxDebugDropCount.<p>
    * The number of times to artificially drop each packet sent to this member
    * before actually sending them.  Used for debugging only.
     */
    public int getTxDebugDropCount()
        {
        return __m_TxDebugDropCount;
        }
    
    // From interface: com.tangosol.net.Member
    // Accessor for the property "Uid"
    /**
     * Getter for property Uid.<p>
    * The unique identifier of the Member (Timestamp, Address, Port, MachineId)
    * packed into a UID. This is an obsoleted 16 byte UID, which is left for
    * backward compatibility reason.
     */
    public com.tangosol.util.UID getUid()
        {
        // import com.tangosol.net.InetAddressHelper;
        // import com.tangosol.util.UID;
        // import java.net.InetAddress;
        
        // for backward compatibility only
        // _trace("An obsolete getUid() call; Use getUuid() instead\n" + get_StackTrace(), 4);
        
        // the Timestamp changes when the member dies, but the UID should not change
        long        ldt        = getUid32().getTimestamp();
        InetAddress addr       = getAddress();
        int         nPort      = getPort();
        int         nMachineId = getMachineId();
        
        int nAddr  = (int) (InetAddressHelper.toLong(addr) & 0xFFFFFFFFL);
        int nCount = (nPort & 0xFFFF) | ((nMachineId & 0xFFFF) << 16);
        return new UID(nAddr, ldt, nCount);
        }
    
    // Accessor for the property "Uid32"
    /**
     * Getter for property Uid32.<p>
    * The unique identifier of the Member (Timestamp, Address, Port, MachineId)
    * packed into a UID. This is a new 32 byte UID that replaced the old 16
    * bytes one.
     */
    public com.tangosol.util.UUID getUid32()
        {
        // import com.tangosol.util.UUID;
        // import java.net.InetAddress;
        
        UUID uid = __m_Uid32;
        
        if (uid == null)
            {
            long        ldt        = getTimestamp();
            InetAddress addr       = getAddress();
            int         nPort      = getPort();
            int         nMachineId = getMachineId();
        
            _assert(ldt != 0);
        
            uid = new UUID(ldt, addr, nPort, nMachineId);
            setUid32(uid);
            }
        
        return uid;
        }
    
    // From interface: com.tangosol.net.Member
    // Accessor for the property "Uuid"
    /**
     * Getter for property Uuid.<p>
    * The unique identifier of the Member (Timestamp, Address, Port, MachineId)
    * packed into a UID. This is a new 32 byte UID that replaced the old 16
    * bytes one.
     */
    public com.tangosol.util.UUID getUuid()
        {
        return getUid32();
        }
    
    // Declared at the super level
    public int hashCode()
        {
        // import com.tangosol.util.UUID;
        
        UUID uuid = getUid32();
        
        return uuid == null ? super.hashCode() : uuid.hashCode();
        }
    
    /**
     * Initialize various communication-related data structures.
     */
    public void initCommSupport()
        {
        // import Component.Util.WindowedArray;
        
        // allow this method to be called more than once
        if (getMessageIncoming() == null)
            {
            // initialize message windowed-array to skip index zero
            WindowedArray waMsg = new WindowedArray();
            waMsg.remove(waMsg.add(null));
            setMessageIncoming(waMsg);
        
            setRecentPacketQueue((Member.SentQueue) _newChild("SentQueue"));
        
            if (Member.FlowControl.isEnabled())
                {
                Member.FlowControl fc = (Member.FlowControl) _newChild("FlowControl");
                setFlowControl(fc);
                }
        
            resetStats();
            }
        }
    
    /**
     * Return true if the specified member is collocated with this member. 
    * Collocated here means that the two members reside on the same IP stack,
    * i.e. they should be able to reach each other using only the loopback
    * address for instance.
     */
    public boolean isCollocated(Member member)
        {
        return member.getAddress().equals(getAddress());
        }
    
    // Accessor for the property "Dead"
    /**
     * Getter for property Dead.<p>
    * Set to true when the Member is determined to be dead.
     */
    public boolean isDead()
        {
        return __m_Dead;
        }
    
    // Accessor for the property "Deaf"
    /**
     * Getter for property Deaf.<p>
    * Set to true to indicates that the Member has failed to acknowledge at
    * least one directed packet within a timeout interval.
    * 
    * That could mean one of two things:
    * a) the member has indeed left the cluster and has to be removed;
    * b) the local member is the deaf one and has to be stopped
    * 
    * Note: A deaf member represents a danger to the cluster integrity since it
    * does not lisen to any advice from others, but can send (broadcast)
    * incoherent and confusing messages. A dumb (mute) node does not cause any
    * problems to the outside world.
    * 
    * Note: If the Member is marked as Dead, the Deaf flag indicates that it
    * became a "zombie".
    * 
    * @see Zombie
     */
    public boolean isDeaf()
        {
        return __m_Deaf;
        }
    
    // Accessor for the property "Leaving"
    /**
     * Getter for property Leaving.<p>
    * Set to true when the Member is known to be leaving the cluster.
    * 
    * Overloaded to indicate if the notification to all members is in doubt.
    * Having "leaving" and "dead" both true means that the local node is unsure
    * if all nodes have been informed of the death of the member.
     */
    public boolean isLeaving()
        {
        return __m_Leaving;
        }
    
    // Accessor for the property "TimedOut"
    /**
     * Getter for property TimedOut.<p>
     */
    public boolean isTimedOut()
        {
        return getLastTimeoutMillis() != 0L;
        }
    
    // Accessor for the property "Zombie"
    /**
     * Getter for property Zombie.<p>
    * Calculated property indicating if this member has been declared a Zombie
    * - as it keeps broadcasting after it has been forcefully removed from the
    * cluster.
    * 
    * @see Deaf
    * @see declareZombie
     */
    public boolean isZombie()
        {
        return isDead() && isDeaf();
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
        if (is_Deserialized())
            {
            configure(getUid32());
        
            int nId = getId();
            setByteOffset(calcByteOffset(nId));
            setByteMask(calcByteMask(nId));
            }
        
        super.onInit();
        }
    
    // From interface: com.tangosol.io.pof.PortableObject
    public void readExternal(com.tangosol.io.pof.PofReader in)
            throws java.io.IOException
        {
        // import com.tangosol.util.UUID;
        
        UUID uid = (UUID) in.readObject(0);
        int  nId = in.readShort(1);
        if (nId != 0)
            {
            setId(nId);
            }
        
        configure(uid);
        
        setEdition(in.readInt(2));
        setMode(in.readInt(3));
        setCpuCount(in.readInt(4));
        setSocketCount(in.readInt(5));
        setClusterName(in.readString(6));
        setSiteName(in.readString(7));
        setRackName(in.readString(8));
        setMachineName(in.readString(9));
        setProcessName(in.readString(10));
        setMemberName(in.readString(11));
        setRoleName(in.readString(12));
        setPriority(in.readInt(13));
        }
    
    // From interface: com.tangosol.io.ExternalizableLite
    public void readExternal(java.io.DataInput in)
            throws java.io.IOException
        {
        // import com.tangosol.util.ExternalizableHelper as com.tangosol.util.ExternalizableHelper;
        // import com.tangosol.util.UUID;
        
        // pre-3.2
        UUID uid = new UUID(in);
        int  nId = in.readUnsignedShort();
        if (nId != 0)
            {
            setId(nId);
            }
        
        configure(uid);
        
        // version 3.2
        setEdition(com.tangosol.util.ExternalizableHelper.readInt(in));
        setMode(com.tangosol.util.ExternalizableHelper.readInt(in));
        setCpuCount(com.tangosol.util.ExternalizableHelper.readInt(in));
        setSocketCount(com.tangosol.util.ExternalizableHelper.readInt(in));
        setClusterName(com.tangosol.util.ExternalizableHelper.readUTF(in));
        setSiteName(com.tangosol.util.ExternalizableHelper.readUTF(in));
        setRackName(com.tangosol.util.ExternalizableHelper.readUTF(in));
        setMachineName(com.tangosol.util.ExternalizableHelper.readUTF(in));
        setProcessName(com.tangosol.util.ExternalizableHelper.readUTF(in));
        setMemberName(com.tangosol.util.ExternalizableHelper.readUTF(in));
        setRoleName(com.tangosol.util.ExternalizableHelper.readUTF(in));
        setPriority(com.tangosol.util.ExternalizableHelper.readInt(in));
        }
    
    // From interface: java.io.Externalizable
    public void readExternal(java.io.ObjectInput in)
            throws java.io.IOException,
                   java.lang.ClassNotFoundException
        {
        // import java.io.DataInput;
        
        readExternal((DataInput) in);
        }
    
    /**
     * Reset the statistics.
     */
    public void resetStats()
        {
        // import com.tangosol.util.Base;
        
        Member.FlowControl flowControl = getFlowControl();
        
        if (flowControl != null)
            {
            flowControl.resetStats();
            }
        setStatsSent    (0L);
        setStatsResent  (0L);
        setStatsReceived(0L);
        setStatsRepeated(0L);
        setStatsReset   (Base.getSafeTimeMillis());
        }
    
    // Accessor for the property "Address"
    /**
     * Setter for property Address.<p>
    * The IP address of the member's DatagramSocket for point-to-point
    * communication. This is part of the unique identifier of the Member
    * (Timestamp, Address, Port, MachineId).
     */
    protected void setAddress(java.net.InetAddress addr)
        {
        // import java.net.InetAddress;
        
        if (addr == null)
            {
            throw new IllegalArgumentException();
            }
        
        InetAddress addrOld = getAddress();
        _assert(addrOld == null || addrOld.equals(addr));
        
        __m_Address = (addr);
        }
    
    // Accessor for the property "ByteMask"
    /**
     * Setter for property ByteMask.<p>
    * A 32-bit integer value between 1 and 128 inclusive, with only one bit
    * set. When bitanded with the byte value from ByteOffset in a byte array
    * holding member indicators, the value will = 0 (member not specified) or
    * != 0 (member specified).
    * 
    * (This is actually an "int mask", not a "byte mask".)
     */
    protected void setByteMask(int nMask)
        {
        __m_ByteMask = nMask;
        }
    
    // Accessor for the property "ByteOffset"
    /**
     * Setter for property ByteOffset.<p>
    * An offset into a byte array holding member indicators, used in
    * conjunction with the ByteMask property.
    * 
    * (This is actually an "int index", not a "byte offset".)
     */
    protected void setByteOffset(int nOffset)
        {
        __m_ByteOffset = nOffset;
        }
    
    // Accessor for the property "ClusterName"
    /**
     * Setter for property ClusterName.<p>
    * A configured name that should be the same for all Members that should be
    * able to join the same cluster. Like the Mode property, this is used only
    * to verify that the join can be successful.
     */
    protected void setClusterName(String sName)
        {
        String sNameOld = getClusterName();
        _assert(sNameOld == null || sNameOld.equals(sName));
        
        __m_ClusterName = (sName);
        }
    
    // Accessor for the property "ContiguousFromPacketId"
    /**
     * Setter for property ContiguousFromPacketId.<p>
    * The packet id of last contiguous packet received by us from this Member. 
    * This id will be transmitted back to this Member in Acks, and will be
    * stored as ContiguousToPacketId.  It will be used by the remote member to
    * detect and fill in wholes caused by missing Acks. 
    * 
    * @volatile
     */
    public void setContiguousFromPacketId(com.tangosol.net.internal.PacketIdentifier packetId)
        {
        __m_ContiguousFromPacketId = packetId;
        }
    
    // Accessor for the property "ContiguousToPacketId"
    /**
     * Setter for property ContiguousToPacketId.<p>
    * As reported by the remote member, this is the id of the last packet in
    * the contigous message stream to have been received without gaps from us. 
    * Any packet ids before this one, have therefore been received.
    * 
    * This propery is maintained by the receiver, see onPacketAck.
    * @volatile
     */
    public void setContiguousToPacketId(com.tangosol.net.internal.PacketIdentifier packetId)
        {
        __m_ContiguousToPacketId = packetId;
        }
    
    // Accessor for the property "CpuCount"
    /**
     * Setter for property CpuCount.<p>
    * Number of CPU cores for the machine this Member is running on.
     */
    private void setCpuCount(int cCpus)
        {
        if (cCpus > 0 && getCpuCount() == 0)
            {
            __m_CpuCount = (cCpus);
            }
        }
    
    // Accessor for the property "Dead"
    /**
     * Setter for property Dead.<p>
    * Set to true when the Member is determined to be dead.
     */
    public void setDead(boolean fDead)
        {
        // import com.tangosol.util.Base;
        
        _assert(fDead);
        
        if (!isDead())
            {
            __m_Dead = (fDead);
            setTimestamp(Base.getSafeTimeMillis());
            }
        
        // reset the Deaf flag; it will be reused if the Member becomes a zombie
        setDeaf(false);
        
        // TODO - clean up the "expensive" data structures without screwing
        //        up any of the daemons
        }
    
    // Accessor for the property "Deaf"
    /**
     * Setter for property Deaf.<p>
    * Set to true to indicates that the Member has failed to acknowledge at
    * least one directed packet within a timeout interval.
    * 
    * That could mean one of two things:
    * a) the member has indeed left the cluster and has to be removed;
    * b) the local member is the deaf one and has to be stopped
    * 
    * Note: A deaf member represents a danger to the cluster integrity since it
    * does not lisen to any advice from others, but can send (broadcast)
    * incoherent and confusing messages. A dumb (mute) node does not cause any
    * problems to the outside world.
    * 
    * Note: If the Member is marked as Dead, the Deaf flag indicates that it
    * became a "zombie".
    * 
    * @see Zombie
     */
    public void setDeaf(boolean fDeaf)
        {
        __m_Deaf = fDeaf;
        }
    
    // Accessor for the property "Edition"
    /**
     * Setter for property Edition.<p>
    * The Edition is the product type.
    * 
    * 0=Data Client (DC)
    * 1=Real-Time Client (RTC)
    * 2=Compute Client (CC)
    * 3=Standard Edition (SE)
    * 4=Enterprise Edition (EE)
    * 5=DataGrid Edition (GE)
    * 
    * To be in a cluster, a member must be running CC, SE, EE or GE. The valid
    * combinations are:
    * 
    * CC: can join a GE cluster (i.e. servers running GE and/or CC)
    * SE: can join a SE cluster
    * EE: can join an EE cluster
    * GE: can join a GE cluster (i.e. servers running GE and/or CC)
     */
    private void setEdition(int n)
        {
        _assert(getEdition() == 0);
        
        __m_Edition = (n);
        }
    
    // Accessor for the property "FlowControl"
    /**
     * Setter for property FlowControl.<p>
    * The Member's flow control related properties.  This object also acts as a
    * synchronization point for all flow control operations.  This property is
    * null if flow control is disabled.
     */
    protected void setFlowControl(Member.FlowControl flowControl)
        {
        __m_FlowControl = flowControl;
        }
    
    // Accessor for the property "Id"
    /**
     * Setter for property Id.<p>
    * A small number that uniquely identifies the Member at this point in time
    * and does not change for the life of this Member. This value does not
    * uniquely identify the Member throughout the duration of the cluster
    * because Members that existed but left the cluster before this Member
    * existed may have had the same mini-id value and the same goes for Members
    * that may join the cluster after this Member leaves the cluster.
    * 
    * (Sometimes referred to as a "MiniId" in comparison to the "Uid".)
     */
    public void setId(int nId)
        {
        if (nId <= 0)
            {
            throw new IllegalArgumentException();
            }
        
        int nIdOld = getId();
        _assert(nIdOld == 0 || nId == nIdOld);
        
        __m_Id = (nId);
        
        setByteOffset(calcByteOffset(nId));
        setByteMask(calcByteMask(nId));
        }
    
    // Accessor for the property "LastHeuristicDeathMillis"
    /**
     * Setter for property LastHeuristicDeathMillis.<p>
    * The most recent datetime value that this Member failed to acknowledge at
    * least one directed packet within a heuristic timeout interval.  This
    * information is used in the witness protocol to help identify poorly
    * performing but still reachable members.
    * 
    * @since 12.2.1.4
     */
    public void setLastHeuristicDeathMillis(long lMillis)
        {
        __m_LastHeuristicDeathMillis = lMillis;
        }
    
    // Accessor for the property "LastIncomingMessageId"
    /**
     * Setter for property LastIncomingMessageId.<p>
    * The Id of the last Message (actually, unique Directed Packets) sent from
    * the Member to us. Approximate (may be in the wrong trint range);
    * maintained by PacketReceiver.
     */
    public void setLastIncomingMessageId(long cMessages)
        {
        __m_LastIncomingMessageId = cMessages;
        }
    
    // Accessor for the property "LastIncomingMillis"
    /**
     * Setter for property LastIncomingMillis.<p>
    * Datetime value that the last direct acknowlegement (AckPacket) from this
    * Member was received by the local Member. Maintained by the
    * PacketListener.
    * 
    * Note: Prior to Coherence 2.5.1 this property referred to ANY direct
    * incoming communication. The major reason for this change was realization
    * of the fact that a deaf, but still speaking member, is even more
    * dangerous than a completely dead (deaf and mute) one. Therefore, an
    * unsolicited communication (even a directed one) cannot serve as a proof
    * of a functioning communication chanel and cannot be taken into
    * consideration.
     */
    public void setLastIncomingMillis(long cMillis)
        {
        __m_LastIncomingMillis = (cMillis);
        }
    
    // Accessor for the property "LastOutgoingMessageId"
    /**
     * Setter for property LastOutgoingMessageId.<p>
    * The Id of the last message  (actually, unique Directed Packets sent to
    * this Member.
     */
    protected void setLastOutgoingMessageId(long cMessages)
        {
        __m_LastOutgoingMessageId = cMessages;
        }
    
    // Accessor for the property "LastOutgoingMillis"
    /**
     * Setter for property LastOutgoingMillis.<p>
    * Datetime value that the last packet that requires a direct acknowlegement
    * was sent to this Member by the local Member. Maintained by the
    * PacketPublisher.
    * 
    * Note: Prior to Coherence 2.5.1 this property referred to ANY direct
    * outgoing communication. The major reason for this change was realization
    * of the fact that a deaf, but still speaking member, is even more
    * dangerous than a completely dead (deaf and mute) one. Therefore, an
    * unsolicited communication (even a directed one) cannot serve as a proof
    * of a functioning communication chanel and cannot be taken into
    * consideration.
     */
    public void setLastOutgoingMillis(long cMillis)
        {
        __m_LastOutgoingMillis = cMillis;
        }
    
    // Accessor for the property "LastSlowMillis"
    /**
     * Setter for property LastSlowMillis.<p>
    * The most recent datetime value that this Member failed to acknowledge at
    * least one directed packet within a 3/4 of the packet timeout interval.
    * This information allows us to see how many Members are likely to leave
    * the cluster in the near future (within a timeout interval). If a local
    * Member sees not more than one "slow" Member, it's quite more likely that
    * there is something wrong with that member than with the local one.
    * Alternatively, presence of many "slow" Members may point to this Member
    * being a culprit. Maintained by the PacketPublisher.
    * 
    * @since Coherence 3.2; added to improve handling for a "partial delivery
    * failure".
     */
    public void setLastSlowMillis(long lMillis)
        {
        __m_LastSlowMillis = lMillis;
        }
    
    // Accessor for the property "LastTimeoutMillis"
    /**
     * Setter for property LastTimeoutMillis.<p>
    * The most recent datetime value that this Member timed-out (exceeded its
    * configured timeout; either packet-timeout or ip-timeout).
    * 
    * @since Coherence 3.6
     */
    public void setLastTimeoutMillis(long cTimeoutMillis)
        {
        __m_LastTimeoutMillis = cTimeoutMillis;
        }
    
    // Accessor for the property "Leaving"
    /**
     * Setter for property Leaving.<p>
    * Set to true when the Member is known to be leaving the cluster.
    * 
    * Overloaded to indicate if the notification to all members is in doubt.
    * Having "leaving" and "dead" both true means that the local node is unsure
    * if all nodes have been informed of the death of the member.
     */
    public void setLeaving(boolean fLeaving)
        {
        __m_Leaving = fLeaving;
        }
    
    // Accessor for the property "MachineId"
    /**
     * Setter for property MachineId.<p>
    * An identifier that should be the same for Members that are on the same
    * physical machine, and different for Members that are on different
    * physical machines. This is part of the unique identifier of the Member
    * (Timestamp, Address, Port, MachineId).
     */
    protected void setMachineId(int nId)
        {
        if (nId < 0 || nId > 0xFFFF)
            {
            throw new IllegalArgumentException();
            }
        
        int nIdOld = getMachineId();
        _assert(nIdOld == 0 || nId == nIdOld);
        
        __m_MachineId = (nId);
        }
    
    // Accessor for the property "MachineName"
    /**
     * Setter for property MachineName.<p>
    * A configured name that should be the same for Members that are on the
    * same physical machine, and different for Members that are on different
    * physical machines.
     */
    protected void setMachineName(String sName)
        {
        String sNameOld = getMachineName();
        _assert(sNameOld == null || sNameOld.equals(sName));
        
        __m_MachineName = (sName);
        }
    
    // Accessor for the property "MemberName"
    /**
     * Setter for property MemberName.<p>
    * A configured name that must be unique for every Member.
     */
    protected void setMemberName(String sName)
        {
        String sNameOld = getMemberName();
        _assert(sNameOld == null || sNameOld.equals(sName));
        
        __m_MemberName = (sName);
        }
    
    // Accessor for the property "MessageIncoming"
    /**
     * Setter for property MessageIncoming.<p>
    * [ambient] Used by the PacketReceiver to collect out-of-order Messages and
    * Message parts in case of partial delivery.
     */
    public void setMessageIncoming(com.tangosol.coherence.component.util.WindowedArray waMessage)
        {
        __m_MessageIncoming = waMessage;
        }
    
    // Accessor for the property "MessagePile"
    /**
     * Setter for property MessagePile.<p>
    * [ambient] Sequel Message Packets from the Member that have not yet been
    * associated with a Message because the head (Directed) Packet has not been
    * received yet. Maintained by the PacketReceiver. Key is a long value
    * corresponding to the "from" Message id. Value is a SimpleLongArray of
    * Sequel Packets, or a Message object once the Directed packet is received
    * and until the Message is complete (i.e. delivered to a Service).
     */
    public void setMessagePile(com.tangosol.util.SparseArray map)
        {
        __m_MessagePile = map;
        }
    
    // Accessor for the property "Mode"
    /**
     * Setter for property Mode.<p>
    * The Mode is the "license type", i.e. evaluation, development or
    * production use.
    * 
    * 0=evaluation
    * 1=development
    * 2=production
    * 
    * It is important that members of different modes are not mixed and matched
    * within a particular cluster, since it is likely an accident (e.g. a
    * developer accidentally connecting to a production cluster).
     */
    private void setMode(int n)
        {
        _assert(getMode() == 0);
        
        __m_Mode = (n);
        }
    
    // Accessor for the property "NewestFromPacketId"
    /**
     * Setter for property NewestFromPacketId.<p>
    * The message id of newest packet known to be sent from this Member to us.
    * 
    * @volatile
     */
    public void setNewestFromPacketId(com.tangosol.net.internal.PacketIdentifier packetId)
        {
        __m_NewestFromPacketId = packetId;
        }
    
    // Accessor for the property "NewestToPacketId"
    /**
     * Setter for property NewestToPacketId.<p>
    * The packet id of the newest ackable packet sent to this Member from us.
    * 
    * Note: this is not the last packet sent to this member, i.e. packet
    * resends will not update this value.
    * 
    * @volatile
     */
    public void setNewestToPacketId(com.tangosol.net.internal.PacketIdentifier packetId)
        {
        __m_NewestToPacketId = packetId;
        }
    
    // Accessor for the property "PacketAck"
    /**
     * Even though PacketAck is volatile the mutator must be synchronized to
    * handle the case when the publisher is setting this property to null
    * because it is sending the Ack.  If this is not synchronized then the
    * publisher could overwrite a value which was updated by the receiver.  An
    * AtomicReference is really what is needed here.  Volatile is still used to
    * allow for up to date unsynchronized reads.
    * 
    * @see PacketPublisher$AckQueue.removeNoWait
    * @see PacketPublisher$AckQueue.add
    * @see PacketReceiver.confirm
     */
    public void setPacketAck(com.tangosol.coherence.component.net.packet.notifyPacket.Ack packet)
        {
        __m_PacketAck = packet;
        }
    
    // Accessor for the property "Port"
    /**
     * Setter for property Port.<p>
    * The port of the member's DatagramSocket for point-to-point communication.
    * This is part of the unique identifier of the Member (Timestamp, Address,
    * Port, MachineId).
     */
    protected void setPort(int nPort)
        {
        if (nPort < 0 || nPort > 0xFFFF)
            {
            throw new IllegalArgumentException();
            }
        
        int nPortOld = getPort();
        _assert(nPortOld == 0 || nPort == nPortOld);
        
        __m_Port = (nPort);
        }
    
    // Accessor for the property "PreferredAckSize"
    /**
     * Setter for property PreferredAckSize.<p>
    * The AckSize (as a number of acknowledged packets) which this node prefers
    * to receive.
     */
    public void setPreferredAckSize(int cPackets)
        {
        __m_PreferredAckSize = cPackets;
        }
    
    // Accessor for the property "PreferredPacketLength"
    /**
     * Setter for property PreferredPacketLength.<p>
    * The Member's preferred packet length.  When sending packets to this
    * member it is preferred that they not exceede the minimum of this value
    * and the sending member's value.
    * 
    * This value is updated when handshaking with the member during the
    * ClusterService welcome messages.
    * 
    * @since Coherence 3.6.1
     */
    public void setPreferredPacketLength(int cPackets)
        {
        __m_PreferredPacketLength = cPackets;
        }
    
    // Accessor for the property "PreferredPort"
    /**
     * Setter for property PreferredPort.<p>
    * The port of the member's DatagramSocket which is ideal for point-to-point
    * communication of packets at or under the preferred packet length.
    * 
    * This value is updated when handshaking with the member during the
    * ClusterService welcome messages.
    * 
    * @since Coherence 3.6.1
    * @since Coherence 12.2.1 left unset if the member runs the TransportService
     */
    public void setPreferredPort(int nPort)
        {
        if (nPort < 0 || nPort > 0xFFFF)
            {
            throw new IllegalArgumentException();
            }
        
        int nPortOld = getPreferredPort();
        _assert(nPortOld == 0 || nPort == nPortOld);
        
        __m_PreferredPort = (nPort);
        setPreferredSocketAddress(null);
        }
    
    // Accessor for the property "PreferredSocketAddress"
    /**
     * Setter for property PreferredSocketAddress.<p>
    * Lazily calculated InetSocketAddress of the member's preferred
    * DatagramSocket for point-to-point communication.
    * 
    * This value is lazily computed, and will return the SocketAddress in the
    * case that the PreferredPort has not been set.
    * 
    * @since Coherence 3.6.1
     */
    protected void setPreferredSocketAddress(java.net.InetSocketAddress addr)
        {
        __m_PreferredSocketAddress = addr;
        }
    
    // Accessor for the property "Priority"
    /**
     * Setter for property Priority.<p>
    * The priority or "weight" of the member; used to determine tie-breakers.
     */
    protected void setPriority(int n)
        {
        __m_Priority = (Math.min(Math.max(n, 0), 10));
        }
    
    // Accessor for the property "ProcessName"
    /**
     * Setter for property ProcessName.<p>
    * A configured name that should be the same for Members that are in the
    * same process (JVM), and different for Members that are in different
    * processes.
     */
    protected void setProcessName(String sName)
        {
        String sNameOld = getProcessName();
        _assert(sNameOld == null || sNameOld.equals(sName));
        
        __m_ProcessName = (sName);
        }
    
    // Accessor for the property "RackName"
    /**
     * Setter for property RackName.<p>
    * A configured name that should be the same for Members that are on the
    * same physical "rack" (or frame or cage), and different for Members that
    * are on different physical "racks".
     */
    protected void setRackName(String sName)
        {
        String sNameOld = getRackName();
        _assert(sNameOld == null || sNameOld.equals(sName));
        
        __m_RackName = (sName);
        }
    
    // Accessor for the property "RecentPacketQueue"
    /**
     * Setter for property RecentPacketQueue.<p>
    * A queue tracking the order in which packets were sent to this member. 
    * The queue only contains confirmationRequired packets. As packets are
    * confirmed or lost they are removed from the queue; once resent a packet
    * is moved to the tail of the queue.  This is used as part of the early
    * packet loss detection (see Receiver.doEarlyLossDetection). At any point
    * the queue may contain both ack'd and unack'd packets.  The tail is
    * handled by the publisher, while the head is handled by the receiver.
     */
    protected void setRecentPacketQueue(Member.SentQueue queue)
        {
        __m_RecentPacketQueue = queue;
        }
    
    // Accessor for the property "RoleName"
    /**
     * Setter for property RoleName.<p>
    * A configured name that can be used to indicate the role of a member to
    * the application. While managed by Coherence, this property is used only
    * by the application.
     */
    protected void setRoleName(String sName)
        {
        String sNameOld = getRoleName();
        _assert(sNameOld == null || sNameOld.equals(sName));
        
        __m_RoleName = (sName);
        }
    
    // Accessor for the property "SiteName"
    /**
     * Setter for property SiteName.<p>
    * A configured name that should be the same for Members that are on the
    * same physical site (e.g. data center), and different for Members that are
    * on different physical sites.
     */
    protected void setSiteName(String sName)
        {
        String sNameOld = getSiteName();
        _assert(sNameOld == null || sNameOld.equals(sName));
        
        __m_SiteName = (sName);
        }
    
    // Accessor for the property "SocketAddress"
    /**
     * Setter for property SocketAddress.<p>
    * Lazily calculated InetSocketAddress of the member's DatagramSocket for
    * point-to-point communication.
     */
    protected void setSocketAddress(java.net.InetSocketAddress addr)
        {
        __m_SocketAddress = addr;
        }
    
    // Accessor for the property "SocketCount"
    /**
     * Setter for property SocketCount.<p>
    * Number of CPU sockets for the machine this Member is running on.
     */
    private void setSocketCount(int cSockets)
        {
        if (cSockets > 0 && getSocketCount() == 0)
            {
            __m_SocketCount = (cSockets);
            }
        }
    
    // Accessor for the property "StatsReceived"
    /**
     * Setter for property StatsReceived.<p>
    * Statistics: total number of received packets.
     */
    public void setStatsReceived(long cReceived)
        {
        __m_StatsReceived = cReceived;
        }
    
    // Accessor for the property "StatsRepeated"
    /**
     * Setter for property StatsRepeated.<p>
    * Statistics: number of repeated packets (recieved more then once)
     */
    public void setStatsRepeated(long cRepeated)
        {
        __m_StatsRepeated = cRepeated;
        }
    
    // Accessor for the property "StatsResent"
    /**
     * Setter for property StatsResent.<p>
    * Statistics: total number of re-sent packets.
     */
    public void setStatsResent(long cResent)
        {
        __m_StatsResent = cResent;
        }
    
    // Accessor for the property "StatsReset"
    /**
     * Setter for property StatsReset.<p>
    * Statistics: Date/time value that the stats have been reset.
     */
    public void setStatsReset(long ldtReset)
        {
        __m_StatsReset = ldtReset;
        }
    
    // Accessor for the property "StatsSent"
    /**
     * Setter for property StatsSent.<p>
    * Statistics: total number of sent packets.
     */
    public void setStatsSent(long cSent)
        {
        __m_StatsSent = cSent;
        }
    
    // Accessor for the property "TcpRingPort"
    /**
     * Setter for property TcpRingPort.<p>
    * The port on which this member is listening for TcpRing connections.  Note
    * that when the member is using a configured port this will match the Port
    * property.
    * 
    * @since 12.2.1
     */
    public void setTcpRingPort(int nPort)
        {
        __m_TcpRingPort = nPort;
        }
    
    // Accessor for the property "Timestamp"
    /**
     * Setter for property Timestamp.<p>
    * Date/time value that the Member was born. This is part of the unique
    * identifier of the Member (Timestamp, Address, Port, MachineId).
    * 
    * Note: If the Member is marked as Dead, the Timestamp value indicates the
    * departure time (local clock).  In the case of zombies, the timestamp is
    * periodically updated after death as part of the panic protocol.
    * 
    * @see declareZombie
     */
    protected void setTimestamp(long cMillis)
        {
        if (cMillis <= 0)
            {
            throw new IllegalArgumentException();
            }
        
        long cMillisOld = getTimestamp();
        _assert(isDead() || isDeaf() || cMillisOld == 0 || cMillis == cMillisOld);
        
        __m_Timestamp = (cMillis);
        }
    
    // Accessor for the property "TxDebugDropCount"
    /**
     * Setter for property TxDebugDropCount.<p>
    * The number of times to artificially drop each packet sent to this member
    * before actually sending them.  Used for debugging only.
     */
    public void setTxDebugDropCount(int cPacket)
        {
        __m_TxDebugDropCount = cPacket;
        }
    
    // Accessor for the property "Uid32"
    /**
     * Setter for property Uid32.<p>
    * The unique identifier of the Member (Timestamp, Address, Port, MachineId)
    * packed into a UID. This is a new 32 byte UID that replaced the old 16
    * bytes one.
     */
    protected void setUid32(com.tangosol.util.UUID uid)
        {
        __m_Uid32 = uid;
        }
    
    // Declared at the super level
    public String toString()
        {
        return toString(SHOW_STD);
        }
    
    /**
     * Obtain a human-readable Member descrition with specified level of details
     */
    public String toString(int nShow)
        {
        // import com.tangosol.net.InetAddressHelper;
        // import java.sql.Timestamp;
        
        StringBuffer sb = new StringBuffer();
        
        sb.append("Member(Id="  ).append(getId())
          .append(", Timestamp=").append(new Timestamp(getTimestamp()))
          .append(", Address="  ).append(InetAddressHelper.toString(getAddress())).append(':').append(getPort())
          .append(", MachineId=").append(getMachineId());
        
        String sLocation = getLocationInfo();
        if (sLocation.length() > 0)
            {
            sb.append(", Location=").append(sLocation);
            }
        
        String sRole = getRoleName();
        if (sRole != null && sRole.length() > 0)
            {
            sb.append(", Role=").append(sRole);
            }
        
        if ((nShow & SHOW_LICENSE) != 0)
            {
            sb.append(", ").append(getLicenseInfo());
            }
        
        if ((nShow & SHOW_STATS) != 0)
            {
            sb.append(", ").append(formatStats());
            }
        
        sb.append(')');
        return sb.toString();
        }
    
    // From interface: com.tangosol.io.pof.PortableObject
    public void writeExternal(com.tangosol.io.pof.PofWriter out)
            throws java.io.IOException
        {
        out.writeObject(0, getUid32());
        out.writeShort(1, (short) getId());
        out.writeInt(2, getEdition());
        out.writeInt(3, getMode());
        out.writeInt(4, getCpuCount());
        out.writeInt(5, getSocketCount());
        out.writeString(6, getClusterName());
        out.writeString(7, getSiteName());
        out.writeString(8, getRackName());
        out.writeString(9, getMachineName());
        out.writeString(10, getProcessName());
        out.writeString(11, getMemberName());
        out.writeString(12, getRoleName());
        out.writeInt(13, getPriority());
        }
    
    // From interface: com.tangosol.io.ExternalizableLite
    public void writeExternal(java.io.DataOutput out)
            throws java.io.IOException
        {
        // import com.tangosol.util.ExternalizableHelper as com.tangosol.util.ExternalizableHelper;
        
        // pre-3.2
        getUid32().writeExternal(out);
        out.writeShort(getId());
        
        // version 3.2
        com.tangosol.util.ExternalizableHelper.writeInt(out, getEdition());
        com.tangosol.util.ExternalizableHelper.writeInt(out, getMode());
        com.tangosol.util.ExternalizableHelper.writeInt(out, getCpuCount());
        com.tangosol.util.ExternalizableHelper.writeInt(out, getSocketCount());
        com.tangosol.util.ExternalizableHelper.writeUTF(out, getClusterName());
        com.tangosol.util.ExternalizableHelper.writeUTF(out, getSiteName());
        com.tangosol.util.ExternalizableHelper.writeUTF(out, getRackName());
        com.tangosol.util.ExternalizableHelper.writeUTF(out, getMachineName());
        com.tangosol.util.ExternalizableHelper.writeUTF(out, getProcessName());
        com.tangosol.util.ExternalizableHelper.writeUTF(out, getMemberName());
        com.tangosol.util.ExternalizableHelper.writeUTF(out, getRoleName());
        com.tangosol.util.ExternalizableHelper.writeInt(out, getPriority());
        }
    
    // From interface: java.io.Externalizable
    public void writeExternal(java.io.ObjectOutput out)
            throws java.io.IOException
        {
        // import java.io.DataOutput;
        
        writeExternal((DataOutput) out);
        }

    // ---- class: com.tangosol.coherence.component.net.Member$FlowControl
    
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class FlowControl
            extends    com.tangosol.coherence.component.Net
        {
        // ---- Fields declarations ----
        
        /**
         * Property AggressionFactor
         *
         * The factor used when changing the outstanding packet threshold.  The
         * lower the number the larger the change.  The resulting threshold is
         * bound by the OutstandingPacketMinimum and OutstandingPacketMaximum.
         * 
         * For example a factor of 1 causes the threshold to increase/decrease
         * by 1/1 (100%).  A factor of 2 would increase/decrease by 1/2 (50%).
         * A factor of 20 would increase/decrease by 1/20 (5%).
         * 
         * Set in _initStatic()
         * 
         * This setting can be adjusted using the (undocumented)
         * tangosol.coherence.flowcontrol.aggressive system property.
         */
        private static transient int __s_AggressionFactor;
        
        /**
         * Property DeferredPacketCount
         *
         * The number of currently deferred packets for the Member.  This
         * includes packets in the Member's deferred queue, as well as packets
         * in the publisher's ready queue.
         */
        private transient int __m_DeferredPacketCount;
        
        /**
         * Property DeferredQueue
         *
         * Queue of deferred packets for this member.
         */
        private transient com.tangosol.coherence.component.util.Queue __m_DeferredQueue;
        
        /**
         * Property Enabled
         *
         * Static configuration indicating if FlowControl is enabled.
         */
        private static transient boolean __s_Enabled;
        
        /**
         * Property LostPacketThreshold
         *
         * The number of sequential packets which may be lost before declaring
         * the member paused, and starting to trickle packets.
         */
        private static transient int __s_LostPacketThreshold;
        
        /**
         * Property OutstandingPacketCount
         *
         * Tracks the number of currently outstanding packets for the Member. 
         * A packet is outstanding if it has been sent, but neither ack'd or
         * marked as lost (hit resend time).  This is only tracked when flow
         * control is enabled.
         * 
         * So this is really the number of deferrable packets for this Member
         * which are in the resend queue.
         * 
         * @volatile updated by the publisher but read from client threads
         * during drainOverflow
         */
        private volatile transient int __m_OutstandingPacketCount;
        
        /**
         * Property OutstandingPacketHighMark
         *
         * Indicates the high water mark for outstanding packets, since the
         * threshold was last increased.
         */
        private transient int __m_OutstandingPacketHighMark;
        
        /**
         * Property OutstandingPacketMaximum
         *
         * The upper bound for the number of packets which are allowed to be
         * outstanding (unack'd) before packet to this Member begin to be
         * deferred.
         * 
         * If the upper and lower bounds are not equal then TCMP will vary the
         * allowable number based on how successful the local node is at
         * pushing packets to this Member.
         */
        private static transient int __s_OutstandingPacketMaximum;
        
        /**
         * Property OutstandingPacketMinimum
         *
         * TCMP will vary the allowable number of outstanding packets
         * (threshold), between the max and min, based on how successful the
         * local node is at pushing packets to this Member.
         */
        private static transient int __s_OutstandingPacketMinimum;
        
        /**
         * Property OutstandingPacketThreshold
         *
         * The number of packets which are allowed to be outstanding (unack'd)
         * before packets to this memeber begin to be deferred.
         */
        private transient int __m_OutstandingPacketThreshold;
        
        /**
         * Property PacketThreshold
         *
         * The value of the tangosol.coherence.flowcontrol.threshold system
         * property, which is used to initialize the outstanding packet
         * threshold.
         * -1 if not explicitly specified.
         */
        private static transient int __s_PacketThreshold;
        
        /**
         * Property Paused
         *
         * Indicates if this Member is paused, i.e. temporarily not ack'ing,
         * only tracked when flow control is enabled.
         */
        private transient boolean __m_Paused;
        
        /**
         * Property PauseStartMillis
         *
         * The start of the Pause, see Paused.
         */
        private transient long __m_PauseStartMillis;
        
        /**
         * Property SequentialConfirmedCount
         *
         * Count of the number of sequentially ack'd packets, only tracked when
         * flow control is enabled.
         */
        private transient int __m_SequentialConfirmedCount;
        
        /**
         * Property SequentialLostCount
         *
         * Count of the number of sequentially lost packets, only tracked when
         * flow control is enabled.
         */
        private transient int __m_SequentialLostCount;
        
        /**
         * Property StatsPausedMillis
         *
         * The total amount of time this member has spent in the paused state
         * since the last stats reset.
         */
        private transient long __m_StatsPausedMillis;
        
        /**
         * Property SuccessGoal
         *
         * The target publisher success rate.  While the rate remains above the
         * goal, flow control will not automatically lower the threshold.
         * 
         * The goal is specified as the number of confirmations which must
         * occur between losses.  E.g. a value of 100 would mean that resending
         * 1 out of 100 packets is considered acceptable.
         * 
         * The auto adjustment based on this rate is rather course grained as
         * it must deal with chunks of acks and losses.
         * 
         * Set in _initStatic()
         * 
         * This setting can be adjusted using the (undocumented)
         * tangosol.coherence.flowcontrol.goal system property.
         */
        private static transient int __s_SuccessGoal;
        
        private static void _initStatic$Default()
            {
            __initStatic();
            }
        
        // Static initializer (from _initStatic)
        static
            {
            // import com.tangosol.coherence.config.Config;
            
            _initStatic$Default();
            
            try
                {
                String sGoal       = Config.getProperty("coherence.flowcontrol.goal");
                String sAggression = Config.getProperty("coherence.flowcontrol.aggressive");
                String sThreshold  = Config.getProperty("coherence.flowcontrol.threshold");
               
                if (sGoal != null)
                    {
                    setSuccessGoal(Integer.parseInt(sGoal));
                    }
            
                if (sAggression != null)
                    {
                    setAggressionFactor(Integer.parseInt(sAggression));
                    }
            
                if (sThreshold != null)
                    {
                    setPacketThreshold(Integer.parseInt(sThreshold));
                    }
                }
            catch (Exception e) {}
            }
        
        // Default static initializer
        private static void __initStatic()
            {
            // state initialization: static properties
            try
                {
                setAggressionFactor(20);
                __s_PacketThreshold = -1;
                setSuccessGoal(200);
                }
            catch (java.lang.Exception e)
                {
                // re-throw as a runtime exception
                throw new com.tangosol.util.WrapperException(e);
                }
            }
        
        // Default constructor
        public FlowControl()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public FlowControl(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            _addChild(new Member.FlowControl.DeferredQueue("DeferredQueue", this, true), "DeferredQueue");
            
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
            return new com.tangosol.coherence.component.net.Member.FlowControl();
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
                clz = Class.forName("com.tangosol.coherence/component/net/Member$FlowControl".replace('/', '.'));
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
         * Return flow control statistics for communication between the local
        * Member and the parent Member. 
         */
        public String formatStats(boolean fVerbose)
            {
            double dPaused = getStatsPauseRate();
            
            // round rates
            dPaused = ((int) (dPaused * 10000.0)) / 10000.0;
            
            String sStats = "PauseRate="   + dPaused +
                            ", Threshold=" + getOutstandingPacketThreshold();
            
            if (fVerbose)
                {
                int cDeferred = getDeferredQueue().size();
                int cReady    = getDeferredPacketCount() - cDeferred;
                
                sStats += ", Paused="             + isPaused() +
                          ", Deferring="          + isDeferring() +
                          ", OutstandingPackets=" + getOutstandingPacketCount() +               
                          ", DeferredPackets="    + cDeferred +
                          ", ReadyPackets="       + cReady;
                }
            
            return sStats;
            }
        
        // Accessor for the property "AggressionFactor"
        /**
         * Getter for property AggressionFactor.<p>
        * The factor used when changing the outstanding packet threshold.  The
        * lower the number the larger the change.  The resulting threshold is
        * bound by the OutstandingPacketMinimum and OutstandingPacketMaximum.
        * 
        * For example a factor of 1 causes the threshold to increase/decrease
        * by 1/1 (100%).  A factor of 2 would increase/decrease by 1/2 (50%). A
        * factor of 20 would increase/decrease by 1/20 (5%).
        * 
        * Set in _initStatic()
        * 
        * This setting can be adjusted using the (undocumented)
        * tangosol.coherence.flowcontrol.aggressive system property.
         */
        public static int getAggressionFactor()
            {
            return __s_AggressionFactor;
            }
        
        // Accessor for the property "DeferredPacketCount"
        /**
         * Getter for property DeferredPacketCount.<p>
        * The number of currently deferred packets for the Member.  This
        * includes packets in the Member's deferred queue, as well as packets
        * in the publisher's ready queue.
         */
        public int getDeferredPacketCount()
            {
            return __m_DeferredPacketCount;
            }
        
        // Accessor for the property "DeferredQueue"
        /**
         * Getter for property DeferredQueue.<p>
        * Queue of deferred packets for this member.
         */
        public com.tangosol.coherence.component.util.Queue getDeferredQueue()
            {
            return __m_DeferredQueue;
            }
        
        // Accessor for the property "LostPacketThreshold"
        /**
         * Getter for property LostPacketThreshold.<p>
        * The number of sequential packets which may be lost before declaring
        * the member paused, and starting to trickle packets.
         */
        public static int getLostPacketThreshold()
            {
            return __s_LostPacketThreshold;
            }
        
        // Accessor for the property "Member"
        /**
         * Getter for property Member.<p>
        * The Member this FlowControl object belongs to.
         */
        public Member getMember()
            {
            return (Member) get_Parent();
            }
        
        // Accessor for the property "OutstandingPacketCount"
        /**
         * Getter for property OutstandingPacketCount.<p>
        * Tracks the number of currently outstanding packets for the Member.  A
        * packet is outstanding if it has been sent, but neither ack'd or
        * marked as lost (hit resend time).  This is only tracked when flow
        * control is enabled.
        * 
        * So this is really the number of deferrable packets for this Member
        * which are in the resend queue.
        * 
        * @volatile updated by the publisher but read from client threads
        * during drainOverflow
         */
        public int getOutstandingPacketCount()
            {
            return __m_OutstandingPacketCount;
            }
        
        // Accessor for the property "OutstandingPacketHighMark"
        /**
         * Getter for property OutstandingPacketHighMark.<p>
        * Indicates the high water mark for outstanding packets, since the
        * threshold was last increased.
         */
        public int getOutstandingPacketHighMark()
            {
            return __m_OutstandingPacketHighMark;
            }
        
        // Accessor for the property "OutstandingPacketMaximum"
        /**
         * Getter for property OutstandingPacketMaximum.<p>
        * The upper bound for the number of packets which are allowed to be
        * outstanding (unack'd) before packet to this Member begin to be
        * deferred.
        * 
        * If the upper and lower bounds are not equal then TCMP will vary the
        * allowable number based on how successful the local node is at pushing
        * packets to this Member.
         */
        public static int getOutstandingPacketMaximum()
            {
            return __s_OutstandingPacketMaximum;
            }
        
        // Accessor for the property "OutstandingPacketMinimum"
        /**
         * Getter for property OutstandingPacketMinimum.<p>
        * TCMP will vary the allowable number of outstanding packets
        * (threshold), between the max and min, based on how successful the
        * local node is at pushing packets to this Member.
         */
        public static int getOutstandingPacketMinimum()
            {
            return __s_OutstandingPacketMinimum;
            }
        
        // Accessor for the property "OutstandingPacketThreshold"
        /**
         * Getter for property OutstandingPacketThreshold.<p>
        * The number of packets which are allowed to be outstanding (unack'd)
        * before packets to this memeber begin to be deferred.
         */
        public int getOutstandingPacketThreshold()
            {
            return __m_OutstandingPacketThreshold;
            }
        
        // Accessor for the property "PacketThreshold"
        /**
         * Getter for property PacketThreshold.<p>
        * The value of the tangosol.coherence.flowcontrol.threshold system
        * property, which is used to initialize the outstanding packet
        * threshold.
        * -1 if not explicitly specified.
         */
        private static int getPacketThreshold()
            {
            return __s_PacketThreshold;
            }
        
        // Accessor for the property "PauseStartMillis"
        /**
         * Getter for property PauseStartMillis.<p>
        * The start of the Pause, see Paused.
         */
        public long getPauseStartMillis()
            {
            return __m_PauseStartMillis;
            }
        
        // Accessor for the property "PendingPacketCount"
        /**
         * Getter for property PendingPacketCount.<p>
        * The number of packets this member is contributing to the traffic jam.
        *  This is only a count of the deferrable (ConfirmationRequired &
        * unipoint) packets.  Used for traffic jam control.
        * 
        * @see Publisher.drainOverflow
         */
        public int getPendingPacketCount()
            {
            return getDeferredQueue().size() + getOutstandingPacketCount();
            }
        
        // Accessor for the property "SequentialConfirmedCount"
        /**
         * Getter for property SequentialConfirmedCount.<p>
        * Count of the number of sequentially ack'd packets, only tracked when
        * flow control is enabled.
         */
        public int getSequentialConfirmedCount()
            {
            return __m_SequentialConfirmedCount;
            }
        
        // Accessor for the property "SequentialLostCount"
        /**
         * Getter for property SequentialLostCount.<p>
        * Count of the number of sequentially lost packets, only tracked when
        * flow control is enabled.
         */
        public int getSequentialLostCount()
            {
            return __m_SequentialLostCount;
            }
        
        // Accessor for the property "StatsPausedMillis"
        /**
         * Getter for property StatsPausedMillis.<p>
        * The total amount of time this member has spent in the paused state
        * since the last stats reset.
         */
        public long getStatsPausedMillis()
            {
            // import com.tangosol.util.Base;
            
            long lMillis = __m_StatsPausedMillis;
            
            if (isPaused())
                {
                lMillis += Base.getSafeTimeMillis() - getPauseStartMillis();
                }
            
            return lMillis;
            }
        
        // Accessor for the property "StatsPauseRate"
        /**
         * Getter for property StatsPauseRate.<p>
        * The percentage of time this member has been in the paused state since
        * the stats were last reset.
         */
        public double getStatsPauseRate()
            {
            // import com.tangosol.util.Base;
            
            long   ldtNow        = Base.getSafeTimeMillis();
            long   cPausedMillis = getStatsPausedMillis();
            
            return cPausedMillis == 0L ? 0.0 : ((double) cPausedMillis) /
                    ((double) (ldtNow - getMember().getStatsReset()));
            }
        
        // Accessor for the property "SuccessGoal"
        /**
         * Getter for property SuccessGoal.<p>
        * The target publisher success rate.  While the rate remains above the
        * goal, flow control will not automatically lower the threshold.
        * 
        * The goal is specified as the number of confirmations which must occur
        * between losses.  E.g. a value of 100 would mean that resending 1 out
        * of 100 packets is considered acceptable.
        * 
        * The auto adjustment based on this rate is rather course grained as it
        * must deal with chunks of acks and losses.
        * 
        * Set in _initStatic()
        * 
        * This setting can be adjusted using the (undocumented)
        * tangosol.coherence.flowcontrol.goal system property.
         */
        public static int getSuccessGoal()
            {
            return __s_SuccessGoal;
            }
        
        // Accessor for the property "Adjustable"
        /**
         * Getter for property Adjustable.<p>
        * Indicate if this member's flow control threshold may be dynamically
        * adjusted.
         */
        public boolean isAdjustable()
            {
            return getOutstandingPacketMaximum() != getOutstandingPacketMinimum();
            }
        
        // Accessor for the property "Deferring"
        /**
         * Getter for property Deferring.<p>
        * Indicate if packet to this member are currently being deferred.
         */
        public boolean isDeferring()
            {
            return getDeferredPacketCount() > 0;
            }
        
        // Accessor for the property "Enabled"
        /**
         * Getter for property Enabled.<p>
        * Static configuration indicating if FlowControl is enabled.
         */
        public static boolean isEnabled()
            {
            return __s_Enabled;
            }
        
        // Accessor for the property "Paused"
        /**
         * Getter for property Paused.<p>
        * Indicates if this Member is paused, i.e. temporarily not ack'ing,
        * only tracked when flow control is enabled.
         */
        public boolean isPaused()
            {
            return __m_Paused;
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
            int cThreshold = getPacketThreshold();
            if (cThreshold < 0)
                {
                // choose a starting threshold at the half way point
                setOutstandingPacketThreshold(
                    (getOutstandingPacketMaximum() + getOutstandingPacketMinimum())/2);
                }
            else
                {
                // use user specified starting point
                setOutstandingPacketThreshold(cThreshold);
                }
                
            setDeferredQueue((Member.FlowControl.DeferredQueue) _findChild("DeferredQueue"));
            
            super.onInit();
            }
        
        /**
         * Called by the publisher when it detects the start of a pause.
         */
        public void onPauseStart()
            {
            // import com.tangosol.util.Base;
            
            setPauseStartMillis(Base.getSafeTimeMillis());
            
            if (_isTraceEnabled(9))
                {
                _trace(getMember() + " has failed to respond to " +
                       getSequentialLostCount() +
                       " packets; declaring this member as paused.", 9);
                }
            }
        
        /**
         * Called by the publisher when it detects the end of a pause.
         */
        public void onPauseStop()
            {
            // import com.tangosol.util.Base;
            
            long ldtStart  = getPauseStartMillis();
            long lDelta    = Base.getSafeTimeMillis() - ldtStart;
            int  iLogLevel = lDelta > 1000L ? 2
                           : lDelta > 100L  ? 6
                           : lDelta > 10L   ? 8
                           : 9;
            
            if (lDelta > 0)
                {
                setStatsPausedMillis(getStatsPausedMillis() + lDelta);
            
                //if (lDelta > 1000L && TracingHelper.isEnabled())
                //    {
                //    TracingHelper.newSpan("commdelay")
                //                .withMetadata(SpanType.COMPONENT.key(), "transport")
                //                .withMetadata("layer", "datagram")
                //                .withMetadata("member.destination", Long.valueOf(getMember().getId()).longValue())
                //                .setStartTimestamp((System.currentTimeMillis() - lDelta) * 1000L)
                //                .startSpan().end();
                //    }
                // else; don't bother to trace shorter
                }
            
            if (_isTraceEnabled(iLogLevel))
                {
                _trace("Experienced a " + lDelta + " ms communication delay (probable remote GC) with "
                     + (iLogLevel == 2 ? getMember().toString() : "member " + getMember().getId())
                     + "; " + getSequentialLostCount() + " packets rescheduled, "
                     + formatStats(false), iLogLevel);
                }
            }
        
        /**
         * Reset the statistics.
         */
        public void resetStats()
            {
            setStatsPausedMillis(0L);
            }
        
        // Accessor for the property "AggressionFactor"
        /**
         * Setter for property AggressionFactor.<p>
        * The factor used when changing the outstanding packet threshold.  The
        * lower the number the larger the change.  The resulting threshold is
        * bound by the OutstandingPacketMinimum and OutstandingPacketMaximum.
        * 
        * For example a factor of 1 causes the threshold to increase/decrease
        * by 1/1 (100%).  A factor of 2 would increase/decrease by 1/2 (50%). A
        * factor of 20 would increase/decrease by 1/20 (5%).
        * 
        * Set in _initStatic()
        * 
        * This setting can be adjusted using the (undocumented)
        * tangosol.coherence.flowcontrol.aggressive system property.
         */
        public static void setAggressionFactor(int iFactor)
            {
            __s_AggressionFactor = (Math.max(1, iFactor));
            }
        
        // Accessor for the property "DeferredPacketCount"
        /**
         * Setter for property DeferredPacketCount.<p>
        * The number of currently deferred packets for the Member.  This
        * includes packets in the Member's deferred queue, as well as packets
        * in the publisher's ready queue.
         */
        public void setDeferredPacketCount(int cPackets)
            {
            if (cPackets < 0)
                {
                throw new IllegalStateException("DeferredPacketCount for " + getMember() + " cannot be negative");
                }
            __m_DeferredPacketCount = (cPackets);
            }
        
        // Accessor for the property "DeferredQueue"
        /**
         * Setter for property DeferredQueue.<p>
        * Queue of deferred packets for this member.
         */
        public void setDeferredQueue(com.tangosol.coherence.component.util.Queue queue)
            {
            __m_DeferredQueue = queue;
            }
        
        // Accessor for the property "Enabled"
        /**
         * Setter for property Enabled.<p>
        * Static configuration indicating if FlowControl is enabled.
         */
        public static void setEnabled(boolean fEnabled)
            {
            __s_Enabled = fEnabled;
            }
        
        // Accessor for the property "LostPacketThreshold"
        /**
         * Setter for property LostPacketThreshold.<p>
        * The number of sequential packets which may be lost before declaring
        * the member paused, and starting to trickle packets.
         */
        public static void setLostPacketThreshold(int nThreshold)
            {
            __s_LostPacketThreshold = nThreshold;
            }
        
        // Accessor for the property "OutstandingPacketCount"
        /**
         * Setter for property OutstandingPacketCount.<p>
        * Tracks the number of currently outstanding packets for the Member.  A
        * packet is outstanding if it has been sent, but neither ack'd or
        * marked as lost (hit resend time).  This is only tracked when flow
        * control is enabled.
        * 
        * So this is really the number of deferrable packets for this Member
        * which are in the resend queue.
        * 
        * @volatile updated by the publisher but read from client threads
        * during drainOverflow
         */
        public void setOutstandingPacketCount(int cPackets)
            {
            if (cPackets < 0)
                {
                throw new IllegalStateException("OutstandingPacketCount for " + getMember() + " cannot be negative");
                }
            if (cPackets > getOutstandingPacketHighMark())
                {
                setOutstandingPacketHighMark(cPackets);
                }
            __m_OutstandingPacketCount = (cPackets);
            }
        
        // Accessor for the property "OutstandingPacketHighMark"
        /**
         * Setter for property OutstandingPacketHighMark.<p>
        * Indicates the high water mark for outstanding packets, since the
        * threshold was last increased.
         */
        public void setOutstandingPacketHighMark(int cPackets)
            {
            __m_OutstandingPacketHighMark = cPackets;
            }
        
        // Accessor for the property "OutstandingPacketMaximum"
        /**
         * Setter for property OutstandingPacketMaximum.<p>
        * The upper bound for the number of packets which are allowed to be
        * outstanding (unack'd) before packet to this Member begin to be
        * deferred.
        * 
        * If the upper and lower bounds are not equal then TCMP will vary the
        * allowable number based on how successful the local node is at pushing
        * packets to this Member.
         */
        public static void setOutstandingPacketMaximum(int cPackets)
            {
            __s_OutstandingPacketMaximum = cPackets;
            }
        
        // Accessor for the property "OutstandingPacketMinimum"
        /**
         * Setter for property OutstandingPacketMinimum.<p>
        * TCMP will vary the allowable number of outstanding packets
        * (threshold), between the max and min, based on how successful the
        * local node is at pushing packets to this Member.
         */
        public static void setOutstandingPacketMinimum(int cPackets)
            {
            __s_OutstandingPacketMinimum = cPackets;
            }
        
        // Accessor for the property "OutstandingPacketThreshold"
        /**
         * Setter for property OutstandingPacketThreshold.<p>
        * The number of packets which are allowed to be outstanding (unack'd)
        * before packets to this memeber begin to be deferred.
         */
        public void setOutstandingPacketThreshold(int cPackets)
            {
            // PacketMinimum <= cPackets <= PacketMaximum
            cPackets = Math.max(getOutstandingPacketMinimum(),
                         Math.min(cPackets,
                            getOutstandingPacketMaximum()));
            __m_OutstandingPacketThreshold = (cPackets);
            }
        
        // Accessor for the property "PacketThreshold"
        /**
         * Setter for property PacketThreshold.<p>
        * The value of the tangosol.coherence.flowcontrol.threshold system
        * property, which is used to initialize the outstanding packet
        * threshold.
        * -1 if not explicitly specified.
         */
        private static void setPacketThreshold(int cPacketThreshold)
            {
            __s_PacketThreshold = cPacketThreshold;
            }
        
        // Accessor for the property "Paused"
        /**
         * Setter for property Paused.<p>
        * Indicates if this Member is paused, i.e. temporarily not ack'ing,
        * only tracked when flow control is enabled.
         */
        public void setPaused(boolean fPaused)
            {
            if (fPaused == isPaused())
                {
                // no change
                return;
                }
            
            __m_Paused = (fPaused);
            
            if (fPaused)
                {
                onPauseStart();
                }
            else
                {
                onPauseStop();
                }
            }
        
        // Accessor for the property "PauseStartMillis"
        /**
         * Setter for property PauseStartMillis.<p>
        * The start of the Pause, see Paused.
         */
        public void setPauseStartMillis(long ldtPauseStartMillis)
            {
            __m_PauseStartMillis = ldtPauseStartMillis;
            }
        
        // Accessor for the property "SequentialConfirmedCount"
        /**
         * Setter for property SequentialConfirmedCount.<p>
        * Count of the number of sequentially ack'd packets, only tracked when
        * flow control is enabled.
         */
        public void setSequentialConfirmedCount(int cPackets)
            {
            __m_SequentialConfirmedCount = cPackets;
            }
        
        // Accessor for the property "SequentialLostCount"
        /**
         * Setter for property SequentialLostCount.<p>
        * Count of the number of sequentially lost packets, only tracked when
        * flow control is enabled.
         */
        public void setSequentialLostCount(int cPackets)
            {
            __m_SequentialLostCount = cPackets;
            }
        
        // Accessor for the property "StatsPausedMillis"
        /**
         * Setter for property StatsPausedMillis.<p>
        * The total amount of time this member has spent in the paused state
        * since the last stats reset.
         */
        public void setStatsPausedMillis(long cMillis)
            {
            __m_StatsPausedMillis = cMillis;
            }
        
        // Accessor for the property "SuccessGoal"
        /**
         * Setter for property SuccessGoal.<p>
        * The target publisher success rate.  While the rate remains above the
        * goal, flow control will not automatically lower the threshold.
        * 
        * The goal is specified as the number of confirmations which must occur
        * between losses.  E.g. a value of 100 would mean that resending 1 out
        * of 100 packets is considered acceptable.
        * 
        * The auto adjustment based on this rate is rather course grained as it
        * must deal with chunks of acks and losses.
        * 
        * Set in _initStatic()
        * 
        * This setting can be adjusted using the (undocumented)
        * tangosol.coherence.flowcontrol.goal system property.
         */
        public static void setSuccessGoal(int iGoal)
            {
            __s_SuccessGoal = (Math.max(1, iGoal));
            }
        
        // Declared at the super level
        public String toString()
            {
            return formatStats(true);
            }

        // ---- class: com.tangosol.coherence.component.net.Member$FlowControl$DeferredQueue
        
        /**
         * The Queue provides a means to efficiently (and in a thread-safe
         * manner) queue received messages and messages to be sent.
         * 
         * The PriorityQueue will choose a new items position in the queue
         * based on its priority.  Priorities are assumed to be fixed, and
         * should not change while the element remains in the queue.
         */
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class DeferredQueue
                extends    com.tangosol.coherence.component.util.queue.PriorityQueue
                implements java.util.Comparator
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
                __mapChildren.put("Iterator", Member.FlowControl.DeferredQueue.Iterator.get_CLASS());
                }
            
            // Default constructor
            public DeferredQueue()
                {
                this(null, null, true);
                }
            
            // Initializing constructor
            public DeferredQueue(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                    setSortedElementSet(new java.util.TreeSet());
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
                return new com.tangosol.coherence.component.net.Member.FlowControl.DeferredQueue();
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
                    clz = Class.forName("com.tangosol.coherence/component/net/Member$FlowControl$DeferredQueue".replace('/', '.'));
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
             * Appends the specified element to the end of this queue.
            * 
            * Queues may place limitations on what elements may be added to
            * this Queue.  In particular, some Queues will impose restrictions
            * on the type of elements that may be added. Queue implementations
            * should clearly specify in their documentation any restrictions on
            * what elements may be added.
            * 
            * @param oElement element to be appended to this Queue
            * 
            * @return true (as per the general contract of the Collection.add
            * method)
            * 
            * @throws ClassCastException if the class of the specified element
            * prevents it from being added to this Queue
             */
            public synchronized boolean add(Object oElement)
                {
                // import Component.Net.Packet.MessagePacket;
                
                MessagePacket packet = (MessagePacket) oElement;
                if (!super.add(packet) &&
                    packet.getPendingResendSkips() > 0)
                    {
                    // add method returned false (set already contained the packet)
                    // if the packet is set to do resend skips and we already hold
                    // a reference to the packet we must eat any added skip as our
                    // set will obviously not hold onto the multiple references
                    packet.setPendingResendSkips(packet.getPendingResendSkips() - 1);
                    }
                
                return getSortedElementSet().last() == packet;
                }
            
            // From interface: java.util.Comparator
            public int compare(Object o1, Object o2)
                {
                // import Component.Net.Packet.MessagePacket;
                // import com.tangosol.net.internal.PacketComparator;
                
                return PacketComparator.compare((MessagePacket) o1, (MessagePacket) o2);
                }
            
            // Declared at the super level
            /**
             * The "component has been initialized" method-notification called
            * out of setConstructed() for the topmost component and that in
            * turn notifies all the children.
            * 
            * This notification gets called before the control returns back to
            * this component instantiator (using <code>new Component.X()</code>
            * or <code>_newInstance(sName)</code>) and on the same thread. In
            * addition, visual components have a "posted" notification
            * <code>onInitUI</code> that is called after (or at the same time
            * as) the control returns back to the instantiator and possibly on
            * a different thread.
             */
            public void onInit()
                {
                super.onInit();
                
                setComparator(this);
                }

            // ---- class: com.tangosol.coherence.component.net.Member$FlowControl$DeferredQueue$Iterator
            
            /**
             * Iterator of a snapshot of the List object that backs the Queue.
             * Supports remove(). Uses the Queue as the monitor if any
             * synchronization is required.
             */
            @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
            public static class Iterator
                    extends    com.tangosol.coherence.component.util.Queue.Iterator
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
                    return new com.tangosol.coherence.component.net.Member.FlowControl.DeferredQueue.Iterator();
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
                        clz = Class.forName("com.tangosol.coherence/component/net/Member$FlowControl$DeferredQueue$Iterator".replace('/', '.'));
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
        }

    // ---- class: com.tangosol.coherence.component.net.Member$SentQueue
    
    /**
     * A Queue of recently sent packets.  This Queue is only to be accessed on
     * the publisher thread.
     *         
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class SentQueue
            extends    com.tangosol.coherence.component.util.queue.OptimisticQueue
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
            __mapChildren.put("Iterator", Member.SentQueue.Iterator.get_CLASS());
            }
        
        // Default constructor
        public SentQueue()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public SentQueue(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.net.Member.SentQueue();
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
                clz = Class.forName("com.tangosol.coherence/component/net/Member$SentQueue".replace('/', '.'));
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
        
        /**
         * Remove the specified element from the queue.  This may be a costly
        * operation if the element is not near the front of the queue.  Note if
        * the element is stored more then once in the queue only the first
        * occrance will be removed.
        * 
        * @return true if the element was removed.
         */
        public boolean remove(Object oElement)
            {
            return getElementList().remove(oElement);
            }

        // ---- class: com.tangosol.coherence.component.net.Member$SentQueue$Iterator
        
        /**
         * Iterator of a snapshot of the List object that backs the Queue.
         * Supports remove(). Uses the Queue as the monitor if any
         * synchronization is required.
         */
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class Iterator
                extends    com.tangosol.coherence.component.util.Queue.Iterator
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
                return new com.tangosol.coherence.component.net.Member.SentQueue.Iterator();
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
                    clz = Class.forName("com.tangosol.coherence/component/net/Member$SentQueue$Iterator".replace('/', '.'));
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
