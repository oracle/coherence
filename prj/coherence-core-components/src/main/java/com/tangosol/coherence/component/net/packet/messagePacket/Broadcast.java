
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.packet.messagePacket.Broadcast

package com.tangosol.coherence.component.net.packet.messagePacket;

import com.tangosol.io.nio.ByteBufferReadBuffer;
import com.tangosol.util.Base;
import java.nio.ByteBuffer;

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
public class Broadcast
        extends    com.tangosol.coherence.component.net.packet.MessagePacket
    {
    // ---- Fields declarations ----
    
    /**
     * Property BufferController
     *
     * Since Broadcast packets don't get confirmed, they are no longer needed
     * after the "best delivery attempt" was made. Unlike confirmable packets
     * that could share buffers, the broadcast ones don't, so the Publisher
     * should release all pooled resources as soon as the packet is sent by
     * calling the dispose() method on the BufferController.
     */
    private transient com.oracle.coherence.common.base.Disposable __m_BufferController;
    
    /**
     * Property ClusterNameBuffer
     *
     * This cluster's name length encoded into a ByteBuffer.
     * 
     * This field is only set for outbound packets.  The PacketListener filters
     * out forgeign cluster datagrams and thus and they never get materialized
     * into Broadcast packets.
     */
    private transient java.nio.ByteBuffer __m_ClusterNameBuffer;
    
    /**
     * Property FromAddress
     *
     * The source address of the packet.
     */
    private java.net.InetSocketAddress __m_FromAddress;
    
    /**
     * Property ToAddress
     *
     * The recepient address for this Broadcast message. This property is
     * specified only for the "reply" type of DiscoveryMessages. If the
     * multicast is disabled and the address is not specified, a list of
     * well-known-addresses will be used.
     */
    private transient java.net.InetSocketAddress[] __m_ToAddress;
    
    // Default constructor
    public Broadcast()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public Broadcast(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            setMessagePartCount(1);
            setMessagePartIndex(0);
            setPacketType(232718546);
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
    
    // Getter for virtual constant OutgoingBroadcast
    public boolean isOutgoingBroadcast()
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
        return new com.tangosol.coherence.component.net.packet.messagePacket.Broadcast();
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
            clz = Class.forName("com.tangosol.coherence/component/net/packet/messagePacket/Broadcast".replace('/', '.'));
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
     * @see Directed#selectType
    * @see Sequel#selectType
     */
    public static int calcBodyLength(int cbHeader, int cbPref, int cbMax)
        {
        return cbMax - cbHeader;
        }
    
    /**
     * @see Directed#selectType
    * @see Sequel#selectType
     */
    public int calcHeaderLength()
        {
        // (see doc for TYPE_BROADCAST)
        return 10 + getClusterNameBuffer().remaining();
        }
    
    // Accessor for the property "BufferController"
    /**
     * Getter for property BufferController.<p>
    * Since Broadcast packets don't get confirmed, they are no longer needed
    * after the "best delivery attempt" was made. Unlike confirmable packets
    * that could share buffers, the broadcast ones don't, so the Publisher
    * should release all pooled resources as soon as the packet is sent by
    * calling the dispose() method on the BufferController.
     */
    public com.oracle.coherence.common.base.Disposable getBufferController()
        {
        return __m_BufferController;
        }
    
    // Accessor for the property "ClusterNameBuffer"
    /**
     * Getter for property ClusterNameBuffer.<p>
    * This cluster's name length encoded into a ByteBuffer.
    * 
    * This field is only set for outbound packets.  The PacketListener filters
    * out forgeign cluster datagrams and thus and they never get materialized
    * into Broadcast packets.
     */
    public java.nio.ByteBuffer getClusterNameBuffer()
        {
        return __m_ClusterNameBuffer;
        }
    
    // Declared at the super level
    /**
     * Getter for property Description.<p>
    * Human-readable description of attributes added to the sub-class of
    * Packet; used by toString for debugging purposes.
     */
    public String getDescription()
        {
        // import com.tangosol.util.Base;
        // import com.tangosol.io.nio.ByteBufferReadBuffer;
        
        StringBuffer sb = new StringBuffer();
        
        sb.append("MessageType=")
          .append(getMessageType())
          .append(", ServiceId=")
          .append(getServiceId())
          .append(", MessagePartCount=")
          .append(getMessagePartCount())
          .append(", MessagePartIndex=")
          .append(getMessagePartIndex())
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
    
    // Accessor for the property "FromAddress"
    /**
     * Getter for property FromAddress.<p>
    * The source address of the packet.
     */
    public java.net.InetSocketAddress getFromAddress()
        {
        return __m_FromAddress;
        }
    
    // Declared at the super level
    /**
     * Getter for property HeaderLength.<p>
    * The length of the packet's header in bytes.
     */
    public int getHeaderLength()
        {
        return calcHeaderLength();
        }
    
    // Accessor for the property "ToAddress"
    /**
     * Getter for property ToAddress.<p>
    * The recepient address for this Broadcast message. This property is
    * specified only for the "reply" type of DiscoveryMessages. If the
    * multicast is disabled and the address is not specified, a list of
    * well-known-addresses will be used.
     */
    public java.net.InetSocketAddress[] getToAddress()
        {
        return __m_ToAddress;
        }
    
    // Accessor for the property "ToAddress"
    /**
     * Getter for property ToAddress.<p>
    * The recepient address for this Broadcast message. This property is
    * specified only for the "reply" type of DiscoveryMessages. If the
    * multicast is disabled and the address is not specified, a list of
    * well-known-addresses will be used.
     */
    public java.net.InetSocketAddress getToAddress(int i)
        {
        return getToAddress()[i];
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
        ensureSkipBytes(input, input.readUnsignedShort()); // skip cluster name
        
        setFromId(input.readUnsignedShort());
        setMessageType(input.readShort());
        setBodyLength(input.readUnsignedShort());
        
        assertIO(input.available() >= getBodyLength());
        }
    
    // Accessor for the property "BufferController"
    /**
     * Setter for property BufferController.<p>
    * Since Broadcast packets don't get confirmed, they are no longer needed
    * after the "best delivery attempt" was made. Unlike confirmable packets
    * that could share buffers, the broadcast ones don't, so the Publisher
    * should release all pooled resources as soon as the packet is sent by
    * calling the dispose() method on the BufferController.
     */
    public void setBufferController(com.oracle.coherence.common.base.Disposable controller)
        {
        __m_BufferController = controller;
        }
    
    // Accessor for the property "ClusterNameBuffer"
    /**
     * Setter for property ClusterNameBuffer.<p>
    * This cluster's name length encoded into a ByteBuffer.
    * 
    * This field is only set for outbound packets.  The PacketListener filters
    * out forgeign cluster datagrams and thus and they never get materialized
    * into Broadcast packets.
     */
    public void setClusterNameBuffer(java.nio.ByteBuffer bufBuffer)
        {
        __m_ClusterNameBuffer = bufBuffer;
        }
    
    // Accessor for the property "FromAddress"
    /**
     * Setter for property FromAddress.<p>
    * The source address of the packet.
     */
    public void setFromAddress(java.net.InetSocketAddress addressFrom)
        {
        __m_FromAddress = addressFrom;
        }
    
    // Accessor for the property "ToAddress"
    /**
     * Setter for property ToAddress.<p>
    * The recepient address for this Broadcast message. This property is
    * specified only for the "reply" type of DiscoveryMessages. If the
    * multicast is disabled and the address is not specified, a list of
    * well-known-addresses will be used.
     */
    public void setToAddress(java.net.InetSocketAddress[] aaddressTo)
        {
        __m_ToAddress = aaddressTo;
        }
    
    // Accessor for the property "ToAddress"
    /**
     * Setter for property ToAddress.<p>
    * The recepient address for this Broadcast message. This property is
    * specified only for the "reply" type of DiscoveryMessages. If the
    * multicast is disabled and the address is not specified, a list of
    * well-known-addresses will be used.
     */
    public void setToAddress(int i, java.net.InetSocketAddress addressTo)
        {
        getToAddress()[i] = addressTo;
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
        ensureSkipBytes(input, input.readUnsignedShort()); // skip cluster name
        input.readUnsignedShort(); // fromId
        input.readShort();         // msgType
        
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
        // import java.nio.ByteBuffer;
        
        ByteBuffer bufName = getClusterNameBuffer();
        
        output.writeInt(TYPE_BROADCAST);
        output.write(bufName.array(), bufName.arrayOffset() + bufName.position(), bufName.remaining());
        output.writeShort(getFromId());
        output.writeShort(getMessageType());
        output.writeShort(getBodyLength());
        output.writeBuffer(getReadBuffer());
        }
    }
