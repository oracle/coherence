
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.extend.util.TcpUtil

package com.tangosol.coherence.component.net.extend.util;

import com.tangosol.net.internal.SocketAddressHelper;
import java.io.IOException;

/**
 * A collection of TCP/IP-releated utility methods.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public abstract class TcpUtil
        extends    com.tangosol.coherence.component.net.extend.Util
    {
    // ---- Fields declarations ----
    
    // Initializing constructor
    public TcpUtil(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
            clz = Class.forName("com.tangosol.coherence/component/net/extend/util/TcpUtil".replace('/', '.'));
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
     * Cancel the given SelectionKey. If the SelectionKey is canceled
    * successfully, this method returns true; otherwise, this method returns
    * false.
    * 
    * @param key  the SelectionKey to cancel; may be null
    * 
    * @return true if successful; false otherwise
     */
    public static boolean cancel(java.nio.channels.SelectionKey key)
        {
        if (key == null)
            {
            return false;
            }
        
        key.cancel();
        return true;
        }
    
    /**
     * Close the given ServerSocket. If the ServerSocket is closed successfully,
    * this method returns true; otherwise, this method returns false.
    * 
    * @param socket  the ServerSocket to close; may be null
    * 
    * @return true if successful; false otherwise
     */
    public static boolean close(java.net.ServerSocket socket)
        {
        // import java.io.IOException;
        
        if (socket != null)
            {
            try
                {
                socket.close();
                return true;
                }
            catch (IOException e) {}
            }
        return false;
        }
    
    /**
     * Close the given Socket. If the Socket is closed successfully, this method
    * returns true; otherwise, this method returns false.
    * 
    * @param socket  the Socket to close; may be null
    * 
    * @return true if successful; false otherwise
     */
    public static boolean close(java.net.Socket socket)
        {
        // import java.io.IOException;
        
        if (socket != null)
            {
            try
                {
                socket.close();
                return true;
                }
            catch (IOException e) {}
            }
        return false;
        }
    
    /**
     * Close the given Channel. If the Channel is closed successfully, this
    * method returns true; otherwise, this method returns false.
    * 
    * @param channel  the Channel to close; may be null
    * 
    * @return true if successful; false otherwise
     */
    public static boolean close(java.nio.channels.Channel channel)
        {
        // import java.io.IOException;
        
        if (channel != null)
            {
            try
                {
                channel.close();
                return true;
                }
            catch (IOException e) {}
            }
        return false;
        }
    
    /**
     * Close the given Selector. If the Selector is closed successfully, this
    * method returns true; otherwise, this method returns false.
    * 
    * @param selector  the Selector to close; may be null
    * 
    * @return true if successful; false otherwise
     */
    public static boolean close(java.nio.channels.Selector selector)
        {
        // import java.io.IOException;
        
        if (selector != null)
            {
            try
                {
                selector.close();
                return true;
                }
            catch (IOException e) {}
            }
        return false;
        }
    
    /**
     * Set the blocking mode for the given SelectableChannel. The blocking mode
    * is only changed if the given value differs from the current value.
    * 
    * @param channel    the SelectableChannel to configure
    * @param fBlocking  the new blocking mode
     */
    public static void setBlockingMode(java.nio.channels.SelectableChannel channel, boolean fBlocking)
        {
        try
            {
            if (channel.isBlocking() != fBlocking)
                {
                channel.configureBlocking(fBlocking);
                }
            }
        catch (Exception e)
            {
            throw ensureRuntimeException(e, "error configuring blocking mode for: "
                    + channel);
            }
        }
    
    /**
     * Enable or disable SO_KEEPALIVE on the given Socket. The value of
    * SO_KEEPALIVE is only changed if the given value differs from the current
    * value.
    * 
    * @param socket         the Socket to configure
    * @param fKeepAlive  the new value of SO_KEEPALIVE
     */
    public static void setKeepAlive(java.net.Socket socket, boolean fKeepAlive)
            throws java.net.SocketException
        {
        if (socket.getKeepAlive() != fKeepAlive)
            {
            socket.setKeepAlive(fKeepAlive);
            }
        }
    
    /**
     * Set the receive buffer size of the given ServerSocket to the specified
    * number of bytes. The maximum value is platform specific. The receive
    * buffer size is only changed if the given value is greater than 0 and
    * differs from the current value.
    * 
    * @param socket the ServerSocket to configure
    * @param cb        the new size of the receive buffer (in bytes)
     */
    public static void setReceiveBufferSize(java.net.ServerSocket socket, int cb)
            throws java.net.SocketException
        {
        if (cb > 0 && socket.getReceiveBufferSize() != cb)
            {
            socket.setReceiveBufferSize(cb);
            validateBufferSize(socket, cb);
            }
        }
    
    /**
     * Set the receive buffer size of the given Socket to the specified number
    * of bytes. The maximum value is platform specific. The receive buffer size
    * is only changed if the given value is greater than 0 and differs from the
    * current value.
    * 
    * @param socket the Socket to configure
    * @param cb        the new size of the receive buffer (in bytes)
     */
    public static void setReceiveBufferSize(java.net.Socket socket, int cb)
            throws java.net.SocketException
        {
        if (cb > 0 && socket.getReceiveBufferSize() != cb)
            {
            socket.setReceiveBufferSize(cb);
            validateBufferSize(socket, true, cb);
            }
        }
    
    /**
     * Enable or disable SO_REUSEADDR on the given ServerSocket. The value of
    * SO_REUSEADDR is only changed if the given value differs from the current
    * value. If the ServerSocket is already bound, this method has no effect.
    * 
    * @param socket         the Socket to configure
    * @param fKeepAlive  the new value of SO_REUSEADDR
     */
    public static void setReuseAddress(java.net.ServerSocket socket, boolean fReuseAddress)
            throws java.net.SocketException
        {
        if (!socket.isBound() && socket.getReuseAddress() != fReuseAddress)
            {
            socket.setReuseAddress(fReuseAddress);
            }
        }
    
    /**
     * Enable or disable SO_REUSEADDR on the given Socket. The value of
    * SO_REUSEADDR is only changed if the given value differs from the current
    * value. If the Socket is already bound, this method has no effect.
     */
    public static void setReuseAddress(java.net.Socket socket, boolean fReuseAddress)
            throws java.net.SocketException
        {
        if (!socket.isBound() && socket.getReuseAddress() != fReuseAddress)
            {
            socket.setReuseAddress(fReuseAddress);
            }
        }
    
    /**
     * Set the send buffer size of the given Socket to the specified number of
    * bytes. The maximum value is platform specific. The send buffer size is
    * only changed if the given value is greater than 0 and differs from the
    * current value.
    * 
    * @param socket the Socket to configure
    * @param cb        the new size of the send buffer (in bytes)
     */
    public static void setSendBufferSize(java.net.Socket socket, int cb)
            throws java.net.SocketException
        {
        if (cb > 0 && socket.getSendBufferSize() != cb)
            {
            socket.setSendBufferSize(cb);
            validateBufferSize(socket, false, cb);
            }
        }
    
    /**
     * Enable or disable SO_LINGER with the specified linger time in seconds on
    * the given Socket. The maximum value is platform specific. A value of -1
    * disables SO_LINGER. The value of SO_LINGER is only changed if the given
    * value is not 0 and differs from the current value.
    * 
    * @param socket  the Socket to configure
    * @param cSecs  the new value of SO_LINGER (in seconds)
     */
    public static void setSoLinger(java.net.Socket socket, int cSecs)
            throws java.net.SocketException
        {
        cSecs = Math.max(cSecs, -1);
        
        if (cSecs == 0)
            {
            return;
            }
        
        if (socket.getSoLinger() != cSecs)
            {
            if (cSecs > 0)
                {
                socket.setSoLinger(true, cSecs);
                }
            else
                {
                socket.setSoLinger(false, 0);
                }
        
            if (socket.getSoLinger() != cSecs)
                {
                if (cSecs > 0)
                    {
                    _trace("Failed to set a TCP Socket linger time to " + cSecs
                            + " seconds; actual value is " + socket.getSoLinger()
                            + " seconds", 2);
                    }
                else
                    {
                    _trace("Failed to disabled a TCP Socket linger time");
                    }
                }
            }
        }
    
    /**
     * Enable or disable TCP_NODELAY on the given Socket. The value of
    * TCP_NODELAY is only changed if the given value differs from the current
    * value.
    * 
    * @param socket         the Socket to configure
    * @param fKeepAlive  the new value of TCP_NODELAY
     */
    public static void setTcpNoDelay(java.net.Socket socket, boolean fTcpNoDelay)
            throws java.net.SocketException
        {
        if (socket.getTcpNoDelay() != fTcpNoDelay)
            {
            socket.setTcpNoDelay(fTcpNoDelay);
            }
        }
    
    /**
     * Return a String representation of the given SocketAddress.
    * 
    * @param addr  the SocketAddress
    * 
    * @return a String representation of the given SocketAddress
     */
    public static String toString(java.net.SocketAddress addr)
        {
        // import com.tangosol.net.internal.SocketAddressHelper;
        
        return SocketAddressHelper.toString(addr);
        }
    
    /**
     * Log a warning message if the receive buffer size of the given
    * ServerSocket is less than the specified number of bytes.
    * 
    * @param socket             the target ServerSocket
    * @param cbRequested  the requested size of the receive buffer (in bytes)
     */
    protected static void validateBufferSize(java.net.ServerSocket socket, int cbRequested)
            throws java.net.SocketException
        {
        validateBufferSize(true, socket.getReceiveBufferSize(), cbRequested);
        }
    
    /**
     * Log a warning message if the receive or send buffer size of the given
    * Socket is less than the specified number of bytes.
    * 
    * @param socket                the target Socket
    * @param fReceiveBuffer  if true, the specified buffer size is assumed to
    * be a receive buffer size; false if it is a send buffer size
    * @param cbRequested     the requested size of the buffer (in bytes)
     */
    protected static void validateBufferSize(java.net.Socket socket, boolean fReceiveBuffer, int cbRequested)
            throws java.net.SocketException
        {
        int cbActual = fReceiveBuffer
                ? socket.getReceiveBufferSize()
                : socket.getSendBufferSize();
        validateBufferSize(fReceiveBuffer, cbActual, cbRequested);
        }
    
    /**
     * Log a warning message if the given Socket or ServerSocket receive or send
    * buffer size is less than the specified number of bytes.
    * 
    * @param fReceiveBuffer  if true, the specified buffer sizes are assumed to
    * be receive buffer sizes; false if they are send buffer sizes
    * @param cbActual            the actual size of the buffer (in bytes)
    * @param cbRequested     the requested size of the buffer (in bytes)
     */
    protected static void validateBufferSize(boolean fReceiveBuffer, int cbActual, int cbRequested)
            throws java.net.SocketException
        {
        String sName = fReceiveBuffer ? "receive" : "send";
        if (cbActual < cbRequested)
            {
            _trace("Failed to set a TCP Socket " + sName + " buffer size to "
                    + cbRequested + " bytes; actual size is " + cbActual + " bytes. "
                    + "Consult your OS documentation regarding increasing the maximum "
                    + "TCP Socket " + sName + " buffer size. Proceeding with the "
                    + "actual value may cause sub-optimal performance.", 2);
            }
        }
    }
