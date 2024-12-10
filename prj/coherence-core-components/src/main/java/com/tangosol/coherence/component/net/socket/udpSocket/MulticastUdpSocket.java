
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.socket.udpSocket.MulticastUdpSocket

package com.tangosol.coherence.component.net.socket.udpSocket;

import com.tangosol.util.HashHelper;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.SocketAddress;

@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class MulticastUdpSocket
        extends    com.tangosol.coherence.component.net.socket.UdpSocket
    {
    // ---- Fields declarations ----
    
    /**
     * Property Address
     *
     * The multicast address
     */
    private java.net.SocketAddress __m_Address;
    
    /**
     * Property InterfaceInetAddress
     *
     * @see java.net.MulticastSocket#setInterface
     */
    private java.net.InetAddress __m_InterfaceInetAddress;
    
    /**
     * Property TimeToLive
     *
     * @see java.net.MulticastSocket#setTimeToLive
     */
    private int __m_TimeToLive;
    
    // Default constructor
    public MulticastUdpSocket()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public MulticastUdpSocket(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            setIgnoreSendErrors(false);
            setPacketLength(0);
            setRxDebugDropRate(0);
            setSoTimeout(-1);
            setTimeToLive(-1);
            setTxDebugDropRate(0);
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
        return new com.tangosol.coherence.component.net.socket.udpSocket.MulticastUdpSocket();
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
            clz = Class.forName("com.tangosol.coherence/component/net/socket/udpSocket/MulticastUdpSocket".replace('/', '.'));
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
     * Bind the socket.
     */
    protected void bind(java.net.DatagramSocket socket)
            throws java.io.IOException
        {
        // import java.net.MulticastSocket;
        // import java.net.InetSocketAddress;
        
        MulticastSocket socketMulti = (MulticastSocket) socket;
        socketMulti.bind(new InetSocketAddress(getPort()));
        socketMulti.joinGroup(getInetAddress());
        }
    
    // Accessor for the property "Address"
    /**
     * Getter for property Address.<p>
    * The multicast address
     */
    public java.net.SocketAddress getAddress()
        {
        // import java.net.InetSocketAddress;
        // import java.net.SocketAddress;
        
        SocketAddress addr = __m_Address;
        if (addr == null)
            {
            setAddress(addr = new InetSocketAddress(getInetAddress(), getPort()));
            }
        return addr;
        }
    
    // Declared at the super level
    /**
     * Getter for property Description.<p>
    * Human readable socekt description.
     */
    public String getDescription()
        {
        StringBuffer sb = new StringBuffer(super.getDescription());
        
        sb.append(", InterfaceAddress=")
          .append(toString(getInterfaceInetAddress()))
          .append(", TimeToLive=")
          .append(getTimeToLive());
        
        return sb.toString();
        }
    
    // Accessor for the property "InterfaceInetAddress"
    /**
     * Getter for property InterfaceInetAddress.<p>
    * @see java.net.MulticastSocket#setInterface
     */
    public java.net.InetAddress getInterfaceInetAddress()
        {
        return __m_InterfaceInetAddress;
        }
    
    // Accessor for the property "TimeToLive"
    /**
     * Getter for property TimeToLive.<p>
    * @see java.net.MulticastSocket#setTimeToLive
     */
    public int getTimeToLive()
        {
        return __m_TimeToLive;
        }
    
    // Declared at the super level
    public int hashCode()
        {
        // import com.tangosol.util.HashHelper;
        
        // see ClusterConfig.setClusterName
        return HashHelper.hash(getTimeToLive() == 0, super.hashCode());
        }
    
    // Declared at the super level
    /**
     * Set up the specified java.net.DatagramSocket.
     */
    protected void initializeDatagramSocket(java.net.DatagramSocket socket)
            throws java.io.IOException
        {
        // import java.net.MulticastSocket;
        // import java.net.InetSocketAddress;
        
        super.initializeDatagramSocket(socket);
        
        MulticastSocket socketMulti = (MulticastSocket) socket;
        
        socketMulti.setTimeToLive(getTimeToLive());
        socketMulti.setInterface(getInterfaceInetAddress());
        }
    
    // Declared at the super level
    /**
     * Instantiate an underlying java.net.DatagramSocket.
     */
    protected java.net.DatagramSocket instantiateDatagramSocket()
            throws java.io.IOException
        {
        return getDatagramSocketProvider().openMulticastSocket();
        }
    
    // Accessor for the property "Address"
    /**
     * Setter for property Address.<p>
    * The multicast address
     */
    protected void setAddress(java.net.SocketAddress addr)
        {
        __m_Address = addr;
        }
    
    // Declared at the super level
    /**
     * Setter for property InetAddress.<p>
    * The IP address that the socket uses if it is a socket that connects to an
    * IP address. This property must be configured before the socket is opened.
     */
    public void setInetAddress(java.net.InetAddress addr)
        {
        if (addr == null || !addr.isMulticastAddress())
            {
            throw new IllegalArgumentException(
                "MulticastUdpSocket.InetAddress is not in the multicast range: " + toString(addr));
            }
        
        super.setInetAddress(addr);
        }
    
    // Accessor for the property "InterfaceInetAddress"
    /**
     * Setter for property InterfaceInetAddress.<p>
    * @see java.net.MulticastSocket#setInterface
     */
    public void setInterfaceInetAddress(java.net.InetAddress addr)
        {
        if (addr != null && addr.isMulticastAddress())
            {
            throw new IllegalArgumentException(
                "MulticastUdpSocket.InterfaceInetAddress is in the multicast range: " + toString(addr));
            }
        
        synchronized (getLock())
            {
            _assert(getState() != STATE_OPEN,
                "InterfaceInetAddress cannot be modified once the socket is open");
        
            __m_InterfaceInetAddress = (addr);
            }
        }
    
    // Accessor for the property "TimeToLive"
    /**
     * Setter for property TimeToLive.<p>
    * @see java.net.MulticastSocket#setTimeToLive
     */
    public void setTimeToLive(int ttl)
        {
        if (ttl < 0)
            {
            // means use system default
            return;
            }
        
        if (ttl > 255)
            {
            throw new IllegalArgumentException(
                "MulticastUdpSocket.TimeToLive is out of range (0..255): " + ttl);
            }
        
        synchronized (getLock())
            {
            _assert(getState() != STATE_OPEN,
                "TimeToLive cannot be modified once the socket is open");
        
            __m_TimeToLive = (ttl);
            }
        }
    }
