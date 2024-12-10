
/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.Message

package com.tangosol.coherence.component.net;

import com.tangosol.coherence.Component;
import com.tangosol.coherence.component.net.memberSet.DependentMemberSet;
import com.tangosol.coherence.component.net.memberSet.SingleMemberSet;
import com.tangosol.coherence.component.net.packet.MessagePacket;
import com.tangosol.coherence.component.net.packet.messagePacket.Directed;
import com.tangosol.coherence.component.net.packet.messagePacket.Sequel;
import com.oracle.coherence.common.base.Disposable;
import com.tangosol.internal.tracing.SpanContext;
import com.tangosol.internal.tracing.TracingHelper;
import com.tangosol.io.MultiBufferReadBuffer;
import com.tangosol.io.ReadBuffer;
import com.tangosol.io.nio.ByteBufferReadBuffer;
import com.tangosol.util.Base;
import com.tangosol.util.ExternalizableHelper;
import java.util.Map;

/**
 * The Message contains all of the information necessary to describe a message
 * to send or to describe a received message.
 * <p>
 * With regards to the use of Message components within clustered Services,
 * Services are designed by dragging Message components into them as static
 * children. These Messages are the components that a Service can send to other
 * running instances of the same Service within a cluster. To send a Message, a
 * Service calls <code>instantiateMessage(String sMsgName)</code> with the name
 * of the child, then configures the Message object and calls Service.send
 * passing the Message. An incoming Message is created by the Message Receiver
 * by calling the <code>Service.instantiateMessage(int nMsgType)</code> and the
 * configuring the Message using the Received data. The Message is then queued
 * in the Service's Queue. When the Service thread gets the Message out of the
 * Queue, it invokes onMessage passing the Message, and the default
 * implementation for onMessage in turn calls <code>onReceived()</code> on the
 * Message object.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class Message
        extends    com.tangosol.coherence.component.Net
        implements com.tangosol.internal.net.MessageComponent
    {
    // ---- Fields declarations ----
    
    /**
     * Property BUFFER_COUNT_ORDER_BIT
     *
     * The bit in the BufferUsageCounter which is used to indicate that there
     * is an unordered release.  0x40000000
     */
    public static final int BUFFER_COUNT_ORDER_BIT = 1073741824;
    
    /**
     * Property BufferController
     *
     * The life cycle controller of the buffers (MultiBufferWriteBuffer,
     * BufferSequence) containing the serialized form of this Message.
     * 
     * For outgoing messages the dispose() method on the controller should be
     * called when the delivery of all packets is confirmed for "datagram"
     * transport and all delivery receipts for "bus" transport have been
     * received (see releaseOutgoing() method).  It is the responsibility of
     * the transport (datagram/bus) to call releaseOutgoing.  The controller is
     * never null for outgoing messages.
     * 
     * For incoming messages: not used as of 12.1.2
     */
    private transient com.oracle.coherence.common.base.Disposable __m_BufferController;
    
    /**
     * Property BufferUsageCounter
     *
     * The count of the number of active users of the buffer(s) managed by the
     * BufferController.  When the count is decremented to zero it is ready for
     * disposal.
     * 
     * Currently this will only be > 1 for outgoing messages.  See
     * MessageHandler.sendMulti
     */
    private transient int __m_BufferUsageCounter;
    
    /**
     * Property DeserializationRequired
     *
     * True if the message requires deserialization before it can be processed.
     * 
     * @see PacketReceiver#instantiateMessage
     * @see MessageHandler#onMessage
     * 
     * @functional
     */
    
    /**
     * Property FromMember
     *
     * The Member that is sending (or that did send) the Message. During the
     * discovery phase (and only for Service 0), this could be a temporary
     * Member object with an Id value of zero. Set by the Dispatcher for
     * outgoing Messages.
     */
    private Member __m_FromMember;
    
    /**
     * Property FromMessageId
     *
     * The Message number assigned to this Message by the sender of the
     * Message. Set by the PacketPublisher.InQueue for outgoing Messages.
     */
    private long __m_FromMessageId;
    
    /**
     * Property MASK_DESER_REQ
     *
     * Bit used to represent the DeserializationRequired property value.
     */
    private static final int MASK_DESER_REQ = 2;
    
    /**
     * Property MASK_NOTIFY
     *
     * Bit used to represent the NotifyDelivery property.
     */
    private static final int MASK_NOTIFY = 1;
    
    /**
     * Property MessageType
     *
     * An integer value, unique to the Service that created this Message, that
     * identifies the Message type.
     * 
     * Message types less than 0 are reserved for "internal" notification
     * Messages generated by the Cluster Service (service 0).
     */
    private int __m_MessageType;
    
    /**
     * Property NotifyDelivery
     *
     * Set to true to get a "return receipt" notification when the Message has
     * been delivered (or when Message is determined to be undeliverable).
     * 
     * As of Coherence 3.2, this provides a stronger guarantee than an ack of
     * all the message's packets.  This notification will not be delivered
     * until all living recipients have ack'd all older packets from us.  This
     * ensures that nothing will stop [the living] recipients from processing
     * the message.
     * 
     * This notification generally has a very high latency (compared to a
     * poll), and is meant mostly for cleanup tasks.
     * 
     * @functional
     */
    
    /**
     * Property NullPacketCount
     *
     * Number of Packets that compose the Message that are null references.
     * When the Message is being assembled (on the receiver), the null Packets
     * are "missing", and when the Message is being acknowledge (on the
     * sender), the null Packets are the checked-off (acknowledged) ones.
     */
    private int __m_NullPacketCount;
    
    /**
     * Property Packet
     *
     * The packets that this message was constructed from (if received) or was
     * deconstructed to (if sent).
     * 
     * Note: The packets are not guaranteed to remain available once the
     * message has been serialized/deserialized.
     * 
     * Note:This property only utilized when the message is sent using the
     * "datagram" transport.
     */
    private com.tangosol.coherence.component.net.packet.MessagePacket[] __m_Packet;
    
    /**
     * Property Poll
     *
     * The Poll that this Message is a response to, or null if this Message is
     * not a response to a Poll (or the Poll has already been unregistered).
     */
    private transient Poll __m_Poll;
    
    /**
     * Property ReadBuffer
     *
     * For incoming messages, the ReadBuffer is used to deserialize the Message
     * payload. This property is "pre-set" only for incoming messages delivered
     * over any "bus" transport and calculated for the "datagram" transport.
     * 
     * For outgoing messages, the ReadBuffer is used to packetize the message
     * for "datagram" transport in the case when the Message needs to be sent
     * using multiple transports (see PacketPublisher#serializeMessage).
     */
    private transient com.tangosol.io.ReadBuffer __m_ReadBuffer;
    
    /**
     * Property Service
     *
     * The Service object that created the Message (for outgoing Messages) or
     * the Service object to which the Message is delivered (for incoming
     * Messages). In the case of an outgoing Message, the Service must set this
     * property before queueing the Message to be sent with the Dispatcher.
     * 
     * @see #onInit
     */
    private com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid __m_Service;
    
    /**
     * Property ToMemberSet
     *
     * The Set of Members to which the Message will be sent. May only be empty
     * for Messages being sent from/to the Service whose id is 0. As of
     * Coherence 3.7, it's never assigned for incoming messages.
     */
    private MemberSet __m_ToMemberSet;
    
    /**
     * Property ToMessageId
     *
     * Transient property used to order inbound Messages within a Service's
     * queue.
     * 
     * Note: the FromMessageId property cannot be re-used for the purpose due
     * to the case where a client thread sends a message addressed to its local
     * service and other service members.  In this case the Message object
     * would exist in two MultiQueues and each one needs a property to hold the
     * virtual index.
     */
    private transient long __m_ToMessageId;
    
    /**
     * Property ToPollId
     *
     * The PollId that this Message is a response to. Configured by the
     * respondTo method.
     */
    private long __m_ToPollId;
    
    /**
     * Property TracingSpanContext
     *
     * The OpenTracing SpanContext.
     * 
     * @since 14.4.1.0
     */
    private com.tangosol.internal.tracing.SpanContext __m_TracingSpanContext;
    
    // Default constructor
    public Message()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public Message(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
        return new com.tangosol.coherence.component.net.Message();
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
            clz = Class.forName("com.tangosol.coherence/component/net/Message".replace('/', '.'));
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
    
    public void addToMember(Member member)
        {
        ensureToMemberSet().add(member);
        }
    
    /**
     * Instantiate a copy of this message. This is quite different from the
    * standard "clone" since only the "transmittable" portion of the message
    * (and none of the internal) state should be cloned.
     */
    public Message cloneMessage()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid;
        
        com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid service = getService();
        if (service == null)
            {
            throw new IllegalStateException("Unable to clone message: " + this);
            }
        else
            {
            return service.instantiateMessage(getMessageType());
            }
        }
    
    /**
     * Route the message towards the proper service.  For local services (within
    * this member) the message is simply be placed on service's queue.  For
    * messages sent to services running on other members the message will be
    * forwarded to the publisher. The actual transmission of the message may be
    * deferred due to the send queue batching.
    * 
    * @param msg  the message to dispatch
     */
    protected void dispatch(Message msg)
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid;
        
        // configure the Message "from" information
        com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid    service    = msg.getService();
        Member  memberFrom = msg.getFromMember();
        if (memberFrom == null)
            {
            memberFrom = service.getThisMember();
            if (memberFrom != null)
                {
                msg.setFromMember(memberFrom);
                }
            }
        // else; this cluster node is still joining, ThisMember is not yet configured
        // and the AnnounceMember or RequestMember is set explicitly
        
        // identify local and or remote destination(s)
        MemberSet setMemberTo  = msg.getToMemberSet();
        final int LOCAL        = 1;
        final int REMOTE       = 2;
        int       nDestination;
        if (setMemberTo == null)
            {
            // unaddressed is messages is either broadcast or internal
            if (msg.isInternal())
                {
                nDestination = LOCAL;
        
                // COH-2949; note memberTo == memberFrom in this case
                MemberSet.writeBarrier(memberFrom);
                }
            else if (service.getServiceId() == 0)
                {
                nDestination = REMOTE; // broadcast; COH-2949 doesn't apply
                }
            else
                {
                throw new IllegalStateException("Broadcast Message sent from unexpected"
                    + " service " + service + " (Message=" + msg + ")");
                }
            }
        else
            {
            // addressed message
            int cMembers = setMemberTo.size();
        
            if (memberFrom.getId() == 0)
                {
                throw new IllegalStateException(
                    "Directed message sent before Member ID obtained: " + msg);
                }
            else if (setMemberTo.contains(memberFrom))
                {
                nDestination = cMembers == 1 ? LOCAL : (LOCAL | REMOTE);
                }
            else
                {
                nDestination = REMOTE;
                }
        
            if (cMembers > 1)
                {
                // replace any multi-MemberSet with a stable MemberSet
                setMemberTo = MemberSet.instantiate(setMemberTo);
                setMemberTo.remove(memberFrom);
                msg.setToMemberSet(setMemberTo);
                }
        
            setMemberTo.writeBarrier(); // COH-2949
            }
        
        boolean fLocal  = (nDestination & LOCAL)  != 0 && service.getQueue().add(msg);
        
        boolean fRemote = (nDestination & REMOTE) != 0 && service.getMessagePublisher().post(msg);
        
        // update stats
        service.setStatsSent(service.getStatsSent() + 1L);
        if (fLocal)
            {
            service.setStatsSentLocal(service.getStatsSentLocal() + 1L);
        
            if (!fRemote)
                {
                // the message is local-only (COH-13351)
                msg.notifyDelivery();
                }
            }
        }
    
    public MemberSet ensureToMemberSet()
        {
        MemberSet setMember = getToMemberSet();
        
        if (setMember == null)
            {
            setMember = new MemberSet();
            setToMemberSet(setMember);
            }
        
        return setMember;
        }
    
    // Declared at the super level
    /**
     * Getter for property _Parent.<p>
    * Parent component for this component. This property is not meant to be
    * designed directly.
     */
    public com.tangosol.coherence.Component get_Parent()
        {
        // import Component;
        
        Component parent = super.get_Parent();
        return parent == null ? getService() : parent;
        }
    
    // Accessor for the property "BufferController"
    /**
     * Getter for property BufferController.<p>
    * The life cycle controller of the buffers (MultiBufferWriteBuffer,
    * BufferSequence) containing the serialized form of this Message.
    * 
    * For outgoing messages the dispose() method on the controller should be
    * called when the delivery of all packets is confirmed for "datagram"
    * transport and all delivery receipts for "bus" transport have been
    * received (see releaseOutgoing() method).  It is the responsibility of the
    * transport (datagram/bus) to call releaseOutgoing.  The controller is
    * never null for outgoing messages.
    * 
    * For incoming messages: not used as of 12.1.2
     */
    public com.oracle.coherence.common.base.Disposable getBufferController()
        {
        return __m_BufferController;
        }
    
    // Accessor for the property "BufferUsageCounter"
    /**
     * Getter for property BufferUsageCounter.<p>
    * The count of the number of active users of the buffer(s) managed by the
    * BufferController.  When the count is decremented to zero it is ready for
    * disposal.
    * 
    * Currently this will only be > 1 for outgoing messages.  See
    * MessageHandler.sendMulti
     */
    public int getBufferUsageCounter()
        {
        return __m_BufferUsageCounter;
        }
    
    // Accessor for the property "Description"
    /**
     * Getter for property Description.<p>
    * Used for debugging purposes (from toString). Create a human-readable
    * description of the specific Message data.
     */
    public String getDescription()
        {
        return null;
        }
    
    // Accessor for the property "EstimatedByteSize"
    /**
     * Getter for property EstimatedByteSize.<p>
    * The estimated serialized size of this message.  A negative value
    * indicates that the size is unknown and that it is safe to estimate the
    * size via a double serialization.
     */
    public int getEstimatedByteSize()
        {
        return 1 +                          // boolean - lPollId != 0
               getToPollId() == 0L ? 0 : 3; // trint   - ToPollId
        }
    
    // Accessor for the property "FromMember"
    /**
     * Getter for property FromMember.<p>
    * The Member that is sending (or that did send) the Message. During the
    * discovery phase (and only for Service 0), this could be a temporary
    * Member object with an Id value of zero. Set by the Dispatcher for
    * outgoing Messages.
     */
    public Member getFromMember()
        {
        return __m_FromMember;
        }
    
    // Accessor for the property "FromMessageId"
    /**
     * Getter for property FromMessageId.<p>
    * The Message number assigned to this Message by the sender of the Message.
    * Set by the PacketPublisher.InQueue for outgoing Messages.
     */
    public long getFromMessageId()
        {
        return __m_FromMessageId;
        }
    
    /**
     * Return the number of packets that this message was constructed from (if
    * received) or packetized into (if sent).
     */
    public int getMessagePartCount()
        {
        // import Component.Net.Packet.MessagePacket;
        
        MessagePacket[] aPacket = getPacket();
        return aPacket == null ? 0 : aPacket.length;
        }
    
    // Accessor for the property "MessageType"
    /**
     * Getter for property MessageType.<p>
    * An integer value, unique to the Service that created this Message, that
    * identifies the Message type.
    * 
    * Message types less than 0 are reserved for "internal" notification
    * Messages generated by the Cluster Service (service 0).
     */
    public int getMessageType()
        {
        return __m_MessageType;
        }
    
    // Accessor for the property "NullPacketCount"
    /**
     * Getter for property NullPacketCount.<p>
    * Number of Packets that compose the Message that are null references. When
    * the Message is being assembled (on the receiver), the null Packets are
    * "missing", and when the Message is being acknowledge (on the sender), the
    * null Packets are the checked-off (acknowledged) ones.
     */
    public int getNullPacketCount()
        {
        return __m_NullPacketCount;
        }
    
    // Accessor for the property "Packet"
    /**
     * Getter for property Packet.<p>
    * The packets that this message was constructed from (if received) or was
    * deconstructed to (if sent).
    * 
    * Note: The packets are not guaranteed to remain available once the message
    * has been serialized/deserialized.
    * 
    * Note:This property only utilized when the message is sent using the
    * "datagram" transport.
     */
    public com.tangosol.coherence.component.net.packet.MessagePacket[] getPacket()
        {
        return __m_Packet;
        }
    
    // Accessor for the property "Packet"
    /**
     * Getter for property Packet.<p>
    * The packets that this message was constructed from (if received) or was
    * deconstructed to (if sent).
    * 
    * Note: The packets are not guaranteed to remain available once the message
    * has been serialized/deserialized.
    * 
    * Note:This property only utilized when the message is sent using the
    * "datagram" transport.
     */
    public com.tangosol.coherence.component.net.packet.MessagePacket getPacket(int i)
        {
        return getPacket()[i];
        }
    
    // Accessor for the property "Poll"
    /**
     * Getter for property Poll.<p>
    * The Poll that this Message is a response to, or null if this Message is
    * not a response to a Poll (or the Poll has already been unregistered).
     */
    public Poll getPoll()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid;
        
        Poll poll = __m_Poll;
        if (poll == null)
            {
            long lPollId = getToPollId();
            if (lPollId != 0L)
                {
                com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid service = getService();
                if (service != null)
                    {
                    setPoll(poll = (Poll) service.getPollArray().get(lPollId));
                    }
                }
            }
        return poll;
        }
    
    // Accessor for the property "ReadBuffer"
    /**
     * Getter for property ReadBuffer.<p>
    * For incoming messages, the ReadBuffer is used to deserialize the Message
    * payload. This property is "pre-set" only for incoming messages delivered
    * over any "bus" transport and calculated for the "datagram" transport.
    * 
    * For outgoing messages, the ReadBuffer is used to packetize the message
    * for "datagram" transport in the case when the Message needs to be sent
    * using multiple transports (see PacketPublisher#serializeMessage).
     */
    public com.tangosol.io.ReadBuffer getReadBuffer()
        {
        // import com.tangosol.io.ReadBuffer;
        // import com.tangosol.io.MultiBufferReadBuffer;
        // import com.tangosol.io.nio.ByteBufferReadBuffer;
        
        ReadBuffer buffer = __m_ReadBuffer;
        if (buffer != null || !isDeserializationRequired())
            {
            return buffer;
            }
        
        int cPackets = getMessagePartCount();
        switch (cPackets)
            {
            case 0:
                throw new IllegalStateException("empty message: " + this);
        
            case 1:
                return new ByteBufferReadBuffer(getPacket(0).getByteBuffer());
        
            default:
                {
                ReadBuffer[] arb = new ReadBuffer[cPackets];
                for (int i = 0; i < cPackets; ++i)
                    {
                    arb[i] = new ByteBufferReadBuffer(getPacket(i).getByteBuffer());
                    }
                return new MultiBufferReadBuffer(arb);
                }
            }
        }
    
    // Accessor for the property "Service"
    /**
     * Getter for property Service.<p>
    * The Service object that created the Message (for outgoing Messages) or
    * the Service object to which the Message is delivered (for incoming
    * Messages). In the case of an outgoing Message, the Service must set this
    * property before queueing the Message to be sent with the Dispatcher.
    * 
    * @see #onInit
     */
    public com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid getService()
        {
        return __m_Service;
        }
    
    // Accessor for the property "ToMemberSet"
    /**
     * Getter for property ToMemberSet.<p>
    * The Set of Members to which the Message will be sent. May only be empty
    * for Messages being sent from/to the Service whose id is 0. As of
    * Coherence 3.7, it's never assigned for incoming messages.
     */
    public MemberSet getToMemberSet()
        {
        return __m_ToMemberSet;
        }
    
    // Accessor for the property "ToMessageId"
    /**
     * Getter for property ToMessageId.<p>
    * Transient property used to order inbound Messages within a Service's
    * queue.
    * 
    * Note: the FromMessageId property cannot be re-used for the purpose due to
    * the case where a client thread sends a message addressed to its local
    * service and other service members.  In this case the Message object would
    * exist in two MultiQueues and each one needs a property to hold the
    * virtual index.
     */
    public long getToMessageId()
        {
        return __m_ToMessageId;
        }
    
    // Accessor for the property "ToPollId"
    /**
     * Getter for property ToPollId.<p>
    * The PollId that this Message is a response to. Configured by the
    * respondTo method.
     */
    public long getToPollId()
        {
        return __m_ToPollId;
        }
    
    // Accessor for the property "TracingSpanContext"
    /**
     * Getter for property TracingSpanContext.<p>
    * The OpenTracing SpanContext.
    * 
    * @since 14.4.1.0
     */
    public com.tangosol.internal.tracing.SpanContext getTracingSpanContext()
        {
        return __m_TracingSpanContext;
        }
    
    // Accessor for the property "Delivered"
    /**
     * Getter for property Delivered.<p>
    * True if the [outgoing] message was delivered to all recipients.
     */
    public boolean isDelivered()
        {
        // see releaseOutgoing
        return getBufferController() == null;
        }
    
    // Accessor for the property "DeserializationRequired"
    /**
     * Getter for property DeserializationRequired.<p>
    * True if the message requires deserialization before it can be processed.
    * 
    * @see PacketReceiver#instantiateMessage
    * @see MessageHandler#onMessage
    * 
    * @functional
     */
    public boolean isDeserializationRequired()
        {
        return (get_StateAux() & MASK_DESER_REQ) != 0;
        }
    
    // Accessor for the property "Internal"
    /**
     * Getter for property Internal.<p>
    * True for "internal" notification Messages (those that do not come from
    * the network but come from the cluster Service).
     */
    public boolean isInternal()
        {
        return getMessageType() < 0;
        }
    
    // Accessor for the property "NotifyDelivery"
    /**
     * Getter for property NotifyDelivery.<p>
    * Set to true to get a "return receipt" notification when the Message has
    * been delivered (or when Message is determined to be undeliverable).
    * 
    * As of Coherence 3.2, this provides a stronger guarantee than an ack of
    * all the message's packets.  This notification will not be delivered until
    * all living recipients have ack'd all older packets from us.  This ensures
    * that nothing will stop [the living] recipients from processing the
    * message.
    * 
    * This notification generally has a very high latency (compared to a poll),
    * and is meant mostly for cleanup tasks.
    * 
    * @functional
     */
    public boolean isNotifyDelivery()
        {
        return (get_StateAux() & MASK_NOTIFY) != 0;
        }
    
    // From interface: com.tangosol.internal.net.MessageComponent
    public boolean isRecipientCompatible(int nYear, int nMonth, int nPatch)
        {
        return getService().isVersionCompatible(getToMemberSet(), nYear, nMonth, nPatch);
        }
    
    // From interface: com.tangosol.internal.net.MessageComponent
    public boolean isRecipientCompatible(int nMajor, int nMinor, int nMicro, int nPatchSet, int nPatch)
        {
        return getService().isVersionCompatible(getToMemberSet(), nMajor, nMinor, nMicro, nPatchSet, nPatch);
        }

    @Override
    public boolean isRecipientCompatible(int nEncodedVersion)
        {
        return getService().isVersionCompatible(getToMemberSet(), nEncodedVersion);
        }

    @Override
    public boolean isRecipientPatchCompatible(int nEncodedVersion)
        {
        return getService().isPatchCompatible(getToMemberSet(), nEncodedVersion);
        }

    // From interface: com.tangosol.internal.net.MessageComponent
    public boolean isSenderCompatible(int nYear, int nMonth, int nPatch)
        {
        return getService().isVersionCompatible(getFromMember(), nYear, nMonth, nPatch);
        }
    
    // From interface: com.tangosol.internal.net.MessageComponent
    public boolean isSenderCompatible(int nMajor, int nMinor, int nMicro, int nPatchSet, int nPatch)
        {
        return getService().isVersionCompatible(getFromMember(), nMajor, nMinor, nMicro, nPatchSet, nPatch);
        }
    
    @Override
    public boolean isSenderCompatible(int nEncodedVersion)
        {
        return getService().isVersionCompatible(getFromMember(), nEncodedVersion);
        }

    @Override
    public boolean isSenderPatchCompatible(int nEncodedVersion)
        {
        return getService().isPatchCompatible(getFromMember(), nEncodedVersion);
        }

    /**
     * Ensure that the delivery notification is posted if necessary.
     */
    protected void notifyDelivery()
        {
        if (isNotifyDelivery())
            {
            setNotifyDelivery(false);
            getService().onMessageReceipt(this);
            }
        }
    
    /**
     * This is the event that occurs when a Message with NotifySent set to true
    * and the Message is sent and fully acknowledged. Note that this event does
    * not mean that all Members received the Message; it just means that those
    * Members that are still alive and that the Message was addressed to have
    * acknowledged the Message, as well as all older messages from this member.
     */
    public void onDelivery()
        {
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
        // import Component.Util.Daemon.QueueProcessor.Service.Grid as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid;
        
        super.onInit();
        
        _assert(get_Parent() == null,
            "Message must be instantiated using instantiateMessage() method.");
        }
    
    /**
     * This is the event that is executed when a Message is received.
    * <p>
    * It is the main processing event of the Message called by the
    * <code>Service.onMessage()</code> event. With regards to the use of
    * Message components within clustered Services, Services are designed by
    * dragging Message components into them as static children. These Messages
    * are the components that a Service can send to other running instances of
    * the same Service within a cluster. When the onReceived event is invoked
    * by a Service, it means that the Message has been received; the code in
    * the onReceived event is therefore the Message specific logic for
    * processing a received Message. For example, when onReceived is invoked on
    * a Message named FindData, the onReceived event should do the work to
    * "find the data", because it is being invoked by the Service that received
    * the "find the data" Message.
     */
    public void onReceived()
        {
        }
    
    /**
     * Split a message into packets in preparation for sending the message to
    * another member.  The packets will only be addressed to external members,
    * it is up to the Service.dispatchMessage method to handle local delivery.
    * 
    * @return true if the message was packetized and should be sent on the
    * network, false if the message was not packetized (all addressees are gone)
     */
    public boolean packetize(com.tangosol.coherence.component.util.daemon.queueProcessor.packetProcessor.PacketPublisher publisher, com.tangosol.coherence.component.net.memberSet.actualMemberSet.ServiceMemberSet setMembersBase, com.tangosol.io.ReadBuffer buffer, int cbPreferred, int cbMax)
        {
        // import Component.Net.MemberSet.DependentMemberSet;
        // import Component.Net.Packet.MessagePacket;
        // import Component.Net.Packet.MessagePacket.Directed;
        // import Component.Net.Packet.MessagePacket.Sequel;
        
        // NOTE: This implementation does not support broadcast messages, see
        // DiscoveryMessage.packetize for broadcast support
        
        DependentMemberSet setTo       = null;
        int                nMemberTo   = 0;
        MemberSet          setMemberTo = getToMemberSet();
        
        switch (setMemberTo.size())
            {
            case 0: // no one to send to
                return false;
        
            case 1: // common case
                nMemberTo = setMemberTo.getFirstId();
                if (nMemberTo == 0)
                    {
                    return false; // no one to send to
                    }
                break;
        
            default:
                setTo = new DependentMemberSet();
                setTo.setBaseSet(setMembersBase);
                setTo.addAll(setMemberTo);
                break;
            }
        
        // Identify packet count and sizes
        int cbBuffer         = buffer.length();   // total length of the message
        int cbDirectedBody   = Math.min(cbBuffer, // length of the direct body
            Directed.calcBodyLength(Directed.calcHeaderLength(setMemberTo), cbPreferred, cbMax));
        int cbSequelBody;                         // maximum body length of each sequel
        int cPackets;                             // number of Packets in this Message
        
        if (cbDirectedBody >= cbBuffer)
            {
            // message will use a single direct packet
            cPackets     = 1;
            cbSequelBody = 0;
            }
        else
            {
            // message will require sequel packets
            cbSequelBody   = Sequel.calcBodyLength(
                Sequel.calcHeaderLength(setMemberTo), cbPreferred, cbMax);
            int cbSequel   = cbBuffer - cbDirectedBody;
            cPackets       = 1 // DIRECTED
                + ((cbSequel + cbSequelBody - 1) / cbSequelBody);
            }
        
        setMessagePartCount(cPackets);
        
        int nMemberFrom = getFromMember().getId();
        int nServiceId  = getService().getServiceId();
        int nMsgType    = getMessageType();
        
        // create the Directed Packet
        Directed packetHead = new Directed();
        if (setTo == null)
            {
            packetHead.setToId(nMemberTo);
            }
        else
            {
            // The directed packet holds the supplied DependentMemberSet
            packetHead.setToMemberSet(setTo);
            }
        
        packetHead.setFromId(nMemberFrom);
        packetHead.setServiceId(nServiceId);
        packetHead.setMessageType(nMsgType);
        packetHead.setMessagePartCount(cPackets);
        packetHead.defineBufferView(buffer, 0, cbDirectedBody);
        
        setPacket(0, packetHead);
        
        // create the Sequel Packets
        for (int i = 1, cbWritten = cbDirectedBody; i < cPackets; ++i)
            {
            Sequel packet = new Sequel();
            if (setTo == null)
                {
                packet.setToId(nMemberTo);
                }
            else
                {
                // The value of Message.ToMemberSet is not guaranteed to be immutable,
                // and may change while the message is being sent, so we clone the one
                // belonging to the DirectedPacket.
                packet.setToMemberSet((DependentMemberSet) setTo.clone());
                }
        
            packet.setFromId(nMemberFrom);
            packet.setServiceId(nServiceId);
            packet.setMessageType(nMsgType);
            packet.setMessagePartCount(cPackets);
        
            int cb = Math.min(cbSequelBody, cbBuffer - cbWritten);
            packet.defineBufferView(buffer, cbWritten, cb);
            cbWritten += cb;
        
            packet.setMessagePartIndex(i);
            setPacket(i, packet);
            }
        
        return true;
        }
    
    /**
     * Asynchronously send this message.  The actual transmission of the message
    * may be deferred due to the send queue batching.
    * This method should not be called directly; see Grid#post(Message).
     */
    public void post()
        {
        dispatch(this);
        }
    
    /**
     * Preprocess this message.
    * 
    * @return true iff this message has been fully processed (onReceived was
    * called)
     */
    public boolean preprocess()
        {
        return false;
        }
    
    /**
     * Preprocess the sent notification of this message.
    * 
    * @return true iff this notification has been fully processed (onSent was
    * called)
     */
    public boolean preprocessSentNotification()
        {
        return false;
        }
    
    public void read(com.tangosol.io.ReadBuffer.BufferInput input)
            throws java.io.IOException
        {
        }
    
    public void readInternal(com.tangosol.io.ReadBuffer.BufferInput input)
            throws java.io.IOException
        {
        // check if the Message is in response to a poll
        if (input.readBoolean())
            {
            // three-byte Poll id
            int nId = Packet.readUnsignedTrint(input);
            setToPollId(Packet.translateTrint(nId,
                    getService().getPollArray().getFirstIndex()));
            }
        }
    
    /**
     * Read an object from the specified BufferInput.
    * 
    * @param input the BufferInput containing the serialized object
    * 
    * @return an instance of the deserialized object
    * 
    * @see writeObject
     */
    public Object readObject(com.tangosol.io.ReadBuffer.BufferInput input)
            throws java.io.IOException
        {
        return getService().readObject(input);
        }
    
    /**
     * This should be called by Message serialization methods in order to
    * propgate tracing information.
    * 
    * @since 14.1.1.0
     */
    protected void readTracing(com.tangosol.io.ReadBuffer.BufferInput input)
            throws java.io.IOException
        {
        // import com.tangosol.internal.tracing.TracingHelper;
        // import com.tangosol.util.ExternalizableHelper;
        // import java.util.Map;
        
        if (input.available() > 0)
            {
            Map mapSpanCtx = (Map) ExternalizableHelper.readObject(input);
            if (mapSpanCtx != null && !mapSpanCtx.isEmpty())
                {
                setTracingSpanContext(TracingHelper.getTracer().extract(mapSpanCtx));
                }
            }
        }
    
    /**
     * Release buffer(s) held by this incoming message.
     */
    public void releaseIncoming()
        {
        // import com.oracle.coherence.common.base.Disposable;
        
        // NOTE: releaseIncoming is only called by the deserializing thread, thus
        // this method does not need to be thread-safe.
        
        _assert(getBufferUsageCounter() == 1);
        
        Disposable controller = getBufferController();
        if (controller != null)
            {
            controller.dispose();
            }
        
        setBufferUsageCounter(0);
        
        releasePackets(getService().getBufferManager());
        }
    
    /**
     * Release the buffer(s) that held serialized form of this outgoing message.
    * This method must be called once per recipient.  An ordered release is
    * assumed, see multi-parameter version for unordered release.
    * 
    * @param fSuspect true if it is unclear if the message will be delivered
     */
    public final void releaseOutgoing(boolean fSuspect)
        {
        releaseOutgoing(fSuspect, /*fOrdered*/ true);
        }
    
    /**
     * Release the buffer(s) that held serialized form of this outgoing message.
    * This method must be called once per recipient.
    * 
    * @param fSuspect true if it is unclear if the message will be delivered
    * @param fOrdered true if messages are released in deliverey order, false
    * otherwise, a value of false can be specified at most once and must be
    * ultimately followed by a call to releaseOutgoingComplete(), the use of
    * false is reserved for TCMP/datagram
     */
    public void releaseOutgoing(boolean fSuspect, boolean fOrdered)
        {
        // import com.oracle.coherence.common.base.Disposable;
        
        // Note: releaseOutgoing may be called concurrently when there are multiple
        // destinations and using exabus; thus it must be thread-safe
        
        Disposable controller = getBufferController();
        if (controller == null)
            {
            throw new IllegalStateException();
            }
        
        int cUsage = getBufferUsageCounter();
        if (cUsage == 1) // dirty read
            {
            // optimization for common case of a single usage
            // this dirty read can only see a value of 1 in two cases
            // - this is the last thread needing to do a decrement, in which case it is safe
            // - there is a bug and multiple threads are here, in which case we miss the IllegalStateException below
            //   but we avoid the cost of unneeded synchronization if we don't have the bug
        
            // no need to assert like below, the above if wouldn't have passed
            cUsage = fOrdered ? 0 : BUFFER_COUNT_ORDER_BIT;
        
            setBufferUsageCounter(cUsage);
            }
        else
            {
            // failed dirty read or contended release; both cases are rare
            synchronized (this)
                {
                cUsage = getBufferUsageCounter() - 1;
                if (!fOrdered)
                    {
                    _assert((cUsage & BUFFER_COUNT_ORDER_BIT) == 0);
                    cUsage |= BUFFER_COUNT_ORDER_BIT;
                    }
                setBufferUsageCounter(cUsage);
                }
            }
        
        if ((cUsage & ~BUFFER_COUNT_ORDER_BIT) == 0)
            {
            // all receipts are in
            controller.dispose();
            setBufferController(null);
        
            if (cUsage == 0) // not marked as unordered
                {
                notifyDelivery();
                }
            }
        else if (cUsage < 0)
            {
            throw new IllegalStateException();
            }
        }
    
    /**
     * Finish the release of previous unordred releaseOutgoing.
     */
    public void releaseOutgoingComplete()
        {
        if (getBufferUsageCounter() == BUFFER_COUNT_ORDER_BIT) // dirty read; no users left; common case
            {
            setBufferUsageCounter(0);
            notifyDelivery();
            }
        else // potentially some users left; may be concurrently releasing
            {
            synchronized (this)
                {
                int cUsage = getBufferUsageCounter();
        
                _assert((cUsage & BUFFER_COUNT_ORDER_BIT) != 0);
        
                // remove the unordered bit
                setBufferUsageCounter(cUsage & ~BUFFER_COUNT_ORDER_BIT);
        
                if (cUsage == BUFFER_COUNT_ORDER_BIT) // no users left
                    {
                    notifyDelivery();
                    }
                // else; still users left; they will notify if needed during release
                }
            }
        }
    
    /**
     * Release buffer(s) held by this incoming message's packets.
    * 
    * @param mgr the BufferManager to release the buffers to
     */
    public void releasePackets(com.oracle.coherence.common.io.BufferManager mgr)
        {
        // import Component.Net.Packet.MessagePacket;
        
        MessagePacket[] aPacket = getPacket(); // null for messages from MessageBus
        if (aPacket != null)
            {
            for (int i = 0, c = getMessagePartCount(); i < c; ++i)
                {
                mgr.release(aPacket[i].getByteBuffer());
                }
        
            setPacket(null);
            }
        }
    
    public void respondTo(com.tangosol.coherence.component.net.message.RequestMessage msg)
        {
        // import Component.Net.MemberSet.SingleMemberSet;
        
        // destination (i.e. the source of the request)
        setToMemberSet(SingleMemberSet.instantiate(msg.getFromMember()));
        
        // poll id
        setToPollId(msg.getFromPollId());
        }
    
    // Accessor for the property "BufferController"
    /**
     * Setter for property BufferController.<p>
    * The life cycle controller of the buffers (MultiBufferWriteBuffer,
    * BufferSequence) containing the serialized form of this Message.
    * 
    * For outgoing messages the dispose() method on the controller should be
    * called when the delivery of all packets is confirmed for "datagram"
    * transport and all delivery receipts for "bus" transport have been
    * received (see releaseOutgoing() method).  It is the responsibility of the
    * transport (datagram/bus) to call releaseOutgoing.  The controller is
    * never null for outgoing messages.
    * 
    * For incoming messages: not used as of 12.1.2
     */
    public void setBufferController(com.oracle.coherence.common.base.Disposable disposableController)
        {
        __m_BufferController = disposableController;
        }
    
    // Accessor for the property "BufferController"
    /**
     * Setter for property BufferController.<p>
    * The life cycle controller of the buffers (MultiBufferWriteBuffer,
    * BufferSequence) containing the serialized form of this Message.
    * 
    * For outgoing messages the dispose() method on the controller should be
    * called when the delivery of all packets is confirmed for "datagram"
    * transport and all delivery receipts for "bus" transport have been
    * received (see releaseOutgoing() method).  It is the responsibility of the
    * transport (datagram/bus) to call releaseOutgoing.  The controller is
    * never null for outgoing messages.
    * 
    * For incoming messages: not used as of 12.1.2
     */
    public void setBufferController(com.oracle.coherence.common.base.Disposable controller, int cUsage)
        {
        setBufferController(controller);
        setBufferUsageCounter(cUsage);
        }
    
    // Accessor for the property "BufferUsageCounter"
    /**
     * Setter for property BufferUsageCounter.<p>
    * The count of the number of active users of the buffer(s) managed by the
    * BufferController.  When the count is decremented to zero it is ready for
    * disposal.
    * 
    * Currently this will only be > 1 for outgoing messages.  See
    * MessageHandler.sendMulti
     */
    protected void setBufferUsageCounter(int c)
        {
        __m_BufferUsageCounter = c;
        }
    
    // Accessor for the property "DeserializationRequired"
    /**
     * Setter for property DeserializationRequired.<p>
    * True if the message requires deserialization before it can be processed.
    * 
    * @see PacketReceiver#instantiateMessage
    * @see MessageHandler#onMessage
    * 
    * @functional
     */
    public void setDeserializationRequired(boolean fRequired)
        {
        if (fRequired)
            {
            if (getBufferUsageCounter() == 0)
                {
                setBufferUsageCounter(1);
                }
        
            set_StateAux(get_StateAux() | MASK_DESER_REQ);
            }
        else
            {
            set_StateAux(get_StateAux() & ~MASK_DESER_REQ);
            }
        }
    
    // Accessor for the property "FromMember"
    /**
     * Setter for property FromMember.<p>
    * The Member that is sending (or that did send) the Message. During the
    * discovery phase (and only for Service 0), this could be a temporary
    * Member object with an Id value of zero. Set by the Dispatcher for
    * outgoing Messages.
     */
    public void setFromMember(Member member)
        {
        __m_FromMember = member;
        }
    
    // Accessor for the property "FromMessageId"
    /**
     * Setter for property FromMessageId.<p>
    * The Message number assigned to this Message by the sender of the Message.
    * Set by the PacketPublisher.InQueue for outgoing Messages.
     */
    public void setFromMessageId(long lMsgId)
        {
        __m_FromMessageId = lMsgId;
        }
    
    /**
     * Specify the number of packets that this message is going to be
    * constructed from (if received) or packetized into (if sent).
     */
    public void setMessagePartCount(int cPackets)
        {
        // import Component.Net.Packet.MessagePacket;
        
        if (cPackets < 1 || getPacket() != null )
            {
            throw new IllegalStateException("cPackets: " + cPackets);
            }
        
        setPacket(new MessagePacket[cPackets]);
        setNullPacketCount(cPackets);
        }
    
    // Accessor for the property "MessageType"
    /**
     * Setter for property MessageType.<p>
    * An integer value, unique to the Service that created this Message, that
    * identifies the Message type.
    * 
    * Message types less than 0 are reserved for "internal" notification
    * Messages generated by the Cluster Service (service 0).
     */
    public void setMessageType(int nType)
        {
        __m_MessageType = nType;
        }
    
    // Accessor for the property "NotifyDelivery"
    /**
     * Setter for property NotifyDelivery.<p>
    * Set to true to get a "return receipt" notification when the Message has
    * been delivered (or when Message is determined to be undeliverable).
    * 
    * As of Coherence 3.2, this provides a stronger guarantee than an ack of
    * all the message's packets.  This notification will not be delivered until
    * all living recipients have ack'd all older packets from us.  This ensures
    * that nothing will stop [the living] recipients from processing the
    * message.
    * 
    * This notification generally has a very high latency (compared to a poll),
    * and is meant mostly for cleanup tasks.
    * 
    * @functional
     */
    public void setNotifyDelivery(boolean fNotify)
        {
        if (fNotify)
            {
            set_StateAux(get_StateAux() | MASK_NOTIFY);
            }
        else
            {
            set_StateAux(get_StateAux() & ~MASK_NOTIFY);
            }
        }
    
    // Accessor for the property "NullPacketCount"
    /**
     * Setter for property NullPacketCount.<p>
    * Number of Packets that compose the Message that are null references. When
    * the Message is being assembled (on the receiver), the null Packets are
    * "missing", and when the Message is being acknowledge (on the sender), the
    * null Packets are the checked-off (acknowledged) ones.
     */
    protected void setNullPacketCount(int cPackets)
        {
        __m_NullPacketCount = cPackets;
        }
    
    // Accessor for the property "Packet"
    /**
     * Setter for property Packet.<p>
    * The packets that this message was constructed from (if received) or was
    * deconstructed to (if sent).
    * 
    * Note: The packets are not guaranteed to remain available once the message
    * has been serialized/deserialized.
    * 
    * Note:This property only utilized when the message is sent using the
    * "datagram" transport.
     */
    public void setPacket(com.tangosol.coherence.component.net.packet.MessagePacket[] aPacket)
        {
        __m_Packet = aPacket;
        }
    
    // Accessor for the property "Packet"
    /**
     * Setter for property Packet.<p>
    * The packets that this message was constructed from (if received) or was
    * deconstructed to (if sent).
    * 
    * Note: The packets are not guaranteed to remain available once the message
    * has been serialized/deserialized.
    * 
    * Note:This property only utilized when the message is sent using the
    * "datagram" transport.
     */
    public void setPacket(int i, com.tangosol.coherence.component.net.packet.MessagePacket packet)
        {
        Packet[] aPacket = getPacket();
        if (packet == null)
            {
            if (aPacket[i] != null)
                {
                // one more null packet
                setNullPacketCount(getNullPacketCount() + 1);
                }
            }
        else
            {
            if (aPacket[i] == null)
                {
                // one less null packet
                setNullPacketCount(getNullPacketCount() - 1);
                }
            else
                {
                // potential TODO for debugging purposes is to
                // assert that the packet data is the same
                }
            }
        
        aPacket[i] = packet;
        }
    
    // Accessor for the property "Poll"
    /**
     * Setter for property Poll.<p>
    * The Poll that this Message is a response to, or null if this Message is
    * not a response to a Poll (or the Poll has already been unregistered).
     */
    protected void setPoll(Poll poll)
        {
        __m_Poll = poll;
        }
    
    // Accessor for the property "ReadBuffer"
    /**
     * Setter for property ReadBuffer.<p>
    * For incoming messages, the ReadBuffer is used to deserialize the Message
    * payload. This property is "pre-set" only for incoming messages delivered
    * over any "bus" transport and calculated for the "datagram" transport.
    * 
    * For outgoing messages, the ReadBuffer is used to packetize the message
    * for "datagram" transport in the case when the Message needs to be sent
    * using multiple transports (see PacketPublisher#serializeMessage).
     */
    public void setReadBuffer(com.tangosol.io.ReadBuffer buffer)
        {
        _assert(getReadBuffer() == null);
        
        __m_ReadBuffer = (buffer);
        }
    
    // Accessor for the property "Service"
    /**
     * Setter for property Service.<p>
    * The Service object that created the Message (for outgoing Messages) or
    * the Service object to which the Message is delivered (for incoming
    * Messages). In the case of an outgoing Message, the Service must set this
    * property before queueing the Message to be sent with the Dispatcher.
    * 
    * @see #onInit
     */
    public void setService(com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid service)
        {
        __m_Service = service;
        }
    
    // Accessor for the property "ToMemberSet"
    /**
     * Setter for property ToMemberSet.<p>
    * The Set of Members to which the Message will be sent. May only be empty
    * for Messages being sent from/to the Service whose id is 0. As of
    * Coherence 3.7, it's never assigned for incoming messages.
     */
    public void setToMemberSet(MemberSet setMember)
        {
        __m_ToMemberSet = setMember;
        }
    
    // Accessor for the property "ToMessageId"
    /**
     * Setter for property ToMessageId.<p>
    * Transient property used to order inbound Messages within a Service's
    * queue.
    * 
    * Note: the FromMessageId property cannot be re-used for the purpose due to
    * the case where a client thread sends a message addressed to its local
    * service and other service members.  In this case the Message object would
    * exist in two MultiQueues and each one needs a property to hold the
    * virtual index.
     */
    public void setToMessageId(long lMsgId)
        {
        __m_ToMessageId = lMsgId;
        }
    
    // Accessor for the property "ToPollId"
    /**
     * Setter for property ToPollId.<p>
    * The PollId that this Message is a response to. Configured by the
    * respondTo method.
     */
    public void setToPollId(long lPollId)
        {
        __m_ToPollId = lPollId;
        }
    
    // Accessor for the property "TracingSpanContext"
    /**
     * Setter for property TracingSpanContext.<p>
    * The OpenTracing SpanContext.
    * 
    * @since 14.4.1.0
     */
    public void setTracingSpanContext(com.tangosol.internal.tracing.SpanContext contextSpan)
        {
        __m_TracingSpanContext = contextSpan;
        }
    
    // Declared at the super level
    public String toString()
        {
        return toString(true);
        }
    
    /**
     * Output the message as a string.
    * 
    * @param fVerbose include verbose packet output
    * 
    * @return the message.
     */
    public String toString(boolean fVerbose)
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid;
        // import com.tangosol.util.Base;
        
        int cParts   = getMessagePartCount();
        int cPending = isDeserializationRequired()
                ? getNullPacketCount()
                : cParts - getNullPacketCount();
        
        StringBuilder sb = new StringBuilder();
        sb.append("Message \"")
          .append(get_Name())
          .append("\"\n  {")
          .append("\n  FromMember=")
          .append(getFromMember())
          .append("\n  FromMessageId=")
          .append(getFromMessageId())
          .append("\n  MessagePartCount=")
          .append(cParts)
          .append("\n  PendingCount=")
          .append(cPending);
        
        // this output is only for outgoing messages
        int cBuffers = getBufferUsageCounter();
        if (cBuffers == 0)
            {
            sb.append("\n  Delivered");
            }
        else if (getBufferController() != null) 
            {
            sb.append("\n  BufferCounter=")
              .append(cBuffers);
            }
        
        sb.append("\n  MessageType=")
          .append(getMessageType())
          .append("\n  ToPollId=")
          .append(getToPollId())
          .append("\n  Poll=")
          .append(getPoll());
        
        if (fVerbose)
            {
            // Packets
            sb.append("\n  Packets")
              .append("\n    {");
        
            int cDigts = cParts > 1000 ? 4 : 3;
            for (int i = 0; i < cParts; ++i)
                {
                Packet packet = getPacket(i);
                if (packet != null)
                    {
                    sb.append("\n    [")
                      .append(Base.toDecString(i, cDigts))
                      .append("]=")
                      .append(Base.indentString(packet.toString(),
                            "          ", false));
                    }
                }
            sb.append("\n    }");
            }
        
        sb.append("\n  Service=");
        com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid service = getService();
        if (service == null)
            {
            sb.append("null");
            }
        else
            {
            sb.append(Base.indentString(service.toString(), "    ", false));
            }
        
        sb.append("\n  ToMemberSet=");
        MemberSet setMember = getToMemberSet();
        if (setMember == null)
            {
            sb.append("null");
            }
        else
            {
            sb.append(Base.indentString(setMember.toString(), "    ", false));
            }
        
        sb.append("\n  NotifyDelivery=")
          .append(isNotifyDelivery());
        
        String sDesc = getDescription();
        if (sDesc != null && sDesc.length() > 0)
            {
            sb.append('\n')
              .append(Base.indentString(sDesc, "  "));
            }
        
        sb.append("\n  }");
        return sb.toString();
        }
    
    public void write(com.tangosol.io.WriteBuffer.BufferOutput output)
            throws java.io.IOException
        {
        }
    
    public void writeInternal(com.tangosol.io.WriteBuffer.BufferOutput output)
            throws java.io.IOException
        {
        long    lPollId   = getToPollId();
        boolean fResponse = lPollId != 0L;
        output.writeBoolean(fResponse);
        if (fResponse)
            {
            // three-byte Poll id
            Packet.writeTrint(output, lPollId);
            }
        }
    
    /**
     * Write the specified object to the specified BufferOutput.
    * 
    * @param output the BufferOutput to serialize the object into
    * @param o  the object to serialize
    * 
    * @see #readObject
     */
    public void writeObject(com.tangosol.io.WriteBuffer.BufferOutput output, Object o)
            throws java.io.IOException
        {
        getService().writeObject(output, o);
        }
    
    /**
     * This should be called by Message serialization methods in order to
    * propgate tracing information.
    * 
    * @since 14.1.1.0
     */
    protected void writeTracing(com.tangosol.io.WriteBuffer.BufferOutput output)
            throws java.io.IOException
        {
        // import com.tangosol.internal.tracing.TracingHelper;
        // import com.tangosol.internal.tracing.SpanContext;
        // import com.tangosol.util.ExternalizableHelper;
        // import java.util.Map;
        
        SpanContext ctxSpan    = getTracingSpanContext();
        Map         mapSpanCtx = null;
        
        if (!TracingHelper.isNoop(ctxSpan))
            {
            mapSpanCtx = TracingHelper.getTracer().inject(ctxSpan);
            }
        ExternalizableHelper.writeObject(output, mapSpanCtx);
        }
    }
