
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketPublisher

package com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor;

import com.tangosol.coherence.component.net.Member;
import com.tangosol.coherence.component.net.MemberSet;
import com.tangosol.coherence.component.net.Message;
import com.tangosol.coherence.component.net.Packet;
import com.tangosol.coherence.component.net.PacketBundle;
import com.tangosol.coherence.component.net.memberSet.ActualMemberSet;
import com.tangosol.coherence.component.net.memberSet.DependentMemberSet;
import com.tangosol.coherence.component.net.memberSet.SingleMemberSet;
import com.tangosol.coherence.component.net.packet.DiagnosticPacket;
import com.tangosol.coherence.component.net.packet.MessagePacket;
import com.tangosol.coherence.component.net.packet.messagePacket.Broadcast;
import com.tangosol.coherence.component.net.packet.messagePacket.Directed;
import com.tangosol.coherence.component.net.packet.notifyPacket.Ack;
import com.tangosol.coherence.component.net.socket.UdpSocket;
import com.tangosol.coherence.component.util.WindowedArray;
import com.oracle.coherence.common.base.Blocking;
import com.oracle.coherence.common.base.Disposable;
import com.oracle.coherence.common.io.BufferSequence;
import com.tangosol.coherence.config.Config;
import com.tangosol.io.MultiBufferReadBuffer;
import com.tangosol.io.MultiBufferWriteBuffer;
import com.tangosol.io.ReadBuffer;
import com.tangosol.io.nio.ByteBufferReadBuffer;
import com.tangosol.net.RequestTimeoutException;
import com.tangosol.net.internal.PacketComparator;
import com.tangosol.net.internal.PacketIdentifier;
import com.tangosol.util.Base;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This is a Daemon component that waits for items to process from a Queue.
 * Whenever the Queue contains an item, the onNotify event occurs. It is
 * expected that sub-classes will process onNotify as follows:
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
 * 
 * <br>
 * A client of PacketProcessor must configure:<br>
 * <br><ul>
 * <li>MemberSet property</li>
 * </ul><br>
 * A client of PacketDispatcher may configure:<br>
 * <br><ul>
 * <li>Priority property</li>
 * <li>ThreadGroup property</li>
 * </ul><br>
 * See the associated documentation for each.<br>
 * <br>
 * Once the PacketProcessor is configured, the client can start the processor
 * using the start() method.<br>
 * 
 * 
 * A client of PacketPublisher must additionally configure:
 * <ul>
 * <li>DatagramSocket property</li>
 * <li>MulticastAddress property</li>
 * <li>MulticastSocket property</li>
 * <li>MessageOutgoing property</li>
 * </ul>
 * A client of PacketPublisher may configure:
 * <ul>
 * <li>ResendDelay property</li>
 * <li>ResendTimeout property</li>
 * <li>AckDelay property</li>
 * <li>RequestDelay property</li>
 * </ul>
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class PacketPublisher
        extends    com.tangosol.coherence.component.util.daemon.queueProcessor.PacketProcessor
        implements com.tangosol.internal.util.MessagePublisher
    {
    // ---- Fields declarations ----
    
    /**
     * Property AckDelay
     *
     * The minimum number of milliseconds between the queueing of an Ack packet
     * and the sending of the same.
     */
    private int __m_AckDelay;
    
    /**
     * Property AckdPacketListTemp
     *
     * Used in the processing of Acks to avoid frequent creation of a temporary
     * list.
     * 
     * @see #registerAcks
     * @see #doEarlyLossDetection
     */
    private transient java.util.List __m_AckdPacketListTemp;
    
    /**
     * Property AckQueue
     *
     * A reference to the AckQueue child, which is the Queue in which
     * Packet-notification Packets are placed so that they are automatically
     * after a configurable period of time.
     */
    private PacketPublisher.AckQueue __m_AckQueue;
    
    /**
     * Property BroadcastAddresses
     *
     * A Set<InetSocketAddress> used for broadcast in leu of multicast. This
     * list always contains pre-configured set of WellKnownAddresses, but could
     * additionally contain dynamically obtained addresses. This set can be
     * modified by the ClusterService.
     */
    private java.util.Set __m_BroadcastAddresses;
    
    /**
     * Property CloggedCount
     *
     * The maximum number of packets in the send plus resend queues before
     * determining that the publisher is clogged. Zero means no limit.
     */
    private int __m_CloggedCount;
    
    /**
     * Property CloggedDelay
     *
     * The number of milliseconds to pause client threads when a clog occurs,
     * to wait for the clog to dissipate. (The pause is repeated until the clog
     * is gone.) Anything less than one (e.g. zero) is treated as one.
     */
    private int __m_CloggedDelay;
    
    /**
     * Property ClusterNameBuffer
     *
     * This cluster's name length encoded into a ByteBuffer just as is used in
     * Broadcast packets.
     */
    private transient java.nio.ByteBuffer __m_ClusterNameBuffer;
    
    /**
     * Property ConfirmationQueue
     *
     * A reference to the ConfirmationQueue child, which is the Queue in which
     * incomming Ack packets from other members are placed.
     */
    private PacketPublisher.ConfirmationQueue __m_ConfirmationQueue;
    
    /**
     * Property DeferredReadyQueue
     *
     * A queue of previously deferred packets which are ready to be sent.  This
     * value will be null if deferral is not enabled.
     * 
     * @see Member#DeferredQueue
     */
    private com.tangosol.coherence.component.util.Queue __m_DeferredReadyQueue;
    
    /**
     * Property FromMessageId
     *
     * "allocator" for from message ids
     */
    private long __m_FromMessageId;
    
    /**
     * Property IncomingPacketQueues
     *
     * The set of queues responsible for supplying the publisher with packets.
     */
    private transient com.tangosol.coherence.component.util.Queue[] __m_IncomingPacketQueues;
    
    /**
     * Property LastTrafficJamWarningTimeMillis
     *
     * Timestamp of the last time a warning was issued due to overly agressive
     * traffic jam settings.
     * 
     * @volatile, accessed by all client threads
     */
    private volatile long __m_LastTrafficJamWarningTimeMillis;
    
    /**
     * Property LostPacketListTemp
     *
     * Used in the processing of Acks to avoid frequent creation of a temporary
     * list.
     * 
     * @see #doEarlyLossDetection
     */
    private transient java.util.List __m_LostPacketListTemp;
    
    /**
     * Property MaximumPacketLength
     *
     * The minimum safe packet length required to be supported by all cluster
     * members.
     */
    private int __m_MaximumPacketLength;
    
    /**
     * Property MessageBufferAllocator
     *
     * The pool of buffers used when serialize a Message.
     */
    private transient com.tangosol.io.MultiBufferWriteBuffer.WriteBufferPool __m_MessageBufferAllocator;
    
    /**
     * Property MessageOutgoing
     *
     * The array of outgoing Message objects.
     */
    private com.tangosol.coherence.component.util.WindowedArray __m_MessageOutgoing;
    
    /**
     * Property MsgArrayTemp
     *
     * Used in the processing of Acks to avoid frequent creation of a temporary
     * array.
     * 
     * @see #onPacketAck
     * @see #ensureMsgArrayTemp
     */
    private transient com.tangosol.coherence.component.net.Message[] __m_MsgArrayTemp;
    
    /**
     * Property MsgIdArrayTemp
     *
     * Used in the processing of Acks to avoid frequent creation of a temporary
     * array.
     * 
     * @see #onPacketAck
     * @see #ensureMsgIdArrayTemp
     */
    private transient long[] __m_MsgIdArrayTemp;
    
    /**
     * Property MulticastAddress
     *
     * The cluster's mulitcast address, if enabled
     */
    private java.net.InetSocketAddress __m_MulticastAddress;
    
    /**
     * Property MulticastBypassCount
     *
     * A count of the number of future multicast transmissions which should be
     * skipped (turned into unicast).  This is set when it appears that
     * multicast may be flaky.
     */
    private transient int __m_MulticastBypassCount;
    
    /**
     * Property MulticastEnabled
     *
     * Specifies whether or not the multicast is enabled.
     */
    private boolean __m_MulticastEnabled;
    
    /**
     * Property MulticastThreshold
     *
     * The percentage (0.0 to 100.0) of the servers in the cluster that a
     * packet will be sent to, above which the packet will be multicasted and
     * below which it will be unicasted.
     */
    private double __m_MulticastThreshold;
    
    /**
     * Property NackDelayMillis
     *
     * The number of milliseconds to delay the sending of a NACK after
     * detecting probable packet loss.
     */
    private transient long __m_NackDelayMillis;
    
    /**
     * Property NackEnabled
     *
     * Specifies if the receiver should use request packets, i.e. NACKs to do
     * early packet loss detection.
     */
    private transient boolean __m_NackEnabled;
    
    /**
     * Property PacketAdapterQueue
     *
     * The InQueue's PacketAdpater
     */
    private transient com.tangosol.coherence.component.util.Queue __m_PacketAdapterQueue;
    
    /**
     * Property PreferredPacketLength
     *
     * The "preferred" (optimal) length of each outgoing Packet.
     * 
     * @see ClusterConfig
     */
    private int __m_PreferredPacketLength;
    
    /**
     * Property ResendDelay
     *
     * The minimum number of milliseconds that a Packet will remain queued in
     * the ResendQueue before being removed from the front of the Queue to be
     * resent.
     * 
     * (2-way calculated; do not design.)
     */
    private int __m_ResendDelay;
    
    /**
     * Property ResendQueue
     *
     * A reference to the ResendQueue child, which is the Queue in which
     * guaranteed-delivery Packets are placed so that they are automatically
     * resent.
     */
    private PacketPublisher.ResendQueue __m_ResendQueue;
    
    /**
     * Property ResendTimeout
     *
     * The maximum number of milliseconds that a Packet will be resent before
     * it is assumed that the remaining unacknowledging Members have died.
     * 
     * (2-way calculated; do not design.)
     */
    private int __m_ResendTimeout;
    
    /**
     * Property SendQueue
     *
     * The Queue on which OutgoingUdpPackets are enqueued for processing by the
     * Speaker.
     */
    private transient com.tangosol.coherence.component.util.Queue __m_SendQueue;
    
    /**
     * Property SingleMemberSetTemp
     *
     * A cached SingleMemberSet object that is used to get a snap-shot of the
     * MemberSet that a DIRECTED_ONE Packet is being delivered to. Used by
     * onPacket.
     * 
     * Unlike the getMemberSetTemp getSingleMeberSetTemp does not clear the set
     * before returning it.
     */
    private transient com.tangosol.coherence.component.net.memberSet.SingleMemberSet __m_SingleMemberSetTemp;
    
    /**
     * Property StatsCpu
     *
     * Statistics: total time spent while sending packets.
     */
    private transient long __m_StatsCpu;
    
    /**
     * Property StatsNacksSent
     *
     * Statistics: total number of Nack packets (early Acks) sent.
     */
    private transient long __m_StatsNacksSent;
    
    /**
     * Property StatsResent
     *
     * Statistics: total number of re-sent packets.
     */
    private transient long __m_StatsResent;
    
    /**
     * Property StatsResentEarly
     *
     * Statistics: total number of packets re-sent ahead of schedule, i.e. due
     * to a NACK.
     */
    private transient long __m_StatsResentEarly;
    
    /**
     * Property StatsResentExcess
     *
     * Statistics: total number of repeated package acknowledgements.
     */
    private transient long __m_StatsResentExcess;
    
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
     * Property UdpSocketMulticast
     *
     */
    private com.tangosol.coherence.component.net.socket.UdpSocket __m_UdpSocketMulticast;
    
    /**
     * Property UdpSocketUnicast
     *
     */
    private com.tangosol.coherence.component.net.socket.udpSocket.UnicastUdpSocket __m_UdpSocketUnicast;
    
    // Default constructor
    public PacketPublisher()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public PacketPublisher(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            setAckdPacketListTemp(new com.tangosol.util.RecyclingLinkedList());
            setCloggedCount(1024);
            setCloggedDelay(32);
            setDaemonState(0);
            setDefaultGuardRecovery(0.9F);
            setDefaultGuardTimeout(60000L);
            setLostPacketListTemp(new com.tangosol.util.RecyclingLinkedList());
            setMulticastEnabled(true);
            setMulticastThreshold(0.25);
            setNotifier(new com.oracle.coherence.common.base.SingleWaiterMultiNotifier());
            setSingleMemberSetTemp(new com.tangosol.coherence.component.net.memberSet.SingleMemberSet());
            }
        catch (java.lang.Exception e)
            {
            // re-throw as a runtime exception
            throw new com.tangosol.util.WrapperException(e);
            }
        
        // containment initialization: children
        _addChild(new PacketPublisher.AckQueue("AckQueue", this, true), "AckQueue");
        _addChild(new PacketPublisher.ConfirmationQueue("ConfirmationQueue", this, true), "ConfirmationQueue");
        _addChild(new com.tangosol.coherence.component.util.Daemon.Guard("Guard", this, true), "Guard");
        _addChild(new PacketPublisher.InQueue("InQueue", this, true), "InQueue");
        _addChild(new PacketPublisher.OutgoingMessageArray("OutgoingMessageArray", this, true), "OutgoingMessageArray");
        _addChild(new PacketPublisher.ResendQueue("ResendQueue", this, true), "ResendQueue");
        
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
        return new com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketPublisher();
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
            clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/packetProcessor/PacketPublisher".replace('/', '.'));
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
     * Address a non-single member bundle, i.e. is either multipoint, broadcast
    * or has "no live receipients".
     */
    protected int addressMany(com.tangosol.coherence.component.net.PacketBundle bundle)
        {
        // import Component.Net.Member;
        // import Component.Net.Member$FlowControl as com.tangosol.coherence.component.net.Member.FlowControl;
        // import Component.Net.MemberSet;
        // import Component.Net.Packet;
        // import Component.Net.Packet.MessagePacket;
        // import Component.Net.Packet.MessagePacket.Broadcast;
        // import com.tangosol.util.Base;
        // import java.net.InetSocketAddress;
        // import java.util.Iterator;
        // import java.util.Set;
        
        Packet packet = bundle.getPacket(0);
        
        // only MessagePacket can be addressed to multiple destinations
        // otherwise this is a "no live recipients" packet
        if (packet instanceof MessagePacket)
            {
            MemberSet setTo = ((MessagePacket) packet).getToMemberSet();
            
            if (setTo != null && packet.isConfirmationRequired())
                {
                int[] anToId = setTo.toIdArray();
                int   cAddrs = anToId.length;
        
                boolean fMulticast = false;
                if (isMulticastEnabled() && isMulticast(cAddrs))
                    {
                    int cBypass = getMulticastBypassCount();
                    if (cBypass > 0)
                        {
                        setMulticastBypassCount(cBypass - 1);
                        }
                    else if (packet.getSentCount() > 0)
                        {                
                        // We only send a given packet over MC once to work around flaky
                        // MC implementations.  One example is IB which will drop MC packets larger
                        // then 2KB (despite having a larger IP MTU).
                        // Additionally to help performance we avoid MC for some relatively large
                        // number of multipoint sends to make up for the retry delay.  We don't permantently
                        // disable MC as it may be a temporary issue, and if so, this simple
                        // algorithm will result in us using MC most of the time so long as it isn't
                        // dropping packets.
                        setMulticastBypassCount(com.tangosol.coherence.component.net.Member.FlowControl.getSuccessGoal());
                        }
                    else // first send of this packet, no skips configured, use MC
                        {
                        fMulticast = true;
                        }            
                    }
                
                if (fMulticast) // multicast confirmed
                    {            
                    bundle.setUdpSocket(getUdpSocketMulticast());
                    bundle.addDestination(getMulticastAddress());
                    }
                else // unicast confirmed
                    {
                    bundle.setUdpSocket(getUdpSocketUnicast());
                    
                    for (int i = 0; i < cAddrs; ++i)
                        {
                        Member member = getMember(anToId[i]);
                        if (member != null)
                            {
                            bundle.addDestination(getSocketAddress(member, packet));
                            }
                        }
                    }
        
                int  cbPref = getPreferredPacketLength();
                long ldtNow = Base.getLastSafeTimeMillis();
                for (int i = 0; i < cAddrs; ++i)
                    {
                    Member member = getMember(anToId[i]);
                    if (member != null)
                        {
                        member.setLastOutgoingMillis(ldtNow);
                        cbPref = Math.min(cbPref, member.getPreferredPacketLength());
                        }
                    }
                bundle.setMaximumLength(cbPref);        
                }
            else if (packet instanceof Broadcast)
                {
                if (isMulticastEnabled() && ((Broadcast) packet).getToAddress() == null)
                    {
                    bundle.setUdpSocket(getUdpSocketMulticast());
                    bundle.addDestination(getMulticastAddress());
                    }
                else // broadcast over unicast
                    {
                    bundle.setUdpSocket(getUdpSocketUnicast());
        
                    if (((Broadcast) packet).getToAddress() == null)
                        {
                        // broadcast to the dynamic broadcast set (which includes all WKAs)
                        Set setBroadcast = getBroadcastAddresses();
                        synchronized (setBroadcast)
                            {
                            bundle.addDestinations(setBroadcast);
                            }
                        // Note: as of 12.2.1 we no longer broadcast to the dead member set as there may be other
                        // non-coherence processes now using those ports, it just isn't correct. Instead we rely
                        // upon the fact that a zombie member will quickly identify that it has been shunned do
                        // to lack of HeartBeat responses. Quickly here is 1.5x the packet timeout, which isn't to
                        // bad considering that the member was kicked from the cluster. If we need to further reduce
                        // this time we could do so by having every member send a SMHB (or similar) to a random WKA
                        // once per HB interval. The receiving side could identify if the sender is not in the cluster
                        // and only in such a case relay this to its senior (via a Panic?), which would then allow the
                        // senior to send a directed SMHB to the offending node which would then restart. The senior
                        // should not simply add the offending node to its dynamic broadcast list though as after the
                        // node restarted the senior would still be broadcasting to a non-coherence address assuming
                        // the offending node had been on an ephemeral port.  While this solution is quite doable it
                        // seems unnecessary given that packet timeout based deaths are now uncommon and will be resolved
                        // in 1.5x the packet timeout.
                        }
                    else
                        {
                        // "direct" broadcast (e.g. to a newcomer)
                        InetSocketAddress[] aAddr = ((Broadcast) packet).getToAddress();          
                        for (int i = 0, c = aAddr.length; i < c; ++i)
                            { 
                            bundle.addDestination(aAddr[i]);
                            }
                        }
                    }
        
                // Note: We don't allow for broadcast bundling since it is not generally addressed to a cluster member
                // and thus we don't know their preferred size, and bundling up to the allowable maximum packet size
                // could result in the network infrastructure dropping the packet if fragmentation is not enabled
                }
            }
        
        return bundle.getAddressCount();
        }
    
    /**
     * Assign  "To" and "From" Ids to the message.  This method is reserved for
    * use by the publisher thread, see InQueue.removeNoWait.
     */
    public void assignMessageIds(com.tangosol.coherence.component.net.Message msg)
        {
        // import Component.Net.Member;
        // import Component.Net.MemberSet.DependentMemberSet;
        // import Component.Net.Packet;
        // import Component.Net.Packet.MessagePacket.Directed;
        // import Component.Util.WindowedArray;
        
        // assign from id
        long lIdFrom = getFromMessageId() + 1L;
        
        setFromMessageId(lIdFrom);
        msg.setFromMessageId(lIdFrom);
        
        // it is essential that the "from" message id and the destination member specific
        // "to" message id are incremented simultaneously since the PacketReciever heavily
        // relies on that assumption
        
        Packet packetFirst = msg.getPacket(0);
        if (packetFirst instanceof Directed)
            {
            // Assign the "to" Message id(s) to the Directed packet.
            // ToIds are sequential keys for all Messages received by the other Members
            // that are sent from this Member
            Directed           packetHead = (Directed) packetFirst;
            DependentMemberSet setTo      = packetHead.getToMemberSet();
            if (setTo == null)
                {    
                // single recipient
                Member memberTo = getMember(packetHead.getToId());
                if (memberTo != null)
                    {
                    // the "to" id is only on the header (i.e. the
                    // directed) packet; sequel packets are correlated
                    // to the directed packet using the "from" id
                    packetHead.setToMessageId(
                        memberTo.getNextDestinationMessageId());
                    }
                }
            else
                {
                // multi recipient
                Object[] aMember = setTo.toArray();
                for (int i = aMember.length - 1; i >= 0; --i)
                    {
                    Member memberTo = (Member) aMember[i];
                    setTo.setDestinationMessageId(memberTo.getId(),
                        memberTo.getNextDestinationMessageId());
                    }
                }
        
            // Assign the "from" Message id to all packets.
            // That is a sequential key for Messages sent by this Member
            long lFromMsgId = getMessageOutgoing().add(msg);
            long lSetId     = msg.getFromMessageId();
        
            if (lSetId != lFromMsgId)
                {
                throw new IllegalStateException("Set MsgFromId " + lSetId + " != expected " + lFromMsgId);
                }
        
            for (int i = 0, c = msg.getMessagePartCount(); i < c; ++i)
                {
                msg.getPacket(i).setFromMessageId(lFromMsgId);
                }
            }
        else
            {
            // We don't need to track this message, but we must
            // maintain id sync between InQueue and MessageOutgoing array
            WindowedArray wa = getMessageOutgoing();
            wa.remove(wa.add(msg));
            }
        }
    
    /**
     * Compute the preferred packet size to use when sending a message to the
    * specified member set.
     */
    protected int computePreferredPacketLength(com.tangosol.coherence.component.net.MemberSet setTo)
        {
        // import Component.Net.Member;
        // import Component.Net.MemberSet;
        // import Component.Net.MemberSet.ActualMemberSet;
        // import java.util.Iterator;
        
        int cbStd  = 1452; // based on standard 1500B MTU and IPv6 40B + UDP 8B header
        int cbPref = getPreferredPacketLength();
        int cTo    = setTo == null ? 0 : setTo.size();
        if (cTo == 1) // common case
            {
            Member memberTo = getMemberSet().getMember(setTo.getFirstId());
            if (memberTo != null)
                {
                // before the MemberWelcome handshake completes, we don't have the peer's preferred size
                int cbPrefThat = memberTo.getPreferredPacketLength();
                cbPref = Math.min(cbPref, cbPrefThat == 0 ? Math.min(cbStd, getMaximumPacketLength()) : cbPrefThat);
                }
        
            return cbPref;
            }
        else if (cTo == 0)
            {
            return cbPref;
            }
        
        if (isMulticastEnabled() && isMulticast(cTo))
            {
            // MC may be on a different NIC and have only the standard MTU, also even when MC NIC's MTU
            // is larger some NICs can still be problematic, some IB NICs for example drop MC packets over 2KB.
            cbPref = Math.min(cbPref, Math.min(cbStd, getMaximumPacketLength()));
            }
        
        if (setTo instanceof ActualMemberSet)
            {
            for (Iterator iter = setTo.iterator(); iter.hasNext(); )
                {
                Member memberTo = (Member) iter.next();
        
                int cbPrefThat = memberTo.getPreferredPacketLength();
                cbPref = Math.min(cbPref, cbPrefThat == 0 ? Math.min(cbStd, getMaximumPacketLength()) : cbPrefThat);
                }
            }
        else
            {
            MemberSet setCluster = getMemberSet();
            int[]     anToId     = setTo.toIdArray();
            for (int i = 0, c = anToId.length; i < c; ++i)
                {
                Member memberTo = setCluster.getMember(anToId[i]);
                if (memberTo != null)
                    {
                    int cbPrefThat = memberTo.getPreferredPacketLength();
                    cbPref = Math.min(cbPref, cbPrefThat == 0 ? Math.min(cbStd, getMaximumPacketLength()) : cbPrefThat);
                    }
                }
            }
        
        return cbPref;
        }
    
    /**
     * Allow member to potentially defer the packet.  This method is called
    * soley by the publisher thread, and only for packets which are deferrable.
    * 
    * @param packet                                  the packet to potentially
    * defer
    * @param fPacketCurrentlyDeferred   indiciates if this packet is currently
    * deferred.
    * 
    * @return true if packet is deferred, false otherwise.
     */
    public boolean deferPacket(com.tangosol.coherence.component.net.Member member, com.tangosol.coherence.component.net.packet.MessagePacket packet)
        {
        // import Component.Net.Member$FlowControl as com.tangosol.coherence.component.net.Member.FlowControl;
        // import Component.Net.Packet.MessagePacket;
        // import Component.Util.Queue as com.tangosol.coherence.component.util.Queue;
        
        boolean     fResult     = false;
        com.tangosol.coherence.component.net.Member.FlowControl flowControl = member.getFlowControl();
        if (flowControl != null)
            {
            // avoid sync if packet is already in the CONFIRMED (terminal) state
            if (packet.getDeliveryState() == MessagePacket.DELIVERY_CONFIRMED)
                {
                // already Ackd packets are let through for free
                return false;
                }
        
            int     cThreshold   = flowControl.isPaused() ? 1 :
                                    flowControl.getOutstandingPacketThreshold();
            int     cOutstanding = flowControl.getOutstandingPacketCount();
            boolean fOverLimit   = cOutstanding > cThreshold;
            boolean fDeferring   = flowControl.isDeferring();
            int     nState       = packet.getDeliveryState();
        
            if (nState == MessagePacket.DELIVERY_CONFIRMED)
                {
                // already Ackd packets are let through for free
                // this was rechecked now that we are syncd it may
                // may have been updated
                fResult = false;
                }        
            else if (fOverLimit)
                {
                // if we are overlimit everything is deferred
                synchronized (flowControl) // see onMemberLeft
                    {
                    flowControl.getDeferredQueue().add(packet);
                    }
                packet.setDeliveryState(MessagePacket.DELIVERY_DEFERRED, member);            
                fResult = true;
                }
            else if (fDeferring && nState != MessagePacket.DELIVERY_DEFERRED)
                {
                // if not over the limit, but still clearning out our deferred packets
                // we defer any currently non-deferred packet to avoid starving
                // deferred packets.  If we didn't do this, these packets could
                // push us over the limit again, and cause already deferred packets
                // to be re-deferred when the would have otherwise made it out
        
                synchronized (flowControl) // see onMemberLeft
                    {
                    com.tangosol.coherence.component.util.Queue deferredQueue = flowControl.getDeferredQueue();
                    if (deferredQueue.isEmpty())
                        {
                        // take a short-cut and add directly to the end of ready queue
                        getDeferredReadyQueue().add(packet);
                        }
                    else
                        {
                        deferredQueue.add(packet);
                        }
                    }
        
                packet.setDeliveryState(MessagePacket.DELIVERY_DEFERRED, member);            
                fResult = true;
                }
            }
        
        return fResult;
        }
    
    /**
     * Detect packets which appear to be lost based on the information from
    * Acks.  Early loss detection allows identifying a packet as being lost
    * long before it's packet resend time is reached.  
    * 
    * Detection utilizes the knowledge of the newest packet (from this member)
    * that the remote member is aware of, and will consider any unacked packets
    * which were sent before this packet to be lost.  Any packets detected as
    * lost will be added to the specified list.  For performance reasons the
    * caller should first ensure that packetNewest is still in the member's
    * outstanding packet list, the Receiver does this by verifying that it was
    * not ack'd prior to the Ack which triggered this call.
    * 
    * This method will also detect if packets sent to this member have been
    * lost, and if so a nack will be scheduled.
    * 
    * This method may only be run on the publisher  thread as it makes use of a
    * PacketListTemp property.
    * 
    * @param member                       the member to run detection against
    * @param packetNewestTo        the newest packet from us, that this member
    * is aware of
    * @param packetIdNewestFrom the newest packet from this member that we are
    * aware of
     */
    protected void doEarlyLossDetection(com.tangosol.coherence.component.net.Member member, com.tangosol.coherence.component.net.packet.MessagePacket packetNewestTo, com.tangosol.net.internal.PacketIdentifier ptidNewestFrom)
        {
        // import Component.Net.Packet.MessagePacket;
        // import Component.Util.Queue as com.tangosol.coherence.component.util.Queue;
        // import com.tangosol.net.internal.PacketComparator;
        // import com.tangosol.net.internal.PacketIdentifier;
        // import com.tangosol.util.Base;
        // import java.util.Collections;
        // import java.util.List;
        
        // detect if we've missed packets from this member
        if (PacketComparator.compare(ptidNewestFrom,
                member.getContiguousFromPacketId()) > 0)
            {
            // the newest they sent us is younger than (comes after) the end of
            // our contiguous packet stream from this member. Even though we may actually
            // have that packet, there are definitly some missing packets. 
            scheduleNack(member);
            }
        
        // detect packets from us which they've missed
        com.tangosol.coherence.component.util.Queue queueRecent = member.getRecentPacketQueue();
        int   nMemberId   = member.getId();
        if (packetNewestTo == null)
            {
            // not enough info to do early loss detection; keep the recentQueue from
            // getting too long by clearing all contiguous ack'd packets
            while (true)
                {
                MessagePacket packet = (MessagePacket) queueRecent.peekNoWait();
                if (packet == null || packet.isAddressedTo(nMemberId))
                    {
                    // end of queue, or first unack'd packet stop cleaning
                    return;
                    }
                // packet has been ack'd
                if (packet != queueRecent.removeNoWait())
                    {
                    throw new IllegalStateException();
                    }
                }
            }
        
        // remove all packets sent before the specified newest packet
        // and make note of those which are still unack'd by this member
        PacketIdentifier ptidContTo = member.getContiguousToPacketId();
        long             ldtNow     = Base.getSafeTimeMillis();
        List             listLost   = getLostPacketListTemp();
        List             listAckd   = getAckdPacketListTemp();
        boolean          fAdded     = false;
        MessagePacket    packet;
        do
            {
            packet = (MessagePacket) queueRecent.removeNoWait();
            if (packet == null)
                {
                // We hit the end of list before finding the indicated newest packet
                // we therefore can't tell which packets in our lost list
                // were sent before the newest packet, so we don't do anything.
                // This can happen if the publisher thread declares this packet
                // as lost while we are trying to use it as our marker
                // TODO: would be nice to know this ahead of time
                fAdded = false;
                break;
                }
        
            if (packet.isAddressedTo(nMemberId) &&
                packet.getResendScheduled() > ldtNow)
                {
                // still unack'd by this member, and it was sent before packetNewest
                // thus it is considered lost
        
                // Check if it is older (or equal to) the last contiguous packet we
                // know they've received.
                // Note: the publisher makes the same check before resending, we do
                // this check here to prevent against extraneous adds to the resend
                // queue, and also to prevent against a potential orphan packet warning
                if (PacketComparator.compare(ptidContTo, packet) >= 0)
                    {
                    // packet is older (or equal) to ptidContigTo, this indicates
                    // that we missed the Ack for the packet
                    if (packet.registerAck(member))
                        {
                        listAckd.add(packet);
                        }
                    }
                else
                    {
                    listLost.add(packet);
                    fAdded = true;
                    }
                }
            }
        while (packet != packetNewestTo);
        
        // handle ack'd packets
        if (!listAckd.isEmpty())
            {
            onAcknowledgedPacket(member, listAckd);    
            }
        
        // schedule any missing packets for early resend
        if (fAdded)
            {
            getResendQueue().addAllHead(listLost, true);
            }
        }
    
    /**
     * Move as many defered packets as possible from the member's defered queue,
    * to publisher's defered-ready queue.
     */
    public void drainDeferredPackets(com.tangosol.coherence.component.net.Member member)
        {
        // import Component.Net.Member$FlowControl as com.tangosol.coherence.component.net.Member.FlowControl;
        // import Component.Net.Packet;
        // import Component.Util.Queue as com.tangosol.coherence.component.util.Queue;
        
        com.tangosol.coherence.component.net.Member.FlowControl flowControl = member.getFlowControl();
        if (flowControl != null)
            {
            int cThreshold = flowControl.isPaused() ? 1 :
                             flowControl.getOutstandingPacketThreshold();
            int cSpots     = cThreshold - flowControl.getOutstandingPacketCount();        
        
            if (cSpots > 0)
                {
                // move out deferred packets to the publisher queue
                com.tangosol.coherence.component.util.Queue   queueDeferred = flowControl.getDeferredQueue();
                com.tangosol.coherence.component.util.Queue   queueReady    = getDeferredReadyQueue();
        
                // COH-10336: this is to prevent publisher from running into endless loop
                //            queueDeferred == queueReady after member has left
                if (queueDeferred != queueReady)
                    {      
                    do
                        {
                        Packet packet = (Packet) queueDeferred.removeNoWait();
                        if (packet == null)
                            {
                            // end of deferred queue        
                            break;
                            }
        
                        queueReady.add(packet);
        
                        // only count packets which haven't been ack'd
                        if (packet.getToId() != 0)
                            {
                            --cSpots;
                            }
                        }
                    while (cSpots > 0);
                    }
                }
            }
        }
    
    // From interface: com.tangosol.internal.util.MessagePublisher
    public long drainOverflow(java.util.Set setDest, long cMillisTimeout)
            throws java.lang.InterruptedException
        {
        // import Component.Net.Member;
        // import Component.Net.Member$FlowControl as com.tangosol.coherence.component.net.Member.FlowControl;
        // import Component.Net.MemberSet;
        
        // This method is optimized for the common cases, i.e. single non-backlogged destination
        
        int cMaxPackets = getCloggedCount();
        if (cMaxPackets <= 0 || setDest == null)
            {
            return cMillisTimeout; // traffic-jam feature disabled
            }
        
        MemberSet setMember = (MemberSet) setDest;
        Member    member    = getMember(setMember.getFirstId());
        if (com.tangosol.coherence.component.net.Member.FlowControl.isEnabled() && setMember.size() == 1 && member != null && member != getThisMember())
            {
            long cPackets = getResendQueue().getMultipointPacketCount() + getDeferredReadyQueue().size();
        
            com.tangosol.coherence.component.net.Member.FlowControl flowControl = member.getFlowControl();
            if (flowControl != null)
                {
                cPackets += flowControl.getPendingPacketCount();
                }
        
            if (cPackets < cMaxPackets)
                {
                return cMillisTimeout; // common case; single member send, under threshold
                }    
            }
        
        // handle backlogged as well as complex and uncommon cases to allow this method to be optimized
        return drainOverflowComplex(setMember, cMillisTimeout);
        }
    
    /**
     * Wait for any backlog condition to clear.
    * 
    * @param setMembers  the members of interest
    * @param cMillisTimeout  the maximum wait time, or zero for infinite
    * 
    * @return the remaining timeout
     */
    protected long drainOverflowComplex(com.tangosol.coherence.component.net.MemberSet setMembers, long cMillisTimeout)
            throws java.lang.InterruptedException
        {
        // import Component.Net.Member;
        // import Component.Net.Member$FlowControl as com.tangosol.coherence.component.net.Member.FlowControl;
        // import Component.Net.MemberSet;
        // import Component.Util.Queue as com.tangosol.coherence.component.util.Queue;
        // import com.oracle.coherence.common.base.Blocking;
        // import com.tangosol.util.Base;
        // import com.tangosol.net.RequestTimeoutException;
        
        int          cMaxPackets  = getCloggedCount();
        com.tangosol.coherence.component.util.Queue        queueIn      = getPacketAdapterQueue();
        com.tangosol.coherence.component.util.Queue        queueReady   = getDeferredReadyQueue();
        PacketPublisher.ResendQueue queueResend  = getResendQueue();
        boolean      fFlowcontrol = com.tangosol.coherence.component.net.Member.FlowControl.isEnabled();
        Member       memberThis   = getThisMember();
        int[]        anToId       = null;
        long         ldtNow       = 0L;
        long         ldtStart     = 0L;
        long         ldtAlarmNext = 0L;
        long         ldtTimeout   = 0L;
        
        for (int cIters = 0; isStarted(); ++cIters)
            {
            int   cPackets        = queueIn.size();
            int   cPacketsIn      = cPackets;
            int   cPacketsResend  = -1;
            int   cPacketsReady   = -1;
            int   cPacketsMp      = -1;
            int   cPacketsPending = -1;
            Member memberJammed   = null;
        
            if (fFlowcontrol)
                {
                // packet deferral is enabled; only count the packets in the InQueue and
                // the non-deferrable packets
                // pause if we are over limit, or if any addressed member is deferring
                cPackets += (cPacketsMp = queueResend.getMultipointPacketCount());
                cPackets += (cPacketsReady = queueReady.size());
        
                if (setMembers == null)
                    {
                    // broadcast; only on cluster service component
                    break;
                    }
                else if (setMembers.size() == 1)
                    {
                    Member member = getMember(setMembers.getFirstId());
                    if (member != null)
                        {
                        com.tangosol.coherence.component.net.Member.FlowControl flowControl = member.getFlowControl();
                        if (flowControl != null)
                            {
                            memberJammed = member;
                            cPackets    += (cPacketsPending = flowControl.getPendingPacketCount());
                            }
                        }
                    }
                else
                    {
                    // compute the member Ids
                    anToId = setMembers.toIdArray();
        
                    // if any member is over limit then wait
                    for (int i = 0, c = anToId.length; i < c; ++i )
                        {
                        Member member = getMember(anToId[i]);
                        if (member != null)
                            {
                            com.tangosol.coherence.component.net.Member.FlowControl flowControl = member.getFlowControl();
                            if (flowControl != null)
                                {
                                int cPending = flowControl.getPendingPacketCount();
                                if (cPackets + cPending > cMaxPackets)
                                    {
                                    memberJammed = member;
                                    cPackets    += (cPacketsPending = cPending);
                                    break;
                                    }
                                }
                            }
                        }
                    }
                }
            else
                {
                // no flow control, include resend queue in count
                cPackets += (cPacketsResend = queueResend.size());
                }
        
            if (cPackets > cMaxPackets && ldtNow <= ldtTimeout)
                {
                if (ldtNow >= ldtAlarmNext) // always true on first pass
                    {
                    if (ldtStart == 0L)
                        {
                        // first sleep, record time, and compute timeout
                        ldtStart   = ldtNow = Base.getSafeTimeMillis();
                        ldtTimeout = cMillisTimeout == 0L
                            ? Long.MAX_VALUE
                            : ldtStart + cMillisTimeout;
                        }
                    else
                        {
                        // we've been paused for a long time, issue warning and debugging
                        _trace("Overloaded packet queue; " +
                            cPackets + "/" + cMaxPackets + " packet limit" +
                            ", Duration="    + ((ldtNow - ldtStart) / 1000) + "s" +
                            ", InQueue="     + cPacketsIn          +
                            ", ResendQueue=" + cPacketsResend      +
                            ", MultiPoint="  + cPacketsMp          +
                            ", Pending="     + cPacketsPending     +
                            ", Ready="       + cPacketsReady       +
                            ", "             + memberJammed        +
                            ", com.tangosol.coherence.component.net.Member.FlowControl(" + (memberJammed == null
                                 ? null : memberJammed.getFlowControl()) +
                            ")", 2);
                        }
        
                    // schedule next warning
                    ldtAlarmNext = ldtNow + (getResendTimeout() / 4);
                    }
        
                // a pause is needed, flush the publisher's input queue
                getQueue().flush();
        
                Blocking.sleep(getCloggedDelay());
        
                ldtNow = Base.getSafeTimeMillis();
                }
            else
                {
                // under limit or over timeout, allow send to continue
                if (cPackets == 0 && cIters > 1)
                   {
                   // while sleeping the entire traffic jam cleared
                   ldtNow = Base.getSafeTimeMillis();
        
                   if (ldtNow > getLastTrafficJamWarningTimeMillis() + 60000L)
                       {        
                       // we slept for too long, and are undersupplying the publisher
                       // don't log more then once per-minute
                       _trace("The \"traffic-jam\" settings may be overly cautious; consider "
                              + "increasing the \"maximum-packets\", or decreasing "
                              + "\"pause-milliseconds\"", 3);
                       setLastTrafficJamWarningTimeMillis(ldtNow);     
                       }
                   }
                break;
                }
            }
        
        if (cMillisTimeout == 0L || ldtTimeout == 0L)
            {
            return cMillisTimeout;
            }
        else if (ldtTimeout > ldtNow)
            {
            return ldtTimeout - ldtNow;
            }
        else
            {
            throw new RequestTimeoutException("Request timed out");
            }
        }
    
    /**
     * Return the MsgArrayTemp property, ensuring that the size of the array is
    * greater or equal to the specified size.
    * 
    * @see #onPacketAck
     */
    protected com.tangosol.coherence.component.net.Message[] ensureMsgArrayTemp(int cMinSize)
        {
        // import Component.Net.Message;
        
        Message[] aMsgTemp = getMsgArrayTemp();
        
        if (aMsgTemp == null || aMsgTemp.length < cMinSize)
            {
            aMsgTemp = new Message[cMinSize];
            setMsgArrayTemp(aMsgTemp);
            }
        
        return aMsgTemp;
        }
    
    /**
     * Return the MsgIdArrayTemp property, ensuring that the size of the array
    * is greater or equal to the specified size.
    * 
    * @see #onPacketAck
     */
    protected long[] ensureMsgIdArrayTemp(int cMinSize)
        {
        long[] alTemp = getMsgIdArrayTemp();
        
        if (alTemp == null || alTemp.length < cMinSize)
            {
            alTemp = new long[cMinSize];
            setMsgIdArrayTemp(alTemp);
            }
        
        return alTemp;
        }
    
    /**
     * Return a temporary MemberSet containing the supplied member.
     */
    protected com.tangosol.coherence.component.net.MemberSet ensureSingleMemberSetTemp(com.tangosol.coherence.component.net.Member member)
        {
        // import Component.Net.MemberSet.SingleMemberSet;
        
        SingleMemberSet setMember = getSingleMemberSetTemp();
        setMember.setTheMember(member);
        
        return setMember;
        }
    
    // From interface: com.tangosol.internal.util.MessagePublisher
    public void flush()
        {
        getQueue().flush();
        }
    
    /**
     * Ensure that the send queue has been flushed.
     */
    public void flushSend()
        {
        getSendQueue().flush();
        }
    
    public String formatStats()
        {
        // import com.tangosol.util.Base;
        
        long   cCpu        = getStatsCpu();
        long   cMillis     = Base.getSafeTimeMillis() - getStartTimestamp();
        long   lSent       = getStatsSent();
        long   lResent     = getStatsResent();
        long   lWasted     = getStatsResentExcess();
        double dCpu        = cMillis == 0L ? 0.0 : ((double) cCpu)/((double) cMillis);
        double dThru       = cCpu    == 0L ? 0.0 : ((double) lSent*1000)/((double) cCpu);
        double dSuccess    = lSent   == 0L ? 1.0 : 1.0 - ((double) lResent)/((double) lSent);
        double dEfficiency = lSent   == 0L ? 1.0f
                           : (float) (1.0 - ((double) lWasted) / ((double) lSent));
        
        
        // round rates
        dCpu        = ((int) (dCpu        * 1000.0))  / 10.0; // percentage
        dSuccess    = ((int) (dSuccess    * 10000.0)) / 10000.0;
        dEfficiency = ((int) (dEfficiency * 10000.0)) / 10000.0;
        
        return "Cpu=" + cCpu + "ms (" + dCpu + "%)"
               + ", PacketsSent="   + lSent
               + ", PacketsResent=" + lResent
               + ", SuccessRate="   + dSuccess
               + ", Efficiency="    + dEfficiency       
               + ", Throughput="    + (int) dThru + "pkt/sec"
               ;
        }
    
    // Accessor for the property "AckDelay"
    /**
     * Getter for property AckDelay.<p>
    * The minimum number of milliseconds between the queueing of an Ack packet
    * and the sending of the same.
     */
    public int getAckDelay()
        {
        return getAckQueue().getDelayMillis();
        }
    
    // Accessor for the property "AckdPacketListTemp"
    /**
     * Getter for property AckdPacketListTemp.<p>
    * Used in the processing of Acks to avoid frequent creation of a temporary
    * list.
    * 
    * @see #registerAcks
    * @see #doEarlyLossDetection
     */
    protected java.util.List getAckdPacketListTemp()
        {
        // import java.util.List;
        
        List list = __m_AckdPacketListTemp;
        list.clear();
        return list;
        }
    
    // Accessor for the property "AckQueue"
    /**
     * Getter for property AckQueue.<p>
    * A reference to the AckQueue child, which is the Queue in which
    * Packet-notification Packets are placed so that they are automatically
    * after a configurable period of time.
     */
    public PacketPublisher.AckQueue getAckQueue()
        {
        return __m_AckQueue;
        }
    
    // Accessor for the property "BroadcastAddresses"
    /**
     * Getter for property BroadcastAddresses.<p>
    * A Set<InetSocketAddress> used for broadcast in leu of multicast. This
    * list always contains pre-configured set of WellKnownAddresses, but could
    * additionally contain dynamically obtained addresses. This set can be
    * modified by the ClusterService.
     */
    public java.util.Set getBroadcastAddresses()
        {
        return __m_BroadcastAddresses;
        }
    
    public com.tangosol.io.ByteArrayWriteBuffer.Allocator getBufferAllocator(int cb)
        {
        return null;
        }
    
    // Accessor for the property "CloggedCount"
    /**
     * Getter for property CloggedCount.<p>
    * The maximum number of packets in the send plus resend queues before
    * determining that the publisher is clogged. Zero means no limit.
     */
    public int getCloggedCount()
        {
        return __m_CloggedCount;
        }
    
    // Accessor for the property "CloggedDelay"
    /**
     * Getter for property CloggedDelay.<p>
    * The number of milliseconds to pause client threads when a clog occurs, to
    * wait for the clog to dissipate. (The pause is repeated until the clog is
    * gone.) Anything less than one (e.g. zero) is treated as one.
     */
    public int getCloggedDelay()
        {
        return __m_CloggedDelay;
        }
    
    // Accessor for the property "ClusterNameBuffer"
    /**
     * Getter for property ClusterNameBuffer.<p>
    * This cluster's name length encoded into a ByteBuffer just as is used in
    * Broadcast packets.
     */
    public java.nio.ByteBuffer getClusterNameBuffer()
        {
        return __m_ClusterNameBuffer;
        }
    
    // Accessor for the property "ConfirmationQueue"
    /**
     * Getter for property ConfirmationQueue.<p>
    * A reference to the ConfirmationQueue child, which is the Queue in which
    * incomming Ack packets from other members are placed.
     */
    public PacketPublisher.ConfirmationQueue getConfirmationQueue()
        {
        return __m_ConfirmationQueue;
        }
    
    // Accessor for the property "DeferredReadyQueue"
    /**
     * Getter for property DeferredReadyQueue.<p>
    * A queue of previously deferred packets which are ready to be sent.  This
    * value will be null if deferral is not enabled.
    * 
    * @see Member#DeferredQueue
     */
    public com.tangosol.coherence.component.util.Queue getDeferredReadyQueue()
        {
        return __m_DeferredReadyQueue;
        }
    
    // Accessor for the property "FromMessageId"
    /**
     * Getter for property FromMessageId.<p>
    * "allocator" for from message ids
     */
    public long getFromMessageId()
        {
        return __m_FromMessageId;
        }
    
    // Accessor for the property "IncomingPacketQueues"
    /**
     * Getter for property IncomingPacketQueues.<p>
    * The set of queues responsible for supplying the publisher with packets.
     */
    protected com.tangosol.coherence.component.util.Queue[] getIncomingPacketQueues()
        {
        return __m_IncomingPacketQueues;
        }
    
    // Accessor for the property "LastTrafficJamWarningTimeMillis"
    /**
     * Getter for property LastTrafficJamWarningTimeMillis.<p>
    * Timestamp of the last time a warning was issued due to overly agressive
    * traffic jam settings.
    * 
    * @volatile, accessed by all client threads
     */
    public long getLastTrafficJamWarningTimeMillis()
        {
        return __m_LastTrafficJamWarningTimeMillis;
        }
    
    // Accessor for the property "LostPacketListTemp"
    /**
     * Getter for property LostPacketListTemp.<p>
    * Used in the processing of Acks to avoid frequent creation of a temporary
    * list.
    * 
    * @see #doEarlyLossDetection
     */
    protected java.util.List getLostPacketListTemp()
        {
        // import java.util.List;
        
        List list = __m_LostPacketListTemp;
        list.clear();
        return list;
        }
    
    // Accessor for the property "MaximumPacketLength"
    /**
     * Getter for property MaximumPacketLength.<p>
    * The minimum safe packet length required to be supported by all cluster
    * members.
     */
    public int getMaximumPacketLength()
        {
        return __m_MaximumPacketLength;
        }
    
    // Accessor for the property "MessageBufferAllocator"
    /**
     * Getter for property MessageBufferAllocator.<p>
    * The pool of buffers used when serialize a Message.
     */
    public com.tangosol.io.MultiBufferWriteBuffer.WriteBufferPool getMessageBufferAllocator()
        {
        return __m_MessageBufferAllocator;
        }
    
    // Accessor for the property "MessageOutgoing"
    /**
     * Getter for property MessageOutgoing.<p>
    * The array of outgoing Message objects.
     */
    public com.tangosol.coherence.component.util.WindowedArray getMessageOutgoing()
        {
        return __m_MessageOutgoing;
        }
    
    // Accessor for the property "MsgArrayTemp"
    /**
     * Getter for property MsgArrayTemp.<p>
    * Used in the processing of Acks to avoid frequent creation of a temporary
    * array.
    * 
    * @see #onPacketAck
    * @see #ensureMsgArrayTemp
     */
    public com.tangosol.coherence.component.net.Message[] getMsgArrayTemp()
        {
        return __m_MsgArrayTemp;
        }
    
    // Accessor for the property "MsgIdArrayTemp"
    /**
     * Getter for property MsgIdArrayTemp.<p>
    * Used in the processing of Acks to avoid frequent creation of a temporary
    * array.
    * 
    * @see #onPacketAck
    * @see #ensureMsgIdArrayTemp
     */
    public long[] getMsgIdArrayTemp()
        {
        return __m_MsgIdArrayTemp;
        }
    
    // Accessor for the property "MulticastAddress"
    /**
     * Getter for property MulticastAddress.<p>
    * The cluster's mulitcast address, if enabled
     */
    public java.net.InetSocketAddress getMulticastAddress()
        {
        return __m_MulticastAddress;
        }
    
    // Accessor for the property "MulticastBypassCount"
    /**
     * Getter for property MulticastBypassCount.<p>
    * A count of the number of future multicast transmissions which should be
    * skipped (turned into unicast).  This is set when it appears that
    * multicast may be flaky.
     */
    public int getMulticastBypassCount()
        {
        return __m_MulticastBypassCount;
        }
    
    // Accessor for the property "MulticastThreshold"
    /**
     * Getter for property MulticastThreshold.<p>
    * The percentage (0.0 to 100.0) of the servers in the cluster that a packet
    * will be sent to, above which the packet will be multicasted and below
    * which it will be unicasted.
     */
    public double getMulticastThreshold()
        {
        return __m_MulticastThreshold;
        }
    
    // Accessor for the property "NackDelayMillis"
    /**
     * Getter for property NackDelayMillis.<p>
    * The number of milliseconds to delay the sending of a NACK after detecting
    * probable packet loss.
     */
    public long getNackDelayMillis()
        {
        return __m_NackDelayMillis;
        }
    
    // Accessor for the property "PacketAdapterQueue"
    /**
     * Getter for property PacketAdapterQueue.<p>
    * The InQueue's PacketAdpater
     */
    public com.tangosol.coherence.component.util.Queue getPacketAdapterQueue()
        {
        return __m_PacketAdapterQueue;
        }
    
    // Accessor for the property "PreferredPacketLength"
    /**
     * Getter for property PreferredPacketLength.<p>
    * The "preferred" (optimal) length of each outgoing Packet.
    * 
    * @see ClusterConfig
     */
    public int getPreferredPacketLength()
        {
        return __m_PreferredPacketLength;
        }
    
    // Accessor for the property "ResendDelay"
    /**
     * Getter for property ResendDelay.<p>
    * The minimum number of milliseconds that a Packet will remain queued in
    * the ResendQueue before being removed from the front of the Queue to be
    * resent.
    * 
    * (2-way calculated; do not design.)
     */
    public int getResendDelay()
        {
        return getResendQueue().getResendMillis();
        }
    
    // Accessor for the property "ResendQueue"
    /**
     * Getter for property ResendQueue.<p>
    * A reference to the ResendQueue child, which is the Queue in which
    * guaranteed-delivery Packets are placed so that they are automatically
    * resent.
     */
    public PacketPublisher.ResendQueue getResendQueue()
        {
        return __m_ResendQueue;
        }
    
    // Accessor for the property "ResendTimeout"
    /**
     * Getter for property ResendTimeout.<p>
    * The maximum number of milliseconds that a Packet will be resent before it
    * is assumed that the remaining unacknowledging Members have died.
    * 
    * (2-way calculated; do not design.)
     */
    public int getResendTimeout()
        {
        return getResendQueue().getTimeoutMillis();
        }
    
    // Accessor for the property "SendQueue"
    /**
     * Getter for property SendQueue.<p>
    * The Queue on which OutgoingUdpPackets are enqueued for processing by the
    * Speaker.
     */
    public com.tangosol.coherence.component.util.Queue getSendQueue()
        {
        return __m_SendQueue;
        }
    
    // Accessor for the property "SingleMemberSetTemp"
    /**
     * Getter for property SingleMemberSetTemp.<p>
    * A cached SingleMemberSet object that is used to get a snap-shot of the
    * MemberSet that a DIRECTED_ONE Packet is being delivered to. Used by
    * onPacket.
    * 
    * Unlike the getMemberSetTemp getSingleMeberSetTemp does not clear the set
    * before returning it.
     */
    protected com.tangosol.coherence.component.net.memberSet.SingleMemberSet getSingleMemberSetTemp()
        {
        return __m_SingleMemberSetTemp;
        }
    
    /**
     * Return the SocketAddress to use for sending the specified packet to the
    * specified member.
    * 
    * @param member       the member to send to
    * @param packetData  the packet being sent
     */
    protected java.net.SocketAddress getSocketAddress(com.tangosol.coherence.component.net.Member member, com.tangosol.coherence.component.net.Packet packetData)
        {
        // import Component.Net.Packet;
        // import java.net.InetSocketAddress;
        
        switch (packetData.getPacketType())
            {
            case Packet.TYPE_DIRECTED_ONE:
            case Packet.TYPE_DIRECTED_FEW:
            case Packet.TYPE_DIRECTED_MANY:
                if (packetData.getSentCount() < member.getTxDebugDropCount())
                    {
                    // the recipient member has a send delay set (debugging function),
                    // then return a null address; this will cause the packet to be
                    // dropped, delaying the overall message delivery
                    return new InetSocketAddress("0.0.0.0", 0);
                    }
                // fall through
        
            default:
                return packetData.getLength() <= member.getPreferredPacketLength()
                        ? member.getPreferredSocketAddress()
                        : member.getSocketAddress();
            }
        }
    
    // Accessor for the property "StatsCpu"
    /**
     * Getter for property StatsCpu.<p>
    * Statistics: total time spent while sending packets.
     */
    public long getStatsCpu()
        {
        return __m_StatsCpu;
        }
    
    // Accessor for the property "StatsNacksSent"
    /**
     * Getter for property StatsNacksSent.<p>
    * Statistics: total number of Nack packets (early Acks) sent.
     */
    public long getStatsNacksSent()
        {
        return __m_StatsNacksSent;
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
    
    // Accessor for the property "StatsResentEarly"
    /**
     * Getter for property StatsResentEarly.<p>
    * Statistics: total number of packets re-sent ahead of schedule, i.e. due
    * to a NACK.
     */
    public long getStatsResentEarly()
        {
        return __m_StatsResentEarly;
        }
    
    // Accessor for the property "StatsResentExcess"
    /**
     * Getter for property StatsResentExcess.<p>
    * Statistics: total number of repeated package acknowledgements.
     */
    public long getStatsResentExcess()
        {
        return __m_StatsResentExcess;
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
    
    // Accessor for the property "UdpSocketMulticast"
    /**
     * Getter for property UdpSocketMulticast.<p>
     */
    public com.tangosol.coherence.component.net.socket.UdpSocket getUdpSocketMulticast()
        {
        return __m_UdpSocketMulticast;
        }
    
    // Accessor for the property "UdpSocketUnicast"
    /**
     * Getter for property UdpSocketUnicast.<p>
     */
    public com.tangosol.coherence.component.net.socket.udpSocket.UnicastUdpSocket getUdpSocketUnicast()
        {
        return __m_UdpSocketUnicast;
        }
    
    // Declared at the super level
    /**
     * Getter for property WaitMillis.<p>
    * The number of milliseconds that the daemon will wait for notification.
    * Zero means to wait indefinitely. Negative value means to skip waiting
    * altogether.
    * 
    * @see #onWait
     */
    public long getWaitMillis()
        {
        long lWaitResend = getResendQueue().getWaitMillis();
        long lWaitAck    = getAckQueue().getWaitMillis();
        
        if (lWaitResend == 0L && lWaitAck == 0L)
            {
            return 0L;
            }
        
        if (lWaitResend == 0L)
            {
            lWaitResend = 60000L;
            }
        
        if (lWaitAck == 0L)
            {
            lWaitAck = 60000L;
            }
        
        return Math.min(lWaitResend, lWaitAck);
        }
    
    // Declared at the super level
    /**
     * Halt the daemon.  Brings down the daemon in an ungraceful manner.
    * This method should not synchronize or block in any way.
    * This method may not return.
     */
    protected void halt()
        {
        setUdpSocketMulticast(null);
        setUdpSocketUnicast(null);
        
        super.halt();
        }
    
    // Declared at the super level
    /**
     * Create the queue for this QueueProcessor.
     */
    protected com.tangosol.coherence.component.util.Queue instantiateQueue()
        {
        return (PacketPublisher.InQueue) _findChild("InQueue");
        }
    
    /**
     * Determine if a Packet should be sent using a multi-cast socket. Do not
    * use the "to" information from the Packet to make the determination;
    * rather, used the passed set.
    * 
    * @param packet  the Packet to send
    * @param setTo  a Member Set of potentially multiple Members to send the
    * Packet to
    * 
    * @return true to multi-cast the Packet data
     */
    public boolean isMulticast(int cToMembers)
        {
        if (cToMembers <= 1)
            {
            return false;
            }
        
        int    cOtherMembers         = getMemberSet().size() - 1;
        double dflMulticastThreshold = getMulticastThreshold();
        
        return cToMembers > (int) (dflMulticastThreshold * cOtherMembers);
        }
    
    // Accessor for the property "MulticastEnabled"
    /**
     * Getter for property MulticastEnabled.<p>
    * Specifies whether or not the multicast is enabled.
     */
    public boolean isMulticastEnabled()
        {
        return __m_MulticastEnabled;
        }
    
    // Accessor for the property "NackEnabled"
    /**
     * Getter for property NackEnabled.<p>
    * Specifies if the receiver should use request packets, i.e. NACKs to do
    * early packet loss detection.
     */
    public boolean isNackEnabled()
        {
        return __m_NackEnabled;
        }
    
    /**
     * Called by the publisher to indicate a series of packets which have been
    * ack'd by a member.
    * 
    * @param listAcknowledged a series of newly ack'd packets.
     */
    protected void onAcknowledgedPacket(com.tangosol.coherence.component.net.Member member, java.util.Collection collAcknowledged)
        {
        // import Component.Net.Member$FlowControl as com.tangosol.coherence.component.net.Member.FlowControl;
        // import Component.Net.Packet.MessagePacket;
        // import com.tangosol.util.Base;
        // import java.util.Iterator;
        // import java.util.List;
        
        com.tangosol.coherence.component.net.Member.FlowControl flowControl = member.getFlowControl();
        if (flowControl != null)
            {
            // count the ack'd packets which would have
            // contributed to the outstanding packet counter
            // this must be done under synchronization to ensure
            // that the "outstanding" state doesn't get flipped
            // by the publisher as packets are declared as lost.
            int cNewAcks = 0;
        
            for (Iterator iter = collAcknowledged.iterator(); iter.hasNext(); )
                {
                MessagePacket packet = (MessagePacket) iter.next();
        
                if (packet.isDeferrable())
                    {
                    ++cNewAcks;
                    packet.setDeliveryState(MessagePacket.DELIVERY_CONFIRMED, member);
                    }
                }
          
            int cSeqAcks = flowControl.getSequentialConfirmedCount();
            int cSeqLost = flowControl.getSequentialLostCount();
        
            if (cSeqLost > 0)
                {
                flowControl.setPaused(false);
                flowControl.setSequentialLostCount(0);
                flowControl.setSequentialConfirmedCount(cNewAcks);
                }
            else
                {
                flowControl.setSequentialConfirmedCount(cSeqAcks + cNewAcks);
                }
        
            // auto adjust threshold
            if (flowControl.isAdjustable()) 
                {
                int     cThreshold     = flowControl.getOutstandingPacketThreshold();
                int     cEffectiveAcks = cSeqLost == 0 ? cSeqAcks : cSeqAcks / cSeqLost;
                int     iGoalDelta     = cEffectiveAcks - com.tangosol.coherence.component.net.Member.FlowControl.getSuccessGoal();
                boolean fNearCap       = flowControl.getOutstandingPacketHighMark() >
                                            (cThreshold - (cThreshold >>> 2));
        
                if (iGoalDelta < 0 && cSeqLost > 0)
                    {
                    // under goal and loosing packets, decrease threshold
                    cThreshold -= Math.max(1,
                        cThreshold / com.tangosol.coherence.component.net.Member.FlowControl.getAggressionFactor());
                    flowControl.setOutstandingPacketThreshold(cThreshold);
                    }
                else if (iGoalDelta > 0 && fNearCap)
                    {
                    // over goal, and we were recently near the capacity limit;
                    // increase according to the aggression factor
                    flowControl.setOutstandingPacketHighMark
                        (flowControl.getOutstandingPacketCount());
                    cThreshold += Math.max(1,
                        cThreshold / com.tangosol.coherence.component.net.Member.FlowControl.getAggressionFactor());
                    flowControl.setOutstandingPacketThreshold(cThreshold);
                    }
                }
        
            drainDeferredPackets(member);
            }
        }
    
    /**
     * Process Ack packet received from other members.
     */
    protected void onConfirmation(com.tangosol.coherence.component.net.packet.notifyPacket.Ack packetAck)
        {
        // import Component.Net.Member;
        // import Component.Net.Message;
        // import Component.Net.Packet;
        // import Component.Net.Packet.MessagePacket;
        // import Component.Util.WindowedArray;
        // import com.tangosol.net.internal.PacketComparator;
        // import com.tangosol.net.internal.PacketIdentifier;
        // import com.tangosol.util.Base;
        
        int    nFromId = packetAck.getFromId();
        Member member  = getMember(nFromId);
        if (member == null)
            {
            return;
            }
        
        // compute msg ids
        WindowedArray waMsg       = getMessageOutgoing();
        long          lMsgFirst   = waMsg.getFirstIndex();
        long          lMsgId      = 0L;
        int           cUniqueMsgs = 0;
        long[]        alMsgId;
        try
            {
            int cNotify = packetAck.getNotifyCount();
            alMsgId = ensureMsgIdArrayTemp(cNotify + 1);    
            for (int iNotify = 0, iLastTrint  = -1; iNotify < cNotify; ++iNotify)
                {
                int iMsgIdTrint = packetAck.getMessageId(iNotify);
                if (iMsgIdTrint != iLastTrint)
                    {
                    // minimize the number of message lookups needed based on assumption
                    // that the ack will contain many packets from the same message
                    lMsgId = Packet.translateTrint(iMsgIdTrint, lMsgFirst);
                    iLastTrint = iMsgIdTrint;
                    alMsgId[cUniqueMsgs++] = lMsgId;
                    }
                }
            }
        catch (Exception e)
            {
            _trace("Exception while translating Trints from Ack " + packetAck +
                   " front of outgoing message array is " + waMsg.get(lMsgFirst) +
                   " array window size is " + waMsg.getWindowSize(), 1);
            throw Base.ensureRuntimeException(e);
            }
        
        boolean          fUseNacks      = isNackEnabled();
        PacketIdentifier ptidNewestTo   = packetAck.getNewestFromPacketId(lMsgFirst);
        MessagePacket    packetNewestTo = null;
        Message[]        aMsgAck;
        
        if (fUseNacks && ptidNewestTo != null)
            {
            // before registering the Ack'd packets we must identify the "newest" previously
            // unack'd packet
            long lNewestMsgId = ptidNewestTo.getFromMessageId();
            if (lNewestMsgId != lMsgId)
                {
                alMsgId[cUniqueMsgs++] = lNewestMsgId;
                }
        
            // lookup only the unique msgs from ids
            aMsgAck = ensureMsgArrayTemp(cUniqueMsgs);
            waMsg.getAll(alMsgId, cUniqueMsgs, aMsgAck);   
        
            // determine if the newest packet was not previously ack'd
            Message msgNewest = aMsgAck[cUniqueMsgs - 1];
            int     iPacket   = ptidNewestTo.getMessagePartIndex();
            if (msgNewest != null && iPacket < msgNewest.getMessagePartCount())
                {
                packetNewestTo = msgNewest.getPacket(iPacket);
                // only use this as the "newest" packet if it has not yet been ack'd
                // it was already ack'd then it is useless to us here
                if (packetNewestTo != null &&
                    !packetNewestTo.isAddressedTo(nFromId))
                    {
                    packetNewestTo = null;
                    }
                }
            }
        else
            {
            // lookup only the unique msgs from ids
            aMsgAck = ensureMsgArrayTemp(cUniqueMsgs);
            waMsg.getAll(alMsgId, cUniqueMsgs, aMsgAck);
            }
        
        // register and confirm the Ack'd packets
        MessagePacket packetLast = registerAcks(packetAck, member, aMsgAck);
        
        // store last contiguous packet id that the other member has reported receiving
        // from us; this allows us to detect if we've missed Acks from this member and
        // avoid needlesly resending the affected packets
        PacketIdentifier ptidCont = packetAck.getContiguousFromPacketId(lMsgFirst);
        if (PacketComparator.compare(ptidCont, member.getContiguousToPacketId()) > 0)
            {
            member.setContiguousToPacketId(ptidCont);
            }
        
        // do Nack processing
        if (fUseNacks)
            {
            // do early loss detection based on the newest packet from us they know about
            // i.e. detect packets which we sent but they apparently didn't receive
            doEarlyLossDetection(member,
                packetNewestTo == null || packetNewestTo.isOutgoingMultipoint()
                    ? packetLast
                    : packetNewestTo,
                    packetAck.getNewestToPacketId
                                (member.getLastIncomingMessageId()));
            }
        
        // record stats
        Member memberThis = getThisMember();
        if (memberThis != null)
            {
            long lRecvTime = packetAck.getReceivedMillis();
        
            // update global "last received ACK"
            memberThis.setLastIncomingMillis(lRecvTime);
        
            if (member != memberThis)
                {
                // update point-to-point "last received ACK"
                member.setLastIncomingMillis(lRecvTime);
                }
            }
        }
    
    // Declared at the super level
    /**
     * Event notification called once the daemon's thread starts and before the
    * daemon thread goes into the "wait - perform" loop. Unlike the
    * <code>onInit()</code> event, this method executes on the daemon's thread.
    * 
    * Note1: this method is called while the caller's thread is still waiting
    * for a notification to  "unblock" itself.
    * Note2: any exception thrown by this method will terminate the thread
    * immediately
     */
    protected void onEnter()
        {
        super.onEnter();
        
        resetStats();
        }
    
    // Declared at the super level
    /**
     * This event occurs when an exception is thrown from onEnter, onWait,
    * onNotify and onExit.
    * 
    * If the exception should terminate the daemon, call stop(). The default
    * implementation prints debugging information and terminates the daemon.
    * 
    * @param e  the Throwable object (a RuntimeException or an Error)
    * 
    * @throws RuntimeException may be thrown; will terminate the daemon
    * @throws Error may be thrown; will terminate the daemon
     */
    public void onException(Throwable e)
        {
        super.onException(e);
        }
    
    // Declared at the super level
    /**
     * The "component has been initialized" method-notification (kind of
    * WM_NCCREATE event) called out of setConstructed() for the topmost
    * component and that in turn notifies all the children. <p>
    * 
    * This notification gets called before the control returns back to this
    * component instantiator (using <code>new Component.X()</code> or
    * <code>_newInstance(sName)</code>) and on the same thread. In addition,
    * visual components have a "posted" notification <code>onInitUI</code> that
    * is called after (or at the same time as)  the control returns back to the
    * instatiator and possibly on a different thread.
     */
    public void onInit()
        {
        // initialize OutgoingMessageArray
        setMessageOutgoing((PacketPublisher.OutgoingMessageArray) _findChild("OutgoingMessageArray"));
        
        // initialize Queues
        setAckQueue((PacketPublisher.AckQueue) _findChild("AckQueue"));
        setResendQueue((PacketPublisher.ResendQueue) _findChild("ResendQueue"));
        setConfirmationQueue((PacketPublisher.ConfirmationQueue) _findChild("ConfirmationQueue"));
        
        super.onInit();
        }
    
    /**
     * Called from the ClusterService, on either it's thread or possibly the
    * publisher or TcpRingListener thread to inform the publisher that a member
    * has left the cluster.
     */
    public void onMemberLeft(com.tangosol.coherence.component.net.Member member)
        {
        // import Component.Net.Member$FlowControl as com.tangosol.coherence.component.net.Member.FlowControl;
        // import Component.Util.Queue as com.tangosol.coherence.component.util.Queue;
        
        // NOTE: This method may run on ClusterService, TcpRing, or Publisher thread
        
        com.tangosol.coherence.component.net.Member.FlowControl flowControl = member.getFlowControl();
        if (flowControl != null)
            {
            com.tangosol.coherence.component.util.Queue queueDeferred = flowControl.getDeferredQueue();
            com.tangosol.coherence.component.util.Queue queueReady    = getDeferredReadyQueue();
            
            if (queueDeferred != queueReady)
                {
                // All packets which are sitting in the deferred queue need to be cleaned up.
                // They need to follow the normal life cycle of a packet so that they can be removed
                // from the OutgoingMessageArray
                
                synchronized (flowControl)
                    {
                    // prevent publisher from deferring any more packets for this member
                    // we must be sync'd on flowControl to make sure publisher won't add
                    // to the queue after we've reset it
                    flowControl.setDeferredQueue(getDeferredReadyQueue());
                    }
        
                // move over all packets from the deferred queue to the ready queue
                for (Object o = queueDeferred.removeNoWait();
                     o != null;
                     o = queueDeferred.removeNoWait())
                     {
                     queueReady.add(o);
                     }
                 }
            }
        }
    
    protected void onNoRecipientPacket(com.tangosol.coherence.component.net.Packet packet)
        {
        // import Component.Net.Packet.MessagePacket;
        
        // we have to put it back to the queue
        // to have the corresponding message removed
        if (packet.isConfirmationRequired())
            {
            ((MessagePacket) packet).clearRecipients();    
            getResendQueue().addHead(packet);
            }
        }
    
    // Declared at the super level
    /**
     * Event notification to perform a regular daemon activity. To get it
    * called, another thread has to set Notification to true:
    * <code>daemon.setNotification(true);</code>
    * 
    * @see #onWait
     */
    protected void onNotify()
        {
        // import Component.Net.Packet;
        // import Component.Net.Packet.MessagePacket;
        // import Component.Net.Packet.NotifyPacket.Ack;
        // import Component.Util.Daemon.QueueProcessor.PacketProcessor.PacketSpeaker$BundlingQueue as PacketSpeaker.BundlingQueue;
        // import Component.Util.Queue as com.tangosol.coherence.component.util.Queue;
        // import com.tangosol.util.Base;
        
        try
            {
            long    ldtStart       = Base.getLastSafeTimeMillis(); // it has just been primed in Daemon.run()
            com.tangosol.coherence.component.util.Queue[] aQueue         = getIncomingPacketQueues();
            com.tangosol.coherence.component.util.Queue   queueReady     = getDeferredReadyQueue();
            com.tangosol.coherence.component.util.Queue   queueConfirm   = getConfirmationQueue();
            com.tangosol.coherence.component.util.Queue   queueIn        = getPacketAdapterQueue();
            int     cQueues        = aQueue.length;
            int     cPacketsStart  = queueIn.size();
            int     cSentPackets   = 0;
            int     cResentPackets = 0;
            int     cEarlyPackets  = 0;
            boolean fMore;
            
            do
                {
                // incoming acks
                while (true)
                    {
                    Ack ack = (Ack) queueConfirm.removeNoWait();
                    if (ack == null)
                        {
                        break;
                        }
                    onConfirmation(ack);
                    }
        
                // outgoing packets
                fMore = false;
                for (int iQueue = 0, cDeferred = 0; iQueue < cQueues; )
                    {
                    com.tangosol.coherence.component.util.Queue  queue  = aQueue[iQueue];            
                    Packet packet = (Packet) queue.removeNoWait();
                    if (packet != null)
                        {
                        boolean fResend = packet.getSentMillis() != 0L;
                        int     cSent   = onPacket(packet);
                        fMore           = true;
                        cSentPackets   += cSent;
                        if (fResend)
                            {
                            cResentPackets += cSent;
                            if (((MessagePacket) packet).getPendingResendSkips() > 0)
                                {
                                // retransmitted early due to Nack
                                cEarlyPackets += cSent;
                                }
                            }
        
                        if (queue == queueReady)
                            {
                            // queueReady is processed last, see start()
                            if (cDeferred-- > 0)
                                {
                                // packets got deferred, to minimize deferred queue churning
                                // we give it extra attention, this will be at most two
                                // extra times per interation through the array of queues
                                continue;
                                }
                            }
                        else if (cSent == 0 && packet.isDeferrable())
                            {
                            ++cDeferred;
                            }
                        }
                    ++iQueue; // move on to next queue
                    }
        
                if (!fMore || cSentPackets >= 100)
                    {
                    // ensure we move the LastSafeTimeMillis value;
                    // 100 packets represent about 2ms worth volume of 1468b packets on 1gb nic
                    long ldtNow = Base.getSafeTimeMillis();
        
                    if (!fMore || cSentPackets >= 30000)
                        {
                        // ensure we periodically update statistics;
                        // 30,000 would represent about 1/2 second worth volume
        
                        int cPacketsNow = fMore ? queueIn.size() : 0;
        
                        ((PacketSpeaker.BundlingQueue) getSendQueue()).tuneVolumeThreshold(
                            cPacketsNow - cPacketsStart);
                    
                        setStatsSent       (getStatsSent()        + cSentPackets);
                        setStatsResent     (getStatsResent()      + cResentPackets);
                        setStatsResentEarly(getStatsResentEarly() + cEarlyPackets);
                        setStatsCpu        (getStatsCpu()         + ldtNow - ldtStart);
        
                        cSentPackets  = cResentPackets = cEarlyPackets = 0;
                        cPacketsStart = cPacketsNow;
                        ldtStart      = ldtNow;
                        }
                    }
                }
            while (fMore);
            }
        catch (Exception e)
            {
            if (isExiting())
                {
                // ignore exception
                return;
                }
        
            throw Base.ensureRuntimeException(e);
            }
        }
    
    /**
     * Handle a packet.
    * 
    * @param packet the packet to handle
    * 
    * @return the number of packets which will be transmitted as a result of
    * this operation
     */
    protected int onPacket(com.tangosol.coherence.component.net.Packet packet)
        {
        // import Component.Net.Member;
        // import Component.Net.MemberSet.SingleMemberSet;
        // import Component.Net.Packet;
        // import Component.Net.Packet.MessagePacket;
        // import Component.Net.Packet.MessagePacket.Broadcast;
        // import Component.Net.PacketBundle;
        // import com.tangosol.util.Base;
        
        // check for missing Acks before resending a packet this check is performed here
        // as well as during removal from the resendQueue as it allows for detection
        // after a packet has been sitting on the deferred queue
        if (packet.getSentMillis() > 0L && !verifyResendNecessary((MessagePacket) packet))
            {
            onNoRecipientPacket(packet);
            return 0;
            }
        
        // check if we need to defer the packet
        // TODO: add multipoint deferral support
        int nToId = packet.getToId();
        Member memberSingle = nToId > 0 ? getMember(nToId) : null;
        if (memberSingle != null && packet.isDeferrable() &&
                deferPacket(memberSingle, (MessagePacket) packet))
            {
            // packet needs to be deferred
            return 0;
            }
        
        // prepare packet for transmission
        long         ldtNow     = Base.getLastSafeTimeMillis();
        boolean      fReliable  = packet.isConfirmationRequired();
        int          cAddresses = 0;
        PacketBundle bundle     = new PacketBundle();
        
        bundle.addPacket(packet);
        
        // address the UdpPacket
        if (memberSingle == null)
            {
            // multipoint, broadcast or "no live receipients" packet
            cAddresses = addressMany(bundle);
            if (cAddresses == 0)
                {
                // the Packet is either addressed to a Member that is dead
                // (i.e. it isn't in the MasterMemberSet any more)
                // or is no longer addressed to any member
                // (i.e. resend packet which became fully acked between being
                // dequeued from the resend queue, and making it here
                onNoRecipientPacket(packet);
                return 0;
                }
            }
        else
            {
            // DIRECTED_ONE, SEQUEL_ONE, or ACK; inlined common case; either will fit in
            // a preferred packet
            bundle.setUdpSocket(getUdpSocketUnicast());
            bundle.addDestination(getSocketAddress(memberSingle, packet));
            bundle.setMaximumLength(Math.min(getPreferredPacketLength(),
                memberSingle.getPreferredPacketLength()));
        
            cAddresses = 1;
        
            if (fReliable)
                {
                memberSingle.setLastOutgoingMillis(ldtNow);
                }
            }
        
        // at this point we know for sure that the packet will be sent
        // so we do packet tracking
        onSendPacket(packet);
        
        int cSent = packet.getSentCount();
        if (cSent == 0)
            {
            // schedule the Packet for delivery
            getSendQueue().add(bundle);
            }
        else
            {
            // (COH-7310) don't queue resends to the speaker, this protects against
            // the publisher outpacing the speaker and potentially placing multiple
            // needless "copies" of the same packet on the queue.  This is especially
            // problematic in large WKA clusters as multipoint packets are not
            // flow-controlled and each one queue'd to the speaker will require N sends.
            bundle.send();
            ldtNow = Base.getLastSafeTimeMillis(); // signficant time may have passed
            }
        
        // update stats
        packet.setSentMillis(ldtNow);
        packet.setSentCount(cSent + 1);
        
        // requeue Packet for re-send if delivery is guaranteed
        if (fReliable)
            {
            getResendQueue().add(packet);
            }
        
        return cAddresses;
        }
    
    /**
     * Called by the publisher when a packet is ready to be sent on the network.
     */
    protected void onSendPacket(com.tangosol.coherence.component.net.Member member, com.tangosol.coherence.component.net.Packet packet)
        {
        // import Component.Net.Member$FlowControl as com.tangosol.coherence.component.net.Member.FlowControl;
        // import Component.Net.Packet.MessagePacket;
        
        com.tangosol.coherence.component.net.Member.FlowControl flowControl = member.getFlowControl();
        if (flowControl != null && packet.isDeferrable())
            {
            MessagePacket msgPacket = (MessagePacket) packet;
            
            if (msgPacket.getDeliveryState() != MessagePacket.DELIVERY_CONFIRMED)
                {
                msgPacket.setDeliveryState(MessagePacket.DELIVERY_OUTSTANDING, member);
                }
            }
        
        // track unipoint packet order for nacking
        if (isNackEnabled()
            && packet.isConfirmationRequired()
            && !packet.isOutgoingMultipoint())
            {
            if (packet.getSentMillis() == 0L)
                {
                // track the last "new" sent ackable packet id
                member.setNewestToPacketId((MessagePacket) packet);
                }
            // record packet send order, see receiver.onPacketAck
            member.getRecentPacketQueue().add(packet);
            }
        
        // update stats
        member.setStatsSent(member.getStatsSent() + 1);
        if (packet.getSentMillis() > 0)
            {
            member.setStatsResent(member.getStatsResent() + 1);
            }
        }
    
    /**
     * Called by the publisher when a packet is ready to be sent on the network.
     */
    protected void onSendPacket(com.tangosol.coherence.component.net.Packet packet)
        {
        // import Component.Net.Member;
        // import Component.Net.Packet.MessagePacket;
        
        if (packet.isOutgoingMultipoint())
            {
            int[] anToId = ((MessagePacket) packet).getToMemberSet().toIdArray();
            for (int i = 0, c = anToId.length; i < c; ++i)
                {
                Member member = getMember(anToId[i]);
                if (member != null)
                    {
                    onSendPacket(member, packet);
                    }
                }
            }
        else
            {
            Member member = getMember(packet.getToId());
            if (member != null)
                {
                onSendPacket(member, packet);
                }
            }
        }
    
    public void onSlowPacket(com.tangosol.coherence.component.net.packet.MessagePacket packet)
        {
        // this should be over-ridden where applicable;
        // this implementation does not do anything
        }
    
    public void onUndeliverablePacket(com.tangosol.coherence.component.net.packet.MessagePacket packet)
        {
        // import Component.Net.Member;
        // import Component.Net.MemberSet;
        
        // this should be over-ridden where applicable; this
        // implementation just kills all Members that have
        // not acknowledged the Packet
        
        int nMemberTo = packet.getToId();
        if (nMemberTo != 0)
            {
            Member member = getMember(nMemberTo);
            if (member != null)
                {
                getMemberSet().remove(member);
                }
            packet.setToId(0);
            }
        
        MemberSet setMemberTo = packet.getToMemberSet();
        if (setMemberTo != null)
            {
            getMemberSet().removeAll(setMemberTo);
            setMemberTo.clear();
            }
        }
    
    // Declared at the super level
    /**
     * Event notification called when  the daemon's Thread is waiting for work.
    * 
    * @see #run
     */
    protected void onWait()
            throws java.lang.InterruptedException
        {
        flushSend();
        super.onWait();
        }
    
    /**
     * Serialize and packetize the specified message.
    * 
    * This method executes on the client thread.
    * 
    * @return true if the message was packetized and should be sent on the
    * network, false if the message was not serialized (all addressees are gone)
     */
    public boolean packetizeMessage(com.tangosol.coherence.component.net.Message msg)
        {
        // import com.tangosol.io.MultiBufferReadBuffer;
        // import com.tangosol.io.MultiBufferWriteBuffer;
        // import com.tangosol.io.ReadBuffer;
        // import com.tangosol.io.nio.ByteBufferReadBuffer;
        // import com.tangosol.util.Base;
        // import com.oracle.coherence.common.base.Disposable;
        // import com.oracle.coherence.common.io.BufferSequence;
        // import java.io.IOException;
        
        // message couuld have already been serialized for "bus" transports
        ReadBuffer bufferRead;
        Disposable controller = msg.getBufferController();
        if (controller == null)
            {
            // write the message to the output
            try
                {
                MultiBufferWriteBuffer buffer =
                    new MultiBufferWriteBuffer(getMessageBufferAllocator());
                msg.setBufferController(buffer, 1);
        
                msg.getService().serializeMessage(msg, buffer.getBufferOutput());
        
                bufferRead = buffer.getUnsafeReadBuffer();
                }
            catch (IOException e)
                {
                throw Base.ensureRuntimeException(e);
                }
            }
        else // already serialized by a MessageHandler
            {
            // each member remaining in the member set has consumed a reference count, but the
            // publisher will only use one of these, so remove the extras
            for (int i = 0, c = msg.getToMemberSet().size() - 1; i < c; ++i)
                {
                msg.releaseOutgoing(/*fSuspect*/ true); // datagram delivery is always suspect
                }
        
            // we must also create a ReadBuffer for the buffer sequence
            BufferSequence bufseq = (BufferSequence) controller;
            int cBuffers = bufseq.getBufferCount();
            if (cBuffers == 1)
                {
                bufferRead = new ByteBufferReadBuffer(bufseq.getBuffer(0));
                }
            else
                {
                ReadBuffer[] abuf = new ReadBuffer[cBuffers];
                for (int i = 0; i < cBuffers; ++i)
                    {
                    abuf[i] = new ByteBufferReadBuffer(bufseq.getBuffer(i));
                    }
                bufferRead = new MultiBufferReadBuffer(abuf);
                }
            
            msg.setReadBuffer(bufferRead);
            }
        
        // divide the buffer into packets
        return msg.packetize(this, getMemberSet(), bufferRead,
            computePreferredPacketLength(msg.getToMemberSet()),
            getMaximumPacketLength());
        }
    
    // From interface: com.tangosol.internal.util.MessagePublisher
    public boolean post(Object oMsg)
        {
        return getQueue().add(oMsg);
        }
    
    /**
     * Register acknowledgement for packets from the Ack and return the last
    * newly ack'd packet.  This method may only be run on the publisher thread
    * as it makes use of a PacketListTemp property.
    * 
    * @param packetAck the packet containing the acks
    * @param member the member who sent the Ack
    * @param aMsgAck an array of unique messages, corresponding to the packets
    * in the Ack
    * 
    * @return the last unipoint newly ack'd packet
     */
    protected com.tangosol.coherence.component.net.packet.MessagePacket registerAcks(com.tangosol.coherence.component.net.packet.notifyPacket.Ack packetAck, com.tangosol.coherence.component.net.Member member, com.tangosol.coherence.component.net.Message[] aMsgAck)
        {
        // import Component.Net.Message;
        // import Component.Net.Packet.MessagePacket;
        // import java.util.List;
        
        Message       msgLast    = null;
        MessagePacket packetLast = null;
        List          listAckd   = getAckdPacketListTemp();
        int           cNotify    = packetAck.getNotifyCount();
        int           cNewAck    = 0;
        for (int i = 0, iLastTrint = -1, iMsg = 0;  i < cNotify; ++i)
            {
            // determine the associated message
            int iMsgIdTrint = packetAck.getMessageId(i);
            if (iMsgIdTrint != iLastTrint)
                {
                msgLast         = aMsgAck[iMsg];
                aMsgAck[iMsg++] = null;
                iLastTrint      = iMsgIdTrint;
                }
        
            // find the Packet from the message
            if (msgLast != null)
                {
                MessagePacket packetMsg = msgLast.getPacket
                        (packetAck.getMessagePartIndex(i));
        
                if (packetMsg != null && packetMsg.registerAck(member))
                    {
                    // the Member that sent the Ack Packet has just been removed from
                    // the "to" portion of the corresponding Packet, i.e. this is
                    // the first time it's been ack'd
                    listAckd.add(packetMsg);
                    if (!packetMsg.isOutgoingMultipoint())
                        {
                        packetLast = packetMsg;
                        }
                    ++cNewAck;
                    }
                }
            }
        
        if (cNewAck < cNotify)
            {
            // We had some duplicate Acks, indicating that we'd performed unneeded
            // resends. Due to out of order Acks we could overcount here, ensure
            // that excess <= resent
            setStatsResentExcess(Math.min(
                getStatsResent(),
                getStatsResentExcess() + (cNotify - cNewAck)));
            }
        
        onAcknowledgedPacket(member, listAckd);
        return packetLast;
        }
    
    /**
     * Reset the statistics.
     */
    public void resetStats()
        {
        // import com.tangosol.util.Base;
        
        setStatsSent        (0L);
        setStatsResent      (0L);
        setStatsResentEarly (0L);
        setStatsResentExcess(0L);
        setStatsNacksSent   (0L);
        setStatsCpu         (0L);
        setStatsReset       (Base.getSafeTimeMillis());
        }
    
    /**
     * Schedule an Ack (Nack) to be sent to the indicated member.  If there is
    * already a pending Ack for this member it will be rescheduled, otherwise a
    * new Ack will be created.  This method may be called by the publisher or
    * receiver.
    * 
    * @param member the member to send the Ack to
     */
    public void scheduleNack(com.tangosol.coherence.component.net.Member member)
        {
        // import Component.Net.Packet.NotifyPacket.Ack;
        // import com.tangosol.util.Base;
        
        Ack  packetAck = member.getPacketAck();
        long ldtSend   = Base.getSafeTimeMillis() + getNackDelayMillis();
        if (packetAck != null &&
            packetAck.getScheduledMillis() <= ldtSend)
            {
            if (!packetAck.isFlushed())
                {
                // there is already an open Ack scheduled to go out within this time
                // we don't need to do anything
                return;
                }
            // there is an ack which fits the schedule but it is already flushed
            // create a new one
            packetAck = null;    
            }
        
        if (packetAck == null)
            {
            packetAck = new Ack();
            packetAck.setFromId(getMemberId());
            packetAck.setToId(member.getId());
            }
        
        // schedule the nack or re-schedule the ack to the nack window
        packetAck.setScheduledMillis(ldtSend);
        
        setStatsNacksSent(getStatsNacksSent() + 1L);
        
        getAckQueue().addHead(packetAck);
        }
    
    /**
     * Send an out of band diagnostic packet.  This method may be called on
    * threads other then the publisher.
     */
    public void sendDiagnosticPacket(com.tangosol.coherence.component.net.packet.DiagnosticPacket packet)
        {
        // import Component.Net.Member;
        // import Component.Net.MemberSet.SingleMemberSet;
        // import Component.Net.PacketBundle;
        // import com.tangosol.util.Base;
        
        try
            { 
            Member memberTo = getMember(packet.getToId());
            if (memberTo == null)
                {
                _trace("Aborting diagnostic to unknown member " + packet, 3);
                return;
                }
        
            PacketBundle bundle = new PacketBundle();
            bundle.addPacket(packet);
        
            // address the packet
            // TODO: consider testing multicast as well
            packet.setFromId(getThisMember().getId());
            bundle.setUdpSocket(getUdpSocketUnicast());
            bundle.addDestination(packet.isPreferredPortUsed()
                ? memberTo.getPreferredSocketAddress()
                : memberTo.getSocketAddress());
        
            // stamp the sent time; unlike most packets this goes on the wire
            packet.setSentMillis(Base.getSafeTimeMillis());
        
            // stream the Packet data into the UdpPacket
            SingleMemberSet setTo = new SingleMemberSet();
            setTo.add(memberTo);
        
            // send the packet
            bundle.send();
            packet.setSentCount(1);
        
            _trace("Sent " + packet, 6);    
            }
        catch (Exception e)
            {
            if (!isExiting())
                {
                _trace("Failed to send " + packet + " due to exception: " + e, 1);
                }
            }
        }
    
    // Accessor for the property "AckDelay"
    /**
     * Setter for property AckDelay.<p>
    * The minimum number of milliseconds between the queueing of an Ack packet
    * and the sending of the same.
     */
    public void setAckDelay(int cMillis)
        {
        getAckQueue().setDelayMillis(cMillis);
        }
    
    // Accessor for the property "AckdPacketListTemp"
    /**
     * Setter for property AckdPacketListTemp.<p>
    * Used in the processing of Acks to avoid frequent creation of a temporary
    * list.
    * 
    * @see #registerAcks
    * @see #doEarlyLossDetection
     */
    protected void setAckdPacketListTemp(java.util.List listPacket)
        {
        __m_AckdPacketListTemp = listPacket;
        }
    
    // Accessor for the property "AckQueue"
    /**
     * Setter for property AckQueue.<p>
    * A reference to the AckQueue child, which is the Queue in which
    * Packet-notification Packets are placed so that they are automatically
    * after a configurable period of time.
     */
    protected void setAckQueue(PacketPublisher.AckQueue queue)
        {
        __m_AckQueue = queue;
        }
    
    // Accessor for the property "BroadcastAddresses"
    /**
     * Setter for property BroadcastAddresses.<p>
    * A Set<InetSocketAddress> used for broadcast in leu of multicast. This
    * list always contains pre-configured set of WellKnownAddresses, but could
    * additionally contain dynamically obtained addresses. This set can be
    * modified by the ClusterService.
     */
    public void setBroadcastAddresses(java.util.Set list)
        {
        __m_BroadcastAddresses = list;
        }
    
    // Accessor for the property "CloggedCount"
    /**
     * Setter for property CloggedCount.<p>
    * The maximum number of packets in the send plus resend queues before
    * determining that the publisher is clogged. Zero means no limit.
     */
    public void setCloggedCount(int cMaxPackets)
        {
        __m_CloggedCount = cMaxPackets;
        }
    
    // Accessor for the property "CloggedDelay"
    /**
     * Setter for property CloggedDelay.<p>
    * The number of milliseconds to pause client threads when a clog occurs, to
    * wait for the clog to dissipate. (The pause is repeated until the clog is
    * gone.) Anything less than one (e.g. zero) is treated as one.
     */
    public void setCloggedDelay(int cMillis)
        {
        __m_CloggedDelay = (Math.max(1, cMillis));
        }
    
    // Accessor for the property "ClusterNameBuffer"
    /**
     * Setter for property ClusterNameBuffer.<p>
    * This cluster's name length encoded into a ByteBuffer just as is used in
    * Broadcast packets.
     */
    public void setClusterNameBuffer(java.nio.ByteBuffer bufBuffer)
        {
        __m_ClusterNameBuffer = bufBuffer;
        }
    
    // Accessor for the property "ConfirmationQueue"
    /**
     * Setter for property ConfirmationQueue.<p>
    * A reference to the ConfirmationQueue child, which is the Queue in which
    * incomming Ack packets from other members are placed.
     */
    protected void setConfirmationQueue(PacketPublisher.ConfirmationQueue queue)
        {
        __m_ConfirmationQueue = queue;
        }
    
    // Accessor for the property "DeferredReadyQueue"
    /**
     * Setter for property DeferredReadyQueue.<p>
    * A queue of previously deferred packets which are ready to be sent.  This
    * value will be null if deferral is not enabled.
    * 
    * @see Member#DeferredQueue
     */
    public void setDeferredReadyQueue(com.tangosol.coherence.component.util.Queue queue)
        {
        __m_DeferredReadyQueue = queue;
        }
    
    // Accessor for the property "FromMessageId"
    /**
     * Setter for property FromMessageId.<p>
    * "allocator" for from message ids
     */
    protected void setFromMessageId(long lId)
        {
        __m_FromMessageId = lId;
        }
    
    // Accessor for the property "IncomingPacketQueues"
    /**
     * Setter for property IncomingPacketQueues.<p>
    * The set of queues responsible for supplying the publisher with packets.
     */
    protected void setIncomingPacketQueues(com.tangosol.coherence.component.util.Queue[] aQueue)
        {
        __m_IncomingPacketQueues = aQueue;
        }
    
    // Accessor for the property "LastTrafficJamWarningTimeMillis"
    /**
     * Setter for property LastTrafficJamWarningTimeMillis.<p>
    * Timestamp of the last time a warning was issued due to overly agressive
    * traffic jam settings.
    * 
    * @volatile, accessed by all client threads
     */
    protected void setLastTrafficJamWarningTimeMillis(long cMillis)
        {
        __m_LastTrafficJamWarningTimeMillis = cMillis;
        }
    
    // Accessor for the property "LostPacketListTemp"
    /**
     * Setter for property LostPacketListTemp.<p>
    * Used in the processing of Acks to avoid frequent creation of a temporary
    * list.
    * 
    * @see #doEarlyLossDetection
     */
    protected void setLostPacketListTemp(java.util.List listPacket)
        {
        __m_LostPacketListTemp = listPacket;
        }
    
    // Accessor for the property "MaximumPacketLength"
    /**
     * Setter for property MaximumPacketLength.<p>
    * The minimum safe packet length required to be supported by all cluster
    * members.
     */
    public void setMaximumPacketLength(int cbMax)
        {
        __m_MaximumPacketLength = cbMax;
        }
    
    // Accessor for the property "MessageBufferAllocator"
    /**
     * Setter for property MessageBufferAllocator.<p>
    * The pool of buffers used when serialize a Message.
     */
    public void setMessageBufferAllocator(com.tangosol.io.MultiBufferWriteBuffer.WriteBufferPool allocator)
        {
        __m_MessageBufferAllocator = allocator;
        }
    
    // Accessor for the property "MessageOutgoing"
    /**
     * Setter for property MessageOutgoing.<p>
    * The array of outgoing Message objects.
     */
    protected void setMessageOutgoing(com.tangosol.coherence.component.util.WindowedArray waMsg)
        {
        __m_MessageOutgoing = waMsg;
        }
    
    // Accessor for the property "MsgArrayTemp"
    /**
     * Setter for property MsgArrayTemp.<p>
    * Used in the processing of Acks to avoid frequent creation of a temporary
    * array.
    * 
    * @see #onPacketAck
    * @see #ensureMsgArrayTemp
     */
    protected void setMsgArrayTemp(com.tangosol.coherence.component.net.Message[] aMsg)
        {
        __m_MsgArrayTemp = aMsg;
        }
    
    // Accessor for the property "MsgIdArrayTemp"
    /**
     * Setter for property MsgIdArrayTemp.<p>
    * Used in the processing of Acks to avoid frequent creation of a temporary
    * array.
    * 
    * @see #onPacketAck
    * @see #ensureMsgIdArrayTemp
     */
    protected void setMsgIdArrayTemp(long[] alMsgId)
        {
        __m_MsgIdArrayTemp = alMsgId;
        }
    
    // Accessor for the property "MulticastAddress"
    /**
     * Setter for property MulticastAddress.<p>
    * The cluster's mulitcast address, if enabled
     */
    public void setMulticastAddress(java.net.InetSocketAddress addressMulticast)
        {
        __m_MulticastAddress = addressMulticast;
        }
    
    // Accessor for the property "MulticastBypassCount"
    /**
     * Setter for property MulticastBypassCount.<p>
    * A count of the number of future multicast transmissions which should be
    * skipped (turned into unicast).  This is set when it appears that
    * multicast may be flaky.
     */
    protected void setMulticastBypassCount(int nCount)
        {
        __m_MulticastBypassCount = nCount;
        }
    
    // Accessor for the property "MulticastEnabled"
    /**
     * Setter for property MulticastEnabled.<p>
    * Specifies whether or not the multicast is enabled.
     */
    public void setMulticastEnabled(boolean fEnabled)
        {
        __m_MulticastEnabled = fEnabled;
        }
    
    // Accessor for the property "MulticastThreshold"
    /**
     * Setter for property MulticastThreshold.<p>
    * The percentage (0.0 to 100.0) of the servers in the cluster that a packet
    * will be sent to, above which the packet will be multicasted and below
    * which it will be unicasted.
     */
    public void setMulticastThreshold(double dflThresholdPercent)
        {
        __m_MulticastThreshold = dflThresholdPercent;
        }
    
    // Accessor for the property "NackDelayMillis"
    /**
     * Setter for property NackDelayMillis.<p>
    * The number of milliseconds to delay the sending of a NACK after detecting
    * probable packet loss.
     */
    public void setNackDelayMillis(long cDelayMillis)
        {
        __m_NackDelayMillis = cDelayMillis;
        }
    
    // Accessor for the property "NackEnabled"
    /**
     * Setter for property NackEnabled.<p>
    * Specifies if the receiver should use request packets, i.e. NACKs to do
    * early packet loss detection.
     */
    public void setNackEnabled(boolean fEnabled)
        {
        __m_NackEnabled = fEnabled;
        }
    
    // Accessor for the property "PacketAdapterQueue"
    /**
     * Setter for property PacketAdapterQueue.<p>
    * The InQueue's PacketAdpater
     */
    protected void setPacketAdapterQueue(com.tangosol.coherence.component.util.Queue queue)
        {
        __m_PacketAdapterQueue = queue;
        }
    
    // Accessor for the property "PreferredPacketLength"
    /**
     * Setter for property PreferredPacketLength.<p>
    * The "preferred" (optimal) length of each outgoing Packet.
    * 
    * @see ClusterConfig
     */
    public void setPreferredPacketLength(int cbPacket)
        {
        __m_PreferredPacketLength = cbPacket;
        }
    
    // Accessor for the property "ResendDelay"
    /**
     * Setter for property ResendDelay.<p>
    * The minimum number of milliseconds that a Packet will remain queued in
    * the ResendQueue before being removed from the front of the Queue to be
    * resent.
    * 
    * (2-way calculated; do not design.)
     */
    public void setResendDelay(int cMillis)
        {
        PacketPublisher.ResendQueue queue = getResendQueue();
        if (queue != null)
            {
            queue.setResendMillis(cMillis);
            }
        }
    
    // Accessor for the property "ResendQueue"
    /**
     * Setter for property ResendQueue.<p>
    * A reference to the ResendQueue child, which is the Queue in which
    * guaranteed-delivery Packets are placed so that they are automatically
    * resent.
     */
    protected void setResendQueue(PacketPublisher.ResendQueue queue)
        {
        __m_ResendQueue = queue;
        }
    
    // Accessor for the property "ResendTimeout"
    /**
     * Setter for property ResendTimeout.<p>
    * The maximum number of milliseconds that a Packet will be resent before it
    * is assumed that the remaining unacknowledging Members have died.
    * 
    * (2-way calculated; do not design.)
     */
    public void setResendTimeout(int cMillis)
        {
        PacketPublisher.ResendQueue queue = getResendQueue();
        if (queue != null)
            {
            queue.setTimeoutMillis(cMillis);
            }
        }
    
    // Accessor for the property "SendQueue"
    /**
     * Setter for property SendQueue.<p>
    * The Queue on which OutgoingUdpPackets are enqueued for processing by the
    * Speaker.
     */
    public void setSendQueue(com.tangosol.coherence.component.util.Queue queue)
        {
        __m_SendQueue = queue;
        }
    
    // Accessor for the property "SingleMemberSetTemp"
    /**
     * Setter for property SingleMemberSetTemp.<p>
    * A cached SingleMemberSet object that is used to get a snap-shot of the
    * MemberSet that a DIRECTED_ONE Packet is being delivered to. Used by
    * onPacket.
    * 
    * Unlike the getMemberSetTemp getSingleMeberSetTemp does not clear the set
    * before returning it.
     */
    protected void setSingleMemberSetTemp(com.tangosol.coherence.component.net.memberSet.SingleMemberSet set)
        {
        __m_SingleMemberSetTemp = set;
        }
    
    // Accessor for the property "StatsCpu"
    /**
     * Setter for property StatsCpu.<p>
    * Statistics: total time spent while sending packets.
     */
    protected void setStatsCpu(long cMillis)
        {
        __m_StatsCpu = cMillis;
        }
    
    // Accessor for the property "StatsNacksSent"
    /**
     * Setter for property StatsNacksSent.<p>
    * Statistics: total number of Nack packets (early Acks) sent.
     */
    protected void setStatsNacksSent(long cPackets)
        {
        __m_StatsNacksSent = cPackets;
        }
    
    // Accessor for the property "StatsResent"
    /**
     * Setter for property StatsResent.<p>
    * Statistics: total number of re-sent packets.
     */
    protected void setStatsResent(long cPackets)
        {
        __m_StatsResent = cPackets;
        }
    
    // Accessor for the property "StatsResentEarly"
    /**
     * Setter for property StatsResentEarly.<p>
    * Statistics: total number of packets re-sent ahead of schedule, i.e. due
    * to a NACK.
     */
    protected void setStatsResentEarly(long cPackets)
        {
        __m_StatsResentEarly = cPackets;
        }
    
    // Accessor for the property "StatsResentExcess"
    /**
     * Setter for property StatsResentExcess.<p>
    * Statistics: total number of repeated package acknowledgements.
     */
    protected void setStatsResentExcess(long cPackets)
        {
        __m_StatsResentExcess = cPackets;
        }
    
    // Accessor for the property "StatsReset"
    /**
     * Setter for property StatsReset.<p>
    * Statistics: Date/time value that the stats have been reset.
     */
    protected void setStatsReset(long lMillis)
        {
        __m_StatsReset = lMillis;
        }
    
    // Accessor for the property "StatsSent"
    /**
     * Setter for property StatsSent.<p>
    * Statistics: total number of sent packets.
     */
    protected void setStatsSent(long cPackets)
        {
        __m_StatsSent = cPackets;
        }
    
    // Accessor for the property "UdpSocketMulticast"
    /**
     * Setter for property UdpSocketMulticast.<p>
     */
    public void setUdpSocketMulticast(com.tangosol.coherence.component.net.socket.UdpSocket socket)
        {
        __m_UdpSocketMulticast = socket;
        }
    
    // Accessor for the property "UdpSocketUnicast"
    /**
     * Setter for property UdpSocketUnicast.<p>
     */
    public void setUdpSocketUnicast(com.tangosol.coherence.component.net.socket.udpSocket.UnicastUdpSocket socket)
        {
        __m_UdpSocketUnicast = socket;
        }
    
    // Declared at the super level
    /**
     * Starts the daemon thread associated with this component. If the thread is
    * already starting or has started, invoking this method has no effect.
    * 
    * Synchronization is used here to verify that the start of the thread
    * occurs; the lock is obtained before the thread is started, and the daemon
    * thread notifies back that it has started from the run() method.
     */
    public synchronized void start()
        {
        // import Component.Util.Queue as com.tangosol.coherence.component.util.Queue;
        
        if (getSendQueue() == null)
            {
            throw new IllegalStateException("SendQueue is required!");
            }
        
        if (getMaximumPacketLength() == 0)
            {
            throw new IllegalStateException("MaximumPacketLength is required!");
            }
        
        if (getPreferredPacketLength() == 0)
            {
            throw new IllegalStateException("PreferredPacketLength is required!");
            }
        
        if (getUdpSocketUnicast() == null)
            {
            throw new IllegalStateException("UdpSocketUnicast is required!");
            }
        
        if (getUdpSocketMulticast() == null && isMulticastEnabled())
            {
            throw new IllegalStateException("UdpSocketMulticast is required!");
            }
        
        // setup the packet adapter
        setPacketAdapterQueue(((PacketPublisher.InQueue) getQueue()).getPacketAdapter());
        
        // initialize packet queue array
        // this can't be done in onInit as we would not have been configured
        // with a deferred queue yet
        com.tangosol.coherence.component.util.Queue queueDeferred = getDeferredReadyQueue();
        com.tangosol.coherence.component.util.Queue aQueue[]      = new com.tangosol.coherence.component.util.Queue[queueDeferred == null ? 3 : 4];
        int   iQueue        = 0;
        
        // ackQueues should be first
        aQueue[iQueue++] = getAckQueue();
        aQueue[iQueue++] = getPacketAdapterQueue();
        aQueue[iQueue++] = getResendQueue();
        if (queueDeferred != null)
            {
            // deferred ready queue must come after all others
            // see onNotify()
            aQueue[iQueue++] = queueDeferred;
            }
        setIncomingPacketQueues(aQueue);
        
        super.start();
        }
    
    // Declared at the super level
    /**
     * Stops the daemon thread associated with this component.
     */
    public void stop()
        {
        // import Component.Net.Socket.UdpSocket;
        
        super.stop();
        
        try
            {
            UdpSocket socket = getUdpSocketMulticast();
            if (socket != null)
                {
                socket.close();
                }
            }
        catch (Throwable e) {}
        }
    
    // Declared at the super level
    public String toString()
        {
        return get_Name() + ':' + formatStats();
        }
    
    /**
     * Check if the packet was unacked due to a missing Ack, rather then the
    * packet not being received by the recipient.  If missing Acks are detected
    * they will be applied.
    * 
    * @return true if the packet still needs to be resent
     */
    public boolean verifyResendNecessary(com.tangosol.coherence.component.net.packet.MessagePacket packet)
        {
        // import Component.Net.Member;
        // import Component.Net.Packet.MessagePacket;
        // import com.tangosol.net.internal.PacketComparator;
        // import java.util.Collections;
        
        PacketPublisher publisher    = (PacketPublisher) get_Module();
        long    lMsgId       = packet.getFromMessageId();
        boolean fLiveMembers = false;
        
        if (packet.isOutgoingMultipoint())
            {
            int[] anToId = packet.getToMemberSet().toIdArray();
        
            for (int i = 0, c = anToId.length; i < c; ++i)
                {
                Member member = publisher.getMember(anToId[i]);
                if (member != null)
                    {
                    fLiveMembers = true;
                    if (PacketComparator.compare(member.getContiguousToPacketId(), packet) >= 0 &&
                        packet.registerAck(member))
                        {
                        onAcknowledgedPacket(member, Collections.singleton(packet));
                        }
                    }
                }
            }
        else
            {
            Member member = publisher.getMember(packet.getToId());
            if (member != null)
                {
                fLiveMembers = true;
                if (PacketComparator.compare(member.getContiguousToPacketId(), packet) >= 0 &&
                    packet.registerAck(member))
                    {
                    onAcknowledgedPacket(member, Collections.singleton(packet));
                    }
                }
            }
        
        return fLiveMembers && packet.isResendNecessary();
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketPublisher$AckQueue
    
    /**
     * The DualQueue is optimized for the producer consumer use case.
     * 
     * Producers work on the tail of the queue, consumers operate on the head
     * of the queue.  The two portions of the queue are maintained as seperate
     * lists, and protected by seperate locks.
     * 
     * When a consumer looks at the head of the queue, if it is empty, the head
     * and tail will be swaped.
     * 
     * The PacketPublisher's AckQueue child is used to queue Ack Packets such
     * that they can be slightly delayed to allow multiple acknowledgements to
     * be bundled together.  This queue will cross notify the Publisher's input
     * queue as needed.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class AckQueue
            extends    com.tangosol.coherence.component.util.queue.concurrentQueue.DualQueue
        {
        // ---- Fields declarations ----
        
        /**
         * Property DelayMillis
         *
         * The minimum number of milliseconds that a Packet will remain queued
         * in the queue before being removed from the front of the queue.
         * 
         * (This property is designable.)
         */
        private int __m_DelayMillis;
        
        /**
         * Property LastKnownReadyIndex
         *
         * The logical index of the last packet known to be ready to be sent. 
         * This index spans both lists within the queue, i.e. if it is greater
         * then the length of the head the packet is in the tail.  Note that
         * this does not mean that there are no other ready packets after this
         * one, just that this is the last one we know for sure is ready.
         * 
         * If negative there is no known ready packet.
         */
        private int __m_LastKnownReadyIndex;
        
        /**
         * Property UPDATE_ONLY_FLAG
         *
         * Constant indicating that an "added" entry resulted in an update
         * rather then an add.  Value is 0x100000000L, beyond Integer domain.
         */
        public static final long UPDATE_ONLY_FLAG = 4294967296L;
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
            __mapChildren.put("Iterator", PacketPublisher.AckQueue.Iterator.get_CLASS());
            }
        
        // Default constructor
        public AckQueue()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public AckQueue(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                setBatchSize(1);
                setDelayMillis(10);
                setElementList(new com.tangosol.util.RecyclingLinkedList());
                setHeadElementList(new com.tangosol.util.RecyclingLinkedList());
                setHeadLock(new java.lang.Object());
                setLastKnownReadyIndex(-1);
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketPublisher.AckQueue();
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
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/packetProcessor/PacketPublisher$AckQueue".replace('/', '.'));
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
         * Appends the specified element to the end of this queue.
        * 
        * Queues may place limitations on what elements may be added to this
        * Queue.  In particular, some Queues will impose restrictions on the
        * type of elements that may be added. Queue implementations should
        * clearly specify in their documentation any restrictions on what
        * elements may be added.
        * 
        * @param oElement element to be appended to this Queue
        * 
        * @return true (as per the general contract of the Collection#add
        * method)
        * 
        * @throws ClassCastException if the class of the specified element
        * prevents it from being added to this Queue
         */
        public boolean add(Object oElement)
            {
            // import Component.Net.Member;
            // import Component.Net.MemberSet;
            // import Component.Net.Packet.NotifyPacket.Ack;
            // import com.tangosol.util.Base;
              
            // schedule send
            Ack packet = (Ack) oElement;
            int cDelayMillis = getDelayMillis();
            if (cDelayMillis > 0)
                {
                packet.setScheduledMillis(Base.getSafeTimeMillis() + cDelayMillis);
                }
            
            MemberSet memberSet = ((PacketPublisher) get_Module()).getMemberSet();
            Member    member    = memberSet.getMember(packet.getToId());
            if (member != null)
                {
                // allow other acks to the same Member to be placed
                // into this Packet by letting the Packet be found
                // easily from the Member
                member.setPacketAck(packet);
                }
            
            // add the Packet to the queue
            return super.add(packet);
            }
        
        // Declared at the super level
        /**
         * Schedules a packet for early departure by placeing it in the queue
        * based on its pre-configured send time.  The added packet will not be
        * allowed to cut in front of other packets which have a scheduled
        * departure time, less then its own.
        * 
        * @see #add
        * 
        * @return true if the added packet is the first in the queue
         */
        public boolean addHead(Object oElement)
            {
            // import Component.Net.Member;
            // import Component.Net.Packet.NotifyPacket.Ack;
            // import com.tangosol.util.Base;
            // import java.util.List;
            
            // schedule the packet for early departure, but don't let it cut in front of
            // other packets which are ripe
            Ack    packet = (Ack) oElement;
            Member member = ((PacketPublisher) get_Module()).getMember(packet.getToId());
            long   ldtNow = Base.getSafeTimeMillis();
            if (member != null)
                {
                // allow other acks to the same Member to be placed
                // into this Packet by letting the Packet be found
                // easily from the Member
                member.setPacketAck(packet);
                }
            
            boolean fAdded  = false;
            int     iIndex;
            synchronized (getHeadLock())
                {
                List listHead  = getHeadElementList();
                int  cHeadSize = listHead.size();
                int  iLast     = getLastKnownReadyIndex(); // index spaning the head & tail
                
                // determine if the search will start in the head or tail
                if (iLast >= cHeadSize || cHeadSize == 0)
                    {       
                    // search in tail only
                    int  iStart   = iLast < 0 ? 0 : iLast - cHeadSize;
                    List listTail = getElementList();
                    long lResult  = orderedInsert(listTail, iStart, packet);
                    if (lResult < 0L)
                        {
                        iIndex = listTail.size();
                        listTail.add(iIndex, packet);
                        fAdded = true;
                        }
                    else
                        {
                        iIndex = (int) lResult;
                        fAdded = (lResult & UPDATE_ONLY_FLAG) == 0L;
                        }
                    iIndex += cHeadSize; // adjust for the head
                    }
                else
                    {
                    // search in head and tail
                    int  iStart  = iLast < 0 ? 0 : iLast;
                    long lResult = orderedInsert(listHead, iStart, packet);
                    
                    if (lResult < 0L)
                        {
                        List listTail = getElementList();
                        
                        lResult = orderedInsert(listTail, iStart, packet);
                        if (lResult < 0L)
                            {
                            iIndex = listTail.size();
                            listTail.add(iIndex, packet);
                            fAdded = true;
                            }
                         else
                            {
                            iIndex = (int) lResult;
                            fAdded = (lResult & UPDATE_ONLY_FLAG) == 0L;
                            }
                        iIndex += cHeadSize; // adjust for the head
                        }
                    else
                        {
                        iIndex = (int) lResult;
                        fAdded = (lResult & UPDATE_ONLY_FLAG) == 0L;
                        }
                    }
            
                // if this packet is ready to be sent update the last know
                if (packet.getScheduledMillis() <= ldtNow)
                    {
                    setLastKnownReadyIndex(iIndex);
                    }
                }
            
            // it is important that any potential flush happens outside of
            // this queue's synchronization, in order to prevent deadlock
            if (fAdded)
                {
                onAddElement();
                }
            
            // if it is the first element we must notify so that the publisher
            // can update its waitMillis time
            if (iIndex == 0)
                {
                updateFlushState(FLUSH_PENDING); // force flush
                flush();
                }
                
            return true;
            }
        
        // Declared at the super level
        /**
         * Flush the queue.
        * 
        * @param fAuto iff the flush was invoked automatically based on the
        * notification batch size
         */
        protected void flush(boolean fAuto)
            {
            // as this queue can be used by the publisher and receiver we allow
            // the publisher to skip self-notification.
            if (((PacketPublisher) get_Module()).getThread() != Thread.currentThread())
                {
                super.flush(fAuto);
                }
            }
        
        // Accessor for the property "DelayMillis"
        /**
         * Getter for property DelayMillis.<p>
        * The minimum number of milliseconds that a Packet will remain queued
        * in the queue before being removed from the front of the queue.
        * 
        * (This property is designable.)
         */
        public int getDelayMillis()
            {
            return __m_DelayMillis;
            }
        
        // Accessor for the property "LastKnownReadyIndex"
        /**
         * Getter for property LastKnownReadyIndex.<p>
        * The logical index of the last packet known to be ready to be sent. 
        * This index spans both lists within the queue, i.e. if it is greater
        * then the length of the head the packet is in the tail.  Note that
        * this does not mean that there are no other ready packets after this
        * one, just that this is the last one we know for sure is ready.
        * 
        * If negative there is no known ready packet.
         */
        protected int getLastKnownReadyIndex()
            {
            return __m_LastKnownReadyIndex;
            }
        
        // Accessor for the property "WaitMillis"
        /**
         * Getter for property WaitMillis.<p>
        * (calculated) The number of milliseconds that the current thread
        * should rest (Object.wait) for the next Packet to be ready to come off
        * the front of the queue.
         */
        public long getWaitMillis()
            {
            // import Component.Net.Packet.NotifyPacket.Ack;
            // import com.tangosol.util.Base;
            
            // - if the next packet is ready, don't wait
            // - avoid returning 0 unless the queue is empty
            
            Ack packet = (Ack) peekNoWait();
            if (packet == null)
                {
                return 0L;
                }
            
            long ldtScheduled = packet.getScheduledMillis();
            if (ldtScheduled < 0L)
                {
                return -1L;
                }
            
            long cMillisWait = ldtScheduled - Base.getSafeTimeMillis();
            return cMillisWait <= 0L ? -1L : cMillisWait;
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
            super.onInit();
            
            // setup cross notification
            setNotifier(((PacketPublisher) get_Module()).getNotifier());
            }
        
        /**
         * Scan the list and insert the supplied packet just before the first
        * packet which has a later resend time.  Return the insertion index, or
        * -1 if it the packet was not inserted.
        * 
        * If the element to be inserted is encountered during the earch, the
        * index is decorated by UPDATE_ONLY_FLAG, to differentiate it from an
        * insertion.
         */
        protected long orderedInsert(java.util.List list, int iStart, com.tangosol.coherence.component.net.packet.notifyPacket.Ack packet)
            {
            // import Component.Net.Packet.NotifyPacket.Ack;
            
            long ldtScheduled = packet.getScheduledMillis();
            for (int i = iStart, c = list.size(); i < c; ++i)
                {
                Ack pktCur = (Ack) list.get(i);
                if (pktCur == packet)
                    {
                    // we've encounterd the same packet we were trying to add
                    // don't add it, simply return its current index
                    return UPDATE_ONLY_FLAG | i; // encode that there is no add
                    }
                else if (pktCur.getScheduledMillis() > ldtScheduled)
                    {
                    list.add(i, packet);
                    return i;
                    }
                }
            
            // no insertion point found
            return -1L;
            }
        
        // Declared at the super level
        /**
         * Waits for and removes the first element from the front of this Queue.
        * 
        * If the Queue is empty, this method will block until an element is in
        * the Queue or until the specified wait time has passed. The
        * non-blocking equivalent of this method is "removeNoWait".
        * 
        * @param cMillis  the number of ms to wait for an element; pass 0 to
        * wait indefinitely
        * 
        * @return the first element in the front of this Queue or null if the
        * Queue is empty after the specified wait time has passed
        * 
        * @see #removeNoWait
         */
        public Object remove(long cMillis)
            {
            throw new UnsupportedOperationException();
            }
        
        // Declared at the super level
        /**
         * Removes and returns the first element from the front of this Queue.
        * If the Queue is empty, no element is returned.
        * 
        * The blocking equivalent of this method is "remove".
        * 
        * @return the first element in the front of this Queue or null if the
        * Queue is empty
        * 
        * @see #remove
         */
        public Object removeNoWait()
            {
            // import Component.Net.Packet.NotifyPacket.Ack;
            // import com.tangosol.util.Base;
            // import java.util.List;
            
            Ack     packet;
            long    ldtSchedule;
            long    ldtNow    = 0L;
            PacketPublisher publisher = (PacketPublisher) get_Module();
            synchronized (getHeadLock())
                {
                do
                    {
                    packet = (Ack) peekNoWait();
                    if (packet == null)
                        {
                        // no packet
                        return null;
                        }
            
                    // there is a packet, see if it is ready
                    ldtSchedule = packet.getScheduledMillis();
                    if (ldtNow == 0L)
                        {
                        // read the current time as late as possible
                        // but at most once
                        ldtNow = Base.getSafeTimeMillis();
                        }
            
                    if (ldtSchedule > ldtNow)
                        {
                        // next ack is not ready
                        return null;
                        }
            
                    if (packet == super.removeNoWait())
                        {
                        setLastKnownReadyIndex(getLastKnownReadyIndex() - 1);
                        }
                    else
                        {
                        throw new IllegalStateException();
                        }
            
                    // skip if packet was already sent
                    // see Receiver.confirm
                    }
                while (packet.getSentMillis() > 0L);
                }
            
            packet.close(publisher.getMember(packet.getToId()));
            
            return packet;
            }
        
        // Accessor for the property "DelayMillis"
        /**
         * Setter for property DelayMillis.<p>
        * The minimum number of milliseconds that a Packet will remain queued
        * in the queue before being removed from the front of the queue.
        * 
        * (This property is designable.)
         */
        public void setDelayMillis(int cMillis)
            {
            __m_DelayMillis = (Math.max(1, cMillis));
            }
        
        // Accessor for the property "LastKnownReadyIndex"
        /**
         * Setter for property LastKnownReadyIndex.<p>
        * The logical index of the last packet known to be ready to be sent. 
        * This index spans both lists within the queue, i.e. if it is greater
        * then the length of the head the packet is in the tail.  Note that
        * this does not mean that there are no other ready packets after this
        * one, just that this is the last one we know for sure is ready.
        * 
        * If negative there is no known ready packet.
         */
        protected void setLastKnownReadyIndex(int i)
            {
            __m_LastKnownReadyIndex = i;
            }

        // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketPublisher$AckQueue$Iterator
        
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
                return new com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketPublisher.AckQueue.Iterator();
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
                    clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/packetProcessor/PacketPublisher$AckQueue$Iterator".replace('/', '.'));
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

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketPublisher$ConfirmationQueue
    
    /**
     * SingleConsumerQueue is a concurrent queue optimized for multi producer
     * single consumer workloads.  More specifically it is not safe to consume
     * from multiple threads.
     * 
     * The PacketPublisher's ConfirmationQueue holds Ack packets received from
     * other members, which must be processed as confirmations to packets sent
     * by this member.  This queue will cross notify the Publisher's input
     * queue as needed.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class ConfirmationQueue
            extends    com.tangosol.coherence.component.util.queue.SingleConsumerQueue
        {
        // ---- Fields declarations ----
        
        // Default constructor
        public ConfirmationQueue()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public ConfirmationQueue(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                setBatchSize(1);
                setDelegate(new com.oracle.coherence.common.collections.ConcurrentLinkedQueue());
                setNotifier(new com.oracle.coherence.common.base.SingleWaiterMultiNotifier());
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketPublisher.ConfirmationQueue();
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
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/packetProcessor/PacketPublisher$ConfirmationQueue".replace('/', '.'));
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
            // import com.tangosol.coherence.config.Config;
            
            super.onInit();
            
            // setup cross notification
            setNotifier(((PacketPublisher) get_Module()).getNotifier());
            
            setBatchSize(Integer.parseInt(
                Config.getProperty("coherence.publisher.batch", "8")));
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketPublisher$InQueue
    
    /**
     * SingleConsumerQueue is a concurrent queue optimized for multi producer
     * single consumer workloads.  More specifically it is not safe to consume
     * from multiple threads.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class InQueue
            extends    com.tangosol.coherence.component.util.queue.SingleConsumerQueue
        {
        // ---- Fields declarations ----
        
        /**
         * Property PacketAdapter
         *
         */
        private transient PacketPublisher.InQueue.PacketAdapter __m_PacketAdapter;
        
        /**
         * Property YieldOnFlush
         *
         * If true then the calling thread will yield upon an explicit flush.
         * 
         * An explicit flush is an indication that the calling thread is going
         * to block. Testing has shown that heavily multi-threaded applications
         * benefit significantly from performing this yield.  As we assume that
         * the caller is about to block, yielding to other threads is not
         * unreasonable.  Note that single threaded client/server tests show no
         * performance degradation either.
         * 
         * This feature may be controlled via the
         * tangosol.coherence.publisher.yieldonflush system property.
         */
        private boolean __m_YieldOnFlush;
        
        // Default constructor
        public InQueue()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public InQueue(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                setBatchSize(1);
                setDelegate(new com.oracle.coherence.common.collections.ConcurrentLinkedQueue());
                setNotifier(new com.oracle.coherence.common.base.SingleWaiterMultiNotifier());
                }
            catch (java.lang.Exception e)
                {
                // re-throw as a runtime exception
                throw new com.tangosol.util.WrapperException(e);
                }
            
            // containment initialization: children
            _addChild(new PacketPublisher.InQueue.PacketAdapter("PacketAdapter", this, true), "PacketAdapter");
            
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketPublisher.InQueue();
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
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/packetProcessor/PacketPublisher$InQueue".replace('/', '.'));
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
         * Appends the specified element to the end of this queue.
        * 
        * Queues may place limitations on what elements may be added to this
        * Queue.  In particular, some Queues will impose restrictions on the
        * type of elements that may be added. Queue implementations should
        * clearly specify in their documentation any restrictions on what
        * elements may be added.
        * 
        * @param oElement element to be appended to this Queue
        * 
        * @return true (as per the general contract of the Collection.add
        * method)
        * 
        * @throws ClassCastException if the class of the specified element
        * prevents it from being added to this Queue
         */
        public boolean add(Object oElement)
            {
            // import Component.Net.Message;
            // import com.tangosol.util.Base;
            
            PacketPublisher publisher = (PacketPublisher) get_Module();
            Message message   = (Message) oElement;
            
            // packetize the message and enqueue
            if (!publisher.packetizeMessage(message))
                {
                message.releaseOutgoing(/*fSuspect*/ true);
                return false;
                }
            
            try
                {
                super.add(message);
            
                // increment packet counter based on the size of the message
                // decrement is done by PacketAdapter.removeNoWait
                // packetCounter must be incremented before notification
            
                getPacketAdapter().getPacketCounter().
                    addAndGet(message.getMessagePartCount());
            
                return true;
                }
            catch (Throwable e)
                {
                // we may have corrupted the queue, need to fail fast
                // this will stop the cluster node
                publisher.onException(e);
                
                throw Base.ensureRuntimeException(e);
                }
            }
        
        // Declared at the super level
        /**
         * Flush the queue.
         */
        public void flush()
            {
            super.flush();
            
            if (isYieldOnFlush() &&
                Thread.currentThread().getThreadGroup() !=
                ((PacketPublisher) get_Module()).getThreadGroup())
                {
                Thread.yield();
                }
            }
        
        // Accessor for the property "PacketAdapter"
        /**
         * Getter for property PacketAdapter.<p>
         */
        public PacketPublisher.InQueue.PacketAdapter getPacketAdapter()
            {
            return __m_PacketAdapter;
            }
        
        // Accessor for the property "YieldOnFlush"
        /**
         * Getter for property YieldOnFlush.<p>
        * If true then the calling thread will yield upon an explicit flush.
        * 
        * An explicit flush is an indication that the calling thread is going
        * to block. Testing has shown that heavily multi-threaded applications
        * benefit significantly from performing this yield.  As we assume that
        * the caller is about to block, yielding to other threads is not
        * unreasonable.  Note that single threaded client/server tests show no
        * performance degradation either.
        * 
        * This feature may be controlled via the
        * tangosol.coherence.publisher.yieldonflush system property.
         */
        public boolean isYieldOnFlush()
            {
            return __m_YieldOnFlush;
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
            // import com.tangosol.coherence.config.Config;
            
            setPacketAdapter((PacketPublisher.InQueue.PacketAdapter) _findChild("PacketAdapter"));
            
            setBatchSize(Integer.parseInt(
                Config.getProperty("coherence.publisher.batch", "8")));
            
            setYieldOnFlush(Boolean.valueOf(
                Config.getProperty("coherence.publisher.yieldonflush", "false"))
                    .booleanValue());
            
            super.onInit();
            }
        
        // Declared at the super level
        /**
         * Removes and returns the first element from the front of this Queue.
        * If the Queue is empty, no element is returned.
        * 
        * The blocking equivalent of this method is "remove".
        * 
        * @return the first element in the front of this Queue or null if the
        * Queue is empty
        * 
        * @see #remove
         */
        public Object removeNoWait()
            {
            // import Component.Net.Message;
            
            Message msg = (Message) super.removeNoWait();
            
            if (msg != null)
                {
                ((PacketPublisher) get_Module()).assignMessageIds(msg);
                }
            
            return msg;
            }
        
        // Accessor for the property "PacketAdapter"
        /**
         * Setter for property PacketAdapter.<p>
         */
        protected void setPacketAdapter(PacketPublisher.InQueue.PacketAdapter packetAdapter)
            {
            __m_PacketAdapter = packetAdapter;
            }
        
        // Accessor for the property "YieldOnFlush"
        /**
         * Setter for property YieldOnFlush.<p>
        * If true then the calling thread will yield upon an explicit flush.
        * 
        * An explicit flush is an indication that the calling thread is going
        * to block. Testing has shown that heavily multi-threaded applications
        * benefit significantly from performing this yield.  As we assume that
        * the caller is about to block, yielding to other threads is not
        * unreasonable.  Note that single threaded client/server tests show no
        * performance degradation either.
        * 
        * This feature may be controlled via the
        * tangosol.coherence.publisher.yieldonflush system property.
         */
        protected void setYieldOnFlush(boolean fYield)
            {
            __m_YieldOnFlush = fYield;
            }

        // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketPublisher$InQueue$PacketAdapter
        
        /**
         * The PacketAdapter provides a means for dequeueing packets from the
         * message queue.
         */
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class PacketAdapter
                extends    com.tangosol.coherence.component.util.Queue
            {
            // ---- Fields declarations ----
            
            /**
             * Property LastPacketIndex
             *
             * The index of the last packet in NextMessage.
             */
            private transient int __m_LastPacketIndex;
            
            /**
             * Property NextMessage
             *
             * The last message removed from the head of the queue.  Packets
             * are then dequeued from the message by the removeNextPacket
             * method.
             */
            private transient com.tangosol.coherence.component.net.Message __m_NextMessage;
            
            /**
             * Property NextPacketIndex
             *
             * The index of the next packet to dequeue from the NextMessage.
             */
            private transient int __m_NextPacketIndex;
            
            /**
             * Property PacketCounter
             *
             * This property is used to access the total number of packets
             * contained within all Messages on the InQueue.  The actual count
             * is maintained seperatly using an AtomicCounter. The
             * AtomicCounter is used instead of a volatile property, as we
             * don't want to risk over-counting, and causing interminable
             * traffic jam delays.
             */
            private transient java.util.concurrent.atomic.AtomicLong __m_PacketCounter;
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
                __mapChildren.put("Iterator", PacketPublisher.InQueue.PacketAdapter.Iterator.get_CLASS());
                }
            
            // Default constructor
            public PacketAdapter()
                {
                this(null, null, true);
                }
            
            // Initializing constructor
            public PacketAdapter(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                return new com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketPublisher.InQueue.PacketAdapter();
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
                    clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/packetProcessor/PacketPublisher$InQueue$PacketAdapter".replace('/', '.'));
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
            public boolean add(Object oElement)
                {
                throw new UnsupportedOperationException();
                }
            
            // Declared at the super level
            /**
             * Inserts  the specified element to the front of this queue.
            * 
            * @see #add
             */
            public boolean addHead(Object oElement)
                {
                throw new UnsupportedOperationException();
                }
            
            // Declared at the super level
            /**
             * Getter for property ElementList.<p>
            * The List that backs the Queue.
            * 
            * @volatile subclasses are allowed to change the value of
            * ElementList over time, and this property is accessed in
            * unsynchronized methods, thus it is volatile.
             */
            public com.tangosol.util.RecyclingLinkedList getElementList()
                {
                return super.getElementList();
                }
            
            // Accessor for the property "LastPacketIndex"
            /**
             * Getter for property LastPacketIndex.<p>
            * The index of the last packet in NextMessage.
             */
            public int getLastPacketIndex()
                {
                return __m_LastPacketIndex;
                }
            
            // Accessor for the property "NextMessage"
            /**
             * Getter for property NextMessage.<p>
            * The last message removed from the head of the queue.  Packets are
            * then dequeued from the message by the removeNextPacket method.
             */
            public com.tangosol.coherence.component.net.Message getNextMessage()
                {
                return __m_NextMessage;
                }
            
            // Accessor for the property "NextPacketIndex"
            /**
             * Getter for property NextPacketIndex.<p>
            * The index of the next packet to dequeue from the NextMessage.
             */
            public int getNextPacketIndex()
                {
                return __m_NextPacketIndex;
                }
            
            // Accessor for the property "PacketCounter"
            /**
             * Getter for property PacketCounter.<p>
            * This property is used to access the total number of packets
            * contained within all Messages on the InQueue.  The actual count
            * is maintained seperatly using an AtomicCounter. The AtomicCounter
            * is used instead of a volatile property, as we don't want to risk
            * over-counting, and causing interminable traffic jam delays.
             */
            public java.util.concurrent.atomic.AtomicLong getPacketCounter()
                {
                return __m_PacketCounter;
                }
            
            // Declared at the super level
            /**
             * Determine the number of elements in this Queue. The size of the
            * Queue may change after the size is returned from this method,
            * unless the Queue is synchronized on before calling size() and the
            * monitor is held until the operation based on this size result is
            * complete.
            * 
            * @return the number of elements in this Queue
             */
            public boolean isEmpty()
                {
                return size() == 0;
                }
            
            // Declared at the super level
            /**
             * Provides an iterator over the elements in this Queue. The
            * iterator is a point-in-time snapshot, and the contents of the
            * Queue may change after the iterator is returned, unless the Queue
            * is synchronized on before calling iterator() and until the
            * iterator is exhausted.
            * 
            * @return an iterator of the elements in this Queue
             */
            public java.util.Iterator iterator()
                {
                throw new UnsupportedOperationException();
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
                // import java.util.concurrent.atomic.AtomicLong;
                
                setPacketCounter(new AtomicLong());
                
                super.onInit();
                }
            
            // Declared at the super level
            /**
             * Returns the first element from the front of this Queue. If the
            * Queue is empty, no element is returned.
            * 
            * There is no blocking equivalent of this method as it would
            * require notification to wake up from an empty Queue, and this
            * would mean that the "add" and "addHead" methods would need to
            * perform notifyAll over notify which has performance implications.
            * 
            * @return the first element in the front of this Queue or null if
            * the Queue is empty
            * 
            * @see #remove
             */
            public Object peekNoWait()
                {
                throw new UnsupportedOperationException();
                }
            
            // Declared at the super level
            /**
             * Waits for and removes the first element from the front of this
            * Queue.
            * 
            * If the Queue is empty, this method will block until an element is
            * in the Queue or until the specified wait time has passed. The
            * non-blocking equivalent of this method is "removeNoWait".
            * 
            * @param cMillis  the number of ms to wait for an element; pass 0
            * to wait indefinitely
            * 
            * @return the first element in the front of this Queue or null if
            * the Queue is empty after the specified wait time has passed
            * 
            * @see #removeNoWait
             */
            public Object remove(long cMillis)
                {
                throw new UnsupportedOperationException();
                }
            
            // Declared at the super level
            /**
             * Removes and returns the first element from the front of this
            * Queue. If the Queue is empty, no element is returned.
            * 
            * The blocking equivalent of this method is "remove".
            * 
            * @return the first element in the front of this Queue or null if
            * the Queue is empty
            * 
            * @see #remove
             */
            public Object removeNoWait()
                {
                // import Component.Net.Message;
                // import Component.Util.Queue as com.tangosol.coherence.component.util.Queue;
                
                Message message = getNextMessage();
                boolean fNewMessage;
                int     iPacket;
                int     iPacketLast;
                
                if (message == null)
                    {
                    message = (Message) ((com.tangosol.coherence.component.util.Queue) get_Parent()).removeNoWait();
                    if (message == null)
                        {
                        return null;
                        }
                    fNewMessage = true;
                    iPacket     = 0;
                    iPacketLast = message.getMessagePartCount() - 1;
                    }
                else
                    {
                    fNewMessage = false;
                    iPacket     = getNextPacketIndex();
                    iPacketLast = getLastPacketIndex();
                    }
                
                if (iPacket < iPacketLast)
                    {
                    // there are more packets for this message
                    if (fNewMessage)
                        {
                        setNextMessage(message);
                        setLastPacketIndex(iPacketLast);
                        }
                    setNextPacketIndex(iPacket + 1);
                    }
                else if (!fNewMessage)
                    {
                    // end of message, don't hold onto it
                    setNextMessage(null);
                    }
                
                getPacketCounter().decrementAndGet();
                return message.getPacket(iPacket);
                }
            
            // Accessor for the property "LastPacketIndex"
            /**
             * Setter for property LastPacketIndex.<p>
            * The index of the last packet in NextMessage.
             */
            protected void setLastPacketIndex(int i)
                {
                __m_LastPacketIndex = i;
                }
            
            // Accessor for the property "NextMessage"
            /**
             * Setter for property NextMessage.<p>
            * The last message removed from the head of the queue.  Packets are
            * then dequeued from the message by the removeNextPacket method.
             */
            protected void setNextMessage(com.tangosol.coherence.component.net.Message message)
                {
                __m_NextMessage = message;
                }
            
            // Accessor for the property "NextPacketIndex"
            /**
             * Setter for property NextPacketIndex.<p>
            * The index of the next packet to dequeue from the NextMessage.
             */
            protected void setNextPacketIndex(int i)
                {
                __m_NextPacketIndex = i;
                }
            
            // Accessor for the property "PacketCounter"
            /**
             * Setter for property PacketCounter.<p>
            * This property is used to access the total number of packets
            * contained within all Messages on the InQueue.  The actual count
            * is maintained seperatly using an AtomicCounter. The AtomicCounter
            * is used instead of a volatile property, as we don't want to risk
            * over-counting, and causing interminable traffic jam delays.
             */
            protected void setPacketCounter(java.util.concurrent.atomic.AtomicLong counter)
                {
                __m_PacketCounter = counter;
                }
            
            // Declared at the super level
            /**
             * Determine the number of elements in this Queue. The size of the
            * Queue may change after the size is returned from this method,
            * unless the Queue is synchronized on before calling size() and the
            * monitor is held until the operation based on this size result is
            * complete.
            * 
            * @return the number of elements in this Queue
             */
            public int size()
                {
                return (int) getPacketCounter().get();
                }
            
            // Declared at the super level
            public String toString()
                {
                return get_Name() + "{size=" + size() +
                    ", NextIndex=" + getNextPacketIndex() +
                    ", LastIndex=" + getLastPacketIndex() + '}';
                }

            // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketPublisher$InQueue$PacketAdapter$Iterator
            
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
                    return new com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketPublisher.InQueue.PacketAdapter.Iterator();
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
                        clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/packetProcessor/PacketPublisher$InQueue$PacketAdapter$Iterator".replace('/', '.'));
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

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketPublisher$OutgoingMessageArray
    
    /**
     * A WindowedArray is an object that has attributes of a queue and a
     * dynamically resizing array.
     * 
     * The "window" is the active, or visible, portion of the virtual array.
     * Only elements within the window may be accessed or removed.
     * 
     * As elements are added, they are added to the "end" or "top" of the
     * array, dynamically resizing if necessary, and adjusting the window so
     * that it includes the new elements.
     * 
     * As items are removed, if they are removed from the "start" or "bottom"
     * of the array, the window adjusts such that those elements are no longer
     * visible.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class OutgoingMessageArray
            extends    com.tangosol.coherence.component.util.WindowedArray
        {
        // ---- Fields declarations ----
        
        // Default constructor
        public OutgoingMessageArray()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public OutgoingMessageArray(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketPublisher.OutgoingMessageArray();
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
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/packetProcessor/PacketPublisher$OutgoingMessageArray".replace('/', '.'));
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
         * The "component has been initialized" method-notification called out
        * of setConstructed() for the topmost component and that in turn
        * notifies all the children.
        * 
        * This notification gets called before the control returns to this
        * component instantiator (using <code>new Component.X()</code> or
        * <code>_newInstance(sName)</code>) and on the same thread. In
        * addition, visual components have a "posted" notification
        * <code>onInitUI</code> that is called after (or at the same time as)
        * the control returns to the instantiator and possibly on a different
        * thread.
         */
        public void onInit()
            {
            super.onInit();
            
            remove(add(null)); // eat msgId 0, must happen after onInit
            }
        
        // Declared at the super level
        /**
         * Setter for property FirstStuckIndex.<p>
        * The index of an element which appears to be stuck and causing the
        * window to grow uncontrollably.
        * 
        * @see #setWindowSize
         */
        public void setFirstStuckIndex(long lIndex)
            {
            super.setFirstStuckIndex(lIndex);
            }
        
        // Declared at the super level
        /**
         * Setter for property LastSizeWarningMillis.<p>
        * The time at which the last size warning was issued.
        * 
        * @see #setWindowSize
         */
        public void setLastSizeWarningMillis(long ldtMillis)
            {
            super.setLastSizeWarningMillis(ldtMillis);
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketPublisher$ResendQueue
    
    /**
     * An unsynchronized (non-notifying) Queue implementation.
     * 
     * The PacketPublisher's ResendQueue child is used to implement
     * guaranteed-delivery; Packets are placed in the Queue so that they are
     * automatically resent if they are not first acknowledged.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class ResendQueue
            extends    com.tangosol.coherence.component.util.queue.OptimisticQueue
        {
        // ---- Fields declarations ----
        
        /**
         * Property DiagnosticMemberSet
         *
         * The MemberSet holding members to which a diagnotic packet was sent
         * within the ResendMillis interval.
         * 
         * @see onPacketLost
         */
        private transient com.tangosol.coherence.component.net.MemberSet __m_DiagnosticMemberSet;
        
        /**
         * Property DiagnosticTimeToLive
         *
         * Specifies the number of round trips each diagnostic packet should
         * take.
         */
        private byte __m_DiagnosticTimeToLive;
        
        /**
         * Property DiagnosticWindowMillis
         *
         * Specifies a window of time before a node death in which diagnostics
         * will be performed.  Specifically, packets which have not been ack'ed
         * for over T-D time (where T is the packet timeout, D is this property
         * value) will trigger the transmission of an out-of-band diagnostic
         * packet.
         */
        private long __m_DiagnosticWindowMillis;
        
        /**
         * Property IMMEDIATE
         *
         */
        public static final long IMMEDIATE = -1L;
        
        /**
         * Property LastDiagnosticMillis
         *
         * Timestamp of last diagnostic packet send.
         */
        private transient long __m_LastDiagnosticMillis;
        
        /**
         * Property MultipointPacketCount
         *
         * The number of Multipoint packets in the resend queue.
         * 
         * @volatile, maintained by the publisher, accessed by client threads
         * 
         * @see #onPacketAdd
         * @see #onPacketConfirmation
         * @see #drainOverflow
         */
        private volatile transient int __m_MultipointPacketCount;
        
        /**
         * Property ResendMillis
         *
         * The minimum number of milliseconds that a Packet will remain queued
         * in the ResendQueue before being removed from the front of the Queue
         * to be resent.
         * 
         * (This property is designable.)
         */
        private int __m_ResendMillis;
        
        /**
         * Property TimeoutMillis
         *
         * The maximum number of milliseconds that a Packet will be resent
         * before it is assumed that the remaining unacknowledging Members have
         * died.
         * 
         * (This property is designable.)
         */
        private int __m_TimeoutMillis;
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
            __mapChildren.put("Iterator", PacketPublisher.ResendQueue.Iterator.get_CLASS());
            }
        
        // Default constructor
        public ResendQueue()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public ResendQueue(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
                setResendMillis(400);
                setTimeoutMillis(20000);
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketPublisher.ResendQueue();
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
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/packetProcessor/PacketPublisher$ResendQueue".replace('/', '.'));
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
         * Appends the specified element to the end of this queue.
        * 
        * Queues may place limitations on what elements may be added to this
        * Queue.  In particular, some Queues will impose restrictions on the
        * type of elements that may be added. Queue implementations should
        * clearly specify in their documentation any restrictions on what
        * elements may be added.
        * 
        * @param oElement element to be appended to this Queue
        * 
        * @return true (as per the general contract of the Collection.add
        * method)
        * 
        * @throws ClassCastException if the class of the specified element
        * prevents it from being added to this Queue
         */
        public boolean add(Object oElement)
            {
            // import Component.Net.Packet.MessagePacket;
            
            MessagePacket packet = (MessagePacket) oElement;
            
            // reschedule send
            if (packet.isNackInProgress())
                {
                return addHead(packet);
                }
            
            packet.setResendScheduled(packet.getSentMillis() + getResendMillis());
            
            onPacketAdd(packet);
            
            return super.add(packet);
            }
        
        /**
         * Inserts  the specified element to the front of this queue.
        * 
        * @param listPackets a series of packets to add to the head of the
        * resend queue
        * @param fNack true if the packets in listPackets are being rescheduled
        * due to a nack
        * 
        * @see #add
         */
        public boolean addAllHead(java.util.List listPackets, boolean fNack)
            {
            // import Component.Net.Packet.MessagePacket;
            // import com.tangosol.util.Base;
            // import java.util.Iterator as java.util.Iterator;
            // import java.util.List;
            
            long ldtScheduled = Base.getSafeTimeMillis();
            
            // find insertion point based on scheduled resend time
            List list    = getElementList();
            int  iInsert = 0;
            for (int cPackets = list.size(); iInsert < cPackets &&
                    ((MessagePacket) list.get(iInsert)).getResendScheduled() <= ldtScheduled;
                    ++iInsert)
                {
                }
            
            // schedule all the packets for immediate departure;
            // we use "now" rather then IMMEDIATE to prevent them from cutting in line
            int iStart = 0;
            int iCurr  = 0;
            for (java.util.Iterator iter = listPackets.iterator(); iter.hasNext(); )
                {
                MessagePacket packet = (MessagePacket) iter.next();
                packet.setResendScheduled(ldtScheduled);
            
                if (fNack)
                    {
                    if (packet.isNackInProgress())
                        {
                        // Avoid double addition if nack is already in progress.
                        // This is needed to ensure that the PendingSkip count is properly
                        // maintained, we can't allow any double sets on NackInProgress
                        // Bulk insert the last segments worth of packets
                        if (iCurr != iStart)
                            {
                            list.addAll(iInsert, listPackets.subList(iStart, iCurr));
                            }
                        iStart = ++iCurr;
                        continue;
                        }
                    else
                        {
                        packet.setNackInProgress(true);
                        }
                    }
                
                onPacketAdd(packet);
                ++iCurr;
                }
            
            if (iStart == 0)
                {
                // there was no segmenting, bulk insert all packets
                list.addAll(iInsert, listPackets);
                }
            else
                {
                // bulk insert the last segment
                list.addAll(iInsert, listPackets.subList(iStart, iCurr));
                }
                
            return true;
            }
        
        // Declared at the super level
        /**
         * Inserts  the specified element to the front of this queue.
        * 
        * @see #add
         */
        public boolean addHead(Object oElement)
            {
            // import Component.Net.Packet.MessagePacket;
            // import java.util.List;
            
            MessagePacket packet = (MessagePacket) oElement;
            
            // schedule for immediate departure
            packet.setResendScheduled(IMMEDIATE);
            
            onPacketAdd(packet);
            
            // add at the end of all the other immediate packets
            // (i.e. before any regularly scheduled packets)
            List listPackets = getElementList();
            int  iInsert     = 0;
            for (int cPackets = listPackets.size(); iInsert < cPackets &&
                    ((MessagePacket) listPackets.get(iInsert)).getResendScheduled() <= IMMEDIATE;
                    ++iInsert)
                {
                }
            
            listPackets.add(iInsert, packet);
            
            // it is not permitted to wait on this queue; see remove()
            // notify();
            
            return true;
            }
        
        // Accessor for the property "DiagnosticMemberSet"
        /**
         * Getter for property DiagnosticMemberSet.<p>
        * The MemberSet holding members to which a diagnotic packet was sent
        * within the ResendMillis interval.
        * 
        * @see onPacketLost
         */
        public com.tangosol.coherence.component.net.MemberSet getDiagnosticMemberSet()
            {
            return __m_DiagnosticMemberSet;
            }
        
        // Accessor for the property "DiagnosticTimeToLive"
        /**
         * Getter for property DiagnosticTimeToLive.<p>
        * Specifies the number of round trips each diagnostic packet should
        * take.
         */
        public byte getDiagnosticTimeToLive()
            {
            return __m_DiagnosticTimeToLive;
            }
        
        // Accessor for the property "DiagnosticWindowMillis"
        /**
         * Getter for property DiagnosticWindowMillis.<p>
        * Specifies a window of time before a node death in which diagnostics
        * will be performed.  Specifically, packets which have not been ack'ed
        * for over T-D time (where T is the packet timeout, D is this property
        * value) will trigger the transmission of an out-of-band diagnostic
        * packet.
         */
        public long getDiagnosticWindowMillis()
            {
            return __m_DiagnosticWindowMillis;
            }
        
        // Accessor for the property "LastDiagnosticMillis"
        /**
         * Getter for property LastDiagnosticMillis.<p>
        * Timestamp of last diagnostic packet send.
         */
        public long getLastDiagnosticMillis()
            {
            return __m_LastDiagnosticMillis;
            }
        
        // Accessor for the property "MessageOutgoing"
        /**
         * Getter for property MessageOutgoing.<p>
        * The "windowed array" of outgoing Messages; cleaned up by the resend
        * Queue if a Packet is removed from the front  of the Queue and it has
        * been fully acknowledged.
         */
        public com.tangosol.coherence.component.util.WindowedArray getMessageOutgoing()
            {
            return ((PacketPublisher) get_Module()).getMessageOutgoing();
            }
        
        // Accessor for the property "MultipointPacketCount"
        /**
         * Getter for property MultipointPacketCount.<p>
        * The number of Multipoint packets in the resend queue.
        * 
        * @volatile, maintained by the publisher, accessed by client threads
        * 
        * @see #onPacketAdd
        * @see #onPacketConfirmation
        * @see #drainOverflow
         */
        public int getMultipointPacketCount()
            {
            return __m_MultipointPacketCount;
            }
        
        // Accessor for the property "ResendMillis"
        /**
         * Getter for property ResendMillis.<p>
        * The minimum number of milliseconds that a Packet will remain queued
        * in the ResendQueue before being removed from the front of the Queue
        * to be resent.
        * 
        * (This property is designable.)
         */
        public int getResendMillis()
            {
            return __m_ResendMillis;
            }
        
        // Accessor for the property "TimeoutMillis"
        /**
         * Getter for property TimeoutMillis.<p>
        * The maximum number of milliseconds that a Packet will be resent
        * before it is assumed that the remaining unacknowledging Members have
        * died.
        * 
        * (This property is designable.)
         */
        public int getTimeoutMillis()
            {
            return __m_TimeoutMillis;
            }
        
        // Accessor for the property "WaitMillis"
        /**
         * Getter for property WaitMillis.<p>
        * (calculated) The number of milliseconds that the current thread
        * should rest (Object.wait) for the next Packet to be ready to come off
        * the front of the resend Queue.
         */
        public long getWaitMillis()
            {
            // import Component.Net.Packet.MessagePacket;
            // import com.tangosol.util.Base;
            
            // - if the next packet is ready, don't wait
            // - avoid returning 0 unless the queue is empty
            // - we must remove any resendSkip packets sitting at the head
            //   of the resend queue, as their scheduled time cannot be used here
            
            MessagePacket packet = (MessagePacket) peekNoWait();
            if (packet == null)
                {
                return 0L;
                }
            
            long ldtScheduled = packet.getResendScheduled();
            if (ldtScheduled < 0L)
                {
                return -1L;
                }
            
            long cMillisWait = ldtScheduled - Base.getSafeTimeMillis();
            return cMillisWait <= 0L ? -1L : cMillisWait;
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
            // import com.tangosol.coherence.config.Config;
            // import Component.Net.MemberSet;
            
            setDiagnosticTimeToLive(Byte.parseByte(Config.getProperty(
                "coherence.tcmp.diag.ttl", "2")));
            setDiagnosticWindowMillis(Long.parseLong(Config.getProperty(
                "coherence.tcmp.diag.window", "2000")));
            setDiagnosticMemberSet(new MemberSet());
            
            super.onInit();
            }
        
        /**
         * Called as a packet is being added to the resend queue.
         */
        protected void onPacketAdd(com.tangosol.coherence.component.net.packet.MessagePacket packet)
            {
            // schedule timeout
            if (packet.getResendTimeout() == 0L)
                {
                packet.setResendTimeout(packet.getSentMillis() + getTimeoutMillis());
            
                // count multipoint the first time it is added to the queue
                if (packet.isOutgoingMultipoint())
                    {
                    setMultipointPacketCount(getMultipointPacketCount() + 1);
                    }
                }
            }
        
        /**
         * Called when a sent packet has been confirmed by all living
        * receipients.
        * Packet does not need to be resent any longer; remove it from the
        * associated outgoing Message and potentially remove the Message
        * altogether.
         */
        protected void onPacketDone(com.tangosol.coherence.component.net.packet.MessagePacket packet)
            {
            // import Component.Net.Member;
            // import Component.Net.Message;
            // import Component.Net.Packet.MessagePacket;
            // import Component.Util.WindowedArray;
            
            // count mulitpoint when the are finally removed
            if (packet.isOutgoingMultipoint())
                {
                setMultipointPacketCount(getMultipointPacketCount() - 1);
                }
            
            // remove the packet from the associated outgoing message
            WindowedArray waMsg  = getMessageOutgoing();
            long          lMsgId = packet.getFromMessageId();
            Message       msg    = (Message) waMsg.get(lMsgId);
            if (msg == null)
                {
                // we've lost track of packets/messages, this is not allowed
                _trace("Encountered orphan packet:\n" + packet
                        + "\nOutgoing Message Array:\n" + waMsg, 1);
                
                throw new IllegalStateException("Encountered orphan packet");
                }
            
            msg.setPacket(packet.getMessagePartIndex(), null);
            
            if (msg.getNullPacketCount() == msg.getMessagePartCount())
                {
                // allow release of all buffers currently held by this outgoing message
                msg.releaseOutgoing(/*fSuspect*/ true, /*fOrdered*/ false);
            
                // message has been delivered
                if (lMsgId == waMsg.getFirstIndex())
                    {
                    // message was first. Process all sequential fully confirmed
                    // messages.
                    long lLastId = waMsg.getLastIndex();
            
                    do
                        {
                        waMsg.remove(lMsgId);
            
                        // allow delivery of receipt if needed
                        msg.releaseOutgoingComplete();
            
                        // move to next non-null message
                        for (msg = null; msg == null && lMsgId < lLastId; )
                            {
                            msg = (Message) waMsg.get(++lMsgId);
                            }
                        }
                    while (msg != null && msg.getNullPacketCount() == msg.getMessagePartCount());
                    }
                else if (!msg.isNotifyDelivery())
                    {
                    // not first, but no receipt required, we can remove it now
                    waMsg.remove(lMsgId);
                    msg.releaseOutgoingComplete();
                    }
                }
            }
        
        /**
         * Called by the publisher when a sent packet has not been ack'd and
        * needs to be resent.
         */
        protected void onPacketLost(com.tangosol.coherence.component.net.Member member, com.tangosol.coherence.component.net.packet.MessagePacket packet)
            {
            // import Component.Net.Member$FlowControl as com.tangosol.coherence.component.net.Member.FlowControl;
            // import Component.Net.Packet.DiagnosticPacket;
            // import Component.Net.Packet.MessagePacket;
            // import com.tangosol.util.Base;
            
            PacketPublisher     publisher   = (PacketPublisher) get_Module();
            boolean     fTimedout   = packet.getPendingResendSkips() == 0;
            com.tangosol.coherence.component.net.Member.FlowControl flowControl = member.getFlowControl();
            long        ldtNow      = Base.getLastSafeTimeMillis();
            if (flowControl != null && packet.isDeferrable())
                {
                int cSeqLost = flowControl.getSequentialLostCount();
                flowControl.setSequentialLostCount(++cSeqLost);
            
                // remote pause detection isn't triggered by nack packets as they
                // indicate that the other side is alive
                if (fTimedout)
                    {            
                    int iLostPacketThreshold = flowControl.getLostPacketThreshold();
                    if (iLostPacketThreshold > 0 &&
                        cSeqLost > iLostPacketThreshold &&
                        !flowControl.isPaused() &&
                        ldtNow - packet.getResendScheduled() <
                            publisher.getClockResolutionMillis())
                        {
                        // We only declare the other member as paused if we are
                        // not running behind schedule.  If we are behind schedule
                        // then the loss could be our fault.
                        flowControl.setPaused(true);
                        }
                    }
            
                packet.setDeliveryState(MessagePacket.DELIVERY_LOST, member);
                publisher.drainDeferredPackets(member);
                }
            
            if (publisher.isNackEnabled() && fTimedout)
                {
                // Natural timeout, remove the packet from the recently sent queue
                // this may be a costly operation, but with nacks it should be rare.
                // If it were not removed here, then the next nack would re-identify it as lost,
                // and it would be re-sent too soon.
                member.getRecentPacketQueue().remove(packet);
                }
            
            long ldtTimeout = packet.getResendTimeout();
            if (!member.isDeaf() &&
                ldtTimeout - ldtNow < getDiagnosticWindowMillis() &&
                ldtTimeout > ldtNow) // if we're still sending post timeout then quorum vetoed death, don't continue diagnostics
                {
                if (ldtNow >= getLastDiagnosticMillis() + getResendMillis())
                    {
                    getDiagnosticMemberSet().clear();
                    setLastDiagnosticMillis(ldtNow);
                    }
            
                if (getDiagnosticMemberSet().add(member))
                    {
                    DiagnosticPacket packetDiag;
                    int              nToId = member.getId();
                    byte             nTTL  = getDiagnosticTimeToLive();
            
                    // send a packet to both the preferred and advertished ports; this
                    // provides a means of identifying issues with a single socket
                    if (member.getPreferredPort() != 0)
                        {
                        packetDiag = new DiagnosticPacket();
                        packetDiag.setPreferredPortUsed(true);
                        packetDiag.setToId(nToId);
                        packetDiag.setTimeToLive(nTTL);
                        publisher.sendDiagnosticPacket(packetDiag);
                        }
            
                    packetDiag = new DiagnosticPacket();
                    packetDiag.setToId(nToId);
                    packetDiag.setTimeToLive(nTTL);
                    publisher.sendDiagnosticPacket(packetDiag);
                    }
                }
            }
        
        /**
         * Called (on the publisher thread) when a sent packet has not been
        * acked and needs to be resent.
         */
        protected void onPacketLost(com.tangosol.coherence.component.net.packet.MessagePacket packet)
            {
            // import Component.Net.Member;
            // import Component.Net.Packet.MessagePacket;
            
            PacketPublisher publisher = (PacketPublisher) get_Module();
            
            // determine who hasn't ack'd the packet
            if (packet.isOutgoingMultipoint())
                {
                int[] anToId = ((MessagePacket) packet).getToMemberSet().toIdArray();
            
                for (int i = 0, c = anToId.length; i < c; ++i)
                    {
                    Member member = publisher.getMember(anToId[i]);
                    if (member != null)
                        {
                        onPacketLost(member, packet);
                        }
                    }
                }
            else
                {
                Member member = publisher.getMember(packet.getToId());
                if (member != null)
                    {
                    onPacketLost(member, packet);
                    }
                }
            
            if (packet.isNackInProgress())
                {
                // early resend (i.e. nack)
                packet.setNackInProgress(false);
                }
            }
        
        // Declared at the super level
        /**
         * Returns the first element from the front of this Queue. If the Queue
        * is empty, no element is returned.  For the resendQueue this will also
        * filter out any resendSkip packets as they are not in cronological
        * order.
        * 
        * There is no blocking equivalent of this method as it would require
        * notification to wake up from an empty Queue, and this would mean that
        * the "add" and "addHead" methods would need to perform notifyAll over
        * notify which has performance implications.
        * 
        * For the ResendQueue this will also discard any packets which reach
        * the head and are marked for skipping.  Thus the return value is the
        * next packet which will require a resend if it is not ack'd before
        * it's resend time.
        * 
        * @return the first element in the front of this Queue or null if the
        * Queue is empty
        * 
        * @see #remove
         */
        public Object peekNoWait()
            {
            // import Component.Net.Packet.MessagePacket;
            // import java.util.List;
            
            // resend skip packets are filtered out here
            // their timestamp is not in the proper cronological
            // order with respect to the rest of the resend queue
            
            List list = getElementList();
            while (!list.isEmpty())
                {
                MessagePacket packet = (MessagePacket) list.get(0);
            
                if (packet == null)
                    {
                    return null;
                    }
            
                if (packet.isNackInProgress())
                    {
                    // adjust nack setting and account for extra packet which had been added
                    packet.setNackInProgress(false);
                    packet.setPendingResendSkips(packet.getPendingResendSkips() + 1);            
                    }
                else if (packet.getPendingResendSkips() > 0)
                    {
                    // eat the skip packet and adjust the count
                    if (packet == list.remove(0))
                        {
                        packet.setPendingResendSkips(packet.getPendingResendSkips() - 1);
                        continue;
                        }
                    throw new IllegalStateException();
                    }
                return packet;
                }
            return null;
            }
        
        // Declared at the super level
        /**
         * Waits for and removes the first element from the front of this Queue.
        * 
        * If the Queue is empty, this method will block until an element is in
        * the Queue. The non-blocking equivalent of this method is
        * "removeNoWait".
        * 
        * @return the first element in the front of this Queue
        * 
        * @see #removeNoWait
         */
        public Object remove()
            {
            // retry queue should be used only by the PacketPublisher thread
            throw new UnsupportedOperationException();
            }
        
        // Declared at the super level
        /**
         * Removes and returns the first element from the front of this Queue.
        * If the Queue is empty, no element is returned.
        * 
        * The blocking equivalent of this method is "remove".
        * 
        * @return the first element in the front of this Queue or null if the
        * Queue is empty
        * 
        * @see #remove
         */
        public Object removeNoWait()
            {
            // import Component.Net.Member;
            // import Component.Net.MemberSet;
            // import Component.Net.Message;
            // import Component.Net.Packet.MessagePacket;
            // import com.tangosol.util.Base;
            // import java.util.Iterator as java.util.Iterator;
            
            for (MessagePacket packet = (MessagePacket) peekNoWait();
                    packet != null;
                    packet = (MessagePacket) peekNoWait())
                {
                if (packet.isResendNecessary())
                    {
                    long ldtNow       = Base.getSafeTimeMillis();
                    long ldtScheduled = packet.getResendScheduled();
                    if (ldtScheduled > ldtNow)
                        {
                        // scheduled resend time not reached
                        break;
                        }
            
                    // scheduled time has been reached and packet has not been fully ack'd
                    if (packet != super.removeNoWait())
                        {
                        throw new IllegalStateException();
                        }
            
                    // check if the member should be declared as dead
                    long ldtResendTimeout    = packet.getResendTimeout();
                    long cMillisTimeout      = getTimeoutMillis();
                    long ldtHeuristicTimeout = ldtResendTimeout - (cMillisTimeout >> 1);
                    long ldtSlowTimeout      = ldtResendTimeout - (cMillisTimeout >> 2);
            
                    if (ldtScheduled > ldtHeuristicTimeout)
                        {
                        PacketPublisher publisher = (PacketPublisher) get_Module();
            
                        // before declaring anyone as slow/dead check for missing Acks
                        if (!publisher.verifyResendNecessary(packet))
                            {
                            // missing ack detection identified this packet as confirmed
                            // or member(s) already dead
                            onPacketDone(packet);
                            continue;
                            }
            
                        // this packet is at least 1/2 on its way to be declared undeliverable
                        int nToId = packet.getToId();
                        if (nToId != 0)
                            {
                            Member member = publisher.getMember(nToId);
                            if (member != null)
                                {
                                member.setLastHeuristicDeathMillis(ldtNow);
                                }
                            }
                        else
                            {
                            // check the multi "addressed to" packet property
                            MemberSet setToMember = packet.getToMemberSet();
                            if (setToMember != null)
                                {
                                for (java.util.Iterator iter = setToMember.iterator(); iter.hasNext();)
                                    {
                                    Member member = (Member) iter.next();
                                    member.setLastHeuristicDeathMillis(ldtNow);
                                    }
                                }
                            }
            
                        if (ldtScheduled > ldtResendTimeout)
                            {
                            // the remaining Members may be dead
                            publisher.onUndeliverablePacket(packet);
                            // resend state may have changed, due to members
                            // being declared as dead.  Note: Member death may be
                            // delayed due to witness protocol, quorum, and/or
                            // other reasons.
                            if (!packet.isResendNecessary())
                                {
                                // the members which didn't ack the packet are now dead
                                onPacketDone(packet);
                                continue;
                                }
                            }
                        else if (ldtScheduled > ldtSlowTimeout)
                            {
                            // the remaining Members are slow to ACK
                            publisher.onSlowPacket(packet);
                            }
                        }
            
                    onPacketLost(packet);
                    return packet;
                    }
                else
                    {
                    // packet is fully ack'd
                    if (packet == super.removeNoWait())
                        {
                        onPacketDone(packet);
                        }
                    else
                        {
                        throw new IllegalStateException();
                        }
                    }    
                }
            
            return null;
            }
        
        // Accessor for the property "DiagnosticMemberSet"
        /**
         * Setter for property DiagnosticMemberSet.<p>
        * The MemberSet holding members to which a diagnotic packet was sent
        * within the ResendMillis interval.
        * 
        * @see onPacketLost
         */
        public void setDiagnosticMemberSet(com.tangosol.coherence.component.net.MemberSet setMember)
            {
            __m_DiagnosticMemberSet = setMember;
            }
        
        // Accessor for the property "DiagnosticTimeToLive"
        /**
         * Setter for property DiagnosticTimeToLive.<p>
        * Specifies the number of round trips each diagnostic packet should
        * take.
         */
        protected void setDiagnosticTimeToLive(byte cTrips)
            {
            __m_DiagnosticTimeToLive = cTrips;
            }
        
        // Accessor for the property "DiagnosticWindowMillis"
        /**
         * Setter for property DiagnosticWindowMillis.<p>
        * Specifies a window of time before a node death in which diagnostics
        * will be performed.  Specifically, packets which have not been ack'ed
        * for over T-D time (where T is the packet timeout, D is this property
        * value) will trigger the transmission of an out-of-band diagnostic
        * packet.
         */
        protected void setDiagnosticWindowMillis(long cMillis)
            {
            __m_DiagnosticWindowMillis = cMillis;
            }
        
        // Accessor for the property "LastDiagnosticMillis"
        /**
         * Setter for property LastDiagnosticMillis.<p>
        * Timestamp of last diagnostic packet send.
         */
        public void setLastDiagnosticMillis(long lMillis)
            {
            __m_LastDiagnosticMillis = lMillis;
            }
        
        // Accessor for the property "MultipointPacketCount"
        /**
         * Setter for property MultipointPacketCount.<p>
        * The number of Multipoint packets in the resend queue.
        * 
        * @volatile, maintained by the publisher, accessed by client threads
        * 
        * @see #onPacketAdd
        * @see #onPacketConfirmation
        * @see #drainOverflow
         */
        protected void setMultipointPacketCount(int cPackets)
            {
            __m_MultipointPacketCount = cPackets;
            }
        
        // Accessor for the property "ResendMillis"
        /**
         * Setter for property ResendMillis.<p>
        * The minimum number of milliseconds that a Packet will remain queued
        * in the ResendQueue before being removed from the front of the Queue
        * to be resent.
        * 
        * (This property is designable.)
         */
        public void setResendMillis(int cMillis)
            {
            __m_ResendMillis = (Math.max(1, cMillis));
            }
        
        // Accessor for the property "TimeoutMillis"
        /**
         * Setter for property TimeoutMillis.<p>
        * The maximum number of milliseconds that a Packet will be resent
        * before it is assumed that the remaining unacknowledging Members have
        * died.
        * 
        * (This property is designable.)
         */
        public void setTimeoutMillis(int cMillis)
            {
            __m_TimeoutMillis = (Math.max(10, cMillis));
            }

        // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketPublisher$ResendQueue$Iterator
        
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
                return new com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketPublisher.ResendQueue.Iterator();
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
                    clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/packetProcessor/PacketPublisher$ResendQueue$Iterator".replace('/', '.'));
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
