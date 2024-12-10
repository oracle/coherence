
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.socket.TcpSocket

package com.tangosol.coherence.component.net.socket;

import com.tangosol.util.WrapperException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;

@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class TcpSocket
        extends    com.tangosol.coherence.component.net.Socket
    {
    // ---- Fields declarations ----
    
    /**
     * Property BufferLength
     *
     */
    private int __m_BufferLength;
    
    /**
     * Property KeepAlive
     *
     * Specifies whether or not SO_KEEPALIVE is enabled.
     */
    private boolean __m_KeepAlive;
    
    /**
     * Property RemoteInetAddress
     *
     */
    private java.net.InetAddress __m_RemoteInetAddress;
    
    /**
     * Property RemotePort
     *
     */
    private int __m_RemotePort;
    
    /**
     * Property Socket
     *
     */
    private java.net.Socket __m_Socket;
    
    /**
     * Property SoLinger
     *
     * Specifies linger time in seconds. The maximum value is platform
     * specific. Value of -1 indicates that the linger is off.The setting only
     * affects socket close.
     */
    private int __m_SoLinger;
    
    /**
     * Property TcpNoDelay
     *
     * Specifies whether or not to enable/disable TCP_NODELAY (disable/enable
     * Nagle's algorithm).
     */
    private boolean __m_TcpNoDelay;
    
    // Default constructor
    public TcpSocket()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public TcpSocket(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            setSoLinger(-1);
            setSoTimeout(-1);
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
        return new com.tangosol.coherence.component.net.socket.TcpSocket();
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
            clz = Class.forName("com.tangosol.coherence/component/net/socket/TcpSocket".replace('/', '.'));
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
    public void close()
        {
        // import java.net.Socket;
        
        synchronized (getLock())
            {
            if (getState() != STATE_CLOSED)
                {
                Socket socket = getSocket();
                if (socket != null)
                    {
                    try
                        {
                        socket.close();
                        }
                    catch (Exception e)
                        {
                        // ignore exception on close; assume the socket is
                        // closed since there is nothing else that can be
                        // done to close it
                        }
                    setSocket(null);
                    }
        
                setState(STATE_CLOSED);
                }
            }
        }
    
    // Accessor for the property "BufferLength"
    /**
     * Getter for property BufferLength.<p>
     */
    public int getBufferLength()
        {
        return __m_BufferLength;
        }
    
    // Declared at the super level
    /**
     * Getter for property Description.<p>
    * Human readable socekt description.
     */
    public String getDescription()
        {
        if (getState() == STATE_OPEN)
            {
            return "Socket=" + getSocket();
            }
        else
            {
            StringBuffer sb = new StringBuffer();
        
            sb.append("Remote address:port=")
              .append(toString(getRemoteInetAddress()))
              .append(':')
              .append(getRemotePort());
        
            if (getInetAddress() != null)
                {
                sb.append(", Local address:port=")
                  .append(toString(getInetAddress()))
                  .append(':')
                  .append(getPort());
                }
            return sb.toString();
            }
        }
    
    // Accessor for the property "InputStream"
    /**
     * Getter for property InputStream.<p>
    * Returns an input stream for this socket.
     */
    public java.io.InputStream getInputStream()
        {
        // import com.tangosol.util.WrapperException;
        // import java.io.IOException;
        
        if (getState() != STATE_OPEN)
            {
            throw new IllegalStateException("Socket is closed");
            }
        
        try
            {
            return getSocket().getInputStream();
            }
        catch (IOException e)
            {
            throw new WrapperException(e);
            }
        }
    
    // Accessor for the property "OutputStream"
    /**
     * Getter for property OutputStream.<p>
    * Returns an output stream for this socket.
     */
    public java.io.OutputStream getOutputStream()
        {
        // import com.tangosol.util.WrapperException;
        // import java.io.IOException;
        
        if (getState() != STATE_OPEN)
            {
            throw new IllegalStateException("Socket is closed");
            }
        
        try
            {
            return getSocket().getOutputStream();
            }
        catch (IOException e)
            {
            throw new WrapperException(e);
            }
        }
    
    // Accessor for the property "RemoteInetAddress"
    /**
     * Getter for property RemoteInetAddress.<p>
     */
    public java.net.InetAddress getRemoteInetAddress()
        {
        return __m_RemoteInetAddress;
        }
    
    // Accessor for the property "RemotePort"
    /**
     * Getter for property RemotePort.<p>
     */
    public int getRemotePort()
        {
        return __m_RemotePort;
        }
    
    // Accessor for the property "Socket"
    /**
     * Getter for property Socket.<p>
     */
    public java.net.Socket getSocket()
        {
        return __m_Socket;
        }
    
    // Accessor for the property "SoLinger"
    /**
     * Getter for property SoLinger.<p>
    * Specifies linger time in seconds. The maximum value is platform specific.
    * Value of -1 indicates that the linger is off.The setting only affects
    * socket close.
     */
    public int getSoLinger()
        {
        return __m_SoLinger;
        }
    
    protected void initializeSocket(java.net.Socket socket)
            throws java.io.IOException
        {
        int cbBuffer = getBufferLength();
        if (cbBuffer > 0)
            {
            socket.setSendBufferSize(cbBuffer);
            validateBufferSize("send", socket.getSendBufferSize(), cbBuffer, cbBuffer);
        
            socket.setReceiveBufferSize(cbBuffer);
            validateBufferSize("receive", socket.getReceiveBufferSize(), cbBuffer, cbBuffer);
            }
        
        int cMillis = getSoTimeout();
        if (cMillis >= 0)
            {
            socket.setSoTimeout(cMillis);
            validateSoTimeout(socket.getSoTimeout(), cMillis);
            }
        
        int cLinger = getSoLinger();
        socket.setSoLinger(cLinger >= 0, Math.max(cLinger, 0));
        socket.setTcpNoDelay(isTcpNoDelay());
        socket.setKeepAlive(isKeepAlive());
        }
    
    protected java.net.Socket instantiateSocket()
            throws java.io.IOException
        {
        // import java.io.IOException;
        // import java.net.InetAddress;
        // import java.net.InetSocketAddress;
        // import java.net.Socket;
        
        InetAddress addrLocal   = getInetAddress();
        InetAddress addrRemote  = getRemoteInetAddress();
        int         nPortRemote = getRemotePort();
        
        _assert(addrRemote != null, "TcpSocket.open: "
            + "RemoteInetAddress is required");
        _assert(nPortRemote > 0 && nPortRemote <= 65535, "TcpSocket.open: "
            + "RemotePort out of range (" + nPortRemote + ")");
        
        Socket socket = getProvider().openSocket();
        try
            {
            if (addrLocal != null)
                {
                int nPortLocal = getPort();
        
                // bind to a specific local address
                _assert(nPortLocal > 0 && nPortLocal <= 65535, "TcpSocket.open: "
                    + "Port out of range (" + nPortLocal + ")");
        
                socket.bind(new InetSocketAddress(addrLocal, nPortLocal));
                }
        
            socket.connect(new InetSocketAddress(addrRemote, nPortRemote), getSoTimeout());
            }
        catch (IOException e)
            {
            socket.close();
            throw e;
            }
        return socket;
        }
    
    // Accessor for the property "KeepAlive"
    /**
     * Getter for property KeepAlive.<p>
    * Specifies whether or not SO_KEEPALIVE is enabled.
     */
    public boolean isKeepAlive()
        {
        return __m_KeepAlive;
        }
    
    // Accessor for the property "TcpNoDelay"
    /**
     * Getter for property TcpNoDelay.<p>
    * Specifies whether or not to enable/disable TCP_NODELAY (disable/enable
    * Nagle's algorithm).
     */
    public boolean isTcpNoDelay()
        {
        return __m_TcpNoDelay;
        }
    
    // Declared at the super level
    public void open()
            throws java.io.IOException
        {
        // import java.net.Socket;
        
        synchronized (getLock())
            {
            if (getState() != STATE_OPEN)
                {
                Socket socket = instantiateSocket();
                try
                    {
                    initializeSocket(socket);
        
                    setSocket(socket);
                    }
                catch (RuntimeException e)
                    {
                    try
                        {
                        socket.close();
                        }
                    catch (Exception ignored) {}
                    setSocket(null);
                    throw e;
                    }
        
                setState(STATE_OPEN);
                }
            }
        }
    
    // Accessor for the property "BufferLength"
    /**
     * Setter for property BufferLength.<p>
     */
    public void setBufferLength(int cb)
        {
        // import com.tangosol.util.WrapperException;
        // import java.net.Socket;
        // import java.net.SocketException;
        
        _assert(cb > 0);
        
        synchronized (getLock())
            {
            if (getState() == STATE_OPEN)
                {
                Socket socket = getSocket();
                try
                    {
                    socket.setSendBufferSize(cb);
                    validateBufferSize("send", socket.getSendBufferSize(), cb, cb);
            
                    socket.setReceiveBufferSize(cb);
                    validateBufferSize("receive", socket.getReceiveBufferSize(), cb, cb);
                    }
                catch (SocketException e)
                    {
                    throw new WrapperException(e);
                    }
                }
        
            __m_BufferLength = (cb);
            }
        }
    
    // Accessor for the property "KeepAlive"
    /**
     * Setter for property KeepAlive.<p>
    * Specifies whether or not SO_KEEPALIVE is enabled.
     */
    public void setKeepAlive(boolean fKeepAlive)
        {
        // import com.tangosol.util.WrapperException;
        // import java.net.SocketException;
        
        synchronized (getLock())
            {
            if (getState() == STATE_OPEN)
                {
                try
                    {
                    getSocket().setKeepAlive(fKeepAlive);
                    }
                catch (SocketException e)
                    {
                    throw new WrapperException(e);
                    }
                catch (NoSuchMethodError e)
                    {
                    // this method did not exist prior to JDK 1.3
                    return;
                    }
                }
        
            __m_KeepAlive = (fKeepAlive);
            }
        }
    
    // Accessor for the property "RemoteInetAddress"
    /**
     * Setter for property RemoteInetAddress.<p>
     */
    public void setRemoteInetAddress(java.net.InetAddress addr)
        {
        synchronized (getLock())
            {
            _assert(getState() == STATE_INITIAL,
                "RemoteInetAddress cannot be modified once the socket has been opened");
        
            __m_RemoteInetAddress = (addr);
            }
        }
    
    // Accessor for the property "RemotePort"
    /**
     * Setter for property RemotePort.<p>
     */
    public void setRemotePort(int nPort)
        {
        synchronized (getLock())
            {
            _assert(getState() == STATE_INITIAL,
                "RemotePort cannot be modified once the socket has been opened");
        
            __m_RemotePort = (nPort);
            }
        }
    
    // Accessor for the property "Socket"
    /**
     * Setter for property Socket.<p>
     */
    protected void setSocket(java.net.Socket socket)
        {
        __m_Socket = socket;
        }
    
    // Accessor for the property "SoLinger"
    /**
     * Setter for property SoLinger.<p>
    * Specifies linger time in seconds. The maximum value is platform specific.
    * Value of -1 indicates that the linger is off.The setting only affects
    * socket close.
     */
    public void setSoLinger(int cLinger)
        {
        // import com.tangosol.util.WrapperException;
        // import java.net.SocketException;
        
        synchronized (getLock())
            {
            if (getState() == STATE_OPEN)
                {
                try
                    {
                    getSocket().setSoLinger(cLinger >= 0, Math.max(cLinger, 0));
                    }
                catch (SocketException e)
                    {
                    throw new WrapperException(e);
                    }
                }
        
            __m_SoLinger = (cLinger);
            }
        }
    
    // Declared at the super level
    /**
     * Setter for property SoTimeout.<p>
    * Enable/disable SO_TIMEOUT with the specified timeout, in milliseconds.
    * With this value set to a non-zero timeout, a call to read(), receive() or
    * accept() for TcpSocket,  UdpSocket or TcpSecketAccepter will block for
    * only this amount of time. If the timeout expires, an 
    * java.io.InterruptedIOException is raised and onInterruptedIOException
    * event is called, though the Socket is still valid. The option must be
    * enabled prior to entering the blocking operation to have effect. The
    * timeout value must be > 0. A timeout of zero is interpreted as an
    * infinite timeout.
     */
    public void setSoTimeout(int cMillis)
        {
        // import com.tangosol.util.WrapperException;
        // import java.net.Socket;
        // import java.net.SocketException;
        
        if (cMillis >= 0)
            {
            synchronized (getLock())
                {
                if (getState() == STATE_OPEN)
                    {
                    Socket socket = getSocket();
                    try
                        {
                        socket.setSoTimeout(cMillis);
                        validateSoTimeout(socket.getSoTimeout(), cMillis);
                        }
                    catch (SocketException e)
                        {
                        throw new WrapperException(e);
                        }
                    }
        
                super.setSoTimeout(cMillis);
                }
            }
        }
    
    // Accessor for the property "TcpNoDelay"
    /**
     * Setter for property TcpNoDelay.<p>
    * Specifies whether or not to enable/disable TCP_NODELAY (disable/enable
    * Nagle's algorithm).
     */
    public void setTcpNoDelay(boolean fNoDelay)
        {
        // import com.tangosol.util.WrapperException;
        // import java.net.SocketException;
        
        synchronized (getLock())
            {
            if (getState() == STATE_OPEN)
                {
                try
                    {
                    getSocket().setTcpNoDelay(fNoDelay);
                    }
                catch (SocketException e)
                    {
                    throw new WrapperException(e);
                    }
                }
        
            __m_TcpNoDelay = (fNoDelay);
            }
        }
    
    public void shutdownInput()
            throws java.io.IOException
        {
        synchronized (getLock())
            {
            if (getState() == STATE_OPEN)
                {
                try
                    {
                    getSocket().shutdownInput();
                    }
                catch (NoSuchMethodError e)
                    {
                    // this method did not exist prior to JDK 1.3
                    }
                }
            }
        }
    
    public void shutdownOutput()
            throws java.io.IOException
        {
        synchronized (getLock())
            {
            if (getState() == STATE_OPEN)
                {
                try
                    {
                    getSocket().shutdownOutput();
                    }
                catch (NoSuchMethodError e)
                    {
                    // this method did not exist prior to JDK 1.3
                    }
                }
            }
        }
    }
