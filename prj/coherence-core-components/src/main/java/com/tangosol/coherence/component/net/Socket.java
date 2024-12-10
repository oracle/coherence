
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.Socket

package com.tangosol.coherence.component.net;

import com.tangosol.net.InetAddressHelper;
import com.tangosol.net.SocketProviderFactory;
import com.tangosol.util.Base;
import java.net.InetAddress;

@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public abstract class Socket
        extends    com.tangosol.coherence.component.Net
    {
    // ---- Fields declarations ----
    
    /**
     * Property InetAddress
     *
     * The IP address that the socket uses if it is a socket that connects to
     * an IP address. This property must be configured before the socket is
     * opened.
     */
    private java.net.InetAddress __m_InetAddress;
    
    /**
     * Property LastException
     *
     * The last Exception that occurred on this Socket.
     */
    private Exception __m_LastException;
    
    /**
     * Property LastExceptionLogMillis
     *
     * The last time an exception was logged.
     */
    private long __m_LastExceptionLogMillis;
    
    /**
     * Property Lock
     *
     * The object that is used for synchronization. By default, it is this
     * socket. This property must be configured before the socket is opened.
     */
    private Object __m_Lock;
    
    /**
     * Property Port
     *
     * The network port number that the socket uses if it is a socket that
     * opens on a network port number. This property must be configured before
     * the socket is opened.
     */
    private int __m_Port;
    
    /**
     * Property Provider
     *
     * The SocketProvider to use in producing sockets.
     */
    private com.oracle.coherence.common.net.SocketProvider __m_Provider;
    
    /**
     * Property SoTimeout
     *
     * Enable/disable SO_TIMEOUT with the specified timeout, in milliseconds.
     * With this value set to a non-zero timeout, a call to read(), receive()
     * or accept() for TcpSocket,  UdpSocket or TcpSecketAccepter will block
     * for only this amount of time. If the timeout expires, an 
     * java.io.InterruptedIOException is raised and onInterruptedIOException
     * event is called, though the Socket is still valid. The option must be
     * enabled prior to entering the blocking operation to have effect. The
     * timeout value must be > 0. A timeout of zero is interpreted as an
     * infinite timeout.
     */
    private int __m_SoTimeout;
    
    /**
     * Property State
     *
     * One of STATE_INITIAL, STATE_OPEN and STATE_CLOSED. Configurable
     * properties should be set while the state is still STATE_INITIAL (before
     * the socket is first opened).
     */
    private int __m_State;
    
    /**
     * Property STATE_CLOSED
     *
     * Signifies that the socket is in a closed state.
     */
    public static final int STATE_CLOSED = 2;
    
    /**
     * Property STATE_INITIAL
     *
     * The initial state of the socket before it is opened for the first time.
     */
    public static final int STATE_INITIAL = 0;
    
    /**
     * Property STATE_OPEN
     *
     * Signifies that the socket is in an open state.
     */
    public static final int STATE_OPEN = 1;
    
    // Initializing constructor
    public Socket(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
        {
        super(sName, compParent, false);
        }
    
    // Private initializer
    protected void __initPrivate()
        {
        
        super.__initPrivate();
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
            clz = Class.forName("com.tangosol.coherence/component/net/Socket".replace('/', '.'));
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
    
    public void close()
        {
        }
    
    // Declared at the super level
    protected void finalize()
            throws java.lang.Throwable
        {
        close();
        }
    
    public static String formatStateName(int nState)
        {
        switch (nState)
            {
            case STATE_INITIAL:
                return "STATE_INITIAL";
            case STATE_OPEN:
                return "STATE_OPEN";
            case STATE_CLOSED:
                return "STATE_CLOSED";
            default:
                return "<unknown>";
            }
        }
    
    // Accessor for the property "Description"
    /**
     * Getter for property Description.<p>
    * Human readable socekt description.
     */
    public String getDescription()
        {
        return "address:port=" + toString(getInetAddress()) + ":" + getPort();
        }
    
    // Accessor for the property "InetAddress"
    /**
     * Getter for property InetAddress.<p>
    * The IP address that the socket uses if it is a socket that connects to an
    * IP address. This property must be configured before the socket is opened.
     */
    public java.net.InetAddress getInetAddress()
        {
        return __m_InetAddress;
        }
    
    // Accessor for the property "LastException"
    /**
     * Getter for property LastException.<p>
    * The last Exception that occurred on this Socket.
     */
    public Exception getLastException()
        {
        return __m_LastException;
        }
    
    // Accessor for the property "LastExceptionLogMillis"
    /**
     * Getter for property LastExceptionLogMillis.<p>
    * The last time an exception was logged.
     */
    public long getLastExceptionLogMillis()
        {
        return __m_LastExceptionLogMillis;
        }
    
    // Accessor for the property "Lock"
    /**
     * Getter for property Lock.<p>
    * The object that is used for synchronization. By default, it is this
    * socket. This property must be configured before the socket is opened.
     */
    public Object getLock()
        {
        Object lock = __m_Lock;
        return lock == null ? this : lock;
        }
    
    // Accessor for the property "Port"
    /**
     * Getter for property Port.<p>
    * The network port number that the socket uses if it is a socket that opens
    * on a network port number. This property must be configured before the
    * socket is opened.
     */
    public int getPort()
        {
        return __m_Port;
        }
    
    // Accessor for the property "Provider"
    /**
     * Getter for property Provider.<p>
    * The SocketProvider to use in producing sockets.
     */
    public com.oracle.coherence.common.net.SocketProvider getProvider()
        {
        return __m_Provider;
        }
    
    // Accessor for the property "SoTimeout"
    /**
     * Getter for property SoTimeout.<p>
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
    public int getSoTimeout()
        {
        return __m_SoTimeout;
        }
    
    // Accessor for the property "State"
    /**
     * Getter for property State.<p>
    * One of STATE_INITIAL, STATE_OPEN and STATE_CLOSED. Configurable
    * properties should be set while the state is still STATE_INITIAL (before
    * the socket is first opened).
     */
    public int getState()
        {
        return __m_State;
        }
    
    /**
     * Generic level for handling a Socket exception
    * 
    * @param eException  the causal exception
     */
    protected void onException(java.io.IOException eException)
        {
        // import com.tangosol.util.Base;
        
        setLastException(eException);
        
        long ldtNow = Base.getSafeTimeMillis();
        if (getLastExceptionLogMillis() < ldtNow - 1000L)
            {
            setLastExceptionLogMillis(ldtNow);
            _trace("Exception on " + this + "; " + eException + "\n" + Base.getStackTrace(eException), 9);
            }
        }
    
    // Declared at the super level
    /**
     * The "component has been initialized" method-notification called out of
    * setConstructed() for the topmost component and that in turn notifies all
    * the children.
    * 
    * This notification gets called before the control returns back to this
    * component instantiator (using <code>new Component.X()</code> or
    * <code>_newInstance(sName)</code>) and on the same thread. In addition,
    * visual components have a "posted" notification <code>onInitUI</code> that
    * is called after (or at the same time as) the control returns back to the
    * instantiator and possibly on a different thread.
     */
    public void onInit()
        {
        // import com.tangosol.net.SocketProviderFactory;
        
        setProvider(SocketProviderFactory.DEFAULT_SOCKET_PROVIDER);
        
        super.onInit();
        }
    
    /**
     * InterruptedIOException could be raised only when SoTimeout value is
    * greater then zero, and the timeout expires during a call to receive() for
    * a DatagramSocket, accept() for ServerSocket or getInputStream().read()
    * for Socket. The underlying socket remains valid.
    * 
    * @param eException  the causal exception
    * @param lSocketActionMillis  the time that the exception occurred (or the
    * enclosing operation began or was in progress)
    * 
    * @see SoTimeout property
     */
    protected void onInterruptedIOException(java.io.InterruptedIOException eException)
        {
        onException(eException);
        }
    
    public void open()
            throws java.io.IOException
        {
        }
    
    // Accessor for the property "InetAddress"
    /**
     * Setter for property InetAddress.<p>
    * The IP address that the socket uses if it is a socket that connects to an
    * IP address. This property must be configured before the socket is opened.
     */
    public void setInetAddress(java.net.InetAddress addr)
        {
        // import java.net.InetAddress;
        
        synchronized (getLock())
            {
            InetAddress addrCurr = getInetAddress();
            _assert(getState() == STATE_INITIAL || addrCurr == null || addrCurr.isAnyLocalAddress(),
                "InetAddress cannot be modified once the socket has been opened");
        
            __m_InetAddress = (addr);
            }
        }
    
    // Accessor for the property "LastException"
    /**
     * Setter for property LastException.<p>
    * The last Exception that occurred on this Socket.
     */
    protected void setLastException(Exception e)
        {
        __m_LastException = e;
        }
    
    // Accessor for the property "LastExceptionLogMillis"
    /**
     * Setter for property LastExceptionLogMillis.<p>
    * The last time an exception was logged.
     */
    protected void setLastExceptionLogMillis(long lMillis)
        {
        __m_LastExceptionLogMillis = lMillis;
        }
    
    // Accessor for the property "Lock"
    /**
     * Setter for property Lock.<p>
    * The object that is used for synchronization. By default, it is this
    * socket. This property must be configured before the socket is opened.
     */
    public void setLock(Object oLock)
        {
        _assert(getState() == STATE_INITIAL,
            "Lock cannot be modified once the socket has been opened");
        
        __m_Lock = (oLock);
        }
    
    // Accessor for the property "Port"
    /**
     * Setter for property Port.<p>
    * The network port number that the socket uses if it is a socket that opens
    * on a network port number. This property must be configured before the
    * socket is opened.
     */
    public void setPort(int nPort)
        {
        synchronized (getLock())
            {
            _assert(getState() == STATE_INITIAL,
                "Port cannot be modified once the socket has been opened");
        
            __m_Port = (nPort);
            }
        }
    
    // Accessor for the property "Provider"
    /**
     * Setter for property Provider.<p>
    * The SocketProvider to use in producing sockets.
     */
    public void setProvider(com.oracle.coherence.common.net.SocketProvider provider)
        {
        // import com.tangosol.net.SocketProviderFactory;
        
        if (provider == null)
            {
            provider = SocketProviderFactory.DEFAULT_SOCKET_PROVIDER;
            }
        
        __m_Provider = (provider);
        }
    
    // Accessor for the property "SoTimeout"
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
        __m_SoTimeout = cMillis;
        }
    
    // Accessor for the property "State"
    /**
     * Setter for property State.<p>
    * One of STATE_INITIAL, STATE_OPEN and STATE_CLOSED. Configurable
    * properties should be set while the state is still STATE_INITIAL (before
    * the socket is first opened).
     */
    protected void setState(int nState)
        {
        __m_State = nState;
        }
    
    // Declared at the super level
    public String toString()
        {
        StringBuffer sb = new StringBuffer(get_Name());
        
        sb.append("{State=")
          .append(formatStateName(getState()))
          .append(", ")
          .append(getDescription())
          .append('}');
        
        return sb.toString();
        }
    
    public static String toString(java.net.InetAddress addr)
        {
        // import com.tangosol.net.InetAddressHelper;
        
        // prevent the addr.getHostName() call which could be very expensive
        
        return InetAddressHelper.toString(addr);
        }
    
    protected void validateBufferSize(String sBufferName, int cbActualSize, int cbRequestedSize, int cbMinimumSize)
        {
        if (cbActualSize < cbRequestedSize)
            {
            String sMsg = get_Name() + " failed to set " + sBufferName + " buffer size to " +
                   cbRequestedSize + " bytes; actual size is " + cbActualSize + " bytes. " +
                   "Consult your OS documentation regarding increasing the maximum socket buffer size.";
        
            if (cbActualSize < cbMinimumSize)
                {
                // under minimum log error, and throw
                _trace(sMsg, 1);
                throw new RuntimeException(sMsg);
                }
            else
                {
                // over minimum, just log warning, and continue
                sMsg += " Proceeding with the actual value may cause sub-optimal performance.";
                _trace(sMsg, 2);        
                }
            }
        }
    
    protected void validateSoTimeout(int cActual, int cRequired)
        {
        if (cActual != cRequired)
            {
            throw new RuntimeException("Failed to set SoTimeout to " +
                cRequired + "; actual value is " + cActual);
            }
        }
    }
