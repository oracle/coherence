
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.socket.TcpSocketAccepter

package com.tangosol.coherence.component.net.socket;

import com.tangosol.net.messaging.ConnectionException;
import com.tangosol.util.WrapperException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class TcpSocketAccepter
        extends    com.tangosol.coherence.component.net.Socket
    {
    // ---- Fields declarations ----
    
    /**
     * Property Backlog
     *
     * Specifies the maximum queue length for incoming connection indications
     * (a request to connect). If a connection indication arrives when the
     * queue is full, the connection is refused. 
     * 
     */
    private int __m_Backlog;
    
    /**
     * Property ServerSocket
     *
     */
    private java.net.ServerSocket __m_ServerSocket;
    private static com.tangosol.util.ListMap __mapChildren;
    
    // Static initializer
    static
        {
        __initStatic();
        }
    
    // Default static initializer
    private static void __initStatic()
        {
        // register child classes
        __mapChildren = new com.tangosol.util.ListMap();
        __mapChildren.put("TcpSocket", TcpSocketAccepter.TcpSocket.get_CLASS());
        }
    
    // Default constructor
    public TcpSocketAccepter()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public TcpSocketAccepter(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            setBacklog(32);
            }
        catch (java.lang.Exception e)
            {
            // re-throw as a runtime exception
            throw new com.tangosol.util.WrapperException(e);
            }
        
        // containment initialization: children
        
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
        return new com.tangosol.coherence.component.net.socket.TcpSocketAccepter();
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
            clz = Class.forName("com.tangosol.coherence/component/net/socket/TcpSocketAccepter".replace('/', '.'));
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
    
    //++ getter for autogen property _ChildClasses
    /**
     * This is an auto-generated method that returns the map of design time
    * [static] children.
    * 
    * Note: the class generator will ignore any custom implementation for this
    * behavior.
     */
    protected java.util.Map get_ChildClasses()
        {
        return __mapChildren;
        }
    
    /**
     * Wait for incoming request to create a (point-to-point) TcpSocket.
    * 
    * @return the new TcpSocket; null if timeout occured.
     */
    public TcpSocket accept()
        {
        // import com.tangosol.net.messaging.ConnectionException;
        // import java.io.InterruptedIOException;
        // import java.io.IOException;
        // import java.net.ServerSocket;
        // import java.net.Socket;
        
        while (true)
            {
            IOException  eIO       = null;
            ServerSocket socketSrv = getServerSocket();
        
            try
                {
                if (socketSrv != null)
                    {
                    Socket     socket    = socketSrv.accept();
                    TcpSocketAccepter.TcpSocket socketTcp = (TcpSocketAccepter.TcpSocket) _newChild("TcpSocket");
        
                    socketTcp.setSocket(socket);
                    return socketTcp;
                    }
                }
            catch (InterruptedIOException e)
                {
                onInterruptedIOException(e);
                return null;
                }
            catch (IOException e)
                {
                eIO = e;
                }
        
            synchronized (getLock())
                {
                // verify that a socket refresh has not already occurred
                if (socketSrv == getServerSocket())
                    {        
                    switch (getState())
                        {
                        case STATE_OPEN:
                            // re-open the socket or take other action
                            onAcceptException(eIO);
                            break;
                        default:
                            throw new ConnectionException("TcpSocketAccepter.accept: " +
                                "unable to reopen socket; State=" + formatStateName(getState()), eIO);
                        }
                    }
                }
            }
        }
    
    // Declared at the super level
    public void close()
        {
        // import java.net.ServerSocket;
        
        synchronized (getLock())
            {
            if (getState() != STATE_CLOSED)
                {
                ServerSocket socket = getServerSocket();
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
                    setServerSocket(null);
                    }
        
                setState(STATE_CLOSED);
                }
            }
        }
    
    // Accessor for the property "Backlog"
    /**
     * Getter for property Backlog.<p>
    * Specifies the maximum queue length for incoming connection indications (a
    * request to connect). If a connection indication arrives when the queue is
    * full, the connection is refused. 
    * 
     */
    public int getBacklog()
        {
        return __m_Backlog;
        }
    
    // Declared at the super level
    /**
     * Getter for property Description.<p>
    * Human readable socekt description.
     */
    public String getDescription()
        {
        // import java.net.ServerSocket;
        
        if (getState() == STATE_OPEN)
            {
            ServerSocket socket = getServerSocket();
            return "ServerSocket=" +
                toString(socket.getInetAddress()) + ':' + socket.getLocalPort();
            }
        else
            {
            return super.getDescription();
            }
        }
    
    // Accessor for the property "ServerSocket"
    /**
     * Getter for property ServerSocket.<p>
     */
    public java.net.ServerSocket getServerSocket()
        {
        return __m_ServerSocket;
        }
    
    protected java.net.ServerSocket instantiateServerSocket()
            throws java.io.IOException
        {
        // import java.io.IOException;
        // import java.net.InetAddress;
        // import java.net.InetSocketAddress;
        // import java.net.ServerSocket;
        
        InetAddress addr     = getInetAddress();
        int         nPort    = getPort();
        int         cBacklog = getBacklog();
        
        _assert(addr != null, "TcpSocketAccepter.open: "
            + "InetAddress is required");
        _assert(nPort >= 0 && nPort <= 65535, "TcpSocketAccepter.open: "
            + "Port out of range (" + nPort + ")");
        _assert(cBacklog > 0 && cBacklog <= 65535, "TcpSocketAccepter.open: "
            + "Backlog out of range (" + cBacklog + ")");
        
        ServerSocket socket = getProvider().openServerSocket();
        try
            {
            socket.bind(new InetSocketAddress(addr, nPort), cBacklog);
            }
        catch (IOException e)
            {
            socket.close();
            throw e;
            }
        
        return socket;
        }
    
    /**
     * 
    * @param eException  the causal exception
    * @param lSocketActionMillis  the time that the exception occurred (or the
    * enclosing operation began or was in progress)
     */
    protected void onAcceptException(java.io.IOException eException)
        {
        onException(eException);
        }
    
    // Declared at the super level
    public void open()
            throws java.io.IOException
        {
        // import com.tangosol.util.WrapperException;
        // import java.net.ServerSocket;
        // import java.net.SocketException;
        
        synchronized (getLock())
            {
            if (getState() != STATE_OPEN)
                {
                ServerSocket socket = instantiateServerSocket();
                try
                    {
                    try
                        {
                        int cMillis = getSoTimeout();
                        _assert(cMillis >= 0, "TcpSocketAccepter.open: "
                            + "ReceiveTimeout property must be greater than or equal to zero");
        
                        socket.setSoTimeout(cMillis);
                        validateSoTimeout(socket.getSoTimeout(), cMillis);
                        }
                    catch (SocketException e)
                        {
                        throw new WrapperException(e);
                        }
        
        
                    if (getPort() == 0)
                        {
                        setPort(socket.getLocalPort());
                        }
                    setServerSocket(socket);
                    }
                catch (RuntimeException e)
                    {
                    try
                        {
                        socket.close();
                        }
                    catch (Exception eIgnore) {}
                    setServerSocket(null);
                    throw e;
                    }
        
                setState(STATE_OPEN);
                }
            }
        }
    
    // Accessor for the property "Backlog"
    /**
     * Setter for property Backlog.<p>
    * Specifies the maximum queue length for incoming connection indications (a
    * request to connect). If a connection indication arrives when the queue is
    * full, the connection is refused. 
    * 
     */
    public void setBacklog(int cItems)
        {
        synchronized (getLock())
            {
            _assert(getState() == STATE_INITIAL,
                "Backlog cannot be modified once the socket has been opened");
        
            __m_Backlog = (cItems);
            }
        }
    
    // Accessor for the property "ServerSocket"
    /**
     * Setter for property ServerSocket.<p>
     */
    protected void setServerSocket(java.net.ServerSocket socket)
        {
        __m_ServerSocket = socket;
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
        // import java.io.IOException;
        // import java.net.ServerSocket;
        
        synchronized (getLock())
            {
            _assert(cMillis >= 0);
        
            if (getState() == STATE_OPEN)
                {
                ServerSocket socket = getServerSocket();
                try
                    {
                    socket.setSoTimeout(cMillis);
                    validateSoTimeout(socket.getSoTimeout(), cMillis);
                    }
                catch (IOException e)
                    {
                    throw new WrapperException(e);
                    }
                }
        
            super.setSoTimeout(cMillis);
            }
        }

    // ---- class: com.tangosol.coherence.component.net.socket.TcpSocketAccepter$TcpSocket
    
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class TcpSocket
            extends    com.tangosol.coherence.component.net.socket.TcpSocket
        {
        // ---- Fields declarations ----
        
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
            return new com.tangosol.coherence.component.net.socket.TcpSocketAccepter.TcpSocket();
            }
        
        //++ getter for static property _CLASS
        /**
         * Getter for property _CLASS.<p>
        * Property with auto-generated accessor that returns the Class object
        * for a given component.
         */
        public static Class get_CLASS()
            {
            Class clz;
            try
                {
                clz = Class.forName("com.tangosol.coherence/component/net/socket/TcpSocketAccepter$TcpSocket".replace('/', '.'));
                }
            catch (ClassNotFoundException e)
                {
                throw new NoClassDefFoundError(e.getMessage());
                }
            return clz;
            }
        
        //++ getter for autogen property _Module
        /**
         * This is an auto-generated method that returns the global [design
        * time] parent component.
        * 
        * Note: the class generator will ignore any custom implementation for
        * this behavior.
         */
        private com.tangosol.coherence.Component get_Module()
            {
            return this.get_Parent();
            }
        
        // Declared at the super level
        protected java.net.Socket instantiateSocket()
                throws java.io.IOException
            {
            throw new UnsupportedOperationException();
            }
        
        // Declared at the super level
        public void open()
                throws java.io.IOException
            {
            throw new UnsupportedOperationException();
            }
        
        // Declared at the super level
        /**
         * Setter for property Socket.<p>
         */
        public void setSocket(java.net.Socket socket)
            {
            // import com.tangosol.util.WrapperException;
            // import java.io.IOException;
            
            if (socket != null)
                {
                _assert(getSocket() == null, "Socket is not resettable");
            
                setInetAddress      (socket.getLocalAddress());
                setPort             (socket.getLocalPort()   );
                setRemoteInetAddress(socket.getInetAddress() );
                setRemotePort       (socket.getPort()        );
            
                try
                    {
                    initializeSocket(socket);
                    }
                catch (IOException e)
                    {
                    throw new WrapperException(e);
                    }
            
                setState(STATE_OPEN);
                }
            
            super.setSocket(socket);
            }
        }
    }
