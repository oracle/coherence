
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.socket.UdpSocket

package com.tangosol.coherence.component.net.socket;

import com.tangosol.coherence.config.Config;
import com.tangosol.io.ByteArrayWriteBuffer;
import com.tangosol.net.messaging.ConnectionException;
import com.tangosol.util.Base;
import com.tangosol.util.HashHelper;
import com.tangosol.util.WrapperException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.Set;

@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public abstract class UdpSocket
        extends    com.tangosol.coherence.component.net.Socket
    {
    // ---- Fields declarations ----
    
    /**
     * Property BufferReceivedBytes
     *
     * Maximum number of packets to ask the OS to buffer when receiving.
     */
    private int __m_BufferReceivedBytes;
    
    /**
     * Property BufferSentBytes
     *
     * Maximum number of packets to ask the OS to buffer when sending.
     */
    private int __m_BufferSentBytes;
    
    /**
     * Property BytesReceived
     *
     * The number of bytes received by this socket.
     */
    private long __m_BytesReceived;
    
    /**
     * Property BytesSent
     *
     * The number of bytes sent by this socket.
     */
    private long __m_BytesSent;
    
    /**
     * Property CountReceived
     *
     * The count of received packets.  Used only by the
     * SocketManager.refreshSocket()
     */
    private int __m_CountReceived;
    
    /**
     * Property CountSent
     *
     * The count of sent packets.  Used only by the
     * SocketManager.refreshSocket()
     */
    private int __m_CountSent;
    
    /**
     * Property DatagramSocket
     *
     * The actual socket.
     */
    private java.net.DatagramSocket __m_DatagramSocket;
    
    /**
     * Property DatagramSocketProvider
     *
     */
    private com.tangosol.net.DatagramSocketProvider __m_DatagramSocketProvider;
    
    /**
     * Property IgnoreSendErrors
     *
     * Indicates if IOExceptions resulting from a send operation should be
     * ignored.  On some OSs such as OS X, plain IOExceptions are thrown to
     * indicate an error regarding the destination.  For instance "Host is
     * down", or "No route to host".  As these are just thrown as IOExceptions
     * it is not possible to identify them other then via exception message
     * comparisions.
     * 
     * While the send method has some message hard coded, we can't know all
     * possible variants (including locale specific ones), we have this flag to
     * simply ignore all IOExceptions thrown from the send method.
     * 
     * The default value for the process may be controlled via the
     * tangosol.coherence.udp.ignoretxerror system property.
     */
    private transient boolean __m_IgnoreSendErrors;
    
    /**
     * Property IncomingPacket
     *
     * The DatagramPacket used for all receives.
     */
    private transient java.net.DatagramPacket __m_IncomingPacket;
    
    /**
     * Property OutgoingBuffer
     *
     * The reusable BufferOutput used to write into the OutgoingPacket byte[].
     */
    private transient com.tangosol.io.WriteBuffer.BufferOutput __m_OutgoingBuffer;
    
    /**
     * Property OutgoingPacket
     *
     * The outgoing DatagramPacket used for all sends.
     */
    private transient java.net.DatagramPacket __m_OutgoingPacket;
    
    /**
     * Property PacketLength
     *
     * This property controls both SendBufferSize and ReceiveBufferSize
     * settings of the underlying java.net.DatagramSocket.
     * 
     * @see java.net.DatagramSocket#setReceiveBufferSize
     * @see java.net.DatagramSocket#setSendBufferSize
     */
    private int __m_PacketLength;
    
    /**
     * Property RxDebugDropAddresses
     *
     * For testing only. If null then RxDebugDropRate applies to all packets,
     * if non-null it only applies to packets from InetSocketAddresses included
     * in this Set.
     */
    private transient java.util.Set __m_RxDebugDropAddresses;
    
    /**
     * Property RxDebugDropRate
     *
     * For testing only.  Forces this socket to drop a certain percentage of
     * incomming packets.  For performance reasons the rate is expressed as an
     * integer where 100,000 == 100%
     */
    private transient int __m_RxDebugDropRate;
    
    /**
     * Property SendLock
     *
     * Lock held while sending.  Note this used to simply be the
     * OutgoingPakcet, but an apparent bug (Bug 20122611) in JDK8u40 resulted
     * in the lock not being released even after the holding thread had
     * terminated.  Oddly switching to locking on a seperate Object appears to
     * work around this issue.  It may have someting to do with the locked
     * object being passed across then JNI boundary in DatagramSocketImpl?
     */
    private Object __m_SendLock;
    
    /**
     * Property TxDebugDropAddresses
     *
     * For testing only.  If null then TxDebugDropRate applies to all packets,
     * if non-null it only applies to packets from InetSocketAddresses included
     * in this Set.
     */
    private transient java.util.Set __m_TxDebugDropAddresses;
    
    /**
     * Property TxDebugDropRate
     *
     * For testing only.  Forces this socket to drop a certain percentage of
     * outgoing packets.  For performance reasons the rate is expressed as an
     * integer where 100,000 == 100%
     */
    private transient int __m_TxDebugDropRate;
    
    // Initializing constructor
    public UdpSocket(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
        {
        super(sName, compParent, false);
        }
    
    // Private initializer
    protected void __initPrivate()
        {
        
        super.__initPrivate();
        
        // state initialization: private properties
        try
            {
            __m_SendLock = new java.lang.Object();
            }
        catch (java.lang.Exception e)
            {
            // re-throw as a runtime exception
            throw new com.tangosol.util.WrapperException(e);
            }
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
            clz = Class.forName("com.tangosol.coherence/component/net/socket/UdpSocket".replace('/', '.'));
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
     * Bind the socket.
     */
    protected void bind(java.net.DatagramSocket socket)
            throws java.io.IOException
        {
        // import java.net.InetSocketAddress;
        
        socket.bind(new InetSocketAddress(getInetAddress(), getPort()));
        }
    
    // Declared at the super level
    public void close()
        {
        // import java.net.DatagramSocket;
        
        synchronized (getLock())
            {
            if (getState() != STATE_CLOSED)
                {
                DatagramSocket socket = getDatagramSocket();
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
                    }
        
                setState(STATE_CLOSED);
                }
            }
        }
    
    // Accessor for the property "BufferReceivedBytes"
    /**
     * Getter for property BufferReceivedBytes.<p>
    * Maximum number of packets to ask the OS to buffer when receiving.
     */
    public int getBufferReceivedBytes()
        {
        return __m_BufferReceivedBytes;
        }
    
    // Accessor for the property "BufferSentBytes"
    /**
     * Getter for property BufferSentBytes.<p>
    * Maximum number of packets to ask the OS to buffer when sending.
     */
    public int getBufferSentBytes()
        {
        return __m_BufferSentBytes;
        }
    
    // Accessor for the property "BytesReceived"
    /**
     * Getter for property BytesReceived.<p>
    * The number of bytes received by this socket.
     */
    public long getBytesReceived()
        {
        return __m_BytesReceived;
        }
    
    // Accessor for the property "BytesSent"
    /**
     * Getter for property BytesSent.<p>
    * The number of bytes sent by this socket.
     */
    public long getBytesSent()
        {
        return __m_BytesSent;
        }
    
    // Accessor for the property "CountReceived"
    /**
     * Getter for property CountReceived.<p>
    * The count of received packets.  Used only by the
    * SocketManager.refreshSocket()
     */
    public int getCountReceived()
        {
        return __m_CountReceived;
        }
    
    // Accessor for the property "CountSent"
    /**
     * Getter for property CountSent.<p>
    * The count of sent packets.  Used only by the SocketManager.refreshSocket()
     */
    public int getCountSent()
        {
        return __m_CountSent;
        }
    
    // Accessor for the property "DatagramSocket"
    /**
     * Getter for property DatagramSocket.<p>
    * The actual socket.
     */
    public java.net.DatagramSocket getDatagramSocket()
        {
        return __m_DatagramSocket;
        }
    
    // Accessor for the property "DatagramSocketProvider"
    /**
     * Getter for property DatagramSocketProvider.<p>
     */
    public com.tangosol.net.DatagramSocketProvider getDatagramSocketProvider()
        {
        return __m_DatagramSocketProvider;
        }
    
    // Accessor for the property "IncomingPacket"
    /**
     * Getter for property IncomingPacket.<p>
    * The DatagramPacket used for all receives.
     */
    public java.net.DatagramPacket getIncomingPacket()
        {
        return __m_IncomingPacket;
        }
    
    // Accessor for the property "OutgoingBuffer"
    /**
     * Getter for property OutgoingBuffer.<p>
    * The reusable BufferOutput used to write into the OutgoingPacket byte[].
     */
    public com.tangosol.io.WriteBuffer.BufferOutput getOutgoingBuffer()
        {
        // import com.tangosol.io.WriteBuffer$BufferOutput as com.tangosol.io.WriteBuffer.BufferOutput;
        
        com.tangosol.io.WriteBuffer.BufferOutput buffer = __m_OutgoingBuffer;
        
        buffer.setOffset(0);
        return buffer;
        }
    
    // Accessor for the property "OutgoingPacket"
    /**
     * Getter for property OutgoingPacket.<p>
    * The outgoing DatagramPacket used for all sends.
     */
    public java.net.DatagramPacket getOutgoingPacket()
        {
        return __m_OutgoingPacket;
        }
    
    // Accessor for the property "PacketLength"
    /**
     * Getter for property PacketLength.<p>
    * This property controls both SendBufferSize and ReceiveBufferSize settings
    * of the underlying java.net.DatagramSocket.
    * 
    * @see java.net.DatagramSocket#setReceiveBufferSize
    * @see java.net.DatagramSocket#setSendBufferSize
     */
    public int getPacketLength()
        {
        return __m_PacketLength;
        }
    
    // Accessor for the property "RxDebugDropAddresses"
    /**
     * Getter for property RxDebugDropAddresses.<p>
    * For testing only. If null then RxDebugDropRate applies to all packets, if
    * non-null it only applies to packets from InetSocketAddresses included in
    * this Set.
     */
    public java.util.Set getRxDebugDropAddresses()
        {
        return __m_RxDebugDropAddresses;
        }
    
    // Accessor for the property "RxDebugDropRate"
    /**
     * Getter for property RxDebugDropRate.<p>
    * For testing only.  Forces this socket to drop a certain percentage of
    * incomming packets.  For performance reasons the rate is expressed as an
    * integer where 100,000 == 100%
     */
    public int getRxDebugDropRate()
        {
        return __m_RxDebugDropRate;
        }
    
    // Accessor for the property "SendLock"
    /**
     * Getter for property SendLock.<p>
    * Lock held while sending.  Note this used to simply be the OutgoingPakcet,
    * but an apparent bug (Bug 20122611) in JDK8u40 resulted in the lock not
    * being released even after the holding thread had terminated.  Oddly
    * switching to locking on a seperate Object appears to work around this
    * issue.  It may have someting to do with the locked object being passed
    * across then JNI boundary in DatagramSocketImpl?
     */
    protected Object getSendLock()
        {
        return __m_SendLock;
        }
    
    // Accessor for the property "TxDebugDropAddresses"
    /**
     * Getter for property TxDebugDropAddresses.<p>
    * For testing only.  If null then TxDebugDropRate applies to all packets,
    * if non-null it only applies to packets from InetSocketAddresses included
    * in this Set.
     */
    public java.util.Set getTxDebugDropAddresses()
        {
        return __m_TxDebugDropAddresses;
        }
    
    // Accessor for the property "TxDebugDropRate"
    /**
     * Getter for property TxDebugDropRate.<p>
    * For testing only.  Forces this socket to drop a certain percentage of
    * outgoing packets.  For performance reasons the rate is expressed as an
    * integer where 100,000 == 100%
     */
    public int getTxDebugDropRate()
        {
        return __m_TxDebugDropRate;
        }
    
    // Declared at the super level
    public int hashCode()
        {
        // import com.tangosol.util.HashHelper;
        
        return HashHelper.hash(getInetAddress(), getPort());
        }
    
    /**
     * Set up the specified java.net.DatagramSocket.
     */
    protected void initializeDatagramSocket(java.net.DatagramSocket socket)
            throws java.io.IOException
        {
        // import com.tangosol.io.ByteArrayWriteBuffer;
        // import java.net.DatagramPacket;
        // import java.net.SocketException;
        
        int cbPacket = getPacketLength();
        _assert(cbPacket > 0, "UdpSocket.open: "
            + "PacketLength property is required and must be greater than zero");
        
        // Configure the underlying send and receive buffers.
        // Note: different JVMs and OSs will respond differently when the requested
        // size cannot be allocated.  Some with throw (IBM/AIX), others will ignore
        // and leave the default size (OSX), and some may give as much as they can
        // (Linux). We therefor need to use a backoff algorithm to see how much we
        // can manage to get.
        
        byte[] abPacket = new byte[cbPacket];
        
        setOutgoingBuffer(new ByteArrayWriteBuffer(abPacket).getBufferOutput());
        setOutgoingPacket(new DatagramPacket(abPacket, cbPacket));
        
        int cbSend = getBufferSentBytes();
        if (cbSend > 0)
            {
            int cbReq    = cbSend;
            int cbActual = socket.getSendBufferSize();
        
            for (; cbActual < cbReq; cbReq = Math.max(cbActual, (cbReq * 3) / 4))
                {
                try
                    {
                    socket.setSendBufferSize(cbReq);
                    }
                catch (SocketException e) {}
                cbActual = socket.getSendBufferSize();
                }
            
            validateBufferSize("send", cbActual, cbSend, cbPacket);
            }
        
        int cbRecv = getBufferReceivedBytes();
        if (cbRecv > 0)
            {
            int cbReq    = cbRecv;
            int cbActual = socket.getReceiveBufferSize();
        
            for (; cbActual < cbReq; cbReq = Math.max(cbActual, (cbReq * 3) / 4))
                {
                try
                    {
                    socket.setReceiveBufferSize(cbReq);
                    }
                catch (SocketException e) {}
                cbActual = socket.getReceiveBufferSize();
                }
            
            validateBufferSize("receive", cbActual, cbRecv, cbPacket);
            }
        
        int cMillis = getSoTimeout();
        if (cMillis >= 0)
            {
            socket.setSoTimeout(cMillis);
            validateSoTimeout(socket.getSoTimeout(), cMillis);
            }
        }
    
    /**
     * Instantiate an underlying java.net.DatagramSocket.
     */
    protected java.net.DatagramSocket instantiateDatagramSocket()
            throws java.io.IOException
        {
        return getDatagramSocketProvider().openDatagramSocket();
        }
    
    // Accessor for the property "IgnoreSendErrors"
    /**
     * Getter for property IgnoreSendErrors.<p>
    * Indicates if IOExceptions resulting from a send operation should be
    * ignored.  On some OSs such as OS X, plain IOExceptions are thrown to
    * indicate an error regarding the destination.  For instance "Host is
    * down", or "No route to host".  As these are just thrown as IOExceptions
    * it is not possible to identify them other then via exception message
    * comparisions.
    * 
    * While the send method has some message hard coded, we can't know all
    * possible variants (including locale specific ones), we have this flag to
    * simply ignore all IOExceptions thrown from the send method.
    * 
    * The default value for the process may be controlled via the
    * tangosol.coherence.udp.ignoretxerror system property.
     */
    public boolean isIgnoreSendErrors()
        {
        return __m_IgnoreSendErrors;
        }
    
    /**
     * For testing only, returns true if the packet should be dropped rather
    * then processed.
    * 
    * Note: private to encourage Inlining
     */
    private boolean isRxDebugDrop(java.net.SocketAddress socketAddress)
        {
        // import com.tangosol.util.Base;
        // import java.util.Set;
        
        int iDropRate = getRxDebugDropRate();
        if (iDropRate == 0 || iDropRate < Base.getRandom().nextInt(100000))
            {
            return false;
            }
        
        Set setAddress = getRxDebugDropAddresses();
        return setAddress == null || setAddress.contains(socketAddress);
        }
    
    /**
     * For testing only, returns true if the packet should be dropped rather
    * then transmitted.
    * 
    * Note: private to encourage Inlining
     */
    private boolean isTxDebugDrop(java.net.SocketAddress socketAddress)
        {
        // import com.tangosol.util.Base;
        // import java.util.Set;
        
        int iDropRate = getTxDebugDropRate();
        if (iDropRate == 0 || iDropRate < Base.getRandom().nextInt(100000))
            {
            return false;
            }
        
        Set setAddress = getTxDebugDropAddresses();
        return setAddress == null || setAddress.contains(socketAddress);
        }
    
    // Declared at the super level
    /**
     * Generic level for handling a Socket exception
    * 
    * @param eException  the causal exception
     */
    public void onException(java.io.IOException eException)
        {
        super.onException(eException);
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
        // import com.tangosol.coherence.config.Config;
        // import java.net.DatagramPacket;
        
        setIgnoreSendErrors(Config.getBoolean("coherence.udp.ignoretxerror"));
        setIncomingPacket(new DatagramPacket(new byte[0], 0));
        
        super.onInit();
        }
    
    /**
     * 
    * @param eException  the causal exception
    * @param lSocketActionMillis  the time that the exception occurred (or the
    * enclosing operation began or was in progress)
     */
    protected void onReceiveException(java.io.IOException eException)
        {
        onException(eException);
        }
    
    /**
     * 
    * @param eException  the causal exception
    * @param lSocketActionMillis  the time that the exception occurred (or the
    * enclosing operation began or was in progress)
     */
    protected void onSendException(java.io.IOException eException)
        {
        onException(eException);
        }
    
    // Declared at the super level
    public void open()
            throws java.io.IOException
        {
        // import com.tangosol.util.Base;
        // import java.io.IOException;
        // import java.net.DatagramSocket;
        
        synchronized (getLock())
            {
            if (getState() != STATE_OPEN)
                {
                DatagramSocket socket = null;
                try
                    {
                    socket = instantiateDatagramSocket();        
                    initializeDatagramSocket(socket);
        
                    bind(socket);
                    if (getPort() == 0)
                        {
                        setPort(socket.getLocalPort());
                        }
        
                    setDatagramSocket(socket);
                    }
                catch (IOException e)
                    {
                    try
                        {
                        socket.close();
                        }
                    catch (Exception eIgnore) {}
                    throw e;
                    }
        
                setCountSent(0);
                setCountReceived(0);
                setBytesSent(0L);
                setBytesReceived(0L);
                setState(STATE_OPEN);
                }
            }
        }
    
    /**
     * Rebind a wildcard bound socket to a more specific IP.
     */
    public void rebind(java.net.InetAddress addr)
            throws java.io.IOException
        {
        // import java.io.IOException;
        // import java.net.DatagramSocket;
        // import java.net.InetSocketAddress;
        
        DatagramSocket socket = getDatagramSocket();
        if (!socket.getLocalAddress().isAnyLocalAddress())
            {
            throw new IOException("not bound to wildcard address");
            }
        
        DatagramSocket socketNew = instantiateDatagramSocket();
        initializeDatagramSocket(socketNew);
        
        synchronized (getLock())
            {
            int nPort = socket.getLocalPort(); // must obtain before close
            socket.close();
        
            // there may be a thread in receive, and if so that could temporarily block
            // us from being able to re-bind to the same port, ensure that thread is gone
            synchronized (getIncomingPacket())
                {
                socketNew.bind(new InetSocketAddress(addr, nPort));
                }
        
            setInetAddress(addr);
            setDatagramSocket(socketNew);
            }
        }
    
    /**
     * @return the address associated with the received buffer, or null if no
    * packet was received
     */
    public java.net.SocketAddress receive(java.nio.ByteBuffer buffer)
        {
        // import com.tangosol.net.messaging.ConnectionException;
        // import java.io.InterruptedIOException;
        // import java.io.IOException;
        // import java.net.DatagramPacket;
        // import java.net.DatagramSocket;
        
        DatagramSocket socket = getDatagramSocket();
        
        try
            {
            int            cPos     = buffer.position();            
            DatagramPacket dgPacket = getIncomingPacket();
            int            cb;
            synchronized (dgPacket)
                {
                dgPacket.setData(buffer.array(),
                     buffer.arrayOffset() + cPos, buffer.remaining());
                socket.receive(dgPacket);
                cb = dgPacket.getLength();
        
                if (getRxDebugDropRate() !=0 && isRxDebugDrop(dgPacket.getSocketAddress()))
                    {
                    return null; // drop the packet for debugging
                    }
                }
        
            buffer.limit(cPos + cb);
            setCountReceived(getCountReceived() + 1);
            setBytesReceived(getBytesReceived() + cb);
            
            return dgPacket.getSocketAddress();
            }
        catch (InterruptedIOException e)
            {
            onInterruptedIOException(e);
            }
        catch (IOException e)
            {
            if (socket.isClosed())
                {
                synchronized (getLock())
                    {
                    if (getDatagramSocket() == socket)
                        {
                        throw new ConnectionException(e);
                        }
                    else
                        {
                        // concurrent rebind
                        return null;
                        }
                    }
                }
            onReceiveException(e);
            }
        
        return null;
        }
    
    public void send(com.tangosol.coherence.component.net.PacketBundle bundle)
        {
        // import com.tangosol.net.messaging.ConnectionException;
        // import java.io.IOException;
        // import java.net.DatagramPacket;
        // import java.net.DatagramSocket;
        // import java.net.InetSocketAddress;
        
        IOException    eIO       = null;
        DatagramSocket socket    = getDatagramSocket();
        int            iDropRate = getTxDebugDropRate();
        
        try
            {
            synchronized (getSendLock())
                {
                DatagramPacket packet = getOutgoingPacket();
                int cb = bundle.write(getOutgoingBuffer());
                if (cb > 0)
                    {
                    int cAddr = bundle.getAddressCount();
                    packet.setLength(cb);
                    for (int i = 0; i < cAddr; i++)
                        {
                        InetSocketAddress addr = (InetSocketAddress) bundle.getAddress(i);
        
                        if (iDropRate == 0 || !isTxDebugDrop(addr))
                            {
                            packet.setAddress(addr.getAddress());
                            packet.setPort(addr.getPort());
                            try
                                {
                                socket.send(packet);
                                }
                            catch (IOException e)
                                {
                                // hold exception so that we can try on all addresses in bundle
                                // before handling.  This is important if the send error is
                                // because of the target address.
                                eIO = e;
                                }
                            }
                        }
                    setCountSent(getCountSent() + cAddr);
                    setBytesSent(getBytesSent() + cb * cAddr);
        
                    if (eIO != null)
                        {
                        throw eIO;
                        }
                    }
                return;
                }
            }
        catch (IOException e)
            {
            if (socket.isClosed())
                {
                synchronized (getLock())
                    {
                    if (getDatagramSocket() == socket)
                        {
                        throw new ConnectionException(e);
                        }
                    else
                        {
                        // concurrent rebind
                        return;
                        }
                    }
                }
            onSendException(e);
            return; // it's UDP we don't need to retry the send        
            }
        }
    
    // Accessor for the property "BufferReceivedBytes"
    /**
     * Setter for property BufferReceivedBytes.<p>
    * Maximum number of packets to ask the OS to buffer when receiving.
     */
    public void setBufferReceivedBytes(int cPackets)
        {
        synchronized (getLock())
            {
            _assert(getState() != STATE_OPEN,
                "BufferReceived cannot be modified once the socket is open");
        
            __m_BufferReceivedBytes = (cPackets);
            }
        }
    
    // Accessor for the property "BufferSentBytes"
    /**
     * Setter for property BufferSentBytes.<p>
    * Maximum number of packets to ask the OS to buffer when sending.
     */
    public void setBufferSentBytes(int cPackets)
        {
        synchronized (getLock())
            {
            _assert(getState() != STATE_OPEN,
                "BufferSent cannot be modified once the socket is open");
        
            __m_BufferSentBytes = (cPackets);
            }
        }
    
    // Accessor for the property "BytesReceived"
    /**
     * Setter for property BytesReceived.<p>
    * The number of bytes received by this socket.
     */
    public void setBytesReceived(long cBytes)
        {
        __m_BytesReceived = cBytes;
        }
    
    // Accessor for the property "BytesSent"
    /**
     * Setter for property BytesSent.<p>
    * The number of bytes sent by this socket.
     */
    public void setBytesSent(long cBytes)
        {
        __m_BytesSent = cBytes;
        }
    
    // Accessor for the property "CountReceived"
    /**
     * Setter for property CountReceived.<p>
    * The count of received packets.  Used only by the
    * SocketManager.refreshSocket()
     */
    protected void setCountReceived(int cReceived)
        {
        __m_CountReceived = cReceived;
        }
    
    // Accessor for the property "CountSent"
    /**
     * Setter for property CountSent.<p>
    * The count of sent packets.  Used only by the SocketManager.refreshSocket()
     */
    protected void setCountSent(int cSent)
        {
        __m_CountSent = cSent;
        }
    
    // Accessor for the property "DatagramSocket"
    /**
     * Setter for property DatagramSocket.<p>
    * The actual socket.
     */
    protected void setDatagramSocket(java.net.DatagramSocket socket)
        {
        __m_DatagramSocket = socket;
        }
    
    // Accessor for the property "DatagramSocketProvider"
    /**
     * Setter for property DatagramSocketProvider.<p>
     */
    public void setDatagramSocketProvider(com.tangosol.net.DatagramSocketProvider providerSocket)
        {
        __m_DatagramSocketProvider = providerSocket;
        }
    
    // Accessor for the property "IgnoreSendErrors"
    /**
     * Setter for property IgnoreSendErrors.<p>
    * Indicates if IOExceptions resulting from a send operation should be
    * ignored.  On some OSs such as OS X, plain IOExceptions are thrown to
    * indicate an error regarding the destination.  For instance "Host is
    * down", or "No route to host".  As these are just thrown as IOExceptions
    * it is not possible to identify them other then via exception message
    * comparisions.
    * 
    * While the send method has some message hard coded, we can't know all
    * possible variants (including locale specific ones), we have this flag to
    * simply ignore all IOExceptions thrown from the send method.
    * 
    * The default value for the process may be controlled via the
    * tangosol.coherence.udp.ignoretxerror system property.
     */
    public void setIgnoreSendErrors(boolean fIgnore)
        {
        __m_IgnoreSendErrors = fIgnore;
        }
    
    // Accessor for the property "IncomingPacket"
    /**
     * Setter for property IncomingPacket.<p>
    * The DatagramPacket used for all receives.
     */
    protected void setIncomingPacket(java.net.DatagramPacket packetIncoming)
        {
        __m_IncomingPacket = packetIncoming;
        }
    
    // Accessor for the property "OutgoingBuffer"
    /**
     * Setter for property OutgoingBuffer.<p>
    * The reusable BufferOutput used to write into the OutgoingPacket byte[].
     */
    protected void setOutgoingBuffer(com.tangosol.io.WriteBuffer.BufferOutput output)
        {
        __m_OutgoingBuffer = output;
        }
    
    // Accessor for the property "OutgoingPacket"
    /**
     * Setter for property OutgoingPacket.<p>
    * The outgoing DatagramPacket used for all sends.
     */
    protected void setOutgoingPacket(java.net.DatagramPacket packetOutgoing)
        {
        __m_OutgoingPacket = packetOutgoing;
        }
    
    // Accessor for the property "PacketLength"
    /**
     * Setter for property PacketLength.<p>
    * This property controls both SendBufferSize and ReceiveBufferSize settings
    * of the underlying java.net.DatagramSocket.
    * 
    * @see java.net.DatagramSocket#setReceiveBufferSize
    * @see java.net.DatagramSocket#setSendBufferSize
     */
    public void setPacketLength(int cb)
        {
        synchronized (getLock())
            {
            _assert(getState() != STATE_OPEN,
                "PacketLength cannot be modified once the socket is open");
        
            __m_PacketLength = (cb);
            }
        }
    
    // Accessor for the property "RxDebugDropAddresses"
    /**
     * Setter for property RxDebugDropAddresses.<p>
    * For testing only. If null then RxDebugDropRate applies to all packets, if
    * non-null it only applies to packets from InetSocketAddresses included in
    * this Set.
     */
    public void setRxDebugDropAddresses(java.util.Set set)
        {
        __m_RxDebugDropAddresses = set;
        }
    
    // Accessor for the property "RxDebugDropRate"
    /**
     * Setter for property RxDebugDropRate.<p>
    * For testing only.  Forces this socket to drop a certain percentage of
    * incomming packets.  For performance reasons the rate is expressed as an
    * integer where 100,000 == 100%
     */
    public void setRxDebugDropRate(int iRate)
        {
        // import java.util.Set;
        
        if (iRate != getRxDebugDropRate())
            {
            Set setAddr = getRxDebugDropAddresses();
            __m_RxDebugDropRate = (iRate);
            _trace("Configuring " + this + " to drop " + ((float) iRate) / 1000f +
                "% of incoming packets" + (setAddr == null ? "." : " from " + setAddr), 2);
            }
        }
    
    // Accessor for the property "SendLock"
    /**
     * Setter for property SendLock.<p>
    * Lock held while sending.  Note this used to simply be the OutgoingPakcet,
    * but an apparent bug (Bug 20122611) in JDK8u40 resulted in the lock not
    * being released even after the holding thread had terminated.  Oddly
    * switching to locking on a seperate Object appears to work around this
    * issue.  It may have someting to do with the locked object being passed
    * across then JNI boundary in DatagramSocketImpl?
     */
    private void setSendLock(Object oLock)
        {
        __m_SendLock = oLock;
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
        // import java.net.DatagramSocket;
        // import java.net.SocketException;
        
        if (cMillis >= 0)
            {
            synchronized (getLock())
                {
                if (getState() == STATE_OPEN)
                    {
                    DatagramSocket socket = getDatagramSocket();
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
    
    // Accessor for the property "TxDebugDropAddresses"
    /**
     * Setter for property TxDebugDropAddresses.<p>
    * For testing only.  If null then TxDebugDropRate applies to all packets,
    * if non-null it only applies to packets from InetSocketAddresses included
    * in this Set.
     */
    public void setTxDebugDropAddresses(java.util.Set set)
        {
        __m_TxDebugDropAddresses = set;
        }
    
    // Accessor for the property "TxDebugDropRate"
    /**
     * Setter for property TxDebugDropRate.<p>
    * For testing only.  Forces this socket to drop a certain percentage of
    * outgoing packets.  For performance reasons the rate is expressed as an
    * integer where 100,000 == 100%
     */
    public void setTxDebugDropRate(int iRate)
        {
        // import java.util.Set;
        
        if (iRate != getTxDebugDropRate())
            {
            Set setAddr = getTxDebugDropAddresses();
            __m_TxDebugDropRate = (iRate);
            _trace("Configuring " + this + " to drop " + ((float) iRate) / 1000f +
                    "% of outgoing packets" + (setAddr == null ? "." : " to " + setAddr), 2);
            }
        }
    
    // Declared at the super level
    protected void validateBufferSize(String sBufferName, int cbActualSize, int cbRequestedSize, int cbMinimumSize)
        {
        // import com.tangosol.util.Base;
        
        if (cbActualSize < cbRequestedSize)
            {
            int nPct = (cbActualSize * 100) / cbRequestedSize; 
            int iPacketLength = getPacketLength();
            String sMsg = get_Name() + " failed to set " + sBufferName +
                " buffer size to " + cbRequestedSize / iPacketLength + " packets (" +
                Base.toMemorySizeString(cbRequestedSize, false) + "); actual size is " +
                nPct + "%, " + cbActualSize / iPacketLength + " packets (" +
                Base.toMemorySizeString(cbActualSize, false) + "). Consult your OS " +
                "documentation regarding increasing the maximum socket buffer size.";
        
            if (cbActualSize < cbMinimumSize)
                {
                // under minimum log error, and throw
                 _trace(sMsg, 1);
                throw new RuntimeException(sMsg);
                }
            else if (nPct < 80)
                {
                // over minimum, just log warning, and continue
                sMsg += " Proceeding with the actual value may cause sub-optimal performance.";
                _trace(sMsg, nPct < 50 ? 2 : 6);        
                }
            }
        }
    }
