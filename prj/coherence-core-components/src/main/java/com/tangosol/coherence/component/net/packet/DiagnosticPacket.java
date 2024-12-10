
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.packet.DiagnosticPacket

package com.tangosol.coherence.component.net.packet;

import java.sql.Time;

/**
 * A DiagnosticPacket is used to test if the underlying communication channel
 * is functional, bypassing most of TCMP tiers.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class DiagnosticPacket
        extends    com.tangosol.coherence.component.net.Packet
    {
    // ---- Fields declarations ----
    
    /**
     * Property PreferredPortUsed
     *
     * Specifies whether the preferred port should be destination of this
     * DiagnosticPacket, otherwise the advertised port is used.
     */
    private boolean __m_PreferredPortUsed;
    
    /**
     * Property TimeToLive
     *
     * If greater then zero, this indicates a request to decrement and
     * retransmit this packet back to its source.  Once zero the ping-pong ends.
     */
    private byte __m_TimeToLive;
    
    // Default constructor
    public DiagnosticPacket()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public DiagnosticPacket(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            setPacketType(232718544);
            setPreferredPortUsed(false);
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
        return new com.tangosol.coherence.component.net.packet.DiagnosticPacket();
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
            clz = Class.forName("com.tangosol.coherence/component/net/packet/DiagnosticPacket".replace('/', '.'));
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
     * Getter for property Description.<p>
    * Human-readable description of attributes added to the sub-class of
    * Packet; used by toString for debugging purposes.
     */
    public String getDescription()
        {
        // import java.sql.Time;
        
        StringBuffer sb = new StringBuffer();
        
        if (isIncoming())
            {
            long lTime = getSentMillis();
            sb.append("SentMillis=")
              .append(new Time(lTime))
              .append('.')
              .append(lTime % 1000)
              .append(", ");
            }
        
        return sb.append("PreferredPort=").append(isPreferredPortUsed())
                 .append(", ")
                 .append("TTL=").append(getTimeToLive()).toString();
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
        return 4  // int packet type
              + 2 // short to id
              + 2 // short from id
              + 1 // byte ttl
              + 8 // long sent millis
              + 1;// boolean preferred port used
        }
    
    // Accessor for the property "TimeToLive"
    /**
     * Getter for property TimeToLive.<p>
    * If greater then zero, this indicates a request to decrement and
    * retransmit this packet back to its source.  Once zero the ping-pong ends.
     */
    public byte getTimeToLive()
        {
        return __m_TimeToLive;
        }
    
    // Accessor for the property "PreferredPortUsed"
    /**
     * Getter for property PreferredPortUsed.<p>
    * Specifies whether the preferred port should be destination of this
    * DiagnosticPacket, otherwise the advertised port is used.
     */
    public boolean isPreferredPortUsed()
        {
        return __m_PreferredPortUsed;
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
        setToId(input.readUnsignedShort());
        setFromId(input.readUnsignedShort());
        setTimeToLive(input.readByte());
        setSentMillis(input.readLong());
        setPreferredPortUsed(input.readBoolean());
        }
    
    // Accessor for the property "PreferredPortUsed"
    /**
     * Setter for property PreferredPortUsed.<p>
    * Specifies whether the preferred port should be destination of this
    * DiagnosticPacket, otherwise the advertised port is used.
     */
    public void setPreferredPortUsed(boolean fPreferredPortUsed)
        {
        __m_PreferredPortUsed = fPreferredPortUsed;
        }
    
    // Accessor for the property "TimeToLive"
    /**
     * Setter for property TimeToLive.<p>
    * If greater then zero, this indicates a request to decrement and
    * retransmit this packet back to its source.  Once zero the ping-pong ends.
     */
    public void setTimeToLive(byte cTTL)
        {
        __m_TimeToLive = cTTL;
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
        input.readByte(); // TTL
        input.readLong(); // SentMillis
        }
    
    // Declared at the super level
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
        output.writeInt(getPacketType());
        output.writeShort(getToId());
        output.writeShort(getFromId());
        output.writeByte(getTimeToLive());
        output.writeLong(getSentMillis());
        output.writeBoolean(isPreferredPortUsed());
        }
    }
