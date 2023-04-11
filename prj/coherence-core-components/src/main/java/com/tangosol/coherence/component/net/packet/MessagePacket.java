
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.packet.MessagePacket

package com.tangosol.coherence.component.net.packet;

import com.tangosol.coherence.component.net.MemberSet;
import com.tangosol.coherence.component.net.memberSet.actualMemberSet.serviceMemberSet.MasterMemberSet;
import com.tangosol.io.ReadBuffer;
import com.tangosol.util.Base;
import java.io.IOException;
import java.sql.Time;

/**
 * A Packet is a unit of data transmission between the Members of the network.
 * There are five different type of Packets: Broadcast, Directed, Sequel,
 * Request and Ack.
 * 
 * A Message Packet represents a whole or a part of a Message. A Message could
 * be sent to a number of recipient Members. There are Broadcast and "point"
 * Message Packets depending on the intended recipients of the Message.
 * 
 * If the Message is being sent to all Members or potential Members, it uses a
 * Broadcast Packet, which is unaddressed (aka no "to" Member id information,
 * and it may not even have a return address, aka a "from" Member id, if the
 * sender has not been assigned a Member id by the cluster). A Message the is
 * formatted into a Broadcast Packet must fit entirely in one Packet.
 * 
 * There are also "point" Message Packets, which come from this Member and go
 * to one or more addressed Members (recipients). For each recipient, there is
 * a sequential counter which is unique in the scope of the sender/recipient
 * pair that a Message is marked with and for each sender there is a global
 * sequential counter that an outgoing Message is marked with. These counters
 * are used to quickly identify incoming point Message Packets, acknowledge
 * them, and determine if any prerequisite point Packets are missing.
 * 
 * There are two types of "point" Message Packets: Directed and Sequel. Each
 * "point" Message is formatted into one Directed Packet and zero or more
 * Sequel Packets. The Directed Packet carries the Message-describing
 * information and the first chunk of Message data, and the Sequel Packets
 * carry any additional chunks of Message data that did not fit into the
 * Directed Packet.
 * 
 * A recipient of "point" Message Packets is responsible for acknowledging the
 * receipt of those Packets. The Ack Packet is sent back to the sender to
 * acknowledge one or more "point" Message Packets.
 * 
 * If a recipient determines that it missed a "point" Message Packet, it can
 * send a Request Packet to tell the sender of the "point" Message Packet that
 * the "point" Message Packet was never received and is being waited upon by
 * the recipient.
 * 
 * The Ack and Request Packets are referred to as Notify Packets because they
 * are used by one Member to Notify another Member of Packet communication
 * success and failure.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public abstract class MessagePacket
        extends    com.tangosol.coherence.component.net.Packet
        implements com.tangosol.net.internal.PacketIdentifier,
                   Cloneable
    {
    // ---- Fields declarations ----
    
    /**
     * Property BodyLength
     *
     * The length in bytes of this Packet's body.
     */
    private int __m_BodyLength;
    
    /**
     * Property ByteBuffer
     *
     * Assigned only on incomig MessagePackets. The ByteBuffer retains one MTU
     * size worth of a Messages body. The ByteBuffer is released back to the
     * BufferManager which it was acquired from when the complete Message has
     * been received.
     */
    private transient java.nio.ByteBuffer __m_ByteBuffer;
    
    /**
     * Property DELIVERY_CONFIRMED
     *
     * Indicates that all recipients of the packet have acknowledged it.
     */
    public static final int DELIVERY_CONFIRMED = 4;
    
    /**
     * Property DELIVERY_DEFERRED
     *
     * Indicates that a packet will be delayed in being sent.
     */
    public static final int DELIVERY_DEFERRED = 2;
    
    /**
     * Property DELIVERY_LOST
     *
     * Indicates that the packet was already sent but was not confirmed before
     * reaching its scheduled resend time.
     */
    public static final int DELIVERY_LOST = 3;
    
    /**
     * Property DELIVERY_OUTSTANDING
     *
     * Indicates that a packet has been sent but not yet confirmed or lost.
     */
    public static final int DELIVERY_OUTSTANDING = 1;
    
    /**
     * Property DELIVERY_UNSENT
     *
     * The initial delivery state of a packet upon creation. 
     */
    public static final int DELIVERY_UNSENT = 0;
    
    /**
     * Property DeliveryState
     *
     * The delivery state of the packet, only applicable to outgoing packets. 
     * Only maintained when flow control is enabled.
     */
    private transient int __m_DeliveryState;
    
    /**
     * Property FromMessageId
     *
     * A Directed or Sequel Packet represents a whole or a part of a Message.
     * Each sender maintains a global sequential number that every outgoing
     * Message is marked with (except for Broadcast Messages). This property
     * represents the sender specific Message id for this packet or zero if the
     * Message id is not applicable. Prior to 3.2.2 the receiver only held the
     * trint representation for this value.
     */
    private long __m_FromMessageId;
    
    /**
     * Property MessagePartCount
     *
     * Specifies the number of Packet components that compose the Message to
     * which this Packet belongs.
     * Broadcast:  1 (Broadcast does not support Sequel Packets)
     * Directed:  1 or greater (the first will be a Directed Packet, all others
     * will be Sequel Packets)
     * Sequel:  Always more than one (otherwise no need for a Sequel Packet)
     * 
     * Note that incoming Sequel cannot determine this property until it is
     * part of a Message (i.e. until Message property is set)
     */
    private int __m_MessagePartCount;
    
    /**
     * Property MessagePartIndex
     *
     * Specifies an zero-based index of this Packet within the multi-Packet
     * Message. The value is only applicable (i.e. non-zero) for Sequel
     * Packets.
     */
    private int __m_MessagePartIndex;
    
    /**
     * Property MessageType
     *
     * Specifies the type of the Message that will be constructed from this
     * Packet. Only Directed (and thus Sequel) and Broadcast Packets form
     * Message objects.
     */
    private int __m_MessageType;
    
    /**
     * Property NackInProgress
     *
     * Indicates that the packet has been Nackd and requires immediate resend. 
     * Once the packet comes off the head of the resend queue this property is
     * cleared.
     * 
     * May only be accessed while synchronized on the resend queue.
     */
    private transient boolean __m_NackInProgress;
    
    /**
     * Property PendingResendSkips
     *
     * The number of times the packet needs to be skipped from processing by
     * the resend queue.
     * 
     * This property is reserved for use by the Publisher thread.
     */
    private transient int __m_PendingResendSkips;
    
    /**
     * Property ReadBuffer
     *
     * Only assigned on outgoig MessagePackets by defineBufferView. The
     * ReadBuffer is a view into the region this MessagePacket corresponds to. 
     */
    private com.tangosol.io.ReadBuffer __m_ReadBuffer;
    
    /**
     * Property ResendScheduled
     *
     * This property is reserved for use by the PacketPublisher. The
     * ResendScheduled value is the date/time (in millis) at which the Packet
     * (ConfirmationRequired=true) will be resent if a confirmation for the
     * Packet has not been received.
     */
    private long __m_ResendScheduled;
    
    /**
     * Property ResendTimeout
     *
     * This property is reserved for use by the PacketPublisher. The
     * ResendTimeout value is the date/time (in millis) at which the Packet
     * (ConfirmationRequired=true) will stop being resent even if a
     * confirmation for the Packet has not been received, and the Members that
     * have not acknowledged the Packet will be assumed to be dead.
     */
    private long __m_ResendTimeout;
    
    /**
     * Property ServiceId
     *
     * Specifies the Service to which the assembled Message will go (or from
     * which the Message that was disassembled into this Packet came).
     */
    private int __m_ServiceId;
    
    /**
     * Property ToMemberSet
     *
     * Used for outgoing Packets only. Set of Members still to deliver to. Use
     * of ToMemberSet and ToId properties are either/or (exclusive).
     * 
     * Presence of a non-null value is used to identify that the packet started
     * as a Multipoint packet (see isOutgoingMultipoint). Therefore this
     * property should never be nulled out, but cleared instead.
     */
    private com.tangosol.coherence.component.net.memberSet.DependentMemberSet __m_ToMemberSet;
    
    /**
     * Property TYPE_NO_DESTINATION
     *
     * There is no remaining members which to send this packet.
     */
    public static final int TYPE_NO_DESTINATION = 0;
    
    // Initializing constructor
    public MessagePacket(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            clz = Class.forName("com.tangosol.coherence/component/net/packet/MessagePacket".replace('/', '.'));
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
     * Similar to _assert(), but IOException is thrown.
     */
    public static void assertIO(boolean fCondition)
            throws java.io.IOException
        {
        // import java.io.IOException;
        
        if (!fCondition)
            {
            throw new IOException();
            }
        }
    
    /**
     * @see Directed#selectType
    * @see Sequel#selectType
     */
    public static int calcBodyLength(int cbHeader, int cbPref, int cbMax)
        {
        return Math.min(Math.max(cbHeader << 2, cbPref), // desired size
                        Math.max(cbPref, cbMax)) // the largest packet we can exchange
               - cbHeader;
        }
    
    /**
     * Compute the maximum number of members (member ids) that can be encoded in
    * a packet of a given size. This is a pessimistic calculation and
    * intentionally includes recycled members within the limit.
    * 
    * @see calcBodyLength
    * @see Directed#selectType
    * @see Sequel#selectType
     */
    public static int calcMaxMembers(int cb)
        {
        // import Component.Net.MemberSet.ActualMemberSet.ServiceMemberSet.MasterMemberSet;
        
        // the limit is based on sending a DIRECTED_MANY packet
        // starts with a fix cost
        // followed a MANY encoded memberset
        //     encoded in 4 byte increments each representing up to 32 members
        // followed by N trints (3 bytes each) (one for each member in the memberset)
        
        cb -= 21; // fixed cost of DIRECTED_MANY packet
        
        // computing the number of members which can be encoded is done in two stages:
        
        // stage 1: compute the number of members which can be encoded using fully
        // populated 4B words, and the corresponding trints.  These are blocks of 100B.
        
        int cMembers = (cb / 100) * 32;
        
        // stage 2: add in the number of members which can be used with the remainder
        
        cb = (cb % 100);
        if (cb >= 7)
            {
            // there is room for another word and some trints
            cMembers += (cb - 4) / 3;
            }
        
        return Math.min(MasterMemberSet.MAX_MEMBERS, cMembers);
        }
    
    /**
     * Remove all recipients for this packet.
     */
    public void clearRecipients()
        {
        // import com.tangosol.util.Base;
        
        if (isOutgoingMultipoint())
            {
            getToMemberSet().clear();
            }
        else
            {
            setToId(0);
            }
            
        setSentMillis(Base.getSafeTimeMillis());
        }
    
    // Declared at the super level
    /**
     * Is used to report undeliverable Packets.
     */
    public Object clone()
        {
        // import com.tangosol.util.Base;
        // import com.tangosol.io.ReadBuffer;
        
        // since clone is used exclusively for outgoing MessagePackets,
        // ByteBuffer should be null as it is only used by incoming MessagePackets
        _assert(getByteBuffer() == null);
        
        MessagePacket packet;
        try
            {
            packet = (MessagePacket) super.clone();
            }
        catch (CloneNotSupportedException e)
            {
            throw Base.ensureRuntimeException(e);
            }
        
        ReadBuffer readBuffer = getReadBuffer();
        
        if (readBuffer != null)
            {
            packet.setReadBuffer((ReadBuffer) readBuffer.clone());
            }
        
        return packet;
        }
    
    /**
     * Defines the buffer and the region where the body of this packet resides.
    * 
    * @param buffer  the ReadBuffer which defines this packet
    * @param of  the offset within the ReadBuffer where this packet begins
    * @param cb  the length of this packet
     */
    public void defineBufferView(com.tangosol.io.ReadBuffer buffer, int of, int cb)
        {
        setBodyLength(cb);
        setReadBuffer(buffer.getReadBuffer(of, cb));
        }
    
    // Declared at the super level
    public boolean equals(Object obj)
        {
        // two Packets are considered equal if they have the same from
        // Member id, from Message id, and MessagePartIndex
        if (obj instanceof MessagePacket)
            {
            MessagePacket that = (MessagePacket) obj;
            return this.getFromId()           == that.getFromId()
                && this.getFromMessageId()    == that.getFromMessageId()
                && this.getMessagePartIndex() == that.getMessagePartIndex();
            }
        return false;
        }
    
    public String formatDeliveryState(int nDeliveryState)
        {
        switch (nDeliveryState)
            {
            case DELIVERY_UNSENT:
                return "unsent";
            case DELIVERY_OUTSTANDING:
                return "outstanding";
            case DELIVERY_DEFERRED :
                return "deferred";
            case DELIVERY_LOST :
                return "lost";
            case DELIVERY_CONFIRMED :
                return "confirmed";
            default:
                return "<unknown>";
            }
        }
    
    // Accessor for the property "BodyLength"
    /**
     * Getter for property BodyLength.<p>
    * The length in bytes of this Packet's body.
     */
    public int getBodyLength()
        {
        return __m_BodyLength;
        }
    
    // Accessor for the property "ByteBuffer"
    /**
     * Getter for property ByteBuffer.<p>
    * Assigned only on incomig MessagePackets. The ByteBuffer retains one MTU
    * size worth of a Messages body. The ByteBuffer is released back to the
    * BufferManager which it was acquired from when the complete Message has
    * been received.
     */
    public java.nio.ByteBuffer getByteBuffer()
        {
        return __m_ByteBuffer;
        }
    
    // Accessor for the property "DeliveryState"
    /**
     * Getter for property DeliveryState.<p>
    * The delivery state of the packet, only applicable to outgoing packets. 
    * Only maintained when flow control is enabled.
     */
    public int getDeliveryState()
        {
        return __m_DeliveryState;
        }
    
    // From interface: com.tangosol.net.internal.PacketIdentifier
    // Accessor for the property "FromMessageId"
    /**
     * Getter for property FromMessageId.<p>
    * A Directed or Sequel Packet represents a whole or a part of a Message.
    * Each sender maintains a global sequential number that every outgoing
    * Message is marked with (except for Broadcast Messages). This property
    * represents the sender specific Message id for this packet or zero if the
    * Message id is not applicable. Prior to 3.2.2 the receiver only held the
    * trint representation for this value.
     */
    public long getFromMessageId()
        {
        return __m_FromMessageId;
        }
    
    // Accessor for the property "HeaderLength"
    /**
     * Getter for property HeaderLength.<p>
    * The length of the packet's header in bytes.
     */
    public int getHeaderLength()
        {
        return 0;
        }
    
    // Declared at the super level
    /**
     * Getter for property Length.<p>
    * The maximum size of the packet in serialized form. Used to determine
    * bundling of individual packets into the same PacketBundle. 
    * 
    * The number of actual bytes may be less than this as member are departing
    * the cluster. 
     */
    public int getLength()
        {
        return getHeaderLength() + getBodyLength();
        }
    
    // Accessor for the property "MessagePartCount"
    /**
     * Getter for property MessagePartCount.<p>
    * Specifies the number of Packet components that compose the Message to
    * which this Packet belongs.
    * Broadcast:  1 (Broadcast does not support Sequel Packets)
    * Directed:  1 or greater (the first will be a Directed Packet, all others
    * will be Sequel Packets)
    * Sequel:  Always more than one (otherwise no need for a Sequel Packet)
    * 
    * Note that incoming Sequel cannot determine this property until it is part
    * of a Message (i.e. until Message property is set)
     */
    public int getMessagePartCount()
        {
        return __m_MessagePartCount;
        }
    
    // From interface: com.tangosol.net.internal.PacketIdentifier
    // Accessor for the property "MessagePartIndex"
    /**
     * Getter for property MessagePartIndex.<p>
    * Specifies an zero-based index of this Packet within the multi-Packet
    * Message. The value is only applicable (i.e. non-zero) for Sequel Packets.
     */
    public int getMessagePartIndex()
        {
        return __m_MessagePartIndex;
        }
    
    // Accessor for the property "MessageType"
    /**
     * Getter for property MessageType.<p>
    * Specifies the type of the Message that will be constructed from this
    * Packet. Only Directed (and thus Sequel) and Broadcast Packets form
    * Message objects.
     */
    public int getMessageType()
        {
        return __m_MessageType;
        }
    
    // Accessor for the property "PendingResendSkips"
    /**
     * Getter for property PendingResendSkips.<p>
    * The number of times the packet needs to be skipped from processing by the
    * resend queue.
    * 
    * This property is reserved for use by the Publisher thread.
     */
    public int getPendingResendSkips()
        {
        return __m_PendingResendSkips;
        }
    
    // Accessor for the property "ReadBuffer"
    /**
     * Get the ReadBuffer for this Packet. The ReadBuffer will represent the
    * region within the WriteBuffer from which the content of this Packet can
    * be read.
     */
    public com.tangosol.io.ReadBuffer getReadBuffer()
        {
        return __m_ReadBuffer;
        }
    
    // Accessor for the property "ResendScheduled"
    /**
     * Getter for property ResendScheduled.<p>
    * This property is reserved for use by the PacketPublisher. The
    * ResendScheduled value is the date/time (in millis) at which the Packet
    * (ConfirmationRequired=true) will be resent if a confirmation for the
    * Packet has not been received.
     */
    public long getResendScheduled()
        {
        return __m_ResendScheduled;
        }
    
    // Accessor for the property "ResendTimeout"
    /**
     * Getter for property ResendTimeout.<p>
    * This property is reserved for use by the PacketPublisher. The
    * ResendTimeout value is the date/time (in millis) at which the Packet
    * (ConfirmationRequired=true) will stop being resent even if a confirmation
    * for the Packet has not been received, and the Members that have not
    * acknowledged the Packet will be assumed to be dead.
     */
    public long getResendTimeout()
        {
        return __m_ResendTimeout;
        }
    
    // Accessor for the property "ServiceId"
    /**
     * Getter for property ServiceId.<p>
    * Specifies the Service to which the assembled Message will go (or from
    * which the Message that was disassembled into this Packet came).
     */
    public int getServiceId()
        {
        return __m_ServiceId;
        }
    
    // Accessor for the property "ToMemberSet"
    /**
     * Getter for property ToMemberSet.<p>
    * Used for outgoing Packets only. Set of Members still to deliver to. Use
    * of ToMemberSet and ToId properties are either/or (exclusive).
    * 
    * Presence of a non-null value is used to identify that the packet started
    * as a Multipoint packet (see isOutgoingMultipoint). Therefore this
    * property should never be nulled out, but cleared instead.
     */
    public com.tangosol.coherence.component.net.memberSet.DependentMemberSet getToMemberSet()
        {
        return __m_ToMemberSet;
        }
    
    // Declared at the super level
    public int hashCode()
        {
        return getFromId()
             ^ ((int) getFromMessageId())
             ^ getMessagePartIndex();
        }
    
    // Declared at the super level
    /**
     * Check if the packet is still addressed to the specified member Id.  Once
    * the packet has been ack'd by a member this will return false.
     */
    public boolean isAddressedTo(int nMemberId)
        {
        // import Component.Net.MemberSet;
        
        if (super.isAddressedTo(nMemberId))
            {
            return true;
            }
        
        MemberSet memberSet = getToMemberSet();
        return memberSet != null && memberSet.contains(nMemberId);
        }
    
    // Accessor for the property "NackInProgress"
    /**
     * Getter for property NackInProgress.<p>
    * Indicates that the packet has been Nackd and requires immediate resend. 
    * Once the packet comes off the head of the resend queue this property is
    * cleared.
    * 
    * May only be accessed while synchronized on the resend queue.
     */
    public boolean isNackInProgress()
        {
        return __m_NackInProgress;
        }
    
    // Declared at the super level
    /**
     * Getter for property OutgoingMultipoint.<p>
    * True if the Packet may have multiple Members to which it is addressed.
    * (Note: False for Broadcast, which is not addressed.)
    * 
    * This property is only true for Message Packets that have a ToMemberSet.
    * 
    * Should be used only if Outgoing is set.
     */
    public boolean isOutgoingMultipoint()
        {
        return getToMemberSet() != null;
        }
    
    // Accessor for the property "ResendNecessary"
    /**
     * Getter for property ResendNecessary.<p>
    * ResendNecessary evaluates to true until the MessagePacket has been
    * acknowledged by all of the recipients (or those recipients have left the
    * cluster).
     */
    public boolean isResendNecessary()
        {
        // import Component.Net.MemberSet;
        
        if (getToId() != 0)
            {
            return true;
            }
        
        MemberSet set = getToMemberSet();
        return set != null && !set.isEmpty();
        }
    
    public boolean registerAck(com.tangosol.coherence.component.net.Member memberFrom)
        {
        // import Component.Net.MemberSet;
        // import java.sql.Time;
        
        if (!isOutgoing())
            {
            // debugging to help track down cause of COH-13095
            long lSentMillis = getSentMillis();
            long lRecvMillis = getReceivedMillis();
            _trace("Received ACK from " + memberFrom + " for incomming packet, with sent count of  " + getSentCount()
                + ", last send timestamp of " + lSentMillis + "(" + new Time(lSentMillis) + "), recv timestamp of "
                + lRecvMillis + "(" + new Time(lRecvMillis) + "):  " + this, 1);
            _assert(isOutgoing()); // original assert
            }
        
        MemberSet set = getToMemberSet();
        if (set == null)
            {
            int nToId   = getToId();
            int nFromId = memberFrom.getId();
            if (nToId != 0)
                {
                if (nFromId != nToId)
                    {
                    // debugging to help track down cause of COH-13095
                    long lSentMillis = getSentMillis();
                    long lRecvMillis = getReceivedMillis();
                    _trace("Received ACK from " + memberFrom + " " + nFromId + " for packet sent to " + nToId + " , with sent count of  " + getSentCount()
                        + ", last send timestamp of " + lSentMillis + "(" + new Time(lSentMillis) + "), recv timestamp of "
                        + lRecvMillis + "(" + new Time(lRecvMillis) + "):  " + this, 1);
                    
                    _assert(nFromId == nToId); // original assert
                    }
                setToId(0);
                return true;
                }
            return false;
            }
        else
            {
            return set.remove(memberFrom);
            }
        }
    
    // Accessor for the property "BodyLength"
    /**
     * Setter for property BodyLength.<p>
    * The length in bytes of this Packet's body.
     */
    protected void setBodyLength(int cbBody)
        {
        __m_BodyLength = cbBody;
        }
    
    // Accessor for the property "ByteBuffer"
    /**
     * Setter for property ByteBuffer.<p>
    * Assigned only on incomig MessagePackets. The ByteBuffer retains one MTU
    * size worth of a Messages body. The ByteBuffer is released back to the
    * BufferManager which it was acquired from when the complete Message has
    * been received.
     */
    public void setByteBuffer(java.nio.ByteBuffer buffer)
        {
        __m_ByteBuffer = buffer;
        }
    
    // Accessor for the property "DeliveryState"
    /**
     * Setter for property DeliveryState.<p>
    * The delivery state of the packet, only applicable to outgoing packets. 
    * Only maintained when flow control is enabled.
     */
    protected void setDeliveryState(int nState)
        {
        __m_DeliveryState = nState;
        }
    
    // Accessor for the property "DeliveryState"
    /**
     * Helper method for setting the delivery state of a packet, for the
    * specified member, only called on the Publisher thread.
     */
    public void setDeliveryState(int nNewState, com.tangosol.coherence.component.net.Member member)
        {
        // import Component.Net.Member$FlowControl as com.tangosol.coherence.component.net.Member.FlowControl;
        
        int nCurrentState = getDeliveryState();
        
        if (nNewState == nCurrentState)
            {
            return;
            }
        
        com.tangosol.coherence.component.net.Member.FlowControl flowControl = member.getFlowControl();
        if (flowControl != null)
            {
            switch (nCurrentState)
                {
                case DELIVERY_OUTSTANDING:
                    flowControl.setOutstandingPacketCount(flowControl.getOutstandingPacketCount() - 1);
                    break;
                case DELIVERY_DEFERRED:
                    flowControl.setDeferredPacketCount(flowControl.getDeferredPacketCount() - 1);
                    break;
                }
        
            switch (nNewState)
                {
                case DELIVERY_OUTSTANDING:
                    flowControl.setOutstandingPacketCount(flowControl.getOutstandingPacketCount() + 1);
                    break;
                case DELIVERY_DEFERRED:
                    flowControl.setDeferredPacketCount(flowControl.getDeferredPacketCount() + 1);
                    break;
                }
            }
        
        setDeliveryState(nNewState);
        }
    
    // Accessor for the property "FromMessageId"
    /**
     * Setter for property FromMessageId.<p>
    * A Directed or Sequel Packet represents a whole or a part of a Message.
    * Each sender maintains a global sequential number that every outgoing
    * Message is marked with (except for Broadcast Messages). This property
    * represents the sender specific Message id for this packet or zero if the
    * Message id is not applicable. Prior to 3.2.2 the receiver only held the
    * trint representation for this value.
     */
    public void setFromMessageId(long nId)
        {
        __m_FromMessageId = nId;
        }
    
    // Accessor for the property "MessagePartCount"
    /**
     * Setter for property MessagePartCount.<p>
    * Specifies the number of Packet components that compose the Message to
    * which this Packet belongs.
    * Broadcast:  1 (Broadcast does not support Sequel Packets)
    * Directed:  1 or greater (the first will be a Directed Packet, all others
    * will be Sequel Packets)
    * Sequel:  Always more than one (otherwise no need for a Sequel Packet)
    * 
    * Note that incoming Sequel cannot determine this property until it is part
    * of a Message (i.e. until Message property is set)
     */
    public void setMessagePartCount(int cParts)
        {
        __m_MessagePartCount = cParts;
        }
    
    // Accessor for the property "MessagePartIndex"
    /**
     * Setter for property MessagePartIndex.<p>
    * Specifies an zero-based index of this Packet within the multi-Packet
    * Message. The value is only applicable (i.e. non-zero) for Sequel Packets.
     */
    public void setMessagePartIndex(int i)
        {
        __m_MessagePartIndex = i;
        }
    
    // Accessor for the property "MessageType"
    /**
     * Setter for property MessageType.<p>
    * Specifies the type of the Message that will be constructed from this
    * Packet. Only Directed (and thus Sequel) and Broadcast Packets form
    * Message objects.
     */
    public void setMessageType(int nType)
        {
        __m_MessageType = nType;
        }
    
    // Accessor for the property "NackInProgress"
    /**
     * Setter for property NackInProgress.<p>
    * Indicates that the packet has been Nackd and requires immediate resend. 
    * Once the packet comes off the head of the resend queue this property is
    * cleared.
    * 
    * May only be accessed while synchronized on the resend queue.
     */
    public void setNackInProgress(boolean fNack)
        {
        __m_NackInProgress = fNack;
        }
    
    // Accessor for the property "PendingResendSkips"
    /**
     * Setter for property PendingResendSkips.<p>
    * The number of times the packet needs to be skipped from processing by the
    * resend queue.
    * 
    * This property is reserved for use by the Publisher thread.
     */
    public void setPendingResendSkips(int pPendingResendSkips)
        {
        __m_PendingResendSkips = pPendingResendSkips;
        }
    
    // Accessor for the property "ReadBuffer"
    /**
     * Setter for property ReadBuffer.<p>
    * Only assigned on outgoig MessagePackets by defineBufferView. The
    * ReadBuffer is a view into the region this MessagePacket corresponds to. 
     */
    protected void setReadBuffer(com.tangosol.io.ReadBuffer buffer)
        {
        __m_ReadBuffer = buffer;
        }
    
    // Accessor for the property "ResendScheduled"
    /**
     * Setter for property ResendScheduled.<p>
    * This property is reserved for use by the PacketPublisher. The
    * ResendScheduled value is the date/time (in millis) at which the Packet
    * (ConfirmationRequired=true) will be resent if a confirmation for the
    * Packet has not been received.
     */
    public void setResendScheduled(long cMillis)
        {
        __m_ResendScheduled = cMillis;
        }
    
    // Accessor for the property "ResendTimeout"
    /**
     * Setter for property ResendTimeout.<p>
    * This property is reserved for use by the PacketPublisher. The
    * ResendTimeout value is the date/time (in millis) at which the Packet
    * (ConfirmationRequired=true) will stop being resent even if a confirmation
    * for the Packet has not been received, and the Members that have not
    * acknowledged the Packet will be assumed to be dead.
     */
    public void setResendTimeout(long pResendTimeout)
        {
        __m_ResendTimeout = pResendTimeout;
        }
    
    // Accessor for the property "ServiceId"
    /**
     * Setter for property ServiceId.<p>
    * Specifies the Service to which the assembled Message will go (or from
    * which the Message that was disassembled into this Packet came).
     */
    public void setServiceId(int nId)
        {
        __m_ServiceId = nId;
        }
    
    // Accessor for the property "ToMemberSet"
    /**
     * Setter for property ToMemberSet.<p>
    * Used for outgoing Packets only. Set of Members still to deliver to. Use
    * of ToMemberSet and ToId properties are either/or (exclusive).
    * 
    * Presence of a non-null value is used to identify that the packet started
    * as a Multipoint packet (see isOutgoingMultipoint). Therefore this
    * property should never be nulled out, but cleared instead.
     */
    public void setToMemberSet(com.tangosol.coherence.component.net.memberSet.DependentMemberSet setMember)
        {
        __m_ToMemberSet = setMember;
        }
    }
