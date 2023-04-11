
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.message.DiscoveryMessage

package com.tangosol.coherence.component.net.message;

import com.tangosol.coherence.component.net.Member;
import com.tangosol.coherence.component.net.packet.messagePacket.Broadcast;
import com.tangosol.io.ReadBuffer;
import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * A base component for all [broadcast] messages used by the cluster discovery
 * protocol.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class DiscoveryMessage
        extends    com.tangosol.coherence.component.net.Message
    {
    // ---- Fields declarations ----
    
    /**
     * Property ExternalAddress
     *
     * The external address (if any) that the sender believes may be associated
     * with the recipient.  This can be used in NAT based configurations for
     * the recipient to learn its external NAT address without having to
     * explicitly configure it.
     * 
     * @since 12.2.3
     */
    private java.net.InetSocketAddress __m_ExternalAddress;
    
    /**
     * Property ReadError
     *
     * Indicates if an error occurred while reading the message.  A read error
     * may be treated as a protocol version mismatch.
     */
    private transient boolean __m_ReadError;
    
    /**
     * Property SourceAddress
     *
     * The source address for incomming packets.
     */
    private java.net.SocketAddress __m_SourceAddress;
    
    /**
     * Property ToMember
     *
     * The Member to which the DiscoveryMessage will be sent. May be null if
     * the addressee is not yet known. This could also be a temporary Member
     * object with an Id value of zero.
     */
    private com.tangosol.coherence.component.net.Member __m_ToMember;
    
    // Default constructor
    public DiscoveryMessage()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public DiscoveryMessage(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
        return new com.tangosol.coherence.component.net.message.DiscoveryMessage();
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
            clz = Class.forName("com.tangosol.coherence/component/net/message/DiscoveryMessage".replace('/', '.'));
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
     * Test that the end of the input stream has been reached.  If the EOS has
    * not been reached the readError flag is set.  Note this method may attempt
    * to read data from the stream, so calling it on a stream which may still
    * be used requires marking the stream position ahead of time.
     */
    public void ensureEOS(java.io.DataInput stream)
            throws java.io.IOException
        {
        // import java.io.IOException;
        
        // determine if any additional bytes are available from the stream;
        // skipBytes will not throw if it reaches the end of the stream, so
        // we are exception safe
        if (stream.skipBytes(1) == 0)
            {
            // EOS has been reached
            return;
            }
        
        // if we got here then the message contains more data then expected
        throw new IOException("message contains more data then expected");
        }
    
    // Accessor for the property "ClusterName"
    /**
     * Getter for property ClusterName.<p>
    * The name of the cluster from which this message is sent.
     */
    public String getClusterName()
        {
        return getFromMember().getClusterName();
        }
    
    // Accessor for the property "ExternalAddress"
    /**
     * Getter for property ExternalAddress.<p>
    * The external address (if any) that the sender believes may be associated
    * with the recipient.  This can be used in NAT based configurations for the
    * recipient to learn its external NAT address without having to explicitly
    * configure it.
    * 
    * @since 12.2.3
     */
    public java.net.InetSocketAddress getExternalAddress()
        {
        return __m_ExternalAddress;
        }
    
    // Accessor for the property "SourceAddress"
    /**
     * Getter for property SourceAddress.<p>
    * The source address for incomming packets.
     */
    public java.net.SocketAddress getSourceAddress()
        {
        return __m_SourceAddress;
        }
    
    // Accessor for the property "ToMember"
    /**
     * Getter for property ToMember.<p>
    * The Member to which the DiscoveryMessage will be sent. May be null if the
    * addressee is not yet known. This could also be a temporary Member object
    * with an Id value of zero.
     */
    public com.tangosol.coherence.component.net.Member getToMember()
        {
        return __m_ToMember;
        }
    
    // Accessor for the property "ReadError"
    /**
     * Getter for property ReadError.<p>
    * Indicates if an error occurred while reading the message.  A read error
    * may be treated as a protocol version mismatch.
     */
    public boolean isReadError()
        {
        return __m_ReadError;
        }
    
    // Declared at the super level
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
        // import Component.Net.Member;
        // import Component.Net.Packet.MessagePacket.Broadcast;
        // import com.tangosol.io.ReadBuffer;
        // import java.net.InetSocketAddress;
        
        if (getToMemberSet() == null) // broadcast packet
            {
            // create outgoing Broadcast Packet
            Broadcast packet  = new Broadcast();
            packet.setClusterNameBuffer(publisher.getClusterNameBuffer());
        
            int cbMsg  = buffer.length();
            int cbBody = Broadcast.calcBodyLength(
                            packet.calcHeaderLength(), cbPreferred, cbMax);
        
            if (cbMsg > cbBody)
                {
                throw new IllegalStateException("Broadcast Message is too "
                    + "large (max=" + cbBody + ", actual=" + buffer.length() + ", " + this);
                }
        
            packet.setFromId(getFromMember().getId());
            packet.setMessageType(getMessageType());
        
            // since the broadcast packet cannot be acked, it will never
            // be released back into the appropriate BufferManager. hence
            // we need to allocate and copy the buffer and then release the
            // acquired buffer back to the manager
            packet.defineBufferView((ReadBuffer) buffer.clone(), 0, cbMsg);
            getBufferController().dispose();
            setBufferController(null);
            
            Member            memberTo = getToMember();
            InetSocketAddress addrExt  = getExternalAddress();
            int               cAddr    = (memberTo == null ? 0 : 1) + (addrExt == null ? 0 : 1);
            if (cAddr > 0)
                {
                InetSocketAddress[] aAddr = new InetSocketAddress[cAddr];
        
                if (memberTo != null)
                    {
                    aAddr[0] = memberTo.getSocketAddress();
                    }
        
                if (addrExt != null)
                    {
                    aAddr[aAddr.length - 1] = addrExt;
                    }
                packet.setToAddress(aAddr);
                }
        
            setMessagePartCount(1);
            setPacket(0, packet);
            return true;
            }
        else // directed discovery message; rare but allowed
            {
            return super.packetize(publisher, setMembersBase, buffer, cbPreferred, cbMax);
            }
        }
    
    // Declared at the super level
    public void read(com.tangosol.io.ReadBuffer.BufferInput input)
            throws java.io.IOException
        {
        // import Component.Net.Member;
        
        Member memberFrom = new Member();
        memberFrom.readExternal(input);
        setFromMember(memberFrom);
        
        if (getSourceAddress() == null)
            {
            setSourceAddress(memberFrom.getSocketAddress());
            }
        
        if (input.readBoolean())
            {
            Member memberTo = new Member();
            memberTo.readExternal(input);
            setToMember(memberTo);
            }
        }
    
    // Accessor for the property "ExternalAddress"
    /**
     * Setter for property ExternalAddress.<p>
    * The external address (if any) that the sender believes may be associated
    * with the recipient.  This can be used in NAT based configurations for the
    * recipient to learn its external NAT address without having to explicitly
    * configure it.
    * 
    * @since 12.2.3
     */
    public void setExternalAddress(java.net.InetSocketAddress addressExternal)
        {
        __m_ExternalAddress = addressExternal;
        }
    
    // Declared at the super level
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
        // import Component.Net.Packet.MessagePacket.Broadcast;
        
        super.setPacket(i, packet);
        
        if (packet instanceof Broadcast)
            {
            setSourceAddress(((Broadcast) packet).getFromAddress());
            }
        }
    
    // Accessor for the property "ReadError"
    /**
     * Setter for property ReadError.<p>
    * Indicates if an error occurred while reading the message.  A read error
    * may be treated as a protocol version mismatch.
     */
    public void setReadError(boolean fReadError)
        {
        __m_ReadError = fReadError;
        }
    
    // Accessor for the property "SourceAddress"
    /**
     * Setter for property SourceAddress.<p>
    * The source address for incomming packets.
     */
    public void setSourceAddress(java.net.SocketAddress pSourceAddress)
        {
        __m_SourceAddress = pSourceAddress;
        }
    
    // Accessor for the property "ToMember"
    /**
     * Setter for property ToMember.<p>
    * The Member to which the DiscoveryMessage will be sent. May be null if the
    * addressee is not yet known. This could also be a temporary Member object
    * with an Id value of zero.
     */
    public void setToMember(com.tangosol.coherence.component.net.Member member)
        {
        __m_ToMember = member;
        }
    
    // Declared at the super level
    public void write(com.tangosol.io.WriteBuffer.BufferOutput output)
            throws java.io.IOException
        {
        // import Component.Net.Member;
        
        Member memberFrom = getFromMember();
        Member memberTo   = getToMember();
        
        memberFrom.writeExternal(output);
        
        if (memberTo == null)
            {
            output.writeBoolean(false);
            }
        else
            {
            output.writeBoolean(true);
            memberTo.writeExternal(output);
            }
        }
    }
