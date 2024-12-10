
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.packet.messagePacket.Sequel

package com.tangosol.coherence.component.net.packet.messagePacket;

import com.tangosol.coherence.component.net.Member;
import com.tangosol.coherence.component.net.MemberSet;
import com.tangosol.coherence.component.net.memberSet.DependentMemberSet;
import com.tangosol.io.nio.ByteBufferReadBuffer;
import com.tangosol.util.Base;
import java.io.IOException;
import java.sql.Time;
import java.util.Iterator;

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
public class Sequel
        extends    com.tangosol.coherence.component.net.packet.MessagePacket
    {
    // ---- Fields declarations ----
    
    // Default constructor
    public Sequel()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public Sequel(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            setDeliveryState(0);
            setPacketType(232718552);
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
    
    // Getter for virtual constant ConfirmationRequired
    public boolean isConfirmationRequired()
        {
        return true;
        }
    
    //++ getter for static property _Instance
    /**
     * Getter for property _Instance.<p>
    * Auto generated
     */
    public static com.tangosol.coherence.Component get_Instance()
        {
        return new com.tangosol.coherence.component.net.packet.messagePacket.Sequel();
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
            clz = Class.forName("com.tangosol.coherence/component/net/packet/messagePacket/Sequel".replace('/', '.'));
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
     * @see Directed#selectType
    * @see Sequel#selectType
     */
    public static int calcHeaderLength(com.tangosol.coherence.component.net.MemberSet memberSet)
        {
        switch(selectType(memberSet))
            {
            case TYPE_NO_DESTINATION:
                return 0;
        
            case TYPE_SEQUEL_ONE:
                return  4    // packet-type TYPE_SEQUEL_ONE (int)
                        + 2  // to-id (short)
                        + 2  // from-id (short)
                        + 3  // from-msg-id (trint)
                        + 3  // msg-part (trint)
                        + 2; // payload-length (short)
        
            case TYPE_SEQUEL_FEW:
                return 4                        // packet-type TYPE_SEQUEL_FEW (int)
                       + 1                      // to-member-id-count (byte)
                       + (2 * memberSet.size()) // to-member-ids (short[])
                       + 2                      // from-id (short)
                       + 3                      // from-msg-id (trint)
                       + 3                      // msg-part (trint)
                       + 2;                     // payload-length (short)
        
        
            case TYPE_SEQUEL_MANY:
                return 4                                           // packet-type TYPE_SEQUEL_MANY (int)
                       + 1                                         // to-member-id-bitset-count (byte)
                       + (4 * ((memberSet.getLastId() + 31) / 32)) // to-member-ids-bitset (int[])
                       + 2                                         // from-id (short)
                       + 3                                         // from-msg-id (trint)
                       + 3                                         // msg-part (trint)
                       + 2;                                        // payload-length (short)
                 
            default:
                throw new IllegalArgumentException("Unknown type");
            }
        }
    
    // Declared at the super level
    /**
     * Getter for property Description.<p>
    * Human-readable description of attributes added to the sub-class of
    * Packet; used by toString for debugging purposes.
     */
    public String getDescription()
        {
        // import Component.Net.Member;
        // import Component.Net.MemberSet.DependentMemberSet;
        // import com.tangosol.io.nio.ByteBufferReadBuffer;
        // import com.tangosol.util.Base;
        // import java.sql.Time;
        // import java.util.Iterator;
        
        StringBuffer sb = new StringBuffer();
        
        sb.append("ToMemberSet=");
        DependentMemberSet setMember = getToMemberSet();
        if (setMember == null)
            {
            sb.append("null");
            }
        else
            {
            sb.append('[');
            boolean fFirst = true;
            for (Iterator iter = setMember.iterator(); iter.hasNext(); )
                {
                if (fFirst)
                    {
                    fFirst = false;
                    }
                else
                    {
                    sb.append(", ");
                    }
        
                Member member  = (Member) iter.next();
                int    nMember = member.getId();
                sb.append(nMember);
                }
            sb.append(']');
            }
        
        long   ldtResend  = getResendScheduled();
        long   ldtTimeout = getResendTimeout();
        String sResend    = ldtResend  <= 0 ? "none" : new Time(ldtResend).toString()
            + "." + ldtResend % 1000;
        String sTimeout   = ldtTimeout <= 0 ? "none" : new Time(ldtTimeout).toString()
            + "." + ldtTimeout % 1000;
        
        sb.append(", ServiceId=")
          .append(getServiceId())
          .append(", MessageType=")
          .append(getMessageType())
          .append(", FromMessageId=")
          .append(getFromMessageId())
          .append(", MessagePartCount=")
          .append(getMessagePartCount())
          .append(", MessagePartIndex=")
          .append(getMessagePartIndex())
          .append(", NackInProgress=")
          .append(isNackInProgress())
          .append(", ResendScheduled=")
          .append(sResend)
          .append(", Timeout=")
          .append(sTimeout)
          .append(", PendingResendSkips=")
          .append(getPendingResendSkips())
          .append(", DeliveryState=")
          .append(formatDeliveryState(getDeliveryState()))  
          .append(", Body=");
        
        if (isOutgoing())
            {
            sb.append(getReadBuffer() == null
                    ? "Empty"
                    : Base.toHexEscape(getReadBuffer().toByteArray()));
            }
        else
            {
            sb.append(getByteBuffer() == null
                    ? "Empty"
                    : Base.toHexEscape(new ByteBufferReadBuffer(getByteBuffer()).toByteArray()));
            }    
        
        return sb.toString();
        }
    
    // Declared at the super level
    /**
     * Getter for property HeaderLength.<p>
    * The length of the packet's header in bytes.
     */
    public int getHeaderLength()
        {
        return calcHeaderLength(getToMemberSet());
        }
    
    // Declared at the super level
    /**
     * Read the Packet (not counting the Packet type id) from a BufferInput.
    * 
    * Note: The read method is not responsible for configuring the "To" portion
    * of the packet.
    * 
    * @param input  the BufferInput to read from
    * @param nMemberId  this Member's id if known, otherwise zero
     */
    public void read(com.tangosol.io.ReadBuffer.BufferInput input, int nMemberId)
            throws java.io.IOException
        {
        // import java.io.IOException;
        
        // skip "to" stuff -- the Packet is supposed to be for this Member
        // (ToId is set by the Packet.instantiate method)
        switch (getPacketType())
            {
            case TYPE_SEQUEL_ONE:
                input.readUnsignedShort();
                break;
        
            case TYPE_SEQUEL_FEW:
                input.skip(input.readUnsignedByte() << 1);
                break;
        
            case TYPE_SEQUEL_MANY:
                input.skip(input.readUnsignedByte() << 2);
                break;
        
            default:
                throw new IOException("unknown packet type: " + getPacketType());
            }
        
        setFromId(input.readUnsignedShort());
        setFromMessageId(readUnsignedTrint(input));
        setMessagePartIndex(readUnsignedTrint(input));
        setBodyLength(input.readUnsignedShort());
        
        assertIO(input.available() >= getBodyLength());
        }
    
    protected static int selectType(com.tangosol.coherence.component.net.MemberSet memberSet)
        {
        // decide on the best format to use to write the Sequel Packet
        int cMembers = memberSet == null ? 1 : memberSet.size();
        
        switch (cMembers)
            {
            case 0:
                // all the members have exited the cluster
                return TYPE_NO_DESTINATION;
        
            case 1:
                return TYPE_SEQUEL_ONE;
        
            case 2:
                // as small for 2 Members to send a "few" Packet
                return TYPE_SEQUEL_FEW;
        
            default:
                {
                if (cMembers > 255)
                    {
                    // "few" Packets only support up to 255 Members
                    return TYPE_SEQUEL_MANY;
                    }
        
                // calculate the optimum packing for the specified number of Members
                // see calcHeaderLength for detailed explaination
                return 15 + 2 * cMembers
                    <= 15 + 4 * ((memberSet.getLastId() + 31) / 32)
                        ? TYPE_SEQUEL_FEW : TYPE_SEQUEL_MANY;
                }
            }
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
        // import java.io.IOException;
        
        int cbSkip = 0;
        switch (nType)
            {
            case TYPE_SEQUEL_ONE:
                cbSkip += 2;
                break;
        
            case TYPE_SEQUEL_FEW:
                cbSkip += input.readUnsignedByte() << 1;
                break;
        
            case TYPE_SEQUEL_MANY:
                cbSkip += input.readUnsignedByte() << 2;
                break;
        
            default:
                throw new IOException("unknown packet type: " + nType);
            }
        
        cbSkip += 8;
            // short FromId
            // trint FromMsgId
            // trint MessagePartIndex
        ensureSkipBytes(input, cbSkip);
        ensureSkipBytes(input, input.readUnsignedShort()); // ready length and skip body
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
        // import Component.Net.MemberSet;
        // import java.io.IOException;
        
        MemberSet memberSet = getToMemberSet();
        
        if (memberSet == null)
            {
            // common path
            setPacketType(TYPE_SEQUEL_ONE);
            output.writeInt(TYPE_SEQUEL_ONE);
            output.writeShort(getToId());    
            }
        else
            {
            // since the MemberSet is a dependent memberset it may change concurrently
            // with serialization, as members departs the cluster. Synchronizing on the
            // MemberSet will ensure that the serialization will not be corrupted
            synchronized (memberSet)
                {
                int nType = selectType(memberSet);
        
                if (nType == TYPE_NO_DESTINATION)
                    {
                    // all members must have departed the cluster between
                    // the packet was enqueued and being serialized; nothing
                    // to serialize
                    return;
                    }
            
                setPacketType(nType);
                output.writeInt(nType);
        
                switch (nType)
                    {
                    case TYPE_SEQUEL_ONE:
                        memberSet.writeOne(output);
                        break;
        
                    case TYPE_SEQUEL_FEW:
                        memberSet.writeFew(output);
                        break;
        
                    case TYPE_SEQUEL_MANY:
                        // see Directed.write
                        memberSet.writeMany(output);
                        break;
        
                    default:
                        throw new IOException("unknown packet type: " + nType);
                    }
                }
            }
        
        output.writeShort(getFromId());
        writeTrint(output, getFromMessageId());
        writeTrint(output, getMessagePartIndex());
        output.writeShort(getBodyLength());
        output.writeBuffer(getReadBuffer());
        }
    }
