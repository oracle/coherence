
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.packet.NotifyPacket

package com.tangosol.coherence.component.net.packet;

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
public abstract class NotifyPacket
        extends    com.tangosol.coherence.component.net.Packet
    {
    // ---- Fields declarations ----
    
    /**
     * Property MessageId
     *
     * The Message Id that specifies the Packet being acknowledged or
     * requested.
     * 
     * This indexed property is in the range 0..NotifyCount-1.
     */
    private int[] __m_MessageId;
    
    /**
     * Property MessagePartIndex
     *
     * A 0-based index of the Message part that specifies the Packet being
     * requested or acknowledged; only non-zero to acknowledge a Sequel Packet.
     * Corresponds to MessageId property.
     * 
     * This indexed property is in the range 0..NotifyCount-1.
     */
    private int[] __m_MessagePartIndex;
    
    /**
     * Property NotifyCount
     *
     * The number of items in the MessageId and MessagePartIndex properties.
     * (The number of Packets being requested or acknowledged.)
     * 
     * @volatile - set on receiver, read on publisher; this also serves as a
     * memory barrier for the contents of the message id and message part
     * arrays.
     */
    private volatile int __m_NotifyCount;
    
    /**
     * Property ScheduledMillis
     *
     * The time, in milliseconds, when this packet is scheduled to be sent.
     * 
     * @volatile  Accessed by the receiver and publisher without
     * syncronization.  The receiver will reset this value to NOW once it has
     * filled a Ack packet.
     */
    private volatile long __m_ScheduledMillis;
    
    // Initializing constructor
    public NotifyPacket(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            clz = Class.forName("com.tangosol.coherence/component/net/packet/NotifyPacket".replace('/', '.'));
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
     * Add the passed Packet information to the notification Packet.
    * 
    * @param nMsgId  the Message id to add to this notification Packet
    * @param nMsgPart  the Message part number to add to this notification
    * Packet, or zero if n/a
    * @return true if the packet was added, false otherwise
     */
    public boolean addPacket(int nMsgId, int nMsgPart)
        {
        _assert(isOutgoing());
        
        int c = getNotifyCount();
        
        setMessageId       (c, nMsgId);
        setMessagePartIndex(c, nMsgPart);
        
        // if case any of the above causes OutOfMemory, this should be last
        // to prevent the ArrayIndexOutOfBounds during write
        setNotifyCount(c + 1);
        
        return true;
        }
    
    /**
     * Add the passed Packet information to the notification Packet.
    * 
    * @param packet  the Packet whose information should be added to this
    * notification Packet
    * @return true if the packet was added, false otherwise
     */
    public boolean addPacket(MessagePacket packet)
        {
        return addPacket(makeTrint(packet.getFromMessageId()),
                packet.getMessagePartIndex());
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
        
        int c = getNotifyCount();
        sb.append("NotifyCount=")
          .append(c);
        
        if (c > 0)
            {
            sb.append(", MessageId:MessagePartIndex=[");
            for (int i = 0; i < c; ++i)
                {
                if (i > 0)
                    {
                    sb.append(", ");
                    }
                sb.append(getMessageId(i))
                  .append(':')
                  .append(getMessagePartIndex(i));
                }
            sb.append("]");
            }
        
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
        return 4                       // int packet type
               + 2                     // short from id
               + 2                     // short to id
               + 2                     // short packet pair count
               + getNotifyCount() * 6; // trint pairs (message id, part index)
        }
    
    // Accessor for the property "MessageId"
    /**
     * Getter for property MessageId.<p>
    * The Message Id that specifies the Packet being acknowledged or requested.
    * 
    * This indexed property is in the range 0..NotifyCount-1.
     */
    protected int[] getMessageId()
        {
        return __m_MessageId;
        }
    
    // Accessor for the property "MessageId"
    /**
     * Getter for property MessageId.<p>
    * The Message Id that specifies the Packet being acknowledged or requested.
    * 
    * This indexed property is in the range 0..NotifyCount-1.
     */
    public int getMessageId(int i)
        {
        return getMessageId()[i];
        }
    
    // Accessor for the property "MessagePartIndex"
    /**
     * Getter for property MessagePartIndex.<p>
    * A 0-based index of the Message part that specifies the Packet being
    * requested or acknowledged; only non-zero to acknowledge a Sequel Packet.
    * Corresponds to MessageId property.
    * 
    * This indexed property is in the range 0..NotifyCount-1.
     */
    protected int[] getMessagePartIndex()
        {
        return __m_MessagePartIndex;
        }
    
    // Accessor for the property "MessagePartIndex"
    /**
     * Getter for property MessagePartIndex.<p>
    * A 0-based index of the Message part that specifies the Packet being
    * requested or acknowledged; only non-zero to acknowledge a Sequel Packet.
    * Corresponds to MessageId property.
    * 
    * This indexed property is in the range 0..NotifyCount-1.
     */
    public int getMessagePartIndex(int i)
        {
        return getMessagePartIndex()[i];
        }
    
    // Accessor for the property "NotifyCount"
    /**
     * Getter for property NotifyCount.<p>
    * The number of items in the MessageId and MessagePartIndex properties.
    * (The number of Packets being requested or acknowledged.)
    * 
    * @volatile - set on receiver, read on publisher; this also serves as a
    * memory barrier for the contents of the message id and message part arrays.
     */
    public int getNotifyCount()
        {
        return __m_NotifyCount;
        }
    
    // Accessor for the property "ScheduledMillis"
    /**
     * Getter for property ScheduledMillis.<p>
    * The time, in milliseconds, when this packet is scheduled to be sent.
    * 
    * @volatile  Accessed by the receiver and publisher without syncronization.
    *  The receiver will reset this value to NOW once it has filled a Ack
    * packet.
     */
    public long getScheduledMillis()
        {
        return __m_ScheduledMillis;
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
        setToId  (input.readUnsignedShort());
        setFromId(input.readUnsignedShort());
        int c = input.readUnsignedShort();
        setNotifyCount(c);
        
        // presize indexed props
        setMessageId       (new int[c]);
        setMessagePartIndex(new int[c]);
        
        for (int i = 0; i < c; ++i)
            {
            setMessageId       (i, readUnsignedTrint(input));
            setMessagePartIndex(i, readUnsignedTrint(input));
            }
        }
    
    // Accessor for the property "MessageId"
    /**
     * Setter for property MessageId.<p>
    * The Message Id that specifies the Packet being acknowledged or requested.
    * 
    * This indexed property is in the range 0..NotifyCount-1.
     */
    protected void setMessageId(int[] anMsgId)
        {
        __m_MessageId = anMsgId;
        }
    
    // Accessor for the property "MessageId"
    /**
     * Setter for property MessageId.<p>
    * The Message Id that specifies the Packet being acknowledged or requested.
    * 
    * This indexed property is in the range 0..NotifyCount-1.
     */
    protected void setMessageId(int i, int nMsgId)
        {
        int[] an = getMessageId();
        
        if (an == null || i >= an.length)
            {
            // resize, making the array bigger than necessary (avoid resizes)
            int   cNew  = Math.max(i + (i >>> 1), 16);
            int[] anNew = new int[cNew];
        
            // copy original data
            if (an != null)
                {
                System.arraycopy(an, 0, anNew, 0, an.length);
                }
        
            setMessageId(an = anNew);
            }
        
        an[i] = nMsgId;
        }
    
    // Accessor for the property "MessagePartIndex"
    /**
     * Setter for property MessagePartIndex.<p>
    * A 0-based index of the Message part that specifies the Packet being
    * requested or acknowledged; only non-zero to acknowledge a Sequel Packet.
    * Corresponds to MessageId property.
    * 
    * This indexed property is in the range 0..NotifyCount-1.
     */
    protected void setMessagePartIndex(int[] anMsgPart)
        {
        __m_MessagePartIndex = anMsgPart;
        }
    
    // Accessor for the property "MessagePartIndex"
    /**
     * Setter for property MessagePartIndex.<p>
    * A 0-based index of the Message part that specifies the Packet being
    * requested or acknowledged; only non-zero to acknowledge a Sequel Packet.
    * Corresponds to MessageId property.
    * 
    * This indexed property is in the range 0..NotifyCount-1.
     */
    protected void setMessagePartIndex(int i, int nMsgPart)
        {
        int[] an = getMessagePartIndex();
        
        if (an == null || i >= an.length)
            {
            // resize, making the array bigger than necessary (avoid resizes)
            int   cNew  = Math.max(i + (i >>> 1), 16);
            int[] anNew = new int[cNew];
        
            // copy original data
            if (an != null)
                {
                System.arraycopy(an, 0, anNew, 0, an.length);
                }
        
            setMessagePartIndex(an = anNew);
            }
        
        an[i] = nMsgPart;
        }
    
    // Accessor for the property "NotifyCount"
    /**
     * Setter for property NotifyCount.<p>
    * The number of items in the MessageId and MessagePartIndex properties.
    * (The number of Packets being requested or acknowledged.)
    * 
    * @volatile - set on receiver, read on publisher; this also serves as a
    * memory barrier for the contents of the message id and message part arrays.
     */
    protected void setNotifyCount(int c)
        {
        __m_NotifyCount = c;
        }
    
    // Accessor for the property "ScheduledMillis"
    /**
     * Setter for property ScheduledMillis.<p>
    * The time, in milliseconds, when this packet is scheduled to be sent.
    * 
    * @volatile  Accessed by the receiver and publisher without syncronization.
    *  The receiver will reset this value to NOW once it has filled a Ack
    * packet.
     */
    public void setScheduledMillis(long cMillis)
        {
        __m_ScheduledMillis = cMillis;
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
        input.readUnsignedShort(); // ToId
        input.readUnsignedShort(); // FromId
        
        int cbSkip = input.readUnsignedShort() // short NotifyCount
                    * (3 + 3) ;                // trint MsgId + trint MsgPartIndex
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
        output.writeInt(getPacketType());
        output.writeShort(getToId());
        output.writeShort(getFromId());
        
        int   c         = getNotifyCount();
        int[] anMsgId   = getMessageId();
        int[] anMsgPart = getMessagePartIndex();
        
        output.writeShort(c);
        for (int i = 0; i < c; ++i)
            {
            writeTrint(output, anMsgId  [i]);
            writeTrint(output, anMsgPart[i]);
            }
        }
    }
