
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.packet.notifyPacket.Ack

package com.tangosol.coherence.component.net.packet.notifyPacket;

import com.tangosol.coherence.component.net.packet.NotifyPacket;
import com.tangosol.net.internal.SimplePacketIdentifier;

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
public class Ack
        extends    com.tangosol.coherence.component.net.packet.NotifyPacket
    {
    // ---- Fields declarations ----
    
    /**
     * Property ContiguousFromMessageId
     *
     * The last (based on numerical comparision) contiguous message ID (in
     * trint form) to have been received.  This indicates that all messages
     * with lower message IDs have been fully received.  This information is
     * used by the receipient of the Ack to fill in wholes in the ack stream,
     * caused by missing Acks.
     * 
     * @since 3.2
     */
    private int __m_ContiguousFromMessageId;
    
    /**
     * Property ContiguousFromMessagePartIndex
     *
     * The message part index corresponding to ContiguousFromMessageId.
     * 
     * @see ContiguousFromMessageId
     * @since 3.2
     */
    private int __m_ContiguousFromMessagePartIndex;
    
    /**
     * Property Flushed
     *
     * Indicates whether or not the Ack has been flushed.
     * 
     * @since 3.2
     */
    private transient boolean __m_Flushed;
    
    /**
     * Property LENGTH_FIXED
     *
     * Fixed size (in bytes) of the Ack header.
     */
    public static final int LENGTH_FIXED = 30;
    
    /**
     * Property LENGTH_VARIABLE
     *
     * Variable size (in bytes) of the Ack for each message ID and part that is
     * ack'd.
     */
    public static final int LENGTH_VARIABLE = 6;
    
    /**
     * Property NewestFromMessageId
     *
     * From the perspective of the sender of the Ack.  This is the newest
     * message ID (int trint form) to be known to have been sent by recipient
     * of the Ack.
     * 
     * Note: this does not mean that the identified packet was received, the ID
     * may be known do to the other side having sent a MaxOutgoingId higher
     * then any received ID.
     * 
     * @since 3.2
     */
    private int __m_NewestFromMessageId;
    
    /**
     * Property NewestFromMessagePartIndex
     *
     * From the perspective of the sender of the Ack.  This is the greatest
     * message part index from the numerically greatest message ID to be known
     * to have been sent by recipient of the Ack.
     * 
     * @see MaxFromMessageId
     * @since 3.2
     */
    private int __m_NewestFromMessagePartIndex;
    
    /**
     * Property NewestToMessageId
     *
     * From the perspective of the sender of the Ack.  This is the newest
     * message ID (int trint form) to have been sent to the recipient of the
     * Ack from this member.  A value of zero indicates that no ackable packets
     * have previously been sent.
     * 
     * @since 3.2
     */
    private int __m_NewestToMessageId;
    
    /**
     * Property NewestToMessagePartIndex
     *
     * From the perspective of the sender of the Ack.  This is the greatest
     * message part index from the numerically greatest message ID to have been
     * sent to the recipient of the Ack.
     * 
     * @see MaxToMessageId
     * @since 3.2
     */
    private int __m_NewestToMessagePartIndex;
    
    /**
     * Property PreferredAckSize
     *
     * The size of the Ack (in Ack'd packets) which the sending node preferrs
     * to receive.  This is used so that the sending node can cause the
     * receiving node to Ack before the Ack packet is full, which is usefull
     * when the flowcontrol threshold falls below the size of an Ack.
     */
    private int __m_PreferredAckSize;
    
    // Default constructor
    public Ack()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public Ack(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            setPacketType(232718545);
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
        return new com.tangosol.coherence.component.net.packet.notifyPacket.Ack();
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
            clz = Class.forName("com.tangosol.coherence/component/net/packet/notifyPacket/Ack".replace('/', '.'));
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
    
    // Declared at the super level
    /**
     * Add the passed Packet information to the notification Packet.  Ack
    * synchronizes this method as an Ack may be turned into a UdpPacket by the
    * Publisher while the Receiver is attempting to add to it.
    * 
    * @param nMsgId  the Message id to add to this notification Packet
    * @param nMsgPart  the Message part number to add to this notification
    * Packet, or zero if n/a
    * @return true if the packet was added, false otherwise
     */
    public synchronized boolean addPacket(int nMsgId, int nMsgPart)
        {
        return !isFlushed() && super.addPacket(nMsgId, nMsgPart);
        }
    
    // Declared at the super level
    /**
     * Add the passed Packet information to the notification Packet.  For the
    * Ack packet this and the write methods are synchronized as the packet may
    * be actively processed by both the publisher and receiver threads.  Ack
    * synchronizes this method as an Ack may be turned into a UdpPacket by the
    * Publisher while the Receiver is attempting to add to it.
    * 
    * @param packet  the Packet whose information should be added to this
    * notification Packet
    * @return true if the packet was added, false otherwise
     */
    public synchronized boolean addPacket(com.tangosol.coherence.component.net.packet.MessagePacket packet)
        {
        return super.addPacket(packet);
        }
    
    /**
     * Close the Ack, so that it may be sent.  When an Ack is closed tracking
    * information associated with the recipient member is added.  This method
    * is called on the Publisher thread.
     */
    public void close(com.tangosol.coherence.component.net.Member member)
        {
        // import Component.Net.Member$FlowControl as com.tangosol.coherence.component.net.Member.FlowControl;
        
        if (member != null)
            {
            flush(member);
        
            setNewestToPacketId(member.getNewestToPacketId());
        
            // request that acks be sent back at fraction of the flowcontrol threshold
            com.tangosol.coherence.component.net.Member.FlowControl fc = member.getFlowControl();
            setPreferredAckSize(fc == null ? 0xFFFF
                : Math.max(1, (fc.getOutstandingPacketThreshold() << 3) / 10));
            }
        }
    
    /**
     * Called once the Ack has reached its capacity.  When an Ack is flushed,
    * some tracking information associated with the recipient member is added
    * to the packet.  The rest of the tracking information will be included
    * when the Ack is finally closed; just prior to being sent.  This method
    * may be called by either the publisher or receiver.
     */
    public synchronized void flush(com.tangosol.coherence.component.net.Member member)
        {
        // don't allow it to be flushed more then once
        if (member != null && !isFlushed())
            {
            setFlushed(true);
            setNewestFromPacketId(member.getNewestFromPacketId());
            setContiguousFromPacketId(member.getContiguousFromPacketId());
            
            if (member.getPacketAck() == this)
                {
                // as this is not sync'd is is possible that we are clobbering a different
                // stored Ack.  This is very unlikely as Member.PacketAck is volatile.
                // If we do end up clobbering another Ack, it only means that the clobbered
                // Ack will be underutilized, i.e. sent without being full.
                member.setPacketAck(null);
                }
            }
        }
    
    // Accessor for the property "ContiguousFromMessageId"
    /**
     * Getter for property ContiguousFromMessageId.<p>
    * The last (based on numerical comparision) contiguous message ID (in trint
    * form) to have been received.  This indicates that all messages with lower
    * message IDs have been fully received.  This information is used by the
    * receipient of the Ack to fill in wholes in the ack stream, caused by
    * missing Acks.
    * 
    * @since 3.2
     */
    public int getContiguousFromMessageId()
        {
        return __m_ContiguousFromMessageId;
        }
    
    // Accessor for the property "ContiguousFromMessagePartIndex"
    /**
     * Getter for property ContiguousFromMessagePartIndex.<p>
    * The message part index corresponding to ContiguousFromMessageId.
    * 
    * @see ContiguousFromMessageId
    * @since 3.2
     */
    public int getContiguousFromMessagePartIndex()
        {
        return __m_ContiguousFromMessagePartIndex;
        }
    
    // Accessor for the property "ContiguousFromPacketId"
    /**
     * Translate are return the de-trintified PacketIdentifier.
    * 
    * @param lCurrent  the signed 8-byte integer value that the trint will be
    * translated based on ("close to")
     */
    public com.tangosol.net.internal.PacketIdentifier getContiguousFromPacketId(long lCurrent)
        {
        // import com.tangosol.net.internal.SimplePacketIdentifier;
        
        int iMsgId = getContiguousFromMessageId();
        if (iMsgId == 0)
            {
            return null;
            }
        
        long lMsgId = translateTrint(iMsgId, lCurrent);
        return new SimplePacketIdentifier(lMsgId, getContiguousFromMessagePartIndex());
        }
    
    // Declared at the super level
    /**
     * Getter for property Description.<p>
    * Human-readable description of attributes added to the sub-class of
    * Packet; used by toString for debugging purposes.
     */
    public String getDescription()
        {
        StringBuffer sb = new StringBuffer();
        
        sb.append("NewestTo=")
          .append(getNewestToMessageId())
          .append(':')
          .append(getNewestToMessagePartIndex());
        
        sb.append(", ");
        
        sb.append("NewestFrom=")
          .append(getNewestFromMessageId())
          .append(':')
          .append(getNewestFromMessagePartIndex());
        
        sb.append(", ");
         
        sb.append("ContiguousFrom=")
          .append(getContiguousFromMessageId())
          .append(':')
          .append(getContiguousFromMessagePartIndex());
        
        sb.append(", ")
          .append(super.getDescription());
        
        return sb.toString();
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
        return super.getLength()
            + 3 // trint newest to messageId
            + 3 // trint newest to message part index
            + 3 // trint newest from messageId
            + 3 // trint newest from message part index
            + 3 // trint contiguous from messageId
            + 3 // trint contiguous from message part index
            + 2; // short preferred Ack size
        }
    
    // Accessor for the property "NewestFromMessageId"
    /**
     * Getter for property NewestFromMessageId.<p>
    * From the perspective of the sender of the Ack.  This is the newest
    * message ID (int trint form) to be known to have been sent by recipient of
    * the Ack.
    * 
    * Note: this does not mean that the identified packet was received, the ID
    * may be known do to the other side having sent a MaxOutgoingId higher then
    * any received ID.
    * 
    * @since 3.2
     */
    protected int getNewestFromMessageId()
        {
        return __m_NewestFromMessageId;
        }
    
    // Accessor for the property "NewestFromMessagePartIndex"
    /**
     * Getter for property NewestFromMessagePartIndex.<p>
    * From the perspective of the sender of the Ack.  This is the greatest
    * message part index from the numerically greatest message ID to be known
    * to have been sent by recipient of the Ack.
    * 
    * @see MaxFromMessageId
    * @since 3.2
     */
    public int getNewestFromMessagePartIndex()
        {
        return __m_NewestFromMessagePartIndex;
        }
    
    // Accessor for the property "NewestFromPacketId"
    /**
     * Translate are return the de-trintified PacketIdentifier.
    * 
    * @param lCurrent  the signed 8-byte integer value that the trint will be
    * translated based on ("close to")
     */
    public com.tangosol.net.internal.PacketIdentifier getNewestFromPacketId(long lCurrent)
        {
        // import com.tangosol.net.internal.SimplePacketIdentifier;
        
        int iMsgId = getNewestFromMessageId();
        if (iMsgId == 0)
            {
            return null;
            }
        
        long lMsgId = translateTrint(iMsgId, lCurrent);
        return new SimplePacketIdentifier(lMsgId, getNewestFromMessagePartIndex());
        }
    
    // Accessor for the property "NewestToMessageId"
    /**
     * Getter for property NewestToMessageId.<p>
    * From the perspective of the sender of the Ack.  This is the newest
    * message ID (int trint form) to have been sent to the recipient of the Ack
    * from this member.  A value of zero indicates that no ackable packets have
    * previously been sent.
    * 
    * @since 3.2
     */
    public int getNewestToMessageId()
        {
        return __m_NewestToMessageId;
        }
    
    // Accessor for the property "NewestToMessagePartIndex"
    /**
     * Getter for property NewestToMessagePartIndex.<p>
    * From the perspective of the sender of the Ack.  This is the greatest
    * message part index from the numerically greatest message ID to have been
    * sent to the recipient of the Ack.
    * 
    * @see MaxToMessageId
    * @since 3.2
     */
    public int getNewestToMessagePartIndex()
        {
        return __m_NewestToMessagePartIndex;
        }
    
    // Accessor for the property "NewestToPacketId"
    /**
     * Translate are return the de-trintified PacketIdentifier.
    * 
    * @param lCurrent  the signed 8-byte integer value that the trint will be
    * translated based on ("close to")
     */
    public com.tangosol.net.internal.PacketIdentifier getNewestToPacketId(long lCurrent)
        {
        // import com.tangosol.net.internal.SimplePacketIdentifier;
        
        int iMsgId = getNewestToMessageId();
        if (iMsgId == 0)
            {
            return null;
            }
        
        long lMsgId = translateTrint(iMsgId, lCurrent);
        return new SimplePacketIdentifier(lMsgId, getNewestToMessagePartIndex());
        }
    
    // Accessor for the property "PreferredAckSize"
    /**
     * Getter for property PreferredAckSize.<p>
    * The size of the Ack (in Ack'd packets) which the sending node preferrs to
    * receive.  This is used so that the sending node can cause the receiving
    * node to Ack before the Ack packet is full, which is usefull when the
    * flowcontrol threshold falls below the size of an Ack.
     */
    public int getPreferredAckSize()
        {
        return __m_PreferredAckSize;
        }
    
    // Accessor for the property "Flushed"
    /**
     * Getter for property Flushed.<p>
    * Indicates whether or not the Ack has been flushed.
    * 
    * @since 3.2
     */
    public boolean isFlushed()
        {
        return __m_Flushed;
        }
    
    // Declared at the super level
    /**
     * Read the Packet (not counting the Packet type id) from a Stream.
    * 
    * Note: The read method is not responsible for configuring the "To" portion
    * of the packet.
    * 
    * @param stream  the DataInputStream to read from
    * @param nMemberId  this Member's id if known, otherwise zero
     */
    public void read(com.tangosol.io.ReadBuffer.BufferInput input, int nMemberId)
            throws java.io.IOException
        {
        super.read(input, nMemberId);
        
        setNewestToMessageId(readUnsignedTrint(input));
        setNewestToMessagePartIndex(readUnsignedTrint(input));
        setNewestFromMessageId(readUnsignedTrint(input));
        setNewestFromMessagePartIndex(readUnsignedTrint(input));
        setContiguousFromMessageId(readUnsignedTrint(input));
        setContiguousFromMessagePartIndex(readUnsignedTrint(input));
        setPreferredAckSize(input.readUnsignedShort());
        }
    
    // Accessor for the property "ContiguousFromMessageId"
    /**
     * Setter for property ContiguousFromMessageId.<p>
    * The last (based on numerical comparision) contiguous message ID (in trint
    * form) to have been received.  This indicates that all messages with lower
    * message IDs have been fully received.  This information is used by the
    * receipient of the Ack to fill in wholes in the ack stream, caused by
    * missing Acks.
    * 
    * @since 3.2
     */
    public void setContiguousFromMessageId(int pContiguousFromMessageId)
        {
        __m_ContiguousFromMessageId = pContiguousFromMessageId;
        }
    
    // Accessor for the property "ContiguousFromMessagePartIndex"
    /**
     * Setter for property ContiguousFromMessagePartIndex.<p>
    * The message part index corresponding to ContiguousFromMessageId.
    * 
    * @see ContiguousFromMessageId
    * @since 3.2
     */
    public void setContiguousFromMessagePartIndex(int pContiguousFromMessageId)
        {
        __m_ContiguousFromMessagePartIndex = pContiguousFromMessageId;
        }
    
    // Accessor for the property "ContiguousFromPacketId"
    /**
     * Setter for property ContiguousFromPacketId.<p>
    * Functional property for setting ContiguousFromMessageId and
    * ContiguousFromMessagePartIndex.
    * 
    * To obtain the ContiguousFromPacketId the caller must pass in the current
    * message index.
    * 
    * @see getContiguousFromPacketId(long lCurrent)
    * @since 3.2
     */
    public void setContiguousFromPacketId(com.tangosol.net.internal.PacketIdentifier ptid)
        {
        if (ptid == null)
            {
            setContiguousFromMessageId(0);
            setContiguousFromMessagePartIndex(0);
            }
        else
            {
            setContiguousFromMessageId(makeTrint(ptid.getFromMessageId()));
            setContiguousFromMessagePartIndex(ptid.getMessagePartIndex());
            }
        }
    
    // Accessor for the property "Flushed"
    /**
     * Setter for property Flushed.<p>
    * Indicates whether or not the Ack has been flushed.
    * 
    * @since 3.2
     */
    protected void setFlushed(boolean fFlushed)
        {
        __m_Flushed = fFlushed;
        }
    
    // Accessor for the property "NewestFromMessageId"
    /**
     * Setter for property NewestFromMessageId.<p>
    * From the perspective of the sender of the Ack.  This is the newest
    * message ID (int trint form) to be known to have been sent by recipient of
    * the Ack.
    * 
    * Note: this does not mean that the identified packet was received, the ID
    * may be known do to the other side having sent a MaxOutgoingId higher then
    * any received ID.
    * 
    * @since 3.2
     */
    protected void setNewestFromMessageId(int pMaxSentId)
        {
        __m_NewestFromMessageId = pMaxSentId;
        }
    
    // Accessor for the property "NewestFromMessagePartIndex"
    /**
     * Setter for property NewestFromMessagePartIndex.<p>
    * From the perspective of the sender of the Ack.  This is the greatest
    * message part index from the numerically greatest message ID to be known
    * to have been sent by recipient of the Ack.
    * 
    * @see MaxFromMessageId
    * @since 3.2
     */
    public void setNewestFromMessagePartIndex(int pMaxSentId)
        {
        __m_NewestFromMessagePartIndex = pMaxSentId;
        }
    
    // Accessor for the property "NewestFromPacketId"
    /**
     * Setter for property NewestFromPacketId.<p>
    * Helper for setting NewestFromMessageId and NewestFromMessagePartIndex
    * 
    * @since 3.2
     */
    protected void setNewestFromPacketId(com.tangosol.net.internal.PacketIdentifier ptid)
        {
        if (ptid != null)
            {
            setNewestFromMessageId(makeTrint(ptid.getFromMessageId()));
            setNewestFromMessagePartIndex(ptid.getMessagePartIndex());
            }
        }
    
    // Accessor for the property "NewestToMessageId"
    /**
     * Setter for property NewestToMessageId.<p>
    * From the perspective of the sender of the Ack.  This is the newest
    * message ID (int trint form) to have been sent to the recipient of the Ack
    * from this member.  A value of zero indicates that no ackable packets have
    * previously been sent.
    * 
    * @since 3.2
     */
    public void setNewestToMessageId(int pNewestToMessageId)
        {
        __m_NewestToMessageId = pNewestToMessageId;
        }
    
    // Accessor for the property "NewestToMessagePartIndex"
    /**
     * Setter for property NewestToMessagePartIndex.<p>
    * From the perspective of the sender of the Ack.  This is the greatest
    * message part index from the numerically greatest message ID to have been
    * sent to the recipient of the Ack.
    * 
    * @see MaxToMessageId
    * @since 3.2
     */
    public void setNewestToMessagePartIndex(int pMaxSentId)
        {
        __m_NewestToMessagePartIndex = pMaxSentId;
        }
    
    // Accessor for the property "NewestToPacketId"
    /**
     * Setter for property NewestToPacketId.<p>
    * Helper for setting NewestToMessageId and NewestToMessagePartIndex
    * 
    * @since 3.2
     */
    public void setNewestToPacketId(com.tangosol.net.internal.PacketIdentifier ptid)
        {
        if (ptid == null)
            {
            setNewestToMessageId(0);
            setNewestToMessagePartIndex(0);
            }
        else
            {
            setNewestToMessageId(makeTrint(ptid.getFromMessageId()));
            setNewestToMessagePartIndex(ptid.getMessagePartIndex());
            }
        }
    
    // Accessor for the property "PreferredAckSize"
    /**
     * Setter for property PreferredAckSize.<p>
    * The size of the Ack (in Ack'd packets) which the sending node preferrs to
    * receive.  This is used so that the sending node can cause the receiving
    * node to Ack before the Ack packet is full, which is usefull when the
    * flowcontrol threshold falls below the size of an Ack.
     */
    public void setPreferredAckSize(int cPackets)
        {
        __m_PreferredAckSize = cPackets;
        }
    
    // Declared at the super level
    /**
     * Skip the Packet (not counting the Packet type id) from a BufferInput.
    * 
    * @param input  the BufferInput to read from
    * @param nType the packet type
     */
    public static void skip(com.tangosol.io.ReadBuffer.BufferInput input, int nType)
            throws java.io.IOException
        {
        // import Component.Net.Packet.NotifyPacket;
        
        NotifyPacket.skip(input, nType);
        
        int cbSkip = 3  // trint NewestToMsgId
                   + 3  // trint NewestToMsgPartIndex
                   + 3  // trint NewestFromMsgId
                   + 3  // trint NewestFromMsgPartIndex
                   + 3  // trint ContigFromMsgId
                   + 3  // trint ContigFromMsgPartIndx
                   + 2; // short PreferredAckSize
        ensureSkipBytes(input, cbSkip);
        }
    
    // Declared at the super level
    /**
     * Write the Packet to a BufferOutput.
    * 
    * @param output  the BufferOutput to write to
    * @param nMemberId  if non-zero, it indicates that the Packet should be
    * addressed only to one member
     */
    public void write(com.tangosol.io.WriteBuffer.BufferOutput output)
            throws java.io.IOException
        {
        super.write(output);
        
        writeTrint(output, getNewestToMessageId());
        writeTrint(output, getNewestToMessagePartIndex());
        writeTrint(output, getNewestFromMessageId());
        writeTrint(output, getNewestFromMessagePartIndex());
        writeTrint(output, getContiguousFromMessageId());
        writeTrint(output, getContiguousFromMessagePartIndex());
        output.writeShort(getPreferredAckSize());
        }
    }
