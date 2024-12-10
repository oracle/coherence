
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketReceiver

package com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor;

import com.tangosol.coherence.component.net.Member;
import com.tangosol.coherence.component.net.Message;
import com.tangosol.coherence.component.net.Packet;
import com.tangosol.coherence.component.net.packet.DiagnosticPacket;
import com.tangosol.coherence.component.net.packet.MessagePacket;
import com.tangosol.coherence.component.net.packet.messagePacket.Broadcast;
import com.tangosol.coherence.component.net.packet.messagePacket.Directed;
import com.tangosol.coherence.component.net.packet.messagePacket.Sequel;
import com.tangosol.coherence.component.net.packet.notifyPacket.Ack;
import com.tangosol.coherence.component.net.packet.notifyPacket.Request;
import com.tangosol.coherence.component.util.WindowedArray;
import com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketPublisher;
import com.oracle.coherence.common.io.BufferManager;
import com.tangosol.coherence.config.Config;
import com.tangosol.io.nio.ByteBufferReadBuffer;
import com.tangosol.net.internal.PacketComparator;
import com.tangosol.net.internal.PacketIdentifier;
import com.tangosol.util.Base;
import com.tangosol.util.ClassHelper;
import com.tangosol.util.LongArray;
import com.tangosol.util.SimpleLongArray;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.Iterator;

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
 * --
 * 
 * A client of PacketReceiver must additionally configure:
 * - SendQueue property
 * - MessageOutgoing property
 * 
 * Once the Member mini-id is assigned to this Member, the onJoined event must
 * be triggered.
 * 
 * The following properties need to be updated as other Cluster-enabled
 * Services are added to this Member:
 * - Service property
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public abstract class PacketReceiver
        extends    com.tangosol.coherence.component.util.daemon.queueProcessor.PacketProcessor
    {
    // ---- Fields declarations ----
    
    /**
     * Property AckSendQueue
     *
     * The Queue on which Ack Packets are enqueued for sending.
     */
    private transient com.tangosol.coherence.component.util.Queue __m_AckSendQueue;
    
    /**
     * Property BufferManager
     *
     * The BufferManager all ByteBuffers used for incoming packets are acquired
     * from.
     */
    private com.oracle.coherence.common.io.BufferManager __m_BufferManager;
    
    /**
     * Property ConfirmationQueue
     *
     * The Queue on which incoming Ack Packets are enqueued for processing.
     */
    private transient com.tangosol.coherence.component.util.Queue __m_ConfirmationQueue;
    
    /**
     * Property FlushPendingService
     *
     * Service which must be flushed prior to the receiver entering a blocking
     * state.
     */
    private transient com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid __m_FlushPendingService;
    
    /**
     * Property GarbagePacketCount
     *
     * The number of non-Coherence packets which have arrived since the last
     * warning was issued.
     */
    private transient long __m_GarbagePacketCount;
    
    /**
     * Property LastGarbageWarningMillis
     *
     * Timestamp of the last time a warning was issued due to reception of a
     * non-Coherence packet.
     */
    private transient long __m_LastGarbageWarningMillis;
    
    /**
     * Property MaximumPacketLength
     *
     * The absolute maximum number of bytes that can be placed into an outgoing
     * Packet.
     * 
     * @see ClusterConfig
     */
    private int __m_MaximumPacketLength;
    
    /**
     * Property NackEnabled
     *
     * Specifies if the receiver should use request packets, i.e. NACKs to do
     * early packet loss detection.
     */
    private transient boolean __m_NackEnabled;
    
    /**
     * Property PreferredPacketLength
     *
     * The preferred number of bytes that can be placed into an outgoing
     * Packet.  Used here to figure out how many ACK could be coalesced
     * together, as well as the size of Packets in the UdpPacketPool.
     * 
     * @see ClusterConfig
     */
    private int __m_PreferredPacketLength;
    
    /**
     * Property Publisher
     *
     * The Cluster's Publisher daemon.
     */
    private transient PacketPublisher __m_Publisher;
    
    /**
     * Property StatsReceived
     *
     * Statistics: total number of received Packets.
     */
    private transient long __m_StatsReceived;
    
    /**
     * Property StatsRepeated
     *
     * Statistics: number of repeated packets (received more then once)
     */
    private transient long __m_StatsRepeated;
    
    /**
     * Property StatsReset
     *
     * Statistics: Date/time value that the stats have been reset.
     */
    private transient long __m_StatsReset;
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
        __mapChildren.put("AddressedBuffer", PacketReceiver.AddressedBuffer.get_CLASS());
        }
    
    // Initializing constructor
    public PacketReceiver(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/packetProcessor/PacketReceiver".replace('/', '.'));
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
    
    protected void checkReadyMessages(com.tangosol.coherence.component.net.Member member)
        {
        // import Component.Net.Message;
        // import Component.Util.WindowedArray;
        // import com.tangosol.util.LongArray;
        
        // check for completed Messages from the Member
        WindowedArray waMsg   = member.getMessageIncoming();
        long          lMsgId  = waMsg.getFirstIndex();
        Message       msg     = (Message) waMsg.get(lMsgId);
        
        if (msg == null)
            {
            return;
            }
        
        LongArray laPile = member.getMessagePile();
        while (msg.getNullPacketCount() == 0)
            {
            // remove the Message from the Message window array and the "pile" and
            // update the global Message counter kept for the "from" Member so that
            // repeated Sequel Packets for already-processed Messages will be discarded
            waMsg.remove(lMsgId);
        
            long lFromMsgId = msg.getFromMessageId();
            if (msg.getMessagePartCount() > 1)
                {
                laPile.remove(lFromMsgId);
                }
            member.setLastIncomingMessageId(lFromMsgId);
            member.setContiguousFromPacketId(msg.getPacket(
                msg.getMessagePartCount() - 1));
            
            // process Message
            onMessage(msg);
        
            // proceed to next Message in the window
            msg = (Message) waMsg.get(++lMsgId);
            if (msg == null)
                {
                return; // no more messages
                }
            }
        
        // The msg now points to the first partially full message; we know that all
        // packets from the sender up to the gap have been received. The ID of the last
        // packet before the gap will be used in ACK/NACK feedback.
        for (int i = 1, c = msg.getMessagePartCount(); i < c; ++i)
            {
            if (msg.getPacket(i) == null)
                {
                member.setContiguousFromPacketId(msg.getPacket(i - 1));
                break;
                }
            }
        }
    
    /**
     * Cleanup pending messages associated with a member which has been removed
    * from the cluster.
     */
    protected void cleanup(com.tangosol.coherence.component.net.Member member)
        {
        // import Component.Net.Message;
        // import Component.Net.Packet.MessagePacket;
        // import Component.Util.WindowedArray;
        // import com.oracle.coherence.common.io.BufferManager;
        // import com.tangosol.util.LongArray;
        // import java.util.Iterator;
        
        BufferManager mgr = getBufferManager();
        
        for (Iterator iter = member.getMessagePile().iterator(); iter.hasNext();)
            {
            Object o = iter.next();
            if (o instanceof LongArray) // all we have are sequel packets
                {
                for (Iterator iterPacket = ((LongArray) o).iterator(); iterPacket.hasNext(); )
                    {
                    mgr.release(((MessagePacket) iterPacket.next()).getByteBuffer());
                    }
                }
            // else o instanceof Message; handled below
            }
        member.setMessagePile(null);
        
        WindowedArray la = member.getMessageIncoming();
        for (long li = la.getFirstIndex(), le = la.getLastIndex(); li <= le; li = la.getFirstIndex())
            {
            Message msg = (Message) la.remove(li);
            if (msg == null)
                {
                break; // signifies that the remainder or the indexes must be null
                }
        
            for (int i = 0, c = msg.getMessagePartCount(); i < c; ++i)
                {
                MessagePacket packet = msg.getPacket(i);
                if (packet != null)
                    {
                    mgr.release(packet.getByteBuffer());
                    }
                }
            }
        member.setMessageIncoming(null);
        }
    
    /**
     * Inform sender that we've received the packet.
     */
    protected void confirm(com.tangosol.coherence.component.net.Member member, com.tangosol.coherence.component.net.packet.MessagePacket packet)
        {
        // import Component.Net.Member;
        // import Component.Net.Packet.NotifyPacket.Ack;
        // import com.tangosol.net.internal.PacketComparator;
        // import com.tangosol.net.internal.PacketIdentifier;
        // import com.tangosol.util.Base;
        
        // record the packet in an ack
        Ack packetAck = member.getPacketAck();
        if (packetAck == null || !packetAck.addPacket(packet))
            {
            packetAck = new Ack();
            packetAck.setFromId(getMemberId());
            packetAck.setToId(packet.getFromId());
            packetAck.addPacket(packet);
            getAckSendQueue().add(packetAck);
            }
        
        // record if this packet is the newest
        if (isNackEnabled() && PacketComparator.compare(packet, member.getNewestFromPacketId()) > 0)
            {
            // Note: we must change this state between adding the packet to the Ack and flushing it.
            // Otherwise it is possible that the publisher might concurrently flush the Ack taking
            // the newestFormId but not include this packet in the Ack, which would make the sender
            // think the packet had been dropped
            member.setNewestFromPacketId(packet);
            }
        
        // flush the ack if it has reached the desired size
        int cbPref    = Math.min(getPreferredPacketLength(), member.getPreferredPacketLength());
        int cMaxSlots = (cbPref - Ack.LENGTH_FIXED) / Ack.LENGTH_VARIABLE;    
        if (packetAck.getNotifyCount() >= Math.min(cMaxSlots, member.getPreferredAckSize()))
            {
            // We just filled the packet, flush it based on the state at the time the
            // packet was filled, and schedule it for immediate departure
            packetAck.flush(member);
        
            long ldtNow = Base.getSafeTimeMillis();
            if (packetAck.getScheduledMillis() > ldtNow)
                {
                packetAck.setScheduledMillis(ldtNow);
                getAckSendQueue().addHead(packetAck);
                }
            }
        }
    
    /**
     * Flush all the pending service queues.
     */
    protected void flushSend()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid;
        
        com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid service = getFlushPendingService();
        if (service != null)
            {
            service.getQueue().flush();
            setFlushPendingService(null);
            }
        }
    
    public String formatStats()
        {
        long   lReceived = getStatsReceived();
        long   lRepeated = getStatsRepeated();
        double dSuccess  = lReceived == 0L ? 1.0 : 1.0 - ((double) lRepeated)/((double) lReceived);
        
        dSuccess = ((int) (dSuccess * 10000)) / 10000D;
        
        return "PacketsReceived=" + lReceived
           + ", PacketsRepeated=" + lRepeated
           + ", SuccessRate="     + dSuccess
           ;
        }
    
    // Accessor for the property "AckSendQueue"
    /**
     * Getter for property AckSendQueue.<p>
    * The Queue on which Ack Packets are enqueued for sending.
     */
    public com.tangosol.coherence.component.util.Queue getAckSendQueue()
        {
        return __m_AckSendQueue;
        }
    
    // Accessor for the property "BufferManager"
    /**
     * Getter for property BufferManager.<p>
    * The BufferManager all ByteBuffers used for incoming packets are acquired
    * from.
     */
    public com.oracle.coherence.common.io.BufferManager getBufferManager()
        {
        return __m_BufferManager;
        }
    
    // Accessor for the property "ConfirmationQueue"
    /**
     * Getter for property ConfirmationQueue.<p>
    * The Queue on which incoming Ack Packets are enqueued for processing.
     */
    public com.tangosol.coherence.component.util.Queue getConfirmationQueue()
        {
        return __m_ConfirmationQueue;
        }
    
    // Accessor for the property "FlushPendingService"
    /**
     * Getter for property FlushPendingService.<p>
    * Service which must be flushed prior to the receiver entering a blocking
    * state.
     */
    public com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid getFlushPendingService()
        {
        return __m_FlushPendingService;
        }
    
    // Accessor for the property "GarbagePacketCount"
    /**
     * Getter for property GarbagePacketCount.<p>
    * The number of non-Coherence packets which have arrived since the last
    * warning was issued.
     */
    public long getGarbagePacketCount()
        {
        return __m_GarbagePacketCount;
        }
    
    // Accessor for the property "LastGarbageWarningMillis"
    /**
     * Getter for property LastGarbageWarningMillis.<p>
    * Timestamp of the last time a warning was issued due to reception of a
    * non-Coherence packet.
     */
    public long getLastGarbageWarningMillis()
        {
        return __m_LastGarbageWarningMillis;
        }
    
    // Accessor for the property "MaximumPacketLength"
    /**
     * Getter for property MaximumPacketLength.<p>
    * The absolute maximum number of bytes that can be placed into an outgoing
    * Packet.
    * 
    * @see ClusterConfig
     */
    public int getMaximumPacketLength()
        {
        return __m_MaximumPacketLength;
        }
    
    // Accessor for the property "MessageQueue"
    /**
     * Getter for property MessageQueue.<p>
    * Stores the Message Queue for each Service.
     */
    public com.tangosol.coherence.component.util.Queue getMessageQueue(int i)
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid;
        
        com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid service = getService(i);
        return service == null ? null : service.getQueue();
        }
    
    // Accessor for the property "PreferredPacketLength"
    /**
     * Getter for property PreferredPacketLength.<p>
    * The preferred number of bytes that can be placed into an outgoing Packet.
    *  Used here to figure out how many ACK could be coalesced together, as
    * well as the size of Packets in the UdpPacketPool.
    * 
    * @see ClusterConfig
     */
    public int getPreferredPacketLength()
        {
        return __m_PreferredPacketLength;
        }
    
    // Accessor for the property "Publisher"
    /**
     * Getter for property Publisher.<p>
    * The Cluster's Publisher daemon.
     */
    public PacketPublisher getPublisher()
        {
        return __m_Publisher;
        }
    
    // Accessor for the property "Service"
    /**
     * Getter for property Service.<p>
    * Indexed property of Services that are running on this Member. The Service
    * is used to instantiate Message objects from incoming Packet information.
     */
    public com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid getService(int i)
        {
        return null;
        }
    
    // Accessor for the property "StatsReceived"
    /**
     * Getter for property StatsReceived.<p>
    * Statistics: total number of received Packets.
     */
    public long getStatsReceived()
        {
        return __m_StatsReceived;
        }
    
    // Accessor for the property "StatsRepeated"
    /**
     * Getter for property StatsRepeated.<p>
    * Statistics: number of repeated packets (received more then once)
     */
    public long getStatsRepeated()
        {
        return __m_StatsRepeated;
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
    
    /**
     * Instantiate a new Message for the specified packet. If the corresponding
    * service has terminated, make a "serviceless" packet to be discarded
    * later.
    * 
    * @param member   the Member which sent the packet, may be null in the case
    * of broadcasts
    * @param packet     the packet
    * 
    * @return  a newly instantiated message (must not be null)
     */
    protected com.tangosol.coherence.component.net.Message instantiateMessage(com.tangosol.coherence.component.net.Member member, com.tangosol.coherence.component.net.packet.MessagePacket packet)
        {
        // import Component.Net.Message;
        // import Component.Util.Daemon.QueueProcessor.Service.Grid as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid;
        
        Message msg;
        int     nType   = packet.getMessageType();
        com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid service = getService(packet.getServiceId());
        if (service == null)
            {
            // There is no com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid for incoming packet;
            // we have to create a "serviceless" message to collect all the
            // Sequel packages and discard the message later at onMessage()
            msg = new Message();
            }
        else
            {
            // make message
            msg = service.instantiateMessage(nType);
            if (msg == null)  
                {
                throw new IllegalStateException("Failed to instantiate Message Type="
                    + nType + " for Service=" + service.getServiceName());
                }
            }
        
        // configure message
        msg.setDeserializationRequired(true);
        msg.setMessageType(nType);
        msg.setFromMember(member);
        msg.setMessagePartCount(packet.getMessagePartCount());
        msg.setPacket(0, packet);
        
        return msg;
        }
    
    // Declared at the super level
    /**
     * Create the queue for this QueueProcessor.
     */
    protected com.tangosol.coherence.component.util.Queue instantiateQueue()
        {
        return (PacketReceiver.InQueue) _findChild("InQueue");
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
    
    /**
     * Handle malformed UdpPacket.
    * 
    * @param udppacket   the UdpPacket which was malformed
    * @param nBundle  the bundle in which the error was encountered
    * @param e  the processing error
     */
    protected void onMalformedBuffer(java.nio.ByteBuffer buffer, Exception e)
        {
        // import Component.Net.Packet;
        // import com.tangosol.io.nio.ByteBufferReadBuffer;
        // import com.tangosol.io.ReadBuffer$BufferInput as com.tangosol.io.ReadBuffer.BufferInput;
        // import com.tangosol.util.Base;
        
        if (!isExiting())
            {  
            try
                {
                com.tangosol.io.ReadBuffer.BufferInput input = new ByteBufferReadBuffer(buffer).getBufferInput();
                
                input.reset();
                if (Packet.isForCoherence(input))
                    {
                    _trace("An exception occurred while processing packet "
                        + getStackTrace(e) + "\nexception will be ignored.", 2);
                    }
                else
                    {
                    // periodically log that garbage is being received
                    long ldtNow  = Base.getSafeTimeMillis();
                    long ldtWarn = getLastGarbageWarningMillis();
                    long cMillis = ldtNow - ldtWarn;
                    long cJunk   = getGarbagePacketCount() + 1L;
        
                    if (cMillis > 10000L)
                        {
                        setLastGarbageWarningMillis(ldtNow);
                        if (ldtWarn != 0L)
                            {
                            long cRate = cJunk / (cMillis / 1000L);
                            _trace("Dropped " + cJunk + " non-Coherence packets (" +    
                                cRate + "/sec); ",
                                cRate < 100L ? 4 : 2);
                            cJunk = 0L;
                            }
                        }
                    
                    setGarbagePacketCount(cJunk);
                    }
                }
            catch (Throwable x) {}
            }
        }
    
    /**
     * Called on ClusterService thread when a Member is removed from the cluster.
     */
    public void onMemberLeft(com.tangosol.coherence.component.net.Member member)
        {
        // NOTE: this method runs on the ClusterService thread.  Rather then trying to
        // do concurrent thread-safe with the Receiver, we will send the Receiver an
        // indication that it needs to do cleanup.  While hacky we simply push the
        // dead member into the Receiver's queue.  See onNotify() for the other half
        // of this hack.
        
        getQueue().add(member);
        }
    
    public void onMessage(com.tangosol.coherence.component.net.Message msg)
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid;
        
        // discard a message that came after the service has been stopped
        com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid service = msg.getService();
        if (service == null)
            {
            msg.releasePackets(getBufferManager());
            }
        else
            {
            service.getQueue().add(msg);
            com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid servicePending = getFlushPendingService();
            if (servicePending != service)
                {
                if (servicePending != null)
                    {            
                    servicePending.getQueue().flush();
                    }
                setFlushPendingService(service);
                }
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
        // import Component.Util.Queue as com.tangosol.coherence.component.util.Queue;
        // import Component.Net.Member;
        // import Component.Net.Packet;
        // import com.oracle.coherence.common.io.BufferManager;
        // import java.net.SocketAddress;
        // import java.nio.ByteBuffer;
        // import java.io.IOException;
        
        ByteBuffer    buffer    = null;
        SocketAddress addrSrc   = null;
        BufferManager mgr       = getBufferManager();
        com.tangosol.coherence.component.util.Queue         queue     = getQueue();
        Packet[]      aPacket   = new Packet[1];
        int           nMemberId = getMemberId();
        int           cReceived = 0;
        
        try
            {
            Object oItem = queue.removeNoWait();
            while (true)
                {
                if (oItem == null)
                    {
                    break;
                    }
                else if (oItem instanceof ByteBuffer)
                    {
                    buffer       = (ByteBuffer) oItem;
                    aPacket      = Packet.extract(addrSrc, buffer, mgr, aPacket, nMemberId);
                    int cPackets = aPacket.length;
        
                    for (int i = 0; i < cPackets; ++i)
                        {
                        Packet packet = aPacket[i];
                        if (packet == null)
                            {
                            cPackets = i;
                            break;
                            }
                        else
                            {
                            onPacket(packet);
                            }
                        }
                    cReceived += cPackets;
        
                    if (cReceived > 30000)
                        {
                        // ensure we periodically update statistics;
                        // 30,000 packets would represent about 1/2 second worth volume
                        // of 1468b packets on 1gb nic
                        setStatsReceived(getStatsReceived() + cReceived);
                        cReceived = 0;
                        }
                    }
                else if (oItem instanceof PacketReceiver.AddressedBuffer) // only for broadcasts
                    {
                    PacketReceiver.AddressedBuffer bufferAddr = (PacketReceiver.AddressedBuffer) oItem;
                    
                    oItem   = bufferAddr.getBuffer();
                    addrSrc = bufferAddr.getSourceAddress();
        
                    continue;
                    }
                else if (oItem instanceof Member)
                    {
                    // see onMemberLeft
                    cleanup((Member) oItem);
                    }
        
                addrSrc = null;
                oItem   = queue.removeNoWait();
                }
            setStatsReceived(getStatsReceived() + cReceived);
            }
        catch (IOException e)
            {
            onMalformedBuffer(buffer, e);
            }
        catch (RuntimeException e)
            {
            if (isExiting())
                {
                // ignore exception
                return;
                }
            throw e;
            }
        }
    
    protected void onPacket(com.tangosol.coherence.component.net.Packet packet)
        {
        // import Component.Net.Packet;
        // import Component.Net.Packet.DiagnosticPacket;
        // import Component.Net.Packet.MessagePacket;
        // import Component.Net.Packet.MessagePacket.Broadcast;
        // import Component.Net.Packet.MessagePacket.Directed;
        // import Component.Net.Packet.MessagePacket.Sequel;
        // import Component.Net.Packet.NotifyPacket.Ack;
        // import Component.Net.Packet.NotifyPacket.Request;
        // import Component.Net.Member;
        
        // a Packet is either from a Member that has not joined the cluster yet,
        // so the FromId will be zero, or the Member is (or thinks it is)
        // part of the cluster, so the FromId will be non-zero;
        // verify that the sending Member is known to this Member
        int    nFromId = packet.getFromId();
        Member member  = getMember(nFromId);
        if (member == null)
            {
            // packet is from unknown Member;
            // allow it to be processed only if it has not joined (fromId is zero)
            // or its a broadcast Packet (toId is zero)
            if (nFromId != 0 && packet.getToId() != 0)
                {
                // cannot receive an addressed Packet from an unknown Member
                if (packet instanceof MessagePacket)
                    {
                    getBufferManager().release(((MessagePacket) packet).getByteBuffer());
                    }
                return;
                }
            }
        else if (packet.isConfirmationRequired())
            {
            // translate fromId, and confirm
            MessagePacket msgPacket = (MessagePacket) packet;
            msgPacket.setFromMessageId(Packet.translateTrint(
                (int) msgPacket.getFromMessageId(), member.getLastIncomingMessageId()));
        
            confirm(member, msgPacket);
            }
        
        int nType = packet.getPacketType();
        switch (nType)
            {
            case Packet.TYPE_BROADCAST:
                onPacketBroadcast(member, (Broadcast) packet);
                break;
        
            case Packet.TYPE_DIRECTED_ONE:
            case Packet.TYPE_DIRECTED_FEW:
            case Packet.TYPE_DIRECTED_MANY:
                onPacketDirected(member, (Directed) packet);
                break;
        
            case Packet.TYPE_SEQUEL_ONE:
            case Packet.TYPE_SEQUEL_FEW:
            case Packet.TYPE_SEQUEL_MANY:
                onPacketSequel(member, (Sequel) packet);
                break;
        
            case Packet.TYPE_REQUEST:
                onPacketRequest(member, (Request) packet);
                break;
        
            case Packet.TYPE_ACK:
                onPacketAck(member, (Ack) packet);
                break;
        
            case Packet.TYPE_DIAGNOSTIC:
                onPacketDiagnostic(member, (DiagnosticPacket) packet);
                break;
        
            default:
                throw new IllegalArgumentException("unknown packet type: " + nType);
            }
        
        // update stats
        if (member != null)
            {
            member.setStatsReceived(member.getStatsReceived() + 1);
            }
        }
    
    /**
     * Process an Ack packet.
     */
    protected void onPacketAck(com.tangosol.coherence.component.net.Member member, com.tangosol.coherence.component.net.packet.notifyPacket.Ack packetAck)
        {
        // import com.tangosol.net.internal.PacketComparator;
        // import com.tangosol.net.internal.PacketIdentifier;
        
        if (member != null)
            {
            PacketIdentifier pidMemberNewestFrom = member.getNewestFromPacketId();
            PacketIdentifier pidAckNewestFrom    = packetAck.getNewestToPacketId
                                    (member.getLastIncomingMessageId());
        
            if (PacketComparator.compare(pidAckNewestFrom, pidMemberNewestFrom) > 0)
                {
                member.setNewestFromPacketId(pidAckNewestFrom);
                }
        
            member.setPreferredAckSize(packetAck.getPreferredAckSize());
            getConfirmationQueue().add(packetAck);
            }
        }
    
    protected void onPacketBroadcast(com.tangosol.coherence.component.net.Member member, com.tangosol.coherence.component.net.packet.messagePacket.Broadcast packet)
        {
        onMessage(instantiateMessage(member, packet));
        }
    
    /**
     * Process a DiagnosticPacket.
     */
    protected void onPacketDiagnostic(com.tangosol.coherence.component.net.Member member, com.tangosol.coherence.component.net.packet.DiagnosticPacket packetDiag)
        {
        // import Component.Net.Member;
        // import Component.Net.Packet.DiagnosticPacket;
        // import Component.Util.Daemon.QueueProcessor.PacketProcessor.PacketPublisher;
        // import com.tangosol.util.ClassHelper;
        // import com.tangosol.coherence.config.Config;
        
        // Diagnostic packets are sent only when some "real" packet is about to hit the
        // timeout threshold. Receiving the "ping" instance apparently proves at least
        // the unidirectional passage, while receiving the "pong" proves the bi-directional
        // passage and the question of a TCMP bug.
        
        _trace("Received " + packetDiag + " regarding connection with " +
            member.toString(Member.SHOW_STATS), 5);
        
        byte cTTL = packetDiag.getTimeToLive();
        if (cTTL > 0)
            {
            // respond with reduced TTL
            DiagnosticPacket packetResp = new DiagnosticPacket();
            packetResp.setTimeToLive(--cTTL);
            packetResp.setToId(packetDiag.getFromId());
            packetResp.setPreferredPortUsed(packetDiag.isPreferredPortUsed());
        
            getPublisher().sendDiagnosticPacket(packetResp);
            }
        
        // run custom plugin
        String sClassPlugin = Config.getProperty("coherence.tcmp.diag.plugin");
        if (sClassPlugin != null)
            {
            try
                {
                ((Runnable) ClassHelper.newInstance(Class.forName(sClassPlugin),
                    new Object[]{member})).run();
                }
            catch (Throwable e)
                {
                _trace("Failed to run diagnostic plugin " + sClassPlugin, 1);
                _trace(e);
                }
            }
        }
    
    protected void onPacketDirected(com.tangosol.coherence.component.net.Member member, com.tangosol.coherence.component.net.packet.messagePacket.Directed packet)
        {
        // import Component.Net.Member;
        // import Component.Net.Message;
        // import Component.Net.Packet;
        // import Component.Net.Packet.MessagePacket.Sequel;
        // import Component.Util.WindowedArray;
        // import com.tangosol.util.LongArray;
        // import java.util.Iterator;
        
        if (member == null)
            {
            getBufferManager().release(packet.getByteBuffer());
            return;
            }
        
        // the array of incoming Messages from the Member that sent the packet
        WindowedArray waMsg = member.getMessageIncoming();
        
        // the point-to-point Message id assigned to the Message that the
        // Directed Packet is the "header" for (ie. first or only part of)
        long lMsgFirst = waMsg.getFirstIndex();
        long lToMsgId  = Packet.translateTrint(packet.getToMessageId(), lMsgFirst);
        
        if (lToMsgId >= lMsgFirst && waMsg.get(lToMsgId) == null)
            {
            // the Message has not yet been processed and it does not
            // exist; instantiate it and add it to the array
            Message msg = instantiateMessage(member, packet);
            waMsg.set(lToMsgId, msg);
        
            long lFromMsgId = packet.getFromMessageId();
            msg.setFromMessageId(lFromMsgId);
        
            // check for sequel packets
            if (msg.getMessagePartCount() > 1)
                {
                // check to see if any parts of the Message are in the pile;
                // the "pile" is a Map keyed by Message id (type Long) with
                // the corresponding value being a List of Sequel Packets
            
                // grab any Packets already in the sequel pile
                LongArray laPile = member.getMessagePile();
                if (!laPile.isEmpty())
                    {
                    LongArray laSequel = (LongArray) laPile.get(lFromMsgId);
                    if (laSequel != null)
                        {
                        for (Iterator iter = laSequel.iterator(); iter.hasNext(); )
                            {
                            Sequel packetSequel = (Sequel) iter.next();
                            msg.setPacket(packetSequel.getMessagePartIndex(), packetSequel);
                            }
                        }
                    }
        
                // put the Message into the Map so subsequent
                // sequel Packets can "find their way home"
                laPile.set(lFromMsgId, msg);
                }
        
            // check for completed Messages in the array
            if (lToMsgId == lMsgFirst)
                {            
                checkReadyMessages(member);
                }
            else if (isNackEnabled())
                {
                // if not the first message then there is at least one incomplete
                // message in front of us, indicating out of order packets, or packet loss
                // prepare a Nack
                getPublisher().scheduleNack(member);
                }
            }
        else
            {
            getBufferManager().release(packet.getByteBuffer());
        
            setStatsRepeated(getStatsRepeated() + 1);
            member.setStatsRepeated(member.getStatsRepeated() + 1);
            }
        }
    
    protected void onPacketRequest(com.tangosol.coherence.component.net.Member member, com.tangosol.coherence.component.net.packet.notifyPacket.Request packet)
        {
        throw new UnsupportedOperationException();
        }
    
    protected void onPacketSequel(com.tangosol.coherence.component.net.Member member, com.tangosol.coherence.component.net.packet.messagePacket.Sequel packet)
        {
        // import Component.Net.Member;
        // import Component.Net.Message;
        // import Component.Net.Packet;
        // import Component.Util.WindowedArray;
        // import com.tangosol.util.LongArray;
        // import com.tangosol.util.SimpleLongArray;
        
        if (member == null)
            {
            getBufferManager().release(packet.getByteBuffer());
            return;
            }
        
        // the "pile" of Packets/Messages from the Member
        LongArray laPile     = member.getMessagePile();
        long      lLastMsgId = member.getLastIncomingMessageId();
        long      lFromMsgId = packet.getFromMessageId();
        boolean   fRepeated  = false;
        
        if (lFromMsgId > lLastMsgId)
            {
            int    iPart = packet.getMessagePartIndex();
            Object oVal  = laPile.get(lFromMsgId);
        
            if (oVal instanceof Message)
                {
                // the Pile contains the Message; add Packet to Message
                Message msg = (Message) oVal;
                if (msg.getPacket(iPart) == null)
                    {
                    msg.setPacket(iPart, packet);
        
                    // the array of incoming Messages from the Member that sent the packet
                    WindowedArray waMsg = member.getMessageIncoming();
        
                    // it is possible that this Sequel Packet was the last
                    // Packet to complete a Message
                    if (msg == waMsg.get(waMsg.getFirstIndex()))
                        {
                        checkReadyMessages(member);
                        }
                    else if (isNackEnabled())
                        {
                        // if not the first message then there is at least one incomplete
                        // message in front of us, indicating out of order packets, or packet loss
                        // prepare a Nack
                        getPublisher().scheduleNack(member);
                        }
                    }
                else
                    {
                    fRepeated = true;
                    }
                }
            else
                {
                // the Pile contains a List of Sequel Packets; the
                // Message does not exist yet because the Directed
                // Packet that heads the Message has not yet been
                // received
                
                LongArray laSequel = (LongArray) oVal;
                if (laSequel == null)
                    {
                    // instantiate long-array of sequel packets
                    laSequel = new SimpleLongArray();
        
                    // store the long-array of sequel packets
                    // under the only message id that we have,
                    // which is the message id as known by the
                    // sender (not this member's message id,
                    // which will come in the directed packet
                    // from which the message will be created)
                    laPile.set(lFromMsgId, laSequel);
                    }
                else
                    {
                    fRepeated = (laSequel.get(iPart) != null);
                    }
        
                if (!fRepeated)
                    {                
                    // store the sequel packet with any others for this
                    // message that arrived before the directed packet
                    laSequel.set(iPart, packet);
        
                    if (isNackEnabled())
                        {
                        // since we haven't received the directed packet
                        // we know we are missing packets, schedule a nack
                        // if one is not already scheduled
                        getPublisher().scheduleNack(member);
                        }                
                    }
                }
            }
        else
            {
            fRepeated = true;
            }
        
        if (fRepeated)
            {         
            getBufferManager().release(packet.getByteBuffer());
        
            setStatsRepeated(getStatsRepeated() + 1);
            member.setStatsRepeated(member.getStatsRepeated() + 1);
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
     * Reset the statistics.
     */
    public void resetStats()
        {
        // import com.tangosol.util.Base;
        
        setStatsReceived(0L);
        setStatsRepeated(0L);
        setStatsReset   (Base.getSafeTimeMillis());
        }
    
    // Accessor for the property "AckSendQueue"
    /**
     * Setter for property AckSendQueue.<p>
    * The Queue on which Ack Packets are enqueued for sending.
     */
    public void setAckSendQueue(com.tangosol.coherence.component.util.Queue queue)
        {
        _assert(!isStarted());
        
        __m_AckSendQueue = (queue);
        }
    
    // Accessor for the property "BufferManager"
    /**
     * Setter for property BufferManager.<p>
    * The BufferManager all ByteBuffers used for incoming packets are acquired
    * from.
     */
    public void setBufferManager(com.oracle.coherence.common.io.BufferManager allocator)
        {
        __m_BufferManager = allocator;
        }
    
    // Accessor for the property "ConfirmationQueue"
    /**
     * Setter for property ConfirmationQueue.<p>
    * The Queue on which incoming Ack Packets are enqueued for processing.
     */
    public void setConfirmationQueue(com.tangosol.coherence.component.util.Queue queue)
        {
        _assert(!isStarted());
        
        __m_ConfirmationQueue = (queue);
        }
    
    // Accessor for the property "FlushPendingService"
    /**
     * Setter for property FlushPendingService.<p>
    * Service which must be flushed prior to the receiver entering a blocking
    * state.
     */
    protected void setFlushPendingService(com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid service)
        {
        __m_FlushPendingService = service;
        }
    
    // Accessor for the property "GarbagePacketCount"
    /**
     * Setter for property GarbagePacketCount.<p>
    * The number of non-Coherence packets which have arrived since the last
    * warning was issued.
     */
    protected void setGarbagePacketCount(long cPackets)
        {
        __m_GarbagePacketCount = cPackets;
        }
    
    // Accessor for the property "LastGarbageWarningMillis"
    /**
     * Setter for property LastGarbageWarningMillis.<p>
    * Timestamp of the last time a warning was issued due to reception of a
    * non-Coherence packet.
     */
    protected void setLastGarbageWarningMillis(long ldtWarning)
        {
        __m_LastGarbageWarningMillis = ldtWarning;
        }
    
    // Accessor for the property "MaximumPacketLength"
    /**
     * Setter for property MaximumPacketLength.<p>
    * The absolute maximum number of bytes that can be placed into an outgoing
    * Packet.
    * 
    * @see ClusterConfig
     */
    public void setMaximumPacketLength(int cbMax)
        {
        _assert(!isStarted());
        
        __m_MaximumPacketLength = (cbMax);
        }
    
    // Accessor for the property "NackEnabled"
    /**
     * Setter for property NackEnabled.<p>
    * Specifies if the receiver should use request packets, i.e. NACKs to do
    * early packet loss detection.
     */
    public void setNackEnabled(boolean fUseRequestPackets)
        {
        __m_NackEnabled = fUseRequestPackets;
        }
    
    // Accessor for the property "PreferredPacketLength"
    /**
     * Setter for property PreferredPacketLength.<p>
    * The preferred number of bytes that can be placed into an outgoing Packet.
    *  Used here to figure out how many ACK could be coalesced together, as
    * well as the size of Packets in the UdpPacketPool.
    * 
    * @see ClusterConfig
     */
    public void setPreferredPacketLength(int cBytes)
        {
        _assert(!isStarted());
        
        __m_PreferredPacketLength = (cBytes);
        }
    
    // Accessor for the property "Publisher"
    /**
     * Setter for property Publisher.<p>
    * The Cluster's Publisher daemon.
     */
    public void setPublisher(PacketPublisher publisher)
        {
        __m_Publisher = publisher;
        }
    
    // Accessor for the property "StatsReceived"
    /**
     * Setter for property StatsReceived.<p>
    * Statistics: total number of received Packets.
     */
    protected void setStatsReceived(long cPackets)
        {
        __m_StatsReceived = cPackets;
        }
    
    // Accessor for the property "StatsRepeated"
    /**
     * Setter for property StatsRepeated.<p>
    * Statistics: number of repeated packets (received more then once)
     */
    protected void setStatsRepeated(long cPackets)
        {
        __m_StatsRepeated = cPackets;
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
    
    // Declared at the super level
    public String toString()
        {
        return get_Name() + ':' + formatStats();
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketReceiver$AddressedBuffer
    
    /**
     * A ByteBuffer and corresponding source address.
     * 
     * @since 12.2.3
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class AddressedBuffer
            extends    com.tangosol.coherence.Component
        {
        // ---- Fields declarations ----
        
        /**
         * Property Buffer
         *
         * The packet payload.
         */
        private java.nio.ByteBuffer __m_Buffer;
        
        /**
         * Property SourceAddress
         *
         * The source address of the packet.
         */
        private java.net.SocketAddress __m_SourceAddress;
        
        // Default constructor
        public AddressedBuffer()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public AddressedBuffer(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketReceiver.AddressedBuffer();
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
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/packetProcessor/PacketReceiver$AddressedBuffer".replace('/', '.'));
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
        
        // Accessor for the property "Buffer"
        /**
         * Getter for property Buffer.<p>
        * The packet payload.
         */
        public java.nio.ByteBuffer getBuffer()
            {
            return __m_Buffer;
            }
        
        // Accessor for the property "SourceAddress"
        /**
         * Getter for property SourceAddress.<p>
        * The source address of the packet.
         */
        public java.net.SocketAddress getSourceAddress()
            {
            return __m_SourceAddress;
            }
        
        // Accessor for the property "Buffer"
        /**
         * Setter for property Buffer.<p>
        * The packet payload.
         */
        public void setBuffer(java.nio.ByteBuffer bufBuffer)
            {
            __m_Buffer = bufBuffer;
            }
        
        // Accessor for the property "SourceAddress"
        /**
         * Setter for property SourceAddress.<p>
        * The source address of the packet.
         */
        public void setSourceAddress(java.net.SocketAddress addressSource)
            {
            __m_SourceAddress = addressSource;
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketReceiver$InQueue
    
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
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketReceiver.InQueue();
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
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/packetProcessor/PacketReceiver$InQueue".replace('/', '.'));
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
        }
    }
