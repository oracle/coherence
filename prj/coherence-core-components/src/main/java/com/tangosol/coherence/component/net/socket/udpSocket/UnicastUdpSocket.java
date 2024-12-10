
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.socket.udpSocket.UnicastUdpSocket

package com.tangosol.coherence.component.net.socket.udpSocket;

@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class UnicastUdpSocket
        extends    com.tangosol.coherence.component.net.socket.UdpSocket
    {
    // ---- Fields declarations ----
    
    // Default constructor
    public UnicastUdpSocket()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public UnicastUdpSocket(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
        return new com.tangosol.coherence.component.net.socket.udpSocket.UnicastUdpSocket();
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
            clz = Class.forName("com.tangosol.coherence/component/net/socket/udpSocket/UnicastUdpSocket".replace('/', '.'));
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
     * Setter for property InetAddress.<p>
    * The IP address that the socket uses if it is a socket that connects to an
    * IP address. This property must be configured before the socket is opened.
     */
    public void setInetAddress(java.net.InetAddress addr)
        {
        if (addr != null && addr.isMulticastAddress())
            {
            throw new IllegalArgumentException(
                "UnicastUdpSocket.InetAddress is in the multicast range: " + toString(addr));
            }
        
        super.setInetAddress(addr);
        }
    }
