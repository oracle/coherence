
/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.extend.Connection

package com.tangosol.coherence.component.net.extend;

import com.tangosol.net.Member;
import com.tangosol.net.messaging.ConnectionAcceptor;
import com.tangosol.net.messaging.ConnectionException;
import com.tangosol.util.Base;
import com.tangosol.util.LongArray;
import com.tangosol.util.ThreadGate;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Base definition of a Connection component.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class Connection
        extends    com.tangosol.coherence.component.net.Extend
        implements com.tangosol.net.messaging.Connection
    {
    // ---- Fields declarations ----
    
    /**
     * Property ChannelArray
     *
     * A LongArray of open Channel objects created by this Connection, indexed
     * by Channel identifier.
     */
    private com.tangosol.util.LongArray __m_ChannelArray;
    
    /**
     * Property ChannelPendingArray
     *
     * A LongArray of newly created Channel objects that have not yet accepted
     * by the peer, indexed by Channel identifier.
     */
    private com.tangosol.util.LongArray __m_ChannelPendingArray;
    
    /**
     * Property CloseNotify
     *
     * Peer notification flag used when the Connection is closed upon exiting
     * the ThreadGate (see CloseOnExit property).
     * 
     * @volatile
     */
    private volatile boolean __m_CloseNotify;
    
    /**
     * Property CloseOnExit
     *
     * If true, the Thread that is currently executing within the Connection
     * should close the Connection immedately upon exiting the Connection's
     * ThreadGate.
     * 
     * @volatile
     */
    private volatile boolean __m_CloseOnExit;
    
    /**
     * Property CloseThrowable
     *
     * The Throwable to pass to the close() method when the Connection is
     * closed upon exiting the ThreadGate (see CloseOnExit property).
     * 
     * @volatile
     */
    private volatile Throwable __m_CloseThrowable;
    
    /**
     * Property ConnectionManager
     *
     * The ConnectionManager that created or accepted this Connection.
     * 
     * @volatile
     * @see com.tangosol.net.messaging.Connection#getConnectionManager
     */
    private volatile com.tangosol.net.messaging.ConnectionManager __m_ConnectionManager;
    
    /**
     * Property Id
     *
     * The unique identifier of this Connection object.
     * 
     * @see com.tangosol.net.messaging.Connection#getId
     */
    private com.tangosol.util.UUID __m_Id;
    
    /**
     * Property MAX_PENDING
     *
     * The maximum number of pending new Channel objects. If the limit is
     * reached, a pending Channel will be discarded.
     */
    private static final int MAX_PENDING = 100;
    
    /**
     * Property Member
     *
     * The optional Member object associated with this Connection.
     */
    private com.tangosol.net.Member __m_Member;
    
    /**
     * Property MessageFactoryMap
     *
     * A Map of MessageFactory objects that may be used by Channel objects
     * created by this Connection, keyed by Protocol name.
     */
    private transient java.util.Map __m_MessageFactoryMap;
    
    /**
     * Property MessagingDebug
     *
     * Debug flag.  When true and the node's logging level is 6 or higher, sent
     * and received messages will be logged.
     */
    private boolean __m_MessagingDebug;
    
    /**
     * Property Open
     *
     * True if the Connection is open; false otherwise.
     * 
     * @see com.tangosol.net.messaging.Connection#isOpen
     * @volatile
     */
    private volatile boolean __m_Open;
    
    /**
     * Property PeerEdition
     *
     * The product edition used by the peer.
     * 
     * @see Component.Application.Console.Coherence#getEdition
     */
    private int __m_PeerEdition;
    
    /**
     * Property PeerId
     *
     * The unique identifier of the peer process to which this Connection
     * object is connected.
     * 
     * @see com.tangosol.net.messaging.Connection#getPeerId
     */
    private com.tangosol.util.UUID __m_PeerId;
    
    /**
     * Property PingLastMillis
     *
     * The send time of the last outstanding PingRequest or 0 if a PingRequest
     * is not outstanding.
     */
    private transient long __m_PingLastMillis;
    
    /**
     * Property StatsBytesReceived
     *
     * Statistics: total number of bytes received over this Connection.
     */
    private transient long __m_StatsBytesReceived;
    
    /**
     * Property StatsBytesSent
     *
     * Statistics: total number of bytes sent over this Connection.
     */
    private transient long __m_StatsBytesSent;
    
    /**
     * Property StatsReceived
     *
     * Statistics: total number of Messages received over this Connection.
     */
    private transient long __m_StatsReceived;
    
    /**
     * Property StatsReset
     *
     * Statistics: date/time value that the stats have been reset.
     */
    private transient long __m_StatsReset;
    
    /**
     * Property StatsSent
     *
     * Statistics: total number of messages sent over this Connection.
     */
    private transient long __m_StatsSent;
    
    /**
     * Property ThreadGate
     *
     * A ThreadGate used to prevent concurrent use of this Connection while it
     * is being closed.
     */
    private com.tangosol.util.ThreadGate __m_ThreadGate;
    
    // Default constructor
    public Connection()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public Connection(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
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
        return new com.tangosol.coherence.component.net.extend.Connection();
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
            clz = Class.forName("com.tangosol.coherence/component/net/extend/Connection".replace('/', '.'));
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
    
    // From interface: com.tangosol.net.messaging.Connection
    public com.tangosol.net.messaging.Channel acceptChannel(java.net.URI uri, ClassLoader loader, com.tangosol.net.messaging.Channel.Receiver receiver, javax.security.auth.Subject subject)
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Peer as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer;
        
        return ((com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer) getConnectionManager()).acceptChannel(this, uri, loader,
                receiver, subject);
        }
    
    /**
     * The acceptChannel() implementation method. This method is called on the
    * service thread.
    * 
    * @see com.tangosol.net.messaging.Connection#acceptChannel
     */
    public com.tangosol.net.messaging.Request.Status acceptChannelInternal(java.net.URI uri, com.tangosol.io.Serializer serializer, com.tangosol.net.messaging.Channel.Receiver receiver, javax.security.auth.Subject subject, byte[] abToken)
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Peer$MessageFactory$AcceptChannelRequest as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.AcceptChannelRequest;
        // import com.tangosol.net.messaging.Protocol$MessageFactory as com.tangosol.net.messaging.Protocol.MessageFactory;
        
        assertOpen();
        
        if (serializer == null)
            {
            throw new IllegalArgumentException("serializer cannot be null");
            }
        
        int nId;
        try
            {
            nId = Integer.parseInt(uri.getSchemeSpecificPart());
            }
        catch (RuntimeException e)
            {
            throw new IllegalArgumentException("illegal URI: " + uri);
            }
        
        if (nId == 0)
            {
            throw new IllegalArgumentException("channel 0 is reserved");
            }
        
        if (getChannel(nId) != null)
            {
            throw new IllegalArgumentException("duplicate channel: " + nId);
            }
        
        String sProtocol = uri.getFragment();
        if (sProtocol == null)
            {
            throw new IllegalArgumentException("illegal URI: " + uri);
            }
        
        com.tangosol.net.messaging.Protocol.MessageFactory factory = (com.tangosol.net.messaging.Protocol.MessageFactory) getMessageFactoryMap().get(sProtocol);
        if (factory == null)
            {
            throw new IllegalArgumentException("unknown protocol: " + sProtocol);
            }
        
        if (receiver != null)
            {
            if (receiver.getProtocol() != factory.getProtocol())
                {
                throw new IllegalArgumentException("protocol mismatch; expected "
                        + factory.getProtocol() + ", retrieved "
                        + receiver.getProtocol() + ")");
                }
            }
        
        // send a AcceptChannelRequest to the peer via "Channel0"
        Channel channel0 = (Channel) getChannel(0);
        com.tangosol.net.messaging.Protocol.MessageFactory factory0 = (com.tangosol.net.messaging.Protocol.MessageFactory) channel0.getMessageFactory();
        com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.AcceptChannelRequest request  = (com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.AcceptChannelRequest) factory0.createMessage(com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.AcceptChannelRequest.TYPE_ID);
        
        request.setChannelId(nId);
        request.setIdentityToken(abToken);
        request.setMessageFactory(factory);
        request.setProtocolName(sProtocol);
        request.setReceiver(receiver);
        request.setSerializer(serializer);
        request.setSubject(subject);
        
        return channel0.send(request);
        }
    
    /**
     * The acceptChannel() recipient implementation method. This method is
    * called on the service thread in response to a ChannelAcceptRequest.
    * 
    * @see com.tangosol.net.messaging.Connection#acceptChannel
     */
    public void acceptChannelRequest(int nId, javax.security.auth.Subject subject, com.tangosol.internal.net.security.AccessAdapter adapter)
        {
        // import com.tangosol.net.messaging.Protocol$MessageFactory as com.tangosol.net.messaging.Protocol.MessageFactory;
        
        if (nId == 0)
            {
            throw new IllegalArgumentException("channel 0 is reserved");
            }
        
        if (getChannel(nId) != null)
            {
            throw new IllegalArgumentException("channel already exists: " + nId);
            }
        
        Channel channel = (Channel) getChannelPendingArray().remove(nId);
        if (channel == null)
            {
            throw new IllegalArgumentException("no such channel: " + nId);
            }
        
        channel.setSubject(subject);
        channel.setAccessAdapter(adapter);
        channel.openInternal();
        
        registerChannel(channel);
        }
    
    /**
     * The acceptChannel() initiator implementation method. This method is
    * called on the service thread in response to a ChannelAcceptResponse.
    * 
    * @return the newly accepted Channel
    * 
    * @see com.tangosol.net.messaging.Connection#acceptChannel
     */
    public com.tangosol.net.messaging.Channel acceptChannelResponse(int nId, com.tangosol.net.messaging.Protocol.MessageFactory factory, com.tangosol.io.Serializer serializer, com.tangosol.net.messaging.Channel.Receiver receiver, javax.security.auth.Subject subject)
        {
        // import com.tangosol.net.messaging.Protocol$MessageFactory as com.tangosol.net.messaging.Protocol.MessageFactory;
        
        assertOpen();
        
        if (factory == null)
            {
            throw new IllegalArgumentException("factory cannot be null");
            }
        
        if (serializer == null)
            {
            throw new IllegalArgumentException("serializer cannot be null");
            }
        
        Channel channel = new Channel();
        channel.setId(nId);
        channel.setConnection(this);
        channel.setMessageFactory(factory);
        channel.setReceiver(receiver);
        channel.setSerializer(serializer);
        channel.setSubject(subject);
        channel.openInternal();
        
        registerChannel(channel);
        
        return channel;
        }
    
    /**
     * @throws ConnectionException if the Connection is closed or closing
     */
    protected void assertOpen()
        {
        // import com.tangosol.net.messaging.ConnectionException;
        
        if (!isOpen())
            {
            // REVIEW
            throw new ConnectionException("connection is closed", this);
            }
        }
    
    // From interface: com.tangosol.net.messaging.Connection
    /**
     * Close the Connection.
     */
    public void close()
        {
        close(/*fNotify*/ true, null);
        }
    
    /**
     * Close the Connection.
    * 
    * @param fNotify  if true, notify the peer that the Connection is being
    * closed
    * @param e  the optional reason why the Connection is being closed
     */
    public void close(boolean fNotify, Throwable e)
        {
        close(fNotify, e, /*fWait*/ true);
        }
    
    /**
     * Close the Connection.
    * 
    * @param fNotify  if true, notify the peer that the Connection is being
    * closed
    * @param e  the optional reason why the Connection is being closed
    * @param fWait  if true wait for the ConnectionManager to close the
    * Connection when called on a client thread. This method will always wait
    * for the ConnectionManager to close the Connection if called on the
    * service thread.
     */
    public void close(boolean fNotify, Throwable e, boolean fWait)
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Peer as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer;
        
        if (isOpen())
            {
            com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer manager = (com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer) getConnectionManager();
            if (Thread.currentThread() == manager.getThread())
                {
                closeInternal(fNotify, e, 0L);
                }
            else
                {
                _assert(!isActiveThread(),
                        "cannot close a connection while executing within the connection");
        
                manager.closeConnection(this, fNotify, e, fWait);
                }
            }
        }
    
    /**
     * The close() implementation method. This method is called on the service
    * thread.
    * 
    * @param fNotify  if true, notify the peer that the Connection is being
    * closed
    * @param e  the optional reason why the Connection is being closed
    * @param cMillis  the number of milliseconds to wait for the Connection to
    * close; pass 0L to perform a non-blocking close or -1L to wait forever
    * 
    * @return true iff the invocation of this method closed the Connection
     */
    public boolean closeInternal(boolean fNotify, Throwable e, long cMillis)
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Peer as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer;
        // import Component.Util.Daemon.QueueProcessor.Service.Peer$MessageFactory$NotifyConnectionClosed as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.NotifyConnectionClosed;
        // import com.tangosol.net.messaging.Protocol$MessageFactory as com.tangosol.net.messaging.Protocol.MessageFactory;
        // import com.tangosol.util.LongArray;
        // import java.util.Iterator;
        
        if (!isOpen())
            {
            return false;
            }
        
        // close all open Channels, except for "Channel0"
        Channel   channel0;
        LongArray la = getChannelArray();
        synchronized (la)
            {
            channel0 = (Channel) la.get(0);
            for (Iterator iter = la.iterator(); iter.hasNext(); )
                {
                Channel channel = (Channel) iter.next();
                if (channel != channel0)
                    {
                    iter.remove();
                    channel.closeInternal(false, e, 0L);
                    }
                }
            }
        
        boolean fClose = gateClose(cMillis);
        try
            {
            if (!fClose)
                {
                // can't close the gate; signal to the holding Thread(s) that it
                // must close the Connection immediately after exiting the gate
                setCloseOnExit(true);
                setCloseNotify(fNotify);
                setCloseThrowable(e);
        
                // double check if we can close the gate, as we want to be sure
                // that the Thread(s) saw the close notification prior to exiting
                fClose = gateClose(0L);
                }
        
            if (fClose && isOpen())
                {
                // notify the peer that the Connection is now closed
                if (fNotify)
                    {
                    // send a NotifyConnectionClosed to the peer via "Channel0"
                    try
                        {
                        com.tangosol.net.messaging.Protocol.MessageFactory factory0 = channel0.getMessageFactory();
                        com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.NotifyConnectionClosed message  = (com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.NotifyConnectionClosed) factory0.createMessage(com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.NotifyConnectionClosed.TYPE_ID);
        
                        message.setCause(e);
                        channel0.send(message);
                        }
                    catch (RuntimeException ee) {}
                    }
        
                // clean up
                channel0.closeInternal(false, e, -1L);
                getChannelPendingArray().clear();
                setPeerId(null);
        
                setOpen(false);
                }
            else
                {
                return false;
                }
            }
        finally
            {
            if (fClose)
                {
                gateOpen();
                }
            }
        
        // notify the ConnectionManager that the Connection is now closed
        com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer manager = (com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer) getConnectionManager();
        if (e == null)
            {
            manager.onConnectionClosed(this);
            }
        else
            {
            manager.onConnectionError(this, e);
            }
        
        return true;
        }
    
    // From interface: com.tangosol.net.messaging.Connection
    public java.net.URI createChannel(com.tangosol.net.messaging.Protocol protocol, ClassLoader loader, com.tangosol.net.messaging.Channel.Receiver receiver)
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Peer as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer;
        
        com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer manager = (com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer) getConnectionManager();
        if (Thread.currentThread() == manager.getThread())
            {
            return createChannelInternal(protocol,
                    manager.ensureSerializer(loader),
                    receiver);
            }
        else
            {
            return manager.createChannel(this, protocol, loader, receiver);
            }
        }
    
    /**
     * The createChannel() implementation method. This method is called on the
    * service thread.
    * 
    * @see com.tangosol.net.messaging.Connection#createChannel
     */
    public java.net.URI createChannelInternal(com.tangosol.net.messaging.Protocol protocol, com.tangosol.io.Serializer serializer, com.tangosol.net.messaging.Channel.Receiver receiver)
        {
        // import com.tangosol.net.messaging.ConnectionException;
        // import com.tangosol.net.messaging.Protocol$MessageFactory as com.tangosol.net.messaging.Protocol.MessageFactory;
        // import com.tangosol.util.LongArray;
        // import java.net.URI;
        // import java.net.URISyntaxException;
        
        assertOpen();
        
        if (protocol == null)
            {
            throw new IllegalArgumentException("protocol cannot be null");
            }
        
        String sProtocol = protocol.getName();
        if (sProtocol == null)
            {
            throw new IllegalArgumentException("missing protocol name: " + protocol);
            }
        
        com.tangosol.net.messaging.Protocol.MessageFactory factory = (com.tangosol.net.messaging.Protocol.MessageFactory) getMessageFactoryMap().get(sProtocol);
        if (factory == null)
            {
            throw new IllegalArgumentException("unsupported protocol: " + protocol);
            }
        
        int nId = generateChannelId();
        
        // create a new Channel
        Channel channel = new Channel();
        channel.setId(nId);
        channel.setConnection(this);
        channel.setReceiver(receiver);
        channel.setMessageFactory(factory);
        channel.setSerializer(serializer);
        
        // add the new Channel to the pending array; log a warning if the number
        // of pending channels is high
        LongArray la   = getChannelPendingArray();
        int       size = la.getSize();
        if (size > MAX_PENDING)
            {
            _trace("There is a high number of pending open channel requests [" + size + "] for connection="
                    + this, 2);
            }
        la.set(nId, channel);
        
        try
            {
            return new URI("channel", String.valueOf(nId), sProtocol);
            }
        catch (URISyntaxException e)
            {
            la.remove(nId);
            throw ensureRuntimeException(e, "error creating URI");
            }
        }
    
    /**
     * Attempt to close the Connection ThreadGate.
    * 
    * @param cMillis  the number of milliseconds to wait for the ThreadGate to
    * close; pass 0L to perform a non-blocking close or -1L to wait forever
    * 
    * @return true if the Connection ThreadGate was closed; false otherwise
     */
    protected boolean gateClose(long cMillis)
        {
        return getThreadGate().close(cMillis);
        }
    
    /**
     * Enter the Connection ThreadGate.
    * 
    * @throws ConnectionException if the Connection is closing or closed
     */
    public void gateEnter()
        {
        // import com.tangosol.net.messaging.ConnectionException;
        // import com.tangosol.util.ThreadGate;
        
        ThreadGate gate = getThreadGate();
        
        // if the thread is entering for the first time, throw an exception if the
        // Connection has been marked for close; this prevents new threads from
        // entering the Connection and thus keeping it open longer than necessary
        if (isCloseOnExit() && !gate.isEnteredByCurrentThread())
            {
            // REVIEW
            throw new ConnectionException("connection is closing", this);
            }
        
        if (gate.enter(0L)) // see #gateClose
            {
            try
                {
                assertOpen();
                }
            catch (Throwable e)
                {
                gate.exit();
                throw ensureRuntimeException(e);
                }
            }
        else
            {
            // REVIEW
            throw new ConnectionException("connection is closing", this);
            }
        }
    
    /**
     * Exit the Connection ThreadGate.
     */
    public void gateExit()
        {
        // import com.tangosol.util.ThreadGate;
        
        ThreadGate gate = getThreadGate();
        gate.exit();
        
        // see if we've been asked to close the Connection
        if (isCloseOnExit() && !gate.isEnteredByCurrentThread())
            {
            boolean fClose = gateClose(0L);
            try
                {
                if (fClose && isOpen())
                    {
                    gateOpen();
                    fClose = false;
                    close(isCloseNotify(), getCloseThrowable());
                    }
                }
            finally
                {
                if (fClose)
                    {
                    gateOpen();
                    }
                }
            }
        }
    
    /**
     * Open the Connection ThreadGate.
     */
    protected void gateOpen()
        {
        getThreadGate().open();
        }
    
    /**
     * Generate a new unique Channel identifier. 
    * 
    * If the ConnectionManager that created this Channel is a
    * ConnectionAcceptor, the returned value will be in the range:
    * 
    * [-Integer.MAX_VALUE, 0)
    * 
    * If the ConnectionManager that created this Channel is a
    * ConnectionInitiator, the returned value will be in the range:
    * 
    * (0, Integer.MAX_VALUE)
    * 
    * The space of identifiers must be partitioned in order to prevent
    * collisions.
    * 
    * @return a new unique Channel identifier
     */
    protected int generateChannelId()
        {
        // import com.tangosol.net.messaging.ConnectionAcceptor;
        // import com.tangosol.util.Base;
        // import com.tangosol.util.LongArray;
        
        LongArray la     = getChannelPendingArray();
        int       nScale = getConnectionManager() instanceof ConnectionAcceptor ? 1 : -1;
        int       nId;
        do
            {
            nId = Base.getRandom().nextInt(Integer.MAX_VALUE) * nScale;
            }
        while (nId == 0 || getChannel(nId) != null || la.get(nId) != null);
        
        return nId;
        }
    
    // From interface: com.tangosol.net.messaging.Connection
    public com.tangosol.net.messaging.Channel getChannel(int nId)
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Peer as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer;
        // import com.tangosol.util.LongArray;
        
        LongArray la = getChannelArray();
        
        // avoid synchronization if possible; see Peer#decodeMessage
        if (((com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer) getConnectionManager()).isServiceThread(false))
            {
            return (Channel) la.get(nId);
            }
        
        synchronized (la)
            {
            return (Channel) la.get(nId);
            }
        }
    
    // Accessor for the property "ChannelArray"
    /**
     * Getter for property ChannelArray.<p>
    * A LongArray of open Channel objects created by this Connection, indexed
    * by Channel identifier.
     */
    public com.tangosol.util.LongArray getChannelArray()
        {
        return __m_ChannelArray;
        }
    
    // Accessor for the property "ChannelPendingArray"
    /**
     * Getter for property ChannelPendingArray.<p>
    * A LongArray of newly created Channel objects that have not yet accepted
    * by the peer, indexed by Channel identifier.
     */
    public com.tangosol.util.LongArray getChannelPendingArray()
        {
        return __m_ChannelPendingArray;
        }
    
    // From interface: com.tangosol.net.messaging.Connection
    public java.util.Collection getChannels()
        {
        // import com.tangosol.util.LongArray;
        // import java.util.ArrayList;
        // import java.util.List;
        // import java.util.Iterator;
        
        LongArray la = getChannelArray();
        synchronized (la)
            {
            List list = new ArrayList(la.getSize());
            for (Iterator iter = la.iterator(); iter.hasNext(); )
                {
                list.add(iter.next());
                }
        
            return list;
            }
        }
    
    // Accessor for the property "CloseThrowable"
    /**
     * Getter for property CloseThrowable.<p>
    * The Throwable to pass to the close() method when the Connection is closed
    * upon exiting the ThreadGate (see CloseOnExit property).
    * 
    * @volatile
     */
    public Throwable getCloseThrowable()
        {
        return __m_CloseThrowable;
        }
    
    // From interface: com.tangosol.net.messaging.Connection
    // Accessor for the property "ConnectionManager"
    /**
     * Getter for property ConnectionManager.<p>
    * The ConnectionManager that created or accepted this Connection.
    * 
    * @volatile
    * @see com.tangosol.net.messaging.Connection#getConnectionManager
     */
    public com.tangosol.net.messaging.ConnectionManager getConnectionManager()
        {
        return __m_ConnectionManager;
        }
    
    // Declared at the super level
    /**
     * Return a human-readable description of this component.
    * 
    * @return a String representation of this component
     */
    protected String getDescription()
        {
        // import com.tangosol.net.Member;
        
        StringBuffer sb = new StringBuffer();
        
        sb.append("Id="    ).append(getId())
          .append(", Open=").append(isOpen());
        
        Member member = getMember();
        if (member != null)
            {
            sb.append(", ").append(member.toString());
            }
        
        return sb.toString();
        }
    
    // From interface: com.tangosol.net.messaging.Connection
    // Accessor for the property "Id"
    /**
     * Getter for property Id.<p>
    * The unique identifier of this Connection object.
    * 
    * @see com.tangosol.net.messaging.Connection#getId
     */
    public com.tangosol.util.UUID getId()
        {
        return __m_Id;
        }
    
    // Accessor for the property "Member"
    /**
     * Getter for property Member.<p>
    * The optional Member object associated with this Connection.
     */
    public com.tangosol.net.Member getMember()
        {
        return __m_Member;
        }
    
    // Accessor for the property "MessageFactoryMap"
    /**
     * Getter for property MessageFactoryMap.<p>
    * A Map of MessageFactory objects that may be used by Channel objects
    * created by this Connection, keyed by Protocol name.
     */
    public java.util.Map getMessageFactoryMap()
        {
        return __m_MessageFactoryMap;
        }
    
    // Accessor for the property "PeerEdition"
    /**
     * Getter for property PeerEdition.<p>
    * The product edition used by the peer.
    * 
    * @see Component.Application.Console.Coherence#getEdition
     */
    public int getPeerEdition()
        {
        return __m_PeerEdition;
        }
    
    // From interface: com.tangosol.net.messaging.Connection
    // Accessor for the property "PeerId"
    /**
     * Getter for property PeerId.<p>
    * The unique identifier of the peer process to which this Connection object
    * is connected.
    * 
    * @see com.tangosol.net.messaging.Connection#getPeerId
     */
    public com.tangosol.util.UUID getPeerId()
        {
        return __m_PeerId;
        }
    
    // Accessor for the property "PingLastMillis"
    /**
     * Getter for property PingLastMillis.<p>
    * The send time of the last outstanding PingRequest or 0 if a PingRequest
    * is not outstanding.
     */
    public long getPingLastMillis()
        {
        return __m_PingLastMillis;
        }
    
    // Accessor for the property "StatsBytesReceived"
    /**
     * Getter for property StatsBytesReceived.<p>
    * Statistics: total number of bytes received over this Connection.
     */
    public long getStatsBytesReceived()
        {
        return __m_StatsBytesReceived;
        }
    
    // Accessor for the property "StatsBytesSent"
    /**
     * Getter for property StatsBytesSent.<p>
    * Statistics: total number of bytes sent over this Connection.
     */
    public long getStatsBytesSent()
        {
        return __m_StatsBytesSent;
        }
    
    // Accessor for the property "StatsReceived"
    /**
     * Getter for property StatsReceived.<p>
    * Statistics: total number of Messages received over this Connection.
     */
    public long getStatsReceived()
        {
        return __m_StatsReceived;
        }
    
    // Accessor for the property "StatsReset"
    /**
     * Getter for property StatsReset.<p>
    * Statistics: date/time value that the stats have been reset.
     */
    public long getStatsReset()
        {
        return __m_StatsReset;
        }
    
    // Accessor for the property "StatsSent"
    /**
     * Getter for property StatsSent.<p>
    * Statistics: total number of messages sent over this Connection.
     */
    public long getStatsSent()
        {
        return __m_StatsSent;
        }
    
    // Accessor for the property "ThreadGate"
    /**
     * Getter for property ThreadGate.<p>
    * A ThreadGate used to prevent concurrent use of this Connection while it
    * is being closed.
     */
    protected com.tangosol.util.ThreadGate getThreadGate()
        {
        return __m_ThreadGate;
        }
    
    // Accessor for the property "ActiveThread"
    /**
     * Getter for property ActiveThread.<p>
    * Return true if the calling thread is currently executing within the
    * Connection's ThreadGate.
     */
    public boolean isActiveThread()
        {
        return getThreadGate().isEnteredByCurrentThread();
        }
    
    // Accessor for the property "CloseNotify"
    /**
     * Getter for property CloseNotify.<p>
    * Peer notification flag used when the Connection is closed upon exiting
    * the ThreadGate (see CloseOnExit property).
    * 
    * @volatile
     */
    public boolean isCloseNotify()
        {
        return __m_CloseNotify;
        }
    
    // Accessor for the property "CloseOnExit"
    /**
     * Getter for property CloseOnExit.<p>
    * If true, the Thread that is currently executing within the Connection
    * should close the Connection immedately upon exiting the Connection's
    * ThreadGate.
    * 
    * @volatile
     */
    public boolean isCloseOnExit()
        {
        return __m_CloseOnExit;
        }
    
    // Accessor for the property "MessagingDebug"
    /**
     * Getter for property MessagingDebug.<p>
    * Debug flag.  When true and the node's logging level is 6 or higher, sent
    * and received messages will be logged.
     */
    public boolean isMessagingDebug()
        {
        return __m_MessagingDebug;
        }
    
    // From interface: com.tangosol.net.messaging.Connection
    // Accessor for the property "Open"
    /**
     * Getter for property Open.<p>
    * True if the Connection is open; false otherwise.
    * 
    * @see com.tangosol.net.messaging.Connection#isOpen
    * @volatile
     */
    public boolean isOpen()
        {
        return __m_Open;
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
        // import com.tangosol.util.ThreadGate;
        
        // create and register "Channel0"
        Channel channel0 = new Channel();
        channel0.setConnection(this);
        registerChannel(channel0);
        
        setThreadGate(new ThreadGate());
        
        super.onInit();
        }
    
    /**
     * Open the Connection.
     */
    public void open()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Peer as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer;
        
        if (!isOpen())
            {
            ((com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer) getConnectionManager()).openConnection(this);
            }
        }
    
    // From interface: com.tangosol.net.messaging.Connection
    public com.tangosol.net.messaging.Channel openChannel(com.tangosol.net.messaging.Protocol protocol, String sName, ClassLoader loader, com.tangosol.net.messaging.Channel.Receiver receiver, javax.security.auth.Subject subject)
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Peer as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer;
        
        return ((com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer) getConnectionManager()).openChannel(this, protocol, sName,
                loader, receiver, subject);
        }
    
    /**
     * The openChannel() implementation method. This method is called on the
    * service thread.
    * 
    * @see com.tangosol.net.messaging.Connection#openChannel
     */
    public com.tangosol.net.messaging.Request.Status openChannelInternal(com.tangosol.net.messaging.Protocol protocol, String sName, com.tangosol.io.Serializer serializer, com.tangosol.net.messaging.Channel.Receiver receiver, javax.security.auth.Subject subject, byte[] abToken)
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Peer$MessageFactory$OpenChannelRequest as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.OpenChannelRequest;
        // import com.tangosol.net.messaging.Protocol$MessageFactory as com.tangosol.net.messaging.Protocol.MessageFactory;
        
        assertOpen();
        
        if (protocol == null)
            {
            throw new IllegalArgumentException("protocol cannot be null");
            }
        
        if (sName == null)
            {
            throw new IllegalArgumentException("name cannot be null");
            }
        
        if (serializer == null)
            {
            throw new IllegalArgumentException("serializer cannot be null");
            }
        
        String sProtocol = protocol.getName();
        _assert(sProtocol != null);
        
        com.tangosol.net.messaging.Protocol.MessageFactory factory = (com.tangosol.net.messaging.Protocol.MessageFactory) getMessageFactoryMap().get(sProtocol);
        if (factory == null)
            {
            throw new IllegalArgumentException("unknown protocol: " + sProtocol);
            }
        
        if (receiver != null)
            {
            if (receiver.getProtocol() != factory.getProtocol())
                {
                throw new IllegalArgumentException("protocol mismatch; expected "
                        + factory.getProtocol() + ", retrieved "
                        + receiver.getProtocol() + ")");
                }
            }
        
        // send a ChannelOpenRequest to the peer via "Channel0"
        Channel channel0 = (Channel) getChannel(0);
        com.tangosol.net.messaging.Protocol.MessageFactory factory0 = (com.tangosol.net.messaging.Protocol.MessageFactory) channel0.getMessageFactory();
        com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.OpenChannelRequest request  = (com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.OpenChannelRequest) factory0.createMessage(com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.OpenChannelRequest.TYPE_ID);
        
        request.setIdentityToken(abToken);
        request.setMessageFactory(factory);
        request.setProtocolName(sProtocol);
        request.setReceiver(receiver);
        request.setReceiverName(sName);
        request.setSerializer(serializer);
        request.setSubject(subject);
        
        return channel0.send(request);
        }
    
    /**
     * The openChannel() recipient implementation method. This method is called
    * on the service thread in response to a ChannelOpenRequest.
    * 
    * @return the identifier of the newly opened Channel
    * 
    * @see com.tangosol.net.messaging.Connection#openChannel
     */
    public int openChannelRequest(String sProtocol, com.tangosol.io.Serializer serializer, com.tangosol.net.messaging.Channel.Receiver receiver, javax.security.auth.Subject subject, com.tangosol.internal.net.security.AccessAdapter adapter)
        {
        // import com.tangosol.net.messaging.Protocol$MessageFactory as com.tangosol.net.messaging.Protocol.MessageFactory;
        
        assertOpen();
        
        if (sProtocol == null)
            {
            throw new IllegalArgumentException("protocol name cannot be null");
            }
        
        if (serializer == null)
            {
            throw new IllegalArgumentException("serializer cannot be null");
            }
        
        com.tangosol.net.messaging.Protocol.MessageFactory factory = (com.tangosol.net.messaging.Protocol.MessageFactory) getMessageFactoryMap().get(sProtocol);
        if (factory == null)
            {
            throw new IllegalArgumentException("unknown protocol: " + sProtocol);
            }
        
        if (receiver != null)
            {
            if (receiver.getProtocol() != factory.getProtocol())
                {
                throw new IllegalArgumentException("protocol mismatch; expected "
                        + factory.getProtocol() + ", retrieved "
                        + receiver.getProtocol() + ')');
                }
            }
        
        int nId = generateChannelId();
        
        Channel channel = new Channel();
        channel.setConnection(this);
        channel.setId(nId);
        channel.setMessageFactory(factory);
        channel.setReceiver(receiver);
        channel.setSerializer(serializer);
        channel.setSubject(subject);
        channel.setAccessAdapter(adapter);
        channel.openInternal();
        
        registerChannel(channel);
        
        return nId;
        }
    
    /**
     * The openChannel() initiator implementation method. This method is called
    * on the service thread in response to a ChannelOpenResponse.
    * 
    * @return the newly opened Channel
    * 
    * @see com.tangosol.net.messaging.Connection#openChannel
     */
    public com.tangosol.net.messaging.Channel openChannelResponse(int nId, com.tangosol.net.messaging.Protocol.MessageFactory factory, com.tangosol.io.Serializer serializer, com.tangosol.net.messaging.Channel.Receiver receiver, javax.security.auth.Subject subject)
        {
        // import com.tangosol.net.messaging.Protocol$MessageFactory as com.tangosol.net.messaging.Protocol.MessageFactory;
        
        assertOpen();
        
        if (factory == null)
            {
            throw new IllegalArgumentException("factory cannot be null");
            }
        
        if (serializer == null)
            {
            throw new IllegalArgumentException("serializer cannot be null");
            }
        
        Channel channel = new Channel();
        channel.setId(nId);
        channel.setConnection(this);
        channel.setMessageFactory(factory);
        channel.setReceiver(receiver);
        channel.setSerializer(serializer);
        channel.setSubject(subject);
        channel.openInternal();
        
        registerChannel(channel);
        
        return channel;
        }
    
    /**
     * The open() implementation method. This method is called on the service
    * thread.
     */
    public void openInternal()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Peer as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer;
        // import Component.Util.Daemon.QueueProcessor.Service.Peer$Protocol as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.Protocol;
        // import com.tangosol.net.messaging.Protocol$MessageFactory as com.tangosol.net.messaging.Protocol.MessageFactory;
        
        if (isOpen())
            {
            return;
            }
        
        com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer manager = (com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer) getConnectionManager();
        _assert(manager != null);
        
        // make sure the ConnectionManager has the MessagingProtocol
        com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.Protocol protocol = (com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.Protocol) manager.getProtocol(com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.Protocol.PROTOCOL_NAME);
        _assert(protocol != null);
        
        // look up the appropriate MessagingProtocol MessageFactory
        com.tangosol.net.messaging.Protocol.MessageFactory factory = protocol.getMessageFactory(protocol.getCurrentVersion());
        
        // open "Channel0"
        Channel channel0 = (Channel) getChannel(0);
        channel0.setMessageFactory(factory);
        channel0.setReceiver(manager);
        channel0.setSerializer(manager.ensureSerializer());
        channel0.openInternal();
        
        setOpen(true);
        
        // note that we do not notify the ConnectionManager that the Connection has
        // opened just yet; the Connection still needs to be connected or accepted
        // (See ConnectionOpenRequest and ConnectionOpenResponse)
        }
    
    /**
     * Send a PingRequest via "Channel0" and update the PingLastMillis property.
    * This method will only send a PingRequest if one is not already
    * outstanding.
    * 
    * This method is only called on the service thread.
    * 
    * @return true if a PingRequest was sent
     */
    public boolean ping()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Peer$MessageFactory$PingRequest as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.PingRequest;
        // import com.tangosol.net.messaging.Protocol$MessageFactory as com.tangosol.net.messaging.Protocol.MessageFactory;
        // import com.tangosol.util.Base;
        
        if (getPingLastMillis() == 0)
            {
            Channel channel0 = (Channel) getChannel(0);
            com.tangosol.net.messaging.Protocol.MessageFactory factory  = channel0.getMessageFactory();
            com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.PingRequest request  = (com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.PingRequest) factory.createMessage(com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer.MessageFactory.PingRequest.TYPE_ID);
        
            try
                {
                channel0.send(request);
                }
            catch (RuntimeException e)
                {
                return false;
                }
        
            setPingLastMillis(Base.getSafeTimeMillis());
            return true;
            }
        else
            {
            return false;
            }
        }
    
    /**
     * Register the given Channel in the ChannelArray.
    * 
    * @param channel  the Channel to register; must not be null
     */
    protected void registerChannel(Channel channel)
        {
        // import com.tangosol.util.LongArray;
        
        LongArray la = getChannelArray();
        synchronized (la)
            {
            Object oChannel = la.set(channel.getId(), channel);
            if (oChannel != null)
                {
                la.set(channel.getId(), oChannel);
                throw new IllegalArgumentException("duplicate channel: " + channel);
                }
            }
        }
    
    /**
     * Release the given ReadBuffer.
    * 
    * @param buf  the ReadBuffer to release
     */
    protected void releaseReadBuffer(com.tangosol.io.ReadBuffer rb)
        {
        }
    
    /**
     * Reset the Connection statistics.
     */
    public void resetStats()
        {
        // import com.tangosol.util.Base;
        
        setStatsBytesReceived(0L);
        setStatsBytesSent(0L);
        setStatsReceived(0L);
        setStatsSent(0L);
        setStatsReset(Base.getSafeTimeMillis());
        }
    
    /**
     * Send the given WriteBuffer over this Connection.
    * 
    * @param wb  the WriteBuffer to send
    * 
    * @throws ConnectionException on fatal Connection error
     */
    public void send(com.tangosol.io.WriteBuffer wb)
            throws com.tangosol.net.messaging.ConnectionException
        {
        assertOpen();
        
        // update stats
        setStatsBytesSent(getStatsBytesSent() + wb.length());
        setStatsSent(getStatsSent() + 1);
        }
    
    // Accessor for the property "ChannelArray"
    /**
     * Setter for property ChannelArray.<p>
    * A LongArray of open Channel objects created by this Connection, indexed
    * by Channel identifier.
     */
    protected void setChannelArray(com.tangosol.util.LongArray list)
        {
        __m_ChannelArray = list;
        }
    
    // Accessor for the property "ChannelPendingArray"
    /**
     * Setter for property ChannelPendingArray.<p>
    * A LongArray of newly created Channel objects that have not yet accepted
    * by the peer, indexed by Channel identifier.
     */
    protected void setChannelPendingArray(com.tangosol.util.LongArray list)
        {
        __m_ChannelPendingArray = list;
        }
    
    // Accessor for the property "CloseNotify"
    /**
     * Setter for property CloseNotify.<p>
    * Peer notification flag used when the Connection is closed upon exiting
    * the ThreadGate (see CloseOnExit property).
    * 
    * @volatile
     */
    public void setCloseNotify(boolean f)
        {
        __m_CloseNotify = f;
        }
    
    // Accessor for the property "CloseOnExit"
    /**
     * Setter for property CloseOnExit.<p>
    * If true, the Thread that is currently executing within the Connection
    * should close the Connection immedately upon exiting the Connection's
    * ThreadGate.
    * 
    * @volatile
     */
    public void setCloseOnExit(boolean fClose)
        {
        __m_CloseOnExit = fClose;
        }
    
    // Accessor for the property "CloseThrowable"
    /**
     * Setter for property CloseThrowable.<p>
    * The Throwable to pass to the close() method when the Connection is closed
    * upon exiting the ThreadGate (see CloseOnExit property).
    * 
    * @volatile
     */
    public void setCloseThrowable(Throwable e)
        {
        __m_CloseThrowable = e;
        }
    
    // Accessor for the property "ConnectionManager"
    /**
     * Setter for property ConnectionManager.<p>
    * The ConnectionManager that created or accepted this Connection.
    * 
    * @volatile
    * @see com.tangosol.net.messaging.Connection#getConnectionManager
     */
    public void setConnectionManager(com.tangosol.net.messaging.ConnectionManager manager)
        {
        _assert(!isOpen());
        
        __m_ConnectionManager = (manager);
        }
    
    // Accessor for the property "Id"
    /**
     * Setter for property Id.<p>
    * The unique identifier of this Connection object.
    * 
    * @see com.tangosol.net.messaging.Connection#getId
     */
    public void setId(com.tangosol.util.UUID uuid)
        {
        _assert(getId() == null);
        
        __m_Id = (uuid);
        }
    
    // Accessor for the property "Member"
    /**
     * Setter for property Member.<p>
    * The optional Member object associated with this Connection.
     */
    public void setMember(com.tangosol.net.Member member)
        {
        __m_Member = member;
        }
    
    // Accessor for the property "MessageFactoryMap"
    /**
     * Setter for property MessageFactoryMap.<p>
    * A Map of MessageFactory objects that may be used by Channel objects
    * created by this Connection, keyed by Protocol name.
     */
    public void setMessageFactoryMap(java.util.Map map)
        {
        __m_MessageFactoryMap = map;
        }
    
    // Accessor for the property "MessagingDebug"
    /**
     * Setter for property MessagingDebug.<p>
    * Debug flag.  When true and the node's logging level is 6 or higher, sent
    * and received messages will be logged.
     */
    public void setMessagingDebug(boolean fMessageDebug)
        {
        __m_MessagingDebug = fMessageDebug;
        }
    
    // Accessor for the property "Open"
    /**
     * Setter for property Open.<p>
    * True if the Connection is open; false otherwise.
    * 
    * @see com.tangosol.net.messaging.Connection#isOpen
    * @volatile
     */
    protected void setOpen(boolean fOpen)
        {
        __m_Open = fOpen;
        }
    
    // Accessor for the property "PeerEdition"
    /**
     * Setter for property PeerEdition.<p>
    * The product edition used by the peer.
    * 
    * @see Component.Application.Console.Coherence#getEdition
     */
    public void setPeerEdition(int nEdition)
        {
        __m_PeerEdition = nEdition;
        }
    
    // Accessor for the property "PeerId"
    /**
     * Setter for property PeerId.<p>
    * The unique identifier of the peer process to which this Connection object
    * is connected.
    * 
    * @see com.tangosol.net.messaging.Connection#getPeerId
     */
    public void setPeerId(com.tangosol.util.UUID uuid)
        {
        __m_PeerId = uuid;
        }
    
    // Accessor for the property "PingLastMillis"
    /**
     * Setter for property PingLastMillis.<p>
    * The send time of the last outstanding PingRequest or 0 if a PingRequest
    * is not outstanding.
     */
    public void setPingLastMillis(long ldt)
        {
        __m_PingLastMillis = ldt;
        }
    
    // Accessor for the property "StatsBytesReceived"
    /**
     * Setter for property StatsBytesReceived.<p>
    * Statistics: total number of bytes received over this Connection.
     */
    public void setStatsBytesReceived(long cb)
        {
        __m_StatsBytesReceived = cb;
        }
    
    // Accessor for the property "StatsBytesSent"
    /**
     * Setter for property StatsBytesSent.<p>
    * Statistics: total number of bytes sent over this Connection.
     */
    public void setStatsBytesSent(long cb)
        {
        __m_StatsBytesSent = cb;
        }
    
    // Accessor for the property "StatsReceived"
    /**
     * Setter for property StatsReceived.<p>
    * Statistics: total number of Messages received over this Connection.
     */
    public void setStatsReceived(long cMessage)
        {
        __m_StatsReceived = cMessage;
        }
    
    // Accessor for the property "StatsReset"
    /**
     * Setter for property StatsReset.<p>
    * Statistics: date/time value that the stats have been reset.
     */
    public void setStatsReset(long ldt)
        {
        __m_StatsReset = ldt;
        }
    
    // Accessor for the property "StatsSent"
    /**
     * Setter for property StatsSent.<p>
    * Statistics: total number of messages sent over this Connection.
     */
    public void setStatsSent(long cMessage)
        {
        __m_StatsSent = cMessage;
        }
    
    // Accessor for the property "ThreadGate"
    /**
     * Setter for property ThreadGate.<p>
    * A ThreadGate used to prevent concurrent use of this Connection while it
    * is being closed.
     */
    protected void setThreadGate(com.tangosol.util.ThreadGate gate)
        {
        __m_ThreadGate = gate;
        }
    
    /**
     * Unregister the given Channel from the ChannelArray.
    * 
    * @param channel  the Channel to unregister; must not be null
     */
    public void unregisterChannel(com.tangosol.net.messaging.Channel channel)
        {
        // import com.tangosol.util.LongArray;
        
        if (channel.getId() == 0)
            {
            // never unregister "Channel0"
            // see #onInit
            return;
            }
        
        LongArray la = getChannelArray();
        synchronized (la)
            {
            la.remove(channel.getId());
            }
        }
    }
