
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.extend.connection.TcpConnection

package com.tangosol.coherence.component.net.extend.connection;

import com.tangosol.coherence.component.net.extend.util.TcpUtil;
import java.net.Socket;

/**
 * Connection implementation that wraps a TCP/IP Socket.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class TcpConnection
        extends    com.tangosol.coherence.component.net.extend.Connection
    {
    // ---- Fields declarations ----
    
    /**
     * Property Socket
     *
     * The Socket wrapped by this Connection.
     */
    private java.net.Socket __m_Socket;
    
    // Default constructor
    public TcpConnection()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public TcpConnection(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            setChannelArray(new com.tangosol.util.SparseArray());
            setChannelPendingArray(new com.tangosol.util.SparseArray());
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
        return new com.tangosol.coherence.component.net.extend.connection.TcpConnection();
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
            clz = Class.forName("com.tangosol.coherence/component/net/extend/connection/TcpConnection".replace('/', '.'));
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
     * Return a human-readable description of this component.
    * 
    * @return a String representation of this component
     */
    protected String getDescription()
        {
        // import Component.Net.Extend.Util.TcpUtil;
        // import java.net.Socket;
        
        Socket socket = getSocket();
        if (socket == null)
            {
            return super.getDescription();
            }
        else
            {
            String s = super.getDescription();
            try
                {
                return s + ", LocalAddress="
                         + TcpUtil.toString(socket.getLocalSocketAddress())
                         + ", RemoteAddress="
                         + TcpUtil.toString(socket.getRemoteSocketAddress());
                }
            catch (Throwable e)
                {
                // see COH-5386
                return s;
                }
            }
        }
    
    // Accessor for the property "Socket"
    /**
     * Getter for property Socket.<p>
    * The Socket wrapped by this Connection.
     */
    public java.net.Socket getSocket()
        {
        return __m_Socket;
        }
    
    // Accessor for the property "Socket"
    /**
     * Setter for property Socket.<p>
    * The Socket wrapped by this Connection.
     */
    public void setSocket(java.net.Socket socket)
        {
        _assert(!isOpen());
        
        __m_Socket = (socket);
        }
    }
