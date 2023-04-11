
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.Packet

package com.tangosol.coherence.component.net;

import com.tangosol.coherence.component.net.packet.DiagnosticPacket;
import com.tangosol.coherence.component.net.packet.MessagePacket;
import com.tangosol.coherence.component.net.packet.messagePacket.Broadcast;
import com.tangosol.coherence.component.net.packet.messagePacket.Directed;
import com.tangosol.coherence.component.net.packet.messagePacket.Sequel;
import com.tangosol.coherence.component.net.packet.notifyPacket.Ack;
import com.tangosol.coherence.component.net.packet.notifyPacket.Request;
import com.tangosol.io.ReadBuffer;
import com.tangosol.io.nio.ByteBufferReadBuffer;
import com.tangosol.util.Base;
import java.io.EOFException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.sql.Time;
import java.util.ArrayList;

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
public abstract class Packet
        extends    com.tangosol.coherence.component.Net
    {
    // ---- Fields declarations ----
    
    /**
     * Property FromId
     *
     * This is the sender's Member id. The id may be zero only for Broadcast
     * Packets, which are used before an id is obtained.
     */
    private int __m_FromId;
    
    /**
     * Property PacketType
     *
     * Specifies the persistence layout of the Packet. This value is available
     * only for received Packets. See the enumeration of type values on the
     * Packet component that begin with "TYPE_".
     */
    private int __m_PacketType;
    
    /**
     * Property ReceivedMillis
     *
     * Date/time value when the Packet was received.
     */
    private long __m_ReceivedMillis;
    
    /**
     * Property SentCount
     *
     * The number of times an outgoing packet has been sent.
     */
    private int __m_SentCount;
    
    /**
     * Property SentMillis
     *
     * Date/time value when the Packet was sent. For Packets that can be
     * resent, this is the most recent date/time value that the Packet was sent.
     */
    private long __m_SentMillis;
    
    /**
     * Property ToId
     *
     * This is the receiver's Member id. This property is valid for all Packets
     * except outgoing Directed and Sequel Packets that correspond to Messages
     * with multiple recipient Members.
     */
    private int __m_ToId;
    
    /**
     * Property TRINT_DOMAIN_SPAN
     *
     * Trint uses 6 hexits (3 bytes), so its domain span is 0x01000000.
     */
    public static final int TRINT_DOMAIN_SPAN = 16777216;
    
    /**
     * Property TRINT_MAX_VALUE
     *
     * Trint uses 6 hexits (3 bytes), so its maximum is 0x00FFFFFF.
     */
    public static final int TRINT_MAX_VALUE = 16777215;
    
    /**
     * Property TRINT_MAX_VARIANCE
     *
     * Trint uses 6 hexits (3 bytes), so its maximum variance (from a "current"
     * value) is half its domain span, or 0x00800000.
     */
    public static final int TRINT_MAX_VARIANCE = 8388608;
    
    /**
     * Property TYPE_ACK
     *
     * An Ack Packet is sent to an explicitly specified Member of the network
     * as a receipt confirmation for one or more messages.
     * 
     * value=0x0DDF00D1
     * 
     * Binary format:
     *     packet-type TYPE_ACK (4 bytes)
     *     to-id (2 bytes)
     *     from-id (2 bytes)
     *     newest-to-msg-id (3 bytes) (since 3.2)
     *     newest-to-msg-part (3 bytes) (since 3.2)
     *     newest-from-msg-id (3 bytes) (since 3.2)
     *     newest-from-msg-part (3 bytes) (since 3.2)
     *     contiguous-from-msg-id (3 bytes) (since 3.2)
     *     contiguous-from-msg-part (3bytes) (since 3.2)
     *     preferred-ack-size (2bytes) (since 3.3)
     *     count (2 byte)
     *         from-msg-id (3 bytes)
     *         msg-part (3 bytes)
     * 
     * Note: The *-from-msg-id values are the values that were passed in the
     * Directed, Sequel, or Ack Packets -- the "from" in *-from-msg-id does not
     * refer to the sender of the Ack but the sender of the Packet which the
     * Ack is acknowledging.
     */
    public static final int TYPE_ACK = 232718545;
    
    /**
     * Property TYPE_BROADCAST
     *
     * A Broadcast Packet is sent to all the Members (and non-Members) of the
     * network and is not confirmed. The sender id will be zero if the
     * Broadcast Packet is coming from a source that is attempting to become a
     * Member. The recipient is always the "this member", and the destination
     * service id is always zero.
     * 
     * value=0x0DDF00D2
     * 
     * Binary format:
     *     packet-type TYPE_BROADCAST (4 bytes)
     *     cluster-name - UTF formatted
     *     from-id (2 bytes) (may be zero)
     *     msg-type (2 bytes)
     *     payload-length (2 bytes)
     *     payload-bytes (n bytes)
     */
    public static final int TYPE_BROADCAST = 232718546;
    
    /**
     * Property TYPE_DIAGNOSTIC
     *
     * A Diagnostic Packet is out-of-band communication with respect to normal
     * TCMP traffic.  It is used to test if the network is working.  Assuming
     * its embedded TTL is > 0 the response is another TYPE_DIAGNOSTIC packet
     * with a decremented TTL.
     * 
     * value=0x0DDF00D0
     * 
     * Binary format:
     *     packet-type TYPE_DIAGNOSTIC (4 bytes)
     *     to-id (2 bytes)
     *     from-id (2 bytes)
     *     ttl (1 byte)
     *     ldtSent (4 bytes)
     * 
     * @since 3.5.4
     */
    public static final int TYPE_DIAGNOSTIC = 232718544;
    
    /**
     * Property TYPE_DIRECTED_FEW
     *
     * This Directed Packet is sent to a few explicitly specified Members of
     * the network; a Directed Packet requires confirmation.
     * 
     * PacketIndex (Message part) is zero.
     * 
     * value=0x0DDF00D3
     * 
     * Binary format:
     *     packet-type TYPE_DIRECTED_FEW (4 bytes)
     *     count (1 byte)
     *         to-id (2 bytes)
     *     [implied count (0 bytes)]
     *         to-msg-id (3 bytes)
     *     from-id (2 bytes)
     *     from-msg-id (3 bytes)
     *     msg-part-count (3 bytes)
     *     service-id (2 bytes)
     *     msg-type (2 bytes)
     *     payload-length (2 bytes)
     *     payload-bytes (n bytes)
     */
    public static final int TYPE_DIRECTED_FEW = 232718547;
    
    /**
     * Property TYPE_DIRECTED_MANY
     *
     * This Directed Packet is sent to many explicitly specified Members of the
     * network; a Directed Packet requires confirmation.
     * 
     * PacketIndex (Message part) is zero.
     * 
     * value=0x0DDF00D4
     * 
     * Binary format:
     *     packet-type TYPE_DIRECTED_MANY (4 bytes)
     *     bits-count (1 byte)
     *         bits (4 bytes) --> note: implies member-count
     *     member-count (2 bytes)
     *         to-msg-id (3 bytes)
     *     from-id (2 bytes)
     *     from-msg-id (3 bytes)
     *     msg-part-count (3 bytes)
     *     service-id (2 bytes)
     *     msg-type (2 bytes)
     *     payload-length (2 bytes)
     *     payload-bytes (n bytes)
     * 
     * (limited to 255 recipients by member-count)
     */
    public static final int TYPE_DIRECTED_MANY = 232718548;
    
    /**
     * Property TYPE_DIRECTED_ONE
     *
     * This Directed Packet is sent to one explicitly specified Member of the
     * network; a Directed Packet requires confirmation.
     * 
     * PacketIndex (Message part) is zero.
     * 
     * value=0x0DDF00D5
     * 
     * Binary format:
     *     packet-type TYPE_DIRECTED_ONE (4 bytes)
     *     to-id (2 bytes)
     *     to-msg-id (3 bytes)
     *     from-id (2 bytes)
     *     from-msg-id (3 bytes)
     *     msg-part-count (3 bytes)
     *     service-id (2 bytes)
     *     msg-type (2 bytes)
     *     payload-length (2 bytes)
     *     payload-bytes (n bytes)
     */
    public static final int TYPE_DIRECTED_ONE = 232718549;
    
    /**
     * Property TYPE_NAME_SERVICE
     *
     * A NameService Packet is sent to issue or respond to a NameService
     * request.
     * 
     * Note: there is no corresponding component for this packet type, it is
     * handled in-line in Cluster#onNameServicePacket
     * 
     * value=0x0DDF00DA
     * 
     * Binary format:
     *     packet-type TYPE_NAME_SERVICE (4 bytes)
     *     cluster-name (UTF formatted)
     *     attempt-count (1 byte) - 0 for replies
     *     attempt-limit (1 byte) - 0 for replies
     *     return-addr-byte-count (1 byte)
     *     return-address (4 | 16 bytes)
     *     return-port (4 bytes)
     *     name (UTF formatted)
     *     value-byte-count (4 bytes)
     *     value (Binary) (optionally the Member on lookups)
     */
    public static final int TYPE_NAME_SERVICE = 232718554;
    
    /**
     * Property TYPE_REQUEST
     *
     * A Request Packet is sent to request a missing or damaged Packet to be
     * re-sent.
     * 
     * value=0x0DDF00D6
     * 
     * Binary format:
     *     packet-type TYPE_REQUEST (4 bytes)
     *     to-id (2 bytes)
     *     from-id (2 bytes)
     *     count (2 byte)
     *         from-msg-id (3 bytes)
     *         msg-part (3 bytes)
     * 
     * Note: The from-msg-id value is the from-msg-id that will be passed in
     * the Directed or Sequel Packet -- the "from" in from-msg-id does not
     * refer to the sender of the Request itself but rather the sender of the
     * Packet which the Request is requesting.
     */
    public static final int TYPE_REQUEST = 232718550;
    
    /**
     * Property TYPE_SEQUEL_FEW
     *
     * Sequel Packets are sent after a Directed Packet to convey the remainder
     * of a Message that does not fit into a Directed Packet. A Sequel Packet
     * requires confirmation.
     * 
     * value=0x0DDF00D7
     * 
     * Binary format:
     *     packet-type TYPE_SEQUEL_FEW (4 bytes)
     *     count (1 byte)
     *         to-id (2 bytes)
     *     from-id (2 bytes)
     *     from-msg-id (3 bytes)
     *     msg-part (3 bytes)
     *     payload-length (2 bytes)
     *     payload-bytes (n bytes)
     */
    public static final int TYPE_SEQUEL_FEW = 232718551;
    
    /**
     * Property TYPE_SEQUEL_MANY
     *
     * Sequel Packets are sent after a Directed Packet to convey the remainder
     * of a Message that does not fit into a Directed Packet. A Sequel Packet
     * requires confirmation.
     * 
     * value=0x0DDF00D8
     * 
     * Binary format:
     *     packet-type TYPE_SEQUEL_MANY (4 bytes)
     *     bits-count (1 byte)
     *         bits (4 bytes)
     *     from-id (2 bytes)
     *     from-msg-id (3 bytes)
     *     msg-part (3 bytes)
     *     payload-length (2 bytes)
     *     payload-bytes (n bytes)
     */
    public static final int TYPE_SEQUEL_MANY = 232718552;
    
    /**
     * Property TYPE_SEQUEL_ONE
     *
     * Sequel Packets are sent after a Directed Packet to convey the remainder
     * of a Message that does not fit into a Directed Packet. A Sequel Packet
     * requires confirmation.
     * 
     * value=0x0DDF00D9
     * 
     * Binary format:
     *     packet-type TYPE_SEQUEL_ONE (4 bytes)
     *     to-id (2 bytes)
     *     from-id (2 bytes)
     *     from-msg-id (3 bytes)
     *     msg-part (3 bytes)
     *     payload-length (2 bytes)
     *     payload-bytes (n bytes)
     */
    public static final int TYPE_SEQUEL_ONE = 232718553;
    
    /**
     * Property TYPE_TEST_MULTICAST
     *
     * A multicast test packet is generated by the
     * com.tangosol.net.MulticastTest utility.  Though not part of Coherence
     * this is known type of traffic, and not detrimental to the cluster.
     * 
     * The value is ASCII 'test'.
     */
    public static final int TYPE_TEST_MULTICAST = 1952805748;
    
    // Initializing constructor
    public Packet(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
        {
        super(sName, compParent, false);
        }
    
    // Private initializer
    protected void __initPrivate()
        {
        
        super.__initPrivate();
        }
    
    // Getter for virtual constant ConfirmationRequired
    public boolean isConfirmationRequired()
        {
        return false;
        }
    
    // Getter for virtual constant OutgoingBroadcast
    public boolean isOutgoingBroadcast()
        {
        return false;
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
            clz = Class.forName("com.tangosol.coherence/component/net/Packet".replace('/', '.'));
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
     * Skip the specified number of bytes in the supplied BufferInput
    * 
    * @param input  the buffer input to operate upon
    * @param cb       the number of bytes to skip
    * 
    * @throws EOFException less bytes are skipped because the buffer's content
    * was exhausted
    * @throws IOException if less bytes were skipped for any other reason
     */
    public static void ensureSkipBytes(com.tangosol.io.ReadBuffer.BufferInput input, int cb)
            throws java.io.EOFException,
                   java.io.IOException
        {
        // import java.io.EOFException;
        // import java.io.IOException;
        
        if (input.skipBytes(cb) != cb)
            {
            throw input.available() == 0
                ? new EOFException()
                : new IOException("failed to skip " + cb + " bytes");
            }
        }
    
    /**
     * Extracts Packet(s) from a Buffer. If multiple packets exists they will be
    * the owner of their own buffer allocated from the BufferManager.
    * 
    * @param addrSrc        the source address (for broadcasts)
    * @param buffer           the buffer from which to read the Packet(s)
    * @param mgr              the BufferManager from which to allocated new
    * buffers if the packet contains multiple packets
    * @param aPacket         a preallocated array to use unless it is not of
    * sufficient size, per the general contract of Collection.toArray
    * @param nMemberId  the id of the Member receiving the Packet
    * 
    * @return the Packet(s) in an array
     */
    public static Packet[] extract(java.net.SocketAddress addrSrc, java.nio.ByteBuffer buffer, com.oracle.coherence.common.io.BufferManager mgr, Packet[] aPacket, int nMemberId)
            throws java.io.IOException
        {
        // import Component.Net.Packet.MessagePacket;
        // import Component.Net.Packet.MessagePacket.Broadcast;
        // import com.tangosol.io.ReadBuffer;
        // import com.tangosol.io.nio.ByteBufferReadBuffer;
        // import com.tangosol.io.ReadBuffer$BufferInput as com.tangosol.io.ReadBuffer.BufferInput;
        // import java.net.InetSocketAddress;
        // import java.nio.ByteBuffer;
        // import java.util.ArrayList;
        
        ReadBuffer  bufferIn   = new ByteBufferReadBuffer(buffer);
        com.tangosol.io.ReadBuffer.BufferInput input      = bufferIn.getBufferInput();
        int         cPacket    = 0;
        ArrayList   listPacket = null;
        boolean     fOwned     = false;
        int         cSlots     = aPacket == null ? 0 : aPacket.length;
        
        try
            {
            do
                {
                // determine if the Packet is intended for this Member
                input.mark(0xF00);
                if (isForMember(input, nMemberId))
                    {
                    // create a Packet component from the next buffer segment
                    input.reset();
                    Packet packet = Packet.instantiate(input, nMemberId);
        
                    // record the packet into the result
                    if (cPacket < cSlots) // common case
                        {
                        aPacket[cPacket] = packet;  
                        }
                    else if (cPacket == cSlots)
                        {
                        listPacket = new ArrayList(cSlots << 1);
                        for (int i = 0; i < cPacket; i++)
                            {
                            listPacket.add(aPacket[i]);
                            }
                        listPacket.add(packet);
                        }
                   else
                        {
                        listPacket.add(packet);
                        }
                    ++cPacket;
        
                    // determine the buffer owner
                    if (packet instanceof MessagePacket)
                        {
                        if (addrSrc != null && packet instanceof Broadcast)
                            {
                            ((Broadcast) packet).setFromAddress((InetSocketAddress) addrSrc);
                            }
        
                        MessagePacket msgPacket = (MessagePacket) packet;
                        int           cb        = msgPacket.getBodyLength();
        
                        // The body starts at the current offset 
                        buffer.position(input.getOffset());
        
                        if (buffer.remaining() > cb)
                            {
                            // duplicate the buffer and assign the limit to the end of the
                            // body
                            ByteBuffer bufDup = (ByteBuffer) buffer
                                    .duplicate()
                                    .limit(buffer.position() + cb);
        
                            // acquire a new buffer with appropriate length and copy
                            // the body from the duplicate buffer
                            msgPacket.setByteBuffer((ByteBuffer) mgr.acquire(cb)
                                   .put(bufDup)
                                   .flip());
        
                            ensureSkipBytes(input, cb);
                            }
                        else // common case
                            {
                            // last packet, is the owner of the original buffer
                            msgPacket.setByteBuffer(buffer);
                            fOwned = true;
                            break;
                            }
                        }
                     // else; non-MessagePacket's don't retain buffers
                     }
                else // not for this member
                    {
                    // skip over the remainder of the packet required for bundles
                    // which include packets not for this member
                    input.reset();
                    skip(input);
                    }
                }
            while (input.available() > 0);
            }
        finally
            {
            if (!fOwned)
                {
                // the original buffer was not "taken" by any MessagePacket; release now
                mgr.release(buffer);
                }
            }
        
        if (aPacket.length > cPacket)
            {
            aPacket[cPacket] = null; // terminate the array
            }
        
        return listPacket == null
            ? aPacket
            : (Packet[]) listPacket.toArray(new Packet[cPacket]);
        }
    
    // Accessor for the property "Description"
    /**
     * Getter for property Description.<p>
    * Human-readable description of attributes added to the sub-class of
    * Packet; used by toString for debugging purposes.
     */
    public String getDescription()
        {
        return null;
        }
    
    // Accessor for the property "FromId"
    /**
     * Getter for property FromId.<p>
    * This is the sender's Member id. The id may be zero only for Broadcast
    * Packets, which are used before an id is obtained.
     */
    public int getFromId()
        {
        return __m_FromId;
        }
    
    // Accessor for the property "Length"
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
        return 0;
        }
    
    // Accessor for the property "PacketType"
    /**
     * Getter for property PacketType.<p>
    * Specifies the persistence layout of the Packet. This value is available
    * only for received Packets. See the enumeration of type values on the
    * Packet component that begin with "TYPE_".
     */
    public int getPacketType()
        {
        return __m_PacketType;
        }
    
    // Accessor for the property "ReceivedMillis"
    /**
     * Getter for property ReceivedMillis.<p>
    * Date/time value when the Packet was received.
     */
    public long getReceivedMillis()
        {
        return __m_ReceivedMillis;
        }
    
    // Accessor for the property "SentCount"
    /**
     * Getter for property SentCount.<p>
    * The number of times an outgoing packet has been sent.
     */
    public int getSentCount()
        {
        return __m_SentCount;
        }
    
    // Accessor for the property "SentMillis"
    /**
     * Getter for property SentMillis.<p>
    * Date/time value when the Packet was sent. For Packets that can be resent,
    * this is the most recent date/time value that the Packet was sent.
     */
    public long getSentMillis()
        {
        return __m_SentMillis;
        }
    
    // Accessor for the property "ToId"
    /**
     * Getter for property ToId.<p>
    * This is the receiver's Member id. This property is valid for all Packets
    * except outgoing Directed and Sequel Packets that correspond to Messages
    * with multiple recipient Members.
     */
    public int getToId()
        {
        return __m_ToId;
        }
    
    /**
     * Instantiate a Packet from a BufferInput.
    * 
    * @param input  the BufferInput from which to read the Packet
    * @param nMemberId  the id of the Member receiving the Packet
    * 
    * @return the new Packet object
     */
    public static Packet instantiate(com.tangosol.io.ReadBuffer.BufferInput input, int nMemberId)
            throws java.io.IOException
        {
        // import Component.Net.Packet.DiagnosticPacket;
        // import Component.Net.Packet.NotifyPacket.Ack;
        // import Component.Net.Packet.MessagePacket.Broadcast;
        // import Component.Net.Packet.MessagePacket.Directed;
        // import Component.Net.Packet.MessagePacket.Sequel;
        // import Component.Net.Packet.NotifyPacket.Request;
        // import com.tangosol.util.Base;
        // import java.io.IOException;
        
        Packet packet;
        int    nType = input.readInt();
        
        switch (nType)
            {
            case TYPE_BROADCAST:
                packet = new Broadcast();
                break;
            
            case TYPE_DIRECTED_ONE:
            case TYPE_DIRECTED_FEW:
            case TYPE_DIRECTED_MANY:
                packet = new Directed();
                break;
        
            case TYPE_SEQUEL_ONE:
            case TYPE_SEQUEL_FEW:
            case TYPE_SEQUEL_MANY:
                packet = new Sequel();
                break;
        
            case TYPE_REQUEST:
                packet = new Request();
                break;
        
            case TYPE_ACK:
                packet = new Ack();
                break;
        
            case TYPE_DIAGNOSTIC:
                packet = new DiagnosticPacket();
                break;
        
            default:
                throw new IOException("unknown packet type: " + nType);
            }
        
        // configure the packet
        packet.setPacketType(nType);
        if (nType != TYPE_BROADCAST)
            {
            packet.setToId(nMemberId);
            }
        
        // read the rest of the packet information
        packet.read(input, nMemberId);
        
        // mark the packet as incoming
        packet.setReceivedMillis(Base.getSafeTimeMillis());
        
        return packet;
        }
    
    /**
     * Check if the packet is still addressed to the specified member Id.  Once
    * the packet has been ack'd by a member this will return false.
     */
    public boolean isAddressedTo(int nMemberId)
        {
        return getToId() == nMemberId;
        }
    
    // Accessor for the property "Deferrable"
    /**
     * Getter for property Deferrable.<p>
    * Indicates if the packet is eligible for deferred sending.
     */
    public boolean isDeferrable()
        {
        return isConfirmationRequired() && !isOutgoingMultipoint();
        }
    
    /**
     * Determine if the BufferInput contains a Coherence packet.
    * 
    * @param input  the BufferInput from which to read the Packet
    * 
    * @return true if the Packet is a Coherence packet.
     */
    public static boolean isForCoherence(com.tangosol.io.ReadBuffer.BufferInput input)
            throws java.io.IOException
        {
        if (input.available() >= 4)
            {
            switch (input.readInt())
                {
                case TYPE_BROADCAST:
                case TYPE_DIRECTED_ONE:
                case TYPE_SEQUEL_ONE:
                case TYPE_REQUEST:
                case TYPE_ACK:
                case TYPE_DIRECTED_FEW:
                case TYPE_SEQUEL_FEW:
                case TYPE_DIRECTED_MANY:
                case TYPE_SEQUEL_MANY:
                case TYPE_NAME_SERVICE:
                case TYPE_TEST_MULTICAST:
                    return true;
                }
            }
        
        return false;
        }
    
    /**
     * Determine if the Packet in the passed BufferInput if for a specified
    * Member to receive.
    * 
    * @param input  the BufferInput from which to read the Packet
    * @param nMemberId  the id of the Member receiving the Packet
    * 
    * @return true if the Packet is for the specified Member, otherwise false
     */
    public static boolean isForMember(com.tangosol.io.ReadBuffer.BufferInput input, int nMemberId)
            throws java.io.IOException
        {
        // import java.io.EOFException;
        // import java.io.IOException;
        
        try
            {
            int nType = input.readInt();
        
            if (nMemberId == 0)
                {
                return nType == TYPE_BROADCAST;
                }
        
            switch (nType)
                {
                case TYPE_BROADCAST:
                    // broadcast is for everyone; even receive broadcast
                    // Messages that appear to be from this Member just
                    // in case someone else has the same Member id (e.g.
                    // two Members with id==1)
                    return true;
                    // return (input.readUnsignedShort() != nMemberId);
        
                case TYPE_DIRECTED_ONE:
                case TYPE_SEQUEL_ONE:
                case TYPE_REQUEST:
                case TYPE_ACK:
                case TYPE_DIAGNOSTIC:
                    return (input.readUnsignedShort() == nMemberId);
        
                case TYPE_DIRECTED_FEW:
                case TYPE_SEQUEL_FEW:
                    for (int i = 0, c = input.readUnsignedByte(); i < c; ++i)
                        {
                        if (input.readUnsignedShort() == nMemberId)
                            {
                            return true;
                            }
                        }
                    return false;
        
                case TYPE_DIRECTED_MANY:
                case TYPE_SEQUEL_MANY:
                    int of = Member.calcByteOffset(nMemberId);
                    int c  = input.readUnsignedByte();
                    if (of >= c)
                        {
                        return false;
                        }
                    if (of > 0)
                        {
                        int cbSkip = of << 2;
                        if (input.skipBytes(cbSkip) != cbSkip)
                            {
                            throw new IOException("skip bytes failed!");
                            }
                        }
                    int nBits = input.readInt();
                    int nMask = Member.calcByteMask(nMemberId);
                    return (nBits & nMask) != 0;
        
                default:
                    return false;
                }
            }
        catch (EOFException e)
            {
            // this can legally happen if this part of a bundle which was multicast and not addressed to this member
            // specifically if the addressed members had a larger preferred packet length then this member
            return false;
            }
        }
    
    // Accessor for the property "Incoming"
    /**
     * Getter for property Incoming.<p>
    * True if the Packet is an in-coming Packet (was received from the
    * network).
    * 
    * False if the Packet is an out-going Packet (to be sent, or was sent,
    * etc.).
     */
    public boolean isIncoming()
        {
        return getReceivedMillis() != 0L;
        }
    
    // Accessor for the property "Outgoing"
    /**
     * Getter for property Outgoing.<p>
    * True if the Packet is an out-going Packet (to be sent, or was sent,
    * etc.).
    * 
    * False if the Packet is an in-coming Packet (was received from the
    * network).
     */
    public boolean isOutgoing()
        {
        return getReceivedMillis() == 0L;
        }
    
    // Accessor for the property "OutgoingMultipoint"
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
        return false;
        }
    
    /**
     * Convert a long integer to a trint.
    * 
    * @param l  the long value to convert to a trint
    * 
    * @return  the equivalent unsigned 3-byte integer value (a "trint")
     */
    public static int makeTrint(long l)
        {
        return (int) (l & 0xFFFFFFL);
        }
    
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
        }
    
    /**
     * Read an unsigned three-byte integer value from a BufferInput.
    * 
    * @param input  to read from
    * 
    * @return a three-byte unsigned integer value as an int
     */
    public static int readUnsignedTrint(com.tangosol.io.ReadBuffer.BufferInput input)
            throws java.io.IOException
        {
        byte[] ab = new byte[3];
        
        input.readFully(ab);
        
        return  ((int) ab[0] & 0xFF) << 16
                    | ((int) ab[1] & 0xFF) << 8
                    | ((int) ab[2] & 0xFF) << 0;
        }
    
    // Accessor for the property "FromId"
    /**
     * Setter for property FromId.<p>
    * This is the sender's Member id. The id may be zero only for Broadcast
    * Packets, which are used before an id is obtained.
     */
    public void setFromId(int nId)
        {
        _assert(!isIncoming());
        __m_FromId = (nId);
        }
    
    // Accessor for the property "PacketType"
    /**
     * Setter for property PacketType.<p>
    * Specifies the persistence layout of the Packet. This value is available
    * only for received Packets. See the enumeration of type values on the
    * Packet component that begin with "TYPE_".
     */
    protected void setPacketType(int nType)
        {
        __m_PacketType = nType;
        }
    
    // Accessor for the property "ReceivedMillis"
    /**
     * Setter for property ReceivedMillis.<p>
    * Date/time value when the Packet was received.
     */
    protected void setReceivedMillis(long cMillis)
        {
        __m_ReceivedMillis = cMillis;
        }
    
    // Accessor for the property "SentCount"
    /**
     * Setter for property SentCount.<p>
    * The number of times an outgoing packet has been sent.
     */
    public void setSentCount(int cSent)
        {
        __m_SentCount = cSent;
        }
    
    // Accessor for the property "SentMillis"
    /**
     * Setter for property SentMillis.<p>
    * Date/time value when the Packet was sent. For Packets that can be resent,
    * this is the most recent date/time value that the Packet was sent.
     */
    public void setSentMillis(long cMillis)
        {
        __m_SentMillis = cMillis;
        }
    
    // Accessor for the property "ToId"
    /**
     * Setter for property ToId.<p>
    * This is the receiver's Member id. This property is valid for all Packets
    * except outgoing Directed and Sequel Packets that correspond to Messages
    * with multiple recipient Members.
     */
    public void setToId(int nId)
        {
        _assert(!isIncoming());
        __m_ToId = (nId);
        }
    
    /**
     * Skip a Packet from a BufferInput.
    * 
    * @param input  the BufferInput from which to read the Packet
     */
    public static void skip(com.tangosol.io.ReadBuffer.BufferInput input)
            throws java.io.IOException
        {
        // import Component.Net.Packet.DiagnosticPacket;
        // import Component.Net.Packet.NotifyPacket.Ack;
        // import Component.Net.Packet.MessagePacket.Broadcast;
        // import Component.Net.Packet.MessagePacket.Directed;
        // import Component.Net.Packet.MessagePacket.Sequel;
        // import Component.Net.Packet.NotifyPacket.Request;
        // import java.io.EOFException;
        // import java.io.IOException;
        
        try
            {
            int nType = input.readInt();
            switch (nType)
                {
                case TYPE_BROADCAST:
                    Broadcast.skip(input, nType);
                    break;
        
                case TYPE_DIRECTED_ONE:
                case TYPE_DIRECTED_FEW:
                case TYPE_DIRECTED_MANY:
                    Directed.skip(input, nType);
                    break;
        
                case TYPE_SEQUEL_ONE:
                case TYPE_SEQUEL_FEW:
                case TYPE_SEQUEL_MANY:
                    Sequel.skip(input, nType);
                    break;
        
                case TYPE_REQUEST:
                    Request.skip(input, nType);
                    break;
        
                case TYPE_ACK:
                    Ack.skip(input, nType);
                    break;
        
                case TYPE_DIAGNOSTIC:
                    DiagnosticPacket.skip(input, nType);
                    break;
        
                case TYPE_TEST_MULTICAST:
                    input.skip(input.available());
                    break;
        
                default:
                    throw new IOException("unknown packet type: " + nType);
                }
            }
        catch (EOFException e)
            {
            // wasn't for this node; safe to ignore
            // this can happen if the sender is multicasting to a set of members with
            // a larger preferred packet length then this member
        
            // COH-11224 need to skip the available bytes
            input.skip(input.available());
            }
        }
    
    /**
     * Skip the Packet (not counting the Packet type id) from a BufferInput.
    * 
    * @param input  the BufferInput to read from
    * @param nType the packet type
     */
    public static void skip(com.tangosol.io.ReadBuffer.BufferInput input, int nType)
            throws java.io.IOException
        {
        throw new UnsupportedOperationException();
        }
    
    // Declared at the super level
    public String toString()
        {
        // import com.tangosol.util.Base;
        // import java.sql.Time;
        
        StringBuffer sb = new StringBuffer();
        
        sb.append(get_Name())
          .append("{PacketType=0x")
          .append(Base.toHexString(getPacketType(), 8))
          .append(", ToId=")
          .append(getToId())
          .append(", FromId=")
          .append(getFromId())
          .append(", Direction=");
        
        long lTime;
        if (isIncoming())
            {
            sb.append("Incoming, ReceivedMillis=");
            lTime = getReceivedMillis();
            }
        else
            {
            sb.append("Outgoing, SentCount=");
            sb.append(getSentCount());
            sb.append(", SentMillis=");
            lTime = getSentMillis();
            }
        
        if (lTime == 0)
            {
            sb.append("none");
            }
        else
            {
            sb.append(new Time(lTime));
            sb.append('.');
            sb.append(lTime % 1000);
            }
        
        String sDesc = getDescription();
        if (sDesc != null && sDesc.length() > 0)
            {
            sb.append(", ")
              .append(sDesc);
            }
        
        sb.append('}');
        return sb.toString();
        }
    
    /**
     * Convert a three-byte unsigned integer ("trint") to a long value. This
    * guesses what the long value should be based on its proximity to the
    * passed "current" long value.
    * 
    * @param nTrint  the unsigned 3-byte integer value (a "trint")
    * @param lCurrent  the signed 8-byte integer value that the trint will be
    * translated based on ("close to")
    * 
    * @return the long value represented by the trint
     */
    public static long translateTrint(int nTrint, long lCurrent)
        {
        long lLo = lCurrent - TRINT_MAX_VARIANCE;
        long lHi = lCurrent + TRINT_MAX_VARIANCE;
        
        // @since Coherence 2.2
        // only use the known trint hexits; this bullet-proofs against
        // accidental multiple "translate" calls, and against the "hack"
        // that bit-ors the poll trints with TRINT_DOMAIN_SPAN (that
        // forces them to be non-zero trints even when they wrap around)
        nTrint &= TRINT_MAX_VALUE;
        
        long lBase = lCurrent >>> 24;
        for (int i = -1; i <= 1; ++i)
            {
            long lGuess = ((lBase + i) << 24) | nTrint;
            if (lGuess >= lLo && lGuess <= lHi)
                {
                // @since Coherence 2.2
                // 1) disallow negative trints because they are used as indexes
                // 2) disallow zero value trints because all windowed arrays
                //    for which trints are translated start at 1
                if (lGuess < 1L)
                    {
                    // there is only one acceptable case in which the value is
                    // negative, and that is when the current is unknown, which
                    // implies that it is zero (although we will also accept
                    // one just in case the current was primed like the windowed
                    // arrays are) ... since packets can come out-of-order, the
                    // assertion allows for some "slop"
                    if (lCurrent > 0x800L)
                        {
                        // This can happen if there is an extended duration between packets
                        // from the transmitting node, which its FromMessageId may naturally move
                        // well beyond the window size.  In this case we will not translate the
                        // trint into the same value which the transmitter used, but it will still
                        // be unique from our perspective, which is all that is required.
                        // COH-767 downgraded this from an exception to a debug message
                        _trace("Large gap while initializing packet translation; "
                               + "current=" + lCurrent + " packet=" + nTrint + " value="
                               + lGuess, 6);
                        }
                    lGuess += TRINT_DOMAIN_SPAN;
                    _assert(lGuess >= 1L);
                    }
        
                return lGuess;
                }
            }
        
        throw new IllegalStateException("translateTrint failed: "
                + "nTrint=" + nTrint + ", lCurrent=" + lCurrent);
        }
    
    /**
     * Write the Packet to a Stream.
    * 
    * @param stream  the DataOutputStream to write to
    * @param nMemberId  if non-zero, it indicates that the Packet should be
    * addressed only to one member
     */
    public void write(com.tangosol.io.WriteBuffer.BufferOutput output)
            throws java.io.IOException
        {
        }
    
    /**
     * Write a three-byte integer value to a BufferOutput.
    * 
    * @param output  to write to
    * @param n  a three-byte unsigned integer value passed as an int
     */
    public static void writeTrint(com.tangosol.io.WriteBuffer.BufferOutput output, int n)
            throws java.io.IOException
        {
        output.write(new byte[] {(byte) (n >>> 16), (byte) (n >>> 8),
            (byte) n});
        }
    
    /**
     * Write a three-byte integer value to a BufferOutput.
    * 
    * @param output  to write to
    * @param l  a three-byte unsigned integer value passed as a long
     */
    public static void writeTrint(com.tangosol.io.WriteBuffer.BufferOutput output, long l)
            throws java.io.IOException
        {
        writeTrint(output, (int) (l & 0xFFFFFFL));
        }
    }
