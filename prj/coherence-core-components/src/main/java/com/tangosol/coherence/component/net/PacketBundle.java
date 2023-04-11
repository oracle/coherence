
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.PacketBundle

package com.tangosol.coherence.component.net;

import com.oracle.coherence.common.internal.util.HeapDump;
import com.tangosol.util.Base;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Iterator;

@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class PacketBundle
        extends    com.tangosol.coherence.component.Net
    {
    // ---- Fields declarations ----
    
    /**
     * Property AddressCount
     *
     * The number of destinations this bundle is addressed to.
     */
    private transient int __m_AddressCount;
    
    /**
     * Property Addresses
     *
     * Polymorphic property that contains either a single SocketAddress or a
     * list of addresses to send the Packet(s) to.
     */
    private transient Object __m_Addresses;
    
    /**
     * Property Length
     *
     * The estimated current length of the bundled Packet(s). Used to determine
     * if additional Packets can be bundled.
     */
    private transient int __m_Length;
    
    /**
     * Property MaximumLength
     *
     * The maximum length the bundle can grow to.
     */
    private int __m_MaximumLength;
    
    /**
     * Property PacketCount
     *
     * The number of packets in this bundle.
     */
    private transient int __m_PacketCount;
    
    /**
     * Property Packets
     *
     * Polymorphic property that contains either a single Packet or a list of
     * all bundled Packets.
     */
    private Object __m_Packets;
    
    /**
     * Property UdpSocket
     *
     * The socket that is used to send this bundle.
     */
    private com.tangosol.coherence.component.net.socket.UdpSocket __m_UdpSocket;
    
    // Default constructor
    public PacketBundle()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public PacketBundle(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
        return new com.tangosol.coherence.component.net.PacketBundle();
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
            clz = Class.forName("com.tangosol.coherence/component/net/PacketBundle".replace('/', '.'));
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
     * Add a destination for this bundle.
    * 
    * @param address the address to add
     */
    public void addDestination(java.net.SocketAddress address)
        {
        // import java.net.SocketAddress;
        // import java.util.ArrayList;
        
        int cAddresses = getAddressCount();
        switch (cAddresses)
            {
            case 0:
                setAddresses(address);
                break;
        
            case 1:
                SocketAddress address0 = (SocketAddress) getAddresses();
                ArrayList     list     = new ArrayList(8);
                list.add(address0);
                list.add(address);
                setAddresses(list);
                break;
        
            default:
                ((ArrayList) getAddresses()).add(address);
                break;
            }
        
        setAddressCount(cAddresses + 1);
        }
    
    /**
     * Add multiple destinations for this bundle.
    * 
    * @param colAddr the collection of addresses to add
     */
    public void addDestinations(java.util.Collection colAddr)
        {
        // import java.net.SocketAddress;
        // import java.util.Iterator;
        
        for (Iterator iter = colAddr.iterator(); iter.hasNext(); )
            {
            addDestination((SocketAddress) iter.next());
            }
        }
    
    /**
     * Append a new Packet to this bundle.
    * 
    * @param packet the Packet to add
     */
    public void addPacket(Packet packet)
        {
        // import java.util.ArrayList;
        
        int cPackets = getPacketCount();
        switch (cPackets)
            {
            case 0:
                setPackets(packet);
                break;
        
            case 1:
                Packet    packet0 = (Packet) getPackets();
                ArrayList list    = new ArrayList(8);
                list.add(packet0);
                list.add(packet);
                setPackets(list);
                break;
        
            default:
                ((ArrayList) getPackets()).add(packet);
                break;
            }
        
        setPacketCount(cPackets + 1);
        setLength(getLength() + packet.getLength());
        }
    
    /**
     * Append the specified bundle to this bundle, i.e. merge the bundles.
    * 
    * @param bundle  the bundle to append to this bundle
    * 
    * @return true if the bundle were merged, false otherwise
     */
    public boolean append(PacketBundle bundle)
        {
        // import java.util.ArrayList;
        
        int cbTotal = getLength() + bundle.getLength();
        int cbMax   = Math.min(bundle.getMaximumLength(), getMaximumLength());
        if (cbTotal <= cbMax && isCommonDestination(bundle))
            {
            int cPackets = bundle.getPacketCount();
            if (cPackets > 1)
                {
                ArrayList listPackets = (ArrayList) bundle.getPackets();
                for (int i = 0; i < cPackets; i++)
                    {
                    addPacket((Packet) listPackets.get(i));
                    }
                }
            else
                {
                addPacket((Packet) bundle.getPackets());
                }
        
            setMaximumLength(cbMax);
            return true;
            }
        
        // insufficient space, or different addresses
        return false;
        }
    
    /**
     * Return an address for a given index.
     */
    public java.net.SocketAddress getAddress(int i)
        {
        // import java.net.SocketAddress;
        // import java.util.ArrayList;
        
        Object oAddresses = getAddresses();
        
        if (getAddressCount() == 1)
            {
            if (i == 0)
                {
                return (SocketAddress) oAddresses;
                }
            throw new ArrayIndexOutOfBoundsException();
            }
        
        return (SocketAddress) ((ArrayList) oAddresses).get(i);
        }
    
    // Accessor for the property "AddressCount"
    /**
     * Getter for property AddressCount.<p>
    * The number of destinations this bundle is addressed to.
     */
    public int getAddressCount()
        {
        return __m_AddressCount;
        }
    
    /**
     * Return a human readable address string.
     */
    public String getAddressDescription()
        {
        StringBuilder sb = new StringBuilder('[');
        
        for (int i = 0, c = getAddressCount(); i < c; ++i)
            {
            if (i != 0)
                {
                sb.append(" ");
                }
            sb.append(getAddress(i));
            }
        
        return sb.append(']').toString();
        }
    
    // Accessor for the property "Addresses"
    /**
     * Getter for property Addresses.<p>
    * Polymorphic property that contains either a single SocketAddress or a
    * list of addresses to send the Packet(s) to.
     */
    protected Object getAddresses()
        {
        return __m_Addresses;
        }
    
    // Accessor for the property "Length"
    /**
     * Getter for property Length.<p>
    * The estimated current length of the bundled Packet(s). Used to determine
    * if additional Packets can be bundled.
     */
    public int getLength()
        {
        return __m_Length;
        }
    
    // Accessor for the property "MaximumLength"
    /**
     * Getter for property MaximumLength.<p>
    * The maximum length the bundle can grow to.
     */
    public int getMaximumLength()
        {
        return __m_MaximumLength;
        }
    
    /**
     * Return a packet for a given index.
     */
    public Packet getPacket(int i)
        {
        // import java.util.ArrayList;
        
        if (getPacketCount() == 1)
            {
            if (i == 0)
                {
                return (Packet) getPackets();
                }
            throw new ArrayIndexOutOfBoundsException();
            }
        
        return (Packet) ((ArrayList) getPackets()).get(i);
        }
    
    // Accessor for the property "PacketCount"
    /**
     * Getter for property PacketCount.<p>
    * The number of packets in this bundle.
     */
    public int getPacketCount()
        {
        return __m_PacketCount;
        }
    
    // Accessor for the property "Packets"
    /**
     * Getter for property Packets.<p>
    * Polymorphic property that contains either a single Packet or a list of
    * all bundled Packets.
     */
    protected Object getPackets()
        {
        return __m_Packets;
        }
    
    // Accessor for the property "UdpSocket"
    /**
     * Getter for property UdpSocket.<p>
    * The socket that is used to send this bundle.
     */
    public com.tangosol.coherence.component.net.socket.UdpSocket getUdpSocket()
        {
        return __m_UdpSocket;
        }
    
    /**
     * Checks if the bundle has any destinations.
    * 
    * @returns true iff this bundle has any destinations
     */
    public boolean hasDestinations()
        {
        return getAddressCount() > 0;
        }
    
    /**
     * Return true if the specified bundle is desitned for the same address as
    * this bundle.
     */
    protected boolean isCommonDestination(PacketBundle bundle)
        {
        // import com.tangosol.util.Base;
        
        // To avoid n^2 scan, only ensure that address arrays are identical, i.e. order must
        // also be the same.  This could be impacted by an implementation of MemberSet used in
        // filling out the address
        
        int cAddresses = getAddressCount();
        return cAddresses ==  bundle.getAddressCount() &&
            Base.equals(getAddresses(), bundle.getAddresses());
        }
    
    /**
     * Send this UdpPacket to the destination specified by SocketAddress.
     */
    public void send()
        {
        getUdpSocket().send(this);
        }
    
    // Accessor for the property "AddressCount"
    /**
     * Setter for property AddressCount.<p>
    * The number of destinations this bundle is addressed to.
     */
    protected void setAddressCount(int cAddresses)
        {
        __m_AddressCount = cAddresses;
        }
    
    // Accessor for the property "Addresses"
    /**
     * Setter for property Addresses.<p>
    * Polymorphic property that contains either a single SocketAddress or a
    * list of addresses to send the Packet(s) to.
     */
    protected void setAddresses(Object oAddresses)
        {
        __m_Addresses = oAddresses;
        }
    
    // Accessor for the property "Length"
    /**
     * Setter for property Length.<p>
    * The estimated current length of the bundled Packet(s). Used to determine
    * if additional Packets can be bundled.
     */
    protected void setLength(int nLength)
        {
        __m_Length = nLength;
        }
    
    // Accessor for the property "MaximumLength"
    /**
     * Setter for property MaximumLength.<p>
    * The maximum length the bundle can grow to.
     */
    public void setMaximumLength(int cb)
        {
        __m_MaximumLength = cb;
        }
    
    // Accessor for the property "PacketCount"
    /**
     * Setter for property PacketCount.<p>
    * The number of packets in this bundle.
     */
    protected void setPacketCount(int cPackets)
        {
        __m_PacketCount = cPackets;
        }
    
    // Accessor for the property "Packets"
    /**
     * Setter for property Packets.<p>
    * Polymorphic property that contains either a single Packet or a list of
    * all bundled Packets.
     */
    protected void setPackets(Object oPackets)
        {
        __m_Packets = oPackets;
        }
    
    // Accessor for the property "UdpSocket"
    /**
     * Setter for property UdpSocket.<p>
    * The socket that is used to send this bundle.
     */
    public void setUdpSocket(com.tangosol.coherence.component.net.socket.UdpSocket socket)
        {
        __m_UdpSocket = socket;
        }
    
    // Declared at the super level
    public String toString()
        {
        StringBuilder sb = new StringBuilder();
        
        sb.append(get_Name())
          .append("@")
          .append(hashCode())
          .append(", socket=")
          .append(getUdpSocket())
          .append(", address=")
          .append(getAddressDescription())
          .append(", Payload={ ");
        
        for (int i = 0, c = getPacketCount(); i < c; ++i)
            {
            if (i != 0)
                {
                sb.append(" ");
                }
            sb.append(getPacket(i));
            }
        
        sb.append("}");
        
        return sb.toString();
        }
    
    /**
     * Serialize the contained Packet(s) to the datagram buffer.
    * 
    * @throws IOException
     */
    public int write(com.tangosol.io.WriteBuffer.BufferOutput output)
            throws java.io.IOException
        {
        // import com.oracle.coherence.common.internal.util.HeapDump;
        // import java.util.ArrayList;
        
        int cPackets = getPacketCount();
        switch (cPackets)
            {
            case 0:
                break;
        
            case 1:
                ((Packet) getPackets()).write(output);
                break;
        
            default:
                ArrayList list = (ArrayList) getPackets();
                for (int i = 0; i < cPackets; i++)
                    {
                    ((Packet) list.get(i)).write(output);
                    }
                break;
            }
        
        int of = output.getOffset();
        
        // the number of bytes may be less after the packet(s) has been serialized,
        // since the destination member set is allowed to shrink after the packet
        // has been assigned
        if (of > getLength())
            {    
            // Detection and debugging for Bug 25908156.  This particular bug has now been fixed, but we can't be sure
            // there aren't other things which could also trigger this so we'll leave this debugging in place as it
            // has no cost unless triggered.
            String sDump = HeapDump.dumpHeapForBug("Bug25908156");
            
            // for debug purpose
            StringBuilder sb = new StringBuilder();
            if (cPackets == 1)
                {
                sb.append(((Packet) getPackets()).toString());
                }
            else if (cPackets > 1)
                {
                ArrayList list = (ArrayList) getPackets();
                for (int i = 0; i < cPackets; ++i)
                    {
                    sb.append(" packet").append(i).append(":");
                    sb.append(((Packet) list.get(i)).toString());
                    }
                }
        
            throw new IllegalStateException(String.format(
                    "HeapDump " + sDump + " has been generated due to illegal buffer offset in writing packet; " +
                    "(offset: %d, length: %d, packet-count: %d, class: %s, output: %s, packets: %s)",
                    new Object[] {Integer.valueOf(of), Integer.valueOf(getLength()),
                    Integer.valueOf(cPackets), output.getClass().getName(), output, sb}));
            }
        
        return of;
        }
    }
